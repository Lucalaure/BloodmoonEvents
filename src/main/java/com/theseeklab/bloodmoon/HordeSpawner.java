package com.theseeklab.bloodmoon;

import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Spawns waves of horde mobs in a ring around each player while a Blood Moon is active. */
public final class HordeSpawner {
	private static int ticksUntilWave;

	private HordeSpawner() {
	}

	/** @param bloodMoonLevel number of the running Blood Moon, which sets its difficulty */
	public static void tick(final ServerLevel level, final int bloodMoonLevel) {
		if (level.getDifficulty() == Difficulty.PEACEFUL) {
			return;
		}
		if (--ticksUntilWave > 0) {
			return;
		}

		BloodMoonConfig config = BloodMoonConfig.get();
		ticksUntilWave = Math.max(20, config.waveIntervalTicks.intAt(bloodMoonLevel));
		int cap = config.maxHordePerPlayer.intAt(bloodMoonLevel);
		int perWave = config.mobsPerWave.intAt(bloodMoonLevel);

		for (ServerPlayer player : level.players()) {
			if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
				continue;
			}

			AABB area = player.getBoundingBox().inflate(config.maxSpawnDistance + 32);
			int nearby = level.getEntitiesOfClass(Mob.class, area, HordeMobs::isHorde).size();
			int toSpawn = Math.min(perWave, cap - nearby);
			for (int i = 0; i < toSpawn; i++) {
				spawnOne(level, player, bloodMoonLevel);
			}
		}
	}

	private static void spawnOne(final ServerLevel level, final ServerPlayer player, final int bloodMoonLevel) {
		EntityType<?> type = pickType(level.getRandom(), bloodMoonLevel);
		if (type == null) {
			return;
		}

		BlockPos pos = findSpawnPos(level, player, type);
		if (pos == null) {
			return;
		}

		Entity entity = type.create(level, EntitySpawnReason.EVENT);
		if (!(entity instanceof Mob mob)) {
			if (entity != null) {
				entity.discard();
			}
			return;
		}

		mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, level.getRandom().nextFloat() * 360.0F, 0.0F);
		mob.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.EVENT, null);
		HordeMobs.prepare(mob, bloodMoonLevel);
		mob.setTarget(player);
		level.addFreshEntityWithPassengers(mob);
	}

	/** Picks a weighted random mob from the ones unlocked at this level. */
	private static @Nullable EntityType<?> pickType(final RandomSource random, final int bloodMoonLevel) {
		List<EntityType<?>> types = new ArrayList<>();
		List<Integer> typeWeights = new ArrayList<>();
		int total = 0;
		for (BloodMoonConfig.HordeSpawn spawn : BloodMoonConfig.get().hordeSpawns) {
			if (spawn.id == null || spawn.weight <= 0 || spawn.fromLevel > bloodMoonLevel) {
				continue;
			}
			Identifier id = Identifier.tryParse(spawn.id);
			if (id == null) {
				continue;
			}
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElse(null);
			if (type == null) {
				continue;
			}
			types.add(type);
			typeWeights.add(spawn.weight);
			total += spawn.weight;
		}
		if (total <= 0) {
			return null;
		}

		int roll = random.nextInt(total);
		for (int i = 0; i < types.size(); i++) {
			roll -= typeWeights.get(i);
			if (roll < 0) {
				return types.get(i);
			}
		}
		return types.getLast();
	}

	private static @Nullable BlockPos findSpawnPos(final ServerLevel level, final ServerPlayer player, final EntityType<?> type) {
		BloodMoonConfig config = BloodMoonConfig.get();
		RandomSource random = level.getRandom();
		BlockPos playerPos = player.blockPosition();
		int surfaceAtPlayer = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, playerPos.getX(), playerPos.getZ());
		boolean underground = config.spawnUnderground && playerPos.getY() < surfaceAtPlayer - 8;

		for (int attempt = 0; attempt < 12; attempt++) {
			double angle = random.nextDouble() * Math.PI * 2.0;
			double distance = Mth.nextDouble(random, config.minSpawnDistance, config.maxSpawnDistance);
			// Underground the horde spawns closer, since caves rarely have long open sight lines.
			if (underground) {
				distance *= 0.6;
			}
			int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
			int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
			if (!level.hasChunkAt(new BlockPos(x, playerPos.getY(), z))) {
				continue;
			}

			BlockPos candidate = underground
				? findCavePos(level, x, playerPos.getY(), z, type)
				: new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z), z);
			if (candidate != null && isValidSpawn(level, candidate, type)) {
				return candidate;
			}
		}
		return null;
	}

	private static @Nullable BlockPos findCavePos(final ServerLevel level, final int x, final int y, final int z, final EntityType<?> type) {
		for (int dy = 0; dy <= 12; dy++) {
			for (int sign : new int[]{1, -1}) {
				BlockPos pos = new BlockPos(x, y + dy * sign, z);
				if (isValidSpawn(level, pos, type)) {
					return pos;
				}
			}
		}
		return null;
	}

	private static boolean isValidSpawn(final ServerLevel level, final BlockPos pos, final EntityType<?> type) {
		if (!level.getWorldBorder().isWithinBounds(pos)) {
			return false;
		}
		if (!level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP)) {
			return false;
		}
		if (!level.getFluidState(pos).isEmpty()) {
			return false;
		}
		if (!SpawnPlacements.isSpawnPositionOk(type, level, pos)) {
			return false;
		}
		if (!level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
			return false;
		}
		// Never pop up right next to someone.
		double minDistance = BloodMoonConfig.get().minSpawnDistance * 0.5;
		return level.getNearestPlayer(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, minDistance, false) == null;
	}
}
