package dk.sdu.mmmi.cbse.asteroid;

import dk.sdu.mmmi.cbse.common.asteroids.Asteroid;
import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IGamePluginService;
import java.util.Random;

public class AsteroidPlugin implements IGamePluginService {

    private static final int STARTING_ASTEROID_COUNT = 4;
    private static final Random RANDOM = new Random();

    @Override
    public void start(GameData gameData, World world) {
        for (int i = 0; i < STARTING_ASTEROID_COUNT; i++) {
            world.addEntity(createAsteroid(gameData));
        }
    }

    @Override
    public void stop(GameData gameData, World world) {
        // Remove entities
        for (Entity asteroid : world.getEntities(Asteroid.class)) {
            world.removeEntity(asteroid);
        }
    }

    public static Entity createAsteroid(GameData gameData) {
        Entity asteroid = new Asteroid();
        int size = (RANDOM.nextInt(10) + 5) * 2;
        asteroid.setPolygonCoordinates(
                0, size,
                size, size / 3,
                size * 2 / 3, -size,
                -size * 2 / 3, -size,
                -size, size / 3
        );
        if (gameData != null) {
            asteroid.setX(RANDOM.nextDouble() * gameData.getDisplayWidth());
            asteroid.setY(RANDOM.nextDouble() * gameData.getDisplayHeight());
        } else {
            asteroid.setX(0);
            asteroid.setY(0);
        }
        asteroid.setRadius(size);
        asteroid.setRotation(RANDOM.nextInt(360));
        return asteroid;
    }
}
