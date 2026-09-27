# -*- coding: utf-8 -*-
"""对比三枚计数图标（CHAL / TRIAL / FUN）的 7×7 字形是否逐像素相同，并出一张放大预览图。
只读 icons.png。"""
import sys, os

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
from verify_sprite_frames import decode_png

P = os.path.join(ROOT, 'core/src/main/assets/interfaces/icons.png')
W, H, CT, NCH, PLTE, TRNS, PX = decode_png(P)


def px(x, y):
    i = (y * W + x) * NCH
    return (PX[i], PX[i + 1], PX[i + 2], PX[i + 3])


def cell(x0, y0, w=7, h=7):
    return tuple(px(x0 + dx, y0 + dy) for dy in range(h) for dx in range(w))


cells = {
    'CHAL_COUNT': cell(160, 80),
    'TRIAL_COUNT': cell(160, 88),
    'FUN_COUNT': cell(160, 48),
}
names = list(cells)
for a in range(len(names)):
    for b in range(a + 1, len(names)):
        same = cells[names[a]] == cells[names[b]]
        print('%s vs %s : %s' % (names[a], names[b], '逐像素相同' if same else '不同'))

print()
for n in names:
    dif = sum(1 for p in cells[n] if p[3] > 0)
    print('%-12s 非透明像素 = %d' % (n, dif))

# 出一张 ×8 放大的并排预览图（不需要 PIL：直接手写一个最小 PNG）
Z = 8
GW, GH = (7 * Z + 4) * 3, 7 * Z
canvas = [[(30, 30, 36, 255)] * GW for _ in range(GH)]
for k, n in enumerate(names):
    ox = k * (7 * Z + 4)
    for dy in range(7):
        for dx in range(7):
            c = cells[n][dy * 7 + dx]
            for zy in range(Z):
                for zx in range(Z):
                    canvas[dy * Z + zy][ox + dx * Z + zx] = c


def write_png(path, pix, w, h):
    import struct, zlib
    raw = b''.join(b'\x00' + bytes(v for pxl in row for v in pxl) for row in pix)
    def chunk(t, d):
        c = t + d
        return struct.pack('>I', len(d)) + c + struct.pack('>I', zlib.crc32(c) & 0xFFFFFFFF)
    out = b'\x89PNG\r\n\x1a\n'
    out += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    out += chunk(b'IDAT', zlib.compress(raw, 9))
    out += chunk(b'IEND', b'')
    open(path, 'wb').write(out)


OUT = os.path.join(HERE, '_count_icons_compare.png')
write_png(OUT, canvas, GW, GH)
print('\n预览（左→右：CHAL_COUNT / TRIAL_COUNT / FUN_COUNT，×8）=>', OUT)
