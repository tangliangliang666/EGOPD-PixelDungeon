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
import com.shatteredpixel.shatteredpixeldungeon.Bones;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.SacrificialFire;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AdrenalineSurge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AccelerationShot;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Afterimage;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.IgnominiousHeartBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PalermoSwordBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Awareness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blessing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Brag;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.CheatGuard;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Combo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FamilyBetrayal;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Foresight;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GritTeethBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeroDisguise;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HoldFast;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HolyBarrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Instruction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PhysicalEmpower;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.DivineBlessing;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeartFateBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Recharging;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Release;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Regeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TempStrength;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TimeStasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.AscendedForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.ElementalStrike;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.huntress.NaturesPower;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerVoice;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BodyForm;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HallowedGround;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWard;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.HolyWeapon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Smite;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mimic;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Monk;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Snake;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.CheckedCell;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.Ankh;
import com.shatteredpixel.shatteredpixeldungeon.items.Dewdrop;
import com.shatteredpixel.shatteredpixeldungeon.items.EquipableItem;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap.Type;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClothArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Stone;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.AlchemistsToolkit;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CapeOfThorns;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.CloakOfShadows;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.EtherealChains;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HolyTome;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.HornOfPlenty;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.SkeletonKey;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.MagicalHolster;
import com.shatteredpixel.shatteredpixeldungeon.items.journal.Guidebook;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.CrystalKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.GoldenKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.IronKey;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.Key;
import com.shatteredpixel.shatteredpixeldungeon.items.keys.WornKey;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.DarkGold;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.Diamond;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfAccuracy;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEvasion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfFuror;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfTenacity;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ThirteenLeafClover;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SilentPrice;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BlackSwan;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Crossbow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Flail;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LingeringScent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LogicStudio;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.NarcissusCrossSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.NarcissusSwordMang;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PalermoSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Quarterstaff;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.RoundShield;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sai;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Scimitar;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SmilingBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WornShortsword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.journal.Document;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.MiningLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.MuseumLevel;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.LevelTransition;
import com.shatteredpixel.shatteredpixeldungeon.levels.rooms.special.WeakFloorRoom;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ShadowCaster;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.AlchemyScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.HeroSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.QuickSlotButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.StatusPane;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndHero;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndResurrect;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTradeItem;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Point;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;

public class Hero extends Char {

	{
		actPriority = HERO_PRIO;
		
		alignment = Alignment.ALLY;
	}
	
	public static final int MAX_LEVEL = 30;

	public static final int STARTING_STR = 10;
	
	private static final float TIME_TO_REST		    = 1f;
	private static final float TIME_TO_SEARCH	    = 2f;
	private static final float HUNGER_FOR_SEARCH	= 6f;
	
	public HeroClass heroClass = HeroClass.ROGUE;
	public HeroSubClass subClass = HeroSubClass.NONE;
	public ArmorAbility armorAbility = null;
	public ArrayList<LinkedHashMap<Talent, Integer>> talents = new ArrayList<>();
	public LinkedHashMap<Talent, Talent> metamorphedTalents = new LinkedHashMap<>();
	
	private int attackSkill = 10;
	private int defenseSkill = 5;

	public boolean ready = false;
	public boolean damageInterrupt = true;
	public HeroAction curAction = null;
	public HeroAction lastAction = null;

	//reference to the enemy the hero is currently in the process of attacking
	private Char attackTarget;
	
	public boolean resting = false;
	
	public Belongings belongings;
	
	public int STR;
	
	public float awareness;
	
	public int lvl = 1;
	public int exp = 0;
	
	public int HTBoost = 0;
	
	private ArrayList<Mob> visibleEnemies;

	//This list is maintained so that some logic checks can be skipped
	// for enemies we know we aren't seeing normally, resulting in better performance
	public ArrayList<Mob> mindVisionEnemies = new ArrayList<>();

	public Hero() {
		super();

		HP = HT = 20;
		STR = STARTING_STR;
		
		belongings = new Belongings( this );
		
		visibleEnemies = new ArrayList<>();
	}
	
	public void updateHT( boolean boostHP ){
		int curHT = HT;
		
		HT = 20 + 5*(lvl-1) + HTBoost;
		float multiplier = RingOfMight.HTMultiplier(this);
		HT = Math.round(multiplier * HT);
		
		if (buff(ElixirOfMight.HTBoost.class) != null){
			HT += buff(ElixirOfMight.HTBoost.class).boost();
		}

		//祝福：最大生命值 +5%
		if (buff(Blessing.class) != null){
			HT = Math.round(HT * buff(Blessing.class).HTMultiplier());
		}
		
		if (boostHP){
			HP += Math.max(HT - curHT, 0);
		}
		HP = Math.min(HP, HT);
	}

	public int STR() {
		int strBonus = 0;

		strBonus += RingOfMight.strengthBonus( this );
		
		AdrenalineSurge buff = buff(AdrenalineSurge.class);
		if (buff != null){
			strBonus += buff.boost();
		}

		//复仇账簿「仇怨」：持续期间力量 +12（buff 由神器激活时挂上，充能归零自动解除）
		if (buff(RevengeLedger.Rancor.class) != null){
			strBonus += RevengeLedger.RANCOR_STR;
		}

		//中指长兄 T1「健身一餐」：进食后获得的临时力量（限时，覆盖式刷新）
		strBonus += TempStrength.amountOf( this );

		//中指长兄 转职「背叛家人者」的分支特色：攻击莱瓦汀系列之前的「预支力量」补足
		//（不在仇怨时自动消耗 2% 账簿充能换来，只维持这一次攻击；见 FamilyBetrayal）
		strBonus += FamilyBetrayal.amountOf( this );

		if (hasTalent(Talent.STRONGMAN)){
			strBonus += (int)Math.floor(STR * (0.03f + 0.05f*pointsInTalent(Talent.STRONGMAN)));
		}

		return STR + strBonus;
	}

	private static final String CLASS       = "class";
	private static final String SUBCLASS    = "subClass";
	private static final String ABILITY     = "armorAbility";

	private static final String ATTACK		= "attackSkill";
	private static final String DEFENSE		= "defenseSkill";
	private static final String STRENGTH	= "STR";
	private static final String LEVEL		= "lvl";
	private static final String EXPERIENCE	= "exp";
	private static final String HTBOOST     = "htboost";
	
	@Override
	public void storeInBundle( Bundle bundle ) {

		super.storeInBundle( bundle );

		bundle.put( CLASS, heroClass );
		bundle.put( SUBCLASS, subClass );
		bundle.put( ABILITY, armorAbility );
		Talent.storeTalentsInBundle( bundle, this );
		
		bundle.put( ATTACK, attackSkill );
		bundle.put( DEFENSE, defenseSkill );
		
		bundle.put( STRENGTH, STR );
		
		bundle.put( LEVEL, lvl );
		bundle.put( EXPERIENCE, exp );
		
		bundle.put( HTBOOST, HTBoost );

		belongings.storeInBundle( bundle );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {

		lvl = bundle.getInt( LEVEL );
		exp = bundle.getInt( EXPERIENCE );

		HTBoost = bundle.getInt(HTBOOST);

		super.restoreFromBundle( bundle );

		heroClass = bundle.getEnum( CLASS, HeroClass.class );
		subClass = bundle.getEnum( SUBCLASS, HeroSubClass.class );
		armorAbility = (ArmorAbility)bundle.get( ABILITY );
		Talent.restoreTalentsFromBundle( bundle, this );
		
		attackSkill = bundle.getInt( ATTACK );
		defenseSkill = bundle.getInt( DEFENSE );
		
		STR = bundle.getInt( STRENGTH );

		belongings.restoreFromBundle( bundle );
	}
	
	public static void preview( GamesInProgress.Info info, Bundle bundle ) {
		info.level = bundle.getInt( LEVEL );
		info.str = bundle.getInt( STRENGTH );
		info.exp = bundle.getInt( EXPERIENCE );
		info.hp = bundle.getInt( Char.TAG_HP );
		info.ht = bundle.getInt( Char.TAG_HT );
		info.shld = bundle.getInt( Char.TAG_SHLD );
		info.heroClass = bundle.getEnum( CLASS, HeroClass.class );
		info.subClass = bundle.getEnum( SUBCLASS, HeroSubClass.class );
		Belongings.preview( info, bundle );
	}

	public boolean hasTalent( Talent talent ){
		return pointsInTalent(talent) > 0;
	}

	public int pointsInTalent( Talent talent ){
		for (LinkedHashMap<Talent, Integer> tier : talents){
			for (Talent f : tier.keySet()){
				if (f == talent) return tier.get(f);
			}
		}
		return 0;
	}

	public void upgradeTalent( Talent talent ){
		for (LinkedHashMap<Talent, Integer> tier : talents){
			for (Talent f : tier.keySet()){
				if (f == talent) tier.put(talent, tier.get(talent)+1);
			}
		}
		Talent.onTalentUpgraded(this, talent);
	}

	public int talentPointsSpent(int tier){
		int total = 0;
		for (int i : talents.get(tier-1).values()){
			total += i;
		}
		return total;
	}

	public int talentPointsAvailable(int tier){
		if (lvl < (Talent.tierLevelThresholds[tier] - 1)
			|| (tier == 3 && subClass == HeroSubClass.NONE)
			|| (tier == 4 && armorAbility == null)) {
			return 0;
		} else if (lvl >= Talent.tierLevelThresholds[tier+1]){
			return Talent.tierLevelThresholds[tier+1] - Talent.tierLevelThresholds[tier] - talentPointsSpent(tier) + bonusTalentPoints(tier);
		} else {
			return 1 + lvl - Talent.tierLevelThresholds[tier] - talentPointsSpent(tier) + bonusTalentPoints(tier);
		}
	}

	public int bonusTalentPoints(int tier){
		if (lvl < (Talent.tierLevelThresholds[tier]-1)
				|| (tier == 3 && subClass == HeroSubClass.NONE)
				|| (tier == 4 && armorAbility == null)) {
			return 0;
		} else if (buff(PotionOfDivineInspiration.DivineInspirationTracker.class) != null
					&& buff(PotionOfDivineInspiration.DivineInspirationTracker.class).isBoosted(tier)) {
			return 2;
		} else {
			return 0;
		}
	}
	
	public String className() {
		return subClass == null || subClass == HeroSubClass.NONE ? heroClass.title() : subClass.title();
	}

	@Override
	public String name(){
		if (buff(HeroDisguise.class) != null) {
			return buff(HeroDisguise.class).getDisguise().title();
		} else {
			return className();
		}
	}

	@Override
	public void hitSound(float pitch) {
		if (!RingOfForce.fightingUnarmed(this)) {
			belongings.attackingWeapon().hitSound(pitch);
		} else if (RingOfForce.getBuffedBonus(this, RingOfForce.Force.class) > 0) {
			//pitch deepens by 2.5% (additive) per point of strength, down to 75%
			super.hitSound( pitch * GameMath.gate( 0.75f, 1.25f - 0.025f*STR(), 1f) );
		} else {
			super.hitSound(pitch * 1.1f);
		}
	}

	@Override
	public boolean blockSound(float pitch) {
		if ( belongings.weapon() != null && belongings.weapon().defenseFactor(this) >= 4 ){
			Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1, pitch);
			return true;
		}
		return super.blockSound(pitch);
	}

	public void live() {
		for (Buff b : buffs()){
			if (!b.revivePersists) b.detach();
		}
		Buff.affect( this, Regeneration.class );
		Buff.affect( this, Hunger.class );

		//剑刃解放（神谕代行者 T2）：解放层数在死亡时清除，但「业」是复活保留的
		//⇒ 复活后按业重新挂上保底解放，否则保底层数无从生效（buff 不再出现）
		Release.onHeroRevive( this );
	}
	
	public int tier() {
		Armor armor = belongings.armor();
		if (armor instanceof ClassArmor){
			return 6;
		} else if (armor != null){
			return armor.tier;
		} else {
			return 0;
		}
	}
	
	public boolean shoot( Char enemy, MissileWeapon wep ) {

		attackTarget = enemy;
		boolean wasEnemy = enemy.alignment == Alignment.ENEMY
				|| (enemy instanceof Mimic && enemy.alignment == Alignment.NEUTRAL);

		//temporarily set the hero's weapon to the missile weapon being used
		//TODO improve this!
		belongings.thrownWeapon = wep;
		boolean hit = attack( enemy );
		Invisibility.dispel();
		belongings.thrownWeapon = null;

		if (hit && subClass == HeroSubClass.GLADIATOR && wasEnemy){
			Buff.affect( this, Combo.class ).hit( enemy );
		}

		if (hit && heroClass == HeroClass.DUELIST && wasEnemy){
			Buff.affect( this, Sai.ComboStrikeTracker.class).addHit();
		}

		attackTarget = null;
		return hit;
	}
	
