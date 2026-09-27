# 一次性补丁：把 verify_tree_trials_icons.py 的「未绘制帧」相关说明更新为「十帧已全部绘齐」。
#   ① 头部注释 ⑤；
#   ② [⑤] 段在 undrawn 为空时也打印一句 note（否则这段静悄悄，看不出是「全部绘齐」还是「忘了跑」）。
# 幂等：以 MARK 判定，已打过就 SKIP。纯 LF 文件，断言「打补丁前后裸 LF 增量 == 该条替换行数差之和」。
import sys

P = '_chk/verify_tree_trials_icons.py'

PAIRS = [
    (
        '#  ⑤ 未绘制的帧：仅 NOTE（当前按 16x16 占位，画出来是全透明、不报错）。',
        '#  ⑤ 未绘制的帧：仅 NOTE（历史上曾按 16x16 占位；2026-09-24 十帧已全部绘齐，此段应为空）。',
    ),
    (
        "    if undrawn:\n"
        "        print('  note ' + '；'.join(notes))",
        "    if undrawn:\n"
        "        print('  note ' + '；'.join(notes))\n"
        "    else:\n"
        "        print('  note 十帧已全部绘齐，无占位帧（声明尺寸＝实测包围盒）')",
    ),
]

MARK = b'2026-09-24'

d = open(P, 'rb').read()
assert b'\r\n' not in d, '本文件应为纯 LF'

if MARK in d:
    print('[SKIP] 已打过补丁（文件内已出现 %s）' % MARK.decode('utf-8'))
    sys.exit(0)

before_lf = d.count(b'\n')
added_lines = 0
cur = d
for old, new in PAIRS:
    ob, nb = old.encode('utf-8'), new.encode('utf-8')
    n = cur.count(ob)
    assert n == 1, '锚点命中 %d 次（应为 1）：%r' % (n, old[:30])
    cur = cur.replace(ob, nb)
    added_lines += nb.count(b'\n') - ob.count(b'\n')

after_lf = cur.count(b'\n')
print('裸 LF：%d -> %d（增量 %d，应为 %d）' % (before_lf, after_lf, after_lf - before_lf, added_lines))
assert after_lf - before_lf == added_lines, '行尾增量与替换行数差不符'
assert b'\r\n' not in cur, '补丁引入了 CRLF'

open(P, 'wb').write(cur)
print('[OK] 已应用 %d 条替换' % len(PAIRS))
