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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BerryRegeneration;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Drowsy;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 太阳伞菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/PixieParasol}。
 * <p>
 * 吃下后「精灵的安眠曲」响起：全层敌对怪物先是昏昏欲睡
 * （{@link Drowsy}，3~5 回合后转入沉睡 {@link com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicalSleep}），
 * 同时被麻痹 10~16 回合，头顶还会冒出音符粒子；英雄本人获得一份以自身满血量为
 * 回合数的缓慢回复。
 * 1/10 概率为「好运档」：英雄**不会**眩晕；其余 9/10 为普通档：英雄额外眩晕 5 回合。
 * <p>
 * 注1：源仓库的 Drowsy 在 {@code attachTo} 里自赋 {@code Random.Int(3,6)}（= 3~5）回合延迟后
 * 才转入 MagicalSleep；本作的 Drowsy 不会自赋时长，故这里显式传入同样的 3~5 回合。
 * 注2：源仓库的 Paralysis 时长按 {@code Paralysis.duration(mob)} 计算，本作的 Paralysis
 * 没有该辅助方法（其 DURATION 为 10，由 {@code Buff.prolong} 的 {@code resist()} 调整），
 * 故这里直接沿用源仓库对**怪物**固定的 10~16 回合数值。
 */
public class PixieParasol extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_PIXIEPARASOL;
	}

	@Override
	protected void eatEffect( Hero hero ) {

		//与源仓库一致：先掷「好运档」再处理怪物，保持随机数取用顺序相同
		boolean lucky = Random.Int( 10 ) == 1;

		for (Mob mob : allMobs()) {
			if (mob.alignment == Char.Alignment.ENEMY) {
				Buff.affect( mob, Drowsy.class, Random.IntRange( 3, 5 ) );
				Buff.prolong( mob, Paralysis.class, Random.IntRange( 10, 16 ) );
				if (mob.sprite != null) {
					mob.sprite.centerEmitter().start( Speck.factory( Speck.NOTE ), 0.3f, 5 );
				}
			}
		}

		//「好运档」的唯一差别：英雄不眩晕
		if (!lucky) {
			Buff.affect( hero, Vertigo.class, 5f );
		}
		Buff.affect( hero, BerryRegeneration.class ).level( hero.HT );
	}
}
