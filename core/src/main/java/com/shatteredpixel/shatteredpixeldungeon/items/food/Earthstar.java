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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 地球之星蘑菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/Earthstar}。
 * <p>
 * 吃下后**「外星力量」不分敌我地爆发**：全层敌对怪物与英雄本人都被施加流血与残废，
 * 英雄还要额外承受自身当前生命值一半的伤害（最低 1 点）。
 * 流血强度按楼层深度计算：怪物 {@code depth+3}、英雄 {@code depth}，再减去各自的
 * 护甲（怪物减半）随机值。1/10 概率英雄受到的护甲减免是全额而非减半。
 */
public class Earthstar extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_EARTHSTAR;
	}

	@Override
	protected void eatEffect( Hero hero ) {

		//源仓库「好运档」与「普通档」的唯一差别：英雄这一侧的护甲减免是全额还是减半
		boolean fullArmor = Random.Int( 10 ) == 1;

		for (Mob mob : allMobs()) {
			if (mob.alignment == Char.Alignment.ENEMY) {
				int damage = Math.max( 0, (Dungeon.depth + 3) - Random.IntRange( 0, mob.drRoll() / 2 ) );
				Buff.affect( mob, Bleeding.class ).set( damage );
				Buff.prolong( mob, Cripple.class, Cripple.DURATION * 2 );
			}
		}

		int armorCut = fullArmor ? hero.drRoll() : hero.drRoll() / 2;
		int heroDamage = Math.max( 0, Dungeon.depth - Random.IntRange( 0, armorCut ) );

		//源仓库写作 Math.max(1, Math.round(hero.HP / 2))，整数除法下 Math.round 是空操作，等价于下面这行
		hero.damage( Math.max( 1, hero.HP / 2 ), this );
		Buff.affect( hero, Bleeding.class ).set( heroDamage );
		Buff.prolong( hero, Cripple.class, Cripple.DURATION );
	}
}
