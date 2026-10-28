"""
Renders 32x32 item icons from the real golem geometry and painted entity textures.

Each icon is a small 3D render: the part's boxes (the same ones the in-game model uses, with the same
Minecraft UV layout) are projected in a 3/4 view, lit from the upper left, sampled from the material's
entity texture, then finished with a dark outline and a rim highlight so they read cleanly in a slot.
Geometry is rasterized once per shape; each material is just a texture lookup.
"""
import math
import os

import numpy as np
from PIL import Image

SIZE = 32
SS = 4  # subsamples per pixel side

# View: turn the model to show its left side, and look down on it a little.
YAW = math.radians(28)
PITCH = math.radians(24)
LIGHT = np.array([-0.45, -1.0, -0.55])
LIGHT = LIGHT / np.linalg.norm(LIGHT)

DIRS = {"DOWN": (0, -1, 0), "UP": (0, 1, 0), "WEST": (-1, 0, 0), "EAST": (1, 0, 0),
        "NORTH": (0, 0, -1), "SOUTH": (0, 0, 1)}


# ------------------------------------------------------------------ math
def rot_matrix(rx, ry, rz):
    """Minecraft ModelPart rotation: rotationZYX(z, y, x) = Rz * Ry * Rx."""
    cx, sx = math.cos(rx), math.sin(rx)
    cy, sy = math.cos(ry), math.sin(ry)
    cz, sz = math.cos(rz), math.sin(rz)
    Rx = np.array([[1, 0, 0], [0, cx, -sx], [0, sx, cx]])
    Ry = np.array([[cy, 0, sy], [0, 1, 0], [-sy, 0, cy]])
    Rz = np.array([[cz, -sz, 0], [sz, cz, 0], [0, 0, 1]])
    return Rz @ Ry @ Rx


def view(p):
    """Model space (y down, front -z) to screen: x right, y down, depth (smaller = nearer)."""
    x, y, z = p
    x1 = x * math.cos(YAW) + z * math.sin(YAW)
    z1 = -x * math.sin(YAW) + z * math.cos(YAW)
    y2 = y * math.cos(PITCH) - z1 * math.sin(PITCH)
    z2 = y * math.sin(PITCH) + z1 * math.cos(PITCH)
    return np.array([x1, y2, z2])


def view_dir(n):
    v = view(n)
    return v / (np.linalg.norm(v) or 1)


LIGHT_VIEW = view_dir(LIGHT)


# ------------------------------------------------------------------ geometry
def cube_polys(o, s, u, v, mirror, xf, uv_size=None):
    """The six faces of a Minecraft model box: [(4 screen points, 4 uvs, normal, face name)]."""
    x0, y0, z0 = o
    w, h, d = s
    x1, y1, z1 = x0 + w, y0 + h, z0 + d
    if uv_size is not None:
        w, h, d = uv_size
    if mirror:
        x0, x1 = x1, x0
    v7 = (x0, y0, z0); v_ = (x1, y0, z0); v1 = (x1, y1, z0); v2 = (x0, y1, z0)
    v3 = (x0, y0, z1); v4 = (x1, y0, z1); v5 = (x1, y1, z1); v6 = (x0, y1, z1)
    f4 = u; f5 = u + d; f6 = u + d + w; f7 = u + d + w + w; f8 = u + d + w + d; f9 = u + d + w + d + w
    f10 = v; f11 = v + d; f12 = v + d + h
    faces = [
        ("DOWN", [v4, v3, v7, v_], f5, f10, f6, f11),
        ("UP", [v1, v2, v6, v5], f6, f11, f7, f10),
        ("WEST", [v7, v3, v6, v2], f4, f11, f5, f12),
        ("NORTH", [v_, v7, v2, v1], f5, f11, f6, f12),
        ("EAST", [v4, v_, v1, v5], f6, f11, f8, f12),
        ("SOUTH", [v3, v4, v5, v6], f8, f11, f9, f12),
    ]
    out = []
    for name, verts, ua, va, ub, vb in faces:
        uvs = [(ub, va), (ua, va), (ua, vb), (ub, vb)]
        pts = [xf(np.array(p, dtype=float)) for p in verts]
        n = np.array(DIRS[name], dtype=float)
        if mirror and name in ("WEST", "EAST"):
            n = -n
        R = xf.rot
        out.append(([view(p) for p in pts], uvs, view_dir(R @ n), name))
    return out


