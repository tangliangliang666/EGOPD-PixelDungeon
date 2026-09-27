#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
核验 SPD 调试控制台的「宿主适配层」四个必须兜底的点。

背景（2026-09-21）：控制台窗口能开、页签和图标都出来了，但 ① 绝大部分生物名显示不出来、
② 点「怪物」页签整页崩。根因两条：

  A. 文本走了 BitmapText(PixelScene.pixelFont)。该字体用 LATIN_FULL 加载，注释写明
     "Only latin characters supported" —— 中文名全部落到 get('?')，于是整列变成问号。
     旧版窗口用的是 RenderedTextBlock（FreeType 管线，有 CJK 字形），所以必须让字体
     接缝能返回 Component 型文本控件（库侧 Gizmo 而非 Visual）。
  B. Mob.sprite() = Reflection.newInstance(spriteClass)，而 VaultMob / DemonSpawner /
     FungalCore 这类活体怪物的 spriteClass 直到放入关卡才赋值 ⇒ 这里收到 null，
     libGDX 的 ClassReflection.newInstance(null) 抛 NPE。旧的 WndDebug$MobTab 用
     try/catch 兜到 ItemSpriteSheet.MOB_HOLDER，新版漏了。

本脚本做纯静态检查（不需要编译游戏），断言：

  1. SpdConsoleFonts 的 provider 返回 RenderedTextBlock（不是 BitmapText）；
  2. 库侧 Fonts.Provider.create 的返回类型是 Gizmo（不是 Visual）——否则 ① 根本做不到；
  3. Fonts.at / widthOf / heightOf / ConsolePanel.align 都能处理 Component 型文本；
  4. SpdConsoleHost 的 icon() 对 Mob 走带兜底的路径（icon → mobIcon → fallbackMobIcon）；
  5. fallbackMobIcon 真的指向 ItemSpriteSheet.MOB_HOLDER；
  6. 源码里不存在「用 BitmapText(pixelFont) 做列表标签」的写法。

用法：python _chk/verify_console_fonts_and_mobs.py [--selftest]
"""

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

LIB_FONTS = os.path.join(ROOT, "debug-console", "src", "main", "java",
                         "com", "mypd", "debugconsole", "ui", "Fonts.java")
LIB_ROW = os.path.join(ROOT, "debug-console", "src", "main", "java",
                       "com", "mypd", "debugconsole", "ui", "ConsoleListView.java")
LIB_PANEL = os.path.join(ROOT, "debug-console", "src", "main", "java",
                         "com", "mypd", "debugconsole", "ui", "ConsolePanel.java")
HOST = os.path.join(ROOT, "core", "src", "main", "java", "com", "shatteredpixel",
                    "shatteredpixeldungeon", "debug", "SpdConsoleHost.java")
SPD_FONTS = os.path.join(ROOT, "core", "src", "main", "java", "com", "shatteredpixel",
                         "shatteredpixeldungeon", "debug", "SpdConsoleFonts.java")


def read(path):
    with io.open(path, encoding="utf-8") as f:
        return f.read()


def strip_comments(src):
    """去掉 // 与 /* */ 注释，避免把注释里的说明当成真代码。"""
    src = re.sub(r"/\*.*?\*/", "", src, flags=re.S)
    src = re.sub(r"//[^\n]*", "", src)
    return src


class Checker:
    def __init__(self):
        self.fails = []
        self.passes = []

    def ok(self, cond, msg):
        if cond:
            self.passes.append(msg)
        else:
            self.fails.append(msg)
        return cond

    def contains(self, src, needle, msg):
        return self.ok(needle in src, msg)

    def not_contains(self, src, needle, msg):
        return self.ok(needle not in src, msg)


