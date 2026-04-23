package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Main extends ApplicationAdapter {

    SpriteBatch batch;
    OrthographicCamera camera;
    Viewport viewport;

    Texture walkSheet, fireSheet, treeTexture, bgTexture, branchTexture, fireOffTexture, grassTexture;
    Animation<TextureRegion> walkAnimation;
    Animation<TextureRegion> fireAnimation;
    Animation<TextureRegion> fireOffAnimation;
    TextureRegion[] walkFrames;
    TextureRegion[] fireFrames;
    TextureRegion[] fireOffFrames;

    float playerX, playerY;
    float stateTime;
    float fireStateTime;
    boolean isMoving;
    boolean facingRight = true;

    float fireHealth = 100f;
    int woodCount = 0;
    int carriedWood = 0;
    int maxWood = 5;
    float branchRespawnTimer = 0f;
    float BRANCH_RESPAWN_TIME = 60f;
    boolean isGameOver = false;

    Array<Tree> trees;
    Array<Branch> branches;
    Array<Grass> grasses;

    int TILE_SIZE = 16;
    int MAP_WIDTH = 100;
    int MAP_HEIGHT = 100;
    float WORLD_WIDTH = MAP_WIDTH * TILE_SIZE;
    float WORLD_HEIGHT = MAP_HEIGHT * TILE_SIZE;
    int TREE_DRAW_SIZE = 180;
    static int BRANCH_DRAW_SIZE = 40;
    int FIRE_DRAW_SIZE = 70;
    int MIN_DISTANCE_TILES = 8;

    @Override
    public void create() {
        batch = new SpriteBatch();

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
        int fWidth = fireSheet.getWidth() / 6;
        int fHeight = fireSheet.getHeight();

        fireFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            fireFrames[i] = new TextureRegion(fireSheet, i * fWidth, 0, fWidth, fHeight);
        }
        fireAnimation = new Animation<>(0.15f, fireFrames);

        bgTexture = new Texture("bg.png");
        treeTexture = new Texture("tree.png");
        branchTexture = new Texture("branch.png");
        fireOffTexture = new Texture("fire_off.png");
        grassTexture = new Texture("grass.png");

        int offWidth = fireOffTexture.getWidth() / 6;
        int offHeight = fireOffTexture.getHeight();

        fireOffFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            fireOffFrames[i] = new TextureRegion(fireOffTexture, i * offWidth, 0, offWidth, offHeight);
        }
        fireOffAnimation = new Animation<>(0.20f, fireOffFrames);

        resetGame();
    }

    private void resetGame() {
        playerX = 850;
        playerY = 800;
        fireHealth = 0f;
        woodCount = 0;
        isGameOver = false;
        stateTime = 0;
        fireStateTime = 0;

        boolean[][] occupied = new boolean[MAP_WIDTH][MAP_HEIGHT];

        int fireTileX = 800 / TILE_SIZE;
        int fireTileY = 800 / TILE_SIZE;
        int MIN_DISTANCE_FROM_FIRE = 8;

        java.util.function.BiFunction<Integer, Integer, Boolean> isTooClose = (tx, ty) -> {

            for (int x = Math.max(0, tx - MIN_DISTANCE_TILES); x <= Math.min(MAP_WIDTH - 1, tx + MIN_DISTANCE_TILES); x++) {
                for (int y = Math.max(0, ty - MIN_DISTANCE_TILES); y <= Math.min(MAP_HEIGHT - 1, ty + MIN_DISTANCE_TILES); y++) {
                    if (occupied[x][y]) return true;
                }
            }

            int dx = tx - fireTileX;
            int dy = ty - fireTileY;
            float distance = (float)Math.sqrt(dx * dx + dy * dy);

            if (distance < MIN_DISTANCE_FROM_FIRE) return true;

            return false;
        };

        trees = new Array<>();

        for (int i = 0; i < 80; i++) {
            int tileX = MathUtils.random(0, MAP_WIDTH - 1);
            int tileY = MathUtils.random(0, MAP_HEIGHT - 1);

            if (occupied[tileX][tileY] || isTooClose.apply(tileX, tileY)) continue;

            occupied[tileX][tileY] = true;

            float worldX = tileX * TILE_SIZE;
            float worldY = tileY * TILE_SIZE;

            float drawX = worldX;
            float drawY = worldY;

            float trunkWidth = 20;
            float trunkHeight = 20;

            Rectangle bounds = new Rectangle(
                    worldX + (TREE_DRAW_SIZE - trunkWidth) / 2f,
                    worldY + 30,
                    trunkWidth,
                    trunkHeight
            );

            trees.add(new Tree(bounds, drawX, drawY));
        }

        branches = new Array<>();

        for (int i = 0; i < 320; i++) {
            int tileX = MathUtils.random(0, MAP_WIDTH - 1);
            int tileY = MathUtils.random(0, MAP_HEIGHT - 1);

            if (occupied[tileX][tileY] || isTooClose.apply(tileX, tileY)) continue;

            occupied[tileX][tileY] = true;

            float worldX = tileX * TILE_SIZE;
            float worldY = tileY * TILE_SIZE;

            branches.add(new Branch(worldX, worldY));
        }

        grasses = new Array<>();

        for (int i = 0; i < 80; i++) {
            int tileX = MathUtils.random(0, MAP_WIDTH - 1);
            int tileY = MathUtils.random(0, MAP_HEIGHT - 1);

            if (occupied[tileX][tileY]) continue;

            float worldX = tileX * TILE_SIZE;
            float worldY = tileY * TILE_SIZE;

            grasses.add(new Grass(worldX, worldY));
        }
    }

    @Override
    public void render() {
        ScreenUtils.clear(0, 0, 0, 1);

        if (!isGameOver) {
            handleInput();
            updateLogic();
            fireStateTime += Gdx.graphics.getDeltaTime();
        }

        float lerp = 0.1f;

        float targetX = playerX + 60;
        float targetY = playerY + 60;

        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        float halfWidth = camera.viewportWidth / 2;
        float halfHeight = camera.viewportHeight / 2;

        if (camera.position.x < halfWidth) camera.position.x = halfWidth;
        if (camera.position.y < halfHeight) camera.position.y = halfHeight;
        if (camera.position.x > WORLD_WIDTH - halfWidth) camera.position.x = WORLD_WIDTH - halfWidth;
        if (camera.position.y > WORLD_HEIGHT - halfHeight) camera.position.y = WORLD_HEIGHT - halfHeight;

        viewport.apply();
        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        for (int x = 0; x < MAP_WIDTH; x++) {
            for (int y = 0; y < MAP_HEIGHT; y++) {
                batch.draw(bgTexture, x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            }
        }

        for (Grass grass : grasses) {
            batch.draw(grassTexture, grass.x, grass.y, 80, 80);
        }

        for (Branch branch : branches) {
            if (!branch.collected) {
                batch.draw(branchTexture, branch.drawX, branch.drawY, BRANCH_DRAW_SIZE, BRANCH_DRAW_SIZE);
            }
        }

        float fireX = 800;
        float fireY = 800;

        if (fireHealth > 0) {
            TextureRegion currentFireFrame = fireAnimation.getKeyFrame(fireStateTime, true);
            batch.draw(currentFireFrame, fireX, fireY, FIRE_DRAW_SIZE, FIRE_DRAW_SIZE);
        } else {
            TextureRegion currentFireOffFrame = fireOffAnimation.getKeyFrame(fireStateTime, true);
            batch.draw(currentFireOffFrame, fireX, fireY, FIRE_DRAW_SIZE, 140);
        }

        for (Tree tree : trees) {
            if (tree.drawY >= playerY + 40) {
                batch.draw(
                    treeTexture,
                    tree.drawX,
                    tree.drawY,
                    TREE_DRAW_SIZE,
                    TREE_DRAW_SIZE
                );
            }
        }

        TextureRegion currentFrame = isMoving
                ? walkAnimation.getKeyFrame(stateTime, true)
                : walkFrames[0];

        if (!facingRight) {
            batch.draw(currentFrame, playerX, playerY, 120, 120);
        } else {
            batch.draw(currentFrame.getTexture(),
                    playerX, playerY, 120, 120,
                    currentFrame.getRegionX(), currentFrame.getRegionY(),
                    currentFrame.getRegionWidth(), currentFrame.getRegionHeight(),
                    true, false);
        }

        for (Tree tree : trees) {
            if (tree.drawY < playerY + 40) {
                batch.draw(
                    treeTexture,
                    tree.drawX,
                    tree.drawY,
                    TREE_DRAW_SIZE,
                    TREE_DRAW_SIZE
                );
            }
        }

        batch.end();
    }

    private void handleInput() {

        float hitboxWidth = 80;
        float hitboxHeight = 80;
        float offsetX = (120 - hitboxWidth) / 2;
        float offsetY = (120 - hitboxHeight) / 2;

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
            if (futureX.overlaps(tree.bounds)) {
                collisionX = true;
                break;
            }
        }

        for (Branch branch : branches) {
            if (!branch.collected && futureX.overlaps(branch.bounds)) {
                collisionX = true;
                break;
            }
        }

        Rectangle fireRect = new Rectangle(800 + 30, 800 + 20, 10, 10);
        if (futureX.overlaps(fireRect)) collisionX = true;

        if (!collisionX) {
            playerX = newX;
            if (playerX < 0) playerX = 0;
            if (playerX > WORLD_WIDTH - 120) playerX = WORLD_WIDTH - 120;
        }

        Rectangle futureY = new Rectangle(playerX + offsetX, newY + offsetY, hitboxWidth, hitboxHeight);
        boolean collisionY = false;

        for (Tree tree : trees) {
            if (futureY.overlaps(tree.bounds)) {
                collisionY = true;
                break;
            }
        }

        for (Branch branch : branches) {
            if (!branch.collected && futureY.overlaps(branch.bounds)) {
                collisionY = true;
                break;
            }
        }

        Rectangle fireRectY = new Rectangle(800 + 30, 800 + 20, 10, 10);
        if (futureY.overlaps(fireRectY)) collisionY = true;

        if (!collisionY) {
            playerY = newY;
            if (playerY < 0) playerY = 0;
            if (playerY > WORLD_HEIGHT - 120) playerY = WORLD_HEIGHT - 120;
        }

        if (isMoving) stateTime += Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Rectangle playerBounds = new Rectangle(
                    playerX,
                    playerY,
                    120,
                    120
            );

            for (Branch branch : branches) {
                if (!branch.collected && playerBounds.overlaps(branch.bounds)) {
                    if (carriedWood < maxWood) {
                        branch.collected = true;
                        branch.respawnTime = BRANCH_RESPAWN_TIME;
                        carriedWood++;
                    }
                }
            }
        }

        Rectangle playerBounds = new Rectangle(playerX, playerY, 120, 120);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            if (playerBounds.overlaps(fireRect) && carriedWood > 0) {
                fireHealth += carriedWood * 20;
                if (fireHealth > 100) fireHealth = 100;
                woodCount += carriedWood;
                carriedWood = 0;
            }
        }

        if (isGameOver && Gdx.input.isKeyPressed(Input.Keys.R)) resetGame();
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
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    @Override
    public void dispose() {
        batch.dispose();
        walkSheet.dispose();
        treeTexture.dispose();
        fireSheet.dispose();
        bgTexture.dispose();
        branchTexture.dispose();
        fireOffTexture.dispose();
        grassTexture.dispose();
    }

    static class Branch {
        public Rectangle bounds;
        public float drawX, drawY;
        public boolean collected = false;
        public float respawnTime = 0f;

        public Branch(float x, float y) {
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

    static class Tree {
        public Rectangle bounds;
        public float drawX, drawY;

        public Tree(Rectangle bounds, float drawX, float drawY) {
            this.bounds = bounds;
            this.drawX = drawX;
            this.drawY = drawY;
        }
    }

    static class Grass {
        public float x, y;

        public Grass(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
