package dk.sdu.mmmi.cbse.enemysystem;

import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;

public class EnemyPostProcessor implements IPostEntityProcessingService {

    @Override
    public void process(GameData gameData, World world) {
        int enemyCount = 0;
        for (Entity enemy : world.getEntities(Enemy.class)) {
            enemyCount++;

            if (enemy.getX() < 0) {
                enemy.setX(enemy.getX() + gameData.getDisplayWidth());
            }
            if (enemy.getX() > gameData.getDisplayWidth()) {
                enemy.setX(enemy.getX() - gameData.getDisplayWidth());
            }
            if (enemy.getY() < 0) {
                enemy.setY(enemy.getY() + gameData.getDisplayHeight());
            }
            if (enemy.getY() > gameData.getDisplayHeight()) {
                enemy.setY(enemy.getY() - gameData.getDisplayHeight());
            }
        }

        if (enemyCount == 0) {
            world.addEntity(EnemyPlugin.createEnemy(gameData));
        }
    }
}
