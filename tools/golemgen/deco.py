"""
0.27 content: garage decorations (tyre stacks, oil drums, workbench, chain hoist, neon signs), trophies (Junk Titan
head, medals), rig gadgets (harpoon, smoke dispenser, spike strips, roof rack), the spike strip block and the jerry
can. Writes textures, models, blockstates, loot tables, recipes and names. Called from gen.py after power.py.
"""
import json
import os
import random

from PIL import Image

import paint
from blocks import metal_panel

MODID = "golemgarage"
ALLF = ("north", "south", "east", "west", "up", "down")
ROT = (("north", 0), ("east", 90), ("south", 180), ("west", 270))

DRUMS = {"red": "#a8321f", "blue": "#2d5c9c", "rust": "#7a4a26"}
NEON = {"open": ("OPEN", (90, 255, 120)), "garage": ("GARAGE", (255, 150, 40)), "scrap": ("SCRAP", (255, 90, 200))}

NAMES = {
    "block.%s.tyre_stack": "Tyre Stack",
    "block.%s.oil_drum_red": "Red Oil Drum",
    "block.%s.oil_drum_blue": "Blue Oil Drum",
    "block.%s.oil_drum_rust": "Rusty Oil Drum",
    "block.%s.workbench": "Garage Workbench",
    "block.%s.chain_hoist": "Chain Hoist",
    "block.%s.neon_open": "Neon Sign: Open",
    "block.%s.neon_garage": "Neon Sign: Garage",
    "block.%s.neon_scrap": "Neon Sign: Scrap",
    "block.%s.titan_head": "Junk Titan Head",
    "block.%s.spike_strip": "Spike Strip",
    "item.%s.rig_harpoon": "Rig Harpoon",
    "item.%s.rig_smoke": "Smoke Dispenser",
    "item.%s.rig_spikes": "Spike Dropper",
    "item.%s.rig_rack": "Roof Rack",
    "item.%s.jerry_can": "Jerry Can",
    "item.%s.legend_medal": "Legend Medal",
    "item.%s.arena_medal": "Arena Medal",
}

STRINGS = {
    "golemgarage.jerry_can.fuel": "Fuel: %s%%",
    "golemgarage.jerry_can.empty": "Empty",
    "golemgarage.jerry_can.tip": "Fill it at a Scrap Burner, then use it on a rig.",
    "golemgarage.jerry_can.full": "The can is full.",
    "golemgarage.jerry_can.no_fuel": "The burner has no lava or fuel to spare.",
    "golemgarage.medal.got": "%s for %s!",
    "golemgarage.medal.legend_line": "Reached Legend rank",
    "golemgarage.medal.arena_line": "Won %s arena bouts",
    "golemgarage.rig.rack": "Roof Rack",
    "golemgarage.rig.gadget_fitted.harpoon": "Harpoon fitted. Press Z while driving to fire it where you look.",
    "golemgarage.rig.gadget_fitted.smoke": "Smoke Dispenser fitted. Press V while driving to drop smoke.",
    "golemgarage.rig.gadget_fitted.spikes": "Spike Dropper fitted. Press B while driving to drop spikes.",
    "golemgarage.rig.gadget_fitted.rack": "Roof Rack fitted. Press R while aboard to open it.",
    "golemgarage.rig.no_gadget.harpoon": "This rig has no harpoon.",
    "golemgarage.rig.no_gadget.smoke": "This rig has no Smoke Dispenser.",
    "golemgarage.rig.no_gadget.spikes": "This rig has no Spike Dropper.",
    "golemgarage.rig.no_gadget.rack": "This rig has no Roof Rack.",
    "key.golemgarage.rig_harpoon": "Rig: fire harpoon",
    "key.golemgarage.rig_smoke": "Rig: drop smoke",
    "key.golemgarage.rig_spikes": "Rig: drop spikes",
    "key.golemgarage.rig_lights": "Rig: headlights on/off",
    "key.golemgarage.rig_rack": "Rig: open roof rack",
}

