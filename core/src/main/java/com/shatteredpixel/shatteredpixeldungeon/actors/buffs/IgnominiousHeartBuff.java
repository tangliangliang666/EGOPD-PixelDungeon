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

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.HeartFateParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;

import java.util.HashSet;

/**
 * 「心-不光彩」buff（拇指 前二老板盔甲技能，2026-09-08）。
 * <p>持有期间：免疫燃烧；移动速度提升（基础 20%，「狂乱之心」每点 +20%，上限 100%）；
 * 攻击延迟降低（基础 20%，每点 +5%，上限 40%）。
 * 「耻辱之心」使持有者获得 = 移速提升 / (4/3/2/1) 的伤害加成（对应 +1~+4）。
 * 「执拗之心」在获得该 buff 时额外增加持续时间（由盔甲技能计算）。</p>
 * <p>图标按用户决定复用「心-命运」的 buff 图标（BuffIndicator.HEART_FATE = 92）。</p>
 */
public class IgnominiousHeartBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	/** 「狂乱之心」点数（0~4；未装备该盔甲技能时自然为 0）。 */
	private int wildPoints(){
		if (target instanceof Hero){
			return ((Hero) target).pointsInTalent( Talent.WILD_HEART );
		}
		return 0;
	}

	/** 移动速度提升比例（0.2 ~ 1.0）。 */
	public float speedBonus(){
		return 0.2f * (1f + Math.min( 4, wildPoints() ));
	}

	/** 攻击延迟降低比例（0.2 ~ 0.4）。 */
	public float delayCut(){
		return Math.min( 0.4f, 0.2f + 0.05f * wildPoints() );
	}

	/**
	 * 「耻辱之心」：伤害加成 = 移速提升比例 ÷ (4/3/2/1，对应 +1~+4)。
	 * 未点「耻辱之心」时无加成。
	 */
	public float damageBonus(){
		if (!(target instanceof Hero)) return 0f;
		int points = ((Hero) target).pointsInTalent( Talent.SHAMEFUL_HEART );
		if (points <= 0) return 0f;
		return speedBonus() / (5f - points);
	}

	/** 免疫燃烧。 */
	@Override
	public HashSet<Class> immunities() {
		HashSet<Class> immunes = super.immunities();
		immunes.add( Burning.class );
		return immunes;
	}

	//金色火焰粒子（与「心-命运」同款特效，复用 HeartFateParticle）
	private Emitter fateEmitter = null;

	@Override
	public void fx(boolean on) {
		if (on){
			if (fateEmitter == null && target.sprite != null){
				fateEmitter = target.sprite.emitter();
				fateEmitter.pour(HeartFateParticle.FACTORY, 0.06f);
			}
		} else {
			if (fateEmitter != null){
				fateEmitter.on = false;
				fateEmitter = null;
			}
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.HEART_FATE; //复用「心-命运」buff 图标
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
