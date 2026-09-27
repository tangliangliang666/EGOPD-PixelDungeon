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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 凡作其三：血肉圆盾（图标：item.png 38行第5个，16x16）。
 * ——数值套用圆盾（RoundShield）：基础上限 12、成长 +2 每级；并提供 4+lvl 格挡值。
 */
public class CommonWorkC extends CommonWork {
	{
		image = ItemSpriteSheet.COMMON_WORK_3;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1f;
	}

	@Override
	public int max(int lvl) {
		return Math.round(( Math.round(3f * (tier + 1)) +   //12 base（从默认 20 下调，同圆盾）
				lvl * (tier - 1)) * boneMultiplier());               //+2 每级（从默认 +4 下调，同圆盾）
	}

	//格挡值：为装备者提供等于 DRMax() 的 DR（同圆盾）
	@Override
	public int defenseFactor( Char owner ) {
		return DRMax();
	}

	public int DRMax(){
		return DRMax(buffedLvl());
	}

	//4 点基础防御，+1 每级
	public int DRMax(int lvl){
		return 4 + lvl;
	}

	//statsInfo 显示格挡值
	public String statsInfo(){
		if (isIdentified()){
			return Messages.get(this, "stats_desc", 4 + buffedLvl());
		} else {
			return Messages.get(this, "typical_stats_desc", 4);
		}
	}
}
