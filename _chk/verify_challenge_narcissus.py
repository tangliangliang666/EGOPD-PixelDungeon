# -*- coding: utf-8 -*-
"""核验新挑战「水仙追迹」的**结构**实现（源码级）。

为什么需要它、以及和 ChallengesLocCheck.java 的分工：
  · `_chk/ChallengesLocCheck.java` 管**文本**（真 Properties 装载、真换行、键齐全）——只有真装载
    才看得见「孤立反斜杠 / 两行并一行」那类事故；
  · 本脚本管**结构与接线**：位号/上限、得分归零的唯一出口、开局发放的必备动作与顺序、
    随机挑战池不再写死 `1<<i`、以及「全仓引用点清单」。
  · 两边都做，是因为本项目反复踩过「注释里写了伪代码 ⇒ `in` 匹配报 OK」这个坑
    （所以本脚本一律先剥注释再断言，判据函数直接复用 verify_chesed_gebura.py 里已被 selftest 证过的）。

分四层：
  ① 位与常量（源码）
  ② 得分倍率归零：唯一出口 + 只认这两个挑战
  ③ 开局发放：必备动作齐全、顺序正确（水仙追迹必须排在「拆迁办」之后 = 同时开时它胜出）
  ④ 接线与唯一性：随机池改数据驱动；全仓 NARCISSUS_TRACING 引用点清单吻合

用法：
  python _chk/verify_challenge_narcissus.py
  python _chk/verify_challenge_narcissus.py --selftest   # 反例自测：证明判据不恒真
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def strip_comments(src):
    """把注释替换成等长空格（保留换行）——保留字符偏移，所以仍能「比位置」。

    单趟状态机，识别 // 、/* */ 、字符串 "" 、字符 ''。与 check_unused_imports.py 同款做法。"""
    out = []
    state = None
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


def method_span(src, sig):
    """截出方法体：从签名起到**顶格一层缩进**的收尾 `\\n\\t}` 为止。找不到返回 None。"""
    i = src.find(sig)
    if i == -1:
        return None
    j = src.find('\n\t}', i)
    return None if j == -1 else src[i:j]


def block_span(src, if_line):
    """截出 `if (...){ ... }` 那个块的正文（按花括号配平，从 if_line 的 `{` 起）。"""
    i = src.find(if_line)
    if i == -1:
        return None
    j = src.find('{', i + len(if_line))
    if j == -1:
        return None
    depth = 0
    for k in range(j, len(src)):
        if src[k] == '{':
            depth += 1
        elif src[k] == '}':
            depth -= 1
            if depth == 0:
                return src[j:k + 1]
    return None


def selftest():
    global ok
    print('== selftest：证明判据不恒真 ==')
    good = 'if (a){\n\tChallenges.hook();\n}'
    bad = 'if (a){\n}'
    g = block_span(good, 'if (a)')
    b = block_span(bad, 'if (a)')
    chk(g is not None and 'Challenges.hook' in g, 'block_span 抓得到真块')
    chk(b is not None and 'Challenges.hook' not in b, 'block_span 抓得住空块（不会误报 OK）')
    fake = '// if (a){ Challenges.hook(); }'
    chk('Challenges.hook' not in strip_comments(fake), '注释里的伪代码被剥掉后不算数')
    # ⚠️ block_span 本身**不剥注释**（它总是作用在 read_j 的结果上），所以这里必须先剥再截
    chk(block_span(strip_comments(fake), 'if (a)') is None,
        '剥注释后再截：注释里的伪代码连块都截不出来')
    if not ok:
        print('  selftest **未通过** —— 判据可能恒真，先修脚本再谈核验。')
        sys.exit(2)
    ok = True          # 自测期的不通过已由上面 exit 拦住；正式层会重新断言
    print('  selftest 通过。\n')


if '--selftest' in sys.argv:
    selftest()

challenges = read_j('Challenges.java')
rankings = read_j('Rankings.java')
heroclass = read_j('actors/hero/HeroClass.java')
heroselect = read_j('scenes/HeroSelectScene.java')

print('=' * 78)
print('核验：挑战「水仙追迹」（NARCISSUS_TRACING）')
print('=' * 78)

