# 核验考验「CHESED（慈悲）」与「GEBURA（严厉）」的实现。
#
# 为什么需要它：这两条考验和 HOKMA/BINAH 是**第三类**规则 —— 它们不改道具，而是**改敌方单位**：
#   · CHESED = 「入场时改生命上限」+「一只常驻计时 buff」。收口点是 Mob.onAdd()（原版给
#     AscensionChallenge 改血用的「首次入场」唯一收口），以及挂在那里的 ChesedMend。
#   · GEBURA = 「致死闸门上一问」。收口点是 Char.damage 里 checkBypass 与 `if(!isAlive())`
#     之间的那一行 —— 这是**最晚的可撤回点**，早一行晚一行都会改变原版战续/子类 die() 的行为。
#
# 所以核验不能只看「函数写没写」，必须证明：
#   · 收口**清单吻合**（全仓的 modifyMobHT / bindMobPassives / interceptLethalDamage 调用点必须
#     恰好等于预期的那几个文件；GEBURA 有 **3 个**调用点：Char.damage ＋ Brute/ArmoredBrute
#     战续耗尽那两处 —— 战续是靠覆写 isAlive() 撑住的，狂暴期间闸门天然问不到它，
#     但护盾耗尽那一刻是 BruteRage 自己调 die(null)，绕开了 Char.damage，所以要单独收口）；
#   · **位置正确**（+25% 夹在挑战倍率之后、HP 回算之前；重挂放在 firstAdded 守卫之外；
#     致死判定夹在 checkBypass 之后、isAlive() 之前；战续那两处夹在 die(null) 之前且被 if 包住）；
#   · **不靠白名单**（Brute 战续期间靠覆写 isAlive() 自动躲开、Ghoul 有自己的 deathIsDeferred 判据）；
#   · **随机流一点没动**（不碰 Random ⇒ 关卡种子布局不漂移）；
#   · **锁血那一击的僵直**（2026-09-24 削弱）：豁免发下的同一回合它**不行动**（愣过一个回合才恢复）。
#     它是「Mob 上的第三个瞬态标记」：Trials 置位、Mob.act() 最前面一次性消费 ⇒ 两条要害：
#     ① **只消费一次**（问过即清，忘了清就变成永久眩晕）；② **消费点必须在 chooseEnemy() /
#     state.act() 之前**（放到后面去，「这一回合不行动」就形同虚设）。
#
# 分六层：
#   ① 源码结构层：常量值、收口唯一性、各处「比位置」的顺序断言、buff 语义（POSITIVE、不写死数值）。
#   ② 编译产物层：javap -p 看签名 —— 源码解析可能被注释骗到，这里看真编出来的东西。
#   ③ 字节码层：javap -c 看指令 —— 证明 Char.damage 里 interceptLethalDamage 真的**先于**死亡判定
#      （`if (!isAlive())`）执行，且四个 Trials 钩子与两个 buff 的方法体里**没有** Random 调用。
#      这一层专门防「顺序写反」「偷偷用了 Random」，是纯源码断言抓不到的。
#   ④ 存档层：两个 buff 的往返字段；两个瞬态标记**不得**进存档（缓存重建才是设计）。
#   ⑤ 行为层：读 _chk/_cgprobe.out（ChesedGeburaProbe 的实证输出）。行为问题只有真跑才算数，
#      本脚本只负责「那份证据确实在、且全绿」。
#   ⑥ 边界层：谁覆写了 act() 却**不调** super.act() ⇒ 绕过僵直消费点。清单逐字吻合；全库唯一
#      「ENEMY 且 EXP>0 的绕过者」是 YogDzewa（EXP=50），已与用户确认按**既定边界**处理、不开特判。
#
# 用法：
#   python _chk/verify_chesed_gebura.py            # 需先按 skill 的单文件 javac 命令编到 _chk/_javachk
#   python _chk/verify_chesed_gebura.py --selftest # 反例自测：证明「顺序」「无 Random」判据不恒真
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'
PROBE_OUT = os.path.join(HERE, '_cgprobe.out')

HEADER = 'com.shatteredpixel.shatteredpixeldungeon'

TRIALS = HEADER + '.Trials'
MOB = HEADER + '.actors.mobs.Mob'
CHAR = HEADER + '.actors.Char'
GHOUL = HEADER + '.actors.mobs.Ghoul'
BRUTE_RAGE = HEADER + '.actors.mobs.Brute$BruteRage'
ARMORED_RAGE = HEADER + '.actors.mobs.ArmoredBrute$ArmoredRage'
CHESED_MEND = HEADER + '.actors.buffs.ChesedMend'
GEBURA_GRACE = HEADER + '.actors.buffs.GeburaGrace'

# GEBURA 闸门的**全部**调用点（全仓必须恰好这三个，多一个少一个都是回归）
GEBURA_CALLERS = ['actors/Char.java', 'actors/mobs/ArmoredBrute.java', 'actors/mobs/Brute.java']

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def strip_comments(src):
    """把注释替换成等长空格（保留换行）——**保留字符偏移**，所以仍能「比位置」。

    单趟状态机，识别 // 、/* */ 、字符串 "" 、字符 ''。
    不能用正则去 /*...*/ ：注释里出现 `//**` 时正则会一路吞到文件末尾
    （项目里已踩过，`check_unused_imports.py` 同款做法）。
    工程内无 Java 文本块（已核），故不必处理 \"\"\"。"""
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
    """读源码并**剥掉注释**，用于「必须含 / 必须不含 / 比位置」这类断言。

    ⚠️ 这是本项目反复踩过的坑：注释里写的伪代码会被 `in` 匹配到，
    于是断言在「代码根本没写」时也报 OK（或反过来误报 FAIL）。"""
    raw = read(rel)
    assert '"""' not in raw, '出现 Java 文本块，strip_comments 需要升级'
    return strip_comments(raw)


def method_span(src, sig):
    """截出方法体：从签名起到**顶格一层缩进**的收尾 `\\n\\t}` 为止。

    本工程方法体缩进为两个 tab，收尾为 `\\n\\t}`；嵌套块的收尾是 `\\n\\t\\t}`，
    不含子串 `\\n\\t}`，所以不会被提前截断。找不到返回 None。"""
    i = src.find(sig)
    if i == -1:
        return None
    j = src.find('\n\t}', i)
    if j == -1:
        return None
    return src[i:j]


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


def method_body(disasm, header_prefix):
    """从 javap -c 输出里截出某个方法体的指令列表（到下一个方法声明或结尾）。

    返回 [(offset, opcode, rest)]，**按代码顺序**排列（javap 就是按代码顺序打印的）。
    找不到方法返回 None。"""
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
            break          # 下一个方法声明
        if s.startswith('Code:') or s.startswith('descriptor:') or s.startswith('flags:'):
            continue
        m = re.match(r'^(\d+):\s+(\S+)(.*)$', s)
        if m:
            ins.append((int(m.group(1)), m.group(2), m.group(3)))
    return ins


# ---------------------------------------------------------------- 判据（供 selftest 复用）
def seq_ok(s, anchors):
    """anchors 按**期望先后**给出；返回 (ok, 位置列表)。

    缺失的锚点位置记 -1 ⇒ 直接 False（比位置，且不允许「缺项也算通过」）。
    纯「存在性」断言抓不出顺序写反：把两个锚点互换，两边都还在，只是先后错了。"""
    pos = [s.find(a) for a in anchors]
    if any(p == -1 for p in pos):
        return False, pos
    return all(pos[i] < pos[i + 1] for i in range(len(pos) - 1)), pos


