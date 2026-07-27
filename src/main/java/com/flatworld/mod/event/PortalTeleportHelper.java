package com.flatworld.mod.event;

import com.flatworld.mod.block.FlatPortalBlock;
import com.flatworld.mod.block.ModBlocks;
import com.flatworld.mod.dimension.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.DimensionTransition;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Handles the actual teleportation logic when an entity touches a {@link FlatPortalBlock}.
 * <p>
 * This mirrors vanilla's Nether portal behaviour:
 * <ul>
 *     <li>The entity needs to stand in the portal for a short debounce period before teleporting,
 *     to avoid being bounced back and forth instantly.</li>
 *     <li>Coordinates are mapped 1:1 between the Overworld and our flat dimension (no /8 scaling
 *     like the Nether, since this isn't meant to be a "shortcut" dimension).</li>
 *     <li>If no portal exists near the destination, one is built automatically out of Compressed
 *     Stone, exactly like vanilla builds an obsidian frame in the Nether.</li>
 * </ul>
 */
public final class PortalTeleportHelper {

    /** How many ticks an entity must stand inside the portal before teleporting (vanilla uses ~4-ish via a counter, we keep it simple and snappy). */
    private static final int TELEPORT_DELAY_TICKS = 4;

    /** Radius (in blocks) searched around the target coordinates for a re-usable existing portal. */
    private static final int SEARCH_RADIUS = 128;

    /** Tracks how long each entity has been standing in a portal, and cooldown after a teleport. */
    private static final Map<UUID, Integer> PORTAL_TIME = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final int COOLDOWN_TICKS = 20;

    private PortalTeleportHelper() {
    }

    public static void onEntityCollidedWithPortal(Entity entity, BlockPos portalPos) {
        if (!(entity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        UUID id = entity.getUUID();
        Integer cooldown = COOLDOWN.get(id);
        if (cooldown != null && cooldown > 0) {
            COOLDOWN.put(id, cooldown - 1);
            return;
        }

        int time = PORTAL_TIME.merge(id, 1, Integer::sum);
        if (time < TELEPORT_DELAY_TICKS) {
            return;
        }
        PORTAL_TIME.remove(id);
        COOLDOWN.put(id, COOLDOWN_TICKS);

        teleport(entity, serverLevel, portalPos);
    }

    /** Called by tick handlers to decay/clear stale entries; also usable to reset when an entity leaves the portal. */
    public static void clearPortalTime(UUID entityId) {
        PORTAL_TIME.remove(entityId);
    }

    private static void teleport(Entity entity, ServerLevel currentLevel, BlockPos portalPos) {
        boolean goingToFlatWorld = currentLevel.dimension() != ModDimensions.FLAT_WORLD_LEVEL;

        ServerLevel destinationLevel = goingToFlatWorld
                ? currentLevel.getServer().getLevel(ModDimensions.FLAT_WORLD_LEVEL)
                : currentLevel.getServer().overworld();

        if (destinationLevel == null) {
            // The flat_world dimension isn't loaded (e.g. datapack missing) - abort safely.
            return;
        }

        // 1:1 coordinate mapping between both dimensions.
        BlockPos destinationColumn = new BlockPos(portalPos.getX(), portalPos.getY(), portalPos.getZ());

        BlockPos portalFrameOrigin = findOrCreatePortal(destinationLevel, destinationColumn, currentLevel.getBlockState(portalPos));

        DimensionTransition transition = new DimensionTransition(
                destinationLevel,
                new net.minecraft.world.phys.Vec3(portalFrameOrigin.getX() + 0.5, portalFrameOrigin.getY() + 0.5, portalFrameOrigin.getZ() + 0.5),
                entity.getDeltaMovement(),
                entity.getYRot(),
                entity.getXRot(),
                DimensionTransition.DO_NOTHING
        );

        entity.changeDimension(transition);
    }

    /**
     * Looks for an existing Flat Portal block near {@code near}; if none is found within
     * {@link #SEARCH_RADIUS}, builds a brand-new 2x3 Compressed Stone frame (with the portal lit)
     * at that location, similar to how vanilla auto-generates a return Nether portal.
     */
    private static BlockPos findOrCreatePortal(ServerLevel level, BlockPos near, BlockState sourcePortalState) {
        Optional<BlockPos> existing = findNearbyPortal(level, near);
        if (existing.isPresent()) {
            return existing.get();
        }

        return buildNewPortal(level, near, sourcePortalState);
    }

    private static Optional<BlockPos> findNearbyPortal(ServerLevel level, BlockPos center) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();
        int searchY = Math.max(minY, Math.min(maxY - 1, center.getY()));

        for (int r = 0; r <= SEARCH_RADIUS; r += 1) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) {
                        continue;
                    }
                    mutable.set(center.getX() + dx, searchY, center.getZ() + dz);
                    if (level.getBlockState(mutable).is(ModBlocks.FLAT_PORTAL.get()) && level.isLoaded(mutable)) {
                        return Optional.of(mutable.immutable());
                    }
                }
            }
            if (r >= 16) {
                // Keep the search cheap: only do a wide scan if chunks are actually loaded/generated.
                break;
            }
        }
        return Optional.empty();
    }

    private static BlockPos buildNewPortal(ServerLevel level, BlockPos near, BlockState sourcePortalState) {
        // Make sure the target column is loaded and find solid ground to stand the frame on.
        int surfaceY = findSurfaceY(level, near);
        BlockPos base = new BlockPos(near.getX(), surfaceY, near.getZ());

        Direction.Axis axis = sourcePortalState.hasProperty(FlatPortalBlock.AXIS)
                ? sourcePortalState.getValue(FlatPortalBlock.AXIS)
                : Direction.Axis.X;
        Direction rightDir = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;

        BlockState frameState = ModBlocks.COMPRESSED_STONE.get().defaultBlockState();
        BlockState portalState = ModBlocks.FLAT_PORTAL.get().defaultBlockState().setValue(FlatPortalBlock.AXIS, axis);

        // Build a minimal 2-wide x 3-tall frame (4x5 outer, matching a standard Nether portal).
        // Bottom-left corner of the frame:
        BlockPos frameBottomLeft = base;

        for (int w = -1; w <= 2; w++) {
            for (int h = -1; h <= 3; h++) {
                BlockPos pos = frameBottomLeft.relative(rightDir, w).above(h);
                boolean isBorder = (w == -1 || w == 2 || h == -1 || h == 3);
                if (isBorder) {
                    level.setBlock(pos, frameState, 3);
                } else {
                    level.setBlock(pos, portalState, 3);
                }
            }
        }

        return frameBottomLeft;
    }

    private static int findSurfaceY(ServerLevel level, BlockPos column) {
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, column.getX(), column.getZ());
        int clamped = Math.max(level.getMinBuildHeight() + 4, Math.min(level.getMaxBuildHeight() - 5, y));
        return clamped;
    }
}
