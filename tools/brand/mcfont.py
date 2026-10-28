"""Draws text in Minecraft's own pixel font (ascii.png from the game jar), at any whole-number scale."""
import os

from PIL import Image

# Extract assets/minecraft/textures/font/ascii.png from the Minecraft client jar and point MC_ASCII at it.
FONT = os.environ.get("MC_ASCII", "/tmp/golemgarage/ascii.png")
_img = Image.open(FONT).convert("RGBA")
_glyphs = {}


def _glyph(ch):
    if ch in _glyphs:
        return _glyphs[ch]
    code = ord(ch)
    if code > 255:
        code = ord("?")
    gx, gy = (code % 16) * 8, (code // 16) * 8
    g = _img.crop((gx, gy, gx + 8, gy + 8))
    a = g.split()[3]
    w = 0
    for x in range(8):
        if any(a.getpixel((x, y)) > 0 for y in range(8)):
            w = x + 1
    if ch == " ":
        w = 3
    _glyphs[ch] = (g, w)
    return _glyphs[ch]


def width(text, scale=1):
    return sum(_glyph(c)[1] + 1 for c in text) * scale - scale


def draw(canvas, xy, text, scale=2, color=(255, 255, 255), shadow=(63, 63, 63), shadow_off=None, top=None, bottom=None):
    """Draws text with a drop shadow. top/bottom give a vertical colour gradient instead of a flat colour."""
    x0, y0 = xy
    off = scale if shadow_off is None else shadow_off
    for pass_ in ("shadow", "fill"):
        x = x0
        for c in text:
            g, w = _glyph(c)
            mask = g.split()[3].resize((8 * scale, 8 * scale), Image.NEAREST)
            if pass_ == "shadow":
                if shadow is not None:
                    canvas.paste(Image.new("RGBA", mask.size, shadow + (255,)), (x + off, y0 + off), mask)
            else:
                if top is not None:
                    fill = Image.new("RGBA", mask.size)
                    for yy in range(mask.size[1]):
                        t = min(1.0, yy / max(1, 7 * scale))
                        col = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3)) + (255,)
                        fill.paste(col, (0, yy, mask.size[0], yy + 1))
                else:
                    fill = Image.new("RGBA", mask.size, tuple(color) + (255,))
                canvas.paste(fill, (x, y0), mask)
            x += (w + 1) * scale
    return x
