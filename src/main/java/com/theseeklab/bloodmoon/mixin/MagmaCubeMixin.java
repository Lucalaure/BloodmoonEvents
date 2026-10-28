package com.theseeklab.bloodmoon.mixin;

import com.theseeklab.bloodmoon.ai.SlimeSuperJump;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.MagmaCube;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Magma cubes override the slime jump without calling super, so they need their own hook. */
@Mixin(MagmaCube.class)
public abstract class MagmaCubeMixin {
	@Inject(method = "jumpFromGround", at = @At("TAIL"))
	private void bloodmoon$superJump(final CallbackInfo ci) {
		SlimeSuperJump.boost((Mob) (Object) this);
	}
}
