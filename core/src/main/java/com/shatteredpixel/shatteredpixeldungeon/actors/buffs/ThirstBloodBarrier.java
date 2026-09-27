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
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.BloodFeastChalice;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;

/**
 * 「渴望」把流血伤害转化成的奥术屏障（2026-09-20）。
 *
 * <h3>它和原版 Barrier 的关系</h3>
 * <p>行为**逐字节沿用 {@link Barrier}**（每约 20 回合衰减 1 点、耗尽即散、图标与描述都不变），
 * 只多一件事：每次衰减都会问一句「英雄身上有没有装备血宴圣杯」。装了，就把衰减量的
 * {@link #HEAL_DIVISOR} 分之一折算成回血；没装，一个分支都不走 —— 与原版完全一致。
 * 所以 {@code Char.damage} 里那条「渴望把流血转成护盾」的路径**不需要分两条**，
 * 统一挂本类即可。</p>
 *
 * <h3>为什么要单独一个类，而不是在 Barrier 里加判断</h3>
 * <p>Barrier 是全游戏共用的通用护盾（格挡、矮人王、圣职者升华形态……）。把「血宴回血」写进
 * Barrier，等于给每一个来源的护盾都加上回血，而且必须在那里猜「这个护盾是不是渴望造成的」——
 * 猜不出来。换成一个专属子类，语义就是自明的：**只有渴望造出来的护盾会回血。**</p>
 *
 * <h3>为什么要有 healPool 零头池</h3>
 * <p>衰减一次只有 1 点，1/5 就是 0.2 —— 直接取整的话每次都被抹成 0，效果等于不存在。
 * 所以先把零头攒着，攒满 1 点再回。</p>
 *
 * <h3>已知代价：本类与「普通护盾」会分成两份（2026-09-20 复核后如实记录）</h3>
 * <p>{@code Char.buff(Class)} 用的是**精确类匹配**（{@code b.getClass() == c}，见该方法上方的注释
 * 「Not just assignable」），**不是** {@code isInstance}。于是当英雄身上同时存在本类与普通
 * {@code Barrier} 时（例如先被流血转化出护盾、随后又喝了护盾药剂）：</p>
 * <ul>
 *   <li>{@code Buff.affect(hero, Barrier.class)} 会**新建**一个普通 Barrier 而不是并入本类
 *       ⇒ buff 栏出现**两个 ARMOR 图标**（{@code BuffIndicator} 按 buff **实例**建图标，不合并）；</li>
 *   <li>所有 {@code hero.buff(Barrier.class)} 的判定**都看不见本类**，例如
 *       {@code Dewdrop} / {@code Waterskin} 读取已有护盾量来限量、{@code Blocking} 与
 *       {@code NarcissusShield.fx} 的「还有没有护盾」判断、{@code Barrier.fx} 摘除 SHIELDED 状态。</li>
 * </ul>
 * <p>这些后果都**轻微**：只会多显示一个图标、或让上面那几处判定多给一点护盾，不会崩、
 * 也不会让本类的回血失效。**伤害吸收不受影响** —— {@code ShieldBuff.processDamage} 走的是
 * {@code target.buffs(ShieldBuff.class)}，那是 {@code isInstance}，普通护盾与本类会被一起算进伤害池。</p>
 * <p>（同一类问题在本 mod 早已存在：{@code TearShield}/{@code CheatGuard}/{@code NarcissusShield}
 * 都不是 {@code Barrier} 的子类，却与 Barrier 并存，上面那几处判定对它们同样「看不见」。）</p>
 * <p>替代方案是**不建子类**：改为在 {@code Barrier.act()} 里开一个衰减钩子，并给「渴望充入的
 * 护盾余量」记账。那样能消掉上述全部现象，但要把血宴逻辑塞进**全游戏共用的热路径**，且伤害
 * 分摊之后分不清池子里哪一份才是渴望的份额。当前选择的是**语义精确 + 改动局部**这一侧。</p>
 */
public class ThirstBloodBarrier extends Barrier {

	/** 衰减量折算回血的比例：每 5 点衰减恢复 1 点血量。 */
	public static final int HEAL_DIVISOR = 5;

	//不足 1 点的零头先攒着，否则每次取整都会把 0.2 抹掉，效果等于不存在
	private float healPool = 0f;

	@Override
	public boolean act() {
		//先把挨打对象存下来：Barrier.act() 在护盾耗尽时会自我 detach，而 detach() 会把 target 置空
		Char carrier = target;
		int before = shielding();

		boolean result = super.act();

		int lost = before - shielding();
		//未装备血宴圣杯时一个分支都不走 ⇒ 与原版 Barrier 完全一致
		if (lost > 0 && carrier != null && BloodFeastChalice.linkActive(carrier)) {
			healPool += lost / (float) HEAL_DIVISOR;

			int heal = (int) healPool;
			//满血时先不花零头池（留着下次掉血再回）
			if (heal > 0 && carrier.HP < carrier.HT) {
				heal = Math.min(heal, carrier.HT - carrier.HP);
				healPool -= heal;
				int healed = carrier.heal( heal );
				if (healed > 0 && carrier.sprite != null) {
					carrier.sprite.showStatusWithIcon(CharSprite.POSITIVE,
							Integer.toString(healed), FloatingText.HEALING);
				}
			}
		}

		return result;
	}

	private static final String HEAL_POOL = "heal_pool";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HEAL_POOL, healPool);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		healPool = bundle.getFloat(HEAL_POOL);
	}
}
