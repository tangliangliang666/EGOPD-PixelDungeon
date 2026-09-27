#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把标题火把的火焰改成「黑色为主 + 掺杂金黄色」。

对象：`core/src/main/assets/effects/fireball-tall.png`（横屏，61×61 帧 ×24）
      `core/src/main/assets/effects/fireball-short.png`（竖屏，47×47 帧 ×24）
上游是**绿焰**（亮部 (225,255,186)、核心 (0,234,69)），本次改色后偏黑金。

## 为什么是逐像素变换而不是改调色板
这两张是 **RGBA（color type 6）**，没有 PLTE ⇒ 只能变换 RGB 并**保持 alpha 逐字节不变**。
无损性依然成立：尺寸/位深/色型/交错不变、alpha 完全不动、没有缩放重采样。

## 映射：三分段色阶（黑 → 余烬 → 金）
按**亮度**映射，分界点由「要让多少比例的像素变金」反推——即 `--gold-quantile`。
这一点很关键：两张图的亮度分布**差很多**（tall 只有 3.5% 像素在 0.90 以上，
short 有 9.2%，且 51.8% 落在 0.75-0.90），用固定阈值会让 short 几乎全变金，
达不到「掺杂**部分**金黄」。按分位数取阈值则两张都稳定。

    L ≤ thr : 黑 → 余烬   （大片低 alpha 的外焰因此变成「暗影」，而不是原来的绿光晕）
    L >  thr : 余烬 → 金   （最亮的焰心/底部变金黄，占比 = --gold-quantile）

## 一个必须知道的观感前提
原图**大部分像素是低 alpha**（tall 的平均 alpha 在中等亮度带只有 23~46，只有 3.5% 达 255）。
所以「黑色」在深色标题背景上主要表现成**把背景压暗的阴影**，而不是一块纯黑。
这正是黑焰该有的样子（烟熏感），但也意味着**金黄色的那部分才是真正「看得见」的形**。

用法：
    python _chk/recolor_fireball.py --check
    python _chk/recolor_fireball.py --apply
    python _chk/recolor_fireball.py --apply --gold-quantile 0.30     # 更多金黄
    python _chk/recolor_fireball.py --apply --black 30,26,30          # 黑里带一点灰，更「有形」
    python _chk/recolor_fireball.py --apply --gold 255,214,80         # 更亮的金
    python _chk/recolor_fireball.py --preview                         # 原图/新图逐帧对照