class Xf:
    """An affine transform: p -> R p + t."""

    def __init__(self, R=None, t=None):
        self.rot = np.eye(3) if R is None else R
        self.t = np.zeros(3) if t is None else np.array(t, dtype=float)

    def then_local(self, pivot, rot):
        """Child transform: translate(pivot) then rotate (in the parent's frame)."""
        R = rot_matrix(*rot)
        return Xf(self.rot @ R, self.t + self.rot @ np.array(pivot, dtype=float))

    def __call__(self, p):
        return self.rot @ p + self.t


def collect_part(part, xf, uv_of, polys, tag):
    child = xf.then_local(part["pivot"], part.get("rot", (0, 0, 0)))
    for b in part["boxes"]:
        u, v = uv_of(b)
        mirror = bool(b.get("mirror"))
        for poly in cube_polys(b["o"], b["s"], u, v, mirror, child):
            polys.append(poly + (tag(b),))
    for c in part["children"]:
        collect_part(c, child, uv_of, polys, tag)


# ------------------------------------------------------------------ rasterizing
class GBuffer:
    """Per subsample: covered?, texture u/v, light, and which face (for edge lines)."""

    def __init__(self, polys, size=SIZE, margin=1.5, zoom=1.0, spin=0.0):
        n = size * SS
        if spin:
            # Turn the whole picture in the screen plane (used to lay long arms diagonally).
            ca, sa = math.cos(math.radians(spin)), math.sin(math.radians(spin))
            polys = [([np.array([p[0] * ca - p[1] * sa, p[0] * sa + p[1] * ca, p[2]]) for p in pp], uvs, nrm, nm, tg)
                     for (pp, uvs, nrm, nm, tg) in polys]
        pts = np.array([p for poly in polys for p in poly[0]])
        minx, miny = pts[:, 0].min(), pts[:, 1].min()
        maxx, maxy = pts[:, 0].max(), pts[:, 1].max()
        span = max(maxx - minx, maxy - miny)
        scale = (size - 2 * margin) * SS / span * zoom
        offx = n / 2 - (minx + maxx) / 2 * scale
        offy = n / 2 - (miny + maxy) / 2 * scale
        self.depth = np.full((n, n), np.inf)
        self.u = np.zeros((n, n))
        self.v = np.zeros((n, n))
        self.light = np.zeros((n, n))
        self.face = np.full((n, n), -1, dtype=int)
        self.tag = np.zeros((n, n), dtype=int)
        ys, xs = np.mgrid[0:n, 0:n] + 0.5
        for fi, (pp, uvs, normal, name, tag) in enumerate(polys):
            if normal[2] >= 0:  # back-facing
                continue
            P = [np.array([p[0] * scale + offx, p[1] * scale + offy, p[2]]) for p in pp]
            a, b, c = P[1], P[0], P[2]  # a=(u1,v1), b=(u2,v1), c=(u1,v2)
            e1 = b[:2] - a[:2]
            e2 = c[:2] - a[:2]
            det = e1[0] * e2[1] - e1[1] * e2[0]
            if abs(det) < 1e-6:
                continue
            x0 = int(max(0, math.floor(min(p[0] for p in P))))
            x1 = int(min(n, math.ceil(max(p[0] for p in P))))
            y0 = int(max(0, math.floor(min(p[1] for p in P))))
            y1 = int(min(n, math.ceil(max(p[1] for p in P))))
            if x0 >= x1 or y0 >= y1:
                continue
            px = xs[y0:y1, x0:x1] - a[0]
            py = ys[y0:y1, x0:x1] - a[1]
            s = (px * e2[1] - py * e2[0]) / det
            t = (e1[0] * py - e1[1] * px) / det
            inside = (s >= 0) & (s <= 1) & (t >= 0) & (t <= 1)
            z = a[2] + s * (b[2] - a[2]) + t * (c[2] - a[2])
            region = self.depth[y0:y1, x0:x1]
            hit = inside & (z < region - 1e-4)
            if not hit.any():
                continue
            (ua, va), (ub, _), (_, vb) = uvs[1], uvs[0], uvs[2]
            uu = ua + s * (ub - ua)
            vv = va + t * (vb - va)
            ndl = max(0.0, float(np.dot(normal, LIGHT_VIEW)))
            light = 0.5 + 0.62 * ndl
            region[hit] = z[hit]
            self.u[y0:y1, x0:x1][hit] = uu[hit]
            self.v[y0:y1, x0:x1][hit] = vv[hit]
            self.light[y0:y1, x0:x1][hit] = light
            self.face[y0:y1, x0:x1][hit] = fi
            self.tag[y0:y1, x0:x1][hit] = tag
        self.covered = self.face >= 0


