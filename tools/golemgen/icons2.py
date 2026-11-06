"""
Every hand-made item icon, rebuilt as a small 3D model with itemkit so they match the part and golem renders.
Runs last in gen.py and overwrites the older 16x16 drawings.
"""
import math
import os

import itemkit as K
from itemkit import Paint, Faces, Draw, Scene, mix, mul, rgb

# ------------------------------------------------------------------ palette
STEEL = "#9aa2ab"
DARK = "#3a3d44"
IRON = "#b8bcc2"
GOLD = "#e8b830"
BRASS = "#c89a3a"
RUST = "#8a4a26"
RED = "#c8342c"
CREAM = "#e8dcc0"
BLUE = "#2d62b4"
RUBBER = "#2a2a2e"
WOOD = "#9a6a3a"
GREEN = "#4caa3c"


def cyl(sc, c, r, h, paint, axis="y", glow=False, rot=(0, 0, 0), pivot=None, parent=None):
    """An octagonal prism: two square boxes, one turned 45 degrees about the axis."""
    x, y, z = c
    for turn in (0, 45):
        if axis == "y":
            o, s, rr = (-r, -h / 2, -r), (2 * r, h, 2 * r), (rot[0], rot[1] + turn, rot[2])
        elif axis == "x":
            o, s, rr = (-h / 2, -r, -r), (h, 2 * r, 2 * r), (rot[0] + turn, rot[1], rot[2])
        else:
            o, s, rr = (-r, -r, -h / 2), (2 * r, 2 * r, h), (rot[0], rot[1], rot[2] + turn)
        sc.box(o, s, paint, pivot=pivot or (x, y, z), rot=rr, glow=glow, parent=parent)


def save(img, assets, name):
    img.save(os.path.join(assets, "textures/item", name + ".png"))


# ================================================================== materials and tools
def steel_ingot():
    sc = Scene(k=2)
    def shine(f, r):
        if f.role == "top":
            f.hline(1, r[6], 2, f.w - 4)
            f.p(2, 2, r[6])
    p = Draw(STEEL, shine, grain=0.08)
    sc.box((-7, -1, -3.5), (14, 3, 7), p)
    sc.box((-5.5, -3, -2.5), (11, 2, 5), p)
    return sc.render(yaw=30, pitch=34)


def scrap():
    sc = Scene(k=2)
    def bolts(f, r):
        for x in range(2, f.w - 1, 5):
            f.p(x, 1, r[6]); f.p(x, f.h - 2, r[0])
    sc.box((-7, -1, -5), (11, 1.5, 8), Draw(RUST, bolts, grain=0.25), pivot=(0, 0, 0), rot=(0, 20, 8))
    sc.box((-3, -2.5, -4), (9, 1.2, 6), Draw("#7c848e", bolts, grain=0.12), pivot=(0, 0, 0), rot=(0, -25, -10))
    # A gear.
    cyl(sc, (-4, -3.2, 2), 3, 1.2, Paint("#6a6e76", grain=0.1))
    for a in range(0, 180, 45):
        sc.box((-4, -0.6, -0.8), (8, 1.2, 1.6), Paint("#6a6e76"), pivot=(-4, -3.2, 2), rot=(0, a, 0))
    sc.box((-0.8, -0.8, -0.8), (1.6, 1.6, 1.6), Paint("#2a2c30"), pivot=(-4, -3.9, 2))
    # A bolt.
    sc.box((-0.6, -0.6, -3), (1.2, 1.2, 6), Paint(IRON), pivot=(4, -3.5, 3), rot=(0, 60, 0))
    cyl(sc, (0, 0, 0), 1.2, 1.0, Paint(IRON), axis="z", pivot=(4, -3.5, 3), rot=(0, 60, 0))
    return sc.render(yaw=30, pitch=38)


