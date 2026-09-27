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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ResinCoatingBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.Enchanting;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

import java.util.ArrayList;

/**
 * 松脂消耗品基类（2026-09-08 用户新道具：焦炭松脂 / 散装焦炭松脂 / 黄金松脂 / 散装黄金松脂）。
 * <p>对英雄当前装备的近战武器使用，施加对应 {@link ResinCoatingBuff} 涂层：
 * 临时烈焰/电击附魔覆盖原本附魔，并随攻击附带元素伤害。只能通过炼金配方获得（不入掉落池）。
 * <p>完整松脂使用消耗 1 回合；散装松脂可快速使用（不消耗回合），但持续时间更短。
 */
public abstract class Resin extends Item {

	public static final String AC_USE = "USE";

	//炼金产物：恒为已辨识状态；消耗品可叠加（同类共用一栏，每次使用经 detach 自动扣 1）
	{
		defaultAction = AC_USE;
		stackable = true;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	/** 涂层持续回合数（焦炭 200 / 散装焦炭 75 / 黄金 150 / 散装黄金 50）。 */
	protected abstract float duration();

	/** 是否为散装（散装可快速使用，不消耗回合）。 */
	protected abstract boolean fast();

	/** 本松脂对应的涂层 buff 类型（焦炭/散装焦炭→CharcoalResinBuff，黄金/散装黄金→GoldenResinBuff）。 */
	protected abstract Class<? extends ResinCoatingBuff> coating();

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		actions.add(AC_USE);
		return actions;
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (action.equals(AC_USE)) {

			if (!hero.isAlive()) return;

			//主武器槽类型为 KindOfWeapon（该 fork 中 Weapon/MissileWeapon 均继承自它），
			//涂层仅在 Weapon 分支的 proc 中生效，故非 Weapon（含空槽）一律按无武器处理。
			KindOfWeapon kow = hero.belongings.weapon();
			if (!(kow instanceof Weapon)){
				GLog.w(Messages.get(Resin.class, "no_weapon"));
				return;
			}
			Weapon wep = (Weapon)kow;

			//施加涂层：互斥替换另一涂层并刷新为完整时长
			ResinCoatingBuff.coat(hero, coating(), duration());

			detach(hero.belongings.backpack);

			//复现神圣武器的"附魔武器虚影"视觉
			hero.sprite.operate(hero.pos);
			Enchanting.show(hero, wep);
			Sample.INSTANCE.play(Assets.Sounds.READ);

			GLog.i(Messages.get(Resin.class, "apply"));

			Item.updateQuickslot();

			if (!fast()){
				//完整松脂：使用花费 1 回合
				hero.spendAndNext(1f);
			}
		}
	}
}
