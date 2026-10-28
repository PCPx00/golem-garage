"""Datapack for checking 0.7.0 visuals by eye: dressed cubs (one per style), painted golems with modules,
golems showing status icons, a Linker with marks, and a chest of outfit items."""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from scenes import pos_long, parts, part, matched  # noqa: E402

world = sys.argv[1]
pack = os.path.join(world, "datapacks", "showcase")
fn = os.path.join(pack, "data", "showcase", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "showcase"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))

Y = -60
SETS = {
    "cool": ("cap", "leather_jacket", "shades"),
    "goth": ("witch_hat", "lace_dress", "bat_wings"),
    "cute": ("bunny_ears", "strawberry_dress", "angel_wings"),
    "fancy": ("crown", "tuxedo", "monocle"),
    "mix": ("frog_hat", "bee_costume", "scarf"),
    "space": ("astronaut_helmet", "hoodie", "jetpack"),
}
COLORS = {"cool": 3, "goth": 15, "cute": 6, "fancy": 0, "mix": 5, "space": 8}


def wardrobe(ids):
    items = ",".join('{Slot:%db,id:"golemgarage:outfit_%s",count:1}' % (i, o) for i, o in enumerate(ids))
    return "Wardrobe:{Items:[%s]}" % items


setup = [
    "tag @s add sc_setup", "gamemode creative @s", "time set 6000", "gamerule doDaylightCycle false",
    "gamerule doMobSpawning false", "weather clear",
    "kill @e[type=golemgarage:golem]", "kill @e[type=golemgarage:cub]",
    "tp @s 0.5 -60 0.5 0 15",
]
# Row of cubs, 3 blocks in front of the player.
for i, (k, ids) in enumerate(SETS.items()):
    x = -5 + i * 2
    setup.append('summon golemgarage:cub %d.5 %d 4.5 {Rotation:[180f,0f],NoAI:1b,PersistenceRequired:1b,'
                 'CustomName:\'"%s"\',Color:%d,%s}' % (x, Y, k, COLORS[k], wardrobe(ids)))
# Golems further back: painted, with modules; one stuck (shows its icon), one low on power.
g = ("summon golemgarage:golem %s %d %s {Rotation:[180f,0f],PersistenceRequired:1b,Parts:%s,Rules:[%s]%s}")
setup.append(g % ("-4.5", Y, "9.5", matched("iron"), '{T:"ALWAYS",P:0,A:"WAIT",AP:5}',
                  ',NoAI:1b,Paint:14,Modules:["lamp","magnet"],CustomName:\'"Painted"\''))
setup.append(g % ("-1.5", Y, "9.5", matched("netherite"), '{T:"ALWAYS",P:0,A:"WAIT",AP:5}',
                  ',NoAI:1b,Paint:10,CustomName:\'"Purple"\''))
setup.append(g % ("1.5", Y, "9.5", matched("gold"), '{T:"ALWAYS",P:0,A:"TAKE_FROM_A",AP:0}',
                  ',CustomName:\'"Stuck"\''))
setup.append(g % ("4.5", Y, "9.5", matched("copper"), '{T:"ALWAYS",P:0,A:"WAIT",AP:5}',
                  ',NoAI:1b,Power:1,CustomName:\'"Tired"\''))
# 0.8.0: a golem resting on its dock, and two golems in group 3.
setup.append("setblock 7 %d 6 golemgarage:golem_dock" % Y)
setup.append(g % ("7.5", Y, "6.5", matched("steel"), '{T:"ALWAYS",P:0,A:"GO_HOME",AP:0}',
                  ',Power:20,Links:{HOME:%dL},CustomName:\'"Sleepy"\'' % pos_long(7, Y, 6)))
setup.append(g % ("0.5", Y, "2.2", matched("gold"), '{T:"ALWAYS",P:0,A:"WAIT",AP:5}', ',NoAI:1b,Group:3,CustomName:\'"Crew A"\''))
setup.append(g % ("-7.5", Y, "9.5", matched("gold"), '{T:"ALWAYS",P:0,A:"WAIT",AP:5}', ',NoAI:1b,Group:3,CustomName:\'"Crew B"\''))
setup += [
    "clear @s",
    'give @s golemgarage:golem_linker[minecraft:custom_data={Mode:7,Group:3,POST:%dL,CHEST_A:%dL,AREA_1:%dL,AREA_2:%dL}] 1'
    % (pos_long(0, Y, 12), pos_long(3, Y, 12), pos_long(-6, Y, 6), pos_long(6, Y, 14)),
    "give @s golemgarage:module_speed 1", "give @s minecraft:blue_dye 1", "give @s golemgarage:outfit_crown 1",
    "give @s golemgarage:manual 1", "give @s minecraft:water_bucket 1",
]
tick = ["execute as @a[tag=!sc_setup] run function showcase:setup"]
open(os.path.join(fn, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
open(os.path.join(fn, "tick.mcfunction"), "w").write("\n".join(tick) + "\n")
json.dump({"values": ["showcase:tick"]}, open(os.path.join(pack, "data", "minecraft", "tags", "function", "tick.json"), "w"))
print("showcase commands:", len(setup))
