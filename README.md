Build the project from the root folder with the following Maven command:
mvn clean install

Run the project from root folder with following Maven command :
mvn exec:exec

Dynamic plugin loading

The game supports runtime plugin reload from the plugins folder.

Dynamic gameplay modules (loaded from plugins):
- Player
- Enemy
- Bullet (Weapon)
- Asteroids

Static modules (loaded from mods-mvn at startup):
- Core
- Common
- CommonBullet
- CommonAsteroids
- Collision
- JavaFX and Spring dependencies

Where to add and remove plugin JARs
- Add or remove plugin JARs in the root-level plugins folder.
- The running game watches this folder and reloads dynamic modules automatically.

Hot-swap workflow
1. Build from project root: mvn clean install
2. Start the game: mvn exec:exec
3. While the game is running, copy in or delete one of these JARs in plugins:
	- Player-1.0.1-SNAPSHOT.jar
	- Enemy-1.0.1-SNAPSHOT.jar
	- Bullet-1.0.1-SNAPSHOT.jar
	- Asteroids-1.0.1-SNAPSHOT.jar
4. The plugin manager detects the change, unloads old dynamic plugins, and loads the updated set.

Notes
- Do not keep the same dynamic module JAR in both mods-mvn and plugins.
- If a module is on the boot module-path (mods-mvn), it is treated as static and will not be dynamically reloaded.
