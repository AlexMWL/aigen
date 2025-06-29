package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class PortalProtectionHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
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
    public static void onMultiBlockBreak(BlockEvent.EntityMultiPlaceEvent event) {
        // Prevent multi-block placement that might affect portals
        if (event.getReplacedBlockSnapshots().stream()
                .anyMatch(snapshot -> snapshot.getCurrentBlock().is(ModBlocks.PSIONIC_PORTAL.get()))) {
            ElysianIslesMod.LOGGER.info("Blocked multi-block event affecting portal blocks");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        // Prevent placing blocks that would replace portal blocks
        if (event.getBlockSnapshot().getReplacedBlock().is(ModBlocks.PSIONIC_PORTAL.get())) {
            ElysianIslesMod.LOGGER.info("Blocked attempt to place block over portal block at {}", event.getPos());
            event.setCanceled(true);
        }
    }
}