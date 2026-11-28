"""Cuts the demo video from the film mode's frames.

    ./gradlew runClient -Pfilm=true          # writes run/screenshots/film/f00000.png...
    python3 tools/make_video.py              # writes site/media/venuesound-demo.mp4, docs/images/demo.gif

Adds the title, English and Chinese subtitles (they follow the shots in client/FilmMode) and the
end card, scales to 1280x720, and puts music from tools/make_music.py under it. The GIF is the
console close-up, for the README.
"""

import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
FRAMES = ROOT / "run" / "screenshots" / "film"
FPS = 20
W, H = 1280, 720
GOLD = (232, 184, 74)

LATIN = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
LATIN_REG = "/System/Library/Fonts/Supplemental/Arial.ttf"
CJK = next((str(p) for p in Path("/System/Library/AssetsV2").glob("*/*/AssetData/PingFang.ttc")),
           "/System/Library/Fonts/STHeiti Medium.ttc")

# (from s, to s, English, Chinese): follows the shots in FilmMode (frame = tick - 2)
SUBS = [
    (5.0, 10.0, "Wireless mics on 16 channels, into a receiver", "16 通道无线话筒，信号进入接收器"),
    (10.0, 16.0, "Cables plugged socket to socket: XLR, TRS, Speakon", "插头对插孔：XLR、TRS、Speakon，和真设备一样"),
    (16.0, 25.0, "Every fader, knob and button works, with live meters", "每个推子、旋钮、按钮都能用，电平表实时跳动"),
    (25.0, 30.0, "Sockets on the rear like a real desk, +48V phantom power", "插孔在背面，像真调音台；还有 +48V 幻象电源"),
    (30.0, 36.0, "Amplifiers, EQ, compressors, effects, crossovers", "功放、均衡器、压缩器、效果器、分频器"),
    (36.0, 41.0, "Floor wedges, in-ear monitors and a crew intercom", "地板返听、入耳监听、工作人员内通"),
    (41.0, 47.0, "Run the console from a tablet over OSC", "用平板通过 OSC 遥控调音台"),
]
TITLE = (0.3, 4.8)
END = 47.0


def font(path: str, size: int, index: int = 0) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size, index=index)


def alpha(t: float, a: float, b: float, fade: float = 0.4) -> float:
    if t < a or t > b:
        return 0.0
    return min(1.0, (t - a) / fade, (b - t) / fade)


