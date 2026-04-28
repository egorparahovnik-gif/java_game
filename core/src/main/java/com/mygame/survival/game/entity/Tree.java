package com.mygame.survival.game.entity;

import com.badlogic.gdx.math.Rectangle;

public final class Tree {
    public final Rectangle bounds;
    public final float drawX;
    public final float drawY;

    public Tree(Rectangle bounds, float drawX, float drawY) {
        this.bounds = bounds;
        this.drawX = drawX;
        this.drawY = drawY;
    }
}

