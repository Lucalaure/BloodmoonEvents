package com.theseeklab.bloodmoon.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tints the open-air fog red during a Blood Moon. Water, lava and powder snow fog are left alone. */
@Mixin(FogRenderer.class)
public abstract class FogRendererMixin {
	/** The biome fog colour, sampled once per frame before sunrise, rain and sky blending are applied. */
	@ModifyExpressionValue(
		method = "setupColor",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/util/CubicSampler;gaussianSampleVec3(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/util/CubicSampler$Vec3Fetcher;)Lnet/minecraft/world/phys/Vec3;")
	)
	private static Vec3 bloodmoon$fogColor(final Vec3 color, @Local(argsOnly = true) final ClientLevel level) {
		return BloodMoonEnvironment.fogColor(level, color);
	}
}
