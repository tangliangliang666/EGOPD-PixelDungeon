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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.CheckBox;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import java.util.ArrayList;

/**
 * 「趣味挑战」的选择/展示窗口 —— 结构**逐行照抄** {@link WndChallenges}，
 * 只把「列哪几条」换成 {@link Challenges#funMasks()}（拆迁办 / 依旧果冻人 / 水仙追迹 / 调试模式，
 * 2026-09-24 起从常规挑战移出）。
 *
 * <h3>为什么单独开一个窗口，而不是往 WndChallenges 里加分区</h3>
 * 趣味挑战与常规挑战**共用同一个 {@code challenges} 位域**（存档 / 设置 / 选人界面都不分家），
 * 但口径不同：趣味挑战不计入常规挑战数（随机池、局内计数、{@code CHAMPION_*} 徽章都不受影响），
 * 所以入口分开、各自只列自己那一类，写回时也只覆写自己负责的位——见下面 {@link #onBackPressed()}。
 *
 * <h3>与 WndChallenges 的全部区别</h3>
 * <ul>
 *   <li>标题文案走本类：{@code windows.wndfunchallenges.title}；</li>
 *   <li>列 {@link Challenges#funMasks()} 而非 {@code regularMasks()}；</li>
 *   <li>写回时清的是 {@link Challenges#funMask()}，常规挑战位原样保留。</li>
 * </ul>
 * 不设特殊图标 / 排版（用户 2026-09-24 口径：套用挑战即可）。
 *
 * <h3>写回时机</h3>
 * 与 WndChallenges / WndTrials 一致：勾选只改内存里的勾选框，**关窗时一次性写回** {@link SPDSettings}。
 */
public class WndFunChallenges extends Window {

	private static final int WIDTH		= 120;
	private static final int TTL_HEIGHT = 16;
	private static final int BTN_HEIGHT = 16;
	private static final int GAP        = 1;

	private boolean editable;
	private ArrayList<CheckBox> boxes = new ArrayList<>();
	//每个勾选行对应的挑战位（与 boxes 同序）。关闭时按它写回，常规挑战位从当前设置里原样带过去。
	private ArrayList<Integer> boxMasks = new ArrayList<>();

	public WndFunChallenges( int checked, boolean editable ) {

		super();

		this.editable = editable;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 12 );
		title.hardlight( TITLE_COLOR );
		title.setPos(
				(WIDTH - title.width()) / 2,
				(TTL_HEIGHT - title.height()) / 2
		);
		PixelScene.align(title);
		add( title );

		float pos = TTL_HEIGHT;
		int row = 0;
		for (int mask : Challenges.funMasks()) {

			final String challenge = Challenges.nameId( mask );

			CheckBox cb = new CheckBox( Messages.titleCase(Messages.get(Challenges.class, challenge)) );
			cb.checked( (checked & mask) != 0 );
			cb.active = editable;

			if (row > 0) {
				pos += GAP;
			}
			cb.setRect( 0, pos, WIDTH-16, BTN_HEIGHT );

			add( cb );
			boxes.add( cb );
			boxMasks.add( mask );

			IconButton info = new IconButton(Icons.get(Icons.INFO)){
				@Override
				protected void onClick() {
					super.onClick();
					ShatteredPixelDungeon.scene().add(
							new WndMessage(Messages.get(Challenges.class, challenge+"_desc"))
					);
				}
			};
			info.setRect(cb.right(), pos, 16, BTN_HEIGHT);
			add(info);

			pos = cb.bottom();
			row++;
		}

		resize( WIDTH, (int)pos );
	}

	@Override
	public void onBackPressed() {

		if (editable) {
			//只覆写本窗口负责的趣味挑战位：常规挑战位不在本窗口内，必须原样保留
			//（否则开一次趣味挑战窗口就会把已选的常规挑战全清掉）
			int value = SPDSettings.challenges() & ~Challenges.funMask();
			for (int i=0; i < boxes.size(); i++) {
				if (boxes.get( i ).checked()) {
					value |= boxMasks.get( i );
				}
			}
			SPDSettings.challenges( value );
		}

		super.onBackPressed();
	}
}
