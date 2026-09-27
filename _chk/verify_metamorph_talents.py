# -*- coding: utf-8 -*-
"""
蜕变卷轴天赋判据回归（2026-09-20 起）
==================================
背景：`ScrollOfMetamorphosis` 会把**别职业**的天赋塞进本英雄的天赋表
（`TalentButton` 里 `newTier.put(talent, tier.get(replacing))` → `Dungeon.hero.talents`，
并记进 `Dungeon.hero.metamorphedTalents`）。所以「战士身上有过人的毅力」是合法状态。

若天赋效果函数写成 `if (hero.heroClass != HeroClass.MIDDLE_FINGER) return ...;`，
这些被蜕变出来的天赋就会**静默失效**：点满了却零效果、不报错、无提示。

本脚本断言两类事实：
  A. **必须**只按天赋点数判定（`hasTalent` / `pointsInTalent`）——函数体内不得出现 `heroClass`；
  B. **必须保留**职业判定的地方（核心机制 buff / 盔甲技能 / 子职业 / 台词音效 / 上游代码）仍在，
     防止「过度修正」把职业独有机制也一并拆掉。
含反例自测：把坏写法注回去，断言脚本会变红（防止恒真空测）。

用法：python _chk/verify_metamorph_talents.py [--list]
"""
import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"

PASS = []
FAIL = []


def ok(cond, msg):
    (PASS if cond else FAIL).append(msg)
    return bool(cond)


# --------------------------------------------------------------- 源码读取/清洗
def read(path):
    with io.open(os.path.join(ROOT, path), "r", encoding="utf-8", newline="") as f:
        return f.read().replace("\r\n", "\n")


