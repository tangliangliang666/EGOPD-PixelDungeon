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
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 「临时力量」（中指长兄 T1「健身一餐」）：进食后获得的<b>限时力量加成</b>。
 *
 * <p>数值（+1/+2 点）与时长（75/150 回合）都由 {@code Talent.onFoodEaten()} 灌入，
 * <b>覆盖式刷新</b>而不是叠层——再次进食会把点数与剩余时长整体重置为当时的食物档位。</p>
 *
 * <p>实际生效点在 {@code Hero.STR()}：这是全工程唯一的力量汇聚点，
 * 戒指 / 复仇账簿「仇怨」/ 壮汉天赋都从那里加算。</p>
 *
 * <p>继承 {@link FlavourBuff}：时间到自动分离，所以直接吃 {@code Buff.affect(ch, cls, duration)}
 * 那套时长语义。</p>
 */
public class TempStrength extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	private int amount = 0;

	public int amount(){
		return amount;
	}

	/** 覆盖式刷新剩余时长（不是叠加）。{@code spend} 走 {@code Actor.time}，与 AdrenalineSurge.reset 同法。 */
	public void setTurns( float turns ){
		spend( turns - cooldown() );
	}

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 给 {@code ch} 施加/刷新临时力量：{@code amount} 点、{@code turns} 回合。 */
	public static void apply( Char ch, int amount, float turns ){
		if (ch == null || amount <= 0 || turns <= 0) return;

		TempStrength buff = Buff.affect( ch, TempStrength.class );
		//Buff.append 会吞掉 attachTo 的失败结果，可能拿到 target == null 的幽灵对象
		if (buff == null || buff.target == null) return;

		buff.amount = amount;
		buff.setTurns( turns );
		BuffIndicator.refreshHero();
	}

	/** 当前临时力量点数（没有该 buff 时为 0），供 {@code Hero.STR()} 取用。 */
	public static int amountOf( Char ch ){
		TempStrength buff = (ch == null) ? null : ch.buff( TempStrength.class );
		return (buff != null) ? buff.amount : 0;
	}

	//==========================================================================
	// 显示 / 存档
	//==========================================================================

	@Override
	public int icon(){
		//暂无自绘图标 ⇒ 借原版「增强」帧 50（BuffIndicator.UPGRADE，被 肾上腺素/肉体强化/强化戒指 共用）：
		//它是原版里语义最接近「临时提升某项属性」的图标。
		//按 BuffIndicator 里的【惯例】条目，未自绘的 buff 不预留帧号常量——等画好后
		//按当时的空帧顺序在 BuffIndicator 里新增常量，再把这里改过去。
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ){
		//暖橙着色：与同帧的原版 buff（肾上腺素等）区分开
		icon.hardlight( 1f, 0.45f, 0.2f );
	}

	@Override
	public String desc(){
		//FlavourBuff.dispTurns() 返回 String ⇒ 模板里用 %s；剩余回合数另由 iconTextDisplay() 直接显示
		return Messages.get( this, "desc", amount, dispTurns() );
	}

	private static final String AMOUNT = "amount";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( AMOUNT, amount );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		amount = bundle.getInt( AMOUNT );
	}
}
