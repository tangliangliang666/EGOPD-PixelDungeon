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

import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts.RingMasterArt;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.ui.IconButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.Icons;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.NinePatch;

import java.util.ArrayList;

/**
 * 环指大师技艺列表窗口（实现模式参考牧师的 {@link WndClericSpells} 法术列表）。
 * <p>技艺按层级分组展示（tier 1 指环技艺 / tier 2 展品技艺），
 * 每个技艺显示为一个图标按钮（不可用时置灰，当前绑定到指环的技艺高亮）：
 * 点击执行，长按绑定到大师指环（快速执行），info 模式下点击查看完整描述。</p>
 */
public class WndRingMasterArts extends Window {

	//宽度需容纳 tier 2 一行 6 个按钮（构建/指挥/活化/修整/激发/装备，6×24=144）
	protected static final int WIDTH = 150;
	public static final int BTN_SIZE = 20;
	private static final int MARGIN = 2;

	public WndRingMasterArts( Hero hero, boolean info ){

		RenderedTextBlock title = PixelScene.renderTextBlock(
				Messages.titleCase( info ? "技艺详情" : "大师指环技艺" ), 9);
		title.hardlight(TITLE_COLOR);
		title.setPos((WIDTH - title.width()) / 2, MARGIN);
		title.maxWidth(WIDTH - MARGIN * 2);
		add(title);

		//右上角 info 模式切换按钮（同法典窗口）
		IconButton btnInfo = new IconButton(info ? new ItemSpriteRing() : Icons.INFO.get()){
			@Override
			protected void onClick() {
				GameScene.show(new WndRingMasterArts(hero, !info));
				hide();
			}
		};
		btnInfo.setRect(WIDTH-16, 0, 16, 16);
		add(btnInfo);

		RenderedTextBlock msg;
		if (info){
			msg = PixelScene.renderTextBlock( "技艺详情。点击图标查看该技艺的完整描述。", 6);
		} else {
			msg = PixelScene.renderTextBlock( "点击执行；长按将其绑定到大师指环（绑定后屏幕右侧出现该技艺的按钮，点击直接执行；再次长按取消绑定）", 6);
		}
		msg.maxWidth(WIDTH - 18);
		msg.setPos(0, title.bottom() + 3);
		msg.alpha(0.6f);
		add(msg);

		int top = (int) msg.bottom() + 4;

		MasterRing ring = hero.belongings.getItem( MasterRing.class );

		//按层级分组：每层级一行居中网格，层级间加分隔线 + 组名
		int lastTier = -1;
		ArrayList<IconButton> tierBtns = new ArrayList<>();

		for (RingMasterArt art : RingMasterArt.getArtList(hero)) {

			if (art.tier() != lastTier){
				//先把上一层的按钮排完
				top = layoutTierButtons(tierBtns, top);
				tierBtns.clear();

				if (lastTier != -1){
					top += 2;
					ColorBlock sep = new ColorBlock(WIDTH, 1, 0xFF000000);
					sep.y = top;
					add(sep);
					top += 4;
				}

				RenderedTextBlock tierTitle = PixelScene.renderTextBlock(
						RingMasterArt.tierName(art.tier()), 7);
				tierTitle.hardlight(0xBBBBBB);
				tierTitle.maxWidth(WIDTH - MARGIN * 2);
				tierTitle.setPos(MARGIN, top);
				add(tierTitle);
				top = (int) tierTitle.bottom() + 2;

				lastTier = art.tier();
			}

			ArtButton btn = new ArtButton(art, ring, info);
			add(btn);
			tierBtns.add(btn);
		}
		top = layoutTierButtons(tierBtns, top);

		resize(WIDTH, top + MARGIN);
	}

	/** 一行内居中排列图标按钮，返回新的 y 坐标。 */
	private int layoutTierButtons( ArrayList<IconButton> btns, int top ){
		if (btns.isEmpty()) return top;

		int left = 2 + (WIDTH - btns.size() * (BTN_SIZE + 4)) / 2;
		for (IconButton btn : btns) {
			btn.setRect(left, top, BTN_SIZE, BTN_SIZE);
			left += btn.width() + 4;
		}
		return top + BTN_SIZE + 2;
	}

	public WndRingMasterArts( Hero hero ){
		this( hero, false );
	}

	//大师指环的小图标（info 模式切换按钮用；指环图标 8×10 略小于按钮框，可接受）
	private static class ItemSpriteRing extends com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite {
		ItemSpriteRing(){
			super( com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet.MASTER_RING, null );
		}
	}

	public class ArtButton extends IconButton {

		RingMasterArt art;
		boolean info;

		NinePatch bg;

		public ArtButton( RingMasterArt art, MasterRing ring, boolean info ){
			super(new HeroIcon(art));

			this.art = art;
			this.info = info;

			if (!info && !art.canPerform(Dungeon.hero)){
				icon.alpha( 0.3f );
			}
			//当前绑定到指环的技艺高亮（同法典快速施法的高亮方式）
			if (ring != null && ring.quickArt() == art){
				icon.brightness( 3f );
			}

			bg = Chrome.get(Chrome.Type.TOAST);
			addToBack(bg);
		}

		@Override
		protected void onPointerUp() {
			super.onPointerUp();
			if (!info && !art.canPerform(Dungeon.hero)){
				icon.alpha( 0.3f );
			}
		}

		@Override
		protected void layout() {
			super.layout();

			if (bg != null) {
				bg.size(width, height);
				bg.x = x;
				bg.y = y;
			}
		}

		@Override
		protected void onClick() {
			hide();

			if (info){
				GameScene.show(new WndTitledMessage(new HeroIcon(art), art.name(), art.desc()));
				return;
			}

			if (!art.canPerform(Dungeon.hero)){
				GLog.w("当前无法执行该技艺。");
				return;
			}
			art.onPerform(Dungeon.hero);
		}

		@Override
		protected boolean onLongClick() {
			if (info) return false;

			//hide() 解析到本类继承的 Window.hide()（不是 Gizmo.hide()）：连技艺窗口一起关掉
			hide();
			MasterRing ring = Dungeon.hero.belongings.getItem( MasterRing.class );
			if (ring == null){
				//指环不在身上（理论上不会发生）：明确说出来，别让长按「静默无事发生」
				GLog.w( "大师指环不在身上，无法绑定。" );
				return true;
			}
			ring.setQuickArt(art);
			if (ring.quickArt() == art){
				GLog.i( "已将「_" + art.name() + "_」绑定到大师指环，点击屏幕右侧的按钮即可直接执行。" );
			} else {
				GLog.i( "已取消「_" + art.name() + "_」的绑定。" );
			}
			return true;
		}

		@Override
		protected String hoverText() {
			return "_" + art.name() + "_\n" + art.shortDesc();
		}
	}
}
