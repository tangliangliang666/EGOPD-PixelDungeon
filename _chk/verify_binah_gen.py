# 核验考验「BINAH（理解）」的生成时等级规则实现。
#
# 为什么需要它：BINAH 与 HOKMA 是**两类**规则 —— HOKMA 是「一切结算之后再夹」的全局规则，
# BINAH 是「道具刚生成的那一瞬间」的规则。所以核验不能只看「函数写没写」，必须证明：
#   · 收口是唯一的（`Item.random()` final，全仓没有第二个 `public Item random()`）；
#   · **两步判定的顺序**正确（先「因诅咒反转」、后「正等级归零」）—— 顺序反了整个考验就退化；
#   · 四处任务 NPC 的压平都发生在**它们自己加等级之后**；
#   · **随机流一点没动**（关卡种子布局不能漂移）。
#
# 分四层：
#   ① 源码结构层：final 收口、8 个子类改名、两步顺序（比位置）、四处 NPC 调用点比位置。
#   ② 编译产物层：javap -p 看签名 —— 源码解析可能被注释骗到，这里看真编出来的东西。
#   ③ 字节码层：javap -c 看指令 —— 证明 `ineg`（反转）真的**先于** `iconst_0`（归零）
#      执行，且两个方法体里**没有** `com/watabou/utils/Random` 调用（RNG 不变）。
#      这一层专门防「顺序写反」「偷偷用了 Random」，是纯源码断言抓不到的。
#   ④ 备份差异层：8 个子类的 `randomRaw()` 方法体必须与改动前**逐字节一致**（只差签名一行）。
#
# 用法：
#   python _chk/verify_binah_gen.py            # 需先按 skill 的单文件 javac 命令编到 _chk/_javachk
#   python _chk/verify_binah_gen.py --selftest # 反例自测：证明「顺序」判据不恒真
import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk')
BAK = os.path.join(HERE, '_bak_binah')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'

HEADER = 'com.shatteredpixel.shatteredpixeldungeon'

# 8 个子类：类全名 -> 相对源码路径
SUBCLASSES = {
    'items.armor.Armor': 'items/armor/Armor.java',
    'items.artifacts.Artifact': 'items/artifacts/Artifact.java',
    'items.bombs.Bomb': 'items/bombs/Bomb.java',
    'items.Gold': 'items/Gold.java',
    'items.rings.Ring': 'items/rings/Ring.java',
    'items.wands.Wand': 'items/wands/Wand.java',
    'items.weapon.missiles.MissileWeapon': 'items/weapon/missiles/MissileWeapon.java',
    'items.weapon.Weapon': 'items/weapon/Weapon.java',
}

# 四处任务 NPC：(相对路径, [(自家加等级的标志串, 压平调用的标志串, 说明)])
QUESTS = {
    'actors/mobs/npcs/Wandmaker.java': [
        ('wand1.upgrade();', 'flattenQuestReward( wand1 )', '制杖人 wand1 +1'),
        ('wand2.upgrade();', 'flattenQuestReward( wand2 )', '制杖人 wand2 +1'),
    ],
    'actors/mobs/npcs/Ghost.java': [
        ('armor.upgrade(itemLevel);', 'flattenQuestReward( weapon )', '幽灵武器 +0~+3'),
        ('armor.upgrade(itemLevel);', 'flattenQuestReward( armor )', '幽灵护甲 +0~+3'),
    ],
    'actors/mobs/npcs/Blacksmith.java': [
        ('i.level(rewardLevel);', 'flattenQuestReward( i )', '铁匠 +0~+3'),
    ],
    'actors/mobs/npcs/Imp.java': [
        ('reward.upgrade( 2 );', 'flattenQuestReward( reward )', '小恶魔 +2'),
    ],
}

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def readb(p):
    return open(p, 'rb').read()


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

    ⚠️ 这是本项目反复踩过的坑：注释里写的伪代码、`updateQuickslot` 之类的词
    会被 `in` 匹配到，于是断言在「代码根本没写」时也报 OK（或反过来误报 FAIL）。"""
    raw = read(rel)
    assert '"""' not in raw, '出现 Java 文本块，strip_comments 需要升级'
    return strip_comments(raw)


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


