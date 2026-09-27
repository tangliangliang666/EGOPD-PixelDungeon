# 核验考验「YESOD（根基）」与 NETZACH 淡出阈值的改动。
#
# 为什么需要它：YESOD 是一次「**跨三种机制**的收口」，每一条都能静默失败：
#   · 装备自然鉴定：4 处「数满即 identify()」的站点（Weapon.proc / Armor.proc /
#     Ring.onHeroGainExp / Wand.wandUsed）各加一个 else-if 分支。漏一处 = 那类装备照旧自鉴，
#     而且**不会报错**。
#   · 炼金分解：WndEnergizeItem.energize 是全作唯一的「分解成炼金能量」入口，能量照给、
#     只拦附带的鉴定（AlchemyScene 内表现为 showIdentify）。
#   · 地面显示：贴图（ItemSprite.view(Heap)）/ 名字（Heap.title）/ 描述（WndInfoItem(Heap)）
#     / 战利品指示器（LootIndicator，站在堆上时 HUD 那个图标）**四处都得读同一个
#     hidesGroundHeap**，否则会出现「图标是问号、名字却照旧」这类半吊子状态。
#     2026-09-26 用户追加：**六类容器**（宝箱/上锁宝箱/水晶宝箱/坟墓/骷髅堆/英雄遗骸）的贴图
#     与名字也一并变未知 —— 于是判据从「枚举 HEAP|FOR_SALE」改成「非空堆一律」，判据本体**不再
#     出现任何 Heap.Type**；这一条要被反向钉住（谁再把类型枚举加回来，容器就漏了）。
#     同一轮还加了第 4 个出口：LootIndicator 原本把脚下的堆映射成 CHEST/SKELETON 等图标，
#     等于绕开考验把「脚下是什么」重新写在屏幕上 —— 它现在改走 ItemSlot.UNKNOWN 虚拟物品。
#
# 反向判据同样重要 —— 本考验**故意**不碰这些东西，碰了就是 scope 越界：
#   · Item.identify()：全作鉴定总闸，连英雄初始装备（HeroClass.initHero）都走它 ⇒ 绝不能加闸门；
#   · 卷轴/药水的使用与投掷鉴定、卷轴拆分为符石时的鉴定（Scroll.ScrollToStone）；
#   · 鉴定卷轴 / 占卜卷轴 / 感知符石；
#   · 三个「直觉」天赋与神器装备时的自动鉴定（用户明确只禁四类装备）。
#   · ShopRoom.spacesNeeded()：它数 itemsToSpawn.size() 来定房间最小尺寸，而它在**房间尺寸
#     计算**阶段被调用 ⇒ 额外那张鉴定卷轴若写进 generateItems()，开关这个考验就会改变整层布局。
#
# 用法：
#   bash _chk/_build_yesod.sh probe     # 先编到 _chk/_javachk2（并刷 _chk/_javachk）
#   bash _chk/_build_yesod.sh run       # 跑行为探针 → _chk/_yesod.out
#   python _chk/verify_yesod.py
#   python _chk/verify_yesod.py --selftest
#
# 若有 PIL，本脚本还会顺带核对 items.png 上 xy(16,2) 的实际像素包围盒（10×15）；没有就 SKIP。

import os
import re
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CLASSES = os.path.join(HERE, '_javachk2')
JAVAP = 'D:/PD/tools/jdk-21.0.12.1+1/bin/javap'
PROBE_OUT = os.path.join(HERE, '_yesod.out')
# javap 的 classpath 必须**顺延到已编译的构建产物**：
#   1) `core/build/classes` 里才有 `items/Item.class`（本次它只是被隐式引用，没被显式编译进 _javachk2）
#      —— 少了它 `javap Item` 直接报「找不到类」，反向判据会静默变成 None ⇒ 假 FAIL；
#   2) `_javachk2` 放最前，保证本轮新编的类优先于旧产物。
JAVAP_CP = os.pathsep.join([
    CLASSES,
    'D:/PD/core/build/classes/java/main',
    'D:/PD/SPD-classes/build/classes/java/main',
    'D:/PD/services/build/classes/java/main',
])
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/misc')
ITEMS_PNG = os.path.join(ROOT, 'core/src/main/assets/sprites/items.png')

PKG = 'com.shatteredpixel.shatteredpixeldungeon'
TRIALS = PKG + '.Trials'
DUNGEON = PKG + '.Dungeon'
HEAP = PKG + '.items.Heap'
ITEMSPRITESHEET = PKG + '.sprites.ItemSpriteSheet'

# `Trials.naturalIdDisabled` 的**全部引用处**：4 个装备站点 + 炼金分解。多一处少一处都是回归。
# ⚠️ 这里**不含** `Trials.java`：callers_of 数的是「字面量 `Trials.naturalIdDisabled`」的**引用**，
#    而 `Trials.java` 里是**定义**（`public static boolean naturalIdDisabled(){`）与 javadoc
#    （`{@link #naturalIdDisabled()}`）—— 都不带 `Trials.` 前缀，天然落不进清单。别把它当成漏项补进去。
NATURAL_CALLERS = sorted([
    'items/armor/Armor.java',
    'items/rings/Ring.java',
    'items/wands/Wand.java',
    'items/weapon/Weapon.java',
    'windows/WndEnergizeItem.java',
])
# `Trials.hidesGroundHeap` 的**全部引用处**：贴图 + 名字 + 描述 + 战利品指示器
# （同样不含定义处 Trials.java）。
HIDES_CALLERS = sorted([
    'items/Heap.java',
    'sprites/ItemSprite.java',
    'ui/LootIndicator.java',
    'windows/WndInfoItem.java',
])
# 装备自然鉴定的 4 个站点：(文件, 该文件里「数满」判定的锚点)
ID_SITES = [
    ('items/weapon/Weapon.java', 'if (usesLeftToID <= 0)'),
    ('items/armor/Armor.java',   'if (usesLeftToID <= 0)'),
    ('items/wands/Wand.java',    'if (usesLeftToID <= 0 || Dungeon.hero.pointsInTalent'),
    ('items/rings/Ring.java',    'if (levelsToID <= 0)'),
]

ok = True


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def skip(msg):
    print('  [SKIP] ' + msg)


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


# ---------------------------------------------------------------- 通用工具
def strip_comments(src):
    """把注释换成等长空格（保留换行与偏移）。单趟状态机；正则会在 `//**` 处吞到文件尾。"""
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
    """截出方法体：从签名起到顶格一层缩进的收尾 `\\n\\t}` 为止。找不到返回 None。"""
    i = src.find(sig)
    if i == -1:
        return None
    j = src.find('\n\t}', i)
    if j == -1:
        return None
    return src[i:j]


