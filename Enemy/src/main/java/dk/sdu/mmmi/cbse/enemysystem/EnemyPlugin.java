package dk.sdu.mmmi.cbse.enemysystem;

import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IGamePluginService;

import java.util.concurrent.ThreadLocalRandom;

public class EnemyPlugin implements IGamePluginService {

    @Override
    public void start(GameData gameData, World world) {
        world.addEntity(createEnemy(gameData));
        gameData.setEnemyHealth(GameData.DEFAULT_ENEMY_HIT_POINTS);
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
        enemy.setRadius(10);
        double minX = enemy.getRadius();
        double maxX = Math.max(minX, gameData.getDisplayWidth() - enemy.getRadius());
        double minY = enemy.getRadius();
        double maxY = Math.max(minY, gameData.getDisplayHeight() - enemy.getRadius());
        enemy.setX(ThreadLocalRandom.current().nextDouble(minX, maxX + 1));
        enemy.setY(ThreadLocalRandom.current().nextDouble(minY, maxY + 1));
        enemy.setRotation(15);
        return enemy;
    }
}
