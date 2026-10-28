"""
Pixel painting for golem textures and icons.

ramp(hex) builds a 7-step hue-shifted palette for a material:
  0 outline, 1 deep shadow, 2 shadow, 3 base, 4 light, 5 highlight, 6 specular.
Shadows lean blue and highlights lean warm, the classic pixel-art look.
"""
import colorsys
import random

from PIL import Image


def hex_rgb(h):
    h = h.lstrip("#")
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _shift_hue(h, target, amount):
    d = (target - h + 0.5) % 1.0 - 0.5
    return (h + d * amount) % 1.0


def _clamp(x):
    return max(0.0, min(1.0, x))


def ramp(base_hex):
    r, g, b = [c / 255 for c in hex_rgb(base_hex)]
    h, s, v = colorsys.rgb_to_hsv(r, g, b)
    blue, warm = 0.64, 0.13
    steps = [
        (blue, 0.30, 1.10, 0.06, 0.32, 0.00),
        (blue, 0.20, 1.08, 0.04, 0.52, 0.00),
        (blue, 0.10, 1.04, 0.02, 0.73, 0.00),
        (h, 0.00, 1.00, 0.00, 1.00, 0.00),
        (warm, 0.06, 0.90, 0.00, 1.12, 0.05),
        (warm, 0.12, 0.72, 0.00, 1.24, 0.10),
        (warm, 0.16, 0.42, 0.00, 1.38, 0.18),
    ]
    # Light steps approach white gradually so pale metals keep visible shading.
    toward_white = {4: 0.35, 5: 0.6, 6: 0.85}
    out = []
    for i, (target, amt, smul, sadd, vmul, vadd) in enumerate(steps):
        hh = _shift_hue(h, target, amt)
        ss = _clamp(s * smul + sadd)
        vv = v * vmul + vadd
        if i in toward_white:
            vv = min(vv, v + (1 - v) * toward_white[i])
        vv = _clamp(vv)
        rr, gg, bb = colorsys.hsv_to_rgb(hh, ss, vv)
        out.append((int(rr * 255), int(gg * 255), int(bb * 255)))
    return out


RUBBER = ramp("#36363c")
GLASS = ramp("#a9dbe6")
SOLAR = ramp("#203a78")
SOUL = ramp("#3fd6dc")
GLOW_BASE = (34, 26, 26)
PATINA = hex_rgb("#5db39a")


