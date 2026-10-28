"""
Cub outfits: craftable, equipable pieces in three wardrobe slots (HEAD, BODY, ACCESSORY).

Each piece is extra geometry that follows the Cub's head, body, arms or legs, with its own painted texture.
Writes textures/entity/cub_outfit/<id>.png, client/model/OutfitGeometry.java, item icons (3D renders),
item models, recipes and translations (added to the lang dict passed in).
"""
import json
import os
import random
import zlib

from PIL import Image

import paint

MODID = "golemgarage"
W = 64

# The Cub's parts and their pivots (must match cub.py).
PARENTS = {"head": (0, 16, 0), "body": (0, 16, 0), "right_arm": (-3.5, 16.5, 0), "left_arm": (3.5, 16.5, 0),
           "right_leg": (-1.5, 21, 0), "left_leg": (1.5, 21, 0)}


def B(o, s, style, grow=0.0, deco=None):
    return dict(o=o, s=s, style=style, grow=grow, deco=deco)


def P(name, boxes, pivot=(0, 0, 0), rot=(0, 0, 0), children=(), mirror=False):
    return dict(name=name, pivot=pivot, rot=rot, boxes=list(boxes), children=list(children), mirror=mirror)


# ------------------------------------------------------------------ styles: base color and surface pattern
STYLES = {
    "cap_red": ("#d8342c", "fabric"), "cap_red_dark": ("#a8231d", "fabric"), "cap_white": ("#f2f0ea", "fabric"),
    "black_plastic": ("#2a2a32", "glossy"), "neon_cyan": ("#4ff0ff", "neon"), "neon_red": ("#ff4a4a", "neon"),
    "witch_black": ("#2b2433", "fabric"), "witch_purple": ("#8e44c9", "fabric"), "gold": ("#f2c14e", "metal"),
    "bunny_pink": ("#f8c4d8", "fur"), "bow_pink": ("#ff7fb3", "polka"), "frog_green": ("#6ccb5f", "spots"),
    "frog_eye": ("#f4f4ee", "eye"), "vine": ("#4e8b3a", "leafy"), "flower_pink": ("#ff8fc8", "flower"),
    "flower_yellow": ("#ffd84a", "flower"), "flower_white": ("#f6f6f0", "flower"), "flower_blue": ("#7fb2ff", "flower"),
    "glass": ("#c8ecff", "glass"), "suit_white": ("#eef1f5", "tech"), "gem_red": ("#e8344a", "gem"),
    "leather_black": ("#2a2630", "leather"), "hoodie_gray": ("#8f939c", "knit"), "lace_black": ("#221d2a", "lace"),
    "berry_red": ("#e23b4f", "seeds"), "leaf_green": ("#59b341", "leafy"), "bee_stripes": ("#f6c834", "stripes"),
    "bee_yellow": ("#f6c834", "fabric"), "shades": ("#1c1c22", "lenses"), "silver": ("#c9ced6", "metal"),
    "cape_black": ("#1c1820", "silk"), "cape_red": ("#a3162e", "silk"), "bat_wing": ("#3a2f45", "membrane"),
    "feather_white": ("#f5f3ee", "feather"), "scarf": ("#3f7fd6", "scarf"), "pack_orange": ("#f08a2a", "canvas"),
    "pack_orange_dark": ("#c4661a", "canvas"), "wing_clear": ("#dff6ff", "clear"),
    "tux_black": ("#1e1d26", "silk"), "shirt_white": ("#f4f3ee", "fabric"), "bowtie_red": ("#c81e34", "silk"), "flame": ("#ffb238", "neon"),
}

