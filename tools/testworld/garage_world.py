"""
Adds the 0.10.0 Garage test functions to a world: gtest:build (a 5x5 Garage with a roller door, windows and
emitters next to spawn), gtest:cubs (eight cubs owned by the nearest player), gtest:broken (a broken golem item)
and gtest:visitor (a golem by the door, to watch it roll up).
Usage: python3 garage_world.py <world dir>
"""
import json
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from scenes import matched  # noqa: E402

world = sys.argv[1]
pack = os.path.join(world, "datapacks", "gtest")
fn = os.path.join(pack, "data", "gtest", "function")
os.makedirs(fn, exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "garage tests"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))


def write(name, lines):
    open(os.path.join(fn, name + ".mcfunction"), "w").write("\n".join(lines) + "\n")


write("build", [
    "fill 6 -61 -4 14 -55 4 air",
    "fill 7 -61 -3 13 -56 3 minecraft:stone_bricks hollow",
    "fill 8 -61 -2 12 -61 2 minecraft:smooth_stone",
    "setblock 7 -60 0 golemgarage:garage_controller[facing=east]",
    "setblock 9 -56 -1 golemgarage:laser_emitter",
    "setblock 11 -56 1 golemgarage:steel_emitter",
    "fill 13 -60 -1 13 -58 1 golemgarage:garage_door[facing=east]",
    "fill 9 -59 -3 11 -58 -3 golemgarage:garage_window",
    "fill 9 -59 3 11 -58 3 golemgarage:garage_window",
    "fill 7 -58 -2 7 -58 -1 golemgarage:garage_window",
    "fill 7 -58 1 7 -58 2 golemgarage:garage_window",
    "time set noon",
    "say garage built",
])
cubs = ["kill @e[type=golemgarage:cub]"]
for i in range(8):
    cubs.append('execute at @p run summon golemgarage:cub ~ ~ ~ {Tags:["t"],Color:%d,PersistenceRequired:1b}' % i)
cubs.append("execute as @e[type=golemgarage:cub,tag=t] run data modify entity @s Owner set from entity @p UUID")
cubs.append("say cubs placed")
write("cubs", cubs)
write("broken", [
    "give @p golemgarage:golem[custom_data={Broken:1b,Parts:%s}]" % matched("iron"),
    "say broken golem given",
])
write("visitor", [
    "kill @e[type=golemgarage:golem,tag=v]",
    'summon golemgarage:golem 16 -60 0 {Tags:["v"],PersistenceRequired:1b,Parts:%s,Rules:[{T:"ALWAYS",P:0,A:"WAIT",AP:5}]}'
    % matched("copper"),
    "say visitor placed",
])
write("away", ["tp @e[type=golemgarage:golem,tag=v] 22 -60 0", "say visitor moved"])

# Cheats on, so the functions can be run from chat.
try:
    import nbtlib
    level = nbtlib.load(os.path.join(world, "level.dat"))
    level["Data"]["allowCommands"] = nbtlib.Byte(1)
    level.save()
except ImportError:
    print("nbtlib missing: turn cheats on with Open to LAN")

os.makedirs(os.path.join(world, "serverconfig"), exist_ok=True)
with open(os.path.join(world, "serverconfig", "golemgarage-server.toml"), "w") as fh:
    fh.write("[garage]\n\trepairSeconds = 40\n")
