package com.theseeklab.bloodmoon.ai;

import com.theseeklab.bloodmoon.BloodMoonManager;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * Horde mobs always know where the nearest survival player is, through walls and without line of sight,
 * so they converge on bases instead of wandering around.
 */
public class HordeTargetGoal extends Goal {
	private final Mob mob;
	private int recheckTicks;

	public HordeTargetGoal(final Mob mob) {
		this.mob = mob;
		this.setFlags(EnumSet.of(Goal.Flag.TARGET));
	}

	@Override
	public boolean canUse() {
		return BloodMoonManager.isActive(mob.level()) && findTarget() != null;
	}

	@Override
	public boolean canContinueToUse() {
		return BloodMoonManager.isActive(mob.level()) && mob.getTarget() instanceof ServerPlayer player && isValid(player);
	}

	@Override
	public void start() {
		mob.setTarget(findTarget());
		recheckTicks = 20;
	}

	@Override
	public void tick() {
		if (--recheckTicks <= 0) {
			recheckTicks = 20;
			ServerPlayer closest = findTarget();
			if (closest != null && closest != mob.getTarget()) {
				mob.setTarget(closest);
			}
		}
	}

	private @Nullable ServerPlayer findTarget() {
		if (!(mob.level() instanceof ServerLevel level)) {
			return null;
		}
		ServerPlayer best = null;
		double bestDist = Double.MAX_VALUE;
		for (ServerPlayer player : level.players()) {
			if (!isValid(player)) {
				continue;
			}
			double dist = mob.distanceToSqr(player);
			if (dist < bestDist) {
				bestDist = dist;
				best = player;
			}
		}
		return best;
	}

	private boolean isValid(final ServerPlayer player) {
		if (!player.isAlive() || player.isSpectator() || player.isCreative()) {
			return false;
		}
		double range = mob.getAttributeValue(Attributes.FOLLOW_RANGE) * 1.5;
		return mob.distanceToSqr(player) <= range * range;
	}
}
