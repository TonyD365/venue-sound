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

# ================================================================== v0.2 devices

# ---------- wall outlet (stage box plate)
o = Image.new("RGBA", (16, 16), hexc("9aa0a6"))
po = o.load()
for i in range(16):
    po[i, 0] = hexc("c9cdd2"); po[0, i] = hexc("c9cdd2"); po[i, 15] = hexc("6b7178"); po[15, i] = hexc("6b7178")
for cx, cy in ((5, 5), (10, 5), (5, 10), (10, 10)):   # four XLR sockets
    for x in range(cx - 2, cx + 2):
        for y in range(cy - 2, cy + 2):
            po[x, y] = hexc("1c1e22")
    po[cx - 1, cy - 1] = hexc("5a5f66")
rect(o, 3, 13, 12, 13, hexc("e8b84a"))                # label strip
o.save(BLOCK / "outlet_front.png")
Image.new("RGBA", (16, 16), hexc("7f8c8d")).save(BLOCK / "outlet_side.png")

# ---------- stand mic
Image.new("RGBA", (16, 16), hexc("1c1e22")).save(BLOCK / "stand_metal.png")
head = Image.new("RGBA", (16, 16), hexc("9aa0a6"))
ph = head.load()
for x in range(16):
    for y in range(16):
        if (x + y) % 2 == 0:
            ph[x, y] = hexc("c9cdd2")
head.save(BLOCK / "mic_head.png")

# ---------- amplifier (rack power amp)
a = base(hexc("1f2124"))
rect(a, 1, 2, 14, 2, hexc("5a5f66"))
rect(a, 2, 4, 7, 9, hexc("0e0f11"))                   # ventilation grille
for y in (5, 7):
    rect(a, 2, y, 7, y, hexc("2b2e33"))
for x, c in ((9, "3ddc84"), (11, "3ddc84"), (13, "e74c3c")):
    rect(a, x, 4, x, 4, hexc(c))                       # LEDs
rect(a, 9, 7, 10, 8, hexc("c9cdd2")); rect(a, 12, 7, 13, 8, hexc("c9cdd2"))  # gain knobs
rect(a, 2, 12, 13, 12, hexc("e8b84a"))                # brand strip
rect(a, 1, 1, 1, 14, hexc("7f8c8d")); rect(a, 14, 1, 14, 14, hexc("7f8c8d"))  # rack ears
a.save(BLOCK / "amplifier_front.png")
base(hexc("1f2124")).save(BLOCK / "amplifier_side.png")
at = base(hexc("1f2124"))
for y in range(3, 13, 2):
    rect(at, 3, y, 12, y, hexc("111111"))
at.save(BLOCK / "amplifier_top.png")

# ---------- media deck
dk = base(hexc("23262b"))
rect(dk, 2, 3, 9, 6, hexc("0b2a12")); rect(dk, 3, 4, 8, 5, hexc("3ddc84"))   # display
rect(dk, 11, 3, 13, 5, hexc("c9cdd2"))                                        # jog
for x, c in ((2, "3498db"), (5, "2ecc71"), (8, "e74c3c")):
    rect(dk, x, 9, x + 1, 10, hexc(c))                                        # transport buttons
rect(dk, 2, 12, 13, 13, hexc("111111"))                                       # disc slot
dk.save(BLOCK / "media_deck_front.png")
dt = base(hexc("23262b"))
for x in range(16):
    for y in range(16):
        if (x - 7.5) ** 2 + (y - 7.5) ** 2 <= 30:
            dt.load()[x, y] = hexc("3a3d42")
dt.load()[7, 7] = hexc("c9cdd2")
dt.save(BLOCK / "media_deck_top.png")

# ---------- big PA cabinet (2 blocks tall)
def grille(img):
    p = img.load()
    for x in range(1, 15):
        for y in range(1, 15):
            p[x, y] = hexc("16171a") if (x + y) % 2 else hexc("202226")

low = Image.new("RGBA", (16, 16), hexc("101113"))
grille(low)
pl = low.load()
for cy in (4, 11):                                     # two 15" woofers
    for x in range(16):
        for y in range(16):
            dd = (x - 7.5) ** 2 + (y - cy) ** 2
            if dd <= 14:
                pl[x, y] = hexc("26282c")
            if dd <= 5:
                pl[x, y] = hexc("33363b")
            if dd <= 1.2:
                pl[x, y] = hexc("5a5f66")
