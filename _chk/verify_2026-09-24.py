# -*- coding: utf-8 -*-
"""
核验：2026-09-24
① 中指长兄「背叛家人者」预支账簿充能的**生命周期**修好：
   力量补足必须活到「本击的回合成本结算完」，否则攻击延迟的 ×1.2ⁿ 惩罚漏掉。
② 死亡证明 → 999 层陈列室不再过滤神器「骷髅钥匙」。

判据来源：源码 4 个改动文件 + 2 个「依赖链」文件（Weapon / 反向确认没改）。
每条断言都带反例自测：把改动前的备份喂给同一批判据，必须判 FAIL。
"""
import os
import re
import sys

ROOT = r"D:\PD"


def J(*p):
    return os.path.join(ROOT, *p)


SRC = J("core", "src", "main", "java", "com", "shatteredpixel", "shatteredpixeldungeon")
FAMILY = J(SRC, "actors", "buffs", "FamilyBetrayal.java")
HERO = J(SRC, "actors", "hero", "Hero.java")
TALENT = J(SRC, "actors", "hero", "Talent.java")
WEAPON = J(SRC, "items", "weapon", "Weapon.java")
MUSEUM = J(SRC, "levels", "MuseumLevel.java")
GENERATOR = J(SRC, "items", "Generator.java")
BAK = J("_chk", "_bak_2026-09-24")

fails = []
total = [0]


def chk(cond, msg):
    total[0] += 1
    if cond:
        print("  ok   %s" % msg)
    else:
        fails.append(msg)
        print("  FAIL %s" % msg)
    return bool(cond)


def load(p):
    with open(p, "rb") as f:
        return f.read().decode("utf-8").replace("\r\n", "\n")


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


def before(hay, a, b):
    """a 在 b 之前；任一缺失都算 False（空串时不会抛 ValueError，也不会恒真）"""
    try:
        return hay.index(a) < hay.index(b)
    except ValueError:
        return False


# =====================================================================
# ①-a  FamilyBetrayal.java
# =====================================================================
def check_family(text):
    """返回 [(说明, 布尔)]；对改动前的备份，前两条必须为 False。"""
    out = []
    code = strip_comments(text)
    start = code_of(text, r"public\s+static\s+void\s+onAttackStarted\s*\(") or ""
    cons = code_of(text, r"public\s+static\s+void\s+consumeAfterAttack\s*\(") or ""
    doc = text[:text.index("public class FamilyBetrayal")]

    out.append(("onAttackStarted 方法体取到", bool(start)))
    out.append(("consumeAfterAttack 方法体取到", bool(cons)))

    # ── 核心：先摘残留，再算 need ──
    out.append(("onAttackStarted 里有 consumeAfterAttack( hero )", "consumeAfterAttack( hero )" in start))
    out.append(("残留清理发生在 need 计算之前",
                before(start, "consumeAfterAttack( hero )", "sword.STRReq() - hero.STR()")))
    out.append(("残留清理发生在 rancor 早退之后",
                before(start, "rancorActive", "consumeAfterAttack( hero )")))

    # ── 原有规则一条都不能丢 ──
    out.append(("仍是 FAMILY_BETRAYER 限定", "HeroSubClass.FAMILY_BETRAYER" in start))
    out.append(("目标无敌仍提前退出", "isInvulnerable" in start))
    out.append(("仍只认莱瓦汀系列", "instanceof SealedSwordBase" in start))
    out.append(("仇怨期间仍不触发", "rancorActive" in start))
    out.append(("力量已够仍不收费", "if (need <= 0) return;" in start))
    out.append(("整笔扣款仍是 CHARGE_COST", "consumeCharge( hero, CHARGE_COST )" in start))
    out.append(("付不起仍整件事作罢", "<= 0) return;" in start))
    out.append(("仍 apply( hero, need )", "apply( hero, need )" in start))

    # ── 消费点：文档必须改成 Hero.spend，且不再宣称 onHeroAttackResolved ──
    out.append(("类注释写明消费点＝Hero.spend(float)", "解除在 {@code Hero.spend(float)}" in doc))
    out.append(("类注释解释了为什么不能在 resolved 摘",
                "为什么不能提前到 {@code Talent.onHeroAttackResolved} 摘" in doc))
    out.append(("类注释不再把 onHeroAttackResolved 当解除点",
                "解除在 {@code Talent.onHeroAttackResolved}" not in doc))
    out.append(("consumeAfterAttack 注释写明消费点",
                "消费点＝{@code Hero.spend(float)}" in text))
    out.append(("consumeAfterAttack 仍是真的摘 buff",
                "hero.buff( FamilyBetrayal.class )" in cons and "detach()" in cons))
    out.append(("DURATION 兜底仍在", bool(re.search(r"DURATION\s*=\s*1f\s*;", code))))
    out.append(("amountOf 仍在（Hero.STR 的取数口）", "public static int amountOf" in code))
    return out