def brace_body(src, start):
    """从 start 处按大括号配平截出其所属的 `{...}`，返回 (body, end)。"""
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
    """anchors 按期望先后给出；缺失锚点（-1）直接 False（不允许「缺项也算通过」）。"""
    pos = [s.find(a) for a in anchors]
    if any(p == -1 for p in pos):
        return False, pos
    return all(pos[i] < pos[i + 1] for i in range(len(pos) - 1)), pos


def const_of(src, name):
    m = re.search(r'public static final \w+ ' + re.escape(name) + r'\s*=\s*([^;]+);', src)
    return m.group(1).strip() if m else None


def mask_value(src, name):
    """把 `public static final int NAME = <字面量>;` 求成 int。

    故意**不用 eval**、也故意只认三种形态（十进制 / 十六进制 / `1 << n`）：本仓考验掩码
    就是 `1 << n` 与直给两种写法，多一种形态就该有人来改判据，而不是让脚本悄悄算错。
    """
    raw = (const_of(src, name) or '').replace('_', '').replace(' ', '')
    m = re.fullmatch(r'1<<(\d+)', raw)
    if m:
        return 1 << int(m.group(1))
    if re.fullmatch(r'0[xX][0-9a-fA-F]+', raw):
        return int(raw, 16)
    if re.fullmatch(r'\d+', raw):
        return int(raw)
    return None


def has_int_const(ins, val):
    """指令流里有没有一条「把整数 val 压栈」的指令。

    ⚠️ 这是本考验最容易写错的一条：`Trials.YESOD` 是**编译期常量**（= 256），
    javac 会把掩码**内联**进字节码（`sipush 256`），所以**没有** `getstatic Trials.YESOD`。
    判据必须落在「内联值 == 源码读出来的值」上，而不是去找字段引用。
    """
    for _o, op, rest in (ins or []):
        s = rest.strip()
        if op in ('sipush', 'bipush') and s == str(val):
            return True
        if op in ('ldc', 'ldc_w', 'ldc2_w') and re.match(r'#\d+\s+//\s+int\s+%d\s*$' % val, s):
            return True
        if op == 'iconst_%d' % val:  # val ∈ 0..5
            return True
    return False


def callers_of(token):
    hits = []
    for dirpath, _d, files in os.walk(SRC):
        for fn in files:
            if fn.endswith('.java'):
                p = os.path.join(dirpath, fn)
                if token in open(p, encoding='utf-8', errors='replace').read():
                    hits.append(os.path.relpath(p, SRC).replace('\\', '/'))
    return sorted(set(hits))


def javap(args):
    r = subprocess.run([JAVAP] + args, capture_output=True, text=True, encoding='utf-8',
                       errors='replace')
    return r.stdout + r.stderr


def method_body(disasm, header_prefix):
    """从 javap -c 输出截出某方法体的指令列表 [(offset, opcode, rest)]。"""
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
        m = re.match(r'^(\d+):\s+(\S+)(.*)$', s)
        if m:
            ins.append((int(m.group(1)), m.group(2), m.group(3)))
    return ins


def invokes(ins, needle):
    return [off for (off, _op, rest) in (ins or []) if needle in rest]


def first_invoke(ins, needle):
    got = invokes(ins, needle)
    return got[0] if got else -1


def after_offset(ins, gate, token):
    """token 的**首个**引用是否落在偏移 gate 之后。

    缺项（指令流里根本没有 token）⇒ False —— 「缺项也算通过」是这类顺序判据最常见的假绿来源，
    所以这里显式要求「必须出现，且在 gate 之后」。layer 与 selftest 共用同一份实现。
    """
    hits = invokes(ins, token)
    return bool(hits) and hits[0] > gate


