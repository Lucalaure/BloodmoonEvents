package com.theseeklab.bloodmoon.mixin;

import com.theseeklab.bloodmoon.ai.SlimeSuperJump;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Slime;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Blood Moon slimes jump higher, further and more often. Magma cubes reuse the jump delay via super. */
@Mixin(Slime.class)
public abstract class SlimeMixin {
	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	private void bloodmoon$superJump(final CallbackInfo ci) {
		SlimeSuperJump.boost((Mob) (Object) this);
	}

	@Inject(method = "getJumpDelay", at = @At("RETURN"), cancellable = true)
	private void bloodmoon$fasterJumps(final CallbackInfoReturnable<Integer> cir) {
		if (SlimeSuperJump.isEmpowered((Mob) (Object) this)) {
			cir.setReturnValue(Math.max(5, cir.getReturnValue() / 2));
		}
	}
}
