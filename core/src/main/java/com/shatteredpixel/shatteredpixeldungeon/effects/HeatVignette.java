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

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NoosaScriptWarp;

/**
 * 「封印之剑」解封形态的<b>温度滤镜</b>：<b>屏幕四周最强、屏幕中央接近透明</b>的半透明色罩。
 *
 * <p>三种解封形态对应三种温度：一阶段＝黄、二阶段＝橙、莱瓦汀＝红（见 {@link #HEAT_COLORS}）。</p>
 *
 * <p>本类同时是「莱瓦汀场景特效」的<b>总控制台</b>：它持有唯一的平滑热度 {@link #heat}（0~3），
 * 并把它推给另外两套表现 ——
 * {@link HeatFlames 屏幕四周的火苗}（{@link #heat()}）与
 * {@link NoosaScriptWarp 热浪扭曲}（{@link #beginWorldPass()}）。</p>
 *
 * <h3>怎么画出来的</h3>
 * <ul>
 *   <li>贴图：一张 {@code 64×64} 的<b>白色径向渐变</b>（{@code RGB=白}、{@code A=} 由中心 0 递增到边缘 1），
 *       全局缓存一张（{@link #TEXTURE_KEY}），三种颜色靠 {@code hardlight()} 现场染，不重复占显存。</li>
 *   <li>铺屏：{@code Image} 自身的 quad 只有 64×64，用 {@code scale} 拉伸到整个 {@code uiCamera}
 *       ⇒ 渐变被拉成<b>椭圆</b>，四条屏幕边缘正好落在渐变最外圈。</li>
 *   <li>着色：{@code hardlight(color)} 是纯乘算（{@code uColorM}），{@code alpha(a)} 走 {@code am}
 *       ⇒ 片元 {@code gl_FragColor = tex * uColorM}，再按 {@code SRC_ALPHA / ONE_MINUS_SRC_ALPHA} 混合，
 *       得到「按渐变强度给画面染一层色」的效果。</li>
 * </ul>
 *
 * <h3>为什么不遮 UI / 为什么扭曲不糊 UI</h3>
 * 它是 {@code GameScene} 里<b>最后一个世界层元素、第一个 UI 元素之前</b>被 {@code add()} 进去的，
 * 而 noosa 的 {@code Group.draw()} 严格按加入顺序绘制 ⇒ 世界层在它下面着色，状态栏/工具栏/动作按钮/
 * 窗口全部在它上面，不受影响。
 *
 * <p>热浪扭曲用的是同一处「分界线」：扭曲开关在 {@link #update()} 里打开（每帧的 update 都先于所有
 * draw），在 {@link #draw()} 的第一行关掉 —— 于是世界被折射，而滤镜自身与其后的所有 UI 都保持原样。</p>
 *
 * <h3>状态从哪来</h3>
 * 不挂任何钩子，{@link #update()} 每帧<b>拉取</b>当前手持武器算目标温度（{@link #targetHeat()}）——
 * 装备、解封、主副对调、读档、换层重建场景全都自动正确，不需要通知。
 * 设置里关掉「莱瓦汀场景特效」后 {@link #targetHeat()} 直接返回 0，三套表现会一起平滑淡出。
 */
public class HeatVignette extends Image {

	/** 渐变贴图的缓存键：全局只有一张（白色），颜色靠染色。 */
	private static final String TEXTURE_KEY = "egopd_heat_vignette";

	/** 渐变贴图分辨率：够平滑，且只是一次性开销。 */
	private static final int TEXTURE_SIZE = 64;

	/** 归一化半径小于它时完全透明（中心留白圈）。 */
	private static final float INNER_RADIUS = 0.26f;

	/** 中心仍保留的一丁点色度——「接近透明」而不是一个硬邦邦的空洞。 */
	private static final float CENTER_FLOOR = 0.04f;

	/**
	 * 热度色板，下标＝形态序号：0 封印（不显示）、1 黄、2 橙、3 红。
	 * <p>0 号刻意与 1 号同色：这样「无滤镜 ↔ 一阶段」的过渡只动 alpha，
	 * 不会在淡入淡出途中经过一段发暗的中间色。</p>
	 */
	private static final int[] HEAT_COLORS = { 0xFFD24A, 0xFFD24A, 0xFF8A00, 0xFF2A00 };

	/** 火焰粒子用色：比色罩更亮更饱和，加色混合下才有「火」的亮芯。 */
	private static final int[] FLAME_COLORS = { 0xFFE070, 0xFFE070, 0xFFA828, 0xFF7A20 };

	/** 各热度下<b>屏幕边缘</b>的不透明度（半透明滤镜，中心会被 {@link #CENTER_FLOOR} 兜底）。 */
	private static final float[] HEAT_ALPHA = { 0f, 0.20f, 0.28f, 0.36f };

