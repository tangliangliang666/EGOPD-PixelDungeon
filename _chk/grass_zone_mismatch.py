# -*- coding: utf-8 -*-
"""
草叶细节「区域错配」核验（2026-09-19）

问题：27 层（LobTestLevel，tiles_lob.png）草地显示出五区（恶魔大厅）的蘑菇形草叶细节，
而 tiles_lob 自己的草帧明明是监狱区（第二区）风格。

机制：草叶细节（长草尖、草簇阴影）来自**全局唯一**的 terrain_features.png，
      它按 Dungeon.depth 自动选区段：
          int stage = (Dungeon.depth-1)/5;   // 1-5→0 下水道 … 21-25→4 恶魔大厅
          stage = Math.min(stage, 4);
      27 层真实 depth=27 ⇒ (27-1)/5 = 5 ⇒ 夹成 4 ⇒ 永远取恶魔大厅那套。
      ——这是本次"蘑菇形草地"的直接根因。

本脚本做两件事：
  1) 把 terrain_features.png 各 stage 的草叶细节主色打出来（确认该区段确实是恶魔大厅/蘑菇色）；
  2) 与 tiles_lob.png 的对应帧主色并排，指出「楼层贴图 = 监狱区」而「细节 = 恶魔大厅区」的错配。

用法：
    python _chk/grass_zone_mismatch.py
"""
import os
import struct
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ENV = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'environment')

FRAME = 16
# 草叶细节槽位（TerrainFeaturesTilemap.getTileVisual）
FEATURE_ROW = 1          # 帧槽 16..31 是"标准地形细节"那一行
FEATURE_NAME = {9: 'HIGH_GRASS', 11: 'FURROWED', 13: 'GRASS'}


# ---------------------------------------------------------------- PNG decode
def read_png(path):
    d = open(path, 'rb').read()
    pos = 8
    idat = b''
    pal = trns = None
    w = h = bd = ct = None
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
    assert bd == 8, 'bit depth %s unsupported' % bd
    stride = w * channels
    lines = []
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
        lines.append(bytes(line))
        prev = line

    def px(x, y):
        s = lines[y]
        if ct == 6:
            o = x * 4
            return s[o], s[o + 1], s[o + 2], s[o + 3]
        if ct == 2:
            o = x * 3
            return s[o], s[o + 1], s[o + 2], 255
        if ct == 0:
            v = s[x]
            return v, v, v, 255
        if ct == 3:
            idx = s[x]
            r, g, b = pal[idx * 3:idx * 3 + 3]
            a = trns[idx] if trns and idx < len(trns) else 255
            return r, g, b, a
        raise NotImplementedError
    return w, h, px


def frame_top_colors(px, slot, width=16, topn=4):
    """按「帧槽号」取帧（槽号 = (slot % 16) 列 + 1, slot // 16 行 + 1）"""
    col = slot % width
    row = slot // width
    hist = {}
    opaque = 0
    for y in range(row * FRAME, row * FRAME + FRAME):
        for x in range(col * FRAME, col * FRAME + FRAME):
            r, g, b, a = px(x, y)
            if a > 0:
                opaque += 1
                hist[(r, g, b)] = hist.get((r, g, b), 0) + 1
    top = sorted(hist.items(), key=lambda kv: -kv[1])[:topn]
    return opaque, top


def main():
    fw, fh, fpx = read_png(os.path.join(ENV, 'terrain_features.png'))
    lw, lh, lpx = read_png(os.path.join(ENV, 'tiles_lob.png'))
    print('terrain_features.png %dx%d   tiles_lob.png %dx%d' % (fw, fh, lw, lh))
    print()

    regions = ['下水道(1区)', '监狱(2区)', '矿洞(3区)', '都市(4区)', '恶魔大厅(5区)']
    print('=== terrain_features.png 各 stage 的 HIGH_GRASS 草叶细节 ===')
    for stage in range(5):
        slot = 9 + 16 * stage
        opaque, top = frame_top_colors(fpx, slot)
        s = ' '.join('#%02X%02X%02X x%d' % (c[0], c[1], c[2], n) for c, n in top)
        print('  stage %d %-12s 槽 %-3d 不透明 %3dpx  %s' % (stage, regions[stage], slot, opaque, s))

    print()
    print('=== tiles_lob.png 的草帧（本层的真实材质）===')
    # 取样：GRASS 平铺帧（GROUND+2 = 2）、RAISED_HIGH_GRASS（122）、HIGH_GRASS_UNDERHANG（250）
    for slot, label in [(2, 'GRASS 平铺'), (122, 'RAISED_HIGH_GRASS'), (234, 'HIGH_GRASS_OVERHANG'), (250, 'HIGH_GRASS_UNDERHANG')]:
        opaque, top = frame_top_colors(lpx, slot)
        s = ' '.join('#%02X%02X%02X x%d' % (c[0], c[1], c[2], n) for c, n in top)
        print('  %-22s 槽 %-3d 不透明 %3dpx  %s' % (label, slot, opaque, s))

    print()
    print('=== 对照 tiles_prison.png（第二区，用户说 tiles_lob 草地类似它）===')
    ppath = os.path.join(ENV, 'tiles_prison.png')
    if os.path.exists(ppath):
        _, _, ppx = read_png(ppath)
        for slot, label in [(2, 'GRASS 平铺'), (122, 'RAISED_HIGH_GRASS'), (250, 'HIGH_GRASS_UNDERHANG')]:
            opaque, top = frame_top_colors(ppx, slot)
            s = ' '.join('#%02X%02X%02X x%d' % (c[0], c[1], c[2], n) for c, n in top)
            print('  %-22s 槽 %-3d 不透明 %3dpx  %s' % (label, slot, opaque, s))
    else:
        print('  (缺 tiles_prison.png)')

    print()
    print('=== 结论 ===')
    print('  27 层 tiles_lob 的草 = %s 风格' % '监狱区')
    print('  但草叶细节 stage = min((27-1)/5, 4) = 4 ⇒ 恶魔大厅（蘑菇形）')
    print('  ⇒ 必须让 LobTestLevel 覆写「草叶细节所用深度」，回到 GEN_DEPTH 对应的 stage。')


if __name__ == '__main__':
    main()
