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
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBodyCraft;

/**
 * 指环技艺「创作」（tier 1）：打开 3×3 创作窗口，将素材合成为人体派作品。
 * <p>原大师指环 AC_CREATE 按钮功能的技艺化封装。</p>
 */
public class CraftArt extends RingMasterArt {

	public static final CraftArt INSTANCE = new CraftArt();

	private CraftArt(){}

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
		GameScene.show( new WndBodyCraft( ring ) );
	}

	@Override
	public String name() {
		return "创作";
	}

	@Override
	public String shortDesc() {
		return "消耗素材，合成人体派作品";
	}

	@Override
	public String desc() {
		return "打开展品创作窗口，将最多 9 格素材（人体派素材与作品）按四项数值合成为新的人体派作品。\n\n"
				+ "素材的 Weapon 值决定作品等阶，Bone/Meat/Blood 值决定作品的伤害/攻速/精准加成。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_CRAFT; //3×3 合成格图案
	}
}