print("[1] FamilyBetrayal.java")
fam = load(FAMILY)
fam_res = check_family(fam)
for msg, ok in fam_res:
    chk(ok, "family: " + msg)

fam_old = load(os.path.join(BAK, "FamilyBetrayal.java"))
old_res = dict(check_family(fam_old))
print("  反例中间量：旧文件 onAttackStarted 长度=%d，含 consumeAfterAttack=%s"
      % (len(code_of(fam_old, r"public\s+static\s+void\s+onAttackStarted\s*\(") or ""),
         old_res.get("onAttackStarted 里有 consumeAfterAttack( hero )")))
chk(old_res.get("onAttackStarted 里有 consumeAfterAttack( hero )") is False,
    "反例 FAIL：旧 onAttackStarted 里没有残留清理")
chk(old_res.get("类注释写明消费点＝Hero.spend(float)") is False,
    "反例 FAIL：旧类注释没写 Hero.spend")
chk(old_res.get("类注释不再把 onHeroAttackResolved 当解除点") is False,
    "反例 FAIL：旧类注释正是把 onHeroAttackResolved 当解除点")

# =====================================================================
# ①-b  Hero.java：spend() 是消费点；STR 汇聚点喂了 buff
# =====================================================================
def check_hero(text):
    out = []
    spend = code_of(text, r"public\s+void\s+spend\s*\(\s*float\s+time\s*\)") or ""
    strm = code_of(text, r"public\s+int\s+STR\s*\(\s*\)") or ""
    ad = code_of(text, r"public\s+final\s+float\s+attackDelay\s*\(\s*\)") or ""
    adr = code_of(text, r"protected\s+float\s+attackDelayRaw\s*\(\s*\)") or ""
    oac = code_of(text, r"public\s+void\s+onAttackComplete\s*\(\s*\)") or ""

    out.append(("spend(float) 方法体取到", bool(spend)))
    out.append(("STR() 方法体取到", bool(strm)))
    out.append(("attackDelay() 方法体取到", bool(ad)))
    out.append(("attackDelayRaw() 方法体取到", bool(adr)))
    out.append(("onAttackComplete() 方法体取到", bool(oac)))

    # ── 消费点 ──
    out.append(("spend 里调 FamilyBetrayal.consumeAfterAttack( this )",
                "FamilyBetrayal.consumeAfterAttack( this )" in spend))
    out.append(("消费发生在 super.spend(time) 之后",
                before(spend, "super.spend(time)", "FamilyBetrayal.consumeAfterAttack")))
    out.append(("消费发生在 SilentPrice.resetIdleClock() 之后（保持既有钩子顺序）",
                before(spend, "SilentPrice.resetIdleClock()", "FamilyBetrayal.consumeAfterAttack")))

    # ── 攻击延迟仍走唯一出口，且延迟读的是 STR() ──
    out.append(("attackDelay 仍 final", "public final float attackDelay()" in text))
    out.append(("attackDelay 仍夹考验（Trials.modifyHeroAttackDelay）",
                "Trials.modifyHeroAttackDelay" in ad))
    out.append(("attackDelayRaw 仍读武器 delayFactor",
                "belongings.attackingWeapon().delayFactor( this )" in adr))
    out.append(("onAttackComplete 在攻击之后才算 attackDelay",
                before(oac, "attack(attackTarget)", "spend( attackDelay() )")))

    # ── 力量汇聚点 ──
    out.append(("Hero.STR() 里加算 FamilyBetrayal.amountOf", "FamilyBetrayal.amountOf( this )" in strm))
    return out


