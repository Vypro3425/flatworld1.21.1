package com.flatworld.mod.block;

import com.flatworld.mod.FlatWorldMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(FlatWorldMod.MOD_ID);

    /** The frame block for the portal. Crafted from 9 cobblestone. */
    public static final DeferredBlock<Block> COMPRESSED_STONE = BLOCKS.registerBlock(
            "compressed_stone",
            Block::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.STONE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 6.0F)
                    .sound(SoundType.STONE)
    );

    /** The portal block itself, spawned inside a Compressed Stone frame. */
    public static final DeferredBlock<FlatPortalBlock> FLAT_PORTAL = BLOCKS.registerBlock(
            "flat_portal",
            FlatPortalBlock::new,
            BlockBehaviour.Properties.of()
                    .noCollission()
                    .randomTicks()
                    .strength(-1.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 11)
                    .noLootTable()
                    .pushReaction(PushReaction.BLOCK)
    );
}
