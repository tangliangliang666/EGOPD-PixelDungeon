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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.JellyWobble;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.items.Heap;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DiamondSword;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.tiles.DungeonTilemap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Matrix;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.MovieClip;
import com.watabou.noosa.NoosaScript;
import com.watabou.noosa.NoosaScriptGlint;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class ItemSprite extends MovieClip {

	public static final int SIZE	= 16;
	
	private static final float DROP_INTERVAL = 0.4f;
	
	public Heap heap;
	
	private Glowing glowing;
	//FIXME: a lot of this emitter functionality isn't very well implemented.
	//right now I want to ship 0.3.0, but should refactor in the future.
	protected Emitter emitter;
	private float phase;
	private boolean glowUp;
	
	private float dropInterval;

	//the amount the sprite is raised from flat when viewed in a raised perspective
	protected float perspectiveRaise    = 5 / 16f; //5 pixels

	//the width and height of the shadow are a percentage of sprite size
	//offset is the number of pixels the shadow is moved down or up (handy for some animations)
	protected boolean renderShadow  = false;
	protected float shadowWidth     = 1f;
	protected float shadowHeight    = 0.25f;
	protected float shadowOffset    = 0.5f;

	//============ 钻石剑专属附魔流光（MC enchanted item glint） ============
	//最近一次 view(Item) 传入的物品引用；用于判断当前图标是否需要显示流光
	private Item viewItem;

	//附魔贴图规格：256×256 噪声，由 Assets.Sprites.ENCHANTED_GLINT 加载
	private static final int   GLINT_TEX_SIZE  = 256;
	//两方向滚动速度（贴图像素/秒）；16 px/s ≈ 一个完整周期 16 秒
	private static final float GLINT_SCROLL_X  = 16f;
	private static final float GLINT_SCROLL_Y  = 16f;
	//半透明程度：alpha 越小越透明（0.45 = 45% 不透明）
	private static final float GLINT_ALPHA     = 0.45f;

	private float glintOffX, glintOffY;

	//流光不在 ItemSprite 挂 child（noosa.Visual.draw 不会遍历子节点，且游离 Image 会解析到错误相机），
	//而是作为 ItemSprite 自有的第二张 quad：在 draw() 里复用图标自身的 matrix/camera 仅换绑贴图绘制。
	//为贴合图标形状，流光用双纹理 shader（NoosaScriptGlint）：主贴图=附魔噪声(滚动)，遮罩=图标贴图当前帧，
	//mask UV 直接拷贝图标 quad 的 UV —— 图标透明像素处的流光被 alpha 滤除，与图标逐像素对齐。
	private boolean glintOn;                    //当前是否应绘制流光
	private SmartTexture glintTex;              //流光贴图引用（TextureCache 缓存，勿 delete 贴图本身）
	private float[] glintVertices;              //quad 顶点：6 float/顶点（x,y + 滚动UV + 遮罩UV），共 24
	private FloatBuffer glintVB;                //CPU 顶点缓冲
	private Vertexbuffer glintBuffer;           //GPU 顶点缓冲
	private boolean glintDirty;                 //顶点已改、待上传
	
	public ItemSprite() {
		this( ItemSpriteSheet.SOMETHING, null );
	}
	
	public ItemSprite( Heap heap ){
		super(Assets.Sprites.ITEMS);
		view( heap );
	}
	
	public ItemSprite( Item item ) {
		super(Assets.Sprites.ITEMS);
		view( item );
	}
	
	public ItemSprite( int image ){
		this( image, null );
	}
	
	public ItemSprite( int image, Glowing glowing ) {
		super( Assets.Sprites.ITEMS );
		
		view(image, glowing);
	}
	
	public void link() {
		link(heap);
	}
	
	public void link( Heap heap ) {
		this.heap = heap;
		view(heap);
		renderShadow = true;
		visible = heap.seen;
		place(heap.pos);
	}
	
	@Override
	public void revive() {
		super.revive();

		speed.set( 0 );
		acc.set( 0 );
		dropInterval = 0;

		heap = null;
		viewItem = null;
		glintOffX = glintOffY = 0f;
		glintOn = false;
		//注意：不在这里释放流光 GPU 缓冲——revive() 可能发生在 Actor 线程（如 GameScene 回收 heap 精灵），
		//该线程没有 GL context，delete 缓冲会令 JVM 直接 abort（参见 Heap.destroy→sprite.kill 崩溃栈）。
		//glintTex/glintVertices/glintVB/glintBuffer 随精灵实例保留复用，仅在 destroy()（GL 线程）统一释放。
		if (emitter != null) {
			emitter.killAndErase();
			emitter = null;
		}
	}

	@Override
	public void destroy() {
		super.destroy();
		releaseGlint();
	}

	@Override
	public void copy(Image other) {
		super.copy(other);

		if (other instanceof ItemSprite && ((ItemSprite) other).glowing != null){
			glow(((ItemSprite) other).glowing);
		}

	}

	public void visible(boolean value){
		this.visible = value;
		if (emitter != null && !visible){
			emitter.killAndErase();
			emitter = null;
		}
	}
	
	public PointF worldToCamera( int cell ) {
		final int csize = DungeonTilemap.SIZE;
		
		return new PointF(
				PixelScene.align(Camera.main, ((cell % Dungeon.level.width()) + 0.5f) * csize - width() * 0.5f),
				PixelScene.align(Camera.main, ((cell / Dungeon.level.width()) + 1.0f) * csize - height() - csize * perspectiveRaise)
		);
	}
	
	public void place( int p ) {
		if (Dungeon.level != null) {
			point(worldToCamera(p));
			shadowOffset = 0.5f;
		}
	}
	
	public void drop() {

		if (heap.isEmpty()) {
			return;
		} else if (heap.size() == 1){
			// normally this would happen for any heap, however this is not applied to heaps greater than 1 in size
			// in order to preserve an amusing visual bug/feature that used to trigger for heaps with size > 1
			// where as long as the player continually taps, the heap sails up into the air.
			place(heap.pos);
		}
			
		dropInterval = DROP_INTERVAL;
		
		speed.set( 0, -100 );
		acc.set(0, -speed.y / DROP_INTERVAL * 2);
		
		if (heap != null && heap.seen && heap.peek() instanceof Gold) {
			CellEmitter.center( heap.pos ).burst( Speck.factory( Speck.COIN ), 5 );
			Sample.INSTANCE.play( Assets.Sounds.GOLD, 1, 1, Random.Float( 0.9f, 1.1f ) );
		}
	}
	
	public void drop( int from ) {

		if (heap.pos == from) {
			drop();
		} else {
			
			float px = x;
			float py = y;
			drop();
			
			place(from);
	
			speed.offset((px - x) / DROP_INTERVAL, (py - y) / DROP_INTERVAL);
		}
	}

	public ItemSprite view( Item item ){
		viewItem = item;
		view(item.image(), item.glowing());
		Emitter emitter = item.emitter();
		if (emitter != null && parent != null) {
			emitter.pos( this );
			parent.add( emitter );
			this.emitter = emitter;
		}
		refreshGlint();
		return this;
	}

	public ItemSprite view( Heap heap ){
		viewItem = null;
		if (heap.size() <= 0 || heap.items == null){
			return view( 0, null );
		}

		if (Trials.hidesGroundHeap( heap )){
			//YESOD（根基）：地面上的堆一律显示为未知贴图 —— 散落物、商店货架与**六类容器**
			//（宝箱 / 上锁宝箱 / 水晶宝箱 / 坟墓 / 骷髅堆 / 英雄遗骸）都算在内。
			//viewItem 已在上面置 null，所以附魔流光不会被误触发。
			view( ItemSpriteSheet.UNKNOWN_ITEM, null );
		} else {
			switch (heap.type) {
				case HEAP: case FOR_SALE:
					view( heap.peek() ); //内部 view(Item) 会刷新 viewItem
					break;
				case CHEST:
					view( ItemSpriteSheet.CHEST, null ); break;
				case LOCKED_CHEST:
					view( ItemSpriteSheet.LOCKED_CHEST, null ); break;
				case CRYSTAL_CHEST:
					view( ItemSpriteSheet.CRYSTAL_CHEST, null ); break;
				case TOMB:
					view( ItemSpriteSheet.TOMB, null ); break;
				case SKELETON:
					view( ItemSpriteSheet.BONES, null ); break;
				case REMAINS:
					view( ItemSpriteSheet.REMAINS, null ); break;
				default:
					view( 0, null ); break;
			}
		}

		alpha( heap.hidden ? 0.15f : 1f);
		refreshGlint();
		return this;
	}

	public ItemSprite view( int image, Glowing glowing ) {
		if (this.emitter != null) this.emitter.killAndErase();
		emitter = null;
		frame( image );
		glow( glowing );
		refreshGlint();
		return this;
	}

	/** 当前图标是否应当显示 MC 附魔流光 */
	private boolean wantGlint(){
		return viewItem instanceof DiamondSword && ((DiamondSword) viewItem).hasEnchants();
	}

	/** 依据 viewItem 决定流光开关；首次需要时懒创建 quad 缓冲 */
	private void refreshGlint(){
		glintOn = wantGlint();
		if (glintOn){
			if (glintTex == null){
				glintTex = TextureCache.get(Assets.Sprites.ENCHANTED_GLINT);
				glintVertices = new float[24];
				//注意：不能复用 Quad.create()（固定 16 float = 4-float/顶点），流光顶点是 6 float/顶点共 24 float
				glintVB = ByteBuffer.
						allocateDirect( glintVertices.length * Float.SIZE / 8 ).
						order( ByteOrder.nativeOrder() ).
						asFloatBuffer();
				glintBuffer = null;
				glintOffX = glintOffY = 0f;
			}
			//缓冲可能来自本精灵上一次生命周期（kill/revive 后保留复用）：立即按当前图标帧重建顶点，
			//保证 view() 后首个 draw 就使用正确的遮罩 UV（update() 每帧也会重建，这里是双保险）
			updateGlintFrame();
		}
	}

	/** 释放流光 GL/CPU 缓冲。仅应在 GL 线程调用——目前唯一调用点是 destroy()（场景整组销毁，GL context 有效）；
	 *  kill()/revive() 不得调用（它们可能发生在 Actor 线程，无 GL context）。贴图由 TextureCache 托管，不 delete。 */
	private void releaseGlint(){
		glintOn = false;
		if (glintBuffer != null){
			glintBuffer.delete();
			glintBuffer = null;
		}
		glintTex = null;
		glintVertices = null;
		glintVB = null;
		glintDirty = false;
	}

	/** 按当前偏移计算流光 quad 顶点（6 float/顶点：位置=图标区域、UV=贴图内滑动窗口、maskUV=图标帧 UV） */
	private void updateGlintFrame(){
		if (glintTex == null || glintVertices == null) return;
		int texSize = glintTex.width;
		if (texSize <= 0) return;

		float u0 = glintOffX / texSize;
		float v0 = glintOffY / texSize;
		float u1 = (glintOffX + this.width) / texSize;
		float v1 = (glintOffY + this.height) / texSize;

		//图标 quad 顶点（4 float/顶点：x, y, u, v）；其 uv 即当前帧在贴图上的区域（已含 flip 语义）
		float[] iconV = this.vertices;
		for (int i = 0; i < 4; i++){
			int out = i * 6;
			int in  = i * 4;
			//位置：与图标一致的本地矩形
			glintVertices[out]     = iconV[in];
			glintVertices[out + 1] = iconV[in + 1];
			//遮罩 UV：直接拷贝图标 quad 的 UV，保证遮罩采样与图标渲染逐像素对齐
			glintVertices[out + 4] = iconV[in + 2];
			glintVertices[out + 5] = iconV[in + 3];
		}

		//流光贴图滚动 UV（滑动窗口，随偏移环绕）
		glintVertices[2]  = u0; glintVertices[3]  = v0;
		glintVertices[8]  = u1; glintVertices[9]  = v0;
		glintVertices[14] = u1; glintVertices[15] = v1;
		glintVertices[20] = u0; glintVertices[21] = v1;

		glintDirty = true;
	}

	public void frame( int image ){
		frame( ItemSpriteSheet.film.get( image ));

		float height = ItemSpriteSheet.film.height( image );
		//adds extra raise to very short items, so they are visible
		if (height < 8f){
			perspectiveRaise =  (5 + 8 - height) / 16f;
		}
	}
	
	public synchronized void glow( Glowing glowing ){
		this.glowing = glowing;
		if (glowing == null) resetColor();
	}

	@Override
	public void kill() {
		super.kill();
		if (emitter != null) {
			emitter.on = false;
			emitter.autoKill = true;
		}
		emitter = null;
		//注意：kill() 会在 Actor 线程被 Heap.destroy 触发（拾取/清空地面堆），该线程无 GL context，
		//绝不能在这里 delete GPU 缓冲（见崩溃栈 Vertexbuffer.delete ← ItemSprite.kill ← Heap.destroy）。
		//流光仅关绘制标志，缓冲随精灵实例保留，待 destroy()（GL 线程）统一释放。
		glintOn = false;
	}

	private float[] shadowMatrix = new float[16];

	@Override
	protected void updateMatrix() {
		super.updateMatrix();
		Matrix.copy(matrix, shadowMatrix);
		Matrix.translate(shadowMatrix,
				(width() * (1f - shadowWidth)) / 2f,
				(height() * (1f - shadowHeight)) + shadowOffset);
		Matrix.scale(shadowMatrix, shadowWidth, shadowHeight);
	}

	@Override
	public void draw() {
		if (texture == null || (!dirty && buffer == null))
			return;

		if (renderShadow) {
			if (dirty) {
				((Buffer)verticesBuffer).position(0);
				verticesBuffer.put(vertices);
				if (buffer == null)
					buffer = new Vertexbuffer(verticesBuffer);
				else
					buffer.updateVertices(verticesBuffer);
				dirty = false;
			}

			NoosaScript script = script();

			texture.bind();

			script.camera(camera());

			updateMatrix();

			script.uModel.valueM4(shadowMatrix);
			script.lighting(
					0, 0, 0, am * .6f,
					0, 0, 0, aa * .6f);

			script.drawQuad(buffer);
		}

		super.draw();

		//钻石剑专属附魔流光：与图标共用同一 matrix/camera，仅换绑 glint 贴图再画一个 quad。
		//这样在任意容器（地面 Camera.main / 背包窗口独立 Camera）下都与图标严格重合。
		if (glintOn && glintTex != null && glintVB != null){
			if (glintDirty){
				((Buffer)glintVB).position(0);
				glintVB.put(glintVertices);
				if (glintBuffer == null)
					glintBuffer = new Vertexbuffer(glintVB);
				else
					glintBuffer.updateVertices(glintVB);
				glintDirty = false;
			}

			if (glintBuffer == null) return;

			//双纹理流光：单元0=附魔噪声，单元1=图标贴图当前帧（其 alpha 作遮罩，裁掉图标透明区域）
			NoosaScriptGlint glintScript = NoosaScriptGlint.get();
			if (texture != null) glintScript.bindMask(texture);
			glintTex.bind();
			glintScript.camera(camera());
			glintScript.uModel.valueM4(matrix);
			glintScript.lighting(1f, 1f, 1f, GLINT_ALPHA, 0f, 0f, 0f, 0f);
			glintScript.drawMaskedQuad(glintBuffer);
		}

	}

	@Override
	public synchronized void update() {
		super.update();

		visible = (heap == null || heap.seen);

		if (emitter != null){
			emitter.visible = visible;
		}

		//钻石剑专属附魔流光：随时间推进两个方向的偏移并刷新 quad UV
		if (glintOn && glintTex != null){
			glintOffX = (glintOffX + GLINT_SCROLL_X * Game.elapsed) % GLINT_TEX_SIZE;
			glintOffY = (glintOffY + GLINT_SCROLL_Y * Game.elapsed) % GLINT_TEX_SIZE;
			if (glintOffX < 0) glintOffX += GLINT_TEX_SIZE;
			if (glintOffY < 0) glintOffY += GLINT_TEX_SIZE;
			updateGlintFrame();
		}

		if (dropInterval > 0){
			shadowOffset -= speed.y * Game.elapsed * 0.8f;

			if ((dropInterval -= Game.elapsed) <= 0){

				speed.set(0);
				acc.set(0);
				shadowOffset = 0.25f;
				place(heap.pos);

				//「依旧果冻人」挑战（2026-09-15）：物品落地、贴图归位后摆一下（纯视觉）
				JellyWobble.land( this );

				if (visible) {

					if (Dungeon.level.water[heap.pos]) {
						GameScene.ripple(heap.pos);
					}

					if (Dungeon.level.water[heap.pos]) {
						Sample.INSTANCE.play( Assets.Sounds.WATER, 0.8f, Random.Float( 1f, 1.45f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.EMPTY_SP) {
						Sample.INSTANCE.play( Assets.Sounds.STURDY, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.GRASS
							|| Dungeon.level.map[heap.pos] == Terrain.EMBERS
							|| Dungeon.level.map[heap.pos] == Terrain.FURROWED_GRASS){
						Sample.INSTANCE.play( Assets.Sounds.GRASS, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else if (Dungeon.level.map[heap.pos] == Terrain.HIGH_GRASS) {
						Sample.INSTANCE.play( Assets.Sounds.STEP, 0.8f, Random.Float( 1.16f, 1.25f ) );
					} else {
						Sample.INSTANCE.play( Assets.Sounds.STEP, 0.8f, Random.Float( 1.16f, 1.25f ));
					}
				}
			}
		}

		if (visible && glowing != null) {
			if (glowUp && (phase += Game.elapsed) > glowing.period) {
				
				glowUp = false;
				phase = glowing.period;
				
			} else if (!glowUp && (phase -= Game.elapsed) < 0) {
				
				glowUp = true;
				phase = 0;
				
			}
			
			float value = phase / glowing.period * 0.6f;
			
			rm = gm = bm = 1 - value;
			ra = glowing.red * value;
			ga = glowing.green * value;
			ba = glowing.blue * value;
		}
	}

	public static int pick( int index, int x, int y ) {
		SmartTexture tx = TextureCache.get( Assets.Sprites.ITEMS );
		int rows = tx.width / SIZE;
		int row = index / rows;
		int col = index % rows;
		return tx.getPixel( col * SIZE + x, row * SIZE + y );
	}
	
	public static class Glowing {
		
		public int color;
		public float red;
		public float green;
		public float blue;
		public float period;
		
		public Glowing( int color ) {
			this( color, 1f );
		}
		
		public Glowing( int color, float period ) {

			this.color = color;

			red = (color >> 16) / 255f;
			green = ((color >> 8) & 0xFF) / 255f;
			blue = (color & 0xFF) / 255f;
			
			this.period = period;
		}
	}
}
