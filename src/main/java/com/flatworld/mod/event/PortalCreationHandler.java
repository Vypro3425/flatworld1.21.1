package com.flatworld.mod.event;

import com.flatworld.mod.block.FlatPortalShape;
import com.flatworld.mod.block.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.Optional;

/**
 * Listens for the player right-clicking a {@code Compressed Stone} block with vanilla
 * Flint and Steel, and — if a valid frame is found — lights the portal, exactly like
 * lighting a Nether portal with obsidian.
 */
public class PortalCreationHandler {

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        if (level.isClientSide) {
            return;
        }

        if (!event.getItemStack().is(Items.FLINT_AND_STEEL)) {
            return;
        }

        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModBlocks.COMPRESSED_STONE.get())) {
            return;
        }

        // Try to light a portal using the empty space adjacent to the clicked face.
        BlockPos ignitionPos = pos.relative(event.getFace());
        Optional<FlatPortalShape> shape = FlatPortalShape.findValidFrame(level, ignitionPos);

        if (shape.isPresent()) {
            shape.get().createPortalBlocks();
            level.playSound(event.getEntity(), pos, SoundEvents.FLINTANDSTEEL_USE, SoundSource.BLOCKS, 1.0F,
                    level.getRandom().nextFloat() * 0.4F + 0.8F);

            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && event.getEntity() instanceof net.minecraft.world.entity.LivingEntity livingEntity) {
                event.getItemStack().hurtAndBreak(1, serverLevel, livingEntity, item -> {
                });
            }

            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }
}
