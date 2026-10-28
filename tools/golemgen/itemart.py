"""
All item icons at 32x32.

Parts, golems and cubs are 3D renders of the real models (iconrender.py) in each material.
Tools and materials (steel ingot, repair kit, blueprint, linker, manual) are drawn here by hand.
Runs last in gen.py, after every entity texture exists.
"""
import math
import os

import numpy as np
from PIL import Image, ImageDraw

import cub as C
import iconrender as R
import materials as M
import shapes as S
from items import CUB_COLORS

SIZE = 32


def _hex(h):
    return tuple(int(h[i:i + 2], 16) for i in (1, 3, 5))


# ================================================================== 3D renders
def generate_renders(gen, assets):
    tex_dir = os.path.join(assets, "textures/entity/golem")
    groups = gen.build_tree()
    gen.pack(groups)
    glow = Image.open(os.path.join(tex_dir, "glow.png"))
    cracks = Image.open(os.path.join(tex_dir, "cracks_3.png"))
    atlases = {m["id"]: Image.open(os.path.join(tex_dir, m["id"] + ".png")) for m in M.MATERIALS}

    # Parts: one render per shape, shaded per material. Arms show one arm, bigger.
    for sh in S.SHAPES:
        sid = sh["id"]
        only = {"right_arm"} if sh["slot"] == "ARMS" else None
        polys = R.shape_polys(groups, {sid}, gen.uv_of, {}, only_groups=only)
        gb = R.GBuffer(polys, spin=-38 if sh["slot"] == "ARMS" else 0, margin=1.0 if sh["slot"] == "ARMS" else 1.5)
        glow_rgb = _hex(sh.get("glow") or "#ff3b2a")
        for mat in M.CRAFTABLE:
            img = R.shade(gb, atlases[mat["id"]], glow, glow_rgb, flat_tags=(1,))
            img.save(os.path.join(assets, "textures/item/part/%s/%s.png" % (sid, mat["id"])))

    # Golems: the standard five-part golem in each material, and broken.
    std = {"sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core"}
    frame = {f["id"] for f in S.FRAMES}
    gb_golem = R.GBuffer(R.shape_polys(groups, std, gen.uv_of, {}), margin=1.0)
    gb_frame = R.GBuffer(R.shape_polys(groups, frame, gen.uv_of, {}), margin=1.0)
    out = os.path.join(assets, "textures/item/golem")
    os.makedirs(out, exist_ok=True)
    for mat in M.MATERIALS:
        gb = gb_frame if mat["id"] == "frame" else gb_golem
        R.shade(gb, atlases[mat["id"]], glow, (255, 59, 42), flat_tags=(1,)).save(
            os.path.join(out, mat["id"] + ".png"))
        R.shade(gb, atlases[mat["id"]], None, None, overlay=cracks, darken=0.62).save(
            os.path.join(out, mat["id"] + "_broken.png"))

    # Cubs: the chibi model in all 16 colors.
    boxes = []
    for p in C.PARTS:
        C.all_boxes(p, boxes)
    C.pack(boxes)
    polys = []
    for p in C.PARTS:
        R.collect_part(p, R.Xf(), lambda b: b["uv"], polys, lambda b: 0)
    gb_cub = R.GBuffer(polys, margin=1.0)
    cub_dir = os.path.join(assets, "textures/entity/cub")
    cub_glow = Image.open(os.path.join(cub_dir, "glow.png"))
    out = os.path.join(assets, "textures/item/cub")
    os.makedirs(out, exist_ok=True)
    for name, _ in CUB_COLORS:
        atlas = Image.open(os.path.join(cub_dir, name + ".png"))
        R.shade(gb_cub, atlas, cub_glow, (255, 120, 150)).save(os.path.join(out, name + ".png"))


# ================================================================== hand-drawn
class Canvas:
    def __init__(self):
        self.img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
        self.px = self.img.load()

    def poly(self, pts, col):
        self.d.polygon(pts, fill=tuple(col) + (255,))

    def rect(self, x0, y0, x1, y1, col):
        self.d.rectangle([x0, y0, x1, y1], fill=tuple(col) + (255,))

    def line(self, a, b, col, w=1):
        self.d.line([a, b], fill=tuple(col) + (255,), width=w)

    def p(self, x, y, col, a=255):
        if 0 <= x < SIZE and 0 <= y < SIZE:
            self.px[x, y] = tuple(col) + (a,)

    def get(self, x, y):
        return self.px[x, y]

    def finish(self):
        arr = np.asarray(self.img, dtype=float)
        return Image.fromarray(np.clip(R._outline(arr), 0, 255).astype(np.uint8), "RGBA")


