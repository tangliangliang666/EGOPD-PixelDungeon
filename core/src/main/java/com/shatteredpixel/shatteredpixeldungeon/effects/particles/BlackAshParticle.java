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

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * 笑靥（成长可用状态）的物品显示特效：向下掉落的黑色粒子（黑灰）。
 */
public class BlackAshParticle extends PixelParticle.Shrinking {

	public static final Emitter.Factory FALLING = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((BlackAshParticle)emitter.recycle( BlackAshParticle.class )).reset( x, y );
		}
	};

	public BlackAshParticle() {
		super();

		color( 0x111111 ); //黑色
		lifespan = 1.0f;

		acc.set( 0, +30 ); //向下飘落
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan;

		size = 3;
		speed.set( Random.Float( -8, +8 ), Random.Float( 0, 8 ) );
	}
}
