package com.flatworld.mod.event;

import com.flatworld.mod.dimension.ModDimensions;
import net.minecraft.world.entity.MobSpawnType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

/**
 * Prevents any natural mob spawning inside our flat dimension.
 * <p>
 * Mobs spawned explicitly (spawn eggs, /summon, commands, dispensers, etc.) are unaffected —
 * only NATURAL spawn attempts are blocked, matching the request that "no mobs spawn there
 * on their own". Two hooks are used for safety/redundancy: PositionCheck (early, cheap) and
 * FinalizeSpawn (final safety net before the entity actually joins the level).
 */
public class NoMobSpawnHandler {

    @SubscribeEvent
    public void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && isFlatWorld(event)) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }

    @SubscribeEvent
    public void onFinalizeSpawn(FinalizeSpawnEvent event) {
        if (event.getSpawnType() == MobSpawnType.NATURAL && isFlatWorld(event)) {
            event.setSpawnCancelled(true);
        }
    }

    private boolean isFlatWorld(MobSpawnEvent event) {
        return event.getLevel().getLevel().dimension() == ModDimensions.FLAT_WORLD_LEVEL;
    }
}
