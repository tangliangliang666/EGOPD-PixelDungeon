#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""新版横幅版面（banners.png + BannerSprites.java）的核验（回归断言）。

判据：
  A. banners.png 仍 512×256 / RGBA
  B. **从 BannerSprites.java 现读**六个 uvRect，逐个断言等于约定值
     （改代码或改图任一边不一致就会被抓出来）
  C. **闪烁帧指向的区域在图内必须完全透明** —— 这是「空帧」这个约定的全部依据；
     一旦素材挪动导致那块不再为空，空帧就会画出东西（而且是错位的碎片）
  D. 两个标题帧内必须有实际画面（不是空帧），且三边（左/右/上）不越界
  E. BOSS_SLAIN / GAME_OVER 的矩形**保持上游原值**，且区域内仍有画面
     （用户手工重排时最容易误伤的两块）
  F. 备份存在（banners.png 是手工资产，没有 git 版本可回退）
  G. 报告横幅宽度与相机宽度的适配关系（新版面比旧版宽，可能被裁）

用法：python _chk/verify_banners.py
"""

import os
import re
import struct
import sys
import zlib

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BANNERS = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'interfaces', 'banners.png')
BAK = os.path.join(ROOT, '_chk', '_bak_title_assets', 'banners.png')
JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                    'shatteredpixeldungeon', 'effects', 'BannerSprites.java')

EXPECT = {
    'TITLE_PORT': (0, 0, 169, 94),
    'TITLE_LAND': (176, 0, 427, 65),
    'TITLE_GLOW_PORT': (253, 66, 422, 160),
    'TITLE_GLOW_LAND': (253, 66, 504, 131),
}
UPSTREAM_KEEP = {
    'BOSS_SLAIN': (0, 157, 127, 225),
    'GAME_OVER': (128, 157, 256, 192),
}
GLOW = ('TITLE_GLOW_PORT', 'TITLE_GLOW_LAND')

PASS, FAIL = [], []


def check(name, ok, detail=''):
    (PASS if ok else FAIL).append(name)
    print('%-4s %s%s' % ('PASS' if ok else 'FAIL', name, ('  <- ' + detail) if detail else ''))


def load_alpha():
    raw = open(BANNERS, 'rb').read()
    w, h = struct.unpack('>II', raw[16:24])
    bd, ct = raw[24], raw[25]
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
    return w, h, bd, ct, out


def java_rects():
    text = open(JAVA, encoding='utf-8').read()
    out = {}
    for key in list(EXPECT) + list(UPSTREAM_KEEP):
        m = re.search(r'case %s:(.*?)break;' % key, text, re.S)
        if not m:
            out[key] = None
            continue
        r = re.search(r'uvRect\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', m.group(1))
        out[key] = tuple(int(v) for v in r.groups()) if r else None
    return text, out


def count(alpha, r):
    x0, y0, x1, y1 = r
    return sum(1 for y in range(y0, y1) for x in range(x0, x1) if alpha[y][x] > 0)


def main():
    w, h, bd, ct, alpha = load_alpha()
    check('banners.png 仍 512×256', (w, h) == (512, 256), '%dx%d' % (w, h))
    check('banners.png 仍 RGBA（色型 6，位深 8）', ct == 6 and bd == 8, 'ct=%d bd=%d' % (ct, bd))
    print()

    text, rects = java_rects()
    for key, want in EXPECT.items():
        got = rects.get(key)
        check('BannerSprites %-16s == %s' % (key, want), got == want, '实际 %s' % (got,))
        if got and key in GLOW:
            n = count(alpha, got)
            check('  └ 空帧 %s 指向区域**确实全透明**（%d px）' % (key, n), n == 0,
                  '' if n == 0 else '非透明像素 %d ⇒ 会画出错位的碎片！' % n)
        elif got and key not in GLOW:
            n = count(alpha, got)
            check('  └ 标题帧 %s 内有实际画面' % key, n > 500, '%d 个非透明像素' % n)
    print()

    for key, want in UPSTREAM_KEEP.items():
        got = rects.get(key)
        check('%s 保持上游原值 %s' % (key, want), got == want, '实际 %s' % (got,))
        if got:
            n = count(alpha, got)
            check('  └ %s 区域内仍有画面（未被误伤）' % key, n > 500, '%d 个非透明像素' % n)
    print()

    # 边界越界检查
    bad = []
    for key, got in rects.items():
        if not got:
            bad.append(key)
            continue
        x0, y0, x1, y1 = got
        if not (0 <= x0 < x1 <= w and 0 <= y0 < y1 <= h):
            bad.append('%s=%s' % (key, got))
    check('六个帧矩形全部在图内且合法', not bad, '异常：%s' % bad if bad else '')
    print()

    check('banners.png 有备份（手工资产无 git 版本可退）', os.path.isfile(BAK),
          os.path.relpath(BAK, ROOT) if os.path.isfile(BAK) else '缺备份！')
    check('BannerSprites.java 是合法 UTF-8（含中文注释）', '\ufffd' not in text)
    print()

    # 闪烁帧的消费方仍在（说明空帧这条路径确实被走到）
    cons = []
    for base, _dirs, files in os.walk(os.path.join(ROOT, 'core', 'src', 'main', 'java')):
        for f in files:
            if f.endswith('.java'):
                p = os.path.join(base, f)
                t = open(p, encoding='utf-8', errors='replace').read()
                if 'TITLE_GLOW' in t and f != 'BannerSprites.java':
                    cons.append(f)
    check('闪烁帧仍被消费（空帧路径会被走到）', len(cons) >= 2, '消费方：%s' % '、'.join(sorted(cons)))
    print()

    # 适配报告：新版横幅比旧版宽
    print('=== 横幅宽度 vs 相机宽度（新版比旧版宽：竖 139→169、横 240→251）===')
    print('  判定：Camera 宽度 ≥ 横幅宽度才不被裁（TitleScene:112 `x = left+(w-横幅宽)/2`）')
    for label, banner_w, min_w in (('竖屏 TITLE_PORT', EXPECT['TITLE_PORT'][2], 135),
                                   ('横屏 TITLE_LAND', EXPECT['TITLE_LAND'][2] - EXPECT['TITLE_LAND'][0], 240)):
        print('    %-16s 横幅宽 %d，各模式最小相机宽 %d ⇒ %s'
              % (label, banner_w, min_w,
                 '最小宽度下**必被裁 %d px**' % (banner_w - min_w) if banner_w > min_w
                 else '最小宽度下也放得下'))
    print()
    print('  常见机型的相机宽度（= 屏幕像素 / zoom，zoom 由 PixelScene.create() 111-145 行算）：')
    for res, dens in (((1080, 2400), 2.75), ((1440, 3200), 3.5), ((720, 1280), 2.0)):
        sw, sh = res
        mx = max(2, int(min(sw / 135.0, sh / 225.0)))
        zoom = max(2, min(-(-int(dens * 2.5 * 100) // 100), mx))  # ceil(density*2.5) 再 gate
        cw = sw / zoom
        print('    %dx%d @density %.2f ⇒ zoom %d，相机宽 ≈ %.0f  %s'
              % (sw, sh, dens, zoom, cw,
                 '✅ 放得下 169' if cw >= 169 else '⚠️ 小于 169，竖屏横幅会被裁 %.0f px' % (169 - cw)))
    print('    （density 是估算值；要确认就实机跑一次并打 Camera.main.width）')
    print()

    print('=== 已知的 1px 问题（按你给的原值配置，供你定夺）===')
    print('  两块标题画面的底边都比帧多 1 行（那一行满宽、alpha 255）：')
    print('    移动端 画面 y=0..94 ⇒ 帧高应为 95（你给 94）')
    print('    PC 端  画面 y=0..65 ⇒ 帧高应为 66（你给 65）')
    print('  要补上就把 uvRect 的 y1 各加 1：TITLE_PORT (0,0,169,95)、TITLE_LAND (176,0,427,66)')
    print()

    print('%d 项通过，%d 项失败' % (len(PASS), len(FAIL)))
    if FAIL:
        print('失败：' + '、'.join(FAIL))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
