/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package dk.sdu.mmmi.cbse.main;

import dk.sdu.mmmi.cbse.common.data.Entity;
import dk.sdu.mmmi.cbse.common.data.GameData;
import dk.sdu.mmmi.cbse.common.data.GameKeys;
import dk.sdu.mmmi.cbse.common.data.World;
import dk.sdu.mmmi.cbse.common.services.IEntityProcessingService;
import dk.sdu.mmmi.cbse.common.services.IGamePluginService;
import dk.sdu.mmmi.cbse.common.services.IPostEntityProcessingService;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.Stage;

/**
 *
 * @author jcs
 */
class Game {

    private final GameData gameData = new GameData();
    private final World world = new World();
    private final Map<Entity, Polygon> polygons = new ConcurrentHashMap<>();
    private final Pane gameWindow = new Pane();
    private final PluginManager pluginManager;

    private StackPane root;
    private StackPane pauseOverlay;
    private StackPane deathOverlay;
    private boolean paused = false;
    private boolean gameOver = false;
    private Text asteroidsDestroyedText;
    private Text playerHealthText;
    private Text enemyHealthText;
    private int displayedAsteroidsDestroyed = Integer.MIN_VALUE;
    private int displayedPlayerHealth = Integer.MIN_VALUE;
    private int displayedEnemyHealth = Integer.MIN_VALUE;
    private AnimationTimer gameLoop;

