package com.theseeklab.bloodmoon.client.config;

import com.theseeklab.bloodmoon.BloodmoonEvents;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import com.theseeklab.bloodmoon.config.BloodMoonConfig.HordeSpawn;
import com.theseeklab.bloodmoon.config.BloodMoonConfig.Scaled;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The in-game config screen (Mod Menu → Bloodmoon Events → Configure), built with Cloth Config. Only loaded when
 * Cloth Config is installed; see {@link BloodMoonModMenu}.
 * <p>
 * Saving writes config/bloodmoonevents.json and applies the values straight away, the same as /bloodmoon reload.
 * Option names and tooltips live in the language file under {@code config.bloodmoonevents.*}.
 */
public final class BloodMoonConfigScreen {
	private static final String PREFIX = "config." + BloodmoonEvents.MOD_ID + ".";

	private final ConfigEntryBuilder entries;
	private final BloodMoonConfig config = BloodMoonConfig.get();
	private final BloodMoonConfig defaults = new BloodMoonConfig();

	private BloodMoonConfigScreen(final ConfigEntryBuilder entries) {
		this.entries = entries;
	}

	public static Screen create(final Screen parent) {
		ConfigBuilder builder = ConfigBuilder.create()
			.setParentScreen(parent)
			.setTitle(Component.translatable(PREFIX + "title"))
			.setSavingRunnable(BloodMoonConfig::applyEdits);
		new BloodMoonConfigScreen(builder.entryBuilder()).build(builder);
		return builder.build();
	}

