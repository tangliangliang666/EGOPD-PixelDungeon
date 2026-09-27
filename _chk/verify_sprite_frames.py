# 纯 Python（无 PIL）解码 PNG 核验精灵图帧布局：尺寸须被帧宽/帧高整除、每帧须有非透明像素、并统计半透明像素与重复帧。
# 支持 colorType 0/2/3/4/6 与 tRNS（索引色透明）。用法：python _chk/verify_sprite_frames.py <png> [帧宽] [帧高]
import struct
import sys
import zlib


def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    if pb <= pc:
        return b
    return c


def decode_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', '不是 PNG：' + path
    pos, idat, plte, trns, ihdr = 8, b'', None, None, None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            ihdr = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'PLTE':
            plte = data
        elif typ == b'tRNS':
            trns = data
        elif typ == b'IEND':
            break
        pos += 12 + ln
    w, h, bd, ct, comp, filt, inter = ihdr
    assert bd == 8, '仅支持 8 位深（实得 %d）' % bd
    assert inter == 0, '不支持隔行扫描'
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    raw = zlib.decompress(idat)
    stride = w * nch
    out = bytearray()
    prev = bytearray(stride)
    p = 0
    for _ in range(h):
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
                c = prev[i - nch] if i >= nch else 0
                line[i] = (line[i] + paeth(a, prev[i], c)) & 255
        out += line
        prev = line
    return w, h, ct, nch, plte, trns, bytes(out)


def pixel(px, ct, nch, plte, trns, x, y, w):
    i = (y * w + x) * nch
    if ct == 6:
        return tuple(px[i:i + 4])
    if ct == 2:
        r, g, b = px[i:i + 3]
        return (r, g, b, 255)
    if ct == 4:
        v, a = px[i:i + 2]
        return (v, v, v, a)
    if ct == 0:
        v = px[i]
        return (v, v, v, 255)
    if ct == 3:
        idx = px[i]
        r, g, b = plte[idx * 3:idx * 3 + 3]
        a = 255
        if trns is not None and idx < len(trns):
            a = trns[idx]
        return (r, g, b, a)
    raise AssertionError('未知 colorType %d' % ct)


def main():
    path = sys.argv[1]
    fw = int(sys.argv[2]) if len(sys.argv) > 2 else 16
    fh = int(sys.argv[3]) if len(sys.argv) > 3 else 16

    w, h, ct, nch, plte, trns, px = decode_png(path)
    print('图：%s' % path)
    print('尺寸：%d×%d  colorType=%d  通道=%d  透明方式=%s'
          % (w, h, ct, nch, 'tRNS（索引色透明）' if trns is not None else ('alpha 通道' if ct in (4, 6) else '无（全不透明）')))

    ok = True

    def chk(cond, msg):
        nonlocal ok
        print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
        if not cond:
            ok = False

    chk(w % fw == 0, '宽 %d 能被帧宽 %d 整除' % (w, fw))
    chk(h % fh == 0, '高 %d 能被帧高 %d 整除' % (h, fh))
    cols, rows = w // fw, h // fh
    print('帧网格：%d 列 × %d 行 = %d 帧（%d×%d）' % (cols, rows, cols * rows, fw, fh))

    frames = []
    semi_total = 0
    for r in range(rows):
        for c in range(cols):
            opaque = 0
            semi = 0
            sig = []
            for y in range(r * fh, (r + 1) * fh):
                for x in range(c * fw, (c + 1) * fw):
                    pr, pg, pb, pa = pixel(px, ct, nch, plte, trns, x, y, w)
                    if pa > 0:
                        opaque += 1
                        if pa < 255:
                            semi += 1
                    sig.append((pr, pg, pb, pa))
            frames.append((r * cols + c, opaque, semi, tuple(sig)))
            semi_total += semi

    for idx, opaque, semi, _ in frames:
        flag = '[OK]  ' if opaque > 0 else '[FAIL]'
        print('  %s 帧 %-2d  非透明像素 %-4d  半透明 %d' % (flag, idx, opaque, semi))
        if opaque == 0:
            ok = False

    # 帧内容重复检测（仅提示，不算错）
    dup = []
    for i in range(len(frames)):
        for j in range(i + 1, len(frames)):
            if frames[i][3] == frames[j][3]:
                dup.append((i, j))
    print('  相同帧对：%s' % (dup if dup else '无'))
    print('  半透明像素合计：%d（%s）' % (semi_total, '建议改 0/255 二值' if semi_total else '全为二值，绘制干净'))

    print()
    print('===== 帧布局核验：' + ('ALL PASS' if ok else 'FAIL') + ' =====')
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
