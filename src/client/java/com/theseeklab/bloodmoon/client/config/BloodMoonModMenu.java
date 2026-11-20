package com.theseeklab.bloodmoon.client.config;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import com.theseeklab.bloodmoon.BloodmoonEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.AlertScreen;
import net.minecraft.network.chat.Component;

/**
 * Adds the Configure button in Mod Menu. Both Mod Menu and Cloth Config are optional: without Mod Menu this class is
 * never loaded, and without Cloth Config the button explains what to install instead of crashing.
 */
public class BloodMoonModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		if (FabricLoader.getInstance().isModLoaded("cloth-config")) {
			return BloodMoonConfigScreen::create;
		}
		return parent -> new AlertScreen(
			() -> Minecraft.getInstance().setScreen(parent),
			Component.translatable("config." + BloodmoonEvents.MOD_ID + ".title"),
			Component.translatable("config." + BloodmoonEvents.MOD_ID + ".needs_cloth")
		);
	}
}
