# 一次性补丁（2026-09-24）：把英文 trials.binah/chesed/gebura_desc 对齐到用户改定的**中文基准**。
#
# 背景：用户把这三条中文改成「单段 + 精简」，英文侧仍是旧的多段详版 ⇒ zh/en 结构性分歧。
# 用户指示「以我修改后的中文为基准，不要改动中文描述」⇒ 本脚本**只动 misc.properties（en）**，
# 逐条整行替换为中文的 1:1 英译，段落数（=1）与 `_` 标记数都对齐。
#
# 幂等：整行等于新值即 SKIP。断言：CRLF 数不变（该文件为 CRLF）、行数不变（整行替换）、
# 每条锚点命中恰好 1 次、`_` 成对。
import sys

P = 'core/src/main/assets/messages/misc/misc.properties'

# key -> 新值（单段；不含任何真换行；`_` 必须成对）
NEW = {
    'trials.binah_desc':
        'Items that spawn naturally in the dungeon _never have a positive upgrade level_. '
        'If a levelled item rolls cursed when it is generated, the level it rolled is '
        '_flipped to negative_ -- otherwise its level is set to 0. '
        'Items given by quests are always level _0_.',

    'trials.chesed_desc':
        'All _hostile units_ have _+25%_ max health, and recover _10% of their max health_ '
        'once every _5_ turns. Units worth _0_ experience do not receive this healing.',

    'trials.gebura_desc':
        'All _hostile units_ do not fall when they are about to die. Instead they enter a '
        'period of _invulnerability_ lasting a number of turns '
        '_equal to the experience they give_. '
        'Units worth _0_ experience do not gain the invulnerability.',
}

for k, v in NEW.items():
    assert '\n' not in v and '\r' not in v, k + ' 的新值里混进了真换行'
    us = v.count('_')
    assert us % 2 == 0, '%s 的 _ 标记不成对（%d 个）' % (k, us)

d = open(P, 'rb').read()
crlf_before = d.count(b'\r\n')
assert d.count(b'\n') - crlf_before == 0, '该文件应为纯 CRLF'
lines = d.split(b'\r\n')

done, skip = [], []
for k, v in NEW.items():
    anchor = (k + '=').encode('utf-8')
    idx = [i for i, l in enumerate(lines) if l.startswith(anchor)]
    assert len(idx) == 1, '%s 的锚点命中 %d 行（应为 1）' % (k, len(idx))
    i = idx[0]
    want = (k + '=' + v).encode('utf-8')
    if lines[i] == want:
        skip.append(k)
        continue
    print('[PATCH] %s\n        旧：%s' % (k, lines[i].decode('utf-8')[:110]))
    print('        新：%s' % want.decode('utf-8')[:110])
    lines[i] = want
    done.append(k)

if not done:
    print('[SKIP] 全部已是新值（幂等）')
    sys.exit(0)

out = b'\r\n'.join(lines)
assert out.count(b'\r\n') == crlf_before, 'CRLF 数变了（%d -> %d）' % (crlf_before, out.count(b'\r\n'))
assert out.count(b'\n') - out.count(b'\r\n') == 0, '引入了裸 LF'
open(P, 'wb').write(out)
print('[OK] 已替换 %d 条：%s（另有 %d 条本就一致）' % (len(done), done, len(skip)))
