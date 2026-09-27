# -*- coding: utf-8 -*-
"""QuickRecipe 指南第 6 页（液金 / 松脂）补入：焦炭松脂系列、黄金松脂系列、洛伊德护符。
字节级读写 + 断言，幂等。"""
import sys

P = r"D:\PD\core\src\main\java\com\shatteredpixel\shatteredpixeldungeon\ui\QuickRecipe.java"

IMP_ANCHOR = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.Generator;\r\n"
)
IMP_NEW = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.ArcaneResin;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.CharcoalResin;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.Generator;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.GoldenResin;\r\n"
)

IMP2_ANCHOR = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.Item;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;\r\n"
)
IMP2_NEW = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.Item;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.LiquidMetal;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.LloydTalisman;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.LooseCharcoalResin;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.LooseGoldenResin;\r\n"
)

IMP3_ANCHOR = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;\r\n"
)
IMP3_NEW = (
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;\r\n"
    b"import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.AquaBrew;\r\n"
)

CASE6_ANCHOR = (
    b"\t\t\t\t\tnew ArcaneResin()));\r\n"
    b"\t\t\t\treturn result;\r\n"
)
CASE6_NEW = (
    b"\t\t\t\t\tnew ArcaneResin()));\r\n"
    b"\t\t\t\t//2026-09-24:\xe7\x84\xa6\xe7\x82\xad\xe6\x9d\xbe\xe8\x84\x82 / \xe9\xbb\x84\xe9\x87\x91\xe6\x9d\xbe\xe8\x84\x82\xe7\xb3\xbb\xe5\x88\x97\xef\xbc\x88\xe5\x8e\x9f\xe5\x85\x88\xe5\x8f\xaa\xe7\x99\xbb\xe8\xae\xb0\xe5\x9c\xa8 Recipe \xe8\xa1\xa8\xe9\x87\x8c\xef\xbc\x8c\xe7\x82\xbc\xe9\x87\x91\xe6\x8c\x87\xe5\x8d\x97\xe9\xa1\xb5\xe7\x9c\x8b\xe4\xb8\x8d\xe5\x88\xb0\xef\xbc\x8c\xe8\xbf\x99\xe9\x87\x8c\xe8\xa1\xa5\xe4\xb8\x8a\xe5\xb1\x95\xe7\xa4\xba\xef\xbc\x89\r\n"
    b"\t\t\t\tresult.add(new QuickRecipe( new CharcoalResin.FromPotion(),\r\n"
    b"\t\t\t\t\t\tnew ArrayList<Item>(Arrays.asList(new PotionOfLiquidFlame())),\r\n"
    b"\t\t\t\t\t\tnew CharcoalResin()));\r\n"
    b"\t\t\t\tresult.add(new QuickRecipe( new LooseCharcoalResin.SplitRecipe(),\r\n"
    b"\t\t\t\t\t\tnew ArrayList<Item>(Arrays.asList(new CharcoalResin())),\r\n"
    b"\t\t\t\t\t\tnew LooseCharcoalResin().quantity(2)));\r\n"
    b"\t\t\t\tresult.add(new QuickRecipe( new GoldenResin.FromPotion(),\r\n"
    b"\t\t\t\t\t\tnew ArrayList<Item>(Arrays.asList(new ShockingBrew())),\r\n"
    b"\t\t\t\t\t\tnew GoldenResin()));\r\n"
    b"\t\t\t\tresult.add(new QuickRecipe( new LooseGoldenResin.SplitRecipe(),\r\n"
    b"\t\t\t\t\t\tnew ArrayList<Item>(Arrays.asList(new GoldenResin())),\r\n"
    b"\t\t\t\t\t\tnew LooseGoldenResin().quantity(2)));\r\n"
    b"\t\t\t\t//2026-09-24: \xe6\xb4\x9b\xe4\xbc\x8a\xe5\xbe\xb7\xe6\x8a\xa4\xe7\xac\xa6\xef\xbc\x9a\xe6\xb2\xbb\xe7\x96\x97\xe8\x8d\xaf\xe6\xb0\xb4 \xc3\x97 1 + \xe6\xb6\xb2\xe9\x87\x91 \xc3\x97 20\xef\xbc\x8c3 \xe8\x83\xbd\xe9\x87\x8f \xe2\x86\x92 \xe6\xb4\x9b\xe4\xbc\x8a\xe5\xbe\xb7\xe6\x8a\xa4\xe7\xac\xa6 \xc3\x97 6\r\n"
    b"\t\t\t\tresult.add(new QuickRecipe( new LloydTalisman.CraftRecipe(),\r\n"
    b"\t\t\t\t\t\tnew ArrayList<Item>(Arrays.asList(new PotionOfHealing(), new LiquidMetal().quantity(20))),\r\n"
    b"\t\t\t\t\t\tnew LloydTalisman().quantity(6)));\r\n"
    b"\t\t\t\treturn result;\r\n"
)

OPS = [("import-1", IMP_ANCHOR, IMP_NEW, "CharcoalResin;"),
       ("import-2", IMP2_ANCHOR, IMP2_NEW, "LloydTalisman;"),
       ("import-3", IMP3_ANCHOR, IMP3_NEW, "PotionOfHealing;"),
       ("case6", CASE6_ANCHOR, CASE6_NEW, "LloydTalisman.CraftRecipe()")]


def main():
    dry = "--dry" in sys.argv
    raw = open(P, "rb").read()
    crlf0 = raw.count(b"\r\n")
    probe = raw.replace(b"\r\n", b"")
    assert not probe.count(b"\r"), "QuickRecipe.java 含裸 CR"
    if crlf0 == 0:
        eol = b"\n"
        print("  [info] QuickRecipe.java 为纯 LF（无 CRLF）")
    else:
        assert not probe.count(b"\n"), "QuickRecipe.java 行尾混存"
        eol = b"\r\n"
    ops = [(n, o.replace(b"\r\n", eol), w.replace(b"\r\n", eol), m)
           for (n, o, w, m) in OPS]

    for name, old, new, marker in ops:
        if old not in raw:
            if marker.encode("utf-8") in raw:
                print("  [skip] %s 已应用" % name)
                continue
            print("[FAIL] %s 锚点未命中" % name)
            sys.exit(1)
        assert raw.count(old) == 1, "%s 锚点命中 %d 次" % (name, raw.count(old))
        nb = new.count(eol)
        ob = old.count(eol)
        old_brace = old.count(b"(") - old.count(b")")
        new_brace = new.count(b"(") - new.count(b")")
        assert old_brace == new_brace, "%s 括号收支不等 (%d -> %d)" % (name, old_brace, new_brace)
        raw = raw.replace(old, new)
        print("  [%s] %-10s 行 %+d  括号收支 %d" % ("dry" if dry else "ok", name, nb - ob, new_brace))

    if dry:
        print("dry-run，未落盘")
        return
    open(P, "wb").write(raw)
    crlf1 = raw.count(b"\r\n")
    print("CRLF %d -> %d (delta %+d)" % (crlf0, crlf1, crlf1 - crlf0))


if __name__ == "__main__":
    main()
