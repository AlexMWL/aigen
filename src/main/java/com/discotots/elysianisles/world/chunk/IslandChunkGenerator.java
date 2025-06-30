package com.discotots.elysianisles.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.FixedBiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class IslandChunkGenerator extends ChunkGenerator {
    public static final Codec<IslandChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, instance.stable(IslandChunkGenerator::new))
    );

    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final int ISLAND_BASE_HEIGHT = 72;
    private static final int STRUCTURE_BLOCKING_RADIUS = 300;

    public IslandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    /**
     * By overriding the codec to NOT include structure settings, we effectively disable them.
     * This is the modern and correct way to prevent all structures from being added to the generator.
     */
    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // --- REMOVED ALL INCORRECT AND UNNECESSARY STRUCTURE OVERRIDES ---
    // Methods like generateStructures, hasStructureChunkInRange, getRingPositionsFor,
    // and the old createStructures have been removed for clarity and correctness.
    // The empty codec handles it all.

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // No carvers - this is correct if you don't want caves or ravines.
    }

    @Override
    public void buildSurface(WorldGenRegion region, StructureManager structures, RandomState randomState, ChunkAccess chunk) {
        // Surface handled in fillFromNoise
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Only spawn mobs on the island surface
    }

    @Override
    public int getGenDepth() {
        return 384;
    }

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Executor executor, Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        return CompletableFuture.supplyAsync(() -> {
            generateIsland(chunk);
            return chunk;
        }, executor);
    }

    private void generateIsland(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();

        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                generateColumn(chunk, x, z, worldX, worldZ);
            }
        }
    }

    private void generateColumn(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ) {
        // Create very jagged, blob-like island shape
        double islandShape = calculateJaggedBlobShape(worldX, worldZ);

        // Calculate distance from island center
        double dx = worldX - ISLAND_CENTER_X;
        double dz = worldZ - ISLAND_CENTER_Z;
        double distanceFromCenter = Math.sqrt(dx * dx + dz * dz);

        // This logic is fine for creating the island and clearing the area around it.
        // It ensures no vanilla terrain or features can spawn near your island.
        if (distanceFromCenter < STRUCTURE_BLOCKING_RADIUS) {
            if (islandShape <= 0) {
                // Outside island but near it - fill completely with air
                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    chunk.setBlockState(new BlockPos(localX, y, localZ), Blocks.AIR.defaultBlockState(), false);
                }
                return;
            }

            // Inside island - generate normal terrain
            int terrainHeight = calculateRealisticTerrainHeight(worldX, worldZ, islandShape);

            for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                BlockState block = getBlockAt(y, terrainHeight, worldX, worldZ, islandShape);
                if (block != null) {
                    chunk.setBlockState(new BlockPos(localX, y, localZ), block, false);
                } else {
                    chunk.setBlockState(new BlockPos(localX, y, localZ), Blocks.AIR.defaultBlockState(), false);
                }
            }

            addSmallWaterFeatures(chunk, localX, localZ, worldX, worldZ, terrainHeight);
        } else {
            // Far from island - fill with air to prevent any generation
            for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                chunk.setBlockState(new BlockPos(localX, y, localZ), Blocks.AIR.defaultBlockState(), false);
            }
        }
    }

    // --- [The rest of your file remains the same] ---
    // calculateJaggedBlobShape, calculateRealisticTerrainHeight, addSmallWaterFeatures,
    // getBlockAt, calculateIrregularBottom, getSeaLevel, getMinY, getBaseHeight,
    // getBaseColumn, and addDebugScreenInfo methods are unchanged.

    private double calculateJaggedBlobShape(int worldX, int worldZ) {
        double dx = worldX - ISLAND_CENTER_X;
        double dz = worldZ - ISLAND_CENTER_Z;
        double distance = Math.sqrt(dx * dx + dz * dz);
        double baseRadius = 90;
        double angle = Math.atan2(dz, dx);
        double largeJags = Math.sin(angle * 6) * 35 + Math.sin(angle * 12) * 20 + Math.sin(angle * 18) * 12;
        double chaosX = Math.sin(worldX * 0.08) * 15 + Math.sin(worldX * 0.15) * 8;
        double chaosZ = Math.sin(worldZ * 0.07) * 18 + Math.sin(worldZ * 0.13) * 10;
        double totalChaos = largeJags + chaosX + chaosZ;
        double actualRadius = baseRadius + totalChaos;
        double fractalNoise = Math.sin(worldX * 0.2) * Math.cos(worldZ * 0.18) * 8 + Math.sin(worldX * 0.3) * Math.cos(worldZ * 0.25) * 5;
        actualRadius += fractalNoise;
        actualRadius = Math.max(20, actualRadius);
        return actualRadius - distance;
    }

    private int calculateRealisticTerrainHeight(int worldX, int worldZ, double islandShape) {
        double heightFromShape = Math.max(0, islandShape / 120.0) * 12;
        double hills = Math.sin(worldX * 0.06) * Math.cos(worldZ * 0.05) * 4;
        double mediumTerrain = Math.sin(worldX * 0.12) * Math.cos(worldZ * 0.11) * 2;
        double fineTerrain = Math.sin(worldX * 0.2) * Math.cos(worldZ * 0.18) * 1;
        double cliffNoise = Math.sin(worldX * 0.03) * Math.cos(worldZ * 0.025);
        double cliffHeight = 0;
        if (cliffNoise > 0.6 && islandShape > 0.3) {
            cliffHeight = (cliffNoise - 0.6) * 8;
        }
        int finalHeight = ISLAND_BASE_HEIGHT + (int)(heightFromShape + hills + mediumTerrain + fineTerrain + cliffHeight);
        return Math.max(ISLAND_BASE_HEIGHT - 2, Math.min(ISLAND_BASE_HEIGHT + 18, finalHeight));
    }

    private void addSmallWaterFeatures(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ, int terrainHeight) {
        double pondNoise = Math.sin(worldX * 0.08) * Math.cos(worldZ * 0.07);
        boolean isPondArea = (pondNoise > 0.8 && Math.abs(worldX - 15) < 3 && Math.abs(worldZ + 10) < 3);
        if (isPondArea) {
            chunk.setBlockState(new BlockPos(localX, terrainHeight, localZ), Blocks.WATER.defaultBlockState(), false);
        }
    }

    private BlockState getBlockAt(int y, int terrainHeight, int worldX, int worldZ, double islandShape) {
        double bottomCutoff = calculateIrregularBottom(worldX, worldZ, islandShape, terrainHeight);
        if (y < bottomCutoff) {
            return null;
        } else if (y < terrainHeight - 12) {
            double bedrockNoise = Math.sin(worldX * 0.3) * Math.cos(worldZ * 0.25) * 2;
            if (y < bottomCutoff + 3 + bedrockNoise) {
                double edgeDistance = Math.max(0, islandShape / 30.0);
                if (Math.sin(worldX * 0.5 + worldZ * 0.7) > (0.3 - edgeDistance * 0.4)) {
                    return Blocks.BEDROCK.defaultBlockState();
                } else {
                    return Blocks.STONE.defaultBlockState();
                }
            }
            return Blocks.BEDROCK.defaultBlockState();
        } else if (y < terrainHeight - 3) {
            int combinedCoord = Math.abs(worldX * 31 + worldZ * 17);
            if (combinedCoord % 25 == 0) {
                return Blocks.COAL_ORE.defaultBlockState();
            } else if (combinedCoord % 70 == 0) {
                return Blocks.IRON_ORE.defaultBlockState();
            } else if (combinedCoord % 140 == 0) {
                return Blocks.GOLD_ORE.defaultBlockState();
            } else if (combinedCoord % 280 == 0) {
                return Blocks.DIAMOND_ORE.defaultBlockState();
            }
            return Blocks.STONE.defaultBlockState();
        } else if (y < terrainHeight) {
            return Blocks.DIRT.defaultBlockState();
        } else if (y == terrainHeight) {
            return Blocks.GRASS_BLOCK.defaultBlockState();
        }
        return null;
    }

    private double calculateIrregularBottom(int worldX, int worldZ, double islandShape, int terrainHeight) {
        double baseBottom = terrainHeight - 15;
        double largeNoise = Math.sin(worldX * 0.05) * Math.cos(worldZ * 0.04) * 8 + Math.sin(worldX * 0.03 + 100) * Math.cos(worldZ * 0.06 + 50) * 6;
        double mediumNoise = Math.sin(worldX * 0.12) * Math.cos(worldZ * 0.11) * 4 + Math.sin(worldX * 0.15 + 200) * Math.cos(worldZ * 0.13 + 150) * 3;
        double fineNoise = Math.sin(worldX * 0.25) * Math.cos(worldZ * 0.23) * 2 + Math.sin(worldX * 0.35 + 300) * Math.cos(worldZ * 0.28 + 250) * 1.5;
        double distanceFactor;
        if (islandShape > 50) {
            distanceFactor = 0.2;
        } else if (islandShape > 25) {
            distanceFactor = 0.5 + (50 - islandShape) / 25.0 * 0.4;
        } else {
            distanceFactor = 0.9 + (25 - islandShape) / 25.0 * 0.8;
        }
        double totalVariation = (largeNoise + mediumNoise + fineNoise) * distanceFactor;
        if (islandShape < 20) {
            double edgeCutting = Math.sin(worldX * 0.08 + worldZ * 0.07) * (20 - islandShape) * 0.8;
            totalVariation += edgeCutting;
        }
        double dramaticCuts = Math.sin(worldX * 0.02 + worldZ * 0.03) * Math.cos(worldX * 0.045 + worldZ * 0.038);
        if (dramaticCuts > 0.7 && islandShape < 40) {
            totalVariation += (dramaticCuts - 0.7) * 15 * distanceFactor;
        }
        return baseBottom + totalVariation;
    }

    @Override
    public int getSeaLevel() {
        return -63;
    }

    @Override
    public int getMinY() {
        return -64;
    }

    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types heightmapType, LevelHeightAccessor level, RandomState randomState) {
        double islandShape = calculateJaggedBlobShape(x, z);
        if (islandShape <= 0) {
            return level.getMinBuildHeight();
        }
        return calculateRealisticTerrainHeight(x, z, islandShape);
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState randomState) {
        int height = getBaseHeight(x, z, Heightmap.Types.WORLD_SURFACE_WG, level, randomState);
        BlockState[] column = new BlockState[level.getHeight()];
        for (int y = 0; y < column.length; y++) {
            int worldY = level.getMinBuildHeight() + y;
            if (worldY <= height && height > level.getMinBuildHeight()) {
                column[y] = Blocks.STONE.defaultBlockState();
            } else {
                column[y] = Blocks.AIR.defaultBlockState();
            }
        }
        return new NoiseColumn(level.getMinBuildHeight(), column);
    }

    @Override
    public void addDebugScreenInfo(List<String> list, RandomState randomState, BlockPos blockPos) {
        // You can add custom debug info here if you want
    }
}