	private void build(final ConfigBuilder builder) {
		BloodMoonConfig c = config;
		BloodMoonConfig d = defaults;

		ConfigCategory schedule = category(builder, "schedule");
		schedule.addEntry(entries.startTextDescription(Component.translatable(PREFIX + "server_note").withStyle(ChatFormatting.GRAY)).build());
		schedule.addEntry(intField("intervalNights", c.intervalNights, d.intervalNights, 1, 1000, v -> c.intervalNights = v));
		schedule.addEntry(intField("firstNight", c.firstNight, d.firstNight, 1, 100000, v -> c.firstNight = v));
		schedule.addEntry(doubleField("randomChance", c.randomChance, d.randomChance, 0.0, 1.0, v -> c.randomChance = v));
		schedule.addEntry(intField("startTime", c.startTime, d.startTime, 0, 23999, v -> c.startTime = v));
		schedule.addEntry(intField("endTime", c.endTime, d.endTime, 0, 23999, v -> c.endTime = v));
		schedule.addEntry(intField("warningTime", c.warningTime, d.warningTime, 0, 23999, v -> c.warningTime = v));

		ConfigCategory atmosphere = category(builder, "atmosphere");
		atmosphere.addEntry(toggle("preventSleeping", c.preventSleeping, d.preventSleeping, v -> c.preventSleeping = v));
		atmosphere.addEntry(toggle("showTitle", c.showTitle, d.showTitle, v -> c.showTitle = v));
		atmosphere.addEntry(floatField("skyTintStrength", c.skyTintStrength, d.skyTintStrength, 0.0F, 1.0F, v -> c.skyTintStrength = v));
		atmosphere.addEntry(floatField("moonScale", c.moonScale, d.moonScale, 0.1F, 10.0F, v -> c.moonScale = v));

		ConfigCategory difficulty = category(builder, "difficulty");
		difficulty.addEntry(intField("maxLevel", c.maxLevel, d.maxLevel, 1, 1000, v -> c.maxLevel = v));
		difficulty.addEntry(scaled("maxHordePerPlayer", c.maxHordePerPlayer, d.maxHordePerPlayer, 0.0, 500.0));
		difficulty.addEntry(scaled("mobsPerWave", c.mobsPerWave, d.mobsPerWave, 0.0, 100.0));
		difficulty.addEntry(scaled("waveIntervalTicks", c.waveIntervalTicks, d.waveIntervalTicks, 1.0, 24000.0));
		difficulty.addEntry(scaled("breakTicksPerHardness", c.breakTicksPerHardness, d.breakTicksPerHardness, 0.0, 1000.0));
		difficulty.addEntry(scaled("armorChance", c.armorChance, d.armorChance, 0.0, 1.0));

		ConfigCategory unlocks = category(builder, "unlocks");
		unlocks.addEntry(intField("zombiesPlaceBlocksFromLevel", c.zombiesPlaceBlocksFromLevel, d.zombiesPlaceBlocksFromLevel, 1, 1000, v -> c.zombiesPlaceBlocksFromLevel = v));
		unlocks.addEntry(intField("zombiesBreakBlocksFromLevel", c.zombiesBreakBlocksFromLevel, d.zombiesBreakBlocksFromLevel, 1, 1000, v -> c.zombiesBreakBlocksFromLevel = v));
		unlocks.addEntry(intField("creepersBreachFromLevel", c.creepersBreachFromLevel, d.creepersBreachFromLevel, 1, 1000, v -> c.creepersBreachFromLevel = v));
		unlocks.addEntry(intField("slimeSuperJumpFromLevel", c.slimeSuperJumpFromLevel, d.slimeSuperJumpFromLevel, 1, 1000, v -> c.slimeSuperJumpFromLevel = v));
		unlocks.addEntry(intField("longRangeTrackingFromLevel", c.longRangeTrackingFromLevel, d.longRangeTrackingFromLevel, 1, 1000, v -> c.longRangeTrackingFromLevel = v));
		unlocks.addEntry(doubleField("followRange", c.followRange, d.followRange, 1.0, 256.0, v -> c.followRange = v));
		unlocks.addEntry(doubleField("longFollowRange", c.longFollowRange, d.longFollowRange, 1.0, 256.0, v -> c.longFollowRange = v));

		ConfigCategory horde = category(builder, "horde");
		horde.addEntry(entries.startStrList(name("hordeSpawns"), formatSpawns(c.hordeSpawns))
			.setDefaultValue(formatSpawns(d.hordeSpawns))
			.setTooltip(tooltip("hordeSpawns"))
			.setExpanded(true)
			.setSaveConsumer(lines -> c.hordeSpawns = parseSpawns(lines))
			.build());
		horde.addEntry(intField("minSpawnDistance", c.minSpawnDistance, d.minSpawnDistance, 8, 128, v -> c.minSpawnDistance = v));
		horde.addEntry(intField("maxSpawnDistance", c.maxSpawnDistance, d.maxSpawnDistance, 12, 128, v -> c.maxSpawnDistance = v));
		horde.addEntry(toggle("spawnUnderground", c.spawnUnderground, d.spawnUnderground, v -> c.spawnUnderground = v));
		horde.addEntry(toggle("despawnAtDawn", c.despawnAtDawn, d.despawnAtDawn, v -> c.despawnAtDawn = v));
		horde.addEntry(doubleField("speedBonus", c.speedBonus, d.speedBonus, -0.9, 5.0, v -> c.speedBonus = v));

		ConfigCategory ai = category(builder, "ai");
		ai.addEntry(toggle("respectMobGriefing", c.respectMobGriefing, d.respectMobGriefing, v -> c.respectMobGriefing = v));
		ai.addEntry(toggle("onlyHordeMobsUpgraded", c.onlyHordeMobsUpgraded, d.onlyHordeMobsUpgraded, v -> c.onlyHordeMobsUpgraded = v));
		ai.addEntry(stringList("builderMobs", c.builderMobs, d.builderMobs, v -> c.builderMobs = v));
		ai.addEntry(toggle("zombiesPlaceBlocks", c.zombiesPlaceBlocks, d.zombiesPlaceBlocks, v -> c.zombiesPlaceBlocks = v));
		ai.addEntry(toggle("zombiesBreakBlocks", c.zombiesBreakBlocks, d.zombiesBreakBlocks, v -> c.zombiesBreakBlocks = v));
		ai.addEntry(floatField("maxBreakableHardness", c.maxBreakableHardness, d.maxBreakableHardness, 0.0F, 1000.0F, v -> c.maxBreakableHardness = v));
		ai.addEntry(intField("minBreakTicks", c.minBreakTicks, d.minBreakTicks, 1, 1000, v -> c.minBreakTicks = v));
		ai.addEntry(toggle("protectContainers", c.protectContainers, d.protectContainers, v -> c.protectContainers = v));
		ai.addEntry(stringList("unbreakableBlocks", c.unbreakableBlocks, d.unbreakableBlocks, v -> c.unbreakableBlocks = v));
		ai.addEntry(toggle("creepersBreach", c.creepersBreach, d.creepersBreach, v -> c.creepersBreach = v));
		ai.addEntry(doubleField("creeperBreachDistance", c.creeperBreachDistance, d.creeperBreachDistance, 1.0, 64.0, v -> c.creeperBreachDistance = v));
		ai.addEntry(toggle("slimesJumpHigher", c.slimesJumpHigher, d.slimesJumpHigher, v -> c.slimesJumpHigher = v));
		ai.addEntry(floatField("slimeJumpMultiplier", c.slimeJumpMultiplier, d.slimeJumpMultiplier, 1.0F, 10.0F, v -> c.slimeJumpMultiplier = v));

		ConfigCategory damage = category(builder, "damage");
		damage.addEntry(toggle("restoreBrokenBlocks", c.restoreBrokenBlocks, d.restoreBrokenBlocks, v -> c.restoreBrokenBlocks = v));
		damage.addEntry(toggle("restoreOnlyPreexistingBlocks", c.restoreOnlyPreexistingBlocks, d.restoreOnlyPreexistingBlocks, v -> c.restoreOnlyPreexistingBlocks = v));
		damage.addEntry(toggle("dropBrokenBlocks", c.dropBrokenBlocks, d.dropBrokenBlocks, v -> c.dropBrokenBlocks = v));

		ConfigCategory compat = category(builder, "compat");
		compat.addEntry(entries.startTextDescription(Component.translatable(PREFIX + "compat_note").withStyle(ChatFormatting.GRAY)).build());
		compat.addEntry(scaled("championChanceMultiplier", c.championChanceMultiplier, d.championChanceMultiplier, 0.0, 100.0));
		compat.addEntry(scaled("championTierGap", c.championTierGap, d.championTierGap, 0.0, 0.95));
	}

