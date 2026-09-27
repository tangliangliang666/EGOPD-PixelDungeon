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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Badges;
import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Electricity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Adrenaline;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AllyBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Amok;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AimHeartMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ChampionEnemy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Charm;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Corruption;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GreaterHaste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Instruction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MindVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MonkEnergy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Preparation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ResinCoatingBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Sleep;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.SoulMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VengeanceArts;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.cleric.PowerOfMany;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.duelist.Feint;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster.RingMasterVoice;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.ShadowClone;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.GuidingLight;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.Stasis;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.DirectableAlly;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Surprise;
import com.shatteredpixel.shatteredpixeldungeon.effects.Wound;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Generator;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.MasterThievesArmband;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TimekeepersHourglass;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.ExoticPotion;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.Ring;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfWealth;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.exotic.ExoticScroll;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfAggression;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ExoticCrystals;
import com.shatteredpixel.shatteredpixeldungeon.items.trinkets.ShardOfOblivion;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.SpiritBow;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Lucky;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DimDusk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.NarcissusCrossSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SmilingBlade;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.TinyGalaxy;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.VoidDiffraction;
import com.shatteredpixel.shatteredpixeldungeon.items.RingMasterLoot;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.journal.Notes;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

public abstract class Mob extends Char {

	{
		actPriority = MOB_PRIO;
		
		alignment = Alignment.ENEMY;
	}

	public AiState SLEEPING     = new Sleeping();
	public AiState HUNTING		= new Hunting();
	public AiState INVESTIGATING= new Investigating();
	public AiState WANDERING	= new Wandering();
	public AiState FLEEING		= new Fleeing();
	public AiState PASSIVE		= new Passive();
	public AiState state = SLEEPING;
	
	public Class<? extends CharSprite> spriteClass;
	
	protected int target = -1;
	
	public int defenseSkill = 0;
	
	public int EXP = 1;
	public int maxLvl = Hero.MAX_LEVEL-1;
	
	protected Char enemy;
	protected int enemyID = -1; //used for save/restore
	protected boolean enemySeen;
	protected boolean alerted = false;

	protected static final float TIME_TO_WAKE_UP = 1f;

	protected boolean firstAdded = true;
	protected void onAdd(){
		if (firstAdded) {
			//modify health for ascension challenge if applicable, only on first add
			float percent = HP / (float) HT;
			HT = Math.round(HT * AscensionChallenge.statModifier(this));
			//CHESED（慈悲）：敌方生命上限 +25%。夹在这里是因为本方法就是「首次入场」的唯一收口，
			//且必须在 HP 按 percent 回算**之前** —— 怪物才会以满血入场。存档里存的是算完的 HT，
			//restoreFromBundle 又已把 firstAdded 置 false ⇒ 读档不会二次放大。
			HT = Trials.modifyMobHT( this, HT );
			HP = Math.round(HT * percent);
			firstAdded = false;
		}

		//CHESED 回血计时器：幂等重挂（首次入场 / 读档 / 尸体群倒地后复活 / 反复入场都覆盖得到）。
		//必须放在 firstAdded 守卫**之外** —— 读档时 firstAdded 已是 false，放里面会漏挂。
		Trials.bindMobPassives( this );
	}

	//GEBURA（严厉）用到的三个瞬态标记 —— 刻意**不进存档**：
	//  · geburaGrace 是「当前处于濒死无敌」的**缓存**，供 isAlive()/isInvulnerable() 高频读取。
	//    原版在 Char.deathMarked 上注明过：isAlive() 会在绘制期被调用，不能在里面调 buff(...)
	//    （性能 + 线程约束），所以这里缓存成布尔；真值由 GeburaGrace 的 attachTo/detach 维护，
	//    于是读档时会随 buff 的恢复自动重建，不需要单独存。
	//  · geburaUsed 是「豁免已用掉」的一次性门闩。它不需要存档：用掉豁免的结局一定是
	//    「计时结束后真正死亡」，而死掉的单位不会被写进存档。
	//  · geburaStagger 是「锁血那一击的僵直」一次性标记（2026-09-24 削弱）：刚被打成濒死无敌的
	//    那一回合它**不行动**，整整愣过一个回合之后才照常行动。由 act() 在**下一次行动机会**上
	//    经 consumeGeburaStagger() 消费（消费即清），所以每只怪最多只会僵直一次。
	//    不必存档：它的生命周期是「同一帧内挂上、下一回合消费掉」，玩家没有能存盘的中间态；
	//    真读档（多回合之后）时那一次僵直早已兑现，留着反而会平白多愣一回合。
	public boolean geburaGrace = false;
	public boolean geburaUsed = false;
	public boolean geburaStagger = false;

	//GEBURA 掉落修复（2026-09-24）：**掉落判定的凭据**也要跟着「致死那一击」走，不能等到真正倒下才去读。
	//原版里「额外掉落」的两处判定都发生在 `Mob.die()` 的同一个调用栈内（攻击 → 伤害 → die），
	//所以它们读到的都是「致死那一击那一刻」的状态；而本考验把真正倒下推迟了 EXP 个回合
	//（由 GeburaGrace.act() 在若干回合后调 die()），推迟到的那一刻状态早已变了：
	//  · geburaLuckyProc —— 幸运附魔的 `LuckProc`。它是**只活到下一个 buff tick 的瞬态标记**
	//    （`LuckProc.act()` 里第一句就是 detach），vanilla 靠「同一调用栈」才读得到；
	//    推迟若干回合后它必然不在 ⇒ `Mob.die` 的 `luckyProc` 参数与 `rollToDropLoot` 的幸运块
	//    双双落空（幸运附魔的额外掉落、环指大师素材的「幸运再判一次」都失效）。
	//    闸门在锁血那一刻把这个**实例**抄下来（掉落强度等信息一并留存），真正倒下时优先用 live 的、
	//    取不到才回落它。
	//  · geburaWealthBonus —— 财富戒指的等级（`Ring.getBuffedBonus(hero, Wealth.class)`）。
	//    原版同一次调用栈内它与判定同刻；推迟之后英雄侧的凭据可能已经变了（复活/卸下等），
	//    于是同样把「致死那一刻的等级」抄下来作回落（live 优先，取不到才回落）。
	//两者都**不进存档**：豁免用掉的结局一定是「计时结束后真死」，而死者不会被写进存档。
	public Lucky.LuckProc geburaLuckyProc = null;
	public int geburaWealthBonus = 0;

	//「带着标记倒下」的击杀判定（2026-09-26）：拇指「荣耀凯旋」（AimHeartMark）与食指「击杀指令目标」
	//（InstructionTarget）原先都要求「英雄这一击把它打死了」——前者读 `!enemy.isAlive()`，
	//后者挂在 die() 的「英雄击杀」分支里。GEBURA 锁血把死亡推迟了 EXP 个回合，这两条判据都会落空：
	//  · AimHeartMark 是 10 回合的限时标记，锁血期间可能已经过期；
	//  · 攻击调用栈在锁血那一刻就退栈了，等真正倒下时早没有「这一击」的上下文。
	//改判据＝**死亡时身上还带着对应 buff**（与「谁打死的」解耦，见 Mob.die 的收口点）。
	//锁血那一刻先在这里抄一份携带状态，真正倒下时 **live 优先、取不到才回落** —— 与 geburaLuckyProc
	//同一套写法（原版情形下 live 必然存在，行为逐字不变）。同样不进存档：死者不会被写进存档。
	public boolean geburaAimMarked = false;
	public boolean geburaInstrTarget = false;

