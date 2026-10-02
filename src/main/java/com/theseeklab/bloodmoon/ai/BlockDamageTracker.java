package com.theseeklab.bloodmoon.ai;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Shared block-breaking progress, so several zombies hitting the same block break it faster and the
 * crack overlay is consistent. Progress decays if nobody touches the block for a while.
 */
public final class BlockDamageTracker {
	private static final int FORGET_AFTER_TICKS = 100;

	private static final Map<ResourceKey<Level>, Long2ObjectMap<Damage>> damageByLevel = new HashMap<>();

	private BlockDamageTracker() {
	}

	private static final class Damage {
		float progress;
		long lastHitTick;
		int lastStage = -1;
	}

	/**
	 * Adds one tick of breaking work to a block.
	 *
	 * @return the total progress so far, in ticks
	 */
	public static float addProgress(final ServerLevel level, final BlockPos pos, final float amount) {
		Damage damage = damageByLevel.computeIfAbsent(level.dimension(), k -> new Long2ObjectOpenHashMap<>())
			.computeIfAbsent(pos.asLong(), k -> new Damage());
		damage.progress += amount;
		damage.lastHitTick = level.getGameTime();
		return damage.progress;
	}

	/** Updates the crack overlay; {@code stage} is 0-9. */
	public static void showStage(final ServerLevel level, final BlockPos pos, final int stage) {
		Long2ObjectMap<Damage> map = damageByLevel.get(level.dimension());
		Damage damage = map == null ? null : map.get(pos.asLong());
		if (damage != null && damage.lastStage != stage) {
			damage.lastStage = stage;
			level.destroyBlockProgress(crackId(pos), pos, stage);
		}
	}

	public static void clear(final ServerLevel level, final BlockPos pos) {
		Long2ObjectMap<Damage> map = damageByLevel.get(level.dimension());
		if (map != null && map.remove(pos.asLong()) != null) {
			level.destroyBlockProgress(crackId(pos), pos, -1);
		}
	}

	public static void tick(final MinecraftServer server) {
		if (server.getTickCount() % 20 != 0) {
			return;
		}
		for (Map.Entry<ResourceKey<Level>, Long2ObjectMap<Damage>> entry : damageByLevel.entrySet()) {
			ServerLevel level = server.getLevel(entry.getKey());
			if (level == null) {
				continue;
			}
			long now = level.getGameTime();
			Iterator<Long2ObjectMap.Entry<Damage>> it = entry.getValue().long2ObjectEntrySet().iterator();
			while (it.hasNext()) {
				Long2ObjectMap.Entry<Damage> damage = it.next();
				if (now - damage.getValue().lastHitTick > FORGET_AFTER_TICKS) {
					BlockPos pos = BlockPos.of(damage.getLongKey());
					level.destroyBlockProgress(crackId(pos), pos, -1);
					it.remove();
				}
			}
		}
	}

	/** Crack overlays are keyed by a "breaker id"; use a stable negative id per block so it never clashes with an entity id. */
	private static int crackId(final BlockPos pos) {
		return -1 - (int) (pos.asLong() & 0x3FFFFFFF);
	}
}
