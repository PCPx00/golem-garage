"""
Datapack that stages short camera shots for the CurseForge banners. Each scene is built far apart,
then a spectator camera is moved along a path, one tp per tick. A scoreboard timer drives it all, and
the timeline (scene, start tick, length) is written to timeline.json for cutting the recording.
Usage: python3 trailer_world.py <world dir>
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from scenes import pos_long, part, parts, matched  # noqa: E402

Y = -60
PREP = 70  # ticks the camera sits at a scene before its shot, so chunks and models load
world = sys.argv[1]
pack = os.path.join(world, "datapacks", "trailer")
fn = os.path.join(pack, "data", "trailer", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "trailer"}}, open(os.path.join(pack, "pack.mcmeta"), "w"))


def g(x, z, p, yaw=180, extra="", ai=False, y=Y):
    return ("summon golemgarage:golem %s %s %s {Rotation:[%sf,0f],%sPersistenceRequired:1b,Parts:%s%s}"
            % (x, y, z, yaw, "" if ai else "NoAI:1b,", p, extra))


def rule(t, a, p=0, ap=0):
    return '{T:"%s",P:%d,A:"%s",AP:%d}' % (t, p, a, ap)


def wardrobe(ids):
    return "Wardrobe:{Items:[%s]}" % ",".join(
        '{Slot:%db,id:"golemgarage:outfit_%s",count:1}' % (i, o) for i, o in enumerate(ids))


build = ["gamerule doDaylightCycle false", "gamerule doMobSpawning false", "gamerule doWeatherCycle false",
         "gamerule sendCommandFeedback false", "time set 6000", "weather clear",
         "kill @e[type=!player]"]

# ---------------------------------------------------------------- scene 1: a lineup of builds (x ~ 0)
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
    build.append(g(-10.5 + i * 3, 6.5, p, extra=extra))

# ---------------------------------------------------------------- scene 2: the Garage at work (x ~ 100)
gx0, gz0 = 96, 96          # outer corner; 9x9 outside, 7x7 inside, 5 tall inside
build += ["fill %d %d %d %d %d %d minecraft:stone_bricks hollow" % (gx0, Y - 1, gz0, gx0 + 8, Y + 5, gz0 + 8),
          "fill %d %d %d %d %d %d minecraft:stone" % (gx0 + 1, Y - 1, gz0 + 1, gx0 + 7, Y - 1, gz0 + 7)]
for e in [(gx0 + 2, gz0 + 2), (gx0 + 6, gz0 + 2), (gx0 + 2, gz0 + 6), (gx0 + 6, gz0 + 6)]:
    build.append("setblock %d %d %d golemgarage:steel_emitter" % (e[0], Y + 5, e[1]))
ctrl = (gx0 + 4, Y + 1, gz0)
build.append("setblock %d %d %d golemgarage:garage_controller[facing=south]" % ctrl)
worn = parts(part("sentry_head", "gold", 60), part("plated_chest", "gold", 120), part("golem_fists", "gold", 160),
             part("stub_legs", "gold", 90), part("redstone_core", "gold", 40))
build.append(g(gx0 + 4.5, gz0 + 3.5, worn, ai=True,
               extra=',Rules:[%s],Links:{GARAGE:%dL},Items:[{Slot:0b,id:"minecraft:gold_ingot",count:20}]'
               % (rule("ALWAYS", "GO_TO_GARAGE"), pos_long(*ctrl))))

# ---------------------------------------------------------------- scene 3: a farm crew (x ~ 200)
fx, fz = 196, 196
farm_live = []
farm_static = ["fill %d %d %d %d %d %d minecraft:farmland" % (fx, Y - 1, fz, fx + 10, Y - 1, fz + 8),
          "fill %d %d %d %d %d %d minecraft:wheat[age=7]" % (fx, Y, fz, fx + 10, Y, fz + 8),
          "fill %d %d %d %d %d %d minecraft:water" % (fx + 5, Y - 1, fz, fx + 5, Y - 1, fz + 8),
          "fill %d %d %d %d %d %d minecraft:air" % (fx + 5, Y, fz, fx + 5, Y, fz + 8),
          "setblock %d %d %d minecraft:chest" % (fx + 13, Y, fz + 4),
          "setblock %d %d %d minecraft:barrel[facing=up]" % (fx + 13, Y, fz + 6)]
scythe = parts(part("lantern_head", "iron"), part("cargo_barrel", "iron"), part("scythe_arms", "iron"),
               part("strider_legs", "iron"), part("solar_core", "iron"))
claws = parts(part("sentry_head", "copper"), part("cargo_barrel", "copper"), part("grabber_claws", "copper"),
              part("wheel_legs", "copper"), part("redstone_core", "copper"))
farm_live.append(g(fx + 2.5, fz + 4.5, scythe, ai=True, extra=",Paint:4,Rules:[%s,%s]" % (
    rule("CROP_READY", "HARVEST_CROPS", 12), rule("ALWAYS", "GIVE_TO_GOLEM"))))
farm_live.append(g(fx + 8.5, fz + 3.5, scythe, ai=True, extra=",Paint:1,Rules:[%s,%s]" % (
    rule("CROP_READY", "HARVEST_CROPS", 12), rule("ALWAYS", "GIVE_TO_GOLEM"))))
farm_live.append(g(fx + 12.5, fz + 9.5, claws, ai=True, extra=",Rules:[%s,%s,%s]" % (
    rule("INVENTORY_FULL", "DEPOSIT_TO_NEAREST"), rule("ALWAYS", "PICK_UP_ITEMS"), rule("ALWAYS", "WAIT", 0, 3))))

farm_live = ["fill %d %d %d %d %d %d minecraft:wheat[age=7]" % (fx, Y, fz, fx + 4, Y, fz + 8),
             "fill %d %d %d %d %d %d minecraft:wheat[age=7]" % (fx + 6, Y, fz, fx + 10, Y, fz + 8)] + farm_live
build += [c for c in farm_static if "wheat" not in c]

# ---------------------------------------------------------------- scene 4: on guard (x ~ 300)
hx, hz = 300, 300
guard_live = []
guard_live.append(g(hx - 2.5, hz + 0.5, matched("netherite", ("visor_head", "plated_chest", "hammer_arms", "tread_legs",
                                                         "soul_core")),
               ai=True, extra=",Paint:14,Rules:[%s,%s]" % (rule("HOSTILE_WITHIN", "ATTACK_HOSTILE", 16),
                                                            rule("ALWAYS", "WAIT", 0, 2))))
guard_live.append(g(hx + 2.5, hz + 0.5, matched("diamond", ("scope_head", "plated_chest", "golem_fists", "stub_legs",
                                                       "lightning_core")),
               ai=True, extra=",Rules:[%s,%s]" % (rule("HOSTILE_WITHIN", "ATTACK_HOSTILE", 16),
                                                  rule("ALWAYS", "WAIT", 0, 2))))
husks = ["summon minecraft:husk %d %d %d {PersistenceRequired:1b}" % (hx + dx, Y, hz + dz)
         for dx, dz in ((-4, 8), (0, 9), (4, 8), (-2, 11), (2, 11))]

# ---------------------------------------------------------------- scene 5: cubs in outfits (x ~ 400)
cx, cz = 400, 400
SETS = [("cute", ("bunny_ears", "strawberry_dress", "angel_wings"), 6),
        ("goth", ("witch_hat", "lace_dress", "bat_wings"), 15),
        ("cool", ("cap", "leather_jacket", "shades"), 3),
        ("fancy", ("crown", "tuxedo", "monocle"), 0),
        ("bee", ("flower_crown", "bee_costume", "scarf"), 4),
        ("space", ("astronaut_helmet", "hoodie", "jetpack"), 8),
        ("frog", ("frog_hat", "hoodie", "backpack"), 5)]
for i, (k, ids, col) in enumerate(SETS):
    ang = math.radians(-60 + i * 20)
    x, z = cx + 3.2 * math.sin(ang), cz + 3.2 * math.cos(ang)
    build.append('summon golemgarage:cub %.2f %d %.2f {Rotation:[0f,0f],NoAI:1b,PersistenceRequired:1b,Color:%d,%s}'
                 % (x, Y, z, col, wardrobe(ids)))
build.append(g(cx + 0.5, cz - 1.5, matched("gold", ("lantern_head", "plated_chest", "golem_fists", "stub_legs",
                                                    "solar_core")), yaw=0, extra=",Paint:6"))
build += ["setblock %d %d %d minecraft:poppy" % (cx - 4, Y, cz - 2), "setblock %d %d %d minecraft:dandelion" % (cx + 4, Y, cz - 2),
          "setblock %d %d %d minecraft:cornflower" % (cx - 5, Y, cz + 1), "setblock %d %d %d minecraft:allium" % (cx + 5, Y, cz + 1)]


# ---------------------------------------------------------------- camera paths
def ease(t):
    return t * t * (3 - 2 * t)


def path(points, ticks):
    """points: list of (x, y, z, yaw, pitch) keyframes, evenly spread. Returns per-tick poses."""
    out = []
    segs = len(points) - 1
    for k in range(ticks):
        u = ease(k / (ticks - 1)) * segs
        i = min(int(u), segs - 1)
        f = u - i
        a, b = points[i], points[i + 1]
        out.append(tuple(a[j] + (b[j] - a[j]) * f for j in range(5)))
    return out


def orbit(cx, cy, cz, r, a0, a1, ticks, pitch, look_y=None):
    out = []
    for k in range(ticks):
        a = math.radians(a0 + (a1 - a0) * ease(k / (ticks - 1)))
        x, z = cx + r * math.sin(a), cz - r * math.cos(a)
        yaw = math.degrees(math.atan2(-(cx - x), cz - z))
        out.append((x, cy, z, yaw, pitch))
    return out


SHOTS = [
    ("lineup", path([(-11, -57.6, -0.5, -18, 14), (0.5, -57.2, -1.5, 0, 14), (11, -57.6, -0.5, 18, 14)], 220), []),
    ("garage", orbit(gx0 + 4.5, Y + 2.6, gz0 + 4.5, 2.6, 160, 230, 220, 18), []),
    ("farm", path([(fx - 1, -55.5, fz + 13.5, 160, 30), (fx + 5.5, -55, fz + 14, 180, 32),
                   (fx + 12, -55.5, fz + 13.5, 200, 30)], 260), farm_live),
    ("guard", path([(hx + 0.5, -58.2, hz - 4.5, 0, 6), (hx + 0.5, -58.4, hz - 2.5, 0, 6)], 220),
     ["kill @e[type=husk]", "kill @e[type=golemgarage:golem,x=%d,y=-64,z=%d,distance=..30]" % (hx, hz)] + guard_live + husks),
    ("cubs", orbit(cx + 0.5, Y + 0.9, cz + 0.5, 4.6, 145, 215, 240, 12), []),
]

timeline = []
t = 0
tick = ["scoreboard players add #t tr_t 1",
        "gamemode spectator @a[gamemode=!spectator]",
        "effect give @a[tag=!tr_nv] minecraft:night_vision infinite 0 true",
        "tag @a add tr_nv",
        "execute if score #t tr_t matches -199..-1 run tp @a %.3f %.3f %.3f %.2f %.2f" % SHOTS[0][1][0]]
for name, poses, at_start in SHOTS:
    x, y, z, yaw, pitch = poses[0]
    tick.append("execute if score #t tr_t matches %d..%d run tp @a %.3f %.3f %.3f %.2f %.2f"
                % (t, t + PREP - 1, x, y, z, yaw, pitch))
    for c in at_start:
        tick.append("execute if score #t tr_t matches %d run %s" % (t + PREP - 6, c))
    start = t + PREP
    tick.append("execute if score #t tr_t matches %d run say SHOT %s %d" % (start, name, len(poses)))
    for k, (x, y, z, yaw, pitch) in enumerate(poses):
        tick.append("execute if score #t tr_t matches %d run tp @a %.3f %.3f %.3f %.2f %.2f"
                    % (start + k, x, y, z, yaw, pitch))
    timeline.append({"scene": name, "start_tick": start, "ticks": len(poses)})
    t = start + len(poses)
tick.append("execute if score #t tr_t matches %d run say trailer done" % t)
timeline.append({"end_tick": t})

setup = ["scoreboard objectives add tr_t dummy", "scoreboard players set #t tr_t -200",
         "forceload add -30 -30 30 30", "forceload add 80 80 120 120", "forceload add 180 180 230 230",
         "forceload add 280 280 320 330", "forceload add 380 380 420 420",
         "gamemode spectator @a", "effect give @a minecraft:night_vision infinite 0 true"] + build
open(os.path.join(fn, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
open(os.path.join(fn, "tick.mcfunction"), "w").write(
    "execute if score #t tr_t matches -200.. run function trailer:run\n")
open(os.path.join(fn, "run.mcfunction"), "w").write("\n".join(tick) + "\n")
open(os.path.join(fn, "load.mcfunction"), "w").write(
    "scoreboard objectives add tr_t dummy\nexecute unless score #t tr_t matches -1000.. run function trailer:setup\n")
tags = os.path.join(pack, "data", "minecraft", "tags", "function")
json.dump({"values": ["trailer:tick"]}, open(os.path.join(tags, "tick.json"), "w"))
json.dump({"values": ["trailer:load"]}, open(os.path.join(tags, "load.json"), "w"))
json.dump(timeline, open(os.path.join(os.path.dirname(__file__), "timeline.json"), "w"), indent=1)
print("trailer: %d commands, %d ticks" % (len(tick), t))
