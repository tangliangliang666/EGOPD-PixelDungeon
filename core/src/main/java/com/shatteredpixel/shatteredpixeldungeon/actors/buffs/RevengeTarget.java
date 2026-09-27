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

import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;

/**
 * 「报复对象」（中指长兄 T2「永不遗忘」）：被中指长兄记恨上的敌人。
 *
 * <p>受到<b>来自中指长兄的伤害增加 {@link #DAMAGE_BONUS}</b>（+20%）。加成写在
 * {@code Char.damage(int, Object)} 里——那里同时拿得到「承伤者」与「伤害来源」，
 * 于是近战、远程、法术、技能等一切<b>以英雄为源</b>的伤害都会被一并放大。</p>
 *
 * <p><b>没有时限</b>：不覆写 {@code act()}，基类 {@code Actor.diactivate()} 会把 {@code time}
 * 顶到 {@code Float.MAX_VALUE} 从而永不自动分离——毕竟这个天赋叫「永不遗忘」。
 * 同一个敌人只会被打一次标记，所以也没有刷新/叠加的问题（见
 * {@code Talent.onEnemyAttackLanded}）。</p>
 *
 * <p><b>顶层类</b>：挂在怪物身上、会随存档序列化并通过反射重建，不能做成非静态内部类。</p>
 */
public class RevengeTarget extends Buff {

	/** 来自中指长兄的伤害倍率（+20%）。 */
	public static final float DAMAGE_BONUS = 1.2f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	//图标保持 BuffIndicator.NONE：本 buff 挂在「敌人」身上，而 buff 条只画英雄的 buff，
	//没有可用的展示位；上标记时会在敌人头顶弹一次名字作为反馈（Talent.onEnemyAttackLanded）。
	@Override
	public String desc() {
		return Messages.get(this, "desc", Math.round((DAMAGE_BONUS - 1f) * 100f));
	}
}
