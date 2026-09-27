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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 迫近之日：原版「先知护符」的强化形态（2026-09-20）。
 *
 * <h3>新增效果</h3>
 * <ol>
 *   <li><b>探查到隐藏陷阱额外累积升级进度</b>：原版揭示一个隐藏陷阱给 10 点进度，
 *       强化后额外再加 {@value #TRAP_BONUS_EXP} 点（见 {@link TalismanOfForesight#extraTrapExp}）。</li>
 *   <li><b>探查到敌人时施加 {@value #TERROR_DURATION} 回合恐惧</b>
 *       （见 {@link TalismanOfForesight#onEnemyScryed}）。</li>
 * </ol>
 *
 * <h3>为什么这两个点要做成父类的钩子</h3>
 * <p>先知护符的整套探查逻辑写在一个**匿名字段初始化**里（{@code public CellSelector.Listener scry
 * = new CellSelector.Listener(){...}}）。Java 的字段不参与多态 —— 子类再声明一个同名 {@code scry}
 * 只会**遮蔽**父类那个，而父类 {@code execute()} 里读到的仍是父类自己的实例，覆写根本不生效。
 * 所以父类本次在「揭到隐藏陷阱」与「探查到敌人」两处各开了一个 protected 钩子，
 * 本类只覆写钩子即可，不必把整段探查逻辑重写一遍。</p>
 *
 * <p>文本键 {@code items.artifacts.approachingday.*}；护符原有的
 * {@code ac_scry/low_charge/prompt/uneasy/levelup/full_charge/desc_worn/desc_cursed}
 * 等键沿父类链自动继承。</p>
 */
public class ApproachingDay extends TalismanOfForesight {

	/** 探查到隐藏陷阱时，在原版 10 点之外额外累积的升级进度。 */
	public static final int TRAP_BONUS_EXP = 10;

	/** 探查到敌人时施加的恐惧回合数。 */
	public static final float TERROR_DURATION = 10f;

	{
		image = ItemSpriteSheet.ARTIFACT_APPROACHING_DAY;
	}

	@Override
	protected int extraTrapExp( int oldValue ){
		//需求只点名「陷阱」：隐藏门（SECRET_DOOR）维持原版 100 点，不额外加码
		return TRAP_BONUS_EXP;
	}

	@Override
	protected void onEnemyScryed( Char ch, float dist ){
		if (ch == null || !ch.isAlive()) return;
		if (ch.isImmune(Terror.class)) return;

		//object 记的是恐惧的来源，供 Terror 结算「恢复」时判断（与恐惧卷轴/恐惧符石同一口径）
		Buff.affect(ch, Terror.class, TERROR_DURATION).object = Dungeon.hero.id();
	}

	//==========================================================================
	// 炼金合成：先知护符 + 30 脑啡肽，12 能量
	//==========================================================================

	public static class CraftRecipe extends ArtifactEnhanceRecipe<TalismanOfForesight, ApproachingDay> {

		@Override
		protected Class<TalismanOfForesight> acceptedArtifact(){
			return TalismanOfForesight.class;
		}

		@Override
		protected ApproachingDay createEnhanced(){
			return new ApproachingDay();
		}

		@Override
		protected void transferState(TalismanOfForesight source, ApproachingDay enhanced){
			enhanced.level( source.level() );
			enhanced.exp = source.exp;
			enhanced.chargeCap = source.chargeCap;
			enhanced.charge = source.charge;
			enhanced.partialCharge = source.partialCharge;
			enhanced.cooldown = source.cooldown;

			enhanced.cursed = source.cursed;
			enhanced.cursedKnown = source.cursedKnown;
			enhanced.levelKnown = source.levelKnown;

			//父类的 upgrade() 用 exp 累积进度，且升级阈值带 level()，所以 exp 必须原样带走
		}
	}
}
