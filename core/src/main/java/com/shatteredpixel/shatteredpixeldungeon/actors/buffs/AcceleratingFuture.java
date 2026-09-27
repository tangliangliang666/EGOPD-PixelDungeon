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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 加速的未来 —— 「拇指 前二老板」巴勒莫剑术的层数增益 buff（2026-09-06）
 *
 * <p>获取方式：闪避敌人的攻击、或攻击命中敌人时各获得 1 层；
 * 每次获得都会把持续时间刷新为固定 3 回合（不叠加时长，只刷新）。
 * 施展巴勒莫剑术、或被敌人攻击命中时立即失去全部层数。</p>
 *
 * <p>层数用于强化巴勒莫剑术：1 层以上必中；3 层以上命中麻痹 5 回合；
 * 5 层以上命中再点燃目标。</p>
 *
 * <p>独立顶层类（非 artifact / 能力内部类），保证存档经 Bundle 反射可正常重建。</p>
 */
public class AcceleratingFuture extends Buff {

	/** 单次层数的持续回合数（重复获得时刷新而非累加）。 */
	public static final float DURATION = 3f;

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;
	private float timeLeft = 0f;

	@Override
	public int icon() {
		return BuffIndicator.ACCELERATING_FUTURE;
	}

	//随时间流逝淡出图标（快结束时图标渐隐）
	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - timeLeft) / DURATION);
	}

	//大图标右下角显示当前层数
	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	public int getStacks() {
		return stacks;
	}

	@Override
	public boolean act() {
		timeLeft -= TICK;
		spend(TICK);
		if (timeLeft <= 0){
			detach();
			BuffIndicator.refreshHero();
		}
		return true;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks, dispTurns(timeLeft));
	}

	//==========================================================================
	// 层数管理（静态入口）
	//==========================================================================

	/** 巴勒莫剑术自身那一击是否被标记为“不产生层数”（施展时层数已被清零，不能立刻回一层）。 */
	private static boolean strikeSuppressed = false;

	public static boolean isStrikeSuppressed() {
		return strikeSuppressed;
	}

	public static void suppressStrike( boolean suppress ) {
		strikeSuppressed = suppress;
	}

	/** +1 层：若尚未携带则挂载，并把持续时间刷新为固定 3 回合。 */
	public static void gain( Hero hero ) {
		gain( hero, 1 );
	}

	/** 一次性 +amount 层（返还半层 / 斩杀奖励等批量入口）：同样只刷新一次持续时间。 */
	public static void gain( Hero hero, int amount ) {
		if (hero == null || hero.heroClass != HeroClass.VALENCINA || amount <= 0) return;

		AcceleratingFuture af = hero.buff( AcceleratingFuture.class );
		if (af == null){
			af = Buff.affect( hero, AcceleratingFuture.class );
		}
		af.stacks += amount;
		af.timeLeft = DURATION; //刷新持续时间，不叠加
		BuffIndicator.refreshHero();
	}

	/** 失去全部层数（施展巴勒莫剑术 / 被敌人攻击命中）。 */
	public static void lose( Hero hero ) {
		if (hero == null) return;
		Buff.detach( hero, AcceleratingFuture.class );
		BuffIndicator.refreshHero();
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String STACKS    = "stacks";
	private static final String TIME_LEFT = "timeLeft";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
		bundle.put( TIME_LEFT, timeLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stacks = bundle.getInt( STACKS );
		timeLeft = bundle.getFloat( TIME_LEFT );
	}
}
