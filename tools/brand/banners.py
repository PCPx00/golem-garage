"""
Animated section banners (800x260 GIFs) cut from in-game footage recorded by
tools/testworld/record_trailer.sh, with a caption in Minecraft's font and a moving hazard stripe.
Usage: python3 banners.py <footage dir> <out dir>
"""
import os
import shutil
import subprocess
import sys

from PIL import Image

import mcfont
from logo import hazard, save_gif, title

SRC, OUT = sys.argv[1], sys.argv[2]
W, H = 800, 260
FPS = 10

# name, footage, start s, length s, crop top (in the 1280x720 frame), title, subtitle
BANNERS = [
    ("build", "lineup", 3.0, 4.2, 196, "BUILD YOUR GOLEMS", "Every part is a shape and a material."),
    ("jobs", "farm", 2.0, 4.4, 250, "GIVE THEM JOBS", "Simple rules: farm, haul, guard and more."),
    ("garage", "garage", 3.0, 4.2, 210, "THE GARAGE", "Build the room. The lasers fix the golem."),
    ("cubs", "cubs", 3.0, 4.6, 236, "CUBS & OUTFITS", "Tiny helpers, dressed cool, goth or cute."),
]


def plate():
    """A dark fade on the left so the caption reads over any footage."""
    img = Image.new("RGBA", (W, H))
    for x in range(W):
        a = 200 if x < 200 else max(0, int(200 * (1 - (x - 200) / 300)))
        img.paste((10, 12, 18, a), (x, 0, x + 1, H))
    return img


def build(name, src, start, length, top, head, sub):
    tmp = os.path.join(OUT, name + "_raw")
    shutil.rmtree(tmp, ignore_errors=True)
    os.makedirs(tmp)
    crop_h = int(1280 * H / W)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-ss", str(start), "-t", str(length), "-i",
                    os.path.join(SRC, src + ".mp4"), "-vf",
                    "fps=%d,crop=1280:%d:0:%d,scale=%d:%d:flags=lanczos" % (FPS, crop_h, top, W, H),
                    os.path.join(tmp, "r%03d.png")], check=True)
    raw = sorted(os.listdir(tmp))
    order = raw + raw[-2:0:-1]  # forward then back, so the loop has no jump
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
    for b in BANNERS:
        build(*b)
        print("banner", b[0], os.path.getsize(os.path.join(OUT, "banner_%s.gif" % b[0])) // 1024, "KB")
