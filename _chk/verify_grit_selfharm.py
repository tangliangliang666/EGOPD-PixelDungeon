# -*- coding: utf-8 -*-
"""
回归核验（2026-09-20）：解放按天赋取上限 + 「自伤换成长」击穿「咬紧牙关」免死。

覆盖两条链，各跨多个文件，任何一处单独改动都会静默失效：

① 解放叠加上限（Release.gainStack）
   - items/…/buffs/Release.java   MAX_STACKS=9 / MAX_STACKS_1=6 + growthCap(Hero)
   - gainStack() 必须走 growthCap（旧写法 `stacks < MAX_STACKS` ⇒ +1 也能叠到 9 层，与天赋文本不符）
   - actors_zh.properties 的 blade_release.desc 必须仍写「6层 / 9层」（文本与常量口径一致）

② 自伤换成长击穿免死（GritTeethBuff）
   - ChaliceOfBlood / WristSlit  必须 implements SelfHarmCost、且 hero.damage(dmg, this) 把 this 当 src
   - GritTeethBuff.isSelfHarm    判据只有 `src instanceof SelfHarmCost`（不许另开 instanceof 名单）
   - GritTeethBuff.checkBypass   必须 `!isRay(src) && !isSelfHarm(src)` 才短路（漏掉一半 ⇒ 另一半失效）
   - Char.damage                 调用点必须在 `if (HP < 0) HP = 0;` 之后、`if (!isAlive())` 之前
   - 旧名 checkRayBypass 不得残留
   - 中英文本（技能 short_desc/desc + buff desc）必须点名「血祭 / 割腕」这类自伤换成长

反例自测：把每处修复分别「改坏」，确认对应断言确实变红（防恒真空测）。

用法：python _chk/verify_grit_selfharm.py [--list]
退出码 0 = ALL PASS。
"""

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
MSG = "core/src/main/assets/messages/actors/"

F_RELEASE = PKG + "actors/buffs/Release.java"
F_GRIT = PKG + "actors/buffs/GritTeethBuff.java"
F_CHAR = PKG + "actors/Char.java"
F_SELFHARM = PKG + "items/SelfHarmCost.java"
F_CHALICE = PKG + "items/artifacts/ChaliceOfBlood.java"
F_WRIST = PKG + "items/weapon/melee/WristSlit.java"
F_WNDDEBUG = PKG + "windows/WndDebug.java"
F_ZH = MSG + "actors_zh.properties"
F_EN = MSG + "actors.properties"

PASS = []
FAIL = []


def read(path):
    return io.open(os.path.join(ROOT, path), encoding="utf-8", newline="").read().replace("\r\n", "\n")


def ok(cond, msg):
    (PASS if cond else FAIL).append(msg)
    return bool(cond)


def code_of(src):
    """去掉注释与字符串字面量，避免注释里的示例代码造成「假绿/假红」。"""
    out = []
    i, n = 0, len(src)
    while i < n:
        c = src[i]
        if c == "/" and i + 1 < n and src[i + 1] == "/":
            j = src.find("\n", i)
            j = n if j < 0 else j
            out.append(" " * (j - i))
            i = j
        elif c == "/" and i + 1 < n and src[i + 1] == "*":
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


def body_of(src, name, sig=None):
    c = code_of(src)
    pat = sig if sig else r"\b" + re.escape(name) + r"\s*\("
    m = re.search(pat, c)
    if not m:
        return None
    i = c.find("{", m.end())
    if i < 0:
        return None
    depth = 0
    for j in range(i, len(c)):
        if c[j] == "{":
            depth += 1
        elif c[j] == "}":
            depth -= 1
            if depth == 0:
                return c[i:j + 1]
    return None


def sig_of(src, name):
    c = code_of(src)
    m = re.search(r"([\w<>\[\],\s\.]+?\b" + re.escape(name) + r"\s*\([^;{}]*?\))\s*\{", c)
    return m.group(1).strip() if m else ""


def prop(raw, key):
    """取 properties 里某个键的值（文件中的原始形态，字面量 \\n 保持为两字符）。"""
    for l in raw.split("\n"):
        if l.startswith(key + "="):
            return l[len(key) + 1:]
    return None


def broken_lines(raw):
    """真换行污染的产物：非空、非注释、又不含 = 的行（＝某个 entry 被断成两截）。"""
    return [l for l in raw.split("\n")
            if l.strip() and not l.lstrip().startswith("#") and "=" not in l]


