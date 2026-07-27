package com.flatworld.mod.dimension;

import com.flatworld.mod.FlatWorldMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Holds the {@link ResourceKey}s that identify our custom dimension and its dimension type.
 * <p>
 * The actual dimension is *data driven*: it is defined by the JSON files under
 * {@code data/flatworld/dimension/flat_world.json} and
 * {@code data/flatworld/dimension_type/flat_world_type.json}. These keys are only used
 * in code to reference that data (e.g. for teleporting the player).
 */
public class ModDimensions {

    public static final ResourceKey<Level> FLAT_WORLD_LEVEL = ResourceKey.create(
            Registries.DIMENSION,
            ResourceLocation.fromNamespaceAndPath(FlatWorldMod.MOD_ID, "flat_world")
    );

    public static final ResourceKey<DimensionType> FLAT_WORLD_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            ResourceLocation.fromNamespaceAndPath(FlatWorldMod.MOD_ID, "flat_world_type")
    );
}
