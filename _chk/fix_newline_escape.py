# -*- coding: utf-8 -*-
"""修复（2026-09-17）：instant_exec_text.py 把 Python 源码里的 `\n` 当成了真换行，
写进 .properties 后变成「一条 entry 断成多行」——续行没有 `=`，会被解析成一堆垃圾键。

本脚本把断掉的 entry 重新接回单行（续行之间补字面量 \\n），并用「期望值表」逐键断言，
确保修完的内容与设计文案逐字一致。

用法：cd /d/PD && python _chk/fix_newline_escape.py
"""
import io
import sys

FILES = {
    'zh': r'D:/PD/core/src/main/assets/messages/actors/actors_zh.properties',
    'en': r'D:/PD/core/src/main/assets/messages/actors/actors.properties',
}

NL2 = '\\n\\n'
NL1 = '\\n'

# 期望值：源码里写 \\n 才是 .properties 里的字面量 \n
EXPECT = {
    'actors.hero.abilities.middlefinger.instantexecution.desc': {
        'zh': '中指长兄对敌人进行_即刻处刑_。'
              + NL2 + '主手未持_莱瓦汀_时，立刻把它换上来。'
              + NL1 + '随后连续施展 _三段_ 攻击，每段 _30%_ 攻击倍率。'
              + NL1 + '第三段命中后，把目标_击飞 5 格_。'
              + NL1 + '再_投出手中的莱瓦汀_追击目标，造成_一次基于莱瓦汀面板的攻击伤害_。'
              + NL1 + '最后_跃至目标身边_，以一记收尾重击终结：伤害 _5+力量_ ~ _15+3×力量_'
              + '（力量已计入_仇怨_等加成），并附带_震击_。',
        'en': 'The Middle Finger performs an _instant execution_.'
              + NL2 + 'If Laevatinn is not in his main hand, he switches it in at once.'
              + NL1 + 'He then strikes the target _three times_, each at _30%_ attack power.'
              + NL1 + 'The third hit _knocks the target back 5 tiles_.'
              + NL1 + 'He then _throws Laevatinn_ after it, dealing _one attack based on Laevatinn\'s damage roll_.'
              + NL1 + 'Finally he _leaps to the target\'s side_ and ends it with a finishing blow for '
              + '_5+STR_ ~ _15+3xSTR_ (STR includes bonuses such as _Rancor_), with a _shockwave_.',
    },
    'actors.hero.talent.no_ledger_needed.desc': {
        'zh': '_+1：_使用「即刻处刑[莱瓦汀]」时，_立刻消耗复仇账簿的全部充能_，'
              '并按消耗充能的 _0.5 倍_获得伤害加成。'
              + NL2 + '_+2：_按消耗充能的 _1 倍_获得伤害加成。'
              + NL2 + '_+3：_按消耗充能的 _1.5 倍_获得伤害加成。',
        'en': '_+1:_ Instant Execution [Laevatinn] _immediately consumes all of the Revenge Ledger\'s charge_, '
              'and gains bonus damage equal to _0.5x_ that charge.'
              + NL2 + '_+2:_ Bonus damage equal to _1x_ the charge.'
              + NL2 + '_+3:_ Bonus damage equal to _1.5x_ the charge.',
    },
    'actors.hero.talent.overdue_release.desc': {
        'zh': '_+1：_使用「即刻处刑[莱瓦汀]」时，_莱瓦汀系列_武器获得 _1 临时等级_，持续 _10 回合_。'
              + NL2 + '_+2：_获得 _2 临时等级_，持续 _15 回合_。'
              + NL2 + '_+3：_获得 _3 临时等级_，持续 _20 回合_。',
        'en': '_+1:_ Using Instant Execution [Laevatinn] gives _Laevatinn-series weapons_ '
              '_1 temporary level_ for _10 turns_.'
              + NL2 + '_+2:_ _2 temporary levels_ for _15 turns_.'
              + NL2 + '_+3:_ _3 temporary levels_ for _20 turns_.',
    },
    'actors.hero.talent.legendary_blade.desc': {
        'zh': '_+1：_使用「即刻处刑[莱瓦汀]」时，若血量低于 _50%_，则消耗的充能 _降低 20%_。'
              + NL2 + '_+2：_除 +1 效果外，若血量低于 _40%_，则消耗的充能_额外降低 10%_。'
              + NL2 + '_+3：_除 +1、+2 效果外，若血量低于 _30%_，则消耗的充能_额外降低 10%_。',
        'en': '_+1:_ Using Instant Execution [Laevatinn] while below _50%_ HP reduces the charge cost by _20%_.'
              + NL2 + '_+2:_ As +1, and while below _40%_ HP the charge cost is reduced by a further _10%_.'
              + NL2 + '_+3:_ As +1 & +2, and while below _30%_ HP the charge cost is reduced by a further _10%_.',
    },
    'actors.buffs.executionunleashed.desc': {
        'zh': '莱瓦汀系列武器获得 _+%1$d 临时等级_。' + NL2 + '剩余回合：%2$s',
        'en': 'Laevatinn-series weapons gain _+%1$d temporary levels_.'
              + NL2 + 'Turns remaining: %2$s',
    },
}

fail = 0


def bad(msg):
    global fail
    fail += 1
    print('FAIL  ' + msg)


for side, path in FILES.items():
    with io.open(path, encoding='utf-8') as f:
        lines = f.read().split('\n')

    out = []
    joined = 0
    i = 0
    while i < len(lines):
        line = lines[i]
        st = line.strip()
        if not st or st[0] in '#!':
            out.append(line)
            i += 1
            continue
        if '=' in line:
            out.append(line)
            i += 1
            continue
        # 续行：把之前累积的空行折成 \n\n
        blanks = 0
        while out and not out[-1].strip():
            out.pop()
            blanks += 1
        if not out:
            bad('%s:%d 续行出现在文件开头' % (side, i + 1))
            i += 1
            continue
        out[-1] = out[-1] + (NL2 if blanks else NL1) + line
        joined += 1
        i += 1

    with io.open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(out))

    # 断言值
    vals = {}
    for l in out:
        st = l.strip()
        if not st or st[0] in '#!' or '=' not in l:
            continue
        k, v = l.split('=', 1)
        vals[k.strip()] = v

    for k, per_side in EXPECT.items():
        got = vals.get(k)
        if got != per_side[side]:
            bad('[VAL] %s %s 与期望不符\n      期望: %s\n      实际: %s' % (side, k, per_side[side], got))

    # 修复后不允许再有任何「没有 = 的行」
    stray = [j + 1 for j, l in enumerate(out)
             if l.strip() and l.lstrip()[0] not in '#!' and '=' not in l]
    if stray:
        bad('%s 仍有 %d 行缺少 =（行号 %s）' % (side, len(stray), stray[:8]))

    print('%s: 接回续行 %d 处，行数 -> %d，键数 -> %d' % (side, joined, len(out), len(vals)))

print('--- 修复完毕 ---')
print('OK' if fail == 0 else '共 %d 处问题' % fail)
sys.exit(1 if fail else 0)
