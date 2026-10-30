"""Generate placeholder item models/textures for TACZ Ammo Box.

Convention (Blockbench hand-off):
  - models/item/box_base.json     : the 3D geometry, UVs and display transforms.
                                    Replace this with your Blockbench export.
  - models/item/crate_base.json   : same for the crate.
  - models/item/box_<id>.json     : tiny child model, only overrides the label texture.
  - textures/item/box_side.png    : shared case side texture (32x32).
  - textures/item/box_top.png     : shared case top.
  - textures/item/box_bottom.png  : shared case bottom.
  - textures/item/crate_side.png  : shared crate side.
  - textures/item/crate_top.png   : shared crate top.
  - textures/item/crate_bottom.png: shared crate bottom.
  - textures/item/label/box_<id>.png   : full front-face texture (case + label).
  - textures/item/label/crate_<id>.png : full front-face texture (crate + plate).
"""
import json
import re
import zipfile
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
ASSETS = ROOT / "src/main/resources/assets/taczammobox"
ITEM_MODELS = ASSETS / "models/item"
BLOCK_MODELS = ASSETS / "models/block"
BLOCKSTATES = ASSETS / "blockstates"
ITEM_TEXTURES = ASSETS / "textures/item"
LABELS = ITEM_TEXTURES / "label"
TOPS = ITEM_TEXTURES / "top"
RECIPES = ROOT / "src/main/resources/data/taczammobox/recipes"
LOOT = ROOT / "src/main/resources/data/taczammobox/loot_tables/blocks"

SIZE = 32

TYPES = [
    ("12g", "12 GAUGE", "#C2574A"),
    ("22wmr", ".22 WMR", "#D8B25E"),
    ("30_06", ".30-06", "#B98A4E"),
    ("308", ".308", "#C49A54"),
    ("338", ".338 LM", "#8E9BB0"),
    ("357mag", ".357 MAG", "#C9A24E"),
    ("40mm", "40 MM", "#7E9A5A"),
    ("45_70", ".45-70", "#B97B45"),
    ("45acp", ".45 ACP", "#D2A85E"),
    ("46x30", "4.6x30", "#8FB25E"),
    ("500mag", ".500 MAG", "#D9B25E"),
    ("50ae", ".50 AE", "#CFA554"),
    ("50bmg", ".50 BMG", "#7C8FA8"),
    ("545x39", "5.45x39", "#91B05A"),
    ("556x45", "5.56x45", "#9CB25E"),
    ("57x28", "5.7x28", "#A5B25E"),
    ("58x42", "5.8x42", "#96B262"),
    ("68x51fury", "6.8x51", "#B09354"),
    ("762x25", "7.62x25", "#C09A5C"),
    ("762x39", "7.62x39", "#B09256"),
    ("762x54", "7.62x54R", "#B98A4E"),
    ("792x57", "8x57 M", "#B07E45"),
    ("9mm", "9x19", "#D3A96A"),
    ("rpg_rocket", "RPG-7", "#6E8A52"),
]

FONT_BOLD = "C:/Windows/Fonts/arialbd.ttf"
FONT_REGULAR = "C:/Windows/Fonts/arial.ttf"

# Big, readable code printed on the 32px label. Full caliber stays in the tooltip.
SHORT = {
    "12g": "12G",
    "22wmr": "22W",
    "30_06": "3006",
    "308": "308",
    "338": "338",
    "357mag": "357",
    "40mm": "40M",
    "45_70": "4570",
    "45acp": "45A",
    "46x30": "4630",
    "500mag": "500",
    "50ae": "50A",
    "50bmg": "50B",
    "545x39": "545",
    "556x45": "556",
    "57x28": "5728",
    "58x42": "5842",
    "68x51fury": "6851",
    "762x25": "7625",
    "762x39": "762",
    "762x54": "7654",
    "792x57": "857",
    "9mm": "9MM",
    "rpg_rocket": "RPG",
}


