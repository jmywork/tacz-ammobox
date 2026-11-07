"""GUI style previews drawn at final resolution (2x) for the three machines."""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

OUT = Path(r"D:\WorksTable\.tmp-art")
FONT_CN = "C:/Windows/Fonts/msyhbd.ttc"
FONT_MONO = "C:/Windows/Fonts/consolab.ttf"

S = 2  # preview scale


def font(path, size):
    return ImageFont.truetype(path, int(size * S))


PALETTES = [
    {
        "file": "A_dark_tech",
        "name": "A · 深色科技 Dark Tech",
        "bg": (14, 18, 24),
        "panel": (24, 31, 41),
        "panel2": (33, 43, 56),
        "border": (52, 72, 94),
        "accent": (56, 225, 208),
        "accent2": (255, 176, 32),
        "text": (219, 231, 246),
        "muted": (118, 136, 160),
        "slot": (18, 24, 32),
    },
    {
        "file": "B_light_industrial",
        "name": "B · 浅色工业 Light Industrial",
        "bg": (235, 238, 243),
        "panel": (255, 255, 255),
        "panel2": (243, 246, 250),
        "border": (203, 212, 224),
        "accent": (214, 122, 34),
        "accent2": (22, 122, 108),
        "text": (28, 38, 52),
        "muted": (104, 116, 132),
        "slot": (228, 234, 241),
    },
    {
        "file": "C_cyber_hud",
        "name": "C · 赛博 HUD Cyber HUD",
        "bg": (6, 9, 13),
        "panel": (12, 18, 26),
        "panel2": (20, 29, 40),
        "border": (34, 96, 118),
        "accent": (56, 225, 208),
        "accent2": (255, 138, 30),
        "text": (188, 232, 240),
        "muted": (86, 128, 148),
        "slot": (8, 13, 19),
    },
]


def px(v):
    return int(v * S)


def draw_panel(d, x, y, w, h, pal, fill=None, outline=None, r=6, width=2):
    d.rounded_rectangle((px(x), px(y), px(x + w), px(y + h)), radius=px(r),
                        fill=fill or pal["panel"], outline=outline or pal["border"], width=px(width))


def draw_slot(d, x, y, pal, size=18):
    d.rounded_rectangle((px(x), px(y), px(x + size), px(y + size)), radius=px(2),
                        fill=pal["slot"], outline=pal["border"], width=px(1))


def draw_button(d, x, y, w, h, label, pal, color=None):
    d.rounded_rectangle((px(x), px(y), px(x + w), px(y + h)), radius=px(4),
                        fill=pal["panel2"], outline=pal["border"], width=px(1))
    d.text((px(x + 8), px(y + h / 2)), label, font=font(FONT_CN, 10),
           fill=color or pal["muted"], anchor="lm")


def assembler(pal):
    W, H = 340, 180
    img = Image.new("RGB", (px(W), px(H)), pal["bg"])
    d = ImageDraw.Draw(img)
    draw_panel(d, 0, 0, W, H, pal, r=8)

    d.text((px(10), px(5)), "弹药装配机", font=font(FONT_CN, 13), fill=pal["text"])
    d.line((px(10), px(24), px(170), px(24)), fill=pal["accent"], width=px(2))

    for cx, cw, name in [(8, 52, "弹药"), (60, 140, "配方"), (206, 44, "产物")]:
        draw_panel(d, cx, 28, cw, 124, pal, fill=pal["panel2"], r=4, width=1)
        d.text((px(cx + 3), px(31)), name, font=font(FONT_CN, 10), fill=pal["muted"])

    for i in range(6):
        y = 47 + i * 17
        if i == 0:
            d.rounded_rectangle((px(10), px(y - 1), px(58), px(y + 17)), radius=px(2),
                                fill=pal["panel2"], outline=pal["accent"], width=px(1))
        draw_slot(d, 13, y, pal, 15)

    for r in range(2):
        y = 47 + r * 50
        outline = pal["accent"] if r == 0 else pal["border"]
        d.rounded_rectangle((px(62), px(y - 1), px(198), px(y + 46)), radius=px(3),
                            fill=pal["slot"], outline=outline, width=px(1))
        for line in range(2):
            for c in range(3):
                sx = 66 + c * 43
                sy = y + 3 + line * 20
                draw_slot(d, sx, sy, pal, 15)
                d.text((px(sx + 16), px(sy + 5)), "18", font=font(FONT_MONO, 7),
                       fill=pal["accent2"] if c == 0 else pal["text"])

    draw_slot(d, 216, 50, pal, 16)
    d.text((px(214), px(74)), "x100", font=font(FONT_MONO, 9), fill=pal["text"])
    d.rounded_rectangle((px(210), px(96), px(246), px(103)), radius=px(3),
                        fill=pal["slot"], outline=pal["border"], width=px(1))
    d.rounded_rectangle((px(210), px(96), px(232), px(103)), radius=px(3), fill=pal["accent"])

    draw_panel(d, 252, 28, 80, 144, pal, fill=pal["panel2"], r=4, width=1)
    for i in range(6):
        draw_button(d, 256, 32 + i * 19, 72, 16, "上 入" if i == 0 else "下 关", pal,
                    pal["accent2"] if i == 0 else None)
    draw_button(d, 256, 148, 72, 16, "忽略红石", pal)
    d.text((px(252), px(14)), "面配置", font=font(FONT_CN, 9), fill=pal["muted"])
    return img


