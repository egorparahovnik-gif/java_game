package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygame.survival.game.GameWorld;
import com.mygame.survival.gfx.GameAssets;
import com.mygame.survival.gfx.WorldRenderer;
import com.mygame.survival.input.PlayerController;
import com.mygame.survival.ui.MenuScreen;

public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private OrthographicCamera camera;
    private Viewport viewport;
    private GameAssets assets;
    private GameWorld world;
    private PlayerController controller;
    private WorldRenderer renderer;

    // ── Menu ──────────────────────────────────────────────────────────────
    private MenuScreen menuScreen;
    private GameState  gameState = GameState.MENU;
    private boolean restartOnExit = false;

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        titleFont = new BitmapFont();
        bodyFont  = new BitmapFont();

        titleFont.getData().setScale(2.2f);
        bodyFont.getData().setScale(1.2f);

        camera   = new OrthographicCamera();
        viewport = new ExtendViewport(872, 654, camera);
        viewport.apply();

        assets = new GameAssets();
        assets.load();

        world = new GameWorld();
        world.reset();

        controller = new PlayerController();
        renderer   = new WorldRenderer();

        menuScreen = new MenuScreen();
        menuScreen.create();
        menuScreen.resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        float dt = Gdx.graphics.getDeltaTime();

        // cursor visible only in menu
        if (gameState == GameState.MENU) {
            Gdx.input.setCursorCatched(false);
        } else {
            Gdx.input.setCursorCatched(true);
        }
        if (gameState == GameState.MENU) {
            // draw last frame of the game (frozen)
            renderGame(0f);
            renderMenu(dt);
        } else {
            renderGame(dt);
        }
    }

    // ── Menu render ───────────────────────────────────────────────────────

    private void renderMenu(float dt) {
        // Render the game world in the background (paused – no update).
        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);
        renderer.render(batch, shapeRenderer, camera, viewport, assets, world);

        // Draw menu UI on top.
        boolean playPressed = menuScreen.update();
        menuScreen.render(batch);

        if (playPressed) {
            if (restartOnExit) {
                // start new game only after death
                world = new GameWorld();
                world.reset();
            }
            // otherwise just resume

            gameState = GameState.PLAYING;
        }
    }

    // ── Game render ───────────────────────────────────────────────────────

    private void renderGame(float dt) {
        // ESC → open menu
        if (dt > 0 && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            restartOnExit = false; // just pause, not restart
            gameState = GameState.MENU;
            return;
        }

        controller.update(world, dt);
        world.update(dt);

        // If game is over, freeze world and open menu (do NOT reset here)
        if (world.gameOver) {
            restartOnExit = true; // next exit from menu should restart game
            gameState = GameState.MENU;
            return;
        }

        renderer.updateCamera(camera, viewport, world);

        viewport.apply();
        camera.update();

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        renderer.render(batch, shapeRenderer, camera, viewport, assets, world);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
        menuScreen.resize(width, height);
    }

    @Override
    public void dispose() {
        if (assets     != null) assets.dispose();
        if (menuScreen != null) menuScreen.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        bodyFont.dispose();
    }
}
