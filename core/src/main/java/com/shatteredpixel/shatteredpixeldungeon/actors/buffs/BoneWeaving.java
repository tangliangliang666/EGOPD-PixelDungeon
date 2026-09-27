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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「骨骸编织」buff：环指大师「骨骸编织」天赋消耗骨质/肉质素材时获得。
 *
 * <h3>它是一个「没有层数」的护盾 buff</h3>
 * <p>本类<b>不保存任何护甲数值</b>：素材被消耗的那一刻不掷点，护甲上界<b>实时</b>取持有者当前等级
 * （见 {@link #maxArmor(Char)}），每次结算减伤时再由 {@code Char.drRoll()} 在
 * {@code [0, maxArmor]} 上掷一次（{@code Random.NormalIntRange}）——与 {@link Barkskin}
 * 完全同一套口径。好处是升级立刻变强，且不需要任何存档字段。</p>
 * <p>随之而来的是图标角落里<b>没有层数</b>：本类<b>不覆写</b> {@code iconTextDisplay()}，
 * 走 {@link FlavourBuff} 的默认实现（显示剩余回合数）；时长观感由
 * {@link #iconFadePercent()} 的灰色进度弧 ＋ {@link #desc()} 表达。</p>
 *
 * <h3>时长（与层数无关）</h3>
 * <p>+1 消耗骨质素材 50 回合（仅骨质可用），+2 消耗骨质/肉质素材 100 回合。续期走
 * {@code Buff.prolong} → {@code Actor.postpone}，语义是「<b>只会变成更长的那个</b>」：
 * 既不叠加、也不会被更短的覆盖。所以 {@link #maxDuration}（进度弧基准）同样只能取 {@code max}，
 * 否则先用 +2 再用 +1 会让弧算出「已过期」。</p>
 */
public class BoneWeaving extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;
	}

	/** 进度弧基准：本 buff 被施加（或续期到更长）时的总时长，不是层数。 */
	public float maxDuration = 0;

	/**
	 * 护甲上界 = 持有者当前等级（<b>实时</b>读取，不在消耗素材时冻结）。
	 * <p>非英雄返回 0 —— 本 buff 只有英雄拿得到，该分支纯属防御。</p>
	 */
	public static int maxArmor( Char ch ) {
		return ch instanceof Hero ? ((Hero) ch).lvl : 0;
	}

	@Override
	public int icon() {
		return BuffIndicator.BONE_WEAVING;
	}

	@Override
	public float iconFadePercent() {
		if (maxDuration <= 0) return 0;
		return Math.max( 0, 1f - visualcooldown() / maxDuration );
	}

	@Override
	public String name() {
		return "骨骸编织";
	}

	@Override
	public String desc() {
		return "环指大师将骨质或肉质素材编织入躯体，获得临时护甲加成。\n\n"
				+ "当前护甲加成：_0~" + maxArmor( target ) + "_\n\n"
				+ "剩余时长：" + dispTurns( visualcooldown() ) + "回合";
	}

	private static final String MAX_DURATION = "max_duration";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( MAX_DURATION, maxDuration );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		maxDuration = bundle.getFloat( MAX_DURATION );
	}
}