print("[2] Hero.java")
hero = load(HERO)
for msg, ok in check_hero(hero):
    chk(ok, "hero: " + msg)

hero_old = load(os.path.join(BAK, "Hero.java"))
old_hero = dict(check_hero(hero_old))
print("  反例中间量：旧 spend 长度=%d，含 consumeAfterAttack=%s"
      % (len(code_of(hero_old, r"public\s+void\s+spend\s*\(\s*float\s+time\s*\)") or ""),
         old_hero.get("spend 里调 FamilyBetrayal.consumeAfterAttack( this )")))
chk(old_hero.get("spend 里调 FamilyBetrayal.consumeAfterAttack( this )") is False,
    "反例 FAIL：旧 Hero.spend 里没有 consumeAfterAttack")
chk(old_hero.get("Hero.STR() 里加算 FamilyBetrayal.amountOf") is True,
    "对照：旧 Hero.STR() 里本来就有 FamilyBetrayal.amountOf（说明旧版只修了精准）")

# =====================================================================
# ①-c  Talent.java：resolved 钩子不再是消费点
# =====================================================================
def check_talent(text):
    out = []
    started = code_of(text, r"public\s+static\s+void\s+onHeroAttackStarted\s*\(") or ""
    resolved = code_of(text, r"public\s+static\s+void\s+onHeroAttackResolved\s*\(") or ""
    allc = strip_comments(text)

    out.append(("onHeroAttackStarted 方法体取到", bool(started)))
    out.append(("onHeroAttackResolved 方法体取到", bool(resolved)))
    out.append(("攻击前钩子仍调 FamilyBetrayal.onAttackStarted",
                "FamilyBetrayal.onAttackStarted( hero, enemy )" in started))
    out.append(("resolved 钩子已不再消费 FamilyBetrayal",
                "FamilyBetrayal.consumeAfterAttack" not in resolved))
    out.append(("全文件只剩「攻击前」一处 FamilyBetrayal 代码调用",
                allc.count("FamilyBetrayal.") == 1))
    out.append(("import 仍在（攻击前钩子用得到）",
                "import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FamilyBetrayal;" in text))
    # 其余规则不能被碰掉
    out.append(("战争英雄连锁判定仍在 resolved 里",
                "TremblingScorch.onParalysisLifted" in resolved))
    out.append(("加速弹消耗仍在 resolved 里",
                "AccelerationShot.consumeOnAttack( hero )" in resolved))
    return out


print("[3] Talent.java")
talent = load(TALENT)
for msg, ok in check_talent(talent):
    chk(ok, "talent: " + msg)

talent_old = load(os.path.join(BAK, "Talent.java"))
old_tal = dict(check_talent(talent_old))
print("  反例中间量：旧 resolved 长度=%d，含 consumeAfterAttack=%s"
      % (len(code_of(talent_old, r"public\s+static\s+void\s+onHeroAttackResolved\s*\(") or ""),
         old_tal.get("resolved 钩子已不再消费 FamilyBetrayal")))
chk(old_tal.get("resolved 钩子已不再消费 FamilyBetrayal") is False,
    "反例 FAIL：旧 resolved 钩子里正是消费点")
chk(old_tal.get("全文件只剩「攻击前」一处 FamilyBetrayal 代码调用") is False,
    "反例 FAIL：旧文件里 FamilyBetrayal 被调了两处")

# =====================================================================
# ①-d  Weapon.java（未改动，作为「为什么 STR 路线能管到延迟」的依赖链证据）
# =====================================================================
print("[4] Weapon.java（未改动，依赖链证据）")
weapon = load(WEAPON)
acc = code_of(weapon, r"public\s+float\s+accuracyFactor\s*\(\s*Char\s+owner\s*,\s*Char\s+target\s*\)") or ""
bd = code_of(weapon, r"protected\s+float\s+baseDelay\s*\(\s*Char\s+owner\s*\)") or ""
delay = code_of(weapon, r"public\s+float\s+delayFactor\s*\(\s*Char\s+owner\s*\)") or ""
chk("STRReq() - ((Hero)owner).STR()" in acc, "weapon: 精准惩罚读 owner.STR()")
chk("STRReq() - ((Hero)owner).STR()" in bd, "weapon: 延迟惩罚读 owner.STR()（同一来源）")
chk("Math.pow( 1.2, encumbrance )" in bd, "weapon: 延迟惩罚确为 ×1.2ⁿ")
chk("baseDelay(owner)" in delay, "weapon: delayFactor = baseDelay × 1/speedMultiplier")

