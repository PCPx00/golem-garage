"""Datapack for the two-player test: gives Alice and Bob a golem, a cub and a Linker and stands them
where Alice can place a golem and Bob can look at it."""
import json
import os
import sys

world = sys.argv[1]
pack = os.path.join(world, "datapacks", "mptest")
fn = os.path.join(pack, "data", "mptest", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "mp test"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))
GOLEM = ('golemgarage:golem[minecraft:custom_data={Parts:{head:{Shape:"sentry_head",Material:"iron"},'
         'torso:{Shape:"plated_chest",Material:"iron"},arms:{Shape:"golem_fists",Material:"iron"},'
         'legs:{Shape:"stub_legs",Material:"iron"},core:{Shape:"redstone_core",Material:"iron"}}}]')
setup = ["tag @s add mp_setup", "gamemode survival @s", "clear @s",
         "give @s " + GOLEM, "give @s golemgarage:cub", "give @s golemgarage:golem_linker", "give @s minecraft:blue_dye 4"]
tick = [
    "time set 6000", "weather clear",
    "execute as @a[tag=!mp_setup] run function mptest:setup",
    "execute as @a[name=Alice,tag=!mp_placed] run tp @s 0.5 -60 -1.5 0 50",
    "execute as @a[name=Bob,tag=!mp_placed] run tp @s 4.5 -60 1.5 90 25",
    "execute if entity @e[type=golemgarage:golem] run tag @a add mp_placed",
]
open(os.path.join(fn, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
open(os.path.join(fn, "tick.mcfunction"), "w").write("\n".join(tick) + "\n")
json.dump({"values": ["mptest:tick"]}, open(os.path.join(pack, "data", "minecraft", "tags", "function", "tick.json"), "w"))
first = ["gamerule doMobSpawning false", "gamerule doDaylightCycle false", "kill @e[type=golemgarage:golem]"]
open(os.path.join(fn, "load.mcfunction"), "w").write("\n".join(first) + "\n")
json.dump({"values": ["mptest:load"]}, open(os.path.join(pack, "data", "minecraft", "tags", "function", "load.json"), "w"))
print("mp pack written")
