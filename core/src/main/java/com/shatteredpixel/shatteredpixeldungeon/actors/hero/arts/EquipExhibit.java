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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BodyArtWeapon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;

/**
 * 展品技艺「装备」（tier 2，天赋「作品交叉展览」+1 解锁）：
 * 选择一只环指自动人偶，再从背包中选择一件人体派作品作为它的武器
 * （实现模式参考枯萎玫瑰悲伤幽灵的借武器：武器脱离背包、随人偶存档、人偶死亡时掉落）。
 * <p>可装备的作品 tier 受天赋点数限制：
 * +1 = 试作/劣作（tier ≤ 2）；+2 = 凡作/良作（tier ≤ 4）；+3 = 任意人体派作品。</p>
 */
public class EquipExhibit extends RingMasterArt {

	public static final EquipExhibit INSTANCE = new EquipExhibit();

	private EquipExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	/** 当前天赋点数允许装备的最高作品 tier（+1：试作/劣作 ≤2；+2：凡作/良作 ≤4；+3：任意）。 */
	private static int maxTier( Hero hero ){
		int p = hero.pointsInTalent( Talent.CROSS_EXHIBITION );
		if (p >= 3) return Integer.MAX_VALUE;
		if (p == 2) return 4;
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		return hero.pointsInTalent( Talent.CROSS_EXHIBITION ) >= 1
				&& !collectDolls().isEmpty();
	}

	@Override
	public void onPerform( Hero hero ) {
		final int maxTier = maxTier( hero );
		selectDoll( "选择要装备作品的展品", new DollCallback() {
			@Override
			public void onDollSelected( final RingfingerAutomaton doll ) {
				GameScene.selectItem( new WndBag.ItemSelector() {
					@Override
					public String textPrompt() {
						return "选择一件人体派作品装备给环指自动人偶";
					}

					@Override
					public Class<? extends Bag> preferredBag() {
						return Belongings.Backpack.class;
					}

					@Override
					public boolean itemSelectable( Item item ) {
						//已装备在英雄身上的作品不可选（防止装备槽物品被复制的坑）
						return item instanceof BodyArtWeapon
								&& !item.isEquipped( Dungeon.hero )
								&& ((BodyArtWeapon) item).tier <= maxTier;
					}

					@Override
					public void onSelect( Item item ) {
						if (item == null){
							return; //取消选择
						}
						BodyArtWeapon weapon = (BodyArtWeapon) item;

						if (weapon.isEquipped( Dungeon.hero ) || weapon.tier > maxTier){
							GLog.w( "这件作品不能装备给展品。" );
							return;
						}

						//人偶已持有作品时，旧作品掉落在英雄脚下
						BodyArtWeapon old = doll.weapon();
						if (old != null){
							Dungeon.level.drop( old, Dungeon.hero.pos ).sprite.drop( Dungeon.hero.pos );
						}

						weapon.detach( Dungeon.hero.belongings.backpack );
						doll.setWeapon( weapon );

						Dungeon.hero.spendAndNext( Actor.TICK );
						GLog.p( "环指自动人偶接过了_" + weapon.name() + "_，将它握在手中。" );
					}
				} );
			}
		} );
	}

	@Override
	public String name() {
		return "装备";
	}

	@Override
	public String shortDesc() {
		return "让一只展品装备人体派作品作为武器";
	}

	@Override
	public String desc() {
		return "画廊导师将自己的作品交给展品使用（作品交叉展览）。\n\n"
				+ "选择一只场上的环指自动人偶，再从背包中选择一件_人体派作品_作为它的武器：" +
				"人偶的命中、攻速、攻击距离、伤害、格挡都会按该作品的属性结算。\n\n"
				+ "可装备的作品受天赋点数限制：\n"
				+ "_+1：_试作、劣作；_+2：_凡作、良作；_+3：_任意人体派作品。\n\n"
				+ "人偶_死亡时会掉落_它装备的作品；为它更换作品时，旧作品会掉落在画廊导师脚下。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_EQUIP; //剑形图案（创作图标正下方）
	}
}
