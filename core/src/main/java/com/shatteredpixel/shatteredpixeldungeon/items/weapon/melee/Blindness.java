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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ExtraVision;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 盲目：四阶E.G.O武器。
 * <p>面板 4+L ~ 55+12L。攻击延迟 1.5（极其缓慢）、精准修正 0.8（不太精准）、无法偷袭
 * （见 {@link Hero#canSurpriseAttack()}）。装备（主手或副手）时英雄挂载 {@link ExtraVision}
 * 视野加成 buff，视野扩大 1 格，随每次视野重算（Level.updateFieldOfView）生效。</p>
 */
public class Blindness extends MeleeWeapon {

	{
		image = ItemSpriteSheet.BLINDNESS;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 4;
		DLY = 1.5f; //0.67x speed（极其缓慢）
		ACC = 0.8f; //0.8x accuracy（不太精准）
		//无法偷袭：见 Hero.canSurpriseAttack
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  55 +  //base
				12*lvl;   //level scaling
	}

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//装备时挂载视野加成 buff（主手/副手 activate 都会调用；Buff.affect 防重复）
		if (ch instanceof Hero){
			Buff.affect(ch, ExtraVision.class);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)){
			Buff.detach(hero, ExtraVision.class);
			return true;
		} else {
			return false;
		}
	}

}
