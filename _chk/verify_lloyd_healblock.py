# -*- coding: utf-8 -*-
# 核验「洛伊德护符」与「禁疗」（HealBlock）的落地，且**把全仓回血收口钉成回归护栏**。
#
# 为什么需要它：
#   「禁疗」这条需求有一句最硬的话 ——「持续期间获得的所有血量恢复被强制置 0（需要能够覆盖
#   包括 CHESED 考验效果在内的回血）」。玩家要的是**语义全覆盖**，不是「把几个常见回血路径
#   各打一个补丁」。所以本作的做法是把「把 HP 往上抬」收口到唯一入口 Char.heal(int)，
#   禁疗只在那一个地方判（HealBlock.blocks）——这样药水 / 圣草 / 食物 / 吸血鬼 / 法杖 /
#   CHESED 的 ChesedMend 全都天然被拦。
#
#   代价是：**这条不变量会被以后的任何一次「顺手 HP += ...」悄悄破坏**。所以核验不能只看
#   LloydTalisman / HealBlock 写没写，必须每次重扫一遍全仓「抬血站点」，断言它们**恰好**
#   等于预先登记的白名单（白名单里全是「有意排除」的非回血站点）。
#
# 分四层：
#   ① 结构层：护符 / 禁疗 / Char.heal / 图集 / 粒子 / 配方 / 指南页 / 文本键 / 炼金釜修复的
#      源码断言（比位置，防顺序写反；剥注释，防被注释里的伪代码骗）。
#   ② 收口层：全仓抬血站点扫描 ≡ 白名单。少一个 = 有人把回血改回直写；多一个白名单外的 =
#      新增站点绕过了 heal()。**这是本脚本存在的主要理由。**
#   ③ 排除层：HT 钳制 / 生成初始化 / 阶段血 / 上限重算这些**刻意不动**的站点仍在原地
#      —— 证明收口没有「用力过猛」把 Boss 阶段阈值、满血初始化也一起拦了。
#   ④ 反例自测（--selftest）：拿人为改坏的样本喂同一批判据，必须报 FAIL，
#      证明判据不是恒真的。
#
# 用法：
#   python _chk/verify_lloyd_healblock.py
#   python _chk/verify_lloyd_healblock.py --selftest
import os
import re
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')
MSG = os.path.join(ROOT, 'core/src/main/assets/messages')

ok = True


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
    """把注释替换成等长空格（保留换行）——**保留字符偏移**，所以仍能「比位置」。

    单趟状态机，识别 // 、/* */ 、字符串 "" 、字符 ''。
    不能用正则去 /*...*/ ：注释里出现 `//**` 时正则会一路吞到文件末尾
    （项目里已踩过，check_unused_imports.py 同款做法）。
    """
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
    """读源码并剥注释，用于「必须含 / 必须不含 / 比位置」这类断言。"""
    raw = read(rel)
    assert '"""' not in raw, '出现 Java 文本块，strip_comments 需要升级'
    return strip_comments(raw)


def norm(s):
    return ' '.join(s.split())


def order_ok(src, anchors):
    """anchors 必须按给定顺序在 src 里出现（各取首次出现）。返回 (bool, 首个逆序对)。"""
    pos, last, last_a = [], -1, None
    for a in anchors:
        p = src.find(a)
        if p < 0:
            return False, ('缺失', a)
        if p < last:
            return False, (last_a, a)
        last, last_a = p, a
    return True, None


# ================================================================
# ① 结构层
# ================================================================
print('=' * 78)
print('① 结构层：洛伊德护符 / 禁疗 / 收口点 / 图集 / 配方 / 文本键')
print('=' * 78)

LLOYD = read_j('items/LloydTalisman.java')
HB = read_j('actors/buffs/HealBlock.java')
CHAR = read_j('actors/Char.java')
ISS = read_j('sprites/ItemSpriteSheet.java')
WSP = read_j('effects/particles/WhiteSmokeParticle.java')
RECIPE = read_j('items/Recipe.java')
QRE = read_j('ui/QuickRecipe.java')
AER = read_j('items/artifacts/ArtifactEnhanceRecipe.java')
ALCH = read_j('scenes/AlchemyScene.java')

