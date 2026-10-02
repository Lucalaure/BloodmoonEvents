package com.theseeklab.bloodmoon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/** Per-world Blood Moon bookkeeping, saved in data/bloodmoonevents/state.dat. */
public class BloodMoonState extends SavedData {
	public static final Codec<BloodMoonState> CODEC = RecordCodecBuilder.create(
		i -> i.group(
				Codec.LONG.optionalFieldOf("forced_day", -1L).forGetter(s -> s.forcedDay),
				Codec.LONG.optionalFieldOf("skipped_day", -1L).forGetter(s -> s.skippedDay),
				Codec.INT.optionalFieldOf("completed", 0).forGetter(s -> s.completed),
				Codec.LONG.optionalFieldOf("last_completed_day", -1L).forGetter(s -> s.lastCompletedDay)
			)
			.apply(i, BloodMoonState::new)
	);
	// Fabric API handles a null DataFixTypes for modded saved data.
	public static final SavedDataType<BloodMoonState> TYPE = new SavedDataType<>(BloodmoonEvents.id("state"), BloodMoonState::new, CODEC, null);

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
