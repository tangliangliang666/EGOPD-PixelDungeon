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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HolyBarrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WhiteSmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * 「神圣卡」（2026-09-24 用户新道具）。
 *
 * <h3>用法</h3>
 * <p>在背包里对自己使用（{@code defaultAction = AC_USE}），<b>花费一回合</b>，获得
 * {@link HolyBarrier}「神圣屏障」：一次性无敌，替你挡下<b>下一次</b>伤害，挡完即消失。</p>
 *
 * <h3>不可叠加</h3>
 * <p>屏障在场时再使用一张卡<b>不生效</b>：不消耗回合、也不消耗卡，只提示一句
 * （{@code items.holycard.already}）。这样「不可叠加」不会变成「白扔一张卡」。</p>
 *
 * <h3>获得方式</h3>
 * <p>只能炼金合成，不入任何掉落池：<b>任意卷轴 ×1 ＋ 13 炼金能量 → 神圣卡 ×1</b>
 * （见 {@link CraftRecipe}）。配方吃的是 {@code instanceof Scroll} 整类，
 * 普通卷轴与异域卷轴都能用。</p>
 *
 * <h3>贴图</h3>
 * <p>{@code ItemSpriteSheet.HOLY_CARD} = {@code xy(8,41)}，15×15（用户已绘，紧接洛伊德护符之后）。</p>
 */
public class HolyCard extends Item {

	public static final String AC_USE = "USE";

	/** 使用消耗的回合数。 */
	public static final float TIME_TO_USE = 1f;

	/** 合成一张卡的炼金能量。 */
	public static final int ENERGY_COST = 13;

	{
		image = ItemSpriteSheet.HOLY_CARD;

		stackable = true;
		defaultAction = AC_USE;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_USE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (!action.equals( AC_USE )) return;

		if (!hero.isAlive()) return;

		//不可叠加：已有屏障时整段不生效——不花回合、不扣卡，只提示
		if (HolyBarrier.active( hero )) {
			GLog.w( Messages.get( this, "already" ) );
			return;
		}

		Buff.affect( hero, HolyBarrier.class );

		detach( hero.belongings.backpack );

		//纯白卡牌 ⇒ 使用与生效都用既有的白色烟雾粒子，不另画素材
		hero.sprite.operate( hero.pos );
		hero.sprite.emitter().burst( WhiteSmokeParticle.BURST, 8 );
		Sample.INSTANCE.play( Assets.Sounds.READ );

		GLog.i( Messages.get( this, "used" ) );

		Item.updateQuickslot();

		//使用花费一回合
		hero.spendAndNext( TIME_TO_USE );
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		//炼金产物、效果固定：恒已识别（与洛伊德护符 / 松脂系列一致）
		return true;
	}

	@Override
	public int value() {
		return 40 * quantity;
	}

	//==========================================================================
	// 炼金配方
	//==========================================================================

	/**
	 * 炼金配方：<b>任意卷轴 ×1 ＋ 13 炼金能量 → 神圣卡 ×1</b>。
	 *
	 * <p>不能用 {@code Recipe.SimpleRecipe}：它按 {@code ingredient.getClass() == inputs[i]} 精确匹配，
	 * 表达不了「任意卷轴」这个类别。所以自己实现 {@link Recipe}，判据用 {@code instanceof Scroll}。</p>
	 */
	public static class CraftRecipe extends Recipe {

		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			//只吃一卷；且必须是卷轴（普通 / 异域都算）
			return ingredients.size() == 1 && ingredients.get( 0 ) instanceof Scroll;
		}

		@Override
		public int cost( ArrayList<Item> ingredients ) {
			return ENERGY_COST;
		}

		@Override
		public Item brew( ArrayList<Item> ingredients ) {
			if (!testIngredients( ingredients )) return null;

			//整卷消耗（本配方只吃一卷，quantity 恒为 1 的那一份由炼金釜切出来）
			Scroll scroll = (Scroll) ingredients.get( 0 );
			scroll.quantity( scroll.quantity() - 1 );

			return new HolyCard();
		}

		@Override
		public Item sampleOutput( ArrayList<Item> ingredients ) {
			return new HolyCard();
		}
	}
}
