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
 * 名作其三：巨肋巨镰（图标：item.png 40行第5个，14x15）。
 * ——数值套用战镰（WarScythe）：基础上限 40、成长 +6 每级；ACC=0.8f（精准惩罚）。
 */
public class FamedWorkC extends FamedWork {
	{
		image = ItemSpriteSheet.FAMED_WORK_3;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		ACC = 0.8f; //20% 精准惩罚（同战镰）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( Math.round(6.67f * (tier + 1)) +    //40 base（从默认 30 上调，同战镰）
				lvl * (tier + 1)) * boneMultiplier());                   //+6 每级，成长不变
	}
}
