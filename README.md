# Bloodmoon Events

A Fabric mod for **Minecraft 26.3**. Every 7th night (configurable) a **Blood Moon** rises. The sky, fog, clouds and light turn red, the moon swells and turns crimson, and nobody can sleep. A horde of mobs with upgraded AI also lays siege to every player until dawn.

Inspired by [Seventh Night: Blood Moon](https://www.curseforge.com/minecraft/mc-mods/seventh-night-blood-moon), [Enhanced Celestials](https://github.com/CorgiTaco-MC/Enhanced-Celestials) and [ZombieBreakAndBuild](https://github.com/XTiK555/ZombieBreakAndBuild).

## Features

- **Schedule:** every Nth night, plus an optional random chance on other nights. You get a warning at sunset, a title and raid horn when it starts, and a message at dawn.
- **Atmosphere:** red sky, fog, clouds and sky light, fading in and out over 5 seconds. The moon is red, larger and always full. Beds don't work.
- **Horde:** waves spawn in a ring 24–44 blocks around each survival player, in caves too if the player is underground. Horde mobs track players through walls and despawn in a puff of smoke at dawn.
- **Zombies** (and husks, zombie villagers, drowned) build their way to you:
  - pillar up to players above them.
  - climb over walls.
  - build steps up to higher ground.
  - bridge gaps.
  - later they also break walls, doors and ceilings and dig down. Harder blocks take longer, and several zombies on one block stack their progress.
- **Creepers** that can't path to you ignite once they're within 8 blocks.
- **Slimes and magma cubes** jump about 1.8× higher, twice as often, and lunge toward their target.
- Respects the `mob_griefing` gamerule. Containers (chests, barrels…) and obsidian-tier blocks are never broken.

## Difficulty progression

Every Blood Moon that lasts until dawn makes the next one harder. Player deaths don't matter. Mob health, damage and speed never scale; what grows is the horde's size, its roster, its armour and its abilities. Values climb evenly from #1 to #10 (`maxLevel`) and then stay there.

| | #1 | #3 | #5 | #10+ |
|---|---|---|---|---|
| Max horde per player | 12 | 20 | 29 | 50 |
| Mobs per wave | 2 | 3 | 4 | 6 |
| Time between waves | 8 s | 7 s | 6 s | 4 s |
| Time for a zombie to break a plank | — | ~5 s | ~4 s | ~2 s |
| Chance of extra armour | 0% | 13% | 27% | 60% |

| From # | New mobs | New abilities |
|---|---|---|
| 1 | Zombies, skeletons, spiders | Zombies place blocks (pillar, climb walls, steps, bridges) |
| 3 | Creepers, husks, zombie villagers, slimes | Zombies break blocks; creepers blast through walls |
| 4 | — | Slime super jump |
| 5 | Witches, cave spiders | Tracking range 40 → 64 blocks |

Armour shows up from #2 onward. It starts as leather and chainmail, with iron mixed in toward #10, and it never drops. The start title shows the number ("Blood Moon #4"), and chat lists what's new that night.

At one Blood Moon every 7 nights, #10 lands on night 70. That's about 23 hours of play, or about 15 if you sleep through every normal night.

## Commands

| Command | Permission | Effect |
|---|---|---|
| `/bloodmoon` or `/bloodmoon status` | anyone | Shows whether it's active and the nights until the next one |
| `/bloodmoon start` | op | Starts a Blood Moon now, skipping to nightfall if it's day |
| `/bloodmoon stop` | op | Ends or cancels tonight's Blood Moon (doesn't count toward the level) |
| `/bloodmoon level <n>` | op | Sets the number of the next or current Blood Moon, for testing or tuning |
| `/bloodmoon reload` | op | Reloads `config/bloodmoonevents.json` |

## Config

`config/bloodmoonevents.json` is created on first launch. Every option is commented in [BloodMoonConfig.java](src/main/java/com/theseeklab/bloodmoon/config/BloodMoonConfig.java). The main ones are:
- `intervalNights`, `firstNight`, `randomChance`
- `maxLevel`, plus `{start, end}` pairs for `maxHordePerPlayer`, `mobsPerWave`, `waveIntervalTicks`, `breakTicksPerHardness` and `armorChance`
- `...FromLevel`: the Blood Moon number each ability unlocks on
- `hordeSpawns`: a list of `{id, weight, fromLevel}`
- `builderMobs`, `buildBlock`, `maxBreakableHardness`
- `slimeJumpMultiplier`, `creeperBreachDistance`
- `onlyHordeMobsUpgraded`: set it to false to give the AI to every mob during a Blood Moon

## Compatibility

**[RPG Advanced Difficulty](https://github.com/Lucalaure/rpgAdvancedDifficulty)** (optional): when it's installed, Blood Moon horde mobs are more likely to spawn as champions, and later Blood Moons favour higher tiers:

| | Blood Moon #1 | #5 | #10 |
| --- | --- | --- | --- |
| Champion odds (`championChanceMultiplier`) | ×1.5 | ×2.6 | ×4 |
| Extra per tier above 1 (`championTierBonus`) | ×1 | ×1.16 | ×1.35 |

This works through entity tags (`rpgadvanceddifficulty.champion_chance.<n>` and `rpgadvanceddifficulty.champion_tier_bonus.<n>`), so neither mod needs the other to build or run. Only horde mobs are affected.

## Building

Requires JDK 25.

```bash
./gradlew build                # jar in build/libs/
./gradlew runClient            # play in a dev client
./gradlew runClientGameTest    # automated end-to-end test (opens a game window, ~90 s)
```

The automated test lives in `src/gametest`. It plays Blood Moon #1 and #10 in a real client and checks each step:
- **#1:** red sky, no sleeping, a small horde of only zombies, skeletons and spiders. Zombies leave a sealed box intact but build up a tower to reach the player.
- **#10:** a horde several times larger, with armoured mobs and the full roster, breaks into the box.
- **Both:** everything ends at dawn and the counter advances.

Screenshots are saved to `build/run/clientGameTest/screenshots/`.

## How it works

| Piece | Where |
|---|---|
| Scheduling, levels, announcements, client sync, dawn cleanup | `BloodMoonManager` |
| "What's new tonight" messages | `BloodMoonLevels` |
| Horde wave spawning | `HordeSpawner` |
| Tagging horde mobs, armour, attaching AI goals on load | `HordeMobs` |
| Break/pillar/bridge AI | `ai/BreakAndBuildGoal`, `ai/StuckTracker`, `ai/BlockDamageTracker` |
| Creeper breach | `ai/CreeperBreachGoal` |
| Horde targeting through walls | `ai/HordeTargetGoal` |
| Red sky, fog, light, full moon, no sleeping | `BloodMoonEnvironment` + `mixin/EnvironmentAttributeSystemBuilderMixin` |
| Red, enlarged moon | `client/mixin/SkyRendererMixin` |
| Slime jumps | `mixin/AbstractCubeMobMixin` |

Since 26.x, Minecraft drives the sky colour, fog, moon phase and bed rules through **environment attributes**. The mod adds its own layer to that system for the overworld instead of patching renderers, so the same code covers both the client visuals and the server's no-sleep rule.
