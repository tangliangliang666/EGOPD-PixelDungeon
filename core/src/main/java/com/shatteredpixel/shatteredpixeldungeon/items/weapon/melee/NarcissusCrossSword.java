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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 水仙十字圣剑 · <b>常态</b>：生成时的状态，也是英雄处于 2~29 级时的状态。
 *
 * <p>面板 {@code 2+L ~ 15+3L}（正是二阶武器的默认公式，这里显式写出以便与另两个形态对照），
 * <b>没有任何特殊能力</b>，也没有形态光效。</p>
 *
 * <p>形态规则见 {@link NarcissusCrossSwordBase}。</p>
 */
public class NarcissusCrossSword extends NarcissusCrossSwordBase {

	{
		image = ItemSpriteSheet.NARCISSUS_CROSS_SWORD;
	}

	/** 面板下限 2+L（二阶默认）。 */
	@Override
	public int min(int lvl) {
		return 2 + lvl;
	}

	/** 面板上限 15+3L（二阶默认）。 */
	@Override
	public int max(int lvl) {
		return 15 + 3 * lvl;
	}
}
