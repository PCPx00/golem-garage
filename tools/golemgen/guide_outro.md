
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

1. **Shift + right-click the air** to choose a mode: Post, Chest A, Chest B, Garage, Area corner 1 or Area corner 2.
2. **Right-click a block** to mark it. Any block that stores items works for Chest A and Chest B; Garage needs a
   Garage Controller; Post and the area corners mark the spot you click.
3. **Right-click a golem** to give it every mark on the Linker. Shift + right-click a golem with the Linker clears
   its links.

The panel also has "Post here" and "Clear links" buttons. While you hold a Linker, every golem's links and work
areas show as colored outlines, along with the marks the Linker is holding.

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
| othersCanPickUp | false | other players may pick up your golems and cubs |
| othersCanEdit | false | other players may open, edit and link your golems |
| harvestNeedsMobGriefing | false | harvesting follows the mobGriefing game rule |
| giveManualOnJoin | true | new players get the manual |

`golemgarage-client.toml`: `showGaragePlan` (the Garage build plan) and `showStatusIcons` (icons over golems that are
stopped, stuck, out of power or have a broken part).

## Sounds, advancements and helper mods

- **Sounds:** golems have their own footsteps (feet, wheels or treads), work clanks, hurt and attack sounds, a power-low
  beep and a breakdown sound; the Garage hums, zaps and chimes; cubs chirp. All have subtitles.
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
