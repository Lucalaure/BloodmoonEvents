package com.theseeklab.bloodmoon.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.theseeklab.bloodmoon.BloodmoonEvents;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Plain JSON config stored at config/bloodmoonevents.json. Missing fields fall back to the defaults below,
 * and the file is rewritten on load so newly added options show up for the user.
 */
public class BloodMoonConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve(BloodmoonEvents.MOD_ID + ".json");

	private static BloodMoonConfig instance = new BloodMoonConfig();

	// --- Scheduling ---
	/** A Blood Moon happens on every Nth night (night 7, 14, 21... with the default of 7). */
	public int intervalNights = 7;
	/** The first night that is allowed to be a Blood Moon. */
	public int firstNight = 7;
	/** Extra chance (0-1) for any other night to randomly become a Blood Moon. */
	public double randomChance = 0.0;
	/** Time of day (0-23999) the Blood Moon starts. 12600 is just after sunset. */
	public int startTime = 12600;
	/** Time of day the Blood Moon ends. 23200 is just before sunrise. */
	public int endTime = 23200;
	/** Time of day players are warned that tonight is a Blood Moon. */
	public int warningTime = 11500;

	// --- Atmosphere ---
	public boolean preventSleeping = true;
	public boolean showTitle = true;
	/** How strongly the sky, fog and light are tinted red (0-1). */
	public float skyTintStrength = 0.75F;
	/** How much bigger the moon is during a Blood Moon (1 = vanilla size). */
	public float moonScale = 1.6F;

	// --- Difficulty scaling ---
	/**
	 * Each Blood Moon is one level harder than the last (the 1st is level 1, the 2nd level 2...), whether or not
	 * anyone died. Scaled values grow evenly from their "start" (level 1) to their "end" (this level) and stay there.
	 */
	public int maxLevel = 10;
	/** Maximum horde mobs alive near each player. */
	public Scaled maxHordePerPlayer = new Scaled(12, 50);
	public Scaled mobsPerWave = new Scaled(2, 6);
	/** Ticks between spawn waves around each player (20 ticks = 1 second). */
	public Scaled waveIntervalTicks = new Scaled(160, 80);
	/** Break time in ticks per point of block hardness (planks are 2), per zombie. Several zombies on one block stack. */
	public Scaled breakTicksPerHardness = new Scaled(60, 20);
	/** Chance (0-1) a zombie or skeleton spawns wearing extra armor. Higher levels also roll better materials. */
	public Scaled armorChance = new Scaled(0.0, 0.6);

	// --- RPG Advanced Difficulty compatibility (only used when that mod is installed) ---
	/** Champion odds multiplier for horde mobs. */
	public Scaled championChanceMultiplier = new Scaled(1.5, 4.0);
	/**
	 * Fraction (0-0.95) of each champion tier's odds gap to tier 1 that is closed, so higher tiers get proportionally
	 * more common (the rarest gain the most) but never as common as the tier below them.
	 */
	public Scaled championTierGap = new Scaled(0.0, 0.2);

	// --- Unlocks: the Blood Moon number each ability switches on ---
	public int zombiesPlaceBlocksFromLevel = 1;
	public int zombiesBreakBlocksFromLevel = 3;
	public int creepersBreachFromLevel = 3;
	public int slimeSuperJumpFromLevel = 4;
	public int longRangeTrackingFromLevel = 5;
	/** How far horde mobs track players (through walls) before and after long-range tracking unlocks. */
	public double followRange = 40.0;
	public double longFollowRange = 64.0;

	// --- Horde spawning ---
	/** Which mobs the horde is made of, how common each is, and the Blood Moon number it first appears on. */
	public List<HordeSpawn> hordeSpawns = defaultHordeSpawns();
	public int minSpawnDistance = 24;
	public int maxSpawnDistance = 44;
	/** Also spawn the horde in caves when the player is underground. */
	public boolean spawnUnderground = true;
	/** Horde mobs are removed at dawn when the Blood Moon ends. */
	public boolean despawnAtDawn = true;
	/** Flat movement speed bonus for every horde mob (0.1 = +10%). Does not scale with level. */
	public double speedBonus = 0.10;

	// --- Mob AI ---
	/** Respect the mob_griefing gamerule for block breaking, placing and creeper breaching. */
	public boolean respectMobGriefing = true;
	/** Mobs that can break and place blocks to reach their target. */
	public List<String> builderMobs = new ArrayList<>(List.of(
		"minecraft:zombie", "minecraft:husk", "minecraft:zombie_villager", "minecraft:drowned"
	));
	/** Only horde mobs get the upgraded AI. Set false to give it to every matching mob during a Blood Moon. */
	public boolean onlyHordeMobsUpgraded = true;
	/** Master switches; the unlock levels above still apply when these are on. */
	public boolean zombiesBreakBlocks = true;
	public boolean zombiesPlaceBlocks = true;
	/** Block used when pillaring and bridging. */
	public String buildBlock = "minecraft:cobblestone";
	/** Blocks harder than this (obsidian is 50) can never be broken. */
	public float maxBreakableHardness = 20.0F;
	public int minBreakTicks = 15;
	public boolean dropBrokenBlocks = true;
	/** Never break blocks that hold items (chests, barrels, furnaces...). */
	public boolean protectContainers = true;
	public List<String> unbreakableBlocks = new ArrayList<>(List.of(
		"minecraft:bedrock", "minecraft:obsidian", "minecraft:crying_obsidian", "minecraft:reinforced_deepslate",
		"minecraft:respawn_anchor", "minecraft:end_portal_frame", "minecraft:spawner", "minecraft:beacon"
	));
	/** Creepers that can't reach their target ignite when they are this close. */
	public boolean creepersBreach = true;
	public double creeperBreachDistance = 8.0;
	/** Slime and magma cube jump height multiplier. */
	public boolean slimesJumpHigher = true;
	public float slimeJumpMultiplier = 1.8F;

	/** A value that grows from {@code start} at level 1 to {@code end} at {@link #maxLevel}. */
	public static class Scaled {
		public double start;
		public double end;

		public Scaled(final double start, final double end) {
			this.start = start;
			this.end = end;
		}

		public double at(final int level) {
			int max = get().maxLevel;
			if (max <= 1) {
				return end;
			}
			double t = (Math.min(Math.max(level, 1), max) - 1) / (double) (max - 1);
			return start + (end - start) * t;
		}

		public int intAt(final int level) {
			return (int) Math.round(at(level));
		}
	}

	public static class HordeSpawn {
		public String id;
		public int weight;
		public int fromLevel;

		public HordeSpawn(final String id, final int weight, final int fromLevel) {
			this.id = id;
			this.weight = weight;
			this.fromLevel = fromLevel;
		}
	}

	private static List<HordeSpawn> defaultHordeSpawns() {
		return new ArrayList<>(List.of(
			new HordeSpawn("minecraft:zombie", 40, 1),
			new HordeSpawn("minecraft:skeleton", 15, 1),
			new HordeSpawn("minecraft:spider", 12, 1),
			new HordeSpawn("minecraft:creeper", 12, 3),
			new HordeSpawn("minecraft:husk", 5, 3),
			new HordeSpawn("minecraft:zombie_villager", 5, 3),
			new HordeSpawn("minecraft:slime", 6, 3),
			new HordeSpawn("minecraft:witch", 3, 5),
			new HordeSpawn("minecraft:cave_spider", 5, 5)
		));
	}

	public static BloodMoonConfig get() {
		return instance;
	}

	public static void load() {
		BloodMoonConfig loaded = null;
		if (Files.exists(PATH)) {
			try (Reader reader = Files.newBufferedReader(PATH)) {
				loaded = GSON.fromJson(reader, BloodMoonConfig.class);
			} catch (Exception e) {
				BloodmoonEvents.LOGGER.error("Failed to read {}, using defaults", PATH, e);
			}
		}

		instance = loaded != null ? loaded : new BloodMoonConfig();
		instance.validate();
		save();
	}

	public static void save() {
		try {
			Files.createDirectories(PATH.getParent());
			try (Writer writer = Files.newBufferedWriter(PATH)) {
				GSON.toJson(instance, writer);
			}
		} catch (IOException e) {
			BloodmoonEvents.LOGGER.error("Failed to write {}", PATH, e);
		}
	}

	private void validate() {
		intervalNights = Math.max(1, intervalNights);
		firstNight = Math.max(1, firstNight);
		startTime = Math.floorMod(startTime, 24000);
		endTime = Math.floorMod(endTime, 24000);
		if (endTime <= startTime) {
			BloodmoonEvents.LOGGER.warn("endTime must be after startTime within the same night, resetting both");
			startTime = 12600;
			endTime = 23200;
		}
		maxLevel = Math.max(1, maxLevel);
		if (maxHordePerPlayer == null) {
			maxHordePerPlayer = new Scaled(12, 50);
		}
		if (mobsPerWave == null) {
			mobsPerWave = new Scaled(2, 6);
		}
		if (waveIntervalTicks == null) {
			waveIntervalTicks = new Scaled(160, 80);
		}
		if (breakTicksPerHardness == null) {
			breakTicksPerHardness = new Scaled(60, 20);
		}
		if (armorChance == null) {
			armorChance = new Scaled(0.0, 0.6);
		}
		minSpawnDistance = Math.max(8, minSpawnDistance);
		maxSpawnDistance = Math.max(minSpawnDistance + 4, maxSpawnDistance);
		if (hordeSpawns == null || hordeSpawns.isEmpty()) {
			hordeSpawns = defaultHordeSpawns();
		}
		if (builderMobs == null) {
			builderMobs = new ArrayList<>();
		}
		if (unbreakableBlocks == null) {
			unbreakableBlocks = new ArrayList<>();
		}
	}
}
