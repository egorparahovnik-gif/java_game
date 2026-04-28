package com.mygame.survival.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.math.Rectangle;
import com.mygame.survival.game.GameConstants;
import com.mygame.survival.game.GameWorld;
import com.mygame.survival.game.entity.Berry;
import com.mygame.survival.game.entity.Branch;

public final class PlayerController {
    public void update(GameWorld world, float dt) {

        handleMovement(world, dt);
        handleInteraction(world);
    }

    private void handleMovement(GameWorld world, float dt) {
        float speed = 110f * dt;
        
        // Apply slowdown if hunger is zero
        if (world.player.hunger <= GameConstants.HUNGER_SLOWDOWN_THRESHOLD) {
            speed *= GameConstants.SLOWDOWN_MULTIPLIER;
        }
        
        float newX = world.player.x;
        float newY = world.player.y;
        world.player.moving = false;

        if (Gdx.input.isKeyPressed(Input.Keys.W)) { newY += speed; world.player.moving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) { newY -= speed; world.player.moving = true; }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) { newX -= speed; world.player.moving = true; world.player.facingRight = false; }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) { newX += speed; world.player.moving = true; world.player.facingRight = true; }

        float hitboxSize = GameConstants.PLAYER_HITBOX_SIZE;
        float drawSize = GameConstants.PLAYER_DRAW_SIZE;
        float offset = (drawSize - hitboxSize) / 2f;

        Rectangle futureX = new Rectangle(newX + offset, world.player.y + offset, hitboxSize, hitboxSize);
        if (!world.isBlocked(futureX)) {
            world.player.x = clamp(newX, 0f, world.worldWidth() - drawSize);
        }

        Rectangle futureY = new Rectangle(world.player.x + offset, newY + offset, hitboxSize, hitboxSize);
        if (!world.isBlocked(futureY)) {
            world.player.y = clamp(newY, 0f, world.worldHeight() - drawSize);
        }

        if (world.player.moving) {
            world.player.walkStateTimeSec += dt;
        }
    }

    private void handleInteraction(GameWorld world) {
        if (!Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) return;

        Rectangle playerBounds = new Rectangle(world.player.x, world.player.y, GameConstants.PLAYER_DRAW_SIZE, GameConstants.PLAYER_DRAW_SIZE);

        // Collect berries (only if not at max hunger)
        for (Berry berry : world.berries) {
            if (berry.collected) continue;
            if (!playerBounds.overlaps(berry.bounds)) continue;
            
            // Cannot collect berries if at max hunger
            if (world.player.hunger >= GameConstants.MAX_HUNGER) continue;

            berry.collected = true;
            berry.respawnTimeSec = GameConstants.BERRY_RESPAWN_TIME_SEC;
            world.player.hunger = Math.min(GameConstants.MAX_HUNGER, world.player.hunger + GameConstants.HUNGER_RESTORE);
            world.player.health = Math.min(GameConstants.MAX_HEALTH, world.player.health + GameConstants.HEALTH_RESTORE);
        }

        for (Branch branch : world.branches) {
            if (branch.collected) continue;
            if (!playerBounds.overlaps(branch.bounds)) continue;
            if (world.player.carriedWood >= GameConstants.MAX_CARRIED_WOOD) continue;

            branch.collected = true;
            branch.respawnTimeSec = GameConstants.BRANCH_RESPAWN_TIME_SEC;
            world.player.carriedWood++;
        }

        if (playerBounds.overlaps(world.campfire.blockBounds) && world.player.carriedWood > 0) {
            world.campfire.addWood(world.player.carriedWood);
            world.player.carriedWood = 0;
        }
    }

    private float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}

