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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VileBlood;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 葬花楔 —— 四阶E.G.O投掷武器（可炼金重构获得，亦登记入四阶投掷武器掉落池）。
 *
 * <p>面板套用标枪（Javelin，默认公式 8+L ~ 20+4L），命中后卡入敌人（中矢），
 * 投掷飞行不旋转（角速度 0，见 {@code MissileSprite}）。
 * 效果：
 * <ol>
 *   <li>命中并卡入敌人时，使敌人获得_污血_（{@link VileBlood}）；</li>
 *   <li>带污血的敌人每次受到攻击命中时，额外受到 (1+葬花楔强化等级) 点无视护甲的魔法伤害；
 *       若目标身上卡着多把不同等级的葬花楔，取最高等级（结算见 {@code Char.attack()}）。</li>
 * </ol></p>
 */
public class BuryingWedge extends MissileWeapon {

	{
		image = ItemSpriteSheet.BURYING_WEDGE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1f;

		tier = 4;
		//sticky 保持默认 true：命中后卡入敌人（中矢）
	}

	@Override
	protected void rangedHit(Char enemy, int cell) {
		super.rangedHit(enemy, cell);

		//仅在本次投掷真正卡入（中矢）后赋予/刷新污血。
		//super 卡入成功的条件：耐久>0、非 spawnedForEffect、sticky、目标存活且非友方
		if (enemy == null || spawnedForEffect || durability <= 0
				|| !enemy.isActive() || enemy.alignment == Char.Alignment.ALLY){
			return;
		}
		PinCushion pincushion = enemy.buff(PinCushion.class);
		if (pincushion == null) return;

		//污血等级 = 目标身上所有葬花楔的最高强化等级
		int maxLvl = 0;
		for (MissileWeapon stuck : pincushion.getStuckItems()){
			if (stuck instanceof BuryingWedge){
				maxLvl = Math.max(maxLvl, stuck.trueLevel());
			}
		}

		VileBlood vile = Buff.affect(enemy, VileBlood.class);
		vile.level = Math.max(vile.level, maxLvl);
	}

}
