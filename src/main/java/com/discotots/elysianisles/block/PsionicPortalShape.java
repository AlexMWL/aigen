package com.discotots.elysianisles.block;

import com.discotots.elysianisles.ElysianIslesMod;
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

        ElysianIslesMod.LOGGER.info("Creating portal shape at {} for axis {}", pos, axis);
        ElysianIslesMod.LOGGER.info("Right direction: {}, Left direction: {}", rightDir, leftDir);

        this.bottomLeft = this.calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            ElysianIslesMod.LOGGER.info("Could not find bottom left, using clicked position");
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            ElysianIslesMod.LOGGER.info("Bottom left found at: {}", bottomLeft);
            this.width = this.calculateWidth();
            ElysianIslesMod.LOGGER.info("Calculated width: {}", width);
            if (this.width > 0) {
                this.height = this.calculateHeight();
                ElysianIslesMod.LOGGER.info("Calculated height: {}", height);
            }
        }

        ElysianIslesMod.LOGGER.info("Final portal dimensions: {}x{}, valid: {}", width, height, isValid());
    }

    private BlockPos calculateBottomLeft(BlockPos pos) {
        ElysianIslesMod.LOGGER.info("Calculating bottom left from position: {}", pos);

        // Find the bottom of the portal (move down until we hit a frame block or can't go further)
        BlockPos bottomPos = pos;
        for (int i = Math.max(0, pos.getY() - 21); bottomPos.getY() > i && this.isEmpty(bottomPos.below()); bottomPos = bottomPos.below()) {
            ElysianIslesMod.LOGGER.info("Moving down from {} to {}", bottomPos, bottomPos.below());
        }

        ElysianIslesMod.LOGGER.info("Found bottom at: {}", bottomPos);

        // Find the left edge (move in the right direction until we hit a frame block)
        Direction direction = this.rightDir;
        BlockPos leftPos = bottomPos;
        for (int j = Math.max(1, bottomPos.relative(direction, 1).getX() - 21);
             leftPos.relative(direction).getX() > j && this.isEmpty(leftPos.relative(direction));
             leftPos = leftPos.relative(direction)) {
            ElysianIslesMod.LOGGER.info("Moving {} from {} to {}", direction, leftPos, leftPos.relative(direction));
        }

        ElysianIslesMod.LOGGER.info("Found left edge at: {}", leftPos);

        // Check if we have a frame block to the right of our left edge
        BlockPos frameCheck = leftPos.relative(direction);
        boolean hasFrame = this.isFrameBlock(frameCheck);
        ElysianIslesMod.LOGGER.info("Frame check at {}: {} (block: {})",
                frameCheck, hasFrame, level.getBlockState(frameCheck).getBlock());

        if (hasFrame) {
            return leftPos;
        } else {
            ElysianIslesMod.LOGGER.info("No frame block found, returning null");
            return null;
        }
    }

    private int calculateWidth() {
        int width = 0;
        for (int i = 0; i < 21; ++i) {
            BlockPos pos = this.bottomLeft.relative(this.leftDir, i);
            ElysianIslesMod.LOGGER.info("Checking width position {}: {}", i, pos);

            if (!this.isEmpty(pos)) {
                ElysianIslesMod.LOGGER.info("Position {} is not empty: {}", pos, level.getBlockState(pos).getBlock());
                break;
            }

            BlockPos framePos = pos.below();
            if (!this.isFrameBlock(framePos)) {
                ElysianIslesMod.LOGGER.info("No frame block below position {}: {}", framePos, level.getBlockState(framePos).getBlock());
                break;
            }

            ++width;
            ElysianIslesMod.LOGGER.info("Width now: {}", width);
        }

        boolean validWidth = width >= 2 && width <= 21;
        ElysianIslesMod.LOGGER.info("Final width: {}, valid: {}", width, validWidth);
        return validWidth ? width : 0;
    }

    private int calculateHeight() {
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int height = this.getDistanceUntilEdge(mutablePos, this.bottomLeft, this.leftDir, this.rightDir);
        boolean hasTop = height >= 3 && height <= 21 && this.hasTopFrame(mutablePos, height);
        ElysianIslesMod.LOGGER.info("Height calculation: {}, has top frame: {}", height, hasTop);
        return hasTop ? height : 0;
    }

    private boolean hasTopFrame(BlockPos.MutableBlockPos mutablePos, int height) {
        for (int i = 0; i < this.width; ++i) {
            mutablePos.set(this.bottomLeft.relative(this.leftDir, i).relative(Direction.UP, height));
            if (!this.isFrameBlock(mutablePos)) {
                ElysianIslesMod.LOGGER.info("Missing top frame at position: {}", mutablePos);
                return false;
            }
        }
        ElysianIslesMod.LOGGER.info("Top frame complete");
        return true;
    }

    private int getDistanceUntilEdge(BlockPos.MutableBlockPos mutablePos, BlockPos pos, Direction leftDir, Direction rightDir) {
        mutablePos.set(pos);
        int distance = 0;
        while (distance < 21) {
            mutablePos.move(Direction.UP);
            BlockState state = this.level.getBlockState(mutablePos);
            if (!this.isEmpty(state)) {
                ElysianIslesMod.LOGGER.info("Hit non-empty block at height {}: {}", distance, state.getBlock());
                break;
            }

            BlockState leftState = this.level.getBlockState(mutablePos.relative(leftDir));
            BlockState rightState = this.level.getBlockState(mutablePos.relative(rightDir));
            if (!this.isFrameBlock(leftState) || !this.isFrameBlock(rightState)) {
                ElysianIslesMod.LOGGER.info("Missing side frame at height {}: left={}, right={}",
                        distance, leftState.getBlock(), rightState.getBlock());
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
        boolean empty = state.isAir() || state.is(ModBlocks.PSIONIC_PORTAL.get());
        ElysianIslesMod.LOGGER.debug("Block {} empty: {}", state.getBlock(), empty);
        return empty;
    }

    private boolean isFrameBlock(BlockPos pos) {
        return this.isFrameBlock(this.level.getBlockState(pos));
    }

    private boolean isFrameBlock(BlockState state) {
        boolean isFrame = state.is(Blocks.SMOOTH_STONE);
        ElysianIslesMod.LOGGER.debug("Block {} is frame: {}", state.getBlock(), isFrame);
        return isFrame;
    }

    public boolean isValid() {
        boolean valid = this.bottomLeft != null && this.width >= 2 && this.width <= 21 && this.height >= 3 && this.height <= 21;
        ElysianIslesMod.LOGGER.info("Portal valid: {} (bottomLeft: {}, width: {}, height: {})",
                valid, bottomLeft != null, width, height);
        return valid;
    }

    public void createPortalBlocks() {
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState().setValue(PsionicPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.leftDir, this.width - 1))
                .forEach(pos -> this.level.setBlock(pos, portalState, 18));
    }
}