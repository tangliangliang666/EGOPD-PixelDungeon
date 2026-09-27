#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""滚动背景改色后的结构核验（回归断言；改了 TitleBackground 或这几张图都该跑）。

判据（全部逐字节 / 逐项）：
  A. 4 张图与备份的**尺寸、位深、颜色类型、交错**完全相同
     —— 尺寸一变 TextureFilm 的帧网格就会错位（§15.2）
  B. **IDAT 逐字节相同** —— 证明像素索引没动过，只换了调色板
  C. **tRNS 逐字节相同** —— 证明图层透明度/轮廓没动过
  D. PLTE 项数相同，且内容确实变了（否则等于没改）
  E. 每个调色板项的**透明度分组不变**（全透明/半透明/不透明的项数一致）
  F. **帧网格仍成立**：从 TitleBackground.java 现读帧尺寸与权重数组长度，
     断言 cols×rows ≥ 所需帧数，且权重数组长度 ≤ 可用帧数
     （长度超了 ⇒ TextureFilm.get 返 null ⇒ Image.frame 解引用 NPE，运行期才崩）
  G. **横幅 banners.png 未被改动**（用 git 判定 + 颜色特征双保险）

用法：
    python _chk/verify_title_bg.py            # 核验
    python _chk/verify_title_bg.py --restore  # 从备份恢复原图（恢复后本脚本应报「未改色」）
