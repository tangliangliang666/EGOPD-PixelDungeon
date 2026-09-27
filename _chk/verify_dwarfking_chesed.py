# -*- coding: utf-8 -*-
"""
核验：矮人国王二阶段「王座屏障」刻度（修 CHESED 引发的二阶段卡死）。

背景（根因是**算术**，不是随机）：
  原版二阶段屏障的三个刻度是写死的绝对量 —— 满值 = HT、每击杀一名仆从削减 HT/12、
  波次阈值 200 / 100（强化挑战 300 / 150）。它们靠恒等式咬合：HT=300 ⇒ 4/8/12 次击杀后
  屏障为 200/100/0，正好放第二波 / 第三波 / 进三阶段。唯一前提是 **HT 能被 12（强化 18）整除**。
  考验 CHESED 给敌方生命上限 +25%（300→375）后整除不成立 ⇒ 玩家在二阶段无解可打
  （国王除 KingDamager 外免疫一切伤害），屏障停在 251 > 200，第二波永不出 ⇒ 卡死。

本脚本三层核验：
  ① 源码结构：三条刻度都是从 HT 现算（无残留绝对字面量）、削减量向上取整、四处阈值全部走唯一出口、
     屏障满值仍是 HT、两个削减站点都走 barrierChip()、等式两侧取自同一个 HT。
  ② **算术模拟**（本脚本的重点）：把二阶段的波次状态机复刻成一个纯函数，对一大片 HT 真跑一遍，
     断言「必定能进三阶段、恰好 kills 次击杀、恰好 kills 次出怪、屏障恰在最后一次击杀归零」；
     并断言 HT=300/450 时三个算式的取值与原写死字面量**逐个相同**（⇒ 原版行为不变）。
  ③ 反例自测（`--selftest`）：把「根因的两半」分别还原，断言模拟器**必须判失败** ——
     (a) 阈值恢复绝对字面量（用户报的那个 bug 本体）；
     (b) 削减量改回向下取整 HT/kills。
     另加源码层反例：把 `barrierThreshold( 2 )` 改回 `200` ⇒ 结构检查必须 FAIL。
"""
import io
import os
import re
import sys

P = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/DwarfKing.java'

OK = 0
FAIL = 0

# 反例自测要跑「故意写错」的源码：那时判失败是**期望行为**，不能计进总账。
# 用 SILENT 静音打印 + 快照还原计数。
SILENT = [False]


def ok(msg):
    global OK
    OK += 1
    if not SILENT[0]:
        print('  [OK]   %s' % msg)


def fail(msg):
    global FAIL
    FAIL += 1
    if not SILENT[0]:
        print('  [FAIL] %s' % msg)


def run_quiet(fn, *a, **kw):
    """静音跑一段检查，返回 (新增 OK, 新增 FAIL)，并把总账还原。"""
    global OK, FAIL
    snap_ok, snap_fail = OK, FAIL
    SILENT[0] = True
    try:
        fn(*a, **kw)
    finally:
        SILENT[0] = False
    d_ok, d_fail = OK - snap_ok, FAIL - snap_fail
    OK, FAIL = snap_ok, snap_fail
    return d_ok, d_fail


# --------------------------------------------------------------------------- 工具
def strip_comments(src):
    """保字符偏移的单趟状态机：注释与字符串/字符字面量一律换成空格，换行保留。
    必须是单趟扫描 —— 先用正则去 /* */ 会在 `//**强调**` 这类行上吞到文件尾。"""
    out = list(src)
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            while i < n and src[i] != '\n':
                out[i] = ' '
                i += 1
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            out[i] = out[i + 1] = ' '
            i += 2
            while i < n and not (src[i] == '*' and i + 1 < n and src[i + 1] == '/'):
                if src[i] != '\n':
                    out[i] = ' '
                i += 1
            for _ in range(2):
                if i < n:
                    out[i] = ' '
                    i += 1
        elif c == '"':
            if src.startswith('"""', i):
                for _ in range(3):
                    out[i] = ' '
                    i += 1
                while i < n and not src.startswith('"""', i):
                    if src[i] != '\n':
                        out[i] = ' '
                    i += 1
                for _ in range(3):
                    if i < n:
                        out[i] = ' '
                        i += 1
            else:
                out[i] = ' '
                i += 1
                while i < n and src[i] != '"':
                    if src[i] == '\\':
                        out[i] = ' '
                        i += 1
                        if i < n:
                            out[i] = ' '
                            i += 1
                        continue
                    if src[i] != '\n':
                        out[i] = ' '
                    i += 1
                if i < n:
                    out[i] = ' '
                    i += 1
        elif c == "'":
            out[i] = ' '
            i += 1
            while i < n and src[i] != "'":
                if src[i] == '\\':
                    out[i] = ' '
                    i += 1
                    if i < n:
                        out[i] = ' '
                        i += 1
                    continue
                if src[i] != '\n':
                    out[i] = ' '
                i += 1
            if i < n:
                out[i] = ' '
                i += 1
        else:
            i += 1
    return ''.join(out)


