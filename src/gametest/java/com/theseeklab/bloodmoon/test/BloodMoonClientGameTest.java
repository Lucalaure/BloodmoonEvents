package com.theseeklab.bloodmoon.test;

import com.theseeklab.bloodmoon.BloodMoonEnvironment;
import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.HordeMobs;
import java.util.Set;
import java.util.TreeSet;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.block.Blocks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * End-to-end check in a real client + integrated server, comparing an early and a late Blood Moon:
 * <ul>
 *   <li>#1: red sky, no sleeping, a small horde of zombies/skeletons/spiders, zombies can build but not break</li>
 *   <li>#10: a much bigger, armored horde with creepers and the rest, which breaks into a sealed box</li>
 * </ul>
 * Run with ./gradlew runClientGameTest; screenshots land in build/run/clientGameTest/screenshots.
 */
public class BloodMoonClientGameTest implements FabricClientGameTest {
	private static final Logger LOGGER = LoggerFactory.getLogger("bloodmoonevents-test");
	private static final Set<String> LEVEL_ONE_MOBS = Set.of("minecraft:zombie", "minecraft:skeleton", "minecraft:spider");

	@Override
	public void runTest(final ClientGameTestContext context) {
		try (TestSingleplayerContext singleplayer = context.worldBuilder().create()) {
			TestServerContext server = singleplayer.getServer();
			server.runCommand("gamemode survival @a");
			server.runCommand("difficulty normal");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("effect give @a resistance infinite 255 true");
			server.runCommand("effect give @a regeneration infinite 255 true");
			server.runCommand("effect give @a saturation infinite 255 true");
			server.runCommand("time set 18000");
			server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 -35");
			BlockPos center = server.computeOnServer(s -> player(s).blockPosition());
			context.waitTicks(60);
			context.takeScreenshot("1_normal_night");

			// ---------- Blood Moon #1 ----------
			server.runCommand("bloodmoon start");
			context.waitTicks(140);
			check(server.computeOnServer(s -> BloodMoonManager.activeLevel()) == 1, "first Blood Moon is #1");
			check(BloodMoonEnvironment.clientIntensity >= 1.0F, "client sky fully faded in");
			context.takeScreenshot("2_blood_moon_1_sky");
			check(server.computeOnServer(s -> {
				BedRule rule = s.overworld().environmentAttributes().getValue(EnvironmentAttributes.BED_RULE, player(s).position());
				return !rule.canSleep(s.overworld());
			}), "sleeping is blocked during the Blood Moon");

			// Sealed in a wooden box: at #1 zombies may build but must not break anything.
			sealInBox(server);
			int shellBefore = server.computeOnServer(s -> countShell(s, center));
			waitNight(context, server, 600);
			int shellAfter = server.computeOnServer(s -> countShell(s, center));
			Set<String> rosterOne = server.computeOnServer(BloodMoonClientGameTest::hordeTypes);
			int hordeOne = server.computeOnServer(BloodMoonClientGameTest::countHorde);
			LOGGER.info("#1 after 30s: horde {} of types {}, shell {} -> {}", hordeOne, rosterOne, shellBefore, shellAfter);
			check(hordeOne > 0, "#1 horde spawned (" + hordeOne + ")");
			check(hordeOne <= 12, "#1 horde stays within its cap of 12 (" + hordeOne + ")");
			check(LEVEL_ONE_MOBS.containsAll(rosterOne), "#1 horde is only zombies, skeletons and spiders " + rosterOne);
			check(shellAfter == shellBefore, "#1 zombies did not break any blocks");
			check(server.computeOnServer(s -> countArmored(s)) == 0, "#1 horde has no extra armor");

			// Stranded on a tower: #1 zombies should still build their way up.
			resetArena(server, center);
			server.runCommand(fill(center, -1, 0, -1, 1, 4, 1, "minecraft:stone_bricks"));
			server.runCommand(String.format("tp @a %d.5 %d %d.5 0 30", center.getX(), center.getY() + 5, center.getZ()));
			int cobbleBefore = server.computeOnServer(s -> countBlock(s, center, Blocks.COBBLESTONE));
			boolean reached = false;
			int placed = 0;
			for (int i = 0; i < 24 && !reached; i++) {
				waitNight(context, server, 200);
				placed = server.computeOnServer(s -> countBlock(s, center, Blocks.COBBLESTONE)) - cobbleBefore;
				reached = server.computeOnServer(s -> {
					ServerPlayer player = player(s);
					for (Entity entity : s.overworld().getAllEntities()) {
						if (entity instanceof Zombie && HordeMobs.isHorde(entity) && entity.distanceTo(player) < 2.5 && entity.getY() > player.getY() - 1.5) {
							return true;
						}
					}
					return false;
				});
				LOGGER.info("#1 tower t={}s: {} cobblestone placed, zombie on top: {}", (i + 1) * 10, placed, reached);
			}
			context.takeScreenshot("3_blood_moon_1_tower");
			check(placed > 0, "#1 zombies placed blocks (" + placed + ")");
			check(reached, "#1 a zombie built its way up to the player");

			endAtDawn(context, server);
			check(server.computeOnServer(s -> BloodMoonManager.state(s).nextLevel()) == 2, "next Blood Moon is #2");

			// ---------- Blood Moon #10 ----------
			resetArena(server, center);
			server.runCommand("bloodmoon level 10");
			server.runCommand("bloodmoon start");
			context.waitTicks(40);
			setTimeOfDay(server, 18000);
			check(server.computeOnServer(s -> BloodMoonManager.activeLevel()) == 10, "this Blood Moon is #10");

			BlockPos center10 = server.computeOnServer(s -> player(s).blockPosition());
			sealInBox(server);
			int shell10Before = server.computeOnServer(s -> countShell(s, center10));
			int broken = 0;
			int maxHorde = 0;
			Set<String> rosterTen = new TreeSet<>();
			for (int i = 0; i < 12; i++) {
				waitNight(context, server, 200);
				broken = shell10Before - server.computeOnServer(s -> countShell(s, center10));
				int horde = server.computeOnServer(BloodMoonClientGameTest::countHorde);
				maxHorde = Math.max(maxHorde, horde);
				rosterTen.addAll(server.computeOnServer(BloodMoonClientGameTest::hordeTypes));
				LOGGER.info("#10 t={}s: horde {}, {} armored, {} shell blocks gone, types {}",
					(i + 1) * 10, horde, server.computeOnServer(s -> countArmored(s)), broken, rosterTen);
				if (i >= 3 && broken > 0 && maxHorde > 12) {
					break;
				}
			}
			context.takeScreenshot("4_blood_moon_10_siege");
			check(maxHorde > 12, "#10 horde is bigger than #1's cap (" + maxHorde + ")");
			check(!LEVEL_ONE_MOBS.containsAll(rosterTen), "#10 horde includes mobs locked at #1 " + rosterTen);
			check(server.computeOnServer(s -> countArmored(s)) > 0 || maxHorde == 0, "#10 horde includes armored mobs");
			check(broken > 0, "#10 horde broke into the box (" + broken + " blocks gone)");

			endAtDawn(context, server);
			check(server.computeOnServer(s -> BloodMoonManager.state(s).nextLevel()) == 11, "next Blood Moon is #11");
			context.waitTicks(120);
			check(BloodMoonEnvironment.clientIntensity <= 0.0F, "client sky faded back out");
		}
		LOGGER.info("All Blood Moon checks passed");
	}

