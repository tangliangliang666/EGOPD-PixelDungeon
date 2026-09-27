# -*- coding: utf-8 -*-
"""修复「开启考验」开关点击无反应 —— WndTrials.java 一侧。

根因（已查实，见 WndTrialInfo 类注释）：
  Button 的 PointerArea 在**构造时**注册进全局 PointerEvent 监听表，而那张表是
  `new Signal<>(true)`（stackMode）：add() 走 addFirst、dispatch() 从队首遍历、
  **遇到第一个返回 true 的就 return**。Window 构造时会加一个**覆盖全屏**的 blocker
  （点窗外即关窗，且 Gizmo.isActive() 不看 visible，所以它照样拦截）。
  原先 openDetail 里是「先 new CheckBox(...) 再 new WndTrialInfo(...)」⇒ 开关比
  自己窗口的 blocker 早注册 ⇒ 每次点击都被 blocker 抢先命中吞掉。

修法：开关改由 WndTrialInfo 自己在 super(...) 之后构造，这里只传文案 / 初值 / 回调。

本补丁同时把两个因 TIPHERETH 图标改 15x15 而变动的数字同步掉（树外包 104.27→103.27、
宽度上限 56.6→57.1）。幂等：已改过就跳过。字节级读写，保住纯 LF。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WND = os.path.join(ROOT, 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/windows/WndTrials.java')

# ── 1. 不再需要 CheckBox 的 import（开关由详情窗自己 new）─────────────────────
DROP_IMPORT = 'import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;\n'

# ── 2. 类注释：树外包宽随 TIPHERETH 变窄 ────────────────────────────────────
OLD_J1 = ' * = **104.3 × 212**（a = 47.5、图标 1.40 缩放时），装进 120 的内容宽（左右各留约 7.9px）。'
NEW_J1 = ' * = **103.3 × 212**（a = 47.5、图标 1.40 缩放时），装进 120 的内容宽（左右各留约 8.4px）。'

# ── 3. 容量判据里的宽度上限（最大图标显示宽 22 → 21）────────────────────────
OLD_J2 = '//  宽度：a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ 120 宽下 a ≤ 56.6（不是瓶颈）'
NEW_J2 = '//  宽度：a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ 120 宽下 a ≤ 57.1（不是瓶颈）'

# ── 4. 类注释「交互」段：写清开关为什么不能在这里 new ──────────────────────
OLD_J3 = (' * 质点本身就是热点：点开 {@link WndTrialInfo} 详情窗（左上角图标 + 名称 + 描述 + 开关），\n'
          ' * **开关放在详情窗里**。勾选只改本窗口的「待提交」位掩码，仍然是在关窗时\n'
          ' * （{@link #onBackPressed()}）一次性写回 SPDSettings —— 别改成实时写回（那样每点一下都会落盘）。')
NEW_J3 = (' * 质点本身就是热点：点开 {@link WndTrialInfo} 详情窗（左上角图标 + 名称 + 描述 + 开关）。\n'
          ' * **开关放在详情窗里，而且必须由那个窗口自己构造** —— ⚠️ 不能在这里 new 好再传进去：\n'
          ' * 全局 PointerArea 监听表是 stackMode 的 Signal（后注册优先、首个 true 吞掉其余），而窗口\n'
          ' * 构造时会加一个覆盖全屏的 blocker，早注册的控件会被它抢先命中、永远点不动。见 WndTrialInfo 类注释。\n'
          ' * 勾选只改本窗口的「待提交」位掩码，仍然是在关窗时（{@link #onBackPressed()}）一次性写回\n'
          ' * SPDSettings —— 别改成实时写回（那样每点一下都会落盘）。')

# ── 5. openDetail：把「先 new 开关再传进去」改成「只传文案/初值/回调」───────
OLD_OD = '''\t\tCheckBox toggle = new CheckBox( Messages.get( this, "enable" ) ) {
\t\t\t@Override
\t\t\tprotected void onClick() {
\t\t\t\tsuper.onClick();
\t\t\t\tif (checked()) {
\t\t\t\t\tmask |= Trials.MASKS[index];
\t\t\t\t} else {
\t\t\t\t\tmask &= ~Trials.MASKS[index];
\t\t\t\t}
\t\t\t\t//树上的那枚图标立刻跟着换行（已开启 = 彩色行）
\t\t\t\tupdateIcon( icons[index], index );
\t\t\t}
\t\t};
\t\ttoggle.checked( (mask & Trials.MASKS[index]) != 0 );
\t\ttoggle.active = editable && Trials.isUnlocked( index );

\t\tShatteredPixelDungeon.scene().add( new WndTrialInfo(
\t\t\t\ticon,
\t\t\t\tMessages.get( Trials.class, Trials.NAME_IDS[index] ),
\t\t\t\tMessages.get( Trials.class, Trials.NAME_IDS[index] + "_desc" ),
\t\t\t\ttoggle ) );'''

NEW_OD = '''\t\t//⚠️ 开关**必须由 WndTrialInfo 自己在 super(...) 之后构造**，这里只能给文案 / 初值 / 回调。
\t\t//  Button 的 PointerArea 是在**构造时**注册进全局监听表的，而那张表是 stackMode 的 Signal
\t\t//  （后注册者优先、首个返回 true 的吞掉其余）；WndTrialInfo 构造时会加一个**覆盖全屏**的
\t\t//  blocker（点窗口外即关窗）。若在这里先 new 好开关再传进去，它就比那个 blocker 早注册 ⇒
\t\t//  每次点击都被 blocker 抢先吃掉，表现为「点上去毫无反应」。详见 WndTrialInfo 类注释。
\t\tShatteredPixelDungeon.scene().add( new WndTrialInfo(
\t\t\t\ticon,
\t\t\t\tMessages.get( Trials.class, Trials.NAME_IDS[index] ),
\t\t\t\tMessages.get( Trials.class, Trials.NAME_IDS[index] + "_desc" ),
\t\t\t\tMessages.get( this, "enable" ),
\t\t\t\t(mask & Trials.MASKS[index]) != 0,
\t\t\t\teditable && Trials.isUnlocked( index ),
\t\t\t\tnew WndTrialInfo.ToggleListener() {
\t\t\t\t\t@Override
\t\t\t\t\tpublic void onToggle( boolean checked ) {
\t\t\t\t\t\tif (checked) {
\t\t\t\t\t\t\tmask |= Trials.MASKS[index];
\t\t\t\t\t\t} else {
\t\t\t\t\t\t\tmask &= ~Trials.MASKS[index];
\t\t\t\t\t\t}
\t\t\t\t\t\t//树上的那枚图标立刻跟着换行（已开启 = 彩色行）
\t\t\t\t\t\tupdateIcon( icons[index], index );
\t\t\t\t\t}
\t\t\t\t} ) );'''


def main():
    raw = open(WND, 'rb').read()
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    txt = raw.decode('utf-8')

    # 幂等判据：新串已在 ⇒ 跳过；否则旧串必须恰好命中 1 次
    for old, new, label in ((OLD_J1, NEW_J1, '类注释·树外包'),
                            (OLD_J2, NEW_J2, '容量判据·宽度上限'),
                            (OLD_J3, NEW_J3, '类注释·交互段'),
                            (OLD_OD, NEW_OD, 'openDetail 开关回调')):
        if txt.count(new) == 1 and txt.count(old) == 0:
            print('  跳过 %s：已是新值' % label)
            continue
        if txt.count(old) != 1:
            print('FAIL %s：旧串命中 %d 次（应为 1）⇒ 回查源文件' % (label, txt.count(old)))
            return 1
        txt = txt.replace(old, new)
        print('  改 %s ✓' % label)

    if txt.count(DROP_IMPORT) == 1:
        txt = txt.replace(DROP_IMPORT, '')
        print('  删 CheckBox import ✓')
    elif 'CheckBox' in txt:
        print('FAIL：CheckBox 仍在文中但 import 未找到 ⇒ 回查')
        return 1
    else:
        print('  跳过 import：已删')

    # 落地前的「比位置」断言：改完不该再出现开关的旧写法
    code = txt
    for bad, why in (('new CheckBox', '开关必须由 WndTrialInfo 建'),
                     ('toggle.checked(', '初值不再在这里设'),
                     ('toggle.active', 'active 不再在这里设')):
        if bad in code:
            print('FAIL：残留 %r（%s）' % (bad, why))
            return 1

    out = txt.encode('utf-8')
    open(WND, 'wb').write(out)
    after = open(WND, 'rb').read()
    print('落盘：%d 行 → %d 行；CRLF=%d 裸LF=%d'
          % (lf - crlf, after.count(b'\n') - after.count(b'\r\n'),
             after.count(b'\r\n'), after.count(b'\n') - after.count(b'\r\n')))
    assert after.count(b'\r\n') == crlf, '行尾被改动了'
    return 0


if __name__ == '__main__':
    sys.exit(main())
