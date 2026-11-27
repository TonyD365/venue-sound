#!/usr/bin/env python3
"""Renders docs/images/banner.png: a stage with two PA cabinets, a console and the project name."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parent.parent
TEX = ROOT / "src/main/resources/assets/venuesound/textures/block"
W, H = 1280, 400
img = Image.new("RGBA", (W, H), (14, 15, 18, 255))
d = ImageDraw.Draw(img)

# light beams
beams = Image.new("RGBA", (W, H), (0, 0, 0, 0))
bd = ImageDraw.Draw(beams)
for x, col in ((240, (232, 184, 74, 60)), (640, (61, 220, 132, 45)), (1040, (52, 152, 219, 55))):
    bd.polygon([(x - 10, 0), (x + 10, 0), (x + 180, H), (x - 180, H)], fill=col)
img.alpha_composite(beams.filter(ImageFilter.GaussianBlur(18)))
# stage floor
d.rectangle((0, 330, W, H), fill=(30, 22, 16, 255))
for x in range(0, W, 48):
    d.line((x, 330, x, H), fill=(42, 31, 22, 255))

def tex(n, s):
    return Image.open(TEX / f"{n}.png").convert("RGBA").resize((16 * s, 16 * s), Image.NEAREST)

s = 9
for x in (70, W - 70 - 16 * s):                    # two PA cabinets
    img.alpha_composite(tex("pa_front_upper", s), (x, 330 - 32 * s))
    img.alpha_composite(tex("pa_front_lower", s), (x, 330 - 16 * s))
console = tex("mixer_top", 7).resize((16 * 7, 8 * 7), Image.NEAREST)
img.alpha_composite(console, (W // 2 - 56, 300))

try:
    big = ImageFont.truetype("/System/Library/Fonts/Hiragino Sans GB.ttc", 96)
    small = ImageFont.truetype("/System/Library/Fonts/Hiragino Sans GB.ttc", 28)
except OSError:
    big = small = ImageFont.load_default()
d.text((W // 2, 120), "VenueSound", fill=(255, 255, 255), font=big, anchor="mm")
d.text((W // 2, 205), "mics · receivers · mixing console · amplifiers · PA speakers", fill=(232, 184, 74), font=small, anchor="mm")
d.text((W // 2, 245), "场馆扩声系统 · Minecraft 1.20.1 Fabric · Simple Voice Chat", fill=(200, 200, 200), font=small, anchor="mm")
img.convert("RGB").save(ROOT / "docs/images/banner.png", optimize=True)
print("wrote docs/images/banner.png")
