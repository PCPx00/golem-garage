"""
16x16 item icons. Each shape has a drawing function that paints with a material palette,
so one drawing gives an icon for every material.

Palette indices: 0 outline, 1 deep shadow, 2 shadow, 3 base, 4 light, 5 highlight, 6 specular.
"""
from PIL import Image

import paint

EYE = (255, 74, 54)
EYE_HI = (255, 196, 160)
CYAN = (110, 232, 255)
CYAN_HI = (220, 252, 255)
FIRE = (255, 136, 38)
FIRE_HI = (255, 226, 110)
LANTERN = (255, 214, 120)
LANTERN_HI = (255, 246, 210)
BLACK = (24, 22, 26)


class Icon:
    def __init__(self, pal):
        self.img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        self.px = self.img.load()
        self.pal = pal

    def c(self, col):
        return self.pal[col] if isinstance(col, int) else col

    def p(self, x, y, col):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[x, y] = tuple(self.c(col)[:3]) + (255,)

    def rect(self, x0, y0, x1, y1, col):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.p(x, y, col)

    def block(self, x0, y0, x1, y1, base=3, round_corners=False):
        """Outlined, bevelled block. Inclusive coordinates; the outline is the border."""
        self.rect(x0, y0, x1, y1, 0)
        if x1 - x0 < 2 or y1 - y0 < 2:
            return
        self.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, base)
        for x in range(x0 + 1, x1):
            self.p(x, y0 + 1, min(6, base + 2))
            self.p(x, y1 - 1, max(1, base - 1))
        for y in range(y0 + 1, y1):
            self.p(x0 + 1, y, min(6, base + 1))
            self.p(x1 - 1, y, max(1, base - 1))
        self.p(x0 + 1, y0 + 1, min(6, base + 3))
        if round_corners:
            for (x, y) in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
                self.px[x, y] = (0, 0, 0, 0)

    def hline(self, y, x0, x1, col):
        for x in range(x0, x1 + 1):
            self.p(x, y, col)

    def vline(self, x, y0, y1, col):
        for y in range(y0, y1 + 1):
            self.p(x, y, col)

    def line(self, a, b, col):
        (x0, y0), (x1, y1) = a, b
        n = max(abs(x1 - x0), abs(y1 - y0), 1)
        for i in range(n + 1):
            self.p(round(x0 + (x1 - x0) * i / n), round(y0 + (y1 - y0) * i / n), col)


# ------------------------------------------------------------------ heads
def _face(ic, top, eyes_y, left_eye=True):
    ic.hline(eyes_y - 1, 3, 12, 2)
    if left_eye:
        ic.p(4, eyes_y, EYE); ic.p(5, eyes_y, EYE_HI)
    ic.p(10, eyes_y, EYE_HI); ic.p(11, eyes_y, EYE)
    ic.rect(7, eyes_y + 1, 8, eyes_y + 4, 4)
    ic.vline(8, eyes_y + 1, eyes_y + 4, 3)
    ic.p(7, eyes_y + 4, 2); ic.p(8, eyes_y + 4, 1)


def sentry_head(ic):
    ic.block(2, 2, 13, 14, round_corners=True)
    ic.hline(4, 3, 12, 5)
    _face(ic, 2, 7)
    ic.hline(13, 4, 11, 1)
    for x in range(4, 12, 2):
        ic.p(x, 13, 0)
    ic.p(1, 9, 1); ic.p(1, 10, 2); ic.p(14, 9, 1); ic.p(14, 10, 2)


def scope_head(ic):
    ic.vline(11, 0, 2, 1); ic.p(11, 0, CYAN_HI); ic.p(12, 0, CYAN)
    ic.block(2, 3, 13, 15, round_corners=True)
    _face(ic, 3, 8, left_eye=False)
    # Scope ring over the right eye.
    ic.rect(1, 6, 6, 11, 0)
    ic.rect(2, 7, 5, 10, 1)
    ic.rect(3, 8, 4, 9, CYAN)
    ic.p(3, 8, CYAN_HI)
    ic.hline(6, 1, 6, 4)


def lantern_head(ic):
    ic.hline(0, 6, 9, 1)
    ic.block(5, 1, 10, 7)
    ic.rect(6, 2, 9, 6, LANTERN)
    ic.p(6, 2, LANTERN_HI); ic.p(7, 3, LANTERN_HI)
    ic.vline(5, 1, 7, 1); ic.vline(10, 1, 7, 1)
    ic.hline(1, 5, 10, 4); ic.hline(7, 4, 11, 3)
    ic.block(3, 8, 12, 15, round_corners=True)
    ic.p(5, 11, EYE); ic.p(10, 11, EYE)
    ic.rect(7, 12, 8, 14, 4)


