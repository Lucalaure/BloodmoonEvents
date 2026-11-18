package com.theseeklab.bloodmoon.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.restore.BrokenBlockRestorer;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerExplosion;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks blown up by a Blood Moon creeper are remembered so they come back after dawn (see
 * {@link BrokenBlockRestorer}), and drop nothing so the explosion can't be farmed. Container contents still spill.
 */
@Mixin(ServerExplosion.class)
public abstract class ServerExplosionMixin {
	@Unique
	private @Nullable LongSet bloodmoon$restoredPositions;

	@Inject(method = "interactWithBlocks", at = @At("HEAD"))
	private void bloodmoon$recordBlocks(final List<BlockPos> blocks, final CallbackInfo ci) {
		Explosion explosion = (Explosion) (Object) this;
		ServerLevel level = explosion.level();
		Entity source = explosion.getDirectSourceEntity();
		if (source instanceof Mob mob && BloodMoonManager.isActive(level) && HordeMobs.isUpgradedNow(mob)) {
			bloodmoon$restoredPositions = BrokenBlockRestorer.recordAll(level, blocks);
		}
	}

	@WrapWithCondition(
		method = "interactWithBlocks",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
		)
	)
	private boolean bloodmoon$skipRestoredDrops(final Level level, final BlockPos pos, final ItemStack stack) {
		return bloodmoon$restoredPositions == null || !bloodmoon$restoredPositions.contains(pos.asLong());
	}
}
