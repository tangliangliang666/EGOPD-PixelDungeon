# -*- coding: utf-8 -*-
"""
给 verify_trials_tree_ui.py 补上「窗口尺寸必须与原版挑战窗逐像素同规格」这条判据。

顺带两处加固：
  · `_jround`：Python 的 round 是银行家舍入（15*1.5 = 22.5 → 22，Java 给 23），
    原先直接 `int(round(...))`，换个非整数缩放就会**判据自己算错**、误报 FAIL；
  · 反例自测加两条（宽度回到 148 / a 放大到 60），证明新判据不恒真。

判据不抄死数字：挑战窗高从 WndChallenges.java + Challenges.NAME_IDS 现推。
幂等；每条替换断言恰好命中一次。
"""
import sys

P = '_chk/verify_trials_tree_ui.py'
raw = open(P, 'rb').read()
EOL = '\r\n' if b'\r\n' in raw else '\n'
TXT = raw.decode('utf-8')

WINDOW_FN = EOL.join([
    "def judge_window(src, trials_src, chal_src, ch_src, tag=''):",
    '    """窗口内容尺寸必须与「原版挑战窗」WndChallenges 逐像素同规格。',
    '',
    '    用户 2026-09-25：「考验窗口的大小也需要缩小，现在占据了整个屏幕，导致上下沿都看不到了，',
    '    需要参考原先就有的普通挑战栏的窗口大小」。旧参数 a = 70 ⇒ 内容 326 / 外框 338，机上被顶出。',
    '',
    '    判据**从两边源码现算**、不抄死数字：挑战窗高 = TTL + n×BTN + (n−1)×GAP。',
    '    """',
    "    cw = num(chal_src, 'WIDTH')",
    "    cttl = num(chal_src, 'TTL_HEIGHT')",
    "    cbtn = num(chal_src, 'BTN_HEIGHT')",
    "    cgap = num(chal_src, 'GAP')",
    "    m = re.search(r'NAME_IDS\\s*=\\s*\\{(.*?)\\};', ch_src, re.S)",
    "    n = len(re.findall(r'\"[^\"]+\"', m.group(1))) if m else 0",
    "    check(None not in (cw, cttl, cbtn, cgap) and n >= 1,",
    "          tag + 'WndChallenges/Challenges 规格取不全（判据本身失效）',",
    "          (cw, cttl, cbtn, cgap, n))",
    "    if None in (cw, cttl, cbtn, cgap) or n < 1:",
    '        return None',
    '',
    "    ref_w = int(cw)",
    "    ref_h = int(cttl + n * cbtn + (n - 1) * cgap)",
    '',
    '    # 本窗口',
    "    w2 = num(src, 'WIDTH')",
    "    ttl = num(src, 'TTL_HEIGHT')",
    "    pad = num(src, 'PAD')",
    "    A = num(src, 'A')",
    "    scale = num(src, 'ICON_SCALE')",
    "    W = intarr(trials_src, 'ICON_W')",
    "    HH = intarr(trials_src, 'ICON_H')",
    '    if None in (w2, ttl, pad, A, scale) or not W or not HH:',
    "        check(False, tag + 'WndTrials 尺寸常量取不全（判据本身失效）',",
    '              (w2, ttl, pad, A, scale))',
    '        return None',
    '    mw = max(jround(w * scale) for w in W)',
    '    mh = max(jround(h * scale) for h in HH)',
    '    tree_w = 2 * (A * math.sqrt(3) / 2) + mw',
    '    tree_h = 4 * A + mh',
    '    got_w = int(w2)',
    '    got_h = int(ttl + pad + tree_h + pad)',
    '',
    '    check(got_w == ref_w and got_h == ref_h,',
    "          tag + '窗口内容应为 %d x %d（与 WndChallenges 同规格），实为 %d x %d'",
    '          % (ref_w, ref_h, got_w, got_h), (got_w, got_h))',
    '    # 内容高必须由「标题 + 上下留白 + 4a + 最大图标高」推出',
    '    area = ref_h - ttl - 2 * pad',
    '    check(abs(tree_h - area) < 1e-6,',
    "          tag + '树高 %.2f 必须正好吃满剩余空间 %.0f' % (tree_h, area), tree_h)",
    '    # a 是被**高度**卡出来的上限：再大一点就装不下（宽度不是瓶颈）',
    '    a_max = (area - mh) / 4.0',
    "    check(A <= a_max + 1e-6, tag + 'a = %.1f 超出高度上限 %.2f' % (A, a_max), A)",
    "    a_wmax = (ref_w - mw) / math.sqrt(3)",
    '    check(a_max < a_wmax,',
    "          tag + 'a 的上限应由高度决定（%.2f < 宽度上限 %.2f），否则说明比例已走样'",
    '          % (a_max, a_wmax), (a_max, a_wmax))',
    "    note('窗口内容 = %d x %d（外框 %d x %d，与 WndChallenges 同规格）；树 %.2f x %.2f，左右各留 %.2f'",
    "         % (got_w, got_h, got_w + 12, got_h + 12, tree_w, tree_h, (got_w - tree_w) / 2))",
    "    return {'w': got_w, 'h': got_h, 'tree_w': tree_w, 'tree_h': tree_h}",
    '',
    '',
])

