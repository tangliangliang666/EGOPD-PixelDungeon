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

/**
 * 「预感」buff（拇指 前二老板 T2「未来视角」+2，图标 ID 104）。
 *
 * <p>即使预知眼没有生效，在周围（相邻 8 格）存在隐藏门或陷阱时依旧会获得
 * 预感。由 {@link ValencinaTracker} 每回合按条件刷新。</p>
 */
public class Premonition extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	@Override
	public int icon() {
		return BuffIndicator.PREMONITION;
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	//desc 由 FlavourBuff 基类实现（本类无额外状态，直接继承即可）
}
