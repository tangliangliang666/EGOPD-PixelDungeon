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

import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;

/**
 * 「水仙十字圣剑」两个特殊形态的<b>共同冷却</b>（2026-09-20 建）：芒性的「受击化解」与
 * 荒性的「攻击扩张」共用这一个标记，冷却 <b>60 回合</b>。
 *
 * <p>为什么不做成两个 buff：剑同一时刻只可能处于一个形态，两者的冷却不可能同时在跑；
 * 而形态切换本身极其罕见（只在英雄等级跨过 1 / 30 时发生），
 * 所以「一份冷却」在行为上与「各管各的」等价，却少一份要同步的状态。</p>
 *
 * <p>用 {@code FlavourBuff} 当计时器：挂上时给 60 回合的寿命，到点自己 detach；
 * 判定侧只看「英雄身上有没有这个 buff」（{@code NarcissusCrossSwordBase.offCooldown}）。</p>
 */
public class NarcissusCrossCooldown extends FlavourBuff {

	/** 冷却总回合数（与 {@code NarcissusCrossSwordBase.COOLDOWN} 一致）。 */
	public static final float DURATION = 60f;

	@Override
	public int icon() {
		return BuffIndicator.RECHARGING;
	}

	@Override
	public void tintIcon(Image icon) {
		//复用「充能」的沙漏图标，但染成剑的冷白蓝，和黄色的充能 / 绿色的神器充能区分开
		icon.hardlight(0.7f, 0.85f, 1.6f);
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}
}
