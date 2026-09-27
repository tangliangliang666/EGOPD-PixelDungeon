#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
4 位/2 位/1 位调色板 PNG 解码 + 主色统计（SPD 的 launcher 图标 foreground/background 就是 4-bit 索引 PNG）。

用法：
  python _chk/png_palette_dump.py <png> [png ...]

输出：尺寸、位深、颜色类型、调色板条目、不透明像素占主色 top-N（十六进制）。
比 tilemap_sim.Sheet 多支持 bit depth 1/2/4 的索引格式。
"""
import sys, os, zlib, struct
from collections import Counter


def load_png_any(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not a png'
    pos = 8
    idat = b''
    plte = None
    trns = None
    w = h = bitd = ctype = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bitd, ctype = struct.unpack('>IIBB', data[:10])
        elif typ == b'PLTE':
            plte = [(data[i], data[i + 1], data[i + 2]) for i in range(0, len(data), 3)]
        elif typ == b'tRNS':
            trns = list(data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
        pos += 12 + ln

    # tRNS 给调色板附 alpha
    alpha = [255] * (len(plte) if plte else 0)
    if trns:
        for i, a in enumerate(trns):
            if i < len(alpha):
                alpha[i] = a

    raw = zlib.decompress(idat)

    # 每像素通道数 & 每行字节数
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    if ctype == 3:
        rowbytes = (w * bitd + 7) // 8
    else:
        rowbytes = w * nch * bitd // 8
    bpp = max(1, (nch * bitd) // 8)  # filter 用的「前方像素字节数」

    out = bytearray()
    prev = bytearray(rowbytes)
    p = 0
    for y in range(h):
        ft = raw[p]
        p += 1
        line = bytearray(raw[p:p + rowbytes])
        p += rowbytes
        if ft == 1:
            for i in range(bpp, rowbytes):
                line[i] = (line[i] + line[i - bpp]) & 0xFF
        elif ft == 2:
            for i in range(rowbytes):
                line[i] = (line[i] + prev[i]) & 0xFF
        elif ft == 3:
            for i in range(rowbytes):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xFF
        elif ft == 4:
            for i in range(rowbytes):
                a = line[i - bpp] if i >= bpp else 0
                b = prev[i]
                c = prev[i - bpp] if i >= bpp else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xFF
        out += line
        prev = line

    return dict(w=w, h=h, bitd=bitd, ctype=ctype, nch=nch, plte=plte, alpha=alpha, buf=bytes(out))


def pixels(img):
    """产出 (r,g,b,a) 生成器（涵盖索引与真彩）。"""
    w, h, bitd, ctype, nch, plte, alpha, buf = (img['w'], img['h'], img['bitd'], img['ctype'],
                                                img['nch'], img['plte'], img['alpha'], img['buf'])
    if ctype == 3:
        rowbytes = (w * bitd + 7) // 8
        mask = (1 << bitd) - 1
        for y in range(h):
            base = y * rowbytes
            for x in range(w):
                if bitd == 8:
                    idx = buf[base + x]
                else:
                    bitpos = x * bitd
                    byte = buf[base + (bitpos >> 3)]
                    shift = 8 - bitd - (bitpos & 7)
                    idx = (byte >> shift) & mask
                if plte and idx < len(plte):
                    r, g, b = plte[idx]
                    a = alpha[idx] if idx < len(alpha) else 255
                else:
                    r = g = b = 0
                    a = 0
                yield r, g, b, a
    elif ctype == 6:
        for i in range(0, len(buf), 4):
            yield buf[i], buf[i + 1], buf[i + 2], buf[i + 3]
    elif ctype == 2:
        for i in range(0, len(buf), 3):
            yield buf[i], buf[i + 1], buf[i + 2], 255
    elif ctype == 0:
        for i in range(len(buf)):
            v = buf[i]
            yield v, v, v, 255
    else:
        for i in range(0, len(buf), 2):
            v = buf[i]
            yield v, v, v, buf[i + 1]


def report(path):
    try:
        img = load_png_any(path)
    except Exception as e:
        print('%-52s 读取失败: %s' % (os.path.basename(path), e))
        return
    cnt = Counter()
    opaque = 0
    for r, g, b, a in pixels(img):
        if a > 200:
            cnt[(r, g, b)] += 1
            opaque += 1
    print('%-52s %4dx%-4d bit=%d ctype=%d 不透明=%6d (%.0f%%)' % (
        os.path.basename(path), img['w'], img['h'], img['bitd'], img['ctype'],
        opaque, 100.0 * opaque / (img['w'] * img['h'])))
    top = ', '.join('#%02X%02X%02X x%-6d' % (r, g, b, k) for (r, g, b), k in cnt.most_common(6))
    print('     主色: %s' % top)
    if img['ctype'] == 3 and img['plte']:
        print('     调色板 %d 色: %s' % (len(img['plte']),
              ' '.join('#%02X%02X%02X' % c for c in img['plte'][:16]) + (' ...' if len(img['plte']) > 16 else '')))


if __name__ == '__main__':
    args = sys.argv[1:]
    if not args:
        print(__doc__)
        sys.exit(0)
    for a in args:
        report(a)
