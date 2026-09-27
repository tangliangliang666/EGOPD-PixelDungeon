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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetMarker;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「狩猎目标」buff（拇指 前二老板 T2，图标 ID 103，挂在敌人身上）。
 *
 * <p>场上只能存在一个狩猎目标：前二老板每次攻击前会把目标标记为狩猎目标
 * （同一目标叠加层数、转移到其它目标时清空旧标记）。被标记的目标防御值下降
 * （层数×5，不低于 0），且每次被前二老板攻击时为前二老板恢复（层数×0.2）
 * 的剑术充能。</p>
 *
 * <p>参考「指令目标」{@link InstructionTarget}：在目标头顶叠加一张半透明的
 * 「巴勒莫对剑」贴图（{@link ItemSpriteSheet#PALERMO_SWORD}）跟随目标移动。</p>
 */
public class HuntingTarget extends Buff {

	{
		type = buffType.NEGATIVE;
	}

	//当前层数（同一目标连续被攻击时叠加）
	public int stacks = 1;

	private TargetMarker marker;

	/** 攻击开始前调用：把 {@code target} 标记为狩猎目标（转移/叠加语义）。 */
	public static void mark( Char target ){
		if (target == null || !target.isAlive()) return;

		//场上唯一：转移标记时清空原目标身上的狩猎目标
		if (Dungeon.level != null){
			for (Mob m : Dungeon.level.mobs){
				if (m != target && m.isAlive() && m.buff(HuntingTarget.class) != null){
					Buff.detach(m, HuntingTarget.class);
				}
			}
		}

		HuntingTarget ht = target.buff( HuntingTarget.class );
		if (ht == null){
			ht = Buff.affect( target, HuntingTarget.class );
			ht.stacks = 1;
		} else {
			ht.stacks++;
		}
		//层数上限 5（2026-09-08）
		if (ht.stacks > MAX_STACKS){
			ht.stacks = MAX_STACKS;
		}
	}

	/** 狩猎目标的最大叠层数 */
	public static final int MAX_STACKS = 5;

	/** 目标当前的狩猎目标层数（无标记返回 0）。 */
	public static int stacksOn( Char c ){
		if (c == null) return 0;
		HuntingTarget ht = c.buff( HuntingTarget.class );
		return ht == null ? 0 : ht.stacks;
	}

	@Override
	public void fx( boolean on ) {
		if (on){
			if (marker == null && target.sprite != null){
				marker = new TargetMarker( target, ItemSpriteSheet.PALERMO_SWORD );
				GameScene.effect( marker );
			}
		} else {
			if (marker != null){
				marker.killAndErase();
				marker = null;
			}
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.HUNTING_TARGET;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String name() {
		return Messages.get(this, "name");
	}

	@Override
	public String desc() {
		return Messages.get(this, "desc", stacks);
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = bundle.getInt(STACKS);
	}

	private static final String STACKS = "stacks";
}
