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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts.EnchantArt;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.MagicMaterial;
import com.shatteredpixel.shatteredpixeldungeon.items.MasterRing;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
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
import com.watabou.utils.Random;
import com.watabou.utils.Reflection;

/**
 * 艺术之巅「附魔」窗口（大师指环技艺，结构照抄 {@link WndBodyReinforce}）。
 * <p>左侧 1 格放入人体派作品武器（附魔目标），右侧 2 格：上格仅放魔质素材，
 * 下格放任意物品（配方素材）。点击「附魔」消耗材料施加附魔：
 * 配方物品在 {@link EnchantArt#RECIPES} 注册过则施加对应附魔，
 * 否则按天赋「学习未知之物」允许的级别随机施加（+1 普通 common / +2 追加稀有 uncommon / +3 追加罕见 rare）。
 * 附魔可叠加：主附魔槽为空时填入主槽，否则追加进武器的附加附魔列表。关闭窗口时物品退回背包。</p>
 */
public class WndBodyEnchant extends Window implements WindowReturnsItems {

	private static final int WIDTH        = 116;
	private static final int LEFT_SIZE    = 44;   //左侧武器格
	private static final int RIGHT_SIZE   = 30;   //右侧材料格
	private static final int GAP          = 3;
	private static final int SLOT_AREA_H  = 2 * RIGHT_SIZE + GAP;  //右侧两格总高

	private CraftSlot leftSlot;          //附魔目标格
	private CraftSlot slotMagic;         //魔质素材格（仅魔质）
	private CraftSlot slotRecipe;        //配方物品格（任意物品）
	private CraftSlot slotPressed;        //当前待填入的格
	private RedButton btnEnchant;

	public WndBodyEnchant( Hero hero ) {
		super();

		// --- 标题 ---
		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( ItemSpriteSheet.MASTER_RING, null ) );
		titlebar.label( "附魔" );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		RenderedTextBlock message = PixelScene.renderTextBlock( 6 );
		message.text( "左侧放入人体派作品武器，右上放入魔质素材，右下放入对应配方物品。附魔会消耗材料为武器附加附魔；为已有附魔的作品附魔时，两种附魔会叠加生效。" );
		message.maxWidth( WIDTH );
		message.setPos( 0, titlebar.bottom() + GAP );
		add( message );

		float areaTop = message.bottom() + 3 * GAP;

		// --- 左侧武器格（垂直居中于材料区） ---
		leftSlot = new CraftSlot();
		float leftY = areaTop + (SLOT_AREA_H - LEFT_SIZE) / 2f;
		leftSlot.setRect( 8, leftY, LEFT_SIZE, LEFT_SIZE );
		add( leftSlot );

		// --- 右侧两格（纵向堆叠） ---
		slotMagic = new CraftSlot();
		slotMagic.setRect( 66, areaTop, RIGHT_SIZE, RIGHT_SIZE );
		add( slotMagic );

		slotRecipe = new CraftSlot();
		slotRecipe.setRect( 66, areaTop + RIGHT_SIZE + GAP, RIGHT_SIZE, RIGHT_SIZE );
		add( slotRecipe );

		// --- 附魔按钮 ---
		btnEnchant = new RedButton( "附魔" ) {
			@Override
			protected void onClick() {
				enchant();
			}
		};
		btnEnchant.enable( false );
		btnEnchant.setRect( 0, areaTop + SLOT_AREA_H + 2 * GAP, WIDTH, 20 );
		add( btnEnchant );

