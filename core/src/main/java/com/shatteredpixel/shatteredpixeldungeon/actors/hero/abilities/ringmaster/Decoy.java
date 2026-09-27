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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.DecoyDoll;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * 环指大师的盔甲技能「诱饵」（2026-09-04）。
 * <p>消耗 {@code baseChargeUse=40}% 盔甲充能。在身旁召唤一件「环指诱饵人偶」
 * {@link DecoyDoll}：不会移动、血量为人偶 1.5 倍，每隔 50 回合发出盛怒嚎叫
 * 吸引本层的敌人；持续到被击杀为止。</p>
 * <p>对应三个四阶分支天赋：诱惑的青光（召唤时获得全层嘲讽）、
 * 爆裂之美（死亡时炸弹爆炸）、痴迷欣赏（攻击诱饵的敌人获得 debuff）。</p>
 */
public class Decoy extends ArmorAbility {

	{
		baseChargeUse = 40f; //消耗40%盔甲充能
	}

	@Override
	protected void activate(final ClassArmor armor, final Hero hero, Integer target) {

		//先找英雄身旁一个可站立的空格，找不到不消耗充能直接返回
		int pos = -1;
		if (Actor.findChar(hero.pos) == null && !Dungeon.level.solid[hero.pos]){
			pos = hero.pos;
		} else {
			pos = emptyCellAround(hero.pos);
		}
		if (pos == -1){
			GLog.w( "周围没有可以放置诱饵人偶的空地。" );
			return;
		}

		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();

		DecoyDoll doll = DecoyDoll.spawn( pos );

		//诱惑的青光：召唤时若有点数，直接给对应回合数的众矢之的
		if (hero.pointsInTalent(Talent.SEDUCTIVE_GLOW) > 0){
			DecoyDoll.SeductiveGlow.apply( doll );
		}

		CellEmitter.get( pos ).burst( Speck.factory( Speck.WOOL ), 8 );
		Sample.INSTANCE.play( com.shatteredpixel.shatteredpixeldungeon.Assets.Sounds.PUFF );

		GameScene.updateMap( pos );
		GameScene.updateFog();

		hero.spendAndNext( Actor.TICK );
	}

	//英雄 8 邻域中找一个可站立空格；找不到返回 -1
	private static int emptyCellAround( int center ){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = center + i;
			if (Dungeon.level.insideMap( c )
					&& !Dungeon.level.solid[c]
					&& !Dungeon.level.pit[c]
					&& Actor.findChar( c ) == null){
				candidates.add( c );
			}
		}
		if (candidates.isEmpty()) return -1;
		return com.watabou.utils.Random.element( candidates );
	}

	@Override
	public int icon() {
		return HeroIcon.DECOY; //hero_icons.png 第9行第4列（帧 67）
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.SEDUCTIVE_GLOW, Talent.EXPLOSIVE_BEAUTY, Talent.OBSESSIVE_ADMIRATION, Talent.HEROIC_ENERGY};
	}
}
