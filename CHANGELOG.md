# Changelog

## Unreleased

### Changed
- **Zombies now build with horde cobblestone instead of real cobblestone.** It's a darker, blood-stained cobblestone that breaks just like cobblestone, but it drops nothing and has no item, so it can't be collected in survival or creative. After the Blood Moon ends, every block the horde placed crumbles away within a few seconds, so the horde never leaves permanent pillars or bridges in your world. Blocks in chunks that weren't loaded at dawn crumble as soon as the chunk loads again.
- The `buildBlock` config option is gone, since the horde always builds with horde cobblestone now. Old configs that still have it load fine.

## 1.0.1 (Minecraft 1.21.1), 2026-10-28

The first release for Minecraft 1.21.1, ported from 1.0.1 for 26.3. It has the same features, config and commands, and the same config file works on both.

### Differences from the 26.3 version
- The full moon during a Blood Moon is visual only. On 26.3 the server also treats the night as a full moon, which slightly raises local difficulty and swamp slime spawns. On 1.21.1 those follow the real moon phase.

## 1.0.1 (Minecraft 26.3), 2026-10-06

### Fixed
- **Horde mobs no longer delete your items at dawn.** They still vanish at sunrise, so you can't be spawn-camped after the Blood Moon ends. But first they drop everything they picked up (such as your gear after you died) where they stand. Those items get a 10-minute despawn timer instead of 5, so you have time to get back to them.
- The armour horde mobs spawn with still never drops, so a Blood Moon can't be farmed for iron.
- Horde mobs in chunks that weren't loaded at dawn are now handled the same way when the chunk loads again. Before, they were deleted along with anything they carried.
- With `despawnAtDawn` set to `false`, surviving horde mobs now become ordinary mobs that can despawn naturally. Before, they stayed in the world forever. Mobs holding picked-up items stay, same as vanilla.

### Changed
- New mod icon.

## 1.0.0 (Minecraft 26.3), 2026-10-02

The first release.

### Added
- **Blood Moons:** they start on night 7, then come every 7th night. You can change the interval and add a random chance for other nights.
  - There's a warning at sunset, a "Blood Moon #N" title and raid horn at the start, and a message at dawn.
  - The sky, fog, clouds and light turn red, and the moon is larger, red and always full.
  - Sleeping is blocked.
- **The horde:** waves of mobs spawn around every survival player until dawn, and in caves too if the player is underground. Horde mobs track players through walls.
- **Smarter mobs:**
  - Zombies pillar up, climb walls, build steps and bridge gaps. From Blood Moon #3 they also break blocks.
  - Creepers blow through walls they can't get past.
  - Slimes super-jump.
  - All of this respects `mob_griefing`, and containers and obsidian-tier blocks are never broken.
- **Difficulty that grows with each Blood Moon**, up to #10. The horde gets bigger, waves come faster, new mobs and abilities unlock, and mobs get more armour. Mob health, damage and speed never change.
- **Commands:** `/bloodmoon` for anyone, plus `start`, `stop`, `level <n>` and `reload` for operators.
- **Config:** everything lives in `config/bloodmoonevents.json`.
- **Optional RPG Advanced Difficulty compatibility:** horde mobs are more likely to be champions, and higher tiers become proportionally more common on later Blood Moons.
