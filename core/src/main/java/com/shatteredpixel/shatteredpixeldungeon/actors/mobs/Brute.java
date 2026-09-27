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
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.AscensionChallenge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ShieldBuff;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.items.Gold;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.Chasm;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.BruteSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

public class Brute extends Mob {
	
	{
		spriteClass = BruteSprite.class;
		
		HP = HT = 40;
		defenseSkill = 15;
		
		EXP = 8;
		maxLvl = 16;
		
		loot = Gold.class;
		lootChance = 0.5f;
	}
	
	protected boolean hasRaged = false;
	
	@Override
	public int damageRoll() {
		return buff(BruteRage.class) != null ?
				Random.NormalIntRange( 15, 40 ) :
				Random.NormalIntRange( 5, 25 );
	}
	
	@Override
	public int attackSkill( Char target ) {
		return 20;
	}
	
	@Override
	public int drRoll() {
		return super.drRoll() + Random.NormalIntRange(0, 8);
	}

	@Override
	public void die(Object cause) {
		super.die(cause);

		if (cause == Chasm.class){
			hasRaged = true; //don't let enrage trigger for chasm deaths
		}
	}

	//cache this buff to prevent having to call buff(...) a bunch in isAlive
	private BruteRage rage;

	@Override
	public boolean isAlive() {
		if (super.isAlive()){
			return true;
		} else {
			if (!hasRaged){
				triggerEnrage();
			}
			if (rage == null){
				for (BruteRage b : buffs(BruteRage.class)){
					rage = b;
				}
			}
			return rage != null && rage.shielding() > 0;
		}
	}
	
	protected void triggerEnrage(){
		rage = Buff.affect(this, BruteRage.class);
		rage.setShield(HT/2 + 4);
		sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString(HT/2), FloatingText.SHIELDING );
		if (Dungeon.level.heroFOV[pos]) {
			SpellSprite.show( this, SpellSprite.BERSERK);
		}
		spend( TICK );
		hasRaged = true;
	}
	
	private static final String HAS_RAGED = "has_raged";
	
	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(HAS_RAGED, hasRaged);
	}
	
	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		hasRaged = bundle.getBoolean(HAS_RAGED);
	}
	
	public static class BruteRage extends ShieldBuff {
		
		{
			type = buffType.POSITIVE;
		}
		
		@Override
		public boolean act() {
			
			if (target.HP > 0){
				detach();
				return true;
			}
			
			absorbDamage( Math.round(4*AscensionChallenge.statModifier(target)));
			
			if (shielding() <= 0){
				//GEBURA（严厉）：护盾耗尽＝这只怪战续到头的真正死亡时刻。原版这里直接 die(null)，
				//绕过了 Char.damage 的致死闸门，所以把闸门补在这里 —— 位置取该路径最靠后的点
				//（紧贴原版那句 die 之前），战续耗尽的判定链因此与「被打死」完全一致。
				//返回 false 表示考验未接管（没开/已用过一次的豁免/联盟单位/无经验值…）⇒ 照原版立即倒下。
				//接管时跳过 die(null)：血量停在 0、进入无敌，等计时结束由 GeburaGrace 用原始来源调 die()。
				//⚠️ 此处 BruteRage 已由上面的 absorbDamage（detachesAtZero ⇒ shielding 归零即 detach）
				//   自行摘掉，所以无敌期间不会一边无敌一边回盾；即使没摘，isAlive() 也会因 geburaGrace
				//   为真而短路，不会再走到 triggerEnrage。
				if (!Trials.interceptLethalDamage( target, null )){
					target.die(null);
				}
			}
			
			spend( TICK );
			
			return true;
		}
		
		@Override
		public void detach() {
			super.detach();
			decShield(shielding()); //clear shielding to track that this was detached
		}

		@Override
		public int icon () {
			return BuffIndicator.FURY;
		}
		
		@Override
		public String desc () {
			return Messages.get(this, "desc", shielding());
		}

	}
}
