# -*- coding: utf-8 -*-
"""把「seq_ok 锚点互为前缀」这一条陷阱追加到 docs/handbook/pitfalls.md 的表格末尾（幂等）。"""
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
P = os.path.join(ROOT, 'docs', 'handbook', 'pitfalls.md')

ROW = ('| **`seq_ok` 的锚点互为前缀 ⇒ 顺序断言永远失败**（2026-09-27：HOD 的 `hodEliteTurns`） | '
       '`seq_ok(body, [t1, t2, …])` 靠 `body.index(token)` 取偏移再判**严格递增**。若两个 token **互为前缀**'
       '（`HOD_ELITE_TURNS` 恰是 `HOD_ELITE_TURNS_BOSS` 的子串），后者的命中**起点与前者的起点相同** —— '
       '实测偏移 `[92, 121, 121]`，于是「121 不 > 121」直接判失败。⚠️ 这类失败**长得像生产代码错了**'
       '（"顺序不对"），实际是断言写法问题，极易误改源码 | '
       '锚点改用**不互为前缀**的串：`[\'Char.Property.BOSS\', \'return HOD_ELITE_TURNS_BOSS;\', '
       '\'return HOD_ELITE_TURNS;\']`（尾缀 `_BOSS;` 把两者区分开）。通则在选 `seq_ok` token 时自问'
       '「这个串会不会是别的 token 的前缀 / 子串」；避不开就带上下文（前后各留几个字符）。'
       '核验 `_chk/verify_hod_netzach.py` ①层 |')

d = open(P, 'rb').read()
if ROW.encode('utf-8') in d:
    print('[SKIP] pitfalls.md 已含本条')
else:
    open(P, 'wb').write(d.rstrip(b'\n') + b'\n' + ROW.encode('utf-8') + b'\n')
    print('[OK] 已追加 1 行 → docs/handbook/pitfalls.md')
