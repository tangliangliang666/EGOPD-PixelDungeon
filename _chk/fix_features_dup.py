#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""去掉 docs/features.md 里**重复追加**的批次块（同名 `# 标题` 只保留第一份）。

为什么会发生：追加入口若用「另一段文字」当幂等标记，标记与实际写入的标题不一致时
就拦不住第二次追加 —— 本仓 2026-09-21 就这么多写了一份「火焰改为亮红」批次。
本工具按**标题本身**判定，比字符串标记可靠。

用法：
    python _chk/fix_features_dup.py            # 只报告，不写盘
    python _chk/fix_features_dup.py --apply
"""

import argparse
import io
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FEATURES = os.path.join(ROOT, 'docs', 'features.md')


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--apply', action='store_true')
    args = ap.parse_args()

    with io.open(FEATURES, encoding='utf-8', newline='') as fh:
        lines = fh.read().split('\n')

    starts = [i for i, l in enumerate(lines) if l.startswith('# ')]
    if not starts:
        print('没有一级标题，异常')
        return 1

    # 每个一级标题块的边界：[起, 下一个一级标题起)
    blocks = []
    for k, s in enumerate(starts):
        e = starts[k + 1] if k + 1 < len(starts) else len(lines)
        blocks.append((s, e, lines[s]))

    seen, dup = {}, []
    for s, e, title in blocks:
        if title in seen:
            dup.append((s, e, title, seen[title]))
        else:
            seen[title] = s

    print('一级标题块共 %d 个，其中重复 %d 个：' % (len(blocks), len(dup)))
    for s, e, title, first in dup:
        print('  重复：%s' % title)
        print('        首次 L%d，重复 L%d~L%d（%d 行）' % (first + 1, s + 1, e, e - s))

    if not dup:
        print('无重复，无需处理。')
        return 0
    if not args.apply:
        print('\n[report] 未写盘。加 --apply 删除重复块（保留首次出现的那份）。')
        return 0

    drop = set()
    for s, e, _, _ in dup:
        drop.update(range(s, e))
    out = [l for i, l in enumerate(lines) if i not in drop]
    # 收掉可能出现的连续空行（删块后留下的接缝）
    cleaned = []
    for l in out:
        if l == '' and cleaned and cleaned[-1] == '':
            continue
        cleaned.append(l)
    with io.open(FEATURES, 'w', encoding='utf-8', newline='') as fh:
        fh.write('\n'.join(cleaned))
    print('\n[apply] 已删除 %d 行；文件 %d → %d 行' % (len(drop), len(lines), len(cleaned)))
    return 0


if __name__ == '__main__':
    sys.exit(main())
