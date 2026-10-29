"""The wasteland skin a finished Garage turns into: one 128x64 sheet of 16x16 tiles, drawn over the room's
blocks by the Garage Controller renderer. Tile numbers must match GarageSkin.java."""
import os
import random

from PIL import Image

RUST = [(58, 30, 20), (84, 42, 24), (112, 56, 28), (140, 72, 34), (166, 94, 46), (190, 120, 64)]
STEEL = [(38, 38, 42), (56, 56, 60), (76, 75, 78), (98, 96, 96), (124, 120, 116), (150, 146, 138)]
BRICK = [(70, 30, 24), (96, 42, 32), (120, 54, 40), (140, 68, 50)]
WOOD = [(42, 28, 18), (60, 40, 24), (78, 54, 32), (96, 68, 42), (112, 82, 52)]
CONCRETE = [(54, 52, 50), (70, 68, 64), (86, 83, 78), (102, 99, 92), (118, 114, 106)]
OIL = (24, 22, 24)


class Tile:
    def __init__(self, img, index, seed):
        self.img = img
        self.ox = (index % 8) * 16
        self.oy = (index // 8) * 16
        self.rng = random.Random(seed)
        self.px = img.load()

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[self.ox + x, self.oy + y] = tuple(int(max(0, min(255, v))) for v in c) + (255,)

    def get(self, x, y):
        return self.px[self.ox + x, self.oy + y][:3]

    def fill_noise(self, pal, lo=1, hi=3):
        for y in range(16):
            for x in range(16):
                self.set(x, y, pal[self.rng.randint(lo, hi)])

    def rust_spots(self, n, pal=RUST):
        for _ in range(n):
            cx, cy, r = self.rng.randint(0, 15), self.rng.randint(0, 15), self.rng.uniform(1, 3)
            for y in range(16):
                for x in range(16):
                    if (x - cx) ** 2 + (y - cy) ** 2 < r * r and self.rng.random() < 0.8:
                        self.set(x, y, pal[self.rng.randint(1, 4)])

    def streaks(self, n, col, length=6):
        for _ in range(n):
            x, y = self.rng.randint(0, 15), self.rng.randint(0, 8)
            for k in range(self.rng.randint(2, length)):
                self.set(x, y + k, col)

    def darken(self, x, y, f):
        c = self.get(x, y)
        self.set(x, y, tuple(v * f for v in c))


GRIME = [(40, 34, 30), (56, 46, 38), (74, 60, 46), (94, 74, 54), (118, 90, 62), (140, 108, 74)]


def corrugated(t, pal, ribs=4):
    """Ribbed sheet metal: a bright crest, two mid tones and a dark trough per rib, weathered."""
    for y in range(16):
        for x in range(16):
            idx = (4, 3, 2, 1)[x % ribs]
            if t.rng.random() < 0.12:
                idx += t.rng.choice([-1, 1])
            t.set(x, y, pal[max(0, min(5, idx))])
    # Rust running down from the bolts at the top, grime pooling at the bottom.
    for _ in range(3):
        x = t.rng.randint(0, 15)
        for y in range(t.rng.randint(4, 12)):
            if t.rng.random() < 0.85:
                t.set(x, y, RUST[t.rng.randint(2, 4)])
    for y in range(12, 16):
        for x in range(16):
            if t.rng.random() < (y - 11) * 0.2:
                t.darken(x, y, 0.7)


def rivets(t, pts, light, dark):
    for x, y in pts:
        t.set(x, y, light)
        t.set(x + 1, y + 1, dark)


def generate(assets):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 255))

    # 0: rusty corrugated sheet (inside walls)
    t = Tile(img, 0, 1)
    corrugated(t, GRIME)
    t.rust_spots(2)
    for x in range(16):
        t.set(x, 0, RUST[0]); t.set(x, 15, RUST[0])
    rivets(t, [(1, 1), (7, 1), (13, 1), (1, 13), (7, 13), (13, 13)], RUST[5], RUST[0])

    # 1: grimy red brick
    t = Tile(img, 1, 2)
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            mortar = y % 4 == 3 or (x + off) % 8 == 7
            t.set(x, y, (44, 38, 34) if mortar else BRICK[t.rng.randint(0, 3)])
    t.streaks(3, (36, 30, 28), 7)
    t.rust_spots(1, [(30, 24, 20)] + BRICK)

    # 2: patched steel plates, welded and bolted
    t = Tile(img, 2, 3)
    t.fill_noise(STEEL, 1, 3)
    for y in range(8, 16):
        for x in range(0, 9):
            t.set(x, y, RUST[t.rng.randint(1, 3)])
    for x in range(16):
        t.set(x, 7, STEEL[0])
    for y in range(8, 16):
        t.set(9, y, STEEL[0])
    rivets(t, [(2, 2), (12, 2), (2, 10), (12, 12), (6, 10)], STEEL[5], STEEL[0])
    t.rust_spots(2)
    t.streaks(2, RUST[1], 6)

    # 3: kick plate with worn hazard stripes (bottom of inside walls)
    t = Tile(img, 3, 4)
    t.fill_noise(STEEL, 1, 2)
    for y in range(9, 15):
        for x in range(16):
            yellow = ((x + y) // 3) % 2 == 0
            c = (196, 150, 40) if yellow else (30, 28, 30)
            if t.rng.random() < 0.25:
                c = RUST[t.rng.randint(1, 3)]
            t.set(x, y, c)
    for x in range(16):
        t.set(x, 8, STEEL[0]); t.set(x, 15, STEEL[0])
    t.rust_spots(2)

    # 4: dark plank ceiling
    t = Tile(img, 4, 5)
    for y in range(16):
        for x in range(16):
            gap = y % 4 == 3
            t.set(x, y, WOOD[0] if gap else WOOD[t.rng.randint(1, 3)])
    for y in (1, 9):
        t.set(t.rng.randint(0, 15), y, WOOD[0])
    t.streaks(2, WOOD[0], 3)

    # 5: ceiling with a rusty cross-beam
    t = Tile(img, 5, 6)
    for y in range(16):
        for x in range(16):
            t.set(x, y, WOOD[0] if y % 4 == 3 else WOOD[t.rng.randint(1, 3)])
    for y in range(16):
        for x in range(6, 10):
            t.set(x, y, RUST[(x - 6) + 1])
    rivets(t, [(7, 3), (7, 11)], RUST[5], RUST[0])

    # 6: oily cracked concrete floor
    t = Tile(img, 6, 7)
    t.fill_noise(CONCRETE, 1, 3)
    cx, cy, r = t.rng.randint(4, 11), t.rng.randint(4, 11), 2.6
    for y in range(16):
        for x in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 < r * r * t.rng.uniform(0.5, 1.0):
                t.set(x, y, (46, 42, 44) if t.rng.random() < 0.6 else (60, 56, 56))
    x, y = t.rng.randint(0, 15), 0
    while y < 16:
        t.set(x, y, CONCRETE[0])
        x += t.rng.choice([-1, 0, 1]); y += 1

    # 7: steel floor grate
    t = Tile(img, 7, 8)
    for y in range(16):
        for x in range(16):
            hole = x % 4 in (1, 2) and y % 4 in (1, 2)
            t.set(x, y, (16, 14, 14) if hole else STEEL[t.rng.randint(1, 3)])
    t.rust_spots(2)

    # 8: weathered corrugated outer wall
    t = Tile(img, 8, 9)
    corrugated(t, GRIME)
    t.rust_spots(3)
    t.streaks(3, (36, 28, 24), 10)
    for x in range(16):
        t.set(x, 15, RUST[0])

    # 9: outer wall with a hazard band and bolted patch
    t = Tile(img, 9, 10)
    corrugated(t, STEEL)
    t.rust_spots(3)
    for y in range(5, 9):
        for x in range(16):
            yellow = ((x + y) // 2) % 2 == 0
            if t.rng.random() < 0.85:
                t.set(x, y, (200, 150, 40) if yellow else (30, 28, 30))
    for y in range(10, 15):
        for x in range(9, 15):
            t.set(x, y, RUST[t.rng.randint(2, 4)])
    rivets(t, [(9, 10), (13, 10), (9, 13), (13, 13)], RUST[5], RUST[0])

    # 10: rusty roof sheet
    t = Tile(img, 10, 11)
    for y in range(16):
        for x in range(16):
            t.set(x, y, RUST[(3, 2, 1, 2)[y % 4] + t.rng.choice([0, 0, 1, -1])])
    t.rust_spots(3, [(40, 22, 16)] + RUST)

    # 11: tyre tread (side) and 12: tyre (top, with a hole)
    t = Tile(img, 11, 12)
    for y in range(16):
        for x in range(16):
            tread = (x + (y // 2) % 2 * 2) % 4 < 2
            t.set(x, y, (34, 33, 34) if tread else (20, 19, 20))
    t = Tile(img, 12, 13)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.5:
                t.set(x, y, (8, 8, 8))
            elif d < 4.6:
                t.set(x, y, (90, 88, 86))
            else:
                t.set(x, y, (30 + t.rng.randint(0, 8),) * 3)

    # 13: rusty iron (beams, pipes)
    t = Tile(img, 13, 14)
    t.fill_noise(RUST, 1, 3)
    t.rust_spots(3, [(40, 22, 16)] + RUST)
    for x in range(16):
        t.set(x, 0, RUST[0]); t.set(x, 15, RUST[0])

    # 14: red jerry can
    t = Tile(img, 14, 15)
    for y in range(16):
        for x in range(16):
            t.set(x, y, (150 + t.rng.randint(-15, 15), 26, 22))
    for k in range(2, 14):
        t.set(k, k, (100, 16, 14)); t.set(15 - k, k, (100, 16, 14))
    t.rust_spots(1, [(60, 20, 14), (90, 30, 18), (110, 40, 20), (130, 50, 24), (150, 60, 30)])

    # 15: workbench top, scarred wood
    t = Tile(img, 15, 16)
    for y in range(16):
        for x in range(16):
            t.set(x, y, WOOD[t.rng.randint(2, 4)] if y % 5 else WOOD[1])
    for _ in range(5):
        x, y = t.rng.randint(0, 14), t.rng.randint(0, 14)
        t.set(x, y, WOOD[0]); t.set(x + 1, y, WOOD[0])
    t.streaks(1, OIL, 3)

    # 16: rusty sign board
    t = Tile(img, 16, 17)
    t.fill_noise(RUST, 0, 2)
    t.rust_spots(2, [(30, 16, 12)] + RUST)
    for x in range(16):
        t.set(x, 0, RUST[4]); t.set(x, 15, RUST[0])

    # 17: oil drum side (red with ribs) and 18: drum lid
    t = Tile(img, 17, 18)
    for y in range(16):
        for x in range(16):
            rib = y in (2, 3, 12, 13)
            t.set(x, y, (90, 22, 18) if rib else (128 + t.rng.randint(-12, 12), 34, 26))
    t.rust_spots(3)
    t = Tile(img, 18, 19)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            t.set(x, y, (90, 22, 18) if d > 6.5 else (120 + t.rng.randint(-10, 10), 32, 24))
    t.set(4, 4, (20, 20, 20)); t.set(5, 4, (20, 20, 20))
    t.rust_spots(2)

    # 19: dark soot-blackened steel (exhaust pipe, I-beam)
    t = Tile(img, 19, 20)
    t.fill_noise(STEEL, 0, 1)
    t.rust_spots(2)
    t.streaks(3, (16, 16, 18), 8)

    # ---- cleaner, brighter tiles for the planned layout (these replace the first drafts above) ----
    PANEL = [(62, 60, 58), (84, 80, 76), (108, 102, 94), (132, 124, 112), (156, 146, 130), (180, 170, 152)]
    WARM = [(70, 46, 32), (96, 62, 40), (124, 82, 50), (150, 102, 62), (176, 126, 80), (198, 150, 100)]

    def clear(t, col):
        for y in range(16):
            for x in range(16):
                t.set(x, y, col)

    def ribbed(t, pal, drip=2):
        for y in range(16):
            for x in range(16):
                t.set(x, y, pal[(4, 3, 3, 1)[x % 4]])
        for x in range(16):
            t.set(x, 0, pal[0]); t.set(x, 15, pal[1])
        for _ in range(drip):
            x = t.rng.randint(0, 15)
            for y in range(1, t.rng.randint(3, 8)):
                t.set(x, y, RUST[3] if y < 3 else RUST[2])
        for (x, y) in ((1, 1), (9, 1), (1, 14), (9, 14)):
            t.set(x, y, pal[5])

    # 0: clean ribbed steel sheet with a little rust running from the bolts
    t = Tile(img, 0, 101)
    ribbed(t, PANEL)

    # 2: bolted plate, a neat grid of four plates
    t = Tile(img, 2, 102)
    for y in range(16):
        for x in range(16):
            t.set(x, y, PANEL[3] if (x // 8 + y // 8) % 2 == 0 else PANEL[2])
            if t.rng.random() < 0.08:
                t.set(x, y, PANEL[t.rng.randint(2, 4)])
    for i in range(16):
        t.set(i, 7, PANEL[0]); t.set(7, i, PANEL[0]); t.set(i, 8, PANEL[4]); t.set(8, i, PANEL[4])
    rivets(t, [(2, 2), (12, 2), (2, 12), (12, 12)], PANEL[5], PANEL[0])
    t.rust_spots(1)

    # 3: kick plate: diamond-tread steel with a clean hazard band
    t = Tile(img, 3, 103)
    for y in range(16):
        for x in range(16):
            t.set(x, y, PANEL[4] if (x + y) % 4 == 0 and (x - y) % 4 == 0 else PANEL[2])
    for y in range(10, 15):
        for x in range(16):
            t.set(x, y, (222, 172, 46) if ((x + y) // 3) % 2 == 0 else (34, 32, 34))
    for x in range(16):
        t.set(x, 9, PANEL[0]); t.set(x, 15, PANEL[0]); t.set(x, 0, PANEL[5])

    # 6: smooth sealed concrete with faint slab seams
    t = Tile(img, 6, 104)
    for y in range(16):
        for x in range(16):
            t.set(x, y, CONCRETE[t.rng.choice([3, 3, 3, 4, 2])])
    for i in range(16):
        t.set(i, 15, CONCRETE[1]); t.set(15, i, CONCRETE[1])
    if t.rng.random() < 1:
        t.set(5, 6, CONCRETE[1]); t.set(6, 6, CONCRETE[1]); t.set(6, 7, CONCRETE[1])

    # 7: drain grate (gutter round the floor)
    t = Tile(img, 7, 105)
    for y in range(16):
        for x in range(16):
            hole = x % 3 != 0 and y % 8 not in (0, 7)
            t.set(x, y, (20, 18, 18) if hole else PANEL[2 + (y % 8 == 0)])

    # 8: outer wall: ribbed sheet, warm weathered paint
    t = Tile(img, 8, 106)
    ribbed(t, WARM, drip=3)

    # 9: outer base: hazard band on dark steel
    t = Tile(img, 9, 107)
    clear(t, PANEL[1])
    for y in range(3, 13):
        for x in range(16):
            t.set(x, y, (222, 172, 46) if ((x + y) // 3) % 2 == 0 else (30, 28, 30))
    for x in range(16):
        t.set(x, 2, PANEL[0]); t.set(x, 13, PANEL[0])

    # 20: steel support pillar (I-beam), inside
    t = Tile(img, 20, 108)
    for y in range(16):
        for x in range(16):
            if x < 2 or x > 13:
                c = PANEL[1]
            elif x < 4 or x > 11:
                c = PANEL[4]
            else:
                c = PANEL[2]
            t.set(x, y, c)
    for y in (3, 11):
        t.set(2, y, PANEL[5]); t.set(13, y, PANEL[5])
    t.rust_spots(1)

    # 21: rusty corner pillar, outside
    t = Tile(img, 21, 109)
    for y in range(16):
        for x in range(16):
            t.set(x, y, RUST[2] if 3 < x < 12 else RUST[1])
        t.set(3, y, RUST[4]); t.set(12, y, RUST[0])
    rivets(t, [(5, 2), (9, 2), (5, 10), (9, 10)], RUST[5], RUST[0])

    # 22: louvred vent
    t = Tile(img, 22, 110)
    clear(t, PANEL[2])
    for y in range(2, 14):
        for x in range(2, 14):
            t.set(x, y, PANEL[4] if y % 3 == 0 else (24, 22, 22) if y % 3 == 1 else PANEL[1])
    for i in range(16):
        t.set(i, 0, PANEL[0]); t.set(i, 15, PANEL[0]); t.set(0, i, PANEL[0]); t.set(15, i, PANEL[0])

    # ---- rustic pass: everything weathered back down to rust and grime ----
    RP = [(52, 34, 26), (74, 46, 30), (98, 60, 36), (122, 76, 44), (146, 96, 56), (168, 118, 72)]

    # 0: rusted ribbed sheet, flaking, with streaks and grime at the foot
    t = Tile(img, 0, 201)
    ribbed(t, RP, drip=6)
    t.rust_spots(4, [(44, 26, 18)] + RUST)
    for _ in range(4):
        x, y = t.rng.randint(0, 15), t.rng.randint(2, 13)
        t.set(x, y, PANEL[2]); t.set(x, y + 1, PANEL[1])
    for y in range(12, 16):
        for x in range(16):
            if t.rng.random() < (y - 11) * 0.22:
                t.darken(x, y, 0.65)

    # 2: bolted plates, rust bleeding from every seam
    t = Tile(img, 2, 202)
    for y in range(16):
        for x in range(16):
            t.set(x, y, RP[3] if (x // 8 + y // 8) % 2 == 0 else RP[2])
    for i in range(16):
        t.set(i, 7, RP[0]); t.set(7, i, RP[0]); t.set(i, 8, RP[4]); t.set(8, i, RP[4])
    rivets(t, [(2, 2), (12, 2), (2, 12), (12, 12)], PANEL[5], RP[0])
    t.rust_spots(3)
    t.streaks(4, RP[0], 6)

    # 6: stained concrete: oil, tyre marks and grime
    t = Tile(img, 6, 203)
    for y in range(16):
        for x in range(16):
            t.set(x, y, CONCRETE[t.rng.choice([2, 3, 3, 2, 1])])
    cx, cy = t.rng.randint(4, 11), t.rng.randint(4, 11)
    for y in range(16):
        for x in range(16):
            d = (x - cx) ** 2 + (y - cy) ** 2
            if d < 9 * t.rng.uniform(0.5, 1.0):
                t.set(x, y, (34, 30, 32))
            elif d < 16 and t.rng.random() < 0.4:
                t.set(x, y, (50, 46, 46))
    for y in range(16):
        if t.rng.random() < 0.7:
            t.set(3, y, CONCRETE[0]); t.set(12, y, CONCRETE[0])
    for i in range(16):
        t.set(i, 15, CONCRETE[0]); t.set(15, i, CONCRETE[0])

    # 20: rusted I-beam pillar
    t = Tile(img, 20, 204)
    for y in range(16):
        for x in range(16):
            c = RP[1] if x < 2 or x > 13 else RP[4] if x < 4 or x > 11 else RP[2]
            t.set(x, y, c)
    for y in (3, 11):
        t.set(2, y, PANEL[5]); t.set(13, y, PANEL[5])
    t.rust_spots(3, [(44, 26, 18)] + RUST)
    t.streaks(2, RP[0], 8)

    # 23: car body, flat black paint worn through to rust, scratched
    t = Tile(img, 23, 205)
    for y in range(16):
        for x in range(16):
            t.set(x, y, (28 + t.rng.randint(0, 8), 26 + t.rng.randint(0, 6), 26 + t.rng.randint(0, 6)))
    t.rust_spots(4)
    for _ in range(4):
        x, y = t.rng.randint(0, 12), t.rng.randint(0, 15)
        for k in range(t.rng.randint(2, 5)):
            t.set(x + k, y, (96, 92, 88))
    rivets(t, [(1, 1), (14, 1), (1, 14), (14, 14)], PANEL[4], (10, 10, 10))

    # 24: cracked, grimy windscreen
    t = Tile(img, 24, 206)
    for y in range(16):
        for x in range(16):
            t.set(x, y, (36, 48, 52) if y > 1 else (24, 22, 22))
    for k in range(3, 12):
        t.set(k, 14 - k, (120, 140, 140))
    x, y = 8, 7
    for _ in range(6):
        t.set(x, y, (150, 170, 170)); x += t.rng.choice([-1, 1]); y += t.rng.choice([-1, 0, 1])
    for x in range(16):
        t.set(x, 15, (60, 44, 34))

    # 25: gear: dark iron, worn bright on the edges
    t = Tile(img, 25, 207)
    for y in range(16):
        for x in range(16):
            edge = x in (0, 15) or y in (0, 15)
            t.set(x, y, PANEL[4] if edge else RP[t.rng.randint(1, 2)])
    t.rust_spots(1)

    # 26: pressure gauge face
    t = Tile(img, 26, 208)
    for y in range(16):
        for x in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            t.set(x, y, (40, 34, 30) if d > 7 else (60, 56, 50) if d > 6.2 else (222, 214, 190))
    for a in range(0, 360, 30):
        import math
        x = int(round(7.5 + math.cos(math.radians(a)) * 5)); y = int(round(7.5 + math.sin(math.radians(a)) * 5))
        t.set(x, y, (30, 30, 30))
    for (x, y) in ((11, 3), (12, 4), (12, 5)):
        t.set(x, y, (200, 40, 30))

    # 27: dirty chrome
    t = Tile(img, 27, 209)
    for y in range(16):
        for x in range(16):
            t.set(x, y, PANEL[5] if (x + y) % 7 == 0 else PANEL[4] if x % 4 < 2 else PANEL[3])
    t.rust_spots(1)
    t.streaks(2, (40, 34, 30), 5)

    # 28: radiator grille
    t = Tile(img, 28, 210)
    for y in range(16):
        for x in range(16):
            t.set(x, y, (20, 18, 18) if x % 3 != 0 else PANEL[4])
    for x in range(16):
        t.set(x, 0, PANEL[5]); t.set(x, 15, PANEL[1])

    path = os.path.join(assets, "textures/entity/garage_skin.png")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    img.save(path)
