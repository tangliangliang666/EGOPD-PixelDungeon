#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""修正 AGENTS.md §0 里关于「指令预算截断方向」的错误表述（幂等）。

错在哪：原文写「运行时**从头部截断**」——读起来是砍掉开头、保留尾部，
但那样位于文件末尾的 §5~§8 反而该存活，与实际情况自相矛盾。

实际行为（已核 `packages/context/agent-instructions/src/render.ts`）：
  * `truncateUtf8()` 返回 `bytes.subarray(0, end)` ⇒ **保留头部、截掉尾部**；
  * 预算分配是「先整份丢弃较宽容的泛化文件，再截最具体的那一份」
    （`renderInstructionContext`，`render.ts:284-331`）；
  * 预算作用于**整条渲染消息**，不是每个文件各一份；
  * 会打印 `Workspace instruction budget <N> bytes: omitted …; truncated … from A to B bytes`。

用法：python _chk/fix_agents_budget_note.py [--check]
"""

import argparse
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AGENTS = os.path.join(ROOT, 'AGENTS.md')

OLD = (
    '> **2026-09-21 \u91cd\u6784**\uff1a\u539f\u6587 197KB \u8d85\u51fa DSH \u5de5\u4f5c\u533a\u6307\u4ee4\u9884\u7b97\uff0865536 \u5b57\u8282\uff09\uff0c\n'
    '> \u8fd0\u884c\u65f6**\u4ece\u5934\u90e8\u622a\u65ad** \u21d2 \u00a75~\u00a78 \u7ea6 96KB **\u4ece\u672a\u9001\u8fbe\u6a21\u578b**\u3002\n'
)

NEW = (
    '> **2026-09-21 \u91cd\u6784**\uff1a\u539f\u6587 197KB \u8d85\u51fa DSH \u5de5\u4f5c\u533a\u6307\u4ee4\u9884\u7b97\uff0865536 \u5b57\u8282\uff09\u3002\n'
    '> \u6e32\u67d3\u5668\u662f**\u4fdd\u7559\u5934\u90e8\u3001\u622a\u6389\u5c3e\u90e8**\uff08`render.ts` \u7684 `truncateUtf8` \u53d6 `bytes.subarray(0, end)`\uff09\uff0c\n'
    '> \u4e14\u5206\u914d\u987a\u5e8f\u662f\u300c\u5148\u6574\u4efd\u4e22\u5f03\u8f83\u5bbd\u7684\u6cdb\u5316\u6587\u4ef6\uff08\u5982 `~/.dsh/AGENTS.md`\uff09\uff0c\u518d\u622a\u6700\u5177\u4f53\u7684\u90a3\u4e00\u4efd\u300d\n'
    '> \u21d2 \u4f4d\u4e8e**\u6587\u4ef6\u672b\u5c3e**\u7684 \u00a75~\u00a78 \u7ea6 96KB **\u4ece\u672a\u9001\u8fbe\u6a21\u578b**\uff08\u4f1a\u6253\u5370 `Workspace instruction budget \u2026 truncated \u2026` \u63d0\u793a\uff09\u3002\n'
    '> **\u9884\u7b97\u4f5c\u7528\u4e8e\u6574\u6761\u6e32\u67d3\u6d88\u606f**\uff08\u4e0d\u662f\u6bcf\u4e2a\u6587\u4ef6\u5404\u4e00\u4efd\uff09\uff0c\u6240\u4ee5\u518d\u52a0\u4e00\u4e2a `CLAUDE.md` \u4f1a\u4e89\u540c\u4e00\u7b14 65536\u3002\n'
    '> **\u65e0\u6587\u4ef6\u76d1\u542c**\uff1a\u6539\u5b8c AGENTS.md \u540e\u9700\u4e00\u6b21 `read`/`write`/`edit` \u6216\u4f1a\u8bdd\u6062\u590d\u624d\u4f1a\u5237\u65b0\u57fa\u7ebf\u3002\n'
)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    with open(AGENTS, 'r', encoding='utf-8', newline='') as handle:
        text = handle.read()

    if NEW in text and OLD not in text:
        print('\u5df2\u4fee\u6b63\u8fc7\uff08\u5e42\u7b49\uff09\u3002')
        return 0
    hits = text.count(OLD)
    if hits != 1:
        raise SystemExit('\u951a\u70b9\u672a\u552f\u4e00\u547d\u4e2d\uff08%d \u6b21\uff09\uff0c\u4eba\u5de5\u786e\u8ba4\u540e\u518d\u6539\u672c\u811a\u672c' % hits)

    text = text.replace(OLD, NEW)
    size = len(text.encode('utf-8'))
    print('AGENTS.md \u4fee\u6b63\u540e %d \u5b57\u8282\uff08\u9884\u7b97 65536\uff0c\u4f59\u91cf %d\uff09' % (size, 65536 - size))
    if size > 55296:
        raise SystemExit('\u8d85\u8fc7 54KB \u5b89\u5168\u7ebf')
    if args.check:
        print('[check] \u672a\u5199\u76d8\u3002')
        return 0
    with open(AGENTS, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)
    print('[apply] \u5df2\u5199\u5165 AGENTS.md')
    return 0


if __name__ == '__main__':
    sys.exit(main())