# —— LloydTalisman 外观与身份
chk('class LloydTalisman extends Item' in LLOYD, '洛伊德护符 extends Item（投掷道具的标准基类）')
chk(re.search(r'HEAL_BLOCK_TURNS\s*=\s*50f\s*;', LLOYD) is not None, '禁疗时长 = 50 回合')
chk(re.search(r'MIMIC_DISABLE_TURNS\s*=\s*20f\s*;', LLOYD) is not None, '宝箱怪额外禁用 = 20 回合')
chk(re.search(r'image\s*=\s*ItemSpriteSheet\.LLOYD_TALISMAN\s*;', LLOYD) is not None,
    'image 指向 ItemSpriteSheet.LLOYD_TALISMAN')
chk(re.search(r'stackable\s*=\s*true\s*;', LLOYD) is not None, 'stackable = true（可堆叠，配方一次出 6 个）')
chk(re.search(r'defaultAction\s*=\s*AC_THROW\s*;', LLOYD) is not None,
    'defaultAction = AC_THROW（用户要求「投掷生效」）')
chk(re.search(r'isUpgradable\s*\(\s*\)\s*\{\s*return\s+false\s*;', LLOYD) is not None,
    'isUpgradable() 返回 false（炼金产物，不可强化）')
chk(re.search(r'isIdentified\s*\(\s*\)\s*\{[^}]*return\s+true\s*;', LLOYD) is not None,
    'isIdentified() 返回 true（与松脂系列一致，效果固定）')

# —— onThrow 的顺序（这里顺序就是语义，必须比位置）
m = re.search(r'protected void onThrow\s*\(\s*int cell\s*\)\s*\{', LLOYD)
chk(m is not None, '有 onThrow(int cell) 覆写')
body = LLOYD[m.end():] if m else ''
# 截到方法收尾（顶格一层 \n\t}）
end = body.find('\n\t}')
body = body[:end] if end > 0 else body

_o, _bad = order_ok(body, [
    'Actor.findChar( cell )',          # 1 先找落点单位
    'super.onThrow( cell )',           # 2 未命中：照常落地（可捡回）
    'WhiteSmokeParticle.BURST',        # 3 命中：目标身上炸白烟
    'CellEmitter.get( cell )',         # 4 落点也起一团
    'Assets.Sounds.PUFF',              # 5 音效
    'instanceof Mimic',                # 6 判定宝箱怪
    'stopHiding()',                    # 7 先揭穿伪装
    'HealBlock.apply( ch, HEAL_BLOCK_TURNS )',  # 8 再挂禁疗
    'Paralysis.class',                 # 9 麻痹
    'Weakness.class',                  # 10 虚弱
    'Blindness.class',                 # 11 致盲
])
chk(_o, 'onThrow 内顺序正确（未命中落地 → 白烟 → 揭穿 → 禁疗 → 麻痹/虚弱/致盲）%s'
    % ('' if _o else '首个逆序：%s → %s' % _bad))
chk(body.count('super.onThrow') == 1,
    'super.onThrow 只出现在「未命中」分支一次（命中即消耗，刻意不落地）'
    '（实得 %d 次）' % body.count('super.onThrow'))
chk(body.find('stopHiding()') < body.find('HealBlock.apply'),
    'stopHiding() 在 HealBlock.apply 之前 —— 否则 Mimic.add() 的 NEGATIVE 自动暴露会重复播特效')
chk(re.search(r'if\s*\(\s*ch\s*==\s*null\s*\|\|\s*!\s*ch\.isAlive\(\)\s*\)', body) is not None,
    '空/死亡目标走未命中分支（不会给尸体挂 buff）')

# —— 配方数值（用户三次确认）：治疗药水×1 + 液金×20，3 能量 → 6 个
m = re.search(r'class CraftRecipe extends Recipe\.SimpleRecipe\s*\{', LLOYD)
cr = LLOYD[m.end():] if m else ''
cr = cr[:cr.find('\n\t}')] if '\n\t}' in cr else cr
chk('PotionOfHealing.class' in cr and 'LiquidMetal.class' in cr,
    '配方原料 = 治疗药水 + 液金')
