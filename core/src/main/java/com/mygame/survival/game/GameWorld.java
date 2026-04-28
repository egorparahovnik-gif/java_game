package com.mygame.survival.game;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.Array;
import com.mygame.survival.game.entity.Berry;
import com.mygame.survival.game.entity.Branch;
import com.mygame.survival.game.entity.Campfire;
import com.mygame.survival.game.entity.Grass;
import com.mygame.survival.game.entity.Player;
import com.mygame.survival.game.entity.Tree;

public final class GameWorld {
    public final Player player = new Player(1650f, 1600f);
    public final Campfire campfire = new Campfire(GameConstants.CAMPFIRE_X, GameConstants.CAMPFIRE_Y);

    public final Array<Tree> trees = new Array<>();
    public final Array<Branch> branches = new Array<>();
    public final Array<Grass> grasses = new Array<>();
    public final Array<Berry> berries = new Array<>();

    public float dayNightTimeSec = 0f;
    public float fireAnimTimeSec = 0f;
    public boolean gameOver = false;

    public void reset() {
        player.x = 1650f;
        player.y = 1600f;
        player.moving = false;
        player.facingRight = true;
        player.walkStateTimeSec = 0f;
        player.carriedWood = 0;
        player.hunger = GameConstants.MAX_HUNGER;
        player.health = GameConstants.MAX_HEALTH;

        campfire.health = 0f;
        campfire.hasBeenLit = false;

        dayNightTimeSec = 0f;
        fireAnimTimeSec = 0f;
        gameOver = false;

        generateWorld();
    }

    public void update(float dt) {
        if (gameOver) return;

        dayNightTimeSec += dt;
        fireAnimTimeSec += dt;

        // Update hunger - decreases over time
        player.hunger -= GameConstants.HUNGER_DRAIN_RATE * dt;
        if (player.hunger < 0f) player.hunger = 0f;

        // If hunger is zero, player takes damage
        if (player.hunger <= GameConstants.HUNGER_SLOWDOWN_THRESHOLD) {
            player.health -= GameConstants.HEALTH_DAMAGE_RATE * dt;
            if (player.health < 0f) player.health = 0f;
        }

        if (campfire.hasBeenLit && campfire.health > 0f) {
            campfire.health -= 1f * dt;
            if (campfire.health < 0f) campfire.health = 0f;
        }

        for (Branch branch : branches) {
            if (branch.collected) {
                branch.respawnTimeSec -= dt;
                if (branch.respawnTimeSec <= 0f) {
                    branch.collected = false;
                    branch.respawnTimeSec = 0f;
                }
            }
        }

        for (Berry berry : berries) {
            if (berry.collected) {
                berry.respawnTimeSec -= dt;
                if (berry.respawnTimeSec <= 0f) {
                    berry.collected = false;
                    berry.respawnTimeSec = 0f;
                }
            }
        }

        // Check if it's dark night and the campfire is not burning - game over
        if (isNightTime() && campfire.health <= 0f) {
            gameOver = true;
        }

        // Check if player is dead - game over
        if (player.health <= 0f) {
            gameOver = true;
        }

    }

    public boolean isNightTime() {
        float t = (dayNightTimeSec % GameConstants.DAY_LENGTH_SEC) / GameConstants.DAY_LENGTH_SEC; // 0..1
        float daylight = 0.5f + 0.5f * MathUtils.cos(MathUtils.PI2 * t);
        return daylight < 0.05f;
    }

    public float worldWidth() {
        return GameConstants.WORLD_WIDTH;
    }

    public float worldHeight() {
        return GameConstants.WORLD_HEIGHT;
    }

    public boolean isBlocked(Rectangle futureBounds) {
        for (Tree tree : trees) {
            if (futureBounds.overlaps(tree.bounds)) return true;
        }

        for (Branch branch : branches) {
            if (!branch.collected && futureBounds.overlaps(branch.bounds)) return true;
        }

        for (Berry berry : berries) {
            if (!berry.collected && futureBounds.overlaps(berry.bounds)) return true;
        }

        return futureBounds.overlaps(campfire.blockBounds);
    }

