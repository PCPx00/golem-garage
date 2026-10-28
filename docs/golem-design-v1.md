# Modular Golems, Next Generation: Design Pack

Three parts: v1 scope, parts table, and behavior system.

---

## 1. v1 Scope

### Goal
Ship a golem that is fun to assemble on day one and has enough depth to keep players building for a long time. v1 proves the core loop: build, deploy, watch, repair, upgrade.

### In v1
- **Golem assembly.** Five slots: head, torso, arms, legs, core. Each slot holds one part. Parts change stats and appearance.
- **Stats.** Health, speed, carry capacity, work rate, and power draw. Derived from the equipped parts.
- **Wear and repair.** Parts degrade over time. Repair kits restore them. Weather affects wear (rain, cold, sand).
- **Basic behaviors.** Follow owner, guard area, haul items between two chests, and harvest crops.
- **Behavior builder (simple).** A card stack of up to 8 rules. Each rule is one trigger plus one action. See section 3.
- **Blueprints.** Save a build and rebuild it from inventory.
- **Status panel.** Shows current task, health, power, and any problems. Opens from a golem right-click.
- **Workshop block.** Required for repair and part swaps.
- **Two biomes of flavor.** Plains and desert. Desert parts get sand-proof seals.

### Deferred to v2+
- Foreman and team command system
- Nether and End materials
- Phase-shifting legs and wall climbing
- Autocrafting jobs (Grim Storage)
- Mob grinding roles (Grim Soul Industries)
- Full animation set (Lively Motions)
- Advanced behavior builder (loops, variables, conditionals)

### Success criteria for v1
- A new player can build and deploy a working golem in under 10 minutes without a guide.
- A golem with mismatched parts visibly looks worse and performs worse.
- A player can leave a golem running for an in-game day without it getting stuck, or it tells them clearly why it stopped.
- Repair takes more than one trip, so the workshop matters.

### Open questions
- How fast should parts wear? Too fast is punishing, too slow is invisible.
- Do blueprints store part types only, or exact tiers?
- Does the golem need a power source, or does power come from the core alone?

---

## 2. Parts Table

Tiers run from 1 (starter) to 4 (endgame). Materials are the main gate. Stats are per-tier baselines and can be tuned.

### Head (sensors and awareness)

| Part | Tier | Material | Range | Effect | Trade-off |
|---|---|---|---|---|---|
| Cracked Skull | 1 | Bone, cobble | 8 blocks | Basic sight | Jittery, slight wander |
| Lantern Head | 1 | Iron, glass | 10 blocks | Works in dark | Heavier |
| Scope Eye | 2 | Iron, amethyst | 16 blocks | Spots hostiles early | Narrow field |
| Sand-Seal Visor | 2 | Iron, sandstone | 12 blocks | Immune to sand storms | Dim in rain |
| Oracle Crown | 3 | Gold, echo shard | 24 blocks | Shows item counts through walls | Draws aggro from mobs |

### Torso (capacity and durability)

| Part | Tier | Material | Carry | Durability | Trade-off |
|---|---|---|---|---|---|
| Rusted Barrel | 1 | Iron | 9 slots | Low | Slow to repair |
| Riveted Chest | 1 | Iron, copper | 9 slots | Medium | Heavy |
| Reinforced Drum | 2 | Steel | 18 slots | High | Slows legs |
| Sealed Hold | 2 | Steel, rubber | 18 slots | High, water-proof | Expensive |
| Vault Core Casing | 3 | Netherite | 27 slots | Very high | Needs power |

### Arms (work and combat)

| Part | Tier | Material | Work rate | Melee | Trade-off |
|---|---|---|---|---|---|
| Hand Claws | 1 | Iron | Normal | Low | None |
| Hooked Grabbers | 1 | Iron, chain | Normal | Low | Better pickup range |
| Hydraulic Press | 2 | Steel, piston | Fast | Medium | Power hungry |
| Harvest Scythe | 2 | Steel, diamond | Fast on crops | Medium | Poor at hauling |
| Heavy Hammers | 3 | Netherite | Slow | High | Slow to swing |

### Legs (movement)

| Part | Tier | Material | Speed | Terrain | Trade-off |
|---|---|---|---|---|---|
| Stubs | 1 | Cobble | Slow | Flat only | Cheap |
| Piston Stilts | 1 | Iron, piston | Normal | Step up 1 block | Fragile |
| Treads | 2 | Steel, rubber | Normal | Sand, mud | Loud |
| Wheels | 2 | Steel | Fast | Flat only | Stuck on stairs |
| Spider Legs | 3 | Steel, slime | Normal | Climbs walls | Costly to repair |

