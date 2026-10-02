package com.theseeklab.bloodmoon.mixin;

import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the Blood Moon layers to every overworld environment (client and server levels). */
@Mixin(EnvironmentAttributeSystem.Builder.class)
public abstract class EnvironmentAttributeSystemBuilderMixin {
	@Inject(method = "addDefaultLayers", at = @At("RETURN"))
	private void bloodmoon$addLayers(final Level level, final CallbackInfoReturnable<EnvironmentAttributeSystem.Builder> cir) {
		if (level.dimension() == Level.OVERWORLD) {
			BloodMoonEnvironment.addLayers((EnvironmentAttributeSystem.Builder) (Object) this, level);
		}
	}
}