def visor_head(ic):
    ic.rect(7, 0, 8, 2, 5)
    ic.block(2, 2, 13, 14, round_corners=True)
    ic.hline(4, 3, 12, 5)
    ic.rect(3, 6, 12, 9, 0)
    ic.hline(7, 4, 11, CYAN); ic.hline(8, 4, 11, CYAN)
    ic.p(5, 7, CYAN_HI); ic.p(6, 7, CYAN_HI)
    ic.p(3, 11, 6); ic.p(12, 11, 6)
    ic.hline(13, 5, 10, 1)


# ------------------------------------------------------------------ torsos
def _core_window(ic, cx, cy):
    ic.rect(cx - 1, cy - 1, cx + 2, cy + 2, 0)
    ic.rect(cx, cy, cx + 1, cy + 1, EYE)
    ic.p(cx, cy, EYE_HI)


def plated_chest(ic):
    ic.block(0, 1, 4, 5); ic.block(11, 1, 15, 5)
    ic.block(1, 3, 14, 12)
    ic.vline(7, 4, 11, 2)
    ic.hline(9, 2, 13, 2)
    for (x, y) in ((3, 5), (12, 5), (3, 10), (12, 10)):
        ic.p(x, y, 6)
    _core_window(ic, 7, 6)
    ic.block(4, 12, 11, 15, base=2)


def cargo_barrel(ic):
    ic.block(2, 0, 13, 15, round_corners=True)
    for x in (5, 8, 11):
        ic.vline(x, 1, 14, 2)
    ic.hline(3, 2, 13, 0); ic.hline(4, 3, 12, 1)
    ic.hline(11, 2, 13, 0); ic.hline(12, 3, 12, 1)
    _core_window(ic, 7, 7)


def sealed_hold(ic):
    ic.vline(1, 2, 13, 1); ic.vline(14, 2, 13, 1)
    ic.block(2, 1, 13, 14)
    ic.hline(2, 3, 12, 5); ic.hline(13, 3, 12, 2)
    ic.rect(4, 4, 11, 11, 0)
    ic.rect(5, 5, 10, 10, 4)
    ic.rect(6, 6, 9, 9, (120, 200, 220))
    ic.p(6, 6, CYAN_HI); ic.p(7, 6, (200, 240, 250))


def furnace_chest(ic):
    ic.block(11, 0, 13, 3, base=2)
    ic.block(1, 2, 14, 14)
    for y in (5, 8, 11):
        ic.hline(y, 2, 13, 2)
    for y, off in ((3, 0), (6, 2), (9, 0), (12, 2)):
        for x in range(3 + off, 13, 4):
            ic.p(x, y, 2)
            ic.p(x, y + 1, 2)
    ic.rect(4, 7, 11, 12, 0)
    ic.rect(5, 10, 10, 11, FIRE)
    ic.rect(6, 9, 9, 9, FIRE_HI)
    ic.p(7, 8, FIRE)


# ------------------------------------------------------------------ arms
def _shoulder(ic, x0, x1):
    ic.block(x0, 0, x1, 3, base=4)


def golem_fists(ic):
    ic.block(4, 0, 11, 3, base=4)
    ic.block(5, 3, 10, 8)
    ic.hline(8, 4, 11, 1)
    ic.block(5, 8, 10, 11)
    ic.block(2, 10, 13, 15)
    for x in (5, 8, 11):
        ic.vline(x, 11, 14, 2)
        ic.p(x - 1, 11, 6)


def grabber_claws(ic):
    ic.block(4, 0, 11, 3, base=4)
    ic.block(5, 3, 10, 10)
    ic.rect(4, 10, 11, 11, 1)
    for x, bend in ((3, -1), (7, 0), (11, 1)):
        ic.rect(x, 12, x + 1, 14, 4)
        ic.vline(x + 1, 12, 14, 2)
        ic.p(x + bend, 15, 5)
        ic.p(x + bend + 1, 15, 3)


def hammer_arms(ic):
    ic.block(4, 0, 11, 3, base=4)
    ic.block(5, 3, 10, 8)
    ic.rect(7, 8, 8, 9, 1)
    ic.block(0, 9, 15, 15)
    ic.vline(2, 10, 14, 2); ic.vline(13, 10, 14, 2)
    ic.vline(7, 10, 14, 5); ic.vline(8, 10, 14, 5)