		resize( WIDTH, (int) btnEnchant.bottom() );
	}

	// 附魔格：照抄 WndBodyReinforce.CraftSlot 模式
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
						//已放物品：取回背包
						returnToBackpack( CraftSlot.this );
					} else {
						//空格：打开背包选择物品
						slotPressed = CraftSlot.this;
						GameScene.selectItem( itemSelector );
					}
					updateEnchantButton();
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

	// 把三格内所有已放入物品退回背包（背包满则掉脚下）。
	// 供 onBackPressed / GameScene.onPause 收尾(returnItemsAndClose) / destroy 兜底调用——
	// 防止物品只存在于 UI 格子（已从背包 detach、不可存档），切后台/旋转/进程被杀后丢失。
	private void returnAllItems() {
		if (Dungeon.hero == null || Dungeon.level == null) return;
		returnToBackpack( leftSlot );
		returnToBackpack( slotMagic );
		returnToBackpack( slotRecipe );
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

	private void updateEnchantButton() {
		btnEnchant.enable( leftSlot.item() instanceof BodyArtWeapon
				&& slotMagic.item() instanceof MagicMaterial
				&& slotRecipe.item() != null );
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

	// --- 附魔逻辑 ---
	private void enchant() {
		Item target = leftSlot.item();
		if (!(target instanceof BodyArtWeapon)) {
			GLog.w( "左侧需要放入人体派作品武器。" );
			return;
		}
		BodyArtWeapon weapon = (BodyArtWeapon) target;
		if (!(slotMagic.item() instanceof MagicMaterial)) {
			GLog.w( "右上需要放入魔质素材。" );
			return;
		}
		Item recipe = slotRecipe.item();
		if (recipe == null) {
			GLog.w( "右下需要放入配方物品。" );
			return;
		}

		int points = Dungeon.hero.pointsInTalent( Talent.LEARN_UNKNOWN );

		//配方→附魔映射；未注册的配方物品按天赋允许的级别随机（不含诅咒）
		Class<? extends Weapon.Enchantment> enchCls;
		if (EnchantArt.RECIPES.containsKey( recipe.getClass() )){
			enchCls = EnchantArt.RECIPES.get( recipe.getClass() );
		} else {
			switch (points){
				case 3:
					//普通/稀有/罕见按原版权重 50/40/10
					enchCls = Weapon.Enchantment.random().getClass();
					break;
				case 2:
					//普通/稀有加权随机（50:40 归一化）
					enchCls = (Random.chances( new float[]{50, 40} ) == 0
							? Weapon.Enchantment.randomCommon()
							: Weapon.Enchantment.randomUncommon()).getClass();
					break;
				case 1:
				default:
					enchCls = Weapon.Enchantment.randomCommon().getClass();
					break;
			}
		}

		//天赋门控：+1 普通（1）；+2 追加稀有（2）；+3 追加罕见（3）。诅咒（-1）永远不允许
		int enchTier = EnchantArt.enchantTier( enchCls );
		int maxTier = points;
		if (enchTier < 1 || enchTier > maxTier){
			GLog.w( "以当前的天赋等级，无法用_"
					+ recipe.name() + "_进行这种附魔。" );
			return; //不消耗材料
		}

		//施加附魔（可叠加）：主槽为空填主槽，否则追加进附加附魔列表
		Weapon.Enchantment ench = Reflection.newInstance( enchCls );
		String enchName = ench.name( "" );
		if (weapon.enchantment == null){
			weapon.enchant( ench );
		} else {
			weapon.addExtraEnchant( ench );
		}

		//消耗材料（放入格子时已 detach，清空即消耗）
		slotMagic.item( null );
		slotRecipe.item( null );

		if (weapon.enchantment == ench){
			GLog.p( "附魔成功！%s 获得了_" + enchName + "_附魔。", weapon.title() );
		} else {
			GLog.p( "附魔成功！%s 的附魔叠加了_" + enchName + "_。", weapon.title() );
		}

		//刷新左侧武器格显示
		leftSlot.item( weapon );
		Item.updateQuickslot();
		updateEnchantButton();
	}

	// 背包物品选择器：左侧仅人体派作品，右上仅魔质，右下任意物品
	protected WndBag.ItemSelector itemSelector = new WndBag.ItemSelector() {

		@Override
		public String textPrompt() {
			if (slotPressed == leftSlot){
				return "选择人体派作品武器放入左侧";
			} else if (slotPressed == slotMagic){
				return "选择魔质素材放入右上";
			} else {
				return "选择配方物品放入右下";
			}
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable( Item item ) {
			//装备中的物品不可加入（会导致物品复制 bug）
			if (item.isEquipped( Dungeon.hero )) return false;
			if (slotPressed == leftSlot) {
				return item instanceof BodyArtWeapon;
			}
			if (slotPressed == slotMagic) {
				return item instanceof MagicMaterial;
			}
			//右下配方格：任意物品
			return true;
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
				updateEnchantButton();
			}
		}
	};
}
