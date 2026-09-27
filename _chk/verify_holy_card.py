# -*- coding: utf-8 -*-
# 核验「神圣卡」与「神圣屏障」（HolyBarrier）的落地。
#
# 这条需求有两句最容易写错的话：
#   ① 「使用花费一回合，获得『神圣屏障』buff」—— 一回合 + 一次性无敌 + 持续无限 + 不可叠加；
#   ② 「可以响应敌人攻击、陷阱伤害，但不响应角色自身buff的伤害」—— **判据的边界**。
#
# ② 是本次实现的核心风险点：它决定了「哪些伤害会被屏障吃掉」。本作把这条边界收在
# HolyBarrier.blocks(Char, Object) 里（黑名单式：除「自身 buff 的 DOT」与「自伤换成长」外，
# 一切落到血量上的伤害都算一次），所以核验必须钉住：
#   · blocks() 里那两条排除**存在且先于 return true**；
#   · 全仓 `HolyBarrier.blocks(` **恰好 1 个调用点**；
#   · 调用点在 Hero.damage 靠前处（在 super.damage **之前**、在 flinch/interrupt **之前**）
#     —— 位置错了就变成「挡了血，但照样被打断、照样给神器充能」。
#
# 分四层：
#   ① 结构层：道具 / buff / 判据 / 特效 / 接线（图集常量、图标帧、配方、指南页、文本键）
#   ② 位置层：Hero.damage 的插入点顺序（比位置，剥注释）、blocks() 内部顺序、execute() 流程顺序
#   ③ 贴图层：真解 PNG —— items.png 第 41 行 col 8 必须恰是 15×15；large_buffs/buffs 帧 117 非空
#   ④ 反例自测（--selftest）：把判据改坏，必须报 FAIL
#
# 用法：
#   python _chk/verify_holy_card.py
#   python _chk/verify_holy_card.py --selftest
import os
import re
import struct
import sys
import zlib

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages')
ASSETS = os.path.join(ROOT, 'core/src/main/assets')

ok = True
_selftest_fails = 0


def chk(cond, msg):
    global ok
    print(('  [OK]   ' if cond else '  [FAIL] ') + msg)
    if not cond:
        ok = False


def read(rel):
    return open(os.path.join(SRC, rel), encoding='utf-8').read()


def read_msg(rel):
    return open(os.path.join(MSG, rel), encoding='utf-8').read()


def strip_comments(src):
    """把注释替换成等长空格（保留换行）——**保留字符偏移**，所以仍能「比位置」。"""
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


def order_ok(src, anchors):
    """anchors 必须按给定顺序出现（各取首次）。返回 (bool, 首个逆序对)。"""
    last, last_a = -1, None
    for a in anchors:
        p = src.find(a)
        if p < 0:
            return False, ('缺失', a)
        if p < last:
            return False, (last_a, a)
        last, last_a = p, a
    return True, None


def body_of(src, sig_regex):
    """取某方法签名的函数体（从 { 后到下一个列 0 的 '}'）。找不到返回 None。"""
    m = re.search(sig_regex, src)
    if not m:
        return None
    b = src[m.end():]
    k = b.find('\n\t}')
    return b[:k if k > 0 else len(b)]


# ================================================================
# PNG 解码（只读）
# ================================================================
def load_png(path):
    d = open(path, 'rb').read()
    assert d[:8] == b'\x89PNG\r\n\x1a\n', 'not png: ' + path
    pos, idat, w, h, ctype = 8, b'', 0, 0, 6
    while pos < len(d):
        ln = struct.unpack('>I', d[pos:pos + 4])[0]
        typ = d[pos + 4:pos + 8]
        data = d[pos + 8:pos + 8 + ln]
        if typ == b'IHDR':
            w, h, _, ctype, _, _, _ = struct.unpack('>IIBBBBB', data)
        elif typ == b'IDAT':
            idat += data
        elif typ == b'IEND':
            break
        pos += 12 + ln
    nch = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}[ctype]
    raw = zlib.decompress(idat)
    stride = w * nch
    out = bytearray(h * stride)
    prev = bytearray(stride)
    p = 0
    for y in range(h):
        f = raw[p]; p += 1
        line = bytearray(raw[p:p + stride]); p += stride
        if f == 1:
            for i in range(nch, stride):
                line[i] = (line[i] + line[i - nch]) & 255
        elif f == 2:
            for i in range(stride):
                line[i] = (line[i] + prev[i]) & 255
        elif f == 3:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                line[i] = (line[i] + ((a + prev[i]) >> 1)) & 255
        elif f == 4:
            for i in range(stride):
                a = line[i - nch] if i >= nch else 0
                b = prev[i]
                c = prev[i - nch] if i >= nch else 0
                pa, pb, pc = abs(b - c), abs(a - c), abs(a + b - 2 * c)
                pr = a if (pa <= pb and pa <= pc) else (b if pb <= pc else c)
                line[i] = (line[i] + pr) & 255
        out[y * stride:(y + 1) * stride] = line
        prev = line
    return out, w, h, nch


