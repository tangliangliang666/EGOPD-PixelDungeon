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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.effects.Splash;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite.Glowing;
import com.watabou.utils.Random;

/**
 * 欲望附魔（2026-09-10 用户新增）。
 * <p>粉色光效，机制照抄烈焰的概率范式：{@code (lvl+1)/(lvl+5) × 附魔强度倍率}，
 * 因此在无附魔强度、0 级武器时正好是 20%。</p>
 * <p>两段式判定：</p>
 * <ul>
 *     <li>概率尚未达到 100%（{@code procChance < 1}）→ 命中即施加<b>残废</b>（固定 5 回合）；</li>
 *     <li>附魔强度已把概率顶到 100%（{@code procChance >= 1}，即"必定残废"）→
 *         溢出的强度（{@code procChance - 1}）转为<b>扎根</b>（固定 3 回合）的概率，
 *         未命中则仍然施加残废。</li>
 * </ul>
 * <p>扎根由 {@link Roots} 施加，飞行单位对其免疫（{@link Roots#attachTo} 返回 false），
 * 这类敌人会直接落在残废分支。</p>
 */
public class Desire extends Weapon.Enchantment {

	private static final Glowing PINK = new ItemSprite.Glowing( 0xFF69B4 );

	/** 残废：固定 5 回合（不经附魔强度放大，仍会走目标的 debuff 抗性）。 */
	public static final float CRIPPLE_DURATION = 5f;
	/** 扎根：固定 3 回合。 */
	public static final float ROOT_DURATION = 3f;

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {
		int level = Math.max( 0, weapon.buffedLvl() );

		// lvl 0 - 20%（无附魔强度）
		// lvl 1 - 33.3%
		// lvl 2 - 42.9%
		float procChance = (level+1f)/(level+5f) * procChanceMultiplier(attacker);

		if (Random.Float() < procChance) {

			// procChance < 1 时 rootChance <= 0，等价于"未达阈值只上残废"
			float rootChance = procChance - 1f;

			if (rootChance > 0 && !defender.flying
					&& Random.Float() < Math.min( 1f, rootChance )) {
				Buff.affect( defender, Roots.class, ROOT_DURATION );
			} else {
				Buff.affect( defender, Cripple.class, CRIPPLE_DURATION );
			}

			Splash.at( defender.sprite.center(), 0xFFFF69B4, 5 );
		}

		return damage;
	}

	@Override
	public Glowing glowing() {
		return PINK;
	}
}