def strip_literals(src):
    """把注释与字符串/字符字面量换成空格（保留换行），便于括号配平与方法定位。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        nxt = src[i + 1] if i + 1 < n else ""
        if c == "/" and nxt == "/":
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif c == "/" and nxt == "*":
            j = src.find("*/", i + 2)
            j = n if j < 0 else j + 2
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j]))
            i = j
        elif c in "\"'":
            j = i + 1
            while j < n and src[j] != c:
                j += 2 if src[j] == "\\" else 1
            j = min(j + 1, n)
            out.append("".join(ch if ch == "\n" else " " for ch in src[i:j]))
            i = j
        else:
            out.append(c)
            i += 1
    return "".join(out)


def methods(src):
    """返回 [(name, start_line, end_line)]（1-based 闭区间），name 可能是 None（静态块等）。"""
    clean = strip_literals(src)
    res, stack = [], []
    depth, last_break, line = 0, 0, 1
    for i, ch in enumerate(clean):
        if ch == "\n":
            line += 1
        elif ch == "{":
            sig = clean[last_break:i].replace("\n", " ")
            m = re.search(r"(\w+)\s*\([^;{}]*\)\s*$", sig)
            name = m.group(1) if m else None
            # 控制结构（if/for/…）不是方法：置 None，让 --list 落到真正的方法名上
            if name in ("if", "for", "while", "switch", "try", "catch", "synchronized", "do", "else", "new"):
                name = None
            stack.append((name, line, depth))
            depth += 1
            last_break = i + 1
        elif ch == "}":
            depth -= 1
            if stack and stack[-1][2] == depth:
                name, sl, _ = stack.pop()
                res.append((name, sl, line))
            last_break = i + 1
        elif ch == ";":
            last_break = i + 1
    return res


def body_of(src, name):
    """取具名方法的函数体（含花括号）。找不到返回 None。"""
    clean = strip_literals(src)
    pat = re.compile(r"(?:public|private|protected)\s+(?:static\s+)?[\w<>\[\],\.\s]*?\b" + re.escape(name) + r"\s*\(")
    m = pat.search(clean)
    if not m:
        return None
    i = clean.find("{", clean.find(")", m.end()))
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(clean)):
        if clean[j] == "{":
            depth += 1
        elif clean[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i:j + 1]
    return None


def code(text):
    """去掉注释后的代码（查代码里的关键词，免得被注释里的示例骗绿）。"""
    return strip_literals(text)


# ================================================================== A 类断言
# A1) 天赋效果函数体内**不得**出现 heroClass
NATIVE_FREE = [
    # 中指长兄
    "onHeroAttackStarted", "onHeroAttackLanded", "onHeroAttackResolved",
    "onHeroAttackBrag", "onHeroAttackMissed", "onEnemyAttackLanded",
    "isHeroDealtDamage", "tooHotImmune",
    "perseveranceFloorPercent", "perseveranceCap", "beastFuryMultiplier",
    "gritTeethDuration", "gritTeethEndHeal", "nearDeathFuryMultiplier",
    "ledgerDevourMultiplier", "executionUnleashLevels", "executionUnleashDuration",
    "instantExecutionChargeFactor", "handyToySwapCost", "onAttackProc",
    # 拇指 前二老板
    "onPrecognitionActivated", "onHeroDodgedEnemyAttack", "valencinaSmoke",
]


def check_talent(src):
    ok("【判据红线】" in src, "Talent.java 枚举体顶部有【判据红线】说明块")
    ok("_chk/verify_metamorph_talents.py" in src, "说明块指向本回归脚本")

    for name in NATIVE_FREE:
        b = body_of(src, name)
        if not ok(b is not None, "Talent.%s 存在" % name):
            continue
        ok("heroClass" not in code(b), "Talent.%s 不再按职业判定" % name)

    # 进食钩子的两段（同一函数里还夹着上游的 DUELIST/CLERIC 分支，只能按片段断言）
    food = body_of(src, "onFoodEaten")
    ok(food is not None, "Talent.onFoodEaten 存在")
    if food:
        ok("hasTalent(HUNTING_MEAL)" in code(food)
           and "heroClass == HeroClass.VALENCINA && hero.hasTalent(HUNTING_MEAL)" not in code(food),
           "Talent.onFoodEaten 的「狩猎一餐」只按天赋判定")
        ok("hasTalent(RECORD_MEAL)" in code(food)
           and "heroClass == HeroClass.MIDDLE_FINGER && hero.hasTalent(RECORD_MEAL)" not in code(food),
           "Talent.onFoodEaten 的「记录一餐」只按天赋判定")

    # 子职业判定要留着（蜕变卷轴不发子职业）
    for name, sub in (("onHeroAttackStarted", "FAMILY_SHAME"),
                      ("onHeroAttackLanded", "WAR_HERO"),
                      ("onHeroAttackResolved", "WAR_HERO"),
                      ("onPrecognitionActivated", "FAMILY_SHAME")):
        b = body_of(src, name) or ""
        if name == "onHeroAttackStarted":
            b += " " + (body_of(src, "onHeroAttackLanded") or "")  # 剑轨清空在 started 里
        ok(sub in code(b), "Talent.%s 仍按子职业 %s 判定" % (name, sub))

    # 家族背叛者的两个钩子是「取消外层职业判定、改由被调方自守」
    for name, call in (("onHeroAttackStarted", "FamilyBetrayal.onAttackStarted"),
                       ("onHeroAttackResolved", "FamilyBetrayal.consumeAfterAttack")):
        b = code(body_of(src, name) or "")
        ok(call + "(" in b.replace(" ", "").replace("\t", ""), "Talent.%s 仍调用 %s" % (name, call))

    # 旧名必须绝迹（改成了 isHeroDealtDamage）
    ok("isMiddleFingerDamage" not in src, "Talent.java 已无旧名 isMiddleFingerDamage")
    ok("isHeroDealtDamage" in src, "Talent.java 含新名 isHeroDealtDamage")


# ================================================================== B 类断言
# 核心机制 / 上游代码的职业判定必须保留（按「文件 + 代码片段 + 期望出现次数」断言，
# 次数一并钉住：少一次＝被误删，多一次＝有人又拿职业当天赋判据）
KEEP = [
    (PKG + "actors/hero/Talent.java", "HeroClass cls = Dungeon.hero", 1,
     "Talent.icon() 仍按职业取「充能天赋」图标（同一个天赋不同职业不同图）"),
    (PKG + "actors/buffs/AcceleratingFuture.java", "hero.heroClass != HeroClass.VALENCINA", 1,
     "加速的未来（拇指核心机制 buff）仍判职业"),
    (PKG + "actors/buffs/ValencinaTracker.java", "hero.heroClass == HeroClass.VALENCINA", 1,
     "ValencinaTracker（拇指核心机制载体）仍判职业"),
    (PKG + "actors/buffs/InstructionTarget.java", "HeroClass.ORACLE", 1,
     "指令标记（神谕核心机制）仍判职业"),
    (PKG + "actors/Char.java", "hero.heroClass != HeroClass.VALENCINA", 1,
     "Char.trackAcceleratingFuture（拇指核心机制）仍判职业"),
    (PKG + "actors/Char.java", "Dungeon.hero.heroClass != HeroClass.CLERIC", 3,
     "上游 Cleric 的天赋分支未被误改（Char.java 3 处）"),
    (PKG + "actors/hero/spells/GuidingLight.java", "HeroClass.CLERIC", 1,
     "上游 GuidingLight 未被误改"),
    (PKG + "actors/mobs/Mob.java", "HeroClass.CLERIC", 1,
     "上游 Mob 未被误改"),
]


def check_keeps():
    for path, needle, cnt, desc in KEEP:
        got = code(read(path)).count(needle)
        ok(got == cnt, "%s（实际 %d 处）" % (desc, got))


def check_visuals():
    # 拇指：加速弹的两条获取途径都只看天赋
    s = code(body_of(read(PKG + "actors/mobs/npcs/Shopkeeper.java"), "canBuyAccelerationRounds") or "")
    ok("hasTalent(Talent.ACCEL_AMMO)" in s and "heroClass" not in s,
       "Shopkeeper.canBuyAccelerationRounds 只看「加速弹药」不看职业")

    src = read(PKG + "items/AccelerationRound.java")
    b = body_of(src, "testIngredients")
    ok(b is not None and "pointsInTalent( Talent.ACCEL_AMMO ) < 2" in code(b) and "heroClass" not in code(b),
       "AccelerationRound 的液金配方 +2 只看「加速弹药」不看职业")

    src = read(PKG + "actors/buffs/TremblingScorch.java")
    b = body_of(src, "onParalysisLifted") or ""
    ok("subClass == HeroSubClass.WAR_HERO" in code(b) and "heroClass" not in code(b),
       "TremblingScorch 按子职业判定（不再叠职业替身）")

    # 不再需要 HeroClass 的文件不该留死 import
    for path in (PKG + "actors/mobs/npcs/Shopkeeper.java",
                 PKG + "items/AccelerationRound.java",
                 PKG + "actors/buffs/TremblingScorch.java"):
        ok("HeroClass" not in read(path), "%s 已无 HeroClass 引用/死 import" % path.split("/")[-1])


# ================================================================== 反例自测
BROKEN = {}


def selftest():
    """把坏写法注回去 / 把该保留的判定删掉，断言脚本变红。"""
    checks = []

    t = read(PKG + "actors/hero/Talent.java")
    b = body_of(t, "beastFuryMultiplier")
    checks.append(("天赋函数重新按职业判",
                   t.replace(b, b.replace("if (hero == null) return 1f;",
                                          "if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return 1f;")),
                   check_talent))

    t2 = read(PKG + "actors/hero/Talent.java").replace("【判据红线】", "【???】")
    checks.append(("说明块被删", t2, check_talent))

    t3 = read(PKG + "actors/hero/Talent.java").replace("isHeroDealtDamage", "isMiddleFingerDamage")
    checks.append(("旧名回潮", t3, check_talent))

    spirit = read(PKG + "actors/buffs/AcceleratingFuture.java")
    checks.append(("核心机制的职业判定被误删",
                   spirit.replace("hero.heroClass != HeroClass.VALENCINA || ", ""),
                   lambda s: ok(code(s).count("hero.heroClass != HeroClass.VALENCINA") == 1,
                                "加速的未来（拇指核心机制 buff）仍判职业")))

    shop = read(PKG + "actors/mobs/npcs/Shopkeeper.java")
    bs = body_of(shop, "canBuyAccelerationRounds")
    checks.append(("加速弹又按职业判",
                   shop.replace(bs, bs.replace("&& Dungeon.hero.hasTalent(Talent.ACCEL_AMMO);",
                                               "&& Dungeon.hero.heroClass == HeroClass.VALENCINA\n"
                                               "\t\t\t\t&& Dungeon.hero.hasTalent(Talent.ACCEL_AMMO);")),
                   lambda s: ok("heroClass" not in code(body_of(s, "canBuyAccelerationRounds") or ""),
                                "Shopkeeper.canBuyAccelerationRounds 只看「加速弹药」不看职业")))

    t4 = read(PKG + "actors/buffs/TremblingScorch.java").replace(
        "hero != null && hero.subClass == HeroSubClass.WAR_HERO",
        "hero != null && hero.heroClass == HeroClass.VALENCINA && hero.subClass == HeroSubClass.WAR_HERO")
    checks.append(("震颤-灼热又把职业替身叠回去",
                   t4,
                   lambda s: ok("heroClass" not in code(body_of(s, "onParalysisLifted") or ""),
                                "TremblingScorch 按子职业判定（不再叠职业替身）")))

    all_red = True
    for desc, src, fn in checks:
        PASS.clear(); FAIL.clear()
        fn(src)
        red = len(FAIL) > 0
        print("  反例自测 %-28s → %s" % (desc, "变红 ✔" if red else "仍然全绿 ✘"))
        all_red &= red
    return all_red


# ================================================================== main
def main():
    src = read(PKG + "actors/hero/Talent.java")
    if "--list" in sys.argv:
        ms = methods(src)
        for ln, l in enumerate(src.split("\n"), 1):
            st = l.strip()
            if "heroClass" in l and not st.startswith("//") and not st.startswith("*"):
                best = None
                for name, a, b in ms:
                    if name and a <= ln <= b and (best is None or (b - a) < (best[2] - best[1])):
                        best = (name, a, b)
                print("%5d  %-28s %s" % (ln, best[0] if best else "(顶层)", st[:88]))
        return 0

    PASS.clear(); FAIL.clear()
    check_talent(src)
    check_keeps()
    check_visuals()

    n_pass, n_fail = len(PASS), len(FAIL)
    print("===== 断言 =====")
    for m in PASS:
        print("  PASS  " + m)
    for m in FAIL:
        print("  FAIL  " + m)

    print("\n===== 反例自测 =====")
    st_ok = selftest()

    print("\n断言 %d 条：PASS %d / FAIL %d；反例自测 %s"
          % (n_pass + n_fail, n_pass, n_fail, "全部变红" if st_ok else "有漏网"))
    if n_fail or not st_ok:
        print("结果：FAIL")
        return 1
    print("结果：ALL PASS")
    return 0


if __name__ == "__main__":
    sys.exit(main())
