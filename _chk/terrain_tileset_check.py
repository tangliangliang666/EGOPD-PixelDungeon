# -*- coding: utf-8 -*-
"""新地形图集自检工具：按「地形角色」核验每个 16x16 帧画得对不对（供 docs/terrain-creation-guide.md 配套使用）。

用法:
    python _chk/terrain_tileset_check.py <图集.png> [<角色>:<帧号或 列,行> ...]

不带帧参数时，会对整张图集做一次总览（每帧有效行 / 不透明像素数），并列出空白帧。

角色（决定核验规则）:
    ground    地面/草皮：必须满格不透明(256px)，否则草皮下会漏出背景
    raised    抬高地块本体：必须满格不透明
    overhang  草叶上段：画在「上一格」，有效像素应从下往上；若有效行落在 0..7 会跑到更上面一格
    underhang 草叶下段/前景：画在「本格」，有效像素应集中在上半(0..7 常规)
    flat      仅 examine 小图：整格不透明、无 3D 效果
    any       不做规则断言，只报告

示例:
    python _chk/terrain_tileset_check.py core/src/main/assets/environment/my_tiles.png \
        ground:122 overhang:234 underhang:250
"""
import sys, os, zlib, struct

W = 16


def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not a png: ' + path
    pos, idat, plte, trns = 8, b'', b'', b''
    w = h = bitd = ctype = interlace = 0
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, bitd, ctype, _, _, interlace = struct.unpack('>IIBBBBB', data)
        elif typ == b'PLTE':
            plte = data
        elif typ == b'tRNS':
            trns = data
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
        pos += 12 + ln
    assert bitd == 8 and interlace == 0, '仅支持 8bit 非隔行 PNG: bitd=%s interlace=%s' % (bitd, interlace)
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(nch, stride): line[i] = (line[i] + line[i - nch]) & 255
        elif f == 2:
            for i in range(stride): line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    # 统一成 RGBA
    rgba = bytearray(w * h * 4)
    if ctype == 6:
        rgba = out
    else:
        for i in range(w * h):
            if ctype == 3:
                idx = out[i]
                r, g, b = plte[idx * 3], plte[idx * 3 + 1], plte[idx * 3 + 2]
                a = trns[idx] if idx < len(trns) else 255
            elif ctype == 2:
                r, g, b, a = out[i * 3], out[i * 3 + 1], out[i * 3 + 2], 255
            elif ctype == 0:
                r = g = b = out[i]; a = 255
            else:  # 4 = 灰度+alpha
                r = g = b = out[i * 2]; a = out[i * 2 + 1]
            rgba[i * 4:i * 4 + 4] = bytes((r, g, b, a))
    return w, h, rgba


class Sheet:
    def __init__(self, path):
        self.path = path
        self.w, self.h, self.buf = load_png(path)
        self.cols, self.rows = self.w // W, self.h // W

    def frame(self, idx):
        cx, cy = idx % self.cols, idx // self.cols
        f = []
        for y in range(W):
            row = []
            for x in range(W):
                o = ((cy * W + y) * self.w + cx * W + x) * 4
                row.append(tuple(self.buf[o:o + 4]))
            f.append(row)
        return f

    def frames(self):
        return self.cols * self.rows


def analyze(f):
    counts, alphas = [], []
    for row in f:
        counts.append(sum(1 for p in row if p[3] > 0))
        alphas.append(max([p[3] for p in row] + [0]))
    used = [y for y, n in enumerate(counts) if n]
    return counts, alphas, used


def ascii_art(f):
    out = []
    for row in f:
        s = ''
        for p in row:
            a = p[3]
            s += ' ' if a == 0 else ('.' if a < 128 else ('*' if a < 255 else '#'))
        out.append(s)
    return out