def body_of(code, sig_re):
    """按大括号配平取方法体（返回含花括号的完整块）。"""
    m = re.search(sig_re, code)
    if not m:
        return None
    i = code.find('{', m.end() - 1)
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(code)):
        if code[j] == '{':
            depth += 1
        elif code[j] == '}':
            depth -= 1
            if depth == 0:
                return code[i:j + 1]
    return None


# --------------------------------------------------------------------------- ① 源码结构
def check_source(src):
    code = strip_comments(src)

    # A1 三条刻度助手齐备且签名正确
    if re.search(r'private\s+static\s+int\s+barrierKills\s*\(\s*\)', code):
        ok('A1a barrierKills() 是 private static int')
    else:
        fail('A1a 找不到 private static int barrierKills()')

    kb = body_of(code, r'private\s+static\s+int\s+barrierKills\s*\(\s*\)')
    if kb and re.search(r'STRONGER_BOSSES\s*\)\s*\?\s*18\s*:\s*12', kb):
        ok('A1b barrierKills() = STRONGER_BOSSES ? 18 : 12')
    else:
        fail('A1b barrierKills() 的 18/12 判据不对：%r' % (kb,))

    if re.search(r'private\s+int\s+barrierChip\s*\(\s*\)', code):
        ok('A1c barrierChip() 存在')
    else:
        fail('A1c 找不到 private int barrierChip()')

    cb = body_of(code, r'private\s+int\s+barrierChip\s*\(\s*\)')
    if cb and re.search(r'\(\s*HT\s*\+\s*kills\s*-\s*1\s*\)\s*/\s*kills', cb):
        ok('A1d barrierChip() 用向上取整 (HT + kills - 1) / kills')
    elif cb and re.search(r'HT\s*/\s*kills', cb):
        fail('A1d barrierChip() 是向下取整（HT / kills）—— 整除不成立时凑不到 0')
    else:
        fail('A1d barrierChip() 的算式认不出：%r' % (cb,))

    if re.search(r'private\s+int\s+barrierThreshold\s*\(\s*int\s+\w+\s*\)', code):
        ok('A1e barrierThreshold(int) 存在')
    else:
        fail('A1e 找不到 private int barrierThreshold(int)')

    tb = body_of(code, r'private\s+int\s+barrierThreshold\s*\(\s*int\s+\w+\s*\)')
    if tb and re.search(r'return\s+HT\s*\*\s*\w+\s*/\s*3\s*;', tb):
        ok('A1f barrierThreshold() = HT * num / 3')
    else:
        fail('A1f barrierThreshold() 的算式认不出：%r' % (tb,))

    # A2 全文件不得再有绝对的 shielding() 阈值
    left = re.findall(r'shielding\(\)\s*<=\s*\d+', code)
    if not left:
        ok('A2 无残留的绝对 shielding() 阈值')
    else:
        fail('A2 仍有绝对 shielding() 阈值：%s' % left)

    # A3 四处波次阈值都走唯一出口，且 num 与 summonsMade 守卫配对正确
    pairs = re.findall(r'shielding\(\)\s*<=\s*barrierThreshold\(\s*(\d)\s*\)\s*&&\s*summonsMade\s*<\s*(\d+)', code)
    expect = {('2', '12'), ('1', '18'), ('2', '8'), ('1', '12')}
    if len(pairs) == 4 and set(pairs) == expect:
        ok('A3 四处阈值全部走 barrierThreshold()，num↔summonsMade 配对正确：%s' % sorted(pairs))
    else:
        fail('A3 阈值站点不齐或配对错：%s（期望 %s）' % (pairs, sorted(expect)))

    # A4 屏障满值仍与削减量取自同一个 HT
    if code.count('setShield(HT)') == 1:
        ok('A4a 屏障满值仍是 setShield(HT)（与 barrierChip 同源）')
    else:
        fail('A4a setShield(HT) 出现 %d 次（期望 1）' % code.count('setShield(HT)'))

    # A5 两个削减站点都走 barrierChip()，旧算式彻底退场
    if code.count('barrierChip()') == 3:
        ok('A5a barrierChip() 恰好被调用 3 处（1 个定义 + 2 个削减站点）')
    else:
        fail('A5a barrierChip() 出现 %d 次（期望 3：定义 1 + 站点 2）' % code.count('barrierChip()'))

    stale = re.findall(r'HT\s*/\s*(?:18|12)\b', code)
    if not stale:
        ok('A5b 旧的 HT/18、HT/12 裸算式已彻底退场')
    else:
        fail('A5b 仍有裸算式：%s' % stale)

    if 'int damage = m.HT /' not in code:
        ok('A5c KingDamager.detach 不再自算削减量')
    else:
        fail('A5c KingDamager.detach 仍在自算削减量')

    # A6 新代码不引入 Random（随机流必须与改动前逐字节一致）
    helpers = ''.join(x for x in [kb, cb, tb] if x)
    if 'Random' not in helpers:
        ok('A6 三条刻度助手不含 Random（随机流不受影响）')
    else:
        fail('A6 助手体里出现了 Random')

    # A7 除屏蔽的减号外，改动不得削弱既有的「不回到 Char.damage 闸门」前提
    if 'Trials' not in code:
        ok('A7 本文件不引用 Trials（本次修复不依赖考验系统的其它钩子）')
    elif 'Trials.interceptLethalDamage' in code:
        fail('A7 意外出现 GEBURA 闸门调用')
    else:
        ok('A7 未新增 GEBURA 闸门调用')


