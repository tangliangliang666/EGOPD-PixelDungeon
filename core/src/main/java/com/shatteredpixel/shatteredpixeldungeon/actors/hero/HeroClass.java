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
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.QuickSlot;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTimer;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Karma;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OracleMarkTracker;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ValencinaTracker;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.Trinity;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpectralBlades;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.SpiritHawk;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.ElementalBlast;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WarpBeacon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.GritTeeth;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.InstantExecution;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.NeverForget;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.oracle.HeartFate;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.oracle.FuriosoReplica;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.oracle.PinHaoFan;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster.ClosingTime;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster.Decoy;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster.Corridor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.SmokeBomb;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.AimHeart;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.Disposal;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.IgnominiousHeart;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.HeroicLeap;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Shockwave;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Waterskin;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.InstructionTerminal;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.VelvetPouch;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MaterialBox;
import com.shatteredpixel.shatteredpixeldungeon.items.food.Food;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfMindVision;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfIdentify;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRecharging;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfLullaby;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRage;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTerror;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfMagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Cudgel;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Dagger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FetalDoctor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Gloves;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HermesCaduceus;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.KnuckleDuster;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TrialDissectionKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.NarcissusCrossSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Rapier;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornTwinSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingKnife;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.PoopStoryBook;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.SharpBoneSpike;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ThrowingStone;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.EmptyBottle;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.watabou.utils.DeviceCompat;

public enum HeroClass {

	WARRIOR( HeroSubClass.BERSERKER, HeroSubClass.GLADIATOR ),
	MAGE( HeroSubClass.BATTLEMAGE, HeroSubClass.WARLOCK ),
	ROGUE( HeroSubClass.ASSASSIN, HeroSubClass.FREERUNNER ),
	HUNTRESS( HeroSubClass.SNIPER, HeroSubClass.WARDEN ),
	DUELIST( HeroSubClass.CHAMPION, HeroSubClass.MONK ),
	CLERIC( HeroSubClass.PRIEST, HeroSubClass.PALADIN ),
	//神谕代行者的两个专精：神的宠儿 / 命运弃子
	ORACLE( HeroSubClass.DIVINE_FAVORITE, HeroSubClass.FATE_FORSAKEN ),
	//环指大师：画廊导师（召唤物制作方向）/ 艺术之巅（武器强化方向）
	RING_MASTER( HeroSubClass.GALLERY_MENTOR, HeroSubClass.ART_PINNACLE ),
	//拇指 前二老板：战争英雄 / 家族之耻（2026-09-07 起替代原套用的狂战士/角斗士）
	VALENCINA( HeroSubClass.WAR_HERO, HeroSubClass.FAMILY_SHAME ),
	//中指 长兄（2026-09-13 新增，名称中的「长兄」以删除线显示）：
	//第一个专精「忠义巡礼者」2026-09-16 实现（复仇技艺：怨恨标记 + 五式复仇技能）；
	//第二个专精「背叛家人者」2026-09-16 实现（莱瓦汀方向：不够力量时预支账簿充能换一次力量补足，
	//专精天赋 得心应手 / 融化而死 / 拆开包装）。
	//（其天赋/盔甲技能仍整体套用战士）
	MIDDLE_FINGER( HeroSubClass.LOYAL_PILGRIM, HeroSubClass.FAMILY_BETRAYER );

	private HeroSubClass[] subClasses;

	HeroClass( HeroSubClass...subClasses ) {
		this.subClasses = subClasses;
	}

