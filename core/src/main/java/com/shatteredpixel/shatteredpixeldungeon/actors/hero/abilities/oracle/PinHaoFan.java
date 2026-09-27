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

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.UnstableSpellbook;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Random;

/**
 * 神谕代行者的第三个盔甲技能「拼好饭」。
 * <p>消耗 15% 盔甲充能：播放使用道具的动画，恢复 50 饱食度与 5 点生命，
 * 头顶显示消息「苦痛啊，你便是我的唯一....」。默认不触发进食特效（其他进食相关天赋的进食判定），
 * 除非点出对应分支天赋。</p>
 * <ul>
 *     <li>疗愈便当：生命恢复提升为 5/10/15/20</li>
 *     <li>饭前诵言：触发无序魔典效果（未升级/已升级形态）</li>
 *     <li>食之有味：触发进食特效（进食判定）</li>
 * </ul>
 */
public class PinHaoFan extends ArmorAbility {

	{
		baseChargeUse = 30f; //消耗30%盔甲充能（2026-09-20 由 15% 加倍：原消耗下强度过高）
	}

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Invisibility.dispel();

		hero.busy();

		//使用道具的动画
		hero.sprite.operate(hero.pos);

		//头顶消息
		hero.sprite.showStatus(CharSprite.DEFAULT, "苦痛啊，你便是我的唯一....");

		//恢复50饱食度
		Buff.affect(hero, Hunger.class).satisfy(50);

		//恢复血量：基础 5；疗愈便当 +1~+4 → 5/10/15/20
		int heal = 5;
		if (hero.hasTalent(Talent.HEALING_LUNCHBOX)){
			heal = 5 * hero.pointsInTalent(Talent.HEALING_LUNCHBOX);
		}
		int healed = hero.heal( heal );
		if (healed > 0) {
			hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, "+" + healed, FloatingText.HEALING);
		}

		//食之有味：触发进食特效（进食判定）
		//+1 50% / +2 必定触发一次；+3 必定一次+50%第二次；+4 必定两次
		int tasty = hero.pointsInTalent(Talent.TASTY_MEAL);
		if (tasty >= 1){
			if (tasty >= 2 || Random.Float() < 0.5f){
				Talent.onFoodEaten(hero, 50, null);
			}
			if (tasty >= 3 && (tasty >= 4 || Random.Float() < 0.5f)){
				Talent.onFoodEaten(hero, 50, null);
			}
		}

		//饭前诵言：触发无序魔典效果（由卷轴阅读占用回合，技能不再额外消耗回合）
		//+1 50%未升级 / +2 必定未升级 / +3 50%未升级或50%已升级 / +4 必定已升级
		boolean spellbookFired = false;
		int prayer = hero.pointsInTalent(Talent.PRAYER_BEFORE_MEALS);
		if (prayer >= 1){
			boolean unupgraded, empowered;
			if (prayer == 1){
				unupgraded = Random.Float() < 0.5f;
				empowered = false;
			} else if (prayer == 2){
				unupgraded = true;
				empowered = false;
			} else if (prayer == 3){
				unupgraded = Random.Float() < 0.5f;
				empowered = !unupgraded;
			} else {
				unupgraded = false;
				empowered = true;
			}

			if (empowered){
				new UnstableSpellbook().doEmpoweredRead(hero);
				spellbookFired = true;
			} else if (unupgraded){
				new UnstableSpellbook().doReadEffect(hero);
				spellbookFired = true;
			}
		}

		//时间消耗：触发无序魔典时由卷轴阅读占用一回合；否则技能自身消耗一回合
		if (!spellbookFired){
			hero.spendAndNext(Actor.TICK);
		}
	}

	@Override
	public int icon() {
		return HeroIcon.PIN_HAO_FAN;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.HEALING_LUNCHBOX, Talent.PRAYER_BEFORE_MEALS, Talent.TASTY_MEAL, Talent.HEROIC_ENERGY};
	}
}
