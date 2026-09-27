# -*- coding: utf-8 -*-
"""同步 verify_trials_tree_ui.py：把「开关必须由详情窗自己在 super 之后构造」钉进去。

原来有两条判据（树上不许有 CheckBox / add(toggle) 在 super 之后），都**没能拦住本轮的真 bug**：
真 bug 是调用方先把开关 new 好再传进来 —— 那时 `add(toggle)` 确实在 super 之后、树上确实没有
CheckBox，可开关照样点不动（它的 PointerArea 比窗口的全屏 blocker 早注册）。

新判据：
  · WndTrials 里连 `CheckBox` 这个词都不许有（含 import）；
  · WndTrialInfo 里 `new CheckBox(` 必须出现在 `super(` 之后（**构造**顺序 ＝ PointerArea 注册顺序）；
  · 初值走 `checked(boolean)`、可否切换走 `toggleActive`、切换回调走 `onToggle`。
另加 3 条反例（开关构造抢在 super 前 / 初值直接写字段 / 调用方自己 new 开关），证明判据不恒真。

幂等：已改过就跳过。字节级读写。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
VER = os.path.join(ROOT, '_chk/verify_trials_tree_ui.py')

OLD_DOC = '''  ④ 交互：关窗一次性写回（全仓只有一个 SPDSettings.trials( ）、开关只在详情窗里、
     WndTrialInfo 的开关必须摆在 super(...) 之后；'''
NEW_DOC = '''  ④ 交互：关窗一次性写回（全仓只有一个 SPDSettings.trials( ）；开关由**详情窗自己**在
     super(...) 之后**构造** —— ⚠️ 关键判据：全局 PointerArea 表是 stackMode 的 Signal
     （后注册优先、首个 true 吞掉其余），早注册的控件会被窗口那个全屏 blocker 抢先命中，
     症状就是「开关点上去毫无反应」；所以 WndTrials 里连 CheckBox 这个词都不许有；'''

OLD_WB = '''    # 开关只在详情窗里 ⇒ 树上不得出现 CheckBox（先去掉 import 行，否则会被 import 误命中）
    code_noimp = re.sub(r'^import .*$', '', code, flags=re.M)
    main = code_noimp[:code_noimp.find('private void openDetail')] \\
        if 'private void openDetail' in code_noimp else code_noimp
    check('CheckBox' not in main, tag + '树上不得挂开关（CheckBox 只该出现在 openDetail 里）', None)
    check('WndTrialInfo' in code, tag + '质点点击要开 WndTrialInfo 详情窗', None)'''
NEW_WB = '''    # 开关由**详情窗自己构造** ⇒ 本文件里连 CheckBox 这个词都不该有（含 import）。
    # ⚠️ 这不是洁癖：在 openDetail 里先 new 再传进去，会让开关的 PointerArea 比 WndTrialInfo
    #    那个全屏 blocker **早注册**，于是每次点击都被 blocker 抢先命中、开关永远点不动
    #    （用户 2026-09-25 报的「点击后无反应」）。构造顺序的判据见 judge_info。
    check('CheckBox' not in code,
          tag + 'WndTrials 里不得出现 CheckBox（开关必须由详情窗自己造）', None)
    check('WndTrialInfo' in code, tag + '质点点击要开 WndTrialInfo 详情窗', None)
    check('WndTrialInfo.ToggleListener' in code,
          tag + '勾选要经 WndTrialInfo.ToggleListener 回调（不在调用方 new 开关）', None)'''

OLD_INFO = '''def judge_info(info_src, tag=''):
    check('extends WndTitledMessage' in info_src, tag + 'WndTrialInfo 必须继承 WndTitledMessage', None)
    code = strip_comments(info_src)
    sup = code.find('super(')
    addt = code.find('add( toggle )')
    res = code.find('resize( width')
    check(sup >= 0 and addt > sup, tag + '开关必须摆在下 super(...) 之后', None)
    check(res > addt, tag + '撑高窗口的 resize 必须在 add(toggle) 之后', None)
    check('toggle != null' in code, tag + '要允许不传开关（纯展示）', None)
    return True'''
NEW_INFO = '''def judge_info(info_src, tag=''):
    check('extends WndTitledMessage' in info_src, tag + 'WndTrialInfo 必须继承 WndTitledMessage', None)
    code = strip_comments(info_src)

    # ⚠️ 本文件最要紧的一条：开关必须在 super(...) **之后构造**。
    # Button 的 PointerArea 在构造时就注册进全局 PointerEvent 监听表，而那张表是 stackMode 的
    # Signal（add 走 addFirst、dispatch 从队首遍历、首个返回 true 即 return）；WndTitledMessage →
    # Window 的 super(...) 会加一个**覆盖全屏**的 blocker（点窗外即关窗），且它照样拦截
    # （Gizmo.isActive() 只看 active && parent.isActive()，不看 visible）。开关若早于它注册，
    # 每一次点击都被它抢先吃掉 —— 症状正是「点上去毫无反应」。
    sup = code.find('super(')
    newcb = code.find('new CheckBox(')
    check(sup >= 0 and newcb >= 0 and newcb > sup,
          tag + '开关必须在 super(...) 之后**构造**（早于窗口 blocker 注册就永远点不动）',
          (sup, newcb))

    addt = code.find('add( toggle )')
    res = code.find('resize( width')
    check(addt > newcb, tag + '要先构造出开关再 add(toggle)', None)
    check(res > addt, tag + '撑高窗口的 resize 必须在 add(toggle) 之后', None)

    # 初值、可否切换、回调都得有出口
    check('toggle.checked(' in code,
          tag + '开关初值要走 CheckBox.checked(boolean)（直接写字段不会换勾选图标）', None)
    check('toggleActive' in code, tag + '要能把开关置为不可切换（局内查看用）', None)
    check('ToggleListener' in code and 'onToggle' in code,
          tag + '切换要回调 ToggleListener.onToggle', None)
    return True'''

OLD_ST = '''    # ⑥ 详情窗：开关摆在 super 之前
    bad = info.replace('super( icon, title, message );',
                       'add( toggle );\\n\\t\\tsuper( icon, title, message );')
    expect_fail(judge_info, bad, 'x', label='详情窗：add(toggle) 抢在 super 之前')'''
NEW_ST = '''    # ⑥ 详情窗：开关**构造**抢在 super 之前（本轮修掉的那个真 bug 的失败形态）
    bad = info.replace('\\t\\tsuper( icon, title, message );',
                       '\\t\\tnew CheckBox( toggleLabel );\\n\\t\\tsuper( icon, title, message );')
    expect_fail(judge_info, bad, 'x', label='详情窗：开关构造抢在 super 之前')
    # ⑥b 详情窗：初值直接写字段（勾选图标不会换）
    bad = info.replace('toggle.checked( checked );', 'toggle.checked = checked;')
    expect_fail(judge_info, bad, 'x', label='详情窗：初值不走 checked(boolean)')
    # ⑥c 调用方自己 new 开关（又被窗口的全屏 blocker 吃掉）
    bad = wnd.replace('\\t\\tImage icon = new Image( Assets.Interfaces.TRIALS );',
                      '\\t\\tnew CheckBox( "x" );\\n'
                      '\\t\\tImage icon = new Image( Assets.Interfaces.TRIALS );')
    expect_fail(judge_writeback, bad, 'x', label='调用方自己 new 开关（会被 blocker 吃掉）')'''

PAIRS = ((OLD_DOC, NEW_DOC, '文档头'),
         (OLD_WB, NEW_WB, 'judge_writeback'),
         (OLD_INFO, NEW_INFO, 'judge_info'),
         (OLD_ST, NEW_ST, 'selftest ⑥'))


def main():
    raw = open(VER, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    for old, new, label in PAIRS:
        if txt.count(new) == 1 and txt.count(old) == 0:
            print('  跳过 %s：已是新值' % label)
            continue
        if txt.count(old) != 1:
            print('FAIL %s：旧串命中 %d 次（应为 1）⇒ 回查' % (label, txt.count(old)))
            return 1
        txt = txt.replace(old, new)
        print('  改 %s ✓' % label)

    # 落地断言：旧的弱判据不该再以原样出现
    for bad in ('addt > sup', 'toggle != null', 'code_noimp'):
        if bad in txt:
            print('FAIL：残留旧判据 %r ⇒ 回查' % bad)
            return 1

    out = txt.encode('utf-8')
    open(VER, 'wb').write(out)
    after = open(VER, 'rb').read()
    print('落盘：%d 行 → %d 行；CRLF=%d 裸LF=%d'
          % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
             after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
    assert after.count(b'\r\n') == crlf, '行尾被改动了'
    return 0


if __name__ == '__main__':
    sys.exit(main())
