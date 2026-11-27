"""Textures of the pro devices, drawn on top of the base textures make_textures.py writes. Run after it."""
from pathlib import Path
from PIL import Image, ImageDraw

A = Path(__file__).resolve().parent.parent / "src/main/resources/assets/venuesound/textures"


def tex(name, base, draw):
    im = Image.open(A / f"{base}.png").convert("RGBA")
    draw(ImageDraw.Draw(im), *im.size)
    im.save(A / f"{name}.png")


def stripe(col, extra=None):
    def f(d, W, H):
        d.rectangle([0, 0, W - 1, max(1, H // 16)], fill=col + (255,))
        if extra:
            extra(d, W, H)
    return f


def eq_marks(d, W, H):
    for i in range(7):
        x = int(W * (2 + i * 1.7) / 16)
        d.rectangle([x, H * 12 // 16, x + max(1, W // 32), H * 15 // 16], fill=(220, 220, 220, 255))


for k, col, extra in [("graphic_eq", (40, 120, 200), eq_marks), ("compressor", (200, 60, 50), None),
                      ("di_box", (60, 170, 90), None), ("fx_processor", (140, 70, 180), None),
                      ("crossover", (230, 140, 30), None), ("power_sequencer", (220, 200, 40), None),
                      ("iem_transmitter", (30, 160, 200), None)]:
    tex(f"block/{k}_front", "block/amplifier_front", stripe(col, extra))

tex("block/floor_wedge_front", "block/speaker_front", lambda d, W, H: None)


def sub(d, W, H):
    d.ellipse([W * 2 // 16, H * 2 // 16, W * 14 // 16, H * 14 // 16], fill=(25, 25, 28, 255), outline=(90, 90, 95, 255))
    d.ellipse([W * 6 // 16, H * 6 // 16, W * 10 // 16, H * 10 // 16], fill=(45, 45, 50, 255))


tex("block/subwoofer_front", "block/speaker_front", sub)


def stage_box(n):
    def f(d, W, H):
        d.rectangle([0, 0, W - 1, H - 1], fill=(38, 40, 44, 255))
        per = n // 2
        for i in range(n):
            if n == 8:
                x, y, s = 1.5 + (i % 4) * 3.4, 16 - (12.4 if i < 4 else 7.9), 2.4
            else:
                x, y, s = 0.8 + (i % 8) * 1.8, 16 - (12.4 if i < per else 8.9), 1.4
            d.ellipse([W * x / 16, H * y / 16, W * (x + s) / 16, H * (y + s) / 16], fill=(150, 155, 160, 255))
        d.rectangle([W * 6 / 16, H * 12 / 16, W * 10 / 16, H * 15 / 16], fill=(200, 60, 50, 255))
    return f


tex("block/stage_box_front", "block/amplifier_front", stage_box(8))
tex("block/stage_box_16_front", "block/amplifier_front", stage_box(16))


def rack(d, W, H):
    d.rectangle([0, 0, W - 1, H - 1], fill=(20, 20, 22, 255))
    d.rectangle([W // 16, 0, W * 15 // 16, H - 1], outline=(110, 110, 115, 255))
    for y in range(2, 16, 3):
        d.line([W * 2 // 16, H * y // 16, W * 14 // 16, H * y // 16], fill=(60, 60, 64, 255))


tex("block/rack_cabinet_front", "block/amplifier_front", rack)


def truss(d, W, H):
    d.rectangle([0, 0, W - 1, H - 1], fill=(170, 172, 176, 255))
    d.line([0, 0, W, H], fill=(110, 112, 116, 255), width=max(1, W // 16))
    d.line([0, H, W, 0], fill=(110, 112, 116, 255), width=max(1, W // 16))


tex("block/truss", "block/stand_metal", truss)

for k, base, col in [("headset_mic", "wireless_mic", (230, 200, 170)), ("lavalier_mic", "wireless_mic", (60, 60, 60)),
                     ("iem_earpiece", "wireless_mic", (30, 160, 200)), ("xlr_cable", "audio_cable", (40, 40, 40)),
                     ("speaker_cable", "audio_cable", (200, 60, 50)), ("snake_cable", "audio_cable", (30, 90, 200)),
                     ("cable_reel", "audio_cable", (240, 180, 30))]:
    tex(f"item/{k}", f"item/{base}", lambda d, W, H, c=col: d.rectangle([W * 10 // 16, 0, W - 1, H * 6 // 16], fill=c + (255,)))
print("pro device textures written")
