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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTeleportation;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfBlastWave;
import com.shatteredpixel.shatteredpixeldungeon.mechanics.Ballistica;
import com.shatteredpixel.shatteredpixeldungeon.scenes.CellSelector;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.ActionIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.AttackIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.ui.HeroIcon;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndScorchingSkill;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Callback;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 命运弃子专精的「烧灼的伤口」buff。
 * <p>每当指令终端响起（*哔哔*）时叠加 1 层；持续时间无限，层数上限 {@link #MAX_STACKS}。
 * 层数超过 10 时，每回合有（层数×10-100）% 概率受到 1 点火焰伤害（不造成燃烧）。
 * 可被命运弃子的苦痛技艺消耗使用。</p>
 * <p>苦痛技艺的入口是右下角的 {@link ActionIndicator}（参考角斗士连击），
 * 而非指令终端上的按钮：至少 1 层时显示，点击后打开 {@link WndScorchingSkill} 选择招数。</p>
 */
public class ScorchingWound extends Buff implements ActionIndicator.Action {

	public static final int MAX_STACKS = 20;

	{
		type = buffType.NEUTRAL;
	}

	private int stacks = 0;

	public int stacks(){
		return stacks;
	}

	public void gainStack(){
		stacks = Math.min(MAX_STACKS, stacks + 1);
		BuffIndicator.refreshHero();
		//右下角苦痛技艺入口：层数 ≥1 且为命运弃子时显示
		if (shouldShowAction()){
			ActionIndicator.setAction(this);
		} else {
			ActionIndicator.clearAction(this);
		}
	}

	//尝试消耗层数，不足则返回 false
	public boolean useStacks(int amount){
		if (stacks < amount) return false;
		stacks -= amount;
		BuffIndicator.refreshHero();
		if (shouldShowAction()){
			ActionIndicator.refresh(); //更新右下角显示的层数
		} else {
			ActionIndicator.clearAction(this);
		}
		return true;
	}

	//烧灼之痛：层数超过 10 时，每回合有（层数×10-100）% 概率受到 1 点火焰伤害（不造成燃烧）
	@Override
	public boolean act() {
		if (target.isAlive() && stacks > 10){
			if (Random.Float(100f) < stacks * 10 - 100){
				target.damage(1, this);
			}
		}
		spend(TICK);
		return true;
	}

	//========== 苦痛技艺：右下角 ActionIndicator 入口（参考角斗士连击） ==========

	//是否显示右下角苦痛技艺入口：命运弃子且至少 1 层烧灼的伤口（满足最便宜招数的消耗）
	private boolean shouldShowAction(){
		return target instanceof Hero
				&& ((Hero) target).subClass == HeroSubClass.FATE_FORSAKEN
				&& stacks >= 1;
	}

	@Override
	public String actionName() {
		return "苦痛技艺";
	}

	@Override
	public int actionIcon() {
		//复用命运弃子的专精图标（hero_icons.png 第 13 帧）
		return HeroIcon.FATE_FORSAKEN;
	}

	//右下角小字：当前烧灼的伤口层数
	@Override
	public Visual secondaryVisual() {
		BitmapText txt = new BitmapText(PixelScene.pixelFont);
		txt.text(Integer.toString(stacks));
		txt.hardlight(CharSprite.NEGATIVE);
		txt.measure();
		return txt;
	}

	@Override
	public int indicatorColor() {
		return 0xDD3322; //烧灼的暗红
	}

	@Override
	public void doAction() {
		GameScene.show(new WndScorchingSkill(this));
	}

	//绑定目标后（含读档时 Char.restoreFromBundle → attachTo）注册右下角苦痛技艺入口。
	//注意不能在 restoreFromBundle 里注册：反序列化时 target 尚未绑定（为 null），
	//无法判断 subClass，所以统一在 attachTo（target 已就位）时注册。
	@Override
	public boolean attachTo(Char target) {
		if (super.attachTo(target)){
			if (shouldShowAction()){
				ActionIndicator.setAction(this);
			}
			return true;
		}
		return false;
	}

	@Override
	public void detach() {
		super.detach();
		ActionIndicator.clearAction(this);
	}

	//========== 苦痛技艺 ==========

	public enum ScorchingMove {
		SHATTER_SOUND(1),  //粉碎声音，踏平思维
		PIERCE_SILENCE(2), //刺入静寂的心，贯穿记忆
		REFINED_SLASH(3);  //以淬炼锋利的秘技，斩落其形

		public final int req;

		ScorchingMove(int req){
			this.req = req;
		}

		public String title(){
			switch (this){
				case SHATTER_SOUND: default: return "粉碎声音，踏平思维";
				case PIERCE_SILENCE: return "刺入静寂的心，贯穿记忆";
				case REFINED_SLASH: return "以淬炼锋利的秘技，斩落其形";
			}
		}

		public String desc(){
			switch (this){
				case SHATTER_SOUND: default:
					return "消耗_1层_烧灼的伤口：选定_4格内_的目标，瞬移至其身边造成一次_100%_攻击，随后获得_5回合隐身_并对目标施加_5回合致盲_。";
				case PIERCE_SILENCE:
					return "消耗_2层_烧灼的伤口：对_攻击范围内_的目标造成一次_50%_攻击并_击退三格_，同时对目标施加_5回合_的_残废_和_眩晕_。";
				case REFINED_SLASH:
					return "消耗_3层_烧灼的伤口：选定_8格内_的目标，瞬移至其身边造成_200%_伤害，并造成_伤害量一半_的_流血_。";
			}
		}
	}

	//使用一个苦痛技艺（消耗对应层数）
	public void useMove(ScorchingMove move){
		if (target instanceof Hero){
			final Hero hero = (Hero) target;
			//所有招数都需要选择目标
			GameScene.selectCell(new CellSelector.Listener() {
				@Override
				public void onSelect(Integer cell) {
					if (cell == null) return;
					Char enemy = Actor.findChar(cell);
					if (enemy == null || enemy == hero || !Dungeon.level.heroFOV[cell] || hero.isCharmedBy(enemy)){
						GLog.w("无效的目标");
						return;
					}
					//招数专属的距离检查
					int dist = Dungeon.level.distance(hero.pos, enemy.pos);
					switch (move){
						case SHATTER_SOUND:
							if (dist > 4){
								GLog.w("目标过远");
								return;
							}
							break;
						case PIERCE_SILENCE:
							if (!hero.canAttack(enemy)){
								GLog.w("目标不在攻击范围内");
								return;
							}
							break;
						case REFINED_SLASH:
							if (dist > 8){
								GLog.w("目标过远");
								return;
							}
							break;
					}
					useMoveOnTarget(hero, enemy, move);
				}
				@Override
				public String prompt() {
					return "选择苦痛技艺的目标";
				}
			});
		}
	}

	//对选定目标执行招数
	private void useMoveOnTarget(final Hero hero, final Char enemy, ScorchingMove move){
		//先验证可行性（瞬移招数需要目标周围有空地），再消耗层数
		final int blinkCell;
		if (move == ScorchingMove.SHATTER_SOUND || move == ScorchingMove.REFINED_SLASH){
			blinkCell = emptyCellAround(enemy);
			if (blinkCell == -1){
				GLog.w("目标周围没有可站立的空地");
				return;
			}
		} else {
			blinkCell = -1;
		}

		if (!useStacks(move.req)) {
			GLog.w("烧灼的伤口不足");
			return;
		}

		hero.busy();

		switch (move){
			case SHATTER_SOUND:
			case REFINED_SLASH:
				//瞬间移动到目标周围，再发动攻击
				ScrollOfTeleportation.appear(hero, blinkCell);
				final float multi = (move == ScorchingMove.SHATTER_SOUND) ? 1f : 2f;
				hero.sprite.attack(enemy.pos, new Callback() {
					@Override
					public void call() {
						AttackIndicator.target(enemy);
						final int beforeHP = enemy.HP;
						if (hero.attack(enemy, multi, 0, Char.INFINITE_ACCURACY)){
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
						if (move == ScorchingMove.SHATTER_SOUND){
							//粉碎声音，踏平思维：获得5回合隐身，目标5回合致盲
							Buff.prolong(hero, Invisibility.class, 5f);
							if (enemy.isAlive()){
								Buff.prolong(enemy, Blindness.class, 5f);
							}
						} else if (enemy.isAlive()){
							//以淬炼锋利的秘技，斩落其形：造成伤害量一半的流血
							int dealt = Math.max(0, beforeHP - enemy.HP);
							if (dealt > 0){
								Buff.affect(enemy, Bleeding.class).set(dealt / 2f);
							}
						}
						hero.spendAndNext(hero.attackDelay());
					}
				});
				break;

			case PIERCE_SILENCE:
				//刺入静寂的心，贯穿记忆：50%攻击 + 击退三格 + 残废与眩晕
				hero.sprite.attack(enemy.pos, new Callback() {
					@Override
					public void call() {
						AttackIndicator.target(enemy);
						if (hero.attack(enemy, 0.5f, 0, Char.INFINITE_ACCURACY)){
							Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG);
						}
						if (enemy.isAlive()){
							//沿英雄→目标的直线方向击退三格（参考法术冲击波的击退）
							int dir = enemy.pos - hero.pos;
							Ballistica trajectory = new Ballistica(enemy.pos, enemy.pos + dir, Ballistica.MAGIC_BOLT);
							WandOfBlastWave.throwChar(enemy, trajectory, 3, false, false, ScorchingWound.this);
							Buff.prolong(enemy, Cripple.class, 5f);
							Buff.prolong(enemy, Vertigo.class, 5f);
						}
						hero.spendAndNext(hero.attackDelay());
					}
				});
				break;
		}
	}

	//寻找目标周围一个可站立的空地（8邻域随机），没有则返回 -1
	private static int emptyCellAround(Char target){
		ArrayList<Integer> candidates = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			int c = target.pos + i;
			if (Dungeon.level.insideMap(c) && Dungeon.level.passable[c] && Actor.findChar(c) == null){
				candidates.add(c);
			}
		}
		if (candidates.isEmpty()) return -1;
		return Random.element(candidates);
	}

	@Override
	public int icon() {
		return BuffIndicator.SCORCHING_WOUND;
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString(stacks);
	}

	@Override
	public String name() {
		return "烧灼的伤口";
	}

	@Override
	public String desc() {
		return "伴随着终端的蜂鸣声，你的伤疤正在烧灼作痛。\n\n层数超过 10 时，每回合有（层数×10-100）% 的概率受到 1 点火焰伤害（不会引起燃烧）。";
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put(STACKS, stacks);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		stacks = Math.min(MAX_STACKS, bundle.getInt(STACKS));
		//读档兜底注册右下角苦痛技艺入口：Hero.restoreFromBundle 先恢复 buffs（attachTo）后恢复 subClass，
		//attachTo 里依赖 subClass 的判定必然失败；本 buff 只会在转职 FATE_FORSAKEN 时被施加，
		//故在自身状态恢复后直接注册（同官方 Berserk.restoreFromBundle 模式）
		if (stacks >= 1){
			ActionIndicator.setAction(this);
		}
	}

}
