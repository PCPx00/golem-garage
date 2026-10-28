"""Pixel art for the Golem Engineer's Manual: the book GUI, its sprites, the item icon and diagrams."""
import os
import random

from PIL import Image

import icons
import paint

COVER = (43, 52, 66)
COVER_HI = (66, 79, 99)
COVER_LO = (26, 31, 40)
BRASS = (201, 160, 82)
BRASS_HI = (243, 214, 140)
BRASS_LO = (128, 92, 42)
PAPER = (240, 229, 203)
PAPER_LO = (222, 207, 175)
GRID = (233, 221, 195)
INK = (31, 59, 99)
INK_HI = (64, 108, 168)
BROWN = (110, 95, 69)
BROWN_LO = (140, 122, 91)


def _put(px, x, y, c, w, h):
    if 0 <= x < w and 0 <= y < h:
        px[x, y] = tuple(c[:3]) + ((c[3],) if len(c) > 3 else (255,))


def _rect(px, x0, y0, x1, y1, c, w=512, h=256):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            _put(px, x, y, c, w, h)


def _mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def book_gui():
    w, h = 512, 256
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    rnd = random.Random(7)
    bw, bh = 320, 200

    # ---- cover --------------------------------------------------------------------
    for y in range(bh):
        for x in range(bw):
            n = rnd.randint(-4, 4)
            c = tuple(max(0, min(255, v + n)) for v in COVER)
            px[x, y] = c + (255,)
    for x in range(bw):
        px[x, 0] = COVER_LO + (255,)
        px[x, bh - 1] = COVER_LO + (255,)
        px[x, 1] = COVER_HI + (255,)
        px[x, bh - 2] = _mix(COVER, COVER_LO, 0.5) + (255,)
    for y in range(bh):
        px[0, y] = COVER_LO + (255,)
        px[bw - 1, y] = COVER_LO + (255,)
        px[1, y] = COVER_HI + (255,)
        px[bw - 2, y] = _mix(COVER, COVER_LO, 0.5) + (255,)
    # Rounded outer corners.
    for (cx, cy) in ((0, 0), (bw - 1, 0), (0, bh - 1), (bw - 1, bh - 1)):
        px[cx, cy] = (0, 0, 0, 0)

    # Brass corner guards with rivets.
    def corner(fx, fy):
        for i in range(12):
            for j in range(12 - i):
                x = i if fx > 0 else bw - 1 - i
                y = j if fy > 0 else bh - 1 - j
                edge = (i + j == 11) or i == 0 or j == 0
                col = BRASS_LO if edge else (BRASS_HI if i + j < 4 else BRASS)
                if (x, y) not in ((0, 0), (bw - 1, 0), (0, bh - 1), (bw - 1, bh - 1)):
                    px[x, y] = col + (255,)
        rx = 4 if fx > 0 else bw - 5
        ry = 4 if fy > 0 else bh - 5
        px[rx, ry] = BRASS_HI + (255,)
        px[rx + (1 if fx > 0 else -1), ry + (1 if fy > 0 else -1)] = BRASS_LO + (255,)
    for fx in (1, -1):
        for fy in (1, -1):
            corner(fx, fy)

    # ---- pages ------------------------------------------------------------------------
    pages = [(8, 156), (164, 312)]
    top, bottom = 6, 194
    for (x0, x1) in pages:
        left = x0 < 160
        for y in range(top, bottom):
            for x in range(x0, x1):
                # Darken toward the gutter.
                gd = (x1 - x) if left else (x - x0)
                shade = max(0.0, 1.0 - gd / 14.0) * 0.18
                base = PAPER
                if (x - x0) % 8 == 0 or (y - top) % 8 == 0:
                    base = GRID
                c = _mix(base, (120, 100, 70), shade)
                n = rnd.randint(-3, 3)
                c = tuple(max(0, min(255, v + n)) for v in c)
                px[x, y] = c + (255,)
        # Page stack along the outer edge and the bottom.
        ox = x0 - 1 if left else x1
        for y in range(top + 1, bottom + 1):
            px[ox, y] = PAPER_LO + (255,)
        for i, y in enumerate((bottom, bottom + 1)):
            for x in range(x0 + (2 if left else 0), x1 - (0 if left else 2)):
                px[x, y] = (PAPER_LO if i == 0 else BROWN_LO) + (255,)
        for x in range(x0, x1):
            px[x, top - 1] = _mix(COVER_LO, PAPER_LO, 0.4) + (255,)

    # ---- gutter with binding rings -----------------------------------------------------
    for y in range(top - 1, bottom + 2):
        for x in range(156, 164):
            t = abs(x - 159.5) / 4.0
            px[x, y] = _mix((40, 34, 28), (92, 78, 60), t) + (255,)
    for y in range(18, 180, 16):
        for dy in range(4):
            for x in range(154, 166):
                if dy in (0, 3) and x in (154, 165):
                    continue
                c = (196, 200, 206) if dy == 0 else (150, 156, 164) if dy < 3 else (88, 92, 100)
                px[x, y + dy] = c + (255,)
        px[155, y + 1] = (240, 242, 245, 255)

    # ---- sprites ------------------------------------------------------------------------
    # Slot 18x18 at (0, 208): inset parchment well.
    _rect(px, 0, 208, 17, 225, BROWN_LO)
    _rect(px, 1, 209, 16, 224, (217, 203, 168))
    _rect(px, 1, 209, 16, 209, BROWN)
    _rect(px, 1, 209, 1, 224, BROWN)
    _rect(px, 2, 224, 16, 224, (246, 238, 219))
    _rect(px, 16, 210, 16, 224, (246, 238, 219))

    # Crafting arrow 22x15 at (24, 208).
    for y in range(15):
        for x in range(22):
            inside = (x < 14 and 5 <= y <= 9) or (x >= 13 and abs(y - 7) <= (21 - x) * 7 // 8)
            if inside:
                px[24 + x, 208 + y] = BROWN + (255,)
    for x in range(1, 13):
        px[24 + x, 214] = BROWN_LO + (255,)

    def nav_arrow(u, right, hover):
        v = 208 + (11 if hover else 0)
        col = INK_HI if hover else INK
        for y in range(10):
            for x in range(18):
                ax = x if right else 17 - x
                inside = (ax < 11 and 3 <= y <= 6) or (ax >= 10 and abs(y - 4.5) <= (17 - ax) * 0.75)
                if inside:
                    edge = y in (3, 6) and ax < 10
                    px[u + x, v + y] = (_mix(col, (255, 255, 255), 0.25) if y <= 4 else col) + (255,)
    for hover in (False, True):
        nav_arrow(48, True, hover)
        nav_arrow(68, False, hover)

    # Back arrow (curved) 13x10 at (88, 208).
    back = ["......#......", ".....##......", "....#########", "...##########",
            "....#######.##", ".....##....##", "......#....##", "...........##",
            "......#######", "......######."]
    for hover in (False, True):
        v = 208 + (11 if hover else 0)
        col = INK_HI if hover else INK
        for y, row in enumerate(back):
            for x, ch in enumerate(row[:13]):
                if ch == "#":
                    px[88 + x, v + y] = col + (255,)
    # Home 13x11 at (104, 208).
    home = [".....###.....", "....#####....", "...#######...", "..#########..",
            ".###########.", "#############", "..##.....##..", "..##.###.##..",
            "..##.###.##..", "..##.###.##..", "..#########.."]
    for hover in (False, True):
        v = 208 + (12 if hover else 0)
        col = BRASS_HI if hover else BRASS
        for y, row in enumerate(home):
            for x, ch in enumerate(row):
                if ch == "#":
                    px[104 + x, v + y] = (col if y < 6 else BRASS_LO if hover is False else BRASS) + (255,)

    # Pedestal 64x10 at (120, 208): a round steel plate seen from the front.
    for y in range(10):
        for x in range(64):
            dx = (x - 31.5) / 32.0
            dy = (y - 3.5) / 4.5
            if dx * dx + dy * dy <= 1.0:
                if y <= 2:
                    c = (178, 184, 192)
                elif y <= 5:
                    c = (132, 138, 148)
                else:
                    c = (86, 90, 98)
                if dx * dx + dy * dy > 0.82:
                    c = (60, 62, 68)
                px[120 + x, 208 + y] = c + (255,)
    for x in range(26, 38):
        px[120 + x, 211] = (110, 232, 255, 255)

    # Divider 80x5 at (0, 230).
    for x in range(80):
        d = abs(x - 39.5)
        if d > 38:
            continue
        a = int(255 * min(1.0, (38 - d) / 12.0))
        px[x, 232] = INK + (a,)
    for y in range(5):
        r = 2 - abs(y - 2)
        for x in range(40 - r - 1, 40 + r + 1):
            px[x, 230 + y] = BRASS + (255,)
    px[39, 232] = BRASS_HI + (255,)
    return img


def manual_icon():
    ic = icons.Icon(paint.ramp("#2b3442"))
    ic.block(2, 1, 13, 14)
    for y in range(2, 14):
        ic.p(12, y, (232, 220, 190))
        ic.p(13, y, (200, 186, 150))
    ic.rect(3, 2, 3, 13, (20, 24, 32))
    # Brass gear emblem: a ring with eight teeth around a cyan core.
    import math
    gold = (233, 190, 92)
    gold_lo = (160, 118, 48)
    cx, cy = 7.5, 6.5
    for y in range(2, 12):
        for x in range(4, 12):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = math.cos(ang * 4) > 0.3
            if 1.6 <= r <= 2.9 or (2.9 < r <= 3.9 and tooth):
                ic.p(x, y, gold if dy < 0.5 else gold_lo)
    ic.p(7, 6, (110, 232, 255)); ic.p(8, 6, (110, 232, 255)); ic.p(7, 7, (60, 170, 210)); ic.p(8, 7, (60, 170, 210))
    ic.hline(12, 5, 10, gold_lo)
    ic.p(2, 1, (0, 0, 0)); ic.p(13, 1, (0, 0, 0))
    return ic.img


# ------------------------------------------------------------------ diagrams
def _brick(px, x0, y0, s, w, h):
    for y in range(s):
        for x in range(s):
            mortar = y % 4 == 3 or (x + (2 if (y // 4) % 2 else 0)) % 6 == 5
            c = (110, 110, 116) if mortar else (150, 150, 156)
            if (x + y) % 7 == 0 and not mortar:
                c = (165, 165, 170)
            _put(px, x0 + x, y0 + y, c, w, h)


def garage_top():
    """Top view of a 5 x 5 Garage: walls, the controller in a wall, emitters (dashed) above the bay."""
    cell = 12
    n = 7
    w, h = n * cell + 2, n * cell + 2
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for gy in range(n):
        for gx in range(n):
            x0, y0 = 1 + gx * cell, 1 + gy * cell
            wall = gx in (0, n - 1) or gy in (0, n - 1)
            if wall:
                _brick(px, x0, y0, cell, w, h)
            else:
                for y in range(cell):
                    for x in range(cell):
                        c = (226, 214, 186) if (x + y) % 2 else (218, 205, 176)
                        if x == 0 or y == 0:
                            c = (204, 190, 160)
                        _put(px, x0 + x, y0 + y, c, w, h)
    # Controller in the left wall, middle row.
    cx0, cy0 = 1, 1 + 3 * cell
    for y in range(cell):
        for x in range(cell):
            c = (60, 64, 72)
            if 2 <= x <= 9 and 2 <= y <= 7:
                c = (40, 140, 170) if (y % 2) else (110, 232, 255)
            if y >= 9 and (x + y) % 4 < 2:
                c = (235, 190, 40)
            elif y >= 9:
                c = (30, 30, 30)
            _put(px, cx0 + x, cy0 + y, c, w, h)
    # Emitters seen from above (dashed cyan rings).
    for (gx, gy) in ((2, 2), (4, 4), (4, 2), (2, 4)):
        x0, y0 = 1 + gx * cell, 1 + gy * cell
        for i in range(cell):
            for (x, y) in ((i, 1), (i, cell - 2), (1, i), (cell - 2, i)):
                if 1 <= x <= cell - 2 and 1 <= y <= cell - 2 and (x + y) % 3 != 0:
                    _put(px, x0 + x, y0 + y, (30, 170, 210), w, h)
        for y in range(4, 8):
            for x in range(4, 8):
                _put(px, x0 + x, y0 + y, (110, 232, 255), w, h)
    # The golem in the middle (seen from above).
    gx0, gy0 = 1 + 3 * cell + 1, 1 + 3 * cell + 1
    for y in range(10):
        for x in range(10):
            c = (205, 198, 188)
            if x in (0, 9) or y in (0, 9):
                c = (80, 76, 72)
            if 3 <= x <= 6 and 3 <= y <= 6:
                c = (180, 172, 160)
            _put(px, gx0 + x, gy0 + y, c, w, h)
    # Outline.
    for x in range(w):
        _put(px, x, 0, (60, 52, 40), w, h)
        _put(px, x, h - 1, (60, 52, 40), w, h)
    for y in range(h):
        _put(px, 0, y, (60, 52, 40), w, h)
        _put(px, w - 1, y, (60, 52, 40), w, h)
    return img


def garage_side():
    """Side cut-away: floor, walls, ceiling emitters firing beams at a golem on the lift."""
    cell = 10
    cols, rows = 9, 7
    w, h = cols * cell + 2, rows * cell + 2
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    px = img.load()
    for gy in range(rows):
        for gx in range(cols):
            x0, y0 = 1 + gx * cell, 1 + gy * cell
            if gx in (0, cols - 1) or gy in (0, rows - 1):
                _brick(px, x0, y0, cell, w, h)
            else:
                for y in range(cell):
                    for x in range(cell):
                        _put(px, x0 + x, y0 + y, (40, 44, 54), w, h)
    # Emitters in the ceiling.
    em = [(3, 0), (5, 0)]
    for (gx, gy) in em:
        x0, y0 = 1 + gx * cell, 1 + gy * cell
        for y in range(cell):
            for x in range(cell):
                c = (120, 126, 136)
                if y >= 6 and 2 <= x <= 7:
                    c = (110, 232, 255)
                _put(px, x0 + x, y0 + y, c, w, h)
    # Lift platform and golem.
    fx = 1 + 4 * cell + cell // 2
    floor = 1 + (rows - 1) * cell
    for x in range(fx - 9, fx + 9):
        for y in range(floor - 6, floor - 3):
            _put(px, x, y, (90, 94, 102), w, h)
        _put(px, x, floor - 6, (170, 176, 186), w, h)
    for y in range(floor - 3, floor):
        for x in range(fx - 2, fx + 2):
            _put(px, x, y, (60, 62, 68), w, h)
    gb = floor - 6
    body = [(-3, -26, 6, 6, (205, 198, 188)), (-6, -20, 12, 9, (190, 182, 170)), (-8, -20, 2, 11, (170, 162, 150)),
            (6, -20, 2, 11, (170, 162, 150)), (-4, -11, 3, 11, (160, 152, 140)), (1, -11, 3, 11, (160, 152, 140))]
    for (ox, oy, bw, bh, c) in body:
        for y in range(bh):
            for x in range(bw):
                _put(px, fx + ox + x, gb + oy + y, c, w, h)
    _put(px, fx - 2, gb - 24, (255, 74, 54), w, h)
    _put(px, fx + 1, gb - 24, (255, 74, 54), w, h)
    # Beams from each emitter to the golem.
    for (gx, gy) in em:
        sx = 1 + gx * cell + cell // 2
        sy = 1 + cell
        tx, ty = fx, gb - 16
        steps = max(abs(tx - sx), abs(ty - sy))
        for i in range(steps + 1):
            x = round(sx + (tx - sx) * i / steps)
            y = round(sy + (ty - sy) * i / steps)
            _put(px, x, y, (180, 250, 255), w, h)
            _put(px, x + 1, y, (60, 190, 230), w, h)
    for x in range(w):
        _put(px, x, 0, (60, 52, 40), w, h)
        _put(px, x, h - 1, (60, 52, 40), w, h)
    for y in range(h):
        _put(px, 0, y, (60, 52, 40), w, h)
        _put(px, w - 1, y, (60, 52, 40), w, h)
    return img


def generate(assets):
    gui = os.path.join(assets, "textures/gui")
    os.makedirs(os.path.join(gui, "manual"), exist_ok=True)
    book_gui().save(os.path.join(gui, "manual.png"))
    garage_top().save(os.path.join(gui, "manual/garage_top.png"))
    garage_side().save(os.path.join(gui, "manual/garage_side.png"))
    os.makedirs(os.path.join(assets, "textures/item"), exist_ok=True)
    return {"garage_top": garage_top().size, "garage_side": garage_side().size}
