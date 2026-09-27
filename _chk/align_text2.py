# -*- coding: utf-8 -*-
"""补丁 2：修 /n/n 换行笔误（9 处）+ 去掉 everlasting_grudge 英文的修饰性开头。"""
import io, sys

p = 'actors/actors.properties'
t = io.open(p, 'r', encoding='utf-8', newline='').read()

n = t.count('/n/n')
print('find /n/n  x%d' % n)
t = t.replace('/n/n', '\\n\\n')

old = 'actors.hero.talent.everlasting_grudge.desc=The Middle Finger never settles a debt on memory alone.\\n\\n_+1:_'
new = 'actors.hero.talent.everlasting_grudge.desc=_+1:_'
c = t.count(old)
print('find everlasting_grudge flavor  x%d' % c)
t = t.replace(old, new)

if n == 9 and c == 1:
    io.open(p, 'w', encoding='utf-8', newline='').write(t)
    print('WROTE ' + p)
else:
    print('SKIP (命中数不符，未写盘)')
    sys.exit(1)
