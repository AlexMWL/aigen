package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class PlayerSpawnHandler {

    @SubscribeEvent
    public static void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check if this is a new player (hasn't been to sky dimension yet)
            if (!player.getPersistentData().getBoolean("elysian_spawned")) {
                teleportToSkyIsland(player);
                player.getPersistentData().putBoolean("elysian_spawned", true);
                ElysianIslesMod.LOGGER.info("New player {} spawned in Elysian dimension", player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // If player dies in sky dimension, respawn them there
            if (player.level().dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
                // They're already in the right dimension, just make sure they spawn safely
                ensureSafeSpawn(player);
            }
        }
    }

    private static void teleportToSkyIsland(ServerPlayer player) {
        ServerLevel skyLevel = player.getServer().getLevel(ModDimensions.ELYSIAN_LEVEL_KEY);
        if (skyLevel != null) {
            // Find a safe spawn location on the island center
            BlockPos spawnPos = findSafeSpawnOnIsland(skyLevel);

            ElysianIslesMod.LOGGER.info("Attempting to teleport player {} to Sky Island at {}",
                    player.getName().getString(), spawnPos);

            // Teleport the player
            player.teleportTo(skyLevel, spawnPos.getX() + 0.5, spawnPos.getY() + 1.0, spawnPos.getZ() + 0.5,
                    0.0F, 0.0F); // Face north, spawn 1 block above surface

            ElysianIslesMod.LOGGER.info("Successfully teleported player {} to Sky Island", player.getName().getString());
        } else {
            ElysianIslesMod.LOGGER.error("Could not find Elysian dimension for player {}", player.getName().getString());
        }
    }

    private static void ensureSafeSpawn(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos currentPos = player.blockPosition();

        ElysianIslesMod.LOGGER.info("Ensuring safe spawn for player {} at {}", player.getName().getString(), currentPos);

        // Check if current position is safe
        if (isPositionSafe(level, currentPos)) {
            return;
        }

        // Find a safe position on the island
        BlockPos safePos = findSafeSpawnOnIsland(level);
        player.teleportTo(level, safePos.getX() + 0.5, safePos.getY() + 1.0, safePos.getZ() + 0.5,
                player.getYRot(), player.getXRot());

        ElysianIslesMod.LOGGER.info("Moved player {} to safe position {}", player.getName().getString(), safePos);
    }

    private static BlockPos findSafeSpawnOnIsland(ServerLevel level) {
        int centerX = 0;
        int centerZ = 0;

        ElysianIslesMod.LOGGER.info("Searching for safe spawn location around center ({}, {})", centerX, centerZ);

        // Try the center first
        BlockPos centerSpawn = findSafeSpotAt(level, centerX, centerZ);
        if (centerSpawn != null) {
            ElysianIslesMod.LOGGER.info("Found safe spawn at center: {}", centerSpawn);
            return centerSpawn;
        }

        // Spiral outward from center with smaller increments
        for (int radius = 3; radius <= 30; radius += 3) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = centerX + (int)(radius * Math.cos(radians));
                int z = centerZ + (int)(radius * Math.sin(radians));

                BlockPos spawnSpot = findSafeSpotAt(level, x, z);
                if (spawnSpot != null) {
                    ElysianIslesMod.LOGGER.info("Found safe spawn at radius {}: {}", radius, spawnSpot);
                    return spawnSpot;
                }
            }
        }

        // Fallback to a higher position at center
        BlockPos fallback = new BlockPos(centerX, 90, centerZ);
        ElysianIslesMod.LOGGER.warn("Using fallback spawn position: {}", fallback);
        return fallback;
    }

    private static BlockPos findSafeSpotAt(ServerLevel level, int x, int z) {
        // Get the highest solid block at this position
        BlockPos checkPos = new BlockPos(x, 0, z);
        int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, checkPos).getY();

        ElysianIslesMod.LOGGER.info("Checking position ({}, {}) - heightmap says surface at Y={}", x, z, surfaceY);

        // Check a range around the heightmap result
        for (int y = surfaceY + 5; y >= surfaceY - 5; y--) {
            BlockPos testPos = new BlockPos(x, y, z);
            if (isPositionSafe(level, testPos)) {
                ElysianIslesMod.LOGGER.info("Found safe position at {}", testPos);
                return testPos;
            }
        }

        return null;
    }

    private static boolean isPositionSafe(ServerLevel level, BlockPos pos) {
        // Check that there's solid ground beneath
        BlockPos below = pos.below();
        if (!level.getBlockState(below).isSolidRender(level, below)) {
            return false;
        }

        // Check that there's air space for the player (2 blocks high)
        if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) {
            return false;
        }

        // Make sure it's not too low (avoid void)
        if (pos.getY() < 60) {
            return false;
        }

        // Make sure it's not too high (avoid spawning in sky)
        if (pos.getY() > 120) {
            return false;
        }

        // Make sure we're on the island (within reasonable distance from center)
        double distanceFromCenter = Math.sqrt(pos.getX() * pos.getX() + pos.getZ() * pos.getZ());
        if (distanceFromCenter > 125) {
            return false;
        }

        return true;
    }
}