"""
Power blocks: Solar Panel, Wind Turbine (base; the mast and rotor are drawn in Java), Scrap Burner.

Writes block textures, models, blockstates, item models, loot tables, recipes, the burner's GUI texture and
names. Called from gen.py after blocks.py.
"""
import json
import math
import os
import random

from PIL import Image

import paint
from blocks import metal_panel

MODID = "golemgarage"
BLOCKS = ("solar_panel", "wind_turbine", "burner_generator", "battery", "cub_charger")
NAMES = {"solar_panel": "Solar Panel", "wind_turbine": "Wind Turbine", "burner_generator": "Scrap Burner",
         "battery": "Scrap Battery", "cub_charger": "Cub Charger"}

RECIPES = {
    "battery": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["SCS", "RBR", "SCS"],
                "key": {"S": {"item": "golemgarage:scrap"}, "C": {"tag": "c:ingots/copper"},
                        "R": {"tag": "c:dusts/redstone"}, "B": {"tag": "c:storage_blocks/redstone"}},
                "result": {"id": "golemgarage:battery", "count": 1}},
    "cub_charger": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": [" W ", "CRC"],
                    "key": {"W": {"tag": "minecraft:wool"}, "C": {"tag": "c:ingots/copper"},
                            "R": {"tag": "c:dusts/redstone"}},
                    "result": {"id": "golemgarage:cub_charger", "count": 1}},
    "solar_panel": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["GGG", "LRL", "ISI"],
                    "key": {"G": {"tag": "c:glass_panes"}, "L": {"tag": "c:gems/lapis"},
                            "R": {"tag": "c:dusts/redstone"}, "I": {"tag": "c:ingots/iron"},
                            "S": {"item": "golemgarage:scrap"}},
                    "result": {"id": "golemgarage:solar_panel", "count": 1}},
    "wind_turbine": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["IBI", "ICI", "SRS"],
                     "key": {"I": {"tag": "c:ingots/iron"}, "B": {"item": "minecraft:iron_bars"},
                             "C": {"tag": "c:ingots/copper"}, "R": {"tag": "c:dusts/redstone"},
                             "S": {"item": "golemgarage:scrap"}},
                     "result": {"id": "golemgarage:wind_turbine", "count": 1}},
    "burner_generator": {"type": "minecraft:crafting_shaped", "category": "redstone", "pattern": ["SCS", "IFI", "SRS"],
                         "key": {"S": {"item": "golemgarage:scrap"}, "C": {"tag": "c:ingots/copper"},
                                 "I": {"tag": "c:ingots/iron"}, "F": {"item": "minecraft:blast_furnace"},
                                 "R": {"tag": "c:dusts/redstone"}},
                         "result": {"id": "golemgarage:burner_generator", "count": 1}},
}


def _save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def _json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


# ===================================================================== textures
def solar_top():
    """Four photovoltaic cells in a steel frame, with a soft sky glare across them."""
    c = paint.Canvas(16, 16)
    frame = [(70, 72, 78), (110, 112, 118), (150, 152, 158)]
    for y in range(16):
        for x in range(16):
            if x in (0, 15) or y in (0, 15):
                c.set(x, y, frame[1] if x == 0 or y == 0 else frame[0])
            elif x in (7, 8) or y in (7, 8):
                c.set(x, y, frame[2] if x == 7 or y == 7 else frame[0])
            else:
                # Cells: deep blue with thin silver bus lines.
                base = (26, 46, 108) if (x + y) % 2 else (30, 52, 118)
                if y in (3, 12):
                    base = (90, 106, 150)
                glare = max(0, 1 - abs((x - y) - 2) / 3.0)
                c.set(x, y, tuple(min(255, int(v + glare * 70)) for v in base))
    return c.img


def solar_side():
    c, pal = metal_panel("#5a5e66", 81, rivets=False)
    for x in range(16):
        c.set(x, 0, (26, 46, 108)); c.set(x, 1, (60, 84, 150))
    for x in range(16):
        for y in range(10, 16):
            if (x + y) % 5 == 0:
                c.set(x, y, pal[1])
    return c.img


