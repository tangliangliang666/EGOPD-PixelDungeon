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

import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/**
 * 屏幕四周火焰用的火苗：一颗会一边飘一边缩小、并按热度着色的像素方块。
 *
 * <p>与 {@link FlameParticle} 的区别是「颜色、漂移方向、大小、寿命都由发射方喂」——
 * 所以本类<b>不提供</b>通用 {@code FACTORY}，而是由 {@code HeatFlames} 按每条火线的方向
 * 各建一个 {@code Emitter.Factory}（见 {@code HeatFlames#factoryFor}）。</p>
 *
 * <p>{@code lightMode()} 由那个工厂返回 true ⇒ 加色混合，火苗叠在画面上才是亮芯而不是一层脏色。</p>
 */
public class HeatFlameParticle extends PixelParticle.Shrinking {

	public HeatFlameParticle() {
		super();
		lifespan = 0.9f;
	}

	/**
	 * @param color    粒子颜色
	 * @param ax       横向加速度（像素/秒²）
	 * @param ay       纵向加速度（像素/秒²；noosa 里 y 轴向下为正，所以「向上飘」要传负值）
	 * @param size     边长（像素）
	 * @param lifespan 寿命（秒）
	 */
	public void reset( float x, float y, int color, float ax, float ay, float size, float lifespan ) {
		revive();

		this.x = x;
		this.y = y;

		this.lifespan = lifespan;
		left = lifespan;
		this.size = size;
		speed.set( 0 );

		//给一点横向抖动，免得一列火苗整齐得像栅栏
		float jitter = Math.abs(ay) * 0.18f;
		acc.set( ax + Random.Float( -jitter, jitter ), ay );

		color( color );
	}

	@Override
	public void update() {
		super.update();

		float p = left / lifespan;
		//出生时快速亮起、末期淡出，中段保持实心
		if (p > 0.85f) {
			am = (1f - p) / 0.15f;
		} else if (p < 0.35f) {
			am = p / 0.35f;
		} else {
			am = 1f;
		}
	}
}
