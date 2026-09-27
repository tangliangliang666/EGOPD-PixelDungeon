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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

//黑天鹅：四阶近战武器，格挡5+L，打击音效，武技配置套用圆盾（RoundShield）。
//效果：持有这把武器时，受到近战攻击有20%的概率不受到伤害，并反过来对攻击者造成等量伤害
//（见 Hero.defenseProc 的黑天鹅反射处理）。
public class BlackSwan extends RoundShield {

	{
		image = ItemSpriteSheet.BLACK_SWAN;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 4;
	}

	@Override
	public int min(int lvl) {
		return  4 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  15 +  //base
				3*lvl;   //level scaling
	}

	@Override
	public int defenseFactor( Char owner ) {
		return DRMax();
	}

	public int DRMax(){
		return DRMax(buffedLvl());
	}

	//格挡5点伤害，每级+1
	public int DRMax(int lvl){
		return 5 + lvl;
	}

	@Override
	public String statsInfo(){
		//不区分鉴定状态，始终显示完整格挡/反弹文本
		return Messages.get(this, "stats_desc", 5+buffedLvl());
	}

}
