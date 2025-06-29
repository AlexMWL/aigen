package com.discotots.elysianisles.block;

import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class PsionicPortalShape {
    private final LevelAccessor level;
    private final Direction.Axis axis;
    private final Direction rightDir;
    private final Direction leftDir;
    private int portalBlockCount;
    private BlockPos bottomLeft;
    private int height;
    private int width;

    public PsionicPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = axis == Direction.Axis.X ? Direction.WEST : Direction.SOUTH;
        this.leftDir = this.rightDir.getOpposite();

        this.bottomLeft = this.calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            this.width = this.calculateWidth();
            if (this.width > 0) {
                this.height = this.calculateHeight();
            }
        }
    }

    private BlockPos calculateBottomLeft(BlockPos pos) {
        for (int i = Math.max(0, pos.getY() - 21); pos.getY() > i && this.isEmpty(pos.below()); pos = pos.below()) {
        }

        Direction direction = this.rightDir;
        for (int j = Math.max(1, pos.relative(direction, 1).getX() - 21); pos.relative(direction).getX() > j && this.isEmpty(pos.relative(direction)); pos = pos.relative(direction)) {
        }

        if (this.isFrameBlock(pos.relative(direction))) {
            return pos;
        } else {
            return null;
        }
    }

    private int calculateWidth() {
        int width = 0;
        for (int i = 0; i < 21; ++i) {
            BlockPos pos = this.bottomLeft.relative(this.leftDir, i);
            if (!this.isEmpty(pos)) {
                break;
            }

            BlockPos framePos = pos.below();
            if (!this.isFrameBlock(framePos)) {
                break;
            }

            ++width;
        }

        return width >= 2 && width <= 21 ? width : 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int height = this.getDistanceUntilEdge(mutablePos, this.bottomLeft, this.leftDir, this.rightDir);
        return height >= 3 && height <= 21 && this.hasTopFrame(mutablePos, height) ? height : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos mutablePos, int height) {
        for (int i = 0; i < this.width; ++i) {
            mutablePos.set(this.bottomLeft.relative(this.leftDir, i).relative(Direction.UP, height));
            if (!this.isFrameBlock(mutablePos)) {
                return false;
            }
        }
        return true;
    }

    private int getDistanceUntilEdge(BlockPos.MutableBlockPos mutablePos, BlockPos pos, Direction leftDir, Direction rightDir) {
        mutablePos.set(pos);
        int distance = 0;
        while (distance < 21) {
            mutablePos.move(Direction.UP);
            BlockState state = this.level.getBlockState(mutablePos);
            if (!this.isEmpty(state)) {
                break;
            }

            BlockState leftState = this.level.getBlockState(mutablePos.relative(leftDir));
            BlockState rightState = this.level.getBlockState(mutablePos.relative(rightDir));
            if (!this.isFrameBlock(leftState) || !this.isFrameBlock(rightState)) {
                break;
            }

            ++distance;
        }
        return distance;
    }

    private boolean isEmpty(BlockPos pos) {
        return this.isEmpty(this.level.getBlockState(pos));
    }

    private boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.PSIONIC_PORTAL.get());
    }

    private boolean isFrameBlock(BlockPos pos) {
        return this.isFrameBlock(this.level.getBlockState(pos));
    }

    private boolean isFrameBlock(BlockState state) {
        // Use smooth stone as the frame block
        return state.is(Blocks.SMOOTH_STONE);
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
    }

    public void createPortalBlocks() {
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState().setValue(PsionicPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.leftDir, this.width - 1))
                .forEach(pos -> this.level.setBlock(pos, portalState, 18));
    }
}