def parse_frame(tok, sheet):
    if ',' in tok:
        c, r = tok.split(',')
        c, r = int(c), int(r)
        if not (1 <= c <= sheet.cols and 1 <= r <= sheet.rows):
            raise ValueError('xy(%d,%d) 超出图集 %dx%d 帧网格' % (c, r, sheet.cols, sheet.rows))
        return (r - 1) * W + (c - 1), 'xy(%d,%d)' % (c, r)
    idx = int(tok)
    if not (0 <= idx < sheet.frames()):
        raise ValueError('帧号 %d 超出图集范围 0..%d' % (idx, sheet.frames() - 1))
    return idx, 'xy(%d,%d)' % (idx % sheet.cols + 1, idx // sheet.cols + 1)


RULE_TEXT = {
    'ground':    '地面/草皮 —— 必须满格 256px 不透明',
    'raised':    '抬高地块本体 —— 必须满格 256px 不透明',
    'flat':      'examine 小图 —— 应满格 256px 不透明',
    'overhang':  '草叶上段 —— 画在上一格，有效行应偏下；整帧有效行落在 0..7 会窜到更高一格',
    'underhang': '草叶下段/前景 —— 画在本格，有效像素集中在 0..10 行',
    'any':       '不做规则断言',
}


def check(name, role, sheet, idx, label, verbose=True):
    f = sheet.frame(idx)
    counts, alphas, used = analyze(f)
    total = sum(counts)
    lo, hi = (min(used), max(used)) if used else (None, None)
    warn = []

    if role in ('ground', 'raised', 'flat'):
        if total != 256:
            warn.append('要求满格 256px 不透明，实际 %d px（草皮下会漏出背景）' % total)
        if any(a < 255 for a in alphas):
            warn.append('存在半透明像素（最大 alpha %d），地面层不应半透明' % max(alphas))
    elif role == 'overhang':
        if not used:
            warn.append('整帧空白：该草格的草叶上段不会显示')
        else:
            if hi is not None and hi < 8:
                warn.append('有效行 %d..%d 全在帧的上半 —— 画在上一格时会窜到再上一格，'
                            '通常应让有效行落进 8..15' % (lo, hi))
            if lo is not None and lo < 8 and hi is not None and hi >= 8:
                warn.append('有效行跨了 8 这条中线（%d..%d）：8..15 段贴在草地格顶边上方，'
                            '0..7 段会跑到上一格的中上部' % (lo, hi))
            if max(alphas) == 255 and total == 256:
                warn.append('整帧满格不透明：会完全盖住上一格，草叶上段一般应留大量透明')
    elif role == 'underhang':
        if not used:
            warn.append('整帧空白：草叶前景层不会显示')
        elif lo is not None and lo >= 8:
            warn.append('有效行从 %d 才开始（>=8）：草地格顶部 %d 行没有草叶，'
                        '角色上半身会露在草外' % (lo, lo))
    elif role == 'any':
        pass
    else:
        warn.append('未知角色 "%s"（见文件头说明）' % role)

    status = 'OK ' if not warn else 'WARN'
    print('[%s] %-24s %-10s 帧%-4d %s  有效行 %s..%s  不透明 %3d px  alpha<=%s'
          % (status, name, label, idx, role, lo if lo is not None else '-',
             hi if hi is not None else '-', total, max(alphas) if alphas else 0))
    print('       规则: %s' % RULE_TEXT.get(role, '?'))
    print('       逐行不透明: ' + ' '.join('%2d' % n for n in counts))
    if verbose:
        for line in ascii_art(f):
            print('       |%s|' % line)
    for w in warn:
        print('       !! ' + w)
    return not warn


def main():
    if len(sys.argv) < 2:
        print(__doc__)
        return 1
    path = sys.argv[1]
    sheet = Sheet(path)
    print('图集 %s' % path)
    print('尺寸 %dx%d  帧网格 %dx%d  共 %d 帧  每帧 16x16' % (sheet.w, sheet.h, sheet.cols, sheet.rows, sheet.frames()))
    if sheet.w % W or sheet.h % W:
        print('!! 尺寸不是 16 的整数倍：TextureFilm(tex,16,16) 会按整除算列数，'
              '右/下边缘的帧会被丢弃或错位（列数=%d 行数=%d）' % (sheet.w // W, sheet.h // W))
    print()

    specs = sys.argv[2:]
    if not specs:
        print('未指定帧，输出总览：')
        blanks, nonblank = [], 0
        for idx in range(sheet.frames()):
            counts, alphas, used = analyze(sheet.frame(idx))
            if not used:
                blanks.append(idx)
            else:
                nonblank += 1
        print('  非空帧 %d 个，空白帧 %d 个' % (nonblank, len(blanks)))
        if blanks:
            print('  空白帧号: ' + ', '.join(str(b) for b in blanks))
            print('  空白帧的图集坐标(x,y 1基): ' + ', '.join(
                '(%d,%d)' % (b % sheet.cols + 1, b // sheet.cols + 1) for b in blanks))
        return 0

    ok = True
    for spec in specs:
        if ':' not in spec:
            print('!! 参数格式应为 <角色>:<帧号|列,行>，收到 "%s"' % spec)
            ok = False
            continue
        role, tok = spec.split(':', 1)
        idx, label = parse_frame(tok, sheet)
        if not check(path.split(os.sep)[-1], role, sheet, idx, label):
            ok = False
    print()
    print('结论: %s' % ('全部通过' if ok else '存在需要修正的帧（见上面 !!)'))
    return 0 if ok else 2


if __name__ == '__main__':
    sys.exit(main())
