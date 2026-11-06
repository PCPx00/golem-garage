"""
All item icons at 32x32.

Parts, golems and cubs are 3D renders of the real models (iconrender.py) in each material.
Tools, materials and gear are small 3D models in icons2.py; this file keeps the GUI status icons.
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


# ================================================================== GUI
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
    status_icons().save(os.path.join(assets, "textures/gui/status_icons.png"))
    # Every hand-made item icon is a small 3D render now (icons2.py).
    import icons2
    icons2.generate(assets)


def generate(gen, assets):
    generate_renders(gen, assets)
    generate_hand(assets)
    print("item art: 32x32 renders for %d parts x %d materials, golems, cubs and tools" % (
        len(S.SHAPES), len(M.CRAFTABLE)))
