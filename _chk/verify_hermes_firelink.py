# -*- coding: utf-8 -*-
"""
核验：2026-09-20
① 赫尔墨斯的双蛇杖 { bones = false }（死亡后不进英雄遗骸）
② 传火大剑跨局继承强化为「原 trueLevel()/5（向下取整）+ 附魔」

判据来源：源码（4 个文件）+ 文本（items_zh / items）。每条断言都配反例自测
（把改动前的备份文本喂给同一批判据，必须判 FAIL），避免恒真的空测。
"""
import os
import re
import sys

ROOT = r"D:\PD"


def J(*p):
    return os.path.join(ROOT, *p)


HERMES = J("core", "src", "main", "java", "com", "shatteredpixel", "shatteredpixeldungeon",
           "items", "weapon", "melee", "HermesCaduceus.java")
FIRELINK = J("core", "src", "main", "java", "com", "shatteredpixel", "shatteredpixeldungeon",
             "items", "weapon", "melee", "FirelinkGreatsword.java")
SETTINGS = J("core", "src", "main", "java", "com", "shatteredpixel", "shatteredpixeldungeon", "SPDSettings.java")
ZH = J("core", "src", "main", "assets", "messages", "items", "items_zh.properties")
EN = J("core", "src", "main", "assets", "messages", "items", "items.properties")
BAK = J("_chk", "_bak_2026-09-20c")

KEY = "items.weapon.melee.firelinkgreatsword.stats_desc"

fails = []
notes = []


def chk(cond, msg):
    if cond:
        print("  ok   %s" % msg)
    else:
        fails.append(msg)
        print("  FAIL %s" % msg)
    return bool(cond)


def load(p):
    with open(p, "rb") as f:
        return f.read().decode("utf-8")


def strip_comments(t):
    t = re.sub(r"/\*.*?\*/", "", t, flags=re.S)
    t = re.sub(r"//[^\n]*", "", t)
    return t


def body_of(src, sig):
    m = re.search(sig, src)
    if not m:
        return None
    i = src.index("{", m.end())
    depth = 0
    for j in range(i, len(src)):
        if src[j] == "{":
            depth += 1
        elif src[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i:j + 1]
    return None


def code_of(src, sig):
    b = body_of(src, sig)
    return strip_comments(b) if b is not None else None


def prop_value(text, key):
    hits = [l for l in text.split("\n") if l.startswith(key + "=")]
    return hits[0][len(key) + 1:] if len(hits) == 1 else None


def before(hay, a, b):
    """a 在 b 之前；任一缺失都算 False（空方法体时不会抛 ValueError，也不会恒真）"""
    try:
        return hay.index(a) < hay.index(b)
    except ValueError:
        return False


# =====================================================================
# ① HermesCaduceus: bones = false
# =====================================================================
def check_hermes(text):
    out = []
    code = strip_comments(text)  # 注释里写了「EquipableItem 默认 bones = true」⇒ 必须先剥注释
    # 取「实例初始化块」：从 image = ...HERMES_CADUCEUS 到块尾
    m = re.search(r"image\s*=\s*ItemSpriteSheet\.HERMES_CADUCEUS;", code)
    blk = code[m.start(): code.index("\n\t}", m.start())] if m else ""
    out.append(("实例块内 bones = false", bool(re.search(r"bones\s*=\s*false\s*;", blk))))
    # 类内任何位置都不允许再出现 bones = true（注释已剥掉）
    out.append(("代码内没有 bones = true", not re.search(r"bones\s*=\s*true", code)))
    return out


print("[1] HermesCaduceus.java")
hm = load(HERMES)
for msg, ok in check_hermes(hm):
    chk(ok, "hermes: " + msg)
# 反例：改动前的备份在「实例块内 bones = false」上必须判 FAIL
hm_old = load(os.path.join(BAK, "HermesCaduceus.java"))
old_hermes = dict(check_hermes(hm_old))
chk(old_hermes.get("实例块内 bones = false") is False, "hermes 反例 FAIL：旧文件实例块内没有 bones = false")

