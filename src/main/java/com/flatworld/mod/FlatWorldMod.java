package com.flatworld.mod;

import com.flatworld.mod.block.ModBlocks;
import com.flatworld.mod.event.DayNightCycleHandler;
import com.flatworld.mod.event.NoMobSpawnHandler;
import com.flatworld.mod.event.PortalCreationHandler;
import com.flatworld.mod.item.ModCreativeTabs;
import com.flatworld.mod.item.ModItems;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Entry point of the Flat World Portal mod.
 * <p>
 * The mod adds a "Compressed Stone" block, crafted from 9 cobblestone, which can be arranged
 * into a Nether-Portal-like frame and lit with vanilla Flint and Steel. Lighting the frame
 * creates a green portal that leads to a custom peaceful superflat dimension.
 */
@Mod(FlatWorldMod.MOD_ID)
public class FlatWorldMod {

    public static final String MOD_ID = "flatworld";
    public static final Logger LOGGER = LoggerFactory.getLogger(FlatWorldMod.class);

    public FlatWorldMod(IEventBus modEventBus) {
        // Register deferred registers on the mod bus.
        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_TABS.register(modEventBus);

        // Register game-logic listeners on the game (forge) bus.
        NeoForge.EVENT_BUS.register(new DayNightCycleHandler());
        NeoForge.EVENT_BUS.register(new NoMobSpawnHandler());
        NeoForge.EVENT_BUS.register(new PortalCreationHandler());

        LOGGER.info("Flat World Portal mod initialized");
    }
}