for i in range(16):
    pl[i, 15] = hexc("0a0a0b"); pl[0, i] = hexc("2b2e33"); pl[15, i] = hexc("0a0a0b")
low.save(BLOCK / "pa_front_lower.png")

up = Image.new("RGBA", (16, 16), hexc("101113"))
grille(up)
pu = up.load()
for x in range(2, 14):                                 # horn flare
    for y in range(3, 11):
        if abs(x - 7.5) <= 2 + (y - 3) * 0.6:
            pu[x, y] = hexc("2c2f34")
rect(up, 6, 3, 9, 5, hexc("0b0b0c"))                   # horn throat
rect(up, 4, 12, 11, 13, hexc("c9cdd2"))                # logo plate
rect(up, 5, 12, 10, 12, hexc("e8b84a"))
for i in range(16):
    pu[i, 0] = hexc("2b2e33"); pu[0, i] = hexc("2b2e33"); pu[15, i] = hexc("0a0a0b")
up.save(BLOCK / "pa_front_upper.png")

side = Image.new("RGBA", (16, 16), hexc("1c1e22"))
ps = side.load()
for x in range(16):
    for y in range(16):
        if (x * 7 + y * 3) % 11 == 0:
            ps[x, y] = hexc("232529")                  # textured paint
rect(side, 5, 6, 10, 7, hexc("0a0a0b"))               # handle
rect(side, 6, 7, 9, 7, hexc("5a5f66"))
side.save(BLOCK / "pa_side.png")
pt = base(hexc("1c1e22")); rect(pt, 6, 6, 9, 9, hexc("7f8c8d")); pt.save(BLOCK / "pa_top.png")   # pole mount
print("v0.2 textures written")

# ================================================================== v0.3 control surfaces
# Layout must match block/MixerLayout.java (panel units: u across both halves, depth from front).
R = 4  # texture pixels per panel unit on the 64x64 console top

def strip_u(i):
    return 2.5 + 3.0 * i

def console_top(half):
    img = Image.new("RGBA", (64, 64), hexc("23262b"))
    p = img.load()
    def put(u, d, c):           # u in console units (0-32), d depth
        lu = u - 16 * half
        if 0 <= lu < 16 and 0 <= d < 16:
            x = int((16 - lu) * R) - 1
            y = int(d * R)
            if 0 <= x < 64 and 0 <= y < 64:
                p[x, y] = c
    def fill(u0, u1, d0, d1, c):
        uu = u0
        while uu < u1:
            dd = d0
            while dd < d1:
                put(uu, dd, c)
                dd += 1 / R
            uu += 1 / R
    centers = [strip_u(i) for i in range(8)] + [28.5]
    for i, cu in enumerate(centers):
        fill(cu - 1.45, cu + 1.45, 0.3, 11.95, hexc("2b2e33") if i % 2 == 0 else hexc("272a2f"))   # strip panel
        fill(cu - 0.12, cu + 0.12, 1.0, 7.4, hexc("0a0a0b"))                                       # fader slot
        for k in range(7):                                                                         # scale ticks
            dd = 1.2 + k * 1.0
            fill(cu - 0.9, cu - 0.45, dd, dd + 0.12, hexc("6b7178"))
        fill(cu + 0.45, cu + 1.1, 4.17, 4.3, hexc("e8b84a"))                                     # unity mark
        if i < 8:
            fill(cu - 0.75, cu + 0.75, 7.85, 9.25, hexc("1a1b1e"))                                 # mute well
            fill(cu - 1.0, cu + 1.0, 9.6, 11.4, hexc("1a1b1e"))                                    # knob well
            for n in range(i + 1):                                                                 # channel number dots
                fill(cu - 1.2 + n * 0.3, cu - 1.05 + n * 0.3, 0.45, 0.6, hexc("c9cdd2"))
    fill(25.4, 31.6, 0.3, 11.95, hexc("2e261c"))                                                   # master section
    fill(28.38, 28.62, 1.0, 7.4, hexc("0a0a0b"))
    fill(25.9, 26.7, 8.5, 11.4, hexc("111111"))                                                    # meter bezel
    fill(27.2, 31.2, 8.4, 11.6, hexc("0b2a12"))                                                    # screen
    fill(27.5, 30.9, 9.0, 9.2, hexc("3ddc84")); fill(27.5, 29.6, 9.8, 10.0, hexc("3ddc84"))
    fill(25.5, 31.5, 0.4, 0.7, hexc("e8b84a"))                                                     # brand strip
    return img

