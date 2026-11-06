"""
A small kit for building item icons as tiny 3D models.

An icon is a Scene of boxes (Minecraft model units, y down, front -z). Each box gets its own patch of a
generated texture atlas, painted by a Paint (flat color with bevels and grain, or a custom drawing per
face). The scene is then rendered with the same G-buffer, light and outline as the part and golem
renders (iconrender.py), so every icon in the mod shares one look: a 3/4 view lit from the upper left,
crisp face creases, a dark tinted outline and a top-left rim.
"""
import math
import random

import numpy as np
from PIL import Image, ImageDraw

import iconrender as R
import paint as P

FACES = ("DOWN", "UP", "WEST", "NORTH", "EAST", "SOUTH")
# In the icon view the model's DOWN face (y down) is on top, NORTH faces the viewer and EAST is the lit-side right.
ROLE = {"DOWN": "top", "NORTH": "front", "EAST": "side", "UP": "bottom", "WEST": "left", "SOUTH": "back"}


def rgb(c):
    if isinstance(c, str):
        return P.hex_rgb(c)
    return tuple(int(v) for v in c[:3])


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def mul(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in rgb(c))


# ================================================================== paints
class Face:
    """A drawing surface for one face: w x h texels, with helpers. (0, 0) is the face's top-left."""

    def __init__(self, img, x, y, w, h, name, seed):
        self.img, self.x, self.y, self.w, self.h, self.name = img, x, y, w, h, name
        self.px = img.load()
        self.rng = random.Random(seed)
        self.role = ROLE[name]

    def p(self, x, y, c, a=255):
        if 0 <= x < self.w and 0 <= y < self.h:
            self.px[self.x + x, self.y + y] = rgb(c) + (a,)

    def get(self, x, y):
        return self.px[self.x + x, self.y + y][:3]

    def rect(self, x0, y0, x1, y1, c):
        for yy in range(max(0, y0), min(self.h, y1 + 1)):
            for xx in range(max(0, x0), min(self.w, x1 + 1)):
                self.p(xx, yy, c)

    def hline(self, y, c, x0=0, x1=None):
        self.rect(x0, y, self.w - 1 if x1 is None else x1, y, c)

    def vline(self, x, c, y0=0, y1=None):
        self.rect(x, y0, x, self.h - 1 if y1 is None else y1, c)

    def clear(self):
        for yy in range(self.h):
            for xx in range(self.w):
                self.px[self.x + xx, self.y + yy] = (0, 0, 0, 0)


class Paint:
    """Flat color from a 7-step ramp, with bevelled edges and a little grain."""

    def __init__(self, base, grain=0.05, bevel=True, ramp=None, detail=None):
        if ramp is None:
            ramp = P.ramp(base if isinstance(base, str) else "#%02x%02x%02x" % rgb(base))
        self.ramp = ramp
        self.grain, self.bevel, self.detail = grain, bevel, detail

    def paint(self, f):
        r = self.ramp
        for y in range(f.h):
            for x in range(f.w):
                c = r[3]
                n = f.rng.random()
                if n < self.grain:
                    c = r[2]
                elif n > 1 - self.grain:
                    c = r[4]
                f.p(x, y, c)
        if self.bevel and f.w > 2 and f.h > 2:
            f.hline(0, r[5])
            f.vline(0, r[4], 1)
            f.hline(f.h - 1, r[1], 1)
            f.vline(f.w - 1, r[2], 1, f.h - 2)
        if self.detail:
            self.detail(f, r)


class Faces:
    """Different paints per face; anything not listed uses the default."""

    def __init__(self, default, **faces):
        self.default, self.faces = default, faces

    def paint(self, f):
        self.faces.get(f.role, self.default).paint(f)


class Draw:
    """Paints a base, then calls fn(face, ramp) to draw on top (only on the named faces, if given)."""

    def __init__(self, base, fn, faces=None, **kw):
        self.base = Paint(base, **kw) if not isinstance(base, (Paint, Faces, Draw)) else base
        self.fn, self.faces = fn, faces

    def paint(self, f):
        self.base.paint(f)
        if self.faces is None or f.role in self.faces:
            self.fn(f, getattr(self.base, "ramp", None))


