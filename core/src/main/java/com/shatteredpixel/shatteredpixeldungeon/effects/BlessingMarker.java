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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.PointF;

/**
 * 「祝福」标记贴图：静态叠加在目标角色贴图下方（挂载到地板发射器层，先于角色贴图渲染）。
 * <p>与指令目标的标记（挂在特效层、半透明、中心对齐）不同：本标记
 * 挂在角色贴图之下、完全不透明，且相比指令目标实现向上偏移4像素。
 * 贴图位于 items.png 的 (32,560)-(47,575) 区域。</p>
 */
public class BlessingMarker extends ItemSprite {

	private final Char target;

	public BlessingMarker( Char target ){
		super( ItemSpriteSheet.BLESSING_MARK );
		this.target = target;
		place();
	}

	@Override
	public void update() {
		super.update();
		if (target == null || target.sprite == null || !target.sprite.visible){
			visible = false;
			return;
		}
		visible = true;
		place();
	}

	//位置同步：对齐目标贴图中心，相比指令目标实现向上偏移4像素
	private void place(){
		PointF c = target.sprite.center();
		x = PixelScene.align( c.x - width()/2f );
		y = PixelScene.align( c.y - height()/2f - 4f );
	}

	//挂载到地板发射器层（角色贴图之下）
	public static BlessingMarker attach( Char target ){
		BlessingMarker marker = new BlessingMarker( target );
		GameScene.floorEmittersAdd( marker );
		return marker;
	}

	public void remove(){
		killAndErase();
	}
}
