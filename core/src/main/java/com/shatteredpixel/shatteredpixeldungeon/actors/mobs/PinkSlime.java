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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.PinkSlimeSprite;

/**
 * 粉色史莱姆：溶解之爱击杀目标时生成的友方史莱姆。
 * 动画帧配置与数值完全套用 {@link Slime}，仅阵营为友方（ALLY）。
 */
public class PinkSlime extends Slime {

	{
		spriteClass = PinkSlimeSprite.class;

		alignment = Char.Alignment.ALLY;   //我方阵营（其余数值继承史莱姆）
		state = WANDERING;                 //生成后不睡眠，立即游荡/接敌

		EXP = 0;
		lootChance = 0;
	}

}