def method_body(disasm, header_prefix):
    """从 javap -c 输出里截出某个方法体的指令列表（到下一个方法声明或结尾）。

    返回 [(offset, opcode, rest)]。找不到方法返回 None。"""
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
def negate_before_zero(ins):
    """判断「因诅咒反转」是否**先于**「正等级归零」执行。

    字节码事实（实测）：
        ... getfield cursed / ifeq -> iload_1 / ineg / istore_1 / iload_1 / ifle -> iconst_0 / istore_1 ...
    返回 (ok, 说明)。反转 = 指令 `ineg`；归零 = `iconst_0` 且**下一条是 istore_1**。
    两者取偏移比较 —— 比语义、不比存在（光看「有没有 ineg」抓不出顺序写反）。"""
    if not ins:
        return False, '空方法体'
    neg = [o for (o, op, _) in ins if op == 'ineg']
    zero = []
    for idx, (o, op, _) in enumerate(ins):
        if op == 'iconst_0' and idx + 1 < len(ins) and ins[idx + 1][1] == 'istore_1':
            zero.append(o)
    if not neg:
        return False, '没有 ineg（反转缺失）'
    if not zero:
        return False, '没有 iconst_0/istore_1（归零缺失）'
    return neg[0] < zero[0], 'ineg@%d vs iconst_0@%d' % (neg[0], zero[0])


def selftest():
    print('== selftest：确认「顺序」判据不恒真 ==')
    good = [(0, 'aload_0', ''), (1, 'getfield', 'cursed'), (4, 'ifeq', '10'),
            (7, 'ineg', ''), (8, 'istore_1', ''), (9, 'ifle', '14'),
            (12, 'iconst_0', ''), (13, 'istore_1', '')]
    bad = [(0, 'iload_1', ''), (1, 'ifle', '6'), (4, 'iconst_0', ''), (5, 'istore_1', ''),
           (6, 'getfield', 'cursed'), (9, 'ifeq', '15'), (12, 'ineg', ''), (13, 'istore_1', '')]
    no_neg = [(0, 'iload_1', ''), (1, 'ifle', '6'), (4, 'iconst_0', ''), (5, 'istore_1', '')]
    cases = [
        (good, True, '先 ineg 后归零（正确）'),
        (bad, False, '先归零后 ineg（顺序写反，必须抓出来）'),
        (no_neg, False, '压根没有 ineg（考验会退化成全 +0）'),
        ([], False, '空方法体'),
    ]
    for ins, want, desc in cases:
        got, why = negate_before_zero(ins)
        print('  %-34s -> %-5s（期望 %-5s） %s' % (desc, got, want, why))
        assert got == want, '判据错：%s 实得 %s 期望 %s' % (desc, got, want)
    print('  selftest 通过：抓得住顺序写反与反转缺失，不会恒真。')

    print('== selftest：确认 strip_comments 保偏移、不吞代码 ==')
    sc = [
        ('int a = 1; // if (x) y = 0;\nint b = 2;\n', 'int b = 2;', '行注释被剥'),
        ('/* if (x) y = 0; */int b = 2;\n', 'int b = 2;', '块注释被剥'),
        ('//** 注释里带星号 */ 之后\nint b = 2;\n', 'int b = 2;', '`//**` 不吞后续（正则写法会一路吞掉）'),
        ('String s = "// 不是注释";int b = 2;\n', '// 不是注释', '字符串里的 // 保留'),
        ('char q = \'/\'; int b = 2;\n', 'int b = 2;', "字符字面量不误开注释"),
    ]
    for src, must_keep, desc in sc:
        got = strip_comments(src)
        assert len(got) == len(src), '偏移没保住：%s' % desc
        assert must_keep in got, '被误剥：%s' % desc
        if 'if (x) y = 0;' in src and '//**' not in src:
            assert 'if (x) y = 0;' not in got, '注释内容没剥掉：%s' % desc
        print('  %-42s -> OK' % desc)
    print('  selftest 通过：剥注释且字符偏移不变。\n')


print('=' * 78)
print('核验：考验 BINAH 的生成时等级规则')
print('=' * 78)

if '--selftest' in sys.argv:
    selftest()


# ================================================================ ① 源码结构层
print('== ① 源码结构层 ==')

trials = read_j('Trials.java')
item = read_j('items/Item.java')