def overlay(im: Image.Image, t: float, total: float) -> Image.Image:
    im = im.convert("RGBA")
    layer = Image.new("RGBA", im.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    for a, b, en, zh in SUBS:
        k = alpha(t, a, b)
        if k <= 0:
            continue
        fe, fz = font(LATIN, 34), font(CJK, 30)
        we, wz = d.textlength(en, font=fe), d.textlength(zh, font=fz)
        box_w = max(we, wz) + 60
        x0, y0 = (W - box_w) / 2, H - 150
        d.rounded_rectangle([x0, y0, x0 + box_w, y0 + 104], 14, fill=(10, 10, 12, int(170 * k)))
        d.text(((W - we) / 2, y0 + 12), en, font=fe, fill=(255, 255, 255, int(255 * k)))
        d.text(((W - wz) / 2, y0 + 58), zh, font=fz, fill=(GOLD[0], GOLD[1], GOLD[2], int(255 * k)))
    k = alpha(t, *TITLE, fade=0.6)
    if k > 0:
        d.rectangle([0, 0, W, H], fill=(0, 0, 0, int(90 * k)))
        ft, fs, fz = font(LATIN, 96), font(LATIN_REG, 34), font(CJK, 34)
        title, sub, zh = "VenueSound", "A real concert sound system for Minecraft", "Minecraft 里的真实演唱会扩声系统"
        d.text(((W - d.textlength(title, font=ft)) / 2, 240), title, font=ft, fill=(GOLD[0], GOLD[1], GOLD[2], int(255 * k)))
        d.text(((W - d.textlength(sub, font=fs)) / 2, 360), sub, font=fs, fill=(255, 255, 255, int(255 * k)))
        d.text(((W - d.textlength(zh, font=fz)) / 2, 410), zh, font=fz, fill=(230, 230, 230, int(255 * k)))
    k = alpha(t, END, total + 1, fade=0.8)
    if k > 0:
        d.rectangle([0, 0, W, H], fill=(12, 12, 14, int(185 * k)))
        ft, fs, fz = font(LATIN, 84), font(LATIN_REG, 30), font(CJK, 30)
        lines = [("VenueSound", ft, GOLD, 200), ("Fabric 1.20.1  ·  Simple Voice Chat", fs, (255, 255, 255), 320),
                 ("Download: Modrinth  ·  CurseForge  ·  GitHub", fs, (255, 255, 255), 372),
                 ("github.com/TonyD365/venue-sound", fs, (200, 200, 205), 424),
                 ("免费下载 · 开源 · 支持中文", fz, GOLD, 490)]
        for text, f, col, yy in lines:
            d.text(((W - d.textlength(text, font=f)) / 2, yy), text, font=f, fill=(col[0], col[1], col[2], int(255 * k)))
    return Image.alpha_composite(im, layer).convert("RGB")


def main() -> None:
    frames = sorted(FRAMES.glob("f*.png"))
    if not frames:
        sys.exit("no frames: run ./gradlew runClient -Pfilm=true first")
    total = len(frames) / FPS
    out_dir = ROOT / "site" / "media"
    out_dir.mkdir(parents=True, exist_ok=True)
    tmp = Path(tempfile.mkdtemp(prefix="venuesound-video-"))
    try:
        for i, f in enumerate(frames):
            im = Image.open(f).convert("RGB").resize((W, H), Image.LANCZOS)
            overlay(im, i / FPS, total).save(tmp / f"g{i:05d}.jpg", quality=92)
        music = tmp / "music.wav"
        subprocess.run([sys.executable, str(ROOT / "tools" / "make_music.py"), str(music), f"{total:.2f}"], check=True)
        mp4 = out_dir / "venuesound-demo.mp4"
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-framerate", str(FPS), "-i", str(tmp / "g%05d.jpg"), "-i", str(music),
                        "-c:v", "libx264", "-preset", "slow", "-crf", "29", "-tune", "film", "-pix_fmt", "yuv420p", "-r", "30",
                        "-c:a", "aac", "-b:a", "160k", "-shortest", "-movflags", "+faststart", str(mp4)], check=True)
        # poster frame for the site
        Image.open(tmp / f"g{int(min(total - 1, 20) * FPS):05d}.jpg").save(out_dir / "venuesound-demo-poster.jpg", quality=86)
        # README GIF: the console close-up (16-25 s), small and looping
        gif = ROOT / "docs" / "images" / "demo.gif"
        palette = tmp / "palette.png"
        clip = ["-ss", "16.2", "-t", "7", "-framerate", str(FPS), "-i", str(tmp / "g%05d.jpg")]
        vf = "fps=10,scale=560:-1:flags=lanczos"
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-framerate", str(FPS), "-start_number", str(int(16.2 * FPS)),
                        "-i", str(tmp / "g%05d.jpg"), "-t", "7", "-vf", vf + ",palettegen=max_colors=96:stats_mode=diff", str(palette)], check=True)
        subprocess.run(["ffmpeg", "-y", "-v", "error", "-framerate", str(FPS), "-start_number", str(int(16.2 * FPS)),
                        "-i", str(tmp / "g%05d.jpg"), "-i", str(palette), "-t", "7",
                        "-lavfi", vf + " [x]; [x][1:v] paletteuse=dither=bayer:bayer_scale=4:diff_mode=rectangle", str(gif)], check=True)
        del clip
        print(f"wrote {mp4} ({total:.1f} s) and {gif}")
    finally:
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == "__main__":
    main()
