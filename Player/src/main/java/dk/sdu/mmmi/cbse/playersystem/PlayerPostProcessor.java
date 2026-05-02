package dk.sdu.mmmi.cbse.playersystem;

import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;

public class PlayerPostProcessor implements IPostEntityProcessingService {

    @Override
    public void process(GameData gameData, World world) {
        for (Entity player : world.getEntities(Player.class)) {
            if (player.getX() < 0) {
                player.setX(player.getX() + gameData.getDisplayWidth());
            }
            if (player.getX() > gameData.getDisplayWidth()) {
                player.setX(player.getX() - gameData.getDisplayWidth());
            }
            if (player.getY() < 0) {
                player.setY(player.getY() + gameData.getDisplayHeight());
            }
            if (player.getY() > gameData.getDisplayHeight()) {
                player.setY(player.getY() - gameData.getDisplayHeight());
            }

            if (player.getRotation() >= 360 || player.getRotation() <= -360) {
                player.setRotation(player.getRotation() % 360);
            }
        }
    }
}
