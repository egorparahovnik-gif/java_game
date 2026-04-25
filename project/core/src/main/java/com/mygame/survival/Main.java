package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
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

    private float fireHealth = 100f;
    private int woodCount = 0;
    private int carriedWood = 0;
    private final int maxWood = 5;
    private float branchRespawnTimer = 0f;
    private final float branchRespawnTime = 60f;
    private boolean isGameOver = false;

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

        resetGame();
        showMenu = true;
        hasActiveRun = false;
        continueEnabled = false;
        screenState = ScreenState.TITLE_MENU;
    }

    private void resetGame() {
        playerX = 1650;
        playerY = 1600;
        fireHealth = 100f;
        woodCount = 0;
        carriedWood = 0;
        isGameOver = false;
        stateTime = 0;
        fireStateTime = 0;
        branchRespawnTimer = 0f;

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

        ui.render(batch, shapeRenderer, camera, viewport, titleFont, bodyFont, showMenu, continueEnabled, screenState == ScreenState.GAME_OVER, fireHealth, carriedWood, maxWood, screenState == ScreenState.PLAYING);
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
        fireHealth -= 1 * Gdx.graphics.getDeltaTime();
        if (fireHealth < 0) fireHealth = 0;

        for (Branch branch : branches) {
            if (branch.collected) {
                branch.respawnTime -= Gdx.graphics.getDeltaTime();
                if (branch.respawnTime <= 0) {
                    branch.collected = false;
                }
            }
        }

        if (fireHealth <= 0f) {
            isGameOver = true;
            showMenu = true;
            screenState = ScreenState.GAME_OVER;
            continueEnabled = false;
        }

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
