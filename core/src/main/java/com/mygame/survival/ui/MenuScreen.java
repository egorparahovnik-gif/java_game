package com.mygame.survival.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

/**
 * Pixel-art styled main menu.
 *
 * Sprite-sheet layout of ui_menu.png (256×256):
 * Row 0 (y=0..31) : PLAY button – col 0 normal (0..95), col 1 hover (96..191)
 * Row 3 (y=192..255): three panel windows, each ~80×64 px
 * left-panel x=0, width=80
 * mid-panel x=80, width=80 ← used as the menu window background
 * right-panel x=160,width=80
 */
public final class MenuScreen {

    // ── sprite-sheet regions ──────────────────────────────────────────────
    // The entire sheet is 256×256.
    // Row heights: rows 0-2 are buttons/icons (~32 px each), row 3+ are panels.
    private static final int SHEET_W = 256;
    private static final int SHEET_H = 256;

    // PLAY button (96×32 each cell, two states side-by-side)
    private static final int BTN_SRC_X = 0;
    private static final int BTN_SRC_Y = 0; // top of sheet in image-coords
    private static final int BTN_SRC_W = 92;
    private static final int BTN_SRC_H = 33;
    private static final int BTN_HOVER_X = 96;

    // Panel / window (bottom part of the sheet, three panels)
    // Using the middle panel as the menu window.
    private static final int PANEL_SRC_X = 0;           // left panel
    private static final int PANEL_SRC_Y_TOP = 160;     // bottom row in the sheet
    private static final int PANEL_SRC_W = 100;
    private static final int PANEL_SRC_H = 100;

    // ── display sizes ─────────────────────────────────────────────────────
    // The panel is stretched to give a nice large menu window.
    private static final float WIN_SCALE = 4f; // pixel scale for window
    private static final float WIN_W = PANEL_SRC_W * WIN_SCALE; // 320
    private static final float WIN_H = PANEL_SRC_H * WIN_SCALE; // 256

    // Button is rendered at 3× (gives 288×96) – a bit large, so we trim height.
    private static final float BTN_SCALE = 1.2f;
    private static final float BTN_W = BTN_SRC_W * BTN_SCALE; // 288
    private static final float BTN_H = BTN_SRC_H * BTN_SCALE; // 96

    // ── state ─────────────────────────────────────────────────────────────
    private Texture sheet;
    private TextureRegion btnNormal;
    private TextureRegion btnHover;
    private TextureRegion panelRegion;
    private ShapeRenderer shapeRenderer;

    private final OrthographicCamera uiCamera = new OrthographicCamera();
    private final Viewport viewport = new FitViewport(680, 480, uiCamera);
    private final Vector3 tmpVec = new Vector3();
    private final Rectangle btnRect = new Rectangle();

    private boolean playClicked = false;

    // ── life-cycle ────────────────────────────────────────────────────────

    public void create() {
        sheet = new Texture(Gdx.files.internal("ui_menu.png"));
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        // LibGDX TextureRegion uses image-space (y=0 at top of image).
        btnNormal = new TextureRegion(sheet, BTN_SRC_X, BTN_SRC_Y, BTN_SRC_W, BTN_SRC_H);
        btnHover = new TextureRegion(sheet, BTN_HOVER_X, BTN_SRC_Y, BTN_SRC_W, BTN_SRC_H);
        panelRegion = new TextureRegion(sheet, PANEL_SRC_X, PANEL_SRC_Y_TOP, PANEL_SRC_W, PANEL_SRC_H);

        shapeRenderer = new ShapeRenderer();

        viewport.apply();
        uiCamera.position.set(400, 300, 0);
    }

    public void resize(int screenW, int screenH) {
        viewport.update(screenW, screenH, true);
    }

    /**
     * Returns true the first time the Play button is pressed.
     * Caller should reset this by calling {@link #consumePlayPressed()}.
     */
    public boolean update() {
        playClicked = false;

        // ESC -> start game (same as Play button)
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            playClicked = true;
        }

        // Convert mouse to world coords
        tmpVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tmpVec);

        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        float aspect = PANEL_SRC_W / (float) PANEL_SRC_H;

        float winW = screenW * 0.6f;
        float winH = winW / aspect;

        if (winH > screenH * 0.6f) {
            winH = screenH * 0.6f;
            winW = winH * aspect;
        }

        float winX = (screenW - winW) / 2f;
        float winY = (screenH - winH) / 2f;

        float btnX = winX + winW / 2f - BTN_W / 2f + 10f;
        float btnY = winY + winH / 2f - BTN_H / 2f;

        btnRect.set(btnX, btnY, BTN_W, BTN_H);

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (btnRect.contains(tmpVec.x, tmpVec.y)) {
                playClicked = true;
            }
        }
        return playClicked;
    }

    public void render(SpriteBatch batch) {
        float screenW = viewport.getWorldWidth();
        float screenH = viewport.getWorldHeight();

        // Keep aspect ratio of the panel
        float aspect = PANEL_SRC_W / (float) PANEL_SRC_H;

        float winW = screenW * 0.6f;
        float winH = winW / aspect;

        if (winH > screenH * 0.6f) {
            winH = screenH * 0.6f;
            winW = winH * aspect;
        }

        float winX = (screenW - winW) / 2f;
        float winY = (screenH - winH) / 2f;

        float btnX = winX + winW / 2f - BTN_W / 2f + 3f;
        float btnY = winY + winH / 2f - BTN_H / 2f;

        tmpVec.set(Gdx.input.getX(), Gdx.input.getY(), 0);
        viewport.unproject(tmpVec);
        boolean hovered = btnRect.contains(tmpVec.x, tmpVec.y);

        // ── full-screen dark overlay ──────────────────────────────────────
        float sw = viewport.getWorldWidth();
        float sh = viewport.getWorldHeight();

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setProjectionMatrix(viewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.6f);
        shapeRenderer.rect(0, 0, sw, sh);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);

        // ── menu panel & button ───────────────────────────────────────────
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();

        // Drop-shadow behind the window
        batch.setColor(0f, 0f, 0f, 0.45f);
        batch.draw(panelRegion,
                winX + 8f, winY - 8f,
                winW, winH);

        // Menu window
        batch.setColor(Color.WHITE);
        batch.draw(panelRegion, winX, winY, winW, winH);

        // Play button
        if (hovered) {
            float scaleExtra = 1.08f;
            float bwS = BTN_W * scaleExtra;
            float bhS = BTN_H * scaleExtra;
            batch.draw(btnHover,
                    btnX - (bwS - BTN_W) / 2f,
                    btnY - (bhS - BTN_H) / 2f,
                    bwS, bhS);
        } else {
            batch.draw(btnNormal, btnX, btnY, BTN_W, BTN_H);
        }

        batch.end();
    }

    public void dispose() {
        if (sheet != null) sheet.dispose();
        if (shapeRenderer != null) shapeRenderer.dispose();
    }
}
