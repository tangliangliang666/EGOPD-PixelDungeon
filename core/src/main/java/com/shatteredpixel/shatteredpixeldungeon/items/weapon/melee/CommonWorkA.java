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
 * 凡作其一：韧带长鞭（图标：item.png 38行第3个，14x14）。
 * ——数值套用长鞭（Whip）：基础上限 15、成长 3 每级；RCH=3。
 */
public class CommonWorkA extends CommonWork {
	{
		image = ItemSpriteSheet.COMMON_WORK_1;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		RCH = 3;    //长鞭型攻击距离
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 5 * tier +         //15 base（从默认 20 下调，同长鞭）
				lvl * tier) * boneMultiplier());        //+3 每级（从默认 +4 下调，同长鞭）
	}
}
