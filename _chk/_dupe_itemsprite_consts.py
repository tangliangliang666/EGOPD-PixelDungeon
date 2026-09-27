# -*- coding: utf-8 -*-
"""解析 ItemSpriteSheet.java 里的公开常量，做「重复值」体检。
用来抓「新加的图集格位常量与既有常量撞号」这类静默 bug。
"""
import re
import sys

SRC = 'core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/sprites/ItemSpriteSheet.java'
TX = 16


def xy(x, y):
    return (y - 1) * TX + (x - 1)


def main():
    src = open(SRC, encoding='utf-8').read()
    src = re.sub(r'/\*.*?\*/', '', src, flags=re.S)
    src = re.sub(r'//[^\n]*', '', src)

    # 先抓组的基址
    vals = {}
    order = []
    # 迭代若干轮，直到不再有新常量解出（组基址可能引用其它常量）
    decls = re.findall(r'public static final int\s+(\w+)\s*=\s*([^;]+);', src)
    for rnd in range(6):
        changed = False
        for name, expr in decls:
            e = expr.strip()
            e = re.sub(r'\bxy\s*\(\s*(\d+)\s*,\s*(\d+)\s*\)',
                       lambda m: str(xy(int(m.group(1)), int(m.group(2)))), e)
            # 把已知常量名替换成字面量
            for k in sorted(vals, key=len, reverse=True):
                e = re.sub(r'\b%s\b' % re.escape(k), str(vals[k]), e)
            if re.fullmatch(r'[\d\s+\-*()]+', e):
                try:
                    v = eval(e, {'__builtins__': {}}, {})
                except Exception:
                    continue
                if name not in vals:
                    order.append(name)
                    changed = True
                if vals.get(name) != v:
                    changed = True
                vals[name] = v
        if not changed:
            break

    unresolved = [n for n, _ in decls if n not in vals]
    print('解出 %d 个常量；未解出 %d 个 %s' % (len(vals), len(unresolved), unresolved[:12]))

    rev = {}
    for n in order:
        rev.setdefault(vals[n], []).append(n)

    dup = {v: ns for v, ns in rev.items() if len(ns) > 1}
    print('=== 重复值 ===')
    if not dup:
        print('无')
    for v in sorted(dup):
        print('  %d (xy(%d,%d)) -> %s' % (v, v % TX + 1, v // TX + 1, dup[v]))

    for name in ('UNKNOWN_ITEM', 'CHEST', 'LOCKED_CHEST', 'CRYSTAL_CHEST', 'EBONY_CHEST',
                 'BONES', 'REMAINS', 'TOMB', 'GRAVE'):
        if name in vals:
            v = vals[name]
            print('  %-14s = %-4d  xy(%d,%d)' % (name, v, v % TX + 1, v // TX + 1))
        else:
            print('  %-14s = ???' % name)

    print('=== 31 号格位（UNKNOWN_ITEM 应占）谁在用 ===')
    print('  ', rev.get(31, '（无人）'))


main()
