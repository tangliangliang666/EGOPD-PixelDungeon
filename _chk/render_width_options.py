# -*- coding: utf-8 -*-
"""窗口宽度方案对比：140 内容宽到底装不装得下这棵树？

树（蜂窝 3-4-3，相邻节点距 a、图标原始 13~16px）的**实际外包尺寸**：
    宽 = a*√3 + 16      高 = 4a + 16
  a=100 -> 189.2 x 416     a=67 -> 132.0 x 284

三种方案（都画出来给人眼判）：
  ① a=100 原生、图标 1:1        -> 需要内容宽 ≈196（外框 208）
  ② a=100 整体缩到 0.74 塞进 140 -> 图标落到 ~11.8px，且非整数倍 ⇒ NEAREST 下会「花」
  ③ a=67 原生、图标 1:1          -> 140 装得下，但图标相对树更大更密（比例 0.16 -> 0.24）

渲染口径：1 逻辑像素 = Z(=2) 图像像素，面板内直接摆；图标 NEAREST 对齐游戏内取值。
"""
import os
from PIL import Image, ImageDraw

import render_tree_draft as R

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
OUT = os.path.join(BASE, '_chk/_width_options.png')
Z = 2                      # 面板放大倍数（仅为了看清，不改相对比例）
ROW = R.ROW                # 用哪一行图标（row2 未开启，深色，最考验辨识度最公平）

SQ3_2 = 3 ** 0.5 / 2.0


def nodes_for(a):
    """返回 (name, relx, rely, idx)，Keter 在 (0,0)，y 向下。"""
    dh = a * SQ3_2
    return [
        ('Keter',     0.0,  0.0,  0),
        ('Hokma',    +dh,   0.5 * a, 1),
        ('Binah',    -dh,   0.5 * a, 2),
        ('Chesed',   +dh,   1.5 * a, 3),
        ('Gebura',   -dh,   1.5 * a, 4),
        ('Tiphereth', 0.0,  2.0 * a, 5),
        ('Netzach',  +dh,   2.5 * a, 6),
        ('Hod',      -dh,   2.5 * a, 7),
        ('Yesod',     0.0,  3.0 * a, 8),
        ('Malkuth',   0.0,  4.0 * a, 9),
    ]


def load_icons(row):
    im = Image.open(SPRITE).convert('RGBA')
    out = []
    for i in range(10):
        f = im.crop((i * 16, row * 16, i * 16 + 16, row * 16 + 16))
        out.append((f.crop(f.getbbox()), f.getbbox()))
    return out


def draw_tree(canvas, ox, oy, a, iscale, icons, icon_ext, line_alpha=89, line_w=2):
    """把树画到 canvas 的 (ox,oy) 处。ox/oy = 树**外包框左上角**的落点。
    icon_ext = 该方案下图标在逻辑坐标里的外包边长（① ③ =16；② 整体缩小后 =16*s）。"""
    d = ImageDraw.Draw(canvas, 'RGBA')
    ns = nodes_for(a)
    # 外包框（相对坐标）
    half_max = icon_ext / 2.0           # 图标最大半宽/半高
    minx = min(n[1] for n in ns) - half_max
    miny = min(n[2] for n in ns) - half_max
    maxx = max(n[1] for n in ns) + half_max
    maxy = max(n[2] for n in ns) + half_max
    bw, bh = maxx - minx, maxy - miny

    def P(nx, ny):
        """相对坐标 -> 画布坐标（放大 Z 倍，并保证外包框左上落在 ox,oy）"""
        return ((nx - minx) * Z * 1.0 + ox, (ny - miny) * Z * 1.0 + oy)

    pos = {n[0]: P(n[1], n[2]) for n in ns}
    trim = 0.28 * a * Z
    for a_, b_ in R.EDGE_PAIRS:
        ax, ay = pos[a_]
        bx, by = pos[b_]
        dx, dy = bx - ax, by - ay
        L = (dx * dx + dy * dy) ** 0.5
        ux, uy = dx / L, dy / L
        d.line([(ax + ux * trim, ay + uy * trim), (bx - ux * trim, by - uy * trim)],
               fill=(0, 0, 0, line_alpha), width=max(1, int(round(line_w * Z * iscale / 3.0))))

    for name, nx, ny, idx in ns:
        art, bbox = icons[idx]
        # 目标像素宽 = 原宽 * Z * (iscale/3)：① ③ iscale=3 ⇒ 整数倍（清晰）；② 非整数倍（会花）
        iw = max(1, int(round(art.width * Z * iscale / 3.0)))
        ih = max(1, int(round(art.height * Z * iscale / 3.0)))
        big = art.resize((iw, ih), Image.NEAREST)
        cx, cy = pos[name]
        canvas.alpha_composite(
            big, (int(round(cx - iw / 2.0)), int(round(cy - ih / 2.0))))
    return bw * Z, bh * Z


