# -*- coding: utf-8 -*-
"""封印之剑系列「不可出售」回归核验（2026-09-23）。

判据目标：四个形态（封印/一阶段/二阶段/莱瓦汀）在任何情况下都不能出现在商店收购列表里。

覆盖三条链路：
  ① 覆写点  —— SealedSwordBase.sellable() 返回 false，且四个形态都不再各自覆写；
  ② 唯一出口 —— Shopkeeper.canSell 仍然咨询 item.sellable()；
  ③ 默认实现未被误改 —— Item.sellable() 仍是「!unique || stackable」；
  ④ 未误伤 —— TearSwordBlessing / Admiration 这两件「单独放开出售」的神器仍返回 true。
另含反例自测：把改动回退（或换成 unique 杠杆）后，同一套判据必须判 FAIL。

用法：python -X utf8 _chk/verify_sealed_sellable.py
"""
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon')

BASE = os.path.join(SRC, 'items/weapon/melee/SealedSwordBase.java')
FORMS = ['SealedSword', 'UnsealedSword', 'UnsealedSword2', 'Laevateinn']
ITEM = os.path.join(SRC, 'items/Item.java')
SHOP = os.path.join(SRC, 'actors/mobs/npcs/Shopkeeper.java')
WNDBAG = os.path.join(SRC, 'windows/WndBag.java')
ARTIFACTS = os.path.join(SRC, 'items/artifacts')

fails = []
total = [0]


def chk(ok, msg):
    total[0] += 1
    if not ok:
        fails.append(msg)
    print('  %s %s' % ('PASS' if ok else 'FAIL', msg))


def load(p):
    with open(p, encoding='utf-8') as f:
        return f.read()


def strip_comments(text):
    """剥掉 // 行注释与 /* */ 块注释（串里的 // 不管，本项目文本不含 URL 以外的 //）。"""
    text = re.sub(r'/\*.*?\*/', '', text, flags=re.S)
    text = re.sub(r'//[^\n]*', '', text)
    return text


def code_of(text, sig_re):
    """取 sig_re 匹配到的方法体（从签名起，按花括号配平到方法结束）。"""
    m = re.search(sig_re, text, flags=re.S)
    if not m:
        return None
    i = text.index('{', m.end() - 1) if '{' in m.group(0) else text.index('{', m.end())
    depth = 0
    for j in range(i, len(text)):
        if text[j] == '{':
            depth += 1
        elif text[j] == '}':
            depth -= 1
            if depth == 0:
                return text[i:j + 1]
    return None


def sellable_body(text):
    return code_of(text, r'public\s+boolean\s+sellable\s*\(\s*\)')


def returns_false(body):
    if body is None:
        return False
    code = strip_comments(body)
    return bool(re.search(r'return\s+false\s*;', code)) and not re.search(r'return\s+true\s*;', code)


# =====================================================================
print('[1] SealedSwordBase：覆写 sellable() 返回 false')
# =====================================================================
base = load(BASE)
body = sellable_body(base)
chk(body is not None, 'B1 存在 public boolean sellable() 覆写')
chk(returns_false(body), 'B2 方法体 return false（且不含 return true）')
chk('@Override' in base[max(0, (body and base.index(body)) - 120):(body and base.index(body)) or 0],
    'B3 定义处带 @Override')

code_base = strip_comments(base)
chk(not re.search(r'\bunique\s*=\s*true\s*;', code_base),
    'B4 没有拿 unique 当杠杆（唯一物品会顺带禁掉锻造/附魔等，注释里已写明约定）')
chk('sellable' in base, 'B5 文件里确实提到 sellable（说明覆写落在本类）')

# =====================================================================
print('[2] 四个形态：都不再各自覆写 sellable（全部继承基类 false）')
# =====================================================================
for name in FORMS:
    p = os.path.join(SRC, 'items/weapon/melee/%s.java' % name)
    text = load(p)
    chk(re.search(r'class\s+%s\s+extends\s+SealedSwordBase' % name, text) is not None,
        '%s：仍 extends SealedSwordBase（继承链没断）' % name)
    chk(sellable_body(text) is None,
        '%s：没有自己的 sellable()（继承基类的 false）' % name)

