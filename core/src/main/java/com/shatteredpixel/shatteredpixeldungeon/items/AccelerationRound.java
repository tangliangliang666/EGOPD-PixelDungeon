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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AccelerationShot;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * 加速弹（拇指 前二老板 T2「加速弹药」）——弹药类消耗品。
 *
 * <p>贴图 xy(9,36) 13×13。商店老板处以 200 金 / 10 发出售（特殊窗口），
 * +2 时可在炼金釜用 20 液金 + 1 炼金能量合成 10 发。</p>
 *
 * <p>物品带「备弹」功能按钮：使用后获得「加速弹」buff（攻击延迟减半，
 * 每次攻击消耗 1 发；弹药耗尽或再次使用「备弹」时解除）。</p>
 */
public class AccelerationRound extends Item {

	public static final String AC_LOAD = "LOAD";

	//一次商店购买 / 一次炼金合成的数量
	public static final int PACK = 10;

	{
		image = ItemSpriteSheet.ACCELERATION_ROUND;

		stackable = true;
		defaultAction = AC_LOAD;

		bones = true;
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LOAD );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_LOAD )){
			AccelerationShot.toggle( hero );
			hero.sprite.operate( hero.pos );
		}
	}

	@Override
	public String info() {
		return super.info(); //name/desc 均来自本地化
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	@Override
	public int value() {
		return 0; //不可出售（只能通过商店购买 / 炼金合成获取）
	}

	//==========================================================================
	// 弹药统计与消耗
	//==========================================================================

	/** 统计背包（含各类收纳袋）中加速弹的总数。 */
	public static int count( Hero hero ){
		if (hero == null || hero.belongings == null) return 0;
		int total = 0;
		for (AccelerationRound r : hero.belongings.getAllItems( AccelerationRound.class )){
			total += r.quantity();
		}
		return total;
	}

	/** 消耗 1 发（从任意一叠扣除）。返回是否成功扣到。 */
	public static boolean consumeOne( Hero hero ){
		if (hero == null || hero.belongings == null) return false;
		for (AccelerationRound r : hero.belongings.getAllItems( AccelerationRound.class )){
			if (r.quantity() > 0){
				r.detach( hero.belongings.backpack );
				return true;
			}
		}
		return false;
	}

	/** 给予一包（10 发）：背包放不下则掉落在地上。 */
	public static void grantPack( Hero hero ){
		AccelerationRound rounds = new AccelerationRound();
		rounds.quantity( PACK );
		if (!rounds.collect( hero.belongings.backpack )){
			Dungeon.level.drop( rounds, hero.pos ).sprite.drop();
		}
		Item.updateQuickslot();
	}

	//==========================================================================
	// 炼金合成配方：20 液金 + 1 炼金能量 → 10 发（需「加速弹药」+2）
	//==========================================================================
	public static class CraftRecipe extends Recipe.SimpleRecipe {

		{
			inputs = new Class[]{ LiquidMetal.class };
			inQuantity = new int[]{ 20 };
			cost = 1; //1 炼金能量

			output = AccelerationRound.class;
			outQuantity = PACK;
		}

		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			//「加速弹药」+2 可用液金制造（不判职业：蜕变卷轴变出来的也算，见 Talent 的判据红线）
			if (Dungeon.hero == null
					|| !Dungeon.hero.hasTalent( Talent.ACCEL_AMMO )
					|| Dungeon.hero.pointsInTalent( Talent.ACCEL_AMMO ) < 2){
				return false;
			}
			return super.testIngredients( ingredients );
		}
	}
}