chk(re.search(r'inQuantity\s*=\s*new int\[\]\s*\{\s*1\s*,\s*20\s*\}\s*;', cr) is not None,
    '原料数量 = { 1, 20 }')
chk(re.search(r'\bcost\s*=\s*3\s*;', cr) is not None, '炼金能量 = 3（用户第二次修正）')
chk(re.search(r'output\s*=\s*LloydTalisman\.class\s*;', cr) is not None, '产物 = LloydTalisman')
chk(re.search(r'outQuantity\s*=\s*6\s*;', cr) is not None, '产出数量 = 6')

# —— HealBlock 语义
chk('class HealBlock extends FlavourBuff' in HB, 'HealBlock 是顶层类（存档反射重建，不能做内部类）')
chk(re.search(r'DURATION\s*=\s*50f\s*;', HB) is not None, 'DURATION = 50f')
chk(re.search(r'type\s*=\s*buffType\.NEGATIVE\s*;', HB) is not None,
    'type = NEGATIVE（护符命中才挂，不踩「入场即挂」红线）')
chk(re.search(r'announced\s*=\s*true\s*;', HB) is not None, 'announced = true（挂上时提示）')
chk(re.search(r'Buff\.prolong\(\s*ch\s*,\s*HealBlock\.class\s*,\s*duration\s*\)', HB) is not None,
    'apply() 走 Buff.prolong（只延不缩、不叠加）')
chk(re.search(r'public static boolean blocks\(\s*Char ch\s*\)', HB) is not None,
    'blocks(Char) 是 static 工具（禁疗语义只此两处，便于日后收窄条件）')
chk(re.search(r'return ch\s*!=\s*null\s*&&\s*ch\.buff\(\s*HealBlock\.class\s*\)\s*!=\s*null\s*;', HB) is not None,
    'blocks() 判据 = 目标身上有 HealBlock')
chk(re.search(r'private transient Emitter smoke\s*=\s*null\s*;', HB) is not None,
    '持续烟雾发射器缓存为 transient 字段（共享池对象必须缓存引用才能关掉）')
chk(re.search(r'smoke\.pour\(\s*WhiteSmokeParticle\.FACTORY\s*,\s*0\.09f\s*\)', HB) is not None,
    'fx(true) 里 pour(FACTORY, 0.09f)')
chk(re.search(r'smoke\.on\s*=\s*false\s*;', HB) is not None, 'fx(false) 里把 emitter 关掉并置空')
chk('Messages.get( this, "desc", dispTurns() )' in HB,
    'desc() 把剩余回合传进模板（%s）—— 2026-09-24 精简掉后按用户要求又恢复')

# —— Char.heal：唯一判定点，且判在钳制之前
m = re.search(r'public int heal\(\s*int amount\s*\)\s*\{', CHAR)
chk(m is not None, 'Char.heal(int amount) 存在（回血唯一入口）')
hbody = CHAR[m.end():]
hbody = hbody[:hbody.find('\n\t}')] if '\n\t}' in hbody else hbody
chk('HealBlock.blocks( this )' in hbody, 'heal() 内以 HealBlock.blocks(this) 为唯一判据')
chk(hbody.find('HealBlock.blocks( this )') < hbody.find('Math.min( HT, HP + amount )'),
    '禁疗判定在钳制/加血**之前**执行')
chk(re.search(r'return HP - before\s*;', hbody) is not None,
    'heal() 返回「实际回复量」（带浮字站点据此守卫，禁疗时不再弹假 +N）')

# —— 全仓 HealBlock.blocks( 调用点恰好 1 处（Char.java）
callers = []
_heal_calls = 0
_healed_guards = 0
for dp, dn, fn in os.walk(SRC):
    for f in fn:
        if not f.endswith('.java'):
            continue
        rel = os.path.relpath(os.path.join(dp, f), SRC).replace('\\', '/')
        s = strip_comments(open(os.path.join(dp, f), encoding='utf-8').read())
        if 'HealBlock.blocks(' in s:
            callers.append(rel)
        _heal_calls += len(re.findall(r'\.heal\(', s))
        _healed_guards += len(re.findall(r'int healed\b', s))
chk(callers == ['actors/Char.java'],
    'HealBlock.blocks( 全仓恰好 1 个调用点 ⇒ 禁疗语义无第二处（实得 %s）' % callers)