# ==================================================================== 断言
def analyze(rel, grit, ch, sh, chal, wrist, zh, en, wd):
    del PASS[:], FAIL[:]

    rc, gc, cc = code_of(rel), code_of(grit), code_of(ch)
    zhc, enc = code_of(zh), code_of(en)

    # ---------- ① 解放叠加上限 ----------
    m9 = re.search(r"int\s+MAX_STACKS\s*=\s*(\d+)", rc)
    m6 = re.search(r"int\s+MAX_STACKS_1\s*=\s*(\d+)", rc)
    ok(m9 is not None and m9.group(1) == "9", "[Release] MAX_STACKS = 9（+2 上限）")
    ok(m6 is not None and m6.group(1) == "6", "[Release] MAX_STACKS_1 = 6（+1 上限）")

    gcap = body_of(rel, "growthCap")
    ok(gcap is not None, "[Release] 存在 growthCap(Hero)")
    ok(re.search(r"\bstatic\b", sig_of(rel, "growthCap") or ""),
       "[Release] growthCap 是 static")
    ok(gcap and re.search(r"pointsInTalent\(\s*Talent\.BLADE_RELEASE\s*\)", gcap),
       "[Release] growthCap 按 BLADE_RELEASE 的点数取上限")
    ok(gcap and re.search(r">=\s*2\s*\)\s*return\s+MAX_STACKS\s*;", gcap),
       "[Release] growthCap：+2 ⇒ MAX_STACKS(9)")
    ok(gcap and re.search(r">=\s*1\s*\)\s*return\s+MAX_STACKS_1\s*;", gcap),
       "[Release] growthCap：+1 ⇒ MAX_STACKS_1(6)")
    ok(gcap and re.search(r"return\s+0\s*;\s*\}\s*$", gcap.strip()) or
       (gcap and gcap.count("return 0;") >= 1),
       "[Release] growthCap：0 点 ⇒ 0（没这门天赋就不叠）")

    gs = body_of(rel, "gainStack")
    ok(gs is not None, "[Release] 找到 gainStack()")
    ok(gs and "growthCap(" in gs,
       "[Release] gainStack 按天赋点数取上限（走 growthCap）")
    ok(gs is not None and not re.search(r"stacks\s*<\s*MAX_STACKS\b", gs),
       "[Release] gainStack 已不再写死 stacks < MAX_STACKS")

    # 文本与常量口径一致
    bd = prop(zh, "actors.hero.talent.blade_release.desc")
    cap1 = int(m6.group(1)) if m6 else -1
    cap2 = int(m9.group(1)) if m9 else -1
    ok(bd is not None and ("%d层" % cap1) in bd.replace("_", ""),
       "[文本] blade_release.desc 的 +1 上限与 MAX_STACKS_1(%d) 一致" % cap1)
    ok(bd is not None and ("%d层" % cap2) in bd.replace("_", ""),
       "[文本] blade_release.desc 的 +2 上限与 MAX_STACKS(%d) 一致" % cap2)

    # 调试台仍能叠到 9 层（否则看粒子/层数显示的入口会被新上限锁死）
    dgs = body_of(rel, "debugGainStack")
    ok(dgs is not None, "[Release] 存在 debugGainStack()（调试台专用叠层入口）")
    ok(dgs is not None and "growthCap" not in dgs and re.search(r"stacks\s*<\s*MAX_STACKS\b", dgs),
       "[Release] debugGainStack 无视天赋上限（仍用 MAX_STACKS）")
    ok(re.search(r"release\.debugGainStack\(\)", code_of(wd)),
       "[WndDebug] 实验解放叠层走 debugGainStack（用 gainStack 会被天赋上限锁住）")
    ok(not re.search(r"release\.gainStack\(\)", code_of(wd)),
       "[WndDebug] 已不再直接调 gainStack")

    # ---------- ② 自伤换成长击穿免死 ----------
    ok("checkRayBypass" not in gc + cc,
       "[GritTeeth] 旧名 checkRayBypass 已不存在（改名 checkBypass）")

    cb = body_of(grit, "checkBypass")
    ok(cb is not None, "[GritTeeth] 存在 checkBypass(Char, Object)")
    ok(re.search(r"\bstatic\b", sig_of(grit, "checkBypass") or ""),
       "[GritTeeth] checkBypass 是 static")
    ok(cb and re.search(r"!\s*\(\s*ch\s+instanceof\s+Hero\s*\)\s*\)\s*return", cb),
       "[GritTeeth] checkBypass 只对英雄生效（廉价短路）")
    ok(cb and re.search(r"ch\.HP\s*>\s*0\s*\)\s*return", cb),
       "[GritTeeth] checkBypass 只在真的打到 0 血时判定（非致命一击不作废免死）")
    ok(cb and re.search(r"!\s*isRay\(\s*src\s*\)\s*&&\s*!\s*isSelfHarm\(\s*src\s*\)", cb),
       "[GritTeeth] checkBypass：射线与自伤换成长都算击穿来源")
    ok(cb and re.search(r"buff\.bypassed\s*=\s*true", cb),
       "[GritTeeth] checkBypass 置位 bypassed")
    ok(cb and not re.search(r"heroClass", cb), "[红线] checkBypass 不判职业")

    sh_body = body_of(grit, "isSelfHarm")
    ok(sh_body is not None, "[GritTeeth] 存在 isSelfHarm(Object)")
    ok(sh_body and re.search(r"src\s+instanceof\s+SelfHarmCost", sh_body),
       "[GritTeeth] isSelfHarm 判据是 SelfHarmCost（唯一取点，不许另开名单）")
    ok(sh_body and not re.search(r"heroClass", sh_body), "[红线] isSelfHarm 不判职业")

    ok('RAY_SOURCES.add( Eye.DeathGaze.class )' in code_of(grit),
       "[GritTeeth] 射线口径仍在（Eye.DeathGaze 未被误删）")

    # 两件道具都带标记、且把 this 当 src 传下去
    for nm, src, path in (("ChaliceOfBlood", chal, F_CHALICE), ("WristSlit", wrist, F_WRIST)):
        ok(re.search(r"class\s+" + nm + r"\b[^{]*implements\s+[^{]*\bSelfHarmCost\b", code_of(src)),
           "[%s] implements SelfHarmCost（漏掉 ⇒ 该道具仍可被免死刷满）" % nm)
        ok(re.search(r"hero\.damage\(\s*damage\s*,\s*this\s*\)", code_of(src)),
           "[%s] hero.damage(damage, this)：src 携带 SelfHarmCost 标记" % nm)

    # Char.damage 调用点：在 clamp 之后、死亡判定之前
    dmg_body = body_of(ch, "damage", sig=r"public\s+void\s+damage\s*\(\s*int\s+dmg\s*,\s*Object\s+src\s*\)")
    ok(dmg_body is not None, "[Char] 找到 damage(int, Object)")
    if dmg_body:
        i_clamp = dmg_body.find("if (HP < 0) HP = 0;")
        i_call = dmg_body.find("GritTeethBuff.checkBypass( this, src )")
        i_die = dmg_body.find("if (!isAlive())")
        ok(i_call >= 0, "[Char] damage 调用 GritTeethBuff.checkBypass( this, src )")
        ok(0 <= i_clamp < i_call,
           "[Char] 调用点在「HP 归零到下限」之后（否则判不出这一击是否致命）")
        ok(i_call >= 0 and 0 <= i_call < i_die,
           "[Char] 调用点在 if (!isAlive()) 死亡判定之前（否则免死来不及作废）")

    ok("GritTeethBuff.checkBypass" in cc and "checkRayBypass" not in cc,
       "[Char] 调用点已改用新名")

    # 标记接口的 javadoc 必须写明两处取用点
    ok("perseveranceCap" in sh and "checkBypass" in sh,
       "[SelfHarmCost] javadoc 写明两处取用点（perseveranceCap / checkBypass）")

    # ---------- ③ 文本 ----------
    ZH_PAIRS = [
        ("actors.hero.abilities.middlefinger.gritteeth.short_desc", ["解离射线", "血祭", "割腕"]),
        ("actors.hero.abilities.middlefinger.gritteeth.desc", ["解离射线", "血祭", "割腕"]),
        ("actors.buffs.gritteethbuff.desc", ["解离射线", "血祭", "割腕"]),
    ]
    EN_PAIRS = [
        ("actors.hero.abilities.middlefinger.gritteeth.short_desc", ["disintegration rays", "blood rites", "wrist-slitting"]),
        ("actors.hero.abilities.middlefinger.gritteeth.desc", ["disintegration rays", "blood rites", "wrist-slitting"]),
        ("actors.buffs.gritteethbuff.desc", ["disintegration rays", "blood rites", "wrist-slitting"]),
    ]
    for raw, table, tag in ((zh, ZH_PAIRS, "zh"), (en, EN_PAIRS, "en")):
        for key, musts in table:
            v = prop(raw, key)
            short = key.split(".")[-2] + "." + key.split(".")[-1]
            if v is None:
                ok(False, "[文本] %s %s 缺失" % (tag, short))
                continue
            for m in musts:
                ok(m.lower() in v.lower(),
                   "[文本] %s %s 点名「%s」（否则玩家不知道自伤换成长也能击穿）" % (tag, short, m))

    # 多段文本必须是**字面量 \n**，不能是真换行（否则渲染只剩第一段）
    for raw, tag in ((zh, "zh"), (en, "en")):
        bl = broken_lines(raw)
        ok(not bl, "[文本] %s 无「缺 = 的断行」%s" % (tag, ("→ " + repr(bl[:2])) if bl else ""))
        d = prop(raw, "actors.hero.abilities.middlefinger.gritteeth.desc")
        b = prop(raw, "actors.buffs.gritteethbuff.desc")
        ok(d is not None and "\\n\\n" in d, "[文本] %s 技能 desc 用字面量 \\n\\n 分段" % tag)
        ok(b is not None and "\\n\\n" in b, "[文本] %s buff desc 用字面量 \\n\\n 分段" % tag)
        ok(b is not None and "%s" in b, "[文本] %s buff desc 保留 %%s 占位符（剩余回合）" % tag)
        ok(d is not None and d.count("%") == 0, "[文本] %s 技能 desc 无裸 %% （有的话必须写成 %%%%）" % tag)


