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

import com.watabou.glscripts.Script;
import com.watabou.glwrap.Uniform;

/**
 * 「热浪」扭曲着色器：在<b>屏幕空间</b>给采样 UV 叠一组正弦抖动，模拟热空气造成的画面折射。
 *
 * <h3>和 {@link NoosaScript} 的关系</h3>
 * 片元逻辑只比默认脚本多一段「算抖动 → 偏移 vUV」，光照（{@code uColorM}/{@code uColorA}）完全保留，
 * 所以任何本来走默认脚本的元素（{@code Image}/{@code Tilemap}/{@code RenderedText}…）换成它都不会变色。
 *
 * <p>{@link NoosaScript#get()} 与 {@link NoosaScriptNoLighting#get()} 在 {@link #enabled} 为真时会返回
 * 本类实例 —— 这是「谁被扭曲」的<b>唯一开关</b>：核心侧（GameScene 里的温度滤镜）每帧只在<b>世界层</b>
 * 打开它、在<b>第一件 UI 之前</b>关掉它，于是世界被折射而 UI 不动。</p>
 *
 * <h3>为什么用 vScreen 而不是 gl_FragCoord</h3>
 * {@code gl_FragCoord} 在部分移动 GPU 上只有 mediump 精度，1080p 下误差可达数个像素；
 * 顶点着色器里的裁剪空间坐标 {@code (uCamera*uModel*aXYZW).xy} 天然是 [-1,1]，精度足够且更省。
 *
 * <h3>时间精度</h3>
 * 相位用的 {@code uWaveTime} 在 Java 侧按 {@code 2π} 回绕，且所有时间系数都取整数
 * （1 / 2 / 3）⇒ 回绕点上相位连续，不会有跳变；参数幅度始终很小，mediump 足够。
 */
public class NoosaScriptWarp extends NoosaScript {

	/** 是否启用整屏扭曲。<b>由核心侧每帧驱动</b>，是「谁被扭曲」的唯一开关。 */
	public static boolean enabled = false;

	private Uniform uWaveTime;
	private Uniform uWaveAmp;
	private Uniform uWaveRes;
	private Uniform uWaveEdge;

	/** 本脚本全局唯一实例；只为在没有 {@code Script} 实例句柄时也能清相机缓存。 */
	private static NoosaScriptWarp instance;

	public NoosaScriptWarp() {
		super();

		uWaveTime = uniform( "uWaveTime" );
		uWaveAmp  = uniform( "uWaveAmp" );
		uWaveRes  = uniform( "uWaveRes" );
		uWaveEdge = uniform( "uWaveEdge" );
	}

	public static NoosaScriptWarp get(){
		instance = Script.use( NoosaScriptWarp.class );
		return instance;
	}

	/**
	 * 清掉「上一次的相机」缓存（由 {@code Game.render()} 每帧调用一次）。
	 *
	 * <p>{@link NoosaScript#camera(Camera)} 只在相机<b>对象</b>变化时才上传 {@code uCamera}，
	 * 而 {@code Camera.main} 是同一个对象、其 matrix 数组每帧原地改写 —— 所以不每帧强制清一次，
	 * 热浪脚本的相机矩阵就只会被上传一次，画面将不再跟随镜头。这里不绑定 program，开销可忽略。</p>
	 */
	public static void resetCameraCache() {
		if (instance != null) {
			instance.resetCamera();
		}
	}

	/**
	 * 上传一次扭曲参数。
	 *
	 * <p>uniform 值属于 program 对象，会一直保留到下次修改，所以<b>每帧上传一次就够</b>，
	 * 不需要（也不应该）在每次 {@code get()} 时重传。</p>
	 *
	 * @param time       相位（弧度，0~2π 循环）
	 * @param ampPixels  屏幕边缘处的最大像素偏移
	 * @param edgeBias   0＝全屏一致，1＝只在屏幕边缘明显（中心几乎不扭曲，和温度滤镜的观感一致）
	 */
	public static void upload( float time, float ampPixels, float edgeBias ) {
		NoosaScriptWarp s = get();
		s.uWaveTime.value1f( time );
		s.uWaveAmp.value2f( ampPixels, ampPixels * 0.6f );
		s.uWaveRes.value2f( Game.width, Game.height );
		s.uWaveEdge.value1f( edgeBias );
	}

	@Override
	protected String shader() {
		return SHADER;
	}

	static final String SHADER =

		//vertex shader
		"uniform mat4 uCamera;\n" +
		"uniform mat4 uModel;\n" +
		"attribute vec4 aXYZW;\n" +
		"attribute vec2 aUV;\n" +
		"varying vec2 vUV;\n" +
		"varying vec2 vScreen;\n" +
		"void main() {\n" +
		"  vec4 p = uCamera * uModel * aXYZW;\n" +
		"  gl_Position = p;\n" +
		"  vUV = aUV;\n" +
		"  vScreen = p.xy;\n" +   //裁剪空间 [-1,1]，片元里换算成屏幕比例
		"}\n" +

		//this symbol separates the vertex and fragment shaders (see Script.compile)
		"//\n" +

		//fragment shader
		"#ifdef GL_ES\n" +
		"  precision mediump float;\n" +
		"#endif\n" +
		"varying vec2 vUV;\n" +
		"varying vec2 vScreen;\n" +
		"uniform sampler2D uTex;\n" +
		"uniform vec4 uColorM;\n" +
		"uniform vec4 uColorA;\n" +
		"uniform float uWaveTime;\n" +
		"uniform vec2 uWaveAmp;\n" +
		"uniform vec2 uWaveRes;\n" +
		"uniform float uWaveEdge;\n" +
		"void main() {\n" +
		"  vec2 sp = vScreen * 0.5 + 0.5;\n" +          //0~1 的屏幕位置
		"  float t = uWaveTime;\n" +
		//两组不同频率的纵向波纹叠加成横向抖动，再给一点纵向，避免看出是规则的正弦
		"  float w = sin(sp.y * 42.0 + t) * 0.55\n" +
		"          + sin(sp.y * 13.0 - 2.0 * t + sp.x * 5.0) * 0.45;\n" +
		"  float v = sin(sp.x * 11.0 - 3.0 * t) * 0.5;\n" +
		//离屏幕中心越远越强：中心保持清晰，边缘热浪最盛
		"  float d = clamp(length((sp - 0.5) * 2.0), 0.0, 1.0);\n" +
		"  float k = mix(1.0 - uWaveEdge, 1.0, d);\n" +
		"  vec2 wob = vec2(w, v) * uWaveAmp * k / uWaveRes;\n" +
		"  gl_FragColor = texture2D( uTex, vUV + wob ) * uColorM + uColorA;\n" +
		"}\n";
}
