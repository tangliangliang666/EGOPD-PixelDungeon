# -*- coding: utf-8 -*-
"""
回退 2026-09-26 那一轮写进文档的两处表格行与一整章。

  docs/features.md      ：把追加的 §15 整章切掉（按字节前缀截断，前面一字不动）
  AGENTS.md             ：删掉 §6 精简表里新增的那一行
  docs/handbook/pitfalls.md：删掉 §6 全量表里新增的那一行

行尾：三个文件都是纯 LF。每步都先断言命中，不符即中止、不落盘。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BAK_DIR = os.path.join(ROOT, '_chk', '_bak_revert_noexp')

FEATURES = os.path.join(ROOT, 'docs', 'features.md')
AGENTS = os.path.join(ROOT, 'AGENTS.md')
PITFALLS = os.path.join(ROOT, 'docs', 'handbook', 'pitfalls.md')

FEATURES_ANCHOR = '\n# 2026-09-26 修 Bug：矿洞任务层小怪'
ROW_PREFIX = '| **「打死不给经验」的怪照样吃 CHESED 回血 / GEBURA 濒死豁免**'


def read(p):
    s = open(p, 'r', encoding='utf-8', newline='').read()
    assert '\r' not in s, '%s 含 CR' % p
    return s


def drop_row(path, label):
    s = read(path)
    lines = s.split('\n')
    hits = [i for i, l in enumerate(lines) if l.startswith(ROW_PREFIX)]
    if len(hits) != 1:
        print('[%s] 期望正好命中 1 行，实得 %d 行' % (label, len(hits)))
        sys.exit(2)
    i = hits[0]
    removed = lines[i]
    assert 'verify_noexp_trial_exempt' in removed or 'NoExpTrialExemptProbe' in removed, \
        '[%s] 命中的行不像本轮新增行：%r' % (label, removed[:80])
    del lines[i]
    out = '\n'.join(lines)
    assert ROW_PREFIX not in out and 'verify_noexp_trial_exempt' not in out, '[%s] 未删净' % label
    open(os.path.join(BAK_DIR, os.path.basename(path) + '.revert'), 'w',
         encoding='utf-8', newline='').write(s)
    open(path, 'w', encoding='utf-8', newline='').write(out)
    print('[%s] 已删 1 行（%d -> %d 行）' % (label, len(s.split('\n')), len(out.split('\n'))))


def main():
    os.makedirs(BAK_DIR, exist_ok=True)

    # --- features.md：按字节前缀截断 ---
    s = read(FEATURES)
    idx = s.find(FEATURES_ANCHOR)
    if idx < 0:
        print('[features.md] 找不到 §15 标题锚点；**未落盘**')
        sys.exit(2)
    if s.count(FEATURES_ANCHOR) != 1:
        print('[features.md] §15 锚点出现 %d 次，异常' % s.count(FEATURES_ANCHOR))
        sys.exit(2)
    out = s[:idx + 1]                      # 保留 §15 前那一行的换行符
    assert '2026-09-26 修 Bug' not in out, '未切净'
    assert '15.1 需求原文' not in out
    assert out.rstrip('\n').endswith('---'), '截断后结尾不像章节分隔线：%r' % out[-40:]
    open(os.path.join(BAK_DIR, 'features.md.revert'), 'w', encoding='utf-8', newline='').write(s)
    open(FEATURES, 'w', encoding='utf-8', newline='').write(out)
    print('[features.md] 已切掉 §15 整章（%d -> %d 行）' % (len(s.split('\n')), len(out.split('\n'))))

    # --- 两张表 ---
    drop_row(AGENTS, 'AGENTS.md')
    drop_row(PITFALLS, 'pitfalls.md')

    for p in (FEATURES, AGENTS, PITFALLS):
        b = open(p, 'rb').read()
        print('  %-34s 行数=%-6d CRLF=%d' % (os.path.relpath(p, ROOT), b.count(b'\n'), b.count(b'\r\n')))


if __name__ == '__main__':
    main()
