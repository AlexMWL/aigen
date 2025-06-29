package com.discotots.elysianisles.world.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeSource;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.Climate;

import java.util.stream.Stream;

public class ElysianBiomeSource extends BiomeSource {
    public static final Codec<ElysianBiomeSource> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryOps.retrieveGetter(Registries.BIOME).forGetter(source -> source.biomeRegistry),
                    Codec.LONG.fieldOf("seed").forGetter(source -> source.seed)
            ).apply(instance, ElysianBiomeSource::new)
    );

    private final HolderGetter<Biome> biomeRegistry;
    private final long seed;
    private final Holder<Biome> defaultBiome;

    public ElysianBiomeSource(HolderGetter<Biome> biomeRegistry, long seed) {
        this.biomeRegistry = biomeRegistry;
        this.seed = seed;
        // Use plains as our default biome - will give it a gloomy sky color via JSON
        this.defaultBiome = biomeRegistry.getOrThrow(Biomes.PLAINS);
    }

    @Override
    protected Codec<? extends BiomeSource> codec() {
        return CODEC;
    }

    @Override
    protected Stream<Holder<Biome>> collectPossibleBiomes() {
        return Stream.of(defaultBiome);
    }

    @Override
    public Holder<Biome> getNoiseBiome(int x, int y, int z, Climate.Sampler sampler) {
        return defaultBiome;
    }
}