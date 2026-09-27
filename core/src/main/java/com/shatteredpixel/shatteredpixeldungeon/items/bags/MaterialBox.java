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

package com.shatteredpixel.shatteredpixeldungeon.items.bags;

import com.shatteredpixel.shatteredpixeldungeon.items.BodyArtMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 「素材箱」：环指大师专属背包，用于容纳所有人体派素材（硬/骨/肉/血/魔质及后续特殊掉落素材）。
 * <p>开局自带，不可丢弃。自动将拾取的 BodyArtMaterial 物品归入其中。</p>
 */
public class MaterialBox extends Bag {

	{
		image = ItemSpriteSheet.MATERIAL_BOX;
	}

	@Override
	public boolean canHold( Item item ) {
		if (item instanceof BodyArtMaterial) {
			return super.canHold(item);
		}
		return false;
	}

	@Override
	public int capacity() {
		return 24;
	}

	@Override
	public int value() {
		return 0;
	}
}
