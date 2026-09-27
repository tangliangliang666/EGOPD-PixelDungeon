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

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.EGOWeapons;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MeleeWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.EGOThrowingWeapons;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.MissileWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles.darts.Dart;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Reflection;

import java.util.ArrayList;

/**
 * 脑啡肽：从"异想体"中获得的能源，可由E.G.O武器分解而来，也可用于重构E.G.O武器。
 * <p>分解：X阶E.G.O武器 → (X-1)×5 个脑啡肽；
 * 重构：X×5 个脑啡肽 + 一把X阶武器 → 随机一把X阶E.G.O武器。</p>
 */
public class Enkephalin extends Item {

	{
		image = ItemSpriteSheet.ENKEPHALIN;

		stackable = true;
	}

	@Override
	public boolean isUpgradable() {
		return false;
	}

	@Override
	public boolean isIdentified() {
		return true;
	}

	//近战与投掷E.G.O武器的通用阶位读取（两族武器均自带 tier 字段）
	private static int tierOf(Item it){
		if (it instanceof MeleeWeapon) return ((MeleeWeapon) it).tier;
		if (it instanceof MissileWeapon) return ((MissileWeapon) it).tier;
		return -1;
	}

	//E.G.O武器分解为脑啡肽：X阶武器（近战或投掷） → (X-1)×5 个
	public static class DecomposeRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 1) return false;
			Item it = ingredients.get(0);
			//2026-09-08：不再要求 isIdentified()——EGO武器未鉴定状态也可直接分解为脑啡肽；
			//已确认诅咒的仍禁止。2026-09-09：支持E.G.O投掷武器（同公式）。
			return (it instanceof MeleeWeapon && EGOWeapons.isEGO(it)
					|| it instanceof MissileWeapon && EGOThrowingWeapons.isEGO(it))
					&& !it.cursed;
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Item it = ingredients.get(0);
			int tier = tierOf(it);
			it.quantity(0); //消耗武器
			return new Enkephalin().quantity((tier - 1) * 5);
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			if (ingredients == null || ingredients.isEmpty()) return null;
			int tier = tierOf(ingredients.get(0));
			if (tier == -1) return null;
			return new Enkephalin().quantity((tier - 1) * 5);
		}
	}

	//脑啡肽重构E.G.O武器：X×5 个脑啡肽 + 一把X阶武器 → 随机一把X阶E.G.O武器
	//近战素材 → 随机近战E.G.O（沿用旧逻辑）；同阶普通投掷素材 → 对应阶E.G.O投掷武器
	public static class ReconstructRecipe extends Recipe {

		@Override
		public boolean testIngredients(ArrayList<Item> ingredients) {
			if (ingredients.size() != 2) return false;
			Enkephalin e = null;
			Item weapon = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin){
					e = (Enkephalin) it;
				} else if (isEligibleBase(it)){
					weapon = it;
				} else {
					return false;
				}
			}
			return e != null && weapon != null && e.quantity() >= tierOf(weapon) * 5;
		}

		//重构素材判定：已鉴定且非诅咒的武器。
		//近战分支沿用旧行为（任意近战皆可）；投掷分支排除飞镖族与E.G.O投掷本体，
		//且只接受存在对应E.G.O投掷的阶位（4/5）。
		private static boolean isEligibleBase(Item it){
			if (!it.isIdentified() || it.cursed) return false;
			if (it instanceof MeleeWeapon) return true;
			if (it instanceof MissileWeapon){
				return !(it instanceof Dart)
						&& !EGOThrowingWeapons.isEGO(it)
						&& EGOThrowingWeapons.supportsTier(((MissileWeapon) it).tier);
			}
			return false;
		}

		//按素材类别返回对应E.G.O登记表的随机产物类
		private static Class<? extends Item> outputOf(Item weapon){
			if (weapon instanceof MissileWeapon){
				return EGOThrowingWeapons.randomOfTier(((MissileWeapon) weapon).tier);
			}
			return EGOWeapons.randomOfTier(((MeleeWeapon) weapon).tier);
		}

		@Override
		public int cost(ArrayList<Item> ingredients) {
			return 0;
		}

		@Override
		public Item brew(ArrayList<Item> ingredients) {
			if (!testIngredients(ingredients)) return null;
			Enkephalin e = null;
			Item weapon = null;
			for (Item it : ingredients){
				if (it instanceof Enkephalin) e = (Enkephalin) it;
				else weapon = it;
			}
			int tier = tierOf(weapon);
			e.quantity(e.quantity() - tier * 5);
			weapon.quantity(0); //消耗武器
			return Reflection.newInstance(outputOf(weapon));
		}

		@Override
		public Item sampleOutput(ArrayList<Item> ingredients) {
			if (ingredients == null || ingredients.isEmpty()) return null;
			for (Item it : ingredients){
				if (it instanceof MissileWeapon
						&& EGOThrowingWeapons.supportsTier(((MissileWeapon) it).tier)){
					return Reflection.newInstance(EGOThrowingWeapons.randomOfTier(((MissileWeapon) it).tier));
				}
			}
			for (Item it : ingredients){
				if (it instanceof MeleeWeapon){
					return Reflection.newInstance(EGOWeapons.randomOfTier(tierOf(it)));
				}
			}
			return null;
		}
	}

}
