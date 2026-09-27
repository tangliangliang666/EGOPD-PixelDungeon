# -*- coding: utf-8 -*-
"""2026-09-23：把「专属武器锁在手上 ≠ 不可售」这条陷阱同时补进
① docs/handbook/pitfalls.md（全量表）② AGENTS.md §6 精简表。
幂等：已存在则跳过。"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PIT = os.path.join(ROOT, 'docs/handbook/pitfalls.md')
AG = os.path.join(ROOT, 'AGENTS.md')

# 幂等标记：两个文件里都出现的东西（不能只出现在其中一个文件的措辞里，
# 否则第二个文件重跑时判不出「已应用」）
MARK = 'verify_sealed_sellable.py'

ROW_FULL = (
    u'| 角色专属武器**锁死在手上照样能在商店卖掉**（本次：封印之剑系列四形态被当六阶武器换钱，'
    u'而它明明是「不能卸下」的） | 「锁在手上」＝`doUnequip()` 一律拒绝，但**挡不住出售**：'
    u'商店收购窗口用的 `WndBag` 在布局里把 `belongings.weapon`（主手）与 `belongings.secondWep`（副手）'
    u'**一起** `placeItem`，所以剑只要挂在副手就会出现在收购列表里。而 `Item.sellable()` 默认＝'
    u'`!unique \\|\\| stackable`，本系列是普通武器**没标** `unique` ⇒ 默认可售 | '
    u'覆写 `Item.sellable()` 返回 `false`，放**基类一处**即覆盖全系列四形态（与 `isUpgradable()` 同理由）；'
    u'`Shopkeeper.canSell` 是全仓**唯一**消费点，封锁它即封锁整条路径。**别**改用 `unique = true` —— '
    u'会顺带禁掉锻造/附魔等一整套。回归 `_chk/verify_sealed_sellable.py` |'
)

ROW_SHORT = (
    u'| **专属武器「锁在手上」照样能被卖掉** | `doUnequip()` 拒绝卸下**挡不住商店**：收购窗口 `WndBag` '
    u'把主手 `weapon` 与副手 `secondWep` **一起**摆进列表；而 `Item.sellable()` 默认＝'
    u'`!unique \\|\\| stackable`，专属普通武器没标 `unique` ⇒ 默认可售。覆写 `Item.sellable()` 返回 false '
    u'（放**基类一处**覆盖全系列），**别**改用 `unique`（会连坐锻造/附魔）。`verify_sealed_sellable.py` |'
)


def read(path):
    with open(path, 'rb') as f:
        raw = f.read()
    return (b'\r\n' in raw), raw.decode('utf-8').replace('\r\n', '\n')


def write(path, text, crlf):
    out = text.replace('\n', '\r\n') if crlf else text
    with open(path, 'wb') as f:
        f.write(out.encode('utf-8'))


# ============ ① pitfalls.md：追加到表格末行之后 ============
crlf, text = read(PIT)
if MARK in text:
    print('SKIP pitfalls.md（已存在）')
else:
    if not text.endswith('\n'):
        text += '\n'
    lines = text.split('\n')
    rows = [i for i, l in enumerate(lines) if l.startswith('|')]
    assert len(rows) == 108, '全量表行数变了（实得 %d），需人工确认' % len(rows)
    last = rows[-1]
    assert lines[last].rstrip().endswith('|'), '末行不是完整表格行'
    lines.insert(last + 1, ROW_FULL)
    text = '\n'.join(lines)
    write(PIT, text, crlf)
    print('OK pitfalls.md 已插入（表格行 108 -> %d）'
          % len([l for l in text.split('\n') if l.startswith('|')]))

# ============ ② AGENTS.md：追加到精简表末行之后 + 更新计数 ============
crlf, text = read(AG)
if MARK in text:
    print('SKIP AGENTS.md（已存在）')
else:
    if not text.endswith('\n'):
        text += '\n'
    lines = text.split('\n')
    # 精简表：从「### 高频静默陷阱（精简表）」到下一个空行/标题之间的 | 行
    start = next(i for i, l in enumerate(lines) if l.startswith('### 高频静默陷阱'))
    rows = [i for i, l in enumerate(lines) if l.startswith('|') and i > start]
    assert len(rows) >= 20, '精简表行数异常（实得 %d）' % len(rows)
    last = rows[-1]
    assert lines[last].rstrip().endswith('|'), '精简表末行不是完整表格行'
    lines.insert(last + 1, ROW_SHORT)
    text = '\n'.join(lines)
    # 注记里的全量表行数 111 -> 112
    before = text
    text = text.replace(u'全量 111 行原表在', u'全量 112 行原表在', 1)
    assert text != before, '未找到「全量 111 行原表在」注记，需人工确认'
    write(AG, text, crlf)
    print('OK AGENTS.md 已插入（%d bytes）' % len(text.encode('utf-8')))

print('done')
