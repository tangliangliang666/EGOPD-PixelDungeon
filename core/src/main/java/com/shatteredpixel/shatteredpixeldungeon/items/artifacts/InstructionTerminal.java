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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Instruction;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.InstructionTimer;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PendingInstruction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 指令终端：神谕代行者的专属物品。
 * <p>不占用任何装备栏位（非神器/非杂项栏），放在背包中即可正常使用；
 * 无法丢弃。按钮包括：「谨遵指令之意」（仅在持有 {@link PendingInstruction} 待接指令时可用，
 * 接取并触发指令任务）；从心所欲天赋提供的调节按钮（加速/减速指令出现速度、刷新当前指令）。</p>
 */
public class InstructionTerminal extends Item {

	public static final String AC_INSTRUCTION = "INSTRUCTION";
	//从心所欲：调节指令速度 / 刷新指令
	public static final String AC_SPEED_UP   = "SPEED_UP";
	public static final String AC_SLOW_DOWN  = "SLOW_DOWN";
	public static final String AC_REFRESH    = "REFRESH";

	{
		image = ItemSpriteSheet.INSTRUCTION_TERMINAL; //自定义贴图

		defaultAction = AC_INSTRUCTION;

		//唯一物品：不可被嬗变卷轴选为嬗变对象（见 ScrollOfTransmutation.usableOnItem）
		unique = true;
		//死亡后不进入遗骨掉落
		bones = false;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	//按钮：谨遵指令之意（仅待接时；命运弃子分支无法接取指令，不显示）+ 从心所欲的调节按钮（对应天赋加点时出现）
	//不提供丢弃/投掷按钮：指令终端无法被丢弃
	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = new ArrayList<>();
		//命运弃子：拒绝接收任何指令，不显示接取按钮
		if (hero.buff(PendingInstruction.class) != null && hero.subClass != HeroSubClass.FATE_FORSAKEN){
			actions.add(AC_INSTRUCTION);
		}
		//从心所欲 +1：加速 / 减速（指令出现速度）
		if (hero.hasTalent(Talent.FREE_WILL) && hero.pointsInTalent(Talent.FREE_WILL) >= 1){
			actions.add(AC_SPEED_UP);
			actions.add(AC_SLOW_DOWN);
		}
		//从心所欲 +2：刷新当前执行的指令（每次指令任务限一次）
		if (hero.hasTalent(Talent.FREE_WILL) && hero.pointsInTalent(Talent.FREE_WILL) >= 2){
			Instruction instruction = hero.buff(Instruction.class);
			if (instruction != null && !instruction.refreshed){
				actions.add(AC_REFRESH);
			}
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_INSTRUCTION)){
			return "谨遵指令之意";
		} else if (action.equals(AC_SPEED_UP)){
			return "加速";
		} else if (action.equals(AC_SLOW_DOWN)){
			return "减速";
		} else if (action.equals(AC_REFRESH)){
			return "刷新";
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action ) {
		super.execute(hero, action);

		if (action.equals(AC_INSTRUCTION)){

			//命运弃子：无法接取指令。按钮已被禁用，但快捷栏点击/物品栏点击仍会走到这里，必须在此拦截。
			if (hero.subClass == HeroSubClass.FATE_FORSAKEN){
				GLog.w("你不再理会它的哔哔声响");
				return;
			}

			if (hero.buff(PendingInstruction.class) == null){
				GLog.w("当前没有待接取的指令");
				return;
			}

			//消除待接指令（手动移除，不触发未接取的业惩罚）
			Buff.detach(hero, PendingInstruction.class);

			//获得一则指令讯息并触发指令任务
			Instruction.startInstruction(hero);

			hero.spendAndNext(hero.cooldown()); //占用 1 回合行动
		} else if (action.equals(AC_SPEED_UP)){
			//指令出现速度加快（周期减半），从下次指令起生效
			InstructionTimer.setSpeedMultiplier(hero, 0.5f);
			GLog.i("指令出现速度加快（下次指令起生效）");
			hero.spendAndNext(hero.cooldown());
		} else if (action.equals(AC_SLOW_DOWN)){
			//指令出现速度减慢（周期翻倍），从下次指令起生效
			InstructionTimer.setSpeedMultiplier(hero, 2f);
			GLog.i("指令出现速度减慢（下次指令起生效）");
			hero.spendAndNext(hero.cooldown());
		} else if (action.equals(AC_REFRESH)){
			Instruction instruction = hero.buff(Instruction.class);
			if (instruction == null || instruction.refreshed){
				GLog.w("当前无法刷新指令");
				return;
			}
			instruction.detach();
			Instruction.startInstruction(hero); //改为执行另一个指令
			//刷新限次按「指令期间」计算：新指令继承「已刷新」状态，防止无限刷新
			Instruction newInstruction = hero.buff(Instruction.class);
			if (newInstruction != null){
				newInstruction.refreshed = true;
			}
			hero.spendAndNext(hero.cooldown());
		}
	}

	@Override
	public String name() {
		return "指令终端";
	}

	@Override
	public String desc() {
		return "小巧的银白色物件，带有黑色的显示屏。作为指令之意传达的终端，在有需要执行的指令时，会发出刺耳的哔哔声提醒持有者。";
	}

}
