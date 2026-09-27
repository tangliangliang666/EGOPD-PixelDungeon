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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Actor;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.ArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barkskin;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Barrier;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.BlobImmunity;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Haste;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Invisibility;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Levitation;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.LockedFloor;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Stamina;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MeltingLove;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.Speck;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.Potion;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfExperience;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfFrost;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHaste;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfInvisibility;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLevitation;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfLiquidFlame;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfParalyticGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfPurity;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfStrength;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.PotionOfToxicGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfArcaneArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfFeatherFall;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfHoneyedHealing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCleansing;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfCorrosiveGas;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDivineInspiration;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfDragonsBreath;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfEarthenArmor;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShielding;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfShroudingFog;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfSnapFreeze;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStamina;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.exotic.PotionOfStormClouds;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.Scroll;
import com.shatteredpixel.shatteredpixeldungeon.items.scrolls.ScrollOfTransmutation;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.sprites.MeltingLoveSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 爱慕（Admiration）：神器「爱慕」（2026-09-05，效果按枯萎玫瑰思路实现）。
 *
 * <p>机制：充能上限 100（状态栏按百分比显示）。未召唤时充能随时间缓缓蓄积，
 * 蓄满即自动召唤友方召唤物「溶解之爱」（{@link MeltingLove}，不可装备武器/防具），
 * 此后神器的充能实时反映溶解之爱的血量百分比。溶解之爱存活时按存活回合数成长：
 * 每满 150+50n 回合（n=升级前等级）神器升 1 级，其数值随之成长（等效蜜蜂等级=神器等级×3+5）。</p>
 *
 * <p>初始必定带诅咒，但诅咒不影响上述行为。若在溶解之爱存活时摘除爱慕（需先祛除诅咒），
 * 会红色提示"溶解之爱焦躁不安地看着你的方向"，10 回合内未重新装备则溶解之爱反噬英雄。</p>
 *
 * <p><b>「神器充能」buff</b>（秘能卷轴 / 冥想 / 魔杖天赋给的 {@code ArtifactRecharge}）走
 * {@link #charge(Hero, float)}：溶解之爱不在场时加速蓄能；在场时转为<b>回复她的生命</b>
 * ——与同属「神器 × 召唤物」的 {@link DriedRose} 同一套双路写法。</p>
 *
 * <p>贴图四档随充能切换（0-24/25-49/50-74/75-100），见 {@link #updateImage()}。</p>
 */
public class Admiration extends Artifact {

	//未召唤时的充能速度（回合数 ~ 蓄满时间，可调）
	private static final int CHARGE_PER_TURN = 2;

	/** 「神器充能」buff 未召唤溶解之爱时的换算系数——参考干枯玫瑰：一次满额（30 回合）≈ 蓄满一条 100。 */
	private static final float RECHARGE_CHARGE_PER_TURN = 4f;

	{
		image = ItemSpriteSheet.ARTIFACT_ADMIRATION1;

		levelCap = 10;
		chargeCap = 100;

		cursed = true;   //初始必定诅咒；诅咒不影响行为（卸下需先祛除诅咒）
	}

	/**
	 * 爱慕<b>不是专属神器</b>，允许在商店出售。
	 *
	 * <p>本神器本就没有 {@code unique} 标记，默认即可售；此处显式覆写以把该语义固定在类上——
	 * 日后若有人为其它目的给它补上 {@code unique}，也不会顺带禁售。</p>
	 *
	 * <p>注意：因初始必被诅咒，<b>装备中的爱慕</b>仍会被 {@code Shopkeeper.canSell} 的
	 * 「已装备且被诅咒」通则拦下——需先用祛除诅咒卷轴解咒、卸下后才能卖出（与原版诅咒装备一致）。</p>
	 */
	@Override
	public boolean sellable() {
		return true;
	}

	/**
	 * 允许被嬗变卷轴选为嬗变对象（本神器本就没有 {@code unique}，默认即可嬗变；显式覆写以固定语义）。
	 *
	 * <p>被嬗变掉时的收尾见 {@link #onTransmuted}——爱慕一去不返，溶解之爱照常进入反叛倒计时。</p>
	 */
	@Override
	public boolean transmutable() {
		return true;
	}

	//溶解之爱存活累计回合（用于升级）
	private int survivalTurns = 0;

	//========== 「赠予」动作（2026-09-11） ==========
	//装备期间可把背包里的药水/卷轴送给溶解之爱。分类与效果见 giveToLove / applyPotionEffect。

	public static final String AC_GIFT = "GIFT";

	//溶解之爱的图像组（0/1/2 = lovenew 的第一/二/三组，见 MeltingLoveSprite.PALETTE_*）：
	//记录在神器上，因此即使溶解之爱阵亡后重新召唤，也会保持玩家之前用嬗变卷轴切换到的图像。
	private int lovePalette = MeltingLoveSprite.PALETTE_A;

	/** 溶解之爱"喜欢但拒绝"的礼物：力量类——她认为你比她更需要。返还原物、不产生效果。 */
	private static final Set<Class<?>> REFUSE_POTIONS = new HashSet<>(Arrays.asList(
			PotionOfStrength.class,
			ElixirOfMight.class,
			PotionOfExperience.class,
			PotionOfDivineInspiration.class
	));

	/** 负面（投掷型）药水：不喜欢，返还原物且不生效——即"原版只能扔、不能喝"的那些。 */
	private static final Set<Class<?>> NEGATIVE_POTIONS = new HashSet<>(Arrays.asList(
			PotionOfToxicGas.class,
			PotionOfLiquidFlame.class,
			PotionOfParalyticGas.class,
			PotionOfFrost.class,
			PotionOfCorrosiveGas.class,
			PotionOfSnapFreeze.class,
			PotionOfShroudingFog.class,
			PotionOfStormClouds.class,
			PotionOfDragonsBreath.class
	));

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		//仅装备时可赠予（artifact / misc 两个神器槽都算装备，见 KindofMisc.isEquipped）
		if (isEquipped(hero)) {
			actions.add(AC_GIFT);
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_GIFT)) {
			return "赠予";
		}
		return super.actionName(action, hero);
	}

	/**
	 * 快捷栏 / 工具栏 / 背包的"默认动作"：装备期间即「赠予」，这样把爱慕放进快捷栏点一下
	 * 就能给溶解之爱送礼（{@code Item.execute(Hero)} 每次现取本方法，见 QuickSlotButton）。
	 * 未装备时沿用父类（默认动作为空，点按无反应，与改动前一致）。
	 */
	@Override
	public String defaultAction() {
		Hero hero = Dungeon.hero;
		if (hero != null && isEquipped(hero)) {
			return AC_GIFT;
		}
		return super.defaultAction();
	}

	@Override
	public void execute(Hero hero, String action) {
		super.execute(hero, action);

		if (action.equals(AC_GIFT)) {
			MeltingLove love = MeltingLove.findLoyalAlly();
			if (love == null) {
				GLog.w("溶解之爱不在你身边，没有人可以收下礼物。");
			} else {
				GameScene.selectItem(giftSelector);
			}
		}
	}

	/** 选择背包中的药水/卷轴。 */
	private static final WndBag.ItemSelector giftSelector = new WndBag.ItemSelector() {
		@Override
		public String textPrompt() {
			return "选择要赠予溶解之爱的药水或卷轴";
		}

		@Override
		public Class<? extends Bag> preferredBag() {
			return Belongings.Backpack.class;
		}

		@Override
		public boolean itemSelectable(Item item) {
			return item instanceof Potion || item instanceof Scroll;
		}

		@Override
		public void onSelect(Item item) {
			if (item == null) return;
			MeltingLove love = MeltingLove.findLoyalAlly();
			if (love == null) {
				GLog.w("溶解之爱不在你身边，没有人可以收下礼物。");
				return;
			}
			((Admiration) curItem).giveToLove(Dungeon.hero, love, item);
		}
	};

	/**
	 * 把一件礼物交给溶解之爱。消耗/返还与表情、提示文字按礼物种类区分：
	 * <ul>
	 *   <li>正面药水（力量类、经验、神圣启示除外）→ 消耗并施加对应效果，播放"快乐"；</li>
	 *   <li>力量药剂 / 根骨秘药 / 经验药剂 / 神圣启示药剂 → 返还，不生效，播放"快乐"（她认为你更需要）；</li>
	 *   <li>负面（投掷型）药水 → 返还，不生效，播放"尴尬"；</li>
	 *   <li>嬗变卷轴 → 消耗，播放嬗变特效，并在三组图像中<b>随机换到另外一组</b>（不会原地不动）；</li>
	 *   <li>其它卷轴 → 返还，不生效，播放"尴尬"（她读不懂）。</li>
	 * </ul>
	 */
	private void giveToLove(Hero hero, MeltingLove love, Item gift) {

		//卷轴：只有嬗变卷轴能被"读懂"
		if (gift instanceof Scroll) {
			if (gift instanceof ScrollOfTransmutation) {
				gift.detach(hero.belongings.backpack);
				//在溶解之爱身上播放嬗变卷轴的特效（同 ScrollOfTransmutation 对物品生效时的粒子）
				if (love.sprite != null) {
					love.sprite.emitter().start(Speck.factory(Speck.CHANGE), 0.2f, 10);
				}
				Sample.INSTANCE.play(Assets.Sounds.READ);

				//三组图像（PALETTE_A/B/C）里随机换到「另外两组」之一：原地不动不算切换
				int next;
				do {
					next = Random.Int( MeltingLoveSprite.PALETTE_COUNT );
				} while (next == love.palette());
				lovePalette = next;
				love.setPalette(next);
				GLog.p("溶解之爱在触碰到这张卷轴时，发生了奇妙的变化！");
			} else {
				love.playEmote(MeltingLoveSprite.EMO_EMBARRASSED);
				GLog.w("溶解之爱读不懂这张卷轴...");
			}
			return;
		}

		if (!(gift instanceof Potion)) {
			//理论上选择器已过滤，这里只是兜底
			return;
		}

		if (REFUSE_POTIONS.contains(gift.getClass())) {
			love.playEmote(MeltingLoveSprite.EMO_HAPPY);
			GLog.p("溶解之爱喜欢你的礼物，但她觉得你比她更需要它。");
			return;
		}

		if (NEGATIVE_POTIONS.contains(gift.getClass())) {
			love.playEmote(MeltingLoveSprite.EMO_EMBARRASSED);
			GLog.w("溶解之爱似乎不太喜欢这份礼物...");
			return;
		}

		//正面药水：消耗并让溶解之爱获得对应效果
		applyPotionEffect(love, (Potion) gift);
		gift.detach(hero.belongings.backpack);
		love.playEmote(MeltingLoveSprite.EMO_HAPPY);
		GLog.p("溶解之爱因你的礼物而感到心生爱意。");
	}

	/**
	 * 把正面药水的效果施加到溶解之爱身上（逐种映射）。
	 * <p>原版里"只能作用于英雄"的那些（心灵视界/魔法视觉/深渊秘药/各类灵药等）统一走通用正面效果：
	 * 治愈 + 回血。</p>
	 */
	private static void applyPotionEffect(MeltingLove love, Potion potion) {
		Class<?> c = potion.getClass();

		if (c == PotionOfHealing.class || c == ElixirOfHoneyedHealing.class) {
			PotionOfHealing.cure(love);
			PotionOfHealing.heal(love);
		} else if (c == PotionOfHaste.class) {
			Buff.prolong(love, Haste.class, Haste.DURATION);
		} else if (c == PotionOfInvisibility.class) {
			Buff.prolong(love, Invisibility.class, Invisibility.DURATION);
		} else if (c == PotionOfLevitation.class) {
			Buff.prolong(love, Levitation.class, Levitation.DURATION);
		} else if (c == ElixirOfFeatherFall.class) {
			Buff.append(love, ElixirOfFeatherFall.FeatherBuff.class, ElixirOfFeatherFall.FeatherBuff.DURATION);
		} else if (c == PotionOfPurity.class) {
			Buff.prolong(love, BlobImmunity.class, BlobImmunity.DURATION);
		} else if (c == PotionOfCleansing.class) {
			PotionOfCleansing.cleanse(love);
		} else if (c == PotionOfShielding.class) {
			Buff.affect(love, Barrier.class).setShield((int) (0.6f * love.HT + 10));
		} else if (c == ElixirOfArcaneArmor.class) {
			Buff.affect(love, ArcaneArmor.class).set(5 + love.beeLevel() / 3, 80);
		} else if (c == PotionOfEarthenArmor.class) {
			Barkskin.conditionallyAppend(love, 2 + love.beeLevel() / 3, 50);
		} else if (c == PotionOfStamina.class) {
			Buff.prolong(love, Stamina.class, Stamina.DURATION);
		} else {
			//通用正面效果：治愈 + 回血（心灵视界/魔法视觉/深渊秘药/其余灵药等英雄专属效果）
			PotionOfHealing.cure(love);
			PotionOfHealing.heal(love);
		}
	}

	//========== 「神器充能」buff 的接口（2026-09-15） ==========
	//参考干枯玫瑰 DriedRose —— 它同样是「神器 × 召唤物」结构，charge() 里按召唤物在不在场走两条完全不同的路。
	//父类 Artifact.charge 是空实现，不覆写就完全吃不到这条充能（秘能卷轴 / 冥想 / 魔杖天赋等）。

	/**
	 * 「神器充能」buff（{@code ArtifactRecharge}）→ 按「溶解之爱是否存活」分双路。
	 *
	 * <ul>
	 *   <li><b>溶解之爱未存活</b>：正常蓄能——同 {@link #CHARGE_PER_TURN} 那条路，只是快得多
	 *       （{@link #RECHARGE_CHARGE_PER_TURN}：一次满额 30 回合 ≈ 蓄满一条 100，可借此提前召唤）；</li>
	 *   <li><b>溶解之爱存活</b>：充能<b>不再进能量条</b>，改为<b>回复她的生命</b>
	 *       {@code round((1 + 神器等级/3) × amount)}（与干枯玫瑰治疗幽灵同式），并在她头顶弹出治疗数字。
	 *       之所以不能顺手写回 {@code charge}：溶解之爱在场时该字段已被 {@link LoveSync} 占用为
	 *       「血量百分比」，写进去下一回合就会被抹掉；</li>
	 *   <li><b>溶解之爱存活但已反叛（敌对）</b>：两者都不做——不该给敌人回血。</li>
	 * </ul>
	 */
	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;

		MeltingLove love = MeltingLove.findLoyalAlly();
		if (love != null) {
			if (love.HP < love.HT) {
				int heal = Math.round((1 + level()/3f) * amount);
				int healed = love.heal( heal );
				if (healed > 0 && love.sprite != null) {
					love.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(healed), FloatingText.HEALING);
				}
				refreshUI();
			}
			return;
		}

		//存活但敌对（已反噬）：不蓄能、也不回血
		if (MeltingLove.findAnyAlive() != null) return;

		if (charge < chargeCap) {
			partialCharge += RECHARGE_CHARGE_PER_TURN * amount;
			while (partialCharge >= 1f) {
				partialCharge -= 1f;
				charge++;
			}
			if (charge >= chargeCap) {
				charge = chargeCap;
				partialCharge = 0f;
				//刻意不在此处弹一句"满溢"：蓄满只说明下一回合 AdmirationBuff.act() 会召唤，
				//而 summonLove() 本来就会提示"爱慕满溢，溶解之爱苏醒并追随着你。"——重复弹两遍反而吵。
			}
			refreshUI();
		}
	}

	//按当前充能切换贴图档位（0-24/25-49/50-74/75-100）
	private void updateImage() {
		if (charge >= 75) {
			image = ItemSpriteSheet.ARTIFACT_ADMIRATION4;
		} else if (charge >= 50) {
			image = ItemSpriteSheet.ARTIFACT_ADMIRATION3;
		} else if (charge >= 25) {
			image = ItemSpriteSheet.ARTIFACT_ADMIRATION2;
		} else {
			image = ItemSpriteSheet.ARTIFACT_ADMIRATION1;
		}
	}

	private void refreshUI() {
		updateImage();
		Item.updateQuickslot();
	}

	/** 统一自 Hero 身上查找爱慕神器（已装备在 artifact 槽 / misc 槽（第二神器栏）或背包里），供 LoveSync 等持久化 buff 反复调用。 */
	static Admiration findAdmirationInInventory(Hero hero) {
		if (hero.belongings.artifact instanceof Admiration) {
			return (Admiration) hero.belongings.artifact;
		}
		//第二神器栏：misc 槽同样可装备神器（KindofMisc 模型，artifact 槽被占时神器落入 misc 槽）
		if (hero.belongings.misc instanceof Admiration) {
			return (Admiration) hero.belongings.misc;
		}
		for (Item i : hero.belongings.backpack.items) {
			if (i instanceof Admiration) return (Admiration) i;
		}
		return null;
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new AdmirationBuff();
	}

	//重新装备爱慕时：若正处在"反噬倒计时"内则取消（溶解之爱仍然忠诚）
	@Override
	public void activate(Char ch) {
		super.activate(ch);
		if (ch instanceof Hero){
			BetrayalTimer timer = ((Hero) ch).buff(BetrayalTimer.class);
			if (timer != null) timer.detach();
		}
	}

	//摘除爱慕（须先祛除诅咒）：溶解之爱存活时进入 10 回合反噬倒计时，并开始播放"焦虑"表情
	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		//被诅咒时父类会拒绝卸下（神器仍捆在身上），此时也不该开始倒计时
		if (!cursed) startBetrayal( hero );
		return super.doUnequip(hero, collect, single);
	}

	/**
	 * 被「嬗变卷轴」替换掉时的收尾（{@link Item#onTransmuted} 回调，由
	 * {@code ScrollOfTransmutation.onItemSelected} 在替换前调用）。
	 *
	 * <p>爱慕被抹掉＝永远拿不回来，效果等同一次「卸下」：溶解之爱进入 10 回合反叛倒计时。</p>
	 *
	 * <p>这里<b>不看诅咒</b>——诅咒只是「摘不下来」，而嬗变直接把神器销毁。装备中被诅咒的爱慕被嬗变时，
	 * 本回调（触发点在 {@code item.cursed = false} 之前）负责起头，随后 {@code doUnequip} 会再调一次
	 * {@link #startBetrayal}，由「已在倒计时中」的判定去重，不会重复弹提示。</p>
	 */
	@Override
	public void onTransmuted(Hero hero) {
		startBetrayal( hero );
	}

	/**
	 * 让溶解之爱进入 10 回合反叛倒计时，并开始播放"焦虑"表情。
	 *
	 * <p>两个入口共用：① 正常卸下（{@link #doUnequip}）；② 被嬗变卷轴替换掉（{@link #onTransmuted}）。
	 * 已在倒计时中时只把时长刷回 10、不再重复弹提示（避免嬗变路径「回调 + 卸下」连走两遍时刷屏）；
	 * 重新装备时 {@code activate()} 会把倒计时整个取消。</p>
	 */
	private void startBetrayal( Hero hero ) {
		if (hero == null) return;
		MeltingLove love = MeltingLove.findLoyalAlly();
		if (love == null) return;
		BetrayalTimer timer = hero.buff(BetrayalTimer.class);
		if (timer == null){
			GLog.n("溶解之爱焦躁不安地看着你的方向");
			love.beginAnxious();
			timer = new BetrayalTimer();
			timer.attachTo(hero);
		}
		timer.left = 10;
	}

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		bundle.put("survival_turns", survivalTurns);
		bundle.put("love_palette", lovePalette);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		survivalTurns = bundle.getInt("survival_turns");
		//旧档无此键 → 0 = 第一组；非法值由 normalizePalette 统一回落到第一组
		lovePalette = MeltingLoveSprite.normalizePalette(bundle.getInt("love_palette"));
		updateImage();
	}

	//========== 装备期间逻辑 ==========

	public class AdmirationBuff extends ArtifactBuff {

		@Override
		public boolean act() {
			if (!(target instanceof Hero)){
				spend(TICK);
				return true;
			}
			Hero hero = (Hero) target;

			// 任何阵营的存活溶解之爱都视作"已召唤"——卸下神器或反噬后都该持续同步，不该再走蓄能。
			MeltingLove love = MeltingLove.findAnyAlive();

			if (love == null){
				// 未召唤：充能蓄满即自动召唤
				if (charge < chargeCap){
					charge = Math.min(chargeCap, charge + CHARGE_PER_TURN);
					refreshUI();
				}
				if (charge >= chargeCap){
					summonLove(hero);
				}
			}
			// 充能↔血量同步 / 升级 由持久的 LoveSync 处理（不依赖神器是否装备）——见 LoveSync.act()。

			spend(TICK);
			return true;
		}
	}

	//在英雄身边召唤溶解之爱（等效蜜蜂等级 = 神器等级×3+5）
	private void summonLove(Hero hero) {
		int cell = hero.pos;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = hero.pos + n;
			if (Dungeon.level.passable[c] && Actor.findChar(c) == null){
				cell = c;
				break;
			}
		}
		MeltingLove love = MeltingLove.summon(hero, cell, level(), lovePalette);
		if (love != null){
			charge = chargeCap;
			refreshUI();
			// 持久化同步 buff：脱下神器 / 反噬为敌对 都不再中断 充能↔血量 同步（2026-09-05 修复）
			Buff.affect(hero, LoveSync.class);
			GLog.p("爱慕满溢，溶解之爱苏醒并追随着你。");
		}
	}

	//========== 持久化同步 buff（2026-09-05 新增） ==========
	//挂载在英雄身上，存活期间独立于神器是否装备、负责"神器充能 ↔ 溶解之爱血量"持续同步。
	//解决两个旧 bug：①卸下爱慕神器后失同步；②反噬后 love 变敌对时失同步。
	//注意：必须为 static——Buff.affect/读档恢复都经反射无参构造实例化，非静态内部类没有无参构造器会崩（2026-09-06 修复）。
	//本类不引用外层 Admiration 实例，只通过 findAdmirationInInventory 查找，故可安全 static。

	public static class LoveSync extends Buff {

		{
			type = buffType.POSITIVE;
		}

		@Override
		public boolean act() {
			// 任何阵营的存活溶解之爱都视作"已召唤"
			MeltingLove love = MeltingLove.findAnyAlive();
			if (love == null){
				// 当前层无存活溶解之爱（可能在跨层持有中、或者根本没召唤过）
				// 此时若英雄身上已无爱慕神器（背包/装备槽都没有），则本 buff 不再有意义，自卸载避免永久驻留。
				if (target instanceof Hero){
					if (Admiration.findAdmirationInInventory((Hero) target) == null){
						detach();
						return true;
					}
				}
				spend(TICK);
				return true;
			}
			if (target instanceof Hero){
				Admiration adm = Admiration.findAdmirationInInventory((Hero) target);
				if (adm != null){
					// 1. 充能 ↔ 溶解之爱血量 百分比 同步（含反噬为敌对的情况）
					int hpPct = Math.round(100f * love.HP / Math.max(1, love.HT));
					if (hpPct != adm.charge){
						adm.charge = hpPct;
						adm.refreshUI();
					}
					// 2. 存活计时 → 神器升级（与原 AdmirationBuff 内逻辑一致，独立于神器是否装备）
					//    修复（2026-09-06）：boss 战锁层（背水一战/LockedFloor）期间不累计升级进度，
					//    与原版"boss 战禁用被动收益"语义一致；锁层解除后继续累计。
					if (((Hero) target).buff(LockedFloor.class) == null){
						adm.survivalTurns++;
						int need = 150 + 50 * adm.level();
						if (adm.survivalTurns >= need && adm.level() < adm.levelCap){
							adm.survivalTurns = 0;
							adm.upgrade();
							love.refreshForLevel( adm.level() );
							GLog.p("爱慕因溶解之爱的相伴而愈发深沉。");
						}
					}
				}
			}
			spend(TICK);
			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.NONE;
		}
	}

	//========== 反噬倒计时（10 回合；重新装备或溶解之爱消失则取消） ==========
	//注意：必须为 static——该 buff 随英雄存档，读档时经反射无参构造重建；非静态内部类没有无参构造器会崩（2026-09-06 修复）。
	//本类不引用外层 Admiration 实例，可安全 static。

	public static class BetrayalTimer extends Buff {

		{
			type = buffType.NEGATIVE;
		}

		private int left = 10;

		@Override
		public boolean act() {
			if (!(target instanceof Hero)){
				detach();
				return true;
			}
			Hero hero = (Hero) target;

			MeltingLove love = MeltingLove.findLoyalAlly();
			if (love == null){
				//溶解之爱已不在了，倒计时失去意义
				detach();
				return true;
			}
			//重新装备爱慕（第一或第二神器栏均可）→ 取消反噬
			if (hero.belongings.artifact instanceof Admiration
					|| hero.belongings.misc instanceof Admiration){
				detach();
				return true;
			}

			left--;
			if (left <= 0){
				love.betray(hero);
				GLog.n("溶解之爱反噬了你的背叛！");
				detach();
				return true;
			}

			//倒计时期间维持"焦虑"表情（可重复调用；读档后下一回合也会自动接上，
			//移动中则只置标志，移动结束由 Sprite.idle() 接上，不会打断跑动动画）
			love.beginAnxious();

			spend(TICK);
			return true;
		}

		@Override
		public void detach() {
			//倒计时因任何原因结束（重新装备 / 溶解之爱消失 / 已反噬）都收掉"焦虑"表情。
			//注意：已反噬时精灵正在播黑化两帧，Sprite.clearAnxious() 不会打断它。
			MeltingLove love = MeltingLove.findLoyalAlly();
			if (love != null) love.endAnxious();
			super.detach();
		}

		@Override
		public String name() {
			return "爱慕的反噬倒计时";
		}

		@Override
		public String desc() {
			return "若在 " + left + " 回合内没有重新装备「爱慕」，溶解之爱将反噬你。";
		}

		@Override
		public int icon() {
			return BuffIndicator.TIME;
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(left);
		}

		private static final String LEFT = "left";

		@Override
		public void storeInBundle(Bundle bundle) {
			super.storeInBundle(bundle);
			bundle.put(LEFT, left);
		}

		@Override
		public void restoreFromBundle(Bundle bundle) {
			super.restoreFromBundle(bundle);
			left = bundle.getInt(LEFT);
		}
	}

}
