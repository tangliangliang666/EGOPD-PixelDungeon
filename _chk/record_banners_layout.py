#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把「横幅改新版面」记入文档（幂等）：features.md 追加批次 + 标题文档 §15.1 更新为当前状态。

§15.1 原来记的是旧版面（139×100 / 240×57），是**活文档**，必须改成当前值，
并补上这次实测出来的两个风险（横幅变宽会被裁、画面底边多 1 行）。

用法：python _chk/record_banners_layout.py [--check]
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

MARK = '2026-09-21 标题横幅改新版面（闪烁帧保留但置空）'

BATCH = '''
# 2026-09-21 标题横幅改新版面（闪烁帧保留但置空）

## 需求
用户手工重排了 `interfaces/banners.png`：
```
移动端标题 (0,0)-(169,94)      169×94     （旧 139×100）
PC 端标题  (176,0)-(427,65)    251×65     （旧 240×57）
闪烁帧保留以供后续开发，但**置为空**
BOSS_SLAIN / GAME_OVER 保持不变
```
要求据此配置，以便检查视觉效果。

## 做法
只改 `effects/BannerSprites.java` 的 4 个 `uvRect`（本项目对该文件的第一处修改，7 增 4 删）：

| Type | 新 uvRect | 尺寸 | 说明 |
|---|---|---|---|
| `TITLE_PORT` | `(0, 0, 169, 94)` | 169×94 | 移动端标题 |
| `TITLE_LAND` | `(176, 0, 427, 65)` | 251×65 | PC 端标题 |
| `TITLE_GLOW_PORT` | `(253, 66, 422, 160)` | 169×94 | **空帧**（尺寸刻意与对应标题帧一致） |
| `TITLE_GLOW_LAND` | `(253, 66, 504, 131)` | 251×65 | **空帧** |

**空帧是怎么置空的**：不删枚举值、不动 `TitleScene:125` / `WelcomeScene:127` 的代码，
而是把 uvRect 指向图内一块**实测全透明**的区域（`(253,66)` 起，来自「最大空白矩形」算法算出的
259×190 空白区）。这样闪烁帧仍会参与 alpha 动画与 `Blending.setLightMode()`，只是画不出东西 ——
以后画好闪烁素材，把这两个 uvRect 改到真实位置即可。尺寸刻意取成**与对应标题帧相同**，
这样将来直接叠在标题上时定位天然对齐。

## 实测发现（两个都要你定夺）

### ① 横幅变宽 ⇒ 在常见机型上**会被裁**
`TitleScene:112` 是 `title.x = left + (w - 横幅宽)/2`，所以**相机宽 ≥ 横幅宽才不被裁**。
新版比旧版宽（竖 139→169、横 240→251），而各模式最小相机宽只有 135 / 240：

| 机型（估算 density） | 相机宽 | 竖屏横幅 169 |
|---|---|---|
| 1080×2400 @2.75 | ≈154 | ⚠️ 被裁 15 px |
| 1440×3200 @3.50 | ≈160 | ⚠️ 被裁 9 px |
| 720×1280 @2.00 | ≈144 | ⚠️ 被裁 25 px |

旧版 139 宽的横幅在这些机型上都放得下（144 > 139）⇒ **这是新版面引入的新问题**。
横屏同理：251 > 240，最小宽度下会被裁 11 px（常见 16:9 横屏相机更宽，一般不触发）。
要彻底避免，竖屏画面宽需 ≤ ~144（取上述最小相机宽的下界）。

### ② 两块标题画面的底边比帧多 1 行
实测画面包围盒：移动端 y=0..94、PC 端 y=0..65，而帧只到 93 / 64；那一行是**满宽且 alpha 255**。
按用户给的原值配置（原值即规格），若要补上就把 y1 各 +1：
`TITLE_PORT (0,0,169,95)`、`TITLE_LAND (176,0,427,66)`。

## 核验与预览工具
```
python _chk/analyze_banners.py            # 帧内容包围盒 / 逐边 1px 切断检测 / 最大空白矩形
python _chk/verify_banners.py             # 18 项：矩形值、空帧必须真透明、BOSS/GAME_OVER 未误伤、适配报告
python _chk/render_title_preview.py --devices   # 渲染模拟标题界面预览图（供人工看效果）
```
`verify_banners.py` 的关键断言是 **「空帧指向的区域在图内必须全透明」** ——
这是「空帧」这个约定的全部依据；素材一旦挪动导致那块不再为空，空帧就会画出错位的碎片，
而那种错误在真机上表现为「标题旁边多了几片莫名其妙的图案」，很难反查。

## 核验结果（18 项全通过）
矩形值与约定逐项一致；两个空帧区域实测 **0 个非透明像素**；
两个标题帧内分别有 8308 / 8668 个非透明像素；`BOSS_SLAIN` `(0,157,127,225)` 与
`GAME_OVER` `(128,157,256,192)` **保持上游原值**且区域内仍有画面（3861 / 2830 像素）；
六个矩形全部在图内；`BannerSprites.java` 合法 UTF-8；备份存在；闪烁帧仍有两个消费方。

## 备份
`_chk/_bak_title_assets/banners.png` —— 这是**手工资产且没有 git 版本可回退**
（git 里的 HEAD 版本是 mod 早先替换过的自定义图，不是本次手工改的版本），所以必须备份。

## 未做 / 待人工验证
- **未启动游戏**，只做源码级与字节级核验。
- 预览图 `_chk/_title_preview_*.png` 是**模拟**：横幅裁切与按钮坐标精确，
  但背景是**静态近似**（真机是 6 层视差滚动），按钮里的中文无法渲染（用编号 + 图例代替）。
- 需**重新构建**才能在游戏里看到。

'''