# ================================================================== scene
class Scene:
    def __init__(self, k=2):
        self.k = k          # texels per model unit
        self.boxes = []

    def box(self, o, s, paint, pivot=(0, 0, 0), rot=(0, 0, 0), glow=False, parent=None):
        """A box at o (relative to pivot) of size s; rot is degrees about the pivot (x, y, z). An optional
        parent (pivot, rot) turns the whole thing again afterwards, for tilting a built-up group."""
        par = None
        if parent is not None:
            par = (parent[0], tuple(math.radians(a) for a in parent[1]))
        self.boxes.append(dict(o=o, s=s, paint=paint, pivot=pivot,
                               rot=tuple(math.radians(a) for a in rot), glow=glow, parent=par))
        return self

    def render(self, yaw=28, pitch=24, spin=0.0, margin=1.5, zoom=1.0):
        k = self.k
        # Pack each box's UV layout into an atlas, shelf by shelf.
        layouts, x, y, shelf, width = [], 0, 0, 0, 1024
        for b in self.boxes:
            w, h, d = (max(1, int(round(v * k))) for v in b["s"])
            rw, rh = 2 * (d + w), d + h
            if x + rw > width:
                x, y, shelf = 0, y + shelf, 0
            layouts.append((x, y, w, h, d))
            x += rw
            shelf = max(shelf, rh)
        atlas = Image.new("RGBA", (width, max(1, y + shelf)), (0, 0, 0, 0))
        for i, (b, (u, v, w, h, d)) in enumerate(zip(self.boxes, layouts)):
            regions = {"DOWN": (u + d, v, w, d), "UP": (u + d + w, v, w, d), "WEST": (u, v + d, d, h),
                       "NORTH": (u + d, v + d, w, h), "EAST": (u + d + w, v + d, d, h),
                       "SOUTH": (u + d + w + d, v + d, w, h)}
            for name, (fx, fy, fw, fh) in regions.items():
                b["paint"].paint(Face(atlas, fx, fy, fw, fh, name, i * 31 + FACES.index(name)))
        # Geometry in the requested view.
        old = (R.YAW, R.PITCH, R.LIGHT_VIEW)
        R.YAW, R.PITCH = math.radians(yaw), math.radians(pitch)
        R.LIGHT_VIEW = R.view_dir(R.LIGHT)
        try:
            polys = []
            for b, (u, v, w, h, d) in zip(self.boxes, layouts):
                base = R.Xf()
                if b.get("parent"):
                    base = base.then_local(*b["parent"])
                xf = base.then_local(b["pivot"], b["rot"])
                for poly in R.cube_polys(b["o"], b["s"], u, v, False, xf, uv_size=(w, h, d)):
                    polys.append(poly + (1 if b["glow"] else 0,))
            gb = R.GBuffer(polys, margin=margin, zoom=zoom, spin=spin)
        finally:
            R.YAW, R.PITCH, R.LIGHT_VIEW = old
        return R.shade(gb, atlas, flat_tags=(1,))


# ================================================================== flat (2D) helper for paper-like icons
class Flat:
    """A 32x32 hand-drawn canvas for things that are genuinely flat; finished with the same outline."""

    def __init__(self):
        self.img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)
        self.px = self.img.load()

    def p(self, x, y, c, a=255):
        if 0 <= x < 32 and 0 <= y < 32:
            self.px[x, y] = rgb(c) + (a,)

    def rect(self, x0, y0, x1, y1, c):
        self.d.rectangle([x0, y0, x1, y1], fill=rgb(c) + (255,))

    def poly(self, pts, c):
        self.d.polygon(pts, fill=rgb(c) + (255,))

    def line(self, a, b, c, w=1):
        self.d.line([a, b], fill=rgb(c) + (255,), width=w)

    def ellipse(self, box, c):
        self.d.ellipse(box, fill=rgb(c) + (255,))

    def finish(self):
        arr = np.asarray(self.img, dtype=float)
        return Image.fromarray(np.clip(R._outline(arr), 0, 255).astype(np.uint8), "RGBA")