def rgb(hex_color: str):
    value = hex_color.lstrip("#")
    return tuple(int(value[i:i + 2], 16) for i in (0, 2, 4))


def shade(color, factor: float):
    return tuple(max(0, min(255, int(c * factor))) for c in color)


def load_font(size: int, bold: bool = True):
    path = FONT_BOLD if bold else FONT_REGULAR
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


# --------------------------------------------------------------------------- #
# shared textures
# --------------------------------------------------------------------------- #

CASE_BASE = rgb("#34383C")
CASE_EDGE = rgb("#1F2225")
CASE_HIGHLIGHT = rgb("#4B5257")
CASE_GROOVE = rgb("#26292C")


def draw_case_side():
    img = Image.new("RGBA", (SIZE, SIZE), CASE_BASE)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, SIZE - 1, 1], fill=CASE_HIGHLIGHT)
    draw.rectangle([0, SIZE - 2, SIZE - 1, SIZE - 1], fill=CASE_EDGE)
    # panel grooves
    for y in (9, 20):
        draw.line([(0, y), (SIZE - 1, y)], fill=CASE_GROOVE)
        draw.line([(0, y + 1), (SIZE - 1, y + 1)], fill=CASE_HIGHLIGHT)
    # subtle vertical seam
    draw.line([(SIZE // 2, 2), (SIZE // 2, SIZE - 3)], fill=shade(CASE_BASE, 0.9))
    return img


def draw_case_top():
    img = Image.new("RGBA", (SIZE, SIZE), CASE_BASE)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, SIZE - 1, 1], fill=CASE_HIGHLIGHT)
    draw.rectangle([0, SIZE - 2, SIZE - 1, SIZE - 1], fill=CASE_EDGE)
    # recessed panel
    draw.rectangle([4, 4, SIZE - 5, SIZE - 5], fill=shade(CASE_BASE, 0.92), outline=CASE_HIGHLIGHT)
    # latch strip
    draw.rectangle([9, 14, SIZE - 10, 17], fill=rgb("#5A6167"))
    draw.rectangle([9, 14, SIZE - 10, 15], fill=rgb("#717A81"))
    for x in (10, SIZE - 11):
        draw.point((x, 16), fill=CASE_EDGE)
    return img


def draw_case_bottom():
    img = Image.new("RGBA", (SIZE, SIZE), shade(CASE_BASE, 0.9))
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, SIZE - 1, SIZE - 1], outline=CASE_EDGE)
    for x, y in [(4, 4), (SIZE - 5, 4), (4, SIZE - 5), (SIZE - 5, SIZE - 5)]:
        draw.ellipse([x - 1, y - 1, x + 1, y + 1], fill=CASE_EDGE)
    return img


WOOD = rgb("#8C6B3F")
WOOD_DARK = rgb("#5E4526")
WOOD_LIGHT = rgb("#A5814F")


def draw_crate_side():
    img = Image.new("RGBA", (SIZE, SIZE), WOOD)
    draw = ImageDraw.Draw(img)
    for y in range(0, SIZE, 8):
        draw.rectangle([0, y, SIZE - 1, y + 7], fill=WOOD)
        draw.line([(0, y), (SIZE, y)], fill=WOOD_DARK, width=1)
        draw.line([(0, y + 1), (SIZE, y + 1)], fill=WOOD_LIGHT, width=1)
        for x in range(4, SIZE, 9):
            draw.line([(x, y + 3), (x + 1, y + 6)], fill=shade(WOOD, 0.9), width=1)
    # metal corner brackets
    for cx, cy in [(0, 0), (SIZE - 1, 0), (0, SIZE - 1), (SIZE - 1, SIZE - 1)]:
        sx = 0 if cx == 0 else SIZE - 6
        sy = 0 if cy == 0 else SIZE - 6
        draw.rectangle([sx, sy, sx + 5, sy + 5], fill=rgb("#5A5F63"), outline=rgb("#33383B"))
        draw.point((sx + 2, sy + 2), fill=rgb("#C9CED2"))
    return img


