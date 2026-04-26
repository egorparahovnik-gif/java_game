package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
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

    @Override
    public void create() {
        batch = new SpriteBatch();
        shapeRenderer = new ShapeRenderer();
        titleFont = new BitmapFont();
        bodyFont = new BitmapFont();

        titleFont.getData().setScale(2.2f);
        bodyFont.getData().setScale(1.2f);

        camera = new OrthographicCamera();
        viewport = new ExtendViewport(960, 720, camera);
        viewport.apply();

        assets = new GameAssets();
        assets.load();

        world = new GameWorld();
        world.reset();

        controller = new PlayerController();
        renderer = new WorldRenderer();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        float dt = Gdx.graphics.getDeltaTime();
        controller.update(world, dt);
        world.update(dt);

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
    }

    @Override
    public void dispose() {
        if (assets != null) assets.dispose();
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        bodyFont.dispose();
    }
}
