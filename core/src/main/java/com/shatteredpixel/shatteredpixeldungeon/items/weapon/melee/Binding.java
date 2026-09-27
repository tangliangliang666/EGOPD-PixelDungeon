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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Paralysis;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Roots;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 拘束：四阶E.G.O武器。
 * <p>面板 4+L ~ 30+6L。命中时对双方（攻击者自身与被攻击目标）各按以下规则处理：</p>
 * <ul>
 *   <li>该单位身上没有残废 → 施加 10 回合残废；</li>
 *   <li>该单位已有残废 → 改为施加（刷新为）5 回合扎根（重复施加不叠加、只刷新回合数）。</li>
 * </ul>
 * <p>若攻击目标（不含自己）此时带有扎根，则有 (20+武器等级)% 概率对其施加 3 回合麻痹。</p>
 */
public class Binding extends MeleeWeapon {

	{
		image = ItemSpriteSheet.BINDING;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  30 +  //base
				6*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		// 攻击者抗魔（HolyWard / StoneOfMagic 等）或目标已阵亡 → 跳过附加效果。
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){

			//双方（含自己）各结算一次：残废 ↔ 扎根
			restrain(attacker);
			if (defender.isAlive()){
				restrain(defender);
			}

			//攻击目标带有扎根时，有 (20+武器等级)% 概率施加 3 回合麻痹（只对目标，不对自己）
			if (defender.isAlive() && defender.buff(Roots.class) != null
					&& Random.Int(100) < 20 + buffedLvl()){
				Buff.affect(defender, Paralysis.class, 3f);
			}
		}

		return damage;
	}

	//对单个单位结算：无残废→10回合残废；已有残废→（移除残废）改施5回合扎根（刷新，不叠加）
	private static void restrain(Char ch){
		if (ch.buff(Cripple.class) != null){
			Buff.detach(ch, Cripple.class);
			Buff.detach(ch, Roots.class);
			Buff.affect(ch, Roots.class, 5f);
		} else {
			Buff.affect(ch, Cripple.class, 10f);
		}
	}

}
