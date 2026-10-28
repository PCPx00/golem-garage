"""
Behavior test datapack for a headless server run. Each scenario is a named golem; run the server
with -Pgolemdebug and read what each golem did from the log.
Usage: python3 behaviors.py <world dir>
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
from scenes import pos_long, part, parts, matched  # noqa: E402

Y = -60


def name(n):
    return "CustomName:'\"%s\"'" % n


def golem(x, z, n, p, rules, extra=""):
    return ("summon golemgarage:golem %d %d %d {%s,PersistenceRequired:1b,Parts:%s,Rules:[%s]%s}"
            % (x, Y, z, name(n), p, ",".join(rules), extra))


def rule(t, a, p=0, ap=0, u=None, up=0):
    s = '{T:"%s",P:%d,A:"%s",AP:%d' % (t, p, a, ap)
    if u:
        s += ',U:"%s",UP:%d' % (u, up)
    return s + "}"


def links(**kw):
    return ",Links:{%s}" % ",".join("%s:%dL" % (k, pos_long(*v)) for k, v in kw.items())


def items(*stacks):
    return ",Items:[%s]" % ",".join('{Slot:%db,id:"minecraft:%s",count:%d}' % (i, s, c) for i, (s, c) in enumerate(stacks))


claws = parts(part("sentry_head", "iron"), part("cargo_barrel", "iron"), part("grabber_claws", "iron"),
              part("stub_legs", "iron"), part("redstone_core", "iron"))
L = [
    "time set 18000",
    "gamerule doDaylightCycle false",
    "gamerule doMobSpawning false",
    "weather clear",
    "kill @e[type=golemgarage:golem]",
    "kill @e[type=minecraft:zombie]",
    "kill @e[type=minecraft:item]",
]

# 1. Hauler: chest A -> chest B.
a, b = (-20, Y, -20), (-12, Y, -20)
L += ["setblock %d %d %d minecraft:chest" % a, "setblock %d %d %d minecraft:chest" % b,
      'data merge block %d %d %d {Items:[{Slot:0b,id:"minecraft:cobblestone",count:64},{Slot:1b,id:"minecraft:cobblestone",count:64},{Slot:2b,id:"minecraft:oak_log",count:32}]}' % a]
L.append(golem(-16, -18, "Hauler", claws, [
    rule("POWER_BELOW", "RECHARGE", 20), rule("INVENTORY_FULL", "DEPOSIT_TO_B"),
    rule("CHEST_A_HAS", "TAKE_FROM_A", 1, 0, "INVENTORY_FULL"), rule("ALWAYS", "DEPOSIT_TO_B")],
    links(CHEST_A=a, CHEST_B=b, POST=(-16, Y, -18))))

# 2. Harvester on ripe wheat.
L += ["fill 0 %d -24 4 %d -20 minecraft:farmland" % (Y - 1, Y - 1),
      "fill 0 %d -24 4 %d -20 minecraft:wheat[age=7]" % (Y, Y),
      "setblock 6 %d -22 minecraft:chest" % Y]
L.append(golem(2, -17, "Harvester", parts(part("sentry_head", "iron"), part("cargo_barrel", "iron"),
                                            part("scythe_arms", "iron"), part("stub_legs", "iron"),
                                            part("redstone_core", "iron")),
               [rule("INVENTORY_FULL", "DEPOSIT_TO_NEAREST"), rule("CROP_READY", "HARVEST_CROPS", 10),
                rule("ALWAYS", "DEPOSIT_TO_NEAREST")]))

# 3. Recharge at a redstone block.
L += ["setblock 14 %d -20 minecraft:redstone_block" % Y]
L.append(golem(11, -17, "Charger", matched("iron"), [rule("POWER_BELOW", "RECHARGE", 90), rule("ALWAYS", "WAIT", 0, 5)],
               ",Power:5"))

# 4. Furnace chest smelting raw iron.
L.append(golem(20, -18, "Smelter", parts(part("sentry_head", "iron"), part("furnace_chest", "iron"),
                                         part("golem_fists", "iron"), part("stub_legs", "iron"),
                                         part("furnace_core", "iron")),
               [rule("ALWAYS", "WAIT", 0, 5)], items(("raw_iron", 8), ("coal", 4))))

# 5. Damaged golem walks into its Garage and pays with its own iron.
gx0, gz0 = -22, 4
L += ["fill %d %d %d %d %d %d minecraft:stone_bricks hollow" % (gx0, Y - 1, gz0, gx0 + 6, Y + 5, gz0 + 6)]
emitters = [(gx0 + 2, Y + 5, gz0 + 2), (gx0 + 4, Y + 5, gz0 + 4)]
for e in emitters:
    L.append("setblock %d %d %d golemgarage:laser_emitter" % e)
ctrl = (gx0 + 3, Y + 1, gz0)
L.append("setblock %d %d %d golemgarage:garage_controller" % ctrl)
worn = parts(part("sentry_head", "iron", 30), part("plated_chest", "iron", 60), part("golem_fists", "iron", 140),
             part("stub_legs", "iron", 20), part("redstone_core", "iron"))
L.append(golem(gx0 + 3, gz0 + 4, "Medic", worn, [rule("PART_BROKEN", "GO_TO_GARAGE"), rule("ALWAYS", "WAIT", 0, 5)],
               links(GARAGE=ctrl) + items(("iron_ingot", 10))))

# 6. Signal plate + lamp.
L += ["setblock 8 %d 6 golemgarage:signal_plate" % Y, "setblock 9 %d 6 minecraft:redstone_lamp" % Y]
L.append(golem(8, 6, "Signal", matched("iron"), [rule("ALWAYS", "EMIT_SIGNAL")]))

# 7. Guard vs zombie at night.
L.append(golem(20, 6, "Guard", matched("iron"), [rule("HOSTILE_WITHIN", "ATTACK_HOSTILE", 10), rule("ALWAYS", "WAIT", 0, 2)]))
L.append('summon minecraft:zombie 24 %d 9 {PersistenceRequired:1b,ArmorItems:[{},{},{},{id:"minecraft:iron_helmet",count:1}]}' % Y)

# 8. Breakdown: nearly dead golem takes a hit.
L.append(golem(0, 14, "Fragile", matched("stone"), [rule("ALWAYS", "WAIT", 0, 5)], ",HealthLeft:2f"))

# 9. Collector picks up dropped items.
L.append(golem(-8, 14, "Collector", claws, [rule("ALWAYS", "PICK_UP_ITEMS")]))
for i in range(4):
    L.append('summon minecraft:item %d %d %d {Item:{id:"minecraft:diamond",count:3},PickupDelay:0}' % (-11 + i * 2, Y, 17))

# 10. A rule that can't run: reports why, then stops after five tries.
L.append(golem(14, 14, "Lost", matched("iron"), [rule("ALWAYS", "TAKE_FROM_A")]))

# 11. Any storage block works: deposits into a barrel (item handler, not just chests).
L += ["setblock -30 %d 22 minecraft:barrel" % Y]
L.append(golem(-28, 20, "BarrelUser", claws, [rule("ALWAYS", "DEPOSIT_TO_NEAREST")], items(("oak_planks", 20), ("stick", 8))))

# 12. Teamwork: Giver hands its items to Receiver.
L.append(golem(-20, 26, "Giver", claws, [rule("ALWAYS", "GIVE_TO_GOLEM")], items(("wheat", 30), ("carrot", 12))))
L.append(golem(-14, 26, "Receiver", claws, [rule("ALWAYS", "WAIT", 0, 5)]))

# 13. Patrol inside a work area, and come back when outside it.
L.append(golem(10, 26, "Patroller", matched("iron"),
               [rule("OUTSIDE_AREA", "PATROL"), rule("ALWAYS", "PATROL")],
               links(AREA_1=(4, Y, 22), AREA_2=(12, Y, 30))))
L.append(golem(24, 34, "Wanderer", matched("iron"), [rule("OUTSIDE_AREA", "PATROL"), rule("ALWAYS", "WAIT", 0, 5)],
               links(AREA_1=(4, Y, 22), AREA_2=(12, Y, 30))))

# 14. Modules: magnet pulls items in, regen heals, paint and plating load from save data.
L.append(golem(-4, 34, "Magnet", claws, [rule("ALWAYS", "WAIT", 0, 5)],
               ',Modules:["magnet","regen"],Paint:11,HealthLeft:10f'))
for i in range(3):
    L.append('summon minecraft:item %d %d %d {Item:{id:"minecraft:emerald",count:2},PickupDelay:0}' % (-8 + i * 4, Y, 38))

# 15. No area set: reports why.
L.append(golem(30, 26, "NoArea", matched("iron"), [rule("ALWAYS", "PATROL")]))

# 16. Rest at its dock: walks home and recharges slowly.
L += ["setblock 34 %d 34 golemgarage:golem_dock" % Y]
L.append(golem(28, 32, "Docker", matched("iron"), [rule("ALWAYS", "GO_HOME")], links(HOME=(34, Y, 34)) + ",Power:5"))

# 17. A job the server turned off (the test config disables pick_up_items).
L.append(golem(-30, 34, "Blocked", claws, [rule("ALWAYS", "PICK_UP_ITEMS")]))

later = ["say hitting Fragile", "damage @e[type=golemgarage:golem,name=Fragile,limit=1] 6 minecraft:generic"]

world = sys.argv[1]
pack = os.path.join(world, "datapacks", "golembehave")
fn = os.path.join(pack, "data", "golembehave", "function")
os.makedirs(fn, exist_ok=True)
os.makedirs(os.path.join(pack, "data", "minecraft", "tags", "function"), exist_ok=True)
with open(os.path.join(pack, "pack.mcmeta"), "w") as fh:
    json.dump({"pack": {"pack_format": 48, "description": "Golem Garage behavior tests"}}, fh)
with open(os.path.join(fn, "setup.mcfunction"), "w") as fh:
    fh.write("forceload add -48 -48 48 48\nschedule function golembehave:place 60t\n")
with open(os.path.join(fn, "place.mcfunction"), "w") as fh:
    fh.write("\n".join(L) + "\nsay behavior tests placed\nschedule function golembehave:later 200t\n")
with open(os.path.join(fn, "later.mcfunction"), "w") as fh:
    fh.write("\n".join(later) + "\n")
with open(os.path.join(pack, "data", "minecraft", "tags", "function", "load.json"), "w") as fh:
    json.dump({"values": ["golembehave:setup"]}, fh)
print("behavior commands:", len(L))
