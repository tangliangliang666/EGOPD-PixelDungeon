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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfUpgrade;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 意志之刃 —— 四阶近战武器。
 *
 * <p>效果：武器会以「背包中升级卷轴的数量」获得等量的临时等级
 * （与奥术聚酯给法杖的临时等级同理，仅提升战斗强度，不改变真实升级等级）。
 * 升级卷轴若收纳在卷轴筒等小背包中也会一并计入。</p>
 */
public class WillBlade extends MeleeWeapon {

	{
		image = ItemSpriteSheet.WILL_BLADE;

		tier = 4;
	}

	/** 背包中升级卷轴（含卷轴筒等小背包内）的总数。 */
	public int scrollBonus(){
		if (Dungeon.hero == null){
			return 0;
		}
		int count = 0;
		for (Bag bag : Dungeon.hero.belongings.getBags()){
			for (Item item : bag.items){
				if (item instanceof ScrollOfUpgrade){
					count += item.quantity();
				}
			}
		}
		return count;
	}

	@Override
	public int buffedLvl() {
		return super.buffedLvl() + scrollBonus();
	}

}
