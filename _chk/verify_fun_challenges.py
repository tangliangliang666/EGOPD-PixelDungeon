# -*- coding: utf-8 -*-
"""「趣味挑战」分类的跨文件接线核验（EGOPD，2026-09-24）。

用户口径：新建一类挑战「趣味挑战」，把 拆迁办 / 依旧果冻人 / 水仙追迹 / 调试模式 四条从常规挑战移出；
趣味挑战**不计入常规挑战数**（随机池、局内计数、CHAMPION_* 徽章都不受影响），
但**仍计入得分倍率**（各自设定；调试模式 / 水仙追迹仍把倍率置 0）；套用挑战的图标与排版。

本脚本跨 10 个源码文件 + 2 个文案文件断言「接线」，分 A~K 十组：

  A. Challenges 判据层：FUN_MASK 恰为那四条、isFun 用位与、isRegular = !isFun、
     regularMasks/funMasks 各自过滤、regularMask/funMask 并集、nameId 反查、
     activeChallenges 只数常规、activeFun 只数趣味、**scoredChallenges 只排调试模式**（旧口径）；
     并由 MASKS/NAME_IDS 实测推出「常规数 == MAX_CHALS」与两条计数关系。
  B. Icons 图标层：枚举三件套 + 三处 rect 字面量。
  C. WndChallenges：只列常规挑战、写回**保留趣味位**、不再用下标配对。
  D. WndFunChallenges：新窗口存在、只列趣味挑战、写回**保留常规位**、不再用下标配对。
  E. HeroSelectScene：funButton + 入口 + 图标判据必须带参（⚠️ 选中界面里 Dungeon.challenges 恒 0）。
  F. MenuPane：局内计数块（FUN_COUNT）+ 栏位偏移 -28 + 无参 activeFun()。
  G. WndGame：局内菜单按钮；且**常规挑战按钮的判据要跟着拆**（否则只开趣味挑战时弹空窗）。
  H. WndGameInProgress：hasChals / hasFun 两个判据 + 两个按钮。
  I. WndRanking：7 个页签（尾位 FUN_COLOR）、挑战页只列常规、新增 FunChallengesTab 只列趣味、
     且**页签行数能塞进 HEIGHT=144**（拆分顺带修掉了老裁剪 bug）。
  J. Rankings：得分倍率走 scoredChallenges。
  K. 文案键：windows zh/en 三处「趣味挑战」。

--selftest：每个「能抓到错」的判据都配反例（把旧写法/漏写喂给同一判据，确认判 FAIL）。
"""
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
sys.path.insert(0, HERE)
# 保偏移的单趟状态机剥注释（**别用正则去 /* */ **——`//**` 会一路吞到文件末尾，见 skill §10 / 铁律 3）
from check_unused_imports import strip_comments

SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
CH       = os.path.join(SRC, 'Challenges.java')
ICONS    = os.path.join(SRC, 'ui/Icons.java')
WNDCH    = os.path.join(SRC, 'windows/WndChallenges.java')
WNDFUN   = os.path.join(SRC, 'windows/WndFunChallenges.java')
HSS      = os.path.join(SRC, 'scenes/HeroSelectScene.java')
MENU     = os.path.join(SRC, 'ui/MenuPane.java')
WNDGAME  = os.path.join(SRC, 'windows/WndGame.java')
WNDGIP   = os.path.join(SRC, 'windows/WndGameInProgress.java')
WNDRANK  = os.path.join(SRC, 'windows/WndRanking.java')
RANK     = os.path.join(SRC, 'Rankings.java')
MSGS     = os.path.join(ROOT, 'core/src/main/assets/messages/windows')
WZH      = os.path.join(MSGS, 'windows_zh.properties')
WEN      = os.path.join(MSGS, 'windows.properties')

ok = True
results = []


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False
    results.append(bool(cond))


def read(p):
    return open(p, encoding='utf-8').read()


def sq(t):
    """压掉空白，便于跨行表达式做子串判定。"""
    return " ".join(t.split())


# ---------------- 通用小工具 ----------------
def body_of(text, signature):
    """按大括号配平取方法/块主体（够用即可）。"""
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