# =====================================================================
# ② FirelinkGreatsword: 等级/5 + 附魔
# =====================================================================
def check_firelink(text, setting_text):
    out = []
    code = strip_comments(text)  # 实现注释里会引用被删掉的旧写法原文
    mark = code_of(text, r"public\s+static\s+void\s+markNextRun\s*\(") or ""
    grant = code_of(text, r"public\s+static\s+void\s+grantAtRunStart\s*\(") or ""
    enchf = code_of(text, r"private\s+static\s+Enchantment\s+enchantFromName\s*\(") or ""
    out.append(("markNextRun 方法体取到", bool(mark)))
    out.append(("grantAtRunStart 方法体取到", bool(grant)))
    out.append(("enchantFromName 方法体取到", bool(enchf)))

    # —— 常量与取数口径 ——
    out.append(("INHERIT_LEVEL_DIVISOR = 5",
                bool(re.search(r"INHERIT_LEVEL_DIVISOR\s*=\s*5\s*;", code))))
    out.append(("markNextRun 取 trueLevel()（纯升级等级）", "trueLevel()" in mark))
    out.append(("markNextRun 不用 level()（含诅咒注能加成）",
                not re.search(r"wep\.level\(\)", mark)))
    out.append(("markNextRun 写入 firelinkNextRun(true) / firelinkLevel( / firelinkEnchant(",
                all(s in mark for s in ("firelinkNextRun(true)", "firelinkLevel(", "firelinkEnchant("))))
    out.append(("markNextRun 取附魔类全名 getName()", "getClass().getName()" in mark))

    # —— 发放侧：先读后清、除法在前、升级、附魔 ——
    out.append(("grant: 等级 = srcLevel / INHERIT_LEVEL_DIVISOR",
                "srcLevel / INHERIT_LEVEL_DIVISOR" in grant))
    out.append(("grant: 先读记录再清零",
                before(grant, "SPDSettings.firelinkLevel()", "SPDSettings.firelinkLevel(0)")))
    out.append(("grant: 清零早于造剑（不残留到再下一局）",
                before(grant, "firelinkLevel(0)", "new FirelinkGreatsword()")))
    out.append(("grant: 先算等级再 upgrade",
                before(grant, "srcLevel / INHERIT_LEVEL_DIVISOR", "upgrade(inheritLevel)")))
    out.append(("grant: upgrade 有 inheritLevel > 0 守卫",
                bool(re.search(r"if\s*\(\s*inheritLevel\s*>\s*0\s*\)\s*firelink\.upgrade\(\s*inheritLevel\s*\)", grant))))
    out.append(("grant: 附魔经 enchantFromName 并 enchant(ench)",
                "enchantFromName(enchantClass)" in grant and "firelink.enchant(ench)" in grant))
    out.append(("grant: 附魔早于 identify/collect",
                before(grant, "firelink.enchant(ench)", "firelink.identify()")))
    out.append(("grant: 不存在 srcLevel / 5.0 之类浮点写法",
                not re.search(r"srcLevel\s*/\s*5\.\d", grant)))

    # —— 反射重建附魔 ——
    out.append(("enchantFromName 用 Reflection.forName + isAssignableFrom(Enchantment)",
                "Reflection.forName(" in enchf and "Enchantment.class.isAssignableFrom(" in enchf))
    out.append(("enchantFromName 失败时返回 null 且有 catch 兜底",
                bool(re.search(r"return\s+null\s*;", enchf)) and "catch" in enchf))

    # —— 旧写法必须消失（已剥注释） ——
    out.append(("equippedBy 走 equippedInstance（旧的双 instanceof 写法已消失）",
                "equippedInstance(hero) != null" in code))
    out.append(("旧「Item firelink = new FirelinkGreatsword();」白板写法已消失",
                "Item firelink = new FirelinkGreatsword();" not in code))
    out.append(("Reflection 已 import", "import com.watabou.utils.Reflection;" in text))

    # —— SPDSettings 三键齐全 ——
    out.append(("SPDSettings: KEY_FIRELINK_LEVEL = \"firelink_level\"",
                '"firelink_level"' in setting_text))
    out.append(("SPDSettings: KEY_FIRELINK_ENCHANT = \"firelink_enchant\"",
                '"firelink_enchant"' in setting_text))
    out.append(("SPDSettings: firelinkLevel 读 getInt(KEY, 0)",
                bool(re.search(r"getInt\(\s*KEY_FIRELINK_LEVEL\s*,\s*0\s*\)", setting_text))))
    out.append(("SPDSettings: firelinkEnchant 读 getString(KEY, \"\")",
                bool(re.search(r'getString\(\s*KEY_FIRELINK_ENCHANT\s*,\s*""\s*\)', setting_text))))
    out.append(("SPDSettings: firelinkEnchant 写入时 null 兜底为空串",
                bool(re.search(r'value\s*==\s*null\s*\?\s*""\s*:\s*value', setting_text))))
    return out


