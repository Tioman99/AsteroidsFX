package dk.sdu.mmmi.cbse.common.services;

import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;

/**
 * Per-frame post-processing contract executed after all entity processors.
 * Suitable for collision handling, cleanup, and cross-entity reconciliation.
 */
public interface IPostEntityProcessingService {

    /**
     * Executes one frame of post-processing.
     *
     * <p>Preconditions:</p>
     * <ul>
     *   <li>{@code gameData != null}</li>
     *   <li>{@code world != null}</li>
     *   <li>All {@code IEntityProcessingService} processors have already completed for the frame</li>
     * </ul>
     *
     * <p>Postconditions:</p>
     * <ul>
     *   <li>Cross-entity consistency rules are enforced (for example collisions or cleanup)</li>
     *   <li>World is ready for rendering and the next frame</li>
     * </ul>
     *
     * @param gameData current game state and frame input
     * @param world mutable world containing all active entities
     */
    void process(GameData gameData, World world);
}
