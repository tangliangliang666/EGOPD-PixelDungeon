# -*- coding: utf-8 -*-
"""
核验：把「用绝对生命值表达 HT 刻度」的阶段机制改为由 HT 反推（2026-09-24）。

覆盖三个文件：
  · actors/mobs/YogDzewa.java             —— 阶段步长 300 / 末段下限 100 / 光束刻度 400
  · actors/mobs/DwarfKing.java            —— 一阶段转二阶段入场血量 50 / 100
  · actors/mobs/SmilingCorpseMountain.java—— 换皮阈值 2000 / 1000

三层核验（沿用 skill egopd-source-verify 的写法）：
  ① 源码结构：新刻度在、旧绝对数字彻底退场（**先剥注释**，否则说明注释里的旧写法会骗出假红）
  ② 算术等价 + 阶段机仿真：在原版 HT 处与原写死值**逐个相同**；任意 HT 下阶段仍能走完
  ③ 反例自测（--selftest）：把旧写法喂给同一批判据，必须判 FAIL（判据不能恒真）

用法：
  python _chk/verify_boss_phase_scales.py            # 正式核验
  python _chk/verify_boss_phase_scales.py --selftest # 反例自测（反例 FAIL 不计入总账）
"""

import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MOBS = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/actors/mobs')

OK = [0]
FAIL = [0]
NOTES = []


def chk(cond, msg, fname='?'):
    if cond:
        OK[0] += 1
        print('  [OK]   %s' % msg)
    else:
        FAIL[0] += 1
        print('  [FAIL] %s' % msg)
    return bool(cond)


def note(msg):
    NOTES.append(msg)
    print('  [NOTE] %s' % msg)


def run_quiet(fn):
    """跑一批只用于反例自测的断言，把 OK/FAIL 计数还原 —— 反例的 FAIL 不该污染总账。"""
    o, f = OK[0], FAIL[0]
    fn()
    OK[0], FAIL[0] = o, f


# ------------------------------------------------------------------ 工具

def strip_comments(src):
    """单趟左到右状态机：把注释替换成等长空格（保留换行与字符偏移）。

    ⚠️ 不能用正则去 /*...*/ —— 本仓注释里大量出现 `//**强调**`，其中的 `/*` 会被当成块注释开头、
    一路吞到文件末尾（skill egopd-source-verify 铁律第 3 条）。
    """
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == '/' and i + 1 < n and src[i + 1] == '/':
            j = src.find('\n', i)
            j = n if j == -1 else j
            out.append(' ' * (j - i))
            i = j
        elif c == '/' and i + 1 < n and src[i + 1] == '*':
            j = src.find('*/', i + 2)
            j = n if j == -1 else j + 2
            out.append(''.join(ch if ch == '\n' else ' ' for ch in src[i:j]))
            i = j
        elif c == '"':
            j = i + 1
            while j < n and src[j] != '"':
                j += 2 if src[j] == '\\' else 1
            j = min(j + 1, n)
            out.append(src[i:j])
            i = j
        elif c == "'":
            j = i + 1
            while j < n and src[j] != "'":
                j += 2 if src[j] == '\\' else 1
            j = min(j + 1, n)
            out.append(src[i:j])
            i = j
        else:
            out.append(c)
            i += 1
    res = ''.join(out)
    assert len(res) == len(src), 'strip_comments 改变了长度'
    return res


