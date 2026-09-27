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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Paralysis extends FlavourBuff {

	public static final float DURATION	= 10f;

	//出生保护：刚被施加的麻痹豁免「当次」破麻判定（伤害照常计入累计，但跳过全部破麻判定）。
	//用于防止战争英雄天赋二 +1「命中同击新上的麻痹」被同一次伤害结算立即掷掉——
	//下一次攻击即可正常走「解除麻痹」判定。瞬态标志，不存档。
	private transient boolean newborn = false;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}
	
	@Override
	public boolean attachTo( Char target ) {
		if (super.attachTo( target )) {
			target.paralysed++;
			newborn = true;   //仅在新建（append→attachTo）时置位；已有麻痹的刷新走 prolong 不经过此处
			return true;
		} else {
			return false;
		}
	}
	
	public void processDamage( int damage ){
		if (target == null) return;

		ParalysisResist resist = target.buff(ParalysisResist.class);
		if (resist == null){
			resist = Buff.affect(target, ParalysisResist.class);
		}
		resist.damage += damage;

		//出生保护：当次伤害结算豁免破麻判定（原生累计判定与震颤-灼热附加判定都不掷），
		//确保「本击新上的麻痹」能留到下一次攻击，由其正常触发解除判定与连锁。
		if (newborn){
			newborn = false;
			return;
		}

		//震颤-灼热（战争英雄 WAR_HERO 专属标记，2026-09-07）：每次实际受伤（damage>0）时，
		//额外掷一次独立破麻判定，概率 = 震颤-灼热层数 × 15%——对全部伤害源生效，与下方原生累计判定并存。
		boolean tremorBreak = false;
		if (damage > 0){
			TremblingScorch ts = target.buff( TremblingScorch.class );
			if (ts != null && Random.Int( 100 ) < 15 * ts.stacks){
				tremorBreak = true;
			}
		}
		if (tremorBreak || Random.NormalIntRange(0, resist.damage) >= Random.NormalIntRange(0, target.HP)){
			if (Dungeon.level.heroFOV[target.pos]) {
				target.sprite.showStatus(CharSprite.NEUTRAL, Messages.get(this, "out"));
			}
			detach();
		}
	}
	
	@Override
	public void detach() {
		super.detach();
		if (target.paralysed > 0)
			target.paralysed--;
	}
	
	@Override
	public int icon() {
		return BuffIndicator.PARALYSIS;
	}

	@Override
	public float iconFadePercent() {
		return Math.max(0, (DURATION - visualcooldown()) / DURATION);
	}

	@Override
	public void fx(boolean on) {
		if (on)                         target.sprite.add(CharSprite.State.PARALYSED);
		else if (target.paralysed <= 1) target.sprite.remove(CharSprite.State.PARALYSED);
	}

	public static class ParalysisResist extends Buff {
		
		{
			type = buffType.POSITIVE;
		}
		
		private int damage;
		
		@Override
		public boolean act() {
			if (target.buff(Paralysis.class) == null) {
				damage -= Math.ceil(damage / 10f);
				if (damage <= 0) detach();
			}
			spend(TICK);
			return true;
		}
		
		private static final String DAMAGE = "damage";
		
		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put( DAMAGE, damage );
		}
		
		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			damage = bundle.getInt(DAMAGE);
		}
	}
}