chk(re.search(r'public static final int BINAH\s*=\s*4\s*;', trials) is not None,
    'Trials.BINAH == 4（位号与 MASKS 顺序一致）')

chk(re.search(r'public final Item random\(\)\s*\{', item) is not None,
    'Item.random() 已 final（编译器保证没有第二个出口）')
chk('Trials.modifyGeneratedItem( randomRaw() )' in item,
    'Item.random() 转调 Trials.modifyGeneratedItem(randomRaw())')
chk(re.search(r'protected Item randomRaw\(\)\s*\{\s*return this;\s*\}', item) is not None,
    'Item.randomRaw() 为 protected 默认实现（返回 this）')

# 8 个子类：各恰好 1 个 randomRaw，且全仓再无 public Item random()
for cls, rel in SUBCLASSES.items():
    s = read_j(rel)
    n = len(re.findall(r'protected Item randomRaw\(\)\s*\{', s))
    chk(n == 1, '%-38s protected Item randomRaw() 恰好 1 处（实得 %d）' % (rel, n))

stray = []
for dirpath, _d, files in os.walk(SRC):
    for fn in files:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            if 'public Item random()' in strip_comments(open(p, encoding='utf-8').read()):
                stray.append(os.path.relpath(p, SRC).replace(os.sep, '/'))
chk(not stray, '全仓无残留 `public Item random()` 覆写' + ('' if not stray else '：' + ','.join(stray)))

stray_super = []
for dirpath, _d, files in os.walk(SRC):
    for fn in files:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            if 'super.random()' in strip_comments(open(p, encoding='utf-8').read()):
                stray_super.append(os.path.relpath(p, SRC).replace(os.sep, '/'))
chk(not stray_super, '全仓无 `super.random()`（否则会重复夹取）'
    + ('' if not stray_super else '：' + ','.join(stray_super)))

# --- 两步判定的**顺序**（本次需求澄清的核心）---
i_neg = trials.find('if (item.cursed) lvl = -lvl;')
i_zero = trials.find('if (lvl > 0) lvl = 0;')
chk(i_neg != -1 and i_zero != -1, '两步判定源码都在（①反转 ②归零）')
chk(i_neg != -1 and i_zero != -1 and i_neg < i_zero,
    '顺序正确：先「因诅咒反转」→ 再「正等级归零」（位置 %d < %d）' % (i_neg, i_zero))

# 读的是原始存储等级，不是 Wand 那类带加成的 level()
chk('int lvl = item.trueLevel();' in trials,
    '读 `trueLevel()`（原始存储等级）而不是 `level()`（Wand 会算咒能灌注/树脂加成）')
chk('int lvl = item.level();' not in trials,
    '没有误用 `item.level()` 读等级')

# 三个钩子都以 Dungeon.isTrialled( BINAH ) 门控
for m in ('modifyGeneratedItem', 'flattenQuestReward', 'curseReverseLevel'):
    blk = trials[trials.find('public static Item %s(' % m):] if m == 'modifyGeneratedItem' \
        else trials[trials.find('public static void %s(' % m):]
    blk = blk[:blk.find('\n\t}')]
    chk('Dungeon.isTrialled( BINAH )' in blk, '%s 以 Dungeon.isTrialled( BINAH ) 门控' % m)
    chk('Random' not in blk, '%s 方法体不含 Random（随机流不变）' % m)
    chk('updateQuickslot' not in blk, '%s 不手工调 updateQuickslot（交给 level(int)）' % m)

# 只在等级真的变了才写回
chk('if (lvl != item.trueLevel()) item.level( lvl );' in trials,
    'modifyGeneratedItem 仅在等级变化时写回 level(int)')
chk('if (item.trueLevel() != 0) item.level( 0 );' in trials,
    'flattenQuestReward 仅在等级非 0 时写回')

# --- 四处任务 NPC：压平必须在自家加等级**之后**（比位置）---
for rel, pairs in QUESTS.items():
    s = read_j(rel)
    chk(('import %s.Trials;' % HEADER) in s, '%-38s 已 import Trials' % rel)
    for anchor, call, desc in pairs:
        pa, pc = s.find(anchor), s.find(call)
        chk(pa != -1, '%-38s 找到加等级处 `%s`' % (rel, anchor))
        chk(pc != -1, '%-38s 找到压平处 `%s`' % (rel, call))
        chk(pa != -1 and pc != -1 and pa < pc,
            '%-38s %s：压平在加等级之后（%d < %d）' % (rel, desc, pa, pc))