def shade(gb, atlas, glow_mask=None, glow_rgb=None, overlay=None, darken=1.0, flat_tags=()):
    """Turns a G-buffer into a finished 32x32 RGBA icon using one texture atlas."""
    tex = np.asarray(atlas.convert("RGBA"), dtype=float)
    th, tw = tex.shape[:2]
    n = SIZE * SS
    ui = np.clip(np.floor(gb.u).astype(int), 0, tw - 1)
    vi = np.clip(np.floor(gb.v).astype(int), 0, th - 1)
    rgb = tex[vi, ui, :3]
    alpha = tex[vi, ui, 3]
    if overlay is not None:
        ov = np.asarray(overlay.convert("RGBA"), dtype=float)[vi, ui]
        a = ov[..., 3:4] / 255.0
        rgb = rgb * (1 - a) + ov[..., :3] * a
    light = gb.light[..., None]
    col = rgb * light * darken
    glow = np.zeros((n, n), dtype=bool)
    if glow_mask is not None and glow_rgb is not None:
        gm = np.asarray(glow_mask.convert("RGBA"), dtype=float)[vi, ui]
        glow = (gm[..., 3] > 0) & gb.covered
        gl = gm[..., :3] / 255.0
        bright = np.array(glow_rgb, dtype=float)
        gcol = np.clip(bright * (0.55 + 0.6 * gl.mean(axis=-1, keepdims=True)), 0, 255)
        col = np.where(glow[..., None], gcol, col)
    for t in flat_tags:  # unshaded (already-lit) surfaces like glass
        m = gb.tag == t
        col[m] = rgb[m]
    covered = gb.covered & (alpha > 8)
    # Downsample: a pixel is drawn when enough of it is covered; its color is the covered average.
    cov = covered.reshape(SIZE, SS, SIZE, SS).sum(axis=(1, 3))
    csum = (col * covered[..., None]).reshape(SIZE, SS, SIZE, SS, 3).sum(axis=(1, 3))
    gsum = glow.reshape(SIZE, SS, SIZE, SS).sum(axis=(1, 3))
    out = np.zeros((SIZE, SIZE, 4))
    draw = cov >= (SS * SS) * 0.38
    avg = csum / np.maximum(cov, 1)[..., None]
    out[..., :3] = avg
    out[..., 3] = np.where(draw, 255, 0)
    # Face edges: darken pixels where two faces meet so the shape reads clearly.
    faces = gb.face.reshape(SIZE, SS, SIZE, SS)
    out = _edges(out, faces, draw, gsum)
    out = _outline(out)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")


def _edges(out, faces, draw, glow):
    """Subtly darkens pixels that straddle two faces (crease lines) and lightens top-left rims."""
    res = out.copy()
    for y in range(SIZE):
        for x in range(SIZE):
            if not draw[y, x] or glow[y, x] > 0:
                continue
            f = faces[y, :, x, :]
            vals = set(f[f >= 0].ravel().tolist())
            if len(vals) > 1:
                res[y, x, :3] *= 0.86
    return res


def _outline(img):
    """A dark outline around the silhouette, tinted by the neighboring color, plus a soft top-left rim."""
    a = img[..., 3] > 0
    res = img.copy()
    for y in range(SIZE):
        for x in range(SIZE):
            if a[y, x]:
                up = y == 0 or not a[y - 1, x]
                left = x == 0 or not a[y, x - 1]
                if up or left:
                    res[y, x, :3] = np.minimum(255, img[y, x, :3] * 1.12 + 10)
                continue
            neigh = []
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < SIZE and 0 <= ny < SIZE and a[ny, nx]:
                    neigh.append(img[ny, nx, :3])
            if neigh:
                c = np.mean(neigh, axis=0)
                res[y, x, :3] = c * 0.28 + np.array([12, 10, 14])
                res[y, x, 3] = 255
    return res


# ------------------------------------------------------------------ public helpers
def shape_polys(groups, shape_ids, uv_of, group_pivots, only_groups=None):
    """Polygons for the given shapes (from gen.build_tree groups), placed where the golem model puts them."""
    polys = []
    for gname, (pivot, items) in groups.items():
        if only_groups is not None and gname not in only_groups:
            continue
        root = Xf().then_local(group_pivots.get(gname, pivot), (0, 0, 0))
        for sid, part in items:
            if sid in shape_ids:
                collect_part(part, root, uv_of, polys, lambda b: 1 if b.get("style") in ("glass", "solar") else 0)
    return polys
