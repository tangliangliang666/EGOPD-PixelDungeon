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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//悔恨：二阶近战武器，攻击延迟1.5（相当缓慢），攻击音效与打击有关。
//效果：命中时有(40+10×武器等级)%的概率对目标施加(3+武器等级)回合的眩晕（Vertigo）。
public class Remorse extends MeleeWeapon {

	{
		image = ItemSpriteSheet.REMORSE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 2;
		DLY = 1.5f; //0.67x speed
	}

	@Override
	public int min(int lvl) {
		return  2 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  28 +  //base
				4*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//悔恨：命中时概率施加眩晕
		if (attacker.buff(MagicImmune.class) == null
				&& defender.isAlive()
				&& defender.buff(Vertigo.class) == null){
			int chance = 40 + 10*buffedLvl();
			if (Random.Int(100) < chance){
				Buff.affect(defender, Vertigo.class, 3 + buffedLvl());
			}
		}

		return damage;
	}

}