REPL = [
    (
        'docstring 加窗口条目',
        EOL.join([
            '  ① 几何：NODE_DX/NODE_DY/EDGES 与「生命之树」规格逐条一致，',
            '     22 条路径的长度多重集恰为 {a:14, √3·a:7, 2a:1}，且整树装得进窗口内容宽（WIDTH）；',
        ]),
        EOL.join([
            '  ① 几何：NODE_DX/NODE_DY/EDGES 与「生命之树」规格逐条一致，',
            '     22 条路径的长度多重集恰为 {a:14, √3·a:7, 2a:1}，且整树装得进窗口内容宽（WIDTH）；',
            '  ①b 窗口：内容尺寸必须与「原版挑战窗」WndChallenges **逐像素同规格**（两边源码现算，',
            '     不抄死数字）—— 用户 2026-09-25：旧内容高 326（外框 338）在机上把上下沿顶出屏幕；',
        ]),
    ),
    (
        '加挑战窗路径常量',
        "SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')",
        EOL.join([
            "SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')",
            "CHAL = os.path.join(PKG, 'windows/WndChallenges.java')",
            "CHALLENGES = os.path.join(PKG, 'Challenges.java')",
        ]),
    ),
    (
        '加 jround',
        'def num(src, name, default=None):',
        EOL.join([
            'def jround(v):',
            '    """Java 的 Math.round = floor(v + 0.5)。',
            '',
            '    ⚠️ Python 3 的 round 是银行家舍入：15 * 1.5 = 22.5 会算成 22，而 Java 给 23。',
            '    当前 ICON_SCALE = 1.40 碰巧没有 .5 的case，但换个整数缩放就会让**判据自己算错**、',
            '    把好源码误判成 FAIL。所以这里显式复刻 Java 口径。',
            '    """',
            '    return int(math.floor(v + 0.5))',
            '',
            '',
            'def num(src, name, default=None):',
        ]),
    ),
    (
        'A 期望值',
        "    check(A == 70.0, tag + 'A 必须取 70（定稿）', A)",
        "    check(A == 47.5, tag + 'A 必须取 47.5（窗口高度卡出的上限，见 judge_window）', A)",
    ),
    (
        '显示尺寸用 jround',
        EOL.join([
            '    dw = [max(1, int(round(w * scale))) for w in W]',
            '    dh = [max(1, int(round(h * scale))) for h in HH]',
        ]),
        EOL.join([
            '    dw = [max(1, jround(w * scale)) for w in W]',
            '    dh = [max(1, jround(h * scale)) for h in HH]',
        ]),
    ),
    (
        '插入 judge_window',
        "def judge_rows(trials_src, tag=''):",
        WINDOW_FN + "def judge_rows(trials_src, tag=''):",
    ),
    (
        'main 读取挑战窗源码',
        EOL.join([
            '    wnd = read(WND)',
            '    info = read(INFO)',
            '    tri = read(TRI)',
        ]),
        EOL.join([
            '    wnd = read(WND)',
            '    info = read(INFO)',
            '    tri = read(TRI)',
            '    chal = read(CHAL)',
            '    ch = read(CHALLENGES)',
        ]),
    ),
    (
        'main 加第六节',
        EOL.join([
            "    print('== ⑥ tree.png ==')",
            '    judge_png()',
        ]),
        EOL.join([
            "    print('== ⑥ 窗口尺寸（对齐挑战窗）==')",
            '    judge_window(wnd, tri, chal, ch)',
            "    print('== ⑦ tree.png ==')",
            '    judge_png()',
        ]),
    ),
    (
        'selftest 读取挑战窗源码',
        '    wnd, tri, info = read(WND), read(TRI), read(INFO)',
        EOL.join([
            '    wnd, tri, info = read(WND), read(TRI), read(INFO)',
            '    chal, ch = read(CHAL), read(CHALLENGES)',
        ]),
    ),
    (
        'selftest 加两条反例',
        "    expect_fail(judge_icons, bad, tri, 'x', label='图标：改用 resize 重采样')",
        EOL.join([
            "    expect_fail(judge_icons, bad, tri, 'x', label='图标：改用 resize 重采样')",
            '    # ③b 窗口：宽度改回旧的 148（与挑战窗不同规格）',
            "    bad = wnd.replace('private static final int WIDTH\\t\\t= 120;',",
            "                      'private static final int WIDTH\\t\\t= 148;')",
            "    expect_fail(judge_window, bad, tri, chal, ch, 'x',",
            "                label='窗口：宽度回到 148')",
            '    # ③c 窗口：a 放大到 60（4a + 22 = 262 > 212，装不下）',
            "    bad = wnd.replace('float A\\t= 47.5f;', 'float A\\t= 60f;')",
            "    expect_fail(judge_window, bad, tri, chal, ch, 'x',",
            "                label='窗口：a 放大到 60 撑破高度')",
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

    if changed == 0:
        print('无改动。')
        return 0

    for m in ['def judge_window(', 'def jround(', "chal = read(CHAL)",
              "judge_window(wnd, tri, chal, ch)"]:
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