def strip_strings(src):
    """把字符串/字符字面量的**内容**替换成空格（保留引号与偏移）。

    判「有没有真调用 Random」时必须连字面量一起剥掉：`"Random.Int(3)"` 是字符串，
    不是在调随机数；不剥就是**假阳性**（把无害的文案当成偷用随机数）。"""
    out = []
    state = None          # None | 'str' | 'chr'
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ''
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
    """方法体里有没有**真的调用** Random（body 必须 already 剥注释）。

    故意找 `Random.` 这种调用形态，且先剥掉字符串/字符字面量（`"Random.Int(3)"`
    只是一段文案，不是在调随机数）。本工程对随机数的用法一律是 `Random.Int/ Float/ Long/ chances/ ...`。"""
    return re.search(r'\bRandom\s*\.', strip_strings(body)) is not None


def first_invoke(ins, needle):
    """字节码里第一条 rest 含 needle 的指令的下标（无则 -1）。"""
    for idx, (_o, _op, rest) in enumerate(ins or []):
        if needle in rest:
            return idx
    return -1


def putfield_then_iconst1(ins):
    """找「写字段（putfield）」与「返回 true（iconst_1）」两个指令的下标（无则 -1）。

    ⚠️ javap 的行是 `<offset>: <opcode> <操作数>`，`method_body` 拆出来的 `rest` **不含 opcode**
    ⇒ 拿 `first_invoke(..., 'putfield')` 去 rest 里找会**恒返 -1**（本项目已踩：一条假 FAIL，
    看着像「忘了清标记」）。这里一律比 **opcode 字段**。"""
    i_put = i_true = -1
    for idx, (_o, op, _rest) in enumerate(ins or []):
        if i_put == -1 and op == 'putfield':
            i_put = idx
        if i_true == -1 and op == 'iconst_1':
            i_true = idx
    return i_put, i_true


def clears_before_return_true(ins):
    """`putfield`（清标记）必须在 `iconst_1`（return true）**之前**。"""
    i_put, i_true = putfield_then_iconst1(ins)
    return i_put != -1 and i_true != -1 and i_put < i_true


def brace_body(src, start):
    """从 '{' 的下标起按**大括号配平**取方法体。

    ⚠️ 不能按行取（如「取到下一个 `}` 为止」）：方法体是多行的，正则按行会在第一个
    内层 `}` 就截断，于是「体里有没有 super.act()」这个判据就变成随机数。
    """
    i, depth = start, 0
    while i < len(src):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return src[start:i + 1]
        i += 1
    return src[start:]


def top_level_act_bodies(src):
    """取「**顶格**（恰好 1 个 tab）」的 `protected boolean act()` 方法体。

    ⚠️ 只认顶格是为了躲开**匿名内部类**：Tengu 里那个「跳跃」用的 Actor 也写了 act()，
    但缩进更深；把它的「没调 super.act()」算进来就是**假阳性**（写一次性扫描脚本时已踩过）。
    """
    return [brace_body(src, m.end() - 1)
            for m in re.finditer(r'(?m)^\tprotected boolean act\(\)\s*\{', src)]


def scan_act_overrides():
    """全仓扫一遍：谁覆写了 act()、谁**没调** super.act()。返回 (绕过者, 调了 super 的)。"""
    byp, calls = {}, {}
    for dirpath, _dirs, files in os.walk(SRC):
        for fn in files:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            rel = os.path.relpath(p, SRC).replace(os.sep, '/')
            src = open(p, encoding='utf-8').read()
            for body in top_level_act_bodies(src):
                d = calls if 'super.act()' in body else byp
                d[rel] = d.get(rel, 0) + 1
    return byp, calls


# GEBURA 闸门的 return 计数期望：1 个 true（真接管）+ 7 个 false（每一处 false 都精确对应
# 「原版那句 die() 该照常执行」）。
GATE_EXPECT_FALSE = 7