def draw_crate_top():
    img = draw_crate_side()
    draw = ImageDraw.Draw(img)
    draw.line([(3, 3), (SIZE - 4, SIZE - 4)], fill=rgb("#6B4E2C"), width=2)
    draw.line([(SIZE - 4, 3), (3, SIZE - 4)], fill=rgb("#6B4E2C"), width=2)
    draw.rectangle([1, 1, SIZE - 2, SIZE - 2], outline=rgb("#4A351C"), width=2)
    return img


def draw_crate_bottom():
    return draw_crate_side()


# --------------------------------------------------------------------------- #
# bullet artwork for the top face
# --------------------------------------------------------------------------- #

BRASS = rgb("#B08D57")
BRASS_DARK = rgb("#8A6B3E")
COPPER = rgb("#C07A4A")
STEEL = rgb("#9AA3A8")
HULL_RED = rgb("#B23A32")
OUTLINE = rgb("#1A1D1F")


def bullet_style(item_id: str) -> str:
    if item_id == "12g":
        return "shotgun"
    if item_id == "rpg_rocket":
        return "rocket"
    if item_id == "40mm":
        return "grenade"
    if item_id in {"9mm", "45acp", "50ae", "357mag", "22wmr", "500mag", "762x25", "57x28", "46x30"}:
        return "pistol"
    return "rifle"


