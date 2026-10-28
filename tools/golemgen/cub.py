"""
The Cub: a chibi golem. Geometry, UVs and skins come from here, like the golem.
Writes client/model/CubGeometry.java and textures/entity/cub/<color>.png (+ cub_glow.png).
"""
import os
import random
import zlib

import paint
from items import CUB_COLORS

ATLAS = (64, 64)


def B(o, s, style="body", deco=None, glow=False):
    return dict(o=o, s=s, style=style, deco=deco, glow=glow)


def P(name, pivot, boxes, children=(), mirror=False):
    return dict(name=name, pivot=pivot, boxes=list(boxes), children=list(children), mirror=mirror)


PARTS = [
    P("head", (0, 16, 0), [
        B((-5, -9, -4.5), (10, 9, 9), deco="face"),
        B((-1, -4, -5.5), (2, 2, 1), style="shade"),
        B((-6, -6, -1), (1, 2, 2), style="metal"),
        B((5, -6, -1), (1, 2, 2), style="metal"),
    ], children=[
        P("sprout", (0, -9, 0), [
            B((-0.5, -2, -0.5), (1, 2, 1), style="stem"),
            B((0.5, -3, -1), (3, 1, 2), style="leaf"),
            B((-3.5, -2, -1), (3, 1, 2), style="leaf"),
        ]),
        P("eyelids", (0, 0, 0), [
            B((-4, -6, -4.75), (3, 3, 1), style="body"),
            B((1, -6, -4.75), (3, 3, 1), style="body"),
        ]),
    ]),
    P("body", (0, 16, 0), [
        B((-3, 0, -2), (6, 5, 4), deco="belly"),
        B((-1, 1.5, -2.25), (2, 2, 1), style="heart", glow=True),
    ]),
    P("right_arm", (-3.5, 16.5, 0), [B((-1.5, 0, -1), (2, 4, 2), deco="paw")]),
    P("left_arm", (3.5, 16.5, 0), [B((-0.5, 0, -1), (2, 4, 2), deco="paw")], mirror=True),
    P("right_leg", (-1.5, 21, 0), [B((-1, 0, -1), (2, 3, 2), style="shade")]),
    P("left_leg", (1.5, 21, 0), [B((-1, 0, -1), (2, 3, 2), style="shade")], mirror=True),
]


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
        if x + rw > ATLAS[0]:
            x, y, shelf = 0, y + shelf, 0
        b["uv"] = (x, y)
        x += rw
        shelf = max(shelf, rh)
    assert y + shelf <= ATLAS[1], "cub atlas overflow"


EYE = (34, 30, 40)
WHITE = (255, 255, 255)
BLUSH = (255, 128, 160)