# --- Generator 的漏斗未被破坏、也没被重复夹取 ---
gen = read_j('items/Generator.java')
n_rand = len(re.findall(r'\.random\(\)', gen))
chk(n_rand >= 5, 'Generator 仍经 `.random()` 出道具（%d 处）⇒ 收口点自然生效' % n_rand)
chk('modifyGeneratedItem' not in gen and 'flattenQuestReward' not in gen,
    'Generator 内不重复调用 BINAH 钩子（唯一收口在 Item.random 内）')


# ================================================================ ② 编译产物层
print('\n== ② 编译产物层（javap -p） ==')
if not os.path.isdir(CLASSES):
    chk(False, '缺 %s —— 先按 skill 跑单文件 javac' % CLASSES)
else:
    item_sig = javap(['-p', '-cp', CLASSES, HEADER + '.items.Item'])
    chk(re.search(r'public final .*\.Item random\(\)', item_sig) is not None,
        'Item.random() 在 class 里确实是 final')
    chk(re.search(r'protected .*\.Item randomRaw\(\)', item_sig) is not None,
        'Item.randomRaw() 在 class 里确实是 protected')
    chk(re.search(r'public final int trueLevel\(\)', item_sig) is not None,
        'Item.trueLevel() 仍是 final')

    for cls, rel in SUBCLASSES.items():
        sig = javap(['-p', '-cp', CLASSES, HEADER + '.' + cls])
        has_raw = re.search(r'protected .*\.Item randomRaw\(\)', sig) is not None
        chk(has_raw, '%-38s class 里有 protected randomRaw()' % cls)

    tr_sig = javap(['-p', '-cp', CLASSES, HEADER + '.Trials'])
    chk(re.search(r'public static .*\.items\.Item modifyGeneratedItem\(.*\.items\.Item\)', tr_sig) is not None,
        'Trials.modifyGeneratedItem(Item) -> Item')
    chk(re.search(r'public static void flattenQuestReward\(.*\.items\.Item\)', tr_sig) is not None,
        'Trials.flattenQuestReward(Item) -> void')


# ================================================================ ③ 字节码层
print('\n== ③ 字节码层（javap -c） ==')
if not os.path.isdir(CLASSES):
    chk(False, '缺 %s —— 跳过字节码层' % CLASSES)
else:
    item_c = javap(['-p', '-c', '-cp', CLASSES, HEADER + '.items.Item'])
    body = method_body(item_c, 'public final com.shatteredpixel.shatteredpixeldungeon.items.Item random()')
    chk(body is not None, '取到 Item.random() 字节码')
    if body:
        txt = '\n'.join('%d: %s %s' % t for t in body)
        chk('randomRaw' in txt, 'Item.random() 先调 randomRaw()')
        chk('Trials.modifyGeneratedItem' in txt, 'Item.random() 再调 Trials.modifyGeneratedItem（唯一收口）')

    trials_c = javap(['-p', '-c', '-cp', CLASSES, HEADER + '.Trials'])
    for pref, label in (
            ('public static com.shatteredpixel.shatteredpixeldungeon.items.Item modifyGeneratedItem',
             'modifyGeneratedItem'),
            ('public static void flattenQuestReward', 'flattenQuestReward')):
        ins = method_body(trials_c, pref)
        chk(ins is not None, '取到 Trials.%s 字节码' % label)
        if not ins:
            continue
        txt = '\n'.join('%d: %s %s' % t for t in ins)
        chk('iconst_4' in txt and 'Dungeon.isTrialled' in txt,
            '%s 以 iconst_4（内联的 BINAH=4）+ Dungeon.isTrialled 门控' % label)
        chk('Item.level:(I)V' in txt, '%s 最终经 Item.level(int) 写回' % label)
        # RNG 不变量：字节码里不得出现 Random
        chk('watabou/utils/Random' not in txt,
            '%s 字节码不含 com/watabou/utils/Random ⇒ 随机流不变、关卡种子不漂移' % label)

    ins = method_body(trials_c, 'public static com.shatteredpixel.shatteredpixeldungeon.items.Item modifyGeneratedItem')
    if ins:
        got, why = negate_before_zero(ins)
        chk(got, 'modifyGeneratedItem 字节码里 `ineg`（反转）先于 `iconst_0`（归零）—— %s' % why)
        txt = '\n'.join('%d: %s %s' % t for t in ins)
        chk('Item.trueLevel:()I' in txt, 'modifyGeneratedItem 读 trueLevel()（而非 level()）')
        chk('Item.cursed:Z' in txt, 'modifyGeneratedItem 读 cursed 字段')
        chk('Item.level:(I)V' in txt, 'modifyGeneratedItem 经 level(int) 写回')


