# -*- coding: utf-8 -*-
"""水仙十字圣剑三形态贴图核验（2026-09-20）。

用纯 Python 解码 items.png（无 PIL），dump 三格矩形并报告不透明像素数，
同时按 (列,行) 全表扫描 ItemSpriteSheet.java 的 xy 撞格。

用法: python _chk/verify_narcissus_sprites.py

行列均 1 基（与 ItemSpriteSheet.xy 一致），尺寸为 assignItemRect 的 (w,h)。

为什么值得单独跑：用户说「已经画好了」时，实际落盘的可能仍是空图（历史上踩过），
而空图**不报任何错**——只在游戏里表现为一个看不见的武器。这个脚本一眼能看出来。
另外 14×14 的矩形落在 16×16 格里，要顺带确认没有像素溢出到邻格。
"""
import re
import struct
import sys
import zlib

PNG = "core/src/main/assets/sprites/items.png"
ITEMSPRITE = ("core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
              "/sprites/ItemSpriteSheet.java")

# (常量名, 列, 行, w, h) —— 与 ItemSpriteSheet 的 assignItemRect 保持一致
RECTS = [
    ("NARCISSUS_CROSS_SWORD", 3, 41, 14, 14),
    ("NARCISSUS_SWORD_MANG",  4, 41, 14, 14),
    ("NARCISSUS_SWORD_HUANG", 5, 41, 14, 14),
]

# 三形态应当**互不相同**的贴图：内容若完全一样，多半是复制时忘了改
DISTINCT = True


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


def rect_signature(buf, W, nch, col, row, w, h):
    """取该矩形的像素指纹，用于判断三张图是否真的各不相同。"""
    x0, y0 = (col - 1) * 16, (row - 1) * 16
    sig = []
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            sig.append(pixel(buf, W, nch, x, y))
    return tuple(sig)


def scan_xy_collisions():
    """全表扫描 ItemSpriteSheet 的 xy 常量，报告 (列,行) 撞格。"""
    try:
        src = open(ITEMSPRITE, encoding='utf-8').read()
    except OSError as e:
        return None, "读不到 ItemSpriteSheet.java: %s" % e

    # 常量声明： public static final int NAME = xy( 3, 41 );
    decl = {}
    for m in re.finditer(r"int\s+([A-Z][A-Z0-9_]*)\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)", src):
        name, c, r = m.group(1), int(m.group(2)), int(m.group(3))
        decl.setdefault((c, r), []).append(name)

    # assignItemRect： assignItemRect( NAME, 14, 14 );
    rects = {}
    for m in re.finditer(r"assignItemRect\(\s*([A-Z][A-Z0-9_]*)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", src):
        rects[m.group(1)] = (int(m.group(2)), int(m.group(3)))

    problems = []
    for name in ("NARCISSUS_CROSS_SWORD", "NARCISSUS_SWORD_MANG", "NARCISSUS_SWORD_HUANG"):
        if name not in rects:
            problems.append("缺少 assignItemRect(%s, ...)" % name)
    for (c, r), names in sorted(decl.items()):
        if len(names) > 1 and r >= 38:
            problems.append("xy 撞格 (%d,%d): %s" % (c, r, "/".join(names)))
    return (decl, rects), problems


def main():
    buf, W, H, nch = load_png(PNG)
    print("items.png = %dx%d, channels=%d" % (W, H, nch))
    ok = True

    outlines = {}
    for name, col, row, w, h in RECTS:
        x0, y0 = (col - 1) * 16, (row - 1) * 16
        assert w <= 16 and h <= 16, (name, w, h)
        opaque = 0
        overflow = 0
        rows_txt = []
        for y in range(y0, y0 + 16):
            line = ""
            for x in range(x0, x0 + 16):
                r, g, b, a = pixel(buf, W, nch, x, y)
                inrect = (x - x0) < w and (y - y0) < h
                if a > 0:
                    if inrect:
                        opaque += 1
                    else:
                        overflow += 1
                    line += "#" if inrect else "!"
                else:
                    line += "." if inrect else ","
            rows_txt.append(line)
        outlines[name] = rect_signature(buf, W, nch, col, row, w, h)
        flag = "OK   " if opaque > 0 else "EMPTY"
        if opaque == 0:
            ok = False
        if overflow:
            ok = False
        print("\n[%s] %s(%d,%d) %dx%d 不透明像素=%d 溢出=%d"
              % (name, flag, col, row, w, h, opaque, overflow))
        for i, l in enumerate(rows_txt):
            if i < h:
                print("   " + l)

    if DISTINCT:
        names = [r[0] for r in RECTS]
        for i in range(len(names)):
            for j in range(i + 1, len(names)):
                if outlines[names[i]] == outlines[names[j]]:
                    ok = False
                    print("FAIL  贴图内容完全相同：%s 与 %s（复制后忘了改？）"
                          % (names[i], names[j]))

    info, problems = scan_xy_collisions()
    print("\n=== ItemSpriteSheet 接线 ===")
    if info is None:
        ok = False
        print("FAIL  " + problems)
    else:
        decl, rects = info
        for name in ("NARCISSUS_CROSS_SWORD", "NARCISSUS_SWORD_MANG", "NARCISSUS_SWORD_HUANG"):
            hit = [k for k, v in decl.items() if name in v]
            print("   %-24s xy=%s  assignItemRect=%s"
                  % (name, hit[0] if hit else "缺失", rects.get(name, "缺失")))
        for p in problems:
            ok = False
            print("FAIL  " + p)

    print("\n" + ("ALL PASS" if ok else "FAIL"))
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