def code_body(text, signature):
    """取方法体 + 剥注释（**顺序敏感/不得出现 X** 的判据统一走这里）。"""
    b = body_of(strip_comments(text), signature)
    return "" if b is None else b


def braced_after(text, anchor):
    """取 anchor 之后第一对配平大括号里的内容（用于解析数组字面量）。"""
    i = text.find(anchor)
    if i < 0:
        return None
    s = text.find("{", i)
    if s < 0:
        return None
    depth = 0
    for j in range(s, len(text)):
        if text[j] == "{":
            depth += 1
        elif text[j] == "}":
            depth -= 1
            if depth == 0:
                return text[s + 1:j]
    return None


def count_entries(inner):
    """数数组字面量里的顶层元素个数（按括号深度忽略内层逗号）。"""
    if inner is None or not inner.strip():
        return 0
    depth = 0
    n = 1
    for ch in inner:
        if ch in '([':
            depth += 1
        elif ch in ')]':
            depth -= 1
        elif ch == ',' and depth == 0:
            n += 1
    return n


def parse_rects(text):
    """case XXX: -> uvRectBySize(l, t, w, h)，只取纯数字形式。"""
    out = {}
    cur = None
    for line in text.splitlines():
        m = re.match(r'\s*case\s+([A-Za-z_]\w*)\s*:', line)
        if m:
            cur = m.group(1)
            continue
        m = re.search(r'uvRectBySize\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', line)
        if m and cur is not None:
            out[cur] = tuple(int(v) for v in m.groups())
            cur = None
    return out


# ---------------- 可被反例复用的判据（函数化，便于 --selftest） ----------------
def pred_wndch_preserves_fun(text):
    """WndChallenges 写回时必须「只清常规位、保留趣味位」。"""
    return "SPDSettings.challenges() & ~Challenges.regularMask()" in sq(strip_comments(text))


def pred_wndfun_preserves_regular(text):
    """WndFunChallenges 写回时必须「只清趣味位、保留常规位」。"""
    return "SPDSettings.challenges() & ~Challenges.funMask()" in sq(strip_comments(text))


def pred_scene_activefun_with_arg(text):
    """选中界面必须用带参 activeFun(SPDSettings.challenges())，且**不得**出现无参 activeFun()。"""
    c = sq(strip_comments(text))
    if "Challenges.activeFun(SPDSettings.challenges()) > 0" not in c:
        return False
    return "Challenges.activeFun()" not in c


def pred_menu_activefun_noarg(text):
    """局内菜单计数必须用无参 activeFun()（＝Dungeon.challenges）。"""
    return "Challenges.activeFun() > 0" in sq(strip_comments(text))


def pred_rankings_scored(text):
    """倍率必须走 scoredChallenges，且旧写法（activeChallenges 进 pow）必须消失。"""
    c = sq(strip_comments(text))
    return ("Math.pow(1.25, Challenges.scoredChallenges())" in c
            and "Math.pow(1.25, Challenges.activeChallenges())" not in c)


def pred_funmasks_uses_isfun(text):
    """funMasks() 必须按 isFun 过滤（用 isRegular 就是反向 bug）。"""
    b = code_body(text, "public static int[] funMasks()")
    return "isFun( ch )" in b and "isRegular( ch )" not in b


def pred_regularmasks_uses_isregular(text):
    b = code_body(text, "public static int[] regularMasks()")
    return "isRegular( ch )" in b and "isFun( ch )" not in b


def pred_isregular_is_not_isfun(text):
    b = sq(code_body(text, "public static boolean isRegular( int mask )"))
    return "return !isFun( mask );" in b


def pred_isfun_uses_bitand(text):
    b = sq(code_body(text, "public static boolean isFun( int mask )"))
    return "(mask & FUN_MASK) != 0" in b


def pred_scored_only_excludes_debug(text):
    """scoredChallenges(int) 只排 DEBUG_MODE，不得引入 isFun/isRegular 分类过滤。"""
    b = code_body(text, "public static int scoredChallenges(int mask)")
    return "ch == DEBUG_MODE" in b and "isFun" not in b and "isRegular" not in b