# Item icons are 3D renders made by icons2.py; these ids just need a flat item model.
ICON_IDS = ["rig_harpoon", "rig_smoke", "rig_spikes", "rig_rack", "jerry_can", "legend_medal", "arena_medal",
            "solar_panel", "cub_charger"]

RECIPES = {
    "tyre_stack": {"type": "minecraft:crafting_shapeless", "category": "decorations",
                   "ingredients": [{"item": "golemgarage:tyre"}],
                   "result": {"id": "golemgarage:tyre_stack", "count": 1}},
    "workbench": {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": ["III", "SCS", "S S"],
                  "key": {"I": {"tag": "c:ingots/iron"}, "S": {"item": "golemgarage:scrap"},
                          "C": {"item": "minecraft:crafting_table"}},
                  "result": {"id": "golemgarage:workbench", "count": 1}},
    "chain_hoist": {"type": "minecraft:crafting_shaped", "category": "decorations", "pattern": [" C ", "SIS", " H "],
                    "key": {"C": {"item": "minecraft:chain"}, "S": {"item": "golemgarage:scrap"},
                            "I": {"tag": "c:ingots/iron"}, "H": {"item": "minecraft:tripwire_hook"}},
                    "result": {"id": "golemgarage:chain_hoist", "count": 1}},
    "rig_harpoon": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IIF", "SCS"],
                    "key": {"I": {"tag": "c:ingots/iron"}, "F": {"item": "minecraft:flint"},
                            "C": {"item": "minecraft:crossbow"}, "S": {"item": "golemgarage:scrap"}},
                    "result": {"id": "golemgarage:rig_harpoon", "count": 1}},
    "rig_smoke": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["S S", "CGC", "SSS"],
                  "key": {"S": {"item": "golemgarage:scrap"}, "C": {"item": "minecraft:campfire"},
                          "G": {"item": "minecraft:gunpowder"}},
                  "result": {"id": "golemgarage:rig_smoke", "count": 1}},
    "rig_spikes": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["DDD", "SIS"],
                   "key": {"D": {"item": "minecraft:pointed_dripstone"}, "S": {"item": "golemgarage:scrap"},
                           "I": {"tag": "c:ingots/iron"}},
                   "result": {"id": "golemgarage:rig_spikes", "count": 1}},
    "rig_rack": {"type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["IBI", "SCS"],
                 "key": {"I": {"tag": "c:ingots/iron"}, "B": {"item": "minecraft:iron_bars"},
                         "S": {"item": "golemgarage:scrap"}, "C": {"tag": "c:chests/wooden"}},
                 "result": {"id": "golemgarage:rig_rack", "count": 1}},
    "jerry_can": {"type": "minecraft:crafting_shaped", "category": "tools", "pattern": [" I ", "SBS", "SSS"],
                  "key": {"I": {"tag": "c:ingots/iron"}, "S": {"item": "golemgarage:scrap"},
                          "B": {"item": "minecraft:bucket"}},
                  "result": {"id": "golemgarage:jerry_can", "count": 1}},
}
for _c, _d in (("red", "minecraft:red_dye"), ("blue", "minecraft:blue_dye"), ("rust", "minecraft:iron_nugget")):
    RECIPES["oil_drum_" + _c] = {
        "type": "minecraft:crafting_shaped", "category": "decorations", "pattern": ["SDS", "S S", "SSS"],
        "key": {"S": {"item": "golemgarage:scrap"}, "D": {"item": _d}},
        "result": {"id": "golemgarage:oil_drum_" + _c, "count": 2}}
