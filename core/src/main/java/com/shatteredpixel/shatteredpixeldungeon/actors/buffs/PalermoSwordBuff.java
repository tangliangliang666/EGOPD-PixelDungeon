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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 连斩 —— 「巴勒莫对剑」的叠层增益 buff（2026-09-08，帧 111 用户已绘制）。
 *
 * <p>效果：每次攻击后叠加 1 层（上限 {@link #MAX_STACKS}=5），每层降低 10% 攻击延迟
 * （层间线性，{@link #delayMultiplier(int)} = 1 - 0.1×层数，满层攻击延迟减半）。
 * 与加速弹 / 加速剑法 / 心-不光彩等其它加速来源为「乘法叠加」（见 Hero.attackDelay 的逐项相乘）。</p>
 *
 * <p>持续时间固定 {@link #DURATION}=3 回合，每次攻击刷新（不叠加时长）；
 * 若 3 回合内不再攻击则层数全部消失。</p>
 *
 * <p>独立顶层类（非能力/武器内部类），保证存档经 Bundle 反射可正常重建。</p>
 */
public class PalermoSwordBuff extends Buff {

	/** 单次层数的持续回合数（重复攻击时刷新而非累加）。 */
	public static final float DURATION = 3f;

	/** 层数上限：满层时攻击延迟减半（-50%）。 */
	public static final int MAX_STACKS = 5;

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;
	private float timeLeft = 0f;

	@Override
	public int icon() {
		return BuffIndicator.PALERMO_SWORD;
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

	public int stacks() {
		return stacks;
	}

	/** 指定层数下的攻击延迟乘子：每层 -10%（线性），0 层时 =1、满层 5 时 =0.5。 */
	public static float delayMultiplier( int stacks ) {
		return 1f - 0.1f * stacks;
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
		return Messages.get(this, "desc", stacks, MAX_STACKS, dispTurns(timeLeft));
	}

	//==========================================================================
	// 层数管理（静态入口）
	//==========================================================================

	/** 每次攻击后调用：+1 层（上限 MAX_STACKS），并将持续时间刷新为 DURATION。 */
	public static void gain( Hero hero ) {
		if (hero == null) return;
		PalermoSwordBuff b = hero.buff( PalermoSwordBuff.class );
		if (b == null){
			b = Buff.affect( hero, PalermoSwordBuff.class );
		}
		b.stacks = Math.min( MAX_STACKS, b.stacks + 1 );
		b.timeLeft = DURATION; //刷新持续时间，不叠加
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
