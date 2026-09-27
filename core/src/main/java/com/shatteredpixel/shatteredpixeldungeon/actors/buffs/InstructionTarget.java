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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.TargetMarker;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TalismanOfForesight;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 指令目标 buff：在目标怪物头顶显示额外的标记贴图。
 * <p>通过 {@link #fx(boolean)} 在 buff 附加/移除时创建/销毁标记
 * （{@link TargetMarker}，静态贴图、跟随目标移动、半透明，无动态特效）。
 * 标记贴图为自定义贴图（指令终端右侧）。</p>
 * <p>维护规则（由 {@link OracleMarkTracker} 每回合调用 {@link #updateMark()}）：
 * 当角色为神谕代行者且本层有存活怪物时，随机选取一个存活怪物挂上标记（持续时间永久）；
 * 被标记的怪物死亡后（buff 随死亡清理），自动将标记挂给其他存活怪物。</p>
 */
public class InstructionTarget extends Buff {

	private TargetMarker marker;

	/**
	 * 给目标挂上指令目标标记（幂等：已有则不做任何事）。
	 */
	public static void mark( Char target ){
		if (target != null && target.isAlive()){
			Buff.affect(target, InstructionTarget.class);
			//指令感知（T2）：对指令对象获得灵视感知（参考奥术感知/CharAwareness）
			if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.INSTRUCTION_SENSE)){
				int dur = 10 + 10 * Dungeon.hero.pointsInTalent(Talent.INSTRUCTION_SENSE);
				Buff.append(Dungeon.hero, TalismanOfForesight.CharAwareness.class, dur).charID = target.id();
			}
			//命运弃子专精天赋：指令目标获得残废(+1)/恍惚(+2)/定命(+3)
			if (Dungeon.hero != null && Dungeon.hero.subClass == HeroSubClass.FATE_FORSAKEN
					&& Dungeon.hero.hasTalent(Talent.FORSAKEN_MARK)){
				int points = Dungeon.hero.pointsInTalent(Talent.FORSAKEN_MARK);
				if (points >= 3){
					Buff.affect(target, Doom.class); //定命：永久
				} else if (points == 2){
					Buff.prolong(target, Daze.class, 999f); //恍惚：999回合
				} else if (points == 1){
					Buff.prolong(target, Cripple.class, 999f); //残废：999回合
				}
			}
		}
	}

	/**
	 * 维护指令标记：确保神谕代行者在有存活怪物的楼层时，恰好有一个随机存活怪物被标记。
	 */
	public static void updateMark(){
		if (Dungeon.hero == null || Dungeon.level == null) return;
		if (Dungeon.hero.heroClass != HeroClass.ORACLE) return;

		//已有存活的标记目标（且该目标仍是敌人）
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.buff(InstructionTarget.class) != null){
				//持续刷新指令感知：对指令对象保持灵视感知
				if (Dungeon.hero != null && Dungeon.hero.hasTalent(Talent.INSTRUCTION_SENSE)){
					Buff.prolong(Dungeon.hero, TalismanOfForesight.CharAwareness.class, 20f).charID = m.id();
				}
				return; //已有标记
			}
		}

		//无存活标记：随机选取一个存活的敌对怪物
		ArrayList<Mob> candidates = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m.isAlive() && m.alignment == Char.Alignment.ENEMY && m.buff(InstructionTarget.class) == null){
				candidates.add(m);
			}
		}
		if (!candidates.isEmpty()){
			//统一走 mark()（包含指令感知：对指令对象挂灵视感知）
			mark(Random.element(candidates));
		} else if (Dungeon.hero.hasTalent(Talent.INSTRUCTION_SENSE)
				&& Dungeon.hero.pointsInTalent(Talent.INSTRUCTION_SENSE) == 2){
			//指令感知 +2：本层没有指令对象时，获得极速
			Buff.prolong(Dungeon.hero, Haste.class, 2f);
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on){
			if (marker == null && target.sprite != null){
				marker = new TargetMarker( target, ItemSpriteSheet.TARGET_MARK );
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
		return BuffIndicator.INSTRUCTION_TARGET;
	}

	@Override
	public String name() {
		return "指令目标";
	}

	@Override
	public String desc() {
		return "这个单位受到指令的标记，成为指令的执行对象";
	}

}
