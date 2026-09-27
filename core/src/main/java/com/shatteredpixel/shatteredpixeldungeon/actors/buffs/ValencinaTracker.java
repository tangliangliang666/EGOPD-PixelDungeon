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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.OdinsEye;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfMagicMapping;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.PathFinder;

/**
 * 拇指 前二老板的每回合机制维护器（挂在英雄身上，无图标）。
 *
 * <p>T2「未来视角」：预知眼生效期间立刻察觉周围（相邻 8 格）的隐藏门与陷阱
 * （就地揭示，同先见护符的感知）；+2 时即使预知眼未生效，只要周围存在隐藏
 * 门/陷阱也会获得「预感」buff。</p>
 *
 * <p>参考神谕代行者的 {@link OracleMarkTracker}：开局由 {@code initValencina}
 * 挂载、十字架复活后保留。</p>
 */
public class ValencinaTracker extends Buff {

	{
		//核心常驻机制：十字架复活后保留
		revivePersists = true;
	}

	@Override
	public boolean act() {

		if (target instanceof Hero){
			Hero hero = (Hero) target;
			if (hero.heroClass == HeroClass.VALENCINA && hero.hasTalent(Talent.FUTURE_VISION)){
				scan(hero);
			}
		}

		spend( TICK );
		return true;
	}

	/**
	 * 未来视角：预知眼期间揭示相邻 8 格隐藏门/陷阱；
	 * 无预知眼但 +2 时，若相邻存在隐藏物则获得「预感」。
	 */
	private void scan( Hero hero ){

		Level level = Dungeon.level;
		if (level == null) return;

		boolean precog = hero.buff(OdinsEye.precognition.class) != null;
		int points = hero.pointsInTalent(Talent.FUTURE_VISION);

		boolean foundHidden = false;

		for (int offset : PathFinder.NEIGHBOURS8){
			int cell = hero.pos + offset;
			if (!level.insideMap(cell)) continue;

			//只关心尚未被发现的隐藏门 / 隐藏陷阱
			if (!level.secret[cell]) continue;
			int old = level.map[cell];
			if (old != Terrain.SECRET_DOOR && old != Terrain.SECRET_TRAP) continue;

			foundHidden = true;

			if (precog){
				//预知眼生效：立刻察觉（就地揭示，同先见护符 scry 的做法）
				GameScene.discoverTile(cell, old);
				level.discover(cell);
				ScrollOfMagicMapping.discover(cell);
			}
		}

		//+2：即使预知眼没生效，周围有隐藏门/陷阱时也获得「预感」
		if (!precog && points >= 2){
			if (foundHidden){
				boolean fresh = hero.buff(Premonition.class) == null;
				//prolong = 刷新制：重复授予只把剩余回合重置为 3，不叠加（affect 的 spend 会累加）
				Buff.prolong( hero, Premonition.class, 3f );
				if (fresh) BuffIndicator.refreshHero();
			}
			//离开危险区域后让 buff 自然流逝即可，不必主动 detach（避免闪烁）
		}
	}
}
