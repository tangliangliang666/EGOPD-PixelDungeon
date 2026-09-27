# -*- coding: utf-8 -*-
"""按**实机绘制口径**渲染新版考验界面（生命之树）。

关键：所有几何**直接从 WndTrials.java / Trials.java 里解析**（A、ICON_SCALE、WIDTH、
NODE_DX/NODE_DY、EDGES、ICON_W/H…），所以这张图与代码不可能漂移。

渲染口径（与游戏逐项对应）：
  · 窗口边框 = 真取 interfaces/chrome.png 的九宫格(0,0,20,20,margin 6)；
  · 勾选框 = chrome.png 的 RED_BUTTON(38,0,6,6,margin 2) + icons.png 的 (48,32,12,12)/(64,32,12,12)；
  · 图标 = tree.png 里边那帧的**原生** 13~16px 取样矩形，NEAREST 缩放到显示尺寸 ×3
    （＝游戏里 `Image.scale` + 相机整数 zoom 做的事，**贴图不重采样**）；
  · 路径 = 与 Pixmap 同一套算法（端点按 LINE_TRIM 退让、整数取整、35% 黑）；
  · 文字为近似排版（真机是像素字体），字号按逻辑像素 ×3 取。
"""
import math
import os
import re
import sys

from PIL import Image, ImageDraw

import render_tree_draft as R          # 借 load_font