def turbine_base_side():
    """A riveted steel housing with a hazard band and a small maintenance hatch."""
    c, pal = metal_panel("#6b4a35", 91, rivets=True)
    for x in range(16):
        for y in (13, 14):
            c.set(x, y, (232, 186, 48) if ((x + y) // 2) % 2 == 0 else (34, 32, 36))
        c.set(x, 12, pal[1])
    for y in range(3, 11):
        c.set(5, y, pal[1]); c.set(10, y, pal[5])
    for x in range(5, 11):
        c.set(x, 3, pal[1]); c.set(x, 10, pal[5])
    c.set(9, 7, (200, 200, 190))
    return c.img


def turbine_base_top():
    c, pal = metal_panel("#5d636b", 92, rivets=True)
    for y in range(5, 11):
        for x in range(5, 11):
            c.set(x, y, pal[1] if (x + y) % 2 else pal[2])
    return c.img


def burner_side():
    c, pal = metal_panel("#4f4a46", 101, rivets=True)
    # Heat-blued band and soot from the top.
    for x in range(16):
        c.set(x, 6, (70, 76, 110)); c.set(x, 7, (52, 56, 86))
    rng = random.Random(7)
    for _ in range(5):
        x = rng.randint(1, 14)
        for y in range(1, rng.randint(3, 6)):
            c.set(x, y, (30, 28, 28))
    return c.img


def burner_front(on):
    """A firebox door: a grate of glowing slots when lit, dark when cold, under a pressure gauge."""
    c, pal = metal_panel("#4f4a46", 102, rivets=True)
    # Door frame.
    for x in range(2, 14):
        c.set(x, 6, pal[5]); c.set(x, 14, pal[1])
    for y in range(6, 15):
        c.set(2, y, pal[5]); c.set(13, y, pal[1])
    # Grate slots.
    for y in range(7, 14):
        for x in range(3, 13):
            slot = x % 2 == 1
            if slot:
                if on:
                    t = (y - 7) / 6
                    col = (255, int(220 - 120 * t), int(80 - 60 * t)) if y > 8 else (255, 240, 160)
                else:
                    col = (24, 20, 20)
            else:
                col = pal[2]
            c.set(x, y, col)
    # Pressure gauge.
    for y in range(1, 6):
        for x in range(5, 11):
            d = math.hypot(x - 7.5, y - 3)
            if d < 2.8:
                c.set(x, y, (226, 220, 200) if d < 2.1 else (40, 36, 34))
    c.set(8, 2, (200, 40, 30) if on else (60, 60, 60))
    c.set(7, 3, (30, 30, 30))
    return c.img


def burner_top():
    c, pal = metal_panel("#4f4a46", 103, rivets=True)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.6:
                c.set(x, y, (18, 16, 16) if d < 2.6 else (36, 34, 34))
    return c.img


def chimney():
    c, pal = metal_panel("#3e3a38", 104, rivets=False)
    for y in range(16):
        for x in range(16):
            if y % 5 == 0:
                c.set(x, y, pal[1])
    return c.img


def battery_side(level):
    """A riveted case with a charge gauge: five bars, the lit ones green."""
    c, pal = metal_panel("#3f4652", 111, rivets=True)
    for y in range(3, 13):
        for x in range(5, 11):
            c.set(x, y, (14, 16, 20))
    for k in range(5):
        y0 = 11 - k * 2
        lit = k < level
        col = (90, 240, 120) if lit and level >= 3 else (240, 200, 60) if lit and level >= 2 else (240, 90, 60) if lit else (40, 46, 40)
        for x in range(6, 10):
            c.set(x, y0, col)
    for x in range(16):
        c.set(x, 0, (232, 186, 48) if (x // 2) % 2 == 0 else (34, 32, 36))
        c.set(x, 15, (232, 186, 48) if (x // 2) % 2 == 0 else (34, 32, 36))
    return c.img


def battery_front():
    c, pal = metal_panel("#3f4652", 112, rivets=True)
    for y in range(4, 12):
        for x in range(4, 12):
            d = math.hypot(x - 7.5, y - 7.5)
            if d < 3.8:
                c.set(x, y, (196, 120, 40) if d > 2.6 else (30, 30, 34))
    c.set(6, 7, (200, 200, 190)); c.set(9, 7, (200, 200, 190))
    return c.img


def battery_top():
    c, pal = metal_panel("#3f4652", 113, rivets=True)
    for (x0, col) in ((3, (200, 50, 40)), (10, (40, 40, 44))):
        for y in range(5, 11):
            for x in range(x0, x0 + 3):
                c.set(x, y, col)
    return c.img


def charger_top(on):
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            if d > 6.6:
                col = (150, 96, 120)
            elif d > 5.6:
                col = (230, 170, 196)
            else:
                col = (60, 52, 64)
                if abs(x - 7.5) < 1.2 or abs(y - 7.5) < 1.2:
                    col = (120, 240, 255) if on else (80, 90, 100)
                elif d < 2.2:
                    col = (180, 250, 255) if on else (90, 96, 104)
            c.set(x, y, col)
    return c.img


def charger_side():
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c.set(x, y, (196, 120, 150) if (x // 2) % 2 else (182, 106, 138))
    for x in range(16):
        c.set(x, 12, (196, 120, 40)); c.set(x, 13, (150, 90, 30))
    return c.img


# ===================================================================== GUI
def burner_gui():
    """176x174 panel: lava tank, flame, fuel and bucket slots, readout and energy gauge; lit flame sprite at 176,0."""
    W, H = 176, 174
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    rng = random.Random(5)
    body = [(44, 46, 52), (54, 56, 62), (60, 62, 68)]
    for y in range(H):
        for x in range(W):
            c = body[rng.randint(0, 2)] if rng.random() < 0.25 else body[1]
            if x in (0, W - 1) or y in (0, H - 1):
                c = (16, 16, 18)
            elif x == 1 or y == 1:
                c = (96, 98, 106)
            elif x == W - 2 or y == H - 2:
                c = (28, 28, 32)
            px[x, y] = c + (255,)
    for (x, y) in ((0, 0), (W - 1, 0), (0, H - 1), (W - 1, H - 1)):
        px[x, y] = (0, 0, 0, 0)
    # Rust streaks and corner rivets.
    for _ in range(6):
        x = rng.randint(4, W - 5)
        for y in range(2, rng.randint(6, 14)):
            px[x, y] = (98, 58, 34, 255)
    for (x, y) in ((4, 4), (W - 6, 4), (4, H - 6), (W - 6, H - 6)):
        px[x, y] = (140, 140, 146, 255); px[x + 1, y + 1] = (20, 20, 22, 255)

    def inset(x0, y0, x1, y1, fill=(20, 21, 24)):
        for y in range(y0, y1):
            for x in range(x0, x1):
                c = fill
                if x == x0 or y == y0:
                    c = (14, 14, 16)
                elif x == x1 - 1 or y == y1 - 1:
                    c = (110, 112, 120)
                px[x, y] = c + (255,)

    def slot(x, y):
        for yy in range(18):
            for xx in range(18):
                c = (139, 139, 139)
                if xx == 0 or yy == 0:
                    c = (55, 55, 55)
                elif xx == 17 or yy == 17:
                    c = (255, 255, 255)
                px[x + xx, y + yy] = c + (255,)

    # Hazard band under the title.
    for x in range(4, W - 4):
        for y in (14, 15):
            px[x, y] = ((222, 172, 46) if ((x + y) // 3) % 2 == 0 else (30, 28, 30)) + (255,)
    inset(9, 18, 27, 78)          # lava tank
    inset(151, 18, 167, 78)       # energy gauge
    inset(64, 18, 148, 78, (24, 28, 26))  # readout screen
    # Gauge ticks.
    for k in range(1, 6):
        y = 18 + k * 10
        for x in (152, 153, 164, 165):
            px[x, y] = (90, 92, 100, 255)
        for x in (10, 11):
            px[x, y] = (90, 92, 100, 255)
    # Flame frame (empty flame silhouette), fuel slot and bucket slot.
    flame = [
        "......#.......",
        ".....##.......",
        ".....###......",
        "....####......",
        "....#####.#...",
        "...########...",
        "..##########..",
        "..##########..",
        ".############.",
        ".############.",
        ".############.",
        "..##########..",
        "...########...",
        "....######....",
    ]
    for yy, row in enumerate(flame):
        for xx, ch in enumerate(row):
            if ch == "#":
                px[46 + xx, 19 + yy] = (34, 34, 38, 255)
                t = yy / 13
                px[176 + xx, yy] = (255, int(230 - 150 * t), int(120 - 100 * t), 255)
    slot(43, 35)
    slot(43, 57)
    bucket = ["#......#", "#......#", ".#....#.", ".######."]
    for yy, row in enumerate(bucket):
        for xx, ch in enumerate(row):
            if ch == "#":
                px[48 + xx, 64 + yy] = (118, 118, 118, 255)
    for r in range(3):
        for col in range(9):
            slot(7 + col * 18, 91 + r * 18)
    for col in range(9):
        slot(7 + col * 18, 149)
    return img


# ===================================================================== models
def generate(assets, data, lang):
    tex = os.path.join(assets, "textures/block")
    _save(solar_top(), os.path.join(tex, "solar_panel_top.png"))
    _save(solar_side(), os.path.join(tex, "solar_panel_side.png"))
    _save(turbine_base_side(), os.path.join(tex, "wind_turbine_side.png"))
    _save(turbine_base_top(), os.path.join(tex, "wind_turbine_top.png"))
    _save(burner_side(), os.path.join(tex, "burner_generator_side.png"))
    _save(burner_front(False), os.path.join(tex, "burner_generator_front.png"))
    _save(burner_front(True), os.path.join(tex, "burner_generator_front_on.png"))
    _save(burner_top(), os.path.join(tex, "burner_generator_top.png"))
    _save(chimney(), os.path.join(tex, "burner_generator_chimney.png"))
    _save(burner_gui(), os.path.join(assets, "textures/gui/burner_generator.png"))
    for lvl in range(5):
        _save(battery_side(lvl), os.path.join(tex, "battery_side_%d.png" % lvl))
    _save(battery_front(), os.path.join(tex, "battery_front.png"))
    _save(battery_top(), os.path.join(tex, "battery_top.png"))
    _save(charger_top(False), os.path.join(tex, "cub_charger_top.png"))
    _save(charger_top(True), os.path.join(tex, "cub_charger_top_on.png"))
    _save(charger_side(), os.path.join(tex, "cub_charger_side.png"))

    models = os.path.join(assets, "models")
    allf = ("north", "south", "east", "west", "up", "down")

    def faces(t, up=None, down=None):
        return {d: {"texture": up if d == "up" and up else down if d == "down" and down else t} for d in allf}

    # Solar panel: a frame on four legs with the panel on top.
    _json(os.path.join(models, "block/solar_panel.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "golemgarage:block/solar_panel_side", "top": "golemgarage:block/solar_panel_top",
                     "side": "golemgarage:block/solar_panel_side"},
        "elements": [
            {"from": [1, 0, 1], "to": [3, 4, 3], "faces": faces("#side")},
            {"from": [13, 0, 1], "to": [15, 4, 3], "faces": faces("#side")},
            {"from": [1, 0, 13], "to": [3, 4, 15], "faces": faces("#side")},
            {"from": [13, 0, 13], "to": [15, 4, 15], "faces": faces("#side")},
            {"from": [0, 4, 0], "to": [16, 7, 16], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#side"},
                "north": {"texture": "#side", "uv": [0, 0, 16, 3]}, "south": {"texture": "#side", "uv": [0, 0, 16, 3]},
                "east": {"texture": "#side", "uv": [0, 0, 16, 3]}, "west": {"texture": "#side", "uv": [0, 0, 16, 3]}}},
        ]})
    _json(os.path.join(assets, "blockstates/solar_panel.json"), {"variants": {"": {"model": "golemgarage:block/solar_panel"}}})
    _json(os.path.join(models, "item/solar_panel.json"), {"parent": "golemgarage:block/solar_panel"})

    # Wind turbine base: a housing, slightly inset. Mast and rotor come from the block entity renderer.
    _json(os.path.join(models, "block/wind_turbine.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "golemgarage:block/wind_turbine_side", "side": "golemgarage:block/wind_turbine_side",
                     "top": "golemgarage:block/wind_turbine_top"},
        "elements": [
            {"from": [0, 0, 0], "to": [16, 3, 16], "faces": faces("#top")},
            {"from": [1, 3, 1], "to": [15, 16, 15], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#top"},
                "north": {"texture": "#side"}, "south": {"texture": "#side"}, "east": {"texture": "#side"},
                "west": {"texture": "#side"}}},
        ]})
    _json(os.path.join(assets, "blockstates/wind_turbine.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/wind_turbine"}, **({"y": r} if r else {}))
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _json(os.path.join(models, "item/wind_turbine.json"), {
        "parent": "minecraft:builtin/entity", "textures": {"particle": "golemgarage:block/wind_turbine_side"},
        "gui_light": "side"})

    # Scrap Burner: firebox with a chimney.
    for on in (False, True):
        _json(os.path.join(models, "block/burner_generator%s.json" % ("_on" if on else "")), {
            "parent": "minecraft:block/block",
            "textures": {"particle": "golemgarage:block/burner_generator_side",
                         "side": "golemgarage:block/burner_generator_side",
                         "front": "golemgarage:block/burner_generator_front" + ("_on" if on else ""),
                         "top": "golemgarage:block/burner_generator_top",
                         "pipe": "golemgarage:block/burner_generator_chimney"},
            "elements": [
                {"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
                    "north": {"texture": "#front", "cullface": "north"}, "south": {"texture": "#side", "cullface": "south"},
                    "east": {"texture": "#side", "cullface": "east"}, "west": {"texture": "#side", "cullface": "west"},
                    "up": {"texture": "#top", "cullface": "up"}, "down": {"texture": "#top", "cullface": "down"}}},
                {"from": [5, 16, 5], "to": [11, 22, 11], "faces": dict(
                    {d: {"texture": "#pipe", "uv": [5, 0, 11, 6]} for d in ("north", "south", "east", "west")},
                    up={"texture": "#top", "uv": [5, 5, 11, 11]})},
                {"from": [4, 22, 4], "to": [12, 23, 12], "faces": dict(
                    {d: {"texture": "#pipe", "uv": [4, 0, 12, 1]} for d in ("north", "south", "east", "west")},
                    up={"texture": "#pipe", "uv": [4, 4, 12, 12]}, down={"texture": "#pipe", "uv": [4, 4, 12, 12]})},
            ]})
    _json(os.path.join(assets, "blockstates/burner_generator.json"), {"variants": {
        "facing=%s,lit=%s" % (f, str(on).lower()): dict(
            {"model": "golemgarage:block/burner_generator" + ("_on" if on else "")}, **({"y": r} if r else {}))
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270)) for on in (False, True)}})
    _json(os.path.join(models, "item/burner_generator.json"), {"parent": "golemgarage:block/burner_generator"})

    # Battery: the plug on the front, the gauge on the sides.
    for lvl in range(5):
        _json(os.path.join(models, "block/battery_%d.json" % lvl), {
            "parent": "minecraft:block/orientable_with_bottom",
            "textures": {"front": "golemgarage:block/battery_front", "side": "golemgarage:block/battery_side_%d" % lvl,
                         "top": "golemgarage:block/battery_top", "bottom": "golemgarage:block/battery_top"}})
    rots = {"north": (0, 0), "east": (0, 90), "south": (0, 180), "west": (0, 270), "up": (270, 0), "down": (90, 0)}
    _json(os.path.join(assets, "blockstates/battery.json"), {"variants": {
        "facing=%s,charge=%d" % (f, lvl): dict({"model": "golemgarage:block/battery_%d" % lvl},
                                               **({"x": r[0]} if r[0] else {}), **({"y": r[1]} if r[1] else {}))
        for f, r in rots.items() for lvl in range(5)}})
    _json(os.path.join(models, "item/battery.json"), {"parent": "golemgarage:block/battery_2"})
    for on in (False, True):
        _json(os.path.join(models, "block/cub_charger%s.json" % ("_on" if on else "")), {
            "parent": "minecraft:block/block",
            "textures": {"particle": "golemgarage:block/cub_charger_side", "side": "golemgarage:block/cub_charger_side",
                         "top": "golemgarage:block/cub_charger_top" + ("_on" if on else "")},
            "elements": [{"from": [1, 0, 1], "to": [15, 4, 15], "faces": {
                "up": {"texture": "#top"}, "down": {"texture": "#side"},
                "north": {"texture": "#side", "uv": [1, 10, 15, 14]}, "south": {"texture": "#side", "uv": [1, 10, 15, 14]},
                "east": {"texture": "#side", "uv": [1, 10, 15, 14]}, "west": {"texture": "#side", "uv": [1, 10, 15, 14]}}}]})
    _json(os.path.join(assets, "blockstates/cub_charger.json"), {"variants": {
        "lit=false": {"model": "golemgarage:block/cub_charger"}, "lit=true": {"model": "golemgarage:block/cub_charger_on"}}})
    _json(os.path.join(models, "item/cub_charger.json"), {"parent": "golemgarage:block/cub_charger_on"})
    # The turbine's invisible mast: no model, just break particles.
    _json(os.path.join(models, "block/turbine_mast.json"), {"textures": {"particle": "golemgarage:block/wind_turbine_side"}})
    _json(os.path.join(assets, "blockstates/turbine_mast.json"), {"variants": {"": {"model": "golemgarage:block/turbine_mast"}}})
    lang["block.%s.turbine_mast" % MODID] = "Wind Turbine"

    for b in BLOCKS:
        _json(os.path.join(data, MODID, "loot_table/blocks", b + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [dict({"type": "minecraft:item", "name": "%s:%s" % (MODID, b)},
                **({"functions": [{"function": "minecraft:copy_components", "source": "block_entity",
                                   "include": ["minecraft:custom_data"]}]} if b == "battery" else {}))],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
        lang["block.%s.%s" % (MODID, b)] = NAMES[b]
    for rid, recipe in RECIPES.items():
        _json(os.path.join(data, MODID, "recipe", rid + ".json"), recipe)
    tag = os.path.join(data, "minecraft/tags/block/mineable/pickaxe.json")
    with open(tag) as fh:
        t = json.load(fh)
    for b in BLOCKS:
        if "%s:%s" % (MODID, b) not in t["values"]:
            t["values"].append("%s:%s" % (MODID, b))
    _json(tag, t)
