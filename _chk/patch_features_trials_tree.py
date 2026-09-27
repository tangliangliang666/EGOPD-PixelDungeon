# -*- coding: utf-8 -*-
"""把「考验界面改为生命之树排版」同步进 docs/features.md。

字节级读写（docs/features.md 是**纯 LF**），四处改动一次原子落盘，每处都带锚点唯一性断言。
幂等：已应用过就跳过。
"""
import os

PATH = r'D:/PD/docs/features.md'
MARK = 'verify_trials_tree_ui.py'      # 两处新增共用的幂等标记

raw = open(PATH, 'rb').read()
text = raw.decode('utf-8')

if MARK in text:
    print('[SKIP] 已应用过')
    raise SystemExit(0)

OPS = []

# ── ① §2 接线清单：新文件 ────────────────────────────────────────────────
OPS.append((
    '  `windows/WndTrials.java`（照抄 `WndChallenges`，每行多一个 16×16 图标槽，`WIDTH` 120→140）、\n'
    '  `assets/interfaces/tree.png`。\n',
    '  `windows/WndTrials.java`（**2026-09-25 起改为「生命之树」排版**，`WIDTH` 140）、\n'
    '  `windows/WndTrialInfo.java`（质点详情小窗，继承 `WndTitledMessage`）、\n'
    '  `assets/interfaces/tree.png`。\n',
))

# ── ② §4 美术资源：tree.png 尺寸与行语义 ─────────────────────────────────
OPS.append((
    '- `tree.png`：**160×16 ＝ 10 帧 ×(16×16)**，一行十帧、帧序＝`Trials.NAME_IDS`（KETER→MALKUTH）；\n'
    '  **帧内图标的实际像素尺寸各不相同**（其余像素透明），画面起点恒为 `(0,0)`。\n',
    '- `tree.png`：**160×48 ＝ 3 行 × 10 帧（每帧 16×16）**，列序＝`Trials.NAME_IDS`（KETER→MALKUTH）；\n'
    '  **帧内图标的实际像素尺寸各不相同**（其余像素透明），画面起点恒为 `(0,0)`。\n'
    '  三行＝三种状态（2026-09-25 用户口径）：**第 1 行彩色＝已开启**、**第 2 行浅灰＝尚未解锁**、\n'
    '  **第 3 行深灰＝已解锁但未开启**（初始态）；判据收口在 `Trials.iconRow(index, enabled)`，\n'
    '  「是否解锁」另留 `Trials.isUnlocked(index)`（**目前恒为 true**）。\n'
    '  ⚠️ **三行包围盒逐格完全一致**（`_chk/verify_tree_rows_bbox.py` 实测 0/10 不一致）⇒ 尺寸表三行共用，\n'
    '  换状态只改取样矩形的 y 偏移。\n',
))

# ── ③ 新增小节（插在 §5 核验 之前）───────────────────────────────────────
NEW_SECTION = '''### 考验选择界面改为「生命之树」排版（2026-09-25）

- **新布局**：十个质点按卡巴拉生命之树排三列，列间**严格等距蜂窝**（列间距 `h = a·√3/2`，相邻列错开 `a/2`）；
  中列在 `y = 0 / 2a / 3a / 4a`（`a` 处是**空掉的 Da'at 位**，纯空白），左右两列在 `0.5a / 1.5a / 2.5a`；
  **22 条路径**全画（14 条长 `a`、7 条长 `√3·a`、1 条 `KETER—TIPHERETH` 长 `2a`）。
  `a = 70`、图标显示缩放 `0.70` ⇒ 整树 **132.24 × 291**，装进 140 内容宽（左右各留 3.88px）。
- **几何全在 `WndTrials` 的常量区**（`A` / `H` / `ICON_SCALE` / `NODE_DX` / `NODE_DY` / `EDGES`）：
  树是纯静态几何、**不依赖任何手绘像素**，改 `A` 或 `ICON_SCALE` 会自己重排。
  ⚠️ 红线：**别让 `a·√3 + 最大图标显示宽 > WIDTH`**，那会让树溢出窗口（`verify_trials_tree_ui.py` 有这条断言）。
- **图标「只改显示尺寸、不重采样」**（用户 2026-09-25 口径）：取样矩形恒取原生 13~16px
  （`Trials.ICON_W/ICON_H`，`uvRectBySize`），要变小只改 `Image.scale` ⇒ **贴图一个字节都不动**，
  缩放由 NEAREST 完成（13→9 这类**非整数倍**会抽稀掉若干列，是这套口径的已知代价，用户已确认接受）。
  **备选口径**：图标保持原生 16px、`a` 仍取 70 ⇒ 树 137.24 × 296，**同样装得进 140**（左右各留 1.38px），
  代价是图标相对树更大更密；切换只需把 `ICON_SCALE` 改成 `1.0f`。
- **路径层**：整层一次画进运行时 `Pixmap`（`TextureCache.create(KEY, w, h)` 后往 `tx.bitmap` 写像素，
  与 `effects/HeatVignette` 同一套；**不需要手动上传**，首次 `bind()` 才生成）。
  35% 黑、线端按「最大图标半宽 + 2px」退让（草稿期的 `0.28a` 是为「有定位圆」定的，无圆时会空出 14px 的断线感）。
  ⚠️ 用 `Pixmap.Blending.None` **直接写入**而不是 SourceOver —— 否则两条线交叉处会叠两次、比别处更黑。
- **交互**：质点即热点（`Button` 无外观、命中区 20×20；质点间距 ≥60，不会互相压住）⇒ 开 `WndTrialInfo`
  详情窗（左上角原生尺寸图标 + 名称 + 描述 + 开关）；**开关放在详情窗里**，勾选只改本窗口的「待提交」位掩码，
  仍是**关窗时**（`WndTrials.onBackPressed`）一次性写回 `SPDSettings`。
  开关一动、树上那枚图标立刻换行 ⇒ 图标行始终跟着**待提交值**走（`Trials.iconRow` 特意做成纯函数、不读设置）。
- ⚠️ **窗口底色是深棕木纹**（`chrome.png` 的 `WINDOW` 九宫格，内部约 `#4E3A2D`），文本默认色为白、
  标题/名称/高亮走 `Window.TITLE_COLOR`（黄）—— 出预览图时别照抄「浅色窗口」的印象。
- ⚠️ **旧的逐条勾选列表已整体退役**（`CheckBox` 排在每行左侧那套）⇒ §3 与 §4 里
  「行高 14 / 图标在 16×16 槽内居中 / `ICON_SLOT`」这些结论对当前界面**不再适用**。

'''

