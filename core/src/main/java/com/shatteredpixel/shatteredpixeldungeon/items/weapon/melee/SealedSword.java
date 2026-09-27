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
 * 封印之剑：系列的初始形态（六阶面板 6+L ~ 35+7L，力量需求 22）。
 *
 * <p>规则（解封门槛、锁手、落进副手即封回等）见 {@link SealedSwordBase}。握在主手时点击快捷栏即可
 * 依次解封为一阶段解封之剑、二阶段解封之剑、莱瓦汀；<b>被切换到副手时自动恢复为本形态</b>。</p>
 *
 * <p><b>特殊效果</b>：无——面板与效果都停在「封印」状态（{@link SealedSwordBase#fireDamagePercent()}
 * 返回 0），火焰从一阶段才开始。</p>
 */
public class SealedSword extends SealedSwordBase {

	{
		image = ItemSpriteSheet.SEALED_SWORD;
	}
}
