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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 金色果冻蘑菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/GoldenJelly}。
 * <p>
 * 吃下后「整层地牢充满粘稠的孢子」：全层敌对怪物被定身（Roots），
 * 英雄本人陷入眩晕（Vertigo）。
 * 1/10 概率为「好运档」：怪物定身 20 回合、英雄只眩晕 1 回合；
 * 其余 9/10 为普通档：怪物定身 10 回合、英雄眩晕 3 回合。
 */
public class GoldenJelly extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_GOLDENJELLY;
	}

	@Override
	protected void eatEffect( Hero hero ) {

		boolean lucky = Random.Int( 10 ) == 1;

		float rootTime = lucky ? 20f : 10f;

		for (Mob mob : allMobs()) {
			if (mob.alignment == Char.Alignment.ENEMY) {
				Buff.prolong( mob, Roots.class, rootTime );
			}
		}

		Buff.affect( hero, Vertigo.class, lucky ? 1f : 3f );
	}
}
