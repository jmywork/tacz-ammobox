"""CurseForge project icon: square crop of the crate render, matching the cover."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter

ROOT = Path(r"D:\WorksTable\tacz-ammobox")
PREVIEW = ROOT / "preview"
OUT = ROOT / "release" / "0.3.0"

SIZE = 400
PAPER = (237, 234, 225)
ORANGE = (232, 154, 58)


def build():
    shot = Image.open(PREVIEW / "crate_detail.png").convert("RGB").crop((67, 55, 832, 820))
    shot = shot.resize((SIZE, SIZE), Image.LANCZOS)
    img = shot.convert("RGBA")

    shade = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for i in range(60):
        sd.rectangle((i, i, SIZE - i, SIZE - i), outline=(120, 104, 80, 3))
    img.alpha_composite(shade)

    draw = ImageDraw.Draw(img)
    draw.rectangle((0, SIZE - 8, SIZE, SIZE), fill=ORANGE)

    OUT.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(OUT / "cf_avatar_400.png", optimize=True)
    print("wrote", OUT / "cf_avatar_400.png")


if __name__ == "__main__":
    build()
