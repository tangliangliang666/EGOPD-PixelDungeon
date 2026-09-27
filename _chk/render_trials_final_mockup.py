# -*- coding: utf-8 -*-
"""最终方案预览：0.70 等比缩放后塞进 140 内容宽的考验窗口 + 详情小窗。

锁定参数（本轮用户决定）：
  等比缩放 s = 0.70（窗宽维持 140；不用 0.74 是为了左右各留 4px 呼吸位）
  a = 70         相邻节点距（逻辑像素）
  h = a*√3/2 ≈ 60.62
  图标**按整数像素显示** round(原生 13~16 × 0.70) = 9~11px
      —— 整数显示 + NEAREST ⇒ 每个图标自身不会出现「像素翻倍不匀」
  树外包 = 132.2 x 291   ⇒  内容宽 140 时左右各留 ~3.9px

渲染口径（关键）：面板先在 **1:1 虚拟像素**下画好，再用 **整数倍 NEAREST** 放大 VIEW 倍，
                 最后在放大图上补文字。这样图标/线条的观感与游戏内整数 zoom 完全一致。
"""
import math
import os

from PIL import Image, ImageDraw

import render_tree_draft as R

BASE = r'D:/PD'
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
OUT = os.path.join(BASE, '_chk/_trials_final_mockup.png')

S = 0.70                 # 等比缩放
A = 100.0 * S            # = 70.0
CONTENT_W = 140          # 窗口内容宽（= WndTrials.WIDTH，照旧）
TITLE_H = 16
PAD = 4
VIEW = 3                 # 预览放大倍数（整数，等价游戏内 camera.zoom）
DH = A * 3 ** 0.5 / 2.0
LINE_ALPHA = 89          # 半透明黑：0x00*35%
LINE_W = 1               # 2px * 0.70 → 1px

NAMES = ['KETER', 'HOKMA', 'BINAH', 'CHESED', 'GEBURA',
         'TIPHERETH', 'NETZACH', 'HOD', 'YESOD', 'MALKUTH']

# 名字必须与 R.EDGE_PAIRS 用的一致（Hokma 在右、Binah 在左）
NODES = [
    ('Keter',     0.0,  0.0,       0),
    ('Hokma',    +DH,   0.5 * A,   1),
    ('Binah',    -DH,   0.5 * A,   2),
    ('Chesed',   +DH,   1.5 * A,   3),
    ('Gebura',   -DH,   1.5 * A,   4),
    ('Tiphereth', 0.0,  2.0 * A,   5),
    ('Netzach',  +DH,   2.5 * A,   6),
    ('Hod',      -DH,   2.5 * A,   7),
    ('Yesod',     0.0,  3.0 * A,   8),
    ('Malkuth',   0.0,  4.0 * A,   9),
]
CHESED_IDX = 3

BG = (248, 246, 240, 255)
CHROME_OUT = (196, 190, 178, 255)
PAGE = (232, 228, 218, 255)


def load_rows():
    """按行读出 10 个图标（已裁包围盒），返回 rows[row][i] = (art, dw, dh)。"""
    im = Image.open(SPRITE).convert('RGBA')
    rows = []
    for r in range(3):
        row = []
        for i in range(10):
            f = im.crop((i * 16, r * 16, i * 16 + 16, r * 16 + 16))
            art = f.crop(f.getbbox())
            dw = max(1, int(art.width * S + 0.5))
            dh = max(1, int(art.height * S + 0.5))
            row.append((art, dw, dh))
        rows.append(row)
    return rows


def tree_extent(icon_rows):
    mdw = max(icon_rows[i][1] for i in range(10))
    mdh = max(icon_rows[i][2] for i in range(10))
    return (2 * DH + mdw), (4 * A + mdh), mdh


def draw_tree(img, ox, oy, icon_rows, enabled_idx):
    """在 1:1 画布上画树：ox/oy = 树外包框左上角落点。"""
    d = ImageDraw.Draw(img, 'RGBA')
    half = max(icon_rows[i][1] for i in range(10)) / 2.0
    vhalf = max(icon_rows[i][2] for i in range(10)) / 2.0
    pos = {}
    for name, nx, ny, idx in NODES:
        pos[name] = (ox + (nx + DH + half), oy + (ny + vhalf))
    trim = 0.28 * A
    for p, q in R.EDGE_PAIRS:
        ax, ay = pos[p]
        bx, by = pos[q]
        dx, dy = bx - ax, by - ay
        L = (dx * dx + dy * dy) ** 0.5
        ux, uy = dx / L, dy / L
        d.line([(ax + ux * trim, ay + uy * trim), (bx - ux * trim, by - uy * trim)],
               fill=(0, 0, 0, LINE_ALPHA), width=LINE_W)
    for name, nx, ny, idx in NODES:
        row = icon_rows if enabled_idx is None else None
        art, dw, dh = icon_rows[idx]
        big = art.resize((dw, dh), Image.NEAREST)
        cx, cy = pos[name]
        img.alpha_composite(big, (int(round(cx - dw / 2.0)), int(round(cy - dh / 2.0))))


def upscale(img):
    return img.resize((img.width * VIEW, img.height * VIEW), Image.NEAREST)


