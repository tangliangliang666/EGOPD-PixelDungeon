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

package com.shatteredpixel.shatteredpixeldungeon.actors;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.StormCloud;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AcceleratingFuture;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BoneWeaving;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Berserk;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ThirstBloodBarrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bless;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.IgnominiousHeartBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corrosion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FireImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FrostImbue;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Fury;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GritTeethBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HealBlock;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hex;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HuntingTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LostInventory;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Melting;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Momentum;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Ooze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Poison;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.RevengeTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SnipersMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Speed;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VengeanceArts;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VileBlood;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vulnerable;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Challenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.ValencinaSfx;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.warrior.Endure;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.AuraOfProtection;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.BeamingRay;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.LifeLinkSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ShieldOfLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.CrystalSpire;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DwarfKing;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Elemental;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.GnollGeomancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Necromancer;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Tengu;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.YogDzewa;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.MirrorImage;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.PrismaticImage;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.JellyWobble;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.BrokenSeal;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.Armor;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.curses.Bulk;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.AntiMagic;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Brimstone;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Flow;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Obfuscation;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Potential;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Swiftness;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.quest.Pickaxe;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfElements;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfRetribution;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfChallenge;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ScrollOfPsionicBlast;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.FerretTuft;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.SilentPrice;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLivingEarth;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Grim;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Kinetic;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DimDusk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HolyDecree;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.JusticeArbiter;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.ParadiseLost;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Sickle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.Thirst;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.WaxWing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.ShockingDart;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Door;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GeyserTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GnollRockfallTrap;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.GrimTrap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Earthroot;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MobSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.TargetHealthIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;

public abstract class Char extends Actor {
	
	public int pos = 0;
	
	public CharSprite sprite;
	
	public int HT;
	public int HP;
	
	protected float baseSpeed	= 1;
	protected PathFinder.Path path;

	public int paralysed	    = 0;
	public boolean rooted		= false;
	public boolean flying		= false;
	public int invisible		= 0;

	//these are relative to the hero
	public enum Alignment{
		ENEMY,
		NEUTRAL,
		ALLY
	}
	public Alignment alignment;
	
	public int viewDistance	= 8;
	
	public boolean[] fieldOfView = null;
	
	private LinkedHashSet<Buff> buffs = new LinkedHashSet<>();
	
	@Override
	protected boolean act() {
		if (fieldOfView == null || fieldOfView.length != Dungeon.level.length()){
			fieldOfView = new boolean[Dungeon.level.length()];
		}
		Dungeon.level.updateFieldOfView( this, fieldOfView );

		//throw any items that are on top of an immovable char
		if (properties().contains(Property.IMMOVABLE)){
			throwItems();
		}
		return false;
	}