	@Override
	public boolean attack(Char enemy, float dmgMulti, float dmgBonus, float accMulti) {
		boolean result = super.attack(enemy, dmgMulti, dmgBonus, accMulti);
		if (!(belongings.attackingWeapon() instanceof MissileWeapon)){
			if (buff(Talent.PreciseAssaultTracker.class) != null){
				buff(Talent.PreciseAssaultTracker.class).detach();
			} else if (buff(Talent.LiquidAgilACCTracker.class) != null
						&& buff(Talent.LiquidAgilACCTracker.class).uses <= 0){
				buff(Talent.LiquidAgilACCTracker.class).detach();
			}
		}
		return result;
	}

	@Override
	public int attackSkill( Char target ) {
		KindOfWeapon wep = belongings.attackingWeapon();
		
		float accuracy = 1;
		accuracy *= RingOfAccuracy.accuracyMultiplier( this );
		
		//precise assault and liquid agility
		if (!(wep instanceof MissileWeapon)) {
			if ((hasTalent(Talent.PRECISE_ASSAULT) || hasTalent(Talent.LIQUID_AGILITY))
					//does not trigger on ability attacks
					&& belongings.abilityWeapon != wep && buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) == null){

				//non-duelist benefit for precise assault, can stack with liquid agility
				if (heroClass != HeroClass.DUELIST) {
					//persistent +10%/20%/30% ACC for other heroes
					accuracy *= 1f + 0.1f * pointsInTalent(Talent.PRECISE_ASSAULT);
				}

				if (wep instanceof Flail && buff(Flail.SpinAbilityTracker.class) != null){
					//do nothing, this is not a regular attack so don't consume talent fx
				} else if (wep instanceof Crossbow && buff(Crossbow.ChargedShot.class) != null){
					//do nothing, this is not a regular attack so don't consume talent fx
				} else if (buff(Talent.PreciseAssaultTracker.class) != null) {
					// 2x/5x/inf. ACC for duelist if she just used a weapon ability
					switch (pointsInTalent(Talent.PRECISE_ASSAULT)){
						default: case 1:
							accuracy *= 2; break;
						case 2:
							accuracy *= 5; break;
						case 3:
							accuracy *= Float.POSITIVE_INFINITY; break;
					}
				} else if (buff(Talent.LiquidAgilACCTracker.class) != null){
					// 3x/inf. ACC, depending on talent level
					accuracy *= pointsInTalent(Talent.LIQUID_AGILITY) == 2 ? Float.POSITIVE_INFINITY : 3f;
					Talent.LiquidAgilACCTracker buff = buff(Talent.LiquidAgilACCTracker.class);
					buff.uses--;
				}
				//卷藏指令：阅读卷轴/秘卷后，5回合内下一次近战攻击 5 倍精准（+2 无限精准）
				if (buff(Talent.ScrolledACCTracker.class) != null){
					accuracy *= pointsInTalent(Talent.SCROLLED_INSTRUCTION) == 2 ? Float.POSITIVE_INFINITY : 5f;
					Talent.ScrolledACCTracker scrolled = buff(Talent.ScrolledACCTracker.class);
					scrolled.uses--;
					if (scrolled.uses <= 0) scrolled.detach();
				}
			}
		} else {
			if (buff(Momentum.class) != null && buff(Momentum.class).freerunning()){
				accuracy *= 1f + pointsInTalent(Talent.PROJECTILE_MOMENTUM)/2f;
			}
		}

		if (buff(Scimitar.SwordDance.class) != null){
			accuracy *= 1.50f;
		}

		//指令加护：获得额外 X×3% 精准
		if (buff(DivineBlessing.class) != null){
			accuracy *= buff(DivineBlessing.class).accuracyMultiplier();
		}

		//心-命运：持有期间获得额外 20% 精准
		if (buff(HeartFateBuff.class) != null){
			accuracy *= 1.2f;
		}

		//祝福：持有期间获得额外 5% 精准
		if (buff(Blessing.class) != null){
			accuracy *= buff(Blessing.class).accuracyMultiplier();
		}

		//中指长兄 T1「仔细看好！」：每层「夸耀」+5% 精准（最高 8 层 → +40%）
		Brag brag = buff(Brag.class);
		if (brag != null){
			accuracy *= brag.accuracyMultiplier();
		}
		