def scythe_arms(ic):
    ic.block(9, 0, 14, 3, base=4)
    ic.block(10, 3, 13, 9)
    ic.rect(9, 10, 14, 11, 1)
    # Curved blade sweeping left.
    pts = [(9, 12), (8, 12), (7, 12), (6, 12), (5, 12), (4, 12), (3, 11), (2, 11), (1, 10), (0, 9)]
    for (x, y) in pts:
        ic.p(x, y, 4)
        ic.p(x, y + 1, 6)
        ic.p(x, y - 1, 2)
    ic.p(0, 8, 1)


# ------------------------------------------------------------------ legs
def stub_legs(ic):
    ic.block(1, 1, 6, 9); ic.block(9, 1, 14, 9)
    ic.hline(7, 2, 5, 1); ic.hline(7, 10, 13, 1)
    ic.block(0, 9, 7, 14); ic.block(8, 9, 15, 14)
    ic.vline(3, 11, 13, 2); ic.vline(12, 11, 13, 2)


def strider_legs(ic):
    ic.block(3, 0, 12, 2, base=2)
    for x in (4, 9):
        ic.block(x, 2, x + 3, 6)
        ic.rect(x, 7, x + 3, 7, 5)
        ic.block(x + 1, 8, x + 2, 12)
    ic.block(2, 13, 7, 15, base=4)
    ic.block(8, 13, 13, 15, base=4)


def tread_legs(ic):
    ic.rect(7, 0, 8, 3, 2)
    ic.hline(3, 1, 14, 4)
    ic.rect(0, 5, 15, 13, paint.RUBBER[0])
    ic.rect(1, 6, 14, 12, paint.RUBBER[2])
    for x in range(1, 15, 2):
        ic.p(x, 5, paint.RUBBER[3]); ic.p(x, 13, paint.RUBBER[3])
    for cx in (3, 7, 12):
        ic.rect(cx - 1, 8, cx + 1, 10, 3)
        ic.p(cx, 9, 6)
    ic.p(0, 5, (0, 0, 0)); ic.px[0, 5] = (0, 0, 0, 0); ic.px[15, 5] = (0, 0, 0, 0)
    ic.px[0, 13] = (0, 0, 0, 0); ic.px[15, 13] = (0, 0, 0, 0)


def wheel_legs(ic):
    ic.rect(7, 0, 8, 4, 3); ic.vline(8, 0, 4, 2)
    r = paint.RUBBER
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 9.5
            d = (dx * dx + dy * dy) ** 0.5
            if d <= 6.4:
                ic.p(x, y, r[0] if d > 5.6 else r[2])
    for a in range(8):
        import math
        ang = a * math.pi / 4
        ic.p(round(7.5 + 5 * math.cos(ang)), round(9.5 + 5 * math.sin(ang)), r[4])
    ic.rect(5, 7, 10, 12, 0)
    ic.rect(6, 8, 9, 11, 3)
    ic.hline(9, 6, 9, 2); ic.vline(7, 8, 11, 2)
    ic.p(6, 8, 6)


def spider_legs(ic):
    for side in (-1, 1):
        cx = 7.5
        for (ex, ey, fx, fy) in ((5.5, 2, 6.5, 14), (3.5, 4, 4.5, 15)):
            kx = cx + side * ex
            ax = cx + side * 1
            ic.line((round(ax), 7), (round(kx), ey), 4)
            ic.line((round(kx), ey), (round(cx + side * (ex + 1)), fy), 3)
            ic.p(round(kx), ey, 6)
    ic.block(5, 5, 10, 10, base=2)
    ic.p(6, 7, EYE); ic.p(9, 7, EYE)


# ------------------------------------------------------------------ cores
def redstone_core(ic):
    ic.rect(5, 0, 6, 1, 1); ic.rect(9, 0, 10, 1, 1)
    ic.block(3, 2, 12, 15)
    ic.hline(5, 4, 11, EYE); ic.hline(12, 4, 11, EYE)
    ic.rect(7, 7, 8, 10, 5); ic.rect(6, 8, 9, 9, 5)


def furnace_core(ic):
    ic.block(10, 0, 12, 4, base=2)
    ic.block(2, 4, 13, 15)
    for y in (7, 10, 13):
        ic.hline(y, 3, 12, 2)
    ic.rect(5, 8, 10, 13, 0)
    ic.rect(6, 11, 9, 12, FIRE)
    ic.rect(7, 10, 8, 10, FIRE_HI)


