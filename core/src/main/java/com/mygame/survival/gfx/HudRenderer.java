package com.mygame.survival.gfx;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.MathUtils;
import com.mygame.survival.game.GameConstants;
import com.mygame.survival.game.GameWorld;

public final class HudRenderer {
    private static final float HUD_MARGIN = 14f;
    private static final float HUD_BAR_HEIGHT = 20f;
    private static final Matrix4 HUD_MATRIX = new Matrix4();
    private static final Color HUD_BAR_BG = new Color(0.23f, 0.17f, 0.13f, 0.96f);
    private static final Color HUD_BAR_BORDER = new Color(0.44f, 0.26f, 0.19f, 1f);
    private static final Color HUD_TEXT_BROWN = new Color(0.52f, 0.32f, 0.26f, 1f);

    private final GlyphLayout hudLayout = new GlyphLayout();

    public void renderHud(SpriteBatch batch, ShapeRenderer shapeRenderer, BitmapFont font, GameAssets assets, GameWorld world) {
        float screenWidth = Gdx.graphics.getWidth();
        float screenHeight = Gdx.graphics.getHeight();
        float leftPanelWidth = Math.min(280f, screenWidth * 0.30f);
        float leftPanelHeight = Math.min(208f, screenHeight * 0.31f);
        float centerPanelWidth = Math.min(500f, screenWidth * 0.58f);
        float centerPanelHeight = Math.min(176f, screenHeight * 0.27f);

        float leftPanelX = HUD_MARGIN;
        float leftPanelY = screenHeight - HUD_MARGIN - leftPanelHeight;
        float centerPanelX = (screenWidth - centerPanelWidth) / 2f;
        float centerPanelY = screenHeight - HUD_MARGIN - centerPanelHeight;

        HUD_MATRIX.setToOrtho2D(0f, 0f, screenWidth, screenHeight);
        batch.setProjectionMatrix(HUD_MATRIX);
        shapeRenderer.setProjectionMatrix(HUD_MATRIX);

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        batch.begin();
        drawHudPanel(batch, assets.hudPanelLeft, leftPanelX, leftPanelY, leftPanelWidth, leftPanelHeight);
        drawHudPanel(batch, assets.hudPanelWide, centerPanelX, centerPanelY, centerPanelWidth, centerPanelHeight);
        batch.end();

        float leftBarWidth = leftPanelWidth - 102f;
        float leftBarX = leftPanelX + (leftPanelWidth - leftBarWidth) / 2f;
        float centerBarWidth = centerPanelWidth - 136f;
        float centerBarX = centerPanelX + (centerPanelWidth - centerBarWidth) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        drawLabeledBar(shapeRenderer, leftBarX, leftPanelY + leftPanelHeight - 98f, leftBarWidth, playerHealthRatio(world), new Color(0.73f, 0.28f, 0.22f, 1f));
        drawLabeledBar(shapeRenderer, leftBarX, leftPanelY + leftPanelHeight - 148f, leftBarWidth, playerHungerRatio(world), new Color(0.86f, 0.62f, 0.21f, 1f));
        drawLabeledBar(shapeRenderer, centerBarX, centerPanelY + 88f, centerBarWidth, campfireRatio(world), new Color(0.87f, 0.43f, 0.15f, 1f));
        shapeRenderer.end();

        batch.begin();
        drawHudText(batch, font, "HEALTH", leftPanelX + 42f, leftPanelY + leftPanelHeight - 60f, HUD_TEXT_BROWN, 1.02f);
        drawHudTextRight(batch, font, MathUtils.floor(world.player.health) + "/" + (int) GameConstants.MAX_HEALTH, leftPanelX + leftPanelWidth - 42f, leftPanelY + leftPanelHeight - 60f, HUD_TEXT_BROWN, 0.94f);
        drawHudText(batch, font, "HUNGER", leftPanelX + 42f, leftPanelY + leftPanelHeight - 108f, HUD_TEXT_BROWN, 1.02f);
        drawHudTextRight(batch, font, MathUtils.floor(world.player.hunger) + "/" + (int) GameConstants.MAX_HUNGER, leftPanelX + leftPanelWidth - 42f, leftPanelY + leftPanelHeight - 108f, HUD_TEXT_BROWN, 0.94f);

        drawHudTextBoldCentered(batch, font, "DAY " + String.format("%02d", world.currentDayNumber()), centerPanelX + centerPanelWidth / 2f, centerPanelY + centerPanelHeight - 22f, HUD_TEXT_BROWN, 2.18f);
        batch.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    private void drawHudPanel(SpriteBatch batch, com.badlogic.gdx.graphics.g2d.TextureRegion panelRegion, float x, float y, float width, float height) {
        batch.setColor(0f, 0f, 0f, 0.30f);
        batch.draw(panelRegion, x + 6f, y - 5f, width, height);
        batch.setColor(Color.WHITE);
        batch.draw(panelRegion, x, y, width, height);
    }

    private void drawLabeledBar(ShapeRenderer shapeRenderer, float x, float y, float width, float ratio, Color fillColor) {
        drawBar(shapeRenderer, x, y, width, HUD_BAR_HEIGHT, ratio, fillColor);
    }

    private void drawBar(ShapeRenderer shapeRenderer, float x, float y, float width, float height, float ratio, Color fillColor) {
        float clampedRatio = MathUtils.clamp(ratio, 0f, 1f);
        drawPixelRoundedRect(shapeRenderer, x - 2f, y - 2f, width + 4f, height + 4f, HUD_BAR_BORDER);
        drawPixelRoundedRect(shapeRenderer, x, y, width, height, HUD_BAR_BG);

        float fillWidth = MathUtils.clamp(width * clampedRatio, 0f, width);
        if (fillWidth <= 0f) {
            return;
        }

        float fillInnerWidth = Math.max(0f, fillWidth - 4f);
        float fillInnerHeight = Math.max(0f, height - 4f);
        shapeRenderer.setColor(fillColor);
        shapeRenderer.rect(x + 2f, y + 2f, fillInnerWidth, fillInnerHeight);

        if (fillWidth > 6f) {
            shapeRenderer.rect(x + 2f, y + 1f, Math.min(2f, fillWidth - 2f), 2f);
            shapeRenderer.rect(x + 2f, y + height - 3f, Math.min(2f, fillWidth - 2f), 2f);
        }

        shapeRenderer.setColor(1f, 1f, 1f, 0.12f);
        shapeRenderer.rect(x + 3f, y + height - 5f, Math.max(0f, fillInnerWidth - 2f), 2f);
    }

    private void drawPixelRoundedRect(ShapeRenderer shapeRenderer, float x, float y, float width, float height, Color color) {
        float w = Math.max(0f, width);
        float h = Math.max(0f, height);
        if (w == 0f || h == 0f) return;

        shapeRenderer.setColor(color);

        float topBand = Math.min(2f, h);
        float midBand = Math.max(0f, h - 4f);
        float bottomBand = Math.min(2f, Math.max(0f, h - topBand - midBand));

        if (h >= 2f) {
            shapeRenderer.rect(x + 2f, y + h - 2f, Math.max(0f, w - 4f), topBand);
        }
        if (h >= 4f) {
            shapeRenderer.rect(x + 1f, y + h - 4f, Math.max(0f, w - 2f), 2f);
        }
        if (midBand > 0f) {
            shapeRenderer.rect(x, y + 2f, w, midBand);
        } else if (h > 4f) {
            shapeRenderer.rect(x, y + 2f, w, h - 4f);
        }
        if (h >= 4f) {
            shapeRenderer.rect(x + 1f, y, Math.max(0f, w - 2f), 2f);
        }
        if (bottomBand > 0f) {
            shapeRenderer.rect(x + 2f, y, Math.max(0f, w - 4f), bottomBand);
        }
    }

    private void drawHudText(SpriteBatch batch, BitmapFont font, String text, float x, float y, Color color, float scale) {
        font.getData().setScale(scale);
        font.setColor(color);
        font.draw(batch, text, x - 1f, y);
        font.draw(batch, text, x + 1f, y);
        font.draw(batch, text, x, y - 1f);
        font.draw(batch, text, x, y + 1f);
        font.setColor(color);
        font.draw(batch, text, x, y);
        font.setColor(Color.WHITE);
    }

    private void drawHudTextBoldCentered(SpriteBatch batch, BitmapFont font, String text, float centerX, float y, Color color, float scale) {
        font.getData().setScale(scale);
        hudLayout.setText(font, text);
        float x = centerX - hudLayout.width / 2f;
        font.setColor(color);
        font.draw(batch, text, x - 1.5f, y);
        font.draw(batch, text, x + 1.5f, y);
        font.draw(batch, text, x, y - 1.5f);
        font.draw(batch, text, x, y + 1.5f);
        font.draw(batch, text, x - 1.5f, y - 1.5f);
        font.draw(batch, text, x + 1.5f, y - 1.5f);
        font.draw(batch, text, x - 1.5f, y + 1.5f);
        font.draw(batch, text, x + 1.5f, y + 1.5f);
        font.draw(batch, text, x, y);
        font.draw(batch, text, x + 1.2f, y);
        font.setColor(Color.WHITE);
    }

    private void drawHudTextRight(SpriteBatch batch, BitmapFont font, String text, float rightX, float y, Color color, float scale) {
        font.getData().setScale(scale);
        hudLayout.setText(font, text);
        drawHudText(batch, font, text, rightX - hudLayout.width, y, color, scale);
    }

    private float campfireRatio(GameWorld world) {
        return world.campfire.health / GameConstants.CAMPFIRE_MAX_HEALTH;
    }

    private float playerHealthRatio(GameWorld world) {
        return world.player.health / GameConstants.MAX_HEALTH;
    }

    private float playerHungerRatio(GameWorld world) {
        return world.player.hunger / GameConstants.MAX_HUNGER;
    }
}
