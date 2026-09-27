from PIL import Image

p = "core/src/main/assets/interfaces/talent_icons.png"
img = Image.open(p).convert("RGBA")
print("size", img.size)
W, H = img.size
CELL = 16
cols = W // CELL
rows = H // CELL
print("cols", cols, "rows", rows, "frames", cols * rows)
# 第 11 行（row index 10）=> 帧 10*cols .. 10*cols+cols-1
row = 10
base = row * cols
out = Image.new("RGBA", (CELL * 3 * cols, CELL * 3), (40, 40, 48, 255))
for c in range(cols):
    f = img.crop((c * CELL, row * CELL, c * CELL + CELL, row * CELL + CELL))
    nz = sum(1 for px in f.getdata() if px[3] > 0)
    print("frame", base + c, "col", c, "opaque", nz)
    out.alpha_composite(f.resize((CELL * 3, CELL * 3), Image.NEAREST), (c * CELL * 3, 0))
out.convert("RGB").save("D:/PD/_chk/talent_row11.png")
