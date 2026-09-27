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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invulnerability;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 测试用道具：无敌开关（套用圆盾贴图）。
 * <p>使用后永久获得无敌效果（{@link Invulnerability}），再次使用则关闭。由神谕代行者开局携带。</p>
 */
public class InvulnTester extends Item {

	{
		image = ItemSpriteSheet.ROUND_SHIELD; //圆盾贴图
		defaultAction = AC_TOGGLE; //背包内点击/快捷栏直接切换
		unique = true; //每局唯一
		bones = false; //死亡后不进入遗骨掉落
	}

	public static final String AC_TOGGLE = "TOGGLE";

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_TOGGLE);
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_TOGGLE)){
			return hero.buff(Invulnerability.class) != null ? "关闭无敌" : "开启无敌";
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);
		if (action.equals(AC_TOGGLE)){
			if (hero.buff(Invulnerability.class) != null){
				Buff.detach(hero, Invulnerability.class);
				GLog.i("无敌已关闭");
			} else {
				Buff.affect(hero, Invulnerability.class, 99999f); //永久无敌
				GLog.i("获得无敌！");
			}
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
		return "无敌测试器";
	}

	@Override
	public String desc() {
		return "测试用道具。使用后_永久获得无敌_，再次使用则关闭无敌。\n\n（当前套用圆盾贴图）";
	}
}