def selftest():
    print('== selftest：确认「比位置」判据不恒真 ==')
    good = ('if (HP < 0) HP = 0;'
            'GritTeethBuff.checkBypass( this, src );'
            'Trials.interceptLethalDamage( this, src );'
            'if (!isAlive()) {')
    bad = ('if (HP < 0) HP = 0;'
           'Trials.interceptLethalDamage( this, src );'
           'GritTeethBuff.checkBypass( this, src );'
           'if (!isAlive()) {')
    missing = ('GritTeethBuff.checkBypass( this, src );'
               'if (!isAlive()) {')
    anchors = ['GritTeethBuff.checkBypass( this, src );',
               'Trials.interceptLethalDamage( this, src );',
               'if (!isAlive()) {']
    cases = [
        (good, True, '正确顺序：checkBypass → GEBURA → 死亡判定'),
        (bad, False, 'GEBURA 插在 checkBypass 之前（位置错，必须抓出来）'),
        (missing, False, 'GEBURA 整段缺失（缺失也算 FAIL）'),
    ]
    for s, want, desc in cases:
        got, pos = seq_ok(s, anchors)
        print('  %-44s -> %-5s（期望 %-5s） %s' % (desc, got, want, pos))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)
    print('  selftest 通过：抓得住顺序颠倒与锚点缺失。')

    print('== selftest：确认「无 Random」判据不恒真 ==')
    rc = [('return super.isAlive() || geburaGrace;', False, '真代码（不含 Random）'),
          ('mob.HP += Random.Int( 3 );', True, '偷用了 Random（必须抓出来）'),
          ('int x = Random.chances( probs );', True, '偷用了 Random.chances'),
          ('String s = "Random.Int(3)";', False, '字符串里出现（不算调用）')]
    for body, want, desc in rc:
        got = body_has_random(body)
        print('  %-44s -> %-5s（期望 %-5s）' % (desc, got, want))
        assert got == want, '判据错：%s' % desc
    print('  selftest 通过：抓得住偷用随机数，且不被字符串误伤。')

    print('== selftest：确认 strip_comments 保偏移、不吞代码 ==')
    sc = [
        ('int a = 1; // if (x) y = 0;\nint b = 2;\n', 'int b = 2;', '行注释被剥'),
        ('/* if (x) y = 0; */int b = 2;\n', 'int b = 2;', '块注释被剥'),
        ('//** 注释里带星号 */ 之后\nint b = 2;\n', 'int b = 2;', '`//**` 不吞后续'),
        ('String s = "// 不是注释";int b = 2;\n', '// 不是注释', '字符串里的 // 保留'),
        ('char q = \'/\'; int b = 2;\n', 'int b = 2;', "字符字面量不误开注释"),
    ]
    for src, must_keep, desc in sc:
        got = strip_comments(src)
        assert len(got) == len(src), '偏移没保住：%s' % desc
        assert must_keep in got, '被误剥：%s' % desc
        if 'if (x) y = 0;' in src and '//**' not in src:
            assert 'if (x) y = 0;' not in got, '注释内容没剥掉：%s' % desc
        print('  %-44s -> OK' % desc)
    print('  selftest 通过：剥注释且字符偏移不变。\n')

    print('== selftest：确认「僵直消费点先于 AI」与「先清后返回」判据不恒真 ==')

    def mk_ins(*items):
        """造一份 javap 风格的指令表（只用到 first_invoke 的 needle 匹配）。"""
        return [(i * 3, op, rest) for i, (op, rest) in enumerate(items)]

    def ai_order_ok(ins):
        i_c = first_invoke(ins, 'consumeGeburaStagger')
        i_ch = first_invoke(ins, 'chooseEnemy')
        i_ai = first_invoke(ins, 'Mob$AiState.act')
        return i_c != -1 and i_ch != -1 and i_ai != -1 and i_c < i_ch < i_ai

    head = [('invokevirtual', 'Method spend:(F)V'),
            ('invokevirtual', 'Method consumeGeburaStagger:()Z')]
    tail = [('invokevirtual', 'Method chooseEnemy:()Lcom/x/Char;'),
            ('invokeinterface', 'InterfaceMethod Mob$AiState.act:(ZZ)Z')]
    for ins, want, desc in [
            (mk_ins(*(head + tail)), True, '正确：僵直消费在 chooseEnemy/AiState.act 之前'),
            (mk_ins(*(tail + head)), False,
             '错位：消费点被挪到 AI 之后（「本回合不行动」就形同虚设）'),
            (mk_ins(*(head[:1] + tail)), False, '消费点整段缺失（缺项也算 FAIL）')]:
        got = ai_order_ok(ins)
        print('  %-46s -> %-5s（期望 %-5s）' % (desc, got, want))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)

    def consume_clears_ok(ins):
        return clears_before_return_true(ins)

    for ins, want, desc in [
            (mk_ins(('getfield', 'Field geburaStagger:Z'), ('ifne', '9'),
                    ('iconst_0', ''), ('ireturn', ''),
                    ('iconst_0', ''), ('putfield', 'Field geburaStagger:Z'),
                    ('iconst_1', ''), ('ireturn', '')),
             True, '真代码形态：先 putfield 清标记、再 iconst_1 返回 true'),
            (mk_ins(('getfield', 'Field geburaStagger:Z'), ('ifne', '9'),
                    ('iconst_0', ''), ('ireturn', ''),
                    ('iconst_1', ''), ('ireturn', '')),
             False, '反例：忘了清标记（没有 putfield）⇒ 永久眩晕，必须抓出来')]:
        got = consume_clears_ok(ins)
        print('  %-46s -> %-5s（期望 %-5s）' % (desc, got, want))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)
    print('  selftest 通过：抓得住「消费点后置」与「忘了清标记」，且不误伤正确形态。\n')

    print('== selftest：确认「只认顶格 act()」判据不恒真 ==')
    # 三段：① 顶格、不调 super.act()；② 顶格、调了 super.act()；③ **缩进更深**的匿名内部类 act()
    # —— ③ 必须被忽略，否则全仓清单里会多出一堆假阳性（一次性扫描脚本就是被它骗过）。
    src = ('public class X extends Mob {\n'
           '\tprotected boolean act() {\n'
           '\t\tenemy = chooseEnemy();\n'
           '\t\treturn true;\n'
           '\t}\n'
           '\tprotected boolean act() {\n'
           '\t\tsuper.act();\n'
           '\t\treturn true;\n'
           '\t}\n'
           '\tvoid f() {\n'
           '\t\tActor a = new Actor() {\n'
           '\t\t\tprotected boolean act() {\n'
           '\t\t\t\treturn true;\n'
           '\t\t\t}\n'
           '\t\t};\n'
           '\t}\n'
           '}\n')
    bodies = top_level_act_bodies(src)
    n_byp = sum(1 for b in bodies if 'super.act()' not in b)
    n_calls = sum(1 for b in bodies if 'super.act()' in b)
    print('  %-46s -> 顶格 %d 个（不调 super %d / 调 %d），期望 2 / 1 / 1'
          % ('顶格 + 匿名内部类混排', len(bodies), n_byp, n_calls))
    assert len(bodies) == 2 and n_byp == 1 and n_calls == 1, \
        '判据错：应只认 2 个顶格 act()，实得 %d（不调 %d / 调 %d）' % (len(bodies), n_byp, n_calls)
    print('  selftest 通过：匿名内部类的 act() 不会被误算成「绕过者」。\n')


print('=' * 78)
print('核验：考验 CHESED（慈悲） + GEBURA（严厉）')
print('=' * 78)

if '--selftest' in sys.argv:
    selftest()


# ================================================================ ① 源码结构层
print('== ① 源码结构层 ==')

trials = read_j('Trials.java')
mob = read_j('actors/mobs/Mob.java')
char = read_j('actors/Char.java')
ghoul = read_j('actors/mobs/Ghoul.java')
mend = read_j('actors/buffs/ChesedMend.java')
grace = read_j('actors/buffs/GeburaGrace.java')

# --- 位号与常量 ---
chk(re.search(r'public static final int CHESED\s*=\s*8\s*;', trials) is not None,
    'Trials.CHESED == 8（位号与 MASKS 顺序一致）')
chk(re.search(r'public static final int GEBURA\s*=\s*16\s*;', trials) is not None,
    'Trials.GEBURA == 16（位号与 MASKS 顺序一致）')

chk(re.search(r'public static final float CHESED_HP_MULT\s*=\s*1\.25f\s*;', trials) is not None,
    'CHESED_HP_MULT == 1.25f（+25%）')
chk(re.search(r'public static final int CHESED_MEND_TURNS\s*=\s*5\s*;', trials) is not None,
    'CHESED_MEND_TURNS == 5（每 5 回合）')
chk(re.search(r'public static final float CHESED_MEND_PERCENT\s*=\s*0\.10f\s*;', trials) is not None,
    'CHESED_MEND_PERCENT == 0.10f（10%，口径集中在此，可随时调）')

# --- 收口唯一性：调用点清单必须**逐字**吻合 ---
for call, expect_files, desc in [
        ('Trials.modifyMobHT(', ['actors/mobs/Mob.java'], 'CHESED 生命上限'),
        ('Trials.bindMobPassives(', ['actors/mobs/Mob.java'], 'CHESED 常驻重挂'),
        ('Trials.interceptLethalDamage(', GEBURA_CALLERS, 'GEBURA 致死判定')]:
    hits = []
    for dirpath, _d, files in os.walk(SRC):
        for fn in files:
            if fn.endswith('.java'):
                p = os.path.join(dirpath, fn)
                rel = os.path.relpath(p, SRC).replace(os.sep, '/')
                if call in strip_comments(open(p, encoding='utf-8').read()):
                    hits.append(rel)
    chk(sorted(hits) == sorted(expect_files), '%s 的调用点是全仓**唯一清单**（期望 %s，实得 %s）'
        % (desc, expect_files, sorted(hits) if hits else '无'))

# --- CHESED 生命上限：位置在挑战倍率之后、HP 回算之前；重挂在 firstAdded 守卫之外 ---
onadd = method_span(mob, 'protected void onAdd(){')
chk(onadd is not None, 'Mob.onAdd() 方法体截取成功')
if onadd is not None:
    good, pos = seq_ok(onadd, [
        'float percent = HP / (float) HT;',
        'HT = Math.round(HT * AscensionChallenge.statModifier(this));',
        'HT = Trials.modifyMobHT( this, HT );',
        'HP = Math.round(HT * percent);',
        'firstAdded = false;',
        'Trials.bindMobPassives( this );',
    ])
    chk(good, f'onAdd 的四步顺序正确：取比例 → 挑战倍率 → CHESED 放大 → 回算 HP；'
              f'重挂在 firstAdded 守卫之外（位置 {pos}）')
    chk('Trials.bindMobPassives( this );' in onadd, 'onAdd 里挂了常驻状态（CHESED 回血计时器）')

