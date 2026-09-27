# -*- coding: utf-8 -*-
"""一次性文本脚本（2026-09-17）：
为「即刻处刑[莱瓦汀]」（InstantExecution）盔甲技能写入 zh / en 文本，
并清掉「其三」的占位键与两行过期注释。

用法：cd /d/PD && python _chk/instant_exec_text.py
"""
import io
import sys

ZH = 'core/src/main/assets/messages/actors/actors_zh.properties'
EN = 'core/src/main/assets/messages/actors/actors.properties'

PLACEHOLDER = 'actors.hero.abilities.middlefinger.middlefingerabilitythree.'


def read(p):
    with io.open(p, encoding='utf-8') as f:
        return f.read().split('\n')


def write(p, lines):
    with io.open(p, 'w', encoding='utf-8', newline='\n') as f:
        f.write('\n'.join(lines))


def drop(lines, needle, tag):
    hits = [i for i, l in enumerate(lines) if needle in l]
    assert len(hits) == 1, '%s: 命中 %d 次（%s）' % (tag, len(hits), needle)
    del lines[hits[0]]


def drop_prefix(lines, prefix, tag, expect):
    hits = [i for i, l in enumerate(lines) if l.startswith(prefix)]
    assert len(hits) == expect, '%s: %s 命中 %d 次（期望 %d）' % (tag, prefix, len(hits), expect)
    for i in reversed(hits):
        del lines[i]


def insert_after(lines, anchor, block, tag):
    hits = [i for i, l in enumerate(lines) if l.startswith(anchor + '=')]
    assert len(hits) == 1, '%s: 锚点 %s 命中 %d 次' % (tag, anchor, len(hits))
    lines[hits[0] + 1:hits[0] + 1] = esc(block)


def esc(block):
    """块里的字符串在 Python 源码里可以写成真换行（更好读），落盘前必须换成字面量 \\n。

    <p>踩过的坑（2026-09-17）：直接写进去会让一条 entry 断成多行——续行没有 `=`，
    会被 properties 解析成一堆垃圾键，界面上显示成「乱码/空文本」。</p>
    """
    return [l.replace('\n', '\\n') for l in block]


def assert_no_stray(lines, tag):
    """落盘前自检：不允许出现任何「没有 = 的非空非注释行」。"""
    stray = [i + 1 for i, l in enumerate(lines)
             if l.strip() and l.lstrip()[0] not in '#!' and '=' not in l]
    assert not stray, '%s 出现缺少 = 的行（%s）' % (tag, stray[:8])


def count(lines, prefix):
    return len([1 for l in lines if l.startswith(prefix)])


# ===========================================================================
ZH_ABILITY = [
    '',
    '# 2026-09-17：中指 长兄 盔甲技能「即刻处刑[莱瓦汀]」（InstantExecution）',
    '# 80% 充能；三段 30% → 第三段击飞 5 格 → 第四次投出莱瓦汀追击 → 跃至敌侧 + 5+S~15+3S 收尾震击',
    'actors.hero.abilities.middlefinger.instantexecution.name=即刻处刑[莱瓦汀]',
    'actors.hero.abilities.middlefinger.instantexecution.short_desc=对目标施展 _三段 30%_ 的斩击，第三段将其_击飞 5 格_；随后_投出莱瓦汀_追击，再_跃至其身边_以一记收尾重击终结。',
    'actors.hero.abilities.middlefinger.instantexecution.desc=中指长兄对敌人进行_即刻处刑_。\n\n主手未持_莱瓦汀_时，立刻把它换上来。\n随后连续施展 _三段_ 攻击，每段 _30%_ 攻击倍率。\n第三段命中后，把目标_击飞 5 格_。\n再_投出手中的莱瓦汀_追击目标，造成_一次基于莱瓦汀面板的攻击伤害_。\n最后_跃至目标身边_，以一记收尾重击终结：伤害 _5+力量_ ~ _15+3×力量_（力量已计入_仇怨_等加成），并附带_震击_。',
    'actors.hero.abilities.middlefinger.instantexecution.prompt=选择要处刑的目标',
    'actors.hero.abilities.middlefinger.instantexecution.bad_target=这里没有可以处刑的敌人！',
    'actors.hero.abilities.middlefinger.instantexecution.too_far=目标太远了，即刻处刑只能对_相邻_的敌人施展。',
    'actors.hero.abilities.middlefinger.instantexecution.no_laevateinn=你手上没有封印之剑，换不出莱瓦汀。',
    'actors.hero.abilities.middlefinger.instantexecution.cast=中指长兄挥出了即刻处刑。',
    'actors.hero.abilities.middlefinger.instantexecution.devour=复仇账簿的充能已被吞尽（_%1$d 点_），化作 _%2$d 点_伤害加成。',
]

