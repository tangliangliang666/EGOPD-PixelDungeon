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
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.watabou.utils.Bundle;

/**
 * 考验 HOD（荣耀）：「敌方单位存活超过 N 回合即获得随机精英效果」的那只计时器。
 *
 * <p>与 {@link ChesedMend} 同型：**常驻 buff**（不限期），由 {@code Trials.bindMobPassives}
 * 在 {@code Mob.onAdd()} 里幂等重挂 —— 挂 buff 而不是写进 {@code Mob.act()}，是因为一部分单位的
 * {@code act()} 覆写不调 {@code super.act()}（CrystalSpire、Masterpiece、MobSpawner、Sheep 等），
 * 而 buff 有自己的回合，谁都漏不掉；另一个好处是它随存档保存（{@code Char.storeInBundle} 存的就是
 * buffs 列表），读档后继续沿用原来的计时。</p>
 *
 * <p>计时口径严格照需求「**存活**超过 N 回合」（普通 300 / Boss 450，见 {@link Trials#hodEliteTurns}）：
 * 按回合累加，只有在「是敌方阵营 + 活着 + 本考验仍开着」
 * 时才推进；一旦不再适用就**清零**（不再算存活）。数满 {@link Trials#hodEliteTurns} 之后调用
 * {@link Trials#grantHodElite} 晋升，然后**自摘** —— 这个 buff 的使命就是「数到 N 发一次精英」，
 * 发完即止，绝不再重掷（「不可叠加」的一半保障在这里；另一半在 {@code grantHodElite} 里）。</p>
 *
 * <p>⚠️ buff 的 type 只能是 POSITIVE / NEUTRAL：{@code Mob.Sleeping} 把 type == NEGATIVE 的 buff
 * 当成「被打醒」的信号（见 Mob.Sleeping.act 的第一个循环），标成 NEGATIVE 会让全图怪物一入场就醒来
 * 并进入警戒 —— 那是灾难性的副作用（与 {@link ChesedMend} 同一条红线）。</p>
 */
public class HodGlory extends Buff {

	{
		type = buffType.POSITIVE;
	}

	/** 已存活回合数（只在「是敌方且活着」时推进）。 */
	private int turns = 0;

	@Override
	public boolean act() {

		Mob mob = (target instanceof Mob) ? (Mob) target : null;

		//只认敌方单位（与 Trials.modifyMobHT / ChesedMend 同判据）。每次现判 alignment 而不是挂 buff
		//时判一次：被腐蚀、被招募、被魅惑成友方之后会立刻停止计时，变回敌方后再从 0 重数。
		if (mob == null
				|| !Dungeon.isTrialled( Trials.HOD )
				|| mob.alignment != Char.Alignment.ENEMY
				|| !mob.isAlive()) {
			turns = 0;
			spend( TICK );
			return true;
		}

		if (++turns > Trials.hodEliteTurns( mob )) {
			//晋升为精英（已有精英时不叠加，见 Trials.grantHodElite）。
			Trials.grantHodElite( mob );
			//发完即止：自摘，本 buff 不再继续计时（任务已完成，留着也不会再发第二次）。
			detach();
			spend( TICK );
			return true;
		}

		spend( TICK );
		return true;
	}

	private static final String TURNS = "turns";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TURNS, turns );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		turns = bundle.getInt( TURNS );
	}
}