	/** 各热度下热浪扭曲的<b>最大像素偏移</b>（屏幕边缘处；0＝不扭曲）。 */
	private static final float[] HEAT_WARP = { 0f, 0.9f, 1.4f, 2.0f };

	/** 扭曲强度的「近中心衰减」：0＝全屏一致，1＝只在屏幕边缘明显。 */
	private static final float WARP_EDGE_BIAS = 0.7f;

	/** 热浪抖动的循环周期（秒）。相位按 2π 整数倍回绕，所以时间系数只能取整数（见 shader）。 */
	private static final float WAVE_PERIOD = 1.2f;

	private static final float TWO_PI = (float) (Math.PI * 2);

	/** 冷热切换的平滑时长（秒）。 */
	private static final float FADE_TIME = 0.35f;

	/** 低于这个温度就彻底不画（也意味着不扭曲、不冒火）。 */
	private static final float MIN_HEAT = 0.002f;

	/** 当前显示的热度 0~3，每帧向 {@link #targetHeat()} 平滑靠拢（所以换形态是渐变而不是硬切）。 */
	private float heat;

	/** 当前场景里那一个实例；火焰粒子等外部表现通过它共读同一份平滑热度。 */
	private static HeatVignette scene;

	/** 热浪相位（0~2π 循环，整数时间系数保证回绕处连续）。 */
	private static float waveTime = 0f;

	public HeatVignette() {
		super( makeTexture() );

		scene = this;

		//必须用 uiCamera：跟随 Camera.main 的话滤镜会跟着英雄滚动，而我们要的是「屏幕」边缘
		camera = PixelScene.uiCamera;

		x = 0;
		y = 0;
		fitToScreen();

		//初始状态直接对齐，避免每次换层重建场景都闪一次淡入
		//（热度为 0 时 applyHeat() 会自己把 visible 关掉）
		heat = targetHeat();
		applyHeat();
	}

	@Override
	public void update() {
		super.update();

		//兜底：窗口尺寸变了但场景没重建时，重新贴合屏幕
		fitToScreen();

		float target = targetHeat();
		if (heat != target) {
			//指数逼近，快慢与差值成正比，收尾不会拖泥带水
			float dt = Game.elapsed > 0 ? Game.elapsed : 1 / 60f;
			float step = Math.min(1f, dt / FADE_TIME);
			heat += (target - heat) * step;
			if (Math.abs(target - heat) < MIN_HEAT) heat = target;
			applyHeat();
		}
	}

	@Override
	public void draw() {
		//世界层到此为止：从这里开始的每一笔（滤镜自己 + 随后所有 UI）都不再被热浪扭曲
		NoosaScriptWarp.enabled = false;
		super.draw();
	}

	@Override
	public void destroy() {
		//场景销毁时兜底关掉，免得残留状态把别的场景也扭了
		NoosaScriptWarp.enabled = false;
		if (scene == this) scene = null;
		super.destroy();
	}

	//==========================================================================
	// 对外：供火焰粒子等共用同一份热度
	//==========================================================================

	/** 当前平滑热度：0＝无特效，1/2/3＝黄/橙/红。受设置开关约束。 */
	public static float heat() {
		return scene != null ? scene.heat : 0f;
	}

	/** 当前热度对应的火焰粒子颜色（比色罩更亮）。 */
	public static int flameColor() {
		return sample( FLAME_COLORS, heat() );
	}

	/**
	 * 目标热度：0＝无特效，1/2/3＝黄/橙/红。
	 * <p>只看<b>手上</b>（主手或副手）的封印之剑系列武器——背在包里不算装备。</p>
	 * <p>设置里关掉「莱瓦汀场景特效」时恒为 0，于是三套表现一起淡出。</p>
	 */
	public static float targetHeat() {
		if (!SPDSettings.laevateinnFX()) return 0f;

		Hero hero = Dungeon.hero;
		if (hero == null || hero.belongings == null) return 0f;

		SealedSwordBase sword = SealedSwordBase.swordInHands(hero);
		//stage(): 0 封印之剑（无滤镜）～ 3 莱瓦汀
		return sword == null ? 0f : sword.stage();
	}

	//==========================================================================
	// 上色
	//==========================================================================

	/** 把当前的 {@link #heat} 换算成颜色与不透明度，套到本 Image 上。 */
	private void applyHeat() {
		float a = alphaAt(heat);
		if (a <= 0.001f) {
			visible = false;
			return;
		}
		hardlight(colorAt(heat));
		alpha(a);
		visible = true;
	}

