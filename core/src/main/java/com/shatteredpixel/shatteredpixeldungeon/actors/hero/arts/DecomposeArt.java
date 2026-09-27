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
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBodyDecompose;

/**
 * 指环技艺「分解」（tier 1）：将非人体派近战武器拆解为硬质素材（N 阶 → N-1 个）。
 * <p>原大师指环 AC_DECOMPOSE 按钮功能的技艺化封装。</p>
 */
public class DecomposeArt extends RingMasterArt {

	public static final DecomposeArt INSTANCE = new DecomposeArt();

	private DecomposeArt(){}

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
		GameScene.show( new WndBodyDecompose( ring ) );
	}

	@Override
	public String name() {
		return "分解";
	}

	@Override
	public String shortDesc() {
		return "将武器拆解为硬质素材";
	}

	@Override
	public String desc() {
		return "打开分解窗口，将非人体派的近战武器拆解为硬质素材（N 阶武器 → N-1 个硬质素材）。\n\n"
				+ "1 阶武器无产出，不可分解。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_DECOMPOSE; //断裂之剑图案
	}
}
