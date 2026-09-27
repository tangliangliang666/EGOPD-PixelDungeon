# -*- coding: utf-8 -*-
"""
BINAH（理解）实现 · 第 1 步：把「自然生成」收口到 Item.random()。

做法与 HOKMA 的 `Char.speed()` 完全同构：
  · `Item.random()` 改为 `final`，内部转调 `Trials.modifyGeneratedItem(randomRaw())`；
  · 8 个子类原本覆写的 `public Item random()` 一律改名 `protected Item randomRaw()`。
`final` 让编译器保证「没有第二个出口」——以后谁再覆写 random() 会直接编译不过。

⚠️ 本脚本**只改方法签名那一行**，绝不触碰方法体：子类 random() 里全是 `Random.xxx()`
   调用，动一个字节都可能改变随机流 ⇒ 关卡种子布局全体漂移。

⚠️ 必须走**字节级**替换（`open(p,'rb')`）：本工程 Java 源是 **CRLF**，
   用文本模式 `io.open(..., newline='\\n')` 会把整个文件改写成 LF，
   git diff 立刻变成「每行都改了」——曾踩过一次。脚本末尾有 CRLF 计数断言兜底。

幂等：任一文件已出现 `randomRaw` 即整体跳过。
备份：改动前把原文件拷到 `_chk/_bak_binah/`。
"""
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, 'core', 'src', 'main', 'java', 'com', 'shatteredpixel', 'shatteredpixeldungeon')

# 8 个子类（第 9 处是 Item 基类，已在收口时手改）
TARGETS = [
    'items/armor/Armor.java',
    'items/artifacts/Artifact.java',
    'items/bombs/Bomb.java',
    'items/Gold.java',
    'items/rings/Ring.java',
    'items/wands/Wand.java',
    'items/weapon/missiles/MissileWeapon.java',
    'items/weapon/Weapon.java',
]

OLD = b'\tpublic Item random() {'
NEW = b'\tprotected Item randomRaw() {'

failures = []


def readb(p):
    with open(p, 'rb') as f:
        return f.read()


def writeb(p, b):
    with open(p, 'wb') as f:
        f.write(b)


def report(ok, msg):
    print(('  [OK]   ' if ok else '  [FAIL] ') + msg)
    if not ok:
        failures.append(msg)


# ---------- 0. 前置：确认 Item 基类已收口 ----------
print('== 0. 前置检查：Item.random() 已是 final 收口 ==')
item_java = os.path.join(JAVA, 'items', 'Item.java')
src_item = readb(item_java)
report(b'public final Item random() {' in src_item, 'Item.random() 为 final')
report(b'protected Item randomRaw() {' in src_item, 'Item.randomRaw() 为 protected 默认实现')
report(b'Trials.modifyGeneratedItem( randomRaw() )' in src_item, 'Item.random() 转调 Trials.modifyGeneratedItem')

# ---------- 1. 逐文件改名 ----------
print('== 1. 8 个子类 random() -> randomRaw() ==')
bakdir = os.path.join(ROOT, '_chk', '_bak_binah')
for rel in TARGETS:
    p = os.path.join(JAVA, *rel.split('/'))
    if not os.path.exists(p):
        report(False, '缺文件：' + rel)
        continue
    raw = readb(p)

    if b'Item randomRaw()' in raw:
        report(True, '%-46s 已应用，跳过' % rel)
        continue

    hits = raw.count(OLD)
    if hits != 1:
        report(False, '%-46s 期望恰好 1 处 `public Item random() {`，实得 %d' % (rel, hits))
        continue

    if not os.path.isdir(bakdir):
        os.makedirs(bakdir)
    bak = os.path.join(bakdir, rel.replace('/', '__'))
    shutil.copy2(p, bak)

    crlf_before = raw.count(b'\r\n')
    raw2 = raw.replace(OLD, NEW)

    # 比位置而非比存在：确认改动落在原方法声明处
    if raw2.find(NEW) != raw.find(OLD):
        report(False, '%-46s 替换位置偏移，已放弃' % rel)
        continue

    writeb(p, raw2)

    back = readb(p)
    report(back.count(b'\r\n') == crlf_before,
           '%-46s CRLF 计数保持 %d' % (rel, crlf_before))
    report(b'Item randomRaw()' in back and OLD not in back,
           '%-46s 改名完成' % rel)

# ---------- 2. 全库收尾断言 ----------
print('== 2. 全库断言 ==')
bad = []
for dirpath, _dirnames, filenames in os.walk(JAVA):
    for fn in filenames:
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        if b'public Item random()' in readb(p):
            bad.append(os.path.relpath(p, JAVA).replace(os.sep, '/'))
report(not bad, '全库无残留 `public Item random()` 覆写' + ('' if not bad else '：' + ', '.join(bad)))

# 禁止 super.random()（基类 random() 现在带 BINAH 夹子，子类若转调会重复夹取）
sup = []
for dirpath, _dirnames, filenames in os.walk(JAVA):
    for fn in filenames:
        if not fn.endswith('.java'):
            continue
        p = os.path.join(dirpath, fn)
        if b'super.random()' in readb(p):
            sup.append(os.path.relpath(p, JAVA).replace(os.sep, '/'))
report(not sup, '全库无 `super.random()` 调用' + ('' if not sup else '：' + ', '.join(sup)))

# 混行文件（既有 CRLF 又有裸 LF）会把 git diff 弄脏，顺手查一遍本脚本碰过的 8 个文件
print('== 3. 换行风格复核（本脚本碰过的文件） ==')
mixed = []
for rel in TARGETS:
    p = os.path.join(JAVA, *rel.split('/'))
    raw = readb(p)
    crlf, lf = raw.count(b'\r\n'), raw.count(b'\n')
    if crlf != lf:
        mixed.append('%s(CRLF=%d LF=%d)' % (rel, crlf, lf))
report(not mixed, '8 个文件换行一致（纯 CRLF）' + ('' if not mixed else '：' + ', '.join(mixed)))

print()
if failures:
    print('结果：%d 项失败' % len(failures))
    sys.exit(1)
print('结果：ALL PASS')
