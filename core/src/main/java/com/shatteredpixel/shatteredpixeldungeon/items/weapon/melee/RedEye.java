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

//赤瞳：二阶近战武器，武技配置套用铁头棍（Cudgel）。
//效果：持有时，英雄获得对4格范围内所有单位的灵视感知（参考女猎人天赋"敏锐感知"，见 Level.updateFieldOfView）。
public class RedEye extends Cudgel {

	{
		image = ItemSpriteSheet.RED_EYE;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1.2f;

		tier = 2;
		ACC = 1.2f; //20% boost to accuracy（铁头棍继承的1.40倍命中削弱至1.20倍）
	}

	@Override
	public int min(int lvl) {
		return  2 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  12 +  //base
				3*lvl;   //level scaling
	}

}
