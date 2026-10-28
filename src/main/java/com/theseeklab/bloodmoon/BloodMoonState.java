package com.theseeklab.bloodmoon;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world Blood Moon bookkeeping, saved in data/bloodmoonevents_state.dat. */
public class BloodMoonState extends SavedData {
	public static final String FILE_ID = BloodmoonEvents.MOD_ID + "_state";
	// Fabric API handles a null DataFixTypes for modded saved data.
	public static final SavedData.Factory<BloodMoonState> FACTORY = new SavedData.Factory<>(BloodMoonState::new, BloodMoonState::load, null);

	/** Day forced to be a Blood Moon by command, or -1. */
	private long forcedDay;
	/** Day whose Blood Moon was cancelled by command, or -1. */
	private long skippedDay;
	/** Number of Blood Moons that lasted until dawn (player deaths don't matter). The next one is level completed + 1. */
	private int completed;
	private long lastCompletedDay;

	public BloodMoonState() {
		this(-1L, -1L, 0, -1L);
	}

	private BloodMoonState(final long forcedDay, final long skippedDay, final int completed, final long lastCompletedDay) {
		this.forcedDay = forcedDay;
		this.skippedDay = skippedDay;
		this.completed = completed;
		this.lastCompletedDay = lastCompletedDay;
	}

	private static BloodMoonState load(final CompoundTag tag, final HolderLookup.Provider registries) {
		return new BloodMoonState(
			tag.contains("forced_day") ? tag.getLong("forced_day") : -1L,
			tag.contains("skipped_day") ? tag.getLong("skipped_day") : -1L,
			tag.getInt("completed"),
			tag.contains("last_completed_day") ? tag.getLong("last_completed_day") : -1L
		);
	}

	@Override
	public CompoundTag save(final CompoundTag tag, final HolderLookup.Provider registries) {
		tag.putLong("forced_day", forcedDay);
		tag.putLong("skipped_day", skippedDay);
		tag.putInt("completed", completed);
		tag.putLong("last_completed_day", lastCompletedDay);
		return tag;
	}

	public long forcedDay() {
		return forcedDay;
	}

	public long skippedDay() {
		return skippedDay;
	}

	public int completed() {
		return completed;
	}

	/** Level of the next (or currently running) Blood Moon. */
	public int nextLevel() {
		return completed + 1;
	}

	/** Makes the next Blood Moon this level. */
	public void setNextLevel(final int level) {
		completed = Math.max(0, level - 1);
		setDirty();
	}

	public void force(final long day) {
		forcedDay = day;
		if (skippedDay == day) {
			skippedDay = -1L;
		}
		setDirty();
	}

	public void skip(final long day) {
		skippedDay = day;
		if (forcedDay == day) {
			forcedDay = -1L;
		}
		setDirty();
	}

	public void markCompleted(final long day) {
		if (lastCompletedDay != day) {
			lastCompletedDay = day;
			completed++;
			setDirty();
		}
	}
}
