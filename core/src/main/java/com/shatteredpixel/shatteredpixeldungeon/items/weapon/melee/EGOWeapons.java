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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

/**
 * E.G.O武器登记表：神谕代行者的自定义近战武器（2~5阶各四把）。
 * 用于炼金分解/重构配方与E.G.O标注。
 */
public class EGOWeapons {

	private static final Class<? extends MeleeWeapon>[] TIER_2 = new Class[]{
			RedEye.class, LastLight.class, WristSlit.class, Remorse.class
	};
	private static final Class<? extends MeleeWeapon>[] TIER_3 = new Class[]{
			BloodThirst.class, FrostShard.class, Oblivion.class, TinyGalaxy.class
	};
	private static final Class<? extends MeleeWeapon>[] TIER_4 = new Class[]{
			HolyDecree.class, LingeringScent.class, BlackSwan.class, CrimsonScar.class,
			VoidDiffraction.class, Binding.class, Blindness.class, Thirst.class
	};
	private static final Class<? extends MeleeWeapon>[] TIER_5 = new Class[]{
			Mimicry.class, SmilingBlade.class, DaCapo.class, JusticeArbiter.class, CensoredWeapon.class
	};
	//合成限定E.G.O武器（蜡翼，五阶，2026-09-08 用户定制）：仅可炼金合成获得，
	//参与E.G.O判定（可分解/标注），但<em>不</em>进入重构随机池与掉落池
	private static final Class<? extends MeleeWeapon>[] TIER_5_CRAFT_ONLY = new Class[]{
			WaxWing.class
	};
	//六阶E.G.O武器（失乐园/薄暝）：仅可炼金合成，不参与重构随机
	private static final Class<? extends MeleeWeapon>[] TIER_6 = new Class[]{
			ParadiseLost.class, DimDusk.class
	};

	public static boolean isEGO(Item item){
		return isEGO(item.getClass());
	}

	public static boolean isEGO(Class<?> cls){
		for (Class<?> c : TIER_2) if (c == cls) return true;
		for (Class<?> c : TIER_3) if (c == cls) return true;
		for (Class<?> c : TIER_4) if (c == cls) return true;
		for (Class<?> c : TIER_5) if (c == cls) return true;
		for (Class<?> c : TIER_5_CRAFT_ONLY) if (c == cls) return true;
		for (Class<?> c : TIER_6) if (c == cls) return true;
		return false;
	}

	public static int tierOf(Class<?> cls){
		for (Class<?> c : TIER_2) if (c == cls) return 2;
		for (Class<?> c : TIER_3) if (c == cls) return 3;
		for (Class<?> c : TIER_4) if (c == cls) return 4;
		for (Class<?> c : TIER_5) if (c == cls) return 5;
		for (Class<?> c : TIER_5_CRAFT_ONLY) if (c == cls) return 5;
		for (Class<?> c : TIER_6) if (c == cls) return 6;
		return -1;
	}

	//随机一把X阶E.G.O武器
	public static Class<? extends MeleeWeapon> randomOfTier(int tier){
		switch (tier){
			case 2: default: return Random.element(TIER_2);
			case 3: return Random.element(TIER_3);
			case 4: return Random.element(TIER_4);
			case 5: return Random.element(TIER_5);
		}
	}

}
