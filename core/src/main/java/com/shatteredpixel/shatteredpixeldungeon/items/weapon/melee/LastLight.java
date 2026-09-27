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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Burning;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.curses.Explosive;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//终末之光：二阶近战武器，武技配置套用短剑（Shortsword），攻击音效与火焰有关。
//效果：拥有无法祛除和覆盖的烈焰附魔与易爆诅咒。
//（附魔与诅咒无法共存于同一槽位，故仅显示为易爆诅咒，烈焰附魔的能力内建到武器代码中。）
public class LastLight extends Shortsword {

	{
		image = ItemSpriteSheet.LAST_LIGHT;
		hitSound = Assets.Sounds.BURNING; //火焰燃烧音效
		hitSoundPitch = 1f;

		tier = 2;

		//易爆诅咒：无法祛除、无法覆盖（由下方 enchant() 覆写保证，不采用硬化标记）
		enchantment = new Explosive();
	}

	@Override
	public int min(int lvl) {
		return  2 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  18 +  //base
				3*lvl;   //level scaling
	}

	//终末之光：附魔无法被祛除或覆盖，永远保持易爆诅咒
	@Override
	public Weapon enchant(Enchantment ench) {
		return super.enchant(new Explosive());
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		//内建的烈焰附魔能力（参考 Blazing 附魔）
		if (attacker.buff(MagicImmune.class) == null && defender.isAlive()){
			blazeProc(attacker, defender);
		}

		return damage;
	}

	private void blazeProc(Char attacker, Char defender){
		int level = Math.max( 0, buffedLvl() );

		// lvl 0 - 33%
		// lvl 1 - 50%
		// lvl 2 - 60%
		float procChance = (level+1f)/(level+3f) * Enchantment.genericProcChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			if (defender.buff(Burning.class) == null){
				Buff.affect(defender, Burning.class).reignite(defender, 8f);
				powerMulti -= 1;
			}

			if (powerMulti > 0){
				int burnDamage = Random.NormalIntRange( 1, 3 + Dungeon.scalingDepth()/4 );
				burnDamage = Math.round(burnDamage * 0.67f * powerMulti);
				if (burnDamage > 0) {
					defender.damage(burnDamage, this);
				}
			}

			defender.sprite.emitter().burst( FlameParticle.FACTORY, level + 1 );

		}
	}

}