chk(_heal_calls >= 50,
    '全仓 `.heal(` 调用点 ≥ 50（实得 %d）⇒ 收口没有被人回滚成直写 HP' % _heal_calls)
chk(_healed_guards >= 30,
    '全仓 `int healed = ...heal(...)` 守卫 ≥ 30 处（实得 %d）⇒ 浮字站点按实际回复量把关' % _healed_guards)

# —— CHESED 这条路径单独钉死（用户需求的原文点名了它）
_ches = read_j('actors/buffs/ChesedMend.java')
chk(re.search(r'int healed\s*=\s*mob\.heal\(\s*heal\s*\)\s*;', _ches) is not None,
    'ChesedMend（CHESED 考验的回血）走 mob.heal(heal) ⇒ 与其它回血同一条收口')
chk(re.search(r'if\s*\(\s*healed\s*>\s*0\s*&&\s*mob\.sprite\s*!=\s*null\s*\)', _ches) is not None,
    'ChesedMend 的浮字有 healed > 0 + sprite null 双重守卫（禁疗时不弹假 +N、换场景不 NPE）')

# —— 图集
chk(re.search(r'LLOYD_TALISMAN\s*=\s*xy\(\s*7\s*,\s*41\s*\)\s*;', ISS) is not None,
    'ItemSpriteSheet.LLOYD_TALISMAN = xy(7, 41)')
chk(re.search(r'assignItemRect\(\s*LLOYD_TALISMAN\s*,\s*14\s*,\s*16\s*\)\s*;', ISS) is not None,
    'assignItemRect(LLOYD_TALISMAN, 14, 16) —— 必须恰好是包围盒，与用户贴图一致')

# —— 粒子
chk(re.search(r'FACTORY\s*=\s*new Emitter\.Factory', WSP) is not None, 'WhiteSmokeParticle.FACTORY 存在')
chk(re.search(r'BURST\s*=\s*new Emitter\.Factory', WSP) is not None, 'WhiteSmokeParticle.BURST 存在')
# 尺寸 = 原值的一半（面积 1/4）：size( 6.5f - p*3.5f )；原为 size( 13 - p*7 )
_m = re.search(r'size\(\s*([0-9.]+)f?\s*-\s*p\s*\*\s*([0-9.]+)f?\s*\)', WSP)
chk(_m is not None and abs(float(_m.group(1)) - 6.5) < 1e-6 and abs(float(_m.group(2)) - 3.5) < 1e-6,
    '白烟粒子 size() 为 6.5f - p*3.5f（原尺寸的一半 ⇒ 面积四分之一）')
chk(re.search(r'size\(\s*13\s*-', WSP) is None, '旧的 size( 13 - p * 7 ) 已不存在')

# —— 配方注册（Recipe 表）+ 松脂系列仍在
chk(re.search(r'new LloydTalisman\.CraftRecipe\(\)', RECIPE) is not None, 'Recipe 表已登记洛伊德护符配方')
for name, pat in [
    ('焦炭松脂·药剂转', r'new CharcoalResin\.FromPotion\(\)'),
    ('散装焦炭松脂·拆分', r'new LooseCharcoalResin\.SplitRecipe\(\)'),
    ('黄金松脂·药剂转', r'new GoldenResin\.FromPotion\(\)'),
    ('散装黄金松脂·拆分', r'new LooseGoldenResin\.SplitRecipe\(\)'),
]:
    chk(re.search(pat, RECIPE) is not None, 'Recipe 表已登记「%s」' % name)

# —— 指南页（QuickRecipe 第 6 页）：松脂 4 条 + 洛伊德 1 条
c6 = re.search(r'case 6:\s*(.*?)\n\t\t\tcase 7:', QRE, re.S)
c6 = c6.group(1) if c6 else ''
chk(re.search(r'new LloydTalisman\.CraftRecipe\(\)', c6) is not None, 'QuickRecipe 第 6 页含洛伊德护符')
for name, pat in [
    ('焦炭松脂·药剂转', r'new CharcoalResin\.FromPotion\(\)'),
    ('散装焦炭松脂·拆分', r'new LooseCharcoalResin\.SplitRecipe\(\)'),
    ('黄金松脂·药剂转', r'new GoldenResin\.FromPotion\(\)'),
    ('散装黄金松脂·拆分', r'new LooseGoldenResin\.SplitRecipe\(\)'),
]:
    chk(re.search(pat, c6) is not None, 'QuickRecipe 第 6 页含「%s」（原先只在 Recipe 表，指南页看不到）' % name)