def panel_tree(rows):
    """① 树窗口：CHESED 已开启(row0 彩色)，其余未开启(row2 深灰)。"""
    mixed = [rows[0][i] if i == CHESED_IDX else rows[2][i] for i in range(10)]
    tw, th, mdh = tree_extent(mixed)
    ch = int(round(TITLE_H + PAD + th + PAD))
    cw = CONTENT_W
    # 1:1 内容 + chrome 6px
    img = Image.new('RGBA', (cw + 12, ch + 12), BG)
    d = ImageDraw.Draw(img, 'RGBA')
    d.rectangle([6, 6, cw + 5, ch + 5], fill=BG, outline=CHROME_OUT, width=1)
    ox = 6 + (cw - tw) / 2.0
    draw_tree(img, ox, 6 + TITLE_H + PAD, mixed, None)
    big = upscale(img)
    d = ImageDraw.Draw(big)
    d.text((big.width / 2, (6 + TITLE_H / 2) * VIEW), '考验',
           font=R.load_font(12 * VIEW), fill=(0x33, 0x33, 0x33), anchor='mm')
    return big


def panel_detail(rows):
    """② 详情小窗（继承 WndTitledMessage）：左上角图标 + 名称 + 描述 + 开关。"""
    cw = 150
    art, dw, dh = rows[0][3]                     # CHESED
    lines = ['濒死单位会获得短暂无敌，',
             '并在该回合僵直不动。',
             '（示例文案，实装取 trials.chesed_desc）']
    desc_lh = 12
    ch = TITLE_H + 2 + len(lines) * desc_lh + 4 + 18 + 2
    img = Image.new('RGBA', (cw + 12, ch + 12), BG)
    d = ImageDraw.Draw(img, 'RGBA')
    d.rectangle([6, 6, cw + 5, ch + 5], fill=BG, outline=CHROME_OUT, width=1)
    # 左上角图标（1:1，取原生 16 尺寸那一档：这里直接用原生大小，符合 WndTitledMessage 的 IconTitle）
    icon = rows[0][3][0]
    img.alpha_composite(icon, (10, 10))
    big = upscale(img)
    d = ImageDraw.Draw(big)
    fx20 = R.load_font(12 * VIEW)
    f12 = R.load_font(9 * VIEW)
    d.text((10 * VIEW + icon.width * VIEW + 6 * VIEW, (6 + TITLE_H / 2) * VIEW), 'CHESED',
           font=fx20, fill=(0x33, 0x33, 0x33), anchor='lm')
    y = (6 + TITLE_H + 2) * VIEW
    for ln in lines:
        d.text((10 * VIEW, y + desc_lh * VIEW / 2), ln, font=f12, fill=(0x44, 0x44, 0x44), anchor='lm')
        y += desc_lh * VIEW
    # 开关（CheckBox：按钮 + 右侧勾选框）
    bw = cw - 12
    by = (6 + ch - 20) * VIEW
    d.rectangle([10 * VIEW, by, (10 + bw) * VIEW, by + 16 * VIEW], fill=(0xE8, 0xE4, 0xDA, 255),
                outline=(0xB4, 0xB2, 0xA9, 255), width=VIEW)
    d.text((14 * VIEW, by + 8 * VIEW), '开启该考验', font=f12, fill=(0x33, 0x33, 0x33), anchor='lm')
    bx = (10 + bw - 4 - 9) * VIEW
    d.rectangle([bx, by + 3 * VIEW, bx + 9 * VIEW, by + 12 * VIEW],
                fill=(0xFF, 0xFF, 0xFF, 255), outline=(0x99, 0x99, 0x99, 255), width=VIEW)
    return big


def panel_states(rows):
    """③ 三态 + 0.70 缩放的两种重采样对比（都放大 6 倍看细节）。"""
    Z = 6
    items = []
    for lab, r in (('row1 锁定', 1), ('row2 未开启(初始)', 2), ('row0 已开启', 0)):
        items.append((lab, rows[r][3], Image.NEAREST))
    items.append(('0.70 用 LANCZOS', rows[2][3], Image.LANCZOS))
    cellw, cellh = 132, 96
    img = Image.new('RGBA', (cellw * len(items) + 8, cellh + 22), BG)
    d = ImageDraw.Draw(img, 'RGBA')
    f = R.load_font(4 * Z - 6)
    for k, (lab, (art, dw, dh), filt) in enumerate(items):
        x0 = 4 + k * cellw
        big = art.resize((dw * Z, dh * Z), filt)
        img.alpha_composite(big, (int(x0 + (cellw - 8 - big.width) / 2), 22))
        d.text((x0 + (cellw - 8) / 2, 12), lab, font=f, fill=(0x44, 0x44, 0x44), anchor='mm')
    return img


def main():
    rows = load_rows()
    p1 = panel_tree(rows)
    p2 = panel_detail(rows)
    p3 = panel_states(rows)

    right_w = max(p2.width, p3.width)
    gap = 14
    W = p1.width + gap * 3 + right_w
    H = max(p1.height, p2.height + p3.height + gap) + 44
    out = Image.new('RGBA', (W, H), (255, 255, 255, 255))
    d = ImageDraw.Draw(out)
    d.text((W / 2, 16), '考验界面 · 0.70 等比缩放方案（面板按 1:1 虚拟像素绘制后整数放大 %d 倍）' % VIEW,
           font=R.load_font(15), fill=(0, 0, 0), anchor='mm')
    out.alpha_composite(p1, (gap, 36))
    out.alpha_composite(p2, (gap * 2 + p1.width, 36))
    out.alpha_composite(p3, (gap * 2 + p1.width, 36 + p2.height + gap))
    out.convert('RGB').save(OUT)
    print('[OK] %s  (%dx%d)' % (OUT, W, H))
    print('     a=%.1f  h=%.2f  图标显示尺寸=%s' % (
        A, DH, ['%dx%d' % (rows[2][i][1], rows[2][i][2]) for i in range(10)]))
    tw, th, mdh = tree_extent(rows[2])
    print('     树外包 = %.1f x %.1f   内容宽 %d → 左右各留 %.1f' % (
        tw, th, CONTENT_W, (CONTENT_W - tw) / 2.0))


if __name__ == '__main__':
    main()
