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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * 「犯规」护盾（中指长兄 T1「你犯规了」+2）：<b>等待时</b>按「已失去体力 20%」获得的护盾。
 *
 * <p>规则：最高 {@link #MAX_SHIELD} 点、<b>不可叠加</b>（{@code ShieldBuff.setShield()} 只抬高不降低，
 * 所以反复等待只会把护盾补到「当前已失去体力的 20%」，不会越攒越多）。</p>
 *
 * <p><b>补盾</b>：{@code Hero.rest()}——单次等待（{@code rest(false)}）与长按休息（{@code rest(true)}）
 * 都会经过那里，<b>每次等待补一次</b>。</p>
 *
 * <p><b>解除</b>：{@code Hero.act()}——本回合执行了任何动作（移动/攻击/交互/拾取…，即进入
 * {@code curAction != null} 的分支）就立刻清掉。站着不动（等待/空闲）时护盾保留。</p>
 *
 * <p><b>刻意不做的两件事</b>（否则就不是「护盾」而是「护甲」了）：</p>
 * <ol>
 *   <li>不每回合 refresh：等待期间每回合回满 = 常驻减伤；本实现里护盾是<b>一次性</b>的，
 *       补上后被打掉就没了，要再挨打就得重新等待一次。</li>
 *   <li>不进 {@code drRoll()}：护盾只走 {@link ShieldBuff#processDamage} 在 {@code Char.damage()}
 *       里被消耗，与护甲（{@code Hero.drRoll()} 的随机减伤掷点）是两条完全独立的路。</li>
 * </ol>
 *
 * <p>与 {@link Barrier} 的区别：<b>没有自然衰减</b>，只在挨打时被消耗；归零由
 * {@link ShieldBuff#detachesAtZero} 自动分离。</p>
 *
 * <p><b>顶层类</b>：参与存档反射重建，不能做成非静态内部类。</p>
 */
public class CheatGuard extends ShieldBuff {

	{
		type = buffType.POSITIVE;
	}

	/** 护盾上限（点）。 */
	public static final int MAX_SHIELD = 5;
	/** 护盾 = 已失去体力 × 该系数。 */
	public static final float LOST_HP_FACTOR = 0.2f;

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 按「当前已失去体力的 20%（最高 5 点）」把护盾补到该值；已满血时直接解除。
	 *  由 {@code Hero.rest()} 在<b>每次等待时调用一次</b>（不是每回合轮询）。 */
	public static void refresh( Hero hero ){
		if (hero == null || !hero.isAlive()) return;

		int lost = Math.max( 0, hero.HT - hero.HP );
		int amount = Math.min( MAX_SHIELD, Math.round( lost * LOST_HP_FACTOR ) );
		if (amount <= 0){
			clear( hero );
			return;
		}

		CheatGuard guard = hero.buff( CheatGuard.class );
		if (guard == null){
			guard = Buff.affect( hero, CheatGuard.class );
		}
		//Buff.append 会吞掉 attachTo 的失败结果，可能拿到 target == null 的幽灵对象
		if (guard == null || guard.target == null) return;

		guard.setShield( amount ); //只抬高、不降低 ⇒ 天然「不可叠加」
	}

	/** 收回护盾（移动 / 结束等待时调用）。 */
	public static void clear( Hero hero ){
		if (hero != null) Buff.detach( hero, CheatGuard.class );
	}

	//==========================================================================
	// 行为
	//==========================================================================

	@Override
	public boolean act(){
		//无自然衰减：只在挨打（absorbDamage）时减少，归零由 detachesAtZero 自动 detach
		if (target == null || !target.isAlive()){
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		if (target == null) return;
		if (on){
			target.sprite.add( CharSprite.State.SHIELDED );
		} else if (target.buff( Blocking.BlockBuff.class ) == null ){
			//与「格挡」雕文的护盾共用 SHIELDED 状态，别把它一起摘掉（同 Barrier / TearShield 的处理）
			target.sprite.remove( CharSprite.State.SHIELDED );
		}
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon(){
		//暂无自绘图标 ⇒ 借原版「护盾」帧 20（BuffIndicator.ARMOR）：原版所有护盾类 buff 都用它
		//（Barrier / TearShield / 格挡附魔 Blocking / 坚守阵地 HoldFast），是本工程护盾的通用图标。
		//按 BuffIndicator 里的【惯例】条目，未自绘的 buff 不预留帧号常量——等画好后
		//按当时的空帧顺序在 BuffIndicator 里新增常量，再把这里改过去。
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon( Image icon ){
		icon.hardlight( 0.85f, 0.5f, 1.1f );
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( shielding() );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", shielding(), MAX_SHIELD );
	}
}