	/**
	 * 世界层的起点：把当前热度推给热浪扭曲脚本（由 {@code GameScene.draw()} 的第一句调用）。
	 *
	 * <p>为什么放在 draw 而不是 {@code update()}：{@code Game.render()} 的顺序是
	 * {@code resetCamera() → draw() → step()(update)}。若在 update 里开关，那么下一帧
	 * {@code Game.render()} 开头那两句 {@code NoosaScript.get().resetCamera()} /
	 * {@code NoosaScriptNoLighting.get().resetCamera()} 也会被换成扭曲脚本，普通脚本的相机缓存
	 * 就再也清不掉（UI 层会用上过期矩阵）。在 draw 的第一句开、在 {@link #draw()} 的第一行关，
	 * 正好把整个世界层夹在中间，而 {@code Game.render()} 的相机重置又总能落回普通脚本上。</p>
	 */
	public static void beginWorldPass() {
		//相位一直走，开关不影响它的连续性
		waveTime = (waveTime + Game.elapsed * (TWO_PI / WAVE_PERIOD)) % TWO_PI;

		float amp = scene == null ? 0f : warpAt(scene.heat);
		if (amp <= 0.01f) {
			NoosaScriptWarp.enabled = false;
			return;
		}
		NoosaScriptWarp.enabled = true;
		NoosaScriptWarp.upload( waveTime, amp, WARP_EDGE_BIAS );
	}

	/** 在色板上按热度线性插值。 */
	private static int colorAt(float heat) {
		return sample( HEAT_COLORS, heat );
	}

	/** 在强度表上按热度线性插值。 */
	private static float alphaAt(float heat) {
		return sample( HEAT_ALPHA, heat );
	}

	/** 在扭曲强度表上按热度线性插值。 */
	private static float warpAt(float heat) {
		return sample( HEAT_WARP, heat );
	}

	private static int sample(int[] palette, float heat) {
		float h = Math.max(0f, Math.min(palette.length - 1f, heat));
		int i = Math.min((int) h, palette.length - 2);
		return lerpColor(palette[i], palette[i + 1], h - i);
	}

	private static float sample(float[] table, float heat) {
		float h = Math.max(0f, Math.min(table.length - 1f, heat));
		int i = Math.min((int) h, table.length - 2);
		return table[i] + (table[i + 1] - table[i]) * (h - i);
	}

	private static int lerpColor(int from, int to, float f) {
		int r = Math.round(((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
		int g = Math.round(((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
		int b = Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
		return (r << 16) | (g << 8) | b;
	}

	/** 让 64×64 的 quad 拉伸铺满整个 uiCamera 视野。 */
	private void fitToScreen() {
		if (PixelScene.uiCamera == null) return;
		float sx = PixelScene.uiCamera.width / (float) TEXTURE_SIZE;
		float sy = PixelScene.uiCamera.height / (float) TEXTURE_SIZE;
		if (scale.x != sx || scale.y != sy) {
			scale.set(sx, sy);
			x = 0;
			y = 0;
		}
	}

	//==========================================================================
	// 渐变贴图
	//==========================================================================

	/**
	 * 取（必要时生成）那张白色径向渐变贴图。
	 *
	 * <p>像素写进 {@code SmartTexture.bitmap} 后<b>不需要手动上传</b>：{@code TextureCache.create}
	 * 只是建好对象（{@code id == -1}，它内部的 {@code filter/wrap} 见到未生成会跳过 GL 调用），
	 * 真正的 {@code generate() → bitmap(pixmap)} 发生在第一次 {@code bind()}（也就是第一次绘制），
	 * 那时像素已经写好了。断上下文后 {@code TextureCache.reload()} 也会拿同一个 Pixmap 重新上传。</p>
	 */
	private static SmartTexture makeTexture() {
		if (!TextureCache.contains(TEXTURE_KEY)) {
			SmartTexture tx = TextureCache.create(TEXTURE_KEY, TEXTURE_SIZE, TEXTURE_SIZE);
			Pixmap pix = tx.bitmap;

			for (int y = 0; y < TEXTURE_SIZE; y++) {
				for (int x = 0; x < TEXTURE_SIZE; x++) {
					//归一化到 [-1,1]，(0,0) 在贴图正中
					float dx = (x + 0.5f) / TEXTURE_SIZE * 2f - 1f;
					float dy = (y + 0.5f) / TEXTURE_SIZE * 2f - 1f;

					//椭圆半径：=1 正好落在四条边的中点，四角会被 clamp 到最浓
					float r = Math.min(1f, (float) Math.sqrt(dx * dx + dy * dy));

					//中心 INNER_RADIUS 之内全透明，之后 smoothstep 平滑升到 1
					float t = Math.max(0f, Math.min(1f, (r - INNER_RADIUS) / (1f - INNER_RADIUS)));
					t = t * t * (3f - 2f * t);

					float a = CENTER_FLOOR + (1f - CENTER_FLOOR) * t;
					int alpha = Math.round(a * 255f);

					//libGDX 的 Pixmap 是 RGBA8888（R 在高位）；这里要非预乘：RGB 恒为白，只让 A 走渐变
					pix.drawPixel(x, y, 0xFFFFFF00 | alpha);
				}
			}
		}
		return TextureCache.get(TEXTURE_KEY);
	}
}
