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
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MorphCombo;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.CellEmitter;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The base form of the shape-shifting weapon: a pair of dark gloves hiding nine weapons.
 * <p>Displayed as tier 5, but applies the stats of {@link Gloves} (tier 1: very fast
 * 2x speed, low per-hit damage), per the series design doc.</p>
 * <p>Holds the series' finisher skill <b>Furioso</b>: usable only when the {@link MorphCombo}
 * buff has reached {@link MorphCombo#MAX_COMBO} stacks (all nine forms used). It plays nine
 * attack animations in sequence, then lands a single massive hit.</p>
 * <p>Two conveniences for that finisher:</p>
 * <ul>
 *   <li>索敌距离 {@link #FURIOSO_RANGE} 格（本体近战只有 1 格），可直接锁定远处敌人，
 *       释放时英雄会闪身到目标身边；</li>
 *   <li>连击集齐后 {@link #defaultAction()} 由"切换"变为"释放 Furioso"，
 *       配合 {@link MorphWeapon#scheduleAutoSwitch} 的"集齐即切回本体"，可以打满九层后直接开大。</li>
 * </ul>
 */
public class DarkSilence extends MorphWeapon {

	public static final String AC_FURIOSO = "FURIOSO";

	//Furioso 的终结一击：9 倍武器伤害、必中（数值可调）
	private static final float FURIOSO_DMG_MULTI = 9f;
	private static final int   FURIOSO_DMG_BONUS = 0;

	//Furioso 播放的攻击动画次数
	private static final int FURIOSO_HITS = 9;

	//Furioso 的索敌距离（格）：本体近战距离只有 1，这里放宽到 9 格（释放后英雄会闪身到目标身边）
	public static final int FURIOSO_RANGE = 9;

	{
		image = ItemSpriteSheet.TEST_WEAPON;
		hitSound = Assets.Sounds.HIT;
		hitSoundPitch = 1.3f;

		tier = 5; //display tier; actual stats follow tier-1 gloves
		DLY = 0.5f; //2x speed

		bones = false;
	}

	@Override
	public int min(int lvl) {
		return 1 + lvl; //tier-1 gloves
	}

	@Override
	public int max(int lvl) {
		return  5 +       //5 base, tier-1 gloves
				lvl;      //+1 per level
	}

	@Override
	public int STRReq(int lvl) {
		int req = STRReq(1, lvl); //tier-1 gloves strength requirement
		if (masteryPotionBonus){
			req -= 2;
		}
		return req;
	}

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		MorphCombo combo = hero.buff(MorphCombo.class);
		if (combo != null && combo.comboCount() >= MorphCombo.MAX_COMBO){
			actions.add(AC_FURIOSO);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_FURIOSO)){
			return "Furioso";
		} else {
			return super.actionName(action, hero);
		}
	}

	/**
	 * 快捷栏点击的默认动作：连击集齐且本体在手时为 <b>释放 Furioso</b>，否则仍是"切换"。
	 * <p>自动切换在集齐九层后会把本体送回手中（见 {@link MorphWeapon#scheduleAutoSwitch}），
	 * 二者配合使得"打满九层 → 直接点快捷栏开大"一气呵成。
	 * <p>未装备时不改判（仍返回"切换"），避免背包里的本体把"切换"入口顶掉——
	 * 毕竟切换窗口是手动换形态的唯一入口。
	 */
	@Override
	public String defaultAction() {
		Hero hero = Dungeon.hero;
		if (hero != null && isEquipped(hero)){
			MorphCombo combo = hero.buff(MorphCombo.class);
			if (combo != null && combo.comboCount() >= MorphCombo.MAX_COMBO){
				return AC_FURIOSO;
			}
		}
		return super.defaultAction();
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_FURIOSO)){

			if (!isEquipped(hero)){
				GLog.w("需要装备漆黑噤默才能使用Furioso。");
				return;
			}

			MorphCombo combo = hero.buff(MorphCombo.class);
			if (combo == null || combo.comboCount() < MorphCombo.MAX_COMBO){
				GLog.w("漆黑噤默的连击尚未集齐，无法使用Furioso。");
				return;
			}

			usesTargeting = true;
			GameScene.selectCell(targeter);
		}
	}

	private CellSelector.Listener targeter = new CellSelector.Listener() {
		@Override
		public void onSelect(Integer cell) {
			if (cell == null) return;
			final Hero hero = Dungeon.hero;
			final Char enemy = Actor.findChar(cell);
			if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[cell] || hero.isCharmedBy(enemy)){
				GLog.w("无效的目标");
			} else if (!inFuriosoRange(hero, enemy)){
				GLog.w("目标不在攻击范围内（需在 " + FURIOSO_RANGE + " 格之内）");
			} else {
				hero.busy();
				furiosoAttack(enemy, FURIOSO_HITS);
			}
		}

		@Override
		public String prompt() {
			return "选择Furioso的目标（" + FURIOSO_RANGE + "格内）";
		}
	};

	/**
	 * Furioso 的索敌范围判定：<b>不</b>走 {@link Hero#canAttack}——本体的近战距离只有 1 格，
	 * 而本技能允许锁定 {@link #FURIOSO_RANGE} 格之内的敌人（释放时英雄会闪身到目标身边）。
	 * 距离口径为切比雪夫距离（{@link com.shatteredpixel.shatteredpixeldungeon.levels.Level#distance(int, int)}），
	 * 与近战/远程的攻击距离口径一致。
	 */
	public static boolean inFuriosoRange(Hero hero, Char enemy){
		return hero != null && enemy != null && Dungeon.level.distance(hero.pos, enemy.pos) <= FURIOSO_RANGE;
	}


	/**
	 * 连续播放 {@code hitsLeft} 次攻击动画，<b>每次动画前都随机闪身到目标周围的一格</b>；
	 * 最后一次动画结束后造成一次特大的伤害，并消耗掉全部漆黑噤默连击层数。
	 * 参考武僧连击 FURY 的动画递归 + {@code FuriosoReplica} 的瞬移写法。
	 */
	private void furiosoAttack(final Char enemy, final int hitsLeft){
		final Hero hero = Dungeon.hero;

		//闪身：随机绕到目标周围的一格再出手（目标四周无可用空地时本次不位移，技能照常推进）
		blinkAround(hero, enemy);

		hero.sprite.attack(enemy.pos, new Callback() {
			@Override
			public void call() {
				if (hitsLeft > 1){
					//还有动画要播
					furiosoAttack(enemy, hitsLeft - 1);
				} else {
					//第九次动画结束：终结一击
					AttackIndicator.target(enemy);
					if (hero.attack(enemy, FURIOSO_DMG_MULTI, FURIOSO_DMG_BONUS, Char.INFINITE_ACCURACY)){
						Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
					}
					Invisibility.dispel();
					hero.spendAndNext(hero.attackDelay());

					//消耗全部连击层数
					MorphCombo combo = hero.buff(MorphCombo.class);
					if (combo != null){
						combo.detach();
					}
				}
			}
		});
	}

	/**
	 * 把英雄瞬间移动到目标周围的<b>随机</b>一格（无位移动画），到达后带一团白烟与音效。
	 * 优先避开英雄当前所在格，避免出现"原地不动"的一击；目标四周被占满时退回原地，
	 * 完全没有可站立格时不做任何事（返回 false），由调用方照常推进动画。
	 *
	 * @return 是否发生了位移。
	 */
	private static boolean blinkAround(Hero hero, Char enemy){
		int dest = randomCellAround(enemy, hero.pos);
		if (dest == -1){
			return false;
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

		return true;
	}

	/**
	 * 在目标 8 邻域里随机取一个可站立（passable）且无其他角色的格；
	 * 英雄当前格 {@code heroPos} 只作为最后手段（四周被围死时允许原地）。
	 *
	 * @return 目标格索引；无可站立格时返回 -1。
	 */
	private static int randomCellAround(Char enemy, int heroPos){
		ArrayList<Integer> candidates = new ArrayList<>();
		boolean heroCellUsable = false;

		for (int offset : PathFinder.NEIGHBOURS8){
			int cell = enemy.pos + offset;
			if (cell < 0 || cell >= Dungeon.level.length() || !Dungeon.level.passable[cell]){
				continue;
			}
			if (cell == heroPos){
				//英雄当前格：格子本身被英雄占着（findChar 必然返回英雄），
				//故单独记为"退路"，仅在四周再无可站立空地时使用
				heroCellUsable = true;
				continue;
			}
			if (Actor.findChar(cell) != null){
				continue; //被其他角色占住
			}
			candidates.add(cell);
		}

		if (!candidates.isEmpty()){
			return Random.element(candidates);
		}
		return heroCellUsable ? heroPos : -1;
	}

	@Override
	public String name() {
		return "漆黑噤默";
	}

	@Override
	public String desc() {
		return "内部藏有九把不同武器的漆黑手套。可以消除挥动武器的声音，在寂静无声中发动攻击。\n\n这是一件非常快的武器。";
	}
}