ZH_TALENT = [
    '# ---- 中指 长兄 盔甲技能「即刻处刑[莱瓦汀]」的 T4 天赋（343/344/345）----',
    'actors.hero.talent.no_ledger_needed.title=没有打开账簿的必要',
    'actors.hero.talent.no_ledger_needed.desc=_+1：_使用「即刻处刑[莱瓦汀]」时，_立刻消耗复仇账簿的全部充能_，并按消耗充能的 _0.5 倍_获得伤害加成。\n\n_+2：_按消耗充能的 _1 倍_获得伤害加成。\n\n_+3：_按消耗充能的 _1.5 倍_获得伤害加成。',
    'actors.hero.talent.overdue_release.title=好久没解放到这种程度了',
    'actors.hero.talent.overdue_release.desc=_+1：_使用「即刻处刑[莱瓦汀]」时，_莱瓦汀系列_武器获得 _1 临时等级_，持续 _10 回合_。\n\n_+2：_获得 _2 临时等级_，持续 _15 回合_。\n\n_+3：_获得 _3 临时等级_，持续 _20 回合_。',
    'actors.hero.talent.legendary_blade.title=只属于我的传说之剑',
    'actors.hero.talent.legendary_blade.desc=_+1：_使用「即刻处刑[莱瓦汀]」时，若血量低于 _50%_，则消耗的充能 _降低 20%_。\n\n_+2：_除 +1 效果外，若血量低于 _40%_，则消耗的充能_额外降低 10%_。\n\n_+3：_除 +1、+2 效果外，若血量低于 _30%_，则消耗的充能_额外降低 10%_。',
]

ZH_BUFF = [
    'actors.buffs.executionunleashed.name=莱瓦汀解放',
    'actors.buffs.executionunleashed.desc=莱瓦汀系列武器获得 _+%1$d 临时等级_。\n\n剩余回合：%2$s',
]

EN_ABILITY = [
    '',
    '# 2026-09-17: Middle Finger armour ability "Instant Execution [Laevatinn]" (InstantExecution)',
    '# 80% charge; three 30% hits -> knock back 5 tiles on the third -> throw Laevatinn -> leap in for a 5+STR~15+3xSTR finisher',
    'actors.hero.abilities.middlefinger.instantexecution.name=instant execution [Laevatinn]',
    'actors.hero.abilities.middlefinger.instantexecution.short_desc=Strike the target _three times_ at _30%_, _knock it back 5 tiles_ on the third hit, then _throw Laevatinn_ after it and _leap to its side_ for a finishing blow.',
    'actors.hero.abilities.middlefinger.instantexecution.desc=The Middle Finger performs an _instant execution_.\n\nIf Laevatinn is not in his main hand, he switches it in at once.\nHe then strikes the target _three times_, each at _30%_ attack power.\nThe third hit _knocks the target back 5 tiles_.\nHe then _throws Laevatinn_ after it, dealing _one attack based on Laevatinn\'s damage roll_.\nFinally he _leaps to the target\'s side_ and ends it with a finishing blow for _5+STR_ ~ _15+3xSTR_ (STR includes bonuses such as _Rancor_), with a _shockwave_.',
    'actors.hero.abilities.middlefinger.instantexecution.prompt=Choose the target to execute',
    'actors.hero.abilities.middlefinger.instantexecution.bad_target=There is no enemy to execute here!',
    'actors.hero.abilities.middlefinger.instantexecution.too_far=That target is too far away; instant execution only works on an _adjacent_ enemy.',
    'actors.hero.abilities.middlefinger.instantexecution.no_laevateinn=You have no sealed sword, so Laevatinn cannot be brought out.',
    'actors.hero.abilities.middlefinger.instantexecution.cast=The Middle Finger delivers an instant execution.',
    'actors.hero.abilities.middlefinger.instantexecution.devour=The Revenge Ledger\'s charge is devoured (_%1$d points_), becoming _%2$d_ bonus damage.',
]