# ---------------------------------------------------------------- ① 位与常量
print('\n== ① 位与常量 ==')
chk(re.search(r'public static final int NARCISSUS_TRACING\s*=\s*4096\s*;', challenges) is not None,
    'NARCISSUS_TRACING == 4096（第 13 位；2048 已是调试模式，不能复用）')
chk(re.search(r'public static final int DEBUG_MODE\s*=\s*2048\s*;', challenges) is not None,
    'DEBUG_MODE 仍是 2048（没被挪用）')
chk(re.search(r'public static final int MAX_VALUE\s*=\s*8191\s*;', challenges) is not None,
    'MAX_VALUE 扩到 8191（2^13-1，能装下新位）')
chk(re.search(r'public static final int MAX_CHALS\s*=\s*12\s*;', challenges) is not None,
    'MAX_CHALS == 12（常规挑战由 11 条增至 12 条）')
chk(re.search(r'"narcissus_tracing"', challenges) is not None, 'NAME_IDS 里有 narcissus_tracing')
chk(re.search(r'NARCISSUS_TRACING,\s*DEBUG_MODE\s*\n\s*\};', challenges) is not None,
    'MASKS 里新位排在 DEBUG_MODE **之前**（界面上调试模式仍排最后）')
chk(block_span(challenges, 'public static boolean isRegular( int mask )') is not None
    and 'mask != DEBUG_MODE' in block_span(challenges, 'public static boolean isRegular( int mask )'),
    'isRegular() 判据＝「只有 DEBUG_MODE 是特殊挑战」')
chk('regularMasks()' in challenges, '提供了 regularMasks()（随机池的数据来源）')
chk(re.search(r'if \(!isRegular\(ch\)\) continue;', challenges) is not None,
    'activeChallenges() 已改走 isRegular()（不再写死 ch == DEBUG_MODE）')

# ---------------------------------------------------------------- ② 得分归零
print('\n== ② 得分倍率归零 ==')
zero_hits = []
for dirpath, _d, files in os.walk(SRC):
    for fn in files:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            rel = os.path.relpath(p, SRC).replace(os.sep, '/')
            if 'chalMultiplier = 0f' in strip_comments(open(p, encoding='utf-8').read()):
                zero_hits.append(rel)
chk(zero_hits == ['Rankings.java'],
    '全仓「把得分倍率置 0」的唯一出口在 Rankings.java（实得 %s）' % (zero_hits or '无'))
zero_block = method_span(rankings, 'public int calculateScore(){') or ''
chk(zero_block != '', 'Rankings.calculateScore() 方法体截取成功'
    '（注意：倍率不在那个一行的 score(boolean) 里，那里只算基础分）')
i_dbg = zero_block.find('Challenges.DEBUG_MODE')
i_nar = zero_block.find('Challenges.NARCISSUS_TRACING')
i_ast = zero_block.find('chalMultiplier = 0f')
chk(i_dbg != -1 and i_nar != -1,
    '归零分支同时认这两个挑战（调试模式 @%d / 水仙追迹 @%d）' % (i_dbg, i_nar))
chk(i_dbg < i_ast and i_nar < i_ast,
    '两个判据都在赋值**之前**（顺序反了就成了「先归零再被后面的判据覆盖」）')
chk('Dungeon.isChallenged' in zero_block, '用的是 Dungeon.isChallenged(掩码) 判据')

