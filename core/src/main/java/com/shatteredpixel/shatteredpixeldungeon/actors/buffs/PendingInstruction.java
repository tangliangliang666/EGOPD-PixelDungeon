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
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 待接指令 buff：有新的指令等待接取。
 * <p>持续 {@link #DURATION} 回合。若在持续时间内未被接取（自然消失），
 * 「业」增加 {@link #KARMA_PENALTY}；接取时由指令终端手动移除（不触发惩罚）。</p>
 */
public class PendingInstruction extends FlavourBuff {

	public static final float DURATION = 5f;
	public static final int KARMA_PENALTY = 5;

	{
		type = buffType.POSITIVE;
	}

	@Override
	public boolean act() {
		//从心所欲 +3：待接的指令不会过期，一直等待到接收指令为止
		if (target instanceof Hero && ((Hero) target).hasTalent(Talent.FREE_WILL)
				&& ((Hero) target).pointsInTalent(Talent.FREE_WILL) == 3){
			spend(TICK);
			return true;
		}
		//自然消失（未接取）：业 +5
		if (target instanceof Hero){
			Karma karma = target.buff(Karma.class);
			if (karma != null) karma.addKarma(KARMA_PENALTY);
		}
		//若有待接的特殊指令（命运宠儿+3），一并清除
		Instruction.clearPendingSpecial();
		detach();
		return true;
	}

	@Override
	public int icon() {
		return BuffIndicator.PENDING_INSTRUCTION;
	}

	@Override
	public String name() {
		return "待接指令";
	}

	@Override
	public String desc() {
		return "指令终端正发出刺耳的哔哔声，需要接收指令。";
	}

}