for _w, _d in (("open", "minecraft:lime_dye"), ("garage", "minecraft:orange_dye"), ("scrap", "minecraft:pink_dye")):
    RECIPES["neon_" + _w] = {
        "type": "minecraft:crafting_shaped", "category": "decorations", "pattern": ["GDG", "GLG", "SRS"],
        "key": {"G": {"tag": "c:glass_panes"}, "D": {"item": _d}, "L": {"item": "minecraft:glowstone_dust"},
                "S": {"item": "golemgarage:scrap"}, "R": {"tag": "c:dusts/redstone"}},
        "result": {"id": "golemgarage:neon_" + _w, "count": 1}}


def _save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def _json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


# ------------------------------------------------------------------ textures
def tyre_side():
    rng = random.Random(5)
    pal = paint.RUBBER
    c = paint.Canvas(16, 16)
    for y in range(16):
        band = y % 4
        for x in range(16):
            v = pal[1] if band in (0, 3) else pal[3]
            if band in (1, 2) and (x + (y // 4) * 2) % 4 == 0:
                v = pal[2]  # tread blocks
            if band == 0:
                v = pal[0]
            if rng.random() < 0.06:
                v = pal[4]
            c.set(x, y, v)
    return c.img


def tyre_top():
    pal = paint.RUBBER
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            if r <= 3.2:
                c.set(x, y, (16, 14, 16))
            elif r <= 4.2:
                c.set(x, y, pal[4])
            else:
                c.set(x, y, pal[2] if (x + y) % 3 else pal[3])
    return c.img


def drum_side(hexc, seed):
    rng = random.Random(seed)
    pal = paint.ramp(hexc)
    rust = paint.ramp("#7a4a26")
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            shade = 3 + (1 if 3 <= x <= 6 else 0) - (1 if x >= 12 else 0)
            v = pal[shade]
            if y in (0, 15):
                v = pal[1]
            if y in (4, 11):
                v = pal[5]
            if y in (5, 12):
                v = pal[1]
            if rng.random() < 0.08 or (hexc == "#7a4a26" and rng.random() < 0.3):
                v = rust[rng.choice((2, 3, 4))]
            c.set(x, y, v)
    # A dent and a faded hazard label.
    for (x, y) in ((9, 7), (10, 7), (10, 8), (9, 8)):
        c.set(x, y, pal[1])
    for y in range(7, 10):
        for x in range(2, 7):
            c.set(x, y, (230, 200, 60) if (x + y) % 2 else (40, 36, 30))
    return c.img


def drum_top(hexc, lit):
    pal = paint.ramp(hexc)
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = (dx * dx + dy * dy) ** 0.5
            if lit:
                v = (255, 140, 40) if r < 3 else (200, 70, 20) if r < 5 else (70, 30, 20) if r < 6.3 else pal[1]
            else:
                v = pal[1] if r > 6.3 else pal[4] if r > 5.6 else pal[3]
            c.set(x, y, v)
    if not lit:
        for (x, y) in ((10, 4), (11, 4), (10, 5), (11, 5)):
            c.set(x, y, (40, 40, 44))
        c.set(4, 10, pal[5]); c.set(5, 10, pal[5])
    return c.img


def bench_top():
    c, pal = metal_panel("#6a6e72", 41, rivets=True)
    rng = random.Random(42)
    # Oil stains and a spanner outline.
    for _ in range(10):
        c.set(rng.randrange(1, 15), rng.randrange(1, 15), (40, 38, 36))
    for x in range(3, 11):
        c.set(x, 9, (200, 200, 206))
    for (x, y) in ((2, 8), (2, 10), (11, 8), (11, 10), (12, 9)):
        c.set(x, y, (200, 200, 206))
    return c.img


def bench_front():
    c, pal = metal_panel("#a8321f", 43, rivets=False)
    for y in (5, 10):
        for x in range(1, 15):
            c.set(x, y, pal[1])
    for y in (2, 7, 12):
        for x in (7, 8):
            c.set(x, y, (210, 210, 214))
    return c.img


def hazard():
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c.set(x, y, (226, 180, 48) if ((x + y) // 4) % 2 else (40, 36, 34))
    return c.img


def neon_board(word, color, lit):
    """32x32: a dark backing board with the word bent in glass tube, two rows of a 3x5 pixel font."""
    font = {
        "O": ["111", "101", "101", "101", "111"], "P": ["111", "101", "111", "100", "100"],
        "E": ["111", "100", "110", "100", "111"], "N": ["101", "111", "111", "111", "101"],
        "G": ["111", "100", "101", "101", "111"], "A": ["010", "101", "111", "101", "101"],
        "R": ["110", "101", "110", "101", "101"], "S": ["111", "100", "111", "001", "111"],
        "C": ["111", "100", "100", "100", "111"],
    }
    c = paint.Canvas(32, 32)
    for y in range(32):
        for x in range(32):
            edge = x in (0, 31) or y in (6, 25)
            if 6 <= y <= 25:
                c.set(x, y, (60, 60, 66) if edge else (22, 20, 26))
    scale = 1
    total = len(word) * 4 - 1
    x0 = (32 - total * scale) // 2
    y0 = 13
    tube = color if lit else tuple(int(v * 0.35) for v in color)
    halo = tuple(int(v * 0.45) for v in color) if lit else None
    pix = []
    for i, ch in enumerate(word):
        g = font[ch]
        for ry, row in enumerate(g):
            for rx, bit in enumerate(row):
                if bit == "1":
                    pix.append((x0 + i * 4 + rx, y0 + ry))
    if halo:
        for (x, y) in pix:
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                if (x + dx, y + dy) not in pix and 6 < y + dy < 25:
                    c.set(x + dx, y + dy, halo)
    for (x, y) in pix:
        c.set(x, y, tube)
    # An underline tube and a flicker-dot.
    for x in range(x0, x0 + total):
        c.set(x, y0 + 7, tube)
    return c.img


def neon_face(word, color):
    """32x32 transparent overlay with only the lit tubes (drawn full-bright on top of the board)."""
    img = neon_board(word, color, True)
    px = img.load()
    out = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
    po = out.load()
    for y in range(32):
        for x in range(32):
            r, g, b, a = px[x, y]
            if a and max(r, g, b) > 70 and (r, g, b) != (60, 60, 66):
                po[x, y] = (r, g, b, 255)
    return out


def titan_skin():
    rng = random.Random(77)
    rust = paint.ramp("#7a4a26")
    iron = paint.ramp("#5a5c60")
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            v = iron[rng.choice((2, 3, 3, 4))]
            if rng.random() < 0.35:
                v = rust[rng.choice((2, 3, 4, 5))]
            c.set(x, y, v)
    for (x, y) in ((2, 2), (13, 2), (2, 13), (13, 13)):
        c.set(x, y, iron[6]); c.set(x + 1, y + 1, iron[0])
    for x in range(16):
        c.set(x, 0, iron[1])
    return c.img


def titan_eye():
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            c.set(x, y, (255, 60, 30) if 4 <= x <= 11 and 5 <= y <= 10 else (200, 20, 16))
    return c.img


def spike_plate():
    c = paint.Canvas(16, 16)
    pal = paint.ramp("#55585e")
    for y in range(16):
        for x in range(16):
            c.set(x, y, pal[2] if (y // 2) % 2 else pal[3])
    for y in (2, 6, 10, 14):
        for x in range(1, 16, 4):
            c.set(x, y, pal[6]); c.set(x + 1, y, pal[5])
    return c.img


def cape(base, trim, emblem):
    """16x26 rank cape: cloth with folds, a trim, a ragged hem and an emblem."""
    pal = paint.ramp(base)
    tr = paint.hex_rgb(trim)
    c = paint.Canvas(16, 26)
    for y in range(26):
        for x in range(16):
            fold = (x // 3) % 2
            v = pal[3 + fold] if y > 0 else pal[1]
            if x in (0, 15) or y == 1:
                v = tr
            c.set(x, y, v)
    # A ragged hem at the bottom of the long capes, and at row 13 for the short one (it only shows the top part).
    for hem in ((13,) if emblem is None else (25,)):
        for x in range(0, 16, 3):
            c.set(x, hem, (0, 0, 0), 0)
            c.set(x + 1, hem, tr)
        for x in range(16):
            if c.get(x, hem)[3]:
                c.set(x, hem - 1, tr)
    if emblem == "chevron":
        for i in range(4):
            c.set(7 - i, 5 + i, tr); c.set(8 + i, 5 + i, tr)
            c.set(7 - i, 8 + i, tr); c.set(8 + i, 8 + i, tr)
    elif emblem == "crest":
        for (x, y) in ((7, 4), (8, 4), (6, 5), (9, 5), (5, 6), (10, 6), (5, 7), (10, 7), (6, 8), (9, 8), (7, 9), (8, 9),
                       (7, 6), (8, 6), (7, 7), (8, 7)):
            c.set(x, y, tr)
        c.set(7, 6, (255, 240, 160)); c.set(8, 7, (255, 240, 160))
    return c.img


# ------------------------------------------------------------------ models
def el(frm, to, tex, faces=ALLF, uv=None, **extra):
    f = {}
    for d in faces:
        face = {"texture": tex}
        if uv and d in uv:
            face["uv"] = uv[d]
        f[d] = face
    e = {"from": frm, "to": to, "faces": f}
    e.update(extra)
    return e


GLOW = {"neoforge_data": {"block_light": 15, "sky_light": 15}, "shade": False}


def generate(assets, data, lang):
    tex = os.path.join(assets, "textures/block")
    models = os.path.join(assets, "models")
    bs = os.path.join(assets, "blockstates")
    _save(tyre_side(), os.path.join(tex, "tyre_side.png"))
    _save(tyre_top(), os.path.join(tex, "tyre_top.png"))
    for col, hexc in DRUMS.items():
        _save(drum_side(hexc, len(col)), os.path.join(tex, "oil_drum_%s.png" % col))
        _save(drum_top(hexc, False), os.path.join(tex, "oil_drum_%s_top.png" % col))
        _save(drum_top(hexc, True), os.path.join(tex, "oil_drum_%s_top_lit.png" % col))
    _save(bench_top(), os.path.join(tex, "workbench_top.png"))
    _save(bench_front(), os.path.join(tex, "workbench_front.png"))
    _save(hazard(), os.path.join(tex, "hazard_paint.png"))
    for w, (word, color) in NEON.items():
        _save(neon_board(word, color, False), os.path.join(tex, "neon_%s_board.png" % w))
        _save(neon_face(word, color), os.path.join(tex, "neon_%s_tubes.png" % w))
    _save(titan_skin(), os.path.join(tex, "titan_skin.png"))
    _save(titan_eye(), os.path.join(tex, "titan_eye.png"))
    _save(spike_plate(), os.path.join(tex, "spike_strip.png"))
    golem = os.path.join(assets, "textures/entity/golem")
    _save(cape("#5e6b34", "#3a4220", None), os.path.join(golem, "cape_veteran.png"))
    _save(cape("#9c2420", "#2a1410", "chevron"), os.path.join(golem, "cape_elite.png"))
    _save(cape("#5a2a8c", "#f0c040", "crest"), os.path.join(golem, "cape_legend.png"))

    # Tyre stack: one model per height.
    for n in range(1, 5):
        _json(os.path.join(models, "block/tyre_stack_%d.json" % n), {
            "parent": "minecraft:block/block",
            "textures": {"particle": "golemgarage:block/tyre_side", "side": "golemgarage:block/tyre_side",
                         "top": "golemgarage:block/tyre_top"},
            "elements": [el([1, 4 * i, 1], [15, 4 * i + 4, 15], "#side",
                            uv={d: [0, 4 * (i % 4), 16, 4 * (i % 4) + 4] for d in ("north", "south", "east", "west")})
                         | {"faces": {**{d: {"texture": "#side", "uv": [1, 4 * (i % 4), 15, 4 * (i % 4) + 4]}
                                         for d in ("north", "south", "east", "west")},
                                      "up": {"texture": "#top"}, "down": {"texture": "#top"}}}
                         for i in range(n)]})
    _json(os.path.join(bs, "tyre_stack.json"),
          {"variants": {"tyres=%d" % n: {"model": "golemgarage:block/tyre_stack_%d" % n} for n in range(1, 5)}})
    _json(os.path.join(models, "item/tyre_stack.json"), {"parent": "golemgarage:block/tyre_stack_2"})

    # Oil drums: a ribbed barrel; lit ones carry a fire.
    for col in DRUMS:
        for lit in (False, True):
            texs = {"particle": "golemgarage:block/oil_drum_" + col, "side": "golemgarage:block/oil_drum_" + col,
                    "top": "golemgarage:block/oil_drum_%s_top%s" % (col, "_lit" if lit else "")}
            elements = [
                {"from": [2, 0, 2], "to": [14, 16, 14], "faces": {
                    **{d: {"texture": "#side", "uv": [2, 0, 14, 16]} for d in ("north", "south", "east", "west")},
                    "up": {"texture": "#top", "uv": [2, 2, 14, 14]}, "down": {"texture": "#top", "uv": [2, 2, 14, 14]}}},
                el([1.6, 4, 1.6], [14.4, 5, 14.4], "#side", faces=("north", "south", "east", "west"),
                   uv={d: [0, 4, 16, 5] for d in ("north", "south", "east", "west")}),
                el([1.6, 11, 1.6], [14.4, 12, 14.4], "#side", faces=("north", "south", "east", "west"),
                   uv={d: [0, 11, 16, 12] for d in ("north", "south", "east", "west")}),
            ]
            if lit:
                texs["fire"] = "minecraft:block/fire_0"
                for rot in (45, -45):
                    elements.append({"from": [2, 13, 8], "to": [14, 27, 8], "shade": False,
                                     "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": rot},
                                     "faces": {"north": {"texture": "#fire"}, "south": {"texture": "#fire"}}})
            _json(os.path.join(models, "block/oil_drum_%s%s.json" % (col, "_lit" if lit else "")), {
                "parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": texs,
                "elements": elements})
        _json(os.path.join(bs, "oil_drum_%s.json" % col), {"variants": {
            "lit=false": {"model": "golemgarage:block/oil_drum_" + col},
            "lit=true": {"model": "golemgarage:block/oil_drum_%s_lit" % col}}})
        _json(os.path.join(models, "item/oil_drum_%s.json" % col), {"parent": "golemgarage:block/oil_drum_" + col})

    # Workbench: steel top, drawer front, legs, a vice and a pegboard of tools at the back.
    _json(os.path.join(models, "block/workbench.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "golemgarage:block/workbench_top", "top": "golemgarage:block/workbench_top",
                     "front": "golemgarage:block/workbench_front", "metal": "golemgarage:block/emitter_side",
                     "board": "minecraft:block/spruce_planks", "iron": "minecraft:block/iron_block",
                     "tool": "minecraft:block/anvil"},
        "elements": [
            el([0, 12, 0], [16, 14, 16], "#top"),
            el([1, 2, 1], [9, 12, 2], "#front", faces=("north",), uv={"north": [1, 2, 15, 14]}),
            el([1, 2, 2], [9, 12, 15], "#metal", faces=("south", "east", "west", "up", "down")),
            el([12, 0, 1], [15, 12, 3], "#metal"), el([12, 0, 13], [15, 12, 15], "#metal"),
            el([1, 0, 1], [3, 2, 3], "#metal"), el([1, 0, 13], [3, 2, 15], "#metal"),
            el([10, 3, 2], [15, 4, 14], "#board"),
            el([0, 14, 13], [16, 16, 16], "#board"),
            el([3, 14, 3], [7, 16, 7], "#tool"), el([2, 14.5, 4], [3, 15.5, 6], "#iron"),
            el([10, 14, 6], [13, 14.6, 9], "#iron"),
        ]})
    _json(os.path.join(bs, "workbench.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/workbench"}, **({"y": r} if r else {})) for f, r in ROT}})
    _json(os.path.join(models, "item/workbench.json"), {"parent": "golemgarage:block/workbench"})

    # Chain hoist: chain up to the ceiling, a yellow block and tackle, chain down to a hook.
    chain = lambda y0, y1: [  # noqa: E731
        {"from": [6.5, y0, 8], "to": [9.5, y1, 8], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
         "faces": {"north": {"texture": "#chain", "uv": [0, 0, 3, y1 - y0]}, "south": {"texture": "#chain", "uv": [0, 0, 3, y1 - y0]}}},
        {"from": [8, y0, 6.5], "to": [8, y1, 9.5], "rotation": {"origin": [8, 8, 8], "axis": "y", "angle": 45},
         "faces": {"east": {"texture": "#chain", "uv": [3, 0, 6, y1 - y0]}, "west": {"texture": "#chain", "uv": [3, 0, 6, y1 - y0]}}}]
    _json(os.path.join(models, "block/chain_hoist.json"), {
        "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
        "textures": {"particle": "golemgarage:block/hazard_paint", "body": "golemgarage:block/hazard_paint",
                     "chain": "minecraft:block/chain", "metal": "golemgarage:block/emitter_side",
                     "hook": "minecraft:block/iron_block"},
        "elements": chain(13, 16) + [
            el([5, 8, 5], [11, 13, 11], "#body"),
            el([4, 9, 6], [5, 12, 10], "#metal"), el([11, 9, 6], [12, 12, 10], "#metal"),
        ] + chain(3, 8) + [
            el([7, 1, 7.5], [9, 3, 8.5], "#hook"), el([6, 0, 7.5], [7, 2, 8.5], "#hook"),
            el([9, 0, 7.5], [10, 1, 8.5], "#hook"),
        ]})
    _json(os.path.join(bs, "chain_hoist.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/chain_hoist"}, **({"y": r} if r else {})) for f, r in ROT}})
    _json(os.path.join(models, "item/chain_hoist.json"), {"parent": "golemgarage:block/chain_hoist"})

    # Neon signs: a board on the wall behind (south side for a north-facing sign) and glowing tubes in front.
    for w in NEON:
        for lit in (False, True):
            elements = [{"from": [0, 3, 14.5], "to": [16, 13, 16], "faces": {
                "north": {"texture": "#board", "uv": [0, 3, 16, 13]},
                "south": {"texture": "#back"}, "east": {"texture": "#back"}, "west": {"texture": "#back"},
                "up": {"texture": "#back"}, "down": {"texture": "#back"}}}]
            if lit:
                elements.append(dict({"from": [0, 3, 14.4], "to": [16, 13, 14.4], "faces": {
                    "north": {"texture": "#tubes", "uv": [0, 3, 16, 13]}}}, **GLOW))
            _json(os.path.join(models, "block/neon_%s%s.json" % (w, "" if lit else "_off")), {
                "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                "textures": {"particle": "golemgarage:block/neon_%s_board" % w,
                             "board": "golemgarage:block/neon_%s_board" % w,
                             "tubes": "golemgarage:block/neon_%s_tubes" % w,
                             "back": "golemgarage:block/emitter_side"},
                "elements": elements})
        _json(os.path.join(bs, "neon_%s.json" % w), {"variants": {
            "facing=%s,lit=%s" % (f, str(lit).lower()): dict(
                {"model": "golemgarage:block/neon_%s%s" % (w, "" if lit else "_off")}, **({"y": r} if r else {}))
            for f, r in ROT for lit in (False, True)}})
        _json(os.path.join(models, "item/neon_%s.json" % w), {"parent": "golemgarage:block/neon_" + w})

    # Junk Titan Head: a rusted head on a plinth, eyes still glowing.
    _json(os.path.join(models, "block/titan_head.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "golemgarage:block/titan_skin", "skin": "golemgarage:block/titan_skin",
                     "eye": "golemgarage:block/titan_eye", "plinth": "minecraft:block/dark_oak_planks",
                     "metal": "golemgarage:block/emitter_side"},
        "elements": [
            el([1, 0, 1], [15, 2, 15], "#plinth"),
            el([3, 2, 3], [13, 13, 13], "#skin"),
            el([2, 9, 2.5], [14, 11, 13], "#skin"),
            el([4, 2, 2], [12, 5, 3], "#metal"),
            el([7, 5, 2], [9, 9, 3], "#skin"),
            el([5, 13, 7], [6, 15.5, 8], "#metal"), el([10, 13, 7], [11, 15, 8], "#metal"),
            dict(el([4, 6.5, 2.9], [7, 8.5, 3], "#eye", faces=("north",)), **GLOW),
            dict(el([9, 6.5, 2.9], [12, 8.5, 3], "#eye", faces=("north",)), **GLOW),
        ]})
    _json(os.path.join(bs, "titan_head.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/titan_head"}, **({"y": r} if r else {})) for f, r in ROT}})
    _json(os.path.join(models, "item/titan_head.json"), {"parent": "golemgarage:block/titan_head"})

    # Spike strip: a low plate bristling with spikes.
    spikes = []
    for x in (2, 6, 10, 14):
        for z in (2, 6, 10, 14):
            spikes.append(el([x - 0.5, 1, z - 0.5], [x + 0.5, 3, z + 0.5], "#spike"))
    _json(os.path.join(models, "block/spike_strip.json"), {
        "parent": "minecraft:block/block",
        "textures": {"particle": "golemgarage:block/spike_strip", "plate": "golemgarage:block/spike_strip",
                     "spike": "minecraft:block/iron_block"},
        "elements": [el([0, 0, 0], [16, 1, 16], "#plate")] + spikes})
    _json(os.path.join(bs, "spike_strip.json"), {"variants": {"": {"model": "golemgarage:block/spike_strip"}}})
    _json(os.path.join(models, "item/spike_strip.json"), {"parent": "golemgarage:block/spike_strip"})

    # Item icons.
    for iid in ICON_IDS:
        _json(os.path.join(models, "item", iid + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/%s" % (MODID, iid)}})

    # Loot tables: decorations drop themselves; a tyre stack drops its tyres.
    loot = os.path.join(data, MODID, "loot_table/blocks")
    for b in ["workbench", "chain_hoist", "titan_head"] + ["oil_drum_" + c for c in DRUMS] + ["neon_" + w for w in NEON]:
        _json(os.path.join(loot, b + ".json"), {"type": "minecraft:block", "pools": [{
            "rolls": 1, "entries": [{"type": "minecraft:item", "name": "%s:%s" % (MODID, b)}],
            "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    _json(os.path.join(loot, "tyre_stack.json"), {"type": "minecraft:block", "pools": [{
        "rolls": 1, "entries": [{"type": "minecraft:item", "name": "golemgarage:tyre", "functions": [
            {"function": "minecraft:set_count", "count": n, "conditions": [
                {"condition": "minecraft:block_state_property", "block": "golemgarage:tyre_stack",
                 "properties": {"tyres": str(n)}}]} for n in range(1, 5)]}]}]})

    for rid, recipe in RECIPES.items():
        _json(os.path.join(data, MODID, "recipe", rid + ".json"), recipe)
    for k, v in NAMES.items():
        lang[k % MODID] = v
    lang.update(STRINGS)
