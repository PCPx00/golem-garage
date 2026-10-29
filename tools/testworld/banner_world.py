"""
Night-time wasteland set for the CurseForge header and banners: a transformed Garage (7 x 7 inside, roller door,
windows, neon sign), a lineup of golems in front of it, a farm crew beside it and cubs in outfits inside the
workshop. A spectator camera follows a scripted path for each shot, one tp per tick, and each shot is announced
with "SHOT <name> <ticks>" for tools/testworld/record_trailer.sh to record.
Usage: python3 banner_world.py <world dir>
"""
import json
import math
import os
import random
import sys

sys.path.insert(0, os.path.dirname(__file__))
from scenes import pos_long, part, parts, matched  # noqa: E402,F401

Y = -60
PREP = 90
world = sys.argv[1]
pack = os.path.join(world, "datapacks", "bannershots")
fn = os.path.join(pack, "data", "bannershots", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "banner shots"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))
rng = random.Random(7)


def g(x, z, p, yaw=180, extra="", ai=False, y=Y):
    return ("summon golemgarage:golem %s %s %s {Rotation:[%sf,0f],%sPersistenceRequired:1b,Parts:%s%s}"
            % (x, y, z, yaw, "" if ai else "NoAI:1b,", p, extra))


def rule(t, a, p=0, ap=0):
    return '{T:"%s",P:%d,A:"%s",AP:%d}' % (t, p, a, ap)


def wardrobe(ids):
    return "Wardrobe:{Items:[%s]}" % ",".join(
        '{Slot:%db,id:"golemgarage:outfit_%s",count:1}' % (i, o) for i, o in enumerate(ids))


def ease(t):
    return t * t * (3 - 2 * t)


def path(points, ticks):
    """points: (x, y, z, yaw, pitch) keyframes, evenly spread. Returns one pose per tick."""
    out = []
    segs = len(points) - 1
    for k in range(ticks):
        u = ease(k / (ticks - 1)) * segs
        i = min(int(u), segs - 1)
        f = u - i
        a, b = points[i], points[i + 1]
        out.append(tuple(a[j] + (b[j] - a[j]) * f for j in range(5)))
    return out


def orbit(cx, cy, cz, r, a0, a1, ticks, pitch):
    out = []
    for k in range(ticks):
        a = math.radians(a0 + (a1 - a0) * ease(k / (ticks - 1)))
        x, z = cx + r * math.sin(a), cz - r * math.cos(a)
        yaw = math.degrees(math.atan2(-(cx - x), cz - z))
        out.append((x, cy, z, yaw, pitch))
    return out

build = ["gamerule doDaylightCycle false", "gamerule doMobSpawning false", "gamerule doWeatherCycle false",
         "gamerule sendCommandFeedback false", "gamerule doFireTick false", "time set 18000", "weather clear",
         "kill @e[type=!player]"]

# ---------------------------------------------------------------- wasteland ground
build.append("fill -16 %d -18 32 %d 22 minecraft:coarse_dirt" % (Y - 1, Y - 1))
for _ in range(90):
    x, z = rng.randint(-16, 32), rng.randint(-18, 22)
    r = rng.randint(1, 2)
    build.append("fill %d %d %d %d %d %d minecraft:%s" % (x, Y - 1, z, x + r, Y - 1, z + r,
                                                        rng.choice(["dirt", "rooted_dirt", "podzol", "mud"])))
for _ in range(40):
    x, z = rng.randint(-16, 32), rng.randint(-18, 22)
    if not (-1 <= x <= 26 and -7 <= z <= 10):
        build.append("setblock %d %d %d minecraft:dead_bush" % (x, Y, z))

# ---------------------------------------------------------------- the Garage (outer 0..8, inside 1..7)
build += ["fill 0 %d 0 8 %d 8 minecraft:stone_bricks hollow" % (Y - 1, Y + 5),
          "fill 1 %d 1 7 %d 7 minecraft:stone" % (Y - 1, Y - 1)]
for ex, ez in ((2, 2), (6, 2), (2, 6), (6, 6)):
    build.append("setblock %d %d %d golemgarage:steel_emitter" % (ex, Y + 5, ez))
ctrl = (4, Y, 0)
build.append("setblock %d %d %d golemgarage:garage_controller[facing=south]" % ctrl)
build.append("fill 3 %d 8 5 %d 8 golemgarage:garage_door[facing=south]" % (Y, Y + 2))
build.append("fill 0 %d 3 0 %d 5 golemgarage:garage_window" % (Y + 1, Y + 2))
build.append("fill 8 %d 3 8 %d 5 golemgarage:garage_window" % (Y + 1, Y + 2))

