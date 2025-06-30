package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Function;

/**
 * Handles all player spawning and respawning logic to ensure players
 * are correctly placed in the Elysian Isles dimension.
 */
@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class PlayerSpawnHandler {

    // --- NBT Keys for Player Data ---
    private static final String NBT_KEY_ELYSIAN_SPAWNED = "elysian_spawned";
    private static final String NBT_KEY_SHOULD_RESPAWN_IN_SKY = "should_respawn_in_sky";

    // --- Island Spawn Coordinates ---
    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final int ISLAND_MAX_RADIUS = 125;


    /**
     * When a player joins the server for the very first time, teleport them to the sky dimension.
     */
    @SubscribeEvent
    public static void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            // Check for our custom NBT tag to see if this is a new player.
            if (!playerData.getBoolean(NBT_KEY_ELYSIAN_SPAWNED)) {
                playerData.putBoolean(NBT_KEY_ELYSIAN_SPAWNED, true);
                teleportToSkyIsland(player);
                ElysianIslesMod.LOGGER.info("New player {} spawned in Elysian dimension.", player.getName().getString());
            }
        }
    }

    /**
     * When a player dies, check which dimension they died in.
     * This is the crucial first step in the respawn process.
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // **THE CORE FIX**: Only set the respawn flag if the player died IN the sky dimension.
            if (player.level().dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
                player.getPersistentData().putBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY, true);
                ElysianIslesMod.LOGGER.info("Player {} died in Elysian Isles. Marking for sky respawn.", player.getName().getString());
            }
            // If the player dies in any other dimension (e.g., Overworld), we do nothing,
            // allowing the default vanilla respawn mechanics to proceed as normal.
        }
    }

    /**
     * After a player dies, their data needs to be copied to the new player entity.
     * This event ensures our respawn flag persists across death.
     */
    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        // We only care if the player is actually coming back from the dead.
        if (event.isWasDeath()) {
            ServerPlayer originalPlayer = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

            // Copy our custom NBT data from the old player instance to the new one.
            CompoundTag originalData = originalPlayer.getPersistentData();
            CompoundTag newData = newPlayer.getPersistentData();

            if (originalData.getBoolean(NBT_KEY_ELYSIAN_SPAWNED)) {
                newData.putBoolean(NBT_KEY_ELYSIAN_SPAWNED, true);
            }
            if (originalData.getBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY)) {
                newData.putBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY, true);
            }
        }
    }

    /**
     * This event fires when the player is actually respawning. We check our flag here
     * and override their spawn location if necessary.
     */
    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Check for the flag we set in onPlayerDeath (and preserved in onPlayerClone).
            if (player.getPersistentData().getBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY)) {
                // IMPORTANT: Reset the flag so this logic doesn't run again on a subsequent death.
                player.getPersistentData().remove(NBT_KEY_SHOULD_RESPAWN_IN_SKY);

                ElysianIslesMod.LOGGER.info("Player {} is respawning. Forcing to sky dimension.", player.getName().getString());
                teleportToSkyIsland(player);
            }
        }
    }

    /**
     * Handles the logic of moving a player to the Elysian Isles dimension and finding a safe spot.
     */
    private static void teleportToSkyIsland(ServerPlayer player) {
        ServerLevel skyLevel = player.getServer().getLevel(ModDimensions.ELYSIAN_LEVEL_KEY);
        if (skyLevel == null) {
            ElysianIslesMod.LOGGER.error("Could not find Elysian dimension! Cannot teleport player {}.", player.getName().getString());
            return;
        }

        BlockPos spawnPos = findSafeSpawnOnIsland(skyLevel);

        // If the player is not already in the target dimension, use changeDimension.
        if (player.level().dimension() != skyLevel.dimension()) {
            player.changeDimension(skyLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    Entity repositionedEntity = repositionEntity.apply(false); // Let Forge do initial repositioning.
                    repositionedEntity.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    return repositionedEntity;
                }
            });
        } else {
            // If the player is already in the sky dimension (e.g., respawning), a simple teleport is sufficient.
            player.teleportTo(skyLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        }

        ElysianIslesMod.LOGGER.info("Successfully teleported player {} to a safe location on the Sky Island: {}", player.getName().getString(), spawnPos);
    }

    /**
     * Scans for a safe place for the player to spawn on the main island.
     * A safe spot has a solid block below and two air blocks for the player.
     *
     * @param level The ServerLevel of the sky dimension.
     * @return A safe BlockPos to spawn the player.
     */
    private static BlockPos findSafeSpawnOnIsland(ServerLevel level) {
        // Try the absolute center first
        BlockPos centerSpawn = findSafeSpotAt(level, ISLAND_CENTER_X, ISLAND_CENTER_Z);
        if (centerSpawn != null) {
            return centerSpawn;
        }

        // If center is not safe, spiral outwards to find a valid spot.
        for (int radius = 3; radius <= 50; radius += 3) {
            for (int angle = 0; angle < 360; angle += 45) {
                double radians = Math.toRadians(angle);
                int x = ISLAND_CENTER_X + (int) (radius * Math.cos(radians));
                int z = ISLAND_CENTER_Z + (int) (radius * Math.sin(radians));

                BlockPos spawnSpot = findSafeSpotAt(level, x, z);
                if (spawnSpot != null) {
                    ElysianIslesMod.LOGGER.info("Found safe spawn at ({}, {})", x, z);
                    return spawnSpot;
                }
            }
        }

        // As a last resort, fallback to a hardcoded position high above the center.
        BlockPos fallbackPos = new BlockPos(ISLAND_CENTER_X, 90, ISLAND_CENTER_Z);
        ElysianIslesMod.LOGGER.warn("Could not find a safe spawn location. Using fallback position: {}", fallbackPos);
        return fallbackPos;
    }

    /**
     * Checks a specific X,Z column for a safe spawn point.
     *
     * @return A safe BlockPos in that column, or null if none is found.
     */
    private static BlockPos findSafeSpotAt(ServerLevel level, int x, int z) {
        // Get the highest block that can block motion (e.g., leaves, grass).
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos(x, surfaceY, z);

        // Check from a bit above the surface down to a minimum height.
        for (int y = testPos.getY() + 5; y > 60; y--) {
            testPos.setY(y);
            // Check for 2 blocks of air
            if (level.getBlockState(testPos).isAir() && level.getBlockState(testPos.above()).isAir()) {
                // Check for a solid block below to stand on.
                if (level.getBlockState(testPos.below()).isSolidRender(level, testPos.below())) {
                    return testPos.immutable(); // Found a safe spot
                }
            }
        }
        return null; // No safe spot found in this column
    }
}