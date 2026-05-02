package dk.sdu.mmmi.cbse.enemysystem;

import dk.sdu.mmmi.cbse.common.bullet.BulletSPI;
import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IEntityProcessingService;

import java.util.Collection;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.Collectors.toList;

public class EnemyControlSystem implements IEntityProcessingService {

    private static final int SHOOT_COOLDOWN_FRAMES = 45;
    private final Map<String, Integer> cooldownByEnemyId = new ConcurrentHashMap<>();

    @Override
    public void process(GameData gameData, World world) {
        for (Entity enemy : world.getEntities(Enemy.class)) {
            Entity target = getTarget(world, enemy);
            if (target != null) {
                double targetAngle = Math.toDegrees(Math.atan2(target.getY() - enemy.getY(), target.getX() - enemy.getX()));
                enemy.setRotation(enemy.getRotation() + normalizeAngle(targetAngle - enemy.getRotation()) * 0.05);
            }

            double speed = 0.7;
            enemy.setX(enemy.getX() + Math.cos(Math.toRadians(enemy.getRotation())) * speed);
            enemy.setY(enemy.getY() + Math.sin(Math.toRadians(enemy.getRotation())) * speed);

            int cooldown = cooldownByEnemyId.getOrDefault(enemy.getID(), 0);
            if (cooldown <= 0 && target != null) {
                getBulletSPIs().stream().findFirst().ifPresent(spi -> world.addEntity(spi.createBullet(enemy, gameData)));
                cooldownByEnemyId.put(enemy.getID(), SHOOT_COOLDOWN_FRAMES);
            } else {
                cooldownByEnemyId.put(enemy.getID(), Math.max(0, cooldown - 1));
            }
        }

        cooldownByEnemyId.keySet().removeIf(id -> world.getEntity(id) == null);
    }

    private Collection<? extends BulletSPI> getBulletSPIs() {
        return ServiceLoader.load(BulletSPI.class).stream().map(ServiceLoader.Provider::get).collect(toList());
    }

    private Entity getTarget(World world, Entity enemy) {
        Entity bestTarget = null;
        double bestDistance = Double.MAX_VALUE;

        for (Entity candidate : world.getEntities()) {
            if (candidate.getID().equals(enemy.getID()) || candidate instanceof Enemy) {
                continue;
            }

            double dx = candidate.getX() - enemy.getX();
            double dy = candidate.getY() - enemy.getY();
            double distSq = dx * dx + dy * dy;
            if (distSq < bestDistance) {
                bestDistance = distSq;
                bestTarget = candidate;
            }
        }

        return bestTarget;
    }

    private double normalizeAngle(double angle) {
        double normalized = angle % 360;
        if (normalized > 180) {
            return normalized - 360;
        }
        if (normalized < -180) {
            return normalized + 360;
        }
        return normalized;
    }
}
