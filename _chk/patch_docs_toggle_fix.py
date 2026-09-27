# -*- coding: utf-8 -*-
"""同步 docs/features.md：① TIPHERETH 图标订正带来的数字；② 开关点不动的红线与修复。

幂等：新串已在就跳过。字节级读写（该文件为纯 LF）。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOC = os.path.join(ROOT, 'docs/features.md')

PAIRS = (
    # ① 逐帧包围盒表：第 5 帧 16×15 → 15×15
    ('  `13×16 / 15×13 / 15×14 / 15×14 / 13×16 / 16×15 / 15×15 / 15×14 / 15×15 / 15×16`。',
     '  `13×16 / 15×13 / 15×14 / 15×14 / 13×16 / 15×15 / 15×15 / 15×14 / 15×15 / 15×16`。\n'
     '  ⚠️ **2026-09-25 订正**：TIPHERETH（第 5 帧）右侧曾残了一道**极薄的不透明像素**，实测包围盒被算成\n'
     '  16×15；用户擦掉后回填为 **15×15**，整树外包宽随之 **104.27 → 103.27**（左右余量 7.86 → 8.36）。'),

    # ② 尺寸表那条补充「源图一改就要重跑」＋顺手修掉的过期判据
    ('  `_chk/verify_tree_trials_icons.py` 双端核对（它解析 `Trials.java` 与 PNG；声明尺寸小于实测 ⇒ FAIL）。',
     '  `_chk/verify_tree_trials_icons.py` 双端核对（它解析 `Trials.java` 与 PNG；声明尺寸小于实测 ⇒ FAIL，\n'
     '  声明大于实测 ⇒ 带透明边被居中推偏）。\n'
     '  ⚠️ **源图一改就要重跑**：2026-09-25 擦掉 TIPHERETH 那道像素后，该脚本当场报「帧 5：声明 16x15 ≠ 实测 15x15」。\n'
     '  该脚本自身还藏过一条**过期判据**（整图高按「单行 16」写死，而 tree.png 2026-09-24 起就是 3 行 48 高）⇒\n'
     '  永久报红 1 条，已修成 `h == ICON_ROWS * ICON_FRAME`。**陈旧判据会把真回归埋掉，见到就修，别习惯性忽略**。'),

    # ③ 三轮尺寸表：本版数字
    ('| **104.27 × 212** | **120 × 236** | **132 × 248** | **7.86** |',
     '| **103.27 × 212** | **120 × 236** | **132 × 248** | **8.36** |'),

    # ④ 宽度上限随最大图标显示宽 22 → 21 变动
    ('22)/√3 = 56.6', '21)/√3 = 57.1'),

    # ⑤ 交互段：详情窗图标口径 + 开关由谁构造
    ('  详情窗（左上角原生尺寸图标 + 名称 + 描述 + 开关）；**开关放在详情窗里**，勾选只改本窗口的「待提交」位掩码，',
     '  详情窗（左上角**与树上同显示尺寸**的图标 + 名称 + 描述 + 开关）；**开关放在详情窗里，而且必须由\n'
     '  `WndTrialInfo` 自己在 `super(...)` 之后构造**（⚠️ 见下一条红线）、调用方只传文案 / 初值 / 回调；\n'
     '  勾选只改本窗口的「待提交」位掩码，'),

    # ⑥ 新增红线（插在「窗口底色」告警之前）
    ('- ⚠️ **窗口底色是深棕木纹**',
     '- ⚠️ **红线：详情窗里的开关不能在调用方 new**（2026-09-25 修，`verify_trials_tree_ui.py` 有断言）。\n'
     '  `Button` 的 `PointerArea` 是在**构造时**就把自己注册进全局 `PointerEvent` 监听表的，而那张表是\n'
     '  `new Signal<>(true)`（**stackMode**：`add()` 走 `addFirst`、`dispatch()` 从队首遍历、**首个返回 true 就\n'
     '  `return`**）⇒ **后注册者优先，且第一个命中的会吞掉其余所有人**。而 `Window` 的构造会加一个**覆盖全屏**\n'
     '  的 blocker（点窗口外即关窗），它照样拦截：`Gizmo.isActive()` 只看 `active && parent.isActive()`，\n'
     '  **不看 `visible`**，而 blocker 正是 `visible = false` 的那个。\n'
     '  原先 `openDetail` 是「先 `new CheckBox(...)` 再 `new WndTrialInfo(...)`」⇒ 开关比该 blocker **早注册**，\n'
     '  每次点击都被 blocker 抢先命中 —— 症状就是**开关点上去毫无反应**（用户 2026-09-25 报）。\n'
     '  现在开关由 `WndTrialInfo` 自己的构造体（`super(...)` 之后）建，参数是\n'
     '  `toggleLabel / checked / toggleActive / ToggleListener`；`WndTrials` 里**连 `CheckBox` 这个词都不许有**。\n'
     '  同族坑见 §6「UI 组件构造即崩」（都是**构造顺序**）。\n'
     '- ⚠️ **窗口底色是深棕木纹**'),

    # ⑦ 核验条目：断言数与反例数
    ('- `_chk/verify_trials_tree_ui.py` —— **生命之树界面静态回归**（**108 条断言** + 10 组反例自测）：',
     '- `_chk/verify_trials_tree_ui.py` —— **生命之树界面静态回归**（**112 条断言** + **12 组**反例自测）：'),

    # ⑧ ④ 交互判据细化
    ('  ④ 交互（`SPDSettings.trials(` 全仓**恰好 1 处**且在 `onBackPressed` 的 `editable` 守卫里、\n'
     '  树上不得挂 `CheckBox`、`WndTrialInfo` 的 `add(toggle)` 必须在 `super(...)` 之后）；',
     '  ④ 交互（`SPDSettings.trials(` 全仓**恰好 1 处**且在 `onBackPressed` 的 `editable` 守卫里、\n'
     '  **`WndTrials` 里连 `CheckBox` 这个词都不许有**（开关只能由详情窗自己造）、\n'
     '  **`WndTrialInfo` 里 `new CheckBox(` 必须出现在 `super(...)` 之后** —— 构造顺序＝PointerArea 注册顺序，\n'
     '  早注册就会被窗口那个全屏 blocker 抢先命中、开关永远点不动；\n'
     '  初值走 `checked(boolean)`（直接写字段不换勾选图标）、可否切换走 `toggleActive`、切换回调走 `onToggle`）；'),

    # ⑨ 反例清单
    ('  `--selftest` 用 10 组反例（少一条路径 / Binah 放错列 / 改用 resize / **窗口宽回到 148** /\n'
     '  **`A` 放大到 60 撑破高度** / iconRow 读设置 / 多一处实时写回 / `add(toggle)` 抢在 super 前 /\n'
     '  键名拼错 / `strip_comments` 被 `//**` 吞）证明判据不恒真。',
     '  `--selftest` 用 **12 组**反例（少一条路径 / Binah 放错列 / 改用 resize / **窗口宽回到 148** /\n'
     '  **`A` 放大到 60 撑破高度** / iconRow 读设置 / 多一处实时写回 / **开关构造抢在 `super` 之前** /\n'
     '  **开关初值直接写字段** / **调用方自己 `new CheckBox`** / 键名拼错 / `strip_comments` 被 `//**` 吞）\n'
     '  证明判据不恒真。'),
)


def main():
    raw = open(DOC, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    for old, new in PAIRS:
        if txt.count(new) >= 1 and txt.count(old) == 0:
            print('  跳过：已是新值')
            continue
        if txt.count(old) != 1:
            print('FAIL：旧串命中 %d 次 ⇒ 回查（首 60 字：%r）' % (txt.count(old), old[:60]))
            return 1
        txt = txt.replace(old, new)
        print('  改 ✓ %s' % old.strip()[:46])

    open(DOC, 'wb').write(txt.encode('utf-8'))
    after = open(DOC, 'rb').read()
    print('落盘：%d 行 → %d 行；CRLF=%d 裸LF=%d'
          % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
             after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
    assert after.count(b'\r\n') == crlf
    return 0


if __name__ == '__main__':
    sys.exit(main())
