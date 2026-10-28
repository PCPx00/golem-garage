# Golem Garage: Player Guide

Version 0.9.0 for Minecraft 1.21.1 (NeoForge). By xTrip.

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


## Part shapes

Every shape can be made from any material. Numbers are for the shape itself; the material scales them.


### Head

| Shape | Stats | What it does | Recipe |
|---|---|---|---|
| Sentry Head | sight 16, health +4 | A sturdy all-round head. Good sight in every direction. | `MMM` / `MRM` / `·M·` (M = material, R = redstone) |
| Scope Head | sight 32, health +3 | A telescopic eye. Doubles sight range and spots hostiles early. | `MMM` / `MSM` / `·M·` (M = material, S = spyglass) |
| Lantern Head | sight 12, health +4 | Carries its own lantern. Lights up the ground around it as it walks. | `·L·` / `MMM` / `MRM` (M = material, L = lantern, R = redstone) |
| Visor Head | sight 14, health +6 | A sealed helmet. Rain and desert sand can't wear it down. | `MMM` / `MGM` / `·M·` (M = material, G = glass pane) |

### Torso

| Shape | Stats | What it does | Recipe |
|---|---|---|---|
| Plated Chest | health +40, carries 9 | Heavy armor plating. The most health of any torso. | `M·M` / `MRM` / `MMM` (M = material, R = redstone) |
| Cargo Barrel | health +26, carries 27 | A banded barrel body. Carries 27 stacks. | `MMM` / `MBM` / `MMM` (M = material, B = barrel) |
| Sealed Hold | health +32, carries 18 | A pressurized tank with a porthole. Carries 18 stacks and keeps rain out. | `MMM` / `MKM` / `MMM` (M = material, K = bucket) |
| Furnace Chest | health +30, carries 9 | A working furnace. Smelts ores and food in its inventory using any fuel it carries. | `MMM` / `MFM` / `MMM` (M = material, F = furnace) |

### Arms

| Shape | Stats | What it does | Recipe |
|---|---|---|---|
| Golem Fists | damage 8, work 80% | Big block fists. Hit hard and knock enemies back. | `M·M` / `M·M` / `MRM` (M = material, R = redstone) |
| Grabber Claws | damage 5, work 120% | Three-fingered claws. Best at picking up items and hauling. | `M·M` / `MCM` / `M·M` (M = material, C = chain) |
| Hammer Arms | damage 12, work 70% | Heavy hammers. The strongest hits, and they strike every enemy nearby. | `MMM` / `MMM` / `R·R` (M = material, R = redstone) |
| Scythe Arms | damage 9, work 150% | Curved blades. Harvest crops three times faster and slice through groups. | `MMM` / `·RM` / `··M` (M = material, R = redstone) |

### Legs

| Shape | Stats | What it does | Recipe |
|---|---|---|---|
| Stub Legs | speed 80%, knockback resist 60% | Short, wide legs. Slow, but very hard to knock back. | `MRM` / `M·M` (M = material, R = redstone) |
| Strider Legs | speed 110%, knockback resist 10% | Long piston legs. Fast, and step straight up full blocks. | `MPM` / `M·M` / `M·M` (M = material, P = piston) |
| Tread Legs | speed 92%, knockback resist 100% | Tank treads. Can't be knocked back, take no fall damage, and cross sand and mud. | `MMM` / `D·D` / `MMM` (M = material, D = dried kelp block) |
| Wheel Legs | speed 135%, knockback resist 20% | Big rubber wheels. The fastest legs on flat ground, but they can't jump. | `·M·` / `MDM` / `·M·` (M = material, D = dried kelp block) |
| Spider Legs | speed 100%, knockback resist 30% | Four jointed legs. Climbs straight up walls. | `M·M` / `MSM` / `M·M` (M = material, S = string) |

### Core

