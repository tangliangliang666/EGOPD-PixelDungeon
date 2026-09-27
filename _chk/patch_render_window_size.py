# -*- coding: utf-8 -*-
"""
预览脚本改造：从「图标翻倍」主题切到「窗口尺寸对齐挑战窗」主题。

改三处：
  · geometry() 支持 A 覆盖（上一版 a = 70 要做对照，节点偏移按 A/H 的倍数重算）；
  · load_spec() 顺带读出 WndChallenges 的**真实内容尺寸**（判据用现算，不抄死数字）；
  · main() 整段重排：① / ② 本版、③ 上一版（同 zoom，可比大小）、④ 详情窗、
    ⑤ 与挑战窗 1:1 的尺寸对照条。

幂等；每条替换断言恰好命中一次。
"""
import sys

P = '_chk/render_trials_ingame.py'
raw = open(P, 'rb').read()
EOL = '\r\n' if b'\r\n' in raw else '\n'
TXT = raw.decode('utf-8')

NEW_MAIN = EOL.join([
    'def main():',
    '    spec = load_spec()',
    "    g = geometry(spec)                  # 本版：a = 47.5 ⇒ 内容 120 x 236（与挑战窗同规格）",
    '    # 对照口径：**上一版**（用户 2026-09-25 反馈「占据整个屏幕、上下沿都看不到」的那一版）',
    '    g_old = geometry(spec, A=70.0)',
    "    g_old['WIDTH'] = 148",
    "    tree = Image.open(os.path.join(ASSETS, 'tree.png')).convert('RGBA')",
    "    chrome = Image.open(os.path.join(ASSETS, 'chrome.png')).convert('RGBA')",
    "    icons = Image.open(os.path.join(ASSETS, 'icons.png')).convert('RGBA')",
    '',
    "    desc = ''",
    '    for line in _read(ZH).splitlines():',
    "        if line.startswith('trials.chesed_desc='):",
    "            desc = line.split('=', 1)[1].replace('\\\\n', '\\n')",
    "    assert desc, '取不到 trials.chesed_desc'",
    '',
    "    R_READY, R_LOCK, R_ON = spec['ROW_READY'], spec['ROW_LOCKED'], spec['ROW_ENABLED']",
    '',
    '    # ① 初始态：全部「已解锁未开启」',
    '    pA = tree_window(g, [R_READY] * 10, tree, chrome)',
    '    # ② 开启 CHESED / GEBURA 之后（彩色行）',
    '    rows_on = [R_ON if i in (3, 4) else R_READY for i in range(10)]',
    '    pB = tree_window(g, rows_on, tree, chrome)',
    '    # ③ 对照：上一版口径（a = 70、内容宽 148、内容高 326）—— 同一个 zoom，可直接比大小',
    '    pC = tree_window(g_old, rows_on, tree, chrome)',
    '    # ④ 详情小窗',
    "    pD = info_window(g, tree, chrome, icons, desc, 'chesed', R_ON, True)",
    '',
    '    # ⑤ 尺寸 1:1 对照条：挑战窗 / 本版 / 上一版。横轴 = 内容宽、纵轴 = 内容高，1 单位 = 1 像素。',
    "    chal_w, chal_h = spec['CHAL']",
    '    bars = [(%s原版挑战窗%s, chal_w, chal_h, (0x7A, 0x7A, 0x7A)),' % ("'", "'"),
    "            ('本版考验窗', g['WIDTH'], g['CONTENT_H'], (0x2E, 0x7D, 0x32)),",
    "            ('上一版', g_old['WIDTH'], g_old['CONTENT_H'], (0xC6, 0x28, 0x28))]",
    '    pad = 34',
    '    maxh = max(b[2] for b in bars)',
    '    top_y = 26                       # 给「宽×高」标注留出的顶空',
    '    bw = sum(b[1] for b in bars) + pad * (len(bars) + 1)',
    '    pE = Image.new(%sRGBA%s, (bw, top_y + maxh + 58), (0xFA, 0xF8, 0xF2, 255))' % ("'", "'"),
    "    de = ImageDraw.Draw(pE, 'RGBA')",
    '    fe = R.load_font(12)',
    '    x = pad',
    '    for lab, w, h, col in bars:',
    '        y0 = top_y + (maxh - h)',
    '        de.rectangle([x, y0, x + w, top_y + maxh], fill=col + (0x3C,), outline=col + (0xFF,))',
    "        de.text((x + w / 2, y0 - 16), '%dx%d' % (w, h), font=fe,",
    '                fill=(0x33, 0x33, 0x33), anchor=%sma%s)' % ("'", "'"),
    '        de.text((x + w / 2, top_y + maxh + 6), lab, font=fe,',
    '                fill=(0x33, 0x33, 0x33), anchor=%sma%s)' % ("'", "'"),
    '        x += w + pad',
    '    de.text((bw / 2, top_y + maxh + 30),',
    "            '内容尺寸 1:1 —— 本版 236 与挑战窗完全一致；上一版 326，高 90 ⇒ 上下各顶出 45',",
    '            font=fe, fill=(0x55, 0x55, 0x55), anchor=%sma%s)' % ("'", "'"),
    '',
    '    # ── 拼总图 ────────────────────────────────────────────────────────────',
    '    gap = 26',
    '    top = 56',
    '    fh = R.load_font(15)',
    '    fl = R.load_font(13)',
    '',
    '    # ⚠️ 每块都要经 place() 登记，重叠会被断言当场拦下',
    '    placed = []',
    '',
    '    def place(canvas, img, x, y, cap):',
    '        for (ox, oy, ow, oh, ocap) in placed:',
    '            if x < ox + ow and ox < x + img.width and y < oy + oh and oy < y + img.height:',
    "                raise AssertionError('面板重叠：%s 与 %s' % (cap, ocap))",
    '        placed.append((x, y, img.width, img.height, cap))',
    '        canvas.alpha_composite(img, (x, y))',
    '        d.text((x, y - 20), cap, font=fl, fill=(0x33, 0x33, 0x33))',
    '',
    '    col1 = gap',
    '    col2 = col1 + pA.width + gap',
    '    col3 = col2 + pB.width + gap',
    '    row1 = top',
    '    row2 = row1 + max(pA.height, pB.height, pC.height) + gap',
    '',
    '    W = max(col3 + pC.width, col2 + pE.width) + gap',
    '    H = row2 + max(pD.height, pE.height) + 40',
    '    out = Image.new(%sRGBA%s, (W, H), (0xF2, 0xEF, 0xE7, 255))' % ("'", "'"),
    '    d = ImageDraw.Draw(out)',
    "    d.text((W / 2, 20), '考验界面 · 生命之树 -- 窗口尺寸对齐原版挑战窗（实机口径：几何取自 Java 常量）',",
    "           font=fh, fill=(0, 0, 0), anchor='mm')",
    '',
    "    place(out, pA, col1, row1, '① 本版 · 初始态（十个质点皆「已解锁未开启」）· 内容 120x236')",
    "    place(out, pB, col2, row1, '② 本版 · 开启 CHESED 与 GEBURA 后')",
    "    place(out, pC, col3, row1, '③ 对照 · 上一版（a=70 / 宽 148 / 内容 148x326）')",
    "    place(out, pD, col1, row2, '④ 点质点 → 详情小窗（WndTrialInfo）')",
    "    place(out, pE, col2, row2, '⑤ 与挑战窗同规格：内容尺寸 1:1 对照')",
    "    out.convert('RGB').save(OUT)",
    '',
    "    print('[OK] %s  (%dx%d)' % (OUT, W, H))",
    "    for tag, gg in (('本版', g), ('上一版', g_old)):",
    "        print('     %s：a=%.1f 图标显示=%s 树=%.2fx%.2f 内容=%dx%d（左右各留 %.2f）'",
    "              % (tag, gg['A'], ['%dx%d' % (gg['DW'][i], gg['DH'][i]) for i in range(10)],",
    "                 gg['TREE_W'], gg['TREE_H'], gg['WIDTH'], gg['CONTENT_H'],",
    "                 (gg['WIDTH'] - gg['TREE_W']) / 2))",
    "    print('     挑战窗内容 = %dx%d → 本版与之逐像素同规格：%s'",
    "          % (chal_w, chal_h, (g['WIDTH'], g['CONTENT_H']) == (chal_w, chal_h)))",
    '',
    '',
])

