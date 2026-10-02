package com.theseeklab.bloodmoon.ai;

import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Lets a mob dig and build its way to its target when walking there is impossible:
 * <ul>
 *   <li>breaks walls, doors and ceilings in its way (time scales with block hardness)</li>
 *   <li>pillars up (jump + place a block underneath) when the target is above it</li>
 *   <li>builds steps up to higher ground and bridges over gaps</li>
 *   <li>digs down when the target is directly below</li>
 * </ul>
 * Placing and breaking unlock at different Blood Moon levels (see the config), so early zombies can only build.
 * The goal claims no control flags, so it runs alongside the vanilla movement and attack goals and only
 * steps in when {@link StuckTracker} says the mob can't make progress. Modelled on ZombieBreakAndBuild.
 */
public class BreakAndBuildGoal extends Goal {
	private static final int PILLAR_TIMEOUT_TICKS = 20;

	private final Mob mob;
	private final StuckTracker stuckTracker = new StuckTracker();

	private @Nullable BlockPos breakingPos;
	private int breakingTicks;
	private @Nullable BlockPos pillarPos;
	private int pillarTicks;
	private int cooldown;
	private boolean useSecondaryAxis;

	public BreakAndBuildGoal(final Mob mob) {
		this.mob = mob;
	}

	@Override
	public boolean canUse() {
		LivingEntity target = mob.getTarget();
		return (canPlaceNow() || canBreakNow())
			&& target != null
			&& target.isAlive()
			&& mob.level() instanceof ServerLevel level
			&& HordeMobs.isUpgradedNow(mob)
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
		breakingPos = null;
		pillarPos = null;
		stuckTracker.reset();
	}

	@Override
	public void tick() {
		LivingEntity target = mob.getTarget();
		if (target == null || !(mob.level() instanceof ServerLevel level)) {
			return;
		}

		if (cooldown > 0) {
			cooldown--;
		}
		boolean stuck = stuckTracker.update(mob, target);

		if (pillarPos != null) {
			continuePillar(level);
			return;
		}
		if (breakingPos != null) {
			continueBreaking(level);
			return;
		}
		if (stuck && cooldown == 0) {
			if (!act(level, target)) {
				// Nothing useful along this axis; try the other one next time.
				useSecondaryAxis = !useSecondaryAxis;
				cooldown = 10;
			}
		}
	}

	/** Picks one action to get closer to the target. Returns false if nothing could be done. */
	private boolean act(final ServerLevel level, final LivingEntity target) {
		BloodMoonConfig config = BloodMoonConfig.get();
		BlockPos feet = mob.blockPosition();
		int height = Math.max(1, Mth.ceil(mob.getBbHeight()));
		double dx = target.getX() - mob.getX();
		double dz = target.getZ() - mob.getZ();
		double dy = target.getY() - mob.getY();
		double horizontal = Math.sqrt(dx * dx + dz * dz);

		// Target is above us and close: build a pillar.
		if (dy > 1.5 && horizontal < 4.0 && canPlaceNow()) {
			if (startPillar(level, feet, height)) {
				return true;
			}
		}

		// Target is right below us: dig down.
		if (dy < -1.5 && horizontal < 1.5 && tryStartBreaking(level, feet.below())) {
			return true;
		}

		Direction dir = horizontalDirection(dx, dz);
		BlockPos front = feet.relative(dir);

		if (dy >= 1.0) {
			// Going up: clear room to step onto the block in front, then build a step if there is none.
			for (int i = height; i >= 1; i--) {
				if (blocksMovement(level, front.above(i)) && tryStartBreaking(level, front.above(i))) {
					return true;
				}
			}
			if (blocksMovement(level, feet.above(height)) && tryStartBreaking(level, feet.above(height))) {
				return true;
			}
			if (!blocksMovement(level, front) && blocksMovement(level, front.below()) && tryPlace(level, front)) {
				return true;
			}
		} else {
			// Same level or going down: open a mob-sized hole, top block first.
			for (int i = height - 1; i >= 0; i--) {
				if (blocksMovement(level, front.above(i)) && tryStartBreaking(level, front.above(i))) {
					return true;
				}
			}
		}

		// A wall we can't (or aren't allowed to) break yet: build up and climb over it instead.
		boolean wallAhead = false;
		for (int i = 0; i <= height; i++) {
			wallAhead |= blocksMovement(level, front.above(i));
		}
		if (wallAhead && dy > -2.0 && canPlaceNow() && startPillar(level, feet, height)) {
			return true;
		}

		// Bridge over gaps deeper than one block, unless the target is down there.
		if (dy > -2.0 && !blocksMovement(level, front) && isOpen(level, front.below()) && isOpen(level, front.below(2))) {
			return tryPlace(level, front.below());
		}
		return false;
	}

	private Direction horizontalDirection(final double dx, final double dz) {
		boolean xMajor = Math.abs(dx) >= Math.abs(dz);
		if (useSecondaryAxis) {
			xMajor = !xMajor;
		}
		return xMajor
			? (dx >= 0 ? Direction.EAST : Direction.WEST)
			: (dz >= 0 ? Direction.SOUTH : Direction.NORTH);
	}

	// --- Pillaring ---

	private boolean startPillar(final ServerLevel level, final BlockPos feet, final int height) {
		if (!mob.onGround()) {
			return false;
		}
		BlockPos headroom = feet.above(height);
		if (blocksMovement(level, headroom)) {
			return tryStartBreaking(level, headroom);
		}
		if (!level.getBlockState(feet).canBeReplaced()) {
			return false;
		}

		mob.getNavigation().stop();
		Vec3 motion = mob.getDeltaMovement();
		mob.setDeltaMovement(0.0, motion.y, 0.0);
		mob.getJumpControl().jump();
		pillarPos = feet;
		pillarTicks = 0;
		return true;
	}

