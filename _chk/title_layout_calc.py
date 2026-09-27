#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""标题界面（TitleScene）布局复算器：不启动游戏就能预览排版与溢出。

逐字复刻 `scenes/TitleScene.java` 的 create() 排版段（原文件 96~271 行），
用来回答「改一处会不会把按钮挤出屏幕」「某分辨率下第几行开始越界」。

关键事实（读源码得到，别凭印象）：
  * 相机尺寸 = Game.width/zoom × Game.height/zoom，且 zoom ≤ min(w/minWidth, h/minHeight)
    ⇒ `Camera.main` 至少是 (240,160) 横屏 / (135,225) 竖屏，实际随设备长宽比变大。
    （`PixelScene.create()` 111~145 行）
  * 标题贴图尺寸写死在 `effects/BannerSprites.java` 的 uvRect 里：
    竖屏 TITLE_PORT 139×100、横屏 TITLE_LAND 240×157。
  * `topRegion = max(titleH - 6, h*0.45)`（TitleScene:110）
  * `GAP = int((h - topRegion - N*20)/3)`，N = 横屏 3 / 竖屏 4，再 `//3` 或 `//5`，最后 `max(GAP,2)`（:217~220）
    ⚠️ GAP 的预算只按 N 行算，而**实际画出来的行数是另一回事** —— 见下方行数核对。

用法：
    python _chk/title_layout_calc.py                 # 默认几组典型相机尺寸
    python _chk/title_layout_calc.py 400 180         # 指定 宽 高（横屏）
    python _chk/title_layout_calc.py 180 400         # 指定 宽 高（竖屏）
    python _chk/title_layout_calc.py --sw 400 --sh 180 --insets 24,0,0,8
