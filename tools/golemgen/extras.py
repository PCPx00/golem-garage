"""
Icons, item models, recipes and names for the wasteland update items (rigs, scrap, cards, horn, tablet and the
rest). Icons are drawn as 16x16 character grids; each character is a colour from PAL ('.' is clear).
"""
import json
import os

from PIL import Image

MODID = "golemgarage"

PAL = {
    "k": (20, 18, 20), "d": (48, 44, 46), "g": (92, 88, 86), "G": (140, 134, 128), "w": (210, 206, 196),
    "W": (245, 242, 232), "r": (96, 46, 26), "R": (150, 76, 36), "o": (196, 112, 48), "O": (232, 160, 70),
    "y": (226, 180, 48), "Y": (250, 220, 110), "b": (30, 28, 30), "e": (170, 30, 26), "E": (226, 64, 50),
    "c": (60, 170, 200), "C": (140, 226, 246), "n": (90, 60, 36), "N": (130, 92, 56), "p": (226, 214, 170),
    "P": (246, 238, 206), "v": (70, 120, 50), "V": (120, 180, 80), "m": (110, 70, 140), "M": (170, 120, 210),
    "s": (176, 180, 186), "S": (220, 224, 230), "f": (240, 120, 30), "F": (255, 200, 80), "t": (40, 60, 70),
    "T": (70, 110, 130), "h": (230, 150, 160), "H": (250, 200, 210), "B": (40, 80, 150), "u": (80, 130, 210),
}

# Item icons are 3D renders made by icons2.py; these ids just need a flat item model.
ICON_IDS = ["garage_blueprint", "scrap", "tyre", "rig_ram", "rig_turret", "rig_plating", "rule_card", "war_horn",
            "golem_tablet"]

# The rig item renders as a 3D model; this small drawing is only its break particle.
RIG_PARTICLE = [

        "................",
        "................",
        "................",
        "......kkkkk.....",
        ".....kTttTk.....",
        "....kTtttTtk.g..",
        ".kkkkkkkkkkkkgsg",
        "kRrbbbbbbbbbbbkg",
        "kRrbkbbbbbbkbbbk",
        "krrbbbbbbbbbbbOk",
        "kkkkkkkkkkkkkkkk",
        ".kkkk.....kkkk..",
        "kbgGbk...kbgGbk.",
        "kbGgbk...kbGgbk.",
        ".kkkk.....kkkk..",
        "................"]

# id -> English name
NAMES = {
    "scrap": "Scrap",
    "tyre": "Tyre",
    "rig": "War Rig",
    "rig_interceptor": "Interceptor",
    "rig_juggernaut": "Juggernaut",
    "rig_skimmer": "Swamp Skimmer",
    "rig_ram": "Spiked Ram",
    "rig_turret": "Rig Turret",
    "rig_plating": "Rig Plating",
    "rule_card": "Rule Card",
    "war_horn": "War Horn",
    "golem_tablet": "Golem Tablet",
    "fabricator": "Parts Fabricator",
    "cub_bed": "Cub Bed",
    "garage_radio": "Garage Radio",
    "garage_blueprint": "Garage Blueprint",
    "arena_bell": "Arena Bell",
}

SPRAY = {
    # finish: (name, cap colour, band colours, dyes)
    "flames": ("Flames Spray Can", (214, 60, 26), [(250, 220, 110), (240, 120, 30), (214, 60, 26)],
               ["minecraft:red_dye", "minecraft:orange_dye", "minecraft:yellow_dye"]),
    "hazard": ("Hazard Spray Can", (226, 180, 48), [(226, 180, 48), (30, 28, 30), (226, 180, 48)],
               ["minecraft:yellow_dye", "minecraft:black_dye"]),
    "rust": ("Rust Spray Can", (150, 76, 36), [(176, 104, 52), (122, 62, 28), (98, 48, 24)],
             ["minecraft:orange_dye", "minecraft:brown_dye"]),
    "camo": ("Camo Spray Can", (84, 92, 52), [(84, 92, 52), (116, 104, 68), (40, 44, 30)],
             ["minecraft:green_dye", "minecraft:brown_dye"]),
    "chrome": ("Chrome Spray Can", (220, 224, 230), [(245, 245, 250), (176, 180, 186), (220, 224, 230)],
               ["minecraft:quartz", "minecraft:white_dye"]),
    "tiger": ("Tiger Spray Can", (226, 128, 36), [(226, 128, 36), (24, 20, 20), (226, 128, 36)],
              ["minecraft:orange_dye", "minecraft:black_dye"]),
}


