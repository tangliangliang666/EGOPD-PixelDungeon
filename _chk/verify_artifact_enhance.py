# -*- coding: utf-8 -*-
"""原版神器强化形态（5 件）接线 + 文本 + 配方核验（2026-09-20）。

覆盖五组：

  A. **父类钩子**：5 个原版神器各自被新开的 protected 钩子必须存在、默认实现＝原版行为、
     且**调用点在正确的位置**（顺序敏感的一律 `body.index()` 比大小，不是比存在）。
  B. **强化形态类**：`extends` 正确的父类、`image` 指向正确的常量、覆写正确的钩子、
     关键数值常量、内嵌 CraftRecipe 的 acceptedArtifact。
  C. **炼金配方**：通用基类 ArtifactEnhanceRecipe 的常量与「严格不接受已强化品」、
     Recipe.twoIngredientRecipes 登记、usableInRecipe 放行 Artifact、QuickRecipe case 8。
  D. **联动接线**：Bleeding→血宴累积、Char→ThirstBloodBarrier、渴望护盾衰减回血。
  E. **文本**：16 键 × zh/en（非空 / `_` 成对 / 无 U+FFFD / 无 `/n/` / 占位符数量），
     外加 5 个中文名硬断言。

用法:
    python _chk/verify_artifact_enhance.py
    python _chk/verify_artifact_enhance.py --selftest   # 反例自测（必跑）

写断言的铁律（见 skill egopd-source-verify）：
  ① 只认稳定概念名（物品中文名、类名、常量名），不认会被改写的措辞；
  ② 每条判据都要反例自测；
  ③ 判「某方法行为」时先按大括号配平取方法体、**再剥注释**（注释里常引用被修的原文，会喂假绿）；
  ④ 顺序敏感的 bug 必须比 `index()` 位置。
"""
import os
import re
import sys

ROOT = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
ART = ROOT + "/items/artifacts"
ZH = "core/src/main/assets/messages/items/items_zh.properties"
EN = "core/src/main/assets/messages/items/items.properties"

PASS, FAIL = [], []


def ok(msg):
    PASS.append(msg)
    print("  OK    " + msg)


def bad(msg):
    FAIL.append(msg)
    print("  FAIL  " + msg)


def check(cond, msg, counterexample=None):
    """cond 为真 → OK；否则 FAIL（可附反例说明）。"""
    if cond:
        ok(msg)
    else:
        bad(msg + ("  ← %s" % counterexample if counterexample else ""))


def read(path):
    return open(path, encoding="utf-8").read()


# --------------------------------------------------------------------------
# 源码工具（模板来自 skill 铁律 3/4）
# --------------------------------------------------------------------------
def strip_comments(src):
    """单趟扫描剥注释；字符串原样保留。

    不能写成「先正则去 /*...*/ 再去 //」：本仓注释里有 markdown 强调 `//**文字**`，
    其中的 `/*` 会被当成块注释开头，一路吞到文件末尾（实测 Char.java 因此产生 9 个假阳性）。
    """
    out, i, n = [], 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n:
            if src[i + 1] == "/":
                j = src.find("\n", i)
                i = n if j == -1 else j
                continue
            if src[i + 1] == "*":
                j = src.find("*/", i + 2)
                i = n if j == -1 else j + 2
                continue
        if src.startswith('"""', i):
            j = src.find('"""', i + 3)
            i = n if j == -1 else j + 3
            continue
        if c in ('"', "'"):
            q, j = c, i + 1
            while j < n:
                if src[j] == "\\":
                    j += 2
                    continue
                if src[j] == q:
                    j += 1
                    break
                if src[j] == "\n":
                    break
                j += 1
            out.append(src[i:j])
            i = j
            continue
        out.append(c)
        i += 1
    return "".join(out)


def body_of(src, sig):
    """按大括号配平取方法体。sig 建议带修饰符（只给方法名会撞上更早的同名调用）。"""
    m = re.search(sig, src)
    if not m:
        return None
    i = src.find("{", m.end() - 1)
    if i == -1:
        return None
    depth, j = 0, i
    while j < len(src):
        if src[j] == "{":
            depth += 1
        elif src[j] == "}":
            depth -= 1
            if depth == 0:
                return src[i:j + 1]
        j += 1
    return None


def code_of(src, sig):
    """取方法体并剥注释（铁律 4：只做 body_of 不剥注释会因注释里的示例文本判成假绿）。"""
    b = body_of(src, sig)
    return strip_comments(b) if b else None


