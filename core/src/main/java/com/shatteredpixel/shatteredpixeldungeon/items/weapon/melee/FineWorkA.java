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
 * 良作其一：胸骨战斧（图标：item.png 39行第3个，16x16）。
 * ——数值套用战斧（BattleAxe）：基础上限 20、成长 +5 每级；ACC=1.24f。
 */
public class FineWorkA extends FineWork {
	{
		image = ItemSpriteSheet.FINE_WORK_1;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 0.9f;

		ACC = 1.24f; //24% 精准加成（同战斧）
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 4 * (tier + 1) +    //20 base（从默认 25 下调，同战斧）
				lvl * (tier + 1)) * boneMultiplier());    //+5 每级，成长不变
	}
}
