"""Render isometric previews of the generated item models.

Not a Minecraft renderer: it projects the model cuboid and pastes the face
textures, so the preview matches the geometry/UV intent closely enough for review.
"""
import json
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
MODELS = ROOT / "src/main/resources/assets/taczammobox/models/item"
TEX = ROOT / "src/main/resources/assets/taczammobox/textures/item"
OUT = ROOT / "preview"
OUT.mkdir(exist_ok=True)

COS30 = math.cos(math.radians(30))
SIN30 = math.sin(math.radians(30))

# Camera at (+1, +1, -1): sees east (+X), top (+Y) and north (-Z, the label face).
RIGHT = (0.70710678, 0.0, 0.70710678)
UP = (-0.40824829, 0.81649658, 0.40824829)


def project(x, y, z, scale, ox, oy):
    sx = x * RIGHT[0] + y * RIGHT[1] + z * RIGHT[2]
    sy = x * UP[0] + y * UP[1] + z * UP[2]
    return (ox + sx * scale, oy - sy * scale)


def quad_points(face, box, scale, ox, oy):
    x0, y0, z0, x1, y1, z1 = box
    if face == "north":
        pts = [(x0, y1, z0), (x1, y1, z0), (x1, y0, z0), (x0, y0, z0)]
    elif face == "east":
        pts = [(x1, y1, z0), (x1, y1, z1), (x1, y0, z1), (x1, y0, z0)]
    elif face == "top":
        pts = [(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)]
    else:
        raise ValueError(face)
    return [project(*p, scale, ox, oy) for p in pts]


def paste_face(canvas, texture, quad, shade=1.0):
    width, height = canvas.size
    (x0, y0), (x1, y1), (x2, y2), (x3, y3) = quad
    ux, uy = x1 - x0, y1 - y0
    vx, vy = x3 - x0, y3 - y0
    det = ux * vy - uy * vx
    if abs(det) < 1e-6:
        return
    a = vy / det
    b = -vx / det
    c = (vx * y0 - vy * x0) / det
    d = -uy / det
    e = ux / det
    f = (uy * x0 - ux * y0) / det

    tw, th = texture.size
    warped = texture.transform(
        (width, height),
        Image.AFFINE,
        (a * tw, b * tw, c * tw, d * th, e * th, f * th),
        resample=Image.NEAREST,
    )
    if shade != 1.0:
        overlay = Image.new("RGBA", warped.size, (0, 0, 0, int(255 * (1.0 - shade))))
        warped = Image.alpha_composite(warped, overlay)
    mask = Image.new("L", (width, height), 0)
    ImageDraw.Draw(mask).polygon(quad, fill=255)
    canvas.paste(warped, (0, 0), mask)


def load_rgba(path):
    return Image.open(path).convert("RGBA")


