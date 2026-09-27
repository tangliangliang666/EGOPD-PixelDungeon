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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArtifactRecharge;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Brute;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.utils.Callback;

import java.util.ArrayList;

/**
 * 中指 长兄 盔甲技能「<b>永不遗忘</b>」（2026-09-17）。
 *
 * <p>消耗 70 点盔甲充能：冲向选中的地格（起跳、落地的过程照抄「英勇之跃」），
 * 落地后对落点周围的范围<b>各发动一次攻击</b>。</p>
 *
 * <h3>范围</h3>
 * <p>基础半径 1（正好 3×3）；「大闹一场吧」+1/+2/+3 ⇒ 半径 2/3/4，
 * 即 5×5/7×7/9×9 的<b>圆形</b>范围（判据是欧氏距离 ≤ 半径+0.5，所以半径 1 时恰好铺满 3×3）。
 * 范围<b>不判墙体遮挡</b>——设计要求是「范围内的所有目标」。</p>
 *
 * <h3>伤害倍率</h3>
 * <ul>
 *   <li>范围内<b>没有</b>友方单位：所有目标都吃 <b>100%</b>（一次普通攻击）。</li>
 *   <li>范围内<b>有</b>友方单位：友方单位吃 <b>100%</b>、敌方单位吃 <b>200%</b>。</li>
 *   <li>「家人的心意」（只在范围内真有友方单位时生效）把上面两档抬到
 *       +1 友 200%/敌 300%、+2 友 300%/敌 400%、+3 友<b>必定击杀</b>/敌 600%。</li>
 * </ul>
 *
 * <p>倍率直接交给 {@code Char.attack(enemy, dmgMulti, dmgBonus, accMulti)}——所以命不命中、
 * 护甲怎么减、武器特效怎么触发全部走原版那一套，倍率只乘在 {@code damageRoll()} 的结果上。
 * <b>副作用</b>：每次攻击都会各自触发一遍「攻击开始/命中/落空」钩子（例如「背叛家人者」的
 * 账簿预支、T1「仔细看好！」的夸耀），这与「对每个目标各发动一次攻击」的语义一致。</p>
 */
public class NeverForget extends ArmorAbility {

	/** 基础范围半径（1 ⇒ 3×3）。 */
	public static final int BASE_RADIUS = 1;

	/** 「我会牢记在心」每点给予的神器充能回合数。 */
	private static final float ARTIFACT_RECHARGE_PER_POINT = 5f;

	{
		baseChargeUse = 70f;
	}

	@Override
	public String targetingPrompt(){
		return Messages.get( this, "prompt" );
	}

	@Override
	public int targetedPos( Char user, int dst ){
		return new Ballistica( user.pos, dst, Ballistica.STOP_SOLID | Ballistica.STOP_TARGET ).collisionPos;
	}

	@Override
	protected void activate( ClassArmor armor, Hero hero, Integer target ){

		if (target == null) return;

		if (hero.rooted){
			PixelScene.shake( 1, 1f );
			return;
		}

		//落点：与「英勇之跃」同款——沿弹道走到碰撞点，再往回退到第一个没被别人占着的格子
		Ballistica route = new Ballistica( hero.pos, target, Ballistica.STOP_TARGET | Ballistica.STOP_SOLID );
		int cell = route.collisionPos;
		int backTrace = route.dist - 1;
		while (Actor.findChar( cell ) != null && cell != hero.pos ){
			cell = route.path.get( backTrace );
			backTrace--;
		}

		armor.charge -= chargeUse( hero );
		armor.updateQuickslot();

		final int dest = cell;
		hero.busy();
		hero.sprite.jump( hero.pos, cell, new Callback(){
			@Override
			public void call(){
				hero.move( dest );
				Dungeon.level.occupyCell( hero );
				Dungeon.observe();
				GameScene.updateFog();

				strike( hero );
				WandOfBlastWave.BlastWave.blast( dest );
				PixelScene.shake( 2, 0.5f );

				Invisibility.dispel();
				hero.spendAndNext( Actor.TICK );
			}
		} );
	}

