# -*- coding: utf-8 -*-
"""「带着标记倒下」击杀判定 + 水晶矿洞层虚空地形 的核验（EGOPD，2026-09-26）。

用户口径（两部分）：

  【Part A】两个相似缺陷，**同一根因**：
    · 拇指（Thumb）盔甲技能「瞄准心脏」的天赋「荣耀凯旋」（`Talent.GLORIOUS_TRIUMPH`）；
    · 食指（Index）「击杀指令目标」（`Instruction` 任务 `TASK_KILL_TARGET`）的**完成判定**与天赋
      「神谕庇佑」（`Talent.ORACULAR_BLESSING`）。
    两者旧判据都要求「**英雄这一击把它打死了**」——分别是 `Char.attack` 里紧接 `enemy.damage()` 的
    `!enemy.isAlive()`，以及 `Mob.die` 英雄击杀分支里的 `buff(InstructionTarget.class) != null`。
    而 GEBURA 锁血把死亡**推迟**了 `EXP` 个回合（`GeburaGrace.act()` 到点才 `die(cause)`）⇒
    致死那一刻的攻击调用栈早已返回，判据必然落空。
    **共性 = 要击杀的目标身上带着对应 buff** ⇒ 触发条件改为「**携带该 buff 的怪死亡**」：
    判据收口到 `Mob.die()` 开头（在 `super.die()` **之前**，那之后 `onRemove` 会把 buff 全摘掉），
    并在 GEBURA 闸门处把「致死那一击」的携带状态抄到 Mob 上（live 优先、取不到才回落）。

  【Part B】水晶任务矿洞层（`Blacksmith.Quest.Type() == CRYSTAL` 的 `MiningLevel`）：
    · 地形生成时**正常生成深渊/虚空**（替代**地板**，不替代墙体）；
    · 玩家掉进去**不换层**，而是被传回本层某处 + 结算坠落伤害，提示
      「一股魔法乱流把你送到了这一层某处」。

本脚本分 A~I 九组断言（关键判据都配 `--selftest` 反例）：

  A. `Mob` 的两个「带着标记倒下」留存字段：声明、**不进存档**、在 `die()` 里**消费**（一次性）。
  B. 闸门的抄写：位置（`geburaUsed` 门闩之后、`Buff.affect(GeburaGrace)` 之前）、纯读取、**不碰 Random**。
  C. 判据收口：旧判据全部撤除，全仓 `onKillByHero` / `onTargetKilled` 的调用点**各只剩 1 处、都在 `Mob.die`**。
  D. `Level.handlesChasmFall()` 的默认实现与 `MiningLevel` 的覆写（按任务类型门控）。
  E. `Chasm.heroFall` 的**不换层**分支 + `heroRiftReturn`（复用传送 + 坠落伤害，不进 `beforeTransition()`）。
  F. `MiningLevel.carveVoid` 的地形规则：只吃地板、留一整圈地板、避让关键单位、**不改 flags 的前提**。
  G. 三个矿洞房间的调用点（都在 CRYSTAL 分支内）+ 画师 `decorate` 只还原「房间之间」那批深渊。
  H. 三份 `levels*.properties` 的文本键。
  I. 行为层（`_chk/_markedkill.out`，即 `MarkedKillProbe` 的实证输出）。

用法：`python _chk/verify_marked_kill_routes.py`（加 `--selftest` 跑反例自测，反例 FAIL **不计总账**）。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
# 剥注释用既有的单趟状态机（**别用正则去 /* */ **——`//**` 会一路吞到文件末尾，见 skill §10）
from check_unused_imports import strip_comments

SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/levels')

# ⚠️ 本仓行尾是混的：Mob / Trials / PalermoFencing 是**纯 LF**，其余改动文件是 **CRLF**。
# 所以所有反例正则都用 `\s*` / `[^\n]*\r?\n`，别写死 `\n`。核验只读不写，不会碰行尾。
FILES = {
    'mob':     os.path.join(SRC, 'actors/mobs/Mob.java'),
    'trials':  os.path.join(SRC, 'Trials.java'),
    'char':    os.path.join(SRC, 'actors/Char.java'),
    'palermo': os.path.join(SRC, 'items/weapon/melee/PalermoFencing.java'),
    'aim':     os.path.join(SRC, 'actors/buffs/AimHeartMark.java'),
    'instr':   os.path.join(SRC, 'actors/buffs/Instruction.java'),
    'level':   os.path.join(SRC, 'levels/Level.java'),
    'chasm':   os.path.join(SRC, 'levels/features/Chasm.java'),
    'mine':    os.path.join(SRC, 'levels/MiningLevel.java'),
    'small':   os.path.join(SRC, 'levels/rooms/quest/MineSmallRoom.java'),
    'large':   os.path.join(SRC, 'levels/rooms/quest/MineLargeRoom.java'),
    'giant':   os.path.join(SRC, 'levels/rooms/quest/MineGiantRoom.java'),
    'painter': os.path.join(SRC, 'levels/painters/MiningLevelPainter.java'),
}

TEXT = {
    'lv_en':   os.path.join(MSG, 'levels.properties'),
    'lv_zh':   os.path.join(MSG, 'levels_zh.properties'),
    'lv_hant': os.path.join(MSG, 'levels_zh-hant.properties'),
}

PROBE_OUT = os.path.join(HERE, '_markedkill.out')
PROBE_B_OUT = os.path.join(HERE, '_miningvoid.out')

ok = True
results = []


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False
    results.append(bool(cond))


def sq(t):
    """压掉空白，便于跨行/跨行尾做子串判定。"""
    return " ".join(t.split())


def has(text, snippet):
    return sq(snippet) in sq(text)


def body_of(text, signature):
    """按大括号配平取方法主体（签名必须唯一，且已剥注释）。"""
    idx = text.find(signature)
    if idx < 0:
        return None
    if text.find(signature, idx + 1) >= 0:
        raise AssertionError('签名不唯一：%s' % signature)
    start = text.find("{", idx + len(signature) - 1)
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
    """源码读成 {name: {'raw','code'}}；文本文件读成 {name: {'raw'}}。"""
    S = {}
    for k, p in FILES.items():
        raw = open(p, encoding='utf-8').read()
        S[k] = {'raw': raw, 'code': strip_comments(raw)}
    for k, p in TEXT.items():
        S[k] = {'raw': open(p, encoding='utf-8').read()}
    return S


def body(S, key, signature):
    b = body_of(S[key]['code'], signature)
    if b is None:
        raise AssertionError('取不到主体：%s @ %s' % (key, signature))
    return b


# ============================================================ 判据表
CASES = []


def case(cid, desc, mutator=None):
    def deco(fn):
        CASES.append((cid, desc, fn, mutator))
        return fn
    return deco


def mut_sub(key, pattern, repl, count=1):
    """反例变异器工厂：在 raw 上按正则替换，再重算剥注释版本。"""
    def f(S):
        S[key]['raw'], n = re.subn(pattern, repl, S[key]['raw'], count=count, flags=re.S)
        assert n == count, '反例变异没命中（%d 次）：%s' % (n, pattern)
        if 'code' in S[key]:
            S[key]['code'] = strip_comments(S[key]['raw'])
    return f


def mut_drop(key, pattern, count=1):
    """把命中的片段删掉（用于「本该存在」的判据）。"""
    return mut_sub(key, pattern, '', count=count)


# ---------------------------------------------------------------- A 组
@case('A1', 'Mob 声明了 boolean 留存字段 geburaAimMarked（初值 false）',
      mut_drop('mob', r'public boolean geburaAimMarked = false;'))
def a1(S):
    return 'public boolean geburaAimMarked = false;' in sq(S['mob']['code'])


@case('A2', 'Mob 声明了 boolean 留存字段 geburaInstrTarget（初值 false）',
      mut_drop('mob', r'public boolean geburaInstrTarget = false;'))
def a2(S):
    return 'public boolean geburaInstrTarget = false;' in sq(S['mob']['code'])


@case('A3', '两份留存都不进存档（storeInBundle / restoreFromBundle 都不碰）',
      mut_sub('mob', r'(bundle\.put\( MAX_LVL, maxLvl \);)', r'\1 bundle.put( "geburaAimMarked", geburaAimMarked );'))
def a3(S):
    st = body(S, 'mob', 'public void storeInBundle( Bundle bundle )')
    rs = body(S, 'mob', 'public void restoreFromBundle( Bundle bundle )')
    return not any(n in st or n in rs for n in ('geburaAimMarked', 'geburaInstrTarget'))


@case('A4', '字段/钩子注释写明「锁血那一刻抄存、live 优先、取不到才回落」的理由',
      mut_drop('mob', r'//\s*锁血那一刻先在这里抄一份携带状态[^\n]*\r?\n'))
def a4(S):
    return has(S['mob']['raw'], '锁血那一刻先在这里抄一份携带状态') and \
        has(S['mob']['raw'], 'live 优先') and has(S['mob']['raw'], '取不到才回落')


@case('A5', 'Mob.die 的判据把留存算进 live（`… != null || geburaAimMarked`）',
      mut_sub('mob', r'\|\|\s*geburaAimMarked', '|| false'))
def a5(S):
    return 'boolean aimMarked = buff(AimHeartMark.class) != null || geburaAimMarked;' in \
        sq(body(S, 'mob', 'public void die( Object cause )'))


@case('A6', 'Mob.die 的判据把留存算进 live（`… != null || geburaInstrTarget`）',
      mut_sub('mob', r'\|\|\s*geburaInstrTarget', '|| false'))
def a6(S):
    return 'boolean instrTarget = buff(InstructionTarget.class) != null || geburaInstrTarget;' in \
        sq(body(S, 'mob', 'public void die( Object cause )'))


@case('A7', '两份留存都在 die() 里被消费成 false（一次性，不会二次触发）',
      mut_sub('mob', r'geburaAimMarked = false;\s*//', 'geburaAimMarked = true;\t//'))
def a7(S):
    d = body(S, 'mob', 'public void die( Object cause )')
    return 'geburaAimMarked = false;' in sq(d) and 'geburaInstrTarget = false;' in sq(d)


@case('A8', '消费发生在读取**之后**（先取布尔、再清字段），否则回落会被自己抹掉',
      mut_sub('mob',
              r'(boolean aimMarked[^\n]*\r?\n)(\s*boolean instrTarget[^\n]*\r?\n)'
              r'(\s*geburaAimMarked = false;[^\n]*\r?\n\s*geburaInstrTarget = false;[^\n]*\r?\n)',
              r'\3\1\2'))
def a8(S):
    d = body(S, 'mob', 'public void die( Object cause )')
    i_read = d.find('boolean aimMarked')
    i_clear = d.find('geburaAimMarked = false;')
    return 0 <= i_read < i_clear


@case('A9', '注释点明「必须放在 super.die(cause) 之前」的时序约束（后人别挪）',
      mut_drop('mob', r'//\s*⚠️\s*必须放在 super\.die\(cause\) 之前[^\n]*\r?\n'))
def a9(S):
    return has(S['mob']['raw'], '必须放在 super.die(cause) 之前')


# ---------------------------------------------------------------- B 组
@case('B1', 'Trials 闸门：抄写两行的原样形式（纯读取 buff 是否存在）',
      mut_drop('trials', r'mob\.geburaAimMarked = mob\.buff\( AimHeartMark\.class \) != null;'))
def b1(S):
    t = body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src )')
    return has(t, 'mob.geburaAimMarked = mob.buff( AimHeartMark.class ) != null;') and \
        has(t, 'mob.geburaInstrTarget = mob.buff( InstructionTarget.class ) != null;')


@case('B2', '抄写在 `geburaUsed` 门闩之后（没走到闸门就不该抄）',
      mut_sub('trials', r'(mob\.geburaUsed = true;)', r'mob.geburaAimMarked = true; \1'))
def b2(S):
    t = body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src )')
    i_gate = t.find('mob.geburaUsed = true;')
    i_copy = t.find('mob.geburaAimMarked =')
    return 0 <= i_gate < i_copy


@case('B3', '抄写在 `Buff.affect( mob, GeburaGrace.class )` **之前**（挂 buff 前读完状态）',
      mut_sub('trials', r'(mob\.geburaAimMarked =)',
              r'Buff.affect( mob, GeburaGrace.class ).set( turns, src ); \1'))
def b3(S):
    t = body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src )')
    i_copy = t.find('mob.geburaAimMarked =')
    i_buff = t.find('Buff.affect( mob, GeburaGrace.class )')
    return 0 <= i_copy < i_buff


@case('B4', '闸门体内不出现 `Random`（本考验「不改随机流」的硬验收）',
      mut_sub('trials', r'(mob\.geburaUsed = true;)', r'\1 int _r = com.watabou.utils.Random.Int(3);'))
def b4(S):
    return 'Random' not in body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src )')


@case('B5', '接 GEBURA 的门控仍是首行守卫（未开启时逐字不变）',
      mut_drop('trials', r'if \(!Dungeon\.isTrialled\( GEBURA \) \|\| !\(ch instanceof Mob\)\) return false;'))
def b5(S):
    return 'if (!Dungeon.isTrialled( GEBURA ) || !(ch instanceof Mob)) return false;' in \
        sq(body(S, 'trials', 'public static boolean interceptLethalDamage( Char ch, Object src )'))


@case('B6', 'Trials 已 import 两个 buff 类（否则抄写编译不过）',
      mut_drop('trials', r'import com\.shatteredpixel\.shatteredpixeldungeon\.actors\.buffs\.InstructionTarget;'))
def b6(S):
    c = S['trials']['code']
    return 'import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;' in c and \
        'import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget;' in c


# ---------------------------------------------------------------- C 组
@case('C1', '全仓只有「AimHeartMark 定义 + Mob.die 调用」两处出现 onKillByHero',
      mut_sub('char', r'(enemy\.damage\( effectiveDamage, this \);)',
              r'\1 if (this == Dungeon.hero && !enemy.isAlive()){ AimHeartMark.onKillByHero( (Hero) this ); }'))
def c1(S):
    hits = {k for k in FILES if 'onKillByHero' in S[k]['code']}
    return hits == {'aim', 'mob'}


@case('C2', '全仓只有「Instruction 定义 + Mob.die 调用」两处出现 onTargetKilled',
      mut_sub('char', r'(enemy\.damage\( effectiveDamage, this \);)',
              r'\1 Instruction.onTargetKilled(Dungeon.hero);'))
def c2(S):
    hits = {k for k in FILES if 'onTargetKilled' in S[k]['code']}
    return hits == {'instr', 'mob'}


@case('C3', 'Char.java 里那颗「攻击前记下标记」的局部变量已删除',
      mut_sub('char', r'(public void die\( Object src \) \{)',
              r'boolean aimMarkedBeforeHit = false; \1'))
def c3(S):
    return 'aimMarkedBeforeHit' not in S['char']['code']


@case('C4', 'Char.java 不再写「打完立刻死才回血」的收尾块',
      mut_sub('char', r'(enemy\.damage\( effectiveDamage, this \);)',
              r'\1 if (this == Dungeon.hero && !enemy.isAlive()){ AimHeartMark.onKillByHero( (Hero) this ); }'))
def c4(S):
    c = S['char']['code']
    return 'onKillByHero' not in c and 'AimHeartMark.onKillByHero' not in c


@case('C5', 'PalermoFencing 不再在 tryExecute 里补一句 onKillByHero（死代码已清）',
      mut_sub('palermo', r'(enemy\.die\( hero \);)', r'\1 AimHeartMark.onKillByHero( hero );'))
def c5(S):
    return 'onKillByHero' not in S['palermo']['code']


@case('C6', 'Mob 的**英雄击杀分支**里那句 `buff(InstructionTarget.class) != null` 已消失',
      mut_sub('mob', r'(Instruction\.onEnemyKilled\(Dungeon\.hero\);)',
              r'\1 if (buff(InstructionTarget.class) != null){ Talent.onOracleBlessing(Dungeon.hero); Instruction.onTargetKilled(Dungeon.hero); }'))
def c6(S):
    d = body(S, 'mob', 'public void die( Object cause )')
    return 'buff(InstructionTarget.class) != null){' not in sq(d)


@case('C7', '新判据落在 alignment == ENEMY 块内、且在 super.die() **之前**',
      mut_sub('mob', r'(boolean aimMarked)', r'super.die( cause ); \1'))
def c7(S):
    d = body(S, 'mob', 'public void die( Object cause )')
    i_enemy = d.find('if (alignment == Alignment.ENEMY){')
    i_hook = d.find('boolean aimMarked')
    i_super = d.find('super.die( cause );')
    return 0 <= i_enemy < i_hook < i_super


@case('C8', '英雄击杀分支的既有行为一字未改（onEnemyKilled 仍在）',
      mut_drop('mob', r'Instruction\.onEnemyKilled\(Dungeon\.hero\);'))
def c8(S):
    return 'Instruction.onEnemyKilled(Dungeon.hero);' in \
        sq(body(S, 'mob', 'public void die( Object cause )'))


@case('C9', '两个入口的签名仍是 `(Hero)`（判据能把英雄交出去）',
      mut_sub('instr', r'public static void onTargetKilled\(Hero hero\)',
              'public static void onTargetKilled(Char hero)'))
def c9(S):
    return 'public static void onKillByHero( Hero hero ){' in sq(S['aim']['code']) and \
        'public static void onTargetKilled(Hero hero){' in sq(S['instr']['code'])


@case('C10', 'Mob 已 import AimHeartMark / Instruction / InstructionTarget',
      mut_drop('mob', r'import com\.shatteredpixel\.shatteredpixeldungeon\.actors\.buffs\.Instruction;'))
def c10(S):
    c = S['mob']['code']
    return all(('import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.%s;' % n) in c
               for n in ('AimHeartMark', 'Instruction', 'InstructionTarget'))


@case('C11', '撤掉死代码后没留孤儿 import：PalermoFencing 里 AimHeartMark 彻底不见了',
      mut_sub('palermo', r'(package com\.shatteredpixel\.shatteredpixeldungeon\.items\.weapon\.melee;)',
              r'\1 import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;'))
def c11(S):
    return 'AimHeartMark' not in S['palermo']['code'] and 'InstructionTarget' not in S['char']['code']


# ---------------------------------------------------------------- D 组
@case('D1', 'Level.handlesChasmFall() 默认返回 false（＝原版：落坑换层）',
      mut_sub('level', r'(public boolean handlesChasmFall\(\)\{\s*return )false', r'\1true'))
def d1(S):
    return 'return false;' in sq(body(S, 'level', 'public boolean handlesChasmFall(){'))


@case('D2', '它是**实例方法**（public、非 static）⇒ Chasm 能按当前层多态分派',
      mut_sub('level', r'public boolean handlesChasmFall\(\)\{', 'public static boolean handlesChasmFall(){'))
def d2(S):
    c = S['level']['code']
    return 'public boolean handlesChasmFall(){' in c and 'static boolean handlesChasmFall' not in c


@case('D3', 'MiningLevel 覆写它，并按**任务类型**门控（只有水晶任务才不换层）',
      mut_sub('mine', r'return Blacksmith\.Quest\.Type\(\) == Blacksmith\.Quest\.CRYSTAL;', 'return true;'))
def d3(S):
    return 'return Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL;' in \
        sq(body(S, 'mine', 'public boolean handlesChasmFall(){'))


@case('D4', '覆写处带 @Override（签名漂移会立刻编译失败）',
      mut_sub('mine', r'@Override(\s*public boolean handlesChasmFall)', r'\1'))
def d4(S):
    return has(S['mine']['raw'], '@Override public boolean handlesChasmFall(){')


# ---------------------------------------------------------------- E 组
@case('E1', 'Chasm.heroFall 的「本层自理」分支**在 beforeTransition 之前**',
      mut_drop('chasm',
               r'if \(Dungeon\.level != null && Dungeon\.level\.handlesChasmFall\(\) && Dungeon\.hero\.isAlive\(\)\)\s*'
               r'\{\s*heroRiftReturn\(\);\s*return;\s*\}'))
def e1(S):
    f = body(S, 'chasm', 'public static void heroFall( int pos )')
    i_guard = f.find('Dungeon.level.handlesChasmFall()')
    i_call = f.find('heroRiftReturn();')
    i_bt = f.find('Level.beforeTransition();')
    return 0 <= i_guard < i_call < i_bt


@case('E2', '该分支确实 `return`（不会继续走到换层那几行）',
      mut_sub('chasm', r'(heroRiftReturn\(\);)\s*\r?\n\s*return;', r'\1'))
def e2(S):
    f = body(S, 'chasm', 'public static void heroFall( int pos )')
    i_call = f.find('heroRiftReturn();')
    return i_call >= 0 and 'return;' in sq(f[i_call:i_call + 40])


@case('E3', '分支守卫含 `Dungeon.hero.isAlive()`（阵亡时不搞位移）',
      mut_sub('chasm', r'&& Dungeon\.hero\.isAlive\(\)', '&& true'))
def e3(S):
    return 'Dungeon.level != null && Dungeon.level.handlesChasmFall() && Dungeon.hero.isAlive()' in \
        sq(body(S, 'chasm', 'public static void heroFall( int pos )'))


@case('E4', 'heroRiftReturn 存在、私有、静态（只经 heroFall 一个入口）',
      mut_sub('chasm', r'private static void heroRiftReturn\(\)', 'public static void heroRiftReturn()'))
def e4(S):
    return 'private static void heroRiftReturn() {' in sq(S['chasm']['code'])


@case('E5', 'heroRiftReturn 复用 ScrollOfTeleportation.appear 把英雄挪到本层另一格',
      mut_drop('chasm', r'ScrollOfTeleportation\.appear\( hero, cell \);'))
def e5(S):
    return 'ScrollOfTeleportation.appear( hero, cell );' in \
        sq(body(S, 'chasm', 'private static void heroRiftReturn() {'))


@case('E6', '落脚点来自 Level.randomRespawnCell（与传送卷轴同源，天然避开深渊/密室）',
      mut_sub('chasm', r'Dungeon\.level\.randomRespawnCell\( hero \)', '0'))
def e6(S):
    b = body(S, 'chasm', 'private static void heroRiftReturn() {')
    return 'Dungeon.level.randomRespawnCell( hero )' in sq(b) and 'Dungeon.level.secret[c]' in sq(b)


@case('E7', '坠落伤害直接复用原版 heroLand()（残废 + 流血 + 伤害，羽落秘药照常免伤）',
      mut_drop('chasm', r'heroLand\(\);', count=2))
def e7(S):
    return 'heroLand();' in sq(body(S, 'chasm', 'private static void heroRiftReturn() {')) and \
        'public static void heroLand()' in sq(S['chasm']['code'])


@case('E8', '提示走 messages 键 rift_return（三语齐全，见 H 组）',
      mut_drop('chasm', r'GLog\.i\( Messages\.get\(Chasm\.class, "rift_return"\) \);'))
def e8(S):
    return 'GLog.i( Messages.get(Chasm.class, "rift_return") );' in \
        sq(body(S, 'chasm', 'private static void heroRiftReturn() {'))


@case('E9', '不换层 ⇒ heroRiftReturn 体内**不出现** InterlevelScene / switchScene',
      mut_sub('chasm', r'(heroLand\(\);)',
              r'InterlevelScene.mode = InterlevelScene.Mode.FALL; \1'))
def e9(S):
    b = body(S, 'chasm', 'private static void heroRiftReturn() {')
    return 'InterlevelScene' not in b and 'switchScene' not in b


@case('E10', '取不到落脚点时保持原状（不做无谓位移）',
      mut_drop('chasm', r'if \(cell == -1\) return;'))
def e10(S):
    return 'if (cell == -1) return;' in sq(body(S, 'chasm', 'private static void heroRiftReturn() {'))


@case('E11', 'Chasm 已 import ScrollOfTeleportation（否则编译不过）',
      mut_drop('chasm', r'import com\.shatteredpixel\.shatteredpixeldungeon\.items\.scrolls\.ScrollOfTeleportation;'))
def e11(S):
    return 'import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;' \
        in S['chasm']['code']


# ---------------------------------------------------------------- F 组
@case('F1', 'carveVoid 的签名与可见性（public static，房间可直接调）',
      mut_sub('mine', r'public static void carveVoid', 'static void carveVoid'))
def f1(S):
    return 'public static void carveVoid( Level level, Room room, int protectedCell ){' in sq(S['mine']['code'])


@case('F2', '只把**地板**换成深渊：`!= EMPTY && != EMPTY_DECO` 就跳过',
      mut_sub('mine', r'if \(map\[cell\] != Terrain\.EMPTY && map\[cell\] != Terrain\.EMPTY_DECO\) continue;',
              'if (false) continue;'))
def f2(S):
    return 'if (map[cell] != Terrain.EMPTY && map[cell] != Terrain.EMPTY_DECO) continue;' in \
        sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))


@case('F3', '体内从不写 Terrain.WALL（「替代地板而不是墙体」的字面落点）',
      mut_sub('mine', r'(Painter\.set\( level, cell, Terrain\.CHASM \);)',
              r'Painter.set( level, cell, Terrain.WALL ); \1'))
def f3(S):
    return 'Terrain.WALL' not in \
        body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')


@case('F4', '落在深渊上的只有 Painter.set(..., Terrain.CHASM) 一种地形（两处写入点都必须是这样）',
      mut_sub('mine', r'Terrain\.CHASM \);', 'Terrain.EMPTY_DECO );', count=2))
def f4(S):
    return 'Painter.set( level, cell, Terrain.CHASM );' in \
        sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))


@case('F5', '不动矿脉 MINE_CRYSTAL（体内根本不出现该地形）',
      mut_sub('mine', r'(int cell = x \+ y \* w;)',
              r'\1 if (map[cell] == Terrain.MINE_CRYSTAL) continue;'))
def f5(S):
    return 'MINE_CRYSTAL' not in \
        body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')


@case('F6', '深内区 = 房间内圈再各缩 2 格 ⇒ 外面永远留着一整圈地板（不封死门口）',
      mut_sub('mine', r'room\.left \+ 2, right = room\.right - 2', 'room.left, right = room.right'))
def f6(S):
    b = body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')
    return 'int left = room.left + 2, right = room.right - 2;' in sq(b) and \
        'int top = room.top + 2, bottom = room.bottom - 2;' in sq(b)


@case('F7', '半径有界（1~3），并显式排除过小的房间',
      mut_sub('mine', r'radius = Math\.max\( 1, Math\.min\( 3, radius \) \);', 'radius = 99;'))
def f7(S):
    b = body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')
    return 'radius = Math.max( 1, Math.min( 3, radius ) );' in sq(b) and \
        'if (right < left || bottom < top) return;' in sq(b)


@case('F8', '圆心候选避开 protectedCell 及其**邻域**（切比雪夫距离 ≤ radius+1 的全跳过）',
      mut_sub('mine', r'<= radius \+ 1\) continue;', '< 0) continue;'))
def f8(S):
    return 'Math.max( Math.abs( x - px ), Math.abs( y - py ) ) <= radius + 1' in \
        sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))


@case('F9', '没有候选圆心时安全退出（不做半截挖掘）',
      mut_sub('mine', r'if \(centers\.isEmpty\(\)\) return;', 'if (false) return;'))
def f9(S):
    b = body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')
    return 'if (centers.isEmpty()) return;' in sq(b) and 'Random.element( centers )' in sq(b)


@case('F10', '外圈按 50% 概率保留（边缘不规则，照 FissureRoom 的写法）——抽中的先入 fringe 缓冲',
      mut_drop('mine', r'if \(Random\.Int\(2\) == 0\) continue;'))
def f10(S):
    b = sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))
    return 'if (Random.Int(2) == 0) continue;' in b and 'fringe.add( cell );' in b


@case('F16', '⭐ 整块圆盘夹在深内区里（越界格直接跳过）⇒ 外面必留一整圈地板、也不会贴墙',
      mut_drop('mine', r'if \(x < left \|\| x > right \|\| y < top \|\| y > bottom\) continue;'))
def f16(S):
    return 'if (x < left || x > right || y < top || y > bottom) continue;' in \
        sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))


@case('F17', '⭐ 第二趟把外圈里「四邻皆空」的孤立格收回 ⇒ 不留孤零零的单格陷阱',
      mut_sub('mine', r'map\[cell - 1\] == Terrain\.CHASM', 'false'))
def f17(S):
    b = sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))
    return 'for (int cell : fringe){' in b \
        and 'map[cell - 1] == Terrain.CHASM || map[cell + 1] == Terrain.CHASM' in b \
        and 'map[cell - w] == Terrain.CHASM || map[cell + w] == Terrain.CHASM' in b \
        and 'Painter.set( level, cell, Terrain.CHASM );' in b


@case('F11', '不动已有堆叠物（heaps 上还有东西的格不挖）',
      mut_drop('mine', r'if \(level\.heaps\.get\( cell \) != null\) continue;'))
def f11(S):
    return 'if (level.heaps.get( cell ) != null) continue;' in \
        sq(body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){'))


@case('F12', '⚠️ 只改 map[]（不碰 pit[]/updateCellFlags）——这个前提被注释显式记录',
      mut_drop('mine', r'⚠️ 这里只写 \{@code map\[\]\}[^\n]*\r?\n'))
def f12(S):
    return has(S['mine']['raw'], '这里只写') and has(S['mine']['raw'], 'buildFlagMaps()') and \
        has(S['mine']['raw'], '幽灵格')


@case('F13', '该前提为真：Level.create() 里 buildFlagMaps() 在 while(!build()) 之后（画完地形才重算 flags）',
      mut_sub('level', r'\} while \(!build\(\)\);\s*buildFlagMaps\(\);', '} while (!build());'))
def f13(S):
    return has(S['level']['code'], '} while (!build()); buildFlagMaps(); cleanWalls(); createMobs();')


@case('F14', 'carveVoid 体内确实没有 pit[] / updateCellFlags 的写入',
      mut_sub('mine', r'(Painter\.set\( level, cell, Terrain\.CHASM \);)', r'level.pit[cell] = true; \1'))
def f14(S):
    b = body(S, 'mine', 'public static void carveVoid( Level level, Room room, int protectedCell ){')
    return 'level.pit' not in b and 'updateCellFlags' not in b


@case('F15', '调用时机写在房间 paint() 里（不是画师的 decorate()）——注释显式说明',
      mut_drop('mine', r'调用时机＝房间自己的 \{@code paint\(\)\} 阶段[^\n]*\r?\n'))
def f15(S):
    return has(S['mine']['raw'], '调用时机＝房间自己的') and has(S['mine']['raw'], 'paint()')


# ---------------------------------------------------------------- G 组
@case('G1', 'MineSmallRoom：CRYSTAL 分支内调 carveVoid(…, -1)（没有需保护的关键单位）',
      mut_sub('small', r'MiningLevel\.carveVoid\( level, this, -1 \);',
              'MiningLevel.carveVoid( level, this, 0 );'))
def g1(S):
    return 'MiningLevel.carveVoid( level, this, -1 );' in sq(S['small']['code'])


@case('G2', 'MineLargeRoom：保护水晶守卫的落脚点（level.pointToCell(p)）',
      mut_sub('large', r'MiningLevel\.carveVoid\( level, this, level\.pointToCell\(p\) \);',
              'MiningLevel.carveVoid( level, this, -1 );'))
def g2(S):
    return 'MiningLevel.carveVoid( level, this, level.pointToCell(p) );' in sq(S['large']['code'])


@case('G3', 'MineGiantRoom：保护水晶尖塔的落脚点（level.pointToCell(p)）',
      mut_sub('giant', r'MiningLevel\.carveVoid\( level, this, level\.pointToCell\(p\) \);',
              'MiningLevel.carveVoid( level, this, -1 );'))
def g3(S):
    return 'MiningLevel.carveVoid( level, this, level.pointToCell(p) );' in sq(S['giant']['code'])


@case('G4', '三处调用都落在 CRYSTAL 分支内（其后才是 GNOLL 分支）',
      mut_drop('small', r'MiningLevel\.carveVoid\( level, this, -1 \);'))
def g4(S):
    for k in ('small', 'large', 'giant'):
        c = S[k]['code']
        i_cr = c.find('Blacksmith.Quest.CRYSTAL')
        i_call = c.find('MiningLevel.carveVoid')
        i_gn = c.find('Blacksmith.Quest.GNOLL')
        if not (0 <= i_cr < i_call < i_gn):
            return False
    return True


@case('G5', '三处都 import 了 MiningLevel（否则编译不过）',
      mut_drop('small', r'import com\.shatteredpixel\.shatteredpixeldungeon\.levels\.MiningLevel;'))
def g5(S):
    return all('import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;' in S[k]['code']
               for k in ('small', 'large', 'giant'))


@case('G6', '画师先给「房间里的虚空」拍快照（在 super.decorate **之前**）',
      mut_sub('painter', r'boolean\[\] roomVoid = new boolean\[level\.length\(\)\];',
              'boolean[] roomVoid = null;'))
def g6(S):
    d = body(S, 'painter', 'protected void decorate(Level level, ArrayList<Room> rooms)')
    i_snap = d.find('boolean[] roomVoid = new boolean[level.length()];')
    i_super = d.find('super.decorate(level, rooms);')
    return 0 <= i_snap < i_super


@case('G7', '快照只在水晶任务下拍（其它任务保持原行为）',
      mut_sub('painter', r'if \(Blacksmith\.Quest\.Type\(\) == Blacksmith\.Quest\.CRYSTAL\)\{', 'if (true){'))
def g7(S):
    return 'if (Blacksmith.Quest.Type() == Blacksmith.Quest.CRYSTAL){' in \
        sq(body(S, 'painter', 'protected void decorate(Level level, ArrayList<Room> rooms)'))


@case('G8', '还原只作用于「房间之间」那批（`&& !roomVoid[i]` 是真条件）',
      mut_sub('painter', r'&& !roomVoid\[i\]', '&& true'))
def g8(S):
    d = body(S, 'painter', 'protected void decorate(Level level, ArrayList<Room> rooms)')
    return 'if (level.map[i] == Terrain.CHASM && !roomVoid[i]){' in sq(d) and \
        'level.map[i] = Terrain.EMPTY;' in sq(d)


@case('G9', '画师已 import Blacksmith（否则编译不过）',
      mut_drop('painter', r'import com\.shatteredpixel\.shatteredpixeldungeon\.actors\.mobs\.npcs\.Blacksmith;'))
def g9(S):
    return 'import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Blacksmith;' in \
        S['painter']['code']


# ---------------------------------------------------------------- H 组
def _msg_line(S, key, prefix):
    for ln in S[key]['raw'].splitlines():
        if ln.startswith(prefix):
            return ln
    return None


@case('H1', '英文键 rift_return 存在且非空',
      mut_sub('lv_en', r'levels\.features\.chasm\.rift_return=.*', 'levels.features.chasm.rift_return='))
def h1(S):
    ln = _msg_line(S, 'lv_en', 'levels.features.chasm.rift_return=')
    return bool(ln) and len(ln.split('=', 1)[1].strip()) > 0


@case('H2', '简中键＝用户指定原文「一股魔法乱流把你送回了这一层某处」',
      mut_sub('lv_zh', r'(levels\.features\.chasm\.rift_return=).*', r'\1送回了这一层'))
def h2(S):
    return _msg_line(S, 'lv_zh', 'levels.features.chasm.rift_return=') == \
        'levels.features.chasm.rift_return=一股魔法乱流把你送回了这一层某处'


@case('H3', '繁中键是简中的繁体转写（不能漏译 / 保留简体）',
      mut_sub('lv_hant', r'(levels\.features\.chasm\.rift_return=).*',
              r'\1一股魔法乱流把你送回了这一层某处'))
def h3(S):
    ln = _msg_line(S, 'lv_hant', 'levels.features.chasm.rift_return=')
    return bool(ln) and ln.endswith('一股魔法亂流把你送回了這一層某處') and '这一层' not in ln


@case('H4', '三个键都归在 features.chasm 段（不是散落别处）',
      mut_sub('lv_zh', r'levels\.features\.chasm\.rift_return=', 'levels.rift_return='))
def h4(S):
    return all(_msg_line(S, k, 'levels.features.chasm.rift_return=') is not None
               for k in ('lv_en', 'lv_zh', 'lv_hant'))


# ---------------------------------------------------------------- I 组（行为层）
def _probe():
    if not os.path.isfile(PROBE_OUT):
        return None
    return open(PROBE_OUT, encoding='utf-8', errors='replace').read()


@case('I1', '探针输出存在，且没有任何 [FAIL]', None)
def i1(S):
    t = _probe()
    return t is not None and '[FAIL]' not in t


@case('I2', '探针摘要：失败 0 条，且断言总数 ≥ 20（六段都真的跑到了）', None)
def i2(S):
    t = _probe()
    if not t:
        return False
    m = re.search(r'断言\s*(\d+)\s*条：通过\s*(\d+)，失败\s*(\d+)', t)
    if not m:
        return False
    total, passed, failed = (int(x) for x in m.groups())
    return failed == 0 and passed == total and total >= 20


@case('I3', '探针六段标题齐全（①~⑥ 都执行到底）', None)
def i3(S):
    t = _probe()
    return bool(t) and all(('--- %s ' % c) in t for c in '①②③④⑤⑥')


@case('I4', '行为实证①：带标记的怪死亡 → 荣耀凯旋回血（判据与「谁打死的」解耦）', None)
def i4(S):
    t = _probe()
    return bool(t) and '荣耀凯旋真的回血了' in t and '判据已与攻击调用栈解耦' in t


@case('I5', '行为实证②：带标志的怪死亡 → 神谕庇佑给盾；对照组不给', None)
def i5(S):
    t = _probe()
    return bool(t) and '神谕庇佑（+5 护盾）触发' in t and '对照组' in t


@case('I6', '行为实证③：live 取不到时回落到抄存（GEBURA 锁血的真实路径）', None)
def i6(S):
    t = _probe()
    return bool(t) and 'live 取不到时回落到抄存' in t


@case('I7', '行为实证④：负向对照——既无 live 又无抄存 ⇒ 什么都不发生', None)
def i7(S):
    t = _probe()
    return bool(t) and '负向对照' in t and '不回血' in t


@case('I8', '行为实证⑤：闸门真的抄下两个字段；关掉考验时保持初值', None)
def i8(S):
    t = _probe()
    return bool(t) and '抄存：geburaAimMarked = true' in t and '抄存：geburaInstrTarget = true' in t \
        and '关掉 GEBURA：抄存字段保持初值' in t


@case('I9', '行为实证⑥：闸门两次读取不碰随机数（同 seed 200 个随机数逐位相同）', None)
def i9(S):
    t = _probe()
    return bool(t) and '200 个随机数逐位相同' in t


# ---------------------------------------------------------------- J 组（行为层 · 探针 B）
def _probe_b():
    if not os.path.isfile(PROBE_B_OUT):
        return None
    return open(PROBE_B_OUT, encoding='utf-8', errors='replace').read()


@case('J1', '探针 B 输出存在，且没有任何 [FAIL]', None)
def j1(S):
    t = _probe_b()
    return t is not None and '[FAIL]' not in t


@case('J2', '探针 B 摘要：失败 0 条，且断言总数 ≥ 20（八段都真的跑到了）', None)
def j2(S):
    t = _probe_b()
    if not t:
        return False
    m = re.search(r'断言\s*(\d+)\s*条：通过\s*(\d+)，失败\s*(\d+)', t)
    if not m:
        return False
    total, passed, failed = (int(x) for x in m.groups())
    return failed == 0 and passed == total and total >= 20


@case('J3', '探针 B 八段标题齐全（①~⑧ 都执行到底）', None)
def j3(S):
    t = _probe_b()
    return bool(t) and all(('--- %s ' % c) in t for c in '①②③④⑤⑥⑦⑧')


@case('J4', '行为实证①：handlesChasmFall 只对 CRYSTAL 为真，其余（含基类）都 false', None)
def j4(S):
    t = _probe_b()
    return bool(t) and '水晶任务（CRYSTAL=1）⇒ 本层自理落坑（true）' in t \
        and '矮人任务（GNOLL=2）⇒ 交给原版换层（false）' in t \
        and '真菌任务（FUNGI=3）⇒ false' in t \
        and '未开始任务（type=0）⇒ false' in t \
        and '基类 Level 默认 false' in t


@case('J5', '行为实证②：只吃地板、整块夹在深内区、不贴墙、干净地板上是一整块', None)
def j5(S):
    t = _probe_b()
    return bool(t) and '只在原 EMPTY/EMPTY_DECO 上挖' in t \
        and '整块圆盘都夹在深内区 [4,15]² 内（越界 0 格）' in t \
        and '每个深渊格与房间内墙都至少隔 1 格（贴墙 0 格）' in t \
        and '干净地板上 30 次采样都是一整块' in t


@case('J6', '行为实证③：⭐ 只写 map[] 的幽灵格前提被实证（未重算时 pit[] 全 false、重算后全 true）', None)
def j6(S):
    t = _probe_b()
    return bool(t) and '未调 buildFlagMaps() 时深渊格的 pit[] 全是 false' in t \
        and 'buildFlagMaps() 之后每个深渊格 pit[] 都为 true' in t \
        and '且每个深渊格 passable[] 都为 false' in t


@case('J7', '行为实证④：关键单位及其八邻格从不被挖；同 seed 逐格可复现；地板不被虚空切成孤岛', None)
def j7(S):
    t = _probe_b()
    return bool(t) and '保护格及其八邻格一次都没被挖' in t \
        and '同一 seed 下两次 carveVoid 的 map[] 逐格相同' in t \
        and '地板全连通、一个孤岛都没有' in t


# 探针输出的反例：把输出文件换成一个「只破坏目标那一句」的坏版本
PMUT_TEXT = {
    'I1': '[FAIL]  故意塞一条失败\n断言 23 条：通过 22，失败 1（另有 0 条环境说明）\n',
    'I2': '断言 3 条：通过 3，失败 0（另有 0 条环境说明）\n',
    'I3': '--- ① a ---\n--- ② b ---\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I4': '荣耀凯旋没回血\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I5': '神谕庇佑（+5 护盾）触发\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I6': 'live 还在，不需要回落\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I7': '负向对照：还是回血了\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I8': '抄存：geburaAimMarked = false\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
    'I9': '200 个随机数第 37 位不同\n断言 23 条：通过 23，失败 0（另有 0 条环境说明）\n',
}

# 探针 B 的反例（同一口径，只是换 J 组的输出文件）
PMUT_B_TEXT = {
    'J1': '[FAIL]  故意塞一条失败\n断言 26 条：通过 25，失败 1（另有 0 条环境说明）\n',
    'J2': '断言 3 条：通过 3，失败 0（另有 0 条环境说明）\n',
    'J3': '--- ① a ---\n--- ② b ---\n断言 26 条：通过 26，失败 0（另有 0 条环境说明）\n',
    'J4': '水晶任务（CRYSTAL=1）⇒ 交给原版换层（false）\n断言 26 条：通过 26，失败 0（另有 0 条环境说明）\n',
    'J5': '整块圆盘都夹在深内区 [4,15]² 内（越界 3 格）\n断言 26 条：通过 26，失败 0（另有 0 条环境说明）\n',
    'J6': 'buildFlagMaps() 之后深渊格 pit[] 仍是 false\n断言 26 条：通过 26，失败 0（另有 0 条环境说明）\n',
    'J7': '保护格被挖了 3 格\n断言 26 条：通过 26，失败 0（另有 0 条环境说明）\n',
}


def run_cases(S, only=None):
    for cid, desc, fn, _mut in CASES:
        if only and cid not in only:
            continue
        try:
            r = fn(S)
        except Exception:
            r = False
        chk(bool(r), '[%s] %s' % (cid, desc))


def main():
    print('=' * 78)
    print('「带着标记倒下」击杀判定 + 水晶矿洞层虚空地形 · 核验（2026-09-26）')
    print('=' * 78)

    groups = [
        ('A. Mob 的两个「带着标记倒下」留存字段', 'A'),
        ('B. 闸门的抄写（Trials.interceptLethalDamage）', 'B'),
        ('C. 判据收口：旧判据撤除、全仓调用点唯一', 'C'),
        ('D. Level.handlesChasmFall 的默认 / 覆写', 'D'),
        ('E. Chasm 的不换层分支与 heroRiftReturn', 'E'),
        ('F. MiningLevel.carveVoid 的地形规则', 'F'),
        ('G. 三个矿洞房间的调用点 + 画师保虚空', 'G'),
        ('H. 三份 levels*.properties 的文本键', 'H'),
        ('I. 行为层（MarkedKillProbe 的实证输出）', 'I'),
        ('J. 行为层（MiningVoidProbe 的实证输出：地形不变量 + 幽灵格前提）', 'J'),
    ]
    S = load()
    for title, g in groups:
        print('\n-- %s --' % title)
        run_cases(S, only={c[0] for c in CASES if c[0].startswith(g)})

    print('\n' + '=' * 78)
    if ok:
        print('全部核验通过（%d 条）。' % len(results))
    else:
        print('有 %d 条与预期不符。' % sum(1 for r in results if not r))
    print('=' * 78)

    if '--selftest' in sys.argv:
        print('\n反例自测（每条判据喂一个「改坏」的变体，必须判 FAIL；不计总账）')
        bad = 0
        mut_all = dict(PMUT_TEXT)
        mut_all.update(PMUT_B_TEXT)
        for cid, desc, fn, _mut in CASES:
            if cid in mut_all:
                # I/J 组按输出文件判 ⇒ 反例＝换掉输出文件（跑完恢复）
                path = PROBE_B_OUT if cid in PMUT_B_TEXT else PROBE_OUT
                real = open(path, encoding='utf-8').read() if os.path.isfile(path) else None
                try:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(mut_all[cid])
                    try:
                        r = fn(load())
                    except Exception:
                        r = False
                    print(('  [FAIL] %s 反例仍判 PASS ⇒ 判据抓不到这类错' % cid) if r
                          else ('  [OK]   %s 反例已判 FAIL（判据有效）' % cid))
                    if r:
                        bad += 1
                finally:
                    if real is None:
                        os.remove(path)
                    else:
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(real)
                continue
            mut = _mut
            if mut is None:
                print('  [SKIP] %s 无反例' % cid)
                continue
            S2 = load()
            try:
                mut(S2)
            except AssertionError as e:
                print('  [SKIP] %s 反例变异未命中（%s）' % (cid, e))
                continue
            try:
                r = fn(S2)
            except Exception:
                r = False
            if r:
                print('  [FAIL] %s 反例仍判 PASS ⇒ 判据抓不到这类错' % cid)
                bad += 1
            else:
                print('  [OK]   %s 反例已判 FAIL（判据有效）' % cid)
        print('\n反例自测：%d 条判据，%d 条抓不到错。' % (len(CASES), bad))
        if bad:
            sys.exit(2)

    if not ok:
        sys.exit(1)


if __name__ == '__main__':
    main()