| Shape | Stats | What it does | Recipe |
|---|---|---|---|
| Redstone Core | power 150 | A redstone battery. Recharges at redstone blocks and burns redstone dust it carries. | `·M·` / `MRM` / `·M·` (M = material, R = redstone block) |
| Furnace Core | power 200 | Burns coal, charcoal or any fuel it carries to recharge. | `·M·` / `MCM` / `·M·` (M = material, C = coals) |
| Lightning Core | power 250 | Charges in rain and thunderstorms. A lightning strike fills it instantly. | `·M·` / `MLM` / `·M·` (M = material, L = lightning rod) |
| Solar Core | power 120 | A solar panel. Recharges in daylight under open sky. | `·M·` / `MDM` / `·M·` (M = material, D = daylight detector) |
| Soul Core | power 220 | A bound soul flame. Slowly recharges anywhere and mends the golem over time. | `·M·` / `MSM` / `·M·` (M = material, S = soul lantern) |

## Materials

Materials are matched by ingot tag, so any mod that adds the ingot works. A material only appears (in recipes and the Parts tab) when some installed mod provides it.

| Material | Tier | Ingredient | Usually from | Special |
|---|---|---|---|---|
| Stone | 0 | `#c:cobblestones` | Minecraft | durability x0.8, speed x0.9, power x0.8, work x0.9 |
| Copper | 1 | `#c:ingots/copper` | Minecraft | durability x0.9, Oxidizes, Conductive |
| Iron | 2 | `#c:ingots/iron` | Minecraft | - |
| Gold | 2 | `#c:ingots/gold` | Minecraft | durability x0.6, speed x1.08, power x1.5, work x1.3 |
| Steel | 3 | `#c:ingots/steel` | Golem Garage | durability x1.3 |
| Diamond | 4 | `#c:gems/diamond` | Minecraft | durability x1.5, Hardened |
| Netherite | 5 | `#c:ingots/netherite` | Minecraft | durability x1.4, speed x0.95, Fireproof, Hardened |
| Aluminum | 1 | `#c:ingots/aluminum` | All The Ores | durability x0.8, speed x1.1 |
| Tin | 1 | `#c:ingots/tin` | All The Ores | durability x0.85 |
| Zinc | 1 | `#c:ingots/zinc` | All The Ores | durability x0.85 |
| Lead | 2 | `#c:ingots/lead` | All The Ores | speed x0.9, Heavy |
| Nickel | 2 | `#c:ingots/nickel` | All The Ores | - |
| Silver | 2 | `#c:ingots/silver` | All The Ores | Smite |
| Bronze | 2 | `#c:ingots/bronze` | All The Ores / Silent Gear | durability x1.1 |
| Brass | 2 | `#c:ingots/brass` | All The Ores | work x1.1 |
| Constantan | 2 | `#c:ingots/constantan` | All The Ores | power x1.2 |
| Electrum | 2 | `#c:ingots/electrum` | All The Ores | power x1.4, Conductive |
| Invar | 3 | `#c:ingots/invar` | All The Ores | durability x1.25 |
| Platinum | 3 | `#c:ingots/platinum` | All The Ores | power x1.2 |
| Osmium | 3 | `#c:ingots/osmium` | All The Ores | durability x1.2, Heavy |
| Uranium | 3 | `#c:ingots/uranium` | All The Ores | Luminous |
| Signalum | 4 | `#c:ingots/signalum` | All The Ores | power x1.5, Conductive |
| Lumium | 4 | `#c:ingots/lumium` | All The Ores | Luminous |
| Iridium | 5 | `#c:ingots/iridium` | All The Ores | durability x1.3, Hardened |
| Enderium | 5 | `#c:ingots/enderium` | All The Ores | Ender |
| Allthemodium | 6 | `#c:ingots/allthemodium` | Allthemodium | durability x1.3, Fireproof |
| Vibranium | 7 | `#c:ingots/vibranium` | Allthemodium | durability x1.4, speed x1.1, Thorns |
| Unobtainium | 8 | `#c:ingots/unobtainium` | Allthemodium | durability x1.6, speed x1.15, power x1.3, Featherweight |
| Vibranium-Allthemodium Alloy | 8 | `#c:ingots/vibranium_allthemodium_alloy` | Allthemodium | durability x1.6, speed x1.1, Fireproof, Thorns |
| Unobtainium-Allthemodium Alloy | 9 | `#c:ingots/unobtainium_allthemodium_alloy` | Allthemodium | durability x1.8, speed x1.15, power x1.4, Fireproof, Featherweight |
| Unobtainium-Vibranium Alloy | 9 | `#c:ingots/unobtainium_vibranium_alloy` | Allthemodium | durability x1.8, speed x1.2, power x1.4, Thorns, Featherweight |
| Crimson Iron | 3 | `#c:ingots/crimson_iron` | Silent Gear | durability x1.1 |
| Crimson Steel | 4 | `#c:ingots/crimson_steel` | Silent Gear | durability x1.35, Fireproof |
| Azure Silver | 3 | `#c:ingots/azure_silver` | Silent Gear | Smite |
| Azure Electrum | 4 | `#c:ingots/azure_electrum` | Silent Gear | power x1.4, Conductive |
| Tyrian Steel | 5 | `#c:ingots/tyrian_steel` | Silent Gear | durability x1.5, Hardened |
| Blaze Gold | 3 | `#c:ingots/blaze_gold` | Silent Gear | power x1.3, work x1.2, Fireproof |

