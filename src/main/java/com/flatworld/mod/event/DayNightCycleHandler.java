package com.flatworld.mod.event;

import com.flatworld.mod.dimension.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.HashMap;
import java.util.Map;

/**
 * Implements a custom day/night cycle for the flat dimension: a 30-minute day and a 10-minute
 * night, instead of vanilla's ~10/10 minute cycle.
 * <p>
 * Vanilla's {@code dimension_type} JSON has no fields to configure day/night length, so this is
 * done the same way every "day length" mod does it: manually advance {@code dayTime} every tick
 * at a rate that stretches the 12000-tick "day" segment (0-12000) to 30 real-world minutes, and
 * the 12000-tick "night" segment (12000-24000) to 10 real-world minutes. The datapack sets
 * {@code doDaylightCycle} to false so vanilla's own progression never fights with ours (see the
 * flat_world dimension's data pack function, or set the gamerule manually the first time you
 * enter the dimension: {@code /gamerule doDaylightCycle false}).
 * <p>
 * A fractional accumulator is used so the integer tick-time advances smoothly without drifting.
 */
public class DayNightCycleHandler {

    private static final long DAY_SEGMENT_TICKS = 12000L; // vanilla ticks representing the "day" window (0-12000)
    private static final long NIGHT_SEGMENT_TICKS = 12000L; // vanilla ticks representing the "night" window (12000-24000)
    private static final long FULL_CYCLE_TICKS = DAY_SEGMENT_TICKS + NIGHT_SEGMENT_TICKS;

    private static final long DAY_REAL_TICKS = 30 * 60 * 20L; // 30 minutes, 20 ticks/sec
    private static final long NIGHT_REAL_TICKS = 10 * 60 * 20L; // 10 minutes, 20 ticks/sec

    // How much vanilla "dayTime" to add per real server tick while in the day/night segment.
    private static final double DAY_RATE = (double) DAY_SEGMENT_TICKS / (double) DAY_REAL_TICKS;
    private static final double NIGHT_RATE = (double) NIGHT_SEGMENT_TICKS / (double) NIGHT_REAL_TICKS;

    /** Per-level fractional accumulator so we don't lose precision when the rate is < 1 tick/tick. */
    private final Map<ServerLevel, Double> accumulators = new HashMap<>();

    @SubscribeEvent
    public void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (level.dimension() != ModDimensions.FLAT_WORLD_LEVEL) {
            return;
        }

        long currentDayTime = level.getDayTime();
        long timeOfDay = Math.floorMod(currentDayTime, FULL_CYCLE_TICKS);
        boolean isDaySegment = timeOfDay < DAY_SEGMENT_TICKS;
        double rate = isDaySegment ? DAY_RATE : NIGHT_RATE;

        double accumulated = accumulators.merge(level, rate, Double::sum);
        long wholeTicks = (long) Math.floor(accumulated);
        if (wholeTicks > 0) {
            accumulators.put(level, accumulated - wholeTicks);
            level.setDayTime(currentDayTime + wholeTicks);
        }
    }
}