class Canvas:
    def __init__(self, w, h):
        self.img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
        self.px = self.img.load()
        self.w, self.h = w, h

    def set(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[x, y] = tuple(c[:3]) + (a,)

    def get(self, x, y):
        return self.px[x, y]


# ---------------------------------------------------------------- box layout
def box_faces(u, v, w, h, d):
    """Face rectangles (x0, y0, x1, y1) in the standard Minecraft box UV layout."""
    return {
        "top": (u + d, v, u + d + w, v + d),
        "bottom": (u + d + w, v, u + d + 2 * w, v + d),
        "right": (u, v + d, u + d, v + d + h),
        "front": (u + d, v + d, u + d + w, v + d + h),
        "left": (u + d + w, v + d, u + 2 * d + w, v + d + h),
        "back": (u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h),
    }


# Brightness offset per face so boxes read as solid even in flat lighting.
FACE_SHIFT = {"top": 1, "front": 0, "left": 0, "right": 0, "back": -1, "bottom": -1}


def _ri(rng, a, b):
    return rng.randint(a, b)


# ---------------------------------------------------------------- material fill
def fill_face(c, rect, pal, style, mat, face, rng):
    """Paints the base surface of one face: fill, texture noise, bevel."""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0:
        return
    shift = FACE_SHIFT[face]
    base = {"metal": 3, "dark": 2, "trim": 4}.get(style, 3) + shift
    base = max(1, min(5, base))

    stype = mat.get("style", "metal") if style in ("metal", "dark", "trim") else style
    if style == "rubber":
        pal = RUBBER
    elif style == "glass":
        pal = GLASS
    elif style == "solar":
        pal = SOLAR
    elif style == "soul":
        pal = SOUL

    for yy in range(h):
        streak = 0
        for xx in range(w):
            idx = base
            if stype == "metal":
                # Brushed metal: short horizontal streaks one shade up or down.
                if xx == 0 or rng.random() < 0.18:
                    streak = rng.choice([0, 0, 0, 1, -1])
                idx = base + streak
            elif stype == "stone":
                n = rng.random()
                idx = base + (1 if n > 0.8 else -1 if n < 0.22 else 0)
            elif stype == "gem":
                # Diagonal facets.
                band = ((xx + yy) // 3) % 3
                idx = base + (1 if band == 0 else 0 if band == 1 else -1)
            elif style in ("rubber", "solar"):
                idx = 2 + (1 if rng.random() < 0.15 else 0)
            elif style in ("glass", "soul"):
                idx = 4 if (xx + yy) % 5 else 5
            c.set(x0 + xx, y0 + yy, pal[max(0, min(6, idx))])

    if stype == "stone" and w >= 3 and h >= 3:
        # Cobble mortar lines.
        for _ in range(max(1, (w * h) // 18)):
            sx, sy = x0 + _ri(rng, 0, w - 1), y0 + _ri(rng, 0, h - 1)
            for _ in range(_ri(rng, 2, 4)):
                c.set(sx, sy, pal[max(0, base - 2)])
                sx += rng.choice([-1, 0, 1])
                sy += rng.choice([0, 1])
    if stype == "gem" and w >= 2 and h >= 2:
        for _ in range(max(1, (w * h) // 24)):
            c.set(x0 + _ri(rng, 0, w - 1), y0 + _ri(rng, 0, h - 1), pal[6])

    # Bevel: light top/left edge, dark bottom/right edge.
    if w >= 3 and h >= 3 and style not in ("glass", "soul"):
        for xx in range(w):
            c.set(x0 + xx, y0, pal[min(6, base + 1)])
            c.set(x0 + xx, y1 - 1, pal[max(0, base - 2)])
        for yy in range(h):
            c.set(x0, y0 + yy, pal[min(6, base + 1)])
            c.set(x1 - 1, y0 + yy, pal[max(0, base - 1)])
        c.set(x0, y0, pal[min(6, base + 2)])
        c.set(x1 - 1, y1 - 1, pal[max(0, base - 3)])
    elif style == "glass" and w >= 2 and h >= 2:
        for xx in range(w):
            c.set(x0 + xx, y0, pal[2])
            c.set(x0 + xx, y1 - 1, pal[2])
        for yy in range(h):
            c.set(x0, y0 + yy, pal[2])
            c.set(x1 - 1, y0 + yy, pal[2])
        if w >= 3 and h >= 3:
            c.set(x0 + 1, y0 + 1, pal[6])


def material_fx(c, rect, pal, mat, rng):
    """Surface effects that belong to the material: veins, sparkles, copper patina."""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    fx = mat.get("fx", "none")
    if w < 2 or h < 2 or fx == "none":
        return
    area = w * h
    if fx in ("veins", "glow_veins"):
        color = pal[1] if fx == "veins" else pal[6]
        for _ in range(max(1, area // 40)):
            x, y = x0 + _ri(rng, 0, w - 1), y0 + _ri(rng, 0, h - 1)
            for _ in range(_ri(rng, 3, 7)):
                c.set(x, y, color)
                x += rng.choice([-1, 1, 1])
                y += rng.choice([-1, 0, 1])
                if not (x0 <= x < x1 and y0 <= y < y1):
                    break
    elif fx == "sparkle":
        for _ in range(max(1, area // 20)):
            x, y = x0 + _ri(rng, 0, w - 1), y0 + _ri(rng, 0, h - 1)
            c.set(x, y, pal[6])
    elif fx.startswith("patina"):
        level = {"patina_light": 0.12, "patina_mid": 0.35, "patina_full": 0.7}[fx]
        pat = ramp("#5db39a") if fx != "patina_light" else ramp("#7fae8c")
        for yy in range(h):
            for xx in range(w):
                if rng.random() < level:
                    c.set(x0 + xx, y0 + yy, pat[rng.choice([2, 3, 3, 4])])
        if fx != "patina_full":
            # Leftover copper glints.
            cop = ramp("#d0743f")
            for _ in range(max(1, area // 30)):
                c.set(x0 + _ri(rng, 0, w - 1), y0 + _ri(rng, 0, h - 1), cop[4])


# ---------------------------------------------------------------- decorations
def _rivet(c, x, y, pal):
    c.set(x, y, pal[6])
    c.set(x + 1, y + 1, pal[1])


def deco(c, name, face, rect, pal, rng, box):
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w <= 0 or h <= 0 or not name:
        return
    mid = x0 + w // 2

    def hline(y, a, b, col):
        for x in range(a, b):
            c.set(x, y, col)

    def vline(x, a, b, col):
        for y in range(a, b):
            c.set(x, y, col)

    if name == "rivets":
        if w >= 4 and h >= 3:
            for (rx, ry) in ((x0 + 1, y0 + 1), (x1 - 3, y0 + 1)):
                _rivet(c, rx, ry, pal)
            if h >= 5:
                for (rx, ry) in ((x0 + 1, y1 - 3), (x1 - 3, y1 - 3)):
                    _rivet(c, rx, ry, pal)
    elif name == "panel":
        if w >= 4 and h >= 5 and face in ("front", "back", "left", "right"):
            for x in range(x0 + 1, x1 - 1):
                c.set(x, y0 + 1, pal[2])
            vline(x0 + 1, y0 + 1, y1 - 1, pal[2])
            if h >= 8:
                hline(y0 + h // 2, x0 + 1, x1 - 1, pal[1])
    elif name == "face" and face == "front":
        hline(y0 + 1, x0 + 1, x1 - 1, pal[2])          # forehead plate seam
        hline(y0 + 3, x0, x1, pal[1])                  # brow shadow
        vline(x0 + 1, y0 + 4, y1 - 1, pal[2])          # cheek seams
        vline(x1 - 2, y0 + 4, y1 - 1, pal[2])
        hline(y1 - 2, x0 + 2, x1 - 2, pal[1])          # mouth grille
        for x in range(x0 + 2, x1 - 2, 2):
            c.set(x, y1 - 2, pal[0])
    elif name == "face" and face == "top":
        hline(y0 + h // 2, x0 + 1, x1 - 1, pal[2])
    elif name == "nose" and face == "front":
        hline(y0, x0, x1, pal[5])
        hline(y1 - 1, x0, x1, pal[1])
    elif name == "helmet":
        if face == "front":
            hline(y0 + 1, x0, x1, pal[5])
            hline(y0 + 2, x0, x1, pal[2])
            _rivet(c, x0 + 1, y1 - 3, pal)
            _rivet(c, x1 - 3, y1 - 3, pal)
        elif face == "top":
            vline(mid, y0, y1, pal[5])
        else:
            hline(y0 + 2, x0, x1, pal[2])
            if w >= 4:
                _rivet(c, x0 + 1, y0 + 4, pal)
    elif name == "chest_plates":
        if face == "front":
            vline(mid, y0 + 1, y1 - 1, pal[1])
            hline(y0 + h // 2 + 1, x0 + 1, x1 - 1, pal[1])
            for (rx, ry) in ((x0 + 1, y0 + 1), (x1 - 3, y0 + 1), (x0 + 1, y1 - 3), (x1 - 3, y1 - 3),
                             (mid - 2, y0 + 1), (mid + 1, y0 + 1)):
                _rivet(c, rx, ry, pal)
            # Wear scratches.
            for _ in range(3):
                sx, sy = x0 + _ri(rng, 2, w - 3), y0 + _ri(rng, 2, h - 3)
                c.set(sx, sy, pal[5])
                c.set(sx + 1, sy + 1, pal[5])
        elif face == "top":
            vline(mid, y0, y1, pal[2])
            hline(y0 + h // 2, x0, x1, pal[2])
        elif face == "back":
            for y in range(y0 + 2, y1 - 1, 3):
                hline(y, x0 + 1, x1 - 1, pal[2])
        else:
            for y in range(y0 + 1, y1 - 1, 3):
                _rivet(c, x0 + w // 2 - 1, y, pal)
    elif name == "vents":
        if face in ("front", "back", "left", "right"):
            for y in range(y0 + 1, y1 - 1, 2):
                hline(y, x0 + 1, x1 - 1, pal[0])
    elif name == "barrel":
        if face in ("front", "back", "left", "right"):
            for x in range(x0 + 2, x1 - 1, 3):
                vline(x, y0, y1, pal[1])
            for x in range(x0, x1, 3):
                k = rng.choice([-1, 0, 0, 1])
                if k:
                    for xx in range(x, min(x + 2, x1)):
                        for yy in range(y0 + 1, y1 - 1):
                            r = rng.random()
                            if r < 0.5:
                                c.set(xx, yy, pal[3 + k])
        else:
            for rr in range(1, min(w, h) // 2, 2):
                hline(y0 + rr, x0 + rr, x1 - rr, pal[2])
                hline(y1 - 1 - rr, x0 + rr, x1 - rr, pal[2])
                vline(x0 + rr, y0 + rr, y1 - rr, pal[2])
                vline(x1 - 1 - rr, y0 + rr, y1 - rr, pal[2])
    elif name == "tank":
        if face == "front":
            hline(y0 + 3, x0, x1, pal[2])
            hline(y1 - 4, x0, x1, pal[2])
            for x in range(x0 + 1, x1 - 1, 4):
                _rivet(c, x, y0 + 1, pal)
        elif face in ("left", "right", "back"):
            hline(y0 + 3, x0, x1, pal[2])
            hline(y1 - 4, x0, x1, pal[2])
    elif name == "bricks":
        if face in ("front", "back", "left", "right", "top"):
            for yy in range(h):
                y = y0 + yy
                if yy % 3 == 2:
                    hline(y, x0, x1, pal[1])
                else:
                    off = 0 if (yy // 3) % 2 == 0 else 2
                    for x in range(x0 + off, x1, 4):
                        c.set(x, y, pal[1])
    elif name == "grate":
        for x in range(x0, x1):
            if (x - x0) % 2 == 0:
                vline(x, y0, y1, pal[0])
    elif name == "knuckles":
        if face in ("front", "bottom"):
            for x in range(x0 + 1, x1 - 1, 2):
                c.set(x, y0 + 1, pal[5])
                vline(x + 1, y0, y1, pal[1])
    elif name == "hammer":
        if face in ("front", "back"):
            hline(y0, x0, x1, pal[1])
            hline(y1 - 1, x0, x1, pal[1])
            vline(x0, y0, y1, pal[1])
            vline(x1 - 1, y0, y1, pal[1])
            for x in range(x0 + 1, x1 - 1):
                for y in range(y0 + 1, y1 - 1):
                    if rng.random() < 0.15:
                        c.set(x, y, pal[2])
        elif face in ("left", "right", "top", "bottom"):
            a = x0 + w // 2 - 1
            for x in range(a, a + 2):
                vline(x, y0, y1, pal[5])
    elif name == "blade":
        if face in ("left", "right"):
            hline(y1 - 1, x0, x1, pal[6])
            hline(y0, x0, x1, pal[1])
    elif name == "foot":
        if face == "front":
            vline(x0 + w // 3, y0 + 1, y1, pal[1])
            vline(x0 + 2 * w // 3, y0 + 1, y1, pal[1])
            hline(y0, x0, x1, pal[5])
    elif name == "tread":
        if face in ("front", "back", "top", "bottom"):
            for y in range(y0, y1, 2):
                hline(y, x0, x1, pal[0])
        else:
            # Sprocket wheels on the track sides.
            cy = y0 + h // 2
            for cx in (x0 + 3, x0 + w // 2, x1 - 4):
                for (dx, dy) in ((-1, -1), (0, -1), (1, -1), (-1, 0), (1, 0), (-1, 1), (0, 1), (1, 1)):
                    c.set(cx + dx, cy + dy, RUBBER[4])
                c.set(cx, cy, RUBBER[5])
            hline(y0, x0, x1, RUBBER[4])
            hline(y1 - 1, x0, x1, RUBBER[0])
    elif name == "tire":
        for y in range(y0, y1, 2):
            hline(y, x0, x1, pal[0])
    elif name == "hub":
        vline(mid, y0, y1, pal[1])
        hline(y0 + h // 2, x0, x1, pal[1])
        c.set(mid, y0 + h // 2, pal[6])
    elif name == "ring":
        if w >= 3 and h >= 3:
            for x in range(x0 + 1, x1 - 1):
                for y in range(y0 + 1, y1 - 1):
                    c.set(x, y, pal[1])
    elif name == "pipe":
        for y in range(y0 + 1, y1, 4):
            hline(y, x0, x1, pal[5])
    elif name == "battery":
        if face == "back":
            hline(y0 + 1, x0 + 1, x1 - 1, pal[5])
            hline(y1 - 2, x0 + 1, x1 - 1, pal[5])
            c.set(mid, y0 + h // 2, pal[6])
            c.set(mid - 1, y0 + h // 2, pal[6])
            c.set(mid, y0 + h // 2 - 1, pal[6])
    elif name == "coils":
        for y in range(y0, y1):
            if (y - y0) % 2 == 0:
                hline(y, x0, x1, pal[5])
    elif name == "solar":
        if face in ("top", "bottom"):
            for x in range(x0, x1):
                for y in range(y0, y1):
                    edge = x in (x0, x1 - 1) or y in (y0, y1 - 1)
                    grid = (x - x0) % 4 == 0 or (y - y0) % 3 == 0
                    if edge:
                        c.set(x, y, (182, 190, 200))
                    elif grid:
                        c.set(x, y, (120, 140, 175))
                    elif (x + y) % 7 == 0:
                        c.set(x, y, SOLAR[5])


# ---------------------------------------------------------------- overlays
def crack_face(c, rect, level, rng):
    """Thin hairline cracks. Level 1 is a few on big faces; level 3 covers most faces."""
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w < 4 or h < 4:
        return
    area = w * h
    per = {1: 150, 2: 70, 3: 32}[level]
    count = int(area / per + rng.random())
    if level >= 2 and count == 0 and area >= 24:
        count = 1
    dark = (38, 32, 32)
    edge = (92, 84, 80)
    for _ in range(count):
        x, y = x0 + _ri(rng, 1, w - 2), y0 + _ri(rng, 1, h - 2)
        dx, dy = rng.choice([(1, 1), (-1, 1), (1, 0), (0, 1)])
        for _ in range(_ri(rng, 2, 3 + level)):
            if not (x0 <= x < x1 and y0 <= y < y1):
                break
            c.set(x, y, dark, 255)
            if rng.random() < 0.25 and x0 <= x + 1 < x1:
                c.set(x + 1, y, edge, 255)
            if rng.random() < 0.3:
                dx, dy = rng.choice([(1, 1), (-1, 1), (1, 0), (0, 1)])
            x += dx
            y += dy


VINE = ramp("#4d8a2e")


def vine_face(c, face, rect, rng):
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    if w < 2 or h < 2:
        return
    if face == "top":
        for x in range(x0, x1):
            for y in range(y0, y1):
                if rng.random() < 0.35:
                    c.set(x, y, VINE[rng.choice([2, 3, 4])])
    elif face in ("front", "back", "left", "right"):
        for x in range(x0, x1):
            if rng.random() < 0.4:
                length = _ri(rng, 1, max(1, min(h, 7)))
                for y in range(y0, y0 + length):
                    c.set(x, y, VINE[rng.choice([2, 3, 3, 4])])
                if rng.random() < 0.5:
                    c.set(x, y0 + length, VINE[5])


def glow_face(c, rect, deco_name):
    x0, y0, x1, y1 = rect
    w, h = x1 - x0, y1 - y0
    for xx in range(w):
        for yy in range(h):
            if deco_name == "grate" and xx % 2 == 0:
                continue
            edge = xx in (0, w - 1) or yy in (0, h - 1)
            v = 205 if edge and w > 2 and h > 2 else 255
            c.set(x0 + xx, y0 + yy, (v, v, v), 255)
