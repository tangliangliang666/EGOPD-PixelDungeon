# -*- coding: utf-8 -*-
"""次元撕裂者（三阶自定义武器）接线 + 文本 + 粒子 + 概率成长核验（2026-09-22）。

覆盖七组：

  A. **数值照抄弯刀**：`extends Scimitar`、tier/DLY、不覆写 min/max（面板整套继承）。
  B. **转移（proc）**：挂点、`super.proc` 在触发判定**之前**、
     概率走唯一的 `procChance(buffedLvl())` 入口 × `genericProcChanceMultiplier`、
     50/50 双分支、**不回退**（分支数＝2）、只有英雄亲手挥才开窗、不是真诅咒（不碰 enchantment）。
  C. **空间撕裂窗口（RiftWindow）**：3 回合、`turnsLeft--` 在 `detach()` **之前**、
     三个失效判据、占副槽（setSecondAction 而非 setAction）、`attachTo` 按 stillValid 把关、
     不随存档持久化、落点用 `appear`（无视距离）而非 `teleportToLocation`。
  D. **接线**：ItemSpriteSheet 常量声明**早于** static{} + rect 13×15 + 41 行不撞格；
     Generator.WEP_T3 含新类且 classes 与 defaultProbs **等长**。
  E. **文本**：6 键武器 + 2 键 buff × zh/en（非空 / `_` 成对 / 无 U+FFFD / 无 `/n/`），
     中文名硬断言，且文本里的数值（起点 1/8、上限 1/2、3 回合、`%d%%` 占位符）必须与源码常量一致。
  F. **粒子特效（RiftParticle）**：六色调**逐色回比 items.png 的 xy(6,41) 那一格**
     （判据：alpha 满 + `B ≥ R`；半透明黑描边与两处棕色不算色调 ⇒ 恰好 6 色）、
     明度从白到深紫单调、三个 Factory 都 lightMode、按 `index % 6` 循环取色、
     **不得插值**（不出现 ColorMath）、IMPLODE 速度＝位移÷存活期（正好落到中心），
     以及三处挂点：命中（damage>0）、转移（warp）、空间撕裂（doAction 先记出发点再 appear）。
  G. **概率随等级成长**：按源码三常量（BASE/MAX/DECAY）**复算整条曲线**——
     `+0` 恰为 1/8、`+0→+30` 严格递增、任何等级都 `< 1/2`（渐近不是封顶）、
     增幅逐级递减（指数趋近而非线性）、`+60/+100` 无限贴近 1/2、四个实用等级落在设计区间。

用法:
    python _chk/verify_dimensional_ripper.py
    python _chk/verify_dimensional_ripper.py --selftest   # 反例自测（必跑）

写断言的铁律（见 skill egopd-source-verify）：
  ① 只认稳定概念名（类名、常量名、中文名），不认会被改写的措辞；
  ② 每条判据都要反例自测；
  ③ 判「某方法行为」时先按大括号配平取方法体、**再剥注释**（注释里常引用被修的原文，会喂假绿）；
  ④ 顺序敏感的 bug 必须比 `index()` 位置。
"""
import os
import re
import sys

ROOT = "core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
WEAPON = ROOT + "/items/weapon/melee/DimensionalRipper.java"
BUFF = ROOT + "/actors/buffs/RiftWindow.java"
PART = ROOT + "/effects/particles/RiftParticle.java"
SHEET = ROOT + "/sprites/ItemSpriteSheet.java"
GEN = ROOT + "/items/Generator.java"
ITEMS_PNG = "core/src/main/assets/sprites/items.png"

# 武器贴图那一格（xy(6,41)，1 基）与单格边长
SPRITE_CELL = (6, 41)
CELL_PX = 16
ZH = "core/src/main/assets/messages/items/items_zh.properties"
EN = "core/src/main/assets/messages/items/items.properties"
AZH = "core/src/main/assets/messages/actors/actors_zh.properties"
AEN = "core/src/main/assets/messages/actors/actors.properties"

NAME_ZH = "次元撕裂者"

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
    """取方法体并剥注释（铁律 4）。"""
    b = body_of(src, sig)
    return strip_comments(b) if b else None


def static_block_line(src, const_name):
    """返回包含 assignItemRect(const_name, ...) 的那个 `static {` 的行号（常量必须早于它）。"""
    m = re.search(r"assignItemRect\(\s*" + re.escape(const_name) + r"\s*,", src)
    if not m:
        return None
    before = src[:m.start()]
    blocks = [b.start() for b in re.finditer(r"static\s*\{", before)]
    if not blocks:
        return None
    return before[:blocks[-1]].count("\n") + 1


# --------------------------------------------------------------------------
# A. 数值照抄弯刀
# --------------------------------------------------------------------------
def part_a():
    print("\n=== A. 数值与形象（整套照抄弯刀）===")
    s = read(WEAPON)

    check(re.search(r"public\s+class\s+DimensionalRipper\s+extends\s+Scimitar\b", s) is not None,
          "DimensionalRipper extends Scimitar（面板/攻击延迟/力量需求整套继承）",
          "父类不是 Scimitar ⇒ 数值不再与弯刀一致")

    check(re.search(r"image\s*=\s*ItemSpriteSheet\.DIMENSIONAL_RIPPER\s*;", s) is not None,
          "image 指向 ItemSpriteSheet.DIMENSIONAL_RIPPER")

    check(re.search(r"tier\s*=\s*3\s*;", s) is not None
          and re.search(r"DLY\s*=\s*0\.8f\s*;", s) is not None,
          "tier=3 / DLY=0.8f（与弯刀同为 1.25 倍速）")

    cod = strip_comments(s)
    check(re.search(r"\bint\s+min\s*\(\s*int\s+\w+\s*\)", cod) is None
          and re.search(r"\bint\s+max\s*\(\s*int\s+\w+\s*\)", cod) is None,
          "不覆写 min()/max() ⇒ 面板严格等于弯刀（16+4L / 3+L）",
          "出现覆写即可能偏离弯刀数值")


