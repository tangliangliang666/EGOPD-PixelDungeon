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

package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 「拇指 前二老板」的职业护甲：拇指大衣。
 * <p>当前为占位实现：行为完全复用战士（由 HeroClass.armorAbilities 提供三个盔甲技能）。</p>
 *
 * <p>贴图取自 items.png 第 12 行第 14 列（ItemSpriteSheet.ARMOR_VALENCINA），尺寸 14×16。</p>
 */
public class ThumbCoat extends ClassArmor {

	{
		image = ItemSpriteSheet.ARMOR_VALENCINA;
	}

}
