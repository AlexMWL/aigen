package com.discotots.elysianisles.init;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.world.chunk.IslandChunkGenerator;
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
                    ElysianIslesMod.modLoc("simple_island_chunk_generator"),
                    () -> IslandChunkGenerator.CODEC);

            ElysianIslesMod.LOGGER.info("Registered Simple Island Chunk Generator");
        }
    }
}