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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 良作其二：肋骨暗杀镰（图标：item.png 39行第4个，14x16）。
 * ——数值套用暗杀之刃（AssassinsBlade）：基础上限 20、成长 +5 每级；
 *   对「被突袭 surprisedBy」的目标，取 50% 到 100% 上限之间的伤害。
 */
public class FineWorkB extends FineWork {
	{
		image = ItemSpriteSheet.FINE_WORK_2;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;
	}

	@Override
	public int max(int lvl) {
		return Math.round(( 4 * (tier + 1) +    //20 base（从默认 25 下调，同暗杀之刃）
				lvl * (tier + 1)) * boneMultiplier());    //+5 每级，成长不变
	}

	// 对被突袭目标：50% 上限 ~ 100% 上限 之间摇伤害
	@Override
	public int damageRoll(Char owner) {
		if (owner instanceof Hero) {
			Hero hero = (Hero)owner;
			Char enemy = hero.attackTarget();
			if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
				int diff = max() - min();
				int damage = augment.damageFactor(Hero.heroDamageIntRange(
						min() + Math.round(diff * 0.50f),
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
}