REPL = [
    (
        '加挑战窗路径',
        "TRI_JAVA = os.path.join(PKG, 'Trials.java')",
        EOL.join([
            "TRI_JAVA = os.path.join(PKG, 'Trials.java')",
            "CHAL_JAVA = os.path.join(PKG, 'windows/WndChallenges.java')",
            "CHALLENGES_JAVA = os.path.join(PKG, 'Challenges.java')",
        ]),
    ),
    (
        'load_spec 读数挑战窗尺寸',
        EOL.join([
            "def load_spec():",
            '    wnd, tri = _read(WND_JAVA), _read(TRI_JAVA)',
            '    A = _num(wnd, %sA%s)' % ("'", "'"),
        ]),
        EOL.join([
            "def load_spec():",
            '    wnd, tri = _read(WND_JAVA), _read(TRI_JAVA)',
            '    A = _num(wnd, %sA%s)' % ("'", "'"),
            '    # 「对齐原版挑战窗」这条要求的参照物：从 WndChallenges.java + Challenges.NAME_IDS 现算，',
            '    # 不抄死数字（挑战条数会变，抄了就过期）。',
            '    chal, chj = _read(CHAL_JAVA), _read(CHALLENGES_JAVA)',
            "    mch = re.search(r'NAME_IDS\\s*=\\s*\\{(.*?)\\};', chj, re.S)",
            "    n_ch = len(re.findall(r'\"[^\"]+\"', mch.group(1)))",
            "    chal_w = _int(chal, 'WIDTH')",
            "    chal_h = (_int(chal, 'TTL_HEIGHT') + n_ch * _int(chal, 'BTN_HEIGHT')",
            "              + (n_ch - 1) * _int(chal, 'GAP'))",
        ]),
    ),
    (
        'spec 加 CHAL',
        "        ROW_READY=_int(tri, 'ICON_ROW_READY'),",
        EOL.join([
            "        ROW_READY=_int(tri, 'ICON_ROW_READY'),",
            '        CHAL=(chal_w, chal_h),',
        ]),
    ),
    (
        'geometry 支持 A 覆盖',
        EOL.join([
            'def geometry(spec, icon_scale=None):',
            '    """按（可能覆盖的）ICON_SCALE 算出显示尺寸与外包框。"""',
            "    s = spec['ICON_SCALE'] if icon_scale is None else icon_scale",
            "    dw = [max(1, _jround(w * s)) for w in spec['ICON_W']]",
            "    dh = [max(1, _jround(h * s)) for h in spec['ICON_H']]",
            '    g = dict(spec)',
            "    g['SCALE'] = s",
            "    g['DW'], g['DH'] = dw, dh",
            "    g['TREE_W'] = 2 * spec['H'] + max(dw)",
            "    g['TREE_H'] = 4 * spec['A'] + max(dh)",
            "    g['TRIM'] = max(max(dw), max(dh)) / 2.0 + spec['LINE_GAP']",
            "    g['CONTENT_H'] = spec['TTL'] + spec['PAD'] + int(round(g['TREE_H'])) + spec['PAD']",
            '    return g',
        ]),
        EOL.join([
            'def geometry(spec, icon_scale=None, A=None):',
            '    """按（可能覆盖的）ICON_SCALE / A 算出显示尺寸与外包框。',
            '',
            '    A 可覆盖是为了画「上一版 a = 70」的对照：源码里 NODE_DX 是 H 的倍数、NODE_DY 是 A 的倍数，',
            '    这里按倍数重算，改 A 时 H 与全部质点坐标会一起走（否则对照面板的树会散架）。',
            '    """',
            "    s = spec['ICON_SCALE'] if icon_scale is None else icon_scale",
            "    a = spec['A'] if A is None else A",
            '    h = a * math.sqrt(3) / 2',
            "    dw = [max(1, _jround(w * s)) for w in spec['ICON_W']]",
            "    dh = [max(1, _jround(v * s)) for v in spec['ICON_H']]",
            '    g = dict(spec)',
            "    g['SCALE'] = s",
            "    g['A'], g['H'] = a, h",
            "    g['NODE_DX'] = [d / spec['H'] * h for d in spec['NODE_DX']]",
            "    g['NODE_DY'] = [d / spec['A'] * a for d in spec['NODE_DY']]",
            "    g['DW'], g['DH'] = dw, dh",
            "    g['TREE_W'] = 2 * h + max(dw)",
            "    g['TREE_H'] = 4 * a + max(dh)",
            "    g['TRIM'] = max(max(dw), max(dh)) / 2.0 + spec['LINE_GAP']",
            "    g['CONTENT_H'] = int(spec['TTL'] + spec['PAD'] + g['TREE_H'] + spec['PAD'])",
            '    return g',
        ]),
    ),
]


