"""Block textures and models: Garage controller, laser/steel emitters, signal plate."""
import json
import os
import random

from PIL import Image

import paint


def _save(img, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)


def _json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def metal_panel(base_hex, seed, rivets=True, border=True):
    rng = random.Random(seed)
    pal = paint.ramp(base_hex)
    c = paint.Canvas(16, 16)
    for y in range(16):
        streak = 0
        for x in range(16):
            if x == 0 or rng.random() < 0.2:
                streak = rng.choice([0, 0, 1, -1])
            c.set(x, y, pal[3 + streak])
    if border:
        for i in range(16):
            c.set(i, 0, pal[5]); c.set(0, i, pal[4])
            c.set(i, 15, pal[1]); c.set(15, i, pal[2])
    if rivets:
        for (x, y) in ((2, 2), (12, 2), (2, 12), (12, 12)):
            c.set(x, y, pal[6]); c.set(x + 1, y + 1, pal[1])
    return c, pal


def controller():
    c, pal = metal_panel("#5d636b", 11)
    # Hazard stripes along the bottom.
    for x in range(16):
        for y in (12, 13, 14):
            yellow = ((x + y) // 2) % 2 == 0
            c.set(x, y, (232, 186, 48) if yellow else (34, 32, 36))
    for x in range(16):
        c.set(x, 11, pal[1])
    # Screen with a golem-head icon and status lights.
    for x in range(3, 13):
        for y in range(2, 10):
            c.set(x, y, (16, 30, 38))
    for x in range(3, 13):
        c.set(x, 2, pal[1]); c.set(x, 9, pal[5])
    for (x, y) in ((6, 4), (7, 4), (8, 4), (9, 4), (6, 5), (9, 5), (6, 6), (7, 6), (8, 6), (9, 6), (7, 7), (8, 7)):
        c.set(x, y, (90, 220, 240))
    c.set(7, 5, (255, 90, 70)); c.set(8, 5, (255, 90, 70))
    c.set(4, 8, (90, 230, 110)); c.set(11, 8, (255, 190, 60))
    return c.img


def controller_top():
    c, pal = metal_panel("#5d636b", 12)
    for i in range(3, 13):
        c.set(i, 7, pal[1]); c.set(i, 8, pal[5])
    return c.img


def emitter_side(seed):
    c, pal = metal_panel("#4a4f57", seed, rivets=False)
    for y in range(3, 13, 2):
        for x in range(3, 13):
            c.set(x, y, pal[0])
    return c.img


def lens(color_hex):
    pal = paint.ramp(color_hex)
    c = paint.Canvas(16, 16)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            idx = 6 if d < 2 else 5 if d < 4 else 4 if d < 6 else 3
            c.set(x, y, pal[idx])
    c.set(5, 5, (255, 255, 255)); c.set(6, 5, (255, 255, 255)); c.set(5, 6, (255, 255, 255))
    return c.img


def emitter_plate(seed):
    c, pal = metal_panel("#6a7079", seed)
    for x in range(4, 12):
        c.set(x, 4, pal[1]); c.set(x, 11, pal[5])
    for y in range(4, 12):
        c.set(4, y, pal[1]); c.set(11, y, pal[5])
    return c.img


def signal_plate(on):
    c, pal = metal_panel("#9a6a48", 21, rivets=False)
    trace = (255, 60, 40) if on else (120, 30, 26)
    for x in range(3, 13):
        c.set(x, 7, trace)
    for y in range(3, 13):
        c.set(7, y, trace)
    for (x, y) in ((3, 7), (12, 7), (7, 3), (7, 12)):
        c.set(x, y, (255, 120, 90) if on else (150, 50, 40))
    c.set(7, 7, (255, 230, 200) if on else (170, 70, 60))
    return c.img


def emitter_model(lens_tex):
    side = "golemgarage:block/emitter_side"
    plate = "golemgarage:block/emitter_plate"
    lt = "golemgarage:block/" + lens_tex

    def faces(down, other):
        return {f: {"texture": other} for f in ("north", "south", "east", "west", "up")} | {"down": {"texture": down}}

    return {
        "parent": "minecraft:block/block",
        "textures": {"particle": side, "side": side, "plate": plate, "lens": lt},
        "elements": [
            {"from": [1, 13, 1], "to": [15, 16, 15], "faces": faces("#plate", "#side")},
            {"from": [4, 9, 4], "to": [12, 13, 12], "faces": faces("#side", "#side")},
            {"from": [3, 8, 3], "to": [13, 9, 13], "faces": faces("#side", "#side")},
            {"from": [5, 7.5, 5], "to": [11, 8, 11], "faces": faces("#lens", "#lens"),
             "neoforge_data": {"block_light": 15, "sky_light": 15}},
        ],
        "display": {
            "gui": {"rotation": [210, 225, 0], "translation": [0, 3, 0], "scale": [0.7, 0.7, 0.7]},
            "ground": {"rotation": [180, 0, 0], "translation": [0, 3, 0], "scale": [0.5, 0.5, 0.5]},
            "fixed": {"rotation": [180, 0, 0], "scale": [0.6, 0.6, 0.6]},
        },
    }


def generate(assets):
    tex = os.path.join(assets, "textures/block")
    _save(controller(), os.path.join(tex, "garage_controller.png"))
    _save(controller_top(), os.path.join(tex, "garage_controller_top.png"))
    _save(emitter_side(31), os.path.join(tex, "emitter_side.png"))
    _save(emitter_plate(32), os.path.join(tex, "emitter_plate.png"))
    _save(lens("#62e6f2"), os.path.join(tex, "lens_cyan.png"))
    _save(lens("#ffb23a"), os.path.join(tex, "lens_amber.png"))
    _save(signal_plate(False), os.path.join(tex, "signal_plate.png"))
    _save(signal_plate(True), os.path.join(tex, "signal_plate_on.png"))
    for old in ("laser_emitter.png", "steel_emitter.png"):
        p = os.path.join(tex, old)
        if os.path.exists(p):
            os.remove(p)

    models = os.path.join(assets, "models")
    _json(os.path.join(models, "block/garage_controller.json"), {
        "parent": "minecraft:block/cube_bottom_top",
        "textures": {"side": "golemgarage:block/garage_controller",
                     "top": "golemgarage:block/garage_controller_top",
                     "bottom": "golemgarage:block/garage_controller_top"}})
    _json(os.path.join(models, "block/laser_emitter.json"), emitter_model("lens_cyan"))
    _json(os.path.join(models, "block/steel_emitter.json"), emitter_model("lens_amber"))
    _json(os.path.join(models, "block/signal_plate.json"),
          {"parent": "minecraft:block/pressure_plate_up", "textures": {"texture": "golemgarage:block/signal_plate"}})
    _json(os.path.join(models, "block/signal_plate_down.json"),
          {"parent": "minecraft:block/pressure_plate_down", "textures": {"texture": "golemgarage:block/signal_plate_on"}})
    for b in ("garage_controller", "laser_emitter", "steel_emitter", "signal_plate"):
        _json(os.path.join(models, "item/%s.json" % b), {"parent": "golemgarage:block/%s" % b})

    bs = os.path.join(assets, "blockstates")
    for b in ("laser_emitter", "steel_emitter"):
        _json(os.path.join(bs, b + ".json"), {"variants": {"": {"model": "golemgarage:block/" + b}}})
    # The controller's screen faces into the room it was placed toward.
    _json(os.path.join(bs, "garage_controller.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/garage_controller"}, **({"y": r} if r else {}))
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _json(os.path.join(bs, "signal_plate.json"), {"variants": {
        "powered=false": {"model": "golemgarage:block/signal_plate"},
        "powered=true": {"model": "golemgarage:block/signal_plate_down"}}})