# ---------- 3x2 console (v0.4): top panel 48x32 units at 4 px/unit, cut into six 64x64 tiles
W_U, D_U = 48, 32
panel = Image.new("RGBA", (W_U * R, D_U * R), hexc("1e2024"))
pp = panel.load()
def pfill(u0, u1, d0, d1, c):
    for u in range(int(u0 * R), int(u1 * R)):
        for d in range(int(d0 * R), int(d1 * R)):
            if 0 <= u < W_U * R and 0 <= d < D_U * R:
                pp[u, d] = c
for i in range(10):
    cu = 2.2 + 3.5 * i
    pfill(cu - 1.7, cu + 1.7, 0.3, 31.7, hexc("2b2e33") if i % 2 == 0 else hexc("272a2f"))
    pfill(cu - 1.7, cu + 1.7, 9.9, 10.1, hexc("3d4148"))      # line above the fader section
    pfill(cu - 1.7, cu + 1.7, 22.5, 22.7, hexc("3d4148"))     # EQ section divider
    pfill(cu - 1.7, cu + 1.7, 29.1, 29.3, hexc("3d4148"))
pfill(36.6, 47.8, 0.3, 31.7, hexc("2e261c"))                  # master section
pfill(36.6, 47.8, 17.8, 18.0, hexc("4a3d2a"))
pfill(1.0, 35.6, 31.75, 31.95, hexc("e8b84a"))                # gold trim at the back edge
def tile(col, row):
    img = Image.new("RGBA", (64, 64))
    px = img.load()
    for x in range(64):
        for y in range(64):
            # up-face texture x = 16 - u_local (model x), y = depth_local
            u = col * 16 + (16 - (x + 0.5) / R)
            d = row * 16 + (y + 0.5) / R
            px[x, y] = pp[min(W_U * R - 1, int(u * R)), min(D_U * R - 1, int(d * R))]
    return img
for c in range(3):
    for r in range(2):
        tile(c, r).save(BLOCK / f"console_top_c{c}_r{r}.png")
cf2 = Image.new("RGBA", (16, 16), hexc("17181b"))
rect(cf2, 0, 0, 15, 0, hexc("3d4148"))
rect(cf2, 0, 7, 15, 7, hexc("e8b84a"))
cf2.save(BLOCK / "console_front.png")
# amplifier front: live switch, knob and LED are drawn by the renderer
a2 = base(hexc("1f2124"))
rect(a2, 1, 1, 1, 14, hexc("7f8c8d")); rect(a2, 14, 1, 14, 14, hexc("7f8c8d"))     # rack ears
for y in range(2, 5):
    rect(a2, 2, y, 13, y, hexc("111111") if y % 2 == 0 else hexc("1f2124"))        # vent
rect(a2, 2, 14, 13, 14, hexc("e8b84a"))                                             # brand strip
a2.save(BLOCK / "amplifier_front.png")

# media deck and receiver fronts: displays and buttons are drawn by the renderer
d2 = base(hexc("23262b")); rect(d2, 1, 1, 14, 1, hexc("e8b84a")); d2.save(BLOCK / "media_deck_front.png")
r2 = base(hexc("2b2e33")); r2.save(BLOCK / "receiver_front.png")
print("v0.3 textures written")

