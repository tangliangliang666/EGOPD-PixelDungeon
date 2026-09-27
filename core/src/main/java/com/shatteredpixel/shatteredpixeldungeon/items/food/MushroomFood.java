/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon / Easily-Sprouted-Pixel-Dungeon
 * Copyright (C) 2015-2018 dachhack / zay448345045
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

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 「可食用蘑菇」基类 —— 移植自 Easily Sprouted PD 的 {@code items/food} 蘑菇组
 * （{@code JackOLantern} / {@code Earthstar} / {@code DeathCap} / {@code BlueMilk} /
 * {@code GoldenJelly} / {@code PixieParasol}），2026-09-25。
 * <p>
 * 这一组蘑菇在源仓库中的共同点：
 * <ul>
 * <li>饱食度极低（源写法 {@code (Hunger.STARVING - Hunger.HUNGRY) / 10}）——它们是用来
 *     「吃效果」的，不是用来填肚子的；吃下的收益与代价都写在效果里。</li>
 * <li>Boss 层**禁止食用**：给出提示并直接返回，蘑菇不会被消耗（源仓库的潜意识设定）。</li>
 * <li>不吃骨头、可堆叠、售价 20。</li>
 * </ul>
 * 效果按 1/10 的概率分成「好运档」（case 1）与「普通档」（其余 9/10），逐条对齐源仓库。
 */
public abstract class MushroomFood extends Food {

	{
		//源仓库写法：(Hunger.STARVING - Hunger.HUNGRY) / 10
		//本作 Hunger.HUNGRY=300 / STARVING=450 ⇒ 每次 15 点饱食度（远低于一份口粮的 300）
		energy = (Hunger.STARVING - Hunger.HUNGRY) / 10f;

		bones = false;
	}

	@Override
	public void execute( Hero hero, String action ) {

		if (action.equals( AC_EAT )) {

			//源仓库约定：Boss 层不允许食用。此处必须在 super.execute 之前返回，
			//否则蘑菇会被照常吃掉（super 里才做 detach + 饱食度结算）。
			if (Dungeon.bossLevel()) {
				GLog.w( Messages.get( MushroomFood.class, "prevent" ) );
				return;
			}

			showEffect();
			eatEffect( hero );
		}

		super.execute( hero, action );
	}

	/**
	 * 食用时播报效果提示。源仓库里除蓝牛奶菇用正面（p 档）播报外，其余都是警示（w 档），
	 * 故把播报留成覆写点。
	 */
	protected void showEffect() {
		GLog.w( Messages.get( getClass(), "effect" ) );
	}

	/**
	 * 蘑菇的实际效果。基类默认**什么都不做** —— 源仓库的地衣丛只有贴图没有效果，
	 * 本作按占位物品实现，等后续定好设计再把效果写进对应子类。
	 */
	protected void eatEffect( Hero hero ) {
		//占位：无实际效果
	}

	/**
	 * 全层怪物快照。效果里会击杀怪物，直接遍历 {@code Dungeon.level.mobs} 会踩并发修改，
	 * 故一律先 {@code toArray} 取快照（源仓库同样如此）。
	 */
	protected static Mob[] allMobs() {
		return Dungeon.level.mobs.toArray( new Mob[0] );
	}

	@Override
	public int value() {
		return 20 * quantity;
	}
}
