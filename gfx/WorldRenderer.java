package com.mygame.survival.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.mygame.survival.game.GameConstants;
import com.mygame.survival.game.GameWorld;
import com.mygame.survival.game.entity.Berry;
import com.mygame.survival.game.entity.Branch;
import com.mygame.survival.game.entity.Grass;
import com.mygame.survival.game.entity.Tree;

public final class WorldRenderer {
    // Darker, colder night tone. Alpha is controlled dynamically.
    private final Color nightOverlayColor = new Color(0.02f, 0.03f, 0.06f, 1f);
    private final Color tmpColor = new Color();

    public void updateCamera(OrthographicCamera camera, Viewport viewport, GameWorld world) {
        float lerp = 0.1f;
        float targetX = world.player.x + GameConstants.PLAYER_DRAW_SIZE / 2f;
        float targetY = world.player.y + GameConstants.PLAYER_DRAW_SIZE / 2f;
        camera.position.x += (targetX - camera.position.x) * lerp;
        camera.position.y += (targetY - camera.position.y) * lerp;

        float halfWidth = camera.viewportWidth / 2f;
        float halfHeight = camera.viewportHeight / 2f;
        if (camera.position.x < halfWidth) camera.position.x = halfWidth;
        if (camera.position.y < halfHeight) camera.position.y = halfHeight;
        if (camera.position.x > world.worldWidth() - halfWidth) camera.position.x = world.worldWidth() - halfWidth;
        if (camera.position.y > world.worldHeight() - halfHeight) camera.position.y = world.worldHeight() - halfHeight;
        viewport.apply();
    }

    public void render(
        SpriteBatch batch,
        ShapeRenderer shapeRenderer,
        OrthographicCamera camera,
        Viewport viewport,
        GameAssets assets,
        GameWorld world
    ) {
        batch.begin();
        for (int x = 0; x < GameConstants.MAP_WIDTH_TILES; x++) {
            for (int y = 0; y < GameConstants.MAP_HEIGHT_TILES; y++) {
                batch.draw(assets.bgTexture, x * GameConstants.TILE_SIZE, y * GameConstants.TILE_SIZE, GameConstants.TILE_SIZE, GameConstants.TILE_SIZE);
            }
        }

        for (Grass grass : world.grasses) {
            batch.draw(assets.grassTexture, grass.x, grass.y, GameConstants.GRASS_DRAW_SIZE, GameConstants.GRASS_DRAW_SIZE);
        }

        for (Branch branch : world.branches) {
            if (!branch.collected) {
                batch.draw(assets.branchTexture, branch.drawX, branch.drawY, GameConstants.BRANCH_DRAW_SIZE, GameConstants.BRANCH_DRAW_SIZE);
            }
        }

        for (Berry berry : world.berries) {
            if (!berry.collected) {
                batch.draw(assets.berryTexture, berry.drawX, berry.drawY, GameConstants.BERRY_DRAW_SIZE, GameConstants.BERRY_DRAW_SIZE);
            }
        }

        if (world.campfire.health > 0f) {
            TextureRegion current = assets.fireAnimation.getKeyFrame(world.fireAnimTimeSec, true);
            batch.draw(current, GameConstants.CAMPFIRE_X, GameConstants.CAMPFIRE_Y, GameConstants.CAMPFIRE_DRAW_SIZE, GameConstants.CAMPFIRE_DRAW_SIZE);
        } else {
            TextureRegion current = assets.fireOffAnimation.getKeyFrame(world.fireAnimTimeSec, true);
            batch.draw(current, GameConstants.CAMPFIRE_X, GameConstants.CAMPFIRE_Y, GameConstants.CAMPFIRE_DRAW_SIZE, 140f);
        }

        // Depth sorting: draw by Y so "upper" trees don't overlap "lower" trees incorrectly.
        // Higher Y => farther away => draw earlier. Lower Y => closer => draw later on top.
        Array<Tree> sortedTrees = new Array<>(world.trees);
        sortedTrees.sort((a, b) -> Float.compare(b.drawY, a.drawY)); // desc

        TextureRegion playerFrame = world.player.moving ? assets.walkAnimation.getKeyFrame(world.player.walkStateTimeSec, true) : assets.idleFrame;
        float playerSortY = world.player.y + 40f;
        boolean playerDrawn = false;

        for (Tree tree : sortedTrees) {
            if (!playerDrawn && tree.drawY < playerSortY) {
                drawPlayer(batch, world, playerFrame);
                playerDrawn = true;
            }
            batch.draw(assets.treeTexture, tree.drawX, tree.drawY, GameConstants.TREE_DRAW_SIZE, GameConstants.TREE_DRAW_SIZE);
        }
        if (!playerDrawn) {
            drawPlayer(batch, world, playerFrame);
        }
        batch.end();

        renderDayNightAndFireGlow(batch, shapeRenderer, camera, world, assets);
    }