# ---------------------------------------------------------------- 反例自测
def selftest():
    print('== selftest：确认「比位置」判据不恒真 ==')
    good, _ = seq_ok('AAA_BBB_CCC', ['AAA', 'BBB', 'CCC'])
    chk(good, '正例：AAA→BBB→CCC 判为真')
    bad, _ = seq_ok('AAA_CCC_BBB', ['AAA', 'BBB', 'CCC'])
    chk(not bad, '反例：BBB/CCC 互换 ⇒ 判为假（抓得住顺序颠倒）')
    miss, pos = seq_ok('AAA_CCC', ['AAA', 'BBB', 'CCC'])
    chk(not miss and -1 in pos, '反例：缺锚点 ⇒ 判为假（不允许缺项通过）')

    print('== selftest：确认 strip_comments 保偏移、不吞后续 ==')
    s = 'int a = 1; // 注释 //** 里\nint b = 2; /* 块 */ int c = 3;'
    st = strip_comments(s)
    chk(len(st) == len(s), '剥注释后长度不变（保偏移）')
    chk('int b = 2;' in st and 'int c = 3;' in st, '`//**` 不会吞到文件末尾')
    chk('\n' in st, '换行保留')

    print('== selftest：确认 brace_body 截的是**所属**那个块 ==')
    code = 'x = 1; else if (Trials.naturalIdDisabled()){\n\tif (a) { p(); }\n\tsetIDReady();\n} else {\n\tidentify();\n}'
    body, _ = brace_body(code, code.index('Trials.naturalIdDisabled()'))
    chk('setIDReady()' in body, '截出的块含 setIDReady()')
    chk('identify()' not in body, '截出的块**不含** identify()（else 分支被排除在外）')

    print('== selftest：确认常量断言真的读源码、不是写死 ==')
    t = read_j('Trials.java')
    chk(const_of(t, 'NETZACH_FADE_NEAR') == '1', '实读 NETZACH_FADE_NEAR = 1')
    fake = re.sub(r'(public static final int NETZACH_FADE_NEAR\s*=\s*)1', r'\g<1>9', t, count=1)
    chk(fake != t, '反例样本确实被改过（否则下面这条无意义）')
    chk(const_of(fake, 'NETZACH_FADE_NEAR') == '9', '反例：改成 9 ⇒ 断言随之读到 9（非写死）')

    print('== selftest：确认调用点清单不是恒真 ==')
    chk(callers_of('Trials.hidesGroundHeap') == HIDES_CALLERS,
        '实仓 hidesGroundHeap 引用清单与预期一致（%s）' % HIDES_CALLERS)
    # callers_of 数的是**引用**、不是**定义** ⇒ 定义处天然不在清单里。
    # 这条反向自测钉住这个语义：谁要是"顺手"把 Trials.java 加回清单，这里立刻炸。
    chk('Trials.java' not in callers_of('Trials.naturalIdDisabled'),
        'callers_of 不含定义处 Trials.java（它按引用数，不按定义数）')
    chk(callers_of('Trials.naturalIdDisabled') != NATURAL_CALLERS + ['Trials.java'],
        '反例：把 Trials.java 硬塞进清单 ⇒ 判为不等（清单不是随便写都过）')

    print('== selftest：掩码常量内联判据 ==')
    chk(mask_value('public static final int YESOD = 256;\t//根基', 'YESOD') == 256,
        'mask_value 读十进制 = 256')
    chk(mask_value('public static final int HOD = 1 << 7;', 'HOD') == 128,
        'mask_value 读 `1 << 7` = 128')
    chk(mask_value('public static final int WEIRD = SOME_OTHER_CONST;', 'WEIRD') is None,
        '反例：非字面量 ⇒ None（不硬猜，逼人来改判据）')
    real_ins = [(0, 'sipush', '       256'),
                (3, 'invokestatic', '  #13                 // Method Dungeon.isTrialled:(I)Z'),
                (6, 'ireturn', '')]
    chk(has_int_const(real_ins, 256), 'has_int_const：认得 `sipush 256`（常量被内联）')
    chk(not has_int_const(real_ins, 128), '反例：掩码对不上 ⇒ 判为假')
    chk(not has_int_const([], 256), '反例：空指令流 ⇒ 判为假（不是恒真）')

    print('== selftest：认识「identify_ready 字符串常量」这个陷阱 ==')
    trap = [(624, 'ldc', '            #199                // String identify_ready'),
            (654, 'invokestatic', '  #220  // Method .../Trials.naturalIdDisabled:()Z'),
            (703, 'invokevirtual', '  #227  // Method identify:()Lcom/.../items/Item;')]
    chk(first_invoke(trap, 'identify') == 624,
        '松判据 `identify` 会先撞上 624 行（String identify_ready）—— 这就是原判据判反的根因')
    chk(first_invoke(trap, 'identify:()') == 703,
        '紧判据 `identify:()` 落在真正的调用 703 行 ⇒ 顺序判据成立')

    print('== selftest：确认「闸门罩住容器」的判据不恒真（layer 与 selftest 共用 after_offset）==')
    # 样本必须**保持文件顺序**（method_body 返回的就是递增偏移），否则反例只是「把数组倒过来」
    # 而不是「真实的错误写法」，判据就成了假反例。
    def ins(off, op, rest):
        return (off, op, rest)

    gate_at = 6
    fwd = [ins(0, 'invokestatic', '  // Method .../Trials.hidesGroundHeap:(L...Heap;)Z'),
           ins(3, 'getstatic', '      // Field .../ItemSpriteSheet.CHEST:I'),
           ins(6, 'getstatic', '      // Field .../ItemSpriteSheet.BONES:I')]
    fwd_gate = 0
    conts = ['ItemSpriteSheet.CHEST', 'ItemSpriteSheet.BONES']
    chk(all(after_offset(fwd, fwd_gate, c) for c in conts),
        '正例：闸门排在最前 ⇒ 容器判据全过')
    rev = [ins(0, 'getstatic', '      // Field .../ItemSpriteSheet.CHEST:I'),
           ins(3, 'getstatic', '      // Field .../ItemSpriteSheet.BONES:I'),
           ins(gate_at, 'invokestatic', '  // Method .../Trials.hidesGroundHeap:(L...Heap;)Z')]
    chk(not all(after_offset(rev, gate_at, c) for c in conts),
        '反例：闸门被挪到容器取值之后 ⇒ 同一判据判为假（抓得住「闸门被绕过」）')
    chk(not after_offset(fwd, fwd_gate, 'ItemSpriteSheet.TOMB'),
        '反例：指令流里没有该容器 ⇒ 判为假（缺项不算通过）')
    chk(not after_offset([], fwd_gate, 'ItemSpriteSheet.CHEST'),
        '反例：空指令流 ⇒ 判为假（不是恒真）')

    print('\nselftest 结束（上面每一条都应是 [OK]）。\n')


