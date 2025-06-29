package com.discotots.elysianisles.init;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.world.biome.ElysianBiomeSource;
import com.discotots.elysianisles.world.chunk.ElysianChunkGenerator;
import net.minecraft.core.registries.Registries;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegisterEvent;

@Mod.EventBusSubscriber(modid = ElysianIslesMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModRegistryHandler {

    @SubscribeEvent
    public static void registerCodecs(RegisterEvent event) {
        // Register Chunk Generator
        if (event.getRegistryKey().equals(Registries.CHUNK_GENERATOR)) {
            event.register(Registries.CHUNK_GENERATOR,
                    ElysianIslesMod.modLoc("elysian_chunk_generator"),
                    () -> ElysianChunkGenerator.CODEC);

            ElysianIslesMod.LOGGER.info("Registered Elysian Chunk Generator");
        }

        // Register Biome Source
        if (event.getRegistryKey().equals(Registries.BIOME_SOURCE)) {
            event.register(Registries.BIOME_SOURCE,
                    ElysianIslesMod.modLoc("elysian_biome_source"),
                    () -> ElysianBiomeSource.CODEC);

            ElysianIslesMod.LOGGER.info("Registered Elysian Biome Source");
        }
    }
}