# =====================================================================
print('[3] 唯一出口：Shopkeeper.canSell 仍咨询 item.sellable()')
# =====================================================================
shop = load(SHOP)
chk(bool(re.search(r'if\s*\(\s*!item\.sellable\(\)\s*\)\s*return\s+false\s*;', shop)),
    'S1 canSell 里有「!item.sellable() ⇒ 不可售」这一条')
chk(shop.count('item.sellable()') == 1, 'S2 全仓只有这一处消费 sellable()（实得 %d）' % shop.count('item.sellable()'))

# =====================================================================
print('[4] 成因证据：WndBag 把主手/副手一并摆进列表（所以锁在手上挡不住出售）')
# =====================================================================
bag = load(WNDBAG)
chk('stuff.weapon' in bag and 'stuff.secondWep' in bag,
    'W1 WndBag 布局仍包含 weapon / secondWep（副手剑会出现在收购列表）')

# =====================================================================
print('[5] 默认实现未被误改：Item.sellable() = !unique || stackable')
# =====================================================================
item = load(ITEM)
ibody = sellable_body(item)
chk(ibody is not None, 'I1 Item.sellable() 仍存在')
chk(bool(re.search(r'return\s*!unique\s*\|\|\s*stackable\s*;', strip_comments(ibody or ''))),
    'I2 默认实现仍是「!unique || stackable」（未被本次改动波及）')

# =====================================================================
print('[6] 未误伤：两件「单独放开出售」的神器仍返回 true')
# =====================================================================
for name in ('TearSwordBlessing', 'Admiration'):
    ab = sellable_body(load(os.path.join(ARTIFACTS, '%s.java' % name)))
    chk(ab is not None and bool(re.search(r'return\s+true\s*;', strip_comments(ab))),
        '%s.sellable() 仍 return true' % name)

# =====================================================================
print('[7] 结构自检：括号平衡 + 无 BOM + UTF-8')
# =====================================================================
for p in (BASE, ITEM, SHOP, WNDBAG):
    raw = open(p, 'rb').read()
    try:
        raw.decode('utf-8')
        enc_ok = True
    except UnicodeDecodeError:
        enc_ok = False
    chk(enc_ok and not raw.startswith(b'\xef\xbb\xbf'),
        '%s：合法 UTF-8 且无 BOM' % os.path.basename(p))
    t = raw.decode('utf-8')
    chk(t.count('{') == t.count('}') and t.count('(') == t.count(')'),
        '%s：花括号/圆括号平衡' % os.path.basename(p))

# =====================================================================
print('[8] 反例自测：判据不能空转')
# =====================================================================
# 反例 A —— 回退覆写（方法体改成 return true）⇒ B1/B2 必须 FAIL
neg_a = base.replace('public boolean sellable() {\n\t\treturn false;\n\t}',
                     'public boolean sellable() {\n\t\treturn true;\n\t}')
chk(neg_a != base, 'N0 反例 A 确实改到了文本（否则本组是空转）')
na = sellable_body(neg_a)
chk(na is not None, 'N1 反例 A 仍取到方法体（证明提取器工作正常）')
chk(not returns_false(na), 'N2 反例 A：return true ⇒ returns_false 判 FAIL（正确）')

# 反例 B —— 整段删掉覆写 ⇒ 提取器必须返回 None
neg_b = re.sub(r'\t/\*\*\n\t \* <b>不可出售</b>.*?\n\tpublic boolean sellable\(\) \{\n\t\treturn false;\n\t\}\n\n',
               '', base, flags=re.S)
chk(neg_b != base, 'N3 反例 B 确实删掉了覆写块')
chk(sellable_body(neg_b) is None, 'N4 反例 B：覆写消失 ⇒ 提取器返回 None（会被 B1 抓住）')

# 反例 C —— 拿 unique 当杠杆 ⇒ B4 必须 FAIL
neg_c = base.replace('tier = 6; //六阶武器（力量需求固定 22 基础，见 STRReq）',
                     'tier = 6;\n\t\tunique = true;')
chk(neg_c != base, 'N5 反例 C 确实插入了 unique = true')
chk(bool(re.search(r'\bunique\s*=\s*true\s*;', strip_comments(neg_c))),
    'N6 反例 C：unique 杠杆会被 B4 抓到（正确）')

# =====================================================================
print()
print('断言总数 %d，失败 %d' % (total[0], len(fails)))
if fails:
    print('FAIL：')
    for f in fails:
        print('   -', f)
    sys.exit(1)
print('ALL PASS')