# ================================================================ ④ 备份差异层
# ⚠️ 2026-09-26 修：本层原本断言「与 BINAH 备份逐字节同长、且只有 1 行不同」。那只在
#    「BINAH 之后再没人动过这四个基类」时成立 —— 考验 YESOD 往 `Weapon/Armor/Ring/Wand`
#    各插了 7 行 YESOD 守卫块，于是「行数不变」当场假红（**是判据写死了，不是回归**）。
#    改成**集合差**式判据：备份里的**原行**只有那 1 行签名不见了（＝没有人改/删过既有方法体），
#    新增行一律**允许**（后加的功能自然会加行），只把增量打成 [INFO] 供人核对。
print('\n== ④ 备份差异层：8 个子类的**既有方法体**一字未动（只有 BINAH 那行签名变了） ==')
if not os.path.isdir(BAK):
    print('  [skip] 无 %s（备份已清理，跳过；不影响结论）' % BAK)
else:
    RENAME_OLD = 'public Item random() {'
    for cls, rel in SUBCLASSES.items():
        b = os.path.join(BAK, rel.replace('/', '__'))
        cur = os.path.join(SRC, *rel.split('/'))
        if not os.path.exists(b):
            print('  [skip] %s 无备份' % rel)
            continue
        old = readb(b).decode('utf-8').split('\n')
        new = readb(cur).decode('utf-8').split('\n')
        nset = set(new)
        # ⚠️ 两边都是**字节读**（行尾保留 `\r`）⇒ 比对一律先 `.strip()`，别拿裸行去比。
        missing = [l for l in old if l not in nset]     # 被删或被改动的「原行」
        chk([l.strip() for l in missing] == [RENAME_OLD],
            '%-38s 原文件里只有 `public Item random()` 那行消失 ⇒ 方法体逐字节未动（原 %d 行，实得缺失 %d 行）'
            % (rel, len(old), len(missing)))
        added = len(new) - len(old)
        print('  [INFO] %-38s 行数 %d → %d（Δ %+d，来自后续功能新增，非 BINAH）'
              % (rel, len(old), len(new), added))


# ================================================================ ⑤ 房间赠品层：祭坛 / 墓室
print('\n== ⑤ 房间赠品层：祭坛 / 墓室的「因诅咒取负」 ==')

# 这两个房间把诅咒补在生成**之后**（`prize.cursed = true` 写在 Generator.random() 之后），
# 所以规则② 在生成出口读到的 cursed 仍是 false；同时「未诅咒就白送一次 upgrade()」也在生成之后。
# ⇒ 必须在**事后 upgrade 之后**再补一次取负，且只处理正等级（负数是生成时就掷中诅咒、已算过的，原样保留）。
ROOMS = (
    'levels/rooms/special/SacrificeRoom.java',
    'levels/rooms/special/CryptRoom.java',
)
SEQS = {
    'levels/rooms/special/SacrificeRoom.java': [
        ('Generator.randomWeapon(', 'Generator 生成（走 Item.random 收口）'),
        ('if (!prize.cursed){', '建在生成之后的「未诅咒就白送 upgrade」分支'),
        ('prize.upgrade();', '事后 +1'),
        ('prize.cursed = prize.cursedKnown = true;', '事后强制诅咒'),
        ('Trials.curseReverseLevel( prize );', '补「因诅咒取负」'),
    ],
    'levels/rooms/special/CryptRoom.java': [
        ('Generator.randomArmor(', 'Generator 生成（走 Item.random 收口）'),
        ('if (!prize.cursed){', '建在生成之后的「未诅咒就白送 upgrade」分支'),
        ('prize.upgrade();', '事后 +1'),
        ('prize.cursed = prize.cursedKnown = true;', '事后强制诅咒'),
        ('Trials.curseReverseLevel( prize );', '补「因诅咒取负」'),
    ],
}
for rel in ROOMS:
    s = read_j(rel)
    chk(('import %s.Trials;' % HEADER) in s, '%-42s 已 import Trials' % rel)
    pos = []
    for anchor, desc in SEQS[rel]:
        p = s.find(anchor)
        if p == -1:
            chk(False, '%-42s 找不到 `%s`（%s）' % (rel, anchor, desc))
        pos.append(p)
    if -1 not in pos:
        chk(all(pos[i] < pos[i + 1] for i in range(len(pos) - 1)),
            '%-42s 顺序：生成 → 事后 upgrade → 强制诅咒 → 取负（%s）'
            % (rel, '<'.join(str(p) for p in pos)))
        tail = s[pos[-1]:]
        chk('upgrade(' not in tail and '.level(' not in tail,
            '%-42s 取负之后再无升级/改等级 ⇒ 不会被重新顶成正数' % rel)

