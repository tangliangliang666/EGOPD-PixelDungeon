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

package com.shatteredpixel.shatteredpixeldungeon.actors.mobs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Daze;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Doom;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Dread;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Terror;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MasterpieceSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

/**
 * 「杰作」单位（2026-09-04，环指大师盔甲技能「闭馆」召唤）。
 * <p>一个有贴图、<b>固定不动</b>的中立单位：不移动、不攻击、免疫一切伤害，
 * 持续 {@link #TURNS}（50）回合后自行消失（随楼层存档，读档自动恢复）。
 * 作为真实单位出现，灵视/查看均可发现它。</p>
 * <p>在场期间每回合对「视野内目睹杰作」的敌方单位按「请尽情欣赏吧」天赋施加恐惧系状态
 * （N 点叠加前 N 种）：+1 恐惧({@link Terror})、+2 恍惚({@link Daze})、
 * +3 定命({@link Doom}，永久)、+4 极度恐慌({@link Dread})。</p>
 */
public class Masterpiece extends Mob {

	public static final int TURNS = 50; //陈列回合数

	protected int turnsLeft = TURNS;

	{
		spriteClass = MasterpieceSprite.class;

		alignment = Alignment.NEUTRAL;
		HP = HT = 10000; //数值无关：杰作免疫一切伤害
		defenseSkill = 0;
		EXP = 0;
		maxLvl = 0;

		state = PASSIVE; //不会主动行动/接敌
	}

	/**
	 * 在英雄身旁（8 邻格）陈列一件杰作；优先选择英雄视野内（heroFOV）的可站空格，
	 * 视野内无空位则退回任意可站空格；完全没有可站空格时返回 null（不生成）。
	 */
	public static Masterpiece exhibit( Hero hero ){
		//先找英雄视野内的空格（确保召唤后立即可见）
		for (int i : PathFinder.NEIGHBOURS8){
			int c = hero.pos + i;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null
					&& Dungeon.level.heroFOV[c]){
				Masterpiece m = new Masterpiece();
				m.pos = c;
				m.turnsLeft = TURNS;
				GameScene.add( m );
				return m;
			}
		}
		//视野内无空位：退回任意空位（精灵常驻可见，雾外区域仍会显示在已访问的地图上）
		for (int i : PathFinder.NEIGHBOURS8){
			int c = hero.pos + i;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
				Masterpiece m = new Masterpiece();
				m.pos = c;
				m.turnsLeft = TURNS;
				GameScene.add( m );
				return m;
			}
		}
		return null;
	}

	//固定不动：只倒计时、对目睹者施加 debuff，不走任何 AI
	@Override
	protected boolean act() {
		turnsLeft--;
		if (turnsLeft <= 0){
			vanish();
			return true;
		}
		applyWitnessDebuffs();
		spend( TICK );
		return true;
	}

	//每回合对「英雄视野内」（展厅灯光下）目睹杰作的敌人施加恐惧系状态
	private void applyWitnessDebuffs(){
		Hero hero = Dungeon.hero;
		if (hero == null) return;
		int points = hero.pointsInTalent(Talent.ENJOY_THE_SHOW);
		if (points <= 0) return;

		for (Char ch : Actor.chars().toArray( new Char[0] )){
			if (ch == this || ch == hero || !ch.isAlive() || ch.alignment != Char.Alignment.ENEMY) continue;
			if (!Dungeon.level.heroFOV[ch.pos]) continue;

			if (points >= 1) Buff.affect( ch, Terror.class, Terror.DURATION );
			if (points >= 2) Buff.affect( ch, Daze.class, Daze.DURATION );
			if (points >= 3 && ch.buff(Doom.class) == null) Buff.affect( ch, Doom.class );
			if (points >= 4){
				//Dread 会覆盖 Terror（原版行为：dread overrides terror）
				Dread dread = ch.buff(Dread.class);
				if (dread == null) Buff.affect( ch, Dread.class );
			}
		}
	}

	//时间到：从场上消失（不产生击杀/掉落/徽章）
	private void vanish(){
		if (sprite != null) sprite.killAndErase();
		destroy();
	}

	//杰作不可被伤害（区域伤害/陷阱/敌方攻击均无效）
	@Override
	public void damage( int dmg, Object src ){
		//do nothing
	}

	@Override
	public String description() {
		return "一件被安静陈列的杰作。它只是立在那里，并不会去妨碍任何人的行动。";
	}

	private static final String TURNS_LEFT = "turns_left";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( TURNS_LEFT, turnsLeft );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		turnsLeft = bundle.getInt( TURNS_LEFT );
		if (turnsLeft <= 0) turnsLeft = 1; //防御：至少再存续一回合完成展示清理
	}
}
