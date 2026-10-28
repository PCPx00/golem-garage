"""
Animated Golem Garage logo (400x400) and title banner (800x220).

A painted golem turns gently under two scanning garage lasers, its eyes and core pulsing, over a
garage-door backdrop with a scrolling hazard stripe. Frames are written as PNGs and turned into
GIFs with ffmpeg (shared palette, no dithering noise). Also writes a still PNG of the logo.
Usage: python3 logo.py <out dir>
"""
import math
import os
import subprocess
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

import golem3d
import mcfont

OUT = sys.argv[1] if len(sys.argv) > 1 else "/tmp/golemgarage/brand"
os.makedirs(OUT, exist_ok=True)
FRAMES = 40
BUILD = ["sentry_head", "plated_chest", "golem_fists", "stub_legs", "redstone_core"]
YELLOW = (247, 196, 38)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(len(a)))


def backdrop(w, h, slat_top, slat_bottom):
    img = Image.new("RGBA", (w, h))
    px = ImageDraw.Draw(img)
    for y in range(h):
        px.line([(0, y), (w, y)], fill=lerp((34, 40, 56), (13, 16, 23), y / h) + (255,))
    # Garage-door slats.
    slats = Image.new("RGBA", (w, h))
    sd = ImageDraw.Draw(slats)
    for y in range(slat_top + 8, slat_bottom, 18):
        sd.line([(0, y), (w, y)], fill=(255, 255, 255, 14))
        sd.line([(0, y + 1), (w, y + 1)], fill=(0, 0, 0, 60))
    img.alpha_composite(slats)
    return img


def glow_spot(w, h, cx, cy, r, color, strength):
    yy, xx = np.mgrid[0:h, 0:w]
    d = np.sqrt((xx - cx) ** 2 + ((yy - cy) * 1.15) ** 2) / r
    a = np.clip(1 - d, 0, 1) ** 2 * strength
    arr = np.zeros((h, w, 4), dtype=np.uint8)
    arr[..., :3] = color
    arr[..., 3] = (a * 255).astype(np.uint8)
    return Image.fromarray(arr, "RGBA")


def hazard(w, h, shift):
    img = Image.new("RGBA", (w, h), (24, 24, 28, 255))
    d = ImageDraw.Draw(img)
    period = 24
    for x in range(-period * 2, w + period * 2, period):
        x0 = x + shift
        d.polygon([(x0, h), (x0 + period // 2, h), (x0 + period // 2 + h, 0), (x0 + h, 0)], fill=YELLOW + (255,))
    d.line([(0, 0), (w, 0)], fill=(0, 0, 0, 255), width=2)
    return img


def beam(layer, a, b, strength):
    glow = Image.new("RGBA", layer.size)
    d = ImageDraw.Draw(glow)
    d.line([a, b], fill=(80, 230, 255, int(110 * strength)), width=11)
    glow = glow.filter(ImageFilter.GaussianBlur(4))
    layer.alpha_composite(glow)
    d = ImageDraw.Draw(layer)
    d.line([a, b], fill=(120, 240, 255, int(230 * strength)), width=4)
    d.line([a, b], fill=(235, 255, 255, int(255 * strength)), width=2)
    for k in range(5):
        ang = (k * 72 + strength * 50) * math.pi / 180
        r = 3 + (k % 3)
        d.rectangle([b[0] + math.cos(ang) * r * 2 - 1, b[1] + math.sin(ang) * r * 2 - 1,
                     b[0] + math.cos(ang) * r * 2 + 1, b[1] + math.sin(ang) * r * 2 + 1], fill=(255, 255, 220, 230))


def emitter(layer, x, y):
    d = ImageDraw.Draw(layer)
    d.rectangle([x - 14, y - 10, x + 14, y + 6], fill=(58, 62, 72, 255), outline=(16, 18, 22, 255), width=2)
    d.rectangle([x - 6, y + 6, x + 6, y + 12], fill=(30, 34, 40, 255), outline=(16, 18, 22, 255))
    d.rectangle([x - 4, y + 8, x + 4, y + 12], fill=(130, 240, 255, 255))


def title(text, scale, outline=4):
    """Big pixel title: gold gradient, dark outline and a deep drop for a chunky look."""
    w = mcfont.width(text, scale) + outline * 2 + scale * 2
    h = 8 * scale + outline * 2 + scale * 2
    base = Image.new("RGBA", (w, h))
    mcfont.draw(base, (outline, outline), text, scale, shadow=None, top=(255, 238, 150), bottom=(236, 150, 34))
    a = base.split()[3]
    fat = a.filter(ImageFilter.MaxFilter(outline * 2 + 1))
    out = Image.new("RGBA", (w, h))
    drop = Image.new("RGBA", (w, h), (92, 46, 10, 255))
    out.paste(drop, (0, scale + 1), fat)
    out.paste(Image.new("RGBA", (w, h), (20, 14, 10, 255)), (0, 0), fat)
    out.alpha_composite(base)
    return out


def shine(img, t):
    """A bright diagonal glint that sweeps across the opaque pixels of img; t in 0..1."""
    w, h = img.size
    arr = np.asarray(img).astype(float)
    yy, xx = np.mgrid[0:h, 0:w]
    pos = -0.3 * w + t * 1.6 * w
    band = np.clip(1 - np.abs((xx + yy * 0.6) - pos) / 14, 0, 1)
    lit = (arr[..., 3] > 200) & (arr[..., :3].mean(axis=-1) > 110)
    arr[..., :3] += (band * lit)[..., None] * 120
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def golem_frames(size):
    frames, fixed = [], None
    for k in range(FRAMES):
        ph = 2 * math.pi * k / FRAMES
        yaw = 32 + 14 * math.sin(ph)
        pulse = 0.5 + 0.5 * math.sin(ph * 2)
        eye = lerp((210, 40, 30), (255, 110, 70), pulse)
        img, fixed = golem3d.render(BUILD, "iron", size, yaw, 12, glow_rgb=eye, fixed=fixed, paint=YELLOW)
        frames.append(img)
    return frames


def save_gif(frame_dir, out_path, fps):
    pal = os.path.join(frame_dir, "palette.png")
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i",
                    os.path.join(frame_dir, "f%03d.png"), "-vf", "palettegen=max_colors=255:stats_mode=full", pal],
                   check=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-framerate", str(fps), "-i",
                    os.path.join(frame_dir, "f%03d.png"), "-i", pal, "-lavfi",
                    "paletteuse=dither=none", "-loop", "0", out_path], check=True)


