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
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 「散装黄金松脂」（2026-09-08 用户新道具）：黄金松脂的散装形态。
 * <p>可快速使用（不消耗回合），使武器在 50 回合内强化电击属性并获得电击附魔。
 * 炼金配方：黄金松脂 ×1 → 散装黄金松脂 ×2（免费拆分，0 炼金能量）。
 * 贴图：ItemSpriteSheet 第40行第9格（xy(9,40)），16×15。
 */
public class LooseGoldenResin extends Resin {

	{
		image = ItemSpriteSheet.LOOSE_GOLDEN_RESIN;
	}

	@Override
	protected float duration() {
		return 50f;
	}

	@Override
	protected boolean fast() {
		return true;
	}

	@Override
	protected Class<? extends ResinCoatingBuff> coating() {
		return GoldenResinBuff.class;
	}

	/** 炼金配方：黄金松脂 ×1 → 散装黄金松脂 ×2（0 能量）。 */
	public static class SplitRecipe extends Recipe.SimpleRecipe {
		{
			inputs = new Class[]{GoldenResin.class};
			inQuantity = new int[]{1};
			cost = 0;
			output = LooseGoldenResin.class;
			outQuantity = 2;
		}
	}
}
