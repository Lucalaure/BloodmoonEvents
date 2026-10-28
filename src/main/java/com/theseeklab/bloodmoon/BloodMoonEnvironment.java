package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * The Blood Moon's effect on the world's atmosphere. 1.21.1 has no environment attribute system, so the client
 * render mixins call in here to tint the sky, fog, clouds and light red, force a full moon and dim the stars, and
 * the server's bed mixin asks whether sleeping is blocked. Clients use the synced fade value, servers use the
 * authoritative state.
 */
public final class BloodMoonEnvironment {
	private static final Vec3 SKY_RED = new Vec3(0.35, 0.02, 0.02);
	private static final Vec3 FOG_RED = new Vec3(0.28, 0.03, 0.02);
	private static final Vec3 CLOUD_RED = new Vec3(0.55, 0.12, 0.12);
	private static final Vector3fc LIGHT_RED = new Vector3f(1.0F, 0.32F, 0.28F);
	public static final Component SLEEP_BLOCKED =
		Component.translatableWithFallback("bloodmoonevents.sleep_blocked", "You can't sleep during a Blood Moon!");

	/** Client-side fade value (0 = normal night, 1 = full Blood Moon). Written by the client tick handler. */
	public static volatile float clientIntensity;

	private BloodMoonEnvironment() {
	}

	public static float intensity(final Level level) {
		if (level.dimension() != Level.OVERWORLD) {
			return 0.0F;
		}
		if (level.isClientSide()) {
			return clientIntensity;
		}
		return BloodMoonManager.isActive(level) ? 1.0F : 0.0F;
	}

	public static Vec3 skyColor(final Level level, final Vec3 base) {
		return tint(level, base, SKY_RED);
	}

	public static Vec3 fogColor(final Level level, final Vec3 base) {
		return tint(level, base, FOG_RED);
	}

	public static Vec3 cloudColor(final Level level, final Vec3 base) {
		return tint(level, base, CLOUD_RED);
	}

	/** Tints the sky light colour used by the lightmap, in place. */
	public static void tintSkyLight(final Level level, final Vector3f color) {
		float t = tintAmount(level);
		if (t > 0.0F) {
			color.lerp(LIGHT_RED, t);
		}
	}

	public static float starBrightness(final Level level, final float base) {
		return base * (1.0F - 0.7F * intensity(level));
	}

	public static boolean forceFullMoon(final Level level) {
		return intensity(level) > 0.0F;
	}

	public static boolean sleepBlocked(final Level level) {
		return BloodMoonConfig.get().preventSleeping && intensity(level) > 0.0F;
	}

	private static Vec3 tint(final Level level, final Vec3 base, final Vec3 red) {
		float t = tintAmount(level);
		return t <= 0.0F ? base : base.lerp(red, t);
	}

	private static float tintAmount(final Level level) {
		return intensity(level) * BloodMoonConfig.get().skyTintStrength;
	}
}
