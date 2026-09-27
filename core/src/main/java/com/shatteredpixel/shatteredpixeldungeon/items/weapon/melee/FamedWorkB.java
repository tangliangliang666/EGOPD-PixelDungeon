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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 名作其二：血肉战锤（图标：item.png 40行第4个，16x16）。
 * ——数值套用战锤（WarHammer）：基础上限 24、成长 +6 每级；ACC=1.20f。
 */
public class FamedWorkB extends FamedWork {
	{
		image = ItemSpriteSheet.FAMED_WORK_2;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		ACC = 1.20f; //20% 精准加成（同战锤）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 4 * (tier + 1) +    //24 base（从默认 30 下调，同战锤）
				lvl * (tier + 1)) * boneMultiplier());    //+6 每级，成长不变
	}
}
