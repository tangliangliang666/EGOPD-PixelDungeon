/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AcceleratingFuture;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AccelerationShot;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BodyTheater;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Brag;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CounterBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.EnhancedRings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ExecutionUnleashed;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FamilyBetrayal;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GritTeethBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HuntingTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Inspiration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Instruction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeRegen;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Release;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevealedArea;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevengeTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ScrollEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TempStrength;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TremblingScorch;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.WandEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.Ratmogrify;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.DivineSense;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.RecallInscription;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Flare;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.LeafParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.Artifact;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.food.FrozenCarpaccio;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.Runestone;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfIntuition;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PalermoFencing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

public enum Talent {

	//==========================================================================
	// 【判据红线】天赋的生效判据只能是「这英雄身上有没有这门天赋的点数」：
	//   hero.hasTalent(X)  或  hero.pointsInTalent(X) > 0
	//
	// 【绝不要】用 hero.heroClass == HeroClass.XXX 当天赋判定的替身。蜕变卷轴
	//   （ScrollOfMetamorphosis）会把别职业的天赋经 Hero.metamorphedTalents 塞进
	//   Hero.talents 的同一层槽位 ⇒「战士身上有过人的毅力」是完全合法的状态；
	//   职业判定会让这些天赋在别职业身上【静默失效】（点满了却零效果、不报错、无提示）。
	//
	// 职业判定只在该机制【本身就是该职业独有】时才用（蜕变卷轴给不了，谈不上失效）：
	//   核心机制 buff（加速的未来 / 夸耀 / 复仇账簿充能）、盔甲技能、专属装备、
	//   子职业分支、台词与音效、图标分发、存档槽与图鉴。
	// 回归：_chk/verify_metamorph_talents.py
	//==========================================================================

	//Warrior T1
	HEARTY_MEAL(0), VETERANS_INTUITION(1), PROVOKED_ANGER(2), IRON_WILL(3),
	//Warrior T2
	IRON_STOMACH(4), LIQUID_WILLPOWER(5), RUNIC_TRANSFERENCE(6), LETHAL_MOMENTUM(7), IMPROVISED_PROJECTILES(8),
	//Warrior T3
	HOLD_FAST(9, 3), STRONGMAN(10, 3),
	//Berserker T3
	ENDLESS_RAGE(11, 3), DEATHLESS_FURY(12, 3), ENRAGED_CATALYST(13, 3),
	//Gladiator T3
	CLEAVE(14, 3), LETHAL_DEFENSE(15, 3), ENHANCED_COMBO(16, 3),
	//Heroic Leap T4
	BODY_SLAM(17, 4), IMPACT_WAVE(18, 4), DOUBLE_JUMP(19, 4),
	//Shockwave T4
	EXPANDING_WAVE(20, 4), STRIKING_WAVE(21, 4), SHOCK_FORCE(22, 4),
	//Endure T4
	SUSTAINED_RETRIBUTION(23, 4), SHRUG_IT_OFF(24, 4), EVEN_THE_ODDS(25, 4),

	//Mage T1
	EMPOWERING_MEAL(32), SCHOLARS_INTUITION(33), LINGERING_MAGIC(34), BACKUP_BARRIER(35),
	//Mage T2
	ENERGIZING_MEAL(36), INSCRIBED_POWER(37), WAND_PRESERVATION(38), ARCANE_VISION(39), SHIELD_BATTERY(40),
	//Mage T3
	DESPERATE_POWER(41, 3), ALLY_WARP(42, 3),
	//Battlemage T3
	EMPOWERED_STRIKE(43, 3), MYSTICAL_CHARGE(44, 3), EXCESS_CHARGE(45, 3),
	//Warlock T3
	SOUL_EATER(46, 3), SOUL_SIPHON(47, 3), NECROMANCERS_MINIONS(48, 3),
	//Elemental Blast T4
	BLAST_RADIUS(49, 4), ELEMENTAL_POWER(50, 4), REACTIVE_BARRIER(51, 4),
	//Wild Magic T4
	WILD_POWER(52, 4), FIRE_EVERYTHING(53, 4), CONSERVED_MAGIC(54, 4),
	//Warp Beacon T4
	TELEFRAG(55, 4), REMOTE_BEACON(56, 4), LONGRANGE_WARP(57, 4),

	//Rogue T1
	CACHED_RATIONS(64), THIEFS_INTUITION(65), SUCKER_PUNCH(66), PROTECTIVE_SHADOWS(67),
	//Rogue T2
	MYSTICAL_MEAL(68), INSCRIBED_STEALTH(69), WIDE_SEARCH(70), SILENT_STEPS(71), ROGUES_FORESIGHT(72),
	//Rogue T3
	ENHANCED_RINGS(73, 3), LIGHT_CLOAK(74, 3),
	//Assassin T3
	ENHANCED_LETHALITY(75, 3), ASSASSINS_REACH(76, 3), BOUNTY_HUNTER(77, 3),
	//Freerunner T3
	EVASIVE_ARMOR(78, 3), PROJECTILE_MOMENTUM(79, 3), SPEEDY_STEALTH(80, 3),
	//Smoke Bomb T4
	HASTY_RETREAT(81, 4), BODY_REPLACEMENT(82, 4), SHADOW_STEP(83, 4),
	//Death Mark T4
	FEAR_THE_REAPER(84, 4), DEATHLY_DURABILITY(85, 4), DOUBLE_MARK(86, 4),
	//Shadow Clone T4
	SHADOW_BLADE(87, 4), CLONED_ARMOR(88, 4), PERFECT_COPY(89, 4),

	//Huntress T1
	NATURES_BOUNTY(96), SURVIVALISTS_INTUITION(97), FOLLOWUP_STRIKE(98), NATURES_AID(99),
	//Huntress T2
	INVIGORATING_MEAL(100), LIQUID_NATURE(101), REJUVENATING_STEPS(102), HEIGHTENED_SENSES(103), DURABLE_PROJECTILES(104),
	//Huntress T3
	POINT_BLANK(105, 3), SEER_SHOT(106, 3),
	//Sniper T3
	FARSIGHT(107, 3), SHARED_ENCHANTMENT(108, 3), SHARED_UPGRADES(109, 3),
	//Warden T3
	DURABLE_TIPS(110, 3), BARKSKIN(111, 3), SHIELDING_DEW(112, 3),
	//Spectral Blades T4
	FAN_OF_BLADES(113, 4), PROJECTING_BLADES(114, 4), SPIRIT_BLADES(115, 4),
	//Natures Power T4
	GROWING_POWER(116, 4), NATURES_WRATH(117, 4), WILD_MOMENTUM(118, 4),
	//Spirit Hawk T4
	EAGLE_EYE(119, 4), GO_FOR_THE_EYES(120, 4), SWIFT_SPIRIT(121, 4),

	//Duelist T1
	STRENGTHENING_MEAL(128), ADVENTURERS_INTUITION(129), PATIENT_STRIKE(130), AGGRESSIVE_BARRIER(131),
	//Duelist T2
	FOCUSED_MEAL(132), LIQUID_AGILITY(133), WEAPON_RECHARGING(134), LETHAL_HASTE(135), SWIFT_EQUIP(136),
	//Duelist T3
	PRECISE_ASSAULT(137, 3), DEADLY_FOLLOWUP(138, 3),
	//Champion T3
	VARIED_CHARGE(139, 3), TWIN_UPGRADES(140, 3), COMBINED_LETHALITY(141, 3),
	//Monk T3
	UNENCUMBERED_SPIRIT(142, 3), MONASTIC_VIGOR(143, 3), COMBINED_ENERGY(144, 3),
	//Challenge T4
	CLOSE_THE_GAP(145, 4), INVIGORATING_VICTORY(146, 4), ELIMINATION_MATCH(147, 4),
	//Elemental Strike T4
	ELEMENTAL_REACH(148, 4), STRIKING_FORCE(149, 4), DIRECTED_POWER(150, 4),
	//Feint T4
	FEIGNED_RETREAT(151, 4), EXPOSE_WEAKNESS(152, 4), COUNTER_ABILITY(153, 4),

	//Cleric T1
	SATIATED_SPELLS(160), HOLY_INTUITION(161), SEARING_LIGHT(162), SHIELD_OF_LIGHT(163),
	//Cleric T2
	ENLIGHTENING_MEAL(164), RECALL_INSCRIPTION(165), SUNRAY(166), DIVINE_SENSE(167), BLESS(168),
	//Cleric T3
	CLEANSE(169, 3), LIGHT_READING(170, 3),
	//Priest T3
	HOLY_LANCE(171, 3), HALLOWED_GROUND(172, 3), MNEMONIC_PRAYER(173, 3),
	//Paladin T3
	LAY_ON_HANDS(174, 3), AURA_OF_PROTECTION(175, 3), WALL_OF_LIGHT(176, 3),
	//Ascended Form T4
	DIVINE_INTERVENTION(177, 4), JUDGEMENT(178, 4), FLASH(179, 4),
	//Trinity T4
	BODY_FORM(180, 4), MIND_FORM(181, 4), SPIRIT_FORM(182, 4),
	//Power of Many T4
	BEAMING_RAY(183, 4), LIFE_LINK(184, 4), STASIS(185, 4),

	//神谕代行者（Oracle）——图标直接使用 talent_icons.png 第八行的绝对索引（224 起，每行 32 列）
	//注意：不得在 icon() 里按当前职业做全局偏移（会使其他职业天赋图标越界，蜕变卷轴界面即因此闪退）
	//T1
	NOTE_IN_MEAL(224), ORACULAR_INTUITION(225), TARGETED_STRIKE(226), ORACULAR_BLESSING(227),
	//T2
	INSTRUCTION_MEAL(228), SCROLLED_INSTRUCTION(229), BLADE_RELEASE(230), INSTRUCTION_SENSE(231), SHIFTING_FATE(232),
	//T3
	INSTRUCTION_ADDICTION(233, 3), FALSE_FATHER(234, 3),

	//神谕代行者专精天赋（T3）
	//神的宠儿
	FORTUNE_FAVORITE(235, 3), KARMA_TRANSFER(236, 3), FREE_WILL(237, 3),
	//命运弃子
	FORSAKEN_MARK(238, 3), KARMA_CAP(239, 3), RAPID_BUZZ(240, 3),

	//神谕代行者四阶天赋（盔甲技能「心-命运」的分支天赋，第八行17/18/19）
	UNMOVING_HEART(241, 4), UNBOUND_HEART(242, 4), IMMORTAL_HEART(243, 4),

	//神谕代行者四阶天赋（盔甲技能「Furioso-Replica」的分支天赋，第八行20/21/22）
	PERFECT_REPLICATION(244, 4), DIVINE_ARTISTRY(245, 4), REUNION(246, 4),

	//神谕代行者四阶天赋（盔甲技能「拼好饭」的分支天赋，第八行23/24/25）
	HEALING_LUNCHBOX(247, 4), PRAYER_BEFORE_MEALS(248, 4), TASTY_MEAL(249, 4),

	//环指大师（RING_MASTER）——图标使用 talent_icons.png 第九行的绝对索引（256 起，每行 32 列）
	//T1
	PROSTHETIC_BODY(256), LOOKOUT(257), ANATOMY(258), WOUND(259),
	//T2
	INSPIRED_MEAL(260), LIQUID_FLESH(261), MATERIAL_HARVEST(262), BONE_WEAVING(263), ART_CRITIQUE(264),
	//T3（环指大师通用天赋，不随子职业变化）
	BODY_THEATER(265, 3), OUTDATED_ART(266, 3),
	//T3（画廊导师专精天赋，talent_icons.png 第九行 267~269，用户已绘制人偶头部系列图标）
	EXHIBIT_MAINTENANCE(267, 3), GALLERY_TOUCHUP(268, 3), CROSS_EXHIBITION(269, 3),
	//T3（艺术之巅专精天赋，talent_icons.png 第九行 270~272，270/271 用户已绘制，272 待绘制）
	LEARN_UNKNOWN(270, 3), LOVE_FLESH_BLOOD(271, 3), BONE_TENDON_MELODY(272, 3),
	//T4（盔甲技能「闭馆」的天赋，talent_icons.png 第九行 18/19/20 列 = 273/274/275，用户已绘制，各 4 点）
	ENJOY_THE_SHOW(273, 4), SCATTERED_FLESH(274, 4), BECOME_THE_AUDIENCE(275, 4),
	//T4（盔甲技能「诱饵」的天赋，talent_icons.png 第九行 21/22/23 列 = 276/277/278，用户已绘制，各 4 点）
	SEDUCTIVE_GLOW(276, 4), EXPLOSIVE_BEAUTY(277, 4), OBSESSIVE_ADMIRATION(278, 4),
	//T4（盔甲技能「走廊」的天赋，talent_icons.png 第九行 24/25/26 列 = 279/280/281，用户已绘制，各 4 点）
	VANISHING_FORM(279, 4), CORRIDOR_VISION(280, 4), CLOSE_RANGE_SHUTTLE(281, 4),

	//拇指 前二老板（VALENCINA）——图标使用 talent_icons.png 第十行的绝对索引（288 起，每行 32 列）
	//T1（需用户在 talent_icons.png 第十行 1~4 列绘制图标）
	RESERVED_WINE(288), SWIFT_FENCING(289), BURN_SHOCK(290), GET_LOST(291),
	//T2（talent_icons.png 第十行 5~9 列 = 绝对索引 292~296，用户已绘制）
	HUNTING_MEAL(292), LIQUID_ALCOHOL(293), ACCEL_AMMO(294), HUNT_TARGET(295), FUTURE_VISION(296),
	//T3 通用（talent_icons.png 第十行 10/11 列 = 297/298，需用户绘制；各 3 点）——机制待设计（占位）
	VALENCINA_T3_GENERIC_1(297, 3), VALENCINA_T3_GENERIC_2(298, 3),
	//T3 战争英雄分支（talent_icons.png 第十行 12~14 列 = 299~301，需用户绘制；各 3 点）
	//  灼热追击 / 麻痹震击 / 剑刃狂澜（震颤-灼热体系，2026-09-07 已实现）
	VALENCINA_T3_WARHERO_1(299, 3), VALENCINA_T3_WARHERO_2(300, 3), VALENCINA_T3_WARHERO_3(301, 3),
	//T3 家族之耻分支（talent_icons.png 第十行 15~17 列 = 302~304，需用户绘制；各 3 点）
	//  极速追杀 / 叠加剑轨 / 宰杀处置（2026-09-07 已实现）
	VALENCINA_T3_SHAME_1(302, 3), VALENCINA_T3_SHAME_2(303, 3), VALENCINA_T3_SHAME_3(304, 3),

