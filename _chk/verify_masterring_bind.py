"""大师指环「绑定后无法重新绑定」修复的跨文件不变量核验（2026-09-18）。

病因：`MasterRing.defaultAction()` 曾在「已绑定」时返回 AC_QUICK（点击指环直接执行绑定的技艺），
于是绑定之后点击指环只执行技艺、再也打不开技艺窗口——而绑定/取消绑定只能在窗口里长按技艺完成
⇒ 玩家无法重新绑定。修法：默认动作恒定 AC_ARTS（永远开窗），执行动作独立成屏幕右侧副槽按钮
`MasterRing.QuickArtAction`（仿 `SealedSwordBase.SwordSwap`）。

本脚本把这条不变量钉住：三处（默认动作 / 窗口长按 / 副槽按钮）必须同时成立，
任何一处被改回去都判 FAIL——尤其是「默认动作别再被 AC_QUICK 劫持」，那是 bug 本身。

用法：python _chk/verify_masterring_bind.py
"""

import io
import re
import sys

ROOT = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
RING = ROOT + "items/MasterRing.java"
WND = ROOT + "windows/WndRingMasterArts.java"

fails = []


def fail(msg):
    fails.append(msg)


def read(path):
    with io.open(path, encoding="utf-8") as f:
        return f.read()


def body_of(text, signature):
    """取出某个方法的 `{...}` 方法体（按大括号配平，够用即可）。"""
    idx = text.find(signature)
    if idx < 0:
        return None
    start = text.find("{", idx)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return None


def strip_comments(text):
    """剥掉块注释与行注释，只留下代码（含字符串字面量）。

    话术类判据必须只看「玩家看得见的字符串」，不能连带把「这里曾经怎么写」的历史注释也判失败
    （本文件的历史说明里保留了旧话术原文）。这两个文件的字符串里没有 `//`，所以直接正则即可。
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def main():
    ring = read(RING)
    wnd = read(WND)

    # ---------- ① 默认动作：恒定 AC_ARTS，绝不能含 AC_QUICK（bug 本体） ----------
    da = body_of(ring, "public String defaultAction()")
    if da is None:
        fail("MasterRing 找不到 defaultAction() 覆写（点击指环必须恒定打开技艺窗口）")
    else:
        if "AC_QUICK" in da:
            fail("defaultAction() 又出现了 AC_QUICK —— 默认动作被「执行技艺」劫持，绑定后将无法重新绑定")
        if "AC_ARTS" not in da:
            fail("defaultAction() 未返回 AC_ARTS")

    # 字段初始化也必须是 AC_ARTS（未覆写时的兜底）
    if not re.search(r"defaultAction\s*=\s*AC_ARTS\s*;", ring):
        fail("defaultAction 字段未初始化为 AC_ARTS")

    # ---------- ② actions()：AC_ARTS 恒有；有绑定时补 AC_QUICK ----------
    acts = body_of(ring, "public ArrayList<String> actions( Hero hero )")
    if acts is None:
        fail("MasterRing 找不到 actions(Hero)")
    else:
        if "AC_ARTS" not in acts:
            fail("actions() 未提供 AC_ARTS（物品窗口里将无法打开技艺窗口）")
        if "AC_QUICK" not in acts:
            fail("actions() 未提供 AC_QUICK（物品窗口里缺少「执行：X」入口）")
        if "heroClass != HeroClass.RING_MASTER" not in acts.replace("(", "(").replace("hero.heroClass", "heroClass"):
            # 宽松判据：只要存在职业门控即可
            if "HeroClass.RING_MASTER" not in acts:
                fail("actions() 缺少「仅环指大师可用」门控")

    # ---------- ③ 副槽按钮：类存在、占副槽、自带去留守卫 ----------
    if "class QuickArtAction extends Buff implements ActionIndicator.Action" not in ring:
        fail("缺少 QuickArtAction（屏幕右侧一键执行按钮的载体）")
    else:
        act = body_of(ring, "public boolean act()")
        if act is None:
            fail("QuickArtAction 缺少 act()")
        else:
            if "ActionIndicator.setSecondAction" not in act:
                fail("QuickArtAction.act() 未占副槽（setSecondAction）")
            if "detach()" not in act:
                fail("QuickArtAction.act() 未在没有绑定时自我摘除（按钮会残留）")
            if "ActionIndicator.secondAction != this" not in act:
                fail("QuickArtAction.act() 缺少「槽位不是自己才占位」守卫（每回合都会重建按钮）")
        if "ActionIndicator.clearAction( this )" not in ring:
            fail("QuickArtAction 未覆写 detach() 释放副槽")
        if "revivePersists = true" not in ring:
            fail("QuickArtAction 未设置 revivePersists（安卡复活后按钮会消失）")
        if "ring.quickArt().icon()" not in ring:
            fail("QuickArtAction.actionIcon() 未取绑定技艺的图标")
        if "ring.execute( Dungeon.hero, AC_QUICK )" not in ring:
            fail("QuickArtAction.doAction() 未复用指环的执行分支（会绕过 canPerform 校验）")

    # 挂载点：设置绑定时 + 打开窗口时各补挂一次
    if "Buff.affect( hero, QuickArtAction.class )" not in ring:
        fail("缺少 ensureQuickArtButton 的 Buff.affect 挂载")
    if "ensureQuickArtButton( hero );" not in ring:
        fail("打开技艺窗口时未补挂副槽按钮（修复前存的旧档没有这个 buff）")

    # ---------- ④ 窗口长按：指环缺失时必须出声，不能静默 ----------
    if "大师指环不在身上" not in wnd:
        fail("WndRingMasterArts 长按绑定时未处理「指环不在身上」（会静默无事发生）")
    if "ring.setQuickArt(art);" not in wnd:
        fail("WndRingMasterArts 长按未调用 setQuickArt")

    # ---------- ⑤ 话术：玩家可见的字符串里不能再宣称「点击指环直接执行」 ----------
    #（历史注释里保留旧话术原文，故先剥注释再判）
    code_ring = strip_comments(ring)
    code_wnd = strip_comments(wnd)
    if "点击指环直接执行" in code_ring or "点击指环直接执行" in code_wnd:
        fail("玩家可见文案仍残留「点击指环直接执行」（与修复后的行为不符）")
    if "快速技艺" not in code_ring:
        fail("MasterRing.desc() 未提示当前绑定的快速技艺")

    # ---------- 输出 ----------
    if fails:
        print("FAIL (%d)" % len(fails))
        for f in fails:
            print("  -", f)
        sys.exit(1)
    print("ALL PASS：默认动作恒定开窗 / 物品窗口双入口 / 副槽一键执行按钮 / 话术一致")


if __name__ == "__main__":
    main()