    private void drawPlayer(SpriteBatch batch, GameWorld world, TextureRegion playerFrame) {
        if (!world.player.facingRight) {
            batch.draw(playerFrame, world.player.x, world.player.y, GameConstants.PLAYER_DRAW_SIZE, GameConstants.PLAYER_DRAW_SIZE);
        } else {
            batch.draw(
                playerFrame.getTexture(),
                world.player.x,
                world.player.y,
                GameConstants.PLAYER_DRAW_SIZE,
                GameConstants.PLAYER_DRAW_SIZE,
                playerFrame.getRegionX(),
                playerFrame.getRegionY(),
                playerFrame.getRegionWidth(),
                playerFrame.getRegionHeight(),
                true,
                false
            );
        }
    }

    private void renderDayNightAndFireGlow(SpriteBatch batch, ShapeRenderer shapeRenderer, OrthographicCamera camera, GameWorld world, GameAssets assets) {
        float t = (world.dayNightTimeSec % GameConstants.DAY_LENGTH_SEC) / GameConstants.DAY_LENGTH_SEC;
        float daylight = 0.5f + 0.5f * MathUtils.cos(MathUtils.PI2 * t);
        daylight = MathUtils.clamp(daylight, 0f, 1f);

        float darkness = 1f - daylight;
        // Stronger perceived transition: keep "day" mostly bright, then drop faster into night.
        float darknessCurve = (float) Math.pow(MathUtils.clamp(darkness, 0f, 1f), 1.8f);
        float overlayAlpha = MathUtils.lerp(0.03f, 0.88f, darknessCurve);

        float left = camera.position.x - camera.viewportWidth / 2f;
        float bottom = camera.position.y - camera.viewportHeight / 2f;
        float width = camera.viewportWidth;
        float height = camera.viewportHeight;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        tmpColor.set(nightOverlayColor);
        tmpColor.a = overlayAlpha;
        shapeRenderer.setColor(tmpColor);
        shapeRenderer.rect(left, bottom, width, height);
        shapeRenderer.end();

        if (world.campfire.health > 0f) {
            float fireCenterX = GameConstants.CAMPFIRE_X + GameConstants.CAMPFIRE_DRAW_SIZE / 2f;
            float fireCenterY = GameConstants.CAMPFIRE_Y + GameConstants.CAMPFIRE_DRAW_SIZE / 2f;

            float health01 = MathUtils.clamp(world.campfire.health / 100f, 0f, 1f);
            float nightBoost = MathUtils.lerp(0.80f, 1.20f, darknessCurve);
            float microFlicker = 0.97f
                + 0.03f * MathUtils.sin(world.fireAnimTimeSec * 9.0f)
                + 0.02f * MathUtils.sin(world.fireAnimTimeSec * 16.0f);
            // Slow pulse to make the light feel alive (radius + intensity).
            float pulse = 1f + 0.08f * MathUtils.sin(world.fireAnimTimeSec * 1.35f)
                + 0.03f * MathUtils.sin(world.fireAnimTimeSec * 0.62f + 1.3f);
            float healthCurve = 0.55f + 0.45f * health01;
            float intensity = healthCurve * nightBoost * microFlicker;
            intensity = MathUtils.clamp(intensity, 0f, 0.55f) * pulse;
            intensity = MathUtils.clamp(intensity, 0f, 0.65f);

            float baseRadius = 420f + 520f * intensity;
            baseRadius = MathUtils.clamp(baseRadius, 420f, 1200f);
            float drawSize = baseRadius * 2f;

            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            batch.begin();
            float tintBoost = MathUtils.lerp(0.70f, 1.15f, darknessCurve);
            // Slightly lower alpha per unit intensity because radius is larger now.
            float a = MathUtils.clamp(0.42f * intensity, 0f, 0.85f);
            batch.setColor(1.0f * tintBoost, 0.62f * tintBoost, 0.20f * tintBoost, a);
            batch.draw(
                assets.fireGlowTexture,
                (fireCenterX - drawSize / 2f),
                (fireCenterY + 10f - drawSize / 2f),
                drawSize,
                drawSize
            );
            batch.setColor(Color.WHITE);
            batch.end();
        }

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }
}