# =====================================================================
# ②  MuseumLevel.java（陈列室放行骷髅钥匙）
# =====================================================================
def check_museum(text):
    out = []
    code = strip_comments(text)
    skip = code_of(text, r"private\s+boolean\s+skip\s*\(\s*Class<\?>\s+c\s*\)") or ""

    out.append(("skip() 方法体取到", bool(skip)))
    out.append(("skip() 仍过滤英雄专属盔甲", "ClassArmor.class.isAssignableFrom(c)" in skip))
    out.append(("skip() 不再过滤 SkeletonKey", "SkeletonKey" not in skip))
    out.append(("createItems 仍陈列神器图鉴", "section(Catalog.ARTIFACTS, true);" in code))
    out.append(("SkeletonKey import 已删除（只剩注释提及）",
                "items.artifacts.SkeletonKey;" not in text))
    # 其余板块一个都不能少（改动只该动 skip）
    for cat in ("MELEE_WEAPONS", "ARMOR", "WANDS", "RINGS", "ARTIFACTS", "TRINKETS",
                "POTIONS", "SCROLLS", "SEEDS", "STONES", "FOOD", "EXOTIC_POTIONS",
                "EXOTIC_SCROLLS", "BOMBS", "TIPPED_DARTS", "BREWS_ELIXIRS", "SPELLS"):
        out.append(("仍陈列 Catalog.%s" % cat, "Catalog.%s" % cat in code))
    out.append(("投掷武器仍走 sectionMissiles", "sectionMissiles();" in code))
    return out


print("[5] MuseumLevel.java")
museum = load(MUSEUM)
for msg, ok in check_museum(museum):
    chk(ok, "museum: " + msg)

museum_old = load(os.path.join(BAK, "MuseumLevel.java"))
old_mus = dict(check_museum(museum_old))
print("  反例中间量：旧 skip() 长度=%d，其中 SkeletonKey 出现 %d 次"
      % (len(code_of(museum_old, r"private\s+boolean\s+skip\s*\(\s*Class<\?>\s+c\s*\)") or ""),
         (code_of(museum_old, r"private\s+boolean\s+skip\s*\(\s*Class<\?>\s+c\s*\)") or "").count("SkeletonKey")))
chk(old_mus.get("skip() 不再过滤 SkeletonKey") is False,
    "反例 FAIL：旧 skip() 里正是把 SkeletonKey 过滤掉")
chk(old_mus.get("SkeletonKey import 已删除（只剩注释提及）") is False,
    "反例 FAIL：旧文件里 SkeletonKey import 还在")

# =====================================================================
# ②-b  Generator.java：SkeletonKey 确实在神器池里（否则放行也没用）
# =====================================================================
print("[6] Generator.java（神器池确实含 SkeletonKey）")
gen = load(GENERATOR)
m = re.search(r"ARTIFACT\.classes\s*=\s*new Class<\?>\s*\[\]\s*\{", gen)
assert m, "找不到 ARTIFACT.classes 赋值"
blk = gen[m.end(): gen.index("};", m.end())]
names = re.findall(r"(\w+)\.class", blk)
print("  神器池 %d 项：%s" % (len(names), ", ".join(names)))
chk("SkeletonKey" in names, "generator: ARTIFACT.classes 含 SkeletonKey.class")
chk(len(names) == 15, "generator: 神器池 15 项（实得 %d）" % len(names))

# =====================================================================
# 附：javac 告警对比 + javap 字节码证据 + 真装载陈列清单
# =====================================================================
import glob
import subprocess

JDK = J("tools", "jdk-21.0.12.1+1", "bin")
MAVEN_ROOT = r"C:/Users/*/.gradle/caches/modules-2/files-2.1"


def find_jar(*pat):
    hits = glob.glob(os.path.join(MAVEN_ROOT, *pat))
    assert hits, "找不到 jar：%s" % os.path.join(MAVEN_ROOT, *pat)
    return hits[0].replace("\\", "/")