	private void continuePillar(final ServerLevel level) {
		BlockPos pos = pillarPos;
		if (pos == null || ++pillarTicks > PILLAR_TIMEOUT_TICKS) {
			pillarPos = null;
			return;
		}

		mob.getNavigation().stop();
		Vec3 motion = mob.getDeltaMovement();
		mob.setDeltaMovement(0.0, motion.y, 0.0);

		if (mob.getY() > pos.getY() + 1.0 && motion.y >= -0.1) {
			if (tryPlace(level, pos)) {
				// Re-centre on the new block so the next jump goes straight up.
				mob.getMoveControl().setWantedPosition(pos.getX() + 0.5, mob.getY(), pos.getZ() + 0.5, 0.0);
				cooldown = 8;
			}
			pillarPos = null;
		}
	}

	// --- Breaking ---

	private boolean tryStartBreaking(final ServerLevel level, final BlockPos pos) {
		if (!canBreakNow() || !canBreak(level, pos)) {
			return false;
		}
		breakingPos = pos.immutable();
		breakingTicks = 0;
		mob.getNavigation().stop();
		return true;
	}

	private void continueBreaking(final ServerLevel level) {
		BlockPos pos = breakingPos;
		if (pos == null) {
			return;
		}
		BlockState state = level.getBlockState(pos);
		Vec3 center = Vec3.atCenterOf(pos);
		if (state.isAir() || !canBreak(level, pos) || mob.distanceToSqr(center) > 4.5 * 4.5) {
			breakingPos = null;
			return;
		}

		BloodMoonConfig config = BloodMoonConfig.get();
		mob.getNavigation().stop();
		mob.getLookControl().setLookAt(center.x, center.y, center.z);

		if (breakingTicks++ % 8 == 0) {
			mob.swingForAttack(InteractionHand.MAIN_HAND);
			level.playSound(null, pos, state.getSoundType().getHitSound(), SoundSource.HOSTILE, 0.6F, 0.6F);
		}

		float ticksPerHardness = (float) config.breakTicksPerHardness.at(BloodMoonManager.activeLevel());
		float required = Math.max(config.minBreakTicks, state.getDestroySpeed(level, pos) * ticksPerHardness);
		float progress = BlockDamageTracker.addProgress(level, pos, 1.0F);
		BlockDamageTracker.showStage(level, pos, Mth.clamp((int) (progress / required * 10.0F), 0, 9));

		if (progress >= required) {
			BlockDamageTracker.clear(level, pos);
			level.destroyBlock(pos, config.dropBrokenBlocks, mob);
			breakingPos = null;
			cooldown = 4;
			stuckTracker.reset();
		}
	}

	private static boolean canBreak(final ServerLevel level, final BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (state.isAir() || !level.getWorldBorder().isWithinBounds(pos)) {
			return false;
		}
		BloodMoonConfig config = BloodMoonConfig.get();
		float hardness = state.getDestroySpeed(level, pos);
		if (hardness < 0.0F || hardness > config.maxBreakableHardness) {
			return false;
		}
		Identifier id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
		if (config.unbreakableBlocks.contains(id.toString())) {
			return false;
		}
		return !config.protectContainers || !(level.getBlockEntity(pos) instanceof Container);
	}

	// --- Placing ---

	private boolean tryPlace(final ServerLevel level, final BlockPos pos) {
		BloodMoonConfig config = BloodMoonConfig.get();
		if (!canPlaceNow() || !level.getWorldBorder().isWithinBounds(pos)) {
			return false;
		}
		if (!level.getBlockState(pos).canBeReplaced()) {
			return false;
		}
		AABB box = new AABB(pos);
		if (mob.getBoundingBox().intersects(box) || !level.getEntitiesOfClass(LivingEntity.class, box, e -> e != mob).isEmpty()) {
			return false;
		}

		BlockState state = buildBlock().defaultBlockState();
		level.setBlockAndUpdate(pos, state);
		level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.HOSTILE, 1.0F, 0.8F);
		mob.swingForAttack(InteractionHand.MAIN_HAND);
		cooldown = Math.max(cooldown, 10);
		stuckTracker.reset();
		return true;
	}

	private static Block buildBlock() {
		Identifier id = Identifier.tryParse(BloodMoonConfig.get().buildBlock);
		Block block = id == null ? null : BuiltInRegistries.BLOCK.getOptional(id).orElse(null);
		return block == null || block == Blocks.AIR ? Blocks.COBBLESTONE : block;
	}

	// --- Helpers ---

	/** Placing unlocks first (Blood Moon #1 by default)... */
	private static boolean canPlaceNow() {
		BloodMoonConfig config = BloodMoonConfig.get();
		return config.zombiesPlaceBlocks && BloodMoonManager.unlocked(config.zombiesPlaceBlocksFromLevel);
	}

	/** ...breaking comes later (Blood Moon #3 by default). */
	private static boolean canBreakNow() {
		BloodMoonConfig config = BloodMoonConfig.get();
		return config.zombiesBreakBlocks && BloodMoonManager.unlocked(config.zombiesBreakBlocksFromLevel);
	}

	private static boolean blocksMovement(final ServerLevel level, final BlockPos pos) {
		return !level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
	}

	/** Air or something you'd fall through, and not a fluid you could swim in. */
	private static boolean isOpen(final ServerLevel level, final BlockPos pos) {
		return !blocksMovement(level, pos) && level.getFluidState(pos).isEmpty();
	}
}
