# -*- coding: utf-8 -*-
"""一次性文案对齐：2% -> 5%、剔除修饰句、zh/en 内容对齐。每处替换断言恰好命中 1 次。"""
import io, sys

EDITS = {
 'actors/actors_zh.properties': [
   # 1) effortless_grip 层级标记补上下划线（其余天赋都是 _+1：_）
   ('actors.hero.talent.effortless_grip.desc=+1：_封印之剑系列的_力量需求 -2_。',
    'actors.hero.talent.effortless_grip.desc=_+1：_封印之剑系列的_力量需求 -2_。'),
   # 2) 转职短描述：2%->5% + 去修饰
   ('自动消耗 _2%_ 的_复仇账簿_充能，换来一次「_刚好挥得动这把剑_」的力量补足。',
    '自动消耗 _5%_ 的_复仇账簿_充能，补足出手所需的力量。'),
   # 3) 转职长描述：2%->5%
   ('会自动消耗 _2%_ 的_复仇账簿_充能，补足本次攻击的力量需求。',
    '会自动消耗 _5%_ 的_复仇账簿_充能，补足本次攻击所需的力量。'),
   # 4) 融化 buff：去掉叙述性开头
   ('actors.buffs.melting.desc=这个单位正在莱瓦汀的烈焰里一点点化开：受到的_火焰伤害_提高 _%1$d%%_。',
    'actors.buffs.melting.desc=受到的_火焰伤害_提高 _%1$d%%_。'),
   # 5) 背叛之力 buff：去掉叙述性开头
   ('actors.buffs.familybetrayal.desc=用账簿里的旧账换来的一次力量：_+%1$d 点力量_，只维持这一次攻击。',
    'actors.buffs.familybetrayal.desc=_+%1$d 点力量_，只维持这一次攻击。'),
 ],
 'actors/actors.properties': [
   # 6) effortless_grip：删掉开头修饰句
   ("actors.hero.talent.effortless_grip.desc=Wielding a blade well is a matter of familiarity, not of force.\\n\\n_+1:_ the Laevateinn series'",
    "actors.hero.talent.effortless_grip.desc=_+1:_ the Laevateinn series'"),
   # 7) melt_to_death：删修饰句；正文压成与 zh 同构
   ("actors.hero.talent.melt_to_death.desc=Whatever stands in Laevateinn's way ends up as a puddle.\\n\\nHits from the Laevateinn series inflict _Melting_ on the target — lasting _8 turns_, refreshed rather than stacked — and a melting enemy takes _50%_ more _fire damage_.",
    "actors.hero.talent.melt_to_death.desc=_+1:_ Hits from the Laevateinn series inflict _Melting_ on the target; a melting enemy takes _50%_ more _fire damage_."),
   ("On top of that, while _Laevateinn_ is in your _main hand_, every enemy within the _5×5 circular area_ around you is dragged straight into _Melting_.",
    "In addition, while _Laevateinn_ is in your _main hand_, every enemy within the _5×5 circular area_ around you is directly inflicted with _Melting_."),
   ("Enemies _immune to fire_ cannot benefit from a fire bonus, so _all_ damage they take is increased by the same fraction instead.",
    "Enemies _immune to fire_ instead take the same fraction as additional damage."),
   # 8) unwrap：删修饰句与脚注
   ("actors.hero.talent.unwrap.desc=Every layer of the seal that comes off leaves the blade a little keener.\\n\\n_+1:_ the Laevateinn series gains",
    "actors.hero.talent.unwrap.desc=_+1:_ the Laevateinn series gains"),
   ("_+3:_ on top of +1 and +2, _every unsealing requirement is removed_ — no ledger charge and no low health needed.\\n\\n(The extra levels only affect the damage rolled with the weapon; they are never written into the weapon's own level.)",
    "_+3:_ on top of +1 and +2, _every unsealing requirement is removed_."),
   # 9) 转职短描述（en）：2%->5% + 去修饰
   ("The _Family Betrayer_ burns the family ledger for power: whenever he is _not_ in a state of _rancor_, attacking with a weapon of the _Laevateinn series_ automatically spends _2%_ of his _Revenge Ledger_ charge for a strength top-up that lasts for that one attack.",
    "The _Family Betrayer_ specialises in the _Laevateinn series_: while he is _not_ in a state of _rancor_, attacking with a _Laevateinn-series_ weapon automatically spends _5%_ of his _Revenge Ledger_ charge to cover the strength needed for that blow."),
   # 10) 转职长描述（en）：删修饰句与额外段落，2%->5%
   ("The one who turns his back on the family keeps no grudges on anyone's behalf — he burns the whole ledger as firewood.\\n\\nAs long as he is _not_ in a state of _rancor_, attacking with a _Laevateinn-series_ weapon (sealed sword / first-stage unsealed sword / second-stage unsealed sword / Laevateinn) automatically spends _2%_ of _Revenge Ledger_ charge _before_ the blow, buying a _strength top-up_ that is _just enough to swing that blade_ — not a point more, and only for _that one attack_.\\n\\nThree edges: if his strength _already suffices_ nothing is spent at all, during _rancor_ the feature never triggers (it already grants plenty of strength), and if he cannot afford the _2%_ nothing happens — the attack simply resolves as an under-strength swing.\\n\\nThe whole subclass revolves around the _Laevateinn series_: the talent _Effortless Grip_ lowers its strength requirement, _Melt to Death_ makes whatever it strikes dissolve in flame, and _Unwrap_ grants an extra level for every layer of the seal broken.",
    "The _Family Betrayer_ specialises in the use of the _Laevateinn series_.\\n\\nAs long as he is _not_ in a state of _rancor_, attacking with a _Laevateinn-series_ weapon (sealed sword / first-stage unsealed sword / second-stage unsealed sword / Laevateinn) automatically spends _5%_ of _Revenge Ledger_ charge _before_ the blow to cover that attack's strength requirement, provided his strength falls short of it."),
   # 11) 融化 buff（en）：去叙述性开头
   ("actors.buffs.melting.desc=This creature is coming apart in Laevateinn's flames: it takes _%1$d%%_ more _fire damage_.",
    "actors.buffs.melting.desc=Takes _%1$d%%_ more _fire damage_."),
   # 12) 背叛之力 buff（en）：去叙述性开头
   ("actors.buffs.familybetrayal.desc=Strength bought with old debts out of the ledger: _+%1$d strength_, for this one attack only.",
    "actors.buffs.familybetrayal.desc=_+%1$d strength_, for this one attack only."),
 ],
}

fail = False
for path, pairs in EDITS.items():
    txt = io.open(path, 'r', encoding='utf-8', newline='').read()
    for old, new in pairs:
        n = txt.count(old)
        tag = 'OK ' if n == 1 else 'BAD'
        if n != 1:
            fail = True
        print('%s  x%d  %s' % (tag, n, old[:70].replace('\n', '\\n')))
        if n == 1:
            txt = txt.replace(old, new)
    if not fail:
        io.open(path, 'w', encoding='utf-8', newline='').write(txt)
        print('WROTE ' + path)
    else:
        print('SKIP  ' + path + ' (有未命中项，未写盘)')
        break

sys.exit(1 if fail else 0)
