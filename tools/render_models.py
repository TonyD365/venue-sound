#!/usr/bin/env python3
"""Renders the real block models (the JSON files in assets/venuesound/models/block) to docs/images/models.png.

A small software renderer: every element of a model is drawn face by face from its own geometry and UVs,
seen from the front (north), a little from above and from the right, the way you see a device you face in game.
"""
import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/venuesound"
P = 14            # screen pixels per model unit (1/16 block)
KX, KY = 0.5, 0.42  # how far depth shifts right / up

def texture(ref):
    name = ref.split(":")[1]
    return Image.open(ASSETS / "textures" / f"{name}.png").convert("RGBA")

def load_model(name):
    m = json.loads((ASSETS / "models/block" / f"{name}.json").read_text())
    tex = {k: v for k, v in m.get("textures", {}).items()}
    if m.get("parent") == "block/orientable":       # vanilla cube: front on north, sides, top
        els = [{"from": [0, 0, 0], "to": [16, 16, 16], "faces": {
            "north": {"texture": "#front"}, "west": {"texture": "#side"}, "east": {"texture": "#side"},
            "south": {"texture": "#side"}, "up": {"texture": "#top"}, "down": {"texture": "#top"}}}]
    else:
        els = m["elements"]
    return tex, els

def resolve(tex, ref):
    while ref.startswith("#"):
        ref = tex[ref[1:]]
    return texture(ref)

def proj(x, y, z, oy=0, ox=0):
    # viewer stands north of the block, slightly up and to the west (screen right = west)
    return ((16 - x + ox) + KX * z) * P, ((32 - y - oy) - KY * z) * P

def shade(c, f):
    return (int(c[0] * f), int(c[1] * f), int(c[2] * f), c[3])

def draw_face(dr, img_tex, uv, corners, f):
    """corners: 3D points for (u0,v0), (u1,v0), (u1,v1), (u0,v1) of the face."""
    u0, v0, u1, v1 = uv
    tw, th = img_tex.size
    px = img_tex.load()
    nu = max(1, round(abs(u1 - u0))); nv = max(1, round(abs(v1 - v0)))
    a, b, c, d = corners
    lerp = lambda p, q, t: tuple(p[i] + (q[i] - p[i]) * t for i in range(2))
    for i in range(nu):
        for j in range(nv):
            s0, s1, t0, t1 = i / nu, (i + 1) / nu, j / nv, (j + 1) / nv
            tu = u0 + (u1 - u0) * (i + 0.5) / nu; tv = v0 + (v1 - v0) * (j + 0.5) / nv
            col = px[min(tw - 1, int(tu * tw / 16)), min(th - 1, int(tv * th / 16))]
            if col[3] < 10:
                continue
            def at(s, t):
                return lerp(lerp(a, b, s), lerp(d, c, s), t)
            dr.polygon([at(s0, t0), at(s1, t0), at(s1, t1), at(s0, t1)], fill=shade(col, f))

def render(models, size=(64 * P, 46 * P)):
    img = Image.new("RGBA", size, (0, 0, 0, 0))
    dr = ImageDraw.Draw(img)
    for entry in models:
        name, oy = entry[0], entry[1]
        ox = entry[2] if len(entry) > 2 else 0
        tex, els = load_model(name)
        # painter's order: far (south) first, then left-to-right on screen, low to high
        for e in sorted(els, key=lambda e: (-e["to"][2], e["from"][0], e["from"][1])):
            (x1, y1, z1), (x2, y2, z2) = e["from"], e["to"]
            fc = e["faces"]
            P3 = lambda x, y, z: proj(x, y, z, oy, ox)
            if "west" in fc:   # right side on screen
                t = fc["west"]; uv = t.get("uv", [z1, 16 - y2, z2, 16 - y1])
                draw_face(dr, resolve(tex, t["texture"]), uv, [P3(x1, y2, z1), P3(x1, y2, z2), P3(x1, y1, z2), P3(x1, y1, z1)], 0.72)
            if "up" in fc:
                t = fc["up"]; uv = t.get("uv", [x1, z1, x2, z2])
                draw_face(dr, resolve(tex, t["texture"]), uv, [P3(x1, y2, z1), P3(x2, y2, z1), P3(x2, y2, z2), P3(x1, y2, z2)], 1.1)
            if "north" in fc:  # the front
                t = fc["north"]; uv = t.get("uv", [16 - x2, 16 - y2, 16 - x1, 16 - y1])
                draw_face(dr, resolve(tex, t["texture"]), uv, [P3(x2, y2, z1), P3(x1, y2, z1), P3(x1, y1, z1), P3(x2, y1, z1)], 1.0)
    box = img.getbbox()
    return img.crop(box) if box else img

devices = [
    ("Wireless Receiver\n无线接收器", [("receiver", 0)]),
    ("Stand Microphone\n立式有线麦克风", [("stand_mic", 0)]),
    ("Media Deck\n媒体播放机", [("media_deck", 0)]),
    ("Wall Outlet\n墙面接口", [("outlet", 0)]),
    ("Mixing Console (3x2 blocks)\n调音台（3×2）", [("mixer_c2_r0", 0, 32), ("mixer_c1_r0", 0, 16), ("mixer_c0_r0", 0, 0)]),
    ("Power Amplifier\n功放机", [("amplifier", 0)]),
    ("Line Array Module\n线阵音箱模块", [("speaker", 0)]),
    ("PA Speaker (2 blocks)\n大型 PA 音箱（两格高）", [("pa_speaker_lower", 0), ("pa_speaker_upper", 16)]),
]
items = [("Wireless Microphone\n无线麦克风", "wireless_mic"), ("Audio Cable\n音频线", "audio_cable")]

try:
    font = ImageFont.truetype("/System/Library/Fonts/Hiragino Sans GB.ttc", 22)
    title = ImageFont.truetype("/System/Library/Fonts/Hiragino Sans GB.ttc", 32)
except OSError:
    font = title = ImageFont.load_default()

cells = [(n, render(m)) for n, m in devices]
cells += [(n, Image.open(ASSETS / "textures/item" / f"{t}.png").convert("RGBA").resize((16 * 14, 16 * 14), Image.NEAREST)) for n, t in items]
CW, CH, COLS = 330, 420, 5
rows = (len(cells) + COLS - 1) // COLS
sheet = Image.new("RGBA", (CW * COLS, 70 + CH * rows), (58, 62, 70, 255))
d = ImageDraw.Draw(sheet)
d.text((24, 16), "VenueSound — block models / 方块模型", fill=(232, 184, 74), font=title)
for i, (name, im) in enumerate(cells):
    cx, cy = (i % COLS) * CW, 70 + (i // COLS) * CH
    sc = min(290 / im.width, 320 / im.height, 1.0)
    im2 = im.resize((max(1, int(im.width * sc)), max(1, int(im.height * sc))), Image.NEAREST)
    sheet.alpha_composite(im2, (cx + (CW - im2.width) // 2, cy + 330 - im2.height))
    d.multiline_text((cx + CW // 2, cy + 345), name, fill=(240, 240, 240), font=font, anchor="ma", align="center")
(ROOT / "docs/images").mkdir(exist_ok=True)
sheet.save(ROOT / "docs/images/models.png")
print("wrote docs/images/models.png")