# --- GEBURA 致死闸门：位置在 checkBypass 之后、isAlive() 死亡判定之前（本次需求的核心）---
damage = method_span(char, 'public void damage( int dmg, Object src ) {')
chk(damage is not None, 'Char.damage() 方法体截取成功')
if damage is not None:
    good, pos = seq_ok(damage, [
        'if (HP < 0) HP = 0;',
        'GritTeethBuff.checkBypass( this, src );',
        'Trials.interceptLethalDamage( this, src );',
        'if (!isAlive()) {',
    ])
    chk(good, '致死判定「靠后」且夹在免死判据与死亡判定之间：'
              'HP 归零 → checkBypass → GEBURA → if(!isAlive())（位置 %s）' % pos)

# --- Mob 的三个钩子 ---
chk(re.search(r'public boolean isAlive\(\)\s*\{\s*return super\.isAlive\(\) \|\| geburaGrace;\s*\}',
              mob) is not None,
    'Mob.isAlive() = super.isAlive() || geburaGrace（血量停在 0 也不倒）')
chk(re.search(r'public boolean isInvulnerable\( Class effect \)\s*\{\s*'
              r'return geburaGrace \|\| super\.isInvulnerable\( effect \);\s*\}', mob) is not None,
    'Mob.isInvulnerable() = geburaGrace || super...（复用原版无敌分支）')
chk(re.search(r'public boolean deathIsDeferred\( Object cause \)\s*\{\s*return false;\s*\}',
              mob) is not None,
    'Mob.deathIsDeferred() 默认 false（真死）—— 战续单位才覆写')
chk(re.search(r'public boolean geburaGrace = false;', mob) is not None,
    'geburaGrace 是 public 字段（Trials 与两个 buff 都要跨包读写）')
chk(re.search(r'public boolean geburaUsed = false;', mob) is not None,
    'geburaUsed 是 public 字段（一次性门闩，跨包读写）')
mob_isalive = method_span(mob, 'public boolean isAlive() {')
chk(mob_isalive is not None and 'buff(' not in mob_isalive,
    'isAlive() 里不现查 buff(...)（原版注明它会在绘制期被调用 ⇒ 只能读缓存）')

# --- GEBURA 削弱（2026-09-24）：锁血那一击的僵直 = 一次性标记 + 唯一消费点 ---
chk(re.search(r'public boolean geburaStagger = false;', mob) is not None,
    'geburaStagger 是 public 字段（Trials 跨包置位、Mob.act 消费）')

stagger = method_span(mob, 'public boolean consumeGeburaStagger(){')
chk(stagger is not None, 'Mob.consumeGeburaStagger() 方法体截取成功')
if stagger is not None:
    good, pos = seq_ok(stagger, [
        'if (!geburaStagger) return false;',
        'geburaStagger = false;',
        'return true;',
    ])
    chk(good, '一次性语义：没标记直接 false → **先清标记** → 再 return true（位置 %s）' % pos)
    chk(not body_has_random(stagger), 'consumeGeburaStagger 不调用 Random')

mob_act = method_span(mob, 'protected boolean act() {')
chk(mob_act is not None, 'Mob.act() 方法体截取成功')
if mob_act is not None:
    good, pos = seq_ok(mob_act, [
        'if (paralysed > 0) {',
        'if (consumeGeburaStagger()) {',
        'enemy = chooseEnemy();',
        'boolean result = state.act( enemyInFOV, justAlerted );',
    ])
    chk(good, '僵直消费点位置正确：paralysed 之后、chooseEnemy()/state.act() **之前**'
              '（放到后面去就等于没做；位置 %s）' % pos)
    chk(mob_act.count('consumeGeburaStagger()') == 1,
        'Mob.act() 里消费点**只有一个**（实得 %d 处）' % mob_act.count('consumeGeburaStagger()'))
    chk(re.search(r'if \(consumeGeburaStagger\(\)\) \{\s*\n\s*spend\( TICK \);\s*\n\s*return true;',
                  mob_act) is not None,
        '僵直分支与 paralysed 同型：spend( TICK ) 花掉一整个回合后直接 return true')

# --- Ghoul：判据同源、让路 ---
chk(re.search(r'@Override\s*\n\tpublic boolean deathIsDeferred\( Object cause \)', ghoul) is not None,
    'Ghoul 覆写了 deathIsDeferred()（唯一的战续让路者）')
ghoul_die = method_span(ghoul, 'public void die(Object cause)')
chk(ghoul_die is not None, 'Ghoul.die() 方法体截取成功')
if ghoul_die is not None:
    good, pos = seq_ok(ghoul_die, ['deathIsDeferred(cause)', 'super.die(cause);'])
    chk(good, 'Ghoul.die() 先问 deathIsDeferred(cause) 再 super.die(cause)'
              '（与 GEBURA 的让路判据同源，位置 %s）' % pos)

# --- 两个 buff 的语义 ---
chk(re.search(r'public class ChesedMend extends Buff', mend) is not None,
    'ChesedMend extends Buff')
chk(re.search(r'public class GeburaGrace extends Buff', grace) is not None,
    'GeburaGrace extends Buff')
for name, src in (('ChesedMend', mend), ('GeburaGrace', grace)):
    chk('type = buffType.POSITIVE;' in src,
        '%s 标 POSITIVE（NEGATIVE 会被 Mob.Sleeping 当成「被打醒」⇒ 全图怪物入场就醒）' % name)

# CHESED 回血：满血清计时、回血量现算（不写死数值）
mend_act = method_span(mend, 'public boolean act() {')
chk(mend_act is not None, 'ChesedMend.act() 方法体截取成功')
if mend_act is not None:
    chk('mob.HP >= mob.HT' in mend_act, 'ChesedMend 在满血时清计时（需求：不满血才开始计时）')
    chk('turns = 0;' in mend_act, 'ChesedMend 满血分支把计时归零')
    chk('Trials.CHESED_MEND_TURNS' in mend_act, 'ChesedMend 用 Trials.CHESED_MEND_TURNS 判间隔')
    chk('Trials.CHESED_MEND_PERCENT' in mend_act,
        'ChesedMend 用 Trials.CHESED_MEND_PERCENT 现算回血量（不写死数值）')
    chk(not re.search(r'\*\s*0\.1\d*f', mend_act) and '10f' not in mend_act,
        'ChesedMend 里没有 0.1/10f 这类硬编码回血量')
    chk('mob.alignment != Char.Alignment.ENEMY' in mend_act,
        'ChesedMend 每次现判 alignment（被腐蚀成友方会立刻停回血）')

# GEBURA 计时：先问活着、后摘自己（顺序反了会赖着不死）
grace_act = method_span(grace, 'public boolean act() {')
chk(grace_act is not None, 'GeburaGrace.act() 方法体截取成功')
if grace_act is not None:
    good, pos = seq_ok(grace_act, ['t.isAlive()', 'detach();', 't.die( src );'])
    chk(good, 'GeburaGrace.act() 顺序正确：先问 isAlive() → 再 detach() → 最后 die(src)'
              '（反过来问就永远 false、怪物赖着不死；位置 %s）' % pos)