def pred_rank_page4_guarded(text):
    return ("if (Challenges.activeChallenges(Dungeon.challenges) > 0) pages[4] = new ChallengesTab();"
            in sq(strip_comments(text)))


def pred_rank_page6_guarded(text):
    return ("if (Challenges.activeFun(Dungeon.challenges) > 0) pages[6] = new FunChallengesTab();"
            in sq(strip_comments(text)))


def rows_fit(rows, limit=144):
    """WndRanking 的行排版：第 i 行 top=16i、bottom=16i+15 ⇒ rows 行占 16*rows-1 px。"""
    return rows * 16 - 1 <= limit


def parse_masks(text):
    """由源码推出 (masks 列表, 常量表, fun 位集合, regular 数)。"""
    code = strip_comments(text)
    consts = dict(
        (m.group(1), int(m.group(2)))
        for m in re.finditer(r'public static final int\s+([A-Z_0-9]+)\s*=\s*(\d+)\s*;', code))
    inner = braced_after(code, "public static final int[] MASKS")
    names = [x.strip() for x in (inner or '').split(',') if x.strip()]
    masks = [consts[n] for n in names]
    fun_names = ['DEMOLITION_SQUAD', 'JELLY_PERSON', 'NARCISSUS_TRACING', 'DEBUG_MODE']
    fun_bits = set(consts[n] for n in fun_names)
    regular = [m for m in masks if m not in fun_bits]
    return masks, consts, fun_bits, regular


def selftest():
    print('== selftest：反例自测（每条判据都要能抓到错，不能恒真）==')
    global ok
    keep = ok
    ok = True
    cases = [
        ('WndChallenges 写回保留趣味位',
         pred_wndch_preserves_fun,
         'int value = 0; SPDSettings.challenges(value);'),
        ('WndFunChallenges 写回保留常规位',
         pred_wndfun_preserves_regular,
         'int value = 0; SPDSettings.challenges(value);'),
        ('选中界面用带参 activeFun',
         pred_scene_activefun_with_arg,
         'funButton.icon(Icons.get(Challenges.activeFun() > 0 ? Icons.FUN_COLOR : Icons.FUN_GREY));'),
        ('局内计数用无参 activeFun',
         pred_menu_activefun_noarg,
         'if (Challenges.activeFun(SPDSettings.challenges()) > 0){'),
        ('倍率走 scoredChallenges',
         pred_rankings_scored,
         'Statistics.chalMultiplier = (float)Math.pow(1.25, Challenges.activeChallenges());'),
        ('funMasks 按 isFun 过滤',
         pred_funmasks_uses_isfun,
         'public static int[] funMasks(){ for (int ch : MASKS) if (isRegular( ch )) out[i++] = ch; return out; }'),
        ('regularMasks 按 isRegular 过滤',
         pred_regularmasks_uses_isregular,
         'public static int[] regularMasks(){ for (int ch : MASKS) if (isFun( ch )) out[i++] = ch; return out; }'),
        ('isRegular == !isFun',
         pred_isregular_is_not_isfun,
         'public static boolean isRegular( int mask ){ return mask != DEBUG_MODE; }'),
        ('isFun 用位与',
         pred_isfun_uses_bitand,
         'public static boolean isFun( int mask ){ return mask == FUN_MASK; }'),
        ('scoredChallenges 只排调试模式',
         pred_scored_only_excludes_debug,
         'public static int scoredChallenges(int mask){ for (int ch : MASKS){ if (!isFun(ch)) continue; if ((mask & ch) != 0) chCount++; } return chCount; }'),
        ('WndRanking 挑战页判据已拆',
         pred_rank_page4_guarded,
         'if (Dungeon.challenges != 0) pages[4] = new ChallengesTab();'),
        ('WndRanking 趣味页有门控',
         pred_rank_page6_guarded,
         'pages[6] = new FunChallengesTab();'),
    ]
    bad = 0
    for name, fn, counter in cases:
        got = fn(counter)
        print('  反例 %-30s -> 判据结果 %s（应为 False）' % (name, got))
        if got:
            bad += 1
            print('    !! 判据恒真：反例也通过，等于没测')
    # 正例对照：真源码必须通过
    trues = [
        ('WndChallenges 写回保留趣味位', pred_wndch_preserves_fun, WNDCH),
        ('WndFunChallenges 写回保留常规位', pred_wndfun_preserves_regular, WNDFUN),
        ('选中界面用带参 activeFun', pred_scene_activefun_with_arg, HSS),
        ('局内计数用无参 activeFun', pred_menu_activefun_noarg, MENU),
        ('倍率走 scoredChallenges', pred_rankings_scored, RANK),
        ('funMasks 按 isFun 过滤', pred_funmasks_uses_isfun, CH),
        ('regularMasks 按 isRegular 过滤', pred_regularmasks_uses_isregular, CH),
        ('isRegular == !isFun', pred_isregular_is_not_isfun, CH),
        ('isFun 用位与', pred_isfun_uses_bitand, CH),
        ('scoredChallenges 只排调试模式', pred_scored_only_excludes_debug, CH),
        ('WndRanking 挑战页判据已拆', pred_rank_page4_guarded, WNDRANK),
        ('WndRanking 趣味页有门控', pred_rank_page6_guarded, WNDRANK),
    ]
    for name, fn, path in trues:
        got = fn(read(path))
        print('  正例 %-30s -> 判据结果 %s（应为 True）' % (name, got))
        if not got:
            bad += 1
            print('    !! 正例被判 FAIL —— 判据写错了')
    # 行数能塞进 HEIGHT
    print('  rows_fit(9)=%s（应 True）  rows_fit(13)=%s（应 False）' % (rows_fit(9), rows_fit(13)))
    if not rows_fit(9) or rows_fit(13):
        bad += 1
        print('    !! rows_fit 判据失效')
    ok = keep
    if bad:
        raise AssertionError('selftest 失败 %d 项' % bad)
    print('  selftest 通过：每条判据都抓得住它的反例，且真源码判 True。\n')


