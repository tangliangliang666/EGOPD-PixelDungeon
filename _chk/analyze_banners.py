#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分析用户手工改过的 banners.png：核对新帧范围、找可用的空白区。

做三件事：
  1. 精确测出「移动端标题帧 (0,0)-(169,94)」「PC端标题帧 (176,0)-(427,65)」内的
     画面包围盒，并检查**帧外紧邻处**是否还有非透明像素（有 ⇒ 说明帧切到了画面）
  2. 检查 BOSS_SLAIN (0,157)-(127,225) / GAME_OVER (128,157)-(256,192) 是否原样还在
  3. 用「直方图最大矩形」算法找出图内**最大的完全透明矩形** —— 供 TITLE_GLOW_*
     指向空白用（要求同尺寸空白区未必存在，所以要实算）

用法：python _chk/analyze_banners.py
"""

import os
import struct
import sys
import zlib

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BANNERS = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'interfaces', 'banners.png')

FRAMES = {
    '移动端标题（新）': (0, 0, 169, 94),
    'PC端标题（新）': (176, 0, 427, 65),
    'BOSS_SLAIN（应不变）': (0, 157, 127, 225),
    'GAME_OVER（应不变）': (128, 157, 256, 192),
}
OLD = {
    '旧 TITLE_PORT': (0, 0, 139, 100),
    '旧 TITLE_GLOW_PORT': (139, 0, 278, 100),
    '旧 TITLE_LAND': (0, 100, 240, 157),
    '旧 TITLE_GLOW_LAND': (240, 100, 480, 157),
}


def load():
    raw = open(BANNERS, 'rb').read()
    w, h = struct.unpack('>II', raw[16:24])
    i, idat = 8, b''
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        t = raw[i + 4:i + 8]
        if t == b'IDAT':
            idat += raw[i + 8:i + 8 + ln]
        i += 12 + ln
        if t == b'IEND':
            break
    data = zlib.decompress(idat)
    stride = w * 4
    alpha = []
    prev, pos = bytearray(stride), 0
    for _ in range(h):
        ft = data[pos]; pos += 1
        line = bytearray(data[pos:pos + stride]); pos += stride
        if ft == 1:
            for k in range(4, stride):
                line[k] = (line[k] + line[k - 4]) & 255
        elif ft == 2:
            for k in range(stride):
                line[k] = (line[k] + prev[k]) & 255
        elif ft == 3:
            for k in range(stride):
                a = line[k - 4] if k >= 4 else 0
                line[k] = (line[k] + ((a + prev[k]) >> 1)) & 255
        elif ft == 4:
            for k in range(stride):
                a = line[k - 4] if k >= 4 else 0
                c = prev[k - 4] if k >= 4 else 0
                b = prev[k]
                pp = a + b - c
                pa, pb, pc = abs(pp - a), abs(pp - b), abs(pp - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[k] = (line[k] + pr) & 255
        alpha.append([line[x * 4 + 3] for x in range(w)])
        prev = line
    return w, h, alpha


def bbox(alpha, x0, y0, x1, y1, thresh=0):
    minx, miny, maxx, maxy, n = None, None, None, None, 0
    for y in range(y0, min(y1, len(alpha))):
        row = alpha[y]
        for x in range(x0, min(x1, len(row))):
            if row[x] > thresh:
                n += 1
                minx = x if minx is None or x < minx else minx
                maxx = x if maxx is None or x > maxx else maxx
                miny = y if miny is None or y < miny else miny
                maxy = y if maxy is None or y > maxy else maxy
    return (minx, miny, maxx, maxy, n)


def ring_outside(alpha, x0, y0, x1, y1, pad=1):
    """帧外**紧邻** pad 像素内、非透明像素的逐边计数。

    判据要看**紧邻**：早先用 pad=6 会把相邻帧的画面算进来
    （例如 GAME_OVER 的左边 6px 落进 BOSS_SLAIN 的画面），
    于是每个帧都被误报「可能切到画面」。逐边 1px 才说明问题：
    某条边紧邻处有大量非透明像素 ⇒ 画面确实是**被这条边切断**的。
    """
    W, H = len(alpha[0]), len(alpha)
    sides = {'左': 0, '右': 0, '上': 0, '下': 0}
    for y in range(max(0, y0 - pad), min(H, y1 + pad)):
        for x in range(max(0, x0 - pad), min(W, x1 + pad)):
            if (x0 <= x < x1) and (y0 <= y < y1):
                continue
            if alpha[y][x] == 0:
                continue
            if x < x0:
                sides['左'] += 1
            elif x >= x1:
                sides['右'] += 1
            elif y < y0:
                sides['上'] += 1
            elif y >= y1:
                sides['下'] += 1
    return sides


def largest_clear_rect(alpha):
    """直方图最大矩形：全图内最大的「alpha 恒为 0」矩形。"""
    W, H = len(alpha[0]), len(alpha)
    heights = [0] * W
    best = (0, 0, 0, 0, 0)   # 面积, x0, y0, x1, y1
    for y in range(H):
        row = alpha[y]
        for x in range(W):
            heights[x] = heights[x] + 1 if row[x] == 0 else 0
        stack = []
        for x in range(W + 1):
            cur = heights[x] if x < W else 0
            start = x
            while stack and stack[-1][1] > cur:
                sx, sh = stack.pop()
                area = sh * (x - sx)
                if area > best[0]:
                    best = (area, sx, y - sh + 1, x, y + 1)
                start = sx
            stack.append((start, cur))
    return best


def main():
    w, h, alpha = load()
    print('banners.png  %dx%d' % (w, h))
    print()
    print('=== 各帧内画面的精确包围盒 ===')
    for name, (x0, y0, x1, y1) in FRAMES.items():
        bx0, by0, bx1, by1, n = bbox(alpha, x0, y0, x1, y1)
        if n == 0:
            print('  %-22s 帧 (%d,%d)-(%d,%d)  %dx%d   ** 全透明（空帧）**'
                  % (name, x0, y0, x1, y1, x1 - x0, y1 - y0))
            continue
        print('  %-22s 帧 (%d,%d)-(%d,%d)  %dx%d' % (name, x0, y0, x1, y1, x1 - x0, y1 - y0))
        print('        画面包围盒 (%d,%d)-(%d,%d)  非透明像素 %d' % (bx0, by0, bx1, by1, n))
        pad_l, pad_r = bx0 - x0, (x1 - 1) - bx1
        pad_t, pad_b = by0 - y0, (y1 - 1) - by1
        print('        帧内四边留白  左 %d  右 %d  上 %d  下 %d' % (pad_l, pad_r, pad_t, pad_b))
        sides = ring_outside(alpha, x0, y0, x1, y1, pad=1)
        hot = {k: v for k, v in sides.items() if v > 0}
        if not hot:
            print('        ✅ 帧外紧邻 1px 全透明 ⇒ 切得干净，画面没有被切断')
        else:
            print('        ⚠️ 帧外紧邻 1px 仍有像素：%s'
                  % '  '.join('%s %d' % (k, v) for k, v in hot.items()))
            print('           （若数值很大说明画面是被这条边**切断**的；'
                  '数值很小可能是相邻帧的画面，非本帧问题）')
    print()
    print('=== 旧帧位置现在还有没有内容（判断旧帧是否已作废） ===')
    for name, (x0, y0, x1, y1) in OLD.items():
        _, _, _, _, n = bbox(alpha, x0, y0, x1, y1)
        print('  %-20s (%d,%d)-(%d,%d)  非透明像素 %d%s'
              % (name, x0, y0, x1, y1, n, '' if n else '   ← 空'))
    print()
    area, x0, y0, x1, y1 = largest_clear_rect(alpha)
    print('=== 全图最大的「完全透明」矩形（供 TITLE_GLOW_* 指向空白）===')
    print('  (%d,%d)-(%d,%d)  尺寸 %dx%d  面积 %d px' % (x0, y0, x1, y1, x1 - x0, y1 - y0, area))
    print()
    print('  需要同尺寸空白的两个帧：')
    for name, (fw, fh) in (('TITLE_PORT 169×94', (169, 94)), ('TITLE_LAND 251×65', (251, 65))):
        ok = (x1 - x0) >= fw and (y1 - y0) >= fh
        print('    %-20s 最大空白 %dx%d ⇒ 同尺寸空白区%s'
              % (name, x1 - x0, y1 - y0, '存在' if ok else '**不存在**，只能指向更小的空白块'))
    print()
    print('=== 空白候选区（供选择） ===')
    for label, (cx0, cy0, cx1, cy1) in {
        'PC帧右侧': (428, 0, 512, 94),
        'y=96..151 整条': (0, 96, 512, 151),
        'y=226..255 整条': (0, 226, 512, 256),
    }.items():
        _, _, _, _, n = bbox(alpha, cx0, cy0, cx1, cy1)
        print('  %-16s (%d,%d)-(%d,%d) %dx%d  非透明像素 %d%s'
              % (label, cx0, cy0, cx1, cy1, cx1 - cx0, cy1 - cy0, n, '  ✅ 纯空白' if n == 0 else ''))
    return 0


if __name__ == '__main__':
    sys.exit(main())