# --------------------------------------------------------------------------
# B. 转移（proc）
# --------------------------------------------------------------------------
def part_b():
    print("\n=== B. 转移：命中时的双向随机传送 ===")
    s = read(WEAPON)

    #概率随等级成长（2026-09-22 用户定：+0 = 1/8，随等级趋近 1/2）。
    #这里只断言结构，真正的数值复算在 part_g()。
    m = re.search(r"PROC_CHANCE_BASE\s*=\s*(\d+)/(\d+)f\s*;", s)
    check(m is not None and m.group(1) == "1" and m.group(2) == "8",
          "PROC_CHANCE_BASE = 1/8f（+0 时的概率，2026-09-22 用户定）",
          "起点常量不在，或不再是 1/8")
    m2 = re.search(r"PROC_CHANCE_MAX\s*=\s*(\d+)/(\d+)f\s*;", s)
    check(m2 is not None and m2.group(1) == "1" and m2.group(2) == "2",
          "PROC_CHANCE_MAX = 1/2f（渐近上限）",
          "上限常量不在，或不再是 1/2")
    m3 = re.search(r"PROC_CHANCE_DECAY\s*=\s*(\d+)/(\d+)f\s*;", s)
    if m3:
        ok("PROC_CHANCE_DECAY = %s/%s ∈ (0,1)" % (m3.group(1), m3.group(2)))
    check(m3 is not None and 0 < int(m3.group(1)) < int(m3.group(2)),
          "衰减系数落在 (0,1)（每级吃掉 1/8 的差距）",
          "缺失或 ≥ 1 ⇒ 概率永远停在 1/8，等于没有成长")

    #旧的那种「一个定值常量」必须彻底退场，否则改了公式却漏掉某个引用点会静默半生效
    check(re.search(r"PROC_CHANCE(?![_A-Z])", s) is None,
          "旧的裸常量 PROC_CHANCE 已彻底退场（没有半改的引用点）",
          "还有地方在引用裸 PROC_CHANCE ⇒ 那处仍是固定 1/8")

    check(re.search(r"public\s+static\s+float\s+procChance\s*\(\s*int\s+\w+\s*\)", s) is not None,
          "有 procChance(int) 这个唯一概率入口（proc 判定与面板文本共用一处）",
          "缺了它 ⇒ 面板文本没法显示当前等级的真实概率")

    pb = code_of(s, r"public\s+int\s+proc\s*\(\s*Char\s+\w+\s*,\s*Char\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    if pb is None:
        bad("DimensionalRipper.proc() 取不到方法体")
        return

    i_super = pb.find("super.proc(")
    i_roll = pb.find("procChance(")
    check(i_super != -1 and i_roll != -1 and i_super < i_roll,
          "proc()：super.proc() 在触发判定**之前**（先走完原版附魔/护盾结算再判转移）",
          "两段 code 都在，只比存在是空测；必须比 index")

    check(re.search(r"procChance\s*\(\s*buffedLvl\s*\(\s*\)\s*\)", pb) is not None,
          "判定喂进去的是**当前武器等级** buffedLvl()（与 BattleAxe/DaCapo 等原版惯例一致）",
          "喂了别的等级源 ⇒ 概率不会随强化成长，或与项目「按等级算数值」的惯例脱节")

    check("genericProcChanceMultiplier" in pb,
          "概率乘 genericProcChanceMultiplier（奥术之环/狂暴等加成同样生效）")

    check(re.search(r"Random\.Float\(\)\s*<", pb) is not None,
          "用 Random.Float() 掷概率（与原版诅咒同一写法）")

    check(re.search(r"Random\.Int\(\s*2\s*\)\s*==\s*0", pb) is not None,
          "50/50：Random.Int(2) == 0 决定传目标还是传自己")

    check(pb.count("warp(") == 2,
          "warp() 恰好调用 2 次（目标一次 + 自己一次）——**没有**「传不动就回退给另一方」的第三条路",
          "出现第 3 次调用 ⇒ 掷到的对象没传成时会去传另一方，概率不再是 50/50")

    check(pb.find("warp(defender") != -1 and pb.find("warp(attacker") != -1,
          "两个分支分别把目标 / 攻击者交给 warp()")

    check("attacker == Dungeon.hero" in pb,
          "只有英雄亲手挥这把武器时才开窗（按钮长在英雄身上）")

    check("RiftWindow.class" in pb and ".open(defender)" in pb,
          "传送成功后给英雄开 RiftWindow，并把该次被攻击的敌人记成窗口目标")

    check("defender.isAlive()" in pb and "MagicImmune" in pb and "damage > 0" in pb,
          "触发前照原版诅咒判三项：魔法免疫 / 目标存活 / 真造成了伤害")

    wb = code_of(s, r"private\s+static\s+boolean\s+warp\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if wb is None:
        bad("warp() 取不到方法体")
    else:
        check("Char.hasProp(" in wb and "IMMOVABLE" in wb,
              "warp() 先判 IMMOVABLE（雕像/炮台等不可传送者直接跳过）")
        check("isImmune(" in wb, "warp() 先判免疫传送")
        check("ScrollOfTeleportation.teleportChar(" in wb, "warp() 走原版 teleportChar 随机落点")
        check("return false" in wb, "warp() 失败时明确返回 false（调用方据此判定「没传成」）")
        check(re.search(r"\w+\.state\s*==\s*\w+\.HUNTING", wb) is not None
              and re.search(r"\w+\.state\s*=\s*\w+\.WANDERING", wb) is not None,
              "被传走的怪物从「追踪」退回「游荡」（否则下一回合直线跑回来，等于没传）",
              "本作的 HUNTING/WANDERING 是每个怪物各自一份的 AiState 实例字段，只能经实例比"
              "（写成 Mob.HUNTING 会编译不过）")

    check("enchantment" not in strip_comments(s),
          "不是真诅咒：全程不碰 enchantment 字段（可正常卸下、正常附魔，解咒洗不掉转移）",
          "出现 enchantment = ... 即变成真诅咒")

    eb = code_of(s, r"public\s+static\s+DimensionalRipper\s+equipped\s*\(\s*Hero\s+\w+\s*\)\s*\{")
    check(eb is not None and "weapon()" in eb and "secondWep()" in eb,
          "equipped() 主手/副手都查（供窗口校验与按钮取图）")

    ub = code_of(s, r"public\s+boolean\s+doUnequip\s*\(\s*Hero\s+\w+\s*,\s*boolean\s+\w+\s*,\s*boolean\s+\w+\s*\)\s*\{")
    check(ub is not None and "Buff.detach(" in ub and "RiftWindow.class" in ub,
          "doUnequip 时若手上已无此武器 ⇒ 立即作废窗口")


# --------------------------------------------------------------------------
# C. 空间撕裂窗口
# --------------------------------------------------------------------------
def part_c():
    print("\n=== C. 空间撕裂窗口（RiftWindow）===")
    s = read(BUFF)

    check(re.search(r"public\s+class\s+RiftWindow\s+extends\s+Buff\s+implements\s+ActionIndicator\.Action", s) is not None,
          "RiftWindow extends Buff implements ActionIndicator.Action（照 Charger/SwordSwap 的写法）")

    check(re.search(r"WINDOW_TURNS\s*=\s*3\s*;", s) is not None,
          "WINDOW_TURNS = 3")

    ob = code_of(s, r"public\s+void\s+open\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if ob is None:
        bad("RiftWindow.open() 取不到方法体")
    else:
        check("timeToNow()" in ob,
              "open() 调 timeToNow()：把调度时间拉回 now，触发当回合就扣掉一格",
              "新挂载本就是 now（空操作）；少了它，**重复触发**（buff 已存在、time 已被 spend 推进）会白送一格 ⇒ 窗口变 4 回合")
        check("keepSlot()" in ob, "open() 立刻占住副槽（经 keepSlot() 收口 setSecondAction）")

    vb = code_of(s, r"public\s+boolean\s+stillValid\s*\(\s*\)\s*\{")
    if vb is None:
        bad("stillValid() 取不到方法体")
    else:
        check("equipped(" in vb, "仍成立判据①：英雄还握着这把武器")
        check("openLevel != Dungeon.level" in vb, "仍成立判据②：没换层")
        check("isAlive()" in vb, "仍成立判据③：目标还活着")

    ab = code_of(s, r"public\s+boolean\s+act\s*\(\s*\)\s*\{")
    if ab is None:
        bad("RiftWindow.act() 取不到方法体")
    else:
        i_dec = ab.find("turnsLeft--")
        i_det = ab.find("detach()")
        check(i_dec != -1 and i_det != -1 and i_dec < i_det,
              "act()：turnsLeft-- 在 detach() **之前**（先扣格再判是否关窗）",
              "顺序反了会一次就关窗或永远不关")
        check("!stillValid()" in ab, "act() 每回合用 stillValid() 兜住「目标死了/换层/武器离手」")
        check("spend(TICK)" in ab, "act() 用 spend(TICK) 按回合记账")
        check("keepSlot()" in ab, "act() 每回合重新占位（可能被别的动作挤掉，经 keepSlot()）")

    at = code_of(s, r"public\s+boolean\s+attachTo\s*\(\s*Char\s+\w+\s*\)\s*\{")
    check(at is not None and "stillValid()" in at,
          "attachTo 按 stillValid() 把关 ⇒ 读档恢复出来的空窗口不会挂出一个假按钮")

    db = code_of(s, r"public\s+void\s+detach\s*\(\s*\)\s*\{")
    check(db is not None and "clearAction(this)" in db,
          "detach() 里 clearAction(this)（只在槽位确实是自己时才清）")

    cod = strip_comments(s)
    check("ActionIndicator.setAction(" not in cod,
          "占的是**副槽**（setSecondAction），不抢主槽上职业技能按钮的位置")
    check("teleportToLocation" not in cod,
          "落点用 appear 而非 teleportToLocation（后者要过可达性判定，而这里要的正是无视距离）")
    check("storeInBundle" not in cod and "restoreFromBundle" not in cod,
          "刻意不重写存档方法：窗口锁定的 Char 引用无法随存档搬运，读档后自然失效",
          "加了 store/restore 反而会把一个取不到目标的窗口恢复出来")

    ib = code_of(s, r"public\s+int\s+icon\s*\(\s*\)\s*\{")
    check(ib is not None and "BuffIndicator.NONE" in ib,
          "icon() 返回 NONE（不占 buff 条，窗口只靠右下角按钮表达）")

    an = code_of(s, r"public\s+String\s+actionName\s*\(\s*\)\s*\{")
    check(an is not None and 'Messages.get(DimensionalRipper.class, "space_tear")' in an,
          "按钮名字取自武器文本键 space_tear")

    do = code_of(s, r"public\s+void\s+doAction\s*\(\s*\)\s*\{")
    if do is None:
        bad("doAction() 取不到方法体")
    else:
        check("hero.ready" in do and "!stillValid()" in do,
              "doAction 开头复核「英雄可行动 + 窗口仍成立」（按钮是渲染线程按下的）")
        i_cell = do.find("emptyCellAround(")
        i_or = do.find("== -1")
        i_guard = do.find("emptyCellAround(")
        i_busy = do.find("hero.busy()")
        i_appear = do.find("ScrollOfTeleportation.appear(")
        i_atk = do.find(".sprite.attack(")
        i_det = do.rfind("detach()")   # 取末尾那次；doAction 开头还有一次守卫用的 detach()
        check(i_guard != -1 and i_or != -1 and i_cell < i_or,
              "先算落点，算不出（-1）就提示并退出，不吃掉窗口")
        check(i_busy != -1 and i_appear != -1 and i_atk != -1 and i_busy < i_appear < i_atk,
              "顺序：hero.busy() → 传送 → 攻击动作（照 ArtTechniques 的 DISSECT）",
              "顺序错了会中途解锁回合线程，演出与结算错位")
        check(i_det != -1 and i_atk < i_det,
              "攻击动作挂上之后才 detach()（一次性：用掉即关窗）")
        check("hero.spendAndNext(" in do and "attackDelay()" in do,
              "攻击回调里 spendAndNext(hero.attackDelay()) 收尾（＝一次普通攻击的时间）")

    eb = code_of(s, r"private\s+static\s+int\s+emptyCellAround\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if eb is None:
        bad("emptyCellAround() 取不到方法体")
    else:
        check("NEIGHBOURS8" in eb and "insideMap" in eb and "passable" in eb and "findChar" in eb,
              "emptyCellAround 按 8 邻：地图内 + 可站立 + 没人占（照 ArtTechniques）")
        check("return -1" in eb, "没有可用落点时返回 -1")


# --------------------------------------------------------------------------
# D. 接线（贴图 + 掉落池）
# --------------------------------------------------------------------------
def part_d():
    print("\n=== D. 接线：贴图登记与掉落池 ===")
    s = read(SHEET)

    dm = re.search(r"int\s+DIMENSIONAL_RIPPER\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)", s)
    check(dm is not None and (int(dm.group(1)), int(dm.group(2))) == (6, 41),
          "DIMENSIONAL_RIPPER = xy(6, 41)（用户已绘的那一格）",
          "实际 %s" % (dm.group(0) if dm else "(缺失)"))

    rm = re.search(r"assignItemRect\(\s*DIMENSIONAL_RIPPER\s*,\s*(\d+)\s*,\s*(\d+)\s*\)", s)
    check(rm is not None and (int(rm.group(1)), int(rm.group(2))) == (13, 15),
          "assignItemRect(DIMENSIONAL_RIPPER, 13, 15)（与 items.png 里那格的 13×15 内容一致）",
          "实际 %s" % (rm.group(0) if rm else "(缺失)"))

    # 顺序陷阱：常量必须在 assignItemRect 所在的 static{} 之前
    if dm is None or rm is None:
        bad("常量或 rect 缺失，跳过顺序判定")
    else:
        decl_line = s[:dm.start()].count("\n") + 1
        blk = static_block_line(s, "DIMENSIONAL_RIPPER")
        check(blk is not None and decl_line < blk,
              "常量声明（第 %d 行）早于使用它的 static{}（第 %s 行）"
              % (decl_line, blk),
              "静态初始化按源码顺序执行：常量写在块后 ⇒ 该常量为 0 ⇒ **所有物品图标整体错位且不报错**")

    # 41 行不撞格（只看 41 行 —— 别行的重复多是内部类自有坐标系，非真撞格）
    decl = {}
    for m in re.finditer(r"int\s+([A-Z][A-Z0-9_]*)\s*=\s*xy\(\s*(\d+)\s*,\s*(\d+)\s*\)", s):
        decl.setdefault((int(m.group(2)), int(m.group(3))), []).append(m.group(1))
    dup = ["(%d,%d): %s" % (c, r, "/".join(n)) for (c, r), n in sorted(decl.items())
           if len(n) > 1 and r == 41]
    check(not dup, "41 行内无 xy 撞格", "撞格：%s" % dup)

    g = read(GEN)
    check("DimensionalRipper.class" in g, "Generator 已 import 并登记 DimensionalRipper")
    cm = re.search(r"WEP_T3\.classes\s*=\s*new\s+Class<\?>\s*\[\s*\]\s*\{([^}]*)\}", g)
    pm = re.search(r"WEP_T3\.defaultProbs\s*=\s*new\s+float\s*\[\s*\]\s*\{([^}]*)\}", g)
    if cm is None or pm is None:
        bad("Generator 里 WEP_T3 的 classes/defaultProbs 取不到")
    else:
        n_cls = len([x for x in cm.group(1).split(",") if x.strip()])
        n_prob = len([x for x in pm.group(1).split(",") if x.strip()])
        check(n_cls == n_prob,
              "WEP_T3.classes(%d) 与 defaultProbs(%d) **等长**" % (n_cls, n_prob),
              "不等长 ⇒ 末尾武器静默永不掉落（probs 是并列权重，不是累加）")
        check("DimensionalRipper.class" in cm.group(1), "次元撕裂者确实在 WEP_T3.classes 里")


# --------------------------------------------------------------------------
# E. 文本
# --------------------------------------------------------------------------
def load_props(path):
    d = {}
    for line in read(path).splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        if "=" not in line:
            d.setdefault("__BROKEN__", []).append(line)
            continue
        k, v = line.split("=", 1)
        d[k.strip()] = v
    return d


W_KEYS = ["name", "desc", "stats_desc", "space_tear", "no_space", "window_open"]
B_KEYS = ["name", "desc"]


def part_e():
    print("\n=== E. 文本（zh 权威 / en 同样必须齐全）===")
    iz, ie = load_props(ZH), load_props(EN)
    az, ae = load_props(AZH), load_props(AEN)

    check("__BROKEN__" not in iz, "items_zh.properties 无「缺 = 的断行」（续行没被落成真换行）")
    check("__BROKEN__" not in ie, "items.properties 无「缺 = 的断行」")
    check("__BROKEN__" not in az, "actors_zh.properties 无「缺 = 的断行」")
    check("__BROKEN__" not in ae, "actors.properties 无「缺 = 的断行」")

    total = 0
    for slug, keys, base in (("dimensionalripper", W_KEYS, "items.weapon.melee"),
                             ("riftwindow", B_KEYS, "actors.buffs")):
        for key in keys:
            full = "%s.%s.%s" % (base, slug, key)
            total += 1
            for label, d in (("zh", iz if base.startswith("items") else az),
                             ("en", ie if base.startswith("items") else ae)):
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
    ok("共 %d 键 × 2 语言齐备、非空、无 U+FFFD、无 /n/、_ 成对" % total)

    check(iz.get("items.weapon.melee.dimensionalripper.name") == NAME_ZH,
          "中文名硬断言 = %s" % NAME_ZH,
          "实际 %r" % iz.get("items.weapon.melee.dimensionalripper.name"))
    check(az.get("actors.buffs.riftwindow.name") == "空间裂隙",
          "buff 中文名硬断言 = 空间裂隙",
          "实际 %r" % az.get("actors.buffs.riftwindow.name"))

    # 文本里的数值必须与源码常量一致（防「改代码不改文本」）
    wsrc = read(WEAPON)
    bsrc = read(BUFF)
    base = re.search(r"PROC_CHANCE_BASE\s*=\s*(\d+)/(\d+)f", wsrc)
    cmax = re.search(r"PROC_CHANCE_MAX\s*=\s*(\d+)/(\d+)f", wsrc)
    turns = re.search(r"WINDOW_TURNS\s*=\s*(\d+)", bsrc)
    check(base is not None and cmax is not None and turns is not None,
          "源码里 PROC_CHANCE_BASE / PROC_CHANCE_MAX / WINDOW_TURNS 都取得到")
    if base and cmax and turns:
        lo = "%s/%s" % (base.group(1), base.group(2))
        hi = "%s/%s" % (cmax.group(1), cmax.group(2))
        n_turns = turns.group(1)
        for label, d in (("zh", iz), ("en", ie)):
            v = d.get("items.weapon.melee.dimensionalripper.stats_desc", "")
            check(lo in v, "%s 的 stats_desc 写着起点概率 %s（与 PROC_CHANCE_BASE 一致）" % (label, lo),
                  "文本与源码常量脱节（改代码不改文本）")
            check(hi in v, "%s 的 stats_desc 写着渐近上限 %s（与 PROC_CHANCE_MAX 一致）" % (label, hi),
                  "文本与源码常量脱节")
            check(n_turns in v, "%s 的 stats_desc 写着回合数 %s（与 WINDOW_TURNS 一致）" % (label, n_turns),
                  "文本与源码常量脱节")
            #带参文本：占位符要 %d、字面百分号要写成 %%。少写一个 % 会让 String.format 抛异常，
            #而 Messages 的兜底是「整串回退成资源键原文」——游戏里表现为面板直接显示一串键名
            check("%d%%" in v,
                  "%s 的 stats_desc 带 %%d%%%% 占位符（用来显示当前等级的概率）" % label,
                  "缺占位符 ⇒ statsInfo() 传进去的 int 无处落地；"
                  "或字面 % 没写成 %% ⇒ format 抛异常后整串回退成键名")

    # 面板文本必须真的把「当前等级」算出来填进去（否则永远显示写死的 +0 值）
    check(re.search(r"public\s+String\s+statsInfo\s*\(\s*\)", wsrc) is not None
          and 'Messages.get(this, "stats_desc",' in wsrc,
          "覆写 statsInfo() 并把当前概率传进 stats_desc（强化后在面板上看得见变化）",
          "不覆写 ⇒ 面板永远显示 +0 那个数，等级成长在 UI 上完全不可见")


# --------------------------------------------------------------------------
# F. 粒子特效（RiftParticle）
# --------------------------------------------------------------------------
def sprite_cell_colors():
    """采样 items.png 的 xy(6,41) 那格，返回 (该格出现的全部颜色, 其中的「紫色系」色调)。

    紫色系判据（两条都要满足）：
      * ``alpha == 255`` —— 半透明纯黑是像素画的外描边，不是色调；
      * ``B >= R``       —— 六种紫都满足；刀柄那两处棕色都是 ``R > B``。

    返回值里 `全部颜色` 是 ``(r,g,b,a)`` 元组（要连 alpha 一起看），
    `紫色系` 统一压成 ``0xRRGGBB`` **整数** —— 好跟 Java 里的 COLORS 直接比集合。
    （别一边元组一边整数去比：两边 repr 都是 `#FFFFFF`，判据会静默假红/假绿。）

    PNG 解码复用既有 ``verify_sprite_frames.decode_png``（纯 Python，无 PIL）。
    """
    sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
    import verify_sprite_frames as vsf
    w, h, ct, nch, plte, trns, px = vsf.decode_png(ITEMS_PNG)
    cols = set()
    for dy in range(CELL_PX):
        for dx in range(CELL_PX):
            x = (SPRITE_CELL[0] - 1) * CELL_PX + dx
            y = (SPRITE_CELL[1] - 1) * CELL_PX + dy
            r, g, b, a = vsf.pixel(px, ct, nch, plte, trns, x, y, w)
            if a > 0:
                cols.add((r, g, b, a))
    purple = set(pack((r, g, b)) for (r, g, b, a) in cols if a == 255 and b >= r)
    return cols, purple


def palette_of(src):
    """取 Java 里 `COLORS = { 0x……, … }` 的整数值列表（取不到返回 None）。"""
    m = re.search(r"COLORS\s*=\s*\{([^}]*)\}", src)
    if m is None:
        return None
    return [int(x, 16) for x in re.findall(r"0x([0-9A-Fa-f]{6})", m.group(1))]


def pack(rgb):
    """(r,g,b) → 0xRRGGBB。

    **别拿 (r,g,b) 元组去和 0xRRGGBB 比集合**：``set([(255,255,255)]) == set([0xFFFFFF])``
    是 False，而两边的 repr 都是 ``#FFFFFF``，判据会静默假红（本脚本 2026-09-22 真踩过）。
    """
    r, g, b = rgb
    return (r << 16) | (g << 8) | b


def hexa(v):
    """int / (r,g,b[,a]) → '#RRGGBB'。

    注意 `%` 要喂**元组**：直接喂 int 会在**构造反例文案时**就抛 TypeError，
    连断言本身一起掩盖掉（本脚本的 check 会先算好反例文案再判）——所以统一走这个函数。
    """
    if isinstance(v, tuple):
        return "#%02X%02X%02X" % v[:3]
    return "#%02X%02X%02X" % ((v >> 16) & 255, (v >> 8) & 255, v & 255)


def luminance(v):
    return 0.299 * ((v >> 16) & 255) + 0.587 * ((v >> 8) & 255) + 0.114 * (v & 255)


def part_f():
    print("\n=== F. 粒子特效（RiftParticle：贴图取色 + 命中/传送挂点）===")
    s = read(PART)
    code = strip_comments(s)

    check(re.search(r"class\s+RiftParticle\s+extends\s+PixelParticle", s) is not None,
          "RiftParticle extends PixelParticle（1×1 纯色像素粒子，照 PinkParticle / TearSwordTrailParticle）")

    # --- 配色 ---
    vals = palette_of(s)
    if vals is None:
        bad("RiftParticle.COLORS 取不到")
        return

    check(len(vals) == 6,
          "COLORS 恰好 6 个色调（贴图里除半透明黑描边与两处棕色外的全部色调）",
          "实际 %d 个：%s" % (len(vals), [hexa(v) for v in vals]))
    check(all(0 <= v <= 0xFFFFFF for v in vals), "COLORS 都是合法 0xRRGGBB")

    lums = [luminance(v) for v in vals]
    check(all(lums[i] > lums[i + 1] for i in range(len(lums) - 1)),
          "六色按明度**从白到深紫**单调排列（首＝白，尾＝最深）",
          "实际明度 %s" % [round(x) for x in lums])

    # 与贴图逐色比对 —— 「配色确实取自贴图」的硬证据
    try:
        cols, purple = sprite_cell_colors()
    except Exception as e:
        bad("采样 %s 失败（需要 _chk/verify_sprite_frames.py 提供 decode_png）：%s" % (ITEMS_PNG, e))
        cols = purple = None
    if purple is not None:
        check(len(cols) == 9,
              "该格共 9 种颜色 = 6 种紫 + 1 种半透明黑描边 + 2 处棕色（与「恰好六种色调」的说法一致）",
              "实得 %d 种：%s（贴图若重画过，这里与 COLORS 都应同步更新）"
              % (len(cols), sorted("#%02X%02X%02X/a%d" % c for c in cols)))
        check(len(purple) == 6,
              "该格里 alpha 满且 B ≥ R 的色调恰好 6 种",
              "实得 %d 种：%s" % (len(purple), sorted(hexa(c) for c in purple)))
        check(set(vals) == purple,
              "COLORS 与 items.png xy(6,41) 的 6 种紫**逐色一致**",
              "缺 %s / 多 %s" % (sorted(hexa(c) for c in purple - set(vals)),
                                sorted(hexa(c) for c in set(vals) - purple)))

    # --- 三个 Factory ---
    for name, reset in (("HIT", "resetHit"), ("SCATTER", "resetScatter"), ("IMPLODE", "resetImplode")):
        fab = code_of(s, r"Factory\s+" + name + r"\s*=\s*new\s+Factory\s*\(\s*\)\s*\{")
        if fab is None:
            bad("RiftParticle.%s 工厂体取不到" % name)
            continue
        check(reset + "(" in fab and "recycle(" in fab,
              "%s 工厂：recycle(RiftParticle.class) 回收槽位后走 %s()" % (name, reset))
        check(re.search(r"boolean\s+lightMode\s*\(\s*\)\s*\{\s*return\s+true\s*;", fab) is not None,
              "%s 工厂 lightMode()＝true（叠加混合，紫色才有「裂隙发光」味）" % name)

    for name in ("resetHit", "resetScatter", "resetImplode"):
        check(re.search(r"public\s+void\s+" + name + r"\s*\([^)]*\)\s*\{", s) is not None,
              "%s(…) 是 public（工厂在外部匿名类里调用）" % name)

    # --- 取色规则 ---
    check(re.search(r"index\s*%\s*COLORS\.length", code) is not None,
          "起始色阶按发射序号循环取（index % COLORS.length）：一次喷够 6 颗则六色必定全出场",
          "改成随机取色，就可能一整蓬全是深紫，看不出白→深紫的层次")
    check("ColorMath" not in code and "interpolate" not in code,
          "不插值取色（代码里没有 ColorMath / interpolate）：画面出现的永远只是这六种颜色",
          "插值会混出贴图之外的中间色，就不再是「这六种」了")
    check(re.search(r"am\s*=\s*1f", code) is not None,
          "各 reset 手动复位 am = 1f（revive() 不还原透明度，不复位会在重生首帧闪一下）")

    ub = code_of(s, r"public\s+void\s+update\s*\(\s*\)\s*\{")
    if ub is None:
        bad("RiftParticle.update() 取不到方法体")
    else:
        check("super.update()" in ub, "update() 先 super.update() 让基类扣寿命（寿终自动 kill）")
        check("Math.max(0" in ub,
              "淡出系数钳到 0（寿终那一帧 left ≤ 0，不钳会算出负边长）")
        check(re.search(r"color\(\s*COLORS\[", ub) is not None,
              "update() 的取色只能来自 COLORS 数组")

    ib = code_of(s, r"public\s+void\s+resetImplode\s*\([^)]*\)\s*\{")
    if ib is None:
        bad("RiftParticle.resetImplode() 取不到方法体")
    else:
        check("Math.cos(" in ib and "Math.sin(" in ib,
              "IMPLODE 从中心外围随机一点出发（cos/sin 取方位）")
        check(re.search(r"scale\(\s*1f\s*/\s*lifespan\s*\)", ib) is not None,
              "IMPLODE 速度＝位移 ÷ 存活期（scale(1f / lifespan)）⇒ 正好在寿终那帧抵达落点，不越过",
              "固定速度会冲过头，粒子穿过中心又飞出去")

    # --- 武器侧挂点 ---
    w = read(WEAPON)
    wcode = strip_comments(w)

    check("effects.particles.RiftParticle" in w, "DimensionalRipper 已 import RiftParticle")
    check("Speck" not in wcode,
          "原来的 Speck 白色光点已撤（传送演出整体交给裂隙粒子，避免两套粒子叠在同一处）")

    hm = re.search(r"HIT_PARTICLES\s*=\s*(\d+)", w)
    wm = re.search(r"WARP_PARTICLES\s*=\s*(\d+)", w)
    check(hm is not None and int(hm.group(1)) >= 6,
          "命中粒子数 HIT_PARTICLES(%s) ≥ 6 ⇒ 一次命中六色必定全出场" % (hm.group(1) if hm else "?"),
          "少于 6 颗时按 index 循环取色会漏掉最深那几档紫（4 颗只覆盖 5 色）")
    check(wm is not None and int(wm.group(1)) >= 6,
          "传送粒子数 WARP_PARTICLES(%s) ≥ 6 ⇒ 一次传送六色必定全出场" % (wm.group(1) if wm else "?"),
          "少于 6 颗时按 index 循环取色会漏掉后几档紫")

    pb = code_of(w, r"public\s+int\s+proc\s*\(\s*Char\s+\w+\s*,\s*Char\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    if pb is None:
        bad("proc() 取不到方法体")
    else:
        i_dmg = pb.find("damage > 0")
        i_fx = pb.find("hitFx(")
        check(i_fx != -1, "命中时出粒子（proc 里调 hitFx）")
        check(i_dmg != -1 and i_fx != -1 and i_dmg < i_fx,
              "只有真打出伤害才出粒子（damage > 0 在 hitFx **之前**判）",
              "顺序反了会连「被完全格挡的 0 伤害」也炸一蓬粒子")

    wb = code_of(w, r"private\s+static\s+boolean\s+warp\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if wb is None:
        bad("warp() 取不到方法体")
    else:
        i_tel = wb.find("teleportChar(")
        i_fx2 = wb.find("teleportFx(")
        check(i_fx2 != -1 and i_tel != -1 and i_tel < i_fx2,
              "warp() 里传送成功之后才出粒子（teleportFx 在 teleportChar **之后**）")
        check(re.search(r"teleportFx\(\s*oldPos\s*,", wb) is not None,
              "warp() 传的起点是**传送前**存下的 oldPos（不是传完再读的 pos）",
              "传完再读 pos 会拿到新位置 ⇒ 起点粒子出在落点上，等于没炸散")

    hb = code_of(w, r"private\s+static\s+void\s+hitFx\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if hb is None:
        bad("hitFx() 取不到方法体")
    else:
        check("sprite == null" in hb and "!= null" in hb and "centerEmitter()" in hb,
              "hitFx 对「目标没贴图 / 拿不到发射器」都有兜底（CharSprite.emitter() 可能返回 null）")
        check("RiftParticle.HIT" in hb and "HIT_PARTICLES" in hb,
              "hitFx 用 RiftParticle.HIT × HIT_PARTICLES")

    tb = code_of(w, r"public\s+static\s+void\s+teleportFx\s*\(\s*int\s+\w+\s*,\s*int\s+\w+\s*\)\s*\{")
    if tb is None:
        bad("teleportFx() 取不到方法体")
    else:
        i_sc = tb.find("RiftParticle.SCATTER")
        i_im = tb.find("RiftParticle.IMPLODE")
        check(i_sc != -1 and i_im != -1,
              "teleportFx：起点 SCATTER（炸散）＋ 终点 IMPLODE（汇聚）")
        check(i_sc != -1 and i_im != -1 and i_sc < i_im, "teleportFx：先起点后终点")
        check("CellEmitter.get(" in tb and "CellEmitter.center(" in tb,
              "起点撒满整格（get）、终点汇聚到格心一个点（center）",
              "终点若用 get，IMPLODE 的「位移÷存活期」算的就不是它要奔的那个点")
        check("heroFOV" in tb, "两端各自只在英雄视野内才出（看不到的地方不浪费粒子）")
        check("Dungeon.level == null" in tb,
              "teleportFx 有关卡兜底（CellEmitter.get 在无场景时会 NPE）")

    # --- 空间撕裂也要出粒子 ---
    b = read(BUFF)
    do = code_of(b, r"public\s+void\s+doAction\s*\(\s*\)\s*\{")
    if do is None:
        bad("RiftWindow.doAction() 取不到方法体")
    else:
        i_from = do.find("hero.pos")
        i_appear = do.find("ScrollOfTeleportation.appear(")
        i_fx3 = do.find("teleportFx(")
        check(i_appear != -1 and i_fx3 != -1 and i_appear < i_fx3,
              "空间撕裂：传送（appear）之后才出粒子")
        check(i_from != -1 and i_appear != -1 and i_from < i_appear,
              "空间撕裂：**先**记下出发点再 appear（appear 会把 hero.pos 改写成落点）",
              "顺序反了 ⇒ 起点粒子出在落点上，等于没炸散")
        check("DimensionalRipper.teleportFx(" in do,
              "与武器自己的「转移」共用同一个入口（两处传送看起来才是同一件事）")


# --------------------------------------------------------------------------
# G. 概率公式：+0 = 1/8，随等级单调趋近 1/2（数值复算）
# --------------------------------------------------------------------------
def chance_func(src):
    """按源码里的三个常量重建概率函数；任一常量缺失则返回 None。"""
    def num(name):
        m = re.search(r"%s\s*=\s*(\d+)\s*/\s*(\d+)f\s*;" % name, src)
        return (int(m.group(1)) / int(m.group(2))) if m else None

    base = num("PROC_CHANCE_BASE")
    cmax = num("PROC_CHANCE_MAX")
    decay = num("PROC_CHANCE_DECAY")
    if base is None or cmax is None or decay is None:
        return None
    return lambda lvl: cmax - (cmax - base) * (decay ** lvl)


def part_g():
    print("\n=== G. 概率随等级成长（+0 = 1/8 → 渐近 1/2）===")
    f = chance_func(read(WEAPON))
    if f is None:
        bad("三个概率常量取不全，无法复算")
        return

    check(abs(f(0) - 1/8) < 1e-9,
          "+0 时恰为 1/8（实得 %.4f%%）" % (f(0) * 100),
          "起点不是 1/8（实得 %.4f）" % f(0))

    lvls = list(range(0, 31))
    gaps = [f(l + 1) - f(l) for l in lvls[:-1]]
    check(all(g > 0 for g in gaps),
          "+0 → +30 每级概率都**严格变大**（最小增幅 %.3f 个百分点）" % (min(gaps) * 100),
          "存在增幅 ≤ 0 的等级 ⇒ 那一段里强化没有收益，「随等级提升」名不副实")
    check(all(f(l) < 0.5 for l in lvls),
          "任何等级都严格小于 1/2（是渐近线，不是封顶值）")
    check(0.49 < f(60) < 0.5 and 0.499 < f(100) < 0.5,
          "+60 → %.4f%%、+100 → %.6f%%：足够高时无限贴近 1/2" % (f(60) * 100, f(100) * 100),
          "收敛过快/过慢，或根本不是收敛的")
    check(gaps[0] == max(gaps),
          "第一级（+0→+1）收益最大（%.2f 个百分点），此后单调递减" % (gaps[0] * 100),
          "增幅不是单调递减 ⇒ 曲线已不是「指数趋近」，与 javadoc/final 设计不符")

    #实用区间（三阶武器的现实强化范围）——防止曲线参数被悄悄改陡/改缓
    for lvl, lo, hi in ((3, 0.240, 0.260), (6, 0.320, 0.345),
                        (10, 0.390, 0.412), (15, 0.440, 0.460)):
        check(lo <= f(lvl) <= hi,
              "+%d → %.2f%%（落在预期 %.0f%%~%.0f%% 内）"
              % (lvl, f(lvl) * 100, lo * 100, hi * 100),
              "实得 %.2f%%，与设计区间不符（DECAY 被改过）" % (f(lvl) * 100))

    #面板显示口径：四舍五入（+0 的 12.5% 显示成 13%，截断会显示成 12%）
    check(re.search(r"Math\.round\s*\(\s*procChance\s*\(", read(WEAPON)) is not None,
          "面板百分比走 Math.round（+0 的 12.5% → 13%；截断会显示 12%）",
          "用 (int) 截断会让 +0 显示 12%，与「1/8 = 12.5%」轻微不符")

    ok("取值表 " + " / ".join("+%d = %.1f%%" % (l, f(l) * 100)
                             for l in (0, 1, 3, 5, 8, 10, 15, 20)))


# --------------------------------------------------------------------------
# 反例自测
# --------------------------------------------------------------------------
def selftest():
    print("=== 反例自测（证明判据不恒真）===")
    problems = []

    # 1) body_of 必须取到正确方法体
    fake = ("class T{ void a(){ helper(); } "
            "void helper(){ if(x){ y(); } } "
            "void z(){ helper(); } void helper(){ int q=1; } }")
    b = body_of(fake, r"void\s+helper\s*\(\s*\)\s*\{")
    if b is None:
        problems.append("body_of 取不到方法体")
    elif "if(x)" not in b:
        problems.append("body_of 撞上了更早的调用而不是声明")

    # 2) 「super.proc 在触发判定之前」的顺序判据正反例可分
    good = "int proc(){ damage = super.proc(a,b,damage); if(Random.Float() < PROC_CHANCE){} return damage; }"
    weak = "int proc(){ if(Random.Float() < PROC_CHANCE){ damage = super.proc(a,b,damage); } return damage; }"
    if not (good.find("super.proc(") < good.find("PROC_CHANCE")):
        problems.append("顺序判据在正例上就失败")
    if weak.find("super.proc(") < weak.find("PROC_CHANCE"):
        problems.append("顺序判据对反例（先掷概率再走原版结算）没判出问题")

    # 3) 「turnsLeft-- 在 detach 之前」正反例可分
    good2 = "boolean act(){ turnsLeft--; if(turnsLeft <= 0) detach(); spend(TICK); return true; }"
    weak2 = "boolean act(){ if(turnsLeft <= 0) detach(); turnsLeft--; return true; }"
    if not (good2.find("turnsLeft--") < good2.find("detach()")):
        problems.append("窗口扣格顺序判据在正例上就失败")
    if weak2.find("turnsLeft--") < weak2.find("detach()"):
        problems.append("窗口扣格顺序判据对反例（先判关窗再扣格）没判出问题")

    # 4) 「warp() 恰好 2 次」能抓出「回退给另一方」的第 3 条路
    two = "if (Random.Int(2) == 0) { w = warp(defender) ? defender : null; } else { w = warp(attacker) ? attacker : null; }"
    three = two + " if (w == null) w = warp(defender) ? defender : null;"
    if two.count("warp(") != 2:
        problems.append("warp 计数判据在正例上就失败")
    if three.count("warp(") == 2:
        problems.append("warp 计数判据对反例（多了回退分支）没判出问题")

    # 5) 常量必须早于 static{} 的顺序判据正反例可分
    def decl_before_static(src, name):
        m = re.search(r"int\s+" + re.escape(name) + r"\s*=\s*xy\(", src)
        blk = static_block_line(src, name)
        if m is None or blk is None:
            return None
        return (src[:m.start()].count("\n") + 1) < blk
    okk = "class T{ static final int A = xy(6,41);\n static{\n assignItemRect(A, 13, 15);\n }\n}"
    badd = "class T{ static{\n assignItemRect(A, 13, 15);\n }\n static final int A = xy(6,41);\n}"
    if decl_before_static(okk, "A") is not True:
        problems.append("贴图常量顺序判据在正例上就失败")
    if decl_before_static(badd, "A") is not False:
        problems.append("贴图常量顺序判据对反例（常量写在 static{} 之后）没判出问题")

    # 6) classes/defaultProbs 等长判据正反例可分
    n = lambda csv: len([x for x in csv.split(",") if x.strip()])
    if n("A.class, B.class") != 2:
        problems.append("数组长度判据错了")
    if n("A.class, B.class") == n("2, 2, 2"):
        problems.append("数组长度判据对反例（classes 短于 probs）没判出问题")

    # 7) 注释里的旧写法不能喂假绿
    commented = "void f(){ // 这里曾经写 Mob.HUNTING 但现在不这么写了\n }"
    if "Mob.HUNTING" in strip_comments(commented):
        problems.append("strip_comments 没剥掉注释 ⇒ 「不得再出现」类断言会假绿")

    # 8) `//**强调**` 里的 /* 不能吞掉后面的代码
    tricky = "import a.B;\n//**粗体**说明\nclass T{ B b; }"
    if "B b;" not in strip_comments(tricky):
        problems.append("`//**` 里的 /* 把后面的代码吞了（旧写法复发）")

    # 9) Generator 数组声明 `new Class<?>[]{}` / `new float[]{}` 形态必须被正则取到
    #    （曾因漏写 []，导致「classes/probs 等长」这条判据静默跳过、永远不跑）
    gsrc = ("WEP_T3.classes = new Class<?>[]{ A.class, B.class };"
            " WEP_T3.defaultProbs = new float[]{ 1, 2 };")
    gm = re.search(r"WEP_T3\.classes\s*=\s*new\s+Class<\?>\s*\[\s*\]\s*\{([^}]*)\}", gsrc)
    gp = re.search(r"WEP_T3\.defaultProbs\s*=\s*new\s+float\s*\[\s*\]\s*\{([^}]*)\}", gsrc)
    if gm is None or gp is None:
        problems.append("Generator 数组声明（含 []）取不到 ⇒ 「等长」判据会静默跳过")
    elif len([x for x in gm.group(1).split(",") if x.strip()]) != 2:
        problems.append("Generator 数组声明取到了但元素切分错")

    # 10) 「占副槽」类判据要认 keepSlot() 这一层收口（不能只认字面 setSecondAction）
    opened = ("void open(Char e){ this.enemy = e; timeToNow(); keepSlot(); } "
              "void keepSlot(){ ActionIndicator.setSecondAction(this); }")
    ob2 = body_of(opened, r"void\s+open\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if ob2 is None or "keepSlot()" not in ob2:
        problems.append("「open 占副槽」判据认不出经 keepSlot() 收口的写法")
    # 反例：完全没占槽的 open 必须判出问题
    opened_bad = "void open(Char e){ this.enemy = e; timeToNow(); }"
    ob3 = body_of(opened_bad, r"void\s+open\s*\(\s*Char\s+\w+\s*\)\s*\{")
    if ob3 is not None and "keepSlot()" in ob3:
        problems.append("「open 占副槽」判据对反例（根本没占槽）没判出问题")

    # 11) 调色板「恰好 6 色」判据：多一色也要判出（防阈值写成 >= 1）
    six_ok = "int[] COLORS = {0xFFFFFF, 0xF1B8FC, 0xD887F0, 0xAE52DD, 0x6A42BA, 0x4638A8};"
    seven = "int[] COLORS = {0xFFFFFF, 0xF1B8FC, 0xD887F0, 0xAE52DD, 0x6A42BA, 0x4638A8, 0x123456};"
    if len(palette_of(six_ok)) != 6:
        problems.append("调色板取色判据在正例（6 色）上就失败")
    if len(palette_of(seven)) == 6:
        problems.append("调色板判据对反例（7 色）没判出问题 ⇒ 阈值写成了 >= 之类")

    # 12) 「与贴图逐色一致」：改掉其中一色必须被抓出
    if set(palette_of(six_ok)) == set(palette_of(six_ok.replace("0xAE52DD", "0xAE52DE"))):
        problems.append("逐色比对判据对反例（悄悄改了一色）没判出问题")

    # 13) 「明度从白到深紫单调」正反例可分
    def _mono(vs):
        ls = [luminance(v) for v in vs]
        return all(ls[i] > ls[i + 1] for i in range(len(ls) - 1))
    swapped = "int[] COLORS = {0xFFFFFF, 0xAE52DD, 0xD887F0, 0x6A42BA, 0x4638A8, 0xF1B8FC};"
    if not _mono(palette_of(six_ok)):
        problems.append("明度单调判据在正例上就失败")
    if _mono(palette_of(swapped)):
        problems.append("明度单调判据对反例（顺序打乱）没判出问题")

    # 14) 「不得插值」必须先剥注释：javadoc 里正大光明写着 ColorMath，不能喂假红；
    #     真代码里的 interpolate 也不能漏
    doc_only = "class T{ /** 本文件里找不到 {@code ColorMath}，也不该有 */ void f(){} }"
    if "ColorMath" not in doc_only or "ColorMath" in strip_comments(doc_only):
        problems.append("「不得插值」判据没剥注释 ⇒ 文档里提一句就误报")
    real_interp = "class T{ void f(){ color(ColorMath.interpolate(0xFFFFFF, 0x4638A8, 0.5f)); } }"
    if "interpolate" not in strip_comments(real_interp):
        problems.append("「不得插值」判据把真代码里的插值漏掉了")

    # 15) IMPLODE「速度＝位移÷存活期」判据正反例可分
    good4 = ("void resetImplode(float cx, float cy, int index){ "
             "speed.set(cx - this.x, cy - this.y); speed.scale(1f / lifespan); }")
    bad4 = good4.replace("speed.scale(1f / lifespan);", "speed.polar(1f, 50);")
    if re.search(r"scale\(\s*1f\s*/\s*lifespan\s*\)", good4) is None:
        problems.append("IMPLODE 抵达判据在正例上就失败")
    if re.search(r"scale\(\s*1f\s*/\s*lifespan\s*\)", bad4) is not None:
        problems.append("IMPLODE 抵达判据对反例（改回固定速度）没判出问题")

    # 16) 「先记出发点再 appear」顺序判据正反例可分
    g5 = ("void doAction(){ int from = hero.pos; ScrollOfTeleportation.appear(hero, dest); "
          "teleportFx(from, dest); }")
    b5 = ("void doAction(){ ScrollOfTeleportation.appear(hero, dest); "
          "teleportFx(hero.pos, dest); }")
    if not (g5.find("hero.pos") < g5.find("ScrollOfTeleportation.appear(")):
        problems.append("「先记出发点再 appear」判据在正例上就失败")
    if b5.find("hero.pos") < b5.find("ScrollOfTeleportation.appear("):
        problems.append("该判据对反例（先 appear 再取 pos）没判出问题")

    # 17) 颜色归一化 / 反例文案构造：
    #     这两条是真踩过的坑 —— (r,g,b) 元组与 0xRRGGBB 混比会「看起来一样却判不相等」；
    #     反例文案是**先求值**再传给 check 的，`%` 喂错类型会直接抛异常，把断言本身掩盖掉。
    if pack((70, 56, 168)) != 0x4638A8:
        problems.append("pack() 归一化错误 ⇒ 贴图取色永远比不相等")
    if set([pack((255, 255, 255))]) == set([(255, 255, 255)]):
        problems.append("pack() 没起作用，还是元组")
    if hexa(0xFFFFFF) != "#FFFFFF" or hexa((255, 255, 255, 255)) != "#FFFFFF":
        problems.append("hexa() 对 int / 元组两种形态不能都渲染 ⇒ 反例文案会抛异常")

    # 18) 概率成长公式（2026-09-22 新增）：四类错法都要能被抓出来
    trio = ("public static final float PROC_CHANCE_BASE = 1/8f;\n"
            "public static final float PROC_CHANCE_MAX = 1/2f;\n"
            "public static final float PROC_CHANCE_DECAY = 7/8f;\n")
    f0 = chance_func(trio)
    if f0 is None or abs(f0(0) - 1/8) > 1e-9 or not (f0(10) < 0.5):
        problems.append("chance_func 认不出合法的三常量组")
    # 起点常量必须真的被读进去（改成 1/12 就该算出 1/12）
    if abs(chance_func(trio.replace("1/8f", "1/12f"))(0) - 1/12) > 1e-9:
        problems.append("改了起点常量复算结果却不变 ⇒ 常量没被真的读进去（断言会假绿）")
    # DECAY = 1 ⇒ 概率恒定在 1/8、永远不成长（part_g 的「严格递增」要能判出来）
    flat = chance_func(trio.replace("7/8f", "1/1f"))
    if abs(flat(10) - flat(0)) > 1e-12:
        problems.append("DECAY=1 时概率却在变 ⇒ 复算函数没按常量走")
    # DECAY = 0 ⇒ 第一级就跳到 1/2、之后不再涨（「伪成长」形态，用来验证严格递增判据真的好使）
    jump = chance_func(trio.replace("7/8f", "0/1f"))
    if jump is None or abs(jump(1) - 0.5) > 1e-9 or abs(jump(5) - jump(1)) > 1e-12:
        problems.append("DECAY=0 的反例形态与预期不符 ⇒ 判据没被真验证过")
    elif not (jump(1) - jump(0) > 0 and jump(2) - jump(1) == 0):
        problems.append("「伪成长」反例构造失败 ⇒ 严格递增判据没被真验证过")

    for p in problems:
        print("  自测失败：" + p)
    if not problems:
        print("  自测通过：body_of 定位正确 / 三处顺序判据正反例可分 / warp 计数能抓回退分支 / "
              "数组等长可分 / 剥注释有效 / //** 不吞代码 / 调色板 6 色与明度单调可分 / "
              "逐色比对能抓偷改 / IMPLODE 抵达与出发点顺序可分 / 颜色归一化与反例文案不崩 / "
              "概率成长四类错法（起点写错、DECAY=1 不成长、DECAY=0 伪成长、常量没被读）可分")
    return 0 if not problems else 1


def main():
    if "--selftest" in sys.argv:
        return selftest()

    for p in (WEAPON, BUFF, PART, SHEET, GEN, ITEMS_PNG, ZH, EN, AZH, AEN):
        if not os.path.exists(p):
            print("缺文件：%s" % p)
            return 1

    part_a()
    part_b()
    part_c()
    part_d()
    part_e()
    part_f()
    part_g()

    print("\n" + "=" * 60)
    print("通过 %d 条，失败 %d 条" % (len(PASS), len(FAIL)))
    for f in FAIL:
        print("  FAIL  " + f)
    print("ALL PASS" if not FAIL else "FAIL")
    return 0 if not FAIL else 1


if __name__ == "__main__":
    sys.exit(main())