def cell_stats(buf, W, nch, x0, y0, cw, ch):
    """返回 (不透明像素数, 包围盒宽, 包围盒高, 像素元组)"""
    opaque, xs, ys, px = 0, [], [], []
    for y in range(y0, y0 + ch):
        for x in range(x0, x0 + cw):
            o = (y * W + x) * nch
            if buf[o + 3] > 0:
                opaque += 1
                xs.append(x - x0); ys.append(y - y0)
                px.append((x - x0, y - y0, buf[o], buf[o + 1], buf[o + 2], buf[o + 3]))
    if not xs:
        return 0, 0, 0, ()
    return opaque, max(xs) - min(xs) + 1, max(ys) - min(ys) + 1, tuple(px)


# ================================================================
# 读取源码 / 文本
# ================================================================
HC = read_j('items/HolyCard.java')
HB = read_j('actors/buffs/HolyBarrier.java')
HERO = read_j('actors/hero/Hero.java')
BI = read_j('ui/BuffIndicator.java')
ISS = read_j('sprites/ItemSpriteSheet.java')
RECIPE = read_j('items/Recipe.java')
QR = read_j('ui/QuickRecipe.java')
CHG = read('ui/changelist/EGOPD_Changes.java')

print('=' * 78)
print('① 结构层：神圣卡 / 神圣屏障 / 判据 / 接线')
print('=' * 78)

# —— 道具本体
chk(re.search(r'class HolyCard\s+extends Item', HC) is not None, 'class HolyCard extends Item')
chk(re.search(r'image\s*=\s*ItemSpriteSheet\.HOLY_CARD\s*;', HC) is not None,
    'image = ItemSpriteSheet.HOLY_CARD')
chk(re.search(r'stackable\s*=\s*true\s*;', HC) is not None, '可叠加（stackable = true）')
chk(re.search(r'defaultAction\s*=\s*AC_USE\s*;', HC) is not None, 'defaultAction = AC_USE（默认动作＝使用）')
chk(re.search(r'String AC_USE\s*=\s*"USE"\s*;', HC) is not None, 'AC_USE = "USE"')
chk(re.search(r'TIME_TO_USE\s*=\s*1f\s*;', HC) is not None, 'TIME_TO_USE = 1f（花费一回合）')
chk(re.search(r'ENERGY_COST\s*=\s*13\s*;', HC) is not None, 'ENERGY_COST = 13（炼金能量）')
chk(re.search(r'actions\.add\(\s*AC_USE\s*\)', HC) is not None, 'actions() 里加入 AC_USE')
chk(re.search(r'public boolean isUpgradable\(\)\s*\{\s*return false;\s*\}', HC) is not None,
    '不可升级')
chk(re.search(r'public boolean isIdentified\(\)\s*\{\s*return true;\s*\}', HC) is not None,
    '恒已识别（炼金产物）')

_ex = body_of(HC, r'public void execute\(\s*Hero hero\s*,\s*String action\s*\)\s*\{')
chk(_ex is not None, 'HolyCard.execute(Hero, String) 存在')
if _ex:
    o, bad = order_ok(_ex, [
        'super.execute( hero, action )',
        'action.equals( AC_USE )',
        '!hero.isAlive()',
        'HolyBarrier.active( hero )',                # 不可叠加：已有屏障 → 拒绝
        'Messages.get( this, "already" )',
        'Buff.affect( hero, HolyBarrier.class )',    # 挂 buff
        'detach( hero.belongings.backpack )',        # 消耗卡
        'hero.spendAndNext( TIME_TO_USE )',          # 花一回合
    ])
    chk(o, 'execute() 流程顺序：判定 → 拒绝 → 挂 buff → 扣卡 → 花一回合（首个逆序：%s）' % (bad,))
    # 「不可叠加」必须在 Buff.affect / 扣卡 / 花回合之前 return
    i_already = _ex.find('HolyBarrier.active( hero )')
    i_affect = _ex.find('Buff.affect( hero, HolyBarrier.class )')
    i_spend = _ex.find('hero.spendAndNext( TIME_TO_USE )')
    i_ret_at = _ex.find('return;', i_already)
    chk(0 <= i_already < i_ret_at < i_affect and i_already < i_spend and i_ret_at < i_spend,
        '已有屏障时「不消耗回合、也不消耗卡」就 return（不可叠加 ≠ 白扔一张卡）')