print("[2] FirelinkGreatsword.java + SPDSettings.java")
fl = load(FIRELINK)
st = load(SETTINGS)
for msg, ok in check_firelink(fl, st):
    chk(ok, "firelink: " + msg)

# 反例：改动前的备份（+ 改动前的 settings）必须至少在这些关键判据上 FAIL
fl_old = load(os.path.join(BAK, "FirelinkGreatsword.java"))
st_old = load(os.path.join(BAK, "SPDSettings.java"))
old_res = dict(check_firelink(fl_old, st_old))
must_fail = ["INHERIT_LEVEL_DIVISOR = 5", "markNextRun 取 trueLevel()（纯升级等级）",
             "grant: 等级 = srcLevel / INHERIT_LEVEL_DIVISOR",
             "SPDSettings: KEY_FIRELINK_LEVEL = \"firelink_level\"",
             "SPDSettings: KEY_FIRELINK_ENCHANT = \"firelink_enchant\""]
for m in must_fail:
    chk(old_res.get(m) is False, "firelink 反例 FAIL：%s" % m)

# =====================================================================
# ③ 文本
# =====================================================================
print("[3] stats_desc 文本（zh / en）")


def check_prop(text, is_zh):
    out = []
    v = prop_value(text, KEY)
    out.append(("键恰好 1 行", v is not None))
    if v is None:
        return out
    out.append(("值内无真换行（未断行）", "\n" not in v and "\r" not in v))
    out.append(("用字面量转义分段 \\n\\n", "\\n\\n" in v))
    out.append(("没有 /n/ 误写", "/n/" not in v))
    out.append(("_ 强调标记成对", v.count("_") % 2 == 0))
    out.append(("无 U+FFFD", "\ufffd" not in v))
    want = ("五分之一", "附魔") if is_zh else ("a fifth", "enchantment")
    out.append(("写明「1/5 + 附魔」(%s)" % ("zh" if is_zh else "en"),
                all(w in v for w in want)))
    return out


zh = load(ZH)
en = load(EN)
for msg, ok in check_prop(zh, True):
    chk(ok, "items_zh: " + msg)
for msg, ok in check_prop(en, False):
    chk(ok, "items: " + msg)

# 反例
old_zh = load(os.path.join(BAK, "items_zh.properties"))
old_en = load(os.path.join(BAK, "items.properties"))
ro = dict(check_prop(old_zh, True))
chk(ro.get("写明「1/5 + 附魔」(zh)") is False, "items_zh 反例 FAIL：旧文案不写 1/5")
if prop_value(old_en, KEY) is not None:
    re_ = dict(check_prop(old_en, False))
    chk(re_.get("用字面量转义分段 \\n\\n") is False, "items 反例 FAIL：旧英文只有一段")
else:
    notes.append("旧英文 stats_desc 值取不到，跳过该反例")

# =====================================================================
# ④ 全文件断行自检（防「entry 断成两行、续行没 =」）
# =====================================================================
print("[4] .properties 断行自检")
for p in (ZH, EN):
    t = load(p)
    bad = []
    for i, line in enumerate(t.split("\n"), 1):
        s = line.rstrip("\r").strip()
        if not s or s.startswith("#") or s.startswith("!"):
            continue
        if "=" not in s:
            bad.append(i)
    chk(not bad, "%s 无缺 = 的行（可疑行号 %s）" % (os.path.basename(p), bad[:8]))

print()
for n in notes:
    print("NOTE:", n)
print("=" * 60)
if fails:
    print("FAILED: %d 条断言未通过" % len(fails))
    for f in fails:
        print("  -", f)
    sys.exit(1)
print("ALL PASS")
