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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.IgnominiousHeartBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;

/**
 * 拇指 前二老板的盔甲技能「心-不光彩」（2026-09-08）。
 * <p>消耗 20% 盔甲充能，获得 10 回合 {@link IgnominiousHeartBuff}（可叠加持续时间；
 * 「执拗之心」+1~+4 额外增加 5/10/15/20 回合）。持有期间免疫燃烧、
 * 移动速度提升（狂乱之心可加至 100%）、攻击延迟降低（狂乱之心可加至 -40%）。</p>
 * <p>技能图标按用户要求复用神谕代行者「心-命运」的图标（HeroIcon.HEART_FATE）；buff 图标复用 92。</p>
 */
public class IgnominiousHeart extends ArmorAbility {

	{
		baseChargeUse = 20f; //消耗 20% 盔甲充能
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		//获得 10 回合心-不光彩（可叠加）；执拗之心：+1~+4 额外增加 5/10/15/20 回合
		float duration = 10f + 5f * hero.pointsInTalent( Talent.STUBBORN_HEART );
		//点燃状态的残余火焰立即熄灭（避免免疫后仍烧一回合）
		Buff.detach( hero, Burning.class );
		Buff.prolong( hero, IgnominiousHeartBuff.class, duration );

		hero.sprite.operate( hero.pos );

		armor.charge -= chargeUse( hero );
		armor.updateQuickslot();
		Invisibility.dispel();
		hero.spendAndNext( 2f );
	}

	@Override
	public int icon() {
		return HeroIcon.HEART_FATE; //复用神谕代行者「心-命运」的技能图标
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.WILD_HEART, Talent.SHAMEFUL_HEART, Talent.STUBBORN_HEART, Talent.HEROIC_ENERGY};
	}
}
