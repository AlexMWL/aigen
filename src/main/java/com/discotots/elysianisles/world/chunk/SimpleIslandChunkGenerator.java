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
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.featuresize.TwoLayersFeatureSize;
import net.minecraft.world.level.levelgen.feature.foliageplacers.BlobFoliagePlacer;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;
import net.minecraft.world.level.levelgen.feature.trunkplacers.StraightTrunkPlacer;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.util.valueproviders.ConstantInt;

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
        // Add trees and vegetation during surface building
        generateTrees(region, chunk, randomState);
        addVegetation(region, chunk, randomState);
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion region) {
        // Enhanced mob spawning - spawn peaceful mobs on grass areas
        ChunkPos chunkPos = region.getCenter();
        RandomSource random = region.getRandom();

        // Only spawn in chunks near the island center
        double distFromCenter = Math.sqrt(
                Math.pow(chunkPos.x * 16 - ISLAND_CENTER_X, 2) +
                        Math.pow(chunkPos.z * 16 - ISLAND_CENTER_Z, 2)
        );

        if (distFromCenter < 80) { // Within reasonable distance of island center
            for (int attempts = 0; attempts < 8; attempts++) {
                int x = random.nextInt(16);
                int z = random.nextInt(16);
                int worldX = chunkPos.getMinBlockX() + x;
                int worldZ = chunkPos.getMinBlockZ() + z;

                // Find surface height
                for (int y = region.getMaxBuildHeight() - 1; y > region.getMinBuildHeight(); y--) {
                    BlockPos pos = new BlockPos(worldX, y, worldZ);
                    if (region.getBlockState(pos).is(Blocks.GRASS_BLOCK)) {
                        BlockPos spawnPos = pos.above();

                        // Check if spawn location is safe
                        if (region.getBlockState(spawnPos).isAir() &&
                                region.getBlockState(spawnPos.above()).isAir()) {

                            // Spawn different animals based on random chance
                            if (random.nextFloat() < 0.15f) { // 15% chance per attempt
                                spawnAnimal(region, spawnPos, random);
                            }
                        }
                        break;
                    }
                }
            }
        }
    }

    private void spawnAnimal(WorldGenRegion region, BlockPos pos, RandomSource random) {
        // This would require entity registration - for now we'll just log
        // In a full implementation, you'd spawn entities here
        float chance = random.nextFloat();
        String animalType;

        if (chance < 0.3f) {
            animalType = "pig";
        } else if (chance < 0.5f) {
            animalType = "cow";
        } else if (chance < 0.7f) {
            animalType = "sheep";
        } else if (chance < 0.85f) {
            animalType = "chicken";
        } else {
            animalType = "rabbit";
        }

        // Log for debugging - you'd actually spawn entities here
        System.out.println("Would spawn " + animalType + " at " + pos);
    }

    private void generateTrees(WorldGenRegion region, ChunkAccess chunk, RandomState randomState) {
        RandomSource random = region.getRandom();
        ChunkPos chunkPos = chunk.getPos();

        // Generate trees with varying density based on distance from center
        for (int attempt = 0; attempt < 12; attempt++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int worldX = chunkPos.getMinBlockX() + x;
            int worldZ = chunkPos.getMinBlockZ() + z;

            // Check if we're on the island
            double islandShape = calculateJaggedBlobShape(worldX, worldZ);
            if (islandShape <= 5) continue; // Only place trees well within island bounds

            // Find surface
            for (int y = region.getMaxBuildHeight() - 1; y > region.getMinBuildHeight(); y--) {
                BlockPos surfacePos = new BlockPos(worldX, y, worldZ);
                if (region.getBlockState(surfacePos).is(Blocks.GRASS_BLOCK)) {
                    BlockPos treePos = surfacePos.above();

                    // Check if there's enough space for a tree
                    if (hasSpaceForTree(region, treePos)) {
                        // Plant different tree types
                        if (random.nextFloat() < 0.7f) {
                            generateOakTree(region, treePos, random);
                        } else if (random.nextFloat() < 0.3f) {
                            generateBirchTree(region, treePos, random);
                        } else {
                            generateSpruceTree(region, treePos, random);
                        }
                    }
                    break;
                }
            }
        }
    }

    private void addVegetation(WorldGenRegion region, ChunkAccess chunk, RandomState randomState) {
        RandomSource random = region.getRandom();
        ChunkPos chunkPos = chunk.getPos();

        // Add grass, flowers, and other vegetation
        for (int attempt = 0; attempt < 32; attempt++) {
            int x = random.nextInt(16);
            int z = random.nextInt(16);
            int worldX = chunkPos.getMinBlockX() + x;
            int worldZ = chunkPos.getMinBlockZ() + z;

            // Check if we're on the island
            double islandShape = calculateJaggedBlobShape(worldX, worldZ);
            if (islandShape <= 2) continue;

            // Find surface
            for (int y = region.getMaxBuildHeight() - 1; y > region.getMinBuildHeight(); y--) {
                BlockPos surfacePos = new BlockPos(worldX, y, worldZ);
                if (region.getBlockState(surfacePos).is(Blocks.GRASS_BLOCK)) {
                    BlockPos plantPos = surfacePos.above();

                    if (region.getBlockState(plantPos).isAir()) {
                        float vegChance = random.nextFloat();

                        if (vegChance < 0.4f) {
                            region.setBlock(plantPos, Blocks.GRASS.defaultBlockState(), 3);
                        } else if (vegChance < 0.45f) {
                            region.setBlock(plantPos, Blocks.TALL_GRASS.defaultBlockState(), 3);
                        } else if (vegChance < 0.47f) {
                            region.setBlock(plantPos, Blocks.DANDELION.defaultBlockState(), 3);
                        } else if (vegChance < 0.49f) {
                            region.setBlock(plantPos, Blocks.POPPY.defaultBlockState(), 3);
                        } else if (vegChance < 0.5f) {
                            region.setBlock(plantPos, Blocks.BLUE_ORCHID.defaultBlockState(), 3);
                        }
                    }
                    break;
                }
            }
        }
    }

    private boolean hasSpaceForTree(WorldGenRegion region, BlockPos pos) {
        // Check 3x3 area and height clearance
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                for (int y = 0; y < 6; y++) {
                    BlockPos checkPos = pos.offset(x, y, z);
                    if (!region.getBlockState(checkPos).isAir()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void generateOakTree(WorldGenRegion region, BlockPos pos, RandomSource random) {
        int height = 4 + random.nextInt(3); // 4-6 blocks tall

        // Generate trunk
        for (int y = 0; y < height; y++) {
            region.setBlock(pos.above(y), Blocks.OAK_LOG.defaultBlockState(), 3);
        }

        // Generate leaves (simple blob pattern)
        BlockPos leafTop = pos.above(height);

        // Top layer
        region.setBlock(leafTop, Blocks.OAK_LEAVES.defaultBlockState(), 3);

        // Middle layers
        for (int layer = 1; layer <= 2; layer++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (Math.abs(x) == 2 && Math.abs(z) == 2) continue; // Skip corners

                    BlockPos leafPos = leafTop.offset(x, -layer, z);
                    if (random.nextFloat() < 0.85f) { // 85% chance for each leaf block
                        region.setBlock(leafPos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void generateBirchTree(WorldGenRegion region, BlockPos pos, RandomSource random) {
        int height = 5 + random.nextInt(2); // 5-6 blocks tall

        // Generate trunk
        for (int y = 0; y < height; y++) {
            region.setBlock(pos.above(y), Blocks.BIRCH_LOG.defaultBlockState(), 3);
        }

        // Generate leaves
        BlockPos leafTop = pos.above(height);
        region.setBlock(leafTop, Blocks.BIRCH_LEAVES.defaultBlockState(), 3);

        for (int layer = 1; layer <= 2; layer++) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0 && layer == 1) continue; // Skip center in first layer

                    BlockPos leafPos = leafTop.offset(x, -layer, z);
                    if (random.nextFloat() < 0.9f) {
                        region.setBlock(leafPos, Blocks.BIRCH_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
    }

    private void generateSpruceTree(WorldGenRegion region, BlockPos pos, RandomSource random) {
        int height = 6 + random.nextInt(3); // 6-8 blocks tall

        // Generate trunk
        for (int y = 0; y < height; y++) {
            region.setBlock(pos.above(y), Blocks.SPRUCE_LOG.defaultBlockState(), 3);
        }

        // Generate leaves (christmas tree shape)
        BlockPos leafTop = pos.above(height);
        region.setBlock(leafTop, Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);

        // Generate layers getting wider towards bottom
        for (int layer = 1; layer <= 4; layer++) {
            int radius = Math.min(2, (layer + 1) / 2);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    if (x == 0 && z == 0) continue; // Skip center (trunk)

                    double distance = Math.sqrt(x * x + z * z);
                    if (distance <= radius && random.nextFloat() < 0.9f) {
                        BlockPos leafPos = leafTop.offset(x, -layer, z);
                        region.setBlock(leafPos, Blocks.SPRUCE_LEAVES.defaultBlockState(), 3);
                    }
                }
            }
        }
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