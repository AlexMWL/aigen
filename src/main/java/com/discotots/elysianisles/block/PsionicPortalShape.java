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
    private int portalBlockCount;
    private BlockPos bottomLeft;
    private int height;
    private int width;

    private static final int MIN_PORTAL_WIDTH = 2;
    private static final int MAX_PORTAL_WIDTH = 21;
    private static final int MIN_PORTAL_HEIGHT = 3;
    private static final int MAX_PORTAL_HEIGHT = 21;

    public PsionicPortalShape(LevelAccessor level, BlockPos pos, Direction.Axis axis) {
        ElysianIslesMod.LOGGER.info("Creating PsionicPortalShape at {} with axis {}", pos, axis);

        this.level = level;
        this.axis = axis;
        this.rightDir = (axis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;

        ElysianIslesMod.LOGGER.info("Right direction for axis {} is {}", axis, rightDir);

        this.bottomLeft = this.calculateBottomLeft(pos);
        if (this.bottomLeft == null) {
            ElysianIslesMod.LOGGER.info("calculateBottomLeft returned null, using fallback");
            this.bottomLeft = pos;
            this.width = 1;
            this.height = 1;
        } else {
            ElysianIslesMod.LOGGER.info("Found bottom left at {}", this.bottomLeft);
            this.width = this.calculateWidth();
            ElysianIslesMod.LOGGER.info("Calculated width: {}", this.width);
            if (this.width > 0) {
                this.height = this.calculateHeight();
                ElysianIslesMod.LOGGER.info("Calculated height: {}", this.height);
            } else {
                this.height = 0;
                ElysianIslesMod.LOGGER.info("Width is 0, setting height to 0");
            }
        }

        ElysianIslesMod.LOGGER.info("Final portal shape: {}x{} at {}, valid: {}",
                this.width, this.height, this.bottomLeft, isValid());
    }

    private BlockPos calculateBottomLeft(BlockPos pos) {
        ElysianIslesMod.LOGGER.info("calculateBottomLeft starting from {}", pos);

        int i = Math.max(this.level.getMinBuildHeight(), pos.getY() - MAX_PORTAL_HEIGHT);
        ElysianIslesMod.LOGGER.info("Minimum Y to check: {}", i);

        // Move down to find the bottom of the portal opening
        while(pos.getY() > i && isEmpty(this.level.getBlockState(pos.below()))) {
            pos = pos.below();
            ElysianIslesMod.LOGGER.info("Moving down to {}", pos);
        }

        ElysianIslesMod.LOGGER.info("Found bottom level at Y={}", pos.getY());

        Direction searchDir = rightDir.getOpposite();
        ElysianIslesMod.LOGGER.info("Searching left in direction {}", searchDir);

        int distanceToLeft = this.getDistanceUntilEdge(pos, searchDir) - 1;
        ElysianIslesMod.LOGGER.info("Distance to left edge: {}", distanceToLeft);

        if(distanceToLeft < 0) {
            ElysianIslesMod.LOGGER.info("Distance to left is negative, returning null");
            return null;
        }

        BlockPos result = pos.relative(searchDir, distanceToLeft);
        ElysianIslesMod.LOGGER.info("Bottom left calculated as: {}", result);
        return result;
    }

    private int calculateWidth() {
        ElysianIslesMod.LOGGER.info("calculateWidth from {}", this.bottomLeft);
        int i = this.getDistanceUntilEdge(this.bottomLeft, this.rightDir);
        ElysianIslesMod.LOGGER.info("Distance until right edge: {}", i);
        boolean validWidth = i >= MIN_PORTAL_WIDTH && i <= MAX_PORTAL_WIDTH;
        ElysianIslesMod.LOGGER.info("Width {} is valid: {}", i, validWidth);
        return validWidth ? i : 0;
    }

    private int getDistanceUntilEdge(BlockPos pos, Direction direction) {
        ElysianIslesMod.LOGGER.info("getDistanceUntilEdge from {} in direction {}", pos, direction);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();

        for(int i = 0; i <= MAX_PORTAL_WIDTH; ++i) {
            mutablePos.set(pos).move(direction, i);
            BlockState blockAtPos = this.level.getBlockState(mutablePos);
            BlockState blockBelow = this.level.getBlockState(mutablePos.move(Direction.DOWN));

            if (i == 0) {
                ElysianIslesMod.LOGGER.info("At distance {}: block = {}, below = {}",
                        i, blockAtPos.getBlock(), blockBelow.getBlock());
            }

            if (!this.isEmpty(blockAtPos)) {
                ElysianIslesMod.LOGGER.info("Hit non-empty block at distance {}: {}", i, blockAtPos.getBlock());
                return i;
            }

            mutablePos.move(Direction.UP); // Move back up after checking below
            if (!blockBelow.is(Blocks.SMOOTH_STONE)) {
                ElysianIslesMod.LOGGER.info("Missing smooth stone base at distance {}, found: {}", i, blockBelow.getBlock());
                return i;
            }
        }
        ElysianIslesMod.LOGGER.info("Reached maximum width without finding edge");
        return 0;
    }

    private int calculateHeight() {
        ElysianIslesMod.LOGGER.info("calculateHeight for width {}", this.width);
        BlockPos.MutableBlockPos mutablePos = new BlockPos.MutableBlockPos();
        int i = this.getDistanceUntilTop(mutablePos);
        ElysianIslesMod.LOGGER.info("Distance until top: {}", i);
        boolean validHeight = i >= MIN_PORTAL_HEIGHT && i <= MAX_PORTAL_HEIGHT;
        ElysianIslesMod.LOGGER.info("Height {} is valid: {}", i, validHeight);
        return validHeight ? i : 0;
    }

    private int getDistanceUntilTop(BlockPos.MutableBlockPos mutablePos) {
        for(int i = 0; i < MAX_PORTAL_HEIGHT; ++i) {
            BlockPos leftFrame = this.bottomLeft.relative(this.rightDir.getOpposite()).above(i);
            BlockPos rightFrame = this.bottomLeft.relative(this.rightDir, this.width).above(i);

            BlockState leftFrameState = this.level.getBlockState(leftFrame);
            BlockState rightFrameState = this.level.getBlockState(rightFrame);

            if (i < 3) { // Log first few iterations
                ElysianIslesMod.LOGGER.info("Height {}: left frame at {} = {}, right frame at {} = {}",
                        i, leftFrame, leftFrameState.getBlock(), rightFrame, rightFrameState.getBlock());
            }

            if (!leftFrameState.is(Blocks.SMOOTH_STONE) || !rightFrameState.is(Blocks.SMOOTH_STONE)) {
                ElysianIslesMod.LOGGER.info("Missing frame at height {}", i);
                return i;
            }

            // Check interior is empty
            for(int j = 0; j < this.width; ++j) {
                BlockPos interiorPos = this.bottomLeft.relative(this.rightDir, j).above(i);
                BlockState interiorState = this.level.getBlockState(interiorPos);

                if (!this.isEmpty(interiorState)) {
                    ElysianIslesMod.LOGGER.info("Interior not empty at {}: {}", interiorPos, interiorState.getBlock());
                    return i;
                }
            }
        }
        ElysianIslesMod.LOGGER.info("Reached maximum height");
        return MAX_PORTAL_HEIGHT;
    }

    private boolean isEmpty(BlockState state) {
        boolean empty = state.isAir() || state.is(ModBlocks.PSIONIC_PORTAL.get());
        return empty;
    }

    public boolean isValid() {
        boolean valid = this.bottomLeft != null &&
                this.width >= MIN_PORTAL_WIDTH && this.width <= MAX_PORTAL_WIDTH &&
                this.height >= MIN_PORTAL_HEIGHT && this.height <= MAX_PORTAL_HEIGHT;
        ElysianIslesMod.LOGGER.info("Portal shape isValid: {} (bottomLeft: {}, width: {}, height: {})",
                valid, this.bottomLeft, this.width, this.height);
        return valid;
    }

    public void createPortalBlocks() {
        ElysianIslesMod.LOGGER.info("Creating portal blocks for {}x{} portal", this.width, this.height);
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState().setValue(PsionicPortalBlock.AXIS, this.axis);
        BlockPos.betweenClosed(this.bottomLeft, this.bottomLeft.relative(Direction.UP, this.height - 1).relative(this.rightDir, this.width - 1))
                .forEach(pos -> {
                    ElysianIslesMod.LOGGER.info("Setting portal block at {}", pos);
                    this.level.setBlock(pos, portalState, 18);
                });
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

    public BlockPos getBottomLeft() {
        return this.bottomLeft;
    }
}