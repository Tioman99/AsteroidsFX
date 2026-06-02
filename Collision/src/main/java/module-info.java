import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;
import dk.sdu.mmmi.cbse.common.asteroids.IAsteroidSplitter;

module Collision {
    requires Common;
    requires CommonAsteroids;
    requires CommonBullet;
    uses IAsteroidSplitter;
    provides IPostEntityProcessingService with dk.sdu.mmmi.cbse.collisionsystem.CollisionDetector;
}