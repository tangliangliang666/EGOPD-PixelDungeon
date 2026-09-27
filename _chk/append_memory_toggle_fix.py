# -*- coding: utf-8 -*-
"""追加今日工作日志（幂等：已含标记就跳过）。"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOG = os.path.join(ROOT, '.workbuddy/memory/2026-09-25.md')
MARK = '考验详情窗「开启该考验」点不动'

BLOCK = '''
---

## 20:05 考验详情窗「开启该考验」点不动 —— 根因是**构造顺序**

**用户报**：开关点击后无反应。

**根因（读源码查实，不是猜）**：`Button` 的 `PointerArea` 在**构造时**（`Component()` → `createChildren()`）
就注册进全局 `PointerEvent` 监听表；那张表是 `new Signal<>(true)`（**stackMode**：`add()` 走 `addFirst`、
`dispatch()` 从队首遍历、**首个返回 `true` 就 return**）⇒ **后注册者优先、第一个命中的吞掉其余**。
`Window` 构造会加一个**覆盖全屏**的 blocker（点窗外即关窗），且它照样拦截
（`Gizmo.isActive()` 只看 `active && parent.isActive()`、**不看 `visible`**，blocker 正是 `visible=false`）。
原实装是「先 `new CheckBox(...)` 再 `new WndTrialInfo(...)`」⇒ 开关比该 blocker **早注册**，每次点击都被
blocker 抢先吃掉；树上质点能点，恰恰因为它在 `super()` 之后构造。

**修法**：开关改由 `WndTrialInfo` **自己**在 `super(...)` 之后构造，调用方只传
`toggleLabel / checked / toggleActive / ToggleListener`；`WndTrials` 里连 `CheckBox` 这个词都不许有。

**另一件**：`tree.png` 的 TIPHERETH 帧右侧那道极薄不透明像素已被用户擦除 ⇒ `Trials.ICON_W[5]` 16 → 15
（实测三行均 15×15），最大图标显示宽 22 → 21、树外包宽 104.27 → 103.27、左右余量 7.86 → 8.36。
顺带修了 `verify_tree_trials_icons.py` 一条**过期判据**（整图高按「单行 16」写死，而 tree.png 早已 3 行 48 高）
⇒ 改成 `ICON_ROWS * ICON_FRAME`。**陈旧判据会永久报红、把真回归埋掉，见到就修。**

**核验（全绿）**：javac EXIT=0（仅 2 条既有 `this-escape`）、`check_utf8_all.py` 1462 文件、
unused-imports ALL PASS、`verify_trials_tree_ui.py` **112 断言 PASS** + `--selftest` **12/12**
（新增 3 条反例正对这个 bug：构造抢在 super 前 / 初值直接写字段 / 调用方自己 new）、
`verify_tree_trials_icons.py` ALL PASS、`verify_tree_rows_bbox.py` 三行不一致 0/10。

**同步**：`docs/features.md`（数字 + 新红线 + 核验节）、`AGENTS.md` §6 精简表、
`docs/handbook/pitfalls.md` §6 全量表（新陷阱「窗口里的按钮/开关点上去毫无反应」）、
skill `egopd-trials-tree-ui`（新增「🔴 头号坑」整节 + 规格数字）、MEMORY.md 考验索引条
（订正 `W−21` / 103.27 / 112 断言 / 开关归属）。

**未做**：未升版本、未打包（用户未要求）。
'''


def main():
    txt = open(LOG, encoding='utf-8').read()
    if MARK in txt:
        print('已写过，跳过')
        return 0
    if not txt.endswith('\n'):
        txt += '\n'
    txt += BLOCK
    open(LOG, 'w', encoding='utf-8', newline='').write(txt)
    print('已追加：%d → %d 行' % (txt.count('\n') - BLOCK.count('\n'), txt.count('\n')))
    return 0


if __name__ == '__main__':
    sys.exit(main())
