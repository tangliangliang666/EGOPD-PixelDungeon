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

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 神的宠儿专精的「指令加护」buff。
 * <p>完成指令时层数 +1，上限 {@link #MAX_STACKS} 层。
 * 拥有 X 层时，获得额外的 X×3% 精准与闪避属性。</p>
 */
public class DivineBlessing extends Buff {

	public static final int MAX_STACKS = 9;

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;

	public int stacks(){
		return stacks;
	}

	//每层 +3% 精准/闪避
	public float accuracyMultiplier(){
		return 1f + 0.03f * stacks;
	}

	public float evasionMultiplier(){
		return 1f + 0.03f * stacks;
	}

	public void gainStack(){
		if (stacks < MAX_STACKS){
			stacks++;
			BuffIndicator.refreshHero();
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.DIVINE_BLESSING;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String name() {
		return "指令加护";
	}

	@Override
	public String desc() {
		return "受到指令眷顾者所得到的加护，获得额外的" + (stacks * 3) + "%精准和闪避属性";
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
	}

}
