package com.flatworld.mod.item;

import com.flatworld.mod.FlatWorldMod;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(net.minecraft.core.registries.Registries.CREATIVE_MODE_TAB, FlatWorldMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> FLAT_WORLD_TAB = CREATIVE_TABS.register(
            "flat_world_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.flatworld.flat_world_tab"))
                    .icon(() -> new ItemStack(ModItems.COMPRESSED_STONE.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(ModItems.COMPRESSED_STONE.get());
                    })
                    .build()
    );
}
