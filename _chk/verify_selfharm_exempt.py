# -*- coding: utf-8 -*-
"""
核验「自伤换成长」豁免链是否完整（2026-09-18 中指长兄 T3「过人的毅力」）。

这条链跨 5 个文件、4 处代码 + 2 份文本，任何一处单独改动都会静默失效：
  ① items/SelfHarmCost.java          标记载体存在且是 interface
  ② ChaliceOfBlood / WristSlit       都要 implements SelfHarmCost（漏一个 ⇒ 那件道具仍可被刷）
  ③ Talent.perseveranceCap           必须是 4 参（多出 src）且带 `src instanceof SelfHarmCost` 早退，
                                     且守卫要早于「天赋点数」判定（否则只有点了天赋才豁免，语义写反）
  ④ Hero.damage                      调用点必须把 src 当第 4 个实参传下去（不传 ⇒ 豁免永不生效）
  ⑤ actors_zh / actors 的 perseverance.desc 必须写明例外（否则玩家以为点了天赋就白嫖）

运行：python _chk/verify_selfharm_exempt.py
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
JAVA = os.path.join(ROOT, "core", "src", "main", "java", "com", "shatteredpixel", "shatteredpixeldungeon")
MSG = os.path.join(ROOT, "core", "src", "main", "assets", "messages", "actors")

fails = []


def fail(msg):
    fails.append(msg)
    print("FAIL  " + msg)


def read(path):
    with io.open(path, encoding="utf-8") as f:
        return f.read()


def java(*rel):
    return os.path.join(JAVA, *rel)


# ---------- ① 标记接口 ----------
p_face = java("items", "SelfHarmCost.java")
if not os.path.exists(p_face):
    fail("缺少 items/SelfHarmCost.java")
else:
    t = read(p_face)
    if not re.search(r"public\s+interface\s+SelfHarmCost\s*\{", t):
        fail("SelfHarmCost.java 不是 public interface")
    for must in ("ChaliceOfBlood", "WristSlit"):
        if must not in t:
            fail("SelfHarmCost.java 的说明未点名实现者: " + must)
    if "Talent.perseveranceCap" not in t:
        fail("SelfHarmCost.java 未说明「谁消费本标记」")

# ---------- ② 两件道具都实现接口 ----------
IMPL = [
    (("items", "artifacts", "ChaliceOfBlood.java"), "ChaliceOfBlood", "Artifact"),
    (("items", "weapon", "melee", "WristSlit.java"), "WristSlit", "Dirk"),
]
for rel, cls, parent in IMPL:
    p = java(*rel)
    label = "/".join(rel)
    if not os.path.exists(p):
        fail("缺少 " + label)
        continue
    t = read(p)
    if not re.search(r"class\s+%s\s+extends\s+%s\s+implements\s+SelfHarmCost\s*\{" % (cls, parent), t):
        fail("%s 未声明 `class %s extends %s implements SelfHarmCost`" % (label, cls, parent))
    if "import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;" not in t:
        fail("%s 缺少 SelfHarmCost 的 import" % label)

# ---------- ③ perseveranceCap 的签名与守卫 ----------
t_talent = read(java("actors", "hero", "Talent.java"))
if not re.search(r"perseveranceCap\s*\(\s*Hero\s+hero\s*,\s*int\s+dmg\s*,\s*int\s+shield\s*,\s*Object\s+src\s*\)", t_talent):
    fail("Talent.perseveranceCap 签名不是 (Hero, int, int, Object src)")
else:
    i_def = t_talent.index("public static int perseveranceCap(")
    body = t_talent[i_def:i_def + 4000]
    i_guard = body.find("src instanceof SelfHarmCost")
    i_floor = body.find("perseveranceFloorPercent( hero )")
    if i_guard < 0:
        fail("Talent.perseveranceCap 缺少 `src instanceof SelfHarmCost` 豁免")
    elif i_floor >= 0 and i_guard > i_floor:
        fail("豁免守卫写在 perseveranceFloorPercent 之后 ⇒ 只有点了天赋才生效，语义写反")

# ---------- ④ 调用点必须传 src ----------
t_hero = read(java("actors", "hero", "Hero.java"))
calls = re.findall(r"Talent\.perseveranceCap\s*\(([^;]*?)\)\s*;", t_hero, re.S)
if not calls:
    fail("Hero.java 找不到 perseveranceCap 调用点")
for args in calls:
    flat = " ".join(args.split())
    if flat.count(",") != 3:
        fail("Hero.java 调用点实参个数 != 4（src 没传下去）：%s" % flat)
    elif not flat.rstrip().endswith("src"):
        fail("Hero.java 调用点第 4 实参不是 src：%s" % flat)

# ---------- ⑤ 两份文本都写明例外 ----------
DESC = {
    "actors_zh.properties": ("例外", "血祭", "割腕"),
    "actors.properties": ("Exception", "prick", "Wrist Slit"),
}
for name, musts in DESC.items():
    t = read(os.path.join(MSG, name))
    m = re.search(r"^actors\.hero\.talent\.perseverance\.desc=(.*)$", t, re.M)
    if not m:
        fail("%s 缺 actors.hero.talent.perseverance.desc" % name)
        continue
    desc = m.group(1)
    for must in musts:
        if must not in desc:
            fail("%s 的 perseverance.desc 未写明例外关键字: %s" % (name, must))
    if desc.count("_") % 2 != 0:
        fail("%s 的 perseverance.desc 下划线标记不成对（%d 个）" % (name, desc.count("_")))
    if "\\n" not in desc:
        fail("%s 的 perseverance.desc 没有换行" % name)
    if "/n/" in desc:
        fail("%s 的 perseverance.desc 出现 /n/ 误写" % name)
    if "\ufffd" in desc:
        fail("%s 的 perseverance.desc 含 U+FFFD" % name)

print("--- 检查完毕 ---")
if fails:
    print("FAILED (%d)" % len(fails))
    sys.exit(1)
print("ALL PASS（豁免链 1~5 全部成立）")
