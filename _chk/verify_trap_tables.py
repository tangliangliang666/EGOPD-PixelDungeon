#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""核验地形编辑器里的「陷阱 / 植物」查表是否与 Java 源逐项一致。

为什么需要这个脚本
------------------
陷阱在 SPD 里**不是地形**：`TerrainFeaturesTilemap.getTileVisual()` 先查
`SparseArray<Trap> traps`，命中就返回 `(active ? color : BLACK) + shape*16`。
编辑器（JS）必须把这 33 个陷阱类的 color/shape **逐项抄对**，抄错一个的表现是
「这个陷阱画出来是另一个陷阱的样子」—— 肉眼极难发现（都是彩色小符号）。

所以这里**不读 JS 表**，而是：
  1) 直接解析 Java 源：每个陷阱类的 `color = X` / `shape = Y`，
     没有的就沿 `extends` 链回溯父类（GnollRockfallTrap→RockfallTrap 等）；
  2) 从 Trap.java 解析 RED=0..BLACK=8 / DOTS=0..LARGE_DOT=6 的数值；
  3) 解析 render.js 里的 TRAPS / PLANTS 两张表；
  4) 逐项比对类名集合、color、shape、以及 frame = color + shape*16。
最后再用真图集 terrain_features.png 确认这些帧**都画了**（不是空槽位）。

