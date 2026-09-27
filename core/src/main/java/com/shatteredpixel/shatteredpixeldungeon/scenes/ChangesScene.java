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

package com.shatteredpixel.shatteredpixeldungeon.scenes;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.messages.Languages;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ExitButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.TitleBackground;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.ScrollPane;
import com.shatteredpixel.shatteredpixeldungeon.ui.StyledButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.ChangeInfo;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.EGOPD_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.WndChanges;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.WndChangesTabbed;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_1_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_2_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_3_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_4_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_5_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_6_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_7_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_8_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v0_9_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v1_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v2_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.ui.changelist.v3_X_Changes;
import com.shatteredpixel.shatteredpixeldungeon.windows.IconTitle;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.Scene;
import com.watabou.noosa.audio.Music;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.RectF;

import java.util.ArrayList;

public class ChangesScene extends PixelScene {
	
	public static int changesSelected = 0;

	//[EGOPD] EGOPD 自家改动栏的页签序号（排在最前）。内容由本作作者撰写，
	//故这一页不显示上游那句「改动详情仅提供英文版本」的提示。
	private static final int EGOPD_TAB = 0;

	private NinePatch rightPanel;
	private ScrollPane rightScroll;
	private IconTitle changeTitle;
	private RenderedTextBlock changeBody;
	
