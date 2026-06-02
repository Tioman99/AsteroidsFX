package dk.sdu.mmmi.cbse.main;

import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IEntityProcessingService;
import dk.sdu.mmmi.cbse.common.services.IGamePluginService;
import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;

import java.io.IOException;
import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

/**
 * Manages runtime loading and unloading of plugin modules from the {@code plugins/} directory.
 *
 * <p>JARs placed in {@code plugins/} are loaded into a child {@link ModuleLayer} and their
 * {@link IGamePluginService}, {@link IEntityProcessingService}, and
 * {@link IPostEntityProcessingService} implementations are discovered via
 * {@link ServiceLoader}. When a JAR is added or removed, the affected plugins'
 * entities are cleaned up via {@code stop()} before the new set is activated via
 * {@code start()}.</p>
 *
 * <p>Modules already present in the boot layer (i.e. on the main {@code --module-path}) are
 * not loaded again from {@code plugins/} to prevent duplicate instances.</p>
 *
 * <p>To make a module dynamic (hot-swappable), move its JAR out of {@code mods-mvn/} and
 * into the {@code plugins/} directory. The game will detect it on the next change event
 * and call {@code start()} automatically.</p>
 */
class PluginManager {

    private final Path pluginsDir;

    /** Services from modules on the main module-path (boot layer). Never change at runtime. */
    private final List<IGamePluginService> staticPluginServices;
    private final List<IEntityProcessingService> staticEntityProcessors;
    private final List<IPostEntityProcessingService> staticPostProcessors;

    /** Services from the dynamic child ModuleLayer (plugins/ directory). Rebuilt on each change. */
    private List<IGamePluginService> dynamicPluginServices = new ArrayList<>();
    private List<IEntityProcessingService> dynamicEntityProcessors = new ArrayList<>();
    private List<IPostEntityProcessingService> dynamicPostProcessors = new ArrayList<>();

    /** The active child ModuleLayer. {@code null} when no plugins are currently loaded. */
    private ModuleLayer pluginLayer;

    /**
     * Signals from the WatchService daemon thread to the game loop thread.
     * Any non-null offer means "the plugins directory changed".
     */
    private final Queue<Object> changeSignals = new ConcurrentLinkedQueue<>();

    private Thread watcherThread;
    private volatile boolean watcherRunning = true;

    PluginManager(Path pluginsDir) {
        this.pluginsDir = pluginsDir;

        // Snapshot static services from the boot layer once at construction time.
        staticPluginServices = ServiceLoader.load(IGamePluginService.class)
                .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        staticEntityProcessors = ServiceLoader.load(IEntityProcessingService.class)
                .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
        staticPostProcessors = ServiceLoader.load(IPostEntityProcessingService.class)
                .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());

        try {
            Files.createDirectories(pluginsDir);
        } catch (IOException e) {
            System.err.println("[PluginManager] Could not create plugins directory: " + e.getMessage());
        }

