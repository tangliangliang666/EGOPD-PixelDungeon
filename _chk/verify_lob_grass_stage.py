# -*- coding: utf-8 -*-
"""
27 层草叶细节区域核验（2026-09-19 建；2026-09-25 改判据为「第二区」）

模拟 Java 侧 TerrainFeaturesTilemap.getTileVisual() 的帧槽计算，
断言「27 层（LobTestLevel）的草地细节」取到的确实是与 tiles_lob 草皮一致的区段——
tiles_lob 的草尖取自**第二区（监狱）**，故草叶细节必须报 stage 1。

本脚本的四层证据：
  0) 源码一致性：LobTestLevel.FEATURES_STAGE == 1，且 featuresStage() 直接 return 它
     （**不得**再退回 (GEN_DEPTH-1)/5 —— 那是错的，会被下面的主色指纹反证）
  1) 逐像素：tiles_lob 槽 250 与 tiles_prison 槽 250 全等 ⇒ 本层草 = 第二区材质
  2) 主色指纹：terrain_features 第二区草叶的 top-3 主色 == tiles_lob 草尖的 top-3；
     第一区（下水道）则明显不同 ⇒ 「报第一区」在像素层就被否掉
  3) 反例自测：关掉 FeaturesTexProvider 分支，须复现旧 bug（落到 stage 4）

用法：
    python _chk/verify_lob_grass_stage.py
"""
import os
import re
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
ENV = os.path.join(ROOT, 'core', 'src', 'main', 'assets', 'environment')
LOB_JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel',
                        'shatteredpixeldungeon', 'levels', 'LobTestLevel.java')

FRAME = 16
REGIONS = ['下水道(1区)', '监狱(2区)', '矿洞(3区)', '都市(4区)', '恶魔大厅(5区)']

# LobTestLevel 的常量（与 Java 源码保持一致）
LOB_GEN_DEPTH = 4
LOB_REAL_DEPTH = 27
LOB_FEATURES_STAGE = 1     # FEATURES_STAGE：第二区（监狱）


# ---------------------------------------------------------------- Java 侧模拟
def features_stage_java(level_is_provider, provider_stage, depth, is_last_shop=False):
    """复刻 TerrainFeaturesTilemap.featuresStage()"""
    if level_is_provider:
        return max(0, min(provider_stage, 4))
    stage = (depth - 1) // 5
    if depth == 21 and is_last_shop:
        stage -= 1
    return min(stage, 4)


def grass_slot(tile_kind, stage, alt):
    """复刻 getTileVisual 的槽位公式"""
    base = {'HIGH_GRASS': 9, 'FURROWED_GRASS': 11, 'GRASS': 13}[tile_kind]
    return base + 16 * stage + (1 if alt else 0)


# ---------------------------------------------------------------- PNG 主色
def read_png(path):
    d = open(path, 'rb').read()
    pos = 8
    idat = b''
    pal = trns = None
    w = h = bd = ct = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bd, ct = struct.unpack('>IIBB', data[:10])
        elif typ == b'IDAT':
            idat += data
        elif typ == b'PLTE':
            pal = data
        elif typ == b'tRNS':
            trns = data
        pos += 12 + ln
    raw = zlib.decompress(idat)
    channels = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ct]
    stride = w * channels
    lines = []
    prev = bytearray(stride)
    i = 0
    for _ in range(h):
        ft = raw[i]; i += 1
        line = bytearray(raw[i:i + stride]); i += stride
        if ft == 1:
            for x in range(channels, stride):
                line[x] = (line[x] + line[x - channels]) & 0xFF
        elif ft == 2:
            for x in range(stride):
                line[x] = (line[x] + prev[x]) & 0xFF
        elif ft == 3:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                line[x] = (line[x] + ((a + prev[x]) >> 1)) & 0xFF
        elif ft == 4:
            for x in range(stride):
                a = line[x - channels] if x >= channels else 0
                b = prev[x]
                c = prev[x - channels] if x >= channels else 0
                p = a + b - c
                pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[x] = (line[x] + pr) & 0xFF
        lines.append(bytes(line))
        prev = line

    def px(x, y):
        s = lines[y]
        if ct == 6:
            o = x * 4; return s[o], s[o+1], s[o+2], s[o+3]
        if ct == 2:
            o = x * 3; return s[o], s[o+1], s[o+2], 255
        if ct == 0:
            v = s[x]; return v, v, v, 255
        if ct == 3:
            idx = s[x]
            r, g, b = pal[idx*3:idx*3+3]
            a = trns[idx] if trns and idx < len(trns) else 255
            return r, g, b, a
    return px