SEC_OLD = '''| Type | uvRect | 尺寸 | 用在哪 |
|---|---|---|---|
| `TITLE_PORT` | `(0, 0, 139, 100)` | 139×100 | 竖屏标题（`TitleScene:107`） |
| `TITLE_GLOW_PORT` | `(139, 0, 278, 100)` | 139×100 | 竖屏发光（`TitleScene:125`） |
| `TITLE_LAND` | `(0, 100, 240, 157)` | 240×**57** | 横屏标题 |
| `TITLE_GLOW_LAND` | `(240, 100, 480, 157)` | 240×**57** | 横屏发光 |
'''

SEC_NEW = '''> **当前版面（2026-09-21 用户手工重排，本节已同步为最新值）**：闪烁帧**保留但置空**。

| Type | uvRect | 尺寸 | 用在哪 |
|---|---|---|---|
| `TITLE_PORT` | `(0, 0, 169, 94)` | **169×94** | 竖屏标题（`TitleScene:107`） |
| `TITLE_LAND` | `(176, 0, 427, 65)` | **251×65** | 横屏标题 |
| `TITLE_GLOW_PORT` | `(253, 66, 422, 160)` | 169×94 | **空帧**（指向实测全透明区；尺寸与标题帧一致，便于日后叠加） |
| `TITLE_GLOW_LAND` | `(253, 66, 504, 131)` | 251×65 | **空帧** |

⚠️ **两个待定夺的实测问题**（详见 `docs/features.md` 同日批次）：
1. **横幅变宽会被裁**：`TitleScene:112` 是 `x = left + (w - 横幅宽)/2`，**相机宽 ≥ 横幅宽才不裁**。
   新版 169（竖）/ 251（横）都大于各模式最小相机宽 135 / 240；常见手机竖屏相机宽约 144~160
   ⇒ **会被裁 9~25 px**（旧版 139 宽不会）。要避免需把竖屏画面宽压到 ≤ ~144。
2. **两块标题画面底边比帧多 1 行**（满宽、alpha 255）：补上则 y1 各 +1 →
   `(0,0,169,95)` / `(176,0,427,66)`。

自检：`python _chk/verify_banners.py`（18 项，关键断言是「空帧区域必须真透明」）；
预览：`python _chk/render_title_preview.py --devices`（模拟渲染，供人工看效果）。
'''


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--check', action='store_true')
    ap.add_argument('--apply', action='store_true')
    args = ap.parse_args()

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

    if SEC_NEW.split('\n')[0] in doc:
        print('title-scene-structure.md §15.1：已更新，跳过')
    elif doc.count(SEC_OLD) == 1:
        doc = doc.replace(SEC_OLD, SEC_NEW, 1)
        changed.append('docs/title-scene-structure.md')
    else:
        raise SystemExit('§15.1 旧表格锚点未唯一命中（%d 次）' % doc.count(SEC_OLD))

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
