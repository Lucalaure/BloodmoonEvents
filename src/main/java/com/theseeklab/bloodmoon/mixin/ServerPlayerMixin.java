package com.theseeklab.bloodmoon.mixin;

import com.mojang.datafixers.util.Either;
import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Beds still set your spawn point during a Blood Moon, but you can't sleep in them. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin {
	@Inject(
		method = "startSleepInBed",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;setRespawnPosition(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;FZZ)V", shift = At.Shift.AFTER),
		cancellable = true
	)
	private void bloodmoon$blockSleep(final BlockPos pos, final CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		ServerPlayer player = (ServerPlayer) (Object) this;
		if (BloodMoonEnvironment.sleepBlocked(player.level())) {
			player.displayClientMessage(BloodMoonEnvironment.SLEEP_BLOCKED, true);
			cir.setReturnValue(Either.left(Player.BedSleepingProblem.OTHER_PROBLEM));
		}
	}
}