BASE = r'D:/PD'
PKG = os.path.join(BASE, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
WND_JAVA = os.path.join(PKG, 'windows/WndTrials.java')
TRI_JAVA = os.path.join(PKG, 'Trials.java')
CHAL_JAVA = os.path.join(PKG, 'windows/WndChallenges.java')
CHALLENGES_JAVA = os.path.join(PKG, 'Challenges.java')
ASSETS = os.path.join(BASE, 'core/src/main/assets/interfaces')
OUT = os.path.join(BASE, '_chk/_trials_ingame.png')
ZH = os.path.join(BASE, 'core/src/main/assets/messages/misc/misc_zh.properties')

VIEW = 3          # 预览放大倍数（＝游戏里的整数 zoom）
CHROME_M = 6      # Chrome.Type.WINDOW 的 margin
NAMES = ['keter', 'hokma', 'binah', 'chesed', 'gebura',
         'tiphereth', 'netzach', 'hod', 'yesod', 'malkuth']
TITLE_COLOR = (0xFF, 0xFF, 0x44)     # Window.TITLE_COLOR
TEXT_COLOR = (0xFF, 0xFF, 0xFF)      # 文本默认色（白）


# ── 解析 Java 常量 ────────────────────────────────────────────────────────
def _read(p):
    return open(p, encoding='utf-8').read()


def _num(src, name, default=None):
    m = re.search(r'\b' + name + r'\s*=\s*(-?[\d.]+)f?\s*;', src)
    return float(m.group(1)) if m else default


def _int(src, name):
    m = re.search(r'\b' + name + r'\s*=\s*(-?\d+)\s*;', src)
    return int(m.group(1)) if m else None


def _iarr(src, name):
    m = re.search(r'\b' + name + r'\s*=\s*\{([^}]*)\}', src)
    return [int(x) for x in re.findall(r'-?\d+', m.group(1))] if m else None


def _farr(src, name, env):
    m = re.search(r'\b' + name + r'\s*=\s*\{([^}]*)\}', src)
    out = []
    for tok in m.group(1).split(','):
        tok = tok.strip().replace('f', '')
        if tok:
            out.append(eval(tok, {'__builtins__': {}}, env))
    return out


def _pairs(src, name):
    m = re.search(r'\b' + name + r'\s*=\s*\{(.*?)\}\s*;', src, re.S)
    return [[int(a), int(b)] for a, b in re.findall(r'\{\s*(\d+)\s*,\s*(\d+)\s*\}', m.group(1))]


def load_spec():
    wnd, tri = _read(WND_JAVA), _read(TRI_JAVA)
    A = _num(wnd, 'A')
    # 「对齐原版挑战窗」这条要求的参照物：从 WndChallenges.java + Challenges.NAME_IDS 现算，
    # 不抄死数字（挑战条数会变，抄了就过期）。
    chal, chj = _read(CHAL_JAVA), _read(CHALLENGES_JAVA)
    mch = re.search(r'NAME_IDS\s*=\s*\{(.*?)\};', chj, re.S)
    n_ch = len(re.findall(r'"[^"]+"', mch.group(1)))
    chal_w = _int(chal, 'WIDTH')
    chal_h = (_int(chal, 'TTL_HEIGHT') + n_ch * _int(chal, 'BTN_HEIGHT')
              + (n_ch - 1) * _int(chal, 'GAP'))
    H = A * math.sqrt(3) / 2
    spec = dict(
        WIDTH=_int(wnd, 'WIDTH'), TTL=_int(wnd, 'TTL_HEIGHT'), PAD=_int(wnd, 'PAD'),
        A=A, H=H,
        ICON_SCALE=_num(wnd, 'ICON_SCALE'), LINE_ALPHA=_num(wnd, 'LINE_ALPHA'),
        LINE_GAP=_num(wnd, 'LINE_GAP'), TOUCH=_num(wnd, 'TOUCH'),
        NODE_DX=_farr(wnd, 'NODE_DX', {'A': A, 'H': H}),
        NODE_DY=_farr(wnd, 'NODE_DY', {'A': A, 'H': H}),
        EDGES=_pairs(wnd, 'EDGES'),
        ICON_W=_iarr(tri, 'ICON_W'), ICON_H=_iarr(tri, 'ICON_H'),
        ICON_FRAME=_int(tri, 'ICON_FRAME'), ICON_COLS=_int(tri, 'ICON_COLS'),
        ROW_ENABLED=_int(tri, 'ICON_ROW_ENABLED'),
        ROW_LOCKED=_int(tri, 'ICON_ROW_LOCKED'),
        ROW_READY=_int(tri, 'ICON_ROW_READY'),
        CHAL=(chal_w, chal_h),
    )
    return spec


def _jround(v):
    """Java 的 Math.round(float)：floor(v + 0.5)。

    ⚠️ 别用 Python 的 round()（银行家舍入）：15 × 0.70 = 10.5，Python 给 10、Java 给 11，
       预览就会和实机差一像素 —— 而本文件存在的全部意义就是「和实机一致」。
       （1.40 这档恰好没有 .5 的case，但 0.70 的对照面板有。）
    """
    return int(math.floor(v + 0.5))


def geometry(spec, icon_scale=None, A=None):
    """按（可能覆盖的）ICON_SCALE / A 算出显示尺寸与外包框。

    A 可覆盖是为了画「上一版 a = 70」的对照：源码里 NODE_DX 是 H 的倍数、NODE_DY 是 A 的倍数，
    这里按倍数重算，改 A 时 H 与全部质点坐标会一起走（否则对照面板的树会散架）。
    """
    s = spec['ICON_SCALE'] if icon_scale is None else icon_scale
    a = spec['A'] if A is None else A
    h = a * math.sqrt(3) / 2
    dw = [max(1, _jround(w * s)) for w in spec['ICON_W']]
    dh = [max(1, _jround(v * s)) for v in spec['ICON_H']]
    g = dict(spec)
    g['SCALE'] = s
    g['A'], g['H'] = a, h
    g['NODE_DX'] = [d / spec['H'] * h for d in spec['NODE_DX']]
    g['NODE_DY'] = [d / spec['A'] * a for d in spec['NODE_DY']]
    g['DW'], g['DH'] = dw, dh
    g['TREE_W'] = 2 * h + max(dw)
    g['TREE_H'] = 4 * a + max(dh)
    g['TRIM'] = max(max(dw), max(dh)) / 2.0 + spec['LINE_GAP']
    g['CONTENT_H'] = int(spec['TTL'] + spec['PAD'] + g['TREE_H'] + spec['PAD'])
    return g


def node_pos(g, i):
    return (g['TREE_W'] / 2.0 + g['NODE_DX'][i],
            (g['TREE_H'] - 4 * g['A']) / 2.0 + g['NODE_DY'][i])


# ── 九宫格 ────────────────────────────────────────────────────────────────
def blit_patch(canvas, src, rect, margin, dst, size, z=1):
    """九宫格。⚠️ 源矩形是**贴图原始像素**（边距不乘 z），落点边距才乘 z。"""
    rx, ry, rw, rh = rect
    ms, md = margin, margin * z      # ms = 贴图边距；md = 屏幕边距（乘放大倍数）
    dx, dy = dst
    dw, dh = size

    def blit(sub, x, y, w, h):
        if w <= 0 or h <= 0:
            return
        img = src.crop((sub[0], sub[1], sub[0] + sub[2], sub[1] + sub[3]))
        if img.size != (w, h):
            img = img.resize((w, h), Image.NEAREST)
        canvas.alpha_composite(img, (int(x), int(y)))

    blit((rx, ry, ms, ms), dx, dy, md, md)
    blit((rx + rw - ms, ry, ms, ms), dx + dw - md, dy, md, md)
    blit((rx, ry + rh - ms, ms, ms), dx, dy + dh - md, md, md)
    blit((rx + rw - ms, ry + rh - ms, ms, ms), dx + dw - md, dy + dh - md, md, md)
    blit((rx + ms, ry, rw - 2 * ms, ms), dx + md, dy, dw - 2 * md, md)
    blit((rx + ms, ry + rh - ms, rw - 2 * ms, ms), dx + md, dy + dh - md, dw - 2 * md, md)
    blit((rx, ry + ms, ms, rh - 2 * ms), dx, dy + md, md, dh - 2 * md)
    blit((rx + rw - ms, ry + ms, ms, rh - 2 * ms), dx + dw - md, dy + md, md, dh - 2 * md)
    blit((rx + ms, ry + ms, rw - 2 * ms, rh - 2 * ms),
         dx + md, dy + md, dw - 2 * md, dh - 2 * md)


# ── 富文本（`_高亮_`）────────────────────────────────────────────────────
def draw_rich(d, xy, text, font, max_w, normal=TEXT_COLOR, hl=TITLE_COLOR):
    x0, y0 = xy
    lh = int(round(font.size * 1.5))
    y = y0
    for para in text.split('\n'):
        if not para.strip():
            y += lh // 2
            continue
        toks = []
        for k, seg in enumerate(para.split('_')):
            toks += [(ch, k % 2 == 1) for ch in seg]
        line, w = '', 0
        for ch, is_hl in toks:
            cw = font.getlength(ch)
            if w + cw > max_w and line:
                d.text((x0, y), line, font=font, fill=hl if prev_hl else normal)
                y += lh
                line, w = '', 0
            if not line:
                prev_hl = is_hl
            line += ch
            w += cw
        if line:
            d.text((x0, y), line, font=font, fill=hl if prev_hl else normal)
            y += lh
    return y


# ── 树窗口 ────────────────────────────────────────────────────────────────
def tree_window(g, rows, tree, chrome, title='考验'):
    """rows: 10 个质点各用哪一行图标；返回 VIEW 倍图。"""
    z = VIEW
    W = (g['WIDTH'] + 2 * CHROME_M) * z
    H = (g['CONTENT_H'] + 2 * CHROME_M) * z
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    blit_patch(img, chrome, (0, 0, 20, 20), CHROME_M, (0, 0), (W, H), z)

    ox = round((g['WIDTH'] - g['TREE_W']) / 2.0 * z) / z
    oy = round((g['TTL'] + g['PAD']) * z) / z
    base = (CHROME_M * z, CHROME_M * z)

    d = ImageDraw.Draw(img, 'RGBA')
    for a, b in g['EDGES']:
        ax, ay = node_pos(g, a)
        bx, by = node_pos(g, b)
        dx, dy = bx - ax, by - ay
        L = math.hypot(dx, dy)
        dx, dy = dx / L, dy / L
        d.line([(base[0] + (ox + ax + dx * g['TRIM']) * z, base[1] + (oy + ay + dy * g['TRIM']) * z),
                (base[0] + (ox + bx - dx * g['TRIM']) * z, base[1] + (oy + by - dy * g['TRIM']) * z)],
               fill=(0, 0, 0, int(round(g['LINE_ALPHA'] * 255))), width=z)

    for i in range(10):
        row = rows[i]
        art = tree.crop((i * 16, row * 16, i * 16 + g['ICON_W'][i], row * 16 + g['ICON_H'][i]))
        art = art.resize((g['DW'][i] * z, g['DH'][i] * z), Image.NEAREST)
        cx, cy = node_pos(g, i)
        px = round((ox + cx - g['DW'][i] / 2.0) * z)
        py = round((oy + cy - g['DH'][i] / 2.0) * z)
        img.alpha_composite(art, (base[0] + px, base[1] + py))

    f = R.load_font(int(round(12 * z)))
    d.text((W / 2, base[1] + g['TTL'] * z / 2), title, font=f, fill=TITLE_COLOR, anchor='mm')
    return img


# ── 详情小窗（WndTrialInfo）───────────────────────────────────────────────
def info_window(g, tree, chrome, icons_png, desc, name, row, checked):
    z = VIEW
    WIDTH_MIN = 120          # WndTitledMessage.WIDTH_MIN
    TTL_H = 16               # IconTitle 的高度
    GAP = 2
    f_desc = R.load_font(int(round(6 * z)))
    lh = int(round(f_desc.size * 1.5))

    # 先量描述文本的高度（与 draw_rich 同步）
    lines = 0
    for para in desc.split('\n'):
        if not para.strip():
            lines += 0.5
            continue
        w = 0
        n = 1
        for ch in para.replace('_', ''):
            cw = f_desc.getlength(ch)
            if w + cw > WIDTH_MIN * z and w:
                n += 1
                w = 0
            w += cw
        lines += n
    desc_h = int(lines * lh)

    content_h = TTL_H + 2 * GAP + desc_h + 2 * GAP + 16     # 描述 + 开关（16 高）
    W = (WIDTH_MIN + 2 * CHROME_M) * z
    H = (content_h + 2 * CHROME_M) * z
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    blit_patch(img, chrome, (0, 0, 20, 20), CHROME_M, (0, 0), (W, H), z)
    base = (CHROME_M * z, CHROME_M * z)

    # 左上角图标（原生 1:1）
    art = tree.crop((NAMES.index(name) * 16, row * 16,
                     NAMES.index(name) * 16 + g['ICON_W'][NAMES.index(name)],
                     row * 16 + g['ICON_H'][NAMES.index(name)]))
    art = art.resize((art.width * z, art.height * z), Image.NEAREST)
    ix = int(8 - art.width / 2.0) if art.width <= 16 * z else 0
    img.alpha_composite(art, (base[0] + max(0, int(round(8 * z - art.width / 2.0))), base[1]))

    d = ImageDraw.Draw(img, 'RGBA')
    f_name = R.load_font(int(round(9 * z)))
    nx = base[0] + max(16, g['ICON_W'][NAMES.index(name)]) * z + GAP * z
    d.text((nx, base[1] + TTL_H * z / 2), name.upper(), font=f_name, fill=TITLE_COLOR, anchor='lm')
    draw_rich(d, (base[0], base[1] + (TTL_H + 2 * GAP) * z), desc, f_desc, WIDTH_MIN * z)

    # 开关（CheckBox：RED_BUTTON 底 + 左对齐文字 + 右侧 icons.png 那一格）
    by = base[1] + int(round((TTL_H + 2 * GAP + desc_h / float(z) + 2) * z))
    blit_patch(img, chrome, (38, 0, 6, 6), 2, (base[0], by), (WIDTH_MIN * z, 16 * z), z)
    f_btn = R.load_font(int(round(9 * z)))
    d.text((base[0] + int(round(2 * z)), by + 8 * z), '开启该考验', font=f_btn, anchor='lm',
           fill=(0x3F, 0x30, 0x26))
    cb = icons_png.crop((64 if checked else 48, 32, (64 if checked else 48) + 12, 44))
    cb = cb.resize((12 * z, 12 * z), Image.NEAREST)
    img.alpha_composite(cb, (base[0] + WIDTH_MIN * z - int(round(6 * z)), by + int(round(2 * z))))
    return img


# ── 组装 ──────────────────────────────────────────────────────────────────
def label(d, x, y, text, font, fill=(0x22, 0x22, 0x22)):
    d.text((x, y), text, font=font, fill=fill, anchor='la')


def main():
    spec = load_spec()
    g = geometry(spec)                  # 本版：a = 47.5 ⇒ 内容 120 x 236（与挑战窗同规格）
    # 对照口径：**上一版**（用户 2026-09-25 反馈「占据整个屏幕、上下沿都看不到」的那一版）
    g_old = geometry(spec, A=70.0)
    g_old['WIDTH'] = 148
    tree = Image.open(os.path.join(ASSETS, 'tree.png')).convert('RGBA')
    chrome = Image.open(os.path.join(ASSETS, 'chrome.png')).convert('RGBA')
    icons = Image.open(os.path.join(ASSETS, 'icons.png')).convert('RGBA')

    desc = ''
    for line in _read(ZH).splitlines():
        if line.startswith('trials.chesed_desc='):
            desc = line.split('=', 1)[1].replace('\\n', '\n')
    assert desc, '取不到 trials.chesed_desc'

    R_READY, R_LOCK, R_ON = spec['ROW_READY'], spec['ROW_LOCKED'], spec['ROW_ENABLED']

    # ① 初始态：全部「已解锁未开启」
    pA = tree_window(g, [R_READY] * 10, tree, chrome)
    # ② 开启 CHESED / GEBURA 之后（彩色行）
    rows_on = [R_ON if i in (3, 4) else R_READY for i in range(10)]
    pB = tree_window(g, rows_on, tree, chrome)
    # ③ 对照：上一版口径（a = 70、内容宽 148、内容高 326）—— 同一个 zoom，可直接比大小
    pC = tree_window(g_old, rows_on, tree, chrome)
    # ④ 详情小窗
    pD = info_window(g, tree, chrome, icons, desc, 'chesed', R_ON, True)

    # ⑤ 尺寸 1:1 对照条：挑战窗 / 本版 / 上一版。横轴 = 内容宽、纵轴 = 内容高，1 单位 = 1 像素。
    chal_w, chal_h = spec['CHAL']
    bars = [('原版挑战窗', chal_w, chal_h, (0x7A, 0x7A, 0x7A)),
            ('本版考验窗', g['WIDTH'], g['CONTENT_H'], (0x2E, 0x7D, 0x32)),
            ('上一版', g_old['WIDTH'], g_old['CONTENT_H'], (0xC6, 0x28, 0x28))]
    pad = 34
    maxh = max(b[2] for b in bars)
    top_y = 26                       # 给「宽×高」标注留出的顶空
    bw = sum(b[1] for b in bars) + pad * (len(bars) + 1)
    pE = Image.new('RGBA', (bw, top_y + maxh + 58), (0xFA, 0xF8, 0xF2, 255))
    de = ImageDraw.Draw(pE, 'RGBA')
    fe = R.load_font(12)
    x = pad
    for lab, w, h, col in bars:
        y0 = top_y + (maxh - h)
        de.rectangle([x, y0, x + w, top_y + maxh], fill=col + (0x3C,), outline=col + (0xFF,))
        de.text((x + w / 2, y0 - 16), '%dx%d' % (w, h), font=fe,
                fill=(0x33, 0x33, 0x33), anchor='ma')
        de.text((x + w / 2, top_y + maxh + 6), lab, font=fe,
                fill=(0x33, 0x33, 0x33), anchor='ma')
        x += w + pad
    de.text((bw / 2, top_y + maxh + 30),
            '内容尺寸 1:1 —— 本版 236 与挑战窗完全一致；上一版 326，高 90 ⇒ 上下各顶出 45',
            font=fe, fill=(0x55, 0x55, 0x55), anchor='ma')

    # ── 拼总图 ────────────────────────────────────────────────────────────
    gap = 26
    top = 56
    fh = R.load_font(15)
    fl = R.load_font(13)

    # ⚠️ 每块都要经 place() 登记，重叠会被断言当场拦下
    placed = []

    def place(canvas, img, x, y, cap):
        for (ox, oy, ow, oh, ocap) in placed:
            if x < ox + ow and ox < x + img.width and y < oy + oh and oy < y + img.height:
                raise AssertionError('面板重叠：%s 与 %s' % (cap, ocap))
        placed.append((x, y, img.width, img.height, cap))
        canvas.alpha_composite(img, (x, y))
        d.text((x, y - 20), cap, font=fl, fill=(0x33, 0x33, 0x33))

    col1 = gap
    col2 = col1 + pA.width + gap
    col3 = col2 + pB.width + gap
    row1 = top
    row2 = row1 + max(pA.height, pB.height, pC.height) + gap

    W = max(col3 + pC.width, col2 + pE.width) + gap
    H = row2 + max(pD.height, pE.height) + 40
    out = Image.new('RGBA', (W, H), (0xF2, 0xEF, 0xE7, 255))
    d = ImageDraw.Draw(out)
    d.text((W / 2, 20), '考验界面 · 生命之树 -- 窗口尺寸对齐原版挑战窗（实机口径：几何取自 Java 常量）',
           font=fh, fill=(0, 0, 0), anchor='mm')

    place(out, pA, col1, row1, '① 本版 · 初始态（十个质点皆「已解锁未开启」）· 内容 120x236')
    place(out, pB, col2, row1, '② 本版 · 开启 CHESED 与 GEBURA 后')
    place(out, pC, col3, row1, '③ 对照 · 上一版（a=70 / 宽 148 / 内容 148x326）')
    place(out, pD, col1, row2, '④ 点质点 → 详情小窗（WndTrialInfo）')
    place(out, pE, col2, row2, '⑤ 与挑战窗同规格：内容尺寸 1:1 对照')
    out.convert('RGB').save(OUT)

    print('[OK] %s  (%dx%d)' % (OUT, W, H))
    for tag, gg in (('本版', g), ('上一版', g_old)):
        print('     %s：a=%.1f 图标显示=%s 树=%.2fx%.2f 内容=%dx%d（左右各留 %.2f）'
              % (tag, gg['A'], ['%dx%d' % (gg['DW'][i], gg['DH'][i]) for i in range(10)],
                 gg['TREE_W'], gg['TREE_H'], gg['WIDTH'], gg['CONTENT_H'],
                 (gg['WIDTH'] - gg['TREE_W']) / 2))
    print('     挑战窗内容 = %dx%d → 本版与之逐像素同规格：%s'
          % (chal_w, chal_h, (g['WIDTH'], g['CONTENT_H']) == (chal_w, chal_h)))

if __name__ == '__main__':
    sys.exit(main())