# ==================================================================== 反例自测
def selftest(rel, grit, ch, sh, chal, wrist, zh, en, wd):
    cases = []

    # ① gainStack 回退成写死 MAX_STACKS
    cases.append(("gainStack 写死 9 层", F_RELEASE, rel,
                  rel.replace("if (stacks < growthCap( ownerHero() )){", "if (stacks < MAX_STACKS){"),
                  "gainStack 按天赋点数取上限"))
    # ① +1 分支被改成也返回 MAX_STACKS
    cases.append(("+1 分支误用 9 层", F_RELEASE, rel,
                  rel.replace("if (points >= 1) return MAX_STACKS_1;", "if (points >= 1) return MAX_STACKS;"),
                  "+1 ⇒ MAX_STACKS_1(6)"))
    # ① 常量被改回 9（文本与代码漂移）
    cases.append(("MAX_STACKS_1 改成 9", F_RELEASE, rel,
                  rel.replace("public static final int MAX_STACKS_1 = 6;", "public static final int MAX_STACKS_1 = 9;"),
                  "MAX_STACKS_1 = 6"))
    # ② checkBypass 漏掉自伤来源（只留射线）
    cases.append(("击穿只认射线", F_GRIT, grit,
                  grit.replace("if (!isRay( src ) && !isSelfHarm( src )) return;", "if (!isRay( src )) return;"),
                  "射线与自伤换成长都算击穿来源"))
    # ② 击穿漏掉「非致命不作废」的保护
    cases.append(("任意一击都作废免死", F_GRIT, grit,
                  grit.replace("if (ch.HP > 0) return;", "if (false) return;"),
                  "只在真的打到 0 血时判定"))
    # ② isSelfHarm 改成别的判据
    cases.append(("isSelfHarm 不用标记接口", F_GRIT, grit,
                  grit.replace("return src instanceof SelfHarmCost;", "return false;"),
                  "isSelfHarm 判据是 SelfHarmCost"))
    # ② 某件道具掉了 implements
    cases.append(("圣杯掉了 SelfHarmCost", F_CHALICE, chal,
                  chal.replace("extends Artifact implements SelfHarmCost", "extends Artifact"),
                  "ChaliceOfBlood] implements SelfHarmCost"))
    # ② 某件道具的 damage 没把 this 当 src
    cases.append(("割腕 src 丢失", F_WRIST, wrist,
                  wrist.replace("hero.damage(damage, this);", "hero.damage(damage, null);"),
                  "WristSlit] hero.damage(damage, this)"))
    # ② Char.damage 调用点挪到死亡判定之后
    cases.append(("调用点挪到死亡判定之后", F_CHAR, ch,
                  ch.replace("\t\tGritTeethBuff.checkBypass( this, src );\n\n\t\tif (!isAlive()) {",
                             "\n\t\tif (!isAlive()) {\n\t\t\tGritTeethBuff.checkBypass( this, src );"),
                  "调用点在 if (!isAlive()) 死亡判定之前"))
    # ② 旧名复活
    cases.append(("旧名 checkRayBypass 复活", F_GRIT, grit,
                  grit.replace("public static void checkBypass(", "public static void checkRayBypass("),
                  "旧名 checkRayBypass 已不存在"))
    # ① 调试台误用受天赋上限约束的 gainStack
    cases.append(("调试台改用 gainStack", F_WNDDEBUG, wd,
                  wd.replace("release.debugGainStack()", "release.gainStack()"),
                  "叠层走 debugGainStack"))
    # ③ 文本漏掉「血祭」
    cases.append(("zh 文本漏掉血祭", F_ZH, zh,
                  zh.replace("_血祭_、_割腕_", "_自残_、_割腕_"),
                  "点名「血祭」"))
    # ③ 文本被真换行污染（只剩第一段）
    cases.append(("zh buff desc 断行", F_ZH, zh,
                  zh.replace("actors.buffs.gritteethbuff.desc=血量归零也_不会死亡_；",
                             "actors.buffs.gritteethbuff.desc=血量归零也_不会死亡_；\n"),
                  "zh 无「缺 = 的断行」"))

    all_red = True
    base = {F_RELEASE: rel, F_GRIT: grit, F_CHAR: ch, F_SELFHARM: sh,
            F_CHALICE: chal, F_WRIST: wrist, F_ZH: zh, F_EN: en, F_WNDDEBUG: wd}
    for title, path, src, mutated, expect_key in cases:
        assert mutated != src, "自测用例没改动源码：%s" % title
        args = dict(base)
        args[path] = mutated
        analyze(args[F_RELEASE], args[F_GRIT], args[F_CHAR], args[F_SELFHARM],
                args[F_CHALICE], args[F_WRIST], args[F_ZH], args[F_EN], args[F_WNDDEBUG])
        hit = any(expect_key in m for m in FAIL)
        print("  %s  %s  -> %s" % ("OK  " if hit else "漏洞", title, "变红" if hit else "**没变红**"))
        if not hit:
            all_red = False
    analyze(rel, grit, ch, sh, chal, wrist, zh, en, wd)
    return all_red