def tyre():
    sc = Scene(k=2)
    def tread(f, r):
        if f.role in ("front", "side", "back", "left"):
            for x in range(0, f.w, 3):
                f.vline(x, r[1])
                f.vline(x + 1, r[4])
        if f.role in ("top", "bottom"):
            f.hline(0, r[4])
    n = 14
    R_, w = 7.5, 5.0
    seg = 2 * R_ * math.tan(math.pi / n) + 0.35
    par = ((0, 0, 0), (-68, 0, 0))
    for i in range(n):
        sc.box((-seg / 2, -w / 2, -R_), (seg, w, 3.0), Draw(RUBBER, tread, grain=0.06), rot=(0, i * 360 / n, 0),
               parent=par)
    hub = Draw("#8a9098", lambda f, r: f.role in ("top", "bottom") and [f.rect(x, y, x + 1, y + 1, r[1]) for x, y in
        ((f.w // 2 - 1, 2), (2, f.h // 2 - 1), (f.w - 4, f.h // 2 - 1), (f.w // 2 - 1, f.h - 4))], grain=0.05)
    cyl(sc, (0, 0, 0), 4.5, 3.6, hub, parent=par)
    cyl(sc, (0, -1.9, 0), 1.6, 0.8, Paint("#d0d4d8"), parent=par)
    return sc.render(yaw=-30, pitch=12)


def repair_kit():
    sc = Scene(k=2)
    def front(f, r):
        if f.role == "front":
            cx, cy = 7, f.h // 2 + 1
            f.rect(cx - 1, cy - 4, cx + 2, cy + 4, "#f2ede4")
            f.rect(cx - 4, cy - 1, cx + 5, cy + 2, "#f2ede4")
            f.hline(3, r[1])
    sc.box((-8, -6, -4.5), (16, 9, 9), Draw(RED, front))
    sc.box((-8.3, -8.2, -4.8), (16.6, 2.4, 9.6), Paint("#d8463c"))
    sc.box((-1.2, -7, -5.0), (2.4, 3, 0.8), Paint(IRON))
    sc.box((-4.5, -11, -0.8), (1.2, 3, 1.6), Paint(IRON))
    sc.box((3.3, -11, -0.8), (1.2, 3, 1.6), Paint(IRON))
    sc.box((-4.5, -11.6, -0.8), (9, 1.2, 1.6), Paint("#c8ccd2"))
    # A wrench leaning on the side.
    sc.box((-0.6, -7, -0.6), (1.2, 12, 1.2), Paint(STEEL), pivot=(9.2, -1, 2), rot=(0, 0, 18))
    sc.box((-1.8, -9, -0.7), (3.6, 2.4, 1.4), Paint(STEEL), pivot=(9.2, -1, 2), rot=(0, 0, 18))
    return sc.render(yaw=26, pitch=26)


def _paper_roll(sc, paper, base, roll_col, drawing):
    sc.box((-7, -0.4, -9), (14, 0.8, 16), Draw(paper, drawing, faces=("top",), grain=0.04, bevel=False))
    cyl(sc, (0, -1.0, -9), 1.8, 15.6, Paint(roll_col), axis="x")
    cyl(sc, (0, -0.4, 7.5), 1.2, 15.2, Paint(roll_col), axis="x")


def blueprint():
    sc = Scene(k=2)
    def golem(f, r):
        line = (210, 232, 255)
        faint = mix(BLUE, "#ffffff", 0.25)
        for x in range(2, f.w, 5):
            f.vline(x, faint)
        for y in range(2, f.h, 5):
            f.hline(y, faint)
        cx = f.w // 2
        f.rect(cx - 3, 5, cx + 3, 10, line); f.rect(cx - 2, 6, cx + 2, 9, BLUE)       # head
        f.p(cx - 1, 7, (255, 120, 90)); f.p(cx + 1, 7, (255, 120, 90))
        f.rect(cx - 6, 12, cx + 6, 21, line); f.rect(cx - 5, 13, cx + 5, 20, BLUE)    # torso
        f.rect(cx - 9, 12, cx - 7, 22, line); f.rect(cx + 7, 12, cx + 9, 22, line)    # arms
        f.rect(cx - 5, 23, cx - 2, 28, line); f.rect(cx + 2, 23, cx + 5, 28, line)    # legs
    _paper_roll(sc, BLUE, None, "#4a86d0", golem)
    return sc.render(yaw=24, pitch=52)


def garage_blueprint():
    sc = Scene(k=2)
    def garage(f, r):
        line = (230, 240, 255)
        faint = mix("#2f6f9a", "#ffffff", 0.22)
        for x in range(2, f.w, 5):
            f.vline(x, faint)
        for y in range(2, f.h, 5):
            f.hline(y, faint)
        # A garage front: roof, walls, roller door, a laser on top.
        for i in range(10):
            f.p(f.w // 2 - i, 6 + i // 2, line); f.p(f.w // 2 + i, 6 + i // 2, line)
        f.vline(f.w // 2 - 9, line, 11, 27); f.vline(f.w // 2 + 9, line, 11, 27)
        f.hline(27, line, f.w // 2 - 9, f.w // 2 + 9)
        for y in range(15, 27, 2):
            f.hline(y, line, f.w // 2 - 5, f.w // 2 + 5)
        f.rect(f.w // 2 - 1, 3, f.w // 2 + 1, 4, (255, 90, 70))
    _paper_roll(sc, "#2f6f9a", None, "#e08a2c", garage)
    return sc.render(yaw=24, pitch=52)


def rule_card(punched=True):
    sc = Scene(k=2)
    def holes(f, r):
        rows = [(3, "1011010"), (8, "0110101"), (13, "1101001"), (18, "0101110"), (23, "1010011")]
        for y, bits in rows:
            for i, b in enumerate(bits):
                x = 3 + i * 4
                if b == "1" and x + 1 < f.w:
                    f.rect(x, y, x + 1, y + 2, "#2a2420")
        f.hline(1, "#c8402c", 2, f.w - 3)
        f.rect(f.w - 4, f.h - 4, f.w - 2, f.h - 2, "#c8402c")
    sc.box((-7, -0.4, -9), (14, 0.8, 16), Draw(CREAM, holes, faces=("top",), grain=0.03))
    sc.box((4, -0.4, -10), (3, 0.8, 1.2), Paint(CREAM))
    return sc.render(yaw=18, pitch=56)


def golem_linker():
    sc = Scene(k=2)
    piv = (0, 0, 0)
    rot = (0, 0, -42)
    sc.box((-0.9, -2, -0.9), (1.8, 15, 1.8), Draw("#5a4636", lambda f, r: [f.hline(y, r[1]) for y in range(3, f.h, 6)]),
           pivot=piv, rot=rot)
    sc.box((-1.6, -4, -1.6), (3.2, 2.5, 3.2), Paint(BRASS), pivot=piv, rot=rot)
    sc.box((-2.4, -11, -2.4), (4.8, 6.5, 4.8), Paint("#a070e0"), pivot=piv, rot=(rot[0], 45, rot[2]), glow=True)
    sc.box((-1.4, -13, -1.4), (2.8, 2.5, 2.8), Paint("#d8b8ff"), pivot=piv, rot=(rot[0], 45, rot[2]), glow=True)
    sc.box((-3.2, -5.2, -0.6), (6.4, 1.2, 1.2), Paint(BRASS), pivot=piv, rot=rot)
    return sc.render(yaw=28, pitch=20)


def manual():
    sc = Scene(k=2)
    def cover(f, r):
        if f.role == "top":
            cx, cy = f.w // 2, f.h // 2
            for a in range(0, 360, 45):
                x = cx + round(5 * math.cos(math.radians(a))); y = cy + round(5 * math.sin(math.radians(a)))
                f.rect(x - 1, y - 1, x, y, GOLD)
            for yy in range(cy - 3, cy + 3):
                for xx in range(cx - 3, cx + 3):
                    if (xx - cx + 0.5) ** 2 + (yy - cy + 0.5) ** 2 <= 10:
                        f.p(xx, yy, GOLD)
            f.rect(cx - 1, cy - 1, cx, cy, "#5ad0ff")
            f.hline(2, mix(GOLD, "#000000", 0.3), 2, f.w - 3)
            f.hline(f.h - 3, mix(GOLD, "#000000", 0.3), 2, f.w - 3)
    pages = Draw(CREAM, lambda f, r: [f.hline(y, r[2]) for y in range(1, f.h, 2)], grain=0.0, bevel=False)
    sc.box((-7, -3.5, -9), (14, 7, 18), Faces(pages, top=Draw("#24406e", cover), bottom=Paint("#24406e"),
                                                 left=Paint("#1c3358")))
    sc.box((-7.6, -4.1, -9.4), (14.4, 0.8, 18.8), Draw("#24406e", cover))
    sc.box((-7.6, 3.3, -9.4), (14.4, 0.8, 18.8), Paint("#24406e"))
    sc.box((-7.8, -4.1, -9.4), (1.2, 8.2, 18.8), Paint("#1c3358"))
    sc.box((5.5, -2.5, -9.8), (2.4, 5, 1.2), Paint(BRASS))
    return sc.render(yaw=30, pitch=34)


def golem_tablet():
    sc = Scene(k=2)
    def screen(f, r):
        if f.role == "front":
            w, h = f.w, f.h
            cx, cy = w // 2, h // 2
            for rad in (4, 8):
                for a in range(0, 360, 6):
                    f.p(cx + round(rad * math.cos(math.radians(a))), cy + round(rad * math.sin(math.radians(a))), "#2a8a80")
            f.hline(cy, "#1e6a66", 2, w - 3); f.vline(cx, "#1e6a66", 2, h - 3)
            for x, y, c in ((cx + 3, cy - 5, "#7aff9a"), (cx - 6, cy + 2, "#7aff9a"), (cx + 6, cy + 4, "#ffd060"),
                            (cx, cy, "#ffffff")):
                f.rect(x, y, x + 1, y + 1, c)
            for i in range(9):
                f.p(cx + i // 2, cy - i, "#5affd0")
    sc.box((-8, -11, -1.5), (16, 21, 3), Paint("#3c4048"))
    sc.box((-6.5, -9.5, -1.7), (13, 16, 0.3), Draw("#0c2a30", screen, bevel=False, grain=0.0), glow=True)
    sc.box((5, -15, -0.6), (1.2, 4.5, 1.2), Paint(STEEL))
    sc.box((4.6, -16, -1), (2, 1.6, 2), Paint("#ff5a40"), glow=True)
    sc.box((-1.2, 8.4, -1.8), (2.4, 1, 0.4), Paint("#80868e"))
    return sc.render(yaw=22, pitch=14)


# ================================================================== modules
EMBLEMS = {
    "magnet": ["............",
               ".RRr....bBB.",
               ".RRr....bBB.",
               ".RRr....bBB.",
               ".RRr....bBB.",
               ".RRr....bBB.",
               ".RRrsssssBB.",
               ".SSSSSSSSSS.",
               "..SSSSSSSS..",
               "............",
               ".WW......WW.",
               "............"],
    "lamp": ["....YYYY....",
             "...YyyyyY...",
             "..YyWyyyyY..",
             "..YyWyyyyY..",
             "..YyyyyyyY..",
             "...YyyyyY...",
             "....YyyY....",
             "....SSSS....",
             "....ssss....",
             "....SSSS....",
             ".....ss.....",
             "............"],
    "speed": ["............",
              ".G....G.....",
              ".GG...GG....",
              "..GG...GG...",
              "...GG...GG..",
              "....gG...gG.",
              "....gG...gG.",
              "...GG...GG..",
              "..GG...GG...",
              ".GG...GG....",
              ".G....G.....",
              "............"],
    "storage": ["............",
                ".OOOOOOOOOO.",
                ".OooooooooO.",
                ".OooooooooO.",
                ".kkkkSSkkkk.",
                ".OooosSoooO.",
                ".OooooooooO.",
                ".OooooooooO.",
                ".OooooooooO.",
                ".OOOOOOOOOO.",
                "............",
                "............"],
    "plating": ["............",
                ".SSSSSSSSSS.",
                ".SWWSSSSSSs.",
                ".SWSSSSSSSs.",
                ".SSSSSSSSSs.",
                ".SSSSSSSSSs.",
                "..SSSSSSSs..",
                "..sSSSSSSs..",
                "...sSSSSs...",
                "....sSSs....",
                ".....ss.....",
                "............"],
    "regen": ["............",
              "..PP....PP..",
              ".PpWP..PppP.",
              ".PWppPPpppP.",
              ".PpppppppppP"[:12],
              ".PpppppppppP"[:12],
              "..PpppppppP.",
              "...PpppppP..",
              "....PpppP...",
              ".....PpP....",
              "......P.....",
              "............"],
    "self_repair": ["........OO..",
                    ".......O..O.",
                    "........O.O.",
                    ".......OOO..",
                    "......OO....",
                    ".....OO.....",
                    "....OO......",
                    "...OO.......",
                    ".OOO........",
                    "O.O.........",
                    "O..O........",
                    ".OO........."],
    None: ["............",
           ".ss..ss..ss.",
           ".ss..ss..ss.",
           "............",
           "............",
           ".ss..ss..ss.",
           ".ss..ss..ss.",
           "............",
           "............",
           ".ss..ss..ss.",
           ".ss..ss..ss.",
           "............"],
}
EMBLEM_COLORS = {"R": "#e0443a", "r": "#a02a24", "B": "#4a7ae8", "b": "#2a4aa8", "S": "#c8ccd4", "s": "#7c828c",
                 "W": "#ffffff", "Y": "#e8a820", "y": "#ffe070", "G": "#5aff7a", "g": "#2ac84a", "O": "#c8884a",
                 "o": "#a06a34", "k": "#4a2a14", "P": "#d83a5a", "p": "#ff7a9a"}


def _emblem(kind):
    grid = EMBLEMS[kind]

    def draw(f, r):
        if f.role != "top":
            return
        f.rect(1, 1, f.w - 2, f.h - 2, "#1a1e26")
        f.hline(1, "#2c323c", 1, f.w - 2)
        ox, oy = (f.w - 24) // 2, (f.h - 24) // 2
        for y, row in enumerate(grid):
            for x, ch in enumerate(row):
                if ch != ".":
                    f.rect(ox + 2 * x, oy + 2 * y, ox + 2 * x + 1, oy + 2 * y + 1, EMBLEM_COLORS[ch])
    return draw


def module(kind):
    sc = Scene(k=2)
    sc.box((-7, -2, -7), (14, 3, 14), Draw("#8a929c", _emblem(kind)))
    pins = Paint(GOLD, grain=0.0)
    for i in range(4):
        z = -5.5 + i * 3.4
        sc.box((-8.4, -0.8, z), (1.4, 1, 1.4), pins)
        sc.box((7.0, -0.8, z), (1.4, 1, 1.4), pins)
    for i in range(4):
        x = -5.5 + i * 3.4
        sc.box((x, -0.8, -8.4), (1.4, 1, 1.4), pins)
    led = {"magnet": "#ff4a3a", "lamp": "#ffe070", "speed": "#5aff7a", "storage": "#ffb050", "plating": "#c8d8ff",
           "regen": "#ff7aa0", "self_repair": "#ffb040"}.get(kind)
    if led:
        sc.box((4.4, -2.6, -6.2), (1.6, 0.8, 1.6), Paint(led), glow=True)
    return sc.render(yaw=24, pitch=52)


# ================================================================== rig gear
def war_horn():
    """A curved copper horn: thin mouthpiece on the left sweeping up to a wide flared bell on the right."""
    sc = Scene(k=2)
    ivory = "#d88a54"
    n = 9

    def at(t):
        x = -9 + 17 * t
        y = 5 - 11 * (t ** 1.6) + 4 * math.sin(math.pi * t) * 0.0
        y = 4 - 10 * t + 5 * (t - 0.5) ** 2 * 2
        return x, y

    for i in range(n):
        t = i / (n - 1)
        x, y = at(t)
        x2, y2 = at(min(1, t + 0.05))
        ang = math.degrees(math.atan2(y2 - y, x2 - x))
        r = 0.7 + 2.9 * t ** 1.5
        col = mix("#8a4a2a", ivory, min(1, t * 1.6))
        cyl(sc, (0, 0, 0), r, 2.6, Paint(col, grain=0.06), axis="x", pivot=(x, y, 0), rot=(0, 0, ang))
        if i in (3, 6, 8):
            cyl(sc, (0, 0, 0), r + 0.35, 0.8, Paint("#e8b84a"), axis="x", pivot=(x, y, 0), rot=(0, 0, ang))
    x, y = at(1.0)
    x0, y0 = at(0.95)
    ang = math.degrees(math.atan2(y - y0, x - x0))
    cyl(sc, (0, 0, 0), 3.0, 0.3, Paint("#1e1812", grain=0.0), axis="x", pivot=(x + 1.3, y, 0), rot=(0, 0, ang))
    # A leather strap hanging under.
    sc.box((-6, -0.5, -0.6), (12, 1, 1.2), Paint("#7a4422"), pivot=(-0.5, 3.6, 0), rot=(0, 0, 8))
    return sc.render(yaw=-34, pitch=16)


def spray_can(cap, bands):
    sc = Scene(k=2)
    def label(f, r):
        if f.role in ("front", "side", "back", "left"):
            n = len(bands)
            for y in range(f.h):
                band = bands[min(n - 1, (y * n) // f.h)] if 4 <= y < f.h - 3 else None
                if band:
                    for x in range(f.w):
                        f.p(x, y, mul(band, 1.08 if x < 2 else 0.9 if x > f.w - 3 else 1.0))
            f.hline(3, "#ececec"); f.hline(f.h - 3, "#ececec")
    cyl(sc, (0, 1, 0), 4.2, 16, Draw("#c8ccd2", label, grain=0.0))
    cyl(sc, (0, -7.6, 0), 3.4, 1.6, Paint("#d8dce2"))
    cyl(sc, (0, -9.6, 0), 2.6, 2.6, Paint(cap))
    sc.box((-0.7, -11.2, -3.2), (1.4, 1.2, 1.8), Paint("#2a2a2e"))
    return sc.render(yaw=26, pitch=18)


def jerry_can(fill="#c8342c"):
    sc = Scene(k=2)
    def emboss(f, r):
        if f.role == "front":
            for i in range(min(f.w, f.h) - 6):
                x = 3 + i * (f.w - 7) / (min(f.w, f.h) - 7)
                f.p(int(x), 3 + i, r[5]); f.p(int(x) + 1, 3 + i, r[1])
                f.p(f.w - 4 - int(x) + 3, 3 + i, r[5]); f.p(f.w - 3 - int(x) + 3, 3 + i, r[1])
            f.hline(1, r[5], 1, f.w - 2); f.hline(f.h - 2, r[1], 1, f.w - 2)
    sc.box((-5, -8, -7), (10, 17, 14), Draw(fill, emboss))
    for x in (-3.4, 0.0):
        sc.box((x, -11, -5), (1.6, 3.2, 1.4), Paint(fill))
    sc.box((-3.4, -11.6, -5), (5, 1.4, 1.4), Paint(fill))
    sc.box((-2, -10.5, 2.6), (4, 2.5, 4), Paint("#4a4e56"))
    sc.box((-1.2, -11.6, 3.4), (2.4, 1.2, 2.4), Paint("#2a2a2e"))
    return sc.render(yaw=-32, pitch=20)


def medal(metal, ribbon, emblem):
    sc = Scene(k=2)
    def face(f, r):
        if f.role == "front":
            cx, cy = f.w // 2, f.h // 2
            if emblem == "star":
                for a in range(5):
                    ang = math.radians(-90 + a * 72)
                    for t in range(0, 7):
                        f.p(cx + round(t * math.cos(ang)), cy + round(t * math.sin(ang)), r[6])
                f.rect(cx - 2, cy - 2, cx + 1, cy + 1, r[6])
            else:
                for t in range(-5, 6):
                    f.p(cx + t, cy + t, r[6]); f.p(cx + t, cy - t, r[6])
                    f.p(cx + t + 1, cy + t, r[1]); f.p(cx + t + 1, cy - t, r[1])
    stripes = Draw(ribbon, lambda f, r: (f.vline(f.w // 2 - 1, "#f0f0f0"), f.vline(f.w // 2, "#f0f0f0")), bevel=False)
    sc.box((-3.5, -16, 0.4), (7, 11, 0.8), stripes)
    sc.box((-2.4, -7.4, -0.4), (4.8, 1.8, 1.6), Paint(metal))
    cyl(sc, (0, 0, 0), 6, 1.6, Draw(metal, face, bevel=False), axis="z")
    cyl(sc, (0, 0, -0.3), 4.6, 1.6, Draw(metal, face, bevel=False, grain=0.0), axis="z")
    return sc.render(yaw=14, pitch=8)


def solar_panel():
    sc = Scene(k=2)
    def cells(f, r):
        if f.role == "top":
            for y in range(f.h):
                for x in range(f.w):
                    edge = x % 8 == 0 or y % 8 == 0
                    f.p(x, y, "#c8ccd2" if edge else ("#2a5ab0" if (x // 2 + y // 2) % 2 else "#3a78d8"))
            f.p(5, 3, "#d8f0ff"); f.p(6, 3, "#d8f0ff"); f.p(5, 4, "#d8f0ff")
    sc.box((-8, -0.6, -8), (16, 1.2, 16), Draw("#3a3e46", cells), pivot=(0, -4, 0), rot=(32, 0, 0))
    sc.box((-1, -3, -1), (2, 9, 2), Paint(STEEL))
    sc.box((-4, 6, -4), (8, 1.5, 8), Paint("#5a5e66"))
    return sc.render(yaw=26, pitch=26)


def cub_charger():
    sc = Scene(k=2)
    def pad(f, r):
        if f.role == "top":
            for y in range(f.h):
                for x in range(f.w):
                    if (x + y) % 6 == 0:
                        f.p(x, y, r[4])
    def bolt(f, r):
        if f.role == "front":
            pts = [(5, 1), (4, 2), (3, 3), (2, 4), (2, 5), (3, 5), (4, 5), (5, 5), (4, 6), (3, 7), (2, 8), (1, 9)]
            for x, y in pts:
                f.rect(x, y, x + 1, y, "#ffe04a")
    sc.box((-8, -1, -6), (16, 3, 12), Draw("#e890a8", pad))
    sc.box((-8.5, 2, -6.5), (17, 1.6, 13), Paint("#6a6e76"))
    sc.box((-3, -9, 3), (6, 8, 3), Paint("#5a5e66"))
    sc.box((-2.2, -8.2, 2.7), (4.4, 6, 0.4), Draw("#1a1e26", bolt, bevel=False, grain=0.0), glow=True)
    return sc.render(yaw=26, pitch=26)


def rig_ram():
    sc = Scene(k=2)
    sc.box((-10, -2, -2), (20, 4, 3), Draw("#4a4e56", lambda f, r: [f.p(x, 1, r[6]) for x in range(2, f.w, 6)]))
    for x in (-8, -4, 0, 4, 8):
        sc.box((-1, -1, -5), (2, 2, 4), Paint(IRON), pivot=(x, 0, -1), rot=(0, 0, 45))
        sc.box((-0.6, -0.6, -7.5), (1.2, 1.2, 3), Paint("#d8dce2"), pivot=(x, 0, -1), rot=(0, 0, 45))
    sc.box((-8, -1, 1), (3, 2, 5), Paint(RUST))
    sc.box((5, -1, 1), (3, 2, 5), Paint(RUST))
    return sc.render(yaw=-36, pitch=28)


def rig_turret():
    sc = Scene(k=2)
    sc.box((-6, 2, -6), (12, 3, 12), Paint("#4a4e56"))
    cyl(sc, (0, -1, 0), 4.6, 4, Paint("#6a7078"))
    for z in (-1.8, 0.6):
        sc.box((-0.6, -0.6, -12), (1.2, 1.2, 9), Paint("#2a2c30"), pivot=(0, -1.5, z + 0.6))
        sc.box((-0.9, -0.9, -13), (1.8, 1.8, 1.6), Paint(DARK), pivot=(0, -1.5, z + 0.6))
    sc.box((-2, -4.6, 1), (4, 1.6, 3), Paint("#5a6068"))
    return sc.render(yaw=-40, pitch=26)


def rig_plating():
    sc = Scene(k=2)
    def rivets(f, r):
        if f.role in ("top", "front"):
            for x in (1, f.w - 2):
                for y in (1, f.h - 2):
                    f.p(x, y, r[6])
    for i, (y, ry) in enumerate(((3, 0), (0.5, 8), (-2, -6))):
        sc.box((-8, y, -6), (16, 2, 12), Draw("#6a7280" if i != 1 else "#7c8490", rivets, grain=0.1),
               pivot=(0, 0, 0), rot=(0, ry, 0))
    return sc.render(yaw=28, pitch=34)


def rig_harpoon():
    sc = Scene(k=2)
    sc.box((-4, 3, -4), (8, 2, 8), Paint("#4a4e56"))
    sc.box((-1, -1, -1), (2, 4, 2), Paint(DARK))
    piv, rot = (0, -2, 0), (0, 0, -20)
    sc.box((-6, -1.6, -1.6), (12, 3.2, 3.2), Paint(RUST), pivot=piv, rot=rot)
    sc.box((-0.5, -0.5, -0.5), (12, 1, 1), Paint("#d8dce2"), pivot=piv, rot=rot)
    sc.box((-1.4, -1.4, -1.4), (2.8, 2.8, 2.8), Paint("#d8dce2"), pivot=(piv[0] + 11 * math.cos(math.radians(20)),
           piv[1] - 11 * math.sin(math.radians(20)), 0), rot=(0, 45, -20))
    sc.box((-6.8, -2.4, -2.2), (1.2, 4.8, 4.4), Paint(BRASS), pivot=piv, rot=rot)
    return sc.render(yaw=30, pitch=24)


def rig_smoke():
    sc = Scene(k=2)
    for x in (-3.2, 3.2):
        cyl(sc, (x, 0, 0), 2.8, 13, Draw("#3c4048", lambda f, r: f.hline(f.h // 2, "#e8c040")))
        cyl(sc, (x, -7.4, 0), 1.6, 1.8, Paint(STEEL))
        cyl(sc, (x, -9, 0), 0.8, 1.6, Paint("#2a2a2e"))
    sc.box((-7, -1, -3.2), (14, 1.6, 0.8), Paint(RUST))
    sc.box((-7, 3, -3.2), (14, 1.6, 0.8), Paint(RUST))
    # A puff of smoke.
    for (x, y, s) in ((4, -12, 2.4), (6, -14, 1.8), (2, -13.5, 1.6)):
        sc.box((-s, -s, -s), (2 * s, 2 * s, 2 * s), Paint("#d8d8d8", grain=0.0), pivot=(x, y, 0), rot=(20, 30, 10))
    return sc.render(yaw=24, pitch=20)


def rig_spikes():
    sc = Scene(k=2)
    cyl(sc, (0, 0, 0), 3.4, 18, Draw("#3a3d44", lambda f, r: [f.vline(x, r[1]) for x in range(0, f.w, 4)]), axis="x")
    for x in (-7, -3.5, 0, 3.5, 7):
        for a in (0, 120, 240):
            sc.box((-0.5, -6.4, -0.5), (1, 3.4, 1), Paint("#d8dce2"), pivot=(x, 0, 0), rot=(a, 0, 0))
    return sc.render(yaw=24, pitch=26)


def rig_rack():
    sc = Scene(k=2)
    rail = Paint("#3a3d44")
    for z in (-6, 5):
        sc.box((-9, 0, z), (18, 1.2, 1.2), rail)
        sc.box((-9, -2, z), (1.2, 2, 1.2), rail)
        sc.box((7.8, -2, z), (1.2, 2, 1.2), rail)
    for x in (-7, -2, 3, 7):
        sc.box((x, -0.4, -6), (1, 1, 12.2), rail)
    sc.box((-7.5, -7, -5), (7, 7, 7), Draw(WOOD, lambda f, r: (f.hline(f.h // 2, r[1]), f.vline(1, r[2]), f.vline(f.w - 2, r[2]))))
    cyl(sc, (4, -1.8, -0.5), 3.6, 3.2, Draw(RUBBER, lambda f, r: f.role == "top" and f.rect(f.w // 2 - 2, f.h // 2 - 2,
        f.w // 2 + 1, f.h // 2 + 1, "#121214")))
    sc.box((-8, -7.6, -1.6), (17, 0.6, 1.2), Paint("#e0a030"))
    return sc.render(yaw=28, pitch=34)


def garage_door():
    sc = Scene(k=2)
    def slats(f, r):
        if f.role == "front":
            for y in range(0, f.h, 4):
                f.hline(y, r[1]); f.hline(y + 1, r[5])
            f.rect(f.w // 2 - 3, f.h - 5, f.w // 2 + 2, f.h - 4, "#2a2a2e")
    sc.box((-8, -10, -0.8), (16, 20, 1.6), Draw("#a8aeb6", slats))
    sc.box((-9.5, -11.5, -1.4), (1.6, 22, 2.8), Paint("#e0b030"))
    sc.box((7.9, -11.5, -1.4), (1.6, 22, 2.8), Paint("#e0b030"))
    cyl(sc, (0, -12.2, 0.4), 2, 19, Paint("#4a4e56"), axis="x")
    return sc.render(yaw=24, pitch=12)


# ================================================================== generate
def generate(assets):
    out = {
        "steel_ingot": steel_ingot(), "scrap": scrap(), "tyre": tyre(), "repair_kit": repair_kit(),
        "blueprint": blueprint(), "garage_blueprint": garage_blueprint(), "rule_card": rule_card(),
        "golem_linker": golem_linker(), "manual": manual(), "golem_tablet": golem_tablet(),
        "module_casing": module(None), "war_horn": war_horn(), "jerry_can": jerry_can(),
        "legend_medal": medal(GOLD, "#7a3ac8", "star"), "arena_medal": medal("#c8ccd4", "#2a6ad0", "swords"),
        "solar_panel": solar_panel(), "cub_charger": cub_charger(), "rig_ram": rig_ram(),
        "rig_turret": rig_turret(), "rig_plating": rig_plating(), "rig_harpoon": rig_harpoon(),
        "rig_smoke": rig_smoke(), "rig_spikes": rig_spikes(), "rig_rack": rig_rack(), "garage_door": garage_door(),
    }
    for kind in ("magnet", "lamp", "speed", "storage", "plating", "regen", "self_repair"):
        out["module_" + kind] = module(kind)
    sprays = {"flames": ("#e83c1c", ["#2a2a2e", "#e8401c", "#f8a020", "#ffe050"]),
              "hazard": ("#f0c020", ["#f0c020", "#2a2a2e", "#f0c020", "#2a2a2e"]),
              "rust": ("#8a4a26", ["#6a3418", "#8a4a26", "#b06a34", "#8a4a26"]),
              "camo": ("#4a5a2a", ["#3a4a24", "#6a7a3a", "#8a7a4a", "#3a4a24"]),
              "chrome": ("#e8ecf0", ["#9aa2ab", "#e8ecf0", "#c8ccd2", "#ffffff"]),
              "tiger": ("#f08020", ["#f08020", "#1a1a1a", "#f08020", "#1a1a1a"])}
    for name, (cap, bands) in sprays.items():
        out["spray_can_" + name] = spray_can(cap, bands)
    for name, img in out.items():
        save(img, assets, name)
    print("icons2: %d item icons rebuilt as 3D renders" % len(out))
    return out
