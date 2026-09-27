#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
反推「原版图标 → 新版图标」的调色映射。

背景：EEGOPD 的 launcher 图标有两套。
  - legacy 扁平图  res/mipmap-*/ic_launcher.png        （用户已重上色）
  - 自适应图标    res/mipmap-anydpi-v26/ic_launcher.xml → _background + _foreground + _monochrome
                  （仅前景/后景分图层，用户未改）
因为 legacy 图与 foreground 出自同一套像素画，可以逐像素 diff 出「旧色 → 新色」，
再把该映射套用到 432×432 的 foreground 上 —— 几何零损失（只换调色板，不重采样）。

用法：
  python _chk/icon_palette_map.py <旧legacy.png> <新legacy.png> [foreground.png]
"""
import sys, os
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from png_palette_dump import load_png_any, pixels


def load(path):
    img = load_png_any(path)
    return img, list(pixels(img))


def main():
    if len(sys.argv) < 3:
        print(__doc__)
        sys.exit(0)
    old_p, new_p = sys.argv[1], sys.argv[2]
    io_, op = load(old_p)
    in_, np_ = load(new_p)
    assert io_['w'] == in_['w'] and io_['h'] == in_['h'], '尺寸不一致，无法逐像素 diff'

    pairs = Counter()
    unchanged = 0
    for a, b in zip(op, np_):
        if a[:3] == b[:3]:
            unchanged += 1
        else:
            pairs[(a[:3], b[:3])] += 1

    total = len(op)
    print('画布 %dx%d，共 %d 像素' % (io_['w'], io_['h'], total))
    print('未变色 %d (%.1f%%)，变色 %d (%.1f%%)' % (
        unchanged, 100.0 * unchanged / total, total - unchanged, 100.0 * (total - unchanged) / total))
    print()
    print('=== 主要变色对（旧 → 新，按像素数）===')
    for (a, b), c in pairs.most_common(30):
        print('  #%02X%02X%02X → #%02X%02X%02X   %6d' % (a[0], a[1], a[2], b[0], b[1], b[2], c))

    # 汇总成「每旧色 → 各新色」的分布，判断是否 1:1
    print()
    print('=== 按旧色汇总 ===')
    byold = {}
    for (a, b), c in pairs.items():
        byold.setdefault(a, Counter())[b] += c
    for a, cnt in sorted(byold.items(), key=lambda kv: -sum(kv[1].values())):
        tot = sum(cnt.values())
        tops = ', '.join('#%02X%02X%02X(%.0f%%)' % (b[0], b[1], b[2], 100.0 * c / tot)
                         for b, c in cnt.most_common(4))
        print('  #%02X%02X%02X  共%6d  →  %s' % (a[0], a[1], a[2], tot, tops))

    if len(sys.argv) >= 4:
        fg_p = sys.argv[3]
        fo, fp = load(fg_p)
        print()
        print('=== foreground %s (%dx%d) 调色板覆盖检查 ===' % (os.path.basename(fg_p), fo['w'], fo['h']))
        pal = Counter()
        for r, g, b, a in fp:
            if a > 0:
                pal[(r, g, b)] += 1
        for c, n in pal.most_common():
            hit = '有精确 diff 记录' if c in byold else ('★ 无记录（该色在 legacy 图里没出现）' if c != (0, 0, 0) else '透明色')
            print('  #%02X%02X%02X  x%-6d  %s' % (c[0], c[1], c[2], n, hit))


if __name__ == '__main__':
    main()
