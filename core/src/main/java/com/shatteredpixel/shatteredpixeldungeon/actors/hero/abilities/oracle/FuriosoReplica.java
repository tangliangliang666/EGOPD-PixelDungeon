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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.oracle;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ArmorAbility;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.KindOfWeapon;
import com.shatteredpixel.shatteredpixeldungeon.items.armor.ClassArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.Weapon;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Unstable;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.HermesCaduceus;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.tweeners.Delayer;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 神谕代行者的第二个盔甲技能「Furioso-Replica」。
 * <p>消耗 90% 盔甲充能。使用后对视野内的随机目标进行最多九次必中攻击：
 * 每次攻击瞬间移动（无位移动画）到目标周围、攻击并将目标击退一格，然后重新在视野内随机选择目标。
 * 装备赫尔墨斯的双蛇杖时，第九次攻击必定以镰刀形态进行。九次攻击均不消耗回合，
 * 全部结束（或视野内无可选目标）后整体只消耗一回合。</p>
 * <p>对应三个四阶分支天赋：完美重现（连击伤害递增）、神的技艺（附魔触发）、与你再会（击杀/未用攻击返还充能）。</p>
 */
public class FuriosoReplica extends ArmorAbility {

	{
		baseChargeUse = 90f; //消耗90%盔甲充能
	}

	private static final int MAX_ATTACKS = 9;

	//开场信息显示后的短暂停滞（秒）
	private static final float START_DELAY = 0.8f;

	/**
	 * 结束语（三种之一，随机）：**文本与语音必须成对取用**。
	 * <p>若把文本和语音各自独立随机，会出现「显示阿鼻叫唤、念的却是无我梦中」的错配，
	 * 所以这里用二维数组把两者绑在一起，一对一对地选。</p>
	 * <p>对应关系取自素材来源（里恩的解放层数台词）：
	 * 解放-I→无我梦中 / 解放-II→阿鼻叫唤 / 解放-III→支离灭裂。</p>
	 */
	private static final String[][] ENDING_LINES = {
			{"无我梦中", Assets.Sounds.INDEXFATHER_LIBERATION_1},
			{"阿鼻叫唤", Assets.Sounds.INDEXFATHER_LIBERATION_2},
			{"支离灭裂", Assets.Sounds.INDEXFATHER_LIBERATION_3},
	};

	@Override
	protected void activate(ClassArmor armor, Hero hero, Integer target) {
		armor.charge -= chargeUse(hero);
		armor.updateQuickslot();
		Invisibility.dispel();

		hero.busy();

		//开场的白色信息
		hero.sprite.showStatus(CharSprite.DEFAULT, "我将再现那颗激愤之心");

		//神的技艺 +3/+4：整个技能期间获得附魔强度加成（技能结束时移除）
		if (hero.pointsInTalent(Talent.DIVINE_ARTISTRY) >= 3){
			Buff.affect(hero, EnchantBoostTracker.class).boost =
					hero.pointsInTalent(Talent.DIVINE_ARTISTRY) == 3 ? 0.2f : 0.6f;
		}

		//短暂停滞后开始连击
		Delayer delayer = new Delayer(START_DELAY);
		delayer.listener = new Tweener.Listener() {
			@Override
			public void onComplete(Tweener tweener) {
				doAttack(armor, hero, 0, 0, 0);
			}
		};
		hero.sprite.parent.add(delayer);
	}

