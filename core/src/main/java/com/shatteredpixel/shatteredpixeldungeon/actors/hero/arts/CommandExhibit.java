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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * 展品技艺「指挥」（tier 2，画廊导师专精）：向场上的展品下达 DirectableAlly 指令
 * （点击敌人=攻击目标 / 点击英雄=跟随 / 点击空地=驻守，同枯萎玫瑰 AC_DIRECT 分流）。
 */
public class CommandExhibit extends RingMasterArt {

	public static final CommandExhibit INSTANCE = new CommandExhibit();

	private CommandExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		return !collectExhibits().isEmpty();
	}

	@Override
	public void onPerform( Hero hero ) {
		ArrayList<RingfingerAutomaton> dolls = collectExhibits();

		if (dolls.isEmpty()){
			GLog.w("场上没有可以指挥的展品。");
			return;
		}
		if (dolls.size() == 1){
			selectCommandCell(dolls.get(0));
			return;
		}
		GameScene.show(new CommandList(dolls));
	}

	/** 收集场上所有存活的环指自动人偶。 */
	private static ArrayList<RingfingerAutomaton> collectExhibits() {
		ArrayList<RingfingerAutomaton> dolls = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m instanceof RingfingerAutomaton && m.isAlive()){
				dolls.add((RingfingerAutomaton) m);
			}
		}
		return dolls;
	}

	/** 进入指令选格：点击敌人=攻击目标，点击英雄=跟随，点击空地=驻守（directTocell 分流）。 */
	private static void selectCommandCell(final RingfingerAutomaton doll) {
		GameScene.selectCell(new CellSelector.Listener() {
			@Override
			public void onSelect(Integer cell) {
				if (cell == null) return;
				doll.directTocell(cell);
			}

			@Override
			public String prompt() {
				return "\"展品应做什么？\"（点击敌人=攻击 / 点击自己=跟随 / 点击空地=驻守）";
			}
		});
	}

	/** 指挥对象选择列表：场上存在多只人偶时使用（按钮显示生命以区分）。 */
	public static class CommandList extends Window {

		private static final int WIDTH_P = 130;
		private static final int WIDTH_L = 180;
		private static final int MARGIN = 2;

		public CommandList(ArrayList<RingfingerAutomaton> dolls) {
			super();

			int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

			float pos = MARGIN;
			RenderedTextBlock title = PixelScene.renderTextBlock("选择要指挥的展品", 9);
			title.hardlight(TITLE_COLOR);
			title.setPos((width - title.width()) / 2, pos);
			title.maxWidth(width - MARGIN * 2);
			add(title);

			pos = title.bottom() + 3 * MARGIN;

			for (final RingfingerAutomaton doll : dolls){
				String label = "_环指自动人偶:_  生命 " + doll.HP + "/" + doll.HT;
				RedButton dollBtn = new RedButton(label) {
					@Override
					protected void onClick() {
						super.onClick();
						hide();
						selectCommandCell(doll);
					}
				};
				dollBtn.leftJustify = true;
				dollBtn.multiline = true;
				dollBtn.setSize(width, dollBtn.reqHeight());
				dollBtn.setRect(0, pos, width, dollBtn.reqHeight());
				add(dollBtn);
				pos = dollBtn.bottom() + MARGIN;
			}

			resize(width, (int) pos);
		}
	}

	@Override
	public String name() {
		return "指挥";
	}

	@Override
	public String shortDesc() {
		return "向场上的展品下达指令";
	}

	@Override
	public String desc() {
		return "向场上的环指自动人偶下达指令：\n\n"
				+ "在指令选格中_点击敌人_：展品优先攻击该目标\n"
				+ "_点击自己_：展品跟随画廊导师\n"
				+ "_点击空地_：展品驻守该格\n\n"
				+ "场上存在多只展品时会先选择指挥对象。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_COMMAND; //战旗图案
	}
}
