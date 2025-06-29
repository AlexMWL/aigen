package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.block.PsionicPortalShape;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
                // Try to ignite a portal
                if (tryIgnitePortal(level, pos)) {
                    // Damage the flint and steel
                    if (!event.getEntity().isCreative()) {
                        itemStack.hurtAndBreak(1, event.getEntity(), (player) -> {
                            player.broadcastBreakEvent(event.getHand());
                        });
                    }

                    // Play ignition sound
                    level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                            level.random.nextFloat() * 0.4F + 0.8F);

                    event.setCancellationResult(InteractionResult.SUCCESS);
                    event.setCanceled(true);
                }
            }
        }
    }

    private static boolean tryIgnitePortal(Level level, BlockPos pos) {
        // Try both X and Z axis orientations
        for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
            PsionicPortalShape portalShape = new PsionicPortalShape(level, pos, axis);

            if (portalShape.isValid()) {
                // Create the portal blocks
                portalShape.createPortalBlocks();

                // Play portal creation sound
                level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS,
                        1.0F, level.random.nextFloat() * 0.4F + 0.8F);

                ElysianIslesMod.LOGGER.info("Psionic Portal ignited at {} with axis {}", pos, axis);
                return true;
            }
        }

        return false;
    }

    /**
     * Alternative ignition method using a recipe-based approach
     * This could be called from a custom item or ritual
     */
    public static boolean ignitePortalWithRitual(Level level, BlockPos centerPos) {
        // Check for a 3x3 pattern of smooth stone with air in the middle
        boolean validRitual = true;

        // Check the ritual pattern
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                BlockPos checkPos = centerPos.offset(x, 0, z);
                BlockState state = level.getBlockState(checkPos);

                if (x == 0 && z == 0) {
                    // Center should be air or portal block
                    if (!state.isAir() && !state.is(ModBlocks.PSIONIC_PORTAL.get())) {
                        validRitual = false;
                        break;
                    }
                } else {
                    // Outer ring should be smooth stone
                    if (!state.is(Blocks.SMOOTH_STONE)) {
                        validRitual = false;
                        break;
                    }
                }
            }
            if (!validRitual) break;
        }

        if (validRitual) {
            // Find the largest valid portal frame and ignite it
            for (Direction.Axis axis : new Direction.Axis[]{Direction.Axis.X, Direction.Axis.Z}) {
                PsionicPortalShape portalShape = new PsionicPortalShape(level, centerPos, axis);

                if (portalShape.isValid()) {
                    portalShape.createPortalBlocks();

                    // Add some dramatic effects
                    level.playSound(null, centerPos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS,
                            2.0F, 0.5F);
                    level.playSound(null, centerPos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.BLOCKS,
                            0.5F, 1.5F);

                    // Add particle effects (this would need client-side handling)
                    return true;
                }
            }
        }

        return false;
    }
}
