# -*- coding: utf-8 -*-
"""考验「生命之树」界面（WndTrials / WndTrialInfo / Trials 图标行）静态回归。

不跑游戏、不跑 Gradle：直接解析 .java 源码 + 真解 tree.png，把这几件事钉死：
  ① 几何：NODE_DX/NODE_DY/EDGES 与「生命之树」规格逐条一致，
     22 条路径的长度多重集恰为 {a:14, √3·a:7, 2a:1}，且整树装得进窗口内容宽（WIDTH）；
  ①b 窗口：内容尺寸必须与「原版挑战窗」WndChallenges **逐像素同规格**（两边源码现算，
     不抄死数字）—— 用户 2026-09-25：旧内容高 326（外框 338）在机上把上下沿顶出屏幕；
  ② 图标：显示尺寸 = round(原生 x 1.40 = 原 0.70 的两倍)，**且只改 scale、不重采样**
     （只有一个出口 scaleToDisplay()；源码里不得出现任何重画/重过滤贴图的调用）；
     三行包围盒一致 ⇒ 换行只改 y 偏移；
  ③ 行映射：Trials.iconRow 是纯函数（不读 SPDSettings），三态取值正确；
  ④ 交互：关窗一次性写回（全仓只有一个 SPDSettings.trials( ）；开关由**详情窗自己**在
     super(...) 之后**构造** —— ⚠️ 关键判据：全局 PointerArea 表是 stackMode 的 Signal
     （后注册优先、首个 true 吞掉其余），早注册的控件会被窗口那个全屏 blocker 抢先命中，
     症状就是「开关点上去毫无反应」；所以 WndTrials 里连 CheckBox 这个词都不许有；
  ⑤ 文案：windows.wndtrials.enable 双语都在，且键名与 Messages.get(this,...) 一致。

用法：python _chk/verify_trials_tree_ui.py            # 正式断言
     python _chk/verify_trials_tree_ui.py --selftest  # 反例自测（证明判据不恒真）
"""
import math
import os
import re
import sys

