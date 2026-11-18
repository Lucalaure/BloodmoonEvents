package com.theseeklab.bloodmoon.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.theseeklab.bloodmoon.restore.BrokenBlockRestorer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Notes where players place blocks during a Blood Moon, so those blocks aren't restored if the horde breaks them
 * (see {@link BrokenBlockRestorer}). Wraps the call in {@code place} rather than {@code placeBlock} itself, because
 * some block items (beds) override {@code placeBlock} without calling it.
 */
@Mixin(BlockItem.class)
public abstract class BlockItemMixin {
	@WrapOperation(
		method = "place",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/item/BlockItem;placeBlock(Lnet/minecraft/world/item/context/BlockPlaceContext;Lnet/minecraft/world/level/block/state/BlockState;)Z"
		)
	)
	private boolean bloodmoon$trackPlacement(
		final BlockItem item, final BlockPlaceContext context, final BlockState state, final Operation<Boolean> original
	) {
		boolean placed = original.call(item, context, state);
		if (placed && context.getPlayer() != null && context.getLevel() instanceof ServerLevel level) {
			BrokenBlockRestorer.onPlayerPlaced(level, context.getClickedPos());
		}
		return placed;
	}
}
