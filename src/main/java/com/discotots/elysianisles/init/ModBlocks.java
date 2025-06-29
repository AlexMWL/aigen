
package com.discotots.elysianisles.init;

import com.discotots.elysianisles.ElysianIslesMod;
import com.discotots.elysianisles.block.PsionicPortalBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, ElysianIslesMod.MOD_ID);

    // Psionic Portal Block
    public static final RegistryObject<Block> PSIONIC_PORTAL = BLOCKS.register("psionic_portal",
            () -> new PsionicPortalBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(-1.0F)
                    .sound(net.minecraft.world.level.block.SoundType.GLASS)
                    .lightLevel((state) -> 10)
                    .noCollission()
                    .noOcclusion()));

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}