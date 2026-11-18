package com.theseeklab.bloodmoon.restore;

import com.theseeklab.bloodmoon.BloodmoonEvents;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/**
 * Blocks the horde broke that are waiting to come back, saved in data/bloodmoonevents_broken_blocks.dat so they
 * survive restarts and blocks in unloaded chunks come back once the chunk loads. Only the overworld has Blood Moons,
 * so positions are overworld positions.
 */
public class BrokenBlocksState extends SavedData {
	/** One broken block: what was there, and the block entity data to put back (signs, banners...), if any. */
	public record Entry(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntity) {
	}

	public static final String FILE_ID = BloodmoonEvents.MOD_ID + "_broken_blocks";
	// Fabric API handles a null DataFixTypes for modded saved data.
	public static final SavedData.Factory<BrokenBlocksState> FACTORY = new SavedData.Factory<>(BrokenBlocksState::new, BrokenBlocksState::load, null);

	private final Long2ObjectMap<Entry> entries = new Long2ObjectLinkedOpenHashMap<>();
	/** Positions where a player placed a block during the current Blood Moon. Those blocks aren't restored. */
	private final LongSet placed = new LongOpenHashSet();

	public BrokenBlocksState() {
	}

	private static BrokenBlocksState load(final CompoundTag tag, final HolderLookup.Provider registries) {
		BrokenBlocksState state = new BrokenBlocksState();
		ListTag blocks = tag.getList("blocks", Tag.TAG_COMPOUND);
		for (int i = 0; i < blocks.size(); i++) {
			CompoundTag entry = blocks.getCompound(i);
			BlockPos pos = BlockPos.of(entry.getLong("pos"));
			BlockState blockState = NbtUtils.readBlockState(registries.lookupOrThrow(Registries.BLOCK), entry.getCompound("state"));
			Optional<CompoundTag> data = entry.contains("block_entity", Tag.TAG_COMPOUND)
				? Optional.of(entry.getCompound("block_entity"))
				: Optional.empty();
			state.entries.putIfAbsent(pos.asLong(), new Entry(pos, blockState, data));
		}
		for (long pos : tag.getLongArray("placed_during_blood_moon")) {
			state.placed.add(pos);
		}
		return state;
	}

	@Override
	public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		ListTag blocks = new ListTag();
		for (Entry entry : entries.values()) {
			CompoundTag compound = new CompoundTag();
			compound.putLong("pos", entry.pos().asLong());
			compound.put("state", NbtUtils.writeBlockState(entry.state()));
			entry.blockEntity().ifPresent(data -> compound.put("block_entity", data));
			blocks.add(compound);
		}
		tag.put("blocks", blocks);
		tag.put("placed_during_blood_moon", new LongArrayTag(placed.toLongArray()));
		return tag;
	}

	public boolean isEmpty() {
		return entries.isEmpty();
	}

	public boolean contains(final BlockPos pos) {
		return entries.containsKey(pos.asLong());
	}

	public @Nullable Entry get(final BlockPos pos) {
		return entries.get(pos.asLong());
	}

	public Collection<Entry> entries() {
		return entries.values();
	}

	public void add(final Entry entry) {
		if (entries.putIfAbsent(entry.pos().asLong(), entry) == null) {
			setDirty();
		}
	}

	public void markPlaced(final BlockPos pos) {
		if (placed.add(pos.asLong())) {
			setDirty();
		}
	}

	public boolean wasPlaced(final BlockPos pos) {
		return placed.contains(pos.asLong());
	}

	public void clearPlaced() {
		if (!placed.isEmpty()) {
			placed.clear();
			setDirty();
		}
	}

	public void remove(final BlockPos pos) {
		if (entries.remove(pos.asLong()) != null) {
			setDirty();
		}
	}
}
