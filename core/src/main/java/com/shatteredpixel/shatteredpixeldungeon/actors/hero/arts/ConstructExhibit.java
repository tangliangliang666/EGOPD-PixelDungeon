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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 展品技艺「构建」（tier 2，画廊导师专精）：消耗素材召唤新的展品。
 * <p>当前仅「环指自动人偶」（配方见 {@link RingfingerAutomaton} REQ_* 常量），
 * 后续扩充新召唤物时在此追加选择步骤（列表或多选窗）。</p>
 */
public class ConstructExhibit extends RingMasterArt {

	public static final ConstructExhibit INSTANCE = new ConstructExhibit();

	private ConstructExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		return RingfingerAutomaton.hasMaterials();
	}

	@Override
	public void onPerform( Hero hero ) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				if (!Dungeon.level.heroFOV[cell]){
					GLog.w("召唤位置必须在视野内。");
					return;
				}
				if (!Dungeon.level.passable[cell] || Actor.findChar(cell) != null){
					GLog.w("召唤位置必须是没有单位的空地。");
					return;
				}
				summon(cell);
			}

			@Override
			public String prompt() {
				return "选择召唤物出现的位置";
			}
		});
	}

	//消耗材料并生成友方召唤物（spawn 模式参考 Ratmogrify 的盟友鼠群）
	private void summon(int cell) {
		if (!RingfingerAutomaton.consumeMaterials()) return;

		RingfingerAutomaton doll = new RingfingerAutomaton();
		GameScene.add(doll);
		ScrollOfTeleportation.appear(doll, cell);

		Dungeon.hero.spendAndNext(Actor.TICK);
		GLog.p("环指自动人偶被摆上了展位！");
	}

	@Override
	public String name() {
		return "构建";
	}

	@Override
	public String shortDesc() {
		return "消耗_" + RingfingerAutomaton.reqText() + "_，召唤新的展品";
	}

	@Override
	public String desc() {
		return "画廊导师以人体派素材搭建展品。\n\n"
				+ "构建_环指自动人偶_需要消耗_" + RingfingerAutomaton.reqText() + "_（素材箱内的素材会自动计入）。\n\n"
				+ "构建后的展品会服从画廊导师的指挥，跟随跨楼层移动。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_CONSTRUCT; //自动人偶头部图案
	}
}
