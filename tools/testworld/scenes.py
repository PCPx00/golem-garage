"""
Builds the in-game test datapack: summons golems in every material and shape, Cubs, and a
working Garage mid-repair, then moves the player camera between scenes for screenshots.
Usage: python3 scenes.py <world dir>
"""
import json
import os
import sys

Y = -60  # standing height on a default superflat world


def pos_long(x, y, z):
    v = ((x & 0x3FFFFFF) << 38) | ((z & 0x3FFFFFF) << 12) | (y & 0xFFF)
    return v - (1 << 64) if v >= (1 << 63) else v


def part(shape, mat, dmg=0, ox=0):
    s = '{Shape:"%s",Material:"%s"' % (shape, mat)
    if dmg:
        s += ",Damage:%d" % dmg
    if ox:
        s += ",Ox:%d" % ox
    return s + "}"


def parts(head=None, torso=None, arms=None, legs=None, core=None):
    out = []
    for k, v in (("head", head), ("torso", torso), ("arms", arms), ("legs", legs), ("core", core)):
        if v:
            out.append("%s:%s" % (k, v))
    return "{" + ",".join(out) + "}"


def matched(mat, shapes=("sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core")):
    return parts(*(part(s, mat) for s in shapes))


def golem(x, z, p, yaw=180, extra=""):
    return "summon golemgarage:golem %s %d %s {Rotation:[%df,0f],NoAI:1b,PersistenceRequired:1b,Parts:%s%s}" % (
        x, Y, z, yaw, p, extra)


lines = []
# Row 1: matched sets in vanilla materials.
for i, mat in enumerate(["stone", "copper", "iron", "gold", "steel", "diamond", "netherite"]):
    lines.append(golem(-12 + i * 4, 6, matched(mat)))

# Row 2: shape variety and mixed materials.
row2 = [
    parts(part("lantern_head", "gold"), part("cargo_barrel", "gold"), part("grabber_claws", "gold"),
          part("wheel_legs", "gold"), part("solar_core", "gold")),
    parts(part("visor_head", "netherite"), part("furnace_chest", "netherite"), part("hammer_arms", "netherite"),
          part("tread_legs", "netherite"), part("furnace_core", "netherite")),
    parts(part("scope_head", "diamond"), part("sealed_hold", "diamond"), part("scythe_arms", "diamond"),
          part("spider_legs", "diamond"), part("lightning_core", "diamond")),
    parts(part("sentry_head", "iron"), part("plated_chest", "gold"), part("golem_fists", "copper", ox=2),
          part("strider_legs", "netherite"), part("soul_core", "steel")),
    parts(),
    parts(part("visor_head", "allthemodium"), part("plated_chest", "vibranium"), part("hammer_arms", "unobtainium"),
          part("strider_legs", "vibranium"), part("soul_core", "unobtainium")),
    parts(part("scope_head", "crimson_steel"), part("cargo_barrel", "azure_electrum"), part("grabber_claws", "blaze_gold"),
          part("tread_legs", "tyrian_steel"), part("lightning_core", "crimson_iron")),
]
for i, p in enumerate(row2):
    lines.append(golem(-12 + i * 4, 16, p))