### Material traits

| Trait | Effect |
|---|---|
| Oxidizes | Slowly oxidizes (3% slower per stage). Wax with honeycomb; scrape with an axe. |
| Conductive | Each part adds 15% power capacity; recharging is 25% faster. |
| Hardened | 30% chance to ignore wear. |
| Fireproof | Three or more of these parts make the golem immune to fire and lava. |
| Heavy | +15% knockback resistance per part. |
| Smite | Arms of this material deal 50% more damage to undead. |
| Luminous | The golem gives off light. |
| Ender | Teleports to its owner when left more than 24 blocks behind while following. |
| Thorns | Reflects 8% of melee damage per part (up to 40%). |
| Featherweight | Legs walk on water (and lava if fireproof); no fall damage. |

### How tier scales a part

- Durability: (60 + 40 x tier) x material durability x slot (head 0.8, torso 1.2, arms 1.0, legs 1.0, core 0.9).
- Health from a part: shape health x (1 + 0.3 x tier).
- Armor: 0.8 x tier per part.
- Arm damage: shape damage + 1.5 x tier.
- Core power: shape power x (1 + 0.25 x tier) x material power.
- Head sight: shape sight + 2 x tier.

### Set bonuses

| Set | Needs | Bonus |
|---|---|---|
| Matched set | All five parts the same material | +10% speed, 10% less wear, Garage repair 20% faster |
| Garage kit | Three or more steel-family parts (Steel, Crimson Steel, Tyrian Steel) | Garage repair 20% faster |
| Desert kit | Visor Head + Sealed Hold + Tread Legs | Immune to all rain and sand wear |

## Wear, breaking and repair

Each part wears on its own. The golem's panel shows a durability bar per part, and cracks appear on
the part itself as it wears down.

