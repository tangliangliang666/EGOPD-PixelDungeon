# -*- coding: utf-8 -*-
"""GEBURA 削弱（2026-09-24）：玩家文案里「无敌期间照常行动」→「除了刚被打成濒死的那一回合会僵直」。

为什么用脚本：本工程 `.properties` 是 **CRLF**，替换必须走**字节级** IO，
否则整文件被改写成 LF ⇒ git diff 变成「每行都改了」（本项目已踩过）。

做法：**只替换那一小段措辞**（不是整行替换）—— 用户日后在同一行上改别的句子不会被覆盖。
**幂等**：新措辞已在 ⇒ SKIP；旧措辞与新措辞都不在 ⇒ FAIL（不静默通过）。

用法：
  python _chk/fix_gebura_stagger_text.py            # 应用
  python _chk/fix_gebura_stagger_text.py --check    # 只报告，不写盘
"""
import os
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(HERE)
MISC = os.path.join(ROOT, 'core/src/main/assets/messages/misc')
ZH = os.path.join(MISC, 'misc_zh.properties')
EN = os.path.join(MISC, 'misc.properties')

# ⚠️ 这两段是**普通 Python 字符串**（不是 r''），但里面没有任何反斜杠 ⇒ 落盘就是原样文本。
#    绝不要把 `\n` 真换行写进来：`.properties` 的换行必须是**字面** 反斜杠+n。
ZH_OLD = '并且仍会照常行动。'
ZH_NEW = '并且照常行动——唯独刚被打成濒死的那一回合它会_僵直不动_：愣过一个回合之后才恢复正常。'

EN_OLD = 'and they keep taking their turns as usual.'
EN_NEW = ('and they keep taking their turns as usual -- except for the very turn they are stricken down, '
          'which they spend _staggering in place_ instead, only acting again from the following turn onward.')

# 幂等标记必须**两份文件都会出现**的公共子串（否则会出现「一份 SKIP、另一份被再改一遍」）
MARK_ZH = '僵直不动'
MARK_EN = 'staggering in place'

JOBS = [
    (ZH, 'trials.gebura_desc', ZH_OLD, ZH_NEW, MARK_ZH),
    (EN, 'trials.gebura_desc', EN_OLD, EN_NEW, MARK_EN),
]

check_only = '--check' in sys.argv
fails = 0

for path, key, old, new, mark in JOBS:
    tag = os.path.basename(path) + ' ' + key
    raw = open(path, 'rb').read()
    crlf_before = raw.count(b'\r\n')
    text = raw.decode('utf-8')

    if mark in text:
        print('  [SKIP] %-46s 已是新文案（幂等跳过）' % tag)
        continue

    n_old = text.count(old)
    if n_old != 1:
        print('  [FAIL] %-46s 旧措辞期望恰好 1 处，实得 %d 处' % (tag, n_old))
        fails += 1
        continue

    # 只改 key 那一行（避免同名措辞在别的键里被误伤）
    lines = text.split('\r\n')
    hit = [i for i, l in enumerate(lines) if l.startswith(key + '=')]
    if len(hit) != 1:
        print('  [FAIL] %-46s 期望恰好 1 行 %s=，实得 %d' % (tag, key, len(hit)))
        fails += 1
        continue
    i = hit[0]
    if old not in lines[i]:
        print('  [FAIL] %-46s 旧措辞不在 %s 那一行（避免改到别处）' % (tag, key))
        fails += 1
        continue

    lines[i] = lines[i].replace(old, new)
    out = '\r\n'.join(lines).encode('utf-8')

    # 硬核验：CRLF 计数不变、行数不变、UTF-8 可解、下划线标记仍成对
    problems = []
    if out.count(b'\r\n') != crlf_before:
        problems.append('CRLF 计数变了（%d -> %d）' % (crlf_before, out.count(b'\r\n')))
    if len(out.split(b'\r\n')) != len(lines):
        problems.append('行数变了')
    try:
        decoded = out.decode('utf-8')
    except UnicodeDecodeError as e:
        problems.append('写出后不是合法 UTF-8：%s' % e)
        decoded = ''
    if decoded:
        for k in [key + '=']:
            for l in decoded.split('\r\n'):
                if l.startswith(k):
                    # ⚠️ 只能数**值**部分：键名 `trials.gebura_desc=` 自己带一个下划线，
                    # 把整行拿去 count('_') 会永远奇数 ⇒ 假 FAIL（本次已踩）。
                    value = l.split('=', 1)[1]
                    if value.count('_') % 2 != 0:
                        problems.append('_ 标记不成对（%d 个）' % value.count('_'))
                    # 逐段（\\n\\n 分隔）也都要成对：某一对跨段会让整段着色错位
                    for si, seg in enumerate(value.split('\\n\\n')):
                        if seg.count('_') % 2 != 0:
                            problems.append('第 %d 段的 _ 标记不成对（%d 个）' % (si, seg.count('_')))
                    if '/n/' in value:
                        problems.append('出现误写的 /n/')
                    if '\ufffd' in value:
                        problems.append('出现 U+FFFD')
                    break
    if problems:
        print('  [FAIL] %-46s %s' % (tag, '；'.join(problems)))
        fails += 1
        continue

    if check_only:
        print('  [WOULD] %-45s 将替换（--check，不写盘）' % tag)
    else:
        open(path, 'wb').write(out)
        print('  [OK]   %-45s 已替换（CRLF %d 不变）' % (tag, out.count(b'\r\n')))

print('')
print('完成：%s' % ('只检查，未写盘' if check_only else '已写盘'))
sys.exit(1 if fails else 0)