"""

import argparse
import binascii
import os
import shutil
import struct
import sys
import zlib

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'effects')
BACKUP_DIR = os.path.join(ROOT, '_chk', '_bak_fireball')
MARKER = os.path.join(BACKUP_DIR, 'RECOLORED.txt')
PREVIEW = os.path.join(ROOT, '_chk', '_fireball_preview.png')
FIREBALL_JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                             'shatteredpixeldungeon', 'effects', 'Fireball.java')
TARGETS = ['fireball-tall.png', 'fireball-short.png']

DEFAULT_BLACK = (4, 3, 6)
# 暗区**压成深黑**：原本这里是从黑渐变到「余烬色 (70,30,8)」，而中低亮度像素占可见像素约 92%
# （虽然 alpha 低），整体因此被带出一层棕味 ⇒ 观感是「黄棕色火焰」而不是黑焰。
# 现在暗区两端都贴近深黑（几乎平色），棕味消失，金色区完全不动 ⇒ 才是「黑底 + 金黄」两色。
DEFAULT_EMBER = (9, 7, 10)
DEFAULT_GOLD_DIM = (206, 146, 30)
DEFAULT_GOLD = (255, 224, 116)

# ramp 模式（整条亮度色阶）默认给「整体亮红色」：
# 暗端是暗红而不是纯黑（纯黑会让外焰变成不透明的黑影、整体读不出红色），
# 亮端给到饱和亮红，让焰心明确发红。
DEFAULT_RED_DARK = (96, 6, 8)
DEFAULT_RED_BRIGHT = (255, 74, 52)


# ------------------------------------------------------------------ PNG

def parse_png(raw):
    if raw[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit('不是 PNG')
    w, h = struct.unpack('>II', raw[16:24])
    bit_depth, color_type, interlace = raw[24], raw[25], raw[28]
    data = b''
    chunks = []
    i = 8
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        typ = raw[i + 4:i + 8]
        chunks.append((typ, raw[i + 8:i + 8 + ln]))
        if typ == b'IDAT':
            data += raw[i + 8:i + 8 + ln]
        i += 12 + ln
        if typ == b'IEND':
            break
    return w, h, bit_depth, color_type, interlace, chunks, data


def unfilter(raw, w, h, bpp):
    stride = w * bpp
    out, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        ft = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + stride]); pos += stride
        if ft == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif ft == 3:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif ft == 4:
            for i in range(stride):
                a = line[i - bpp] if i >= bpp else 0
                c = prev[i - bpp] if i >= bpp else 0
                b = prev[i]
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out.append(bytes(line))
        prev = line
    return out


def load_rgba(path):
    w, h, bd, ct, inter, chunks, data = parse_png(open(path, 'rb').read())
    if ct != 6 or bd != 8:
        raise SystemExit('%s 不是 8 位 RGBA（bd=%d ct=%d），本脚本不适用' % (path, bd, ct))
    rows = unfilter(zlib.decompress(data), w, h, 4)
    px = []
    for line in rows:
        for k in range(0, len(line), 4):
            px.append((line[k], line[k + 1], line[k + 2], line[k + 3]))
    return w, h, px


def write_rgba(path, w, h, px):
    """px 是**逐像素的 (r,g,b,a) 元组列表**（长度 w*h），不是扁平字节串。

    早先版本按字节下标去切 `px[...]`，写成 `bytes(切片出的元组列表)` ⇒ TypeError。
    那次崩在写盘之前，素材未被破坏（已核对 sha256），但仍要按逐像素语义写。
    """
    raw = bytearray()
    for y in range(h):
        raw.append(0)                      # filter type 0（None）
        for x in range(w):
            raw += bytes(px[y * w + x])
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0)
    idat = zlib.compress(bytes(raw), 9)

    def chunk(typ, payload):
        return (struct.pack('>I', len(payload)) + typ + payload +
                struct.pack('>I', binascii.crc32(typ + payload) & 0xffffffff))
    out = (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) + chunk(b'IDAT', idat) + chunk(b'IEND', b''))
    open(path, 'wb').write(out)


def rgb_png(w, h, rows):
    raw = b''.join(b'\x00' + r for r in rows)
    ihdr = struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)

    def chunk(typ, payload):
        return (struct.pack('>I', len(payload)) + typ + payload +
                struct.pack('>I', binascii.crc32(typ + payload) & 0xffffffff))
    return (b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', ihdr) +
            chunk(b'IDAT', zlib.compress(raw, 6)) + chunk(b'IEND', b''))


# ------------------------------------------------------------------ 映射

def luminance(px):
    return (0.299 * px[0] + 0.587 * px[1] + 0.114 * px[2]) / 255.0


def lum_index(px):
    """亮度 → 0..255 整数索引。**全脚本只认这一个量**。

    为什么必须统一：`apply_lut` 查表用的是截断后的整数索引，
    而「金区/黑区」的判定若用浮点亮度，阈值附近会有几百个像素两边不一致
    （li/255 ≤ lum，所以浮点值刚过阈值的像素，整数索引可能还在阈值这一侧）。
    本脚本早期版本就因此出现过「改了暗色参数，金色区却被动了几百个像素」的假象。
    统一走整数索引后：金色区**只由 gold_dim/gold 决定**，与暗色参数严格无关。
    """
    li = int(luminance(px) * 255)
    return 0 if li < 0 else (255 if li > 255 else li)


def gold_threshold(px, quantile):
    """按 alpha 加权求「最亮的那 quantile 比例」的亮度分界，返回**整数索引**。"""
    vis = sorted(((lum_index(p), p[3]) for p in px if p[3] > 0), key=lambda t: -t[0])
    if not vis:
        return 128
    total = sum(a for _, a in vis)
    want = total * quantile
    acc = 0
    for li, a in vis:
        acc += a
        if acc >= want:
            return li
    return vis[-1][0]


def build_lut(ti, black, ember, gold_dim, gold_bright):
    """两段**各自独立**的色阶，分界按**整数亮度索引**。

    默认参数下暗区几乎平色（黑≈余烬），所以实际效果是**两色**：
    暗处一律深黑，金色区从暗金到亮金。这样才不会出现「黄棕色」。

    早期版本用「余烬 → 金」一条连续色阶跨过阈值，结果绝大多数「金区」像素
    落在阈值刚过一侧、插值到余烬附近，均色只有 (141,90,20)——是暗琥珀不是金黄。
    现在金区**从真正的金起步**（阈值处直接等于 gold_dim），才读得出金色。
    代价是阈值处有一步跳变，但它正好落在焰心亮部的边界上，像素画风格里反而更利落。

        i ≤ ti : 黑 → 余烬        （大片低 alpha 的外焰 → 深黑/烟熏）
        i >  ti : 暗金 → 亮金      （最亮的焰心 → 金黄，占比 = --gold-quantile）
    """
    lut = []
    for i in range(256):
        if i <= ti:
            u = i / ti if ti > 0 else 0.0
            a, b = black, ember
        else:
            u = (i - ti) / max(1, 255 - ti)
            a, b = gold_dim, gold_bright
        lut.append(tuple(int(round(a[k] + (b[k] - a[k]) * u)) for k in range(3)))
    return lut


def build_lut_ramp(dark, bright, gamma):
    """**整条亮度色阶**：暗端 → 亮端，单一色相贯穿全图（「整体某色」用这个模式）。

    与 blackgold 的分段模式相对：那个是「大部分压黑 + 少数变金」的两色方案，
    这个是「全图都是同一色相、只有明暗变化」的方案。
    """
    return [tuple(int(round(dark[k] + (bright[k] - dark[k]) * ((i / 255.0) ** gamma)))
                  for k in range(3)) for i in range(256)]


def ramp_report(px, newpx):
    """ramp 模式的分区报告：按亮度四分之一处切最暗/最亮两组。"""
    pairs = [(p, np) for p, np in zip(px, newpx) if p[3] > 0]
    if not pairs:
        return {'dark_old': (0, 0, 0), 'dark_new': (0, 0, 0),
                'bright_old': (0, 0, 0), 'bright_new': (0, 0, 0), 'bright_opaque': 0}
    pairs.sort(key=lambda t: lum_index(t[0]))
    q = max(1, len(pairs) // 4)
    dark, bright = pairs[:q], pairs[-q:]

    def avg(sel, which):
        if not sel:
            return (0, 0, 0)
        n = len(sel)
        return tuple(round(sum((p[0] if which == 'o' else p[1])[k] for p in sel) / n) for k in range(3))

    return {
        'dark_old': avg(dark, 'o'), 'dark_new': avg(dark, 'n'),
        'bright_old': avg(bright, 'o'), 'bright_new': avg(bright, 'n'),
        'bright_opaque': sum(1 for p, _ in bright if p[3] == 255),
    }


def apply_lut(px, lut):
    return [(lut[lum_index(p)][0], lut[lum_index(p)][1], lut[lum_index(p)][2], p[3]) for p in px]


def stats(px):
    vis = [p for p in px if p[3] > 0]
    if not vis:
        return {'n': 0}
    def avg(sel):
        if not sel:
            return (0, 0, 0)
        n = len(sel)
        return tuple(round(sum(q[k] for q in sel) / n) for k in range(3))
    return {
        'n': len(vis),
        'frac': len(vis) / len(px),
        'avg': avg(vis),
        'avg_alpha': round(sum(p[3] for p in vis) / len(vis)),
        'opaque': sum(1 for p in vis if p[3] == 255),
    }


def zone_report(px, newpx, ti):
    """按**整数亮度索引**是否超过 ti 分区统计 —— 必须与 `apply_lut` 的分支判据一致，
    否则阈值附近会有几百个像素被算错区（本脚本早期版本踩过）。"""
    vis = [(p, np) for p, np in zip(px, newpx) if p[3] > 0]
    gold = [(p, np) for p, np in vis if lum_index(p) > ti]
    dark = [(p, np) for p, np in vis if lum_index(p) <= ti]
    wsum = sum(p[3] for p, _ in vis) or 1

    def avg(pairs, which):
        if not pairs:
            return (0, 0, 0)
        n = len(pairs)
        return tuple(round(sum((pr[0] if which == 'o' else pr[1])[k] for pr in pairs) / n) for k in range(3))

    return {
        'ti': ti,
        'thr_L': ti / 255.0,
        'gold_count_frac': len(gold) / len(vis) if vis else 0,
        'gold_weight_frac': sum(p[3] for p, _ in gold) / wsum,
        'gold_old': avg(gold, 'o'),
        'gold_new': avg(gold, 'n'),
        'dark_old': avg(dark, 'o'),
        'dark_new': avg(dark, 'n'),
        'gold_opaque': sum(1 for p, _ in gold if p[3] == 255),
    }


def goldish_frac(px):
    """「看起来是金黄」的像素占可见像素的比例 —— 直接在**结果图**上量（R>G>B 且够亮）。"""
    vis = [p for p in px if p[3] > 0]
    if not vis:
        return 0.0
    g = sum(1 for p in vis if p[0] > p[1] > p[2] and p[0] >= 140)
    return g / len(vis)


# ------------------------------------------------------------------ 预览

def build_preview():
    """两条（上=原图 / 下=新图）× 两张贴图，各取 4 帧，等比缩放到 61×61 的格子。"""
    cell = 61
    picks = [0, 6, 12, 18]
    BG = (26, 22, 24)
    W = cell * 4 * 2
    H = cell * 2
    canvas = bytearray(W * H * 3)
    info = []
    for row, (label, base) in enumerate((('改色前', BACKUP_DIR), ('改色后', ASSET_DIR))):
        for file_idx, name in enumerate(TARGETS):
            w, h, px = load_rgba(os.path.join(base, name))
            pitch = 61 if name.endswith('tall.png') else 47
            cols = w // pitch
            for fi, frame in enumerate(picks):
                cx = (file_idx * 4 + fi) * cell
                cy = row * cell
                fr, fc = frame // cols, frame % cols
                for y in range(cell):
                    for x in range(cell):
                        sx = fc * pitch + min(x, pitch - 1)
                        sy = fr * pitch + min(y, pitch - 1)
                        r, g, b, a = px[sy * w + sx]
                        r = (r * a + BG[0] * (255 - a)) // 255
                        g = (g * a + BG[1] * (255 - a)) // 255
                        b = (b * a + BG[2] * (255 - a)) // 255
                        o = ((cy + y) * W + cx + x) * 3
                        canvas[o:o + 3] = bytes((r, g, b))
            info.append((label, name, stats(px), goldish_frac(px)))
    rows = [bytes(canvas[i * W * 3:(i + 1) * W * 3]) for i in range(H)]
    open(PREVIEW, 'wb').write(rgb_png(W, H, rows))
    return info


# ------------------------------------------------------------------

def parse_rgb(t):
    v = [int(x) for x in t.split(',')]
    if len(v) != 3:
        raise SystemExit('颜色需要 R,G,B')
    return tuple(v)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--preview', action='store_true')
    ap.add_argument('--black', default=','.join(map(str, DEFAULT_BLACK)))
    ap.add_argument('--ember', default=','.join(map(str, DEFAULT_EMBER)))
    ap.add_argument('--gold-dim', default=','.join(map(str, DEFAULT_GOLD_DIM)), help='金区暗端 R,G,B')
    ap.add_argument('--gold', default=','.join(map(str, DEFAULT_GOLD)), help='金区亮端 R,G,B')
    ap.add_argument('--gold-quantile', type=float, default=0.28,
                    help='要变成金黄的最亮像素比例（alpha 加权，默认 0.28）')
    ap.add_argument('--mode', choices=('blackgold', 'ramp'), default='blackgold',
                    help='blackgold=阈值分段（黑底+金黄）；ramp=整条亮度色阶（整体单一色相）')
    ap.add_argument('--dark', default=','.join(map(str, DEFAULT_RED_DARK)), help='ramp 模式暗端 R,G,B')
    ap.add_argument('--bright', default=','.join(map(str, DEFAULT_RED_BRIGHT)), help='ramp 模式亮端 R,G,B')
    ap.add_argument('--gamma', type=float, default=1.0, help='ramp 模式的亮度曲线')
    ap.add_argument('--force', action='store_true')
    args = ap.parse_args()

    if args.preview:
        info = build_preview()
        print('预览已生成：%s' % os.path.relpath(PREVIEW, ROOT))
        print('  两条：上＝改色前，下＝改色后；每条左 4 格=横屏 61×61 的第 0/6/12/18 帧，')
        print('  右 4 格=竖屏 47×47 的同 4 帧。底色是中性深灰 —— 火焰大量像素是半透明的，')
        print('  纯黑底上「黑/暗」的部分会看不见，用灰底才能把形状显出来。')
        print()
        for label, name, st, gf in info:
            print('  %-6s %-22s 可见占比 %4.1f%%  均色 %-14s 均alpha %2d  不透明 %5d  暖色主导 %.1f%%'
                  % (label, name, st['frac'] * 100, str(st['avg']), st['avg_alpha'],
                     st['opaque'], gf * 100))
        print()
        print('  说明：「暖色主导」= 结果图里 R>G 且 R>B 且 R≥140 的像素占可见像素之比。')
        print('        ramp 红色方案下它接近 100%（全图同色相）；blackgold 方案下只有几个百分点。')
        return 0
    if not args.check and not args.apply:
        ap.error('需要 --check / --apply / --preview 之一')

    black, ember = parse_rgb(args.black), parse_rgb(args.ember)
    gold_dim, gold = parse_rgb(args.gold_dim), parse_rgb(args.gold)
    dark, bright = parse_rgb(args.dark), parse_rgb(args.bright)
    if not 0 < args.gold_quantile <= 1:
        raise SystemExit('--gold-quantile 要在 (0,1] 区间')
    if args.mode == 'ramp' and (dark == bright):
        raise SystemExit('ramp 模式的两端色不能相同')

    if os.path.exists(MARKER) and not args.force:
        raise SystemExit('检测到已改色标记 %s —— 拒绝重复执行（否则会把已改色的图当成原图备份）。\n'
                         '要重做请先 python _chk/verify_fireball.py --restore，或加 --force。'
                         % os.path.relpath(MARKER, ROOT))
    if args.apply and not os.path.isdir(BACKUP_DIR):
        os.makedirs(BACKUP_DIR)

    if args.mode == 'blackgold':
        print('模式 blackgold（阈值分段）：黑 %s → 余烬 %s ｜ 暗金 %s → 亮金 %s；金黄占比目标 %.0f%%'
              % (black, ember, gold_dim, gold, args.gold_quantile * 100))
    else:
        print('模式 ramp（整条亮度色阶）：暗端 %s → 亮端 %s；gamma=%.2f ⇒ 整体呈单一色相'
              % (dark, bright, args.gamma))
    print()

    for name in TARGETS:
        path = os.path.join(ASSET_DIR, name)
        if not os.path.isfile(path):
            raise SystemExit('缺文件 %s' % path)
        bak = os.path.join(BACKUP_DIR, name)
        # ⚠️ 参数迭代的正确做法：**永远从原始备份变换**。
        # LUT 是按亮度查表的，如果在「已改色的图」上再变换一次，用的是改色后的亮度，
        # 结果不可预期（会把金色区也再压一遍）。早期版本只靠 RECOLORED.txt 拦重复执行，
        # 加 --force 就会二次变换 —— 现在改成从备份取源，改参数直接重跑即可。
        src_path = bak if os.path.exists(bak) else path
        w, h, px = load_rgba(src_path)
        before = stats(px)

        if args.mode == 'blackgold':
            ti = gold_threshold(px, args.gold_quantile)
            lut = build_lut(ti, black, ember, gold_dim, gold)
        else:
            ti = None
            lut = build_lut_ramp(dark, bright, args.gamma)

        newpx = apply_lut(px, lut)
        after = stats(newpx)

        print('  %-22s %4dx%-4d' % (name, w, h))
        if ti is not None:
            zr = zone_report(px, newpx, ti)
            print('      分界：亮度索引 > %d（≈ L > %.3f）；金区占可见像素 %.1f%%、按 alpha 加权 %.1f%%'
                  % (zr['ti'], zr['thr_L'], zr['gold_count_frac'] * 100, zr['gold_weight_frac'] * 100))
            print('      金区 %s → %s（其中完全不透明 %d 个）'
                  % (zr['gold_old'], zr['gold_new'], zr['gold_opaque']))
            print('      黑区 %s → %s' % (zr['dark_old'], zr['dark_new']))
        else:
            rr = ramp_report(px, newpx)
            print('      最暗 1/4 %s → %s' % (rr['dark_old'], rr['dark_new']))
            print('      最亮 1/4 %s → %s（完全不透明 %d 个）'
                  % (rr['bright_old'], rr['bright_new'], rr['bright_opaque']))
        print('      整体均色 %s → %s   均alpha %d → %d（必须完全不变）'
              % (before['avg'], after['avg'], before['avg_alpha'], after['avg_alpha']))
        print('      alpha 逐像素未变：%s' % ('OK' if all(a[3] == b[3] for a, b in zip(px, newpx)) else '**不一致！**'))

        if args.check:
            continue
        dst = os.path.join(BACKUP_DIR, name)
        if not os.path.exists(dst):
            shutil.copy2(path, dst)
        write_rgba(path, w, h, newpx)

    if args.check:
        print('\n[check] 未写盘。')
        return 0

    with open(MARKER, 'w', encoding='utf-8') as fh:
        fh.write('mode=%s\n' % args.mode)
        if args.mode == 'blackgold':
            fh.write('blackgold: black=%s ember=%s goldDim=%s gold=%s goldQuantile=%.2f\n'
                     % (black, ember, gold_dim, gold, args.gold_quantile))
        else:
            fh.write('ramp: dark=%s bright=%s gamma=%.2f\n' % (dark, bright, args.gamma))
        fh.write('原图备份在本目录；恢复=python _chk/verify_fireball.py --restore\n')
        fh.write('注意：Fireball 类也被 WelcomeScene 使用，两个界面的火焰会一起变。\n')
    print('\n[apply] 已改色 %d 张；备份在 %s' % (len(TARGETS), os.path.relpath(BACKUP_DIR, ROOT)))
    print('下一步：python _chk/verify_fireball.py')
    print('预览：  python _chk/recolor_fireball.py --preview')
    return 0


if __name__ == '__main__':
    sys.exit(main())
