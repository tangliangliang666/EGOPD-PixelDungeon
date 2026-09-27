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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Inspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.BodyArtMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CommonWorkA;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CommonWorkB;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CommonWorkC;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CrudeWorkA;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CrudeWorkB;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.CrudeWorkC;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FamedWorkA;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FamedWorkB;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FamedWorkC;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FineWorkA;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FineWorkB;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.FineWorkC;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LifeWorkTibia;
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
import com.watabou.utils.Random;

/**
 * 环指大师「创作」合成窗口。
 * <p>类似炼金釜合成界面，但输入栏位为 3×3 共 9 个格子。
 * 仅允许放入「人体派素材」与「人体派作品」两类物品。
 * 合成逻辑：累加 9 格内所有物品的四项内置值（Weapon/Bone/Meat/Blood），
 * 由 Weapon 值决定产出武器的等阶（2~6 阶），产出武器的四项数值 = 素材累加和。
 * 关闭窗口时，未消耗的材料会自动退还至英雄背包。</p>
 */
public class WndBodyCraft extends Window implements WindowReturnsItems {

	private static final int WIDTH        = 116;
	private static final int BTN_SIZE     = 30;   //单格 30px
	private static final int BTN_GAP      = 3;    //格子间距 3px
	private static final int GAP          = 2;

	private CraftSlot[] slots = new CraftSlot[9];        //3×3 合成格
	private CraftSlot slotPressed;                       //当前待填入的格
	private RedButton btnCraft;

	public WndBodyCraft( MasterRing masterRing ) {
		super();

		// --- 标题 ---
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( ItemSpriteSheet.MASTER_RING, null ) );
		titlebar.label( "创作" );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( 6 );
		message.text( "将素材或人体派作品放入 9 个格子，点击「合成」产出新的人体派作品。武器值决定等阶：<30 素材不足，30~59=2阶，60~89=3阶，90~119=4阶，120~149=5阶，≥150=6阶（提比娅）。创作即按超出本阶起始值的武器值给定初始等级（每 10 点 +1 级，上限 A+）。" );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		// --- 3×3 合成格 ---
		final float totalW = 3f * BTN_SIZE + 2f * BTN_GAP;
		float gridLeft = (WIDTH - totalW) / 2f;
		float gridTop  = message.bottom() + 4 * GAP;

		for (int idx = 0; idx < 9; idx++) {
			CraftSlot slot = new CraftSlot();
			int row = idx / 3;
			int col = idx % 3;
			slot.setRect(
					gridLeft + col * (BTN_SIZE + BTN_GAP),
					gridTop  + row * (BTN_SIZE + BTN_GAP),
					BTN_SIZE, BTN_SIZE );
			add( slot );
			slots[idx] = slot;
		}

		// --- 合成按钮 ---
		btnCraft = new RedButton( "合成" ) {
			@Override
			protected void onClick() {
				craft();
			}
		};
		btnCraft.enable( false );
		float gridBottom = gridTop + 3f * BTN_SIZE + 2f * BTN_GAP;
		btnCraft.setRect( 0, gridBottom + BTN_GAP, WIDTH, 20 );
		add( btnCraft );

