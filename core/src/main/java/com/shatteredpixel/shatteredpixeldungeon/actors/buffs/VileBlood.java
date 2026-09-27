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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 污血（葬花楔）：敌人被葬花楔钉中（卡入=中矢）期间获得的负面状态。
 * <p>持续期间每当该角色受到一次攻击命中，额外承受 (1+等级) 点<b>无视护甲</b>的魔法伤害；
 * 等级取插在目标身上的全部葬花楔中最高强化等级（多把不同等级取最高）。
 * 受击追加的结算点：{@code Char.attack()} 中 {@code enemy.damage()} 之后。</p>
 * <p>该状态不自行倒计时：随葬花楔卡入而存在、随目标死亡/插楔清空而消失（参照 PinCushion 的存在方式）。</p>
 */
public class VileBlood extends Buff {

	{
		type = buffType.NEGATIVE;
	}

	//触发强度：受击时额外 (1+level) 点魔法伤害
	public int level = 0;

	@Override
	public int icon() {
		return BuffIndicator.CORRUPT;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	private static final String LEVEL = "level";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(LEVEL, level);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		level = bundle.getInt(LEVEL);
	}

}
