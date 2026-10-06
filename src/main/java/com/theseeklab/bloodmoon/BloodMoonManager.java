package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.ai.BlockDamageTracker;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import com.theseeklab.bloodmoon.network.BloodMoonSyncPayload;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * Server-side brain of the mod: decides when a Blood Moon is active, announces it, drives horde spawning
 * and keeps clients in sync. Blood Moons only happen in the overworld, which owns the day/night clock.
 */
public final class BloodMoonManager {
	private static final int TICKS_PER_DAY = SharedConstants.TICKS_PER_GAME_DAY;

	private static boolean active;
	private static long activeDay = -1L;
	/** Difficulty level of the running Blood Moon (its number: 1st, 2nd...). 0 when none is running. */
	private static int activeLevel;
	private static boolean endedByCommand;
	private static long lastWarnedDay = -1L;
	private static int tickCounter;
	private static final Map<UUID, Boolean> syncedPlayers = new HashMap<>();

	private BloodMoonManager() {
	}

	public static void reset() {
		active = false;
		activeDay = -1L;
		activeLevel = 0;
		endedByCommand = false;
		lastWarnedDay = -1L;
		syncedPlayers.clear();
	}

	public static void forgetPlayer(final ServerPlayer player) {
		syncedPlayers.remove(player.getUUID());
	}

	/** True while a Blood Moon is running in this level. Only the overworld ever has one. */
	public static boolean isActive(final Level level) {
		return active && level.dimension() == Level.OVERWORLD;
	}

	public static boolean isActive() {
		return active;
	}

	/** Number of the running Blood Moon, which is also its difficulty level. 0 when none is running. */
	public static int activeLevel() {
		return active ? activeLevel : 0;
	}

	/** Whether an ability that unlocks on Blood Moon {@code fromLevel} is available right now. */
	public static boolean unlocked(final int fromLevel) {
		return active && activeLevel >= fromLevel;
	}

	/** Sets the number of the next (or currently running) Blood Moon. */
	public static void setLevel(final MinecraftServer server, final int level) {
		state(server).setNextLevel(level);
		if (active) {
			activeLevel = level;
		}
	}

	public static BloodMoonState state(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(BloodMoonState.TYPE);
	}

	public static long currentDay(final ServerLevel overworld) {
		return overworld.getOverworldClockTime() / TICKS_PER_DAY;
	}

	public static long timeOfDay(final ServerLevel overworld) {
		return overworld.getOverworldClockTime() % TICKS_PER_DAY;
	}

	/** Whether the night belonging to {@code day} (day 0 is the first day of the world) is a Blood Moon. */
	public static boolean isBloodMoonNight(final MinecraftServer server, final long day) {
		BloodMoonState state = state(server);
		if (state.forcedDay() == day) {
			return true;
		}
		if (state.skippedDay() == day) {
			return false;
		}
		return isScheduled(server, day);
	}

	private static boolean isScheduled(final MinecraftServer server, final long day) {
		BloodMoonConfig config = BloodMoonConfig.get();
		long night = day + 1;
		if (night < config.firstNight) {
			return false;
		}
		if (night % config.intervalNights == 0) {
			return true;
		}
		if (config.randomChance > 0.0) {
			// Seeded per day so the result is stable across restarts and repeated checks.
			Random random = new Random(server.overworld().getSeed() * 31L + day * 0x9E3779B97F4A7C15L);
			return random.nextDouble() < config.randomChance;
		}
		return false;
	}

	/** Nights until the next Blood Moon: 0 means tonight (or right now). Returns -1 if none in the next 1000 days. */
	public static int nightsUntilNext(final MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long day = currentDay(overworld);
		long tod = timeOfDay(overworld);
		long start = tod >= BloodMoonConfig.get().endTime ? day + 1 : day;
		for (long d = start; d < start + 1000; d++) {
			if (isBloodMoonNight(server, d)) {
				return (int) (d - day);
			}
		}
		return -1;
	}

	public static void tick(final MinecraftServer server) {
		BloodMoonConfig config = BloodMoonConfig.get();
		ServerLevel overworld = server.overworld();
		long day = currentDay(overworld);
		long tod = timeOfDay(overworld);
		boolean tonight = isBloodMoonNight(server, day);
		boolean shouldBeActive = tonight && tod >= config.startTime && tod < config.endTime;

		if (shouldBeActive && !active) {
			start(server, day);
		} else if (!shouldBeActive && active) {
			end(server);
		}

		if (tonight && !active && lastWarnedDay != day && tod >= config.warningTime && tod < config.startTime) {
			lastWarnedDay = day;
			broadcast(server, Component.translatableWithFallback(
				"bloodmoonevents.warning", "The moon is turning red... the Blood Moon rises tonight."
			).withStyle(ChatFormatting.RED, ChatFormatting.ITALIC), false);
		}

		if (active) {
			HordeSpawner.tick(overworld, activeLevel);
		}

		if (++tickCounter % 10 == 0) {
			syncPlayers(server);
		}

		HordeMobs.tickPendingRetirements();
		BlockDamageTracker.tick(server);
	}

