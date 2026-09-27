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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 「瞄准心脏」标记（拇指 前二老板盔甲技能，2026-09-08）。
 * <p>持续 {@link #DURATION} 回合的负面标记：标记期间目标_失去对燃烧与麻痹的免疫_
 * （在 {@link Char#isImmune(Class)} 中豁免这两类），并被前二老板攻击时受到额外伤害
 * （基础 20%伤害；「弱点贯穿」每点 +20%，即 40/60/80/100%）。</p>
 * <p>击杀被标记目标时「荣耀凯旋」使前二老板恢复最大生命 5/10/15/20%。</p>
 * <p>不在地图上叠加任何指示特效（图标帧 108 待用户绘制），该负面标记通过
 * 目标自身的 buff 图标（信息栏 / 状态区）呈现。</p>
 */
public class AimHeartMark extends FlavourBuff {

	public static final float DURATION = 10f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/** 对目标施加/刷新瞄准心脏标记（10 回合）。 */
	public static void mark( Char target ){
		if (target == null || !target.isAlive()) return;
		Buff.prolong( target, AimHeartMark.class, DURATION );
	}

	/** 前二老板攻击该目标时的伤害倍率（基础 1.2；弱点贯穿每点 +0.2）。 */
	public static float damageMultiplier( Hero hero ){
		float extra = 0.2f * (1f + hero.pointsInTalent( Talent.WEAKNESS_PIERCE ));
		return 1f + extra;
	}

	/** 前二老板击杀被瞄准心脏标记的目标 → 「荣耀凯旋」恢复最大生命 5%/10%/15%/20%。 */
	public static void onKillByHero( Hero hero ){
		int points = hero.pointsInTalent( Talent.GLORIOUS_TRIUMPH );
		if (points <= 0) return;
		int heal = Math.round( hero.HT * 0.05f * points );
		if (heal <= 0) return;
		int healed = hero.heal( heal );
		if (healed > 0 && hero.sprite != null){
			hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, "+" + healed, FloatingText.HEALING );
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.AIM_HEART_MARK;
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", dispTurns());
	}
}
