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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.HardMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ItemSlot;
import com.shatteredpixel.shatteredpixeldungeon.ui.RedButton;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

/**
 * 环指大师「分解」窗口。
 * <p>放入 1 件非人体派的近战武器，点击「分解」将其拆解为硬质素材：
 * N 阶武器 → N-1 个硬质素材（1 阶武器无产出，不可分解）。
 * 装备中的物品需先卸下才能放入（与创作/强化一致，避免物品复制 bug）。
 * 关闭窗口时输入格内的武器退回背包。</p>
 */
public class WndBodyDecompose extends Window implements WindowReturnsItems {

	private static final int WIDTH      = 116;
	private static final int SLOT_SIZE  = 44;   //输入格
	private static final int GAP        = 3;

	private CraftSlot inputSlot;                  //武器输入格
	private CraftSlot slotPressed;                 //当前待填入的格
	private RedButton btnDecompose;
	private RenderedTextBlock statusText;          //预览产出数量 / 错误提示

	public WndBodyDecompose( MasterRing masterRing ) {
		super();

		// --- 标题 ---
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( ItemSpriteSheet.MASTER_RING, null ) );
		titlebar.label( "分解" );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( 6 );
		message.text( "放入非人体派的近战武器进行分解。N 阶武器分解为 N-1 个硬质素材（1 阶武器无产出，不可分解）。装备中的武器需先卸下。" );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		float areaTop = message.bottom() + 3 * GAP;

		// --- 居中输入格 ---
		inputSlot = new CraftSlot();
		inputSlot.setRect( (WIDTH - SLOT_SIZE) / 2f, areaTop, SLOT_SIZE, SLOT_SIZE );
		add( inputSlot );

		// --- 产出预览文本 ---
		statusText = PixelScene.renderTextBlock( 7 );
		statusText.maxWidth( WIDTH );
		statusText.setPos( 0, inputSlot.bottom() + 2 * GAP );
		add( statusText );

		// --- 分解按钮 ---
		btnDecompose = new RedButton( "分解" ) {
			@Override
			protected void onClick() {
				decompose();
			}
		};
		btnDecompose.enable( false );
		btnDecompose.setRect( 0, statusText.bottom() + 2 * GAP, WIDTH, 20 );
		add( btnDecompose );

