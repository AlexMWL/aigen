package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class PortalProtectionHandler {

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) { // REMOVED static
        BlockState state = event.getState();

        // Absolutely prevent breaking of portal blocks
        if (state.is(ModBlocks.PSIONIC_PORTAL.get())) {
            ElysianIslesMod.LOGGER.info("Blocked attempt to break portal block at {} by {}",
                    event.getPos(),
                    event.getPlayer() != null ? event.getPlayer().getName().getString() : "unknown");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onMultiBlockBreak(BlockEvent.EntityMultiPlaceEvent event) { // REMOVED static
        // Prevent multi-block placement that might affect portals
        if (event.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> snapshot.getCurrentBlock().is(ModBlocks.PSIONIC_PORTAL.get()))) {
            ElysianIslesMod.LOGGER.info("Blocked multi-block event affecting portal blocks");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) { // REMOVED static
        // Prevent placing blocks that would replace portal blocks
        if (event.getBlockSnapshot().getReplacedBlock().is(ModBlocks.PSIONIC_PORTAL.get())) {
            ElysianIslesMod.LOGGER.info("Blocked attempt to place block over portal block at {}", event.getPos());
            event.setCanceled(true);
        }
    }
}