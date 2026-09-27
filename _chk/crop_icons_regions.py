# 把 icons.png 的若干区域裁出来放大成预览 PNG，便于人眼确认「图标画在哪、有没有越界、和邻居有没有粘连」
import sys, os, struct, zlib
sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
from verify_sprite_frames import decode_png

SRC = 'core/src/main/assets/interfaces/icons.png'
OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '_icons_preview')


def write_png(path, canvas):
    h = len(canvas); w = len(canvas[0]); raw = bytearray()
    for row in canvas:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def ch(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n'
    png += ch(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += ch(b'IDAT', zlib.compress(bytes(raw), 9)) + ch(b'IEND', b'')
    open(path, 'wb').write(png)


w, h, ct, nch, plte, trns, px = decode_png(SRC)
print('icons.png %dx%d colorType=%d nch=%d' % (w, h, ct, nch))


def get(x, y):
    if x < 0 or y < 0 or x >= w or y >= h:
        return (255, 0, 0, 255)          # 越出原图的像素画成红色，一眼可见
    i = (y * w + x) * nch
    if ct == 6:
        r, g, b, a = px[i:i + 4]
    elif ct == 2:
        r, g, b = px[i:i + 3]; a = 255
    elif ct == 3:
        idx = px[i]; r, g, b = plte[idx * 3:idx * 3 + 3]
        a = 255 if (trns is None or idx >= len(trns)) else trns[idx]
    elif ct == 4:
        r = g = b = px[i]; a = px[i + 1]
    else:
        r = g = b = px[i]; a = 255
    # 透明处用浅灰棋盘底衬，半透明按 alpha 与底色混合
    BR, BG, BB = 210, 210, 214
    if a == 0:
        return (BR, BG, BB, 255)
    r = (r * a + BR * (255 - a)) // 255
    g = (g * a + BG * (255 - a)) // 255
    b = (b * a + BB * (255 - a)) // 255
    return (r, g, b, 255)


def crop(x0, y0, cw, chh, scale, path, grid=None):
    canvas = []
    for yy in range(y0, y0 + chh):
        row = []
        for xx in range(x0, x0 + cw):
            row += [get(xx, yy)] * scale
        for _ in range(scale):
            canvas.append(row)
    # 网格线：在放大后的图上，按原图坐标画红/蓝 1px 细线（覆盖在像素上，只用于看对齐）
    if grid:
        for (gx, gy, gcol) in grid:
            px_x = (gx - x0) * scale
            px_y = (gy - y0) * scale
            for s in range(scale):
                if 0 <= px_x + s < cw * scale:
                    for t in range(chh * scale):
                        canvas[t][min(cw * scale - 1, px_x + s)] = gcol
                if 0 <= px_y + s < chh * scale:
                    for t in range(cw * scale):
                        canvas[min(chh * scale - 1, px_y + s)][t] = gcol
    write_png(path, canvas)
    print('写出 %s  (原图区域 x=%d..%d, y=%d..%d, 放大 %dx)'
          % (os.path.basename(path), x0, x0 + cw - 1, y0, y0 + chh - 1, scale))


os.makedirs(OUT, exist_ok=True)
GREEN = (255, 0, 255, 255)   # 洋红线 = 我打算用的矩形边界
crop(192, 16, 64, 48, 8, os.path.join(OUT, 'A_ctx_greycolor.png'),
     grid=[(208, 32, GREEN), (224, 32, GREEN), (240, 32, GREEN), (192, 48, GREEN), (192, 32, GREEN)])
crop(208, 32, 16, 16, 16, os.path.join(OUT, 'B_grey_exact.png'))
crop(224, 32, 16, 16, 16, os.path.join(OUT, 'C_color_exact.png'))
crop(144, 72, 48, 32, 12, os.path.join(OUT, 'D_ctx_count.png'),
     grid=[(160, 80, GREEN), (160, 88, GREEN), (168, 88, GREEN), (168, 96, GREEN), (152, 80, GREEN)])
crop(160, 88, 8, 8, 24, os.path.join(OUT, 'E_count_exact.png'))
