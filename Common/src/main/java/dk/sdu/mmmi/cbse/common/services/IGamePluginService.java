package dk.sdu.mmmi.cbse.common.services;

import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;

/**
 * Lifecycle hook for game feature modules.
 * Implementations register and unregister their own entities/resources in the game world.
 */
public interface IGamePluginService {

    /**
     * Starts the plugin and adds any initial entities/resources.
     *
     * <p>Preconditions:</p>
     * <ul>
     *   <li>{@code gameData != null}</li>
     *   <li>{@code world != null}</li>
     *   <li>Should be called once during game startup for this plugin instance</li>
     * </ul>
     *
     * <p>Postconditions:</p>
     * <ul>
     *   <li>Plugin-managed initial entities/resources are available in {@code world}</li>
     *   <li>Game state remains valid for the next processing frame</li>
     * </ul>
     *
     * @param gameData current game state and configuration
     * @param world mutable world containing all active entities
     */
    void start(GameData gameData, World world);

    /**
     * Stops the plugin and removes plugin-managed entities/resources.
     *
     * <p>Preconditions:</p>
     * <ul>
     *   <li>{@code gameData != null}</li>
     *   <li>{@code world != null}</li>
     *   <li>Plugin has previously been started</li>
     * </ul>
     *
     * <p>Postconditions:</p>
     * <ul>
     *   <li>Entities/resources created and owned by this plugin are removed or released</li>
     *   <li>No plugin-owned state remains that can affect subsequent runs</li>
     * </ul>
     *
     * @param gameData current game state and configuration
     * @param world mutable world containing all active entities
     */
    void stop(GameData gameData, World world);
}
