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
 * Alternate form of the shape-shifting weapon: a one-handed sword.
 * <p>Displayed as tier 5, but applies the stats of {@link Longsword} (tier 4:
 * standard damage and speed, no downsides), per the series design doc.</p>
 */
public class Durandal extends MorphWeapon {

	{
		image = ItemSpriteSheet.DURANDAL;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5; //display tier; actual stats follow tier-4 longsword
	}

	@Override
	public int min(int lvl) {
		return 4 + lvl; //tier-4 longsword
	}

	@Override
	public int max(int lvl) {
		return  25 +       //25 base, tier-4 longsword
				5*lvl;     //+5 per level
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(4, lvl); //tier-4 longsword strength requirement
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	@Override
	public String name() {
		return "杜兰达尔";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n锋利的的单手剑，无坚不摧且绝不会损坏。";
	}
}
