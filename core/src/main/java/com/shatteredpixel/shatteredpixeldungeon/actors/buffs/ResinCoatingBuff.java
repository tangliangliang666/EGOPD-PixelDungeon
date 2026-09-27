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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;

/**
 * 松脂涂层 buff 的公共基类（2026-09-08 用户新道具「焦炭/黄金松脂」）。
 * <p>持有期间（时长按游戏回合计）武器的临时附魔（烈焰/电击）会_覆盖_武器原本附魔，
 * 且每次攻击额外附带一定百分比（基于原本伤害）的元素伤害。两种涂层互斥：
 * 施加一种会自动移除另一种；重新涂抹同种会刷新为对应物品的完整时长。
 * <p>涂层由四个松脂消耗品在使用时施加到英雄身上，作用于英雄当前装备的近战武器；
 * 战斗钩子在 {@link Weapon#proc}，武器辉光/附魔判定见 {@link Weapon#glowing} 与
 * {@link Weapon#hasEnchant}。视觉复现牧师「神圣武器」的附魔武器虚影
 * （{@link com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting}）。
 */
public abstract class ResinCoatingBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	/** 本涂层对应的临时武器附魔实例（焦炭→Blazing 烈焰 / 黄金→Shocking 电击）。 */
	public abstract Weapon.Enchantment enchant();

	/** 每次攻击额外附加的原本伤害百分比（焦炭 10% / 黄金 20%）。 */
	public abstract float bonusDamagePercent();

	/** 攻击命中后附加元素伤害（含命中粒子反馈）。baseDamage 为该次攻击的原本伤害。 */
	public void bonusDamage(Char defender, int baseDamage){
		int bonus = Math.round(baseDamage * bonusDamagePercent());
		if (bonus > 0 && defender.isAlive()){
			defender.damage(bonus, this);
		}
		onBonusDamageFX(defender);
	}

	/** 附加元素伤害命中时的粒子反馈（焦炭→火焰 / 黄金→电火花）。 */
	protected abstract void onBonusDamageFX(Char defender);

	/** 为角色施加指定涂层：先移除另一涂层，再以完整 duration 刷新本涂层。 */
	public static <T extends ResinCoatingBuff> T coat(Char target, Class<T> kind, float turns){
		Buff.detach(target, CharcoalResinBuff.class);
		Buff.detach(target, GoldenResinBuff.class);
		return Buff.affect(target, kind, turns);
	}

	/** 返回角色身上当前的涂层buff；无涂层时返回 null。 */
	public static ResinCoatingBuff current(Char target){
		ResinCoatingBuff coating = target.buff(CharcoalResinBuff.class);
		if (coating == null) coating = target.buff(GoldenResinBuff.class);
		return coating;
	}
}
