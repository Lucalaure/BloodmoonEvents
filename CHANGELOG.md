# Changelog

## 1.3.0 (Minecraft 26.3), 2026-11-20

### Added
- **In-game config screen.** With Mod Menu and Cloth Config installed, open Mods → Bloodmoon Events → the config button to edit every option, sorted into tabs with a tooltip and a reset button for each. Saving applies the changes straight away, no `/bloodmoon reload` needed.
  - The horde mob list is edited as one line per mob, such as `minecraft:zombie 40 1` (mob, weight, first Blood Moon).
  - Both mods are optional. Without Cloth Config, the button explains what to install.
  - The screen edits the config on your own computer. On a dedicated server, edit the server's `config/bloodmoonevents.json` as before.

## 1.2.0 (Minecraft 26.3), 2026-11-18

### Added
- **Optional: blocks the horde breaks come back after the Blood Moon.** It's off by default, so the horde's griefing stays permanent. Turn on `restoreBrokenBlocks` in the config to protect builds. When it's on, a few seconds after dawn, once the horde cobblestone has crumbled, every block zombies dug out or horde creepers blew up reappears, a few at a time and lowest first. Blocks in chunks that weren't loaded at dawn come back when the chunk loads, and restarts don't lose them.
  - Those blocks drop nothing when the horde breaks them, so a Blood Moon can't be farmed.
  - Doors and beds come back whole. Signs and banners keep their text and patterns.
  - Containers come back empty, because their contents spill out when they break, as before. Shulker boxes drop normally with their contents and aren't restored.
  - Only blocks that were there before the Blood Moon started come back. Blocks players place during the night, such as emergency walls and pillars, break and drop like normal blocks.
  - If you built over a hole before dawn, your block stays and the original drops as an item instead. A block won't reappear where a player or mob is standing; it waits until the spot is clear.
- New config options:
  - `restoreBrokenBlocks` (default `false`). Set it to `true` to turn the feature on. While it's off, `dropBrokenBlocks` decides drops as before.
  - `restoreOnlyPreexistingBlocks` (default `true`). Only matters when restoring is on. Set it to `false` to also restore blocks players placed during the Blood Moon.

## 1.1.0 (Minecraft 26.3), 2026-11-05

### Added
- **Two advancements**, granted when a Blood Moon you took part in ends at dawn and you didn't die during it:
  - **Seventh Night:** survive a Blood Moon.
  - **Crimson Dawn** (challenge, 100 XP): survive a Blood Moon at the highest difficulty (#10 by default, or your `maxLevel`).
  - Creative and spectator players can't earn them, and a Blood Moon ended with `/bloodmoon stop` doesn't count.

### Changed
- **Zombies now build with horde cobblestone instead of real cobblestone.** It's a darker, blood-stained cobblestone that breaks just like cobblestone, but it drops nothing and has no item, so it can't be collected in survival or creative. After the Blood Moon ends, every block the horde placed crumbles away within a few seconds, so the horde never leaves permanent pillars or bridges in your world. Blocks in chunks that weren't loaded at dawn crumble as soon as the chunk loads again.
- The `buildBlock` config option is gone, since the horde always builds with horde cobblestone now. Old configs that still have it load fine.

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