def draw_cartridge_h(draw, x, cy, length, width, tip_color, accent):
    half = max(2, width // 2)
    tip_len = max(6, int(length * 0.45))
    body_len = length - tip_len
    # case
    draw.rectangle([x + 1, cy - half, x + body_len, cy + half], fill=BRASS, outline=OUTLINE)
    draw.line([(x + 2, cy + half - 1), (x + body_len - 1, cy + half - 1)], fill=BRASS_DARK)
    # primer
    draw.ellipse([x + 2, cy - 1, x + 3, cy + 1], fill=BRASS_DARK, outline=OUTLINE)
    # rim
    draw.rectangle([x, cy - half - 1, x + 1, cy + half + 1], fill=BRASS_DARK, outline=OUTLINE)
    # ogive projectile
    points = [
        (x + body_len, cy - half),
        (x + body_len + 2, cy - half),
        (x + length - 1, cy - 1),
        (x + length, cy),
        (x + length - 1, cy + 1),
        (x + body_len + 2, cy + half),
        (x + body_len, cy + half),
    ]
    draw.polygon(points, fill=tip_color, outline=OUTLINE)
    # case-mouth accent ring
    draw.rectangle([x + body_len - 2, cy - half, x + body_len - 1, cy + half], fill=accent)


def draw_shell_h(draw, x, cy, length, width, accent):
    half = max(1, width // 2)
    base = max(4, length // 4)
    draw.rectangle([x, cy - half, x + length - base, cy + half], fill=HULL_RED, outline=OUTLINE)
    draw.rectangle([x + length - base, cy - half, x + length, cy + half], fill=BRASS, outline=OUTLINE)
    draw.rectangle([x, cy - half, x + length - base, cy - half + 1], fill=accent)


def draw_rocket_h(draw, x, cy, length, width, accent):
    half = max(2, width // 2)
    nose = max(4, length // 5)
    draw.rectangle([x, cy - half, x + length - nose, cy + half], fill=rgb("#5E6A5A"), outline=OUTLINE)
    draw.polygon([(x + length - nose, cy - half), (x + length, cy), (x + length - nose, cy + half)],
                 fill=STEEL, outline=OUTLINE)
    draw.rectangle([x, cy - half, x + 1, cy + half + 2], fill=STEEL, outline=OUTLINE)
    draw.rectangle([x + 3, cy - half - 2, x + 4, cy - half + 2], fill=STEEL, outline=OUTLINE)
    draw.rectangle([x + 3, cy + half - 2, x + 4, cy + half + 2], fill=STEEL, outline=OUTLINE)
    draw.rectangle([x + 5, cy - half, x + 6, cy + half], fill=accent)


def draw_grenade_h(draw, x, cy, length, width, accent):
    half = max(2, width // 2)
    draw.rounded_rectangle([x, cy - half, x + length - 3, cy + half], radius=2,
                           fill=rgb("#6E7A4E"), outline=OUTLINE)
    draw.polygon([(x + length - 4, cy - half + 1), (x + length, cy), (x + length - 4, cy + half - 1)],
                 fill=STEEL, outline=OUTLINE)
    draw.rectangle([x + 2, cy - half, x + 3, cy + half], fill=accent)


def draw_case_top_plain():
    img = Image.new("RGBA", (SIZE, SIZE), CASE_BASE)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, SIZE - 1, 1], fill=CASE_HIGHLIGHT)
    draw.rectangle([0, SIZE - 2, SIZE - 1, SIZE - 1], fill=CASE_EDGE)
    draw.rectangle([2, 2, SIZE - 3, SIZE - 3], fill=shade(CASE_BASE, 0.94), outline=CASE_HIGHLIGHT)
    return img


def paint_bullets(draw, item_id: str, accent, rows, x, length, width, gap):
    style = bullet_style(item_id)
    for index in range(rows):
        cy = 15 if rows == 1 else 8 + index * gap
        if style == "shotgun":
            draw_shell_h(draw, x, cy, length, width, accent)
        elif style == "rocket":
            draw_rocket_h(draw, x, cy, length, width, accent)
        elif style == "grenade":
            draw_grenade_h(draw, x, cy, length, width, accent)
        else:
            tip = COPPER if style == "rifle" else STEEL
            draw_cartridge_h(draw, x, cy, length, width, tip, accent)


def draw_box_top_art(item_id: str, color_hex: str):
    img = draw_case_top_plain()
    draw = ImageDraw.Draw(img)
    accent = rgb(color_hex)
    icon = tacz_ammo_icon(item_id)
    if icon is not None:
        icon = icon.resize((SIZE - 4, SIZE - 4), Image.LANCZOS)
        img.alpha_composite(icon, (2, 2))
        return img
    style = bullet_style(item_id)
    if style == "shotgun":
        paint_bullets(draw, item_id, accent, 1, 6, 20, 8, 10)
    elif style == "rocket":
        paint_bullets(draw, item_id, accent, 1, 5, 22, 7, 10)
    elif style == "grenade":
        paint_bullets(draw, item_id, accent, 1, 9, 14, 8, 10)
    elif style == "pistol":
        paint_bullets(draw, item_id, accent, 1, 7, 18, 7, 10)
    else:
        paint_bullets(draw, item_id, accent, 1, 6, 20, 7, 10)
    draw.rectangle([2, SIZE - 4, SIZE - 3, SIZE - 3], fill=accent)
    return img


def tacz_ammo_icon(item_id: str):
    entry = f"assets/tacz/custom/tacz_default_gun/assets/tacz/textures/ammo/slot/{item_id}.png"
    try:
        with zipfile.ZipFile(TACZ_JAR) as jar:
            with jar.open(entry) as stream:
                return Image.open(stream).convert("RGBA")
    except Exception as exc:
        print(f"warning: could not read TACZ icon for {item_id}: {exc}")
        return None


def draw_crate_top_art(item_id: str, color_hex: str):
    img = draw_crate_top()
    draw = ImageDraw.Draw(img)
    accent = rgb(color_hex)
    draw.rectangle([2, 2, SIZE - 3, SIZE - 3], fill=rgb("#2E3236"), outline=rgb("#5A5F63"))
    icon = tacz_ammo_icon(item_id)
    if icon is not None:
        icon = icon.resize((SIZE - 4, SIZE - 4), Image.LANCZOS)
        # The crate top UV is read from the front, so rotate the icon 180° to
        # keep the TACZ lettering upright when looking at the labeled side.
        icon = icon.rotate(180)
        img.alpha_composite(icon, (2, 2))
        return img
    style = bullet_style(item_id)
    if style == "shotgun":
        paint_bullets(draw, item_id, accent, 1, 7, 18, 7, 9)
    elif style == "rocket":
        paint_bullets(draw, item_id, accent, 1, 6, 20, 6, 9)
    elif style == "grenade":
        paint_bullets(draw, item_id, accent, 1, 10, 12, 7, 9)
    elif style == "pistol":
        paint_bullets(draw, item_id, accent, 1, 8, 16, 6, 9)
    else:
        paint_bullets(draw, item_id, accent, 1, 7, 18, 6, 9)
    draw.rectangle([4, SIZE - 7, SIZE - 5, SIZE - 6], fill=accent)
    return img


def fit_font(draw, text, max_width, start=9, minimum=4):
    for size in range(start, minimum - 1, -1):
        font = load_font(size)
        box = draw.textbbox((0, 0), text, font=font)
        if box[2] - box[0] <= max_width:
            return font
    return load_font(minimum)


def centered(draw, text, y, font, fill):
    box = draw.textbbox((0, 0), text, font=font)
    x = (SIZE - (box[2] - box[0])) // 2 - box[0]
    draw.text((x, y), text, font=font, fill=fill)


def draw_box_label(label: str, color_hex: str):
    img = draw_case_side()
    draw = ImageDraw.Draw(img)
    color = rgb(color_hex)
    # clean product label
    draw.rounded_rectangle([2, 3, SIZE - 3, SIZE - 4], radius=2,
                           fill=rgb("#EDEFF1"), outline=rgb("#1B1E21"))
    draw.rectangle([2, 3, SIZE - 3, 8], fill=color)
    draw.line([(2, 8), (SIZE - 3, 8)], fill=shade(color, 0.7))
    font = fit_font(draw, label, SIZE - 9, start=12, minimum=7)
    centered(draw, label, 10, font, rgb("#1A1D1F"))
    small = load_font(4, bold=False)
    centered(draw, "AMMO", 21, small, rgb("#5B6065"))
    return img


def draw_crate_label(label: str, color_hex: str):
    img = draw_crate_side()
    draw = ImageDraw.Draw(img)
    color = rgb(color_hex)
    draw.rectangle([2, 4, SIZE - 3, SIZE - 5], fill=rgb("#A7ADB0"), outline=rgb("#3F4447"))
    draw.rectangle([2, 4, SIZE - 3, 9], fill=color)
    draw.rectangle([2, 4, SIZE - 3, SIZE - 5], outline=rgb("#3F4447"))
    font = fit_font(draw, label, SIZE - 9, start=12, minimum=7)
    centered(draw, label, 11, font, rgb("#1A1D1F"))
    small = load_font(4, bold=False)
    centered(draw, "AMMO", 21, small, rgb("#3A3F42"))
    return img


# --------------------------------------------------------------------------- #
# models
# --------------------------------------------------------------------------- #

def base_model(kind: str, label_default: str, is_crate: bool):
    if is_crate:
        elements = [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#label"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "east": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "west": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}
            }
        }]
        gui_scale = 0.625
    else:
        # flat cartridge case: 15 wide, 6 tall, 8 deep
        elements = [{
            "from": [0.5, 5, 4],
            "to": [15.5, 11, 12],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#label"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "east": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "west": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}
            }
        }]
        gui_scale = 0.8

    return {
        "credit": "Placeholder geometry. Replace with your Blockbench Java Item Model export.",
        "texture_size": [SIZE, SIZE],
        "gui_light": "side",
        "textures": {
            "particle": f"taczammobox:item/{kind}_side",
            "side": f"taczammobox:item/{kind}_side",
            "top": f"taczammobox:item/top/{kind}_generic",
            "bottom": f"taczammobox:item/{kind}_bottom",
            "label": label_default
        },
        "elements": elements,
        "display": {
            "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0],
                    "scale": [gui_scale, gui_scale, gui_scale]},
            "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
            "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
            "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 2, 0],
                                      "scale": [0.4, 0.4, 0.4]},
            "thirdperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 3, 0],
                                      "scale": [0.375, 0.375, 0.375]}
        }
    }


