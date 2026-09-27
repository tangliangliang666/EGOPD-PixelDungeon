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

import com.badlogic.gdx.Gdx;
import com.watabou.glscripts.Script;
import com.watabou.gltextures.SmartTexture;
import com.watabou.glwrap.Attribute;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Texture;
import com.watabou.glwrap.Uniform;
import com.watabou.glwrap.Vertexbuffer;

/**
 * 双纹理变体：主贴图 uTex（单元 0）+ 遮罩贴图 uMask（单元 1）。
 * 片元里把主贴图颜色乘以遮罩贴图对应位置的 alpha，从而实现“只显示遮罩不透明区域”的裁剪。
 * <p>
 * 顶点格式为每顶点 6 个 float：x, y, u, v, maskU, maskV —— 见 {@link #drawMaskedQuad(Vertexbuffer)}。
 * 遮罩坐标（maskU/maskV）通常直接拷贝同一图标 quad 的 UV，使遮罩与主图逐像素对齐。
 */
public class NoosaScriptGlint extends NoosaScript {

	public Uniform uMask;
	public Attribute aMaskUV;

	public NoosaScriptGlint() {
		super();
		uMask = uniform( "uMask" );
		aMaskUV = attribute( "aMaskUV" );
	}

	@Override
	public void use() {
		super.use();
		aMaskUV.enable();
		//采样器恒定绑定纹理单元 1（主贴图仍在单元 0）；此值随 program 一起保存，切换回来时由 use() 重设
		Gdx.gl20.glUniform1i( uMask.location(), 1 );
	}

	/**
	 * 把遮罩贴图绑定到纹理单元 1，随后把激活单元恢复为 0（引擎其余绘制都假定单元 0 为激活态）。
	 * 调用方应在之后 bind 主贴图到单元 0 并调用 {@link #drawMaskedQuad(Vertexbuffer)}。
	 *
	 * <p>注意：这里<b>不能</b>用 {@link Texture#bind()} —— 该引擎的 {@link Texture} 以单一静态
	 * bound_id 记录“当前绑定纹理”且不区分纹理单元。图标绘制用的贴图（items.png 等）往往刚在单元 0
	 * 上画过，bound_id 恰好等于遮罩贴图 id，bind() 会短路跳过 glBindTexture → 单元 1 从未真正绑上遮罩，
	 * 采样到默认空纹理（alpha=1）→ 光效退化为整块方形。故这里用原始 glBindTexture 强制绑定单元 1；
	 * bound_id 语义（代表单元 0 当前绑定）不受影响。
	 */
	public void bindMask( SmartTexture mask ) {
		if (mask == null || mask.id == -1) return;
		Texture.activate( 1 );
		Gdx.gl.glBindTexture( Gdx.gl.GL_TEXTURE_2D, mask.id );
		Texture.activate( 0 );
	}

	/** 绘制 6-float/顶点的 quad（布局见类注释）；主贴图与遮罩需已分别绑定到单元 0/1 */
	public void drawMaskedQuad( Vertexbuffer buffer ) {

		buffer.updateGLData();

		buffer.bind();

		aXY.vertexBuffer( 2, 6, 0 );
		aUV.vertexBuffer( 2, 6, 2 );
		aMaskUV.vertexBuffer( 2, 6, 4 );

		buffer.release();

		Gdx.gl20.glDrawElements( Gdx.gl20.GL_TRIANGLES, Quad.SIZE, Gdx.gl20.GL_UNSIGNED_SHORT, 0 );
	}

	public static NoosaScriptGlint get() {
		return Script.use( NoosaScriptGlint.class );
	}

	@Override
	protected String shader() {
		return SHADER;
	}

	private static final String SHADER =

		//vertex shader
		"uniform mat4 uCamera;\n" +
		"uniform mat4 uModel;\n" +
		"attribute vec4 aXYZW;\n" +
		"attribute vec2 aUV;\n" +
		"attribute vec2 aMaskUV;\n" +
		"varying vec2 vUV;\n" +
		"varying vec2 vMaskUV;\n" +
		"void main() {\n" +
		"  gl_Position = uCamera * uModel * aXYZW;\n" +
		"  vUV = aUV;\n" +
		"  vMaskUV = aMaskUV;\n" +
		"}\n" +

		//this symbol separates the vertex and fragment shaders (see Script.compile)
		"//\n" +

		//fragment shader
		//preprocessor directives let us define precision on GLES platforms, and ignore it elsewhere
		"#ifdef GL_ES\n" +
		"  precision mediump float;\n" +
		"#endif\n" +
		"varying vec2 vUV;\n" +
		"varying vec2 vMaskUV;\n" +
		"uniform sampler2D uTex;\n" +
		"uniform sampler2D uMask;\n" +
		"uniform vec4 uColorM;\n" +
		"uniform vec4 uColorA;\n" +
		"void main() {\n" +
		//alpha 同时作用于 rgb 与 a，保持预乘一致，遮罩为 0 的区域完全不贡献颜色
		"  vec4 glint = texture2D( uTex, vUV ) * uColorM;\n" +
		"  float maskA = texture2D( uMask, vMaskUV ).a;\n" +
		"  gl_FragColor = glint * maskA + uColorA;\n" +
		"}\n";
}
