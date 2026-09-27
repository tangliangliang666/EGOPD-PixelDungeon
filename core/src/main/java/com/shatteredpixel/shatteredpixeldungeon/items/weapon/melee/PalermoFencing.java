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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AcceleratingFuture;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.OdinsEyeOverheat;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.rogue.DeathMark;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.ValencinaSfx;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Swiftthistle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 巴勒莫剑术 —— 「拇指 前二老板」的专属固定武技（不随武器改变）。
 *
 * <p>选择一个 3 格内的目标：若未紧贴目标则先突进到目标身边，随后斩出
 * 伤害为「基础伤害 ÷ 攻击延迟」的一击（无必中属性）。</p>
 *
 * <p>与 buff「加速的未来」联动（层数保留到整套斩击结束后再清零，斩击按清零前的层数结算，
 * 因此剑术自身的攻击也能吃到限定层数的天赋）：</p>
 * <ul>
 *   <li>1 层以上：本次斩击改为必中</li>
 *   <li>3 层以上：命中后为目标施加 5 回合麻痹</li>
 *   <li>5 层以上：命中后麻痹 5 回合并点燃目标</li>
 * </ul>
 *
 * <p>战争英雄分支：剑术命中施加「震颤-灼热」，分支天赋三可将斩击拆分为 2/4/8 段独立连斩。</p>
 */
public class PalermoFencing {

	/** 巴勒莫剑术的目标选取范围（格）。 */
	public static final int RANGE = 3;

	//施放上下文（攻击钩子用）：标记“当前 hero.attack 是否为巴勒莫剑术的一段”，以及本次武技开始时的加速的未来层数
	private static boolean fencingSegment = false;
	private static int castStacks = 0;

	//震颤-灼热（战争英雄）施放级一次性标志：一次巴勒莫剑术只施加一次层数，多段斩不重复叠加
	private static boolean tremorApplied = false;

	//宰杀处置（家族之耻）施放级标志：本次施放触发斩杀 → 整套剑术结束时额外 +3 层加速的未来
	private static boolean executeBonusPending = false;

	/** 当前攻击是否为巴勒莫剑术的一段（供 Talent 命中钩子识别）。 */
	public static boolean isFencingSegment() {
		return fencingSegment;
	}

	/** 本次武技开始时的「加速的未来」层数（段内恒定，供层数限定天赋使用）。 */
	public static int fencingCastStacks() {
		return castStacks;
	}

	/** 本次施放首次命中段：返回 true 并置位；同一次施放的后续命中段返回 false（震颤-灼热只施加一次）。 */
	public static boolean tremorMarkOnce() {
		if (tremorApplied) return false;
		tremorApplied = true;
		return true;
	}

	private PalermoFencing() {}

	/** 巴勒莫剑术需要选格瞄准（指向攻击目标）。 */
	public static String targetingPrompt(){
		return Messages.get(PalermoFencing.class, "prompt");
	}

	/** 武技说明（显示于武器信息面板）。 */
	public static String abilityInfo(){
		return Messages.get(PalermoFencing.class, "ability_info");
	}

