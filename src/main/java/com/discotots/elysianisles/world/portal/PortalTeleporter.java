package com.discotots.elysianisles.world.portal;

import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.ITeleporter;
import java.util.Optional;
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
        repositionedEntity.teleportTo(destinationPos.getX() + 0.5, destinationPos.getY(), destinationPos.getZ() + 0.5);
        return repositionedEntity;
    }

    private BlockPos findOrCreatePortal(Entity entity) {
        PortalManager portalManager = PortalManager.get(this.level);
        BlockPos existingPortalPos = portalManager.getPortalPos(this.level.dimension());

        // If a portal is registered, exists, and is loaded, use it.
        if (existingPortalPos != null && this.level.isLoaded(existingPortalPos) && this.level.getBlockState(existingPortalPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
            return existingPortalPos;
        }

        // Otherwise, find a new safe spot and create a new portal.
        BlockPos spawnPos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
        BlockPos safePos = this.findSafePortalLocation(spawnPos);
        return this.createPortal(safePos);
    }

    private BlockPos findSafePortalLocation(BlockPos portalPos) {
        boolean isOverworld = this.level.dimension() == Level.OVERWORLD;
        int searchRadius = 128;

        for (int r = 0; r < searchRadius; ++r) {
            for (int x = -r; x <= r; ++x) {
                for (int z = -r; z <= r; ++z) {
                    if (r > 0 && Math.abs(x) != r && Math.abs(z) != r) continue;

                    BlockPos candidatePos = portalPos.offset(x, 0, z);
                    BlockPos surfacePos;

                    if (isOverworld) {
                        surfacePos = this.level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidatePos);
                        if (this.level.getFluidState(surfacePos).isSource() || this.level.getFluidState(surfacePos.below()).isSource()) {
                            continue;
                        }
                    } else {
                        BlockPos.MutableBlockPos scanner = new BlockPos.MutableBlockPos(candidatePos.getX(), 128, candidatePos.getZ());
                        while (scanner.getY() > this.level.getMinBuildHeight() && this.level.isEmptyBlock(scanner)) {
                            scanner.move(Direction.DOWN);
                        }
                        surfacePos = scanner.immutable();
                    }

                    if (surfacePos.getY() > this.level.getMinBuildHeight()) {
                        BlockPos platformPos = surfacePos.below();
                        boolean hasClearance = true;
                        for (BlockPos checkPos : BlockPos.betweenClosed(platformPos.offset(-2, 1, -2), platformPos.offset(this.width + 1, this.height + 2, 2))) {
                            if (!this.level.isEmptyBlock(checkPos)) {
                                hasClearance = false;
                                break;
                            }
                        }
                        if (hasClearance) {
                            return platformPos;
                        }
                    }
                }
            }
        }
        return new BlockPos(portalPos.getX(), 80, portalPos.getZ());
    }

    private BlockPos createPortal(BlockPos pos) {
        PortalManager portalManager = PortalManager.get(this.level);
        portalManager.destroyOldPortal(this.level); // Deactivate old portal before creating new

        Direction frameDir = this.axis == Direction.Axis.X ? Direction.EAST : Direction.NORTH;
        BlockPos bottomLeft = pos.above().relative(frameDir.getOpposite(), this.width / 2);

        BlockPos.betweenClosed(bottomLeft.offset(-1, -1, 0), bottomLeft.offset(this.width, -1, 0)).forEach(p -> {
            level.setBlock(p, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        });

        for(int w = -1; w <= this.width; ++w) {
            for(int h = -1; h <= this.height; ++h) {
                if (w == -1 || w == this.width || h == -1 || h == this.height) {
                    BlockPos framePos = bottomLeft.relative(frameDir, w).relative(Direction.UP, h);
                    this.level.setBlock(framePos, Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                }
            }
        }

        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_AXIS, this.axis);
        for(int w = 0; w < this.width; ++w) {
            for(int h = 0; h < this.height; ++h) {
                BlockPos portalPos = bottomLeft.relative(frameDir, w).relative(Direction.UP, h);
                this.level.setBlock(portalPos, portalState, 18);
            }
        }

        portalManager.setPortalPos(this.level.dimension(), bottomLeft); // Register the new portal
        return bottomLeft;
    }
}