	private static final String STATE	= "state";
	private static final String SEEN	= "seen";
	private static final String TARGET	= "target";
	private static final String MAX_LVL	= "max_lvl";

	private static final String ENEMY_ID	= "enemy_id";
	
	@Override
	public void storeInBundle( Bundle bundle ) {
		
		super.storeInBundle( bundle );

		if (state == SLEEPING) {
			bundle.put( STATE, Sleeping.TAG );
		} else if (state == WANDERING) {
			bundle.put( STATE, Wandering.TAG );
		} else if (state == INVESTIGATING) {
			bundle.put( STATE, Investigating.TAG );
		} else if (state == HUNTING) {
			bundle.put( STATE, Hunting.TAG );
		} else if (state == FLEEING) {
			bundle.put( STATE, Fleeing.TAG );
		} else if (state == PASSIVE) {
			bundle.put( STATE, Passive.TAG );
		}
		bundle.put( SEEN, enemySeen );
		bundle.put( TARGET, target );
		bundle.put( MAX_LVL, maxLvl );

		if (enemy != null) {
			bundle.put(ENEMY_ID, enemy.id() );
		}
	}
	
	@Override
	public void restoreFromBundle( Bundle bundle ) {
		
		super.restoreFromBundle( bundle );

		String state = bundle.getString( STATE );
		if (state.equals( Sleeping.TAG )) {
			this.state = SLEEPING;
		} else if (state.equals( Wandering.TAG )) {
			this.state = WANDERING;
		} else if (state.equals( Investigating.TAG )) {
			this.state = INVESTIGATING;
		} else if (state.equals( Hunting.TAG )) {
			this.state = HUNTING;
		} else if (state.equals( Fleeing.TAG )) {
			this.state = FLEEING;
		} else if (state.equals( Passive.TAG )) {
			this.state = PASSIVE;
		}

		enemySeen = bundle.getBoolean( SEEN );

		target = bundle.getInt( TARGET );

		if (bundle.contains(MAX_LVL)) maxLvl = bundle.getInt(MAX_LVL);

		if (bundle.contains(ENEMY_ID)) {
			enemyID = bundle.getInt(ENEMY_ID);
		}

		//no need to actually save this, must be false
		firstAdded = false;
	}

	//mobs need to remember their targets after every actor is added
	public void restoreEnemy(){
		if (enemyID != -1 && enemy == null) enemy = (Char)Actor.findById(enemyID);
	}
	
	public CharSprite sprite() {
		return Reflection.newInstance(spriteClass);
	}
	
	@Override
	protected boolean act() {
		
		super.act();
		
		boolean justAlerted = alerted;
		alerted = false;
		
		if (justAlerted){
			sprite.showAlert();
		} else {
			sprite.hideAlert();
			sprite.hideLost();
			sprite.hideInvestigate();
		}
		
		if (paralysed > 0) {
			enemySeen = false;
			spend( TICK );
			return true;
		}

		//GEBURA（严厉）：「锁血」那一击的僵直 —— 刚被打成濒死无敌的这一回合**不行动**，
		//愣过一个回合之后才照常行动（2026-09-24 削弱；此前它无敌期间是照常出手的）。
		//形态刻意与上面 paralysed 那段一致（花掉一个 TICK 后直接 return），差别只有两点：
		//  · 标记是**一次性**的：由 consumeGeburaStagger() 消费，所以每只怪最多只愣一回合；
		//  · 不动 enemySeen —— 它只是愣住了，不是失去目标/失去视野（paralysed 那句 false 是瘫痪自己的语义）。
		//放在 paralysed **之后**：瘫痪期间本来就不能行动，僵直记在账上，等它真能动的那一回合再兑现。
		//⚠️ 必须在 chooseEnemy() / state.act() **之前**：否则「这一回合不行动」形同虚设。
		//⚠️ 作用边界：**自己写 act() 却不在里面调 super.act() 的单位绕过这里**（全仓只有 YogDzewa，
		//   EXP=50；它连 paralysed 都不吃，是上游自带的同类边界）。已与用户确认**按既定边界处理**、
		//   不为它开特判 —— 完整清单与理由钉在 _chk/verify_chesed_gebura.py 的第 ⑥ 层。
		if (consumeGeburaStagger()) {
			spend( TICK );
			return true;
		}

		if (buff(Terror.class) != null || buff(Dread.class) != null ){
			state = FLEEING;
		}
		
		enemy = chooseEnemy();
		
		boolean enemyInFOV = enemy != null && enemy.isAlive() && fieldOfView[enemy.pos] && enemy.invisible <= 0;

		//prevents action, but still updates enemy seen status
		if (buff(Feint.AfterImage.FeintConfusion.class) != null){
			enemySeen = enemyInFOV;
			spend( TICK );
			return true;
		}

		boolean result = state.act( enemyInFOV, justAlerted );

		//for updating hero FOV
		if (buff(PowerOfMany.PowerBuff.class) != null){
			Dungeon.level.updateFieldOfView( this, fieldOfView );
			GameScene.updateFog(pos, viewDistance+(int)Math.ceil(speed()));
		}

		return result;
	}
	
	//FIXME this is sort of a band-aid correction for allies needing more intelligent behaviour
	protected boolean intelligentAlly = false;
	
	protected Char chooseEnemy() {

		Dread dread = buff( Dread.class );
		if (dread != null) {
			Char source = (Char)Actor.findById( dread.object );
			if (source != null) {
				return source;
			}
		}

		Terror terror = buff( Terror.class );
		if (terror != null) {
			Char source = (Char)Actor.findById( terror.object );
			if (source != null) {
				return source;
			}
		}
		
		//if we are an alert enemy, auto-hunt a target that is affected by aggression, even another enemy
		if ((alignment == Alignment.ENEMY || buff(Amok.class) != null ) && state != PASSIVE && state != SLEEPING) {
			if (enemy != null && enemy.buff(StoneOfAggression.Aggression.class) != null){
				state = HUNTING;
				return enemy;
			}
			for (Char ch : Actor.chars()) {
				if (ch != this && fieldOfView[ch.pos] &&
						ch.buff(StoneOfAggression.Aggression.class) != null) {
					state = HUNTING;
					return ch;
				}
			}
		}

		//find a new enemy if..
		boolean newEnemy = false;
		//we have no enemy, or the current one is dead/missing
		if ( enemy == null || !enemy.isAlive() || !Actor.chars().contains(enemy) || state == WANDERING) {
			newEnemy = true;
		//We are amoked and current enemy is the hero
		} else if (buff( Amok.class ) != null && enemy == Dungeon.hero) {
			newEnemy = true;
		//We are charmed and current enemy is what charmed us
		} else if (buff(Charm.class) != null && buff(Charm.class).object == enemy.id()) {
			newEnemy = true;
		}

		//additionally, if we are an ally, find a new enemy if...
		if (!newEnemy && alignment == Alignment.ALLY){
			//current enemy is also an ally
			if (enemy.alignment == Alignment.ALLY){
				newEnemy = true;
			//current enemy is invulnerable
			} else if (enemy.isInvulnerable(getClass())){
				newEnemy = true;
			}
		}

		if ( newEnemy ) {

			HashSet<Char> enemies = new HashSet<>();

			//if we are amoked...
			if ( buff(Amok.class) != null) {
				//try to find an enemy mob to attack first.
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ENEMY && mob != this
							&& fieldOfView[mob.pos] && mob.invisible <= 0) {
						enemies.add(mob);
					}
				
				if (enemies.isEmpty()) {
					//try to find ally mobs to attack second.
					for (Mob mob : Dungeon.level.mobs)
						if (mob.alignment == Alignment.ALLY && mob != this
								&& fieldOfView[mob.pos] && mob.invisible <= 0) {
							enemies.add(mob);
						}
					
					if (enemies.isEmpty()) {
						//try to find the hero third
						if (fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0) {
							enemies.add(Dungeon.hero);
						}
					}
				}
				
			//if we are an ally...
			} else if ( alignment == Alignment.ALLY ) {
				//look for hostile mobs to attack
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ENEMY && fieldOfView[mob.pos]
							&& mob.invisible <= 0 && !mob.isInvulnerable(getClass()))
						//do not target passive mobs
						//intelligent allies also don't target mobs which are wandering or asleep
						if (mob.state != mob.PASSIVE &&
								(!intelligentAlly || (mob.state != mob.SLEEPING && mob.state != mob.WANDERING))) {
							enemies.add(mob);
						}
				
			//if we are an enemy...
			} else if (alignment == Alignment.ENEMY) {
				//look for ally mobs to attack
				for (Mob mob : Dungeon.level.mobs)
					if (mob.alignment == Alignment.ALLY && fieldOfView[mob.pos] && mob.invisible <= 0)
						enemies.add(mob);

				//and look for the hero
				if (fieldOfView[Dungeon.hero.pos] && Dungeon.hero.invisible <= 0) {
					enemies.add(Dungeon.hero);
				}
				
			}