		resize( WIDTH, (int)btnCraft.bottom() );
	}

	// 合成格：照抄 AlchemyScene.InputButton 的模式
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
					updateCraftButton();
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

			// 初始为空格占位
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

	// 关闭窗口时自动退回所有材料
	@Override
	public void onBackPressed() {
		returnAllItems();
		super.onBackPressed();
	}

	// 把 9 格内所有已放入物品退回背包（背包满则掉脚下）。
	// 供 onBackPressed / GameScene.onPause 收尾(returnItemsAndClose) / destroy 兜底调用——
	// 防止物品只存在于 UI 格子（已从背包 detach、不可存档），切后台/旋转/进程被杀后丢失。
	private void returnAllItems() {
		if (Dungeon.hero == null || Dungeon.level == null) return;
		for (CraftSlot s : slots) {
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

	// 启用合成按钮的条件：至少一格放了素材或人体派作品
	private void updateCraftButton() {
		boolean hasAny = false;
		for (CraftSlot s : slots) {
			if (s.item() != null) {
				hasAny = true;
				break;
			}
		}
		btnCraft.enable( hasAny );
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

	// --- 合成逻辑 ---
	private void craft() {
		// 1. 累加 9 格内所有物品的四项内置值
		int totalWeapon = 0, totalBone = 0, totalMeat = 0, totalBlood = 0;
		for (CraftSlot s : slots) {
			Item it = s.item();
			if (it instanceof BodyArtMaterial) {
				BodyArtMaterial m = (BodyArtMaterial) it;
				totalWeapon += m.weaponValue;
				totalBone   += m.boneValue;
				totalMeat   += m.meatValue;
				totalBlood  += m.bloodValue;
			} else if (it instanceof BodyArtWeapon) {
				BodyArtWeapon w = (BodyArtWeapon) it;
				totalWeapon += w.weaponValue;
				totalBone   += w.boneValue;
				totalMeat   += w.meatValue;
				totalBlood  += w.bloodValue;
			}
		}

		// 2. 由 Weapon 值决定等阶并生成武器
		BodyArtWeapon product = createProduct( totalWeapon, totalBone, totalMeat, totalBlood );
		if (product == null) {
			GLog.w( "素材不足以形成武器。" );
			return;
		}

		// 环指大师 T2：灵感——创作时消耗所有灵感层数，成品增加 2×层数的随机数值
		Inspiration insp = Dungeon.hero.buff( Inspiration.class );
		if (insp != null && insp.stacks > 0){
			int bonus = 2 * insp.stacks;
			for (int i = 0; i < bonus; i++){
				switch (com.watabou.utils.Random.Int(4)){
					case 0: product.weaponValue += 1; break;
					case 1: product.boneValue   += 1; break;
					case 2: product.meatValue   += 1; break;
					case 3: product.bloodValue  += 1; break;
				}
			}
			insp.detach();
			product.syncReinforceLevel();
			GLog.p( "灵感涌现：成品额外获得 _%d_ 点随机数值！", bonus );
		}

		// 3. 消耗所有格子内的物品
		for (CraftSlot s : slots) {
			s.item( null );
		}
		Item.updateQuickslot();
		updateCraftButton();

		// 4. 产出武器给英雄（背包满则丢到脚下）
		if (!product.collect( Dungeon.hero.belongings.backpack )) {
			Dungeon.level.drop( product, Dungeon.hero.pos ).sprite.drop();
		}
		GLog.p( "创作完成：%s！", product.title() );

		// TODO: 合成 VFX（参考炼金釜的粒子效果）
	}

	// 由 Weapon 值决定等阶，随机选择对应阶位的武器子类
	private BodyArtWeapon createProduct( int weaponVal, int boneVal, int meatVal, int bloodVal ) {
		// Weapon 值不足 30 时无法形成武器
		if (weaponVal < 30) {
			return null;
		}
		int tier;
		if      (weaponVal < 60)   tier = 2;
		else if (weaponVal < 90)   tier = 3;
		else if (weaponVal < 120)  tier = 4;
		else if (weaponVal < 150)  tier = 5;
		else                        tier = 6;

		BodyArtWeapon weapon;
		switch (tier) {
			case 2:
			default: {
				BodyArtWeapon[] opts = new BodyArtWeapon[]{ new CrudeWorkA(), new CrudeWorkB(), new CrudeWorkC() };
				weapon = opts[Random.Int( opts.length )];
				break;
			}
			case 3: {
				BodyArtWeapon[] opts = new BodyArtWeapon[]{ new CommonWorkA(), new CommonWorkB(), new CommonWorkC() };
				weapon = opts[Random.Int( opts.length )];
				break;
			}
			case 4: {
				BodyArtWeapon[] opts = new BodyArtWeapon[]{ new FineWorkA(), new FineWorkB(), new FineWorkC() };
				weapon = opts[Random.Int( opts.length )];
				break;
			}
			case 5: {
				BodyArtWeapon[] opts = new BodyArtWeapon[]{ new FamedWorkA(), new FamedWorkB(), new FamedWorkC() };
				weapon = opts[Random.Int( opts.length )];
				break;
			}
			case 6: {
				weapon = new LifeWorkTibia();
				break;
			}
		}

		// 产出武器的四项数值 = 素材累加和
		weapon.weaponValue = weaponVal;
		weapon.boneValue   = boneVal;
		weapon.meatValue   = meatVal;
		weapon.bloodValue  = bloodVal;
		weapon.identify();
		// 创作即可带有等级：Weapon 值超阈值部分转化为等级（上限6/A+）
		weapon.syncReinforceLevel();
		return weapon;
	}

	// 背包物品选择器：仅允许素材与人体派作品
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			return "选择素材或人体派作品放入合成格";
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			// 装备中的物品不可加入合成（会导致物品复制 bug）
			if (item.isEquipped( Dungeon.hero )) return false;
			// 仅允许人体派素材（魔质素材除外，usableInCraft=false）与人体派作品
			return (item instanceof BodyArtMaterial && ((BodyArtMaterial) item).usableInCraft())
					|| item instanceof BodyArtWeapon;
		}

		@Override
		public void onSelect( Item item ) {
			if (item != null && slotPressed != null && slotPressed.parent != null) {
				// 如果该格已有物品先退回
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
				updateCraftButton();
			}
		}
	};
}
