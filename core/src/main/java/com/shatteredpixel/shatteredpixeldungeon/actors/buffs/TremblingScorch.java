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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 震颤-灼热 —— 「拇指 前二老板」战争英雄分支的专属标记 debuff（2026-09-07）。
 *
 * <p>被巴勒莫剑术命中时叠加层数（基础 3 层 + 「加速的未来」档位额外层数）。</p>
 *
 * <p>携带期间还有两个辅助效果：目标每次受伤（damage>0）都会额外掷一次
 * （层数 × 15%）的独立破麻判定（见 {@link Paralysis#processDamage(int)}）；目标身上的
 * 「刷新式」燃烧施加会改为叠加回合数（见 {@link Burning#reignite(Char, float)}）。</p>
 *
 * <p>每当目标解除麻痹（自然结束 / 原生受伤概率打断 / 震颤-灼热附加判定 / 被驱散）时触发：
 * 受到等同（燃烧剩余回合数 ÷ 2）的火焰伤害并消耗 1 层；若免疫火焰则改为等量物理伤害，
 * 随后剩余燃烧回合数减半。解除事件同时触发战争英雄分支天赋一 +2（补 2 回合燃烧）、
 * 天赋二 +2/+3（攻击解除时概率重麻、高层的巴勒莫剑术 50% 不消耗层数）。</p>
 */
public class TremblingScorch extends Buff {

	{
		type = buffType.NEGATIVE;
	}

	public int stacks = 1;

	//每回合观察：麻痹是否在本 buff 两次 act 之间被移除（自然结束等非攻击解除）
	private boolean observedParalysis = false;

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )){
			observedParalysis = target.buff( Paralysis.class ) != null;
			return true;
		}
		return false;
	}

	@Override
	public int icon() {
		return BuffIndicator.TREMOR_SCORCH;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( stacks );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", stacks );
	}

	@Override
	public boolean act() {
		if (target == null || !target.isAlive()){
			detach();
			return true;
		}

		boolean par = target.buff( Paralysis.class ) != null;
		if (observedParalysis && !par){
			//自然结束 / 被打断 / 被驱散 —— 未由英雄攻击钩子即时处理的那类解除
			onParalysisLifted( target, false, Dungeon.hero, false, 0 );
		}
		//连锁可能在 onParalysisLifted 中扣光层数并把本 buff 移除；此时不可再继续调度
		if (target.buff( TremblingScorch.class ) != this){
			return true;
		}
		observedParalysis = par;

		spend( TICK );
		return true;
	}

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 叠加层数（不足 1 层或无目标时忽略）。 */
	public static void addStacks( Char target, int amount ){
		if (target == null || !target.isAlive() || amount <= 0) return;
		TremblingScorch ts = target.buff( TremblingScorch.class );
		if (ts == null){
			ts = Buff.affect( target, TremblingScorch.class );
		}
		ts.stacks += amount;
	}

	/** 施加燃烧：战争英雄统一入口（震颤-灼热在场时 Burning.reignite 会自动把刷新改为叠加）。 */
	public static void applyBurning( Char target, float duration ){
		if (target == null || !target.isAlive()) return;
		Buff.affect( target, Burning.class ).reignite( target, duration );
	}

	/**
	 * 目标解除麻痹的统一处理链（注意顺序：先按当前燃烧结算引爆伤害，再补 2 回合燃烧
	 * ——补燃发生在扣层之前，因此仍享受“叠加回合”加成；随后扣层并视天赋补麻）。
	 *
	 * @param byHeroAttack  是否由英雄本次攻击造成解除（原生受伤概率打断）
	 * @param hero          相关英雄（用于分支天赋判定；非攻击解除时传 Dungeon.hero）
	 * @param fencingHit    该次攻击是否为巴勒莫剑术的一段
	 * @param fencingStacks 该次巴勒莫剑术施放开始时的「加速的未来」层数
	 */
	public static void onParalysisLifted( Char target, boolean byHeroAttack, Hero hero,
	                                      boolean fencingHit, int fencingStacks ){
		TremblingScorch ts = target.buff( TremblingScorch.class );
		if (ts == null || !target.isAlive()) return;

		//子职业本身就是职业独有的（蜕变卷轴不发子职业），按子职业判即可——
		//不必再叠一层 heroClass == VALENCINA 的替身判定。
		boolean heroValid = (hero != null && hero.subClass == HeroSubClass.WAR_HERO);

		//1) 引爆：燃烧剩余回合数 ÷ 2 的火焰伤害；免疫火焰 → 等量物理伤害
		Burning burn = target.buff( Burning.class );
		int amount = (burn != null) ? Math.round( burn.turnsLeft() / 2f ) : 0;
		if (amount > 0){
			if (!target.isImmune( Burning.class )){
				target.damage( amount, burn );
			} else {
				//以震颤-灼热自身为伤害源（无元素属性，走物理减免路径）
				target.damage( amount, ts );
			}
		}
		//引爆可能直接致死；目标已死则终止后续（补燃/重麻对尸体无意义）
		if (!target.isAlive()) return;

		//1b) 引爆造成伤害后，剩余燃烧回合数减半（震颤-灼热把「未爆发的火」提前兑现的代价；
		//     减半在补燃之前——天赋一 +2 补的是解除后新燃起的火，不应被旧火的折半规则波及）
		if (burn != null && burn.turnsLeft() > 0f){
			burn.halveTurns();
		}

		int stacksBefore = ts.stacks;

		//2) 分支特化天赋一 +2：每当目标解除麻痹，就补 2 回合燃烧（扣层前，享受叠加）
		if (heroValid && hero.pointsInTalent( Talent.VALENCINA_T3_WARHERO_1 ) >= 2){
			applyBurning( target, 2f );
		}

		//3) 消耗 1 层（巴勒莫剑术 + 天赋二 +3：高层 50% 免扣）
		boolean consume = true;
		if (heroValid && byHeroAttack && fencingHit
				&& hero.pointsInTalent( Talent.VALENCINA_T3_WARHERO_2 ) >= 3
				&& fencingStacks >= 5 && Random.Int( 2 ) == 0){
			consume = false;
		}
		if (consume){
			ts.stacks--;
			if (ts.stacks <= 0){
				Buff.detach( target, TremblingScorch.class );
			}
		}

		//4) 分支特化天赋二 +2：通过攻击解除麻痹时，概率重新麻痹 5 回合
		//   （概率 = 20 + 解除前震颤-灼热层数 × 3）
		if (heroValid && byHeroAttack
				&& hero.pointsInTalent( Talent.VALENCINA_T3_WARHERO_2 ) >= 2
				&& !target.isImmune( Paralysis.class )
				&& Random.Int( 100 ) < 20 + 3 * stacksBefore){
			Buff.prolong( target, Paralysis.class, 5f );
		}

		//本次解除已同步处理：更新观察基线，避免观察器下次 act 对同一解除事件重复触发
		if (target.buff( TremblingScorch.class ) == ts){
			ts.observedParalysis = false;
		}
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String STACKS = "stacks";
	private static final String OBSERVED_PARALYSIS = "observedParalysis";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
		bundle.put( OBSERVED_PARALYSIS, observedParalysis );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stacks = bundle.getInt( STACKS );
		observedParalysis = bundle.getBoolean( OBSERVED_PARALYSIS );
	}
}
