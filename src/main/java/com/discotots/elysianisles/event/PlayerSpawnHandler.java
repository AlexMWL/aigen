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

import java.util.function.Function;

public class PlayerSpawnHandler {

    // --- NBT Keys for Player Data ---
    private static final String NBT_KEY_ELYSIAN_SPAWNED = "elysian_spawned";
    private static final String NBT_KEY_SHOULD_RESPAWN_IN_SKY = "should_respawn_in_sky";

    // --- Island Spawn Coordinates ---
    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final int ISLAND_MAX_RADIUS = 125;

    @SubscribeEvent
    public void onPlayerFirstJoin(PlayerEvent.PlayerLoggedInEvent event) { // REMOVED static
        if (event.getEntity() instanceof ServerPlayer player) {
            CompoundTag playerData = player.getPersistentData();
            if (!playerData.getBoolean(NBT_KEY_ELYSIAN_SPAWNED)) {
                playerData.putBoolean(NBT_KEY_ELYSIAN_SPAWNED, true);
                teleportToSkyIsland(player);
                ElysianIslesMod.LOGGER.info("New player {} spawned in Elysian dimension.", player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerDeath(LivingDeathEvent event) { // REMOVED static
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.level().dimension() == ModDimensions.ELYSIAN_LEVEL_KEY) {
                player.getPersistentData().putBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY, true);
                ElysianIslesMod.LOGGER.info("Player {} died in Elysian Isles. Marking for sky respawn.", player.getName().getString());
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(PlayerEvent.Clone event) { // REMOVED static
        if (event.isWasDeath()) {
            ServerPlayer originalPlayer = (ServerPlayer) event.getOriginal();
            ServerPlayer newPlayer = (ServerPlayer) event.getEntity();

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

    @SubscribeEvent
    public void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) { // REMOVED static
        if (event.getEntity() instanceof ServerPlayer player) {
            if (player.getPersistentData().getBoolean(NBT_KEY_SHOULD_RESPAWN_IN_SKY)) {
                player.getPersistentData().remove(NBT_KEY_SHOULD_RESPAWN_IN_SKY);
                ElysianIslesMod.LOGGER.info("Player {} is respawning. Forcing to sky dimension.", player.getName().getString());
                teleportToSkyIsland(player);
            }
        }
    }

    private static void teleportToSkyIsland(ServerPlayer player) {
        ServerLevel skyLevel = player.getServer().getLevel(ModDimensions.ELYSIAN_LEVEL_KEY);
        if (skyLevel == null) {
            ElysianIslesMod.LOGGER.error("Could not find Elysian dimension! Cannot teleport player {}.", player.getName().getString());
            return;
        }

        BlockPos spawnPos = findSafeSpawnOnIsland(skyLevel);

        if (player.level().dimension() != skyLevel.dimension()) {
            player.changeDimension(skyLevel, new ITeleporter() {
                @Override
                public Entity placeEntity(Entity entity, ServerLevel currentWorld, ServerLevel destWorld, float yaw, Function<Boolean, Entity> repositionEntity) {
                    Entity repositionedEntity = repositionEntity.apply(false);
                    repositionedEntity.teleportTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                    return repositionedEntity;
                }
            });
        } else {
            player.teleportTo(skyLevel, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0.0F, 0.0F);
        }

        ElysianIslesMod.LOGGER.info("Successfully teleported player {} to a safe location on the Sky Island: {}", player.getName().getString(), spawnPos);
    }

    private static BlockPos findSafeSpawnOnIsland(ServerLevel level) {
        BlockPos centerSpawn = findSafeSpotAt(level, ISLAND_CENTER_X, ISLAND_CENTER_Z);
        if (centerSpawn != null) {
            return centerSpawn;
        }

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

        BlockPos fallbackPos = new BlockPos(ISLAND_CENTER_X, 90, ISLAND_CENTER_Z);
        ElysianIslesMod.LOGGER.warn("Could not find a safe spawn location. Using fallback position: {}", fallbackPos);
        return fallbackPos;
    }

    private static BlockPos findSafeSpotAt(ServerLevel level, int x, int z) {
        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos.MutableBlockPos testPos = new BlockPos.MutableBlockPos(x, surfaceY, z);

        for (int y = testPos.getY() + 5; y > 60; y--) {
            testPos.setY(y);
            if (level.getBlockState(testPos).isAir() && level.getBlockState(testPos.above()).isAir()) {
                if (level.getBlockState(testPos.below()).isSolidRender(level, testPos.below())) {
                    return testPos.immutable();
                }
            }
        }
        return null;
    }
}
