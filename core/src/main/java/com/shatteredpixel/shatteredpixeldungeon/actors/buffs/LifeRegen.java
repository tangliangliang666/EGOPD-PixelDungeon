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
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「生命恢复」buff：环指大师「液蕴血肉」天赋使用药剂时获得。
 * <p>不直接回复 HP，而是累积一个恢复池，每回合缓慢回复（池/10），
 * 满血时池子保留等待，池子耗尽后 buff 自动消失。
 * 力量/经验药剂或其炼金产物恢复量翻倍。</p>
 */
public class LifeRegen extends Buff {

	{
		type = buffType.POSITIVE;
	}

	private float regenToGive;

	public void setRegen( float amount ) {
		regenToGive += amount;
	}

	@Override
	public int icon() {
		return BuffIndicator.HEALING;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( Math.round( regenToGive ) );
	}

	@Override
	public boolean act() {
		if (target.isAlive() && regenToGive > 0) {
			// 仅在未满血时回复并消耗恢复池；满血时池子保留等待
			if (target.HP < target.HT) {
				float heal = Math.max( 1f, regenToGive / 10f );
				int h = Math.min( Math.round(heal), (int) Math.ceil(regenToGive) );
				int healed = target.heal( h );
				regenToGive -= h;
				if (healed > 0 && target.sprite != null) {
					target.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );
				}
				if (regenToGive <= 0.5f) {
					detach();
					return true;
				}
			}
			spend( TICK );
			return true;
		}
		diactivate();
		return true;
	}

	@Override
	public String name() {
		return "生命恢复";
	}

	@Override
	public String desc() {
		return "环指大师从药剂中汲取的生命力正在缓慢恢复伤势。\n\n"
				+ "每回合回复一定生命值，直至恢复池耗尽。\n\n"
				+ "剩余恢复量：_" + Math.round( regenToGive ) + "_";
	}

	private static final String REGEN = "regen_to_give";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( REGEN, regenToGive );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		regenToGive = bundle.getFloat( REGEN );
	}
}