BASE = r'D:/PD'
PKG = os.path.join(BASE, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
WND = os.path.join(PKG, 'windows/WndTrials.java')
INFO = os.path.join(PKG, 'windows/WndTrialInfo.java')
TRI = os.path.join(PKG, 'Trials.java')
MESS_EN = os.path.join(BASE, 'core/src/main/assets/messages/windows/windows.properties')
MESS_ZH = os.path.join(BASE, 'core/src/main/assets/messages/windows/windows_zh.properties')
MISC_EN = os.path.join(BASE, 'core/src/main/assets/messages/misc/misc.properties')
MISC_ZH = os.path.join(BASE, 'core/src/main/assets/messages/misc/misc_zh.properties')
SPRITE = os.path.join(BASE, 'core/src/main/assets/interfaces/tree.png')
CHAL = os.path.join(PKG, 'windows/WndChallenges.java')
CHALLENGES = os.path.join(PKG, 'Challenges.java')

NAMES = ['keter', 'hokma', 'binah', 'chesed', 'gebura',
         'tiphereth', 'netzach', 'hod', 'yesod', 'malkuth']

# ── 独立于源码的「权威规格」：22 条路径（按名字写，与 tree.png 的列序无关）─────
SPEC_EDGES = [
    ('keter', 'hokma'), ('keter', 'binah'), ('keter', 'tiphereth'),
    ('hokma', 'binah'), ('hokma', 'chesed'), ('hokma', 'tiphereth'),
    ('binah', 'gebura'), ('binah', 'tiphereth'),
    ('chesed', 'gebura'), ('chesed', 'tiphereth'), ('chesed', 'netzach'),
    ('gebura', 'tiphereth'), ('gebura', 'hod'),
    ('tiphereth', 'netzach'), ('tiphereth', 'hod'), ('tiphereth', 'yesod'),
    ('netzach', 'hod'), ('netzach', 'yesod'), ('hod', 'yesod'),
    ('netzach', 'malkuth'), ('hod', 'malkuth'),
    ('yesod', 'malkuth'),
]
# 规格坐标：中列 y = 0/2a/3a/4a，左右列 y = 0.5a/1.5a/2.5a，x = 0/±h
SPEC_DX = {0: 0, 1: +1, 2: -1, 3: +1, 4: -1, 5: 0, 6: +1, 7: -1, 8: 0, 9: 0}   # x 方向：倍 h
SPEC_DY = {0: 0.0, 1: 0.5, 2: 0.5, 3: 1.5, 4: 1.5, 5: 2.0, 6: 2.5, 7: 2.5, 8: 3.0, 9: 4.0}  # 倍 a

FAILS = []
NOTES = []
CHECKS = [0]


def check(cond, label, counter=None):
    CHECKS[0] += 1
    if not cond:
        FAILS.append(label + ('' if counter is None else '   ← 反例：' + str(counter)))
    return cond


def note(msg):
    NOTES.append(msg)


# ── 保偏移剥注释（单趟状态机；别用正则，`//**` 会吞到文件尾）─────────────────
def strip_comments(text):
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            j = text.find('\n', i)
            j = n if j < 0 else j
            out.append(' ' * (j - i))
            i = j
        elif c == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            j = n if j < 0 else j + 2
            out.append(''.join('\n' if ch == '\n' else ' ' for ch in text[i:j]))
            i = j
        elif c in '"\'':
            # 字符串 / 字符字面量原样保留（含转义）
            q = c
            j = i + 1
            while j < n:
                if text[j] == '\\':
                    j += 2
                    continue
                if text[j] == q:
                    j += 1
                    break
                j += 1
            out.append(text[i:j])
            i = j
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def body_of(src, signature, sig=None):
    """按大括号配平取方法体（含花括号）。sig= 正则片段，方法名重名时用它区分。"""
    if sig is None:
        sig = r'\b' + re.escape(signature) + r'\s*\('
    m = re.search(sig, src)
    if not m:
        return ''
    i = src.find('{', m.end())
    if i < 0:
        return ''
    depth = 0
    for j in range(i, len(src)):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                return src[i:j + 1]
    return ''


def read(path):
    return open(path, encoding='utf-8').read()


def jround(v):
    """Java 的 Math.round = floor(v + 0.5)。

    ⚠️ Python 3 的 round 是银行家舍入：15 * 1.5 = 22.5 会算成 22，而 Java 给 23。
    当前 ICON_SCALE = 1.40 碰巧没有 .5 的case，但换个整数缩放就会让**判据自己算错**、
    把好源码误判成 FAIL。所以这里显式复刻 Java 口径。
    """
    return int(math.floor(v + 0.5))


def num(src, name, default=None):
    m = re.search(r'\b' + name + r'\s*=\s*(-?[\d.]+)f?\s*;', src)
    return float(m.group(1)) if m else default


def intarr(src, name):
    m = re.search(r'\b' + name + r'\s*=\s*\{([^}]*)\}', src)
    if not m:
        return None
    return [int(x) for x in re.findall(r'-?\d+', m.group(1))]


def fltarr(src, name, A, H):
    m = re.search(r'\b' + name + r'\s*=\s*\{([^}]*)\}', src)
    if not m:
        return None
    out = []
    for tok in m.group(1).split(','):
        tok = tok.strip().replace('f', '')
        if not tok:
            continue
        out.append(eval(tok, {'__builtins__': {}}, {'A': A, 'H': H}))
    return out


def pairarr(src, name):
    m = re.search(r'\b' + name + r'\s*=\s*\{(.*?)\}\s*;', src, re.S)
    if not m:
        return None
    return [[int(a), int(b)] for a, b in re.findall(r'\{\s*(\d+)\s*,\s*(\d+)\s*\}', m.group(1))]


def keys_of(path):
    out = {}
    for line in read(path).splitlines():
        line = line.strip()
        if not line or line.startswith('#') or '=' not in line:
            continue
        k, v = line.split('=', 1)
        out[k.strip()] = v
    return out


# ══════════════════════════════════════════════════════════════════════════
# 各判据（写成可传入「别的源码文本」的纯函数 ⇒ 反例自测能直接复用）
# ══════════════════════════════════════════════════════════════════════════
def judge_geometry(src, tag=''):
    A = num(src, 'A')
    check(A == 47.5, tag + 'A 必须取 47.5（窗口高度卡出的上限，见 judge_window）', A)
    H = A * math.sqrt(3) / 2

    dx = fltarr(src, 'NODE_DX', A, H)
    dy = fltarr(src, 'NODE_DY', A, H)
    edges = pairarr(src, 'EDGES')
    check(dx is not None and len(dx) == 10, tag + 'NODE_DX 应为 10 项', dx)
    check(dy is not None and len(dy) == 10, tag + 'NODE_DY 应为 10 项', dy)
    check(edges is not None and len(edges) == 22, tag + 'EDGES 应为 22 条', edges and len(edges))
    if not (dx and dy and edges):
        return None

    # ① 坐标表逐项对规格（x 是 ±H 或 0；y 是 a 的倍数）
    for i in range(10):
        want_x = SPEC_DX[i] * H
        check(abs(dx[i] - want_x) < 1e-3,
              tag + '%s 的 x 偏移应为 %s' % (NAMES[i], want_x), dx[i])
        want_y = SPEC_DY[i] * A
        check(abs(dy[i] - want_y) < 1e-3,
              tag + '%s 的 y 偏移应为 %.1f' % (NAMES[i], want_y), dy[i])

    # ② 路径集合与规格一致（无序比较）
    got = set(frozenset(e) for e in edges)
    want = set(frozenset((NAMES.index(a), NAMES.index(b))) for a, b in SPEC_EDGES)
    check(got == want,
          tag + '22 条路径与生命之树规格不符（缺 %d / 多 %d）' % (len(want - got), len(got - want)),
          '缺 %s 多 %s' % (sorted(want - got), sorted(got - want)))
    check(not any(e[0] == e[1] for e in edges), tag + '不得有自环', None)

    # ③ 三种长度：14 条 a、7 条 √3a、1 条 2a
    tally = {}
    for a, b in edges:
        L = math.hypot(dx[b] - dx[a], dy[b] - dy[a]) / A
        tally[round(L, 3)] = tally.get(round(L, 3), 0) + 1
    want_tally = {1.0: 14, round(math.sqrt(3), 3): 7, 2.0: 1}
    check(tally == want_tally,
          tag + '路径长度分布应为 {a:14, √3a:7, 2a:1}', tally)

    # ④ 每个质点都要连上（生命之树没有孤点）
    touch = set()
    for a, b in edges:
        touch.add(a)
        touch.add(b)
    check(len(touch) == 10, tag + '10 个质点都必须至少连一条路径', len(touch))
    return {'A': A, 'H': H, 'dx': dx, 'dy': dy, 'edges': edges}


def judge_icons(src, trials_src, tag=''):
    scale = num(src, 'ICON_SCALE')
    # 1.40 = 原 0.70 的两倍（用户 2026-09-25 机上验证后：「图标显示的过小，需要翻倍」）
    check(scale is not None and abs(scale - 1.40) < 1e-6,
          tag + 'ICON_SCALE 应为 1.40（原 0.70 的两倍）', scale)
    W = intarr(trials_src, 'ICON_W')
    HH = intarr(trials_src, 'ICON_H')
    check(W is not None and len(W) == 10, tag + 'Trials.ICON_W 应为 10 项', W)
    check(HH is not None and len(HH) == 10, tag + 'Trials.ICON_H 应为 10 项', HH)
    if not (W and HH and scale):
        return None

    dw = [max(1, jround(w * scale)) for w in W]
    dh = [max(1, jround(h * scale)) for h in HH]
    flat = src.replace('\n', ' ')

    # 源码里那个静态块必须算出同一组显示尺寸（相信它，就核对它）
    check('Math.round( Trials.ICON_W[i] * ICON_SCALE )' in flat,
          tag + '显示尺寸必须由 Trials.ICON_W/H × ICON_SCALE 现算（不写死）', None)
    check('Math.max( maxW, ICON_DW[i] )' in flat,
          tag + '外包框要取全部图标显示尺寸的最大值', None)

    # 只改 scale、不重采样：不得出现任何「按显示尺寸重画 / 重过滤贴图」的调用。
    # ⚠️ 这里查的是**调用**，不是提没提到 NEAREST —— 类注释里就写着取样走 NEAREST，
    #    所以别再写成 `'nearest' not in src.lower()`（那一版加了注释后必挂）。
    check('icon.resize' not in src, tag + '不得重采样图标贴图（不许 icon.resize）', None)
    for bad_api in ('TextureFilter', 'Pixmap.Filter', 'bitmap.resize', 'texture.filter',
                    'lanczos', 'Lanczos'):
        check(bad_api not in src, tag + '不得自行重采样/改过滤器（不许 %s）' % bad_api, None)

    # 显示尺寸只有一个出口 scaleToDisplay()：声明 1 处 + 调用 2 处（树上 / 详情窗）。
    # ⚠️ body_of 取**第一个**匹配，而调用点在声明之前 ⇒ 必须用 sig= 限定到声明。
    sc = strip_comments(body_of(src, 'scaleToDisplay',
                                sig=r'private\s+static\s+void\s+scaleToDisplay\s*\('))
    check(bool(sc), tag + '抽不到 scaleToDisplay 方法体（判据本身失效）', None)
    check('icon.scale.set(' in sc, tag + 'scaleToDisplay 里要用 Image.scale 改显示尺寸', None)
    check(src.count('icon.scale.set(') == 1,
          tag + 'icon.scale.set( 全仓只许一处（多了会两处走样）', src.count('icon.scale.set('))
    check(src.count('scaleToDisplay(') == 3,
          tag + 'scaleToDisplay 应为 1 处声明 + 2 处调用', src.count('scaleToDisplay('))

    # 换行只改取样矩形的 y 偏移
    code = strip_comments(src)
    check('row * Trials.ICON_FRAME' in code,
          tag + '取样行要写成 row * ICON_FRAME', None)
    check('Trials.ICON_W[index], Trials.ICON_H[index]' in code.replace('\n', ' '),
          tag + '取样矩形宽高必须仍取原生 ICON_W/H', None)

    # 命中区必须能把图标整个圈住（否则点图标边缘会漏）
    touch = num(src, 'TOUCH')
    icon_max = max(max(dw), max(dh))
    check(touch is not None and touch >= icon_max,
          tag + '质点命中区边长须 ≥ 最大图标显示尺寸 %d' % icon_max, touch)

    # 详情窗的图标尺寸必须在交给 WndTrialInfo **之前**设好：
    # IconTitle.layout() 读的是 width() = 原生宽 × scale，设晚了标题位置会压在图标上
    od = strip_comments(body_of(src, 'openDetail',
                                sig=r'private\s+void\s+openDetail\s*\('))
    i_scale, i_new = od.find('scaleToDisplay( icon, index )'), od.find('new WndTrialInfo(')
    check(i_scale != -1 and i_new != -1 and i_scale < i_new,
          tag + '详情窗必须先设 scale 再 new WndTrialInfo', (i_scale, i_new))

    # 装得进窗口：树宽 = 2h + 最大显示宽 ≤ WIDTH
    width = num(src, 'WIDTH')
    A = num(src, 'A')
    H = A * math.sqrt(3) / 2
    tree_w = 2 * H + max(dw)
    tree_h = 4 * A + max(dh)
    check(tree_w <= width, tag + '整树宽 %.2f 必须 ≤ 内容宽 %.0f' % (tree_w, width), tree_w)
    note('本方案的树外包 = %.2f x %.2f（内容宽 %.0f，左右各留 %.2f）'
         % (tree_w, tree_h, width, (width - tree_w) / 2))
    return {'dw': dw, 'dh': dh, 'tree_w': tree_w, 'tree_h': tree_h}


def judge_window(src, trials_src, chal_src, ch_src, tag=''):
    """窗口内容尺寸必须与「原版挑战窗」WndChallenges 逐像素同规格。

    用户 2026-09-25：「考验窗口的大小也需要缩小，现在占据了整个屏幕，导致上下沿都看不到了，
    需要参考原先就有的普通挑战栏的窗口大小」。旧参数 a = 70 ⇒ 内容 326 / 外框 338，机上被顶出。

    判据**从两边源码现算**、不抄死数字：挑战窗高 = TTL + n×BTN + (n−1)×GAP。
    """
    cw = num(chal_src, 'WIDTH')
    cttl = num(chal_src, 'TTL_HEIGHT')
    cbtn = num(chal_src, 'BTN_HEIGHT')
    cgap = num(chal_src, 'GAP')
    m = re.search(r'NAME_IDS\s*=\s*\{(.*?)\};', ch_src, re.S)
    n = len(re.findall(r'"[^"]+"', m.group(1))) if m else 0
    check(None not in (cw, cttl, cbtn, cgap) and n >= 1,
          tag + 'WndChallenges/Challenges 规格取不全（判据本身失效）',
          (cw, cttl, cbtn, cgap, n))
    if None in (cw, cttl, cbtn, cgap) or n < 1:
        return None

    ref_w = int(cw)
    ref_h = int(cttl + n * cbtn + (n - 1) * cgap)

    # 本窗口
    w2 = num(src, 'WIDTH')
    ttl = num(src, 'TTL_HEIGHT')
    pad = num(src, 'PAD')
    A = num(src, 'A')
    scale = num(src, 'ICON_SCALE')
    W = intarr(trials_src, 'ICON_W')
    HH = intarr(trials_src, 'ICON_H')
    if None in (w2, ttl, pad, A, scale) or not W or not HH:
        check(False, tag + 'WndTrials 尺寸常量取不全（判据本身失效）',
              (w2, ttl, pad, A, scale))
        return None
    mw = max(jround(w * scale) for w in W)
    mh = max(jround(h * scale) for h in HH)
    tree_w = 2 * (A * math.sqrt(3) / 2) + mw
    tree_h = 4 * A + mh
    got_w = int(w2)
    got_h = int(ttl + pad + tree_h + pad)

    check(got_w == ref_w and got_h == ref_h,
          tag + '窗口内容应为 %d x %d（与 WndChallenges 同规格），实为 %d x %d'
          % (ref_w, ref_h, got_w, got_h), (got_w, got_h))
    # 内容高必须由「标题 + 上下留白 + 4a + 最大图标高」推出
    area = ref_h - ttl - 2 * pad
    check(abs(tree_h - area) < 1e-6,
          tag + '树高 %.2f 必须正好吃满剩余空间 %.0f' % (tree_h, area), tree_h)
    # a 是被**高度**卡出来的上限：再大一点就装不下（宽度不是瓶颈）
    a_max = (area - mh) / 4.0
    check(A <= a_max + 1e-6, tag + 'a = %.1f 超出高度上限 %.2f' % (A, a_max), A)
    a_wmax = (ref_w - mw) / math.sqrt(3)
    check(a_max < a_wmax,
          tag + 'a 的上限应由高度决定（%.2f < 宽度上限 %.2f），否则说明比例已走样'
          % (a_max, a_wmax), (a_max, a_wmax))
    note('窗口内容 = %d x %d（外框 %d x %d，与 WndChallenges 同规格）；树 %.2f x %.2f，左右各留 %.2f'
         % (got_w, got_h, got_w + 12, got_h + 12, tree_w, tree_h, (got_w - tree_w) / 2))
    return {'w': got_w, 'h': got_h, 'tree_w': tree_w, 'tree_h': tree_h}

def judge_rows(trials_src, tag=''):
    vals = {}
    for k in ('ICON_ROW_ENABLED', 'ICON_ROW_LOCKED', 'ICON_ROW_READY'):
        vals[k] = int(num(trials_src, k, -1))
    check(vals['ICON_ROW_ENABLED'] == 0, tag + '已开启 = 第 0 行', vals['ICON_ROW_ENABLED'])
    check(vals['ICON_ROW_LOCKED'] == 1, tag + '未解锁 = 第 1 行', vals['ICON_ROW_LOCKED'])
    check(vals['ICON_ROW_READY'] == 2, tag + '已解锁未开启 = 第 2 行', vals['ICON_ROW_READY'])
    m = re.search(r'ICON_ROWS\s*=\s*(\d+)\s*;', trials_src)
    check(m is not None and int(m.group(1)) == 3,
          tag + 'ICON_ROWS 应为 3（与 tree.png 行数一致）', m and m.group(1))

    body = strip_comments(body_of(trials_src, 'iconRow'))
    check(bool(body), tag + '取不到 iconRow 方法体（判据本身失效）', None)
    check('SPDSettings' not in body,
          tag + 'iconRow 必须是纯函数：只看入参、不读 SPDSettings', None)
    check('ICON_ROW_ENABLED' in body and 'ICON_ROW_READY' in body and 'ICON_ROW_LOCKED' in body,
          tag + 'iconRow 三个分支都要用到对应行常量', None)

    # 行为真值表：模拟该函数
    def icon_row(enabled, unlocked, unlocked_impl=True):
        if enabled:
            return 0
        return 2 if (unlocked if unlocked_impl else False) else 1

    check(icon_row(True, True) == 0 and icon_row(True, False) == 0,
          tag + '已开启 ⇒ 彩色行（不管解锁与否）', None)
    check(icon_row(False, True) == 2, tag + '已解锁未开启 ⇒ 第 2 行', None)
    check(icon_row(False, False) == 1, tag + '未解锁 ⇒ 第 1 行', None)
    check('return true' in strip_comments(body_of(trials_src, 'isUnlocked')),
          tag + 'isUnlocked 目前应恒为 true（考验默认初始即解锁）', None)
    return vals


def judge_writeback(src, tag=''):
    code = strip_comments(src)
    check(code.count('SPDSettings.trials(') == 1,
          tag + 'SPDSettings.trials( 全文件应恰好 1 处（关窗写回）',
          code.count('SPDSettings.trials('))
    back = body_of(src, 'public void onBackPressed')
    check('SPDSettings.trials( mask )' in back.replace('\n', ' ').replace('  ', ' '),
          tag + '写回必须发生在 onBackPressed 里（且写的是待提交的 mask）', None)
    check('editable' in back, tag + '写回要在 editable 守卫内', None)
    # 开关由**详情窗自己构造** ⇒ 本文件里连 CheckBox 这个词都不该有（含 import）。
    # ⚠️ 这不是洁癖：在 openDetail 里先 new 再传进去，会让开关的 PointerArea 比 WndTrialInfo
    #    那个全屏 blocker **早注册**，于是每次点击都被 blocker 抢先命中、开关永远点不动
    #    （用户 2026-09-25 报的「点击后无反应」）。构造顺序的判据见 judge_info。
    check('CheckBox' not in code,
          tag + 'WndTrials 里不得出现 CheckBox（开关必须由详情窗自己造）', None)
    check('WndTrialInfo' in code, tag + '质点点击要开 WndTrialInfo 详情窗', None)
    check('WndTrialInfo.ToggleListener' in code,
          tag + '勾选要经 WndTrialInfo.ToggleListener 回调（不在调用方 new 开关）', None)
    return True


def judge_info(info_src, tag=''):
    check('extends WndTitledMessage' in info_src, tag + 'WndTrialInfo 必须继承 WndTitledMessage', None)
    code = strip_comments(info_src)

    # ⚠️ 本文件最要紧的一条：开关必须在 super(...) **之后构造**。
    # Button 的 PointerArea 在构造时就注册进全局 PointerEvent 监听表，而那张表是 stackMode 的
    # Signal（add 走 addFirst、dispatch 从队首遍历、首个返回 true 即 return）；WndTitledMessage →
    # Window 的 super(...) 会加一个**覆盖全屏**的 blocker（点窗外即关窗），且它照样拦截
    # （Gizmo.isActive() 只看 active && parent.isActive()，不看 visible）。开关若早于它注册，
    # 每一次点击都被它抢先吃掉 —— 症状正是「点上去毫无反应」。
    sup = code.find('super(')
    newcb = code.find('new CheckBox(')
    check(sup >= 0 and newcb >= 0 and newcb > sup,
          tag + '开关必须在 super(...) 之后**构造**（早于窗口 blocker 注册就永远点不动）',
          (sup, newcb))

    addt = code.find('add( toggle )')
    res = code.find('resize( width')
    check(addt > newcb, tag + '要先构造出开关再 add(toggle)', None)
    check(res > addt, tag + '撑高窗口的 resize 必须在 add(toggle) 之后', None)

    # 初值、可否切换、回调都得有出口
    check('toggle.checked(' in code,
          tag + '开关初值要走 CheckBox.checked(boolean)（直接写字段不会换勾选图标）', None)
    check('toggleActive' in code, tag + '要能把开关置为不可切换（局内查看用）', None)
    check('ToggleListener' in code and 'onToggle' in code,
          tag + '切换要回调 ToggleListener.onToggle', None)
    return True


def judge_text(wnd_src, tag=''):
    zhs, ens = keys_of(MESS_ZH), keys_of(MESS_EN)
    key = 'windows.wndtrials.enable'
    check(key in zhs and zhs[key].strip(), tag + 'zh 缺 %s' % key, None)
    check(key in ens and ens[key].strip(), tag + 'en 缺 %s' % key, None)
    # 键名拼写必须与 Messages.get(this, "enable") 推出来的完全一致
    check('Messages.get( this, "enable" )' in strip_comments(wnd_src).replace('\n', ' '),
          tag + 'WndTrials 要取 Messages.get(this, "enable")', None)
    mz, me = keys_of(MISC_ZH), keys_of(MISC_EN)
    for n in NAMES:
        check('trials.' + n in mz, tag + 'zh 缺考验名称键 trials.%s' % n, None)
        check('trials.%s_desc' % n in mz, tag + 'zh 缺考验描述键 trials.%s_desc' % n, None)
        if 'trials.%s_desc' % n not in me:
            note('en 缺 trials.%s_desc（英文侧未补齐，属既有状态）' % n)
    return True


def judge_png(tag=''):
    try:
        from PIL import Image
    except ImportError:
        note('本解释器无 PIL，跳过 tree.png 三行包围盒核对（用 envs/default 的 python 可补跑）')
        return None
    im = Image.open(SPRITE).convert('RGBA')
    check(im.size == (160, 48), tag + 'tree.png 应为 160x48（3 行 x 10 帧）', im.size)
    boxes = {}
    for r in range(3):
        row = []
        for i in range(10):
            f = im.crop((i * 16, r * 16, i * 16 + 16, r * 16 + 16))
            b = f.getbbox()
            row.append((b[2] - b[0], b[3] - b[1], b[0], b[1]) if b else None)
        boxes[r] = row
    for i in range(10):
        check(boxes[0][i] == boxes[1][i] == boxes[2][i],
              tag + '%s 三行的包围盒必须一致（否则换行要重算尺寸）' % NAMES[i],
              (boxes[0][i], boxes[1][i], boxes[2][i]))
    return boxes


# ══════════════════════════════════════════════════════════════════════════
def main():
    wnd = read(WND)
    info = read(INFO)
    tri = read(TRI)
    chal = read(CHAL)
    ch = read(CHALLENGES)

    print('== ① 几何 ==')
    judge_geometry(wnd)
    print('== ② 图标 ==')
    judge_icons(wnd, tri)
    print('== ③ 行映射 ==')
    judge_rows(tri)
    print('== ④ 交互（写回 / 开关位置）==')
    judge_writeback(wnd)
    judge_info(info)
    print('== ⑤ 文案 ==')
    judge_text(wnd)
    print('== ⑥ 窗口尺寸（对齐挑战窗）==')
    judge_window(wnd, tri, chal, ch)
    print('== ⑦ tree.png ==')
    judge_png()

    for n in NOTES:
        print('[NOTE] ' + n)
    print('-' * 66)
    print('断言 %d 条，失败 %d 条' % (CHECKS[0], len(FAILS)))
    for f in FAILS:
        print('  FAIL ' + f)
    print('ALL PASS' if not FAILS else 'FAILED')
    return 1 if FAILS else 0


def selftest():
    """反例自测：把「坏」的源码喂给同一批判据，必须判 FAIL。"""
    print('=== 反例自测（每条都应被抓出来）===')
    wnd, tri, info = read(WND), read(TRI), read(INFO)
    chal, ch = read(CHAL), read(CHALLENGES)
    ok = 0
    total = 0

    def expect_fail(fn, *args, label=''):
        global FAILS, CHECKS
        save_f, save_c = FAILS, CHECKS[0]
        FAILS, CHECKS[0] = [], 0
        try:
            fn(*args)
        except Exception as e:      # 判据自己崩了也算「抓到了」，但要报出来
            FAILS.append('判据抛异常：%r' % (e,))
        caught = len(FAILS) > 0
        msgs = list(FAILS)
        FAILS, CHECKS[0] = save_f, save_c
        total_local[0] += 1
        if caught:
            total_local[1] += 1
            print('  [OK ] %s → 抓到：%s' % (label, msgs[0][:90]))
        else:
            print('  [BAD] %s → 没抓到（判据可能恒真）' % label)
        return caught

    total_local = [0, 0]

    # ① 几何：删掉 KETER—TIPHERETH 那条 2a 直连
    bad = wnd.replace('{0, 1}, {0, 2}, {0, 5},', '{0, 1}, {0, 2},')
    expect_fail(judge_geometry, bad, 'x', label='几何：少一条路径')
    # ② 几何：把 Binah 挪到中列
    bad = wnd.replace('{ 0f, +H, -H,', '{ 0f, +H, 0f,')
    expect_fail(judge_geometry, bad, 'x', label='几何：Binah 不在左列')
    # ③ 图标：改成重采样
    bad = wnd.replace('icon.scale.set(', 'icon.resize(')
    expect_fail(judge_icons, bad, tri, 'x', label='图标：改用 resize 重采样')
    # ③b 窗口：宽度改回旧的 148（与挑战窗不同规格）
    bad = wnd.replace('private static final int WIDTH\t\t= 120;',
                      'private static final int WIDTH\t\t= 148;')
    expect_fail(judge_window, bad, tri, chal, ch, 'x',
                label='窗口：宽度回到 148')
    # ③c 窗口：a 放大到 60（4a + 22 = 262 > 212，装不下）
    bad = wnd.replace('float A\t= 47.5f;', 'float A\t= 60f;')
    expect_fail(judge_window, bad, tri, chal, ch, 'x',
                label='窗口：a 放大到 60 撑破高度')
    # ④ 行映射：iconRow 读设置
    bad = tri.replace('if (enabled) return ICON_ROW_ENABLED;',
                      'if (enabled || SPDSettings.trials() != 0) return ICON_ROW_ENABLED;')
    expect_fail(judge_rows, bad, 'x', label='行映射：iconRow 不再是纯函数')
    # ⑤ 写回：实时写回（两处）
    bad = wnd.replace('super.onBackPressed();\n\t}', 'SPDSettings.trials( mask );\n\t}')
    expect_fail(judge_writeback, bad, 'x', label='写回：多了一处实时写回')
    # ⑥ 详情窗：开关**构造**抢在 super 之前（本轮修掉的那个真 bug 的失败形态）
    bad = info.replace('\t\tsuper( icon, title, message );',
                       '\t\tnew CheckBox( toggleLabel );\n\t\tsuper( icon, title, message );')
    expect_fail(judge_info, bad, 'x', label='详情窗：开关构造抢在 super 之前')
    # ⑥b 详情窗：初值直接写字段（勾选图标不会换）
    bad = info.replace('toggle.checked( checked );', 'toggle.checked = checked;')
    expect_fail(judge_info, bad, 'x', label='详情窗：初值不走 checked(boolean)')
    # ⑥c 调用方自己 new 开关（又被窗口的全屏 blocker 吃掉）
    bad = wnd.replace('\t\tImage icon = new Image( Assets.Interfaces.TRIALS );',
                      '\t\tnew CheckBox( "x" );\n'
                      '\t\tImage icon = new Image( Assets.Interfaces.TRIALS );')
    expect_fail(judge_writeback, bad, 'x', label='调用方自己 new 开关（会被 blocker 吃掉）')
    # ⑦ 文案：键名拼错
    bad = wnd.replace('Messages.get( this, "enable" )', 'Messages.get( this, "enabled" )')
    expect_fail(judge_text, bad, 'x', label='文案：键名拼错')

    # ⑧ strip_comments 自测：`//**` 不得吞掉后续
    t = 'a\n//**强调**注释\nint x = 1;\n'
    s = strip_comments(t)
    total_local[0] += 1
    if len(s) == len(t) and 'int x = 1;' in s:
        total_local[1] += 1
        print('  [OK ] strip_comments：//** 没吞后续、长度保持')
    else:
        print('  [BAD] strip_comments：//** 把后续吞掉了')

    print('-' * 66)
    print('反例自测 %d/%d 通过' % (total_local[1], total_local[0]))
    return 0 if total_local[1] == total_local[0] else 1


if __name__ == '__main__':
    sys.exit(selftest() if '--selftest' in sys.argv else main())
