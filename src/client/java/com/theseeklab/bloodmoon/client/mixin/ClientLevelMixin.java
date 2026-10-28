package com.theseeklab.bloodmoon.client.mixin;

import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Tints the sky and clouds red and dims the stars during a Blood Moon. */
@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	@Inject(method = "getSkyColor", at = @At("RETURN"), cancellable = true)
	private void bloodmoon$skyColor(final Vec3 pos, final float partialTick, final CallbackInfoReturnable<Vec3> cir) {
		cir.setReturnValue(BloodMoonEnvironment.skyColor((Level) (Object) this, cir.getReturnValue()));
	}

	@Inject(method = "getCloudColor", at = @At("RETURN"), cancellable = true)
	private void bloodmoon$cloudColor(final float partialTick, final CallbackInfoReturnable<Vec3> cir) {
		cir.setReturnValue(BloodMoonEnvironment.cloudColor((Level) (Object) this, cir.getReturnValue()));
	}

	@Inject(method = "getStarBrightness", at = @At("RETURN"), cancellable = true)
	private void bloodmoon$starBrightness(final float partialTick, final CallbackInfoReturnable<Float> cir) {
		cir.setReturnValue(BloodMoonEnvironment.starBrightness((Level) (Object) this, cir.getReturnValue()));
	}
}
