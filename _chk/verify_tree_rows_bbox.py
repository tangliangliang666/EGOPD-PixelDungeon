# -*- coding: utf-8 -*-
"""三行图标（tree.png 160x48）的包围盒是否逐格完全一致？

为什么要查：WndTrials 现在只用**一套** ICON_W/ICON_H 去裁图。
若三行的 alpha 包围盒不同，那么「同一套 W/H 裁三行」就会让某些行、某些图标裁歪
（右侧多带/少带像素 ⇒ 居中后整体偏移）。

口径：
  halo  = alpha > 0    —— PIL getbbox() 与 Trials.ICON_W/H 目前用的就是这个
  solid = alpha >= 128 —— 真正看得见的实体核心
"""
import os
from PIL import Image

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
ROWS = ['row0 彩色(已开启)', 'row1 浅灰(锁定)', 'row2 深灰(未开启)']
NAMES = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
         'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']


def bb(cell, thr):
    px = list(cell.get_flattened_data())
    w = cell.width
    pts = [(i % w, i // w) for i, p in enumerate(px) if p[3] > thr]
    if not pts:
        return None
    xs = [q[0] for q in pts]
    ys = [q[1] for q in pts]
    return min(xs), min(ys), max(xs), max(ys)


def fmt(b):
    return '--' if b is None else '%dx%d @%d,%d' % (b[2] - b[0] + 1, b[3] - b[1] + 1, b[0], b[1])


def main():
    im = Image.open(SPRITE).convert('RGBA')
    print('tree.png = %dx%d  (10 列 x %d 行, 每格 16x16)\n'
          % (im.width, im.height, im.height // 16))
    diff_halo = diff_solid = 0
    for i, nm in enumerate(NAMES):
        halo = []
        solid = []
        for r in range(3):
            c = im.crop((i * 16, r * 16, i * 16 + 16, r * 16 + 16))
            halo.append(bb(c, 0))
            solid.append(bb(c, 127))
        same_h = len(set(halo)) == 1
        same_s = len(set(solid)) == 1
        if not same_h:
            diff_halo += 1
        if not same_s:
            diff_solid += 1
        print('%-10s halo  %-14s %-14s %-14s %s' % (
            nm, fmt(halo[0]), fmt(halo[1]), fmt(halo[2]),
            'ok 三行一致' if same_h else '<<< 三行不一致'))
        if not same_s:
            print('%-10s solid %-14s %-14s %-14s     (实体盒有差异)' % (
                '', fmt(solid[0]), fmt(solid[1]), fmt(solid[2])))
    print()
    print('halo 三行不一致的图标数 = %d / 10' % diff_halo)
    print('solid 三行不一致的图标数 = %d / 10' % diff_solid)
    print()
    print('结论：若 halo 全一致 ⇒ 一套 ICON_W/H 裁三行是安全的；')
    print('      若有差异 ⇒ 必须(a)把三行补齐成同一包围盒，或(b)ICON_W/H 按行给两套。')


if __name__ == '__main__':
    main()
