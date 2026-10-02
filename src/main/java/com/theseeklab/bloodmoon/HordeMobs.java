package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.ai.BreakAndBuildGoal;
import com.theseeklab.bloodmoon.ai.CreeperBreachGoal;
import com.theseeklab.bloodmoon.ai.HordeTargetGoal;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import com.theseeklab.bloodmoon.mixin.MobAccessor;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Tagging, buffing and AI upgrades for horde mobs. */
public final class HordeMobs {
	/** Entity tag (as in /tag) marking a mob as part of the Blood Moon horde. Saved with the entity. */
	public static final String HORDE_TAG = BloodmoonEvents.MOD_ID + ".horde";

	private static final AttributeModifier.Operation MULTIPLY_BASE = AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
	private static final List<Mob> pendingRemoval = new ArrayList<>();

	private HordeMobs() {
	}

	public static boolean isHorde(final Entity entity) {
		return entity.entityTags().contains(HORDE_TAG);
	}

	/** Called for freshly spawned horde mobs before they are added to the level. */
	public static void prepare(final Mob mob, final int bloodMoonLevel) {
		BloodMoonConfig config = BloodMoonConfig.get();
		mob.addTag(HORDE_TAG);
		mob.setPersistenceRequired();

		double range = bloodMoonLevel >= config.longRangeTrackingFromLevel ? config.longFollowRange : config.followRange;
		AttributeInstance followRange = mob.getAttribute(Attributes.FOLLOW_RANGE);
		if (followRange != null && followRange.getBaseValue() < range) {
			followRange.setBaseValue(range);
		}

		AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null && config.speedBonus != 0.0) {
			speed.addOrReplacePermanentModifier(new AttributeModifier(BloodmoonEvents.id("horde_speed"), config.speedBonus, MULTIPLY_BASE));
		}

		if (mob instanceof Zombie || mob instanceof AbstractSkeleton) {
			equipArmor(mob, bloodMoonLevel);
		}
	}

	/**
	 * Rolls extra armor for humanoid horde mobs. The chance to be armored grows with the level, and so does the
	 * material: leather and chainmail early on, iron mixed in toward the max level. Pieces never drop, so a Blood
	 * Moon can't be farmed for free iron.
	 */
	private static void equipArmor(final Mob mob, final int bloodMoonLevel) {
		BloodMoonConfig config = BloodMoonConfig.get();
		RandomSource random = mob.getRandom();
		if (random.nextDouble() >= config.armorChance.at(bloodMoonLevel)) {
			return;
		}

		double progress = config.maxLevel <= 1 ? 1.0 : (Math.min(bloodMoonLevel, config.maxLevel) - 1) / (double) (config.maxLevel - 1);
		double roll = random.nextDouble();
		int material = roll < progress * 0.5 ? 2 : roll < 0.3 + progress * 0.5 ? 1 : 0;
		Item[][] sets = {
			{Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS},
			{Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS},
			{Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS}
		};
		EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

		for (int i = 0; i < slots.length; i++) {
			// The helmet always comes with it; other pieces are a coin flip each.
			if ((i == 0 || random.nextBoolean()) && mob.getItemBySlot(slots[i]).isEmpty()) {
				mob.setItemSlot(slots[i], new ItemStack(sets[material][i]));
				mob.setDropChance(slots[i], 0.0F);
			}
		}
	}

	/** Attaches the upgraded AI whenever a mob is loaded, so it survives chunk unloads and restarts. */
	public static void onEntityLoad(final Entity entity, final ServerLevel level) {
		if (!(entity instanceof Mob mob) || !(mob instanceof Enemy)) {
			return;
		}

		BloodMoonConfig config = BloodMoonConfig.get();
		boolean horde = isHorde(mob);
		if (horde && config.despawnAtDawn && !BloodMoonManager.isActive(level)) {
			// A leftover from a Blood Moon that ended while this chunk was unloaded.
			// Removing during the load callback is unsafe, so do it on the next tick.
			pendingRemoval.add(mob);
			return;
		}
		if (!horde && config.onlyHordeMobsUpgraded) {
			return;
		}

		GoalSelector goals = ((MobAccessor) mob).bloodmoon$getGoalSelector();
		GoalSelector targets = ((MobAccessor) mob).bloodmoon$getTargetSelector();

		if (horde && !hasGoal(targets, HordeTargetGoal.class)) {
			targets.addGoal(0, new HordeTargetGoal(mob));
		}

		String id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
		if (config.builderMobs.contains(id) && !hasGoal(goals, BreakAndBuildGoal.class)) {
			goals.addGoal(1, new BreakAndBuildGoal(mob));
		}
		if (mob instanceof Creeper creeper && config.creepersBreach && !hasGoal(goals, CreeperBreachGoal.class)) {
			goals.addGoal(1, new CreeperBreachGoal(creeper));
		}
	}

	/** Whether this mob should currently use its upgraded Blood Moon behaviour. */
	public static boolean isUpgradedNow(final Mob mob) {
		return BloodMoonManager.isActive(mob.level()) && (isHorde(mob) || !BloodMoonConfig.get().onlyHordeMobsUpgraded);
	}

	private static boolean hasGoal(final GoalSelector selector, final Class<? extends Goal> type) {
		return selector.getAvailableGoals().stream().anyMatch(wrapped -> type.isInstance(wrapped.getGoal()));
	}

	public static void tickPendingRemovals() {
		if (pendingRemoval.isEmpty()) {
			return;
		}
		List<Mob> mobs = new ArrayList<>(pendingRemoval);
		pendingRemoval.clear();
		for (Mob mob : mobs) {
			if (mob.isRemoved() || !(mob.level() instanceof ServerLevel level)) {
				continue;
			}
			if (BloodMoonManager.isActive(level)) {
				// Loaded while the world was starting up, before the Blood Moon state was evaluated.
				onEntityLoad(mob, level);
			} else {
				mob.discard();
			}
		}
	}

	/** Removes every loaded horde mob with a puff of smoke. */
	public static void despawnAll(final ServerLevel level) {
		List<Entity> horde = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (isHorde(entity)) {
				horde.add(entity);
			}
		}
		for (Entity entity : horde) {
			level.sendParticles(ParticleTypes.LARGE_SMOKE, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 8, 0.3, 0.4, 0.3, 0.02);
			entity.discard();
		}
	}
}
