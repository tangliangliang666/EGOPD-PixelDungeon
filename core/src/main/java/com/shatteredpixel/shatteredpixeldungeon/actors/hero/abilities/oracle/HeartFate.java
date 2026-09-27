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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.oracle;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.HeartFateBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

/**
 * 神谕代行者的第一个盔甲技能「心-命运」。
 * <p>消耗 20% 盔甲充能，获得 10 回合的 {@link HeartFateBuff}（重复使用可叠加）。
 * 持有心-命运时获得额外 20% 精准与闪避。不灭之心天赋可额外增加持续时间。</p>
 */
public class HeartFate extends ArmorAbility {

	{
		baseChargeUse = 20f; //消耗20%盔甲充能
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		//获得10回合心-命运（可叠加）；不灭之心：+1~+4 额外增加 5/10/15/20 回合
		float duration = 10f + 5f * hero.pointsInTalent(Talent.IMMORTAL_HEART);
		Buff.prolong(hero, HeartFateBuff.class, duration);

		hero.sprite.operate(hero.pos);

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Invisibility.dispel();
		hero.spendAndNext(2f);
	}

	@Override
	public int icon() {
		return HeroIcon.HEART_FATE;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.UNMOVING_HEART, Talent.UNBOUND_HEART, Talent.IMMORTAL_HEART, Talent.HEROIC_ENERGY};
	}
}