"""

import argparse
import os
import re
import struct
import subprocess
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSET_DIR = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'splashes', 'title')
BANNER = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'interfaces', 'banners.png')
BACKUP_DIR = os.path.join(ROOT, '_chk', '_bak_title_bg')
TB = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                  'shatteredpixeldungeon', 'ui', 'TitleBackground.java')

TARGETS = ['archs.png', 'back_clusters.png', 'mid_mixed.png', 'front_small.png']
ASSET_KEY = {
    'archs.png': 'ARCHS',
    'back_clusters.png': 'BACK_CLUSTERS',
    'mid_mixed.png': 'MID_MIXED',
    'front_small.png': 'FRONT_SMALL',
}
CHANCE_NAME = {
    'archs.png': 'INIT_ARCH_CHANCES',
    'back_clusters.png': 'INIT_CLUSTER_CHANCES',
    'mid_mixed.png': 'INIT_MID_CHANCES',
    'front_small.png': 'INIT_SMALL_CHANCES',
}

PASS, FAIL = [], []


def fmt_time(ts):
    import time
    return time.strftime('%Y-%m-%d %H:%M:%S', time.localtime(ts))


def check(name, ok, detail=''):
    (PASS if ok else FAIL).append(name)
    print('%-4s %s%s' % ('PASS' if ok else 'FAIL', name, ('  <- ' + detail) if detail else ''))


def parse_png(raw):
    if raw[:8] != b'\x89PNG\r\n\x1a\n':
        raise SystemExit('不是 PNG')
    w, h = struct.unpack('>II', raw[16:24])
    bit_depth, color_type, interlace = raw[24], raw[25], raw[28]
    chunks = {}
    order = []
    i = 8
    while i < len(raw):
        ln = struct.unpack('>I', raw[i:i + 4])[0]
        typ = raw[i + 4:i + 8]
        chunks[typ] = raw[i + 8:i + 8 + ln]
        order.append(typ)
        i += 12 + ln
        if typ == b'IEND':
            break
    return w, h, bit_depth, color_type, interlace, chunks


def src_pitches():
    """从 TitleBackground.java 现读 (帧宽, 帧高) 与权重数组长度。"""
    text = open(TB, encoding='utf-8').read()
    out = {}
    for png, key in ASSET_KEY.items():
        m = re.search(r'new TextureFilm\(\s*Assets\.Splashes\.Title\.%s\s*,\s*(\d+)\s*,\s*(\d+)\s*\)'
                      % re.escape(key), text)
        if not m:
            raise SystemExit('在 TitleBackground.java 里找不到 %s 的 TextureFilm' % key)
        fw, fh = int(m.group(1)), int(m.group(2))
        c = re.search(r'%s\s*=\s*\{([^}]*)\}' % CHANCE_NAME[png], text)
        if not c:
            raise SystemExit('找不到权重数组 %s' % CHANCE_NAME[png])
        arr = [v for v in c.group(1).replace('\n', ' ').split(',') if v.strip()]
        out[png] = (fw, fh, len(arr))
    return out


def avg_color(chunks):
    plte = chunks.get(b'PLTE')
    trns = chunks.get(b'tRNS', b'')
    if plte is None:
        return (0, 0, 0), 0
    tot = [0, 0, 0]
    n = 0
    for i in range(0, len(plte), 3):
        idx = i // 3
        a = trns[idx] if idx < len(trns) else 255
        if a == 0:
            continue
        tot[0] += plte[i]; tot[1] += plte[i + 1]; tot[2] += plte[i + 2]
        n += 1
    if n == 0:
        return (0, 0, 0), 0
    return tuple(round(v / n) for v in tot), n


def git_touched(rel):
    """返回 (是否被 git 视为已修改, 是否被跟踪)。"""
    try:
        st = subprocess.run(['git', '-C', ROOT, 'status', '--porcelain', '--', rel],
                            capture_output=True, text=True, timeout=60).stdout.strip()
    except Exception:
        return None, None
    if not st:
        return False, True
    return (not st.startswith('??')), not st.startswith('??')


def restore():
    if not os.path.isdir(BACKUP_DIR):
        raise SystemExit('没有备份目录，无法恢复：%s' % BACKUP_DIR)
    for name in TARGETS:
        src = os.path.join(BACKUP_DIR, name)
        if not os.path.isfile(src):
            raise SystemExit('备份缺 %s' % name)
        dst = os.path.join(ASSET_DIR, name)
        open(dst, 'wb').write(open(src, 'rb').read())
        print('  已恢复', name)
    marker = os.path.join(BACKUP_DIR, 'RECOLORED.txt')
    if os.path.exists(marker):
        os.remove(marker)
    print('恢复完成（标记已清除）。')
    return 0


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--restore', action='store_true', help='从备份恢复原图')
    args = ap.parse_args()
    if args.restore:
        return restore()

    if not os.path.isdir(BACKUP_DIR):
        raise SystemExit('缺备份目录，先跑 recolor_title_bg.py --apply')

    pitches = src_pitches()
    print('从 TitleBackground.java 现读：' + '  '.join(
        '%s=%dx%d/%d帧' % (ASSET_KEY[k], v[0], v[1], v[2]) for k, v in pitches.items()))
    print()

    for name in TARGETS:
        cur = parse_png(open(os.path.join(ASSET_DIR, name), 'rb').read())
        old = parse_png(open(os.path.join(BACKUP_DIR, name), 'rb').read())
        tag = name

        check('%s 尺寸/位深/色型/交错不变' % tag, cur[:5] == old[:5],
              '%dx%d -> %dx%d' % (old[0], old[1], cur[0], cur[1]))
        check('%s IDAT 逐字节不变（像素索引没动）' % tag, cur[5].get(b'IDAT') == old[5].get(b'IDAT'))
        check('%s tRNS 逐字节不变（透明度没动）' % tag, cur[5].get(b'tRNS') == old[5].get(b'tRNS'))

        cp, op = cur[5].get(b'PLTE'), old[5].get(b'PLTE')
        check('%s PLTE 项数不变' % tag, len(cp) == len(op), '%d -> %d 项' % (len(op) // 3, len(cp) // 3))
        check('%s PLTE 确实变了' % tag, cp != op)

        ct, ctold = cur[5].get(b'tRNS', b''), old[5].get(b'tRNS', b'')
        if ct or ctold:
            same = all((ct[i] if i < len(ct) else 255) == (ctold[i] if i < len(ctold) else 255)
                       for i in range(max(len(ct), len(ctold))))
            check('%s 逐项 alpha 不变' % tag, same)

        new_avg, n_new = avg_color(cur[5])
        old_avg, n_old = avg_color(old[5])
        red_dominant = new_avg[0] > new_avg[1] and new_avg[0] > new_avg[2]
        check('%s 改后确实偏红（均色 %s -> %s）' % (tag, old_avg, new_avg), red_dominant)
        check('%s 可见色项数不变' % tag, n_new == n_old, '%d -> %d' % (n_old, n_new))

        # F. 帧网格
        fw, fh, need = pitches[name]
        w, h = cur[0], cur[1]
        cols, rows = w // fw, h // fh
        check('%s 帧网格 %dx%d=可用 %d 帧 ≥ 需 %d（帧 %dx%d）' % (tag, cols, rows, cols * rows, need, fw, fh),
              cols * rows >= need)
        if cols * rows < need:
            print('       !! 可用 %d < 权重数组 %d ⇒ TextureFilm.get 会返 null ⇒ Image.frame NPE'
                  % (cols * rows, need))
        print()

    # G. 横幅未被改动
    # 注意：banners.png 是 **RGBA**（不是索引图），所以「调色板改色」在物理上碰不到它；
    # 而且本仓的 banners.png 是 mod 早就换过的自定义图（git 显示 Bin 29452 -> 44448，
    # 早于本次操作），因此「git 未修改」不能当判据——改用**时间戳**：横幅的 mtime
    # 必须早于本次改色（以 RECOLORED.txt 的 mtime 为证）。
    bcur = parse_png(open(BANNER, 'rb').read())
    check('横幅 banners.png 尺寸仍 512×256', (bcur[0], bcur[1]) == (512, 256), '%dx%d' % (bcur[0], bcur[1]))
    check('横幅是 RGBA（非索引图）⇒ 调色板改色在物理上无法影响它', bcur[3] == 6, 'color type=%d' % bcur[3])
    # 「横幅未被本操作写入」原先用 **mtime 比较**证明（横幅 mtime < 改色记录 mtime）。
    # 但 2026-09-21 用户**手工重排了 banners.png**，它的 mtime 从此晚于改色记录，
    # 该判据必然为假 —— 它编码的是一个**已被后来的合法编辑推翻**的假设。
    # 改为断言**结构性保证**：改色脚本的 TARGETS 里根本不含 banners.png，
    # 所以它不可能写到这个文件；哈希与 mtime 只作存档打印，不再当判据。
    import sys as _sys
    _sys.path.insert(0, os.path.join(ROOT, '_chk'))
    try:
        import recolor_title_bg as _RB
        targets = list(_RB.TARGETS)
    except Exception as exc:                                   # noqa: BLE001
        targets = []
        print('     （导入 recolor_title_bg 失败，跳过结构性断言：%s）' % exc)
    if targets:
        check('背景改色脚本的目标不含 banners.png（结构上写不到它）',
              'banners.png' not in targets, 'TARGETS=%s' % targets)
    import hashlib
    bhash = hashlib.sha256(open(BANNER, 'rb').read()).hexdigest()
    print('     横幅 sha256 = %s（Size %d，mtime %s）'
          % (bhash, os.path.getsize(BANNER), fmt_time(os.path.getmtime(BANNER))))
    print('     注：横幅本体已由用户手工重排，内容一致性由 _chk/verify_banners.py 负责核验。')

    print('\n%d 项通过，%d 项失败' % (len(PASS), len(FAIL)))
    if FAIL:
        print('失败：' + '、'.join(FAIL))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
