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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

/**
 * 神谕代行者的指令标记维护器（挂在英雄身上，无图标）。
 * <p>每回合调用 {@link InstructionTarget#updateMark()}，确保本层有存活怪物时
 * 恰好有一个随机怪物被标记为指令目标；被标记怪物死亡后自动转移给其他存活怪物。</p>
 */
public class OracleMarkTracker extends Buff {

	{
		//核心常驻机制：十字架复活后保留（否则复活后不再标记指令目标）
		revivePersists = true;
	}

	@Override
	public boolean act() {
		InstructionTarget.updateMark();
		spend(TICK);
		return true;
	}

}
