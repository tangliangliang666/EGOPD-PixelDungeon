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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Alternate form of the shape-shifting weapon: double-blades.
 * <p>Tier 5; applies the data of {@link Sai} (very fast 2x speed, low per-hit damage).</p>
 */
public class CalistaStudio extends MorphWeapon {

	{
		image = ItemSpriteSheet.CALISTA_STUDIO;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.3f;

		tier = 5;
		DLY = 0.5f; //2x speed
	}

	@Override
	public int max(int lvl) {
		return  Math.round(2.5f*(tier+1)) +     //15 base (tier 5)
				lvl*Math.round(0.5f*(tier+1));  //+3 per level
	}

	@Override
	public String name() {
		return "卡莉斯塔工作室";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n卡莉斯塔工作室的产品，双刃。这是一件非常快的武器。";
	}
}
