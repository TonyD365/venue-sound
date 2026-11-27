#!/usr/bin/env python3
"""Generates VenueSound's 16x16 pixel-art textures and the mod icon. Run from the repo root."""
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/venuesound"
BLOCK = ROOT / "textures/block"
ITEM = ROOT / "textures/item"
BLOCK.mkdir(parents=True, exist_ok=True)
ITEM.mkdir(parents=True, exist_ok=True)

def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)

BODY, BODY_D, BODY_L = hexc("2b2e33"), hexc("1c1e22"), hexc("3d4148")
EDGE = hexc("50555c")

def base(fill=BODY):
    img = Image.new("RGBA", (16, 16), fill)
    px = img.load()
    for i in range(16):
        px[i, 0] = EDGE; px[0, i] = EDGE
        px[i, 15] = BODY_D; px[15, i] = BODY_D
    return img

def rect(img, x0, y0, x1, y1, c):
    px = img.load()
    for x in range(x0, x1 + 1):
        for y in range(y0, y1 + 1):
            px[x, y] = c

# ---------- receiver
r = base()
rect(r, 2, 4, 9, 8, hexc("0b2a12"))          # LCD
rect(r, 3, 5, 8, 7, hexc("3ddc84"))          # LCD glow
rect(r, 4, 6, 5, 6, hexc("0b2a12")); rect(r, 7, 5, 7, 7, hexc("0b2a12"))  # digits
rect(r, 11, 5, 12, 6, hexc("9aa0a6")); rect(r, 11, 9, 12, 10, hexc("9aa0a6"))  # knobs
rect(r, 2, 11, 2, 11, hexc("ff4d4d")); rect(r, 4, 11, 4, 11, hexc("3ddc84"))   # LEDs
rect(r, 2, 13, 13, 13, BODY_L)
r.save(BLOCK / "receiver_front.png")
base().save(BLOCK / "receiver_side.png")
t = base(); rect(t, 3, 11, 3, 11, hexc("111111")); rect(t, 12, 11, 12, 11, hexc("111111")); t.save(BLOCK / "receiver_top.png")
ant = Image.new("RGBA", (16, 16), hexc("1a1a1a")); ant.save(BLOCK / "antenna.png")

# ---------- mixer
m = base(hexc("23262b"))
colors = ["e74c3c", "e67e22", "f1c40f", "2ecc71", "1abc9c", "3498db", "9b59b6", "ecf0f1"]
for i in range(7):
    x = 1 + i * 2
    rect(m, x, 1, x, 1, hexc(colors[i]))            # knob caps
    rect(m, x, 3, x, 3, hexc("9aa0a6"))
    rect(m, x, 5, x, 13, hexc("0e0f11"))            # fader slot
    y = 7 + (i * 3) % 5
    rect(m, x, y, x, y + 1, hexc("d5d8dc"))         # fader cap
rect(m, 15, 5, 15, 13, hexc("0e0f11")); rect(m, 15, 7, 15, 8, hexc("e8b84a"))  # master
rect(m, 15, 1, 15, 3, hexc("3ddc84"))               # meter
m.save(BLOCK / "mixer_top.png")
mf = base(); rect(mf, 1, 6, 14, 6, hexc("e8b84a"))
for x in (3, 6, 9, 12):
    rect(mf, x, 9, x, 10, hexc("111111"))           # XLR inputs
mf.save(BLOCK / "mixer_front.png")
base().save(BLOCK / "mixer_side.png")

# ---------- speaker (line array module)
s = Image.new("RGBA", (16, 16), hexc("141517"))
px = s.load()
for x in range(16):
    for y in range(16):
        if (x + y) % 2 == 0:
            px[x, y] = hexc("1e2023")
for x in range(16):
    px[x, 0] = EDGE; px[x, 15] = BODY_D
for y in range(16):
    px[0, y] = EDGE; px[15, y] = BODY_D
for cx in (4, 11):                                  # two woofers
    for x in range(cx - 2, cx + 3):
        for y in range(4, 9):
            if (x - cx) ** 2 + (y - 6) ** 2 <= 5:
                px[x, y] = hexc("2c2f34")
    px[cx, 6] = hexc("5a5f66")
rect(s, 3, 11, 12, 12, hexc("3a3d42"))              # horn
rect(s, 6, 11, 9, 12, hexc("5a5f66"))
rect(s, 13, 13, 13, 13, hexc("3498db"))             # power LED
s.save(BLOCK / "speaker_front.png")
ss = base(); rect(ss, 2, 2, 3, 13, hexc("7f8c8d")); rect(ss, 12, 2, 13, 13, hexc("7f8c8d")); ss.save(BLOCK / "speaker_side.png")
st = base(); rect(st, 3, 3, 12, 4, hexc("7f8c8d")); rect(st, 3, 11, 12, 12, hexc("7f8c8d")); st.save(BLOCK / "speaker_top.png")

# ---------- wireless mic (item)
mic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
p = mic.load()
for i in range(9):                                   # body, diagonal
    x, y = 3 + i, 12 - i
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        p[x + dx - 1, y + dy] = hexc("1c1e22") if i > 1 else hexc("2b2e33")
for x in range(10, 15):                               # grille head
    for y in range(1, 6):
        if (x - 12) ** 2 + (y - 3) ** 2 <= 5:
            p[x, y] = hexc("c9cdd2") if (x + y) % 2 else hexc("9aa0a6")
p[6, 9] = hexc("3ddc84")                              # on LED
p[2, 13] = hexc("e8b84a")                             # antenna tip
mic.save(ITEM / "wireless_mic.png")

# ---------- audio cable (item)
cab = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
p = cab.load()
import math
for a in range(0, 360, 6):
    for rr in (4, 5):
        x = int(round(8 + rr * math.cos(math.radians(a))))
        y = int(round(8 + rr * math.sin(math.radians(a))))
        p[x, y] = hexc("1c1e22") if rr == 5 else hexc("2b2e33")
for y in range(1, 5):
    p[13, y] = hexc("2b2e33")
for x in range(12, 15):
    for y in range(0, 2):
        p[x, y] = hexc("9aa0a6")                     # XLR plug
p[1, 13] = hexc("9aa0a6"); p[2, 13] = hexc("9aa0a6"); p[2, 12] = hexc("1c1e22")
cab.save(ITEM / "audio_cable.png")

# ---------- icon (128x128): mixer top scaled up on a gold frame
icon = Image.new("RGBA", (128, 128), hexc("e8b84a"))
icon.paste(m.resize((112, 112), Image.NEAREST), (8, 8))
icon.save(ROOT / "icon.png")
print("textures written")
