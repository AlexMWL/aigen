package com.discotots.elysianisles.event;

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
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

@Mod.EventBusSubscriber
public class PortalIgnitionHandler {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide()) return;

        BlockPos pos = event.getPos();
        ItemStack itemStack = event.getItemStack();

        if (itemStack.getItem() instanceof FlintAndSteelItem && level.getBlockState(pos).is(Blocks.SMOOTH_STONE)) {
            ServerLevel serverLevel = (ServerLevel) level;
            BlockPos posInPortal = pos.relative(event.getFace());

            Optional<PsionicPortalShape> optionalShape = tryCreatePortal(serverLevel, posInPortal);

            if (optionalShape.isPresent()) {
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
            }
        }
    }

    public static Optional<PsionicPortalShape> tryCreatePortal(Level level, BlockPos pos) {
        Optional<PsionicPortalShape> optionalZ = Optional.of(new PsionicPortalShape(level, pos, Direction.Axis.Z)).filter(PsionicPortalShape::isValid);
        if (optionalZ.isPresent()) {
            return optionalZ;
        }
        return Optional.of(new PsionicPortalShape(level, pos, Direction.Axis.X)).filter(PsionicPortalShape::isValid);
    }
}