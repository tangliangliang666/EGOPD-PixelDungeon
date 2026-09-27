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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.ScarletParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;

//猩红创痕：四阶近战武器，攻击延迟0.5（非常快），斩击音效，武技配置套用连击武技（Gauntlet）。
//效果：生命值低于50%时攻击造成额外50%伤害，低于25%时改为100%额外伤害；
//生命值低于50%时角色身上产生向上漂浮的红色粒子效果，低于25%时密度增加。
public class CrimsonScar extends Gauntlet {

	{
		image = ItemSpriteSheet.CRIMSON_SCAR;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 4;
		DLY = 0.5f; //2x speed
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  10 +  //base
				3*lvl;   //level scaling
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//装备时挂载猩红之怒（粒子效果）buff
		if (ch instanceof Hero){
			Buff.affect(ch, CrimsonAura.class);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			Buff.detach(hero, CrimsonAura.class);
			return true;
		} else {
			return false;
		}
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		//生命值低于50%时攻击造成额外50%伤害，低于25%时改为100%额外伤害
		if (attacker instanceof Hero){
			Hero hero = (Hero) attacker;
			float ratio = hero.HP / (float) hero.HT;
			if (ratio < 0.25f){
				damage *= 2;
			} else if (ratio < 0.5f){
				damage = Math.round(damage * 1.5f);
			}
		}

		return super.proc(attacker, defender, damage);
	}

	//猩红之怒：持有猩红创痕时按生命值持续产生向上漂浮的红色粒子（实时效果，站立不动时也显示）
	public static class CrimsonAura extends Buff {

		private Emitter emberEmitter;
		private float lastInterval = -1;

		{
			type = buffType.NEUTRAL;
		}

		@Override
		public boolean act() {
			if (target instanceof Hero){
				Hero hero = (Hero) target;
				//若武器已不在身上则自行移除（兜底处理）
				if (!(hero.belongings.weapon() instanceof CrimsonScar)
						&& !(hero.belongings.secondWep() instanceof CrimsonScar)){
					detach();
					return true;
				}

				float ratio = hero.HP / (float) hero.HT;
				//低于25%时粒子密度增加（间隔更短）
				float interval = ratio < 0.25f ? 0.05f : (ratio < 0.5f ? 0.12f : -1f);

				if (interval < 0){
					//血量不低于50%，关闭粒子
					if (emberEmitter != null){
						emberEmitter.on = false;
						emberEmitter.kill();
						emberEmitter = null;
						lastInterval = -1;
					}
				} else if (emberEmitter == null || interval != lastInterval){
					//开启或调整粒子密度
					if (emberEmitter == null && hero.sprite != null){
						emberEmitter = hero.sprite.emitter();
					}
					if (emberEmitter != null){
						emberEmitter.start( ScarletParticle.FACTORY, interval, 0 );
						lastInterval = interval;
					}
				}
			}
			spend( TICK );
			return true;
		}

		@Override
		public void detach() {
			if (emberEmitter != null){
				emberEmitter.on = false;
				emberEmitter.kill();
				emberEmitter = null;
			}
			super.detach();
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}

}
