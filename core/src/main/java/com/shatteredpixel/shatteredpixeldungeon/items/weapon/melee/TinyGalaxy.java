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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.items.GlowingPebble;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

//小小银河：三阶近战武器，打击音效，武技配置套用硬头锤（Mace）。
//效果：装备该武器击杀敌人时，有 (10+武器等级×3)% 概率掉落"发光的鹅卵石"。
public class TinyGalaxy extends Mace {

	{
		image = ItemSpriteSheet.TINY_GALAXY;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 3;
		ACC = 1f; //无命中加成（取消继承硬头锤的1.28倍命中）
	}

	@Override
	public int min(int lvl) {
		return  3 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  20 +  //base
				4*lvl;   //level scaling
	}

	//击杀敌人时掉落发光的鹅卵石（由 Mob.die 调用）
	public static void onEnemyKilled(Hero hero, Mob mob){
		TinyGalaxy wep = null;
		if (hero.belongings.weapon() instanceof TinyGalaxy){
			wep = (TinyGalaxy) hero.belongings.weapon();
		} else if (hero.belongings.secondWep() instanceof TinyGalaxy){
			wep = (TinyGalaxy) hero.belongings.secondWep();
		}

		if (wep != null && Random.Int(100) < 10 + 3*wep.buffedLvl()){
			Dungeon.level.drop(new GlowingPebble(), mob.pos).sprite.drop();
		}
	}

}
