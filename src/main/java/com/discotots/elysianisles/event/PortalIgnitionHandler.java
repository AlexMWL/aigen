package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class PortalIgnitionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        ItemStack itemStack = event.getItemStack();

        // Check if player is using flint and steel on smooth stone
        if (itemStack.getItem() instanceof FlintAndSteelItem) {
            BlockState clickedBlock = level.getBlockState(pos);

            if (clickedBlock.is(Blocks.SMOOTH_STONE)) {
                ElysianIslesMod.LOGGER.info("Clicked smooth stone frame at {}", pos);

                // Try to find and create a portal
                if (findAndCreatePortal(level, pos)) {
                    ElysianIslesMod.LOGGER.info("Portal created successfully!");

                    // Damage the flint and steel
                    if (!event.getEntity().isCreative()) {
                        itemStack.hurtAndBreak(1, event.getEntity(), (player) -> {
                            player.broadcastBreakEvent(event.getHand());
                        });
                    }

                    // Play ignition sound
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                            level.random.nextFloat() * 0.4F + 0.8F);
                    level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 1.0F, 0.8F);

                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                } else {
                    ElysianIslesMod.LOGGER.info("No valid portal frame found");
                }
            }
        }
    }

    private static boolean findAndCreatePortal(Level level, BlockPos clickedPos) {
        // Look for portal patterns around the clicked position

        // Try different portal sizes and orientations
        for (int width = 2; width <= 4; width++) {
            for (int height = 3; height <= 5; height++) {
                // Try X-axis portal (portal runs North-South)
                if (tryCreatePortalWithDimensions(level, clickedPos, width, height, Direction.Axis.X)) {
                    return true;
                }
                // Try Z-axis portal (portal runs East-West)
                if (tryCreatePortalWithDimensions(level, clickedPos, width, height, Direction.Axis.Z)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean tryCreatePortalWithDimensions(Level level, BlockPos clickedPos, int width, int height, Direction.Axis axis) {
        ElysianIslesMod.LOGGER.info("Trying {}x{} {} portal at {}", width, height, axis, clickedPos);

        // For X-axis: width goes North-South, frame goes East-West
        // For Z-axis: width goes East-West, frame goes North-South
        Direction widthDir = axis == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        Direction frameDir = axis == Direction.Axis.X ? Direction.EAST : Direction.NORTH;

        // Try different positions within a potential frame
        for (int frameOffset = -1; frameOffset <= 1; frameOffset++) {
            for (int widthOffset = -(width+1); widthOffset <= 1; widthOffset++) {
                for (int heightOffset = -(height+1); heightOffset <= 1; heightOffset++) {

                    BlockPos bottomLeft = clickedPos
                            .relative(frameDir, frameOffset)
                            .relative(widthDir, widthOffset)
                            .relative(Direction.DOWN, heightOffset);

                    if (isValidPortalFrame(level, bottomLeft, width, height, widthDir, frameDir)) {
                        ElysianIslesMod.LOGGER.info("Found valid portal frame at {} with dimensions {}x{}", bottomLeft, width, height);
                        fillPortalInterior(level, bottomLeft, width, height, widthDir, axis);
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private static boolean isValidPortalFrame(Level level, BlockPos bottomLeft, int width, int height, Direction widthDir, Direction frameDir) {
        // Check if this position has a valid portal frame

        // Check bottom edge
        for (int w = -1; w <= width; w++) {
            BlockPos pos = bottomLeft.relative(widthDir, w);
            if (!level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
                return false;
            }
        }

        // Check top edge
        for (int w = -1; w <= width; w++) {
            BlockPos pos = bottomLeft.relative(widthDir, w).relative(Direction.UP, height + 1);
            if (!level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
                return false;
            }
        }

        // Check left edge
        for (int h = 0; h <= height + 1; h++) {
            BlockPos pos = bottomLeft.relative(widthDir, -1).relative(Direction.UP, h);
            if (!level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
                return false;
            }
        }

        // Check right edge
        for (int h = 0; h <= height + 1; h++) {
            BlockPos pos = bottomLeft.relative(widthDir, width).relative(Direction.UP, h);
            if (!level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
                return false;
            }
        }

        // Check interior is empty
        for (int w = 0; w < width; w++) {
            for (int h = 1; h <= height; h++) {
                BlockPos pos = bottomLeft.relative(widthDir, w).relative(Direction.UP, h);
                if (!level.getBlockState(pos).isAir()) {
                    return false;
                }
            }
        }

        ElysianIslesMod.LOGGER.info("Portal frame validation passed for {}x{} at {}", width, height, bottomLeft);
        return true;
    }

    private static void fillPortalInterior(Level level, BlockPos bottomLeft, int width, int height, Direction widthDir, Direction.Axis axis) {
        BlockState portalState = ModBlocks.PSIONIC_PORTAL.get().defaultBlockState()
                .setValue(com.discotots.elysianisles.block.PsionicPortalBlock.AXIS, axis);

        ElysianIslesMod.LOGGER.info("Filling portal interior with {} blocks", width * height);

        // Fill the interior with portal blocks
        for (int w = 0; w < width; w++) {
            for (int h = 1; h <= height; h++) {
                BlockPos pos = bottomLeft.relative(widthDir, w).relative(Direction.UP, h);
                level.setBlock(pos, portalState, 3);
                ElysianIslesMod.LOGGER.debug("Placed portal block at {}", pos);
            }
        }
    }
}