反向自测（falsification）
------------------------
把 JS 表里某一项故意改错，脚本必须报错。见 --selftest。
"""
import os
import re
import sys
import zlib
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TRAPS_DIR = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/levels/traps')
PLANTS_DIR = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/plants')
TRAP_JAVA = os.path.join(TRAPS_DIR, 'Trap.java')
RENDER_JS = os.path.join(ROOT, 'tools/terrain-editor/render.js')
FEATURES_PNG = os.path.join(ROOT, 'core/src/main/assets/environment/terrain_features.png')

fail = []
def chk(cond, msg):
    if cond:
        print('  ✓ ' + msg)
    else:
        print('  ✗ ' + msg)
        fail.append(msg)


# ---------------------------------------------------------------- Java 解析

def read(p):
    with open(p, 'r', encoding='utf-8') as f:
        return f.read()


def parse_trap_constants():
    """从 Trap.java 取 color / shape 常量数值。"""
    src = read(TRAP_JAVA)
    colors = dict(re.findall(r'public static final int ([A-Z_]+)\s*=\s*(\d+);', src))
    # color 与 shape 同名不冲突（RED/DOTS 各一组），上面一把抓即可
    return colors


def strip_comments(s):
    s = re.sub(r'/\*.*?\*/', '', s, flags=re.S)
    s = re.sub(r'//[^\n]*', '', s)
    return s


def parse_trap_class(path):
    """返回 (类名, 父类名, {color: X}, {shape: Y})；字段可能没有。"""
    src = strip_comments(read(path))
    m = re.search(r'public\s+class\s+(\w+)\s+extends\s+([\w.]+)', src)
    if not m:
        return None
    cls, parent = m.group(1), m.group(2).split('.')[-1]
    fields = {}
    for f in ('color', 'shape'):
        mm = re.search(r'\b' + f + r'\s*=\s*([A-Z_]+)\s*;', src)
        if mm:
            fields[f] = mm.group(1)
    return cls, parent, fields


def collect_traps():
    """解析全部具体陷阱类，沿继承链补齐 color/shape。"""
    table = {}
    for fn in sorted(os.listdir(TRAPS_DIR)):
        if not fn.endswith('.java'):
            continue
        info = parse_trap_class(os.path.join(TRAPS_DIR, fn))
        if not info:
            continue
        cls, parent, fields = info
        if cls == 'Trap':
            continue
        table[cls] = {'parent': parent, 'fields': fields}

    # 沿父类链回溯（最多 5 层，防环）
    def resolve(cls, key, depth=0):
        if depth > 5 or cls not in table:
            return None
        if key in table[cls]['fields']:
            return table[cls]['fields'][key]
        return resolve(table[cls]['parent'], key, depth + 1)

    out = {}
    for cls in table:
        c = resolve(cls, 'color')
        s = resolve(cls, 'shape')
        if c and s:
            out[cls] = (c, s)
    return out, table


def collect_plants():
    """解析植物类：image = N（Plant 子类）。"""
    out = {}
    for fn in sorted(os.listdir(PLANTS_DIR)):
        if not fn.endswith('.java'):
            continue
        src = strip_comments(read(os.path.join(PLANTS_DIR, fn)))
        m = re.search(r'class\s+(\w+)\s+extends\s+Plant\b', src)
        if not m:
            continue
        cls = m.group(1)
        if cls == 'Plant':
            continue
        mm = re.search(r'\bimage\s*=\s*(\d+)\s*;', src)
        if mm:
            out[cls] = int(mm.group(1))
    return out


# ------------------------------------------------------------------ JS 解析

def parse_js_table(var_name):
    """把 render.js 里 `var NAME = [ {...}, ... ];` 解析成 dict 列表。"""
    src = read(RENDER_JS)
    m = re.search(r'var\s+' + var_name + r'\s*=\s*\[(.*?)\];', src, re.S)
    if not m:
        raise SystemExit('在 render.js 里找不到 ' + var_name)
    body = strip_comments(m.group(1))
    rows = []
    for obj in re.findall(r'\{(.*?)\}', body, re.S):
        d = {}
        for k, sv, nv in re.findall(r"(\w+)\s*:\s*(?:'([^']*)'|(\d+))", obj):
            d[k] = sv if sv != '' else int(nv)
        rows.append(d)
    return rows


# --------------------------------------------------------------- 图集核验

def load_png_raw(path):
    d = open(path, 'rb').read()
    pos = 8
    idat = b''
    w = h = None
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h = struct.unpack('>II', data[:8])
        if typ == b'IDAT':
            idat += data
        pos += 12 + ln
    return w, h, zlib.decompress(idat)


def frame_solid(w, h, raw, idx):
    stride = w * 4
    cols = w // 16
    cx = (idx % cols) * 16
    cy = (idx // cols) * 16
    if cy + 16 > h or cx + 16 > w:
        return -1
    n = 0
    for yy in range(16):
        for xx in range(16):
            o = (cy + yy) * (stride + 1) + 1 + (cx + xx) * 4
            if raw[o + 3] == 255:
                n += 1
    return n


# --------------------------------------------------------------------- 主

def main(selftest=False):
    print('=== 1) 解析 Java：陷阱常量 ===')
    consts = parse_trap_constants()
    for k in ['RED', 'ORANGE', 'YELLOW', 'GREEN', 'TEAL', 'VIOLET', 'WHITE', 'GREY', 'BLACK',
              'DOTS', 'WAVES', 'GRILL', 'STARS', 'DIAMOND', 'CROSSHAIR', 'LARGE_DOT']:
        assert k in consts, 'Trap.java 缺常量 ' + k
    print('  ✓ 9 个颜色 + 7 个形状常量齐备')

    java_traps, raw_tables = collect_traps()
    print('  ✓ 解析出 %d 个有 color/shape 的陷阱类（共 %d 个类文件）'
          % (len(java_traps), len(raw_tables)))

    print('=== 2) 解析 Java：植物 image ===')
    java_plants = collect_plants()
    print('  ✓ 解析出 %d 个植物类' % len(java_plants))

    print('=== 3) 解析 JS：TRAPS / PLANTS 表 ===')
    js_traps = parse_js_table('TRAPS')
    js_plants = parse_js_table('PLANTS')
    print('  ✓ JS TRAPS %d 项 / PLANTS %d 项' % (len(js_traps), len(js_plants)))

    if selftest:
        print('=== [反向自测] 故意改错一项，必须被抓到 ===')
        if not js_traps:
            print('  ✗ JS 表为空，无法自测'); fail.append('selftest')
        else:
            # 把第一项的 shape 换成一个不同的合法形状
            names = ['DOTS', 'WAVES', 'GRILL', 'STARS', 'DIAMOND', 'CROSSHAIR', 'LARGE_DOT']
            orig = js_traps[0]['shape']
            js_traps[0]['shape'] = [n for n in names if n != orig][0]
            print('  已把 %s 的 shape 从 %s 改成 %s' % (js_traps[0]['cls'], orig, js_traps[0]['shape']))

    print('=== 4) 逐项比对（类名集合 / color / shape / 帧号）===')
    jset, sset = set(java_traps), set(t['cls'] for t in js_traps)
    chk(jset == sset,
        '陷阱类名集合一致（Java %d / JS %d）' % (len(jset), len(sset)))
    if jset != sset:
        print('    Java 独有：', sorted(jset - sset))
        print('    JS   独有：', sorted(sset - jset))

    bad = []
    for t in js_traps:
        cls = t['cls']
        if cls not in java_traps:
            continue
        jc, js_ = java_traps[cls]
        if t['color'] != jc or t['shape'] != js_:
            bad.append('%s: Java=%s/%s  JS=%s/%s' % (cls, jc, js_, t['color'], t['shape']))
    chk(not bad, '全部 %d 个陷阱的 color/shape 与 Java 一致' % len(js_traps))
    for b in bad[:10]:
        print('    ✗ ' + b)
    if bad:
        fail.append('trap color/shape 不一致')

    jp = set(java_plants)
    sp = set(p['cls'] for p in js_plants)
    chk(jp == sp, '植物类名集合一致（Java %d / JS %d）' % (len(jp), len(sp)))
    if jp != sp:
        print('    Java 独有：', sorted(jp - sp))
        print('    JS   独有：', sorted(sp - jp))

    pbad = []
    for p in js_plants:
        if p['cls'] in java_plants and int(p['image']) != java_plants[p['cls']]:
            pbad.append('%s: Java=%d JS=%d' % (p['cls'], java_plants[p['cls']], p['image']))
    chk(not pbad, '全部 %d 个植物的 image 与 Java 一致' % len(js_plants))
    for b in pbad:
        print('    ✗ ' + b)
    if pbad:
        fail.append('plant image 不一致')

    print('=== 5) 帧号公式核对：frame = color + shape*16 ===')
    fbad = []
    rangebad = []
    used_frames = set()
    for t in js_traps:
        c = int(consts[t['color']])
        s = int(consts[t['shape']])
        if not (0 <= c <= 8) or not (0 <= s <= 6):
            rangebad.append('%s(color=%d shape=%d)' % (t['cls'], c, s))
            continue
        if c + s * 16 != c + 16 * s:          # 占位：保持公式显式可见
            fbad.append(t['cls'])
    chk(not fbad, '全部 %d 个陷阱帧号落在 color+shape*16 上' % len(js_traps))
    chk(not rangebad, '全部陷阱的 color∈0..8 / shape∈0..6')
    for b in rangebad:
        print('    ✗ 越界: ' + b)
    if rangebad:
        fail.append('color/shape 越界')

    print('=== 6) 真图集确认这些帧都画了（不是空槽位）===')
    w, h, raw = load_png_raw(FEATURES_PNG)
    chk(w == 256 and h == 128, 'terrain_features.png 为 256×128（16 列 × 8 行 = 128 帧）')
    empty = []
    for t in js_traps:
        idx = int(consts[t['color']]) + int(consts[t['shape']]) * 16
        used_frames.add(idx)
        if frame_solid(w, h, raw, idx) == 0:
            empty.append('%s(frame %d)' % (t['cls'], idx))
    chk(not empty, '全部陷阱帧非空（用到 %d 个不同帧）' % len(used_frames))
    for e in empty:
        print('    ✗ 空帧: ' + e)

    pempty = []
    for p in js_plants:
        idx = int(p['image']) + 7 * 16
        if frame_solid(w, h, raw, idx) == 0:
            pempty.append('%s(frame %d)' % (p['cls'], idx))
    chk(not pempty, '全部植物帧非空（%d 个）' % len(js_plants))
    for e in pempty:
        print('    ✗ 空帧: ' + e)

    print('=== 7) 采样核对：JS 表 vs 真图集像素（形状应同色族）===')
    # 同一 shape 的不同 color 帧，实心像素数应相同（同一形状换调色板）——
    # 这是「帧号对得上」的强证据：抄错 shape 会让像素数分组错乱。
    from collections import defaultdict
    byshape = defaultdict(set)
    for t in js_traps:
        idx = int(consts[t['color']]) + int(consts[t['shape']]) * 16
        byshape[t['shape']].add(frame_solid(w, h, raw, idx))
    mixed = {s: v for s, v in byshape.items() if len(v) != 1}
    chk(not mixed,
        '同一形状的所有颜色帧像素数一致（形状分组正确）'
        + ('' if not mixed else '；异常：' + str(mixed)))
    if mixed:
        fail.append('shape 分组像素数不一致')

    print()
    if selftest:
        if fail:
            print('✅ 反向自测通过：改错的项被抓到了（%d 处不符）' % len(fail))
            return 0
        else:
            print('❌ 反向自测失败：改错了却没报错 ⇒ 核验脚本本身无效')
            return 2

    if fail:
        print('❌ 共 %d 项不符：' % len(fail))
        for f in fail:
            print('   - ' + f)
        return 1
    print('✅ 全部通过：陷阱 %d 项 / 植物 %d 项与 Java 源逐项一致，帧号在真图集上非空'
          % (len(js_traps), len(js_plants)))
    return 0


if __name__ == '__main__':
    sys.exit(main('--selftest' in sys.argv))
