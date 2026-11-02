"""Generate clean CurseForge avatar/cover art for TACZ Ammo Box."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = Path(__file__).resolve().parent.parent / "release" / "0.1.0-beta"
FONT_BOLD = "C:/Windows/Fonts/segoeuib.ttf"
FONT_SEMI = "C:/Windows/Fonts/segoeui.ttf"
FONT_MONO = "C:/Windows/Fonts/consolab.ttf"

ORANGE = (232, 154, 58)
ORANGE_LIGHT = (255, 187, 92)
CREAM = (232, 225, 211)
DARK = (17, 21, 25)
STEEL = (34, 42, 50)
STEEL_LIGHT = (48, 58, 68)


def font(path: str, size: int) -> ImageFont.FreeTypeFont:
    return ImageFont.truetype(path, size)


def vertical_gradient(size, top, bottom):
    img = Image.new("RGB", size)
    draw = ImageDraw.Draw(img)
    for y in range(size[1]):
        t = y / max(1, size[1] - 1)
        color = tuple(int(top[i] + (bottom[i] - top[i]) * t) for i in range(3))
        draw.line([(0, y), (size[0], y)], fill=color)
    return img


def letterspaced(draw, xy, text, fnt, fill, spacing=2, anchor=None):
    x, y = xy
    if anchor == "mm":
        widths = [draw.textlength(ch, font=fnt) for ch in text]
        total = sum(widths) + spacing * (len(text) - 1)
        x -= total / 2
    for ch in text:
        draw.text((x, y), ch, font=fnt, fill=fill, anchor="lm")
        x += draw.textlength(ch, font=fnt) + spacing
    return x


def draw_ammo_can(img, x, y, w, h, depth=22, label="TACZ", subtitle="AMMO BOX", bullets=5):
    draw = ImageDraw.Draw(img)
    # drop shadow
    shadow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.ellipse((x - w * 0.08, y + h * 0.84, x + w * 1.08, y + h * 1.16), fill=(0, 0, 0, 150))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    img.alpha_composite(shadow)

    # right side + top depth
    side = [(x + w, y), (x + w + depth, y - depth * 0.55),
            (x + w + depth, y + h - depth * 0.55), (x + w, y + h)]
    top = [(x, y), (x + w, y), (x + w + depth, y - depth * 0.55), (x + depth, y - depth * 0.55)]
    draw.polygon(side, fill=(22, 27, 32))
    draw.polygon(top, fill=(45, 55, 65))

    # bullets above the lid
    if bullets:
        bw = max(10, int(w * 0.035))
        gap = w * 0.11
        start = x + w * 0.5 - gap * (bullets - 1) / 2
        for i in range(bullets):
            bx = start + i * gap - bw / 2
            by = y - depth * 0.55 - h * 0.30
            casing = (210, 126, 43)
            # case
            draw.rounded_rectangle((bx, by + h * 0.11, bx + bw, y + 2), radius=bw * 0.35,
                                   fill=casing, outline=(96, 54, 18), width=1)
            # tip
            draw.polygon([(bx, by + h * 0.12), (bx + bw / 2, by - h * 0.03),
                          (bx + bw, by + h * 0.12)], fill=(236, 177, 78))
            draw.line((bx + bw * 0.5, by - h * 0.02, bx + bw * 0.5, by + h * 0.10),
                      fill=(255, 224, 151), width=1)

    # front body
    body = (x, y, x + w, y + h)
    draw.rounded_rectangle(body, radius=max(6, int(w * 0.025)), fill=STEEL,
                           outline=(14, 17, 21), width=max(2, int(w * 0.006)))
    # lid
    lid_h = h * 0.30
    draw.rounded_rectangle((x + 2, y + 2, x + w - 2, y + lid_h), radius=max(4, int(w * 0.018)),
                           fill=(39, 48, 58))
    draw.line((x + w * 0.03, y + lid_h, x + w * 0.97, y + lid_h), fill=(12, 15, 19), width=2)
    # top edge highlight
    draw.line((x + w * 0.04, y + 3, x + w * 0.96, y + 3), fill=(84, 98, 112), width=2)

    # orange band
    band_y = y + h * 0.52
    band_h = h * 0.13
    draw.rectangle((x + 2, band_y, x + w - 2, band_y + band_h), fill=ORANGE)
    draw.rectangle((x + 2, band_y, x + w - 2, band_y + max(2, band_h * 0.16)),
                   fill=ORANGE_LIGHT)

    # label plate
    plate = (x + w * 0.07, y + h * 0.61, x + w * 0.93, y + h * 0.94)
    ph = plate[3] - plate[1]
    draw.rounded_rectangle(plate, radius=max(2, int(w * 0.012)), fill=CREAM,
                           outline=(188, 180, 165), width=1)
    # subtle plate lines
    for i in range(1, 4):
        ly = plate[1] + ph * i / 4
        draw.line((plate[0] + 4, ly, plate[2] - 4, ly), fill=(214, 207, 192), width=1)

    # label text: size against the plate, not the whole box
    tw = plate[2] - plate[0]
    size1 = max(12, int(min(tw * 0.30, ph * 0.58)))
    while size1 > 12 and draw.textlength(label, font=font(FONT_BOLD, size1)) > tw * 0.78:
        size1 -= 1
    f1 = font(FONT_BOLD, size1)
    f2 = font(FONT_BOLD, max(7, int(ph * 0.15)))
    tx = (plate[0] + plate[2]) / 2
    draw.text((tx, plate[1] + ph * 0.40), label, font=f1,
              fill=(25, 28, 30), anchor="mm")
    letterspaced(draw, (tx, plate[1] + ph * 0.79), subtitle, f2,
                 (92, 84, 72), spacing=max(1, int(tw * 0.008)), anchor="mm")

    # latch / bolts
    draw.rounded_rectangle((x + w * 0.42, y + h * 0.31, x + w * 0.58, y + h * 0.40),
                           radius=4, fill=(20, 24, 29), outline=(92, 104, 116), width=1)
    for cx, cy in [(x + w * 0.05, y + h * 0.58), (x + w * 0.95, y + h * 0.58),
                   (x + w * 0.05, y + h * 0.96), (x + w * 0.95, y + h * 0.96)]:
        draw.ellipse((cx - 4, cy - 4, cx + 4, cy + 4), fill=(16, 20, 24),
                     outline=(92, 104, 116), width=1)

    # edge highlights
    draw.line((x + 2, y + h * 0.34, x + 2, y + h - 4), fill=(61, 74, 87), width=2)
    draw.line((x + w - 2, y + h * 0.34, x + w - 2, y + h - 4), fill=(10, 13, 16), width=2)


def background(size, grid=True):
    img = vertical_gradient(size, (20, 25, 31), (9, 12, 15)).convert("RGBA")
    overlay = Image.new("RGBA", size, (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    if grid:
        step = 64
        for x in range(0, size[0], step):
            od.line((x, 0, x, size[1]), fill=(255, 255, 255, 8))
        for y in range(0, size[1], step):
            od.line((0, y, size[0], y), fill=(255, 255, 255, 8))
    # vignette
    for i in range(160):
        alpha = int(3 + i * 0.25)
        od.rectangle((i, i, size[0] - i, size[1] - i), outline=(0, 0, 0, alpha))
    img.alpha_composite(overlay)
    return img


def avatar():
    size = 400
    img = background((size, size))
    glow = Image.new("RGBA", img.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((55, 90, 345, 330), fill=(232, 154, 58, 55))
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    img.alpha_composite(glow)

    draw = ImageDraw.Draw(img)
    # corner brackets
    for x, y, dx, dy in [(24, 24, 1, 1), (376, 24, -1, 1), (24, 376, 1, -1), (376, 376, -1, -1)]:
        draw.line((x, y, x + dx * 34, y), fill=ORANGE, width=4)
        draw.line((x, y, x, y + dy * 34), fill=ORANGE, width=4)

    draw_ammo_can(img, 70, 148, 260, 150, depth=24, label="TACZ", subtitle="AMMO BOX", bullets=5)

    draw.text((200, 368), "MINECRAFT 1.20.1  /  FORGE", font=font(FONT_MONO, 11),
              fill=(126, 137, 148), anchor="mm")
    img.convert("RGB").save(OUT / "cf_avatar_400.png", optimize=True)


def cover():
    size = (1280, 720)
    img = background(size)
    draw = ImageDraw.Draw(img)

    # side accent
    draw.rectangle((0, 0, 10, 720), fill=ORANGE)
    draw.rectangle((10, 0, 14, 720), fill=(120, 75, 25))

    # glow behind product
    glow = Image.new("RGBA", size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse((30, 210, 660, 690), fill=(232, 154, 58, 45))
    glow = glow.filter(ImageFilter.GaussianBlur(60))
    img.alpha_composite(glow)

    # product
    draw_ammo_can(img, 92, 328, 430, 235, depth=34, label="TACZ", subtitle="AMMO BOX", bullets=7)

    # text column
    x = 650
    draw.text((x, 88), "MINECRAFT 1.20.1   /   FORGE   /   TACZ ADDON",
              font=font(FONT_MONO, 16), fill=(139, 151, 164))
    draw.line((x, 128, x + 560, 128), fill=(65, 76, 88), width=1)

    draw.text((x, 160), "TACZ", font=font(FONT_BOLD, 100), fill=ORANGE)
    draw.text((x, 254), "AMMO BOX", font=font(FONT_BOLD, 78), fill=CREAM)

    letterspaced(draw, (x + 2, 368), "PACK  •  STORE  •  LOAD", font(FONT_BOLD, 21),
                 (139, 151, 164), spacing=3)

    draw.line((x, 410, x + 420, 410), fill=ORANGE, width=4)

    features = [
        "24 built-in ammo types",
        "Packer & Loader automation",
        "Third-party gun pack ready",
    ]
    fy = 444
    for line in features:
        draw.ellipse((x, fy + 8, x + 9, fy + 17), fill=ORANGE)
        draw.text((x + 24, fy), line, font=font(FONT_SEMI, 24), fill=(210, 217, 224))
        fy += 42

    draw.text((x, 620), "VERSION 0.1.0-BETA", font=font(FONT_MONO, 15), fill=(101, 112, 124))
    draw.text((x, 646), "github.com/jmywork/tacz-ammobox", font=font(FONT_MONO, 14),
              fill=(101, 112, 124))

    # corner brackets
    for bx, by, dx, dy in [(30, 30, 1, 1), (1250, 30, -1, 1), (30, 690, 1, -1), (1250, 690, -1, -1)]:
        draw.line((bx, by, bx + dx * 28, by), fill=(93, 105, 117), width=2)
        draw.line((bx, by, bx, by + dy * 28), fill=(93, 105, 117), width=2)

    img.convert("RGB").save(OUT / "cf_cover_1280x720.png", optimize=True)


if __name__ == "__main__":
    OUT.mkdir(parents=True, exist_ok=True)
    avatar()
    cover()
    print("wrote", OUT / "cf_avatar_400.png", OUT / "cf_cover_1280x720.png")
