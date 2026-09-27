# -*- coding: utf-8 -*-
"""
一次性补丁：把全仓「真·回血」站点收口到 Char.heal(int)
—— 洛伊德护符「禁疗」buff（HealBlock）真正生效的前提。

设计：
  * 每条 op = (文件, 旧代码块(按行, 前导空白通配), 新代码块(按行), 期望起始行号, 幂等标记)
  * 位置断言：匹配到的起始行号必须等于期望值（防止改错地方）
  * 计数断言：每条 op 必须恰好命中 1 次
  * 行尾断言：文件必须纯 CRLF（无裸 LF / 裸 CR），改后 CRLF 增量 == 各 op 行数差之和
  * 改前整文件备份到 _chk/_bak_heal_route/

排除（非回血，刻意不动）：
  * HT 钳制：Math.min(HP, HT) / Math.max(oldHP,1) / 生成初始化 HP = HT
  * 上限重算：Hero.updateHT(boostHP) / ShadowClone HT+= / WandOfWarding tier 升级 HP+=
  * 自持字段：PrismaticGuard.HP(float)
  * 复活：Hero.resurrect / 复活护符
"""
import os, re, sys, shutil

ROOT = r"D:\PD\core\src\main\java\com\shatteredpixel\shatteredpixeldungeon"
BAK  = r"D:\PD\_chk\_bak_heal_route"

OPS = []

def op(path, old_lines, new_lines, line, marker, expect=1):
    OPS.append(dict(path=path, old=old_lines, new=new_lines, line=line,
                    marker=marker, expect=expect))

def build_pat(old_lines):
    """把「按行给出的旧代码」变成行首锚定、前导空白通配的正则。
    第一行的前导空白提升为捕获组 group(1)；末行加 $ 锚定（防 } 前缀误配 } else {）。"""
    parts = []
    for i, s in enumerate(old_lines):
        body = re.escape(s.lstrip(" \t"))
        if i == 0:
            parts.append(r"^([ \t]*)" + body)
        else:
            parts.append(r"\n[ \t]*" + body)
    return "".join(parts) + r"[ \t]*$"

def build_new(m, new_lines):
    I = m.group(1)
    out = []
    for s in new_lines:
        out.append(I + s if s != "" else "")
    return "\n".join(out)

# =====================================================================
# A. buffs
# =====================================================================

op("actors/buffs/ChesedMend.java",
   ["mob.HP += heal;"],
   ["//禁疗：唯一回血出口 Char.heal(int)，被禁疗时返回 0",
    "int healed = mob.heal( heal );"],
   91, "int healed = mob.heal( heal );")

op("actors/buffs/ChesedMend.java",
   ["if (mob.sprite != null) {"],
   ["if (healed > 0 && mob.sprite != null) {"],
   93, "if (healed > 0 && mob.sprite != null) {")

op("actors/buffs/ChesedMend.java",
   ["mob.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );"],
   ["mob.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );"],
   94, "Integer.toString( healed ), FloatingText.HEALING );")

op("actors/buffs/Regeneration.java",
   ["target.HP += (int)partialRegen;",
    "partialRegen -= (int)partialRegen;"],
   ["int heal = (int)partialRegen;",
    "target.heal( heal );",
    "partialRegen -= heal;"],
   92, "int heal = (int)partialRegen;")

op("actors/buffs/WellFed.java",
   ["target.HP += 1;",
    "target.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"1\", FloatingText.HEALING);"],
   ["if (target.heal( 1 ) > 0){",
    "\ttarget.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"1\", FloatingText.HEALING);",
    "}"],
   53, "if (target.heal( 1 ) > 0){")

op("actors/buffs/ThirstBloodBarrier.java",
   ["carrier.HP += heal;",
    "if (carrier.sprite != null) {",
    "carrier.sprite.showStatusWithIcon(CharSprite.POSITIVE,",
    "Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["int healed = carrier.heal( heal );",
    "if (healed > 0 && carrier.sprite != null) {",
    "carrier.sprite.showStatusWithIcon(CharSprite.POSITIVE,",
    "\t\tInteger.toString(healed), FloatingText.HEALING);",
    "}"],
   95, "int healed = carrier.heal( heal );")

