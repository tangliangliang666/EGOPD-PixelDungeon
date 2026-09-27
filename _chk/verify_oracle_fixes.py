# -*- coding: utf-8 -*-
"""
神谕代行者三处修复的回归核验（纯源码静态断言，不跑 Gradle）。

覆盖：
  ① 剑刃解放（Release）死亡清除后，复活按「业」重新挂上保底解放层数
     —— Release.karmaFloor / Release.onHeroRevive + Hero.live() 的接线
  ② 拼好饭（PinHaoFan）充能消耗由 15% 加倍到 30%（且不许误伤心-命运/ Furioso）
  ③ Furioso-Replica「与你再会」的击杀计数顺序（必须先 damage 再判 isAlive）

用法：
    python _chk/verify_oracle_fixes.py            # 正常核验
    python _chk/verify_oracle_fixes.py --list     # 额外打印关键行的上下文
退出码 0 = ALL PASS，1 = 有 FAIL 或反例自测没全变红。
"""

import io
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PKG = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"

F_RELEASE = PKG + "actors/buffs/Release.java"
F_HERO = PKG + "actors/hero/Hero.java"
F_PINHAOFAN = PKG + "actors/hero/abilities/oracle/PinHaoFan.java"
F_FURIOSO = PKG + "actors/hero/abilities/oracle/FuriosoReplica.java"
F_HEARTFATE = PKG + "actors/hero/abilities/oracle/HeartFate.java"

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
    """取方法体（含大括号）。sig 可给更严格的签名正则。"""
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
    """取方法签名（从修饰符到右括号），用于判断是否 static。"""
    c = code_of(src)
    m = re.search(r"([\w<>\[\],\s\.]+?\b" + re.escape(name) + r"\s*\([^;{}]*?\))\s*\{", c)
    return m.group(1).strip() if m else ""


