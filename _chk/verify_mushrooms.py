# -*- coding: utf-8 -*-
"""可食用蘑菇（移植自 Easily Sprouted PD 的 items/food 蘑菇组）核验脚本。

用法：
    python _chk/verify_mushrooms.py            # 跑正式断言
    python _chk/verify_mushrooms.py --selftest # 跑反例自测（证明判据不空转、不误报）

覆盖五层：
  ① ItemSpriteSheet 接线 —— 7 个常量必须是 xy(1..7, 45)、必须有 assignItemRect、
     **常量声明必须早于**它所属的 static{}（静态初始化按源码顺序 ⇒ 写在块后＝传 0，
     表现为所有物品图标整体错位且不报错）；
  ② 贴图包围盒 —— 真解 items.png，7 格必须「贴着格子左上角且包围盒恰好等于 assignItemRect」。
     这条是本脚本的核心：assignItemRect 只裁框、不看画，画偏了就会「留边」或「裁角」；
  ③ 物品类结构 —— 7 个类 extends MushroomFood、image 指向各自常量；
     基类 MushroomFood 的 Boss 层禁食守卫必须早于 super.execute、bones=false、
     饱食度用源公式 (STARVING-HUNGRY)/10；
  ④ buff —— BerryRegeneration type 必须是 POSITIVE（**标 NEGATIVE 会让全图怪立刻醒来**）、
     存读档带 regenleft、走 Char.heal() 而不是直改 HP、
     且**没有**在 BuffIndicator 里新开帧常量（本项目惯例：没自绘图标的 buff 一律借原版帧）；
  ⑤ 文本 —— zh/en 双向成对、非空、无 U+FFFD、`_` 与 `~~` 标记成对、buff desc 带 %d。

⚠️ 剥注释一律用「保偏移单趟状态机」而不是正则去 /*...*/：
本仓注释里大量使用 //**强调**，其子串 /* 会被当成块注释开头、一路吞到文件末尾。
"""
import os
import re
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from verify_sprite_frames import decode_png, pixel  # noqa: E402

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
PKG = 'com/shatteredpixel/shatteredpixeldungeon'
SRC = os.path.join(ROOT, 'core/src/main/java', PKG)
ITEMS_PNG = os.path.join(ROOT, 'core/src/main/assets/sprites/items.png')
MSG_ITEMS = os.path.join(ROOT, 'core/src/main/assets/messages/items')
MSG_ACTORS = os.path.join(ROOT, 'core/src/main/assets/messages/actors')
FILM = os.path.join(SRC, 'sprites/ItemSpriteSheet.java')

ROW = 45
HLINE = 10          # 移植自 Easily Sprouted PD，2026-09-25

# (常量名, 第几列, 类名)
MUSHROOMS = [
    ('MUSHROOM_LANTERN',       1, 'JackOLantern'),
    ('MUSHROOM_EARTHSTAR',     2, 'Earthstar'),
    ('MUSHROOM_LICHEN',        3, 'LichenMushroom'),
    ('MUSHROOM_DEATHCAP',      4, 'DeathCap'),
    ('MUSHROOM_BLUEMILK',      5, 'BlueMilk'),
    ('MUSHROOM_GOLDENJELLY',   6, 'GoldenJelly'),
    ('MUSHROOM_PIXIEPARASOL',  7, 'PixieParasol'),
]


# --------------------------------------------------------------------------
# 工具：保偏移剥注释（单趟左到右状态机）
# --------------------------------------------------------------------------
def strip_comments(text):
    out = []
    i, n = 0, len(text)
    state = None       # None / 'line' / 'block' / 'str' / 'chr'
    while i < n:
        c = text[i]
        nxt = text[i + 1] if i + 1 < n else ''
        if state is None:
            if c == '/' and nxt == '/':
                state = 'line'
                out.append('  ')
                i += 2
                continue
            if c == '/' and nxt == '*':
                state = 'block'
                out.append('  ')
                i += 2
                continue
            if c == '"':
                state = 'str'
            elif c == "'":
                state = 'chr'
            out.append(c)
            i += 1
        elif state == 'line':
            if c == '\n':
                state = None
                out.append(c)
            else:
                out.append(' ')
            i += 1
        elif state == 'block':
            if c == '*' and nxt == '/':
                state = None
                out.append('  ')
                i += 2
                continue
            out.append('\n' if c == '\n' else ' ')
            i += 1
        elif state in ('str', 'chr'):
            if c == '\\' and i + 1 < n:
                out.append(c)
                out.append(nxt)
                i += 2
                continue
            if (state == 'str' and c == '"') or (state == 'chr' and c == "'"):
                state = None
            out.append(c)
            i += 1
    return ''.join(out)