# --------------------------------------------------------------------------- ② 算术模拟
def chip_ceil(ht, kills):
    return (ht + kills - 1) // kills


def chip_floor(ht, kills):
    return ht // kills


def thr_thirds(ht, num, sb=False):
    return ht * num // 3


def thr_absolute(ht, num, sb=False):
    """原版写死的绝对阈值：常规 200/100，强化挑战 300/150。"""
    if sb:
        return 300 if num == 2 else 150
    return 200 if num == 2 else 100


def simulate(ht, sb, chip_fn, thr_fn):
    """复刻二阶段波次状态机：每轮「国王 act（按阈值出怪）」+「玩家把场上清空」。

    返回 dict：phase3_kills（None＝卡死）、kills、spawns、wave2_at、wave3_at、shield_end。
    """
    kills_need = 18 if sb else 12
    chip = chip_fn(ht, kills_need)
    thr2, thr1 = thr_fn(ht, 2, sb), thr_fn(ht, 1, sb)

    shield = max(0, ht)
    summons_made = 0
    kills = spawns = alive = 0
    wave2_at = wave3_at = None
    phase3_kills = None

    limit = kills_need * 60 + 600  # 远超正常所需；跑满即视为卡死
    guard = 0
    while guard < limit and phase3_kills is None:
        guard += 1
        # ---- 国王回合 ----
        if not sb:
            if summons_made < 4:
                summons_made += 1
                spawns += 1
                alive += 1
            elif shield <= thr2 and summons_made < 8:
                if summons_made == 4:
                    wave2_at = kills
                summons_made += 1
                spawns += 1
                alive += 1
            elif shield <= thr1 and summons_made < 12:
                if wave3_at is None:
                    wave3_at = kills
                summons_made = 12
                spawns += 4
                alive += 4
        else:
            if summons_made < 6:
                summons_made += 2
                spawns += 2
                alive += 2
            elif shield <= thr2 and summons_made < 12:
                if summons_made == 6:
                    wave2_at = kills
                summons_made += 3
                spawns += 3
                alive += 3
            elif shield <= thr1 and summons_made < 18:
                if summons_made == 12:
                    if wave3_at is None:
                        wave3_at = kills
                    summons_made += 4
                    spawns += 4
                    alive += 4
                else:
                    summons_made += 2
                    spawns += 2
                    alive += 2
        # ---- 玩家回合：把场上清空 ----
        while alive > 0:
            alive -= 1
            kills += 1
            shield = max(0, shield - chip)
            if shield == 0:
                phase3_kills = kills
                break

    return dict(phase3_kills=phase3_kills, kills=kills, spawns=spawns,
                wave2_at=wave2_at, wave3_at=wave3_at, shield_end=shield,
                kills_need=kills_need, chip=chip)