RECIPES = {
    "garage_blueprint": {"type": "minecraft:crafting_shapeless", "category": "misc",
                         "ingredients": [{"item": "minecraft:paper"}, {"item": "minecraft:paper"}, {"tag": "c:dyes/blue"},
                                         {"item": "golemgarage:garage_controller"}],
                         "result": {"id": "golemgarage:garage_blueprint", "count": 1}},
    "arena_bell": {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": [" G ", " L ", "SSS"],
                   "key": {"G": {"tag": "c:storage_blocks/copper"}, "L": {"tag": "minecraft:logs"},
                           "S": {"item": "golemgarage:scrap"}},
                   "result": {"id": "golemgarage:arena_bell", "count": 1}},
    "rig_interceptor": {"type": "minecraft:crafting_shaped", "category": "transportation", "pattern": ["TST", "GFG", "T T"],
                        "key": {"T": {"item": "golemgarage:tyre"}, "S": {"item": "golemgarage:scrap"},
                                "G": {"tag": "c:ingots/gold"}, "F": {"item": "minecraft:blast_furnace"}},
                        "result": {"id": "golemgarage:rig_interceptor", "count": 1}},
    "rig_juggernaut": {"type": "minecraft:crafting_shaped", "category": "transportation", "pattern": ["OIO", "TFT", "TTT"],
                       "key": {"O": {"tag": "c:obsidians"}, "I": {"tag": "c:storage_blocks/iron"},
                               "T": {"item": "golemgarage:tyre"}, "F": {"item": "minecraft:blast_furnace"}},
                       "result": {"id": "golemgarage:rig_juggernaut", "count": 1}},
    "rig_skimmer": {"type": "minecraft:crafting_shaped", "category": "transportation", "pattern": ["STS", "BFB", "BBB"],
                    "key": {"S": {"item": "golemgarage:scrap"}, "T": {"item": "golemgarage:tyre"},
                            "B": {"tag": "minecraft:boats"}, "F": {"item": "minecraft:blast_furnace"}},
                    "result": {"id": "golemgarage:rig_skimmer", "count": 1}},
    "garage_radio": {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": [" I ", "CJC", "III"],
                     "key": {"I": {"tag": "c:ingots/iron"}, "C": {"tag": "c:ingots/copper"}, "J": {"item": "minecraft:jukebox"}},
                     "result": {"id": "golemgarage:garage_radio", "count": 1}},
    "cub_bed": {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": ["WWW", "SSS"],
                "key": {"W": {"tag": "minecraft:wool"}, "S": {"tag": "minecraft:wooden_slabs"}},
                "result": {"id": "golemgarage:cub_bed", "count": 1}},
    "golem_tablet": {"type": "minecraft:crafting_shaped", "category": "tools", "pattern": ["III", "IGI", "IRI"],
                     "key": {"I": {"tag": "c:ingots/iron"}, "G": {"item": "minecraft:glass_pane"},
                             "R": {"item": "minecraft:redstone_block"}},
                     "result": {"id": "golemgarage:golem_tablet", "count": 1}},
    "fabricator": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SPS", "SCS", "SRS"],
                   "key": {"S": {"tag": "c:ingots/steel"}, "P": {"item": "minecraft:piston"},
                           "C": {"item": "minecraft:crafting_table"}, "R": {"item": "minecraft:redstone"}},
                   "result": {"id": "golemgarage:fabricator", "count": 1}},
    "war_horn": {"type": "minecraft:crafting_shapeless", "category": "tools",
                 "ingredients": [{"tag": "c:ingots/copper"}, {"tag": "c:ingots/copper"}, {"tag": "c:ingots/copper"},
                                 {"item": "minecraft:redstone"}, {"item": "minecraft:leather"}],
                 "result": {"id": "golemgarage:war_horn", "count": 1}},
    "rule_card": {"type": "minecraft:crafting_shapeless", "category": "misc",
                  "ingredients": [{"item": "minecraft:paper"}, {"item": "minecraft:paper"}, {"item": "minecraft:redstone"}],
                  "result": {"id": "golemgarage:rule_card", "count": 2}},
    "rule_card_copy": {"type": "golemgarage:card_copy", "category": "misc"},
    "scrap": {"type": "minecraft:crafting_shapeless", "category": "misc",
              "ingredients": [{"tag": "c:ingots/iron"}, {"item": "minecraft:gravel"}],
              "result": {"id": "golemgarage:scrap", "count": 3}},
    "tyre": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" K ", "KNK", " K "],
             "key": {"K": {"item": "minecraft:dried_kelp_block"}, "N": {"tag": "c:nuggets/iron"}},
             "result": {"id": "golemgarage:tyre", "count": 2}},
    "rig": {"type": "minecraft:crafting_shaped", "category": "transportation", "pattern": ["TST", "IFI", "TST"],
            "key": {"T": {"item": "golemgarage:tyre"}, "S": {"item": "golemgarage:scrap"},
                    "I": {"tag": "c:storage_blocks/iron"}, "F": {"item": "minecraft:blast_furnace"}},
            "result": {"id": "golemgarage:rig", "count": 1}},
    "rig_ram": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["PPP", "ISI"],
                "key": {"P": {"item": "minecraft:pointed_dripstone"}, "I": {"tag": "c:ingots/iron"},
                        "S": {"item": "golemgarage:scrap"}},
                "result": {"id": "golemgarage:rig_ram", "count": 1}},
    "rig_turret": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": [" C ", "IDI", "SSS"],
                   "key": {"C": {"item": "minecraft:crossbow"}, "I": {"tag": "c:ingots/iron"},
                           "D": {"item": "minecraft:dispenser"}, "S": {"item": "golemgarage:scrap"}},
                   "result": {"id": "golemgarage:rig_turret", "count": 1}},
    "rig_plating": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SIS", "ISI", "SIS"],
                    "key": {"S": {"item": "golemgarage:scrap"}, "I": {"tag": "c:ingots/iron"}},
                    "result": {"id": "golemgarage:rig_plating", "count": 1}},
}