def render(item_id: str, kind: str, size=(640, 620), scale=22):
    model_path = MODELS / f"{kind}_{item_id}.json"
    data = json.loads(model_path.read_text(encoding="utf-8"))
    # Resolve the child model's label override, then the base model.
    label = None
    if "textures" in data and "label" in data["textures"]:
        label = data["textures"]["label"]
    base = json.loads((MODELS / f"{kind}_base.json").read_text(encoding="utf-8"))
    element = base["elements"][0]
    x0, y0, z0 = element["from"]
    x1, y1, z1 = element["to"]
    box = (x0, y0, z0, x1, y1, z1)

    top_path = TEX / "top" / f"{kind}_{item_id}.png"
    if not top_path.exists():
        top_path = TEX / f"{kind}_top.png"
    textures = {
        "north": load_rgba(TEX / "label" / f"{kind}_{item_id}.png"),
        "east": load_rgba(TEX / f"{kind}_side.png"),
        "top": load_rgba(top_path),
    }

    canvas = Image.new("RGBA", size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(canvas)
    # soft backdrop
    for i in range(size[1]):
        t = i / size[1]
        color = (int(238 - 18 * t), int(235 - 18 * t), int(226 - 16 * t), 255)
        draw.line([(0, i), (size[0], i)], fill=color)

    ox = size[0] * 0.5
    oy = size[1] * 0.58

    # centre the projected cuboid
    corners = [
        project(x, y, z, scale, 0, 0)
        for x in (x0, x1) for y in (y0, y1) for z in (z0, z1)
    ]
    min_x = min(p[0] for p in corners)
    max_x = max(p[0] for p in corners)
    min_y = min(p[1] for p in corners)
    max_y = max(p[1] for p in corners)
    ox -= (min_x + max_x) / 2
    oy -= (min_y + max_y) / 2
    oy += 20

    # shadow
    shadow = Image.new("RGBA", size, (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse(
        [ox - 150, oy + 40, ox + 150, oy + 100], fill=(60, 50, 35, 70)
    )
    shadow = shadow.filter(__import__("PIL.ImageFilter", fromlist=["ImageFilter"]).GaussianBlur(10))
    canvas = Image.alpha_composite(canvas, shadow)
    draw = ImageDraw.Draw(canvas)

    paste_face(canvas, textures["top"], quad_points("top", box, scale, ox, oy), 1.0)
    paste_face(canvas, textures["east"], quad_points("east", box, scale, ox, oy), 0.82)
    paste_face(canvas, textures["north"], quad_points("north", box, scale, ox, oy), 0.92)

    label_font = ImageFont.truetype("C:/Windows/Fonts/arialbd.ttf", 22)
    small = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 16)
    title = item_id.replace("_", " ").upper()
    text_box = draw.textbbox((0, 0), title, font=label_font)
    draw.text(((size[0] - (text_box[2] - text_box[0])) / 2, 18), title, font=label_font, fill=(52, 44, 32, 255))
    caption = "BOX" if kind == "box" else "CRATE"
    cap_box = draw.textbbox((0, 0), caption, font=small)
    draw.text(((size[0] - (cap_box[2] - cap_box[0])) / 2, size[1] - 34), caption, font=small, fill=(110, 96, 72, 255))
    return canvas


def all_ids(kind: str):
    ids = []
    for path in MODELS.glob(f"{kind}_*.json"):
        name = path.stem
        if name in (f"{kind}_base", f"{kind}_generic"):
            continue
        ids.append(name[len(kind) + 1:])
    return sorted(ids)


def main():
    samples = ["556x45", "762x39", "12g", "338", "generic"]
    for kind in ("box", "crate"):
        tiles = [render(sample, kind) for sample in samples]
        sheet = Image.new("RGBA", (len(tiles) * 320, 310), (0, 0, 0, 0))
        for index, tile in enumerate(tiles):
            tile = tile.resize((320, 310), Image.LANCZOS)
            sheet.alpha_composite(tile, (index * 320, 0))
        sheet.convert("RGB").save(OUT / f"{kind}_preview.png")

    # close-up
    for kind in ("box", "crate"):
        detail = render("556x45", kind, size=(900, 820), scale=32)
        detail.convert("RGB").save(OUT / f"{kind}_detail.png")

    # full set, 6 x 4
    for kind in ("box", "crate"):
        ids = all_ids(kind)
        tiles = [render(item_id, kind, size=(400, 390), scale=14) for item_id in ids]
        columns, rows = 6, (len(tiles) + 5) // 6
        tile_w, tile_h = 200, 195
        sheet = Image.new("RGB", (columns * tile_w, rows * tile_h), (244, 240, 230))
        for index, tile in enumerate(tiles):
            tile = tile.resize((tile_w, tile_h), Image.LANCZOS).convert("RGB")
            sheet.paste(tile, ((index % columns) * tile_w, (index // columns) * tile_h))
        sheet.save(OUT / f"{kind}_all.png")

    # label contact sheet
    labels = sorted((TEX / "label").glob("*.png"))
    columns, cell = 6, 96
    rows = (len(labels) + columns - 1) // columns
    sheet = Image.new("RGB", (columns * cell, rows * (cell + 18)), (244, 240, 230))
    draw = ImageDraw.Draw(sheet)
    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", 10)
    for index, path in enumerate(labels):
        x = (index % columns) * cell
        y = (index // columns) * (cell + 18)
        img = Image.open(path).convert("RGB").resize((cell - 8, cell - 8), Image.NEAREST)
        sheet.paste(img, (x + 4, y + 4))
        name = path.stem
        box = draw.textbbox((0, 0), name, font=font)
        draw.text((x + (cell - (box[2] - box[0])) / 2, y + cell - 2), name, font=font, fill=(60, 52, 40))
    sheet.save(OUT / "labels_contact.png")
    print("wrote previews to", OUT)


if __name__ == "__main__":
    main()
