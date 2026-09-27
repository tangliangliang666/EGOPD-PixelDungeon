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

/**
 * 「深度创伤」buff（2026-09-04，环指大师盔甲技能「闭馆」的专精天赋「将化为观众席」施加）。
 * <p>持续期间目标的流血不会衰减：{@link Bleeding#act()} 结算伤害时跳过「层数缩减」，
 * 直到本 buff 到期（FlavourBuff 自动倒计时）后才恢复原版的每回合衰减。</p>
 * <p>buff_icons.png 帧 98（用户已绘制），对应 {@link BuffIndicator#DEEP_TRAUMA}。</p>
 */
public class DeepTrauma extends FlavourBuff {

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.DEEP_TRAUMA;
	}
}
