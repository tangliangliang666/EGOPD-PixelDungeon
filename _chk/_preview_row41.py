# -*- coding: utf-8 -*-
"""临时工具：把 items.png 的某一行渲染成放大预览 PNG（纯 Python，无 PIL）。

用途：确认「第 41 行第 6 列的孤立图案」到底是不是新武器图标。
只读 items.png，输出到 _chk/_out/。用法: python _chk/_preview_row41.py [行号]
"""
import os
import struct
import sys
import zlib

PNG = "core/src/main/assets/sprites/items.png"
OUTDIR = "_chk/_out"


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


def write_png(path, w, h, rgba):
    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)
    raw = b''.join(b'\x00' + bytes(rgba[y * w * 4:(y + 1) * w * 4]) for y in range(h))
    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    open(path, 'wb').write(png)


def get_px(buf, W, nch, x, y):
    o = (y * W + x) * nch
    if nch == 4:
        return buf[o], buf[o + 1], buf[o + 2], buf[o + 3]
    return buf[o], buf[o], buf[o], 255


def blit_checker(rgba, w, h, scale, ox, oy, checker=8):
    """给放大后的区域铺棋盘底，让透明区域可见。"""
    for y in range(oy, oy + h * scale):
        for x in range(ox, ox + w * scale):
            c = 70 if ((x // checker) + (y // checker)) % 2 == 0 else 100
            o = (y * (w * scale + 2) + x) * 4 if False else None
            _ = o
            yield x, y, c


def main():
    row = int(sys.argv[1]) if len(sys.argv) > 1 else 41
    buf, W, H, nch = load_png(PNG)
    cols = W // 16
    y0 = (row - 1) * 16
    scale = 6
    gap = 2                      # 格间竖线宽
    cw = 16 * scale
    outw = cols * cw + (cols + 1) * gap
    outh = 16 * scale + 2 * gap
    rgba = bytearray(outw * outh * 4)

    # 棋盘底 + 竖线
    for y in range(outh):
        for x in range(outw):
            in_col = (x - gap) % (cw + gap)
            on_line = x < gap or in_col >= cw
            if on_line and not (y < gap or y >= outh - gap):
                c = (255, 0, 255)          # 品红竖线 = 格边界
            else:
                c = (70, 70, 70) if ((x // 8) + (y // 8)) % 2 == 0 else (95, 95, 95)
            o = (y * outw + x) * 4
            rgba[o:o + 4] = bytes((c[0], c[1], c[2], 255))

    # 贴像素
    for col in range(1, cols + 1):
        x0 = (col - 1) * 16
        ox = gap + (col - 1) * (cw + gap)
        oy = gap
        for yy in range(16):
            for xx in range(16):
                r, g, b, a = get_px(buf, W, nch, x0 + xx, y0 + yy)
                if a == 0:
                    continue
                for sy in range(scale):
                    for sx in range(scale):
                        px, py = ox + xx * scale + sx, oy + yy * scale + sy
                        o = (py * outw + px) * 4
                        rgba[o:o + 4] = bytes((r, g, b, 255))
    p1 = OUTDIR + "/row%d_full.png" % row
    write_png(p1, outw, outh, rgba)
    print("已写出 %s  (%dx%d, 每格 %dpx 宽, 共 %d 格)" % (p1, outw, outh, cw, cols))

    # 单格放大（第 6 格 + 参照的第 3 格）
    for col in (3, 6):
        zscale = 20
        zw = zh = 16 * zscale
        z = bytearray(zw * zh * 4)
        for y in range(zh):
            for x in range(zw):
                c = (70, 70, 70) if ((x // 10) + (y // 10)) % 2 == 0 else (95, 95, 95)
                o = (y * zw + x) * 4
                z[o:o + 4] = bytes((c[0], c[1], c[2], 255))
        x0 = (col - 1) * 16
        for yy in range(16):
            for xx in range(16):
                r, g, b, a = get_px(buf, W, nch, x0 + xx, y0 + yy)
                if a == 0:
                    continue
                for sy in range(zscale):
                    for sx in range(zscale):
                        px, py = xx * zscale + sx, yy * zscale + sy
                        o = (py * zw + px) * 4
                        z[o:o + 4] = bytes((r, g, b, 255))
        pz = OUTDIR + "/row%d_col%02d_zoom.png" % (row, col)
        write_png(pz, zw, zh, z)
        print("已写出 %s" % pz)

    # 第 6 格的 ASCII 轮廓（便于文字确认）
    x0 = 5 * 16
    print("\n第 41 行第 6 格（x=80..95）ASCII 轮廓（'#'=不透明, '.'=透明）:")
    for yy in range(16):
        line = ""
        for xx in range(16):
            _, _, _, a = get_px(buf, W, nch, x0 + xx, y0 + yy)
            line += "#" if a > 0 else "."
        print("   " + line)
    return 0


if __name__ == "__main__":
    sys.exit(main())