# ---------------------------------------------------------------- ① 源码结构层
def layer_source():
    print('== ① 源码结构层 ==')
    trials = read_j('Trials.java')

    # --- NETZACH 阈值 ---
    chk(const_of(trials, 'NETZACH_FADE_NEAR') == '1', 'NETZACH_FADE_NEAR = 1（1 格之内完全不透明）')
    chk(const_of(trials, 'NETZACH_FADE_FAR') == '3', 'NETZACH_FADE_FAR = 3（3 格之外完全不可见）')
    chk(const_of(trials, 'NETZACH_FADE_ALPHA') == '0.5f', 'NETZACH_FADE_ALPHA = 0.5f（中间带半透明）')
    fade = method_span(trials, 'public static float enemyFade( Char ch ){')
    chk(fade is not None, '找得到 enemyFade()')
    if fade:
        seq, pos = seq_ok(fade, ['<= NETZACH_FADE_NEAR', '<= NETZACH_FADE_FAR',
                                 'NETZACH_FADE_ALPHA', 'return 0f'])
        chk(seq, 'enemyFade 阈值顺序：≤NEAR ⇒ 1.0 / ≤FAR ⇒ ALPHA / 其余 ⇒ 0（pos=%s）' % pos)

    # --- YESOD 三个收口方法 ---
    nat = method_span(trials, 'public static boolean naturalIdDisabled(){')
    chk(nat is not None, '找得到 naturalIdDisabled()')
    chk(nat is not None and 'Dungeon.isTrialled( YESOD )' in nat,
        'naturalIdDisabled() 只读 Dungeon.isTrialled( YESOD )')

    hides = method_span(trials, 'public static boolean hidesGroundHeap( Heap heap ){')
    chk(hides is not None, '找得到 hidesGroundHeap()')
    if hides:
        seq, pos = seq_ok(hides, ['heap == null', 'heap.isEmpty()', 'Dungeon.isTrialled( YESOD )'])
        chk(seq, 'hidesGroundHeap 判定顺序：null / 空堆 / 考验开关（pos=%s）' % pos)
        # 判据本体必须**不含**类型枚举 —— 一旦有人加回 `type == Heap.Type.CHEST` 这类白名单，
        # 没被列进去的容器就会照旧显形，而且是静默的（不报错、不崩、只是答案写在脸上）。
        chk('Heap.Type' not in hides and 'type ==' not in hides and 'switch' not in hides,
            'hidesGroundHeap 里没有任何 Heap.Type 枚举（非空堆一律隐藏 ⇒ 8 种类型无一遗漏）')
        raw_t = read('Trials.java')
        i = raw_t.index('hidesGroundHeap( Heap heap ){')
        javadoc = raw_t[max(0, i - 1500):i]
        for t in ['CHEST', 'LOCKED_CHEST', 'CRYSTAL_CHEST', 'TOMB', 'SKELETON', 'REMAINS']:
            chk(t in javadoc, 'javadoc 记下了容器类型 %s（口径写清楚，下次改才不用猜）' % t)

    unk = method_span(trials, 'public static String unknownGroundText(){')
    chk(unk is not None and 'Messages.get( Trials.class, "yesod_unknown" )' in (unk or ''),
        'unknownGroundText() 取的是 trials.yesod_unknown（名字与描述共用同一条）')

    # --- 贴图常量 ---
    sheet = read_j('sprites/ItemSpriteSheet.java')
    chk(const_of(sheet, 'UNKNOWN_ITEM') == 'xy(16, 2)',
        'UNKNOWN_ITEM = xy(16, 2)（用户给定的格位）')
    chk('assignItemRect(UNKNOWN_ITEM,    10, 15);' in sheet,
        'UNKNOWN_ITEM 的取样矩形 = 10×15（用户给定尺寸）')

    # --- 贴图出口 ---
    sprite = read_j('sprites/ItemSprite.java')
    vh = method_span(sprite, 'public ItemSprite view( Heap heap ){')
    chk(vh is not None, '找得到 ItemSprite.view(Heap)')
    if vh:
        # 关键：闸门必须**罩住整个 switch**。六类容器的贴图只在 switch 里被设置，
        # 所以「switch 排在 hidesGroundHeap 判定之后、六个容器分支又都在 switch 之后」
        # ＝ 容器被覆盖；只要有人把某个 case 提到闸门之前，这条顺序判据立刻炸。
        seq, pos = seq_ok(vh, ['Trials.hidesGroundHeap( heap )', 'ItemSpriteSheet.UNKNOWN_ITEM',
                               'switch (heap.type)',
                               'case CHEST:', 'case LOCKED_CHEST:', 'case CRYSTAL_CHEST:',
                               'case TOMB:', 'case SKELETON:', 'case REMAINS:'])
        chk(seq, 'view(Heap)：闸门在前，六个容器分支全在其**之后**（pos=%s）' % pos)
        chk(vh.count('ItemSpriteSheet.UNKNOWN_ITEM') == 1,
            'view(Heap) 里只有一条问号出口（不会出现两条路径各写一份、日后改漏一处）')
        chk('viewItem = null;' in vh.split('switch')[0],
            'view(Heap) 仍在 switch 之前把 viewItem 置 null（问号图标不会误触发附魔流光）')

    # --- 名字出口（比位置：必须在 switch(type) 之前）---
    heap = read_j('items/Heap.java')
    seq, pos = seq_ok(heap, ['public String title(){',
                             'type != Type.FOR_SALE && Trials.hidesGroundHeap( this )',
                             'return Trials.unknownGroundText();',
                             'switch(type){'])
    chk(seq, 'Heap.title()：隐名分支排在 switch(type) **之前**，且只放行 FOR_SALE 价签（pos=%s）' % pos)

    # --- 描述出口（比位置：必须排在 type == HEAP 的旧分支之前）---
    wnd = read_j('windows/WndInfoItem.java')
    ctor = method_span(wnd, 'public WndInfoItem( Heap heap ) {')
    chk(ctor is not None, '找得到 WndInfoItem(Heap)')
    if ctor:
        seq, pos = seq_ok(ctor, ['Trials.hidesGroundHeap( heap )', 'Trials.unknownGroundText()',
                                 'heap.type == Heap.Type.HEAP'])
        chk(seq, 'WndInfoItem(Heap)：未知分支排在「普通堆取 peek()」之前（pos=%s）' % pos)

    # --- 第 4 个出口：战利品指示器（站在堆上时 HUD 显示的那个图标）---
    slot = read_j('ui/ItemSlot.java')
    chk('public static final Item UNKNOWN = new Item()' in slot,
        'ItemSlot 新增 UNKNOWN 虚拟物品（与既有 CHEST/SKELETON 同一套写法）')
    ub, _ = brace_body(slot, slot.index('public static final Item UNKNOWN'))
    chk('ItemSpriteSheet.UNKNOWN_ITEM' in ub,
        'ItemSlot.UNKNOWN.image() 取的是 UNKNOWN_ITEM（与地图上是同一格问号）')
    chk('Trials.unknownGroundText()' in ub,
        'ItemSlot.UNKNOWN.name() 取的是同一条未知文本（口径不漂移）')

    loot = read_j('ui/LootIndicator.java')
    lu = method_span(loot, 'public void update() {')
    chk(lu is not None, '找得到 LootIndicator.update()')
    if lu:
        seq, pos = seq_ok(lu, ['Trials.hidesGroundHeap( heap )', 'ItemSlot.UNKNOWN',
                               'heap.type == Heap.Type.CHEST ? ItemSlot.CHEST',
                               'heap.peek()'])
        chk(seq, 'LootIndicator：问号分支排在「按类型挑图标」之前（pos=%s）' % pos)
        chk(lu.count('Trials.hidesGroundHeap') == 1, 'LootIndicator 每帧只判一次（不重复取数）')

    # --- 4 个装备站点 ---
    for rel, anchor in ID_SITES:
        s = read_j(rel)
        n = s.count('Trials.naturalIdDisabled()')
        chk(n == 1, '%s 里 naturalIdDisabled() 恰好 1 次（实测 %d）' % (rel, n))
        if n:
            body, _ = brace_body(s, s.index('Trials.naturalIdDisabled()'))
            chk('setIDReady()' in body, '%s：守卫分支里 setIDReady()（数满后只标记、不鉴定）' % rel)
            chk('identify()' not in body, '%s：守卫分支里**没有** identify()' % rel)
        region = s[s.index(anchor):s.index(anchor) + 900] if anchor in s else ''
        seq, pos = seq_ok(region, ['ShardOfOblivion.passiveIDDisabled()',
                                   'Trials.naturalIdDisabled()', 'identify();'])
        chk(seq, '%s：碎片判定 → YESOD 判定 → identify() 的先后正确（pos=%s）' % (rel, pos))

    # --- 炼金分解 ---
    energize = read_j('windows/WndEnergizeItem.java')
    eb = method_span(energize, 'private static void energize(Item item){')
    chk(eb is not None, '找得到 WndEnergizeItem.energize()')
    if eb:
        chk(eb.count('Trials.naturalIdDisabled()') == 2,
            'energize() 里 2 处守卫（showIdentify / item.identify() 各一）')
        seq, pos = seq_ok(eb, ['!item.isIdentified() && !Trials.naturalIdDisabled()', 'showIdentify',
                               'if (!Trials.naturalIdDisabled()){', 'item.identify();'])
        chk(seq, 'energize()：两处守卫各自罩住 showIdentify 与 item.identify()（pos=%s）' % pos)
        seq2, pos2 = seq_ok(eb, ['Dungeon.energy += item.energyVal();', 'Trials.naturalIdDisabled()'])
        chk(seq2, 'energize()：能量照给（加能量在前、拦鉴定在后）（pos=%s）' % pos2)

    # --- 商店额外鉴定卷轴 ---
    shop = read_j('levels/rooms/special/ShopRoom.java')
    pi = method_span(shop, 'protected void placeItems( Level level ){')
    sp = method_span(shop, 'public int spacesNeeded(){')
    gi = method_span(shop, 'protected static ArrayList<Item> generateItems() {')
    chk(pi is not None and 'Dungeon.isTrialled( Trials.YESOD )' in pi and 'new ScrollOfIdentify()' in pi,
        'ShopRoom.placeItems：YESOD 时补一张 ScrollOfIdentify')
    chk(sp is not None and 'YESOD' not in sp,
        'ShopRoom.spacesNeeded：**不**含 YESOD 分支（否则开关考验会改变房间尺寸 → 改变整层布局）')
    chk(gi is not None and 'YESOD' not in gi and gi.count('new ScrollOfIdentify()') == 1,
        'generateItems 里没有 YESOD 分支、且原本那张鉴定卷轴仍在（没被搬走）')

    # --- 反向判据：不该碰的地方 ---
    item = read_j('items/Item.java')
    ib, _ = brace_body(item, item.index('public Item identify( boolean byHero )'))
    chk('Trials' not in ib,
        '反向：Item.identify(boolean) 里**没有** Trials —— 总闸会把英雄初始装备一起拦掉')
    talent = read_j('actors/hero/Talent.java')
    tbody = method_span(talent, 'public static void onTalentUpgraded( Hero hero, Talent talent ){')
    chk(tbody is not None and 'naturalIdDisabled' not in tbody,
        '反向：三个「直觉」天赋仍照常自动鉴定（用户定：只禁四类装备）')
    chk('naturalIdDisabled' not in read_j('items/artifacts/Artifact.java'),
        '反向：神器「装备即鉴定」保留')
    chk('naturalIdDisabled' not in read_j('items/scrolls/Scroll.java'),
        '反向：卷轴拆符石（ScrollToStone）与卷轴使用鉴定不受影响')
    chk('naturalIdDisabled' not in read_j('items/stones/StoneOfIntuition.java'),
        '反向：感知符石不受影响')
    chk('naturalIdDisabled' not in read_j('items/scrolls/ScrollOfIdentify.java'),
        '反向：鉴定卷轴不受影响')

    # --- 调用点唯一性 ---
    chk(callers_of('Trials.naturalIdDisabled') == NATURAL_CALLERS,
        'naturalIdDisabled 的引用清单恰为「4 个装备 + 炼金分解」（实测 %s）'
        % callers_of('Trials.naturalIdDisabled'))
    chk(callers_of('Trials.hidesGroundHeap') == HIDES_CALLERS,
        'hidesGroundHeap 的引用清单恰为「贴图 + 名字 + 描述 + 战利品指示器」（实测 %s）'
        % callers_of('Trials.hidesGroundHeap'))
    chk(callers_of('ItemSpriteSheet.UNKNOWN_ITEM') == ['sprites/ItemSprite.java', 'ui/ItemSlot.java'],
        'UNKNOWN_ITEM 只被两处问号出口引用（地图贴图 + 战利品指示器）（实测 %s）'
        % callers_of('ItemSpriteSheet.UNKNOWN_ITEM'))