# ------------------------------------------------------------------ the pieces
OUTFITS = [
    # ---- Cool
    dict(id="cap", name="Backwards Cap", slot="HEAD", theme="cool",
         recipe=["minecraft:red_wool", "minecraft:red_wool", "minecraft:string"],
         parts=[("head", P("cap", [
             B((-5, -10, -4.5), (10, 2, 9), "cap_red", 0.3, deco="panel"),
             B((-4, -11, -3.5), (8, 1, 7), "cap_red"),
             B((-0.5, -12, -0.5), (1, 1, 1), "cap_white"),
             B((-3, -9, 4.5), (6, 1, 4), "cap_red_dark", 0.1)]))]),
    dict(id="headphones", name="Headphones", slot="HEAD", theme="cool",
         recipe=["minecraft:black_wool", "minecraft:iron_ingot", "minecraft:note_block"],
         parts=[("head", P("headphones", [
             B((-5, -10.5, -1), (10, 1, 2), "black_plastic", 0.25),
             B((-6.5, -6, -2), (2, 4, 4), "black_plastic"),
             B((4.5, -6, -2), (2, 4, 4), "black_plastic"),
             B((-7, -5, -1), (1, 2, 2), "neon_cyan"),
             B((6, -5, -1), (1, 2, 2), "neon_cyan")]))]),
    dict(id="shades", name="Cool Shades", slot="ACCESSORY", theme="cool",
         recipe=["minecraft:black_stained_glass_pane", "minecraft:black_stained_glass_pane", "minecraft:stick"],
         parts=[("head", P("shades", [
             B((-5, -6.5, -5), (10, 2, 1), "shades", 0.1, deco="lenses"),
             B((-5.5, -6.5, -4.5), (1, 1, 5), "black_plastic"),
             B((4.5, -6.5, -4.5), (1, 1, 5), "black_plastic")]))]),
    dict(id="leather_jacket", name="Leather Jacket", slot="BODY", theme="cool",
         recipe=["minecraft:leather", "minecraft:leather", "minecraft:leather", "minecraft:iron_nugget"],
         parts=[("body", P("jacket", [B((-3, 0, -2), (6, 5, 4), "leather_black", 0.35, deco="zip")])),
                ("right_arm", P("sleeve_r", [B((-1.5, 0, -1), (2, 3, 2), "leather_black", 0.3)])),
                ("left_arm", P("sleeve_l", [B((-0.5, 0, -1), (2, 3, 2), "leather_black", 0.3)], mirror=True))]),
    dict(id="hoodie", name="Cozy Hoodie", slot="BODY", theme="cool",
         recipe=["minecraft:gray_wool", "minecraft:gray_wool", "minecraft:gray_wool", "minecraft:string"],
         parts=[("body", P("hoodie", [B((-3, 0, -2), (6, 5, 4), "hoodie_gray", 0.35, deco="pocket"),
                                       B((-4, -2, 1.5), (8, 2, 2), "hoodie_gray")])),
                ("right_arm", P("sleeve_r", [B((-1.5, 0, -1), (2, 4, 2), "hoodie_gray", 0.3)])),
                ("left_arm", P("sleeve_l", [B((-0.5, 0, -1), (2, 4, 2), "hoodie_gray", 0.3)], mirror=True))]),
    dict(id="backpack", name="Tiny Backpack", slot="ACCESSORY", theme="cool",
         recipe=["minecraft:orange_wool", "minecraft:leather", "minecraft:string"],
         parts=[("body", P("backpack", [B((-2.5, 0.5, 2), (5, 4, 2), "pack_orange", deco="flap"),
                                         B((-1.5, 2.5, 4), (3, 2, 1), "pack_orange_dark")]))]),
    # ---- Goth
    dict(id="witch_hat", name="Witch Hat", slot="HEAD", theme="goth",
         recipe=["minecraft:black_wool", "minecraft:black_wool", "minecraft:purple_dye", "minecraft:gold_nugget"],
         parts=[("head", P("witch_hat", [
             B((-7, -9.75, -7), (14, 1, 14), "witch_black"),
             B((-4.5, -13, -4.5), (9, 3, 9), "witch_black"),
             B((-4.5, -11, -4.5), (9, 1, 9), "witch_purple", 0.15),
             B((-1, -11.5, -4.75), (2, 2, 1), "gold"),
             B((-3, -16, -3), (6, 3, 6), "witch_black")],
             children=[P("tip", [B((-1.5, -3, -1.5), (3, 3, 3), "witch_black")], pivot=(0, -16, 0),
                         rot=(-0.35, 0, 0.25))]))]),
    dict(id="lace_dress", name="Lace Dress", slot="BODY", theme="goth",
         recipe=["minecraft:black_wool", "minecraft:black_wool", "minecraft:string", "minecraft:purple_dye"],
         parts=[("body", P("dress", [B((-3, 0, -2), (6, 3, 4), "lace_black", 0.3),
                                     B((-4, 3, -3), (8, 3, 6), "lace_black", deco="hem"),
                                     B((-1, 0.5, -2.6), (2, 1, 1), "witch_purple")]))]),
    dict(id="spiked_collar", name="Spiked Collar", slot="ACCESSORY", theme="goth",
         recipe=["minecraft:leather", "minecraft:iron_nugget", "minecraft:iron_nugget"],
         parts=[("body", P("collar", [B((-3, -0.5, -2), (6, 1, 4), "leather_black", 0.45),
                                      B((-2.5, -0.5, -3), (1, 1, 1), "silver"),
                                      B((-0.5, -0.5, -3), (1, 1, 1), "silver"),
                                      B((1.5, -0.5, -3), (1, 1, 1), "silver"),
                                      B((-4, -0.5, -0.5), (1, 1, 1), "silver"),
                                      B((3, -0.5, -0.5), (1, 1, 1), "silver")]))]),
    dict(id="vampire_cape", name="Vampire Cape", slot="ACCESSORY", theme="goth",
         recipe=["minecraft:black_wool", "minecraft:red_wool", "minecraft:phantom_membrane"],
         parts=[("body", P("cape_collar", [B((-3.5, -3, 1.5), (7, 3, 1), "cape_red")],
                           children=[P("cape", [B((-3.5, 0, 0), (7, 7, 1), "cape_black", deco="lining")],
                                       pivot=(0, 0, 2.5), rot=(0.12, 0, 0))]))]),
    dict(id="bat_wings", name="Bat Wings", slot="ACCESSORY", theme="goth",
         recipe=["minecraft:phantom_membrane", "minecraft:phantom_membrane", "minecraft:black_dye"],
         parts=[("body", P("wings", [], children=[
             P("wing_r", [B((-6, -1, 0), (6, 4, 1), "bat_wing", deco="veins")], pivot=(-1.5, 1, 2.5), rot=(0, -0.5, 0.25)),
             P("wing_l", [B((0, -1, 0), (6, 4, 1), "bat_wing", deco="veins")], pivot=(1.5, 1, 2.5), rot=(0, 0.5, -0.25),
               mirror=True)]))]),
    # ---- Cute
    dict(id="bunny_ears", name="Bunny Ears", slot="HEAD", theme="cute",
         recipe=["minecraft:pink_wool", "minecraft:white_wool", "minecraft:rabbit_hide"],
         parts=[("head", P("ears", [], children=[
             P("ear_r", [B((-1, -7, -0.5), (2, 7, 1), "bunny_pink", deco="inner_ear")], pivot=(-2.5, -9, 0),
               rot=(-0.15, 0, -0.22)),
             P("ear_l", [B((-1, -7, -0.5), (2, 7, 1), "bunny_pink", deco="inner_ear")], pivot=(2.5, -9, 0),
               rot=(-0.15, 0, 0.22))]))]),
    dict(id="bow", name="Big Bow", slot="HEAD", theme="cute",
         recipe=["minecraft:pink_wool", "minecraft:string", "minecraft:white_dye"],
         parts=[("head", P("bow", [B((2, -10.5, -1), (2, 2, 2), "bow_pink"),
                                   B((-1, -11.5, -0.5), (3, 3, 1), "bow_pink"),
                                   B((4, -11.5, -0.5), (3, 3, 1), "bow_pink")]))]),
    dict(id="frog_hat", name="Frog Hat", slot="HEAD", theme="cute",
         recipe=["minecraft:lime_wool", "minecraft:lime_wool", "minecraft:slime_ball"],
         parts=[("head", P("frog", [B((-5, -11, -4.5), (10, 3, 9), "frog_green", 0.3, deco="smile"),
                                    B((-4.5, -13.5, -4), (3, 3, 3), "frog_eye"),
                                    B((1.5, -13.5, -4), (3, 3, 3), "frog_eye")]))]),
    dict(id="flower_crown", name="Flower Crown", slot="HEAD", theme="cute",
         recipe=["minecraft:vine", "#minecraft:small_flowers", "#minecraft:small_flowers", "#minecraft:small_flowers"],
         parts=[("head", P("crown", [
             B((-5, -9.5, -5), (10, 1, 1), "vine", 0.1), B((-5, -9.5, 4), (10, 1, 1), "vine", 0.1),
             B((-5.5, -9.5, -4.5), (1, 1, 9), "vine", 0.1), B((4.5, -9.5, -4.5), (1, 1, 9), "vine", 0.1),
             B((-4, -10.5, -5.5), (2, 2, 1), "flower_pink"), B((-1, -10.5, -5.5), (2, 2, 1), "flower_yellow"),
             B((2, -10.5, -5.5), (2, 2, 1), "flower_white"), B((-6, -10.5, 0), (1, 2, 2), "flower_blue"),
             B((5, -10.5, -2), (1, 2, 2), "flower_pink"), B((0, -10.5, 4.5), (2, 2, 1), "flower_yellow")]))]),
    dict(id="strawberry_dress", name="Strawberry Dress", slot="BODY", theme="cute",
         recipe=["minecraft:red_wool", "minecraft:red_wool", "minecraft:sweet_berries", "minecraft:green_dye"],
         parts=[("body", P("dress", [B((-3, 0, -2), (6, 3, 4), "berry_red", 0.3),
                                     B((-4, 3, -3), (8, 3, 6), "berry_red"),
                                     B((-3.5, -0.5, -2.5), (7, 1, 5), "leaf_green")]))]),
    dict(id="bee_costume", name="Bee Costume", slot="BODY", theme="cute",
         recipe=["minecraft:yellow_wool", "minecraft:black_wool", "minecraft:honeycomb"],
         parts=[("body", P("bee", [B((-3, 0, -2), (6, 5, 4), "bee_stripes", 0.35),
                                   B((-0.5, 3, 2.3), (1, 1, 2), "black_plastic"),
                                   B((-3.5, 0, 2.4), (3, 3, 1), "wing_clear"),
                                   B((0.5, 0, 2.4), (3, 3, 1), "wing_clear")])),
                ("head", P("antennae", [B((-3, -12, -2), (1, 3, 1), "black_plastic"),
                                        B((2, -12, -2), (1, 3, 1), "black_plastic"),
                                        B((-3.5, -13, -2.5), (2, 1, 2), "bee_yellow"),
                                        B((1.5, -13, -2.5), (2, 1, 2), "bee_yellow")]))]),
    dict(id="angel_wings", name="Angel Wings", slot="ACCESSORY", theme="cute",
         recipe=["minecraft:feather", "minecraft:feather", "minecraft:feather", "minecraft:white_wool"],
         parts=[("body", P("wings", [], children=[
             P("wing_r", [B((-7, -2, 0), (7, 6, 1), "feather_white", deco="feathers")], pivot=(-1.5, 1, 2.5),
               rot=(0, -0.45, 0.2)),
             P("wing_l", [B((0, -2, 0), (7, 6, 1), "feather_white", deco="feathers")], pivot=(1.5, 1, 2.5),
               rot=(0, 0.45, -0.2), mirror=True)]))]),
    dict(id="scarf", name="Cozy Scarf", slot="ACCESSORY", theme="cute",
         recipe=["minecraft:blue_wool", "minecraft:white_wool"],
         parts=[("body", P("scarf", [B((-3, -1, -2), (6, 1, 4), "scarf", 0.45),
                                     B((1, 0, -2.75), (2, 4, 1), "scarf")]))]),
    # ---- Fancy
    dict(id="crown", name="Royal Crown", slot="HEAD", theme="fancy",
         recipe=["minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:gold_ingot", "minecraft:redstone"],
         parts=[("head", P("crown", [
             B((-4, -11, -4), (8, 2, 8), "gold", 0.1),
             B((-4, -13, -4), (2, 2, 1), "gold"), B((-1, -13, -4), (2, 2, 1), "gold"), B((2, -13, -4), (2, 2, 1), "gold"),
             B((-4, -13, 3), (2, 2, 1), "gold"), B((2, -13, 3), (2, 2, 1), "gold"),
             B((-0.5, -10.5, -4.5), (1, 1, 1), "gem_red")]))]),
    dict(id="astronaut_helmet", name="Space Helmet", slot="HEAD", theme="fancy",
         recipe=["minecraft:glass", "minecraft:glass", "minecraft:white_wool", "minecraft:iron_ingot"],
         parts=[("head", P("helmet", [
             B((-6, -10.5, -6), (12, 11, 12), "glass"),
             B((-6, 0, -6), (12, 1, 12), "suit_white", 0.2),
             B((3, -13, 2), (1, 3, 1), "suit_white"),
             B((2.5, -14, 1.5), (2, 1, 2), "neon_red")]))]),
    dict(id="tuxedo", name="Dapper Tuxedo", slot="BODY", theme="fancy",
         recipe=["minecraft:black_wool", "minecraft:black_wool", "minecraft:white_wool", "minecraft:red_dye"],
         parts=[("body", P("tux", [B((-3, 0, -2), (6, 5, 4), "tux_black", 0.35),
                                   B((-1, 0.4, -2.6), (2, 4, 1), "shirt_white"),
                                   B((-1.5, 0.2, -3), (3, 1, 1), "bowtie_red"),
                                   B((-0.5, 2, -2.9), (1, 1, 1), "gold")])),
                ("right_arm", P("sleeve_r", [B((-1.5, 0, -1), (2, 4, 2), "tux_black", 0.3)])),
                ("left_arm", P("sleeve_l", [B((-0.5, 0, -1), (2, 4, 2), "tux_black", 0.3)], mirror=True))]),
    dict(id="monocle", name="Gold Monocle", slot="ACCESSORY", theme="fancy",
         recipe=["minecraft:gold_ingot", "minecraft:glass_pane", "minecraft:string"],
         parts=[("head", P("monocle", [B((-4, -7.5, -5.2), (3, 3, 1), "glass"),
                                       B((-4.5, -8, -5.3), (4, 1, 1), "gold"),
                                       B((-4.5, -5, -5.3), (4, 1, 1), "gold"),
                                       B((-4.5, -7, -5.3), (1, 2, 1), "gold"),
                                       B((-1.5, -7, -5.3), (1, 2, 1), "gold"),
                                       B((-4.5, -4, -5), (1, 4, 1), "gold")]))]),
    dict(id="jetpack", name="Rocket Jetpack", slot="ACCESSORY", theme="fancy",
         recipe=["minecraft:iron_ingot", "minecraft:iron_ingot", "minecraft:blaze_powder", "minecraft:firework_rocket"],
         parts=[("body", P("jetpack", [B((-2.5, 0, 2), (2, 4, 2), "silver", deco="panel"),
                                       B((0.5, 0, 2), (2, 4, 2), "silver", deco="panel"),
                                       B((-2, -1, 2.5), (1, 1, 1), "neon_red"),
                                       B((1, -1, 2.5), (1, 1, 1), "neon_red"),
                                       B((-2, 4, 2.5), (1, 1, 1), "flame"),
                                       B((1, 4, 2.5), (1, 1, 1), "flame")]))]),
]

