package dk.sdu.mmmi.cbse.common.util;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility for locating services from a child {@link ModuleLayer} built from the
 * {@code plugins/} directory.
 *
 * <p><strong>Note:</strong> For full dynamic loading and unloading support (including
 * runtime hot-swap of plugin JARs), prefer {@code PluginManager} in the Core module.
 * {@code ServiceLocator} provides a simpler, snapshot-at-startup view of the same
 * plugins directory and does not watch for filesystem changes.</p>
 */
public enum ServiceLocator {

    INSTANCE;

    private static final Map<Class<?>, ServiceLoader<?>> loaderMap = new HashMap<>();
    private final ModuleLayer layer;

    ServiceLocator() {
        Path pluginsDir = Paths.get("plugins");
        ModuleLayer resolvedLayer = null;

        try {
            if (Files.exists(pluginsDir) && Files.isDirectory(pluginsDir)) {
                ModuleFinder pluginsFinder = ModuleFinder.of(pluginsDir);

                List<String> plugins = pluginsFinder
                        .findAll()
                        .stream()
                        .map(ModuleReference::descriptor)
                        .map(ModuleDescriptor::name)
                        .filter(name -> ModuleLayer.boot().findModule(name).isEmpty())
                        .collect(Collectors.toList());

                if (!plugins.isEmpty()) {
                    Configuration pluginsConfiguration = ModuleLayer
                            .boot()
                            .configuration()
                            .resolve(pluginsFinder, ModuleFinder.of(), plugins);

                    resolvedLayer = ModuleLayer
                            .boot()
                            .defineModulesWithOneLoader(pluginsConfiguration,
                                    ClassLoader.getSystemClassLoader());
                }
            }
        } catch (Exception e) {
            System.err.println("[ServiceLocator] Could not build plugin layer: " + e.getMessage());
        }

        this.layer = resolvedLayer != null ? resolvedLayer : ModuleLayer.boot();
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> locateAll(Class<T> service) {
        ServiceLoader<T> loader = (ServiceLoader<T>) loaderMap.get(service);

        if (loader == null) {
            loader = ServiceLoader.load(layer, service);
            loaderMap.put(service, loader);
        }

        List<T> list = new ArrayList<>();
        try {
            for (T instance : loader) {
                list.add(instance);
            }
        } catch (ServiceConfigurationError serviceError) {
            serviceError.printStackTrace();
        }
        return list;
    }
}
