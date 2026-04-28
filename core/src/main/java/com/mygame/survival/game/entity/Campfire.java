package com.mygame.survival.game.entity;

import com.badlogic.gdx.math.Rectangle;
import com.mygame.survival.game.GameConstants;

public final class Campfire {
    public final float x;
    public final float y;
    public final Rectangle blockBounds;

    public float health = 0f; // 0..100
    public boolean hasBeenLit = false;

    public Campfire(float x, float y) {
        this.x = x;
        this.y = y;
        this.blockBounds = new Rectangle(
            x + GameConstants.CAMPFIRE_BLOCK_OFFSET_X,
            y + GameConstants.CAMPFIRE_BLOCK_OFFSET_Y,
            GameConstants.CAMPFIRE_BLOCK_WIDTH,
            GameConstants.CAMPFIRE_BLOCK_HEIGHT
        );
    }

    public void addWood(int pieces) {
        if (pieces <= 0) return;
        health += pieces * 20f;
        if (health > 100f) health = 100f;
        hasBeenLit = true;
    }
}

