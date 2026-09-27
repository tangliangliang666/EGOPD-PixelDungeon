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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.GritTeethBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

/**
 * 中指 长兄 盔甲技能「<b>咬紧牙关</b>」（2026-09-17）。
 *
 * <p>消耗 40 点盔甲充能，获得 {@link Talent#gritTeethDuration 基础 5 回合}
 * （「野兽意志」每点 +3/+5/+10）的 {@link GritTeethBuff}：<b>0 血不死</b>，
 * 但「解离射线」可以击穿这个判定。</p>
 *
 * <h3>三个分支天赋</h3>
 * <ul>
 *   <li><b>野兽意志</b>：延长持续时间（在 {@link Talent#gritTeethDuration} 里结算）；</li>
 *   <li><b>濒亡狂怒</b>：0 血期间打出去的伤害 +100%/200%/300%
 *       （在 {@code Char.damage} 的「英雄打出去的伤害」乘区，即 {@code beastFuryMultiplier} 旁边结算）；</li>
 *   <li><b>绝境迫发</b>：结束时回复 5%/10%/15% 最大生命值（在 {@link GritTeethBuff#act()} 里结算）。</li>
 * </ul>
 *
 * <h3>图标</h3>
 * <p>使用中指长兄盔甲技能一的<b>专属槽位</b>（{@link HeroIcon#MIDDLE_FINGER_ABILITY_1}，
 * {@code hero_icons.png} 第 17 行第 2 列 = 帧 129）。</p>
 */
public class GritTeeth extends ArmorAbility {

	{
		baseChargeUse = 40f;
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ){

		float duration = Talent.gritTeethDuration( hero );
		//已持有则刷新时长（并把「被射线击穿」的标记清掉）
		GritTeethBuff.refresh( hero, duration );

		hero.sprite.operate( hero.pos );
		SpellSprite.show( hero, SpellSprite.BERSERK );
		Sample.INSTANCE.play( Assets.Sounds.CHALLENGE );
		BuffIndicator.refreshHero();
		GLog.p( Messages.get( this, "cast" ) );

		armor.charge -= chargeUse( hero );
		armor.updateQuickslot();
		Invisibility.dispel();
		hero.spendAndNext( Actor.TICK );
	}

	@Override
	public int icon(){
		//专属槽位：hero_icons.png 第 17 行第 2 列 = 帧 129
		return HeroIcon.MIDDLE_FINGER_ABILITY_1;
	}

	@Override
	public Talent[] talents(){
		return new Talent[]{
				Talent.BEAST_WILL, Talent.NEAR_DEATH_FURY, Talent.DESPERATE_BURST,
				Talent.HEROIC_ENERGY };
	}
}
