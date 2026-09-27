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
 * 巴勒莫对剑：伯特纳利家族的家传宝，施展巴勒莫剑术的特制对剑（5 阶普通武器，2026-09-08 由占位改为正式数值）。
 * <p>极快（攻击延迟 0.5），且连续挥舞时越来越快——每次攻击叠加 1 层「连斩」
 * （{@code actors.buffs.PalermoSwordBuff}，图标帧 111），每层降低 10% 攻击延迟、最高 5 层
 * （满层攻击延迟减半）。层数结算见 {@code Hero.attackDelay()}。</p>
 */
public class PalermoSword extends MeleeWeapon {

	{
		image = ItemSpriteSheet.PALERMO_SWORD;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 5;
		DLY  = 0.5f;   //非常快的武器
	}

	@Override
	public int min(int lvl) {
		return 5 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 18 + 3*lvl;
	}
}
