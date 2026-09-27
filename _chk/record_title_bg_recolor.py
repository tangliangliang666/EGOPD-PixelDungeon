#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把「标题滚动背景改深红」这次操作记入文档（幂等）。

按项目约定：变更记录追加到 `docs/features.md`（只往后写），
并在 `docs/title-scene-structure.md` 的 §15 留一行「已执行」指针。

用法：python _chk/record_title_bg_recolor.py [--check]
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

MARK = '2026-09-21 标题滚动背景改为深红色调'

BATCH = '''
# 2026-09-21 标题滚动背景改为深红色调（横幅不动）

## 需求
把标题界面的滚动背景整体改成深红色调；横幅 `banners.png` 保持不变。

## 做法：为什么只改调色板就够了
4 张图层全是 **8 位索引 PNG（PLTE + tRNS）** ⇒ 改色 = **只重写 PLTE**，
IDAT（像素索引）**逐字节保留** ⇒ 尺寸 / 透明度 / 结构**零风险**，而且没有量化误差。
深红化用**双色调（duotone）**：取亮度后映射到「暗红 (18,2,4) → 亮红 (232,64,46)」色阶，
`gamma=1.0`、`mix=1.0`。这样原图的明暗结构与剪影全部保留，只把色相统一压到深红。

## 落点
- **改色**：`core/src/main/assets/splashes/title/` 下 4 张 ——
  `archs.png`、`back_clusters.png`、`mid_mixed.png`、`front_small.png`
- **未改**：`core/src/main/assets/interfaces/banners.png`（横幅。它是 **RGBA** 不是索引图，
  调色板改色在物理上就碰不到它；另有 mtime 证据：其 mtime 为 2026-09-04，早于本次操作）
- **未改任何代码**：`TitleBackground.java` 的 4 个 `TextureFilm` 帧尺寸与 4 个 `INIT_*_CHANCES`
  数组长度**一个都没动** ⇒ 帧网格仍成立

## 帧网格核对（改色后仍成立）
| PNG | 帧尺寸 | 网格 | 可用帧 | 权重数组需 |
|---|---|---|---|---|
| `archs.png` | 333×100 | 3×2 | 6 | 6 |
| `back_clusters.png` | 450×250 | 1×2 | 2 | 2 |
| `mid_mixed.png` | 273×242 | 7×4 | 28 | 24 |
| `front_small.png` | 112×116 | 9×4 | 36 | 20 |

## 参数与复现
```
python _chk/recolor_title_bg.py --check                  # 只看调色板统计
python _chk/recolor_title_bg.py --apply [--dark R,G,B] [--light R,G,B] [--gamma G] [--mix M]
python _chk/recolor_title_bg.py --preview                # 生成上排原图/下排新图的对照 PNG
python _chk/verify_title_bg.py                           # 39 项结构核验
python _chk/verify_title_bg.py --restore                 # 一键还原原图
```
`--mix 0.6` 可退化成「只染 60%」的轻微染色；`--dark/--light` 调深红色阶两端。

## 核验结果（39 项全通过）
尺寸 / 位深 / 色型 / 交错不变；**IDAT 逐字节不变**；**tRNS 逐字节不变**；
PLTE 项数不变且内容确实变了；逐项 alpha 不变（全透明/半透明/不透明的项数一致）；
可见像素均色 **(49,40,25) → (43,9,9)**（R/G = R/B ≈ 4.78，且整体仍然很暗）；
帧网格成立；横幅未改动。

## 影响面（重要）
这套滚动背景是 **10 个界面共用**的，不只标题界面：
`TitleScene` / `WelcomeScene` / `StartScene`（存档槽）/ `RankingsScene`（排行榜）/
`JournalScene`（日志）/ `NewsScene`（新闻）/ `ChangesScene`（改动）/ `AboutScene`（关于）/
`SupporterScene`（支持）/ `SurfaceScene`（地表结算）
⇒ **这 10 个界面的背景一起变红。**

## 备份与还原
原图备份在 `_chk/_bak_title_bg/`，**故意不放在 assets 里**（否则会被打进 APK 撑大包体）。
一键还原：`python _chk/verify_title_bg.py --restore`。
`banners.png` 未进备份（它没被改），其 sha256 已记在 `_chk/_bak_title_bg/RECOLORED.txt`。

## 未做 / 待人工验证
- **未启动游戏**，只做了源码级与字节级核验。
- 需**重新构建**才能在游戏里看到效果：`desktop/build/resources/main/` 与
  `android/build/intermediates/**/merge*Assets/` 里仍是上一次构建的旧图。
- 观感（深红是否够味、是否与 UI 撞色）只能人工在游戏里判断；
  不满意可调 `--dark/--light/--gamma/--mix` 重跑（脚本幂等，且有备份保护）。
- 横幅本身**未动**，按需求保持原样。

'''

NOTE = ('> **已执行（2026-09-21）**：这 4 张图已改为深红双色调，横幅未动；原图备份在 '
        '`_chk/_bak_title_bg/`，还原用 `python _chk/verify_title_bg.py --restore`。'
        '核验 `_chk/verify_title_bg.py`（39 项）。完整记录见 `docs/features.md`。\n')


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

    anchor = '### 15.2 滚动背景（4 张图 + 一处代码）\n'
    if NOTE.strip() in doc:
        print('title-scene-structure.md：已记录，跳过')
    else:
        if doc.count(anchor) != 1:
            raise SystemExit('§15.2 锚点未唯一命中')
        doc = doc.replace(anchor, anchor + NOTE, 1)
        changed.append('docs/title-scene-structure.md')

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
