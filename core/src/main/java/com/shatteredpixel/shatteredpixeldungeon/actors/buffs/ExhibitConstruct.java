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

/**
 * 「展品构建」buff（2026-09-03 废弃）。
 * <p>原为画廊导师右下角 ActionIndicator 按钮载体，现技艺入口统一由大师指环
 * （AC_ARTS → WndRingMasterArts）承担，转职时不再施加本 buff。</p>
 * <p>保留类定义仅为旧存档兼容：旧档中残留的本 buff 读档后立即自行分离，
 * 不再注册任何右下角按钮。</p>
 */
public class ExhibitConstruct extends Buff {

	{
		type = buffType.POSITIVE;
	}

	//读档恢复后（下一次行动）立即分离，清理旧存档残留
	@Override
	public boolean act() {
		detach();
		return true;
	}

	@Override
	public String name() {
		return "展品构建";
	}

	@Override
	public String desc() {
		return "画廊导师的技艺入口已改由大师指环承担。";
	}
}