			//do not target anything that's charming us
			Charm charm = buff( Charm.class );
			if (charm != null){
				Char source = (Char)Actor.findById( charm.object );
				if (source != null && enemies.contains(source) && enemies.size() > 1){
					enemies.remove(source);
				}
			}

			//neutral characters in particular do not choose enemies.
			if (enemies.isEmpty()){
				return null;
			} else {
				//go after the closest potential enemy, preferring enemies that can be reached/attacked, and the hero if two are equidistant
				PathFinder.buildDistanceMap(pos, Dungeon.findPassable(this, Dungeon.level.passable, fieldOfView, true));
				Char closest = null;
				int closestDist = Integer.MAX_VALUE;

				for (Char curr : enemies){
					int currDist = Integer.MAX_VALUE;
					//we aren't trying to move into the target, just toward them
					for (int i : PathFinder.NEIGHBOURS8){
						if (PathFinder.distance[curr.pos+i] < currDist){
							currDist = PathFinder.distance[curr.pos+i];
						}
					}
					if (closest == null){
						closest = curr;
						closestDist = currDist;
					} else if (canAttack(closest) && !canAttack(curr)){
						continue;
					} else if ((canAttack(curr) && !canAttack(closest))
							|| (currDist < closestDist)){
						closest = curr;
					} else if ( curr == Dungeon.hero &&
							(currDist == closestDist) || (canAttack(curr) && canAttack(closest))){
						closest = curr;
					}
				}
				//if we were going to target the hero, but an afterimage exists, target that instead
				if (closest == Dungeon.hero){
					for (Char ch : enemies){
						if (ch instanceof Feint.AfterImage){
							closest = ch;
							break;
						}
					}
				}

				return closest;
			}

		} else
			return enemy;
	}
	
	@Override
	public boolean add( Buff buff ) {
		if (super.add( buff )) {
			if (buff instanceof Amok || buff instanceof AllyBuff) {
				state = HUNTING;
			} else if (buff instanceof Terror || buff instanceof Dread) {
				state = FLEEING;
			} else if (buff instanceof Sleep) {
				state = SLEEPING;
				postpone(Sleep.SWS);
			}
			return true;
		}
		return false;
	}
	
	@Override
	public boolean remove( Buff buff ) {
		if (super.remove( buff )) {
			if (state == FLEEING && ((buff instanceof Terror && buff(Dread.class) == null)
					|| (buff instanceof Dread && buff(Terror.class) == null))) {
				if (enemySeen) {
					sprite.showStatus(CharSprite.WARNING, Messages.get(this, "rage"));
					state = HUNTING;
				} else {
					state = WANDERING;
				}
			}
			return true;
		}
		return false;
	}
	
	protected boolean canAttack( Char enemy ) {
		if (Dungeon.level.adjacent( pos, enemy.pos )){
			return true;
		}
		for (ChampionEnemy buff : buffs(ChampionEnemy.class)){
			if (buff.canAttackWithExtraReach( enemy )){
				return true;
			}
		}
		return false;
	}

	private boolean cellIsPathable( int cell ){
		if (!Dungeon.level.passable[cell]){
			if (flying || buff(Amok.class) != null){
				if (!Dungeon.level.avoid[cell]){
					return false;
				}
			} else {
				return false;
			}
		}
		if (Char.hasProp(this, Char.Property.LARGE) && !Dungeon.level.openSpace[cell]){
			return false;
		}
		if (Actor.findChar(cell) != null){
			return false;
		}

		return true;
	}

	protected boolean getCloser( int target ) {
		
		if (rooted || target == pos || !Dungeon.level.insideMap(target)) {
			return false;
		}

		int step = -1;

		if (Dungeon.level.adjacent( pos, target )) {

			path = null;

			if (cellIsPathable(target)) {
				step = target;
			}

		} else {

			boolean newPath = false;
			float longFactor = state == WANDERING ? 2f : 1.33f;
			//scrap the current path if it's empty, no longer connects to the current location
			//or if it's quite inefficient and checking again may result in a much better path
			//mobs are much more tolerant of inefficient paths if wandering
			if (path == null || path.isEmpty()
					|| !Dungeon.level.adjacent(pos, path.getFirst())
					|| path.size() > longFactor*Dungeon.level.distance(pos, target))
				newPath = true;
			else if (path.getLast() != target) {
				//if the new target is adjacent to the end of the path, adjust for that
				//rather than scrapping the whole path.
				if (Dungeon.level.adjacent(target, path.getLast())) {
					int last = path.removeLast();

					if (path.isEmpty()) {

						//shorten for a closer one
						if (Dungeon.level.adjacent(target, pos)) {
							path.add(target);
						//extend the path for a further target
						} else {
							path.add(last);
							path.add(target);
						}

					} else {
						//if the new target is simply 1 earlier in the path shorten the path
						if (path.getLast() == target) {

						//if the new target is closer/same, need to modify end of path
						} else if (Dungeon.level.adjacent(target, path.getLast())) {
							path.add(target);

						//if the new target is further away, need to extend the path
						} else {
							path.add(last);
							path.add(target);
						}
					}

				} else {
					newPath = true;
				}

			}

			//checks if the next cell along the current path can be stepped into
			if (!newPath) {
				int nextCell = path.removeFirst();
				if (!cellIsPathable(nextCell)) {

					newPath = true;
					//If the next cell on the path can't be moved into, see if there is another cell that could replace it
					if (!path.isEmpty()) {
						for (int i : PathFinder.NEIGHBOURS8) {
							if (Dungeon.level.adjacent(pos, nextCell + i) && Dungeon.level.adjacent(nextCell + i, path.getFirst())) {
								if (cellIsPathable(nextCell+i)){
									path.addFirst(nextCell+i);
									newPath = false;
									break;
								}
							}
						}
					}
				} else {
					path.addFirst(nextCell);
				}
			}

			//generate a new path
			if (newPath) {
				//If we aren't hunting, always take a full path
				PathFinder.Path full = Dungeon.findPath(this, target, Dungeon.level.passable, fieldOfView, true);
				if (state != HUNTING){
					path = full;
				} else {
					//otherwise, check if other characters are forcing us to take a very slow route
					// and don't try to go around them yet in response, basically assume their blockage is temporary
					PathFinder.Path ignoreChars = Dungeon.findPath(this, target, Dungeon.level.passable, fieldOfView, false);
					if (ignoreChars != null && (full == null || full.size() > 2*ignoreChars.size())){
						//check if first cell of shorter path is valid. If it is, use new shorter path. Otherwise do nothing and wait.
						path = ignoreChars;
						if (!cellIsPathable(ignoreChars.getFirst())) {
							return false;
						}
					} else {
						path = full;
					}
				}
			}

			if (path != null) {
				step = path.removeFirst();
			} else {
				return false;
			}
		}
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}
	
	protected boolean getFurther( int target ) {
		if (rooted || target == pos) {
			return false;
		}
		
		int step = Dungeon.flee( this, target, Dungeon.level.passable, fieldOfView, true );
		if (step != -1) {
			move( step );
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void updateSpriteState() {
		super.updateSpriteState();
		if (Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class) != null
				|| Dungeon.hero.buff(Swiftthistle.TimeBubble.class) != null)
			sprite.add( CharSprite.State.PARALYSED );
	}
	
	public float attackDelay() {
		float delay = 1f;
		if ( buff(Adrenaline.class) != null) delay /= 1.5f;
		return delay;
	}
	
	protected boolean doAttack( Char enemy ) {
		
		if (sprite != null && (sprite.visible || enemy.sprite.visible)) {
			sprite.attack( enemy.pos );
			return false;
			
		} else {
			attack( enemy );
			Invisibility.dispel(this);
			spend( attackDelay() );
			return true;
		}
	}
	
	@Override
	public void onAttackComplete() {
		attack( enemy );
		Invisibility.dispel(this);
		spend( attackDelay() );
		super.onAttackComplete();
	}
	
	@Override
	public int defenseSkill( Char enemy ) {
		if (buff(GuidingLight.Illuminated.class) != null && Dungeon.hero.heroClass == HeroClass.CLERIC){
			//if the attacker is the cleric, they must be using a weapon they have the str for
			if (enemy instanceof Hero){
				Hero h = (Hero) enemy;
				if (!(h.belongings.attackingWeapon() instanceof Weapon)
						|| ((Weapon) h.belongings.attackingWeapon()).STRReq() <= h.STR()){
					return 0;
				}
			} else {
				return 0;
			}
		}

		if ( !surprisedBy(enemy)
				&& paralysed == 0
				&& !(alignment == Alignment.ALLY && enemy == Dungeon.hero)) {
			return this.defenseSkill;
		} else {
			return 0;
		}
	}
	
	@Override
	public int defenseProc( Char enemy, int damage ) {
		
		if (enemy instanceof Hero
				&& ((Hero) enemy).belongings.attackingWeapon() instanceof MissileWeapon){
			Statistics.thrownAttacks++;
			Badges.validateHuntressUnlock();
		}
		
		if (surprisedBy(enemy)) {
			Statistics.sneakAttacks++;
			Badges.validateRogueUnlock();
			//TODO this is somewhat messy, it would be nicer to not have to manually handle delays here
			// playing the strong hit sound might work best as another property of weapon?
			if (Dungeon.hero.belongings.attackingWeapon() instanceof SpiritBow.SpiritArrow
				|| Dungeon.hero.belongings.attackingWeapon() instanceof Dart){
				Sample.INSTANCE.playDelayed(Assets.Sounds.HIT_STRONG, 0.125f);
			} else {
				Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
			}
			if (enemy.buff(Preparation.class) != null) {
				Wound.hit(this);
			} else {
				Surprise.hit(this);
			}
		}

		//if attacked by something else than current target, and that thing is closer, switch targets
		//or if attacked by target, simply update target position
		if (state != FLEEING) {
			if (state != HUNTING) {
				aggro(enemy);
				target = enemy.pos;
			} else {
				recentlyAttackedBy.add(enemy);
			}
		}

		if (buff(SoulMark.class) != null) {
			int restoration = Math.min(damage, HP+shielding());
			
			//physical damage that doesn't come from the hero is less effective
			if (enemy != Dungeon.hero){
				restoration = Math.round(restoration * 0.4f*Dungeon.hero.pointsInTalent(Talent.SOUL_SIPHON)/3f);
			}
			if (restoration > 0) {
				Buff.affect(Dungeon.hero, Hunger.class).affectHunger(restoration*Dungeon.hero.pointsInTalent(Talent.SOUL_EATER)/3f);

				if (Dungeon.hero.HP < Dungeon.hero.HT) {
					int healed = Dungeon.hero.heal( (int)Math.ceil(restoration * 0.4f) );
					if (healed > 0) {
						Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);
					}
				}
			}
		}

		return super.defenseProc(enemy, damage);
	}

	@Override
	public float speedRaw() {
		return super.speedRaw() * AscensionChallenge.enemySpeedModifier(this);
	}

	public final boolean surprisedBy( Char enemy ){
		return surprisedBy( enemy, true);
	}

	public boolean surprisedBy( Char enemy, boolean attacking ){
		return enemy == Dungeon.hero
				&& (enemy.invisible > 0 || !enemySeen || (fieldOfView != null && fieldOfView.length == Dungeon.level.length() && !fieldOfView[enemy.pos])
					//虚无衍射体：即使被敌人发现/看见，攻击也始终被视为偷袭（必中+偷袭结算）
					|| (attacking && ((Hero) enemy).belongings.attackingWeapon() instanceof VoidDiffraction))
				&& (!attacking || enemy.canSurpriseAttack());
	}

	//whether the hero should interact with the mob (true) or attack it (false)
	public boolean heroShouldInteract(){
		return alignment != Alignment.ENEMY && buff(Amok.class) == null;
	}

	public void aggro( Char ch ) {
		enemy = ch;
		if (state != PASSIVE){
			state = HUNTING;
		}
	}

	public void clearEnemy(){
		enemy = null;
		enemySeen = false;
		if (state == HUNTING) state = WANDERING;
	}
	
	public boolean isTargeting( Char ch){
		return enemy == ch;
	}

	@Override
	public void damage( int dmg, Object src ) {

		if (!isInvulnerable(src.getClass())) {
			if (state == SLEEPING) {
				state = WANDERING;
			}
			if (!(src instanceof Corruption) && state != FLEEING) {
				if (state != HUNTING) {
					alerted = true;
					//assume the hero is hitting us in these common cases
					if (src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility) {
						aggro(Dungeon.hero);
						target = Dungeon.hero.pos;
					}
				} else {
					if (src instanceof Wand || src instanceof ClericSpell || src instanceof ArmorAbility) {
						recentlyAttackedBy.add(Dungeon.hero);
					}
				}
			}
		}
		
		super.damage( dmg, src );
	}
	
	
	@Override
	public void destroy() {
		
		super.destroy();
		
		Dungeon.level.mobs.remove( this );

		if (Dungeon.hero.buff(MindVision.class) != null){
			Dungeon.observe();
			GameScene.updateFog(pos, 2);
		}

		if (Dungeon.hero.isAlive()) {
			
			if (alignment == Alignment.ENEMY) {
				Statistics.enemiesSlain++;
				Badges.validateMonstersSlain();
				Statistics.qualifiedForNoKilling = false;
				Bestiary.setSeen(getClass());
				Bestiary.countEncounter(getClass());

				AscensionChallenge.processEnemyKill(this);
				
				int exp = Dungeon.hero.lvl <= maxLvl ? EXP : 0;

				//during ascent, under-levelled enemies grant 10 xp each until level 30
				// after this enemy kills which reduce the amulet curse still grant 10 effective xp
				// for the purposes of on-exp effects, see AscensionChallenge.processEnemyKill
				if (Dungeon.hero.buff(AscensionChallenge.class) != null &&
						exp == 0 && maxLvl > 0 && EXP > 0 && Dungeon.hero.lvl < Hero.MAX_LEVEL){
					exp = Math.round(10 * spawningWeight());
				}

				//水仙十字圣剑「芒性 / 荒性」：这笔经验会被整笔吞掉换成别的收益（护盾 / 祝福），
				//所以不再弹「+经验」浮字——否则玩家会看到经验数字跳出来却没进经验条。
				//替代收益自己会弹对应的浮字（护盾见 NarcissusSwordMang.onExpGained）。
				if (exp > 0 && !NarcissusCrossSwordBase.blocksExpNow(Dungeon.hero)) {
					Dungeon.hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(exp), FloatingText.EXPERIENCE);
				}
				Dungeon.hero.earnExp(exp, getClass());

				if (Dungeon.hero.subClass == HeroSubClass.MONK){
					Buff.affect(Dungeon.hero, MonkEnergy.class).gainEnergy(this);
				}
			}
		}
	}
	
	/**
	 * GEBURA（严厉）：濒死无敌期间仍要「不倒」。
	 * <p>血量按设计停在 0（空血条），所以这里必须再补一份判断，否则 {@code Char.damage} 紧接着的
	 * {@code if (!isAlive())} 会在同一帧立刻补一次 {@code die()}。</p>
	 * <p>读的是 {@link #geburaGrace} 缓存而不是 {@code buff(...)} —— 原版注明 isAlive() 会在绘制期被调用。</p>
	 */
	@Override
	public boolean isAlive() {
		return super.isAlive() || geburaGrace;
	}

	/**
	 * GEBURA（严厉）：濒死无敌期间免疫一切伤害。
	 * <p>覆写它即可，不必新写判定 —— {@code Char.damage} 与 {@code Char.attack} 里原版就有
	 * 「无敌 ⇒ 吃掉伤害并弹 actors.char.invulnerable」的分支，英雄的盟友也会自动不去攻击无敌目标。</p>
	 */
	@Override
	public boolean isInvulnerable( Class effect ){
		return geburaGrace || super.isInvulnerable( effect );
	}

	/**
	 * GEBURA（严厉）：本回合是否要**原地僵直**（＝刚被打成濒死无敌那一下的「愣住」）。
	 *
	 * <p>抽成方法而不是把三行直接写在 {@link #act()} 里，是为了让「一次性」这条语义有**唯一出口**：
	 * 问一次就把 {@link #geburaStagger} 清掉 ⇒ 每只怪最多只僵直一回合，不会变成永久眩晕。
	 * 与隔壁 {@link #deathIsDeferred(Object)} 同属「Mob 上的钩子问题」，区别是它会**消费**状态。</p>
	 */
	public boolean consumeGeburaStagger(){
		if (!geburaStagger) return false;
		geburaStagger = false;
		return true;
	}

	/**
	 * 本次「伤害致死」是否会被该单位**自有的**战续 / 复活机制接管（默认 false ＝ 真死）。
	 * <p>有此类机制的单位覆写它，GEBURA 便让路。判据必须与它 {@code die()} 里的分支**同源**，
	 * 否则两边各判一次、行为会漂移；当前唯一覆写者是 {@code Ghoul}（倒地待复活）。</p>
	 * <p>Brute 那类「靠覆写 {@link #isAlive()} 撑住」的战续**不需要**覆写本方法 ——
	 * 它们的狂暴护盾没耗尽之前根本走不到致死闸门。</p>
	 */
	public boolean deathIsDeferred( Object cause ) {
		return false;
	}

	@Override
	public void die( Object cause ) {

		if (cause == Chasm.class){
			//50% chance to round up, 50% to round down
			if (EXP % 2 == 1) EXP += Random.Int(2);
			EXP /= 2;
		}

		if (alignment == Alignment.ENEMY){
			if (buff(Trap.HazardAssistTracker.class) != null){
				Statistics.hazardAssistedKills++;
				Badges.validateHazardAssists();
			}

			//「带着标记倒下」的击杀判定（2026-09-26）：拇指「荣耀凯旋」与食指「击杀指令目标」。
			//判据＝**这只怪死亡时身上带着对应的 buff**，与「这一击是不是英雄打的」「打完立刻死没死」
			//都无关 —— GEBURA 锁血会把死亡推迟 EXP 回合，那之后原判据必然落空（见两个字段的注释）。
			//⚠️ 必须放在 super.die(cause) 之前：那之后 Actor.remove → onRemove 会把 buff 全摘掉，这里就读不到了。
			//⚠️ 与下面的「英雄击杀」分支**互不重叠**：这两条只跟 buff 走，那边只跟 cause 走。
			boolean aimMarked = buff(AimHeartMark.class) != null || geburaAimMarked;
			boolean instrTarget = buff(InstructionTarget.class) != null || geburaInstrTarget;
			geburaAimMarked = false;	//一次性：抄存的携带状态随这次真实死亡消费掉
			geburaInstrTarget = false;
			if (aimMarked){
				AimHeartMark.onKillByHero( Dungeon.hero );
			}
			if (instrTarget){
				Talent.onOracleBlessing( Dungeon.hero );
				Instruction.onTargetKilled( Dungeon.hero );
			}

			//幸运附魔的 LuckProc 会在 rollToDropLoot 中被消费（detach），先记录下来，
			//供英雄击杀分支的环指大师素材掉落适配（幸运触发时额外掉一次素材）。
			//GEBURA 掉落修复：真正倒下若是被推迟过的（濒死无敌计时结束），LuckProc 早已自行摘掉 ——
			//那时回落到闸门在**致死那一击**抄下的留存（见 geburaLuckyProc 的字段注释）。
			boolean luckyProc = buff(Lucky.LuckProc.class) != null || geburaLuckyProc != null;

			rollToDropLoot();

		if (cause == Dungeon.hero || cause instanceof Weapon || cause instanceof Weapon.Enchantment
				//薄暝的四重伤害来源：灼伤光环/攻击附带伤害击杀同样归入英雄击杀
				|| cause instanceof DimDusk.MagicStrike
				|| cause instanceof DimDusk.FlameStrike
				//泪剑「协同攻击」追加的法术伤害：来源是 TearSword.MagicStrike 标记类（非 buff 本身），
				//击杀同样归入英雄击杀
				|| cause instanceof TearSword.MagicStrike
				//松脂涂层（焦炭/黄金）的附加元素伤害：src 用的是涂层 buff 实例本身
				//（必须保留 buff 作源，才能让元素免疫/抗性判定生效），击杀同样归入英雄击杀
				|| cause instanceof ResinCoatingBuff
				|| cause instanceof Electricity){
			//指令任务：英雄击杀计数
			Instruction.onEnemyKilled(Dungeon.hero);
			//小小银河：装备时击杀敌人有概率掉落发光的鹅卵石
			TinyGalaxy.onEnemyKilled(Dungeon.hero, this);
			//笑靥：击杀敌人时按怪物经验值累积成长经验
			SmilingBlade.onEnemyKilled(Dungeon.hero, this);
			//环指大师：击杀敌人时按怪物类型独立判定掉落人体派素材（适配财富戒指掉率与幸运附魔）
			RingMasterLoot.onEnemyKilled(Dungeon.hero, this, luckyProc);
			//环指大师：击杀播报（10% 概率随机说一句，上一条没播完就跳过）
			RingMasterVoice.onEnemyKilled(Dungeon.hero);
			//复仇账簿：英雄击杀时按「怪物生命上限 ÷ 英雄生命上限」的百分比充能（十分之一）。
			//必须在 super.die() 之前读 mob.HT（那之后 HP 归零、精灵被回收）
			RevengeLedger.onEnemyKilled(Dungeon.hero, this);
			//中指长兄 转职「忠义巡礼者」天赋「永志不忘」+2：击杀带「怨恨标记」的单位时额外补 5% 账簿充能。
			//同样必须在 super.die() 之前——那之后怪物身上的 buff 会被一并清理
			VengeanceArts.onEnemyKilled(Dungeon.hero, this);
				//指令任务：难度0 特殊（击杀目睹的强大生物）
				if (properties().contains(Char.Property.BOSS)){
					Instruction.onBossKilled(Dungeon.hero);
				}

				//（「击杀指令目标」的判定已上移到本方法开头「带着标记倒下」收口 —— 它不再要求英雄击杀，
				//  因为 GEBURA 锁血会把死亡推迟到攻击调用栈之外。）

				if (Dungeon.hero.hasTalent(Talent.LETHAL_MOMENTUM)
						&& Random.Float() < 0.34f + 0.33f* Dungeon.hero.pointsInTalent(Talent.LETHAL_MOMENTUM)){
					Buff.affect(Dungeon.hero, Talent.LethalMomentumTracker.class, 0f);
				}
				if (Dungeon.hero.heroClass != HeroClass.DUELIST
						&& Dungeon.hero.hasTalent(Talent.LETHAL_HASTE)
						&& Dungeon.hero.buff(Talent.LethalHasteCooldown.class) == null){
					Buff.affect(Dungeon.hero, Talent.LethalHasteCooldown.class, 100f);
					Buff.affect(Dungeon.hero, GreaterHaste.class).set(2 + 2*Dungeon.hero.pointsInTalent(Talent.LETHAL_HASTE));
				}
			}

			//环指大师：流血导致的死亡同样触发其素材掉落列表（流血为非英雄直接伤害，原判定不掉落；
			//此处仅当英雄职业为环指大师时补发，其余职业维持原行为）
			if (cause instanceof Bleeding && Dungeon.hero.heroClass == HeroClass.RING_MASTER){
				RingMasterLoot.onEnemyKilled(Dungeon.hero, this, luckyProc);
				//流血致死同样是英雄的战果，击杀播报一并补上（与上面的英雄击杀分支互斥，不会重复触发）
				RingMasterVoice.onEnemyKilled(Dungeon.hero);
			}

		}

		if (Dungeon.hero.isAlive() && !Dungeon.level.heroFOV[pos]) {
			GLog.i( Messages.get(this, "died") );
		}

		boolean soulMarked = buff(SoulMark.class) != null;

		super.die( cause );

		//溶解之爱击杀：目标移除后在其格生成粉色史莱姆
		if (cause instanceof MeltingLove){
			((MeltingLove) cause).onKill( pos );
		}

		if (!(this instanceof Wraith)
				&& soulMarked
				&& Random.Float() < (0.4f*Dungeon.hero.pointsInTalent(Talent.NECROMANCERS_MINIONS)/3f)){
			Wraith w = Wraith.spawnAt(pos, Wraith.class);
			if (w != null) {
				Buff.affect(w, Corruption.class);
				if (Dungeon.level.heroFOV[pos]) {
					CellEmitter.get(pos).burst(ShadowParticle.CURSE, 6);
					Sample.INSTANCE.play(Assets.Sounds.CURSED);
				}
			}
		}
	}

	public float lootChance(){
		float lootChance = this.lootChance;

		float dropBonus = RingOfWealth.dropChanceMultiplier( Dungeon.hero );

		Talent.BountyHunterTracker bhTracker = Dungeon.hero.buff(Talent.BountyHunterTracker.class);
		if (bhTracker != null){
			Preparation prep = Dungeon.hero.buff(Preparation.class);
			if (prep != null){
				// 2/4/8/16% per prep level, multiplied by talent points
				float bhBonus = 0.02f * (float)Math.pow(2, prep.attackLevel()-1);
				bhBonus *= Dungeon.hero.pointsInTalent(Talent.BOUNTY_HUNTER);
				dropBonus += bhBonus;
			}
		}

		dropBonus += ShardOfOblivion.lootChanceMultiplier()-1f;

		return lootChance * dropBonus;
	}
	
	public void rollToDropLoot(){
		if (Dungeon.hero.lvl > maxLvl + 2) return;

		MasterThievesArmband.StolenTracker stolen = buff(MasterThievesArmband.StolenTracker.class);
		if (stolen == null || !stolen.itemWasStolen()) {
			if (Random.Float() < lootChance()) {
				Item loot = createLoot();
				if (loot != null) {
					Dungeon.level.drop(loot, pos).sprite.drop();
				}
			}
		}
		
		//ring of wealth logic
		//GEBURA 掉落修复：判定用的财富等级取「致死那一刻」的口径 —— live 优先（原版情形逐字一致），
		//live 取不到时回落到闸门抄下的 geburaWealthBonus。等级要**显式传给** tryForBonusDrop：
		//它内部还会再查一次 getBuffedBonus，不传的话回落值等于白抄（照样返回 null）。
		int wealthBonus = Ring.getBuffedBonus(Dungeon.hero, RingOfWealth.Wealth.class);
		if (wealthBonus <= 0) wealthBonus = geburaWealthBonus;
		if (wealthBonus > 0) {
			int rolls = 1;
			if (properties.contains(Property.BOSS)) rolls = 15;
			else if (properties.contains(Property.MINIBOSS)) rolls = 5;
			ArrayList<Item> bonus = RingOfWealth.tryForBonusDrop(Dungeon.hero, rolls, wealthBonus);
			if (bonus != null && !bonus.isEmpty()) {
				for (Item b : bonus) Dungeon.level.drop(b, pos).sprite.drop();
				RingOfWealth.showFlareForBonusDrop(sprite);
			}
		}
		
		//lucky enchant logic
		//GEBURA 掉落修复：live 的 LuckProc 优先（原版情形逐字一致）；它若已在锁血那一击之后自行摘掉，
		//就用闸门留存下来的那个实例补判（掉落强度沿用致死那一刻的记录）。用完即放，不留悬挂引用。
		Lucky.LuckProc luck = buff(Lucky.LuckProc.class);
		if (luck == null) luck = geburaLuckyProc;
		if (luck != null){
			geburaLuckyProc = null;
			Dungeon.level.drop(luck.genLoot(), pos).sprite.drop();
			Lucky.showFlare(sprite);
		}

		//soul eater talent
		if (buff(SoulMark.class) != null &&
				Random.Int(10) < Dungeon.hero.pointsInTalent(Talent.SOUL_EATER)){
			Talent.onFoodEaten(Dungeon.hero, 0, null);
		}

	}
	
	protected Object loot = null;
	protected float lootChance = 0;
	
	@SuppressWarnings("unchecked")
	public Item createLoot() {
		Item item;
		if (loot instanceof Generator.Category) {

			item = Generator.randomUsingDefaults( (Generator.Category)loot );

		} else if (loot instanceof Class<?>) {

			if (ExoticPotion.regToExo.containsKey(loot)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					return Generator.random(ExoticPotion.regToExo.get(loot));
				}
			} else if (ExoticScroll.regToExo.containsKey(loot)){
				if (Random.Float() < ExoticCrystals.consumableExoticChance()){
					return Generator.random(ExoticScroll.regToExo.get(loot));
				}
			}

			item = Generator.random( (Class<? extends Item>)loot );

		} else {

			item = (Item)loot;

		}
		return item;
	}

	//how many mobs this one should count as when determining spawning totals
	public float spawningWeight(){
		return 1;
	}
	
	public boolean reset() {
		return false;
	}
	
	public void beckon( int cell ) {
		
		notice();
		
		if (state != HUNTING && state != FLEEING) {
			state = WANDERING;
		}
		target = cell;
	}
	
	public String description() {
		return Messages.get(this, "desc");
	}

	public String info(){
		String desc = description();

		for (Buff b : buffs(ChampionEnemy.class)){
			desc += "\n\n_" + Messages.titleCase(b.name()) + "_\n" + b.desc();
		}

		return desc;
	}
	
	public void notice() {
		sprite.showAlert();
	}
	
	public void yell( String str ) {
		GLog.newLine();
		GLog.n( "%s: \"%s\" ", Messages.titleCase(name()), str );
	}

	//some mobs have an associated landmark entry, which is added when the hero sees them
	//mobs may also remove this landmark in some cases, such as when a quest is complete or they die
	public Notes.Landmark landmark(){
		return null;
	}

	public interface AiState {
		boolean act( boolean enemyInFOV, boolean justAlerted );
	}

	protected class Sleeping implements AiState {

		public static final String TAG	= "SLEEPING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {

			//debuffs cause mobs to wake as well
			for (Buff b : buffs()){
				if (b.type == Buff.buffType.NEGATIVE){
					awaken(enemyInFOV);
					if (state == SLEEPING){
						spend(TICK); //wait if we can't wake up for some reason
					}
					return true;
				}
			}

			//can be awoken by the least stealthy hostile present, not necessarily just our target
			if (enemyInFOV || (enemy != null && enemy.invisible > 0)) {

				float highestChance = Float.POSITIVE_INFINITY;
				Char closestHostile = null;

				for (Char ch : Actor.chars()){
					if (fieldOfView[ch.pos] && ch.invisible == 0 && ch.alignment != alignment && ch.alignment != Alignment.NEUTRAL){
						float bestChance = detectionChance(ch);
						//silent steps rogue talent, which also applies to rogue's shadow clone
						if ((ch instanceof Hero || ch instanceof ShadowClone.ShadowAlly)
								&& Dungeon.hero.hasTalent(Talent.SILENT_STEPS)){
							if (distance(ch) >= 4 - Dungeon.hero.pointsInTalent(Talent.SILENT_STEPS)) {
								bestChance = Float.POSITIVE_INFINITY;
							}
						}
						//flying characters are naturally stealthy
						if (ch.flying && distance(ch) >= 2){
							bestChance = Float.POSITIVE_INFINITY;
						}
						if (bestChance < highestChance){
							highestChance = bestChance;
							closestHostile = ch;
						}
					}
				}

				if (closestHostile != null && Random.Float() < detectionChance(closestHostile)) {
					awaken(enemyInFOV);
					if (state == SLEEPING){
						spend(TICK); //wait if we can't wake up for some reason
					}
					return true;
				}

			}

			enemySeen = false;
			spend( TICK );

			return true;
		}

		//chance is 1 in (distance + stealth)
		protected float detectionChance( Char enemy ){
			return 1 / (distance( enemy ) + enemy.stealth());
		}

		protected void awaken( boolean enemyInFOV ){
			if (enemyInFOV) {
				enemySeen = true;
				notice();
				state = HUNTING;
				target = enemy.pos;
			} else {
				notice();
				state = WANDERING;
				target = Dungeon.level.randomDestination( Mob.this );
			}

			if (alignment == Alignment.ENEMY && Dungeon.isChallenged(Challenges.SWARM_INTELLIGENCE)) {
				for (Mob mob : Dungeon.level.mobs) {
					if (mob.paralysed <= 0
							&& Dungeon.level.distance(pos, mob.pos) <= 8
							&& mob.state != mob.HUNTING) {
						mob.beckon(target);
					}
				}
			}
			spend(TIME_TO_WAKE_UP);
		}
	}

	protected class Wandering implements AiState {

		public static final String TAG	= "WANDERING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			if (enemyInFOV && (justAlerted || Random.Float() < detectionChance(enemy))) {

				return noticeEnemy();

			} else {

				return continueWandering();

			}
		}

		//chance is 1 in (distance/2 + stealth)
		protected float detectionChance( Char enemy ){
			return 1 / (distance( enemy ) / 2f + enemy.stealth());
		}

		protected boolean noticeEnemy(){
			enemySeen = true;
			
			notice();
			alerted = true;
			state = HUNTING;
			target = enemy.pos;
			
			if (alignment == Alignment.ENEMY && Dungeon.isChallenged( Challenges.SWARM_INTELLIGENCE )) {
				for (Mob mob : Dungeon.level.mobs) {
					if (mob.paralysed <= 0
							&& Dungeon.level.distance(pos, mob.pos) <= 8
							&& mob.state != mob.HUNTING) {
						mob.beckon( target );
					}
				}
			}
			
			return true;
		}
		
		protected boolean continueWandering(){
			enemySeen = false;
			
			int oldPos = pos;
			if (target != -1 && getCloser( target )) {
				spend( 1 / speed() );
				return moveSprite( oldPos, pos );
			} else {
				target = randomDestination();
				spend( TICK );
			}
			
			return true;
		}

		protected int randomDestination(){
			return Dungeon.level.randomDestination( Mob.this );
		}
		
	}

	//we keep a list of characters we were recently hit by, so we can switch targets if needed
	protected ArrayList<Char> recentlyAttackedBy = new ArrayList<>();

	protected class Hunting implements AiState {

		public static final String TAG	= "HUNTING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			if (enemyInFOV && !isCharmedBy( enemy ) && canAttack( enemy )) {

				recentlyAttackedBy.clear();
				target = enemy.pos;
				return doAttack( enemy );

			} else {

				//if we cannot attack our target, but were hit by something else that
				// is visible and attackable or closer, swap targets
				if (handleRecentAttackers()){
					return act( true, justAlerted );
				}

				if (enemyInFOV) {
					target = enemy.pos;
				} else if (enemy == null) {
					sprite.showLost();
					state = WANDERING;
					target = ((Mob.Wandering)WANDERING).randomDestination();
					spend( TICK );
					return true;
				}
				
				int oldPos = pos;
				if (target != -1 && getCloser( target )) {
					
					spend( 1 / speed() );
					return moveSprite( oldPos,  pos );

				} else {

					return handleUnreachableTarget(enemyInFOV, justAlerted);
				}
			}
		}

		protected boolean handleRecentAttackers(){
			boolean swapped = false;
			if (!recentlyAttackedBy.isEmpty()){
				for (Char ch : recentlyAttackedBy){
					if (ch != null && ch.isActive() && Actor.chars().contains(ch) && alignment != ch.alignment && fieldOfView[ch.pos] && ch.invisible == 0 && !isCharmedBy(ch)) {
						if (canAttack(ch) || enemy == null || Dungeon.level.distance(pos, ch.pos) < Dungeon.level.distance(pos, enemy.pos)) {
							enemy = ch;
							target = ch.pos;
							swapped = true;
						}
					}
				}
				recentlyAttackedBy.clear();
			}
			return swapped;
		}

		//prevents rare infinite loop cases
		protected boolean recursing = false;

		//Try to switch targets to another enemy that is closer or reachable
		//unless we have already done that and still can't move toward them, then move on.
		protected boolean handleUnreachableTarget(boolean enemyInFOV, boolean justAlerted){
			if (!recursing) {
				Char oldEnemy = enemy;
				enemy = null;
				enemy = chooseEnemy();
				if (enemy != null && enemy != oldEnemy) {
					recursing = true;
					boolean result = act(enemyInFOV, justAlerted);
					recursing = false;
					return result;
				}
			}

			spend( TICK );
			if (!enemyInFOV) {
				sprite.showLost();
				state = WANDERING;
				target = ((Mob.Wandering)WANDERING).randomDestination();
			}
			return true;
		}
	}

	//essentially a more aggressive version of wandering, where target pos is updated like hunting
	//not currently used directly by mobs outside of the vault, which also add more behaviour here
	protected class Investigating extends Wandering {

		public static final String TAG	= "INVESTIGATING";

		@Override
		public boolean act(boolean enemyInFOV, boolean justAlerted) {
			if (enemyInFOV){
				target = enemy.pos;
			} else {
				//we lose our target BEFORE reaching their last known position
				if (Dungeon.level.distance(pos, target) <= 1){
					sprite.showLost();
					state = WANDERING;
					target = ((Mob.Wandering)WANDERING).randomDestination();
					spend( TICK );
					return true;
				}
			}
			return super.act(enemyInFOV, justAlerted);
		}

		//same detection chance as wandering

	}

	protected class Fleeing implements AiState {

		public static final String TAG	= "FLEEING";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			//triggers escape logic when 0-dist rolls a 6 or greater.
			if (enemy == null || !enemyInFOV && 1 + Random.Int(Dungeon.level.distance(pos, target)) >= 6){
				escaped();
				if (state != FLEEING){
					spend( TICK );
					return true;
				}
			
			//if enemy isn't in FOV, keep running from their previous position.
			} else if (enemyInFOV) {
				target = enemy.pos;
			}

			int oldPos = pos;
			if (target != -1 && getFurther( target )) {

				spend( 1 / speed() );
				return moveSprite( oldPos, pos );

			} else {

				spend( TICK );
				nowhereToRun();

				return true;
			}
		}

		protected void escaped(){
			//does nothing by default, some enemies have special logic for this
		}

		//enemies will turn and fight if they have nowhere to run and aren't affect by terror
		protected void nowhereToRun() {
			if (buff( Terror.class ) == null && buff( Dread.class ) == null) {
				if (enemySeen) {
					sprite.showStatus(CharSprite.WARNING, Messages.get(Mob.class, "rage"));
					state = HUNTING;
				} else {
					state = WANDERING;
				}
			}
		}
	}

	protected class Passive implements AiState {

		public static final String TAG	= "PASSIVE";

		@Override
		public boolean act( boolean enemyInFOV, boolean justAlerted ) {
			enemySeen = enemyInFOV;
			spend( TICK );
			return true;
		}
	}
	
	
	private static ArrayList<Mob> heldAllies = new ArrayList<>();

	public static void holdAllies( Level level ){
		holdAllies(level, Dungeon.hero.pos);
	}

	public static void holdAllies( Level level, int holdFromPos ){
		heldAllies.clear();
		for (Mob mob : level.mobs.toArray( new Mob[0] )) {
			//preserve directable allies or empowered intelligent allies no matter where they are
			if (mob instanceof DirectableAlly
				|| (mob.intelligentAlly && PowerOfMany.getPoweredAlly() == mob)) {
				if (mob instanceof DirectableAlly) {
					((DirectableAlly) mob).clearDefensingPos();
				}
				level.mobs.remove( mob );
				heldAllies.add(mob);
				
			//preserve other intelligent allies if they are near the hero
			} else if (mob.alignment == Alignment.ALLY
					&& mob.intelligentAlly
					&& Dungeon.level.distance(holdFromPos, mob.pos) <= 5){
				level.mobs.remove( mob );
				heldAllies.add(mob);
			}
		}
	}

	public static void restoreAllies( Level level, int pos ){
		restoreAllies(level, pos, -1);
	}

	public static void restoreAllies( Level level, int pos, int gravitatePos ){
		if (!heldAllies.isEmpty()){
			
			ArrayList<Integer> candidatePositions = new ArrayList<>();
			for (int i : PathFinder.NEIGHBOURS8) {
				if (!Dungeon.level.solid[i+pos] && !Dungeon.level.avoid[i+pos] && level.findMob(i+pos) == null){
					candidatePositions.add(i+pos);
				}
			}

			//gravitate pos sets a preferred location for allies to be closer to
			if (gravitatePos == -1) {
				Collections.shuffle(candidatePositions);
			} else {
				Collections.sort(candidatePositions, new Comparator<Integer>() {
					@Override
					public int compare(Integer t1, Integer t2) {
						return Dungeon.level.distance(gravitatePos, t1) -
								Dungeon.level.distance(gravitatePos, t2);
					}
				});
			}

			//can only have one empowered ally at once, prioritize incoming ally
			if (Stasis.getStasisAlly() != null){
				for (Mob mob : level.mobs.toArray( new Mob[0] )) {
					if (mob.buff(PowerOfMany.PowerBuff.class) != null){
						mob.buff(PowerOfMany.PowerBuff.class).detach();
					}
				}
			}
			
			for (Mob ally : heldAllies) {

				//can only have one empowered ally at once, prioritize incoming ally
				if (ally.buff(PowerOfMany.PowerBuff.class) != null){
					for (Mob mob : level.mobs.toArray( new Mob[0] )) {
						if (mob.buff(PowerOfMany.PowerBuff.class) != null){
							mob.buff(PowerOfMany.PowerBuff.class).detach();
						}
					}
				}

				level.mobs.add(ally);
				ally.state = ally.WANDERING;
				
				if (!candidatePositions.isEmpty()){
					ally.pos = candidatePositions.remove(0);
				} else {
					ally.pos = pos;
				}
				if (ally.sprite != null) ally.sprite.place(ally.pos);

				if (ally.fieldOfView == null || ally.fieldOfView.length != level.length()){
					ally.fieldOfView = new boolean[level.length()];
				}
				Dungeon.level.updateFieldOfView( ally, ally.fieldOfView );
				
			}
		}
		heldAllies.clear();
	}
	
	public static void clearHeldAllies(){
		heldAllies.clear();
	}
}

