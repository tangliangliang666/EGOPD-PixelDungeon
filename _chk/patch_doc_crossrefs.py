#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把「活文档」里指向 AGENTS.md 旧节号的交叉引用改到重构后的实际位置。

范围界定（与 AGENTS.md §9 的追加式归档约定一致）：
  * 改：**活文档**（会被持续维护的索引/指南）—— docs/ring-master.md、docs/weapon-creation-guide.md、
        docs/oracle-implementation.md、.workbuddy/memory/MEMORY.md
  * 不改：**追加式日志** —— docs/features.md、.workbuddy/memory/YYYY-MM-DD.md
        （只往后写、不回改旧条；旧节号由 AGENTS.md §0「旧引用解析表」覆盖，不会失效）

每个补丁都要求唯一命中；已经改过则跳过（幂等）。
用法：python _chk/patch_doc_crossrefs.py [--check]
"""

import argparse
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

PATCHES = [
    ('.workbuddy/memory/MEMORY.md',
     '> \u6743\u5a01\u6b63\u6587\uff1a`AGENTS.md` \u00a71/\u00a72/\u00a74/\u00a76/\u00a77 + `docs/features.md`\u3002',
     '> \u6743\u5a01\u6b63\u6587\uff1a`AGENTS.md` \u00a71/\u00a72/\u00a73/\u00a75\uff08\u5e38\u9a7b\uff09+ `docs/handbook/`\uff08\u539f \u00a74/\u00a76/\u00a77/\u00a78 \u539f\u6587\uff09'
     '+ `docs/features.md`\u3002\u603b\u7d22\u5f15 `docs/INDEX.md`\u3002'),
    ('.workbuddy/memory/MEMORY.md',
     '## \u9ad8\u9891\u9759\u9ed8\u9677\u9631\uff08\u6309\u75c7\u72b6\u67e5 `AGENTS.md` \u00a76\uff09',
     '## \u9ad8\u9891\u9759\u9ed8\u9677\u9631\uff08\u6309\u75c7\u72b6\u67e5 `docs/handbook/pitfalls.md` \u00a76\uff1bAGENTS.md \u00a76 \u4e3a\u7cbe\u7b80\u8868\uff09'),
    ('.workbuddy/memory/MEMORY.md',
     '\u53cd\u7f16\u8bd1\u8fd8\u539f\u7684\u7ecf\u9a8c\u89c1 AGENTS.md \u00a76\uff08CFR \u955c\u50cf',
     '\u53cd\u7f16\u8bd1\u8fd8\u539f\u7684\u7ecf\u9a8c\u89c1 `docs/handbook/pitfalls.md` \u00a76\uff08CFR \u955c\u50cf'),
    ('docs/ring-master.md',
     '\u4fdd\u7559\u5728 AGENTS.md \u00a77\uff1b\u901a\u7528\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u7ee7\u627f\u3001\u6389\u843d\u6c60\u3001\u56fe\u6807\u3001\u7c92\u5b50\u3001\u6d88\u606f\u683c\u5f0f\u7b49\uff09\u89c1 AGENTS.md \u00a77\u3002',
     '\u4fdd\u7559\u5728 `docs/handbook/weapon-item-dev.md` \u00a77\uff1b\u901a\u7528\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u7ee7\u627f\u3001\u6389\u843d\u6c60\u3001\u56fe\u6807\u3001\u7c92\u5b50\u3001\u6d88\u606f\u683c\u5f0f\u7b49\uff09\u89c1 \u540c\u6587\u4ef6 \u00a77\u3002'),
    ('docs/ring-master.md',
     '## \u4eba\u4f53\u6d3e\u4f5c\u54c1\u6b66\u5668/\u7d20\u6750/\u5408\u6210\u6863\u6848\uff08\u539f AGENTS.md \u00a77 \u62c6\u51fa\uff09',
     '## \u4eba\u4f53\u6d3e\u4f5c\u54c1\u6b66\u5668/\u7d20\u6750/\u5408\u6210\u6863\u6848\uff08\u539f AGENTS.md \u00a77 \u62c6\u51fa\uff0c\u73b0\u5728 `docs/handbook/weapon-item-dev.md`\uff09'),
    ('docs/weapon-creation-guide.md',
     '\u901a\u7528\u9677\u9631/\u89c4\u5f8b\u65b0\u589e\u65f6\u6309 AGENTS.md \u00a76 \u901f\u67e5\u8868\u8865\u5145\u3002',
     '\u901a\u7528\u9677\u9631/\u89c4\u5f8b\u65b0\u589e\u65f6\u6309 `docs/handbook/pitfalls.md` \u00a76 \u901f\u67e5\u8868\uff08AGENTS.md \u00a76 \u4e3a\u7cbe\u7b80\u8868\uff09\u8865\u5145\u3002'),
    ('docs/oracle-implementation.md',
     '\u542b\u795e\u8c15\u793a\u4f8b\u7684\u901a\u7528\u9677\u9631\u4ecd\u5728 AGENTS.md \u00a76 \u9677\u9631\u901f\u67e5\u8868\u3002',
     '\u542b\u795e\u8c15\u793a\u4f8b\u7684\u901a\u7528\u9677\u9631\u4ecd\u5728 `docs/handbook/pitfalls.md` \u00a76 \u9677\u9631\u901f\u67e5\u8868\uff08AGENTS.md \u00a76 \u4e3a\u7cbe\u7b80\u8868\uff09\u3002'),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    cache = {}
    plan = {}
    applied = skipped = 0
    for rel, old, new in PATCHES:
        path = os.path.join(ROOT, rel)
        if path not in cache:
            with open(path, 'r', encoding='utf-8', newline='') as handle:
                cache[path] = handle.read()
        text = cache[path]
        if new in text:
            skipped += 1
            continue
        hits = text.count(old)
        if hits != 1:
            raise SystemExit('%s\uff1a\u8865\u4e01\u672a\u552f\u4e00\u547d\u4e2d\uff08%d \u6b21\uff09\uff1a%r' % (rel, hits, old[:50]))
        cache[path] = text.replace(old, new)
        plan[rel] = plan.get(rel, 0) + 1
        applied += 1

    print('\u5f85\u5e94\u7528 %d \u5904\uff0c\u5df2\u5e94\u7528\u8fc7\uff08\u8df3\u8fc7\uff09%d \u5904' % (applied, skipped))
    for rel, n in sorted(plan.items()):
        print('  %-42s %d \u5904' % (rel, n))

    if args.check:
        print('[check] \u672a\u5199\u76d8\u3002')
        return 0
    for path, text in cache.items():
        with open(path, 'w', encoding='utf-8', newline='') as handle:
            handle.write(text)
    print('[apply] \u5df2\u5199\u5165 %d \u4e2a\u6587\u4ef6\u3002' % len(cache))
    return 0


if __name__ == '__main__':
    sys.exit(main())
