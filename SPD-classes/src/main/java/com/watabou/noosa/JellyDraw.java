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

package com.watabou.noosa;

import com.watabou.glwrap.Quad;

import java.nio.Buffer;
import java.nio.FloatBuffer;

/**
 * 「果冻」摆动（受击形变）的分层绘制，由 {@link Image#draw()} 在 {@code jellyAmp != 0} 时调用。
 *
 * <p><b>为什么要分层</b>：noosa 里一张 {@link Image} 只有一个四边形（4 顶点、4 分量），
 * 单靠顶点只能做出刚性错切（整张图倾斜）。要让轮廓真的「弯」出一条曲线，只能把贴图按高度
 * 切成 {@link #SLICES} 条横带，每条整体横向平移、越靠上平移越多——底边保持不动，
 * 于是「脚底为基准的左右扭曲摆动」就成立了。</p>
 *
 * <p><b>坐标约定</b>：局部坐标 {@code y = 0} 是贴图<b>顶边</b>、{@code y = height} 是<b>底边</b>
 * （见 {@code Image.updateVertices()}），因此「高度比例 t」取 0 在底边、1 在顶边，
 * 横向偏移用 {@code t²} 加权 ⇒ 最底那一条几乎不动，越往上摆幅越大。</p>
 *
 * <p>在这个「横向甩动」之上再叠两种形变，合起来才是果冻而不是纸片：
 * ①<b>竖直压扁 / 拉长</b>（{@code y = h·(1 − t·(1+squash))}，同样以底边为基准 ⇒ 脚底恒定）；
 * ②<b>横向鼓出</b>（压扁时按 t 加宽，近似保体积）。三者零点都在底边，所以「脚底不动」始终成立。</p>
 *
 * <p><b>三个必须守住的点</b>：</p>
 * <ul>
 *   <li><b>不碰 Image 自己的 {@code vertices} / {@code buffer}</b>：顶点写在<b>全局共享的一份</b>
 *       {@link #quads} 里。{@code ItemSprite} 的钻石剑附魔流光直接读 {@code vertices} 当遮罩 UV，
 *       改写它会串味；共享客户端缓冲又天然避开 {@code Vertexbuffer.clear()}（切场景会清空
 *       所有已注册的顶点缓冲）带来的「GL id 失效」问题。</li>
 *   <li><b>UV 跟着层走，且方向别搞反</b>：第 i 层的 v 范围＝当前帧 {@code frame} 按同一比例切分，
 *       并沿用 {@code flipVertical} 的交换（与 {@code Image.updateFrame()} 同构）。
 *       <b>反例</b>：局部 y 从顶边起算、而高度比例 t 从脚底起算，两者相差一个 {@code 1-t}；
 *       若直接拿 t 去插值 UV，贴图会被整个上下颠倒（这是踩过的坑）。</li>
 *   <li><b>邻层留缝</b>：每层底边向下多铺 {@link #SEAM} 像素、UV 同步多取一点，避免浮点边界上
 *       出现 1 像素的横向接缝；最底层不多铺（否则采样会越出帧外，串到贴图册里相邻的帧）。</li>
 * </ul>
 */
class JellyDraw {

	/**
	 * 切层数。贴图帧高 15~16 像素 ⇒ 每层不到 1 像素，弯曲足够圆滑。
	 * 层数越多曲线越细，但相邻层的横向错位量＝{@code 2·amplitude·t / SLICES}
	 * （t 为层高比例），幅度调大后要跟着加层，否则轮廓会露出阶梯感。
	 */
	static final int SLICES = 20;

	/** 每层底边向下多铺的像素数（防接缝）。 */
	static final float SEAM = 0.25f;

	/** 竖直压扁时横向鼓出的比例系数（0.6 ⇒ 压扁 30% 时顶部加宽 18%），近似「保体积」的果冻感。 */
	static final float BULGE = 0.6f;

