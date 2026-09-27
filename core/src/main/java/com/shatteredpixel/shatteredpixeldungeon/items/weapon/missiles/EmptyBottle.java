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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 空酒瓶 —— 一阶投掷武器。
 *
 * <p>高伤害（10+L ~ 25+L）但耐久极低：每次投掷都会碎掉（耐久仅 1 次）。</p>
 */
public class EmptyBottle extends MissileWeapon {

	{
		image = ItemSpriteSheet.EMPTY_BOTTLE;

		tier = 1;
	}

	@Override
	public int min(int lvl) {
		return 10 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 25 + lvl;
	}

	//每次投掷都消耗全部耐久：酒瓶只有 1 次耐久
	@Override
	public float durabilityPerUse(int level){
		return MAX_DURABILITY + 0.001f;
	}

	//单只出现，不与其它酒瓶堆叠成“一打”
	@Override
	public int defaultQuantity(){
		return 1;
	}

}
