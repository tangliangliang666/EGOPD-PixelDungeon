/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon / Easily-Sprouted-Pixel-Dungeon
 * Copyright (C) 2015-2018 dachhack / zay448345045
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

import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「孢子再生」buff —— 移植自 Easily Sprouted PD 的 {@code actors/buffs/BerryRegeneration}
 * （源名 Regenerating / 生命恢复；因本作已有「生命恢复」{@link LifeRegen}，此处改用现名）。
 * <p>
 * 施加时把「剩余回合数」设为施加者的满血量（英雄或怪物的 HT），之后每回合回复
 * {@code 1 + regenleft/25}（整除）点生命、剩余回合数递减 1，递减到 0 自动消失。
 * 因回复量随剩余回合数同步下降，总回复量约等于 HT 的 2 倍左右，是一份「慢速大回复」。
 * <p>
 * 源仓库里 {@code BlueMilk} / {@code PixieParasol} 会把它同时施加给英雄**和全层敌对怪物**
 * （怪物也能回血是这两种蘑菇的代价）。本类保持同一套语义，只把直接改 {@code HP} 的写法
 * 换成 {@link com.shatteredpixel.shatteredpixeldungeon.actors.Char#heal(int)}，
 * 以便正确受本作的「禁疗」（{@link HealBlock}，洛伊德护符）等钩子约束。
 */
public class BerryRegeneration extends Buff {

	{
		//正面状态：务必不要标 NEGATIVE —— 本作 Buff.type 标 NEGATIVE 会让全图怪物立刻醒来
		type = buffType.POSITIVE;
	}

	/** 剩余回复回合数（施加时 = 施加者的 HT）。 */
	private int regenleft = 0;

	private static final String REGENLEFT = "regenleft";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( REGENLEFT, regenleft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		regenleft = bundle.getInt( REGENLEFT );
	}

	public int level() {
		return regenleft;
	}

	/** 只升不降：重复施加时取较大值（与源仓库一致）。 */
	public void level( int value ) {
		if (regenleft < value) {
			regenleft = value;
		}
	}

	//本 buff 尚无专属图标，按本作惯例「借原版帧」：与 LifeRegen 共用 HEALING 帧表示回血。
	//等真画出来了，再在 BuffIndicator 末尾按当时的空帧顺序新增常量并把 icon() 改过来。
	@Override
	public int icon() {
		return BuffIndicator.HEALING;
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", regenleft);
	}

	@Override
	public boolean act() {

		if (!target.isAlive()) {
			detach();
			return true;
		}

		//源仓库公式：回复 1 + regenleft/25。注意这里是**整除**（源代码写的 Math.round(regenleft / 25)
		//在整数除法下是空操作），所以 0~24 回合各回 1 点、25~49 回合各回 2 点，以此类推 —— 照实保留。
		if (target.HP < target.HT) {
			int amount = Math.min( 1 + regenleft / 25, target.HT - target.HP );
			int healed = target.heal( amount );
			if (healed > 0 && target.sprite != null) {
				target.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING );
			}
		}

		spend( TICK );

		if (--regenleft <= 0) {
			detach();
		}

		return true;
	}
}
