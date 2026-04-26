package com.mygame.survival.gfx;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

public final class GameAssets {
    public Texture bgTexture;
    public Texture treeTexture;
    public Texture branchTexture;
    public Texture grassTexture;
    public Texture berryTexture;

    public Texture walkSheet;
    public Texture fireSheet;
    public Texture fireOffSheet;

    public Texture fireGlowTexture;

    public Animation<TextureRegion> walkAnimation;
    public TextureRegion idleFrame;

    public Animation<TextureRegion> fireAnimation;
    public Animation<TextureRegion> fireOffAnimation;

    public void load() {
        bgTexture = new Texture("bg.png");
        treeTexture = new Texture("tree.png");
        branchTexture = new Texture("branch.png");
        grassTexture = new Texture("grass.png");
        berryTexture = new Texture("berries_bush.png");

        walkSheet = new Texture("player_walk_sheet.png");
        TextureRegion[] walkFrames = splitHorizontal(walkSheet, 6);
        idleFrame = walkFrames[0];
        walkAnimation = new Animation<>(0.15f, walkFrames);

        fireSheet = new Texture("fire_animated.png");
        TextureRegion[] fireFrames = splitHorizontal(fireSheet, 6);
        fireAnimation = new Animation<>(0.15f, fireFrames);

        fireOffSheet = new Texture("fire_off.png");
        TextureRegion[] offFrames = splitHorizontal(fireOffSheet, 6);
        fireOffAnimation = new Animation<>(0.20f, offFrames);

        // Low-res + nearest filtering => pixelated glow.
        fireGlowTexture = createRadialGlowTexture(128);
    }

    public void dispose() {
        if (bgTexture != null) bgTexture.dispose();
        if (treeTexture != null) treeTexture.dispose();
        if (branchTexture != null) branchTexture.dispose();
        if (grassTexture != null) grassTexture.dispose();
        if (berryTexture != null) berryTexture.dispose();
        if (walkSheet != null) walkSheet.dispose();
        if (fireSheet != null) fireSheet.dispose();
        if (fireOffSheet != null) fireOffSheet.dispose();
        if (fireGlowTexture != null) fireGlowTexture.dispose();
    }

    private TextureRegion[] splitHorizontal(Texture sheet, int frames) {
        int frameWidth = sheet.getWidth() / frames;
        int frameHeight = sheet.getHeight();
        TextureRegion[] regions = new TextureRegion[frames];
        for (int i = 0; i < frames; i++) {
            regions[i] = new TextureRegion(sheet, i * frameWidth, 0, frameWidth, frameHeight);
        }
        return regions;
    }

    private Texture createRadialGlowTexture(int size) {
        Pixmap pixmap = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pixmap.setBlending(Pixmap.Blending.None);
        float cx = (size - 1) / 2f;
        float cy = (size - 1) / 2f;
        float maxR = Math.min(cx, cy);

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = (x - cx) / maxR;
                float dy = (y - cy) / maxR;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                float t = MathUtils.clamp(d, 0f, 1f);
                // Smooth falloff, then quantize => pixel-art banding.
                float s = t * t * (3f - 2f * t);      // smoothstep(0..1)
                float alpha = (float) Math.pow(1f - s, 2.2f);
                alpha = quantize(alpha, 18);
                int a = (int) (255f * alpha);
                pixmap.drawPixel(x, y, (0xFF << 24) | (0xFF << 16) | (0xFF << 8) | (a & 0xFF));
            }
        }

        Texture tex = new Texture(pixmap);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return tex;
    }

    private float quantize(float v, int steps) {
        if (steps <= 1) return MathUtils.clamp(v, 0f, 1f);
        float clamped = MathUtils.clamp(v, 0f, 1f);
        float q = Math.round(clamped * (steps - 1)) / (float) (steps - 1);
        return q;
    }
}

