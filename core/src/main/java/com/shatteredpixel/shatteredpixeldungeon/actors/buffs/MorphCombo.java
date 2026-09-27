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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DarkSilence;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MorphWeapon;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * 漆黑噤默系列专属连击层数 buff。
 * <p>使用系列中<b>不同</b>的武器命中敌人时 +1 层并刷新持续时间；同一武器重复命中
 * 不叠层（也不刷新）。最多累计 {@link #MAX_COMBO} 层（九种形态各一次），
 * 漆黑噤默本体不参与叠层。</p>
 * <p>图标套用武僧连击图标（{@link BuffIndicator#COMBO}），颜色随层数变化。</p>
 */
public class MorphCombo extends FlavourBuff {

	//持续回合数，命中不同武器时刷新（可调）
	public static final float DURATION = 150f;

	//最大层数：九种形态各一次
	public static final int MAX_COMBO = 9;

	{
		type = buffType.POSITIVE;
	}

	private HashSet<Class<? extends Weapon>> usedWeapons = new HashSet<>();

	public int comboCount(){
		return usedWeapons.size();
	}

	/**
	 * 记录一次命中。若该武器形态尚未在本buff持续期内使用过，则叠层成功。
	 *
	 * @return true 表示叠层成功（新形态），false 表示重复形态或已达上限。
	 */
	public boolean recordHit(Class<? extends Weapon> weaponClass){
		if (weaponClass == null || usedWeapons.size() >= MAX_COMBO){
			return false;
		}
		return usedWeapons.add(weaponClass);
	}

	/** 该形态是否已在本buff持续期内使用过（供 UI 展示，如切换窗口标记）。 */
	public boolean hasUsed(Class<? extends Weapon> weaponClass){
		return usedWeapons.contains(weaponClass);
	}

	@Override
	public int icon() {
		return BuffIndicator.COMBO;
	}

	@Override
	public void tintIcon(Image icon) {
		int count = comboCount();
		if (count >= 9)      icon.hardlight(1.00f, 0.00f, 0.00f); //红
		else if (count >= 7) icon.hardlight(1.00f, 0.55f, 0.00f); //橙
		else if (count >= 5) icon.hardlight(1.00f, 1.00f, 0.00f); //黄
		else if (count >= 3) icon.hardlight(0.55f, 1.00f, 0.00f); //黄绿
		else if (count >= 1) icon.hardlight(0.00f, 1.00f, 0.00f); //绿
		else                 icon.resetColor();
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(comboCount());
	}

	@Override
	public String name() {
		return "漆黑噤默";
	}

	@Override
	public String desc() {
		StringBuilder sb = new StringBuilder();
		sb.append("使用漆黑噤默系列中不同的武器命中敌人，可以叠加连击层数并刷新持续时间。")
				.append("同一武器重复命中无法叠层。");

		//形态清单：按切换窗口九宫格的顺序枚举，已使用的形态用 _高亮_ 标出（本体不参与叠层，不列出）
		StringBuilder used = new StringBuilder();
		StringBuilder unused = new StringBuilder();
		for (Class<? extends Weapon> cls : MorphWeapon.ALL_FORMS){
			if (cls == DarkSilence.class){
				continue;
			}
			String name = MorphWeapon.formName(cls);
			if (usedWeapons.contains(cls)){
				if (used.length() > 0) used.append("、");
				used.append("_").append(name).append("_");
			} else {
				if (unused.length() > 0) unused.append("、");
				unused.append(name);
			}
		}

		sb.append("\n\n已使用（").append(comboCount()).append("/").append(MAX_COMBO).append("）：")
				.append(used.length() > 0 ? used.toString() : "无");
		if (unused.length() > 0){
			sb.append("\n尚未使用：").append(unused);
		}

		sb.append("\n\n当前层数：").append(comboCount())
				.append("，剩余 ").append(dispTurns()).append(" 回合。");
		return sb.toString();
	}

	private static final String USED = "used";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		String[] names = new String[usedWeapons.size()];
		int i = 0;
		for (Class<? extends Weapon> c : usedWeapons){
			names[i++] = c.getName();
		}
		bundle.put(USED, names);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		usedWeapons.clear();
		for (String name : bundle.getStringArray(USED)){
			try {
				Class<?> cls = Class.forName(name);
				if (Weapon.class.isAssignableFrom(cls)){
					usedWeapons.add((Class<? extends Weapon>) cls);
				}
			} catch (ClassNotFoundException e) {
				//ignored: weapon form no longer exists
			}
		}
	}
}
