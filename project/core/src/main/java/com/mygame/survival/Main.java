package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {

    private SpriteBatch batch;
    private ShapeRenderer shapeRenderer;
    private BitmapFont titleFont;
    private BitmapFont bodyFont;
    private OrthographicCamera camera;
    private Viewport viewport;
    private final GameUi ui = new GameUi();

    private Texture walkSheet;
    private Texture fireSheet;
    private Texture treeTexture;
    private Texture bgTexture;
    private Texture branchTexture;
    private Texture fireOffTexture;
    private Texture grassTexture;
    private Animation<TextureRegion> walkAnimation;
    private Animation<TextureRegion> fireAnimation;
    private Animation<TextureRegion> fireOffAnimation;
    private TextureRegion[] walkFrames;
    private TextureRegion[] fireFrames;
    private TextureRegion[] fireOffFrames;

    private float playerX, playerY;
    private float stateTime;
    private float fireStateTime;
    private boolean isMoving;
    private boolean facingRight = true;

    private float fireHealth = 0f;
    private int woodCount = 0;
    private int carriedWood = 0;
    private final int maxWood = 5;
    private float branchRespawnTimer = 0f;
    private final float branchRespawnTime = 60f;
    private boolean isGameOver = false;
    private boolean fireHasBeenLit = false;

    private boolean showMenu = true;
    private boolean hasActiveRun = false;
    private boolean continueEnabled = false;

    private enum ScreenState {
        TITLE_MENU,
        PLAYING,
        GAME_OVER
    }

    private ScreenState screenState = ScreenState.TITLE_MENU;

    private Array<Tree> trees;
    private Array<Branch> branches;
    private Array<Grass> grasses;

    private final int tileSize = 16;
    private final int mapWidth = 200;
    private final int mapHeight = 200;
    private final float worldWidth = mapWidth * tileSize;
    private final float worldHeight = mapHeight * tileSize;
    private final int treeDrawSize = 180;
    private static final int BRANCH_DRAW_SIZE = 40;
    private final int fireDrawSize = 70;
    private final int minDistanceTiles = 8;

    // Day / Night + simple lighting overlay (no shaders, no Box2D lights).
    private float dayNightTimeSec = 0f;
    private static final float DAY_LENGTH_SEC = 120f; // full cycle day->night->day
    private final Color nightOverlayColor = new Color(0.05f, 0.08f, 0.14f, 1f);
    private final Color tmpColor = new Color();
    private Texture fireGlowTexture;

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

        walkSheet = new Texture("player_walk_sheet.png");
        int frameWidth = walkSheet.getWidth() / 6;
        int frameHeight = walkSheet.getHeight();
        walkFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            walkFrames[i] = new TextureRegion(walkSheet, i * frameWidth, 0, frameWidth, frameHeight);
        }
        walkAnimation = new Animation<>(0.15f, walkFrames);

        fireSheet = new Texture("fire_animated.png");
        int fireWidth = fireSheet.getWidth() / 6;
        int fireHeight = fireSheet.getHeight();
        fireFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            fireFrames[i] = new TextureRegion(fireSheet, i * fireWidth, 0, fireWidth, fireHeight);
        }
        fireAnimation = new Animation<>(0.15f, fireFrames);

        fireOffTexture = new Texture("fire_off.png");
        int offWidth = fireOffTexture.getWidth() / 6;
        int offHeight = fireOffTexture.getHeight();
        fireOffFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            fireOffFrames[i] = new TextureRegion(fireOffTexture, i * offWidth, 0, offWidth, offHeight);
        }
        fireOffAnimation = new Animation<>(0.20f, fireOffFrames);

        bgTexture = new Texture("bg.png");
        treeTexture = new Texture("tree.png");
        branchTexture = new Texture("branch.png");
        grassTexture = new Texture("grass.png");
        fireGlowTexture = createRadialGlowTexture(256);

        resetGame();
        showMenu = true;
        hasActiveRun = false;
        continueEnabled = false;
        screenState = ScreenState.TITLE_MENU;
    }

    private void resetGame() {
        playerX = 1650;
        playerY = 1600;
        fireHealth = 0f;
        woodCount = 0;
        carriedWood = 0;
        isGameOver = false;
        fireHasBeenLit = false;
        stateTime = 0;
        fireStateTime = 0;
        branchRespawnTimer = 0f;
        dayNightTimeSec = 0f;

        boolean[][] occupied = new boolean[mapWidth][mapHeight];

        int fireTileX = 1600 / tileSize;
        int fireTileY = 1600 / tileSize;

        java.util.function.BiFunction<Integer, Integer, Boolean> isTooClose = (tx, ty) -> {
            for (int x = Math.max(0, tx - minDistanceTiles); x <= Math.min(mapWidth - 1, tx + minDistanceTiles); x++) {
                for (int y = Math.max(0, ty - minDistanceTiles); y <= Math.min(mapHeight - 1, ty + minDistanceTiles); y++) {
                    if (occupied[x][y]) return true;
                }
            }
            int dx = tx - fireTileX;
            int dy = ty - fireTileY;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);
            return distance < 8f;
        };

        trees = new Array<>();
        for (int i = 0; i < 160; i++) {
            int tileX = MathUtils.random(0, mapWidth - 1);
            int tileY = MathUtils.random(0, mapHeight - 1);
            if (occupied[tileX][tileY] || isTooClose.apply(tileX, tileY)) continue;
            occupied[tileX][tileY] = true;

            float worldX = tileX * tileSize;
            float worldY = tileY * tileSize;
            Rectangle bounds = new Rectangle(
                worldX + (treeDrawSize - 20) / 2f,
                worldY + 30,
                20,
                20
            );
            trees.add(new Tree(bounds, worldX, worldY));
        }

        branches = new Array<>();
        for (int i = 0; i < 320; i++) {
            int tileX = MathUtils.random(0, mapWidth - 1);
            int tileY = MathUtils.random(0, mapHeight - 1);
            if (occupied[tileX][tileY] || isTooClose.apply(tileX, tileY)) continue;
            occupied[tileX][tileY] = true;
            branches.add(new Branch(tileX * tileSize, tileY * tileSize));
        }

        grasses = new Array<>();
        for (int i = 0; i < 160; i++) {
            int tileX = MathUtils.random(0, mapWidth - 1);
            int tileY = MathUtils.random(0, mapHeight - 1);
            if (occupied[tileX][tileY]) continue;
            occupied[tileX][tileY] = true;
            grasses.add(new Grass(tileX * tileSize, tileY * tileSize));
        }
    }

    @Override
    public void render() {
        if (screenState == ScreenState.TITLE_MENU) {
            ScreenUtils.clear(0.24f, 0.19f, 0.13f, 1f);
        } else {
            ScreenUtils.clear(0f, 0f, 0f, 1f);
        }

        float lerp = 0.1f;
        float targetX = playerX + 60;
        float targetY = playerY + 60;
        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        float halfWidth = camera.viewportWidth / 2f;
        float halfHeight = camera.viewportHeight / 2f;
        if (camera.position.x < halfWidth) camera.position.x = halfWidth;
        if (camera.position.y < halfHeight) camera.position.y = halfHeight;
        if (camera.position.x > worldWidth - halfWidth) camera.position.x = worldWidth - halfWidth;
        if (camera.position.y > worldHeight - halfHeight) camera.position.y = worldHeight - halfHeight;

        viewport.apply();
        camera.update();
        ui.update(camera, viewport, showMenu, continueEnabled, screenState == ScreenState.PLAYING);

        handleUiInput();

        if (screenState == ScreenState.PLAYING && !showMenu && !isGameOver) {
            handleInput();
            updateLogic();
            fireStateTime += Gdx.graphics.getDeltaTime();
        }

        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        batch.begin();
        for (int x = 0; x < mapWidth; x++) {
            for (int y = 0; y < mapHeight; y++) {
                batch.draw(bgTexture, x * tileSize, y * tileSize, tileSize, tileSize);
            }
        }
        for (Grass grass : grasses) {
            batch.draw(grassTexture, grass.x, grass.y, 80, 80);
        }
        for (Branch branch : branches) {
            if (!branch.collected) {
                batch.draw(branchTexture, branch.drawX, branch.drawY, 40, 40);
            }
        }

        float fireX = 1600;
        float fireY = 1600;
        if (fireHealth > 0) {
            TextureRegion currentFireFrame = fireAnimation.getKeyFrame(fireStateTime, true);
            batch.draw(currentFireFrame, fireX, fireY, fireDrawSize, fireDrawSize);
        } else {
            TextureRegion currentFireOffFrame = fireOffAnimation.getKeyFrame(fireStateTime, true);
            batch.draw(currentFireOffFrame, fireX, fireY, fireDrawSize, 140);
        }

        for (Tree tree : trees) {
            if (tree.drawY >= playerY + 40) {
                batch.draw(treeTexture, tree.drawX, tree.drawY, treeDrawSize, treeDrawSize);
            }
        }

        TextureRegion currentFrame = isMoving ? walkAnimation.getKeyFrame(stateTime, true) : walkFrames[0];
        if (!facingRight) {
            batch.draw(currentFrame, playerX, playerY, 120, 120);
        } else {
            batch.draw(currentFrame.getTexture(), playerX, playerY, 120, 120,
                currentFrame.getRegionX(), currentFrame.getRegionY(),
                currentFrame.getRegionWidth(), currentFrame.getRegionHeight(),
                true, false);
        }

        for (Tree tree : trees) {
            if (tree.drawY < playerY + 40) {
                batch.draw(treeTexture, tree.drawX, tree.drawY, treeDrawSize, treeDrawSize);
            }
        }
        batch.end();

        if (screenState == ScreenState.PLAYING) {
            renderDayNightAndFireGlow();
        }

        ui.render(batch, shapeRenderer, camera, viewport, titleFont, bodyFont, showMenu, continueEnabled, screenState == ScreenState.GAME_OVER, fireHealth, carriedWood, maxWood, screenState == ScreenState.PLAYING);
    }

    private void renderDayNightAndFireGlow() {
        // Daylight factor: 1 = bright day, 0 = deep night.
        float t = (dayNightTimeSec % DAY_LENGTH_SEC) / DAY_LENGTH_SEC; // 0..1
        float daylight = 0.5f + 0.5f * MathUtils.cos(MathUtils.PI2 * t); // smooth cycle
        daylight = MathUtils.clamp(daylight, 0f, 1f);

        // Dark overlay alpha. Keep a bit of darkness even at day for mood, and cap at night.
        float darkness = 1f - daylight;
        float overlayAlpha = MathUtils.lerp(0.06f, 0.72f, darkness);

        // Fullscreen-ish rect in world coords (cover current camera view).
        float left = camera.position.x - camera.viewportWidth / 2f;
        float bottom = camera.position.y - camera.viewportHeight / 2f;
        float width = camera.viewportWidth;
        float height = camera.viewportHeight;

        Gdx.gl.glEnable(GL20.GL_BLEND);

        // 1) Darkness overlay (standard alpha blend).
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        tmpColor.set(nightOverlayColor);
        tmpColor.a = overlayAlpha;
        shapeRenderer.setColor(tmpColor);
        shapeRenderer.rect(left, bottom, width, height);
        shapeRenderer.end();

        // 2) Fire glow on top (additive), stronger at night.
        if (fireHealth > 0f) {
            float fireX = 1600f + fireDrawSize / 2f;
            float fireY = 1600f + fireDrawSize / 2f;

            float health01 = MathUtils.clamp(fireHealth / 100f, 0f, 1f);
            // Higher baseline intensity, gentler change over health/time.
            float nightBoost = MathUtils.lerp(0.85f, 1.10f, darkness);
            float flicker = 0.97f + 0.03f * MathUtils.sin(fireStateTime * 9.0f) + 0.02f * MathUtils.sin(fireStateTime * 16.0f);
            float healthCurve = 0.55f + 0.45f * health01;
            float intensity = healthCurve * nightBoost * flicker;
            intensity = MathUtils.clamp(intensity, 0f, 0.5f);

            // One textured radial glow => no rings/banding.
            float baseRadius = 320f + 220f * intensity;
            baseRadius = MathUtils.clamp(baseRadius, 320f, 920f);
            float drawSize = baseRadius * 2f;

            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.begin();
            // Warm tint, slightly brighter at night.
            float tintBoost = MathUtils.lerp(0.75f, 1.10f, darkness);
            float a = MathUtils.clamp(0.55f * intensity, 0f, 0.85f);
            batch.setColor(1.0f * tintBoost, 0.62f * tintBoost, 0.20f * tintBoost, a);
            batch.draw(
                fireGlowTexture,
                (fireX - drawSize / 2f),
                (fireY + 10f - drawSize / 2f),
                drawSize,
                drawSize
            );
            batch.setColor(Color.WHITE);
            batch.end();
        }

        // Restore default blending state expected by UI.
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private static Texture createRadialGlowTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float cx = (size - 1) / 2f;
        float cy = (size - 1) / 2f;
        float maxR = Math.min(cx, cy);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - cx) / maxR;
                float dy = (y - cy) / maxR;
                float d = (float)Math.sqrt(dx * dx + dy * dy); // 0..~1.4
                float t = MathUtils.clamp(d, 0f, 1f);          // 0..1 at edge

                // Smoothstep-ish curve: bright center, very soft tail.
                float alpha = 1f - t;
                alpha = alpha * alpha;              // quadratic
                alpha = alpha * (2f - alpha);       // smooth shaping
                alpha = MathUtils.clamp(alpha, 0f, 1f);

                // Store as white w/ alpha; we tint via batch color.
                int a = (int)(255f * alpha);
                // RGBA8888 packing is R<<24 | G<<16 | B<<8 | A.
                pixmap.drawPixel(x, y, (0xFF << 24) | (0xFF << 16) | (0xFF << 8) | (a & 0xFF));
            }
        }

        Texture tex = new Texture(pixmap);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();
        return tex;
    }

    private void handleInput() {
        float hitboxWidth = 80;
        float hitboxHeight = 80;
        float offsetX = (120 - hitboxWidth) / 2f;
        float offsetY = (120 - hitboxHeight) / 2f;
        float speed = 120 * Gdx.graphics.getDeltaTime();

        float newX = playerX;
        float newY = playerY;
        isMoving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { newY += speed; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { newY -= speed; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { newX -= speed; isMoving = true; facingRight = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { newX += speed; isMoving = true; facingRight = true; }

        Rectangle futureX = new Rectangle(newX + offsetX, playerY + offsetY, hitboxWidth, hitboxHeight);
        boolean collisionX = false;
        for (Tree tree : trees) {
            if (futureX.overlaps(tree.bounds)) { collisionX = true; break; }
        }
        for (Branch branch : branches) {
            if (!branch.collected && futureX.overlaps(branch.bounds)) { collisionX = true; break; }
        }
        Rectangle fireRect = new Rectangle(1600 + 30, 1600 + 20, 10, 10);
        if (futureX.overlaps(fireRect)) collisionX = true;
        if (!collisionX) {
            playerX = newX;
            if (playerX < 0) playerX = 0;
            if (playerX > worldWidth - 120) playerX = worldWidth - 120;
        }

        Rectangle futureY = new Rectangle(playerX + offsetX, newY + offsetY, hitboxWidth, hitboxHeight);
        boolean collisionY = false;
        for (Tree tree : trees) {
            if (futureY.overlaps(tree.bounds)) { collisionY = true; break; }
        }
        for (Branch branch : branches) {
            if (!branch.collected && futureY.overlaps(branch.bounds)) { collisionY = true; break; }
        }
        Rectangle fireRectY = new Rectangle(1600 + 30, 1600 + 20, 10, 10);
        if (futureY.overlaps(fireRectY)) collisionY = true;
        if (!collisionY) {
            playerY = newY;
            if (playerY < 0) playerY = 0;
            if (playerY > worldHeight - 120) playerY = worldHeight - 120;
        }

        if (isMoving) stateTime += Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Rectangle playerBounds = new Rectangle(playerX, playerY, 120, 120);
            for (Branch branch : branches) {
                if (!branch.collected && playerBounds.overlaps(branch.bounds)) {
                    if (carriedWood < maxWood) {
                        branch.collected = true;
                        branch.respawnTime = branchRespawnTime;
                        carriedWood++;
                    }
                }
            }

            if (playerBounds.overlaps(fireRect) && carriedWood > 0) {
                fireHealth += carriedWood * 20;
                if (fireHealth > 100) fireHealth = 100;
                fireHasBeenLit = true;
                woodCount += carriedWood;
                carriedWood = 0;
            }
        }
    }

    private void handleUiInput() {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            return;
        }

        GameUi.Action action = ui.handleClick(Gdx.input.getX(), Gdx.input.getY(), camera, viewport, showMenu, continueEnabled, screenState == ScreenState.PLAYING);
        switch (action) {
            case OPEN_MENU:
                showMenu = true;
                break;
            case NEW_GAME:
                resetGame();
                hasActiveRun = true;
                continueEnabled = true;
                showMenu = false;
                screenState = ScreenState.PLAYING;
                break;
            case CONTINUE:
                showMenu = false;
                screenState = ScreenState.PLAYING;
                break;
            case NONE:
            default:
                break;
        }
    }

    private void updateLogic() {
        dayNightTimeSec += Gdx.graphics.getDeltaTime();

        if (fireHasBeenLit && fireHealth > 0f) {
            fireHealth -= 1 * Gdx.graphics.getDeltaTime();
            if (fireHealth < 0) fireHealth = 0;
        }

        for (Branch branch : branches) {
            if (branch.collected) {
                branch.respawnTime -= Gdx.graphics.getDeltaTime();
                if (branch.respawnTime <= 0) {
                    branch.collected = false;
                }
            }
        }

        if (isNightTime() && fireHealth <= 0f) {
            isGameOver = true;
            showMenu = true;
            screenState = ScreenState.GAME_OVER;
            continueEnabled = false;
        }

    }

    private boolean isNightTime() {
        float t = (dayNightTimeSec % DAY_LENGTH_SEC) / DAY_LENGTH_SEC; // 0..1
        float daylight = 0.5f + 0.5f * MathUtils.cos(MathUtils.PI2 * t);
        // Consider it "night" when the overlay is clearly dark.
        return daylight < 0.33f;
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        shapeRenderer.dispose();
        titleFont.dispose();
        bodyFont.dispose();
        walkSheet.dispose();
        fireSheet.dispose();
        fireOffTexture.dispose();
        treeTexture.dispose();
        bgTexture.dispose();
        branchTexture.dispose();
        grassTexture.dispose();
        if (fireGlowTexture != null) fireGlowTexture.dispose();
    }

    private static class Branch {
        Rectangle bounds;
        float drawX, drawY;
        boolean collected = false;
        float respawnTime = 0f;

        Branch(float x, float y) {
            this.drawX = x;
            this.drawY = y;
            float size = BRANCH_DRAW_SIZE / 3f;
            this.bounds = new Rectangle(
                x + (BRANCH_DRAW_SIZE - size) / 2f,
                y + (BRANCH_DRAW_SIZE - size) / 2f,
                size,
                size
            );
        }
    }

    private static class Tree {
        Rectangle bounds;
        float drawX, drawY;

        Tree(Rectangle bounds, float drawX, float drawY) {
            this.bounds = bounds;
            this.drawX = drawX;
            this.drawY = drawY;
        }
    }

    private static class Grass {
        float x, y;

        Grass(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
