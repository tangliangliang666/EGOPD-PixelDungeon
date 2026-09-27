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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.Emitter.Factory;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.ColorMath;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

/**
 * 「仇怨」（复仇账簿激活时的 buff）用的紫色粒子：从角色身上<b>向四周飞散</b>、
 * 一边飞一边变小变深、末期淡出的像素方块。
 *
 * <p>与 {@link PurpleParticle}（腐化/紫雾那套，速度只有 ±5 的乱飘）不同，本类的初速是
 * <b>整圈极坐标</b>（{@link PointF#PI2 随机角度} + 12~30 像素/秒），所以粒子是「从角色向外辐射」，
 * 而不是原地抖动；再叠一点点向上的浮力，免得整体看起来像下坠的碎石。</p>
 *
 * <p>{@link #RAGE} 的 {@code lightMode()} 返回 true ⇒ 加色混合，紫光叠在画面上才是发光的，
 * 而不是一层灰扑扑的脏色。</p>
 *
 * <p>用法见 {@code RevengeLedger.Rancor#ensureAura()}：发射器由
 * {@code CharSprite.emitter()} 领（跟随角色贴图），本类只负责「一颗粒子长什么样」。</p>
 */
public class RancorParticle extends PixelParticle {

	/** 主色：末期的深紫（尾）。 */
	private static final int DEEP = 0x3B0A6B;

	/** 主色：刚喷出时的亮紫（头）。 */
	private static final int BRIGHT = 0xCB86FF;

	/** 单颗寿命（秒）。 */
	private static final float LIFESPAN = 0.7f;

	/** 初速区间（像素/秒）。 */
	private static final float MIN_SPEED = 12f;
	private static final float MAX_SPEED = 30f;

	public static final Emitter.Factory RAGE = new Factory() {
		@Override
		public void emit( Emitter emitter, int index, float x, float y ) {
			((RancorParticle)emitter.recycle( RancorParticle.class )).reset( x, y );
		}
		@Override
		public boolean lightMode() {
			return true; //加色混合：紫光要「亮」而不是把画面压暗
		}
	};

	public RancorParticle() {
		super();
		lifespan = LIFESPAN;
	}

	public void reset( float x, float y ) {
		revive();

		this.x = x;
		this.y = y;

		//整圈随机方向 + 随机初速 ⇒ 从角色向四周辐射
		speed.polar( Random.Float( PointF.PI2 ), Random.Float( MIN_SPEED, MAX_SPEED ) );
		//轻微上浮：纯放射看起来太「机械」，带一点浮力更像怨气在冒
		speed.y -= Random.Float( 3f, 9f );

		left = lifespan;
	}

	@Override
	public void update() {
		super.update();

		float p = left / lifespan; //1（刚出生）→ 0（将消亡）

		//越飞越小：4 像素 → 1 像素
		size( 1f + 3f * p );

		//颜色：亮紫 → 深紫
		color( ColorMath.interpolate( DEEP, BRIGHT, p ) );

		//出生一瞬快速亮起，中段实心，末期淡出
		if (p > 0.9f) {
			am = (1f - p) * 10f;
		} else {
			am = Math.min( 1f, p / 0.35f );
		}
	}
}