    private void generateWorld() {
        trees.clear();
        branches.clear();
        grasses.clear();
        berries.clear();

        boolean[][] occupied = new boolean[GameConstants.MAP_WIDTH_TILES][GameConstants.MAP_HEIGHT_TILES];

        int fireTileX = (int)(GameConstants.CAMPFIRE_X / GameConstants.TILE_SIZE);
        int fireTileY = (int)(GameConstants.CAMPFIRE_Y / GameConstants.TILE_SIZE);

        // Hard exclusion zone around campfire (nothing spawns here).
        final int noSpawnRadiusTiles = 14;

        // More uniform distribution: spawn per chunk to avoid large empty zones.
        final int chunkSize = 10; // tiles
        final int chunksX = (int) Math.ceil(GameConstants.MAP_WIDTH_TILES / (float) chunkSize);
        final int chunksY = (int) Math.ceil(GameConstants.MAP_HEIGHT_TILES / (float) chunkSize);
        final int chunkCount = chunksX * chunksY;

        // Shuffle chunk processing order => more chaotic distribution.
        int[] chunkOrder = new int[chunkCount];
        for (int i = 0; i < chunkCount; i++) chunkOrder[i] = i;
        for (int i = chunkCount - 1; i > 0; i--) {
            int j = MathUtils.random(0, i);
            int tmp = chunkOrder[i];
            chunkOrder[i] = chunkOrder[j];
            chunkOrder[j] = tmp;
        }

        // Approx max distance from fire to map corner, in tiles.
        float maxDx = Math.max(fireTileX, GameConstants.MAP_WIDTH_TILES - 1 - fireTileX);
        float maxDy = Math.max(fireTileY, GameConstants.MAP_HEIGHT_TILES - 1 - fireTileY);
        float maxDist = (float) Math.sqrt(maxDx * maxDx + maxDy * maxDy);

        for (int orderIdx = 0; orderIdx < chunkCount; orderIdx++) {
            int chunkIndex = chunkOrder[orderIdx];
            int cx = chunkIndex % chunksX;
            int cy = chunkIndex / chunksX;

            int startX = cx * chunkSize;
            int startY = cy * chunkSize;
                int endX = Math.min(GameConstants.MAP_WIDTH_TILES, startX + chunkSize);
                int endY = Math.min(GameConstants.MAP_HEIGHT_TILES, startY + chunkSize);

                float chunkCenterX = (startX + endX - 1) / 2f;
                float chunkCenterY = (startY + endY - 1) / 2f;
                float dx = chunkCenterX - fireTileX;
                float dy = chunkCenterY - fireTileY;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);

                float dist01 = (dist - noSpawnRadiusTiles) / Math.max(1f, (maxDist - noSpawnRadiusTiles));
                dist01 = MathUtils.clamp(dist01, 0f, 1f);

                // Density ramps up with distance.
                float treesBase = MathUtils.lerp(0.75f, 1.55f, dist01);
                int treesTarget = Math.round(treesBase + MathUtils.random(-0.40f, 1.10f));
                treesTarget = MathUtils.clamp(treesTarget, 0, 4);
                // Branches should be present across the whole map (including far away).
                float branchesBase = MathUtils.lerp(0.45f, 0.80f, dist01);
                int branchesTarget = Math.round(branchesBase + MathUtils.random(-0.35f, 0.60f));
                branchesTarget = MathUtils.clamp(branchesTarget, 0, 3);

                float grassBase = MathUtils.lerp(0.6f, 1.8f, dist01);
                int grassTarget = Math.round(grassBase + MathUtils.random(-0.9f, 0.9f));
                grassTarget = MathUtils.clamp(grassTarget, 0, 4);

                // Rare "pockets" to break regularity.
                if (MathUtils.random() < 0.08f) treesTarget += MathUtils.random(1, 2);
                if (MathUtils.random() < 0.06f) branchesTarget += 1;
                if (MathUtils.random() < 0.05f) grassTarget += 1;
                treesTarget = MathUtils.clamp(treesTarget, 0, 6);
                branchesTarget = MathUtils.clamp(branchesTarget, 0, 4);
                grassTarget = MathUtils.clamp(grassTarget, 0, 5);

                // Trees: bigger spacing, but more overall.
                for (int i = 0; i < treesTarget; i++) {
                    int minDist = Math.round(MathUtils.lerp(7f, 4f, dist01));
                    int[] tile = pickInChunk(occupied, fireTileX, fireTileY, noSpawnRadiusTiles, startX, startY, endX, endY, minDist, 60);
                    if (tile == null) continue;
                    placeTree(occupied, tile[0], tile[1]);
                }

                // Branches: smaller spacing but fewer items than before.
                for (int i = 0; i < branchesTarget; i++) {
                    int minDist = Math.round(MathUtils.lerp(4f, 2f, dist01));
                    int[] tile = pickInChunk(occupied, fireTileX, fireTileY, noSpawnRadiusTiles, startX, startY, endX, endY, minDist, 45);
                    if (tile == null) continue;
                    placeBranch(occupied, tile[0], tile[1]);
                }

                // Grass: fills visual gaps. Small spacing and higher count.
                for (int i = 0; i < grassTarget; i++) {
                    int minDist = Math.round(MathUtils.lerp(3f, 1f, dist01));
                    int[] tile = pickInChunk(occupied, fireTileX, fireTileY, noSpawnRadiusTiles, startX, startY, endX, endY, minDist, 35);
                    if (tile == null) continue;
                    placeGrass(occupied, tile[0], tile[1]);
                }

                // Berries: denser than before so berry bushes are easier to find.
                float berriesBase = MathUtils.lerp(0.8f, 1.8f, dist01);
                int berriesTarget = Math.round(berriesBase + MathUtils.random(-0.2f, 0.8f));
                berriesTarget = MathUtils.clamp(berriesTarget, 0, 4);
                for (int j = 0; j < berriesTarget; j++) {
                    int minDist = Math.round(MathUtils.lerp(4f, 2f, dist01));
                    int[] tile = pickInChunk(occupied, fireTileX, fireTileY, noSpawnRadiusTiles, startX, startY, endX, endY, minDist, 40);
                    if (tile == null) continue;
                    placeBerry(occupied, tile[0], tile[1]);
                }
        }