	public void initHero( Hero hero ) {

		hero.heroClass = this;
		Talent.initClassTalents(hero);

		Item i = new ClothArmor().identify();
		if (!Challenges.isItemBlocked(i)) hero.belongings.armor = (ClothArmor)i;

		i = new Food();
		if (!Challenges.isItemBlocked(i)) i.collect();

		new VelvetPouch().collect();
		Dungeon.LimitedDrops.VELVET_POUCH.drop();

		Waterskin waterskin = new Waterskin();
		waterskin.collect();

		switch (this) {
			case WARRIOR:
				initWarrior( hero );
				break;

			case MAGE:
				initMage( hero );
				break;

			case ROGUE:
				initRogue( hero );
				break;

			case HUNTRESS:
				initHuntress( hero );
				break;

			case DUELIST:
				initDuelist( hero );
				break;

			case CLERIC:
				initCleric( hero );
				break;

			case ORACLE:
				initOracle( hero );
				break;

			case RING_MASTER:
				initRingMaster( hero );
				break;

			case VALENCINA:
				initValencina( hero );
				break;

			case MIDDLE_FINGER:
				initMiddleFinger( hero );
				break;
		}

		//「拆迁办」挑战：初始武器替换为胎儿博士（四阶，力量需求 10、无法卸下，见 FetalDoctor）
		if (Dungeon.isChallenged(Challenges.DEMOLITION_SQUAD)){
			FetalDoctor doc = new FetalDoctor();
			doc.identify();
			hero.belongings.weapon = doc;
			Dungeon.quickslot.setSlot(0, doc);
		}

		//「水仙追迹」挑战：开局把水仙十字圣剑与财富戒指**都放进背包**（不替换初始武器、不动快捷栏）。
		//  · 剑只发**常态**实例：形态由「英雄等级 ↔ 形态」这条唯一规则决定
		//    （NarcissusCrossSwordBase.formFor）。开局英雄 1 级，所以玩家把它装上主手后，
		//    看护 buff（FormKeeper）会在英雄的下一次 act 把它变形成「芒性」。
		//  · 刻意**不**在这里调 activate()：形态看护由「装备」流程挂上（KindOfWeapon.doEquip → activate）。
		//    此刻剑还在背包里，提前挂上会因 held()==null 在下一回合自行回收，纯属空转。
		//  · 刻意**不**占快捷栏：剑在背包里就不是「已装备」，占槽位反会把该职业自己的初始快捷项挤走
		//    （旧写法占掉 0 号槽，连下面「水袋进快捷栏」那段的落点都会被顶到 1 号）。
		//  · 与「拆迁办」互不干扰：那条改的是 belongings.weapon，本段只 collect()，两条同开时各自成立。
		if (Dungeon.isChallenged(Challenges.NARCISSUS_TRACING)){
			new NarcissusCrossSword().identify().collect();

			//财富戒指：同样放背包（是否戴上交给玩家；开局英雄的戒指槽是空的）
			new RingOfWealth().identify().collect();
		}

		if (SPDSettings.quickslotWaterskin()) {
			for (int s = 0; s < QuickSlot.SIZE; s++) {
				if (Dungeon.quickslot.getItem(s) == null) {
					Dungeon.quickslot.setSlot(s, waterskin);
					break;
				}
			}
		}

	}

	public Badges.Badge masteryBadge() {
		switch (this) {
			case WARRIOR:
				return Badges.Badge.MASTERY_WARRIOR;
			case MAGE:
				return Badges.Badge.MASTERY_MAGE;
			case ROGUE:
				return Badges.Badge.MASTERY_ROGUE;
			case HUNTRESS:
				return Badges.Badge.MASTERY_HUNTRESS;
			case DUELIST:
				return Badges.Badge.MASTERY_DUELIST;
			case CLERIC:
				return Badges.Badge.MASTERY_CLERIC;
			case ORACLE:
				return Badges.Badge.MASTERY_WARRIOR; //暂时复用战士徽章，待独立设计
			case RING_MASTER:
				return Badges.Badge.MASTERY_WARRIOR; //占位复用战士徽章，待独立设计
			case VALENCINA:
				return Badges.Badge.MASTERY_WARRIOR; //占位复用战士徽章，待独立设计
			case MIDDLE_FINGER:
				return Badges.Badge.MASTERY_WARRIOR; //占位复用战士徽章，待独立设计
		}
		return null;
	}

	private static void initWarrior( Hero hero ) {
		(hero.belongings.weapon = new WornShortsword()).identify();
		ThrowingStone stones = new ThrowingStone();
		stones.identify().collect();

		Dungeon.quickslot.setSlot(0, stones);

		if (hero.belongings.armor != null){
			hero.belongings.armor.affixSeal(new BrokenSeal());
			Catalog.setSeen(BrokenSeal.class); //as it's not added to the inventory
		}

		new ScrollOfIdentify().identify();
		new PotionOfHealing().identify();
		new ScrollOfRage().identify();
	}