op("actors/buffs/LifeRegen.java",
   ["int before = target.HP;",
    "target.HP = Math.min( target.HT, target.HP + h );",
    "int healed = target.HP - before;"],
   ["int healed = target.heal( h );"],
   65, "int healed = target.heal( h );")

op("actors/buffs/Healing.java",
   ["if (target.HP < target.HT) {",
    "target.HP = Math.min(target.HT, target.HP + healingThisTick());"],
   ["//禁疗：唯一回血出口 Char.heal(int)，被禁疗时返回 0",
    "int healed = 0;",
    "if (target.HP < target.HT) {",
    "healed = target.heal( healingThisTick() );"],
   53, "healed = target.heal( healingThisTick() );")

op("actors/buffs/Healing.java",
   ["target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healingThisTick()), FloatingText.HEALING);"],
   ["if (healed > 0) {",
    "\ttarget.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   61, "Integer.toString(healed), FloatingText.HEALING);")

op("actors/buffs/MagicalSleep.java",
   ["target.HP = Math.min(target.HP+1, target.HT);"],
   ["target.heal( 1 );"],
   69, "target.heal( 1 );")

op("actors/buffs/GritTeethBuff.java",
   ["hero.HP = Math.min( hero.HT, hero.HP + heal );"],
   ["int healed = hero.heal( heal );"],
   171, "int healed = hero.heal( heal );")

op("actors/buffs/GritTeethBuff.java",
   ["if (hero.sprite != null){"],
   ["if (healed > 0 && hero.sprite != null){"],
   172, "if (healed > 0 && hero.sprite != null){")

op("actors/buffs/GritTeethBuff.java",
   ["hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );"],
   ["hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );"],
   173, "Integer.toString( healed ), FloatingText.HEALING );")

op("actors/buffs/AimHeartMark.java",
   ["hero.HP = Math.min( hero.HT, hero.HP + heal );"],
   ["int healed = hero.heal( heal );"],
   68, "int healed = hero.heal( heal );")

op("actors/buffs/AimHeartMark.java",
   ["if (hero.sprite != null){"],
   ["if (healed > 0 && hero.sprite != null){"],
   69, "if (healed > 0 && hero.sprite != null){")

op("actors/buffs/AimHeartMark.java",
   ["hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"+\" + heal, FloatingText.HEALING );"],
   ["hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"+\" + healed, FloatingText.HEALING );"],
   70, "\"+\" + healed, FloatingText.HEALING );")

# =====================================================================
# B. mobs
# =====================================================================

op("actors/mobs/Bat.java",
   ["HP += reg;",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(reg), FloatingText.HEALING);"],
   ["int healed = heal( reg );",
    "if (healed > 0) {",
    "\tsprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   78, "int healed = heal( reg );")

op("actors/mobs/Goo.java",
   ["HP += healInc;"],
   ["int gooHealed = heal( healInc );"],
   110, "int gooHealed = heal( healInc );")

op("actors/mobs/Goo.java",
   ["if (Dungeon.level.heroFOV[pos] ){"],
   ["if (gooHealed > 0 && Dungeon.level.heroFOV[pos] ){"],
   119, "if (gooHealed > 0 && Dungeon.level.heroFOV[pos] ){")

op("actors/mobs/Goo.java",
   ["sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healInc), FloatingText.HEALING );"],
   ["sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(gooHealed), FloatingText.HEALING );"],
   120, "Integer.toString(gooHealed), FloatingText.HEALING );")

op("actors/mobs/Succubus.java",
   ["HP += 5 + damage;",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, \"5\", FloatingText.HEALING);"],
   ["int healed = heal( 5 + damage );",
    "if (healed > 0) {",
    "\tsprite.showStatusWithIcon(CharSprite.POSITIVE, \"5\", FloatingText.HEALING);",
    "}"],
   91, "int healed = heal( 5 + damage );")

op("actors/mobs/Necromancer.java",
   ["mySkeleton.HP = Math.min(mySkeleton.HP + mySkeleton.HT/5, mySkeleton.HT);"],
   ["int skelHealed = mySkeleton.heal( mySkeleton.HT/5 );"],
   180, "int skelHealed = mySkeleton.heal( mySkeleton.HT/5 );")

op("actors/mobs/Necromancer.java",
   ["if (mySkeleton.sprite.visible) {"],
   ["if (skelHealed > 0 && mySkeleton.sprite.visible) {"],
   181, "if (skelHealed > 0 && mySkeleton.sprite.visible) {")

op("actors/mobs/Necromancer.java",
   ["mySkeleton.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( mySkeleton.HT/5 ), FloatingText.HEALING );"],
   ["mySkeleton.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( skelHealed ), FloatingText.HEALING );"],
   182, "Integer.toString( skelHealed ), FloatingText.HEALING );")

op("actors/mobs/RingfingerAutomaton.java",
   ["target.HP = Math.min( target.HT, target.HP + 1 );",
    "if (target.sprite != null && target.sprite.visible){",
    "target.sprite.showStatus( CharSprite.POSITIVE, \"1\" );",
    "}"],
   ["if (target.heal( 1 ) > 0 && target.sprite != null && target.sprite.visible){",
    "\ttarget.sprite.showStatus( CharSprite.POSITIVE, \"1\" );",
    "}"],
   357, "if (target.heal( 1 ) > 0 && target.sprite != null && target.sprite.visible){")

op("actors/mobs/RotLasher.java",
   ["if (HP < HT && (enemy == null || !Dungeon.level.adjacent(pos, enemy.pos))) {",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(Math.min(5, HT - HP)), FloatingText.HEALING);",
    "HP = Math.min(HT, HP + 5);",
    "}"],
   ["if (HP < HT && (enemy == null || !Dungeon.level.adjacent(pos, enemy.pos))) {",
    "\tint healed = heal( 5 );",
    "\tif (healed > 0) {",
    "\t\tsprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "\t}",
    "}"],
   59, "int healed = heal( 5 );")

op("actors/mobs/MeltingLove.java",
   ["HP = Math.min(HT, HP + 1);"],
   ["heal( 1 );"],
   103, "heal( 1 );")

op("actors/mobs/YogFist.java",
   ["if (Dungeon.level.water[pos] && HP < HT) {",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(HT/50), FloatingText.HEALING);",
    "HP = Math.min(HT, HP + HT/50);",
    "}"],
   ["if (Dungeon.level.water[pos] && HP < HT) {",
    "\tint healed = heal( HT/50 );",
    "\tif (healed > 0) {",
    "\t\tsprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "\t}",
    "}"],
   397, "int healed = heal( HT/50 );")

