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

package com.shatteredpixel.shatteredpixeldungeon.ui;

import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;

public class TargetHealthIndicator extends HealthBar {
	
	public static TargetHealthIndicator instance;
	
	private Char target;
	
	public TargetHealthIndicator() {
		super();
		
		instance = this;
	}
	
	@Override
	public void update() {
		super.update();
		
		if (target != null && target.isAlive() && target.isActive()
				&& target.sprite != null && target.sprite.visible) {
			CharSprite sprite = target.sprite;
			width = sprite.width();
			x = sprite.x;
			y = sprite.y - 3;
			level( target );
			//考验 NETZACH（胜利）：这条「瞄准目标」血条同样浮在怪物头顶，同样要跟着本体淡出。
			//⚠️ 这里**只压 alpha、不动 `visible`** —— `visible` 不是纯显示开关：
			//   `ChaoticCenser`（混沌香炉）拿 `instance.isVisible()` 当「英雄当前是否锁定了目标」在读，
			//   掐掉它会让香炉在 NETZACH 下静默失效（玩法被视觉改动带偏，违反「NETZACH 纯视觉」）。
			//   alpha 压到 0 已经是「完全透明」，玩家一样看不到，行为却与改动前逐位相同。
			float fade = Trials.enemyFade( target );
			setAlpha( fade );
			visible = true;
		} else {
			visible = false;
		}
	}
	
	public void target( Char ch ) {
		if (ch != null && ch.isAlive() && ch.isActive()) {
			target = ch;
		} else {
			target = null;
		}
	}
	
	public Char target() {
		return target;
	}
}
