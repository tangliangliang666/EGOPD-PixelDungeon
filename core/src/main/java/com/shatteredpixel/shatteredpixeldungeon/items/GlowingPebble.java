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

package com.shatteredpixel.shatteredpixeldungeon.items;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.Statistics;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

//发光的鹅卵石：小小银河击杀敌人时掉落的特殊露珠，
//采用和露珠完全相同的拾取逻辑（水袋+1，水袋满则治疗），仅替换贴图与文本。
public class GlowingPebble extends Dewdrop {

	{
		image = ItemSpriteSheet.GLOWING_PEBBLE;
	}

	@Override
	public boolean doPickUp(Hero hero, int pos) {

		Waterskin flask = hero.belongings.getItem( Waterskin.class );
		Catalog.setSeen(getClass());
		Statistics.itemTypesDiscovered.add(getClass());

		if (flask != null && !flask.isFull()){

			GLog.i( Messages.get(this, "pickup") );
			flask.addVolume( quantity );
			if (flask.isFull()){
				GLog.p( Messages.get(Waterskin.class, "full") );
			}
			GameScene.pickUp( this, pos );

		} else {

			int terr = Dungeon.level.map[pos];
			if (!Dewdrop.consumeDew(1, hero, terr == Terrain.ENTRANCE || terr == Terrain.ENTRANCE_SP
					|| terr == Terrain.EXIT || terr == Terrain.UNLOCKED_EXIT)){
				return false;
			} else {
				Catalog.countUse(getClass());
			}

		}

		Sample.INSTANCE.play( Assets.Sounds.DEWDROP );
		hero.spendAndNext( pickupDelay() );

		return true;
	}

}