chk('PotionOfHealing()' in c6 and 'LiquidMetal().quantity( 20 )' in c6.replace('  ', ' ').replace('( ', '(').replace(' )', ')')
    or re.search(r'LiquidMetal\(\)\s*\.quantity\(\s*20\s*\)', c6) is not None,
    'QuickRecipe 展示的原料与配方一致（治疗药水 + 液金×20）')
chk(re.search(r'new LloydTalisman\(\)\s*\.quantity\(\s*6\s*\)', c6) is not None,
    'QuickRecipe 展示的产出为 6 个')

# —— 神器加强：脑啡肽 60 → 30（不留旧值）
chk(re.search(r'ENKEPHALIN_NEEDED\s*=\s*30\s*;', AER) is not None, 'ArtifactEnhanceRecipe.ENKEPHALIN_NEEDED = 30')
_bad60 = re.findall(r'quantity\(\s*60\s*\)', QRE)
chk(not _bad60, 'QuickRecipe 里脑啡肽已无 60 的残留（实得 %d 处）' % len(_bad60))

# —— 炼金釜「点箭头闪退」修复
chk(re.search(r'while\s*\(\s*!found\.isEmpty\(\)\s*&&\s*needed\s*>\s*0\s*&&\s*curslot\s*<\s*inputs\.length\s*\)', ALCH) is not None,
    '快速配方套用循环自行收敛到 inputs.length（防 inputs[3] 越界闪退）')
chk(re.search(r'for\s*\(\s*int i\s*=\s*0\s*;\s*i\s*<\s*recipes\.size\(\)\s*&&\s*i\s*<\s*combines\.length\s*;\s*i\+\+\s*\)', ALCH) is not None,
    'combines 填充循环受 combines.length(=3) 双重上界（防越界）')

# —— 文本键（zh + en）
for rel, keys in [
    ('items/items_zh.properties', ['items.lloydtalisman.name=', 'items.lloydtalisman.desc=']),
    ('items/items.properties', ['items.lloydtalisman.name=', 'items.lloydtalisman.desc=']),
    ('actors/actors_zh.properties', ['actors.buffs.healblock.name=', 'actors.buffs.healblock.desc=']),
    ('actors/actors.properties', ['actors.buffs.healblock.name=', 'actors.buffs.healblock.desc=']),
]:
    txt = read_msg(rel)
    for k in keys:
        chk(('\n' + k) in ('\n' + txt) or txt.startswith(k), '%s 有键 %s' % (rel, k.rstrip('=')))
_hb_zh = read_msg('actors/actors_zh.properties')
_hb_en = read_msg('actors/actors.properties')
_dz = _hb_zh.split('actors.buffs.healblock.desc=')[1].split('\n')[0].strip()
_de = _hb_en.split('actors.buffs.healblock.desc=')[1].split('\n')[0].strip()
chk(_dz == '无法恢复生命值\\n\\n剩余时长：%s回合',
    'healblock zh desc = 一句话 + 剩余回合（实得 %r）' % _dz)
chk(_de == 'Cannot recover health\\n\\nTurns left: %s',
    'healblock en desc = 一句话 + 剩余回合（实得 %r）' % _de)
chk('%s' in _dz and '%s' in _de,
    'healblock 双语 desc 都带 %%s 占位（剩余回合要显示出来）')

