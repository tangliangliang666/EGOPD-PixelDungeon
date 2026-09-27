# -*- coding: utf-8 -*-
"""临时只读工具：核对「神圣卡 / 神圣屏障」的两处贴图是否就位。

1) items.png 第 41 行 col 7/8/9 的 16x16 格占用与包围盒（护符 xy(7,41) 14x16、神圣卡 xy(8,41) 15x15）
2) large_buffs.png 帧 114~120（16x16 格）占用与包围盒 —— 期望 116=融化、117=神圣屏障
3) buffs.png 帧 114~120（7x7 格）

用法：python _chk/_dump_holycard.py
"""
import struct
import zlib


def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not png: ' + path
    pos, idat, w, h, bitd, ctype, interlace = 8, b'', 0, 0, 8, 6, 0
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bitd, ctype, _, _, interlace = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
        pos += 12 + ln
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return out, w, h, nch


def cell_info(buf, W, nch, x0, y0, cw, ch):
    opaque, xs, ys = 0, [], []
    for y in range(y0, y0 + ch):
        for x in range(x0, x0 + cw):
            o = (y * W + x) * nch
            a = buf[o + 3] if nch == 4 else 255
            if a > 0:
                opaque += 1
                xs.append(x - x0); ys.append(y - y0)
    bbox = ('bbox x%d-%d(%d) y%d-%d(%d)' % (min(xs), max(xs), max(xs) - min(xs) + 1,
                                            min(ys), max(ys), max(ys) - min(ys) + 1)) if xs else '空'
    return opaque, bbox


def main():
    # ---- items.png 第 41 行 ----
    buf, W, H, nch = load_png('core/src/main/assets/sprites/items.png')
    print('items.png = %dx%d nch=%d => %d 列 x %d 行' % (W, H, nch, W // 16, H // 16))
    y0 = (41 - 1) * 16
    for col in (7, 8, 9):
        x0 = (col - 1) * 16
        opaque, bbox = cell_info(buf, W, nch, x0, y0, 16, 16)
        print('  row41 col %d  x=%3d..%3d  不透明=%4d  %s' % (col, x0, x0 + 15, opaque, bbox))

    # ---- large_buffs.png 帧 114~120 ----
    for path, size, frames in [
        ('core/src/main/assets/interfaces/large_buffs.png', 16, (114, 120)),
        ('core/src/main/assets/interfaces/buffs.png', 7, (114, 120)),
    ]:
        buf, W, H, nch = load_png(path)
        cols = W // size
        rows = H // size
        print('\n%s = %dx%d 格 %dpx => %d 列 x %d 行 = %d 帧'
              % (path, W, H, size, cols, rows, cols * rows))
        for f in range(frames[0], frames[1] + 1):
            r, c = f // cols, f % cols
            x0, y0 = c * size, r * size
            if y0 + size > H:
                print('  帧 %3d  (r%d c%d) 超出图片' % (f, r, c)); continue
            opaque, bbox = cell_info(buf, W, nch, x0, y0, size, size)
            print('  帧 %3d  (r%d c%d) x=%3d y=%3d  不透明=%3d  %s'
                  % (f, r, c, x0, y0, opaque, bbox))


if __name__ == '__main__':
    main()
