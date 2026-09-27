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

/**
 * 心-命运的粒子效果：套用燃烧状态的火焰粒子（{@link FlameParticle}），
 * 但颜色改为金色，且向上运动速度（加速度）为燃烧状态的 50%。
 */
public class HeartFateParticle extends PixelParticle.Shrinking {

	public static final Emitter.Factory FACTORY = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((HeartFateParticle)emitter.recycle( HeartFateParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	public HeartFateParticle() {
		super();

		color( 0xFFD700 ); //金色
		lifespan = 0.6f;

		acc.set( 0, -40 ); //燃烧粒子（0,-80）运动速度的 50%
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan;

		size = 4;
		speed.set( 0 );
	}

	@Override
	public void update() {
		super.update();
		float p = left / lifespan;
		am = p > 0.8f ? (1 - p) * 5 : 1;
	}
}
