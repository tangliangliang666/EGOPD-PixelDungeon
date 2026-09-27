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
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

/**
 * 「白色烟雾」粒子 —— 洛伊德护符（{@code items/LloydTalisman}）的命中特效，
 * 以及「禁疗」（{@code actors.buffs.HealBlock}）期间在目标身上持续散发的烟雾。
 *
 * <p>原版 {@link SmokeParticle} 是深灰（{@code 0x222222}），烟雾弹 / 爆炸 / 火焰法杖那套都用它；
 * 这里刻意另做一个<b>纯白</b>版本，不去改原版配色——改原版会把烟雾弹、烈焰法杖的粒子一起改掉。</p>
 *
 * <p>两个工厂：
 * <ul>
 *   <li>{@link #BURST} —— 命中瞬间从目标身上炸开一簇（{@code sprite.emitter().burst(BURST, n)}）；</li>
 *   <li>{@link #FACTORY} —— 持续缭绕（{@code sprite.emitter().pour(FACTORY, 0.09f)}）。</li>
 * </ul></p>
 */
public class WhiteSmokeParticle extends PixelParticle {

	/** 持续档：缓缓上浮、慢慢淡出的白烟。 */
	public static final Emitter.Factory FACTORY = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((WhiteSmokeParticle)emitter.recycle( WhiteSmokeParticle.class )).reset( x, y );
		}
	};

	/** 爆发档：向四周炸开，速度更快、寿命更短。 */
	public static final Emitter.Factory BURST = new Emitter.Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((WhiteSmokeParticle)emitter.recycle( WhiteSmokeParticle.class )).resetBurst( x, y );
		}
	};

	public WhiteSmokeParticle() {
		super();

		color( 0xFFFFFF );

		//整体向上飘（负 y 加速度）
		acc.set( 0, -22 );
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan = Random.Float( 0.7f, 1.2f );
		speed.set( Random.Float( -5, +5 ), Random.Float( -14, -4 ) );
	}

	public void resetBurst( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan = Random.Float( 0.5f, 0.9f );
		speed.polar( Random.Float( PointF.PI2 ), Random.Float( 20, 44 ) );
	}

	@Override
	public void update() {
		super.update();

		float p = left / lifespan;
		//先快速涨亮、再慢慢淡出；体积由小变大（烟雾扩散感）
		am = p > 0.75f ? 2 - 2*p : p * 0.66f;
		//尺寸 = 原值的一半（6.5~3.0，原 13~6）⇒ 面积只有原先的四分之一
		size( 6.5f - p * 3.5f );
	}
}
