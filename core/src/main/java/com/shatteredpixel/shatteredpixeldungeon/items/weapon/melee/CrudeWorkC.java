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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 劣作其三：拼接骨矛（图标：item.png 37行第5个，16x16）。
 * ——数值套用长矛（Spear）：基础上限 20（6.67×3）、成长 1.33×3=4 每级；DLY=1.5f；RCH=2。
 */
public class CrudeWorkC extends CrudeWork {
	{
		image = ItemSpriteSheet.CRUDE_WORK_3;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		DLY = 1.5f; //0.67× 攻速
		RCH = 2;    //额外 1 格攻击距离
	}

	@Override
	public int max(int lvl) {
		return Math.round(( Math.round(6.67f * (tier + 1)) +    //20 base（从默认 15 上调）
				lvl * Math.round(1.33f * (tier + 1))) * boneMultiplier()); //+4 每级（从默认 +3 上调）
	}
}
