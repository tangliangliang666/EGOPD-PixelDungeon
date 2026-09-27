# -*- coding: utf-8 -*-
"""把趣味挑战三枚图标裁出来、×8 放大拼成一排，供人眼确认（只读 icons.png）。"""
from PIL import Image

SRC = r"D:\PD\core\src\main\assets\interfaces\icons.png"
OUT = r"D:\PD\_chk\_fun_icons_preview.png"
CELLS = [("grey", 128, 48), ("lit", 144, 48), ("count", 160, 48)]
S, Z = 16, 8

im = Image.open(SRC).convert("RGBA")
tile = Image.new("RGBA", (S * Z * len(CELLS) + 8 * (len(CELLS) - 1), S * Z), (40, 40, 48, 255))
x = 0
for name, cx, cy in CELLS:
    c = im.crop((cx, cy, cx + S, cy + S)).resize((S * Z, S * Z), Image.NEAREST)
    tile.paste(c, (x, 0), c)
    x += S * Z + 8
tile.save(OUT)
print("saved", OUT, tile.size)