# —— 改动栏 v0.3.5：条目在，且**极简**（每条一句话，不换行分段）
CHG = read('ui/changelist/EGOPD_Changes.java')
_i = CHG.find('"EGOPD v0.3.5"')
_j = CHG.find('"EGOPD v0.3.4"', _i) if _i > 0 else -1
chk(_i > 0, 'EGOPD_Changes 有 major 条目「EGOPD v0.3.5」')
_v35 = CHG[_i:_j] if (_i > 0 and _j > _i) else ''
chk('洛伊德护符' in _v35, 'v0.3.5 条目里写了「洛伊德护符」')
_btns = re.findall(r'v035\.addButton\((.+?)\)\s*\);', _v35, re.S)
chk(len(_btns) >= 5, 'v0.3.5 至少有 5 条改动（实得 %d）' % len(_btns))
# 「极简」的机械判据：正文里不出现 \n 转义 ⇒ 只有一段、没有「背景 → 现在 → 边界」的展开。
# （标题与正文之间本来就分两行写，所以这里只看 \n 转义，不看源码换行。）
_fat = [b for b in _btns if '\\n' in b]
chk(not _fat, 'v0.3.5 全部条目均为单段单句、正文无 \\n 分段（2026-09-24 起的极简风格；'
              '越界条目数 %d）' % len(_fat))

# ================================================================
# ② 收口层：全仓抬血站点 ≡ 白名单  ← 本脚本的主要理由
# ================================================================
print()
print('=' * 78)
print('② 收口层：全仓「抬血站点」扫描 ≡ 白名单')
print('=' * 78)

# 「抬血」的四种写法（宁可放宽也不要漏 —— 漏掉真实站点是静默失败，多报出来只会 FAIL 提醒）：
#   HP += x ； HP = HP + x ； HP = Math.min( ..., HP + x ) ； HP = Math.max( HP, 下限 )
# 两条收紧规则（试扫后修的误报）：
#   · 左侧必须**不是标识符的尾巴**：`(?<![\w])` 挡掉 `int postHP = ...` / `int floorHP = ...`
#     这类被当作赋值语句的局部变量声明（它们的 `HP = ` 只是名字的一部分）。
#   · `Math.max` 只认**首参就是 HP** 的形式：`Math.max(1, HP-damage)` 是下压钳制
#     （CrystalGuardian 的减伤、MeltingLove 的百分比重设），不是抬血。
RAISE_PATS = [
    re.compile(r'(?<![\w])(?:[A-Za-z_]\w*\.)?HP\s*\+=\s*[^;{}]+;'),
    re.compile(r'(?<![\w])(?:[A-Za-z_]\w*\.)?HP\s*=\s*(?:[A-Za-z_]\w*\.)?HP\s*\+[^;{}]+;'),
    re.compile(r'(?<![\w])(?:[A-Za-z_]\w*\.)?HP\s*=\s*Math\.min\([^;{}]*HP\s*\+[^;{}]*\)\s*;'),
    re.compile(r'(?<![\w])(?:[A-Za-z_]\w*\.)?HP\s*=\s*Math\.max\(\s*(?:[A-Za-z_]\w*\.)?HP\s*,[^;{}]*\)\s*;'),
]

# 白名单 = 「有意排除」的非回血站点。每一条都要能说出「为什么它不是回血」：
#   · Char.java        —— heal() 本体（收口点自己）
#   · Blessing.java    —— HT 增加时按增量回算当前 HP（上限重算，与 updateHT 同族）
#   · PrismaticGuard   —— 自持 float HP 字段（护盾量，不是生命）
#   · Hero.java        —— updateHT(boostHP)：HT 涨了 HP 跟着补差额（钳制语义）
#   · ShadowClone.java —— HP += hpBonus：分身 HT 增量回算
#   · WandOfWarding    —— 守卫塔升级时按档位重设 HP（阶梯生成，不是回血）
#   · YogDzewa 两处    —— ★ 这条是**设计选择**，不是「它不是回血」：打 Boss 时把血**垫回**
#                          该阶段的 HT 派生下限（防止阶段跳跃式一刀削穿）。它是 Boss 阶段刻度
#                          的一部分（见 _chk/verify_boss_phase_scales.py），本作刻意让它
#                          不受禁疗影响。若以后要「连 Boss 阶段下限也禁疗」，改这里是**唯一**入口。
WHITELIST = {
    ('actors/Char.java',                              'HP = Math.min( HT, HP + amount );'),
    ('actors/buffs/Blessing.java',                    'target.HP += htBonus;'),
    ('actors/buffs/PrismaticGuard.java',              'HP += 0.1f;'),
    ('actors/hero/Hero.java',                         'HP += Math.max(HT - curHT, 0);'),
    ('actors/hero/abilities/rogue/ShadowClone.java',  'HP += hpBonus;'),
    ('items/wands/WandOfWarding.java',                'HP += 19;'),
    ('items/wands/WandOfWarding.java',                'HP += 30;'),
    ('actors/mobs/YogDzewa.java',                     'HP = Math.max(HP, HT - phaseStep() * phase);'),
    ('actors/mobs/YogDzewa.java',                     'HP = Math.max(HP, finalPhaseFloor());'),
}


