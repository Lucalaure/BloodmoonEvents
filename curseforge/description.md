# Bloodmoon Events

**Every 7th night, a Blood Moon rises, and the horde comes for you.**

The sky turns red, the moon swells to a crimson giant, and nobody can sleep. Until dawn, waves of monsters converge on every player. These mobs don't stop at your walls: zombies build towers and bridges to reach you, creepers blow holes in your defences, and slimes leap over your fences. Every Blood Moon you survive makes the next one worse.

## The Blood Moon

- Starts on **night 7**, then every 7th night after. The interval is configurable, and you can add a random chance for other nights.
- You get a warning at sunset, a **"Blood Moon #N"** title and raid horn when it starts, and a message at dawn.
- The sky, fog, clouds and light fade to blood red, and the moon is larger, red and always full.
- **You can't sleep through it.**

## The Horde

- Waves spawn around every survival player, and in caves too if you're hiding underground.
- Horde mobs **track you through walls** and despawn at dawn.

### Smarter mobs

- **Zombies build their way to you.** They:
  - pillar up to players above them.
  - climb over walls.
  - build steps.
  - bridge gaps.
- **From the 3rd Blood Moon, zombies also break blocks.** They tunnel through walls, doors and ceilings. Harder blocks take longer, and several zombies on one block break it faster.
- **Creepers breach walls.** If they can't reach you, they light their fuse once they're close enough.
- **Slimes super-jump** over walls and lunge at you.
- Respects the `mob_griefing` gamerule. Chests and other containers, and obsidian-tier blocks, are never broken.

## It gets harder

Mob health, damage and speed never change. What grows is how many mobs come, what they are, what they wear and what they can do.

| | Blood Moon #1 | #5 | #10+ |
|---|---|---|---|
| Max horde per player | 12 | 29 | 50 |
| Mobs per wave | 2 | 4 | 6 |
| Time between waves | 8 s | 6 s | 4 s |
| Chance of extra armour | 0% | 27% | 60% |

**What unlocks when:**
- **#1:** zombies, skeletons and spiders. Zombies place blocks.
- **#3:** creepers, husks, zombie villagers and slimes join. Zombies break blocks and creepers breach walls.
- **#4:** slimes super-jump.
- **#5:** witches and cave spiders join. The horde senses you from further away.

The armour mobs wear never drops, so you can't farm a Blood Moon for iron.

## Commands

- `/bloodmoon` (anyone): shows the next Blood Moon's number and how many nights until it rises.
- `/bloodmoon start` / `stop` (operators): starts a Blood Moon now, or cancels tonight's.
- `/bloodmoon level <n>` (operators): jumps to a given Blood Moon number.
- `/bloodmoon reload` (operators): reloads the config.

## Configuration

Everything is in `config/bloodmoonevents.json`:
- the schedule
- the horde roster and spawn weights
- every difficulty value (start and end)
- the Blood Moon each ability unlocks on
- toggles for each AI behaviour

## Compatibility

**[RPG Advanced Difficulty](https://github.com/Lucalaure/rpgAdvancedDifficulty)** (optional): horde mobs are more likely to spawn as champions. The odds go from ×1.5 on the first Blood Moon to ×4 on the 10th, and later Blood Moons make high-tier champions proportionally more common. Neither mod requires the other.

## Requirements

- Minecraft 26.3, Fabric Loader, **Fabric API**
- Install on **both client and server**. The server runs the event and the horde, and the client draws the red sky and moon.

Inspired by *Seventh Night: Blood Moon*, *Enhanced Celestials* and *ZombieBreakAndBuild*.

Source and issues: https://github.com/Lucalaure/BloodmoonEvents
