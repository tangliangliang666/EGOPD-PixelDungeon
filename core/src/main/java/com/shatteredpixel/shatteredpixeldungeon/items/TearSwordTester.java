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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 测试用道具：泪剑叠层器（套用长剑贴图）。
 * <p>「使用」获得 1 层 {@link TearSword}（可反复使用以叠满 {@link TearSword#MAX_STACKS} 层）；
 * 另提供「清空」动作一次性移除，方便反复观察 0~上限层数的特效差异。</p>
 * <p>纯测试道具：不设掉落/商店/开局获得渠道，仅能通过调试窗口调出——
 * 本类位于 items 根包，会被调试控制台「杂项」的浅层扫描自动收录，无需额外注册。</p>
 */
public class TearSwordTester extends Item {

	public static final String AC_GAIN  = "GAIN";
	public static final String AC_CLEAR = "CLEAR";

	{
		image = ItemSpriteSheet.LONGSWORD; //临时图标：长剑（调试期占位，日后可换专属帧）
		defaultAction = AC_GAIN;           //背包内点击/快捷栏直接获得 1 层
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_GAIN);
		if (TearSword.stacks(hero) > 0) {
			actions.add(AC_CLEAR);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_GAIN)) {
			return "获得1层泪剑";
		} else if (action.equals(AC_CLEAR)) {
			return "清空泪剑";
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_GAIN)) {
			TearSword.addStacks(hero, 1);
			GLog.i("泪剑 +1（当前 %d/%d 层）", TearSword.stacks(hero), TearSword.MAX_STACKS);
			hero.spendAndNext(hero.cooldown());

		} else if (action.equals(AC_CLEAR)) {
			TearSword.setStacks(hero, 0);
			GLog.i("泪剑已清空");
			hero.spendAndNext(hero.cooldown());
		}
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public String name() {
		return "泪剑测试器";
	}

	@Override
	public String desc() {
		return "测试用道具。使用后获得_1层泪剑_，可反复使用叠加（上限 " + TearSword.MAX_STACKS + " 层）；"
				+ "已有层数时可选择「清空泪剑」。\n\n（泪剑目前只做特效，无任何数值效果；当前套用长剑贴图）";
	}
}