def lightning_core(ic):
    ic.p(7, 0, CYAN_HI); ic.p(8, 0, CYAN)
    ic.vline(7, 1, 7, 4); ic.vline(8, 1, 7, 2)
    ic.block(3, 7, 12, 15)
    for y in (9, 11, 13):
        ic.hline(y, 4, 11, 5)
    ic.rect(1, 9, 2, 12, CYAN); ic.rect(13, 9, 14, 12, CYAN)


def solar_core(ic):
    for row in range(8):
        y = 1 + row
        x0 = 1 + row // 2
        ic.hline(y, x0, x0 + 12, 0)
        if 0 < row < 7:
            for x in range(x0 + 1, x0 + 12):
                cell = (x - x0) % 4 == 0 or row % 3 == 0
                ic.p(x, y, (128, 146, 180) if cell else paint.SOLAR[3])
    ic.p(4, 3, paint.SOLAR[6])
    ic.rect(7, 9, 8, 13, 2)
    ic.block(4, 13, 11, 15)


def soul_core(ic):
    ic.rect(7, 0, 8, 1, 1)
    ic.hline(2, 3, 12, 4); ic.hline(13, 3, 12, 4)
    ic.vline(4, 3, 12, 1); ic.vline(11, 3, 12, 1)
    ic.rect(5, 4, 10, 12, (24, 60, 70))
    ic.rect(6, 6, 9, 12, paint.SOUL[3])
    ic.rect(7, 5, 8, 11, paint.SOUL[5])
    ic.p(7, 4, paint.SOUL[3])
    ic.hline(1, 4, 11, 0); ic.hline(14, 4, 11, 0)


DRAW = {f.__name__: f for f in [
    sentry_head, scope_head, lantern_head, visor_head,
    plated_chest, cargo_barrel, sealed_hold, furnace_chest,
    golem_fists, grabber_claws, hammer_arms, scythe_arms,
    stub_legs, strider_legs, tread_legs, wheel_legs, spider_legs,
    redstone_core, furnace_core, lightning_core, solar_core, soul_core,
]}


def icon(shape_id, base_hex):
    ic = Icon(paint.ramp(base_hex))
    DRAW[shape_id](ic)
    return ic.img


# ------------------------------------------------------------------ other items
def golem_item(base_hex, broken=False, glow=EYE):
    ic = Icon(paint.ramp(base_hex))
    ic.block(5, 0, 10, 4, round_corners=True)
    if not broken:
        ic.p(6, 2, glow); ic.p(9, 2, glow)
    else:
        ic.p(6, 2, 1); ic.p(9, 2, 1)
    ic.block(3, 4, 12, 9)
    ic.block(1, 4, 3, 11)
    ic.block(12, 4, 14, 11)
    ic.block(4, 9, 7, 15)
    ic.block(8, 9, 11, 15)
    ic.rect(7, 6, 8, 7, glow if not broken else 1)
    if broken:
        for (x, y) in ((4, 5), (5, 6), (5, 7), (10, 10), (9, 11), (2, 8), (13, 6)):
            ic.p(x, y, (30, 26, 26))
    return ic.img


def cub_item(base_hex):
    ic = Icon(paint.ramp(base_hex))
    ic.p(7, 0, (84, 170, 70)); ic.p(8, 1, (84, 170, 70)); ic.p(7, 1, (60, 130, 50))
    ic.block(2, 2, 13, 11, round_corners=True)
    ic.rect(4, 5, 5, 7, BLACK); ic.p(4, 5, (255, 255, 255))
    ic.rect(10, 5, 11, 7, BLACK); ic.p(10, 5, (255, 255, 255))
    ic.p(3, 8, (240, 120, 150)); ic.p(12, 8, (240, 120, 150))
    ic.rect(7, 8, 8, 8, 1)
    ic.block(4, 11, 11, 15)
    ic.p(5, 15, 0)
    return ic.img


def linker():
    ic = Icon(paint.ramp("#8e959c"))
    ic.line((2, 13), (10, 5), 3)
    ic.line((3, 13), (11, 5), 2)
    ic.line((2, 12), (10, 4), 5)
    ic.rect(10, 1, 14, 5, 0)
    ic.rect(11, 2, 13, 4, CYAN)
    ic.p(11, 2, CYAN_HI)
    ic.rect(1, 13, 3, 15, 1)
    return ic.img