op("actors/mobs/CrystalGuardian.java",
   ["HP = Math.min(HT, HP+5);",
    "if (Dungeon.level.heroFOV[pos]) {",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, \"5\", FloatingText.HEALING);",
    "}"],
   ["int healed = heal( 5 );",
    "if (Dungeon.level.heroFOV[pos] && healed > 0) {",
    "\tsprite.showStatusWithIcon(CharSprite.POSITIVE, \"5\", FloatingText.HEALING);",
    "}"],
   78, "int healed = heal( 5 );")

op("actors/mobs/Mob.java",
   ["if (Dungeon.hero.HP < Dungeon.hero.HT) {",
    "int heal = (int)Math.ceil(restoration * 0.4f);",
    "Dungeon.hero.HP = Math.min(Dungeon.hero.HT, Dungeon.hero.HP + heal);",
    "Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["if (Dungeon.hero.HP < Dungeon.hero.HT) {",
    "\tint healed = Dungeon.hero.heal( (int)Math.ceil(restoration * 0.4f) );",
    "\tif (healed > 0) {",
    "\t\tDungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "\t}",
    "}"],
   810, "int healed = Dungeon.hero.heal( (int)Math.ceil(restoration * 0.4f) );")

# =====================================================================
# C. items
# =====================================================================

op("items/Dewdrop.java",
   ["hero.HP += heal;",
    "if (heal > 0){",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["int healed = hero.heal( heal );",
    "if (healed > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   112, "int healed = hero.heal( heal );")

op("items/armor/curses/Metabolism.java",
   ["defender.HP += healing;",
    "defender.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healing ), FloatingText.HEALING);",
    "}"],
   ["if (defender.heal( healing ) > 0){",
    "\tdefender.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healing ), FloatingText.HEALING);",
    "}"],
   57, "if (defender.heal( healing ) > 0){")

op("items/artifacts/LifelongStew.java",
   ["hero.HP += heal;",
    "if (hero.sprite != null){",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE,",
    "Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["int healed = hero.heal( heal );",
    "if (healed > 0 && hero.sprite != null){",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE,",
    "\t\tInteger.toString(healed), FloatingText.HEALING);",
    "}"],
   76, "int healed = hero.heal( heal );")

op("items/potions/elixirs/ElixirOfAquaticRejuvenation.java",
   ["target.HP += (int)healAmt;",
    "left -= (int)healAmt;",
    "target.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString((int)healAmt), FloatingText.HEALING );"],
   ["int healedAmt = target.heal( (int)healAmt );",
    "left -= (int)healAmt;",
    "if (healedAmt > 0) {",
    "\ttarget.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healedAmt), FloatingText.HEALING );",
    "}"],
   87, "int healedAmt = target.heal( (int)healAmt );")

op("items/wands/WandOfTransfusion.java",
   ["ch.HP += healing;",
    "",
    "ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2 + buffedLvl() / 2);",
    "if (healing > 0) {",
    "ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);",
    "}"],
   ["int healed = ch.heal( healing );",
    "",
    "ch.sprite.emitter().burst(Speck.factory(Speck.HEALING), 2 + buffedLvl() / 2);",
    "if (healed > 0) {",
    "\tch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   104, "int healed = ch.heal( healing );")

op("items/wands/WandOfWarding.java",
   ["if (tier <= 3){",
    "totalZaps = (Math.max(0, totalZaps-heal));",
    "} else {",
    "HP = Math.min(HT, HP + heal);",
    "}",
    "if (sprite != null) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);"],
   ["if (tier <= 3){",
    "\ttotalZaps = (Math.max(0, totalZaps-heal));",
    "} else {",
    "\t//禁疗：唯一回血出口 Char.heal(int)（被禁疗时返回 0，浮字随之不显示）",
    "\theal = this.heal( heal );",
    "}",
    "if (sprite != null && heal > 0) sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);"],
   327, "heal = this.heal( heal );")

op("items/wands/WandOfLivingEarth.java",
   ["if (HP != 0 && sprite != null){",
    "sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healthToAdd), FloatingText.HEALING);",
    "}",
    "HP = Math.min(HT, HP + healthToAdd);"],
   ["boolean wasAlive = HP != 0;",
    "int healed = heal( healthToAdd );",
    "if (wasAlive && sprite != null && healed > 0){",
    "\tsprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   391, "int healed = heal( healthToAdd );")

op("items/wands/CursedWand.java",
   ["toHeal.HP = Math.min(toHeal.HT, toHeal.HP + damage/2);",
    "toHeal.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);",
    "toHeal.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(damage/2), FloatingText.HEALING );"],
   ["int healed = toHeal.heal( damage/2 );",
    "toHeal.sprite.emitter().burst(Speck.factory(Speck.HEALING), 3);",
    "if (healed > 0) {",
    "\ttoHeal.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   488, "int healed = toHeal.heal( damage/2 );")

op("items/weapon/enchantments/Vampiric.java",
   ["attacker.HP += healAmt;",
    "attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healAmt ), FloatingText.HEALING );"],
   ["int healed = attacker.heal( healAmt );",
    "if (healed > 0) {",
    "\tattacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );",
    "}"],
   58, "int healed = attacker.heal( healAmt );")

op("items/weapon/melee/Mimicry.java",
   ["attacker.HP += healAmt;",
    "attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healAmt ), FloatingText.HEALING );"],
   ["int healed = attacker.heal( healAmt );",
    "if (healed > 0) {",
    "\tattacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );",
    "}"],
   81, "int healed = attacker.heal( healAmt );")

op("items/weapon/melee/ParadiseLost.java",
   ["ch.HP += healing;",
    "ch.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 2 + lvl/2 );",
    "if (healing > 0){",
    "ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING );",
    "}"],
   ["int healed = ch.heal( healing );",
    "ch.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 2 + lvl/2 );",
    "if (healed > 0){",
    "\tch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   276, "int healed = ch.heal( healing );")

op("items/weapon/melee/ParadiseLost.java",
   ["if (attacker.isAlive() && attacker.HP < attacker.HT){",
    "attacker.HP = Math.min(attacker.HT, attacker.HP + 2);",
    "attacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"2\", FloatingText.HEALING );",
    "}"],
   ["if (attacker.isAlive() && attacker.HP < attacker.HT){",
    "\tif (attacker.heal( 2 ) > 0){",
    "\t\tattacker.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"2\", FloatingText.HEALING );",
    "\t}",
    "}"],
   335, "if (attacker.heal( 2 ) > 0){")

op("items/weapon/melee/HermesCaduceus.java",
   ["hero.HP = Math.min(hero.HT, hero.HP + 5);",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+5\", FloatingText.HEALING);"],
   ["if (hero.heal( 5 ) > 0){",
    "\thero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+5\", FloatingText.HEALING);",
    "}"],
   463, "if (hero.heal( 5 ) > 0){")

op("items/artifacts/Admiration.java",
   ["love.HP = Math.min(love.HT, love.HP + heal);",
    "if (love.sprite != null) {",
    "love.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["int healed = love.heal( heal );",
    "if (healed > 0 && love.sprite != null) {",
    "\tlove.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   382, "int healed = love.heal( heal );")

op("items/artifacts/ChaliceOfBlood.java",
   ["target.HP = Math.min(target.HT, target.HP + (int)heal);",
    "target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString((int)heal), FloatingText.HEALING);"],
   ["int healed = target.heal( (int)heal );",
    "if (healed > 0) {",
    "\ttarget.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   249, "int healed = target.heal( (int)heal );")

op("items/artifacts/DriedRose.java",
   ["ghost.HP = Math.min( ghost.HT, ghost.HP + heal);",
    "if (ghost.sprite != null) {",
    "ghost.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(heal), FloatingText.HEALING);",
    "}"],
   ["int healed = ghost.heal( heal );",
    "if (healed > 0 && ghost.sprite != null) {",
    "\tghost.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   328, "int healed = ghost.heal( heal );")

op("items/artifacts/DriedRose.java",
   ["ghost.HP = Math.min(ghost.HP+8, ghost.HT);"],
   ["ghost.heal( 8 );"],
   348, "ghost.heal( 8 );")

op("items/food/FrozenCarpaccio.java",
   ["hero.HP = Math.min( hero.HP + hero.HT / 4, hero.HT );",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hero.HT / 4), FloatingText.HEALING );"],
   ["if (hero.heal( hero.HT / 4 ) > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hero.HT / 4), FloatingText.HEALING );",
    "}"],
   70, "if (hero.heal( hero.HT / 4 ) > 0){")

op("items/food/PhantomMeat.java",
   ["hero.HP = Math.min( hero.HP + hero.HT / 4, hero.HT );",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hero.HT / 4), FloatingText.HEALING );"],
   ["if (hero.heal( hero.HT / 4 ) > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hero.HT / 4), FloatingText.HEALING );",
    "}"],
   55, "if (hero.heal( hero.HT / 4 ) > 0){")

op("items/food/Pasty.java",
   ["hero.HP = Math.min(hero.HP + toHeal, hero.HT);",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(toHeal), FloatingText.HEALING );"],
   ["if (hero.heal( toHeal ) > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(toHeal), FloatingText.HEALING );",
    "}"],
   158, "if (hero.heal( toHeal ) > 0){")

op("items/food/SupplyRation.java",
   ["hero.HP = Math.min(hero.HP + 5, hero.HT);",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"5\", FloatingText.HEALING );"],
   ["if (hero.heal( 5 ) > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, \"5\", FloatingText.HEALING );",
    "}"],
   61, "if (hero.heal( 5 ) > 0){")

op("items/remains/TornPage.java",
   ["hero.HP = Math.min(hero.HP + toHeal, hero.HT);",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(toHeal), FloatingText.HEALING );"],
   ["if (hero.heal( toHeal ) > 0){",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(toHeal), FloatingText.HEALING );",
    "}"],
   40, "if (hero.heal( toHeal ) > 0){")

# =====================================================================
# D. hero abilities / talents / spells
# =====================================================================

op("actors/hero/abilities/duelist/Challenge.java",
   ["Dungeon.hero.HP += hpToHeal;",
    "Dungeon.hero.sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.33f, 6 );",
    "Dungeon.hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(hpToHeal), FloatingText.HEALING );"],
   ["int healed = Dungeon.hero.heal( hpToHeal );",
    "Dungeon.hero.sprite.emitter().start( Speck.factory( Speck.HEALING ), 0.33f, 6 );",
    "if (healed > 0) {",
    "\tDungeon.hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   273, "int healed = Dungeon.hero.heal( hpToHeal );")

op("actors/hero/abilities/duelist/ElementalStrike.java",
   ["hero.HP += heal;",
    "hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );"],
   ["int healed = hero.heal( heal );",
    "if (healed > 0) {",
    "\thero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );",
    "}"],
   264, "int healed = hero.heal( heal );")

op("actors/hero/abilities/mage/ElementalBlast.java",
   ["mob.HP += healing;",
    "",
    "mob.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);",
    "",
    "if (healing > 0) {",
    "mob.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);",
    "}"],
   ["int healed = mob.heal( healing );",
    "",
    "mob.sprite.emitter().burst(Speck.factory(Speck.HEALING), 4);",
    "",
    "if (healed > 0) {",
    "\tmob.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   346, "int healed = mob.heal( healing );")

op("actors/hero/abilities/oracle/PinHaoFan.java",
   ["hero.HP = Math.min(hero.HT, hero.HP + heal);",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+\" + heal, FloatingText.HEALING);"],
   ["int healed = hero.heal( heal );",
    "if (healed > 0) {",
    "\thero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+\" + healed, FloatingText.HEALING);",
    "}"],
   77, "int healed = hero.heal( heal );")

op("actors/hero/Talent.java",
   ["hero.HP = Math.min(hero.HP + healing, hero.HT);",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healing), FloatingText.HEALING);"],
   ["int healed = hero.heal( healing );",
    "if (healed > 0) {",
    "\thero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   848, "int healed = hero.heal( healing );")

op("actors/hero/Talent.java",
   ["hero.HP = Math.min(hero.HT, hero.HP + 5);",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+5\", FloatingText.HEALING);"],
   ["if (hero.heal( 5 ) > 0){",
    "\thero.sprite.showStatusWithIcon(CharSprite.POSITIVE, \"+5\", FloatingText.HEALING);",
    "}"],
   1875, "if (hero.heal( 5 ) > 0){")

op("actors/hero/spells/HallowedGround.java",
   ["ch.HP += 15 - barrier;",
    "ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(15-barrier), FloatingText.HEALING );"],
   ["int healed = ch.heal( 15 - barrier );",
    "if (healed > 0) {",
    "\tch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   148, "int healed = ch.heal( 15 - barrier );")

op("actors/hero/spells/BlessSpell.java",
   ["if (ch.HP != ch.HT) {",
    "ch.HP = ch.HT;",
    "ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal - barrier), FloatingText.HEALING);",
    "}"],
   ["if (ch.HP != ch.HT) {",
    "\tint healed = ch.heal( totalHeal - barrier );",
    "\tif (healed > 0) {",
    "\t\tch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "\t}",
    "}"],
   112, "int healed = ch.heal( totalHeal - barrier );")

op("actors/hero/spells/BlessSpell.java",
   ["ch.HP = ch.HP + totalHeal;",
    "ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(totalHeal), FloatingText.HEALING );"],
   ["int healed = ch.heal( totalHeal );",
    "if (healed > 0) {",
    "\tch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   121, "int healed = ch.heal( totalHeal );")

op("actors/hero/spells/LayOnHands.java",
   ["if (ch.HP != ch.HT) {",
    "ch.HP = ch.HT;",
    "ch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(totalHeal - totalBarrier), FloatingText.HEALING);",
    "}"],
   ["if (ch.HP != ch.HT) {",
    "\tint healed = ch.heal( totalHeal - totalBarrier );",
    "\tif (healed > 0) {",
    "\t\tch.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "\t}",
    "}"],
   120, "int healed = ch.heal( totalHeal - totalBarrier );")

op("actors/hero/spells/LayOnHands.java",
   ["ch.HP = ch.HP + totalHeal;",
    "ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(totalHeal), FloatingText.HEALING );"],
   ["int healed = ch.heal( totalHeal );",
    "if (healed > 0) {",
    "\tch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );",
    "}"],
   134, "int healed = ch.heal( totalHeal );")

# =====================================================================
# E. plants / blobs / buffs(事件型)
# =====================================================================

op("plants/Sungrass.java",
   ["target.HP += healThisTurn;",
    "target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healThisTurn), FloatingText.HEALING);"],
   ["int healed = target.heal( healThisTurn );",
    "if (healed > 0) {",
    "\ttarget.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   101, "int healed = target.heal( healThisTurn );")

op("actors/blobs/WaterOfHealth.java",
   ["hero.HP = hero.HT;",
    "hero.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 4);",
    "hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(hero.HT), FloatingText.HEALING);"],
   ["int healed = hero.heal( hero.HT );",
    "hero.sprite.emitter().start(Speck.factory(Speck.HEALING), 0.4f, 4);",
    "if (healed > 0) {",
    "\thero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   66, "int healed = hero.heal( hero.HT );")

op("actors/buffs/Corruption.java",
   ["target.HP = target.HT;",
    "target.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(target.HT), FloatingText.HEALING);"],
   ["int healed = target.heal( target.HT );",
    "if (healed > 0) {",
    "\ttarget.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);",
    "}"],
   40, "int healed = target.heal( target.HT );")


# =====================================================================
# runner
# =====================================================================
def die(msg):
    print("[FAIL] " + msg)
    sys.exit(1)

def main():
    dry = "--dry" in sys.argv
    if not os.path.isdir(BAK):
        os.makedirs(BAK)
    by_file = {}
    for o in OPS:
        by_file.setdefault(o["path"], []).append(o)

    total_delta = 0
    applied, skipped = 0, 0

    for rel, ops in by_file.items():
        full = os.path.join(ROOT, rel.replace("/", os.sep))
        if not os.path.isfile(full):
            die("missing file: " + full)
        raw = open(full, "rb").read()
        # 行尾体检：单一样式（纯 CRLF 或纯 LF），不允许混存
        crlf_before = raw.count(b"\r\n")
        bare = raw.replace(b"\r\n", b"")
        bare_lf = bare.count(b"\n")
        bare_cr = bare.count(b"\r")
        if bare_cr:
            die("bare CR found in " + rel)
        if crlf_before and bare_lf:
            die("MIXED eol in " + rel)
        eol = "\r\n" if crlf_before else "\n"

        text = raw.decode("utf-8").replace("\r\n", "\n")

        f_applied, f_skipped = 0, 0
        shift = 0   # 已应用 op 造成的行数位移（期望行号按原文件计）
        for o in ops:
            pat = build_pat(o["old"])
            rgx = re.compile(pat, re.M)
            ms = list(rgx.finditer(text))
            if len(ms) == 0:
                if o["marker"] in text:
                    print("  [skip] already applied: %s :: %s" % (rel, o["marker"]))
                    f_skipped += 1
                    continue
                die("no match in %s for op at line %d\n--- old ---\n%s" % (rel, o["line"], "\n".join(o["old"])))
            if len(ms) != o["expect"]:
                die("match count %d != %d in %s for op at line %d" % (len(ms), o["expect"], rel, o["line"]))
            m = ms[0]
            got_line = text[:m.start()].count("\n") + 1
            if got_line != o["line"] + shift:
                die("position mismatch in %s: expected line %d (+shift %d = %d), got %d\n--- old ---\n%s"
                    % (rel, o["line"], shift, o["line"] + shift, got_line, "\n".join(o["old"])))
            new_block = build_new(m, o["new"])
            text = text[:m.start()] + new_block + text[m.end():]
            shift += len(o["new"]) - len(o["old"])
            f_applied += 1

        if f_applied == 0:
            continue
        if dry:
            applied += f_applied
            skipped += f_skipped
            print("  [dry] %-52s %d op(s) would apply" % (rel, f_applied))
            continue

        # 备份（每文件一份，首次写入前留档）
        dst = os.path.join(BAK, rel.replace("/", "__"))
        if not os.path.exists(dst):
            shutil.copy2(full, dst)

        out = text.replace("\n", eol)
        lines_after = out.count(eol)
        if eol == "\r\n" and out.replace("\r\n", "").count("\n"):
            die("eol reserialize failed: " + rel)
        open(full, "wb").write(out.encode("utf-8"))
        delta = lines_after - crlf_before
        total_delta += delta
        applied += f_applied
        skipped += f_skipped
        print("  [ok] %-52s %s lines %d -> %d (delta %+d)"
              % (rel, "CRLF" if eol == "\r\n" else "LF ", crlf_before, lines_after, delta))

    print("\napplied=%d skipped=%d  CRLF delta total=%+d" % (applied, skipped, total_delta))
    print("backup dir: " + BAK)

if __name__ == "__main__":
    main()
