# -*- coding: utf-8 -*-
"""核验「高草」到底由哪些帧的哪些行像素组成（供「新增一种草皮」指南做绘制依据）。
用法: python _chk/grass_frame_audit.py
依赖: 同目录 tilemap_sim.Sheet（纯 Python 解 PNG，支持调色板 PLTE/tRNS）
"""
import sys, os
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
from tilemap_sim import Sheet, xy

ROOT = os.path.join(HERE, '..', 'core', 'src', 'main', 'assets', 'environment')

def rows_of(frame):
    """返回每行的不透明像素数（alpha>0 即算，因为草叶是半透明边缘）。"""
    out = []
    for row in frame:
        out.append(sum(1 for p in row if p[3] > 0))
    return out

def sig(frame, y):
    return tuple(frame[y])

def report(title, sheet, idx):
    f = sheet.frame(idx)
    r = rows_of(f)
    nonempty = [y for y, n in enumerate(r) if n]
    lo = min(nonempty) if nonempty else None
    hi = max(nonempty) if nonempty else None
    total = sum(r)
    print('%-46s 帧%3d  xy(%2d,%2d)  有效行 %s..%s  不透明像素 %4d' % (
        title, idx, idx % 16 + 1, idx // 16 + 1,
        lo if lo is not None else '-', hi if hi is not None else '-', total))
    print('      逐行: ' + ' '.join('%d' % n for n in r))
    return f

def main():
    tiles = Sheet(os.path.join(ROOT, 'tiles_sewers.png'))
    feats = Sheet(os.path.join(ROOT, 'terrain_features.png'))

    print('=' * 118)
    print('A. tiles_sewers.png 里与「高草」相关的帧（DungeonTileSheet 常量）')
    print('=' * 118)
    fr = {}
    fr['RAISED_HIGH_GRASS 122']       = report('RAISED_HIGH_GRASS（草皮，角色之下）', tiles, 122)
    fr['RAISED_HIGH_GRASS_ALT 125']   = report('RAISED_HIGH_GRASS_ALT', tiles, 125)
    fr['HIGH_GRASS_OVERHANG 234']     = report('HIGH_GRASS_OVERHANG（画在上一格）', tiles, 234)
    fr['HIGH_GRASS_OVERHANG_ALT 237'] = report('HIGH_GRASS_OVERHANG_ALT', tiles, 237)
    fr['HIGH_GRASS_UNDERHANG 250']    = report('HIGH_GRASS_UNDERHANG（画在本格）', tiles, 250)
    fr['HIGH_GRASS_UNDERHANG_ALT 253']= report('HIGH_GRASS_UNDERHANG_ALT', tiles, 253)

    print()
    print('=' * 118)
    print('B. terrain_features.png 里 stage=0 的高草帧（TerrainFeaturesTilemap: 9 + 16*stage）')
    print('=' * 118)
    fe = {}
    fe['features 9']  = report('terrain_features 高草 主帧', feats, 9)
    fe['features 10'] = report('terrain_features 高草 ALT', feats, 10)

    print()
    print('=' * 118)
    print('C. 关键判定：OVERHANG vs UNDERHANG 是不是「同一丛草切开上下两半」？')
    print('=' * 118)
    ov = fr['HIGH_GRASS_OVERHANG 234']
    un = fr['HIGH_GRASS_UNDERHANG 250']
    for y in range(16):
        a = sum(1 for p in ov[y] if p[3] > 0)
        b = sum(1 for p in un[y] if p[3] > 0)
        same = '同一批像素' if sig(ov, y) == sig(un, y) else ('不同' if (a or b) else '')
        print('  row%2d  OVERHANG 不透明=%2d   UNDERHANG 不透明=%2d   %s' % (y, a, b, same))

    print()
    print('=' * 118)
    print('D. 关键判定：tileset 的 UNDERHANG 与 terrain_features 的高草帧是否重复绘制？')
    print('=' * 118)
    f9 = fe['features 9']
    for y in range(16):
        a = sum(1 for p in un[y] if p[3] > 0)
        b = sum(1 for p in f9[y] if p[3] > 0)
        same = '完全相同' if sig(un, y) == sig(f9, y) else '不同'
        print('  row%2d  UNDERHANG(tileset) 不透明=%2d   features 不透明=%2d   %s' % (y, a, b, same))

    print()
    print('=' * 118)
    print('E. 合并后的草叶带纵向覆盖范围（以草格顶边 world y=行*16 为 0）')
    print('=' * 118)
    print('  OVERHANG 画在「上一格」：帧内 row  → 世界 y = (行-1)*16 + row')
    print('  UNDERHANG 画在「本格」：帧内 row  → 世界 y =  行  *16 + row')
    for name, f, base in (('OVERHANG(画在上一格)', ov, -16), ('UNDERHANG(画本格)', un, 0)):
        ys = [y for y in range(16) if any(p[3] > 0 for p in f[y])]
        if ys:
            print('  %-22s 帧内 row %2d..%2d  →  相对草地格顶边 %+3d..%+3d  ⇒ 总高 %d px'
                  % (name, min(ys), max(ys), base + min(ys), base + max(ys), max(ys) - min(ys) + 1))

if __name__ == '__main__':
    main()