# ---------------------------------------------------------------- ② javap 签名层
def layer_signatures():
    print('\n== ② javap 签名层 ==')
    t = javap(['-p', '-cp', JAVAP_CP, TRIALS])
    for sig in ['public static boolean naturalIdDisabled();',
                'public static boolean hidesGroundHeap(' + HEAP + ');',
                'public static java.lang.String unknownGroundText();',
                'public static final int NETZACH_FADE_NEAR;',
                'public static final int NETZACH_FADE_FAR;']:
        chk(sig in t, 'Trials 暴露 ' + sig)
    s = javap(['-p', '-cp', JAVAP_CP, ITEMSPRITESHEET])
    chk('public static final int UNKNOWN_ITEM;' in s, 'ItemSpriteSheet 暴露 UNKNOWN_ITEM')
    # ⚠️ UNKNOWN_ITEM 由私有方法 xy() 现算 ⇒ **不是**编译期常量，javap -constants 不给值；
    #    它的实际取值改由探针（①）与 <clinit> 字节码（③）两个方向钉住。
    sl = javap(['-p', '-cp', JAVAP_CP, PKG + '.ui.ItemSlot'])
    chk('public static final ' + PKG + '.items.Item UNKNOWN;' in sl,
        'ItemSlot 暴露 UNKNOWN 虚拟物品（战利品指示器的问号出口）')


