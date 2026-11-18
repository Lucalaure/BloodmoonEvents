package com.theseeklab.bloodmoon.restore;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.theseeklab.bloodmoon.BloodmoonEvents;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/**
 * Blocks the horde broke that are waiting to come back, saved in data/bloodmoonevents/broken_blocks.dat so they
 * survive restarts and blocks in unloaded chunks come back once the chunk loads. Only the overworld has Blood Moons,
 * so positions are overworld positions.
 */
public class BrokenBlocksState extends SavedData {
	/** One broken block: what was there, and the block entity data to put back (signs, banners...), if any. */
	public record Entry(BlockPos pos, BlockState state, Optional<CompoundTag> blockEntity) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(
			i -> i.group(
					BlockPos.CODEC.fieldOf("pos").forGetter(Entry::pos),
					BlockState.CODEC.fieldOf("state").forGetter(Entry::state),
					CompoundTag.CODEC.optionalFieldOf("block_entity").forGetter(Entry::blockEntity)
				)
				.apply(i, Entry::new)
		);
	}

	public static final Codec<BrokenBlocksState> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				Entry.CODEC.listOf().optionalFieldOf("blocks", List.of()).forGetter(BrokenBlocksState::list),
				Codec.LONG.listOf().optionalFieldOf("placed_during_blood_moon", List.of()).forGetter(s -> new ArrayList<>(s.placed))
			)
			.apply(i, BrokenBlocksState::new)
	);
	// Fabric API handles a null DataFixTypes for modded saved data.
	public static final SavedDataType<BrokenBlocksState> TYPE = new SavedDataType<>(
		BloodmoonEvents.id("broken_blocks"), BrokenBlocksState::new, CODEC, null
	);

	private final Long2ObjectMap<Entry> entries = new Long2ObjectLinkedOpenHashMap<>();
	/** Positions where a player placed a block during the current Blood Moon. Those blocks aren't restored. */
	private final LongSet placed = new LongOpenHashSet();

	public BrokenBlocksState() {
	}

	private BrokenBlocksState(final List<Entry> list, final List<Long> placedList) {
		for (Entry entry : list) {
			entries.putIfAbsent(entry.pos().asLong(), entry);
		}
		placed.addAll(placedList);
	}

	private List<Entry> list() {
		return new ArrayList<>(entries.values());
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