# —— 配方
_cr = HC[HC.find('class CraftRecipe'):] if 'class CraftRecipe' in HC else ''
chk(_cr != '', '内部类 CraftRecipe 存在')
o, bad = order_ok(_cr, [
    'ingredients.size() == 1',
    'instanceof Scroll',
    'return ENERGY_COST;',
])
chk(o, 'CraftRecipe：只吃 1 件 + 判 instanceof Scroll + cost = ENERGY_COST（首个逆序：%s）' % (bad,))
chk(re.search(r'return new HolyCard\(\)\s*;', _cr) is not None, 'brew/sampleOutput 产出 new HolyCard()')
chk('SimpleRecipe' not in _cr,
    '配方**不**继承 SimpleRecipe（它按 getClass() 精确匹配，表达不了「任意卷轴」）')

# —— buff 本体
chk(re.search(r'class HolyBarrier\s+extends Buff', HB) is not None, 'class HolyBarrier extends Buff')
chk(re.search(r'type\s*=\s*buffType\.POSITIVE\s*;', HB) is not None,
    'type = POSITIVE（增益；不是 NEGATIVE，避开「宝箱怪自动暴露」那条红线）')
chk(re.search(r'announced\s*=\s*true\s*;', HB) is not None, 'announced = true（获得时飘名字）')
chk(re.search(r'BuffIndicator\.HOLY_BARRIER', HB) is not None, 'icon() 返回 BuffIndicator.HOLY_BARRIER')
chk(re.search(r'public boolean act\(\)', HB) is None,
    '不重写 act()（无持续时间、不会自己掉，靠 blocks() 消耗）')

# —— 判据 blocks()：两条排除必须存在，且先于 return true
_bl = body_of(HB, r'public static boolean blocks\(\s*Char ch\s*,\s*Object src\s*\)\s*\{')
chk(_bl is not None, 'HolyBarrier.blocks(Char, Object) 存在（唯一判据）')
if _bl:
    o, bad = order_ok(_bl, [
        'if (!active( ch )) return false;',
        'if (src instanceof Buff) return false;',
        'if (src instanceof SelfHarmCost) return false;',
        'return true;',
    ])
    chk(o, 'blocks() 顺序：无屏障早退 → 排除自身 buff 伤害 → 排除自伤换成长 → 挡（首个逆序：%s）' % (bad,))
    chk('instanceof HolyBarrier' not in _bl.replace('ch.buff( HolyBarrier.class )', ''),
        'blocks() 里没有第二套「哪些 buff 是 DOT」的 instanceof 名单（只按 Buff 整类排除）')

# —— 消耗 + 特效
_tr = body_of(HB, r'public static void trigger\(\s*Char ch\s*\)\s*\{')
chk(_tr is not None, 'HolyBarrier.trigger(Char) 存在（消耗 + 特效）')
if _tr:
    o, bad = order_ok(_tr, [
        'Buff.detach( ch, HolyBarrier.class )',
        'showStatus(',
        'WhiteSmokeParticle.BURST',
    ])
    chk(o, 'trigger()：先把屏障 detach 掉，再播浮字 / 白烟（首个逆序：%s）' % (bad,))
    chk('Sample.INSTANCE.play(' in _tr, 'trigger() 里播了音效')

# —— 接线：图标帧 117
chk(re.search(r'HOLY_BARRIER\s*=\s*117\s*;', BI) is not None,
    'BuffIndicator.HOLY_BARRIER = 117（「融化」116 的下一位）')
chk(re.search(r'MELTING\s*=\s*116\s*;', BI) is not None, 'BuffIndicator.MELTING = 116 未被动过')
_m_b = BI.find('MELTING       = 116')
_h_b = BI.find('HOLY_BARRIER  = 117')
chk(0 <= _m_b < _h_b, 'HOLY_BARRIER 的声明位置在 MELTING 之后（注释里那句「下一位」名副其实）')

