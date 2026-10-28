"""Golem Garage's own advancement tree.

Most milestones fire from game code through the golemgarage:golem_event
criterion (ModTriggers.award(player, "<event>")); a few use vanilla criteria.
"""
import json
import os

NS = "golemgarage"


def ev(event):
    return {"trigger": f"{NS}:golem_event", "conditions": {"event": event}}


def has_item(*items):
    return {"trigger": "minecraft:inventory_changed",
            "conditions": {"items": [{"items": list(items)}]}}


# id, parent, icon, frame, title, description, criterion, hidden
TREE = [
    ("root", None, "manual", "task", "Golem Garage",
     "Build, fit out and look after your own helper golems", has_item(f"{NS}:manual", f"{NS}:golem"), False),
    ("first_golem", "root", "golem", "task", "It's Alive!",
     "Place a golem down in the world", ev("place_golem"), False),
    ("fit_part", "first_golem", "steel_ingot", "task", "Spare Parts",
     "Fit a new part onto one of your golems", ev("fit_part"), False),
    ("matched_set", "fit_part", "repair_kit", "task", "Matching Outfit",
     "Give a golem a full set of parts made from one material", ev("matched_set"), False),
    ("endgame_part", "matched_set", "blueprint", "challenge", "Built to Last",
     "Fit a part made from Netherite or something even tougher", ev("endgame_part"), False),
    ("module", "fit_part", "module_casing", "task", "Plug and Play",
     "Install an upgrade module in a golem", ev("module"), False),
    ("two_modules", "module", "module_magnet", "goal", "Fully Loaded",
     "Fill both of a golem's module sockets", ev("two_modules"), False),
    ("rules", "first_golem", "manual", "task", "House Rules",
     "Give a golem a set of rules to follow", ev("rules_set"), False),
    ("linked", "rules", "golem_linker", "task", "Connected",
     "Use a Golem Linker to link a golem to a chest, post or work area", ev("linked"), False),
    ("paint", "first_golem", "minecraft:red_dye", "task", "Fresh Coat",
     "Paint a golem with a dye", ev("paint"), False),
    ("breakdown", "first_golem", "repair_kit", "task", "Out of Order",
     "Have one of your golems wear out and break down", ev("breakdown"), True),
    ("golem_kill", "first_golem", "minecraft:iron_sword", "task", "Bodyguard",
     "Have one of your golems defeat a monster", ev("golem_kill"), False),
    ("golem_kill_warden", "golem_kill", "minecraft:echo_shard", "challenge", "Heavy Metal",
     "Have one of your golems defeat a Warden", ev("golem_kill_warden"), True),
    ("garage_build", "root", "garage_controller", "task", "Open for Business",
     "Place a Garage Controller",
     {"trigger": "minecraft:placed_block",
      "conditions": {"location": [{"condition": "minecraft:block_state_property",
                                   "block": f"{NS}:garage_controller"}]}}, False),
    ("garage_repair", "garage_build", "laser_emitter", "goal", "Good as New",
     "Have the Garage repair one of your golems", ev("garage_repair"), False),
    ("cub", "root", "cub", "task", "Little Helper",
     "Get a golem cub", has_item(f"{NS}:cub"), False),
    ("cub_dress", "cub", "outfit_cap", "task", "Dress Up",
     "Put an outfit piece on a cub", ev("cub_dress"), False),
    ("cub_full_set", "cub_dress", "outfit_witch_hat", "goal", "Fashion Icon",
     "Dress a cub in a full matching outfit", ev("cub_full_set"), False),
    ("cub_grow", "cub", "golem", "goal", "All Grown Up",
     "Watch a cub grow into a full golem", ev("cub_grow"), False),
]


def _icon(name):
    return name if ":" in name else f"{NS}:{name}"


def generate(DATA, lang):
    out = os.path.join(DATA, NS, "advancement")
    os.makedirs(out, exist_ok=True)
    for f in os.listdir(out):
        if f.endswith(".json"):
            os.remove(os.path.join(out, f))
    for aid, parent, icon, frame, title, desc, crit, hidden in TREE:
        display = {
            "icon": {"id": _icon(icon)},
            "title": {"translate": f"advancement.{NS}.{aid}"},
            "description": {"translate": f"advancement.{NS}.{aid}.desc"},
            "frame": frame,
            "show_toast": True,
            "announce_to_chat": aid != "root",
            "hidden": hidden,
        }
        if parent is None:
            display["background"] = "minecraft:textures/block/smooth_stone.png"
        adv = {"display": display, "criteria": {"done": crit}, "requirements": [["done"]]}
        if parent:
            adv["parent"] = f"{NS}:{parent}"
        with open(os.path.join(out, aid + ".json"), "w") as fh:
            json.dump(adv, fh, indent=2)
            fh.write("\n")
        lang[f"advancement.{NS}.{aid}"] = title
        lang[f"advancement.{NS}.{aid}.desc"] = desc
    print("advancements:", len(TREE))