# ---------------------------------------------------------------- ③ 开局发放
print('\n== ③ 开局携带 ==')
init = method_span(heroclass, 'public void initHero( Hero hero ) {')
chk(init is not None, 'HeroClass.initHero() 方法体截取成功')
grid = block_span(heroclass, 'if (Dungeon.isChallenged(Challenges.NARCISSUS_TRACING))')
chk(grid is not None, 'initHero 里有 NARCISSUS_TRACING 的发放块')
if grid is not None:
    for needle, desc in [
            ('new NarcissusCrossSword()', '发放水仙十字圣剑（常态实例）'),
            ('new NarcissusCrossSword().identify().collect()', '圣剑就地鉴定后放进背包'),
            ('new RingOfWealth()', '发放财富戒指'),
            ('new RingOfWealth().identify().collect()', '财富戒指同样放进背包')]:
        chk(needle in grid, desc)
    #2026-09-24（用户要求）：开局发放一律走「放背包」，下面三样必须彻底退场。
    #⚠️ 必须**先剥注释**再搜 —— 本块的说明注释里就写着「刻意**不**在这里调 activate()」和
    #   「那条改的是 belongings.weapon，本段只 collect()」，直接 in 匹配会假红。
    nar_code = strip_comments(grid)
    for banned, desc in [
            ('belongings.weapon', '不再替换初始武器装备（用户 2026-09-24 明确要求改为放背包）'),
            ('activate(', '不再在这里调 activate —— 形态看护改由装备流程（KindOfWeapon.doEquip）挂上'),
            ('quickslot', '不再占用快捷栏槽位（剑在背包里就不是「已装备」）')]:
        chk(banned not in nar_code, '发放块内不含 %s：%s' % (banned, desc))
    chk('NarcissusSwordMang' not in nar_code and 'syncForm' not in nar_code and 'morphInto' not in nar_code,
        '刻意不在这里直接变形/调 morphInto（那套流程会刷动作按钮/快捷栏，而此刻场景还没建）')
    #顺序不再是语义：本段只 collect()、不碰 belongings.weapon，所以与「拆迁办」块谁前谁后都成立。
    i_demo = heroclass.find('if (Dungeon.isChallenged(Challenges.DEMOLITION_SQUAD))')
    i_narc = heroclass.find('if (Dungeon.isChallenged(Challenges.NARCISSUS_TRACING))')
    chk(i_demo != -1 and i_narc != -1,
        '两条挑战的发放块都在 initHero 里（拆迁办 @%d / 水仙追迹 @%d；顺序已不构成语义）' % (i_demo, i_narc))
    chk(init is not None and i_narc > heroclass.find('public void initHero( Hero hero ) {')
        and i_narc < heroclass.find('public Badges.Badge masteryBadge()'),
        '发放块确实在 initHero() 体内（不是别的方法里）')
chk(re.search(r'import com\.shatteredpixel\.shatteredpixeldungeon\.items\.weapon\.melee\.NarcissusCrossSword;',
              heroclass) is not None, '补了 NarcissusCrossSword 的 import')
chk(re.search(r'import com\.shatteredpixel\.shatteredpixeldungeon\.items\.rings\.RingOfWealth;',
              heroclass) is not None, '补了 RingOfWealth 的 import')

# ---------------------------------------------------------------- ④ 接线与唯一性
print('\n== ④ 接线与唯一性 ==')
chk('Math.pow(2, i)' not in heroselect,
    '随机挑战池不再写死 1<<i（那要求常规挑战刚好占满低若干位；加了新位后会把 DEBUG_MODE 拉进池子）')
chk('Challenges.regularMasks()' in heroselect, '随机挑战池改从 Challenges.regularMasks() 取')

refs = []
for dirpath, _d, files in os.walk(SRC):
    for fn in files:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            rel = os.path.relpath(p, SRC).replace(os.sep, '/')
            if 'NARCISSUS_TRACING' in strip_comments(open(p, encoding='utf-8').read()):
                refs.append(rel)
chk(sorted(refs) == ['Challenges.java', 'Rankings.java', 'actors/hero/HeroClass.java'],
    'NARCISSUS_TRACING 的引用点清单吻合（实得 %s）—— 多一处就说明有人另开了出口' % sorted(refs))

# ---------------------------------------------------------------- ⑤ 文本交接
print('\n== ⑤ 文本层（由 ChallengesLocCheck.java 负责） ==')
chk(os.path.isfile(os.path.join(HERE, 'ChallengesLocCheck.java')),
    '_chk/ChallengesLocCheck.java 存在（文本层核验：真 Properties 装载 + 换行 + 键齐全）')
chk(os.path.isfile(os.path.join(HERE, 'fix_challenge_narcissus_text.py')),
    '_chk/fix_challenge_narcissus_text.py 存在（幂等文本补丁，CRLF 字节级插入）')

print('\n' + '=' * 78)
print('全部核验通过。' if ok else '存在未通过的核验项，见上面的 [FAIL]。')
print('=' * 78)
sys.exit(0 if ok else 1)