def read(path):
    with open(path, encoding='utf-8') as f:
        return f.read()


def decl_before_rect(film_text, name):
    """常量声明是否早于它那条 assignItemRect（静态初始化按源码顺序 ⇒ 反了就是传 0）。"""
    m = re.search(r'public\s+static\s+final\s+int\s+%s\s*=\s*xy\(' % name, film_text)
    r = re.search(r'assignItemRect\(\s*%s\s*,' % name, film_text)
    if m is None or r is None:
        return False
    return m.start() < r.start()


def cell_bbox(path, col, row, size=16):
    """返回 (x1, y1, w, h)，全部按「格内相对坐标」计；全透明返回 None。"""
    w, h, ct, nch, plte, trns, px = decode_png(path)
    ox, oy = (col - 1) * size, (row - 1) * size
    x1, y1, x2, y2 = None, None, None, None
    for y in range(oy, oy + size):
        for x in range(ox, ox + size):
            if pixel(px, ct, nch, plte, trns, x, y, w)[3] > 0:
                if x1 is None or x < x1:
                    x1 = x
                if y1 is None or y < y1:
                    y1 = y
                if x2 is None or x > x2:
                    x2 = x
                if y2 is None or y > y2:
                    y2 = y
    if x1 is None:
        return None
    return x1 - ox, y1 - oy, x2 - x1 + 1, y2 - y1 + 1


def load_props(path):
    out = {}
    with open(path, encoding='utf-8') as f:
        for raw in f:
            line = raw.rstrip('\n')
            if not line or line.lstrip().startswith('#') or '=' not in line:
                continue
            k, v = line.split('=', 1)
            out[k.strip()] = v
    return out


def text_ok(v):
    """一条 .properties 文本值是否健康：非空 / 无 U+FFFD / 行内标记成对 / 无裸 %。

    裸 % 的判据必须先把 %% 摘掉 —— 否则 '100%%' 里的第二个 % 会被误判成非法转换
    （2026-09-25 被本脚本 --selftest 抓出来的真 bug）。
    """
    if not v:
        return False
    if '\ufffd' in v:
        return False
    if v.count('_') % 2 != 0:          # 行内强调标记必须成对
        return False
    if v.count('~~') % 2 != 0:         # 删除线标记必须成对
        return False
    if re.search(r'%(?![bBhHsScCdoxXeEfgGaAnt%])', v.replace('%%', '')):
        return False
    return True


