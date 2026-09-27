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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 人体观剧：环指大师 T3 天赋「人体观剧」施加的状态。
 * <p>参考 {@link Frost} 实现：目标被固定为展品，无法移动和攻击
 * （通过 {@code paralysed++} 实现完全定身，与冻结一致）。
 * 该 buff 为负面类型，由命中触发，带冷却时间管理。</p>
 */
public class BodyTheater extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	public float maxDuration = 0f;

	private static final String MAX_DURATION = "max_duration";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(MAX_DURATION, maxDuration);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		maxDuration = bundle.getFloat(MAX_DURATION);
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			target.paralysed++;
			return true;
		} else {
			return false;
		}
	}

	@Override
	public void detach() {
		super.detach();
		if (target.paralysed > 0)
			target.paralysed--;
	}

	@Override
	public int icon() {
		return BuffIndicator.BODY_THEATER;
	}

	@Override
	public void tintIcon(Image icon) {
		//略偏紫红的剧场色调
		icon.hardlight(0.85f, 0.4f, 0.6f);
	}

	@Override
	public float iconFadePercent() {
		if (maxDuration <= 0) return 1;
		return Math.max(0, 1f - visualcooldown() / maxDuration);
	}

	@Override
	public void fx(boolean on) {
		if (on) {
			//参考 Frost：使用 PARALYSED 状态视觉（无 FROZEN 冰晶特效，剧场不冻冰）
			target.sprite.add(CharSprite.State.PARALYSED);
		} else {
			target.sprite.remove(CharSprite.State.PARALYSED);
		}
	}
}