	/**
	 * 施放巴勒莫剑术。
	 *
	 * @param hero   施放者（拇指 前二老板）
	 * @param target 目标格
	 * @param wep    当前装备的近战武器（用于射程/伤害/扣费钩子，武技本身不随武器变化）
	 */
	public static void cast( final Hero hero, Integer target, final MeleeWeapon wep ){

		if (target == null){
			return;
		}

		final Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy )){
			GLog.w( Messages.get( PalermoFencing.class, "no_target" ) );
			return;
		}

		int dist = Dungeon.level.distance( hero.pos, enemy.pos );
		if (dist > RANGE){
			GLog.w( Messages.get( PalermoFencing.class, "out_of_range" ) );
			return;
		}

		//家族之耻「极速追杀」：巴勒莫剑术使用前解除自身扎根/残废、视层数给予极速/时间气泡
		//（扎根解除须先于下方 rooted 阻断，否则被定身时根本进不了剑术）
		if (hero.subClass == HeroSubClass.FAMILY_SHAME && hero.hasTalent( Talent.VALENCINA_T3_SHAME_1 )){
			applyPursuitPrecast( hero );
		}

		if (hero.rooted){
			GLog.w( Messages.get( PalermoFencing.class, "out_of_range" ) );
			return;
		}

		//决定落点：目标周围的一个空位（已紧贴时原地挥击）
		final int dest;
		if (dist <= 1){
			dest = hero.pos;
		} else {
			dest = landingPos( hero, enemy );
			if (dest == -1){
				GLog.w( Messages.get( PalermoFencing.class, "blocked" ) );
				return;
			}
		}

		//语音：本次巴勒莫剑术的台词（四选一；上一条尚未播完则本次跳过）
		ValencinaSfx.playPalermoVoice();

		hero.busy();
		AttackIndicator.target( enemy );

		if (dest == hero.pos){
			hero.sprite.attack( enemy.pos, new Callback() {
				@Override
				public void call() {
					strike( hero, enemy, wep );
				}
			} );
		} else {
			hero.sprite.jump( hero.pos, dest, new Callback() {
				@Override
				public void call() {
					hero.move( dest );
					Dungeon.level.occupyCell( hero );
					Dungeon.observe();
					GameScene.updateFog();
					hero.sprite.attack( enemy.pos, new Callback() {
						@Override
						public void call() {
							strike( hero, enemy, wep );
						}
					} );
				}
			} );
		}
	}

	/** 计算突进落点：优先目标正前方（直线路径紧邻目标的一格），否则绕到目标侧面最近的一格；无合适落点返回 -1。 */
	private static int landingPos( Hero hero, Char enemy ){

		//1. 直线突进：目标正前方一格
		Ballistica line = new Ballistica( hero.pos, enemy.pos, Ballistica.PROJECTILE );
		if (line.collisionPos == enemy.pos && line.dist > 1){
			int front = line.path.get( line.dist - 1 );
			if (canLand( hero, front )){
				return front;
			}
		}

		//2. 绕侧：目标周围、从英雄处直线可达的空位中最近的一格
		int best = -1;
		int bestDist = Integer.MAX_VALUE;
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
			int n = enemy.pos + PathFinder.NEIGHBOURS8[i];
			if (!Dungeon.level.insideMap( n ) || !canLand( hero, n )){
				continue;
			}
			Ballistica side = new Ballistica( hero.pos, n, Ballistica.PROJECTILE );
			if (side.collisionPos != n){
				continue; //中间被挡，跳过去会被墙体拦住
			}
			int d = Dungeon.level.distance( hero.pos, n );
			if (d < bestDist){
				bestDist = d;
				best = n;
			}
		}
		return best;
	}

	/** 落点是否可站：可通行（或飞行者落在坑洞上空）且没有其它角色占据。 */
	private static boolean canLand( Hero hero, int cell ){
		if (Actor.findChar( cell ) != null) return false;
		return Dungeon.level.passable[cell] || (hero.flying && Dungeon.level.avoid[cell]);
	}

	/** 挥出斩击：读层→按层数定段→多段连斩→整套结束后统一清层结算耗时。
	 *  「加速的未来」不在施展瞬间清零，而是保留到整套斩击结束——使限定“持有/层数”的分支天赋
	 *  在剑术自身的攻击上同样生效（攻击钩子读取的是实时 buff）。 */
	private static void strike( final Hero hero, final Char enemy, final MeleeWeapon wep ){

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;
		castStacks = stacks;
		tremorApplied = false;   //本次施放震颤-灼热尚未施加（首段命中时置位）
		executeBonusPending = false; //本次施放斩杀奖励尚未结算

		wep.beforeAbilityUsed( hero, enemy );

		float delay = hero.attackDelay();
		//基础伤害 ÷ 攻击延迟：按“平均每回合伤害”结算。
		//注意：绝不能把 delay 钳制到 ≥1——攻击延迟 <1 的快武器（如磨损对剑 0.5）需要 ÷0.5=×2 放大；
		//原 max(delay,1f) 会把这类快武器压回 ×1，慢武器（>1）则被错误缩小。
		float dmgMulti = (delay > 0f) ? 1f / delay : 1f;

		//1 层以上：本次斩击必中（无层数时保留普通命中判定，即无必中属性）
		float accMulti = (stacks >= 1) ? Char.INFINITE_ACCURACY : 1f;

		//战争英雄 分支特化天赋三：多段斩（每段独立攻击、伤害均分、段间重选落点）
		int segs = fencingSegments( hero, stacks );
		if (segs > 1){
			dmgMulti /= segs;
		}

		//家族之耻：预知眼过热期间剑术额外增伤 50%；「叠加剑轨」按层数追加增伤（过热时每层再 +5%）
		if (hero.subClass == HeroSubClass.FAMILY_SHAME){
			boolean overheated = hero.buff( OdinsEyeOverheat.class ) != null;
			if (overheated){
				dmgMulti *= 1.5f;
			}
			int railPoints = hero.pointsInTalent( Talent.VALENCINA_T3_SHAME_2 );
			int rail = Talent.ValencinaSwordRail.stacks( hero );
			if (railPoints > 0 && rail > 0){
				float perStack = (10f + 5f * (railPoints - 1) + (overheated ? 5f : 0f)) / 100f;
				dmgMulti *= 1f + perStack * rail;
			}
		}

		//整套连击期间抑制「加速的未来」层数获取（本武技自身不产生层数）
		AcceleratingFuture.suppressStrike( true );

		//从当前落点开始第一段
		fencingSequence( hero, enemy, wep, segs, 0, stacks, dmgMulti, accMulti, delay );
	}

	/** 战争英雄 分支特化天赋三：按「加速的未来」层数与加点决定段数（1/2/4/8）。 */
	private static int fencingSegments( Hero hero, int stacks ){
		if (hero != null && hero.heroClass == HeroClass.VALENCINA
				&& hero.subClass == HeroSubClass.WAR_HERO
				&& hero.hasTalent( Talent.VALENCINA_T3_WARHERO_3 )){
			int points = hero.pointsInTalent( Talent.VALENCINA_T3_WARHERO_3 );
			if (points >= 3 && stacks >= 5) return 8;
			if (points >= 2 && stacks >= 3) return 4;
			if (points >= 1 && stacks >= 1) return 2;
		}
		return 1;
	}

	/** 依次执行各段：第 idx>0 段先重选一个目标周围的随机空位移动过去再斩。 */
	private static void fencingSequence( final Hero hero, final Char enemy, final MeleeWeapon wep,
	                                     final int segs, final int idx, final int stacks,
	                                     final float dmgMulti, final float accMulti, final float delay ){
		if (idx > 0 && !hero.rooted){
			int dest = randomLanding( hero, enemy );
			if (dest != -1 && dest != hero.pos){
				hero.sprite.jump( hero.pos, dest, new Callback() {
					@Override
					public void call() {
						hero.move( dest );
						Dungeon.level.occupyCell( hero );
						Dungeon.observe();
						GameScene.updateFog();
						hitSegment( hero, enemy, wep, segs, idx, stacks, dmgMulti, accMulti, delay );
					}
				} );
				return;
			}
		}
		hitSegment( hero, enemy, wep, segs, idx, stacks, dmgMulti, accMulti, delay );
	}

	/** 斩出第 idx 段（独立攻击）。命中后结算旧通用特效，若还有下一段则续斩。 */
	private static void hitSegment( final Hero hero, final Char enemy, final MeleeWeapon wep,
	                                final int segs, final int idx, final int stacks,
	                                final float dmgMulti, final float accMulti, final float delay ){

		//标记本段为巴勒莫剑术的一段（Talent 命中钩子据此施加震颤-灼热等）
		fencingSegment = true;
		boolean hit;
		try {
			hit = hero.attack( enemy, dmgMulti, 0, accMulti );
		} finally {
			fencingSegment = false;
		}

		if (hit){
			//攻击音效：技能3 的前八条随机其一（取代原版的 HIT_STRONG）
			ValencinaSfx.playPalermoHit();

			//旧通用特效（战争英雄分支下与新机制叠加共存）：
			//3 层以上：命中后麻痹 5 回合
			if (stacks >= 3){
				Buff.affect( enemy, Paralysis.class, 5f );
			}
			//5 层以上：麻痹 5 回合并点燃目标
			if (stacks >= 5){
				Buff.affect( enemy, Burning.class ).reignite( enemy );
			}

			if (!enemy.isAlive()){
				MeleeWeapon.onAbilityKill( hero, enemy );
			}
		}

		//家族之耻「宰杀处置」：剑术命中后直接斩杀血量低于阈值的非 boss 敌人（过热时斩杀线 +10%）
		//（命中才可触发；斩杀成功则整套剑术结束时额外 +3 层「加速的未来」）
		if (hit && enemy.isAlive()
				&& hero.subClass == HeroSubClass.FAMILY_SHAME
				&& hero.hasTalent( Talent.VALENCINA_T3_SHAME_3 )){
			if (tryExecute( hero, enemy )){
				executeBonusPending = true;
			}
		}

		if (idx + 1 < segs && hero.isAlive() && enemy.isAlive()){
			fencingSequence( hero, enemy, wep, segs, idx + 1, stacks, dmgMulti, accMulti, delay );
		} else {
			finishStrike( hero, delay, wep );
		}
	}

	/** 整套斩击结束：解除抑制、清空「加速的未来」（家族之耻返还一半、斩杀奖励 +3 在此结算）、
	 *  结算耗时与武技后置钩子。 */
	private static void finishStrike( Hero hero, float delay, MeleeWeapon wep ){
		AcceleratingFuture.suppressStrike( false );

		boolean familyShame = hero.subClass == HeroSubClass.FAMILY_SHAME;
		int consumed = castStacks;

		//家族之耻「叠加剑轨」：本次施放结束后 +1 层（供下一次剑术增伤，上限 5）
		if (familyShame && hero.hasTalent( Talent.VALENCINA_T3_SHAME_2 )){
			Talent.ValencinaSwordRail.addStack( hero );
		}

		AcceleratingFuture.lose( hero );
		//家族之耻核心：返还一半（向下取整）被清零的「加速的未来」，支撑剑术连发
		if (familyShame && consumed > 0){
			AcceleratingFuture.gain( hero, consumed / 2 );
		}
		//家族之耻「宰杀处置」：触发斩杀额外获得 3 层
		if (familyShame && executeBonusPending){
			executeBonusPending = false;
			AcceleratingFuture.gain( hero, 3 );
		}

		fencingSegment = false;
		castStacks = 0;
		tremorApplied = false;   //防御性复位（正常路径下 strike 已在下一次施放开头复位）

		Invisibility.dispel();
		//连击中被反伤/荆棘致死时不再结算动作（死亡流程自行接管）
		if (!hero.isAlive()) return;
		hero.spendAndNext( delay );
		wep.afterAbilityUsed( hero );
	}

	/** 家族之耻「极速追杀」预施效果：按「加速的未来」层数与过热档位，在剑术使用前解除自身扎根/残废、
	 *  给予极速与时间气泡。过热时天赋档位视为 +1（只点 +1 可触发 +2 效果、只点 +2 可触发 +3 效果），
	 *  满级 + 过热且 ≥7 层额外给予 3 回合时间气泡。 */
	private static void applyPursuitPrecast( Hero hero ){
		int points = hero.pointsInTalent( Talent.VALENCINA_T3_SHAME_1 );
		if (points <= 0) return;

		boolean overheated = hero.buff( OdinsEyeOverheat.class ) != null;
		int eff = overheated ? Math.min( 3, points + 1 ) : points;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;

		//+1 档（≥1 层）：解除自身扎根
		if (stacks >= 1){
			Buff.detach( hero, Roots.class );
		}
		//+2 档（≥3 层）：解除自身残废
		if (eff >= 2 && stacks >= 3){
			Buff.detach( hero, Cripple.class );
		}
		//+3 档（≥5 层）：给予 3 回合极速
		if (eff >= 3 && stacks >= 5){
			Buff.prolong( hero, Haste.class, 3f );
		}
		//过热 + 满级 +（≥7 层）：额外给予 3 回合时间气泡
		if (overheated && points >= 3 && stacks >= 7){
			Buff.affect( hero, Swiftthistle.TimeBubble.class ).reset( 3 );
		}
	}

	/** 家族之耻「宰杀处置」：目标非 boss/miniboss 且生命低于斩杀线（(10+10×点数)%，过热时 +10）时处决之。
	 *  处决本身不结算为剑术普通击杀（不占用命中后的普通死亡路径）。 */
	private static boolean tryExecute( Hero hero, Char enemy ){
		int points = hero.pointsInTalent( Talent.VALENCINA_T3_SHAME_3 );
		if (points <= 0) return false;
		if (Char.hasProp( enemy, Char.Property.BOSS ) || Char.hasProp( enemy, Char.Property.MINIBOSS )) return false;

		boolean overheated = hero.buff( OdinsEyeOverheat.class ) != null;
		float threshold = 0.1f * (points + 1 + (overheated ? 1 : 0)); //+1/2/3 → 20/30/40%，过热 +10%
		if (enemy.HP / (float) enemy.HT >= threshold) return false;

		enemy.HP = 0;
		if (enemy.buff( Brute.BruteRage.class ) != null){
			enemy.buff( Brute.BruteRage.class ).detach();
		}
		if (!enemy.isAlive()) {
			enemy.die( hero );
		} else {
			//helps with triggering any on-damage effects that need to activate
			enemy.damage( -1, hero );
			DeathMark.processFearTheReaper( enemy );
		}
		if (enemy.sprite != null){
			enemy.sprite.showStatus( CharSprite.NEGATIVE, Messages.get( PalermoFencing.class, "executed" ) );
		}
		MeleeWeapon.onAbilityKill( hero, enemy );
		//瞄准心脏「荣耀凯旋」：处决同样结算击杀被标记目标的恢复 —— 但判定已收口到
		//Mob.die() 的「带着标记倒下」处（上面这行 enemy.die() 就会走到），这里不再重复判。
		//⚠️ 原来那句 `enemy.buff(AimHeartMark.class) != null` 是**死代码**：die() → destroy()
		//   → Actor.remove → onRemove 会把 buff 全摘掉，所以它读到的永远是 null。
		return true;
	}

	/** 多段斩的段间落点：目标 8 邻格的随机空位；无空位返回 -1（保持原位）。 */
	private static int randomLanding( Hero hero, Char enemy ){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++){
			int n = enemy.pos + PathFinder.NEIGHBOURS8[i];
			if (Dungeon.level.insideMap( n ) && canLand( hero, n )){
				candidates.add( n );
			}
		}
		if (candidates.isEmpty()) return -1;
		return Random.element( candidates );
	}

	//==========================================================================
	// 盔甲技能「处置」（2026-09-08）与免费剑术（瞄准心脏「碾杀虫豸」）
	//==========================================================================

	/**
	 * 「处置」：对目标连续施展 4 次巴勒莫剑术，每次命中后击退目标 1 格并追及。
	 * 前 3 次耗时减半、第 4 次完整（总耗时 = 2.5 × 当前攻击延迟）。
	 * 「枪拼刺杀」豁免前 N 次剑术对「加速的未来」的消耗（首次非豁免段结束后才清空层数）。
	 * 「爆碎收尾」在整套结束后追加一段收尾伤害；「毫无悬念」在处置击杀敌人后恢复盔甲充能。
	 */
	public static boolean disposal( final Hero hero, Integer target, final MeleeWeapon wep, final ClassArmor armor ){
		if (target == null || hero == null) return false;
		final Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy )){
			GLog.w( Messages.get( PalermoFencing.class, "no_target" ) );
			return false;
		}
		if (hero.rooted){
			GLog.w( Messages.get( PalermoFencing.class, "blocked" ) );
			return false;
		}
		int dist = Dungeon.level.distance( hero.pos, enemy.pos );
		if (dist > RANGE){
			GLog.w( Messages.get( PalermoFencing.class, "out_of_range" ) );
			return false;
		}

		final int dest;
		if (dist <= 1){
			dest = hero.pos;
		} else {
			dest = landingPos( hero, enemy );
			if (dest == -1){
				GLog.w( Messages.get( PalermoFencing.class, "blocked" ) );
				return false;
			}
		}

		final float baseDelay = Math.max( 0.01f, hero.attackDelay() );
		final int exempt = hero.pointsInTalent( Talent.LUNGE_STAB ); //枪拼刺杀：前 N 次不消耗

		//可变状态
		final float[] total = {0f};
		final boolean[] consumed = {false};
		final int[] spent = {0};
		final boolean[] killed = {false};

		//语音 + 头顶台词：本次「处置」的两条台词成对随机取用（防止显示 A、念的却是 B）
		ValencinaSfx.playDisposalVoice( hero );

		hero.busy();
		AttackIndicator.target( enemy );
		wep.beforeAbilityUsed( hero, enemy );

		//整套连击期间抑制「加速的未来」获取（与单次剑术一致）
		AcceleratingFuture.suppressStrike( true );
		tremorApplied = false;
		executeBonusPending = false;

		Callback strikeStart = new Callback() {
			@Override
			public void call() {
				disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, 0, total, consumed, spent, killed );
			}
		};

		if (dest == hero.pos){
			hero.sprite.attack( enemy.pos, strikeStart );
		} else {
			hero.sprite.jump( hero.pos, dest, new Callback() {
				@Override
				public void call() {
					hero.move( dest );
					Dungeon.level.occupyCell( hero );
					Dungeon.observe();
					GameScene.updateFog();
					hero.sprite.attack( enemy.pos, strikeStart );
				}
			} );
		}
		//所有前置校验均通过、连击已开始 → 返回 true（调用方才扣充能）
		return true;
	}

	/** 处置的第 idx 段（0~3）。 */
	private static void disposalStrike( final Hero hero, final Char enemy, final MeleeWeapon wep, final ClassArmor armor,
	                                    final float baseDelay, final int exempt, final int idx,
	                                    final float[] total, final boolean[] consumed, final int[] spent,
	                                    final boolean[] killed ){

		//耗时：前三段减半，第四段完整
		total[0] += (idx < 3) ? baseDelay * 0.5f : baseDelay;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;
		float dmgMulti = (baseDelay > 0f) ? 1f / baseDelay : 1f;
		float accMulti = (stacks >= 1) ? Char.INFINITE_ACCURACY : 1f;

		boolean hit;
		fencingSegment = true;
		try {
			hit = hero.attack( enemy, dmgMulti, 0, accMulti );
		} finally {
			fencingSegment = false;
		}

		if (hit){
			//攻击音效：与单次剑术同一组（技能3 前八条随机其一，取代原版 HIT_STRONG）
			ValencinaSfx.playPalermoHit();
			//旧通用特效（同单次剑术）
			if (stacks >= 3){
				Buff.affect( enemy, Paralysis.class, 5f );
			}
			if (stacks >= 5){
				Buff.affect( enemy, Burning.class ).reignite( enemy );
			}
			if (!enemy.isAlive()){
				MeleeWeapon.onAbilityKill( hero, enemy );
				killed[0] = true;
			}
		}

		//家族之耻「宰杀处置」：命中后照常尝试斩杀（斩杀奖励在整套结束时结算）
		if (hit && enemy.isAlive()
				&& hero.subClass == HeroSubClass.FAMILY_SHAME
				&& hero.hasTalent( Talent.VALENCINA_T3_SHAME_3 )){
			if (tryExecute( hero, enemy )){
				executeBonusPending = true;
				killed[0] = true;
			}
		}

		if (!enemy.isAlive() || !hero.isAlive()){
			disposalFinish( hero, enemy, wep, armor, total, consumed, spent, killed );
			return;
		}

		//每次施展后击退目标 1 格
		knockBack( hero, enemy, 1 );

		//「枪拼刺杀」：前 N 段不消耗；首次非豁免段结束后清空「加速的未来」（返还等结算于结束时）
		if (!consumed[0] && idx >= exempt){
			af = hero.buff( AcceleratingFuture.class );
			if (af != null && af.getStacks() > 0){
				consumed[0] = true;
				spent[0] = af.getStacks();
				AcceleratingFuture.lose( hero );
			}
		}

		if (idx + 1 < 4 && hero.isAlive() && enemy.isAlive()){
			if (!hero.rooted){
				int land = randomLanding( hero, enemy );
				if (land != -1 && land != hero.pos){
					final int next = idx + 1;
					hero.sprite.jump( hero.pos, land, new Callback() {
						@Override
						public void call() {
							hero.move( land );
							Dungeon.level.occupyCell( hero );
							Dungeon.observe();
							GameScene.updateFog();
							disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, next, total, consumed, spent, killed );
						}
					} );
					return;
				}
			}
			disposalStrike( hero, enemy, wep, armor, baseDelay, exempt, idx + 1, total, consumed, spent, killed );
		} else {
			disposalFinish( hero, enemy, wep, armor, total, consumed, spent, killed );
		}
	}

	/** 处置整套结束：结算耗时、奖励、返还与充能恢复。 */
	private static void disposalFinish( Hero hero, Char enemy, MeleeWeapon wep, ClassArmor armor,
	                                    float[] total, boolean[] consumed, int[] spent, boolean[] killed ){
		AcceleratingFuture.suppressStrike( false );

		boolean familyShame = hero.subClass == HeroSubClass.FAMILY_SHAME;

		//家族之耻「叠加剑轨」：整套处置计为一次剑术施放 → +1 层
		if (familyShame && hero.hasTalent( Talent.VALENCINA_T3_SHAME_2 )){
			Talent.ValencinaSwordRail.addStack( hero );
		}

		//家族之耻核心返还：被清零层数的一半（只在确实发生消耗时）
		if (consumed[0] && spent[0] > 0 && familyShame){
			AcceleratingFuture.gain( hero, spent[0] / 2 );
		}
		//家族之耻「宰杀处置」：处置中触发斩杀 → +3 层
		if (familyShame && executeBonusPending){
			executeBonusPending = false;
			AcceleratingFuture.gain( hero, 3 );
		}

		//「爆碎收尾」：整套结束后对仍存活的目标追加一段收尾伤害
		//（+1 为一次基础剑术伤害的一半，+2 起为全额；+3/+4 附带击退 3 格，+4 再眩晕并麻痹）
		if (hero.isAlive() && enemy != null && enemy.isAlive() && !killed[0]){
			int burst = hero.pointsInTalent( Talent.BURST_FINISH );
			if (burst > 0 && Dungeon.level.distance( hero.pos, enemy.pos ) <= RANGE){
				//第 4 段已把目标击退：先追及贴身；被定身或无处落脚则放弃本次收尾
				//（注意：不能在此 return——必须走到末尾 spendAndNext，否则英雄会永久卡死）
				boolean burstReady = true;
				if (Dungeon.level.distance( hero.pos, enemy.pos ) > 1){
					if (hero.rooted){
						burstReady = false; //无法追及（rooted，正常不会出现）
					} else {
						int land = randomLanding( hero, enemy );
						if (land == -1 || land == hero.pos){
							burstReady = false;
						} else {
							hero.move( land );
							Dungeon.level.occupyCell( hero );
							Dungeon.observe();
							GameScene.updateFog();
						}
					}
				}
				if (burstReady){
					float burstDelay = Math.max( 0.01f, hero.attackDelay() );
					float burstDmg = ((burst >= 2) ? 1f : 0.5f) / burstDelay;
					boolean burstHit;
					fencingSegment = true;
					try {
						burstHit = hero.attack( enemy, burstDmg, 0, Char.INFINITE_ACCURACY );
					} finally {
						fencingSegment = false;
					}
					if (burstHit && enemy.isAlive()){
						if (burst >= 3){
							knockBack( hero, enemy, 3 );
						}
						if (burst >= 4){
							Buff.affect( enemy, Vertigo.class, 5f );
							Buff.affect( enemy, Paralysis.class, 5f );
						}
					}
				}
			}
		}

		fencingSegment = false;
		castStacks = 0;
		tremorApplied = false;

		//「毫无悬念」：处置击杀恢复盔甲充能 20/40/60/80%
		int noSus = hero.pointsInTalent( Talent.NO_SUSPENSE );
		if (killed[0] && noSus > 0 && armor != null){
			armor.charge = Math.min( 100f, armor.charge + 20f * noSus );
			armor.updateQuickslot();
		}

		Invisibility.dispel();
		if (!hero.isAlive()) return;
		hero.spendAndNext( total[0] );
		wep.afterAbilityUsed( hero );
	}

	/**
	 * 免费剑术（瞄准心脏「碾杀虫豸」触发）：完整的一次巴勒莫剑术判定，
	 * 但不占回合、不消耗「加速的未来」。层数仍然提供必中/麻痹/点燃等效果。
	 */
	public static void freeSwing( final Hero hero, final Char enemy, final MeleeWeapon wep ){
		if (hero == null || enemy == null || enemy == hero || !enemy.isAlive()) return;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		int stacks = (af != null) ? af.getStacks() : 0;
		float delay = Math.max( 0.01f, hero.attackDelay() );
		float dmgMulti = 1f / delay;
		float accMulti = (stacks >= 1) ? Char.INFINITE_ACCURACY : 1f;

		AcceleratingFuture.suppressStrike( true );
		tremorApplied = false;

		boolean hit;
		fencingSegment = true;
		try {
			hit = hero.attack( enemy, dmgMulti, 0, accMulti );
		} finally {
			fencingSegment = false;
		}

		if (hit){
			//攻击音效：与单次剑术同一组（技能3 前八条随机其一，取代原版 HIT_STRONG）
			ValencinaSfx.playPalermoHit();
			//旧通用特效（同单次剑术；免费挥击不结算斩杀与剑轨）
			if (stacks >= 3){
				Buff.affect( enemy, Paralysis.class, 5f );
			}
			if (stacks >= 5){
				Buff.affect( enemy, Burning.class ).reignite( enemy );
			}
			if (!enemy.isAlive()){
				MeleeWeapon.onAbilityKill( hero, enemy );
			}
		}

		AcceleratingFuture.suppressStrike( false );
		fencingSegment = false;
	}

	/** 击退：把目标沿远离施放者的方向推动 tiles 格（撞墙/被占/不可移动/Boss 时停在原地）。 */
	private static void knockBack( Hero hero, Char enemy, int tiles ){
		if (enemy == null || !enemy.isAlive() || enemy == hero) return;
		if (Char.hasProp( enemy, Char.Property.BOSS )
				|| Char.hasProp( enemy, Char.Property.MINIBOSS )
				|| Char.hasProp( enemy, Char.Property.IMMOVABLE )) return;

		int cur = enemy.pos;
		boolean moved = false;
		for (int i = 0; i < tiles; i++){
			int next = pushAwayCell( hero.pos, cur );
			if (next == -1) break;
			cur = next;
			moved = true;
		}
		if (moved){
			int from = enemy.pos;
			enemy.pos = cur;
			Dungeon.level.occupyCell( enemy );
			if (enemy.sprite != null){
				enemy.sprite.place( cur );
			}
			if (Dungeon.level.heroFOV != null && Dungeon.level.heroFOV.length > Math.max(from, cur)){
				GameScene.updateFog();
			}
		}
	}

	/** 计算把 cur 处的单位沿远离 from 的方向推 1 格的目标格（不可行返回 -1）。 */
	private static int pushAwayCell( int from, int cur ){
		int w = Dungeon.level.width();
		int fx = from % w;
		int fy = from / w;
		int cx = cur % w;
		int cy = cur / w;
		int dx = (int)Math.signum( cx - fx );
		int dy = (int)Math.signum( cy - fy );
		if (dx == 0 && dy == 0) return -1;
		int n = cur + dx + dy * w;
		if (!Dungeon.level.insideMap( n )) return -1;
		if (!Dungeon.level.passable[n]) return -1;
		if (Actor.findChar( n ) != null) return -1;
		return n;
	}
}

