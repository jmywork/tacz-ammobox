"""CurseForge cover: full-bleed render left, bilingual mod name only on the right."""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(r"D:\WorksTable\tacz-ammobox")
PREVIEW = ROOT / "preview"
OUT = ROOT / "release" / "0.3.0"

W, H = 1280, 720
PAPER = (237, 234, 225)
INK = (27, 32, 38)
INK_SOFT = (94, 101, 108)
ORANGE = (206, 122, 34)
ORANGE_LIGHT = (232, 154, 58)
PANEL_X = 664

FONT_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FONT_MONO = "C:/Windows/Fonts/consolab.ttf"
FONT_CN = "C:/Windows/Fonts/msyhbd.ttc"


def font(path, size):
    return ImageFont.truetype(path, size)


def spaced_width(draw, text, fnt, spacing):
    return sum(draw.textlength(c, font=fnt) for c in text) + spacing * (len(text) - 1)


def draw_spaced(draw, x, y, text, fnt, fill, spacing):
    for ch in text:
        draw.text((x, y), ch, font=fnt, fill=fill, anchor="lm")
        x += draw.textlength(ch, font=fnt) + spacing


def build():
    img = Image.new("RGB", (W, H), PAPER).convert("RGBA")

    shot = Image.open(PREVIEW / "crate_detail.png").convert("RGB").crop((0, 55, 900, 820))
    scale = (H + 40) / shot.height
    shot = shot.resize((int(shot.width * scale), H + 40), Image.LANCZOS)
    img.paste(shot, (-70, -20))

    panel = Image.new("RGBA", (W - PANEL_X, H), PAPER + (255,))
    img.alpha_composite(panel, (PANEL_X, 0))
    draw = ImageDraw.Draw(img)
    draw.rectangle((PANEL_X, 0, PANEL_X + 6, H), fill=ORANGE_LIGHT)
    draw.line((PANEL_X + 6, 0, PANEL_X + 6, H), fill=ORANGE, width=2)

    # unified paper texture
    shade = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for i in range(110):
        sd.rectangle((i, i, W - i, H - i), outline=(120, 104, 80, 2))
    img.alpha_composite(shade)

    random.seed(11)
    grain = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    px = grain.load()
    for y in range(0, H, 2):
        for x in range(0, W, 2):
            v = random.randint(0, 255)
            px[x, y] = (v, v, v, 5)
    img.alpha_composite(grain)

    draw = ImageDraw.Draw(img)
    center_x = (PANEL_X + W) / 2

    f_brand = font(FONT_BOLD, 44)
    f_cn = font(FONT_CN, 84)
    f_en = font(FONT_MONO, 30)

    brand = "TACZ"
    brand_w = draw.textlength(brand, font=f_brand)
    colon_w = draw.textlength(":", font=f_brand)

    cn = "弹药压制"
    cn_w = draw.textlength(cn, font=f_cn)
    en = "TACZ: AMMO SUPPRESSION"
    en_w = spaced_width(draw, en, f_en, 3)

    block_w = max(brand_w + colon_w + 2, cn_w, en_w)
    left = center_x - block_w / 2

    # brand line: TACZ + full-width colon, sitting above the Chinese title
    y_brand = 228
    draw.text((left, y_brand), brand, font=f_brand, fill=ORANGE)
    draw.text((left + brand_w + 2, y_brand), ":", font=f_brand, fill=ORANGE)

    y_cn = 276
    draw.text((left, y_cn), cn, font=f_cn, fill=INK)

    y_en = 412
    draw_spaced(draw, left + 2, y_en, en, f_en, INK_SOFT, 3)
    draw.line((left, y_en + 40, left + en_w - 4, y_en + 40), fill=ORANGE, width=4)

    OUT.mkdir(parents=True, exist_ok=True)
    img.convert("RGB").save(OUT / "cf_cover_1280x720.png", optimize=True)
    img.convert("RGB").save(ROOT / "preview" / "cf_cover_preview.png", optimize=True)
    print("wrote", OUT / "cf_cover_1280x720.png")


if __name__ == "__main__":
    build()