        // Second pass: sprinkle extra grass on remaining free tiles (prevents "empty" areas).
        // Keep it bounded so render cost stays reasonable.
        int extraGrassBudget = 80;
        for (int k = 0; k < extraGrassBudget; k++) {
            int tx = MathUtils.random(0, GameConstants.MAP_WIDTH_TILES - 1);
            int ty = MathUtils.random(0, GameConstants.MAP_HEIGHT_TILES - 1);
            if (occupied[tx][ty]) continue;
            float dist = tileDistance(tx, ty, fireTileX, fireTileY);
            if (dist < noSpawnRadiusTiles) continue;

            float dist01 = (dist - noSpawnRadiusTiles) / Math.max(1f, (maxDist - noSpawnRadiusTiles));
            dist01 = MathUtils.clamp(dist01, 0f, 1f);
            float chance = MathUtils.lerp(0.05f, 0.22f, dist01);
            if (MathUtils.random() > chance) continue;
            if (isTooCloseToAny(occupied, tx, ty, 1)) continue;

            placeGrass(occupied, tx, ty);
        }
    }

    private int[] pickInChunk(
        boolean[][] occupied,
        int fireTileX,
        int fireTileY,
        int noSpawnRadiusTiles,
        int startX,
        int startY,
        int endX,
        int endY,
        int minDistanceTiles,
        int attempts
    ) {
        for (int a = 0; a < attempts; a++) {
            int tx = MathUtils.random(startX, endX - 1);
            int ty = MathUtils.random(startY, endY - 1);
            if (occupied[tx][ty]) continue;
            if (tileDistance(tx, ty, fireTileX, fireTileY) < noSpawnRadiusTiles) continue;
            if (isTooCloseToAny(occupied, tx, ty, minDistanceTiles)) continue;
            return new int[] { tx, ty };
        }
        return null;
    }

    private float tileDistance(float ax, float ay, float bx, float by) {
        float dx = ax - bx;
        float dy = ay - by;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void placeTree(boolean[][] occupied, int tileX, int tileY) {
        occupied[tileX][tileY] = true;
        float worldX = tileX * GameConstants.TILE_SIZE;
        float worldY = tileY * GameConstants.TILE_SIZE;
        float trunkW = GameConstants.TREE_TRUNK_COLLIDER_WIDTH;
        float trunkH = GameConstants.TREE_TRUNK_COLLIDER_HEIGHT;
        Rectangle bounds = new Rectangle(
            worldX + (GameConstants.TREE_DRAW_SIZE - trunkW) / 2f,
            worldY + GameConstants.TREE_TRUNK_COLLIDER_OFFSET_Y,
            trunkW,
            trunkH
        );
        trees.add(new Tree(bounds, worldX, worldY));
    }

    private void placeBranch(boolean[][] occupied, int tileX, int tileY) {
        occupied[tileX][tileY] = true;
        float size = GameConstants.TILE_SIZE * 0.4f;
        float worldX = tileX * GameConstants.TILE_SIZE;
        float worldY = tileY * GameConstants.TILE_SIZE;
        Branch branch = new Branch(worldX, worldY);
        float offsetX = size * 1.3f; // shift right
        float offsetY = size * 1.6f;  // shift up

        branch.bounds = new Rectangle(
            worldX + (GameConstants.TILE_SIZE - size) / 2f + offsetX,
            worldY + (GameConstants.TILE_SIZE - size) / 2f + offsetY,
            size,
            size
        );
        branches.add(branch);
    }

    private void placeGrass(boolean[][] occupied, int tileX, int tileY) {
        occupied[tileX][tileY] = true;
        grasses.add(new Grass(tileX * GameConstants.TILE_SIZE, tileY * GameConstants.TILE_SIZE));
    }

    private void placeBerry(boolean[][] occupied, int tileX, int tileY) {
        occupied[tileX][tileY] = true;
        float size = GameConstants.TILE_SIZE * 0.6f;
        float worldX = tileX * GameConstants.TILE_SIZE;
        float worldY = tileY * GameConstants.TILE_SIZE;
        Berry berry = new Berry(worldX, worldY);
        float offsetX = size * 2.2f; // shift right
        float offsetY = size * 3f; // shift up

        berry.bounds = new Rectangle(
            worldX + (GameConstants.TILE_SIZE - size) / 2f + offsetX,
            worldY + (GameConstants.TILE_SIZE - size) / 2f + offsetY,
            size,
            size
        );
        berries.add(berry);
    }

    private boolean isTooCloseToAny(boolean[][] occupied, int tx, int ty, int minDistanceTiles) {
        for (int x = Math.max(0, tx - minDistanceTiles); x <= Math.min(GameConstants.MAP_WIDTH_TILES - 1, tx + minDistanceTiles); x++) {
            for (int y = Math.max(0, ty - minDistanceTiles); y <= Math.min(GameConstants.MAP_HEIGHT_TILES - 1, ty + minDistanceTiles); y++) {
                if (occupied[x][y]) return true;
            }
        }
        return false;
    }
}
