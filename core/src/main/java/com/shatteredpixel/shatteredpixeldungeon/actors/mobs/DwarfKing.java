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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Degrade;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LifeLink;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.spells.ClericSpell;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.npcs.Sheep;
import com.shatteredpixel.shatteredpixeldungeon.effects.Beam;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Pushing;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ElmoParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ShadowParticle;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.KingsCrown;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.glyphs.Viscosity;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.DriedRose;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.LloydsBeacon;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfForce;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.Wand;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfLightning;
import com.shatteredpixel.shatteredpixeldungeon.journal.Bestiary;
import com.shatteredpixel.shatteredpixeldungeon.levels.CityBossLevel;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.KingSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BossHealthBar;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

import java.util.ArrayList;
import java.util.HashSet;

public class DwarfKing extends Mob {

	{
		spriteClass = KingSprite.class;

		HP = HT = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 450 : 300;
		EXP = 40;
		defenseSkill = 22;

		properties.add(Property.BOSS);
		properties.add(Property.UNDEAD);
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 15, 25 );
	}

	@Override
	public int attackSkill( Char target ) {
		return 26;
	}

	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 10);
	}

	private int phase = 1;
	private int summonsMade = 0;

	private float summonCooldown = 0;
	private float abilityCooldown = 0;
	private final int MIN_COOLDOWN = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 8 : 10;
	private final int MAX_COOLDOWN = Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 10 : 14;

	private int lastAbility = 0;
	private static final int NONE = 0;
	private static final int LINK = 1;
	private static final int TELE = 2;

	private static final String PHASE = "phase";
	private static final String SUMMONS_MADE = "summons_made";

	private static final String SUMMON_CD = "summon_cd";
	private static final String ABILITY_CD = "ability_cd";
	private static final String LAST_ABILITY = "last_ability";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put( PHASE, phase );
		bundle.put( SUMMONS_MADE, summonsMade );
		bundle.put( SUMMON_CD, summonCooldown );
		bundle.put( ABILITY_CD, abilityCooldown );
		bundle.put( LAST_ABILITY, lastAbility );
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		phase = bundle.getInt( PHASE );
		summonsMade = bundle.getInt( SUMMONS_MADE );
		summonCooldown = bundle.getFloat( SUMMON_CD );
		abilityCooldown = bundle.getFloat( ABILITY_CD );
		lastAbility = bundle.getInt( LAST_ABILITY );

		if (phase == 2) properties.add(Property.IMMOVABLE);

		BossHealthBar.assignBoss(this);
		if (phase == 3) BossHealthBar.bleed(true);
	}

	//--- 二阶段「王座屏障」的刻度（2026-09-24 修 CHESED 引发的卡死）--------------------
	//原版把这套数字写死成**绝对量**，三者靠一个算术恒等式咬合：
	//  屏障满值 = HT、每击杀一名仆从削减 HT/12、波次阈值 200 / 100（挑战强化下 300 / 150）：
	//  HT=300 ⇒ 第 4 次击杀后剩 200（= 300×2/3，放第二波）、第 8 次后剩 100（= 300×1/3，放第三波）、
	//  第 12 次恰好为 0（进三阶段）；强化挑战 HT=450 ⇒ 18 次击杀、阈值 300/150，同理。
	//于是整套节奏唯一依赖的前提是「**HT 能被 12（强化 18）整除**」。
	//
	//考验 CHESED 给敌方生命上限 +25%（Trials.CHESED_HP_MULT：300 → 375）后整除不成立，
	//而二阶段的国王除 KingDamager 外**免疫一切伤害**、波次又被绝对阈值卡住 ⇒ 死局：
	//  · 12 次击杀只能削掉 12×(375/12) = 372 ⇒ 屏障永远剩 3 点、三阶段永远进不去；
	//  · 第一波（4 次击杀）之后屏障剩 251 > 200 ⇒ **第二波永远不出**（用户报的「二阶段卡死不出怪」）。
	//
	//⇒ 把两条刻度改成**从 HT 现算**，恒等式对任意 HT 都成立：
	//  ① 削减量 = ceil(HT / kills) —— 向上取整 ⇒ kills 次击杀必定削掉 ≥ HT
	//     （多出的部分由 ShieldBuff.absorbDamage 夹掉）；
	//  ② 阈值 = HT×2/3、HT/3 —— 恰好对应「第 kills/3 次、第 2×kills/3 次击杀」。
	//HT = 300 / 450 时 ①② 与原写死值**逐个相同**（25 / 200 / 100 / 300 / 150）⇒ 原版行为分毫不变。
	//⚠️ 不碰 Random、不新增字段 ⇒ 同 seed 的随机流与改动前逐字节一致。

	/** 打空二阶段屏障所需的击杀次数（＝原版写死的 12 / 强化挑战 18）。 */
	private static int barrierKills(){
		return Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 18 : 12;
	}

	/**
	 * 每击杀一名二阶段仆从对屏障的削减量。<b>向上取整</b>：保证 {@link #barrierKills()} 次击杀
	 * 必定把满值为 {@code HT} 的屏障打空（原版写死 {@code HT/12} 是向下取整，HT 不被整除时凑不到 0）。
	 */
	private int barrierChip(){
		int kills = barrierKills();
		return (HT + kills - 1) / kills;
	}

	/**
	 * 二阶段波次推进阈值：屏障降到满值的 num/3 时放下一波。
	 * @param num 2 → 满值的 2/3（放第二波）；1 → 满值的 1/3（放第三波）
	 */
	private int barrierThreshold( int num ){
		return HT * num / 3;
	}

	/**
	 * 一阶段 → 二阶段的入场血量（＝原版写死的 50；强化挑战 HT=450 时为 100）。
	 * <p>原版是**绝对量**（普通 50、强化 100）。改为按 HT 的同一比例现算 —— 普通 {@code HT/6}、
	 * 强化 {@code HT×2/9} ⇒ HT 变大时入场点同比例后移，而不是固定卡在 50 / 100。
	 * <p>HT = 300 / 450 时与原写死值**逐个相同**（50 / 100）⇒ 原版行为分毫不变。
	 * <p>说明：本值**不是**死局来源（血一定能掉到 50），2026-09-24 那次 CHESED 卡死与它无关；
	 * 这里只是把它从「绝对刻度」归并成「HT 刻度」，与二阶段屏障的两条刻度保持同一口径。
	 */
	private int phase2EntryHP(){
		return Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? (HT * 2 / 9) : (HT / 6);
	}

	@Override
	protected boolean act() {
		if (pos == CityBossLevel.throne){
			throwItems();
		}

		if (phase == 1) {

			if (summonCooldown <= 0 && summonSubject(Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 2 : 3)){
				summonsMade++;
				summonCooldown += Random.NormalIntRange(MIN_COOLDOWN, MAX_COOLDOWN);
			} else if (summonCooldown > 0){
				summonCooldown--;
			}

			if (paralysed > 0){
				spend(TICK);
				return true;
			}

			if (abilityCooldown <= 0){

				if (lastAbility == NONE) {
					//50/50 either ability
					lastAbility = Random.Int(2) == 0 ? LINK : TELE;
				} else if (lastAbility == LINK) {
					//more likely to use tele
					lastAbility = Random.Int(8) == 0 ? LINK : TELE;
				} else {
					//more likely to use link
					lastAbility = Random.Int(8) != 0 ? LINK : TELE;
				}

				if (lastAbility == LINK && lifeLinkSubject()){
					abilityCooldown += Random.NormalIntRange(MIN_COOLDOWN, MAX_COOLDOWN);
					spend(TICK);
					return true;
				} else if (teleportSubject()) {
					lastAbility = TELE;
					abilityCooldown += Random.NormalIntRange(MIN_COOLDOWN, MAX_COOLDOWN);
					spend(TICK);
					return true;
				}

			} else {
				abilityCooldown--;
			}

		} else if (phase == 2){

			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)){
				//challenge logic
				if (summonsMade < 6){
					if (summonsMade == 0) {
						sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
						Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
						yell(Messages.get(this, "wave_1"));
					}
					summonSubject(3, DKGhoul.class);
					summonSubject(3, DKGhoul.class);
					spend(3 * TICK);
					summonsMade += 2;
					return true;
				} else if (shielding() <= barrierThreshold( 2 ) && summonsMade < 12){
					if (summonsMade == 6) {
						sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
						Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
						yell(Messages.get(this, "wave_2"));
					}
					summonSubject(3, DKGhoul.class);
					summonSubject(3, DKGhoul.class);
					if (summonsMade == 6) {
						summonSubject(3, DKMonk.class);
					} else {
						summonSubject(3, DKWarlock.class);
					}
					summonsMade += 3;
					spend(3*TICK);
					return true;
				} else if (shielding() <= barrierThreshold( 1 ) && summonsMade < 18) {
					if (summonsMade == 12) {
						sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
						Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
						yell(Messages.get(this, "wave_3"));
						summonSubject(3, DKWarlock.class);
						summonSubject(3, DKMonk.class);
						summonSubject(3, DKGhoul.class);
						summonSubject(3, DKGhoul.class);
						summonsMade += 4;
						spend(3*TICK);
					} else {
						summonSubject(3, DKGolem.class);
						summonSubject(3, DKGolem.class);
						summonsMade += 2;
						spend(TICK);
					}
					return true;
				} else {
					spend(TICK);
					return true;
				}
			} else {
				//non-challenge logic
				if (summonsMade < 4) {
					if (summonsMade == 0) {
						sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
						Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
						yell(Messages.get(this, "wave_1"));
					}
					summonSubject(3, DKGhoul.class);
					spend(3 * TICK);
					summonsMade++;
					return true;
				} else if (shielding() <= barrierThreshold( 2 ) && summonsMade < 8) {
					if (summonsMade == 4) {
						sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
						Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
						yell(Messages.get(this, "wave_2"));
					}
					if (summonsMade == 7) {
						summonSubject(3, Random.Int(2) == 0 ? DKMonk.class : DKWarlock.class);
					} else {
						summonSubject(3, DKGhoul.class);
					}
					summonsMade++;
					spend(TICK);
					return true;
				} else if (shielding() <= barrierThreshold( 1 ) && summonsMade < 12) {
					sprite.centerEmitter().start(Speck.factory(Speck.SCREAM), 0.4f, 2);
					Sample.INSTANCE.play(Assets.Sounds.CHALLENGE);
					yell(Messages.get(this, "wave_3"));
					summonSubject(4, DKWarlock.class);
					summonSubject(4, DKMonk.class);
					summonSubject(4, DKGhoul.class);
					summonSubject(4, DKGhoul.class);
					summonsMade = 12;
					spend(TICK);
					return true;
				} else {
					spend(TICK);
					return true;
				}
			}
		} else if (phase == 3 && buffs(Summoning.class).size() < 4){
			if (summonSubject(Dungeon.isChallenged(Challenges.STRONGER_BOSSES) ? 2 : 3)) summonsMade++;
		}

		return super.act();
	}

	private boolean summonSubject( int delay ){
		if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES)) {
			//every 3rd summon is always a monk or warlock, otherwise ghoul
			//except every 9th summon, which is a golem!
			if (summonsMade % 3 == 2) {
				if (summonsMade % 9 == 8){
					return summonSubject(delay, DKGolem.class);
				} else {
					return summonSubject(delay, Random.Int(2) == 0 ? DKMonk.class : DKWarlock.class);
				}
			} else {
				return summonSubject(delay, DKGhoul.class);
			}

		} else {
			//every 4th summon is always a monk or warlock, otherwise ghoul
			if (summonsMade % 4 == 3) {
				return summonSubject(delay, Random.Int(2) == 0 ? DKMonk.class : DKWarlock.class);
			} else {
				return summonSubject(delay, DKGhoul.class);
			}
		}
	}

	private boolean summonSubject( int delay, Class<?extends Mob> type ){
		Summoning s = new Summoning();
		s.pos = ((CityBossLevel)Dungeon.level).getSummoningPos();
		if (s.pos == -1) return false;
		s.summon = type;
		s.delay = delay;
		s.attachTo(this);
		return true;
	}

	private HashSet<Mob> getSubjects(){
		HashSet<Mob> subjects = new HashSet<>();
		for (Mob m : Dungeon.level.mobs){
			if (m.alignment == alignment && (m instanceof Ghoul || m instanceof Monk || m instanceof Warlock || m instanceof Golem)){
				subjects.add(m);
			}
		}
		return subjects;
	}

	private boolean lifeLinkSubject(){
		Mob furthest = null;

		for (Mob m : getSubjects()){
			boolean alreadyLinked = false;
			for (LifeLink l : m.buffs(LifeLink.class)){
				if (l.object == id()) alreadyLinked = true;
			}
			if (!alreadyLinked) {
				if (furthest == null || Dungeon.level.distance(pos, furthest.pos) < Dungeon.level.distance(pos, m.pos)){
					furthest = m;
				}
			}
		}

		if (furthest != null) {
			Buff.append(furthest, LifeLink.class, 100f).object = id();
			Buff.append(this, LifeLink.class, 100f).object = furthest.id();
			yell(Messages.get(this, "lifelink_" + Random.IntRange(1, 2)));
			sprite.parent.add(new Beam.HealthRay(sprite.destinationCenter(), furthest.sprite.destinationCenter()));
			Sample.INSTANCE.play( Assets.Sounds.RAY );
			return true;

		}
		return false;
	}

	private boolean teleportSubject(){
		if (enemy == null) return false;

		Mob furthest = null;

		for (Mob m : getSubjects()){
			if (furthest == null || Dungeon.level.distance(pos, furthest.pos) < Dungeon.level.distance(pos, m.pos)){
				furthest = m;
			}
		}

		if (furthest != null){

			float bestDist;
			int bestPos = pos;

			Ballistica trajectory = new Ballistica(enemy.pos, pos, Ballistica.STOP_TARGET);
			int targetCell = trajectory.path.get(trajectory.dist+1);
			//if the position opposite the direction of the hero is open, go there
			if (Actor.findChar(targetCell) == null && !Dungeon.level.solid[targetCell]){
				bestPos = targetCell;

			//Otherwise go to the neighbour cell that's open and is furthest
			} else {
				bestDist = Dungeon.level.trueDistance(pos, enemy.pos);

				for (int i : PathFinder.NEIGHBOURS8){
					if (Actor.findChar(pos+i) == null
							&& !Dungeon.level.solid[pos+i]
							&& Dungeon.level.trueDistance(pos+i, enemy.pos) > bestDist){
						bestPos = pos+i;
						bestDist = Dungeon.level.trueDistance(pos+i, enemy.pos);
					}
				}
			}

			Actor.add(new Pushing(this, pos, bestPos));
			pos = bestPos;

			//find closest cell that's adjacent to enemy, place subject there
			bestDist = Dungeon.level.trueDistance(enemy.pos, pos);
			bestPos = enemy.pos;
			for (int i : PathFinder.NEIGHBOURS8){
				if (Actor.findChar(enemy.pos+i) == null
						&& !Dungeon.level.solid[enemy.pos+i]
						&& Dungeon.level.trueDistance(enemy.pos+i, pos) < bestDist){
					bestPos = enemy.pos+i;
					bestDist = Dungeon.level.trueDistance(enemy.pos+i, pos);
				}
			}

			if (bestPos != enemy.pos) ScrollOfTeleportation.appear(furthest, bestPos);
			yell(Messages.get(this, "teleport_" + Random.IntRange(1, 2)));
			return true;
		}
		return false;
	}

	@Override
	public void notice() {
		super.notice();
		if (!BossHealthBar.isAssigned()) {
			BossHealthBar.assignBoss(this);
			yell(Messages.get(this, "notice"));
			for (Char ch : Actor.chars()){
				if (ch instanceof DriedRose.GhostHero){
					((DriedRose.GhostHero) ch).sayBoss();
				}
			}
		}
	}

	@Override
	public boolean isInvulnerable(Class effect) {
		if (effect == KingDamager.class){
			return false;
		} else {
			return phase == 2 || super.isInvulnerable(effect);
		}
	}

	@Override
	public void damage(int dmg, Object src) {
		//hero counts as unarmed if they aren't attacking with a weapon and aren't benefiting from force
		if (src == Dungeon.hero && (!RingOfForce.fightingUnarmed(Dungeon.hero) || Dungeon.hero.buff(RingOfForce.Force.class) != null)){
			Statistics.qualifiedForBossChallengeBadge = false;
		//Corrosion, corruption, and regrowth do no direct damage and so have their own custom logic
		//Transfusion damages DK and so doesn't need custom logic
		//Lightning has custom logic so that chaining it doesn't DQ for the badge
		} else if (src instanceof Wand && !(src instanceof WandOfLightning)){
			Statistics.qualifiedForBossChallengeBadge = false;
		//Only damage-dealing spells from the Cleric
		} else if (src instanceof ClericSpell){
			Statistics.qualifiedForBossChallengeBadge = false;
		}

		if (isInvulnerable(src.getClass())){
			super.damage(dmg, src);
			return;
		} else if (phase == 3 && !(src instanceof Viscosity.DeferedDamage)){
			if (dmg >= 0) {
				Viscosity.DeferedDamage deferred = Buff.affect( this, Viscosity.DeferedDamage.class );
				deferred.extend( dmg );

				sprite.showStatus( CharSprite.WARNING, Messages.get(Viscosity.class, "deferred", dmg) );
			}
			return;
		}
		int preHP = HP;
		super.damage(dmg, src);

		LockedFloor lock = Dungeon.hero.buff(LockedFloor.class);
		if (lock != null && !isImmune(src.getClass()) && !isInvulnerable(src.getClass())){
			if (Dungeon.isChallenged(Challenges.STRONGER_BOSSES))   lock.addTime(dmg/5f);
			else                                                    lock.addTime(dmg/3f);
		}

		if (phase == 1) {
			int dmgTaken = preHP - HP;
			abilityCooldown -= dmgTaken/8f;
			summonCooldown -= dmgTaken/8f;
			if (HP <= phase2EntryHP()) {
				HP = phase2EntryHP();
				sprite.showStatus(CharSprite.POSITIVE, Messages.get(this, "invulnerable"));
				ScrollOfTeleportation.appear(this, CityBossLevel.throne);
				properties.add(Property.IMMOVABLE);
				phase = 2;
				summonsMade = 0;
				sprite.idle();
				Buff.affect(this, DKBarrior.class).setShield(HT);
				for (Summoning s : buffs(Summoning.class)) {
					s.detach();
				}
				Bestiary.skipCountingEncounters = true;
				for (Mob m : getSubjects()) {
					m.die(null);
				}
				Bestiary.skipCountingEncounters = false;
				for (Buff b: buffs()){
					if (b instanceof LifeLink){
						b.detach();
					}
				}
			}
		} else if (phase == 2 && shielding() == 0) {
			properties.remove(Property.IMMOVABLE);
			phase = 3;
			summonsMade = 1; //monk/warlock on 3rd summon
			sprite.centerEmitter().start( Speck.factory( Speck.SCREAM ), 0.4f, 2 );
			Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
			yell(  Messages.get(this, "enraged", Dungeon.hero.name()) );
			BossHealthBar.bleed(true);
			Game.runOnRenderThread(new Callback() {
				@Override
				public void call() {
					Music.INSTANCE.fadeOut(0.5f, new Callback() {
						@Override
						public void call() {
							Music.INSTANCE.play(Assets.Music.CITY_BOSS_FINALE, true);
						}
					});
				}
			});
		//⚠️ 这里的 20 刻意**保留为绝对量**（不随 HT 缩放）：它只是「血快见底时喊一句」的台词扳机，
		//  不参与任何阶段推进 / 无敌 / 出怪判定；而原版在两个难度下都用同一个常数，想缩放就得给
		//  普通与强化各配一个任意分母（20 在 HT=300 与 450 上并不同比例），反而会改掉原版行为。
		//  与上方「二阶段屏障」「一阶段转二阶段」那两处不同：那两处会**卡住流程**，必须 HT 化。
		} else if (phase == 3 && preHP > 20 && HP < 20 && isAlive()){
			yell( Messages.get(this, "losing") );
		}
	}

	@Override
	public boolean isAlive() {
		return super.isAlive() || phase != 3;
	}

	@Override
	public void die(Object cause) {

		GameScene.bossSlain();

		super.die( cause );

		Heap h = Dungeon.level.heaps.get(CityBossLevel.throne);
		if (h != null) {
			for (Item i : h.items) {
				Dungeon.level.drop(i, CityBossLevel.throne + Dungeon.level.width());
			}
			h.destroy();
		}

		if (pos == CityBossLevel.throne){
			Dungeon.level.drop(new KingsCrown(), pos + Dungeon.level.width()).sprite.drop(pos);
		} else {
			Dungeon.level.drop(new KingsCrown(), pos).sprite.drop();
		}

		Badges.validateBossSlain();
		if (Statistics.qualifiedForBossChallengeBadge){
			Badges.validateBossChallengeCompleted();
		}
		Statistics.bossScores[3] += 4000;

		Dungeon.level.unseal();

		Bestiary.skipCountingEncounters = true;
		for (Mob m : getSubjects()){
			m.die(null);
		}
		Bestiary.skipCountingEncounters = false;

		LloydsBeacon beacon = Dungeon.hero.belongings.getItem(LloydsBeacon.class);
		if (beacon != null) {
			beacon.upgrade();
		}

		//cleanses degrade that may have been applied by a DK warlock, mainly for convenience
		if (Dungeon.hero.buff(Degrade.class) != null){
			Dungeon.hero.buff(Degrade.class).detach();
		}

		yell( Messages.get(this, "defeated") );
	}

	@Override
	public boolean isImmune(Class effect) {
		//immune to damage amplification from doomed in 2nd phase or later, but it can still be applied
		if (phase > 1 && effect == Doom.class && buff(Doom.class) != null ){
			return true;
		}
		return super.isImmune(effect);
	}

	public static class DKGhoul extends Ghoul {
		{
			properties.add(Property.BOSS_MINION);
			state = HUNTING;
		}

		@Override
		protected boolean act() {
			partnerID = -2; //no partners
			return super.act();
		}
	}

	public static class DKMonk extends Monk {
		{
			properties.add(Property.BOSS_MINION);
			state = HUNTING;
		}
	}

	public static class DKWarlock extends Warlock {
		{
			properties.add(Property.BOSS_MINION);
			state = HUNTING;
		}

		@Override
		protected void zap() {
			if (enemy == Dungeon.hero){
				Statistics.bossScores[3] -= 400;
			}
			super.zap();
		}
	}

	public static class DKGolem extends Golem {
		{
			properties.add(Property.BOSS_MINION);
			state = HUNTING;
		}
	}

	public static class Summoning extends Buff {

		private int delay;
		private int pos;
		private Class<?extends Mob> summon;

		private Emitter particles;

		public int getPos() {
			return pos;
		}

		@Override
		public boolean act() {
			delay--;

			if (delay <= 0){

				if (summon == DKGolem.class){
					particles.burst(SparkParticle.FACTORY, 10);
					Sample.INSTANCE.play(Assets.Sounds.CHARGEUP);
				} else if (summon == DKWarlock.class){
					particles.burst(ShadowParticle.CURSE, 10);
					Sample.INSTANCE.play(Assets.Sounds.CURSED);
				} else if (summon == DKMonk.class){
					particles.burst(ElmoParticle.FACTORY, 10);
					Sample.INSTANCE.play(Assets.Sounds.BURNING);
				} else {
					particles.burst(Speck.factory(Speck.BONE), 10);
					Sample.INSTANCE.play(Assets.Sounds.BONES);
				}
				particles = null;

				if (Actor.findChar(pos) != null){
					ArrayList<Integer> candidates = new ArrayList<>();
					for (int i : PathFinder.NEIGHBOURS8){
						if (Dungeon.level.passable[pos+i] && Actor.findChar(pos+i) == null){
							candidates.add(pos+i);
						}
					}
					if (!candidates.isEmpty()){
						pos = Random.element(candidates);
					}
				}

				//kill sheep that are right on top of the spawner instead of failing to spawn
				if (Actor.findChar(pos) instanceof Sheep){
					Actor.findChar(pos).die(null);
				}

				if (Actor.findChar(pos) == null) {
					Mob m = Reflection.newInstance(summon);
					m.pos = pos;
					m.maxLvl = -2;
					GameScene.add(m);
					Dungeon.level.occupyCell(m);
					m.state = m.HUNTING;
					if (((DwarfKing)target).phase == 2){
						Buff.affect(m, KingDamager.class);
					}
				} else {
					Char ch = Actor.findChar(pos);
					ch.damage(Random.NormalIntRange(20, 40), this);
					if (((DwarfKing)target).phase == 2){
						target.damage(((DwarfKing)target).barrierChip(), new KingDamager());
					}
					if (!ch.isAlive() && ch == Dungeon.hero) {
						Dungeon.fail(DwarfKing.class);
						GLog.n( Messages.capitalize(Messages.get(Char.class, "kill", Messages.get(DwarfKing.class, "name"))));
					}
				}

				detach();
			}

			spend(TICK);
			return true;
		}

		@Override
		public void fx(boolean on) {
			if (on && (particles == null || particles.parent == null)) {
				particles = CellEmitter.get(pos);

				if (summon == DKGolem.class){
					particles.pour(SparkParticle.STATIC, 0.05f);
				} else if (summon == DKWarlock.class){
					particles.pour(ShadowParticle.UP, 0.1f);
				} else if (summon == DKMonk.class){
					particles.pour(ElmoParticle.FACTORY, 0.1f);
				} else {
					particles.pour(Speck.factory(Speck.RATTLE), 0.1f);
				}

			} else if (!on && particles != null) {
				particles.on = false;
			}
		}

		private static final String DELAY = "delay";
		private static final String POS = "pos";
		private static final String SUMMON = "summon";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(DELAY, delay);
			bundle.put(POS, pos);
			bundle.put(SUMMON, summon);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			delay = bundle.getInt(DELAY);
			pos = bundle.getInt(POS);
			summon = bundle.getClass(SUMMON);
		}
	}

	public static class KingDamager extends Buff {

		{
			revivePersists = true;
		}

		@Override
		public boolean act() {
			if (target.alignment != Alignment.ENEMY){
				detach();
			}
			spend( TICK );
			return true;
		}

		@Override
		public void detach() {
			super.detach();
			for (Mob m : Dungeon.level.mobs){
				if (m instanceof DwarfKing){
					DwarfKing king = (DwarfKing) m;
					king.damage( king.barrierChip(), this );
				}
			}
		}
	}

	public static class DKBarrior extends Barrier{

		@Override
		public boolean act() {
			incShield();
			return super.act();
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}

}