	// --- Entry helpers ---

	private ConfigCategory category(final ConfigBuilder builder, final String key) {
		return builder.getOrCreateCategory(Component.translatable(PREFIX + "category." + key));
	}

	private static Component name(final String option) {
		return Component.translatable(PREFIX + option);
	}

	private static Component tooltip(final String option) {
		return Component.translatable(PREFIX + option + ".tooltip");
	}

	private AbstractConfigListEntry<?> toggle(final String option, final boolean value, final boolean def, final Consumer<Boolean> save) {
		return entries.startBooleanToggle(name(option), value).setDefaultValue(def).setTooltip(tooltip(option)).setSaveConsumer(save).build();
	}

	private AbstractConfigListEntry<?> intField(final String option, final int value, final int def, final int min, final int max, final Consumer<Integer> save) {
		return entries.startIntField(name(option), value).setDefaultValue(def).setMin(min).setMax(max).setTooltip(tooltip(option)).setSaveConsumer(save).build();
	}

	private AbstractConfigListEntry<?> doubleField(final String option, final double value, final double def, final double min, final double max, final Consumer<Double> save) {
		return entries.startDoubleField(name(option), value).setDefaultValue(def).setMin(min).setMax(max).setTooltip(tooltip(option)).setSaveConsumer(save).build();
	}

	private AbstractConfigListEntry<?> floatField(final String option, final float value, final float def, final float min, final float max, final Consumer<Float> save) {
		return entries.startFloatField(name(option), value).setDefaultValue(def).setMin(min).setMax(max).setTooltip(tooltip(option)).setSaveConsumer(save).build();
	}

	private AbstractConfigListEntry<?> stringList(final String option, final List<String> value, final List<String> def, final Consumer<List<String>> save) {
		return entries.startStrList(name(option), new ArrayList<>(value))
			.setDefaultValue(new ArrayList<>(def))
			.setTooltip(tooltip(option))
			.setSaveConsumer(list -> save.accept(new ArrayList<>(list)))
			.build();
	}

	/** A start/end pair, shown as a collapsible group with the value at Blood Moon #1 and at the max level. */
	private AbstractConfigListEntry<?> scaled(final String option, final Scaled value, final Scaled def, final double min, final double max) {
		List<AbstractConfigListEntry> pair = new ArrayList<>();
		pair.add(entries.startDoubleField(Component.translatable(PREFIX + "scaled.start"), value.start)
			.setDefaultValue(def.start).setMin(min).setMax(max).setTooltip(tooltip(option)).setSaveConsumer(v -> value.start = v).build());
		pair.add(entries.startDoubleField(Component.translatable(PREFIX + "scaled.end"), value.end)
			.setDefaultValue(def.end).setMin(min).setMax(max).setTooltip(tooltip(option)).setSaveConsumer(v -> value.end = v).build());
		return entries.startSubCategory(name(option), pair).setTooltip(tooltip(option)).build();
	}

	// --- Horde spawn list as text lines: "minecraft:zombie 40 1" ---

	private static List<String> formatSpawns(final List<HordeSpawn> spawns) {
		List<String> lines = new ArrayList<>();
		for (HordeSpawn spawn : spawns) {
			lines.add(spawn.id + " " + spawn.weight + " " + spawn.fromLevel);
		}
		return lines;
	}

	/** Lines that don't parse are dropped with a log warning; an empty result falls back to the defaults on save. */
	private static List<HordeSpawn> parseSpawns(final List<String> lines) {
		List<HordeSpawn> spawns = new ArrayList<>();
		for (String line : lines) {
			String[] parts = line.trim().split("[\\s,]+");
			try {
				if (parts.length < 2 || parts[0].isEmpty()) {
					throw new IllegalArgumentException();
				}
				int weight = Integer.parseInt(parts[1]);
				int fromLevel = parts.length >= 3 ? Integer.parseInt(parts[2]) : 1;
				String id = parts[0].contains(":") ? parts[0] : "minecraft:" + parts[0];
				spawns.add(new HordeSpawn(id.toLowerCase(Locale.ROOT), Math.max(0, weight), Math.max(1, fromLevel)));
			} catch (IllegalArgumentException e) {
				BloodmoonEvents.LOGGER.warn("Ignoring horde spawn entry '{}': expected '<mob id> <weight> <from level>'", line);
			}
		}
		return spawns;
	}
}
