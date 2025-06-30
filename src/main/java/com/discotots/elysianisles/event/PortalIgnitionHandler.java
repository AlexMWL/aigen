package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.block.PsionicPortalShape;
import com.discotots.elysianisles.world.portal.PortalManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.FlintAndSteelItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.Optional;

public class PortalIgnitionHandler {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        ItemStack itemStack = event.getItemStack();

        ElysianIslesMod.LOGGER.info("Right click detected at {} with item {}", pos, itemStack.getItem());

        if (itemStack.getItem() instanceof FlintAndSteelItem && level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
            ElysianIslesMod.LOGGER.info("Flint and steel used on smooth stone at {}", pos);

            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos posInPortal = pos.relative(event.getFace());

            ElysianIslesMod.LOGGER.info("Checking portal creation at {}", posInPortal);

            Optional<PsionicPortalShape> optionalShape = tryCreatePortal(serverLevel, posInPortal);

            if (optionalShape.isPresent()) {
                ElysianIslesMod.LOGGER.info("Valid portal shape found! Creating portal...");

                PortalManager portalManager = PortalManager.get(serverLevel);
                // Destroy the old portal in this dimension before creating the new one
                portalManager.destroyOldPortal(serverLevel);

                PsionicPortalShape shape = optionalShape.get();
                shape.createPortalBlocks();

                // Register the new portal's position
                portalManager.setPortalPos(serverLevel.dimension(), shape.getBottomLeft());

                level.playSound(null, pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F, level.random.nextFloat() * 0.4F + 0.8F);
                itemStack.hurtAndBreak(1, event.getEntity(), p -> p.broadcastBreakEvent(event.getHand()));

                event.setCancellationResult(InteractionResult.SUCCESS);
                event.setCanceled(true);

                ElysianIslesMod.LOGGER.info("Portal created successfully at {}", shape.getBottomLeft());
            } else {
                ElysianIslesMod.LOGGER.info("No valid portal shape found at {}", posInPortal);
            }
        }
    }

    public static Optional<PsionicPortalShape> tryCreatePortal(Level level, BlockPos pos) {
        ElysianIslesMod.LOGGER.info("Trying to create portal at {} - checking Z axis first", pos);

        Optional<PsionicPortalShape> optionalZ = Optional.of(new PsionicPortalShape(level, pos, Direction.Axis.Z)).filter(PsionicPortalShape::isValid);
        if (optionalZ.isPresent()) {
            PsionicPortalShape shape = optionalZ.get();
            ElysianIslesMod.LOGGER.info("Found valid Z-axis portal: {}x{} at {}", shape.getWidth(), shape.getHeight(), shape.getBottomLeft());
            return optionalZ;
        }

        ElysianIslesMod.LOGGER.info("Z-axis portal invalid, trying X axis");
        Optional<PsionicPortalShape> optionalX = Optional.of(new PsionicPortalShape(level, pos, Direction.Axis.X)).filter(PsionicPortalShape::isValid);
        if (optionalX.isPresent()) {
            PsionicPortalShape shape = optionalX.get();
            ElysianIslesMod.LOGGER.info("Found valid X-axis portal: {}x{} at {}", shape.getWidth(), shape.getHeight(), shape.getBottomLeft());
            return optionalX;
        }

        ElysianIslesMod.LOGGER.info("No valid portal shape found for either axis");
        return Optional.empty();
    }
}