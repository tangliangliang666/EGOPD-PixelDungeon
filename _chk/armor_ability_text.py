# -*- coding: utf-8 -*-
"""一次性补齐「咬紧牙关 / 永不遗忘」两个盔甲技能的 zh+en 文案，并顺手修掉两处文本缺失。

约定（与项目既有 properties 一致的写法）：
  * 纯 LF、无 BOM、不转义非 ASCII；
  * 形如 `\\n` 的两字符换行序列由 Java 侧还原，所以脚本里写成字面 ``\\n``；
  * 每处插入/替换都带命中数断言，命中数不对就整体不落盘（避免半途而废留下坏文件）。
"""
import io
import sys

ROOT = 'core/src/main/assets/messages/actors/'
ZH = ROOT + 'actors_zh.properties'
EN = ROOT + 'actors.properties'

errors = []


def load(path):
    with io.open(path, encoding='utf-8') as f:
        return f.read().split('\n')


def save(path, lines):
    with io.open(path, 'w', encoding='utf-8', newline='') as f:
        f.write('\n'.join(lines))


def replace_block(lines, first_key, last_key, new_block, where):
    """把 first_key 所在行到 last_key 所在行（含）整段替换成 new_block（行列表）。"""
    start = end = None
    for i, l in enumerate(lines):
        if start is None and l.startswith(first_key):
            start = i
        elif start is not None and l.startswith(last_key):
            end = i
            break
    if start is None or end is None or end < start:
        errors.append('%s: 找不到替换区间 %s .. %s' % (where, first_key, last_key))
        return lines
    return lines[:start] + new_block + lines[end + 1:]


def insert_after(lines, anchor_key, new_block, where):
    """在 anchor_key 所在行之后插入 new_block（行列表），要求锚点唯一。"""
    hits = [i for i, l in enumerate(lines) if l.startswith(anchor_key + '=')]
    if len(hits) != 1:
        errors.append('%s: 锚点 %s 命中 %d 次（应为 1）' % (where, anchor_key, len(hits)))
        return lines
    at = hits[0] + 1
    return lines[:at] + new_block + lines[at:]


# ===========================================================================
# 1) 中指长兄两个盔甲技能：把占位键换成正式键
# ===========================================================================
ZH_ABILITY = [
    '# ---- 中指 长兄 盔甲技能（2026-09-17：「咬紧牙关」/「永不遗忘」已按设计实装；「其三」仍是占位） ----',
    'actors.hero.abilities.middlefinger.gritteeth.name=咬紧牙关',
    'actors.hero.abilities.middlefinger.gritteeth.short_desc=血量归零后 _5回合_ 内_不会死亡_；_解离射线_可以击穿这一判定。',
    'actors.hero.abilities.middlefinger.gritteeth.desc=中指长兄咬紧牙关：_5回合_内血量归零也_不会死亡_。\\n\\n_解离射线_（邪能魔眼的射线、最终boss 的射线等）可以击穿这一免疫死亡的判定。',
    'actors.hero.abilities.middlefinger.gritteeth.cast=中指长兄咬紧了牙关。',
    'actors.hero.abilities.middlefinger.neverforget.name=永不遗忘',
    'actors.hero.abilities.middlefinger.neverforget.short_desc=冲向目标，落地后对落点周围 _3×3_ 内的所有目标各发动一次攻击。',
    'actors.hero.abilities.middlefinger.neverforget.desc=中指长兄冲向目标，落地后对落点周围 _3×3_ 内的所有目标各发动一次攻击。\\n\\n范围内有_友方单位_时，友方单位受到 _100%_ 伤害，敌方单位受到 _200%_ 伤害。',
    'actors.hero.abilities.middlefinger.neverforget.prompt=选择要冲向的地格',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.name=中指技能·其三（占位）',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.short_desc=_占位技能_：槽位与图标已就位，行为暂沿用战士的「坚忍」。',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.desc=_占位技能_，尚无正式设计；行为暂沿用战士的「坚忍」。',
]