print('=' * 78)
print('核验：趣味挑战分类的跨文件接线')
print('=' * 78)
if '--selftest' in sys.argv:
    selftest()

ch_raw = read(CH)
ch = strip_comments(ch_raw)
ch_sq = sq(ch)
icons = read(ICONS)
wndch_sq = sq(strip_comments(read(WNDCH)))
wndfun_sq = sq(strip_comments(read(WNDFUN)))
hss = read(HSS)
hss_sq = sq(strip_comments(hss))
menu = read(MENU)
menu_sq = sq(strip_comments(menu))
game_sq = sq(strip_comments(read(WNDGAME)))
gip_sq = sq(strip_comments(read(WNDGIP)))
rank = read(WNDRANK)
rank_sq = sq(strip_comments(rank))
rr = read(RANK)

print('\n【A. Challenges 判据层】')
chk("FUN_MASK = DEMOLITION_SQUAD | JELLY_PERSON | NARCISSUS_TRACING | DEBUG_MODE" in ch_sq,
    'FUN_MASK 恰为那四条（拆迁办|依旧果冻人|水仙追迹|调试模式）')
chk(re.search(r'MAX_CHALS\s*=\s*9\s*;', ch) is not None,
    'MAX_CHALS == 9（常规挑战数；实得 ' + str(re.search(r'MAX_CHALS\s*=\s*(\d+)', ch).group(1)) + '）')
