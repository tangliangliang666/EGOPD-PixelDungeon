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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 渴望：四阶E.G.O武器。
 * <p>面板 4+L ~ 20+5L、攻击距离 2（继承长矛式额外攻击距离）。命中时对双方
 * （攻击者与目标）施加 20% 造成伤害量的流血。</p>
 * <p>装备（主手或副手）被动：英雄受到的流血伤害绕过护盾、不会因流血致死（HP 保底 1），
 * 且每次流血都会获得等量奥术屏障护盾——结算位于 {@link com.shatteredpixel.shatteredpixeldungeon.actors.Char#damage}
 * 的护盾吸收段特判（见 {@link ThirstPact}）。</p>
 */
public class Thirst extends MeleeWeapon {

	{
		image = ItemSpriteSheet.THIRST;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1f;

		tier = 4;
		RCH = 2; //额外的攻击距离
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  20 +  //base
				5*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		// 攻击者抗魔或目标已阵亡 → 跳过附加效果。
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive() && damage > 0){
			//双方各施加 20% 本次造成伤害量的流血（取整，至少 1 点）
			int bleed = Math.max(1, Math.round(damage * 0.2f));
			Buff.affect(defender, Bleeding.class).set(bleed);
			if (attacker.isAlive()){
				Buff.affect(attacker, Bleeding.class).set(bleed);
			}
		}

		return damage;
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//装备时挂载「流血转化」被动
		if (ch instanceof Hero){
			Buff.affect(ch, ThirstPact.class);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			Buff.detach(hero, ThirstPact.class);
			return true;
		} else {
			return false;
		}
	}

	//装备时的流血转化被动标记：由 Char.damage 的护盾吸收段特判读取。
	//无独立行为，每回合自校验：渴望已不在身上则自行移除（兜底，覆盖非正常卸下路径）。
	public static class ThirstPact extends Buff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero){
				Hero hero = (Hero) target;
				if (!(hero.belongings.weapon() instanceof Thirst)
						&& !(hero.belongings.secondWep() instanceof Thirst)){
					detach();
					return true;
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}

}
