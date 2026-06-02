package dk.sdu.mmmi.cbse.asteroid;

import dk.sdu.mmmi.cbse.common.asteroids.Asteroid;
import dk.sdu.mmmi.cbse.common.asteroids.IAsteroidSplitter;
import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IEntityProcessingService;

import java.util.Random;

public class AsteroidProcessor implements IEntityProcessingService {

    private static final int STARTING_ASTEROID_COUNT = 4;
    private static final int ASTEROID_RESPAWN_DELAY_FRAMES = 120;
    private IAsteroidSplitter asteroidSplitter = new AsteroidSplitterImpl();
    private final Random random = new Random();
    private int respawnCountdown = -1;

    @Override
    public void process(GameData gameData, World world) {
        int asteroidCount = 0;

        for (Entity asteroid : world.getEntities()) {
            if (!(asteroid instanceof Asteroid)) continue;
            asteroidCount++;

            // Random walk: nudge heading a little each frame for less predictable movement.
            asteroid.setRotation(asteroid.getRotation() + (random.nextDouble() - 0.5) * 6);
            double speed = 0.35 + random.nextDouble() * 0.35;

            double changeX = Math.cos(Math.toRadians(asteroid.getRotation()));
            double changeY = Math.sin(Math.toRadians(asteroid.getRotation()));

            asteroid.setX(asteroid.getX() + changeX * speed);
            asteroid.setY(asteroid.getY() + changeY * speed);

            if (asteroid.getX() < 0) {
                asteroid.setX(asteroid.getX() + gameData.getDisplayWidth());
            }

            if (asteroid.getX() > gameData.getDisplayWidth()) {
                asteroid.setX(asteroid.getX() % gameData.getDisplayWidth());
            }

            if (asteroid.getY() < 0) {
                asteroid.setY(asteroid.getY() + gameData.getDisplayHeight());
            }

            if (asteroid.getY() > gameData.getDisplayHeight()) {
                asteroid.setY(asteroid.getY() % gameData.getDisplayHeight());
            }

        }

        if (asteroidCount == 0) {
            if (respawnCountdown < 0) {
                respawnCountdown = ASTEROID_RESPAWN_DELAY_FRAMES;
            }

            if (respawnCountdown > 0) {
                respawnCountdown--;
                return;
            }

            for (int i = 0; i < STARTING_ASTEROID_COUNT; i++) {
                world.addEntity(AsteroidPlugin.createAsteroid(gameData));
            }
            respawnCountdown = -1;
        } else {
            respawnCountdown = -1;
        }

    }

    /**
     * Dependency Injection using OSGi Declarative Services
     */
    public void setAsteroidSplitter(IAsteroidSplitter asteroidSplitter) {
        this.asteroidSplitter = asteroidSplitter;
    }

    public void removeAsteroidSplitter(IAsteroidSplitter asteroidSplitter) {
        this.asteroidSplitter = null;
    }


}
