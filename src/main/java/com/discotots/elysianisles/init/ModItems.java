
package com.discotots.elysianisles.init;

import com.discotots.elysianisles.ElysianIslesMod;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, ElysianIslesMod.MOD_ID);

    // Psionic Portal Block Item (for creative/admin use)
    public static final RegistryObject<Item> PSIONIC_PORTAL = ITEMS.register("psionic_portal",
            () -> new BlockItem(ModBlocks.PSIONIC_PORTAL.get(),
                    new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}