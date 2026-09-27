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

package com.shatteredpixel.shatteredpixeldungeon.items.food;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Hunger;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Vertigo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Recipe;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

import java.util.ArrayList;

//酒水（拇指 前二老板 T1「备用酒水」专属掉落物）：饮下可回复饱食度，但会眩晕20回合
public class WineBottle extends Food {

	public static final float DIZZY_TURNS = 20f;

	{
		image = ItemSpriteSheet.WINE_BOTTLE;
		energy = Hunger.HUNGRY; //饱食度 300（介于干粮 200 与馅饼 450 之间，数值可调）

		bones = false;
	}

	@Override
	protected float eatingTime(){
		//饮酒动作较快，仅需 1 回合
		return TIME_TO_EAT - 2;
	}

	@Override
	protected void satisfy(Hero hero) {
		super.satisfy(hero);

		//后劲十足：获得 20 回合眩晕
		Buff.prolong(hero, Vertigo.class, DIZZY_TURNS);
		GLog.n(Messages.get(this, "dizzy"));
	}

	@Override
	public int value() {
		return 12 * quantity;
	}

	//==========================================================================
	// 炼金配方：任意药水 → 酒水（拇指 前二老板 T2「液蕴酒精」）
	// +1 需 3 瓶；+2 需 2 瓶。无额外炼金能量消耗。
	//==========================================================================
	public static class Recipe extends com.shatteredpixel.shatteredpixeldungeon.items.Recipe {

		@Override
		public boolean testIngredients( ArrayList<Item> ingredients ) {
			if (!enabled()) return false;
			return countPotions(ingredients) >= potionsNeeded();
		}

		@Override
		public int cost( ArrayList<Item> ingredients ) {
			return 0;
		}

		@Override
		public Item brew( ArrayList<Item> ingredients ) {
			if (!testIngredients(ingredients)) return null;

			int needed = potionsNeeded();
			for (Item ingredient : ingredients){
				if (needed <= 0) break;
				if (ingredient instanceof Potion){
					int q = ingredient.quantity();
					if (q <= needed){
						needed -= q;
						ingredient.quantity(0);
					} else {
						ingredient.quantity(q - needed);
						needed = 0;
					}
				}
			}

			return sampleOutput(null);
		}

		@Override
		public Item sampleOutput( ArrayList<Item> ingredients ) {
			return new WineBottle();
		}

		//技能是否可用（拇指 前二老板「液蕴酒精」至少 1 点）
		private static boolean enabled(){
			return Dungeon.hero != null
					&& Dungeon.hero.heroClass == HeroClass.VALENCINA
					&& Dungeon.hero.hasTalent(Talent.LIQUID_ALCOHOL);
		}

		//需要消耗的药水总数：+1 → 3 瓶；+2 → 2 瓶
		private static int potionsNeeded(){
			return 4 - Dungeon.hero.pointsInTalent(Talent.LIQUID_ALCOHOL);
		}

		private static int countPotions( ArrayList<Item> ingredients ){
			int total = 0;
			for (Item i : ingredients){
				if (i instanceof Potion) total += i.quantity();
			}
			return total;
		}
	}

}
