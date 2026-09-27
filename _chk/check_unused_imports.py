# -*- coding: utf-8 -*-
"""未使用 import 自检（EGOPD）。

javac 对多余 import **完全沉默**（连 -Xlint:all 也不报），所以只能自己数：
某个 import 的简单名除 import 行之外在文件里出现 0 次 ⇒ 必然是多余 import。

## 为什么不用「正则去 /* */ 再去 //」

本仓的注释里大量出现 **markdown 强调 `//**文字**`**（例如
`//**远程 / 法术弹道**那一路不走 Char.attack`）。
`//**` 里含子串 `/*` ⇒ 若按「先正则去 `/*...*/`、再去 `//`」的顺序（skill 早先的写法），
这个 `/*` 会被当成块注释开头；文件里**没有**与之配对的 `*/`，于是**从该行一直吞到文件末尾**，
后面所有真实代码都被删掉 ⇒ 那些其实在用的 import 全被误判成「多余」。
（实测 `Char.java`：`/*` 出现 3 次、`*/` 只 2 次 ⇒ 9 个假阳性。）

所以这里用**单趟左到右的状态扫描**：一旦进入 `//` 注释就把整行吃掉，
里面的 `/*` 不再有特殊含义 —— 这才是 javac 的真实行为。

## 口径

字符串 / 字符字面量**原样保留**（不剥）。若某 import 的简单名只出现在字符串里，
本脚本会判它「在用」—— 这是**故意偏向漏报**：漏报只是少一次清理，
误报会让人去删一个真正在被调用的 import。

注释里的名字**一般**不算引用（否则 javadoc 里写 `{@code Messages.get}` 会把真·多余
import 掩盖掉），但 **`@link` / `@linkplain` / `@see` / `@throws` / `@exception` / `@value`
引用的类型名算引用** —— 那些标签本来就是指向类型的，删了 import 会让 javadoc 链接失效。

用法：
    python _chk/check_unused_imports.py <文件1> [文件2 ...]
    python _chk/check_unused_imports.py            # 无参时扫本次改动的那批文件
    python _chk/check_unused_imports.py --selftest # 反例自测（必跑：证明判据不是恒真）
退出码 0 = 全部通过；1 = 有可疑 import 或自测失败。
"""
import os
import re
import sys


IMPORT_RE = re.compile(r"^[ \t]*import[ \t]+(static[ \t]+)?([\w.]+?)(\.\*)?[ \t]*;[ \t]*$", re.M)

# javadoc 标签里对**类型**的真实引用。`{@code X}` 只是排版、不算引用，故不在列。
JAVADOC_REF_RE = re.compile(r"@(?:link|linkplain|see|throws|exception|value)\s+([\w.]+)")


def strip_comments(src: str) -> str:
    """单趟扫描剥注释；字符串 / 文本块 / 字符字面量原样保留（见模块 docstring）。

    **例外**：块注释里被 `@link` / `@linkplain` / `@see` / `@throws` / `@exception` / `@value`
    引用的类型名会被回填进结果 —— 那是 javadoc 对类型的真实引用，对应的 import 不算多余。
    （2026-09-23 实测：`Talent.java` 的 `ExecutionUnleashed` 只被 `{@link ExecutionUnleashed#apply}`
    引用，旧版一律剥注释 ⇒ 误报成多余 import。）"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]

        # ---- 注释 ----
        if c == "/" and i + 1 < n:
            nxt = src[i + 1]
            if nxt == "/":                      # 行注释：整行吃掉（保留 \n）
                j = src.find("\n", i)
                i = n if j == -1 else j
                continue
            if nxt == "*":                      # 块注释：吃到配对的 */
                j = src.find("*/", i + 2)
                end = n if j == -1 else j + 2
                for ref in JAVADOC_REF_RE.findall(src[i:end]):
                    out.append(" " + ref + " ")
                i = end
                continue

        # ---- 文本块 """ ----（必须在普通字符串之前判）
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            j = n if j == -1 else j + 3
            out.append(src[i:j])
            i = j
            continue

        # ---- 字符串 / 字符字面量：原样保留 ----
        if c in ('"', "'"):
            quote = c
            j = i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == quote:
                    j += 1
                    break
                if src[j] == "\n":              # 未闭合（跨行非法）→ 保险退出
                    break
                j += 1
            out.append(src[i:j])
            i = j
            continue

        out.append(c)
        i += 1

    return "".join(out)


IMPORT_RE = re.compile(r"^[ \t]*import[ \t]+(static[ \t]+)?([\w.]+?)(\.\*)?[ \t]*;[ \t]*$", re.M)


