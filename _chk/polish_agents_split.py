#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""AGENTS.md 重构后的排版收尾（一次性，幂等）。

修四处：
  ① §3 迁出指针里被截断的关键词（`docs/terrain-creatio…`）导致读起来断裂
  ② §6 从 MEMORY.md 抄来的表头是 `##`，放进来会与 §0~§9 同级 ⇒ 降为 `###`
  ③ 同一张表里「细节回查 §6」应指向迁出后的实际文件
  ④ 畸形行归一化后成了连写长句，补一个分号；另修 §8 摘要里的繁体「時」

用法：python _chk/polish_agents_split.py [--check]
"""

import argparse
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AGENTS = os.path.join(ROOT, 'AGENTS.md')

POINTER_HEAD = '- **\u5730\u56fe\u8d34\u56fe\uff08\u5206\u5c42 tilemap'
POINTER_TAIL = '  \u5b9a\u4f4d\uff1a'
POINTER_NEW = (
    '- **\u5730\u56fe\u8d34\u56fe\uff08\u5206\u5c42 tilemap / \u8de8\u683c\u62fc\u5e27 / \u8349\u76ae\u906e\u6321\uff09\u539f\u6587\u5df2\u8fc1\u81f3 '
    '`docs/handbook/terrain-tilemap.md`**\uff08\u539f \u00a73 \u5730\u5f62\u6bb5\uff09\u3002\n'
    '  \u5b9a\u4f4d\uff1a`python _chk/docfind.py \u9ad8\u8349`\uff1b\u5efa\u65b0\u5730\u5f62\u770b `docs/terrain-creation-guide.md`\uff0c'
    '\u624b\u7ed8\u901f\u67e5\u8868\u4e0e\u9010\u5e27\u6838\u9a8c\u5de5\u5177\u5728\u90a3\u91cc\u3002\n'
)

FIXES = [
    ('## \u9ad8\u9891\u9759\u9ed8\u9677\u9631\uff08\u6309\u75c7\u72b6\u67e5 `AGENTS.md` \u00a76\uff09',
     '### \u9ad8\u9891\u9759\u9ed8\u9677\u9631\uff08\u7cbe\u7b80\u8868\uff09'),
    ('| \u75c7\u72b6 | \u8981\u5bb3\uff08\u4e00\u53e5\u8bdd\uff1b\u7ec6\u8282\u56de\u67e5 \u00a76 \u4e0e\u540c\u540d\u56de\u5f52\u811a\u672c\uff09 |',
     '| \u75c7\u72b6 | \u8981\u5bb3\uff08\u4e00\u53e5\u8bdd\uff1b\u7ec6\u8282\u56de\u67e5 `docs/handbook/pitfalls.md` \u00a76 \u4e0e\u540c\u540d\u56de\u5f52\u811a\u672c\uff09 |'),
    ('\u5f3a\u5236\u8bc5\u5492\uff09 \u4e13\u5c5e\u4ef6\u5728\u5b9e\u4f8b\u5757',
     '\u5f3a\u5236\u8bc5\u5492\uff09\uff1b\u4e13\u5c5e\u4ef6\u5728\u5b9e\u4f8b\u5757'),
    ('\u56fe\u5c42\u4e0e\u56de\u6536\u3001\u6642\u95f4\u5e38\u91cf\u6807\u5b9a\u3002',
     '\u56fe\u5c42\u4e0e\u56de\u6536\u3001\u65f6\u95f4\u5e38\u91cf\u6807\u5b9a\u3002'),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true', help='只报差异，不写盘')
    parser.add_argument('--apply', action='store_true', help='执行修补（默认行为）')
    args = parser.parse_args()

    with open(AGENTS, 'r', encoding='utf-8', newline='') as handle:
        text = handle.read()

    changed = 0
    lines = text.split('\n')
    out = []
    i = 0
    while i < len(lines):
        if lines[i].startswith(POINTER_HEAD):
            if i + 1 >= len(lines) or not lines[i + 1].startswith(POINTER_TAIL):
                raise SystemExit('§3 指针结构不符预期，行 %d' % (i + 1))
            if lines[i] != POINTER_NEW.split('\n')[0]:
                out.extend(POINTER_NEW.rstrip('\n').split('\n'))
                changed += 1
            else:
                out.extend([lines[i], lines[i + 1]])
            i += 2
            continue
        out.append(lines[i])
        i += 1
    text = '\n'.join(out)

    for old, new in FIXES:
        hits = text.count(old)
        if hits == 0:
            continue  # 已完成，幂等
        if hits != 1:
            raise SystemExit('修补未唯一命中（%d 次）：%r' % (hits, old))
        text = text.replace(old, new)
        changed += 1

    size = len(text.encode('utf-8'))
    print('修补项：%d 处；AGENTS.md 现为 %d 字节' % (changed, size))
    if size > 55296:
        raise SystemExit('AGENTS.md 超过 54KB 安全线')
    if args.check:
        print('[check] 未写盘。')
        return
    if changed == 0:
        print('[apply] 无改动（已是最新）。')
        return
    with open(AGENTS, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)
    print('[apply] 已写入 AGENTS.md')


if __name__ == '__main__':
    main()