def logo():
    W = H = 400
    gol = golem_frames(128)
    ttl = title("GOLEM GARAGE", 4)
    fd = os.path.join(OUT, "logo_frames")
    os.makedirs(fd, exist_ok=True)
    for k in range(FRAMES):
        ph = 2 * math.pi * k / FRAMES
        img = backdrop(W, H, 0, 300)
        img.alpha_composite(glow_spot(W, H, 200, 170, 170, (90, 200, 255), 0.35 + 0.08 * math.sin(ph)))
        # Floor shadow.
        sh = Image.new("RGBA", (W, H))
        ImageDraw.Draw(sh).ellipse([110, 268, 290, 296], fill=(0, 0, 0, 120))
        img.alpha_composite(sh.filter(ImageFilter.GaussianBlur(5)))
        g = gol[k].resize((256, 256), Image.NEAREST)
        img.alpha_composite(g, (72, 36))
        # Lasers scan up and down the golem.
        lay = Image.new("RGBA", (W, H))
        sy = 160 + 70 * math.sin(ph)
        sy2 = 160 + 70 * math.sin(ph + math.pi * 0.8)
        beam(lay, (34, 22), (170, sy), 0.85 + 0.15 * math.sin(ph * 3))
        beam(lay, (366, 22), (232, sy2), 0.85 + 0.15 * math.cos(ph * 3))
        img.alpha_composite(lay)
        emitter(img, 34, 16)
        emitter(img, 366, 16)
        # Title and stripes.
        plate = Image.new("RGBA", (W, 74), (10, 12, 16, 210))
        img.alpha_composite(plate, (0, 296))
        t = shine(ttl, (k / FRAMES) * 1.4 - 0.2)
        img.alpha_composite(t, ((W - ttl.size[0]) // 2, 302))
        img.alpha_composite(hazard(W, 30, (k * 24 // FRAMES) % 24 - 24), (0, 370))
        img.convert("RGB").save(os.path.join(fd, "f%03d.png" % k))
        if k == 0:
            img.convert("RGB").save(os.path.join(OUT, "logo.png"))
    save_gif(fd, os.path.join(OUT, "logo.gif"), 12)


def header():
    W, H = 800, 220
    gol = golem_frames(96)
    ttl = title("GOLEM GARAGE", 6)
    sub = Image.new("RGBA", (mcfont.width("Build them. Teach them. Fix them.", 2) + 4, 20))
    mcfont.draw(sub, (0, 0), "Build them. Teach them. Fix them.", 2, color=(214, 224, 238), shadow=(20, 24, 32))
    fd = os.path.join(OUT, "header_frames")
    os.makedirs(fd, exist_ok=True)
    for k in range(FRAMES):
        ph = 2 * math.pi * k / FRAMES
        img = backdrop(W, H, 0, H - 20)
        img.alpha_composite(glow_spot(W, H, 140, 100, 150, (90, 200, 255), 0.35 + 0.08 * math.sin(ph)))
        img.alpha_composite(glow_spot(W, H, 500, 95, 300, (255, 190, 70), 0.10))
        g = gol[k].resize((192, 192), Image.NEAREST)
        img.alpha_composite(g, (44, 4))
        lay = Image.new("RGBA", (W, H))
        sy = 100 + 50 * math.sin(ph)
        beam(lay, (16, 10), (118, sy), 0.85 + 0.15 * math.sin(ph * 3))
        beam(lay, (262, 10), (164, 100 + 50 * math.sin(ph + 2.4)), 0.85 + 0.15 * math.cos(ph * 3))
        img.alpha_composite(lay)
        emitter(img, 16, 6)
        emitter(img, 262, 6)
        t = shine(ttl, (k / FRAMES) * 1.4 - 0.2)
        tx = 280 + (W - 280 - ttl.size[0]) // 2
        img.alpha_composite(t, (tx, 52))
        img.alpha_composite(sub, (280 + (W - 280 - sub.size[0]) // 2, 132))
        img.alpha_composite(hazard(W, 20, (k * 24 // FRAMES) % 24 - 24), (0, H - 20))
        img.convert("RGB").save(os.path.join(fd, "f%03d.png" % k))
    save_gif(fd, os.path.join(OUT, "header.gif"), 12)


if __name__ == "__main__":
    logo()
    header()
    print("logo and header written to", OUT)
