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
 * 地彗星刀-出鞘：短剑复刻测试武器（占位）。
 * <p>属性完全复制 {@link Shortsword}（短剑），仅名称与图标不同，用于视觉效果检测。</p>
 */
public class CometKnifeDrawn extends Shortsword {

	{
		image = ItemSpriteSheet.COMET_KNIFE_DRAWN;
	}

	@Override
	public String name() {
		return "地彗星刀-出鞘";
	}
}
