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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 《是谁嗯嗯在我的头上》：神谕代行者的初始投掷武器（绘本）。
 * <p>数据完全套用投石（{@link ThrowingStone}）。</p>
 */
public class PoopStoryBook extends MissileWeapon {

	{
		image = ItemSpriteSheet.POOP_STORY_BOOK;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.1f;

		bones = false;

		tier = 1;
		baseUses = 5;
		sticky = false;
	}

	@Override
	public int value() {
		return Math.round(super.value()/2f); //half normal value
	}
}
