# -*- coding: utf-8 -*-
"""
BINAH（理解）实现 · 第 3 步：写入考验效果文本（trials.binah_desc，zh + en）。

关键（沿用 HOKMA 的做法）：属性文件里的换行必须写成**字面**反斜杠 + n（`\\n`），
不能是真换行 —— 真换行会把 entry 截成两半，游戏只显示第一段而且不报错。
为了彻底绕开「源码里的 \\n 被还原成真换行」这个老坑，本脚本一律用 BS = chr(92)
拼出反斜杠，源码里不出现任何反斜杠字面量。

顺带：保留原文件的行尾风格（CRLF / LF），只改需要的那一行。
"""
import io
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MSG = os.path.join(ROOT, 'core/src/main/assets/messages')
BAK = os.path.join(ROOT, '_chk/_bak_binah_text')

BS = chr(92)
NL = BS + 'n'          # 属性文件里表示换行的两个字面字符

BINAH_DESC = {
    'misc/misc_zh.properties':
        '英雄在迷宫中拾获的道具_不会带有正等级_。若该道具生成时掷出了诅咒，则把掷到的等级' +
        '_整体取负_（掷到 0 的仍是 0）。' + NL + NL +
        '判定顺序是_先判诅咒反转、再判正等级归零_ —— 所以诅咒道具得到的是_负等级_，而不是被归零。' + NL + NL +
        '这条规则只管_生成的那一刻_：地面掉落、商店货架、宝箱、雕像的装备都算；' +
        '之后的一切强化（铁匠重铸、升级卷轴、附魔）都不受影响。' + NL + NL +
        '四处任务赠礼另有规定，等级一律为 _0_：1 区的幽灵、2 区的制杖人、3 区的铁匠、4 区的小恶魔。',
    'misc/misc.properties':
        'Items you find are _never generated with a positive upgrade level_. If an item rolls cursed ' +
        'as it is generated, the level it rolled is _flipped to negative_ (a roll of 0 stays 0).' + NL + NL +
        'The curse flip is checked _first_, and positive levels are flattened _afterwards_ -- so cursed ' +
        'items end up negative rather than simply being zeroed.' + NL + NL +
        'This only applies to the _moment of generation_: floor drops, shop stock, chests and statue ' +
        'equipment. Upgrades applied afterwards (reforging, upgrade scrolls, enchanting) are unaffected.' + NL + NL +
        'The four quest rewards are handled separately and are always level _0_: the Ghost in region 1, ' +
        'the Wandmaker in region 2, the Blacksmith in region 3, and the Imp in region 4.',
}

fails = []


def read_props(rel):
    p = os.path.join(MSG, rel)
    raw = open(p, 'rb').read()
    text = raw.decode('utf-8')
    eol = '\r\n' if '\r\n' in text else '\n'
    return p, text, eol


def write_props(p, text, eol):
    if not os.path.isdir(BAK):
        os.makedirs(BAK)
    shutil.copy2(p, os.path.join(BAK, os.path.basename(p)))
    open(p, 'wb').write(text.encode('utf-8'))


def upsert_key(rel, key, value):
    """把 `key=value` 整行替换掉；若不存在则记 FAIL（避免静默新增到错误位置）。"""
    p, text, eol = read_props(rel)
    lines = text.split(eol)
    want = key + '=' + value
    hit = [i for i, l in enumerate(lines) if l.startswith(key + '=')]
    if len(hit) != 1:
        fails.append('%s：`%s` 命中 %d 次（期望 1 次）' % (rel, key, len(hit)))
        return False
    if lines[hit[0]] == want:
        print('  [OK]   %s：%s 已是目标值，跳过' % (rel, key))
        return True
    lines[hit[0]] = want
    write_props(p, eol.join(lines), eol)
    print('  [OK]   %s：已改写 %s（%d 字符）' % (rel, key, len(value)))
    return True


print('=' * 78)
print('写入考验 BINAH 的属性文本')
print('=' * 78)

print('\n--- ① 考验效果描述（trials.binah_desc）---')
for rel in ('misc/misc_zh.properties', 'misc/misc.properties'):
    upsert_key(rel, 'trials.binah_desc', BINAH_DESC[rel])

print('\n--- ② 断言：落位 / 换行是「字面两字符」/ entry 未被真换行截断 ---')
for rel in BINAH_DESC:
    p, text, eol = read_props(rel)
    lines = text.split(eol)
    hit = [l for l in lines if l.startswith('trials.binah_desc=')]
    if len(hit) != 1:
        fails.append('%s：trials.binah_desc 行数 %d' % (rel, len(hit)))
        continue
    line = hit[0]
    val = line.split('=', 1)[1]
    # 字面 \n 必须存在（多段文本），且不得含真换行（真换行说明 entry 断了）
    ok_nl = NL in val
    ok_no_real_nl = '\n' not in val and '\r' not in val
    ok_mentions = ('0' in val) and ('负' in val or 'negative' in val)
    print('  [OK]   %s：含字面 \\n=%s，无真换行=%s，提到 0/负等级=%s'
          % (rel, ok_nl, ok_no_real_nl, ok_mentions))
    if not ok_nl:
        fails.append('%s：缺少字面 \\n（多段文本会被压成一行）' % rel)
    if not ok_no_real_nl:
        fails.append('%s：值里出现真换行（entry 被截断）' % rel)
    if not ok_mentions:
        fails.append('%s：描述未提到 0 / 负等级' % rel)
    # 相邻行不得是以 = 结尾的空值行（本次改写若写坏会立刻暴露）
    for i, l in enumerate(lines):
        s = l.strip()
        if s.startswith('trials.binah') and s.endswith('='):
            fails.append('%s:%d 值缺失：%s' % (rel, i + 1, s))

print('\n' + '=' * 78)
if fails:
    print('!! %d 条 FAIL：' % len(fails))
    for f in fails:
        print('   - ' + f)
    sys.exit(1)
print('ALL PASS —— BINAH 文本已写入')
print('=' * 78)
