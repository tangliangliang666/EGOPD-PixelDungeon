# -*- coding: utf-8 -*-
"""把「生命之树 · 考验选择界面」草稿渲染成位图（与 show_widget 的 SVG 同一套几何）。

用途：SVG 内嵌本地图片可能受渲染环境限制，这里用 PIL 出一张**确定**的对照图。
几何与样式全部由下面的常量推导，改参数即改图。

坐标口径（逻辑坐标，最后整体放大 SS 倍再降采样做抗锯齿）：
  a = 100              相邻节点距（草稿四里 = 2r + 12，即圆留缝 12px）
  h = a*√3/2 = 86.6    列间距（蜂窝条件）
  TRIM = 28            路径端点距圆心的退让量（无圆时＝给图标留的余量）
中列在 y = 0 / 2a / 3a / 4a（2a 处是空掉的 Da'at 位，纯空白），左右列在 0.5a / 1.5a / 2.5a。

tree.png 现有三行（160×48），**三行包围盒完全一致**，仅用色不同：
  row 0 彩色（平均亮度 117 / 饱和度 63）—— 考验**已开启**
  row 1 浅灰褪色（亮度 86 / 饱和度 30）—— 用户指定为「**锁定**」
  row 2 深色近单色（亮度 57 / 饱和度 12）—— 用户指定为「**解锁未开启**」（初始态）
"""
import os
from PIL import Image, ImageDraw, ImageFont

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
OUT = os.path.join(BASE, '_chk/_tree_draft_render.png')

ROW = 2                # 渲染用哪一行图标（0=已开启 / 1=锁定 / 2=未开启）
ICON_SCALE = 3         # 图标相对原图的放大倍数
DRAW_CIRCLES = False   # 是否画定位圆（用户：圆只是设计期定位用，实装不要）

A = 100.0
CX = 340.0
Y0 = 84.0
DH = A * 3 ** 0.5 / 2.0
TRIM = 28.0
R = 44.0               # 仅在 DRAW_CIRCLES=True 时有意义
LEFT_X = CX - DH
RIGHT_X = CX + DH

NODES = [
    ('Keter',     CX,      Y0,            0),
    ('Binah',     LEFT_X,  Y0 + A * 0.5,  2),
    ('Hokma',     RIGHT_X, Y0 + A * 0.5,  1),
    ('Gebura',    LEFT_X,  Y0 + A * 1.5,  4),
    ('Chesed',    RIGHT_X, Y0 + A * 1.5,  3),
    ('Tiphereth', CX,      Y0 + A * 2.0,  5),
    ('Hod',       LEFT_X,  Y0 + A * 2.5,  7),
    ('Netzach',   RIGHT_X, Y0 + A * 2.5,  6),
    ('Yesod',     CX,      Y0 + A * 3.0,  8),
    ('Malkuth',   CX,      Y0 + A * 4.0,  9),
]

PILLAR = {'left': (0x99, 0x3C, 0x1D), 'mid': (0x53, 0x4A, 0xB7), 'right': (0x0F, 0x6E, 0x56)}
PILLAR_OF = {'Binah': 'left', 'Gebura': 'left', 'Hod': 'left',
             'Keter': 'mid', 'Tiphereth': 'mid', 'Yesod': 'mid', 'Malkuth': 'mid',
             'Hokma': 'right', 'Chesed': 'right', 'Netzach': 'right'}

EDGE_PAIRS = [
    ('Keter', 'Hokma'), ('Keter', 'Binah'), ('Keter', 'Tiphereth'),
    ('Hokma', 'Binah'),
    ('Hokma', 'Chesed'), ('Hokma', 'Tiphereth'),
    ('Binah', 'Gebura'), ('Binah', 'Tiphereth'),
    ('Chesed', 'Gebura'), ('Chesed', 'Tiphereth'), ('Chesed', 'Netzach'),
    ('Gebura', 'Tiphereth'), ('Gebura', 'Hod'),
    ('Tiphereth', 'Netzach'), ('Tiphereth', 'Hod'), ('Tiphereth', 'Yesod'),
    ('Netzach', 'Hod'), ('Netzach', 'Yesod'),
    ('Hod', 'Yesod'),
    ('Netzach', 'Malkuth'), ('Hod', 'Malkuth'),
    ('Yesod', 'Malkuth'),
]

