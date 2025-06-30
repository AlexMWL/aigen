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
    private int portalBlockCount;
    private BlockPos bottomLeft;
    private int height;
    private int width;

    private static final int MIN_PORTAL_WIDTH = 2;
    private static final int MAX_PORTAL_WIDTH = 21;
    private static final int MIN_PORTAL_HEIGHT = 3;
    private static final int MAX_PORTAL_HEIGHT = 21;

    public PsionicPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        this.level = level;
        this.axis = axis;
        this.rightDir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;

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
        int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - MAX_PORTAL_HEIGHT);
        while(pos.getY() > i && isEmpty(this.level.getBlockState(pos.below()))) {
            pos = pos.below();
        }

        Direction searchDir = rightDir.getOpposite();
        int distanceToLeft = this.getDistanceUntilEdge(pos, searchDir) -1;
        if(distanceToLeft < 0) {
            return null;
        }
        return pos.relative(searchDir, distanceToLeft);
    }

    private int calculateWidth() {
        int i = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir);
        return i >= MIN_PORTAL_WIDTH && i <= MAX_PORTAL_WIDTH ? i : 0;
    }

    private int getDistanceUntilEdge(BlockPos pos, Direction direction) {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for(int i = 0; i <= MAX_PORTAL_WIDTH; ++i) {
            mutablePos.set(pos).move(direction, i);
            if (!this.isEmpty(this.level.getBlockState(mutablePos))) {
                return i;
            }
            if (!this.level.getBlockState(mutablePos.move(Direction.DOWN)).is(Blocks.SMOOTH_STONE)) {
                return i;
            }
        }
        return 0;
    }


    private int calculateHeight() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int i = this.getDistanceUntilTop(mutablePos);
        return i >= MIN_PORTAL_HEIGHT && i <= MAX_PORTAL_HEIGHT ? i : 0;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos mutablePos) {
        for(int i = 0; i < MAX_PORTAL_HEIGHT; ++i) {
            BlockPos leftFrame = this.bottomLeft.relative(this.rightDir.getOpposite()).above(i);
            BlockPos rightFrame = this.bottomLeft.relative(this.rightDir, this.width).above(i);

            if (!this.level.getBlockState(leftFrame).is(Blocks.SMOOTH_STONE) || !this.level.getBlockState(rightFrame).is(Blocks.SMOOTH_STONE)) {
                return i;
            }

            for(int j = 0; j < this.width; ++j) {
                if (!this.isEmpty(this.level.getBlockState(this.bottomLeft.relative(this.rightDir, j).above(i)))) {
                    return i;
                }
            }
        }
        return MAX_PORTAL_HEIGHT;
    }

    private boolean isEmpty(BlockState state) {
        return state.isAir() || state.is(ModBlocks.PSIONIC_PORTAL.get());
    }

    public boolean isValid() {
        return this.bottomLeft != null && this.width >= MIN_PORTAL_WIDTH && this.width <= MAX_PORTAL_WIDTH && this.height >= MIN_PORTAL_HEIGHT && this.height <= MAX_PORTAL_HEIGHT;
    }

    public void createPortalBlocks() {
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState().setValue(PsionicPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> this.level.setBlock(pos, portalState, 18));
    }

    public Direction.Axis getAxis() {
        return this.axis;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }
    // Add this method to the very end of the PsionicPortalShape class
    public BlockPos getBottomLeft() {
        return this.bottomLeft;
    }
}