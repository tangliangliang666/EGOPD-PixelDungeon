# -*- coding: utf-8 -*-
"""把 CHESED / GEBURA 的占位文案换成正式描述（zh + en 各一份）。

为什么用脚本而不是编辑器：本工程 `.properties` 是 **CRLF**，逐行替换必须走**字节级** IO，
否则整文件被改写成 LF ⇒ git diff 变成「每行都改了」（本项目已踩过）。

做法：按 `\\r\\n` 切行 → 找到 `trials.chesed_desc=` / `trials.gebura_desc=` 两行 → 整行替换 → 用
`\\r\\n` 重新拼接。**幂等**：目标行里已有新文案的特征串就直接跳过，重复跑不会二次修改。

用法：
  python _chk/fix_chesed_gebura_text.py            # 应用
  python _chk/fix_chesed_gebura_text.py --check    # 只报告，不写盘
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MISC = os.path.join(ROOT, 'core/src/main/assets/messages/misc')
ZH = os.path.join(MISC, 'misc_zh.properties')
EN = os.path.join(MISC, 'misc.properties')

# 两份文案都写成「不含真换行」的单行 properties 值：换行一律是字面 \n（游戏按此解析）。
# 注意：这里写的是**两个字符** 反斜杠 + n，不是真换行。
CHESED_ZH = (
    r'trials.chesed_desc=所有_敌方单位_的生命上限 _+25%_，并且每 _5_ 回合恢复一次生命，'
    r'每次恢复其_生命上限的 10%_。\n\n'
    r'回血_只在血量不满时才开始计时_：满血的单位不会累积计时，一旦受伤便从第 1 回合重新数起，'
    r'连续 5 个回合都处于「不满血」才会回一次血。单次回血不会超过缺失的血量。\n\n'
    r'「怪物」仅指_敌方单位_（与挑战「升华」的判据一致）；你的盟友与中立 NPC 不受影响。'
)
GEBURA_ZH = (
    r'trials.gebura_desc=所有_敌方单位_即将死亡时不会立刻倒下，而是进入一段_无敌_时间；'
    r'_无敌的回合数等于它给予的经验值_（例如给予 5 点经验的怪物会撑 5 个回合）。计时结束的那一刻它才真正死亡。\n\n'
    r'无敌期间它_免疫一切伤害_，血量停留在 _0 点_（血条已空，但仍未倒下），并且仍会照常行动。\n\n'
    r'它最终倒下时仍算作_原先那一击_的击杀：经验、掉落与各项统计都与正常击杀完全一致。\n\n'
    r'靠覆写存活判定硬撑的战续单位（如豺狼暴徒）、以及自带「倒地待复活」机制的单位'
    r'（如矮人尸群）不受影响，它们仍按原版行事。\n\n'
    r'两处例外：站在深渊之上的致死不触发本考验；给予经验为 _0_ 的单位不会获得无敌。'
)
CHESED_EN = (
    r'trials.chesed_desc=All _hostile units_ have _+25%_ max health, and recover health once every _5_ turns, '
    r'each time healing for _10% of their max health_.\n\n'
    r'The heal timer only starts while their health is _not full_: a unit at full health does not accumulate the count, '
    r'and after taking damage the count restarts from turn 1 -- it must stay below full health for 5 turns in a row '
    r'to heal once. A single heal never exceeds the missing health.\n\n'
    r'"Enemies" means _hostile units only_ (same rule as the Ascension challenge); your allies and neutral NPCs are unaffected.'
)
GEBURA_EN = (
    r'trials.gebura_desc=All _hostile units_ do not fall when they are about to die. Instead they become _invulnerable_ '
    r'for a number of turns _equal to the experience they give_ (a monster worth 5 exp lasts 5 turns), '
    r'and only die when that timer runs out.\n\n'
    r'While invulnerable they _take no damage at all_ and their health stays at _0_ (an empty health bar that still refuses to fall), '
    r'and they keep taking their turns as usual.\n\n'
    r'When they finally fall, the kill still counts as _the blow that would have killed them_: '
    r'experience, drops and stats are all resolved exactly as a normal kill.\n\n'
    r'Units that stay alive by overriding their own survival check (such as gnoll brutes), and those with a built-in '
    r'"downed then revived" mechanic (such as ghouls), are unaffected and keep acting exactly as in vanilla.\n\n'
    r'Two exceptions: a lethal blow dealt while standing over a chasm does not trigger this trial, '
    r'and units worth _0_ experience never gain the invulnerability.'
)

JOBS = [
    (ZH, 'trials.chesed_desc=', CHESED_ZH, '生命上限 _+25%_'),
    (ZH, 'trials.gebura_desc=', GEBURA_ZH, '无敌的回合数等于它给予的经验值'),
    (EN, 'trials.chesed_desc=', CHESED_EN, '+25%_ max health'),
    (EN, 'trials.gebura_desc=', GEBURA_EN, 'equal to the experience they give'),
]

check_only = '--check' in sys.argv
fails = 0

for path, key, newline, marker in JOBS:
    raw = open(path, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    lines = raw.split(b'\r\n')
    idx = [i for i, l in enumerate(lines) if l.startswith(key.encode('utf-8'))]
    tag = os.path.basename(path) + ' ' + key

    if len(idx) != 1:
        print('  [FAIL] %-44s 期望恰好 1 行，实得 %d' % (tag, len(idx)))
        fails += 1
        continue

    i = idx[0]
    old = lines[i].decode('utf-8')
    if marker in old:
        print('  [SKIP] %-44s 已是新文案（幂等跳过）' % tag)
        continue

    if '占位' not in old and 'Placeholder' not in old and 'not yet decided' not in old:
        print('  [WARN] %-44s 原值既不是占位也不是新文案，仍按计划替换：%s' % (tag, old[:40]))
    if not newline.startswith(key):
        print('  [FAIL] %-44s 新文案的键名写错了' % tag)
        fails += 1
        continue

    lines[i] = newline.encode('utf-8')
    out = b'\r\n'.join(lines)

    # 硬核验：CRLF 计数不变、键序与行数不变、UTF-8 可解
    crlf_after = out.count(b'\r\n')
    if crlf_after != crlf_before:
        print('  [FAIL] %-44s CRLF 计数变了（%d -> %d）' % (tag, crlf_before, crlf_after))
        fails += 1
        continue
    if len(out.split(b'\r\n')) != len(lines):
        print('  [FAIL] %-44s 行数变了' % tag)
        fails += 1
        continue
    try:
        out.decode('utf-8')
    except UnicodeDecodeError as e:
        print('  [FAIL] %-44s 写出后不是合法 UTF-8：%s' % (tag, e))
        fails += 1
        continue

    if check_only:
        print('  [WOULD] %-43s 将替换（--check，不写盘）' % tag)
    else:
        open(path, 'wb').write(out)
        print('  [OK]   %-43s 已替换（CRLF %d 不变）' % (tag, crlf_after))

print('')
print('完成：%s' % ('只检查，未写盘' if check_only else '已写盘'))
sys.exit(1 if fails else 0)
