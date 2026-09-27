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
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

/**
 * 「次元撕裂者」的裂隙粒子：命中时在目标身上炸开、传送时在起点炸散并在终点汇聚。
 *
 * <h3>配色：六个色调全部取自制图本身</h3>
 * <p>{@link #COLORS} 是<b>直接采自武器贴图</b>（{@code items.png} 的 {@code xy(6,41)} 那一格）的
 * 六色，按明度从白排到深紫。那一格里实际共有 9 种颜色：除了这 6 色，还有
 * <b>一种半透明纯黑</b>（{@code #000000}，alpha＝102，是像素画的外描边）与
 * <b>两处棕色</b>（{@code #2E0301}、{@code #561F20}，刀柄/配饰的暗部）——
 * 这两种都不是「色调」，刻意不计入，所以正好<b>六色</b>。</p>
 *
 * <p>辨认方式（核验脚本也按这条判）：六种紫都满足 {@code B ≥ R}，两处棕色都是 {@code R > B}，
 * 描边则是 {@code alpha < 255}。</p>
 *
 * <p>粒子<b>不做颜色插值</b>（本文件里找不到 {@code ColorMath}）：插值会混出贴图之外的中间色，
 * 那就不是「这六种」了。观感上的「冷却」靠<b>色阶整档下沉</b>实现——见 {@link #update()}。</p>
 *
 * <h3>三个 Factory：一套「传送」的视觉词汇</h3>
 * <ul>
 *   <li>{@link #HIT}：贴着被打中的目标向四周炸开一小蓬（射程近、粒子少）。</li>
 *   <li>{@link #SCATTER}：传送<b>起点</b>向外炸散、射程更远更快 ⇒ 读作「人被撕散了」。</li>
 *   <li>{@link #IMPLODE}：传送<b>终点</b>从四周围拢回中心 ⇒ 读作「人重新拼起来」。
 *       速度取「半径 ÷ 存活期」，所以粒子<b>正好</b>在寿命耗尽的同一帧抵达落点，不会越过。</li>
 * </ul>
 * <p>三者都开 {@code lightMode()}（叠加混合）——紫色在加色模式下才有「裂隙发光」的味道。</p>
 *
 * <h3>为什么按发射序号循环取色</h3>
 * <p>厂里拿到的是本轮的发射计数 {@code index}（{@code Emitter} 每次 {@code start} 从 0 重数），
 * 于是 {@code index % COLORS.length} 会让同一蓬粒子的颜色<b>按白 → 深紫的顺序轮着来</b>：
 * 只要一次喷够 6 颗，六色必定全部出场，不会出现「随机取色恰好全是深紫」这种看不见层次的情况。</p>
 */
public class RiftParticle extends PixelParticle {

	/**
	 * 贴图取色结果：六个色调，白 → 深紫。
	 * <p>唯一权威来源就在这里；要改配色只改这一个数组（核验脚本会拿它跟 items.png 逐色比对）。</p>
	 */
	public static final int[] COLORS = {
			0xFFFFFF, //白（刀身高光）
			0xF1B8FC, //淡紫白
			0xD887F0, //亮兰紫（刀身主色）
			0xAE52DD, //中紫
			0x6A42BA, //深紫
			0x4638A8, //靛紫（最深）
	};

	/** 命中：贴着被打中的目标向外炸开一小蓬。 */
	public static final Emitter.Factory HIT = new Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((RiftParticle)emitter.recycle(RiftParticle.class)).resetHit(x, y, index);
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** 传送起点：向外炸散，射程更远更快（「人被撕散」）。 */
	public static final Emitter.Factory SCATTER = new Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((RiftParticle)emitter.recycle(RiftParticle.class)).resetScatter(x, y, index);
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** 传送终点：从四周围拢回中心（「人重新拼起来」）。 */
	public static final Emitter.Factory IMPLODE = new Factory() {
		@Override
		public void emit(Emitter emitter, int index, float x, float y) {
			((RiftParticle)emitter.recycle(RiftParticle.class)).resetImplode(x, y, index);
		}
		@Override
		public boolean lightMode() {
			return true;
		}
	};

	/** 生成时的初始边长（像素）。 */
	private static final float MAX_SIZE = 3.2f;
	/** 消散时的终止边长（像素）。 */
	private static final float MIN_SIZE = 1.1f;

	/** 本世的起始色阶下标（0＝白 … {@code COLORS.length-1}＝靛紫）。 */
	private int base;

	public RiftParticle() {
		super();
		lifespan = 0.4f; //实际值由各 reset 覆写，这里只是给个安全初值
	}

	/** 命中：原地向四周炸开。 */
	public void resetHit(float x, float y, int index) {
		revive();

		this.x = x;
		this.y = y;

		initColor(index);
		left = lifespan = Random.Float(0.30f, 0.50f);

		speed.polar(Random.Float(PointF.PI2), Random.Float(24, 52));
		acc.set(0, -12); //轻微上飘，免得像洒了一地

		size(MAX_SIZE);
		//revive() 只重置 exists/alive，不还原透明度：复用旧槽位时必须手动复位，
		//否则重生后的第一帧会沿用上一世临死时的 am（几乎透明）而「闪一下」。
		am = 1f;
	}

	/** 传送起点：更远更快的向外炸散。 */
	public void resetScatter(float x, float y, int index) {
		revive();

		this.x = x;
		this.y = y;

		initColor(index);
		left = lifespan = Random.Float(0.35f, 0.55f);

		speed.polar(Random.Float(PointF.PI2), Random.Float(50, 95));
		acc.set(0, -16);

		size(MAX_SIZE);
		am = 1f;
	}

	/** 传送终点：从中心外围出发、向心收拢。 */
	public void resetImplode(float cx, float cy, int index) {
		revive();

		//先在中心外围随机取一点（参数名用 cx/cy，别和字段 x/y 撞）
		float ang = Random.Float(PointF.PI2);
		float r = Random.Float(9, 15);
		this.x = cx + (float)Math.cos(ang) * r;
		this.y = cy + (float)Math.sin(ang) * r;

		initColor(index);
		left = lifespan = Random.Float(0.25f, 0.40f);

		//速度＝位移 ÷ 存活期 ⇒ 粒子正好在寿终那一帧抵达中心，不会冲过头
		speed.set(cx - this.x, cy - this.y);
		speed.scale(1f / lifespan);
		acc.set(0);

		size(MAX_SIZE);
		am = 1f;
	}

	/**
	 * 起始色阶：按发射序号在 {@link #COLORS} 里顺序循环（0 ⇒ 白，5 ⇒ 靛紫）。
	 * 一次 burst 只要凑够 6 颗，六色必定全部出场。
	 */
	private void initColor(int index) {
		base = index % COLORS.length;
		color(COLORS[base]);
	}

	@Override
	public void update() {
		super.update();

		//am: 1 → 0 线性淡出，同时 3.2 → 1.1 像素收缩，读作「碎片消散」。
		//钳到 0：寿终那一帧 super.update() 已把 left 减到 <= 0，不钳会算出负边长。
		float a = Math.max(0, left / lifespan);
		am = a;
		size(MIN_SIZE + (MAX_SIZE - MIN_SIZE) * a);

		//色阶随时间整档下沉一档（白 → 淡紫 → … → 靛紫）。全程只取 COLORS 里的值，
		//不插值 —— 这样画面上出现的颜色永远只是贴图里的这六种。
		color(COLORS[Math.min(base + (a < 0.5f ? 1 : 0), COLORS.length - 1)]);
	}
}
