package com.discotots.elysianisles;

import com.discotots.elysianisles.init.ModBlocks;
import com.discotots.elysianisles.init.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(ElysianIslesMod.MOD_ID)
public class ElysianIslesMod {
    public static final String MOD_ID = "elysianisles";
    public static final Logger LOGGER = LogManager.getLogger();

    public ElysianIslesMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register basic mod content
        ModBlocks.register(modEventBus);
        ModItems.register(modEventBus);

        // Setup events
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        LOGGER.info("Elysian Isles - Common setup complete");
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        LOGGER.info("Elysian Isles - Client setup complete");
    }

    public static ResourceLocation modLoc(String name) {
        return new ResourceLocation(MOD_ID, name);
    }
}