def main():
    txt = TXT
    changed = skipped = 0
    for name, old, new in REPL:
        if new in txt and old not in txt:
            print('  [跳过] %s（已是新版）' % name)
            skipped += 1
            continue
        n = txt.count(old)
        if n != 1:
            print('  [FAIL] %s：命中 %d 次（要求恰好 1 次）' % (name, n))
            return 1
        txt = txt.replace(old, new, 1)
        changed += 1
        print('  [OK]   %s' % name)

    # 整段换掉 main()
    a = txt.find('def main():')
    b = txt.find("if __name__ == '__main__':")
    if a == -1 or b == -1 or b < a:
        print('  [FAIL] 找不到 main() 的边界')
        return 1
    old_main = txt[a:b]
    if '本版考验窗' not in old_main:
        txt = txt[:a] + NEW_MAIN + txt[b:]
        print('  [OK]   main() 整段重排（%d 行 → %d 行）'
              % (old_main.count('\n'), NEW_MAIN.count('\n')))
        changed += 1
    else:
        print('  [跳过] main() 已是新版')
        skipped += 1

    if changed == 0:
        print('无改动。')
        return 0

    for m in ['CHAL=(chal_w, chal_h)', 'def geometry(spec, icon_scale=None, A=None)',
              '本版考验窗', "spec['CHAL']"]:
        if m not in txt:
            print('  [FAIL] 落盘前自检：找不到 %r' % m)
            return 1

    open(P, 'w', encoding='utf-8', newline='').write(txt)
    nb = open(P, 'rb').read()
    print('\n写入完成：%d 处改动，%d 处跳过。CRLF=%d 裸LF=%d'
          % (changed, skipped, nb.count(b'\r\n'), nb.count(b'\n') - nb.count(b'\r\n')))
    return 0


if __name__ == '__main__':
    sys.exit(main())
