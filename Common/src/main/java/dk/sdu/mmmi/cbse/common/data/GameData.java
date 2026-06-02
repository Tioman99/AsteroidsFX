package dk.sdu.mmmi.cbse.common.data;

public class GameData {

    public static final int DEFAULT_PLAYER_HIT_POINTS = 3;
    public static final int DEFAULT_ENEMY_HIT_POINTS = 3;

    private int displayWidth  = 800 ;
    private int displayHeight = 800;
    private final GameKeys keys = new GameKeys();
    private boolean playerDead = false;
    private int asteroidsDestroyed = 0;
    private int playerHealth = DEFAULT_PLAYER_HIT_POINTS;
    private int enemyHealth = DEFAULT_ENEMY_HIT_POINTS;


    public GameKeys getKeys() {
        return keys;
    }

    public void setDisplayWidth(int width) {
        this.displayWidth = width;
    }

    public int getDisplayWidth() {
        return displayWidth;
    }

    public void setDisplayHeight(int height) {
        this.displayHeight = height;
    }

    public int getDisplayHeight() {
        return displayHeight;
    }

    public boolean isPlayerDead() {
        return playerDead;
    }

    public void setPlayerDead(boolean playerDead) {
        this.playerDead = playerDead;
    }

    public int getAsteroidsDestroyed() {
        return asteroidsDestroyed;
    }

    public void incrementAsteroidsDestroyed() {
        this.asteroidsDestroyed++;
    }

    public int getPlayerHealth() {
        return playerHealth;
    }

    public void setPlayerHealth(int playerHealth) {
        this.playerHealth = Math.max(0, playerHealth);
    }

    public int getEnemyHealth() {
        return enemyHealth;
    }

    public void setEnemyHealth(int enemyHealth) {
        this.enemyHealth = Math.max(0, enemyHealth);
    }

    public void resetRoundState() {
        playerDead = false;
        asteroidsDestroyed = 0;
        playerHealth = DEFAULT_PLAYER_HIT_POINTS;
        enemyHealth = DEFAULT_ENEMY_HIT_POINTS;
    }

}
