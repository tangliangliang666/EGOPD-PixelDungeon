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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBodyReinforce;

/**
 * 指环技艺「强化」（tier 1）：打开强化窗口，将素材数值注入人体派作品。
 * <p>原大师指环 AC_REINFORCE 按钮功能的技艺化封装。</p>
 */
public class ReinforceArt extends RingMasterArt {

	public static final ReinforceArt INSTANCE = new ReinforceArt();

	private ReinforceArt(){}

	@Override
	public int tier() {
		return 1;
	}

	@Override
	public void onPerform( Hero hero ) {
		MasterRing ring = hero.belongings.getItem( MasterRing.class );
		if (ring == null){
			//防御：技艺入口依赖大师指环（正常不会发生）
			return;
		}
		GameScene.show( new WndBodyReinforce( ring ) );
	}

	@Override
	public String name() {
		return "强化";
	}

	@Override
	public String shortDesc() {
		return "将素材数值注入人体派作品";
	}

	@Override
	public String desc() {
		return "打开作品强化窗口，将素材的四项数值累加到一件人体派作品上。\n\n"
				+ "强化后作品的强化等级会随武器值实时升降（等级上限 A+）。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_REINFORCE; //上升箭头图案
	}
}
