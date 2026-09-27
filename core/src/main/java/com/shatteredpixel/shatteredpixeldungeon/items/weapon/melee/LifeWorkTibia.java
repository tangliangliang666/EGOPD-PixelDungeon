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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 一生之作-提比娅（六阶人体派作品，环指大师的巅峰武器）。
 * 图标：item.png 39行第1个，31x30 超大贴图。
 * ——其它数值沿用 tier=6 默认（6~35，+7/lvl）；仅新增攻击距离 RCH=2。
 */
public class LifeWorkTibia extends BodyArtWeapon {
	{
		image = ItemSpriteSheet.LIFEWORK_TIBIA;

		tier = 6;
		RCH = 2; // 攻击距离 +1
	}
}