# 「只对正等级取负」是这套逻辑的关键：无条件取负会把规则② 已算好的 −1 变回 +1
tcr = trials[trials.find('public static void curseReverseLevel('):]
tcr = tcr[:tcr.find('\n\t}')]
chk('int lvl = item.trueLevel();' in tcr, 'curseReverseLevel 读 trueLevel()（原始存储等级）')
chk('if (lvl > 0) item.level( -lvl );' in tcr,
    'curseReverseLevel 只对**正等级**取负（0 与负数原样保留，避免二次取负）')

# 调用点唯一性：除了定义处，只有这两个房间
users = []
for dirpath, _d, files in os.walk(SRC):
    for fn in files:
        if fn.endswith('.java'):
            p = os.path.join(dirpath, fn)
            if 'curseReverseLevel' in strip_comments(open(p, encoding='utf-8').read()):
                users.append(os.path.relpath(p, SRC).replace(os.sep, '/'))
chk(sorted(users) == ['Trials.java'] + sorted(ROOMS),
    'curseReverseLevel 调用点只有这两个房间（实得 %s）' % ','.join(sorted(users)))

# 三个「永不诅咒」房间（强制 cursed=false）刻意**不**接本钩子 —— 它们不是「必定诅咒」房间
for rel in ('levels/rooms/special/PoolRoom.java', 'levels/rooms/special/SentryRoom.java',
            'levels/rooms/special/TrapsRoom.java'):
    chk('curseReverseLevel' not in read_j(rel),
        '%-42s 强制 cursed=false，刻意不接取负钩子（已记档，范围外）' % rel)

# 字节码层：门控 + 只动正等级 + 不碰 Random
print('  -- curseReverseLevel 字节码 --')
if not os.path.isdir(CLASSES):
    chk(False, '缺 %s —— 跳过字节码层' % CLASSES)
else:
    ins = method_body(trials_c, 'public static void curseReverseLevel')
    chk(ins is not None, '取到 Trials.curseReverseLevel 字节码')
    if ins:
        txt = '\n'.join('%d: %s %s' % t for t in ins)
        chk('iconst_4' in txt and 'Dungeon.isTrialled' in txt,
            'curseReverseLevel 以 iconst_4（内联的 BINAH=4）+ Dungeon.isTrialled 门控')
        chk('Item.trueLevel:()I' in txt, 'curseReverseLevel 读 trueLevel()')
        chk('Item.level:(I)V' in txt, 'curseReverseLevel 经 level(int) 写回')
        chk('ineg' in txt, 'curseReverseLevel 字节码含 ineg（对等级取负）')
        chk(re.search(r'\bif\w+\b', txt) is not None,
            'curseReverseLevel 是「条件取负」而非无条件（有分支指令）')
        chk('watabou/utils/Random' not in txt,
            'curseReverseLevel 字节码不含 com/watabou/utils/Random ⇒ 随机流不变、关卡种子不漂移')


print()
print('=' * 78)
if ok:
    print('ALL PASS —— BINAH 生成时等级规则实现核验通过')
else:
    print('!! 存在 FAIL 项')
sys.exit(0 if ok else 1)
