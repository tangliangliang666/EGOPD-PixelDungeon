# -*- coding: utf-8 -*-
"""
EGOPD 调研工具：原版（SPD-classes 核心 + 本 mod 未改动的 Hero 部分）英雄 1~30 级基础数值曲线。

数据来源（全部为源码公式，逐行核对过）：
  Hero.MAX_LEVEL = 30                                Hero.java:228
  Hero.updateHT(): HT = 20 + 5*(lvl-1) + HTBoost      Hero.java:286
  Hero 初始 attackSkill = 10 / defenseSkill = 5        Hero.java:242-243
  每次升级 attackSkill++ / defenseSkill++              Hero.java:2380-2381
  Hero.maxExp(lvl) = 5 + lvl*5                        Hero.java:2419-2421
  Talent.tierLevelThresholds = {0,2,7,13,21,31}       Talent.java:630
  Char.hit(): acuRoll = U(0,acu), defRoll = U(0,def)   Char.java:810-852

命中判定推导（Random.Float(n) = U[0,n)）：
  A = attackSkill(攻方), D = defenseSkill(守方)
  命中率 P(X>=Y):  A >= D -> 1 - D/(2A) ;  A <  D -> A/(2D)
  即 比值 A/D 决定一切，与绝对数值无关。
"""
import math

MAX_LEVEL = 30
BASE_HT = 20
HT_PER_LVL = 5
BASE_ATK = 10
BASE_DEF = 5
THRESHOLDS = [0, 2, 7, 13, 21, 31]  # 天赋档位门槛（Talent.java:630）
HAS_SUBCLASS = True   # 假设已转职（10 层天狗后可选专精；T3 天赋依赖它）
HAS_CROWN = True      # 假设已戴矮人王冠（20 层后给盔甲技能；T4 天赋依赖它）


def ht(lvl):
    return BASE_HT + HT_PER_LVL * (lvl - 1)


def atk(lvl):
    return BASE_ATK + (lvl - 1)


def dfn(lvl):
    return BASE_DEF + (lvl - 1)


def max_exp(lvl):
    """从 lvl 升到 lvl+1 需要的经验（maxExp 用升级前的等级求值）"""
    return 5 + lvl * 5


def cum_exp(lvl):
    """从 1 级练到 lvl 级累计需要的经验"""
    return sum(max_exp(k) for k in range(1, lvl))


def talent_points(lvl):
    """该等级下可支配的天赋点总数（不含神启药剂额外 +2）。
    假设已转职（击破天狗/10 层后可选专精，T3 才开放）、已戴王冠（20 层后，T4 才开放）。"""
    total = 0
    for tier in range(1, 5):
        if lvl < THRESHOLDS[tier] - 1:
            continue
        if tier == 3 and not HAS_SUBCLASS:
            continue
        if tier == 4 and not HAS_CROWN:
            continue
        if lvl >= THRESHOLDS[tier + 1]:
            total += THRESHOLDS[tier + 1] - THRESHOLDS[tier]
        else:
            total += max(0, 1 + lvl - THRESHOLDS[tier])
    return total


def hit_chance(a, d):
    """命中率：acuRoll=U(0,a) vs defRoll=U(0,d)，X>=Y 即命中"""
    if a <= 0:
        return 0.0
    if d <= 0:
        return 1.0
    if a >= d:
        return max(0.0, 1.0 - d / (2.0 * a))
    return min(1.0, a / (2.0 * d))


# 与深度对应的「典型英雄等级」（SPD 正常通关节奏的经验估算）
DEPTH_LVLS = {1: 1, 5: 6, 10: 12, 15: 17, 20: 23, 25: 27, 30: 30}

# 各区代表性怪物 (所在深度, 名字, 攻击技能, 闪避技能, 血量)
ZONE_MOBS = [
    (5,  "老鼠 Rat", 8, 2, 8),
    (5,  "豺狼人 Gnoll", 10, 4, 12),
    (5,  "螃蟹 Crab", 12, 5, 15),
    (5,  "蛇 Snake", 10, 25, 4),
    (10, "骷髅 Skeleton", 12, 9, 25),
    (10, "窃贼 Thief", 12, 12, 20),
    (10, "侏儒守卫 GnollGuard", 20, 15, 35),
    (15, "蝙蝠 Bat", 16, 15, 30),
    (15, "蛮兵 Brute", 20, 15, 40),
    (15, "萨满 Shaman", 18, 15, 35),
    (20, "魔像 Golem", 28, 15, 120),
    (20, "元素 Elemental", 25, 20, 60),
    (20, "食尸鬼 Ghoul", 24, 20, 45),
    (25, "魅魔 Succubus", 40, 25, 80),
    (25, "眼魔 Eye", 30, 20, 100),
    (25, "蝎狮 Scorpio", 36, 24, 110),
    (30, "尤格之拳 YogFist", 36, 20, 300),
    (30, "武僧 Monk", 30, 30, 70),
]