def static_body(src, decl_sig):
    """取方法体但**保留注释**（用于断言「注释里必须点名某个坑」这类说明性要求）。"""
    return body_of(src, decl_sig)


# --------------------------------------------------------------------------
# A. 父类钩子
# --------------------------------------------------------------------------
def part_a():
    print("\n=== A. 父类钩子（存在 + 默认行为 + 调用点位置）===")

    # --- ChaliceOfBlood：extraPrickHP / mitigatePrickDamage / updateImage(int) ---
    p = ART + "/ChaliceOfBlood.java"
    s = read(p)

    b = code_of(s, r"protected\s+int\s+extraPrickHP\s*\(\s*Hero\s+\w+\s*\)\s*\{")
    check(b is not None and re.search(r"return\s+0\s*;", b),
          "ChaliceOfBlood.extraPrickHP 存在且默认返回 0（＝原版死亡概率不变）",
          "钩子缺失或默认值不是 0")

    b = code_of(s, r"protected\s+int\s+mitigatePrickDamage\s*\(\s*Hero\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    check(b is not None and re.search(r"return\s+damage\s*;", b),
          "ChaliceOfBlood.mitigatePrickDamage 存在且默认原样返回 damage")

    b = code_of(s, r"protected\s+void\s+updateImage\s*\(\s*int\s+\w+\s*\)\s*\{")
    check(b is not None and "ARTIFACT_CHALICE2" in b and "ARTIFACT_CHALICE3" in b,
          "ChaliceOfBlood.updateImage(int) 存在且仍是原版三档图")

    # 顺序：mitigatePrickDamage 必须在「至少 1 点」保底之后（否则永远削不到 0）
    pb = code_of(s, r"private\s+void\s+prick\s*\(\s*Hero\s+\w+\s*\)\s*\{")
    if pb is None:
        bad("ChaliceOfBlood.prick() 取不到方法体")
    else:
        i_floor = pb.find("damage = 1;")
        i_mit = pb.find("mitigatePrickDamage(")
        check(i_floor != -1 and i_mit != -1 and i_floor < i_mit,
              "prick()：mitigatePrickDamage 在「至少 1 点」保底**之后**调用",
              "两段 code 都在，只比存在是空测；必须比 index")
        check("prick_mitigated" in pb, "prick() 统一播报 prick_mitigated（减免提示的唯一出口）")

    # updateImage 的两个调用点
    check(s.count("updateImage(") >= 3, "ChaliceOfBlood 的 upgrade/restoreFromBundle 均改走 updateImage()")

    # --- HornOfPlenty：updateImage() 抽虚方法 + 四处调用点 ---
    p = ART + "/HornOfPlenty.java"
    s = read(p)
    b = code_of(s, r"protected\s+void\s+updateImage\s*\(\s*\)\s*\{")
    check(b is not None and b.count("ARTIFACT_HORN") >= 4,
          "HornOfPlenty.updateImage() 存在且含原版四档图")
    check(len(re.findall(r"\bupdateImage\(\)", strip_comments(s))) >= 5,
          "HornOfPlenty 四处硬编码已全部改为 updateImage()（1 处定义 + 4 处调用）")
    check(re.search(r"protected\s+int\s+storedFoodEnergy", s) is not None,
          "HornOfPlenty.storedFoodEnergy 提升为 protected（供强化形态搬运）")

    # --- EtherealChains：onEnemyPulled ---
    p = ART + "/EtherealChains.java"
    s = read(p)
    b = code_of(s, r"protected\s+void\s+onEnemyPulled\s*\(\s*Hero\s+\w+\s*,\s*Char\s+\w+\s*\)\s*\{")
    check(b is not None and len(strip_comments(b)) < 30,
          "EtherealChains.onEnemyPulled 存在且默认空实现")
    cb = code_of(s, r"private\s+void\s+chainEnemy\s*\(")
    if cb is None:
        cb = code_of(s, r"void\s+chainEnemy\s*\(")
    if cb is None:
        bad("EtherealChains.chainEnemy 取不到方法体")
    else:
        check("onEnemyPulled(" in cb, "chainEnemy 的 Pushing 回调里调用了 onEnemyPulled")
        check(cb.find("artifactProc(") < cb.find("onEnemyPulled("),
              "onEnemyPulled 在 artifactProc **之后**（附加伤害先结算）")

    # --- TalismanOfForesight：两个钩子 + 调用点 ---
    p = ART + "/TalismanOfForesight.java"
    s = read(p)
    b = code_of(s, r"protected\s+int\s+extraTrapExp\s*\(\s*int\s+\w+\s*\)\s*\{")
    check(b is not None and "return 0;" in b, "TalismanOfForesight.extraTrapExp 存在且默认 0")
    b = code_of(s, r"protected\s+void\s+onEnemyScryed\s*\(\s*Char\s+\w+\s*,\s*float\s+\w+\s*\)\s*\{")
    check(b is not None and len(strip_comments(b)) < 30,
          "TalismanOfForesight.onEnemyScryed 存在且默认空实现")
    sc = static_body(s, r"public\s+CellSelector\.Listener\s+scry\s*=")
    if sc is None:
        bad("TalismanOfForesight.scry 字段初始化取不到（父类是匿名字段初始化，钩子才是正解）")
    else:
        c = strip_comments(sc)
        check("extraTrapExp(" in c and "onEnemyScryed(" in c, "scry 内两处钩子均已接入")
        check(c.find("TERRAIN.SECRET_TRAP") != -1 or "SECRET_TRAP" in c
              and c.find("extraTrapExp(") > c.find("SECRET_TRAP"),
              "extraTrapExp 只在 SECRET_TRAP 分支内调用（隐藏门不受影响）")
        check(c.find("artifactProc(") < c.find("onEnemyScryed("),
              "onEnemyScryed 在 artifactProc 之后")

    # --- UnstableSpellbook：scrolls 提升为 protected ---
    p = ART + "/UnstableSpellbook.java"
    s = read(p)
    check(re.search(r"protected\s+final\s+ArrayList<Class>\s+scrolls", s) is not None,
          "UnstableSpellbook.scrolls 提升为 protected（供强化形态搬运随机卷轴清单）")


