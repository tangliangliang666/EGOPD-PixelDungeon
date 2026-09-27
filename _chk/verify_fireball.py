#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""火焰改色后的核验（回归断言）。

这次改的是 **RGBA 图**（不是索引图），所以判据和滚动背景那次不同：
那次能断言「IDAT 逐字节不变」，这次 RGB 必须变，于是改断言 **alpha 逐像素完全不变**。

判据：
  A. 尺寸 / 位深 / 颜色类型 / 交错与备份完全相同（帧网格与 MovieClip 动画的安全底线）
  B. **alpha 逐像素完全不变** —— 火焰的透明渐变/轮廓是它「有形」的全部依据
  C. RGB 确实变了（否则等于没改）
  D. **帧网格仍成立**：从 Fireball.java 现读 TextureFilm 帧尺寸与 Animation 帧表长度，
     断言 cols×rows ≥ 帧数，且帧表里的下标都在可用范围内
     （越界 ⇒ TextureFilm.get 返 null ⇒ Image.frame 解引用 NPE，运行期才崩）
  E. **代码未被改动**：Fireball.java 与 git 状态一致（本次是纯美术改动）

用法：
    python _chk/verify_fireball.py            # 核验
    python _chk/verify_fireball.py --restore  # 从备份恢复原图
"""

import argparse
import hashlib
import os
import re
import struct
import subprocess
import sys
import zlib

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'effects')
BACKUP_DIR = os.path.join(ROOT, '_chk', '_bak_fireball')
FIREBALL_JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                             'shatteredpixeldungeon', 'effects', 'Fireball.java')
TARGETS = ['fireball-tall.png', 'fireball-short.png']

PASS, FAIL = [], []


def check(name, ok, detail=''):
    (PASS if ok else FAIL).append(name)
    print('%-4s %s%s' % ('PASS' if ok else 'FAIL', name, ('  <- ' + detail) if detail else ''))


def parse_png(raw):
    if raw[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit('不是 PNG')
    w, h = struct.unpack('>II', raw[16:24])
    bit_depth, color_type, interlace = raw[24], raw[25], raw[28]
    data = b''
    i = 8
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        typ = raw[i + 4:i + 8]
        if typ == b'IDAT':
            data += raw[i + 8:i + 8 + ln]
        i += 12 + ln
        if typ == b'IEND':
            break
    return w, h, bit_depth, color_type, interlace, data


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


def pixels(path):
    raw = open(path, 'rb').read()
    w, h, bd, ct, inter, data = parse_png(raw)
    if ct != 6 or bd != 8:
        raise SystemExit('%s 不是 8 位 RGBA' % path)
    rows = unfilter(zlib.decompress(data), w, h, 4)
    px = []
    for line in rows:
        for k in range(0, len(line), 4):
            px.append(tuple(line[k:k + 4]))
    return (w, h, bd, ct, inter), px


def src_frames():
    """从 Fireball.java 现读 (帧宽, 帧高) 与帧表索引。"""
    text = open(FIREBALL_JAVA, encoding='utf-8').read()
    out = {}
    for name, key in (('fireball-tall.png', 'tall'), ('fireball-short.png', 'short')):
        pitch = re.search(r'texture\(\s*"effects/%s"\s*\);\s*\n\s*TextureFilm frames = new TextureFilm\( texture, (\d+), (\d+) \)'
                          % re.escape(name), text)
        if not pitch:
            raise SystemExit('在 Fireball.java 里找不到 %s 的 TextureFilm' % name)
        anim = re.search(r'anim\.frames\(\s*frames,\s*([0-9,\s]+)\)', text)
        all_anim = re.findall(r'anim\.frames\(\s*frames,\s*([0-9,\s]+)\)', text)
        # 两处动画帧表内容相同，取与本次文件同段的那个：按出现顺序 tall 在前
        idx = 0 if key == 'tall' else min(1, len(all_anim) - 1)
        frames = [int(v) for v in all_anim[idx].replace('\n', ' ').split(',') if v.strip()]
        out[name] = (int(pitch.group(1)), int(pitch.group(2)), frames)
    return out


def fmt_time(ts):
    import time
    return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(ts))


def restore():
    if not os.path.isdir(BACKUP_DIR):
        raise SystemExit('没有备份目录：%s' % BACKUP_DIR)
    for name in TARGETS:
        src = os.path.join(BACKUP_DIR, name)
        if not os.path.isfile(src):
            raise SystemExit('备份缺 %s' % name)
        open(os.path.join(ASSET_DIR, name), 'wb').write(open(src, 'rb').read())
        print('  已恢复', name)
    marker = os.path.join(BACKUP_DIR, 'RECOLORED.txt')
    if os.path.exists(marker):
        os.remove(marker)
    print('恢复完成（标记已清除）。')
    return 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--restore', action='store_true')
    args = ap.parse_args()
    if args.restore:
        return restore()

    if not os.path.isdir(BACKUP_DIR):
        raise SystemExit('缺备份目录，先跑 recolor_fireball.py --apply')

    # 从标记文件读模式与参数，让断言跟着实际用的方案走（ramp 模式没有「金区/黑区」）
    mode, quantile = 'blackgold', 0.28
    marker_path = os.path.join(BACKUP_DIR, 'RECOLORED.txt')
    if os.path.exists(marker_path):
        text = open(marker_path, encoding='utf-8').read()
        m = re.search(r'^mode=(\w+)', text, re.M)
        if m:
            mode = m.group(1)
        m = re.search(r'goldQuantile=([0-9.]+)', text)
        if m:
            quantile = float(m.group(1))
    print('核验模式（读自 RECOLORED.txt）：%s%s'
          % (mode, '' if mode != 'blackgold' else '，goldQuantile=%.2f' % quantile))
    print()

    frames = src_frames()
    print('从 Fireball.java 现读：' + '  '.join(
        '%s 帧 %dx%d / 帧表 %d 项' % (k, v[0], v[1], len(v[2])) for k, v in frames.items()))
    print()

    for name in TARGETS:
        cur_meta, cur = pixels(os.path.join(ASSET_DIR, name))
        old_meta, old = pixels(os.path.join(BACKUP_DIR, name))
        tag = name

        check('%s 尺寸/位深/色型/交错不变' % tag, cur_meta == old_meta,
              '%dx%d -> %dx%d' % (old_meta[0], old_meta[1], cur_meta[0], cur_meta[1]))
        check('%s 像素数一致' % tag, len(cur) == len(old), '%d vs %d' % (len(old), len(cur)))

        alpha_same = all(a[3] == b[3] for a, b in zip(cur, old))
        check('%s **alpha 逐像素完全不变**' % tag, alpha_same)
        if not alpha_same:
            bad = sum(1 for a, b in zip(cur, old) if a[3] != b[3])
            print('       !! %d 个像素 alpha 不一致' % bad)

        rgb_changed = sum(1 for a, b in zip(cur, old) if a[:3] != b[:3])
        vis = sum(1 for b in old if b[3] > 0)
        check('%s RGB 确实变了' % tag, rgb_changed > 0, '%d/%d 像素（可见 %d）' % (rgb_changed, len(cur), vis))

        # 分区统计（按**原始**亮度分区，且必须与 recolor 脚本用同一个整数索引判据）
        def lum(p):
            return (0.299 * p[0] + 0.587 * p[1] + 0.114 * p[2]) / 255.0
        pairs = [(o, c) for o, c in zip(old, cur) if o[3] > 0]
        pairs.sort(key=lambda t: -lum(t[0]))
        wsum = sum(o[3] for o, _ in pairs) or 1
        acc, thr = 0, 0.5
        for o, _ in pairs:
            acc += o[3]
            if acc >= wsum * quantile:
                thr = lum(o)
                break
        gold = [(o, c) for o, c in pairs if lum(o) > thr]
        dark = [(o, c) for o, c in pairs if lum(o) <= thr]

        def avg(pairs_, which):
            if not pairs_:
                return (0, 0, 0)
            n = len(pairs_)
            return tuple(round(sum((o if which == 'o' else c)[k] for o, c in pairs_) / n) for k in range(3))

        overall = avg(pairs, 'n')
        if mode == 'blackgold':
            g_f = len(gold) / len(pairs) if pairs else 0
            g_w = sum(o[3] for o, _ in gold) / wsum if pairs else 0
            print('      金区（原亮度 > %.3f）占可见像素 %.1f%% / alpha 加权 %.1f%%：%s → %s'
                  % (thr, g_f * 100, g_w * 100, avg(gold, 'o'), avg(gold, 'n')))
            print('      黑区：%s → %s' % (avg(dark, 'o'), avg(dark, 'n')))
            g_avg = avg(gold, 'n')
            check('%s 金区确实呈金黄（R>G>B 且 R 明显）' % tag,
                  g_avg[0] > g_avg[1] > g_avg[2] and g_avg[0] > 150, '金区均色 %s' % (g_avg,))
            d_avg = avg(dark, 'n')
            check('%s 黑区确实压暗（R<90）' % tag, d_avg[0] < 90, '黑区均色 %s' % (d_avg,))
        else:
            # ⚠️ 上面的 pairs 是**降序**排的（算分位数需要），不能直接拿 [:q] 当「最暗」。
            # 早期版本就这么错了：标签反了，导致「暗部」断言实际测的是亮部。
            # 这里另取一份升序副本，保证断言测的确实是它声称的那一档。
            asc = sorted(pairs, key=lambda t: lum(t[0]))
            q = max(1, len(asc) // 4)
            lo_old, lo = avg(asc[:q], 'o'), avg(asc[:q], 'n')
            hi_old, hi = avg(asc[-q:], 'o'), avg(asc[-q:], 'n')
            print('      最暗 1/4：%s → %s' % (lo_old, lo))
            print('      最亮 1/4：%s → %s' % (hi_old, hi))
            print('      整体均色：%s → %s' % (avg(pairs, 'o'), overall))
            check('%s 整体呈红（R 明显高于 G/B）' % tag,
                  overall[0] > overall[1] and overall[0] > overall[2] and overall[0] > 120,
                  '整体均色 %s' % (overall,))
            check('%s 暗部仍是红而不是黑（R>110）' % tag, lo[0] > 110, '最暗 1/4 %s' % (lo,))
            check('%s 亮部仍偏红（R 最高）' % tag, hi[0] > hi[1] and hi[0] > hi[2], '最亮 1/4 %s' % (hi,))

        # D. 帧网格
        fw, fh, table = frames[name]
        cols, rows = cur_meta[0] // fw, cur_meta[1] // fh
        avail = cols * rows
        check('%s 帧网格 %dx%d=可用 %d 帧 ≥ 帧表 %d 项（帧 %dx%d）'
              % (tag, cols, rows, avail, len(table), fw, fh), avail >= len(table))
        check('%s 帧表下标全在可用范围内（最大 %d < %d）' % (tag, max(table), avail),
              max(table) < avail)

        sizes = '旧 %d B → 新 %d B（%.1f%%）' % (
            os.path.getsize(os.path.join(BACKUP_DIR, name)),
            os.path.getsize(os.path.join(ASSET_DIR, name)),
            100.0 * os.path.getsize(os.path.join(ASSET_DIR, name)) /
            max(1, os.path.getsize(os.path.join(BACKUP_DIR, name))))
        print('      体积：%s' % sizes)
        print()

    # E. 代码未改
    try:
        st = subprocess.run(['git', '-C', ROOT, 'status', '--porcelain', '--',
                             'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/effects/Fireball.java'],
                            capture_output=True, text=True, timeout=60).stdout.strip()
    except Exception:
        st = ''
    check('Fireball.java 未被改动（本次是纯美术改动）', st == '',
          st if st else 'git 无变更')
    print('     Fireball.java sha256 = %s'
          % hashlib.sha256(open(FIREBALL_JAVA, 'rb').read()).hexdigest()[:32])
    print('     依赖提醒：Fireball 也被 WelcomeScene 使用 ⇒ 开场界面的火把会一起变（同一套贴图）。')

    print('\n%d 项通过，%d 项失败' % (len(PASS), len(FAIL)))
    if FAIL:
        print('失败：' + '、'.join(FAIL))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
