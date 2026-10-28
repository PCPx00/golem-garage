"""
World for the 0.8.0 server tests: the behavior scenarios, the example Mythril datapack, a test config,
and golems owned by "Alice" (one near spawn, one far away in an unloaded area) for the command tests.
Usage: python3 cmd_world.py <world dir>
"""
import hashlib
import json
import os
import shutil
import subprocess
import sys
import uuid

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.abspath(os.path.join(HERE, "..", ".."))
world = sys.argv[1]
sys.path.insert(0, HERE)
from scenes import part, parts, matched  # noqa: E402


def offline_uuid(name):
    h = bytearray(hashlib.md5(("OfflinePlayer:" + name).encode()).digest())
    h[6] = (h[6] & 0x0F) | 0x30
    h[8] = (h[8] & 0x3F) | 0x80
    return uuid.UUID(bytes=bytes(h))


def uuid_ints(u):
    v = u.int
    out = []
    for i in range(4):
        x = (v >> (96 - 32 * i)) & 0xFFFFFFFF
        out.append(x - (1 << 32) if x >= (1 << 31) else x)
    return "[I;%d,%d,%d,%d]" % tuple(out)


subprocess.run([sys.executable, os.path.join(HERE, "behaviors.py"), world], check=True)
shutil.copytree(os.path.join(ROOT, "docs/examples/mythril_pack"), os.path.join(world, "datapacks", "mythril_pack"),
                dirs_exist_ok=True)
os.makedirs(os.path.join(world, "serverconfig"), exist_ok=True)
with open(os.path.join(world, "serverconfig", "golemgarage-server.toml"), "w") as fh:
    fh.write('[features]\n\tdisabledActions = ["pick_up_items"]\n\n[golems]\n\tkillsCountAsOwner = true\n'
             '\tplayerDamage = "NEVER"\n\n[general]\n\trecallCooldownSeconds = 0\n')

alice = uuid_ints(offline_uuid("Alice"))
pack = os.path.join(world, "datapacks", "cmdtest", "data", "cmdtest", "function")
os.makedirs(pack, exist_ok=True)
os.makedirs(os.path.join(world, "datapacks", "cmdtest", "data", "minecraft", "tags", "function"), exist_ok=True)
json.dump({"pack": {"pack_format": 48, "description": "command tests"}},
          open(os.path.join(world, "datapacks", "cmdtest", "pack.mcmeta"), "w"))
g = 'summon golemgarage:golem %s -60 %s {PersistenceRequired:1b,CustomName:\'"%s"\',Owner:%s,Parts:%s%s}'
setup = [
    "forceload add 1990 1990 2010 2010",
    g % (3, 3, "Rusty", alice, matched("copper"), ',Rules:[{T:"ALWAYS",P:0,A:"WAIT",AP:5}]'),
    g % (2000, 2000, "Faraway", alice, matched("gold"), ',Rules:[{T:"ALWAYS",P:0,A:"WAIT",AP:5}]'),
    g % (-3, 3, "Shiny", alice, parts(part("sentry_head", "mythril"), part("plated_chest", "mythril"),
                                      part("golem_fists", "mythril"), part("stub_legs", "mythril"),
                                      part("redstone_core", "mythril")), ',Rules:[{T:"ALWAYS",P:0,A:"WAIT",AP:5}]'),
    "schedule function cmdtest:unload 100t",
    "say cmd setup done",
]
open(os.path.join(pack, "setup.mcfunction"), "w").write("\n".join(setup) + "\n")
modes = [
    "kill @e[type=minecraft:husk]",
    g % (-60, -60, "Aggro", alice, matched("iron"), ',Mode:"aggressive"'),
    g % (-40, -60, "Calm", alice, matched("iron"), ',Mode:"peaceful"'),
    g % (-20, -60, "Keeper", alice, matched("iron"), ',Mode:"guard"'),
    'summon minecraft:husk -60 -60 -50 {PersistenceRequired:1b}',
    'summon minecraft:husk -60 -60 -76 {PersistenceRequired:1b}',
    'summon minecraft:husk -40 -60 -52 {PersistenceRequired:1b}',
    'summon minecraft:husk -20 -60 -52 {PersistenceRequired:1b}',
    "say modes placed",
]
open(os.path.join(pack, "modes.mcfunction"), "w").write("\n".join(modes) + "\n")
crowd = ["kill @e[type=golemgarage:golem,tag=crowd]"]
for i in range(20):
    crowd.append('summon golemgarage:golem %d -60 %d {Tags:["crowd"],PersistenceRequired:1b,Owner:%s,Parts:%s,'
                 'Rules:[{T:"ALWAYS",P:0,A:"STROLL",AP:0}]}' % (80 + (i % 5) * 2, 80 + (i // 5) * 2, alice, matched("iron")))
open(os.path.join(pack, "crowd.mcfunction"), "w").write("\n".join(crowd) + "\n")
open(os.path.join(pack, "unload.mcfunction"), "w").write("forceload remove 1990 1990 2010 2010\nsay far golem unloaded\n")
json.dump({"values": []},
          open(os.path.join(world, "datapacks", "cmdtest", "data", "minecraft", "tags", "function", "load.json"), "w"))
print("command test world ready; Alice =", offline_uuid("Alice"))
