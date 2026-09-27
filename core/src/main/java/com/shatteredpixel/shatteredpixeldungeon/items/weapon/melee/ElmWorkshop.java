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
 * Alternate form of the shape-shifting weapon: hammer-mace and battle axe.
 * <p>Displayed as tier 5, but applies the stats of {@link Mace} (tier 3: accuracy
 * bonus, standard speed), per the series design doc.</p>
 */
public class ElmWorkshop extends MorphWeapon {

	{
		image = ItemSpriteSheet.ELM_WORKSHOP;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5; //display tier; actual stats follow tier-3 mace
		ACC = 1.28f; //mace accuracy bonus
	}

	@Override
	public int min(int lvl) {
		return 3 + lvl; //tier-3 mace
	}

	@Override
	public int max(int lvl) {
		return  16 +       //16 base, tier-3 mace
				4*lvl;     //+4 per level
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(3, lvl); //tier-3 mace strength requirement
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	@Override
	public String name() {
		return "榉树工坊";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n榉树工坊的产品，锤矛与战斧，某人最爱用的武器组合。这是一件比较精准的武器。";
	}
}
