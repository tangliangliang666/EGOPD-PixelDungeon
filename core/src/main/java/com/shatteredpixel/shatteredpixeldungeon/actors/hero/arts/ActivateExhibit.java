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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.items.BloodMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 展品技艺「活化」（tier 2，天赋「画廊即时修整」+1 解锁）：
 * 花费 1 个血质素材，选择一只环指自动人偶，使其获得 50 回合的激素涌动（行动速度翻倍）。
 */
public class ActivateExhibit extends RingMasterArt {

	public static final ActivateExhibit INSTANCE = new ActivateExhibit();

	private ActivateExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		if (hero.pointsInTalent( Talent.GALLERY_TOUCHUP ) < 1){
			return false;
		}
		if (collectDolls().isEmpty()){
			return false;
		}
		//需要 1 个血质素材作为消耗（2026-09-06 调整）
		BloodMaterial mat = hero.belongings.getItem( BloodMaterial.class );
		return mat != null && mat.quantity() >= 1;
	}

	@Override
	public void onPerform( Hero hero ) {
		selectDoll( "选择要活化的展品", new DollCallback() {
			@Override
			public void onDollSelected( RingfingerAutomaton doll ) {
				//消耗 1 个血质素材
				BloodMaterial mat = hero.belongings.getItem( BloodMaterial.class );
				if (mat == null || mat.quantity() < 1){
					GLog.w( "没有血质素材。" );
					return;
				}
				mat.quantity( mat.quantity() - 1 );
				if (mat.quantity() <= 0){
					//detachAll 会递归搜索背包与素材箱，移除数量归零的堆叠
					mat.detachAll( hero.belongings.backpack );
				}
				Item.updateQuickslot();

				Buff.prolong( doll, Haste.class, 50f );

				hero.spendAndNext( Actor.TICK );
				GLog.p( "环指自动人偶被_活化_了，齿轮飞转，它的动作变得无比迅速！" );
			}
		} );
	}

	@Override
	public String name() {
		return "活化";
	}

	@Override
	public String shortDesc() {
		return "花费_1_个血质素材，使一只展品获得_50回合_激素涌动";
	}

	@Override
	public String desc() {
		return "画廊导师为展品上紧发条，注入活化的魔力。\n\n"
				+ "花费_1_个血质素材，选择一只场上的环指自动人偶，使其获得_50回合_的_激素涌动_效果（行动速度翻倍）。\n\n"
				+ "需要天赋「画廊即时修整」_+1_。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_ACTIVATE; //活化图案（指挥图标右侧）
	}
}
