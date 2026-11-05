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

ICONS = {
    "scrap": [
        "................",
        "................",
        "......gG........",
        ".....gGsg..R....",
        "....gGgRrgRo....",
        "...dgRRrgGRo....",
        "..dgRorRGgrr....",
        "..dgrRRrgdg.gG..",
        ".dgGgrRrRgdgGsg.",
        ".dgsGgrRoRrgGgd.",
        "..dggsGrRRrgddg.",
        "...ddggdgrrgd...",
        ".....dd.ddd.....",
        "................",
        "................",
        "................"],
    "tyre": [
        "................",
        ".....kkkkkk.....",
        "...kkbdbdbdkk...",
        "..kbdkkkkkkdbk..",
        "..kdkk....kkdk..",
        ".kbkk.gGGg.kkbk.",
        ".kdk.gG..Gg.kdk.",
        ".kbk.G....G.kbk.",
        ".kdk.G....G.kdk.",
        ".kbk.gG..Gg.kbk.",
        ".kdkk.gGGg.kkdk.",
        "..kbkk....kkbk..",
        "..kdbkkkkkkbdk..",
        "...kkdbdbdbkk...",
        ".....kkkkkk.....",
        "................"],
    "rig": [
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
        "................"],
    "rig_ram": [
        "................",
        "................",
        "................",
        "W...W...W...W...",
        "sW..sW..sW..sW..",
        "gsW.gsW.gsW.gsW.",
        "rRRRRRRRRRRRRRr.",
        "rRoRRoRRoRRoRRr.",
        "rrrrrrrrrrrrrrr.",
        "dgggggggggggggd.",
        "dd...........dd.",
        "................",
        "................",
        "................",
        "................",
        "................"],
    "rig_turret": [
        "................",
        "................",
        "................",
        "................",
        ".......kkkkkkkkk",
        "......kgggggggk.",
        "....kkkgkkkkkkkk",
        "...kGGgk........",
        "..kGggggk.......",
        "..kgggggk.......",
        "..kdgggdk.......",
        "...kdddk........",
        "..kRRRRRk.......",
        ".kRrrrrrRk......",
        ".kkkkkkkkk......",
        "................"],
    "rig_plating": [
        "................",
        "..kkkkkkkkkkkk..",
        ".kRRRRoRRRRRRRk.",
        ".kRsRRRRRRRRsRk.",
        ".kRRRRRRrRRRRRk.",
        ".kRRrRRRRRRoRRk.",
        ".kRRRRRRRRRRRRk.",
        ".kkkkkkkkkkkkkk.",
        ".kRRRRRRRRRRRRk.",
        ".kRsRRoRRRRRsRk.",
        ".kRRRRRRRRrRRRk.",
        ".kRRRRRRRRRRRRk.",
        ".kRsRRRRRRRRsRk.",
        "..kkkkkkkkkkkk..",
        "................",
        "................"],
    "rule_card": [
        "................",
        "................",
        "..pppppppppppp..",
        "..pPPPPPPPPPPp..",
        "..pPkPkPPkPkPp..",
        "..pPPPPPPPPPPp..",
        "..pPkPPkPkPPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPkPkPPkPkp..",
        "..pPPPPPPPPPPp..",
        "..pPkPPPkPkPPp..",
        "..pPPPPPPPPPPp..",
        "..pPPPPPPPPeep..",
        "..pppppppppppp..",
        "................",
        "................"],
}

# id -> English name
NAMES = {
    "scrap": "Scrap",
    "tyre": "Tyre",
    "rig": "Wasteland Rig",
    "rig_ram": "Spiked Ram",
    "rig_turret": "Rig Turret",
    "rig_plating": "Rig Plating",
    "rule_card": "Rule Card",
}

RECIPES = {
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
    for iid, grid in ICONS.items():
        path = os.path.join(assets, "textures/item", iid + ".png")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw(grid).save(path)
        _json(os.path.join(assets, "models/item", iid + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MODID, iid)}})
    for iid, name in NAMES.items():
        lang["item.%s.%s" % (MODID, iid)] = name
    for rid, recipe in RECIPES.items():
        _json(os.path.join(data, MODID, "recipe", rid + ".json"), recipe)
