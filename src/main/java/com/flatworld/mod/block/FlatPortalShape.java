package com.flatworld.mod.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A minimal re-implementation of vanilla's {@code PortalShape}, adapted to work with our own
 * frame block ({@link ModBlocks#COMPRESSED_STONE}) instead of obsidian.
 * <p>
 * It is responsible for:
 * <ul>
 *     <li>Detecting a valid rectangular frame made of Compressed Stone (min 2x3 up to 21x21 like the Nether portal).</li>
 *     <li>Filling the empty interior with {@link ModBlocks#FLAT_PORTAL} blocks.</li>
 *     <li>Being reused when the player emerges on the other side, to build a return portal.</li>
 * </ul>
 */
public class FlatPortalShape {

    private static final int MIN_WIDTH = 2;
    private static final int MAX_WIDTH = 21;
    private static final int MIN_HEIGHT = 3;
    private static final int MAX_HEIGHT = 21;

    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    @Nullable
    private BlockPos bottomLeft;
    private int height;
    private int width;

    private FlatPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.NORTH;
        this.bottomLeft = calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = calculateWidth();
            if (this.width > 0) {
                this.height = calculateHeight();
            }
        }
    }

    @Nullable
    private BlockPos calculateBottomLeft(BlockPos pos) {
        for (int i = 0; i < MAX_HEIGHT; ++i) {
            BlockPos check = pos.relative(rightDir, -i);
            if (!isEmptyOrPortal(check) || !isFrame(check.relative(Direction.DOWN))) {
                break;
            }
            pos = check;
        }

        Direction leftDir = rightDir.getOpposite();
        int distance = getDistanceUntilEdge(pos, leftDir) - 1;
        return distance < 0 ? null : pos.relative(rightDir, -distance);
    }

    private int calculateWidth() {
        int width = getDistanceUntilEdge(this.bottomLeft, rightDir);
        return width >= MIN_WIDTH && width <= MAX_WIDTH ? width : 0;
    }

    private int getDistanceUntilEdge(BlockPos from, Direction dir) {
        int i;
        for (i = 0; i < MAX_WIDTH; ++i) {
            BlockPos check = from.relative(dir, i);
            if (!isEmptyOrPortal(check) || !isFrame(check.relative(Direction.DOWN))) {
                break;
            }
        }
        return isFrame(from.relative(dir, i)) ? i : 0;
    }

    private int calculateHeight() {
        int height;
        for (height = 0; height < MAX_HEIGHT; ++height) {
            BlockPos pos = this.bottomLeft.relative(Direction.UP, height);
            if (!isEmptyOrPortal(pos)) {
                break;
            }
        }
        for (int w = 0; w < this.width; ++w) {
            BlockPos top = this.bottomLeft.relative(Direction.UP, height).relative(rightDir, w);
            if (!isFrame(top)) {
                return 0;
            }
        }
        for (int w = 0; w < this.width; ++w) {
            if (!isFrame(this.bottomLeft.relative(rightDir, w).relative(Direction.DOWN))) {
                return 0;
            }
        }
        return height >= MIN_HEIGHT && height <= MAX_HEIGHT ? height : 0;
    }

    private boolean isEmptyOrPortal(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.is(ModBlocks.FLAT_PORTAL.get());
    }

    private boolean isFrame(BlockPos pos) {
        return level.getBlockState(pos).is(ModBlocks.COMPRESSED_STONE.get());
    }

    public boolean isValid() {
        return bottomLeft != null && width >= MIN_WIDTH && width <= MAX_WIDTH
                && height >= MIN_HEIGHT && height <= MAX_HEIGHT;
    }

    public void createPortalBlocks() {
        BlockState portalState = ModBlocks.FLAT_PORTAL.get().defaultBlockState()
                .setValue(FlatPortalBlock.AXIS, axis);
        BlockPos.betweenClosed(getPortalOrigin(), getPortalOrigin().relative(Direction.UP, height - 1).relative(rightDir, width - 1))
                .forEach(pos -> level.setBlock(pos.immutable(), portalState, 18));
    }

    private BlockPos getPortalOrigin() {
        return bottomLeft;
    }

    /**
     * Tries to find (and validate) a portal frame around {@code pos} on both possible axes.
     */
    public static Optional<FlatPortalShape> findValidFrame(LevelAccessor level, BlockPos pos) {
        FlatPortalShape onX = new FlatPortalShape(level, pos, Direction.Axis.X);
        if (onX.isValid()) {
            return Optional.of(onX);
        }
        FlatPortalShape onZ = new FlatPortalShape(level, pos, Direction.Axis.Z);
        if (onZ.isValid()) {
            return Optional.of(onZ);
        }
        return Optional.empty();
    }

    public Direction.Axis getAxis() {
        return axis;
    }
}
