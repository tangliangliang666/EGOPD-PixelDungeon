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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * 指令计时器（挂在神谕代行者身上，无图标）。
 * <p>每 {@link #MIN_TURNS}~{@link #MAX_TURNS} 回合触发一次「待接指令」：
 * 信息栏与头顶显示 *哔哔*（淡蓝），获得 {@link PendingInstruction}（5 回合），
 * 并暂停英雄的自动行为（自动寻路/休息，见 {@link Hero#interrupt()}）。</p>
 */
public class InstructionTimer extends Buff {

	{
		//核心常驻机制：十字架复活后保留（否则复活后不再接收指令）
		revivePersists = true;
	}

	public static final int MIN_TURNS = 150;
	public static final int MAX_TURNS = 200;

	private int turnsLeft = rollInterval();

	//从心所欲 +1：指令出现速度倍率（0.5=加速一倍、2=减速一半），从下次指令起生效
	private float speedMultiplier = 1f;

	//设置指令出现速度倍率（0.5 加速 / 2 减速），下次指令起生效
	public static void setSpeedMultiplier(Hero hero, float multi){
		InstructionTimer timer = hero.buff(InstructionTimer.class);
		if (timer != null) timer.speedMultiplier = multi;
	}

	//急促蜂鸣：+1/+2/+3 各缩短 50 回合触发间隔（150~200 → 100~150 → 50~100 → 0~50）
	private int rollInterval(){
		int min = MIN_TURNS, max = MAX_TURNS;
		if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.RAPID_BUZZ)){
			switch (Dungeon.hero.pointsInTalent(Talent.RAPID_BUZZ)){
				case 1: min = 100; max = 150; break;
				case 2: min = 50;  max = 100; break;
				case 3: min = 0;   max = 50;  break;
			}
		}
		return min + Random.Int(max - min + 1);
	}

	@Override
	public boolean act() {
		if (--turnsLeft <= 0){
			trigger();
			turnsLeft = Math.round(rollInterval() * speedMultiplier); //重置（按倍率）
			//防御：倍率异常或掷出 0 时，避免重置为 0 导致每回合连续触发
			if (turnsLeft <= 0) turnsLeft = 1;
		}
		spend(TICK);
		return true;
	}

	private void trigger(){
		if (target instanceof Hero){
			Hero hero = (Hero) target;

			GLog.c("*哔哔*");
			if (hero.sprite != null){
				hero.sprite.showStatus(Instruction.LIGHT_BLUE, "*哔哔*");
			}
			Instruction.playBuzz();

			//获得 5 回合的待接指令
			Buff.affect(hero, PendingInstruction.class, PendingInstruction.DURATION);

			//命运弃子分支：*哔哔*时烧灼的伤口 +1；指令成瘾联动（触发点改为此时）
			if (hero.subClass == HeroSubClass.FATE_FORSAKEN){
				Buff.affect(hero, ScorchingWound.class).gainStack();
				Talent.onNewInstruction(hero);
			}

			//暂停自动行为（自动寻路/休息）
			hero.interrupt();
		}
	}

	private static final String TURNS_LEFT = "turnsLeft";
	private static final String SPEED_MULTIPLIER = "speedMultiplier";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(TURNS_LEFT, turnsLeft);
		bundle.put(SPEED_MULTIPLIER, speedMultiplier);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		turnsLeft = bundle.getInt(TURNS_LEFT);
		speedMultiplier = bundle.getFloat(SPEED_MULTIPLIER);
		//旧存档缺失 speedMultiplier 键时 getFloat 返回 0，会导致指令每回合触发（*哔哔*刷屏）。
		//钳制回默认倍率 1。
		if (speedMultiplier <= 0f){
			speedMultiplier = 1f;
		}
	}

}
