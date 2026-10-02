package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.command.BloodMoonCommand;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import com.theseeklab.bloodmoon.network.BloodMoonSyncPayload;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BloodmoonEvents implements ModInitializer {
	public static final String MOD_ID = "bloodmoonevents";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static Identifier id(final String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		BloodMoonConfig.load();

		PayloadTypeRegistry.clientboundPlay().register(BloodMoonSyncPayload.TYPE, BloodMoonSyncPayload.CODEC);

		ServerTickEvents.END_SERVER_TICK.register(BloodMoonManager::tick);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> BloodMoonManager.reset());
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> BloodMoonManager.forgetPlayer(handler.getPlayer()));
		ServerEntityEvents.ENTITY_LOAD.register(HordeMobs::onEntityLoad);

		CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) -> BloodMoonCommand.register(dispatcher));
	}
}
