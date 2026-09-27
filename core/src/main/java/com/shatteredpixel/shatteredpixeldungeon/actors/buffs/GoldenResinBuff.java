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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.SparkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Shocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 「黄金松脂」涂层（buff 图标帧 110，用户已绘制）。
 * <p>效果：临时_电击_附魔覆盖原本附魔；攻击额外附带 20% 原本伤害的电击伤害。
 * 由「黄金松脂」与「散装黄金松脂」在使用时施加。
 */
public class GoldenResinBuff extends ResinCoatingBuff {

	@Override
	public int icon() {
		return BuffIndicator.GOLDEN_RESIN;
	}

	@Override
	public Weapon.Enchantment enchant() {
		return new Shocking();
	}

	@Override
	public float bonusDamagePercent() {
		return 0.20f;
	}

	@Override
	protected void onBonusDamageFX(Char defender) {
		if (defender.sprite != null && defender.isAlive()){
			defender.sprite.flash();
			defender.sprite.emitter().burst(SparkParticle.FACTORY, 6);
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}
}