chk(pred_isfun_uses_bitand(ch), 'isFun 用位与（同时吃单个位与合并位域）')
chk(pred_isregular_is_not_isfun(ch), 'isRegular == !isFun（唯一的分类判据）')
chk(pred_regularmasks_uses_isregular(ch), 'regularMasks() 按 isRegular 过滤')
chk(pred_funmasks_uses_isfun(ch), 'funMasks() 按 isFun 过滤')
rm = sq(code_body(ch, "public static int regularMask()"))
chk("isRegular( ch )" in rm and "m |= ch" in rm, 'regularMask() 并起全部常规位')
fm = sq(code_body(ch, "public static int funMask()"))
chk("return FUN_MASK;" in fm, 'funMask() 返回 FUN_MASK 本身（而非重算一遍）')
ni = sq(code_body(ch, "public static String nameId( int mask )"))
chk("MASKS[i] == mask" in ni and "return NAME_IDS[i];" in ni, 'nameId() 由位反查文本键（NAME_IDS/MASKS 同位）')
ac = sq(code_body(ch, "public static int activeChallenges(int mask)"))
chk("!isRegular(ch)" in ac and "continue;" in ac, 'activeChallenges(int) 只数常规挑战（其余 continue）')
af = sq(code_body(ch, "public static int activeFun(int mask)"))
chk("!isFun(ch)" in af and "continue;" in af, 'activeFun(int) 只数趣味挑战（其余 continue）')
chk(pred_scored_only_excludes_debug(ch),
    'scoredChallenges(int) 只排 DEBUG_MODE（保留「除调试模式外全算」的旧口径）')

masks, consts, fun_bits, regular = parse_masks(ch_raw)
name_inner = braced_after(ch, "public static final String[] NAME_IDS")
n_names = count_entries(name_inner)
print('        实测：MASKS %d 条 / NAME_IDS %d 条 / 趣味 %d 条 / 常规 %d 条'
      % (len(masks), n_names, len(fun_bits), len(regular)))
chk(len(masks) == n_names, 'MASKS 与 NAME_IDS 等长（否则下标配对会错位）')
chk(len(fun_bits) == 4 and fun_bits == {512, 1024, 2048, 4096},
    '趣味位恰为 512|1024|2048|4096 四条（实得 ' + str(sorted(fun_bits)) + '）')
chk(consts.get('MAX_CHALS') == len(regular),
    'MAX_CHALS 与实测常规条数一致（' + str(consts.get('MAX_CHALS')) + ' vs ' + str(len(regular)) + '）')
chk(consts.get('MAX_VALUE') == 8191, 'MAX_VALUE 仍为 8191')
chk(sum(regular) + sum(fun_bits) == consts.get('MAX_VALUE'),
    '常规位 ∪ 趣味位 == MAX_VALUE（互补无漏；' + str(sum(regular) + sum(fun_bits)) + '）')

print('\n【B. Icons 三件套与 rect】')
for name in ('FUN_GREY', 'FUN_COLOR', 'FUN_COUNT'):
    chk(re.search(r'^\s*' + name + r',\s*$', icons, re.M) is not None,
        '枚举常量 ' + name + ' 存在')
rects = parse_rects(icons)
chk(rects.get('FUN_GREY') == (128, 48, 14, 16), 'FUN_GREY rect == (128,48,14,16)')
chk(rects.get('FUN_COLOR') == (144, 48, 14, 16), 'FUN_COLOR rect == (144,48,14,16)')
chk(rects.get('FUN_COUNT') == (160, 48, 7, 7), 'FUN_COUNT rect == (160,48,7,7)')
chk('FUN_COUNT' in rects and rects['FUN_COUNT'][2:] == rects.get('CHAL_COUNT', (0, 0, 0, 0))[2:],
    'FUN_COUNT 与 CHAL_COUNT 同尺寸（MenuPane 横排计数的对齐前提）')

print('\n【C. WndChallenges：只列常规 + 写回保留趣味位】')
chk('for (int mask : Challenges.regularMasks())' in wndch_sq, '只遍历常规挑战（regularMasks）')
chk('Challenges.nameId( mask )' in wndch_sq, '名字走 nameId 反查（不再按下标配对）')
chk(pred_wndch_preserves_fun(read(WNDCH)), '写回只清常规位（& ~Challenges.regularMask()），趣味位保留')
chk('boxMasks.add( mask )' in wndch_sq and 'boxMasks.get( i )' in wndch_sq,
    'boxMasks 与 boxes 同序记录位掩码')
chk('Challenges.MASKS[' not in wndch_sq and 'Challenges.NAME_IDS[' not in wndch_sq,
    '已无 `Challenges.MASKS[i] / NAME_IDS[i]` 下标配对（防将来新增挑战时静默错位）')