# --------------------------------------------------------------------------
# 判据收集
# --------------------------------------------------------------------------
def collect():
    """返回 [(说明, 是否通过, 反例文案)]；反例文案保持惰性（lambda）。"""
    res = []

    def check(cond, msg, counter=None):
        res.append((msg, bool(cond), counter))

    film_raw = read(FILM)
    film = strip_comments(film_raw)

    # ---- ① 常量与 assignItemRect ----------------------------------------
    decl_pos, rect_pos, rect_val = {}, {}, {}
    for name, col, _cls in MUSHROOMS:
        m = re.search(r'public\s+static\s+final\s+int\s+%s\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)' % name, film)
        check(m is not None, '① %s 常量存在且形如 xy(c, r)' % name,
              lambda n=name: 'File** 里找不到 %s 的 xy 声明' % n)
        if m:
            decl_pos[name] = m.start()

        r = re.search(r'assignItemRect\(\s*%s\s*,\s*(\d+)\s*,\s*(\d+)\s*\)' % name, film)
        check(r is not None, '① %s 有 assignItemRect' % name,
              lambda n=name: '%s 没写 assignItemRect ⇒ 会按 16×16 整格取图，图标偏位' % n)
        if r:
            rect_val[name] = (int(r.group(1)), int(r.group(2)))
            rect_pos[name] = r.start()

        if m and r:
            check(m.group(1) == str(col) and m.group(2) == str(ROW),
                  '① %s 必须落在 xy(%d, %d)' % (name, col, ROW),
                  lambda n=name, g=m: '%s 实际落在 xy(%s, %s)' % (n, g.group(1), g.group(2)))
            check(decl_before_rect(film, name),
                  '① %s 的常量声明早于它的 assignItemRect（静态初始化顺序）' % name,
                  lambda n=name: '%s 的常量写在 static{} 之后 ⇒ 传 0 ⇒ 全表图标错位' % n)

    # 只有「顶格（1 个 tab）」的常量算 items.png 的地址，避免撞进内嵌类 Icons 的 xy
    row45 = re.findall(r'^\tpublic\s+static\s+final\s+int\s+(\w+)\s*=\s*xy\(\s*\d+\s*,\s*%d\s*\)' % ROW,
                       film, re.M)
    check(sorted(row45) == sorted(n for n, _c, _k in MUSHROOMS),
          '① items.png 第 %d 行只有这 7 个顶层常量（无撞格/无遗漏）' % ROW,
          lambda: '第 %d 行实际占用=%s，期望=%s' % (
              ROW, sorted(row45), sorted(n for n, _c, _k in MUSHROOMS)))

    # ---- ② 贴图包围盒 ---------------------------------------------------
    for name, col, _cls in MUSHROOMS:
        bb = cell_bbox(ITEMS_PNG, col, ROW)
        check(bb is not None, '② %s 对应格子非空' % name,
              lambda c=col: 'items.png 第 %d 行第 %d 格全透明（用户说画好了但没落盘/画错格）' % (ROW, c))
        if bb is None:
            continue
        x1, y1, bw, bh = bb
        check(x1 == 0 and y1 == 0,
              '② %s 贴图贴着格子左上角（assignItemRect 从格左上起裁）' % name,
              lambda n=name, b=bb: '%s 包围盒起点=(%d,%d)，不贴左上角 ⇒ 会被裁掉左/上边' % (n, b[0], b[1]))
        if name in rect_val:
            check((bw, bh) == rect_val[name],
                  '② %s 的 assignItemRect 等于实测包围盒 %d×%d' % (name, bw, bh),
                  lambda n=name, b=(bw, bh), r=rect_val[name]: '%s 实测 %s×%s 而 rect 是 %s×%s' % (
                      n, b[0], b[1], r[0], r[1]))

    # ---- ③ 物品类结构 ---------------------------------------------------
    base = read(os.path.join(SRC, 'items/food/MushroomFood.java'))
    base_code = strip_comments(base)

    check('extends Food' in base_code and 'abstract class MushroomFood' in base_code,
          '③ MushroomFood 是 extends Food 的抽象基类')
    check('bones = false;' in base_code, '③ 基类 bones=false（源仓库同款）')
    check(re.search(r'energy\s*=\s*\(\s*Hunger\.STARVING\s*-\s*Hunger\.HUNGRY\s*\)\s*/\s*10f?\s*;', base_code) is not None,
          '③ 基类饱食度沿用源公式 (Hunger.STARVING - Hunger.HUNGRY)/10')
    check('super.execute( hero, action )' in base_code, '③ 基类仍调用 super.execute')
    i_guard = base_code.find('Dungeon.bossLevel()')
    i_super = base_code.find('super.execute')
    check(i_guard != -1 and i_super != -1 and i_guard < i_super,
          '③ Boss 层禁食守卫在 super.execute 之前（否则蘑菇照常被吃掉）',
          lambda: '守卫 idx=%s，super.execute idx=%s' % (i_guard, i_super))
    check('eatEffect' in base_code and 'showEffect' in base_code,
          '③ 基类留出 eatEffect / showEffect 两个覆写点')

    effects = {
        'JackOLantern':  ['Fire.class', 'Blob.seed'],
        'Earthstar':     ['Bleeding.class', 'Cripple.class'],
        'DeathCap':      ['Blindness.class', 'aggro'],
        'BlueMilk':      ['Haste.class', 'Slow.class', 'BerryRegeneration.class'],
        'GoldenJelly':   ['Roots.class', 'Vertigo.class'],
        'PixieParasol':  ['Drowsy.class', 'Paralysis.class', 'Speck.NOTE', 'BerryRegeneration.class'],
    }
    for name, col, cls in MUSHROOMS:
        p = os.path.join(SRC, 'items/food/%s.java' % cls)
        exists = os.path.exists(p)
        check(exists, '③ %s.java 存在' % cls)
        if not exists:
            continue
        code = strip_comments(read(p))
        check(re.search(r'class\s+%s\s+extends\s+MushroomFood' % cls, code) is not None,
              '③ %s extends MushroomFood' % cls)
        check('image = ItemSpriteSheet.%s' % name in code,
              '③ %s 的 image 指向 %s' % (cls, name),
              lambda c=cls, n=name: '%s 的 image 不是 %s' % (c, n))
        for needle in effects.get(cls, []):
            check(needle in code, '③ %s 的效果里含 %s' % (cls, needle),
                  lambda c=cls, nd=needle: '%s 缺 %s ⇒ 效果没移植全' % (c, nd))
        # 敌对判定必须用 alignment（本作没有 Mob.hostile 字段）
        if cls != 'LichenMushroom':
            check('mob.hostile' not in code, '③ %s 不用 Mob.hostile（本作无该字段）' % cls,
                  lambda c=cls: '%s 用了不存在的 mob.hostile' % c)

    # 占位蘑菇：不得有实际效果
    lichen = os.path.join(SRC, 'items/food/LichenMushroom.java')
    if os.path.exists(lichen):
        lc = strip_comments(read(lichen))
        check('eatEffect' not in lc.replace('eatEffect(hero)', ''),
              '③ 地衣蘑菇不覆写 eatEffect（占位物品、无实际效果）',
              lambda: 'LichenMushroom 覆写了 eatEffect ⇒ 与「占位无效果」的口径不符')

    # ---- ④ buff ---------------------------------------------------------
    buff = read(os.path.join(SRC, 'actors/buffs/BerryRegeneration.java'))
    buff_code = strip_comments(buff)
    check('extends Buff' in buff_code, '④ BerryRegeneration extends Buff')
    check('type = buffType.POSITIVE' in buff_code, '④ buff type 标 POSITIVE')
    check('buffType.NEGATIVE' not in buff_code,
          '④ buff 不标 NEGATIVE（标 NEGATIVE 会让全图怪立刻醒来）',
          lambda: 'BerryRegeneration 里出现了 buffType.NEGATIVE')
    check('REGENLEFT' in buff_code and 'bundle.put' in buff_code and 'bundle.getInt' in buff_code,
          '④ buff 存/读档带 regenleft')
    check('target.heal(' in buff_code, '④ 回血走 Char.heal()（受禁疗等钩子约束）')
    check(re.search(r'HP\s*\+=' , buff_code) is None,
          '④ buff 不直写 HP +=（绕过 heal 收口）',
          lambda: 'BerryRegeneration 里有裸 HP += ⇒ 绕过 HealBlock')
    check('1 + regenleft / 25' in buff_code,
          '④ 回复量沿用源公式 1 + regenleft/25（整除）',
          lambda: '不见了源公式 1 + regenleft / 25')

    bi = read(os.path.join(SRC, 'ui/BuffIndicator.java'))
    check('BERRY_REGENERATION' not in bi and 'BERRYREGEN' not in bi,
          '④ 未在 BuffIndicator 新开帧常量（惯例：没自绘图标的 buff 借原版帧）',
          lambda: 'BuffIndicator 里为 BerryRegeneration 新开了帧常量')

    # ---- ④b 调试窗接线 ---------------------------------------------------
    # 调试窗「杂项」页签只浅扫 items 根包，够不到 items.food；7 种蘑菇必须显式挂在 extras 里，
    # 否则「游戏里根本看不到这批蘑菇」，而编译与贴图全绿、不报任何错。
    dbg = strip_comments(read(os.path.join(SRC, 'windows/WndDebug.java')))
    m_dbg = re.search(r'miscTab\s*=\s*new\s+ItemTab\(\s*Item\.class\s*,\s*PKG_ITEMS_ROOT\s*,\s*true\s*,(.*?)\)\s*;',
                      dbg, re.S)
    check(m_dbg is not None, '④b 调试窗 miscTab 带 extras 列表',
          lambda: 'WndDebug 的 miscTab 仍是三参构造 ⇒ 7 种蘑菇在游戏里无法生成')
    if m_dbg:
        extras = m_dbg.group(1)
        for _name, _col, cls in MUSHROOMS:
            check(('%s.class' % cls) in extras,
                  '④b 调试窗 extras 含 %s' % cls,
                  lambda c=cls: 'WndDebug 的 miscTab extras 缺 %s' % c)

    # ---- ⑤ 文本 ---------------------------------------------------------
    zh_items = load_props(os.path.join(MSG_ITEMS, 'items_zh.properties'))
    en_items = load_props(os.path.join(MSG_ITEMS, 'items.properties'))
    zh_act = load_props(os.path.join(MSG_ACTORS, 'actors_zh.properties'))
    en_act = load_props(os.path.join(MSG_ACTORS, 'actors.properties'))

    keys = ['items.food.mushroomfood.eat_msg', 'items.food.mushroomfood.prevent']
    for name, _col, cls in MUSHROOMS:
        low = cls.lower()
        keys += ['items.food.%s.name' % low, 'items.food.%s.desc' % low, 'items.food.%s.effect' % low]

    for k in keys:
        check(k in zh_items and text_ok(zh_items.get(k, '')), '⑤ zh 有非空且格式合法的 %s' % k,
              lambda kk=k: 'items_zh.properties 缺/坏 %s' % kk)
        check(k in en_items and text_ok(en_items.get(k, '')), '⑤ en 有非空且格式合法的 %s' % k,
              lambda kk=k: 'items.properties 缺/坏 %s' % kk)

    # 源文本「嚼~~嚼~~」里的 ~~ 在本作是删除线标记，必须已改写
    for props, lang in ((zh_items, 'zh'), (en_items, 'en')):
        v = props.get('items.food.mushroomfood.eat_msg', '')
        check('~~' not in v,
              '⑤ %s 的 eat_msg 不含 ~~（本作 ~~ 是删除线标记）' % lang,
              lambda l=lang: '%s 的 eat_msg 仍含 ~~（源文本直搬）' % l)

    for k in ('actors.buffs.berryregeneration.name', 'actors.buffs.berryregeneration.desc'):
        check(k in zh_act and text_ok(zh_act.get(k, '')), '⑤ zh 有非空且格式合法的 %s' % k,
              lambda kk=k: 'actors_zh.properties 缺/坏 %s' % kk)
        check(k in en_act and text_ok(en_act.get(k, '')), '⑤ en 有非空且格式合法的 %s' % k,
              lambda kk=k: 'actors.properties 缺/坏 %s' % kk)

    check('%d' in zh_act.get('actors.buffs.berryregeneration.desc', ''),
          '⑤ buff desc(z h) 带 %d 占位符（实参是剩余回合数）')
    check('%d' in en_act.get('actors.buffs.berryregeneration.desc', ''),
          '⑤ buff desc(en) 带 %d 占位符')

    return res