THEMES = {"cool": "Cool", "goth": "Goth", "cute": "Cute", "fancy": "Fancy"}


# ------------------------------------------------------------------ packing and painting
def all_boxes(part, out):
    for b in part["boxes"]:
        b["mirror"] = part["mirror"]
        out.append(b)
    for c in part["children"]:
        all_boxes(c, out)


def pack(boxes):
    order = sorted(boxes, key=lambda b: (-(b["s"][2] + b["s"][1]), -(2 * (b["s"][2] + b["s"][0]))))
    x = y = shelf = 0
    for b in order:
        w, h, d = b["s"]
        rw, rh = 2 * (d + w), d + h
        if x + rw > W:
            x, y, shelf = 0, y + shelf, 0
        b["uv"] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    used = y + shelf
    h = 16
    while h < used:
        h *= 2
    return h


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def paint_face(c, rect, style, face, deco, rng):
    base_hex, pattern = STYLES[style]
    pal = paint.ramp(base_hex)
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    shift = {"top": 1, "front": 0, "left": 0, "right": 0, "back": -1, "bottom": -1}[face]
    alpha = 255
    if pattern == "glass":
        alpha = 80
    elif pattern == "clear":
        alpha = 130
    for yy in range(y0, y1):
        for xx in range(x0, x1):
            lx, ly = xx - x0, yy - y0
            k = 3 + shift
            n = rng.random()
            if pattern in ("fabric", "knit", "canvas", "fur", "silk", "leather", "lace", "scarf", "feather", "membrane"):
                if n < 0.12:
                    k -= 1
                elif n > 0.93:
                    k += 1
            if pattern == "knit" and (lx + ly) % 2 == 0:
                k -= 1 if n < 0.5 else 0
            col = pal[max(1, min(6, k))]
            if pattern == "stripes":
                col = (36, 30, 34) if (ly // 2) % 2 == 1 else pal[max(1, min(6, 3 + shift))]
            elif pattern == "scarf":
                col = (240, 240, 236) if (lx // 2) % 3 == 2 else col
            elif pattern == "polka":
                col = (255, 240, 248) if (lx % 3 == 1 and ly % 3 == 1) else col
            elif pattern == "spots":
                col = pal[2] if rng.random() < 0.08 else col
            elif pattern == "seeds":
                col = (255, 224, 110) if (lx % 3 == (ly // 2) % 3 and ly % 2 == 0) else col
            elif pattern == "lace":
                col = pal[5] if (lx + ly) % 4 == 0 and n < 0.6 else col
            elif pattern == "leafy":
                col = pal[4] if n < 0.2 else pal[2] if n > 0.85 else col
            elif pattern == "metal":
                col = pal[6] if (lx + ly) % 7 == 0 else col
            elif pattern == "gem":
                col = pal[6] if (lx == 0 and ly == 0) else pal[4]
            elif pattern == "glossy":
                col = pal[2 + shift if 2 + shift > 0 else 1]
                if face == "front" and lx == ly:
                    col = pal[5]
            elif pattern == "neon":
                col = pal[5] if (lx + ly) % 2 == 0 else pal[6]
            elif pattern == "glass":
                col = (230, 248, 255) if (lx + ly == 2 or lx + ly == 3) and face in ("front", "left", "top") else pal[4]
            elif pattern == "clear":
                col = pal[5] if (lx + ly) % 3 == 0 else pal[4]
            elif pattern == "tech":
                col = pal[3 + shift] if ly % 3 else pal[2]
            elif pattern == "flower":
                col = (255, 214, 80) if (lx, ly) == (w // 2, h // 2) or (w <= 2 and h <= 2 and (lx + ly) % 2) else col
            elif pattern == "eye":
                col = pal[5]
                if face == "front" and w >= 3 and h >= 3 and lx in (1,) and ly in (1,):
                    col = (24, 22, 28)
            elif pattern == "lenses":
                col = (24, 24, 30)
            elif pattern == "membrane":
                col = col
            c.set(xx, yy, tuple(col[:3]), alpha)
    # Decorations on the front (or back for capes/wings).
    if deco == "zip" and face == "front":
        mx = x0 + w // 2
        for yy in range(y0, y1):
            c.set(mx, yy, (190, 196, 206), 255)
        c.set(mx - 1, y0, (120, 124, 130), 255)
    elif deco == "pocket" and face == "front":
        for xx in range(x0 + 1, x1 - 1):
            c.set(xx, y1 - 3, _mix(pal[2], (0, 0, 0), 0.2), 255)
        for yy in range(y1 - 3, y1):
            c.set(x0 + 1, yy, pal[2], 255)
            c.set(x1 - 2, yy, pal[2], 255)
    elif deco == "panel" and face == "front":
        c.set(x0 + w // 2, y0, (255, 255, 255), 255)
        for xx in range(x0 + 2, x1 - 2):
            c.set(xx, y1 - 1, pal[2], 255)
    elif deco == "lenses" and face == "front":
        for xx in range(x0, x1):
            if xx - x0 in (0, w - 1) or (w // 2 - 1 <= xx - x0 <= w // 2):
                c.set(xx, y0, (60, 60, 70), 255)
            else:
                c.set(xx, y0, (30, 30, 40), 255)
                c.set(xx, y1 - 1, (14, 14, 20), 255)
        c.set(x0 + 2, y0, (220, 240, 255), 255)
        c.set(x0 + w // 2 + 2, y0, (220, 240, 255), 255)
    elif deco == "hem" and face in ("front", "left", "right", "back"):
        for xx in range(x0, x1):
            c.set(xx, y1 - 1, (142, 68, 201), 255)
            if (xx - x0) % 2 == 0:
                c.set(xx, y1 - 2, (60, 40, 76), 255)
    elif deco == "smile" and face == "front":
        for xx in range(x0 + 3, x1 - 3):
            c.set(xx, y1 - 1, (40, 80, 36), 255)
        c.set(x0 + 2, y1 - 2, (40, 80, 36), 255)
        c.set(x1 - 3, y1 - 2, (40, 80, 36), 255)
        c.set(x0 + 1, y1 - 2, (255, 150, 170), 255)
        c.set(x1 - 2, y1 - 2, (255, 150, 170), 255)
    elif deco == "inner_ear" and face == "front":
        for yy in range(y0 + 1, y1 - 1):
            for xx in range(x0, x1):
                c.set(xx, yy, (255, 150, 186), 255)
    elif deco == "flap" and face == "back":
        for xx in range(x0, x1):
            c.set(xx, y0 + 1, pal[1], 255)
        c.set(x0 + w // 2, y0 + 2, (220, 220, 220), 255)
    elif deco == "lining" and face == "front":
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                c.set(xx, yy, (163, 22, 46), 255)
    elif deco == "veins" and face in ("front", "back"):
        for i in range(3):
            xx = x0 + 1 + i * 2
            for yy in range(y0, y1):
                if xx < x1:
                    c.set(xx, yy, (24, 18, 30), 255)
        for xx in range(x0, x1):
            if (xx - x0) % 2 == 1:
                c.set(xx, y1 - 1, (0, 0, 0), 0)
    elif deco == "feathers" and face in ("front", "back"):
        for yy in range(y0, y1):
            for xx in range(x0, x1):
                if (xx - x0 + (yy - y0)) % 3 == 0:
                    c.set(xx, yy, (214, 210, 200), 255)
        for xx in range(x0, x1):
            if (xx - x0) % 2 == 1:
                c.set(xx, y1 - 1, (0, 0, 0), 0)


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()

    def set(self, x, y, col, a=255):
        if 0 <= x < self.img.width and 0 <= y < self.img.height:
            self.px[x, y] = tuple(col[:3]) + (a,)


def paint_outfit(o):
    boxes = []
    for _, p in o["parts"]:
        all_boxes(p, boxes)
    h = pack(boxes)
    c = Canvas(W, h)
    for b in boxes:
        u, v = b["uv"]
        w_, h_, d_ = b["s"]
        rng = random.Random(zlib.crc32(("%s%s%s" % (o["id"], b["o"], b["s"])).encode()))
        for face, rect in paint.box_faces(u, v, w_, h_, d_).items():
            paint_face(c, rect, b["style"], face, b.get("deco"), rng)
    return c.img, h


# ------------------------------------------------------------------ Java geometry
def f(x):
    s = ("%.4f" % x).rstrip("0").rstrip(".")
    if s in ("-0", ""):
        s = "0"
    return s + "F"


def java_part(lines, parent, part, counter):
    var = "p%d" % counter[0]
    counter[0] += 1
    builder = "CubeListBuilder.create()"
    if part["mirror"]:
        builder += ".mirror()"
    for b in part["boxes"]:
        u, v = b["uv"]
        x, y, z = b["o"]
        w, h, d = b["s"]
        if b.get("grow"):
            builder += "\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s, new CubeDeformation(%s))" % (
                u, v, f(x), f(y), f(z), f(w), f(h), f(d), f(b["grow"]))
        else:
            builder += "\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (u, v, f(x), f(y), f(z), f(w), f(h), f(d))
    px, py, pz = part["pivot"]
    rx, ry, rz = part["rot"]
    lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s,\n                PartPose.offsetAndRotation(%s, %s, %s, %s, %s, %s));' % (
        var, parent, part["name"], builder, f(px), f(py), f(pz), f(rx), f(ry), f(rz)))
    for ch in part["children"]:
        java_part(lines, var, ch, counter)


def write_java(java_dir, heights):
    methods = []
    for o in OUTFITS:
        lines = ["    static LayerDefinition %s() {" % o["id"],
                 "        MeshDefinition mesh = new MeshDefinition();",
                 "        PartDefinition root = mesh.getRoot();"]
        for pname, (px, py, pz) in PARENTS.items():
            lines.append('        PartDefinition %s = root.addOrReplaceChild("%s", CubeListBuilder.create(), PartPose.offset(%s, %s, %s));' % (
                pname, pname, f(px), f(py), f(pz)))
        counter = [0]
        for parent, part in o["parts"]:
            body = []
            java_part(body, parent, part, counter)
            lines += body
        lines.append("        return LayerDefinition.create(mesh, %d, %d);" % (W, heights[o["id"]]))
        lines.append("    }")
        methods.append("\n".join(lines))
    entries = ",\n".join('            new Piece("%s", "%s", "%s", OutfitGeometry::%s)' % (
        o["id"], o["slot"], o["theme"], o["id"]) for o in OUTFITS)
    src = """package com.xtrip.golemgarage.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

import java.util.List;
import java.util.function.Supplier;

/**
 * Cub outfit geometry, one layer per piece. Each has the Cub's six parts (head, body, arms, legs) as
 * empty parents, so the outfit model can copy the Cub's pose.
 * GENERATED by tools/golemgen/outfits.py. Edit the generator, not this file.
 */
public final class OutfitGeometry {
    public record Piece(String id, String slot, String theme, Supplier<LayerDefinition> layer) {
    }

    public static final List<Piece> PIECES = List.of(
%s);

%s

    private OutfitGeometry() {
    }
}
""" % (entries, "\n\n".join(methods))
    with open(os.path.join(java_dir, "client/model/OutfitGeometry.java"), "w") as fh:
        fh.write(src)
    # Common-side list of pieces (ids, slots, themes) for items and the entity.
    common = ",\n".join('            new String[]{"%s", "%s", "%s"}' % (o["id"], o["slot"], o["theme"]) for o in OUTFITS)
    src2 = """package com.xtrip.golemgarage.item;

import java.util.List;

/**
 * Every Cub outfit piece: id, wardrobe slot (HEAD, BODY, ACCESSORY) and style.
 * GENERATED by tools/golemgen/outfits.py. Edit the generator, not this file.
 */
public final class OutfitPieces {
    public static final List<String[]> ALL = List.of(
%s);

    private OutfitPieces() {
    }
}
""" % common
    with open(os.path.join(java_dir, "item/OutfitPieces.java"), "w") as fh:
        fh.write(src2)


def _write_json(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w") as fh:
        json.dump(obj, fh, indent=2)
        fh.write("\n")


def generate(java_dir, assets, data):
    out = os.path.join(assets, "textures/entity/cub_outfit")
    os.makedirs(out, exist_ok=True)
    heights = {}
    for o in OUTFITS:
        img, h = paint_outfit(o)
        heights[o["id"]] = h
        img.save(os.path.join(out, o["id"] + ".png"))
    write_java(java_dir, heights)
    wardrobe_gui().save(os.path.join(assets, "textures/gui/cub_wardrobe.png"))
    rdir = os.path.join(data, MODID, "recipe", "outfit")
    for o in OUTFITS:
        _write_json(os.path.join(rdir, o["id"] + ".json"), {
            "type": "minecraft:crafting_shapeless", "category": "misc",
            "ingredients": [{"tag": r[1:]} if r.startswith("#") else {"item": r} for r in o["recipe"]],
            "result": {"id": "%s:outfit_%s" % (MODID, o["id"]), "count": 1},
        })
        _write_json(os.path.join(assets, "models/item/outfit_%s.json" % o["id"]),
                    {"parent": "minecraft:item/generated", "textures": {"layer0": "%s:item/outfit/%s" % (MODID, o["id"])}})
    tags = os.path.join(data, MODID, "tags", "item")
    _write_json(os.path.join(tags, "outfits.json"),
                {"replace": False, "values": ["%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS]})
    for theme in THEMES:
        _write_json(os.path.join(tags, "outfits", theme + ".json"), {"replace": False, "values": [
            "%s:outfit_%s" % (MODID, o["id"]) for o in OUTFITS if o["theme"] == theme]})
    print("outfits: %d pieces" % len(OUTFITS))


def lang(entries):
    for o in OUTFITS:
        entries["item.golemgarage.outfit_%s" % o["id"]] = o["name"]
    for k, v in THEMES.items():
        entries["outfit.golemgarage.theme.%s" % k] = v
    entries["outfit.golemgarage.slot.head"] = "Head"
    entries["outfit.golemgarage.slot.body"] = "Body"
    entries["outfit.golemgarage.slot.accessory"] = "Accessory"
    entries["outfit.golemgarage.tooltip"] = "%s style, %s slot. Right-click a Cub to dress it."
    entries["outfit.golemgarage.set_bonus"] = "Wear a full %s set for a little extra sparkle."


def render_icons(assets):
    """3D icons for every piece, from its own geometry and texture."""
    import iconrender as R
    out = os.path.join(assets, "textures/item/outfit")
    os.makedirs(out, exist_ok=True)
    for o in OUTFITS:
        polys = []
        back = o["id"] in ("angel_wings", "bat_wings", "backpack", "vampire_cape", "jetpack")
        root = R.Xf(R.rot_matrix(0, 3.14159265, 0)) if back else R.Xf()
        for parent, part in o["parts"]:
            pr = root.then_local(PARENTS[parent], (0, 0, 0))
            _collect(part, pr, polys, R)
        gb = R.GBuffer(polys, margin=1.0)
        atlas = Image.open(os.path.join(assets, "textures/entity/cub_outfit/%s.png" % o["id"]))
        R.shade(gb, atlas, None, None, flat_tags=(1,)).save(os.path.join(out, o["id"] + ".png"))


def _collect(part, xf, polys, R):
    child = xf.then_local(part["pivot"], part.get("rot", (0, 0, 0)))
    for b in part["boxes"]:
        g = b.get("grow", 0)
        o = (b["o"][0] - g, b["o"][1] - g, b["o"][2] - g)
        u, v = b["uv"]
        # Grown boxes keep the same UV; draw them slightly bigger like the game does.
        s = b["s"]
        flat = 1 if STYLES[b["style"]][1] in ("neon", "glass") else 0
        for poly in R.cube_polys(o, (s[0] + 2 * g, s[1] + 2 * g, s[2] + 2 * g), u, v, bool(part["mirror"]), child,
                                 uv_size=s):
            polys.append(poly + (flat,))
    for c in part["children"]:
        _collect(c, child, polys, R)


# ------------------------------------------------------------------ wardrobe screen texture
def wardrobe_gui():
    """176x196 panel: cub preview window, three wardrobe slots with ghost icons, pockets and inventory."""
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    W_, H_ = 176, 196
    cream, cream_hi, cream_lo = (250, 238, 230), (255, 252, 248), (214, 190, 186)
    edge = (60, 44, 58)
    for y in range(H_):
        for x in range(W_):
            c = cream
            if x in (0, W_ - 1) or y in (0, H_ - 1):
                c = edge
            elif x == 1 or y == 1:
                c = cream_hi
            elif x == W_ - 2 or y == H_ - 2:
                c = cream_lo
            px[x, y] = c + (255,)
    for (x, y) in ((0, 0), (W_ - 1, 0), (0, H_ - 1), (W_ - 1, H_ - 1)):
        px[x, y] = (0, 0, 0, 0)

    def slot(x, y):
        for yy in range(18):
            for xx in range(18):
                c = (139, 139, 139)
                if xx == 0 or yy == 0:
                    c = (55, 55, 55)
                elif xx == 17 or yy == 17:
                    c = (255, 255, 255)
                px[x + xx, y + yy] = c + (255,)

    # Preview window with a soft pastel gradient.
    for y in range(16, 80):
        for x in range(8, 58):
            t = (y - 16) / 64
            c = _mix((255, 214, 232), (196, 214, 255), t)
            if x in (8, 57) or y in (16, 79):
                c = (150, 120, 140)
            px[x, y] = c + (255,)
    # Wardrobe slots with ghost icons.
    ghosts = {
        0: ["..####..", ".######.", "########", "#......#"],                               # hat
        1: ["##....##", "########", ".######.", ".######.", ".######."],                   # shirt
        2: ["...#....", "..###...", "########", ".######.", "..#..#.."],                   # star
    }
    for i in range(3):
        sx, sy = 61, 17 + i * 20
        slot(sx, sy)
        g = ghosts[i]
        for yy, row in enumerate(g):
            for xx, ch in enumerate(row):
                if ch == "#":
                    px[sx + 5 + xx, sy + 6 + yy] = (118, 118, 118, 255)
    # Pockets and inventory.
    for i in range(9):
        slot(7 + i * 18, 89)
    for r in range(3):
        for c in range(9):
            slot(7 + c * 18, 113 + r * 18)
    for c in range(9):
        slot(7 + c * 18, 171)
    # Info card on the right.
    for y in range(16, 80):
        for x in range(84, 169):
            c = (255, 246, 240)
            if x in (84, 168) or y in (16, 79):
                c = cream_lo
            px[x, y] = c + (255,)
    return img