CP = ";".join([
    J("_chk", "_javachk24"),
    J("core", "build", "classes", "java", "main"),
    J("SPD-classes", "build", "classes", "java", "main"),
    J("services", "build", "classes", "java", "main"),
    find_jar("com.badlogicgames.gdx", "gdx", "1.14.0", "*", "gdx-1.14.0.jar"),
    find_jar("com.badlogicgames.gdx-controllers", "gdx-controllers-core", "2.2.4", "*",
             "gdx-controllers-core-2.2.4.jar"),
])
OUT24 = J("_chk", "_javachk24")
OUTOLD = J("_chk", "_javachk_old24")


def sh(cmd):
    r = subprocess.run(cmd, capture_output=True)
    # javac/javap 的中文输出在 Windows 上是 GBK；子 JVM 的 System.out 也是（-Dfile.encoding
    # 在 Java 18+ 已不管 stdout）——所以先按 UTF-8 严格试，失败再退回 GBK，避免乱码导致断言假红
    def dec(b):
        if not b:
            return ""
        try:
            return b.decode("utf-8")
        except UnicodeDecodeError:
            return b.decode("gbk", "replace")
    return r.returncode, dec(r.stdout), dec(r.stderr)


def warn_kinds(text):
    """抽 (文件.java, 告警类别)；行号会因为插行整体平移，不能直接比行号"""
    return [(m.group(1), m.group(2)) for m in
            re.finditer(r"\\(\w+\.java):(\d+): 警告: \[([\w-]+)\]", text)]


def warn_kinds2(text):
    return sorted((m.group(1), m.group(3)) for m in
                  re.finditer(r"\\(\w+\.java):(\d+): 警告: \[([\w-]+)\]", text))


# ── A. 新编译：0 错误，且告警行号必须落在「未改动行」上 ──
print("[7] javac -Xlint:all（新源码）")
changed_lines = {"Hero.java": (1034, 1046), "Talent.java": (1045, 1056),
                 "FamilyBetrayal.java": (0, 10 ** 6), "MuseumLevel.java": (0, 10 ** 6)}
files = [FAMILY, MUSEUM, TALENT, HERO]
os.makedirs(OUT24, exist_ok=True)
code, out, err = sh([os.path.join(JDK, "javac.exe"), "-proc:none", "-Xlint:all", "-encoding", "UTF-8",
                     "-cp", CP, "-sourcepath", J("core", "src", "main", "java"), "-d", OUT24] + files)
print("    javac EXIT=%d" % code)
chk(code == 0, "javac 退出码 0（0 错误）")
chk("错误" not in err and "error" not in err.lower(), "javac 输出里没有『错误』字样")
warns = re.findall(r"\\(\w+\.java):(\d+): 警告", err)
print("    告警：%s" % ", ".join("%s:%s" % w for w in warns))
offside = []
for f, ln in warns:
    lo, hi = changed_lines.get(f, (0, 0))
    if lo <= int(ln) <= hi:
        offside.append("%s:%s" % (f, ln))
chk(not offside, "没有新增告警落在改动行上（越界：%s）" % (offside or "无"))

# ── B. 旧源码同参数编译：告警类别应完全一致（证明告警是既有；行号会随插行平移，不比行号）──
print("[8] javac -Xlint:all（改动前备份，作对照）")
os.makedirs(OUTOLD, exist_ok=True)
oldfiles = [os.path.join(BAK, n) for n in ("FamilyBetrayal.java", "MuseumLevel.java", "Talent.java", "Hero.java")]
code_old, _, err_old = sh([os.path.join(JDK, "javac.exe"), "-proc:none", "-Xlint:all", "-encoding", "UTF-8",
                           "-cp", CP, "-sourcepath", J("core", "src", "main", "java"), "-d", OUTOLD] + oldfiles)
wn, wo = warn_kinds2(err), warn_kinds2(err_old)
print("    新告警类别：%s" % wn)
print("    旧告警类别：%s" % wo)
chk(code_old == 0, "改动前也编译通过（对照成立）")
chk(wn == wo, "新旧告警「文件+类别」集合完全一致（既有告警，非本次引入）")
chk(len(wn) == 5 and not any(f in ("FamilyBetrayal.java", "MuseumLevel.java") for f, _ in wn),
    "告警只落在 Hero/Talent 的既有行上（实得 %s）" % wn)