def check_arithmetic():
    # B1 原版取值必须与写死的字面量逐个相同（⇒ 原版行为分毫不变）
    for ht, sb, e2, e1, ec in [(300, False, 200, 100, 25), (450, True, 300, 150, 25)]:
        g2 = thr_thirds(ht, 2)
        g1 = thr_thirds(ht, 1)
        gc = chip_ceil(ht, 18 if sb else 12)
        if (g2, g1, gc) == (e2, e1, ec):
            ok('B1 HT=%d(%s) 新算式 = 旧字面量：阈值 %d/%d、削减 %d' % (ht, 'SB' if sb else '普通', e2, e1, ec))
        else:
            fail('B1 HT=%d(%s) 新算式 (%d,%d,%d) ≠ 旧字面量 (%d,%d,%d)'
                 % (ht, 'SB' if sb else '普通', g2, g1, gc, e2, e1, ec))

    # B2 广扫 HT：新算式必须**必定能通关**（不许卡死），且不会多出怪/多算击杀
    bad = []
    for sb in (False, True):
        for ht in range(120, 1201):
            r = simulate(ht, sb, chip_ceil, thr_thirds)
            if r['phase3_kills'] is None:
                bad.append((ht, sb, '卡死'))
            elif r['spawns'] > r['kills_need'] or r['kills'] > r['kills_need']:
                bad.append((ht, sb, 'kills=%d spawns=%d 超出 %d'
                            % (r['kills'], r['spawns'], r['kills_need'])))
            elif r['shield_end'] != 0:
                bad.append((ht, sb, '收尾 shield=%d' % r['shield_end']))
            elif r['phase3_kills'] != r['kills']:
                bad.append((ht, sb, '三阶段触发点与末次击杀不一致'))
    if not bad:
        ok('B2 HT∈[120,1200]×2 模式（2162 例）：全部能进三阶段、击杀/出怪数不超过 kills、屏障收尾为 0')
    else:
        fail('B2 有 %d 例不满足：%s' % (len(bad), bad[:6]))

    # B3 不变量：kills 次击杀**必须**够打空屏障（上取整的要害）
    bad = [(ht, sb) for sb in (False, True) for ht in range(120, 1201)
           if (18 if sb else 12) * chip_ceil(ht, 18 if sb else 12) < ht]
    if not bad:
        ok('B3 不变量成立：kills × chip ≥ HT（任何 HT 都打得空）')
    else:
        fail('B3 打不空的 HT：%s' % bad[:6])

    # B4 真实可达的 HT 上必须**精确**：恰好 kills 次击杀时屏障归零、三波节奏落在设计窗口
    for ht, sb in [(250, False), (300, False), (375, False), (450, True), (563, True), (600, True)]:
        r = simulate(ht, sb, chip_ceil, thr_thirds)
        tag = 'HT=%d(%s)' % (ht, 'SB' if sb else '普通')
        third = r['kills_need'] // 3
        if r['phase3_kills'] is None:
            fail('B4 %s 卡死' % tag)
        elif r['kills'] != r['kills_need'] or r['spawns'] != r['kills_need']:
            fail('B4 %s 不精确：kills=%d spawns=%d（期望 %d）'
                 % (tag, r['kills'], r['spawns'], r['kills_need']))
        elif r['phase3_kills'] != r['kills_need']:
            fail('B4 %s 三阶段在第 %s 次击杀触发（期望 %d）' % (tag, r['phase3_kills'], r['kills_need']))
        elif not (r['wave2_at'] is not None and 1 <= r['wave2_at'] <= third):
            fail('B4 %s 第二波触发点异常：%s' % (tag, r['wave2_at']))
        elif not (r['wave3_at'] is not None and third < r['wave3_at'] <= 2 * third):
            fail('B4 %s 第三波触发点异常：%s' % (tag, r['wave3_at']))
        else:
            ok('B4 %s：恰好 %d 次击杀/出怪，三阶段在第 %d 次触发，波次@%d/%d'
               % (tag, r['kills_need'], r['phase3_kills'], r['wave2_at'], r['wave3_at']))

    # B5 回归：旧写法在 CHESED 的 HT=375 上必须卡死；在原版 300/450 上必须正常
    r = simulate(375, False, chip_floor, thr_absolute)
    if r['phase3_kills'] is None:
        ok('B5a 旧写法 @HT=375 复现卡死（用户报的 bug 被模拟器抓到）')
    else:
        fail('B5a 旧写法 @HT=375 竟然没卡死：%r' % r)

    for ht, sb in [(300, False), (450, True)]:
        r = simulate(ht, sb, chip_floor, thr_absolute)
        if r['phase3_kills'] == r['kills_need']:
            ok('B5b 旧写法 @HT=%d(%s) 正常通关（说明模拟器不是恒判失败）'
               % (ht, 'SB' if sb else '普通'))
        else:
            fail('B5b 旧写法 @HT=%d(%s) 结果异常：%r' % (ht, 'SB' if sb else '普通', r))


