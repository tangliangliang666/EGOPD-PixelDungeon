# -*- coding: utf-8 -*-
"""「GEBURA 濒死无敌导致幸运附魔 / 财富戒指的**额外掉落判定**落空」修复的核验（EGOPD，2026-09-24）。

用户口径：
  ① 只要怪物触发了 GEBURA 的无敌（濒死延后死亡），幸运附魔与财富戒指的额外掉落就都不生效；
  ② 其中「财富戒指增加原本基础掉落掉率」可能是生效的，**额外掉落生成**不生效。

根因（源码层）：「额外掉落」的两处判定在 vanilla 里与**致死那一击**同处一个调用栈
（`Char.attack` → `Char.damage` → `Mob.die` → `rollToDropLoot`），所以它们读到的是「致死那一刻」的状态；
而 GEBURA 把真正倒下推迟了 `EXP` 个回合（`GeburaGrace.act()` 若干回合后才调 `die()`），推迟到的那一刻：
  · `LuckProc`（幸运附魔）—— **只活到下一个 buff tick 的瞬态标记**（`act()` 第一句就是 `detach()`）⇒ 必已不在；
  · 财富戒指等级（`Ring.getBuffedBonus(hero, Wealth.class)`）—— 英雄侧凭据在延后期间同样可能与判定时刻不一致。
⇒ 修法：闸门在**锁血那一击**把这两份凭据抄到 Mob 上，`Mob.die / Mob.rollToDropLoot` 一律
**live 优先、取不到才回落留存**（未开启本考验时行为逐字不变）。

本脚本分 A~G 七组断言（每条判据都配 `--selftest` 反例）：

  A. `Mob` 的两个留存字段：存在、且**不进存档**（storeInBundle / restoreFromBundle 都不碰）。
  B. 闸门抄写：`Trials.interceptLethalDamage` 在 `geburaUsed` 门闩之后、`Buff.affect(GeburaGrace)` 之前抄写；
     各只抄一次；闸门体内 `Random` 出现 0 次（不碰随机流）。
  C. `Mob.die()` 的消费：`luckyProc` 布尔必须把留存算进去，且必须在 `rollToDropLoot()` **之前**取。
  D. `Mob.rollToDropLoot()` 的消费：财富块「live 优先 + 回落 + 等级显式传入」、幸运块「live 优先 + 回落 + 用完即放」，
     且旧写法（直接 `buff(...).genLoot()`）已消失；rollToDropLoot 里 `gebura*` 只出现 3 处（无多余钩子）。
  E. `RingOfWealth.tryForBonusDrop` 三参重载：存在、**体内不再自查等级**（否则回落白抄）、
     二参重载转调三参、主体未删（Random 计数与计数器操作都在）。
  F. `Lucky.LuckProc` 的可留存性：`public static class`、`genLoot()` public（跨包可调）。
  G. 「未开启时逐字一致」的门控前提：闸门首行的 `Dungeon.isTrialled(GEBURA)` 守卫仍在。

用法：`python _chk/verify_gebura_drop.py`（`--selftest` 加跑反例自测，反例 FAIL **不计总账**）。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
# 保偏移的单趟状态机剥注释（**别用正则去 /* */ **——`//**` 会一路吞到文件末尾，见 skill §10）
from check_unused_imports import strip_comments

SRC  = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
MOB  = os.path.join(SRC, 'actors/mobs/Mob.java')
TRI  = os.path.join(SRC, 'Trials.java')
ROW  = os.path.join(SRC, 'items/rings/RingOfWealth.java')
LUCK = os.path.join(SRC, 'items/weapon/enchantments/Lucky.java')

FILES = {'mob': MOB, 'trials': TRI, 'row': ROW, 'lucky': LUCK}

ok = True
results = []


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False
    results.append(bool(cond))


def sq(t):
    """压掉空白，便于跨行表达式做子串判定。"""
    return " ".join(t.split())


def body_of(text, signature):
    """按大括号配平取方法/块主体。"""
    idx = text.find(signature)
    if idx < 0:
        return None
    start = text.find("{", idx)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return None


def load():
    """把四份源码读成 {name: {'raw':原文本, 'code':剥注释文本}}。"""
    S = {}
    for k, p in FILES.items():
        raw = open(p, encoding='utf-8').read()
        S[k] = {'raw': raw, 'code': strip_comments(raw)}
    return S


def code_body(S, key, signature):
    b = body_of(S[key]['code'], signature)
    return '' if b is None else b


# ============================================================ 判据表
# 每条 = (id, 说明, 谓词, 反例变异器 or None)
CASES = []


def case(cid, desc, mutator=None):
    def deco(fn):
        CASES.append((cid, desc, fn, mutator))
        return fn
    return deco


def sub(S, key, pattern, repl, count=1):
    """在 raw 文本上按正则替换（. 跨行），再重算剥注释版本。"""
    S[key]['raw'], n = re.subn(pattern, repl, S[key]['raw'], count=count, flags=re.S)
    assert n == count, '反例变异没命中：%s' % pattern
    S[key]['code'] = strip_comments(S[key]['raw'])


# ---------------------------------------------------------------- A 组
@case('A1', 'Mob 声明了 Lucky.LuckProc 型的留存字段 geburaLuckyProc')
def a1(S):
    return 'public Lucky.LuckProc geburaLuckyProc = null;' in sq(S['mob']['code'])


@case('A2', 'Mob 声明了 int 型的留存字段 geburaWealthBonus')
def a2(S):
    return 'public int geburaWealthBonus = 0;' in sq(S['mob']['code'])


@case('A3', '两份留存都不进存档（storeInBundle / restoreFromBundle 都不碰）')
def a3(S):
    st = code_body(S, 'mob', 'public void storeInBundle( Bundle bundle )')
    rs = code_body(S, 'mob', 'public void restoreFromBundle( Bundle bundle )')
    if not st or not rs:
        return False
    return not any(n in st or n in rs
                   for n in ('geburaLuckyProc', 'geburaWealthBonus'))


@case('A4', '字段注释写明「不进存档」的理由（防后人误加进 Bundle）')
def a4(S):
    return ('两者都**不进存档**' in S['mob']['raw'])


# ---------------------------------------------------------------- B 组
@case('B1', '闸门在锁血那一刻抄下 LuckProc 实例')
def b1(S):
    return ('mob.geburaLuckyProc = mob.buff( Lucky.LuckProc.class );'
            in sq(S['trials']['code']))


@case('B2', '闸门在锁血那一刻抄下财富等级')
def b2(S):
    return sq('mob.geburaWealthBonus = (Dungeon.hero == null) ? 0 '
              ': Ring.getBuffedBonus( Dungeon.hero, RingOfWealth.Wealth.class );'
              ) in sq(S['trials']['code'])


@case('B3', '抄写发生在 geburaUsed 一次性门闩**之后**')
def b3(S):
    b = code_body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src ){')
    if not b:
        return False
    i_used = b.find('mob.geburaUsed = true;')
    i_cap  = b.find('mob.geburaLuckyProc =')
    return 0 <= i_used < i_cap


@case('B4', '抄写发生在 Buff.affect(GeburaGrace) **之前**（计时开始前，凭据还活着）')
def b4(S):
    b = code_body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src ){')
    if not b:
        return False
    i_aff = b.find('Buff.affect( mob, GeburaGrace.class )')
    i_cap = b.find('mob.geburaLuckyProc =')
    i_wea = b.find('mob.geburaWealthBonus =')
    return 0 <= i_aff and 0 <= i_cap < i_aff and 0 <= i_wea < i_aff


@case('B5', '闸门体内不出现任何 Random 调用（不改随机流）')
def b5(S):
    b = code_body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src ){')
    return b is not None and 'Random' not in b


@case('B6', '两份留存各只抄一次（不重复抄、不留半份）')
def b6(S):
    b = code_body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src ){')
    if not b:
        return False
    return b.count('mob.geburaLuckyProc =') == 1 and b.count('mob.geburaWealthBonus =') == 1


# ---------------------------------------------------------------- C 组
@case('C1', 'Mob.die 的 luckyProc 布尔把留存算进去')
def c1(S):
    return ('boolean luckyProc = buff(Lucky.LuckProc.class) != null || geburaLuckyProc != null;'
            in sq(S['mob']['code']))


@case('C2', 'luckyProc 必须在 rollToDropLoot() **之前**取（否则已被消费/已丢失）')
def c2(S):
    b = code_body(S, 'mob', 'public void die( Object cause )')
    if not b:
        return False
    i_lp = b.find('boolean luckyProc =')
    i_rd = b.find('rollToDropLoot();')
    return 0 <= i_lp < i_rd


@case('C3', '环指大师素材掉落两处仍吃这个布尔（英雄击杀 + 流血补发）')
def c3(S):
    b = code_body(S, 'mob', 'public void die( Object cause )')
    return b is not None and b.count('RingMasterLoot.onEnemyKilled(Dungeon.hero, this, luckyProc)') == 2


# ---------------------------------------------------------------- D 组
@case('D1', '财富块先取 live 等级（与 vanilla 逐字同源）')
def d1(S):
    return ('int wealthBonus = Ring.getBuffedBonus(Dungeon.hero, RingOfWealth.Wealth.class);'
            in sq(S['mob']['code']))


@case('D2', 'live 取不到时回落到留存（取不到才回落）')
def d2(S):
    return 'if (wealthBonus <= 0) wealthBonus = geburaWealthBonus;' in sq(S['mob']['code'])


@case('D3', '等级**显式传入** tryForBonusDrop（否则内部自查、回落白抄）')
def d3(S):
    return ('RingOfWealth.tryForBonusDrop(Dungeon.hero, rolls, wealthBonus);'
            in sq(S['mob']['code']))


@case('D4', '幸运块 live 优先、取不到才回落留存')
def d4(S):
    want = ('Lucky.LuckProc luck = buff(Lucky.LuckProc.class); '
            'if (luck == null) luck = geburaLuckyProc; '
            'if (luck != null){')
    return want in sq(S['mob']['code'])


@case('D5', '消费后即放（不留悬挂引用）')
def d5(S):
    b = code_body(S, 'mob', 'public void rollToDropLoot(){')
    if not b:
        return False
    i_null = b.find('geburaLuckyProc = null;')
    i_loot = b.find('luck.genLoot()')
    return 0 <= i_null < i_loot


@case('D6', '旧写法已消失：不再直接 buff(...).genLoot()（那是「读 live 两遍」的坑）')
def d6(S):
    return 'buff(Lucky.LuckProc.class).genLoot()' not in sq(S['mob']['code'])


@case('D7', 'rollToDropLoot 里 gebura* 只出现 3 处（无多余钩子）')
def d7(S):
    b = code_body(S, 'mob', 'public void rollToDropLoot(){')
    if not b:
        return False
    return b.count('geburaWealthBonus') == 1 and b.count('geburaLuckyProc') == 2


# ---------------------------------------------------------------- E 组
@case('E1', 'RingOfWealth.tryForBonusDrop 有了「显式等级」的三参重载')
def e1(S):
    return 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){' in S['row']['code']


@case('E2', '三参重载体内**不再自查** getBuffedBonus（否则回落值等于白抄）')
def e2(S):
    b = code_body(S, 'row', 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){')
    return b is not None and 'getBuffedBonus' not in b


@case('E3', '二参重载转调三参、并把 live 等级原样传进去')
def e3(S):
    return ('return tryForBonusDrop( target, tries, getBuffedBonus(target, Wealth.class) );'
            in sq(S['row']['code']))


@case('E4', '三参重载保留 bonus<=0 的早退')
def e4(S):
    b = code_body(S, 'row', 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){')
    return b is not None and 'if (bonus <= 0) return null;' in sq(b)


@case('E5', '三参重载主体未删：Random 调用数与原版口径一致（4 次）')
def e5(S):
    b = code_body(S, 'row', 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){')
    if b is None:
        return False
    n = len(re.findall(r'\bRandom\.', b))
    return n == 4


@case('E6', '三参重载的两条计数器路径都在（triesToDrop / dropsToEquip）')
def e6(S):
    b = code_body(S, 'row', 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){')
    if b is None:
        return False
    return all(s in b for s in ('triesToDrop.countDown(tries);',
                                'triesToDrop.count() <= 0',
                                'dropsToEquip.countDown(1);',
                                'genEquipmentDrop(',
                                'genConsumableDrop('))


@case('E7', '三参重载仍保两处挑战过滤（装备/消耗品各一次，防「绕过 isItemBlocked」）')
def e7(S):
    b = code_body(S, 'row', 'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){')
    return b is not None and b.count('while (Challenges.isItemBlocked(i));') == 2


# ---------------------------------------------------------------- F 组
@case('F1', 'Lucky.LuckProc 是 public static class（可被 buff(Class) 精确匹配、可跨包持有）')
def f1(S):
    return 'public static class LuckProc extends Buff {' in S['lucky']['code']


@case('F2', 'LuckProc.genLoot() 是 public 且返回 Item（跨包可调）')
def f2(S):
    return 'public Item genLoot(){' in sq(S['lucky']['code'])


# ---------------------------------------------------------------- G 组
@case('G1', '闸门首行仍是 isTrialled(GEBURA) 守卫（未开启 ⇒ 不抄 ⇒ 两字段恒为初值）')
def g1(S):
    b = code_body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src ){')
    if not b:
        return False
    return ('if (!Dungeon.isTrialled( GEBURA ) || !(ch instanceof Mob)) return false;' in sq(b))


@case('G2', 'Mob.die 里没有第二个 rollToDropLoot 调用点（不会重复结算）')
def g2(S):
    b = code_body(S, 'mob', 'public void die( Object cause )')
    return b is not None and b.count('rollToDropLoot();') == 1


# ---------------------------------------------------------------- H 组（行为层）
H_PROBE = os.path.join(HERE, 'GeburaDropProbe.java')
H_OUT   = os.path.join(HERE, '_gdrop.out')


@case('H1', '行为探针 _chk/GeburaDropProbe.java 存在')
def h1(S):
    return os.path.isfile(H_PROBE)


@case('H2', '行为探针的输出存在，且没有 [FAIL]')
def h2(S):
    if not os.path.isfile(H_OUT):
        return False
    return '[FAIL]' not in open(H_OUT, encoding='utf-8').read()


@case('H3', '探针断言全过（汇总行「失败 0」）')
def h3(S):
    if not os.path.isfile(H_OUT):
        return False
    txt = open(H_OUT, encoding='utf-8').read()
    return bool(re.search(r'断言 \d+ 条：通过 \d+，失败 0\b', txt))


@case('H4', '探针覆盖「LuckProc 活不过一个 tick」这一段（根因实证）')
def h4(S):
    if not os.path.isfile(H_OUT):
        return False
    t = open(H_OUT, encoding='utf-8').read()
    return ('① 幸运附魔的 LuckProc 只活到下一个 buff tick' in t
            and '已自行摘掉' in t)


@case('H5', '探针覆盖「live 没了、留存还在」（延后死亡的可达性）')
def h5(S):
    if not os.path.isfile(H_OUT):
        return False
    t = open(H_OUT, encoding='utf-8').read()
    return ('live 的 LuckProc 已不在' in t and '闸门留存仍在' in t)


@case('H6', '探针覆盖「关掉 GEBURA 不留存」与「闸门不消耗随机数」两段')
def h6(S):
    if not os.path.isfile(H_OUT):
        return False
    t = open(H_OUT, encoding='utf-8').read()
    return ('关掉 GEBURA：不留存' in t and '闸门不消耗随机数' in t
            and '200 个数逐位相同' in t)


# ============================================================ 反例表
MUT = {
    # A
    'A1': lambda S: sub(S, 'mob',
                        r'public Lucky\.LuckProc geburaLuckyProc = null;',
                        'public Object geburaLuckyProcRemoved = null;'),
    'A2': lambda S: sub(S, 'mob', r'public int geburaWealthBonus = 0;',
                        'public int geburaWealthBonusRemoved = 0;'),
    'A3': lambda S: sub(S, 'mob',
                        r'(public void storeInBundle\( Bundle bundle \) \{)',
                        r'\1\n\t\tbundle.put( "gebura_lucky_proc", geburaLuckyProc );'),
    'A4': lambda S: sub(S, 'mob', r'两者都\*\*不进存档\*\*', '两份都留着'),
    # B
    'B1': lambda S: sub(S, 'trials',
                        r'mob\.geburaLuckyProc = mob\.buff\( Lucky\.LuckProc\.class \);',
                        'mob.geburaLuckyProc = null;'),
    'B2': lambda S: sub(S, 'trials',
                        r'mob\.geburaWealthBonus = \(Dungeon\.hero == null\) \? 0\s*\n\s*: Ring\.getBuffedBonus\( Dungeon\.hero, RingOfWealth\.Wealth\.class \);',
                        'mob.geburaWealthBonus = 0;'),
    'B3': lambda S: sub(S, 'trials',
                        r'(mob\.geburaUsed = true;)',
                        r'mob.geburaLuckyProc = null;\n\t\t\1'),
    'B4': lambda S: sub(S, 'trials',
                        r'(mob\.geburaLuckyProc = mob\.buff\( Lucky\.LuckProc\.class \);)',
                        r'Buff.affect( mob, GeburaGrace.class ).set( turns, src );\n\t\t\1'),
    'B5': lambda S: sub(S, 'trials',
                        r'(mob\.geburaUsed = true;)',
                        r'\1\n\t\tint _rnd = Random.Int( 3 );'),
    'B6': lambda S: sub(S, 'trials',
                        r'\n\s*mob\.geburaWealthBonus = \(Dungeon\.hero == null\) \? 0\s*\n\s*: Ring\.getBuffedBonus\( Dungeon\.hero, RingOfWealth\.Wealth\.class \);',
                        ''),
    # C
    'C1': lambda S: sub(S, 'mob',
                        r'boolean luckyProc = buff\(Lucky\.LuckProc\.class\) != null \|\| geburaLuckyProc != null;',
                        'boolean luckyProc = buff(Lucky.LuckProc.class) != null;'),
    'C2': lambda S: sub(S, 'mob',
                        r'(boolean luckyProc = buff\(Lucky\.LuckProc\.class\) != null \|\| geburaLuckyProc != null;\s*\n)\s*(rollToDropLoot\(\);)',
                        r'\2\n\t\t\t\1'),
    'C3': lambda S: sub(S, 'mob',
                        r'RingMasterLoot\.onEnemyKilled\(Dungeon\.hero, this, luckyProc\);',
                        'RingMasterLoot.onEnemyKilled(Dungeon.hero, this, false);'),
    # D
    'D1': lambda S: sub(S, 'mob',
                        r'int wealthBonus = Ring\.getBuffedBonus\(Dungeon\.hero, RingOfWealth\.Wealth\.class\);',
                        'int wealthBonus = 0;'),
    'D2': lambda S: sub(S, 'mob',
                        r'\s*if \(wealthBonus <= 0\) wealthBonus = geburaWealthBonus;',
                        ''),
    'D3': lambda S: sub(S, 'mob',
                        r'RingOfWealth\.tryForBonusDrop\(Dungeon\.hero, rolls, wealthBonus\);',
                        'RingOfWealth.tryForBonusDrop(Dungeon.hero, rolls);'),
    'D4': lambda S: sub(S, 'mob',
                        r'Lucky\.LuckProc luck = buff\(Lucky\.LuckProc\.class\);\s*\n\s*if \(luck == null\) luck = geburaLuckyProc;\s*\n\s*if \(luck != null\)\{',
                        'if (buff(Lucky.LuckProc.class) != null){\n\t\t\tLucky.LuckProc luck = buff(Lucky.LuckProc.class);'),
    'D5': lambda S: sub(S, 'mob', r'\n\s*geburaLuckyProc = null;\n', '\n'),
    'D6': lambda S: sub(S, 'mob', r'(Lucky\.LuckProc luck = buff\(Lucky\.LuckProc\.class\);)',
                        r'\1\n\t\tDungeon.level.drop(buff(Lucky.LuckProc.class).genLoot(), 0);'),
    'D7': lambda S: sub(S, 'mob',
                        r'(geburaWealthBonus;\s*\n)',
                        r'\1\t\tint _probe = geburaWealthBonus;\n'),
    # E
    'E1': lambda S: sub(S, 'row',
                        r'public static ArrayList<Item> tryForBonusDrop\(Char target, int tries, int bonus \)\{',
                        'public static ArrayList<Item> tryForBonusDrop3(Char target, int tries, int bonus ){'),
    'E2': lambda S: sub(S, 'row',
                        r'public static ArrayList<Item> tryForBonusDrop\(Char target, int tries, int bonus \)\{\s*\n\s*if \(bonus <= 0\) return null;',
                        'public static ArrayList<Item> tryForBonusDrop(Char target, int tries, int bonus ){\n\t\tif (getBuffedBonus(target, Wealth.class) <= 0) return null;'),
    'E3': lambda S: sub(S, 'row',
                        r'return tryForBonusDrop\( target, tries, getBuffedBonus\(target, Wealth\.class\) \);',
                        'return tryForBonusDrop( target, tries, 0 );'),
    'E4': lambda S: sub(S, 'row',
                        r'if \(bonus <= 0\) return null;',
                        'if (bonus < 0) return null;'),
    'E5': lambda S: sub(S, 'row',
                        r'(\tpublic static ArrayList<Item> tryForBonusDrop\(Char target, int tries, int bonus \)\{\n)',
                        r'\1\n\t\tbonus += Random.Int( 2 );\n'),
    'E6': lambda S: sub(S, 'row', r'dropsToEquip\.countDown\(1\);', 'dropsToEquip.countDown(0);'),
    'E7': lambda S: sub(S, 'row', r'\n\s*\} while \(Challenges\.isItemBlocked\(i\)\);',
                        '\n\t\t\t} while (false);'),
    # F
    'F1': lambda S: sub(S, 'lucky', r'public static class LuckProc extends Buff \{',
                        'public class LuckProc extends Buff {'),
    'F2': lambda S: sub(S, 'lucky', r'public Item genLoot\(\)\{', 'Item genLoot(){'),
    # G
    'G1': lambda S: sub(S, 'trials',
                        r'if \(!Dungeon\.isTrialled\( GEBURA \) \|\| !\(ch instanceof Mob\)\) return false;',
                        'if (!(ch instanceof Mob)) return false;'),
    'G2': lambda S: sub(S, 'mob',
                        r'(\t\t\trollToDropLoot\(\);\n)',
                        r'\1\t\t\tif (HP < 0) rollToDropLoot();\n'),
}

# H 组是「按探针输出文件判」，反例＝把输出文件换成一个坏版本（跑完原样恢复）
HMUT = {
    'H2': '假输出\n  [FAIL] 故意喂一个失败项\n断言 26 条：通过 25，失败 1\n',
    'H3': '假输出\n  [OK]   看着都对\n断言 26 条：通过 25，失败 1\n',
    'H4': '--- ① 没有任何小标题 ---\n  [OK]   x\n断言 1 条：通过 1，失败 0\n',
    'H5': '--- ③ 只有一半 ---\n  [OK]   live 的 LuckProc 已不在\n断言 1 条：通过 1，失败 0\n',
    'H6': '--- ⑤ 关掉 GEBURA：不留存 ---\n  [OK]   x\n断言 1 条：通过 1，失败 0\n',
}


def run_cases(S, only=None):
    for cid, desc, fn, _mut in CASES:
        if only and cid not in only:
            continue
        try:
            r = fn(S)
        except Exception as e:      # 反例把结构改坏时，判据本身也可能抛
            r = False
        chk(bool(r), '[%s] %s' % (cid, desc))


def main():
    print('=' * 78)
    print('GEBURA 掉落修复核验（幸运附魔 / 财富戒指的额外掉落判定，2026-09-24）')
    print('=' * 78)

    groups = [
        ('A. Mob 的两份留存凭据（存在 + 不进存档）', 'A'),
        ('B. 闸门抄写（Trials.interceptLethalDamage）', 'B'),
        ('C. Mob.die 的消费（luckyProc 布尔）', 'C'),
        ('D. Mob.rollToDropLoot 的消费（财富块 + 幸运块）', 'D'),
        ('E. RingOfWealth.tryForBonusDrop 三参重载', 'E'),
        ('F. Lucky.LuckProc 的可留存性', 'F'),
        ('G. 未开启本考验时逐字一致的门控前提', 'G'),
        ('H. 行为层（GeburaDropProbe 的实证输出）', 'H'),
    ]
    for title, g in groups:
        print('\n-- %s --' % title)
        run_cases(load(), only={c[0] for c in CASES if c[0].startswith(g)})

    print('\n' + '=' * 78)
    if ok:
        print('全部核验通过（%d 条）。' % len(results))
    else:
        print('有 %d 条与预期不符。' % sum(1 for r in results if not r))
    print('=' * 78)

    if '--selftest' in sys.argv:
        print('\n反例自测（每条判据喂一个「改坏」的变体，必须判 FAIL；不计总账）')
        bad = 0
        for cid, desc, fn, _mut in CASES:
            if cid in HMUT:
                # H 组是「按输出文件判」，反例＝把输出文件换成一个坏版本（跑完恢复）
                real = open(H_OUT, encoding='utf-8').read() if os.path.isfile(H_OUT) else None
                try:
                    with open(H_OUT, 'w', encoding='utf-8') as f:
                        f.write(HMUT[cid])
                    try:
                        r = fn(load())
                    except Exception:
                        r = False
                    print(('  [FAIL] %s 反例仍判 PASS ⇒ 该判据抓不到这类错' % cid) if r
                          else ('  [OK]   %s 反例已判 FAIL（判据有效）' % cid))
                    if r:
                        bad += 1
                finally:
                    if real is None:
                        os.remove(H_OUT)
                    else:
                        with open(H_OUT, 'w', encoding='utf-8') as f:
                            f.write(real)
                continue
            mut = MUT.get(cid)
            if mut is None:
                print('  [SKIP] %s 无反例' % cid)
                continue
            S = load()
            try:
                mut(S)
            except AssertionError as e:
                print('  [SKIP] %s 反例变异未命中（%s）' % (cid, e))
                continue
            try:
                r = fn(S)
            except Exception:
                r = False
            if r:
                print('  [FAIL] %s 反例仍判 PASS ⇒ 该判据抓不到这类错' % cid)
                bad += 1
            else:
                print('  [OK]   %s 反例已判 FAIL（判据有效）' % cid)
        print('\n反例自测：%d 条判据，%d 条抓不到错。' % (len(CASES), bad))
        if bad:
            sys.exit(2)


if __name__ == '__main__':
    main()