chk(re.search(r'\(\(Mob\) target\)\.geburaGrace = true;', grace) is not None,
    'GeburaGrace.attachTo() 把缓存置真（读档时随 buff 恢复自动重建）')
chk(re.search(r'\(\(Mob\) target\)\.geburaGrace = false;', grace) is not None,
    'GeburaGrace.detach() 把缓存置假（不留幽灵无敌）')

# --- 随机流：四个 Trials 钩子 + 两个 buff 的关键方法都不碰 Random ---
def trials_body(sig, is_void=True):
    return method_span(trials, sig)

for sig, desc in [
        ('public static int modifyMobHT( Mob mob, int ht ){', 'Trials.modifyMobHT'),
        ('public static void bindMobPassives( Mob mob ){', 'Trials.bindMobPassives'),
        ('public static boolean interceptLethalDamage( Char ch, Object src ){',
         'Trials.interceptLethalDamage')]:
    body = trials_body(sig)
    chk(body is not None, '%s 方法体截取成功' % desc)
    if body is not None:
        chk(not body_has_random(body), '%s 不调用 Random（随机流不变）' % desc)
        chk('Dungeon.isTrialled(' in body, '%s 以 Dungeon.isTrialled(...) 门控' % desc)

# CHESED 回血的闸门：EXP<=0 的敌一律不进回血计时器（中立/盟友/不奖励怪）。
_mend = trials_body('public static void bindMobPassives( Mob mob ){')
chk(_mend is not None and 'mob.EXP <= 0' in _mend, 'CHESED 回血闸门按 mob.EXP <= 0 拦')

# geburaGraceTurns 是**纯口径函数**（门控在它的唯一调用者 interceptLethalDamage 里）：
# 故意不做门控，是为了「换口径只改一处」时不必再复制一遍 Dungeon.isTrialled。
gbt = trials_body('public static int geburaGraceTurns( Mob mob ){')
chk(gbt is not None, 'Trials.geburaGraceTurns 方法体截取成功')
if gbt is not None:
    chk(not body_has_random(gbt), 'Trials.geburaGraceTurns 不调用 Random（随机流不变）')
    chk('mob.EXP' in gbt, 'GEBURA 回合数取的是「固有经验值 mob.EXP」（不是实际结算经验）')
    chk('Dungeon.isTrialled(' not in gbt,
        'geburaGraceTurns 刻意不做门控（门控只在唯一调用者 interceptLethalDamage 里）')
# 它的唯一调用者的公共入口（Char.damage 那处）确实做了门控
geb_caller = method_span(trials, 'public static boolean interceptLethalDamage( Char ch, Object src ){')
chk(geb_caller is not None and 'geburaGraceTurns( mob )' in geb_caller,
    'geburaGraceTurns 的唯一调用者 interceptLethalDamage 已门控')

for name, src, methods in [
        ('ChesedMend', mend, ['public boolean act() {', 'public void storeInBundle( Bundle bundle ) {']),
        ('GeburaGrace', grace, ['public boolean act() {', 'public boolean attachTo( Char target ) {',
                                'public void detach() {'])]:
    for sig in methods:
        body = method_span(src, sig)
        chk(body is not None and not body_has_random(body),
            '%s.%s 不调用 Random' % (name, sig.split('(')[0].replace('public ', '').strip()))

# --- GEBURA 只认敌方（别误伤盟友/中立/英雄）---
geb = method_span(trials, 'public static boolean interceptLethalDamage( Char ch, Object src ){')
chk(geb is not None, 'Trials.interceptLethalDamage 签名是 `public static boolean`（返回是否接管死亡）')
if geb is not None:
    chk('ch.alignment != Char.Alignment.ENEMY' in geb, 'GEBURA 只作用于 ENEMY（英雄盟友/中立不吃）')
    chk('mob.geburaUsed' in geb, 'GEBURA 的豁免是一次性的（用过不再救）')
    chk('mob.deathIsDeferred( src )' in geb, 'GEBURA 对有自有战续的单位让路')
    chk('Dungeon.level.pit[ch.pos]' in geb, 'GEBURA 对「站在深渊上」的致死不豁免')
    # 返回值语义：接管的唯一出口是 return true；其余每一处提前返回都必须 return false
    n_true = len(re.findall(r'return\s+true\s*;', geb))
    n_false = len(re.findall(r'return\s+false\s*;', geb))
    chk(n_true == 1 and n_false == GATE_EXPECT_FALSE,
        'interceptLethalDamage 只有**一个** return true（真接管），其余 %d 处提前返回全为 false'
        '（实得 true=%d / false=%d）—— false 必须精确对应「原版那句 die() 该照常执行」'
        % (GATE_EXPECT_FALSE, n_true, n_false))
    # 僵直标记与豁免**同时**发下，顺序固定：门闩 → 僵直 → 挂 buff
    chk('mob.geburaStagger = true;' in geb, '发下豁免的同时置上僵直标记（缺了它就没有削弱）')
    good, pos = seq_ok(geb, ['mob.geburaUsed = true;',
                             'mob.geburaStagger = true;',
                             'Buff.affect( mob, GeburaGrace.class )'])
    chk(good, '顺序：豁免门闩 → 僵直标记 → 挂 GeburaGrace（位置 %s）' % pos)
    chk(geb.count('mob.geburaStagger = true;') == 1,
        '僵直只在**唯一**的接管路径上置位（实得 %d 处 —— 提前返回的分支一条也不许置位）'
        % geb.count('mob.geburaStagger = true;'))

# --- GEBURA 第二、三个调用点：Brute / ArmoredBrute 的战续结束 ---
# 这两处是「靠覆写 isAlive() 撑住的战续」耗尽时的 die(null)，绕开了 Char.damage，所以要单独收口。
# 断言的核心是「守卫写法」：die(null) 必须在 `if (!interceptLethalDamage(...))` 的**内部**，
# 而不是并排再调一次 —— 否则接管后仍会 die，等于白给。
for rel, rage_cls, mob_name in [
        ('actors/mobs/Brute.java', 'BruteRage', '豺狼暴徒'),
        ('actors/mobs/ArmoredBrute.java', 'ArmoredRage', '装甲暴徒')]:
    src = read_j(rel)
    n_die = len(re.findall(r'target\.die\(null\)', src))
    n_hook = len(re.findall(r'Trials\.interceptLethalDamage\( target, null \)', src))
    chk(n_die == 1 and n_hook == 1,
        '%s（%s）：恰好一处 die(null)与一处闸门调用（实得 die=%d / hook=%d）'
        % (mob_name, rage_cls, n_die, n_hook))
    guarded = re.search(
        r'if \(!Trials\.interceptLethalDamage\( target, null \)\)\{\s*\n\s*target\.die\(null\);', src)
    chk(guarded is not None,
        '%s：die(null) 被 `if (!interceptLethalDamage(...))` **包住** ⇒ 被接管时不会把死亡落地'
        % mob_name)
    i_hook = src.find('Trials.interceptLethalDamage( target, null )')
    i_die = src.find('target.die(null)')
    chk(i_hook != -1 and i_die != -1 and i_hook < i_die,
        '%s：闸门在 die(null) **之前**（该路径最靠后的位置），位置 %d < %d' % (mob_name, i_hook, i_die))
    chk(re.search(r'import com\.shatteredpixel\.shatteredpixeldungeon\.Trials;', src) is not None,
        '%s：补了 Trials 的 import' % mob_name)

