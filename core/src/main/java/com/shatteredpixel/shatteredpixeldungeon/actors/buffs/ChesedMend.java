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
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;

/**
 * 考验 CHESED（慈悲）：「每 5 回合恢复 10% 生命上限」那只计时器。
 *
 * <p>它是一只**常驻**buff（不限期），由 {@code Trials.bindMobPassives} 在 {@code Mob.onAdd()}
 * 里幂等重挂 —— 挂在 mob 身上而不是写进 {@code Mob.act()}，是因为一部分单位的 {@code act()}
 * 覆写不调 {@code super.act()}（CrystalSpire、Masterpiece、MobSpawner、Sheep 等），
 * 而 buff 有自己的回合，谁都漏不掉；另一个好处是它会随存档保存（{@code Char.storeInBundle}
 * 存的就是 buffs 列表），读档后继续沿用原来的计时。</p>
 *
 * <p>⚠️ **EXP=0 的敌方单位**（2026-09-24）一律不进回血：这类怪物打死了也不给经验，
 * 回血只会把战斗拖得毫无意义。挂 buff 时的过滤在 {@code Trials.bindMobPassives}
 * 已经做完；这里再判一次是防御性的，万一别处手动 {@code Buff.affect} 仍会按本判据失效。</p>
 *
 * <p>计时规则严格照需求：「**血量不满时才开始计时**」。满血（或已被腐蚀成友方、或已经不在
 * 敌方阵营、或 EXP=0）时计数直接归零，下次受伤从第一回合重新数；只有连续
 * {@link Trials#CHESED_MEND_TURNS} 个回合都处于「不满血的敌方且 EXP>0」的单位才会回一次血。</p>
 *
 * <p>回血量**不写死数值**：一律由 {@link Trials#CHESED_MEND_PERCENT} 乘当前生命上限现算，
 * 口径集中在 {@code Trials} 的常量里，后续要调只改那一处。</p>
 */
public class ChesedMend extends Buff {

	{
		//⚠️ 只能是 POSITIVE / NEUTRAL。Mob.Sleeping 会把 type == NEGATIVE 的 buff 当成
		//「被打醒」的信号（见 Mob.Sleeping.act 的第一个循环），标成 NEGATIVE 会让全图怪物
		//一入场就醒来并进入警戒 —— 那是灾难性的副作用。
		type = buffType.POSITIVE;
	}

	/** 距离下一次回血还差几回合。只在「血量不满」时推进；满血即归零重计。 */
	private int turns = 0;

	@Override
	public boolean act() {

		Mob mob = (target instanceof Mob) ? (Mob) target : null;

		//只认敌方单位（与 Trials.modifyMobHT 同判据）。每次现判 alignment 而不是挂 buff 时判一次：
		//被腐蚀、被招募、被魅惑成友方之后会立刻停止回血，变回敌方后再继续。
		if (mob == null
				|| !Dungeon.isTrialled( Trials.CHESED )
				|| mob.alignment != Char.Alignment.ENEMY
				|| !mob.isAlive()
				|| mob.HP >= mob.HT) {
			//满血（或已不再适用）：按需求「不满才开始计时」，这里就把计时清掉。
			turns = 0;
			spend( TICK );
			return true;
		}

		if (++turns >= Trials.CHESED_MEND_TURNS) {
			turns = 0;

			//回血量现算：生命上限 × 百分比，四舍五入；至少 1 点（极端小血量单位不至于回 0），
			//且不超过缺失的血量（不回溢出）。
			int heal = Math.min( mob.HT - mob.HP,
					Math.max( 1, Math.round( mob.HT * Trials.CHESED_MEND_PERCENT ) ) );

			if (heal > 0) {
				//禁疗：唯一回血出口 Char.heal(int)，被禁疗时返回 0
				int healed = mob.heal( heal );
				//崩落动画/换场景时 sprite 可能已经摘掉，这里必须防 null。
				if (healed > 0 && mob.sprite != null) {
					mob.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );
				}
			}
		}

		spend( TICK );
		return true;
	}

	private static final String TURNS = "turns";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TURNS, turns );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		turns = bundle.getInt( TURNS );
	}
}