# --------------------------------------------------------------------------
# B. 强化形态类
# --------------------------------------------------------------------------
SPEC = [
    dict(cls="BloodFeastChalice", base="ChaliceOfBlood", img="ARTIFACT_BLOOD_FEAST_CHALICE1",
         hooks=["extraPrickHP", "mitigatePrickDamage", "updateImage"],
         consts=[], recipe_base="ChaliceOfBlood"),
    dict(cls="LifelongStew", base="HornOfPlenty", img="ARTIFACT_LIFELONG_STEW1",
         hooks=["updateImage", "doEatEffect"], consts=[("HEAL_PER_CHARGE", "3")],
         recipe_base="HornOfPlenty"),
    dict(cls="ChainOfOthers", base="EtherealChains", img="ARTIFACT_CHAIN_OF_OTHERS",
         hooks=["onEnemyPulled"], consts=[("SELF_DURATION", "3f"), ("ENEMY_DURATION", "20f")],
         recipe_base="EtherealChains"),
    dict(cls="ChapterNineVerseTwo", base="UnstableSpellbook", img="ARTIFACT_CHAPTER_NINE_VERSE_TWO",
         hooks=["doReadEffect"], consts=[("EXTRA_CRIPPLE", "5f")],
         recipe_base="UnstableSpellbook"),
    dict(cls="ApproachingDay", base="TalismanOfForesight", img="ARTIFACT_APPROACHING_DAY",
         hooks=["extraTrapExp", "onEnemyScryed"], consts=[("TRAP_BONUS_EXP", "10"),
                                                          ("TERROR_DURATION", "10f")],
         recipe_base="TalismanOfForesight"),
]


