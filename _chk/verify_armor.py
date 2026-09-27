# -*- coding: utf-8 -*-
"""本轮（咬紧牙关 / 永不遗忘 + 6 个天赋 + 2 个文本修复）的文本回归核验。

核验项：
  1. 重复键（duplicate key）
  2. 本轮新增键在 zh / base 两侧是否成对存在
  3. 换行只认 \n：任何 /n 写法都判 FAIL
  4. 带参文本的占位符与 Java 实参是否匹配（只对真会被 String.format 的键）
  5. 不带参文本中出现的字面 % 不报错（Messages.get 在 args.length==0 时不分式化）
"""
import io, re, collections, sys

FILES = {
    'zh': r'D:/PD/core/src/main/assets/messages/actors/actors_zh.properties',
    'en': r'D:/PD/core/src/main/assets/messages/actors/actors.properties',
}

# 本轮新增/改动的键 -> Java 侧实际传入的格式化参数（None = 不分式化）
ROUND_KEYS = {
    'actors.hero.abilities.middlefinger.gritteeth.name':                  None,
    'actors.hero.abilities.middlefinger.gritteeth.short_desc':            None,
    'actors.hero.abilities.middlefinger.gritteeth.desc':                  None,
    'actors.hero.abilities.middlefinger.gritteeth.cast':                  None,
    'actors.hero.abilities.middlefinger.neverforget.name':                None,
    'actors.hero.abilities.middlefinger.neverforget.short_desc':          None,
    'actors.hero.abilities.middlefinger.neverforget.desc':                None,
    'actors.hero.abilities.middlefinger.neverforget.prompt':              None,
    #注：2026-09-17「其三」占位改为 InstantExecution（即刻处刑[莱瓦汀]），
    #    那三个 middlefingerabilitythree.* 键已删除，其核验移入 _chk/InstExecLocCheck.java
    'actors.hero.talent.beast_will.title':                                None,
    'actors.hero.talent.beast_will.desc':                                 None,
    'actors.hero.talent.near_death_fury.title':                           None,
    'actors.hero.talent.near_death_fury.desc':                            None,
    'actors.hero.talent.desperate_burst.title':                           None,
    'actors.hero.talent.desperate_burst.desc':                            None,
    'actors.hero.talent.family_heart.title':                              None,
    'actors.hero.talent.family_heart.desc':                               None,
    'actors.hero.talent.make_a_scene.title':                              None,
    'actors.hero.talent.make_a_scene.desc':                               None,
    'actors.hero.talent.keep_in_mind.title':                              None,
    'actors.hero.talent.keep_in_mind.desc':                               None,
    # 修复项 1：切换武器冷却 buff（FlavourBuff.desc -> dispTurns() 一个 String）
    'actors.hero.talent$handytoycooldown.name':                           None,
    'actors.hero.talent$handytoycooldown.desc':                           ['%s'],
    # 新增免死 buff（FlavourBuff.desc -> dispTurns() 一个 String）
    'actors.buffs.gritteethbuff.name':                                    None,
    'actors.buffs.gritteethbuff.desc':                                    ['%s'],
    # 修复项 2：融化 buff（desc() 传 (int) 百分比 + dispTurns()）
    'actors.buffs.melting.name':                                          None,
    'actors.buffs.melting.desc':                                          ['%1$d', '%2$s'],
}

JPCT = re.compile(r'%(\d+\$)?[-#+ 0,(]*\d*(?:\.\d+)?[bBhHsScCdoxXeEfgGaAtT%n]')
FAIL = 0

def fail(msg):
    global FAIL
    FAIL += 1
    print('FAIL  ' + msg)

def load(path):
    d, dups = collections.OrderedDict(), []
    with io.open(path, 'r', encoding='utf-8') as f:
        for lineno, raw in enumerate(f, 1):
            line = raw.rstrip('\n').rstrip('\r')
            s = line.lstrip()
            if not s or s[0] in '#!':
                continue
            if '=' not in line:
                fail('无 = 号 %s:%d  %s' % (path.split('/')[-1], lineno, line[:60]))
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
        fail('[DUP] %s:%d 重复键 %s' % (name, ln, k))
    print('%s 键数 = %d' % (name, len(data[name])))

# 2. zh / base 成对
for k in ROUND_KEYS:
    for side in ('zh', 'en'):
        if k not in data[side]:
            fail('[MISS] %s 缺少 %s' % (side, k))

# 3/4/5. 逐条内容检查
for k, args in ROUND_KEYS.items():
    for side in ('zh', 'en'):
        v = data[side].get(k)
        if v is None:
            continue
        tag = '%s %s' % (side, k)
        # 3. 换行只认 \n
        if re.search(r'(?<!\\)/n', v) or '\\/n' in v:
            fail('[NL] %s 出现 /n 写法: %s' % (tag, v[:90]))
        if re.search(r'\\n\\\\n', v):
            fail('[NL] %s 出现 \\\\n 双反斜杠: %s' % (tag, v[:90]))
        # 3b. 转义序列合法性（Properties / I18NBundle 解析规则）
        i = 0
        while i < len(v):
            if v[i] == '\\':
                if i + 1 >= len(v):
                    fail('[ESC] %s 以孤立反斜杠结尾(会被当作续行): %s' % (tag, v))
                    break
                c = v[i + 1]
                if c in 'ntrf\\':
                    i += 2
                    continue
                if c == 'u':
                    hexpart = v[i + 2:i + 6]
                    if len(hexpart) == 4 and all(h in '0123456789abcdefABCDEF' for h in hexpart):
                        i += 6
                        continue
                    fail('[ESC] %s 的 \\u 转义不合法: %s' % (tag, v))
                    i += 2
                    continue
                fail('[ESC] %s 出现未知转义 \\%s : %s' % (tag, c, v[:90]))
                i += 2
                continue
            i += 1
        # 4. 带参键：占位符与 Java 实参一致
        if args is not None:
            # props 文件里 %% 是字面 %，不是占位符
            tmp = v.replace('%%', '')
            found = [m.group(0) for m in JPCT.finditer(tmp)]
            for a in args:
                if a not in found:
                    fail('[FMT] %s 缺少占位符 %s (实际: %s)' % (tag, a, found))
            stray = [x for x in found if x not in args]
            if stray:
                fail('[FMT] %s 有多余占位符 %s' % (tag, stray))
        else:
            # 5. 不分式化的键：出现 %1$d / %s 这类「形似占位符」的写法才算可疑
            if re.search(r'%\d+\$', v) or re.search(r'%[sd]\b', v):
                fail('[FMT] %s 是不分式化文本，却含占位符样式: %s' % (tag, v[:90]))

print('--- 检查完毕 ---')
if FAIL == 0:
    print('ALL PASS (%d 个键 x 2 语言)' % len(ROUND_KEYS))
else:
    print('共 %d 处问题' % FAIL)
sys.exit(1 if FAIL else 0)