	//一次攻击判定；attackIndex 0~8，hitsDone 为已完成攻击数，kills 为本次技能击杀数
	private void doAttack(final ClassArmor armor, final Hero hero, final int attackIndex, final int hitsDone, final int kills){
		if (attackIndex >= MAX_ATTACKS){
			finish(armor, hero, hitsDone, kills);
			return;
		}

		//视野内的随机可选目标（存活、敌对、未被魅惑、周围有可站立的空地）
		final Char enemy = selectTarget(hero);
		if (enemy == null){
			finish(armor, hero, hitsDone, kills);
			return;
		}

		//====== 瞬间移动：直接消失并出现在目标周围（无位移动画） ======
		final int dest = findAdjacentCell(enemy);
		if (dest == -1){
			//目标周围没有可站立空地：视作不可达，直接结束
			finish(armor, hero, hitsDone, kills);
			return;
		}
		if (hero.sprite.visible){
			CellEmitter.get(hero.pos).burst(Speck.factory(Speck.WOOL), 4);
		}
		hero.sprite.interruptMotion();
		hero.pos = dest;
		Dungeon.level.occupyCell(hero);
		hero.sprite.place(dest);
		if (hero.sprite.visible){
			CellEmitter.get(dest).burst(Speck.factory(Speck.WOOL), 4);
			Sample.INSTANCE.play(Assets.Sounds.PUFF);
		}
		Dungeon.observe();
		GameScene.updateFog();
		hero.sprite.turnTo(hero.pos, enemy.pos);

		//第九次攻击（收尾一击）的特殊攻击音。
		//注意：本技能的九次攻击都走 enemy.damage(...)，**不经过 Char.attack**，
		//所以不会像普通攻击那样经由 hitSound 自动出声，必须在这里显式播放。
		if (attackIndex == MAX_ATTACKS - 1){
			Sample.INSTANCE.play(Assets.Sounds.INDEXFATHER_SKILL8_3);
		}

		//====== 攻击判定（必中） ======
		//完美重现：每次攻击使下一次攻击伤害增加 3%×点数（本攻击 = 1 + 3%×点数×已完成攻击数）
		float dmgMulti = 1f + 0.03f * hero.pointsInTalent(Talent.PERFECT_REPLICATION) * hitsDone;

		//双蛇杖：每次攻击重新随机形态；第九次攻击强制镰刀形态
		KindOfWeapon wep = hero.belongings.attackingWeapon();
		if (wep instanceof HermesCaduceus){
			if (attackIndex == MAX_ATTACKS - 1){
				((HermesCaduceus)wep).forceScytheForm();
			} else {
				((HermesCaduceus)wep).rollFormForAttack();
			}
		}

		int dmg = Math.round(hero.damageRoll() * dmgMulti);
		dmg = Math.max(dmg - enemy.drRoll(), 0);

		//神的技艺：附魔触发（+1 40% / +2 80% / +3 与 +4 100%，并附带附魔强度加成）
		//若当前武器没有附魔，则视为拥有紊乱附魔（仅对本次 Furioso-Replica 生效）
		int divinePoints = hero.pointsInTalent(Talent.DIVINE_ARTISTRY);
		if (divinePoints > 0){
			boolean procEnchant = divinePoints >= 3 || Random.Float() < 0.4f * divinePoints;
			if (procEnchant){
				if (wep instanceof Weapon && ((Weapon) wep).enchantment == null){
					dmg = new Unstable().proc((Weapon) wep, hero, enemy, dmg);
				} else {
					dmg = hero.attackProc(enemy, dmg);
				}
			}
		}

		//与你再会：击杀计数（本次技能内击杀数）。
		//必须**先造成伤害、后判定生死**：selectTarget 只返回存活目标，
		//在 damage 之前判定 isAlive() 恒为 true，击杀数会永远是 0（旧写法即踩了这一点）。
		boolean wasAlive = enemy.isAlive();
		if (dmg > 0){
			enemy.damage(dmg, hero);
		}
		final int newKills = (wasAlive && !enemy.isAlive()) ? kills + 1 : kills;
		if (enemy.isAlive()){
			//击退一格
			enemy.sprite.bloodBurstA(hero.sprite.center(), Math.max(dmg, 1));
			enemy.sprite.flash();
			int dir = enemy.pos - hero.pos;
			Ballistica trajectory = new Ballistica(enemy.pos, enemy.pos + dir, Ballistica.MAGIC_BOLT);
			WandOfBlastWave.throwChar(enemy, trajectory, 1, false, false, FuriosoReplica.this);
		}

		//停滞（攻击越来越快）：第1次后1秒，第2次后0.5秒，第3~7次后0.2秒，
		//第8次后1秒（让第九次的收尾更有厚重感）；第9次后直接结束
		if (attackIndex + 1 >= MAX_ATTACKS){
			finish(armor, hero, hitsDone + 1, newKills);
		} else {
			final float pause;
			if (attackIndex == 0 || attackIndex == 7) pause = 1f;
			else if (attackIndex == 1)               pause = 0.5f;
			else                                     pause = 0.2f;

			Delayer delayer = new Delayer(pause);
			delayer.listener = new Tweener.Listener() {
				@Override
				public void onComplete(Tweener tweener) {
					doAttack(armor, hero, attackIndex + 1, hitsDone + 1, newKills);
				}
			};
			hero.sprite.parent.add(delayer);
		}
	}

	//视野内随机选择一个可选目标（周围有可站立空地的存活敌对目标）
	private Char selectTarget(Hero hero){
		ArrayList<Char> candidates = new ArrayList<>();
		for (Char ch : Actor.chars()){
			if (ch != hero && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY
					&& Dungeon.level.heroFOV[ch.pos] && !hero.isCharmedBy(ch)
					&& findAdjacentCell(ch) != -1){
				candidates.add(ch);
			}
		}
		if (candidates.isEmpty()) return null;
		return Random.element(candidates);
	}

	//目标周围一个可站立的空地（8邻域，优先选择与目标距离更近的格，参考双蛇杖镰刀武技）
	private static int findAdjacentCell(Char enemy){
		int best = -1;
		for (int i : PathFinder.NEIGHBOURS8){
			int c = enemy.pos + i;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
				if (best == -1 || Dungeon.level.trueDistance(c, enemy.pos) < Dungeon.level.trueDistance(best, enemy.pos)){
					best = c;
				}
			}
		}
		return best;
	}

	//技能结束：与你再会返还充能、移除附魔强度加成、显示结束语并消耗一回合
	private void finish(ClassArmor armor, Hero hero, int hitsDone, int kills){
		//与你再会：每击杀1位敌人或有1次攻击未使用，获得 3/4/5/6% 盔甲充能
		int reunionPoints = hero.pointsInTalent(Talent.REUNION);
		if (reunionPoints > 0){
			int unused = MAX_ATTACKS - hitsDone;
			armor.charge = Math.min(100, armor.charge + (kills + unused) * (0.03f * reunionPoints));
			armor.updateQuickslot();
		}

		//移除神的技艺的附魔强度加成
		Buff.detach(hero, EnchantBoostTracker.class);

		//结束语（三种之一，随机，白色）——文本与其语音成对取用，同步播放
		String[] ending = Random.element(ENDING_LINES);
		hero.sprite.showStatus(CharSprite.DEFAULT, ending[0]);
		Sample.INSTANCE.play(ending[1]);

		hero.spendAndNext(Actor.TICK); //整个技能只消耗一回合
	}

	//神的技艺 +3/+4：附魔强度加成（叠加到附魔威力倍率上，见 Weapon.Enchantment.genericProcChanceMultiplier）
	public static class EnchantBoostTracker extends Buff {
		public float boost = 0f;
	}

	@Override
	public int icon() {
		return HeroIcon.FURIOSO_REPLICA;
	}

	@Override
	public Talent[] talents() {
		return new Talent[]{Talent.PERFECT_REPLICATION, Talent.DIVINE_ARTISTRY, Talent.REUNION, Talent.HEROIC_ENERGY};
	}
}