# ================================================================== v0.4 concert speaker (4 x 6 blocks front, 32 px per block)
CS = 32
front = Image.new("RGBA", (4 * CS, 6 * CS), hexc("0d0e10"))
fp = front.load()
for x in range(4 * CS):
    for y in range(6 * CS):
        if (x // 2 + y // 2) % 2 == 0:
            fp[x, y] = hexc("15161a")                       # grille mesh
def disc(cx, cy, r, ring, cone, cap):
    for x in range(int(cx - r - 2), int(cx + r + 3)):
        for y in range(int(cy - r - 2), int(cy + r + 3)):
            if 0 <= x < 4 * CS and 0 <= y < 6 * CS:
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                if d <= r + 1.5 and d > r - 1:
                    fp[x, y] = hexc(ring)
                elif d <= r - 1:
                    fp[x, y] = hexc(cone) if (int(d) % 6) else hexc("1d1f23")
                if d <= r * 0.28:
                    fp[x, y] = hexc(cap)
# top two blocks: four line-array horn modules
for m in range(4):
    y0 = m * 16
    for x in range(4, 4 * CS - 4):
        for y in range(y0 + 2, y0 + 14):
            fp[x, y] = hexc("1c1e22")
    for k in range(6):                                       # horn slots
        cx = 10 + k * 21
        for x in range(cx - 6, cx + 7):
            for y in range(y0 + 4, y0 + 12):
                w = 6 - abs(y - (y0 + 8)) * 0.4
                if abs(x - cx) <= w:
                    fp[x, y] = hexc("2c2f34") if abs(x - cx) > 1 else hexc("070708")
# middle two blocks: four 15" drivers, bottom two: two 18" subwoofers
for cx in (32, 96):
    for cy in (78, 114):
        disc(cx, cy, 16, "3d4148", "26282c", "5a5f66")
    disc(cx, 158, 28, "3d4148", "222428", "4a4e55")
for x in range(44, 84):                                     # logo plate
    for y in range(184, 190):
        fp[x, y] = hexc("c9cdd2")
for x in range(48, 80):
    for y in range(186, 188):
        fp[x, y] = hexc("e8b84a")
for x in range(4 * CS):                                     # frame
    for t in (0, 1):
        fp[x, t] = hexc("3d4148"); fp[x, 6 * CS - 1 - t] = hexc("3d4148")
for y in range(6 * CS):
    for t in (0, 1):
        fp[t, y] = hexc("3d4148"); fp[4 * CS - 1 - t, y] = hexc("3d4148")
for c in range(4):
    for l in range(6):
        # tile for column c (from the left, seen from the front) and level l (from the bottom)
        tile = front.crop((c * CS, (5 - l) * CS, (c + 1) * CS, (6 - l) * CS))
        tile.save(BLOCK / f"concert_front_c{c}_l{l}.png")
cs_side = Image.new("RGBA", (16, 16), hexc("17181b"))
for x in range(16):
    for y in range(16):
        if (x * 5 + y * 3) % 9 == 0:
            cs_side.putpixel((x, y), hexc("1d1f23"))
cs_side.save(BLOCK / "concert_side.png")
print("v0.4 concert textures written")

# ================================================================== inventory icons for multi-block devices
def icon_from(img, size=32):
    w, h = img.size
    k = min(size / w, size / h)
    small = img.resize((max(1, int(w * k)), max(1, int(h * k))), Image.LANCZOS)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(small, ((size - small.width) // 2, (size - small.height) // 2))
    return out

# console: the top panel seen from above, with fader caps and knob colours drawn in
con = panel.copy().transpose(Image.FLIP_TOP_BOTTOM)
cd = con.load()
for i in range(10):
    cu = int((2.2 + 3.5 * i) * R)
    for row, col in ((30.3, "c0392b"), (28.0, "2e86c1"), (25.8, "16a085"), (21.4, "f1c40f"), (17.0, "ecf0f1")):
        y = int((D_U - row) * R)
        for dx in range(-3, 4):
            for dy in range(-3, 4):
                if dx * dx + dy * dy <= 9 and 0 <= cu + dx < W_U * R and 0 <= y + dy < D_U * R:
                    cd[cu + dx, y + dy] = hexc(col)
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            cd[cu + dx, int((D_U - 6) * R) + dy] = hexc("d9dcE0")
for cu in (45.1, 47.0):
    for dx in range(-2, 3):
        for dy in range(-2, 3):
            cd[int(cu * R) + dx, int((D_U - 6) * R) + dy] = hexc("d0312b")
icon_from(con).save(ITEM / "mixer.png")
icon_from(front).save(ITEM / "concert_speaker.png")
pa = Image.new("RGBA", (16, 32))
pa.paste(Image.open(BLOCK / "pa_front_upper.png"), (0, 0))
pa.paste(Image.open(BLOCK / "pa_front_lower.png"), (0, 16))
icon_from(pa.resize((32, 64), Image.NEAREST)).save(ITEM / "pa_speaker.png")
print("icons written")

import runpy
runpy.run_path(str(Path(__file__).resolve().parent / "make_pro_textures.py"))
