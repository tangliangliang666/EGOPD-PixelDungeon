# -*- coding: utf-8 -*-
"""
一次性补丁（2026-09-20）：天赋判据去职业化
=========================================
把「天赋的生效判据」从 hero.heroClass 硬判定改成「有没有这门天赋的点数」，
让蜕变卷轴（ScrollOfMetamorphosis）变出的别职业天赋在非本职业英雄身上照样生效。

根因：WndMetamorphReplace 把别职业的天赋经 Hero.metamorphedTalents 塞进 Hero.talents
的同一层槽位 ⇒「战士身上有过人的毅力」是合法状态，而 Talent 里那一排
`hero.heroClass != HeroClass.MIDDLE_FINGER` 直接把它拦死了（点满却零效果、不报错）。

顺序很关键：**先按原始行号做行级改写，再插入说明块**（说明块会把后面所有行号推后）。

⚠️ 本脚本 **2026-09-20 已执行过一次**，带幂等护栏：再跑会打印 SKIP 并退出（不重复插入）。
它留在这里只作为「当时到底改了什么」的完整记录；日常回归用 `_chk/verify_metamorph_talents.py`。
用法：python _chk/fix_metamorph_talent_gate.py
"""
import io
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
BASE = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/%s"

T = BASE % "actors/hero/Talent.java"
C = BASE % "actors/Char.java"
S = BASE % "actors/mobs/npcs/Shopkeeper.java"
R = BASE % "items/AccelerationRound.java"
K = BASE % "actors/buffs/TremblingScorch.java"


def read(path):
    with io.open(os.path.join(ROOT, path), "r", encoding="utf-8", newline="") as f:
        return f.read()


def write(path, text):
    with io.open(os.path.join(ROOT, path), "w", encoding="utf-8", newline="") as f:
        f.write(text)


def line_patch(path, ops):
    """ops: [(start, end, new_text_or_None, expect_first_line)]，1-based 闭区间，
    按行号降序应用；expect 非 None 时断言该行原文一致（防行号漂移）。"""
    raw = read(path)
    nl = "\r\n" if "\r\n" in raw else "\n"
    lines = raw.replace("\r\n", "\n").split("\n")
    for start, end, new, expect in sorted(ops, key=lambda o: -o[0]):
        if expect is not None:
            assert lines[start - 1] == expect, \
                "!! %s:%d 行号漂移\n  期望 %r\n  实际 %r" % (path, start, expect, lines[start - 1])
        lines[start - 1:end] = [] if new is None else new.split("\n")
    write(path, nl.join(lines))


def sub(path, pairs, tag=""):
    t = read(path)
    for old, new, cnt in pairs:
        got = t.count(old)
        assert got == cnt, "!! %s%s：期望 %d 次，实际 %d 次\n%r" % (path, tag, cnt, got, old[:100])
        t = t.replace(old, new)
    write(path, t)


def rename(path, old, new):
    t = read(path)
    n = t.count(old)
    write(path, t.replace(old, new))
    return n


# 幂等护栏
_guard = read(T)
if "【判据红线】" in _guard:
    print("SKIP: 本补丁已应用过（Talent.java 已有【判据红线】说明块）")
    raise SystemExit(0)

# ============================================================ ① Talent.java 行级
MID = "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return;"
FALSE = "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return false;"
ZERO = "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return 0;"
ONE_F = "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return 1f;"

NEW_METHOD = "\n".join([
    "\t/**",
    "\t * 这一下伤害是否算「<b>英雄本人打出来的</b>」——「报复对象」+20%、「野兽狂怒」的增伤与",
    "\t * 「濒亡狂怒」的增伤共用的<b>唯一</b>判据，消费点在 {@code Char.damage(int, Object)}。",
    "\t *",
    "\t * <p>两种情况：</p>",
    "\t * <ol>",
    "\t *   <li><b>以英雄本人为源</b>：近战、投掷武器、以及所有写 {@code enemy.damage(dmg, hero)} 的技能，",
    "\t *       {@code src} 就是英雄本人；</li>",
    "\t *   <li><b>法杖</b>：每个法杖的结算都写成 {@code ch.damage(damageRoll(), this)}，",
    "\t *       {@code src} 是<b>法杖实例</b>而不是英雄，所以额外认一类 {@link Wand}。",
    "\t *       这么认是安全的——法杖在游戏里只有英雄会真正打出伤害",
    "\t *       （DM300 只借 {@code WandOfBlastWave.throwChar} 做位移，元素怪走的是",
    "\t *       {@code CursedWand.effect(null, ...)}，都不以 Wand 为伤害源）。</li>",
    "\t * </ol>",
    "\t *",
    "\t * <p>间接伤害不算：点燃地面后 {@code Burning} 的烫伤、中毒、流血等 {@code src} 是那些 buff 自己，",
    "\t * 不属于「英雄造成的伤害」。</p>",
    "\t *",
    "\t * <p><b>为什么不判职业</b>（2026-09-20 修）：这三个天赋都是「谁点了谁受益」的中指长兄天赋，",
    "\t * 但蜕变卷轴能把它们塞给别职业的英雄（{@code ScrollOfMetamorphosis}）——用",
    "\t * {@code hero.heroClass == MIDDLE_FINGER} 当判据会让那些英雄的天赋静默失效。",
    "\t * 现在统一成「{@code src} 就是玩家英雄本人」（{@code src == Dungeon.hero}），与职业无关。",
    "\t * 判据红线见枚举体顶部的说明块。</p>",
    "\t */",
    "\tpublic static boolean isHeroDealtDamage( Object src, Char victim ){",
    "\t\tHero hero = Dungeon.hero;",
    "\t\tif (hero == null) return false;",
    "\t\tif (src instanceof Wand) return true;",
    "\t\treturn src instanceof Hero && src != victim && src == hero;",
    "\t}",
])