W, H = 680, 528
SS = 3


def load_font(px):
    for p in (r'C:/Windows/Fonts/msyh.ttc', r'C:/Windows/Fonts/simhei.ttf',
              r'C:/Windows/Fonts/arial.ttf'):
        if os.path.exists(p):
            return ImageFont.truetype(p, px)
    return ImageFont.load_default()


def crop_icons(row):
    im = Image.open(SPRITE).convert('RGBA')
    out = []
    for i in range(10):
        f = im.crop((i * 16, row * 16, i * 16 + 16, row * 16 + 16))
        out.append(f.crop(f.getbbox()))
    return out


def main():
    pos = {n[0]: (n[1], n[2]) for n in NODES}
    icons = crop_icons(ROW)
    canvas = Image.new('RGBA', (W * SS, H * SS), (255, 255, 255, 255))
    d = ImageDraw.Draw(canvas, 'RGBA')

    if DRAW_CIRCLES:
        for label, x in (('严厉之柱', LEFT_X), ('均衡之柱', CX), ('慈悲之柱', RIGHT_X)):
            d.text((x * SS, 22 * SS), label, font=load_font(11 * SS),
                   fill=(0x8A, 0x8A, 0x82, 255), anchor='ms')

    for a, b in EDGE_PAIRS:
        ax, ay = pos[a]
        bx, by = pos[b]
        dx, dy = bx - ax, by - ay
        L = (dx * dx + dy * dy) ** 0.5
        ux, uy = dx / L, dy / L
        d.line([((ax + ux * TRIM) * SS, (ay + uy * TRIM) * SS),
                ((bx - ux * TRIM) * SS, (by - uy * TRIM) * SS)],
               fill=(0, 0, 0, 89), width=int(2 * SS))

    if DRAW_CIRCLES:
        gx, gy = CX, Y0 + A
        for deg in range(0, 360, 12):
            d.arc([(gx - R) * SS, (gy - R) * SS, (gx + R) * SS, (gy + R) * SS],
                  start=deg, end=deg + 6, fill=(0xE0, 0xDE, 0xD5, 255), width=SS)

    for name, x, y, idx in NODES:
        icon = icons[idx]
        iw, ih = icon.width * ICON_SCALE, icon.height * ICON_SCALE
        if DRAW_CIRCLES:
            col = PILLAR[PILLAR_OF[name]]
            d.ellipse([(x - R) * SS, (y - R) * SS, (x + R) * SS, (y + R) * SS],
                      fill=(0xF1, 0xEF, 0xE8, 255), outline=col + (255,), width=int(1.5 * SS))
        big = icon.resize((iw * SS, ih * SS), Image.NEAREST)
        canvas.alpha_composite(
            big, (int(round(x * SS - iw * SS / 2)), int(round(y * SS - ih * SS / 2))))
        if DRAW_CIRCLES:
            d.text((x * SS, (y + 31) * SS), name, font=load_font(11 * SS),
                   fill=PILLAR[PILLAR_OF[name]] + (255,), anchor='ms')

    out = canvas.resize((W, H), Image.LANCZOS)
    out.convert('RGB').save(OUT)
    print('[OK] 已输出 %s  (%dx%d)' % (OUT, W, H))
    print('     节点 %d / 路径 %d / 图标行 row%d / 画定位圆=%s'
          % (len(NODES), len(EDGE_PAIRS), ROW, DRAW_CIRCLES))


if __name__ == '__main__':
    main()
