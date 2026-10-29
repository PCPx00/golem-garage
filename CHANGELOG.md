# Changelog

Golem Garage for Minecraft 1.21.1 (NeoForge). By xTrip.

## 0.12.0

### Added
- **The Garage transforms when it's complete**, like a formed multiblock. Rusty panels sweep across every wall,
  floor and ceiling face, starting at the controller, and the blocks you built with disappear behind a
  wasteland workshop:
  - Inside: rusty corrugated sheets, grimy brick and bolted plates, a hazard-striped kick plate, a dark plank
    roof with a hoist beam over the lift, and an oily concrete floor with drain grates.
  - Outside: a weathered metal shell with hazard bands, a rusty roof lined with spikes, and a smoking exhaust
    stack (it smokes harder during a repair).
  - Props: a workbench with a vice, torch and lantern, a stack of tyres, oil drums, jerry cans and loose chains.
  - Sparks and smoke while it transforms, and the clank of it being bolted together.
  - Windows, doors, emitters and the controller keep their own look. Break any block of the room and it goes
    straight back to how you built it.
- The outside sign is now fire-orange neon on a rusty scrap-metal board.

## 0.11.0

### Changed
- **New Garage repair show:** the emitters now fire crackling, forking laser bolts in short bursts, taking turns,
  each in its own colour (red-orange, electric blue, hot pink, amber). Steel Emitters fire fatter bolts.
  - The lens glows as it charges, and each hit flashes, with sparks, crits and glowing embers spraying off.
  - Smoke drifts up off the golem, which shudders with every shot.
  - The golem hangs from two hoist chains while it's on the lift.
  - Each emitter makes its laser sound when it fires.
- A finished Garage has four lanterns hanging on chains from the roof, gently swaying.

## 0.10.0

### Added
- **The Garage looks like a real garage once it's complete:**
  - A neon **GOLEM GARAGE** sign flickers on outside, above the controller. It shows OPEN, or the repair's
    progress while it works.
  - A status panel inside, above the controller, shows READY or a progress bar.
  - Yellow bay lines on the floor and a hazard-striped ring around the lift pad.
  - The emitters glow brighter.
  - Amber warning lights spin inside and on the sign during a repair.
- **Garage Door:** a roller door. Place several side by side and stacked to make one big door that opens and
  closes together. It rolls up by itself for golems and cubs, with redstone, or when you right-click it
  (right-click again to let it close). It counts as Garage wall, open or shut. Iron, an iron trapdoor and
  redstone make 4.
- **Garage Window:** steel-framed glass so you can watch the repairs. Four glass and an iron nugget make 4.
- Any glass block or pane now works in Garage walls, and glass works in the roof for a skylight. Concrete,
  smooth stone, polished andesite and iron blocks also work as walls now.
- The build plan marks a doorway in the back wall (orange) for a Garage Door.

### Fixed
- Cubs no longer bunch up in one spot. Each cub keeps its own spot around you, wanders a little when you
  stand still, and bumps into other cubs instead of passing through them. When they teleport to catch up,
  they land on a free spot near you instead of all on top of you. They still never push players.

## 0.9.0

### Added
- **Simple modes** for when you don't want to write rules. Click **Mode** on the golem's Status tab, or use
  `/golemgarage mode <golem> <mode>`:
  - **Aggressive:** follows you and attacks every hostile mob within 12 blocks.
  - **Peaceful:** follows you and never attacks anything, even if it's hit.
  - **Guard:** follows you and only fights mobs that attack you.
  - **Rules:** runs its rules (the default).
  Any mode but Rules overrides the rules completely; switch back and they run again. The mode shows in Jade.
- **golemVolume** client setting (0 to 100) to turn golem and cub sounds down further.

### Changed
- **Softer, calmer golem sounds:** gentle thuds instead of clanks, rounder beeps, a quiet whirr while working, and
  lower volumes overall. Golem sounds now fade out within 12 blocks.
- **Crowds stay quiet:** when lots of golems are close together, each sound plays quieter, and repeats are skipped
  once a few nearby golems have just made the same kind of sound. Ambient beeps come less often.

## 0.8.0