def packer(pal):
    W, H = 176, 120
    img = Image.new("RGB", (px(W), px(H)), pal["bg"])
    d = ImageDraw.Draw(img)
    draw_panel(d, 0, 0, W, H, pal, r=8)
    d.text((px(8), px(5)), "弹药打包机", font=font(FONT_CN, 12), fill=pal["text"])
    d.line((px(8), px(22), px(140), px(22)), fill=pal["accent"], width=px(2))
    d.text((px(8), px(28)), "模式：打包", font=font(FONT_CN, 9), fill=pal["muted"])
    for x in (16, 44, 120):
        draw_slot(d, x, 46, pal, 18)
    d.rounded_rectangle((px(20), px(78), px(132), px(87)), radius=px(3),
                        fill=pal["slot"], outline=pal["border"], width=px(1))
    d.rounded_rectangle((px(20), px(78), px(96), px(87)), radius=px(3), fill=pal["accent"])
    d.text((px(8), px(94)), "过滤", font=font(FONT_CN, 9), fill=pal["muted"])
    draw_slot(d, 34, 92, pal, 16)
    return img


def loader(pal):
    W, H = 200, 120
    img = Image.new("RGB", (px(W), px(H)), pal["bg"])
    d = ImageDraw.Draw(img)
    draw_panel(d, 0, 0, W, H, pal, r=8)
    d.text((px(8), px(5)), "弹药装填机", font=font(FONT_CN, 12), fill=pal["text"])
    d.line((px(8), px(22), px(160), px(22)), fill=pal["accent"], width=px(2))
    d.text((px(8), px(28)), "红石：忽略", font=font(FONT_CN, 9), fill=pal["muted"])
    for x in (16, 48, 150):
        draw_slot(d, x, 48, pal, 18)
    draw_slot(d, 120, 48, pal, 18)
    d.rounded_rectangle((px(78), px(50), px(112), px(59)), radius=px(3),
                        fill=pal["slot"], outline=pal["border"], width=px(1))
    d.rounded_rectangle((px(78), px(50), px(98), px(59)), radius=px(3), fill=pal["accent"])
    return img


def build():
    for pal in PALETTES:
        canvas_w, canvas_h = 1420, 640
        canvas = Image.new("RGB", (canvas_w, canvas_h), pal["bg"])
        d = ImageDraw.Draw(canvas)
        d.text((24, 16), pal["name"], font=font(FONT_CN, 16), fill=pal["text"])
        d.text((24, 54), "预览 mockup：装配机 / 打包机 / 装填机", font=font(FONT_CN, 9), fill=pal["muted"])

        asm = assembler(pal)
        canvas.paste(asm, (24, 86))
        canvas.paste(packer(pal), (24 + asm.width + 24, 86))
        canvas.paste(loader(pal), (24 + asm.width + 24, 86 + 300))

        d.text((24, 86 + asm.height + 12), "装配机 340 x 180", font=font(FONT_MONO, 8), fill=pal["muted"])
        canvas.save(OUT / f"gui_style_{pal['file']}.png")
        print("wrote", OUT / f"gui_style_{pal['file']}.png")


if __name__ == "__main__":
    build()
