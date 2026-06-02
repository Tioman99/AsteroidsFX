package dk.sdu.mmmi.cbse.asteroid;

import dk.sdu.mmmi.cbse.common.asteroids.Asteroid;
import dk.sdu.mmmi.cbse.common.asteroids.IAsteroidSplitter;
import dk.sdu.mmmi.cbse.common.asteroids.SubAsteroid;
import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.World;

import java.util.Random;

public class AsteroidSplitterImpl implements IAsteroidSplitter {

    private static final int MAX_ASTEROID_BODIES = 6;

    @Override
    public void createSplitAsteroid(Entity e, World world) {
        float subSize = e.getRadius() / 2f;

        // Spawn 2 subasteroids flying off in different directions
        SubAsteroid sub1 = createSubAsteroid(subSize, e.getX(), e.getY(), e.getRotation() + 45);
        SubAsteroid sub2 = createSubAsteroid(subSize, e.getX(), e.getY(), e.getRotation() - 45);
        world.addEntity(sub1);
        world.addEntity(sub2);

        // Count all asteroid bodies now in world (asteroid is already removed)
        int totalBodies = world.getEntities(Asteroid.class).size()
                + world.getEntities(SubAsteroid.class).size();

        if (totalBodies < MAX_ASTEROID_BODIES) {
            world.addEntity(AsteroidPlugin.createAsteroid(null));
        }
    }

    private SubAsteroid createSubAsteroid(float size, double x, double y, double rotation) {
        SubAsteroid sub = new SubAsteroid();
        sub.setPolygonCoordinates(
                0, size,
                size, size / 3,
                size * 2 / 3, -size,
                -size * 2 / 3, -size,
                -size, size / 3
        );
        sub.setX(x);
        sub.setY(y);
        sub.setRadius(size);
        sub.setRotation(rotation);
        return sub;
    }
}

