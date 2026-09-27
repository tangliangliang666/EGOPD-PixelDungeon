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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.VengeanceArts;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/**
 * 忠义巡礼者的「复仇技艺」技能列表窗口（结构照抄 {@link WndScorchingSkill} / {@link WndArtSkill}）。
 *
 * <p>五式按层数排列，每式标注消耗的怨恨标记层数；把招数条件写成
 * {@code VengeanceArts.canUse(Move)}（＝视野内存在带着足够层数的敌人），
 * 不满足的条目置灰——玩家从自己的状态看不出敌人身上有几层标记，所以入口按钮常驻、
 * 由这个列表负责「现在能放哪几式」。</p>
 *
 * <p>这里<b>不再</b>塞「切换主副」入口：那个按钮走 {@code ActionIndicator} 的副槽，
 * 与技艺按钮各占一行、同时显示（见 {@link com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator}）。</p>
 */
public class WndVengeanceSkill extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;

	private static final int MARGIN  = 2;

	public WndVengeanceSkill( VengeanceArts arts ){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		float pos = MARGIN;
		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get( this, "title" ), 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width()) / 2, pos );
		title.maxWidth( width - MARGIN * 2 );
		add( title );

		pos = title.bottom() + 3 * MARGIN;

		for (VengeanceArts.Move move : VengeanceArts.Move.values()){
			String text = "_" + move.title() + "_（" + Messages.get( this, "cost", move.cost ) + "）\n"
					+ move.desc();
			RedButton moveBtn = new RedButton( text, 6 ){
				@Override
				protected void onClick(){
					super.onClick();
					hide();
					arts.useMove( move );
				}
			};
			moveBtn.leftJustify = true;
			moveBtn.multiline = true;
			moveBtn.setSize( width, moveBtn.reqHeight() );
			moveBtn.setRect( 0, pos, width, moveBtn.reqHeight() );
			moveBtn.enable( arts.canUse( move ) );
			add( moveBtn );
			pos = moveBtn.bottom() + MARGIN;
		}

		resize( width, (int) pos );
	}
}
