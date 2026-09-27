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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;

/**
 * 「作品细化」按钮 buff（已弃用）。
 * <p>原为艺术之巅分支的右下角 {@link ActionIndicator} 技能按钮占位；
 * 附魔功能已改为大师指环窗口内的「附魔」技艺
 * （{@code EnchantArt}，由艺术之巅天赋「学习未知之物」解锁）。
 * 本类保留仅为读档兼容：旧存档中残留的该 buff 在 attachTo 阶段被直接丢弃，
 * 不再注册右下角按钮。</p>
 */
public class ArtRefine extends Buff implements ActionIndicator.Action {

	{
		type = buffType.POSITIVE;
	}

	//永久 buff：不计时、不分离
	@Override
	public boolean act() {
		spend(TICK);
		return true;
	}

	@Override
	public String actionName() {
		return "作品细化";
	}

	@Override
	public int actionIcon() {
		//复用艺术之巅的职业图标（hero_icons.png 第二行第 8 列）
		return HeroIcon.ART_PINNACLE;
	}

	@Override
	public int indicatorColor() {
		return 0xFFFFFF; //白色
	}

	@Override
	public void doAction() {
		GLog.w("作品细化的具体机制仍在打磨中……");
	}

	//「作品细化」按钮已移除：返回 false 使旧存档中残留的本 buff 读档时被丢弃，不再注册右下角按钮
	@Override
	public boolean attachTo(Char target) {
		return false;
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	@Override
	public String name() {
		return "作品细化";
	}

	@Override
	public String desc() {
		return "艺术之巅追求作品的极致，能够将素材的价值以更精妙的方式注入人体派作品之中。\n\n" +
				"点击右下角的按钮（或按对应快捷键）进行作品细化。";
	}
}
