package com.theseeklab.bloodmoon.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobAccessor {
	@Accessor("goalSelector")
	GoalSelector bloodmoon$getGoalSelector();

	@Accessor("targetSelector")
	GoalSelector bloodmoon$getTargetSelector();

	@Accessor("persistenceRequired")
	void bloodmoon$setPersistenceRequired(boolean persistenceRequired);
}