	//拇指 前二老板 四阶天赋（盔甲技能分支，talent_icons.png 第十行 18~26 列 = 305~313，需用户绘制；各 4 点）
	//盔甲技能「心-不光彩」：第十行 18/19/20 列 = 305/306/307（狂乱之心 / 耻辱之心 / 执拗之心）
	WILD_HEART(305, 4), SHAMEFUL_HEART(306, 4), STUBBORN_HEART(307, 4),
	//盔甲技能「处置」：第十行 21/22/23 列 = 308/309/310（爆碎收尾 / 枪拼刺杀 / 毫无悬念）
	BURST_FINISH(308, 4), LUNGE_STAB(309, 4), NO_SUSPENSE(310, 4),
	//盔甲技能「瞄准心脏」：第十行 24/25/26 列 = 311/312/313（弱点贯穿 / 碾杀虫豸 / 荣耀凯旋）
	WEAKNESS_PIERCE(311, 4), CRUSH_BUGS(312, 4), GLORIOUS_TRIUMPH(313, 4),

	//中指长兄（MIDDLE_FINGER）——图标使用 talent_icons.png 第十一行的绝对索引（320 起，每行 32 列）
	//T1（第十一行 1~4 列 = 320~323，用户已绘制）
	FITNESS_MEAL(320), WATCH_CLOSELY(321), PROUD_MUSCLES(322), YOU_CHEATED(323),
	//T2（第十一行 5~9 列 = 324~328，用户已绘制；五个）
	RECORD_MEAL(324), TATTOO_ENGRAVING(325), HANDY_TOY(326), NEVER_FORGET(327), TOO_HOT(328),
	//T3 通用（两个转职分支共用，talent_icons.png 第十一行 10/11 列 = 329/330，待绘制；各 3 点）
	PERSEVERANCE(329, 3), BEAST_FURY(330, 3),
	//T3 忠义巡礼者分支（talent_icons.png 第十一行 12/13/14 列 = 331/332/333，待绘制；各 3 点）
	//「加倍清算！」：为全部五式复仇技能追加效果（各层的具体加成见 VengeanceArts 的 javadoc）
	DOUBLE_RECKONING(331, 3),
	//「如数奉还」：敌人单次攻击的伤害跨过 5 / 10 点阈值时，这一击额外多叠怨恨标记
	FULL_REPAYMENT(332, 3),
	//「永志不忘」：标记持续无限 → 击杀带标记单位补 5% 账簿充能 → 对带标记单位获得灵视感知
	EVERLASTING_GRUDGE(333, 3),
	//T3 背叛家人者分支（talent_icons.png 第十一行 15/16/17 列 = 334/335/336，用户已绘制；各 3 点）
	//「得心应手」：莱瓦汀系列的力量需求 -2/-4/-6（图标是向下的箭头）
	EFFORTLESS_GRIP(334, 3),
	//「融化而死」：莱瓦汀系列命中施加「融化」，带融化的敌人受到的火焰伤害 +50%/+100%/+150%（图标是骷髅）
	MELT_TO_DEATH(335, 3),
	//「拆开包装」：每解封一层多 1 级额外等级 → 副手更高级再 +1 → 取消解封条件限制（图标是向上的箭头）
	UNWRAP(336, 3),
	//T4 盔甲技能分支（talent_icons.png 第十一行 18~26 列 = 337~345，用户已绘制；各 3 点）
	//—— 与其它职业同一版式：第 18~26 列按「技能×3」排列，第 27 列（346）是 HEROIC_ENERGY。
	//盔甲技能「咬紧牙关」（GritTeeth）
	//「野兽意志」：持续时间 +3 / +5 / +10 回合
	BEAST_WILL(337, 3),
	//「濒亡狂怒」：持续期间血量等于 0 时，造成的伤害 +100% / +200% / +300%
	NEAR_DEATH_FURY(338, 3),
	//「绝境迫发」：持续时间结束后回复 5% / 10% / 15% 最大生命值
	DESPERATE_BURST(339, 3),
	//盔甲技能「永不遗忘」（NeverForget）
	//「家人的心意」：范围内有友方单位时，友方 200%/300%/必定击杀，敌方 300%/400%/600%
	FAMILY_HEART(340, 3),
	//「大闹一场吧」：攻击范围 5×5 / 7×7 / 9×9 圆形
	MAKE_A_SCENE(341, 3),
	//「我会牢记在心」：击杀了友方单位时获得 5 / 10 / 15 回合神器充能
	KEEP_IN_MIND(342, 3),
	//T4 盔甲技能「其三」（talent_icons.png 第十一行 24/25/26 列 = 343/344/345，用户已绘制）
	//盔甲技能「即刻处刑[莱瓦汀]」（InstantExecution）
	//「没有打开账簿的必要」：使用技能时立刻吞掉复仇账簿的全部充能，按 0.5/1/1.5 倍折算成伤害加成
	NO_LEDGER_NEEDED(343, 3),
	//「好久没解放到这种程度了」：使用技能时莱瓦汀系列获得 1/2/3 临时等级，持续 10/15/20 回合
	OVERDUE_RELEASE(344, 3),
	//「只属于我的传说之剑」：血量低于 50%/40%/30% 时，使用技能消耗的充能依次再降 20%/10%/10%
	LEGENDARY_BLADE(345, 3),

	//universal T4
	HEROIC_ENERGY(26, 4), //See icon() and title() for special logic for this one
	//Ratmogrify T4
	RATSISTANCE(215, 4), RATLOMACY(216, 4), RATFORCEMENTS(217, 4);

	public static class ImprovisedProjectileCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.15f, 0.2f, 0.5f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LethalMomentumTracker extends FlavourBuff{};
	public static class StrikingWaveTracker extends FlavourBuff{};
	public static class WandPreservationCounter extends CounterBuff{{revivePersists = true;}};
	public static class EmpoweredStrikeTracker extends FlavourBuff{
		//blast wave on-hit doesn't resolve instantly, so we delay detaching for it
		public boolean delayedDetach = false;
	};
	public static class ProtectiveShadowsTracker extends Buff {
		float barrierInc = 0.5f;

		@Override
		public boolean act() {
			//barrier every 2/1 turns, to a max of 3/5
			if (((Hero)target).hasTalent(Talent.PROTECTIVE_SHADOWS) && target.invisible > 0){
				Barrier barrier = Buff.affect(target, Barrier.class);
				if (barrier.shielding() < 1 + 2*((Hero)target).pointsInTalent(Talent.PROTECTIVE_SHADOWS)) {
					barrierInc += 0.5f * ((Hero) target).pointsInTalent(Talent.PROTECTIVE_SHADOWS);
				}
				if (barrierInc >= 1){
					barrierInc = 0;
					barrier.incShield(1);
				} else {
					barrier.incShield(0); //resets barrier decay
				}
			} else {
				detach();
			}
			spend( TICK );
			return true;
		}