	/**
	 * Flattens a 25x25 arena around {@code center} using absolute coordinates: a solid floor first, then clears the
	 * air above, and only then puts the player on the floor, so they never stand over a hole between commands.
	 */
	private static void resetArena(final TestServerContext server, final BlockPos center) {
		server.runCommand(fill(center, -12, -4, -12, 12, -2, 12, "minecraft:stone"));
		server.runCommand(fill(center, -12, -1, -12, 12, -1, 12, "minecraft:grass_block"));
		server.runCommand(fill(center, -12, 0, -12, 12, 12, 12, "minecraft:air"));
		server.runCommand(String.format("tp @a %d.5 %d %d.5 0 -35", center.getX(), center.getY(), center.getZ()));
		server.runCommand("kill @e[type=item]");
	}

	private static String fill(final BlockPos c, final int x1, final int y1, final int z1, final int x2, final int y2, final int z2, final String block) {
		return String.format("fill %d %d %d %d %d %d %s",
			c.getX() + x1, c.getY() + y1, c.getZ() + z1, c.getX() + x2, c.getY() + y2, c.getZ() + z2, block);
	}

	private static void sealInBox(final TestServerContext server) {
		server.runCommand("execute as @a at @s run fill ~-3 ~-1 ~-3 ~3 ~4 ~3 minecraft:oak_planks hollow");
		server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 -35");
	}