def main():
    rel = read(F_RELEASE)
    grit = read(F_GRIT)
    ch = read(F_CHAR)
    sh = read(F_SELFHARM)
    chal = read(F_CHALICE)
    wrist = read(F_WRIST)
    zh = read(F_ZH)
    en = read(F_EN)
    wd = read(F_WNDDEBUG)

    analyze(rel, grit, ch, sh, chal, wrist, zh, en, wd)

    print("===== 断言 =====")
    for m in PASS:
        print("  PASS  " + m)
    for m in FAIL:
        print("  FAIL  " + m)

    print("\n===== 反例自测 =====")
    st_ok = selftest(rel, grit, ch, sh, chal, wrist, zh, en, wd)

    print("\n断言 %d 条：PASS %d / FAIL %d；反例自测 %s"
          % (len(PASS) + len(FAIL), len(PASS), len(FAIL), "全部变红" if st_ok else "**有漏网**"))
    if FAIL or not st_ok:
        print("结果：FAIL")
        return 1
    print("结果：ALL PASS")
    return 0


if __name__ == "__main__":
    if "--list" in sys.argv[1:]:
        for p in (F_RELEASE, F_GRIT, F_CHAR, F_WNDDEBUG):
            src = read(p)
            print("===== %s =====" % p.split("/")[-1])
            for ln, l in enumerate(src.split("\n"), 1):
                if re.search(r"MAX_STACKS|growthCap|gainStack|checkBypass|isSelfHarm|checkRayBypass", l):
                    print("  %5d  %s" % (ln, l.strip()[:110]))
        for p in (F_ZH, F_EN):
            raw = read(p)
            print("===== %s =====" % p.split("/")[-1])
            for l in raw.split("\n"):
                if l.startswith("actors.hero.abilities.middlefinger.gritteeth.") \
                        or l.startswith("actors.buffs.gritteethbuff."):
                    print("  " + l[:150])
        print()
    sys.exit(main())
