#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把「legacy 扁平图」的重上色结果，同步到「自适应图标」的 foreground 分图层。

背景（为什么需要这个脚本）：
  Android 8.0+（API 26+）的启动器只认 res/mipmap-anydpi-v26/ic_launcher.xml 这个
  自适应图标，它引用 _background + _foreground 两张分图层；res/mipmap-*/ic_launcher.png
  这套扁平图只在 API < 26 的旧设备上才会被用到。
  只重上色扁平图 ⇒ 新图标在真机上完全不出现。

做法（几何零损失 + 保留「分裂色」）：
  legacy 图与 foreground 出自同一套像素画，实测关系为
      legacy_sprite = foreground_sprite × 0.75 ，左上角对齐 (12,12) ↔ (104,104)
  （面积比 0.5625 = 0.75²，各调色板色的像素数在两张图里严格成 16:9 ⇒ 可精确反查）。
  于是逐像素「按位置」搬运：foreground 的每个不透明像素，去 legacy 新图里取它对应的
  那个像素的颜色。这样能保留「同一旧色裂成多个新色」的情况——例如 #DD9800 金色，
  外框被染成红、钥匙孔牌面仍保持金色，纯调色板映射做不到这一点。

用法：
  python _chk/icon_adaptive_recolor.py            # 只预览，导出对照图到 _chk/_icon/
  python _chk/icon_adaptive_recolor.py --apply    # 实际改写 10 张 ic_launcher_foreground.png