def main(selftest=False):
    c = Checker()

    # ---- 库侧接缝 ----------------------------------------------------------
    fonts = read(LIB_FONTS)
    fonts_code = strip_comments(fonts)

    c.contains(fonts_code, "Gizmo create(String text, int size);",
               "Fonts.Provider.create 返回 Gizmo（不是 Visual）")
    c.not_contains(fonts_code, "Visual create(String text, int size);",
                   "Fonts.Provider.create 不返回 Visual")
    c.contains(fonts_code, "public static float widthOf(Gizmo g)",
               "Fonts.widthOf 接受 Gizmo")
    c.contains(fonts_code, "public static float heightOf(Gizmo g)",
               "Fonts.heightOf 接受 Gizmo")
    c.contains(fonts_code, "public static void at(Gizmo g, float x, float y)",
               "Fonts.at 接受 Gizmo")
    # at() 必须对 Component 走 setPos（否则控件自身 layout 不跑，文字会偏位）
    c.contains(fonts_code, "((Component) g).setPos(x, y)",
               "Fonts.at 对 Component 调 setPos")
    # widthOf/heightOf 对 Component 必须调方法而不是读受保护字段
    c.contains(fonts_code, "((Component) g).width()",
               "Fonts.widthOf 用 Component.width() 访问器")
    c.not_contains(fonts_code, "((Component) g).width;",
                   "Fonts.widthOf 不直接读 protected 字段")

    panel = read(LIB_PANEL)
    panel_code = strip_comments(panel)
    c.contains(panel_code, "public static void align(Gizmo g)",
               "ConsolePanel.align 接受 Gizmo")
    c.contains(panel_code, "c.setPos(align(c.left()), align(c.top()))",
               "ConsolePanel.align 对 Component 调 setPos")

    row = read(LIB_ROW)
    row_code = strip_comments(row)
    c.contains(row_code, "private Gizmo label;",
               "ConsoleListView.Row 的 label 是 Gizmo")
    c.not_contains(row_code, "private Visual label;",
                   "ConsoleListView.Row 的 label 不是 Visual")
    c.contains(row_code, "Fonts.squeezeToFit(label, avail)",
               "Row 用 squeezeToFit 统一处理缩放（Component 不会被硬压）")
    c.contains(row_code, "if (line != null) {",
               "Row.layout 容忍 line 为 null（标题行没有分隔线）")

    # ---- SPD 适配：字体 ---------------------------------------------------
    spd = read(SPD_FONTS)
    spd_code = strip_comments(spd)
    c.contains(spd_code, "PixelScene.renderTextBlock(size)",
               "SpdConsoleFonts 用 PixelScene.renderTextBlock 造文本")
    c.contains(spd_code, "public Gizmo create(String text, int size)",
               "SpdConsoleFonts 的 create 返回 Gizmo")
    # 关键禁令：不得再回退成全 latin 的像素位图字体
    c.not_contains(spd_code, "new BitmapText(PixelScene.pixelFont)",
                   "SpdConsoleFonts 没有用 BitmapText(pixelFont)（latin-only）")
    c.not_contains(spd_code, "LATIN_FULL",
                   "SpdConsoleFonts 没有引用 LATIN_FULL")

    # ---- SPD 适配：怪物图标兜底 ------------------------------------------
    host = read(HOST)
    host_code = strip_comments(host)

    c.contains(host_code, "return mobIcon((Mob) sample);",
               "SpdConsoleHost.icon 对 Mob 走 mobIcon 兜底路径")
    c.not_contains(host_code, "CharSprite sprite = ((Mob) sample).sprite();",
                   "SpdConsoleHost.icon 不直接调用 mob.sprite()（会 NPE）")
    c.contains(host_code, "private static Image mobIcon(Mob mob)",
               "存在 mobIcon 兜底方法")
    c.contains(host_code, "if (sprite != null) {",
               "mobIcon 对 null 精灵做了判空")
    c.contains(host_code, "return fallbackMobIcon();",
               "mobIcon 失败时回落到 fallbackMobIcon()")
    c.contains(host_code, "new ItemSprite(ItemSpriteSheet.MOB_HOLDER)",
               "fallbackMobIcon 指向 ItemSpriteSheet.MOB_HOLDER")

    # 兜底必须在 try/catch 之内（反射构造精灵类可能抛别的异常）
    m = re.search(r"private static Image mobIcon\(Mob mob\)\s*\{(.*?)\n\t\}", host_code, re.S)
    if c.ok(m is not None, "能定位 mobIcon 方法体"):
        body = m.group(1)
        c.contains(body, "try {", "mobIcon 内有 try")
        c.contains(body, "catch (Throwable", "mobIcon 内 catch Throwable")

    # ---- selftest：确认断言真的会红 ---------------------------------------
    if selftest:
        print("[selftest] 用「坏」样本反测断言是否真的会失败……")
        bad = Checker()
        bad_fonts = strip_comments(
            "import com.watabou.noosa.Visual;\n"
            "public interface Provider { Visual create(String text, int size); }\n"
        )
        bad.contains(bad_fonts, "Gizmo create(String text, int size);",
                     "(selftest) Visual-typed create should FAIL")
        bad_src = (
            "public Image icon(Class<?> clazz) {\n"
            "\tCharSprite sprite = ((Mob) sample).sprite();\n"
            "}\n"
        )
        bad.not_contains(bad_src, "CharSprite sprite = ((Mob) sample).sprite();",
                         "(selftest) direct sprite() call should FAIL")
        print("          反测失败数 = %d（应 >= 2）" % len(bad.fails))
        for f in bad.fails:
            print("            - " + f)
        if len(bad.fails) < 2:
            print("SELFTEST FAIL：断言不够硬，坏样本没被抓住")
            return 1
        print("SELFTEST OK：断言确实会在坏样本上变红")
        return 0

    # ---- 汇总 -------------------------------------------------------------
    for p in c.passes:
        print("  OK   " + p)
    for f in c.fails:
        print("  FAIL " + f)

    print("")
    if c.fails:
        print("FAIL：%d 条不合格，%d 条通过" % (len(c.fails), len(c.passes)))
        return 1
    print("ALL PASS（%d 条）" % len(c.passes))
    return 0


if __name__ == "__main__":
    sys.exit(main(selftest="--selftest" in sys.argv))
