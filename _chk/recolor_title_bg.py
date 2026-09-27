#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把标题滚动背景的 4 张图改成深红色调（横幅 banners.png 不动）。

为什么这么做是安全的：这 4 张 PNG 都是 **8 位索引图（PLTE + tRNS）**，
所以「改色」= **只重写 PLTE 调色板**，IDAT（像素索引）**逐字节保留**：
  * 尺寸不可能变（TextureFilm 的帧网格因此绝对安全）
  * tRNS 逐字节保留 ⇒ 图层轮廓/透明度绝对安全
  * 没有量化误差：像素索引不变，只是索引指向的颜色变了

深红化用**双色调（duotone）**：先取亮度，再把亮度映射到「暗红 → 亮红」的色阶上。
这样能完整保留原图的明暗结构与剪影，只是把色相统一压到深红；
`--mix` 可以退化成「轻微染色」而不是完全双色调。

⚠️ 备份放在 **_chk/_bak_title_bg/**（**不能放 assets 里**，
否则 .orig.png 会被打进 APK 撑大包体——素材目录里已经混着 .aseprite 与中文名工作图的先例）。

⚠️ 这套背景是 **10 个界面共用**的（TitleScene/WelcomeScene/StartScene/RankingsScene/
JournalScene/NewsScene/ChangesScene/AboutScene/SupporterScene/SurfaceScene），
换色 = 这 10 个界面的背景一起变。

用法：
    python _chk/recolor_title_bg.py --check                 # 只报调色板统计，不写盘
    python _chk/recolor_title_bg.py --apply                 # 备份 + 改色 + 核验
    python _chk/recolor_title_bg.py --apply --dark 20,0,0 --light 255,70,50 --gamma 0.9
    python _chk/recolor_title_bg.py --apply --mix 0.6        # 只染 60%，保留部分原色
    python _chk/recolor_title_bg.py --preview                # 生成原图/新图对照预览 PNG
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
ASSET_DIR = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'splashes', 'title')
BACKUP_DIR = os.path.join(ROOT, '_chk', '_bak_title_bg')
PREVIEW = os.path.join(ROOT, '_chk', '_title_bg_preview.png')
MARKER = os.path.join(BACKUP_DIR, 'RECOLORED.txt')

TARGETS = ['archs.png', 'back_clusters.png', 'mid_mixed.png', 'front_small.png']

# 深红色阶：暗端接近黑红，亮端是明确的红（不是粉）
DEFAULT_DARK = (18, 2, 4)
DEFAULT_LIGHT = (232, 64, 46)


# ---------------------------------------------------------------- PNG 读写

def parse_png(raw):
    """返回 (width, height, bit_depth, color_type, chunks)；chunks = [(type, data)] 保序。"""
    if raw[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit('不是 PNG: 签名不符')
    w, h = struct.unpack('>II', raw[16:24])
    bit_depth, color_type = raw[24], raw[25]
    chunks = []
    i = 8
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        typ = raw[i + 4:i + 8]
        chunks.append((typ, raw[i + 8:i + 8 + ln]))
        i += 12 + ln
        if typ == b'IEND':
            break
    return w, h, bit_depth, color_type, chunks


def write_png(w, h, bit_depth, color_type, chunks):
    out = [b'\x89PNG\r\n\x1a\n']
    for typ, data in chunks:
        out.append(struct.pack('>I', len(data)))
        out.append(typ)
        out.append(data)
        out.append(struct.pack('>I', binascii.crc32(typ + data) & 0xffffffff))
    return b''.join(out)


def get(chunks, typ):
    for t, d in chunks:
        if t == typ:
            return d
    return None


def palette_of(chunks):
    plte = get(chunks, b'PLTE')
    if plte is None:
        raise SystemExit('缺 PLTE，不是索引图')
    return [tuple(plte[i:i + 3]) for i in range(0, len(plte), 3)]


def trns_of(chunks):
    t = get(chunks, b'tRNS')
    return None if t is None else bytes(t)


# ---------------------------------------------------------------- 双色调

def recolor_palette(palette, dark, light, gamma, mix):
    """亮度 → 深红色阶；mix=1 完全双色调，mix=0 原样。"""
    out = []
    for (r, g, b) in palette:
        lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
        t = lum ** gamma
        nr = dark[0] + (light[0] - dark[0]) * t
        ng = dark[1] + (light[1] - dark[1]) * t
        nb = dark[2] + (light[2] - dark[2]) * t
        out.append((
            int(round(r * (1 - mix) + nr * mix)),
            int(round(g * (1 - mix) + ng * mix)),
            int(round(b * (1 - mix) + nb * mix)),
        ))
    return [tuple(max(0, min(255, c)) for c in px) for px in out]


def palette_stats(palette, trns):
    """按 alpha 分组统计，确认透明条目没被算进「可见色」。"""
    opaque, faded, clear = [], 0, 0
    for i, px in enumerate(palette):
        a = 255 if trns is None or i >= len(trns) else trns[i]
        if a == 0:
            clear += 1
        elif a == 255:
            opaque.append(px)
        else:
            faded += 1
    def avg(lst):
        if not lst:
            return (0, 0, 0)
        n = len(lst)
        return tuple(round(sum(p[k] for p in lst) / n) for k in range(3))
    return {'opaque': len(opaque), 'faded': faded, 'clear': clear, 'avg_opaque': avg(opaque)}


# ---------------------------------------------------------------- 预览

def unfilter(raw, w, h, bpp_bytes):
    """标准 PNG 反滤波，返回按行的字节串列表。"""
    stride = w * bpp_bytes
    out, prev, pos = [], bytearray(stride), 0
    for _ in range(h):
        ft = raw[pos]; pos += 1
        line = bytearray(raw[pos:pos + stride]); pos += stride
        if ft == 1:
            for i in range(bpp_bytes, stride):
                line[i] = (line[i] + line[i - bpp_bytes]) & 0xff
        elif ft == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 0xff
        elif ft == 3:
            for i in range(stride):
                a = line[i - bpp_bytes] if i >= bpp_bytes else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 0xff
        elif ft == 4:
            for i in range(stride):
                a = line[i - bpp_bytes] if i >= bpp_bytes else 0
                c = prev[i - bpp_bytes] if i >= bpp_bytes else 0
                b = prev[i]
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 0xff
        out.append(bytes(line))
        prev = line
    return out


def rgb_png(w, h, rows):
    """把 w×h 的 RGB 行写成 PNG（color type 2）。"""
    raw = b''.join(b'\x00' + r for r in rows)
    return write_png(w, h, 8, 2, [
        (b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 2, 0, 0, 0)),
        (b'IDAT', zlib.compress(raw, 6)),
        (b'IEND', b''),
    ])


def load_rgba(path):
    """解出 (w,h,像素=[[(r,g,b,a)...]])，仅支持 8 位索引或 8 位 RGBA。"""
    raw = open(path, 'rb').read()
    w, h, bd, ct, chunks = parse_png(raw)
    data = zlib.decompress(b''.join(d for t, d in chunks if t == b'IDAT'))
    if ct == 3:
        pal = palette_of(chunks)
        trns = trns_of(chunks) or b''
        rows = unfilter(data, w, h, 1)
        px = []
        for line in rows:
            for idx in line:
                r, g, b = pal[idx]
                a = trns[idx] if idx < len(trns) else 255
                px.append((r, g, b, a))
        return w, h, px
    if ct == 6:
        rows = unfilter(data, w, h, 4)
        px = []
        for line in rows:
            for i in range(0, len(line), 4):
                px.append(tuple(line[i:i + 4]))
        return w, h, px
    raise SystemExit('预览不支持 color type %d' % ct)


def build_preview():
    """上排原图 / 下排新图，4 层各一格，等比缩放到 256×128 的格子里。

    合成方式：标准 alpha 合成到**固定中性深底** (20,16,18) 上。
    （早先版本用 `max(r,12)` 之类逐通道夹底，会给原图行也加一层红底，
      两排就不可比了——预览必须两排用完全相同的底，否则看的人会被误导。）
    """
    cw, ch = 256, 128
    BGR, BGG, BGB = 20, 16, 18
    W = cw * 4
    canvas = bytearray(W * ch * 2 * 3)
    for row, which in enumerate(('orig', 'new')):
        for col, name in enumerate(TARGETS):
            src = os.path.join(BACKUP_DIR, name) if which == 'orig' else os.path.join(ASSET_DIR, name)
            w, h, px = load_rgba(src)
            for y in range(ch):
                for x in range(cw):
                    sx = min(int(x * w / cw), w - 1)
                    sy = min(int(y * h / ch), h - 1)
                    r, g, b, a = px[sy * w + sx]
                    r = (r * a + BGR * (255 - a)) // 255
                    g = (g * a + BGG * (255 - a)) // 255
                    b = (b * a + BGB * (255 - a)) // 255
                    o = ((row * ch + y) * W + col * cw + x) * 3
                    canvas[o:o + 3] = bytes((r, g, b))
    rows = [bytes(canvas[i * W * 3:(i + 1) * W * 3]) for i in range(ch * 2)]
    open(PREVIEW, 'wb').write(rgb_png(W, ch * 2, rows))

    def row_avg(r0):
        """只统计**真实可见**的像素（alpha>0），按 alpha 加权。

        别用整幅均色：图层大部分是全透明的，中性底会把它稀释成「几乎没变」的假象。
        """
        tot = [0, 0, 0]
        wsum = 0
        for y in range(r0, r0 + ch):
            line = rows[y]
            for x in range(W):
                o = x * 3
                r, g, b = line[o], line[o + 1], line[o + 2]
                # 与中性底的「差异量」当权重：差异越大说明该像素越被图层覆盖
                d = abs(r - BGR) + abs(g - BGG) + abs(b - BGB)
                if d < 12:
                    continue
                tot[0] += r * d; tot[1] += g * d; tot[2] += b * d
                wsum += d
        if wsum == 0:
            return (0, 0, 0), 0
        return tuple(round(v / wsum) for v in tot), wsum

    oa, ow = row_avg(0)
    na, nw = row_avg(ch)
    return PREVIEW, oa, na, ow, nw


# ---------------------------------------------------------------- main

def parse_rgb(text):
    parts = [int(v) for v in text.split(',')]
    if len(parts) != 3:
        raise SystemExit('颜色需要 R,G,B 三个值')
    return tuple(parts)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--apply', action='store_true')
    ap.add_argument('--preview', action='store_true', help='生成原图/新图对照预览')
    ap.add_argument('--dark', default=','.join(map(str, DEFAULT_DARK)), help='暗端色 R,G,B')
    ap.add_argument('--light', default=','.join(map(str, DEFAULT_LIGHT)), help='亮端色 R,G,B')
    ap.add_argument('--gamma', type=float, default=1.0, help='亮度曲线（<1 提亮中间调）')
    ap.add_argument('--mix', type=float, default=1.0, help='0=原样 1=完全双色调')
    ap.add_argument('--force', action='store_true', help='备份已存在时也继续（危险）')
    args = ap.parse_args()

    if args.preview:
        path, orig_avg, new_avg, ow, nw = build_preview()
        print('预览已生成：%s' % os.path.relpath(path, ROOT))
        print('  上排＝改色前（4 层从左到右：archs / back_clusters / mid_mixed / front_small）')
        print('  下排＝改色后（同样的 4 层、同样的中性深底，两排可直接对比）')
        print('  可见像素均色（alpha 加权）：原图 %s → 新图 %s' % (orig_avg, new_avg))
        drift = tuple(new_avg[i] - orig_avg[i] for i in range(3))
        print('  通道变化：R %+d  G %+d  B %+d' % drift)
        ratio_g = new_avg[0] / max(1, new_avg[1])
        ratio_b = new_avg[0] / max(1, new_avg[2])
        print('  R/G = %.2f，R/B = %.2f  ⇒ %s'
              % (ratio_g, ratio_b,
                 '深红成立（红远高于绿蓝，且整体仍很暗）' if ratio_g > 2.5 and ratio_b > 2.5 and new_avg[0] < 110
                 else ('还是太亮/太淡，试 --gamma 0.8 或把 --light 调暗' if new_avg[0] >= 110
                       else '红味不足，把 --light 调更红、或 --mix 保持 1.0')))
        return 0
    if not args.check and not args.apply:
        ap.error('需要 --check / --apply / --preview 之一')

    dark, light = parse_rgb(args.dark), parse_rgb(args.light)
    print('双色调参数：暗端=%s 亮端=%s gamma=%.2f mix=%.2f' % (dark, light, args.gamma, args.mix))

    if os.path.exists(MARKER) and not args.force:
        raise SystemExit('检测到已改色标记 %s —— 拒绝重复执行（否则会把已改色的图当成原图备份）。\n'
                         '确要重做请先恢复备份，或加 --force。' % os.path.relpath(MARKER, ROOT))

    if args.apply and not os.path.isdir(BACKUP_DIR):
        os.makedirs(BACKUP_DIR)

    total = 0
    for name in TARGETS:
        path = os.path.join(ASSET_DIR, name)
        if not os.path.isfile(path):
            raise SystemExit('缺文件：%s' % path)
        raw = open(path, 'rb').read()
        w, h, bd, ct, chunks = parse_png(raw)
        if ct != 3 or bd != 8:
            raise SystemExit('%s 不是 8 位索引图（bd=%d ct=%d），本脚本不适用' % (name, bd, ct))
        pal = palette_of(chunks)
        trns = trns_of(chunks)
        new_pal = recolor_palette(pal, dark, light, args.gamma, args.mix)

        before = palette_stats(pal, trns)
        after = palette_stats(new_pal, trns)
        print('  %-20s %4dx%-5d 调色板 %3d 项  可见 %3d / 半透明 %3d / 全透明 %3d'
              % (name, w, h, len(pal), before['opaque'], before['faded'], before['clear']))
        print('      可见均色 %s → %s' % (before['avg_opaque'], after['avg_opaque']))

        if args.check:
            total += 1
            continue

        # 备份（只在首次）
        dst = os.path.join(BACKUP_DIR, name)
        if not os.path.exists(dst):
            shutil.copy2(path, dst)

        plte_data = b''.join(bytes(px) for px in new_pal)
        out_chunks = [(t, plte_data if t == b'PLTE' else d) for t, d in chunks]
        open(path, 'wb').write(write_png(w, h, bd, ct, out_chunks))
        total += 1

    if args.check:
        print('\n[check] 未写盘。%d 张将改色。' % total)
        return 0

    with open(MARKER, 'w', encoding='utf-8') as fh:
        fh.write('深红双色调：dark=%s light=%s gamma=%.2f mix=%.2f\n' % (dark, light, args.gamma, args.mix))
        fh.write('原图备份在本目录；恢复=把这里的 png 复制回 assets/splashes/title/\n')
    print('\n[apply] 已改色 %d 张；原图备份在 %s' % (total, os.path.relpath(BACKUP_DIR, ROOT)))
    print('下一步：python _chk/verify_title_bg.py   （尺寸/透明/结构核验）')
    print('预览：  python _chk/recolor_title_bg.py --preview')
    return 0


if __name__ == '__main__':
    sys.exit(main())
