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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.items.BodyArtMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
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
 * 环指大师「强化」窗口。
 * <p>左侧 1 格放入人体派作品武器（强化目标），右侧 3 格放入素材或人体派作品（材料）。
 * <b>材料整叠放入</b>：点选背包里的素材时，整个堆叠一次性填入该格（格子右下角显示份数），
 * 强化时按<b>份数累计</b>四项数值（如骨素材 ×23 ⇒ 骨值 +230）；3 格 = 最多 3 种素材。
 * 放入后仍可点击格子把整叠取回背包，因此不会误消耗。
 * 点击「强化」将右侧材料的四项内置值累加到左侧武器的对应字段。
 * 武器值正常累积但**不改变武器等阶**；每次强化后按超出本阶起始武器值的部分
 * 重新计算等级（每 10 点 +1 级，上限 6/A+；武器值下降则等级随之降低）。关闭窗口时所有物品退回背包。</p>
 */
public class WndBodyReinforce extends Window implements WindowReturnsItems {

	private static final int WIDTH        = 116;
	private static final int LEFT_SIZE    = 44;   //左侧武器格
	private static final int RIGHT_SIZE   = 30;   //右侧材料格
	private static final int GAP          = 3;
	private static final int SLOT_AREA_H  = 3 * RIGHT_SIZE + 2 * GAP;  // = 96，右侧三格总高

	private CraftSlot leftSlot;                       //武器目标格
	private CraftSlot[] rightSlots = new CraftSlot[3];//材料格
	private CraftSlot slotPressed;                     //当前待填入的格
	private RedButton btnReinforce;

	public WndBodyReinforce( MasterRing masterRing ) {
		super();

		// --- 标题 ---
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( ItemSpriteSheet.MASTER_RING, null ) );
		titlebar.label( "强化" );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( 6 );
		message.text( "左侧放入人体派作品武器，右侧放入素材或人体派作品作为材料（一次放入整叠，按份数累计）。强化时材料的四项数值会累加到武器上；每次强化后按超出本阶起始武器值的部分重算等级（每 10 点 +1 级，上限 A+，下降则降级），等阶不变。" );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		float areaTop = message.bottom() + 3 * GAP;

		// --- 左侧武器格（垂直居中于材料区） ---
		leftSlot = new CraftSlot();
		float leftY = areaTop + (SLOT_AREA_H - LEFT_SIZE) / 2f;
		leftSlot.setRect( 8, leftY, LEFT_SIZE, LEFT_SIZE );
		add( leftSlot );

		// --- 右侧三材料格（纵向堆叠） ---
		for (int i = 0; i < 3; i++) {
			CraftSlot s = new CraftSlot();
			s.setRect( 66, areaTop + i * (RIGHT_SIZE + GAP), RIGHT_SIZE, RIGHT_SIZE );
			add( s );
			rightSlots[i] = s;
		}

		// --- 强化按钮 ---
		btnReinforce = new RedButton( "强化" ) {
			@Override
			protected void onClick() {
				reinforce();
			}
		};
		btnReinforce.enable( false );
		btnReinforce.setRect( 0, areaTop + SLOT_AREA_H + 2 * GAP, WIDTH, 20 );
		add( btnReinforce );

