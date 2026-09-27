# -*- coding: utf-8 -*-
"""
一次性补丁（幂等）：把「水仙追迹」挑战描述里的「圣剑已装备在主手」改成「圣剑放在背包里」。

背景（2026-09-24）：用户要求开局发的「水仙十字圣剑」放进背包，而不是替换初始武器装备。
HeroClass.initHero 里发放方式已改，描述必须同步，否则玩家看到的是旧说法。

写这个脚本而不是直接编辑 .properties 的原因（详见 skill egopd-source-verify「常见故障」）：
.properties 的换行是**字面量转义**（反斜杠 + n，两个字符）。任何一层转义被还原成真换行，
entry 就会断成两截、续行没有 `=`，游戏里「描述只剩第一段」且**不报任何错**。
所以这里的反斜杠一律用 chr(92) 现拼，绝不写字面量。

用法：python _chk/fix_narcissus_backpack_text.py
"""

import sys

BS = chr(92)          # 反斜杠
NL = BS + 'n'         # .properties 里的字面量换行转义
PY = 'C:/Users/14675/.workbuddy/binaries/python/versions/3.13.12/python.exe'

KEY = 'challenges.narcissus_tracing_desc='

ZH_NEW = (KEY +
    '追随那道白色的剑光：回到旅途开始的地方，也回到它结束的地方。' + NL + NL +
    '-得分倍率_恒为 0_（本局不计分）' + NL +
    '-开局携带_水仙十字圣剑_（放在背包里，装不装上主手随你）与_财富戒指_（也放在背包里，戴不戴随你）' + NL + NL +
    '圣剑的形态由英雄等级决定，玩家无法手动切换：英雄处于 _1 级_ 时取_芒性_，达到 _30 级_ 时取_荒性_。'
    '两种特殊形态都会彻底阻断经验值——所以装上剑的那一刻起，你多半就只能靠装备前进了。')

EN_NEW = (KEY +
    'Follow that white blade-light: back to where the journey begins, and back to where it ends.' + NL + NL +
    '-Score multiplier is forced to _0_ (this run is not scored)' + NL +
    '-You start with the _Narcissus Cross Sword_ and a _Ring of Wealth_, both _in your backpack_ '
    '(equip or wear them if you like)' + NL + NL +
    "The sword's aspect follows your level and cannot be switched by hand: the _Mang_ aspect at _level 1_, "
    'the _Huang_ aspect at _level 30_. Both special aspects cut off experience entirely -- so from the moment '
    'you equip the blade, you will most likely advance on gear alone.')

TARGETS = [
    ('core/src/main/assets/messages/misc/misc_zh.properties', ZH_NEW, '放在背包里，装不装上主手随你'),
    ('core/src/main/assets/messages/misc/misc.properties',    EN_NEW, 'both _in your backpack_'),
]

#旧文案里必须彻底消失的片段（反例自测用）
GONE = ['已装备在主手', '(equipped)']


def patch(path, new_line, marker):
    raw = open(path, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    bare_before = raw.count(b'\n') - crlf_before
    text = raw.decode('utf-8')

    if marker in text:
        print('[SKIP] %s ：已是新文案' % path)
        return 'skip'

    lines = text.split('\r\n')
    hits = [i for i, l in enumerate(lines) if l.startswith(KEY)]
    if len(hits) != 1:
        print('[FAIL] %s ：期望恰好 1 行 %s，实得 %d' % (path, KEY, len(hits)))
        return 'fail'

    i = hits[0]
    old_line = lines[i]
    #旧行必须真的是旧文案（防止锚点误解），否则说明文件已被别处改过
    if '已装备在主手' not in old_line and 'both _in your backpack_' not in old_line and '(equipped)' not in old_line:
        print('[FAIL] %s ：锚点行不含任何已知旧文案，拒绝改动' % path)
        print('       实际内容 = %s' % old_line[:120])
        return 'fail'

    lines[i] = new_line
    out = '\r\n'.join(lines)

    #落盘自检一：断行必须仍是字面量转义，真换行不得出现
    for piece in [p for p in new_line.split(KEY)[1].split(NL) if p]:
        if '\n' in piece or '\r' in piece:
            print('[FAIL] %s ：新行里出现了真换行（转义被还原）' % path)
            return 'fail'

    raw2 = out.encode('utf-8')
    crlf_after = raw2.count(b'\r\n')
    bare_after = raw2.count(b'\n') - crlf_after

    #行数不变（同一行原地替换）
    if crlf_after != crlf_before:
        print('[FAIL] %s ：CRLF 计数变了 %d -> %d（应当原地替换、行数不变）'
              % (path, crlf_before, crlf_after))
        return 'fail'
    if bare_after != bare_before:
        print('[FAIL] %s ：出现了裸 LF %d -> %d' % (path, bare_before, bare_after))
        return 'fail'

    open(path, 'wb').write(raw2)
    print('[OK]   %s ：1 行已替换（CRLF %d 不变，裸 LF %d）'
          % (path, crlf_after, bare_after))
    return 'ok'


def main():
    results = [patch(p, n, m) for p, n, m in TARGETS]
    if 'fail' in results:
        print('\n有文件未通过，请人工检查。')
        return 1

    #收尾断言：旧文案片段必须彻底退场
    bad = 0
    for path, _, _ in TARGETS:
        text = open(path, 'rb').read().decode('utf-8')
        hits = [i for i, l in enumerate(text.split('\r\n')) if l.startswith(KEY)]
        body = text.split('\r\n')[hits[0]] if len(hits) == 1 else ''
        for g in GONE:
            if g in body:
                print('[FAIL] %s ：旧片段仍在：%s' % (path, g))
                bad += 1
    if bad:
        return 1
    print('\n旧文案片段已全部退场。')
    return 0


if __name__ == '__main__':
    sys.exit(main())
