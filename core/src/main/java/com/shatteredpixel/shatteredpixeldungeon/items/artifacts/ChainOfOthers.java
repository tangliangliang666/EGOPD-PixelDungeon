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

package com.shatteredpixel.shatteredpixeldungeon.items.artifacts;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Cripple;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Weakness;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;

/**
 * 他人之锁：原版「灵魂锁链」的强化形态（2026-09-20）。
 *
 * <h3>新增效果</h3>
 * <p>用锁链<b>牵引怪物</b>时，对英雄自己施加 {@value #SELF_DURATION} 回合的残废与虚弱，
 * 对被牵引的怪物施加 {@value #ENEMY_DURATION} 回合的残废与虚弱 —— 代价在自身、收益在敌人身上，
 * 是一条「先付账再好用」的锁链。</p>
 *
 * <h3>为什么钩子是 onEnemyPulled 而不是整个 chainEnemy</h3>
 * <p>{@code chainEnemy} 里「不能移动 / 找不到落脚点 / 充能不足」有多个提前 return，
 * 而且真正的位移是**异步**完成的（{@code Pushing} 的回调里才落位）。覆写整个方法既容易漏判
 * 「到底拉没拉动」，又要自己再写一遍动画与回调。父类本次在回调末尾加了一个默认空实现的
 * {@link EtherealChains#onEnemyPulled}，正好只在「真的拉动了怪物」之后触发
 * （把自己拉过去的 {@code chainLocation} 不会走到这里）。</p>
 *
 * <p>文本键 {@code items.artifacts.chainofothers.*}；锁链原有的
 * {@code ac_cast/prompt/cant_pull/does_nothing/no_charge/rooted/inside_wall/
 * nothing_to_grab/cant_reach/desc_equipped/desc_cursed} 等键沿父类链自动继承。</p>
 */
public class ChainOfOthers extends EtherealChains {

	/** 施加给英雄自己的残废 / 虚弱回合数。 */
	public static final float SELF_DURATION = 3f;

	/** 施加给被牵引怪物的残废 / 虚弱回合数。 */
	public static final float ENEMY_DURATION = 20f;

	{
		image = ItemSpriteSheet.ARTIFACT_CHAIN_OF_OTHERS;
	}

	@Override
	protected void onEnemyPulled( Hero hero, Char enemy ){
		//先算自己这一份（需求：敌我双方「同时」被施加）
		if (!hero.isImmune(Cripple.class)){
			Buff.affect(hero, Cripple.class, SELF_DURATION);
		}
		if (!hero.isImmune(Weakness.class)){
			Buff.affect(hero, Weakness.class, SELF_DURATION);
		}

		//敌人可能已经被 artifactProc 的附加伤害打死了（神谕「灼光」等），死人不吃 debuff
		if (enemy != null && enemy.isAlive()){
			if (!enemy.isImmune(Cripple.class)){
				Buff.affect(enemy, Cripple.class, ENEMY_DURATION);
			}
			if (!enemy.isImmune(Weakness.class)){
				Buff.affect(enemy, Weakness.class, ENEMY_DURATION);
			}
		}
	}

	//==========================================================================
	// 炼金合成：灵魂锁链 + 30 脑啡肽，12 能量
	//==========================================================================

	public static class CraftRecipe extends ArtifactEnhanceRecipe<EtherealChains, ChainOfOthers> {

		@Override
		protected Class<EtherealChains> acceptedArtifact(){
			return EtherealChains.class;
		}

		@Override
		protected ChainOfOthers createEnhanced(){
			return new ChainOfOthers();
		}

		@Override
		protected void transferState(EtherealChains source, ChainOfOthers enhanced){
			enhanced.level( source.level() );
			enhanced.exp = source.exp;
			enhanced.chargeCap = source.chargeCap;
			enhanced.charge = source.charge;
			enhanced.partialCharge = source.partialCharge;
			enhanced.cooldown = source.cooldown;

			enhanced.cursed = source.cursed;
			enhanced.cursedKnown = source.cursedKnown;
			enhanced.levelKnown = source.levelKnown;
		}
	}
}
