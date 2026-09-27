#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把「火焰改为整体亮红色（含先回退黑金方案）」记入文档（幂等）。

- 变更记录**追加**到 docs/features.md（只往后写，不删旧记录 —— 黑金那次也算历史）
- docs/title-scene-structure.md §8 的「已执行」提示要**替换**成当前状态（活文档，
  不能留两条互相矛盾的「已执行」）

用法：python _chk/record_fireball_red.py [--check]
"""

import argparse
import io
import os
import sys

if hasattr(sys.stdout, 'reconfigure'):
    sys.stdout.reconfigure(encoding='utf-8', errors='replace')

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FEATURES = os.path.join(ROOT, 'docs', 'features.md')
TITLE_DOC = os.path.join(ROOT, 'docs', 'title-scene-structure.md')

# ⚠️ 幂等标记必须与 BATCH 里**实际写入的标题**逐字一致。
# 早先这里写成「（回退黑金方案）」而批次标题是「（并回退此前的黑金方案）」，
# 判定不成立 ⇒ 第二次运行又追加了一份重复批次。现已对齐。
MARK = '2026-09-21 标题火把火焰改为「整体亮红色」（并回退此前的黑金方案）'

BATCH = '''
# 2026-09-21 标题火把火焰改为「整体亮红色」（并回退此前的黑金方案）

## 需求
黑金方案效果不够好 ⇒ 先**回退到上游原版**，再把火焰改成整体亮红色。

## 回退（先做，且留了证据）
`python _chk/verify_fireball.py --restore` 从 `_chk/_bak_fireball/` 恢复两张原图，
恢复后 `git status` 对这两个文件**报「未修改」**（即逐字节回到上游原版），
且与备份 sha256 一致（`fireball-tall.png` `74cb3960…`、`fireball-short.png` `d46f5840…`）。

## 做法：新增 ramp（整条亮度色阶）模式
脚本原来只有 blackgold（阈值分段：大部分压黑 + 少数变金）一种形状，
这次加了 `--mode ramp`：**整条亮度色阶**，暗端 → 亮端贯穿全图、单一色相。
以后再换配色（红/蓝/紫…）直接给两端色即可，不用改代码。

```
L : 暗端 (96,6,8) → 亮端 (255,74,52)      --mode ramp --dark R,G,B --bright R,G,B [--gamma G]
```

暗端刻意**不是纯黑**：纯黑会让半透明外焰在深色背景上变成一团读不出颜色的黑影，
「整体亮红」就不成立。给成暗红后，全图（含最暗的 1/4）都仍明确是红色。

**⚠️ 中途发现并修掉的两个自身缺陷（都记下来免得重犯）**
1. **阈值判据不一致**：`apply_lut` 用 `int(亮度×255)` 截断后查表，而「金区/黑区」分区却用
   浮点亮度判 ⇒ 阈值附近几百个像素两边落在不同分支。表现是「只改了暗色参数，金色区却被动了几百个像素」。
   现已统一走**整数亮度索引**（`lum_index()`），红色方案下金区与暗色参数因此**严格无关**（已实测 0 像素）。
2. **核验脚本分区标签反了**：`pairs` 按降序排（算分位数需要），却又拿 `[:q]` 当「最暗」⇒
   「暗部仍是红」这条断言实际测的是**亮部**。已改为另取升序副本。

## 结果
| 贴图 | 整体均色 | 最暗 1/4 | 最亮 1/4 | 均 alpha | 不透明像素 |
|---|---|---|---|---|---|
| `fireball-tall.png` | (49,237,91) → **(198,50,36)** | (0,189,58) → (169,37,28) | (143,255,135) → (225,61,44) | 49 → 49 | 4588 → 4588 |
| `fireball-short.png` | (111,246,120) → **(215,57,41)** | (10,218,70) → (182,43,32) | (200,255,170) → (239,67,47) | 58 → 58 | 3500 → 3500 |

**alpha 逐像素完全不变**，可见占比与不透明像素数与改前完全相同 ⇒ 火焰形状/轮廓一点没动，只换了颜色。
体积：`88688 → 82970 B`（93.6%）、`49725 → 45473 B`（91.4%）。

## 参数与复现
```
python _chk/recolor_fireball.py --check  --mode ramp
python _chk/recolor_fireball.py --apply  --mode ramp [--dark R,G,B] [--bright R,G,B] [--gamma G]
python _chk/recolor_fireball.py --preview            # 原图/新图逐帧对照
python _chk/verify_fireball.py                       # 19 项核验（按 RECOLORED.txt 里的模式断言）
python _chk/verify_fireball.py --restore             # 一键还原原版
```
想更红更烈：`--bright 255,60,40 --gamma 1.4`（gamma>1 压暗中间调 ⇒ 暗部更沉、对比更强）。
想更亮更均匀：`--gamma 0.8`。

## 影响面
`Fireball` 类**不是标题界面独占**：`WelcomeScene:240` 也 `new Fireball()`
⇒ **开场界面的火把一起变红**，两个界面共用同一套贴图。横幅与滚动背景本次未动。

## 备份与还原
原图备份 `_chk/_bak_fireball/`（**故意不放 assets 里**，否则会被打进 APK）。
`python _chk/verify_fireball.py --restore` 一键还原。`Fireball.java` 未动（sha256 见核验输出）。

## 未做 / 待人工验证
- **未启动游戏**，只做源码级与字节级核验。
- 需**重新构建**才能在游戏里看到效果。
- 红色浓淡（是否够亮/够红）只能人工在游戏里判断；调参重跑即可（脚本幂等、有备份保护）。

'''

NOTE_OLD_START = '> **已执行（2026-09-21）**：两张火焰图已改为「黑色为主 + 掺杂金黄」'
NOTE_NEW = ('> **当前状态（2026-09-21）**：两张火焰图已改为**整体亮红色** '
            '（`--mode ramp`，暗端 (96,6,8) → 亮端 (255,74,52)）。'
            '此前试过的「黑色为主 + 掺杂金黄」方案**已回退**（先 restore 回上游原版再改红）。\n'
            '> 原图备份 `_chk/_bak_fireball/`；还原 `python _chk/verify_fireball.py --restore`；'
            '核验 `python _chk/verify_fireball.py`（19 项，按 `RECOLORED.txt` 里的模式断言）。\n'
            '> 注意 `Fireball` 也被 `WelcomeScene:240` 使用 ⇒ 开场界面的火把一起变红。'
            '**横幅与滚动背景本次未动。** 完整记录见 `docs/features.md`。\n')


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--check', action='store_true')
    parser.add_argument('--apply', action='store_true')
    args = parser.parse_args()

    with io.open(FEATURES, encoding='utf-8', newline='') as fh:
        feats = fh.read()
    with io.open(TITLE_DOC, encoding='utf-8', newline='') as fh:
        doc = fh.read()

    changed = []

    if MARK in feats:
        print('features.md：已记录，跳过')
    else:
        feats = feats.rstrip('\n') + '\n' + BATCH
        changed.append('docs/features.md')

    lines = doc.split('\n')
    idx = [i for i, l in enumerate(lines) if l.startswith(NOTE_OLD_START)]
    if NOTE_NEW.split('\n')[0] in doc:
        print('title-scene-structure.md：已更新为红色状态，跳过')
    elif len(idx) == 1:
        # 替换「已执行」块的 3 行（它是以 '>' 开头的连续段落）
        start = idx[0]
        end = start
        while end < len(lines) and lines[end].startswith('>'):
            end += 1
        lines[start:end] = NOTE_NEW.rstrip('\n').split('\n')
        doc = '\n'.join(lines)
        changed.append('docs/title-scene-structure.md')
    else:
        raise SystemExit('title 文档里找不到唯一的黑金「已执行」块（命中 %d 处），人工确认后再改' % len(idx))

    print('待写入：%s' % (', '.join(changed) if changed else '（无）'))
    if args.check:
        print('[check] 未写盘。')
        return 0
    for path, text in ((FEATURES, feats), (TITLE_DOC, doc)):
        rel = os.path.relpath(path, ROOT).replace(os.sep, '/')
        if rel in changed:
            with io.open(path, 'w', encoding='utf-8', newline='') as fh:
                fh.write(text)
            print('  已写入', rel)
    print('[apply] 完成。下一步：python _chk/build_doc_index.py --apply')
    return 0


if __name__ == '__main__':
    sys.exit(main())
