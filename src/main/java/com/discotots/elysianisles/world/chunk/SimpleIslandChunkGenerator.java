package com.discotots.elysianisles.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SimpleIslandChunkGenerator extends ChunkGenerator {
    public static final Codec<SimpleIslandChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, SimpleIslandChunkGenerator::new)
    );

    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final int ISLAND_BASE_HEIGHT = 72;

    public SimpleIslandChunkGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected Codec<? extends ChunkGenerator> codec() {
        return CODEC;
    }

    // COMPLETELY BLOCK ALL STRUCTURE GENERATION
    public void generateStructures(net.minecraft.world.level.StructureManager structureManager, ChunkAccess chunk, StructureManager structures, RandomState randomState) {
        // Intentionally empty - no structures at all
    }

    public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement placement, RandomState randomState) {
        return List.of(); // Return empty list - no ring structures
    }

    public boolean hasStructureChunkInRange(StructureSet structureSet, RandomState randomState, long seed, int chunkX, int chunkZ, int range) {
        return false; // Never has structures
    }

    @Override
    public void applyCarvers(WorldGenRegion region, long seed, RandomState randomState, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // No carvers
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

        if (islandShape <= 0) {
            // Outside island - ensure pure air (no structures can spawn here)
            return;
        }

        // Calculate terrain height
        int terrainHeight = calculateRealisticTerrainHeight(worldX, worldZ, islandShape);

        // Generate the terrain column
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
            BlockState block = getBlockAt(y, terrainHeight, worldX, worldZ);
            if (block != null) {
                chunk.setBlockState(new BlockPos(localX, y, localZ), block, false);
            }
        }

        // Add small water features
        addSmallWaterFeatures(chunk, localX, localZ, worldX, worldZ, terrainHeight);
    }

    private double calculateJaggedBlobShape(int worldX, int worldZ) {
        double dx = worldX - ISLAND_CENTER_X;
        double dz = worldZ - ISLAND_CENTER_Z;

        // Base distance
        double distance = Math.sqrt(dx * dx + dz * dz);
        double baseRadius = 90;

        // Create VERY jagged edges using multiple noise functions
        double angle = Math.atan2(dz, dx);

        // Large spikes and indentations
        double largeJags = Math.sin(angle * 6) * 35 +      // Big peninsulas and bays
                Math.sin(angle * 12) * 20 +     // Medium jags
                Math.sin(angle * 18) * 12;      // Small jags

        // Additional chaos using world coordinates
        double chaosX = Math.sin(worldX * 0.08) * 15 +     // X-based variation
                Math.sin(worldX * 0.15) * 8;       // Finer X variation
        double chaosZ = Math.sin(worldZ * 0.07) * 18 +     // Z-based variation
                Math.sin(worldZ * 0.13) * 10;      // Finer Z variation

        // Combine all chaos
        double totalChaos = largeJags + chaosX + chaosZ;

        // Create very irregular radius
        double actualRadius = baseRadius + totalChaos;

        // Add fractal-like detail for extra jaggedness
        double fractalNoise = Math.sin(worldX * 0.2) * Math.cos(worldZ * 0.18) * 8 +
                Math.sin(worldX * 0.3) * Math.cos(worldZ * 0.25) * 5;

        actualRadius += fractalNoise;

        // Ensure minimum size
        actualRadius = Math.max(20, actualRadius);

        return actualRadius - distance;
    }

    private int calculateRealisticTerrainHeight(int worldX, int worldZ, double islandShape) {
        // Base height - gentle rise toward center
        double heightFromShape = Math.max(0, islandShape / 120.0) * 12;

        // Gentle terrain variation
        double hills = Math.sin(worldX * 0.06) * Math.cos(worldZ * 0.05) * 4;
        double mediumTerrain = Math.sin(worldX * 0.12) * Math.cos(worldZ * 0.11) * 2;
        double fineTerrain = Math.sin(worldX * 0.2) * Math.cos(worldZ * 0.18) * 1;

        // Small cliff areas
        double cliffNoise = Math.sin(worldX * 0.03) * Math.cos(worldZ * 0.025);
        double cliffHeight = 0;
        if (cliffNoise > 0.6 && islandShape > 0.3) {
            cliffHeight = (cliffNoise - 0.6) * 8;
        }

        int finalHeight = ISLAND_BASE_HEIGHT + (int)(heightFromShape + hills + mediumTerrain + fineTerrain + cliffHeight);

        return Math.max(ISLAND_BASE_HEIGHT - 2, Math.min(ISLAND_BASE_HEIGHT + 18, finalHeight));
    }

    private void addSmallWaterFeatures(ChunkAccess chunk, int localX, int localZ, int worldX, int worldZ, int terrainHeight) {
        // Very limited water features
        double pondNoise = Math.sin(worldX * 0.08) * Math.cos(worldZ * 0.07);

        // Only 1-2 small ponds
        boolean isPondArea = (pondNoise > 0.8 && Math.abs(worldX - 15) < 3 && Math.abs(worldZ + 10) < 3);

        if (isPondArea) {
            chunk.setBlockState(new BlockPos(localX, terrainHeight, localZ), Blocks.WATER.defaultBlockState(), false);
        }
    }

    private BlockState getBlockAt(int y, int terrainHeight, int worldX, int worldZ) {
        if (y < terrainHeight - 15) {
            return null; // Air below island
        } else if (y < terrainHeight - 12) {
            return Blocks.BEDROCK.defaultBlockState();
        } else if (y < terrainHeight - 3) {
            // Ore placement
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

    }
}