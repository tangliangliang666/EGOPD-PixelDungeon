# -*- coding: utf-8 -*-
"""极简 PNG 解码探针（无第三方依赖），用于检查 items.png 某个 16px 格子里是否已有像素。
用法: python _chk/png_probe.py <png> <col> <row> [w h]
坐标以 ItemSpriteSheet.xy(col,row) 为准（1-based，格子 16×16）。
"""
import struct
import sys
import zlib


def decode(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not a png'
    pos = 8
    idat = b''
    w = h = bd = ct = inter = 0
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        pos += 12 + ln
        if typ == b'IHDR':
            w, h, bd, ct, _comp, _filt, inter = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
    assert bd == 8 and ct == 6 and inter == 0, 'need 8-bit RGBA non-interlaced'
    raw = zlib.decompress(idat)
    bpp = 4
    stride = w * bpp
    out = bytearray()
    prev = bytearray(stride)
    i = 0
    for _y in range(h):
        f = raw[i]
        i += 1
        line = bytearray(raw[i:i + stride])
        i += stride
        if f == 1:
            for x in range(bpp, stride):
                line[x] = (line[x] + line[x - bpp]) & 255
        elif f == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 255
        elif f == 3:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 255
        elif f == 4:
            for x in range(stride):
                a = line[x - bpp] if x >= bpp else 0
                b = prev[x]
                c = prev[x - bpp] if x >= bpp else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 255
        out += line
        prev = line
    return w, h, out


def alpha_at(w, px, x, y):
    return px[(y * w + x) * 4 + 3]


def main():
    png = sys.argv[1]
    col = int(sys.argv[2])
    row = int(sys.argv[3])
    cw = int(sys.argv[4]) if len(sys.argv) > 4 else 16
    ch = int(sys.argv[5]) if len(sys.argv) > 5 else 16
    w, h, px = decode(png)
    print('image', w, 'x', h, ' -> cols', w // 16, 'rows', h // 16)
    x0 = (col - 1) * 16
    y0 = (row - 1) * 16
    nz = 0
    minx = miny = 9999
    maxx = maxy = -1
    for y in range(y0, min(y0 + ch, h)):
        for x in range(x0, min(x0 + cw, w)):
            if alpha_at(w, px, x, y):
                nz += 1
                minx = min(minx, x - x0)
                miny = min(miny, y - y0)
                maxx = max(maxx, x - x0)
                maxy = max(maxy, y - y0)
    print('xy(%d,%d) region %dx%d -> 非透明像素=%d' % (col, row, cw, ch, nz))
    if nz:
        print('  包围盒 x %d..%d  y %d..%d  (宽%d 高%d)'
              % (minx, maxx, miny, maxy, maxx - minx + 1, maxy - miny + 1))
    # 整行概览
    print('  第 %d 行各列非透明数:' % row, end=' ')
    for c in range(1, w // 16 + 1):
        cnt = 0
        for y in range(y0, y0 + 16):
            for x in range((c - 1) * 16, (c - 1) * 16 + 16):
                if y < h and x < w and alpha_at(w, px, x, y):
                    cnt += 1
        if cnt:
            print('%d:%d' % (c, cnt), end=' ')
    print()


if __name__ == '__main__':
    main()