	/** 共享顶点缓冲（{@link Quad#createSet}：SLICES 个 quad）。单线程逐张绘制，用完即弃、不必清。 */
	private static FloatBuffer quads;

	/** 单层顶点的中转数组（{@code Quad.fill} 写入它，再整块 put 进缓冲）。 */
	private static final float[] scratch = new float[16];

	static void draw( Image img ) {

		if (quads == null) {
			quads = Quad.createSet( SLICES );
		}

		NoosaScript script = img.script();

		img.texture.bind();

		script.camera( img.camera() );

		script.uModel.valueM4( img.matrix );
		script.lighting(
				img.rm, img.gm, img.bm, img.am,
				img.ra, img.ga, img.ba, img.aa );

		//当前帧在贴图里的 UV 边界（flip 交换与 Image.updateFrame() 一致）。
		//注意：局部坐标 y = 0 是贴图【顶边】、y = height 是【底边】（见 Image.updateVertices()），
		//所以 vTop 必须对应 y = 0、vBot 对应 y = height——这里搞反整张图会上下颠倒。
		float u1 = img.flipHorizontal ? img.frame.right : img.frame.left;
		float u2 = img.flipHorizontal ? img.frame.left : img.frame.right;
		float vTop = img.flipVertical ? img.frame.bottom : img.frame.top;
		float vBot = img.flipVertical ? img.frame.top : img.frame.bottom;

		float dxTop = img.jellyOffset();
		float squash = img.jellySquash();
		float w = img.width;
		float h = img.height;

		//竖直形变系数（1 = 原高）。压扁时横向鼓出一点，越往上越多、脚底恒为 0。
		float vk = 1f + squash;
		if (vk < 0.05f) vk = 0.05f;
		float bulge = -squash * BULGE;

		((Buffer)quads).position( 0 );

		for (int i = 0; i < SLICES; i++) {

			float t0 = i / (float)SLICES;           //本层底边的高度比例（0 = 脚底）
			float t1 = (i + 1) / (float)SLICES;     //本层顶边的高度比例

			//本层整体的横向位移：取顶边处的偏移，t² 加权 ⇒ 底边不动、越往上摆幅越大
			float dx = dxTop * t1 * t1;

			//底边向下多铺一点：t0e < t0 ⇒ 更低、UV 取到下一层（仍在同一帧内）
			float t0e = i == 0 ? t0 : Math.max( 0f, t0 - SEAM / Math.max( 1f, h ) );

			//竖直形变：y(t) = h·(1 − t·vk) ⇒ t = 0（脚底）处恒为 h，只有上半身被压扁 / 拉长
			float yA = h * (1f - t1 * vk);
			float yB = h * (1f - t0e * vk);

			//横向鼓出：竖直被压时加宽（同样按 t 加权 ⇒ 脚底不变形）。
			//一个四边形仍是矩形，所以整层取同一个 halfW；相邻层的宽度差 ≈ 2w·bulge/SLICES
			//（20 层、最宽时不到 0.2 像素），看不出台阶。
			float halfW = w * 0.5f * (1f + bulge * t1);
			float cx = dx + w * 0.5f;

			//高度比例 t 从【脚底】起算，而局部 y 从【顶边】起算 ⇒ y = h·(1-t)；
			//UV 必须按同一个 (1-t) 插值，否则贴到顶边的是 frame.bottom，整张图上下颠倒。
			Quad.fill( scratch,
					cx - halfW, cx + halfW, yA, yB,
					u1, u2, lerp( vTop, vBot, 1f - t1 ), lerp( vTop, vBot, 1f - t0e ) );
			quads.put( scratch );
		}

		//客户端顶点数组绘制（与 SurfaceScene / RenderedText 同一条路径），一次 drawElements 画完所有层
		script.drawQuadSet( quads, SLICES );
	}

	private static float lerp( float a, float b, float t ) {
		return a + (b - a) * t;
	}
}
