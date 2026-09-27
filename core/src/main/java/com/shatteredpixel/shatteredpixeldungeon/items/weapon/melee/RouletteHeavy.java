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
 * Alternate form of the shape-shifting weapon: a very heavy great sword.
 * <p>Tier 5; applies the data of {@link Greataxe} (high base damage, higher strength requirement).</p>
 */
public class RouletteHeavy extends MorphWeapon {

	{
		image = ItemSpriteSheet.ROULETTE_HEAVY;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
	}

	@Override
	public int max(int lvl) {
		return  5*(tier+4) +    //45 base (tier 5)
				lvl*(tier+1);   //scaling unchanged
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(tier+1, lvl); //20 base strength req, up from 18
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	@Override
	public String name() {
		return "轮盘重工";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n轮盘重工的产品，巨剑。这件武器非常沉重。";
	}
}