# Row 3: wear levels, copper oxidation and vines.
wear = [0, 40, 80, 130, 200]
for i, d in enumerate(wear):
    p = parts(part("sentry_head", "iron", d * 112 // 140), part("plated_chest", "iron", d * 168 // 140),
              part("golem_fists", "iron", d), part("stub_legs", "iron", d), part("redstone_core", "iron", d * 126 // 140))
    lines.append(golem(-12 + i * 4, 26, p))
for j, ox in enumerate([1, 3]):
    p = parts(*(part(s, "copper", 0, ox) for s in ("sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core")))
    lines.append(golem(8 + j * 4, 26, p))
lines.append(golem(16, 26, matched("iron"), extra=",Vines:1b"))

# Row 4: Cubs in every color.
for i in range(16):
    lines.append("summon golemgarage:cub %s %d %s {Rotation:[180f,0f],NoAI:1b,PersistenceRequired:1b,Color:%d}"
                 % (-15 + i * 2, Y, 34, i))

# Garage mid-repair.
gx0, gz0 = 30, -1
lines += [
    "fill %d %d %d %d %d %d minecraft:stone_bricks hollow" % (gx0, Y - 1, gz0, gx0 + 8, Y + 6, gz0 + 8),
    "fill %d %d %d %d %d %d minecraft:sea_lantern" % (gx0 + 3, Y + 6, gz0 + 3, gx0 + 5, Y + 6, gz0 + 5),
]
emitters = [(gx0 + 2, Y + 6, gz0 + 2), (gx0 + 6, Y + 6, gz0 + 2), (gx0 + 2, Y + 6, gz0 + 6), (gx0 + 6, Y + 6, gz0 + 6)]
for k, (x, y, z) in enumerate(emitters):
    block = "steel_emitter" if k == 3 else "laser_emitter"
    lines.append("setblock %d %d %d golemgarage:%s" % (x, y, z, block))
ctrl = (gx0 + 4, Y + 2, gz0)
lines.append("setblock %d %d %d golemgarage:garage_controller" % ctrl)
center = (gx0 + 4, Y, gz0 + 4)
broken = parts(part("sentry_head", "gold", 60), part("plated_chest", "iron", 120), part("hammer_arms", "netherite", 200),
               part("tread_legs", "steel", 150), part("lightning_core", "copper", 40))
lines.append("data merge block %d %d %d {Golem:{Parts:%s,Broken:1b},Progress:400,Required:12000,Center:%dL,Emitters:[%s]}"
             % (ctrl + (broken, pos_long(*center), ",".join("%dL" % pos_long(*e) for e in emitters))))

# A pen of free-roaming golems to show walking animations.
pen = (-36, Y, 6)
walkers = [
    parts(part("lantern_head", "gold"), part("cargo_barrel", "gold"), part("grabber_claws", "gold"),
          part("wheel_legs", "gold"), part("solar_core", "gold")),
    parts(part("scope_head", "diamond"), part("sealed_hold", "diamond"), part("scythe_arms", "diamond"),
          part("spider_legs", "diamond"), part("lightning_core", "diamond")),
    parts(part("sentry_head", "iron"), part("plated_chest", "iron"), part("golem_fists", "iron"),
          part("strider_legs", "iron"), part("redstone_core", "iron")),
    parts(part("visor_head", "netherite"), part("furnace_chest", "netherite"), part("hammer_arms", "netherite"),
          part("tread_legs", "netherite"), part("furnace_core", "netherite")),
    matched("copper"),
]
for i, p in enumerate(walkers):
    lines.append("summon golemgarage:golem %d %d %d {Rotation:[180f,0f],PersistenceRequired:1b,Parts:%s,"
                 "Rules:[{T:\"ALWAYS\",P:0,A:\"STROLL\",AP:0}],Links:{POST:%dL}}"
                 % (pen[0] - 4 + i * 2, Y, pen[2], p, pos_long(*pen)))

setup = [
    "tag @s add gt_setup",
    "time set 6000",
    "weather clear",
    "gamerule doDaylightCycle false",
    "gamerule doWeatherCycle false",
    "gamerule doMobSpawning false",
    "gamerule sendCommandFeedback false",
    "scoreboard objectives add gt_t dummy",
    "scoreboard players set @s gt_t 0",
    "gamemode creative @s",
    "effect give @s minecraft:invisibility infinite 0 true",
] + lines

# Camera scenes: (x, y, z, yaw, pitch). 10 seconds each.
scenes = [
    (0, Y + 1.0, -5, 0, 8),          # row 1 overview
    (-7, Y + 1.0, 0.5, 0, 6),        # row 1 left close
    (5, Y + 1.0, 0.5, 0, 6),         # row 1 right close
    (0, Y + 1.0, 9, 0, 6),           # row 2 overview
    (-8, Y + 0.6, 11, 0, 4),         # row 2 left close
    (4, Y + 0.6, 11, 0, 4),          # row 2 right close
    (0, Y + 1.5, 23.5, 180, 8),      # row 2 backs (core packs)
    (0, Y + 1.0, 20, 0, 6),          # row 3 wear
    (0, Y + 2.0, 29, 0, 28),         # cubs
    (gx0 + 6.8, Y + 0.5, gz0 + 7.6, 140, -8),   # garage interior
    (gx0 + 1.4, Y + 0.5, gz0 + 7.4, 220, -6),   # garage other angle
    (16, Y + 2.0, 10, 125, 14),      # 3/4 view of rows
    (-36, Y + 2.5, -5, 0, 18),       # walking golems
    (-36, Y + 2.5, -5, 0, 18),       # walking golems again
    (0, Y, 13.2, 0, 2),              # close to the mixed golem, for the GUI
]
tick = ["execute as @a[tag=!gt_setup] run function golemtest:setup",
        "scoreboard players add @a[tag=gt_setup] gt_t 1"]
for i, (x, y, z, yaw, pitch) in enumerate(scenes):
    t = 60 + i * 200
    tick.append("execute as @a[scores={gt_t=%d}] run tp @s %s %s %s %s %s" % (t, x, y, z, yaw, pitch))
    tick.append("execute as @a[scores={gt_t=%d}] run say scene %d" % (t, i + 1))

if __name__ == "__main__":
    world = sys.argv[1]
    pack = os.path.join(world, "datapacks", "golemtest")
    fn = os.path.join(pack, "data", "golemtest", "function")
    os.makedirs(fn, exist_ok=True)
    os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
    with open(os.path.join(pack, "pack.mcmeta"), "w") as fh:
        json.dump({"pack": {"pack_format": 48, "description": "Golem Garage test scenes"}}, fh)
    with open(os.path.join(fn, "setup.mcfunction"), "w") as fh:
        fh.write("\n".join(setup) + "\n")
    with open(os.path.join(fn, "tick.mcfunction"), "w") as fh:
        fh.write("\n".join(tick) + "\n")
    with open(os.path.join(pack, "data", "minecraft", "tags", "function", "tick.json"), "w") as fh:
        json.dump({"values": ["golemtest:tick"]}, fh)
    print("scenes:", len(scenes), "commands:", len(setup))
