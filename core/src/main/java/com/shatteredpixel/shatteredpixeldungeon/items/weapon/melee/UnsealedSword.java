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
 * 一阶段解封之剑：系列的第二形态（六阶面板 6+L ~ 35+7L，力量需求 22）。
 *
 * <p>规则（解封门槛、锁手、落进副手即封回等）见 {@link SealedSwordBase}。</p>
 *
 * <p><b>特殊效果</b>：攻击时附带本次伤害 <b>10%</b> 的火焰伤害（2026-09-15 起）。</p>
 */
public class UnsealedSword extends SealedSwordBase {

	{
		image = ItemSpriteSheet.UNSEALED_SWORD;
	}

	/** 攻击附带本次伤害 10% 的火焰伤害。 */
	@Override
	public float fireDamagePercent() {
		return 0.10f;
	}
}
