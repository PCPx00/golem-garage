"""Datapack for testing 0.6.0 features by hand: a golem to edit and pick up, Garage materials, flat ground."""
import json
import os
import sys

world = sys.argv[1]
pack = os.path.join(world, "datapacks", "featuretest")
fn = os.path.join(pack, "data", "featuretest", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "feature test"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))
setup = [
    "tag @s add ft_setup",
    "gamemode creative @s",
    "time set 6000",
    "gamerule doDaylightCycle false",
    "gamerule doMobSpawning false",
    "weather clear",
    "kill @e[type=golemgarage:golem]",
    "tp @s 0.5 -60 0.5 0 12",
    "scoreboard objectives add ft_t dummy",
    'summon golemgarage:golem 0.5 -60 3.5 {Rotation:[180f,0f],NoAI:1b,PersistenceRequired:1b,'
    'Parts:{head:{Shape:"scope_head",Material:"diamond"},torso:{Shape:"cargo_barrel",Material:"gold"},'
    'arms:{Shape:"grabber_claws",Material:"iron"},legs:{Shape:"wheel_legs",Material:"steel"},'
    'core:{Shape:"lightning_core",Material:"copper"}},Rules:[{T:"POWER_BELOW",P:20,A:"RECHARGE",AP:0},'
    '{T:"CHEST_A_HAS",P:1,A:"TAKE_FROM_A",AP:0,U:"INVENTORY_FULL",UP:0},{T:"ALWAYS",P:0,A:"DEPOSIT_TO_B",AP:0}]}',
    "clear @s",
    "give @s minecraft:air 1",
    "give @s minecraft:stone_bricks 64",
    "give @s golemgarage:laser_emitter 4",
    "give @s golemgarage:manual 1",
]
tick = ["execute as @a[tag=!ft_setup] run function featuretest:setup",
        "scoreboard players add @a[tag=ft_setup] ft_t 1",
        "execute as @a[scores={ft_t=900}] run function featuretest:build"]
# Part of a Garage around a controller placed at 0,-60,1 facing south: the west wall and half the ceiling.
build = ["fill -3 -60 2 -3 -57 6 minecraft:stone_bricks",
         "fill -2 -56 2 2 -56 3 minecraft:stone_bricks",
         "setblock 0 -59 4 minecraft:oak_fence",
         "say partial garage built"]
open(os.path.join(fn, "build.mcfunction"), "w").write("\n".join(build) + "\n")
open(os.path.join(fn, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
open(os.path.join(fn, "tick.mcfunction"), "w").write("\n".join(tick) + "\n")
json.dump({"values": ["featuretest:tick"]}, open(os.path.join(pack, "data", "minecraft", "tags", "function", "tick.json"), "w"))
