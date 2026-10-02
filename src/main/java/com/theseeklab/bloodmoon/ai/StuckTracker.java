package com.theseeklab.bloodmoon.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

/**
 * Decides whether a mob is unable to reach its target by walking: no path, a path that ends short of the
 * target, or no real movement for a while. Inspired by ZombieBreakAndBuild's stuck heuristics.
 */
public class StuckTracker {
	private static final int NO_PATH_TICKS = 10;
	private static final int STILL_TICKS = 40;
	private static final double MELEE_RANGE_SQR = 2.5 * 2.5;

	private Vec3 anchor = Vec3.ZERO;
	private int stillTicks;
	private int noPathTicks;

	public void reset() {
		stillTicks = 0;
		noPathTicks = 0;
	}

	public boolean update(final Mob mob, final LivingEntity target) {
		if (mob.distanceToSqr(target) <= MELEE_RANGE_SQR) {
			// Close enough to hit; whatever is happening, it's not "stuck".
			reset();
			anchor = mob.position();
			return false;
		}

		PathNavigation navigation = mob.getNavigation();
		Path path = navigation.getPath();

		if (path == null || path.isDone()) {
			noPathTicks++;
		} else {
			noPathTicks = 0;
		}

		if (mob.position().distanceToSqr(anchor) > 1.0) {
			anchor = mob.position();
			stillTicks = 0;
		} else {
			stillTicks++;
		}

		boolean partialPathEndsHere = false;
		if (path != null && !path.canReach()) {
			Node end = path.getEndNode();
			partialPathEndsHere = end == null || mob.distanceToSqr(end.x + 0.5, end.y, end.z + 0.5) < 2.5 * 2.5;
		}

		return navigation.isStuck() || noPathTicks > NO_PATH_TICKS || stillTicks > STILL_TICKS || partialPathEndsHere;
	}
}
