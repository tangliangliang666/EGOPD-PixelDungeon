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

/**
 * 「泪剑」的深蓝色拖尾粒子。
 * <p>由 {@link com.shatteredpixel.shatteredpixeldungeon.effects.TearSwordVisual} 在每柄泪剑的中心处
 * 按固定间隔生成：粒子原地不动，仅随时间<b>淡出并收缩</b>，因而角色移动时会在剑后留下一串逐渐消失的深蓝色残影。</p>
 * <p>结构照抄 {@code PinkParticle}：{@code PixelParticle} 本质是 1×1 纯白方块（{@code PseudoPixel}），
 * 用 {@code color(...)} 直接替换成纯色、用 {@code size(...)} 控制像素边长、用 {@code am} 控制透明度。
 * 粒子存活期结束由基类 {@code update()} 自动 {@code kill()}，槽位可被 {@code Group.recycle} 复用。</p>
 */
public class TearSwordTrailParticle extends PixelParticle {

	/** 深蓝色（拖尾主色）。 */
	public static final int COLOR = 0x1E3A8A;

	/** 单颗粒子的存活时间（秒），与 {@code TearSwordVisual.TRAIL_INTERVAL} 共同决定拖尾长度。 */
	public static final float LIFESPAN = 0.30f;

	public TearSwordTrailParticle() {
		super();
		lifespan = LIFESPAN;
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		left = lifespan;

		color( COLOR );
		size( MAX_SIZE );

		//revive() 只重置 exists/alive，不还原透明度：复用旧槽位时必须手动复位，
		//否则重生后的第一帧会沿用上一世临死时的 am（几乎透明）而"闪一下"。
		am = 1f;
	}

	/** 生成的初始边长（像素）。 */
	private static final float MAX_SIZE = 1.6f;
	/** 消散时的终止边长（像素）。 */
	private static final float MIN_SIZE = 0.6f;

	@Override
	public void update() {
		super.update();

		// am: 1 -> 0 线性淡出；同时 1.6 -> 0.6 像素收缩，形成"尾部变细"的观感
		am = left / lifespan;
		size( MIN_SIZE + (MAX_SIZE - MIN_SIZE) * am );
	}
}
