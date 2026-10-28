# Golem Garage: Player Guide

Version {VERSION} for Minecraft 1.21.1 (NeoForge). By xTrip.

Build golems from parts. Every golem has five slots (head, torso, arms, legs and core), and every
part is a **shape** made from a **material**. Iron arms look like iron and gold legs look like gold, and each
material changes how strong, fast and tough that part is. Golems never die: they break down into an
item you can repair in a Garage you build yourself, where lasers fix them while you watch.

## The in-game manual

Every player gets a **Golem Engineer's Manual** the first time they join (craft another from a book and an iron
ingot). It has everything in this guide and more: a page for every part shape and every material with its exact
numbers, live 3D golems that turn on the page, crafting grids read from the game's own recipes, Garage diagrams,
and a search box. Hover any item to see it; items with a "Click to read" line open their own entry.

## Getting started (about 10 minutes)

1. **Craft a Golem Frame.** Cobblestone in a ring with redstone in the middle and the bottom-middle left empty:
   `CCC / CRC / C C`. Place it. You get a bare metal skeleton that follows you.
2. **Craft some parts.** The cheapest are stone. For example, Stub Legs are `MRM / M M` with M = cobblestone,
   R = redstone. Right-click the frame with a part to fit it. The part appears on the golem at once.
3. **Give it a core.** A Redstone Core (`·M· / MRM / ·M·`, R = redstone block) powers the golem. Without a core it
   works, but slowly.
4. **Right-click it with an empty hand** to open its panel. The Status tab shows every part, health, power and
   what it's doing; the Rules tab is where you teach it jobs.
5. **Upgrade.** Swap any part for a better shape or material at any time; the old part comes back to you with its
   wear intact.

Press **G** while looking at a golem or cub (or shift + right-click it with an empty hand) to pick it up as an
item, with everything it carries. The key can be changed in Controls.

## How a golem is put together

| Slot | What it decides |
|---|---|
| Head | How far it sees (and spots enemies), plus a little health. Shapes add light, sealing, or long sight. |
| Torso | Most of its health and how many stacks it carries. Shapes add smelting or sealing. |
| Arms | Damage and work speed. Shapes decide what jobs it can do (harvesting needs Scythes or Claws). |
| Legs | Speed, step height and knockback resistance. Shapes add climbing, wheels or treads. |
| Core | Power capacity and how it recharges. Its color is the color its eyes and core window glow. |

An empty slot shows a bare frame and works poorly (no legs: 30% slower; no arms: weak hits).
All five parts in one material is a **matched set**: +10% speed, 10% less wear, and Garage repairs 20% faster.
