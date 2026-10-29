"""
The animated header (800x220) and section banners (800x260 GIFs), cut from in-game footage recorded by
tools/testworld/record_trailer.sh (live_client.sh banners for the night-time wasteland set), with captions in
Minecraft's font and a moving hazard stripe.
Usage: python3 banners.py <footage dir> <out dir>
"""
import os
import shutil
import subprocess
import sys

from PIL import Image

import mcfont
from logo import hazard, save_gif, shine, title

SRC, OUT = sys.argv[1], sys.argv[2]
W, H = 800, 260
FPS = 10

# name, footage, start s, length s, crop top (in the 1280x720 frame), title, subtitle
BANNERS = [
    ("build", "lineup", 4.0, 4.2, 190, "BUILD YOUR GOLEMS", "Every part is a shape and a material."),
    ("jobs", "farm", 4.4, 4.4, 230, "GIVE THEM JOBS", "Simple rules: farm, haul, guard and more."),
    ("garage", "garage", 3.0, 4.2, 110, "THE GARAGE", "Build the room. It becomes a wasteland workshop."),
    ("cubs", "cubs", 4.0, 4.6, 250, "CUBS & OUTFITS", "Tiny helpers, dressed cool, goth or cute."),
]


def plate(w=W, h=H, solid=200, fade=300):
    """A dark fade on the left so the caption reads over any footage."""
    img = Image.new("RGBA", (w, h))
    for x in range(w):
        a = 200 if x < solid else max(0, int(200 * (1 - (x - solid) / fade)))
        img.paste((10, 12, 18, a), (x, 0, x + 1, h))
    return img


def clip(src, start, length, top, w, h, tmp, crop_w=1280, left=0):
    """Cuts a crop_w-wide band from top (and left) of the 1280x720 footage, scaled to w x h."""
    shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp)
    crop_h = int(crop_w * h / w)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", str(start), "-t", str(length), "-i",
                    os.path.join(SRC, src + ".mp4"), "-vf",
                    "fps=%d,crop=%d:%d:%d:%d,scale=%d:%d:flags=lanczos" % (FPS, crop_w, crop_h, left, top, w, h),
                    os.path.join(tmp, "r%03d.png")], check=True)
    raw = sorted(os.listdir(tmp))
    return raw, raw + raw[-2:0:-1]  # forward then back, so the loop has no jump


def header(src="header", start=4.0, length=4.4, top=200, crop_w=1060, left=0):
    """The main header: the finished Garage at night, with the title glinting over it."""
    hw, hh = 800, 220
    tmp = os.path.join(OUT, "header_raw")
    raw, order = clip(src, start, length, top, hw, hh, tmp, crop_w, left)
    ttl = title("GOLEM GARAGE", 5, outline=3)
    sub_text = "Build them. Teach them. Fix them."
    sub = Image.new("RGBA", (mcfont.width(sub_text, 2) + 4, 20))
    mcfont.draw(sub, (0, 0), sub_text, 2, color=(222, 230, 240), shadow=(18, 20, 26))
    fade = plate(hw, hh, solid=330, fade=260)
    fd = os.path.join(OUT, "header_frames")
    shutil.rmtree(fd, ignore_errors=True)
    os.makedirs(fd)
    for k, fname in enumerate(order):
        img = Image.open(os.path.join(tmp, fname)).convert("RGBA")
        img.alpha_composite(fade)
        img.alpha_composite(shine(ttl, (k / len(order)) * 1.4 - 0.2), (24, 58))
        img.alpha_composite(sub, (30, 58 + ttl.size[1] + 12))
        img.alpha_composite(hazard(hw, 12, (k * 3) % 24 - 24), (0, hh - 12))
        img.convert("RGB").save(os.path.join(fd, "f%03d.png" % k))
    save_gif(fd, os.path.join(OUT, "header.gif"), FPS)
    Image.open(os.path.join(fd, "f%03d.png" % (len(raw) // 2))).save(os.path.join(OUT, "header.png"))
    shutil.rmtree(tmp)


def build(name, src, start, length, top, head, sub):
    tmp = os.path.join(OUT, name + "_raw")
    raw, order = clip(src, start, length, top, W, H, tmp)
    head_img = title(head, 3, outline=3)
    sub_img = Image.new("RGBA", (mcfont.width(sub, 2) + 4, 20))
    mcfont.draw(sub_img, (0, 0), sub, 2, color=(222, 230, 240), shadow=(18, 20, 26))
    fade = plate()
    fd = os.path.join(OUT, name + "_frames")
    shutil.rmtree(fd, ignore_errors=True)
    os.makedirs(fd)
    for k, fname in enumerate(order):
        img = Image.open(os.path.join(tmp, fname)).convert("RGBA")
        img.alpha_composite(fade)
        img.alpha_composite(head_img, (24, 84))
        img.alpha_composite(sub_img, (30, 84 + head_img.size[1] + 8))
        img.alpha_composite(hazard(W, 10, (k * 3) % 24 - 24), (0, H - 10))
        img.convert("RGB").save(os.path.join(fd, "f%03d.png" % k))
    save_gif(fd, os.path.join(OUT, "banner_%s.gif" % name), FPS)
    Image.open(os.path.join(fd, "f%03d.png" % (len(raw) // 2))).save(os.path.join(OUT, "banner_%s.png" % name))
    shutil.rmtree(tmp)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    only = sys.argv[3:]  # optional banner names (or "header"), to remake just those
    if not only or "header" in only:
        header()
        print("header", os.path.getsize(os.path.join(OUT, "header.gif")) // 1024, "KB")
    for b in BANNERS:
        if only and b[0] not in only:
            continue
        build(*b)
        print("banner", b[0], os.path.getsize(os.path.join(OUT, "banner_%s.gif" % b[0])) // 1024, "KB")
