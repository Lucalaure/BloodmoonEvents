package com.theseeklab.bloodmoon.mixin;

import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood Moon slimes (and magma cubes) jump higher, further and more often, so walls stop being enough. */
@Mixin(AbstractCubeMob.class)
public abstract class AbstractCubeMobMixin extends Mob {
	protected AbstractCubeMobMixin(final EntityType<? extends Mob> type, final Level level) {
		super(type, level);
	}

	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	private void bloodmoon$superJump(final CallbackInfo ci) {
		if (!bloodmoon$isEmpowered()) {
			return;
		}
		Vec3 motion = this.getDeltaMovement();
		double vx = motion.x;
		double vz = motion.z;
		LivingEntity target = this.getTarget();
		if (target != null) {
			// Lunge toward the target so the extra height actually clears walls.
			Vec3 toTarget = new Vec3(target.getX() - this.getX(), 0.0, target.getZ() - this.getZ());
			if (toTarget.lengthSqr() > 1.0E-4) {
				toTarget = toTarget.normalize().scale(0.35);
				vx += toTarget.x;
				vz += toTarget.z;
			}
		}
		this.setDeltaMovement(vx, motion.y * BloodMoonConfig.get().slimeJumpMultiplier, vz);
	}

	@Inject(method = "getJumpDelay", at = @At("RETURN"), cancellable = true)
	private void bloodmoon$fasterJumps(final CallbackInfoReturnable<Integer> cir) {
		if (bloodmoon$isEmpowered()) {
			cir.setReturnValue(Math.max(5, cir.getReturnValue() / 2));
		}
	}

	private boolean bloodmoon$isEmpowered() {
		BloodMoonConfig config = BloodMoonConfig.get();
		return config.slimesJumpHigher
			&& !this.level().isClientSide()
			&& BloodMoonManager.unlocked(config.slimeSuperJumpFromLevel)
			&& HordeMobs.isUpgradedNow(this);
	}
}
