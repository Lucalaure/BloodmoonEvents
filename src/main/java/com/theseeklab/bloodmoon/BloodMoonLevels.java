package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityType;

/** Describes what becomes new at each Blood Moon level, for the start-of-night announcement. */
public final class BloodMoonLevels {
	private BloodMoonLevels() {
	}

	/** Messages for everything that unlocks exactly at {@code level}, most notable first. */
	public static List<Component> unlocksAt(final int level) {
		BloodMoonConfig config = BloodMoonConfig.get();
		List<Component> unlocks = new ArrayList<>();

		if (config.zombiesBreakBlocks && config.zombiesBreakBlocksFromLevel == level) {
			unlocks.add(Component.translatableWithFallback("bloodmoonevents.unlock.zombie_break", "Zombies can now break through walls"));
		}
		if (config.creepersBreach && config.creepersBreachFromLevel == level) {
			unlocks.add(Component.translatableWithFallback("bloodmoonevents.unlock.creeper_breach", "Creepers blast through walls to reach you"));
		}

		MutableComponent newMobs = null;
		for (BloodMoonConfig.HordeSpawn spawn : config.hordeSpawns) {
			if (spawn.fromLevel != level || level == 1 || spawn.weight <= 0) {
				continue;
			}
			Identifier id = Identifier.tryParse(spawn.id);
			EntityType<?> type = id == null ? null : BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			if (type == null) {
				continue;
			}
			if (newMobs == null) {
				newMobs = Component.translatableWithFallback("bloodmoonevents.unlock.mobs", "New in the horde: ").append(type.getDescription());
			} else {
				newMobs.append(", ").append(type.getDescription());
			}
		}
		if (newMobs != null) {
			unlocks.add(newMobs);
		}

		if (config.zombiesPlaceBlocks && config.zombiesPlaceBlocksFromLevel == level) {
			unlocks.add(Component.translatableWithFallback("bloodmoonevents.unlock.zombie_build", "Zombies build their way up to you"));
		}
		if (config.slimesJumpHigher && config.slimeSuperJumpFromLevel == level) {
			unlocks.add(Component.translatableWithFallback("bloodmoonevents.unlock.slime_jump", "Slimes leap over walls"));
		}
		if (config.longRangeTrackingFromLevel == level) {
			unlocks.add(Component.translatableWithFallback("bloodmoonevents.unlock.long_range", "The horde can sense you from further away"));
		}
		return unlocks;
	}
}