def panel(title, sub, content_w, a, iscale, icons, icon_ext, title_h=16, pad=6):
    """出一块面板：外框 = 内容 + Chrome 边距 6，标题在顶部，下面一行说明。
    所有尺寸在这里都已经是**设备像素**（已乘 Z）。"""
    tz = Z
    m = 6 * tz                                   # Chrome 边距
    tree_h = (4 * a + icon_ext) * tz             # 树设备高
    content_h = (title_h + pad) * tz + tree_h + pad * tz
    total_w = int((content_w + 12) * tz)
    total_h = int(content_h + m * 2 + 34 * tz)   # 底部留说明行
    img = Image.new('RGBA', (total_w, total_h), (232, 228, 218, 255))
    d = ImageDraw.Draw(img, 'RGBA')
    d.rectangle([m, m, total_w - m - 1, total_h - 34 * tz - m], fill=(248, 246, 240, 255),
                outline=(196, 190, 178, 255), width=tz)
    f = R.load_font(13 * tz if total_w > 300 else 11 * tz)
    fs = R.load_font(10 * tz if total_w > 300 else 9 * tz)
    d.text((total_w / 2, m + 8 * tz), title, font=f, fill=(0x33, 0x33, 0x33), anchor='ms')
    # 树：水平居中，顶部对齐标题下方
    tree_w = (a * 3 ** 0.5 + icon_ext) * tz
    ox = m + (content_w * tz - tree_w) / 2.0
    draw_tree(img, ox, m + (title_h + pad) * tz, a, iscale, icons, icon_ext)
    d.text((total_w / 2, total_h - 28 * tz), sub, font=fs, fill=(0x44, 0x44, 0x44), anchor='ms')
    return img


def main():
    icons = load_icons(ROW)
    S = 0.74                                     # 140 / 189.2
    p1 = panel('① a=100 原生 · 内容宽 196', '图标 1:1（13~16px）· 树 189x416 · 外框 208',
               196, 100.0, 3.0, icons, 16.0)
    p2 = panel('② a=74 整体缩 0.74 · 内容宽 140', '图标 ~11.8px（非整数倍，会花）· 树 140x308',
               140, 100.0 * S, 3.0 * S, icons, 16.0 * S)
    p3 = panel('③ a=67 原生 · 内容宽 140', '图标 1:1（13~16px）· 树 132x284 · 更密（比例 .16->.24）',
               140, 67.0, 3.0, icons, 16.0)

    gap = 18
    W = p1.width + p2.width + p3.width + gap * 4
    H = max(p1.height, p2.height, p3.height) + 40
    out = Image.new('RGBA', (W, H), (255, 255, 255, 255))
    x = gap
    for p in (p1, p2, p3):
        out.alpha_composite(p, (x, 20))
        x += p.width + gap
    f = R.load_font(15)
    d = ImageDraw.Draw(out)
    d.text((W / 2, 14), '考验界面 · 窗口宽度三方案（1 逻辑像素 = %d 图像像素）' % Z,
           font=f, fill=(0, 0, 0), anchor='ms')
    out.convert('RGB').save(OUT)
    print('[OK] %s  (%dx%d)' % (OUT, W, H))


if __name__ == '__main__':
    main()
