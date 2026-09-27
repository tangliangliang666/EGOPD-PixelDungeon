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

import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Quad;
import com.watabou.glwrap.Vertexbuffer;
import com.watabou.utils.RectF;

import java.nio.Buffer;
import java.nio.FloatBuffer;

public class Image extends Visual {

	public SmartTexture texture;
	protected RectF frame;
	
	public boolean flipHorizontal;
	public boolean flipVertical;
	
	protected float[] vertices;
	protected FloatBuffer verticesBuffer;
	protected Vertexbuffer buffer;
	
	protected boolean dirty;
	
	public Image() {
		super( 0, 0, 0, 0 );
		
		vertices = new float[16];
		verticesBuffer = Quad.create();
	}
	
	public Image( Image src ) {
		this();
		copy( src );
	}
	
	public Image( Object tx ) {
		this();
		texture( tx );
	}
	
	public Image( Object tx, int left, int top, int width, int height ) {
		this( tx );
		frame( texture.uvRect( left,  top,  left + width, top + height ) );
	}
	
	public void texture( Object tx ) {
		texture = tx instanceof SmartTexture ? (SmartTexture)tx : TextureCache.get( tx );
		frame( new RectF( 0, 0, 1, 1 ) );
	}
	
	public void frame( RectF frame ) {
		this.frame = frame;
		
		width = frame.width() * texture.width;
		height = frame.height() * texture.height;
		
		updateFrame();
		updateVertices();
	}
	
	public void frame( int left, int top, int width, int height ) {
		frame( texture.uvRect( left, top, left + width, top + height ) );
	}
	
	public RectF frame() {
		return new RectF( frame );
	}

	public void copy( Image other ) {
		texture = other.texture;
		frame = new RectF( other.frame );
		
		width = other.width;
		height = other.height;

		scale = other.scale;
		
		updateFrame();
		updateVertices();

		rm = other.rm; gm = other.gm; bm = other.bm; am = other.am;
		ra = other.ra; ga = other.ga; ba = other.ba; aa = other.aa;
	}
	
	protected void updateFrame() {
		
		if (flipHorizontal) {
			vertices[2]		= frame.right;
			vertices[6]		= frame.left;
			vertices[10]	= frame.left;
			vertices[14]	= frame.right;
		} else {
			vertices[2]		= frame.left;
			vertices[6]		= frame.right;
			vertices[10]	= frame.right;
			vertices[14]	= frame.left;
		}
		
		if (flipVertical) {
			vertices[3]		= frame.bottom;
			vertices[7]		= frame.bottom;
			vertices[11]	= frame.top;
			vertices[15]	= frame.top;
		} else {
			vertices[3]		= frame.top;
			vertices[7]		= frame.top;
			vertices[11]	= frame.bottom;
			vertices[15]	= frame.bottom;
		}
		
		dirty = true;
	}
	
	protected void updateVertices() {
		
		vertices[0] 	= 0;
		vertices[1] 	= 0;
		
		vertices[4] 	= width;
		vertices[5] 	= 0;
		
		vertices[8] 	= width;
		vertices[9] 	= height;
		
		vertices[12]	= 0;
		vertices[13]	= height;
		
		dirty = true;
	}

	//=================== 「果冻」摆动（2026-09-15 自定义挑战：依旧果冻人） ===================
	//受击/死亡形变：把整幅贴图按高度切成若干横条，越靠上的条横向位移越大、底边（贴图最下方＝
	//角色脚底）完全不动，位移随时间阻尼振荡回原位；同时整个上半身按同一阻尼节奏「压扁 ↔ 拉长」
	//（竖直形变），压扁时横向鼓出，近似保体积。分层绘制见 {@link JellyDraw}。
	//本类只做「状态 + 计时」，jellyAmp == 0 时下面这些代码一律不参与，绘制路径与改动前一致。

