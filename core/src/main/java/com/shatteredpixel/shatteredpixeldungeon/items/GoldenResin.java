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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GoldenResinBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ResinCoatingBuff;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.brews.ShockingBrew;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 「黄金松脂」（2026-09-08 用户新道具）：散发着黄金光芒的松脂。
 * <p>对当前武器使用花费 1 回合，使武器在 150 回合内强化雷电属性并获得电击附魔。
 * 炼金配方：雷鸣魔药（ShockingBrew）×1 + 4 炼金能量 → 黄金松脂。
 * 贴图：ItemSpriteSheet 第40行第8格（xy(8,40)），16×16。
 */
public class GoldenResin extends Resin {

	{
		image = ItemSpriteSheet.GOLDEN_RESIN;
	}

	@Override
	protected float duration() {
		return 150f;
	}

	@Override
	protected boolean fast() {
		return false;
	}

	@Override
	protected Class<? extends ResinCoatingBuff> coating() {
		return GoldenResinBuff.class;
	}

	/** 炼金配方：雷鸣魔药（ShockingBrew）×1 + 4 能量 → 黄金松脂。 */
	public static class FromPotion extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{ShockingBrew.class};
			inQuantity = new int[]{1};
			cost = 4;
			output = GoldenResin.class;
			outQuantity = 1;
		}
	}
}
