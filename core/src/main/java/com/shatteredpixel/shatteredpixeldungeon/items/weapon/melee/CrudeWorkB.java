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
 * 劣作其二：颅骨打击型（图标：item.png 37行第4个，15x15）。
 * ——数值套用手斧（HandAxe）：伤害上限 4*(tier+1) 基础 + lvl*(tier+1) 成长；ACC=1.32f。
 */
public class CrudeWorkB extends CrudeWork {
	{
		image = ItemSpriteSheet.CRUDE_WORK_2;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		ACC = 1.32f; //32% 精准加成（同手斧）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 4 * (tier + 1) +    //12 base（从默认 15 下调）
				lvl * (tier + 1)) * boneMultiplier());    //+3 每级，成长不变
	}
}