EN_ABILITY = [
    '# ---- Middle Finger armour abilities (2026-09-17: "Grit Teeth" / "Never Forget" are implemented; III is still a placeholder) ----',
    'actors.hero.abilities.middlefinger.gritteeth.name=grit teeth',
    'actors.hero.abilities.middlefinger.gritteeth.short_desc=For _5 turns_ you _cannot die_ at _0 HP_; _disintegration rays_ pierce this.',
    'actors.hero.abilities.middlefinger.gritteeth.desc=The Middle Finger clenches his teeth: for _5 turns_ you _cannot die_, even at _0 HP_.\\n\\n_Disintegration rays_ (the Evil Eye\'s ray, the final boss\'s rays, and the like) pierce this death immunity.',
    'actors.hero.abilities.middlefinger.gritteeth.cast=The Middle Finger clenches his teeth.',
    'actors.hero.abilities.middlefinger.neverforget.name=never forget',
    'actors.hero.abilities.middlefinger.neverforget.short_desc=Dash to a target, then attack every target within _3×3_ of where you land.',
    'actors.hero.abilities.middlefinger.neverforget.desc=The Middle Finger dashes to a target, then attacks every target within a _3×3_ area around where he lands.\\n\\nIf an _ally_ is in the area, allies take _100%_ damage and enemies take _200%_ damage.',
    'actors.hero.abilities.middlefinger.neverforget.prompt=Choose the tile to dash to',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.name=Middle Finger Ability III (WIP)',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.short_desc=_Placeholder ability_: the slot and icon are in place, behaviour is temporarily borrowed from the Warrior\'s _Endure_.',
    'actors.hero.abilities.middlefinger.middlefingerabilitythree.desc=A _placeholder ability_ with no final design yet; behaviour is temporarily borrowed from the Warrior\'s _Endure_.',
]

# ===========================================================================
# 2) 六个 T4 天赋
# ===========================================================================
ZH_TALENTS = [
    '# ---- 中指 长兄 盔甲技能「咬紧牙关」的 T4 天赋（337/338/339）----',
    'actors.hero.talent.beast_will.title=野兽意志',
    'actors.hero.talent.beast_will.desc=_+1：_「咬紧牙关」的持续时间 _+3回合_。\\n\\n_+2：_持续时间 _+5回合_。\\n\\n_+3：_持续时间 _+10回合_。',
    'actors.hero.talent.near_death_fury.title=濒亡狂怒',
    'actors.hero.talent.near_death_fury.desc=_+1：_「咬紧牙关」持续期间，血量_等于0_时，中指长兄造成的伤害 _+100%_。\\n\\n_+2：_造成的伤害 _+200%_。\\n\\n_+3：_造成的伤害 _+300%_。',
    'actors.hero.talent.desperate_burst.title=绝境迫发',
    'actors.hero.talent.desperate_burst.desc=_+1：_「咬紧牙关」的持续时间结束后，回复 _5%_ 最大生命值。\\n\\n_+2：_回复 _10%_。\\n\\n_+3：_回复 _15%_。',
    '# ---- 中指 长兄 盔甲技能「永不遗忘」的 T4 天赋（340/341/342）----',
    'actors.hero.talent.family_heart.title=家人的心意',
    'actors.hero.talent.family_heart.desc=_+1：_「永不遗忘」范围内_有友方单位_时，对友方单位造成的伤害提升为 _200%_，对敌方单位提升为 _300%_。\\n\\n_+2：_对友方单位 _300%_，对敌方单位 _400%_。\\n\\n_+3：_对友方单位改为_必定击杀_，对敌方单位 _600%_。\\n\\n范围内_没有友方单位_时，本天赋_不生效_。',
    'actors.hero.talent.make_a_scene.title=大闹一场吧',
    'actors.hero.talent.make_a_scene.desc=_+1：_「永不遗忘」的攻击范围扩大为 _5×5_ 的圆形范围。\\n\\n_+2：_扩大为 _7×7_ 的圆形范围。\\n\\n_+3：_扩大为 _9×9_ 的圆形范围。',
    'actors.hero.talent.keep_in_mind.title=我会牢记在心',
    'actors.hero.talent.keep_in_mind.desc=_+1：_「永不遗忘」_击杀了友方单位_时，获得 _5回合_ 的_神器充能_。\\n\\n_+2：_获得 _10回合_ 的神器充能。\\n\\n_+3：_获得 _15回合_ 的神器充能。',
]

