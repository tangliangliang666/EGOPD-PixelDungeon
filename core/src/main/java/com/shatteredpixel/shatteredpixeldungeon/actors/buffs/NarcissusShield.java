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

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * 「水仙十字圣剑 · 芒性」的专属护盾：把被吞掉的经验值原样存成护盾（2026-09-20 建）。
 *
 * <p>与 {@link Barrier 奥术屏障} 的唯一、也是关键的区别：<b>不衰减</b>。
 * {@code Barrier.act()} 每回合按剩余量抽掉一点（{@code partialLostShield += min(1, shielding/20)}），
 * 所以它更像「会自己漏的水池」；本护盾的 {@code act()} 只走时间、不动数值——
 * 只会在<b>被伤害消耗</b>时减少，耗尽后照 {@code ShieldBuff.detachesAtZero} 自行脱落。</p>
 *
 * <p>数值入口只有 {@code incShield(int)}（由 {@code NarcissusSwordMang.onExpGained} 调用），
 * 消耗走 {@code ShieldBuff.processDamage}（{@code Char.damage} 里统一结算）。</p>
 *
 * <p><b>离手是否保留</b>：保留。护盾是「已经吃进去的资源」，把剑放下不会把它收回去；
 * 它只会被消耗殆尽。</p>
 */
public class NarcissusShield extends ShieldBuff {

	{
		type = buffType.POSITIVE;
	}

	/**
	 * 只走时间，<b>不扣数值</b>——这就是「不会衰减」。
	 * <p>更贴切的写法是让 {@code FlavourBuff} 那种「到点自己 detach」的语义来做，
	 * 但护盾必须能挂任意久、且只在耗尽时消失，所以这里显式空转。</p>
	 */
	@Override
	public boolean act() {
		spend( TICK );
		return true;
	}

	@Override
	public void fx(boolean on) {
		if (target.sprite == null) return;
		if (on) {
			target.sprite.add(CharSprite.State.SHIELDED);
		} else if (target.buff(Blocking.BlockBuff.class) == null
				&& target.buff(Barrier.class) == null) {
			//别的护盾还在时不能把 SHIELDED 状态摘掉（Barrier 原先只判了格挡，这里连带把奥术屏障也算上）
			target.sprite.remove(CharSprite.State.SHIELDED);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon(Image icon) {
		//与奥术屏障的蓝、格挡的色区分开：偏白的冷光，对应「芒性」的白色辉光
		icon.hardlight(1.4f, 1.4f, 1.6f);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(shielding());
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", shielding());
	}
}