# ---------------------------------------------------------------- ③ 字节码层
def layer_bytecode():
    print('\n== ③ javap -c 字节码层 ==')

    t = javap(['-c', '-p', '-cp', JAVAP_CP, TRIALS])
    yesod = mask_value(read_j('Trials.java'), 'YESOD')
    b = method_body(t, 'public static boolean naturalIdDisabled();')
    chk(b is not None and first_invoke(b, 'Dungeon.isTrialled') != -1,
        'Trials.naturalIdDisabled 字节码：转调 Dungeon.isTrialled')
    chk(b is not None and yesod is not None and has_int_const(b, yesod),
        'Trials.naturalIdDisabled 字节码：掩码内联为 %s（= 源码读出的 YESOD）' % yesod)
    chk(b is not None and [op for _o, op, _r in b] == ['sipush', 'invokestatic', 'ireturn'],
        'Trials.naturalIdDisabled 字节码：只余「掩码 → isTrialled → ireturn」三句（没有取反）')
    b2 = method_body(t, 'public static boolean hidesGroundHeap(')
    chk(b2 is not None and first_invoke(b2, 'Heap.isEmpty') != -1 and
        first_invoke(b2, 'Dungeon.isTrialled') != -1,
        'Trials.hidesGroundHeap 字节码：Heap.isEmpty + Dungeon.isTrialled')
    # 判据本体不许再出现任何 Heap$Type —— 这正是「容器也变问号」的实现方式，
    # 也是它的护栏：谁把类型枚举加回来，容器就会重新显形且不报错。
    chk(b2 is not None and not any('Heap$Type' in r for _o, _p, r in b2),
        'Trials.hidesGroundHeap 字节码：**不再比对任何 Heap$Type**（非空堆一律隐藏）')

    sheet = javap(['-c', '-p', '-cp', JAVAP_CP, ITEMSPRITESHEET])
    cl = method_body(sheet, 'static {};') or method_body(sheet, 'public ' + ITEMSPRITESHEET + '();')
    # <clinit> 在 javap 里写作 `static {};`
    if cl is None:
        cl = [i for i in method_body(sheet, 'static {') or []]
    chk(cl is not None and first_invoke(cl, 'assignItemRect') != -1,
        'ItemSpriteSheet.<clinit> 里调了 assignItemRect（UNKNOWN_ITEM 的矩形在这里定）')
    chk('putstatic' in sheet and 'UNKNOWN_ITEM' in sheet,
        'ItemSpriteSheet.<clinit> 写了 UNKNOWN_ITEM')

    sprite = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.sprites.ItemSprite'])
    vh = method_body(sprite, 'public ' + PKG + '.sprites.ItemSprite view(' + HEAP + ');')
    chk(vh is not None and first_invoke(vh, 'Trials.hidesGroundHeap') != -1 and
        first_invoke(vh, 'ItemSpriteSheet.UNKNOWN_ITEM') != -1,
        'ItemSprite.view(Heap) 字节码：调 hidesGroundHeap 且用到 UNKNOWN_ITEM')
    if vh:
        gate = first_invoke(vh, 'Trials.hidesGroundHeap')
        chk(first_invoke(vh, 'ItemSpriteSheet.UNKNOWN_ITEM') > gate,
            'view(Heap) 字节码：先判 hidesGroundHeap，再取问号格（顺序反了等于没拦）')
        # 「容器被覆盖」的字节码级证据：六个容器贴图的 getstatic 必须都在闸门之后。
        conts = ['ItemSpriteSheet.CHEST', 'ItemSpriteSheet.LOCKED_CHEST',
                 'ItemSpriteSheet.CRYSTAL_CHEST', 'ItemSpriteSheet.TOMB',
                 'ItemSpriteSheet.BONES', 'ItemSpriteSheet.REMAINS']
        late = [c.split('.')[-1] for c in conts if not after_offset(vh, gate, c)]
        chk(not late,
            'view(Heap) 字节码：六个容器贴图全在闸门之后（漏在闸门前的：%s）' % (late or '无'))

    heap = javap(['-c', '-p', '-cp', JAVAP_CP, HEAP])
    hi = method_body(heap, 'public java.lang.String title();')
    chk(hi is not None and first_invoke(hi, 'Trials.hidesGroundHeap') != -1 and
        first_invoke(hi, 'Trials.unknownGroundText') != -1,
        'Heap.title() 字节码：调 hidesGroundHeap 与 unknownGroundText')
    chk(hi is not None and first_invoke(hi, 'Heap$Type.FOR_SALE') != -1 and
        first_invoke(hi, 'Heap$Type.HEAP') == -1,
        'Heap.title() 字节码：只放行 Heap$Type.FOR_SALE（容器与散落物一律隐名，不再是「只拦 HEAP」）')

    wnd = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.windows.WndInfoItem'])
    ctor = method_body(wnd, 'public ' + PKG + '.windows.WndInfoItem(' + HEAP + ');')
    chk(ctor is not None and first_invoke(ctor, 'Trials.hidesGroundHeap') != -1 and
        first_invoke(ctor, 'Trials.unknownGroundText') != -1,
        'WndInfoItem(Heap) 字节码：调 hidesGroundHeap 与 unknownGroundText')

    # --- 第 4 个出口：战利品指示器（站在堆上时 HUD 的那个图标）---
    # 它原本是一串 `heap.type == CHEST ? ItemSlot.CHEST : …` 的三元链，等于把「脚下是什么」
    # 重新写在屏幕上 ⇒ 闸门必须排在这条链**最前**。⚠️ 这里的 `invokes` 只按操作数文本匹配，
    # getstatic（取 ItemSlot.UNKNOWN）也会被它抓到 —— 正是我们要的。
    loot = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.ui.LootIndicator'])
    lu = method_body(loot, 'public void update();')
    chk(lu is not None, '取到 LootIndicator.update() 的字节码')
    if lu:
        g = first_invoke(lu, 'Trials.hidesGroundHeap')
        chk(g != -1, 'LootIndicator.update 字节码：调 Trials.hidesGroundHeap')

        chk(after_offset(lu, g, 'ItemSlot.UNKNOWN'), 'LootIndicator：问号取值排在闸门之后')
        for tok, label in [('ItemSlot.CHEST', '宝箱'), ('ItemSlot.LOCKED_CHEST', '上锁宝箱'),
                           ('ItemSlot.CRYSTAL_CHEST', '水晶宝箱'), ('ItemSlot.TOMB', '坟墓'),
                           ('ItemSlot.SKELETON', '骷髅堆'), ('ItemSlot.REMAINS', '英雄遗骸')]:
            chk(after_offset(lu, g, tok),
                'LootIndicator：%s 图标也在闸门之后（脚下是它时照样变问号）' % label)

    # --- ItemSlot.UNKNOWN 的字节码本体 ---
    sl_c = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.ui.ItemSlot'])
    clinit = method_body(sl_c, 'static {};')
    chk(clinit is not None and any(op == 'putstatic' and 'UNKNOWN' in r
                                   for _o, op, r in clinit),
        'ItemSlot.<clinit> 里 putstatic 了 UNKNOWN（虚拟物品真的被建出来）')
    anon_dir = os.path.join(CLASSES, *PKG.split('.'), 'ui')
    anon = []
    if os.path.isdir(anon_dir):
        for fn in sorted(os.listdir(anon_dir)):
            if fn.startswith('ItemSlot$') and fn.endswith('.class'):
                nm = PKG + '.ui.ItemSlot$' + fn[len('ItemSlot$'):-len('.class')]
                d = javap(['-c', '-p', '-cp', JAVAP_CP, nm])
                if 'ItemSpriteSheet.UNKNOWN_ITEM' in d:
                    anon.append((fn, d))
    chk(len(anon) == 1,
        '恰有 1 个 ItemSlot$ 匿名子类取 UNKNOWN_ITEM（实测 %d 个 —— 多于 1 说明问号图标被复制成了两份）'
        % len(anon))
    for fn, d in anon:
        img = method_body(d, 'public int image();')
        nam = method_body(d, 'public java.lang.String name();')
        chk(img is not None and first_invoke(img, 'ItemSpriteSheet.UNKNOWN_ITEM') != -1,
            '%s.image() 返回 UNKNOWN_ITEM（与地图同一个格位）' % fn)
        chk(nam is not None and first_invoke(nam, 'Trials.unknownGroundText') != -1,
            '%s.name() 返回 Trials.unknownGroundText()（与地图同一个文本源）' % fn)

    # 4 个装备站点：必须调 naturalIdDisabled，且 Item.identify 的调用点在其**之后**
    # ⚠️ 护甲那处不在 `absorb()` 里 —— 原版把「数满即鉴定」写在 `Armor.proc()`（覆写 Item.proc），
    #    写判据时别按直觉找 absorb 签名（`javap Armor` 里根本没有 absorb 方法）。
    for rel, cls, prefix in [
        ('items/weapon/Weapon.java', PKG + '.items.weapon.Weapon', 'public int proc('),
        ('items/armor/Armor.java',   PKG + '.items.armor.Armor',   'public int proc('),
        ('items/rings/Ring.java',    PKG + '.items.rings.Ring',    'public void onHeroGainExp('),
        ('items/wands/Wand.java',    PKG + '.items.wands.Wand',    'public void wandUsed();'),
    ]:
        d = javap(['-c', '-p', '-cp', JAVAP_CP, cls])
        body = method_body(d, prefix)
        nat = first_invoke(body, 'Trials.naturalIdDisabled')
        # 注意判据里带冒号：`first_invoke(body, 'identify')` 会先撞上前面那句
        # `ldc // String identify_ready`（是**字符串常量**不是调用），从而把顺序判反。
        ident = first_invoke(body, 'identify:()')
        chk(nat != -1, '%s 的字节码里调了 Trials.naturalIdDisabled（%s）' % (rel, prefix))
        chk(nat != -1 and ident != -1 and nat < ident,
            '%s：naturalIdDisabled 的取数早于 identify()（%s；否则守卫形同虚设）' % (rel, prefix))

    en = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.windows.WndEnergizeItem'])
    eb = method_body(en, 'private static void energize(')
    chk(eb is not None and len(invokes(eb, 'Trials.naturalIdDisabled')) == 2,
        'WndEnergizeItem.energize 字节码：2 处 Trials.naturalIdDisabled 守卫')
    chk(eb is not None and first_invoke(eb, 'showIdentify') > first_invoke(eb, 'Trials.naturalIdDisabled'),
        'WndEnergizeItem.energize：showIdentify 被守卫罩住')

    shop = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.levels.rooms.special.ShopRoom'])
    pb = method_body(shop, 'protected void placeItems(')
    chk(pb is not None and first_invoke(pb, 'Dungeon.isTrialled') != -1 and
        has_int_const(pb, yesod),
        'ShopRoom.placeItems 字节码：Dungeon.isTrialled 的掩码内联为 %s（= YESOD）' % yesod)
    chk(pb is not None and first_invoke(pb, 'ScrollOfIdentify') != -1,
        'ShopRoom.placeItems 字节码：真的 new 了一张 ScrollOfIdentify')
    sb = method_body(shop, 'public int spacesNeeded();')
    chk(sb is not None and first_invoke(sb, 'Trials') == -1,
        'ShopRoom.spacesNeeded 字节码：**不**读 Trials（房间尺寸与考验无关）')

    # 反向：Item.identify 的字节码里没有 Trials
    # （`items/Item.class` 只存在于 core/build/classes，靠 JAVAP_CP 顺延才解析得到）
    item = javap(['-c', '-p', '-cp', JAVAP_CP, PKG + '.items.Item'])
    ibody = method_body(item, 'public ' + PKG + '.items.Item identify(boolean);')
    chk(ibody is not None, '取到 Item.identify(boolean) 的字节码（总闸定位成功）')
    chk(ibody is not None and first_invoke(ibody, 'Trials') == -1,
        '反向：Item.identify(boolean) 字节码里没有 Trials（总闸未被污染）')