    Game(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    public void start(Stage window) throws Exception {
        gameData.resetRoundState();

        asteroidsDestroyedText = new Text(10, 20, "Destroyed asteroids: 0");
        asteroidsDestroyedText.setFill(Color.WHITE);
        playerHealthText = new Text(10, 42, "Player health: " + GameData.DEFAULT_PLAYER_HIT_POINTS);
        playerHealthText.setFill(Color.WHITE);
        enemyHealthText = new Text(10, 64, "Enemy health: " + GameData.DEFAULT_ENEMY_HIT_POINTS);
        enemyHealthText.setFill(Color.WHITE);

        gameWindow.setPrefSize(gameData.getDisplayWidth(), gameData.getDisplayHeight());
        gameWindow.setStyle("-fx-background-color: black;");
        gameWindow.getChildren().addAll(asteroidsDestroyedText, playerHealthText, enemyHealthText);

        pauseOverlay = buildPauseOverlay();
        deathOverlay = buildDeathOverlay();

        root = new StackPane(gameWindow, pauseOverlay, deathOverlay);

        Scene scene = new Scene(root, gameData.getDisplayWidth(), gameData.getDisplayHeight());
        scene.setFill(Color.BLACK);

        scene.setOnKeyPressed(event -> {
            if (event.getCode().equals(KeyCode.LEFT)) {
                gameData.getKeys().setKey(GameKeys.LEFT, true);
            }
            if (event.getCode().equals(KeyCode.RIGHT)) {
                gameData.getKeys().setKey(GameKeys.RIGHT, true);
            }
            if (event.getCode().equals(KeyCode.UP)) {
                gameData.getKeys().setKey(GameKeys.UP, true);
            }
            if (event.getCode().equals(KeyCode.SPACE)) {
                gameData.getKeys().setKey(GameKeys.SPACE, true);
            }
            if (event.getCode().equals(KeyCode.ESCAPE) && !gameOver) {
                togglePause();
            }
        });

        scene.setOnKeyReleased(event -> {
            if (event.getCode().equals(KeyCode.LEFT)) {
                gameData.getKeys().setKey(GameKeys.LEFT, false);
            }
            if (event.getCode().equals(KeyCode.RIGHT)) {
                gameData.getKeys().setKey(GameKeys.RIGHT, false);
            }
            if (event.getCode().equals(KeyCode.UP)) {
                gameData.getKeys().setKey(GameKeys.UP, false);
            }
            if (event.getCode().equals(KeyCode.SPACE)) {
                gameData.getKeys().setKey(GameKeys.SPACE, false);
            }
        });

        // Lookup all Game Plugins using ServiceLoader
        for (IGamePluginService iGamePlugin : pluginManager.getAllPluginServices()) {
            iGamePlugin.start(gameData, world);
        }
        for (Entity entity : world.getEntities()) {
            Polygon polygon = new Polygon(entity.getPolygonCoordinates());
            polygon.setStroke(Color.WHITE);
            polygon.setFill(Color.TRANSPARENT);
            polygons.put(entity, polygon);
            gameWindow.getChildren().add(polygon);
        }

        window.setScene(scene);
        window.setTitle("ASTEROIDS");
        window.show();

        pluginManager.startWatcher();
    }

    private StackPane buildPauseOverlay() {
        StackPane overlay = new StackPane();
        overlay.setVisible(false);

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(overlay.widthProperty());
        bg.heightProperty().bind(overlay.heightProperty());
        bg.setFill(Color.color(0, 0, 0, 0.70));

        Text title = new Text("PAUSED");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        title.setFill(Color.WHITE);

        Button resumeBtn = styledButton("RESUME");
        resumeBtn.setOnAction(e -> togglePause());

        Button quitBtn = styledButton("QUIT");
        quitBtn.setOnAction(e -> quit());

        VBox menu = new VBox(18, title, resumeBtn, quitBtn);
        menu.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(bg, menu);
        return overlay;
    }

    private StackPane buildDeathOverlay() {
        StackPane overlay = new StackPane();
        overlay.setVisible(false);

        Rectangle bg = new Rectangle();
        bg.widthProperty().bind(overlay.widthProperty());
        bg.heightProperty().bind(overlay.heightProperty());
        bg.setFill(Color.color(0, 0, 0, 0.78));

        Text title = new Text("GAME OVER");
        title.setFont(Font.font("Arial", FontWeight.BOLD, 60));
        title.setFill(Color.web("#ff4444"));

        Button playBtn = styledButton("PLAY AGAIN");
        playBtn.setOnAction(e -> restartGame());

        Button quitBtn = styledButton("QUIT");
        quitBtn.setOnAction(e -> quit());

        VBox menu = new VBox(18, title, playBtn, quitBtn);
        menu.setAlignment(Pos.CENTER);

        overlay.getChildren().addAll(bg, menu);
        return overlay;
    }

    private Button styledButton(String label) {
        Button btn = new Button(label);
        btn.setPrefSize(180, 48);
        btn.setFont(Font.font("Arial", FontWeight.BOLD, 16));
        String base = "-fx-background-color: transparent;"
                + "-fx-text-fill: white;"
                + "-fx-border-color: white;"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-cursor: hand;";
        String hover = "-fx-background-color: rgba(255,255,255,0.12);"
                + "-fx-text-fill: white;"
                + "-fx-border-color: white;"
                + "-fx-border-width: 2;"
                + "-fx-border-radius: 4;"
                + "-fx-background-radius: 4;"
                + "-fx-cursor: hand;";
        btn.setStyle(base);
        btn.setOnMouseEntered(e -> btn.setStyle(hover));
        btn.setOnMouseExited(e -> btn.setStyle(base));
        return btn;
    }

    private void quit() {
        if (gameLoop != null) {
            gameLoop.stop();
        }
        Platform.exit();
    }

    private void togglePause() {
        paused = !paused;
        pauseOverlay.setVisible(paused);
    }

    private void restartGame() {
        gameOver = false;
        paused = false;
        gameData.resetRoundState();

        // Discard any plugin-change signals that arrived during the game-over screen
        // to avoid a spurious reload immediately after restart.
        pluginManager.clearPendingChanges();

        for (IGamePluginService plugin : pluginManager.getAllPluginServices()) {
            plugin.stop(gameData, world);
        }
        for (Entity e : new ArrayList<>(world.getEntities())) {
            world.removeEntity(e);
        }
        for (Polygon p : polygons.values()) {
            gameWindow.getChildren().remove(p);
        }
        polygons.clear();

        for (IGamePluginService plugin : pluginManager.getAllPluginServices()) {
            plugin.start(gameData, world);
        }
        for (Entity entity : world.getEntities()) {
            Polygon polygon = new Polygon(entity.getPolygonCoordinates());
            polygon.setStroke(Color.WHITE);
            polygon.setFill(Color.TRANSPARENT);
            polygons.put(entity, polygon);
            gameWindow.getChildren().add(polygon);
        }

        deathOverlay.setVisible(false);
    }

    public void render() {
        gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (!paused && !gameOver) {
                    update();
                    gameData.getKeys().update();
                }
                draw();
            }
        };
        gameLoop.start();
    }

    private void update() {
        pluginManager.checkForChanges(gameData, world);
        for (IEntityProcessingService entityProcessorService : pluginManager.getAllEntityProcessors()) {
            entityProcessorService.process(gameData, world);
        }
        for (IPostEntityProcessingService postEntityProcessorService : pluginManager.getAllPostProcessors()) {
            postEntityProcessorService.process(gameData, world);
        }
    }

    private void draw() {
        int asteroidsDestroyed = gameData.getAsteroidsDestroyed();
        if (asteroidsDestroyed != displayedAsteroidsDestroyed) {
            displayedAsteroidsDestroyed = asteroidsDestroyed;
            asteroidsDestroyedText.setText("Destroyed asteroids: " + asteroidsDestroyed);
        }

        int playerHealth = gameData.getPlayerHealth();
        if (playerHealth != displayedPlayerHealth) {
            displayedPlayerHealth = playerHealth;
            playerHealthText.setText("Player health: " + playerHealth);
        }

        int enemyHealth = gameData.getEnemyHealth();
        if (enemyHealth != displayedEnemyHealth) {
            displayedEnemyHealth = enemyHealth;
            enemyHealthText.setText("Enemy health: " + enemyHealth);
        }

        if (gameData.isPlayerDead() && !gameOver) {
            gameOver = true;
            deathOverlay.setVisible(true);
        }

        for (Entity polygonEntity : polygons.keySet()) {
            if (!world.getEntities().contains(polygonEntity)) {
                Polygon removedPolygon = polygons.get(polygonEntity);
                polygons.remove(polygonEntity);
                gameWindow.getChildren().remove(removedPolygon);
            }
        }

        for (Entity entity : world.getEntities()) {
            Polygon polygon = polygons.get(entity);
            if (polygon == null) {
                polygon = new Polygon(entity.getPolygonCoordinates());
                polygon.setStroke(Color.WHITE);
                polygon.setFill(Color.TRANSPARENT);
                polygons.put(entity, polygon);
                gameWindow.getChildren().add(polygon);
            }
            polygon.setTranslateX(entity.getX());
            polygon.setTranslateY(entity.getY());
            polygon.setRotate(entity.getRotation());
        }
    }

}

