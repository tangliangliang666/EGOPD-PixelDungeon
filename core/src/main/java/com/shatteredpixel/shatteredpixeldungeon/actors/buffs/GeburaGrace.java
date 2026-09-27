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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Bundle;

/**
 * 考验 GEBURA（严厉）：「即将死亡时进入无敌状态，一定回合内不会倒下，计时结束才死」的状态本体。
 *
 * <p>由 {@code Trials.interceptLethalDamage} 在 {@code Char.damage} 的致死闸门处挂上，
 * 回合数 = 该单位的固有经验值（{@code Mob.EXP}）。它同时负责三件事：</p>
 * <ul>
 *   <li><b>不倒</b>：{@link #attachTo} 把「无敌中」缓存到 {@code Mob.geburaGrace}，
 *       {@code Mob.isAlive()} 因此为真 —— 于是 {@code Char.damage} 紧随其后那句
 *       {@code if (!isAlive()) die(src);} 不会执行（血量按需求停在 0）。</li>
 *   <li><b>无敌</b>：同一个缓存让 {@code Mob.isInvulnerable()} 为真，之后一切伤害都被
 *       {@code Char.damage} / {@code Char.attack} 里原版就有的「无敌」分支吃掉。</li>
 *   <li><b>计时结束即死</b>：{@link #act} 数到 0 时摘下自己并调 {@code die(致死来源)}，
 *       用**原始致死来源**，保证击杀归属（经验、掉落、环指大师素材、复仇账簿充能等）与原版一致。</li>
 * </ul>
 *
 * <p>⚠️ 缓存刻意不放进 {@code isAlive()} 里现查 {@code buff(...)}：原版在 {@code Char.deathMarked}
 * 上注明过 isAlive() 会在**绘制期**被调用（性能 + 线程约束）。缓存由 attachTo/detach 维护，
 * 读档时 buff 会被 {@code Char.restoreFromBundle} 重新 attach，缓存也就自动重建。</p>
 *
 * <p>⚠️ type 只能是 POSITIVE / NEUTRAL：{@code Mob.Sleeping} 把 NEGATIVE 的 buff 当作「被打醒」，
 * 标成 NEGATIVE 会让全怪一入场就醒来。</p>
 */
public class GeburaGrace extends Buff {

	{
		type = buffType.POSITIVE;
	}

	/** 还剩几个回合。数到 0 就真正倒下。 */
	private int turnsLeft = 1;

	/** 本次致死的原始来源。同一次运行内直接沿用（最准的口径）。 */
	private transient Object lethalSource;

	/** 跨存档兜底：来源的类型名。存档存不了对象，只剩它能用来还原「英雄本人」这一类来源。 */
	private String lethalSourceClass = "";

	/** 设置回合数与本次的致死来源。 */
	public void set( int turns, Object src ) {
		turnsLeft = Math.max( 1, turns );
		lethalSource = src;
		lethalSourceClass = (src == null) ? "" : src.getClass().getName();
	}

	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			if (target instanceof Mob) {
				((Mob) target).geburaGrace = true;
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		//先清缓存再走基类流程：无论是因为计时结束、还是因为单位被别的方式终结
		//（destroy → onRemove 摘 buff），缓存都要跟着灭，不能让「无敌」留成幽灵状态。
		if (target instanceof Mob) {
			((Mob) target).geburaGrace = false;
		}
		super.detach();
	}

	@Override
	public boolean act() {

		if (--turnsLeft <= 0) {

			//⚠️ 顺序要紧：detach() 会把 Mob.geburaGrace 清掉，而 isAlive() 依赖它 ——
			//所以必须**先问活着、后摘自己**，反过来问就永远是 false、怪物会赖着不死。
			Char t = target;
			boolean alive = (t != null) && t.isAlive();

			Object src = lethalSource;
			if (src == null && Dungeon.hero != null && Hero.class.getName().equals( lethalSourceClass )) {
				//跨存档：对象还原不了，但「英雄本人击杀」这一类可以还原 —— 它是最常见的一种，
				//关系到环指大师素材掉落、复仇账簿充能、经验值等「英雄击杀」专属结算。
				src = Dungeon.hero;
			}
			lethalSource = null;

			detach();

			if (alive && t instanceof Mob) {
				//计时结束 ⇒ 真正倒下：等价于「那一击本来就在此刻落地」。
				t.die( src );
			}
			return true;
		}

		spend( TICK );
		return true;
	}

	private static final String TURNS_LEFT		= "turns_left";
	private static final String SOURCE_CLASS	= "source_class";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TURNS_LEFT, turnsLeft );
		bundle.put( SOURCE_CLASS, lethalSourceClass );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		turnsLeft = bundle.getInt( TURNS_LEFT );
		if (bundle.contains( SOURCE_CLASS )) {
			lethalSourceClass = bundle.getString( SOURCE_CLASS );
		}
	}
}
