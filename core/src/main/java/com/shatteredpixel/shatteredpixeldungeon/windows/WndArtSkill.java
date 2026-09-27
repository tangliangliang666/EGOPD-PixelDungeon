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

import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtTechniques;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

/**
 * 艺术之巅的专精特性技艺选择窗口（照抄命运弃子 {@link WndScorchingSkill} 结构）。
 * 三招各消耗手中人体派作品的 10 点血/骨/肉值，数值不足时按钮置灰。
 */
public class WndArtSkill extends Window {

	private static final int WIDTH_P = 120;
	private static final int WIDTH_L = 180;

	private static final int MARGIN  = 2;

	public WndArtSkill( ArtTechniques tech ){
		super();

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		float pos = MARGIN;
		RenderedTextBlock title = PixelScene.renderTextBlock( "艺术技艺", 9 );
		title.hardlight( TITLE_COLOR );
		title.setPos( (width - title.width()) / 2, pos );
		title.maxWidth( width - MARGIN * 2 );
		add( title );

		pos = title.bottom() + 3 * MARGIN;

		for (ArtTechniques.ArtMove move : ArtTechniques.ArtMove.values()) {
			String text = "_" + move.title() + "（10" + move.costName() + "）:_ " + move.desc();
			RedButton moveBtn = new RedButton( text, 6 ){
				@Override
				protected void onClick() {
					super.onClick();
					hide();
					tech.useMove( move );
				}
			};
			moveBtn.leftJustify = true;
			moveBtn.multiline = true;
			moveBtn.setSize( width, moveBtn.reqHeight() );
			moveBtn.setRect( 0, pos, width, moveBtn.reqHeight() );
			moveBtn.enable( tech.canUse( move ) );
			add( moveBtn );
			pos = moveBtn.bottom() + MARGIN;
		}

		resize( width, (int) pos );
	}

}
