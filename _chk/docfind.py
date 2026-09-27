#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""跨语料文档检索：一条命令得到「文件:行号」，直接喂给 read 的 offset/limit。

替代「把大文件整读一遍找一句话」——统一搜索 AGENTS.md、docs/、docs/index/、
.workbuddy/memory/ 逐日日志、根目录设计稿、_chk/ 脚本（可选源码）。

用法：
    python _chk/docfind.py 高草                      # 所有关键词都出现（AND）
    python _chk/docfind.py 钩子 命中 --any           # 任一出现（OR）
    python _chk/docfind.py "hitSound.*pitch" --regex
    python _chk/docfind.py 神器 --scope agents,handbook
    python _chk/docfind.py tilemap --limit 30 --context 0
    python _chk/docfind.py Hero.damage --scope code

默认**排除** docs/archive/（重构前的冻结副本），否则每个命中都会翻倍。
"""

import argparse
import os
import re
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

SCOPES = ('agents', 'handbook', 'index', 'docs', 'memory', 'features', 'txt', 'chk', 'code')

SKIP_PARTS = ('_bak_', '_droptest', '_mf_audio', 'node_modules', 'build', '.gradle',
              'SPD-classes', 'tools')

CODE_EXTS = ('.java',)
TEXT_EXTS = ('.py', '.js', '.java')


def collect(scope, include_archive):
    """Returns [(relpath, priority)] — priority 越小越先展示。"""
    hits = []
    if scope == 'agents':
        hits.append(('AGENTS.md', 0))
    elif scope == 'handbook':
        base = os.path.join(ROOT, 'docs', 'handbook')
        for name in sorted(os.listdir(base)):
            if name.endswith('.md'):
                hits.append(('docs/handbook/' + name, 1))
    elif scope == 'index':
        base = os.path.join(ROOT, 'docs', 'index')
        for name in sorted(os.listdir(base)):
            if name.endswith('.md'):
                hits.append(('docs/index/' + name, 3))
    elif scope == 'docs':
        base = os.path.join(ROOT, 'docs')
        for name in sorted(os.listdir(base)):
            path = os.path.join(base, name)
            if os.path.isfile(path) and name.endswith('.md'):
                hits.append(('docs/' + name, 4))
        arc = os.path.join(base, 'archive')
        if include_archive and os.path.isdir(arc):
            for name in sorted(os.listdir(arc)):
                if name.endswith('.md'):
                    hits.append(('docs/archive/' + name, 9))
    elif scope == 'memory':
        base = os.path.join(ROOT, '.workbuddy', 'memory')
        for name in sorted(os.listdir(base)):
            if name.endswith('.md'):
                hits.append(('.workbuddy/memory/' + name, 5))
    elif scope == 'features':
        hits.append(('docs/features.md', 6))
    elif scope == 'txt':
        for name in sorted(os.listdir(ROOT)):
            if name.endswith('.txt'):
                hits.append((name, 7))
    elif scope == 'chk':
        for base, dirs, files in os.walk(os.path.join(ROOT, '_chk')):
            dirs[:] = [d for d in dirs if not any(p in d for p in SKIP_PARTS)]
            for name in sorted(files):
                if os.path.splitext(name)[1].lower() in TEXT_EXTS:
                    rel = os.path.relpath(os.path.join(base, name), ROOT).replace('\\', '/')
                    hits.append((rel, 8))
    elif scope == 'code':
        for base, dirs, files in os.walk(os.path.join(ROOT, 'core', 'src', 'main', 'java')):
            dirs[:] = [d for d in dirs if not any(p in d for p in SKIP_PARTS)]
            for name in sorted(files):
                if name.endswith(CODE_EXTS):
                    rel = os.path.relpath(os.path.join(base, name), ROOT).replace('\\', '/')
                    hits.append((rel, 10))
    return hits


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('terms', nargs='+', help='关键词（默认全部出现才算命中）')
    parser.add_argument('--any', action='store_true', help='任一关键词出现即命中')
    parser.add_argument('--regex', action='store_true', help='把关键词当正则（忽略 --any，多个正则取 OR）')
    parser.add_argument('--ignore-case', action='store_true', help='大小写不敏感')
    parser.add_argument('--scope', default='agents,handbook,index,docs,memory,features,txt,chk',
                        help='逗号分隔：' + ','.join(SCOPES))
    parser.add_argument('--include-archive', action='store_true', help='纳入 docs/archive 冻结副本')
    parser.add_argument('--limit', type=int, default=80, help='总命中上限（默认 80）')
    parser.add_argument('--per-file', type=int, default=12, help='单文件命中上限（默认 12）')
    parser.add_argument('--context', type=int, default=0, help='额外展示前若干行（默认 0）')
    parser.add_argument('--width', type=int, default=170, help='每行截断宽度（默认 170）')
    args = parser.parse_args()

    scopes = [s.strip() for s in args.scope.split(',') if s.strip()]
    bad = [s for s in scopes if s not in SCOPES]
    if bad:
        print('未知 scope：%s（可选 %s）' % (','.join(bad), ','.join(SCOPES)))
        return 2

    flags = re.IGNORECASE if args.ignore_case else 0
    if args.regex:
        patterns = [re.compile(t, flags) for t in args.terms]
        def matched(line):
            return any(p.search(line) for p in patterns)
        desc = '正则 OR：' + ' | '.join(args.terms)
    else:
        # 注意：大小写不敏感时才把小写化的 needle 拿去比；否则必须用原词，
        # 否则 `hitSound` 这类驼峰词会被自己小写化后永远搜不到。
        needles = [t.lower() if args.ignore_case else t for t in args.terms]

        def matched(line):
            hay = line.lower() if args.ignore_case else line
            if args.any:
                return any(n in hay for n in needles)
            return all(n in hay for n in needles)

        desc = ('任一：' if args.any else '全部：') + \
               (' / '.join(args.terms) if args.any else ' + '.join(args.terms))

    targets = []
    for scope in scopes:
        targets.extend(collect(scope, args.include_archive))
    targets.sort(key=lambda item: (item[1], item[0]))

    total = 0
    files_hit = 0
    skipped = 0
    for rel, priority in targets:
        if total >= args.limit:
            skipped += 1
            continue
        path = os.path.join(ROOT, rel)
        if not os.path.isfile(path):
            continue
        try:
            with open(path, 'r', encoding='utf-8', errors='replace', newline='') as handle:
                lines = handle.read().split('\n')
        except OSError:
            continue
        rows = []
        for no, line in enumerate(lines, 1):
            if matched(line):
                rows.append((no, line))
                if len(rows) >= args.per_file:
                    break
        if not rows:
            continue
        files_hit += 1
        print('== %s （%d%s 命中） ==' % (rel, len(rows),
                                       '+' if len(rows) >= args.per_file else ''))
        for no, line in rows:
            total += 1
            if total > args.limit:
                break
            text = line.strip()
            if len(text) > args.width:
                text = text[:args.width] + '…'
            print('  %5d: %s' % (no, text))
            if args.context:
                for k in range(1, args.context + 1):
                    if no - 1 - k >= 0:
                        prev = lines[no - 1 - k].strip()
                        if len(prev) > args.width:
                            prev = prev[:args.width] + '…'
                        print('        %5d| %s' % (no - k, prev))
        print('    -> read %s offset=%d limit=%d' % (rel, max(1, rows[0][0] - 1), min(40, len(rows) + 5)))
        print()

    print('检索式：%s' % desc)
    print('结果：%d 命中 / %d 文件（scope=%s%s）'
          % (total, files_hit, ','.join(scopes),
             '' if args.include_archive else '，已排除 docs/archive'))
    if total == 0:
        print('没命中。可试：拆成更短的词 / --any / --ignore-case / --scope chk,code / --include-archive')
        return 1
    if skipped or total >= args.limit:
        print('（已达上限，用 --limit / --per-file 调大，或换更精确的词）')
    return 0


if __name__ == '__main__':
    sys.exit(main())