# —— 接线：图集常量 + 矩形
_m = re.search(r'HOLY_CARD\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)\s*;', ISS)
chk(_m is not None and _m.group(1) == '8' and _m.group(2) == '41',
    'HOLY_CARD = xy(8, 41)（与用户给定一致）')
chk(re.search(r'assignItemRect\(\s*HOLY_CARD\s*,\s*15\s*,\s*15\s*\)\s*;', ISS) is not None,
    'assignItemRect(HOLY_CARD, 15, 15) —— 恰好是包围盒，与用户贴图一致')
chk(ISS.find('HOLY_CARD                 =') < ISS.find('assignItemRect(HOLY_CARD')
    if 'HOLY_CARD                 =' in ISS else
    ISS.find('HOLY_CARD =') < ISS.find('assignItemRect(HOLY_CARD'),
    '常量声明在 assignItemRect 之前（反了 ⇒ 传 0 ⇒ 所有图标整体错位且不报错）')

# —— 接线：配方登记 + 指南页
chk(re.search(r'new HolyCard\.CraftRecipe\(\)', RECIPE) is not None, 'Recipe 表已登记神圣卡配方')
_rec_region = RECIPE[RECIPE.find('oneIngredientRecipes'):RECIPE.find('twoIngredientRecipes')]
chk('new HolyCard.CraftRecipe()' in _rec_region,
    '登记在 oneIngredientRecipes（只有 1 件原料 ⇒ 按件数分桶必须落在这里）')
chk('HolyCard' in QR and 'new Scroll.PlaceHolder()' in QR,
    '炼金指南页用 Scroll.PlaceHolder 展示「任意卷轴」')
_c6 = QR[QR.find('case 6:'):QR.find('case 7:')]
chk('HolyCard.CraftRecipe' in _c6, '神圣卡条目落在指南第 6 页（与洛伊德护符同页）')

# —— 文本键（zh + en）
_missing = []
for rel, keys in [
    ('items/items_zh.properties', ['items.holycard.name=', 'items.holycard.desc=',
                                   'items.holycard.ac_use=', 'items.holycard.used=',
                                   'items.holycard.already=']),
    ('items/items.properties', ['items.holycard.name=', 'items.holycard.desc=',
                                'items.holycard.ac_use=', 'items.holycard.used=',
                                'items.holycard.already=']),
    ('actors/actors_zh.properties', ['actors.buffs.holybarrier.name=', 'actors.buffs.holybarrier.desc=',
                                     'actors.buffs.holybarrier.blocked=']),
    ('actors/actors.properties', ['actors.buffs.holybarrier.name=', 'actors.buffs.holybarrier.desc=',
                                  'actors.buffs.holybarrier.blocked=']),
]:
    txt = read_msg(rel)
    for k in keys:
        if ('\n' + k) not in ('\n' + txt):
            _missing.append('%s 缺 %s' % (rel, k.rstrip('=')))
chk(not _missing, '文本键 zh/en 双份齐备（缺：%s）' % (_missing or '无'))

_dz = read_msg('items/items_zh.properties').split('items.holycard.desc=')[1].split('\n')[0].strip()
chk(_dz == '绘制着十字架的纯白卡牌，其中蕴含神圣的能量，能为你抵挡下一次伤害。',
    '道具描述逐字等于用户给定文案（实得 %r）' % _dz)
_de = read_msg('items/items.properties').split('items.holycard.desc=')[1].split('\n')[0].strip()
chk(len(_de) > 20 and '\\n' not in _de, 'en 描述存在且为单段（实得 %r）' % _de[:60])

# ================================================================
print()
print('=' * 78)
print('② 位置层：Hero.damage 的插入点（唯一调用点）')
print('=' * 78)

_callers = []
for dp, dn, fn in os.walk(SRC):
    for f in fn:
        if not f.endswith('.java'):
            continue
        rel = os.path.relpath(os.path.join(dp, f), SRC).replace('\\', '/')
        s = strip_comments(open(os.path.join(dp, f), encoding='utf-8').read())
        if 'HolyBarrier.blocks(' in s:
            _callers.append(rel)
chk(_callers == ['actors/hero/Hero.java'],
    'HolyBarrier.blocks( 全仓恰好 1 个调用点 ⇒ 边界只有一处（实得 %s）' % _callers)

