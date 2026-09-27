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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * 试作-解体刀：环指大师 1 阶人体派作品武器（初始武器）。
 * 名称/描述文本键 items.weapon.melee.trialdissectionknife.*。
 * ——保留四项数值/字母等级等人体派作品特性，同时拥有差异化的面板（1~8，成长不变）
 * 以及「攻击命中附加 1~2 流血」效果。
 */
public class TrialDissectionKnife extends BodyArtWeapon {
	{
		image = ItemSpriteSheet.TRIAL_DISSECTION_KNIFE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.1f;

		tier = 1;
		weaponValue = 10;  // 试作初始即带 10 Weapon 值（阈值 10，故仍为 F 级；weapon 值达 20 时 +1 级）
	}

	//试作（1 阶初始武器）的强化阈值修正为 10（weapon 值每超阈值 10 点 +1 级 → 20 值时 +1），
	//不同于 2 阶创作的起始阈值 30——此前误继承 tier1/2 共用阈值导致与二阶武器同门槛。
	@Override
	public int reinforceThreshold() {
		return 10;
	}

	//初始面板 1~8，成长沿用 tier=1 默认：min = tier + lvl = 1 + lvl，故只覆写 max
	@Override
	public int max(int lvl) {
		//原 tier=1 的 max=5*(1+1) + lvl*(1+1) = 10 + 2*lvl。成长保持（lvl+1 系数同默认），
		//仅把基础值从 10 降到 8：1~8 0级面板，等级每 +1 同时 min +1、max +2（同默认）。
		return Math.round((8 + lvl * (tier + 1)) * boneMultiplier());
	}

	//命中时附加 1~2 点流血
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);
		if (defender.isAlive()) {
			int bleed = Random.Int(1, 2 + 1); //Random.Int 上限不包含，故 +1 得到 1..2
			Buff.affect(defender, Bleeding.class).set(bleed);
		}
		return damage;
	}
}
