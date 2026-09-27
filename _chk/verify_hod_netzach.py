# 核验考验「HOD（荣耀）」与「NETZACH（胜利）」的实现。
#
# 为什么需要它：这两条考验是**第四类**规则 —— 它们既改「数值出口」（命中/闪避）又改「渲染」：
#   · NETZACH 的数值侧 = 敌方**闪避**按区递增。收口不是某个方法，而是「取数」这一步：
#     全作有**三处**在算「命中 vs 闪避」，且各自复刻了一整套乘区（各自的注释里自认 copy-pasta）：
#       ① Char.hit（真正的命中判定）② Talent.dodgeAsDamageReduction（闪避折算减伤）
#       ③ Stone.proc（磐岩附魔，同样折算减伤）
#     ⇒ 三处必须改调同一个取数收口 Trials.finalAccuracy / finalEvasion，否则会漂移。
#   · NETZACH 的渲染侧 = 距离淡出。收口是 CharSprite.draw()，且必须**只影响这一帧**
#     （隐形走 AlphaTweener 会持续写 alpha；受击闪光走 hardlight 只碰颜色）⇒ 不改持久状态。
#   · HOD 的数值侧 = 敌方**命中**按区递增（与 NETZACH 共用取数收口）。
#   · HOD 的晋升侧 = 「存活 > 300 回合（Boss 450）⇒ 随机精英（不可叠加）」：
#     计时走常驻 buff HodGlory（由 Mob.onAdd 幂等重挂），晋升走 Trials.grantHodElite。
#
# 所以核验要证明：
#   · 常量口径正确（按区递增 0.10、淡出阈值 1/3、普通 300 / Boss 450 回合、Boss 判据 Char.Property.BOSS）；
#   · 取数收口**唯一**（恰好三处调用，多一处少一处都是回归）；
#   · 位置正确（命中/闪避取数必须早于无限命中/闪避的短路判定；
#     HOD 的计时挂载必须排在 CHESED 的 EXP 提前返回**之前**，否则 EXP=0 的怪被一并跳过；
#     计时→晋升→自摘的先后；淡出在 super.draw 前乘、后还原）；
#   · 随机流**只在授权处**（grantHodElite 只允许经 ChampionEnemy.randomChampionClass 掷一次；
#     其余新方法一律不得碰 Random）；
#   · 反例自测证明「比位置」「无 Random」这些判据不恒真。
#
# 用法：
#   bash _chk/_build_hod_netzach.sh probe     # 先编到 _chk/_javachk2
#   python _chk/verify_hod_netzach.py
#   python _chk/verify_hod_netzach.py --selftest

import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk2')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'
PROBE_OUT = os.path.join(HERE, '_hodnetzach.out')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/misc')

HEADER = 'com.shatteredpixel.shatteredpixeldungeon'
TRIALS = HEADER + '.Trials'
CHAR = HEADER + '.actors.Char'
SPRITE = HEADER + '.sprites.CharSprite'
HODGLORY = HEADER + '.actors.buffs.HodGlory'
CHAMP = HEADER + '.actors.buffs.ChampionEnemy'

# 取数收口的**全部**调用点（全仓必须恰好这三个文件）
FINAL_CALLERS = ['actors/Char.java', 'actors/hero/Talent.java', 'items/armor/glyphs/Stone.java']
# 淡出系数的**全部**调用点（精灵绘制 ＋ 两条浮空血条 ＋ 状态标记 EmoIcon）
FADE_CALLERS = ['effects/EmoIcon.java',
                'sprites/CharSprite.java',
                'ui/CharHealthIndicator.java',
                'ui/TargetHealthIndicator.java']

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def read_raw(rel):
    return open(os.path.join(SRC, rel), 'rb').read()


def strip_comments(src):
    """把注释替换成等长空格（保留换行）——**保留字符偏移**，所以仍能「比位置」。

    单趟状态机，识别 // 、/* */ 、字符串 "" 、字符 ''。
    不能用正则去 /*...*/ ：注释里出现 `//**` 时正则会一路吞到文件末尾。"""
    out = []
    state = None          # None | 'line' | 'block' | 'str' | 'chr'
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
        if state is None:
            if c == '/' and nxt == '/':
                state = 'line'; out.append('  '); i += 2; continue
            if c == '/' and nxt == '*':
                state = 'block'; out.append('  '); i += 2; continue
            if c == '"':
                state = 'str'; out.append(c); i += 1; continue
            if c == "'":
                state = 'chr'; out.append(c); i += 1; continue
            out.append(c); i += 1; continue
        if state == 'line':
            state = None if c == '\n' else 'line'
            out.append(c if c == '\n' else ' ')
            i += 1; continue
        if state == 'block':
            if c == '*' and nxt == '/':
                state = None; out.append('  '); i += 2; continue
            out.append('\n' if c == '\n' else ' ')
            i += 1; continue
        if state in ('str', 'chr'):
            if c == '\\':
                out.append(c); out.append(nxt); i += 2; continue
            if (state == 'str' and c == '"') or (state == 'chr' and c == "'"):
                state = None
            out.append(c); i += 1; continue
    return ''.join(out)


