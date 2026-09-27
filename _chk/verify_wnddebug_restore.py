# -*- coding: utf-8 -*-
"""WndDebug 还原核验（EGOPD）。

背景：调试窗曾抽成独立库（:debug-console），用户反馈「UI 与原版不同、物品点击不获取」，
现已按反编译结果重建游戏内 windows/WndDebug.java（WndTabbed + ScrollingListPane 原生控件）。
本脚本对还原做源码级断言，防止日后又退化回自绘控件 / 失去点击获取行为。

用法：
    python _chk/verify_wnddebug_restore.py
    python _chk/verify_wnddebug_restore.py --selftest   # 反例自测
退出码 0 = 全部通过。
"""
import os
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "core", "src", "main", "java",
                   "com", "shatteredpixel", "shatteredpixeldungeon", "windows", "WndDebug.java")
SPD = os.path.join(ROOT, "core", "src", "main", "java",
                   "com", "shatteredpixel", "shatteredpixeldungeon", "ShatteredPixelDungeon.java")
WNDGAME = os.path.join(ROOT, "core", "src", "main", "java",
                       "com", "shatteredpixel", "shatteredpixeldungeon", "windows", "WndGame.java")


def strip_comments(src: str) -> str:
    """单趟剥注释（同 check_unused_imports.py 的状态扫描口径）。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n:
            nxt = src[i + 1]
            if nxt == "/":
                j = src.find("\n", i)
                i = n if j == -1 else j
                continue
            if nxt == "*":
                j = src.find("*/", i + 2)
                i = n if j == -1 else j + 2
                continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j == -1 else j + 3
            out.append(src[i:j])
            i = j
            continue
        if c in "\"'":
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == c:
                    j += 1
                    break
                j += 1
            out.append(src[i:j])
            i = j
            continue
        out.append(c)
        i += 1
    return "".join(out)


def read(path):
    with open(path, "r", encoding="utf-8") as f:
        return f.read()


def check(src_clean, label, bad=False):
    """src_clean 为已剥注释的正文。"""
    results = []

    def ok(name, cond):
        results.append((name, cond))

    # 1. 必须是 SPD 原生控件：extends WndTabbed + 用 ScrollingListPane
    ok("extends WndTabbed（SPD 原生多页签窗口）", "class WndDebug extends WndTabbed" in src_clean)
    ok("使用 ui.ScrollingListPane（SPD 原生列表）", "new ScrollingListPane()" in src_clean)
    ok("不引用独立库的 WndDebugConsole", "WndDebugConsole" not in src_clean)
    ok("不引用独立库的 ConsolePanel/ConsoleListView", "ConsolePanel" not in src_clean and "ConsoleListView" not in src_clean)

    # 2. 10 个选卡（药水/卷轴/武器/远程/防具/神器/饰品/杂项/Buff/怪物）
    for tab in ["药水", "卷轴", "武器", "远程", "防具", "神器", "饰品", "杂项", "Buff", "怪物"]:
        ok(f"选卡「{tab}」存在", f'LabeledTab( "{tab}" )' in src_clean)

    # 3. 物品点击 → collect() 入背包（用户核心诉求）
    ok("物品 spawnItem 走 collect()（点击自动获取）",
       "if (!item.collect())" in src_clean)
    ok("背包满则掉落到英雄脚下", "Dungeon.level.drop( item, Dungeon.hero.pos )" in src_clean)
    ok("ItemTab 点击回调调用 spawnItem",
       "spawnItem( clazz )" in src_clean)

    # 4. 怪物点击 → 隐藏窗口 + 点击地图放置
    ok("怪物点击先 hide() 再 spawnMob", "hide();" in src_clean and "spawnMob( clazz )" in src_clean)
    ok("怪物走 GameScene.selectCell 点击放置", "GameScene.selectCell" in src_clean)

    # 5. buff 点击 → Buff.affect
    ok("buff 点击走 Buff.affect", "Buff.affect( Dungeon.hero" in src_clean)

    # 6. 类发现：本地 classpath + Android dex 双通道
    ok("本地 classpath 扫描", "java.class.path" in src_clean)
    ok("Android dex 解析", "classes*.dex" not in src_clean and "endsWith( \".dex\" )" in src_clean)

    return results


def run(path, label):
    src = read(path)
    clean = strip_comments(src)
    return check(clean, label)


def selftest():
    """反例自测：证明判据并非恒真。"""
    bad_src = (
        "class WndDebug extends SomeOtherWindow {\n"
        "  void build(){ useCustomPanel(); }\n"
        "}"
    )
    res = check(bad_src, "bad", bad=True)
    # 断言至少有若干条失败
    fail = [n for n, c in res if not c]
    print(f"  SELFTEST：坏样本上 {len(fail)} 条断言变红（应 >0）")
    if not fail:
        print("  SELFTEST FAIL：判据恒真！")
        return 1
    print("  SELFTEST OK")
    return 0


def main():
    if "--selftest" in sys.argv:
        return selftest()

    bad = 0

    # 完整断言只对 WndDebug.java 本体跑
    if not os.path.exists(SRC):
        print(f"  MISSING  {SRC}")
        bad += 1
    else:
        results = run(SRC, "WndDebug.java")
        npass = sum(1 for _, c in results if c)
        for name, cond in results:
            print(f"  {'OK ' if cond else 'FAIL'}  WndDebug.java: {name}")
        if npass != len(results):
            bad += 1

    # 调用点：两处都走 GameScene.show(new WndDebug())
    spd = read(SPD) if os.path.exists(SPD) else ""
    wg = read(WNDGAME) if os.path.exists(WNDGAME) else ""
    ok_f2 = "GameScene.show(new com.shatteredpixel.shatteredpixeldungeon.windows.WndDebug())" in spd
    ok_menu = "GameScene.show( new WndDebug() )" in wg
    print(f"  {'OK ' if ok_f2 else 'FAIL'}  调用点: F2 走 GameScene.show(WndDebug)")
    print(f"  {'OK ' if ok_menu else 'FAIL'}  调用点: WndGame 按钮走 GameScene.show(WndDebug)")
    if not ok_f2 or not ok_menu:
        bad += 1
    # 不应再直接 addToFront 独立库窗口
    if "addToFront(new com.mypd.debugconsole.WndDebugConsole())" in spd:
        print("  FAIL  调用点: 仍残留独立库窗口的 addToFront")
        bad += 1
    # 菜单按钮不应再走 SpdDebugConsole.open()
    if "SpdDebugConsole.open()" in wg:
        print("  FAIL  调用点: WndGame 按钮仍走 SpdDebugConsole.open()")
        bad += 1

    print("ALL PASS" if bad == 0 else f"FAIL: {bad} 处未通过")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