def scan_raise_sites(root):
    """扫全仓抬血站点，返回 {(rel, 归一化行): 行号}。"""
    hits = {}
    for dp, dn, fn in os.walk(root):
        for f in fn:
            if not f.endswith('.java'):
                continue
            p = os.path.join(dp, f)
            rel = os.path.relpath(p, root).replace('\\', '/')
            src = strip_comments(open(p, encoding='utf-8').read())
            for pat in RAISE_PATS:
                for mm in pat.finditer(src):
                    ln = src.count('\n', 0, mm.start()) + 1
                    line = norm(src.split('\n')[ln - 1])
                    hits[(rel, line)] = ln
    return hits


hits = scan_raise_sites(SRC)
extra = sorted(k for k in hits if k not in WHITELIST)
missing = sorted(k for k in WHITELIST if k not in hits)
chk(not extra,
    '全仓无「白名单外」的抬血站点（多 %d 个：%s）'
    % (len(extra), '；'.join('%s:%d %s' % (k[0], hits[k], k[1]) for k in extra[:6])))
chk(not missing,
    '白名单里的站点都还在（少 %d 个：%s）—— 收录里的「排除项」不该被顺手改掉'
    % (len(missing), '；'.join(k[0] for k in missing[:6])))
chk(len(hits) == len(WHITELIST),
    '抬血站点总数 = 白名单条目数 %d（实得 %d）' % (len(WHITELIST), len(hits)))
# heal() 本体必须**在**命中里（证明扫描真的扫到了收口点，而不是正则失灵全空）
chk(('actors/Char.java', 'HP = Math.min( HT, HP + amount );') in hits,
    'heal() 本体被扫描命中 ⇒ 正则有效（正面样本）')

# ================================================================
# ③ 排除层：刻意不动的 HT 族站点仍在原地（防「用力过猛」）
# ================================================================
print()
print('=' * 78)
print('③ 排除层：HT 钳制 / 生成初始化 / 阶段血 —— 刻意未收口')
print('=' * 78)

# 这些若被改成 heal() 调用，Boss 阶段阈值、满血初始化就会被禁疗拦掉 ⇒ 必须保持原样
KEEP = [
    ('actors/mobs/Tengu.java',                     r'HP\s*=\s*\(\s*HT\s*/\s*2\s*\)\s*;'),
    ('actors/mobs/YogFist.java',                   r'HP\s*=\s*HT\s*/\s*2\s*;'),
    ('actors/mobs/Mob.java',                       r'HP\s*=\s*Math\.round\(\s*HT\s*\*\s*percent\s*\)\s*;'),
    ('actors/mobs/MeltingLove.java',               r'HP\s*=\s*Math\.max\(\s*1\s*,\s*Math\.round\(\s*HT\s*\*\s*pct\s*\)\s*\)\s*;'),
    ('actors/mobs/Succubus.java',                  r'HP\s*=\s*HT\s*;'),
    ('actors/mobs/DecoyDoll.java',                 r'HP\s*=\s*HT\s*;'),
    ('actors/mobs/RingfingerAutomaton.java',       r'HP\s*=\s*HT\s*;'),
    ('actors/mobs/SmokeBomb.java'.replace('actors/mobs/', 'actors/hero/abilities/rogue/'),
     r'HP\s*=\s*HT\s*;'),
    ('actors/hero/Hero.java',                      r'HP\s*=\s*HT\s*;'),   # resurrect()
    ('items/artifacts/DriedRose.java',             r'HP\s*=\s*HT\s*;'),
]
for rel, pat in KEEP:
    src = read_j(rel)
    chk(re.search(pat, src) is not None, '%s 的 HT 族赋值仍在（未被误改成回血）' % rel)

