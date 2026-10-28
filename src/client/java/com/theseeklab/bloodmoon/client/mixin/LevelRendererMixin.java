package com.theseeklab.bloodmoon.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import com.theseeklab.bloodmoon.client.BloodmoonEventsClient;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Paints the moon blood red, makes it bigger and keeps it full during a Blood Moon. */
@Mixin(LevelRenderer.class)
public abstract class LevelRendererMixin {
	@Shadow
	private ClientLevel level;

	/** The moon's half-size; the sun is drawn with 30. */
	@ModifyConstant(method = "renderSky", constant = @Constant(floatValue = 20.0F))
	private float bloodmoon$moonSize(final float scale) {
		float t = BloodmoonEventsClient.intensity();
		return t <= 0.0F ? scale : scale * (1.0F + (BloodMoonConfig.get().moonScale - 1.0F) * t);
	}

	/** Runs right before the moon texture is bound (the first bind is the sun's). */
	@Inject(
		method = "renderSky",
		at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;setShaderTexture(ILnet/minecraft/resources/ResourceLocation;)V", ordinal = 1)
	)
	private void bloodmoon$moonColor(final CallbackInfo ci) {
		float t = BloodmoonEventsClient.intensity();
		if (t <= 0.0F) {
			return;
		}
		float[] color = RenderSystem.getShaderColor();
		RenderSystem.setShaderColor(color[0], color[1] * (1.0F - 0.8F * t), color[2] * (1.0F - 0.85F * t), color[3]);
	}

	@ModifyExpressionValue(method = "renderSky", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/ClientLevel;getMoonPhase()I"))
	private int bloodmoon$fullMoon(final int phase) {
		return BloodMoonEnvironment.forceFullMoon(level) ? 0 : phase;
	}
}
