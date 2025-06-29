package com.discotots.elysianisles.world.chunk;

import com.discotots.elysianisles.ElysianIslesMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.biome.BiomeSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class ElysianChunkGenerator extends ChunkGenerator {
    public static final Codec<ElysianChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, ElysianChunkGenerator::new)
    );

    private static final int SEA_LEVEL = 64;
    private static final int ISLAND_CENTER_X = 0;
    private static final int ISLAND_CENTER_Z = 0;
    private static final int MAX_ISLAND_RADIUS = 64;

    public ElysianChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // No carvers needed for floating islands
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
        // Surface building is handled in fillFromNoise
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Let vanilla handle mob spawning
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateTerrain(chunk);
            return chunk;
        }, executor);
    }

    private void generateTerrain(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        RandomSource random = RandomSource.create(chunkPos.toLong());

        // Generate the main floating island
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                generateColumnAt(chunk, x, z, worldX, worldZ, random);
            }
        }
    }

    private void generateColumnAt(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ, RandomSource random) {
        // Calculate distance from island center
        double distanceFromCenter = Math.sqrt((worldX - ISLAND_CENTER_X) * (worldX - ISLAND_CENTER_X) +
                (worldZ - ISLAND_CENTER_Z) * (worldZ - ISLAND_CENTER_Z));

        if (distanceFromCenter > MAX_ISLAND_RADIUS) {
            return; // Outside island bounds - keep as air
        }

        // Create island shape with multiple levels
        double normalizedDistance = distanceFromCenter / MAX_ISLAND_RADIUS;

        // Island height based on distance (higher in center, lower at edges)
        int baseHeight = SEA_LEVEL + (int)(20 * (1.0 - normalizedDistance * normalizedDistance));

        // Add some noise for terrain variation
        double noise = (random.nextDouble() - 0.5) * 8;
        int terrainHeight = Math.max(SEA_LEVEL - 10, baseHeight + (int)noise);

        // Generate terrain layers
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
            BlockState blockToPlace = getBlockForPosition(y, terrainHeight, worldX, worldZ, random);
            if (blockToPlace != null) {
                chunk.setBlockState(new BlockPos(localX, y, localZ), blockToPlace, false);
            }
        }

        // Add water features
        addWaterFeatures(chunk, localX, localZ, worldX, worldZ, terrainHeight, random);

        // Update heightmaps
        Heightmap.primeHeightmaps(chunk, ChunkGenerator.getTerrainBlocks());
    }

    private BlockState getBlockForPosition(int y, int terrainHeight, int worldX, int worldZ, RandomSource random) {
        if (y > terrainHeight) {
            return null; // Air
        }

        if (y == terrainHeight) {
            // Surface layer - grass or dirt
            return Blocks.GRASS_BLOCK.defaultBlockState();
        } else if (y >= terrainHeight - 3) {
            // Subsurface - dirt
            return Blocks.DIRT.defaultBlockState();
        } else if (y >= terrainHeight - 8) {
            // Stone layer with some ores
            if (random.nextInt(100) < 2) {
                return Blocks.COAL_ORE.defaultBlockState();
            } else if (random.nextInt(200) < 1) {
                return Blocks.IRON_ORE.defaultBlockState();
            }
            return Blocks.STONE.defaultBlockState();
        } else {
            // Deep stone
            return Blocks.STONE.defaultBlockState();
        }
    }

    private void addWaterFeatures(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ, int terrainHeight, RandomSource random) {
        // Add small water pockets and streams
        double distanceFromCenter = Math.sqrt((worldX - ISLAND_CENTER_X) * (worldX - ISLAND_CENTER_X) +
                (worldZ - ISLAND_CENTER_Z) * (worldZ - ISLAND_CENTER_Z));

        // Create a small lake/stream in the center area
        if (distanceFromCenter < 20 && random.nextInt(100) < 15) {
            int waterLevel = terrainHeight - 1;
            if (waterLevel > SEA_LEVEL - 5) {
                chunk.setBlockState(new BlockPos(localX, waterLevel, localZ), Blocks.WATER.defaultBlockState(), false);
                // Ensure water has a bottom
                chunk.setBlockState(new BlockPos(localX, waterLevel - 1, localZ), Blocks.DIRT.defaultBlockState(), false);
            }
        }
    }

    @Override
    public int getSeaLevel() {
        return SEA_LEVEL;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
        double distanceFromCenter = Math.sqrt((x - ISLAND_CENTER_X) * (x - ISLAND_CENTER_X) +
                (z - ISLAND_CENTER_Z) * (z - ISLAND_CENTER_Z));

        if (distanceFromCenter > MAX_ISLAND_RADIUS) {
            return level.getMinBuildHeight();
        }

        double normalizedDistance = distanceFromCenter / MAX_ISLAND_RADIUS;
        return SEA_LEVEL + (int)(20 * (1.0 - normalizedDistance * normalizedDistance));
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        BlockState[] column = new BlockState[level.getHeight()];

        for (int y = 0; y < column.length; y++) {
            int worldY = level.getMinBuildHeight() + y;
            if (worldY <= height) {
                column[y] = Blocks.STONE.defaultBlockState();
            } else {
                column[y] = Blocks.AIR.defaultBlockState();
            }
        }

        return new NoiseColumn(level.getMinBuildHeight(), column);
    }
}
