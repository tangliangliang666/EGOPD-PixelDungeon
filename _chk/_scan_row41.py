# -*- coding: utf-8 -*-
"""临时工具：扫描 items.png 第 41 行的 16x16 格占用情况，找出空位。

只读，不改任何文件。用法: python _chk/_scan_row41.py [行号]
"""
import struct
import sys
import zlib

PNG = "core/src/main/assets/sprites/items.png"


def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not png'
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


def main():
    row = int(sys.argv[1]) if len(sys.argv) > 1 else 41
    buf, W, H, nch = load_png(PNG)
    print("items.png = %dx%d nch=%d  => %d 列 x %d 行" % (W, H, nch, W // 16, H // 16))
    y0 = (row - 1) * 16
    if y0 + 16 > H:
        print("行 %d 超出图片高度" % row)
        return 1
    print("\n第 %d 行（y=%d..%d）逐格占用：" % (row, y0, y0 + 15))
    free = []
    for col in range(1, W // 16 + 1):
        x0 = (col - 1) * 16
        opaque = 0
        xs, ys = [], []
        for y in range(y0, y0 + 16):
            for x in range(x0, x0 + 16):
                o = (y * W + x) * nch
                a = buf[o + 3] if nch == 4 else 255
                if a > 0:
                    opaque += 1
                    xs.append(x - x0); ys.append(y - y0)
        tag = "空" if opaque == 0 else "占用"
        bbox = ""
        if xs:
            bbox = " bbox x%d-%d y%d-%d" % (min(xs), max(xs), min(ys), max(ys))
        print("  col %2d  x=%3d..%3d  %-4s 不透明=%4d%s" % (col, x0, x0 + 15, tag, opaque, bbox))
        if opaque == 0:
            free.append(col)
    print("\n空闲列（可放新图标）: %s" % (free if free else "无"))
    return 0


if __name__ == "__main__":
    sys.exit(main())
