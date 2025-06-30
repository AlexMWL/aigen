package com.discotots.elysianisles.event;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.init.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID)
public class ElysianMobSpawnHandler {

    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final Random SPAWN_RANDOM = new Random();

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        LevelAccessor level = event.getLevel();

        // Only process server-side and in our dimension
        if (!(level instanceof ServerLevel serverLevel) ||
                serverLevel.dimension() != ModDimensions.ELYSIAN_LEVEL_KEY) {
            return;
        }

        // Spawn animals when chunks load (but not too frequently)
        if (SPAWN_RANDOM.nextFloat() < 0.3f) { // 30% chance per chunk load
            spawnAnimalsInChunk(serverLevel, event.getChunk().getPos());
        }
    }

    private static void spawnAnimalsInChunk(ServerLevel level, net.minecraft.world.level.ChunkPos chunkPos) {
        // Check if chunk is near island center
        double distFromCenter = Math.sqrt(
                Math.pow(chunkPos.x * 16 - ISLAND_CENTER_X, 2) +
                        Math.pow(chunkPos.z * 16 - ISLAND_CENTER_Z, 2)
        );

        if (distFromCenter > 100) return; // Too far from island center

        ElysianIslesMod.LOGGER.debug("Attempting to spawn animals in chunk {} at distance {}", chunkPos, distFromCenter);

        // Try to spawn 1-3 animals per chunk
        int spawnAttempts = 1 + SPAWN_RANDOM.nextInt(3);

        for (int i = 0; i < spawnAttempts; i++) {
            int x = chunkPos.getMinBlockX() + SPAWN_RANDOM.nextInt(16);
            int z = chunkPos.getMinBlockZ() + SPAWN_RANDOM.nextInt(16);

            // Find suitable spawn location
            BlockPos spawnPos = findAnimalSpawnLocation(level, x, z);
            if (spawnPos != null) {
                spawnRandomAnimal(level, spawnPos);
            }
        }
    }

    private static BlockPos findAnimalSpawnLocation(ServerLevel level, int x, int z) {
        // Get surface height
        int surfaceY = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z)).getY();

        // Check a few blocks around the heightmap result
        for (int y = surfaceY + 2; y >= surfaceY - 2; y--) {
            BlockPos checkPos = new BlockPos(x, y, z);
            BlockPos groundPos = checkPos.below();

            // Must be on grass block with air above
            if (level.getBlockState(groundPos).is(Blocks.GRASS_BLOCK) &&
                    level.getBlockState(checkPos).isAir() &&
                    level.getBlockState(checkPos.above()).isAir()) {

                // Make sure it's not too close to water
                if (!hasWaterNearby(level, checkPos, 2)) {
                    return checkPos;
                }
            }
        }

        return null;
    }

    private static boolean hasWaterNearby(ServerLevel level, BlockPos pos, int radius) {
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                BlockPos checkPos = pos.offset(x, 0, z);
                if (level.getBlockState(checkPos).is(Blocks.WATER)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void spawnRandomAnimal(ServerLevel level, BlockPos pos) {
        EntityType<?> animalType = getRandomAnimalType();

        try {
            // Create and spawn the entity
            if (animalType == EntityType.PIG) {
                Pig pig = new Pig(EntityType.PIG, level);
                pig.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                pig.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
                level.addFreshEntity(pig);
                ElysianIslesMod.LOGGER.debug("Spawned pig at {}", pos);

            } else if (animalType == EntityType.COW) {
                Cow cow = new Cow(EntityType.COW, level);
                cow.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                cow.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
                level.addFreshEntity(cow);
                ElysianIslesMod.LOGGER.debug("Spawned cow at {}", pos);

            } else if (animalType == EntityType.SHEEP) {
                Sheep sheep = new Sheep(EntityType.SHEEP, level);
                sheep.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                sheep.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
                // Randomize sheep color
                sheep.setColor(net.minecraft.world.item.DyeColor.values()[SPAWN_RANDOM.nextInt(net.minecraft.world.item.DyeColor.values().length)]);
                level.addFreshEntity(sheep);
                ElysianIslesMod.LOGGER.debug("Spawned sheep at {}", pos);

            } else if (animalType == EntityType.CHICKEN) {
                Chicken chicken = new Chicken(EntityType.CHICKEN, level);
                chicken.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                chicken.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
                level.addFreshEntity(chicken);
                ElysianIslesMod.LOGGER.debug("Spawned chicken at {}", pos);

            } else if (animalType == EntityType.RABBIT) {
                Rabbit rabbit = new Rabbit(EntityType.RABBIT, level);
                rabbit.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                rabbit.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.CHUNK_GENERATION, null, null);
                // Randomize rabbit variant
                rabbit.setVariant(Rabbit.Variant.values()[SPAWN_RANDOM.nextInt(Rabbit.Variant.values().length)]);
                level.addFreshEntity(rabbit);
                ElysianIslesMod.LOGGER.debug("Spawned rabbit at {}", pos);
            }

        } catch (Exception e) {
            ElysianIslesMod.LOGGER.error("Failed to spawn animal at {}: {}", pos, e.getMessage());
        }
    }

    private static EntityType<?> getRandomAnimalType() {
        float chance = SPAWN_RANDOM.nextFloat();

        if (chance < 0.25f) {
            return EntityType.PIG;
        } else if (chance < 0.45f) {
            return EntityType.COW;
        } else if (chance < 0.65f) {
            return EntityType.SHEEP;
        } else if (chance < 0.85f) {
            return EntityType.CHICKEN;
        } else {
            return EntityType.RABBIT;
        }
    }
}