_dmg = HERO[HERO.find('public void damage( int dmg, Object src )'):]
_dmg = _dmg[:_dmg.find('\n\t}\n')] if '\n\t}\n' in _dmg else _dmg
o, bad = order_ok(_dmg, [
    'Talent.tooHotImmune( this )',            # 先于：既有的两条整段免疫
    'HolyBarrier.blocks( this, src )',        # ← 本 hook
    'Afterimage.flinch( this, src )',         # 后于：受击视觉
    'Talent.perseveranceCap( this, dmg,',
    'super.damage( dmg, src )',               # 后于：血量下限保护与真正扣血
])
chk(o, '在 Hero.damage 里的位置：两条整段免疫之后、flinch 之前、super.damage 之前（首个逆序：%s）' % (bad,))
chk(re.search(r'if \(\s*dmg > 0\s*&&\s*HolyBarrier\.blocks\( this, src \)\s*\)\s*\{\s*'
              r'HolyBarrier\.trigger\( this \)\s*;\s*return;\s*\}', _dmg) is not None,
    'hook 形如「dmg > 0 && blocks(...) ⇒ trigger + return」（0 伤害不吃屏障）')
chk('HolyBarrier' in HERO[:HERO.find('public class Hero')],
    'Hero.java 里 import 了 HolyBarrier（漏了 import ⇒ 编译期就会炸，这里只是提前告诉你是谁）')

# ================================================================
print()
print('=' * 78)
print('③ 贴图层：真解 PNG 实测')
print('=' * 78)

_ip = os.path.join(ASSETS, 'sprites/items.png')
_buf, _W, _H, _nch = load_png(_ip)
chk((_W, _H) == (256, 800), 'items.png = 256x800（16 列 x 50 行，16px 格）')
_o, _bw, _bh, _hc_px = cell_stats(_buf, _W, _nch, 7 * 16, 40 * 16, 16, 16)
chk(_o > 0, 'row41 col8（x=112..127）有像素（神圣卡贴图已绘）')
chk((_bw, _bh) == (15, 15),
    'row41 col8 包围盒恰为 15x15（实得 %dx%d）—— assignItemRect 必须与之逐像素相等' % (_bw, _bh))

_o7, _bw7, _bh7, _px116 = cell_stats(_buf, _W, _nch, 6 * 16, 40 * 16, 16, 16)
chk(_o7 > 0 and (_bw7, _bh7) == (14, 16),
    'row41 col7（洛伊德护符）仍是 14x16（本次没有误动邻格）')