line_patch(T, [
    (798, 798, "\t\tif (hero.hasTalent(HUNTING_MEAL)){",
     "\t\tif (hero.heroClass == HeroClass.VALENCINA && hero.hasTalent(HUNTING_MEAL)){"),
    (812, 812, "\t\tif (hero.hasTalent(RECORD_MEAL)){",
     "\t\tif (hero.heroClass == HeroClass.MIDDLE_FINGER && hero.hasTalent(RECORD_MEAL)){"),
    (950, 955, "\t\tFamilyBetrayal.onAttackStarted( hero, enemy );\n",
     "\t\tif (hero.heroClass == HeroClass.MIDDLE_FINGER){"),
    (977, 977, "\t\tif (hero.subClass != HeroSubClass.WAR_HERO) return;",
     "\t\tif (hero.heroClass != HeroClass.VALENCINA || hero.subClass != HeroSubClass.WAR_HERO) return;"),
    (1028, 1032, "\t\tFamilyBetrayal.consumeAfterAttack( hero );",
     "\t\tif (hero.heroClass == HeroClass.MIDDLE_FINGER){"),
    (1053, 1053, "\t\tif (hero == null) return;", MID),
    (1066, 1066, "\t\tif (hero == null) return;", MID),
    (1168, 1170, "\t\tif (hero == null", "\t\tif (hero == null"),
    (1192, 1192, "\t\tif (hero == null) return;", MID),
    (1209, 1232, NEW_METHOD, "\t/**"),
    (1236, 1236, "\t\tif (hero == null) return false;", FALSE),
    (1254, 1254, "\t\tif (hero == null) return 0;", ZERO),
    (1336, 1336, "\t\tif (hero == null) return 1f;", ONE_F),
    (1359, 1359, "\t\tif (hero == null) return duration;",
     "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return duration;"),
    (1375, 1375, "\t\tif (hero == null) return 0;", ZERO),
    (1394, 1394, "\t\tif (hero == null) return 1f;", ONE_F),
    (1428, 1428, "\t\tif (hero == null) return 0f;",
     "		if (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER) return 0f;"),
    (1443, 1443, "\t\tif (hero == null) return 0;", ZERO),
    (1449, 1449, "\t\tint points = (hero == null)",
     "\t\tint points = (hero == null || hero.heroClass != HeroClass.MIDDLE_FINGER)"),
    (1469, 1469, "\t\tif (hero == null) return 1f;", ONE_F),
    (1568, 1568, "\t\tif (hero.subClass != HeroSubClass.FAMILY_SHAME) return;",
     "\t\tif (hero.heroClass != HeroClass.VALENCINA || hero.subClass != HeroSubClass.FAMILY_SHAME) return;"),
    (1576, 1576, "\t\tif (hero == null) return;",
     "		if (hero == null || hero.heroClass != HeroClass.VALENCINA) return;"),
    (1602, 1602, None, "\t\tif (hero.heroClass != HeroClass.VALENCINA) return damage;"),
    (1923, 1923, "\t\tif (hero.hasTalent(HUNT_TARGET)){",
     "\t\tif (hero.heroClass == HeroClass.VALENCINA && hero.hasTalent(HUNT_TARGET)){"),
])

# ============================================================ ① Talent.java 字符串级
# 说明块 + 旧方法名改名 + javadoc 措辞
sub(T, [(
    "public enum Talent {\r\n",
    "public enum Talent {\r\n"
    "\r\n"
    "\t//==========================================================================\r\n"
    "\t// 【判据红线】天赋的生效判据只能是「这英雄身上有没有这门天赋的点数」：\r\n"
    "\t//   hero.hasTalent(X)  或  hero.pointsInTalent(X) > 0\r\n"
    "\t//\r\n"
    "\t// 【绝不要】用 hero.heroClass == HeroClass.XXX 当天赋判定的替身。蜕变卷轴\r\n"
    "\t//   （ScrollOfMetamorphosis）会把别职业的天赋经 Hero.metamorphedTalents 塞进\r\n"
    "\t//   Hero.talents 的同一层槽位 ⇒「战士身上有过人的毅力」是完全合法的状态；\r\n"
    "\t//   职业判定会让这些天赋在别职业身上【静默失效】（点满了却零效果、不报错、无提示）。\r\n"
    "\t//\r\n"
    "\t// 职业判定只在该机制【本身就是该职业独有】时才用（蜕变卷轴给不了，谈不上失效）：\r\n"
    "\t//   核心机制 buff（加速的未来 / 夸耀 / 复仇账簿充能）、盔甲技能、专属装备、\r\n"
    "\t//   子职业分支、台词与音效、图标分发、存档槽与图鉴。\r\n"
    "\t// 回归：_chk/verify_metamorph_talents.py\r\n"
    "\t//==========================================================================\r\n",
    1)])

