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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Slow;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

/**
 * 蓝牛奶蘑菇 —— 移植自 Easily Sprouted PD 的 {@code items/food/BlueMilk}。
 * <p>
 * 吃下后「思维逐渐松弛」：英雄获得 20 回合急速（Haste）与一份以自身满血量为回合数的
 * 缓慢回复（{@link BerryRegeneration}）；同时**全层敌对怪物也被减速**。
 * 1/10 概率为「好运档」：怪物只吃减速、不会拿到回复；其余 9/10 为普通档：
 * 怪物同样获得与自身血量等长的回复（代价）。
 * <p>
 * 注：源仓库用 {@code Slow.duration(mob)} 计算时长，本作 {@link Slow} 没有该辅助方法，
 * 故统一取 {@link Slow#DURATION}。
 */
public class BlueMilk extends MushroomFood {

	{
		image = ItemSpriteSheet.MUSHROOM_BLUEMILK;
	}

	//源仓库里这是唯一的正面播报（GLog.p），其余蘑菇都是警示播报
	@Override
	protected void showEffect() {
		GLog.p( Messages.get( getClass(), "effect" ) );
	}

	@Override
	protected void eatEffect( Hero hero ) {

		boolean lucky = Random.Int( 10 ) == 1;

		for (Mob mob : allMobs()) {
			if (mob.alignment == Char.Alignment.ENEMY) {
				Buff.affect( mob, Slow.class, Slow.DURATION );
				if (!lucky) {
					Buff.affect( mob, BerryRegeneration.class ).level( mob.HT );
				}
			}
		}

		Buff.affect( hero, Haste.class, Haste.DURATION );
		Buff.affect( hero, BerryRegeneration.class ).level( hero.HT );
	}
}
