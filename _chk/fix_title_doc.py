#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""修正 docs/title-scene-structure.md 里的过期表述与占位符（幂等）。

初稿有三处错/待补，本脚本统一收口：
  ① 把「横屏高度不变量 ≥216px」改成正确结论（竖屏只剩 14px 余量才是真约束）
  ② §2 元素表里横屏横幅尺寸 240×157 → 240×57（uvRect 后两参是右下角坐标）
  ③ 清掉 `[待补]` 占位与一处错误的 `_chk/doc-index` 引用

用法：python _chk/fix_title_doc.py [--check]
"""

import argparse
import io
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(ROOT, 'docs', 'title-scene-structure.md')

FIXES = [
    ('> `[待补]` 标记的部分由并行调研补充。',
     '> 并行调研已全部归档；本文不再含待填占位。'),
    ('3. **横屏有一条高度不变量：相机高必须 ≥ 216px**，否则按钮直接跑出屏幕（§3）。',
     '3. **竖屏最矮机型只剩 14px 余量** —— 再加一行按钮就溢出；横屏则宽裕（见 §3.4）。'),
    ('标题贴图（竖 139×100 / 横 240×157）',
     '标题贴图（竖 139×100 / 横 240×**57**）'),
    ('更新流程用（`[待补]` 确认消费点）',
     '更新流程用；**本场景内无消费点**（见 §13）'),
    ('⑤ 需要新图标见 `_chk/doc-index` → `ui/Icons.java` 帧号分配',
     '⑤ 需要新图标见 §8（`icons.png` 的 `x=154..255, y=0..15` 是空白区）'),
]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    with io.open(DOC, encoding='utf-8', newline='') as handle:
        text = handle.read()

    applied = 0
    for old, new in FIXES:
        if new in text and old not in text:
            continue                      # 已改过
        hits = text.count(old)
        if hits != 1:
            raise SystemExit('未唯一命中（%d 次）：%r' % (hits, old[:60]))
        text = text.replace(old, new)
        applied += 1

    left = text.count('[待补]')
    print('修正 %d 处；残留 [待补] = %d' % (applied, left))
    if left:
        raise SystemExit('仍有待填补占位符，人工处理后重跑')

    if args.check:
        print('[check] 未写盘。')
        return 0
    with io.open(DOC, 'w', encoding='utf-8', newline='') as handle:
        handle.write(text)
    print('[apply] 已写入', os.path.relpath(DOC, ROOT))
    return 0


if __name__ == '__main__':
    sys.exit(main())
