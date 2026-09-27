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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

/**
 * 预知眼-过热（奥丁之眼的惩罚 debuff，2026-09-06）
 *
 * <p>预知眼在充能耗尽且未被手动关闭时自动消失 → 获得本 debuff 30 回合。
 * 期间奥丁之眼无法使用 / 无法被自动触发，且物品栏/快捷栏中奥丁之眼显示过热形态贴图。</p>
 *
 * <p>独立顶层类（非 artifact 内部类）以支持 {@code Buff.affect} 的反射重建
 * （内部类无法被 Bundle 恢复）。</p>
 */
public class OdinsEyeOverheat extends FlavourBuff {

	public static final float DURATION = 30f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	@Override
	public int icon() {
		return BuffIndicator.ODINS_EYE_OVERHEAT;
	}

	//⚠️ 语音**不能**挂在本类的 act() 上：FlavourBuff 的 act() 语义是
	//「等 duration 到期 → detach」，整个生命周期只会被调用一次，
	//挂在那里等于在第 30 回合、过热即将结束时才播（2026-09-18 修）。
	//真正的播放点在授予处 {@link OdinsEye} 的 overheatPenalty()，与过热浮字同步。
	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			//过热形态贴图切换
			OdinsEye.refreshImageFor( target );
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		super.detach(); //先移除，使 target.buff() 判定失效，再恢复常态贴图
		OdinsEye.refreshImageFor( target );
	}
}
