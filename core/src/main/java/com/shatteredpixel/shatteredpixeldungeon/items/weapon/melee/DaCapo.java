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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * Da-Capo：五阶武器。
 * <p>伤害公式 (5+L ~ 28+6×L)；命中时同时施加 (1+武器等级) 回合的眩晕（Vertigo）和恍惚（Daze）。</p>
 */
public class DaCapo extends MeleeWeapon {

	{
		image = ItemSpriteSheet.DA_CAPO;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
	}

	@Override
	public int min(int lvl) {
		return 5 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 28 + 6*lvl;
	}

	//命中时同时施加眩晕和恍惚（各 1+武器等级 回合）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);
		if (defender.isAlive()){
			Buff.prolong(defender, Daze.class, 1 + buffedLvl());
			Buff.prolong(defender, Vertigo.class, 1 + buffedLvl());
		}
		return damage;
	}

	@Override
	public String name() {
		return "Da-Capo";
	}
}