        // Load any JARs already present in plugins/ at startup (start() is called later by Game).
        rebuildPluginLayer();
    }

    // -------------------------------------------------------------------------
    // Lifecycle called by Game
    // -------------------------------------------------------------------------

    /**
     * Starts watching {@code plugins/} for JAR changes.
     * Call this once the game is fully initialised (i.e. after the initial {@code start()} calls).
     */
    void startWatcher() {
        try {
            WatchService watcher = FileSystems.getDefault().newWatchService();
            pluginsDir.register(watcher,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_DELETE,
                    StandardWatchEventKinds.ENTRY_MODIFY);

            watcherThread = new Thread(() -> {
                while (watcherRunning) {
                    try {
                        WatchKey key = watcher.take(); // blocks until an event arrives
                        boolean jarChanged = key.pollEvents().stream()
                                .anyMatch(e -> e.context().toString().endsWith(".jar"));
                        key.reset();
                        if (jarChanged) {
                            Thread.sleep(300); // brief debounce so the file is fully written
                            changeSignals.offer(Boolean.TRUE);
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                try {
                    watcher.close();
                } catch (IOException ignored) {
                }
            }, "PluginWatcher");

            watcherThread.setDaemon(true);
            watcherThread.start();
            System.out.println("[PluginManager] Watching " + pluginsDir.toAbsolutePath() + " for plugin changes.");
        } catch (IOException e) {
            System.err.println("[PluginManager] Could not start watcher: " + e.getMessage());
        }
    }

    /**
     * Checks whether any plugin JARs were added or removed since the last call and, if so,
     * stops the old dynamic plugins, rebuilds the child ModuleLayer, and starts the new ones.
     *
     * <p>Must be called from the game-loop thread (JavaFX Application Thread) so that
     * {@code start()} and {@code stop()} run on the same thread as the rest of the game.</p>
     */
    void checkForChanges(GameData gameData, World world) {
        if (changeSignals.isEmpty()) {
            return;
        }
        changeSignals.clear();
        applyPluginChanges(gameData, world);
    }

    /**
     * Discards pending change signals so that a game restart does not trigger a spurious reload.
     */
    void clearPendingChanges() {
        changeSignals.clear();
    }

    /** Stops all dynamic plugins and the watcher thread. Safe to call multiple times. */
    void shutdown(GameData gameData, World world) {
        watcherRunning = false;
        if (watcherThread != null) {
            watcherThread.interrupt();
        }
        for (IGamePluginService service : dynamicPluginServices) {
            safeSop(service, gameData, world);
        }
    }

    // -------------------------------------------------------------------------
    // Aggregated service accessors (used by Game in the main loop)
    // -------------------------------------------------------------------------

    /** Returns all active plugin services: static (boot layer) + dynamic (plugins/ layer). */
    List<IGamePluginService> getAllPluginServices() {
        List<IGamePluginService> all = new ArrayList<>(staticPluginServices);
        all.addAll(dynamicPluginServices);
        return all;
    }

    /** Returns all active entity processors: static + dynamic. */
    List<IEntityProcessingService> getAllEntityProcessors() {
        List<IEntityProcessingService> all = new ArrayList<>(staticEntityProcessors);
        all.addAll(dynamicEntityProcessors);
        return all;
    }

    /** Returns all active post-processors: static + dynamic. */
    List<IPostEntityProcessingService> getAllPostProcessors() {
        List<IPostEntityProcessingService> all = new ArrayList<>(staticPostProcessors);
        all.addAll(dynamicPostProcessors);
        return all;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    /**
     * Responds to a detected change in the plugins/ directory:
     * <ol>
     *   <li>Stop all currently active dynamic plugins (removes their entities).</li>
     *   <li>Rebuild the child ModuleLayer from the current JARs.</li>
     *   <li>Start all newly loaded dynamic plugins (creates their entities).</li>
     * </ol>
     */
    private void applyPluginChanges(GameData gameData, World world) {
        System.out.println("[PluginManager] Plugin directory changed — reloading dynamic plugins.");

        // Step 1: stop all current dynamic plugins to clean up their entities
        for (IGamePluginService service : dynamicPluginServices) {
            safeSop(service, gameData, world);
        }

        // Step 2: rebuild the layer (clears and repopulates the dynamic lists)
        rebuildPluginLayer();

        // Step 3: start the fresh set of dynamic plugins
        for (IGamePluginService service : dynamicPluginServices) {
            safeStart(service, gameData, world);
        }
    }

    /**
     * Scans {@code plugins/} and builds a new child {@link ModuleLayer}.
     * Populates {@link #dynamicPluginServices}, {@link #dynamicEntityProcessors}, and
     * {@link #dynamicPostProcessors} from the new layer.
     */
    private void rebuildPluginLayer() {
        dynamicPluginServices.clear();
        dynamicEntityProcessors.clear();
        dynamicPostProcessors.clear();
        pluginLayer = null;

        Set<Path> jars = scanPluginJars();
        if (jars.isEmpty()) {
            return;
        }

        try {
            ModuleFinder finder = ModuleFinder.of(jars.toArray(new Path[0]));

            // Only include modules that are NOT already resolved in the boot layer.
            // This prevents double-loading modules that are also on the main module-path.
            List<String> moduleNames = finder.findAll().stream()
                    .map(ModuleReference::descriptor)
                    .map(ModuleDescriptor::name)
                    .filter(name -> ModuleLayer.boot().findModule(name).isEmpty())
                    .collect(Collectors.toList());

            if (moduleNames.isEmpty()) {
                System.out.println("[PluginManager] All JARs in plugins/ are already loaded via the boot layer; nothing to load dynamically.");
                return;
            }

            Configuration config = ModuleLayer.boot().configuration()
                    .resolve(finder, ModuleFinder.of(), moduleNames);

            pluginLayer = ModuleLayer.boot()
                    .defineModulesWithOneLoader(config, ClassLoader.getSystemClassLoader());

            dynamicPluginServices = ServiceLoader.load(pluginLayer, IGamePluginService.class)
                    .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
            dynamicEntityProcessors = ServiceLoader.load(pluginLayer, IEntityProcessingService.class)
                    .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());
            dynamicPostProcessors = ServiceLoader.load(pluginLayer, IPostEntityProcessingService.class)
                    .stream().map(ServiceLoader.Provider::get).collect(Collectors.toList());

            System.out.println("[PluginManager] Loaded dynamic modules: " + moduleNames);

        } catch (Exception e) {
            System.err.println("[PluginManager] Error building plugin layer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Returns all {@code .jar} files currently in the plugins directory. */
    private Set<Path> scanPluginJars() {
        try {
            if (!Files.exists(pluginsDir)) {
                return Set.of();
            }
            return Files.list(pluginsDir)
                    .filter(p -> p.toString().endsWith(".jar"))
                    .collect(Collectors.toSet());
        } catch (IOException e) {
            System.err.println("[PluginManager] Error scanning plugins dir: " + e.getMessage());
            return Set.of();
        }
    }

    private void safeStart(IGamePluginService service, GameData gameData, World world) {
        try {
            service.start(gameData, world);
        } catch (Exception e) {
            System.err.println("[PluginManager] Error starting plugin " + service.getClass().getName() + ": " + e.getMessage());
        }
    }

    private void safeSop(IGamePluginService service, GameData gameData, World world) {
        try {
            service.stop(gameData, world);
        } catch (Exception e) {
            System.err.println("[PluginManager] Error stopping plugin " + service.getClass().getName() + ": " + e.getMessage());
        }
    }
}