### Added
- **Commands:** `/golemgarage` (or `/gg`). Pick a golem by its name, its number in the list, or `all`.
  - For everyone: `list` (where your golems are, their health, power and what they're doing), `find` (distance
    and direction; the golem glows for 10 seconds), `recall` (brings a golem to you, even from unloaded areas
    or another dimension), `rename`, `stop` and `resume`.
  - For operators: `give` (ready-built golems by material and preset, cubs by color), `repair`, `owner` (hand
    golems to another player), `count`, `purge` (remove a player's golems, even unloaded ones) and `debug`.
- **Golem Dock:** a craftable charging pad. Link it with the Golem Linker's new Home mode and use the new
  "rest at its dock" job: the golem walks home, slowly recharges and heals, and the pad lights up.
- **Groups:** the Linker's new Group mode. Shift + scroll picks a group from 1 to 9, and right-clicking golems
  adds them. "Copy to group" in the Rules tab gives every golem in the group the same rules. The group shows
  in the golem's panel, in Jade and in `/golemgarage list`.
- **Custom materials from datapacks:** a small file adds a new golem material with its own tint, stats and
  traits. Every part can be crafted from it and the Garage repairs it. An example pack is included.
- **Item tags** for parts (and each part slot), modules, outfits (and each style), golems, cubs, Garage blocks
  and tools, for modpack recipes and quests.
- **New settings:**
  - Most golems per chunk.
  - Golems pause their jobs while their owner is offline or far away.
  - Whether golems fight players: never, only with PvP on, or always. They never fight their owner.
  - Golem kills can drop XP and player-only loot as if the owner got the kill.
  - How long dropped golems and cubs last on the ground (forever by default).
  - Turn off individual jobs, modules or part shapes.
  - Block materials, or change health, durability, speed, power, work speed and damage per material.
  - Cub magnet range and most cubs per player.
  - Turn the player commands on or off, and set the recall cooldown.

### Changed
- Farms run faster: golems look for crops, chests and redstone blocks less often, which cut server time by
  about a fifth in a test with 60 golems.
- The manual has new pages for commands, the dock, groups, modpack support and every new setting.

## 0.7.0

### Changed
- **Renamed to Golem Garage** (it was Modular Golems). Golems and items from older worlds don't carry over.

### Added
- **Cub outfits:** 24 craftable pieces in four styles (cool, goth, cute and fancy) for the head, body and
  accessory slots. Right-click a cub with a piece or use its new wardrobe screen. Three pieces of one style make
  a matching set with its own sparkles.
- **Upgrade modules:** Magnet, Lamp, Speed, Storage, Plating, Regeneration and Self-Repair, with two sockets per
  golem. Take them out from the golem's panel.
- **Work areas:** mark two corners with the Linker and golems stay inside them. New "outside its area" rule and
  "patrol its area" job.
- **Teamwork:** the "give to a golem" job passes items to another of your golems.
- **Modded storage:** chest jobs and Garage payments work with any storage block, including drawers, crates and
  storage-system interfaces.
- **Paint:** right-click a golem with a dye; a water bucket washes it off.
- **Animations:** idle fidgets, a reach when working, and a slump before breaking down.
- **Status icons** over golems that are stopped, stuck, out of power, low on power or have a broken part.
- **Sounds** with subtitles: footsteps by leg type, work, hurt, attack, breakdown, the Garage, and cubs.
- **Advancements:** a Golem Garage tab with 19 goals.
- **Settings files** for servers and players.
- **JEI and Jade support** (both optional): Garage Repair recipes, info pages, and golem and cub details.
- Holding a Linker shows every golem's links and areas as outlines.
- Everything on screen can now be translated.

### Fixed
- In multiplayer, other players could paint, link and fit parts on your golems. Now only the owner can, unless
  the server allows it.

## 0.6.0

### Added
- **Pick-up hotkey:** press G while looking at a golem or cub to pick it up (rebindable in Controls).
- **Garage build plan:** holding a Garage Controller shows a see-through plan of the room and a checklist of
  what's left. Sneak + scroll picks the room size.

### Changed
- **Friendlier Rules tab:** each rule reads like a sentence. Click a part to pick from a described list, move
  or delete rules, add new ones in two steps, load Quick jobs, and undo changes.
- **New item art:** every part, golem and cub icon is a small 3D render of the real model.
- Picking up a golem or cub only works on your own, or on ones nobody owns.

## 0.5.0

### Added
- **Golem Engineer's Manual:** a craftable in-game guide book, given to every player on their first join. It
  covers every part, material, job and block, with 3D golem previews, crafting grids and a search box.

## 0.4.0

### Added
- Parts look like what they're made of: 22 part shapes in 37 materials, each with its own look.
- Shapes change the body: spider legs, wheels, treads, scythes, claws, hammers, lanterns, furnaces and more.
- Per-part wear with cracks, copper aging (wax it with honeycomb), and vines.
- Rules: up to 8 "when / do / unless" rules per golem, with 6 ready-made jobs.
- The Golem Linker, harvesting and replanting, smelting, recharging, self-repair, walking to the Garage, and
  the Golem Signal Plate.
- Cubs: 16 colors, an item magnet, 9 slots, and they grow into a golem after three parts.
- Extra materials from All The Ores, Allthemodium and Silent Gear when those mods are installed.
