package com.discotots.elysianisles.init;

import com.discotots.elysianisles.ElysianIslesMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraftforge.eventbus.api.IEventBus;

public class ModDimensions {

    public static final ResourceKey<Level> ELYSIAN_LEVEL_KEY = ResourceKey.create(Registries.DIMENSION,
            new ResourceLocation(ElysianIslesMod.MOD_ID, "elysian_dimension"));

    public static final ResourceKey<DimensionType> ELYSIAN_DIM_TYPE = ResourceKey.create(Registries.DIMENSION_TYPE,
            new ResourceLocation(ElysianIslesMod.MOD_ID, "elysian_type"));

    public static void register(IEventBus eventBus) {
        ElysianIslesMod.LOGGER.info("Registering Mod Dimensions for " + ElysianIslesMod.MOD_ID);
    }
}