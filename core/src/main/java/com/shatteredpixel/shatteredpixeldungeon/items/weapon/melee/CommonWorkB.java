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
 * 凡作其二：尖刺弯刀（图标：item.png 38行第4个，13x16）。
 * ——数值套用弯刀（Scimitar）：基础上限 16、成长 +4 每级；DLY=0.8f（1.25× 攻速）。
 */
public class CommonWorkB extends CommonWork {
	{
		image = ItemSpriteSheet.COMMON_WORK_2;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1.2f;

		DLY = 0.8f; //1.25× 攻速（同弯刀）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 4 * (tier + 1) +     //16 base（从默认 20 下调，同弯刀）
				lvl * (tier + 1)) * boneMultiplier());    //+4 每级，成长不变
	}
}
