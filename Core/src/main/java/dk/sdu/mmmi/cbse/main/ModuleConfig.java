package dk.sdu.mmmi.cbse.main;

import java.nio.file.Paths;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires together the core game objects.
 *
 * <p>Service discovery (static and dynamic) is delegated entirely to
 * {@link PluginManager}. Static plugins are resolved from the boot module layer
 * via {@link java.util.ServiceLoader}; dynamic plugins are loaded from the
 * {@code plugins/} directory at runtime without recompilation.</p>
 */
@Configuration
class ModuleConfig {

    @Bean
    public PluginManager pluginManager() {
        return new PluginManager(Paths.get("plugins"));
    }

    @Bean
    public Game game(PluginManager pluginManager) {
        return new Game(pluginManager);
    }
}