# ---------------------------------------------------------------- outside lights: hidden light blocks, campfires, lamps
for x in range(-8, 18, 4):
    build.append("setblock %d %d -5 minecraft:light[level=12]" % (x, Y + 3))
for x, z in ((-9, -4), (16, -4), (27, 10)):
    build.append("setblock %d %d %d minecraft:campfire" % (x, Y, z))
for x, z in ((-10, -7), (17, -7), (11, -1), (11, 9), (24, -1), (24, 9)):
    build += ["setblock %d %d %d minecraft:spruce_fence" % (x, Y, z),
              "setblock %d %d %d minecraft:spruce_fence" % (x, Y + 1, z),
              "setblock %d %d %d minecraft:lantern" % (x, Y + 2, z)]

# ---------------------------------------------------------------- shot 1: a lineup in front of the neon sign
lineup = [
    matched("stone"),
    parts(part("lantern_head", "gold"), part("cargo_barrel", "gold"), part("grabber_claws", "gold"),
          part("wheel_legs", "gold"), part("solar_core", "gold")),
    matched("copper", ("visor_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core")),
    parts(part("scope_head", "diamond"), part("sealed_hold", "diamond"), part("scythe_arms", "diamond"),
          part("spider_legs", "diamond"), part("lightning_core", "diamond")),
    matched("iron"),
    parts(part("visor_head", "netherite"), part("furnace_chest", "netherite"), part("hammer_arms", "netherite"),
          part("tread_legs", "netherite"), part("furnace_core", "netherite")),
    parts(part("sentry_head", "iron"), part("plated_chest", "gold"), part("golem_fists", "copper", ox=2),
          part("strider_legs", "netherite"), part("soul_core", "steel")),
    matched("steel", ("scope_head", "cargo_barrel", "grabber_claws", "tread_legs", "soul_core")),
]
paints = [-1, -1, 9, -1, 14, 10, -1, 11]
for i, p in enumerate(lineup):
    extra = ",Paint:%d" % paints[i] if paints[i] >= 0 else ""
    build.append(g(-6.5 + i * 2.6, -5.5, p, extra=extra, y=Y))

# ---------------------------------------------------------------- shot 2: the farm crew beside the Garage
fx, fz = 12, 0
farm_static = ["fill %d %d %d %d %d %d minecraft:farmland" % (fx, Y - 1, fz, fx + 10, Y - 1, fz + 8),
               "fill %d %d %d %d %d %d minecraft:water" % (fx + 5, Y - 1, fz, fx + 5, Y - 1, fz + 8),
               "setblock %d %d %d minecraft:chest" % (fx + 13, Y, fz + 4),
               "setblock %d %d %d minecraft:barrel[facing=up]" % (fx + 13, Y, fz + 6)]
for x in range(fx, fx + 11, 3):
    for z in range(fz, fz + 9, 4):
        farm_static.append("setblock %d %d %d minecraft:light[level=13]" % (x, Y + 3, z))
build += farm_static
scythe = parts(part("lantern_head", "iron"), part("cargo_barrel", "iron"), part("scythe_arms", "iron"),
               part("strider_legs", "iron"), part("solar_core", "iron"))
claws = parts(part("sentry_head", "copper"), part("cargo_barrel", "copper"), part("grabber_claws", "copper"),
              part("wheel_legs", "copper"), part("redstone_core", "copper"))
farm_live = ["fill %d %d %d %d %d %d minecraft:wheat[age=7]" % (fx, Y, fz, fx + 4, Y, fz + 8),
             "fill %d %d %d %d %d %d minecraft:wheat[age=7]" % (fx + 6, Y, fz, fx + 10, Y, fz + 8),
             "kill @e[type=golemgarage:golem,tag=farm]", "kill @e[type=item]"]
farm_live.append(g(fx + 2.5, fz + 4.5, scythe, ai=True, extra=',Tags:["farm"],Paint:4,Rules:[%s,%s]' % (
    rule("CROP_READY", "HARVEST_CROPS", 12), rule("ALWAYS", "GIVE_TO_GOLEM")), y=Y))
farm_live.append(g(fx + 8.5, fz + 3.5, scythe, ai=True, extra=',Tags:["farm"],Paint:1,Rules:[%s,%s]' % (
    rule("CROP_READY", "HARVEST_CROPS", 12), rule("ALWAYS", "GIVE_TO_GOLEM")), y=Y))
farm_live.append(g(fx + 12.5, fz + 9.5, claws, ai=True, extra=',Tags:["farm"],Rules:[%s,%s,%s]' % (
    rule("INVENTORY_FULL", "DEPOSIT_TO_NEAREST"), rule("ALWAYS", "PICK_UP_ITEMS"), rule("ALWAYS", "WAIT", 0, 3)), y=Y))

