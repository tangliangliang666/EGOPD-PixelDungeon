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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.PalermoFencing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 拇指 前二老板的盔甲技能「处置」（2026-09-08）。
 * <p>消耗 80% 盔甲充能，对 3 格内目标连续施展 4 次巴勒莫剑术，每次命中后击退目标 1 格并追及；
 * 前 3 次耗时减半、第 4 次完整（总耗时 = 2.5 × 攻击延迟）。</p>
 * <p>对应天赋：爆碎收尾（收尾伤害/击退/眩晕麻痹）、枪拼刺杀（前 N 次不消耗「加速的未来」）、
 * 毫无悬念（击杀恢复盔甲充能）。</p>
 */
public class Disposal extends ArmorAbility {

	{
		baseChargeUse = 80f; //消耗 80% 盔甲充能
	}

	@Override
	public String targetingPrompt() {
		return Messages.get(this, "prompt");
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		if (target == null) return;

		Char enemy = Actor.findChar( target );
		if (enemy == null || enemy == hero || hero.isCharmedBy( enemy )){
			GLog.w( Messages.get(this, "no_target") );
			return;
		}

		if (!(hero.belongings.attackingWeapon() instanceof MeleeWeapon)){
			GLog.w( Messages.get(this, "no_weapon") );
			return;
		}

		//校验全部通过（disposal 内部完成距离/扎根/落点等检查）后才扣充能，失败不白扣
		if (PalermoFencing.disposal( hero, target, (MeleeWeapon) hero.belongings.attackingWeapon(), armor )){
			armor.charge -= chargeUse( hero );
			armor.updateQuickslot();
			Invisibility.dispel();
		}
	}

	@Override
	public int icon() {
		return HeroIcon.DISPOSAL;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.BURST_FINISH, Talent.LUNGE_STAB, Talent.NO_SUSPENSE, Talent.HEROIC_ENERGY};
	}
}
