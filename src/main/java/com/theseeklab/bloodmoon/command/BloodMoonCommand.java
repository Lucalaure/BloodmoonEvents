package com.theseeklab.bloodmoon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.theseeklab.bloodmoon.BloodMoonManager;
import com.theseeklab.bloodmoon.config.BloodMoonConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * /bloodmoon status   - anyone: is it active, how many nights until the next one
 * /bloodmoon start    - ops: start a Blood Moon now (skips to nightfall if needed)
 * /bloodmoon stop     - ops: end the current Blood Moon / cancel tonight's
 * /bloodmoon level N  - ops: set the number (difficulty level) of the next or current Blood Moon
 * /bloodmoon reload   - ops: reload config/bloodmoonevents.json
 */
public final class BloodMoonCommand {
	private BloodMoonCommand() {
	}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("bloodmoon")
			.executes(c -> status(c.getSource()))
			.then(Commands.literal("status").executes(c -> status(c.getSource())))
			.then(Commands.literal("start")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> {
					BloodMoonManager.forceStart(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal("Summoning a Blood Moon..."), true);
					return 1;
				}))
			.then(Commands.literal("stop")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> {
					BloodMoonManager.forceStop(c.getSource().getServer());
					c.getSource().sendSuccess(() -> Component.literal("Tonight's Blood Moon has been cancelled."), true);
					return 1;
				}))
			.then(Commands.literal("level")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("level", IntegerArgumentType.integer(1))
					.executes(c -> {
						int level = IntegerArgumentType.getInteger(c, "level");
						BloodMoonManager.setLevel(c.getSource().getServer(), level);
						c.getSource().sendSuccess(() -> Component.literal(
							(BloodMoonManager.isActive() ? "This" : "The next") + " Blood Moon is now #" + level + describeCap(level)
						), true);
						return level;
					})))
			.then(Commands.literal("reload")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.executes(c -> {
					BloodMoonConfig.load();
					c.getSource().sendSuccess(() -> Component.literal("Blood Moon config reloaded."), true);
					return 1;
				})));
	}

	private static int status(final CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		if (BloodMoonManager.isActive()) {
			int level = BloodMoonManager.activeLevel();
			source.sendSuccess(() -> Component.literal("Blood Moon #" + level + " is active right now!" + describeCap(level)), false);
			return 1;
		}
		int level = BloodMoonManager.state(server).nextLevel();
		int nights = BloodMoonManager.nightsUntilNext(server);
		String when = switch (nights) {
			case -1 -> "No Blood Moon is scheduled.";
			case 0 -> "Blood Moon #" + level + " rises tonight.";
			case 1 -> "Blood Moon #" + level + " rises tomorrow night.";
			default -> "Blood Moon #" + level + " rises in " + nights + " nights.";
		};
		source.sendSuccess(() -> Component.literal(when + describeCap(level)), false);
		return 1;
	}

	private static String describeCap(final int level) {
		int max = BloodMoonConfig.get().maxLevel;
		return level >= max ? " (maximum difficulty)" : " (difficulty " + level + "/" + max + ")";
	}
}