- **What wears parts:** taking hits (a random part), landing hits (arms), walking (legs, 1 per 48 blocks),
  working (arms), using power (core), and being outdoors in rain or a desert (every part that isn't sealed).
- **Broken parts** stay fitted but look dark and spark. A broken head sees only 4 blocks, broken arms hit and
  work at 30%, broken legs move at half speed, a broken torso holds 9 stacks, and a broken core holds a quarter
  of its power.
- **Breakdown:** at zero health the golem breaks down into a Broken Golem item that goes to your inventory.
  Nothing is lost: parts, rules, inventory and links are all kept.

### Repairing

| How | What it does |
|---|---|
| Repair Kit (2 iron ingots + slime ball = 2 kits) | Right-click the golem: every part gets back a quarter of its durability, plus a quarter of health. |
| Repair Self rule | The golem uses Repair Kits from its own inventory. |
| Anvil | A loose part item repairs with its own material, like a tool. |
| Garage | Full repair of every part, all health, full power, and copper polished back to new. |

### Copper

Copper parts slowly turn exposed, weathered and oxidized, and get 3% slower per stage. Right-click
the golem with **honeycomb** to wax every copper part (no more aging), or with an **axe** to scrape one stage
off. A Garage repair polishes copper back to new.

## The Garage

A Garage is a sealed room you build from your own blocks, with a Garage Controller in one wall and
Laser Emitters in the ceiling. Put a broken golem in and the lasers repair it in about 2 minutes.

### Rules for the room

- **Inside:** 3 to 9 blocks wide and deep, 4 to 8 tall, fully enclosed.
- **Walls:** any planks, stone, stone bricks, bricks, copper block or deepslate bricks.
- **Floor and ceiling:** any solid block. Glowing blocks like sea lanterns in the ceiling are fine.
- **Controller:** in a side wall, touching the inside of the room.
- **Emitters:** 2 to 4 Laser or Steel Emitters set into the ceiling, above the room's air.

**Hold a Garage Controller** to see a see-through build plan: white for walls, blue-gray for floor and
ceiling, cyan for emitters, yellow for the controller, and red for anything in the way. Blocks disappear
from the plan as you place them, and a checklist in the corner counts what's left. Sneak + scroll changes
the room size (3x3, 5x5, 7x7 or 9x9). Place the controller first; the room extends the way you face.

Right-click the controller with an empty hand to check the room. It tells you what's wrong, or how
many emitters it found.

```
Top view, 5 x 5 inside:          Side view:
 W W W W W W W                     E     E     <- emitters in the ceiling
 W . . . . . W                  ================  ceiling
 C . . . . . W                    .  .  .  .  .   air, 4-8 tall
 W . . . . . W                    .  .  .  .  .
 W W W W W W W                  ================  floor
 C = controller, W = wall block
```

### Recipes

| Block | Recipe |
|---|---|
| Garage Controller | `ILI / ·R· / I·I` (I = iron ingot, L = lever, R = redstone) |
| Laser Emitter | glass pane + 2 iron ingots + redstone torch (shapeless) |
| Steel Emitter | Laser Emitter + steel ingot (shapeless). One in the ceiling repairs 20% faster. |
| Steel Ingot | 2 iron ingots + coal (shapeless) |

### Starting a repair

- **By hand:** right-click the controller holding the Broken Golem.
- **On its own:** link a golem to the controller with a Golem Linker and give it a "go to Garage" rule.
  It walks in by itself (the Medic preset does this).

**Cost:** one unit of each damaged part's own material. Iron parts need iron ingots, gold parts need gold
ingots, a stone part needs cobblestone, and so on. The Garage takes them from you, from a chest next to the
controller, or from the golem's own inventory.

**What you see:** the golem rides a lift platform up to the middle of the room and turns slowly while
every emitter fires a pulsing beam that sweeps up and down it. Sparks fly and a scanning ring climbs
its body. Then it lowers and steps off fully repaired, at full power. Breaking a wall pauses the
repair until you fix it. Breaking the controller drops the golem back as an item.

## Power

The core sets how much power the golem holds. It uses a little standing still, more while moving, and
one point per hit or task. At zero power it moves at 40% speed and its eyes dim and flicker.

| Core | Recharges by |
|---|---|
| Redstone Core | Burns redstone dust (12) or redstone blocks (100) it carries; any golem can also charge next to a placed redstone block with the Recharge rule. |
| Furnace Core | Burns any fuel it carries (coal = 80). |
| Lightning Core | Charges in rain and storms; a lightning strike fills it completely and doesn't hurt the golem. |
| Solar Core | Charges in daylight under open sky. |
| Soul Core | Slowly charges anywhere and mends the golem's health over time. |

## Behaviors: teaching a golem a job

Open the golem's panel and go to **Rules**. Each rule reads like a sentence: "When **its power is below
20%** -> **recharge at a redstone block**, unless **...**". Click any colored part to choose from a grouped
list with explanations; click a yellow number to raise it, right-click to lower it, or scroll. Arrows move
rules up and down, x deletes, and Undo puts back your last change. **Quick jobs** at the top load a
ready-made set of rules. Rules are checked top to bottom twice a second; the first one that's true runs
and is highlighted in green. If none apply, the golem follows you.

### Simple modes

Don't want rules? Click **Mode** under the golem's picture on the Status tab (or use `/golemgarage mode`):

| Mode | What it does |
|---|---|
| Rules | runs its rules (the default) |
| Aggressive | follows you and attacks every hostile mob within 12 blocks |
| Peaceful | follows you and never attacks anything, even if it's hit |
| Guard | follows you and only fights mobs that attack you |

Any mode but Rules overrides the rules completely; switch back to Rules and they run again.

### Triggers

| Trigger | True when |
|---|---|
| always | always |
| health below % / power below % | the golem is low |
| a part below % / a part is broken | any part is worn or broken |
| owner within / owner beyond | you are closer or farther than N blocks |
| owner attacked | something hurt you in the last 10 seconds |
| hostile within | a hostile mob is within N blocks (and within sight) |
| away from post | it is more than N blocks from its post |
| crop ready within | a ripe crop is within N blocks |
| chest A has / chest A below | linked chest A holds at least / fewer than N items |
| chest B full | linked chest B has no room |
| inventory full / inventory empty | the golem's own inventory |
| day / sunset / night, raining, thunderstorm | the time and weather |
| redstone on / redstone off | a redstone signal at the golem or its post |
| outside its area | it has left the work area marked with the Linker |

### Actions

| Action | Does |
|---|---|
| follow owner | walks with you |
| go to post | walks to its post and stays |
| wait N s | stands still for N seconds (a more important rule can interrupt it) |
| stroll | wanders around its post |
| attack hostile / defend owner | fights the nearest hostile, or whatever hurt you |
| take from chest A / deposit to chest B | moves items between its linked chests |
| take from chest / deposit to chest | the same with the nearest storage block within 8 blocks |
| pick up items | collects dropped items (Grabber Claws reach twice as far) |
| harvest crops | harvests ripe wheat, carrots, potatoes, beetroot, nether wart, pumpkins and melons and replants them; needs Scythe Arms (3x3 at a time) or Grabber Claws |
| recharge | walks to a redstone block and charges |
| repair self | uses a Repair Kit from its inventory |
| go to Garage | walks into its linked Garage for a full repair |
| emit redstone | powers a Golem Signal Plate it stands on |
| give to a golem | hands its items to the nearest of your golems with room (within 12 blocks) |
| patrol its area | walks between random spots inside its work area |
| rest at its dock | walks onto its Golem Dock and rests there, slowly recharging and healing |

### Presets

| Preset | Rules |
|---|---|
| Follower | defend owner when attacked; otherwise follow |
| Guard | attack hostiles within 10; defend owner; return to post; otherwise wait |
| Hauler | recharge when low; full: deposit to chest B; chest A has items: take from A; empty: go to post; otherwise deposit to B |
| Harvester | recharge when low; full: deposit to nearest chest; crops ready: harvest; otherwise pick up items |
| Medic | broken part or health below 50: go to Garage; worn part: repair self; otherwise follow |
| Night guard | recharge when low; attack hostiles within 8; at night go to post; otherwise follow |

### When a golem gets stuck

If an action can't run, the panel says why ("Stuck: no path to chest B", "chest A is empty",
"needs Scythe Arms or Grabber Claws to harvest"). If the same rule fails five times in a row, the golem stops
running rules, follows you, and shows "Stopped: ...". Editing its rules starts it again. The panel also keeps
a log of its last ten activities. Repeated work has a short pause built in, shorter for faster arms.

