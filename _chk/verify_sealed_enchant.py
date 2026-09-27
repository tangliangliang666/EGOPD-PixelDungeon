"""封印之剑系列「可附魔 / 可强化」修复的跨文件不变量核验（2026-09-18）。

两个病因，各一条不变量：

① **选不中**：注魔秘卷 / 附魔符石 / 强化符石的可选判据都收敛在
   `ScrollOfEnchantment.enchantable()` 一处。它用 `isUpgradable()` 当「是不是一件正常吃升级资源的装备」
   的代称，而封印之剑系列为了让等级随英雄成长刻意 `isUpgradable()=false` ⇒ 被连坐挡在门外
   （列表整格变灰、点不动、无报错）。修法：像 `SpiritBow` / `BodyArtWeapon` 一样**显式放行**。
   ⚠️ 修法**不是**把 `isUpgradable()` 改成 true —— 那会让升级卷轴重新能选中它、毁掉「随英雄成长」的设计。

② **切形态丢强化**：形态切换（解封 / 落副手封回 / 「即刻处刑」强制换装）都是「造一个新实例再接替槽位」，
   所以没被搬运的字段会静默归零。原先 `copyState()` 漏搬 `Weapon.augment`（强化符石选的方向）
   ⇒ 每次切形态都把符石效果洗掉。

本脚本把这两条钉住。任何一处被改回去（或有人图省事把 `isUpgradable()` 改成 true）都判 FAIL。

用法：python _chk/verify_sealed_enchant.py
"""

import io
import re
import sys

ROOT = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon/"
SOE = ROOT + "items/scrolls/exotic/ScrollOfEnchantment.java"
SSB = ROOT + "items/weapon/melee/SealedSwordBase.java"
MW = ROOT + "items/weapon/melee/MorphWeapon.java"
SCOUT = ROOT + "items/scrolls/ScrollOfUpgrade.java"
STENCH = ROOT + "items/stones/StoneOfEnchantment.java"
STAUG = ROOT + "items/stones/StoneOfAugmentation.java"

fails = []


def fail(msg):
    fails.append(msg)


def read(path):
    with io.open(path, encoding="utf-8") as f:
        return f.read()


def body_of(text, signature):
    """取出某个方法/块的 `{...}` 主体（按大括号配平，够用即可）。"""
    idx = text.find(signature)
    if idx < 0:
        return None
    start = text.find("{", idx)
    if start < 0:
        return None
    depth = 0
    for i in range(start, len(text)):
        if text[i] == "{":
            depth += 1
        elif text[i] == "}":
            depth -= 1
            if depth == 0:
                return text[start:i + 1]
    return None


def squash(text):
    """压掉空白，便于跨行的表达式做子串判定。"""
    return " ".join(text.split())


def strip_comments(text):
    """剥掉块注释与行注释。

    扫「谁调用了 enchantable()」时必须先剥注释：`SealedSwordBase` 的 javadoc 里引用了
    `ScrollOfEnchantment.enchantable()` 说明放行理由，不剥就会被当成一个假的「新调用方」。
    """
    text = re.sub(r"/\*.*?\*/", "", text, flags=re.S)
    return re.sub(r"//[^\n]*", "", text)


def code_of(text, signature):
    """取出方法体并剥掉注释（做「方法里有没有某段代码/调用」这类判据时统一走这里）。

    本文件的方法体里往往带大段中文实现注释，其中会引用被修的表达式原文；不剥注释的话，
    「漏搬 ⇒ 判 FAIL」的反例会被注释里的示例文本判成 PASS（假绿）。
    """
    body = body_of(text, signature)
    return None if body is None else strip_comments(body)


