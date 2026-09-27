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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Blob;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 灯笼蘑菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/JackOLantern}。
 * <p>
 * 吃下后在**全层敌对怪物脚下点起三格火**（1/10 概率必定点燃每一只；其余 9/10 概率
 * 每只各掷一次硬币，并且另有 1/5 概率把两格火点到英雄自己脚下）。
 * 蘑菇本身不含任何增伤，纯粹是把整层变成火场。
 */
public class JackOLantern extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_LANTERN;
	}

	@Override
	protected void eatEffect( Hero hero ) {

		switch (Random.Int( 10 )) {
			case 1:
				for (Mob mob : allMobs()) {
					if (mob.alignment == Char.Alignment.ENEMY) {
						GameScene.add( Blob.seed( mob.pos, 3, Fire.class ) );
					}
				}
				break;

			default:
				for (Mob mob : allMobs()) {
					if (mob.alignment == Char.Alignment.ENEMY && Random.Int( 2 ) == 0) {
						GameScene.add( Blob.seed( mob.pos, 3, Fire.class ) );
					}
				}
				//代价：有 1/5 概率把自己也点着（源仓库中这一句正是「危险」的来源）
				if (Random.Int( 5 ) == 0) {
					GameScene.add( Blob.seed( hero.pos, 2, Fire.class ) );
				}
				break;
		}
	}
}
