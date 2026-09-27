# -*- coding: utf-8 -*-
"""把 items.png 的指定行放大导出，肉眼核验容器格子里画的是什么。
用法: python _chk/_zoom_items_row.py <y0> <y1> <scale> <out>
"""
import struct, sys, zlib
sys.path.insert(0, '_chk')
from png_probe import decode

SRC = 'core/src/main/assets/sprites/items.png'


def write_png(path, canvas):
    h = len(canvas); w = len(canvas[0])
    raw = bytearray()
    for row in canvas:
        raw.append(0)
        for (r, g, b, a) in row:
            raw += bytes((r, g, b, a))

    def chunk(t, d):
        c = struct.pack('>I', len(d)) + t + d
        return c + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    png += chunk(b'IDAT', zlib.compress(bytes(raw), 9))
    png += chunk(b'IEND', b'')
    open(path, 'wb').write(png)


def main():
    y0 = int(sys.argv[1]); y1 = int(sys.argv[2])
    sc = int(sys.argv[3]); out = sys.argv[4]
    w, h, px = decode(SRC)
    cw = w * sc
    canvas = [[(255, 255, 255, 255) for _ in range(cw)] for _ in range((y1 - y0) * sc)]
    for y in range(y0, y1):
        for x in range(w):
            o = (y * w + x) * 4
            r, g, b, a = px[o], px[o + 1], px[o + 2], px[o + 3]
            if a == 0:
                continue
            for dy in range(sc):
                for dx in range(sc):
                    canvas[(y - y0) * sc + dy][x * sc + dx] = (r, g, b, a)
    write_png(out, canvas)
    print('OK %s  %dx%d  (src rows %d..%d, scale %d)' % (out, cw, (y1 - y0) * sc, y0, y1 - 1, sc))


main()
