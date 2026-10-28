package com.theseeklab.bloodmoon.ai;

import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/**
 * Blood Moon slimes and magma cubes jump higher, further and more often, so walls stop being enough. Shared by
 * the slime and magma cube mixins, since in 1.21.1 each has its own jump code.
 */
public final class SlimeSuperJump {
	private SlimeSuperJump() {
	}

	public static boolean isEmpowered(final Mob mob) {
		BloodMoonConfig config = BloodMoonConfig.get();
		return config.slimesJumpHigher
			&& !mob.level().isClientSide()
			&& BloodMoonManager.unlocked(config.slimeSuperJumpFromLevel)
			&& HordeMobs.isUpgradedNow(mob);
	}

	/** Called after a vanilla jump: boosts it and lunges toward the target. */
	public static void boost(final Mob mob) {
		if (!isEmpowered(mob)) {
			return;
		}
		Vec3 motion = mob.getDeltaMovement();
		double vx = motion.x;
		double vz = motion.z;
		LivingEntity target = mob.getTarget();
		if (target != null) {
			// Lunge toward the target so the extra height actually clears walls.
			Vec3 toTarget = new Vec3(target.getX() - mob.getX(), 0.0, target.getZ() - mob.getZ());
			if (toTarget.lengthSqr() > 1.0E-4) {
				toTarget = toTarget.normalize().scale(0.35);
				vx += toTarget.x;
				vz += toTarget.z;
			}
		}
		mob.setDeltaMovement(vx, motion.y * BloodMoonConfig.get().slimeJumpMultiplier, vz);
	}
}