# ── C. javap 字节码：消费点真的落在 Hero.spend 里 ──
print("[9] javap -c 字节码证据")
JAVAP = os.path.join(JDK, "javap.exe")


def javap(cls, member, out_dir):
    _, o, _ = sh([JAVAP, "-p", "-c", "-cp", out_dir, cls])
    o = o.replace("\r\n", "\n")   # Windows javap 输出 CRLF，不归一化的话下面按 \n\n 切不通
    m = re.search(re.escape(member) + r"[^\n]*\n(.*?)(?=\n\n)", o, re.S)
    return m.group(1) if m else None


def bytecode_of(cls, member, out_dir):
    """取某方法的字节码段（javap 用空行分隔方法）"""
    blk = javap(cls, member, out_dir)
    return blk or ""


spend_bc = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero", "public void spend(float)", OUT24)
resolved_bc = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent",
                          "public static void onHeroAttackResolved", OUT24)
started_bc = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent",
                         "public static void onHeroAttackStarted", OUT24)
str_bc = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero", "public int STR()", OUT24)
print("    新 Hero.spend 字节码 %d 字符 / 新 Talent.onHeroAttackResolved %d 字符"
      % (len(spend_bc), len(resolved_bc)))
chk("FamilyBetrayal.consumeAfterAttack" in spend_bc, "字节码：Hero.spend 里真的有 consumeAfterAttack 调用")
chk("Char.spend:(F)V" in spend_bc, "字节码：Hero.spend 里先调了 super（Char.spend）")
chk(spend_bc.index("Char.spend:(F)V") < spend_bc.index("consumeAfterAttack"),
    "字节码：consumeAfterAttack 在 super.spend 之后")
chk("FamilyBetrayal" not in resolved_bc, "字节码：onHeroAttackResolved 里没有任何 FamilyBetrayal 调用")
chk("FamilyBetrayal.onAttackStarted" in started_bc, "字节码：onHeroAttackStarted 仍调 FamilyBetrayal.onAttackStarted")
chk("FamilyBetrayal.amountOf" in str_bc, "字节码：Hero.STR() 仍加算 FamilyBetrayal.amountOf")

# ── D. 旧字节码对照：消费点在 resolved、spend 里什么都没有 ──
print("[10] javap -c 旧字节码对照")
spend_bc_old = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero", "public void spend(float)", OUTOLD)
resolved_bc_old = bytecode_of("com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent",
                              "public static void onHeroAttackResolved", OUTOLD)
print("    旧 Hero.spend 字节码 %d 字符；旧 resolved 含 FamilyBetrayal=%s"
      % (len(spend_bc_old), "FamilyBetrayal" in resolved_bc_old))
chk("FamilyBetrayal" not in spend_bc_old, "旧字节码：spend 里确实没有消费点（bug 现场）")
chk("FamilyBetrayal.consumeAfterAttack" in resolved_bc_old, "旧字节码：消费点在 onHeroAttackResolved（提前摘）")

# ── E. 真装载：陈列清单 ──
print("[11] 真装载：999 层陈列清单（_chk/MuseumLocCheck.java）")
code, out, err = sh([os.path.join(JDK, "java.exe"), "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8",
                     "-cp", CP, J("_chk", "MuseumLocCheck.java")])
tail = [l for l in out.split("\n") if ("ok " in l or "FAIL" in l or "断言总数" in l or "缺席" in l or "合计陈列" in l)]
for l in tail:
    print("    " + l.strip())
chk("ALL PASS" in out, "真装载核验 ALL PASS")
chk("缺席的：无" in out, "神器图鉴里没有缺席项")
chk("ok   骷髅钥匙在陈列清单里" in out, "骷髅钥匙确实在陈列清单里")

# =====================================================================
print()
print("断言总数 %d，失败 %d" % (total[0], len(fails)))
if fails:
    print("FAIL：%d 条" % len(fails))
    for f in fails:
        print("   -", f)
    sys.exit(1)
print("ALL PASS")
