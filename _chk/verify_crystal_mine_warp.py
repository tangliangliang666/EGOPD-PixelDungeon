"""
「矿洞跃迁符」（items/CrystalMineWarp.java）· 源码级核验（2026-09-26）

为什么要有它：这个道具的唯一用途是**把玩家反复送进水晶任务矿洞层看地形**，
所以它一旦写错，坏的不是数值而是**存档**（跳错层 / 任务种类不还原 / 把已探索的矿层洗掉）。
因此按项目惯例做「结构断言 + 反例自测」，覆盖：

  A. 道具本体：类形状、三个动作常量、默认动作、占位图标、调试道具契约、bundle 字段。
  B. `actions()`：跃迁/重 roll 恒在，**返回只在矿层里**给（否则在外面按了会把人乱传）。
  C. `execute()` 跃迁/重 roll 路径：博物馆禁传送 → 换层权限 → 只在矿层外记「来处」→
     顶任务种类为 CRYSTAL → 深度 11~14 轮换 → 只有 REROLL 清生成记录 →
     走 `Mode.RETURN` 且 `returnPos = -1`（交给引擎落到入口）→ `beforeTransition()` 先于切场景。
  D. `execute()` 返回路径：还原任务种类、回到出发层**原坐标**（不是 -1）。
  E. `Blacksmith.Quest.setType` 这个新钩子：确实写 `type`，且没有把 `Type()` 的只读性破坏。
  F. 三份 `items*.properties` 的文本键（en / zh / zh-hant 都在，且 desc 是单行）。
  G. 可及性与「零掉落渠道」：调试窗两个入口都仍是 items 根包浅层扫描；
     全仓除本文件外没有任何引用（证明它没被塞进掉落池/商店）。

用法：`python _chk/verify_crystal_mine_warp.py`（加 `--selftest` 跑反例自测，反例 FAIL **不计总账**）。
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
MSG = os.path.join(ROOT, 'core/src/main/assets/messages/items')

# ⚠️ 本仓行尾是混的：items*.properties 里有纯 LF 的、也有 CRLF 的（见 verify_marked_kill_routes 顶部注释）。
# 所以反例正则一律用 `\s*` / `[^\n]*\r?\n`，别写死 `\n`。核验只读不写，不会碰行尾。
FILES = {
    'warp':   os.path.join(SRC, 'items/CrystalMineWarp.java'),
    'smith':  os.path.join(SRC, 'actors/mobs/npcs/Blacksmith.java'),
    'wnddbg': os.path.join(SRC, 'windows/WndDebug.java'),
    'console': os.path.join(SRC, 'debug/SpdConsoleProviders.java'),
}

TEXT = {
    'it_en':   os.path.join(MSG, 'items.properties'),
    'it_zh':   os.path.join(MSG, 'items_zh.properties'),
    'it_hant': os.path.join(MSG, 'items_zh-hant.properties'),
}

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
    """反例变异器工厂：在 raw 上按正则替换，再重算剥注释版本。

    ⚠️ 这里用 `re.S | re.M`：`re.S` 让 `[^\\n]*\\r?\\n` 之类跨行写法可用，
    `re.M` 让 `^items\\.xxx\\.name=` 这种**行首锚点**能用（否则 `^` 只匹配整个文件开头，
    F1/F5 那两条「删掉某个键」的反例就永远命中 0 次、自测被 SKIP 掉）。"""
    def f(S):
        S[key]['raw'], n = re.subn(pattern, repl, S[key]['raw'], count=count, flags=re.S | re.M)
        assert n == count, '反例变异没命中（%d 次）：%s' % (n, pattern)
        if 'code' in S[key]:
            S[key]['code'] = strip_comments(S[key]['raw'])
    return f


def mut_drop(key, pattern, count=1):
    """把命中的片段删掉（用于「本该存在」的判据）。"""
    return mut_sub(key, pattern, '', count=count)


WARP_BODY = 'public void execute(Hero hero, String action) {'
ACTIONS_BODY = 'public ArrayList<String> actions(Hero hero) {'


# ---------------------------------------------------------------- A 组
@case('A1', '类形状：public class CrystalMineWarp extends Item',
      mut_sub('warp', r'public class CrystalMineWarp extends Item', 'class CrystalMineWarp'))
def a1(S):
    return has(S['warp']['code'], 'public class CrystalMineWarp extends Item')


@case('A2', '位于 items **根包**（调试窗「杂项」浅层扫描才收得到它）',
      mut_sub('warp', r'package com\.shatteredpixel\.shatteredpixeldungeon\.items;',
              'package com.shatteredpixel.shatteredpixeldungeon.items.quest;'))
def a2(S):
    return has(S['warp']['code'], 'package com.shatteredpixel.shatteredpixeldungeon.items;')


@case('A3', '三个动作常量＝WARP / REROLL / RETURN（action 字符串与键名一致）',
      mut_sub('warp', r'public static final String AC_RETURN = "RETURN";',
              'public static final String AC_RETURN = "USE";'))
def a3(S):
    c = sq(S['warp']['code'])
    return 'public static final String AC_WARP = "WARP";' in c \
        and 'public static final String AC_REROLL = "REROLL";' in c \
        and 'public static final String AC_RETURN = "RETURN";' in c


@case('A4', '默认动作＝WARP（点图标直接跳，不用先点按钮）',
      mut_sub('warp', r'defaultAction = AC_WARP;', 'defaultAction = AC_RETURN;'))
def a4(S):
    return has(S['warp']['code'], 'defaultAction = AC_WARP;')


@case('A5', '图标用现成合法常量 ESCAPE（调试期占位，不碰 items.png）',
      mut_sub('warp', r'image = ItemSpriteSheet\.ESCAPE;', 'image = ItemSpriteSheet.NO_SPRITE;'))
def a5(S):
    return has(S['warp']['code'], 'image = ItemSpriteSheet.ESCAPE;')


@case('A6', '调试道具契约：恒已辨识 + 不可升级（与 TestPortal 一致）',
      mut_sub('warp', r'public boolean isIdentified\(\) \{\s*return true;',
              'public boolean isIdentified() { return false;'))
def a6(S):
    b_id = body(S, 'warp', 'public boolean isIdentified() {')
    b_up = body(S, 'warp', 'public boolean isUpgradable() {')
    return has(b_id, 'return true;') and has(b_up, 'return false;')


@case('A7', 'bundle 存了 5 个字段（来处三坐标 + 来处任务种类 + 轮换游标）',
      mut_drop('warp', r'bundle\.put\( NEXT_DEPTH,\s*nextDepth \);'))
def a7(S):
    st = body(S, 'warp', 'public void storeInBundle(Bundle bundle) {')
    rs = body(S, 'warp', 'public void restoreFromBundle(Bundle bundle) {')
    keys = ['FROM_DEPTH', 'FROM_BRANCH', 'FROM_POS', 'FROM_QUEST', 'NEXT_DEPTH']
    return all(('bundle.put( %s,' % k) in sq(st) for k in keys) \
        and all(('bundle.getInt( %s )' % k) in sq(rs) for k in keys)


# ---------------------------------------------------------------- B 组
@case('B1', '跃迁 / 重 roll 两个动作恒在',
      mut_drop('warp', r'actions\.add\(AC_REROLL\);'))
def b1(S):
    b = sq(body(S, 'warp', ACTIONS_BODY))
    return 'actions.add(AC_WARP);' in b and 'actions.add(AC_REROLL);' in b


@case('B2', '「返回」只在「有来处 且 身处矿层」时出现（否则在外面按了会把人乱传）',
      mut_sub('warp', r'if \(fromDepth > 0 && Dungeon\.level instanceof MiningLevel\)\{',
              'if (true){'))
def b2(S):
    b = sq(body(S, 'warp', ACTIONS_BODY))
    return 'actions.add(AC_RETURN);' in b \
        and 'if (fromDepth > 0 && Dungeon.level instanceof MiningLevel){' in b


# ---------------------------------------------------------------- C 组（跃迁 / 重 roll）
@case('C1', '「死亡证明」陈列室内禁传送（与 TestPortal / EscapeCrystal 同口径）',
      mut_drop('warp', r'if \(MuseumLevel\.blockTeleport\( this \) \) return;'))
def c1(S):
    return has(body(S, 'warp', WARP_BODY), 'if (MuseumLevel.blockTeleport( this )) return;')


@case('C2', '换层权限守卫：interfloorTeleportAllowed() 不通过就报标准 no_tele，不硬跳',
      mut_drop('warp', r'if \(!Dungeon\.interfloorTeleportAllowed\(\)\)\{'))
def c2(S):
    b = body(S, 'warp', WARP_BODY)
    return has(b, 'if (!Dungeon.interfloorTeleportAllowed()){') \
        and has(b, 'Messages.get( ScrollOfTeleportation.class, "no_tele" )')


@case('C3', '只在**矿层之外**记「来处」——否则在矿层里连点会把来处冲成矿层自己',
      mut_sub('warp', r'if \(!\(Dungeon\.level instanceof MiningLevel\)\)\{\s*fromDepth',
              'if (true){ fromDepth'))
def c3(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return 'if (!(Dungeon.level instanceof MiningLevel)){' in b \
        and 'fromDepth = Dungeon.depth;' in b and 'fromBranch = Dungeon.branch;' in b \
        and 'fromPos = hero.pos;' in b and 'fromQuestType = Blacksmith.Quest.Type();' in b


@case('C4', '顶任务种类为 CRYSTAL（虚空地形与 handlesChasmFall 都读它）',
      mut_sub('warp', r'Blacksmith\.Quest\.setType\( Blacksmith\.Quest\.CRYSTAL \);',
              'Blacksmith.Quest.setType( Blacksmith.Quest.GNOLL );'))
def c4(S):
    return has(body(S, 'warp', WARP_BODY), 'Blacksmith.Quest.setType( Blacksmith.Quest.CRYSTAL );')


@case('C5', '深度在 11~14 之间轮换（常量 + 回绕表达式）',
      mut_sub('warp', r'nextDepth = \(depth >= LAST_MINE_DEPTH\) \? FIRST_MINE_DEPTH : depth \+ 1;',
              'nextDepth = depth;'))
def c5(S):
    c = sq(S['warp']['code'])
    b = sq(body(S, 'warp', WARP_BODY))
    return 'public static final int FIRST_MINE_DEPTH = 11;' in c \
        and 'public static final int LAST_MINE_DEPTH = 14;' in c \
        and 'nextDepth = (depth >= LAST_MINE_DEPTH) ? FIRST_MINE_DEPTH : depth + 1;' in b


@case('C6', '分支固定为 1（MiningLevel 只在 branch==1 且 depth∈[11,14] 生成）',
      mut_sub('warp', r'InterlevelScene\.returnBranch = MINE_BRANCH;',
              'InterlevelScene.returnBranch = 0;'))
def c6(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return 'public static final int MINE_BRANCH = 1;' in sq(S['warp']['code']) \
        and 'InterlevelScene.returnBranch = MINE_BRANCH;' in b


@case('C7', '只有「重 roll」清生成记录 ⇒ 跃迁保留地形、重 roll 每次都是新地形',
      mut_sub('warp', r'if \(AC_REROLL\.equals\(action\)\)\{\s*Dungeon\.generatedLevels',
              'if (AC_WARP.equals(action)){ Dungeon.generatedLevels'))
def c7(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return 'if (AC_REROLL.equals(action)){' in b \
        and 'Dungeon.generatedLevels.remove( Integer.valueOf( depth + 1000 * MINE_BRANCH ) );' in b


@case('C8', '走 Mode.RETURN，落点 -1（交给 Dungeon.switchLevel 回退到矿层入口楼梯）',
      mut_sub('warp', r'InterlevelScene\.returnPos    = -1;', 'InterlevelScene.returnPos = 0;'))
def c8(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return 'InterlevelScene.mode = InterlevelScene.Mode.RETURN;' in b \
        and 'InterlevelScene.returnDepth = depth;' in b \
        and 'InterlevelScene.returnPos = -1;' in b


@case('C9', 'beforeTransition() 先于切场景（否则换层钩子漏跑）',
      mut_drop('warp', r'Level\.beforeTransition\(\);'))
def c9(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return b.find('Level.beforeTransition();') >= 0 \
        and b.find('Level.beforeTransition();') < b.find('Game.switchScene( InterlevelScene.class );')


# ---------------------------------------------------------------- D 组（返回）
@case('D1', '返回时**还原**出发前的任务种类（用完之后不留副作用）',
      mut_sub('warp', r'Blacksmith\.Quest\.setType\( fromQuestType \);',
              'Blacksmith.Quest.setType( Blacksmith.Quest.CRYSTAL );'))
def d1(S):
    return has(body(S, 'warp', WARP_BODY), 'Blacksmith.Quest.setType( fromQuestType );')


@case('D2', '返回用出发层**原坐标**（不是 -1，否则会被丢回入口/出口楼梯）',
      mut_sub('warp', r'InterlevelScene\.returnPos    = fromPos;', 'InterlevelScene.returnPos = -1;'))
def d2(S):
    b = sq(body(S, 'warp', WARP_BODY))
    return 'InterlevelScene.returnDepth = fromDepth;' in b \
        and 'InterlevelScene.returnBranch = fromBranch;' in b \
        and 'InterlevelScene.returnPos = fromPos;' in b


@case('D3', '没有来处时不返回（fromDepth <= 0 直接退出）',
      mut_sub('warp', r'if \(fromDepth <= 0 \|\| Dungeon\.level == null\) return;',
              'if (false) return;'))
def d3(S):
    return has(body(S, 'warp', WARP_BODY), 'if (fromDepth <= 0 || Dungeon.level == null) return;')


# ---------------------------------------------------------------- E 组（Blacksmith 钩子）
@case('E1', 'Blacksmith.Quest 上存在 public static void setType(int)，且写的正是 type',
      mut_sub('smith', r'public static void setType\( int t \)\{\s*type = t;',
              'public static void setType( int t ){ /* no-op */'))
def e1(S):
    b = body(S, 'smith', 'public static void setType( int t ){')
    return has(b, 'type = t;')


@case('E2', 'Type() 仍是只读返回 type（没被顺手改成别的语义）',
      mut_sub('smith', r'public static int Type\(\)\{\s*return type;', 'public static int Type(){ return 0;'))
def e2(S):
    b = body(S, 'smith', 'public static int Type(){')
    return sq(b) == '{ return type; }'


@case('E3', 'type 字段仍是 private static（外部只能走 setType，没被开成 public）',
      mut_sub('smith', r'private static int type = 0;', 'public static int type = 0;'))
def e3(S):
    return has(S['smith']['code'], 'private static int type = 0;')


# ---------------------------------------------------------------- F 组（文本）
WARP_KEYS = ['name', 'ac_warp', 'ac_reroll', 'ac_return', 'desc', 'warp', 'reroll', 'back']


def _line(S, key, k):
    """取 items.crystalminewarp.<k> 这一整行（原始，未压空白）。"""
    m = re.search(r'^items\.crystalminewarp\.%s[^\n]*$' % re.escape(k), S[key]['raw'], re.M)
    return m.group(0) if m else None


@case('F1', '英文 8 个键齐全，且 name / 动作键都非空',
      mut_drop('it_en', r'^items\.crystalminewarp\.reroll[^\n]*\r?\n'))
def f1(S):
    for k in WARP_KEYS:
        ln = _line(S, 'it_en', k)
        if ln is None or not ln.split('=', 1)[1].strip():
            return False
    return True


@case('F2', '简中键齐全，且 name 是用户会看到的「矿洞跃迁符」',
      mut_sub('it_zh', r'(items\.crystalminewarp\.name=).*', r'\1\n'))
def f2(S):
    ln = _line(S, 'it_zh', 'name')
    return all(_line(S, 'it_zh', k) for k in WARP_KEYS) \
        and bool(ln) and ln.endswith('items.crystalminewarp.name=矿洞跃迁符')


@case('F3', '繁中键齐全，且是繁体转写（不含简体「矿洞跃迁符」）',
      mut_sub('it_hant', r'(items\.crystalminewarp\.name=).*', r'\1矿洞跃迁符'))
def f3(S):
    ln = _line(S, 'it_hant', 'name')
    return all(_line(S, 'it_hant', k) for k in WARP_KEYS) \
        and bool(ln) and ln.endswith('礦洞躍遷符') and '矿洞' not in ln


@case('F4', 'desc 是**单行**（`.properties` 里只能有字面 \\n，不能有真换行）',
      mut_sub('it_zh', r'(items\.crystalminewarp\.desc=)[^\n]*',
              r'\1第一行\n第二行'))
def f4(S):
    for key in ('it_en', 'it_zh', 'it_hant'):
        ln = _line(S, key, 'desc')
        if ln is None:
            return False
        # _line 用的是 [^\n]*，只要能取到且结尾不带 \r 就说明这一行是完整的
        if ln.endswith('\r'):
            return False
        if '\\n' not in ln:
            return False
    return True


@case('F5', '动作按钮的键名与 AC_* 常量对得上（ac_warp / ac_reroll / ac_return 都在）',
      mut_drop('it_en', r'^items\.crystalminewarp\.ac_reroll[^\n]*\r?\n'))
def f5(S):
    return all(_line(S, k, x) for k in ('it_en', 'it_zh', 'it_hant')
               for x in ('ac_warp', 'ac_reroll', 'ac_return'))


# ---------------------------------------------------------------- G 组（可及性 + 零掉落）
@case('G1', 'F2 调试窗「杂项」仍是 items 根包的**浅层**扫描 ⇒ 新道具自动收录',
      mut_sub('wnddbg', r'new ItemTab\( Item\.class,\s*PKG_ITEMS_ROOT,\s*true,',
              'new ItemTab( Item.class, PKG_ITEMS_ROOT, false,'))
def g1(S):
    return has(S['wnddbg']['code'], 'new ItemTab( Item.class, PKG_ITEMS_ROOT, true,') \
        and has(S['wnddbg']['code'], 'miscTab')


@case('G2', '调试控制台「杂项」也仍是 items 根包浅层扫描',
      mut_sub('console', r'ScanProvider\.shallow\("杂项", Item\.class, PKG \+ "items"\)',
              'ScanProvider.shallow("杂项", Item.class, PKG + "items.food")'))
def g2(S):
    return has(S['console']['code'], 'ScanProvider.shallow("杂项", Item.class, PKG + "items")')


@case('G3', '「零掉落渠道」：全仓除本文件外没有任何地方引用 CrystalMineWarp',
      None)
def g3(S):
    hits = []
    for dirpath, _dirnames, filenames in os.walk(SRC):
        for fn in filenames:
            if not fn.endswith('.java'):
                continue
            p = os.path.join(dirpath, fn)
            if os.path.abspath(p) == os.path.abspath(FILES['warp']):
                continue
            src = open(p, encoding='utf-8', errors='replace').read()
            if 'CrystalMineWarp' in src:
                hits.append(os.path.relpath(p, ROOT).replace('\\', '/'))
    if hits:
        print('       引用点：%s' % ', '.join(hits[:5]))
    return not hits


# ============================================================ 反例自测
# 判据靠「源码里该出现的东西」判定，反例＝把它改掉。
# 这里只需要列出由**判据自身**抓错的情况；没有合适反例的（如 G3 的全局扫描）
# 交给 mutator=None + 下面的 G3MUT 特殊处理。
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
    print('「矿洞跃迁符」· 核验（2026-09-26）')
    print('=' * 78)

    groups = [
        ('A. 道具本体：类形状 / 动作常量 / 占位图标 / bundle 字段', 'A'),
        ('B. actions()：返回动作的门控', 'B'),
        ('C. execute()：跃迁 / 重 roll 路径', 'C'),
        ('D. execute()：返回路径', 'D'),
        ('E. Blacksmith.Quest.setType 钩子', 'E'),
        ('F. 三份 items*.properties 的文本键', 'F'),
        ('G. 可及性 + 零掉落渠道', 'G'),
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
        skipped = 0
        for cid, desc, fn, _mut in CASES:
            if _mut is None:
                print('  [SKIP] %s 无反例（全局扫描类）' % cid)
                skipped += 1
                continue
            S2 = load()
            try:
                _mut(S2)
            except AssertionError as e:
                print('  [SKIP] %s 反例变异未命中（%s）' % (cid, e))
                skipped += 1
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
        print('\n反例自测：%d 条判据，%d 条无反例，%d 条抓不到错。'
              % (len(CASES), skipped, bad))
        if bad:
            sys.exit(2)

    if not ok:
        sys.exit(1)


if __name__ == '__main__':
    main()
