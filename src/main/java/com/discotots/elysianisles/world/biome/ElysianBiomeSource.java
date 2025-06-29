package com.discotots.elysianisles.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeSource;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.Climate;
import java.util.stream.Stream;

public class ElysianBiomeSource extends BiomeSource {
    public static final Codec<ElysianBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.LONG.fieldOf("seed").forGetter(source -> source.seed)
            ).apply(instance, ElysianBiomeSource::new)
    );

    private final long seed;
    private final Holder<Biome> defaultBiome;

    public ElysianBiomeSource(long seed) {
        this.seed = seed;
        // We'll set a default biome - in a real implementation you'd get this from a registry
        this.defaultBiome = null; // Will be set properly in actual usage
    }

    public ElysianBiomeSource(HolderGetter<Biome> biomeRegistry, long seed) {
        this.seed = seed;
        this.defaultBiome = biomeRegistry.getOrThrow(Biomes.PLAINS);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(defaultBiome != null ? defaultBiome :
                        // Fallback for when defaultBiome is not set
                        Stream.of(Biomes.PLAINS).map(biome -> null).findFirst().orElse(null))
                .filter(biome -> biome != null);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return defaultBiome != null ? defaultBiome :
                // This is a fallback - in real usage, we'd get from proper registry
                Stream.of(Biomes.PLAINS).map(biome -> null).findFirst().orElse(null);
    }
}
