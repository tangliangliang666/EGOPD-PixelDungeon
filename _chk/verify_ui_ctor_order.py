#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Component 构造顺序坑 · 静态回归。

背景（2026-09-21，:debug-console 一次踩中三处）：
  Component() 的构造函数**自己会调 createChildren()**，而 Java 的**实例字段初始化器
  在 super() 返回后才执行**。于是在 createChildren() 里做「赋值 + 挂到场景」会踩两种后果：

    A. 访问自己的字段（如 `for (X x : panes)`）    ⇒ super() 阶段读到 null ⇒ 构造即 NPE
    B. 局部 new 再赋给字段（如 `bg = new ColorBlock(...)`）
       ⇒ 对象已挂进场景、**字段却被随后的初始化器覆盖回 null**
       ⇒ 不崩、不报错，只是所有 `if (x != null)` 静默跳过

  B 是最阴的：「窗口开了，但每行只有一条分隔横线」「页签底色永不更新」都是它。
  **编译期 100% 正常**，只有真跑起来才看得见。

本脚本静态检查两件事：
  1. 任何 `createChildren()` 方法体里**不得出现赋值**（`x = ...` / `this.x = ...`）
     —— 正确写法是一律留空、把建子元素的动作放进自己的构造函数体。
  2. 同文件里若出现了 `super()` 之后又手动调 `createChildren()` 的类（如 ConsolePanel 故意
     在构造体里再调一次），则它的子类**必须**把 createChildren 留空 —— 这条由 1 覆盖。

用法：
  python _chk/verify_ui_ctor_order.py [目录...]     # 默认扫 debug-console/src
  python _chk/verify_ui_ctor_order.py --selftest    # 反例自测（必须能抓到 buggy 形态）
