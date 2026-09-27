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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//血之渴望：三阶近战武器，1.2倍命中，斩击音效，武技配置套用短柄镰（Sickle）。
//效果：攻击命中时施加 (3+L~10+2L) 的流血。
public class BloodThirst extends Sickle {

	{
		image = ItemSpriteSheet.BLOOD_THIRST;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 3;
		ACC = 1.2f; //20% boost to accuracy
	}

	@Override
	public int min(int lvl) {
		return  3 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  10 +  //base
				2*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//血之渴望：命中时施加 (3+L~10+2L) 的流血
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){
			int bleed = Random.NormalIntRange(3 + buffedLvl(), 10 + 2*buffedLvl());
			Buff.affect(defender, Bleeding.class).set(bleed);
		}

		return damage;
	}

}
