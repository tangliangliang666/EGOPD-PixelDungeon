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
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.PointF;

/**
 * 指令目标的标记贴图：静态叠加在目标角色头顶，跟随目标移动。
 * <p>无动态特效（非粒子/光效），仅持续显示一张标记贴图；目标不可见时隐藏。
 * 挂载到场景特效层（{@link com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene#effect}），
 * 由 {@link com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTarget} 管理生命周期。</p>
 */
public class TargetMarker extends ItemSprite {

	private final Char target;

	public TargetMarker( Char target, int icon ){
		super( icon );
		this.target = target;
		alpha( 0.5f ); //半透明，避免完全遮盖怪物贴图
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

	//位置同步：标记居中重叠在目标怪物贴图上（中心对齐 sprite 中心）
	private void place(){
		PointF c = target.sprite.center();
		x = PixelScene.align( c.x - width()/2f );
		y = PixelScene.align( c.y - height()/2f );
	}

}
