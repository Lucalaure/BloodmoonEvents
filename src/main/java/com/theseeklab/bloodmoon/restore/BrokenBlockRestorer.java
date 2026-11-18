package com.theseeklab.bloodmoon.restore;

import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.block.ModBlocks;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Clearable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.entity.BeehiveBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Puts back the blocks the horde broke once the Blood Moon is over, the counterpart to horde cobblestone crumbling.
 * <p>
 * While a Blood Moon runs, every block a zombie digs out or a horde creeper blows up is remembered (and drops
 * nothing, so it can't be farmed). A few seconds after dawn, after the horde cobblestone has crumbled, the blocks
 * reappear a handful at a time, lowest first so sand and torches have something to rest on. Blocks in unloaded
 * chunks come back when the chunk loads. A spot the player has since built on is left alone and the original block
 * is dropped as an item there instead. A spot a mob or player is standing in waits until it is clear.
 * <p>
 * With restoreOnlyPreexistingBlocks (the default), blocks a player placed during the Blood Moon are not restored:
 * they are combat material, not the build being protected, so the horde breaks them like vanilla blocks and they drop.
 * <p>
 * Containers come back empty, because their contents spilled out when they broke, just like in vanilla. Shulker
 * boxes aren't tracked at all: they keep their contents in the dropped item, so they drop normally.
 */
public final class BrokenBlockRestorer {
	/** Wait this long after dawn so the horde cobblestone crumbles (1-3 s) before the world rebuilds. */
	private static final int START_DELAY_TICKS = 80;
	private static final int PASS_INTERVAL_TICKS = 4;
	private static final int IDLE_INTERVAL_TICKS = 20;
	private static final int BLOCKS_PER_PASS = 6;
	/** After this many passes that restored nothing, blocks that can't stand on their own yet are placed anyway. */
	private static final int STALLED_PASSES_BEFORE_FORCING = 3;

	private static int quietTicks;
	private static int nextPassIn;
	private static int stalledPasses;

	private BrokenBlockRestorer() {
	}

	public static void reset() {
		quietTicks = 0;
		nextPassIn = 0;
		stalledPasses = 0;
	}

	public static BrokenBlocksState state(final MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(BrokenBlocksState.TYPE);
	}

	// --- Recording ---

	/**
	 * Remembers the block at {@code pos} (and the other half of a door or bed) before the horde breaks it.
	 *
	 * @return true if the block will be restored, so breaking it must not drop anything
	 */
	public static boolean record(final ServerLevel level, final BlockPos pos) {
		if (!BloodMoonConfig.get().restoreBrokenBlocks || level.dimension() != Level.OVERWORLD) {
			return false;
		}
		BrokenBlocksState state = state(level.getServer());
		if (state.contains(pos)) {
			// Already broken once tonight, so this is something the player put back. Let it drop as usual.
			return false;
		}
		BlockState blockState = level.getBlockState(pos);
		if (!isRestorable(level, pos, blockState)) {
			return false;
		}
		BlockPos partner = partnerOf(pos, blockState);
		if (BloodMoonConfig.get().restoreOnlyPreexistingBlocks
			&& (state.wasPlaced(pos) || partner != null && state.wasPlaced(partner))) {
			// Placed during the fight, so it isn't part of the build being protected. Breaks like any other block.
			return false;
		}
		state.add(capture(level, pos, blockState));

		if (partner != null && !state.contains(partner)) {
			BlockState partnerState = level.getBlockState(partner);
			if (partnerState.is(blockState.getBlock())) {
				state.add(capture(level, partner, partnerState));
			}
		}
		return true;
	}

	/** Remembers that a player placed a block here during the Blood Moon, so it isn't restored if the horde breaks it. */
	public static void onPlayerPlaced(final ServerLevel level, final BlockPos pos) {
		if (BloodMoonManager.isActive(level)) {
			state(level.getServer()).markPlaced(pos);
		}
	}

	/** Records every block an explosion is about to destroy. Returns the positions whose drops must be suppressed. */
	public static LongSet recordAll(final ServerLevel level, final List<BlockPos> positions) {
		LongSet recorded = new LongOpenHashSet();
		for (BlockPos pos : positions) {
			if (record(level, pos)) {
				recorded.add(pos.asLong());
			}
		}
		return recorded;
	}

	private static boolean isRestorable(final ServerLevel level, final BlockPos pos, final BlockState state) {
		Block block = state.getBlock();
		if (state.isAir()
			|| block instanceof LiquidBlock
			|| block instanceof BaseFireBlock
			|| block instanceof TntBlock
			|| state.is(ModBlocks.HORDE_COBBLESTONE)
			|| state.getDestroySpeed(level, pos) < 0.0F) {
			return false;
		}
		return !(level.getBlockEntity(pos) instanceof ShulkerBoxBlockEntity);
	}

	private static BrokenBlocksState.Entry capture(final ServerLevel level, final BlockPos pos, final BlockState state) {
		BlockEntity blockEntity = level.getBlockEntity(pos);
		Optional<CompoundTag> data = Optional.empty();
		// Containers spill their contents and bees fly out when broken, so keeping that data would duplicate it.
		if (blockEntity != null && !(blockEntity instanceof Clearable) && !(blockEntity instanceof BeehiveBlockEntity)) {
			data = Optional.of(blockEntity.saveWithoutMetadata(level.registryAccess()));
		}
		return new BrokenBlocksState.Entry(pos.immutable(), state, data);
	}

	/** The other half of a two-block block (doors, tall plants, beds), which disappears along with it. */
	private static @Nullable BlockPos partnerOf(final BlockPos pos, final BlockState state) {
		if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
			return state.getValue(BlockStateProperties.DOUBLE_BLOCK_HALF) == DoubleBlockHalf.LOWER ? pos.above() : pos.below();
		}
		if (state.hasProperty(BlockStateProperties.BED_PART) && state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
			return pos.relative(BedBlock.getConnectedDirection(state));
		}
		return null;
	}

	// --- Restoring ---

	public static void tick(final MinecraftServer server) {
		ServerLevel overworld = server.overworld();
		if (BloodMoonManager.isBloodMoonNow(overworld)) {
			quietTicks = 0;
			return;
		}
		if (quietTicks == 0) {
			// The Blood Moon is over, so tonight's placements no longer matter.
			state(server).clearPlaced();
		}
		if (quietTicks < START_DELAY_TICKS) {
			quietTicks++;
			return;
		}
		if (--nextPassIn > 0) {
			return;
		}
		BrokenBlocksState state = state(server);
		nextPassIn = state.isEmpty() || !restorePass(overworld, state) ? IDLE_INTERVAL_TICKS : PASS_INTERVAL_TICKS;
	}

	private enum Outcome { RESTORED, BLOCKED, WAIT }

	/** Restores a few blocks in loaded chunks, lowest first. Returns false if nothing could be done this pass. */
	private static boolean restorePass(final ServerLevel level, final BrokenBlocksState state) {
		List<BrokenBlocksState.Entry> candidates = new ArrayList<>();
		for (BrokenBlocksState.Entry entry : state.entries()) {
			if (level.isLoaded(entry.pos())) {
				candidates.add(entry);
			}
		}
		if (candidates.isEmpty()) {
			return false;
		}
		candidates.sort(Comparator.comparingInt(e -> e.pos().getY()));

		boolean force = stalledPasses >= STALLED_PASSES_BEFORE_FORCING;
		int done = 0;
		for (BrokenBlocksState.Entry entry : candidates) {
			if (done >= BLOCKS_PER_PASS) {
				break;
			}
			if (!state.contains(entry.pos())) {
				// Already restored as the other half of a door or bed.
				continue;
			}
			List<BrokenBlocksState.Entry> group = groupOf(state, entry);
			Outcome outcome = restoreGroup(level, group, force);
			if (outcome != Outcome.WAIT) {
				for (BrokenBlocksState.Entry member : group) {
					state.remove(member.pos());
				}
				done++;
			}
		}
		stalledPasses = done == 0 ? stalledPasses + 1 : 0;
		return done > 0;
	}

	private static List<BrokenBlocksState.Entry> groupOf(final BrokenBlocksState state, final BrokenBlocksState.Entry entry) {
		List<BrokenBlocksState.Entry> group = new ArrayList<>(2);
		group.add(entry);
		BlockPos partnerPos = partnerOf(entry.pos(), entry.state());
		BrokenBlocksState.Entry partner = partnerPos == null ? null : state.get(partnerPos);
		if (partner != null && partner.state().is(entry.state().getBlock())) {
			group.add(partner);
			group.sort(Comparator.comparingInt(e -> e.pos().getY()));
		}
		return group;
	}

	private static Outcome restoreGroup(final ServerLevel level, final List<BrokenBlocksState.Entry> group, final boolean force) {
		BrokenBlocksState.Entry first = group.getFirst();
		for (BrokenBlocksState.Entry member : group) {
			BlockState current = level.getBlockState(member.pos());
			if (current.is(member.state().getBlock())) {
				// The same block is already back (put there by a player, or it never actually broke). Nothing to do.
				return Outcome.BLOCKED;
			}
			if (!isFree(current)) {
				// The player built here since. Leave their block and hand the original back as an item.
				ItemStack item = new ItemStack(first.state().getBlock().asItem());
				if (!item.is(Items.AIR)) {
					Block.popResource(level, first.pos(), item);
				}
				return Outcome.BLOCKED;
			}
			if (isOccupied(level, member)) {
				return Outcome.WAIT;
			}
		}
		if (!force && !first.state().canSurvive(level, first.pos())) {
			// Wait for whatever it rests on (a wall for a torch, ground for sand) to come back first.
			return Outcome.WAIT;
		}

		for (BrokenBlocksState.Entry member : group) {
			place(level, member);
		}
		return Outcome.RESTORED;
	}

	/** Air, water, grass, fire, or leftover horde cobblestone: things a restored block may replace. */
	private static boolean isFree(final BlockState current) {
		return current.canBeReplaced() || current.is(ModBlocks.HORDE_COBBLESTONE);
	}

	/** Whether a mob or player is standing where the block would go, so restoring it would trap them. */
	private static boolean isOccupied(final ServerLevel level, final BrokenBlocksState.Entry entry) {
		VoxelShape shape = entry.state().getCollisionShape(level, entry.pos());
		if (shape.isEmpty()) {
			return false;
		}
		AABB box = shape.bounds().move(entry.pos());
		return !level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive).isEmpty();
	}

	private static void place(final ServerLevel level, final BrokenBlocksState.Entry entry) {
		BlockPos pos = entry.pos();
		BlockState state = entry.state();
		level.setBlock(pos, state, Block.UPDATE_ALL);

		entry.blockEntity().ifPresent(data -> {
			BlockEntity blockEntity = level.getBlockEntity(pos);
			if (blockEntity != null) {
				blockEntity.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), data));
				blockEntity.setChanged();
				level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
			}
		});

		level.sendParticles(
			new BlockParticleOption(ParticleTypes.BLOCK, state), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.05
		);
		level.playSound(null, pos, state.getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.7F, 0.9F);
	}
}