	private static void start(final MinecraftServer server, final long day) {
		active = true;
		activeDay = day;
		activeLevel = state(server).nextLevel();
		endedByCommand = false;
		BloodmoonEvents.LOGGER.info("Blood Moon #{} started on day {}", activeLevel, day);
		syncPlayers(server);

		List<Component> unlocks = BloodMoonLevels.unlocksAt(activeLevel);
		Component subtitle;
		if (activeLevel == 1) {
			subtitle = Component.translatableWithFallback("bloodmoonevents.subtitle", "The horde is coming");
		} else if (unlocks.isEmpty()) {
			subtitle = Component.translatableWithFallback("bloodmoonevents.subtitle.stronger", "The horde grows larger");
		} else {
			subtitle = unlocks.getFirst();
		}

		for (ServerPlayer player : server.overworld().players()) {
			if (BloodMoonConfig.get().showTitle) {
				player.connection.send(new ClientboundSetTitlesAnimationPacket(20, 70, 30));
				player.connection.send(new ClientboundSetTitleTextPacket(
					Component.translatableWithFallback("bloodmoonevents.title.numbered", "Blood Moon #%s", activeLevel)
						.withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD)
				));
				player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle.copy().withStyle(ChatFormatting.RED)));
			}
			player.connection.send(new ClientboundSoundPacket(
				SoundEvents.RAID_HORN, SoundSource.HOSTILE, player.getX(), player.getY(), player.getZ(), 64.0F, 0.6F, player.getRandom().nextLong()
			));
		}

		broadcast(server, Component.translatableWithFallback(
			"bloodmoonevents.started.numbered", "Blood Moon #%s has risen! Survive until dawn.", activeLevel
		).withStyle(ChatFormatting.DARK_RED), false);
		for (Component unlock : unlocks) {
			broadcast(server, Component.literal(" - ").append(unlock).withStyle(ChatFormatting.RED), false);
		}
	}

	private static void end(final MinecraftServer server) {
		active = false;
		BloodMoonState state = state(server);
		if (!endedByCommand && activeDay >= 0) {
			state.markCompleted(activeDay);
		}
		BloodmoonEvents.LOGGER.info("Blood Moon #{} ended (completed total: {})", activeLevel, state.completed());
		syncPlayers(server);

		HordeMobs.retireAll(server.overworld());

		broadcast(server, endedByCommand
			? Component.translatableWithFallback("bloodmoonevents.stopped", "The Blood Moon fades away.").withStyle(ChatFormatting.GRAY)
			: Component.translatableWithFallback("bloodmoonevents.ended", "The Blood Moon has set. You survived the night.").withStyle(ChatFormatting.GOLD),
			false);
		activeDay = -1L;
		activeLevel = 0;
	}

	/** Forces a Blood Moon right now, moving the clock to nightfall if it is currently day. */
	public static void forceStart(final MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		BloodMoonConfig config = BloodMoonConfig.get();
		long day = currentDay(overworld);
		long tod = timeOfDay(overworld);
		if (tod >= config.endTime) {
			// Too late for tonight; jump to tomorrow's nightfall.
			day++;
		}
		state(server).force(day);
		if (tod < config.startTime || tod >= config.endTime) {
			setClock(server, day * TICKS_PER_DAY + config.startTime);
		}
	}

	/** Ends the current Blood Moon (or cancels tonight's) without counting it toward the level. */
	public static void forceStop(final MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		long day = currentDay(overworld);
		state(server).skip(active && activeDay >= 0 ? activeDay : day);
		endedByCommand = true;
	}

	private static void setClock(final MinecraftServer server, final long totalTicks) {
		server.clockManager().setTotalTicks(
			server.registryAccess().getOrThrow(net.minecraft.world.clock.WorldClocks.OVERWORLD), totalTicks
		);
	}

	private static void syncPlayers(final MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			boolean expected = isActive(player.level());
			Boolean sent = syncedPlayers.get(player.getUUID());
			if (sent == null || sent != expected) {
				if (ServerPlayNetworking.canSend(player, BloodMoonSyncPayload.TYPE)) {
					ServerPlayNetworking.send(player, new BloodMoonSyncPayload(expected));
				}
				syncedPlayers.put(player.getUUID(), expected);
			}
		}
	}

	private static void broadcast(final MinecraftServer server, final Component message, final boolean actionBar) {
		for (ServerPlayer player : server.overworld().players()) {
			player.sendSystemMessage(message, actionBar);
		}
	}

	public static boolean mobGriefingAllowed(final ServerLevel level) {
		return !BloodMoonConfig.get().respectMobGriefing || level.getGameRules().get(GameRules.MOB_GRIEFING);
	}
}
