package com.flatworld.mod.item;

import com.flatworld.mod.FlatWorldMod;
import com.flatworld.mod.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {

    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(FlatWorldMod.MOD_ID);

    public static final DeferredItem<BlockItem> COMPRESSED_STONE = ITEMS.registerSimpleBlockItem(
            "compressed_stone",
            ModBlocks.COMPRESSED_STONE,
            new Item.Properties()
    );
}
