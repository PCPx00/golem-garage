# Example: a datapack golem material

Drop this folder into a world's `datapacks` folder. It adds **Mythril**, a golem material that borrows the
iron look tinted light blue, gives off light, and is crafted from anything tagged `c:ingots/mythril`.
For the example, the tag holds prismarine crystals; a real pack would point it at its own ingot.

Every part shape can be crafted from it with the usual part recipes, it repairs in the Garage with the same
item, and `/golemgarage give @s golem mythril` hands out a ready-built Mythril golem.

Fields (all but `item_tag` are optional):

| Field | Meaning |
|---|---|
| `name` | Name shown in game (a language file entry `material.golemgarage.<id>` wins if present) |
| `item_tag` | Item tag of the ingredient |
| `tier` | 0 (stone) to 9; drives health, durability, power and damage (default 2) |
| `base` | Built-in material whose textures it borrows (default `iron`) |
| `color` | Tint laid over those textures, `#rrggbb` (default white) |
| `durability`, `speed`, `power`, `work` | Multipliers (default 1) |
| `traits` | Any of oxidizes, conductive, hardened, fireproof, heavy, smite, luminous, ender, thorns, featherweight |

The file name (`mythril.json`) is the material id. It must not clash with a built-in material.
