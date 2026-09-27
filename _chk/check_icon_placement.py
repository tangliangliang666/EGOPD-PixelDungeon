# -*- coding: utf-8 -*-
"""量化：每个考验图标在 tree.png 的 16x16 格里到底放在哪，以及按不同口径居中的偏差。

口径说明（原图像素，x 向右、y 向下，格里索引 0..15）：
  frame  = alpha>0  的包围盒 —— 我裁剪 / 游戏 Trials.ICON_W,H 用的就是它
  solid  = alpha>=128 的包围盒 —— 真正看得见的实体核心
  mass   = alpha 加权重心 —— 视觉重心
偏差 bias = solid_center - frame_center，即「按 frame 居中后，实体核心偏到哪里」。
"""
import os
from PIL import Image

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
NAMES = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
         'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']


def boxes(cell):
    px = list(cell.get_flattened_data())
    w = cell.width
    halo = [(i % w, i // w) for i, p in enumerate(px) if p[3] > 0]
    solid = [(i % w, i // w) for i, p in enumerate(px) if p[3] >= 128]
    def bb(L):
        xs = [q[0] for q in L]
        ys = [q[1] for q in L]
        return min(xs), min(ys), max(xs), max(ys)
    tot = sx = sy = 0.0
    for i, p in enumerate(px):
        if p[3]:
            tot += p[3]
            sx += (i % w) * p[3]
            sy += (i // w) * p[3]
    return bb(halo), bb(solid), (sx / tot, sy / tot), len(halo) - len(solid)


def c(box):
    return ((box[0] + box[2]) / 2.0, (box[1] + box[3]) / 2.0)


def main():
    im = Image.open(SPRITE).convert('RGBA')
    print('%-10s %-11s %-11s %-9s %-13s %-13s %s' % (
        'point', 'frame(α>0)', 'solid(α≥128)', '弱像素', '实体相对框', '重心相对框', '判定'))
    for i, nm in enumerate(NAMES):
        cell = im.crop((i * 16, 0, i * 16 + 16, 16))
        hb, sb, mass, weak = boxes(cell)
        fc, sc = c(hb), c(sb)
        bs = (sc[0] - fc[0], sc[1] - fc[1])
        bm = (mass[0] - fc[0], mass[1] - fc[1])
        flag = 'ok' if abs(bs[0]) < 0.01 and abs(bs[1]) < 0.01 else '<<< 实体不居中'
        print('%-10s %-11s %-11s %-9d %-13s %-13s %s' % (
            nm, '%dx%d @%d,%d' % (hb[2] - hb[0] + 1, hb[3] - hb[1] + 1, hb[0], hb[1]),
            '%dx%d @%d,%d' % (sb[2] - sb[0] + 1, sb[3] - sb[1] + 1, sb[0], sb[1]),
            weak, '%+.2f,%+.2f' % bs, '%+.2f,%+.2f' % bm, flag))
    print()
    print('说明：实体相对框 =(0,0) ⇒ 按 frame 居中就已经让实体居中，无需补偿。')
    print('      重心相对框 ≠(0,0) ⇒ 图标本身视觉重心不在几何中心（美术如此，不是放置错误）。')


if __name__ == '__main__':
    main()
