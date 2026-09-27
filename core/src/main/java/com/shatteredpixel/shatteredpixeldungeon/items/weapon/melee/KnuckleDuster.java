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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 指虎 —— 一阶近战快武器（2026-09-13 用户新武器）。
 *
 * <p>攻击延迟 0.5（2 倍攻速），伤害 {@code 1+L ~ 5+L}，与拳套（{@link Gloves}）同数值，
 * 因此直接继承拳套；武技（决斗家的连击突刺）也一并沿用拳套。</p>
 *
 * <p>图标为 {@link ItemSpriteSheet#KNUCKLE_DUSTER}（扩展预留区 41 行第 2 格，15×10）。
 * 与磨损对剑/空酒瓶一样<b>不入武器池</b>，目前只作为「中指 长兄」的初始武器发放。</p>
 */
public class KnuckleDuster extends Gloves {

	{
		image = ItemSpriteSheet.KNUCKLE_DUSTER;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 1;
		DLY = 0.5f; //2x speed

		//同拳套：不进入遗骨生成
		bones = false;
	}

	//显式写出伤害公式（与拳套一致）：下限 1+L、上限 5+L
	@Override
	public int min(int lvl) {
		return 1 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 5 + lvl;
	}
}
