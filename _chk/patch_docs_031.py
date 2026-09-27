# -*- coding: utf-8 -*-
"""追加 v0.3.1 发布记录到 docs/features.md 与今日工作日志。"""
import os

BASE = r'D:/PD'

FEATURES = os.path.join(BASE, 'docs/features.md')
LOG = os.path.join(BASE, '.workbuddy/memory/2026-09-20.md')

FEATURE_TEXT = '''
# 2026-09-20 出 v0.3.1（Bug 修复 + 中指长兄割腕穿透无敌 + EGO 特化神器）

## 一、本版内容

- **版本号**：`appVersionCode 930 → 931`、`appVersionName 0.3.0 → 0.3.1`（根 `build.gradle` 的 `ext`，唯一处；`android/desktop/ios` 都从这里取）。
- **更新日志**：`ui/changelist/EGOPD_Changes.java` 新增 major 条目 `EGOPD v0.3.1`（排在 v0.3.0 **之前**），下挂 3 条：

  | 按钮 | 图标 | 要点 |
  | --- | --- | --- |
  | Bug 修复 | `Icons.get(Icons.SHPX)` | 概括性一条 |
  | 中指长兄：割腕穿透无敌 | `ItemSprite(WRIST_SLIT)` | 无敌期间割腕/蓄血圣杯不再能白送成长，自伤换成长行为**绕过无敌免死判定** |
  | EGO 特化神器 | `ItemSprite(ARTIFACT_BLOOD_FEAST_CHALICE1)` | 5 件强化形态；炼金台「原版神器 + 60 脑啡肽」（12 点能量）合成，保留等级/充能/状态 |

  新增 import：`sprites.ItemSprite` / `sprites.ItemSpriteSheet` / `ui.Icons`。
- `ChangesScene` **无需改动**：EGOPD 页（`EGOPD_TAB = 0`）在 v0.3.0 时已建好并适配过布局（两行 9 个页签按钮），本版**未新增页签** ⇒ case 序号 / `changesSelected` / `setRect` 三处都无需同步。

## 二、核验（源码级 + 打包后四道）

- `_chk/check_utf8_all.py`：**1445 文件**全合法 UTF-8。
- 单文件 `javac -proc:none -Xlint:all`（`EGOPD_Changes.java`）：**EXIT=0，0 错误 0 告警**（javac 无任何输出）。
- `_chk/patch_release_031.py`：**30 条断言 ALL PASS** —— 含「v0.3.1 条目唯一且排在 v0.3.0 之前」「恰 3 条 `addButton`」「括号配平」「无 `/n/` 误写」「三个图标常量在 `ItemSpriteSheet`/`Icons` 里真实存在」「5 个中文名确实在 `items_zh.properties`」，另有改动前**双向断言**（先验旧值 930/`0.3.0` 存在再替换）。
- **打包**：`:android:assembleDebug` → **BUILD SUCCESSFUL in 29s**；`:core:compileJava` 与 `:android:packageDebug` 均 **executed**（非 UP-TO-DATE）⇒ 改动确实进包。
- **① 元数据**（`aapt dump badging`）：`versionCode='931'`、`versionName='0.3.1-INDEV'`、`application-label='EGOPD'`、`application-icon-480/640` 均指向 `res/mipmap-anydpi-v26/ic_launcher.xml`。
- **② 新类/新文本进包**（dex 字面量计数）：`EGOPD_Changes` ×3、`EGOPD v0.3.1` ×1、`中指长兄：割腕穿透无敌` ×1、`EGO 特化神器` ×2、`绕过无敌的免死判定` ×1、`原版神器 + 60 脑啡肽` ×1。
- **③ 全量资产比对**（新脚本 `_chk/verify_apk_assets_031.py`）：APK 内 **533** 个 `assets/*` 与工作区 `core/src/main/assets` **md5 逐条一致**，无单边、无不一致。脚本按 skill 备忘先做 **cp437 → utf-8 中文条目名还原**再比。
- **④ 体积变化可解释**：APK **47.10 MB（49,383,020 B）**；对比上一版 `EGOPD_0.3.0.APK`（49,366,899 B）差 **+16,121 B（+0.033%）** —— 与「只多出一段 changelist 代码 + 常量池」相符（上一版几乎无增量打包空洞，故本次不必量空洞）。
- **归档**：`EGOPD_0.3.1.APK`（仓库根），md5 `dc2f86213da64b97e12933dc9f17045a`，与构建产物一致，`aapt` 复核头部正常。

## 三、待人工验证

- 游戏内「改动」界面 → EGOPD 页签：确认 v0.3.1 三条按钮的**图标与文本渲染正常**（`_强调_` 生效、无串行、无把两段并成一段）。
- 中指长兄盔甲技能无敌期间，割腕 / 蓄血圣杯应照常结算自伤与成长（不再被无敌吞掉）。
- **先卸载再安装**：同名同 versionCode 覆盖安装时，MIUI/EMUI 等会继续显示**缓存的**桌面图标与名称。
'''

LOG_TEXT = '''
## 七、出 v0.3.1（2026-09-20d）

- `build.gradle`：`appVersionCode 930 → 931`、`appVersionName 0.3.0 → 0.3.1`。
- `ui/changelist/EGOPD_Changes.java`：加 `EGOPD v0.3.1` major 条目（排 v0.3.0 之前）+ 3 条按钮
  （Bug 修复 / 中指长兄割腕穿透无敌 / EGO 特化神器），补 3 个 import。`ChangesScene` 未动（页签已存在）。
- 补丁脚本 `_chk/patch_release_031.py`（30 断言 ALL PASS，含改动前双向断言）；`check_utf8_all.py` 1445 文件 OK；
  `EGOPD_Changes.java` 单文件 javac **0 错误 0 告警**。
- 打包：`:android:assembleDebug` **BUILD SUCCESSFUL in 29s**，`compileJava`/`packageDebug` 均 executed。
- 四道核验全绿：① 931 / `0.3.1-INDEV` / label=EGOPD / anydpi-v26 图标；② dex 内三条文案与类名均在；
  ③ 新脚本 `_chk/verify_apk_assets_031.py` —— 533 个 assets md5 逐条一致（cp437 中文名还原）；
  ④ 49,383,020 B，比 0.3.0 大 16,121 B（+0.033%），可解释。
- 归档 `EGOPD_0.3.1.APK`（md5 `dc2f86213da64b97e12933dc9f17045a`）。AGENTS.md §1 版本号现况已同步为 931 / `0.3.1`。
'''

for path, text, tag in ((LOG, LOG_TEXT, '工作日志'),):
    with open(path, 'rb') as f:
        raw = f.read()
    crlf = b'\r\n' in raw
    s = raw.decode('utf-8').replace('\r\n', '\n')
    assert '## 七、出 v0.3.1' not in s, tag + ' 已追加过，跳过（防重复）'
    if s.endswith('\n\n'):
        print('  提示：%s 末尾已有空行，rstrip 后再接' % tag)
    s = s.rstrip('\n') + '\n' + text.replace('\r\n', '\n')
    if not s.endswith('\n'):
        s += '\n'
    out = s.replace('\n', '\r\n') if crlf else s
    with open(path, 'wb') as f:
        f.write(out.encode('utf-8'))
    print('OK 已追加 %s（%s，现 %d 字符）' % (tag, 'CRLF' if crlf else 'LF', len(s)))