def dump(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def dump_if_absent(path: Path, data):
    """Keep hand-made Blockbench models: only write the base model when missing."""
    if not path.exists():
        dump(path, data)


def block_base_model():
    return {
        "textures": {
            "particle": "taczammobox:item/crate_side",
            "side": "taczammobox:item/crate_side",
            "top": "taczammobox:item/top/crate_generic",
            "bottom": "taczammobox:item/crate_bottom",
            "label": "taczammobox:item/label/crate_generic"
        },
        "elements": [{
            "from": [0, 0, 0],
            "to": [16, 16, 16],
            "faces": {
                "north": {"uv": [0, 0, 16, 16], "texture": "#label"},
                "south": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "east": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "west": {"uv": [0, 0, 16, 16], "texture": "#side"},
                "up": {"uv": [0, 0, 16, 16], "texture": "#top"},
                "down": {"uv": [0, 0, 16, 16], "texture": "#bottom"}
            }
        }]
    }


def write_block_assets(item_id: str):
    dump(BLOCK_MODELS / f"crate_{item_id}.json", {
        "parent": "taczammobox:block/crate_base",
        "textures": {
            "label": f"taczammobox:item/label/crate_{item_id}",
            "top": f"taczammobox:item/top/crate_{item_id}",
            "side": "taczammobox:item/crate_side",
            "bottom": "taczammobox:item/crate_bottom"
        }
    })
    model = f"taczammobox:block/crate_{item_id}"
    dump(BLOCKSTATES / f"crate_{item_id}.json", {
        "variants": {
            "facing=north": {"model": model},
            "facing=east": {"model": model, "y": 90},
            "facing=south": {"model": model, "y": 180},
            "facing=west": {"model": model, "y": 270}
        }
    })
    dump(LOOT / f"crate_{item_id}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "bonus_rolls": 0,
            "entries": [{"type": "minecraft:item", "name": f"taczammobox:crate_{item_id}"}]
        }],
        "conditions": [{"condition": "minecraft:survives_explosion"}]
    })


