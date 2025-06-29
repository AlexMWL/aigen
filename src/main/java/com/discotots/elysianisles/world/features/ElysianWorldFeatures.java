package com.discotots.elysianisles.world.features;

import com.discotots.elysianisles.ElysianIslesMod;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ElysianWorldFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, ElysianIslesMod.MOD_ID);

    // Custom feature for placing starter trees on islands
    public static final RegistryObject<Feature<NoneFeatureConfiguration>> ELYSIAN_STARTER_FEATURE =
            FEATURES.register("elysian_starter_feature", () -> new ElysianStarterFeature(NoneFeatureConfiguration.CODEC));

    public static void register(IEventBus eventBus) {
        FEATURES.register(eventBus);
    }

    public static class ElysianStarterFeature extends Feature<NoneFeatureConfiguration> {
        public ElysianStarterFeature(com.mojang.serialization.Codec<NoneFeatureConfiguration> codec) {
            super(codec);
        }

        @Override
        public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
            WorldGenLevel level = context.level();
            BlockPos pos = context.origin();
            RandomSource random = context.random();

            // Only place features on grass blocks
            if (!level.getBlockState(pos.below()).is(Blocks.GRASS_BLOCK)) {
                return false;
            }

            // Randomly decide what to place
            int choice = random.nextInt(100);

            if (choice < 40) {
                // Place a tree (40% chance)
                return placeTree(level, pos, random);
            } else if (choice < 60) {
                // Place flowers/grass (20% chance)
                return placeVegetation(level, pos, random);
            } else if (choice < 80) {
                // Place some rocks/stone patches (20% chance)
                return placeRocks(level, pos, random);
            }
            // 20% chance of nothing

            return false;
        }

        private boolean placeTree(WorldGenLevel level, BlockPos pos, RandomSource random) {
            // Simple oak tree generation
            int height = 4 + random.nextInt(3); // 4-6 blocks tall

            // Place trunk
            for (int i = 0; i < height; i++) {
                level.setBlock(pos.above(i), Blocks.OAK_LOG.defaultBlockState(), 3);
            }

            // Place leaves in a simple pattern
            BlockPos leafStart = pos.above(height - 2);
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    for (int y = 0; y <= 2; y++) {
                        BlockPos leafPos = leafStart.offset(x, y, z);
                        double distance = Math.sqrt(x * x + z * z + y * y * 0.5);

                        if (distance <= 2.5 && random.nextFloat() < 0.8) {
                            if (level.getBlockState(leafPos).isAir()) {
                                level.setBlock(leafPos, Blocks.OAK_LEAVES.defaultBlockState(), 3);
                            }
                        }
                    }
                }
            }

            return true;
        }

        private boolean placeVegetation(WorldGenLevel level, BlockPos pos, RandomSource random) {
            // Place some grass and flowers
            int choice = random.nextInt(10);

            if (choice < 6) {
                level.setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 3);
            } else if (choice < 8) {
                level.setBlock(pos, Blocks.DANDELION.defaultBlockState(), 3);
            } else {
                level.setBlock(pos, Blocks.POPPY.defaultBlockState(), 3);
            }

            return true;
        }

        private boolean placeRocks(WorldGenLevel level, BlockPos pos, RandomSource random) {
            // Place small stone formations
            level.setBlock(pos.below(), Blocks.STONE.defaultBlockState(), 3);

            if (random.nextBoolean()) {
                level.setBlock(pos, Blocks.COBBLESTONE.defaultBlockState(), 3);
            }

            // Maybe place a small pile
            for (int i = 0; i < random.nextInt(3); i++) {
                BlockPos rockPos = pos.offset(
                        random.nextInt(3) - 1,
                        0,
                        random.nextInt(3) - 1
                );

                if (level.getBlockState(rockPos).isAir() &&
                        level.getBlockState(rockPos.below()).is(Blocks.GRASS_BLOCK)) {
                    level.setBlock(rockPos, Blocks.COBBLESTONE.defaultBlockState(), 3);
                }
            }

            return true;
        }
    }
}
