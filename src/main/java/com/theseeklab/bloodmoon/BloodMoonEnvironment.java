package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ARGB;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.joml.Vector4f;
import org.joml.Vector4fc;

/**
 * Hooks the Blood Moon into 26.x's environment attribute system. Each level (client and server) gets extra
 * layers on top of the vanilla dimension/biome/timeline layers that tint the sky, fog, clouds and light red,
 * force a full moon and block sleeping. The same code drives both sides: clients use the synced fade value,
 * servers use the authoritative state.
 */
public final class BloodMoonEnvironment {
	private static final Vector3fc SKY_RED = new Vector3f(0.35F, 0.02F, 0.02F);
	private static final Vector3fc FOG_RED = new Vector3f(0.28F, 0.03F, 0.02F);
	private static final Vector4fc CLOUD_RED = new Vector4f(0.55F, 0.12F, 0.12F, 1.0F);
	private static final Vector3fc LIGHT_RED = new Vector3f(1.0F, 0.32F, 0.28F);
	private static final BedRule NO_SLEEP = new BedRule(
		BedRule.Rule.NEVER, BedRule.Rule.ALWAYS, false, false,
		Optional.of(Component.translatableWithFallback("bloodmoonevents.sleep_blocked", "You can't sleep during a Blood Moon!"))
	);

	/** Client-side fade value (0 = normal night, 1 = full Blood Moon). Written by the client tick handler. */
	public static volatile float clientIntensity;

	private BloodMoonEnvironment() {
	}

	public static float intensity(final Level level) {
		if (level.isClientSide()) {
			return clientIntensity;
		}
		return BloodMoonManager.isActive(level) ? 1.0F : 0.0F;
	}

	public static void addLayers(final EnvironmentAttributeSystem.Builder builder, final Level level) {
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_COLOR, (base, tick) -> tint(level, base, SKY_RED));
		builder.addTimeBasedLayer(EnvironmentAttributes.FOG_COLOR, (base, tick) -> tint(level, base, FOG_RED));
		builder.addTimeBasedLayer(EnvironmentAttributes.SKY_LIGHT_COLOR, (base, tick) -> tint(level, base, LIGHT_RED));
		builder.addTimeBasedLayer(EnvironmentAttributes.CLOUD_COLOR, (base, tick) -> {
			float t = intensity(level) * BloodMoonConfig.get().skyTintStrength;
			return t <= 0.0F ? base : ARGB.srgbLerp(t, base, CLOUD_RED);
		});
		builder.addTimeBasedLayer(EnvironmentAttributes.STAR_BRIGHTNESS, (base, tick) -> base * (1.0F - 0.7F * intensity(level)));
		builder.addTimeBasedLayer(EnvironmentAttributes.MOON_PHASE, (base, tick) -> intensity(level) > 0.0F ? MoonPhase.FULL_MOON : base);
		builder.addTimeBasedLayer(
			EnvironmentAttributes.BED_RULE,
			(base, tick) -> BloodMoonConfig.get().preventSleeping && intensity(level) > 0.0F ? NO_SLEEP : base
		);
	}

	private static Vector3fc tint(final Level level, final Vector3fc base, final Vector3fc red) {
		float t = intensity(level) * BloodMoonConfig.get().skyTintStrength;
		return t <= 0.0F ? base : ARGB.srgbLerp(t, base, red);
	}
}
