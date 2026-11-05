"""
Extra tiles in garage_skin.png (rows 4+) for the rig variants and the wind turbine.

  29 red paint      30 red paint with a white racing stripe   31 olive drab with camo
  32 track side     33 track plates                          34 faded teal hull with a waterline
  35 rubber skirt   36 turbine blade                         37 nitro bottle
  38 headlight      39 chrome hubcap                         40 tail light
  41 wire mesh (see-through)                                 42 olive armour plate with rivets
"""
import math


def tiles(img, Tile, rivets, PANEL, RUST):
    def paint(t, base, hi, lo, chips=3, seed_spots=True):
        for y in range(16):
            for x in range(16):
                c = base
                if y < 2:
                    c = hi
                elif y > 13:
                    c = lo
                n = t.rng.randint(-6, 6)
                t.set(x, y, tuple(v + n for v in c))
        for _ in range(chips):
            x, y = t.rng.randint(1, 14), t.rng.randint(2, 13)
            t.set(x, y, PANEL[4]); t.set(x + 1, y, RUST[2])
        if seed_spots:
            t.rust_spots(1)

    RED, RED_HI, RED_LO = (176, 30, 26), (220, 70, 56), (110, 18, 16)

    # 29: glossy red paint, a little chipped
    t = Tile(img, 29, 301)
    paint(t, RED, RED_HI, RED_LO, chips=2, seed_spots=False)
    for x in range(3, 13):
        t.set(x, 3, (240, 120, 104))

    # 30: red with twin white racing stripes
    t = Tile(img, 30, 302)
    paint(t, RED, RED_HI, RED_LO, chips=1, seed_spots=False)
    for y in range(16):
        for x in (4, 5, 6, 9, 10, 11):
            t.set(x, y, (236, 232, 222) if t.rng.random() > 0.06 else (180, 170, 160))

    # 31: olive drab with darker camo blotches
    t = Tile(img, 31, 303)
    OL = (88, 96, 52)
    paint(t, OL, (112, 120, 70), (60, 66, 36), chips=2, seed_spots=False)
    for (cx, cy, r, col) in ((4, 4, 3.2, (60, 58, 34)), (12, 10, 3.6, (60, 58, 34)), (11, 2, 2.2, (110, 92, 58)),
                             (3, 12, 2.6, (110, 92, 58))):
        for y in range(16):
            for x in range(16):
                if (x - cx) ** 2 + ((y - cy) * 1.3) ** 2 < r * r:
                    t.set(x, y, tuple(v + t.rng.randint(-4, 4) for v in col))

    # 32: track side: steel links with drive teeth
    t = Tile(img, 32, 304)
    for y in range(16):
        for x in range(16):
            link = x % 4
            c = (34, 32, 32) if link == 0 else (64, 62, 60) if link == 1 else (52, 50, 48)
            if y in (0, 15):
                c = (24, 22, 22)
            elif y in (1, 14):
                c = (90, 86, 82) if link != 0 else (40, 38, 36)
            t.set(x, y, c)
    for x in range(1, 16, 4):
        t.set(x, 7, (120, 116, 110)); t.set(x, 8, (100, 96, 92))
    t.rust_spots(1)

    # 33: track plates seen from above: rubber pads and steel grousers
    t = Tile(img, 33, 305)
    for y in range(16):
        for x in range(16):
            c = (30, 28, 28) if y % 4 == 0 else (58, 56, 54) if y % 4 == 1 else (44, 42, 40)
            if x in (0, 15):
                c = (24, 22, 22)
            t.set(x, y, c)

    # 34: faded teal hull, rust streaks, a white waterline
    t = Tile(img, 34, 306)
    paint(t, (46, 128, 130), (84, 168, 166), (30, 90, 92), chips=3)
    for x in range(16):
        t.set(x, 11, (226, 222, 210)); t.set(x, 12, (196, 70, 50))
        t.set(x, 13, (150, 56, 40)); t.set(x, 14, (120, 44, 32)); t.set(x, 15, (80, 30, 24))
    t.streaks(3, RUST[2], 6)

    # 35: black rubber skirt with folds
    t = Tile(img, 35, 307)
    for y in range(16):
        for x in range(16):
            fold = (x + (y // 4)) % 5
            t.set(x, y, (26, 26, 28) if fold == 0 else (44, 44, 46) if fold in (1, 4) else (36, 36, 38))
    for x in range(16):
        t.set(x, 0, (60, 58, 56))

    # 36: turbine blade: galvanized sheet, rusty edges and a seam
    t = Tile(img, 36, 308)
    for y in range(16):
        for x in range(16):
            base = (150, 150, 146) if (x + y // 3) % 6 else (174, 174, 168)
            t.set(x, y, tuple(v + t.rng.randint(-8, 8) for v in base))
    for i in range(16):
        t.set(0, i, RUST[2]); t.set(15, i, RUST[3]); t.set(i, 8, (120, 120, 116))
    t.rust_spots(2)

    # 37: nitro bottle: blue with a white label
    t = Tile(img, 37, 309)
    for y in range(16):
        for x in range(16):
            c = (40, 92, 200) if 2 < x < 13 else (28, 64, 150)
            if x in (3, 4):
                c = (90, 150, 240)
            t.set(x, y, c)
    for y in range(6, 11):
        for x in range(2, 14):
            t.set(x, y, (236, 236, 236))
    for x in range(4, 12):
        t.set(x, 8, (200, 40, 40))

    # 38: headlight lens
    t = Tile(img, 38, 310)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = (70, 66, 60) if d > 7 else (255, 248, 210) if d < 3 else (240, 224, 160) if d < 5.5 else (200, 180, 120)
            t.set(x, y, c)
    t.set(5, 5, (255, 255, 255)); t.set(6, 5, (255, 255, 255))

    # 39: chrome hubcap with five bolts
    t = Tile(img, 39, 311)
    for y in range(16):
        for x in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            c = (22, 22, 22) if d > 7.2 else (120, 118, 112) if d > 6 else (200, 198, 190) if d > 2.5 else (150, 148, 140)
            if 2.5 < d < 6 and (x + y) % 5 == 0:
                c = (230, 230, 224)
            t.set(x, y, c)
    for a in range(0, 360, 72):
        x = int(round(7.5 + math.cos(math.radians(a)) * 4)); y = int(round(7.5 + math.sin(math.radians(a)) * 4))
        t.set(x, y, (60, 58, 56))

    # 40: tail light
    t = Tile(img, 40, 312)
    for y in range(16):
        for x in range(16):
            c = (230, 40, 30) if 1 < x < 14 and 1 < y < 14 else (60, 20, 18)
            if (x + y) % 4 == 0 and c[0] > 200:
                c = (255, 90, 70)
            t.set(x, y, c)

    # 41: wire mesh, see-through between the wires
    t = Tile(img, 41, 313)
    for y in range(16):
        for x in range(16):
            if x % 4 == 0 or y % 4 == 0:
                t.set(x, y, (110, 108, 104) if (x + y) % 8 else (150, 148, 142))
            else:
                t.px[t.ox + x, t.oy + y] = (0, 0, 0, 0)

    # 42: olive armour plate, riveted
    t = Tile(img, 42, 314)
    paint(t, (80, 88, 48), (104, 112, 64), (56, 62, 34), chips=3)
    for i in range(16):
        t.set(i, 0, (40, 44, 24)); t.set(i, 15, (40, 44, 24)); t.set(0, i, (104, 112, 64)); t.set(15, i, (48, 52, 28))
    rivets(t, [(2, 2), (7, 2), (12, 2), (2, 12), (7, 12), (12, 12)], (140, 146, 100), (30, 32, 18))
