/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon / Easily-Sprouted-Pixel-Dungeon
 * Copyright (C) 2015-2018 dachhack / zay448345045
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

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 地衣蘑菇 —— **占位物品**（2026-09-25）。
 * <p>
 * 源仓库（Easily Sprouted PD）的 {@code ItemSpriteSheet.MUSHROOM_LICHEN = MUSHROOMS + 3}
 * 只是一个**从未被任何代码引用的预留贴图**：没有物品类、没有文本、没有效果
 * （该仓库的 {@code Generator.Category.MUSHROOM} 只注册了 6 个类，不含它）。
 * 用户已把这格贴图（绿色地衣丛）一并搬到本作 {@code items.png} 第 45 行第 3 格，
 * 故这里按「只接线贴图、不做效果」的占位物品实现：可堆叠、可食用（饱食度同其余蘑菇），
 * 但吃下后**不产生任何额外效果**。
 * <p>
 * 后续若要给它设计效果，只需覆写基类的 {@code eatEffect(hero)} 并在
 * {@code items.properties / items_zh.properties} 里补一句 {@code ...effect=} 即可 ——
 * 基类 {@link MushroomFood} 的 Boss 层禁食守卫与播报逻辑会自动生效。
 */
public class LichenMushroom extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_LICHEN;
	}

	//eatEffect 不做任何事：占位物品，等后续定好设计再补
}
