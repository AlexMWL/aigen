package com.discotots.elysianisles.world.portal;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import com.discotots.elysianisles.init.ModDimensions; // <-- ADD THIS LINE
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.ITeleporter;
import java.util.function.Function;

public class PortalTeleporter implements ITeleporter {
    private final ServerLevel level; // The destination level
    private final Direction.Axis axis;
    private final int width;
    private final int height;

    public PortalTeleporter(ServerLevel level, Direction.Axis axis, int width, int height) {
        this.level = level;
        this.axis = axis;
        this.width = width;
        this.height = height;
    }

    @Override
    public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
        Entity repositionedEntity = repositionEntity.apply(false);
        BlockPos destinationPos = this.findOrCreatePortal(repositionedEntity);

        // Teleport to the center of the portal entrance
        repositionedEntity.teleportTo(
                destinationPos.getX() + 0.5,
                destinationPos.getY() + 1.0, // Place player 1 block above the bottom
                destinationPos.getZ() + 0.5
        );

        // Play teleportation sound
        destWorld.playSound(null, destinationPos, SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.5F, 1.0F);

        ElysianIslesMod.LOGGER.info("Placed entity {} at destination: {}",
                repositionedEntity.getName().getString(), destinationPos);

        return repositionedEntity;
    }

    private BlockPos findOrCreatePortal(Entity entity) {
        PortalManager portalManager = PortalManager.get(this.level);

        // --- FIXED: Determine the correct center point for our search ---
        BlockPos searchCenter;
        if (this.level.dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
            // If the destination is the sky world, always search around the island's center.
            searchCenter = new BlockPos(8, 90, -5);
        } else {
            // Otherwise, search near the entity's coordinates.
            searchCenter = entity.blockPosition();
        }

        ElysianIslesMod.LOGGER.info("Searching for existing portal in {} around {}",
                this.level.dimension().location(), searchCenter);

        // --- FIXED: Prioritize searching for ANY existing portal first ---
        BlockPos foundPortal = searchForExistingPortal(searchCenter);
        if (foundPortal != null) {
            ElysianIslesMod.LOGGER.info("Found an existing portal at {}. Using it.", foundPortal);
            // Update the manager so we find it faster next time.
            portalManager.setPortalPos(this.level.dimension(), foundPortal);
            return foundPortal;
        }

        // --- If no portal is found after a wide search, THEN create a new one ---
        ElysianIslesMod.LOGGER.info("No existing portal found. Creating a new one.");
        BlockPos safePos = this.findSafePortalLocation(searchCenter);
        return this.createPortal(safePos);
    }

    /**
     * Search for existing portal blocks in a large area around the given position
     */
    private BlockPos searchForExistingPortal(BlockPos center) {
        int searchRadius = 128;

        ElysianIslesMod.LOGGER.info("Searching for existing portals in {}x{} area around {}",
                searchRadius * 2, searchRadius * 2, center);

        // Search in expanding rings
        for (int radius = 0; radius <= searchRadius; radius += 16) {
            for (int x = center.getX() - radius; x <= center.getX() + radius; x += 4) {
                for (int z = center.getZ() - radius; z <= center.getZ() + radius; z += 4) {
                    // Only check the perimeter of the current ring to avoid duplicate checks
                    if (radius > 0 && Math.abs(x - center.getX()) < radius - 16 && Math.abs(z - center.getZ()) < radius - 16) {
                        continue;
                    }

                    // Check from build height down to min height
                    for (int y = this.level.getMaxBuildHeight() - 1; y >= this.level.getMinBuildHeight(); y--) {
                        BlockPos checkPos = new BlockPos(x, y, z);

                        if (this.level.isLoaded(checkPos) &&
                                this.level.getBlockState(checkPos).is(ModBlocks.PSIONIC_PORTAL.get())) {

                            ElysianIslesMod.LOGGER.info("Found portal block at {}, validating structure", checkPos);

                            // Find the bottom-left of this portal structure
                            BlockPos portalBottom = findPortalBottomLeft(checkPos);
                            if (portalBottom != null && isValidPortalAt(portalBottom)) {
                                ElysianIslesMod.LOGGER.info("Found valid portal structure at {}", portalBottom);
                                return portalBottom;
                            }
                        }
                    }
                }
            }
        }

        ElysianIslesMod.LOGGER.info("No existing portals found in search area");
        return null;
    }

    /**
     * Find the bottom-left corner of a portal structure given any portal block position
     */
    private BlockPos findPortalBottomLeft(BlockPos portalBlock) {
        BlockState state = this.level.getBlockState(portalBlock);
        if (!state.is(ModBlocks.PSIONIC_PORTAL.get())) {
            return null;
        }

        Direction.Axis portalAxis = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
        Direction rightDir = (portalAxis == Direction.Axis.X) ? Direction.EAST : Direction.SOUTH;
        Direction leftDir = rightDir.getOpposite();

        // Move to the bottom
        BlockPos.MutableBlockPos pos = portalBlock.mutable();
        while (pos.getY() > this.level.getMinBuildHeight() &&
                this.level.getBlockState(pos.below()).is(ModBlocks.PSIONIC_PORTAL.get())) {
            pos.move(Direction.DOWN);
        }

        // Move to the left edge
        while (this.level.getBlockState(pos.relative(leftDir)).is(ModBlocks.PSIONIC_PORTAL.get())) {
            pos.move(leftDir);
        }

        return pos.immutable();
    }

    /**
     * Check if there's a valid portal structure at the given position
     */
    private boolean isValidPortalAt(BlockPos pos) {
        // Check if there are portal blocks in the expected area for this portal size
        Direction frameDir = this.axis == Direction.Axis.X ? Direction.EAST : Direction.NORTH;

        int foundWidth = 0;
        int foundHeight = 0;

        // Count width
        for (int w = 0; w < 21; w++) { // Max portal width
            BlockPos checkPos = pos.relative(frameDir, w);
            if (this.level.getBlockState(checkPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                foundWidth++;
            } else {
                break;
            }
        }

        // Count height
        for (int h = 0; h < 21; h++) { // Max portal height
            BlockPos checkPos = pos.above(h);
            if (this.level.getBlockState(checkPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                foundHeight++;
            } else {
                break;
            }
        }

        boolean valid = foundWidth >= 2 && foundHeight >= 3; // Minimum portal size
        ElysianIslesMod.LOGGER.info("Portal validation at {}: {}x{}, valid: {}",
                pos, foundWidth, foundHeight, valid);

        return valid;
    }

    private BlockPos findSafePortalLocation(BlockPos startPos) {
        boolean isOverworld = this.level.dimension() == Level.OVERWORLD;
        int searchRadius = 128;

        // Try the starting position first
        BlockPos testPos = findSuitableGroundLevel(startPos, isOverworld);
        if (testPos != null && hasEnoughSpace(testPos)) {
            return testPos;
        }

        // Spiral search outwards
        for (int r = 1; r < searchRadius; r += 4) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = startPos.getX() + (int) (r * Math.cos(radians));
                int z = startPos.getZ() + (int) (r * Math.sin(radians));

                BlockPos candidatePos = new BlockPos(x, startPos.getY(), z);
                testPos = findSuitableGroundLevel(candidatePos, isOverworld);

                if (testPos != null && hasEnoughSpace(testPos)) {
                    ElysianIslesMod.LOGGER.info("Found safe portal location at: {}", testPos);
                    return testPos;
                }
            }
        }

        // Fallback position
        BlockPos fallback = new BlockPos(startPos.getX(), isOverworld ? 80 : 90, startPos.getZ());
        ElysianIslesMod.LOGGER.warn("Using fallback portal location: {}", fallback);
        return fallback;
    }

    private BlockPos findSuitableGroundLevel(BlockPos pos, boolean isOverworld) {
        if (isOverworld) {
            // In overworld, find surface level, ignoring trees.
            BlockPos groundPos = this.level.getHeightmapPos(Heightmap.Types.WORLD_SURFACE_WG, pos);

            // Avoid building in water.
            if (this.level.getFluidState(groundPos).isSource()) {
                return null;
            }

            // Return the ground block itself. The portal platform will be built on it.
            return groundPos;
        } else {
            // In sky dimension or other dimensions, find a solid surface
            BlockPos.MutableBlockPos scanner = new BlockPos.MutableBlockPos(pos.getX(), 128, pos.getZ());

            // Scan downward to find solid ground
            while (scanner.getY() > this.level.getMinBuildHeight() + 10) {
                if (!this.level.isEmptyBlock(scanner) &&
                        this.level.getBlockState(scanner).isSolidRender(this.level, scanner)) {
                    return scanner.immutable();
                }
                scanner.move(Direction.DOWN);
            }
        }
        return null;
    }

    /**
     * Check if there's enough space to build a portal at the given position
     */
    private boolean hasEnoughSpace(BlockPos pos) {
        // Check if we have enough clear space for the portal + frame
        int totalWidth = this.width + 2; // Portal width + frame
        int totalHeight = this.height + 2; // Portal height + frame

        for (int x = -1; x < totalWidth - 1; x++) {
            for (int y = 0; y < totalHeight; y++) {
                for (int z = -1; z < 2; z++) { // Check depth too
                    BlockPos checkPos = pos.offset(x, y, z);
                    BlockState state = this.level.getBlockState(checkPos);

                    // FIXED: Allow only air and water, reject solid blocks and other fluids.
                    if (!state.isAir() && !state.getFluidState().is(net.minecraft.world.level.material.Fluids.WATER)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private BlockPos createPortal(BlockPos pos) {
        PortalManager portalManager = PortalManager.get(this.level);

        // Destroy any other portal in the dimension before creating a new one
        portalManager.destroyOldPortal(this.level);

        Direction frameDir = this.axis == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        BlockPos bottomLeft = pos.above().relative(frameDir.getOpposite(), this.width / 2);

        // Create platform
        for (int x = -2; x <= this.width + 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos platformPos = bottomLeft.relative(frameDir, x).offset(0, -1, z);
                this.level.setBlock(platformPos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
            }
        }

        // Create portal frame
        for (int w = -1; w <= this.width; w++) {
            for (int h = -1; h <= this.height; h++) {
                if (w == -1 || w == this.width || h == -1 || h == this.height) {
                    BlockPos framePos = bottomLeft.relative(frameDir, w).relative(Direction.UP, h);
                    this.level.setBlock(framePos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
        }

        // Create portal blocks
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_AXIS, this.axis);

        for (int w = 0; w < this.width; w++) {
            for (int h = 0; h < this.height; h++) {
                BlockPos portalPos = bottomLeft.relative(frameDir, w).relative(Direction.UP, h);
                this.level.setBlock(portalPos, portalState, 18);
            }
        }

        // Register the new portal
        portalManager.setPortalPos(this.level.dimension(), bottomLeft);

        // Play creation sound
        this.level.playSound(null, bottomLeft, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 1.0F);

        ElysianIslesMod.LOGGER.info("Created new portal at: {}", bottomLeft);
        return bottomLeft;
    }
}