anchor5 = '## 5. 核验\n'
OPS.append((anchor5, NEW_SECTION + anchor5))

# ── ④ §5 核验清单：新增这一轮脚本 ────────────────────────────────────────
VERIFY_OLD = ('- `_chk/verify_tree_trials_icons.py` —— 图案与常量互查（含 `--selftest`，证明「声明须包住实测」不是恒真断言）；\n'
              '  ① 整图尺寸 ② 逐帧不越格 ③ 声明须包住实测 ④ **已绘制帧声明＝实测（十帧逐一相等）** ⑤ 未绘制帧仅 NOTE。\n')
VERIFY_NEW = (VERIFY_OLD +
              '- `_chk/verify_trials_tree_ui.py` —— **生命之树界面静态回归**（93 条断言 + 8 组反例自测）：\n'
              '  ① 几何（坐标表逐项对规格、路径集合与长度分布 `{a:14, √3a:7, 2a:1}`、无孤点、树宽 ≤ `WIDTH`）；\n'
              '  ② 图标（显示尺寸由 `round(原生×ICON_SCALE)` 现算、**不得出现 `icon.resize` / 任何降采样**、\n'
              '  换行只改取样矩形 y）；③ 行映射（三行常量 0/1/2、`iconRow` 是纯函数、`isUnlocked` 恒真、行为真值表）；\n'
              '  ④ 交互（`SPDSettings.trials(` 全仓**恰好 1 处**且在 `onBackPressed` 的 `editable` 守卫里、\n'
              '  树上不得挂 `CheckBox`、`WndTrialInfo` 的 `add(toggle)` 必须在 `super(...)` 之后）；\n'
              '  ⑤ 文案键 `windows.wndtrials.enable` 双语齐备且与 `Messages.get(this,"enable")` 拼写一致；\n'
              '  ⑥ `tree.png` 三行包围盒逐格一致。\n'
              '  `--selftest` 用 8 组反例（少一条路径 / Binah 放错列 / 改用 resize / iconRow 读设置 /\n'
              '  多一处实时写回 / `add(toggle)` 抢在 super 前 / 键名拼错 / `strip_comments` 被 `//**` 吞）\n'
              '  证明判据不恒真。\n')
OPS.append((VERIFY_OLD, VERIFY_NEW))

for i, (old, new) in enumerate(OPS, 1):
    n = text.count(old)
    assert n == 1, '第 %d 处锚点命中 %d 次（应为 1）' % (i, n)
    text = text.replace(old, new)

new_raw = text.encode('utf-8')
assert new_raw.count(b'\r\n') == raw.count(b'\r\n') == 0, 'docs/features.md 应保持纯 LF'
open(PATH, 'wb').write(new_raw)
print('[OK] %s  行数 %d -> %d（+%d）'
      % (os.path.basename(PATH), raw.count(b'\n'), new_raw.count(b'\n'),
         new_raw.count(b'\n') - raw.count(b'\n')))
