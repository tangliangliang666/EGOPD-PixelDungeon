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

import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.CorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.ToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.HeartFateParticle;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Random;

import java.util.HashSet;

/**
 * 「心-命运」buff：神谕代行者使用盔甲技能「心-命运」后获得。
 * <p>持有期间获得额外的 20% 精准与闪避；重复使用可以叠加持续时间。
 * 对应三个四阶分支天赋：</p>
 * <ul>
 *     <li>不移之心：持有期间获得 1~英雄等级/X 的额外护甲值（X=4/3/2/1）</li>
 *     <li>不羁之心：持有期间免疫残废/缠绕/冻伤（+1）、麻痹/眩晕/燃烧（+2）、魅惑/恍惚/虚弱（+3）、毒气与酸气（+4）</li>
 *     <li>不灭之心：获得心-命运时额外增加持续时间（由盔甲技能计算）</li>
 * </ul>
 * <p>粒子效果为金色火焰（燃烧粒子配色，运动速度为燃烧的 50%）。</p>
 */
public class HeartFateBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	//持有心-命运：额外 20% 精准
	public float accuracyMultiplier(){
		return 1.2f;
	}

	//持有心-命运：额外 20% 闪避
	public float evasionMultiplier(){
		return 1.2f;
	}

	//不移之心：持有心-命运时获得 1~英雄等级/X 的额外护甲值（X = 4/3/2/1 对应天赋 1~4 点）
	public int armorBonus(){
		if (target instanceof Hero){
			Hero hero = (Hero) target;
			int points = hero.pointsInTalent(Talent.UNMOVING_HEART);
			if (points > 0){
				int divisor = 5 - points; //4 / 3 / 2 / 1
				return Random.NormalIntRange(1, Math.max(1, hero.lvl / divisor));
			}
		}
		return 0;
	}

	//不羁之心：持有心-命运时免疫对应状态（+4 免疫毒气/酸气，类似净化药水）
	@Override
	public HashSet<Class> immunities() {
		HashSet<Class> immunes = super.immunities();
		if (target instanceof Hero){
			Hero hero = (Hero) target;
			int points = hero.pointsInTalent(Talent.UNBOUND_HEART);
			if (points >= 1){
				immunes.add(Cripple.class);
				immunes.add(Roots.class);
				immunes.add(Chill.class);
			}
			if (points >= 2){
				immunes.add(Paralysis.class);
				immunes.add(Vertigo.class);
				immunes.add(Burning.class);
			}
			if (points >= 3){
				immunes.add(Charm.class);
				immunes.add(Daze.class);
				immunes.add(Weakness.class);
			}
			if (points >= 4){
				immunes.add(ToxicGas.class);
				immunes.add(CorrosiveGas.class);
			}
		}
		return immunes;
	}

	//金色火焰粒子（套用燃烧状态的粒子效果，金色配色，运动速度50%）
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
		return BuffIndicator.HEART_FATE;
	}

	@Override
	public String name() {
		return "心-命运";
	}

	@Override
	public String desc() {
		return "心-命运加身：获得额外的_20%精准_和_闪避_属性。\n\n剩余回合：" + dispTurns();
	}
}
