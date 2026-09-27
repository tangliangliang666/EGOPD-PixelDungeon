# -*- coding: utf-8 -*-
"""诊断：tree.png 里每个图标的「实体核心」相对我裁出的包围盒中心偏了多少。

背景：bbox(alpha>0) 会把 1px 半透明描边算进去，导致裁出的图比实体核心大一整圈，
      再按裁剪框居中 ⇒ 实体核心被推向左上。这里把偏移量量化出来。
产出：_chk/_icon_center_diag.png —— 10 个图标的实体图（放大）+ 裁剪框中心十字 + 实体重心十字。
"""
import os
from PIL import Image, ImageDraw

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
OUT = os.path.join(BASE, '_chk/_icon_center_diag.png')
NAMES = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
         'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']
S = 8


def centroid(px, w, h):
    tot = sx = sy = 0.0
    for i, p in enumerate(px):
        a = p[3]
        if a:
            tot += a
            sx += (i % w) * a
            sy += (i // w) * a
    return (sx / tot, sy / tot) if tot else (0.0, 0.0)


def main():
    im = Image.open(SPRITE).convert('RGBA')
    tiles = []
    print('%-10s %-9s %-9s %-9s %-9s %s' % ('point', 'cropSize', 'bboxCenter', 'massCenter', 'bias', 'at 3x'))
    for i, nm in enumerate(NAMES):
        cell = im.crop((i * 16, 0, i * 16 + 16, 16))
        crop = cell.crop(cell.getbbox())
        w, h = crop.size
        px = list(crop.get_flattened_data())
        mx, my = centroid(px, w, h)
        cx, cy = (w - 1) / 2.0, (h - 1) / 2.0
        dx, dy = mx - cx, my - cy
        print('%-10s %-9s %-9s %-9s %-9s %s' % (
            nm, '%dx%d' % (w, h), '%.1f,%.1f' % (cx, cy), '%.2f,%.2f' % (mx, my),
            '%+.2f,%+.2f' % (dx, dy), '%+.1f,%+.1f px' % (dx * 3, dy * 3)))
        tiles.append(crop)

    pad = 10
    cw = max(t.width for t in tiles) * S + pad * 2
    ch = max(t.height for t in tiles) * S + pad * 2 + 16
    out = Image.new('RGBA', (len(tiles) * cw, ch), (255, 255, 255, 255))
    d = ImageDraw.Draw(out)
    for i, t in enumerate(tiles):
        big = t.resize((t.width * S, t.height * S), Image.NEAREST)
        ox = i * cw + pad
        oy = pad
        out.paste(big, (ox, oy), big)
        # 裁剪框中心（绿）—— 我按它居中
        gx, gy = ox + big.width / 2.0, oy + big.height / 2.0
        d.line([(gx - 14, gy), (gx + 14, gy)], fill=(0x0F, 0x6E, 0x56, 255), width=2)
        d.line([(gx, gy - 14), (gx, gy + 14)], fill=(0x0F, 0x6E, 0x56, 255), width=2)
        # 实体框（橙）—— 应当按它居中
        px = list(t.get_flattened_data())
        solid = [(j % t.width, j // t.width) for j, p in enumerate(px) if p[3] >= 128]
        if solid:
            xs = [q[0] for q in solid]
            ys = [q[1] for q in solid]
            d.rectangle([ox + (min(xs) + .5) * S, oy + (min(ys) + .5) * S,
                         ox + (max(xs) + 1.5) * S, oy + (max(ys) + 1.5) * S],
                        outline=(0xD8, 0x5A, 0x30, 255), width=2)
        d.text((ox, oy + big.height + 3), '%d %s' % (i, NAMES[i][:6]), fill=(0, 0, 0, 255))
    out.save(OUT)
    print('\n[OK] 诊断图 %s  (%dx%d)  绿十字=裁剪框中心 / 橙框=实体核心' % (OUT, out.width, out.height))


if __name__ == '__main__':
    main()
