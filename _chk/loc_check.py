# -*- coding: utf-8 -*-
"""核验本地化文本：重复键 / 新增键齐全 / 带参文本的 % 转义是否合法。"""
import io, re, sys, collections

FILES = {
    'zh-actors': r'D:/PD/core/src/main/assets/messages/actors/actors_zh.properties',
    'en-actors': r'D:/PD/core/src/main/assets/messages/actors/actors.properties',
    'zh-items':  r'D:/PD/core/src/main/assets/messages/items/items_zh.properties',
    'en-items':  r'D:/PD/core/src/main/assets/messages/items/items.properties',
}

def load(path):
    d = collections.OrderedDict()
    dups = []
    with io.open(path, 'r', encoding='utf-8') as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.rstrip('\n').rstrip('\r')
            s = line.lstrip()
            if not s or s[0] in '#!':
                continue
            if '=' not in line:
                print('  [WARN] %s:%d 无 = 号: %s' % (path.split('/')[-1], lineno, line[:60]))
                continue
            k, v = line.split('=', 1)
            k = k.strip()
            if k in d:
                dups.append((k, lineno))
            d[k] = v
    return d, dups

data = {}
for name, p in FILES.items():
    data[name], dups = load(p)
    for k, ln in dups:
        print('[DUP] %s:%d 重复键 %s' % (name, ln, k))

# 新增键：zh / en 必须成对
NEW = [
    'actors.hero.herosubclass.family_betrayer',
    'actors.hero.herosubclass.family_betrayer_short_desc',
    'actors.hero.herosubclass.family_betrayer_desc',
    'actors.hero.talent.effortless_grip.title',
    'actors.hero.talent.effortless_grip.desc',
    'actors.hero.talent.melt_to_death.title',
    'actors.hero.talent.melt_to_death.desc',
    'actors.hero.talent.unwrap.title',
    'actors.hero.talent.unwrap.desc',
    'actors.buffs.melting.name',
    'actors.buffs.melting.desc',
    'actors.buffs.familybetrayal.name',
    'actors.buffs.familybetrayal.desc',
]
for k in NEW:
    for side in ('zh-actors', 'en-actors'):
        if k not in data[side]:
            print('[MISS] %s 缺少 %s' % (side, k))

if 'items.weapon.melee.sealedswordbase.level_note' not in data['zh-items']:
    print('[MISS] zh-items 缺少 level_note')
if 'items.weapon.melee.sealedswordbase.level_note' not in data['en-items']:
    print('[MISS] en-items 缺少 level_note')

# 带参文本的 % 检查：只对「有参数」的键做（这里列出手工确认的带参键）
ARG_KEYS = {
    'actors.buffs.melting.name': [],
    'actors.buffs.melting.desc': ['%1$d', '%2$s'],
    'actors.buffs.familybetrayal.desc': ['%1$d', '%2$s'],
    'actors.buffs.grudgemark.desc': ['%1$d', '%2$d', '%3$s'],
    'items.weapon.melee.sealedswordbase.need_charge_or_hp': ['%1$d', '%2$d'],
    'items.weapon.melee.sealedswordbase.level_note': [],
}
PCT = re.compile(r'%')
for k, args in ARG_KEYS.items():
    for side in ('zh-actors', 'en-actors', 'zh-items', 'en-items'):
        if k not in data[side]:
            continue
        v = data[side][k]
        # 把合法占位符与 %% 去掉，剩下的 % 都是错的
        tmp = v.replace('%%', '')
        for a in args:
            tmp = tmp.replace(a, '')
        leftover = PCT.findall(tmp)
        if leftover:
            print('[FMT] %s 的 %s 里残留未转义的 %% (%d 个): %s' % (side, k, len(leftover), v[:80]))
        # 占位符是否都在
        for a in args:
            if a not in v:
                print('[FMT] %s 的 %s 缺少占位符 %s' % (side, k, a))

print('--- 检查完毕 ---')
print('各文件键数：' + ', '.join('%s=%d' % (n, len(d)) for n, d in data.items()))
