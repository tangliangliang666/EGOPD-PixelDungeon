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

package com.shatteredpixel.shatteredpixeldungeon.items.remains;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.stones.StoneOfClairvoyance;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 破损义眼：拇指·前二老板的遗物（原套用战士的纹章残蜡）。
 * <p>效果＝以使用者为中心<b>触发一次明示符石</b>（{@link StoneOfClairvoyance}）：
 * 揭示周围 20 格地图并挖出其中的隐藏门与陷阱，音效/特效都由符石自身播放。
 * 使用后遗物随之消散（{@link RemainsItem} 通用使用流程）。</p>
 */
public class BrokenEye extends RemainsItem {

	{
		image = ItemSpriteSheet.BROKEN_EYE;
	}

	@Override
	protected void doEffect(Hero hero) {
		StoneOfClairvoyance.reveal( hero.pos );
	}

}
