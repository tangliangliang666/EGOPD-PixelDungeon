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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import java.util.ArrayList;

import com.shatteredpixel.shatteredpixeldungeon.effects.particles.HeatFlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;

/**
 * 「封印之剑」解封形态的<b>屏幕四周火焰</b>：沿屏幕四条边铺一串火线，强度随
 * {@link HeatVignette#heat() 热度}升降（黄→橙→红 依次更旺）。开关同样受设置里的
 * 「莱瓦汀场景特效」约束 —— 关掉后热度归零，火线随之熄灭。
 *
 * <h3>怎么摆的</h3>
 * 每条「火线」＝一个 {@link Emitter}，位置用<b>归一化坐标</b>描述，每帧换算成
 * {@link PixelScene#uiCamera} 的像素尺寸（所以窗口尺寸变化会自动跟随）。
 * 底边四条最盛、左右各两条往内上方飘、顶边三条细小火星向下垂落。
 *
 * <h3>为什么在工具栏后面也看得见</h3>
 * 火苗的上升距离取屏幕高度的 {@link #RISE_FRACTION}（约 18%），远高于工具栏，
 * 于是被工具栏挡住的部分自然隐没、露出来的部分在栏顶之上连成一条火带；
 * 这也顺带兼容了「翻转 UI」布局 —— 工具栏在上时，反而是顶边那条被挡住。
 *
 * <h3>挂在哪一层</h3>
 * 紧跟在 {@link HeatVignette} 之后 add ⇒ 压在温度滤镜<b>之上</b>（火苗不被染色）、
 * 但在所有 UI <b>之下</b>（不遮挡状态栏/工具栏/窗口）。
 */
public class HeatFlames extends Group {

	/** 低于这个强度就一条火线都不点了（避免热度归零后还在零星冒火星）。 */
	private static final float MIN_INTENSITY = 0.05f;

	/** 火苗在寿命内飘出的距离，占屏幕高度的比例。 */
	private static final float RISE_FRACTION = 0.18f;

	/** 基准寿命（秒）。 */
	private static final float BASE_LIFE = 0.9f;

	/** 粒子基准边长（像素）与随强度增长的幅度。 */
	private static final float BASE_SIZE = 3.2f;
	private static final float SIZE_GAIN = 2.4f;

	/** 一条火线：一段铺在屏幕边上的发射区，加上它的漂移方向。 */
	private static class Line {
		final float nx, ny, nw, nh;      //归一化位置与范围
		final float ax, ay;              //漂移方向（乘上速度；ay 为负＝向上）
		final float sizeScale;           //相对粒子基准大小的缩放
		final float baseInterval;        //满强度时的发射间隔（秒）
		final Emitter emitter;
		final Emitter.Factory factory;

		/** 当前实际间隔；-1 表示这条线当前没点火。 */
		float current = -1f;

		Line(float nx, float ny, float nw, float nh, float ax, float ay,
				float sizeScale, float baseInterval, Emitter emitter, Emitter.Factory factory) {
			this.nx = nx;
			this.ny = ny;
			this.nw = nw;
			this.nh = nh;
			this.ax = ax;
			this.ay = ay;
			this.sizeScale = sizeScale;
			this.baseInterval = baseInterval;
			this.emitter = emitter;
			this.factory = factory;
		}
	}

	private final ArrayList<Line> lines = new ArrayList<>();

	/** 当前火苗颜色 / 大小 / 漂移速度，由 {@link #update()} 按热度刷新，供各工厂读取。 */
	private int flameColor = 0xFFE070;
	private float flameSize = BASE_SIZE;
	private float speed = 200f;

	public HeatFlames() {
		//底边：最盛的四段
		for (int i = 0; i < 4; i++) {
			line( i * 0.25f, 1f, 0.25f, 0f, 0f, -1f, 1.10f, 0.10f );
		}
		//左右：各两段，往内上方飘
		for (int i = 0; i < 2; i++) {
			float ny = 0.36f + i * 0.34f;
			line( 0f, ny, 0f, 0.34f,  0.45f, -1f, 0.90f, 0.17f );
			line( 1f, ny, 0f, 0.34f, -0.45f, -1f, 0.90f, 0.17f );
		}
		//顶边：三段细小的火星，向下飘
		for (int i = 0; i < 3; i++) {
			line( 0.14f + i * 0.36f, 0f, 0.22f, 0f, 0f, 0.55f, 0.70f, 0.34f );
		}
	}

	/** 建一条火线（连同它自己的 Emitter 与粒子工厂）。 */
	private void line(float nx, float ny, float nw, float nh,
			float ax, float ay, float sizeScale, float baseInterval) {
		Emitter emitter = new Emitter();
		//我们自己用手动开关控制，绝不能让它在没粒子时自杀（自杀后永远不再发射）
		emitter.autoKill = false;
		emitter.camera = PixelScene.uiCamera;
		add(emitter);

		lines.add(new Line(nx, ny, nw, nh, ax, ay, sizeScale, baseInterval,
				emitter, factoryFor(ax, ay, sizeScale)));
	}

	/**
	 * 每条火线一个工厂：方向与缩放<b>固化在工厂里</b>，颜色/速度/大小则在
	 * {@code emit()} 那一刻从外层实例现读 —— 所以不必给每种方向各写一个粒子类。
	 */
	private Emitter.Factory factoryFor( final float ax, final float ay, final float sizeScale ) {
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter emitter, int index, float x, float y ) {
				((HeatFlameParticle)emitter.recycle( HeatFlameParticle.class )).reset(
						x, y,
						flameColor,
						speed * ax, speed * ay,
						flameSize * sizeScale,
						ay > 0f ? BASE_LIFE * 0.75f : BASE_LIFE );
			}
			@Override
			public boolean lightMode() {
				return true;
			}
		};
	}

	@Override
	public void update() {
		//先按最新热度把参数配好，再让 super.update() 去发射：发射是同步的，用的就是这份参数
		if (PixelScene.uiCamera != null) {
			float w = PixelScene.uiCamera.width;
			float h = PixelScene.uiCamera.height;

			float heat = HeatVignette.heat();
			//1.4 次方让火苗「先少后旺」：黄色只有零星几颗，红色才连成一片
			float intensity = heat <= 0f ? 0f : (float) Math.pow(Math.min(1f, heat / 3f), 1.4f);

			flameColor = HeatVignette.flameColor();
			flameSize = BASE_SIZE + SIZE_GAIN * intensity;
			//让一颗火苗在寿命内大致飘出 RISE_FRACTION 个屏幕高：½·a·t² = rise
			speed = 2f * (h * RISE_FRACTION) / (BASE_LIFE * BASE_LIFE);

			for (Line l : lines) {
				//uiCamera 在窗口尺寸变化时会被重建，所以每帧重新认一次
				l.emitter.camera = PixelScene.uiCamera;
				l.emitter.pos(l.nx * w, l.ny * h, l.nw * w, l.nh * h);

				if (intensity <= MIN_INTENSITY) {
					if (l.current >= 0f) {
						l.current = -1f;
						l.emitter.on = false;
					}
					continue;
				}

				float interval = l.baseInterval / Math.max(0.3f, intensity);
				//密度只按档位重设，别每帧重排（会一直重置发射计时器）
				if (l.current < 0f || interval < l.current * 0.85f || interval > l.current * 1.2f) {
					l.current = interval;
					//delay=0 ⇒ 立刻产出第一颗，淡入时才不会先空一拍
					l.emitter.startDelayed(l.factory, interval, 0, 0f);
				}
			}
		}

		super.update();
	}
}