def check(path: str):
    """返回 (多余列表, 已检查的 import 数)。"""
    raw = open(path, encoding="utf-8").read()
    body = strip_comments(raw)
    body_wo_imports = IMPORT_RE.sub("", body)   # 数名字时要排除 import 行本身

    unused, total = [], 0
    for m in IMPORT_RE.finditer(body):
        is_static, fqn, wildcard = m.group(1), m.group(2), m.group(3)
        if is_static or wildcard:               # 静态引入 / 通配不参与判断
            continue
        total += 1
        simple = fqn.rsplit(".", 1)[-1]
        if not re.search(r"\b" + re.escape(simple) + r"\b", body_wo_imports):
            unused.append(fqn)
    return unused, total


DEFAULT_FILES = (
    "items/artifacts/ArtifactEnhanceRecipe.java", "items/artifacts/BloodFeastChalice.java",
    "items/artifacts/LifelongStew.java", "items/artifacts/ChainOfOthers.java",
    "items/artifacts/ChapterNineVerseTwo.java", "items/artifacts/ApproachingDay.java",
    "items/artifacts/ChaliceOfBlood.java", "items/artifacts/HornOfPlenty.java",
    "items/artifacts/EtherealChains.java", "items/artifacts/TalismanOfForesight.java",
    "items/artifacts/UnstableSpellbook.java",
    "actors/buffs/ThirstBloodBarrier.java", "actors/buffs/Bleeding.java",
    "actors/Char.java", "items/Recipe.java", "ui/QuickRecipe.java",
    "sprites/ItemSpriteSheet.java",
)


def selftest() -> int:
    """反例自测：证明判据既不恒真、也不被 //** 强调注释骗到。"""
    src = (
        "package x;\n"
        "import a.b.Used;        // 真在用\n"
        "import a.b.Dead;        // 真多余\n"
        "import a.b.Tricky;      // 只被 //**强调**坑过的那一行影响\n"
        'import a.b.InString;    // 只出现在字符串里\n'
        "import a.b.Linked;      // 只被 javadoc 的 {@link} 引用\n"
        "class T {\n"
        "  //**远程 / 法术弹道**那一路不走 Char.attack\n"   # ← 含 /* 的 // 注释
        "  void f(){ Used u = null; Tricky t = null; }\n"
        "  /** 见 {@link Linked#go}。注意 {@code Dead} 只是排版，不算引用。 */\n"
        "  void g(){}\n"
        '  String s = "InString";\n'
        "}\n"
    )
    stripped = strip_comments(src)

    # 复用 check 的判据，但喂内存字符串（不走文件）
    body_wo_imports = IMPORT_RE.sub("", stripped)
    got = []
    for m in IMPORT_RE.finditer(stripped):
        if m.group(1) or m.group(3):
            continue
        simple = m.group(2).rsplit(".", 1)[-1]
        if not re.search(r"\b" + re.escape(simple) + r"\b", body_wo_imports):
            got.append(simple)

    ok = True
    if "Used" in got:
        print("  自测失败：Used 真被引用，不该判多余"); ok = False
    if "Dead" not in got:
        print("  自测失败：Dead 确实多余，应判出来"); ok = False
    if "Tricky" in got:
        print("  自测失败：//**强调**注释把后面的真代码吞了（旧写法复发）"); ok = False
    if "InString" in got:
        print("  自测失败：字符串里的名字按口径应算「在用」"); ok = False
    if "Linked" in got:
        print("  自测失败：只被 javadoc {@link} 引用的名字不该判多余"); ok = False
    # 再验一条「真的多余」：如果判据恒真，Dead 就出不来 —— 上面已覆盖
    print("  自测通过：真在用不被误报 / 真多余能报出 / //** 不吞代码 / 字符串算在用 / {@link} 算引用"
          if ok else "  自测未通过")
    return 0 if ok else 1


def main():
    if "--selftest" in sys.argv:
        return selftest()

    if len(sys.argv) > 1:
        files = [a for a in sys.argv[1:] if not a.startswith("--")]
    else:
        root = os.path.join("core", "src", "main", "java", "com", "shatteredpixel",
                            "shatteredpixeldungeon")
        files = [os.path.join(root, f) for f in DEFAULT_FILES]

    bad = 0
    for f in files:
        if not os.path.exists(f):
            print(f"  MISSING  {f}")
            bad += 1
            continue
        unused, total = check(f)
        name = os.path.basename(f)
        if unused:
            bad += 1
            print(f"  多余 x{len(unused)}  {name}")
            for u in unused:
                print(f"          -> {u}")
        else:
            print(f"  OK ({total:2d} 条 import)  {name}")

    print()
    print("ALL PASS" if bad == 0 else f"FAIL: {bad} 个文件有多余 import")
    return 0 if bad == 0 else 1


if __name__ == "__main__":
    sys.exit(main())