# ArmoredRage 覆写了 act() ⇒ 它**不会**继承 BruteRage 里那个调用点，必须自己有一处。
# （这条断言防的是「以为继承了就没事」——将来若有人删掉 ArmoredRage.act()，它会立刻由 FAIL 提醒复核。）
armored = read_j('actors/mobs/ArmoredBrute.java')
chk(re.search(r'class ArmoredRage extends Brute\.BruteRage', armored) is not None
    and 'public boolean act() {' in armored,
    '装甲暴徒的 ArmoredRage 覆写了 act() ⇒ 已为它单独接闸门（不能指望继承）')


# ================================================================ ② 编译产物层
print('\n== ② 编译产物层（javap -p） ==')
if not os.path.isdir(CLASSES):
    chk(False, '缺 %s —— 先按 skill 跑单文件 javac' % CLASSES)
else:
    mob_sig = javap(['-p', '-cp', CLASSES, MOB])
    chk(re.search(r'public boolean isAlive\(\)', mob_sig) is not None,
        'Mob 里 isAlive() 是 public（覆写）')
    chk(re.search(r'public boolean isInvulnerable\(java\.lang\.Class', mob_sig) is not None,
        'Mob 里 isInvulnerable(Class) 是 public（覆写）')
    chk(re.search(r'public boolean deathIsDeferred\(java\.lang\.Object\)', mob_sig) is not None,
        'Mob 里 deathIsDeferred(Object) 存在')
    chk(re.search(r'public boolean geburaGrace;', mob_sig) is not None,
        'Mob.geburaGrace 在 class 里是 public 字段')
    chk(re.search(r'public boolean geburaUsed;', mob_sig) is not None,
        'Mob.geburaUsed 在 class 里是 public 字段')

    char_sig = javap(['-p', '-cp', CLASSES, CHAR])
    chk(re.search(r'public void damage\(int, java\.lang\.Object\)', char_sig) is not None,
        'Char.damage(int,Object) 签名未变（外部调用点不受影响）')

    trials_sig = javap(['-p', '-cp', CLASSES, TRIALS])
    chk(re.search(r'public static boolean interceptLethalDamage\(com\.shatteredpixel\.shatteredpixeldungeon'
                  r'\.actors\.Char, java\.lang\.Object\)', trials_sig) is not None,
        'Trials.interceptLethalDamage 编出的返回类型是 boolean')
    chk(re.search(r'public static void interceptLethalDamage', trials_sig) is None,
        'Trials.interceptLethalDamage 不是 void（改回 void 会让 Brute 那侧没法判断「有没有被接管」）')

    for cls, label, sup in [
            (BRUTE_RAGE, 'Brute$BruteRage',
             'extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff'),
            (ARMORED_RAGE, 'ArmoredBrute$ArmoredRage',
             'extends com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute$BruteRage')]:
        rage_sig = javap(['-p', '-cp', CLASSES, cls])
        chk(re.search(r'public boolean act\(\)', rage_sig) is not None,
            '%s.act() 已编出（战续耗尽的收口点）' % label)
        chk(sup in rage_sig,
            '%s 的继承关系没被改动（%s ⇒ 整条链最终仍是 ShieldBuff，护盾语义不变）'
            % (label, sup.replace('extends ', '')))

    ghoul_sig = javap(['-p', '-cp', CLASSES, GHOUL])
    chk(re.search(r'public boolean deathIsDeferred\(java\.lang\.Object\)', ghoul_sig) is not None,
        'Ghoul 的 deathIsDeferred(Object) 已编出')

    mend_sig = javap(['-p', '-cp', CLASSES, CHESED_MEND])
    chk('extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff' in mend_sig,
        'ChesedMend 继承 Buff')
    chk(re.search(r'public boolean act\(\)', mend_sig) is not None, 'ChesedMend.act() 已编出')

    grace_sig = javap(['-p', '-cp', CLASSES, GEBURA_GRACE])
    chk('extends com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff' in grace_sig,
        'GeburaGrace 继承 Buff')
    chk(re.search(r'public boolean attachTo\(com\.shatteredpixel.*Char\)', grace_sig) is not None,
        'GeburaGrace.attachTo(Char) 已编出')
    chk(re.search(r'public void detach\(\)', grace_sig) is not None, 'GeburaGrace.detach() 已编出')
    chk(re.search(r'public void set\(int, java\.lang\.Object\)', grace_sig) is not None,
        'GeburaGrace.set(int,Object) 已编出')


