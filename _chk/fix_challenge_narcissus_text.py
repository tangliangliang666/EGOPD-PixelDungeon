# -*- coding: utf-8 -*-
"""插入新挑战「水仙追迹」的名称与描述（zh + en 各一份）。

为什么用脚本而不是编辑器：本工程 `.properties` 是 **CRLF**，插入行必须走**字节级** IO，
否则整文件被改写成 LF ⇒ git diff 变成「每行都改了」（本项目已踩过）。

做法：按 `\\r\\n` 切行 → 找到 `challenges.debug_mode_desc=` 那一行 → 在它**之后**插入两行
（名称 + 描述）→ 用 `\\r\\n` 重新拼接。**幂等**：已经存在 `challenges.narcissus_tracing=` 就直接跳过。

文案规格（本项目 properties 的硬规则）：
  · 换行一律写成**字面** `\\n`（反斜杠 + n），值里**不许出现真换行**；
  · 字面 `%` 必须写 `%%`（本文案一个都没有）；
  · `_..._` 成对出现（偶数个下划线），否则渲染出半截斜体。

用法：
  python _chk/fix_challenge_narcissus_text.py            # 应用
  python _chk/fix_challenge_narcissus_text.py --check    # 只报告，不写盘
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MISC = os.path.join(ROOT, 'core/src/main/assets/messages/misc')
ZH = os.path.join(MISC, 'misc_zh.properties')
EN = os.path.join(MISC, 'misc.properties')

ANCHOR = 'challenges.debug_mode_desc='

NAME_ZH = r'challenges.narcissus_tracing=水仙追迹'
DESC_ZH = (
    r'challenges.narcissus_tracing_desc=追随那道白色的剑光：回到旅途开始的地方，也回到它结束的地方。\n\n'
    r'-得分倍率_恒为 0_（本局不计分）\n'
    r'-开局携带_水仙十字圣剑_（已装备在主手）与_财富戒指_（放在背包里，戴不戴随你）\n\n'
    r'圣剑的形态由英雄等级决定，玩家无法手动切换：英雄处于 _1 级_ 时取_芒性_，达到 _30 级_ 时取_荒性_。'
    r'两种特殊形态都会彻底阻断经验值——所以拿到剑的那一刻起，你多半就只能靠装备前进了。'
)
NAME_EN = r'challenges.narcissus_tracing=Narcissus tracing'
DESC_EN = (
    r'challenges.narcissus_tracing_desc=Follow that white blade-light: back to where the journey begins, '
    r'and back to where it ends.\n\n'
    r'-Score multiplier is forced to _0_ (this run is not scored)\n'
    r'-You start with the _Narcissus Cross Sword_ (equipped) and a _Ring of Wealth_ '
    r'(in your backpack, wear it if you like)\n\n'
    r"The sword's aspect follows your level and cannot be switched by hand: the _Mang_ aspect at _level 1_, "
    r'the _Huang_ aspect at _level 30_. Both special aspects cut off experience entirely -- so from the moment '
    r'you take the blade, you will most likely advance on gear alone.'
)

JOBS = [
    (ZH, 'misc_zh.properties', [NAME_ZH, DESC_ZH]),
    (EN, 'misc.properties', [NAME_EN, DESC_EN]),
]

check_only = '--check' in sys.argv
fails = 0

for path, tag0, new_lines in JOBS:
    raw = open(path, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    lines = raw.split(b'\r\n')
    tag = tag0

    if any(l.startswith(b'challenges.narcissus_tracing=') for l in lines):
        print('  [SKIP] %-24s 已存在（幂等跳过）' % tag)
        continue

    idx = [i for i, l in enumerate(lines) if l.startswith(ANCHOR.encode('utf-8'))]
    if len(idx) != 1:
        print('  [FAIL] %-24s 锚点 %s 期望恰好 1 行，实得 %d' % (tag, ANCHOR, len(idx)))
        fails += 1
        continue

    # 文案自检：只查**等号右边**的值部分 —— 键名里的下划线（challenges.narcissus_tracing）
    # 不是强调标记，把它算进去会误判成「下划线不成对」。
    for s in new_lines:
        bad = False
        val = s.split('=', 1)[1] if '=' in s else s
        if '\n' in s or '\r' in s:
            print('  [FAIL] %-24s 文案里出现真换行：%s' % (tag, s[:40]))
            bad = True
        if val.count('_') % 2 != 0:
            print('  [FAIL] %-24s 值里下划线不成对（%d 个）：%s' % (tag, val.count('_'), val[:40]))
            bad = True
        if val.replace('%%', '').count('%') != 0:
            print('  [FAIL] %-24s 值里出现裸 %%（properties 里要写 %%）：%s' % (tag, val[:40]))
            bad = True
        if bad:
            fails += 1
    if fails:
        continue

    i = idx[0]
    out_lines = lines[:i + 1] + [s.encode('utf-8') for s in new_lines] + lines[i + 1:]
    out = b'\r\n'.join(out_lines)

    # 硬核验：CRLF 恰好 +2、行数恰好 +2、UTF-8 可解
    crlf_after = out.count(b'\r\n')
    if crlf_after != crlf_before + 2:
        print('  [FAIL] %-24s CRLF 计数应为 %d，实得 %d' % (tag, crlf_before + 2, crlf_after))
        fails += 1
        continue
    if len(out.split(b'\r\n')) != len(lines) + 2:
        print('  [FAIL] %-24s 行数应为 %d，实得 %d' % (tag, len(lines) + 2, len(out.split(b'\r\n'))))
        fails += 1
        continue
    try:
        out.decode('utf-8')
    except UnicodeDecodeError as e:
        print('  [FAIL] %-24s 写出后不是合法 UTF-8：%s' % (tag, e))
        fails += 1
        continue

    if check_only:
        print('  [WOULD] %-23s 将插入 2 行（--check，不写盘）' % tag)
    else:
        open(path, 'wb').write(out)
        print('  [OK]   %-23s 已插入 2 行（CRLF %d -> %d）' % (tag, crlf_before, crlf_after))

print('')
print('完成：%s' % ('只检查，未写盘' if check_only else '已写盘'))
sys.exit(1 if fails else 0)
