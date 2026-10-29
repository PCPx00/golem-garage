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


def dock_top(lit):
    """Charging pad: steel plate, hazard border, a ring of contacts and a glowing core."""
    c, pal = metal_panel("#5b6168", 41, rivets=False)
    for i in range(16):
        for j in (0, 1, 14, 15):
            yellow = ((i + j) // 2) % 2 == 0
            c.set(i, j, (232, 186, 48) if yellow else (34, 32, 36))
            c.set(j, i, (232, 186, 48) if ((i + j) // 2) % 2 == 0 else (34, 32, 36))
    core = (110, 240, 255) if lit else (40, 92, 104)
    ring = (70, 190, 220) if lit else (30, 60, 70)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.2 < d < 5.2:
                c.set(x, y, ring)
            elif d < 2.2:
                c.set(x, y, core)
            elif d < 2.9:
                c.set(x, y, pal[1])
    c.set(7, 6, (230, 255, 255) if lit else (70, 130, 140))
    return c.img


def dock_side():
    c, pal = metal_panel("#4a4f57", 42, rivets=False, border=False)
    for x in range(16):
        c.set(x, 12, pal[5])
        for y in (13, 14):
            c.set(x, y, (232, 186, 48) if ((x + y) // 2) % 2 == 0 else (34, 32, 36))
        c.set(x, 15, pal[0])
    return c.img


def dock_model(top):
    face = lambda tex, uv: {"texture": tex, "uv": uv}
    return {"parent": "minecraft:block/block", "textures": {"particle": "golemgarage:block/" + top,
                                                           "top": "golemgarage:block/" + top,
                                                           "side": "golemgarage:block/golem_dock_side",
                                                           "bottom": "golemgarage:block/emitter_side"},
            "elements": [{"from": [0, 0, 0], "to": [16, 3, 16], "faces": {
                "down": face("#bottom", [0, 0, 16, 16]), "up": face("#top", [0, 0, 16, 16]),
                "north": face("#side", [0, 12, 16, 15]), "south": face("#side", [0, 12, 16, 15]),
                "east": face("#side", [0, 12, 16, 15]), "west": face("#side", [0, 12, 16, 15])}}]}


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


def door_texture():
    """64x32 sheet for the roller door renderer: slat, bottom bar, drum housing, rail."""
    img = Image.new("RGBA", (64, 32), (0, 0, 0, 0))
    pal = paint.ramp("#7d848c")
    dark = paint.ramp("#4a4f57")
    px = img.load()
    rng = random.Random(77)
    # Slat (v 0-4): a ridged steel strip with a groove at its bottom.
    for x in range(16):
        n = rng.choice([0, 0, 1, -1])
        for y, idx in ((0, 5), (1, 4), (2, 3), (3, 1)):
            px[x, y] = tuple(pal[max(0, min(6, idx + n))]) + (255,)
    # Bottom bar (v 4-8): thicker bar, a handle and a rubber seal.
    for x in range(16):
        for y, idx in ((4, 5), (5, 3), (6, 2)):
            px[x, y] = tuple(pal[idx]) + (255,)
        px[x, 7] = (28, 28, 30, 255)
    for x in range(6, 10):
        px[x, 5] = tuple(dark[1]) + (255,)
    px[6, 6] = px[9, 6] = tuple(dark[0]) + (255,)
    # Housing (u 16-32, v 0-16): painted box with hazard stripes along its lower half.
    for x in range(16, 32):
        for y in range(0, 16):
            px[x, y] = tuple(dark[3 if y % 7 else 2]) + (255,)
        for y in range(9, 14):
            yellow = ((x + y) // 2) % 2 == 0
            px[x, y] = (232, 186, 48, 255) if yellow else (34, 32, 36, 255)
        px[x, 0] = tuple(dark[5]) + (255,)
        px[x, 15] = tuple(dark[0]) + (255,)
    # Rail (u 32-36): dark channel steel.
    for x in range(32, 36):
        for y in range(16):
            px[x, y] = tuple(dark[4 if x in (32, 35) else 1]) + (255,)
    return img


def door_icon():
    pal = paint.ramp("#7d848c")
    dark = paint.ramp("#4a4f57")
    c = paint.Canvas(16, 16)
    for x in range(1, 15):
        c.set(x, 1, dark[2]); c.set(x, 2, dark[3])
        for y in range(3, 14):
            c.set(x, y, pal[(5, 4, 3)[(y - 3) % 3]] if (y - 3) % 3 != 2 else pal[1])
        c.set(x, 14, (28, 28, 30))
    for y in range(1, 15):
        c.set(0, y, dark[1]); c.set(15, y, dark[1])
    for x in range(1, 15):
        if ((x + 1) // 2) % 2 == 0:
            c.set(x, 2, (232, 186, 48))
    c.set(7, 12, dark[0]); c.set(8, 12, dark[0])
    return c.img


def window():
    pal = paint.ramp("#6a7079")
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    px = img.load()
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            if edge:
                px[x, y] = tuple(pal[4 if x == 0 or y == 0 else 2]) + (255,)
            else:
                px[x, y] = (150, 200, 220, 70)
    # Glare streaks.
    for i in range(3, 9):
        px[i, 12 - i + 2] = (230, 245, 255, 150)
    for i in range(8, 12):
        px[i, 21 - i - 4] = (230, 245, 255, 110)
    for (x, y) in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = tuple(pal[6]) + (255,)
    return img


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
    _save(dock_top(False), os.path.join(tex, "golem_dock_top.png"))
    _save(dock_top(True), os.path.join(tex, "golem_dock_top_on.png"))
    _save(dock_side(), os.path.join(tex, "golem_dock_side.png"))
    _save(door_texture(), os.path.join(assets, "textures/entity/garage_door.png"))
    _save(door_icon(), os.path.join(assets, "textures/item/garage_door.png"))
    _save(window(), os.path.join(tex, "garage_window.png"))
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
    _json(os.path.join(models, "block/golem_dock.json"), dock_model("golem_dock_top"))
    _json(os.path.join(models, "block/golem_dock_on.json"), dock_model("golem_dock_top_on"))
    # The roller door is drawn by its renderer; the block model only gives break particles.
    _json(os.path.join(models, "block/garage_door.json"), {"textures": {"particle": "golemgarage:block/emitter_side"}})
    _json(os.path.join(models, "item/garage_door.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": "golemgarage:item/garage_door"}})
    _json(os.path.join(models, "block/garage_window.json"),
          {"parent": "minecraft:block/cube_all", "render_type": "minecraft:translucent",
           "textures": {"all": "golemgarage:block/garage_window"}})
    for b in ("garage_controller", "laser_emitter", "steel_emitter", "signal_plate", "golem_dock", "garage_window"):
        _json(os.path.join(models, "item/%s.json" % b), {"parent": "golemgarage:block/%s" % b})

    bs = os.path.join(assets, "blockstates")
    for b in ("laser_emitter", "steel_emitter"):
        _json(os.path.join(bs, b + ".json"), {"variants": {"": {"model": "golemgarage:block/" + b}}})
    # The controller's screen faces into the room it was placed toward.
    _json(os.path.join(bs, "garage_controller.json"), {"variants": {
        "facing=%s" % f: dict({"model": "golemgarage:block/garage_controller"}, **({"y": r} if r else {}))
        for f, r in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    _json(os.path.join(bs, "garage_door.json"), {"variants": {"": {"model": "golemgarage:block/garage_door"}}})
    _json(os.path.join(bs, "garage_window.json"), {"variants": {"": {"model": "golemgarage:block/garage_window"}}})
    _json(os.path.join(bs, "golem_dock.json"), {"variants": {
        "occupied=false": {"model": "golemgarage:block/golem_dock"},
        "occupied=true": {"model": "golemgarage:block/golem_dock_on"}}})
    _json(os.path.join(bs, "signal_plate.json"), {"variants": {
        "powered=false": {"model": "golemgarage:block/signal_plate"},
        "powered=true": {"model": "golemgarage:block/signal_plate_down"}}})