# ================================================================ ③ 字节码层
print('\n== ③ 字节码层（javap -c） ==')
if os.path.isdir(CLASSES):
    char_c = javap(['-p', '-c', '-cp', CLASSES, CHAR])
    dmg = method_body(char_c, 'public void damage(int, java.lang.Object);')
    if dmg is None:
        chk(False, '未能从字节码里截出 Char.damage')
    else:
        i_bypass = first_invoke(dmg, 'GritTeethBuff.checkBypass')
        i_geb = first_invoke(dmg, 'interceptLethalDamage')
        chk(i_geb != -1, 'Char.damage 字节码里确实调用了 Trials.interceptLethalDamage')
        chk(i_bypass != -1 and i_geb != -1 and i_bypass < i_geb,
            '字节码顺序：checkBypass(@%d) 先于 GEBURA(@%d)' % (i_bypass, i_geb))
        # 死亡判定 = intercept 之后**第一条** isAlive 调用
        i_alive_after = -1
        for idx, (_o, _op, rest) in enumerate(dmg):
            if 'isAlive' in rest and idx > i_geb:
                i_alive_after = idx
                break
        chk(i_alive_after != -1,
            '字节码里 GEBURA(@%d) 之后才有死亡判定 isAlive(@%d) ⇒ 「靠后」属实'
            % (i_geb, i_alive_after))

    trials_c = javap(['-p', '-c', '-cp', CLASSES, TRIALS])
    for sig, desc in [
            ('public static int modifyMobHT(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob, int);',
             'modifyMobHT'),
            ('public static void bindMobPassives(com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob);',
             'bindMobPassives'),
            ('public static boolean interceptLethalDamage(com.shatteredpixel.shatteredpixeldungeon.actors.Char, java.lang.Object);',
             'interceptLethalDamage')]:
        body = method_body(trials_c, sig)
        if body is None:
            chk(False, '未能截出 Trials.%s 字节码' % desc)
        else:
            has_rand = any('watabou/utils/Random' in rest for (_o, _op, rest) in body)
            chk(not has_rand, 'Trials.%s 字节码无 Random 调用' % desc)

    # 僵直置位的字节码顺序：豁免门闩 → 僵直 → Buff.affect（与源码层那条互为交叉验证）
    geb_bc = method_body(trials_c, 'public static boolean interceptLethalDamage('
                                   'com.shatteredpixel.shatteredpixeldungeon.actors.Char, '
                                   'java.lang.Object);')
    if geb_bc is None:
        chk(False, '未能截出 Trials.interceptLethalDamage 字节码')
    else:
        i_used = first_invoke(geb_bc, 'Mob.geburaUsed:Z')
        i_stag = first_invoke(geb_bc, 'Mob.geburaStagger:Z')
        i_affect = first_invoke(geb_bc, 'Buff.affect:')
        chk(i_used != -1 and i_stag != -1 and i_affect != -1,
            '字节码里三项都在：geburaUsed(@%s) / geburaStagger(@%s) / Buff.affect(@%s)'
            % (i_used, i_stag, i_affect))
        chk(i_used != -1 and i_stag != -1 and i_affect != -1 and i_used < i_stag < i_affect,
            '字节码顺序：geburaUsed(@%s) → geburaStagger(@%s) → Buff.affect(@%s)'
            % (i_used, i_stag, i_affect))

    grace_c = javap(['-p', '-c', '-cp', CLASSES, GEBURA_GRACE])
    ga = method_body(grace_c, 'public boolean act();')
    if ga is None:
        chk(False, '未能截出 GeburaGrace.act() 字节码')
    else:
        has_rand = any('watabou/utils/Random' in rest for (_o, _op, rest) in ga)
        chk(not has_rand, 'GeburaGrace.act() 字节码无 Random 调用')
        i_alive = first_invoke(ga, 'isAlive')
        i_detach = first_invoke(ga, 'detach:()V')
        i_die = first_invoke(ga, 'Char.die:')
        chk(i_alive != -1 and i_detach != -1 and i_die != -1 and i_alive < i_detach < i_die,
            '字节码顺序：isAlive(@%s) → detach(@%s) → die(@%s)'
            % (i_alive, i_detach, i_die))

    # Brute / ArmoredBrute 的战续结束：字节码层再证一次「闸门先于 die 执行」
    for cls, label in [(BRUTE_RAGE, 'Brute$BruteRage'), (ARMORED_RAGE, 'ArmoredBrute$ArmoredRage')]:
        rage_c = javap(['-p', '-c', '-cp', CLASSES, cls])
        rb = method_body(rage_c, 'public boolean act();')
        if rb is None:
            chk(False, '未能截出 %s.act() 字节码' % label)
        else:
            i_hook = first_invoke(rb, 'interceptLethalDamage')
            i_die = first_invoke(rb, 'Char.die:')
            chk(i_hook != -1, '%s.act() 字节码里确实调用了 GEBURA 闸门' % label)
            chk(i_hook != -1 and i_die != -1 and i_hook < i_die,
                '%s 字节码顺序：闸门(@%s) 先于 die(@%s) ⇒ 接管时 die 落在分支外' % (label, i_hook, i_die))
            has_rand = any('watabou/utils/Random' in rest for (_o, _op, rest) in rb)
            chk(not has_rand, '%s.act() 字节码无 Random 调用（战续结束这步也不动随机流）' % label)

    mob_c = javap(['-p', '-c', '-cp', CLASSES, MOB])
    ia = method_body(mob_c, 'public boolean isAlive();')
    if ia is None:
        chk(False, '未能截出 Mob.isAlive() 字节码')
    else:
        ops = [op for (_o, op, _r) in ia]
        chk(any('geburaGrace' in r for (_o, _op, r) in ia),
            'Mob.isAlive() 字节码读了 geburaGrace 字段')
        chk(any('Char.isAlive' in r for (_o, _op, r) in ia),
            'Mob.isAlive() 字节码调了 super.isAlive()（Char.isAlive，invokespecial）')
        chk('ifne' in ops,
            'Mob.isAlive() 是 `||` 短路（ifne）—— 不是 `|`（ior）那种「两边都算」的写法')
    iv = method_body(mob_c, 'public boolean isInvulnerable(java.lang.Class);')
    if iv is None:
        chk(False, '未能截出 Mob.isInvulnerable() 字节码')
    else:
        chk(any('geburaGrace' in r for (_o, _op, r) in iv),
            'Mob.isInvulnerable() 字节码读了 geburaGrace 字段')

    # GEBURA 僵直（2026-09-24）：消费点必须**先于**任何 AI 调用，否则「这一回合不行动」形同虚设。
    # 这一条只有字节码答得出来 —— 源码里的注释与 javadoc 都可能把「本该在前面」写成假象。
    ma = method_body(mob_c, 'protected boolean act();')
    if ma is None:
        chk(False, '未能截出 Mob.act() 字节码')
    else:
        i_consume = first_invoke(ma, 'consumeGeburaStagger')
        i_choose = first_invoke(ma, 'chooseEnemy')
        i_ai = first_invoke(ma, 'Mob$AiState.act')
        chk(i_consume != -1 and i_choose != -1 and i_ai != -1,
            'Mob.act() 字节码里三项都在：consumeGeburaStagger(@%s) / chooseEnemy(@%s) / AiState.act(@%s)'
            % (i_consume, i_choose, i_ai))
        chk(i_consume != -1 and i_choose != -1 and i_ai != -1 and i_consume < i_choose < i_ai,
            '字节码顺序：僵直消费(@%s) < chooseEnemy(@%s) < AiState.act(@%s) ⇒ 真的「先空过再谈行动」'
            % (i_consume, i_choose, i_ai))
        i_spend_after = -1
        for idx, (_o, _op, rest) in enumerate(ma):
            if 'Method spend:(F)V' in rest and idx > i_consume:
                i_spend_after = idx
                break
        chk(i_spend_after != -1,
            '僵直分支真的花掉了时间（消费调用之后的 spend，@%s）⇒ 是「空过一回合」而不是「原地空转」'
            % i_spend_after)

    cs = method_body(mob_c, 'public boolean consumeGeburaStagger();')
    if cs is None:
        chk(False, '未能截出 Mob.consumeGeburaStagger() 字节码')
    else:
        chk(first_invoke(cs, 'Field geburaStagger:Z') != -1,
            'consumeGeburaStagger 字节码读了 geburaStagger 字段')
        i_put, i_true = putfield_then_iconst1(cs)
        chk(clears_before_return_true(cs),
            'consumeGeburaStagger 字节码：**先 putfield 清标记(@%s) 再 iconst_1 返回 true(@%s)**'
            '—— 反过来（或忘了清）就是「问一次永久眩晕」' % (i_put, i_true))
        chk(sum(1 for (_o, op, _r) in cs if op == 'ireturn') == 2,
            'consumeGeburaStagger 只有两个出口：false / true 各一')


# ================================================================ ④ 存档层
print('\n== ④ 存档层 ==')

for name, src, fields, desc in [
        ('ChesedMend', mend, ['"turns"'], '回血计时'),
        ('GeburaGrace', grace, ['"turns_left"', '"source_class"'], '剩余回合 + 致死来源类型')]:
    chk('storeInBundle( Bundle bundle )' in src and 'restoreFromBundle( Bundle bundle )' in src,
        '%s 覆写了 store/restoreInBundle' % name)
    for f in fields:
        key = f.strip('"')
        chk(('"%s"' % key) in src, '%s 存了字段 %s（%s）' % (name, key, desc))

# 旧档兼容：restoreFromBundle 用 contains 守卫
chk('bundle.contains( SOURCE_CLASS )' in grace,
    'GeburaGrace 读档用 bundle.contains(...) 守卫（旧档缺键不会抛）')