	/** Waits while keeping it night, so the Blood Moon doesn't end mid-test. */
	private static void waitNight(final ClientGameTestContext context, final TestServerContext server, final int ticks) {
		for (int waited = 0; waited < ticks; waited += 200) {
			context.waitTicks(Math.min(200, ticks - waited));
			setTimeOfDay(server, 18000);
		}
	}

	/** Like /time set, but keeps the current day (/time set jumps back to day 0). */
	private static void setTimeOfDay(final TestServerContext server, final int timeOfDay) {
		server.runOnServer(s -> {
			long day = BloodMoonManager.currentDay(s.overworld());
			s.clockManager().setTotalTicks(s.registryAccess().getOrThrow(WorldClocks.OVERWORLD), day * 24000L + timeOfDay);
		});
	}

	private static void endAtDawn(final ClientGameTestContext context, final TestServerContext server) {
		setTimeOfDay(server, 23500);
		context.waitTicks(40);
		check(!server.computeOnServer(s -> BloodMoonManager.isActive(s.overworld())), "Blood Moon ended at dawn");
		check(server.computeOnServer(BloodMoonClientGameTest::countHorde) == 0, "horde despawned at dawn");
	}

	private static void check(final boolean condition, final String what) {
		if (!condition) {
			throw new AssertionError("FAILED: " + what);
		}
		LOGGER.info("PASS: {}", what);
	}

	private static ServerPlayer player(final MinecraftServer server) {
		return server.getPlayerList().getPlayers().getFirst();
	}

	private static int countHorde(final MinecraftServer server) {
		int count = 0;
		for (Entity entity : server.overworld().getAllEntities()) {
			if (HordeMobs.isHorde(entity)) {
				count++;
			}
		}
		return count;
	}

	private static Set<String> hordeTypes(final MinecraftServer server) {
		Set<String> types = new TreeSet<>();
		for (Entity entity : server.overworld().getAllEntities()) {
			if (HordeMobs.isHorde(entity)) {
				types.add(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString());
			}
		}
		return types;
	}

	/** Horde mobs wearing a helmet. Our armor roll always includes one; vanilla's is near zero in a brand new world. */
	private static int countArmored(final MinecraftServer server) {
		int count = 0;
		for (Entity entity : server.overworld().getAllEntities()) {
			if (HordeMobs.isHorde(entity) && entity instanceof Mob mob && !mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
				count++;
			}
		}
		return count;
	}

	private static int countBlock(final MinecraftServer server, final BlockPos center, final Block block) {
		ServerLevel level = server.overworld();
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-16, -2, -16), center.offset(16, 12, 16))) {
			if (level.getBlockState(pos).is(block)) {
				count++;
			}
		}
		return count;
	}

	/** Planks remaining in the walls and roof of the box (the floor is ignored). */
	private static int countShell(final MinecraftServer server, final BlockPos center) {
		ServerLevel level = server.overworld();
		int count = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-3, 0, -3), center.offset(3, 4, 3))) {
			boolean shell = Math.abs(pos.getX() - center.getX()) == 3 || Math.abs(pos.getZ() - center.getZ()) == 3 || pos.getY() == center.getY() + 4;
			if (shell && level.getBlockState(pos).is(Blocks.OAK_PLANKS)) {
				count++;
			}
		}
		return count;
	}
}