	protected void throwItems(){
		Heap heap = Dungeon.level.heaps.get( pos );
		if (heap != null && heap.type == Heap.Type.HEAP
				&& !(heap.peek() instanceof Tengu.BombAbility.BombItem)
				&& !(heap.peek() instanceof Tengu.ShockerAbility.ShockerItem)) {
			ArrayList<Integer> candidates = new ArrayList<>();
			for (int n : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.passable[pos+n]){
					candidates.add(pos+n);
				}
			}
			if (!candidates.isEmpty()){
				Dungeon.level.drop( heap.pickUp(), Random.element(candidates) ).sprite.drop( pos );
			}
		}
	}

	public String name(){
		return Messages.get(this, "name");
	}

	public boolean canInteract(Char c){
		if (Dungeon.level.adjacent( pos, c.pos )){
			return true;
		} else if (c instanceof Hero
				&& alignment == Alignment.ALLY
				&& !hasProp(this, Property.IMMOVABLE)
				&& Dungeon.level.distance(pos, c.pos) <= 2*Dungeon.hero.pointsInTalent(Talent.ALLY_WARP)){
			return true;
		} else {
			return false;
		}
	}
	
	//swaps places by default
	public boolean interact(Char c){

		//don't allow char to swap onto hazard unless they're flying
		//you can swap onto a hazard though, as you're not the one instigating the swap
		if (!Dungeon.level.passable[pos] && !c.flying){
			return true;
		}

		//can't swap into a space without room
		if (properties().contains(Property.LARGE) && !Dungeon.level.openSpace[c.pos]
			|| c.properties().contains(Property.LARGE) && !Dungeon.level.openSpace[pos]){
			return true;
		}

		//we do a little raw position shuffling here so that the characters are never
		// on the same cell when logic such as occupyCell() is triggered
		int oldPos = pos;
		int newPos = c.pos;

		//can't swap or ally warp if either char is immovable
		if (hasProp(this, Property.IMMOVABLE) || hasProp(c, Property.IMMOVABLE)){
			return true;
		}

		//warp instantly with allies in this case
		if (c == Dungeon.hero && Dungeon.hero.hasTalent(Talent.ALLY_WARP)){
			PathFinder.buildDistanceMap(c.pos, BArray.or(Dungeon.level.passable, Dungeon.level.avoid, null));
			if (PathFinder.distance[pos] == Integer.MAX_VALUE){
				return true;
			}
			pos = newPos;
			c.pos = oldPos;
			ScrollOfTeleportation.appear(this, newPos);
			ScrollOfTeleportation.appear(c, oldPos);
			Dungeon.observe();
			GameScene.updateFog();
			return true;
		}

		//can't swap places if one char has restricted movement
		if (paralysed > 0 || c.paralysed > 0 || rooted || c.rooted
				|| buff(Vertigo.class) != null || c.buff(Vertigo.class) != null){
			return true;
		}

		c.pos = oldPos;
		moveSprite( oldPos, newPos );
		move( newPos );

		c.pos = newPos;
		c.sprite.move( newPos, oldPos );
		c.move( oldPos );
		
		c.spend( 1 / c.speed() );

		if (c == Dungeon.hero){
			if (Dungeon.hero.subClass == HeroSubClass.FREERUNNER){
				Buff.affect(Dungeon.hero, Momentum.class).gainStack();
			}

			Dungeon.hero.busy();
		}
		
		return true;
	}
	
	protected boolean moveSprite( int from, int to ) {
		
		if (sprite.isVisible() && sprite.parent != null && (Dungeon.level.heroFOV[from] || Dungeon.level.heroFOV[to])) {
			sprite.move( from, to );
			return true;
		} else {
			sprite.turnTo(from, to);
			sprite.place( to );
			return true;
		}
	}

	public void hitSound( float pitch ){
		Sample.INSTANCE.play(Assets.Sounds.HIT, 1, pitch);
	}

	public boolean blockSound( float pitch ) {
		return false;
	}
	
	protected static final String POS       = "pos";
	protected static final String TAG_HP    = "HP";
	protected static final String TAG_HT    = "HT";
	protected static final String TAG_SHLD  = "SHLD";
	protected static final String BUFFS	    = "buffs";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		
		super.storeInBundle( bundle );
		
		bundle.put( POS, pos );
		bundle.put( TAG_HP, HP );
		bundle.put( TAG_HT, HT );
		bundle.put( BUFFS, buffs );
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );
		
		pos = bundle.getInt( POS );
		HP = bundle.getInt( TAG_HP );
		HT = bundle.getInt( TAG_HT );
		
		for (Bundlable b : bundle.getCollection( BUFFS )) {
			if (b != null) {
				((Buff)b).attachTo( this );
			}
		}
	}

	final public boolean attack( Char enemy ){
		return attack(enemy, 1f, 0f, 1f);
	}
	
	public boolean attack( Char enemy, float dmgMulti, float dmgBonus, float accMulti ) {

		if (enemy == null) return false;

		//拇指 前二老板 T2「狩猎目标」：攻击开始前施加/叠加标记（需在命中判定前，防御削减才对本击生效）
		if (this == Dungeon.hero){
			Talent.onHeroAttackStarted( (Hero)this, enemy );
		}
		
		boolean visibleFight = Dungeon.level.heroFOV[pos] || Dungeon.level.heroFOV[enemy.pos];

		if (enemy.isInvulnerable(getClass())) {

			if (visibleFight) {
				enemy.sprite.showStatus( CharSprite.POSITIVE, Messages.get(this, "invulnerable") );

				Sample.INSTANCE.play(Assets.Sounds.HIT_PARRY, 1f, Random.Float(0.96f, 1.05f));
			}

			return false;

		} else if (hit( this, enemy, accMulti, false )) {
			
			int dr = Math.round(enemy.drRoll() * AscensionChallenge.statModifier(enemy));

			//拇指 前二老板 T2「狩猎目标」：目标防御值下降（层数×2，不低于 0）
			if (this == Dungeon.hero){
				HuntingTarget hunting = enemy.buff( HuntingTarget.class );
				if (hunting != null){
					dr = Math.max( 0, dr - 2 * hunting.stacks );
				}
			}
			
			if (this instanceof Hero){
				Hero h = (Hero)this;
				if (h.belongings.attackingWeapon() instanceof MissileWeapon
						&& h.subClass == HeroSubClass.SNIPER
						&& !Dungeon.level.adjacent(h.pos, enemy.pos)){
					dr = 0;
				}

				if (h.buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) != null){
					dr = 0;
				}

				//圣宣：持有（主手或副手）时攻击，有50%概率造成无视护甲的魔法伤害
				if ((h.belongings.weapon() instanceof HolyDecree
						|| h.belongings.secondWep() instanceof HolyDecree)
						&& Random.Int(100) < 50){
					dr = 0;
					HolyDecree.magicHit = true; //标记本次攻击为魔法伤害（伤害浮字图标用）
				}

				//正义裁决者：持有（主手或副手）时攻击无视目标防御
				if (h.belongings.weapon() instanceof JusticeArbiter
						|| h.belongings.secondWep() instanceof JusticeArbiter){
					dr = 0;
				}
			}

			//we use a float here briefly so that we don't have to constantly round while
			// potentially applying various multiplier effects
			float dmg;
			Preparation prep = buff(Preparation.class);
			if (prep != null){
				dmg = prep.damageRoll(this);
				if (this == Dungeon.hero && Dungeon.hero.hasTalent(Talent.BOUNTY_HUNTER)) {
					Buff.affect(Dungeon.hero, Talent.BountyHunterTracker.class, 0.0f);
				}
			} else {
				dmg = damageRoll();
			}

			dmg = dmg*dmgMulti;

			//flat damage bonus is affected by multipliers
			dmg += dmgBonus;

			if (enemy.buff(GuidingLight.Illuminated.class) != null){
				enemy.buff(GuidingLight.Illuminated.class).detach();
				if (this == Dungeon.hero && Dungeon.hero.hasTalent(Talent.SEARING_LIGHT)){
					dmg += 1 + 2*Dungeon.hero.pointsInTalent(Talent.SEARING_LIGHT);
				}
				if (this != Dungeon.hero && Dungeon.hero.subClass == HeroSubClass.PRIEST){
					enemy.damage(5+Dungeon.hero.lvl, GuidingLight.INSTANCE);
				}
			}

			//拇指 前二老板 T1「烧震打击」：攻击处于燃烧或麻痹状态的敌人时额外造成 1/2 点伤害
			if (this == Dungeon.hero
					&& Dungeon.hero.hasTalent(Talent.BURN_SHOCK)
					&& (enemy.buff(Burning.class) != null || enemy.buff(Paralysis.class) != null)){
				dmg += Dungeon.hero.pointsInTalent(Talent.BURN_SHOCK);
			}

			Berserk berserk = buff(Berserk.class);
			if (berserk != null) dmg = berserk.damageFactor(dmg);

			if (buff( Fury.class ) != null) {
				dmg *= 1.5f;
			}

			if (buff( PowerOfMany.PowerBuff.class) != null){
				if (buff( BeamingRay.BeamingRayBoost.class) != null
					&& buff( BeamingRay.BeamingRayBoost.class).object == enemy.id()){
					dmg *= 1.3f + 0.05f*Dungeon.hero.pointsInTalent(Talent.BEAMING_RAY);
				} else {
					dmg *= 1.25f;
				}
			}

			for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
				dmg *= buff.meleeDamageFactor();
			}

			dmg *= AscensionChallenge.statModifier(this);

			//friendly endure
			Endure.EndureTracker endure = buff(Endure.EndureTracker.class);
			if (endure != null) dmg = endure.damageFactor(dmg);

			//enemy endure
			endure = enemy.buff(Endure.EndureTracker.class);
			if (endure != null){
				dmg = endure.adjustDamageTaken(dmg);
			}

			if (enemy.buff(ScrollOfChallenge.ChallengeArena.class) != null){
				dmg *= 0.67f;
			}

			if (Dungeon.hero.alignment == enemy.alignment
					&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
					&& (Dungeon.level.distance(enemy.pos, Dungeon.hero.pos) <= 2 || enemy.buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)){
				dmg *= 0.9f - 0.1f*Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
			}

			if (enemy.buff(MonkEnergy.MonkAbility.Meditate.MeditateResistance.class) != null){
				dmg *= 0.2f;
			}

			if ( buff(Weakness.class) != null ){
				dmg *= 0.67f;
			}

			//拇指 前二老板 盔甲体系：持有「心-不光彩」时的伤害加成（耻辱之心）；
			//攻击「瞄准心脏」标记目标时的额外伤害（弱点贯穿）
			if (this == Dungeon.hero){
				IgnominiousHeartBuff ignHeart = buff( IgnominiousHeartBuff.class );
				if (ignHeart != null){
					dmg *= 1f + ignHeart.damageBonus();
				}
				if (enemy.buff( AimHeartMark.class ) != null){
					dmg *= AimHeartMark.damageMultiplier( (Hero) this );
				}
			}

			//characters influenced by aggression deal 1/2 damage to bosses
			if ( enemy.buff(StoneOfAggression.Aggression.class) != null
					&& enemy.alignment == alignment
					&& (Char.hasProp(enemy, Property.BOSS) || Char.hasProp(enemy, Property.MINIBOSS))){
				dmg *= 0.5f;
				//yog-dzewa specifically takes 1/4 damage
				if (enemy instanceof YogDzewa){
					dmg *= 0.5f;
				}
			}
			
			int effectiveDamage = enemy.defenseProc( this, Math.round(dmg) );
			//do not trigger on-hit logic if defenseProc returned a negative value
			if (effectiveDamage >= 0) {
				effectiveDamage = Math.max(effectiveDamage - dr, 0);

				if (enemy.buff(Viscosity.ViscosityTracker.class) != null) {
					effectiveDamage = enemy.buff(Viscosity.ViscosityTracker.class).deferDamage(effectiveDamage);
					enemy.buff(Viscosity.ViscosityTracker.class).detach();
				}

				//vulnerable specifically applies after armor reductions
				if (enemy.buff(Vulnerable.class) != null) {
					effectiveDamage *= 1.33f;
				}

				effectiveDamage = attackProc(enemy, effectiveDamage);
			}
			if (visibleFight) {
				if (effectiveDamage > 0 || !enemy.blockSound(Random.Float(0.96f, 1.05f))) {
					hitSound(Random.Float(0.87f, 1.15f));
				}
			}

			// If the enemy is already dead, interrupt the attack.
			// This matters as defence procs can sometimes inflict self-damage, such as armor glyphs.
			if (!enemy.isAlive()){
				if (this == Dungeon.hero) Talent.onHeroAttackResolved( (Hero)this, enemy );
				return true;
			}

			//拇指 前二老板 战争英雄：命中后、伤害生效前施加“命中时”负面状态
			//（须在 enemy.damage 之前，保证状态先于解除麻痹/震颤-灼热等判定）
			if (this == Dungeon.hero && this instanceof Hero && effectiveDamage >= 0){
				Talent.onHeroAttackLanded( (Hero)this, enemy );
			}

			enemy.damage( effectiveDamage, this );

			//中指长兄 T1「仔细看好！」：命中叠「夸耀」，若本击击杀则额外 +1 层
			//（放在 enemy.damage 之后，才能读到「是否击杀」的结果；未命中在下面 else 分支清空）
			if (this == Dungeon.hero) Talent.onHeroAttackBrag( (Hero) this, enemy, !enemy.isAlive() );

			//中指长兄 T2「永不遗忘」：攻击命中英雄（+2 时含任意友方单位）的敌人被记为「报复对象」
			//（同样放在 enemy.damage 之后——这一下已经真的打中了；未命中走下面的 else 分支，不计入）
			Talent.onEnemyAttackLanded( this, enemy );

			//中指长兄 转职「忠义巡礼者」：攻击命中中指长兄本人或任意友方单位的敌人获得
			//1 层「怨恨标记」（复仇技艺的弹药；与上一行的挂钩点完全相同，只是不再需要天赋）。
			//传 effectiveDamage 是为了让专精天赋「如数奉还」按「这一击实际打出多少伤害」判阈值。
			//**远程 / 法术弹道**那一路不走 Char.attack，改由 Char.hit 的 3 参重载收口
			//（VengeanceArts.onRangedAttackHit），两条路互不重叠、不会重复叠层。
			VengeanceArts.onAttackLanded( this, enemy, effectiveDamage );

			//污血（葬花楔）：被葬花楔钉中的单位每次受到攻击命中后，额外承受 (1+楔等级) 点
			//无视护甲的魔法伤害（等级取插在目标身上最高强化的葬花楔；目标已被本击击杀则不追加）
			VileBlood vileBlood = enemy.buff(VileBlood.class);
			if (vileBlood != null && enemy.isAlive()){
				enemy.damage( 1 + vileBlood.level, this );
			}

			//复位圣宣的魔法伤害标记（每击只生效一次）
			HolyDecree.magicHit = false;

			if (buff(FireImbue.class) != null)  buff(FireImbue.class).proc(enemy);
			if (buff(FrostImbue.class) != null) buff(FrostImbue.class).proc(enemy);

			if (enemy.isAlive() && enemy.alignment != alignment && prep != null && prep.canKO(enemy)){
				enemy.HP = 0;
				if (enemy.buff(Brute.BruteRage.class) != null){
					enemy.buff(Brute.BruteRage.class).detach();
				}
				if (!enemy.isAlive()) {
					enemy.die(this);
				} else {
					//helps with triggering any on-damage effects that need to activate
					enemy.damage(-1, this);
					DeathMark.processFearTheReaper(enemy);
				}
				if (enemy.sprite != null) {
					enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Preparation.class, "assassinated"));
				}
			}

			Talent.CombinedLethalityAbilityTracker combinedLethality = buff(Talent.CombinedLethalityAbilityTracker.class);
			if (combinedLethality != null && this instanceof Hero && ((Hero) this).belongings.attackingWeapon() instanceof MeleeWeapon && combinedLethality.weapon != ((Hero) this).belongings.attackingWeapon()){
				if ( enemy.isAlive() && enemy.alignment != alignment && !Char.hasProp(enemy, Property.BOSS)
						&& !Char.hasProp(enemy, Property.MINIBOSS) &&
						(enemy.HP/(float)enemy.HT) <= 0.4f*((Hero)this).pointsInTalent(Talent.COMBINED_LETHALITY)/3f) {
					enemy.HP = 0;
					if (enemy.buff(Brute.BruteRage.class) != null){
						enemy.buff(Brute.BruteRage.class).detach();
					}
					if (!enemy.isAlive()) {
						enemy.die(this);
					} else {
						//helps with triggering any on-damage effects that need to activate
						enemy.damage(-1, this);
						DeathMark.processFearTheReaper(enemy);
					}
					if (enemy.sprite != null) {
						enemy.sprite.showStatus(CharSprite.NEGATIVE, Messages.get(Talent.CombinedLethalityAbilityTracker.class, "executed"));
					}
				}
				combinedLethality.detach();
			}

			if (enemy.sprite != null) {
				enemy.sprite.bloodBurstA(sprite.center(), effectiveDamage);
				enemy.sprite.flash();
			}

			if (!enemy.isAlive() && visibleFight) {
				if (enemy == Dungeon.hero) {
					
					if (this == Dungeon.hero) {
						return true;
					}

					if (this instanceof WandOfLivingEarth.EarthGuardian
							|| this instanceof MirrorImage || this instanceof PrismaticImage){
						Badges.validateDeathFromFriendlyMagic();
					}
					Dungeon.fail( this );
					GLog.n( Messages.capitalize(Messages.get(Char.class, "kill", name())) );
					
			} else if (this == Dungeon.hero) {
				GLog.i( Messages.capitalize(Messages.get(Char.class, "defeat", enemy.name())) );
			}
		}
		
		if (this == Dungeon.hero) Talent.onHeroAttackResolved( (Hero)this, enemy );

		return true;
		
		} else {

			if (enemy.sprite != null){
				//奥丁之眼「预知眼」：闪避成功时 50% 概率头顶弹出嘲讽文本，弹出时随机取五条之一（保留 miss 原因 icon）
				String missText = enemy.defenseVerb();
				//本次取到第几条嘲讽（0 = 没取）；只有第 1 条「怎么，打不中吗？」配了语音
				int dodgeTaunt = 0;
				if (enemy.buff(OdinsEye.precognition.class) != null && Random.Int(2) == 0){
					dodgeTaunt = 1 + Random.Int(5);
					missText = Messages.get(OdinsEye.precognition.class, "dodge_taunt_" + dodgeTaunt);
				}

				if (hitMissIcon != -1){
					//dooking is a playful sound Ferrets can make, like low pitched chirping
					// I doubt this will translate, so it's only in English
					if (hitMissIcon == FloatingText.MISS_TUFT && Messages.lang() == Languages.ENGLISH && Random.Int(10) == 0) {
						enemy.sprite.showStatusWithIcon(CharSprite.NEUTRAL, "dooked", hitMissIcon);
					} else {
						enemy.sprite.showStatusWithIcon(CharSprite.NEUTRAL, missText, hitMissIcon);
					}
					hitMissIcon = -1;
				} else {
					enemy.sprite.showStatus(CharSprite.NEUTRAL, missText);
				}

				//只有第 1 条「怎么，打不中吗？」配了语音；其余四条台词不出声
				if (dodgeTaunt == 1){
					ValencinaSfx.playDodgeTaunt();
				}
			}
			if (visibleFight) {
				//TODO enemy.defenseSound? currently miss plays for monks/crab even when they parry
				Sample.INSTANCE.play(Assets.Sounds.MISS);
			}

			//拇指 前二老板 通用 T3「动作太慢!」：成功闪避敌方普通物理攻击 → 不消耗回合的反击
			if (this != Dungeon.hero && this.alignment == Alignment.ENEMY && enemy == Dungeon.hero){
				Talent.onHeroDodgedEnemyAttack( (Hero) enemy, this );
			}
			
			if (this == Dungeon.hero) Talent.onHeroAttackMissed( (Hero)this, enemy );

			if (this == Dungeon.hero) Talent.onHeroAttackResolved( (Hero)this, enemy );
			return false;
			
		}

	}

	public static int INFINITE_ACCURACY = 1_000_000;
	public static int INFINITE_EVASION = 1_000_000;

	/**
	 * 3 参重载＝「法术弹道」那一类攻击的专用入口（{@code hit( this, enemy, true )}）：
	 * 近战与投掷武器一律走下面那个 4 参重载（{@code Char.attack}），两边互不重叠。
	 */
	final public static boolean hit( Char attacker, Char defender, boolean magic ) {
		boolean result = hit(attacker, defender, magic ? 2f : 1f, magic);

		//中指长兄 转职「忠义巡礼者」：远程 / 法术攻击同样算「攻击命中」，
		//命中的敌人照近战的规矩吃 1 层怨恨标记（近战/投掷那条路在 Char.attack 里，
		//两条路各收一次口、不会重复叠层）。放在这个重载里而不是各怪物自己的 zap() 里：
		//萨满 / 术士 / DM / 元素 / 眼魔 / 尤格之拳 / 哨兵都走这里，一处收口就不会漏。
		if (result) VengeanceArts.onRangedAttackHit( attacker, defender );

		return result;
	}

	public static boolean hit( Char attacker, Char defender, float accMulti, boolean magic ) {

		//奥丁之眼：即将受到攻击前（命中判定前），若充能>0且未过热/未激活，则自动开启预知眼。
		//须在 defStat 计算之前完成，这样本段判定即吃到预知眼的无限闪避。
		if (defender instanceof Hero && defender == Dungeon.hero){
			OdinsEye.autoUse((Hero) defender);
		}

		//命中 / 闪避取数的**唯一收口**：考验 HOD（敌方命中 +10%/区）与 NETZACH（敌方闪避 +10%/区）
		//都在这里生效。⚠️ 另有两处「把闪避折算成减伤」的复刻必须同源：
		//Talent.dodgeAsDamageReduction 与 Stone.proc（它们的注释里也自认是 copy-pasta）。
		float acuStat = Trials.finalAccuracy( attacker, defender );
		float defStat = Trials.finalEvasion( defender, attacker );

		if (defender instanceof Hero && ((Hero) defender).damageInterrupt){
			((Hero) defender).interrupt();
		}

		//invisible chars always hit (for the hero this is surprise attacking)
		if (attacker.invisible > 0 && attacker.canSurpriseAttack()){
			acuStat = INFINITE_ACCURACY;
		}

		if (defender.buff(MonkEnergy.MonkAbility.Focus.FocusBuff.class) != null){
			defStat = INFINITE_EVASION;
		}

		//奥丁之眼「预知眼」：持续期间获得无限闪避（无限闪避胜于无限命中，见下）
		if (defender.buff(OdinsEye.precognition.class) != null){
			defStat = INFINITE_EVASION;
		}

		//if accuracy or evasion are large enough, treat them as infinite.
		//note that infinite evasion beats infinite accuracy
		if (defStat >= INFINITE_EVASION){
			hitMissIcon = FloatingText.getMissReasonIcon(attacker, acuStat, defender, INFINITE_EVASION);
			trackAcceleratingFuture( attacker, defender, false );
			return false;
		} else if (acuStat >= INFINITE_ACCURACY){
			hitMissIcon = FloatingText.getHitReasonIcon(attacker, INFINITE_ACCURACY, defender, defStat);
			trackAcceleratingFuture( attacker, defender, true );
			return true;
		}

		float acuRoll = Random.Float( acuStat );
		if (attacker.buff(Bless.class) != null) acuRoll *= 1.25f;
		if (attacker.buff(  Hex.class) != null) acuRoll *= 0.8f;
		if (attacker.buff( Daze.class) != null) acuRoll *= 0.5f;
		for (ChampionEnemy buff : attacker.buffs(ChampionEnemy.class)){
			acuRoll *= buff.evasionAndAccuracyFactor();
		}
		acuRoll *= AscensionChallenge.statModifier(attacker);
		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.BLESS)
				&& attacker.alignment == Alignment.ALLY){
			// + 3%/5%
			acuRoll *= 1.01f + 0.02f*Dungeon.hero.pointsInTalent(Talent.BLESS);
		}
		acuRoll *= accMulti;

		//沉默的代价：英雄攻击时获得额外精准（命中掷点的乘区提升）
		if (attacker == Dungeon.hero){
			acuRoll *= SilentPrice.heroAccuracyMultiplier();
		}

		float defRoll = evasionRoll( defender, defStat );

		//变化无常（神谕代行者 T2）：闪避掷点额外重复 1/2 次，取最高值。
		//「是否被命中」是比大小判定，重复取最高无法用闪避数值的乘区等价表达，故实现落在掷点处；
		//层数取自天赋点数（+1 = 额外 1 次，+2 = 额外 2 次）。
		if (defender instanceof Hero){
			int rerolls = ((Hero) defender).pointsInTalent( Talent.SHIFTING_FATE );
			for (int i = 0; i < rerolls; i++){
				defRoll = Math.max( defRoll, evasionRoll( defender, defStat ) );
			}
		}

		if (acuRoll >= defRoll){
			hitMissIcon = FloatingText.getHitReasonIcon(attacker, acuRoll, defender, defRoll);
			trackAcceleratingFuture( attacker, defender, true );
			return true;
		} else {
			hitMissIcon = FloatingText.getMissReasonIcon(attacker, acuRoll, defender, defRoll);
			trackAcceleratingFuture( attacker, defender, false );
			return false;
		}
	}

	/**
	 * 单次闪避掷点（含全部乘区）。
	 *
	 * <p>抽成方法是为了让「变化无常」的重复取最高值复用同一套乘区 —— 乘区只写一份，
	 * 避免两条路径漂移。（同类教训：{@code Talent.dodgeAsDamageReduction} 与 {@code Stone.proc}
	 * 各自复刻过一遍本算法的闭式版本，改动本方法时要一并回查那两处。）</p>
	 *
	 * <p>注意：该闭式复刻无法表达「重复取最高值」，所以若靠蜕变卷轴让非神谕角色同时拿到
	 * 变化无常与中指 T1「你犯规了」，其减伤换算会按单次掷点估算（偏保守，不会崩）。</p>
	 */
	private static float evasionRoll( Char defender, float defStat ){

		float defRoll = Random.Float( defStat );
		if (defender.buff(Bless.class) != null) defRoll *= 1.25f;
		if (defender.buff(  Hex.class) != null) defRoll *= 0.8f;
		if (defender.buff( Daze.class) != null) defRoll *= 0.5f;
		for (ChampionEnemy buff : defender.buffs(ChampionEnemy.class)){
			defRoll *= buff.evasionAndAccuracyFactor();
		}
		defRoll *= AscensionChallenge.statModifier(defender);
		if (Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.BLESS)
				&& defender.alignment == Alignment.ALLY){
			// + 3%/5%
			defRoll *= 1.01f + 0.02f*Dungeon.hero.pointsInTalent(Talent.BLESS);
		}
		defRoll *= FerretTuft.evasionMultiplier();

		//沉默的代价：英雄被攻击时获得额外闪避（闪避掷点的乘区提升）
		if (defender == Dungeon.hero){
			defRoll *= SilentPrice.heroEvasionMultiplier();
		}

		return defRoll;
	}

	//「加速的未来」（拇指 前二老板）：命中判定后结算层数变化。
	//- 英雄攻击命中敌人 → +1 层（巴勒莫剑术自身那一击不产生层数，见 isStrikeSuppressed）
	//- 闪避敌人的攻击 → +1 层
	//- 被敌人攻击命中 → 清零全部层数
	private static void trackAcceleratingFuture( Char attacker, Char defender, boolean hit ) {
		if (Dungeon.hero == null) return;
		Hero hero = Dungeon.hero;
		if (hero.heroClass != HeroClass.VALENCINA) return;

		if (attacker == hero){
			if (hit && defender.alignment == Alignment.ENEMY && !AcceleratingFuture.isStrikeSuppressed()){
				AcceleratingFuture.gain( hero );
			}
		} else if (defender == hero && attacker.alignment == Alignment.ENEMY){
			if (hit){
				AcceleratingFuture.lose( hero );
			} else {
				AcceleratingFuture.gain( hero );
			}
		}
	}

	private static int hitMissIcon = -1;

	public int attackSkill( Char target ) {
		return 0;
	}
	
	public int defenseSkill( Char enemy ) {
		return 0;
	}
	
	public String defenseVerb() {
		return Messages.get(this, "def_verb");
	}
	
	public int drRoll() {
		int dr = 0;

		dr += Random.NormalIntRange( 0 , Barkskin.currentLevel(this) );

		// 骨骸编织：护甲加成 0~角色等级。上界每次结算实时读取（不在消耗素材时冻结），
		// 与 Barkskin 同一口径：buff 本身不存数值，因此图标上也没有「层数」。
		// ⚠️ 这里的 if 守卫不能去掉——没有该 buff 时不该多掷随机数（否则会挪动全局随机流）。
		if (buff(BoneWeaving.class) != null){
			dr += Random.NormalIntRange( 0 , BoneWeaving.maxArmor(this) );
		}

		return dr;
	}
	
	public int damageRoll() {
		return 1;
	}
	
	//TODO it would be nice to have a pre-armor and post-armor proc.
	// atm attack is always post-armor and defence is already pre-armor
	
	public int attackProc( Char enemy, int damage ) {
		for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
			buff.onAttackProc( enemy );
		}
		return damage;
	}
	
	public int defenseProc( Char enemy, int damage ) {

		//圣宣：持有者攻击命中时，由目标自身的受击逻辑播放黑白蝴蝶特效
		//（参考原版怪物受击特效的触发方式：效果由"生效单位"自身触发）
		if (enemy instanceof Hero
				&& (((Hero) enemy).belongings.weapon() instanceof HolyDecree
				|| ((Hero) enemy).belongings.secondWep() instanceof HolyDecree)){
			HolyDecree.emitButterflies( this );
		}

		//失乐园：持有者攻击命中敌方时，由目标自身的受击逻辑播放"长矛贯穿"特效
		//（与圣宣相同的触发方式：效果由"生效单位"自身触发，攻击者侧跨单位特效不显示）
		if (enemy instanceof Hero
				&& (((Hero) enemy).belongings.weapon() instanceof ParadiseLost
				|| ((Hero) enemy).belongings.secondWep() instanceof ParadiseLost)){
			com.shatteredpixel.shatteredpixeldungeon.effects.HeavenStrike.hit( this );
		}

		//蜡翼：持有者（主手或副手）受到攻击时点燃攻击者
		if (this instanceof Hero
				&& (((Hero) this).belongings.weapon() instanceof WaxWing
				|| ((Hero) this).belongings.secondWep() instanceof WaxWing)){
			WaxWing.ignite( enemy );
		}

		Earthroot.Armor armor = buff( Earthroot.Armor.class );
		if (armor != null) {
			damage = armor.absorb( damage );
		}

		ShieldOfLight.ShieldOfLightTracker shield = buff( ShieldOfLight.ShieldOfLightTracker.class);
		if (shield != null && shield.object == enemy.id()){
			int min = 1 + Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT);
			damage -= Random.NormalIntRange(min, 2*min);
			damage = Math.max(damage, 0);
		} else if (this == Dungeon.hero
				&& Dungeon.hero.heroClass != HeroClass.CLERIC
				&& Dungeon.hero.hasTalent(Talent.SHIELD_OF_LIGHT)
				&& TargetHealthIndicator.instance.target() == enemy){
			//33/50%
			if (Random.Int(6) < 1+Dungeon.hero.pointsInTalent(Talent.SHIELD_OF_LIGHT)){
				damage -= 1;
			}
		}

		// hero and pris images skip this as they already benefit from hero's armor glyph proc
		if (!(this instanceof Hero || this instanceof PrismaticImage)) {
			if (Dungeon.hero.alignment == alignment && Dungeon.hero.belongings.armor() != null
					&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
					&& (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 2 || buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)) {
				damage = Dungeon.hero.belongings.armor().proc( enemy, this, damage );
			}
		}

		return damage;
	}

	//Returns the level a glyph is at for a char, or -1 if they are not benefitting from that glyph
	//This function is needed as (unlike enchantments) many glyphs trigger in a variety of cases
	public int glyphLevel(Class<? extends Armor.Glyph> cls){
		if (Dungeon.hero != null && Dungeon.level != null
				&& this != Dungeon.hero && Dungeon.hero.alignment == alignment
				&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
				&& (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 2 || buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)) {
			return Dungeon.hero.glyphLevel(cls);
		} else {
			return -1;
		}
	}
	
	/**
	 * 「原始」移动速度：把 baseSpeed、各个 buff（残废 / 耐力 / 肾上腺素 / 迅捷 / 恐惧）
	 * 与迅疾 / 流 / 笨重三种护甲刻文全部叠完，**但不含**考验的最终夹取。
	 * <p><b>子类若要调整速度，一律覆写本方法，不要覆写 {@link #speed()}。</b></p>
	 */
	public float speedRaw() {
		float speed = baseSpeed;
		if ( buff( Cripple.class ) != null ) speed /= 2f;
		if ( buff( Stamina.class ) != null) speed *= 1.5f;
		if ( buff( Adrenaline.class ) != null) speed *= 2f;
		if ( buff( Haste.class ) != null) speed *= 3f;
		if ( buff( Dread.class ) != null) speed *= 2f;

		speed *= Swiftness.speedBoost(this, glyphLevel(Swiftness.class));
		speed *= Flow.speedBoost(this, glyphLevel(Flow.class));
		speed *= Bulk.speedBoost(this, glyphLevel(Bulk.class));

		return speed;
	}

	/**
	 * 最终移动速度（移动延迟 = 1/本值）。<b>全作唯一的移动速度出口，故意做成 final。</b>
	 * <p>移动延迟恒由 {@code 1/speed()} 或 {@code delay/speed()} 算出，所以在这里夹一次
	 * 就覆盖了全部移动延迟站点（含将来新增的），而 {@code final} 保证没人能绕过收口点。
	 * 考验（如 HOKMA 的延迟上下限）要求压过一切 buff / 装备 / 天赋 / 原版挑战修正，
	 * 一旦允许子类覆写本方法，就一定会出现漏夹的路径。</p>
	 * <p>子类的速度修正请覆写 {@link #speedRaw()}。</p>
	 */
	public final float speed(){
		return Trials.modifyMoveSpeed( this, speedRaw() );
	}

	//currently only used by invisible chars, or by the hero
	public boolean canSurpriseAttack(){
		return true;
	}
	
	//used so that buffs(Shieldbuff.class) isn't called every time unnecessarily
	private int cachedShield = 0;
	public boolean needsShieldUpdate = true;
	
	public int shielding(){
		if (!needsShieldUpdate){
			return cachedShield;
		}
		
		cachedShield = 0;
		for (ShieldBuff s : buffs(ShieldBuff.class)){
			cachedShield += s.shielding();
		}
		needsShieldUpdate = false;
		return cachedShield;
	}
	
	/**
	 * 唯一的「回血出口」——本作把一切「把 HP 往上抬」的效果都收口到这里。
	 *
	 * <p>新增回血效果时<b>务必</b>调本方法（而不是直接写 {@code HP = Math.min(HT, HP + n)}），
	 * 否则会绕过 {@link HealBlock}（洛伊德护符的「禁疗」）以及以后可能加在同一个点上的其它规则。</p>
	 *
	 * <p>「禁疗」期间恒返回 0：HP 一点不动，也不会返回可供调用方画浮字的值
	 * （调用方应使用本方法的返回值来做「+N」显示，见 {@code ChesedMend} / {@code ThirstBloodBarrier}）。</p>
	 *
	 * @param amount 期望回复量（≤0 视为无效）
	 * @return 实际回复量（被禁疗、已死亡、或已满血时为 0）
	 */
	public int heal( int amount ) {

		if (amount <= 0 || !isAlive()) return 0;

		//「禁疗」：所有来源的回血一律置 0（含 CHESED 考验的 ChesedMend）
		if (HealBlock.blocks( this )) return 0;

		int before = HP;
		HP = Math.min( HT, HP + amount );
		return HP - before;
	}

	public void damage( int dmg, Object src ) {
		
		if (!isAlive() || dmg < 0) {
			return;
		}

		if(isInvulnerable(src.getClass())){
			sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "invulnerable"));
			return;
		}

		if (!(src instanceof LifeLink || src instanceof Hunger) && buff(LifeLink.class) != null){
			HashSet<LifeLink> links = buffs(LifeLink.class);
			for (LifeLink link : links.toArray(new LifeLink[0])){
				if (Actor.findById(link.object) == null){
					links.remove(link);
					link.detach();
				}
			}
			dmg = (int)Math.ceil(dmg / (float)(links.size()+1));
			for (LifeLink link : links){
				Char ch = (Char)Actor.findById(link.object);
				if (ch != null) {
					ch.damage(dmg, link);
					if (!ch.isAlive()) {
						link.detach();
						if (ch == Dungeon.hero){
							Badges.validateDeathFromFriendlyMagic();
							Dungeon.fail(src);
							GLog.n( Messages.get(LifeLink.class, "ondeath") );
						}
					}
				}
			}
		}

		//temporarily assign to a float to avoid rounding a bunch
		float damage = dmg;

		//沉默的代价：英雄造成的伤害提升（覆盖近战/远程/法术/技能等一切以英雄为源的伤害，未持有时乘 1）
		if (src instanceof Hero && src != this){
			damage *= SilentPrice.heroDamageMultiplier();
		}

		//中指长兄 通用 T3「野兽狂怒」：血量低于 50% 时，自己打出去的伤害 +10/20/40%。
		//① 位置与下面的「报复对象」同一处：这里的 src 覆盖面最广，近战/投掷/技能/法杖一次全管住，
		//   所以增伤口径直接复用 Talent.isHeroDealtDamage（英雄本人为源 + 法杖），与项目既有约定一致。
		//② 与「受到的伤害」那一份加成互不相干：Hero.damage 里乘的是打进来的伤害，这里乘的是打出去的伤害，
		//   同一笔伤害只会走到其中一条（`this` 是挨打方，`src` 是出手方），不存在叠乘两次。
		//③ 放在 float 乘区靠前处（十戒/抗性/冠军倍率等都在其后），乘法可交换、先后不影响结果。
		if (Talent.isHeroDealtDamage(src, this)){
			damage *= Talent.beastFuryMultiplier( Dungeon.hero );

			//中指长兄 盔甲技能「咬紧牙关」的 T4 天赋「濒亡狂怒」：
			//持续期间血量等于 0 时，自己打出去的伤害 +100%/200%/300%。
			//与上面那条同一个判据、同一处，一笔伤害只会乘一次。
			damage *= Talent.nearDeathFuryMultiplier( Dungeon.hero );
		}

		//中指长兄 T2「永不遗忘」：被记为「报复对象」的敌人，受到来自中指长兄的伤害 +20%。
		//放在这里而不是 Char.attack 里：这一处的 src 覆盖面最广，近战/投掷/技能/法杖一次全管住。
		//判定细节（含法杖为什么单独认一类）见 Talent.isHeroDealtDamage。
		if (buff(RevengeTarget.class) != null && Talent.isHeroDealtDamage(src, this)){
			damage *= RevengeTarget.DAMAGE_BONUS;
		}

		//中指长兄 转职「背叛家人者」专精天赋「融化而死」：身上带「融化」的目标受到的火焰伤害
		//提高 50%/100%/150%；对免疫火焰的目标改为其受到的一切伤害都提高同等比例。
		//放在这一处（float 乘区、`this` 是挨打方）与上面的「报复对象」同理：src 覆盖面最广，
		//近战附加火伤、火场、点燃、松脂涂层、烈焰法杖一次全管住。
		//注意目标与本击是不是火焰伤害的判据都收在 Melting 里（唯一取点），这里只乘一次。
		float meltingMulti = Melting.damageMultiplier( this, src );
		if (meltingMulti != 1f){
			damage *= meltingMulti;
		}

		//if dmg is from a character we already reduced it in Char.attack
		if (!(src instanceof Char)) {
			if (Dungeon.hero.alignment == alignment
					&& Dungeon.hero.buff(AuraOfProtection.AuraBuff.class) != null
					&& (Dungeon.level.distance(pos, Dungeon.hero.pos) <= 2 || buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null)) {
				damage *= 0.9f - 0.1f*Dungeon.hero.pointsInTalent(Talent.AURA_OF_PROTECTION);
			}
		}

		if (buff(PowerOfMany.PowerBuff.class) != null){
			if (buff(LifeLinkSpell.LifeLinkSpellBuff.class) != null){
				damage *= 0.70f - 0.05f*Dungeon.hero.pointsInTalent(Talent.LIFE_LINK);
			} else {
				damage *= 0.75f;
			}
		}

		Terror t = buff(Terror.class);
		if (t != null){
			t.recover();
		}
		Dread d = buff(Dread.class);
		if (d != null){
			d.recover();
		}
		Charm c = buff(Charm.class);
		if (c != null){
			c.recover(src);
		}
		if (this.buff(Frost.class) != null){
			Buff.detach( this, Frost.class );
		}
		if (this.buff(MagicalSleep.class) != null){
			Buff.detach(this, MagicalSleep.class);
		}
		if (this.buff(Doom.class) != null && !isImmune(Doom.class)){
			damage *= 1.67f;
		}
		if (alignment != Alignment.ALLY && this.buff(DeathMark.DeathMarkTracker.class) != null){
			damage *= 1.25f;
		}

		if (buff(Sickle.HarvestBleedTracker.class) != null){
			buff(Sickle.HarvestBleedTracker.class).detach();

			if (!isImmune(Bleeding.class)){
				Bleeding b = buff(Bleeding.class);
				if (b == null){
					b = new Bleeding();
				}
				b.announced = false;
				b.set(dmg, Sickle.HarvestBleedTracker.class);
				b.attachTo(this);
				sprite.showStatus(CharSprite.WARNING, Messages.titleCase(b.name()) + " " + (int)b.level());
				return;
			}
		}

		Class<?> srcClass = src.getClass();
		if (isImmune( srcClass )) {
			damage = 0;
		} else {
			damage *= resist( srcClass );
		}

		dmg = Math.round(damage);

		//we ceil these specifically to favor the player vs. champ dmg reduction
		// most important vs. giant champions in the earlygame
		for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
			dmg = (int) Math.ceil(dmg * buff.damageTakenFactor());
		}
		
		//TODO improve this when I have proper damage source logic
		if (AntiMagic.RESISTS.contains(src.getClass())){
			dmg -= AntiMagic.drRoll(this, glyphLevel(AntiMagic.class));
			if (buff(ArcaneArmor.class) != null) {
				dmg -= Random.NormalIntRange(0, buff(ArcaneArmor.class).level());
			}
			if (dmg < 0) dmg = 0;
		}
		
		if (buff( Paralysis.class ) != null) {
			buff( Paralysis.class ).processDamage(dmg);
		}

		BrokenSeal.WarriorShield shield = buff(BrokenSeal.WarriorShield.class);
		if (!(src instanceof Hunger)
				&& dmg > 0
				//either HP is already half or below (ignoring shield)
				// or the hit will reduce it to half or below
				&& (HP <= HT/2 || HP + shielding() - dmg <= HT/2)
				&& shield != null && !shield.coolingDown()){
			sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(buff(BrokenSeal.WarriorShield.class).maxShield()), FloatingText.SHIELDING);
			shield.activate();
		}

		//四阶E.G.O「渴望」（Thirst）装备被动（2026-09-06）：装备该武器时，英雄受到的流血伤害
		//绕过护盾直接扣除血量、不会因流血致死（HP 保底 1）；且每次流血全额转化为奥术屏障（Barrier）护盾。
		//例：剩余 3 血时受到 20 流血伤害 → 血量剩余 1，同时获得 20 奥术屏障。
		int shielded;
		if (src instanceof Bleeding && this == Dungeon.hero && buff(Thirst.ThirstPact.class) != null){
			int total = dmg;
			int hpLoss = Math.min(total, Math.max(0, HP - 1));
			HP -= hpLoss;
			dmg = hpLoss;                    //dmg 在此之后代表实际扣血（供后续死亡/图标等逻辑使用）
			shielded = total - hpLoss;
			//2026-09-20「血宴圣杯」联动：这里改挂 ThirstBloodBarrier（Barrier 的子类）。
			//它比 Barrier 只多一件事 —— 每次衰减都会通知血宴圣杯，装备圣杯时按衰减量的 1/5 回血；
			//未装备时行为与 Barrier 逐字节一致（见该类 act()），所以不需要在这里分两条路。
			//注意：Char.buff(Class) 是**精确类匹配**（getClass()==），英雄同时持有普通 Barrier 时
			//两者会各占一份、buff 栏会画两个 ARMOR 图标 —— 这是**已知且有意的取舍**，
			//详见 ThirstBloodBarrier 类注释「已知代价」一节，修复前先读它。
			ThirstBloodBarrier barrier = Buff.affect(this, ThirstBloodBarrier.class);
			barrier.incShield(total);
		} else {
			shielded = dmg;
			dmg = ShieldBuff.processDamage(this, dmg, src);
			shielded -= dmg;
			HP -= dmg;
		}

		//环指大师 T3「过时的艺术」：按实际扣到 HP 的伤害累计损失（护盾吸收部分不计），用于按损失 HP 累积灵感
		if (dmg > 0 && this instanceof Hero){
			Talent.onHeroHPLost((Hero)this, dmg);
		}

		if (HP > 0 && buff(Grim.GrimTracker.class) != null){

			float finalChance = buff(Grim.GrimTracker.class).maxChance;
			finalChance *= (float)Math.pow( ((HT - HP) / (float)HT), 2);

			if (Random.Float() < finalChance) {
				int extraDmg = Math.round(HP*resist(Grim.class));
				dmg += extraDmg;
				HP -= extraDmg;

				sprite.emitter().burst( ShadowParticle.UP, 5 );
				if (!isAlive() && buff(Grim.GrimTracker.class).qualifiesForBadge){
					Badges.validateGrimWeapon();
				}
			}
		}

		if (HP < 0 && src instanceof Char && alignment == Alignment.ENEMY){
			if (((Char) src).buff(Kinetic.KineticTracker.class) != null){
				int dmgToAdd = -HP;
				dmgToAdd -= ((Char) src).buff(Kinetic.KineticTracker.class).conservedDamage;
				dmgToAdd = Math.round(dmgToAdd * Weapon.Enchantment.genericProcChanceMultiplier((Char) src));
				if (dmgToAdd > 0) {
					Buff.affect((Char) src, Kinetic.ConservedDamage.class).setBonus(dmgToAdd);
				}
				((Char) src).buff(Kinetic.KineticTracker.class).detach();
			}
		}
		
		if (sprite != null) {
			//defaults to normal damage icon if no other ones apply
			int                                                         icon = FloatingText.PHYS_DMG;
			if (NO_ARMOR_PHYSICAL_SOURCES.contains(src.getClass()))     icon = FloatingText.PHYS_DMG_NO_BLOCK;
			if (AntiMagic.RESISTS.contains(src.getClass()))             icon = FloatingText.MAGIC_DMG;
			if (src instanceof Pickaxe)                                 icon = FloatingText.PICK_DMG;

			//薄暝的火焰伤害：显示火焰图标
			if (src instanceof DimDusk.FlameStrike)                     icon = FloatingText.BURNING;

			//莱瓦汀系列的附加火焰伤害：同样显示火焰图标。
			//本系列的附加火伤是以「武器本身」为 src 打出去的（见 SealedSwordBase#proc），
			//而主伤害的 src 是英雄本人 ⇒ 两条浮字各自结算，只有这一条会落到默认的近战图标上，
			//所以必须单独认一类武器，否则附加火伤会顶着「物理伤害」的小图标。
			if (src instanceof SealedSwordBase)                         icon = FloatingText.BURNING;

			//圣宣的魔法伤害：本次命中显示魔法伤害图标
			if (HolyDecree.magicHit && src instanceof Hero)             icon = FloatingText.MAGIC_DMG;

			//special case for sniper when using ranged attacks
			if (src == Dungeon.hero
					&& Dungeon.hero.subClass == HeroSubClass.SNIPER
					&& !Dungeon.level.adjacent(Dungeon.hero.pos, pos)
					&& Dungeon.hero.belongings.attackingWeapon() instanceof MissileWeapon){
				icon = FloatingText.PHYS_DMG_NO_BLOCK;
			}

			//special case for monk using unarmed abilities
			if (src == Dungeon.hero
					&& Dungeon.hero.buff(MonkEnergy.MonkAbility.UnarmedAbilityTracker.class) != null){
				icon = FloatingText.PHYS_DMG_NO_BLOCK;
			}

			if (src instanceof Hunger)                                  icon = FloatingText.HUNGER;
			if (src instanceof Burning)                                 icon = FloatingText.BURNING;
			if (src instanceof Chill || src instanceof Frost)           icon = FloatingText.FROST;
			if (src instanceof GeyserTrap || src instanceof StormCloud) icon = FloatingText.WATER;
			if (src instanceof Burning)                                 icon = FloatingText.BURNING;
			if (src instanceof Electricity)                             icon = FloatingText.SHOCKING;
			if (src instanceof Bleeding)                                icon = FloatingText.BLEEDING;
			if (src instanceof ToxicGas)                                icon = FloatingText.TOXIC;
			if (src instanceof Corrosion)                               icon = FloatingText.CORROSION;
			if (src instanceof Poison)                                  icon = FloatingText.POISON;
			if (src instanceof Ooze)                                    icon = FloatingText.OOZE;
			if (src instanceof Viscosity.DeferedDamage)                 icon = FloatingText.DEFERRED;
			if (src instanceof Corruption)                              icon = FloatingText.CORRUPTION;
			if (src instanceof AscensionChallenge)                      icon = FloatingText.AMULET;

			if ((icon == FloatingText.PHYS_DMG || icon == FloatingText.PHYS_DMG_NO_BLOCK) && hitMissIcon != -1){
				if (icon == FloatingText.PHYS_DMG_NO_BLOCK) hitMissIcon += 18; //extra row
				icon = hitMissIcon;
			}
			hitMissIcon = -1;

			sprite.showStatusWithIcon(CharSprite.NEGATIVE, Integer.toString(dmg + shielded), icon);
		}

		//「依旧果冻人」挑战（2026-09-15）：受击时以脚底为基准左右扭曲摆动（纯视觉，不改数值/机制）。
		//放在这里是因为它是全工程伤害的汇聚点，英雄/友方/敌人/中立一视同仁；内部再筛「直接命中」，
		//把燃烧/流血/中毒这类每回合跳一次的持续伤害排除在外（否则会一直抖）。详见 effects.JellyWobble
		JellyWobble.hit( this, src );

		if (HP < 0) HP = 0;

		//中指长兄 盔甲技能「咬紧牙关」：免死判定的「击穿」检查。
		//必须放在这一行（伤害已打进血量、HP 已归零）与下面那条死亡判定之间——此时才能判出
		//「这一击是否致命」，也才来得及在 isAlive() 被问起之前把免死作废。
		//能击穿的有两类：解离射线，以及血祭/割腕这类「自伤换成长」（SelfHarmCost）——
		//后者是升级代价，被免死兜住就等于无条件刷等级。判据与说明都收在 GritTeethBuff 里（唯一取点）。
		GritTeethBuff.checkBypass( this, src );

		//GEBURA（严厉）：致死闸门 —— 敌方单位「即将死亡」时改为进入一轮濒死无敌，计时结束才真正倒下。
		//位置就钉在上面那段注释说的区间里（伤害已打进血量、HP 已归零，但还没宣布死亡）。
		//「靠后」的两点收益与避开的坑见 Trials.interceptLethalDamage 的 Javadoc：
		//① 子类 die()（含剧情善后/掉落）此刻还没跑 ⇒ 全程只跑一次、且跑在真正该死的那一刻；
		//② 原版战续都自动处理得当，不用白名单：Ghoul 在 super.die() 前 return（靠 deathIsDeferred 让路）；
		//   Brute 战续期间撑在 isAlive() 上、耗尽的战续结束另在 Brute.BruteRage.act() 里接了同一闸门。
		//返回值这里刻意不接：紧接着的 if (!isAlive()) 本来就是原版的落地语句，闸门接管时 isAlive() 为真。
		Trials.interceptLethalDamage( this, src );

		if (!isAlive()) {
			die( src );
		} else if (HP == 0 && buff(DeathMark.DeathMarkTracker.class) != null){
			DeathMark.processFearTheReaper(this);
		}
	}

	//these are misc. sources of physical damage which do not apply armor, they get a different icon
	private static HashSet<Class> NO_ARMOR_PHYSICAL_SOURCES = new HashSet<>();
	{
		NO_ARMOR_PHYSICAL_SOURCES.add(CrystalSpire.SpireSpike.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(GnollGeomancer.Boulder.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(GnollGeomancer.GnollRockFall.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(GnollRockfallTrap.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(DwarfKing.KingDamager.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(DwarfKing.Summoning.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(LifeLink.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(Chasm.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(WandOfBlastWave.Knockback.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(Heap.class); //damage from wraiths attempting to spawn from heaps
		NO_ARMOR_PHYSICAL_SOURCES.add(Necromancer.SummoningBlockDamage.class);
		NO_ARMOR_PHYSICAL_SOURCES.add(DriedRose.GhostHero.NoRoseDamage.class);
	}
	
	public void destroy() {
		HP = 0;
		Actor.remove( this );

		for (Char ch : Actor.chars().toArray(new Char[0])){
			if (ch.buff(Charm.class) != null && ch.buff(Charm.class).object == id()){
				ch.buff(Charm.class).detach();
			}
			if (ch.buff(Dread.class) != null && ch.buff(Dread.class).object == id()){
				ch.buff(Dread.class).detach();
			}
			if (ch.buff(Terror.class) != null && ch.buff(Terror.class).object == id()){
				ch.buff(Terror.class).detach();
			}
			if (ch.buff(SnipersMark.class) != null && ch.buff(SnipersMark.class).object == id()){
				ch.buff(SnipersMark.class).detach();
			}
			if (ch.buff(Talent.FollowupStrikeTracker.class) != null
					&& ch.buff(Talent.FollowupStrikeTracker.class).object == id()){
				ch.buff(Talent.FollowupStrikeTracker.class).detach();
			}
			if (ch.buff(Talent.DeadlyFollowupTracker.class) != null
					&& ch.buff(Talent.DeadlyFollowupTracker.class).object == id()){
				ch.buff(Talent.DeadlyFollowupTracker.class).detach();
			}
		}
	}
	
	public void die( Object src ) {
		destroy();
		if (src != Chasm.class) {
			//「依旧果冻人」挑战：死亡动画也形变（纯视觉，不改任何数值/机制）。
			//放在 sprite.die() 之前——die 动画里可能 killAndErase 把精灵摘走，之后就晚了。
			JellyWobble.death( this, src );
			sprite.die();
			if (!flying && Dungeon.level != null && sprite instanceof MobSprite && Dungeon.level.map[pos] == Terrain.CHASM){
				((MobSprite) sprite).fall();
			}
		}
	}

	//we cache this info to prevent having to call buff(...) in isAlive.
	//This is relevant because we call isAlive during drawing, which has both performance
	//and thread coordination implications
	public boolean deathMarked = false;
	
	public boolean isAlive() {
		return HP > 0 || deathMarked;
	}

	public boolean isActive() {
		return isAlive();
	}

	@Override
	protected void spendConstant(float time) {
		TimekeepersHourglass.timeFreeze freeze = buff(TimekeepersHourglass.timeFreeze.class);
		if (freeze != null) {
			freeze.processTime(time);
			return;
		}

		Swiftthistle.TimeBubble bubble = buff(Swiftthistle.TimeBubble.class);
		if (bubble != null){
			bubble.processTime(time);
			return;
		}

		super.spendConstant(time);
	}

	@Override
	protected void spend( float time ) {

		float timeScale = 1f;
		if (buff( Slow.class ) != null) {
			timeScale *= 0.5f;
			//slowed and chilled do not stack
		} else if (buff( Chill.class ) != null) {
			timeScale *= buff( Chill.class ).speedFactor();
		}
		if (buff( Speed.class ) != null) {
			timeScale *= 2.0f;
		}
		
		super.spend( time / timeScale );
	}
	
	public synchronized LinkedHashSet<Buff> buffs() {
		return new LinkedHashSet<>(buffs);
	}
	
	@SuppressWarnings("unchecked")
	//returns all buffs assignable from the given buff class
	public synchronized <T extends Buff> HashSet<T> buffs( Class<T> c ) {
		HashSet<T> filtered = new HashSet<>();
		for (Buff b : buffs) {
			if (c.isInstance( b )) {
				filtered.add( (T)b );
			}
		}
		return filtered;
	}

	@SuppressWarnings("unchecked")
	//returns an instance of the specific buff class, if it exists. Not just assignable
	public synchronized  <T extends Buff> T buff( Class<T> c ) {
		for (Buff b : buffs) {
			if (b.getClass() == c) {
				return (T)b;
			}
		}
		return null;
	}

	public synchronized boolean isCharmedBy( Char ch ) {
		int chID = ch.id();
		for (Buff b : buffs) {
			if (b instanceof Charm && ((Charm)b).object == chID) {
				return true;
			}
		}
		return false;
	}

	public synchronized boolean add( Buff buff ) {

		if (buff(PotionOfCleansing.Cleanse.class) != null) { //cleansing buff
			if (buff.type == Buff.buffType.NEGATIVE
					&& !(buff instanceof AllyBuff)
					&& !(buff instanceof LostInventory)){
				return false;
			}
		}

		if (sprite != null && buff(Challenge.SpectatorFreeze.class) != null){
			return false; //can't add buffs while frozen and game is loaded
		}

		buffs.add( buff );
		if (Actor.chars().contains(this)) Actor.add( buff );

		if (sprite != null && buff.announced) {
			switch (buff.type) {
				case POSITIVE:
					sprite.showStatus(CharSprite.POSITIVE, Messages.titleCase(buff.name()));
					break;
				case NEGATIVE:
					sprite.showStatus(CharSprite.WARNING, Messages.titleCase(buff.name()));
					break;
				case NEUTRAL:
				default:
					sprite.showStatus(CharSprite.NEUTRAL, Messages.titleCase(buff.name()));
					break;
			}
		}

		return true;

	}
	
	public synchronized boolean remove( Buff buff ) {
		
		buffs.remove( buff );
		Actor.remove( buff );

		return true;
	}
	
	public synchronized void remove( Class<? extends Buff> buffClass ) {
		for (Buff buff : buffs( buffClass )) {
			remove( buff );
		}
	}
	
	@Override
	protected synchronized void onRemove() {
		for (Buff buff : buffs.toArray(new Buff[buffs.size()])) {
			buff.detach();
		}
	}
	
	public synchronized void updateSpriteState() {
		for (Buff buff:buffs) {
			buff.fx( true );
		}
	}
	
	public float stealth() {
		float stealth = 0;

		stealth += Obfuscation.stealthBoost(this, glyphLevel(Obfuscation.class));

		return stealth;
	}

	public final void move( int step ) {
		move( step, true );
	}

	//travelling may be false when a character is moving instantaneously, such as via teleportation
	public void move( int step, boolean travelling ) {

		if (travelling && Dungeon.level.adjacent( step, pos ) && buff( Vertigo.class ) != null) {
			sprite.interruptMotion();
			int newPos = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			if (!(Dungeon.level.passable[newPos] || Dungeon.level.avoid[newPos])
					|| (properties().contains(Property.LARGE) && !Dungeon.level.openSpace[newPos])
					|| Actor.findChar( newPos ) != null)
				return;
			else {
				sprite.move(pos, newPos);
				step = newPos;
			}
		}

		if (Dungeon.level.map[pos] == Terrain.OPEN_DOOR) {
			Door.leave( pos );
		}

		pos = step;
		
		if (this != Dungeon.hero) {
			sprite.visible = Dungeon.level.heroFOV[pos];
		}

		//芝诺的龟螺旋：目标移动时，若"中矢"中含有该武器则累积流血伤害（瞬移/强制位移不触发）
		if (travelling){
			com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.ZenosTurtleSpiral.onMove( this );
		}
		
		Dungeon.level.occupyCell(this );
	}
	
	public int distance( Char other ) {
		return Dungeon.level.distance( pos, other.pos );
	}

	public boolean[] modifyPassable( boolean[] passable){
		//do nothing by default, but some chars can pass over terrain that others can't
		return passable;
	}
	
	public void onMotionComplete() {
		//Does nothing by default
		//The main actor thread already accounts for motion,
		// so calling next() here isn't necessary (see Actor.process)
	}
	
	public void onAttackComplete() {
		next();
	}
	
	public void onOperateComplete() {
		next();
	}
	
	protected final HashSet<Class> resistances = new HashSet<>();
	
	//returns percent effectiveness after resistances
	//TODO currently resistances reduce effectiveness by a static 50%, and do not stack.
	public float resist( Class effect ){
		HashSet<Class> resists = new HashSet<>(resistances);
		for (Property p : properties()){
			resists.addAll(p.resistances());
		}
		for (Buff b : buffs()){
			resists.addAll(b.resistances());
		}
		
		float result = 1f;
		for (Class c : resists){
			if (c.isAssignableFrom(effect)){
				result *= 0.5f;
			}
		}
		return result * RingOfElements.resist(this, effect);
	}
	
	protected final HashSet<Class> immunities = new HashSet<>();
	
	public boolean isImmune(Class effect ){
		//拇指 前二老板「瞄准心脏」：被标记的单位失去对燃烧与麻痹的免疫（2026-09-08）
		if (buff(AimHeartMark.class) != null
				&& (effect == Burning.class || effect == Paralysis.class
				    || Burning.class.isAssignableFrom(effect)
				    || Paralysis.class.isAssignableFrom(effect))){
			return false;
		}

		HashSet<Class> immunes = new HashSet<>(immunities);
		for (Property p : properties()){
			immunes.addAll(p.immunities());
		}
		for (Buff b : buffs()){
			immunes.addAll(b.immunities());
		}
		if (glyphLevel(Brimstone.class) >= 0){
			immunes.add(Burning.class);
		}
		
		for (Class c : immunes){
			if (c.isAssignableFrom(effect)){
				return true;
			}
		}
		return false;
	}

	//similar to isImmune, but only factors in damage.
	//Is used in AI decision-making
	public boolean isInvulnerable( Class effect ){
		return buff(Challenge.SpectatorFreeze.class) != null || buff(Invulnerability.class) != null;
	}

	protected HashSet<Property> properties = new HashSet<>();

	public HashSet<Property> properties() {
		HashSet<Property> props = new HashSet<>(properties);
		//TODO any more of these and we should make it a property of the buff, like with resistances/immunities
		if (buff(ChampionEnemy.Giant.class) != null) {
			props.add(Property.LARGE);
		}
		return props;
	}

	public enum Property{
		BOSS ( new HashSet<Class>( Arrays.asList(Grim.class, GrimTrap.class, ScrollOfRetribution.class, ScrollOfPsionicBlast.class)),
				new HashSet<Class>( Arrays.asList(AllyBuff.class, Dread.class) )),
		MINIBOSS ( new HashSet<Class>(),
				new HashSet<Class>( Arrays.asList(AllyBuff.class, Dread.class) )),
		BOSS_MINION,
		UNDEAD,
		DEMONIC,
		INORGANIC ( new HashSet<Class>(),
				new HashSet<Class>( Arrays.asList(Bleeding.class, ToxicGas.class, Poison.class) )),
		FIERY ( new HashSet<Class>( Arrays.asList(WandOfFireblast.class, Elemental.FireElemental.class)),
				new HashSet<Class>( Arrays.asList(Burning.class, Blazing.class))),
		ICY ( new HashSet<Class>( Arrays.asList(WandOfFrost.class, Elemental.FrostElemental.class)),
				new HashSet<Class>( Arrays.asList(Frost.class, Chill.class))),
		ACIDIC ( new HashSet<Class>( Arrays.asList(Corrosion.class)),
				new HashSet<Class>( Arrays.asList(Ooze.class))),
		ELECTRIC ( new HashSet<Class>( Arrays.asList(WandOfLightning.class, Shocking.class, Potential.class,
										Electricity.class, ShockingDart.class, Elemental.ShockElemental.class )),
				new HashSet<Class>()),
		LARGE,
		IMMOVABLE ( new HashSet<Class>(),
				new HashSet<Class>( Arrays.asList(Vertigo.class) )),
		//A character that acts in an unchanging manner. immune to AI state debuffs or stuns/slows
		STATIC( new HashSet<Class>(),
				new HashSet<Class>( Arrays.asList(AllyBuff.class, Dread.class, Terror.class, Amok.class, Charm.class, Sleep.class,
									Paralysis.class, Frost.class, Chill.class, Slow.class, Speed.class) ));

		private HashSet<Class> resistances;
		private HashSet<Class> immunities;
		
		Property(){
			this(new HashSet<Class>(), new HashSet<Class>());
		}
		
		Property( HashSet<Class> resistances, HashSet<Class> immunities){
			this.resistances = resistances;
			this.immunities = immunities;
		}
		
		public HashSet<Class> resistances(){
			return new HashSet<>(resistances);
		}
		
		public HashSet<Class> immunities(){
			return new HashSet<>(immunities);
		}

	}

	public static boolean hasProp( Char ch, Property p){
		return (ch != null && ch.properties().contains(p));
	}
}
