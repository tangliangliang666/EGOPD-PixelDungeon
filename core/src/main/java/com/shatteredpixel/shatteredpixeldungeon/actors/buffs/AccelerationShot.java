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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.valencina.ValencinaSfx;
import com.shatteredpixel.shatteredpixeldungeon.items.AccelerationRound;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 「加速弹」buff（拇指 前二老板 T2「加速弹药」，图标 ID 102）。
 *
 * <p>由物品「加速弹」的「备弹」动作开启/关闭：开启期间攻击延迟减半，
 * 且每次攻击结束后消耗 1 发加速弹；背包中没有加速弹、或再次使用加速弹时解除本 buff。</p>
 *
 * <p>独立顶层类（非 artifact / 能力内部类），保证存档经 Bundle 反射可正常重建。</p>
 */
public class AccelerationShot extends Buff {

	{
		type = buffType.POSITIVE;
	}

	@Override
	public int icon() {
		return BuffIndicator.ACCEL_AMMO;
	}

	@Override
	public float iconFadePercent() {
		return 0f; //常驻显示（直到取消或弹药耗尽）
	}

	@Override
	public boolean act() {
		//外部因素（卖出/丢弃/炼金等）导致身上一发加速弹都没有时自动解除
		if (target instanceof Hero && AccelerationRound.count((Hero) target) <= 0){
			detach();
			BuffIndicator.refreshHero();
		}
		spend( TICK );
		return true;
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc");
	}

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 物品「备弹」动作：有则解除（卸弹），无则装弹开启（需背包中有加速弹）。 */
	public static void toggle( Hero hero ){
		if (hero == null) return;

		if (hero.buff(AccelerationShot.class) != null){
			Buff.detach(hero, AccelerationShot.class);
			BuffIndicator.refreshHero();
			GLog.i( Messages.get(AccelerationRound.class, "unloaded") );
		} else {
			if (AccelerationRound.count(hero) <= 0){
				GLog.w( Messages.get(AccelerationRound.class, "no_ammo") );
				return;
			}
			Buff.affect(hero, AccelerationShot.class);
			BuffIndicator.refreshHero();
			GLog.i( Messages.get(AccelerationRound.class, "loaded") );
		}
	}

	/** 每次攻击结束后调用：消耗 1 发；耗尽后解除本 buff。 */
	public static void consumeOnAttack( Hero hero ){
		if (hero == null) return;
		if (hero.buff(AccelerationShot.class) == null) return;

		//换弹声：延后约 0.4 秒播放，避开本次攻击的音效（否则两组声音会糊在一起）
		ValencinaSfx.playReload();

		if (AccelerationRound.consumeOne(hero)){
			if (AccelerationRound.count(hero) <= 0){
				Buff.detach(hero, AccelerationShot.class);
				BuffIndicator.refreshHero();
				GLog.w( Messages.get(AccelerationRound.class, "depleted") );
			}
		}
	}

	//保证存档安全（本类无额外字段，仅按惯例补全空实现，无实际数据）
}
