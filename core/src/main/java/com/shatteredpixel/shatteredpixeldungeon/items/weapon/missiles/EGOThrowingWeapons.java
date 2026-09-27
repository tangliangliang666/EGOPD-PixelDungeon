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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.watabou.utils.Random;

/**
 * E.G.O投掷武器登记表：神谕代行者的自定义投掷武器（4~5阶各一把）。
 * 用于炼金分解/重构配方与E.G.O标注（镜像近战 {@code EGOWeapons}）。
 * <p>已同步登记入 Generator 掉落池（{@code MIS_T4 / MIS_T5}，权重与同池原生一致），
 * 兼顾随机掉落与"脑啡肽 + 同阶普通投掷武器 → 重构"两条获取途径。</p>
 */
public class EGOThrowingWeapons {

	private static final Class<? extends MissileWeapon>[] TIER_4 = new Class[]{
			BuryingWedge.class
	};
	private static final Class<? extends MissileWeapon>[] TIER_5 = new Class[]{
			NovaVoice.class
	};

	public static boolean isEGO(Item item){
		return isEGO(item.getClass());
	}

	public static boolean isEGO(Class<?> cls){
		for (Class<?> c : TIER_4) if (c == cls) return true;
		for (Class<?> c : TIER_5) if (c == cls) return true;
		return false;
	}

	//当前是否存在某阶位的E.G.O投掷武器（重构投掷素材的阶位门槛）
	public static boolean supportsTier(int tier){
		return tier == 4 || tier == 5;
	}

	public static int tierOf(Class<?> cls){
		for (Class<?> c : TIER_4) if (c == cls) return 4;
		for (Class<?> c : TIER_5) if (c == cls) return 5;
		return -1;
	}

	//随机一把X阶E.G.O投掷武器（每阶目前仅一把，结果确定）
	public static Class<? extends MissileWeapon> randomOfTier(int tier){
		switch (tier){
			case 4: return Random.element(TIER_4);
			case 5: return Random.element(TIER_5);
			default: return null;
		}
	}

}