def main():
    lines = []

    def w(s=""):
        lines.append(s)

    w("# 英雄 1~30 级基础数值（原版公式实测）\n")

    w("## 一、公式总表\n")
    w("| 项目 | 公式 | 源码位置 |")
    w("|---|---|---|")
    w("| 最大生命 HT | `20 + 5*(lvl-1) + HTBoost` | `Hero.updateHT()` Hero.java:286 |")
    w("| 命中（攻击技能） | `10 + (lvl-1)`，初始 10，每次升级 +1 | Hero.java:242 / 2380 |")
    w("| 闪避（防御技能） | `5 + (lvl-1)`，初始 5，每次升级 +1 | Hero.java:243 / 2381 |")
    w("| 升级所需经验 | `5 + 5*lvl`（用升级前等级） | Hero.java:2419 |")
    w("| 力量 STR | 与等级**无关**，仅靠力量药剂 / 戒指 / buff | Hero.java:305 |")
    w("| 攻击伤害 | 与等级**无关**（只跟武器、力量、强化有关） | `Hero.damageRoll()` Hero.java:814 |")
    w("| 护甲减伤 DR | 与等级**无关**（只跟护甲阶数、强化有关） | `Hero.drRoll()` Hero.java:761 |")
    w()

    w("## 二、逐级数值表（1~30 级，无任何装备 / buff / 天赋加成）\n")
    w("| 等级 | HT | 命中 | 闪避 | 命中/闪避比 | 升下一级需经验 | 累计经验 | 可支配天赋点 |")
    w("|---:|---:|---:|---:|---:|---:|---:|---:|")
    for lvl in range(1, MAX_LEVEL + 1):
        ratio = atk(lvl) / dfn(lvl)
        w(f"| {lvl} | {ht(lvl)} | {atk(lvl)} | {dfn(lvl)} | {ratio:.3f} | "
          f"{max_exp(lvl)} | {cum_exp(lvl)} | {talent_points(lvl)} |")
    w()

    w("## 三、关键节点\n")
    w("| 等级 | HT | 命中 | 闪避 | 相对 1 级的 HT 倍率 | 相对 1 级的命中倍率 | 备注 |")
    w("|---:|---:|---:|---:|---:|---:|---|")
    notes = {
        1: "开局",
        2: "T1 天赋开启",
        6: "第一区（1~5 层）通关时的典型等级",
        7: "T2 天赋开启",
        12: "击破天狗后的典型等级 → 选专精，T3 天赋开启",
        13: "T3 天赋门槛",
        17: "第三区（11~15 层）末",
        21: "T4 天赋门槛（含盔甲技能）",
        23: "第四区（16~20 层）末",
        27: "第五区（21~25 层）末",
        30: "等级上限 MAX_LEVEL；再吃经验只给「祝福」buff |",
    }
    for lvl in [1, 2, 6, 7, 12, 13, 17, 21, 23, 27, 30]:
        w(f"| {lvl} | {ht(lvl)} | {atk(lvl)} | {dfn(lvl)} | {ht(lvl)/ht(1):.2f}x | "
          f"{atk(lvl)/atk(1):.2f}x | {notes[lvl]} |")
    w()

    # ---------- 战斗上下文：命中率 ----------
    w("## 四、命中率实算（命中判定只看 攻/守 技能比值）\n")
    w("英雄打怪物（英雄命中概率，未计武器 ACC 系数与力量不足惩罚）：\n")
    w("| 深度 | 英雄等级 | 英雄命中 | 怪物 | 怪物闪避 | 怪物血量 | 英雄命中率 |")
    w("|---:|---:|---:|---|---:|---:|---:|")
    for depth, name, ma, md, mhp in ZONE_MOBS:
        lvl = DEPTH_LVLS[depth]
        w(f"| {depth} | {lvl} | {atk(lvl)} | {name} | {md} | {mhp} | {hit_chance(atk(lvl), md)*100:.1f}% |")
    w()

    w("怪物打英雄（英雄被命中的概率，未计护甲 / 闪避刻印）：\n")
    w("| 深度 | 英雄等级 | 英雄闪避 | 怪物 | 怪物命中 | 英雄被命中率 | 英雄闪避率 |")
    w("|---:|---:|---:|---|---:|---:|---:|")
    for depth, name, ma, md, mhp in ZONE_MOBS:
        lvl = DEPTH_LVLS[depth]
        hc = hit_chance(ma, dfn(lvl))
        w(f"| {depth} | {lvl} | {dfn(lvl)} | {name} | {ma} | {hc*100:.1f}% | {(1-hc)*100:.1f}% |")
    w()

    # ---------- 同技能怪：命中率随等级的变化 ----------
    w("## 五、面对「与英雄同级技能」的假想敌人时（仅看等级带来的变化）\n")
    w("| 等级 | 英雄命中 | 英雄闪避 | 命中率(若敌我技能相等) |")
    w("|---:|---:|---:|---:|")
    for lvl in [1, 5, 10, 15, 20, 25, 30]:
        w(f"| {lvl} | {atk(lvl)} | {dfn(lvl)} | {hit_chance(atk(lvl), dfn(lvl))*100:.1f}% |")
    w()

    # ---------- 等级冻结 = 战斗力冻结 ----------
    w("## 六、设计含义：等级被冻结时，英雄的「面板底座」是多少\n")
    w("| 冻结点 | HT | 命中 | 闪避 | 天赋点 | 说明 |")
    w("|---:|---:|---:|---:|---:|---|")
    w(f"| 1 级 | {ht(1)} | {atk(1)} | {dfn(1)} | {talent_points(1)} | "
      f"开局数值，比 30 级少 {ht(30)-ht(1)} 点血、{atk(30)-atk(1)} 点命中、{dfn(30)-dfn(1)} 点闪避 |")
    w(f"| 30 级 | {ht(30)} | {atk(30)} | {dfn(30)} | {talent_points(30)} | 满级数值（+29 点天赋全部用掉） |")
    w()
    w(f"- 30 级相对 1 级：**生命 ×{ht(30)/ht(1):.2f}**、命中 +{atk(30)-atk(1)}、闪避 +{dfn(30)-dfn(1)}。")
    w(f"- 生命是**线性**增长（每级 +5，30 级 165），命中/闪避是**每级 +1 的线性**（10→39 / 5→34），")
    w("  所以二者在 1~30 级区间内没有拐点，**唯一的「跳变」来自天赋点**：")
    w(f"  1 级 0 点 → 6 级 5 点（T1 满）→ 12 级 11 点（T2 满）→ 20 级 19 点（T3 满）→ 30 级 29 点（T4 满）。")
    w("- 英雄被「锁在 1 级」时，HT 只有 20 点（老鼠 8 点的 2.5 倍），命中 10、闪避 5，且**永远拿不到天赋点**。")
    w("  这是武器必须自己补上的缺口，而补缺口的方式不可能是「给数值」——")
    w("  因为在 30 级那一侧，英雄已经有 165 HT / 39 命中 / 34 闪避 / 29 天赋点，同样的数值会毫无意义。")
    w()

    w("## 七、经验阻断的落点与副作用（供后续开发参考）\n")
    w("经验进账的唯一收口是 `Hero.earnExp(int exp, Class source)`（Hero.java:2315），"
      "怪物死亡时由 `Mob.die()` 调用（Mob.java:882）。\n")
    w("| 落点 | 效果 | 副作用 |")
    w("|---|---|---|")
    w("| `Hero.earnExp()` 开头直接 return | 彻底不涨经验、不升级 | 同一方法内的**其它收益一起被掐掉**（见下） |")
    w("| 只清 `this.exp` / 冻结 `levelUp` | 经验条不动，但仍会触发收口内所有 on-exp 效果 | 需要拆方法，改动更大 |")
    w()
    w("`Hero.earnExp()` 里除了加经验，还顺带做这些事（都在同一个方法内，按 `percent = exp/maxExp()` 计算）：\n")
    w("- 神器充能：`EtherealChains`、`HornOfPlenty`、`AlchemistsToolkit`、`MasterThievesArmband`、`Berserk`（Hero.java:2323-2336）")
    w("- 背包物品的 `onHeroGainExp(percent, hero)` 回调（Hero.java:2338-2341）")
    w("- 天赋计数器：`RejuvenatingStepsFurrow`、`ElementalStrikeFurrowCounter`、`HallowedGroundFurrowTracker`（Hero.java:2342-2359）")
    w("- 升级本身：`updateHT(true)`（补满增量血量）、`attackSkill++`、`defenseSkill++`（Hero.java:2379-2381）")
    w()
    w("两点必须一并考虑：\n")
    w("- **1 级形态**：`Mob.die()` 里 `int exp = Dungeon.hero.lvl <= maxLvl ? EXP : 0;`（Mob.java:869），"
      "而 `Mob.maxLvl` 默认 `Hero.MAX_LEVEL-1 = 29`。所以 1 级英雄打任何怪都拿全额经验，"
      "「阻止获得经验」必须靠武器自己拦，不能指望系统。")
    w("- **30 级形态**：英雄到 30 级后 `Hero.earnExp()` 里的 `while` 会走 else 分支，"
      "把经验清 0 并给一个 `Bless` buff（Hero.java:2383-2390），本来就是「经验无意义」。")
    w("  所以 30 级形态的「阻止经验」在数值上只是**拦截祝福 buff**，重点应该放在形态强度上。")
    w()

    # ---------- 精确整数对照（供数值设计估量） ----------
    w("## 八、逐级明细（CSV 便于后续计算 / 画图）\n")
    w("```")
    w("lvl,HT,ATK,DEF,exp_next,exp_cum,talents")
    for lvl in range(1, MAX_LEVEL + 1):
        w(f"{lvl},{ht(lvl)},{atk(lvl)},{dfn(lvl)},{max_exp(lvl)},{cum_exp(lvl)},{talent_points(lvl)}")
    w("```")

    out = "\n".join(lines)
    with open(r"d:\PD\_chk\hero_level_curve.md", "w", encoding="utf-8", newline="\n") as f:
        f.write(out + "\n")
    print(out)


if __name__ == "__main__":
    main()
