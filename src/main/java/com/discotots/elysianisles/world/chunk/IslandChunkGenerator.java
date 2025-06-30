package com.discotots.elysianisles.world.chunk;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.WorldGenRegion;
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
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class IslandChunkGenerator extends ChunkGenerator {
    public static final Codec<IslandChunkGenerator> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BiomeSource.CODEC.fieldOf("biome_source").forGetter(generator -> generator.biomeSource)
            ).apply(instance, IslandChunkGenerator::new)
    );

    private static final int ISLAND_CENTER_X = 8;
    private static final int ISLAND_CENTER_Z = -5;
    private static final int ISLAND_BASE_HEIGHT = 72;

    public IslandChunkGenerator(BiomeSource biomeSource) {
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
        // Calculate if this chunk area is near the island
        double minDistance = Double.MAX_VALUE;

        // Check all positions in the chunk range
        for (int x = chunkX - range; x <= chunkX + range; x++) {
            for (int z = chunkZ - range; z <= chunkZ + range; z++) {
                int worldX = x * 16 + 8; // Center of chunk
                int worldZ = z * 16 + 8;

                // Check distance to island
                double dx = worldX - ISLAND_CENTER_X;
                double dz = worldZ - ISLAND_CENTER_Z;
                double distance = Math.sqrt(dx * dx + dz * dz);
                minDistance = Math.min(minDistance, distance);
            }
        }

        // Block structures within 200 blocks of island (above AND below)
        return minDistance > 200;
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
            // Fill with air explicitly to prevent any structure generation
            for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                chunk.setBlockState(new BlockPos(localX, y, localZ), Blocks.AIR.defaultBlockState(), false);
            }
            return;
        }

        // Calculate terrain height
        int terrainHeight = calculateRealisticTerrainHeight(worldX, worldZ, islandShape);

        // Generate the terrain column with irregular bottom
        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
            BlockState block = getBlockAt(y, terrainHeight, worldX, worldZ, islandShape);
            if (block != null) {
                chunk.setBlockState(new BlockPos(localX, y, localZ), block, false);
            } else {
                // Explicitly set air to prevent structures
                chunk.setBlockState(new BlockPos(localX, y, localZ), Blocks.AIR.defaultBlockState(), false);
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

    private BlockState getBlockAt(int y, int terrainHeight, int worldX, int worldZ, double islandShape) {
        // Create highly irregular bottom using multiple noise layers
        double bottomCutoff = calculateIrregularBottom(worldX, worldZ, islandShape, terrainHeight);

        if (y < bottomCutoff) {
            return null; // Air below irregular island bottom
        } else if (y < terrainHeight - 12) {
            // Irregular bedrock layer
            double bedrockNoise = Math.sin(worldX * 0.3) * Math.cos(worldZ * 0.25) * 2;
            if (y < bottomCutoff + 3 + bedrockNoise) {
                // Vary between bedrock and stone near bottom edges
                double edgeDistance = Math.max(0, islandShape / 30.0);
                if (Math.sin(worldX * 0.5 + worldZ * 0.7) > (0.3 - edgeDistance * 0.4)) {
                    return Blocks.BEDROCK.defaultBlockState();
                } else {
                    return Blocks.STONE.defaultBlockState();
                }
            }
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

    private double calculateIrregularBottom(int worldX, int worldZ, double islandShape, int terrainHeight) {
        // Base bottom level
        double baseBottom = terrainHeight - 15;

        // Create highly irregular bottom shape using multiple noise layers

        // Large scale irregularity - creates major indentations and protrusions
        double largeNoise = Math.sin(worldX * 0.05) * Math.cos(worldZ * 0.04) * 8 +
                Math.sin(worldX * 0.03 + 100) * Math.cos(worldZ * 0.06 + 50) * 6;

        // Medium scale noise - creates medium sized variations
        double mediumNoise = Math.sin(worldX * 0.12) * Math.cos(worldZ * 0.11) * 4 +
                Math.sin(worldX * 0.15 + 200) * Math.cos(worldZ * 0.13 + 150) * 3;

        // Fine detail noise - creates small scale roughness
        double fineNoise = Math.sin(worldX * 0.25) * Math.cos(worldZ * 0.23) * 2 +
                Math.sin(worldX * 0.35 + 300) * Math.cos(worldZ * 0.28 + 250) * 1.5;

        // Distance-based tapering - more dramatic near edges
        double distanceFactor;
        if (islandShape > 50) {
            distanceFactor = 0.2; // Minimal variation in center
        } else if (islandShape > 25) {
            distanceFactor = 0.5 + (50 - islandShape) / 25.0 * 0.4; // Gradual increase
        } else {
            distanceFactor = 0.9 + (25 - islandShape) / 25.0 * 0.8; // Maximum variation at edges
        }

        // Combine all noise with distance scaling
        double totalVariation = (largeNoise + mediumNoise + fineNoise) * distanceFactor;

        // Add dramatic edge cutting for very irregular edges
        if (islandShape < 20) {
            double edgeCutting = Math.sin(worldX * 0.08 + worldZ * 0.07) * (20 - islandShape) * 0.8;
            totalVariation += edgeCutting;
        }

        // Create some areas that cut up much higher for very irregular look
        double dramaticCuts = Math.sin(worldX * 0.02 + worldZ * 0.03) *
                Math.cos(worldX * 0.045 + worldZ * 0.038);
        if (dramaticCuts > 0.7 && islandShape < 40) {
            totalVariation += (dramaticCuts - 0.7) * 15 * distanceFactor;
        }

        return baseBottom + totalVariation;
    }


    private double calculateBottomTaper(int worldX, int worldZ, double islandShape, int terrainHeight) {
        double distanceFromEdge = islandShape;

        // Create a smooth taper that gets more dramatic near the edges
        double taperFactor;
        if (distanceFromEdge > 40) {
            taperFactor = 0; // No taper in the center
        } else if (distanceFromEdge > 20) {
            // Gradual taper in middle area
            taperFactor = (40 - distanceFromEdge) / 20.0;
        } else {
            // Dramatic taper near edges
            taperFactor = 1.0 + (20 - distanceFromEdge) / 10.0;
        }

        // Add some noise for irregular bottom shape
        double noise = Math.sin(worldX * 0.1) * Math.cos(worldZ * 0.1) * 2 +
                Math.sin(worldX * 0.2) * Math.cos(worldZ * 0.15) * 1;

        // Calculate how much to taper the bottom
        double taper = taperFactor * (8 + noise); // Base taper of 8 blocks plus noise

        // Make sure we don't taper too much
        return Math.max(-12, -taper);
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