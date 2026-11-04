"""Draws the feed-belt item icon: a coiled belt of brass rounds linked by dark chain."""
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(r"D:\WorksTable\tacz-ammobox\src\main\resources\assets\taczammobox\textures\item\ammo_chain.png")

SIZE = 32
OUTLINE = (22, 22, 26)
LINK_HI = (156, 158, 164)
LINK_MID = (104, 106, 112)
LINK_LO = (66, 68, 74)
BRASS_HI = (255, 222, 132)
BRASS = (226, 172, 62)
BRASS_LO = (168, 116, 34)


def round_px(draw, x, y, vertical=True):
    """A 4x6 brass round with a light tip, anchored at x,y."""
    if vertical:
        draw.rectangle((x, y + 2, x + 3, y + 5), fill=BRASS, outline=OUTLINE)
        draw.line((x + 1, y + 2, x + 3, y + 2), fill=BRASS_HI)
        draw.line((x, y + 4, x + 3, y + 4), fill=BRASS_LO)
        draw.rectangle((x + 1, y, x + 2, y + 1), fill=BRASS_HI, outline=OUTLINE)
    else:
        draw.rectangle((x + 1, y, x + 5, y + 3), fill=BRASS, outline=OUTLINE)
        draw.line((x + 1, y + 1, x + 5, y + 1), fill=BRASS_HI)
        draw.line((x + 2, y + 3, x + 5, y + 3), fill=BRASS_LO)
        draw.rectangle((x, y + 1, x + 1, y + 2), fill=BRASS_HI, outline=OUTLINE)


def link(draw, x, y, w, h):
    draw.rectangle((x, y, x + w - 1, y + h - 1), fill=LINK_MID, outline=OUTLINE)
    draw.line((x, y, x + w - 2, y), fill=LINK_HI)
    draw.line((x, y + h - 1, x + w - 1, y + h - 1), fill=LINK_LO)


def build():
    img = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)

    # diagonal band of rounds with a chain edge on both sides
    for i in range(5):
        x = 3 + i * 5
        y = 22 - i * 4
        round_px(d, x, y, True)
        link(d, x, y + 6, 4, 2)
        link(d, x + 1, y - 2, 4, 2)

    # clasp at the top-right end
    d.rectangle((25, 2, 29, 5), fill=LINK_MID, outline=OUTLINE)
    d.line((25, 2, 29, 2), fill=LINK_HI)
    d.point((29, 3), fill=LINK_LO)
    d.rectangle((25, 1, 26, 1), fill=LINK_HI)
    d.line((24, 4, 25, 4), fill=OUTLINE)

    # sheen along the top edge
    d.line((7, 20, 20, 9), fill=BRASS_HI)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    img.save(OUT)
    img.resize((SIZE * 5, SIZE * 5), Image.NEAREST).save(Path(r"D:\WorksTable\.tmp-art\chain_icon_preview.png"))
    print("wrote", OUT)


if __name__ == "__main__":
    build()
