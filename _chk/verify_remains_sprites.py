# -*- coding: utf-8 -*-
"""遗物贴图核验（2026-09-18）：
用纯 Python 解码 items.png（无 PIL），逐个 dump 遗物所在矩形，
报告不透明像素数并打印 ASCII 预览，用来确认 (11,3)/(12,3) 两格是否已绘制。

用法: python _chk/verify_remains_sprites.py
行列均 1 基（与 ItemSpriteSheet.xy 一致），尺寸为 assignItemRect 的 (w,h)。
"""
import sys, zlib, struct, os

PNG = "core/src/main/assets/sprites/items.png"

# (常量名, 列, 行, w, h) —— 与 ItemSpriteSheet 的 assignItemRect 保持一致
RECTS = [
    ("BROKEN_TERMINAL",   9, 3, 14, 14),
    ("BROKEN_BONE_BLADE", 10, 3, 13,  9),
    ("BROKEN_EYE",        11, 3, 12, 14),
    ("LEDGER_PAGE",       12, 3, 11, 13),
]


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
    assert bitd == 8 and interlace == 0, (bitd, interlace)
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


def pixel(buf, W, nch, x, y):
    o = (y * W + x) * nch
    if nch == 4:
        return buf[o], buf[o + 1], buf[o + 2], buf[o + 3]
    if nch == 3:
        return buf[o], buf[o + 1], buf[o + 2], 255
    return buf[o], buf[o], buf[o], buf[o]


def main():
    buf, W, H, nch = load_png(PNG)
    print("items.png = %dx%d, channels=%d" % (W, H, nch))
    ok = True
    for name, col, row, w, h in RECTS:
        x0, y0 = (col - 1) * 16, (row - 1) * 16
        # 该格允许的最大绘制区域 = 16x16，但如果 w/h > 16 就溢出到邻格
        assert w <= 16 and h <= 16, (name, w, h)
        opaque = 0
        rows_txt = []
        for y in range(y0, y0 + 16):
            line = ""
            for x in range(x0, x0 + 16):
                r, g, b, a = pixel(buf, W, nch, x, y)
                inrect = (x - x0) < w and (y - y0) < h
                if a > 0:
                    if inrect:
                        opaque += 1
                    line += "#" if inrect else "!"   # ! = 溢出到声明矩形之外
                else:
                    line += "." if inrect else ","
            rows_txt.append(line)
        flag = "OK " if opaque > 0 else "EMPTY"
        if opaque == 0:
            ok = False
        print("\n[%s] %s(%d,%d) %dx%d 不透明像素=%d" % (name, flag, col, row, w, h, opaque))
        for i, l in enumerate(rows_txt):
            if i < h:
                print("   " + l)
    print("\n" + ("ALL PASS" if ok else "FAIL: 存在空贴图矩形"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
