package com.theseeklab.bloodmoon.client.mixin;

import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Tints sky light red during a Blood Moon, so everything lit by the night sky takes on a red cast. */
@Mixin(LightTexture.class)
public abstract class LightTextureMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	/** The first Vector3f in updateLightTexture is the sky light colour. */
	@ModifyVariable(method = "updateLightTexture", at = @At(value = "STORE"), ordinal = 0)
	private Vector3f bloodmoon$skyLightColor(final Vector3f color) {
		if (minecraft.level != null) {
			BloodMoonEnvironment.tintSkyLight(minecraft.level, color);
		}
		return color;
	}
}
