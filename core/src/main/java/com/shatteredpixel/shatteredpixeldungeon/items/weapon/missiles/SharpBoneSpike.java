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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 「锋锐骨刺」（2026-09-04，环指大师初始投掷武器）。
 * <p>数值套用「飞刺」{@link ThrowingSpike}（baseUses=12、tier=1），
 * 但基础伤害降为 <b>2~3</b>（每级 +1）；特殊效果：命中时施加 1~2 的流血。</p>
 * <p>图标绘制于 items.png 9行16列，10×11（{@link ItemSpriteSheet#SHARP_BONE_SPIKE}）。</p>
 */
public class SharpBoneSpike extends ThrowingSpike {

	{
		image = ItemSpriteSheet.SHARP_BONE_SPIKE;

		//套用飞刺数值：baseUses=12、tier=1（继承自 ThrowingSpike）
	}

	//基础伤害 2~3（0 级），每级 +1
	@Override
	public int min(int lvl) {
		return 2 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 3 + lvl;
	}

	//命中时施加 1~2 的流血
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);
		if (defender.isAlive()){
			Bleeding bleeding = Buff.affect(defender, Bleeding.class);
			if (bleeding != null){
				bleeding.set( Random.Int( 1, 2 ) );
			}
		}
		return damage;
	}
}