## Golem Linker

The Linker (`··A / ·S· / S··`, A = amethyst shard, S = stick) tells golems where things are.

1. **Shift + right-click the air** to choose a mode: Post, Chest A, Chest B, Garage, Area corner 1, Area corner 2,
   Home or Group.
2. **Right-click a block** to mark it. Any block that stores items works for Chest A and Chest B; Garage needs a
   Garage Controller; Post and the area corners mark the spot you click.
3. **Right-click a golem** to give it every mark on the Linker. Shift + right-click a golem with the Linker clears
   its links.

The panel also has "Post here" and "Clear links" buttons. While you hold a Linker, every golem's links and work
areas show as colored outlines, along with the marks the Linker is holding.

## Golem Dock and groups

- **Golem Dock** (`CRC / III`, C = copper ingot, R = redstone block, I = iron ingot): a charging pad that gives a golem
  a home. Link it with the Linker in Home mode and give the golem a "rest at its dock" rule (for example "night: rest
  at its dock"). On the dock it slowly recharges and heals, and the pad lights up.
- **Groups:** put the Linker in Group mode, shift + scroll to pick a group from 1 to 9, and right-click golems to add
  them (shift + right-click takes one out). In a grouped golem's Rules tab, "Copy to group" saves its rules and gives a
  copy to every one of your golems in that group.

## Work areas, teamwork and storage

- **Work areas:** mark two corners with the Linker (Area corner 1 and 2) and link the golem. It then only strolls,
  fights, picks up items and harvests inside that area. Pair "outside its area" with "patrol its area" to bring it
  back if it wanders.
- **Teamwork:** "give to a golem" walks to the nearest of your golems with room and hands over its items, a stack at
  a time. A harvester that never leaves its field can feed a hauler that does the walking. Golems that are giving
  items themselves are skipped, so items never bounce back and forth.
- **Any storage:** chest actions use the standard item storage of whatever block they point at: chests, barrels,
  shulker boxes, hoppers, drawers, crates, Create vaults, and storage-system interfaces from other mods.

## Upgrade modules

Every golem has **two module sockets**. Craft a Module Casing (`ICI / CRC / ICI`, I = iron ingot, C = copper ingot,
R = redstone; makes 2), then combine it with the items below. Right-click a golem with a module to install it; take
it out again from the Modules row of the golem's Status tab. A golem can't hold two of the same module, and modules
stay in the golem when you pick it up.

| Module | Casing plus | Effect |
|---|---|---|
| Magnet | compass, redstone | pulls dropped items within 6 blocks into its inventory |
| Lamp | glowstone | lights up everything around it |
| Speed | sugar, feather | moves 20% faster |
| Storage | chest | carries 9 more stacks (up to 36) |
| Plating | iron block | +6 armor, +10% knockback resistance |
| Regeneration | glistering melon slice | heals 1 health every 4 seconds |
| Self-Repair | Repair Kit, diamond | slowly mends worn parts, a little every 30 seconds |

## Golem Signal Plate

`IRI` (iron ingot, redstone, iron ingot). A plate that outputs a redstone signal while a golem
running "emit redstone" stands on it. Golems can open doors, ring bells, or tell your machines a job is done.

## Blueprints

Book + redstone + paper. Use an empty blueprint on a golem to record its five parts (shape and material) and
its rules. Use the written blueprint on any golem to rebuild it: matching parts are taken from your inventory and
fitted, the replaced parts come back to you, and the rules are copied.

## Other things you can do to a golem

| Hold | Right-click the golem to |
|---|---|
| A part | fit it (the old part comes back) |
| Repair Kit | repair a quarter of every part |
| Honeycomb / Axe | wax copper parts / scrape oxidation |
| Vines / Shears | drape it in vines like the iron golems of old / trim them |
| Name tag | name it (the name is kept when picked up and shown in its panel) |
| Golem Linker | link it to its post, chests, Garage and work area |
| Dye | paint its trim that color |
| Water bucket | wash the paint off |
| A module | install it in a free socket |
| Blueprint | record or rebuild its build |

## Cubs

Cubs are tiny chibi golems: big head, big eyes, a little leaf sprout. They follow you and carry
things, and they can't be hurt.

- **Craft:** slime ball + chest + redstone (shapeless). Comes in pink; dye it any of 16 colors by right-clicking
  with a dye.
- **Storage and wardrobe:** right-click with an empty hand to open its wardrobe and 9 pocket slots, or right-click
  while holding an item to hand it over.
- **Outfits:** right-click a cub with an outfit piece to put it on, or use the wardrobe's head, body and accessory
  slots. Three pieces of one style make a matching set, and the cub shows it off with its own sparkles. Outfits go
  back to you if the cub grows into a golem.
- **Item magnet:** it picks up items dropped within 3 blocks of it. Right-click with a feather to turn this off or on.
- **Personality:** it hops along behind you, blinks, tilts its head, sits when you crouch next to it, dozes off
  with little "z"s when you stand still for a while, and jumps around with hearts when you come back after being away.
- **Pick up:** shift + right-click with an empty hand. It keeps its items and name.
- **Growing up:** feed it three golem parts for different slots and it grows into a full golem with those parts,
  your ownership and its storage.

### Outfits

| Style | Pieces |
|---|---|
| Cool | Backwards Cap, Headphones, Cool Shades, Leather Jacket, Cozy Hoodie, Tiny Backpack |
| Goth | Witch Hat, Lace Dress, Spiked Collar, Vampire Cape, Bat Wings |
| Cute | Bunny Ears, Big Bow, Frog Hat, Flower Crown, Strawberry Dress, Bee Costume, Angel Wings, Cozy Scarf |
| Fancy | Royal Crown, Space Helmet, Dapper Tuxedo, Gold Monocle, Rocket Jetpack |

Every piece is crafted from a few everyday items (wool, leather, dyes, feathers and so on); JEI and the manual show
each recipe.

## Multiplayer

Golems and cubs belong to the player who placed them. Other players can't open, edit, link, paint or pick them up
unless the server allows it (`othersCanEdit`, `othersCanPickUp`); operators can always edit. The server can also cap
how many golems each player has out.

## Commands

`/golemgarage` (or `/gg`). Name a golem by its name, its number in the list, or `all`.

| Command | Who | What it does |
|---|---|---|
| list | everyone | your golems: where they are, health, power and what they're doing |
| find &lt;golem&gt; | everyone | where it is, how far and which way; loaded golems glow for 10 seconds |
| recall &lt;golem&gt; | everyone | brings it to you, even from unloaded areas or another dimension (60 s cooldown) |
| rename &lt;golem&gt; &lt;name&gt; | everyone | renames it |
| mode &lt;golem&gt; &lt;mode&gt; | everyone | rules, aggressive, peaceful or guard |
| stop / resume &lt;golem&gt; | everyone | pauses its jobs / starts them again |
| give &lt;players&gt; golem &lt;material&gt; [preset] | operators | a ready-built golem; presets: standard, guard, hauler, harvester, medic, night_guard |
| give &lt;players&gt; cub [color] | operators | a cub in any dye color |
| repair &lt;golems&gt; | operators | full repair, health and power |
| owner &lt;golems&gt; &lt;player&gt; | operators | hands golems to another player |
| count | operators | golems on the server, by owner |
| purge &lt;player&gt; | operators | removes all of a player's golems, even unloaded ones |
| debug &lt;golem&gt; | operators | parts, links, rules and recent activity |

## Settings

`golemgarage-server.toml` (per world, in `serverconfig`):

| Setting | Default | What it does |
|---|---|---|
| repairSeconds | 120 | how long a Garage repair takes |
| costPerPart | 1 | material the Garage charges per damaged part (0 = free) |
| wearMultiplier | 1.0 | part wear (0 turns wear off) |
| weatherWear | true | rain and desert sand wear unsealed parts |
| copperAging | true | copper parts oxidize |
| powerDrainMultiplier | 1.0 | how fast golems use power (0 = never run out) |
| maxGolemsPerPlayer | 0 | golems one player can have out (0 = no limit) |
| maxGolemsPerChunk | 0 | golems allowed in one chunk (0 = no limit) |
| workRangeFromOwner | 0 | jobs pause while the owner is offline or further away than this (0 = always work) |
| othersCanPickUp | false | other players may pick up your golems and cubs |
| othersCanEdit | false | other players may open, edit and link your golems |
| playerDamage | PVP | whether golems fight players: NEVER, PVP (only when PvP is on) or ALWAYS; never their owner |
| killsCountAsOwner | false | monsters killed by a golem drop XP and player-only loot as if the owner killed them |
| harvestNeedsMobGriefing | false | harvesting follows the mobGriefing game rule |
| itemDespawnMinutes | 0 | minutes a dropped golem or cub item lasts (0 = forever) |
| disabledActions | [] | jobs golems can't use, such as `harvest_crops` |
| disabledModules | [] | modules that can't be installed (installed ones stop working) |
| disabledShapes | [] | part shapes that can't be fitted |
| blockedMaterials | [] | materials whose parts can't be fitted |
| materialStats | [] | per-material multipliers, e.g. `"diamond;durability=2;speed=0.9"` (health, durability, speed, power, work, damage) |
| magnetRange | 3 | how far a cub's item magnet reaches (0 = off) |
| maxCubsPerPlayer | 0 | cubs one player can have out (0 = no limit) |
| giveManualOnJoin | true | new players get the manual |
| playerCommands | true | players can use the /golemgarage player commands |
| recallCooldownSeconds | 60 | wait between recalls (operators skip it) |

`golemgarage-client.toml`: `showGaragePlan` (the Garage build plan), `showStatusIcons` (icons over golems that are
stopped, stuck, out of power or have a broken part) and `golemVolume` (0 to 100: golem and cub sound volume, on top of
the Friendly Creatures slider).

## For modpack makers

- **Datapack materials:** a file at `data/<pack>/golem_material/<id>.json` adds a golem material: an item tag, a tier,
  a built-in material to borrow the look from, a tint color, and optional stat multipliers and traits. Every part shape
  can then be crafted from it with the usual patterns, and the Garage repairs it with the same item. See
  `docs/examples/mythril_pack` for a working example.
- **Item tags:** `golemgarage:golem_parts` (and `golem_parts/head`, `torso`, `arms`, `legs`, `core`), `modules`,
  `outfits` (and `outfits/cool`, `goth`, `cute`, `fancy`), `golems`, `cubs`, `garage_blocks` and `tools`.
- **Quest rewards:** `/golemgarage give <player> golem <material> <preset>` hands out a ready-built golem.

## Sounds, advancements and helper mods

- **Sounds:** soft footsteps (feet, wheels or treads), a quiet whirr while working, gentle beeps, and hurt, attack and
  breakdown sounds; the Garage hums, zaps and chimes; cubs chirp. Golem sounds fade out within 12 blocks and get
  quieter when many golems are close together, so a busy base stays calm. All have subtitles.
- **Advancements:** a Golem Garage tab with 19 goals, from placing your first golem to dressing a cub in a matching
  set and having a golem take down a Warden.
- **JEI:** part materials, golem builds and cub colors each show separately, every item has an info page, and the
  Garage Controller shows Garage Repair recipes.
- **Jade:** look at a golem for its power, what it's doing and its most worn part; look at a cub for its color and
  outfit.

## Optional mods

Nothing extra is needed. When these are installed, their metals become golem materials automatically:

- **All The Ores** (and any mod with the same ingot tags): aluminum, tin, zinc, lead, nickel, silver, bronze, brass,
  constantan, electrum, invar, platinum, osmium, uranium, signalum, lumium, iridium, enderium.
- **Allthemodium:** allthemodium, vibranium, unobtainium and their three alloys, the strongest materials.
- **Silent Gear:** crimson iron, crimson steel, azure silver, azure electrum, tyrian steel, blaze gold, bronze.

A golem with parts from a mod that is later removed keeps them; they work as iron until the mod returns.

## Messages you may see

| Message | Meaning |
|---|---|
| Your golem broke down. Repair it in a Garage. | It reached zero health. |
| Your golem's ... broke. | One part reached zero durability. |
| The Garage needs 2 more Iron Ingot | Bring those materials (inventory, adjacent chest or the golem's inventory). |
| This golem belongs to someone else. | Only its owner (or an operator) can change or pick it up. |
| You already have N golems out | The server limits golems per player; pick one up first. |
| The Garage isn't built correctly. | Right-click the controller empty-handed to see what's wrong. |
| Stuck: ... / Stopped: ... | A rule can't run; the reason follows. |
| Cub is growing: 2 / 3 | Feed it one more part for a different slot. |