# goo 的收口反例：Goo「满血重算」在收口前后都是 Math.min(HP, HT)（下压），不该被拦
_goo = read_j('actors/mobs/Goo.java')
chk(re.search(r'HP\s*=\s*Math\.min\(\s*HP\s*,\s*HT\s*\)\s*;', _goo) is not None,
    'Goo 满血重算仍是 Math.min(HP, HT)（下压语义，不该收口）')

# ================================================================
if '--selftest' in sys.argv:
    print()
    print('=' * 78)
    print('④ 反例自测：把样本改坏，判据必须报 FAIL（证明不是恒真）')
    print('=' * 78)

    def _selftest_chk(name, cond):
        print(('  [OK]   ' if cond else '  [FAIL] ') + name)
        return cond

    _all = True

    # (a) 删掉 heal() 里的禁疗判定 ⇒ 「判据存在」与「判定先于钳制」两个判据都必须报 FAIL
    broken = CHAR.replace('HealBlock.blocks( this )', 'false', 1)
    mb = re.search(r'public int heal\(\s*int amount\s*\)\s*\{', broken)
    bb = broken[mb.end():]
    bb = bb[:bb.find('\n\t}')]
    _all &= _selftest_chk('反例(a)：heal() 去掉禁疗判定后，「判据存在」检查报 FAIL',
                          'HealBlock.blocks( this )' not in bb)
    _guard_ok = (('HealBlock.blocks( this )' in bb)
                 and (bb.find('HealBlock.blocks( this )') < bb.find('Math.min( HT, HP + amount )')))
    _all &= _selftest_chk('反例(a2)：去掉判定后，「判定先于钳制」判据确实为假（不是恒真）',
                          not _guard_ok)

    # (b) 把唯一的 stopHiding() 挪到禁疗之后 ⇒ 「比位置」必须报 FAIL
    #     注意：判据里正确顺序是 stopHiding → apply（先揭穿、后挂 buff），
    #     所以自测也要用**同一组顺序锚点**，否则测的是另一件事。
    swapped = body.replace('stopHiding()', 'noopHere()') + '\n\t\tstopHiding();'
    o2, bad2 = order_ok(swapped, ['stopHiding()', 'HealBlock.apply( ch, HEAL_BLOCK_TURNS )'])
    _all &= _selftest_chk(
        '反例(b)：把 stopHiding() 挪到禁疗之后，「比位置」报 FAIL（首个逆序：{0}）'.format(bad2),
        (not o2) and set(bad2) == {'stopHiding()', 'HealBlock.apply( ch, HEAL_BLOCK_TURNS )'})

    # (c) 凭空加一个白名单外的抬血站点 ⇒ 扫描必须报 extra
    fake_dir = os.path.join(HERE, '_selftest_lloyd')
    os.makedirs(fake_dir, exist_ok=True)
    with open(os.path.join(fake_dir, 'FakeHeal.java'), 'w', encoding='utf-8') as fh:
        fh.write('class FakeHeal { void f() { HP += 5; } }\n')
    fh_hits = scan_raise_sites(fake_dir)
    _all &= _selftest_chk('反例(c)：新增 HP += 5 站点后，白名单比对会报「多出 %d 个」' % len(fh_hits),
                          len(fh_hits) == 1 and list(fh_hits)[0] not in WHITELIST)
    try:
        os.remove(os.path.join(fake_dir, 'FakeHeal.java'))
        os.rmdir(fake_dir)
    except OSError:
        pass

    # (d) 剥注释有效性：注释里的 `HP += 999;` 不该被扫到
    probe_src = 'class T { /* HP += 999; */ int x = 1; }'
    _all &= _selftest_chk('反例(d)：注释里的 HP += 999 被 strip_comments 剥掉，不计入扫描',
                          not RAISE_PATS[0].search(strip_comments(probe_src)))

    print()
    print('自测结论：%s' % ('判据均能捕获反例（不是恒真）' if _all else '有判据恒真 —— 需要修』'))
    if not _all:
        ok = False


print()
print('=' * 78)
print('全部核验通过。' if ok else '存在未通过的核验项，见上面的 [FAIL]。')
print('=' * 78)
sys.exit(0 if ok else 1)