EN_TALENT = [
    '# ---- Middle Finger armour ability "Instant Execution [Laevatinn]" tier-4 talents (343/344/345) ----',
    'actors.hero.talent.no_ledger_needed.title=No Need to Open the Ledger',
    'actors.hero.talent.no_ledger_needed.desc=_+1:_ Instant Execution [Laevatinn] _immediately consumes all of the Revenge Ledger\'s charge_, and gains bonus damage equal to _0.5x_ that charge.\n\n_+2:_ Bonus damage equal to _1x_ the charge.\n\n_+3:_ Bonus damage equal to _1.5x_ the charge.',
    'actors.hero.talent.overdue_release.title=It Has Been a While Since I Was Released Like This',
    'actors.hero.talent.overdue_release.desc=_+1:_ Using Instant Execution [Laevatinn] gives _Laevatinn-series weapons_ _1 temporary level_ for _10 turns_.\n\n_+2:_ _2 temporary levels_ for _15 turns_.\n\n_+3:_ _3 temporary levels_ for _20 turns_.',
    'actors.hero.talent.legendary_blade.title=The Legendary Sword That Is Only Mine',
    'actors.hero.talent.legendary_blade.desc=_+1:_ Using Instant Execution [Laevatinn] while below _50%_ HP reduces the charge cost by _20%_.\n\n_+2:_ As +1, and while below _40%_ HP the charge cost is reduced by a further _10%_.\n\n_+3:_ As +1 & +2, and while below _30%_ HP the charge cost is reduced by a further _10%_.',
]

EN_BUFF = [
    'actors.buffs.executionunleashed.name=Laevatinn Unleashed',
    'actors.buffs.executionunleashed.desc=Laevatinn-series weapons gain _+%1$d temporary levels_.\n\nTurns remaining: %2$s',
]

# ===========================================================================
# zh
# ===========================================================================
lines = read(ZH)
before = len(lines)

assert count(lines, PLACEHOLDER) == 3, 'zh 占位键不是 3 行'
drop(lines, '中指 长兄 三个盔甲技能（2026-09-17 占位骨架', 'zh 占位注释')
drop(lines, '「其三」仍是占位', 'zh 其三注释')
# 三行占位键 → 新技能块
hits = [i for i, l in enumerate(lines) if l.startswith(PLACEHOLDER)]
lines[hits[0]:hits[-1] + 1] = esc(ZH_ABILITY)
insert_after(lines, 'actors.hero.talent.keep_in_mind.desc', ZH_TALENT, 'zh 天赋')
insert_after(lines, 'actors.buffs.gritteethbuff.desc', ZH_BUFF, 'zh buff')

assert count(lines, PLACEHOLDER) == 0
assert count(lines, 'actors.hero.abilities.middlefinger.instantexecution.') == 9, \
    'zh instantexecution 键数 = %d' % count(lines, 'actors.hero.abilities.middlefinger.instantexecution.')
assert_no_stray(lines, 'zh')
write(ZH, lines)
print('zh 行数 %d -> %d' % (before, len(lines)))

# ===========================================================================
# en
# ===========================================================================
lines = read(EN)
before = len(lines)

assert count(lines, PLACEHOLDER) == 3, 'en 占位键不是 3 行'
drop(lines, 'placeholder skeletons: slots and icons in place', 'en 占位注释')
drop(lines, 'III is still a placeholder', 'en 其三注释')
hits = [i for i, l in enumerate(lines) if l.startswith(PLACEHOLDER)]
lines[hits[0]:hits[-1] + 1] = esc(EN_ABILITY)
insert_after(lines, 'actors.hero.talent.keep_in_mind.desc', EN_TALENT, 'en 天赋')
insert_after(lines, 'actors.buffs.gritteethbuff.desc', EN_BUFF, 'en buff')

assert count(lines, PLACEHOLDER) == 0
assert count(lines, 'actors.hero.abilities.middlefinger.instantexecution.') == 9, \
    'en instantexecution 键数 = %d' % count(lines, 'actors.hero.abilities.middlefinger.instantexecution.')
assert_no_stray(lines, 'en')
write(EN, lines)
print('en 行数 %d -> %d' % (before, len(lines)))

print('OK')
