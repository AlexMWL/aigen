package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class PlayerSpawnHandler {

    @SubscribeEvent
    public static void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if this is a new player (hasn't been to Elysian dimension yet)
            if (!player.getPersistentData().getBoolean("elysian_visited")) {
                teleportToElysianDimension(player);
                player.getPersistentData().putBoolean("elysian_visited", true);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // If player dies in Elysian dimension, respawn them there
            if (player.level().dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
                // They're already in the right dimension, just make sure they spawn safely
                ensureSafeSpawn(player);
            }
        }
    }

    private static void teleportToElysianDimension(ServerPlayer player) {
        ServerLevel elysianLevel = player.getServer().getLevel(ModDimensions.ELYSIAN_LEVEL_KEY);
        if (elysianLevel != null) {
            // Find a safe spawn location on the island
            BlockPos spawnPos = findSafeSpawnLocation(elysianLevel);

            // Teleport the player
            player.teleportTo(elysianLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                    player.getYRot(), player.getXRot());

            ElysianIslesMod.LOGGER.info("Teleported player {} to Elysian Dimension at {}",
                    player.getName().getString(), spawnPos);
        }
    }

    private static void ensureSafeSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos currentPos = player.blockPosition();

        // Check if current position is safe
        if (isPositionSafe(level, currentPos)) {
            return;
        }

        // Find a safe position nearby
        BlockPos safePos = findSafeSpawnLocation(level);
        player.teleportTo(level, safePos.getX() + 0.5, safePos.getY(), safePos.getZ() + 0.5,
                player.getYRot(), player.getXRot());
    }

    private static BlockPos findSafeSpawnLocation(ServerLevel level) {
        // Start from the center of the island and spiral outward
        for (int radius = 0; radius <= 32; radius++) {
            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (Math.abs(x) == radius || Math.abs(z) == radius) { // Only check the perimeter
                        BlockPos checkPos = new BlockPos(x, 0, z);

                        // Get the highest solid block at this position
                        int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, checkPos).getY();
                        BlockPos surfacePos = new BlockPos(x, surfaceY, z);

                        if (isPositionSafe(level, surfacePos)) {
                            return surfacePos;
                        }
                    }
                }
            }
        }

        // Fallback to center of island at a safe height
        return new BlockPos(0, 80, 0);
    }

    private static boolean isPositionSafe(ServerLevel level, BlockPos pos) {
        // Check that there's solid ground beneath
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }

        // Check that there's air space for the player
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Make sure it's not over the void
        if (pos.getY() < 50) {
            return false;
        }

        // Additional safety check - make sure we're on the island
        double distanceFromCenter = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
        return distanceFromCenter <= 60; // Within island bounds
    }
}
