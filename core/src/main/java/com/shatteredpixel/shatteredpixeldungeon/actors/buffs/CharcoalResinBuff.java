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
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 「焦炭松脂」涂层（buff 图标帧 109，用户已绘制）。
 * <p>效果：临时_烈焰_附魔覆盖原本附魔；攻击额外附带 10% 原本伤害的火焰伤害。
 * 由「焦炭松脂」与「散装焦炭松脂」在使用时施加。
 */
public class CharcoalResinBuff extends ResinCoatingBuff {

	@Override
	public int icon() {
		return BuffIndicator.CHARCOAL_RESIN;
	}

	@Override
	public Weapon.Enchantment enchant() {
		return new Blazing();
	}

	@Override
	public float bonusDamagePercent() {
		return 0.10f;
	}

	@Override
	protected void onBonusDamageFX(Char defender) {
		if (defender.sprite != null && defender.isAlive()){
			defender.sprite.emitter().burst(FlameParticle.FACTORY, 4);
		}
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}
}
