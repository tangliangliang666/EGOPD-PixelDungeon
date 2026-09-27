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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Random;

/**
 * し协会的暗杀刃 —— 四阶近战武器。
 *
 * <p>面板：4+4L ~ 24+2L（最小伤害成长高于最大伤害成长，随等级升高掷骰区间不断收窄，
 * +10 时收为固定 44，之后每击都视为"掷出最大伤害"）。</p>
 *
 * <p>效果①（匕首类偷袭加成，参考暗杀之刃/长匕首）：目标未察觉你时，
 * 掷骰区间从「最低+50%区间 ~ 最大值」取整段，伤害向最大值倾斜。</p>
 *
 * <p>效果②：每次攻击掷出最大伤害时，造成的伤害额外增加 44 点。
 * （与偷袭加成联动：偷袭把掷骰区间抬高到最高值附近，触发爆发的概率显著提升。）</p>
 */
public class ShiAssassinsBlade extends MeleeWeapon {

	//随机到最大伤害时的额外伤害
	public static final int MAX_DMG_BONUS = 44;

	{
		image = ItemSpriteSheet.SHI_ASSASSINS_BLADE;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 0.9f;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				4*lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  24 +  //base
				2*lvl;   //level scaling
	}

	@Override
	public int damageRoll(Char owner) {
		int rollMin = min();
		int rollMax = max();

		//匕首类偷袭加成：目标未察觉你时，掷骰区间向最大值方向倾斜
		//（取「最低+50%区间 ~ 最大」，参考暗杀之刃的偷袭结算）
		if (owner instanceof Hero) {
			Hero hero = (Hero)owner;
			Char enemy = hero.attackTarget();
			if (enemy instanceof Mob && ((Mob) enemy).surprisedBy(hero)) {
				int diff = rollMax - rollMin;
				rollMin = rollMin + Math.round(diff*0.50f);
			}
		}

		int roll = (owner instanceof Hero)
				? Hero.heroDamageIntRange(rollMin, rollMax)
				: Random.NormalIntRange(rollMin, rollMax);

		//随机到最大伤害时，造成的伤害额外增加44点
		boolean rolledMax = (roll == max());

		int damage = augment.damageFactor(roll);

		if (owner instanceof Hero) {
			int exStr = ((Hero)owner).STR() - STRReq();
			if (exStr > 0) {
				damage += Hero.heroDamageIntRange( 0, exStr );
			}
		}

		if (rolledMax){
			damage += MAX_DMG_BONUS;
		}

		return damage;
	}

}
