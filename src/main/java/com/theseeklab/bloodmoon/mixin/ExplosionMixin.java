package com.theseeklab.bloodmoon.mixin;

import com.llamalad7.mixinextras.injector.WrapWithCondition;
import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.restore.BrokenBlockRestorer;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks blown up by a Blood Moon creeper are remembered so they come back after dawn (see
 * {@link BrokenBlockRestorer}), and drop nothing so the explosion can't be farmed. Container contents still spill.
 */
@Mixin(Explosion.class)
public abstract class ExplosionMixin {
	@Shadow
	@Final
	private Level level;

	@Unique
	private @Nullable LongSet bloodmoon$restoredPositions;

	@Shadow
	public abstract boolean interactsWithBlocks();

	@Inject(method = "finalizeExplosion", at = @At("HEAD"))
	private void bloodmoon$recordBlocks(final boolean spawnParticles, final CallbackInfo ci) {
		// finalizeExplosion also runs on the client and for explosions that don't break blocks (mob_griefing off).
		if (!(level instanceof ServerLevel serverLevel) || !interactsWithBlocks()) {
			return;
		}
		Explosion explosion = (Explosion) (Object) this;
		Entity source = explosion.getDirectSourceEntity();
		if (source instanceof Mob mob && BloodMoonManager.isActive(serverLevel) && HordeMobs.isUpgradedNow(mob)) {
			bloodmoon$restoredPositions = BrokenBlockRestorer.recordAll(serverLevel, explosion.getToBlow());
		}
	}

	@WrapWithCondition(
		method = "finalizeExplosion",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"
		)
	)
	private boolean bloodmoon$skipRestoredDrops(final Level level, final BlockPos pos, final ItemStack stack) {
		return bloodmoon$restoredPositions == null || !bloodmoon$restoredPositions.contains(pos.asLong());
	}
}
