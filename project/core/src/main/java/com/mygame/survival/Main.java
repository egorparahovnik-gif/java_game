package com.mygame.survival;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;

public class Main extends ApplicationAdapter {
    SpriteBatch batch;

    Texture walkSheet, fireSheet, treeTexture, bgTexture, branchTexture;
    Animation<TextureRegion> walkAnimation;
    Animation<TextureRegion> fireAnimation;
    TextureRegion[] walkFrames;
    TextureRegion[] fireFrames;

    float playerX, playerY;
    float stateTime;
    float fireStateTime;
    boolean isMoving;
    boolean facingRight = true;

    float fireHealth = 100f;
    int woodCount = 0;
    boolean isGameOver = false;

    Array<Rectangle> trees;
    Array<Branch> branches;

    @Override
    public void create() {
        batch = new SpriteBatch();

        walkSheet = new Texture("player_walk_sheet.png");
        walkSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        int frameWidth = walkSheet.getWidth() / 6;
        int frameHeight = walkSheet.getHeight();
        walkFrames = new TextureRegion[6];
        for (int i = 0; i < 6; i++) {
            walkFrames[i] = new TextureRegion(walkSheet, i * frameWidth, 0, frameWidth, frameHeight);
        }
        walkAnimation = new Animation<>(0.15f, walkFrames);

        fireSheet = new Texture("fire_animated.png");
        fireSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
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

        resetGame();
    }

    private void resetGame() {
        playerX = 100;
        playerY = 100;
        fireHealth = 100f;
        woodCount = 0;
        isGameOver = false;
        stateTime = 0;
        fireStateTime = 0;
        facingRight = true;

        trees = new Array<>();
        for (int i = 0; i < 20; i++) {
            trees.add(new Rectangle(MathUtils.random(0, 700), MathUtils.random(0, 500), 100, 100));
        }

        branches = new Array<>();
        for (int i = 0; i < 20; i++) {
            branches.add(new Branch(MathUtils.random(50, 750), MathUtils.random(50, 550)));
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

        batch.begin();
        batch.draw(bgTexture, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        for (Branch branch : branches) {
            if (!branch.collected) {
                batch.draw(branchTexture, branch.bounds.x, branch.bounds.y, 20, 20);
            }
        }

        TextureRegion currentFireFrame = fireAnimation.getKeyFrame(fireStateTime, true);
        batch.draw(currentFireFrame, 300, 225, 40, 40);

        TextureRegion currentFrame = isMoving ? walkAnimation.getKeyFrame(stateTime, true) : walkFrames[0];
        if (!facingRight) {
            batch.draw(currentFrame, playerX, playerY, 70, 70);
        } else {
            batch.draw(currentFrame.getTexture(),
                playerX, playerY, 70, 70,
                currentFrame.getRegionX(), currentFrame.getRegionY(),
                currentFrame.getRegionWidth(), currentFrame.getRegionHeight(),
                true, false);
        }

        for (Rectangle treeBounds : trees) {
            batch.draw(treeTexture, treeBounds.x, treeBounds.y, treeBounds.width, treeBounds.height);
        }
        batch.end();
    }

    private void handleInput() {
        float speed = 80 * Gdx.graphics.getDeltaTime();
        isMoving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { playerY += speed; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { playerY -= speed; isMoving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { playerX -= speed; isMoving = true; facingRight = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { playerX += speed; isMoving = true; facingRight = true; }

        if (isMoving) stateTime += Gdx.graphics.getDeltaTime();

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            Rectangle playerBounds = new Rectangle(playerX, playerY, 64, 64);
            for (Branch branch : branches) {
                if (!branch.collected && playerBounds.overlaps(branch.bounds)) {
                    branch.collected = true;
                    woodCount++;
                    fireHealth += 20;
                    if (fireHealth > 100) fireHealth = 100;
                    break;
                }
            }
        }
        if (isGameOver && Gdx.input.isKeyPressed(Input.Keys.R)) resetGame();
    }

    private void updateLogic() {
        fireHealth -= 1 * Gdx.graphics.getDeltaTime();
        if (fireHealth <= 0) isGameOver = true;
    }

    @Override
    public void dispose() {
        batch.dispose();
        walkSheet.dispose();
        treeTexture.dispose();
        fireSheet.dispose();
        bgTexture.dispose();
        branchTexture.dispose();
    }

    static class Branch {
        public Rectangle bounds;
        public boolean collected = false;
        public Branch(float x, float y) {
            this.bounds = new Rectangle(x, y, 20, 20);
        }
    }
}
