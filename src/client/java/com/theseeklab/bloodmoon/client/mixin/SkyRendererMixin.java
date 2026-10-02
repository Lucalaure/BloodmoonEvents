package com.theseeklab.bloodmoon.client.mixin;

import com.theseeklab.bloodmoon.client.BloodmoonEventsClient;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.client.renderer.SkyRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** Paints the moon blood red and makes it bigger during a Blood Moon. */
@Mixin(SkyRenderer.class)
public abstract class SkyRendererMixin {
	@ModifyArg(
		method = "renderMoon",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/SkyRenderer;applyCelestialBodyTransform(Lcom/mojang/blaze3d/vertex/PoseStack;FF)Lorg/joml/Matrix4f;"),
		index = 2
	)
	private float bloodmoon$moonSize(final float scale) {
		float t = BloodmoonEventsClient.intensity();
		return t <= 0.0F ? scale : scale * (1.0F + (BloodMoonConfig.get().moonScale - 1.0F) * t);
	}

	@ModifyArg(
		method = "renderMoon",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/DynamicGpuData;writeTransform(Lorg/joml/Matrix4f;Lorg/joml/Vector4f;)Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;"),
		index = 1
	)
	private Vector4f bloodmoon$moonColor(final Vector4f color) {
		float t = BloodmoonEventsClient.intensity();
		if (t <= 0.0F) {
			return color;
		}
		return new Vector4f(color.x(), color.y() * (1.0F - 0.8F * t), color.z() * (1.0F - 0.85F * t), color.w());
	}
}
