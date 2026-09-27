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

package com.shatteredpixel.shatteredpixeldungeon.items.armor;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 「中指 长兄」的职业护甲：中指外套。
 * <p>三个盔甲技能已有各自独立的槽位、图标与实装
 * （`HeroClass.armorAbilities` 的 `case MIDDLE_FINGER` → `GritTeeth`「咬紧牙关」/
 * `NeverForget`「永不遗忘」/ `InstantExecution`「即刻处刑[莱瓦汀]」，
 * 图标 = `HeroIcon` 帧 129 / 130 / 131（三格专用，均已绘制）。</p>
 *
 * <p>贴图取自 items.png 第 12 行第 15 列（ItemSpriteSheet.ARMOR_MIDDLE_FINGER），尺寸 16×16。</p>
 */
public class MiddleFingerCoat extends ClassArmor {

	{
		image = ItemSpriteSheet.ARMOR_MIDDLE_FINGER;
	}

}
