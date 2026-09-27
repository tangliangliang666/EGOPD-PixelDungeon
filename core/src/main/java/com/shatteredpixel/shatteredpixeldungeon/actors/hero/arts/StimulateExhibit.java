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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.arts;

import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.RingfingerAutomaton;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 展品技艺「激发」（tier 2，天赋「画廊即时修整」+3 解锁）：
 * 选择一只环指自动人偶，使其获得 10 回合的「激发」效果——
 * 每回合嘲讽附近所有敌人（众矢之的），且效果持续期间不会死亡
 * （效果实现见 {@link RingfingerAutomaton.Stimulated}）。
 * 使用后进入 150 回合冷却（2026-09-06 调整，冷却期间技艺置灰不可用）。
 */
public class StimulateExhibit extends RingMasterArt {

	public static final StimulateExhibit INSTANCE = new StimulateExhibit();

	/** 激发冷却时长（回合）。 */
	public static final int COOLDOWN = 150;

	private StimulateExhibit(){}

	@Override
	public int tier() {
		return 2;
	}

	@Override
	public boolean canPerform( Hero hero ) {
		if (hero.pointsInTalent( Talent.GALLERY_TOUCHUP ) < 3){
			return false;
		}
		//150 回合冷却中不可使用
		if (hero.buff( Cooldown.class ) != null){
			return false;
		}
		return !collectDolls().isEmpty();
	}

	@Override
	public void onPerform( Hero hero ) {
		selectDoll( "选择要激发的展品", new DollCallback() {
			@Override
			public void onDollSelected( RingfingerAutomaton doll ) {
				RingfingerAutomaton.Stimulated.apply( doll );

				//进入 150 回合冷却（2026-09-06 调整）
				Cooldown cd = Buff.affect( hero, Cooldown.class, COOLDOWN );
				if (cd != null) cd.maxCooldown = COOLDOWN;

				hero.spendAndNext( Actor.TICK );
				GLog.p( "环指自动人偶被_激发_了！它将在_10回合_内吸引所有敌人的攻击，并且不会死亡。" );
			}
		} );
	}

	@Override
	public String name() {
		return "激发";
	}

	@Override
	public String shortDesc() {
		return "使一只展品获得_10回合_众矢之的，且期间不会死亡（使用后_150回合_冷却）";
	}

	@Override
	public String desc() {
		return "画廊导师彻底激发展品的机能，将它推上战场的焦点。\n\n"
				+ "选择一只场上的环指自动人偶：\n"
				+ "_众矢之的_：_10回合_内，它每回合都会嘲讽附近的所有敌人，强迫它们攻击自己；\n"
				+ "_不死_：效果持续期间，无论受到怎样的伤害它都不会死亡（至少保留 1 点生命）。\n\n"
				+ "使用后进入_150回合_的_冷却_，冷却期间本技艺不可使用。\n\n"
				+ "需要天赋「画廊即时修整」_+3_。";
	}

	@Override
	public int icon() {
		return HeroIcon.ART_STIMULATE; //右上箭头图案（修整图标右侧）
	}

	/**
	 * 「激发」冷却（150 回合，2026-09-06）：
	 * 以倒计时图标显示剩余回合数（参考人体观剧 BodyTheaterCooldown 的写法）。
	 */
	public static class Cooldown extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			revivePersists = true;
		}

		public float maxCooldown = 0f; //本次冷却总时长（150，用于图标淡出比例）

		private static final String MAX_COOLDOWN = "max_cooldown";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( MAX_COOLDOWN, maxCooldown );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			maxCooldown = bundle.getFloat( MAX_COOLDOWN );
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public void tintIcon( Image icon ) {
			icon.hardlight( 0.9f, 0.45f, 0.15f ); //激发同色系（橙）
		}

		@Override
		public float iconFadePercent() {
			if (maxCooldown <= 0) return 1f;
			return Math.max( 0, 1f - visualcooldown() / maxCooldown );
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString( (int)Math.ceil( visualcooldown() ) );
		}

		@Override
		public String name() {
			return "激发冷却";
		}

		@Override
		public String desc() {
			return "「激发」技艺使用后，需要等待_150回合_才能再次使用。\n\n"
					+ "剩余 " + (int)Math.ceil( visualcooldown() ) + " 回合。";
		}
	}
}
