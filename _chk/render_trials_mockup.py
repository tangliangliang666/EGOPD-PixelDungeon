# -*- coding: utf-8 -*-
"""把「考验选择界面」的定稿示意（生命之树窗口 + 详情小窗）渲染成位图。

几何直接复用 `_chk/render_tree_draft.py` 里的常量表，保证与内嵌 SVG 完全一致。
左侧：考验窗口（列表 → 生命之树），树按 1:1 缩放进窗口。
右侧：点节点后弹出的详情小窗（左上角图标、质点名标题、描述、开关）。
示意中 Chesed 用 row0（已开启）图标，其余九个用 row2（解锁未开启）。
"""
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__))))
from PIL import Image, ImageDraw

import render_tree_draft as R

BASE = r'D:/PD'
OUT = os.path.join(BASE, '_chk/_trials_ui_mockup.png')
ICON_DIR = os.path.join(BASE, '_chk/_tree_icons')

W, H = 680, 590
SS = 3
WIN = (30, 46, 310, 546)          # 左：考验窗口
POP = (350, 150, 650, 362)        # 右：详情小窗
TREE_DX, TREE_DY = -170, 28       # 树局部坐标 → 窗口内坐标

BORDER = (0xC6, 0xC3, 0xB8)
SEP = (0xDC, 0xDA, 0xD1)
INK = (0x2C, 0x2C, 0x2A)
GRAY = (0x6B, 0x6B, 0x65)
MUTE = (0x8A, 0x8A, 0x82)
ON = (0x0F, 0x6E, 0x56)


def icon(name, scale=3):
    im = Image.open(os.path.join(ICON_DIR, name)).convert('RGBA')
    if scale != 1:
        im = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
    return im


def main():
    c = Image.new('RGBA', (W * SS, H * SS), (255, 255, 255, 255))
    d = ImageDraw.Draw(c, 'RGBA')
    f11, f12, f13, f14 = (R.load_font(11 * SS), R.load_font(12 * SS),
                          R.load_font(13 * SS), R.load_font(14 * SS))

    d.text((170 * SS, 32 * SS), '考验窗口 · 列表改为生命之树', font=f12, fill=MUTE + (255,), anchor='ms')
    d.rounded_rectangle([WIN[0] * SS, WIN[1] * SS, WIN[2] * SS, WIN[3] * SS],
                        radius=6 * SS, fill=(0xF7, 0xF6, 0xF1, 255), outline=BORDER + (255,), width=SS)
    d.text((170 * SS, 68 * SS), '考验', font=f13, fill=INK + (255,), anchor='ms')
    d.line([(46 * SS, 80 * SS), (294 * SS, 80 * SS)], fill=SEP + (255,), width=SS)

    pos = {n[0]: (n[1], n[2]) for n in R.NODES}
    for a, b in R.EDGE_PAIRS:
        ax, ay = pos[a]
        bx, by = pos[b]
        dx, dy = bx - ax, by - ay
        L = (dx * dx + dy * dy) ** 0.5
        ux, uy = dx / L, dy / L
        d.line([((ax + ux * R.TRIM + TREE_DX) * SS, (ay + uy * R.TRIM + TREE_DY) * SS),
                ((bx - ux * R.TRIM + TREE_DX) * SS, (by - uy * R.TRIM + TREE_DY) * SS)],
               fill=(0, 0, 0, 89), width=int(2 * SS))

    STATE = {'Chesed': 'r0'}       # 其余用 r2（解锁未开启）
    for name, x, y, idx in R.NODES:
        row = STATE.get(name, 'r2')
        # 注意：_tree_icons/ 里的裁片本身已是 ICON_SCALE 倍，这里**不能再乘一次**
        ic = icon('%s_%s.png' % (row, name.lower()), 1)
        iw, ih = ic.width, ic.height
        px = (x + TREE_DX) * SS - iw * SS / 2
        py = (y + TREE_DY) * SS - ih * SS / 2
        big = ic.resize((iw * SS, ih * SS), Image.NEAREST)
        c.alpha_composite(big, (int(round(px)), int(round(py))))

    d.text((170 * SS, 566 * SS), '整棵树按比例缩放到一屏内，不滚动', font=f11, fill=MUTE + (255,), anchor='ms')

    cy = (pos['Chesed'][1] + TREE_DY) * SS
    for x0 in range(int(281 * SS), int(340 * SS), int(7 * SS)):
        d.line([(x0, cy), (x0 + 4 * SS, cy)], fill=MUTE + (255,), width=SS)
    d.polygon([(344 * SS, cy), (335 * SS, cy - 4 * SS), (335 * SS, cy + 4 * SS)], fill=MUTE + (255,))
    d.text((312 * SS, 254 * SS), '点节点', font=f11, fill=MUTE + (255,), anchor='ms')

    d.rounded_rectangle([POP[0] * SS, POP[1] * SS, POP[2] * SS, POP[3] * SS],
                        radius=6 * SS, fill=(255, 255, 255, 255), outline=BORDER + (255,), width=SS)
    pop_ic = icon('r0_chesed.png', 1)
    pop_ic = pop_ic.resize((40 * SS, int(40 * pop_ic.height / pop_ic.width) * SS), Image.NEAREST)
    c.alpha_composite(pop_ic, (366 * SS, 168 * SS))
    d.text((418 * SS, 192 * SS), 'CHESED', font=f14, fill=INK + (255,), anchor='ls')
    d.line([(366 * SS, 216 * SS), (634 * SS, 216 * SS)], fill=SEP + (255,), width=SS)
    for i, line in enumerate(['「怪物」仅指敌方单位，生命上限 +25%；',
                              '每 5 回合恢复 10% 生命上限的血量；',
                              '不给予经验的怪物不受影响。']):
        d.text((366 * SS, (238 + i * 18) * SS), line, font=f12, fill=GRAY + (255,), anchor='ls')
    d.rounded_rectangle([366 * SS, 316 * SS, 406 * SS, 338 * SS], radius=11 * SS, fill=ON + (255,))
    d.ellipse([379 * SS, 319 * SS, 395 * SS, 335 * SS], fill=(255, 255, 255, 255))
    d.text((418 * SS, 331 * SS), '开启该考验', font=f12, fill=INK + (255,), anchor='ls')
    d.text((500 * SS, 382 * SS), '点节点后弹出，盖在树上方', font=f12, fill=MUTE + (255,), anchor='ms')
    d.text((500 * SS, 400 * SS), '左上角＝该节点图标｜标题＝质点名｜描述取自 trials.chesed_desc',
           font=f11, fill=MUTE + (255,), anchor='ms')

    c.resize((W, H), Image.LANCZOS).convert('RGB').save(OUT)
    print('[OK] 已输出 %s  (%dx%d)' % (OUT, W, H))


if __name__ == '__main__':
    main()
