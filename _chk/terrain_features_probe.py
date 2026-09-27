# -*- coding: utf-8 -*-
"""
terrain_features.png 帧槽探针（2026-09-19）

背景：27 层（LobTestLevel）草地显示成"五区蘑菇形草地"。
根因嫌疑：TerrainFeaturesTilemap.getTileVisual() 里草叶细节的 stage 由
    int stage = (Dungeon.depth-1)/5;
直接取真实深度计算——27 层落到 stage 5（被 Math.min 夹成 4 = 恶魔大厅），
而不是随 tiles_lob 贴图应有的区段。

本脚本：把 terrain_features.png 的每个帧槽 dump 出来，统计不透明像素与主色，
用于确认「每 16 个像素行 = 一个区域的草叶细节风格」这一布局，并核对目标区段。

用法：
    python _chk/terrain_features_probe.py
    python _chk/terrain_features_probe.py --dump 0..3   # 导出指定帧槽为 PNG 便于目视
"""
import os
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
FEATURES = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'environment', 'terrain_features.png')

FRAME = 16


# ---------------------------------------------------------------- PNG decode
def read_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not a png'
    pos = 8
    idat = b''
    w = h = bd = ct = None
    pal = None
    trns = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bd, ct = struct.unpack('>IIBB', data[:10])
        elif typ == b'IDAT':
            idat += data
        elif typ == b'PLTE':
            pal = data
        elif typ == b'tRNS':
            trns = data
        pos += 12 + ln

    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    if bd == 8:
        stride = w * channels
        out = []
        prev = bytearray(stride)
        i = 0
        for _ in range(h):
            ft = raw[i]
            i += 1
            line = bytearray(raw[i:i + stride])
            i += stride
            if ft == 1:
                for x in range(channels, stride):
                    line[x] = (line[x] + line[x - channels]) & 0xFF
            elif ft == 2:
                for x in range(stride):
                    line[x] = (line[x] + prev[x]) & 0xFF
            elif ft == 3:
                for x in range(stride):
                    a = line[x - channels] if x >= channels else 0
                    line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
            elif ft == 4:
                for x in range(stride):
                    a = line[x - channels] if x >= channels else 0
                    b = prev[x]
                    c = prev[x - channels] if x >= channels else 0
                    p = a + b - c
                    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                    pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                    line[x] = (line[x] + pr) & 0xFF
            out.append(bytes(line))
            prev = line
        pixels = out
    else:
        raise NotImplementedError('bit depth %s not handled' % bd)

    def px(x, y):
        s = pixels[y]
        if ct == 6:
            o = x * 4
            return s[o], s[o + 1], s[o + 2], s[o + 3]
        if ct == 2:
            o = x * 3
            return s[o], s[o + 1], s[o + 2], 255
        if ct == 0:
            v = s[x]
            return v, v, v, 255
        if ct == 4:
            o = x * 2
            v = s[o]
            return v, v, v, s[o + 1]
        if ct == 3:
            idx = s[x]
            r, g, b = pal[idx * 3:idx * 3 + 3]
            a = trns[idx] if trns and idx < len(trns) else 255
            return r, g, b, a
        raise NotImplementedError

    return w, h, px


def main():
    w, h, px = read_png(FEATURES)
    print('terrain_features.png: %dx%d  (frames %dx%d)' % (w, h, w // FRAME, h // FRAME))
    cols, rows = w // FRAME, h // FRAME
    print()
    print('frame(col,row)  slot    opaque  colors(top3)')
    for row in range(rows):
        for col in range(cols):
            slot = col + cols * row
            opaque = 0
            hist = {}
            for y in range(row * FRAME, row * FRAME + FRAME):
                for x in range(col * FRAME, col * FRAME + FRAME):
                    r, g, b, a = px(x, y)
                    if a > 0:
                        opaque += 1
                        hist[(r, g, b)] = hist.get((r, g, b), 0) + 1
            top = sorted(hist.items(), key=lambda kv: -kv[1])[:3]
            tops = ' '.join('#%02X%02X%02X x%d' % (c[0], c[1], c[2], n) for c, n in top)
            print('%-14s  %-5d  %6d  %s' % ('(%2d,%2d)' % (col + 1, row + 1), slot, opaque, tops))

    print()
    print('--- 草叶细节槽位对照（TerrainFeaturesTilemap.getTileVisual）---')
    print('  HIGH_GRASS   → 9  + 16*stage [+1 alt]')
    print('  FURROWED     → 11 + 16*stage [+1 alt]')
    print('  GRASS        → 13 + 16*stage [+1 alt]')
    print('  EMBERS       → 9  + 80      [+1 alt]')
    for stage in range(5):
        region = ['下水道', '监狱', '矿洞', '都市', '恶魔大厅'][stage]
        print('  stage %d (%s): HIGH_GRASS 主槽 %d / ALT %d' % (stage, region, 9 + 16 * stage, 10 + 16 * stage))
    print()
    print('  ⚠ 27 层真实 depth=27 → (27-1)/5 = 5 → Math.min(5,4) = 4 ⇒ 取恶魔大厅（蘑菇形）')


if __name__ == '__main__':
    main()
