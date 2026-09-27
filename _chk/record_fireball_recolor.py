#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把「标题火把火焰改黑金」这次操作记入文档（幂等）。

变更记录追加到 docs/features.md；
并在 docs/title-scene-structure.md 的 §8「Fireball」条目后留一行「已执行」指针。

用法：python _chk/record_fireball_recolor.py [--check]
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

MARK = '2026-09-21 标题火把火焰改为「黑金」（黑色为主 + 掺杂金黄）'

BATCH = '''
# 2026-09-21 标题火把火焰改为「黑金」（黑色为主 + 掺杂金黄）

## 需求
把标题界面的燃烧火焰效果改成「掺杂部分金黄色的黑色火焰」。

## 改前是什么颜色（可能出乎意料）
上游 SPD 的标题火把是**绿焰**（不是橙焰）：亮部均色 `(225,255,186)`、核心 `(0,234,69)`。
两张图与 `Fireball.java` 经 git 核对**均为上游原样未改**。

## 为什么是逐像素变换而不是改调色板
这两张是 **RGBA（color type 6）**，没有 PLTE ⇒ 只能变换 RGB 并**保持 alpha 逐像素不变**。
无损性依然成立：尺寸 / 位深 / 色型 / 交错不变、alpha 完全不动、无缩放重采样。

## 映射：两段各自独立的色阶
```
L ≤ thr : 黑 (10,8,10) → 余烬 (70,30,8)      大片低 alpha 的外焰 → 暗影/烟熏
L >  thr : 暗金 (206,146,30) → 亮金 (255,224,116)   最亮的焰心 → 金黄
```
分界点 `thr` 由「要让多少比例的像素变金」反推（`--gold-quantile`，默认 0.28，**alpha 加权**）。
必须按分位数而不能用固定阈值：两张图的亮度分布差很多
（tall 只有 3.5% 像素在 0.90 以上；short 有 9.2%，且 51.8% 落在 0.75–0.90），
固定阈值会让 short 几乎全变金，达不到「掺杂**部分**金黄」。

**踩过的坑（已修，记下来免得重犯）**：第一版用「余烬 → 金」一条连续色阶跨过阈值，
结果绝大多数「金区」像素落在阈值刚过一侧、插值到余烬附近，金区均色只有 `(141,90,20)`
——是暗琥珀不是金黄。改成金区**从真正的金起步**（阈值处直接等于 `gold_dim`）才读得出金色。
代价是阈值处有一步跳变，但它正好落在焰心亮部边界上，像素画风格里反而更利落。

## 落点
- **改色**：`core/src/main/assets/effects/fireball-tall.png`（横屏 61×61×24）、
  `core/src/main/assets/effects/fireball-short.png`（竖屏 47×47×24）
- **未改任何代码**：贴图路径是 `Fireball.java` 里的**硬编码字符串**，文件名不变即可，
  4 个帧网格与 `MovieClip.Animation` 帧表一个都没动
- **未改**：`effects/fireball.png` —— 注意 `Assets.FIREBALL = "effects/fireball.png"` 在**本作是死常量**
  （0 引用、文件也不存在），别被它误导

## 结果
| 贴图 | 可见均色 | 均 alpha | 不透明像素 | 「金」像素占可见 |
|---|---|---|---|---|
| `fireball-tall.png` | (49,237,91) → **(68,37,13)** | 49 → 49 | 4588 → 4588 | 0% → **8.2%** |
| `fireball-short.png` | (111,246,120) → **(67,34,11)** | 58 → 58 | 3500 → 3500 | 0% → **5.8%** |

金区均色 `(224,174,62)` / `(204,153,49)`（真金黄），黑区均色 `(54,24,9)` / `(57,25,8)`（近黑带暖）。
**可见像素占比、均 alpha、不透明像素数三项与改前完全相同** ⇒ 透明轮廓一点没动。

体积反而变小：`88688 → 75728 B`（85.4%）、`49725 → 40670 B`（81.8%）——色数变少，zlib 压得更好。

## 参数与复现
```
python _chk/recolor_fireball.py --check
python _chk/recolor_fireball.py --apply [--gold-quantile Q] [--black R,G,B] [--ember R,G,B] \\
                                       [--gold-dim R,G,B] [--gold R,G,B]
python _chk/recolor_fireball.py --preview      # 生成原图/新图逐帧对照
python _chk/verify_fireball.py                 # 17 项核验
python _chk/verify_fireball.py --restore       # 一键还原原图
```
想要更「金」用 `--gold-quantile 0.45`（金区升到约 14~20% 像素）；
想让黑的部分更「有形」可把 `--black` 提亮到 `30,26,30`（纯黑在深色背景上会显得像一块阴影）。

## ⚠️ 观感前提（重要）
原图**大部分像素是低 alpha**（tall 中等亮度带的平均 alpha 只有 23~46，只有 3.5% 达 255）。
所以「黑色」在深色标题背景上主要表现成**把背景压暗的阴影**，而不是一块纯黑；
**真正「看得见」的形是那 6~8% 的金黄焰心**。这正是黑焰该有的烟熏感，但据此调参时要有预期。

## 影响面
`Fireball` 类**不是标题界面独占**：`WelcomeScene:240` 也 `new Fireball()`
⇒ **开场界面（冷启动那屏）的火把会一起变黑金**，两个界面共用同一套贴图。

## 备份与还原
原图备份在 `_chk/_bak_fireball/`（**故意不放 assets 里**，否则会被打进 APK）。
一键还原：`python _chk/verify_fireball.py --restore`。
`Fireball.java` 未进备份（它没被改），其 sha256 记在核验输出里。

## 未做 / 待人工验证
- **未启动游戏**，只做了源码级与字节级核验。
- 需**重新构建**才能在游戏里看到：`desktop/build/resources/main/effect…` 与
  `android/build/intermediates/**/merge*Assets/` 里仍是上一次构建的旧图。
- 观感（黑金比例是否合适、在深色背景上是否够「有形」）只能人工在游戏里判断；
  不满意可调 `--gold-quantile / --black / --gold-dim / --gold` 重跑（脚本幂等、有备份保护）。

'''

NOTE = ('> **已执行（2026-09-21）**：两张火焰图已改为「黑色为主 + 掺杂金黄」'
        '（`fireball-tall/short.png`，原图备份 `_chk/_bak_fireball/`，还原 '
        '`python _chk/verify_fireball.py --restore`）。**横幅与滚动背景本次未动。**\n'
        '> 注意 `Fireball` 也被 `WelcomeScene:240` 使用 ⇒ 开场界面的火把一起变。'
        '完整记录见 `docs/features.md`。\n')

ANCHOR = ('- **`Fireball` 是 `MovieClip`**，无参构造靠一个静态 `second` 让相邻两只**交替镜像**')


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

    if NOTE.strip() in doc:
        print('title-scene-structure.md：已记录，跳过')
    else:
        if doc.count(ANCHOR) != 1:
            raise SystemExit('§8 Fireball 条目锚点未唯一命中（%d 次）' % doc.count(ANCHOR))
        doc = doc.replace(ANCHOR, NOTE + ANCHOR, 1)
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