def shade(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def ingot(base):
    """A tapered ingot in 3D: drawn from real corner points, lit from the upper left."""
    c = Canvas()
    # Ingot space: x along the bar, y up, z toward the viewer. The bottom is wider than the top.
    w, d, h = 12.0, 7.0, 4.2
    bot = [(-w / 2, 0, -d / 2), (w / 2, 0, -d / 2), (w / 2, 0, d / 2), (-w / 2, 0, d / 2)]
    tw, td = w / 2 - 1.5, d / 2 - 1.2
    top = [(-tw, h, -td), (tw, h, -td), (tw, h, td), (-tw, h, td)]
    yaw = math.radians(30)
    pitch = math.radians(40)

    def proj(p):
        x, y, z = p
        x1 = x * math.cos(yaw) + z * math.sin(yaw)
        z1 = -x * math.sin(yaw) + z * math.cos(yaw)   # toward the viewer
        sy = -y * math.cos(pitch) + z1 * math.sin(pitch)
        return (16 + x1 * 1.9, 17 + sy * 1.9)

    # Visible faces: front (+z), left end (-x), top.
    c.poly([proj(p) for p in (bot[3], bot[2], top[2], top[3])], shade(base, 0.86))   # front
    c.poly([proj(p) for p in (bot[0], bot[3], top[3], top[0])], shade(base, 1.04))   # left end
    c.poly([proj(p) for p in top], shade(base, 1.24))                                  # top
    t = [proj(p) for p in top]
    f = [proj(p) for p in (bot[3], bot[2])]
    c.line(t[3], t[2], mix(shade(base, 1.24), (255, 255, 255), 0.55))   # front top edge
    c.line(t[0], t[3], mix(shade(base, 1.24), (255, 255, 255), 0.3))
    c.line(f[0], f[1], shade(base, 0.6))
    a1, a2 = proj((-tw + 1.5, h, 0)), proj((tw - 2.5, h, 0))
    c.line(a1, a2, mix(shade(base, 1.24), (255, 255, 255), 0.8))
    return c.finish()


def repair_kit():
    """A red toolbox with a steel handle and latch, a wrench and a slime-ball seal."""
    c = Canvas()
    red, red_hi, red_lo = (200, 52, 48), (236, 96, 82), (130, 30, 34)
    steel, steel_hi, steel_lo = (170, 176, 186), (226, 230, 236), (98, 104, 116)
    # Handle.
    c.rect(11, 6, 21, 7, steel)
    c.rect(11, 6, 12, 11, steel)
    c.rect(20, 6, 21, 11, steel_lo)
    c.line((12, 6), (19, 6), steel_hi)
    # Lid and body.
    c.rect(4, 11, 28, 15, red_hi)
    c.rect(4, 16, 28, 27, red)
    c.rect(4, 15, 28, 15, red_lo)
    c.rect(25, 16, 28, 27, red_lo)
    c.line((4, 11), (28, 11), mix(red_hi, (255, 255, 255), 0.35))
    # Latch.
    c.rect(14, 14, 18, 18, steel)
    c.rect(15, 15, 17, 17, steel_lo)
    c.p(14, 14, steel_hi)
    # White cross badge.
    c.rect(8, 19, 12, 25, (240, 236, 228))
    c.rect(6, 21, 14, 23, (240, 236, 228))
    # Wrench across the front corner.
    for i in range(9):
        c.p(19 + i, 26 - i, steel)
        c.p(20 + i, 26 - i, steel_lo)
    c.rect(26, 15, 29, 18, steel)
    c.p(27, 16, (0, 0, 0), 0)
    c.p(28, 15, (0, 0, 0), 0)
    c.p(26, 15, steel_hi)
    # Slime seal.
    c.rect(21, 20, 23, 22, (120, 200, 90))
    c.p(21, 20, (190, 245, 160))
    return c.finish()


def blueprint():
    """A blueprint sheet with a curled top roll and a golem drawn in white lines."""
    c = Canvas()
    blue, blue_hi, blue_lo = (40, 96, 180), (70, 132, 214), (22, 58, 120)
    line = (214, 234, 255)
    faint = (90, 140, 210)
    c.rect(5, 7, 26, 28, blue)
    c.rect(26, 8, 27, 28, blue_lo)
    c.rect(5, 28, 27, 28, blue_lo)
    for x in range(7, 26, 4):
        c.line((x, 9), (x, 27), faint)
    for y in range(10, 28, 4):
        c.line((6, y), (25, y), faint)
    # Roll at the top.
    c.rect(3, 4, 28, 7, blue_hi)
    c.line((3, 4), (28, 4), mix(blue_hi, (255, 255, 255), 0.4))
    c.line((3, 7), (28, 7), blue_lo)
    c.rect(2, 5, 3, 6, blue_lo)
    c.rect(28, 5, 29, 6, blue_lo)
    # Golem schematic.
    c.d.rectangle([13, 10, 18, 14], outline=line + (255,))
    c.d.rectangle([11, 15, 20, 21], outline=line + (255,))
    c.d.rectangle([8, 15, 10, 23], outline=line + (255,))
    c.d.rectangle([21, 15, 23, 23], outline=line + (255,))
    c.d.rectangle([12, 22, 14, 26], outline=line + (255,))
    c.d.rectangle([17, 22, 19, 26], outline=line + (255,))
    c.p(14, 12, (255, 120, 100))
    c.p(17, 12, (255, 120, 100))
    c.p(15, 18, (120, 230, 255))
    c.p(16, 18, (120, 230, 255))
    return c.finish()


def linker():
    """A steel wand with an amethyst crystal tip that glows with linking light."""
    c = Canvas()
    steel, steel_hi, steel_lo = (150, 158, 170), (220, 226, 234), (78, 84, 98)
    grip, grip_lo = (92, 62, 40), (60, 38, 24)
    # Shaft from bottom-left to the crystal.
    for i in range(17):
        x, y = 5 + i, 27 - i
        c.p(x, y, steel)
        c.p(x + 1, y, steel_lo)
        c.p(x, y - 1, steel_hi)
    # Grip wrap.
    for i in range(6):
        x, y = 5 + i, 27 - i
        c.p(x, y, grip)
        c.p(x + 1, y, grip_lo)
        if i % 2 == 0:
            c.p(x, y - 1, (140, 100, 66))
    # Collar.
    c.rect(19, 10, 22, 13, steel)
    c.p(19, 10, steel_hi)
    c.p(22, 13, steel_lo)
    # Amethyst crystal.
    am = [(26, 2), (30, 7), (27, 12), (22, 9)]
    c.poly(am, (150, 92, 214))
    c.poly([(26, 2), (27, 12), (22, 9)], (190, 140, 245))
    c.line((26, 3), (24, 8), (232, 210, 255))
    c.p(26, 2, (255, 255, 255))
    # Link sparkles.
    for (x, y) in ((17, 4), (29, 15), (21, 1)):
        c.p(x, y, (140, 236, 255))
        c.p(x - 1, y, (60, 170, 220), 200)
        c.p(x + 1, y, (60, 170, 220), 200)
        c.p(x, y - 1, (60, 170, 220), 200)
        c.p(x, y + 1, (60, 170, 220), 200)
    return c.finish()


def manual():
    """The Golem Engineer's Manual: a steel-blue book with brass corners, page edges and a gear."""
    c = Canvas()
    cover, cover_hi, cover_lo = (44, 56, 74), (72, 90, 118), (24, 30, 42)
    brass, brass_hi, brass_lo = (214, 170, 84), (250, 220, 140), (140, 100, 44)
    paper, paper_lo = (240, 230, 204), (204, 190, 160)
    # Pages (right edge and bottom).
    c.rect(8, 6, 27, 28, paper)
    for y in range(7, 28, 2):
        c.line((25, y), (27, y), paper_lo)
    c.rect(8, 27, 27, 28, paper_lo)
    # Cover.
    c.rect(4, 3, 24, 26, cover)
    c.rect(4, 3, 6, 26, cover_lo)  # spine
    c.line((7, 3), (24, 3), cover_hi)
    c.line((24, 4), (24, 26), cover_lo)
    for y in range(6, 25, 5):
        c.line((4, y), (6, y), brass_lo)
    # Brass corners.
    for (x, y, fx, fy) in ((24, 3, -1, 1), (24, 26, -1, -1)):
        for i in range(4):
            for j in range(4 - i):
                c.p(x + fx * i, y + fy * j, brass if i + j < 3 else brass_lo)
        c.p(x + fx, y + fy, brass_hi)
    # Gear emblem.
    cx, cy = 15.0, 13.5
    for y in range(7, 21):
        for x in range(9, 22):
            dx, dy = x - cx, y - cy
            r = math.hypot(dx, dy)
            ang = math.atan2(dy, dx)
            tooth = math.cos(ang * 8) > 0.2
            if 3.0 <= r <= 4.6 or (4.6 < r <= 6.0 and tooth):
                c.p(x, y, brass_hi if dy < -1 else brass if dy < 2 else brass_lo)
    c.rect(14, 12, 16, 15, (110, 232, 255))
    c.p(14, 12, (220, 252, 255))
    # Title plate.
    c.rect(10, 21, 20, 22, brass_lo)
    c.line((10, 21), (20, 21), brass)
    return c.finish()


def module_icon(kind):
    """A steel circuit chip with gold contacts and a glowing symbol for the module type."""
    c = Canvas()
    steel, steel_hi, steel_lo = (150, 158, 170), (214, 220, 228), (86, 92, 104)
    gold = (224, 182, 82)
    # Contacts.
    for i in range(4):
        y = 9 + i * 5
        c.rect(2, y, 5, y + 1, gold)
        c.rect(26, y, 29, y + 1, gold)
    # Body.
    c.rect(5, 5, 26, 26, steel)
    c.line((5, 5), (26, 5), steel_hi)
    c.line((5, 5), (5, 26), steel_hi)
    c.line((6, 26), (26, 26), steel_lo)
    c.line((26, 6), (26, 26), steel_lo)
    for (x, y) in ((7, 7), (24, 7), (7, 24), (24, 24)):
        c.p(x, y, steel_lo)
        c.p(x - 1, y - 1, steel_hi)
    # Window.
    win = (24, 30, 40)
    c.rect(9, 9, 22, 22, win)
    c.line((9, 22), (22, 22), (60, 70, 86))
    col = {"magnet": (110, 180, 255), "lamp": (255, 226, 122), "speed": (123, 224, 142), "storage": (198, 154, 116),
           "plating": (200, 208, 220), "regen": (255, 122, 156), "self_repair": (255, 180, 94), None: (90, 100, 116)}[kind]
    hi = mix(col, (255, 255, 255), 0.55)
    if kind == "magnet":
        for y in range(11, 19):
            c.p(12, y, col); c.p(13, y, col); c.p(18, y, col); c.p(19, y, col)
        c.rect(12, 19, 19, 20, col)
        c.rect(12, 11, 13, 12, (240, 90, 90)); c.rect(18, 11, 19, 12, (240, 90, 90))
        c.p(14, 20, hi)
    elif kind == "lamp":
        c.d.ellipse([12, 10, 19, 17], fill=col + (255,))
        c.rect(14, 18, 17, 20, (170, 176, 186))
        c.p(13, 12, (255, 255, 255)); c.p(14, 11, hi)
        for (x, y) in ((10, 10), (21, 10), (10, 17), (21, 17)):
            c.p(x, y, col)
    elif kind == "speed":
        for k in range(2):
            ox = 11 + k * 5
            for i in range(4):
                c.p(ox + i, 11 + i, col); c.p(ox + i + 1, 11 + i, col)
                c.p(ox + i, 19 - i, col); c.p(ox + i + 1, 19 - i, col)
        c.p(12, 11, hi)
    elif kind == "storage":
        c.rect(11, 12, 20, 20, col)
        c.rect(11, 12, 20, 14, mix(col, (255, 255, 255), 0.25))
        c.line((11, 15), (20, 15), (100, 70, 40))
        c.rect(15, 14, 16, 16, (230, 230, 230))
    elif kind == "plating":
        pts = [(11, 11), (20, 11), (20, 16), (15.5, 21), (11, 16)]
        c.poly(pts, col)
        c.line((15, 12), (15, 19), (150, 158, 170))
        c.p(12, 12, (255, 255, 255))
    elif kind == "regen":
        for (x, y) in ((12, 12), (13, 11), (14, 12), (17, 12), (18, 11), (19, 12)):
            c.p(x, y, col)
        c.rect(11, 13, 20, 15, col)
        c.rect(12, 16, 19, 16, col)
        c.rect(13, 17, 18, 17, col)
        c.rect(14, 18, 17, 18, col)
        c.rect(15, 19, 16, 19, col)
        c.p(12, 13, hi); c.p(13, 13, hi)
    elif kind == "self_repair":
        for i in range(8):
            c.p(11 + i, 19 - i, col); c.p(12 + i, 19 - i, col)
        c.rect(18, 10, 21, 13, col)
        c.p(20, 10, win); c.p(21, 11, win)
        c.rect(10, 18, 13, 21, col)
        c.p(10, 20, win); c.p(11, 21, win)
    else:
        for (x, y) in ((12, 12), (19, 12), (12, 19), (19, 19)):
            c.p(x, y, (110, 120, 140))
    if kind:
        # Glow around the symbol.
        for y in range(9, 23):
            for x in range(9, 23):
                if c.get(x, y)[:3] == win:
                    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                        q = c.get(x + dx, y + dy)
                        if q[:3] == col:
                            c.p(x, y, mix(win, col, 0.3))
                            break
    return c.finish()


def status_icons():
    """80x16 strip of 16x16 icons floated over golems: stopped, stuck, broken part, no power, low power."""
    strip = Image.new("RGBA", (80, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(strip)
    ol = (20, 16, 20, 255)
    # 0: stopped (red octagon, white bar)
    oct_pts = [(5, 1), (10, 1), (14, 5), (14, 10), (10, 14), (5, 14), (1, 10), (1, 5)]
    d.polygon(oct_pts, fill=(214, 48, 48, 255), outline=ol)
    d.rectangle([4, 7, 11, 8], fill=(255, 255, 255, 255))
    # 1: stuck (amber warning triangle with !)
    o = 16
    d.polygon([(o + 7.5, 1), (o + 14, 14), (o + 1, 14)], fill=(255, 186, 52, 255), outline=ol)
    d.rectangle([o + 7, 5, o + 8, 10], fill=(40, 30, 20, 255))
    d.rectangle([o + 7, 12, o + 8, 12], fill=(40, 30, 20, 255))
    # 2: broken part (gear with a red crack)
    o = 32
    import math as _m
    for y in range(16):
        for x in range(16):
            dx, dy = x - 7.5, y - 7.5
            r = _m.hypot(dx, dy)
            ang = _m.atan2(dy, dx)
            if 2.2 <= r <= 5.0 or (5.0 < r <= 6.8 and _m.cos(ang * 6) > 0.25):
                strip.putpixel((o + x, y), (176, 184, 196, 255) if dy < 0 else (128, 136, 150, 255))
    for (x, y) in ((9, 2), (8, 3), (8, 4), (7, 5), (8, 6), (7, 7), (6, 8), (7, 9), (6, 10), (5, 11), (6, 12)):
        strip.putpixel((o + x, y), (230, 50, 40, 255))
    # 3: no power (empty battery, red)
    o = 48
    d.rectangle([o + 2, 4, o + 12, 12], fill=(40, 20, 20, 255), outline=(230, 60, 60, 255))
    d.rectangle([o + 13, 6, o + 14, 10], fill=(230, 60, 60, 255))
    d.line([(o + 5, 6), (o + 9, 10)], fill=(255, 90, 90, 255))
    d.line([(o + 9, 6), (o + 5, 10)], fill=(255, 90, 90, 255))
    # 4: low power (battery, one yellow bar)
    o = 64
    d.rectangle([o + 2, 4, o + 12, 12], fill=(30, 30, 36, 255), outline=(240, 220, 120, 255))
    d.rectangle([o + 13, 6, o + 14, 10], fill=(240, 220, 120, 255))
    d.rectangle([o + 4, 6, o + 5, 10], fill=(255, 210, 60, 255))
    # Dark outline around everything for readability against any background.
    arr = np.asarray(strip).copy()
    a = arr[..., 3] > 0
    out = arr.copy()
    for y in range(16):
        for x in range(80):
            if not a[y, x]:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < 80 and 0 <= ny < 16 and a[ny, nx] and (nx // 16 == x // 16):
                        out[y, x] = (20, 16, 20, 230)
                        break
    return Image.fromarray(out, "RGBA")


def generate_hand(assets):
    item = os.path.join(assets, "textures/item")
    ingot(_hex("#9aa2ab")).save(os.path.join(item, "steel_ingot.png"))
    repair_kit().save(os.path.join(item, "repair_kit.png"))
    blueprint().save(os.path.join(item, "blueprint.png"))
    linker().save(os.path.join(item, "golem_linker.png"))
    manual().save(os.path.join(item, "manual.png"))
    status_icons().save(os.path.join(assets, "textures/gui/status_icons.png"))
    module_icon(None).save(os.path.join(item, "module_casing.png"))
    for kind in ("magnet", "lamp", "speed", "storage", "plating", "regen", "self_repair"):
        module_icon(kind).save(os.path.join(item, "module_%s.png" % kind))


def generate(gen, assets):
    generate_renders(gen, assets)
    generate_hand(assets)
    print("item art: 32x32 renders for %d parts x %d materials, golems, cubs and tools" % (
        len(S.SHAPES), len(M.CRAFTABLE)))