def main():
    soe = read(SOE)
    ssb = read(SSB)
    mw = read(MW)
    scout = read(SCOUT)
    stench = read(STENCH)
    staug = read(STAUG)

    # ---------- ① enchantable() 必须显式放行封印之剑系列 ----------
    ench = code_of(soe, "public static boolean enchantable(")
    if ench is None:
        fail("ScrollOfEnchantment 找不到 enchantable(Item)")
    else:
        flat = squash(ench)
        for must in ["item instanceof SealedSwordBase",
                     "item instanceof SpiritBow",
                     "item instanceof BodyArtWeapon",
                     "item.isUpgradable()"]:
            if must not in flat:
                fail("enchantable() 缺少判据项：%s" % must)
        if "item instanceof DiamondSword" not in flat:
            fail("enchantable() 丢掉了钻石剑的排除（钻石剑只吃专属附魔）")

    # ---------- ② isUpgradable() 必须仍是 false（升级卷轴那条路不能被顺手打开） ----------
    iu = code_of(ssb, "public boolean isUpgradable()")
    if iu is None:
        fail("SealedSwordBase 找不到 isUpgradable() 覆写")
    elif "return false" not in squash(iu):
        fail("SealedSwordBase.isUpgradable() 不再返回 false —— 升级卷轴会重新能选中它，"
             "「等级随英雄成长」的设计被破坏（附魔能用了，但那是因为 enchantable 放行，不是靠打开这个）")

    # ---------- ③ 升级卷轴仍以 isUpgradable() 为门槛（把 ② 的意图钉死） ----------
    if "return item.isUpgradable();" not in scout:
        fail("ScrollOfUpgrade.usableOnItem 不再以 isUpgradable() 为判据——② 的前提变了，请复核")

    # ---------- ④ 三件物品的可选判据仍然都收敛到 enchantable ----------
    for path, name in ((STENCH, "StoneOfEnchantment"), (STAUG, "StoneOfAugmentation")):
        src = read(path)
        if "return ScrollOfEnchantment.enchantable(item);" not in src:
            fail("%s.usableOnItem 不再走 ScrollOfEnchantment.enchantable（修复会失效）" % name)

    # 全仓调用方只应有这三个（多出来的新调用点若要另立判据，必须同步放行封印之剑系列）
    callers = []
    import os
    for dirpath, _, files in os.walk(ROOT):
        for fn in files:
            if fn.endswith(".java"):
                p = os.path.join(dirpath, fn).replace("\\", "/")
                if p.endswith("ScrollOfEnchantment.java"):
                    continue  # 定义处，不算调用方
                try:
                    t = strip_comments(read(p))
                except Exception:
                    continue
                if "enchantable(" in t:
                    callers.append(p[len(ROOT):])
    extra = [c for c in callers
             if not c.endswith("StoneOfEnchantment.java") and not c.endswith("StoneOfAugmentation.java")]
    if extra:
        fail("enchantable() 出现了新的调用方，请确认它也能选中封印之剑系列：%s" % ", ".join(sorted(extra)))

    # ---------- ⑤ 形态切换必须搬运 augment（强化符石的方向） ----------
    cs = code_of(ssb, "private static void copyState(")
    if cs is None:
        fail("SealedSwordBase 找不到 copyState(Weapon, Weapon)")
    elif "to.augment = from.augment;" not in squash(cs):
        fail("SealedSwordBase.copyState() 未搬运 augment —— 每次解封/落副手/即刻处刑换装都会洗掉强化符石效果")

    # 搬运面完整性：Weapon.storeInBundle 里列出的持久字段，copyState 必须一条不落
    if cs is not None:
        for field in ["enchantment", "cursed", "cursedKnown", "levelKnown",
                      "masteryPotionBonus", "enchantHardened", "curseInfusionBonus", "augment"]:
            if ("to.%s = from.%s;" % (field, field)) not in squash(cs):
                fail("copyState() 漏搬 Weapon 的持久字段：%s" % field)

    # 同一缺陷的兄弟实现（漆黑噤默的形态切换）也必须搬运
    mi = code_of(mw, "public static boolean morphInto(")
    if mi is None:
        fail("MorphWeapon 找不到 morphInto(Weapon, Hero, Class)")
    elif "replacement.augment = current.augment;" not in squash(mi):
        fail("MorphWeapon.morphInto() 未搬运 augment —— 漆黑噤默系列切形态同样会洗掉强化符石效果")

    # 对照：嬗变卷轴的 changeWeapon 早就搬了 augment（说明「搬运 augment」是本工程的既定口径）
    if "n.augment = w.augment;" not in read(ROOT + "items/scrolls/ScrollOfTransmutation.java"):
        fail("ScrollOfTransmutation.changeWeapon 不再搬运 augment——请复核「搬运 augment」的既定口径是否已废弃")

    # ---------- 输出 ----------
    if fails:
        print("FAIL (%d)" % len(fails))
        for f in fails:
            print("  -", f)
        sys.exit(1)
    print("ALL PASS：附魔/强化三入口已放行封印之剑系列 + 升级卷轴仍被挡死 + 切形态保留强化符石")


if __name__ == "__main__":
    main()
