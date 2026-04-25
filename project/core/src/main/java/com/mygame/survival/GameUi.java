package com.mygame.survival;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameUi {
    public enum Action {
        NONE,
        OPEN_MENU,
        NEW_GAME,
        CONTINUE
    }

    private final GlyphLayout glyphLayout = new GlyphLayout();
    private final Rectangle menuButtonBounds = new Rectangle();
    private final Rectangle continueButtonBounds = new Rectangle();
    private final Rectangle newGameButtonBounds = new Rectangle();
    private final Vector3 hoverPoint = new Vector3();

    private float menuButtonHover = 0f;
    private float continueButtonHover = 0f;
    private float newGameButtonHover = 0f;
    private boolean menuButtonHovered = false;
    private boolean continueButtonHovered = false;
    private boolean newGameButtonHovered = false;
    private float menuButtonPress = 0f;
    private float continueButtonPress = 0f;
    private float newGameButtonPress = 0f;

    private final Color panelBorder = new Color(0.34f, 0.20f, 0.10f, 1f);
    private final Color panelFill = new Color(0.95f, 0.84f, 0.60f, 1f);
    private final Color panelShadow = new Color(0.73f, 0.57f, 0.31f, 1f);
    private final Color fireBarColor = new Color(0.90f, 0.43f, 0.22f, 1f);
    private final Color fireBarBack = new Color(0.42f, 0.22f, 0.12f, 1f);
    private final Color inventoryBarColor = new Color(0.51f, 0.68f, 0.30f, 1f);
    private final Color inventoryBarBack = new Color(0.34f, 0.27f, 0.18f, 1f);

    public void update(OrthographicCamera camera, Viewport viewport, boolean showMenu, boolean continueEnabled, boolean playing) {
        updateLayout(camera, viewport);
        float delta = Gdx.graphics.getDeltaTime();

        menuButtonHovered = playing && containsPointer(menuButtonBounds, viewport);
        newGameButtonHovered = showMenu && containsPointer(newGameButtonBounds, viewport);
        continueButtonHovered = showMenu && continueEnabled && containsPointer(continueButtonBounds, viewport);

        menuButtonHover = approach(menuButtonHover, menuButtonHovered ? 1f : 0f, delta * 10f);
        newGameButtonHover = approach(newGameButtonHover, newGameButtonHovered ? 1f : 0f, delta * 10f);
        continueButtonHover = approach(continueButtonHover, continueButtonHovered ? 1f : 0f, delta * 10f);

        menuButtonPress = Math.max(0f, menuButtonPress - delta * 4f);
        newGameButtonPress = Math.max(0f, newGameButtonPress - delta * 4f);
        continueButtonPress = Math.max(0f, continueButtonPress - delta * 4f);
    }

    public Action handleClick(int screenX, int screenY, OrthographicCamera camera, Viewport viewport, boolean showMenu, boolean continueEnabled, boolean playing) {
        hoverPoint.set(screenX, screenY, 0f);
        viewport.unproject(hoverPoint);

        if (playing && menuButtonBounds.contains(hoverPoint.x, hoverPoint.y)) {
            menuButtonPress = 1f;
            return Action.OPEN_MENU;
        }

        if (!showMenu) {
            return Action.NONE;
        }

        if (newGameButtonBounds.contains(hoverPoint.x, hoverPoint.y)) {
            newGameButtonPress = 1f;
            continueButtonPress = 0f;
            menuButtonPress = 0f;
            return Action.NEW_GAME;
        }

        if (continueEnabled && continueButtonBounds.contains(hoverPoint.x, hoverPoint.y)) {
            continueButtonPress = 1f;
            menuButtonPress = 0f;
            newGameButtonPress = 0f;
            return Action.CONTINUE;
        }

        return Action.NONE;
    }

    public void render(
        SpriteBatch batch,
        ShapeRenderer shapeRenderer,
        OrthographicCamera camera,
        Viewport viewport,
        BitmapFont titleFont,
        BitmapFont bodyFont,
        boolean showMenu,
        boolean continueEnabled,
        boolean isGameOver,
        float fireHealth,
        int carriedWood,
        int maxWood,
        boolean playing
    ) {
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float top = bottom + viewport.getWorldHeight();
        float right = left + viewport.getWorldWidth();

        batch.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);

        drawStatusPanel(shapeRenderer, left + 22f, top - 108f, 280f, 84f);
        drawBar(shapeRenderer, left + 42f, top - 64f, 160f, 18f, fireHealth / 100f, fireBarBack, fireBarColor);
        drawBar(shapeRenderer, left + 42f, top - 92f, 160f, 18f, (float) carriedWood / maxWood, inventoryBarBack, inventoryBarColor);

        batch.begin();
        bodyFont.setColor(panelBorder);
        bodyFont.draw(batch, "Campfire", left + 42f, top - 42f);
        bodyFont.draw(batch, (int) fireHealth + "/100", left + 212f, top - 50f);
        bodyFont.draw(batch, "Backpack", left + 42f, top - 70f);
        bodyFont.draw(batch, carriedWood + "/" + maxWood, left + 212f, top - 78f);
        batch.end();

        if (playing) {
            drawButton(shapeRenderer, batch, bodyFont, menuButtonBounds, "MENU", true, menuButtonHovered, menuButtonHover, menuButtonPress);
        }

        if (showMenu) {
            drawMenuOverlay(shapeRenderer, batch, titleFont, bodyFont, left, bottom, right, top, continueEnabled, isGameOver);
        }
    }

    private void updateLayout(OrthographicCamera camera, Viewport viewport) {
        float left = camera.position.x - viewport.getWorldWidth() / 2f;
        float bottom = camera.position.y - viewport.getWorldHeight() / 2f;
        float top = bottom + viewport.getWorldHeight();
        float right = left + viewport.getWorldWidth();

        menuButtonBounds.set(right - 162f, top - 82f, 140f, 52f);
        float overlayWidth = 420f;
        float overlayHeight = 280f;
        float overlayX = left + (right - left - overlayWidth) / 2f;
        float overlayY = bottom + (top - bottom - overlayHeight) / 2f;
        continueButtonBounds.set(overlayX + 80f, overlayY + 112f, 260f, 52f);
        newGameButtonBounds.set(overlayX + 80f, overlayY + 48f, 260f, 52f);
    }

    private boolean containsPointer(Rectangle bounds, Viewport viewport) {
        hoverPoint.set(Gdx.input.getX(), Gdx.input.getY(), 0f);
        viewport.unproject(hoverPoint);
        return bounds.contains(hoverPoint.x, hoverPoint.y);
    }

    private float approach(float current, float target, float rate) {
        return current + (target - current) * MathUtils.clamp(rate, 0f, 1f);
    }

    private void drawMenuOverlay(
        ShapeRenderer shapeRenderer,
        SpriteBatch batch,
        BitmapFont titleFont,
        BitmapFont bodyFont,
        float left,
        float bottom,
        float right,
        float top,
        boolean continueEnabled,
        boolean isGameOver
    ) {
        float overlayWidth = 420f;
        float overlayHeight = 280f;
        float overlayX = left + (right - left - overlayWidth) / 2f;
        float overlayY = bottom + (top - bottom - overlayHeight) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.33f, 0.26f, 0.17f, 0.30f);
        shapeRenderer.rect(left, bottom, right - left, top - bottom);
        shapeRenderer.setColor(panelShadow);
        shapeRenderer.rect(overlayX + 8f, overlayY - 8f, overlayWidth, overlayHeight);
        shapeRenderer.setColor(panelBorder);
        shapeRenderer.rect(overlayX, overlayY, overlayWidth, overlayHeight);
        shapeRenderer.setColor(panelFill);
        shapeRenderer.rect(overlayX + 6f, overlayY + 6f, overlayWidth - 12f, overlayHeight - 12f);
        shapeRenderer.end();

        drawButton(shapeRenderer, batch, bodyFont, continueButtonBounds, "CONTINUE", continueEnabled, continueButtonHovered, continueButtonHover, continueButtonPress);
        drawButton(shapeRenderer, batch, bodyFont, newGameButtonBounds, "NEW GAME", true, newGameButtonHovered, newGameButtonHover, newGameButtonPress);

        batch.begin();
        titleFont.setColor(panelBorder);
        bodyFont.setColor(panelBorder);
        String title = isGameOver ? "Campfire Out" : "Moonlit Camp";
        glyphLayout.setText(titleFont, title);
        titleFont.draw(batch, glyphLayout, overlayX + (overlayWidth - glyphLayout.width) / 2f, overlayY + 228f);
        String subtitle = "A cozy night in the woods";
        glyphLayout.setText(bodyFont, subtitle);
        bodyFont.draw(batch, glyphLayout, overlayX + (overlayWidth - glyphLayout.width) / 2f, overlayY + 192f);
        if (!continueEnabled) {
            String hint = isGameOver ? "Continue is disabled after defeat" : "Continue will unlock after your first run";
            glyphLayout.setText(bodyFont, hint);
            bodyFont.draw(batch, glyphLayout, overlayX + (overlayWidth - glyphLayout.width) / 2f, overlayY + 28f);
        }
        batch.end();
    }

    private void drawStatusPanel(ShapeRenderer shapeRenderer, float x, float y, float width, float height) {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(panelShadow);
        shapeRenderer.rect(x + 6f, y - 6f, width, height);
        shapeRenderer.setColor(panelBorder);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.setColor(panelFill);
        shapeRenderer.rect(x + 4f, y + 4f, width - 8f, height - 8f);
        shapeRenderer.end();
    }

    private void drawBar(ShapeRenderer shapeRenderer, float x, float y, float width, float height, float progress, Color background, Color fill) {
        float p = MathUtils.clamp(progress, 0f, 1f);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(panelBorder);
        shapeRenderer.rect(x - 3f, y - 3f, width + 6f, height + 6f);
        shapeRenderer.setColor(background);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.setColor(fill);
        shapeRenderer.rect(x, y, width * p, height);
        shapeRenderer.end();
    }

    private void drawButton(
        ShapeRenderer shapeRenderer,
        SpriteBatch batch,
        BitmapFont font,
        Rectangle bounds,
        String label,
        boolean enabled,
        boolean hovered,
        float hover,
        float press
    ) {
        float hoverMix = MathUtils.clamp(hover, 0f, 1f);
        float pressMix = MathUtils.clamp(press, 0f, 1f);
        float scale = 1f + hoverMix * 0.06f - pressMix * 0.08f;
        float offsetX = bounds.width * (1f - scale) / 2f;
        float offsetY = bounds.height * (1f - scale) / 2f - pressMix * 5f;
        float drawX = bounds.x + offsetX;
        float drawY = bounds.y + offsetY;
        float drawW = bounds.width * scale;
        float drawH = bounds.height * scale;
        Color shadow = enabled ? new Color(0.76f, 0.58f, 0.28f, 1f) : new Color(0.57f, 0.50f, 0.42f, 1f);
        Color fill = enabled ? new Color(0.98f, 0.88f, 0.51f, 1f) : new Color(0.84f, 0.79f, 0.71f, 1f);
        if (hovered && enabled) {
            fill = new Color(1f, 0.99f, 0.74f, 1f);
            shadow = new Color(0.95f, 0.75f, 0.42f, 1f);
        }
        if (hovered && !enabled) {
            fill = new Color(0.97f, 0.94f, 0.86f, 1f);
            shadow = new Color(0.80f, 0.71f, 0.60f, 1f);
        }

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(shadow);
        shapeRenderer.rect(drawX + 4f, drawY - 4f, drawW, drawH);
        shapeRenderer.setColor(panelBorder);
        shapeRenderer.rect(drawX, drawY, drawW, drawH);
        shapeRenderer.setColor(fill);
        shapeRenderer.rect(drawX + 4f, drawY + 4f, drawW - 8f, drawH - 8f);
        shapeRenderer.end();

        batch.begin();
        Color labelColor = enabled ? new Color(panelBorder) : new Color(0.48f, 0.42f, 0.34f, 1f);
        if (hovered && enabled) {
            labelColor = new Color(0.22f, 0.11f, 0.04f, 1f);
        }
        if (hovered && !enabled) {
            labelColor = new Color(0.30f, 0.24f, 0.18f, 1f);
        }
        font.setColor(labelColor);
        glyphLayout.setText(font, label);
        font.draw(batch, glyphLayout, drawX + (drawW - glyphLayout.width) / 2f, drawY + drawH / 2f + glyphLayout.height / 2f + hoverMix * 1.5f);
        batch.end();
    }
}