	@Override
	public void create() {
		super.create();

		Music.INSTANCE.playTracks(
				new String[]{Assets.Music.THEME_1, Assets.Music.THEME_2},
				new float[]{1, 1},
				false);

		int w = Camera.main.width;
		int h = Camera.main.height;

		RectF insets = getCommonInsets();

		TitleBackground BG = new TitleBackground(w, h);
		//background added later

		w -= insets.left + insets.right;
		h -= insets.top + insets.bottom;

		IconTitle title = new IconTitle(Icons.CHANGES.get(), Messages.get(this, "title"));
		title.setSize(200, 0);
		title.setPos(
				insets.left + (w - title.reqWidth()) / 2f,
				insets.top + (20 - title.height()) / 2f
		);
		align(title);
		add(title);

		ExitButton btnExit = new ExitButton();
		btnExit.setPos( insets.left + w - btnExit.width(), insets.top );
		add( btnExit );

		NinePatch panel = Chrome.get(Chrome.Type.TOAST);

		int pw = 135 + panel.marginLeft() + panel.marginRight() - 2;
		int ph = h - 55;   //[EGOPD] 上游为 h-36；页签按钮从 8 个增至 9 个后排成两行，故让出 19px

		if (h >= PixelScene.MIN_HEIGHT_FULL && w >= 300) {
			panel.size( pw, ph );
			panel.x = insets.left + (w - pw) / 2f - pw/2 - 1;
			panel.y = insets.top + 20;

			rightPanel = Chrome.get(Chrome.Type.TOAST);
			rightPanel.size( pw, ph );
			rightPanel.x = (w - pw) / 2f + pw/2 + 1;
			rightPanel.y = 20;
			add(rightPanel);

			rightScroll = new ScrollPane(new Component());
			add(rightScroll);
			rightScroll.setRect(
					rightPanel.x + rightPanel.marginLeft(),
					rightPanel.y + rightPanel.marginTop()-1,
					rightPanel.innerWidth() + 2,
					rightPanel.innerHeight() + 2);
			rightScroll.scrollTo(0, 0);

			changeTitle = new IconTitle(Icons.get(Icons.CHANGES), Messages.get(this, "right_title"));
			changeTitle.setPos(0, 1);
			changeTitle.setSize(pw, 20);
			rightScroll.content().add(changeTitle);

			String body = Messages.get(this, "right_body");

			changeBody = PixelScene.renderTextBlock(body, 6);
			changeBody.maxWidth(pw - panel.marginHor());
			changeBody.setPos(0, changeTitle.bottom()+2);
			rightScroll.content().add(changeBody);

		} else {
			panel.size( pw, ph );
			panel.x = insets.left + (w - pw) / 2f;
			panel.y = insets.top + 20;
		}
		align( panel );
		add( panel );
		
		final ArrayList<ChangeInfo> changeInfos = new ArrayList<>();

		//[EGOPD] EGOPD 自己的改动栏不显示「仅英文」提示（该页由本作作者用中文撰写）
		if (Messages.lang() != Languages.ENGLISH && changesSelected != EGOPD_TAB){
			ChangeInfo langWarn = new ChangeInfo("", true, Messages.get(this, "lang_warn"));
			langWarn.hardlight(CharSprite.WARNING);
			changeInfos.add(langWarn);
		}
		
		//[EGOPD] case 序号 = 页签按钮顺序，两者必须一一对应（见下方 tabLabels 数组）
		switch (changesSelected){
			case 0:
				EGOPD_Changes.addAllChanges(changeInfos);
				break;
			case 1: default:
				v3_X_Changes.addAllChanges(changeInfos);
				break;
			case 2:
				v2_X_Changes.addAllChanges(changeInfos);
				break;
			case 3:
				v1_X_Changes.addAllChanges(changeInfos);
				break;
			case 4:
				v0_9_X_Changes.addAllChanges(changeInfos);
				break;
			case 5:
				v0_8_X_Changes.addAllChanges(changeInfos);
				break;
			case 6:
				v0_7_X_Changes.addAllChanges(changeInfos);
				break;
			case 7:
				v0_6_X_Changes.addAllChanges(changeInfos);
				break;
			case 8:
				v0_5_X_Changes.addAllChanges(changeInfos);
				v0_4_X_Changes.addAllChanges(changeInfos);
				v0_3_X_Changes.addAllChanges(changeInfos);
				v0_2_X_Changes.addAllChanges(changeInfos);
				v0_1_X_Changes.addAllChanges(changeInfos);
				break;
		}

		ScrollPane list = new ScrollPane( new Component() ){

			@Override
			public void onClick(float x, float y) {
				for (ChangeInfo info : changeInfos){
					if (info.onClick( x, y )){
						return;
					}
				}
			}

		};
		add( list );

		Component content = list.content();
		content.clear();

		float posY = 0;
		float nextPosY = 0;
		boolean second = false;
		for (ChangeInfo info : changeInfos){
			if (info.major) {
				posY = nextPosY;
				second = false;
				info.setRect(0, posY, panel.innerWidth(), 0);
				content.add(info);
				posY = nextPosY = info.bottom();
			} else {
				if (!second){
					second = true;
					info.setRect(0, posY, panel.innerWidth()/2f, 0);
					content.add(info);
					nextPosY = info.bottom();
				} else {
					second = false;
					info.setRect(panel.innerWidth()/2f, posY, panel.innerWidth()/2f, 0);
					content.add(info);
					nextPosY = Math.max(info.bottom(), nextPosY);
					posY = nextPosY;
				}
			}
		}

		content.setSize( panel.innerWidth(), (int)Math.ceil(posY) );

		list.setRect(
				panel.x + panel.marginLeft(),
				panel.y + panel.marginTop() - 1,
				panel.innerWidth() + 2,
				panel.innerHeight() + 2);
		list.scrollTo(0, 0);

		//[EGOPD] 页签按钮：上游 8 个正好填满一行，新增 EGOPD 栏后排成两行
		//第 1 行（tabY）= EGOPD + 3.X~0.9；第 2 行（tabY+19）= 0.8~0.5-
		float tabX = list.left() - 4f;
		float tabY = list.bottom();

		StyledButton btnEgopd = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "EGOPD", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != EGOPD_TAB) {
					changesSelected = EGOPD_TAB;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != EGOPD_TAB) btnEgopd.textColor( 0xBBBBBB );
		btnEgopd.setRect(tabX, tabY, 32, changesSelected == EGOPD_TAB ? 19 : 15);
		addToBack(btnEgopd);

		tabX += 30;

