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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 「莱瓦汀解放」——中指 长兄 盔甲技能「即刻处刑[莱瓦汀]」的专精天赋
 * 「<b>好久没解放到这种程度了</b>」产物（2026-09-17）。
 *
 * <h3>作用</h3>
 * <p>限时给<b>封印之剑系列</b>（{@link SealedSwordBase} 的四个形态，含莱瓦汀）加等级：
 * 1 / 2 / 3 级，持续 10 / 15 / 20 回合（数值入口全在
 * {@link Talent#executionUnleashLevels} 与 {@link Talent#executionUnleashDuration}）。</p>
 *
 * <h3>为什么走 buffedLvl() 而不是 upgrade()</h3>
 * <p>消费点是 {@link SealedSwordBase#buffedLvl()}——那是 {@code KindOfWeapon.min()/max()} 唯一读的等级，
 * 也就是「拆开包装」（{@code UNWRAP}）走的那条路。<b>坚决不碰真实等级</b>：
 * 本系列的 {@code level()} 是「英雄等级 ÷ 5」现算的，一旦把临时等级写进真实等级字段，
 * 形态切换的 {@code copyState} 会把它固化一次、切几次就叠几层，等级立刻雪崩。</p>
 *
 * <p>反过来说，本 buff 的加成是<b>全局</b>的：只要它在身上，英雄手上的<b>任意</b>封印之剑系列武器
 * （主手或副手、任意形态）都会吃到。这与天赋描述「莱瓦汀系列武器获得临时等级」一致——
 * 系列里哪个形态在用，就由它受益。</p>
 *
 * <h3>时长</h3>
 * <p>走 {@link Buff#prolong}（{@code postpone}），即「刷新成这么多回合」而不是「叠加」：
 * 连续两次即刻处刑不会把时长滚雪球，只会把计时器重新压到满。</p>
 *
 * <h3>图标</h3>
 * <p>本作惯例：还没有自绘图标的 buff 一律「借原版帧」、不占帧号常量
 * （见 {@code BuffIndicator} 尾部说明）。这里借 {@link BuffIndicator#UPGRADE}（原版「增强」帧 50）
 * 并叠一层<b>橙金滤镜</b>，与同样借该帧的「临时力量」「背叛之力」区分开。</p>
 */
public class ExecutionUnleashed extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	/** 临时等级（1 / 2 / 3）。 */
	private int levels = 0;

	//==========================================================================
	// 施加 / 查询
	//==========================================================================

	/**
	 * 施加 / 刷新：给出 {@code levels} 级、{@code duration} 回合的临时等级。
	 *
	 * <p>已经挂着时取<b>较大</b>的等级——天赋点数只增不减，取大值是防呆；
	 * 时长则交给 {@link Buff#prolong}（只延不缩），所以短时长的重复施放不会把长时长压回去。</p>
	 */
	public static void apply( Hero hero, int levels, float duration ){
		if (hero == null || levels <= 0 || duration <= 0f) return;

		ExecutionUnleashed buff = Buff.affect( hero, ExecutionUnleashed.class );
		buff.levels = Math.max( buff.levels, levels );
		Buff.prolong( hero, ExecutionUnleashed.class, duration );
	}

	/**
	 * 指定角色身上的临时等级加成（0 = 没有本 buff）。
	 * 消费点是 {@link SealedSwordBase#buffedLvl()}。
	 */
	public static int bonusFor( Char ch ){
		if (ch == null) return 0;
		ExecutionUnleashed buff = ch.buff( ExecutionUnleashed.class );
		return buff == null ? 0 : buff.levels;
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon(){
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ){
		//橙金滤镜（莱瓦汀的烈焰色），与同样借帧 50 的「临时力量」「背叛之力」区分
		icon.hardlight( 1f, 0.72f, 0.35f );
	}

	/**
	 * 描述要同时给出「临时等级」与「剩余回合」，所以不能沿用 {@code FlavourBuff}
	 * 那个只传 {@code dispTurns()} 的默认实现（文本里有两个占位符，
	 * 实参对不上时 {@code Messages.format} 会整串回退成原文，看起来就是乱码）。
	 */
	@Override
	public String desc(){
		return Messages.get( this, "desc", levels, dispTurns() );
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String LEVELS = "levels";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LEVELS, levels );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		levels = bundle.getInt( LEVELS );
	}
}