"""

import os
import re
import sys

DEFAULT_ROOTS = [r"D:\PD\debug-console\src"]

# 允许出现在 createChildren() 里的「赋值」白名单：局部变量声明（有类型）不算字段赋值
LOCAL_DECL = re.compile(r"^\s*(?:final\s+)?[A-Za-z_][\w.<>\[\],\s]*\s+\w+\s*=")


def strip_comments(text):
    """单趟左到右扫描去注释。

    别用「先正则去 /* */ 再去 //」：本仓注释习惯写 markdown 强调 `//**文字**`，
    其中的 `/*` 会被当成块注释开头 ⇒ 一路吞到文件尾（见 skill egopd-source-verify 铁律第 3 条）。
    """
    out = []
    i, n = 0, len(text)
    while i < n:
        c = text[i]
        if c == '/' and i + 1 < n and text[i + 1] == '/':
            j = text.find('\n', i)
            i = n if j < 0 else j
        elif c == '/' and i + 1 < n and text[i + 1] == '*':
            j = text.find('*/', i + 2)
            i = n if j < 0 else j + 2
        elif c == '"':
            j = i + 1
            while j < n and text[j] != '"':
                j += 2 if text[j] == '\\' else 1
            out.append(text[i:j + 1])
            i = j + 1
        else:
            out.append(c)
            i += 1
    return ''.join(out)


def method_body(src, sig_regex):
    """按大括号配平取方法体。"""
    m = re.search(sig_regex + r"\s*\{", src)
    if not m:
        return None
    start = m.end() - 1
    depth = 0
    for i in range(start, len(src)):
        if src[i] == '{':
            depth += 1
        elif src[i] == '}':
            depth -= 1
            if depth == 0:
                return src[start + 1:i]
    return None


def check_file(path):
    """返回 (问题列表, 检查到的 createChildren 个数)。"""
    problems = []
    raw = open(path, encoding='utf-8').read()
    src = strip_comments(raw)

    found = 0
    for m in re.finditer(r"protected\s+void\s+createChildren\s*\(\s*\)", src):
        found += 1
        # 取这个方法体的起点
        brace = src.find('{', m.end())
        if brace < 0:
            continue
        depth = 0
        end = brace
        for i in range(brace, len(src)):
            if src[i] == '{':
                depth += 1
            elif src[i] == '}':
                depth -= 1
                if depth == 0:
                    end = i
                    break
        body = src[brace + 1:end]

        for ln, line in enumerate(body.split('\n'), 1):
            s = line.strip()
            if not s or s.startswith('//'):
                continue
            if LOCAL_DECL.match(line):
                continue  # 局部变量声明，安全（值不存进字段）
            # 字段赋值： x = ...  或 this.x = ...
            if re.match(r"^(?:this\.)?\w+\s*(?:[+\-*/|&^]?=)(?!=)", s):
                problems.append(
                    f"{path}: createChildren() 内出现字段赋值 -> {s!r}")
            # .add( / addToBack( 等把「可能是 null 的字段」挂进场景
            elif re.search(r"\b(add|addToBack|addToFront)\s*\(", s):
                problems.append(
                    f"{path}: createChildren() 内 add() —— 若参数取自字段，此刻还是 null -> {s!r}")

    return problems, found


def selftest():
    """反例自测：构造 buggy 形态，确认判据能抓到；再构造正确形态，确认不误报。"""
    print("== 反例自测 ==")
    ok = True

    buggy = '''
public class PaneX extends Component {
    private ConsoleListView list;
    private boolean built = false;

    PaneX() {
        super();
    }

    @Override
    protected void createChildren() {
        list = new ConsoleListView();
        DebugConsole.Settings s = DebugConsole.settings();
        list.configure(s.listItemHeight, s.iconBoxSize);
        add(list);
    }
}
'''
    fixed = '''
public class PaneY extends Component {
    private ConsoleListView list;

    PaneY() {
        super();
        list = new ConsoleListView();
        add(list);
    }

    @Override
    protected void createChildren() {
        //deliberately empty
    }
}
'''
    tmp = os.path.join(os.path.dirname(os.path.abspath(__file__)), "_ctor_selftest.java")

    for name, content, want_problem in (("buggy", buggy, True), ("fixed", fixed, False)):
        with open(tmp, "w", encoding="utf-8", newline='\n') as f:
            f.write(content)
        probs, found = check_file(tmp)
        got = len(probs) > 0
        status = "PASS" if got == want_problem else "FAIL"
        if got != want_problem:
            ok = False
        print(f"  [{status}] {name}: 命中 {len(probs)} 条, 扫到 createChildren {found} 个 "
              f"(期望{'有' if want_problem else '无'}问题)")
        for p in probs:
            print("         ", p)

    try:
        os.remove(tmp)
    except OSError:
        pass

    print("反例自测:", "全部符合预期" if ok else "★ 判据有问题，别信它 ★")
    return ok


def main():
    args = sys.argv[1:]
    if "--selftest" in args:
        return 0 if selftest() else 1
    if "--selftest-only" in args:
        return 0

    roots = [a for a in args if not a.startswith("--")] or DEFAULT_ROOTS

    files = []
    for r in roots:
        if os.path.isfile(r):
            files.append(r)
            continue
        for dirpath, dirnames, filenames in os.walk(r):
            dirnames[:] = [d for d in dirnames if d not in ("build", ".gradle", "out")]
            for fn in filenames:
                if fn.endswith(".java"):
                    files.append(os.path.join(dirpath, fn))

    all_problems = []
    total_methods = 0
    scanned = 0
    for f in sorted(files):
        try:
            probs, found = check_file(f)
        except (UnicodeDecodeError, OSError):
            continue
        scanned += 1
        total_methods += found
        all_problems.extend(probs)

    print(f"扫描 {scanned} 个 .java，发现 createChildren() 方法 {total_methods} 个")
    if all_problems:
        print("失败：createChildren() 里存在字段赋值/挂载（构造顺序坑）：")
        for p in all_problems:
            print("  -", p)
        print("\n改法：把这些动作搬进**自己的构造函数体**，createChildren() 留空。")
        print("原理见 debug-console/README.md §5「头号陷阱」。")
        return 1

    print("OK：所有 createChildren() 均为空/无字段赋值（构造顺序坑已避开）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