		resize( WIDTH, (int)btnReinforce.bottom() );
	}

	// 合成格：照抄 AlchemyScene.InputButton / WndBodyCraft.CraftSlot 模式
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
					Item cur = CraftSlot.this.item;
					if (cur != null) {
						// 已放物品：取回背包
						returnToBackpack( CraftSlot.this );
					} else {
						// 空格：打开背包选择物品
						slotPressed = CraftSlot.this;
						GameScene.selectItem( itemSelector );
					}
					updateReinforceButton();
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
				slot.item( new WndBag.Placeholder( ItemSpriteSheet.SOMETHING ) );
			} else {
				slot.item( this.item = item );
			}
		}
	}

	@Override
	public void onBackPressed() {
		returnAllItems();
		super.onBackPressed();
	}

	// 把左右格内所有已放入物品退回背包（背包满则掉脚下）。
	// 供 onBackPressed / GameScene.onPause 收尾(returnItemsAndClose) / destroy 兜底调用——
	// 防止物品只存在于 UI 格子（已从背包 detach、不可存档），切后台/旋转/进程被杀后丢失。
	private void returnAllItems() {
		if (Dungeon.hero == null || Dungeon.level == null) return;
		returnToBackpack( leftSlot );
		for (CraftSlot s : rightSlots) {
			returnToBackpack( s );
		}
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

	private void updateReinforceButton() {
		// 启用条件：左侧有武器 + 右侧至少一格有材料
		boolean ok = leftSlot.item() instanceof BodyArtWeapon;
		if (ok) {
			boolean hasMat = false;
			for (CraftSlot s : rightSlots) {
				if (s.item() != null) { hasMat = true; break; }
			}
			ok = hasMat;
		}
		btnReinforce.enable( ok );
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

	// --- 强化逻辑 ---
	private void reinforce() {
		Item target = leftSlot.item();
		if (!(target instanceof BodyArtWeapon)) {
			GLog.w( "左侧需要放入人体派作品武器。" );
			return;
		}
		BodyArtWeapon weapon = (BodyArtWeapon) target;

		// 1. 累加右侧材料四值（素材整叠放入，按份数累计）
		int addW = 0, addB = 0, addM = 0, addBd = 0;
		for (CraftSlot s : rightSlots) {
			Item it = s.item();
			if (it instanceof BodyArtMaterial) {
				BodyArtMaterial m = (BodyArtMaterial) it;
				int qty = Math.max( 1, m.quantity() );
				addW += m.weaponValue * qty;  addB += m.boneValue * qty;
				addM += m.meatValue   * qty;  addBd += m.bloodValue * qty;
			} else if (it instanceof BodyArtWeapon) {
				BodyArtWeapon w = (BodyArtWeapon) it;
				addW += w.weaponValue;  addB += w.boneValue;
				addM += w.meatValue;    addBd += w.bloodValue;
			}
		}

		if (addW == 0 && addB == 0 && addM == 0 && addBd == 0) {
			GLog.w( "没有可用材料。" );
			return;
		}

		// 2. 记录强化前等级
		int oldLevel = weapon.level();

		// 3. 累加到武器
		weapon.weaponValue += addW;
		weapon.boneValue   += addB;
		weapon.meatValue   += addM;
		weapon.bloodValue  += addBd;

		// 4. 重新计算并应用强化等级（随 Weapon 值实时升降，上限 6/A+）
		weapon.syncReinforceLevel();
		int delta = weapon.level() - oldLevel;

		if (delta > 0) {
			GLog.p( "强化成功！%s 等级提升 %d 级。", weapon.title(), delta );
		} else if (delta < 0) {
			GLog.w( "强化完成。%s 的武器值下降，等级降低 %d 级。", weapon.title(), -delta );
		} else {
			GLog.i( "强化成功！数值已累积，等级暂未变化。" );
		}

		// 5. 消耗右侧材料
		for (CraftSlot s : rightSlots) {
			s.item( null );
		}

		// 6. 刷新左侧武器格显示（数值与等级变了）
		leftSlot.item( weapon );
		Item.updateQuickslot();
		updateReinforceButton();
	}

	// 背包物品选择器：左侧仅武器，右侧素材或武器
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return slotPressed == leftSlot
					? "选择人体派作品武器放入左侧"
					: "选择素材或人体派作品整叠放入右侧";
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			// 装备中的物品不可加入强化（会导致物品复制 bug）
			if (item.isEquipped( Dungeon.hero )) return false;
			if (slotPressed == leftSlot) {
				// 左侧仅允许人体派作品武器
				return item instanceof BodyArtWeapon;
			}
			// 右侧允许素材与人体派作品
			return item instanceof BodyArtMaterial || item instanceof BodyArtWeapon;
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && slotPressed != null && slotPressed.parent != null) {
				if (slotPressed.item() != null) {
					returnToBackpack( slotPressed );
				}
				// 整叠放入：Item.detach() 只会从堆叠中拆出 1 份，这里直接把整个堆叠搬进格子
				Item material = item.detachAll( Dungeon.hero.belongings.backpack );
				if (material == null) return;
				slotPressed.item( material );
				Item.updateQuickslot();
				updateReinforceButton();
			}
		}
	};
}
