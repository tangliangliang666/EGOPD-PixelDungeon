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

import com.shatteredpixel.shatteredpixeldungeon.items.Enkephalin;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;

import java.util.ArrayList;

/**
 * 「原版神器 + 30 脑啡肽 → 强化形态」的通用炼金配方（2026-09-20，2026-09-24 由 60 下调为 30）。
 *
 * <h3>为什么单独抽一个基类</h3>
 * <p>五件强化神器的配方**除了「拿哪件神器、产出什么」以外逐字相同**。照抄五份的话，
 * 日后任何一处调整（改脑啡肽数量、改能量花费、改「不允许重复强化」的口径）都要改五个地方，
 * 必然漏掉一两个。所以把通用逻辑收在这里，各强化神器只声明三件事：
 * {@link #acceptedArtifact()}、{@link #createEnhanced()}、{@link #transferState}。</p>
 *
 * <h3>两个容易静默失效的点</h3>
 * <ul>
 *   <li><b>神器必须能放进炼金釜</b>：入釜选择器走 {@code Recipe.usableInRecipe}，而它原本对
 *       {@code EquipableItem} 只放行近战/可升级投掷武器 ⇒ 神器会被直接挡在釜外，
 *       表现为「配方怎么都不生效」。已在那里放行 {@code Artifact}。</li>
 *   <li><b>产物要自己搬状态</b>：本作没有「A+材料→B 且保留状态」的现成机制，{@code brew} 就是
 *       唯一钩子，拿到的是釜里**真实的那件神器实例**（不是副本），所以直接读它的状态再拷到新实例上。
 *       反过来，产物若是 {@code new Xxx()} 了事，玩家辛苦养到 +10 的神器会被静默清零。</li>
 * </ul>
 *
 * @param <S> 作为原料的原版神器类型
 * @param <R> 强化形态类型
 */
public abstract class ArtifactEnhanceRecipe<S extends Artifact, R extends Artifact> extends Recipe {

	/** 每次强化消耗的脑啡肽数量。 */
	public static final int ENKEPHALIN_NEEDED = 30;

	/** 每次强化消耗的炼金能量。 */
	public static final int ENERGY_COST = 12;

	/**
	 * 本配方接受的原料神器类。用 {@code getClass() == acceptedArtifact()} **严格**比较，
	 * 于是**已经是强化形态的神器不再被接受** —— 否则玩家会白扔 30 脑啡肽换一件一模一样的东西。
	 */
	protected abstract Class<S> acceptedArtifact();

	/** 造一个「空」的强化形态（等级/充能由 {@link #transferState} 补上）。 */
	protected abstract R createEnhanced();

	/** 把原料神器上的等级、充能等状态搬到强化形态上。 */
	protected abstract void transferState(S source, R enhanced);

	@SuppressWarnings("unchecked")
	private S asSource(Item item){
		return acceptedArtifact().isInstance(item) ? (S) item : null;
	}

	@Override
	public boolean testIngredients(ArrayList<Item> ingredients) {
		if (ingredients.size() != 2) return false;

		Artifact source = null;
		Enkephalin enkephalin = null;
		for (Item it : ingredients){
			if (it instanceof Enkephalin){
				enkephalin = (Enkephalin) it;
			} else if (it.getClass() == acceptedArtifact()){
				source = asSource(it);
			} else {
				//釜里有第三样东西、或放的是已经强化过的神器 ⇒ 不匹配
				return false;
			}
		}

		return source != null && enkephalin != null
				&& enkephalin.quantity() >= ENKEPHALIN_NEEDED;
	}

	@Override
	public int cost(ArrayList<Item> ingredients) {
		return ENERGY_COST;
	}

	@Override
	public Item brew(ArrayList<Item> ingredients) {
		if (!testIngredients(ingredients)) return null;

		S source = null;
		Enkephalin enkephalin = null;
		for (Item it : ingredients){
			if (it instanceof Enkephalin){
				enkephalin = (Enkephalin) it;
			} else {
				source = asSource(it);
			}
		}

		//消耗原料：脑啡肽扣 ENKEPHALIN_NEEDED，神器整件消耗（quantity(0) 后由 AlchemyScene.craftItem 清槽）
		enkephalin.quantity(enkephalin.quantity() - ENKEPHALIN_NEEDED);
		source.quantity(0);

		R enhanced = createEnhanced();
		transferState(source, enhanced);
		return enhanced;
	}

	@Override
	public Item sampleOutput(ArrayList<Item> ingredients) {
		//炼金指南里的预览样本，不需要状态
		return createEnhanced();
	}
}