		resize( WIDTH, (int)btnDecompose.bottom() );
	}

	// 输入格：照抄 WndBodyCraft / WndBodyReinforce 的 CraftSlot 模式
	private class CraftSlot extends Component {

		protected NinePatch bg;
		protected ItemSlot slot;

		private Item item = null;

		@Override
		protected void createChildren() {
			super.createChildren();

			bg = Chrome.get( Chrome.Type.RED_BUTTON );
			add( bg );

			slot = new ItemSlot() {
				@Override
				protected void onPointerDown() {
					bg.brightness( 1.2f );
					Sample.INSTANCE.play( Assets.Sounds.CLICK );
				}

				@Override
				protected void onPointerUp() {
					bg.resetColor();
				}

				@Override
				protected void onClick() {
					super.onClick();
					if (CraftSlot.this.item != null) {
						// 已放物品：取回背包
						returnToBackpack( CraftSlot.this );
					} else {
						// 空格：打开背包选择武器
						slotPressed = CraftSlot.this;
						GameScene.selectItem( itemSelector );
					}
					updateDecomposeButton();
				}

				@Override
				protected boolean onLongClick() {
					if (CraftSlot.this.item != null) {
						GameScene.show( new WndInfoItem( CraftSlot.this.item ) );
						return true;
					}
					return false;
				}
			};
			slot.enable( true );
			add( slot );

			item( null );
		}

		@Override
		protected void layout() {
			super.layout();

			bg.x = x;
			bg.y = y;
			bg.size( width, height );

			slot.setRect( x + 2, y + 2, width - 4, height - 4 );
		}

		public Item item() {
			return item;
		}

		public void item( Item item ) {
			if (item == null) {
				this.item = null;
				//空格子用占位虚拟物品，绝不给 ItemSlot 传 null
				slot.item( new WndBag.Placeholder( ItemSpriteSheet.SOMETHING ) );
			} else {
				slot.item( this.item = item );
			}
		}
	}

	// 关闭窗口时自动退回输入格的武器
	@Override
	public void onBackPressed() {
		returnAllItems();
		super.onBackPressed();
	}

	// 把输入格内已放入物品退回背包（背包满则掉脚下）。
	// 供 onBackPressed / GameScene.onPause 收尾(returnItemsAndClose) / destroy 兜底调用——
	// 防止物品只存在于 UI 格子（已从背包 detach、不可存档），切后台/旋转/进程被杀后丢失。
	private void returnAllItems() {
		if (Dungeon.hero == null || Dungeon.level == null) return;
		returnToBackpack( inputSlot );
	}

	@Override
	public void returnItemsAndClose() {
		returnAllItems();
		hide();
	}

	// 任何销毁路径（hide、场景重建/旋转、GameScene 销毁等）都先退回物品再销毁
	@Override
	public void destroy() {
		returnAllItems();
		super.destroy();
	}

	// 刷新分解按钮可用性与产出预览
	private void updateDecomposeButton() {
		Item it = inputSlot.item();
		boolean ok = false;
		String preview = "";
		if (it instanceof MeleeWeapon && !(it instanceof BodyArtWeapon)) {
			int tier = ((MeleeWeapon) it).tier;
			int count = tier - 1;
			if (count > 0) {
				ok = true;
				preview = "将产出 _" + count + "_ 个硬质素材";
			} else {
				preview = "1 阶武器分解无产出";
			}
		} else if (it != null) {
			preview = "只能分解非人体派的近战武器";
		}
		statusText.text( preview );
		btnDecompose.enable( ok );
	}

	private void returnToBackpack( CraftSlot slot ) {
		Item it = slot.item();
		if (it == null) return;
		if (!it.collect( Dungeon.hero.belongings.backpack )) {
			Dungeon.level.drop( it, Dungeon.hero.pos ).sprite.drop();
		}
		slot.item( null );
		Item.updateQuickslot();
	}

	// --- 分解逻辑 ---
	private void decompose() {
		Item it = inputSlot.item();
		if (!(it instanceof MeleeWeapon) || it instanceof BodyArtWeapon) {
			GLog.w( "只能分解非人体派的近战武器。" );
			return;
		}
		int tier = ((MeleeWeapon) it).tier;
		int count = tier - 1;
		if (count <= 0) {
			GLog.w( "1 阶武器分解无产出。" );
			return;
		}

		// 消耗武器（放入格时已从背包 detach，此处直接清格即销毁）
		inputSlot.item( null );

		// 产出硬质素材（可堆叠，一次产出 count 个）
		HardMaterial product = new HardMaterial();
		product.quantity( count );
		if (!product.collect( Dungeon.hero.belongings.backpack )) {
			Dungeon.level.drop( product, Dungeon.hero.pos ).sprite.drop();
		}
		GLog.p( "分解完成：获得 %d 个硬质素材！", count );

		Item.updateQuickslot();
		updateDecomposeButton();
	}

	// 背包物品选择器：仅允许非人体派近战武器，且不能是装备中的物品
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return "选择非人体派的近战武器进行分解";
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			// 装备中的物品不可加入分解（会导致物品复制 bug）
			if (item.isEquipped( Dungeon.hero )) return false;
			// 仅允许非人体派的近战武器
			return item instanceof MeleeWeapon && !(item instanceof BodyArtWeapon);
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && slotPressed != null && slotPressed.parent != null) {
				if (slotPressed.item() != null) {
					returnToBackpack( slotPressed );
				}
				Item material = item.detach( Dungeon.hero.belongings.backpack );
				if (material == null) {
					item.detachAll( Dungeon.hero.belongings.backpack );
					material = item;
				}
				slotPressed.item( material );
				Item.updateQuickslot();
				updateDecomposeButton();
			}
		}
	};
}
