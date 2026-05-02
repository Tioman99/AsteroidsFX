package dk.sdu.mmmi.cbse.common.services;

import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;

/**
 * Per-frame processing contract for active gameplay systems.
 * Implementations typically update movement, input responses, and runtime behavior.
 */
public interface IEntityProcessingService {

    /**
     * Executes one frame of entity processing.
     *
     * <p>Preconditions:</p>
     * <ul>
     *   <li>{@code gameData != null}</li>
     *   <li>{@code world != null}</li>
     *   <li>World entities are in a consistent state from the previous frame</li>
     * </ul>
     *
     * <p>Postconditions:</p>
     * <ul>
     *   <li>Relevant entities may have updated position, rotation, velocity, or state</li>
     *   <li>World remains in a consistent state for post-processing</li>
     *   <li>Method is side-effect free outside of {@code world} and system-owned state</li>
     * </ul>
     *
     * @param gameData current game state and frame input
     * @param world mutable world containing all active entities
     */
    void process(GameData gameData, World world);
}