def read_j(rel):
    raw = read(rel)
    assert '"""' not in raw, '出现 Java 文本块，strip_comments 需要升级'
    return strip_comments(raw)


def strip_strings(src):
    """把字符串/字符字面量的**内容**替换成空格（保留引号与偏移）。

    判「有没有真调用 Random」时必须连字面量一起剥掉：`"Random.Int(3)"` 是字符串，不是随机数调用。"""
    out = []
    state = None
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if state is None:
            if c == '"':
                state = 'str'; out.append(c); i += 1; continue
            if c == "'":
                state = 'chr'; out.append(c); i += 1; continue
            out.append(c); i += 1; continue
        if c == '\\':
            out.append('  '); i += 2; continue
        if (state == 'str' and c == '"') or (state == 'chr' and c == "'"):
            state = None
            out.append(c); i += 1; continue
        out.append('\n' if c == '\n' else ' ')
        i += 1; continue
    return ''.join(out)


def body_has_random(body):
    """判一段代码里有没有**真的**调用随机数。

    先剥注释、再剥字符串字面量内容：
      · 注释里的 `Random.Int(3)` 只是文案（本工程的核验脚本已多次被自己的注释骗过）；
      · `"Random.Int(3)"` 是字符串，不是在调随机数。
    两道都做过才算「真调用」。（本脚本传进来的 body 一般已由 read_j 剥过注释，这里是防御性的。）"""
    s = strip_strings(strip_comments(body))
    return bool(re.search(r'\bRandom\s*\.', s))


def method_span(src, sig):
    """截出方法体：从签名起到**顶格一层缩进**的收尾 `\\n\\t}` 为止。找不到返回 None。"""
    i = src.find(sig)
    if i == -1:
        return None
    j = src.find('\n\t}', i)
    if j == -1:
        return None
    return src[i:j]


def brace_body(src, start):
    """从 start（某方法的签名起点）按大括号配平截出方法体，返回 (body, end)。"""
    i = src.index('{', start)
    depth = 0
    for j in range(i, len(src)):
        if src[j] == '{':
            depth += 1
        elif src[j] == '}':
            depth -= 1
            if depth == 0:
                return src[start:j + 1], j + 1
    return None, -1


def seq_ok(s, anchors):
    """anchors 按**期望先后**给出；返回 (ok, 位置列表)。

    缺失锚点记 -1 ⇒ 直接 False（比位置，且不允许「缺项也算通过」）。"""
    pos = [s.find(a) for a in anchors]
    if any(p == -1 for p in pos):
        return False, pos
    return all(pos[i] < pos[i + 1] for i in range(len(pos) - 1)), pos


def const_of(src, name):
    """取 `public static final <type> NAME = <value>;` 里的字面量（去掉类型后缀）。"""
    m = re.search(r'public static final \w+ ' + re.escape(name) + r'\s*=\s*([^;]+);', src)
    if not m:
        return None
    return m.group(1).strip()


def callers_of(token):
    """全仓含该 token 的文件相对路径列表（排序去重）。"""
    hits = []
    for dirpath, _d, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            if token in open(p, encoding='utf-8', errors='replace').read():
                hits.append(os.path.relpath(p, SRC).replace('\\', '/'))
    return sorted(set(hits))


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


def method_body(disasm, header_prefix):
    """从 javap -c 输出截出某个方法体的指令列表 [(offset, opcode, rest)]，按代码顺序。"""
    lines = disasm.splitlines()
    start = None
    for i, l in enumerate(lines):
        s = l.strip()
        if s.startswith(header_prefix) and s.endswith(';'):
            if i > 0 and lines[i - 1].strip().startswith('//'):
                continue
            start = i
            break
    if start is None:
        return None
    ins = []
    for l in lines[start + 1:]:
        s = l.strip()
        if not s:
            continue
        if s.endswith(';') and '(' in s and not s[0].isdigit():
            break
        if s.startswith('Code:') or s.startswith('descriptor:') or s.startswith('flags:'):
            continue
        m = re.match(r'^(\d+):\s+(\S+)(.*)$', s)
        if m:
            ins.append((int(m.group(1)), m.group(2), m.group(3)))
    return ins