		private static final String BARRIER_INC = "barrier_inc";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( BARRIER_INC, barrierInc);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			barrierInc = bundle.getFloat( BARRIER_INC );
		}
	}
	public static class BountyHunterTracker extends FlavourBuff{};
	public static class RejuvenatingStepsCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.35f, 0.15f); }
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / (15 - 5*Dungeon.hero.pointsInTalent(REJUVENATING_STEPS)), 1); }
	};
	public static class RejuvenatingStepsFurrow extends CounterBuff{{revivePersists = true;}};
	public static class SeerShotCooldown extends FlavourBuff{
		public int icon() { return target.buff(RevealedArea.class) != null ? BuffIndicator.NONE : BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.7f, 0.4f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	};
	public static class SpiritBladesTracker extends FlavourBuff{};
	public static class PatientStrikeTracker extends Buff {
		public int pos;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		@Override
		public boolean act() {
			if (pos != target.pos) {
				detach();
			} else {
				spend(TICK);
			}
			return true;
		}
		private static final String POS = "pos";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(POS, pos);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			pos = bundle.getInt(POS);
		}
	};
	public static class AggressiveBarrierCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 50); }
	};
	public static class LiquidAgilEVATracker extends FlavourBuff{
		{
			//detaches after hero acts, not after mobs act
			actPriority = HERO_PRIO+1;
		}
	};
	public static class LiquidAgilACCTracker extends FlavourBuff{
		public int uses;

		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }

		private static final String USES = "uses";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(USES, uses);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			uses = bundle.getInt(USES);
		}
	};

	//神谕代行者 T2：卷藏指令的 tracker（参考液蕴机敏）
	public static class ScrolledEVATracker extends FlavourBuff{
		{
			actPriority = HERO_PRIO+1;
		}
	};
	public static class ScrolledACCTracker extends FlavourBuff{
		public int uses;

		{ type = buffType.POSITIVE; }

		private static final String USES = "uses";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(USES, uses);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			uses = bundle.getInt(USES);
		}
	};
	public static class LethalHasteCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.35f, 0f, 0.7f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 100); }
	};
	public static class SwiftEquipCooldown extends FlavourBuff{
		public boolean secondUse;
		public boolean hasSecondUse(){
			return secondUse;
		}

		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) {
			if (hasSecondUse()) icon.hardlight(0.85f, 0f, 1.0f);
			else                icon.hardlight(0.35f, 0f, 0.7f);
		}
		public float iconFadePercent() { return GameMath.gate(0, visualcooldown() / 20f, 1); }

		private static final String SECOND_USE = "second_use";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(SECOND_USE, secondUse);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			secondUse = bundle.getBoolean(SECOND_USE);
		}
	};
	public static class DeadlyFollowupTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0.5f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	}
	public static class PreciseAssaultTracker extends FlavourBuff{
		{ type = buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(1f, 1f, 0.0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	};
	public static class VariedChargeTracker extends Buff{
		public Class weapon;

		private static final String WEAPON    = "weapon";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(WEAPON, weapon);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			weapon = bundle.getClass(WEAPON);
		}
	}
	public static class CombinedLethalityAbilityTracker extends FlavourBuff{
		public MeleeWeapon weapon;
	};
	public static class CombinedEnergyAbilityTracker extends FlavourBuff{
		public boolean monkAbilused = false;
		public boolean wepAbilUsed = false;

		private static final String MONK_ABIL_USED  = "monk_abil_used";
		private static final String WEP_ABIL_USED   = "wep_abil_used";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(MONK_ABIL_USED, monkAbilused);
			bundle.put(WEP_ABIL_USED, wepAbilUsed);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			monkAbilused = bundle.getBoolean(MONK_ABIL_USED);
			wepAbilUsed = bundle.getBoolean(WEP_ABIL_USED);
		}
	}
	public static class CounterAbilityTacker extends FlavourBuff{}
	public static class SatiatedSpellsTracker extends Buff{
		@Override
		public int icon() {
			return BuffIndicator.SPELL_FOOD;
		}
	}
	//used for metamorphed searing light
	public static class SearingLightCooldown extends FlavourBuff{
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		public void tintIcon(Image icon) { icon.hardlight(0f, 0f, 1f); }
		public float iconFadePercent() { return Math.max(0, visualcooldown() / 20); }
	}

	int icon;
	int maxPoints;

	// tiers 1/2/3/4 start at levels 2/7/13/21
	public static int[] tierLevelThresholds = new int[]{0, 2, 7, 13, 21, 31};

	Talent( int icon ){
		this(icon, 2);
	}

	Talent( int icon, int maxPoints ){
		this.icon = icon;
		this.maxPoints = maxPoints;
	}

	public int icon(){
		int result;
		if (this == HEROIC_ENERGY){
			if (Ratmogrify.useRatroicEnergy){
				return 218; //特殊事件图标，不做职业偏移
			}
			HeroClass cls = Dungeon.hero != null ? Dungeon.hero.heroClass : GamesInProgress.selectedClass;
			switch (cls){
				case WARRIOR: default:
					result = 26;
					break;
				case MAGE:
					result = 58;
					break;
				case ROGUE:
					result = 90;
					break;
				case HUNTRESS:
					result = 122;
					break;
				case DUELIST:
					result = 154;
					break;
				case CLERIC:
					result = 186;
					break;
				case ORACLE:
				result = 250; //神谕代行者的充能天赋图标（三个盔甲技能通用，talent_icons.png 第八行26 = 224+26）
				break;
			case RING_MASTER:
				result = 282; //环指大师的充能天赋图标（talent_icons.png 第九行26 = 256+26）
				break;
			case VALENCINA:
				result = 314; //拇指 前二老板的充能天赋图标（talent_icons.png 第十行27 = 288+26）
				break;
			case MIDDLE_FINGER:
				result = 346; //中指长兄的充能天赋图标（talent_icons.png 第十一行27 = 320+26，待绘制）
				break;
			}
		} else {
			result = icon;
		}
		return result;
	}

	public int maxPoints(){
		return maxPoints;
	}

	public String title(){
		if (this == HEROIC_ENERGY && Ratmogrify.useRatroicEnergy){
			return Messages.get(this, name() + ".rat_title");
		}
		return Messages.get(this, name() + ".title");
	}

	public final String desc(){
		return desc(false);
	}

	public String desc(boolean metamorphed){
		if (metamorphed){
			String metaDesc = Messages.get(this, name() + ".meta_desc");
			if (!metaDesc.equals(Messages.NO_TEXT_FOUND)){
				return Messages.get(this, name() + ".desc") + "\n\n" + metaDesc + hokmaDelayNote(this);
			}
		}
		return Messages.get(this, name() + ".desc") + hokmaDelayNote(this);
	}

	/**
	 * 考验「HOKMA（智慧）」把「不消耗回合」类效果压回固定延迟时，给受影响的天赋在说明末尾补一句实话。
	 * <p>只在考验真正生效时追加（{@code Dungeon.isTrialled}），所以正常游戏里天赋文案一字不变；
	 * 没有对应 `.hokma` 文本键的天赋也不会被追加，将来要加注只需补一条属性文本。</p>
	 */
	private static String hokmaDelayNote( Talent talent ){
		if (talent == null || !Dungeon.isTrialled( Trials.HOKMA )) return "";
		String note = Messages.get( Talent.class, talent.name() + ".hokma" );
		if (note.equals( Messages.NO_TEXT_FOUND )) return "";
		return "\n\n" + note;
	}

	public static void onTalentUpgraded( Hero hero, Talent talent ){
		//for metamorphosis
		if (talent == IRON_WILL && hero.heroClass != HeroClass.WARRIOR){
			Buff.affect(hero, BrokenSeal.WarriorShield.class);
		}

		if (talent == VETERANS_INTUITION && hero.pointsInTalent(VETERANS_INTUITION) == 2){
			if (hero.belongings.armor() != null && !ShardOfOblivion.passiveIDDisabled())  {
				hero.belongings.armor.identify();
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (hero.belongings.ring instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.ring.identify();
			}
			if (hero.belongings.misc instanceof Ring && !ShardOfOblivion.passiveIDDisabled()) {
				hero.belongings.misc.identify();
			}
			for (Item item : Dungeon.hero.belongings){
				if (item instanceof Ring){
					((Ring) item).setKnown();
				}
			}
		}
		if (talent == THIEFS_INTUITION && hero.pointsInTalent(THIEFS_INTUITION) == 1){
			if (hero.belongings.ring instanceof Ring) hero.belongings.ring.setKnown();
			if (hero.belongings.misc instanceof Ring) ((Ring) hero.belongings.misc).setKnown();
		}
		if (talent == ADVENTURERS_INTUITION && hero.pointsInTalent(ADVENTURERS_INTUITION) == 2){
			if (hero.belongings.weapon() != null && !ShardOfOblivion.passiveIDDisabled()){
				hero.belongings.weapon().identify();
			}
		}

		if (talent == PROTECTIVE_SHADOWS && hero.invisible > 0){
			Buff.affect(hero, Talent.ProtectiveShadowsTracker.class);
		}

		if (talent == LIGHT_CLOAK && hero.heroClass == HeroClass.ROGUE){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof CloakOfShadows){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((CloakOfShadows) item).activate(Dungeon.hero);
					}
				}
			}
		}

		if (talent == HEIGHTENED_SENSES || talent == FARSIGHT || talent == DIVINE_SENSE){
			Dungeon.observe();
		}

		if (talent == TWIN_UPGRADES || talent == DESPERATE_POWER
				|| talent == STRONGMAN || talent == DURABLE_PROJECTILES){
			Item.updateQuickslot();
		}

		if (talent == UNENCUMBERED_SPIRIT && hero.pointsInTalent(talent) == 3){
			Item toGive = new ClothArmor().identify();
			if (!toGive.collect()){
				Dungeon.level.drop(toGive, hero.pos).sprite.drop();
			}
			toGive = new Gloves().identify();
			if (!toGive.collect()){
				Dungeon.level.drop(toGive, hero.pos).sprite.drop();
			}
		}

		if (talent == LIGHT_READING && hero.heroClass == HeroClass.CLERIC){
			for (Item item : Dungeon.hero.belongings.backpack){
				if (item instanceof HolyTome){
					if (!hero.belongings.lostInventory() || item.keptThroughLostInventory()) {
						((HolyTome) item).activate(Dungeon.hero);
					}
				}
			}
		}

		//if we happen to have spirit form applied with a ring of might
		if (talent == SPIRIT_FORM){
			Dungeon.hero.updateHT(false);
		}
	}

	public static class CachedRationsDropped extends CounterBuff{{revivePersists = true;}};
	public static class NatureBerriesDropped extends CounterBuff{{revivePersists = true;}};
	//餐中纸条：整局累计吃出的卷轴数量
	public static class NoteInMealDropped extends CounterBuff{{revivePersists = true;}};
	//备用酒水：整局累计掉落的酒水数量（与前代存档兼容无关，纯计数）
	public static class ReservedWineDropped extends CounterBuff{{revivePersists = true;}};

	public static void onFoodEaten( Hero hero, float foodVal, Item foodSource ){
		//指令系统：难度2 简单指令（吃下一口干粮）的完成判定
		Instruction.onFoodEaten(hero);

		// 环指大师 T2：灵感一餐——进食时获得灵感层数（+1=1层，+2=2层），创作时消耗
		if (hero.hasTalent(INSPIRED_MEAL)){
			Inspiration insp = Buff.affect( hero, Inspiration.class );
			insp.stacks += hero.pointsInTalent( INSPIRED_MEAL );
		}

		// 拇指 前二老板 T2：狩猎一餐——进食恢复奥丁之眼充能（+1 = 1 点 / +2 = 1.25 点，未装备则静默跳过）
		if (hero.hasTalent(HUNTING_MEAL)){
			float eyeCharge = hero.pointsInTalent(HUNTING_MEAL) == 2 ? 1.25f : 1f;
			OdinsEye.restoreCharge( hero, eyeCharge );
		}

		// 中指长兄 T1：健身一餐——进食后获得临时力量（+1 = 1 点/75 回合，+2 = 2 点/150 回合）
		// 覆盖式刷新：再次进食直接重置时长与点数，不做叠层
		if (hero.hasTalent(FITNESS_MEAL)){
			int points = hero.pointsInTalent(FITNESS_MEAL);
			TempStrength.apply( hero, points, points >= 2 ? 150f : 75f );
		}

		// 中指长兄 T2：记录一餐——进食时为复仇账簿充能（+1 = 8%，+2 = 15%）
		// 「只花费 1 回合」那一半在 Food.eatingTime() / HornOfPlenty 的判定里，没有装备账簿时这里静默跳过
		if (hero.hasTalent(RECORD_MEAL)){
			RevengeLedger.chargeByTalent( hero, hero.pointsInTalent(RECORD_MEAL) >= 2 ? 15f : 8f );
		}

		if (hero.hasTalent(HEARTY_MEAL)){
			//4/6 HP healed, when hero is below 33% health (with a little rounding up)
			if (hero.HP/(float)hero.HT < 0.334f) {
				int healing = 2 + 2 * hero.pointsInTalent(HEARTY_MEAL);
				int healed = hero.heal( healing );
				if (healed > 0) {
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);
				}

			}
		}
		//神谕代行者 T1：餐中纸条——从口粮中吃出隐藏的卷轴（整局最多 +1 两张、+2 四张，不含升级卷轴，掉落在地上）
		if (hero.hasTalent(NOTE_IN_MEAL)){
			int limit = 2 * hero.pointsInTalent(NOTE_IN_MEAL); //整局上限
			int dropped = hero.buff(NoteInMealDropped.class) != null
					? (int) hero.buff(NoteInMealDropped.class).count() : 0;
			if (dropped < limit){
				//每次进食出 1 张，直到整局上限
				java.util.ArrayList<Class<? extends Item>> candidates = new java.util.ArrayList<>();
				for (Class<?> c : Generator.Category.SCROLL.classes){
					if (c != ScrollOfUpgrade.class) candidates.add((Class<? extends Item>) c); //升级卷轴是特殊资源，不产出
				}
				Item scroll = Reflection.newInstance(Random.element(candidates));
				if (scroll != null){
					Dungeon.level.drop(scroll, hero.pos); //卷轴掉落在地上，方便确认
					Buff.count(hero, NoteInMealDropped.class, 1);
				}
			}
		}
		//神谕代行者 T2：指令一餐——进食后获得随机一种正面效果（冷冻的肉），+2 额外消除随机一种负面效果
		if (hero.hasTalent(INSTRUCTION_MEAL)){
			FrozenCarpaccio.effect(hero); //随机正面效果（隐形/树皮/净化/恢复），套用冷冻的肉
			if (hero.pointsInTalent(INSTRUCTION_MEAL) == 2){
				removeRandomNegative(hero);
			}
		}
		if (hero.hasTalent(IRON_STOMACH)){
			if (hero.cooldown() > 0) {
				Buff.affect(hero, WarriorFoodImmunity.class, hero.cooldown());
			}
		}
		if (hero.hasTalent(EMPOWERING_MEAL)){
			//2/3 bonus wand damage for next 3 zaps
			Buff.affect( hero, WandEmpower.class).set(1 + hero.pointsInTalent(EMPOWERING_MEAL), 3);
			ScrollOfRecharging.charge( hero );
		}
		int wandChargeTurns = 0;
		if (hero.hasTalent(ENERGIZING_MEAL)){
			//5/8 turns of recharging
			wandChargeTurns += 2 + 3*hero.pointsInTalent(ENERGIZING_MEAL);
		}
		int artifactChargeTurns = 0;
		if (hero.hasTalent(MYSTICAL_MEAL)){
			//3/5 turns of recharging
			artifactChargeTurns += 1 + 2*hero.pointsInTalent(MYSTICAL_MEAL);
		}
		if (hero.hasTalent(INVIGORATING_MEAL)){
			//effectively 1/2 turns of haste
			Buff.prolong( hero, Haste.class, 0.67f+hero.pointsInTalent(INVIGORATING_MEAL));
		}
		if (hero.hasTalent(STRENGTHENING_MEAL)){
			//3 bonus physical damage for next 2/3 attacks
			Buff.affect( hero, PhysicalEmpower.class).set(3, 1 + hero.pointsInTalent(STRENGTHENING_MEAL));
		}
		if (hero.hasTalent(FOCUSED_MEAL)){
			if (hero.heroClass == HeroClass.DUELIST){
				//0.67/1 charge for the duelist
				Buff.affect( hero, MeleeWeapon.Charger.class ).gainCharge((hero.pointsInTalent(FOCUSED_MEAL)+1)/3f);
				ScrollOfRecharging.charge( hero );
			} else {
				// lvl/3 / lvl/2 bonus dmg on next hit for other classes
				Buff.affect( hero, PhysicalEmpower.class).set(Math.round(hero.lvl / (4f - hero.pointsInTalent(FOCUSED_MEAL))), 1);
			}
		}
		if (hero.hasTalent(SATIATED_SPELLS)){
			if (hero.heroClass == HeroClass.CLERIC) {
				Buff.affect(hero, SatiatedSpellsTracker.class);
			} else {
				//3/5 shielding, delayed up to 10 turns
				int amount = 1 + 2*hero.pointsInTalent(SATIATED_SPELLS);
				Barrier b = Buff.affect(hero, Barrier.class);
				if (b.shielding() <= amount){
					b.setShield(amount);
					b.delay(Math.max(10-b.cooldown(), 0));
				}
			}
		}
		if (hero.hasTalent(ENLIGHTENING_MEAL)){
			if (hero.heroClass == HeroClass.CLERIC) {
				HolyTome tome = hero.belongings.getItem(HolyTome.class);
				if (tome != null) {
					// 2/3 of a charge at +1, 1 full charge at +2
					tome.directCharge( (1+hero.pointsInTalent(ENLIGHTENING_MEAL))/3f );
					ScrollOfRecharging.charge(hero);
				}
			} else {
				//2/3 turns of recharging, both kinds
				wandChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
				artifactChargeTurns += 1 + hero.pointsInTalent(ENLIGHTENING_MEAL);
			}
		}

		//we process these at the end as they can stack together from some talents
		if (wandChargeTurns > 0){
			Buff.prolong( hero, Recharging.class, wandChargeTurns );
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE);
		}
		if (artifactChargeTurns > 0){
			ArtifactRecharge buff = Buff.affect( hero, ArtifactRecharge.class);
			if (buff.left() < artifactChargeTurns){
				buff.set(artifactChargeTurns).ignoreHornOfPlenty = foodSource instanceof HornOfPlenty;
			}
			ScrollOfRecharging.charge( hero );
			SpellSprite.show(hero, SpellSprite.CHARGE, 0, 1, 1);
		}
	}

	//==========================================================================
	// 拇指 前二老板：攻击钩子（由 Char.attack 在挥击开始 / 命中 / 结束后调用）
	//==========================================================================

	//战争英雄：一次攻击内的瞬时快照（攻击→伤害→结束全程同步，无重入）
	private static boolean whAttackLanded = false;
	private static boolean whParalysedBefore = false;
	private static boolean whFencingHit = false;
	private static int whFencingStacks = 0;

	/** 攻击开始前（命中判定前）：清空非剑术攻击打断的「叠加剑轨」，随后施加/叠加「狩猎目标」（+2 同时施加 5 回合残废）。 */
	public static void onHeroAttackStarted( Hero hero, Char enemy ){
		if (hero == null) return;

		//中指长兄 转职「背叛家人者」：手持莱瓦汀系列攻击前，若不在仇怨状态且力量不够，
		//自动预支 5% 账簿充能换一次「刚好挥得动」的力量补足（只维持这一击）。
		//必须在这里做：Char.attack 在命中判定与 damageRoll 之前调用本钩子，
		//补足因此同时作用于本击的精准（Weapon.accuracyFactor）与伤害（damageRoll）。
		FamilyBetrayal.onAttackStarted( hero, enemy );

		//家族之耻「叠加剑轨」：任何非巴勒莫剑术的攻击（平砍/远程/反击等）都会清空连击层数
		if (hero.subClass == HeroSubClass.FAMILY_SHAME && !PalermoFencing.isFencingSegment()){
			ValencinaSwordRail.clear( hero );
		}

		if (enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;
		if (!hero.hasTalent(HUNT_TARGET)) return;

		//+2：攻击前同时为目标施加 5 回合残废
		if (hero.pointsInTalent(HUNT_TARGET) >= 2){
			Buff.prolong( enemy, Cripple.class, 5f );
		}
		HuntingTarget.mark( enemy );
	}

	/** 攻击命中后、伤害生效前（Char.attack 于 enemy.damage 前调用）：
	 *  先施加各类“命中时”负面状态，确保它们在“解除麻痹/震颤-灼热判定”之前完成。
	 *  战争英雄分支：剑术叠加震颤-灼热、T1 燃烧、T2 概率麻痹，并快照本次攻击前的麻痹状态。 */
	public static void onHeroAttackLanded( Hero hero, Char enemy ){
		whAttackLanded = false; //防御性复位
		if (hero == null || enemy == null) return;
		if (hero.subClass != HeroSubClass.WAR_HERO) return;
		if (enemy.alignment != Char.Alignment.ENEMY) return;

		//快照必须先于本次新施加的负面状态（否则会把“本击新上的麻痹”误判成“攻击前已带麻痹”）
		boolean parBefore = enemy.buff( Paralysis.class ) != null;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;
		boolean fencing = PalermoFencing.isFencingSegment();

		//1) 巴勒莫剑术命中 → 叠加震颤-灼热（基础 3 层 + 加速的未来档位额外层，最高档位一次性）。
		//   tremorMarkOnce()：一次施放只施加一次——首个命中段叠层，多段斩的后续段不再重复叠加
		if (fencing && PalermoFencing.tremorMarkOnce()){
			int extra = stacks >= 7 ? 5 : stacks >= 5 ? 3 : stacks >= 3 ? 2 : stacks >= 1 ? 1 : 0;
			TremblingScorch.addStacks( enemy, 3 + extra );
		}

		//2) 分支特化天赋一：T1+1 加速的未来≥3 时所有攻击施加 2 回合燃烧；
		//   T1+3 剑术攻击且≥5 层时改为施加 8+(层数×2) 回合燃烧
		int t1 = hero.pointsInTalent( VALENCINA_T3_WARHERO_1 );
		if (t1 >= 1 && stacks >= 3){
			float burnTurns = 2f;
			if (t1 >= 3 && fencing && stacks >= 5){
				burnTurns = 8 + stacks * 2f;
			}
			TremblingScorch.applyBurning( enemy, burnTurns );
		}

		//3) 分支特化天赋二：T2+1 持有加速的未来时，攻击有 (30+目标震颤-灼热层数×2)% 概率施加 3 回合麻痹
		int t2 = hero.pointsInTalent( VALENCINA_T3_WARHERO_2 );
		if (t2 >= 1 && af != null && !enemy.isImmune( Paralysis.class )){
			TremblingScorch tremor = enemy.buff( TremblingScorch.class );
			int tremorStacks = (tremor != null) ? tremor.stacks : 0;
			if (Random.Int( 100 ) < 30 + 2 * tremorStacks){
				Buff.prolong( enemy, Paralysis.class, 3f );
			}
		}

		//4) 记录快照，供结束钩子判断“本次攻击是否解除目标麻痹”
		whAttackLanded = true;
		whParalysedBefore = parBefore;
		whFencingHit = fencing;
		whFencingStacks = (fencing) ? PalermoFencing.fencingCastStacks() : 0;
	}

	/** 攻击结束后（命中或未命中均触发）：战争英雄结算“攻击解除麻痹”→震颤-灼热连锁，随后消耗加速弹。 */
	public static void onHeroAttackResolved( Hero hero, Char enemy ){
		if (hero == null) return;

		//中指长兄 转职「背叛家人者」：这一击的力量补足【不在这里摘】——
		//攻击延迟（Weapon.baseDelay 读 Hero.STR()）发生在本钩子之后的
		//Hero.onAttackComplete → spend(attackDelay())，在这里摘会让 ×1.2ⁿ 的延迟惩罚漏下来。
		//真正的消费点＝Hero.spend()（见 FamilyBetrayal 类注释「生命周期」）。

		//战争英雄：本击命中、攻击前目标带麻痹、结算后麻痹已消失 → 本次攻击造成解除（原生受伤概率打断）
		if (hero.subClass == HeroSubClass.WAR_HERO && whAttackLanded){
			whAttackLanded = false;
			if (enemy != null && enemy.isAlive() && whParalysedBefore
					&& enemy.buff( Paralysis.class ) == null){
				TremblingScorch.onParalysisLifted( enemy, true, hero, whFencingHit, whFencingStacks );
			}
		}

		AccelerationShot.consumeOnAttack( hero );
	}

	//==========================================================================
	// 中指长兄：攻击钩子（由 Char.attack 在「命中后」与「未命中时」分别调用）
	//==========================================================================

	/** 命中敌人后（`enemy.damage()` 之后，可据此判断是否击杀）：叠「夸耀」。
	 *  +1：命中 +1 层、击杀 +2 层；+2：命中 +2 层、击杀 +3 层。 */
	public static void onHeroAttackBrag( Hero hero, Char enemy, boolean killed ){
		if (hero == null) return;
		if (!hero.hasTalent(WATCH_CLOSELY)) return;
		if (enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;

		int points = hero.pointsInTalent(WATCH_CLOSELY);
		int gain = points >= 2 ? 2 : 1;
		if (killed) gain++; //击杀额外 +1 层（+1 → 2 层，+2 → 3 层）

		Brag.gain( hero, gain );
	}

	/** 攻击未命中（含被闪避）：立刻失去所有「夸耀」。 */
	public static void onHeroAttackMissed( Hero hero, Char enemy ){
		if (hero == null) return;
		if (!hero.hasTalent(WATCH_CLOSELY)) return;
		if (enemy == null || enemy.alignment != Char.Alignment.ENEMY) return;

		Brag.clear( hero );
	}

	//==========================================================================
	// 中指长兄 T1「你犯规了」：无法闪避，把「本可闪避的概率」换算成减伤（照磐岩附魔）
	//==========================================================================

	//取数开关：为 true 时 Hero.defenseSkill 不做「无法闪避」的归零，用于取到真实闪避值。
	//命名沿用磐岩附魔的 Stone.testingEvasion() 思路，但独立成自己的开关，
	//避免两个来源（符文 + 天赋）在同一帧互相干扰。
	private static boolean cheatTestingEvasion = false;

	public static boolean youCheatedTestingEvasion(){
		return cheatTestingEvasion;
	}

	/** 把「本来能闪避的概率」折算成减伤（4 点命中率里 3 点算作减伤）——与磐岩附魔 `Stone.proc` 同一套算法。
	 *  取数时打开 testing 开关，让 defenseSkill 返回未归零的真实闪避值。 */
	public static int dodgeAsDamageReduction( Char attacker, Char defender, int damage ){
		if (attacker == null || defender == null || damage <= 0) return damage;

		float accuracy;
		float evasion;
		cheatTestingEvasion = true;
		try {
			//命中 / 闪避取数的唯一收口（与 Char.hit 同源）：考验 HOD 的敌方命中、
			//NETZACH 的敌方闪避都在这里一并生效，否则换算出的概率会与真实命中率漂移。
			accuracy = Trials.finalAccuracy( attacker, defender );
			evasion = Trials.finalEvasion( defender, attacker );
		} finally {
			cheatTestingEvasion = false;
		}

		//命中/闪避掷点的乘区（与 Char.hit 保持一致，否则换算出的概率会偏）
		if (attacker.buff( Bless.class ) != null) accuracy *= 1.25f;
		if (attacker.buff(  Hex.class ) != null) accuracy *= 0.8f;
		if (attacker.buff( Daze.class ) != null) accuracy *= 0.5f;
		for (ChampionEnemy buff : attacker.buffs( ChampionEnemy.class )){
			accuracy *= buff.evasionAndAccuracyFactor();
		}
		accuracy *= AscensionChallenge.statModifier( attacker );

		if (defender.buff( Bless.class ) != null) evasion *= 1.25f;
		if (defender.buff(  Hex.class ) != null) evasion *= 0.8f;
		if (defender.buff( Daze.class ) != null) evasion *= 0.5f;
		for (ChampionEnemy buff : defender.buffs( ChampionEnemy.class )){
			evasion *= buff.evasionAndAccuracyFactor();
		}
		evasion *= AscensionChallenge.statModifier( defender );
		evasion *= FerretTuft.evasionMultiplier();

		float hitChance;
		if (evasion >= accuracy){
			//闪避方更强：命中率最高 50%
			hitChance = (accuracy / evasion) / 2f;
		} else {
			hitChance = 1f - (evasion / accuracy) / 2f;
		}

		//75% 的闪避概率当作减伤（accuracy/evasion 可能为负，故钳位）
		hitChance = GameMath.gate( 0.25f, (1f + 3f * hitChance) / 4f, 1f );

		return (int)Math.ceil( damage * hitChance );
	}

	//==========================================================================
	// 中指长兄 T2（记录一餐 / 纹身铭刻 / 趁手玩具 / 永不遗忘 / 会很烫的！）
	//==========================================================================

	/** 纹身铭刻：消耗一支奥术刻笔换到的复仇账簿充能百分比（+1 = 25%，+2 = 50%）。 */
	public static int tattooChargePercent( Hero hero ){
		if (hero == null || !hero.hasTalent( TATTOO_ENGRAVING )) return 0;
		return hero.pointsInTalent( TATTOO_ENGRAVING ) >= 2 ? 50 : 25;
	}

	/** 纹身铭刻：消耗刻笔额外获得的「生命强化」＝当前生命上限的百分比（+1 = 25%，+2 = 50%）。
	 *  目前与 {@link #tattooChargePercent} 同表，独立成方法是为了日后能单独调数值。 */
	public static int tattooHealthBoostPercent( Hero hero ){
		if (hero == null || !hero.hasTalent( TATTOO_ENGRAVING )) return 0;
		return hero.pointsInTalent( TATTOO_ENGRAVING ) >= 2 ? 50 : 25;
	}

	/**
	 * 趁手玩具的冷却：冷却走完之前（本 buff 还在），主副切换照常消耗回合。
	 *
	 * <p>给它一个 {@code TIME} 图标是为了让「每 20/10 回合一次」变得可见——
	 * 否则玩家没有任何途径知道这次切换为什么收了回合（照 {@code RejuvenatingStepsCooldown} 的写法）。</p>
	 */
	public static class HandyToyCooldown extends FlavourBuff{
		public int icon() { return BuffIndicator.TIME; }
		public void tintIcon(Image icon) { icon.hardlight(0.75f, 0.6f, 0.2f); }
		public float iconFadePercent() {
			int points = Dungeon.hero != null ? Dungeon.hero.pointsInTalent(HANDY_TOY) : 1;
			return GameMath.gate(0, visualcooldown() / (points >= 2 ? 10f : 20f), 1);
		}
	};

	/**
	 * 趁手玩具：主副切换要支付的回合数。
	 *
	 * <p>默认 <b>1 回合</b>；持有天赋且<b>不在冷却中</b>时本次<b>不消耗回合</b>，
	 * 并立刻开始冷却（+1 = 20 回合，+2 = 10 回合）。冷却必须在「这一次免费的切换」上开始，
	 * 不能等冷却结束了才记，否则连点两下会白嫖两次。</p>
	 *
	 * @return 0 = 本次免费（并已开始冷却）；1 = 正常 1 回合
	 */
	public static float handyToySwapCost( Hero hero ){
		if (hero == null
				|| !hero.hasTalent( HANDY_TOY )
				|| hero.buff( HandyToyCooldown.class ) != null){
			return 1f;
		}
		//减 1 是因为本回合自身也占了一格冷却
		float turns = hero.pointsInTalent( HANDY_TOY ) >= 2 ? 9f : 19f;
		Buff.affect( hero, HandyToyCooldown.class, turns );
		return 0f;
	}

	/**
	 * 永不遗忘：<b>攻击命中</b>中指长兄的敌人被记为「报复对象」。
	 *
	 * <p>+1：只要命中英雄本人；+2：命中英雄或<b>任意友方单位</b>都算。
	 * 标记没有时限（「永不遗忘」），且同一个敌人只打一次。</p>
	 *
	 * <p>由 {@code Char.attack} 的命中分支调用——每次攻击都会走到，所以把天赋判定放在
	 * 最前面承担廉价短路的职责。</p>
	 */
	public static void onEnemyAttackLanded( Char attacker, Char victim ){
		if (attacker == null || victim == null) return;
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		if (!hero.hasTalent( NEVER_FORGET )) return;
		//只有「敌人」打过来才算：友军误伤、自伤、陷阱都不计入
		if (attacker.alignment != Char.Alignment.ENEMY) return;

		boolean friendly = (victim == hero)
				|| (hero.pointsInTalent( NEVER_FORGET ) >= 2 && victim.alignment == Char.Alignment.ALLY);
		if (!friendly) return;

		//已经是报复对象了就不再重复；标记无时限，也没有需要刷新/叠加的内容
		if (attacker.buff( RevengeTarget.class ) != null) return;

		//Buff.affect 内部走 Char.add：RevengeTarget 的 announced = true 会让 Char.add
		//自动在敌人头顶弹一次名字，所以这里不必再自己 showStatus（否则会弹两遍）
		Buff.affect( attacker, RevengeTarget.class );
	}

	/**
	 * 这一下伤害是否算「<b>英雄本人打出来的</b>」——「报复对象」+20%、「野兽狂怒」的增伤与
	 * 「濒亡狂怒」的增伤共用的<b>唯一</b>判据，消费点在 {@code Char.damage(int, Object)}。
	 *
	 * <p>两种情况：</p>
	 * <ol>
	 *   <li><b>以英雄本人为源</b>：近战、投掷武器、以及所有写 {@code enemy.damage(dmg, hero)} 的技能，
	 *       {@code src} 就是英雄本人；</li>
	 *   <li><b>法杖</b>：每个法杖的结算都写成 {@code ch.damage(damageRoll(), this)}，
	 *       {@code src} 是<b>法杖实例</b>而不是英雄，所以额外认一类 {@link Wand}。
	 *       这么认是安全的——法杖在游戏里只有英雄会真正打出伤害
	 *       （DM300 只借 {@code WandOfBlastWave.throwChar} 做位移，元素怪走的是
	 *       {@code CursedWand.effect(null, ...)}，都不以 Wand 为伤害源）。</li>
	 * </ol>
	 *
	 * <p>间接伤害不算：点燃地面后 {@code Burning} 的烫伤、中毒、流血等 {@code src} 是那些 buff 自己，
	 * 不属于「英雄造成的伤害」。</p>
	 *
	 * <p><b>为什么不判职业</b>（2026-09-20 修）：这三个天赋都是「谁点了谁受益」的中指长兄天赋，
	 * 但蜕变卷轴能把它们塞给别职业的英雄（{@code ScrollOfMetamorphosis}）——用
	 * {@code hero.heroClass == MIDDLE_FINGER} 当判据会让那些英雄的天赋静默失效。
	 * 现在统一成「{@code src} 就是玩家英雄本人」（{@code src == Dungeon.hero}），与职业无关。
	 * 判据红线见枚举体顶部的说明块。</p>
	 */
	public static boolean isHeroDealtDamage( Object src, Char victim ){
		Hero hero = Dungeon.hero;
		if (hero == null) return false;
		if (src instanceof Wand) return true;
		return src instanceof Hero && src != victim && src == hero;
	}

	/** 会很烫的！：血量低于 25%（+1）/ 50%（+2）时完全不受到燃烧伤害。 */
	public static boolean tooHotImmune( Hero hero ){
		if (hero == null) return false;
		if (!hero.hasTalent( TOO_HOT )) return false;
		int percent = hero.pointsInTalent( TOO_HOT ) >= 2 ? 50 : 25;
		return hero.HT > 0 && hero.HP * 100 < hero.HT * percent;
	}

	//==========================================================================
	// 中指长兄 通用 T3「过人的毅力」/「野兽狂怒」（两个转职分支都能点）
	//==========================================================================

	/** 过人的毅力：每投入 1 点，血量下限 +5 个百分点（+1/+2/+3 = 5/10/15）。 */
	private static final int PERSEVERANCE_FLOOR_STEP = 5;

	/** 野兽狂怒的触发线：血量**低于**该百分比时增伤/增受（严格小于）。 */
	private static final int BEAST_FURY_THRESHOLD = 50;

	/** 过人的毅力：当前的血量下限百分比（未投入该天赋时为 0，表示不生效）。 */
	public static int perseveranceFloorPercent( Hero hero ){
		if (hero == null) return 0;
		return hero.pointsInTalent( PERSEVERANCE ) * PERSEVERANCE_FLOOR_STEP;
	}

	/**
	 * 过人的毅力：把「这一下最终<b>打进血量</b>的伤害」夹进血量下限保护里。
	 *
	 * <p><b>护盾完全不参与减免</b>：护盾按原样整口吃伤害（{@code absorbed = min(dmg, 护盾总量)}），
	 * 下限保护只作用于<b>越过护盾、真正落进血量</b>的那一部分。所以「有盾时挨打」既不会因为
	 * 下限保护而少扣护盾（盾该碎还是碎），也不会因为盾厚就白拿一份减免——保护只保血，不保护盾。</p>
	 *
	 * <p>两段规则（{@code floorHP} ＝ 下限血量，{@code toHP} ＝ 越过护盾的伤害）：</p>
	 * <ol>
	 *   <li><b>HP 高于下限</b>：{@code toHP = min(toHP, HP - floorHP)}。
	 *       所以「站在下限之上」永远挨不出致命一击，最多被打到刚好贴住下限；</li>
	 *   <li><b>HP 已在下限之内</b>：{@code toHP = min(toHP, 1)}。
	 *       仍然会被打死，但绝不会是「一下秒」——想收掉残血的中指长兄必须一刀一刀地磨。</li>
	 * </ol>
	 *
	 * <p>返回值 ＝ 护盾吃掉的部分（原样） ＋ 夹取后落进血量的部分，交回 {@code Char.damage}
	 * 照常走「护盾先吃、再扣血」的结算，两边口径因此完全一致（护盾那一份一个字节都没动）。</p>
	 *
	 * <p>调用点是 {@code Hero.damage} 里 <b>{@code super.damage} 之前、{@code dmg} 已定稿处</b>：
	 * 那时护甲减伤、各类倍率都已结算完，而护盾/血量还一点没动——所以传进来的 {@code shield}
	 * 就是这一下能用的全部护盾。环境伤害、buff 掉血、陷阱、坠落等等一律走同一条路。</p>
	 *
	 * <p>下限血量<b>向上取整</b>（{@code HT=75}、5% ⇒ {@code ceil(3.75)=4}）：宁可多留一点血，
	 * 也不让下限在高生命上限、小百分比时被取整抹成 0。判据用 {@code HP > floorHP} 而不是再算一遍
	 * 百分比，避免「整数取整后的下限」与「百分比比较」两套口径打架。</p>
	 *
	 * <p><b>豁免「自伤换成长」型道具</b>（2026-09-18）：{@link SelfHarmCost} 的实现者——目前是
	 * 「蓄血圣杯」的<i>血祭</i>与「割腕」的<i>割腕</i>——造成的伤害<b>整段跳过本保护</b>，原样返回。
	 * 理由：这两笔伤害是<b>升级代价</b>，而它们的升级判据只是「挨完这一下还活着」。下限保护
	 * （压在下限里时单次最多 1 点）会让中指长兄<b>永远死不掉</b> ⇒ 变成无条件、无限制地刷等级
	 * （圣杯一路刷满 10 级、割腕想怎么割就怎么割），代价还被抹平。豁免后该疼就疼、该死就死，
	 * 与未点该天赋时的手感一致。<b>只</b>豁免本保护：护甲减伤、{@code RingOfTenacity} 之类的
	 * 通用减伤在更早的乘区里照常生效。</p>
	 *
	 * @param shield 这一下能吃的护盾总量（{@code hero.shielding()}，须在 {@code super.damage} 之前取；
	 *               饥饿那种「绕过护盾」的伤害来源应传 0，与 {@code Char.damage} 的结算保持一致）
	 * @param src    这一下伤害的来源（{@code Hero.damage} 的 {@code src} 原样传入）。
	 *               只为识别 {@link SelfHarmCost}；传 null 安全（按不豁免处理）
	 * @return 夹取后的伤害（正常返回入参）
	 */
	public static int perseveranceCap( Hero hero, int dmg, int shield, Object src ){
		if (dmg <= 0) return dmg;

		//「自伤换成长」的代价不参与下限保护（蓄血圣杯的血祭 / 割腕的割腕）。
		//放在 hero/天赋点数的判定之前：豁免与「是否点了过人的毅力」无关，点了也不打折。
		if (src instanceof SelfHarmCost) return dmg;

		int floorPct = perseveranceFloorPercent( hero );
		if (floorPct <= 0 || hero.HT <= 0) return dmg;

		int floorHP = Math.max( 1, (int)Math.ceil( hero.HT * floorPct / 100f ) );

		//护盾先照原样吃满，下限保护只夹剩下真正落进血量的那部分
		int absorbed = Math.max( 0, Math.min( dmg, shield ) );
		int toHP = dmg - absorbed;

		int cappedHP = (hero.HP > floorHP)
				? Math.min( toHP, hero.HP - floorHP )
				: Math.min( toHP, 1 );

		return absorbed + cappedHP;
	}

	/**
	 * 野兽狂怒：血量低于 50% 时，中指长兄<b>造成</b>与<b>受到</b>的伤害一并放大的倍率
	 * （+1/+2/+3 = ×1.1/×1.2/×1.4）；未满足条件返回 1（即乘上去等于没变）。
	 *
	 * <p>两个消费点各一次，绝不在同一笔伤害上叠乘两次：</p>
	 * <ul>
	 *   <li><b>受到</b>：{@code Hero.damage} 的 float 乘区起点（在护甲减伤之后、护盾结算之前）；</li>
	 *   <li><b>造成</b>：{@code Char.damage} 里 {@code src} 判定为「中指长兄打出来的」那一处
	 *       （复用 {@link #isHeroDealtDamage} 的口径：英雄本人为源 + 法杖，近战/投掷/技能/法杖全覆盖）。</li>
	 * </ul>
	 *
	 * <p>「血量<b>低于</b> 50%」取<b>严格小于</b>（整数比较 {@code HP*100 < HT*50}，与
	 * {@link #tooHotImmune} 同款写法，避免浮点误差）。</p>
	 */
	public static float beastFuryMultiplier( Hero hero ){
		if (hero == null) return 1f;
		int points = hero.pointsInTalent( BEAST_FURY );
		if (points <= 0) return 1f;
		if (hero.HT <= 0 || hero.HP * 100 >= hero.HT * BEAST_FURY_THRESHOLD) return 1f;

		if (points >= 3)      return 1.4f;
		else if (points == 2) return 1.2f;
		else                  return 1.1f;
	}

	//==========================================================================
	// 中指长兄 盔甲技能「咬紧牙关」（GritTeeth）的三个 T4 天赋
	//==========================================================================

	/** 咬紧牙关的基础持续时间（回合）。 */
	public static final float GRIT_TEETH_BASE_DURATION = 5f;

	/**
	 * 「咬紧牙关」的持续时间：基础 {@value #GRIT_TEETH_BASE_DURATION} 回合，
	 * 「野兽意志」+1/+2/+3 各<b>增加</b> 3/5/10 回合 ⇒ 8 / 10 / 15 回合。
	 */
	public static float gritTeethDuration( Hero hero ){
		float duration = GRIT_TEETH_BASE_DURATION;
		if (hero == null) return duration;

		int points = hero.pointsInTalent( BEAST_WILL );
		if (points >= 3)      return duration + 10f;
		else if (points == 2) return duration + 5f;
		else if (points == 1) return duration + 3f;
		return duration;
	}

	/**
	 * 「绝境迫发」：咬紧牙关结束时回复的血量（「最大生命值」的百分数向上取整，未投入返回 0）。
	 *
	 * <p>向上取整的理由与「过人的毅力」一致：宁可多给一点，也不让高生命上限、
	 * 小百分比时被取整抹成 0（那时这条天赋就等于不存在了）。</p>
	 */
	public static int gritTeethEndHeal( Hero hero ){
		if (hero == null) return 0;
		int percent = hero.pointsInTalent( DESPERATE_BURST ) * 5;   //5 / 10 / 15
		if (percent <= 0 || hero.HT <= 0) return 0;
		return Math.max( 1, (int)Math.ceil( hero.HT * percent / 100f ) );
	}

	/**
	 * 「濒亡狂怒」：咬紧牙关持续期间血量<b>等于 0</b> 时，自己打出去的伤害的倍率
	 * （+1/+2/+3 ⇒ ×2/×3/×4，即 +100%/200%/300%）；未满足条件返回 1。
	 *
	 * <p>三个条件：英雄本人（即拥有该天赋者）、当前血量真的等于 0、身上还挂着 {@link GritTeethBuff}。
	 * 第三条看着多余（0 血且没死本身就意味着免死生效中），但它把「万一有别的免死来源」
	 * 这种情况挡在外面，语义上与设计要求严格对齐。</p>
	 *
	 * <p>消费点在 {@code Char.damage} 里「英雄打出去的伤害」那一处
	 * （与 {@link #beastFuryMultiplier} 紧挨着、同一个 {@code isHeroDealtDamage} 判据内），
	 * 所以一笔伤害只会乘一次。</p>
	 */
	public static float nearDeathFuryMultiplier( Hero hero ){
		if (hero == null) return 1f;
		if (hero.HP > 0) return 1f;
		if (hero.buff( GritTeethBuff.class ) == null) return 1f;

		int points = hero.pointsInTalent( NEAR_DEATH_FURY );
		if (points <= 0) return 1f;
		return 1f + points;
	}

	//==========================================================================
	// 中指长兄 盔甲技能「即刻处刑[莱瓦汀]」（InstantExecution）的三个 T4 天赋
	//==========================================================================

	/** 「没有打开账簿的必要」把「吞掉的账簿充能」折算成伤害加成的倍率档位（+1/+2/+3 ⇒ 0.5/1/1.5）。 */
	public static final float LEDGER_DEVOUR_PER_POINT = 0.5f;

	/** 「好久没解放到这种程度了」给莱瓦汀系列的基础临时等级与每点多加的量（+1/+2/+3 ⇒ 1/2/3 级）。 */
	public static final int UNLEASH_LEVELS_PER_POINT = 1;
	/** 「好久没解放到这种程度了」的时长：基础 5 回合 + 每点 5 回合（+1/+2/+3 ⇒ 10/15/20 回合）。 */
	public static final float UNLEASH_BASE_DURATION = 5f, UNLEASH_DURATION_PER_POINT = 5f;

	/** 「只属于我的传说之剑」各档的残血门槛（百分比）与对应的额外减免：50%/20%、40%/10%、30%/10%。 */
	public static final int[] LEGENDARY_HP_THRESHOLDS = { 50, 40, 30 };
	public static final float[] LEGENDARY_REDUCTIONS = { 0.20f, 0.10f, 0.10f };

	/**
	 * 「没有打开账簿的必要」：使用「即刻处刑[莱瓦汀]」时，把吞掉的复仇账簿充能换算成
	 * <b>每一段伤害</b>的加成的倍率。
	 *
	 * <p>数量级说明：倍率乘的是<b>充能点数</b>（100 点 = 基础满格，账簿满级上限 200 点），
	 * 所以 +3 吞掉 100 点 ⇒ 每段 +150。这是一条「把整条资源一次性砸进一套连段」的天赋，
	 * 未点 / 账簿未装备 / 已诅咒时返回 0（调用方按「没得吃」处理）。</p>
	 */
	public static float ledgerDevourMultiplier( Hero hero ){
		if (hero == null) return 0f;

		int points = hero.pointsInTalent( NO_LEDGER_NEEDED );
		if (points <= 0) return 0f;
		return LEDGER_DEVOUR_PER_POINT * points;   //0.5 / 1.0 / 1.5
	}

	/**
	 * 「好久没解放到这种程度了」：使用「即刻处刑[莱瓦汀]」后，莱瓦汀系列获得的<b>临时等级</b>
	 * （+1/+2/+3 ⇒ 1/2/3 级）；未投入返回 0。
	 *
	 * <p>消费点是 {@link ExecutionUnleashed#apply}（由 {@code InstantExecution} 施加），
	 * 加成本身走 {@code SealedSwordBase.buffedLvl()}。</p>
	 */
	public static int executionUnleashLevels( Hero hero ){
		if (hero == null) return 0;
		return UNLEASH_LEVELS_PER_POINT * hero.pointsInTalent( OVERDUE_RELEASE );
	}

	/** 「好久没解放到这种程度了」的持续时间（+1/+2/+3 ⇒ 10/15/20 回合）；未投入返回 0。 */
	public static float executionUnleashDuration( Hero hero ){
		int points = (hero == null) ? 0 : hero.pointsInTalent( OVERDUE_RELEASE );
		if (points <= 0) return 0f;
		return UNLEASH_BASE_DURATION + UNLEASH_DURATION_PER_POINT * points;   //10 / 15 / 20
	}

	/**
	 * 「只属于我的传说之剑」：使用「即刻处刑[莱瓦汀]」时，消耗的充能要乘上的系数。
	 *
	 * <p>门槛按<b>当前血量百分比</b>逐档判定、<b>减免互相叠加</b>（用
	 * {@code HP*100 < HT*门槛} 比较，避免整除取整把边界算错）：</p>
	 * <ul>
	 *   <li>+1：血量 &lt; 50% ⇒ 减免 20%（×0.80）</li>
	 *   <li>+2：在 +1 之外，血量 &lt; 40% ⇒ 再减免 10%（×0.70）</li>
	 *   <li>+3：在 +1、+2 之外，血量 &lt; 30% ⇒ 再减免 10%（×0.60）</li>
	 * </ul>
	 * <p>「额外降低 10%」按<b>百分点</b>叠加（而不是×0.9 连乘）——这样三档的最终减免就是
	 * 40%，读起来与天赋文案一一对应。</p>
	 */
	public static float instantExecutionChargeFactor( Hero hero ){
		if (hero == null) return 1f;

		int points = hero.pointsInTalent( LEGENDARY_BLADE );
		if (points <= 0 || hero.HT <= 0) return 1f;

		float reduction = 0f;
		for (int i = 0; i < points && i < LEGENDARY_HP_THRESHOLDS.length; i++){
			if (hero.HP * 100 < hero.HT * LEGENDARY_HP_THRESHOLDS[i]){
				reduction += LEGENDARY_REDUCTIONS[i];
			}
		}
		return 1f - reduction;
	}

	//==========================================================================
	// 拇指 前二老板：家族之耻分支 + 通用 T3（动作太慢！/ 过往的烟霾）
	//==========================================================================

	/** 叠加剑轨（家族之耻 T2）：每次施放巴勒莫剑术结束后 +1 层（上限 5，层数供下一次剑术增伤）；
	 *  做出任何非剑术攻击时清空。 */
	public static class ValencinaSwordRail extends Buff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public static final int MAX_STACKS = 5;

		private int stacks = 0;

		@Override
		public int icon() {
			return BuffIndicator.SWORD_RAIL;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(stacks);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", stacks);
		}

		public static int stacks( Hero hero ){
			if (hero == null) return 0;
			ValencinaSwordRail rail = hero.buff( ValencinaSwordRail.class );
			return (rail != null) ? rail.stacks : 0;
		}

		public static void addStack( Hero hero ){
			if (hero == null) return;
			ValencinaSwordRail rail = hero.buff( ValencinaSwordRail.class );
			if (rail == null){
				rail = Buff.affect( hero, ValencinaSwordRail.class );
			}
			if (rail.stacks < MAX_STACKS){
				rail.stacks++;
				BuffIndicator.refreshHero();
			}
		}

		public static void clear( Hero hero ){
			if (hero == null) return;
			Buff.detach( hero, ValencinaSwordRail.class );
		}

		private static final String STACKS = "stacks";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( STACKS, stacks );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			stacks = bundle.getInt( STACKS );
		}
	}

	/** 通用 T3「动作太慢!」反击的冷却标记（+1/2/3 → 100/50/25 回合）。 */
	public static class ValencinaCounterCooldown extends FlavourBuff {
		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}
		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.15f, 0.2f, 0.5f );
		}
	}

	/** 家族之耻核心：预知眼每次激活（手动或自动，由 OdinsEye.startPrecognition 调用）→ +1 点剑术充能。 */
	public static void onPrecognitionActivated( Hero hero ){
		if (hero == null) return;
		if (hero.subClass != HeroSubClass.FAMILY_SHAME) return;
		Buff.affect( hero, MeleeWeapon.Charger.class ).gainCharge( 1f );
	}

	/** 通用 T3「动作太慢!」：英雄成功闪避敌方普通物理攻击后，对攻击者进行一次不消耗回合数的反击。
	 *  伤害等同巴勒莫剑术（基础伤害 ÷ 当前攻击延迟），走完整攻击判定（可闪避/暴击/触发命中类效果）；
	 *  攻击者须仍在巴勒莫射程（3 格）内。冷却 +1/2/3 → 100/50/25 回合。 */
	public static void onHeroDodgedEnemyAttack( Hero hero, Char attacker ){
		if (hero == null) return;
		int points = hero.pointsInTalent( VALENCINA_T3_GENERIC_1 );
		if (points <= 0) return;
		if (attacker == null || !attacker.isAlive()) return;
		if (attacker.alignment != Char.Alignment.ENEMY) return;
		if (!hero.isAlive() || hero.buff( Paralysis.class ) != null) return;
		if (hero.isCharmedBy( attacker )) return;
		if (hero.buff( ValencinaCounterCooldown.class ) != null) return;
		if (Dungeon.level.distance( hero.pos, attacker.pos ) > PalermoFencing.RANGE) return;

		//完整攻击判定：伤害公式与巴勒莫剑术一致（不进入突进/位移，视为自动突进的一击）
		float delay = hero.attackDelay();
		float dmgMulti = (delay > 0f) ? 1f / delay : 1f;
		AttackIndicator.target( attacker );
		hero.attack( attacker, dmgMulti, 0f, 1f );

		//反击后进入冷却（无论命中与否）
		float cd = (points == 3) ? 25f : (points == 2) ? 50f : 100f;
		Buff.prolong( hero, ValencinaCounterCooldown.class, cd );
	}

	/** 通用 T3「过往的烟霾」：被敌人攻击命中造成实际伤害、或受到流血 tick 伤害时，
	 *  放大本次伤害（+1 = +100% / +2 = +50% / +3 = +25%）并获得 2/3/4 回合「神器充能」。
	 *  返回调整后的伤害值（由 Hero.damage 在伤害结算前调用）。 */
	public static float valencinaSmoke( float damage, Object src, Hero hero ){
		if (damage <= 0 || hero == null) return damage;
		int points = hero.pointsInTalent( VALENCINA_T3_GENERIC_2 );
		if (points <= 0) return damage;

		boolean qualifies;
		if (src instanceof Char){
			qualifies = src != hero && ((Char) src).alignment == Char.Alignment.ENEMY;
		} else {
			qualifies = src instanceof Bleeding;
		}
		if (!qualifies) return damage;

		if (points == 3)        damage *= 1.25f;
		else if (points == 2)   damage *= 1.5f;
		else                    damage *= 2f;

		//获得神器充能回合（ArtifactRecharge 非 FlavourBuff，须用 affect+set 刷新语义）
		ArtifactRecharge recharge = Buff.affect( hero, ArtifactRecharge.class );
		if (recharge.left() < 1f + points){
			recharge.set( 1f + points );
		}
		return damage;
	}

	public static class WarriorFoodImmunity extends FlavourBuff{
		{ actPriority = HERO_PRIO+1; }
	}

	public static float itemIDSpeedFactor( Hero hero, Item item ){
		float factor = 1f;

		// Affected by both Warrior(1.75x/2.5x) and Duelist(2.5x/inst.) talents
		if (item instanceof MeleeWeapon){
			factor *= 1f + 1.5f*hero.pointsInTalent(ADVENTURERS_INTUITION); //instant at +2 (see onItemEquipped)
			factor *= 1f + 0.75f*hero.pointsInTalent(VETERANS_INTUITION);
		}
		// Affected by both Warrior(2.5x/inst.) and Duelist(1.75x/2.5x) talents
		if (item instanceof Armor){
			factor *= 1f + 0.75f*hero.pointsInTalent(ADVENTURERS_INTUITION);
			factor *= 1f + hero.pointsInTalent(VETERANS_INTUITION); //instant at +2 (see onItemEquipped)
		}
		// 3x/instant for Mage (see Wand.wandUsed())
		if (item instanceof Wand){
			factor *= 1f + 2.0f*hero.pointsInTalent(SCHOLARS_INTUITION);
		}
		// 3x/instant speed with Huntress talent (see MissileWeapon.proc)
		if (item instanceof MissileWeapon){
			factor *= 1f + 2.0f*hero.pointsInTalent(SURVIVALISTS_INTUITION);
		}
		// 2x/instant for Rogue (see onItemEqupped), also id's type on equip/on pickup
		if (item instanceof Ring){
			factor *= 1f + hero.pointsInTalent(THIEFS_INTUITION);
		}
		return factor;
	}

	public static void onPotionUsed( Hero hero, int cell, float factor, Item item ){
		if (hero.hasTalent(LIQUID_WILLPOWER)){
			// 6.5/10% of max HP
			int shieldToGive = Math.round( factor * hero.HT * (0.030f + 0.035f*hero.pointsInTalent(LIQUID_WILLPOWER)));
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldToGive), FloatingText.SHIELDING);
			Buff.affect(hero, Barrier.class).setShield(shieldToGive);
		}
		if (hero.hasTalent(LIQUID_NATURE)){
			ArrayList<Integer> grassCells = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS9){
				grassCells.add(cell+i);
			}
			Random.shuffle(grassCells);
			for (int grassCell : grassCells){
				Char ch = Actor.findChar(grassCell);
				if (ch != null && ch.alignment == Char.Alignment.ENEMY){
					//1/2 turns of roots
					Buff.affect(ch, Roots.class, factor * hero.pointsInTalent(LIQUID_NATURE));
				}
				if (Dungeon.level.map[grassCell] == Terrain.EMPTY ||
						Dungeon.level.map[grassCell] == Terrain.EMBERS ||
						Dungeon.level.map[grassCell] == Terrain.EMPTY_DECO){
					Level.set(grassCell, Terrain.GRASS);
					GameScene.updateMap(grassCell);
				}
				CellEmitter.get(grassCell).burst(LeafParticle.LEVEL_SPECIFIC, 4);
			}
			// 4/6 cells total
			int totalGrassCells = (int) (factor * (2 + 2 * hero.pointsInTalent(LIQUID_NATURE)));
			while (grassCells.size() > totalGrassCells){
				grassCells.remove(0);
			}
			for (int grassCell : grassCells){
				int t = Dungeon.level.map[grassCell];
				if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
						|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
						&& Dungeon.level.plants.get(grassCell) == null){
					Level.set(grassCell, Terrain.HIGH_GRASS);
					GameScene.updateMap(grassCell);
				}
			}
			Dungeon.observe();
		}
		if (hero.hasTalent(LIQUID_AGILITY)){
		Buff.prolong(hero, LiquidAgilEVATracker.class, hero.cooldown() + Math.max(0, factor-1));
		if (factor >= 0.5f){
			Buff.prolong(hero, LiquidAgilACCTracker.class, 5f).uses = Math.round(factor);
		}
	}
	// 环指大师 T2：液蕴血肉——使用药剂时获得「生命恢复」buff（最大生命值 5%/7.5%，力量/经验药剂或其炼金产物翻倍）
	// 高产量炼金物品的触发概率已由 Potion.talentChance 在调用前判定
	if (hero.hasTalent(LIQUID_FLESH)){
		int points = hero.pointsInTalent(LIQUID_FLESH);
		float pct = points == 1 ? 0.05f : 0.075f;
		// 力量药剂、经验药剂、异色力量药剂（神圣启示）、由力量药剂炼制的炼金物品（力量灵药）：恢复量翻倍
		if (item instanceof PotionOfStrength
				|| item instanceof PotionOfExperience
				|| item instanceof PotionOfDivineInspiration
				|| item instanceof ElixirOfMight){
			pct *= 2f;
		}
		int regen = Math.round( hero.HT * pct );
		if (regen > 0){
			LifeRegen lr = Buff.affect( hero, LifeRegen.class );
			lr.setRegen( regen );
		}
	}
	}

	public static void onScrollUsed( Hero hero, int pos, float factor, Class<?extends Item> cls ){
		//神谕代行者 T2：卷藏指令——阅读卷轴/秘卷期间获得闪避，5 回合内下一次近战攻击精准（参考液蕴机敏）
		if (hero.hasTalent(SCROLLED_INSTRUCTION)){
			Buff.prolong(hero, ScrolledEVATracker.class, 5f);
			Buff.prolong(hero, ScrolledACCTracker.class, 5f).uses = 1;
		}
		if (hero.hasTalent(INSCRIBED_POWER)){
			// 2/3 empowered wand zaps
			Buff.affect(hero, ScrollEmpower.class).reset((int) (factor * (1 + hero.pointsInTalent(INSCRIBED_POWER))));
		}
		if (hero.hasTalent(INSCRIBED_STEALTH)){
			// 3/5 turns of stealth
			Buff.affect(hero, Invisibility.class, factor * (1 + 2*hero.pointsInTalent(INSCRIBED_STEALTH)));
			Sample.INSTANCE.play( Assets.Sounds.MELD );
		}
		if (hero.hasTalent(RECALL_INSCRIPTION) && Scroll.class.isAssignableFrom(cls) && cls != ScrollOfUpgrade.class){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onRunestoneUsed( Hero hero, int pos, Class<?extends Item> cls ){
		if (hero.hasTalent(RECALL_INSCRIPTION) && Runestone.class.isAssignableFrom(cls)){
			if (hero.heroClass == HeroClass.CLERIC){
				Buff.prolong(hero, RecallInscription.UsedItemTracker.class, hero.pointsInTalent(RECALL_INSCRIPTION) == 2 ? 300 : 10).item = cls;
			} else {

				//don't trigger on 1st intuition use
				if (cls.equals(StoneOfIntuition.class) && hero.buff(StoneOfIntuition.IntuitionUseTracker.class) != null){
					return;
				}
				// 10/15%
				if (Random.Int(20) < 1 + hero.pointsInTalent(RECALL_INSCRIPTION)){
					Reflection.newInstance(cls).collect();
					GLog.p(Messages.get(Talent.class, RECALL_INSCRIPTION.name() + ".refunded"));
				}
			}
		}
	}

	public static void onArtifactUsed( Hero hero ){
		if (hero.hasTalent(ENHANCED_RINGS)){
			Buff.prolong(hero, EnhancedRings.class, 3f*hero.pointsInTalent(ENHANCED_RINGS));
		}

		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.DIVINE_SENSE)){
			Buff.prolong(Dungeon.hero, DivineSense.DivineSenseTracker.class, Dungeon.hero.cooldown()+1);
		}

		// 10/20/30%
		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.CLEANSE)
				&& Random.Int(10) < Dungeon.hero.pointsInTalent(Talent.CLEANSE)){
			boolean removed = false;
			for (Buff b : Dungeon.hero.buffs()) {
				if (b.type == Buff.buffType.NEGATIVE
						&& !(b instanceof LostInventory)) {
					b.detach();
					removed = true;
				}
			}
			if (removed && Dungeon.hero.sprite != null) {
				new Flare( 6, 32 ).color(0xFF4CD2, true).show( Dungeon.hero.sprite, 2f );
			}
		}
	}

	public static void onItemEquipped( Hero hero, Item item ){
		boolean identify = false;
		if (hero.pointsInTalent(VETERANS_INTUITION) == 2 && item instanceof Armor){
			identify = true;
		}
		if (hero.hasTalent(THIEFS_INTUITION) && item instanceof Ring){
			if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
				identify = true;
			}
			((Ring) item).setKnown();
		}
		if (hero.pointsInTalent(ADVENTURERS_INTUITION) == 2 && item instanceof Weapon){
			identify = true;
		}

		if (identify) {
			if (ShardOfOblivion.passiveIDDisabled()) {
				if (item instanceof Weapon){
					((Weapon) item).setIDReady();
				} else if (item instanceof Armor){
					((Armor) item).setIDReady();
				} else if (item instanceof Ring){
					((Ring) item).setIDReady();
				}
			} else {
				item.identify();
			}
		}
	}

	public static void onItemCollected( Hero hero, Item item ){
		if (hero.pointsInTalent(THIEFS_INTUITION) == 2){
			if (item instanceof Ring) ((Ring) item).setKnown();
		}
		//神谕代行者 T1：神谕直觉——捡起神器时直接鉴定（+2 额外祛除诅咒）
		if (hero.hasTalent(ORACULAR_INTUITION) && item instanceof Artifact){
			item.identify();
			if (hero.pointsInTalent(ORACULAR_INTUITION) == 2 && item.cursed){
				item.cursedKnown = true;
				item.cursed = false;
			}
		}
	}

	//神谕代行者 T1：神谕庇佑——完成指令或击杀指令目标时，+1 获得 5 护盾；+2 额外恢复 5 生命
	public static void onOracleBlessing(Hero hero){
		if (hero.hasTalent(ORACULAR_BLESSING)){
			Buff.affect(hero, Barrier.class).setShield(5);
			if (hero.pointsInTalent(ORACULAR_BLESSING) == 2){
				if (hero.heal( 5 ) > 0){
					hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "+5", FloatingText.HEALING);
				}
			}
		}
	}

	//神谕代行者 T3：指令成瘾——激素涌动+易伤。
	//普通/神的宠儿：接取指令时触发（+1 25回合 / +2 50回合；+3 由 Instruction.act 任务期间持续）。
	//命运弃子分支：触发点改为 *哔哔*（待接指令获得时，见 InstructionTimer.trigger），+3 永久获取。
	public static void onNewInstruction(Hero hero){
		if (hero.hasTalent(INSTRUCTION_ADDICTION)){
			if (hero.subClass == HeroSubClass.FATE_FORSAKEN){
				if (hero.pointsInTalent(INSTRUCTION_ADDICTION) == 3){
					Buff.affect(hero, Adrenaline.class, 99999f); //永久
					Buff.affect(hero, Vulnerable.class, 99999f);
				} else {
					int dur = 25 * hero.pointsInTalent(INSTRUCTION_ADDICTION);
					Buff.prolong(hero, Adrenaline.class, dur);
					Buff.prolong(hero, Vulnerable.class, dur);
				}
				return;
			}
			if (hero.pointsInTalent(INSTRUCTION_ADDICTION) < 3){
				int dur = 25 * hero.pointsInTalent(INSTRUCTION_ADDICTION);
				Buff.prolong(hero, Adrenaline.class, dur);
				Buff.prolong(hero, Vulnerable.class, dur);
			}
			//神的宠儿/普通 +3：只要存在尚未完成的指令就持续（由 Instruction.act 每回合刷新）
		}
	}

	//神谕代行者 T2：剑刃解放——完成指令后获得 1 层解放
	public static void onInstructionCompleted(Hero hero){
		if (hero.hasTalent(BLADE_RELEASE)){
			Release release = Buff.affect(hero, Release.class);
			release.gainStack(); //叠层（9 层时由 gainStack 内部触发粒子）
		}
	}

	//指令一餐 +2：随机消除一种可被净化药水祛除的负面效果
	private static void removeRandomNegative(Hero hero){
		@SuppressWarnings("unchecked")
		Class<? extends Buff>[] candidates = new Class[]{
				Poison.class, Cripple.class, Weakness.class, Vulnerable.class,
				Bleeding.class, Blindness.class, Drowsy.class, Slow.class, Vertigo.class
		};
		java.util.ArrayList<Class<? extends Buff>> negatives = new java.util.ArrayList<>();
		for (Class<? extends Buff> c : candidates){
			if (hero.buff(c) != null) negatives.add(c);
		}
		if (!negatives.isEmpty()){
			Buff.detach(hero, Random.element(negatives));
		}
	}

	public static int onAttackProc( Hero hero, Char enemy, int dmg ){

		//神谕代行者 T2：剑刃解放——攻击额外造成 1~X 点伤害
		if (hero.hasTalent(BLADE_RELEASE)){
			Release release = hero.buff(Release.class);
			if (release != null && release.effectiveStacks() > 0){
				dmg += Random.IntRange(1, release.effectiveStacks());
			}
		}

		//神谕代行者 T1：目标打击——攻击指令目标时额外伤害（+2/+3）
		if (hero.hasTalent(TARGETED_STRIKE)
				&& enemy.buff(InstructionTarget.class) != null){
			dmg += 1 + hero.pointsInTalent(TARGETED_STRIKE);
		}

		//拇指 前二老板 T2「狩猎目标」：命中狩猎目标时恢复剑术充能（层数×0.2）
		if (hero.hasTalent(HUNT_TARGET)){
			HuntingTarget ht = enemy.buff( HuntingTarget.class );
			if (ht != null){
				Buff.affect( hero, MeleeWeapon.Charger.class ).gainCharge( 0.2f * ht.stacks );
			}
		}

		if (hero.hasTalent(Talent.PROVOKED_ANGER)
			&& hero.buff(ProvokedAngerTracker.class) != null){
			dmg += 1 + 2*hero.pointsInTalent(Talent.PROVOKED_ANGER);
			hero.buff(ProvokedAngerTracker.class).detach();
		}

		if (hero.hasTalent(Talent.LINGERING_MAGIC)
				&& hero.buff(LingeringMagicTracker.class) != null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.LINGERING_MAGIC) , 2);
			hero.buff(LingeringMagicTracker.class).detach();
		}

		if (hero.hasTalent(Talent.SUCKER_PUNCH)
				&& enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)
				&& enemy.buff(SuckerPunchTracker.class) == null){
			dmg += Random.IntRange(hero.pointsInTalent(Talent.SUCKER_PUNCH) , 2);
			Buff.affect(enemy, SuckerPunchTracker.class);
		}

		if (hero.hasTalent(Talent.FOLLOWUP_STRIKE) && enemy.isAlive() && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				Buff.prolong(hero, FollowupStrikeTracker.class, 5f).object = enemy.id();
			} else if (hero.buff(FollowupStrikeTracker.class) != null
					&& hero.buff(FollowupStrikeTracker.class).object == enemy.id()){
				dmg += 1 + hero.pointsInTalent(FOLLOWUP_STRIKE);
				hero.buff(FollowupStrikeTracker.class).detach();
			}
		}

		if (hero.buff(Talent.SpiritBladesTracker.class) != null
				&& Random.Int(10) < 3*hero.pointsInTalent(Talent.SPIRIT_BLADES)){
			SpiritBow bow = hero.belongings.getItem(SpiritBow.class);
			if (bow != null) dmg = bow.proc( hero, enemy, dmg );
			hero.buff(Talent.SpiritBladesTracker.class).detach();
		}

		if (hero.hasTalent(PATIENT_STRIKE)){
			if (hero.buff(PatientStrikeTracker.class) != null
					&& !(hero.belongings.attackingWeapon() instanceof MissileWeapon)){
				hero.buff(PatientStrikeTracker.class).detach();
				dmg += Random.IntRange(hero.pointsInTalent(Talent.PATIENT_STRIKE), 2);
			}
		}

		if (hero.hasTalent(DEADLY_FOLLOWUP) && enemy.alignment == Char.Alignment.ENEMY) {
			if (hero.belongings.attackingWeapon() instanceof MissileWeapon) {
				if (!(hero.belongings.attackingWeapon() instanceof SpiritBow.SpiritArrow)) {
					Buff.prolong(hero, DeadlyFollowupTracker.class, 5f).object = enemy.id();
				}
			} else if (hero.buff(DeadlyFollowupTracker.class) != null
				&& hero.buff(DeadlyFollowupTracker.class).object == enemy.id()){
			dmg = Math.round(dmg * (1.0f + .1f*hero.pointsInTalent(DEADLY_FOLLOWUP)));
		}
		}

		// 环指大师 T1：解剖——攻击时对目标施加流血（+1=1，+2=2，每次攻击累积）
		if (hero.hasTalent(ANATOMY)){
			Bleeding b = Buff.affect( enemy, Bleeding.class );
			b.extend( hero.pointsInTalent(ANATOMY) );
		}

		// 环指大师 T1：创口——首次攻击目标时施加虚弱（+1=3回合，+2=5回合）
		if (hero.hasTalent(WOUND) && enemy.buff(WoundTracker.class) == null){
			Buff.prolong( enemy, Weakness.class, 1 + 2 * hero.pointsInTalent(WOUND) );
			Buff.affect( enemy, WoundTracker.class );
		}

		// 环指大师 T2：艺术批评 +2——使用 A-/A/A+（4~6级）人体派作品命中时累积 5% 伤害的流血
		if (hero.hasTalent(ART_CRITIQUE) && hero.pointsInTalent(ART_CRITIQUE) >= 2
				&& hero.belongings.attackingWeapon() instanceof BodyArtWeapon){
			BodyArtWeapon w = (BodyArtWeapon) hero.belongings.attackingWeapon();
			if (w.level() >= 4 && w.level() <= 6){
				Bleeding b = Buff.affect( enemy, Bleeding.class );
				b.extend( dmg * 0.05f );
			}
		}

		// 环指大师 T3：人体观剧——命中目标时施加定身（无法移动和攻击），带冷却时间
		// +1: 4回合定身, 200回合冷却；+2: 5回合定身, 150回合冷却；+3: 6回合定身, 100回合冷却
		if (hero.hasTalent(BODY_THEATER) && enemy.alignment == Char.Alignment.ENEMY
				&& hero.buff(BodyTheaterCooldown.class) == null){
			int points = hero.pointsInTalent(BODY_THEATER);
			float duration = 3f + points; //4/5/6
			float cooldown = 250f - 50f * points; //200/150/100
			BodyTheater bt = Buff.affect(enemy, BodyTheater.class, duration);
			if (bt != null){
				bt.maxDuration = duration;
				BodyTheaterCooldown cd = Buff.affect(hero, BodyTheaterCooldown.class, cooldown);
				if (cd != null) cd.maxCooldown = cooldown;
			}
		}

		// 艺术之巅 T3：我们深爱着血与肉——攻击带有流血的敌人时强化手中的武器
		// +1: +1血值；+2: +1血值+1肉值；+3: 额外造成等同于目标流血数额的伤害
		// 目标免疫流血（无法以血肉回应）时，效果改为攻击额外造成 3/5/7 点伤害
		if (hero.hasTalent(LOVE_FLESH_BLOOD)){
			int points = hero.pointsInTalent(LOVE_FLESH_BLOOD);
			Bleeding enemyBleed = enemy.buff(Bleeding.class);
			if (enemyBleed != null){
				if (hero.belongings.attackingWeapon() instanceof BodyArtWeapon){
					BodyArtWeapon w = (BodyArtWeapon) hero.belongings.attackingWeapon();
					w.bloodValue += 1;
					if (points >= 2){
						w.meatValue += 1;
					}
					if (points >= 3){
						dmg += Math.round(enemyBleed.level());
					}
				}
			} else if (enemy.isImmune(Bleeding.class)){
				dmg += 1 + 2 * points; //3/5/7
			}
		}

		// 环指大师 T3：过时的艺术——生命值低于阈值时，攻击额外造成 2 流血
		// +1: <15%；+2: <30%；+3: <45%
		if (hero.hasTalent(OUTDATED_ART)){
			int points = hero.pointsInTalent(OUTDATED_ART);
			float threshold = 0.15f + 0.15f * (points - 1); //0.15/0.30/0.45
			if ((float)hero.HP / hero.HT < threshold){
				Bleeding b = Buff.affect(enemy, Bleeding.class);
				b.extend(2f);
			}
			//确保追踪器存在并已初始化（按当前天赋点数设置阈值）
			OutdatedArtTracker tracker = hero.buff(OutdatedArtTracker.class);
			if (tracker == null){
				tracker = Buff.affect(hero, OutdatedArtTracker.class);
			}
			if (tracker != null){
				tracker.lostHPThreshold = 35 - 5 * points; //30/25/20
			}
		}

	return dmg;
	}

	public static class ProvokedAngerTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 1.43f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class LingeringMagicTracker extends FlavourBuff{
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.WEAPON; }
		public void tintIcon(Image icon) { icon.hardlight(1.43f, 1.43f, 0f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
	}
	public static class SuckerPunchTracker extends Buff{};
	// 环指大师 T1：创口——标记目标已被首次攻击施加虚弱，常驻至目标死亡（参考 SuckerPunchTracker）
	public static class WoundTracker extends Buff{};

	// 环指大师 T3：人体观剧——命中时触发定身，带冷却时间管理
		// 冷却可视化（参考即兴投掷-冷却 ImprovisedProjectileCooldown 的 TIME 图标）
		public static class BodyTheaterCooldown extends FlavourBuff{
			{ type = buffType.POSITIVE; revivePersists = true; }
			public float maxCooldown = 0f;  //本次冷却总时长（200/150/100，用于淡出比例）
			private static final String MAX_COOLDOWN = "max_cooldown";
			@Override
			public void storeInBundle(Bundle bundle) {
				super.storeInBundle(bundle);
				bundle.put(MAX_COOLDOWN, maxCooldown);
			}
			@Override
			public void restoreFromBundle(Bundle bundle) {
				super.restoreFromBundle(bundle);
				maxCooldown = bundle.getFloat(MAX_COOLDOWN);
			}
			public int icon() { return BuffIndicator.TIME; }
			public void tintIcon(Image icon) { icon.hardlight(0.85f, 0.4f, 0.6f); }  //人体观剧同色系（紫红）
			public float iconFadePercent() {
				if (maxCooldown <= 0) return 1f;
				return Math.max(0, 1f - visualcooldown() / maxCooldown);
			}
			@Override
			public String iconTextDisplay() {
				return Integer.toString((int)Math.ceil(visualcooldown()));
			}
		};

	// 环指大师 T3：过时的艺术——按"实际损失 HP"累计灵感的常驻追踪器
	// 显式累计计数：伤害真正扣到英雄 HP 时由 Char.damage → Talent.onHeroHPLost 回调（护盾吸收部分不计），
	// 治疗/护盾/最大生命变化不影响计数。
	public static class OutdatedArtTracker extends Buff{
		{ type = buffType.POSITIVE; revivePersists = true; }
		public int lostHPThreshold = 0;  //每损失多少 HP 获得 1 层灵感（30/25/20）
		public int totalLost = 0;        //累计实际损失 HP
		public int granted = 0;          //已按阈值发放灵感的次数

		@Override
		public boolean act() {
			spend(TICK);
			return true;
		}

		//单次实际损失 HP（已扣护盾吸收），跨过阈值即发放灵感
		public void onHPLost(int amt){
			if (amt <= 0 || !(target instanceof Hero)) return;
			Hero hero = (Hero) target;
			if (!hero.isAlive() || !hero.hasTalent(OUTDATED_ART)) return;
			if (lostHPThreshold <= 0){
				lostHPThreshold = 35 - 5 * hero.pointsInTalent(OUTDATED_ART); //30/25/20
			}
			totalLost += amt;
			int milestones = totalLost / lostHPThreshold;
			if (milestones > granted){
				int gain = milestones - granted;
				granted = milestones;
				Inspiration insp = Buff.affect(hero, Inspiration.class);
				if (insp != null){
					insp.stacks += gain;
				}
			}
		}

		private static final String LOST_THRESHOLD = "lost_hp_threshold";
		private static final String TOTAL_LOST     = "total_lost";
		private static final String GRANTED        = "granted";

		@Override
		public void storeInBundle(Bundle bundle){
			super.storeInBundle(bundle);
			bundle.put(LOST_THRESHOLD, lostHPThreshold);
			bundle.put(TOTAL_LOST, totalLost);
			bundle.put(GRANTED, granted);
		}

		@Override
		public void restoreFromBundle(Bundle bundle){
			super.restoreFromBundle(bundle);
			lostHPThreshold = bundle.getInt(LOST_THRESHOLD);
			totalLost = bundle.getInt(TOTAL_LOST);
			granted = bundle.getInt(GRANTED);
		}
	};

	//由 Char.damage 回调（英雄 HP 被实际扣减后）：过时的艺术按损失 HP 累计灵感
	public static void onHeroHPLost(Hero hero, int hpLost){
		if (hero == null || !hero.hasTalent(OUTDATED_ART)) return;
		OutdatedArtTracker tracker = hero.buff(OutdatedArtTracker.class);
		if (tracker == null){
			tracker = Buff.affect(hero, OutdatedArtTracker.class);
		}
		if (tracker != null){
			tracker.onHPLost(hpLost);
		}
	}
	public static class FollowupStrikeTracker extends FlavourBuff{
		public int object;
		{ type = Buff.buffType.POSITIVE; }
		public int icon() { return BuffIndicator.INVERT_MARK; }
		public void tintIcon(Image icon) { icon.hardlight(0f, 0.75f, 1f); }
		public float iconFadePercent() { return Math.max(0, 1f - (visualcooldown() / 5)); }
		private static final String OBJECT    = "object";
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(OBJECT, object);
		}
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			object = bundle.getInt(OBJECT);
		}
	};

	public static final int MAX_TALENT_TIERS = 4;

	public static void initClassTalents( Hero hero ){
		initClassTalents( hero.heroClass, hero.talents, hero.metamorphedTalents );
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents){
		initClassTalents( cls, talents, new LinkedHashMap<>());
	}

	public static void initClassTalents( HeroClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents, LinkedHashMap<Talent, Talent> replacements ){
		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 1
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HEARTY_MEAL, VETERANS_INTUITION, PROVOKED_ANGER, IRON_WILL);
				break;
			case MAGE:
				Collections.addAll(tierTalents, EMPOWERING_MEAL, SCHOLARS_INTUITION, LINGERING_MAGIC, BACKUP_BARRIER);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, CACHED_RATIONS, THIEFS_INTUITION, SUCKER_PUNCH, PROTECTIVE_SHADOWS);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, NATURES_BOUNTY, SURVIVALISTS_INTUITION, FOLLOWUP_STRIKE, NATURES_AID);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, STRENGTHENING_MEAL, ADVENTURERS_INTUITION, PATIENT_STRIKE, AGGRESSIVE_BARRIER);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, SATIATED_SPELLS, HOLY_INTUITION, SEARING_LIGHT, SHIELD_OF_LIGHT);
				break;
			case ORACLE:
			Collections.addAll(tierTalents, NOTE_IN_MEAL, ORACULAR_INTUITION, TARGETED_STRIKE, ORACULAR_BLESSING);
			break;
		case RING_MASTER:
			Collections.addAll(tierTalents, PROSTHETIC_BODY, LOOKOUT, ANATOMY, WOUND);
			break;
		case VALENCINA:
			Collections.addAll(tierTalents, RESERVED_WINE, SWIFT_FENCING, BURN_SHOCK, GET_LOST);
			break;
		case MIDDLE_FINGER:
			Collections.addAll(tierTalents, FITNESS_MEAL, WATCH_CLOSELY, PROUD_MUSCLES, YOU_CHEATED);
			break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(0).put(talent, 0);
		}
		tierTalents.clear();

		//tier 2
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, IRON_STOMACH, LIQUID_WILLPOWER, RUNIC_TRANSFERENCE, LETHAL_MOMENTUM, IMPROVISED_PROJECTILES);
				break;
			case MAGE:
				Collections.addAll(tierTalents, ENERGIZING_MEAL, INSCRIBED_POWER, WAND_PRESERVATION, ARCANE_VISION, SHIELD_BATTERY);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, MYSTICAL_MEAL, INSCRIBED_STEALTH, WIDE_SEARCH, SILENT_STEPS, ROGUES_FORESIGHT);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, INVIGORATING_MEAL, LIQUID_NATURE, REJUVENATING_STEPS, HEIGHTENED_SENSES, DURABLE_PROJECTILES);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, FOCUSED_MEAL, LIQUID_AGILITY, WEAPON_RECHARGING, LETHAL_HASTE, SWIFT_EQUIP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, ENLIGHTENING_MEAL, RECALL_INSCRIPTION, SUNRAY, DIVINE_SENSE, BLESS);
				break;
			case ORACLE:
			Collections.addAll(tierTalents, INSTRUCTION_MEAL, SCROLLED_INSTRUCTION, BLADE_RELEASE, INSTRUCTION_SENSE, SHIFTING_FATE);
			break;
		case RING_MASTER:
			Collections.addAll(tierTalents, INSPIRED_MEAL, LIQUID_FLESH, MATERIAL_HARVEST, BONE_WEAVING, ART_CRITIQUE);
			break;
		case VALENCINA:
			Collections.addAll(tierTalents, HUNTING_MEAL, LIQUID_ALCOHOL, ACCEL_AMMO, HUNT_TARGET, FUTURE_VISION);
			break;
		case MIDDLE_FINGER:
			Collections.addAll(tierTalents, RECORD_MEAL, TATTOO_ENGRAVING, HANDY_TOY, NEVER_FORGET, TOO_HOT);
			break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(1).put(talent, 0);
		}
		tierTalents.clear();

		//tier 3
		switch (cls){
			case WARRIOR: default:
				Collections.addAll(tierTalents, HOLD_FAST, STRONGMAN);
				break;
			case MAGE:
				Collections.addAll(tierTalents, DESPERATE_POWER, ALLY_WARP);
				break;
			case ROGUE:
				Collections.addAll(tierTalents, ENHANCED_RINGS, LIGHT_CLOAK);
				break;
			case HUNTRESS:
				Collections.addAll(tierTalents, POINT_BLANK, SEER_SHOT);
				break;
			case DUELIST:
				Collections.addAll(tierTalents, PRECISE_ASSAULT, DEADLY_FOLLOWUP);
				break;
			case CLERIC:
				Collections.addAll(tierTalents, CLEANSE, LIGHT_READING);
				break;
			case ORACLE:
				Collections.addAll(tierTalents, INSTRUCTION_ADDICTION, FALSE_FATHER);
				break;
			case RING_MASTER:
				Collections.addAll(tierTalents, BODY_THEATER, OUTDATED_ART);
				break;
			case VALENCINA:
				Collections.addAll(tierTalents, VALENCINA_T3_GENERIC_1, VALENCINA_T3_GENERIC_2);
				break;
			case MIDDLE_FINGER:
				//T3 通用：两个转职分支（忠义巡礼者 / 背叛家人者）都能点，故放在职业天赋层而非子职业层
				Collections.addAll(tierTalents, PERSEVERANCE, BEAST_FURY);
				break;
		}
		for (Talent talent : tierTalents){
			if (replacements.containsKey(talent)){
				talent = replacements.get(talent);
			}
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

		//tier4
		//TBD
	}

	public static void initSubclassTalents( Hero hero ){
		initSubclassTalents( hero.subClass, hero.talents );
	}

	public static void initSubclassTalents( HeroSubClass cls, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (cls == HeroSubClass.NONE) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		ArrayList<Talent> tierTalents = new ArrayList<>();

		//tier 3
		switch (cls){
			case BERSERKER: default:
				Collections.addAll(tierTalents, ENDLESS_RAGE, DEATHLESS_FURY, ENRAGED_CATALYST);
				break;
			case GLADIATOR:
				Collections.addAll(tierTalents, CLEAVE, LETHAL_DEFENSE, ENHANCED_COMBO);
				break;
			case BATTLEMAGE:
				Collections.addAll(tierTalents, EMPOWERED_STRIKE, MYSTICAL_CHARGE, EXCESS_CHARGE);
				break;
			case WARLOCK:
				Collections.addAll(tierTalents, SOUL_EATER, SOUL_SIPHON, NECROMANCERS_MINIONS);
				break;
			case ASSASSIN:
				Collections.addAll(tierTalents, ENHANCED_LETHALITY, ASSASSINS_REACH, BOUNTY_HUNTER);
				break;
			case FREERUNNER:
				Collections.addAll(tierTalents, EVASIVE_ARMOR, PROJECTILE_MOMENTUM, SPEEDY_STEALTH);
				break;
			case SNIPER:
				Collections.addAll(tierTalents, FARSIGHT, SHARED_ENCHANTMENT, SHARED_UPGRADES);
				break;
			case WARDEN:
				Collections.addAll(tierTalents, DURABLE_TIPS, BARKSKIN, SHIELDING_DEW);
				break;
			case CHAMPION:
				Collections.addAll(tierTalents, VARIED_CHARGE, TWIN_UPGRADES, COMBINED_LETHALITY);
				break;
			case MONK:
				Collections.addAll(tierTalents, UNENCUMBERED_SPIRIT, MONASTIC_VIGOR, COMBINED_ENERGY);
				break;
			case PRIEST:
				Collections.addAll(tierTalents, HOLY_LANCE, HALLOWED_GROUND, MNEMONIC_PRAYER);
				break;
			case PALADIN:
				Collections.addAll(tierTalents, LAY_ON_HANDS, AURA_OF_PROTECTION, WALL_OF_LIGHT);
				break;
			case DIVINE_FAVORITE:
				Collections.addAll(tierTalents, FORTUNE_FAVORITE, KARMA_TRANSFER, FREE_WILL);
				break;
			case FATE_FORSAKEN:
			Collections.addAll(tierTalents, FORSAKEN_MARK, KARMA_CAP, RAPID_BUZZ);
			break;
		case GALLERY_MENTOR:
			Collections.addAll(tierTalents, EXHIBIT_MAINTENANCE, GALLERY_TOUCHUP, CROSS_EXHIBITION);
			break;
		case ART_PINNACLE:
			Collections.addAll(tierTalents, LEARN_UNKNOWN, LOVE_FLESH_BLOOD, BONE_TENDON_MELODY);
			break;
		case WAR_HERO:
			Collections.addAll(tierTalents, VALENCINA_T3_WARHERO_1, VALENCINA_T3_WARHERO_2, VALENCINA_T3_WARHERO_3);
			break;
		case FAMILY_SHAME:
			Collections.addAll(tierTalents, VALENCINA_T3_SHAME_1, VALENCINA_T3_SHAME_2, VALENCINA_T3_SHAME_3);
			break;
		case LOYAL_PILGRIM:
			//忠义巡礼者 T3：「加倍清算！」/「如数奉还」/「永志不忘」（三个槽位已满）
			Collections.addAll(tierTalents, DOUBLE_RECKONING, FULL_REPAYMENT, EVERLASTING_GRUDGE);
			break;
		case FAMILY_BETRAYER:
			//背叛家人者 T3：「得心应手」/「融化而死」/「拆开包装」（三个槽位已满）
			Collections.addAll(tierTalents, EFFORTLESS_GRIP, MELT_TO_DEATH, UNWRAP);
			break;
		}
		for (Talent talent : tierTalents){
			talents.get(2).put(talent, 0);
		}
		tierTalents.clear();

	}

	public static void initArmorTalents( Hero hero ){
		initArmorTalents( hero.armorAbility, hero.talents);
	}

	public static void initArmorTalents(ArmorAbility abil, ArrayList<LinkedHashMap<Talent, Integer>> talents ){
		if (abil == null) return;

		while (talents.size() < MAX_TALENT_TIERS){
			talents.add(new LinkedHashMap<>());
		}

		for (Talent t : abil.talents()){
			talents.get(3).put(t, 0);
		}
	}

	private static final String TALENT_TIER = "talents_tier_";

	public static void storeTalentsInBundle( Bundle bundle, Hero hero ){
		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = new Bundle();

			for (Talent talent : tier.keySet()){
				if (tier.get(talent) > 0){
					tierBundle.put(talent.name(), tier.get(talent));
				}
				if (tierBundle.contains(talent.name())){
					tier.put(talent, Math.min(tierBundle.getInt(talent.name()), talent.maxPoints()));
				}
			}
			bundle.put(TALENT_TIER+(i+1), tierBundle);
		}

		Bundle replacementsBundle = new Bundle();
		for (Talent t : hero.metamorphedTalents.keySet()){
			replacementsBundle.put(t.name(), hero.metamorphedTalents.get(t));
		}
		bundle.put("replacements", replacementsBundle);
	}

	private static final HashSet<String> removedTalents = new HashSet<>();
	static{
		//nothing atm
	}

	private static final HashMap<String, String> renamedTalents = new HashMap<>();
	static{
		//nothing atm
	}

	public static void restoreTalentsFromBundle( Bundle bundle, Hero hero ){
		if (bundle.contains("replacements")){
			Bundle replacements = bundle.getBundle("replacements");
			for (String key : replacements.getKeys()){
				String value = replacements.getString(key);
				if (renamedTalents.containsKey(key)) key = renamedTalents.get(key);
				if (renamedTalents.containsKey(value)) value = renamedTalents.get(value);
				if (!removedTalents.contains(key) && !removedTalents.contains(value)){
					try {
						hero.metamorphedTalents.put(Talent.valueOf(key), Talent.valueOf(value));
					} catch (Exception e) {
						ShatteredPixelDungeon.reportException(e);
					}
				}
			}
		}

		if (hero.heroClass != null)     initClassTalents(hero);
		if (hero.subClass != null)      initSubclassTalents(hero);
		if (hero.armorAbility != null)  initArmorTalents(hero);

		for (int i = 0; i < MAX_TALENT_TIERS; i++){
			LinkedHashMap<Talent, Integer> tier = hero.talents.get(i);
			Bundle tierBundle = bundle.contains(TALENT_TIER+(i+1)) ? bundle.getBundle(TALENT_TIER+(i+1)) : null;

			if (tierBundle != null){
				for (String tName : tierBundle.getKeys()){
					int points = tierBundle.getInt(tName);
					if (renamedTalents.containsKey(tName)) tName = renamedTalents.get(tName);
					if (!removedTalents.contains(tName)) {
						try {
							Talent talent = Talent.valueOf(tName);
							if (tier.containsKey(talent)) {
								tier.put(talent, Math.min(points, talent.maxPoints()));
							}
						} catch (Exception e) {
							ShatteredPixelDungeon.reportException(e);
						}
					}
				}
			}
		}
	}

}