# 两个瞬态标记**不得**进存档（缓存靠 buff 重建，这才是设计）
mob_store = method_span(mob, 'public void storeInBundle( Bundle bundle ) {')
chk(mob_store is not None, 'Mob.storeInBundle() 方法体截取成功')
if mob_store is not None:
    chk('geburaGrace' not in mob_store, 'Mob 不把 geburaGrace 写进存档（是缓存，读档随 buff 重建）')
    chk('geburaUsed' not in mob_store,
        'Mob 不把 geburaUsed 写进存档（用掉豁免的结局一定是真死 ⇒ 不会出现在存档里）')
    chk('geburaStagger' not in mob_store,
        'Mob 不把 geburaStagger 写进存档（同一帧内挂上、下次行动就消费；留着反而会平白多愣一回合）')

mob_restore = method_span(mob, 'public void restoreFromBundle( Bundle bundle ) {')
chk(mob_restore is not None and 'firstAdded = false;' in (mob_restore or ''),
    'Mob.restoreFromBundle() 把 firstAdded 置 false ⇒ 读档不会二次 +25%')


# ================================================================ ⑤ 行为层
print('\n== ⑤ 行为层（ChesedGeburaProbe 的实证输出） ==')
probe_src = os.path.join(HERE, 'ChesedGeburaProbe.java')
chk(os.path.isfile(probe_src), '行为探针 _chk/ChesedGeburaProbe.java 存在')
if not os.path.isfile(PROBE_OUT):
    chk(False, '缺 %s —— 先按 skill 的探针命令编译并运行 ChesedGeburaProbe' % PROBE_OUT)
else:
    txt = open(PROBE_OUT, encoding='utf-8', errors='replace').read()
    chk('[FAIL]' not in txt, '探针输出里没有 [FAIL]')
    m = re.search(r'断言总数 = (\d+)，失败 = (\d+)', txt)
    chk(m is not None, '探针输出含「断言总数 / 失败」汇总行')
    if m:
        chk(int(m.group(2)) == 0, '探针断言全部通过（总数 %s，失败 %s）' % (m.group(1), m.group(2)))
    for sec in ('CHESED · 敌方生命上限 +25%', 'CHESED · 每 5 回合回 10%',
                'GEBURA · 致死闸门', 'GEBURA · 数满 EXP 个回合',
                '门控与「战续让路」', '豺狼暴徒/装甲暴徒：战续（狂暴护盾）结束后仍能吃无敌',
                'GEBURA · 锁血那一击的僵直',
                '不消耗随机数'):
        chk(sec in txt, '探针覆盖到「%s」这一段' % sec)


# ================================================================ ⑥ 边界层
print('\n== ⑥ 边界层：act() 覆写者（GEBURA 僵直的**作用边界**） ==')
#
# 为什么单开一层：僵直的**消费点写在 Mob.act() 最前面**（与 paralysed / FeintConfusion 同款），
# 所以「覆写了 act() 却**不调** super.act()」的单位根本不经过消费点 ⇒ 不会僵直。
# 这是上游自带的同类边界（原版 YogDzewa 的 act() 里压根没有 paralysed 那句 ⇒ 它连瘫痪都不吃），
# **不是本次引入的缺陷**；与用户确认后的口径是「接受为既定边界，不为它开特判」。
# 本层把它坐实成全仓清单：将来新增一个绕过者，断言立刻 FAIL，逼你回答「它是不是 GEBURA 相关」。
#
# ⚠️ 分类是**人肉核对**的，不能靠正则推：阵营/EXP 大量来自**父类实例块**（NPC 统一 NEUTRAL + EXP=0）
#    与**运行时改写**（Pylon.activate() 把 NEUTRAL 改成 ENEMY），正则推出来的只会是错的。

# 全仓「覆写 act() 却不调 super.act()」的清单（逐字吻合；多一个少一个都必须回来复核）
ACT_BYPASSERS = {
    # —— 真正的 GEBURA 边界：ENEMY 且 EXP>0 ⇒ 闸门认得它、豁免照发，但僵直发不出 ——
    'actors/mobs/YogDzewa.java',          # Mob, ENEMY, EXP=50 ⇒ 锁血后 50 回合无敌、且全程照常出手
    'actors/mobs/Pylon.java',             # Mob, 入场 NEUTRAL、activate() 后转 ENEMY、EXP 取 Mob 默认 1
                                          #   ⇒ 理论上也吃 1 回合豁免；只 1 回合、NEUTRAL 期间本就无敌，影响可忽略
    # —— 以下都够不到 GEBURA ——
    'actors/Char.java',                   # act() 的原型基类（EXP 字段在 Mob 上，Char 自己没有）
    'actors/mobs/CrystalSpire.java',      # Mob, NEUTRAL, EXP=20 ⇒ 闸门第一关（只认 ENEMY）就挡掉
    'actors/mobs/Masterpiece.java',       # Mob, NEUTRAL, EXP=0
    'actors/mobs/DecoyDoll.java',         # Mob, ALLY, EXP=0
    'actors/mobs/MobSpawner.java',        # Actor（不是 Char）
    'actors/mobs/npcs/Sheep.java',        # NPC ⇒ 父类统一 NEUTRAL + EXP=0
    'actors/mobs/npcs/VaultLaser.java',   # NPC ⇒ 父类统一 NEUTRAL + EXP=0
    'actors/mobs/npcs/VaultSentry.java',  # NPC ⇒ 父类统一 NEUTRAL + EXP=0
    'effects/Pushing.java',               # Actor（不是 Char）
    'effects/Swap.java',                  # Actor（不是 Char）
}

byp, act_calls = scan_act_overrides()
_diff = ''
if set(byp) != ACT_BYPASSERS:
    _diff = '：新增=%s 消失=%s' % (sorted(set(byp) - ACT_BYPASSERS), sorted(ACT_BYPASSERS - set(byp)))
chk(set(byp) == ACT_BYPASSERS,
    '全仓「覆写 act() 但不调 super.act()」的清单与预期逐字吻合（实得 %d 个%s）' % (len(byp), _diff))
chk('actors/mobs/Mob.java' in act_calls,
    'Mob.java 自己调 super.act() ⇒ 消费点对「走 Mob.act() 的绝大多数怪」可达')
chk('actors/mobs/DM300.java' in act_calls,
    '转阶段 Boss DM300 调 super.act() ⇒ 它也吃得到僵直（正面样本：边界不是「凡是 Boss 都没份」）')

# 把 YogDzewa 这条边界本身钉住：证明它是**真的** ENEMY + EXP>0，而不是「本来就够不到，无所谓」
_yog = strip_comments(read('actors/mobs/YogDzewa.java'))
chk(re.search(r'\bEXP\s*=\s*50\s*;', _yog) is not None,
    'YogDzewa 确实 EXP=50 ⇒ 锁血豁免 50 回合（全库唯一「EXP>0 的 act() 绕过者」）')
chk(re.search(r'\balignment\s*=\s*(?:Char\.)?Alignment\.(?:NEUTRAL|ALLY)\s*;', _yog) is None,
    'YogDzewa 没把阵营改成 NEUTRAL/ALLY ⇒ 它是 ENEMY、闸门认它（边界不是「它本来就不吃」）')


# ================================================================
print('\n' + '=' * 78)
if ok:
    print('全部核验通过。')
else:
    print('存在未通过的核验项，见上面的 [FAIL]。')
print('=' * 78)
sys.exit(0 if ok else 1)
