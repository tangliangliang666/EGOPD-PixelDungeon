# -*- coding: utf-8 -*-
"""纯 Python 读 PNG(无 PIL) 并把 spd 图集的指定 16x16 帧打成 ASCII，用于核验贴图分层结构。
用法: python tileframe_dump.py <png> <col,row> [<col,row> ...]   行列均 1 基(与 DungeonTileSheet.xy 一致)
"""
import sys, zlib, struct

def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not png'
    pos, idat, w, h, bitd, ctype, interlace = 8, b'', 0, 0, 8, 6, 0
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos+4])[0]
        typ = d[pos+4:pos+8]
        data = d[pos+8:pos+8+ln]
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
        line = bytearray(raw[p:p+stride]); p += stride
        if f == 1:
            for i in range(nch, stride): line[i] = (line[i] + line[i-nch]) & 255
        elif f == 2:
            for i in range(stride): line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i-nch] if i >= nch else 0
                b = prev[i]; c = prev[i-nch] if i >= nch else 0
                pp = a + b - c
                pa, pb, pc = abs(pp-a), abs(pp-b), abs(pp-c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y*stride:(y+1)*stride] = line
        prev = line
    return w, h, nch, out

def px(w, nch, buf, x, y):
    o = (y*w + x)*nch
    if nch == 4:
        r, g, b, a = buf[o], buf[o+1], buf[o+2], buf[o+3]
    elif nch == 3:
        r, g, b = buf[o], buf[o+1], buf[o+2]; a = 255
    else:
        r = g = b = buf[o]; a = 255
    return r, g, b, a

CH = ' .:-=+*#%@'
def dump(path, frames):
    w, h, nch, buf = load_png(path)
    print('== %s  %dx%d  ch=%d  (%d cols x %d rows of 16px)' % (path, w, h, nch, w//16, h//16))
    for (cx, cy) in frames:
        ox, oy = (cx-1)*16, (cy-1)*16
        print('-- frame(c=%d,r=%d) index=%d  px(%d..%d, %d..%d)' % (cx, cy, (cx-1)+16*(cy-1), ox, ox+15, oy, oy+15))
        rowlbl = []
        for j in range(16):
            row = ''
            for i in range(16):
                r, g, b, a = px(w, nch, buf, ox+i, oy+j)
                if a < 32:
                    row += ' '
                else:
                    lum = (r*299 + g*587 + b*114)//1000
                    idx = min(9, max(1, (255-lum)*10//256))
                    ch = CH[idx]
                    if a < 200: ch = ch.lower() if ch.isalpha() else '.'
                    if a < 200 and ch == '.': ch = ','
                    row += ch
            rowlbl.append('%2d|%s|' % (j, row))
        print('\n'.join(rowlbl))
        print('    ' + ''.join(str(i % 10) for i in range(16)))

if __name__ == '__main__':
    png = sys.argv[1]
    fr = []
    for a in sys.argv[2:]:
        c, r = a.split(',')
        fr.append((int(c), int(r)))
    dump(png, fr)
