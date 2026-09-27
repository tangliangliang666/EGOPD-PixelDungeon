# -*- coding: utf-8 -*-
"""把「窗口内控件必须晚于 blocker 构造」这条静默陷阱写进两张表：

  · AGENTS.md §6「高频静默陷阱（精简表）」—— 加一行（症状 / 要害）；
  · docs/handbook/pitfalls.md §6（全量表）—— 加一行完整因果与兜底手段。

插入位置用「行前缀」定位（AGENTS.md 插在 §6 表末行之后、`## 7` 之前；
pitfalls.md 插在最后一行表格行之后），幂等：已含关键词就跳过。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
AGENTS = os.path.join(ROOT, 'AGENTS.md')
PIT = os.path.join(ROOT, 'docs/handbook/pitfalls.md')

MARK = '窗口里的按钮 / 开关点上去毫无反应'

ROW_SHORT = (
    '| **窗口里的按钮 / 开关点上去毫无反应**（本轮：考验详情窗的「开启该考验」） '
    '| `Button` 的 `PointerArea` 在**构造时**就注册进全局 `PointerEvent` 监听表，而那张表是 '
    '`new Signal<>(true)`（**stackMode**：`add()` 走 `addFirst`、`dispatch()` 从队首遍历、'
    '**首个返回 true 即 return**）⇒ **后注册者优先、第一个命中的吞掉其余**；`Window` 构造会加一个'
    '**覆盖全屏**的 blocker（点窗外即关窗），且它照样拦截（`Gizmo.isActive()` 只看 '
    '`active && parent.isActive()`，**不看 visible**，而 blocker 正是 `visible=false`）。'
    '⇒ 任何**早于**该 blocker **构造**的控件都被自己窗口吃掉 '
    '| **窗口内的控件一律在子类构造体、`super(...)` 之后 new**（SPD 本来就这么写）；'
    '别为了「把逻辑留在调用方」先在调用方 new 好再传进来。万不得已可用 '
    '`button.givePointerPriority()` 把它的 PointerArea 提到队首。同族坑见「UI 组件构造即崩」'
    '（都是**构造顺序**）；回归 `verify_trials_tree_ui.py` |'
)

ROW_FULL = (
    '| **窗口里的按钮 / 开关点上去毫无反应**（2026-09-25：考验选择界面的「开启该考验」开关；'
    '树上质点却能点） | ① `Button` 的 `PointerArea` 是在**构造时**（`Component()` → `createChildren()`）'
    '就把自己注册进全局 `PointerEvent` 的监听表；② 那张表是 `new Signal<>(true)`，即 '
    '**stackMode** —— `add()` 走 `addFirst`、`dispatch()` 从队首逐个问、**遇到第一个返回 `true` 的就 '
    '`return`** ⇒ 「**后注册者优先，且第一个命中的会吞掉后面所有人**」；③ `Window` 的构造会加一个 '
    '`PointerArea(0, 0, uiCamera.width, uiCamera.height)` 的 blocker（靠它实现「点窗口外即关窗」），'
    '它**照样拦截**：`PointerArea.onSignal` 里 `Gizmo.isActive()` 只看 `active && parent.isActive()`，'
    '**不看 `visible`**，而 blocker 恰恰是 `visible = false` 的那个，于是在 `DOWN` 上直接返回 `true`。'
    '⇒ 任何**早于**该 blocker 构造的控件，每一次点击都被自己的窗口抢先命中并吞掉；'
    '而「打开详情窗的那个按钮」能点，是因为它本来就在 `super()` 之后才构造 '
    '| **窗口内的控件一律在子类构造体里 new，且必须在 `super(...)` 之后** —— 这就是 SPD 所有 '
    '`Wnd*` 的写法，不是风格问题。别为了「把回调逻辑留在调用方」而在调用方先 new 好、再作为参数传进窗口'
    '（本次就是这么踩的：`WndTrials.openDetail` 先 `new CheckBox(...)` 再 `new WndTrialInfo(...)`，'
    '开关的 PointerArea 因此比详情窗的 blocker 早注册）。修法：控件由窗口自己造，调用方只传'
    '**文案 / 初值 / 状态 / 回调接口**（`WndTrialInfo.ToggleListener`）。'
    '真要兜底可用 `Button.givePointerPriority()`（内部 `PointerArea.givePointerPriority()` ＝ '
    '`removePointerListener` + `addPointerListener`，把它提到队首），但那只是补丁，首选仍是构造顺序。'
    '⚠️ 另外别忘了 `Button.active = false` 会经父链（`Gizmo.isActive()`）把点击整个关掉 —— '
    '局内只读的窗口里开关点不动是**刻意**的。回归：`_chk/verify_trials_tree_ui.py` '
    '（断言 `WndTrials` 不含 `CheckBox`、`WndTrialInfo` 的 `new CheckBox(` 在 `super(` 之后）|'
)


def insert_after_prefix(lines, prefix, row):
    """在「以 prefix 开头」的那一行之后插入 row；已有 MARK 则跳过。"""
    for i, l in enumerate(lines):
        if MARK in l:
            return lines, False
    for i, l in enumerate(lines):
        if l.startswith(prefix):
            return lines[:i + 1] + [row] + lines[i + 1:], True
    raise AssertionError('找不到锚点行：' + prefix[:40])


def main():
    ok = 0
    for path, prefix, row in (
            (AGENTS, '| **专属武器「锁在手上」照样能被卖掉**', ROW_SHORT),
            (PIT, '| 角色专属武器**锁死在手上照样能在商店卖掉**', ROW_FULL)):
        raw = open(path, 'rb').read()
        crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
        lines = raw.decode('utf-8').split('\n')
        lines, changed = insert_after_prefix(lines, prefix, row)
        if not changed:
            print('  跳过 %s：已写入' % os.path.basename(path))
        else:
            print('  写 %s ✓' % os.path.basename(path))
        out = '\n'.join(lines).encode('utf-8')
        open(path, 'wb').write(out)
        after = open(path, 'rb').read()
        print('     %d 行 → %d 行；CRLF=%d 裸LF=%d'
              % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
                 after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
        assert after.count(b'\r\n') == crlf
        ok += 1
    assert ok == 2
    return 0


if __name__ == '__main__':
    sys.exit(main())
