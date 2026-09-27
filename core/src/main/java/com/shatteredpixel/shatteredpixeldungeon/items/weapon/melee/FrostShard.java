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
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Chill;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Frost;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//霜之碎片：三阶近战武器，1.5攻击延迟、额外攻击距离（继承长矛），突刺音效，武技配置套用长矛（Spear）。
//效果：命中时施加 (2+武器等级) 回合的冻伤；对已有冻伤的敌人，有
//(20+2×敌人剩余冻伤回合数)% 概率改为消除冻伤、施加10回合的冻结。
public class FrostShard extends Spear {

	{
		image = ItemSpriteSheet.FROST_SHARD;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 3;
		DLY = 1.5f; //0.67x speed
		//RCH = 2（额外攻击距离）继承自长矛
	}

	@Override
	public int min(int lvl) {
		return  3 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  25 +  //base
				5*lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		// 冻结施加时序修复（2026-09-06）：
		// 原实现把 Buff.affect(Frost) 直接写在 weapon.proc 里——但 proc 在 Char.attack 中
		// 早于 enemy.damage(...)，而 Char.damage 走 Char.damage 内置的 "被攻击解除 Frost"
		// 逻辑会立即 detach 刚施加的 Frost，导致冻结实际从未生效。
		// 解法：参考 WandOfFrost.onHit，用一次性 FlavourBuff（actPriority=VFX_PRIO）延后
		// 施加 Chill/Frost，使真正的施加时机落在 enemy.damage 之后。
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){

			final int lvl = buffedLvl();
			new FlavourBuff() {
				{
					actPriority = VFX_PRIO;
				}
				public boolean act() {
					if (!target.isAlive()) {
						detach();
						return true;
					}
					Chill chill = target.buff(Chill.class);
					if (chill != null) {
						// 对已有冻伤的敌人：有 (20+2×剩余冻伤回合数)% 概率改为冻结
						int chance = 20 + 2 * Math.round(chill.cooldown());
						if (Random.Int(100) < chance) {
							Buff.detach(target, Chill.class);
							Buff.affect(target, Frost.class, 10f);
						} else {
							// 未触发冻结则照常叠加冻伤
							Buff.affect(target, Chill.class, 2 + lvl);
						}
					} else {
						Buff.affect(target, Chill.class, 2 + lvl);
					}
					detach();
					return true;
				}
			}.attachTo(defender);

		}

		return damage;
	}

}