# ---------------------------------------------------------------- ④ 文本层
def layer_text():
    print('\n== ④ 文本层（直接读 .properties，权威来源）==')
    zh = open(os.path.join(MSG, 'misc_zh.properties'), 'rb').read()
    en = open(os.path.join(MSG, 'misc.properties'), 'rb').read()

    for name, raw in (('misc_zh.properties', zh), ('misc.properties', en)):
        chk(raw.count(b'\r\n') > 0, '%s 仍是 CRLF（%d 处）' % (name, raw.count(b'\r\n')))
        chk(raw.count(b'\n') - raw.count(b'\r\n') == 0, '%s 没有裸 LF' % name)
        chk(not raw.startswith(b'\xef\xbb\xbf'), '%s 没有 BOM' % name)

    def lines_of(raw):
        return raw.decode('utf-8').split('\r\n')

    zl, el = lines_of(zh), lines_of(en)

    def get(lines, key):
        hits = [l[len(key) + 1:] for l in lines if l.startswith(key + '=')]
        return hits

    # --- 新键：唯一 + 逐字 ---
    for key, want in [('trials.yesod_unknown', '你不知道这里是什么。')]:
        got = get(zl, key)
        chk(len(got) == 1, 'zh %s 恰好 1 条（实测 %d）' % (key, len(got)))
        chk(got and got[0] == want, 'zh %s == 「%s」' % (key, want))
    got = get(el, 'trials.yesod_unknown')
    chk(len(got) == 1 and got[0] == "You don't know what this is.",
        'en trials.yesod_unknown == "You don\'t know what this is."')

    for key in ['trials.yesod_desc', 'trials.id_blocked']:
        chk(len(get(zl, key)) == 1, 'zh %s 恰好 1 条' % key)
        chk(len(get(el, key)) == 1, 'en %s 恰好 1 条' % key)

    # --- yesod_desc：非占位、含字面 \n、够长 ---
    # ⚠️ 描述文本（trials.yesod_desc）由**用户**维护：2026-09-26 用户本人重写过中文版，
    #    所以这里只钉「不是占位 / 是字面 \n 分段 / 提到几条核心机制」，**不逐字比对**。
    descs = {}
    for name, lines in (('zh', zl), ('en', el)):
        d = get(lines, 'trials.yesod_desc')[0]
        descs[name] = d
        chk('（占位）' not in d and '(Placeholder)' not in d, '%s yesod_desc 不是占位文本' % name)
        chk('\\n\\n' in d, '%s yesod_desc 用**字面** \\n 分段（不是真换行）' % name)
        chk(len(d) > 90, '%s yesod_desc 长度 %d > 90' % (name, len(d)))
    for kw in ['无法自然鉴定', '炼金能量', '额外出售一张鉴定卷轴', '未知', '背包']:
        chk(kw in descs['zh'], 'zh yesod_desc 提到「%s」' % kw)
    # zh / en 对「容器是否受影响」的口径必须一致。2026-09-26 用户把中文那半句删掉了
    # （容器现在也变问号），英文那半句当时**没跟着删** —— 这条判据专门钉这种「只改一半」。
    z_cont = '容器' in descs['zh']
    e_cont = 'Container' in descs['en'] or 'chest' in descs['en'].lower()
    chk(z_cont == e_cont,
        'zh/en 对「容器」的口径一致（zh 提到=%s / en 提到=%s）' % (z_cont, e_cont))

    # --- id_blocked：带 %s 占位符 ---
    chk('%s' in get(zl, 'trials.id_blocked')[0], 'zh id_blocked 带 %s 占位符')
    chk('%s' in get(el, 'trials.id_blocked')[0], 'en id_blocked 带 %s 占位符')

    # --- netzach_desc：1 格 / 3 格，且不再出现 2 格 / 4 格 ---
    z = get(zl, 'trials.netzach_desc')[0]
    e = get(el, 'trials.netzach_desc')[0]
    chk('距离英雄超过 1 格' in z and '超过 3 格' in z, 'zh netzach_desc：1 格变淡 / 3 格不可见')
    chk('超过 2 格' not in z and '超过 4 格' not in z, 'zh netzach_desc：没有残留的 2 格 / 4 格')
    chk('more than 1 tile' in e and 'more than 3 tiles' in e, 'en netzach_desc：1 tile / 3 tiles')
    chk('more than 2 tiles' not in e and 'more than 4 tiles' not in e,
        'en netzach_desc：没有残留的 2 tiles / 4 tiles')