# ==================================================================== 断言
def analyze(rel, hero, phf, fru, hf):
    del PASS[:], FAIL[:]

    rc = code_of(rel)
    hc = code_of(hero)
    pc = code_of(phf)
    fc = code_of(fru)

    # ---------- ① 剑刃解放：复活按业重挂 ----------
    kf = body_of(rel, "karmaFloor")
    ok(kf is not None, "[Release] 存在 karmaFloor(Hero) 方法")
    ok(re.search(r"\bstatic\b", sig_of(rel, "karmaFloor") or ""),
       "[Release] karmaFloor 是 static（供 Hero.live 调用）")
    ok(kf and re.search(r"pointsInTalent\(\s*Talent\.BLADE_RELEASE\s*\)\s*<\s*2", kf),
       "[Release] karmaFloor 只在天赋点满 2 点时给保底")
    ok(kf and "buff(Karma.class)" in kf, "[Release] karmaFloor 读取「业」Karma buff")
    ok(kf and re.search(r"Math\.min\(\s*MAX_STACKS\s*,\s*karma\.karma\(\)\s*/\s*10\s*\)", kf),
       "[Release] karmaFloor 用「业每 10 点 1 层、上限 MAX_STACKS」")

    es = body_of(rel, "effectiveStacks")
    ok(es is not None and "karmaFloor(Dungeon.hero)" in es,
       "[Release] effectiveStacks 统一走 karmaFloor（保底与复挂同一套规则）")

    rv = body_of(rel, "onHeroRevive")
    ok(rv is not None, "[Release] 存在 onHeroRevive(Hero) 方法")
    ok(re.search(r"\bstatic\b", sig_of(rel, "onHeroRevive") or ""),
       "[Release] onHeroRevive 是 static")
    ok(rv and re.search(r"hero\s*==\s*null\s*\|\|\s*hero\.buff\(\s*Release\.class\s*\)\s*!=\s*null", rv),
       "[Release] onHeroRevive 先短路（空英雄 / 已有 buff 不重复挂）")
    ok(rv and re.search(r"karmaFloor\(\s*hero\s*\)\s*>\s*0", rv),
       "[Release] onHeroRevive 只在保底 > 0 时才挂 buff（业不足 10 点不生成空 buff）")
    ok(rv and "Buff.affect(hero, Release.class)" in rv,
       "[Release] onHeroRevive 用 Buff.affect 重新挂上解放 buff")
    ok(not re.search(r"heroClass", rv or ""),
       "[Release] onHeroRevive 不判职业（蜕变/换职业后依然有效）")

    ok("revivePersists" not in rc,
       "[Release] 解放 buff 仍**不是** revivePersists（死亡清除的语义按用户要求保留）")

    # ---------- ① Hero.live 接线 ----------
    live = body_of(hero, "live", sig=r"public\s+void\s+live\s*\(")
    ok(live is not None, "[Hero] 找到 live()")
    ok(live and "Release.onHeroRevive( this )" in live,
       "[Hero] live() 调用 Release.onHeroRevive( this )")
    if live:
        i_loop = live.find("revivePersists")
        i_call = live.find("Release.onHeroRevive")
        ok(0 <= i_loop < i_call,
           "[Hero] 调用点在「detach 非 revivePersists buff」的循环**之后**")
    ok("import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Release;" in hero,
       "[Hero] 已 import Release（否则编译不过，这条防回退）")

    # ---------- ② 拼好饭充能翻倍 ----------
    ok(re.search(r"baseChargeUse\s*=\s*30f", pc), "[PinHaoFan] baseChargeUse = 30f（由 15f 加倍）")
    ok(not re.search(r"baseChargeUse\s*=\s*15f", pc), "[PinHaoFan] 旧的 15f 已不存在")
    ok(re.search(r"baseChargeUse\s*=\s*20f", code_of(hf)),
       "[HeartFate] 心-命运仍为 20f（未被误伤）")
    ok(re.search(r"baseChargeUse\s*=\s*90f", fc),
       "[FuriosoReplica] Furioso 仍为 90f（未被误伤）")

    # ---------- ③ Furioso 击杀计数 ----------
    atk = body_of(fru, "doAttack")
    ok(atk is not None, "[Furioso] 找到 doAttack(...)")
    if atk:
        i_dmg = atk.find("enemy.damage(")
        i_kill = atk.find("newKills")
        ok(i_dmg >= 0 and i_kill > i_dmg,
           "[Furioso] 先 enemy.damage(...) 再算 newKills（顺序即本 bug 的根因）")
        ok(re.search(r"boolean\s+wasAlive\s*=\s*enemy\.isAlive\(\)", atk),
           "[Furioso] 伤害前先记录 wasAlive")
        ok(re.search(r"newKills\s*=\s*\(\s*wasAlive\s*&&\s*!\s*enemy\.isAlive\(\)\s*\)", atk),
           "[Furioso] newKills = (wasAlive && !enemy.isAlive()) ? kills+1 : kills")
        # 旧写法：在 damage 之前判 isAlive 决定击杀
        ok(not re.search(r"newKills\s*=\s*!\s*enemy\.isAlive\(\)", atk),
           "[Furioso] 旧的「damage 之前判 isAlive」写法已移除")
        ok(re.search(r"if\s*\(\s*enemy\.isAlive\(\)\s*\)", atk),
           "[Furioso] 击退仍被 if (enemy.isAlive()) 保护（尸体不击退）")

    fin = body_of(fru, "finish", sig=r"private\s+void\s+finish\s*\(")
    ok(fin is not None, "[Furioso] 找到 finish(...)")
    ok(fin and re.search(r"unused\s*=\s*MAX_ATTACKS\s*-\s*hitsDone", fin),
       "[Furioso] 未使用次数 = MAX_ATTACKS - hitsDone")
    ok(fin and re.search(r"\(\s*kills\s*\+\s*unused\s*\)", fin),
       "[Furioso] 返还按 (击杀数 + 未使用次数) 计")
    ok(fin and re.search(r"Math\.min\(\s*100\s*,", fin),
       "[Furioso] 返还后充能封顶 100")
    ok(fin and "hero.pointsInTalent(Talent.REUNION)" in fin,
       "[Furioso] 返还只在点出「与你再会」时生效")

    # ---------- ④ 判据红线：不得回退成职业判定 ----------
    for nm, src_body in (("Release.onHeroRevive", rv), ("Furioso.doAttack", atk), ("Furioso.finish", fin)):
        ok(src_body is not None and "heroClass" not in src_body,
           "[红线] %s 内不出现 heroClass（天赋判据只看天赋点数）" % nm)