"""

import argparse
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

MIN_W_L, MIN_H_L = 240.0, 160.0
MIN_W_P, MIN_H_P = 135.0, 225.0
# BannerSprites 的 uvRect(x0, y0, x1, y1) 是**像素矩形** ⇒ 尺寸 = (x1-x0) × (y1-y0)。
# ⚠️ 2026-09-21 用户手工重排了 banners.png，这里是**新版面**：
#     移动端 (0,0)-(169,94)   → 169×94
#     PC 端  (176,0)-(427,65) → 251×65      （闪烁帧已置空，不影响排版）
# 旧值曾是 竖 139×100 / 横 240×57。历史上这里算错过一次（把 uvRect 后两参当尺寸、
# 得出横屏 157 高），把整屏布局结论带反了 —— 改动时务必按 (x1-x0)×(y1-y0) 算。
TITLE_W_L, TITLE_H_L = 427.0 - 176.0, 65.0 - 0.0      # 251 × 65
TITLE_W_P, TITLE_H_P = 169.0 - 0.0, 94.0 - 0.0        # 169 × 94
BTN_HEIGHT = 20.0

# 每个按钮：(key, 中文名, 是否真的被 add 进场景)
# btnNews 在 create() 里被**注释掉**（TitleScene:198-199，本项目相对上游的唯一改动），
# 但排版仍按它的位置摆放 btnChanges / btnSettings / btnAbout。
BUTTONS = [
    ('play', '进入地牢', True),
    ('support', '支持游戏开发', True),
    ('rankings', '排行榜', True),
    ('journal', '日志', True),
    ('news', '游戏新闻', False),
    ('changes', '改动', True),
    ('settings', '设置', True),
    ('about', '关于', True),
]


def align(v):
    """PixelScene.align(float)：四舍五入到整数像素。"""
    return float(int(round(v)))


def layout(cam_w, cam_h, insets=(0.0, 0.0, 0.0, 0.0)):
    """insets = (top, bottom, left, right)，已按 1/zoom 缩放（getCommonInsets()）。"""
    top_i, bot_i, left_i, right_i = insets
    land = cam_w > cam_h
    w = cam_w - left_i - right_i
    h = cam_h - top_i - bot_i

    title_w, title_h = (TITLE_W_L, TITLE_H_L) if land else (TITLE_W_P, TITLE_H_P)
    top_region = max(title_h - 6.0, h * 0.45)

    rows_budget = 3 if land else 4        # 源码里的 (landscape()?3:4)
    gap = int((h - top_region - rows_budget * BTN_HEIGHT) / 3)
    gap //= 3 if land else 5
    gap = max(gap, 2)

    area_w = (MIN_W_L - 6.0) if land else (MIN_W_P - 2.0)
    area_left = left_i + (w - area_w) / 2.0

    r = {}
    if land:
        half = area_w / 2.0 - 1.0
        third = float(int(area_w / 3.0) - 1)
        r['play'] = (area_left, top_i + top_region + gap, half, BTN_HEIGHT)
        r['support'] = (r['play'][0] + half + 2, r['play'][1], half, BTN_HEIGHT)
        r['rankings'] = (area_left, r['play'][1] + BTN_HEIGHT + gap, third, BTN_HEIGHT)
        r['journal'] = (r['rankings'][0] + third + 2, r['rankings'][1], third, BTN_HEIGHT)
        r['news'] = (r['journal'][0] + third + 2, r['journal'][1], third, BTN_HEIGHT)
        r['settings'] = (area_left, r['rankings'][1] + BTN_HEIGHT + gap, third, BTN_HEIGHT)
        r['changes'] = (r['settings'][0] + third + 2, r['settings'][1], third, BTN_HEIGHT)
        r['about'] = (r['changes'][0] + third + 2, r['settings'][1], third, BTN_HEIGHT)
        drawn_rows = 3
    else:
        half = area_w / 2.0 - 1.0
        r['play'] = (area_left, top_i + top_region + gap, area_w, BTN_HEIGHT)
        r['support'] = (r['play'][0], r['play'][1] + BTN_HEIGHT + gap, area_w, BTN_HEIGHT)
        r['rankings'] = (area_left, r['support'][1] + BTN_HEIGHT + gap, half, BTN_HEIGHT)
        r['journal'] = (r['rankings'][0] + half + 2, r['rankings'][1], half, BTN_HEIGHT)
        r['news'] = (area_left, r['rankings'][1] + BTN_HEIGHT + gap, half, BTN_HEIGHT)
        r['changes'] = (r['news'][0] + half + 2, r['news'][1], half, BTN_HEIGHT)
        r['settings'] = (area_left, r['news'][1] + BTN_HEIGHT + gap, half, BTN_HEIGHT)
        r['about'] = (r['settings'][0] + half + 2, r['settings'][1], half, BTN_HEIGHT)
        drawn_rows = 5

    for k in r:
        r[k] = tuple(align(v) for v in r[k])
    return {
        'land': land, 'w': w, 'h': h, 'cam_w': cam_w, 'cam_h': cam_h, 'insets': insets,
        'title': (title_w, title_h),
        'top_region': top_region, 'gap': gap, 'area_w': area_w,
        'area_left': area_left, 'rects': r, 'rows_budget': rows_budget,
        'rows_drawn': drawn_rows,
        'title_x': align(left_i + (w - title_w) / 2.0),
        'title_y': align(top_i + 2 + (top_region - title_h) / 2.0),
        # btnFade 用的是 camera.main.height，**没有**减去 bottom inset 之外的 top inset
        'fade_y': align(cam_h - 16 - bot_i),
        'fade_x': align(area_left + (area_w - 16) / 2.0),
    }


def min_camera_height(land):
    """排版装得下所需的最小相机高度（二分，用 layout() 自身的 GAP 规则）。

    横屏要让 3 行按钮塞进 topRegion 以下；topRegion = max(titleH-6, h*0.45)
    ⇒ 矮屏时 topRegion 恒为 151，需要 h ≥ 151 + 3*20 + 3*GAP(≥2) = 217。
    竖屏要装 5 行，h ≥ 225 时恒满足。
    """
    lo, hi = 60.0, 2000.0
    for _ in range(60):
        mid = (lo + hi) / 2
        cand = mid + (1.0 if mid > 0 else 0.0)  # 保证 mid>60 时判定为横屏/竖屏合理
        w = (cand * 1.6) if land else (cand / 1.6)
        L = layout(w, cand)
        drawn = [L['rects'][k] for k, _, a in BUTTONS if a]
        worst = max(y + bh for _, y, _, bh in drawn)
        if worst <= L['h'] + 1e-9:
            hi = mid
        else:
            lo = mid
    return hi


def ascii_map(cam_w, cam_h, insets=(0.0, 0.0, 0.0, 0.0), cols=76):
    """把排版画成 ASCII 图：每个按钮一个方框，空槽位用 · 标出。"""
    L = layout(cam_w, cam_h, insets)
    cw, ch = L['cam_w'], L['cam_h']
    rows = max(12, int(cols * ch / cw / 2.1))  # 字符高宽比补偿
    grid = [[' '] * cols for _ in range(rows)]

    def put(x, y, s):
        xi, yi = int(x), int(y)
        for k, c in enumerate(s):
            if 0 <= yi < rows and 0 <= xi + k < cols:
                grid[yi][xi + k] = c

    def box(x, y, w, h, label, fill=None):
        x0 = int(round(x / cw * cols))
        y0 = int(round(y / ch * rows))
        x1 = int(round((x + w) / cw * cols)) - 1
        y1 = int(round((y + h) / ch * rows)) - 1
        if x1 <= x0:
            x1 = x0 + 1
        if y1 <= y0:
            y1 = y0
        for xx in range(x0, x1 + 1):
            for yy in range(y0, y1 + 1):
                if 0 <= yy < rows and 0 <= xx < cols:
                    edge = xx in (x0, x1) or yy in (y0, y1)
                    grid[yy][xx] = '+' if (edge and xx in (x0, x1) and yy in (y0, y1)) \
                        else ('-' if yy in (y0, y1) else ('|' if edge else (fill or ' ')))
        if label:
            put(x0 + 2, y0, ' ' + label + ' ')

    # 标题 + 发光层
    box(L['title_x'], L['title_y'], L['title'][0], L['title'][1], '标题贴图 %d×%d'
        % (L['title'][0], L['title'][1]), fill='.')
    # 可用区底边
    put(0, min(rows - 1, int(round((insets[0] + L['h']) / ch * rows))), '#' * cols)
    # 按钮
    added = {k: a for k, _, a in BUTTONS}
    names = {k: cn for k, cn, _ in BUTTONS}
    for key, cn, is_added in BUTTONS:
        x, y, bw, bh = L['rects'][key]
        box(x, y, bw, bh, cn if is_added else '（空槽）', fill='x' if is_added else ':')
    # 下折箭头
    box(L['fade_x'], L['fade_y'], 16, 16, 'v')

    print('ASCII 排版图（"#" = 可用区底边，"x" = 已显示按钮，":" = news 空槽）')
    for line in grid:
        print('  |' + ''.join(line) + '|')
    print('  尺寸 %.0f×%.0f 字符映射，非等比；精确坐标看上面的表。' % (cols, rows))


def report(cam_w, cam_h, insets=(0.0, 0.0, 0.0, 0.0)):
    L = layout(cam_w, cam_h, insets)
    print('=' * 78)
    print('相机 %.0f×%.0f  insets(top,bottom,left,right)=%s  ⇒  %s  (可用 %.0f×%.0f)'
          % (cam_w, cam_h, tuple(int(v) for v in insets),
             '横屏' if L['land'] else '竖屏', L['w'], L['h']))
    print('  标题贴图 %.0f×%.0f   标题左上=(%.0f,%.0f)   topRegion=%.1f   GAP=%d'
          % (L['title'][0], L['title'][1], L['title_x'], L['title_y'], L['top_region'], L['gap']))
    print('  按钮区宽=%.0f  左=%.1f   下折箭头=(%.0f,%.0f)'
          % (L['area_w'], L['area_left'], L['fade_x'], L['fade_y']))
    print('  %-12s %-18s %5s %5s %5s %5s  %s' % ('按钮', '中文名', 'x', 'y', 'w', 'h', '状态'))
    max_bottom = 0.0
    for key, cn, added in BUTTONS:
        x, y, bw, bh = L['rects'][key]
        bottom = y + bh
        if added:
            max_bottom = max(max_bottom, bottom)
        over = ''
        if bottom > L['h']:
            over = '  ← 超出可用高度 %.0fpx' % (bottom - L['h'])
        print('  %-12s %-18s %5.0f %5.0f %5.0f %5.0f  %s%s'
              % (key, cn, x, y, bw, bh, '已显示' if added else '**未 add（仅占位）**', over))
    print('  最底边(仅已显示按钮) = %.0f  /  可用高 %.0f   ⇒ 余量 %.0f px'
          % (max_bottom, L['h'], L['h'] - max_bottom))
    if L['rows_drawn'] != L['rows_budget']:
        print('  ⚠️ GAP 预算按 %d 行算，实际画出 %d 行 ⇒ 预留的高度不够，屏矮时最后一行会贴边/溢出'
              % (L['rows_budget'], L['rows_drawn']))
    need = min_camera_height(L['land'])
    print('  所需最小相机高度 = %.0f px（当前 %.0f，%s）'
          % (need, cam_h, '够' if cam_h >= need else '**不够，会溢出**'))
    # 空位提示
    if not dict((k, a) for k, _, a in BUTTONS)['news']:
        if L['land']:
            print('  ⚠️ 横屏：第 2 行第 3 列（news 槽位）为**空**，改动按钮被排到第 3 行第 2 列')
        else:
            print('  ⚠️ 竖屏：news 所在整行只剩右半边的「改动」按钮，**左半边为空洞**')
    return L


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('size', nargs='*', type=float, help='相机 宽 高')
    ap.add_argument('--insets', default='0,0,0,0', help='top,bottom,left,right')
    ap.add_argument('--ascii', action='store_true', help='额外打印 ASCII 排版图')
    args = ap.parse_args()

    ins = tuple(float(v) for v in args.insets.split(','))
    if len(ins) != 4:
        sys.exit('--insets 需要 4 个值：top,bottom,left,right')

    if len(args.size) == 2:
        report(args.size[0], args.size[1], ins)
        if args.ascii:
            ascii_map(args.size[0], args.size[1], ins)
    elif len(args.size) == 0:
        print('### 典型相机尺寸（相机 = 屏幕像素 / zoom，至少到各模式的最小值）###')
        for cw, ch in [(426, 240), (400, 180), (240, 160),   # 横屏
                       (180, 320), (135, 225), (200, 400)]:  # 竖屏
            report(cw, ch, ins)
    else:
        sys.exit('尺寸要么给两个数，要么不给')
    return 0


if __name__ == '__main__':
    sys.exit(main())