		if (!RingOfForce.fightingUnarmed(this)) {
			return Math.max(1, Math.round(attackSkill * accuracy * wep.accuracyFactor( this, target )));
		} else {
			return Math.max(1, Math.round(attackSkill * accuracy));
		}
	}
	
	@Override
	public int defenseSkill( Char enemy ) {

		if (buff(Combo.ParryTracker.class) != null){
			if (canAttack(enemy) && !isCharmedBy(enemy)){
				Buff.affect(this, Combo.RiposteTracker.class).enemy = enemy;
			}
			return INFINITE_EVASION;
		}

		if (buff(RoundShield.GuardTracker.class) != null){
			return INFINITE_EVASION;
		}
		
		float evasion = defenseSkill;
		
		evasion *= RingOfEvasion.evasionMultiplier( this );

		//水仙十字圣剑「芒性」：闪避加成（到达过的最深层数每 1 层 +20%）。
		//武器没有原生的「闪避系数」钩子（KindOfWeapon.defenseFactor 给的是减伤区间、不是闪避），
		//所以这条加成只能挂在这里；手上不是本系列武器时返回 1，等于没变。
		evasion *= NarcissusCrossSwordBase.heldEvasionMultiplier( this );

		if (buff(Talent.LiquidAgilEVATracker.class) != null){
			if (pointsInTalent(Talent.LIQUID_AGILITY) == 1){
				evasion *= 3f;
			} else if (pointsInTalent(Talent.LIQUID_AGILITY) == 2){
				return INFINITE_EVASION;
			}
		}

		if (buff(Quarterstaff.DefensiveStance.class) != null){
			evasion *= 3;
		}

		//卷藏指令：阅读卷轴/秘卷期间 4 倍闪避（+2 无限闪避）
		if (buff(Talent.ScrolledEVATracker.class) != null){
			if (pointsInTalent(Talent.SCROLLED_INSTRUCTION) == 1){
				evasion *= 4f;
			} else {
				return INFINITE_EVASION;
			}
		}

		//变化无常：不再走闪避数值乘区——改为在命中判定里把闪避掷点额外重复 1/2 次取最高值
		//（见 Char.hit 的 evasionRoll 重复段。「比大小」取最高值无法用闪避乘区等价表达，
		// 故这个天赋在本方法里没有分支，是刻意为之而不是漏改）

		//给我滚开（拇指 前二老板 T1）：获得 10%/20% 额外闪避
		if (hasTalent(Talent.GET_LOST)){
			evasion *= 1f + 0.1f * pointsInTalent(Talent.GET_LOST);
		}

		//指令加护：获得额外 X×3% 闪避
		if (buff(DivineBlessing.class) != null){
			evasion *= buff(DivineBlessing.class).evasionMultiplier();
		}

		//心-命运：持有期间获得额外 20% 闪避
		if (buff(HeartFateBuff.class) != null){
			evasion *= 1.2f;
		}

		//祝福：持有期间获得额外 5% 闪避
	if (buff(Blessing.class) != null){
		evasion *= buff(Blessing.class).evasionMultiplier();
	}

	// 环指大师 T1：眺望——等待（resting）时获得额外闪避（+1=50%，+2=100%）
	if (resting && hasTalent(Talent.LOOKOUT)){
		evasion *= 1f + 0.5f * pointsInTalent(Talent.LOOKOUT);
	}

	if (paralysed > 0) {
			evasion /= 2;
		}

		if (belongings.armor() != null) {
			evasion = belongings.armor().evasionFactor(this, evasion);

			//stone specifically overrides to 0 always, guaranteed hit
			if (belongings.armor().hasGlyph(Stone.class, this) && !Stone.testingEvasion()){
				return 0;
			}
		}

		//中指长兄 T1「你犯规了」：无法闪避（命中判定必中）。
		//真实闪避值仍会被 Talent.dodgeAsDamageReduction 取用，在防御结算里换算成减伤。
		//cheatTestingEvasion 打开时不归零，否则换算出的减伤恒为「保底值」。
		if (hasTalent(Talent.YOU_CHEATED) && !Talent.youCheatedTestingEvasion()){
			return 0;
		}

		return Math.max(1, Math.round(evasion));
	}

	@Override
	public String defenseVerb() {
		Combo.ParryTracker parry = buff(Combo.ParryTracker.class);
		if (parry != null){
			parry.parried = true;
			if (buff(Combo.class) == null || buff(Combo.class).getComboCount() < 9 || pointsInTalent(Talent.ENHANCED_COMBO) < 2){
				parry.detach();
			}
			return Messages.get(Monk.class, "parried");
		}

		if (buff(RoundShield.GuardTracker.class) != null){
			buff(RoundShield.GuardTracker.class).hasBlocked = true;
			BuffIndicator.refreshHero();
			Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
			return Messages.get(RoundShield.GuardTracker.class, "guarded");
		}

		if (buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class) != null){
			buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class).detach();
			if (sprite != null && sprite.visible) {
				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1, Random.Float(0.96f, 1.05f));
			}
			return Messages.get(Monk.class, "parried");
		}

		return super.defenseVerb();
	}

	@Override
	public int drRoll() {
		int dr = super.drRoll();

		if (belongings.armor() != null) {
			int armMin = belongings.armor().DRMin();
			int armMax = belongings.armor().DRMax();
			//泪锋的加护（诅咒）：护甲的「最高防御」减半（向下取整）——身上捆着一件死神器的代价。
			//削的是掷值上端，且绝不把下端一并拉低（复用绝望形态削上端的同一套 clampReducedMax：
			//差值不足就取平），所以区间恒合法，只是再也拿不到高减伤。
			//刻意排在下面「抬下限」之前：惩罚作用在护甲自身的上端，抬下限仍按截断语义在其内腾挪。
			if (TearSword.hasCursedArmorPenalty( this )) {
				armMax = TearSword.clampReducedMax( armMin, armMax, (armMax + 1) / 2 );
			}
			//泪锋的加护（加护形态）：护甲的「最低防御」提高 (泪剑层数 + 神器等级)。
			//只抬投掷区间的下端，所以它加的是「保底减伤」；抬后采用「截断」语义（clampRaisedMin）：
			//下端被夹在原有上端之内，护甲上限不受影响；再调一次 clampMaxToMin 只是幂等兜底。
			int minDrBonus = TearSword.armorMinDrBonus( this );
			if (minDrBonus > 0) {
				armMin = TearSword.clampRaisedMin( armMin, armMax, minDrBonus );
				armMax = TearSword.clampMaxToMin( armMin, armMax );
			}
			int armDr = Random.NormalIntRange( armMin, armMax );
			if (STR() < belongings.armor().STRReq()){
				armDr -= 2*(belongings.armor().STRReq() - STR());
			}
			if (armDr > 0) dr += armDr;
		}
		if (belongings.weapon() != null && !RingOfForce.fightingUnarmed(this))  {
			int wepDr = Random.NormalIntRange( 0 , belongings.weapon().defenseFactor( this ) );
			if (STR() < ((Weapon)belongings.weapon()).STRReq()){
				wepDr -= 2*(((Weapon)belongings.weapon()).STRReq() - STR());
			}
			if (wepDr > 0) dr += wepDr;
		}

		if (buff(HoldFast.class) != null){
			dr += buff(HoldFast.class).armorBonus();
		}

		//剑刃解放：受到攻击时额外减少 1~X 点伤害
		if (buff(Release.class) != null){
			dr += Random.IntRange(1, buff(Release.class).effectiveStacks());
		}

		//不移之心：持有心-命运时获得 1~英雄等级/X 的额外护甲值
		if (buff(HeartFateBuff.class) != null){
			dr += buff(HeartFateBuff.class).armorBonus();
		}
		
		return dr;
	}
	
	@Override
	public int damageRoll() {
		KindOfWeapon wep = belongings.attackingWeapon();
		int dmg;

		if (!RingOfForce.fightingUnarmed(this)) {
			dmg = wep.damageRoll( this );

			if (!(wep instanceof MissileWeapon)) dmg += RingOfForce.armedDamageBonus(this);

			//拇指 前二老板 T1「加速剑法」：
			//+1 快武器（攻击延迟<1）的攻击额外造成 1 点伤害；
			//+2 慢武器（攻击延迟>1）的面板数值（伤害）减半（延迟减半在 attackDelay 中处理）
			if (hasTalent(Talent.SWIFT_FENCING)
					&& wep instanceof Weapon
					&& !(wep instanceof MissileWeapon)){
				float wDelay = ((Weapon) wep).delayFactor(this);
				if (wDelay < 1f){
					dmg += 1;
				} else if (wDelay > 1f && pointsInTalent(Talent.SWIFT_FENCING) >= 2){
					dmg = Math.round(dmg / 2f);
				}
			}
		} else {
			dmg = RingOfForce.damageRoll(this);
			if (RingOfForce.unarmedGetsWeaponAugment(this)){
				dmg = ((Weapon)belongings.attackingWeapon()).augment.damageFactor(dmg);
			}
		}

		PhysicalEmpower emp = buff(PhysicalEmpower.class);
		if (emp != null){
			dmg += emp.dmgBoost;
			emp.left--;
			if (emp.left <= 0) {
				emp.detach();
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.75f, 1.2f);
		}

		if (heroClass != HeroClass.DUELIST
				&& hasTalent(Talent.WEAPON_RECHARGING)
				&& (buff(Recharging.class) != null || buff(ArtifactRecharge.class) != null)){
			dmg = Math.round(dmg * 1.025f + (.025f*pointsInTalent(Talent.WEAPON_RECHARGING)));
		}

		if (dmg < 0) dmg = 0;
		return dmg;
	}

	//damage rolls that come from the hero can have their RNG influenced by clover
	public static int heroDamageIntRange(int min, int max ){
		if (Random.Float() < ThirteenLeafClover.alterHeroDamageChance()){
			return ThirteenLeafClover.alterDamageRoll(min, max);
		} else {
			return Random.NormalIntRange(min, max);
		}
	}
	
	@Override
	public float speedRaw() {

		float speed = super.speedRaw();

		speed *= RingOfHaste.speedMultiplier(this);
		
		if (belongings.armor() != null) {
			speed = belongings.armor().speedFactor(this, speed);
		}
		
		Momentum momentum = buff(Momentum.class);
		if (momentum != null){
			((HeroSprite)sprite).sprint( momentum.freerunning() ? 1.5f : 1f );
			speed *= momentum.speedMultiplier();
		} else {
			((HeroSprite)sprite).sprint( 1f );
		}

		NaturesPower.naturesPowerTracker natStrength = buff(NaturesPower.naturesPowerTracker.class);
		if (natStrength != null){
			speed *= (2f + 0.25f*pointsInTalent(Talent.GROWING_POWER));
		}

		//拇指 前二老板 盔甲「心-不光彩」：移动速度提升（基础 20%，狂乱之心每点 +20%，上限 100%）
		if (buff(IgnominiousHeartBuff.class) != null){
			speed *= 1f + buff(IgnominiousHeartBuff.class).speedBonus();
		}

		//注意：考验「HOKMA」的最终夹取不写在本方法里，而在更外层的 Char.speed()。
		//叠加顺序 ＝ 本方法（含原版挑战修正）→ 考验夹取，即考验优先级最高。
		speed = AscensionChallenge.modifyHeroSpeed(speed);
		
		return speed;
		
	}

	@Override
	public boolean canSurpriseAttack(){
		KindOfWeapon w = belongings.attackingWeapon();
		if (!(w instanceof Weapon))             return true;
		if (RingOfForce.fightingUnarmed(this))  return true;
		//「即刻处刑」余威：封印之剑系列暂时无视力量不足，不因此失去偷袭资格
		if (STR() < ((Weapon)w).STRReq()
				&& !SealedSwordBase.ignoresStrengthPenalty( (Weapon)w, this )){
			return false;
		}
		if (w instanceof Flail)                 return false;
		//盲目：无法偷袭（同链枷）
		if (w instanceof Blindness)             return false;

		return super.canSurpriseAttack();
	}

	public boolean canAttack(Char enemy){
		if (enemy == null || pos == enemy.pos || !Actor.chars().contains(enemy)) {
			return false;
		}

		//can always attack adjacent enemies
		if (Dungeon.level.adjacent(pos, enemy.pos)) {
			return true;
		}

		KindOfWeapon wep = Dungeon.hero.belongings.attackingWeapon();

		if (wep != null){
			return wep.canReach(this, enemy.pos);
		} else if (buff(AscendedForm.AscendBuff.class) != null) {
			boolean[] passable = BArray.not(Dungeon.level.solid, null);
			for (Char ch : Actor.chars()) {
				if (ch != this) passable[ch.pos] = false;
			}

			PathFinder.buildDistanceMap(enemy.pos, passable, 3);

			return PathFinder.distance[pos] <= 3;
		} else {
			return false;
		}
	}
	
	/**
	 * 最终攻击延迟。<b>唯一出口，故意 final</b>：考验「HOKMA」的 0.5 下限要求压过一切天赋与 buff，
	 * 所以夹取必须发生在 {@link #attackDelayRaw()} **之外** —— 否则下面那记
	 * 「手起刀落」的 `return 0`（击杀一击不消耗回合）会从夹取里漏出去。
	 */
	public final float attackDelay(){
		return Trials.modifyHeroAttackDelay( attackDelayRaw() );
	}

	/** 攻击延迟的原始计算（各加速天赋 / 武器 / 戒指在此相乘）。子类若要调整请覆写本方法。 */
	protected float attackDelayRaw() {
		if (buff(Talent.LethalMomentumTracker.class) != null){
			buff(Talent.LethalMomentumTracker.class).detach();
			return 0;
		}

		float delay = 1f;

		if (!RingOfForce.fightingUnarmed(this)) {
			
			float wDelay = belongings.attackingWeapon().delayFactor( this );

			//拇指 前二老板 T1「加速剑法」+2：装备攻击延迟>1 的武器时，攻击延迟减半
			if (hasTalent(Talent.SWIFT_FENCING)
					&& pointsInTalent(Talent.SWIFT_FENCING) >= 2
					&& belongings.attackingWeapon() instanceof Weapon
					&& !(belongings.attackingWeapon() instanceof MissileWeapon)
					&& wDelay > 1f){
				wDelay *= 0.5f;
			}

			//拇指 前二老板 T2「加速弹」buff：攻击延迟减半
			if (buff(AccelerationShot.class) != null){
				wDelay *= 0.5f;
			}

			//拇指 前二老板 盔甲「心-不光彩」：攻击延迟降低（基础 20%，狂乱之心每点 +5%，上限 40%）
			if (buff(IgnominiousHeartBuff.class) != null){
				wDelay *= 1f - buff(IgnominiousHeartBuff.class).delayCut();
			}

			//巴勒莫对剑「连斩」：每次攻击 +1 层（上限5），每层降低 10% 攻击延迟、满层减半。
			//层内线性；与上方各来源同为乘法系数，见 PalermoSwordBuff。
			PalermoSwordBuff palermo = buff( PalermoSwordBuff.class );
			if (palermo != null && belongings.attackingWeapon() instanceof PalermoSword){
				wDelay *= PalermoSwordBuff.delayMultiplier( palermo.stacks() );
			}

			return delay * wDelay;
			
		} else {
			//Normally putting furor speed on unarmed attacks would be unnecessary
			//But there's going to be that one guy who gets a furor+force ring combo
			//This is for that one guy, you shall get your fists of fury!
			float speed = RingOfFuror.attackSpeedMultiplier(this);

			//ditto for furor + sword dance!
			if (buff(Scimitar.SwordDance.class) != null){
				speed += 0.6f;
			}

			//and augments + brawler's stance! My goodness, so many options now compared to 2014!
			if (RingOfForce.unarmedGetsWeaponAugment(this)){
				delay = ((Weapon)belongings.weapon).augment.delayFactor(delay);
			}

			return delay/speed;
		}
	}

	@Override
	public void spend( float time ) {
		super.spend(time);
		//沉默的代价："操作"的唯一定义是消耗回合时间的动作（移动/攻击/使用道具/等待均经由本方法），
		//因此任何一次时间消耗都使饰物的空闲自动等待时钟从头计算（打开背包等不计，见 SilentPrice）。
		SilentPrice.resetIdleClock();
		//中指长兄 转职「背叛家人者」：本击的回合成本（含攻击延迟）刚刚结算完，
		//账簿预支的力量补足到这里才摘——攻击延迟由 Weapon.baseDelay() 读 Hero.STR() 算，
		//而它在 Char.attack 之后、本方法之前；提前到 onHeroAttackResolved 摘会漏掉 ×1.2ⁿ 的延迟惩罚。
		//本方法是英雄所有回合成本的唯一出口（平砍/连击/武技/buff 驱动的一击都经此），
		//没挂该 buff 时是空操作。详见 FamilyBetrayal 类注释「生命周期」。
		FamilyBetrayal.consumeAfterAttack( this );
	}

	@Override
	public void spendConstant(float time) {
		super.spendConstant(time);
	}

	public void spendAndNextConstant(float time ) {
		busy();
		spendConstant( time );
		next();
	}

	public void spendAndNext( float time ) {
		busy();
		spend( time );
		next();
	}
	
	@Override
	public boolean act() {
		
		//calls to dungeon.observe will also update hero's local FOV.
		fieldOfView = Dungeon.level.heroFOV;

		if (buff(Endure.EndureTracker.class) != null){
			buff(Endure.EndureTracker.class).endEnduring();
		}
		
		if (!ready) {
			//do a full observe (including fog update) if not resting.
			if (!resting || buff(MindVision.class) != null || buff(Awareness.class) != null) {
				Dungeon.observe();
			} else {
				//otherwise just directly re-calculate FOV
				Dungeon.level.updateFieldOfView(this, fieldOfView);
			}
		}
		
		checkVisibleMobs();
		BuffIndicator.refreshHero();
		BuffIndicator.refreshBoss();
		
		if (paralysed > 0) {
			
			curAction = null;
			
			spendAndNext( TICK );
			return false;
		}
		
		//中指长兄 T1「你犯规了」+2：本回合是否在「执行动作」（curAction != null 即移动/攻击/交互/拾取…）。
		//必须在下面 if/else 之前取快照：各 actXxx() 分支末尾会把 curAction 置回 null，之后再判就不准了。
		//等待（单次等待/长按休息）走的是 curAction == null 的分支，所以等待不会清除护盾。
		final boolean cheatActed = curAction != null;

		boolean actResult;
		if (curAction == null) {
			
			if (resting) {
				spendConstant( TIME_TO_REST );
				next();
			} else {
				ready();
			}

			//if we just loaded into a level and have a search buff, make sure to process them
			if(Actor.now() == 0){
				if (buff(Foresight.class) != null){
					search(false);
				} else if (buff(TalismanOfForesight.Foresight.class) != null){
					buff(TalismanOfForesight.Foresight.class).checkAwareness();
				}
			}
			
			actResult = false;
			
		} else {
			
			resting = false;
			
			ready = false;
			
			if (curAction instanceof HeroAction.Move) {
				actResult = actMove( (HeroAction.Move)curAction );
				
			} else if (curAction instanceof HeroAction.Interact) {
				actResult = actInteract( (HeroAction.Interact)curAction );
				
			} else if (curAction instanceof HeroAction.Buy) {
				actResult = actBuy( (HeroAction.Buy)curAction );
				
			}else if (curAction instanceof HeroAction.PickUp) {
				actResult = actPickUp( (HeroAction.PickUp)curAction );
				
			} else if (curAction instanceof HeroAction.OpenChest) {
				actResult = actOpenChest( (HeroAction.OpenChest)curAction );
				
			} else if (curAction instanceof HeroAction.Unlock) {
				actResult = actUnlock((HeroAction.Unlock) curAction);
				
			} else if (curAction instanceof HeroAction.Mine) {
				actResult = actMine( (HeroAction.Mine)curAction );

			}else if (curAction instanceof HeroAction.LvlTransition) {
				actResult = actTransition( (HeroAction.LvlTransition)curAction );
				
			} else if (curAction instanceof HeroAction.Attack) {
				actResult = actAttack( (HeroAction.Attack)curAction );
				
			} else if (curAction instanceof HeroAction.Alchemy) {
				actResult = actAlchemy( (HeroAction.Alchemy)curAction );
				
			} else {
				actResult = false;
			}
		}
		
		if(hasTalent(Talent.BARKSKIN) && Dungeon.level.map[pos] == Terrain.FURROWED_GRASS){
			Barkskin.conditionallyAppend(this, (lvl*pointsInTalent(Talent.BARKSKIN))/2, 1 );
		}

		//中指长兄 T1「你犯规了」+2：一旦不再等待（移动/攻击/交互等任何动作）就立刻解除护盾。
		//补盾在 Hero.rest()（等待时补一次）；这里只负责解除，<b>不再每回合 refresh</b>——
		//每回合回满会让等待期间变成常驻减伤，那就是「护甲」而不是「护盾」了。
		if (cheatActed && hasTalent(Talent.YOU_CHEATED)){
			CheatGuard.clear( this );
		}

		return actResult;
	}
	
	public void busy() {
		ready = false;
	}
	
	private void ready() {
		if (sprite.looping()) sprite.idle();
		curAction = null;
		damageInterrupt = true;
		waitOrPickup = false;
		ready = true;
		canSelfTrample = true;

		AttackIndicator.updateState();
		
		GameScene.ready();
	}
	
	public void interrupt() {
		if (isAlive() && curAction != null &&
			((curAction instanceof HeroAction.Move && curAction.dst != pos) ||
			(curAction instanceof HeroAction.LvlTransition))) {
			lastAction = curAction;
		}
		curAction = null;
		GameScene.resetKeyHold();
		resting = false;
	}
	
	public void resume() {
		curAction = lastAction;
		lastAction = null;
		damageInterrupt = false;
		next();
	}

	private boolean canSelfTrample = false;
	public boolean canSelfTrample(){
		return canSelfTrample && !rooted && !flying &&
				//standing in high grass
				(Dungeon.level.map[pos] == Terrain.HIGH_GRASS ||
				//standing in furrowed grass and not huntress
				(heroClass != HeroClass.HUNTRESS && Dungeon.level.map[pos] == Terrain.FURROWED_GRASS) ||
				//standing on a plant
				Dungeon.level.plants.get(pos) != null);
	}
	
	private boolean actMove( HeroAction.Move action ) {

		if (getCloser( action.dst )) {
			canSelfTrample = false;
			return true;

		//Hero moves in place if there is grass to trample
		} else if (pos == action.dst && canSelfTrample()){
			canSelfTrample = false;
			Dungeon.level.pressCell(pos);
			spendAndNext( 1 / speed() );
			return false;
		} else {
			ready();
			return false;
		}
	}
	
	private boolean actInteract( HeroAction.Interact action ) {
		
		Char ch = action.ch;

		if (ch.isAlive() && ch.canInteract(this)) {
			
			ready();
			sprite.turnTo( pos, ch.pos );
			return ch.interact(this);
			
		} else {
			
			if (fieldOfView[ch.pos] && getCloser( ch.pos )) {

				return true;

			} else {
				ready();
				return false;
			}
			
		}
	}
	
	private boolean actBuy( HeroAction.Buy action ) {
		int dst = action.dst;
		if (pos == dst) {

			ready();
			
			Heap heap = Dungeon.level.heaps.get( dst );
			if (heap != null && heap.type == Type.FOR_SALE && heap.size() == 1) {
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show( new WndTradeItem( heap ) );
					}
				});
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	private boolean actAlchemy( HeroAction.Alchemy action ) {
		int dst = action.dst;
		if (Dungeon.level.distance(dst, pos) <= 1) {

			ready();
			
			AlchemistsToolkit.kitEnergy kit = buff(AlchemistsToolkit.kitEnergy.class);
			if (kit != null && kit.isCursed()){
				GLog.w( Messages.get(AlchemistsToolkit.class, "cursed"));
				return false;
			}

			AlchemyScene.clearToolkit();
			ShatteredPixelDungeon.switchScene(AlchemyScene.class);
			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	//used to keep track if the wait/pickup action was used
	// so that the hero spends a turn even if the fail to pick up an item
	public boolean waitOrPickup = false;

	private boolean actPickUp( HeroAction.PickUp action ) {
		int dst = action.dst;
		if (pos == dst) {
			
			Heap heap = Dungeon.level.heaps.get( pos );
			if (heap != null) {
				Item item = heap.peek();
				if (item.doPickUp( this )) {
					heap.pickUp();

					if (item instanceof Dewdrop
							|| item instanceof TimekeepersHourglass.sandBag
							|| item instanceof DriedRose.Petal
							|| item instanceof Key
							|| item instanceof Guidebook
							|| (item instanceof MissileWeapon && !MissileWeapon.UpgradedSetTracker.pickupValid(this, (MissileWeapon) item))) {
						//Do Nothing
					} else if (item instanceof DarkGold) {
						DarkGold existing = belongings.getItem(DarkGold.class);
						if (existing != null){
							if (existing.quantity() >= 40) {
								GLog.p(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
							} else {
								GLog.i(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
							}
						}
					} else {

						//TODO make all unique items important? or just POS / SOU?
						boolean important = item.unique && item.isIdentified() &&
								(item instanceof Scroll || item instanceof Potion);
						if (important) {
							GLog.p( Messages.capitalize(Messages.get(this, "you_now_have", item.name())) );
						} else {
							GLog.i( Messages.capitalize(Messages.get(this, "you_now_have", item.name())) );
						}
					}

					//2026-09-09：999 层物品陈列室——拾起任意陈列品即清空其余展品并送回使用前位置
					if (Dungeon.level instanceof MuseumLevel){
						((MuseumLevel) Dungeon.level).onExhibitPickedUp();
					}
					
					curAction = null;
				} else {

					if (waitOrPickup) {
						spendAndNextConstant(TIME_TO_REST);
					}

					//allow the hero to move between levels even if they can't collect the item
					if (Dungeon.level.getTransition(pos) != null){
						throwItems();
					} else {
						heap.sprite.drop();
					}

					if (item instanceof Dewdrop
							|| item instanceof TimekeepersHourglass.sandBag
							|| item instanceof DriedRose.Petal
							|| item instanceof Key) {
						//Do Nothing
					} else {
						GLog.newLine();
						GLog.n(Messages.capitalize(Messages.get(this, "you_cant_have", item.name())));
					}

					ready();
				}
			} else {
				ready();
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actOpenChest( HeroAction.OpenChest action ) {
		int dst = action.dst;
		if (Dungeon.level.adjacent( pos, dst ) || pos == dst) {
			path = null;
			
			Heap heap = Dungeon.level.heaps.get( dst );
			if (heap != null && (heap.type != Type.HEAP && heap.type != Type.FOR_SALE)) {
				
				if ((heap.type == Type.LOCKED_CHEST && Notes.keyCount(new GoldenKey(Dungeon.depth)) < 1)
					|| (heap.type == Type.CRYSTAL_CHEST && Notes.keyCount(new CrystalKey(Dungeon.depth)) < 1)){

						GLog.w( Messages.get(this, "locked_chest") );
						ready();
						return false;

				}
				
				switch (heap.type) {
				case TOMB:
					Sample.INSTANCE.play( Assets.Sounds.TOMB );
					PixelScene.shake( 1, 0.5f );
					break;
				case SKELETON:
				case REMAINS:
					break;
				default:
					Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
				}
				
				sprite.operate( dst );
				
			} else {
				ready();
			}

			return false;

		} else if (getCloser( dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actUnlock( HeroAction.Unlock action ) {
		int doorCell = action.dst;
		if (Dungeon.level.adjacent( pos, doorCell )) {
			path = null;
			
			boolean hasKey = false;
			int door = Dungeon.level.map[doorCell];
			
			if (door == Terrain.LOCKED_DOOR
					&& Notes.keyCount(new IronKey(Dungeon.depth)) > 0) {
				
				hasKey = true;

			} else if (door == Terrain.HERO_LKD_DR){

				if (belongings.getItem(SkeletonKey.class) != null
						&& !belongings.getItem(SkeletonKey.class).cursed){
					GLog.i(Messages.get(SkeletonKey.class, "locked_with_key"));
					ready();
					return false;
				} else {
					hasKey = true;
				}
				
			} else if (door == Terrain.CRYSTAL_DOOR
					&& Notes.keyCount(new CrystalKey(Dungeon.depth)) > 0) {

				hasKey = true;

			} else if (door == Terrain.LOCKED_EXIT
					&& Notes.keyCount(new WornKey(Dungeon.depth)) > 0) {

				hasKey = true;
				
			}
			
			if (hasKey) {
				
				sprite.operate( doorCell );
				
				Sample.INSTANCE.play( Assets.Sounds.UNLOCK );
				
			} else {
				GLog.w( Messages.get(this, "locked_door") );
				ready();
			}

			return false;

		} else if (getCloser( doorCell )) {

			return true;

		} else {
			ready();
			return false;
		}
	}

	private boolean actMine(HeroAction.Mine action){
		if (Dungeon.level.adjacent(pos, action.dst)){
			path = null;
			if ((Dungeon.level.map[action.dst] == Terrain.WALL
					|| Dungeon.level.map[action.dst] == Terrain.WALL_DECO
					|| Dungeon.level.map[action.dst] == Terrain.MINE_CRYSTAL
					|| Dungeon.level.map[action.dst] == Terrain.MINE_BOULDER
					|| Dungeon.level.map[action.dst] == Terrain.MINE_DIAMOND)
				&& Dungeon.level.insideMap(action.dst)){
				sprite.attack(action.dst, new Callback() {
					@Override
					public void call() {

						boolean crystalAdjacent = false;
						for (int i : PathFinder.NEIGHBOURS8) {
							if (Dungeon.level.map[action.dst + i] == Terrain.MINE_CRYSTAL){
								crystalAdjacent = true;
								break;
							}
						}

						//1 hunger spent total
						if (Dungeon.level.map[action.dst] == Terrain.WALL_DECO){
							DarkGold gold = new DarkGold();
							if (gold.doPickUp( Dungeon.hero )) {
								DarkGold existing = Dungeon.hero.belongings.getItem(DarkGold.class);
								if (existing != null && existing.quantity()%5 == 0){
									if (existing.quantity() >= 40) {
										GLog.p(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
									} else {
										GLog.i(Messages.get(DarkGold.class, "you_now_have", existing.quantity()));
									}
								}
								spend(-Actor.TICK); //picking up the gold doesn't spend a turn here
							} else {
								Dungeon.level.drop( gold, pos ).sprite.drop();
							}
							PixelScene.shake(0.5f, 0.5f);
							CellEmitter.center( action.dst ).burst( Speck.factory( Speck.STAR ), 7 );
							Sample.INSTANCE.play( Assets.Sounds.EVOKE );
							Level.set( action.dst, Terrain.EMPTY_DECO );

							//mining gold doesn't break crystals
							crystalAdjacent = false;

						//4 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.WALL){
							buff(Hunger.class).affectHunger(-3);
							PixelScene.shake(0.5f, 0.5f);
							CellEmitter.get( action.dst ).burst( Speck.factory( Speck.ROCK ), 2 );
							Sample.INSTANCE.play( Assets.Sounds.MINE );
							Level.set( action.dst, Terrain.EMPTY_DECO );

						//1 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.MINE_CRYSTAL){
							Splash.at(action.dst, 0xFFFFFF, 5);
							Sample.INSTANCE.play( Assets.Sounds.SHATTER );
							Level.set( action.dst, Terrain.EMPTY );

						//1 hunger spent total
						} else if (Dungeon.level.map[action.dst] == Terrain.MINE_BOULDER){
							Splash.at(action.dst, 0x555555, 5);
							Sample.INSTANCE.play( Assets.Sounds.MINE, 0.6f );
							Level.set( action.dst, Terrain.EMPTY_DECO );

						//钻石矿（2026-09-04 彩蛋）：1 hunger spent total，掉 1 颗钻石
						} else if (Dungeon.level.map[action.dst] == Terrain.MINE_DIAMOND){
							Diamond diamond = new Diamond();
							Dungeon.level.drop( diamond, action.dst ).sprite.drop();
							Splash.at(action.dst, 0x9FE7FF, 5);
							Sample.INSTANCE.play( Assets.Sounds.SHATTER );
							Level.set( action.dst, Terrain.EMPTY_DECO );

							//mining diamond doesn't break crystals
							crystalAdjacent = false;
						}

						for (int i : PathFinder.NEIGHBOURS9) {
							Dungeon.level.discoverable[action.dst + i] = true;
						}
						for (int i : PathFinder.NEIGHBOURS9) {
							GameScene.updateMap( action.dst+i );
						}

						if (crystalAdjacent){
							sprite.parent.add(new Delayer(0.2f){
								@Override
								protected void onComplete() {
									boolean broke = false;
									for (int i : PathFinder.NEIGHBOURS8) {
										if (Dungeon.level.map[action.dst+i] == Terrain.MINE_CRYSTAL){
											Splash.at(action.dst+i, 0xFFFFFF, 5);
											Level.set( action.dst+i, Terrain.EMPTY );
											broke = true;
										}
									}
									if (broke){
										Sample.INSTANCE.play( Assets.Sounds.SHATTER );
									}

									for (int i : PathFinder.NEIGHBOURS9) {
										GameScene.updateMap( action.dst+i );
									}
									spendAndNext(TICK);
									ready();
								}
							});
						} else {
							spendAndNext(TICK);
							ready();
						}

						Dungeon.observe();
					}
				});
			} else {
				ready();
			}
			return false;
		} else if (getCloser( action.dst )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actTransition(HeroAction.LvlTransition action ) {
		int stairs = action.dst;
		LevelTransition transition = Dungeon.level.getTransition(stairs);

		if (rooted) {
			PixelScene.shake(1, 1f);
			ready();
			return false;

		} else if (!Dungeon.level.locked && transition != null && transition.inside(pos)) {

			if (Dungeon.level.activateTransition(this, transition)){
				curAction = null;
			} else {
				ready();
			}

			return false;

		} else if (getCloser( stairs )) {

			return true;

		} else {
			ready();
			return false;
		}
	}
	
	private boolean actAttack( HeroAction.Attack action ) {

		attackTarget = action.target;

		if (isCharmedBy(attackTarget)){
			GLog.w( Messages.get(Charm.class, "cant_attack"));
			ready();
			return false;
		}

		if (attackTarget.isAlive() && canAttack(attackTarget) && attackTarget.invisible == 0) {

			if (heroClass != HeroClass.DUELIST
					&& hasTalent(Talent.AGGRESSIVE_BARRIER)
					&& buff(Talent.AggressiveBarrierCooldown.class) == null
					&& (HP / (float)HT) <= 0.5f){
				int shieldAmt = 1 + 2*pointsInTalent(Talent.AGGRESSIVE_BARRIER);
				Buff.affect(this, Barrier.class).setShield(shieldAmt);
				sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(shieldAmt), FloatingText.SHIELDING);
				Buff.affect(this, Talent.AggressiveBarrierCooldown.class, 50f);

			}

			//漆黑噤默-逻辑工作室：直接攻击（点按敌人）不再走普通近战结算，改为发射投射物射击。
			//其快捷栏点击被"切换"占用，无法从快捷栏发起射击，故在此接管（放在上面天赋结算之后，
			//保证"攻击性护盾"等攻击触发的天赋对射击同样生效）。
			//cast() 内部已负责英雄 busy、投射物飞行与回合结算（spendAndNext），
			//因此清空 attackTarget/curAction，防止本次攻击动作被重复执行。
			KindOfWeapon atkWep = belongings.attackingWeapon();
			if (atkWep instanceof LogicStudio){
				((LogicStudio) atkWep).shootAt(this, attackTarget.pos);
				attackTarget = null;
				curAction = null;
				return false;
			}

			//attack target cleared on onAttackComplete
			sprite.attack( attackTarget.pos );

			return false;

		} else {

			if (fieldOfView[attackTarget.pos] && getCloser( attackTarget.pos )) {

				attackTarget = null;
				return true;

			} else {
				ready();
				attackTarget = null;
				return false;
			}

		}
	}

	public Char attackTarget(){
		return attackTarget;
	}
	
	public void rest( boolean fullRest ) {
		spendAndNextConstant( TIME_TO_REST );
		if (hasTalent(Talent.HOLD_FAST)){
			Buff.affect(this, HoldFast.class).pos = pos;
		}
		if (hasTalent(Talent.PATIENT_STRIKE)){
			Buff.affect(Dungeon.hero, Talent.PatientStrikeTracker.class).pos = Dungeon.hero.pos;
		}
		//中指长兄 T1「你犯规了」+2：等待时按「已失去体力 20%」补一次护盾（最高 5 点，只抬高不叠加）。
		//单次等待（rest(false)）与长按休息（rest(true)）都走这里；护盾是<b>一次性</b>的：
		//补上之后不再每回合回满，被打掉就没了，直到下次等待重新补——否则等待期间等于常驻减伤（像护甲）。
		//解除点在 Hero.act()：本回合执行了动作（移动/攻击/交互…）就清掉。
		if (hasTalent(Talent.YOU_CHEATED) && pointsInTalent(Talent.YOU_CHEATED) >= 2){
			CheatGuard.refresh( this );
		}
		//余香：装备时进行等待，会获得花矢效果
		if (belongings.weapon() instanceof LingeringScent
				|| belongings.secondWep() instanceof LingeringScent){
			Buff.affect(this, LingeringScent.FlowerArrow.class).refresh();
		}
		if (!fullRest) {
			if (sprite != null) {
				sprite.showStatus(CharSprite.DEFAULT, Messages.get(this, "wait"));
			}
		}
		resting = fullRest;
	}
	
	@Override
	public int attackProc( final Char enemy, int damage ) {
		damage = super.attackProc( enemy, damage );

		KindOfWeapon wep;
		if (RingOfForce.fightingUnarmed(this) && !RingOfForce.unarmedGetsWeaponEnchantment(this)){
			wep = null;
		} else {
			wep = belongings.attackingWeapon();
		}

		damage = Talent.onAttackProc( this, enemy, damage );

		if (wep != null) {
			damage = wep.proc( this, enemy, damage );
		} else {
			boolean wasEnemy = enemy.alignment == Alignment.ENEMY;
			if (buff(BodyForm.BodyFormBuff.class) != null
					&& buff(BodyForm.BodyFormBuff.class).enchant() != null){
				damage = buff(BodyForm.BodyFormBuff.class).enchant().proc(new WornShortsword(), this, enemy, damage);
			}
			if (!wasEnemy || enemy.alignment == Alignment.ENEMY) {
				if (buff(HolyWeapon.HolyWepBuff.class) != null) {
					int dmg = subClass == HeroSubClass.PALADIN ? 6 : 2;
					enemy.damage(Math.round(dmg * Weapon.Enchantment.genericProcChanceMultiplier(this)), HolyWeapon.INSTANCE);
				}
				if (buff(Smite.SmiteTracker.class) != null) {
					enemy.damage(Smite.bonusDmg(this, enemy), Smite.INSTANCE);
				}
			}
		}

		switch (subClass) {
		case SNIPER:
			if (wep instanceof MissileWeapon && !(wep instanceof SpiritBow.SpiritArrow) && enemy != this) {
				Actor.add(new Actor() {
					
					{
						actPriority = VFX_PRIO;
					}
					
					@Override
					protected boolean act() {
						if (enemy.isAlive()) {
							if (hasTalent(Talent.SHARED_UPGRADES)){
								int levelBonus = Math.min( 2*pointsInTalent(Talent.SHARED_UPGRADES), wep.buffedLvl() );
								// bonus dmg is 16.67% x weapon level, max of 2/4/6
								float bonusDmg = levelBonus/6f;
								Buff.prolong(Hero.this, SnipersMark.class, SnipersMark.DURATION + levelBonus).set(enemy.id(), bonusDmg);
							} else {
								Buff.prolong(Hero.this, SnipersMark.class, SnipersMark.DURATION).set(enemy.id(), 0);
							}
						}
						Actor.remove(this);
						return true;
					}
				});
			}
			break;
		default:
		}
		
		return damage;
	}
	
	@Override
	public int defenseProc( Char enemy, int damage ) {
		
		if (damage > 0 && subClass == HeroSubClass.BERSERKER){
			Berserk berserk = Buff.affect(this, Berserk.class);
			berserk.damage(damage);
		}
		
		if (belongings.armor() != null) {
			damage = belongings.armor().proc( enemy, this, damage );
		} else {
			if (buff(BodyForm.BodyFormBuff.class) != null
				&& buff(BodyForm.BodyFormBuff.class).glyph() != null){
				damage = buff(BodyForm.BodyFormBuff.class).glyph().proc(new ClothArmor(), enemy, this, damage);
			}
			if (buff(HolyWard.HolyArmBuff.class) != null){
				int blocking = subClass == HeroSubClass.PALADIN ? 3 : 1;
				damage -= Math.round(blocking * Armor.Glyph.genericProcChanceMultiplier(enemy));
			}
		}

		WandOfLivingEarth.RockArmor rockArmor = buff(WandOfLivingEarth.RockArmor.class);
		if (rockArmor != null) {
			damage = rockArmor.absorb(damage);
		}

		//中指长兄 T1「你犯规了」：无法闪避，但把「本可闪避的概率」折算成减伤（照磐岩附魔）。
		//护甲本身带磐岩附魔时那条路已经在 armor().proc() 里算过同一笔，这里跳过以免双重减免。
		if (damage > 0
				&& hasTalent(Talent.YOU_CHEATED)
				&& (belongings.armor() == null || !belongings.armor().hasGlyph(Stone.class, this))){
			damage = Talent.dodgeAsDamageReduction( enemy, this, damage );
		}

		//黑天鹅：持有这把武器时，受到近战攻击有20%概率不受到伤害，并反弹等量伤害
		if (damage > 0 && (belongings.weapon() instanceof BlackSwan
				|| belongings.secondWep() instanceof BlackSwan)){
			if (Random.Int(100) < 20){
				enemy.damage( damage, this );
				return -1; //负值表示本次攻击不造成伤害
			}
		}

		//水仙十字圣剑「芒性」：受到攻击时免受本次伤害，并对视野内所有敌人各反击一次、施加 12 回合致盲。
		//放在这一处（与上面的黑天鹅同一个钩子）：defenseProc 只由 Char.attack 调用，所以只对「被角色攻击」
		//生效，陷阱 / 饥饿 / 持续伤害都不会误触发；返回 -1 表示本次攻击不造成伤害，且跳过后续命中逻辑。
		//「是否处于芒性 / 冷却是否就绪 / 视野内有哪些敌人」全部收在 NarcissusSwordMang.tryCounter 里。
		if (damage > 0 && NarcissusSwordMang.tryCounter(this)) {
			return -1;
		}

		//笑靥·粘稠刻印：视为拥有粘稠刻印（以武器等级进行刻印数据计算），受到攻击时延后部分伤害
		if (belongings.weapon() instanceof SmilingBlade
				&& ((SmilingBlade) belongings.weapon()).hasGrowth(SmilingBlade.GROW_VISCOSITY)){
			Buff.affect(this, Viscosity.ViscosityTracker.class).setLevel(((SmilingBlade) belongings.weapon()).buffedLvl());
		}

		return super.defenseProc( enemy, damage );
	}

	@Override
	public int glyphLevel(Class<? extends Armor.Glyph> cls) {
		if (belongings.armor() != null && belongings.armor().hasGlyph(cls, this)){
			return Math.max(super.glyphLevel(cls), belongings.armor.buffedLvl());
		} else if (buff(BodyForm.BodyFormBuff.class) != null
				&& buff(BodyForm.BodyFormBuff.class).glyph() != null
				&& buff(BodyForm.BodyFormBuff.class).glyph().getClass() == cls){
			return belongings.armor() != null ? belongings.armor.buffedLvl() : 0;
		} else {
			return super.glyphLevel(cls);
		}
	}

	@Override
	public void damage( int dmg, Object src ) {
		if (buff(TimekeepersHourglass.timeStasis.class) != null
				|| buff(TimeStasis.class) != null) {
			return;
		}

		//TODO hero cannot take damage in the vault tester area
		if (Dungeon.depth > 15 && Dungeon.branch == 1){
			dmg = 0;
		}

		//中指长兄 T2「会很烫的！」：血量低于 25%（+1）/ 50%（+2）时，燃烧的伤害完全不生效。
		//直接 return：不给伤害、不闪红、也不触发下方那句 interrupt()——「烧不动」自然不该打断行动，
		//否则残血时会被一整场火卡住。燃烧本身（烫伤层数、烧背包里的卷轴、点燃地面）照旧由
		//Burning.act() 走完，这里只掐掉它落到 HP 上的那一下。
		if (src instanceof Burning && Talent.tooHotImmune( this )) {
			return;
		}

		//神圣卡「神圣屏障」（2026-09-24）：一次性无敌，把这一击整段挡下。
		//放在这里＝「这一下根本没发生」：不掉血、不闪红、不打断行动，也不给复仇账簿 / 血宴圣杯 /
		//静默的代价之类「按实际掉血结算」的东西计账（它们都在下面 super.damage 之后取差值）。
		//挡什么 / 不挡什么（自身 DOT、自伤换成长），以及为什么放在 Hero.damage 而不是 Char.damage，
		//全部收在 HolyBarrier 的类注释与 blocks() 里——这里只留「判一下 + 用掉」两行。
		if (dmg > 0 && HolyBarrier.blocks( this, src )) {
			HolyBarrier.trigger( this );
			return;
		}

		//「残像纠缠」的受击反馈：挨打时残像被朝远离伤害来源的方向甩开、再迅速拉回（纯视觉，不动数值）。
		//放在这里＝伤害还没真正扣，但已排除上面那些「这次根本不生效」的情形；
		//刻意排除 buff 来源（毒/燃烧/流血等 DOT）——否则会每回合都抖一下，看着像在抽搐。
		//身上没有残像时 flinch 直接返回，零开销。
		if (dmg > 0 && !(src instanceof Buff)) {
			Afterimage.flinch( this, src );
		}

		//regular damage interrupt, triggers on any damage except specific mild DOT effects
		// unless the player recently hit 'continue moving', in which case this is ignored
		if (!(src instanceof Hunger || src instanceof Viscosity.DeferedDamage) && damageInterrupt) {
			interrupt();
		}

		if (this.buff(Drowsy.class) != null){
			Buff.detach(this, Drowsy.class);
			GLog.w( Messages.get(this, "pain_resist") );
		}

		//temporarily assign to a float to avoid rounding a bunch
		float damage = dmg;

		//中指长兄 通用 T3「野兽狂怒」：血量低于 50% 时，自己受到的伤害也一并放大
		//（与「造成的伤害」加成同源，倍率不满足条件时返回 1，等于没变）。
		//放在 float 乘区的最前面：后续 endure/thorns/十戒/沉默的代价等修正照常各管一段，
		//乘法可交换，先后不影响结果；只乘一次，不会与下面 Char.damage 里的增伤叠加。
		damage *= Talent.beastFuryMultiplier( this );

		//拇指 前二老板 通用 T3「过往的烟霾」：被敌人攻击/流血伤害放大并获取神器充能回合
		//（在 endure/thorns/十戒等后续修正之前直接放大入参伤害，无递归风险）
		damage = Talent.valencinaSmoke( damage, src, this );

		Endure.EndureTracker endure = buff(Endure.EndureTracker.class);
		if (!(src instanceof Char)){
			//reduce damage here if it isn't coming from a character (if it is we already reduced it)
			if (endure != null){
				damage = endure.adjustDamageTaken(dmg);
			}
			//the same also applies to challenge scroll damage reduction
			if (buff(ScrollOfChallenge.ChallengeArena.class) != null){
				damage *= 0.67f;
			}
			//and to monk meditate damage reduction
			if (buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class) != null){
				damage *= 0.2f;
			}
		}

		//unused, could be removed
		CapeOfThorns.Thorns thorns = buff( CapeOfThorns.Thorns.class );
		if (thorns != null) {
			damage = thorns.proc((int)damage, (src instanceof Char ? (Char)src : null),  this);
		}

		if (buff(Talent.WarriorFoodImmunity.class) != null){
			if (pointsInTalent(Talent.IRON_STOMACH) == 1)       damage /= 4f;
			else if (pointsInTalent(Talent.IRON_STOMACH) == 2)  damage = 0;
		}

		dmg = Math.round(damage);

		//we ceil this one to avoid letting the player easily take 0 dmg from tenacity early
		//（沉默的代价：持有者受伤再乘 (1 - A%) 的免伤，未持有时该函数返回 1）
		dmg = (int)Math.ceil(dmg * RingOfTenacity.damageMultiplier( this ) * SilentPrice.damageTakenMultiplier());

		//中指长兄 通用 T3「过人的毅力」：血量下限保护。
		//① 位置在这里＝dmg 已经定稿（护甲减伤、全部倍率都已结算），但护盾/血量还一点没动，
		//   所以此刻的 shielding() 就是这一下能用的全部护盾；且必须在 super.damage 之前——
		//   血量被打到 0 是在 super.damage 里直接判死的，事后补血已经来不及。
		//② 只减免「打进血量」的那部分：护盾照原样整口吃伤害，下限保护不替护盾挡刀
		//   （见 Talent.perseveranceCap 的 shield 形参说明）。饥饿伤害绕过护盾，按 0 传。
		//③ 覆盖一切来源：攻击、环境、陷阱、坠落、buff 掉血（毒/燃烧/流血）都走同一个 Hero.damage，
		//   这里一刀就全管住；「会很烫的！」那种更早的整段免疫 return 不受影响。
		//④ 泪剑受击计数在下一行、用的是夹取前的 dmg>0——挨打就是挨打，保护住血量不改变「被打中」的事实。
		//⑤ 例外：「自伤换成长」的道具整段豁免（蓄血圣杯的血祭 / 割腕的割腕）。那两笔伤害是升级代价，
		//   升级判据只是「挨完这一下还活着」——被下限保护抹平就等于无条件、无限制地刷等级，
		//   所以按 src 上的 SelfHarmCost 标记接口放行（见 Talent.perseveranceCap 的 src 形参说明），
		//   这也是这里把 src 一并传下去的唯一原因。
		dmg = Talent.perseveranceCap( this, dmg, src instanceof Hunger ? 0 : shielding(), src );

		//泪锋的加护：受击计数（加护形态）。满 3 次减 1 层泪剑，与绝望形态的协同攻击共用同一个计数。
		//① 位置在这里＝最终伤害值已定、但还没进 super.damage 做护甲/护盾结算：
		//   只要这一下是「角色打过来的」就算挨了，哪怕伤害被护甲减到 0、或被护盾整口吃下——
		//   否则攒了盾反而刷不掉剑（与设计相反），满护甲挨打也白挨。
		//② 只认「来自角色的伤害」：否则流血/毒/燃烧这类每回合跳一次的 DOT 会瞬间把泪剑刮光。
		//③ dmg > 0 兜底：免疫/试炼场等把伤害置 0 的路径不计（那些不算真的挨打）；src != this 排除自伤。
		if (dmg > 0 && src instanceof Char && src != this) TearSword.onHitTaken( this );

		//中指长兄的受击台词「你这……！」（15% 概率，判定在 MiddleFingerVoice 里）。
		//条件与上一行的泪剑计数完全一致，但刻意各写一份：
		//这里只认「被角色打中」，流血/毒/燃烧这类每回合跳一次的持续伤害不出声，
		//自伤（src == this）与环境伤害同样不出声——否则站在火里会一路「你这……！」。
		if (dmg > 0 && src instanceof Char && src != this) MiddleFingerVoice.playHitTaken();

		int preHPOnly = HP;
		int preHP = HP + shielding();
		if (src instanceof Hunger) preHP -= shielding();
		super.damage( dmg, src );
		int postHPOnly = HP;
		int postHP = HP + shielding();
		if (src instanceof Hunger) postHP -= shielding();
		int effectiveDamage = preHP - postHP;

		//复仇账簿：按「实际损失的生命值 ÷ 生命上限」的百分比等比例充能。
		//只算真正从 HP 里扣掉的部分（preHPOnly/postHPOnly 不含护盾），所以被护盾整口吃下的伤害不给充能。
		if (postHPOnly < preHPOnly) RevengeLedger.onHeroDamaged( this, preHPOnly - postHPOnly );

		if (effectiveDamage <= 0) return;

		if (buff(Challenge.DuelParticipant.class) != null){
			buff(Challenge.DuelParticipant.class).addDamage(effectiveDamage);
		}

		//flash red when hit for serious damage.
		float percentDMG = effectiveDamage / (float)preHP; //percent of current HP that was taken
		float percentHP = 1 - ((HT - postHP) / (float)HT); //percent health after damage was taken
		// The flash intensity increases primarily based on damage taken and secondarily on missing HP.
		float flashIntensity = 0.25f * (percentDMG * percentDMG) / percentHP;
		//if the intensity is very low don't flash at all
		if (flashIntensity >= 0.05f){
			flashIntensity = Math.min(1/3f, flashIntensity); //cap intensity at 1/3
			GameScene.flash( (int)(0xFF*flashIntensity) << 16 );
			if (isAlive()) {
				if (flashIntensity >= 1/6f) {
					Sample.INSTANCE.play(Assets.Sounds.HEALTH_CRITICAL, 1/3f + flashIntensity * 2f);
				} else {
					Sample.INSTANCE.play(Assets.Sounds.HEALTH_WARN, 1/3f + flashIntensity * 4f);
				}
				//hero gets interrupted on taking serious damage, regardless of any other factor
				interrupt();
				damageInterrupt = true;
			}
		}
	}
	
	public void checkVisibleMobs() {
		ArrayList<Mob> visible = new ArrayList<>();

		boolean newMob = false;

		Mob target = null;
		for (Mob m : Dungeon.level.mobs.toArray(new Mob[0])) {
			if (fieldOfView[ m.pos ] && m.landmark() != null){
				Notes.add(m.landmark());
			}

			if (fieldOfView[ m.pos ] && m.alignment == Alignment.ENEMY) {
				visible.add(m);
				if (!visibleEnemies.contains( m )) {
					newMob = true;
				}

				//only do a simple check for mind visioned enemies, better performance
				if ((!mindVisionEnemies.contains(m) && QuickSlotButton.autoAim(m) != -1)
						|| (mindVisionEnemies.contains(m) && new Ballistica( pos, m.pos, Ballistica.PROJECTILE ).collisionPos == m.pos)) {
					if (target == null) {
						target = m;
					} else if (distance(target) > distance(m)) {
						target = m;
					}
					if (m instanceof Snake && Dungeon.level.distance(m.pos, pos) <= 4
							&& !Document.ADVENTURERS_GUIDE.isPageRead(Document.GUIDE_EXAMINING)){
						GameScene.flashForDocument(Document.ADVENTURERS_GUIDE, Document.GUIDE_EXAMINING);
						//we set to read here to prevent this message popping up a bunch
						Document.ADVENTURERS_GUIDE.readPage(Document.GUIDE_EXAMINING);
					}
				}
			}
		}

		Char lastTarget = QuickSlotButton.lastTarget;
		if (target != null && (lastTarget == null ||
							!lastTarget.isAlive() || !lastTarget.isActive() ||
							lastTarget.alignment == Alignment.ALLY ||
							!fieldOfView[lastTarget.pos])){
			QuickSlotButton.target(target);
		}
		
		if (newMob) {
			if (resting){
				Dungeon.observe();
			}
			interrupt();
		}

		visibleEnemies = visible;

		//we also scan for blob landmarks here
		for (Blob b : Dungeon.level.blobs.values().toArray(new Blob[0])){
			if (b.volume > 0 && b.landmark() != null && !Notes.contains(b.landmark())){
				int cell;
				boolean found = false;
				//if a single cell within the blob is visible, we add the landmark
				for (int i=b.area.top; i < b.area.bottom; i++) {
					for (int j = b.area.left; j < b.area.right; j++) {
						cell = j + i* Dungeon.level.width();
						if (fieldOfView[cell] && b.cur[cell] > 0) {
							Notes.add( b.landmark() );
							found = true;
							break;
						}
					}
					if (found) break;
				}

				//Clear blobs that only exist for landmarks.
				// Might want to make this a properly if it's used more
				if (found && b instanceof WeakFloorRoom.WellID){
					b.fullyClear();
				}
			}
		}
	}
	
	public int visibleEnemies() {
		return visibleEnemies.size();
	}
	
	public Mob visibleEnemy( int index ) {
		return visibleEnemies.get(index % visibleEnemies.size());
	}

	public ArrayList<Mob> getVisibleEnemies(){
		return new ArrayList<>(visibleEnemies);
	}
	
	private boolean walkingToVisibleTrapInFog = false;
	
	private boolean getCloser( final int target ) {

		if (target == pos)
			return false;

		if (rooted) {
			PixelScene.shake( 1, 1f );
			return false;
		}
		
		int step = -1;
		
		if (Dungeon.level.adjacent( pos, target )) {

			path = null;

			if (Actor.findChar( target ) == null) {
				if (Dungeon.level.passable[target] || Dungeon.level.avoid[target]) {
					step = target;
				}
				if (walkingToVisibleTrapInFog
						&& Dungeon.level.traps.get(target) != null
						&& Dungeon.level.traps.get(target).visible
						&& Dungeon.level.traps.get(target).active){
					return false;
				}
			}
			
		} else {

			boolean newPath = false;
			if (path == null || path.isEmpty() || !Dungeon.level.adjacent(pos, path.getFirst()))
				newPath = true;
			else if (path.getLast() != target)
				newPath = true;
			else {
				if (!Dungeon.level.passable[path.get(0)] || Actor.findChar(path.get(0)) != null) {
					newPath = true;
				}
			}

			if (newPath) {

				int len = Dungeon.level.length();
				boolean[] p = Dungeon.level.passable;
				boolean[] v = Dungeon.level.visited;
				boolean[] m = Dungeon.level.mapped;
				boolean[] passable = new boolean[len];
				for (int i = 0; i < len; i++) {
					passable[i] = p[i] && (v[i] || m[i]);
				}

				PathFinder.Path newpath = Dungeon.findPath(this, target, passable, fieldOfView, true);
				if (newpath != null && path != null && newpath.size() > 2*path.size()){
					path = null;
				} else {
					path = newpath;
				}
			}

			if (path == null) return false;
			step = path.removeFirst();

		}

		if (step != -1) {

			float delay = 1;

			if (buff(GreaterHaste.class) != null){
				delay = 0;
			}

			if (Dungeon.level.pit[step] && !Dungeon.level.solid[step]
					&& (!flying || buff(Levitation.class) != null && buff(Levitation.class).detachesWithinDelay(delay / speed()))){
				if (!Chasm.jumpConfirmed){
					Chasm.heroJump(this);
					interrupt();
				} else {
					flying = false;
					remove(buff(Levitation.class)); //directly remove to prevent cell pressing
					Chasm.heroFall(target);
				}
				canSelfTrample = false;
				return false;
			}

			if (buff(GreaterHaste.class) != null){
				buff(GreaterHaste.class).spendMove();
			}

			if (subClass == HeroSubClass.FREERUNNER){
				Buff.affect(this, Momentum.class).gainStack();
			}
			
			sprite.move(pos, step);
			move(step);

			//考验「HOKMA」：这里必须夹「最终值」，不能只依赖 speed() 里的上限 ——
			//上面的 delay 会被天赋「致命迅捷」（GreaterHaste）置 0，而 0/speed 恒为 0，
			//光封顶速度拦不住「免费移动」，所以最终值要在 spend 处再夹一次。
			//（全作其余移动站点分子恒为 1，已由 Char.speed() 的出口统一保证，无需再夹。）
			spend( Trials.modifyHeroMoveDelay( delay / speed() ) );
			
			search(false);

			return true;

		} else {

			return false;
			
		}

	}
	
	public boolean handle( int cell ) {
		
		if (cell == -1) {
			return false;
		}

		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
			Dungeon.level.updateFieldOfView( this, fieldOfView );
		}

		if (!Dungeon.level.visited[cell] && !Dungeon.level.mapped[cell]
				&& Dungeon.level.traps.get(cell) != null
				&& Dungeon.level.traps.get(cell).visible
				&& Dungeon.level.traps.get(cell).active) {
			walkingToVisibleTrapInFog = true;
		} else {
			walkingToVisibleTrapInFog = false;
		}
		
		Char ch = Actor.findChar( cell );
		Heap heap = Dungeon.level.heaps.get( cell );

		if (Dungeon.level.map[cell] == Terrain.ALCHEMY && cell != pos) {
			
			curAction = new HeroAction.Alchemy( cell );
			
		} else if (fieldOfView[cell] && ch instanceof Mob) {

			if (((Mob) ch).heroShouldInteract()) {
				curAction = new HeroAction.Interact( ch );
			} else {
				curAction = new HeroAction.Attack( ch );
			}

		//TODO perhaps only trigger this if hero is already adjacent? reducing mistaps
		} else if (Dungeon.level instanceof MiningLevel &&
					belongings.getItem(Pickaxe.class) != null &&
				(Dungeon.level.map[cell] == Terrain.WALL
						|| Dungeon.level.map[cell] == Terrain.WALL_DECO
						|| Dungeon.level.map[cell] == Terrain.MINE_CRYSTAL
						|| Dungeon.level.map[cell] == Terrain.MINE_BOULDER
						|| Dungeon.level.map[cell] == Terrain.MINE_DIAMOND)){

			curAction = new HeroAction.Mine( cell );

		} else if (heap != null
				//moving to an item doesn't auto-pickup when enemies are near...
				&& (visibleEnemies.size() == 0 || cell == pos ||
				//...but only for standard heaps. Chests and similar open as normal.
				(heap.type != Type.HEAP && heap.type != Type.FOR_SALE))) {

			switch (heap.type) {
			case HEAP:
				curAction = new HeroAction.PickUp( cell );
				break;
			case FOR_SALE:
				curAction = heap.size() == 1 && heap.peek().value() > 0 ?
					new HeroAction.Buy( cell ) :
					new HeroAction.PickUp( cell );
				break;
			default:
				curAction = new HeroAction.OpenChest( cell );
			}
			
		} else if (Dungeon.level.map[cell] == Terrain.LOCKED_DOOR
				|| Dungeon.level.map[cell] == Terrain.HERO_LKD_DR
				|| Dungeon.level.map[cell] == Terrain.CRYSTAL_DOOR
				|| Dungeon.level.map[cell] == Terrain.LOCKED_EXIT) {
			
			curAction = new HeroAction.Unlock( cell );
			
		} else if (Dungeon.level.getTransition(cell) != null
				//moving to a transition doesn't automatically trigger it when enemies are near
				&& (visibleEnemies.size() == 0 || cell == pos)
				&& !Dungeon.level.locked
				&& !Dungeon.level.plants.containsKey(cell)
				&& (Dungeon.depth < 26 || Dungeon.level.getTransition(cell).type == LevelTransition.Type.REGULAR_ENTRANCE) ) {

			curAction = new HeroAction.LvlTransition( cell );
			
		}  else {
			
			curAction = new HeroAction.Move( cell );
			lastAction = null;
			
		}

		return true;
	}
	
	public void earnExp( int exp, Class source ) {

		//水仙十字圣剑「芒性 / 荒性」：装备期间彻底不获得经验值，改为该形态自己的收益
		//（芒性＝等额护盾 + 经验值 ÷ 5 回合的「神器充能 / 充能 / 祝福」；荒性＝等额回合数的祝福）。
		//放在最开头是有意为之：连 AscensionChallenge 的「只给成长效果」那条、以及下方 5 件神器的充能、
		//物品的 onHeroGainExp、3 个天赋计数器一并吞掉 ——「无法再获得经验值」的应有之义。
		if (NarcissusCrossSwordBase.interceptExp(this, exp)) {
			return;
		}

		//xp granted by ascension challenge is only for on-exp gain effects
		if (source != AscensionChallenge.class) {
			this.exp += exp;
		}
		float percent = exp/(float)maxExp();

		EtherealChains.chainsRecharge chains = buff(EtherealChains.chainsRecharge.class);
		if (chains != null) chains.gainExp(percent);

		HornOfPlenty.hornRecharge horn = buff(HornOfPlenty.hornRecharge.class);
		if (horn != null) horn.gainCharge(percent);
		
		AlchemistsToolkit.kitEnergy kit = buff(AlchemistsToolkit.kitEnergy.class);
		if (kit != null) kit.gainCharge(percent);

		MasterThievesArmband.Thievery armband = buff(MasterThievesArmband.Thievery.class);
		if (armband != null) armband.gainCharge(percent);

		Berserk berserk = buff(Berserk.class);
		if (berserk != null) berserk.recover(percent);
		
		if (source != PotionOfExperience.class) {
			for (Item i : belongings) {
				i.onHeroGainExp(percent, this);
			}
			if (buff(Talent.RejuvenatingStepsFurrow.class) != null){
				buff(Talent.RejuvenatingStepsFurrow.class).countDown(percent*200f);
				if (buff(Talent.RejuvenatingStepsFurrow.class).count() <= 0){
					buff(Talent.RejuvenatingStepsFurrow.class).detach();
				}
			}
			if (buff(ElementalStrike.ElementalStrikeFurrowCounter.class) != null){
				buff(ElementalStrike.ElementalStrikeFurrowCounter.class).countDown(percent*20f);
				if (buff(ElementalStrike.ElementalStrikeFurrowCounter.class).count() <= 0){
					buff(ElementalStrike.ElementalStrikeFurrowCounter.class).detach();
				}
			}
			if (buff(HallowedGround.HallowedFurrowTracker.class) != null){
				buff(HallowedGround.HallowedFurrowTracker.class).countDown(percent*100f);
				if (buff(HallowedGround.HallowedFurrowTracker.class).count() <= 0){
					buff(HallowedGround.HallowedFurrowTracker.class).detach();
				}
			}
		}
		
		boolean levelUp = false;
		while (this.exp >= maxExp()) {
			this.exp -= maxExp();

			if (buff(Talent.WandPreservationCounter.class) != null
				&& pointsInTalent(Talent.WAND_PRESERVATION) == 2){
				buff(Talent.WandPreservationCounter.class).detach();
			}

			if (lvl < MAX_LEVEL) {
				lvl++;
				levelUp = true;
				
				if (buff(ElixirOfMight.HTBoost.class) != null){
					buff(ElixirOfMight.HTBoost.class).onLevelUp();
				}
				
				updateHT( true );
				attackSkill++;
				defenseSkill++;

			} else {
				Buff.prolong(this, Bless.class, Bless.DURATION);
				this.exp = 0;

				GLog.newLine();
				GLog.p( Messages.get(this, "level_cap"));
				Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
			}
			
		}
		
		if (levelUp) {
			
			if (sprite != null) {
				GLog.newLine();
				GLog.p( Messages.get(this, "new_level") );
				sprite.showStatus( CharSprite.POSITIVE, Messages.get(Hero.class, "level_up") );
				Sample.INSTANCE.play( Assets.Sounds.LEVELUP );
				if (lvl < Talent.tierLevelThresholds[Talent.MAX_TALENT_TIERS+1]){
					GLog.newLine();
					GLog.p( Messages.get(this, "new_talent") );
					StatusPane.talentBlink = 10f;
					WndHero.lastIdx = 1;
				}
			}
			
			Item.updateQuickslot();
			
			Badges.validateLevelReached();
		}
	}
	
	public int maxExp() {
		return maxExp( lvl );
	}
	
	public static int maxExp( int lvl ){
		return 5 + lvl * 5;
	}
	
	public boolean isStarving() {
		return Buff.affect(this, Hunger.class).isStarving();
	}
	
	@Override
	public boolean add( Buff buff ) {

		if (buff.type == Buff.buffType.NEGATIVE &&
				(buff(TimekeepersHourglass.timeStasis.class) != null || buff(TimeStasis.class) != null)) {
			return false;
		}

		boolean added = super.add( buff );

		if (sprite != null && added) {
			String msg = buff.heroMessage();
			if (msg != null){
				GLog.w(msg);
			}

			if (buff instanceof Paralysis || buff instanceof Vertigo) {
				interrupt();
			}

		}
		
		BuffIndicator.refreshHero();

		return added;
	}
	
	@Override
	public boolean remove( Buff buff ) {
		if (super.remove( buff )) {
			BuffIndicator.refreshHero();
			return true;
		}
		return false;
	}
	
	@Override
	protected synchronized void onRemove() {
		//same as super, except we retain charger for rankings purposes
		for (Buff buff : buffs()) {
			if (buff instanceof MeleeWeapon.Charger){
				Actor.remove(buff);
			} else {
				buff.detach();
			}
		}
	}

	@Override
	public void die( Object cause ) {
		
		curAction = null;

		Ankh ankh = null;

		//look for ankhs in player inventory, prioritize ones which are blessed.
		for (Ankh i : belongings.getAllItems(Ankh.class)){
			if (ankh == null || i.isBlessed()) {
				ankh = i;
			}
		}

		if (ankh != null) {
			interrupt();

			if (ankh.isBlessed()) {
				this.HP = HT / 4;

				PotionOfHealing.cure(this);
				Buff.prolong(this, Invulnerability.class, Invulnerability.DURATION);

				SpellSprite.show(this, SpellSprite.ANKH);
				GameScene.flash(0x80FFFF40);
				Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
				GLog.w(Messages.get(this, "revive"));
				Statistics.ankhsUsed++;
				Catalog.countUse(Ankh.class);

				ankh.detach(belongings.backpack);

				for (Char ch : Actor.chars()) {
					if (ch instanceof DriedRose.GhostHero) {
						((DriedRose.GhostHero) ch).sayAnhk();
						return;
					}
				}
			} else {

				//this is hacky, basically we want to declare that a wndResurrect exists before
				//it actually gets created. This is important so that the game knows to not
				//delete the run or submit it to rankings, because a WndResurrect is about to exist
				//this is needed because the actual creation of the window is delayed here
				WndResurrect.instance = new Object();
				Ankh finalAnkh = ankh;
				Game.runOnRenderThread(new Callback() {
					@Override
					public void call() {
						GameScene.show( new WndResurrect(finalAnkh) );
					}
				});

				if (cause instanceof Hero.Doom) {
					((Hero.Doom)cause).onDeath();
				}

				SacrificialFire.Marked sacMark = buff(SacrificialFire.Marked.class);
				if (sacMark != null){
					sacMark.detach();
				}

			}
			return;
		}
		
		Actor.fixTime();
		super.die( cause );
		reallyDie( cause );
	}
	
	public static void reallyDie( Object cause ) {
		
		int length = Dungeon.level.length();
		int[] map = Dungeon.level.map;
		boolean[] visited = Dungeon.level.visited;
		boolean[] discoverable = Dungeon.level.discoverable;
		
		for (int i=0; i < length; i++) {
			
			int terr = map[i];
			
			if (discoverable[i]) {
				
				visited[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0) {
					Dungeon.level.discover( i );
				}
			}
		}
		
		Bones.leave();
		
		Dungeon.observe();
		GameScene.updateFog();
				
		Dungeon.hero.belongings.identify();

		int pos = Dungeon.hero.pos;

		ArrayList<Integer> passable = new ArrayList<>();
		for (Integer ofs : PathFinder.NEIGHBOURS8) {
			int cell = pos + ofs;
			if ((Dungeon.level.passable[cell] || Dungeon.level.avoid[cell]) && Dungeon.level.heaps.get( cell ) == null) {
				passable.add( cell );
			}
		}
		Collections.shuffle( passable );

		ArrayList<Item> items = new ArrayList<>(Dungeon.hero.belongings.backpack.items);
		for (Integer cell : passable) {
			if (items.isEmpty()) {
				break;
			}

			Item item = Random.element( items );
			Dungeon.level.drop( item, cell ).sprite.drop( pos );
			items.remove( item );
		}

		for (Char c : Actor.chars()){
			if (c instanceof DriedRose.GhostHero){
				((DriedRose.GhostHero) c).sayHeroKilled();
			}
		}

		Game.runOnRenderThread(new Callback() {
			@Override
			public void call() {
				GameScene.gameOver();
				Sample.INSTANCE.play( Assets.Sounds.DEATH );
			}
		});

		if (cause instanceof Hero.Doom) {
			((Hero.Doom)cause).onDeath();
		}

		Dungeon.deleteGame( GamesInProgress.curSlot, true );
	}

	//effectively cache this buff to prevent having to call buff(...) a bunch.
	//This is relevant because we call isAlive during drawing, which has both performance
	//and thread coordination implications if that method calls buff(...) frequently
	private Berserk berserk;

	//中指长兄 盔甲技能「咬紧牙关」：同一个理由缓存一份（0 血免死只在很短的一段时间里成立）
	private GritTeethBuff gritTeeth;

	@Override
	public boolean isAlive() {
		
		if (HP <= 0){

			//「咬紧牙关」：0 血不死。放在狂暴之前判——它是有限时长的盔甲技能状态，
			//到期（或被解离射线击穿）时 GritTeethBuff 会先把自己标成「不再免死」再卸载，
			//所以这里读到的永远是当下的真话，不会因为缓存而卡住不死。
			if (gritTeeth == null) gritTeeth = buff(GritTeethBuff.class);
			if (gritTeeth != null && gritTeeth.undying()) return true;

			if (berserk == null) berserk = buff(Berserk.class);
			return berserk != null && berserk.berserking();
		} else {
			berserk = null;
			gritTeeth = null;
			return super.isAlive();
		}
	}

	@Override
	public void move(int step, boolean travelling) {
		boolean wasHighGrass = Dungeon.level.map[step] == Terrain.HIGH_GRASS;

		super.move( step, travelling);
		
		if (!flying && travelling) {
			if (Dungeon.level.water[pos]) {
				Sample.INSTANCE.play( Assets.Sounds.WATER, 1, Random.Float( 0.8f, 1.25f ) );
			} else if (Dungeon.level.map[pos] == Terrain.EMPTY_SP) {
				Sample.INSTANCE.play( Assets.Sounds.STURDY, 1, Random.Float( 0.96f, 1.05f ) );
			} else if (Dungeon.level.map[pos] == Terrain.GRASS
					|| Dungeon.level.map[pos] == Terrain.EMBERS
					|| Dungeon.level.map[pos] == Terrain.FURROWED_GRASS){
				if (step == pos && wasHighGrass) {
					Sample.INSTANCE.play(Assets.Sounds.TRAMPLE, 1, Random.Float( 0.96f, 1.05f ) );
				} else {
					Sample.INSTANCE.play( Assets.Sounds.GRASS, 1, Random.Float( 0.96f, 1.05f ) );
				}
			} else {
				Sample.INSTANCE.play( Assets.Sounds.STEP, 1, Random.Float( 0.96f, 1.05f ) );
			}
		}
	}
	
	@Override
	public void onAttackComplete() {

		if (attackTarget == null){
			curAction = null;
			super.onAttackComplete();
			return;
		}
		
		AttackIndicator.target(attackTarget);
		boolean wasEnemy = attackTarget.alignment == Alignment.ENEMY
				|| (attackTarget instanceof Mimic && attackTarget.alignment == Alignment.NEUTRAL);

		boolean hit = attack(attackTarget);

		//泪剑「协同攻击」：近战挥击结算之后，全部泪剑依次突刺目标，并追加协同法术伤害
		//（伤害 = 层数 × (神器等级 + 2)，见 TearSword.strikeAt）。
		//必须放在 attack() 之后——泪剑伤害若先把目标打死，attack() 就会在尸体上白跑一遍
		//（命中判定、武器附魔、Palermo/Combo 等 Tracker 全在其中）。
		//目标坐标仍在攻击当帧取样：即使这一击已把目标打死，其贴图也只是原地播放死亡动画，坐标有效。
		TearSword.strikeAt( this, attackTarget );
		
		Invisibility.dispel();
		spend( attackDelay() );

		//巴勒莫对剑「连斩」：每次攻击（挥击完成）后叠加 1 层，不论命中；上限5、持续3回合、刷新不叠加
		if (belongings.attackingWeapon() instanceof PalermoSword){
			PalermoSwordBuff.gain( this );
		}

		if (hit && subClass == HeroSubClass.GLADIATOR && wasEnemy){
			Buff.affect( this, Combo.class ).hit(attackTarget);
		}

		if (hit && heroClass == HeroClass.DUELIST && wasEnemy){
			Buff.affect( this, Sai.ComboStrikeTracker.class).addHit();
		}

		curAction = null;
		attackTarget = null;

		super.onAttackComplete();
	}
	
	@Override
	public void onMotionComplete() {
		GameScene.checkKeyHold();
	}
	
	@Override
	public void onOperateComplete() {
		
		if (curAction instanceof HeroAction.Unlock) {

			int doorCell = ((HeroAction.Unlock)curAction).dst;
			int door = Dungeon.level.map[doorCell];

			SkeletonKey.keyRecharge skele = buff(SkeletonKey.keyRecharge.class);
			SkeletonKey.KeyReplacementTracker keyUseTrack = buff(SkeletonKey.KeyReplacementTracker.class);

			if (skele != null && skele.isCursed() && Random.Int(6) != 0){
				GLog.n(Messages.get(this, "key_distracted"));
				spendAndNext(2*Key.TIME_TO_UNLOCK);
				Buff.affect(this, Hunger.class).affectHunger(-4);
			} else if (Dungeon.level.distance(pos, doorCell) <= 1) {
				boolean hasKey = true;
				if (door == Terrain.LOCKED_DOOR) {
					hasKey = Notes.remove(new IronKey(Dungeon.depth));
					if (hasKey) {
						if (keyUseTrack != null){
							keyUseTrack.processIronLockOpened();
						}
						Level.set(doorCell, Terrain.DOOR);
					}
				} else if (door == Terrain.HERO_LKD_DR) {
					hasKey = true;
					Level.set(doorCell, Terrain.DOOR);
					GLog.i( Messages.get(SkeletonKey.class, "force_lock"));
				} else if (door == Terrain.CRYSTAL_DOOR) {
					hasKey = Notes.remove(new CrystalKey(Dungeon.depth));
					if (hasKey) {
						if (keyUseTrack != null){
							keyUseTrack.processCrystalLockOpened();
						}
						Level.set(doorCell, Terrain.EMPTY);
						Sample.INSTANCE.play(Assets.Sounds.TELEPORT);
						CellEmitter.get( doorCell ).start( Speck.factory( Speck.DISCOVER ), 0.025f, 20 );
					}
				} else {
					hasKey = Notes.remove(new WornKey(Dungeon.depth));
					if (hasKey) {
						Level.set(doorCell, Terrain.UNLOCKED_EXIT);
					}
				}
				
				if (hasKey) {
					GameScene.updateKeyDisplay();
					GameScene.updateMap(doorCell);
					spend(Key.TIME_TO_UNLOCK);
				}
			}
			
		} else if (curAction instanceof HeroAction.OpenChest) {
			
			Heap heap = Dungeon.level.heaps.get( ((HeroAction.OpenChest)curAction).dst );
			SkeletonKey.keyRecharge skele = buff(SkeletonKey.keyRecharge.class);
			SkeletonKey.KeyReplacementTracker keyUseTrack = buff(SkeletonKey.KeyReplacementTracker.class);

			if (skele != null && skele.isCursed()
					&& (heap.type == Type.LOCKED_CHEST || heap.type == Type.CRYSTAL_CHEST)
					&& Random.Int(6) != 0){
				GLog.n(Messages.get(this, "key_distracted"));
				spend(2*Key.TIME_TO_UNLOCK);
				Buff.affect(this, Hunger.class).affectHunger(-4);
			} else if (Dungeon.level.distance(pos, heap.pos) <= 1){
				boolean hasKey = true;
				if (heap.type == Type.SKELETON || heap.type == Type.REMAINS) {
					Sample.INSTANCE.play( Assets.Sounds.BONES );
				} else if (heap.type == Type.LOCKED_CHEST){
					hasKey = Notes.remove(new GoldenKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processGoldLockOpened();
					}
				} else if (heap.type == Type.CRYSTAL_CHEST){
					hasKey = Notes.remove(new CrystalKey(Dungeon.depth));
					if (hasKey && keyUseTrack != null){
						keyUseTrack.processCrystalLockOpened();
					}
				}

				if (hasKey) {
					GameScene.updateKeyDisplay();
					heap.open(this);
					spend(Key.TIME_TO_UNLOCK);
				}
			}
			
		}
		curAction = null;

		if (!ready) {
			super.onOperateComplete();
		}
	}

	public boolean search( boolean intentional ) {
		
		if (!isAlive()) return false;

		//指令任务：难度2「搜索周围环境三次」的计数（仅主动搜索）
		if (intentional){
			Instruction.onSearched(this);
		}
		
		boolean smthFound = false;

		boolean circular = pointsInTalent(Talent.WIDE_SEARCH) == 1;
		int distance = heroClass == HeroClass.ROGUE ? 2 : 1;
		if (hasTalent(Talent.WIDE_SEARCH)) distance++;
		
		boolean foresight = buff(Foresight.class) != null;
		boolean foresightScan = foresight && !Dungeon.level.mapped[pos];

		if (foresightScan){
			Dungeon.level.mapped[pos] = true;
		}

		if (foresight) {
			distance = Foresight.DISTANCE;
			circular = true;
		}

		Point c = Dungeon.level.cellToPoint(pos);

		TalismanOfForesight.Foresight talisman = buff( TalismanOfForesight.Foresight.class );
		boolean cursed = talisman != null && talisman.isCursed();

		int[] rounding = ShadowCaster.rounding[distance];

		int left, right;
		int curr;
		for (int y = Math.max(0, c.y - distance); y <= Math.min(Dungeon.level.height()-1, c.y + distance); y++) {
			if (!circular){
				left = c.x - distance;
			} else if (rounding[Math.abs(c.y - y)] < Math.abs(c.y - y)) {
				left = c.x - rounding[Math.abs(c.y - y)];
			} else {
				left = distance;
				while (rounding[left] < rounding[Math.abs(c.y - y)]){
					left--;
				}
				left = c.x - left;
			}
			right = Math.min(Dungeon.level.width()-1, c.x + c.x - left);
			left = Math.max(0, left);
			for (curr = left + y * Dungeon.level.width(); curr <= right + y * Dungeon.level.width(); curr++){

				if ((foresight || fieldOfView[curr]) && curr != pos) {

					if ((foresight && (!Dungeon.level.mapped[curr] || foresightScan))){
						GameScene.effectOverFog(new CheckedCell(curr, foresightScan ? pos : curr));
					} else if (intentional) {
						GameScene.effectOverFog(new CheckedCell(curr, pos));
					}

					if (foresight){
						Dungeon.level.mapped[curr] = true;
					}
					
					if (Dungeon.level.secret[curr]){
						
						Trap trap = Dungeon.level.traps.get( curr );
						float chance;

						//searches aided by foresight always succeed, even if trap isn't searchable
						if (foresight){
							chance = 1f;

						//otherwise if the trap isn't searchable, searching always fails
						} else if (trap != null && !trap.canBeSearched){
							chance = 0f;

						//intentional searches always succeed against regular traps and doors
						} else if (intentional){
							chance = 1f;
						
						//unintentional searches always fail with a cursed talisman
						} else if (cursed) {
							chance = 0f;
							
						//unintentional trap detection scales from 40% at floor 0 to 30% at floor 25
						} else if (Dungeon.level.map[curr] == Terrain.SECRET_TRAP) {
							chance = 0.4f - (Dungeon.depth / 250f);
							
						//unintentional door detection scales from 20% at floor 0 to 0% at floor 20
						} else {
							chance = 0.2f - (Dungeon.depth / 100f);
						}

						//don't want to let the player search though hidden doors in tutorial
						if (SPDSettings.intro()){
							chance = 0;
						}
						
						if (Random.Float() < chance) {
						
							int oldValue = Dungeon.level.map[curr];
							
							GameScene.discoverTile( curr, oldValue );
							
							Dungeon.level.discover( curr );
							
							ScrollOfMagicMapping.discover( curr );
							
							if (fieldOfView[curr]) smthFound = true;
	
							if (talisman != null){
								if (oldValue == Terrain.SECRET_TRAP){
									talisman.charge(2);
								} else if (oldValue == Terrain.SECRET_DOOR){
									talisman.charge(10);
								}
							}
						}
					}
				}
			}
		}
		
		if (intentional) {
			sprite.showStatus( CharSprite.DEFAULT, Messages.get(this, "search") );
			sprite.operate( pos );
			if (!Dungeon.level.locked) {
				if (cursed) {
					GLog.n(Messages.get(this, "search_distracted"));
					Buff.affect(this, Hunger.class).affectHunger(TIME_TO_SEARCH - (2 * HUNGER_FOR_SEARCH));
				} else {
					Buff.affect(this, Hunger.class).affectHunger(TIME_TO_SEARCH - HUNGER_FOR_SEARCH);
				}
			}
			spendAndNext(TIME_TO_SEARCH);
			
		}
		
		if (smthFound) {
			GLog.w( Messages.get(this, "noticed_smth") );
			Sample.INSTANCE.play( Assets.Sounds.SECRET );
			interrupt();
		}

		if (foresight){
			GameScene.updateFog(pos, Foresight.DISTANCE+1);
		}

		if (talisman != null){
			talisman.checkAwareness();
		}
		
		return smthFound;
	}
	
	public void resurrect() {
		HP = HT;
		live();

		MagicalHolster holster = belongings.getItem(MagicalHolster.class);

		Buff.affect(this, LostInventory.class);
		Buff.affect(this, Invisibility.class, 3f);
		//lost inventory is dropped in interlevelscene

		//activate items that persist after lost inventory
		//FIXME this is very messy, maybe it would be better to just have one buff that
		// handled all items that recharge over time?
		for (Item i : belongings){
			if (i instanceof EquipableItem && i.isEquipped(this)){
				((EquipableItem) i).activate(this);
			} else if (i instanceof CloakOfShadows && i.keptThroughLostInventory() && hasTalent(Talent.LIGHT_CLOAK)) {
				((CloakOfShadows) i).activate(this);
			} else if (i instanceof HolyTome  && i.keptThroughLostInventory() && hasTalent(Talent.LIGHT_READING)) {
				((HolyTome) i).activate(this);
			} else if (i instanceof Wand && i.keptThroughLostInventory()){
				if (holster != null && holster.contains(i)){
					((Wand) i).charge(this, MagicalHolster.HOLSTER_SCALE_FACTOR);
				} else {
					((Wand) i).charge(this);
				}
			} else if (i instanceof MagesStaff && i.keptThroughLostInventory()){
				((MagesStaff) i).applyWandChargeBuff(this);
			}
		}

		updateHT(false);
	}

	@Override
	public void next() {
		if (isAlive())
			super.next();
	}

	public static interface Doom {
		public void onDeath();
	}
}