# ==================================================================== 反例自测
def selftest(rel, hero, phf, fru, hf):
    """把每处修复「改坏」，确认对应断言确实变红；否则就是恒真空测。"""
    cases = []

    # ① 把 onHeroRevive 的调用从 live() 摘掉
    cases.append(("Hero.live 漏掉重挂", F_HERO,
                  hero, hero.replace("Release.onHeroRevive( this );", ""),
                  "live() 调用 Release.onHeroRevive"))
    # ① 把保底判据改成总是为 0（漏掉「业」）
    cases.append(("karmaFloor 漏读业", F_RELEASE,
                  rel, rel.replace("Karma karma = hero.buff(Karma.class);", "Karma karma = null;"),
                  "karmaFloor 读取「业」"))
    # ① 让解放 buff 变成复活保留（违反「死亡清除」）
    cases.append(("Release 变成 revivePersists", F_RELEASE,
                  rel, rel.replace("type = buffType.POSITIVE;", "type = buffType.POSITIVE;\n\t\trevivePersists = true;"),
                  "不是** revivePersists"))
    # ② 充能没翻倍
    cases.append(("拼好饭仍是 15%", F_PINHAOFAN,
                  phf, phf.replace("baseChargeUse = 30f", "baseChargeUse = 15f"),
                  "baseChargeUse = 30f"))
    # ③ 击杀判定回到 damage 之前（复现原 bug）
    cases.append(("击杀计数顺序回退", F_FURIOSO,
                  fru, fru.replace("\t\tboolean wasAlive = enemy.isAlive();\n"
                                   "\t\tif (dmg > 0){\n\t\t\tenemy.damage(dmg, hero);\n\t\t}\n"
                                   "\t\tfinal int newKills = (wasAlive && !enemy.isAlive()) ? kills + 1 : kills;",
                                   "\t\tfinal int newKills = !enemy.isAlive() ? kills + 1 : kills;\n"
                                   "\t\tif (dmg > 0){\n\t\t\tenemy.damage(dmg, hero);\n\t\t}"),
                  "先 enemy.damage(...) 再算 newKills"))
    # ③ 超修：把击退的 isAlive 保护删掉
    cases.append(("击退失去 isAlive 保护", F_FURIOSO,
                  fru, fru.replace("\t\tif (enemy.isAlive()){\n\t\t\t//击退一格",
                                   "\t\tif (true){\n\t\t\t//击退一格"),
                  "击退仍被 if (enemy.isAlive()) 保护"))

    all_red = True
    for title, path, src, mutated, expect_key in cases:
        assert mutated != src, "自测用例没改动源码：%s" % title
        args = {F_RELEASE: rel, F_HERO: hero, F_PINHAOFAN: phf, F_FURIOSO: fru, F_HEARTFATE: hf}
        args[path] = mutated
        analyze(args[F_RELEASE], args[F_HERO], args[F_PINHAOFAN], args[F_FURIOSO], args[F_HEARTFATE])
        hit = any(expect_key in m for m in FAIL)
        print("  %s  %s  -> %s" % ("OK  " if hit else "漏洞", title, "变红" if hit else "**没变红**"))
        if not hit:
            all_red = False
    # 自测结束后恢复真实断言集
    analyze(rel, hero, phf, fru, hf)
    return all_red


def main():
    rel = read(F_RELEASE)
    hero = read(F_HERO)
    phf = read(F_PINHAOFAN)
    fru = read(F_FURIOSO)
    hf = read(F_HEARTFATE)

    analyze(rel, hero, phf, fru, hf)

    print("===== 断言 =====")
    for m in PASS:
        print("  PASS  " + m)
    for m in FAIL:
        print("  FAIL  " + m)

    print("\n===== 反例自测 =====")
    st_ok = selftest(rel, hero, phf, fru, hf)

    print("\n断言 %d 条：PASS %d / FAIL %d；反例自测 %s"
          % (len(PASS) + len(FAIL), len(PASS), len(FAIL), "全部变红" if st_ok else "**有漏网**"))
    if FAIL or not st_ok:
        print("结果：FAIL")
        return 1
    print("结果：ALL PASS")
    return 0


if __name__ == "__main__":
    if "--list" in sys.argv[1:]:
        for p in (F_RELEASE, F_HERO, F_PINHAOFAN, F_FURIOSO):
            src = read(p)
            print("===== %s =====" % p.split("/")[-1])
            for ln, l in enumerate(src.split("\n"), 1):
                if re.search(r"karmaFloor|onHeroRevive|effectiveStacks|baseChargeUse|wasAlive|newKills|unused\s*=", l):
                    print("  %5d  %s" % (ln, l.strip()[:110]))
        print()
    sys.exit(main())