assert rename(T, "isMiddleFingerDamage", "isHeroDealtDamage") == 2, "Talent.java 残余旧名不是 2 处"

sub(T, [
    ("\t * <p>三个条件：中指长兄本人、当前血量真的等于 0、身上还挂着 {@link GritTeethBuff}。",
     "\t * <p>三个条件：英雄本人（即拥有该天赋者）、当前血量真的等于 0、身上还挂着 {@link GritTeethBuff}。", 1),
    ("\t * <p>由 {@code Char.attack} 的命中分支调用——每次攻击都会走到，所以最前面的职业判定\r\n"
     "\t * 承担廉价短路的职责。</p>\r\n",
     "\t * <p>由 {@code Char.attack} 的命中分支调用——每次攻击都会走到，所以把天赋判定放在\r\n"
     "\t * 最前面承担廉价短路的职责。</p>\r\n", 1),
], tag=" javadoc")

# ============================================================ ② Char.java
assert rename(C, "isMiddleFingerDamage", "isHeroDealtDamage") == 4, "Char.java 旧名不是 4 处"

# ============================================================ ③ Shopkeeper
sub(S, [(
    "\t/** 商店老板处是否出现「购买加速弹」选项（拇指 前二老板 + 已点加速弹药）。 */\r\n"
    "\tpublic static boolean canBuyAccelerationRounds(){\r\n"
    "\t\treturn Dungeon.hero != null\r\n"
    "\t\t\t\t&& Dungeon.hero.heroClass == HeroClass.VALENCINA\r\n"
    "\t\t\t\t&& Dungeon.hero.hasTalent(Talent.ACCEL_AMMO);\r\n"
    "\t}\r\n",
    "\t/** 商店老板处是否出现「购买加速弹」选项（点了「加速弹药」就出现）。\r\n"
    "\t *  <b>不判职业</b>：蜕变卷轴变出来的「加速弹药」也要能用（判据红线见 Talent 枚举体顶部）。 */\r\n"
    "\tpublic static boolean canBuyAccelerationRounds(){\r\n"
    "\t\treturn Dungeon.hero != null\r\n"
    "\t\t\t\t&& Dungeon.hero.hasTalent(Talent.ACCEL_AMMO);\r\n"
    "\t}\r\n",
    1)])
sub(S, [("import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;\r\n", "", 1)])

# ============================================================ ④ AccelerationRound
sub(R, [(
    "\t\t\t//仅 拇指 前二老板「加速弹药」+2 可用液金制造\r\n"
    "\t\t\tif (Dungeon.hero == null\r\n"
    "\t\t\t\t\t|| Dungeon.hero.heroClass != HeroClass.VALENCINA\r\n"
    "\t\t\t\t\t|| !Dungeon.hero.hasTalent( Talent.ACCEL_AMMO )\r\n"
    "\t\t\t\t\t|| Dungeon.hero.pointsInTalent( Talent.ACCEL_AMMO ) < 2){\r\n",
    "\t\t\t//「加速弹药」+2 可用液金制造（不判职业：蜕变卷轴变出来的也算，见 Talent 的判据红线）\r\n"
    "\t\t\tif (Dungeon.hero == null\r\n"
    "\t\t\t\t\t|| !Dungeon.hero.hasTalent( Talent.ACCEL_AMMO )\r\n"
    "\t\t\t\t\t|| Dungeon.hero.pointsInTalent( Talent.ACCEL_AMMO ) < 2){\r\n",
    1)])
sub(R, [("import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;\r\n", "", 1)])

# ============================================================ ⑤ TremblingScorch
sub(K, [(
    "\t\tboolean heroValid = (hero != null && hero.heroClass == HeroClass.VALENCINA\r\n"
    "\t\t\t\t&& hero.subClass == HeroSubClass.WAR_HERO);\r\n",
    "\t\t//子职业本身就是职业独有的（蜕变卷轴不发子职业），按子职业判即可——\r\n"
    "\t\t//不必再叠一层 heroClass == VALENCINA 的替身判定。\r\n"
    "\t\tboolean heroValid = (hero != null && hero.subClass == HeroSubClass.WAR_HERO);\r\n",
    1)])
sub(K, [("import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;\r\n", "", 1)])

print("OK: patch applied to 5 files")
