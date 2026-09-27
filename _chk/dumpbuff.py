import os
from PIL import Image

OUT = "D:/PD/_chk"
os.makedirs(OUT, exist_ok=True)

def strip(path, cell, cols, start, end, scale, name):
    img = Image.open(path).convert("RGBA")
    n = end - start + 1
    out = Image.new("RGBA", (cell * scale * n, cell * scale), (40, 40, 48, 255))
    for i, idx in enumerate(range(start, end + 1)):
        c = idx % cols
        r = idx // cols
        box = (c * cell, r * cell, c * cell + cell, r * cell + cell)
        f = img.crop(box)
        opaque = sum(1 for p in f.convert("RGBA").getdata() if p[3] > 0)
        f = f.resize((cell * scale, cell * scale), Image.NEAREST)
        out.alpha_composite(f, (i * cell * scale, 0))
        print("%s idx=%d col=%d row=%d opaque=%d" % (name, idx, c, r, opaque))
    out.convert("RGB").save(os.path.join(OUT, name + ".png"))

# buffs.png: 128x64, 7px cells -> 18 cols x 9 rows = 162 frames
strip("core/src/main/assets/interfaces/buffs.png", 7, 18, 112, 121, 8, "buff_small")
# large_buffs.png: 256x128, 16px cells -> 16 cols x 8 rows = 128 frames
strip("core/src/main/assets/interfaces/large_buffs.png", 16, 16, 112, 121, 4, "buff_large")