"""
import os, sys, subprocess, zlib, struct
from collections import Counter

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from png_palette_dump import load_png_any, pixels

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ICON_DIR = os.path.join(ROOT, '_chk', '_icon')
LEGACY_REL = 'android/src/main/res/mipmap-xxxhdpi/ic_launcher.png'
FG_REL = 'android/src/main/res/mipmap-xxxhdpi/ic_launcher_foreground.png'
BG_REL = 'android/src/main/res/mipmap-xxxhdpi/ic_launcher_background.png'
DENSITIES = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']
VARIANTS = ['main', 'debug']
APPLY = '--apply' in sys.argv

# foreground 的原始调色板（用于判定「这个 legacy 像素是不是精灵本体」）
FG_PAL = {(0x00, 0x00, 0x00), (0xDD, 0x98, 0x00), (0x0A, 0x3D, 0x0A), (0x0E, 0x4F, 0x0E),
          (0xFF, 0xEB, 0x7B), (0x13, 0x6C, 0x13), (0xFF, 0xFF, 0xFF), (0x1B, 0x9C, 0x1B),
          (0x29, 0x1E, 0x00), (0xA7, 0x74, 0x00)}


# ---------------------------------------------------------------- PNG 写出
def write_png(path, w, h, rgba):
    raw = b''.join(b'\x00' + rgba[y * w * 4:(y + 1) * w * 4] for y in range(h))

    def chunk(t, d):
        return struct.pack('>I', len(d)) + t + d + struct.pack('>I', zlib.crc32(t + d) & 0xffffffff)

    out = b'\x89PNG\r\n\x1a\n'
    out += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    out += chunk(b'IDAT', zlib.compress(raw, 9))
    out += chunk(b'IEND', b'')
    open(path, 'wb').write(out)


def to_rgba(px):
    b = bytearray()
    for r, g, bl, a in px:
        b += bytes((r, g, bl, a))
    return bytes(b)


def load(path):
    img = load_png_any(path)
    return list(pixels(img)), img['w'], img['h']


# ---------------------------------------------------------------- 反推几何 + 映射
def load_head_legacy():
    os.makedirs(ICON_DIR, exist_ok=True)
    dst = os.path.join(ICON_DIR, 'head_legacy_xxx.png')
    r = subprocess.run(['git', 'show', 'HEAD:' + LEGACY_REL], cwd=ROOT,
                       stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    if r.returncode != 0:
        print('!! 无法从 git 取 HEAD 版 legacy 图：', r.stderr.decode('utf-8', 'replace'))
        sys.exit(1)
    open(dst, 'wb').write(r.stdout)
    return dst


def bbox(px, w, h, pred):
    xs, ys = [], []
    for y in range(h):
        for x in range(w):
            if pred(px[y * w + x]):
                xs.append(x)
                ys.append(y)
    return (min(xs), min(ys), max(xs), max(ys)) if xs else None


def build_transfer(old_legacy, new_legacy, fg_px):
    """返回 (映射函数, 统计)。映射函数：fg 像素坐标 → 新色。"""
    w, h = old_legacy[1], old_legacy[2]
    opx = old_legacy[0]
    npx = new_legacy[0]

    # 「变色目标色」集合 —— 只可能是被重上色的精灵区域，据此定位精灵 bbox（不受黑色背景干扰）
    changed = set()
    for a, b in zip(opx, npx):
        if a[:3] != b[:3]:
            changed.add(b[:3])

    lb = bbox(npx, w, h, lambda p: p[:3] in changed)
    fw, fh = fg_px[1], fg_px[2]
    fpx = fg_px[0]
    fb = bbox(fpx, fw, fh, lambda p: p[3] > 0)

    lw, lh = lb[2] - lb[0] + 1, lb[3] - lb[1] + 1
    pw, ph = fb[2] - fb[0] + 1, fb[3] - fb[1] + 1
    sx, sy = lw / float(pw), lh / float(ph)
    ox, oy = lb[0] - fb[0] * sx, lb[1] - fb[1] * sy

    print('=== 几何反推 ===')
    print('  legacy 精灵 bbox %s  宽高 %dx%d' % (tuple(lb), lw, lh))
    print('  fg     精灵 bbox %s  宽高 %dx%d' % (tuple(fb), pw, ph))
    print('  缩放 %.6f x %.6f  偏移 (%.3f, %.3f)' % (sx, sy, ox, oy))
    assert abs(sx - sy) < 1e-6, '两轴缩放不一致，几何关系不成立'

    # 纯色兜底映射（按像素数占优，处理落在精灵边缘、legacy 取样到背景的情况）
    byold = {}
    for a, b in zip(opx, npx):
        if a[:3] != b[:3]:
            byold.setdefault(a[:3], Counter())[b[:3]] += 1
    fallback = {a: c.most_common(1)[0][0] for a, c in byold.items()}

    stat = Counter()

    def transfer(x, y, orig_rgb):
        u = int(round(ox + x * sx))
        v = int(round(oy + y * sy))
        if 0 <= u < w and 0 <= v < h:
            src = opx[v * w + u][:3]
            if src == orig_rgb:
                stat['spatial'] += 1
                return npx[v * w + u][:3]
            if src in FG_PAL:
                stat['spatial'] += 1
                return npx[v * w + u][:3]
        stat['fallback'] += 1
        return fallback.get(orig_rgb, orig_rgb)

    return transfer, stat, lb, fb


# ---------------------------------------------------------------- 换色
def recolor(src, transfer, apply=False, label=None):
    px, w, h = load(src)
    out = []
    n = 0
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[y * w + x]
            if a > 0:
                nb = transfer(x, y, (r, g, b))
                if nb != (r, g, b):
                    n += 1
                    r, g, b = nb
            out.append((r, g, b, a))
    print('  %-58s %3dx%-3d 换色 %6d px' % (label or os.path.relpath(src, ROOT), w, h, n))
    if apply:
        write_png(src, w, h, to_rgba(out))
    return out, w, h


# ---------------------------------------------------------------- 预览
def compose(bg_px, fg_px, w, h, mask_circle=True):
    out = []
    cx = cy = (w - 1) / 2.0
    rad = w * 72.0 / 108.0 / 2.0
    for y in range(h):
        for x in range(w):
            i = y * w + x
            br, bg_, bb, ba = bg_px[i]
            fr, fg_, fb, fa = fg_px[i]
            fr = (fr * fa + br * (255 - fa)) // 255
            fg_ = (fg_ * fa + bg_ * (255 - fa)) // 255
            fb = (fb * fa + bb * (255 - fa)) // 255
            if mask_circle:
                d = ((x - cx) ** 2 + (y - cy) ** 2) ** 0.5
                v = 235 if ((x // 8 + y // 8) % 2) else 205
                if d > rad:
                    out.append((v, v, v, 255))
                    continue
                if d > rad - 1:
                    t = max(0.0, min(1.0, rad - d))
                    out.append((int(fr * t + v * (1 - t)), int(fg_ * t + v * (1 - t)), int(fb * t + v * (1 - t)), 255))
                    continue
            out.append((fr, fg_, fb, 255))
    return out


def scale_nearest(px, w, h, nw, nh):
    return [px[min(h - 1, y * h // nh) * w + min(w - 1, x * w // nw)] for y in range(nh) for x in range(nw)]


def side_by_side(panels, gap=12):
    H = max(p[2] for p in panels)
    W = sum(p[1] for p in panels) + gap * (len(panels) - 1)
    canvas = [(240, 240, 240, 255)] * (W * H)
    x0 = 0
    for px, w, h in panels:
        oy = (H - h) // 2
        for y in range(h):
            for x in range(w):
                canvas[(oy + y) * W + x0 + x] = px[y * w + x]
        x0 += w + gap
    return canvas, W, H


# ---------------------------------------------------------------- main
def main():
    print('工程：', ROOT)
    print()
    old_l = load(load_head_legacy())
    new_l = load(os.path.join(ROOT, LEGACY_REL))
    fg_path = os.path.join(ROOT, FG_REL)
    fg = load(fg_path)

    transfer, stat, lb, fb = build_transfer(old_l, new_l, fg)
    print()

    # 「改前」取自 HEAD 版（工作区可能已被本脚本改过），保证对照始终有意义
    head_fg = os.path.join(ICON_DIR, 'head_fg_xxx.png')
    r = subprocess.run(['git', 'show', 'HEAD:' + FG_REL], cwd=ROOT, stdout=subprocess.PIPE)
    if r.returncode == 0:
        open(head_fg, 'wb').write(r.stdout)
    before, w, h = recolor(head_fg if os.path.exists(head_fg) else fg_path,
                           lambda x, y, c: c, label='[改前] HEAD 版工程图标')
    after, _, _ = recolor(head_fg if os.path.exists(head_fg) else fg_path, transfer,
                          label='[改后] ic_launcher_foreground.png %s' % ('（已写入）' if APPLY else '（预览）'))
    print('  取样来源：按位置搬运 %d px，兜底纯色映射 %d px' % (stat['spatial'], stat['fallback']))

    print()
    print('=== 其余密度 ===')
    for v in VARIANTS:
        for d in DENSITIES:
            p = os.path.join(ROOT, 'android/src/%s/res/mipmap-%s/ic_launcher_foreground.png' % (v, d))
            # 低密度图按比例缩放同一偏移
            k = {'mdpi': 0.25, 'hdpi': 0.375, 'xhdpi': 0.5, 'xxhdpi': 0.75, 'xxxhdpi': 1.0}[d]
            t = transfer if k == 1.0 else (lambda x, y, c, t=transfer, k=k: t(int(x / k), int(y / k), c))
            recolor(p, t, apply=APPLY, label='%s/ic_launcher_foreground.png' % d if v == 'main' else '%s/debug %s' % (d, v))

    bg_px, _, _ = load(os.path.join(ROOT, BG_REL))
    p1 = os.path.join(ICON_DIR, 'preview_before_after.png')
    cv, CW, CH = side_by_side([(scale_nearest(compose(bg_px, before, w, h), w, h, w * 2, h * 2), w * 2, h * 2),
                               (scale_nearest(compose(bg_px, after, w, h), w, h, w * 2, h * 2), w * 2, h * 2)])
    write_png(p1, CW, CH, to_rgba(cv))
    print()
    print('导出 %s   左＝当前真机图标（原版金绿）  右＝换色后' % os.path.relpath(p1, ROOT))

    lc, _, _ = load(os.path.join(ROOT, LEGACY_REL))
    lw, lh = 192, 192
    p2 = os.path.join(ICON_DIR, 'preview_legacy_vs_adaptive.png')
    cv2, CW2, CH2 = side_by_side([(scale_nearest(lc, lw, lh, 288, 288), 288, 288),
                                  (scale_nearest(compose(bg_px, after, w, h), w, h, 288, 288), 288, 288)])
    write_png(p2, CW2, CH2, to_rgba(cv2))
    print('导出 %s   左＝你已画好的 legacy 扁平图  右＝修好的自适应图标' % os.path.relpath(p2, ROOT))

    if not APPLY:
        print()
        print('（未改写任何文件；确认无误后加 --apply 生效）')


if __name__ == '__main__':
    main()
