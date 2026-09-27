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

package com.shatteredpixel.shatteredpixeldungeon.levels.rooms.secret;

import com.shatteredpixel.shatteredpixeldungeon.items.DeathCertificate;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.painters.Painter;
import com.watabou.utils.Point;

/**
 * 死亡证明室（2026-09-09 用户需求）：隐藏房被选中生成时，有 5% 概率<b>整间替换</b>
 * 为这间房（见 {@link SecretRoom#createRoom()}）——房间中央供奉「死亡证明」，
 * 代替该隐藏房原本的奖励掉落物。
 * <p>有意<b>不</b>登记进 {@link SecretRoom#ALL_SECRETS}：仅作为 5% 的替换房型出现，
 * 不会进入常规的隐藏房型轮换池。
 */
public class SecretDeathProofRoom extends SecretRoom {

	@Override
	public void paint(Level level) {
		Painter.fill(level, this, Terrain.WALL);
		Painter.fill(level, this, 1, Terrain.EMPTY);

		//中央供奉死亡证明
		Point center = center();
		level.drop(new DeathCertificate(), level.pointToCell(center));

		entrance().set(Door.Type.HIDDEN);
	}
}
