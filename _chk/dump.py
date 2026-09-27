import os
from PIL import Image

OUT = "D:/PD/_chk"
os.makedirs(OUT, exist_ok=True)

def dump(path, cell, cols, rows, start, end, scale, prefix):
    img = Image.open(path).convert("RGBA")
    W, H = img.size
    for idx in range(start, end + 1):
        c = idx % cols
        r = idx // cols
        if r >= rows:
            break
        box = (c * cell, r * cell, c * cell + cell, r * cell + cell)
        f = img.crop(box)
        nz = sum(1 for p in f.getdata() if p[3] > 0)
        big = f.resize((cell * scale, cell * scale), Image.NEAREST)
        bg = Image.new("RGBA", big.size, (40, 40, 48, 255))
        bg.alpha_composite(big)
        bg.convert("RGB").save(os.path.join(OUT, "%s_%03d.png" % (prefix, idx)))
        print("%s idx=%d cell(%d,%d) opaque=%d" % (prefix, idx, c, r, nz))

# hero_icons.png: 128x256, 8 cols x 16 rows, 16px cells
dump("core/src/main/assets/interfaces/hero_icons.png", 16, 8, 16, 122, 127, 6, "hero")
