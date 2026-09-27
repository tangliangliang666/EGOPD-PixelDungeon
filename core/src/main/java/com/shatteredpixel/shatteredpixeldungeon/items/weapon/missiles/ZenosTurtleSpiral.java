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

package com.shatteredpixel.shatteredpixeldungeon.items.weapon.missiles;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Bleeding;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.PinCushion;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

import java.util.ArrayList;

/**
 * 芝诺的龟螺旋：5阶投掷武器。
 * <p>伤害公式 {@code 4+L ~ 10+2L}（等同捕鱼矛 4+L~10+2L 的成长结构），耐久为 4。
 * 刺入目标（中矢）后，目标每次移动都会累积 {@code 6 + 武器等级×2} 的流血伤害；
 * 原地不动则不会累积。</p>
 */
public class ZenosTurtleSpiral extends MissileWeapon {

	{
		image = ItemSpriteSheet.ZENOS_TURTLE_SPIRAL;
		hitSound = Assets.Sounds.HIT_STAB;
		hitSoundPitch = 1.1f;

		tier = 5;
		baseUses = 4;
		sticky = true;
	}

	@Override
	public int min(int lvl) {
		return 4 + lvl;   //4+L
	}

	@Override
	public int max(int lvl) {
		return 10 + 2*lvl; //10+2L
	}

	//每次移动累积的流血量：6 + 武器等级×2
	public int bleedPerMove(){
		return bleedPerMove(buffedLvl());
	}

	public int bleedPerMove(int lvl){
		return 6 + lvl*2;
	}

	@Override
	public String statsInfo(){
		if (isIdentified()){
			return Messages.get(this, "stats_desc", bleedPerMove());
		} else {
			return Messages.get(this, "typical_stats_desc", bleedPerMove(0));
		}
	}

	//目标移动时触发：若"中矢"（PinCushion）中含有本武器，则累积流血伤害
	public static void onMove( Char ch ) {
		if (ch == null || !ch.isAlive()) return;

		PinCushion pincushion = ch.buff(PinCushion.class);
		if (pincushion == null) return;

		ArrayList<MissileWeapon> stuck = pincushion.getStuckItems();
		int totalBleed = 0;
		for (MissileWeapon w : stuck){
			if (w instanceof ZenosTurtleSpiral){
				//6 + 武器等级×2（复用显示公式，保证一致）
				totalBleed += ((ZenosTurtleSpiral)w).bleedPerMove(w.buffedLvl());
			}
		}
		if (totalBleed > 0){
			//累积流血：原地不动不触发，移动一次累积一次（伤害由流血buff每回合结算）
			Bleeding b = Buff.affect(ch, Bleeding.class);
			b.extend(totalBleed);
		}
	}

}