EN_TALENTS = [
    '# ---- T4 talents of the "Grit Teeth" armour ability (337/338/339) ----',
    'actors.hero.talent.beast_will.title=beast will',
    'actors.hero.talent.beast_will.desc=_+1:_ the duration of _Grit Teeth_ is _increased by 3 turns_.\\n\\n_+2:_ increased by _5 turns_.\\n\\n_+3:_ increased by _10 turns_.',
    'actors.hero.talent.near_death_fury.title=near-death fury',
    'actors.hero.talent.near_death_fury.desc=_+1:_ while _Grit Teeth_ is active and you are at _exactly 0 HP_, the damage you deal is _+100%_.\\n\\n_+2:_ damage dealt is _+200%_.\\n\\n_+3:_ damage dealt is _+300%_.',
    'actors.hero.talent.desperate_burst.title=desperate burst',
    'actors.hero.talent.desperate_burst.desc=_+1:_ when _Grit Teeth_ ends, restore _5%_ of your maximum health.\\n\\n_+2:_ restore _10%_.\\n\\n_+3:_ restore _15%_.',
    '# ---- T4 talents of the "Never Forget" armour ability (340/341/342) ----',
    'actors.hero.talent.family_heart.title=heart of family',
    'actors.hero.talent.family_heart.desc=_+1:_ when an _ally_ is within the area of _Never Forget_, damage dealt to allies becomes _200%_ and damage dealt to enemies becomes _300%_.\\n\\n_+2:_ allies _300%_, enemies _400%_.\\n\\n_+3:_ allies are _instantly killed_, enemies take _600%_.\\n\\nThis talent does _nothing_ if there is _no ally_ in the area.',
    'actors.hero.talent.make_a_scene.title=make a scene',
    'actors.hero.talent.make_a_scene.desc=_+1:_ the area of _Never Forget_ grows to a _5×5 circular area_.\\n\\n_+2:_ a _7×7 circular area_.\\n\\n_+3:_ a _9×9 circular area_.',
    'actors.hero.talent.keep_in_mind.title=i will keep it in mind',
    'actors.hero.talent.keep_in_mind.desc=_+1:_ if _Never Forget_ _kills an ally_, you gain _5 turns_ of _artifact recharging_.\\n\\n_+2:_ _10 turns_ of artifact recharging.\\n\\n_+3:_ _15 turns_ of artifact recharging.',
]

# ===========================================================================
# 3) 状态 buff 文案
# ===========================================================================
ZH_BUFF = [
    'actors.buffs.gritteethbuff.name=咬紧牙关',
    'actors.buffs.gritteethbuff.desc=血量归零也_不会死亡_；_解离射线_可以击穿这一判定。\\n\\n剩余回合：%s',
]
EN_BUFF = [
    'actors.buffs.gritteethbuff.name=grit teeth',
    'actors.buffs.gritteethbuff.desc=You _cannot die_, even at _0 HP_; _disintegration rays_ pierce this.\\n\\nTurns remaining: %s.',
]

# ===========================================================================
# 4) 切换武器的冷却 buff（Talent.HandyToyCooldown）——文本缺失，补上
# ===========================================================================
ZH_COOLDOWN = [
    'actors.hero.talent$handytoycooldown.name=趁手玩具-冷却',
    'actors.hero.talent$handytoycooldown.desc=你刚刚免费切换过主副武器，需要等待冷却结束才能再次免费切换。\\n\\n剩余冷却：%s回合',
]
EN_COOLDOWN = [
    'actors.hero.talent$handytoycooldown.name=handy toy cooldown',
    'actors.hero.talent$handytoycooldown.desc=You have recently swapped weapons for free, and must wait before swapping for free again.\\n\\nTurns remaining: %s.',
]

# ===========================================================================
# 执行
# ===========================================================================
for path, ability, talents, buff, cooldown, where in [
    (ZH, ZH_ABILITY, ZH_TALENTS, ZH_BUFF, ZH_COOLDOWN, 'zh'),
    (EN, EN_ABILITY, EN_TALENTS, EN_BUFF, EN_COOLDOWN, 'en'),
]:
    lines = load(path)
    before = len(lines)

    lines = replace_block(lines,
                          'actors.hero.abilities.middlefinger.middlefingerabilityone.name',
                          'actors.hero.abilities.middlefinger.middlefingerabilitythree.desc',
                          ability, where + '/ability')

    lines = insert_after(lines, 'actors.hero.talent.unwrap.desc', talents, where + '/talents')
    lines = insert_after(lines, 'actors.buffs.familybetrayal.desc', buff, where + '/buff')

    # 冷却键的落点：zh 有 bodytheatercooldown，en 没有 ⇒ 退回最后一个 cooldown 键
    anchor = 'actors.hero.talent$bodytheatercooldown.desc'
    if not any(l.startswith(anchor + '=') for l in lines):
        anchor = 'actors.hero.talent$lethalhastecooldown.desc'
    lines = insert_after(lines, anchor, cooldown, where + '/cooldown')

    print('%-3s 行数 %d -> %d' % (where, before, len(lines)))
    if not errors:
        save(path, lines)

if errors:
    print('!! 未落盘，问题如下：')
    for e in errors:
        print('   -', e)
    sys.exit(1)

print('OK')
