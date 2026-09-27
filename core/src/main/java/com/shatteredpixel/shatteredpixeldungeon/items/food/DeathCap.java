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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Blindness;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 死亡之帽蘑菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/DeathCap}。
 * <p>
 * 吃下后「暗黑能量」炸开：全层敌对怪物与英雄本人一起受到**当前生命值百分比**的伤害
 * （怪物 1/2、英雄 1/4，各有下限），并陷入致盲。
 * 1/10 概率为「好运档」：怪物吃 min 5 点保底、且**不会因此被激怒**；
 * 其余 9/10 为普通档：怪物吃 min 3 点保底并会立刻把英雄锁定为敌人。
 */
public class DeathCap extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_DEATHCAP;
	}

	@Override
	protected void eatEffect( Hero hero ) {

		//源仓库写作 Math.max(..., Math.round(HP / n))，整数除法下 Math.round 是空操作 ⇒ 直接取商
		int heroSelfDamage = Math.max( 1, hero.HP / 4 );

		if (Random.Int( 10 ) == 1) {

			for (Mob mob : allMobs()) {
				if (mob.alignment == Char.Alignment.ENEMY) {
					mob.damage( Math.max( 5, mob.HP / 2 ), this );
				}
			}
			hero.damage( heroSelfDamage, this );
			//源仓库写作 Random.Int(5, 7)（上界开区间）⇒ 实际是 5~6 回合
			Buff.prolong( hero, Blindness.class, Random.IntRange( 5, 6 ) );

		} else {

			for (Mob mob : allMobs()) {
				if (mob.alignment == Char.Alignment.ENEMY) {
					mob.damage( Math.max( 3, mob.HP / 2 ), this );
					mob.aggro( hero );
				}
			}
			hero.damage( heroSelfDamage, this );
			//源仓库写作 Random.Int(6, 9)（上界开区间）⇒ 实际是 6~8 回合
			Buff.prolong( hero, Blindness.class, Random.IntRange( 6, 8 ) );
		}
	}
}
