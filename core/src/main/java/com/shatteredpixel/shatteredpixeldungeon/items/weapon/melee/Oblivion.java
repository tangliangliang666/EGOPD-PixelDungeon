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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.FlavourBuff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

//忘却：三阶近战武器，0.5攻击延迟（相当快速），打击音效，武技配置套用弯刀（Scimitar）。
//效果：命中时为目标施加3回合的"熊熊抱"buff（重复施加刷新回合数为3、层数+1，回合数不叠加）；
//装备时每次攻击根据目标熊熊抱的层数改变面板伤害——每有1层就增加一倍（3层时为4倍面板伤害）。
public class Oblivion extends Scimitar {

	{
		image = ItemSpriteSheet.OBLIVION;
		hitSound = Assets.Sounds.HIT_CRUSH;
		hitSoundPitch = 1f;

		tier = 3;
		DLY = 0.5f; //2x speed
	}

	@Override
	public int min(int lvl) {
		return  3 +  //base
				lvl;   //level scaling
	}

	@Override
	public int max(int lvl) {
		return  5 +  //base
				lvl;   //level scaling
	}

	@Override
	public int proc(Char attacker, Char defender, int damage) {
		boolean magicImmune = attacker.buff(MagicImmune.class) != null;

		//先按目标当前熊熊抱的层数改变面板伤害（每有1层就增加一倍）
		if (!magicImmune){
			BearHug bearHug = defender.buff(BearHug.class);
			if (bearHug != null && bearHug.layers > 0){
				damage *= (1 + bearHug.layers);
			}
		}

		damage = super.proc(attacker, defender, damage);

		//命中后为目标施加/叠层3回合的熊熊抱（刷新回合数为3，不叠加回合数）
		if (!magicImmune && defender.isAlive()){
			Buff.affect(defender, BearHug.class).stack();
		}

		return damage;
	}

	//熊熊抱：套用连击（Combo）的buff图标
	public static class BearHug extends FlavourBuff {

		public int layers = 0;

		{
			announced = true;
			type = buffType.NEGATIVE;
		}

		//层数+1，并把持续回合刷新为3（不叠加）
		public void stack(){
			layers++;
			timeToNow();
			spend(3f * target.resist(getClass()));
		}

		@Override
		public int icon() {
			return BuffIndicator.COMBO;
		}

		@Override
		public String name() {
			return "熊熊抱";
		}

		@Override
		public String desc() {
			return "这个单位正受到忘却的攻击，受到来自忘却的" + (layers+1) + "倍伤害";
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(layers+1);
		}

		private static final String LAYERS = "layers";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(LAYERS, layers);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			layers = bundle.getInt(LAYERS);
		}

	}

}
