package com.theseeklab.bloodmoon.client;

import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import com.theseeklab.bloodmoon.network.BloodMoonSyncPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class BloodmoonEventsClient implements ClientModInitializer {
	/** Ticks for the sky to fully fade in or out (5 seconds). */
	private static final float FADE_TICKS = 100.0F;

	private static boolean active;

	@Override
	public void onInitializeClient() {
		ClientPlayNetworking.registerGlobalReceiver(BloodMoonSyncPayload.TYPE, (payload, context) -> active = payload.active());

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			active = false;
			BloodMoonEnvironment.clientIntensity = 0.0F;
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			float current = BloodMoonEnvironment.clientIntensity;
			float target = active && client.level != null ? 1.0F : 0.0F;
			if (current != target) {
				float step = 1.0F / FADE_TICKS;
				BloodMoonEnvironment.clientIntensity = target > current ? Math.min(target, current + step) : Math.max(target, current - step);
			}
		});
	}

	/** Used by the sky renderer mixin. */
	public static float intensity() {
		return BloodMoonEnvironment.clientIntensity;
	}
}
