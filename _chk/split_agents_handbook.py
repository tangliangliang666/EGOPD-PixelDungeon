#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 AGENTS.md 按「常驻红线 + 目录索引」重构：长尾原文逐字迁入 docs/handbook/。

背景：DSH 的工作区指令预算是 65536 字节，AGENTS.md 原有 197KB ⇒ 运行时从头部截断，
      §5~§8 约 96KB 从未送达模型。本脚本做三件事：
        ① 冻结全量副本到 docs/archive/AGENTS-2026-09-21-full.md
        ② 按节号把原文逐字拆到 docs/handbook/*.md（保留原节号，旧引用仍可解析）
        ③ 重写 AGENTS.md 为「§0 阅读协议 + §1/§2/§3(核心)/§5 原文 + §4/§6/§7/§8 索引存根 + §9 维护」

所有改动集中在本脚本里（不经编辑工具），落盘一律 UTF-8 + LF，避免编码混存。

用法：
    python _chk/split_agents_handbook.py --check   # 只报字节账，不写盘
    python _chk/split_agents_handbook.py --apply    # 执行
"""

import argparse
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AGENTS = os.path.join(ROOT, 'AGENTS.md')
MEMORY = os.path.join(ROOT, '.workbuddy', 'memory', 'MEMORY.md')
HANDBOOK = os.path.join(ROOT, 'docs', 'handbook')
ARCHIVE = os.path.join(ROOT, 'docs', 'archive')
ARCHIVE_FILE = os.path.join(ARCHIVE, 'AGENTS-2026-09-21-full.md')

SPLIT_MARKER = '<!-- agents-handbook-split: v1 -->'

# ---------------------------------------------------------------- 节定位

SECTIONS = [
    ('s1', '## 1. \u6784\u5efa\u4e0e\u8fd0\u884c\uff08\u5feb\u901f\u53c2\u8003\uff09'),
    ('s2', '## 2. \u6587\u672c\u4e0e\u672c\u5730\u5316'),
    ('s3', '## 3. \u8d34\u56fe\u4e0e\u56fe\u6807'),
    ('s4', '## 4. SPD \u6846\u67b6\u673a\u5236\u7ecf\u9a8c'),
    ('s5', '## 5. \u804c\u4e1a\u4e0e\u529f\u80fd\u6863\u6848\uff08\u5185\u5bb9\u5df2\u62c6\u5206\u81f3 docs/\uff09'),
    ('s6', '## 6. \u5e38\u89c1\u9677\u9631\u901f\u67e5'),
    ('s7', '## 7. \u81ea\u5b9a\u4e49\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u901a\u7528\uff09'),
    ('s8', '## 8. \u6cd5\u6756\u7c7b\u6b66\u5668\uff08\u5931\u4e50\u56ed\uff09\u4e0e buff \u7279\u6548\u5f00\u53d1\u7ecf\u9a8c'),
]

S3_TILEMAP = '- **\u5730\u56fe\u8d34\u56fe\uff1d\u300c\u5206\u5c42 tilemap + \u8de8\u683c\u62fc\u5e27\u300d**'
S3_APK = '- **APK \u684c\u9762\u56fe\u6807 / \u5e94\u7528\u540d**'


def load_bytes(path):
    with open(path, 'rb') as handle:
        return handle.read()


def load_text(path):
    with open(path, 'r', encoding='utf-8', newline='') as handle:
        return handle.read()


def write_text(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)


def find_line_start(raw, needle):
    """Byte offset of a line that begins with `needle` (must occur exactly once)."""
    token = ('\n' + needle).encode('utf-8')
    idx = raw.find(token)
    if idx < 0:
        if raw.startswith(needle.encode('utf-8')):
            return 0
        raise SystemExit('锚点未找到（不是行首或不存在）: %s' % needle)
    if raw.find(token, idx + 1) >= 0:
        raise SystemExit('锚点出现多次，无法唯一定位: %s' % needle)
    return idx + 1


def keyword_cloud(section_text, limit=90, label_cap=40):
    """Mechanically harvest bullet-leading **\u2026** labels as a grep vocabulary."""
    labels = []
    for line in section_text.split('\n'):
        stripped = line.strip()
        if not stripped.startswith('- **'):
            continue
        rest = stripped[4:]
        end = rest.find('**')
        if end <= 0:
            continue
        label = rest[:end].strip()
        if len(label) > label_cap:
            label = label[:label_cap] + '\u2026'
        if label and label not in labels:
            labels.append(label)
        if len(labels) >= limit:
            break
    return labels


def distilled_pitfalls():
    """Verbatim-lift the distilled symptom table from .workbuddy/memory/MEMORY.md."""
    text = load_text(MEMORY)
    start = text.index('## \u9ad8\u9891\u9759\u9ed8\u9677\u9631')
    tail = text[start:]
    nxt = tail.find('\n## ', 1)
    block = tail if nxt < 0 else tail[:nxt]
    block = block.rstrip('\n')
    # MEMORY.md 末行是畸形 4 列行（首格为空、症状标签错落在第二格），补齐成 2 列。
    bad = '| | **\u4e13\u5c5e\u88c5\u5907\u6b7b\u4ea1\u540e\u8fdb\u4e86\u82f1\u96c4\u9057\u9ab8**'
    lines = block.split('\n')
    for i, line in enumerate(lines):
        if line.startswith(bad):
            cells = [c.strip() for c in line.strip().strip('|').split('|')]
            cells = [c for c in cells if c]
            if len(cells) < 2:
                raise SystemExit('MEMORY.md 畸形行解析失败: ' + line)
            fixed = '| ' + cells[0] + ' | ' + ' '.join(cells[1:]) + ' |'
            lines[i] = fixed
            block = '\n'.join(lines)
            break
    else:
        raise SystemExit('MEMORY.md 未找到预期的畸形行，人工确认后再改本脚本')
    return block


def stub(number, title, target, body_note, cloud):
    bullets = '\n'.join('- ' + label for label in cloud)
    return (
        '## %s. %s\n\n'
        '> **原文（全量、逐字）已迁至 `%s`**；本节只留检索入口，避免常驻指令超预算。\n'
        '> 取用：`python _chk/docfind.py <关键词>` 或 `grep -n "<关键词>" %s`，再按行号定点 `read`。\n\n'
        '%s\n\n'
        '<details><summary>本节词表（%d 条，可直接拿去 grep）</summary>\n\n%s\n\n</details>\n'
        % (number, title, target, target, body_note, len(cloud), bullets)
    )


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--check', action='store_true', help='只报字节账，不写盘')
    group.add_argument('--apply', action='store_true', help='执行切分')
    args = parser.parse_args()

    raw = load_bytes(AGENTS)
    text = raw.decode('utf-8')
    if SPLIT_MARKER in text:
        raise SystemExit('AGENTS.md 已完成过本切分（发现 %s），拒绝重复执行。' % SPLIT_MARKER)
    if os.path.exists(ARCHIVE_FILE) and args.apply:
        raise SystemExit('冻结副本已存在，拒绝覆盖：%s' % ARCHIVE_FILE)
    if b'\r' in raw:
        raise SystemExit('AGENTS.md 含 CR，本脚本假定纯 LF；请先确认换行风格。')

    offsets = {}
    for key, marker in SECTIONS:
        offsets[key] = find_line_start(raw, marker)
    ordered = [k for k, _ in SECTIONS]
    bounds = {}
    for i, key in enumerate(ordered):
        bounds[key] = (offsets[key], offsets[ordered[i + 1]] if i + 1 < len(ordered) else len(raw))

    header = raw[:bounds['s1'][0]]
    chunks = {k: raw[v[0]:v[1]] for k, v in bounds.items()}

    # §3 内部再切三块：核心（留下）/ 地形 tilemap（迁出）/ APK 图标（留下）
    s3 = chunks['s3']
    tm = find_line_start(s3, S3_TILEMAP)
    apk = find_line_start(s3, S3_APK)
    if not tm < apk:
        raise SystemExit('§3 内锚点顺序异常')
    s3_core, s3_tilemap, s3_apk = s3[:tm], s3[tm:apk], s3[apk:]

    # 完整性闸门：所有切块按原序拼回必须与原文逐字节相同
    reassembled = b''.join([header, chunks['s1'], chunks['s2'], s3_core, s3_tilemap, s3_apk,
                            chunks['s4'], chunks['s5'], chunks['s6'], chunks['s7'], chunks['s8']])
    if reassembled != raw:
        raise SystemExit('切块回拼与原文不一致，拒绝继续（切分逻辑有误）')

    def txt(blob):
        return blob.decode('utf-8')

    def hand_header(title, src_note, anchor):
        return (
            '<!-- 本文件由 AGENTS.md %s 逐字迁出（%s）。 -->\n'
            '<!-- 保留原节号，旧文里的“见 %s”仍可解析；索引见 docs/INDEX.md。 -->\n\n'
            '# %s\n\n'
            '> 定位：`python _chk/docfind.py <关键词>`；或 `grep -n` 本文件。\n'
            '> 本文件是长尾原文，**按需定点读取，不要整读**。\n\n'
            % (anchor, src_note, anchor, title)
        )

    plan = []
    plan.append((ARCHIVE_FILE,
                 '<!-- 冻结存档：2026-09-21 重构前的 AGENTS.md 全量原文（%d 字节 / %d 行）。 -->\n'
                 '<!-- 不要读、不要改；仅供考古。常驻核心见 AGENTS.md，长尾原文见 docs/handbook/。 -->\n\n'
                 % (len(raw), text.count('\n') + 1) + text))
    plan.append((os.path.join(HANDBOOK, 'terrain-tilemap.md'),
                 hand_header('\u5730\u56fe\u8d34\u56fe / \u5206\u5c42 tilemap\uff08\u539f AGENTS.md \u00a73 \u5730\u5f62\u6bb5\uff09',
                             '\u00a73', '\u00a73 \u5730\u5f62\u6bb5') + txt(s3_tilemap)))
    plan.append((os.path.join(HANDBOOK, 'spd-framework.md'),
                 hand_header('SPD \u6846\u67b6\u673a\u5236\u7ecf\u9a8c\uff08\u539f AGENTS.md \u00a74\uff0c\u5168\u91cf\uff09',
                             '\u00a74', '\u00a74') + txt(chunks['s4'])))
    plan.append((os.path.join(HANDBOOK, 'pitfalls.md'),
                 hand_header('\u5e38\u89c1\u9677\u9631\u901f\u67e5\uff08\u539f AGENTS.md \u00a76\uff0c\u5168\u91cf\u8868\uff09',
                             '\u00a76', '\u00a76') + txt(chunks['s6'])))
    plan.append((os.path.join(HANDBOOK, 'weapon-item-dev.md'),
                 hand_header('\u81ea\u5b9a\u4e49\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u539f AGENTS.md \u00a77\uff09',
                             '\u00a77', '\u00a77') + txt(chunks['s7'])))
    plan.append((os.path.join(HANDBOOK, 'wand-buff-fx-dev.md'),
                 hand_header('\u6cd5\u6756\u7c7b\u6b66\u5668\uff08\u5931\u4e50\u56ed\uff09\u4e0e buff \u7279\u6548\u5f00\u53d1\u7ecf\u9a8c\uff08\u539f AGENTS.md \u00a78\uff09',
                             '\u00a78', '\u00a78') + txt(chunks['s8'])))

    # ---- 新 AGENTS.md ----
    head_s0 = HEAD_S0 % {'marker': SPLIT_MARKER}
    s5_text = txt(chunks['s5'])
    s1_text = txt(chunks['s1'])
    for old, new in CROSSREF_FIXES:
        if s5_text.count(old) + s1_text.count(old) != 1:
            raise SystemExit('交叉引用修补未唯一命中：%r' % old)
        s5_text = s5_text.replace(old, new)
        s1_text = s1_text.replace(old, new)

    pitfalls = distilled_pitfalls()
    s6_new = (
        '## 6. \u5e38\u89c1\u9677\u9631\u901f\u67e5\uff08\u7cbe\u7b80\u8868\uff1b**\u5168\u91cf\u8868\u539f\u6587\u5df2\u8fc1\u81f3 '
        '`docs/handbook/pitfalls.md`**\uff09\n\n'
        '> \u4e0b\u8868\u662f\u6700\u9ad8\u9891\u7684\u9759\u9ed8\u578b\u9677\u9631\uff08\u6e90\uff1a`.workbuddy/memory/MEMORY.md`\uff0c\u4eba\u5de5\u540c\u6b65\uff09\uff1b'
        '\u5168\u91cf 111 \u884c\u539f\u8868\u5728 `docs/handbook/pitfalls.md`\uff0c\n'
        '> \u6b63\u6587\u7528 `python _chk/docfind.py <\u75c7\u72b6\u8bcd>` \u5b9a\u4f4d\u3002**\u75c7\u72b6\u4f18\u5148\uff1a\u5148\u67e5\u8868\uff0c\u518d\u52a8\u624b\u3002**\n\n'
        + pitfalls + '\n'
    )
    s4_cloud = keyword_cloud(txt(chunks['s4']))
    s7_cloud = keyword_cloud(txt(chunks['s7']))
    s8_cloud = keyword_cloud(txt(chunks['s8']))
    tilemap_cloud = keyword_cloud(txt(s3_tilemap), limit=30)

    # §3 迁出的地形段：在「英雄皮肤/音效」与「APK 图标」之间留一行指针
    pointer = ('\n- **\u5730\u56fe\u8d34\u56fe\uff08\u5206\u5c42 tilemap / \u8de8\u683c\u62fc\u5e27 / \u8349\u76ae\u906e\u6321\uff09\u539f\u6587\u5df2\u8fc1\u81f3 '
               '`docs/handbook/terrain-tilemap.md`**\uff08\u539f \u00a73 \u5730\u5f62\u6bb5\uff09\u3002'
               '\u6d89\u53ca\uff1a' + '\u3001'.join(tilemap_cloud[:18]) + '\u2026\n'
               '  \u5b9a\u4f4d\uff1a`python _chk/docfind.py \u9ad8\u8349`\uff1b\u5efa\u65b0\u5730\u5f62\u770b `docs/terrain-creation-guide.md`\u3002\n')
    s3_new = txt(s3_core) + pointer + txt(s3_apk)

    new_agents = (
        head_s0 + '\n'
        + s1_text
        + txt(chunks['s2'])
        + s3_new
        + stub('4', 'SPD \u6846\u67b6\u673a\u5236\u7ecf\u9a8c', 'docs/handbook/spd-framework.md',
               '\u672c\u8282\u662f\u5168\u4e66**\u6700\u5e38\u67e5**\u7684\u4e00\u5e93\uff1a\u5404\u7c7b\u94a9\u5b50/\u6c47\u805a\u70b9/\u6846\u67b6\u884c\u4e3a\u3002'
               '\u5148\u7528\u4e0b\u65b9\u8bcd\u8868\u641c\u5230\u884c\u53f7\uff0c\u518d\u5b9a\u70b9\u8bfb\u90a3\u4e00\u6761\u3002',
               s4_cloud)
        + '\n' + s5_text
        + '\n' + s6_new
        + '\n' + stub('7', '\u81ea\u5b9a\u4e49\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u901a\u7528\uff09',
                      'docs/handbook/weapon-item-dev.md',
                      '\u65b0\u6b66\u5668/\u7269\u54c1/\u9970\u54c1/\u9644\u9b54\u3001\u6389\u843d\u6c60\u63a5\u7ebf\u3002'
                      '\u53e6\u53c2 `docs/weapon-creation-guide.md`\u3002', s7_cloud)
        + '\n' + stub('8', '\u6cd5\u6756\u7c7b\u6b66\u5668\uff08\u5931\u4e50\u56ed\uff09\u4e0e buff \u7279\u6548\u5f00\u53d1\u7ecf\u9a8c',
                      'docs/handbook/wand-buff-fx-dev.md',
                      '\u6cd5\u6756/\u7c92\u5b50/\u7279\u6548\u3001\u56fe\u5c42\u4e0e\u56de\u6536\u3001\u6642\u95f4\u5e38\u91cf\u6807\u5b9a\u3002', s8_cloud)
        + '\n' + HEAD_S9
    )

    plan.append((AGENTS, new_agents))

    # ---- 报账 ----
    print('原始 AGENTS.md : %d 字节 / %d 行' % (len(raw), text.count('\n') + 1))
    print('--- 迁出（逐字） ---')
    for name, blob in (('terrain-tilemap.md', s3_tilemap), ('spd-framework.md', chunks['s4']),
                       ('pitfalls.md', chunks['s6']), ('weapon-item-dev.md', chunks['s7']),
                       ('wand-buff-fx-dev.md', chunks['s8'])):
        print('  docs/handbook/%-22s %7d 字节' % (name, len(blob)))
    print('--- 保留在 AGENTS.md ---')
    print('  %-30s %7d 字节' % ('§1 构建与运行', len(chunks['s1'])))
    print('  %-30s %7d 字节' % ('§2 文本与本地化', len(chunks['s2'])))
    print('  %-30s %7d 字节' % ('§3 贴图与图标（核心）', len(s3_core)))
    print('  %-30s %7d 字节' % ('§3 APK 图标段', len(s3_apk)))
    print('  %-30s %7d 字节' % ('§5 档案地图', len(chunks['s5'])))
    new_bytes = new_agents.encode('utf-8')
    print('--- 结果 ---')
    print('  新 AGENTS.md : %d 字节（预算 65536，余量 %d）' % (len(new_bytes), 65536 - len(new_bytes)))
    for path, blob in plan:
        print('  写入 %-52s %7d 字节' % (os.path.relpath(path, ROOT), len(blob.encode('utf-8'))))
    if len(new_bytes) > 55296:
        raise SystemExit('新 AGENTS.md 超过 54KB 安全线，请再迁出长尾内容')

    if args.check:
        print('\n[check] 未写盘。加 --apply 执行。')
        return

    for path, blob in plan:
        write_text(path, blob)
    print('\n[apply] 已写入 %d 个文件。' % len(plan))
    print('下一步：python _chk/build_doc_index.py --apply')


HEAD_S0 = '''# AGENTS.md \u2014 \u5f00\u53d1\u7ecf\u9a8c\u624b\u518c\uff08\u901a\u7528 \u00b7 \u5e38\u9a7b\u6838\u5fc3\uff09

%(marker)s
> \u672c\u6587\u6863\u4e3a**\u901a\u7528\u5f00\u53d1\u7ecf\u9a8c\u624b\u518c**\uff08\u6784\u5efa\u6d41\u7a0b\u3001\u6587\u672c\u672c\u5730\u5316\u3001\u8d34\u56fe\u89c4\u8303\u3001SPD \u6846\u67b6\u673a\u5236\u3001\u901a\u7528\u9677\u9631\u3001\u901a\u7528\u7269\u54c1/\u7279\u6548\u7ecf\u9a8c\uff09\u3002
> **2026-09-21 \u91cd\u6784**\uff1a\u539f\u6587 197KB \u8d85\u51fa DSH \u5de5\u4f5c\u533a\u6307\u4ee4\u9884\u7b97\uff0865536 \u5b57\u8282\uff09\uff0c
> \u8fd0\u884c\u65f6**\u4ece\u5934\u90e8\u622a\u65ad** \u21d2 \u00a75~\u00a78 \u7ea6 96KB **\u4ece\u672a\u9001\u8fbe\u6a21\u578b**\u3002
> \u73b0\u6309\u300c**\u5e38\u9a7b\u7ea2\u7ebf + \u76ee\u5f55\u7d22\u5f15**\u300d\u91cd\u7ec4\uff1a\u672c\u6587\u4ef6\u53ea\u4fdd\u7559\u6bcf\u6b21\u90fd\u8981\u7528\u7684\u90e8\u5206\uff0c\u957f\u5c3e\u539f\u6587\u9010\u5b57\u8fc1\u5165 `docs/handbook/`\u3002

## 0. \u9605\u8bfb\u534f\u8bae\u4e0e\u6587\u6863\u5730\u56fe\uff08**\u5148\u8bfb\u8fd9\u4e00\u8282**\uff09

**\u53d6\u7528\u987a\u5e8f\uff1a\u5148\u7528\u4e0b\u8868\u5b9a\u4f4d \u2192 \u518d `docfind`/`grep` \u62ff\u5230 `\u6587\u4ef6:\u884c\u53f7` \u2192 \u6700\u540e `read` \u5e26 `offset`/`limit` \u53ea\u8bfb\u90a3\u4e00\u6bb5\u3002**
**\u7981\u6b62\u6574\u8bfb** `docs/features.md`\uff086006 \u884c\uff09\u3001`.workbuddy/memory/*.md`\uff0820 \u4e2a\u65e5\u5fd7\uff09\u3001`docs/handbook/*.md` \u4e0e `docs/archive/`\u3002

| \u5185\u5bb9 | \u4f4d\u7f6e | \u4f55\u65f6\u8bfb |
|---|---|---|
| **\u603b\u7d22\u5f15**\uff08\u6240\u6709\u6587\u6863\u7684\u7ae0\u8282\u884c\u53f7\u951a\u70b9\uff09 | `docs/INDEX.md` | \u60f3\u627e\u300c\u67d0\u4ef6\u4e8b\u5199\u5728\u54ea\u300d\u65f6**\u5148\u8bfb\u8fd9\u4e2a** |
| \u8de8\u8bed\u6599\u5173\u952e\u8bcd\u5b9a\u4f4d | `python _chk/docfind.py <\u5173\u952e\u8bcd>` | \u4e00\u6761\u547d\u4ee4\u8fd4\u56de AGENTS/docs/memory/_chk \u7684 `\u6587\u4ef6:\u884c` |
| \u00a73 \u5730\u56fe\u8d34\u56fe\u3001\u5206\u5c42 tilemap\u3001\u8de8\u683c\u62fc\u5e27 \u539f\u6587 | `docs/handbook/terrain-tilemap.md` | \u52a8\u5730\u5f62/\u56fe\u96c6/\u8349\u76ae/\u906e\u6321\u65f6 |
| \u00a74 SPD \u6846\u67b6\u673a\u5236\u7ecf\u9a8c \u539f\u6587\uff08\u5168\u91cf\uff09 | `docs/handbook/spd-framework.md` | \u627e\u94a9\u5b50/\u6c47\u805a\u70b9/\u6846\u67b6\u884c\u4e3a\u65f6\uff08**\u6700\u5e38\u67e5**\uff09 |
| \u00a76 \u5e38\u89c1\u9677\u9631\u901f\u67e5 \u5168\u91cf\u8868 \u539f\u6587 | `docs/handbook/pitfalls.md` | \u9047\u5230\u602a\u73b0\u8c61\u3001\u75c7\u72b6 \u2014\u2014 \u5148\u67e5\u5b83 |
| \u00a77 \u81ea\u5b9a\u4e49\u6b66\u5668/\u7269\u54c1 \u539f\u6587 | `docs/handbook/weapon-item-dev.md` | \u65b0\u6b66\u5668/\u7269\u54c1/\u9644\u9b54/\u6389\u843d\u6c60 |
| \u00a78 \u6cd5\u6756\uff08\u5931\u4e50\u56ed\uff09\u4e0e buff \u7279\u6548 \u539f\u6587 | `docs/handbook/wand-buff-fx-dev.md` | \u6cd5\u6756/\u7c92\u5b50/\u7279\u6548/\u56fe\u5c42 |
| \u975e\u804c\u4e1a\u529f\u80fd\u6863\u6848\u3001\u9010\u6279\u6539\u52a8\u6d41\u6c34\u8d26 | `docs/features.md`\uff0b`docs/index/features.md`\uff08\u6279\u6b21\u2192\u884c\u53f7\uff09 | \u53ea\u60f3\u770b\u67d0\u4e00\u6279\u6539\u52a8\u65f6 |
| \u53e6\u4e00\u5e73\u53f0\uff08WorkBuddy\uff09\u9010\u65e5\u5de5\u4f5c\u65e5\u5fd7 | `.workbuddy/memory/*.md`\uff0b`docs/index/memory.md`\uff08\u65e5\u671f/\u4e3b\u9898\u2192\u884c\u53f7\uff09 | \u8ffd\u6eaf\u300c\u5f53\u65f6\u4e3a\u4ec0\u4e48\u8fd9\u4e48\u6539\u300d\u65f6 |
| `_chk/` \u6838\u9a8c\u811a\u672c\u6e05\u5355\uff08\u7528\u9014/\u65ad\u8a00\u76ee\u6807\uff09 | `docs/index/scripts.md` | \u6539\u5b8c\u4ee3\u7801\u6311\u56de\u5f52\u811a\u672c\u65f6 |
| \u5404\u804c\u4e1a/\u529f\u80fd\u4e13\u9898\u6863\u6848 | \u89c1\u4e0b\u65b9 \u00a75 | \u505a\u5bf9\u5e94\u5f00\u53d1\u65f6 |

**\u65e7\u5f15\u7528\u89e3\u6790\u8868**\uff1a\u65e7\u6587\uff08\u542b `docs/`\u3001`.workbuddy/memory/`\u3001\u4ee3\u7801\u6ce8\u91ca\uff09\u91cc\u7684\u300c\u89c1 \u00a74 / \u00a76 / \u00a77 / \u00a78\u300d\u4e00\u5f8b\u6309\u4e0a\u8868\u89e3\u6790 \u2014\u2014
`docs/handbook/` \u91cc\u7684\u6587\u4ef6**\u4fdd\u7559\u4e86\u539f\u8282\u53f7\u4e0e\u539f\u6587**\uff0c`\u00a76 \u7b2c X \u884c`\u3001`\u00a74 \u67d0\u6761` \u8fd9\u7c7b\u8bf4\u6cd5\u4ecd\u80fd\u5bf9\u4e0a\u3002
\u51bb\u7ed3\u5168\u91cf\u526f\u672c\uff1a`docs/archive/AGENTS-2026-09-21-full.md`\uff08\u4ec5\u4f5c\u5b58\u6863\uff0c**\u4e0d\u8981\u8bfb**\uff09\u3002
`\u00a71/\u00a72/\u00a73/\u00a75` **\u4ecd\u5728\u672c\u6587\u4ef6\u91cc**\uff0c\u539f\u7f16\u53f7\u672a\u53d8\u3002

**\u540c\u6b65\u7ea6\u5b9a**\uff1a\u901a\u7528\u673a\u5236/\u89c4\u8303/\u9677\u9631\u7684\u65b0\u53d1\u73b0\u8bb0\u5165\u672c\u6587\u4ef6\uff1b\u957f\u5c3e\u7ec6\u8282\u8bb0\u5165\u5bf9\u5e94 `docs/handbook/*.md` \u6216\u804c\u4e1a\u6863\u6848\uff08docs/ring-master.md\u3001docs/oracle-class-design.md\u3001docs/features.md\uff09\u3002
\u6587\u4e2d\u300c\u73b0\u4e3a XXX\u300d\u5747\u4e3a\u6700\u65b0\u72b6\u6001\uff0c\u4ee5\u5bf9\u5e94\u6587\u4ef6\u4e3a\u51c6\u3002\uff08\u539f\u5f15\u8a00\u91cc\u7684\u300c\u7b2c 5 \u8282\u5185\u5bb9\u5730\u56fe\u300d\u300c\u7b2c 6 \u8282\u9677\u9631\u901f\u67e5\u8868\u300d\u73b0\u5206\u522b\u6307 \u00a75 \u4e0e \u00a76/\u5168\u91cf\u8868\u3002\uff09

'''

CROSSREF_FIXES = [
    ('\u5b8c\u6574\u793a\u4f8b\u4ecd\u4fdd\u7559\u5728\u672c\u624b\u518c \u00a74\u3002',
     '\u5b8c\u6574\u793a\u4f8b\u4ecd\u4fdd\u7559\u5728 `docs/handbook/spd-framework.md` \u00a74\u3002'),
    ('\u5148\u67e5\u7b2c 6 \u8282"\u6e38\u620f\u9759\u9ed8\u9000\u51fa"\u6761\u76ee',
     '\u5148\u67e5 `docs/handbook/pitfalls.md` \u00a76"\u6e38\u620f\u9759\u9ed8\u9000\u51fa"\u6761\u76ee'),
]

HEAD_S9 = '''
## 9. \u6587\u6863\u7ef4\u62a4\uff08\u7d22\u5f15\u518d\u751f\u6210\uff09

- \u6539\u4e86\u4efb\u4f55 `.md`\uff08\u65b0\u589e\u7ae0\u8282 / \u8ffd\u52a0 features \u6279\u6b21 / \u65b0\u589e memory \u65e5\u5fd7 / \u65b0\u589e `_chk` \u811a\u672c\uff09\u540e\u8dd1\u4e00\u6b21\uff1a
  `python _chk/build_doc_index.py --apply`\uff08\u5e42\u7b49\uff1b\u5148 `--check` \u53ea\u770b\u5dee\u5f02\uff09\u3002
- \u5e38\u9a7b\u7ea2\u7ebf\u7684\u65b0\u53d1\u73b0 \u2192 \u5199\u8fdb\u672c\u6587\u4ef6\u5bf9\u5e94\u8282\uff08\u00a71/\u00a72/\u00a73/\u00a75 \u6216 \u00a76 \u7cbe\u7b80\u8868\uff09\uff1b
  \u957f\u5c3e\u7ec6\u8282 \u2192 \u5199\u8fdb `docs/handbook/*.md` **\u5e76**\u5728 `docs/features.md` \u8ffd\u52a0\u4e00\u6279\u8bb0\u5f55\u3002
- **\u672c\u6587\u4ef6\u6709 64KB \u786c\u9884\u7b97**\uff08\u8d85\u4e86\u5c31\u88ab\u622a\u65ad\uff09\u3002`build_doc_index.py --check` \u4f1a\u62a5\u5360\u7528\uff1b
  \u63a5\u8fd1 54KB \u5c31\u5f97\u628a\u957f\u5c3e\u642c\u53bb `docs/handbook/`\uff0c\u800c\u4e0d\u662f\u7ee7\u7eed\u5f80\u672c\u6587\u4ef6\u91cc\u5806\u3002
- `.workbuddy/memory/` \u4e0e `docs/features.md` \u662f**\u8ffd\u52a0\u5f0f\u5f52\u6863**\uff1a\u53ea\u5f80\u540e\u5199\uff0c\u4e0d\u56de\u6539\u65e7\u6761\uff1b\u65e7\u7ed3\u8bba\u88ab\u63a8\u7ffb\u65f6\u65b0\u5f00\u4e00\u6761\u5e76\u5199\u660e\u300c\u4fee\u6b63\u67d0\u65e5\u65e7\u7ed3\u8bba\u300d\u3002
'''

if __name__ == '__main__':
    main()