print('\n【D. WndFunChallenges：新窗口】')
chk(os.path.isfile(WNDFUN), 'WndFunChallenges.java 已创建')
chk('public class WndFunChallenges extends Window' in wndfun_sq, '类声明正确（extends Window）')
chk('for (int mask : Challenges.funMasks())' in wndfun_sq, '只遍历趣味挑战（funMasks）')
chk('Challenges.nameId( mask )' in wndfun_sq, '名字走 nameId 反查')
chk(pred_wndfun_preserves_regular(read(WNDFUN)), '写回只清趣味位（& ~Challenges.funMask()），常规位保留')
chk('Messages.get(this, "title")' in wndfun_sq,
    '标题取本类文案（⇒ windows.wndfunchallenges.title）')
chk('Challenges.MASKS[' not in wndfun_sq and 'Challenges.NAME_IDS[' not in wndfun_sq,
    '无下标配对')

print('\n【E. HeroSelectScene：选中界面入口】')
chk('protected StyledButton funButton;' in hss_sq, 'funButton 字段已声明')
chk('import com.shatteredpixel.shatteredpixeldungeon.windows.WndFunChallenges;' in hss_sq,
    'import 已加')
chk('funButton = new StyledButton(Chrome.Type.BLANK, Messages.get(WndFunChallenges.class, "title"), 6)' in hss_sq,
    '按钮 = 挑战同款 StyledButton（套用挑战的排版）')
chk('new WndFunChallenges(SPDSettings.challenges(), true)' in hss_sq,
    '入口窗口可编辑（写的是 SPDSettings.challenges）')
chk(pred_scene_activefun_with_arg(hss), '图标判据用带参 activeFun(SPDSettings.challenges())，且无无参版')
chk(hss_sq.count('Icons.FUN_COLOR : Icons.FUN_GREY') >= 2,
    '初始图标与关窗回调都更新灰/彩两态（2 处）')
chk('buttons.add(funButton);' in hss_sq, '已加入按钮组（跟着 Challenges/Trials 一起排版）')

print('\n【F. MenuPane：局内右上角计数】')
chk(pred_menu_activefun_noarg(read(MENU)), '计数门控用无参 activeFun()（＝Dungeon.challenges）')
chk('Icons.get(Icons.FUN_COUNT)' in menu_sq, '用 FUN_COUNT 计数图标')
chk('new WndFunChallenges(Dungeon.challenges, false)' in menu_sq, '点开只读的趣味挑战窗')
chk('btnJournal.left() - 28' in menu_sq, '栏位偏移 -28（挑战 -14 / 考验 -21 / 趣味 -28）')
chk('if (funIcon != null)' in menu_sq, 'layout 里有 funIcon 的空值守卫（未开启时不排版）')
chk('import com.shatteredpixel.shatteredpixeldungeon.windows.WndFunChallenges;' in menu_sq,
    'import 已加')
for fdecl in ('private Image funIcon;', 'private BitmapText funText;', 'private Button funButton;'):
    chk(fdecl in menu_sq, '字段声明与挑战/考验同规格：' + fdecl)

print('\n【G. WndGame：局内菜单】')
chk('if (Challenges.activeFun(Dungeon.challenges) > 0)' in game_sq, '趣味挑战按钮的判据（带参）')
chk('new WndFunChallenges( Dungeon.challenges, false )' in game_sq, '点开只读窗')
chk('curBtn.icon(Icons.get(Icons.FUN_COLOR));' in game_sq, '按钮图标 FUN_COLOR')
chk('if (Challenges.activeChallenges(Dungeon.challenges) > 0)' in game_sq,
    '常规挑战按钮的判据**已跟着拆**（否则只开趣味挑战时会弹出一个全空的挑战窗）')
chk('if (Dungeon.challenges > 0)' not in game_sq, '旧的 `Dungeon.challenges > 0` 门控已退场')

print('\n【H. WndGameInProgress：存档槽】')
chk('final boolean hasChals = Challenges.activeChallenges(info.challenges) > 0;' in gip_sq,
    'hasChals 判据（常规）')
chk('final boolean hasFun = Challenges.activeFun(info.challenges) > 0;' in gip_sq,
    'hasFun 判据（趣味）')
