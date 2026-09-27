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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 装备赫尔墨斯的双蛇杖时获得的常驻 buff「业」。
 * <p>持续时间无限（不覆盖 {@link #act()}，Buff 默认不会自行移除），卸下武器时由
 * {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HermesCaduceus} 解除。</p>
 * <p>层数范围 0~{@link #MAX}。图标暂时套用灵视效果（buff 贴图中的第一个，
 * {@link BuffIndicator#MIND_VISION}），效果与数值机制待后续设计。</p>
 */
public class Karma extends Buff {

	public static final int MIN = 0;
	public static final int MAX = 100;

	//中性 buff（默认即为 NEUTRAL，此处显式声明便于后续调整）
	{
		type = buffType.NEUTRAL;
		//核心常驻机制：十字架复活后保留（否则复活后不再接指令/标记）
		revivePersists = true;
	}

	private int karma = 0;

	public int karma(){
		return karma;
	}

	/**
	 * 当前业力上限。诸业加身（命运弃子 T2）天赋会降低上限：
	 * 1 点 → 80，2 点 → 60，3 点 → 40；未点则为 {@link #MAX}。
	 */
	public int maxKarma(){
		if (target instanceof Hero && ((Hero) target).hasTalent(Talent.KARMA_CAP)){
			int points = ((Hero) target).pointsInTalent(Talent.KARMA_CAP);
			return 100 - 20 * points; //80 / 60 / 40
		}
		return MAX;
	}

	public void setKarma(int value){
		int max = maxKarma();
		karma = Math.max(MIN, Math.min(max, value));
		BuffIndicator.refreshHero();
	}

	public void addKarma(int amount){
		int before = karma;
		setKarma(karma + amount);
		int gained = karma - before;
		//业增加时在头顶弹出淡蓝色提示
		if (gained > 0 && target instanceof Hero && target.sprite != null){
			target.sprite.showStatus(Instruction.LIGHT_BLUE, "业+" + gained);
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.KARMA; //自定义图标（原版最后一个buff右侧）
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(karma);
	}

	@Override
	public String name() {
		return "业";
	}

	@Override
	public String desc() {
		return "接受指令，并将其执行。这将化为一种业，于循环往复中不断积累。\n\n当前业力：" + karma + "。";
	}

	private static final String KARMA = "karma";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(KARMA, karma);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		karma = bundle.getInt(KARMA);
	}
}