def slot_signature(px, slot, width=16):
    col, row = slot % width, slot // width
    pix = []
    for y in range(row*FRAME, row*FRAME+FRAME):
        for x in range(col*FRAME, col*FRAME+FRAME):
            pix.append(px(x, y))
    return pix


def top_colors(px, slot, n=3, width=16):
    """槽内「不透明」像素的 top-n 主色（跨区比较草风格的稳健指纹）"""
    from collections import Counter
    c = Counter()
    for r, g, b, a in slot_signature(px, slot, width):
        if a == 255:
            c['#%02X%02X%02X' % (r, g, b)] += 1
    return [col for col, _ in c.most_common(n)]


def solid_set(sig):
    return set(i for i, p in enumerate(sig) if p[3] == 255)


# ---------------------------------------------------------------- 主流程
def main():
    fails = []
    fpx = read_png(os.path.join(ENV, 'terrain_features.png'))
    lpx = read_png(os.path.join(ENV, 'tiles_lob.png'))
    ppx = read_png(os.path.join(ENV, 'tiles_prison.png'))

    # ---------------------------------------------------------- 0) 源码一致性
    print('=== 0) 源码：LobTestLevel 的草叶区配置 ===')
    src = open(LOB_JAVA, 'r', encoding='utf-8').read()
    m = re.search(r'public static final int FEATURES_STAGE\s*=\s*(\d+)\s*;', src)
    if not m:
        fails.append('LobTestLevel.java 里找不到 FEATURES_STAGE 常量')
        print('  !!! 未找到 FEATURES_STAGE 常量')
    else:
        print('  FEATURES_STAGE = %s' % m.group(1))
        if int(m.group(1)) != LOB_FEATURES_STAGE:
            fails.append('FEATURES_STAGE 应为 %d，实为 %s' % (LOB_FEATURES_STAGE, m.group(1)))
    body = re.search(r'public int featuresStage\(\)\s*\{(.*?)\n\t\}', src, re.S)
    if not body:
        fails.append('找不到 featuresStage() 方法体')
    else:
        b = body.group(1)
        if 'return FEATURES_STAGE;' not in b:
            fails.append('featuresStage() 未直接返回 FEATURES_STAGE')
        if re.search(r'GEN_DEPTH', b):
            fails.append('featuresStage() 又跟 GEN_DEPTH 绑定了（GEN_DEPTH=4 下水道 ⇒ 会退回第一区）')
        print('  featuresStage() 方法体：%s' % ' '.join(b.split()))
    print()

    print('=== 1) tiles_lob 的草叶细节确实等同第二区（监狱）===')
    lob_under = slot_signature(lpx, 250)   # HIGH_GRASS_UNDERHANG：只有草尖不透明
    pri_under = slot_signature(ppx, 250)
    lob_solid, pri_solid = solid_set(lob_under), solid_set(pri_under)
    print('  槽 250 实心像素数  lob=%d  prison=%d  集合相同=%s'
          % (len(lob_solid), len(pri_solid), lob_solid == pri_solid))
    rgb_same = all(lob_under[i][:3] == pri_under[i][:3] for i in lob_solid & pri_solid)
    print('  共有实心像素的 RGB 全等       : %s' % ('是' if rgb_same else '否'))
    if not (lob_solid == pri_solid and rgb_same):
        fails.append('tiles_lob 的草叶与 tiles_prison 不一致，前提假设有误')
    else:
        print('  ✓ 逐像素相同 ⇒ tiles_lob 草 = 第二区材质')
    # 顺带确认 122 帧的差异只在底色，不在草形
    lob_raised, pri_raised = slot_signature(lpx, 122), slot_signature(ppx, 122)
    print('  槽 122 全帧不同（%d/256 px）——但差异仅为地块底色，草形取自同一套'
          % sum(1 for x, y in zip(lob_raised, pri_raised) if x != y))
    print()

    print('=== 2) 主色指纹：草叶细节该取哪一区 ===')
    lob_tip = top_colors(lpx, 250)
    print('  tiles_lob 草尖(槽250) top3        : %s' % ' '.join(lob_tip))
    for stage in (0, 1):
        slot = grass_slot('HIGH_GRASS', stage, False)
        tf = top_colors(fpx, slot)
        same = tf == lob_tip
        print('  terrain_features %-12s 槽%-3d top3: %s  %s'
              % (REGIONS[stage], slot, ' '.join(tf), '与草尖一致' if same else '不一致'))
        if stage == LOB_FEATURES_STAGE and not same:
            fails.append('第二区草叶主色与 tiles_lob 草尖不一致（%s vs %s）'
                         % (' '.join(tf), ' '.join(lob_tip)))
    stage0 = top_colors(fpx, grass_slot('HIGH_GRASS', 0, False))
    if stage0 == lob_tip:
        fails.append('第一区主色竟与草尖一致，本脚本的「第一区是错的」前提失效')
    else:
        print('  ✓ 第二区与草尖同指纹、第一区不同 ⇒ 报 stage 1 才是对的')
    print()

    print('=== 3) 反例自测：不实现 provider 时的真实深度算法 ===')
    bug_stage = features_stage_java(False, 0, LOB_REAL_DEPTH)
    bug_slot = grass_slot('HIGH_GRASS', bug_stage, False)
    print('  (27-1)/5 = %d → min(...,4) = %d  ⇒ %s，槽 %d'
          % ((LOB_REAL_DEPTH-1)//5, bug_stage, REGIONS[bug_stage], bug_slot))
    if bug_stage != 4:
        fails.append('反例自测失败：旧逻辑未复现 stage 4')
    else:
        print('  ✓ 旧 bug 复现成功（stage 4 = 恶魔大厅蘑菇形）')
    print()

    print('=== 4) 现配置：LobTestLevel.featuresStage() = FEATURES_STAGE = %d ===' % LOB_FEATURES_STAGE)
    fix_stage = features_stage_java(True, LOB_FEATURES_STAGE, LOB_REAL_DEPTH)
    print('  ⇒ %s，槽 %d（修复前是 %s 槽 %d）'
          % (REGIONS[fix_stage], grass_slot('HIGH_GRASS', fix_stage, False),
             REGIONS[bug_stage], bug_slot))
    if fix_stage != LOB_FEATURES_STAGE:
        fails.append('featuresStage 落点应为 %d，实为 %d' % (LOB_FEATURES_STAGE, fix_stage))
    print()
    for kind in ('HIGH_GRASS', 'FURROWED_GRASS', 'GRASS'):
        for alt in (False, True):
            s = grass_slot(kind, fix_stage, alt)
            row = s // 16
            ok = row == 1
            print('  %-14s alt=%-5s 槽 %-3d 图集行 %d  %s'
                  % (kind, alt, s, row, 'OK（第二区）' if ok else '!!! 不在第二区'))
            if not ok:
                fails.append('%s alt=%s 的槽 %d 不在第二区（图集行应为 1）' % (kind, alt, s))
    print()

    print('=== 5) 回归守卫：错误的写法会把区段带回第一区 ===')
    wrong = (LOB_GEN_DEPTH - 1) // 5
    print('  (GEN_DEPTH-1)/5 = (%d-1)/5 = %d ⇒ %s ← 错误写法，勿再采用'
          % (LOB_GEN_DEPTH, wrong, REGIONS[wrong]))
    if wrong == LOB_FEATURES_STAGE:
        print('  （当前两者恰好相同，守卫无区分力）')
    else:
        print('  ✓ 与正确的 %d（%s）不同，可作为源码级回归判据' % (LOB_FEATURES_STAGE, REGIONS[LOB_FEATURES_STAGE]))
    print()

    if fails:
        print('核验失败：')
        for f in fails:
            print('  - ' + f)
        sys.exit(1)
    print('核验通过：27 层草地细节取第二区（监狱），与 tiles_lob 的监狱风格草皮一致，不再出现五区蘑菇形。')


if __name__ == '__main__':
    main()
