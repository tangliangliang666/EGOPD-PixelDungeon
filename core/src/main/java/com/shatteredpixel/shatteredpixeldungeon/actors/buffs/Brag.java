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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「夸耀」（中指长兄 T1「仔细看好！」）：<b>叠层型正面 buff</b>，每层 +5% 精准（最高 8 层）。
 *
 * <p>层数由 {@code Talent.onHeroAttackBrag()} 在每次攻击命中后灌入（击杀当次多 1 层），
 * 并被 {@code Talent.onHeroAttackMissed()} 在攻击未命中时<b>整条清空</b>——所以它是一条
 * 「顺风滚雪球、一旦打空就归零」的资源，不随时间衰减。</p>
 *
 * <p><b>没有时长</b>：不覆写 {@link #act()}，沿用 {@link Buff#act()} 的默认行为
 * （首回合 {@code diactivate()} 后不再进入行动队列，但 buff 本体一直挂着），
 * 与 {@code Talent.ValencinaSwordRail}（叠加剑轨）同一写法。</p>
 *
 * <p><b>顶层类</b>：会被写进 {@code Char} 的 buff 列表并参与存档反射重建，
 * 不能做成非静态内部类（见 {@code TearShield} 的同类说明）。</p>
 */
public class Brag extends Buff {

	public static final int MAX_STACKS = 8;
	public static final float ACCURACY_PER_STACK = 0.05f;

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;

	public int stacks(){
		return stacks;
	}

	/** 精准乘区（+5%/层），由 {@code Hero.attackSkill()} 取用。 */
	public float accuracyMultiplier(){
		return 1f + ACCURACY_PER_STACK * stacks;
	}

	/** 叠层（上限 {@link #MAX_STACKS}），返回实际增加量。 */
	public int gainStacks( int amount ){
		if (amount <= 0) return 0;
		int before = stacks;
		stacks = Math.min( MAX_STACKS, stacks + amount );
		if (stacks != before) BuffIndicator.refreshHero();
		return stacks - before;
	}

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 给 {@code ch} 叠 {@code amount} 层夸耀（自动建 buff）。 */
	public static void gain( Char ch, int amount ){
		if (ch == null || amount <= 0) return;

		Brag brag = ch.buff( Brag.class );
		if (brag == null){
			brag = Buff.affect( ch, Brag.class );
		}
		//Buff.append 会吞掉 attachTo 的失败结果，可能拿到 target == null 的幽灵对象
		if (brag == null || brag.target == null) return;

		brag.gainStacks( amount );
	}

	/** 清空夸耀（攻击未命中时调用）。 */
	public static void clear( Char ch ){
		if (ch != null) Buff.detach( ch, Brag.class );
	}

	/** 当前层数（没有该 buff 时为 0）。 */
	public static int stacks( Char ch ){
		Brag brag = (ch == null) ? null : ch.buff( Brag.class );
		return (brag != null) ? brag.stacks : 0;
	}

	//==========================================================================
	// 显示 / 存档
	//==========================================================================

	@Override
	public int icon(){
		return BuffIndicator.BRAG;
	}

	//不覆写 tintIcon()：夸耀图标使用自绘原色，不加任何颜色滤镜。

	@Override
	public String iconTextDisplay(){
		return Integer.toString( stacks );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", stacks, Math.round( stacks * ACCURACY_PER_STACK * 100 ) );
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		stacks = bundle.getInt( STACKS );
	}
}
