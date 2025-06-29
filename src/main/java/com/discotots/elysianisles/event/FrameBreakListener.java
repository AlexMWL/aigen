package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class FrameBreakListener {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        Level level = (Level) event.getLevel();
        BlockPos pos = event.getPos();
        BlockState brokenBlock = event.getState();

        // Check if a smooth stone frame block was broken
        if (brokenBlock.is(Blocks.SMOOTH_STONE)) {
            ElysianIslesMod.LOGGER.info("Smooth stone broken at {}, checking for adjacent portals", pos);

            // Look for portal blocks near the broken frame block
            Set<BlockPos> affectedPortals = findNearbyPortalBlocks(level, pos);

            if (!affectedPortals.isEmpty()) {
                ElysianIslesMod.LOGGER.info("Found {} portal blocks near broken frame, destroying portals", affectedPortals.size());

                // Destroy all connected portal structures
                Set<BlockPos> allPortalBlocks = new HashSet<>();
                for (BlockPos portalPos : affectedPortals) {
                    allPortalBlocks.addAll(findConnectedPortalBlocks(level, portalPos));
                }

                // Remove all portal blocks
                for (BlockPos portalPos : allPortalBlocks) {
                    if (level.getBlockState(portalPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                        level.setBlock(portalPos, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                // Play portal destruction sound
                level.playSound(null, pos, SoundEvents.PORTAL_TRIGGER, SoundSource.BLOCKS, 0.5F, 0.5F);
                level.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
        }
    }

    private static Set<BlockPos> findNearbyPortalBlocks(Level level, BlockPos framePos) {
        Set<BlockPos> portalBlocks = new HashSet<>();

        // Check in a 5x5x5 area around the broken frame block
        for (int x = -2; x <= 2; x++) {
            for (int y = -2; y <= 2; y++) {
                for (int z = -2; z <= 2; z++) {
                    BlockPos checkPos = framePos.offset(x, y, z);
                    if (level.getBlockState(checkPos).is(ModBlocks.PSIONIC_PORTAL.get())) {
                        portalBlocks.add(checkPos);
                    }
                }
            }
        }

        return portalBlocks;
    }

    private static Set<BlockPos> findConnectedPortalBlocks(Level level, BlockPos start) {
        Set<BlockPos> portalBlocks = new HashSet<>();
        Set<BlockPos> toCheck = new HashSet<>();
        toCheck.add(start);

        while (!toCheck.isEmpty()) {
            BlockPos checking = toCheck.iterator().next();
            toCheck.remove(checking);

            if (portalBlocks.contains(checking)) continue;

            if (level.getBlockState(checking).is(ModBlocks.PSIONIC_PORTAL.get())) {
                portalBlocks.add(checking);

                // Add adjacent blocks to check
                for (Direction dir : Direction.values()) {
                    BlockPos adjacent = checking.relative(dir);
                    if (!portalBlocks.contains(adjacent)) {
                        toCheck.add(adjacent);
                    }
                }
            }
        }

        return portalBlocks;
    }
}