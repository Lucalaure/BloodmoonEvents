package com.theseeklab.bloodmoon.ai;

import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

/**
 * A creeper that can get near its target but can't actually reach it (walls, moats, towers) lights its
 * fuse anyway, blowing a hole for the rest of the horde.
 */
public class CreeperBreachGoal extends Goal {
	private final Creeper creeper;
	private final StuckTracker stuckTracker = new StuckTracker();
	private int stuckTicks;

	public CreeperBreachGoal(final Creeper creeper) {
		this.creeper = creeper;
	}

	@Override
	public boolean canUse() {
		LivingEntity target = creeper.getTarget();
		return BloodMoonManager.unlocked(BloodMoonConfig.get().creepersBreachFromLevel)
			&& target != null
			&& target.isAlive()
			&& !creeper.isIgnited()
			&& creeper.level() instanceof ServerLevel level
			&& HordeMobs.isUpgradedNow(creeper)
			&& BloodMoonManager.mobGriefingAllowed(level);
	}

	@Override
	public boolean canContinueToUse() {
		return canUse();
	}

	@Override
	public boolean requiresUpdateEveryTick() {
		return true;
	}

	@Override
	public void stop() {
		stuckTicks = 0;
		stuckTracker.reset();
	}

	@Override
	public void tick() {
		LivingEntity target = creeper.getTarget();
		if (target == null) {
			return;
		}
		double range = BloodMoonConfig.get().creeperBreachDistance;
		boolean stuck = stuckTracker.update(creeper, target);
		if (stuck && creeper.distanceToSqr(target) <= range * range) {
			// A short grace period so creepers don't pop the instant a path flickers.
			if (++stuckTicks > 30) {
				creeper.ignite();
			}
		} else {
			stuckTicks = Math.max(0, stuckTicks - 1);
		}
	}
}