		StyledButton btn3_X = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "3.X", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 1) {
					changesSelected = 1;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 1) btn3_X.textColor( 0xBBBBBB );
		btn3_X.setRect(tabX, tabY, 19, changesSelected == 1 ? 19 : 15);
		addToBack(btn3_X);

		tabX += 17;

		StyledButton btn2_X = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "2.X", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 2) {
					changesSelected = 2;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 2) btn2_X.textColor( 0xBBBBBB );
		btn2_X.setRect(tabX, tabY, 19, changesSelected == 2 ? 19 : 15);
		addToBack(btn2_X);

		tabX += 17;

		StyledButton btn1_X = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "1.X", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 3) {
					changesSelected = 3;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 3) btn1_X.textColor( 0xBBBBBB );
		btn1_X.setRect(tabX, tabY, 19, changesSelected == 3 ? 19 : 15);
		addToBack(btn1_X);

		tabX += 17;

		StyledButton btn0_9 = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "0.9", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 4) {
					changesSelected = 4;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 4) btn0_9.textColor( 0xBBBBBB );
		btn0_9.setRect(tabX, tabY, 19, changesSelected == 4 ? 19 : 15);
		addToBack(btn0_9);

		//第 2 行：更早的版本页
		tabX = list.left() - 4f;
		tabY += 19;

		StyledButton btn0_8 = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "0.8", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 5) {
					changesSelected = 5;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 5) btn0_8.textColor( 0xBBBBBB );
		btn0_8.setRect(tabX, tabY, 19, changesSelected == 5 ? 19 : 15);
		addToBack(btn0_8);

		tabX += 17;
		
		StyledButton btn0_7 = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "0.7", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 6) {
					changesSelected = 6;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 6) btn0_7.textColor( 0xBBBBBB );
		btn0_7.setRect(tabX, tabY, 19, changesSelected == 6 ? 19 : 15);
		addToBack(btn0_7);

		tabX += 17;
		
		StyledButton btn0_6 = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "0.6", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 7) {
					changesSelected = 7;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 7) btn0_6.textColor( 0xBBBBBB );
		btn0_6.setRect(tabX, tabY, 19, changesSelected == 7 ? 19 : 15);
		addToBack(btn0_6);

		tabX += 17;
		
		StyledButton btnOld = new StyledButton(Chrome.Type.GREY_BUTTON_TR, "0.5-", 8){
			@Override
			protected void onClick() {
				super.onClick();
				if (changesSelected != 8) {
					changesSelected = 8;
					ShatteredPixelDungeon.seamlessResetScene();
				}
			}
		};
		if (changesSelected != 8) btnOld.textColor( 0xBBBBBB );
		btnOld.setRect(tabX, tabY, 22, changesSelected == 8 ? 19 : 15);
		addToBack(btnOld);

		addToBack( BG );

		fadeIn();
	}

	private void updateChangesText(Image icon, String title, String... messages){
		if (changeTitle != null){
			changeTitle.icon(icon);
			changeTitle.label(title);
			changeTitle.setPos(changeTitle.left(), changeTitle.top());

			String message = "";
			for (int i = 0; i < messages.length; i++){
				message += messages[i];
				if (i != messages.length-1){
					message += "\n\n";
				}
			}
			changeBody.text(message);
			rightScroll.content().setSize(rightScroll.width(), changeBody.bottom()+2);
			rightScroll.setSize(rightScroll.width(), rightScroll.height());
			rightScroll.scrollTo(0, 0);

		} else {
			if (messages.length == 1) {
				addToFront(new WndChanges(icon, title, messages[0]));
			} else {
				addToFront(new WndChangesTabbed(icon, title, messages));
			}
		}
	}

	public static void showChangeInfo(Image icon, String title, String... messages){
		Scene s = ShatteredPixelDungeon.scene();
		if (s instanceof ChangesScene){
			((ChangesScene) s).updateChangesText(icon, title, messages);
			return;
		}
		if (messages.length == 1) {
			s.addToFront(new WndChanges(icon, title, messages[0]));
		} else {
			s.addToFront(new WndChangesTabbed(icon, title, messages));
		}
	}
	
	@Override
	protected void onBackPressed() {
		ShatteredPixelDungeon.switchNoFade(TitleScene.class);
	}

}
