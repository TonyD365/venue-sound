"""Turn the photo shoot's screenshots into the guide's item images.

    ./gradlew runClient -Pphotos=true      # writes run/screenshots/photo-*.png and icon-*.png
    python3 tools/make_item_images.py      # writes docs/images/items/<id>.jpg and <id>-icon.png

World photos are cropped round the item (tighter for small ones) and saved as 800x450 JPEGs.
Icons are cut out of their magenta background and saved as 128x128 transparent PNGs.
"""

from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "run" / "screenshots"
OUT = ROOT / "docs" / "images" / "items"

# small items: crop closer round the middle of the picture
SMALL = {"mixer", "mixer24", "cable_clip", "choir_mic", "outlet", "receiver", "iem_transmitter", "stand_mic", "gooseneck_mic",
         "media_deck", "video_capture", "intercom_station", "floor_wedge"}


def photo(src: Path, item: str) -> None:
    im = Image.open(src).convert("RGB")
    w, h = im.size
    keep = 0.42 if item in SMALL else 0.7
    cw, ch = int(w * keep), int(w * keep * 9 / 16)
    box = ((w - cw) // 2, (h - ch) // 2, (w + cw) // 2, (h + ch) // 2)
    im.crop(box).resize((800, 450), Image.LANCZOS).save(OUT / f"{item}.jpg", quality=86)


def icon(src: Path, item: str) -> None:
    im = Image.open(src).convert("RGBA")
    px = im.load()
    w, h = im.size
    for y in range(h):
        for x in range(w):
            r, g, b, _ = px[x, y]
            if r > 200 and b > 200 and g < 60:
                px[x, y] = (0, 0, 0, 0)
    bbox = im.getbbox()
    if bbox is None:
        return
    im = im.crop(bbox)
    side = max(im.size)
    sq = Image.new("RGBA", (side, side), (0, 0, 0, 0))
    sq.paste(im, ((side - im.width) // 2, (side - im.height) // 2))
    sq.resize((128, 128), Image.NEAREST).save(OUT / f"{item}-icon.png")


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    for src in sorted(SHOTS.glob("photo-*.png")):
        photo(src, src.stem[len("photo-"):])
    for src in sorted(SHOTS.glob("icon-*.png")):
        icon(src, src.stem[len("icon-"):])
    print(f"item images written to {OUT}")


if __name__ == "__main__":
    main()
