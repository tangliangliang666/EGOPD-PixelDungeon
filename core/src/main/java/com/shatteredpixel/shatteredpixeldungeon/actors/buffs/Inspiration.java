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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「灵感」buff：环指大师「灵感一餐」天赋进食时获得。
 * <p>持有期间不主动消失，进行「创作」合成时消耗所有层数，
 * 为成品增加 2×层数 的随机数值（Weapon/Bone/Meat/Blood 各随机加 1）。</p>
 */
public class Inspiration extends Buff {

	{
		type = buffType.POSITIVE;
	}

	public int stacks = 0;

	@Override
	public boolean act() {
		spend( TICK );
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.INSPIRATION;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( stacks );
	}

	@Override
	public String name() {
		return "灵感";
	}

	@Override
	public String desc() {
		return "环指大师从食物中汲取的创作灵感，层数越高，创作时获得的额外数值越多。\n\n"
				+ "进行_创作_时，最终成品增加_2×灵感层数_的随机数值，并消耗所有灵感层数。\n\n"
				+ "当前灵感层数：_" + stacks + "_";
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stacks = bundle.getInt( STACKS );
	}
}
