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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 正义裁决者：五阶武器。
 * <p>伤害公式 (5+L ~ 18+5×L)，斩击音效。
 * 无视目标防御（见 Char.attack 的 dr 处理）；对生命值高于50%的敌人造成25%额外伤害，
 * 对生命值低于50%的敌人伤害降低25%。</p>
 */
public class JusticeArbiter extends MeleeWeapon {

	{
		image = ItemSpriteSheet.JUSTICE_ARBITER;
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
		return 18 + 5*lvl;
	}

	//对高血量敌人造成25%额外伤害，对低血量敌人伤害降低25%
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		float ratio = defender.HP / (float) defender.HT;
		if (ratio > 0.5f){
			damage = Math.round(damage * 1.25f);
		} else if (ratio < 0.5f){
			damage = Math.round(damage * 0.75f);
		}
		return super.proc(attacker, defender, damage);
	}

	@Override
	public String name() {
		return "正义裁决者";
	}
}