def part_b():
    print("\n=== B. 强化形态类（继承 / 图标 / 钩子 / 常量 / 配方）===")
    for spec in SPEC:
        cls, base = spec["cls"], spec["base"]
        p = "%s/%s.java" % (ART, cls)
        if not os.path.exists(p):
            bad("%s.java 不存在" % cls)
            continue
        s = read(p)
        check(re.search(r"public\s+class\s+%s\s+extends\s+%s\b" % (cls, base), s) is not None,
              "%s extends %s" % (cls, base))
        check(re.search(r"image\s*=\s*ItemSpriteSheet\.%s\s*;" % spec["img"], s) is not None,
              "%s 的 image = %s" % (cls, spec["img"]))
        for h in spec["hooks"]:
            check(re.search(r"@Override\s+[^;{]*\b%s\s*\(" % h, s) is not None,
                  "%s 覆写了 %s" % (cls, h))
        for name, val in spec["consts"]:
            check(re.search(r"static\s+final\s+\w+\s+%s\s*=\s*%s\s*;" % (name, re.escape(val)), s)
                  is not None,
                  "%s.%s = %s" % (cls, name, val))
        # 内嵌配方
        rb = code_of(s, r"protected\s+Class<%s>\s+acceptedArtifact\s*\(\s*\)\s*\{" % base)
        check(rb is not None and ("%s.class" % base) in rb,
              "%s.CraftRecipe.acceptedArtifact() → %s.class" % (cls, base))

    # —— 逐类的关键语义断言 ——

    # 1) 血宴圣杯
    p = ART + "/BloodFeastChalice.java"
    s = read(p)
    b = code_of(s, r"protected\s+int\s+extraPrickHP\s*\(\s*Hero\s+\w+\s*\)\s*\{")
    check(b is not None and "bloodFeast" in b and "maxPrickDmg()" in b,
          "血宴计入死亡概率窗口（min(血宴, 最大伤害)）")

    b = code_of(s, r"protected\s+int\s+mitigatePrickDamage\s*\(\s*Hero\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    check(b is not None and "bloodFeast" in b and "return damage - used" in b.replace(" ; ", " - ").replace(";", ""),
          "血宴按 1:1 抵消升级伤害，可削到 0")
    check(b is not None and "GLog" not in b and "prick_mitigated" not in b,
          "血宴**不自己播报**减免提示（父类 prick() 是唯一出口，否则弹两条重复消息）",
          "两处都打日志 = 玩家会看到两条一样的消息")

    check(re.search(r"static\s+boolean\s+linkActive\s*\(\s*Char\s+\w+\s*\)", s) is not None,
          "BloodFeastChalice.linkActive(Char) 存在（供渴望护盾调用）")
    check(re.search(r"static\s+void\s+onBleedTick\s*\(\s*Char\s+\w+\s*,\s*int\s+\w+\s*\)", s) is not None,
          "BloodFeastChalice.onBleedTick(Char,int) 存在")
    ob = code_of(s, r"public\s+static\s+void\s+onBleedTick\s*\(\s*Char\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    check(ob is not None and "Alignment.ENEMY" in ob and "Dungeon.hero" in ob,
          "onBleedTick 只累计「英雄自身 + 敌人」的流血（友方/中立不计）")
    check("BLOOD_FEAST" in s and "getInt(" in s, "血宴按字段存档（BLOOD_FEAST / getInt 防御旧档）")

    # 2) 一生炖菜
    p = ART + "/LifelongStew.java"
    s = read(p)
    ui = code_of(s, r"protected\s+void\s+updateImage\s*\(\s*\)\s*\{")
    check(ui is not None and all(("ARTIFACT_LIFELONG_STEW%d" % i) in ui for i in (1, 2, 3, 4)),
          "一生炖菜 updateImage() 覆盖四档（1~4 帧都在）")
    de = code_of(s, r"public\s+void\s+doEatEffect\s*\(\s*Hero\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    check(de is not None and "super.doEatEffect(" in de, "doEatEffect 先走父类原逻辑（饱食度/扣充能/耗时）")
    check(de is not None and "HEAL_PER_CHARGE" in de and "chargesToUse" in de,
          "doEatEffect 按「消耗的充能数 × 3」回血")
    tr = code_of(s, r"protected\s+void\s+transferState\s*\(\s*HornOfPlenty\s+\w+\s*,\s*LifelongStew\s+\w+\s*\)\s*\{")
    check(tr is not None and "storedFoodEnergy" in tr,
          "合成时搬运 storedFoodEnergy（否则玩家喂进去的食物会蒸发）")

    # 3) 他人之锁
    p = ART + "/ChainOfOthers.java"
    s = read(p)
    hp = code_of(s, r"protected\s+void\s+onEnemyPulled\s*\(\s*Hero\s+\w+\s*,\s*Char\s+\w+\s*\)\s*\{")
    # 逐条点名「谁吃到哪个 buff」——别数 Cripple.class 的总出现次数：
    # 每个 buff 都带一处 isImmune 判定，总数是 4 而不是 2（这条断言自己曾写错一次）
    quad = 0
    for who in ("hero", "enemy"):
        for buf in ("Cripple", "Weakness"):
            if hp and re.search(r"Buff\.affect\(\s*%s\s*,\s*%s\.class" % (who, buf), hp):
                quad += 1
    check(quad == 4,
          "他人之锁对敌我双方各施加残废 + 虚弱（hero/enemy × Cripple/Weakness 共 4 处）",
          "实际命中 %d 处" % quad)
    check(hp is not None and hp.find("hero") < hp.find("enemy"),
          "先结算自己再结算敌人（需求：同时施加）")
    check(hp is not None and "isAlive()" in hp,
          "敌人可能已被 artifactProc 打死 ⇒ 施加前先判存活")
    check(hp is not None and "SELF_DURATION" in hp and "ENEMY_DURATION" in hp,
          "自己 3 回合 / 敌人 20 回合")

    # 4) 9章2节
    p = ART + "/ChapterNineVerseTwo.java"
    s = read(p)
    dr = code_of(s, r"public\s+void\s+doReadEffect\s*\(\s*Hero\s+\w+\s*\)\s*\{")
    check(dr is not None and "super.doReadEffect(" in dr and "igniteAllInView(" in dr,
          "doReadEffect 先走父类掷卷轴，再点燃视野内敌人")
    ig = code_of(s, r"protected\s+void\s+igniteAllInView\s*\(\s*\)\s*\{")
    if ig is None:
        bad("igniteAllInView 取不到方法体")
    else:
        # 顺序陷阱：必须先在点燃前记录「本来就烧着」
        check(ig.find("wasBurning") != -1 and ig.find("wasBurning") < ig.find("reignite"),
              "igniteAllInView：「本来就烧着」在点燃**之前**判定",
              "边烧边判 ⇒ 本轮先被点着的敌人也满足「已燃烧」，人人白吃一次残废")
        check("heroFOV" in ig, "点燃范围用 heroFOV（含遮挡/隐形过滤，与其它「视野内」口径一致）")
        check("Alignment.ENEMY" in ig, "只点燃敌方单位")
        check("toArray" in ig or "new ArrayList" in ig, "先取快照再改，避免并发修改")
    tr = code_of(s, r"protected\s+void\s+transferState\s*\(\s*UnstableSpellbook\s+\w+\s*,\s*ChapterNineVerseTwo\s+\w+\s*\)\s*\{")
    check(tr is not None and "scrolls" in tr and "clear()" in tr and "addAll(" in tr,
          "合成时搬运随机卷轴清单（先 clear 再 addAll）")

    # 5) 迫近之日
    p = ART + "/ApproachingDay.java"
    s = read(p)
    et = code_of(s, r"protected\s+int\s+extraTrapExp\s*\(\s*int\s+\w+\s*\)\s*\{")
    check(et is not None and "TRAP_BONUS_EXP" in et, "extraTrapExp 返回 TRAP_BONUS_EXP（隐藏门不加码）")
    oe = code_of(s, r"protected\s+void\s+onEnemyScryed\s*\(\s*Char\s+\w+\s*,\s*float\s+\w+\s*\)\s*\{")
    check(oe is not None and "Terror.class" in oe and "TERROR_DURATION" in oe,
          "onEnemyScryed 施加 Terror（10 回合）")
    check(oe is not None and "object" in oe and "Dungeon.hero.id()" in oe,
          "Terror.object 记来源（与恐惧卷轴/符石同口径）")
    check(oe is not None and "isAlive" in oe and "isImmune" in oe,
          "施加前判存活与免疫")


# --------------------------------------------------------------------------
# C. 炼金配方
# --------------------------------------------------------------------------
def part_c():
    print("\n=== C. 炼金配方 ===")
    p = ART + "/ArtifactEnhanceRecipe.java"
    s = read(p)
    # 2026-09-24：用户要求「原版神器加强统一 60 → 30 脑啡肽」
    check(re.search(r"ENKEPHALIN_NEEDED\s*=\s*30\s*;", s) is not None, "通用基类：脑啡肽 30（2026-09-24 由 60 下调）")
    check(re.search(r"ENKEPHALIN_NEEDED\s*=\s*60\s*;", s) is None, "旧值 60 已彻底退场（不许残留）")
    check(re.search(r"ENERGY_COST\s*=\s*12\s*;", s) is not None, "通用基类：能量 12")

    ti = code_of(s, r"public\s+boolean\s+testIngredients\s*\(")
    check(ti is not None and "getClass() == acceptedArtifact()" in ti,
          "testIngredients 用 getClass() == acceptedArtifact() 严格比较（已强化品不再被接受）",
          "用 instanceof 会让强化品被反复接受，玩家白扔 30 脑啡肽")
    check(ti is not None and "ENKEPHALIN_NEEDED" in ti, "testIngredients 校验脑啡肽数量门槛")

    br = code_of(s, r"public\s+Item\s+brew\s*\(")
    check(br is not None and "quantity(" in br and "transferState(" in br and "createEnhanced(" in br,
          "brew 消耗原料并调用 transferState（等级/充能不被清零）")

    ct = code_of(s, r"public\s+int\s+cost\s*\(")
    check(ct is not None and "ENERGY_COST" in ct, "cost() 返回 ENERGY_COST")
    check(s.find("sampleOutput") != -1, "实现了 sampleOutput（炼金指南预览）")

    # Recipe 注册 + 入釜放行
    rp = ROOT + "/items/Recipe.java"
    rs = read(rp)
    rc = strip_comments(rs)
    for spec in SPEC:
        check(("%s.CraftRecipe()" % spec["cls"]) in rc,
              "twoIngredientRecipes 已登记 %s.CraftRecipe" % spec["cls"])
    check(re.search(r"item\s+instanceof\s+Artifact\b", rc) is not None,
          "usableInRecipe 放行 Artifact（否则神器被挡在炼金釜外 ⇒ 配方永不匹配、静默失效）")
    m = re.search(r"ArrayList<Recipe>\s+twoIngredientRecipes", rs)
    if m:
        blk = body_of(rs[m.start():], r"ArrayList<Recipe>\s+twoIngredientRecipes\s*=\s*new\s+ArrayList")
        n = len(re.findall(r"%s\.CraftRecipe\(\)" % "|".join(s["cls"] for s in SPEC),
                           blk or ""))
        check(n == 5, "强化配方在 twoIngredientRecipes 里恰 5 条（实际 %d）" % n)

    # QuickRecipe case 8（Spells 页）
    qp = ROOT + "/ui/QuickRecipe.java"
    qs = read(qp)
    qc = strip_comments(qs)
    c8 = qc[qc.find("case 8:"):]
    n = len(re.findall(r"new\s+(%s)\s*\(\s*\)" % "|".join(s["cls"] for s in SPEC), c8))
    check(n == 5, "QuickRecipe case 8（炼金指南 Spells 页）含 5 条强化配方（实际 %d）" % n)
    for spec in SPEC:
        check(("new %s.CraftRecipe()" % spec["cls"]) in c8,
              "case 8 含 %s.CraftRecipe" % spec["cls"])
    check(c8.count("new Enkephalin().quantity(30)") >= 5,
          "case 8 的预览原料为「原神器 + 30 脑啡肽」×5（2026-09-24 由 60 下调）")
    check("quantity(60)" not in c8, "case 8 无旧值 60 残留")
    # 页签索引：Spells 必须是第 8 页（case 8）
    dp = ROOT + "/journal/Document.java"
    ds = read(dp)
    order = re.findall(r'ALCHEMY_GUIDE\.pagesStates\.put\("([A-Za-z_]+)"', ds)
    want = ["Potions", "Stones", "Energy_Food", "Exotic_Potions", "Exotic_Scrolls",
            "Bombs", "Weapons", "Brews_Elixirs", "Spells"]
    check(order == want,
          "炼金指南页序 %s ⇒ case 8 = Spells" % order,
          "实际 %s" % order)


# --------------------------------------------------------------------------
# D. 联动接线
# --------------------------------------------------------------------------
def part_d():
    print("\n=== D. 联动接线（流血 → 血宴 → 渴望护盾回血）===")
    p = ROOT + "/actors/buffs/Bleeding.java"
    s = read(p)
    ab = code_of(s, r"public\s+boolean\s+act\s*\(\s*\)\s*\{")
    if ab is None:
        bad("Bleeding.act() 取不到方法体")
    else:
        check("BloodFeastChalice.onBleedTick(" in ab, "Bleeding.act() 已接入 onBleedTick")
        check(ab.find("target.damage(") < ab.find("onBleedTick("),
              "onBleedTick 在 target.damage() **之后**（先结算这一跳再计入血宴）",
              "先计入会拿到未结算的伤害口径")

    p = ROOT + "/actors/buffs/ThirstBloodBarrier.java"
    s = read(p)
    check(re.search(r"class\s+ThirstBloodBarrier\s+extends\s+Barrier\b", s) is not None,
          "ThirstBloodBarrier extends Barrier（只多一件事：衰减时通知血宴圣杯）")
    check(re.search(r"HEAL_DIVISOR\s*=\s*5\s*;", s) is not None, "回血比例 1/5（HEAL_DIVISOR = 5）")
    ac = code_of(s, r"public\s+boolean\s+act\s*\(\s*\)\s*\{")
    if ac is None:
        bad("ThirstBloodBarrier.act() 取不到方法体")
    else:
        check("BloodFeastChalice.linkActive(" in ac, "act() 询问 linkActive（未装备圣杯则完全等价原版）")
        check(ac.find("Char carrier = target") < ac.find("super.act()"),
              "act() 在 super.act() **之前**存下 target（护盾耗尽会自我 detach 把 target 置空）",
              "先 super.act() 再读 target ⇒ 拿不到对象，回血整段失效")
        check("healPool" in ac, "零头池 healPool：1/5 取整否则每次都被抹成 0")
    check("HEAL_POOL" in s, "healPool 按字段存档")

    p = ROOT + "/actors/Char.java"
    s = read(p)
    m = re.search(r"Buff\.affect\(\s*this\s*,\s*ThirstBloodBarrier\.class\s*\)", s)
    check(m is not None, "Char 的「渴望把流血转成护盾」路径已改挂 ThirstBloodBarrier")
    check(re.search(r"Buff\.affect\(\s*this\s*,\s*Barrier\.class\s*\)", s) is None,
          "旧的 Buff.affect(this, Barrier.class) 已不存在（只剩一条路径，不分两条）")

    # —— 已知代价：子类与普通 Barrier 会分成两份。这三条把「前提」钉住，
    #    免得后人以为是 bug 去「修」（真正该修的话要先读 ThirstBloodBarrier 的类注释）——
    bb = code_of(s, r"public\s+synchronized\s+<T\s+extends\s+Buff>\s+T\s+buff\s*\(")
    check(bb is not None and "getClass() == c" in bb,
          "前提：Char.buff(Class) 是**精确类匹配**（getClass()==），不是 isInstance",
          "若上游改成 isInstance，本文档的「已知代价」整段就不再成立，需要复核")
    pr = code_of(read(ROOT + "/actors/buffs/ShieldBuff.java"), r"static\s+int\s+processDamage\s*\(")
    check(pr is not None and "buffs(ShieldBuff.class)" in pr,
          "伤害吸收不受影响：processDamage 走 buffs(ShieldBuff.class)（isInstance，两类一起进伤害池）")
    jd = read(ROOT + "/actors/buffs/ThirstBloodBarrier.java")
    check("已知代价" in jd,
          "ThirstBloodBarrier 类注释已如实记录「分成两份 / 两个图标 / buff(Barrier.class) 看不见」的取舍")


# --------------------------------------------------------------------------
# E. 文本
# --------------------------------------------------------------------------
TEXTS = [
    ("bloodfeastchalice", ["name", "desc", "desc_enhanced", "prick_mitigated"], "血宴圣杯",
     {"desc_enhanced": 1, "prick_mitigated": 1}),
    ("lifelongstew", ["name", "desc", "desc_enhanced"], "一生炖菜", {}),
    ("chainofothers", ["name", "desc", "desc_enhanced"], "他人之锁", {}),
    ("chapternineversetwo", ["name", "desc", "desc_enhanced"], "9章2节", {}),
    ("approachingday", ["name", "desc", "desc_enhanced"], "迫近之日", {}),
]


def load_props(path):
    """只取「键=值」行（续行已按 \n 字面量写，不会真断行）。"""
    d = {}
    for line in read(path).splitlines():
        if not line or line.lstrip().startswith("#") or line.lstrip().startswith("!"):
            continue
        if "=" not in line:
            d.setdefault("__BROKEN__", []).append(line)
            continue
        k, v = line.split("=", 1)
        d[k.strip()] = v
    return d


def part_e():
    print("\n=== E. 文本（zh 权威 / en 同样必须齐全）===")
    lst = load_props(ZH)
    ens = load_props(EN)

    check("__BROKEN__" not in lst, "items_zh.properties 无「缺 = 的断行」（续行没被落成真换行）")
    check("__BROKEN__" not in ens, "items.properties 无「缺 = 的断行」")

    total = 0
    for slug, keys, zh_name, params in TEXTS:
        for key in keys:
            full = "items.artifacts.%s.%s" % (slug, key)
            total += 1
            for label, d in (("zh", lst), ("en", ens)):
                v = d.get(full)
                if v is None or v.strip() == "":
                    bad("%s 缺失或为空：%s" % (label, full))
                    continue
                if "\ufffd" in v:
                    bad("%s 含 U+FFFD（编码坏了）：%s" % (label, full))
                if "/n/" in v:
                    bad("%s 含误写的 /n/（不会换行，会原样显示）：%s" % (label, full))
                if v.count("_") % 2 != 0:
                    bad("%s 的 _ 强调标记不成对（奇数个）：%s" % (label, full))
                want = params.get(key, 0)
                got = len(re.findall(r"%(?:\d+\$)?[ds]", v))
                if got != want:
                    bad("%s 的 %s 占位符 %d 个，Messages.get 只传 %d 个实参 "
                        "（数量不符 ⇒ 整串回退原文）" % (label, full, got, want))
        # 中文名硬断言（稳定概念名，铁律 1）
        if lst.get("items.artifacts.%s.name" % slug) != zh_name:
            bad("中文名不是 %s，实际 %r" % (zh_name, lst.get("items.artifacts.%s.name" % slug)))
    ok("16 键 × 2 语言齐备、非空、无 U+FFFD、无 /n/、_ 成对、占位符数量与实参一致")
    print("   （共断言 %d 个键 × 2 语言）" % total)

    # 新增效果的文本要点名机制关键词（只认稳定概念名/数值）
    must = {
        "items.artifacts.bloodfeastchalice.desc_enhanced": ["1:1", "1/5"],
        "items.artifacts.lifelongstew.desc_enhanced": ["3"],
        "items.artifacts.chainofothers.desc_enhanced": ["3", "20"],
        "items.artifacts.chapternineversetwo.desc_enhanced": ["5"],
        "items.artifacts.approachingday.desc_enhanced": ["10"],
    }
    for k, words in must.items():
        v = lst.get(k, "")
        miss = [w for w in words if w not in v]
        check(not miss, "%s 点明数值 %s" % (k.split(".")[-2] + "." + k.split(".")[-1], words),
              "缺 %s" % miss)


# --------------------------------------------------------------------------
# 反例自测
# --------------------------------------------------------------------------
def selftest():
    print("=== 反例自测（证明判据不恒真）===")
    problems = []

    # 1) body_of 必须取到正确方法体（而不是撞上更早的同名调用）
    fake = ("class T{ void a(){ helper(); } "
            "void helper(){ if(x){ y(); } } "
            "void z(){ helper(); } void helper(){ int q=1; } }")
    b = body_of(fake, r"void\s+helper\s*\(\s*\)\s*\{")
    if b is None:
        problems.append("body_of 取不到方法体")
    elif "if(x)" not in b:
        problems.append("body_of 撞上了更早的调用而不是声明（取回风马牛不相及的方法体）")

    # 2) 顺序判据真的会判 FAIL
    good = "void f(){ damage = 1; damage = mitigatePrickDamage(h, damage); }"
    weak = "void f(){ damage = mitigatePrickDamage(h, damage); damage = 1; }"
    if not (good.find("damage = 1;") < good.find("mitigatePrickDamage(")):
        problems.append("顺序判据在正例上就失败")
    if weak.find("damage = 1;") < weak.find("mitigatePrickDamage("):
        problems.append("顺序判据对反例（钩子在保底之前）没判出问题")

    # 3) 注释里的原文不能喂假绿：剥注释后才搜
    commented = "void f(){ // 这里曾经写 GLog.p(prick_mitigated) 但现在不打了\n }"
    if "prick_mitigated" in strip_comments(commented):
        problems.append("strip_comments 没剥掉注释里的文本 ⇒ 「不得再出现」类断言会假绿")

    # 4) `//**强调**` 里的 /* 不能吞掉后面的代码
    tricky = "import a.B;\n//**粗体**说明\nclass T{ B b; }"
    if "B b;" not in strip_comments(tricky):
        problems.append("`//**` 里的 /* 把后面的代码吞了（旧写法复发）")

    # 5) 文本判据：占位符数量不符要能报出
    if len(re.findall(r"%(?:\d+\$)?[ds]", "血宴吞下 _%d_ 点")) != 1:
        problems.append("占位符计数判据错了")
    if len(re.findall(r"%(?:\d+\$)?[ds]", "%d 和 %d")) != 2:
        problems.append("占位符计数判据（两个）错了")

    for p in problems:
        print("  自测失败：" + p)
    if not problems:
        print("  自测通过：body_of 定位正确 / 顺序判据正反例可分 / 剥注释有效 / "
              "//** 不吞代码 / 占位符计数正确")
    return 0 if not problems else 1


def main():
    if "--selftest" in sys.argv:
        return selftest()

    part_a()
    part_b()
    part_c()
    part_d()
    part_e()

    print("\n" + "=" * 60)
    print("通过 %d 条，失败 %d 条" % (len(PASS), len(FAIL)))
    for f in FAIL:
        print("  FAIL  " + f)
    print("ALL PASS" if not FAIL else "FAIL")
    return 0 if not FAIL else 1


if __name__ == "__main__":
    sys.exit(main())
