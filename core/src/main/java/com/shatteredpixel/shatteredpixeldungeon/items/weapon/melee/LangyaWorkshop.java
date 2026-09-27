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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Alternate form of the shape-shifting weapon: hand-claw and dagger.
 * <p>Displayed as tier 5, but applies the stats of {@link AssassinsBlade} (tier 4:
 * bonus damage on surprise attacks), per the series design doc.</p>
 */
public class LangyaWorkshop extends MorphWeapon {

	{
		image = ItemSpriteSheet.LANGYA_WORKSHOP;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 5; //display tier; actual stats follow tier-4 assassin's blade
	}

	@Override
	public int min(int lvl) {
		return 4 + lvl; //tier-4 assassin's blade
	}

	@Override
	public int max(int lvl) {
		return  20 +       //20 base, tier-4 assassin's blade
				5*lvl;     //+5 per level
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(4, lvl); //tier-4 assassin's blade strength requirement
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	@Override
	public int damageRoll(Char owner) {
		if (owner instanceof Hero) {
			Hero hero = (Hero)owner;
			Char enemy = hero.attackTarget();
			if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
				//deals 50% toward max to max on surprise, instead of min to max.
				int diff = max() - min();
				int damage = augment.damageFactor(Hero.heroDamageIntRange(
						min() + Math.round(diff*0.50f),
						max()));
				int exStr = hero.STR() - STRReq();
				if (exStr > 0) {
					damage += Hero.heroDamageIntRange(0, exStr);
				}
				return damage;
			}
		}
		return super.damageRoll(owner);
	}

	@Override
	public String name() {
		return "琅琊工坊";
	}

	@Override
	public String desc() {
		return "隐藏在漆黑噤默手套中的武器，可以随时方便而无声的取用。\n\n琅琊工坊的产品，手铠与匕首。这件武器对未察觉你的敌人更有效。";
	}
}
