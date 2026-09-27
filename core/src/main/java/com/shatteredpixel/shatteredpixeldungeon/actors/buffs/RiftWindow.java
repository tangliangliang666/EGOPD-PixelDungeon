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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DimensionalRipper;
import com.shatteredpixel.shatteredpixeldungeon.levels.Level;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 「空间裂隙」窗口：{@link DimensionalRipper} 的「转移」把任意一方传送走之后，
 * 给英雄开的一扇出招窗口——窗口内右下角副槽（槽位 1）出现「空间撕裂」按钮。
 *
 * <h3>时长</h3>
 * <p>{@link #WINDOW_TURNS} ＝ 3，语义是「<b>触发当回合（余下的行动）+ 其后 2 个回合</b>」。
 * 实现上靠 {@code Buff} 自己按回合记账（{@code spend(TICK)}），挂上就占一格，往后每回合扣一格，
 * 扣到 0 关窗。按钮右下角的数字就是剩余格子数。</p>
 *
 * <p>关键的一行是 {@link #open(Char)} 里的 {@code timeToNow()}。新挂载的 buff 由
 * {@code Buff.attachTo → Char.add(buff) → Actor.add(buff)}（`actor.time += now`）本就调度在
 * {@code now}，当回合即 {@code act}，所以对新挂载它是**空操作**；真正起作用的是<b>重复触发</b>——
 * 此时 buff 已存在、{@code time} 已被 {@code spend(TICK)} 推到 {@code now+1}，不把时间拉回
 * {@code now} 就会白送一格（窗口变成 4 回合）。</p>
 *
 * <h3>为什么是 Buff 而不是常驻物品逻辑</h3>
 * <p>右下角的动作槽位，游戏里所有候选（狂暴、连击、武技充能、苦痛技艺、封印之剑的切换主副……）
 * 都是「{@code Buff implements ActionIndicator.Action}」并自己注册／回收的（参考
 * {@code MeleeWeapon.Charger}、{@code ScorchingWound}、{@code SealedSwordBase.SwordSwap}）。
 * 本 buff 占<b>副槽</b>（槽位 1，配 {@code SPDAction.TAG_ACTION_2}、默认不绑键），
 * 与主槽上的职业技能按钮同时显示、互不顶掉。</p>
 *
 * <p><b>已知代价（有意保留）</b>：副槽目前只有两个候补——本窗口与封印之剑的
 * {@code SwordSwap}（「切换主副」，常驻占用）。同时满足「一只手拿次元撕裂者、另一只手拿封印之剑」
 * 且窗口恰好开着时，两者会争同一个槽位（本项目里这一幕需要同时持有两件终局级物品，极罕见）。
 * 本 buff 每回合都会重新占位，所以最坏情况只是那一两回合里按钮偶尔闪一下，
 * 不影响的判定本身。</p>
 *
 * <h3>刻意不随存档持久化</h3>
 * <p>窗口锁定的目标是一个 {@code Char} 引用，而 {@code Char} 无法随存档搬运，所以本 buff
 * <b>不重写</b> {@code storeInBundle}/{@code restoreFromBundle}：读档后 {@code enemy} 为 null、
 * {@code turnsLeft} 为 0，{@link #stillValid()} 直接判否，第一次 {@code act()} 就自行关闭
 * （{@link #attachTo} 也已按 {@code stillValid()} 把关，不会挂出一个空按钮）。窗口只有 3 回合，
 * 这点代价可以接受。</p>
 */
public class RiftWindow extends Buff implements ActionIndicator.Action {

	/** 窗口长度：触发当回合（余下的行动）+ 其后 2 个回合。 */
	public static final int WINDOW_TURNS = 3;

	/** 触发那次传送时被攻击的敌人——「空间撕裂」唯一的落点依据。 */
	private Char enemy;

	/** 开窗时所在楼层：换层即作废。 */
	private Level openLevel;

	/** 剩余可用回合数（按钮右下角显示的数字）。 */
	private int turnsLeft;

	/** 英雄当前是否处在「空间撕裂」可用的窗口内。 */
	public static RiftWindow active(Hero hero){
		return hero == null ? null : hero.buff(RiftWindow.class);
	}

	/** 窗口锁定的敌人；窗口已失效时为 null。 */
	public Char enemy(){
		return stillValid() ? enemy : null;
	}

	/** 剩余回合数（不会为负）。 */
	public int turnsLeft(){
		return Math.max(0, turnsLeft);
	}

	/**
	 * 开窗；窗口已存在时把它<b>重置</b>为满长（不叠加——连续触发只是保住 3 回合）。
	 * 由 {@link DimensionalRipper#proc} 在传送成功后调用。
	 */
	public void open(Char enemy){
		this.enemy = enemy;
		this.openLevel = Dungeon.level;
		this.turnsLeft = WINDOW_TURNS;

		//把调度时间拉回 now。新建时 buff 的 time 本就是 now（Char.add → Actor.add 里 `time += now`），
		//此调用无副作用；**重复触发**时 buff 已存在、time 已被 spend(TICK) 推到 now+1，
		//不拉回来这一格就不算进窗口（白送一格 ⇒ 变 4 回合）。
		timeToNow();
		keepSlot();

		if (target == Dungeon.hero){
			GLog.i(Messages.get(DimensionalRipper.class, "window_open"));
		}
	}

	/**
	 * 窗口此刻是否仍然成立：英雄还握着这把武器、没换层、目标还活着。
	 * 按钮与 {@code act()} 共用这一个判据，避免两处各写一套。
	 */
	public boolean stillValid(){
		if (!(target instanceof Hero)){
			return false;
		}
		Hero hero = (Hero) target;
		if (DimensionalRipper.equipped(hero) == null){
			return false;
		}
		if (openLevel != Dungeon.level){
			return false;
		}
		return enemy != null && enemy.isAlive();
	}

	@Override
	public boolean act() {
		turnsLeft--;
		if (turnsLeft <= 0 || !stillValid()){
			detach();
			return true;
		}
		//窗口还开着：重新占住副槽（可能被别的动作挤掉，场景重建后也要重挂）
		keepSlot();
		spend(TICK);
		return true;
	}

	private void keepSlot(){
		if (ActionIndicator.secondAction != this){
			ActionIndicator.setSecondAction(this);
		}
	}

	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target)){
			//刚 open() 之前还不成立；读档恢复出来的窗口没有目标引用，也不该挂出按钮
			if (stillValid()){
				keepSlot();
			}
			return true;
		}
		return false;
	}

	@Override
	public void fx(boolean on) {
		//场景重建／英雄精灵刷新之后把按钮重新挂回右下角（照 MeleeWeapon.Charger 的写法）
		if (on && stillValid()){
			keepSlot();
		}
	}

	@Override
	public void detach() {
		super.detach();
		//只在槽位确实是自己时才清（副槽可能已经被封印之剑的 SwordSwap 拿回去了）
		ActionIndicator.clearAction(this);
	}

	@Override
	public int icon() {
		//窗口靠右下角按钮表达，不占 buff 条
		return BuffIndicator.NONE;
	}

	//==========================================================================
	// ActionIndicator.Action：右下角副槽上的「空间撕裂」
	//==========================================================================

	@Override
	public String actionName() {
		return Messages.get(DimensionalRipper.class, "space_tear");
	}

	/**
	 * 按钮主图标＝英雄手上那把次元撕裂者（照交换按钮：加宽 4px 让视觉重心左移，
	 * 给右下角的剩余回合数腾位置）。理论上取不到（取不到说明窗口已失效），退回静态图标。
	 */
	@Override
	public Visual primaryVisual() {
		DimensionalRipper wep = DimensionalRipper.equipped(Dungeon.hero);
		Image ico = wep != null
				? new ItemSprite(wep)
				: new ItemSprite(ItemSpriteSheet.DIMENSIONAL_RIPPER);
		ico.width += 4;
		return ico;
	}

	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text(Integer.toString(turnsLeft()));
		txt.hardlight(CharSprite.POSITIVE);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		return 0x8A5CD6; //次元紫，与武器图标同色系
	}

	/**
	 * 出招：撕开空间落到目标身旁，随即发动一次攻击，然后关窗（一次性）。
	 *
	 * <p>落点用 {@link ScrollOfTeleportation#appear} 而非
	 * {@code teleportToLocation}——后者要先过一遍「可达性」判定，而这里要的正是
	 * <b>无视距离与地形连通性</b>（本作 {@code ArtTechniques} 的「即刻解剖」走同一条路）。</p>
	 */
	@Override
	public void doAction() {
		Hero hero = Dungeon.hero;

		//按钮是在渲染线程被按下的，窗口可能已经在回合线程里失效，这里再判一次
		if (hero == null || !hero.ready || !stillValid()){
			detach();
			return;
		}

		final Char victim = enemy;

		int dest = emptyCellAround(victim);
		if (dest == -1){
			GLog.w(Messages.get(DimensionalRipper.class, "no_space"));
			return;
		}

		hero.busy();
		//先把出发点记下来：appear() 会把 hero.pos 改写成落点
		final int from = hero.pos;
		ScrollOfTeleportation.appear(hero, dest);
		//与武器自己的「转移」共用同一层裂隙粒子（appear 不给英雄出粒子，出入场全靠它）
		DimensionalRipper.teleportFx(from, dest);
		Dungeon.observe();
		GameScene.updateFog();

		hero.sprite.attack(victim.pos, new Callback() {
			@Override
			public void call() {
				AttackIndicator.target(victim);
				if (hero.attack(victim, 1f, 0f, Char.INFINITE_ACCURACY)){
					Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
				}
				hero.spendAndNext(hero.attackDelay());
			}
		});
		//一次性：用掉即关窗
		detach();
	}

	/** 目标周围 8 格里随机一个可站立、且没被占住的格子；没有则返回 -1。 */
	private static int emptyCellAround(Char target){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = target.pos + i;
			if (Dungeon.level.insideMap(c) && Dungeon.level.passable[c] && Actor.findChar(c) == null){
				candidates.add(c);
			}
		}
		if (candidates.isEmpty()){
			return -1;
		}
		return Random.element(candidates);
	}

}