# ---------------------------------------------------------------- shot 3: cubs in outfits inside the workshop
SETS = [("cute", ("bunny_ears", "strawberry_dress", "angel_wings"), 6),
        ("goth", ("witch_hat", "lace_dress", "bat_wings"), 15),
        ("cool", ("cap", "leather_jacket", "shades"), 3),
        ("fancy", ("crown", "tuxedo", "monocle"), 0),
        ("bee", ("flower_crown", "bee_costume", "scarf"), 4),
        ("frog", ("frog_hat", "hoodie", "backpack"), 5)]
pcx, pcz = 4.5, 4.5
for i, (k, ids, col) in enumerate(SETS):
    ang = math.radians(205 + i * 26)
    x, z = pcx + 1.9 * math.cos(ang), pcz + 1.9 * math.sin(ang)
    build.append('summon golemgarage:cub %.2f %d %.2f {Rotation:[%df,0f],NoAI:1b,PersistenceRequired:1b,Color:%d,%s}'
                 % (x, Y, z, 180 - (x - pcx) * 12, col, wardrobe(ids)))
build.append(g(pcx, pcz, matched("gold", ("lantern_head", "plated_chest", "golem_fists", "stub_legs",
                                                      "solar_core")), yaw=200, extra=",Paint:6", y=Y + 0.125))

# ---------------------------------------------------------------- camera shots
SHOTS = [
    # The main header: a slow arc round the front of the Garage, the lineup in the foreground.
    ("header", orbit(4.5, Y + 4.5, 3.0, 12.5, -22, 22, 240, 14), []),
    ("lineup", path([(-4.0, Y + 0.3, -10.0, -14, 3), (4.5, Y + 0.4, -10.5, 0, 3), (13.0, Y + 0.3, -10.0, 14, 3)], 240), []),
    ("farm", path([(fx + 1, -55.6, fz + 13.5, 160, 28), (fx + 5.5, -55.2, fz + 14, 180, 30),
                   (fx + 11, -55.6, fz + 13.5, 200, 28)], 260), farm_live),
    ("cubs", path([(5.4, Y + 0.6, 1.3, 8, 24), (3.6, Y + 0.6, 1.3, -8, 24)], 240), []),
]

timeline = []
t = 0
tick = ["scoreboard players add #t bs_t 1",
        "gamemode spectator @a[gamemode=!spectator]",
        "execute if score #t bs_t matches -199..-1 run tp @a %.3f %.3f %.3f %.2f %.2f" % SHOTS[0][1][0]]
for name, poses, at_start in SHOTS:
    x, y, z, yaw, pitch = poses[0]
    tick.append("execute if score #t bs_t matches %d..%d run tp @a %.3f %.3f %.3f %.2f %.2f"
                % (t, t + PREP - 1, x, y, z, yaw, pitch))
    for c in at_start:
        tick.append("execute if score #t bs_t matches %d run %s" % (t + PREP - 6, c))
    start = t + PREP
    tick.append("execute if score #t bs_t matches %d run say SHOT %s %d" % (start, name, len(poses)))
    for k, (x, y, z, yaw, pitch) in enumerate(poses):
        tick.append("execute if score #t bs_t matches %d run tp @a %.3f %.3f %.3f %.2f %.2f"
                    % (start + k, x, y, z, yaw, pitch))
    timeline.append({"scene": name, "start_tick": start, "ticks": len(poses)})
    t = start + len(poses)
tick.append("execute if score #t bs_t matches %d run say trailer done" % t)

setup = ["scoreboard objectives add bs_t dummy", "scoreboard players set #t bs_t -400",
         "forceload add -20 -20 40 30", "gamemode spectator @a"] + build
open(os.path.join(fn, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
open(os.path.join(fn, "tick.mcfunction"), "w").write(
    "execute if score #t bs_t matches -400.. run function bannershots:run\n")
open(os.path.join(fn, "run.mcfunction"), "w").write("\n".join(tick) + "\n")
open(os.path.join(fn, "load.mcfunction"), "w").write(
    "scoreboard objectives add bs_t dummy\nexecute unless score #t bs_t matches -1000.. run function bannershots:setup\n")
tags = os.path.join(pack, "data", "minecraft", "tags", "function")
json.dump({"values": ["bannershots:tick"]}, open(os.path.join(tags, "tick.json"), "w"))
json.dump({"values": ["bannershots:load"]}, open(os.path.join(tags, "load.json"), "w"))
print("banner shots: %d commands, %d ticks" % (len(tick), t))