def first_invoke(ins, needle):
    for (off, _op, rest) in ins:
        if needle in rest:
            return off
    return -1


# ---------------------------------------------------------------- 反例自测
def selftest():
    print('== selftest：确认「比位置」判据不恒真 ==')
    good, _ = seq_ok('AAA_BBB_CCC', ['AAA', 'BBB', 'CCC'])
    chk(good, '正例：AAA→BBB→CCC 顺序判定为真')
    bad, _ = seq_ok('AAA_CCC_BBB', ['AAA', 'BBB', 'CCC'])
    chk(not bad, '反例：把 BBB/CCC 互换 ⇒ 判定为假（抓得住顺序颠倒）')
    miss, pos = seq_ok('AAA_CCC', ['AAA', 'BBB', 'CCC'])
    chk(not miss and -1 in pos, '反例：缺锚点 ⇒ 判定为假（不允许「缺项也算通过」）')

    print('== selftest：确认 strip_comments 保偏移、不吞后续 ==')
    src = 'int a = 1; // 注释 //** 里\nint b = 2; /* 块 */ int c = 3;'
    st = strip_comments(src)
    chk(len(st) == len(src), '剥注释后长度不变（保偏移）')
    chk('int b = 2;' in st and 'int c = 3;' in st, '`//**` 不会吞到文件末尾')
    chk('\n' in st, '换行保留')

    print('== selftest：确认「无 Random」判据不恒真、且不被字符串/注释误伤 ==')
    chk(body_has_random('int x = Random.Int(3);'), '真调用 Random ⇒ 判为「有」')
    chk(not body_has_random('String s = "Random.Int(3)";'), '字符串字面量 ⇒ 判为「无」（不误伤）')
    chk(not body_has_random('// Random.Int(3)\nint x = 1;'), '注释 ⇒ 判为「无」（不误伤）')
    chk(not body_has_random('/* Random.Int(3) */ int x = 1;'), '块注释 ⇒ 判为「无」（不误伤）')
    chk(body_has_random('int y = 1; // ok\nint x = Random.Int(3);'),
        '注释与真调用并存 ⇒ 仍判为「有」（不因为剥注释而漏报）')

    print('== selftest：确认常量断言真的读源码、不是写死 ==')
    t = read_j('Trials.java')
    chk(const_of(t, 'HOD_ELITE_TURNS') == '300', '实读 HOD_ELITE_TURNS = 300')
    fake = re.sub(r'(public static final int HOD_ELITE_TURNS\s*=\s*)300', r'\g<1>100', t, count=1)
    chk(fake != t, '反例样本确实被改动过（否则下面这条无意义）')
    chk(const_of(fake, 'HOD_ELITE_TURNS') == '100', '反例：改成 100 ⇒ 断言随之读到 100（非写死）')
    chk(const_of(t, 'NETZACH_FADE_FAR') == '3', '实读 NETZACH_FADE_FAR = 3')

    print('== selftest：确认调用点计数不是恒真 ==')
    chk(callers_of('Trials.finalAccuracy(') == FINAL_CALLERS,
        '实仓的 finalAccuracy 调用点清单与预期一致（%s）' % FINAL_CALLERS)

    print('\nselftest 结束（上面每一条都应是 [OK]）。\n')


