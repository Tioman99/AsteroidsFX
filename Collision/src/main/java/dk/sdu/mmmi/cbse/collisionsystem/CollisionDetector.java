package dk.sdu.mmmi.cbse.collisionsystem;

import dk.sdu.mmmi.cbse.common.asteroids.Asteroid;
import dk.sdu.mmmi.cbse.common.asteroids.IAsteroidSplitter;
import dk.sdu.mmmi.cbse.common.bullet.Bullet;
import dk.sdu.mmmi.cbse.common.bullet.EnemyBullet;
import dk.sdu.mmmi.cbse.common.bullet.PlayerBullet;
import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;
import dk.sdu.mmmi.cbse.enemysystem.Enemy;
import dk.sdu.mmmi.cbse.playersystem.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.ServiceLoader;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class CollisionDetector implements IPostEntityProcessingService {

    private static final int SHIP_HIT_POINTS = 3;
    private static final float MIN_SPLIT_RADIUS = 6f;
    private final ConcurrentMap<String, Integer> shipHits = new ConcurrentHashMap<>();

    public CollisionDetector() {
    }

    @Override
    public void process(GameData gameData, World world) {
        List<Entity> entities = new ArrayList<>(world.getEntities());
        Set<String> removedThisFrame = new HashSet<>();

        for (int i = 0; i < entities.size(); i++) {
            Entity entity1 = entities.get(i);
            if (isRemoved(world, removedThisFrame, entity1)) {
                continue;
            }

            for (int j = i + 1; j < entities.size(); j++) {
                Entity entity2 = entities.get(j);
                if (isRemoved(world, removedThisFrame, entity2)) {
                    continue;
                }

                if (collides(entity1, entity2)) {
                    handleCollision(gameData, world, entity1, entity2, removedThisFrame);
                }
            }
        }

        shipHits.keySet().removeIf(id -> world.getEntity(id) == null);
    }

    private void handleCollision(GameData gameData, World world, Entity entity1, Entity entity2, Set<String> removedThisFrame) {
        Entity bullet = getBullet(entity1, entity2);
        Entity asteroid = getAsteroid(entity1, entity2);
        Entity ship = getShip(entity1, entity2);

        if (bullet != null && asteroid != null) {
            remove(world, removedThisFrame, bullet);
            remove(world, removedThisFrame, asteroid);

            if (asteroid.getRadius() / 2f >= MIN_SPLIT_RADIUS) {
                splitAsteroid(asteroid, world);
            }
            return;
        }

        if (ship != null && asteroid != null) {
            if (ship instanceof Player) {
                gameData.setPlayerDead(true);
            }
            remove(world, removedThisFrame, ship);
            remove(world, removedThisFrame, asteroid);
            return;
        }

        if (bullet != null && ship != null && isOpposingBulletHit(bullet, ship)) {
            remove(world, removedThisFrame, bullet);

            int hits = shipHits.getOrDefault(ship.getID(), 0) + 1;
            if (hits >= SHIP_HIT_POINTS) {
                if (ship instanceof Player) {
                    gameData.setPlayerDead(true);
                }
                remove(world, removedThisFrame, ship);
                shipHits.remove(ship.getID());
            } else {
                shipHits.put(ship.getID(), hits);
            }
        }
    }

    private boolean isRemoved(World world, Set<String> removedThisFrame, Entity entity) {
        return removedThisFrame.contains(entity.getID()) || world.getEntity(entity.getID()) == null;
    }

    private void remove(World world, Set<String> removedThisFrame, Entity entity) {
        if (!removedThisFrame.contains(entity.getID())) {
            world.removeEntity(entity);
            removedThisFrame.add(entity.getID());
        }
    }

    private Entity getBullet(Entity entity1, Entity entity2) {
        if (entity1 instanceof Bullet) {
            return entity1;
        }
        if (entity2 instanceof Bullet) {
            return entity2;
        }
        return null;
    }

    private Entity getAsteroid(Entity entity1, Entity entity2) {
        if (entity1 instanceof Asteroid) {
            return entity1;
        }
        if (entity2 instanceof Asteroid) {
            return entity2;
        }
        return null;
    }

    private Entity getShip(Entity entity1, Entity entity2) {
        if (entity1 instanceof Player || entity1 instanceof Enemy) {
            return entity1;
        }
        if (entity2 instanceof Player || entity2 instanceof Enemy) {
            return entity2;
        }
        return null;
    }

    private boolean isOpposingBulletHit(Entity bullet, Entity ship) {
        return (bullet instanceof PlayerBullet && ship instanceof Enemy)
                || (bullet instanceof EnemyBullet && ship instanceof Player);
    }

    private void splitAsteroid(Entity asteroid, World world) {
        for (IAsteroidSplitter splitter : ServiceLoader.load(IAsteroidSplitter.class)) {
            splitter.createSplitAsteroid(asteroid, world);
            break;
        }
    }

    public Boolean collides(Entity entity1, Entity entity2) {
        float dx = (float) entity1.getX() - (float) entity2.getX();
        float dy = (float) entity1.getY() - (float) entity2.getY();
        float distance = (float) Math.sqrt(dx * dx + dy * dy);
        return distance < (entity1.getRadius() + entity2.getRadius());
    }

}