TACZ_JAR = ROOT / "libs" / "tacz-1.20.1-1.1.8-hotfix2.jar"
_STACK_CACHE = {}


def ammo_stack_size(item_id: str) -> int:
    """Read TACZ's own stack_size so one box always equals one stack."""
    if item_id in _STACK_CACHE:
        return _STACK_CACHE[item_id]
    size = 60
    try:
        with zipfile.ZipFile(TACZ_JAR) as jar:
            entry = f"assets/tacz/custom/tacz_default_gun/data/tacz/index/ammo/{item_id}.json"
            text = jar.read(entry).decode("utf-8")
            text = re.sub(r"(?m)//.*$", "", text)
            data = json.loads(text)
            size = int(data.get("stack_size", 60))
    except Exception as exc:  # noqa: BLE001 - placeholder fallback
        print(f"warning: could not read stack_size for {item_id}: {exc}")
    _STACK_CACHE[item_id] = size
    return size


def write_recipes(item_id: str):
    ammo_id = f"tacz:{item_id}"
    box = f"taczammobox:box_{item_id}"
    crate = f"taczammobox:crate_{item_id}"

    dump(RECIPES / f"{item_id}_crate.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": box} for _ in range(9)],
        "result": {"item": crate, "count": 1}
    })
    dump(RECIPES / f"{item_id}_boxes.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": crate}],
        "result": {"item": box, "count": 9}
    })
    dump(RECIPES / f"{item_id}_ammo.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [{"item": box}],
        "result": {
            "item": "tacz:ammo",
            "count": ammo_stack_size(item_id),
            "nbt": "{AmmoId:\"" + ammo_id + "\"}"
        }
    })