	//环指大师开局（2026-09-04 重构：不再调用 initWarrior，删去战士的破损纹章/飞石等初始物）：
	//初始武器 = 试作-解体刀（1阶人体派作品）；初始投掷武器 = 锋锐骨刺；另有大师指环与素材箱
	private static void initRingMaster( Hero hero ) {

		(hero.belongings.weapon = new TrialDissectionKnife()).identify();
		new MasterRing().identify().collect();
		new MaterialBox().collect();

		//初始投掷武器：锋锐骨刺（基础伤害 2~3，命中施加 1~2 流血）
		SharpBoneSpike spikes = new SharpBoneSpike();
		spikes.identify().collect();
		Dungeon.quickslot.setSlot(0, spikes);

		//2026-09-04：开局鉴定 = 鉴定卷轴 / 治疗药剂(生命药水) / 充能卷轴
		new ScrollOfIdentify().identify();
		new PotionOfHealing().identify();
		new ScrollOfRecharging().identify();
	}

	//拇指 前二老板（2026-09-07 重构初始物：不再套用战士开局）：
	//初始武器 = 磨损对剑；初始神器 = 奥丁之眼（装备于神器槽）；
	//初始投掷武器 = 空酒瓶；初始护甲 = 通用布甲（initHero 发放，不贴破损纹章）
	private static void initValencina( Hero hero ) {

		//初始武器：磨损对剑（一阶快剑）。直接赋值不会触发 activate，
		//这里显式激活以挂上武技充能（Charger），确保开局即可施放「巴勒莫剑术」
		(hero.belongings.weapon = new WornTwinSword()).identify();
		hero.belongings.weapon.activate(hero);

		//初始神器：奥丁之眼（装备于神器槽；activate 会挂上被动 eyeRecharge 供自动充能）
		OdinsEye eye = new OdinsEye();
		(hero.belongings.artifact = eye).identify();
		hero.belongings.artifact.activate(hero);

		//初始投掷武器：空酒瓶（一次性投掷，用完即碎）
		EmptyBottle bottle = new EmptyBottle();
		bottle.identify().collect();

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, eye);
		Dungeon.quickslot.setSlot(2, bottle);

		//开局鉴定：充能卷轴 / 麻痹药剂 / 液火药剂（不再沿用战士三件套）
		new ScrollOfRecharging().identify();
		new PotionOfParalyticGas().identify();
		new PotionOfLiquidFlame().identify();

