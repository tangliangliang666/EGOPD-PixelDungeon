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

package com.shatteredpixel.shatteredpixeldungeon.items.wands;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.mage.WildMagic;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.MagicMissile;
import com.shatteredpixel.shatteredpixeldungeon.effects.SpellSprite;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.PinkParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.MagesStaff;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.ConeAOE;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.GameMath;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 旭日之心：新法杖。
 * <p>使用模式同烈焰法杖：锥形范围伤害，充能足够时消耗更多充能造成更大伤害
 * （1/2/3 档）；但不会产生火焰/起火残留，而是粉色特效、并直接摧毁可燃障碍物
 * （类似解离法杖）。战法师近战交互套用魔弹法杖。</p>
 *
 * <p><b>障碍破坏修复（2026-09-06）：</b>原实现仅在 {@link #cone} 的
 * {@code cone.cells} 内逐格检查 {@code flamable} 并 {@code destroy}，
 * 但 ConeAOE 每条 ray 受 {@code STOP_SOLID} 阻挡，且立体障碍物
 * （BARRICADE/BOOKSHELF 等 FLAMABLE|SOLID 物体）通常并不落在 cone.cells
 * 的路径上（尤其是近距离施法时书架在施法者侧面的情况），导致这些
 * 立体障碍物不会被破坏。参考 {@link WandOfFireblast#onZap} 的
 * 「adjacentCells + 邻居扩展」做法，把这些「过路」空地的邻居中
 * 更靠近 collisionPos 的可燃格子也一并 destroy。</p>
 */
public class WandOfBlazingHeart extends DamageWand {

	{
		image = ItemSpriteSheet.WAND_BLAZING_HEART;

		//only used for targeting, actual projectile logic is Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID
		collisionProperties = Ballistica.WONT_STOP;
	}

	//伤害随充能档位提升（已于 2026-09-06 整体上调 1 级：等价于把公式里的 lvl 替换为 lvl+1）：
	//1档 2+L ~ 4+2L；2档 4+2L ~ 15+5L；3档 6+3L ~ 33+9L
	public int min(int lvl){
		return (2+lvl) * chargesPerCast();
	}

	public int max(int lvl){
		switch (chargesPerCast()){
			case 1: default:
				return 4 + 2*lvl;
			case 2:
				return 15 + 5*lvl;
			case 3:
				return 33 + 9*lvl;
		}
	}

	ConeAOE cone;

	@Override
	public void onZap(Ballistica bolt) {

		boolean terrainAffected = false;

		ArrayList<Char> affectedChars = new ArrayList<>();
		// 收集「过路」空地（adjacent to sourcePos 且既不可燃也不固体），用于立体障碍物扩展破坏
		// （参考 WandOfFireblast：近距离施法时书架等立体障碍物可能不在 cone.cells 中）
		ArrayList<Integer> adjacentCells = new ArrayList<>();
		for( int cell : cone.cells ){

			//ignore caster cell
			if (cell == bolt.sourcePos){
				continue;
			}

			boolean flamable = Dungeon.level.flamable[cell];
			boolean solid = Dungeon.level.solid[cell];

			//直接摧毁可燃障碍物（同解离法杖，不残留火焰）
			if (flamable){
				Dungeon.level.destroy(cell);
				GameScene.updateMap(cell);
				terrainAffected = true;
			}

			//仅收集「过路」空地（既不可燃也不固体的 adjacent to sourcePos 格）作为扩展候选
			if (Dungeon.level.adjacent(bolt.sourcePos, cell) && !flamable && !solid){
				adjacentCells.add(cell);
			}

			//粉色特效
			CellEmitter.center(cell).burst(PinkParticle.BURST, Random.IntRange(1, 3));

			Char ch = Actor.findChar( cell );
			if (ch != null) {
				affectedChars.add(ch);
			}
		}

		if (terrainAffected) {
			Dungeon.observe();
		}

		// 若锥体没有任何可通行格（贴脸朝墙/障碍施放），则以施法者自身为扩展中心
		// （参考 WandOfFireblast：防止近距离施法漏掉 barricade/bookshelf）
		if (cone.cells.isEmpty()){
			adjacentCells.add(bolt.sourcePos);
		}

		// 扩展摧毁：过路空地的 8 邻居中，若更靠近 collisionPos 且可燃，则一并 destroy
		// —— 修复 ConeAOE 路径上不包含立体障碍物（BARRICADE/BOOKSHELF 等）导致的漏破坏
		for (int cell : adjacentCells){
			for (int i : PathFinder.NEIGHBOURS8){
				int n = cell + i;
				if (Dungeon.level.insideMap(n)
						&& Dungeon.level.trueDistance(n, bolt.collisionPos) < Dungeon.level.trueDistance(cell, bolt.collisionPos)
						&& Dungeon.level.flamable[n]){
					Dungeon.level.destroy(n);
					GameScene.updateMap(n);
					terrainAffected = true;
				}
			}
		}

		if (terrainAffected){
			Dungeon.observe();
		}

		for ( Char ch : affectedChars ){
			wandProc(ch, chargesPerCast());
			ch.damage(damageRoll(), this);
			ch.sprite.centerEmitter().burst(PinkParticle.BURST, Random.IntRange(1, 3));
			ch.sprite.flash();
		}
	}

	//战法师近战交互：套用魔弹法杖（命中时给其他法杖充能 + 充能法术特效）
	@Override
	public void onHit(MagesStaff staff, Char attacker, Char defender, int damage) {
		SpellSprite.show(attacker, SpellSprite.CHARGE);
		for (Wand.Charger c : attacker.buffs(Wand.Charger.class)){
			if (c.wand() != this){
				c.gainCharge(0.5f * procChanceMultiplier(attacker));
			}
		}
	}

	@Override
	public void fx(Ballistica bolt, Callback callback) {
		//need to perform cone logic here so we can determine what cells to hit.

		// 7/9/12 distance
		int maxDist;
		switch (chargesPerCast()){
			case 1: default:
				maxDist = 7;
				break;
			case 2:
				maxDist = 9;
				break;
			case 3:
				maxDist = 12;
				break;
		}

		// 30/50/70 度（散射更小）
		cone = new ConeAOE( bolt,
				maxDist,
				10 + 20*chargesPerCast(),
				Ballistica.STOP_TARGET | Ballistica.STOP_SOLID | Ballistica.IGNORE_SOFT_SOLID);

		//cast to cells at the tip, rather than all cells, better performance.
		Ballistica longestRay = null;
		for (Ballistica ray : cone.outerRays){
			if (longestRay == null || ray.dist > longestRay.dist){
				longestRay = ray;
			}
			((MagicMissile)curUser.sprite.parent.recycle( MagicMissile.class )).reset(
					MagicMissile.PINK_CONE,
					curUser.sprite,
					ray.path.get(ray.dist),
					null
			);
		}

		//final zap at half distance of the longest ray, for timing of the actual wand effect
		MagicMissile.boltFromChar( curUser.sprite.parent,
				MagicMissile.PINK_CONE,
				curUser.sprite,
				longestRay.path.get(longestRay.dist/2),
				callback );
		Sample.INSTANCE.play( Assets.Sounds.ZAP );
	}

	@Override
	protected int chargesPerCast() {
		if (cursed ||
				(charger != null && charger.target != null && charger.target.buff(WildMagic.WildMagicTracker.class) != null)){
			return 1;
		}
		//consumes 30% of current charges, rounded up, with a min of 1 and a max of 3.
		return (int) GameMath.gate(1, (int)Math.ceil(curCharges*0.3f), 3);
	}

	@Override
	public String statsDesc() {
		if (levelKnown)
			return Messages.get(this, "stats_desc", chargesPerCast(), min(), max());
		else
			return Messages.get(this, "stats_desc", chargesPerCast(), min(0), max(0));
	}

	@Override
	public String upgradeStat1(int level) {
		return (2+level) + "-" + (4+2*level);
	}

	@Override
	public String upgradeStat2(int level) {
		return (4+2*level) + "-" + (15+5*level);
	}

	@Override
	public String upgradeStat3(int level) {
		return (6+3*level) + "-" + (33+9*level);
	}

	@Override
	public void staffFx(MagesStaff.StaffParticle particle) {
		particle.color( 0xFF69B4 ); //粉色
		particle.am = 0.5f;
		particle.setLifespan(0.6f);
		particle.acc.set(0, -40);
		particle.setSize( 0f, 3f);
		particle.shuffleXY( 1.5f );
	}

}