# ---------------------------------------------------------------- ⑤ PNG 层
def layer_png():
    print('\n== ⑤ items.png 实测（xy(16,2) 的像素包围盒）==')
    # 本机**没有 PIL**（两个解释器都装不上）⇒ 直接用仓库里现成的零依赖解码器 `_chk/png_probe.py`。
    # 那一份是「8bit / RGBA / 非隔行」的单文件解码器，正好够读 items.png 的原始像素。
    if HERE not in sys.path:
        sys.path.insert(0, HERE)
    try:
        import png_probe
    except ImportError:
        skip('找不到 _chk/png_probe.py —— 无法直读像素（结论已在 2026-09-26 实测：10×15 @ (0,0)）')
        return
    W, H, px = png_probe.decode(ITEMS_PNG)
    chk((W, H) == (256, 800), 'items.png 尺寸 256×800（实测 %d×%d）' % (W, H))
    x0, y0 = 15 * 16, 1 * 16                 # xy(16,2) 的 1 基坐标 → 0 基 (15,1)
    xs = [x - x0 for y in range(y0, y0 + 16) for x in range(x0, x0 + 16)
          if png_probe.alpha_at(W, px, x, y)]
    ys = [y - y0 for y in range(y0, y0 + 16) for x in range(x0, x0 + 16)
          if png_probe.alpha_at(W, px, x, y)]
    if not xs:
        chk(False, 'xy(16,2) 单元格是空的（应该是 10×15 的问号）')
        return
    w, h = max(xs) - min(xs) + 1, max(ys) - min(ys) + 1
    # 包围盒必须**正好**等于取样矩形 10×15：大了会带透明边被居中推偏、小了直接裁
    # （见 skill egopd-new-weapon 里 assignItemRect 与自然尺寸的耦合）。
    chk(w == 10 and h == 15, 'xy(16,2) 的非透明包围盒 = 10×15（实测 %d×%d）' % (w, h))
    chk(min(xs) == 0 and min(ys) == 0, '包围盒起点在格内 (0,0)（实测 (%d,%d)）' % (min(xs), min(ys)))
    chk(len(xs) > 0, 'xy(16,2) 有非透明像素（实测 %d 个）' % len(xs))


# ---------------------------------------------------------------- ⑥ 探针层
def layer_probe():
    print('\n== ⑥ 行为探针层 ==')
    if not os.path.exists(PROBE_OUT):
        chk(False, '没有 %s —— 先跑 bash _chk/_build_yesod.sh run' % os.path.relpath(PROBE_OUT, ROOT))
        return
    out = open(PROBE_OUT, encoding='utf-8', errors='replace').read()
    m = re.search(r'YesodProbe 结果：OK (\d+) / FAIL (\d+)（断言共 (\d+)', out)
    chk(m is not None, '读得到探针汇总行')
    if m:
        okn, failn, tot = int(m.group(1)), int(m.group(2)), int(m.group(3))
        chk(failn == 0, '探针 0 FAIL（实测 FAIL=%d）' % failn)
        chk(tot >= 35, '探针断言数 %d ≥ 35' % tot)
        chk(okn == tot - failn, 'OK 数与总数自洽（%d / %d）' % (okn, tot))
    for head in ['① 未知贴图常量与取样矩形', '② naturalIdDisabled() 门控',
                 '③ hidesGroundHeap() 真值表', '⑥ NETZACH 淡出阈值（1 格 / 3 格）',
                 '⑦ 鉴定闸门实跑（Weapon.proc 数满）']:
        chk(head in out, '探针跑过「%s」段' % head)
    chk('[INFO]' in out, '探针里受环境限制的两段以 [INFO] 形式留痕（不是静默跳过）')
    chk(not re.search(r'^  \[FAIL\]', out, re.M), '探针输出里没有任何 [FAIL]')


def main():
    if '--selftest' in sys.argv:
        selftest()
        return 0
    if not os.path.isdir(CLASSES):
        print('缺少 %s —— 先跑 bash _chk/_build_yesod.sh probe' % os.path.relpath(CLASSES, ROOT))
        return 2
    print('核验：考验 YESOD（根基）＋ NETZACH 淡出阈值（1 格 / 3 格）\n')
    layer_source()
    layer_signatures()
    layer_bytecode()
    layer_text()
    layer_png()
    layer_probe()
    print('\n' + '=' * 62)
    if ok:
        print('全部核验通过。')
    else:
        print('存在失败项，见上方 [FAIL]。')
    print('=' * 62)
    return 0 if ok else 1


if __name__ == '__main__':
    sys.exit(main())