		//T2「未来视角」每回合感知维护器（预知眼揭示 + 预感）
		Buff.affect(hero, ValencinaTracker.class);
	}

	//中指 长兄开局（2026-09-13 晚：改为专属初始物，不再套用战士）：
	//初始主手武器 = 指虎（一阶快武器）；初始神器 = 复仇账簿（装备于神器槽）；
	//初始副手 = 封印之剑（六阶，力量需求 22，开局力量不足属预期）。
	//战士原有的破旧短剑/投石/破损纹章取消，改为自己的开局鉴定三件套（见方法末）。
	private static void initMiddleFinger( Hero hero ) {

		//初始主手武器：指虎（一阶快武器，攻击延迟 0.5）。直接赋值不会触发 activate，
		//这里显式激活以挂上武技充能（Charger），保证开局即可施放武技
		(hero.belongings.weapon = new KnuckleDuster()).identify();
		hero.belongings.weapon.activate(hero);

		//初始神器：复仇账簿（装备于神器槽；activate 会挂上被动 ledgerRecharge）
		RevengeLedger ledger = new RevengeLedger();
		(hero.belongings.artifact = ledger).identify();
		hero.belongings.artifact.activate(hero);

		//初始副手：封印之剑（六阶，力量需求 22，开局力量不足属预期），开局就直接挂在副手上。
		//它被锁死在双手：无法卸下/丢弃/被顶替，只能用右下角的「切换主副」按钮在主手与副手之间对调，
		//且每落进一次副手都会重新封回封印之剑；在主手时点击快捷栏可逐级解封。
		//直接赋值（同上面主手武器、神器槽的写法），刻意不走 equipSecondary：
		//后者会结算一次装备回合、还会触发「决斗家解锁」徽章，都不适合开局。
		SealedSword sealedSword = new SealedSword();
		sealedSword.identify();
		hero.belongings.secondWep = sealedSword;
		sealedSword.activate(hero);

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, ledger);
		Dungeon.quickslot.setSlot(2, sealedSword);

		//开局鉴定：力量药剂 / 复仇卷轴 / 恐惧卷轴（2026-09-17 定名，对齐职业介绍的第三段）
		new PotionOfStrength().identify();
		new ScrollOfRetribution().identify();
		new ScrollOfTerror().identify();
	}

	//神谕代行者：初始携带指令终端与赫尔墨斯的双蛇杖，开局获得业buff并开始接收指令
	private static void initOracle( Hero hero ) {

		//武器：赫尔墨斯的双蛇杖（装备时获得业buff并触发首个指令）
		(hero.belongings.weapon = new HermesCaduceus()).identify();
		hero.belongings.weapon.activate(hero);

		//指令终端：不占用装备栏位，放入背包即可正常使用
		InstructionTerminal terminal = new InstructionTerminal();
		terminal.identify().collect(hero.belongings.backpack);

		//「业」buff：绑定神谕代行者角色（与武器是否装备无关）
		Buff.affect(hero, Karma.class);

		//初始投掷武器：《是谁嗯嗯在我的头上》×3
		PoopStoryBook book = new PoopStoryBook();
		book.quantity(3).identify().collect(hero.belongings.backpack);

		//指令标记维护器：每回合确保本层有一个随机怪物被标记为指令目标
		Buff.affect(hero, OracleMarkTracker.class);

		//指令计时器：每 150~200 回合触发一次「待接指令」
		Buff.affect(hero, InstructionTimer.class);

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, book);

		//开局鉴定物品
		new ScrollOfMirrorImage().identify();
		new PotionOfInvisibility().identify();
		new ScrollOfTransmutation().identify();
	}

	private static void initMage( Hero hero ) {
		MagesStaff staff;

		staff = new MagesStaff(new WandOfMagicMissile());

		(hero.belongings.weapon = staff).identify();
		hero.belongings.weapon.activate(hero);

		Dungeon.quickslot.setSlot(0, staff);

		new ScrollOfIdentify().identify();
		new ScrollOfUpgrade().identify();
		new PotionOfLiquidFlame().identify();
	}

	private static void initRogue( Hero hero ) {
		(hero.belongings.weapon = new Dagger()).identify();

		CloakOfShadows cloak = new CloakOfShadows();
		(hero.belongings.artifact = cloak).identify();
		hero.belongings.artifact.activate( hero );

		ThrowingKnife knives = new ThrowingKnife();
		knives.identify().collect();

		Dungeon.quickslot.setSlot(0, cloak);
		Dungeon.quickslot.setSlot(1, knives);

		new ScrollOfIdentify().identify();
		new ScrollOfMagicMapping().identify();
		new PotionOfInvisibility().identify();
	}

	private static void initHuntress( Hero hero ) {

		(hero.belongings.weapon = new Gloves()).identify();
		SpiritBow bow = new SpiritBow();
		bow.identify().collect();

		Dungeon.quickslot.setSlot(0, bow);

		new ScrollOfIdentify().identify();
		new PotionOfMindVision().identify();
		new ScrollOfLullaby().identify();
	}

	private static void initDuelist( Hero hero ) {

		(hero.belongings.weapon = new Rapier()).identify();
		hero.belongings.weapon.activate(hero);

		ThrowingSpike spikes = new ThrowingSpike();
		spikes.quantity(2).identify().collect(); //set quantity is 3, but Duelist starts with 2

		Dungeon.quickslot.setSlot(0, hero.belongings.weapon);
		Dungeon.quickslot.setSlot(1, spikes);

		new ScrollOfIdentify().identify();
		new PotionOfStrength().identify();
		new ScrollOfMirrorImage().identify();
	}

	private static void initCleric( Hero hero ) {

		(hero.belongings.weapon = new Cudgel()).identify();
		hero.belongings.weapon.activate(hero);

		HolyTome tome = new HolyTome();
		(hero.belongings.artifact = tome).identify();
		hero.belongings.artifact.activate( hero );

		Dungeon.quickslot.setSlot(0, tome);

		new ScrollOfIdentify().identify();
		new PotionOfPurity().identify();
		new ScrollOfRemoveCurse().identify();
	}

	public String title() {
		return Messages.get(HeroClass.class, name());
	}

	public String desc(){
		return Messages.get(HeroClass.class, name()+"_desc");
	}

	public String shortDesc(){
		return Messages.get(HeroClass.class, name()+"_desc_short");
	}

	public HeroSubClass[] subClasses() {
		return subClasses;
	}

	public ArmorAbility[] armorAbilities(){
		switch (this) {
			case WARRIOR: default:
				return new ArmorAbility[]{new HeroicLeap(), new Shockwave(), new Endure()};
			case MAGE:
				return new ArmorAbility[]{new ElementalBlast(), new WildMagic(), new WarpBeacon()};
			case ROGUE:
				return new ArmorAbility[]{new SmokeBomb(), new DeathMark(), new ShadowClone()};
			case HUNTRESS:
				return new ArmorAbility[]{new SpectralBlades(), new NaturesPower(), new SpiritHawk()};
			case DUELIST:
				return new ArmorAbility[]{new Challenge(), new ElementalStrike(), new Feint()};
			case CLERIC:
				return new ArmorAbility[]{new AscendedForm(), new Trinity(), new PowerOfMany()};
			case ORACLE:
				//三个盔甲技能均已独立设计（心-命运、Furioso-Replica、拼好饭）
				return new ArmorAbility[]{new HeartFate(), new FuriosoReplica(), new PinHaoFan()};
			case RING_MASTER:
				//环指大师三个盔甲技能：闭馆 / 诱饵 / 走廊（2026-09-04 补全）
				return new ArmorAbility[]{new ClosingTime(), new Decoy(), new Corridor()};
			case VALENCINA:
				//拇指 前二老板三个盔甲技能（心-不光彩 / 处置 / 瞄准心脏，2026-09-08 独立设计）
				return new ArmorAbility[]{new IgnominiousHeart(), new Disposal(), new AimHeart()};
			case MIDDLE_FINGER:
				//中指 长兄三个盔甲技能（2026-09-17 全部定名并实装）
				return new ArmorAbility[]{new GritTeeth(), new NeverForget(), new InstantExecution()};
		}
	}

	public String spritesheet() {
		switch (this) {
			case WARRIOR: default:
				return Assets.Sprites.WARRIOR;
			case MAGE:
				return Assets.Sprites.MAGE;
			case ROGUE:
				return Assets.Sprites.ROGUE;
			case HUNTRESS:
				return Assets.Sprites.HUNTRESS;
			case DUELIST:
				return Assets.Sprites.DUELIST;
			case CLERIC:
				return Assets.Sprites.CLERIC;
			case ORACLE:
				return Assets.Sprites.RIEN;
			case RING_MASTER:
				return Assets.Sprites.CALLISTO; //环指大师皮肤（20×24帧）
			case VALENCINA:
				return Assets.Sprites.VALENCINA; //拇指 前二老板（256×128，21×8 帧，与战士同布局）
			case MIDDLE_FINGER:
				return Assets.Sprites.MATTHIAS; //中指 长兄（256×128，21×8 帧，与战士同布局）
		}
	}

	public String splashArt(){
		switch (this) {
			case WARRIOR: default:
				return Assets.Splashes.WARRIOR;
			case MAGE:
				return Assets.Splashes.MAGE;
			case ROGUE:
				return Assets.Splashes.ROGUE;
			case HUNTRESS:
				return Assets.Splashes.HUNTRESS;
			case DUELIST:
				return Assets.Splashes.DUELIST;
			case CLERIC:
				return Assets.Splashes.CLERIC;
			case ORACLE:
				return Assets.Splashes.RIEN;
			case RING_MASTER:
				return Assets.Splashes.CALLISTO; //环指大师立绘
			case VALENCINA:
				return Assets.Splashes.VALENCINA; //拇指 前二老板立绘
			case MIDDLE_FINGER:
				return Assets.Splashes.MATTHIAS; //中指 长兄立绘
		}
	}
	
	public boolean isUnlocked(){
		//always unlock on debug builds
		if (DeviceCompat.isDebug()) return true;

		switch (this){
			case WARRIOR: default:
				return true;
			case MAGE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_MAGE);
			case ROGUE:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_ROGUE);
			case HUNTRESS:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_HUNTRESS);
			case DUELIST:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_DUELIST);
			case CLERIC:
				return Badges.isUnlocked(Badges.Badge.UNLOCK_CLERIC);
			case VALENCINA:
				return true; //拇指 前二老板默认解锁（debug 与 release 均直接可选）
			case MIDDLE_FINGER:
				return true; //中指 长兄默认解锁（debug 与 release 均直接可选）
		}
	}
	
	public String unlockMsg() {
		return shortDesc() + "\n\n" + Messages.get(HeroClass.class, name()+"_unlock");
	}

}