# --------------------------------------------------------------------------
# 反例自测
# --------------------------------------------------------------------------
def selftest():
    fails = []
    n = 0

    def expect(name, cond):
        nonlocal n
        n += 1
        if not cond:
            fails.append(name)

    # --- strip_comments: //**强调** 不许被当成块注释开头吞到文件尾 ---
    src = 'int a = 1; //**强调**在这里\nint b = 2;\nint c = 3;\n'
    out = strip_comments(src)
    expect('strip_comments 保住 //** 之后的行', 'int b = 2;' in out and 'int c = 3;' in out)
    expect('strip_comments 输出长度不变（保偏移）', len(out) == len(src))
    expect('strip_comments 吃掉注释内容', '强调' not in out)
    src2 = 'String s = "// 不是注释";\nint d = 4;\n'
    out2 = strip_comments(src2)
    expect('字符串里的 // 不是注释', '// 不是注释' in out2 and 'int d = 4;' in out2)

    # --- 真跑一遍正式判据，确认全绿（正例） ---
    res = collect()
    expect('正式判据全绿', all(ok for _m, ok, _c in res))

    # --- 反例：常量写在 static{} 之后必须被抓到 ---
    decl = 'public static final int MUSHROOM_LANTERN      =                          xy(1, 45);'
    rect = 'assignItemRect(MUSHROOM_LANTERN, 12, 13);'
    good = decl + '\n' + rect + '\n'
    bad = rect + '\n' + decl + '\n'
    expect('顺序判据：正确顺序判 True', decl_before_rect(good, 'MUSHROOM_LANTERN') is True)
    expect('顺序判据：颠倒顺序判 False（反例抓得住）',
           decl_before_rect(bad, 'MUSHROOM_LANTERN') is False)
    expect('顺序判据：缺 assignItemRect 判 False',
           decl_before_rect(decl + '\n', 'MUSHROOM_LANTERN') is False)
    expect('顺序判据：缺常量声明判 False',
           decl_before_rect(rect + '\n', 'MUSHROOM_LANTERN') is False)
    # 真文件里这条必须真为 True（证明正例没被上面的合成用例带偏）
    film = strip_comments(read(FILM))
    expect('真文件里灯笼蘑菇顺序正确', decl_before_rect(film, 'MUSHROOM_LANTERN') is True)

    # --- 反例：rect 与包围盒不符必须 FAIL ---
    bb1 = cell_bbox(ITEMS_PNG, 1, ROW)
    bb7 = cell_bbox(ITEMS_PNG, 7, ROW)
    expect('包围盒读取正常（灯笼与太阳伞都非空）', bb1 is not None and bb7 is not None)
    if bb1 and bb7:
        expect('反例：错误的 rect(16,16) 与灯笼实测不符', (bb1[2], bb1[3]) != (16, 16))
        expect('反例：拿第 7 格的包围盒去比第 1 格必须不符',
               (bb1[2], bb1[3]) != (bb7[2], bb7[3]))
        # 与正式判据同款：rect 值必须来自真文件
        m_rect = re.search(r'assignItemRect\(\s*MUSHROOM_LANTERN\s*,\s*(\d+)\s*,\s*(\d+)\s*\)', film)
        expect('反例：真文件里灯笼的 rect 确实等于实测包围盒',
               m_rect is not None and (int(m_rect.group(1)), int(m_rect.group(2))) == (bb1[2], bb1[3]))

    # --- 反例：文本判据不空转 ---
    expect('反例：空串判不合法', text_ok('') is False)
    expect('反例：单个 _ 判不合法', text_ok('只有_一个下划线') is False)
    expect('反例：成对 _ 判合法', text_ok('_强调_ 正常') is True)
    expect('反例：U+FFFD 判不合法', text_ok('坏了\ufffd') is False)
    expect('反例：裸 % 判不合法', text_ok('100% 伤害') is False)
    expect('反例：%% 判合法', text_ok('100%% 伤害') is True)
    expect('反例：%d 判合法', text_ok('剩余 %d 回合') is True)
    expect('反例：%d%% 组合判合法', text_ok('剩余 %d%% 充能') is True)
    expect('反例：奇数个 ~~ 判不合法', text_ok('~~删除线没闭合') is False)
    expect('反例：成对 ~~ 判合法（但蘑菇文本里仍应避免）', text_ok('~~划掉~~') is True)

    # --- 反例：贴图全透明必须 FAIL ---
    expect('反例：空格子的包围盒是 None', cell_bbox(ITEMS_PNG, 8, ROW) is None)

    total = n
    print('反例自测：%d 条，失败 %d 条' % (total, len(fails)))
    for f in fails:
        print('  FAIL  %s' % f)
    return 1 if fails else 0


def main():
    if '--selftest' in sys.argv:
        return selftest()

    res = collect()
    bad = 0
    for msg, ok, counter in res:
        if not ok:
            bad += 1
            print('FAIL  %s' % msg)
            if counter is not None:
                print('      ↳ %s' % (counter() if callable(counter) else counter))
    print('断言 %d 条，失败 %d 条' % (len(res), bad))
    print('ALL PASS' if bad == 0 else 'FAILED')
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
