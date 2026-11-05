package com.theseeklab.bloodmoon.block;

import com.theseeklab.bloodmoon.BloodMoonManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The cobblestone horde zombies build with. It has no item and no loot table, and it crumbles away on its own once
 * no Blood Moon is running, so the horde never leaves permanent blocks in the world.
 * <p>
 * Each block re-checks every one to three seconds through a scheduled tick. Scheduled ticks are saved with the
 * chunk, so blocks in chunks that were unloaded at dawn crumble as soon as the chunk loads again, and blocks moved
 * by pistons keep checking from their new spot. The random delay makes a tower crumble over a few seconds instead
 * of vanishing all at once.
 */
public class HordeBlock extends Block {
	private static final int MIN_CHECK_TICKS = 20;
	private static final int MAX_CHECK_TICKS = 60;

	public HordeBlock(final Properties properties) {
		super(properties);
	}

	@Override
	protected void onPlace(final BlockState state, final Level level, final BlockPos pos, final BlockState oldState, final boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!level.isClientSide() && !oldState.is(this)) {
			scheduleCheck(level, pos);
		}
	}

	@Override
	protected void tick(final BlockState state, final ServerLevel level, final BlockPos pos, final RandomSource random) {
		if (BloodMoonManager.isBloodMoonNow(level)) {
			scheduleCheck(level, pos);
		} else {
			// Break particles and sound, no drops.
			level.destroyBlock(pos, false);
		}
	}

	private void scheduleCheck(final Level level, final BlockPos pos) {
		level.scheduleTick(pos, this, Mth.nextInt(level.getRandom(), MIN_CHECK_TICKS, MAX_CHECK_TICKS));
	}
}