# --------------------------------------------------------------------------- ③ 反例自测
def check_selftest(src):
    print('--- 反例自测（判据必须能判失败；反例的 FAIL 不计入总账）---')

    # C0 基线：未改动的源码跑结构检查必须全绿（否则下面「变红」说明不了任何事）
    d_ok, d_fail = run_quiet(check_source, src)
    if d_fail == 0 and d_ok > 0:
        ok('C0 基线：真实源码跑结构检查 %d 条全绿（判据不是恒失败）' % d_ok)
    else:
        fail('C0 基线就不干净：OK %d / FAIL %d' % (d_ok, d_fail))

    # C1 阈值恢复绝对字面量（bug 本体）⇒ 结构检查必须 FAIL
    mut = src.replace('shielding() <= barrierThreshold( 2 ) && summonsMade < 8',
                      'shielding() <= 200 && summonsMade < 8')
    if mut == src:
        fail('C1 注入失败（锚点没命中），反例没跑起来')
    else:
        d_ok, d_fail = run_quiet(check_source, mut)
        if d_fail >= 1:
            ok('C1 阈值改回绝对字面量 ⇒ 结构检查判失败（+%d 条 FAIL）' % d_fail)
        else:
            fail('C1 阈值改回绝对字面量却仍全绿 —— 结构判据是空测！')

    # C2 削减量改回向下取整 ⇒ 算术模拟必须判卡死（HT=375）
    r = simulate(375, False, chip_floor, thr_thirds)
    if r['phase3_kills'] is None:
        ok('C2 削减量改回向下取整 ⇒ 模拟器判卡死')
    else:
        fail('C2 向下取整竟然能通关：%r' % r)

    # C3 阈值幅度写错（/4 而非 /3）⇒ 模拟器必须判失败
    r = simulate(375, False, chip_ceil, lambda ht, num, sb=False: ht * num // 4)
    if r['phase3_kills'] is None:
        ok('C3 阈值改成满值的 1/4 ⇒ 模拟器判卡死（第二波被卡住）')
    else:
        fail('C3 阈值 /4 竟然能通关：%r' % r)

    # C4 助手算式改回向下取整 ⇒ 结构检查必须 FAIL
    mut2 = src.replace('return (HT + kills - 1) / kills;', 'return HT / kills;')
    if mut2 == src:
        fail('C4 注入失败（锚点没命中），反例没跑起来')
    else:
        d_ok, d_fail = run_quiet(check_source, mut2)
        if d_fail >= 1:
            ok('C4 削减量改成向下取整 ⇒ 结构检查判失败（+%d 条 FAIL）' % d_fail)
        else:
            fail('C4 向下取整却仍全绿 —— A1d 是空测！')

    # C5 阈值站点少一处（把第三波阈值改回字面量）⇒ A3 必须 FAIL
    mut3 = src.replace('shielding() <= barrierThreshold( 1 ) && summonsMade < 12',
                       'shielding() <= 100 && summonsMade < 12')
    if mut3 == src:
        fail('C5 注入失败（锚点没命中）')
    else:
        d_ok, d_fail = run_quiet(check_source, mut3)
        if d_fail >= 1:
            ok('C5 第三波阈值改回字面量 ⇒ 结构检查判失败（+%d 条 FAIL）' % d_fail)
        else:
            fail('C5 少一处改回却仍全绿 —— A2/A3 是空测！')


# --------------------------------------------------------------------------- main
def main():
    if not os.path.exists(P):
        print('FAIL 找不到 %s' % P)
        return 1
    src = io.open(P, 'rb').read().decode('utf-8')

    print('=== ① 源码结构 ===')
    check_source(src)
    print('=== ② 算术模拟 ===')
    check_arithmetic()

    if '--selftest' in sys.argv:
        check_selftest(src)

    print('')
    print('断言 %d 条：OK %d，FAIL %d' % (OK + FAIL, OK, FAIL))
    return 0 if FAIL == 0 else 1


if __name__ == '__main__':
    sys.exit(main())