### Core (power and personality)

| Part | Tier | Material | Power | Trait | Trade-off |
|---|---|---|---|---|---|
| Redstone Battery | 1 | Redstone, iron | Low | None | Must recharge often |
| Coal Furnace Core | 1 | Coal, iron | Medium | Hot, slow to cool | Hazard in rain |
| Lightning Core | 2 | Copper, amethyst | High | Fast, twitchy | Short fuse |
| Steady Core | 2 | Iron, quartz | Medium | Calm, loyal to owner | Slower reactions |
| Soul Core | 3 | Soul sand, echo shard | High | Aggressive, guards owner | Attracts hostile mobs |

### Set bonuses

- **Matched set.** All five parts from one material family give +10% speed and +10% durability.
- **Desert kit.** Sand-Seal Visor, Treads, and Sealed Hold together make a golem immune to sand damage.
- **Workshop kit.** Any golem with three Steel parts gets a free repair tick every in-game hour.

---

## 3. Behavior System

### Concept
Each golem runs a stack of rules, read top to bottom. The first rule whose trigger is true runs its action. If none match, the golem idles. This is simple to learn and still powerful.

### Rule structure
Every rule is: **WHEN [trigger] DO [action]**, with an optional **UNLESS [condition]**.

### Triggers
- Item count in a chest is above or below N
- Hostile mob within N blocks
- Owner within or beyond N blocks
- Health below N percent
- Power below N percent
- Time of day (day, night, sunset)
- Redstone signal on or off
- Raining or sandstorm
- Inventory full or empty
- Crop ready to harvest

### Actions
- Go to a position or block
- Follow owner
- Attack target or nearest hostile
- Pick up item type from area
- Deposit items into a chest
- Take items from a chest
- Harvest crops in radius
- Repair self (needs a repair kit in inventory)
- Recharge at a power block
- Wait N seconds
- Emit redstone signal
- Send a chat message (v2 hook, stubbed in v1)

### Roles (presets)
Roles are starter stacks that players can edit.

- **Hauler.** WHEN chest A has items DO take from A. WHEN inventory full DO go to chest B and deposit.
- **Guard.** WHEN hostile within 8 DO attack. WHEN owner beyond 16 DO follow owner. OTHERWISE go to post and wait.
- **Harvester.** WHEN crop ready within 10 DO harvest. WHEN inventory full DO deposit to nearest chest.
- **Medic.** WHEN health below 50 percent DO go to workshop and repair.

### Example programs

**Two-chest hauler**
1. WHEN chest A has 1 or more of Iron Ingot DO take from A.
2. WHEN inventory full DO go to chest B, deposit.
3. WHEN inventory empty DO go to chest A.

**Night guard with recharge**
1. WHEN power below 20 percent DO recharge at power block.
2. WHEN hostile within 8 DO attack.
3. WHEN time is night DO go to post, wait.
4. OTHERWISE DO follow owner.

**Desert harvester**
1. WHEN sandstorm DO go to shelter, wait.
2. WHEN crop ready within 10 DO harvest.
3. WHEN inventory full DO deposit to chest.

### Feedback
- The status panel shows which rule is active and why.
- When a rule fails (no path, empty chest, no power), the golem shows a short reason. Example: "Stuck: no path to chest B."
- Golems log their last 10 actions. Players can read the log from the panel.

### Guardrails
- Max 8 rules in v1.
- Rules can't create loops that run forever without a wait. The builder adds a 1-second pause between repeated actions to prevent tight loops.
- A golem that fails the same rule 5 times in a row stops and asks for help.

### Data model (for implementation)
- `Golem` has `List<Rule> rules`, `GolemState state`, `Map<PartSlot, Part> parts`.
- `Rule` has `Trigger trigger`, `Action action`, `Optional<Condition> unless`.
- `Trigger` and `Action` are registered by id so later versions can add new ones without breaking saved golems.
- Blueprints store part ids and rule stacks, but not world positions.

---

## Next steps
1. Review the v1 scope and cut anything that doesn't fit.
2. Tune the parts table numbers after the first playtest.
3. Prototype the behavior builder first, since it's the biggest risk.
