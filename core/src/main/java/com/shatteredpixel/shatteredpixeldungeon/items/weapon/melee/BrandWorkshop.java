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
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.FlameParticle;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 烙印工坊 —— 三阶近战武器（非E.G.O，随机生成池产出）。
 *
 * <p>面板：3+L ~ 16+4L。
 * 效果：每次攻击附加本次伤害量 20% 的火焰伤害（该附加火焰伤害<em>不会</em>点燃目标）；
 * 若武器带烈焰附魔（Blazing），则额外获得 60% 附魔强度
 * （附魔强度加成的结算见 {@link com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon.Enchantment#genericProcChanceMultiplier(Char)}）。</p>
 */
public class BrandWorkshop extends MeleeWeapon {

	{
		image = ItemSpriteSheet.BRAND_WORKSHOP;
		hitSound = Assets.Sounds.HIT_SLASH;
		hitSoundPitch = 1f;

		tier = 3;
	}

	@Override
	public int min(int lvl) {
		return 3 + lvl;
	}

	@Override
	public int max(int lvl) {
		return 16 + 4*lvl;
	}

	//内置炽热：命中附加本次伤害 20% 的火焰伤害（不附加燃烧状态——刻意不做 ignite）
	@Override
	public int proc(Char attacker, Char defender, int damage) {
		damage = super.proc(attacker, defender, damage);

		if (defender.isAlive()){
			int fireDmg = Math.round(damage * 0.20f);
			if (fireDmg > 0){
				defender.damage(fireDmg, this);
				defender.sprite.emitter().burst( FlameParticle.FACTORY, 4 );
			}
		}

		return damage;
	}

}
