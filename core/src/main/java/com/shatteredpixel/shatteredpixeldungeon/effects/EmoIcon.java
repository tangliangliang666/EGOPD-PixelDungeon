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

import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

public class EmoIcon extends Image {

	protected float maxSize = 2;
	protected float timeScale = 1;

	protected boolean growing	= true;
	
	protected CharSprite owner;
	
	public EmoIcon( CharSprite owner ) {
		super();
		
		this.owner = owner;
		GameScene.add( this );
	}
	
	@Override
	public void update() {
		super.update();

		if (visible) {
			if (growing) {
				scale.set( Math.min(scale.x + Game.elapsed * timeScale, maxSize ));
				if (scale.x >= maxSize) {
					growing = false;
				}
			} else {
				scale.set( Math.max(scale.x - Game.elapsed * timeScale, 1f ));
				if (scale.x <= 1) {
					growing = true;
				}
			}

			if (camera != null) {
				PointF center = centerPoint();
				x = PixelScene.align(camera, owner.x + owner.width() - center.x);
				y = PixelScene.align(camera, owner.y - center.y);
			}
		}
	}

	/**
	 * 考验 NETZACH（胜利）：睡眠 / 警觉 / 搜索 / 迷失这些**状态标记**是挂在宿主精灵旁边的
	 * 独立视觉元素（由 {@code GameScene.add} 直接挂在场景上，不是精灵的子节点），
	 * 因此精灵自己的 alpha 变化管不到它 ⇒ 这里自己查一次淡化系数。
	 *
	 * <p>做法与 {@code CharSprite.draw()} 完全一致：只在绘制这一帧把 alpha 乘上系数、
	 * 画完立刻还原，不落任何持久状态（既避免与别处写 alpha 的地方打架，
	 * 也让 {@code fade == 0} 时自然变成「画了但看不见」）。</p>
	 */
	@Override
	public void draw() {
		float fade = (owner != null) ? Trials.enemyFade( owner.ch ) : 1f;
		float amBak = am, aaBak = aa;
		if (fade < 1f) {
			am *= fade;
			aa *= fade;
		}

		super.draw();

		if (fade < 1f) {
			am = amBak;
			aa = aaBak;
		}
	}

	protected PointF centerPoint(){
		return new PointF(width()/2f, height()/2f);
	};
	
	public static class Sleep extends EmoIcon {
		
		public Sleep( CharSprite owner ) {
			
			super( owner );
			
			copy( Icons.get( Icons.SLEEP ) );
			
			maxSize = 1.2f;
			timeScale = 0.5f;
			
			scale.set( Random.Float( 1, maxSize ) );

			x = owner.x + owner.width - width / 2;
			y = owner.y - height;
		}

		@Override
		protected PointF centerPoint(){
			//centered and significantly up
			return new PointF(width()/2f, 4f+ height()/2f);
		}
	}
	
	public static class Alert extends EmoIcon {
		
		public Alert( CharSprite owner ) {
			
			super( owner );
			
			copy( Icons.get( Icons.ALERT ) );
			
			maxSize = 1.3f;
			timeScale = 2;
			
			scale.set( Random.Float( 1, maxSize ) );

			x = owner.x + owner.width - width / 2;
			y = owner.y - height;
		}

		@Override
		protected PointF centerPoint(){
			//up and left, and centers at the bottom-left
			return new PointF(2.5f + 0.25f*width(), 2.5f + 0.75f*height());
		}
	}

	public static class Investigate extends EmoIcon {

		public Investigate( CharSprite owner ) {

			super( owner );

			copy( Icons.get( Icons.INVESTIGATE ) );

			maxSize = 1.3f;
			timeScale = 1.5f;

			scale.set( Random.Float( 1, maxSize ) );

			x = owner.x + owner.width - width / 2;
			y = owner.y - height;
		}

		@Override
		protected PointF centerPoint(){
			//up and left, and centers at the bottom-left
			return new PointF(2.5f + 0.25f*width(), 2.5f + 0.75f*height());
		}
	}
	
	public static class Lost extends EmoIcon {
		
		public Lost( CharSprite owner ){
			super( owner );
			
			copy( Icons.get( Icons.LOST ) );
			
			maxSize = 1.25f;
			timeScale = 1;
			
			scale.set( Random.Float( 1, maxSize ) );
			
			x = owner.x + owner.width - width / 2;
			y = owner.y - height;
		}

		@Override
		protected PointF centerPoint(){
			//up and left, and centers at the bottom-left
			return new PointF(2.5f + 0.25f*width(), 2.5f + 0.75f*height());
		}
	}

}
