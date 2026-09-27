# -*- coding: utf-8 -*-
"""核验：草稿渲染里，图标到底有没有「偏左/偏上」。

做三件事：
  A. 用当前渲染口径（按 alpha 包围盒裁剪后居中）出图，逐点量「实体中心 vs 节点中心」；
  B. 用「裸 16x16 逐格居中」口径出同一张图，量同样的量 —— 作为对照；
  C. 把两种口径并排出图，节点中心打绿十字，让人眼也能直接比。

注意：本脚本**只画图标、不画路径线**，否则黑色路径会污染质心统计。
"""
import os
from PIL import Image

import render_tree_draft as R   # 复用几何常量：NODES / A / CX / Y0 / DH / W / H

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
OUT_A = os.path.join(BASE, '_chk/_icon_center_current.png')
OUT_B = os.path.join(BASE, '_chk/_icon_center_naive.png')
OUT_C = os.path.join(BASE, '_chk/_icon_center_compare.png')
NAME = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
        'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']


def load_cells(row):
    """返回 10 个**裸 16x16 格**（不裁包围盒）。"""
    im = Image.open(SPRITE).convert('RGBA')
    return [im.crop((i * 16, row * 16, i * 16 + 16, row * 16 + 16)) for i in range(10)]


def place(cells, mode, row):
    """mode='bbox' 按 alpha 包围盒裁剪后居中；mode='naive' 裸 16x16 居中。
    输出与 R.W x R.H 同尺寸的**纯图标**画布（**透明底**，让 alpha 只标记图标本体）。"""
    canvas = Image.new('RGBA', (R.W * R.SS, R.H * R.SS), (0, 0, 0, 0))
    for name, x, y, idx in R.NODES:
        cell = cells[idx]
        if mode == 'bbox':
            art = cell.crop(cell.getbbox())
        else:
            art = cell
        iw, ih = art.width * R.ICON_SCALE, art.height * R.ICON_SCALE
        big = art.resize((iw * R.SS, ih * R.SS), Image.NEAREST)
        canvas.alpha_composite(
            big, (int(round(x * R.SS - iw * R.SS / 2)),
                  int(round(y * R.SS - ih * R.SS / 2))))
    return canvas.resize((R.W, R.H), Image.LANCZOS)


def centroid(img, cx, cy, win, athr):
    """以 (cx,cy) 为中心 win 为半径，统计 alpha>athr 像素的包围盒中心与质心。"""
    px = list(img.get_flattened_data())
    W = img.width
    xs = ys = n = 0.0
    x0 = y0 = 10 ** 6
    x1 = y1 = -1
    for yy in range(max(0, cy - win), min(img.height, cy + win + 1)):
        for xx in range(max(0, cx - win), min(W, cx + win + 1)):
            a = px[yy * W + xx][3]
            if a > athr:
                xs += xx * a
                ys += yy * a
                n += a
                x0 = min(x0, xx); x1 = max(x1, xx)
                y0 = min(y0, yy); y1 = max(y1, yy)
    if n == 0:
        return None
    return ((x0 + x1) / 2.0, (y0 + y1) / 2.0, xs / n, ys / n)


def report(tag, img):
    print('--- %s ---' % tag)
    print('%-10s %-16s %-16s %s' % ('point', '包围盒中心-节点', '质心-节点', '判定'))
    worst = 0.0
    for name, x, y, idx in R.NODES:
        r = centroid(img, int(round(x)), int(round(y)), 25, 0)
        if r is None:
            print('%-10s (空)' % name); continue
        bb = (r[0] - x, r[1] - y)
        ms = (r[2] - x, r[3] - y)
        m = max(abs(bb[0]), abs(ms[0]))
        worst = max(worst, m)
        flag = 'ok' if m <= 1.0 else '<<< 偏了'
        print('%-10s %-16s %-16s %s' % (
            name, '%+.2f,%+.2f' % bb, '%+.2f,%+.2f' % ms, flag))
    print('   最大水平偏差 = %.2f px' % worst)
    print()


def main():
    def flatten(img):
        return Image.alpha_composite(Image.new('RGBA', img.size, (255, 255, 255, 255)), img).convert('RGB')

    cells = load_cells(R.ROW)
    a = place(cells, 'bbox', R.ROW)
    b = place(cells, 'naive', R.ROW)
    flatten(a).save(OUT_A)
    flatten(b).save(OUT_B)
    report('当前口径：按 alpha 包围盒裁剪后居中', a)
    report('对照口径：裸 16x16 逐格居中', b)

    # 并排对照图 + 绿十字
    from PIL import ImageDraw
    gap = 24
    cmp_img = Image.new('RGB', (R.W * 2 + gap, R.H + 30), (255, 255, 255))
    cmp_img.paste(flatten(a), (0, 30))
    cmp_img.paste(flatten(b), (R.W + gap, 30))
    d = ImageDraw.Draw(cmp_img)
    f = R.load_font(13)
    for i, (lab, ox) in enumerate((('当前：按实体包围盒居中', 0),
                                   ('对照：裸 16x16 居中（会偏）', R.W + gap))):
        d.text((ox + R.W // 2, 14), lab, font=f, fill=(0, 0, 0), anchor='ms')
    for name, x, y, idx in R.NODES:
        for ox in (0, R.W + gap):
            cx, cy = ox + x, 30 + y
            d.line([cx - 7, cy, cx + 7, cy], fill=(0, 200, 0), width=1)
            d.line([cx, cy - 7, cx, cy + 7], fill=(0, 200, 0), width=1)
    cmp_img.save(OUT_C)
    print('[OK] 已输出：%s / %s / %s' % (OUT_A, OUT_B, OUT_C))


if __name__ == '__main__':
    main()
