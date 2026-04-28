package com.mygame.survival.game;

public final class GameConstants {
    private GameConstants() {}

    public static final int TILE_SIZE = 16;
    public static final int MAP_WIDTH_TILES = 200;
    public static final int MAP_HEIGHT_TILES = 200;

    public static final float WORLD_WIDTH = MAP_WIDTH_TILES * TILE_SIZE;
    public static final float WORLD_HEIGHT = MAP_HEIGHT_TILES * TILE_SIZE;

    public static final float PLAYER_DRAW_SIZE = 110f;
    // Smaller, more natural collision than full sprite.
    public static final float PLAYER_HITBOX_SIZE = 55f;

    public static final float CAMPFIRE_X = 1600f;
    public static final float CAMPFIRE_Y = 1600f;
    // Collision near the base of the campfire sprite.
    public static final float CAMPFIRE_BLOCK_OFFSET_X = 26f;
    public static final float CAMPFIRE_BLOCK_OFFSET_Y = 18f;
    public static final float CAMPFIRE_BLOCK_WIDTH = 18f;
    public static final float CAMPFIRE_BLOCK_HEIGHT = 14f;
    
    // Campfire light constants
    public static final float CAMPFIRE_MAX_HEALTH = 100f;
    public static final float CAMPFIRE_MIN_LIGHT_RADIUS = 350f;
    public static final float CAMPFIRE_MAX_LIGHT_RADIUS = 800f;
    public static final float CAMPFIRE_MIN_LIGHT_INTENSITY = 0.25f;
    public static final float CAMPFIRE_MAX_LIGHT_INTENSITY = 0.60f;

    public static final int MAX_CARRIED_WOOD = 1;
    public static final float BRANCH_RESPAWN_TIME_SEC = 120f;

    public static final float DAY_LENGTH_SEC = 240f;

    public static final int TREE_DRAW_SIZE = 180;
    // Tree collision is only the trunk area near the bottom.
    public static final float TREE_TRUNK_COLLIDER_WIDTH = 26f;
    public static final float TREE_TRUNK_COLLIDER_HEIGHT = 18f;
    public static final float TREE_TRUNK_COLLIDER_OFFSET_Y = 26f;
    public static final int BRANCH_DRAW_SIZE = 30;
    // Branch pickup collider, closer to the sprite size for easier interaction.
    public static final float BRANCH_COLLIDER_SIZE = 20f;
    public static final int GRASS_DRAW_SIZE = 70;
    public static final int CAMPFIRE_DRAW_SIZE = 70;

    // Berry bush constants
    public static final int BERRY_DRAW_SIZE = 55;
    // Berry bush collider, closer to the bush sprite size.
    public static final float BERRY_COLLIDER_SIZE = 40f;
    public static final float BERRY_RESPAWN_TIME_SEC = 120f;

    // Hunger system constants
    public static final float MAX_HUNGER = 120f;
    public static final float HUNGER_DRAIN_RATE = 1f; // за 20 секунд голод опустится на MAX_HUNGER
    public static final float HUNGER_RESTORE = 24f; // сколько восполняет одна ягода
    public static final float GAME_OVER_HUNGER_THRESHOLD = 0f;
    
    // Health system constants
    public static final float MAX_HEALTH = 120f;
    public static final float HEALTH_DAMAGE_RATE = 4f; // урон в секунду при нулевом голоде
    public static final float HEALTH_RESTORE = 24f;
    public static final float HUNGER_SLOWDOWN_THRESHOLD = 0f; // при этом голоде персонаж замедляется
    public static final float SLOWDOWN_MULTIPLIER = 0.7f; // множитель скорости при голоде

    public static final int MIN_DISTANCE_TILES = 8;
}
