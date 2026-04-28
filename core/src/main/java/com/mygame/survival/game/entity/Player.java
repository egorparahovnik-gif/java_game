package com.mygame.survival.game.entity;

import com.mygame.survival.game.GameConstants;

public final class Player {
    public float x;
    public float y;

    public boolean moving;
    public boolean facingRight = true;
    public float walkStateTimeSec = 0f;

    public int carriedWood = 0;
    public float hunger = GameConstants.MAX_HUNGER; // 0 = голодный, MAX = сытый
    public float health = GameConstants.MAX_HEALTH; // 0 = мёртв, MAX = полное здоровье

    public Player(float x, float y) {
        this.x = x;
        this.y = y;
        this.hunger = GameConstants.MAX_HUNGER;
        this.health = GameConstants.MAX_HEALTH;
    }

    public float drawSize() {
        return GameConstants.PLAYER_DRAW_SIZE;
    }
}

