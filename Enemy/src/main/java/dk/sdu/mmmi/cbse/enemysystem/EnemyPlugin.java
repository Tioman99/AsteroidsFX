package dk.sdu.mmmi.cbse.enemysystem;

import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IGamePluginService;

public class EnemyPlugin implements IGamePluginService {

    @Override
    public void start(GameData gameData, World world) {
        world.addEntity(createEnemy(gameData));
    }

    @Override
    public void stop(GameData gameData, World world) {
        for (Entity enemy : world.getEntities(Enemy.class)) {
            world.removeEntity(enemy);
        }
    }

    static Enemy createEnemy(GameData gameData) {
        Enemy enemy = new Enemy();
        enemy.setPolygonCoordinates(-8, -6, 8, -6, 10, 0, 8, 6, -8, 6, -10, 0);
        enemy.setX(gameData.getDisplayWidth() * 0.2);
        enemy.setY(gameData.getDisplayHeight() * 0.2);
        enemy.setRadius(10);
        enemy.setRotation(15);
        return enemy;
    }
}