def paint_cub(boxes, base_hex):
    pal = paint.ramp(base_hex)
    c = paint.Canvas(*ATLAS)
    leaf = paint.ramp("#6cc24a")
    metal = paint.ramp("#b8bcc4")
    heart = paint.ramp("#ff6f9a")
    for b in boxes:
        u, v = b["uv"]
        w, h, d = b["s"]
        faces = paint.box_faces(u, v, w, h, d)
        rng = random.Random(zlib.crc32(str((b["o"], b["s"])).encode()))
        style = b["style"]
        p = {"body": pal, "shade": pal, "metal": metal, "leaf": leaf, "stem": leaf, "heart": heart}[style]
        for fname, (x0, y0, x1, y1) in faces.items():
            shift = {"top": 1, "bottom": -1, "back": -1}.get(fname, 0)
            base = {"shade": 2, "stem": 2}.get(style, 3) + shift
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    k = base + (1 if rng.random() < 0.08 else 0)
                    c.set(xx, yy, p[max(0, min(6, k))])
            fw, fh = x1 - x0, y1 - y0
            if fw >= 3 and fh >= 3 and style in ("body", "shade"):
                # Soft rounded look: light top edge, darker bottom edge, softened corners.
                for xx in range(x0, x1):
                    c.set(xx, y0, p[min(6, base + 1)])
                    c.set(xx, y1 - 1, p[max(0, base - 1)])
                for (cx, cy) in ((x0, y0), (x1 - 1, y0), (x0, y1 - 1), (x1 - 1, y1 - 1)):
                    c.set(cx, cy, p[max(0, base - 1)])
            if b.get("deco") == "face" and fname == "front":
                # Big sparkly eyes, blush and a tiny smile.
                for (ex, ey) in ((x0 + 1, y0 + 3), (x0 + 6, y0 + 3)):
                    for dx in range(3):
                        for dy in range(3):
                            c.set(ex + dx, ey + dy, EYE)
                    c.set(ex, ey, WHITE)
                    c.set(ex + 2, ey + 2, (90, 80, 120))
                c.set(x0 + 1, y0 + 6, BLUSH); c.set(x0 + 2, y0 + 6, BLUSH)
                c.set(x1 - 2, y0 + 6, BLUSH); c.set(x1 - 3, y0 + 6, BLUSH)
                c.set(x0 + 4, y0 + 7, p[1]); c.set(x0 + 5, y0 + 7, p[1])
                c.set(x0 + 3, y0 + 6, p[1]); c.set(x0 + 6, y0 + 6, p[1])
            if b.get("deco") == "face" and fname == "top":
                c.set(x0 + 2, y0 + 2, p[5]); c.set(x0 + 6, y0 + 4, p[5])
            if b.get("deco") == "belly" and fname == "front":
                for xx in range(x0 + 1, x1 - 1):
                    c.set(xx, y1 - 2, p[2])
            if b.get("deco") == "paw" and fname in ("front", "bottom"):
                c.set(x0, y1 - 1, p[1]); c.set(x1 - 1, y1 - 1, p[1])
            if style == "leaf" and fname in ("top", "bottom"):
                c.set(x0 + fw // 2, y0, leaf[5])
    return c.img


def glow_texture(boxes):
    c = paint.Canvas(*ATLAS)
    for b in boxes:
        if not b["glow"]:
            continue
        u, v = b["uv"]
        w, h, d = b["s"]
        for (x0, y0, x1, y1) in paint.box_faces(u, v, w, h, d).values():
            for yy in range(y0, y1):
                for xx in range(x0, x1):
                    c.set(xx, yy, (255, 150, 190), 255)
    return c.img


def f(x):
    s = ("%.4f" % x).rstrip("0").rstrip(".")
    return ("0" if s in ("-0", "") else s) + "F"


def java_part(lines, parent, part, counter):
    var = "p%d" % counter[0]
    counter[0] += 1
    b = "CubeListBuilder.create()" + (".mirror()" if part["mirror"] else "")
    for box in part["boxes"]:
        u, v = box["uv"]
        x, y, z = box["o"]
        w, h, d = box["s"]
        b += "\n                .texOffs(%d, %d).addBox(%s, %s, %s, %s, %s, %s)" % (u, v, f(x), f(y), f(z), f(w), f(h), f(d))
    px, py, pz = part["pivot"]
    lines.append('        PartDefinition %s = %s.addOrReplaceChild("%s", %s,\n                PartPose.offset(%s, %s, %s));'
                 % (var, parent, part["name"], b, f(px), f(py), f(pz)))
    for ch in part["children"]:
        java_part(lines, var, ch, counter)


def generate(java_dir, assets):
    boxes = []
    for p in PARTS:
        all_boxes(p, boxes)
    pack(boxes)
    out = os.path.join(assets, "textures/entity/cub")
    os.makedirs(out, exist_ok=True)
    for name, hexc in CUB_COLORS:
        paint_cub(boxes, hexc).save(os.path.join(out, name + ".png"))
    glow_texture(boxes).save(os.path.join(out, "glow.png"))
    old = os.path.join(assets, "textures/entity/cub.png")
    if os.path.exists(old):
        os.remove(old)

    # Sleepy "z" particle.
    z = paint.Canvas(8, 8)
    for x in range(1, 7):
        z.set(x, 1, (235, 240, 255)); z.set(x, 6, (235, 240, 255))
    for i in range(4):
        z.set(5 - i, 2 + i, (235, 240, 255))
    for x in range(1, 7):
        z.set(x, 7, (120, 130, 170))
    pdir = os.path.join(assets, "textures/particle")
    os.makedirs(pdir, exist_ok=True)
    z.img.save(os.path.join(pdir, "sleep_z.png"))
    os.makedirs(os.path.join(assets, "particles"), exist_ok=True)
    with open(os.path.join(assets, "particles/sleep_z.json"), "w") as fh:
        fh.write('{ "textures": [ "golemgarage:sleep_z" ] }\n')

    lines = []
    counter = [0]
    for p in PARTS:
        java_part(lines, "root", p, counter)
    src = """package com.xtrip.golemgarage.client.model;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Cub geometry.
 * GENERATED by tools/golemgen/cub.py. Edit the generator, not this file.
 */
public final class CubGeometry {
    public static LayerDefinition create() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
%s
        return LayerDefinition.create(mesh, %d, %d);
    }

    private CubGeometry() {
    }
}
""" % ("\n".join(lines), ATLAS[0], ATLAS[1])
    with open(os.path.join(java_dir, "client/model/CubGeometry.java"), "w") as fh:
        fh.write(src)
