"""Large 3D renders of a golem from the real model (reuses tools/golemgen/iconrender.py) at any yaw."""
import math
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, os.path.join(HERE, "..", "golemgen"))

import numpy as np  # noqa: E402
from PIL import Image  # noqa: E402

import gen  # noqa: E402
import iconrender as R  # noqa: E402

ASSETS = os.path.join(HERE, "..", "..", "src/main/resources/assets/golemgarage")
TEX = os.path.join(ASSETS, "textures/entity/golem")
_groups = gen.build_tree()
gen.pack(_groups)


def _bounds_poly(polys, pad=1.08):
    pts = np.array([p for poly in polys for p in poly[0]])
    cx, cy = (pts[:, 0].min() + pts[:, 0].max()) / 2, (pts[:, 1].min() + pts[:, 1].max()) / 2
    span = max(pts[:, 0].max() - pts[:, 0].min(), pts[:, 1].max() - pts[:, 1].min()) * pad / 2
    return cx, cy, span


def render(shapes, material, size, yaw_deg, pitch_deg=14, glow_rgb=(255, 59, 42), fixed=None, paint=None):
    """Returns (image, fixed) where fixed keeps the framing identical across frames."""
    R.SIZE = size
    R.YAW = math.radians(yaw_deg)
    R.PITCH = math.radians(pitch_deg)
    R.LIGHT_VIEW = R.view_dir(R.LIGHT)
    polys = R.shape_polys(_groups, set(shapes), gen.uv_of, {})
    if fixed is None:
        fixed = _bounds_poly(polys, 1.22)
    cx, cy, span = fixed
    # An invisible (back-facing) square pins the framing so the golem doesn't change size as it turns.
    frame = [np.array([cx - span, cy - span, 0]), np.array([cx + span, cy - span, 0]),
             np.array([cx + span, cy + span, 0]), np.array([cx - span, cy + span, 0])]
    polys = polys + [(frame, [(0, 0)] * 4, np.array([0, 0, 1.0]), "pin", 0)]
    gb = R.GBuffer(polys, size=size, margin=0.0)
    atlas = Image.open(os.path.join(TEX, material + ".png"))
    glow = Image.open(os.path.join(TEX, "glow.png"))
    overlay = None
    if paint is not None:
        p = Image.open(os.path.join(TEX, "paint.png")).convert("RGBA")
        a = np.asarray(p, dtype=float)
        a[..., :3] = a[..., :3] / 255.0 * np.array(paint)
        overlay = Image.fromarray(a.astype(np.uint8), "RGBA")
    img = R.shade(gb, atlas, glow, glow_rgb, overlay=overlay, flat_tags=(1,))
    return img, fixed