def main():
    ITEM_MODELS.mkdir(parents=True, exist_ok=True)
    LABELS.mkdir(parents=True, exist_ok=True)
    TOPS.mkdir(parents=True, exist_ok=True)
    BLOCK_MODELS.mkdir(parents=True, exist_ok=True)
    BLOCKSTATES.mkdir(parents=True, exist_ok=True)
    LOOT.mkdir(parents=True, exist_ok=True)

    dump(BLOCK_MODELS / "crate_base.json", block_base_model())

    draw_case_side().save(ITEM_TEXTURES / "box_side.png")
    draw_case_top().save(ITEM_TEXTURES / "box_top.png")
    draw_case_bottom().save(ITEM_TEXTURES / "box_bottom.png")
    draw_crate_side().save(ITEM_TEXTURES / "crate_side.png")
    draw_crate_top().save(ITEM_TEXTURES / "crate_top.png")
    draw_crate_bottom().save(ITEM_TEXTURES / "crate_bottom.png")

    draw_box_label("AMMO", "#9CB25E").save(LABELS / "box_generic.png")
    draw_crate_label("AMMO", "#9CB25E").save(LABELS / "crate_generic.png")
    draw_box_top_art("generic", "#9CB25E").save(TOPS / "box_generic.png")
    draw_crate_top_art("generic", "#9CB25E").save(TOPS / "crate_generic.png")

    dump_if_absent(ITEM_MODELS / "box_base.json",
                   base_model("box", "taczammobox:item/label/box_generic", is_crate=False))
    dump_if_absent(ITEM_MODELS / "crate_base.json",
                   base_model("crate", "taczammobox:item/label/crate_generic", is_crate=True))

    # Hidden anchors used by the client fallback for config-only ammo types.
    dump(ITEM_MODELS / "box_generic.json", {
        "parent": "taczammobox:item/box_base",
        "textures": {
            "label": "taczammobox:item/label/box_generic",
            "top": "taczammobox:item/top/box_generic"
        }
    })
    dump(ITEM_MODELS / "crate_generic.json", {
        "parent": "taczammobox:item/crate_base",
        "textures": {
            "label": "taczammobox:item/label/crate_generic",
            "top": "taczammobox:item/top/crate_generic"
        }
    })

    for item_id, label, color in TYPES:
        short = SHORT[item_id]
        draw_box_label(short, color).save(LABELS / f"box_{item_id}.png")
        draw_crate_label(short, color).save(LABELS / f"crate_{item_id}.png")
        draw_box_top_art(item_id, color).save(TOPS / f"box_{item_id}.png")
        draw_crate_top_art(item_id, color).save(TOPS / f"crate_{item_id}.png")

        dump(ITEM_MODELS / f"box_{item_id}.json", {
            "parent": "taczammobox:item/box_base",
            "textures": {
                "label": f"taczammobox:item/label/box_{item_id}",
                "top": f"taczammobox:item/top/box_{item_id}"
            }
        })
        dump(ITEM_MODELS / f"crate_{item_id}.json", {
            "parent": "taczammobox:item/crate_base",
            "textures": {
                "label": f"taczammobox:item/label/crate_{item_id}",
                "top": f"taczammobox:item/top/crate_{item_id}"
            }
        })
        write_recipes(item_id)
        write_block_assets(item_id)

    print(f"generated {len(TYPES)} box + {len(TYPES)} crate models and textures at {SIZE}x{SIZE}")


if __name__ == "__main__":
    main()
