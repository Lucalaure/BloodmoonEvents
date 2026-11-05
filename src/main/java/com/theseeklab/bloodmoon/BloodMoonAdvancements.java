package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;

/**
 * The two Blood Moon advancements: surviving one, and surviving one at the highest difficulty level. A player
 * survives a Blood Moon by taking part in it (being in the overworld in survival or adventure while it runs), not
 * dying before dawn, and still being there when it ends. A Blood Moon cancelled with /bloodmoon stop counts for
 * nobody. Both advancements use the impossible trigger, so only this class can grant them.
 */
public final class BloodMoonAdvancements {
	public static final Identifier SURVIVED = BloodmoonEvents.id("survived_blood_moon");
	public static final Identifier SURVIVED_MAX_LEVEL = BloodmoonEvents.id("survived_max_level_blood_moon");
	private static final String CRITERION = "survived";

	private static final Set<UUID> participants = new HashSet<>();
	private static final Set<UUID> died = new HashSet<>();

	private BloodMoonAdvancements() {
	}

	public static void reset() {
		participants.clear();
		died.clear();
	}

	/** Called regularly while a Blood Moon runs. */
	public static void trackParticipants(final ServerLevel overworld) {
		for (ServerPlayer player : overworld.players()) {
			if (canSurvive(player)) {
				participants.add(player.getUUID());
			}
		}
	}

	public static void onDeath(final LivingEntity entity, final DamageSource source) {
		if (entity instanceof ServerPlayer && BloodMoonManager.isActive()) {
			died.add(entity.getUUID());
		}
	}

	/** Called when a Blood Moon lasts until dawn. */
	public static void awardSurvivors(final MinecraftServer server, final int bloodMoonLevel) {
		boolean maxLevel = bloodMoonLevel >= BloodMoonConfig.get().maxLevel;
		for (ServerPlayer player : server.overworld().players()) {
			UUID id = player.getUUID();
			if (canSurvive(player) && participants.contains(id) && !died.contains(id)) {
				award(server, player, SURVIVED);
				if (maxLevel) {
					award(server, player, SURVIVED_MAX_LEVEL);
				}
			}
		}
		reset();
	}

	/** Creative and spectator players can't be hurt by the horde, so they can't earn these. */
	private static boolean canSurvive(final ServerPlayer player) {
		return player.isAlive() && !player.isSpectator() && !player.isCreative();
	}

	private static void award(final MinecraftServer server, final ServerPlayer player, final Identifier id) {
		AdvancementHolder advancement = server.getAdvancements().get(id);
		if (advancement != null) {
			player.getAdvancements().award(advancement, CRITERION);
		}
	}
}
