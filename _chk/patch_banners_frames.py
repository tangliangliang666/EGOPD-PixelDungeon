#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""按用户手工改过的新版面配置 BannerSprites.java（并备份 banners.png）。

新版面（用户于 2026-09-21 手工调整 `interfaces/banners.png`）：
    移动端标题 (0,0)-(169,94)      169×94
    PC端标题  (176,0)-(427,65)    251×65
    闪烁帧**保留但置空**（指向图内一块完全透明区，尺寸与对应标题帧一致，
    以后画好闪烁素材时把坐标改过去即可）
    BOSS_SLAIN (0,157)-(127,225) / GAME_OVER (128,157)-(256,192) 保持不变

安全性：
  * 备份 banners.png 到 `_chk/_bak_title_assets/`（**不放 assets 里**，否则会进 APK）
  * 本脚本用 Python 以 UTF-8 + LF 写 Java，避免编辑工具混存编码
  * 落盘前**断言那两个空帧矩形内 alpha 全为 0**（真的空才敢写进去）

用法：
    python _chk/patch_banners_frames.py --check
    python _chk/patch_banners_frames.py --apply
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

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BANNERS = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'interfaces', 'banners.png')
BAK_DIR = os.path.join(ROOT, '_chk', '_bak_title_assets')
JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                    'shatteredpixeldungeon', 'effects', 'BannerSprites.java')

# 新矩形：(x0, y0, x1, y1) —— uvRect 的后两参是**右下角坐标**（右/下为开区间）
NEW_RECTS = {
    'TITLE_PORT': (0, 0, 169, 94),
    'TITLE_LAND': (176, 0, 427, 65),
    'TITLE_GLOW_PORT': (253, 66, 422, 160),   # 空帧，尺寸 = TITLE_PORT 169×94
    'TITLE_GLOW_LAND': (253, 66, 504, 131),   # 空帧，尺寸 = TITLE_LAND 251×65
}
GLOW_KEYS = ('TITLE_GLOW_PORT', 'TITLE_GLOW_LAND')

# 上游原样（用于定位替换锚点）
OLD_LINES = {
    'TITLE_PORT': '\t\t\t\ticon.frame( icon.texture.uvRect( 0, 0, 139, 100 ) );',
    'TITLE_GLOW_PORT': '\t\t\t\ticon.frame( icon.texture.uvRect( 139, 0, 278, 100 ) );',
    'TITLE_LAND': '\t\t\t\ticon.frame( icon.texture.uvRect( 0, 100, 240, 157) );',
    'TITLE_GLOW_LAND': '\t\t\t\ticon.frame( icon.texture.uvRect( 240, 100, 480, 157 ) );',
}


def load_alpha():
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
    out, prev, pos = [], bytearray(stride), 0
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
        out.append([line[x * 4 + 3] for x in range(w)])
        prev = line
    return w, h, out


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--apply', action='store_true')
    args = ap.parse_args()

    w, h, alpha = load_alpha()
    print('banners.png %dx%d' % (w, h))

    for k, (x0, y0, x1, y1) in NEW_RECTS.items():
        assert 0 <= x0 < x1 <= w and 0 <= y0 < y1 <= h, '%s 越界' % k
        n = sum(1 for y in range(y0, y1) for x in range(x0, x1) if alpha[y][x] > 0)
        print('  %-16s (%3d,%3d)-(%3d,%3d)  %3dx%-3d  非透明像素 %d%s'
              % (k, x0, y0, x1, y1, x1 - x0, y1 - y0, n,
                 '  ← 空帧（要求 0）' if k in GLOW_KEYS else ''))
        if k in GLOW_KEYS and n != 0:
            raise SystemExit('%s 指向的区域不是全透明（%d 个像素），换个空白区再改' % (k, n))

    text = open(JAVA, encoding='utf-8').read()
    if 'uvRect( 0, 0, 169, 94 )' in text:
        print('\nBannerSprites.java：已是新版面，跳过。')
        return 0

    for k, old in OLD_LINES.items():
        x0, y0, x1, y1 = NEW_RECTS[k]
        if k in GLOW_KEYS:
            new = ('\t\t\t\t//闪烁帧暂为空帧：指向图内一块**完全透明**的区域（尺寸与对应标题帧一致，\n'
                   '\t\t\t\t//以后画好闪烁素材时，把这两个 uvRect 改到真实位置即可）。\n'
                   '\t\t\t\t//空帧仍会参与 TitleScene 的 alpha 动画与 Blending，只是画不出东西。\n'
                   '\t\t\t\ticon.frame( icon.texture.uvRect( %d, %d, %d, %d ) );' % (x0, y0, x1, y1))
            # 注释只给第一个闪烁帧，第二个直接一行
            if k == 'TITLE_GLOW_LAND':
                new = '\t\t\t\ticon.frame( icon.texture.uvRect( %d, %d, %d, %d ) );' % (x0, y0, x1, y1)
        else:
            new = '\t\t\t\ticon.frame( icon.texture.uvRect( %d, %d, %d, %d ) );' % (x0, y0, x1, y1)
        if text.count(old) != 1:
            raise SystemExit('%s 锚点未唯一命中（%d 次），人工确认后再改：\n%r' % (k, text.count(old), old))
        text = text.replace(old, new)
        print('  改写 %-16s → uvRect( %d, %d, %d, %d )' % (k, x0, y0, x1, y1))

    if args.check:
        print('\n[check] 未写盘。')
        return 0

    os.makedirs(BAK_DIR, exist_ok=True)
    bak = os.path.join(BAK_DIR, 'banners.png')
    if not os.path.exists(bak):
        shutil.copy2(BANNERS, bak)
        print('\n  已备份 banners.png → %s' % os.path.relpath(bak, ROOT))
    else:
        print('\n  备份已存在，不覆盖：%s' % os.path.relpath(bak, ROOT))

    with open(JAVA, 'w', encoding='utf-8', newline='') as fh:
        fh.write(text)
    print('  已写入 %s' % os.path.relpath(JAVA, ROOT))
    print('[apply] 完成。下一步：python _chk/verify_banners.py')
    return 0


if __name__ == '__main__':
    sys.exit(main())