# ---------------------------------------------------------------- ① 源码结构层
def main():
    print('== ① 源码结构层 ==')
    trials = read_j('Trials.java')

    # --- 常量口径 ---
    chk(const_of(trials, 'REGION_BONUS_STEP') == '0.10f',
        'REGION_BONUS_STEP = 0.10f（每区 +10%%）')
    chk(const_of(trials, 'MAX_REGION') == '5', 'MAX_REGION = 5（封顶五区）')
    chk(const_of(trials, 'NETZACH_FADE_NEAR') == '1', 'NETZACH_FADE_NEAR = 1')
    chk(const_of(trials, 'NETZACH_FADE_FAR') == '3', 'NETZACH_FADE_FAR = 3')
    chk(const_of(trials, 'NETZACH_FADE_ALPHA') == '0.5f', 'NETZACH_FADE_ALPHA = 0.5f')
    chk(const_of(trials, 'HOD_ELITE_TURNS') == '300', 'HOD_ELITE_TURNS = 300（普通怪）')
    chk(const_of(trials, 'HOD_ELITE_TURNS_BOSS') == '450', 'HOD_ELITE_TURNS_BOSS = 450（Boss）')
    hod_et = method_span(trials, 'public static int hodEliteTurns( Mob mob ){')
    chk(hod_et is not None, 'Trials.hodEliteTurns 方法体截取成功（唯一取数收口）')
    if hod_et:
        good, pos = seq_ok(hod_et, ['Char.Property.BOSS', 'return HOD_ELITE_TURNS_BOSS;', 'return HOD_ELITE_TURNS;'])
        chk(good, 'hodEliteTurns：Boss 走 450、其余走 300（位置 %s）' % (pos,))

    # --- 区域换算的唯一公式 ---
    region = method_span(trials, 'public static int currentRegion(){')
    chk(region is not None, 'Trials.currentRegion 方法体截取成功')
    if region:
        chk('Dungeon.scalingDepth()' in region and '/ 5' in region and 'MAX_REGION' in region,
            'currentRegion = min(MAX_REGION, max(1, (scalingDepth-1)/5+1))（唯一口径）')
    rbm = method_span(trials, 'public static float regionBonusMultiplier(){')
    chk(rbm is not None and 'REGION_BONUS_STEP' in rbm and 'currentRegion()' in rbm,
        'regionBonusMultiplier = 1 + REGION_BONUS_STEP × currentRegion()（唯一口径）')

    # --- 两个倍率都只认敌方 + 门控 ---
    for sig, name in [('public static float accuracyFactor( Char ch ){', 'accuracyFactor'),
                      ('public static float evasionFactor( Char ch ){', 'evasionFactor')]:
        b = method_span(trials, sig)
        chk(b is not None, 'Trials.%s 方法体截取成功' % name)
        if b:
            chk('Char.Alignment.ENEMY' in b, '%s 只对敌方生效（判 alignment == ENEMY）' % name)
            chk('Dungeon.isTrialled(' in b, '%s 以 Dungeon.isTrialled(...) 门控' % name)
            chk('regionBonusMultiplier()' in b, '%s 走唯一的区域倍率口径' % name)
            chk(not body_has_random(b), '%s 不调用 Random' % name)

    # --- 取数收口：恰好三个调用点 ---
    for token, want, desc in [
            ('Trials.finalAccuracy(', FINAL_CALLERS, '命中取数收口'),
            ('Trials.finalEvasion(', FINAL_CALLERS, '闪避取数收口'),
            ('Trials.enemyFade(', FADE_CALLERS, '淡出系数'),
            ('Trials.grantHodElite(', ['actors/buffs/HodGlory.java'], 'HOD 晋升'),
            ('Trials.bindMobPassives(', ['actors/mobs/Mob.java'], '常驻重挂')]:
        got = callers_of(token)
        chk(got == want, '%s 的调用点恰好 %s（实测 %s）' % (desc, want, got))

    # --- 取数收口本身：内部只乘自家倍率，不碰 Random ---
    for sig, name, factor in [
            ('public static float finalAccuracy( Char attacker, Char defender ){', 'finalAccuracy', 'accuracyFactor'),
            ('public static float finalEvasion( Char defender, Char attacker ){', 'finalEvasion', 'evasionFactor')]:
        b = method_span(trials, sig)
        chk(b is not None, 'Trials.%s 方法体截取成功' % name)
        if b:
            chk('attackSkill(' in b or 'defenseSkill(' in b,
                '%s 里真的读了单位自身的属性' % name)
            chk(factor + '(' in b, '%s 乘的是 %s' % (name, factor))
            chk(not body_has_random(b), '%s 不调用 Random' % name)

    # --- grantHodElite：不可叠加 + 只经 randomChampionClass 掷一次 ---
    grant = method_span(trials, 'public static boolean grantHodElite( Mob mob ){')
    chk(grant is not None, 'Trials.grantHodElite 方法体截取成功')
    if grant:
        chk('Dungeon.isTrialled( HOD )' in grant, 'grantHodElite 以 Dungeon.isTrialled(HOD) 门控')
        chk('Char.Alignment.ENEMY' in grant, 'grantHodElite 只对敌方生效')
        chk('buffs( ChampionEnemy.class )' in grant and '.isEmpty()' in grant,
            'grantHodElite 的「不可叠加」用 buffs(ChampionEnemy.class).isEmpty() 判定'
            '（⚠️ 不能用 buff(...)：那是精确类匹配，对抽象基类永远 null）')
        chk('ChampionEnemy.randomChampionClass()' in grant,
            'grantHodElite 的随机精英只经 ChampionEnemy.randomChampionClass()')
        chk('Random' not in grant,
            'grantHodElite 自己不直接写 Random（授权点在 randomChampionClass）')

    # --- enemyFade 的两个阈值 + 作用于敌方 ---
    fade = method_span(trials, 'public static float enemyFade( Char ch ){')
    chk(fade is not None, 'Trials.enemyFade 方法体截取成功')
    if fade:
        chk('NETZACH_FADE_NEAR' in fade and 'NETZACH_FADE_FAR' in fade
            and 'NETZACH_FADE_ALPHA' in fade and 'return 0f' in fade,
            'enemyFade 三段阈值齐全（≤NEAR→1；≤FAR→ALPHA；否则 0）')
        chk('Char.Alignment.ENEMY' in fade, 'enemyFade 只对敌方生效')
        chk('Dungeon.level.distance(' in fade, 'enemyFade 用 Dungeon.level.distance（切比雪夫）')
        chk('Dungeon.hero' in fade, 'enemyFade 以英雄为原点，且对 hero 自身返回 1')
        chk('Dungeon.isTrialled(' in fade, 'enemyFade 以 Dungeon.isTrialled(...) 门控')
        chk(not body_has_random(fade), 'enemyFade 不调用 Random')

    # --- bindMobPassives：HOD 的挂载必须排在 CHESED 的 EXP 提前返回之前 ---
    bind = method_span(trials, 'public static void bindMobPassives( Mob mob ){')
    chk(bind is not None, 'Trials.bindMobPassives 方法体截取成功')
    if bind:
        good, pos = seq_ok(bind, ['Dungeon.isTrialled( HOD )', 'HodGlory.class', 'mob.EXP <= 0'])
        chk(good, 'HOD 的挂载在 `mob.EXP <= 0` 提前返回**之前**（否则 EXP=0 的敌拿不到计时器）'
                  '（位置 %s）' % (pos,))
        chk('Dungeon.isTrialled( CHESED )' in bind and 'ChesedMend.class' in bind,
            'CHESED 的原有行为一字未动（仍按 isTrialled(CHESED) + EXP 门控）')
        chk(not body_has_random(bind), 'bindMobPassives 不调用 Random')

    # --- Mob.onAdd 的四步顺序 + 挂载在 firstAdded 守卫之外（CHESED 期就钉过，这里防回归）---
    mob = read_j('actors/mobs/Mob.java')
    onadd = method_span(mob, 'protected void onAdd(){')
    chk(onadd is not None, 'Mob.onAdd 方法体截取成功')
    if onadd:
        good, pos = seq_ok(onadd, [
            'float percent = HP / (float) HT;',
            'HT = Math.round(HT * AscensionChallenge.statModifier(this));',
            'HT = Trials.modifyMobHT( this, HT );',
            'HP = Math.round(HT * percent);',
            'firstAdded = false;',
            'Trials.bindMobPassives( this );',
        ])
        chk(good, 'onAdd 六步顺序正确；重挂仍在 firstAdded 守卫之外（位置 %s）' % (pos,))

    # --- HodGlory：type 必须 POSITIVE；计时→晋升→自摘的顺序 ---
    hg = read_j('actors/buffs/HodGlory.java')
    chk('type = buffType.POSITIVE;' in hg,
        'HodGlory.type = POSITIVE（⚠️ 标 NEGATIVE 会让全图怪物被 Sleeping 判成「被打醒」）')
    chk('buffType.NEGATIVE' not in hg, 'HodGlory 里没出现 NEGATIVE')
    act = method_span(hg, 'public boolean act() {')
    chk(act is not None, 'HodGlory.act 方法体截取成功')
    if act:
        good, pos = seq_ok(act, [
            'Dungeon.isTrialled( Trials.HOD )',
            'Char.Alignment.ENEMY',
            '++turns > Trials.hodEliteTurns( mob )',
            'Trials.grantHodElite( mob );',
            'detach();',
        ])
        chk(good, 'act 顺序：门控(敌方且开着) → 计数越界 → 晋升 → 自摘（位置 %s）' % (pos,))
        chk('turns = 0;' in act, 'act 在「不再适用」时把计时清零（需求：只数存活的回合）')
        chk(not body_has_random(act), 'HodGlory.act 不调用 Random（随机只在 grantHodElite 里）')
    for k in ['storeInBundle', 'restoreFromBundle', 'TURNS']:
        chk(k in hg, 'HodGlory 有 %s（计时随存档往返）' % k)

    # --- ChampionEnemy：随机精英的取值应当只有一处（抽取后 rollForChampion 不能再内联 switch）---
    champ = read_j('actors/buffs/ChampionEnemy.java')
    chk('public static Class<? extends ChampionEnemy> randomChampionClass(){' in champ,
        'ChampionEnemy.randomChampionClass() 存在')
    rcc = method_span(champ, 'public static Class<? extends ChampionEnemy> randomChampionClass(){')
    chk(rcc is not None and rcc.count('Random.Int(6)') == 1,
        'randomChampionClass 只掷一次 Random.Int(6)')
    roll = method_span(champ, 'public static void rollForChampion(Mob m){')
    chk(roll is not None, 'rollForChampion 方法体截取成功')
    if roll:
        chk('randomChampionClass()' in roll, 'rollForChampion 改调 randomChampionClass()（不重复内联 switch）')
        chk('switch' not in roll, 'rollForChampion 里已无内联 switch（六分支只剩一处定义）')
        chk(roll.count('Random.Int(6)') == 0,
            'rollForChampion 不再自己掷 Random.Int(6)（仍恰好一次，只是挪进 randomChampionClass）')
        good, pos = seq_ok(roll, ['Dungeon.mobsToChampion--;', 'randomChampionClass()',
                                  'Dungeon.isChallenged(Challenges.CHAMPION_ENEMIES)'])
        chk(good, 'rollForChampion 的取类仍在「扣额度之后、挑战判定之前」——随机流位置不变（位置 %s）' % (pos,))
    chk(champ.count('switch (Random.Int(6))') == 1,
        '六分支 switch 全仓只剩一处（randomChampionClass 内），rollForChampion 不再内联一份')

    # --- 三处取数点都改成走收口（且不再直接读属性）---
    hit = method_span(read_j('actors/Char.java'),
                      'public static boolean hit( Char attacker, Char defender, float accMulti, boolean magic ) {')
    chk(hit is not None, 'Char.hit 方法体截取成功')
    if hit:
        good, pos = seq_ok(hit, ['Trials.finalAccuracy( attacker, defender )',
                                 'Trials.finalEvasion( defender, attacker )',
                                 'INFINITE_EVASION'])
        chk(good, 'Char.hit 里两次取数都在「无限命中/闪避」短路判定**之前**（位置 %s）' % (pos,))
        chk('attacker.attackSkill( defender )' not in hit and 'defender.defenseSkill( attacker )' not in hit,
            'Char.hit 已不再直接读 attackSkill/defenseSkill（一律走收口）')

    for rel, sig, name in [
            ('actors/hero/Talent.java',
             'public static int dodgeAsDamageReduction( Char attacker, Char defender, int damage ){',
             'Talent.dodgeAsDamageReduction'),
            ('items/armor/glyphs/Stone.java',
             'public int proc(Armor armor, Char attacker, Char defender, int damage) {',
             'Stone.proc')]:
        b = method_span(read_j(rel), sig)
        chk(b is not None, '%s 方法体截取成功' % name)
        if b:
            chk('Trials.finalAccuracy' in b and 'Trials.finalEvasion' in b,
                '%s 也走同一取数收口（否则换算出的概率会与真实命中率漂移）' % name)
            chk('attacker.attackSkill(' not in b and 'defender.defenseSkill(' not in b,
                '%s 已不再直接读 attackSkill/defenseSkill' % name)

    # --- CharSprite.draw：淡出必须在 super.draw 之前乘、之后还原 ---
    spr = read_j('sprites/CharSprite.java')
    draw = method_span(spr, 'public void draw() {')
    chk(draw is not None, 'CharSprite.draw 方法体截取成功')
    if draw:
        good, pos = seq_ok(draw, ['float fade = Trials.enemyFade( ch );',
                                  'am *= fade;',
                                  'super.draw();',
                                  'am = amBak;',
                                  'aa = aaBak;'])
        chk(good, 'draw 顺序：取系数 → 乘上去 → super.draw() → 还原（只影响这一帧）（位置 %s）' % (pos,))
        chk(draw.count('Trials.enemyFade( ch )') == 1, '淡出系数每帧只取一次')
        chk('alpha(' not in draw,
            'draw 里不动 alpha()（那是持久状态，会与隐形的 AlphaTweener 打架）')

    # --- 血条：跟着怪物一起淡出（半透明）＋ 全透明时一并隐藏 ---
    chi = read_j('ui/CharHealthIndicator.java')
    chk('float fade = Trials.enemyFade( target );' in chi and 'setAlpha( fade );' in chi,
        '血条把 enemyFade 喂给 HealthBar.setAlpha（浮空血条也要半透明）')
    chk('visible = (target.HP < target.HT || target.shielding() > 0) && fade > 0f;' in chi,
        '血条 visible 也过一遍 enemyFade（全透明的敌不该被浮空血条暴露）')
    th = read_j('ui/TargetHealthIndicator.java')
    chk('float fade = Trials.enemyFade( target );' in th and 'setAlpha( fade );' in th
        and 'visible = true;' in th and 'visible = fade > 0f' not in th,
        '瞄准血条 TargetHealthIndicator 只压 alpha、**不动 visible**'
        '（ChaoticCenser 拿 instance.isVisible() 当「有没有锁定目标」在读）')

    # ---------------------------------------------------------------- ② 编译产物层
    print('\n== ② 编译产物层（javap -p）==')
    trials_p = javap(['-p', '-cp', CLASSES, TRIALS])
    for name in ['finalAccuracy', 'finalEvasion', 'accuracyFactor', 'evasionFactor',
                 'enemyFade', 'currentRegion', 'regionBonusMultiplier', 'grantHodElite']:
        chk(re.search(r'\b' + name + r'\(', trials_p) is not None,
            '字节码里有 %s（源码层解析可能被注释骗到，这里看真产物）' % name)

    hg_p = javap(['-p', '-cp', CLASSES, HODGLORY])
    chk('class com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HodGlory' in hg_p
        and 'extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff' in hg_p,
        'HodGlory 编译产物存在且 extends Buff')

    ch_p = javap(['-p', '-cp', CLASSES, CHAMP])
    chk('randomChampionClass()' in ch_p, 'ChampionEnemy.randomChampionClass() 已进产物')

    # ---------------------------------------------------------------- ③ 字节码层
    print('\n== ③ 字节码层（javap -c）==')
    trials_c = javap(['-p', '-c', '-cp', CLASSES, TRIALS])
    for sig, name in [
            ('public static int currentRegion();', 'currentRegion'),
            ('public static float regionBonusMultiplier();', 'regionBonusMultiplier'),
            ('public static float accuracyFactor(', 'accuracyFactor'),
            ('public static float evasionFactor(', 'evasionFactor'),
            ('public static float enemyFade(', 'enemyFade'),
            ('public static float finalAccuracy(', 'finalAccuracy'),
            ('public static float finalEvasion(', 'finalEvasion')]:
        body = method_body(trials_c, sig)
        if body is None:
            chk(False, '未能截出 Trials.%s 字节码' % name)
        else:
            has_rand = any('watabou/utils/Random' in rest for (_o, _op, rest) in body)
            chk(not has_rand, 'Trials.%s 字节码无 Random 调用' % name)

    grant_bc = method_body(trials_c, 'public static boolean grantHodElite('
                                     'com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob);')
    if grant_bc is None:
        chk(False, '未能截出 grantHodElite 字节码')
    else:
        i_rcc = first_invoke(grant_bc, 'randomChampionClass')
        has_rand = any('watabou/utils/Random' in rest for (_o, _op, rest) in grant_bc)
        chk(i_rcc != -1, 'grantHodElite 字节码里调了 ChampionEnemy.randomChampionClass')
        chk(not has_rand, 'grantHodElite 字节码里没有直接 Random 调用（随机只经 randomChampionClass）')

    hg_c = javap(['-p', '-c', '-cp', CLASSES, HODGLORY])
    ga = method_body(hg_c, 'public boolean act();')
    if ga is None:
        chk(False, '未能截出 HodGlory.act() 字节码')
    else:
        chk(not any('watabou/utils/Random' in rest for (_o, _op, rest) in ga),
            'HodGlory.act() 字节码无 Random 调用')
        i_grant = first_invoke(ga, 'Trials.grantHodElite')
        i_detach = first_invoke(ga, 'detach:()V')
        chk(i_grant != -1 and i_detach != -1 and i_grant < i_detach,
            '字节码顺序：grantHodElite(@%s) → detach(@%s)' % (i_grant, i_detach))

    hit_c = javap(['-p', '-c', '-cp', CLASSES, CHAR])
    hb = method_body(hit_c, 'public static boolean hit('
                            'com.shatteredpixel.shatteredpixeldungeon.actors.Char, '
                            'com.shatteredpixel.shatteredpixeldungeon.actors.Char, float, boolean);')
    if hb is None:
        chk(False, '未能截出 Char.hit 字节码')
    else:
        i_acu = first_invoke(hb, 'Trials.finalAccuracy')
        i_eva = first_invoke(hb, 'Trials.finalEvasion')
        chk(i_acu != -1 and i_eva != -1, 'Char.hit 字节码里两次取数都在（@%s / @%s）' % (i_acu, i_eva))
        # 无限闪避的比较（getstatic INFINITE_EVASION）必须晚于两次取数
        i_inf = -1
        for (off, _op, rest) in hb:
            if 'INFINITE_EVASION' in rest:
                i_inf = off
                break
        chk(i_inf != -1 and i_acu < i_inf and i_eva < i_inf,
            '字节码：取数(@%s/@%s) 先于无限闪避判定(@%s)' % (i_acu, i_eva, i_inf))

    spr_c = javap(['-p', '-c', '-cp', CLASSES, SPRITE])
    db = method_body(spr_c, 'public void draw();')
    if db is None:
        chk(False, '未能截出 CharSprite.draw 字节码')
    else:
        i_call = first_invoke(db, 'Trials.enemyFade')
        # super.draw() 编成 invokespecial <直接父类>.draw:()V（CharSprite 的直接父类是 MovieClip，
        # 所以不是 Image.draw）⇒ 按「invokespecial 到某个 draw:()V」来找，别写死父类名。
        i_super = -1
        for (off, op, rest) in db:
            if op == 'invokespecial' and 'draw:()V' in rest:
                i_super = off
                break
        chk(i_call != -1 and i_super != -1, 'draw 字节码里取系数(@%s) 与 super.draw(@%s) 都在' % (i_call, i_super))
        if i_call != -1 and i_super != -1:
            # am / aa 的写回（还原）应当出现在 super.draw 之后。⚠️ 用词边界匹配：'frame' 里也含 'am'
            writes = [off for (off, op, rest) in db
                      if 'putfield' in op and re.search(r'\b(am|aa)\b', rest)]
            restores = [off for off in writes if off > i_super]
            chk(len(restores) >= 2,
                'draw 字节码在 super.draw 之后把 am/aa 都写回（还原；@%s）' % (restores,))

    # ---------------------------------------------------------------- ④ 文本层
    print('\n== ④ 文本层（zh + en）==')
    for fname, keys in [
            ('misc_zh.properties', {'trials.netzach_desc': ['区域', '半透明', '完全不可见', '10%', '50%'],
                                    'trials.hod_desc': ['区域', '精英', '300', '450', '不可叠加', '10%', '50%']}),
            ('misc.properties', {'trials.netzach_desc': ['region', 'translucent', 'invisible', '10%', '50%'],
                                 'trials.hod_desc': ['region', 'champion', '300', '450', 'stack', '10%', '50%']})]:
        raw = open(os.path.join(MSG, fname), 'rb').read()
        text = raw.decode('utf-8')
        chk(raw.count(b'\r\n') == raw.count(b'\n'), '%s 仍是纯 CRLF（无裸 LF）' % fname)
        # 按 CRLF 断行再找键（不要用 `$` 锚：那会把行尾的 \r 一并吃进捕获组）
        found = {}
        for ln in text.split('\r\n'):
            for k in keys:
                if ln.startswith(k + '='):
                    found[k] = ln[len(k) + 1:]
        for key, needles in keys.items():
            chk(key in found, '%s: 存在 %s' % (fname, key))
            if key not in found:
                continue
            body = found[key]
            chk('占位' not in body and 'Placeholder' not in body,
                '%s: %s 已不是占位文案' % (fname, key))
            missing = [n for n in needles if n not in body]
            chk(not missing, '%s: %s 含关键要素 %s' % (fname, key, needles))
            chk('\r' not in body and '\n' not in body,
                '%s: %s 的换行是**字面 \\n**（不是真换行）' % (fname, key))
            chk('\\n\\n' in body, '%s: %s 用 \\n\\n 分段' % (fname, key))

    # ---------------------------------------------------------------- ⑤ 行为层
    print('\n== ⑤ 行为层（HodNetzachProbe 的实证输出）==')
    if not os.path.exists(PROBE_OUT):
        chk(False, '探针输出 %s 不存在（先跑 bash _chk/_build_hod_netzach.sh run）' % os.path.basename(PROBE_OUT))
    else:
        out = open(PROBE_OUT, encoding='utf-8', errors='replace').read()
        chk('[FAIL]' not in out, '探针输出里没有任何 [FAIL]')
        m = re.search(r'全部符合预期（(\d+) 条断言）', out)
        chk(m is not None, '探针是「全部符合预期」结尾')
        if m:
            chk(int(m.group(1)) >= 45, '探针断言数 %s ≥ 45（七段都真的跑到了）' % m.group(1))
        for sec in ['① 区域换算与倍率', '② 命中 / 闪避倍率的门控', '③ 取数收口',
                    '④ enemyFade 的距离阈值', '⑤ grantHodElite', '⑥ HodGlory 计时器的边界',
                    '⑦ randomChampionClass']:
            chk(sec in out, '探针跑了「%s」这一节' % sec)
        chk('[INFO]' not in out, '探针没有被 try/catch 吞掉的段（无 [INFO]）')

    # ---------------------------------------------------------------- 收尾
    print('\n' + '=' * 62)
    if ok:
        print('全部核验通过。')
    else:
        print('存在失败项，见上方 [FAIL]。')
    print('=' * 62)
    return 0 if ok else 1


if __name__ == '__main__':
    if '--selftest' in sys.argv:
        selftest()
        sys.exit(0 if ok else 1)
    sys.exit(main())