	//==========================================================================
	// 落地后的范围一击
	//==========================================================================

	/** 落地后，对落点周围范围内的所有目标各发动一次攻击。 */
	public static void strike( Hero hero ){

		int radius = BASE_RADIUS + hero.pointsInTalent( Talent.MAKE_A_SCENE );
		float circle = radius + 0.5f;

		ArrayList<Char> victims = new ArrayList<>();
		int cx = hero.pos % Dungeon.level.width();
		int cy = hero.pos / Dungeon.level.width();
		for (int y = cy - radius; y <= cy + radius; y++){
			for (int x = cx - radius; x <= cx + radius; x++){
				if (x < 0 || y < 0 || x >= Dungeon.level.width() || y >= Dungeon.level.height()) continue;
				int dx = x - cx;
				int dy = y - cy;
				if (Math.sqrt( dx*dx + dy*dy ) > circle) continue;   //圆形范围

				Char ch = Actor.findChar( y * Dungeon.level.width() + x );
				if (ch != null && ch != hero && ch.isAlive()){
					victims.add( ch );
				}
			}
		}

		if (victims.isEmpty()) return;

		//「家人的心意」的生效前提：范围里真的有友方单位（没有就整条天赋作废）
		boolean allyPresent = false;
		for (Char ch : victims){
			if (ch.alignment == hero.alignment){
				allyPresent = true;
				break;
			}
		}
		int heartPoints = allyPresent ? hero.pointsInTalent( Talent.FAMILY_HEART ) : 0;

		//先把友方单位的引用留一份，攻击全部结算完再看谁死了（「我会牢记在心」）
		ArrayList<Char> allies = new ArrayList<>();
		for (Char ch : victims){
			if (ch.alignment == hero.alignment) allies.add( ch );
		}

		for (Char ch : victims){
			boolean isAlly = ch.alignment == hero.alignment;

			if (isAlly && heartPoints >= 3){
				//+3 的友方档位：改为必定击杀（照抄原版「必杀」的写法，顺带清掉蛮兵的狂暴）
				ch.HP = 0;
				if (ch.buff( Brute.BruteRage.class ) != null){
					ch.buff( Brute.BruteRage.class ).detach();
				}
				if (!ch.isAlive()){
					ch.die( hero );
				}
				continue;
			}

			float multi;
			if (isAlly){
				//+1/+2 把友方档位抬到 200%/300%
				multi = heartPoints == 0 ? 1f : (heartPoints == 1 ? 2f : 3f);
			} else if (allyPresent){
				//范围内有友方单位：敌方 200% 起步，家人的心意再往上抬
				multi = heartPoints == 0 ? 2f : (heartPoints == 1 ? 3f : (heartPoints == 2 ? 4f : 6f));
			} else {
				multi = 1f;
			}

			hero.attack( ch, multi, 0f, 1f );
		}

		//「我会牢记在心」：这一击真的收掉了友方单位 ⇒ 给复仇账簿之外的其它神器也补一口充能
		int mindPoints = hero.pointsInTalent( Talent.KEEP_IN_MIND );
		if (mindPoints > 0 && !allies.isEmpty()){
			boolean killedAlly = false;
			for (Char ch : allies){
				if (!ch.isAlive()){
					killedAlly = true;
					break;
				}
			}
			if (killedAlly){
				Buff.affect( hero, ArtifactRecharge.class ).set( ARTIFACT_RECHARGE_PER_POINT * mindPoints );
			}
		}
	}

	@Override
	public int icon(){
		//专属槽位：hero_icons.png 第 17 行第 3 列 = 帧 130
		return HeroIcon.MIDDLE_FINGER_ABILITY_2;
	}

	@Override
	public Talent[] talents(){
		return new Talent[]{
				Talent.FAMILY_HEART, Talent.MAKE_A_SCENE, Talent.KEEP_IN_MIND,
				Talent.HEROIC_ENERGY };
	}
}
