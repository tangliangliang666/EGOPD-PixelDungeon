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

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 四阶武器「虚无衍射体」（2026-09-05）。
 * <p>面板：4+L ~ 15+5L（伤害偏低）；特殊效果：即使被敌人发现，
 * 攻击也始终被视为偷袭（必中 + 偷袭结算）。
 * 该效果在引擎层实现：{@code Mob.surprisedBy} 对持有本武器者的攻击
 * 忽略"敌方是否看见/察觉"这一条件（见 Mob.java surprisedBy）。</p>
 */
public class VoidDiffraction extends MeleeWeapon {

	{
		image = ItemSpriteSheet.VOID_DIFFRACTION;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return 4 + lvl;    //4+L
	}

	@Override
	public int max(int lvl) {
		return 15 + 5*lvl; //15+5L
	}

}
