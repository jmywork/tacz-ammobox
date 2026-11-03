"""CurseForge project icon: crate render with the bilingual mod name underneath."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(r"D:\WorksTable\tacz-ammobox")
PREVIEW = ROOT / "preview"
OUT = ROOT / "release" / "0.3.0"

SIZE = 400
PAPER = (237, 234, 225)
INK = (27, 32, 38)
ORANGE = (206, 122, 34)
ORANGE_LIGHT = (232, 154, 58)

FONT_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FONT_CN = "C:/Windows/Fonts/msyhbd.ttc"

IMG_H = 262


def font(path, size):
    return ImageFont.truetype(path, size)


def build():
    img = Image.new("RGB", (SIZE, SIZE), PAPER).convert("RGBA")

    # product image on top
    shot = Image.open(PREVIEW / "crate_detail.png").convert("RGB").crop((67, 55, 832, 820))
    shot = shot.resize((SIZE, IMG_H + 8), Image.LANCZOS)
    img.paste(shot, (0, -4))

    # paper strip for the name
    strip = Image.new("RGBA", (SIZE, SIZE - IMG_H), PAPER + (255,))
    img.alpha_composite(strip, (0, IMG_H))
    draw = ImageDraw.Draw(img)
    draw.line((0, IMG_H, SIZE, IMG_H), fill=ORANGE_LIGHT, width=4)

    f_brand = font(FONT_BOLD, 58)
    brand = "TACZ"
    brand_w = draw.textlength(brand, font=f_brand)
    draw.text(((SIZE - brand_w) / 2, IMG_H + 16), brand, font=f_brand, fill=ORANGE)

    f_cn = font(FONT_CN, 30)
    cn = "弹药压制"
    cn_w = draw.textlength(cn, font=f_cn)
    draw.text(((SIZE - cn_w) / 2, IMG_H + 84), cn, font=f_cn, fill=INK)

    draw.rectangle((0, SIZE - 6, SIZE, SIZE), fill=ORANGE)

    shade = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for i in range(46):
        sd.rectangle((i, i, SIZE - i, SIZE - i), outline=(120, 104, 80, 3))
    img.alpha_composite(shade)

    OUT.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(OUT / "cf_avatar_400.png", optimize=True)
    print("wrote", OUT / "cf_avatar_400.png")


if __name__ == "__main__":
    build()
