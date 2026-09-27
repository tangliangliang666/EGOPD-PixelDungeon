# -*- coding: utf-8 -*-
"""docs/features.md：2% -> 5%（预支充能），并给解除钩子补上「无敌早退」的例外说明。"""
import io, sys

p = 'docs/features.md'
t = io.open(p, 'r', encoding='utf-8', newline='').read()

E = [
  ('消耗 **2%**（`CHARGE_COST`）账簿充能', '消耗 **5%**（`CHARGE_COST`）账簿充能'),
  ('3. 充能不足 2% → ', '3. 充能不足 5% → '),
  ('- **解除**：`Talent.onHeroAttackResolved`（命中与落空**两条路都会走到**）→ 立刻 `detach`。\n  不放这里就会变成"常驻力量"，不再是"仅一次攻击"。',
   '- **解除**：`Talent.onHeroAttackResolved`（命中与落空**两条路都会走到**；只有"目标无敌、攻击根本没打出去"\n  那条早退路径不走，所以那条路上**压根不收费**，见本文「八、补记」）→ 立刻 `detach`。\n  不放这里就会变成"常驻力量"，不再是"仅一次攻击"。'),
  ('2. 力量不足时挥剑会扣 2% 账簿充能', '2. 力量不足时挥剑会扣 5% 账簿充能'),
  ('结果是**玩家会白掉 2% 充能', '结果是**玩家会白掉 5% 充能'),
]

fail = False
for old, new in E:
    n = t.count(old)
    print(('OK ' if n == 1 else 'BAD') + '  x%d  %s' % (n, old[:60].replace('\n', '\\n')))
    if n != 1:
        fail = True
    else:
        t = t.replace(old, new)

if fail:
    print('SKIP (有未命中项)')
    sys.exit(1)
io.open(p, 'w', encoding='utf-8', newline='').write(t)
print('WROTE ' + p)