for _p, _size in [('interfaces/large_buffs.png', 16), ('interfaces/buffs.png', 7)]:
    _b2, _W2, _H2, _n2 = load_png(os.path.join(ASSETS, _p))
    _cols = _W2 // _size
    def _frame(f, _b2=_b2, _W2=_W2, _n2=_n2, _cols=_cols, _s=_size):
        return cell_stats(_b2, _W2, _n2, (f % _cols) * _s, (f // _cols) * _s, _s, _s)
    _o116, _, _, _px116b = _frame(116)
    _o117, _, _, _px117 = _frame(117)
    _o118, _, _, _ = _frame(118)
    chk(_o117 > 0, '%s 帧 117 非空（神圣屏障图标已绘）' % _p)
    chk(_o116 > 0 and _o117 > 0 and _px116b != _px117,
        '%s 帧 117 与帧 116（融化）不是同一张图（防复制后忘改）' % _p)
    chk(_o118 == 0, '%s 帧 118 仍为空（没有误占下一格）' % _p)

# ================================================================
print()
print('=' * 78)
print('④ 改动栏（极简风格）')
print('=' * 78)

_i = CHG.find('神圣卡')
chk(_i > 0, 'EGOPD_Changes 里已写入「神圣卡」条目')
_btns = re.findall(r'v035\.addButton\((.+?)\)\s*\);', CHG, re.S)
chk(any('神圣卡' in b for b in _btns), '神圣卡条目挂在 v0.3.5 段落里')
_fat = [b for b in _btns if '\\n' in b]
chk(not _fat, 'v0.3.5 全部条目仍为单段单句（越界 %d 条）' % len(_fat))

# ================================================================
if '--selftest' in sys.argv:
    print()
    print('=' * 78)
    print('⑤ 反例自测：把判据改坏，必须报 FAIL（证明不是恒真）')
    print('=' * 78)

    def _selftest_chk(name, cond):
        print(('  [OK]   ' if cond else '  [FAIL] ') + name)
        return cond

    _all = True

    # (a) 删掉「排除自身 buff 伤害」那条 ⇒ 判据被削弱，必须能被发现
    _broken = HB.replace('if (src instanceof Buff) return false;', '', 1)
    _bl2 = body_of(_broken, r'public static boolean blocks\(\s*Char ch\s*,\s*Object src\s*\)\s*\{')
    _all &= _selftest_chk('反例(a)：删掉「src instanceof Buff」排除后，判据检查报 FAIL',
                          'if (src instanceof Buff) return false;' not in (_bl2 or ''))

    # (b) 删掉「自伤换成长」排除 ⇒ 一张卡换一次免费升级
    _broken = HB.replace('if (src instanceof SelfHarmCost) return false;', '', 1)
    _bl2 = body_of(_broken, r'public static boolean blocks\(\s*Char ch\s*,\s*Object src\s*\)\s*\{')
    _all &= _selftest_chk('反例(b)：删掉 SelfHarmCost 排除后，判据检查报 FAIL',
                          'if (src instanceof SelfHarmCost) return false;' not in (_bl2 or ''))

    # (c) 把图集常量挪到 assignItemRect 之后 ⇒ 「声明先于使用」检查必须报 FAIL
    _iss2 = ISS.replace('public static final int HOLY_CARD                 =                          xy(8, 41);',
                        '')
    _iss2 = _iss2.replace('assignItemRect(HOLY_CARD,                   15, 15);',
                          'assignItemRect(HOLY_CARD,                   15, 15);\n'
                          '\t\tpublic static final int HOLY_CARD                 =                          xy(8, 41);')
    _all &= _selftest_chk('反例(c)：把 HOLY_CARD 常量挪到 assignItemRect 之后，顺序检查报 FAIL',
                          not (_iss2.find('HOLY_CARD                 =')
                               < _iss2.find('assignItemRect(HOLY_CARD')))

    # (d) 把 hook 挪到 super.damage 之后 ⇒ 「位置」检查必须报 FAIL
    _h2 = _dmg.replace('if (dmg > 0 && HolyBarrier.blocks( this, src )) {\n'
                       '\t\t\tHolyBarrier.trigger( this );\n\t\t\treturn;\n\t\t}\n', '', 1)
    _h2 = _h2 + '\n\t\tif (dmg > 0 && HolyBarrier.blocks( this, src )) { HolyBarrier.trigger( this ); return; }\n'
    _o2, _bad2 = order_ok(_h2, ['Talent.tooHotImmune( this )',
                                'HolyBarrier.blocks( this, src )',
                                'super.damage( dmg, src )'])
    _all &= _selftest_chk('反例(d)：把 hook 挪到 super.damage 之后，「比位置」报 FAIL（首个逆序：%s）' % (_bad2,),
                          not _o2)

    # (e) 把「已有屏障就直接 return」那一句 return 删掉 ⇒ 「不消耗回合/卡」的早退检查必须报 FAIL
    #     （注意要删的是 active() 分支里的那个 return，而不是更早那句 `!hero.isAlive()` 的）
    if _ex:
        _k = _ex.find('Messages.get( this, "already" )')
        _ex2 = _ex[:_k] + _ex[_k:].replace('return;', '', 1)
    else:
        _ex2 = ''
    _i_al = _ex2.find('HolyBarrier.active( hero )')
    _i_af = _ex2.find('Buff.affect( hero, HolyBarrier.class )')
    _i_sp = _ex2.find('hero.spendAndNext( TIME_TO_USE )')
    _i_rt = _ex2.find('return;', _i_al)
    _all &= _selftest_chk('反例(e)：去掉「已有屏障就直接 return」后，早退检查报 FAIL（找到的 return 位置=%d）' % _i_rt,
                          not (_i_al < _i_rt < _i_af and _i_rt < _i_sp))

    print()
    print('自测结论：' + ('判据均能捕获反例（不是恒真）' if _all else '有判据恒真，需要修'))
    if not _all:
        ok = False

print()
print('=' * 78)
print('全部核验通过。' if ok else '存在未通过的核验项，见上面的 [FAIL]。')
print('=' * 78)
sys.exit(0 if ok else 1)
