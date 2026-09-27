#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""核验 AGENTS.md 的「常驻核心 + handbook 长尾」重构没有丢字（回归断言）。

判据（全部是字节级）：
    A. 冻档 `docs/archive/AGENTS-2026-09-21-full.md` 去掉表头后，与原始 AGENTS.md 逐字节相同
       （以冻档自身为基准重算分节，从而验证分节锚点仍然有效）
    B. 每个 `docs/handbook/*.md` 去掉自己的说明表头后，与冻档里对应节**逐字节相同**
    C. 新 AGENTS.md 里仍然**原样保留**的节（§1/§2/§3 核心/§3 APK 段/§5）逐字节出现在新文件里
    D. 新 AGENTS.md 里被迁走的节（§4/§6/§7/§8 正文）**不再**出现在新文件里（避免同一份内容两处漂移）
    E. 每个 handbook 文件与新 AGENTS.md 都在 §0/§9 的地图里被登记（引用路径真实存在）

用法：python _chk/verify_doc_split.py
"""

import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')
    sys.stderr.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AGENTS = os.path.join(ROOT, 'AGENTS.md')
ARCHIVE = os.path.join(ROOT, 'docs', 'archive', 'AGENTS-2026-09-21-full.md')
HANDBOOK = os.path.join(ROOT, 'docs', 'handbook')

BODY_MARK = '> \u672c\u6587\u4ef6\u662f\u957f\u5c3e\u539f\u6587\uff0c**\u6309\u9700\u5b9a\u70b9\u8bfb\u53d6\uff0c\u4e0d\u8981\u6574\u8bfb**\u3002\n\n'

S1 = '## 1. \u6784\u5efa\u4e0e\u8fd0\u884c\uff08\u5feb\u901f\u53c2\u8003\uff09'
S2 = '## 2. \u6587\u672c\u4e0e\u672c\u5730\u5316'
S3 = '## 3. \u8d34\u56fe\u4e0e\u56fe\u6807'
S4 = '## 4. SPD \u6846\u67b6\u673a\u5236\u7ecf\u9a8c'
S5 = '## 5. \u804c\u4e1a\u4e0e\u529f\u80fd\u6863\u6848\uff08\u5185\u5bb9\u5df2\u62c6\u5206\u81f3 docs/\uff09'
S6 = '## 6. \u5e38\u89c1\u9677\u9631\u901f\u67e5'
S7 = '## 7. \u81ea\u5b9a\u4e49\u6b66\u5668/\u7269\u54c1\u5f00\u53d1\u7ecf\u9a8c\uff08\u901a\u7528\uff09'
S8 = '## 8. \u6cd5\u6756\u7c7b\u6b66\u5668\uff08\u5931\u4e50\u56ed\uff09\u4e0e buff \u7279\u6548\u5f00\u53d1\u7ecf\u9a8c'
TILEMAP = '- **\u5730\u56fe\u8d34\u56fe\uff1d\u300c\u5206\u5c42 tilemap + \u8de8\u683c\u62fc\u5e27\u300d**'
APK = '- **APK \u684c\u9762\u56fe\u6807 / \u5e94\u7528\u540d**'

ORDER = [S1, S2, S3, S4, S5, S6, S7, S8]

# 迁出时**有意**改写的交叉引用（旧节号 → handbook 实际路径）。每条必须唯一命中，
# 否则说明 AGENTS.md 被别处改动、或补丁失效 —— 两种情况都要人来看一眼。
KNOWN_PATCHES = [
    ('\u5148\u67e5\u7b2c 6 \u8282"\u6e38\u620f\u9759\u9ed8\u9000\u51fa"\u6761\u76ee',
     '\u5148\u67e5 `docs/handbook/pitfalls.md` \u00a76"\u6e38\u620f\u9759\u9ed8\u9000\u51fa"\u6761\u76ee'),
    ('\u5b8c\u6574\u793a\u4f8b\u4ecd\u4fdd\u7559\u5728\u672c\u624b\u518c \u00a74\u3002',
     '\u5b8c\u6574\u793a\u4f8b\u4ecd\u4fdd\u7559\u5728 `docs/handbook/spd-framework.md` \u00a74\u3002'),
]


def apply_patches(chunk):
    """把有意补丁应用到期望文本上，并记录命中次数。"""
    hits = 0
    for old, new in KNOWN_PATCHES:
        n = chunk.count(old)
        hits += n
        chunk = chunk.replace(old, new)
    return chunk, hits


FAILS = []
PASSES = []


def check(name, ok, detail=''):
    (PASSES if ok else FAILS).append(name)
    print('%s  %s%s' % ('PASS' if ok else 'FAIL', name, ('  <- ' + detail) if detail else ''))


def read(path):
    with open(path, 'r', encoding='utf-8', newline='') as handle:
        return handle.read()


def split_at(text, marker):
    idx = text.find('\n' + marker)
    if idx < 0:
        if text.startswith(marker):
            return 0
        raise SystemExit('\u951a\u70b9\u672a\u627e\u5230: %s' % marker[:40])
    return idx + 1


def body_of(path):
    text = read(path)
    idx = text.find(BODY_MARK)
    if idx < 0:
        raise SystemExit('handbook 说明表头未找到: %s' % path)
    return text[idx + len(BODY_MARK):]


def main():
    # A. 冻档 == 原文
    archive = read(ARCHIVE)
    marker = '<!-- \u4e0d\u8981\u8bfb\u3001\u4e0d\u8981\u6539\uff1b\u4ec5\u4f9b\u8003\u53e4\u3002\u5e38\u9a7b\u6838\u5fc3\u89c1 AGENTS.md\uff0c\u957f\u5c3e\u539f\u6587\u89c1 docs/handbook/\u3002 -->\n\n'
    idx = archive.find(marker)
    check('A0 \u51bb\u6863\u8868\u5934\u5b8c\u6574', idx >= 0)
    original = archive[idx + len(marker):]

    offsets = {marker: split_at(original, marker) for marker in ORDER}
    chunks = {}
    for i, marker in enumerate(ORDER):
        start = offsets[marker]
        end = offsets[ORDER[i + 1]] if i + 1 < len(ORDER) else len(original)
        chunks[marker] = original[start:end]

    assembled = original[:offsets[S1]] + ''.join(chunks[m] for m in ORDER)
    check('A1 \u51bb\u6863\u5206\u8282\u53ef\u56de\u62fc\u539f\u6587', assembled == original)

    s3 = chunks[S3]
    tm = split_at(s3, TILEMAP)
    apk = split_at(s3, APK)
    s3_core, s3_tilemap, s3_apk = s3[:tm], s3[tm:apk], s3[apk:]
    check('A2 \u00a73 \u5185\u90e8\u4e09\u5206\u53ef\u56de\u62fc',
          s3_core + s3_tilemap + s3_apk == s3)

    # B. handbook 正文 == 冻档对应节
    expect = {
        'terrain-tilemap.md': s3_tilemap,
        'spd-framework.md': chunks[S4],
        'pitfalls.md': chunks[S6],
        'weapon-item-dev.md': chunks[S7],
        'wand-buff-fx-dev.md': chunks[S8],
    }
    for name, chunk in expect.items():
        path = os.path.join(HANDBOOK, name)
        check('B  %s \u9010\u5b57\u7b49\u4e8e\u539f\u6587' % name, body_of(path) == chunk,
              '%d vs %d \u5b57\u8282' % (len(body_of(path).encode('utf-8')), len(chunk.encode('utf-8'))))

    # C/D. 新 AGENTS.md
    agents = read(AGENTS)
    patch_hits = 0
    for name, chunk in (('\u00a71', chunks[S1]), ('\u00a72', chunks[S2]),
                        ('\u00a73 \u6838\u5fc3', s3_core), ('\u00a73 APK \u6bb5', s3_apk),
                        ('\u00a75', chunks[S5])):
        expected, hits = apply_patches(chunk)
        patch_hits += hits
        check('C  AGENTS.md \u4fdd\u7559 %s \u539f\u6587\uff08\u542b\u610f\u56fe\u5185\u7684\u5f15\u7528\u8865\u4e01\uff09' % name,
              expected in agents)
    check('C  \u6709\u610f\u5f15\u7528\u8865\u4e01\u5168\u90e8\u547d\u4e2d\uff08%d/%d\uff09' % (patch_hits, len(KNOWN_PATCHES)),
          patch_hits == len(KNOWN_PATCHES))
    for name, chunk in (('\u00a74', chunks[S4]), ('\u00a76', chunks[S6]),
                        ('\u00a77', chunks[S7]), ('\u00a78', chunks[S8]),
                        ('\u00a73 \u5730\u5f62\u6bb5', s3_tilemap)):
        check('D  AGENTS.md \u5df2\u4e0d\u542b %s \u6b63\u6587' % name, chunk not in agents)

    # E. 引用路径真实存在 + 预算
    refs = ['docs/INDEX.md', 'docs/handbook/terrain-tilemap.md', 'docs/handbook/spd-framework.md',
            'docs/handbook/pitfalls.md', 'docs/handbook/weapon-item-dev.md',
            'docs/handbook/wand-buff-fx-dev.md', 'docs/archive/AGENTS-2026-09-21-full.md',
            'docs/index/features.md', 'docs/index/memory.md', 'docs/index/scripts.md']
    for rel in refs:
        check('E  \u5f15\u7528\u5b58\u5728 %s' % rel, os.path.exists(os.path.join(ROOT, rel)))
    size = len(agents.encode('utf-8'))
    check('E  AGENTS.md \u5728 64KB \u9884\u7b97\u5185\uff08%d \u5b57\u8282\uff09' % size, size <= 55296)

    print('\n%d \u9879\u901a\u8fc7\uff0c%d \u9879\u5931\u8d25' % (len(PASSES), len(FAILS)))
    if FAILS:
        print('\u5931\u8d25\uff1a' + '\u3001'.join(FAILS))
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