def draw(grid):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row[:16]):
            if ch != ".":
                px[x, y] = PAL[ch] + (255,)
    return img


def _json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def generate(assets, data, lang):
    os.makedirs(os.path.join(assets, "textures/item"), exist_ok=True)
    draw(RIG_PARTICLE).save(os.path.join(assets, "textures/item", "rig.png"))
    for iid in ICON_IDS:
        _json(os.path.join(assets, "models/item", iid + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MODID, iid)}})
    for iid, name in NAMES.items():
        lang[("block" if iid in ("fabricator", "cub_bed", "garage_radio", "arena_bell") else "item") + ".%s.%s" % (MODID, iid)] = name
    for fin, (name, cap, bands, dyes) in SPRAY.items():
        iid = "spray_can_" + fin
        _json(os.path.join(assets, "models/item", iid + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MODID, iid)}})
        lang["item.%s.%s" % (MODID, iid)] = name
        _json(os.path.join(data, MODID, "recipe", iid + ".json"),
              {"type": "minecraft:crafting_shapeless", "category": "misc",
               "ingredients": [{"tag": "c:ingots/iron"}, {"item": "minecraft:gunpowder"}] + [{"item": d} for d in dyes],
               "result": {"id": "%s:%s" % (MODID, iid), "count": 1}})
    for rid, recipe in RECIPES.items():
        _json(os.path.join(data, MODID, "recipe", rid + ".json"), recipe)
    # Rigs are drawn as little 3D models by RigItemRenderer.
    for rid in ("rig", "rig_interceptor", "rig_juggernaut", "rig_skimmer"):
        _json(os.path.join(assets, "models/item", rid + ".json"),
              {"parent": "minecraft:builtin/entity", "textures": {"particle": "%s:item/rig" % MODID}, "gui_light": "side"})