	/** 当前摆动幅度（像素；方向见 {@link #jellyDir}）。0 表示不摆动。 */
	public float jellyAmp;

	/** 竖直「压扁 / 拉长」的最大比例（0.3 ⇒ 最多压扁或拉长 30%）。0 表示只做横向形变。 */
	public float jellySquashAmp;

	/** 摆动已经过的时间（秒）。 */
	protected float jellyTime;
	/** 单次摆动的时长（秒）与角频率（弧度/秒）。 */
	protected float jellyDuration = 1f;
	protected float jellyFreq = 8f;
	/** 本次摆动的初始方向（+1 向右 / -1 向左）。 */
	protected float jellyDir = 1f;

	/**
	 * 触发一次「果冻」摆动。
	 * @param amp      贴图<b>顶部</b>的最大横向偏移（像素）
	 * @param squash   竖直压扁 / 拉长的最大比例（0.3 ⇒ ±30%）
	 * @param dir      初始方向（正数向右、负数向左）
	 * @param duration 总时长（秒）
	 * @param freq     角频率（弧度/秒）；横向来回次数 ≈ {@code duration × freq / (2π)}
	 */
	public void jellyWobble( float amp, float squash, float dir, float duration, float freq ) {
		jellyAmp = amp;
		jellySquashAmp = squash;
		jellyDir = dir >= 0 ? 1f : -1f;
		jellyDuration = Math.max( 0.01f, duration );
		jellyFreq = freq;
		jellyTime = 0f;
	}

	/** 本帧的横向摆动量（像素）：t=0 取最大偏移，按余弦振荡、按剩余时间线性衰减，末尾必然归零。 */
	public float jellyOffset() {
		float damping = 1f - jellyTime / jellyDuration;
		if (damping < 0f) damping = 0f;
		return jellyAmp * jellyDir * damping * (float)Math.cos( jellyFreq * jellyTime );
	}

	/**
	 * 本帧的竖直形变比例：正值<b>拉长</b>、负值<b>压扁</b>（以贴图底边为基准）。
	 * 与横向摆动差 1/4 周期（cos → −sin）⇒ 先被甩出去、随后才「一压一弹」地回缩，
	 * 而不是整幅图同步缩放（那样只是呼吸，不像果冻）。
	 */
	public float jellySquash() {
		float damping = 1f - jellyTime / jellyDuration;
		if (damping < 0f) damping = 0f;
		return -jellySquashAmp * damping * (float)Math.sin( jellyFreq * jellyTime );
	}

	@Override
	public void update() {
		super.update();

		if (jellyAmp != 0f && (jellyTime += Game.elapsed) >= jellyDuration) {
			//摆动结束：回到原位，此后走普通绘制路径
			jellyAmp = 0f;
			jellySquashAmp = 0f;
		}
	}

	@Override
	public void draw() {

		if (texture == null || (!dirty && buffer == null))
			return;
		
		super.draw();

		//「果冻」摆动：分层绘制。只有 jellyAmp != 0 时才走这条分支，
		//既不读也不写本类的 vertices / buffer（流光特效要拿 vertices 当遮罩 UV）
		if (jellyAmp != 0f) {
			JellyDraw.draw( this );
			return;
		}

		if (dirty) {
			((Buffer)verticesBuffer).position( 0 );
			verticesBuffer.put( vertices );
			if (buffer == null)
				buffer = new Vertexbuffer( verticesBuffer );
			else
				buffer.updateVertices( verticesBuffer );
			dirty = false;
		}

		NoosaScript script = script();
		
		texture.bind();
		
		script.camera( camera() );
		
		script.uModel.valueM4( matrix );
		script.lighting(
			rm, gm, bm, am,
			ra, ga, ba, aa );

		script.drawQuad( buffer );
		
	}

	protected NoosaScript script(){
		return NoosaScript.get();
	}

	@Override
	public void destroy() {
		super.destroy();
		if (buffer != null)
			buffer.delete();
	}
}