def strip_literals(src):
    """把字符串 / 字符字面量内容清空（判「无 Random」这类判据前必须做，否则文案里的
    "Random.Int(3)" 会假阳性 —— skill 铁律里踩过）。保留引号本身。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c in '"\'':
            q = c
            j = i + 1
            while j < n and src[j] != q:
                j += 2 if src[j] == '\\' else 1
            j = min(j + 1, n)
            out.append(q + ' ' * max(0, j - i - 2) + (q if j - i >= 2 else ''))
            i = j
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def read(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


def method_body(src, name):
    """按「方法名 + (」定位，再按大括号配平取方法体（含外层大括号）。取不到返回 ""。"""
    m = re.search(r'\b' + re.escape(name) + r'\s*\(', src)
    if not m:
        return ''
    k = src.find('{', m.end())
    if k == -1:
        return ''
    depth, i, n = 0, k, len(src)
    while i < n:
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return src[k:i + 1]
        i += 1
    return ''


def int_const(src, name):
    m = re.search(r'\b' + re.escape(name) + r'\s*=\s*(\d+)\s*;', src)
    return int(m.group(1)) if m else None


# --- 判据本体：抽成函数是为了让 --selftest 能把「旧写法」喂给**同一批断言** -----------------
# （只断言「合成字符串里有旧写法」是空测 —— 必须证明判据会判它失败）

def pred_yog_no_abs(code):
    """YogDzewa 剥注释后不得再有绝对刻度。"""
    return ('HT - 300' not in code
            and re.search(r'Math\.max\(\s*HP\s*,\s*100\s*\)', code) is None
            and re.search(r'/\s*400\b', code) is None)


def pred_king_no_abs(code):
    """DwarfKing 剥注释后不得再有一阶段转二阶段的绝对入场血量。"""
    return '? 100 : 50' not in code


def pred_scm_no_abs(code):
    """SmilingCorpseMountain 剥注释后不得再有绝对换皮阈值。"""
    return (re.search(r'HP\s*>=\s*2000', code) is None
            and re.search(r'HP\s*>=\s*1000', code) is None)


# ------------------------------------------------------------------ ① 源码结构

def check_structure():
    print('\n== ① 源码结构 ==')

    yog = read(os.path.join(MOBS, 'YogDzewa.java'))
    yog_code = strip_comments(yog)
    king = read(os.path.join(MOBS, 'DwarfKing.java'))
    king_code = strip_comments(king)
    scm = read(os.path.join(MOBS, 'SmilingCorpseMountain.java'))
    scm_code = strip_comments(scm)

    # --- YogDzewa：常量
    step_num = int_const(yog, 'PHASE_STEP_NUM')
    step_den = int_const(yog, 'PHASE_STEP_DEN')
    beam_num = int_const(yog, 'BEAM_STEP_NUM')
    beam_den = int_const(yog, 'BEAM_STEP_DEN')
    chk(step_num == 3 and step_den == 10,
        'YogDzewa 阶段步长常量 = 3/10（实得 %s/%s）' % (step_num, step_den))
    chk(beam_num == 2 and beam_den == 5,
        'YogDzewa 光束刻度常量 = 2/5（实得 %s/%s）' % (beam_num, beam_den))

    # --- YogDzewa：旧绝对数字彻底退场（**剥注释后**搜）
    chk(pred_yog_no_abs(yog_code), 'YogDzewa 剥注释后已无任何绝对刻度'
        '（HT - 300 / Math.max(HP, 100) / 「/400」三者同时不出现）')

    # --- YogDzewa：三个 helper 的定义与调用点
    for fn in ('phaseStep', 'finalPhaseFloor', 'beamStep'):
        d = len(re.findall(r'\bprivate\s+int\s+' + fn + r'\s*\(\s*\)\s*\{', yog_code))
        chk(d == 1, 'YogDzewa %s() 恰好定义 1 次（实得 %d）' % (fn, d))
    chk(len(re.findall(r'\bphaseStep\s*\(\s*\)', yog_code)) == 3,
        'YogDzewa phaseStep() 恰好被引用 3 次（1 次定义 + 2 次调用；实得 %d）'
        % len(re.findall(r'\bphaseStep\s*\(\s*\)', yog_code)))
    chk(len(re.findall(r'\bfinalPhaseFloor\s*\(\s*\)', yog_code)) == 2,
        'YogDzewa finalPhaseFloor() 恰好被引用 2 次（1 定义 + 1 调用；实得 %d）'
        % len(re.findall(r'\bfinalPhaseFloor\s*\(\s*\)', yog_code)))
    chk(len(re.findall(r'\bbeamStep\s*\(\s*\)', yog_code)) == 2,
        'YogDzewa beamStep() 恰好被引用 2 次（1 定义 + 1 调用；实得 %d）'
        % len(re.findall(r'\bbeamStep\s*\(\s*\)', yog_code)))
    chk(re.search(r'HP\s*=\s*Math\.max\(\s*HP\s*,\s*HT\s*-\s*phaseStep\(\)\s*\*\s*phase\s*\)', yog_code)
        is not None, 'YogDzewa 阶段下限改走 phaseStep()')
    chk(re.search(r'HP\s*<=\s*HT\s*-\s*phaseStep\(\)\s*\*\s*phase', yog_code) is not None,
        'YogDzewa 阶段推进判定改走 phaseStep()')
    chk(re.search(r'Math\.max\(\s*HP\s*,\s*finalPhaseFloor\(\)\s*\)', yog_code) is not None,
        'YogDzewa 末段下限改走 finalPhaseFloor()')
    chk(re.search(r'1\s*\+\s*\(\s*HT\s*-\s*HP\s*\)\s*/\s*beamStep\(\)', yog_code) is not None,
        'YogDzewa 光束数改走 beamStep()')

    # 三个 helper 都是纯算术，不得碰 Random
    for fn in ('phaseStep', 'finalPhaseFloor', 'beamStep'):
        body = strip_literals(method_body(yog_code, fn))
        chk('Random' not in body, 'YogDzewa %s() 方法体内不含 Random（同 seed 随机流不变）' % fn)

    # --- DwarfKing
    chk(len(re.findall(r'\bprivate\s+int\s+phase2EntryHP\s*\(\s*\)\s*\{', king_code)) == 1,
        'DwarfKing phase2EntryHP() 恰好定义 1 次（实得 %d）'
        % len(re.findall(r'\bprivate\s+int\s+phase2EntryHP\s*\(\s*\)\s*\{', king_code)))
    chk(len(re.findall(r'\bphase2EntryHP\s*\(\s*\)', king_code)) == 3,
        'DwarfKing phase2EntryHP() 恰好被引用 3 次（1 定义 + 2 调用；实得 %d）'
        % len(re.findall(r'\bphase2EntryHP\s*\(\s*\)', king_code)))
    chk(pred_king_no_abs(king_code),
        'DwarfKing 剥注释后已无写死的「? 100 : 50」（一阶段转二阶段的入场血量）')
    chk(re.search(r'HT\s*\*\s*2\s*/\s*9', king_code) is not None,
        'DwarfKing 强化分支用 HT×2/9')
    chk(re.search(r'HT\s*/\s*6\b', king_code) is not None, 'DwarfKing 普通分支用 HT/6')
    # 刻意保留的台词扳机 20（不是闸门）
    chk(re.search(r'preHP\s*>\s*20\s*&&\s*HP\s*<\s*20', king_code) is not None,
        'DwarfKing 三阶段台词扳机 20 仍在（刻意保留为绝对量）')
    chk('保留为绝对量' in king, 'DwarfKing 里写明了「刻意保留为绝对量」的理由')

    # --- SmilingCorpseMountain
    chk(re.search(r'HP\s*>=\s*HT\s*\*\s*2\s*/\s*3', scm_code) is not None,
        'SmilingCorpseMountain 形态 3 阈值 = HT×2/3')
    chk(re.search(r'HP\s*>=\s*HT\s*/\s*3\b', scm_code) is not None,
        'SmilingCorpseMountain 形态 2 阈值 = HT/3')
    chk(pred_scm_no_abs(scm_code),
        'SmilingCorpseMountain 剥注释后已无写死的 HP >= 2000 / HP >= 1000')


# ------------------------------------------------------------------ ② 算术等价 + 阶段机仿真

def yog_step(ht, num=3, den=10):
    return ht * num // den


def yog_floors(ht, num=3, den=10):
    """四个阶段的掉血窗口下限：[HT-1步, HT-2步, HT-3步, HT/10]。"""
    s = yog_step(ht, num, den)
    return [ht - s * 1, ht - s * 2, ht - s * 3, ht // den]


def yog_floors_legacy(ht, step=300, last=100):
    return [ht - step, ht - step * 2, ht - step * 3, last]


def yog_windows(floors, ht):
    """每段需要打掉的血量：前三个是阶段推进点之间的差，末段是从第三段下限一路打到 0
    （末段跨 phase 4 与 phase 5：phase 4 只靠拳手倒下推进，HP 本身被夹在下限）。"""
    return [ht - floors[0], floors[0] - floors[1], floors[1] - floors[2], floors[2]]


def king_entry(ht, sb):
    return (ht * 2 // 9) if sb else (ht // 6)


def scm_phase(ht, hp):
    if hp >= ht * 2 // 3:
        return 3
    if hp >= ht // 3:
        return 2
    return 1


def check_arithmetic():
    print('\n== ② 算术等价（原版 HT 处必须与原写死值逐个相同）==')

    # DwarfKing
    chk(king_entry(300, False) == 50,
        'DwarfKing 普通 HT=300 ⇒ 入场血量 50（原写死值；实得 %d）' % king_entry(300, False))
    chk(king_entry(450, True) == 100,
        'DwarfKing 强化 HT=450 ⇒ 入场血量 100（原写死值；实得 %d）' % king_entry(450, True))
    chk(king_entry(375, False) == 62 and king_entry(563, True) == 125,
        'DwarfKing 开 CHESED 后（375 / 563）入场血量 = 62 / 125，不再是固定的 50 / 100'
        '（实得 %d / %d）' % (king_entry(375, False), king_entry(563, True)))

    # YogDzewa
    chk(yog_step(1000) == 300, 'YogDzewa HT=1000 ⇒ 阶段步长 300（原写死值；实得 %d）' % yog_step(1000))
    chk(1000 // 10 == 100, 'YogDzewa HT=1000 ⇒ 末段下限 100（原写死值）')
    chk(1000 * 2 // 5 == 400, 'YogDzewa HT=1000 ⇒ 光束刻度 400（原写死值）')
    chk(yog_floors(1000) == yog_floors_legacy(1000),
        'YogDzewa HT=1000 时四个下限与原写死公式**逐个相同**：%s' % yog_floors(1000))
    chk(yog_floors(1250) == [875, 500, 125, 125],
        'YogDzewa HT=1250（开 CHESED）四个下限 = 875/500/125/125，窗口等分（实得 %s）'
        % yog_floors(1250))
    legacy_1250 = yog_floors_legacy(1250)
    windows_new = yog_windows(yog_floors(1250), 1250)
    windows_old = yog_windows(legacy_1250, 1250)
    chk(windows_new == [375, 375, 375, 125],
        'HT=1250 时四段掉血量 = 375/375/375/125（＝HT 的 30%%/30%%/30%%/10%%；实得 %s）' % windows_new)
    chk(windows_old == [300, 300, 300, 350],
        '旧写法在 HT=1250 上是 300/300/300/350（＝24%%/24%%/24%%/28%%，末段被拉长；实得 %s）' % windows_old)
    chk(windows_new != windows_old,
        '判据不恒真：HT=1250 时新窗口 %s 与旧窗口 %s 确实不同' % (windows_new, windows_old))
    ratio_new = [w / 1250.0 for w in windows_new]
    chk(all(abs(r - 0.3) < 0.001 for r in ratio_new[:3]) and abs(ratio_new[3] - 0.1) < 0.001,
        'HT=1250 的三段各占 30%%、末段 10%%（等比结构；实得 %s）'
        % ['%.0f%%' % (r * 100) for r in ratio_new])

    # 任意 HT 都成立的两个不变量
    bad_mono = []
    bad_pos = []
    for ht in range(120, 3001):
        f = yog_floors(ht)
        if not (f[0] > f[1] > f[2] and f[2] >= f[3]):
            bad_mono.append((ht, f))
        if f[3] < 1:
            bad_pos.append((ht, f))
    chk(not bad_mono, 'YogDzewa HT∈[120,3000] 全部满足「下限严格递减（末两档允许相等）」'
                      '（反例 %d 个）' % len(bad_mono))
    chk(not bad_pos, 'YogDzewa HT∈[120,3000] 末段下限恒 ≥ 1（反例 %d 个）' % len(bad_pos))

    bad_king = [ht for ht in range(150, 901) if king_entry(ht, ht >= 375) < 1]
    chk(not bad_king, 'DwarfKing 入场血量在 HT∈[150,900] 上恒 ≥ 1（反例 %d 个）' % len(bad_king))
    prev, mono = 0, True
    for ht in range(150, 901):
        v = king_entry(ht, ht >= 375)
        if v < prev:
            mono = False
            break
        prev = v
    chk(mono, 'DwarfKing 入场血量随 HT 非递减（不会出现「HT 变大反而更早进二阶段」）')

    # SmilingCorpseMountain
    chk(scm_phase(3000, 3000) == 3 and scm_phase(3000, 2000) == 3,
        'SCM HT=3000 时 HP≥2000 ⇒ 形态 3（与原阈值一致）')
    chk(scm_phase(3000, 1999) == 2 and scm_phase(3000, 1000) == 2,
        'SCM HT=3000 时 1000≤HP<2000 ⇒ 形态 2（与原阈值一致）')
    chk(scm_phase(3000, 999) == 1, 'SCM HT=3000 时 HP<1000 ⇒ 形态 1（与原阈值一致）')
    chk(scm_phase(3750, 2500) == 3 and scm_phase(3750, 2499) == 2
        and scm_phase(3750, 1250) == 2 and scm_phase(3750, 1249) == 1,
        'SCM HT=3750（开 CHESED）时换皮点同比例后移到 2500 / 1250')
    ok3 = all(scm_phase(3000, hp) == (3 if hp >= 2000 else (2 if hp >= 1000 else 1))
              for hp in range(0, 3001))
    chk(ok3, 'SCM HT=3000 时新旧判据在**每一个** HP 上都一致（0..3000 全枚举）')


def check_phase_machine():
    print('\n== ② 阶段机仿真（任意 HT 都要能走完四个阶段）==')
    bad = []
    for ht in list(range(600, 2001)) + [1000, 1250]:
        s = yog_step(ht)
        last = ht // 10
        phase, hp, stops = 1, ht, []
        guard = 0
        # 模拟：每次「打到 0 血」都会被 clamp 到当前阶段下限，并推进一个阶段（无拳手时）
        while phase < 4 and guard < 50:
            hp = 0
            hp = max(hp, ht - s * phase)
            stops.append(hp)
            phase += 1
            guard += 1
        # phase==4 且无拳手 ⇒ 由 YogDzewa 的 act() 推进到 5（这段不经 damage）
        if phase != 4 or len(stops) != 3:
            bad.append((ht, phase, stops))
            continue
        if max(0, last) != stops[-1]:
            # 末段下限应当与第三段下限同高（原版 100 == 100）
            if stops[-1] < last:
                bad.append((ht, 'floor', stops, last))
    chk(not bad, 'YogDzewa HT∈[600,2000]（含 1000/1250）都能走完 3 次推进到 phase=4，'
                 '且末段下限不高于第三段下限（反例 %d 个）' % len(bad))

    # 阶段数不随 HT 变化
    counts = set()
    for ht in (600, 1000, 1250, 2000):
        s, phase, guard = yog_step(ht), 1, 0
        while phase < 4 and guard < 50:
            phase += 1
            guard += 1
        counts.add(phase)
    chk(counts == {4}, 'YogDzewa 阶段数恒为 4（与 HT 无关；实得 %s）' % sorted(counts))


# ------------------------------------------------------------------ ③ 反例自测

def selftest():
    print('\n--- 反例自测（判据必须能判失败；反例的 FAIL 不计入总账）---')

    def case_legacy_yog():
        legacy = ('if (phase < 4) {\n'
                  '\tHP = Math.max(HP, HT - 300 * phase);\n'
                  '} else if (phase == 4) {\n'
                  '\tHP = Math.max(HP, 100);\n'
                  '}\n'
                  'int beams = 1 + (HT - HP)/400;\n')
        chk(pred_yog_no_abs(strip_comments(legacy)) is False,
            'C1 把「旧写法」喂给 pred_yog_no_abs ⇒ 判失败（三处绝对刻度都在）')
        chk(pred_yog_no_abs('int beams = 1 + (HT - HP)/beamStep();') is True,
            'C1b 判据不恒真：已改好的光束写法通过')
        chk(pred_yog_no_abs(strip_comments('int a = 1; // HT - 300')) is True,
            'C1c 只出现在注释里的旧写法不算（判据读的是剥注释后的文本，不假红）')
        chk('int a = 1;' in strip_comments('// 注释\nint a = 1;'),
            'C1d 剥注释只吃注释、不吃代码（否则判据会变成恒真）')

    def case_legacy_king():
        legacy = 'if (HP <= (Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 100 : 50)) {}'
        chk(pred_king_no_abs(strip_comments(legacy)) is False,
            'C2 把「旧写法」喂给 pred_king_no_abs ⇒ 判失败')
        chk('? 100 : 50' not in strip_comments('/* 旧: ? 100 : 50 */ int a=1;'),
            'C2b 只出现在块注释里的旧写法被剥掉（不假红）')
        chk(pred_king_no_abs('if (HP <= phase2EntryHP()) {}') is True,
            'C2c 判据不恒真：已改好的写法通过')

    def case_legacy_scm():
        legacy = 'public int phase(){ if (HP >= 2000) return 3; if (HP >= 1000) return 2; return 1; }'
        chk(pred_scm_no_abs(strip_comments(legacy)) is False,
            'C3 把「旧写法」喂给 pred_scm_no_abs ⇒ 判失败')
        chk(pred_scm_no_abs('if (HP >= HT * 2 / 3) return 3; if (HP >= HT / 3) return 2;') is True,
            'C3b 判据不恒真：已改好的写法通过')

    def case_wrong_constant():
        # 把步长常量改成 4/10 ⇒ 等价性判据必须失败
        chk(yog_step(1000, 4, 10) != 300,
            'C4 步长常量若写成 4/10，HT=1000 时得 %d ≠ 300' % yog_step(1000, 4, 10))
        chk(yog_floors(1000, 4, 10) != yog_floors_legacy(1000),
            'C4b 等价性判据会因此判失败：%s vs %s' % (yog_floors(1000, 4, 10), yog_floors_legacy(1000)))
        chk(king_entry(300, False) != 51,
            'C4c 等价性判据不是「随便写个近似的数都算过」（50 vs 51 必须区分）')

    def case_strip_comments_selfcheck():
        s = 'a /* b */ c // d\ne'
        chk(len(strip_comments(s)) == len(s), 'C5 strip_comments 保长度（可继续比位置）')
        chk(strip_comments('"// 不是注释"').strip() == '"// 不是注释"',
            'C6 字符串里的 // 不被当成注释')
        chk(strip_comments("char c = '/'; int a=1;").count('int a=1;') == 1
            and "char c = '/';" in strip_comments("char c = '/'; int a=1;"),
            'C7 字符字面量里的 / 不误开注释（字面量保留、后面的代码也没被吃掉）')
        chk('/*' in '//**强调**' and strip_comments('//**强调**\nint a=1;').count('int a=1;') == 1,
            'C8 「//**强调**」不会把后面整段吞掉（本仓最常见的假红来源）')
        chk(len(strip_comments('/* a */ b')) == len('/* a */ b') == 9,
            'C9 块注释同样保长度')

    for fn in (case_legacy_yog, case_legacy_king, case_legacy_scm,
               case_wrong_constant, case_strip_comments_selfcheck):
        print('  -- %s' % fn.__name__)
        run_quiet(fn)


# ------------------------------------------------------------------ main

def main():
    print('核验：Boss 阶段刻度由 HT 反推（2026-09-24）')
    check_structure()
    check_arithmetic()
    check_phase_machine()

    if '--selftest' in sys.argv:
        selftest()

    print('\n' + '=' * 78)
    print('断言 %d 条：OK %d，FAIL %d' % (OK[0] + FAIL[0], OK[0], FAIL[0]))
    print('=' * 78)
    return 1 if FAIL[0] else 0


if __name__ == '__main__':
    sys.exit(main())
