package com.mygame.survival.game.entity;

import com.badlogic.gdx.math.Rectangle;
import com.mygame.survival.game.GameConstants;

public final class Branch {
    public Rectangle bounds;
    public final float drawX;
    public final float drawY;

    public boolean collected = false;
    public float respawnTimeSec = 0f;

    public Branch(float x, float y) {
        this.drawX = x;
        this.drawY = y;
        float size = GameConstants.BRANCH_COLLIDER_SIZE;
        this.bounds = new Rectangle(
            x + (GameConstants.BRANCH_DRAW_SIZE - size) / 2f,
            y + (GameConstants.BRANCH_DRAW_SIZE - size) / 2f,
            size,
            size
        );
    }
}
