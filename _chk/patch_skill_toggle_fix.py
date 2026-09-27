# -*- coding: utf-8 -*-
"""同步 skill `egopd-trials-tree-ui`：
  · TIPHERETH 图标订正带来的数字（104.27→103.27、56.6→57.1、ICON_W[5] 16→15）；
  · 新增「🔴 头号坑：窗口里的按钮/开关点上去毫无反应」整节（构造顺序 / PointerArea / blocker）；
  · WndTrialInfo 的 API 与调用方分工改口径；
  · 断言数 108→112、反例 10→12；补 verify_tree_trials_icons.py 的表格行。

幂等：新串已在就跳过。字节级读写（该文件为纯 LF）。
"""
import os
import sys

SC = r'C:/Users/14675/.workbuddy/skills/egopd-trials-tree-ui/SKILL.md'

PAIRS = (
    # ① 树外包宽
    ('| 树外包 | **104.27 × 212.00** | `宽 = 2h + 最大图标显示宽`、`高 = 4A + 最大图标显示高` |',
     '| 树外包 | **103.27 × 212.00** | `宽 = 2h + 最大图标显示宽`、`高 = 4A + 最大图标显示高`；'
     '2026-09-25 TIPHERETH 订正后由 104.27 收到 103.27 |'),

    # ② 宽度上限
    ('① **横向** `A·√3 + 最大图标显示宽 ≤ WIDTH` ⇒ 120 宽下 `A ≤ 56.6`（**不是**瓶颈）；',
     '① **横向** `A·√3 + 最大图标显示宽 ≤ WIDTH` ⇒ 120 宽下 `A ≤ 57.1`（**不是**瓶颈）；'),

    # ③ ICON_W 表 + TIPHERETH 订正说明
    ('⚠️ **`Trials.ICON_W/ICON_H` 只描述 row 0 的包围盒**（`{13,15,15,15,13,16,15,15,15,15}` ×\n'
     '`{16,13,14,14,16,15,15,14,15,16}`），但**三行共用**：切行只改取样矩形的 `y = row*ICON_FRAME`，\n'
     '宽高与原点都不动。',
     '⚠️ **`Trials.ICON_W/ICON_H` 只描述 row 0 的包围盒**（`{13,15,15,15,13,15,15,15,15,15}` ×\n'
     '`{16,13,14,14,16,15,15,14,15,16}`），但**三行共用**：切行只改取样矩形的 `y = row*ICON_FRAME`，\n'
     '宽高与原点都不动。\n'
     '\n'
     '⚠️ **2026-09-25 订正**：TIPHERETH（第 5 帧）右侧曾残了一道**极薄的不透明像素** ⇒ 实测包围盒被算成\n'
     '16×15；用户擦掉后回填 **15×15**（`ICON_W[5]` 16 → 15），**最大图标显示宽 22 → 21**，整树外包宽\n'
     '**104.27 → 103.27**（左右余量 7.86 → 8.36）。**源图一改必须重跑** `_chk/verify_tree_trials_icons.py`\n'
     '（声明≠实测立刻报红）与 `_chk/analyze_tree_png.py` 回填 —— 这两个数**就是**取样矩形，\n'
     '写大了会带透明边被居中推偏、写小了直接裁。'),

    # ④ 文件表：开关由窗口自建
    ('| `windows/WndTrialInfo.java` | 详情小窗，**继承 `WndTitledMessage`**，在描述下方追加传入的 `CheckBox` |',
     '| `windows/WndTrialInfo.java` | 详情小窗，**继承 `WndTitledMessage`**，在描述下方**自建**开关'
     '（`super(...)` 之后 new，⚠️ 见「头号坑」）|'),

    # ⑤ 交互：开关由谁构造
    ('- 开关**放在详情窗里**（用户明确），由 `WndTrials` 构造并传入；勾选只改本窗口的 `mask`，\n'
     '  同时立刻 `updateIcon(icons[idx], idx)` 换树上那枚的行。',
     '- 开关**放在详情窗里**（用户明确），但**由 `WndTrialInfo` 自己构造**（⚠️ 别在 `WndTrials` 里 new 好\n'
     '  再传进去 —— 见下面「🔴 头号坑」）；勾选只改本窗口的 `mask`，同时立刻\n'
     '  `updateIcon(icons[idx], idx)` 换树上那枚的行（这条回调留在 `WndTrials`，即 `ToggleListener`）。'),

    ('- `CheckBox.active = editable && Trials.isUnlocked(index)`。',
     '- 可否切换由调用方算好传进来：`toggleActive = editable && Trials.isUnlocked(index)`\n'
     '  （局内查看为 false ⇒ 开关只是个指示器，见「头号坑」末条）。'),

    # ⑥ 新增「头号坑」整节（插在「配色」小节之前）
    ('**配色（出预览图前必看）**：本作 `chrome.png` 的 `WINDOW` 九宫格是**深棕木纹**',
     '### 🔴 头号坑：窗口里的按钮 / 开关「点上去毫无反应」（2026-09-25 实修）\n'
     '\n'
     '首版实装后用户报「开启该考验的按钮点击后无反应」。根因**不在开关本身**，在**构造顺序**：\n'
     '\n'
     '- `Button` 的 `PointerArea` 是在**构造时**（`Component()` → `createChildren()`）就注册进全局\n'
     '  `PointerEvent` 监听表的；\n'
     '- 那张表是 `new Signal<>(true)` ⇒ **stackMode**：`add()` 走 `addFirst`、`dispatch()` 从队首逐个问、\n'
     '  **遇到第一个返回 `true` 的就 `return`** ⇒ **后注册者优先，且第一个命中的会吞掉后面所有人**；\n'
     '- `Window` 的构造会加一个 `PointerArea(0, 0, uiCamera.width, uiCamera.height)` 的 **blocker**\n'
     '  （「点窗口外即关窗」靠它），而它**照样拦截**：`Gizmo.isActive()` 只看 `active && parent.isActive()`、\n'
     '  **不看 `visible`**，而 blocker 恰恰是 `visible = false` 的那个 ⇒ 在 `DOWN` 上直接返回 `true`。\n'
     '\n'
     '⇒ 任何**早于**该 blocker **构造**的控件，每一次点击都被自己的窗口抢先吃掉。\n'
     '「打开详情窗的那个质点」能点，正是因为它本来就在 `super()` 之后构造。\n'
     '\n'
     '**错误写法**（初版）：调用方先 `new CheckBox(...)`，再 `new WndTrialInfo(..., toggle)`。\n'
     '**正确写法**：控件由窗口自己造，调用方只传文案 / 初值 / 状态 / 回调 —— `WndTrialInfo` 在\n'
     '`super(...)` 之后 new 开关：\n'
     '\n'
     '```java\n'
     '// WndTrialInfo 构造体内（super 之后）\n'
     'CheckBox toggle = new CheckBox( toggleLabel ) {\n'
     '    @Override protected void onClick() {\n'
     '        super.onClick();                    // CheckBox.onClick() 里完成 checked(!checked)\n'
     '        if (listener != null) listener.onToggle( checked() );\n'
     '    }\n'
     '};\n'
     'toggle.checked( checked );   // ⚠️ 初值必须走 checked(boolean)：直接写字段不换勾选图标\n'
     'toggle.active = toggleActive;\n'
     'toggle.setRect( 0, height + 2*TOGGLE_GAP, width, TOGGLE_HEIGHT );\n'
     'add( toggle );\n'
     'resize( width, (int)toggle.bottom() );\n'
     '```\n'
     '\n'
     '- `Button.active = false` 会经父链（`Gizmo.isActive()`）**把点击整个关掉** ⇒ 局内\n'
     '  （`editable=false` 的三个调用点）开关点不动是**刻意**的，不是同一个 bug。\n'
     '- 万不得已可用 `button.givePointerPriority()`（内部 `removePointerListener` + `addPointerListener`\n'
     '  把它提到队首），但那是补丁；首选仍是**构造顺序**。\n'
     '- 回归钉两条：`WndTrials` 里**连 `CheckBox` 这个词都不许有**（含 import）；`WndTrialInfo` 的\n'
     '  `new CheckBox(` 必须出现在 `super(` **之后**。三条反例（构造抢在 super 前 / 初值直接写字段 /\n'
     '  调用方自己 new）都对着这个 bug。\n'
     '- 同族坑见 `AGENTS.md` §6 与 `docs/handbook/pitfalls.md` 的「UI 组件构造即崩」—— **都是构造顺序**。\n'
     '\n'
     '**配色（出预览图前必看）**：本作 `chrome.png` 的 `WINDOW` 九宫格是**深棕木纹**'),

    # ⑦ 脚本表：断言数 + 新增一行
    ('| `_chk/verify_trials_tree_ui.py` | **界面静态回归**（**108 条断言** + 10 组反例）',
     '| `_chk/verify_trials_tree_ui.py` | **界面静态回归**（**112 条断言** + **12 组**反例）'),

    ('| `_chk/verify_tree_rows_bbox.py` | 逐格比对 tree.png 三行包围盒是否一致 |',
     '| `_chk/verify_tree_trials_icons.py` | `Trials.ICON_W/H` ↔ tree.png 逐帧实测包围盒互查'
     '（声明≠实测即红，含 `--selftest`）；⚠️ 它自己曾把整图高按「单行 16」写死（tree.png 早就是'
     '3 行 48 高）⇒ 已修成 `ICON_ROWS * ICON_FRAME`。陈旧判据会永久报红、把真回归埋掉，见到就修 |\n'
     '| `_chk/verify_tree_rows_bbox.py` | 逐格比对 tree.png 三行包围盒是否一致 |'),
)


def main():
    raw = open(SC, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    for old, new in PAIRS:
        if txt.count(new) >= 1 and txt.count(old) == 0:
            print('  跳过：已是新值')
            continue
        if txt.count(old) != 1:
            print('FAIL：旧串命中 %d 次 ⇒ 回查（首 50 字：%r）' % (txt.count(old), old[:50]))
            return 1
        txt = txt.replace(old, new)
        print('  改 ✓ %s' % old.strip()[:46])

    # ⚠️ 别把 104.27 列为禁词：新文案里保留了「104.27 → 103.27」这段历史沿革
    for bad in ('56.6', '13,16,15', '108 条断言', '13, 16, 15, 15, 15, 15'):
        if bad in txt:
            print('FAIL：残留过期内容 %r' % bad)
            return 1

    open(SC, 'wb').write(txt.encode('utf-8'))
    after = open(SC, 'rb').read()
    print('落盘：%d 行 → %d 行；CRLF=%d 裸LF=%d'
          % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
             after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
    assert after.count(b'\r\n') == crlf
    return 0


if __name__ == '__main__':
    sys.exit(main())