chk('if (hasFun) GAP -= 2;' in gip_sq, 'hasFun 参与 GAP 收缩')
chk('if (hasFun) {' in gip_sq and 'new WndFunChallenges( info.challenges, false )' in gip_sq,
    '趣味挑战按钮（读同一个 info.challenges 位域）')
chk('if (hasChals) {' in gip_sq, '常规挑战按钮改用 hasChals')
chk('if (info.challenges > 0) {' not in gip_sq and 'if (info.challenges > 0) GAP -= 2;' not in gip_sq,
    '旧的 `info.challenges > 0` 门控已退场')

print('\n【I. WndRanking：记录页签】')
icons_inner = braced_after(strip_comments(rank), "Icons[] icons =")
pages_inner = braced_after(strip_comments(rank), "Group[] pages =")
n_ic, n_pg = count_entries(icons_inner), count_entries(pages_inner)
print('        icons %d 项 / pages %d 项' % (n_ic, n_pg))
chk(n_ic == 7 and n_pg == 7, 'icons 与 pages 都是 7 项（基础 4 + 挑战 + 考验 + 趣味）')
chk('Icons.FUN_COLOR' in (icons_inner or ''), '第 7 个页签图标是 FUN_COLOR')
chk(n_ic == n_pg, 'icons 与 pages 下标一一对应（否则会张冠李戴）')
chk(pred_rank_page4_guarded(rank), '挑战页判据已拆为 activeChallenges(Dungeon.challenges)')
chk(pred_rank_page6_guarded(rank), '趣味页有门控 activeFun(Dungeon.challenges)')
chk('private class FunChallengesTab extends Group' in rank_sq, 'FunChallengesTab 内部类存在')
chk('Challenges.funMasks()' in rank_sq and 'Challenges.regularMasks()' in rank_sq,
    '两个页签各自遍历自己那一类（funMasks / regularMasks）')
chk('Challenges.MASKS[' not in sq(strip_comments(rank)) and 'Challenges.NAME_IDS[' not in sq(strip_comments(rank)),
    '记录页也已无下标配对')
print('        行数：常规 %d 行占 %d px、趣味 %d 行占 %d px（上限 HEIGHT=144）'
      % (len(regular), 16 * len(regular) - 1, len(fun_bits), 16 * len(fun_bits) - 1))
chk(rows_fit(len(regular)), '常规挑战页 %d 行能塞进 HEIGHT=144（拆分后不再被相机裁掉）' % len(regular))
chk(rows_fit(len(fun_bits)), '趣味挑战页 %d 行能塞进 HEIGHT=144' % len(fun_bits))

print('\n【J. Rankings：得分倍率】')
chk(pred_rankings_scored(rr), '倍率走 scoredChallenges（趣味挑战仍计入倍率；调试/水仙仍单独置 0）')
chk('Dungeon.isChallenged(Challenges.DEBUG_MODE)' in sq(strip_comments(rr))
    and 'Dungeon.isChallenged(Challenges.NARCISSUS_TRACING)' in sq(strip_comments(rr)),
    '两条「不发分」的置 0 分支仍在')

print('\n【K. 文案键（zh / en）】')
wzh, wen = read(WZH), read(WEN)
keys = ['windows.wndfunchallenges.title', 'windows.wndgame.funchallenges',
        'windows.wndgameinprogress.funchallenges']
for k in keys:
    zline = [l for l in wzh.splitlines() if l.startswith(k + '=')]
    eline = [l for l in wen.splitlines() if l.startswith(k + '=')]
    chk(len(zline) == 1 and zline[0].split('=', 1)[1].strip() == '趣味挑战',
        'zh ' + k + ' == 趣味挑战')
    chk(len(eline) == 1 and eline[0].split('=', 1)[1].strip() != '',
        'en ' + k + ' 非空')

print('\n' + '=' * 78)
print('ALL PASS —— 趣味挑战分类已完整接线（%d 条断言）' % len(results) if ok
      else '!! 存在 FAIL，见上')
print('=' * 78)
sys.exit(0 if ok else 1)
