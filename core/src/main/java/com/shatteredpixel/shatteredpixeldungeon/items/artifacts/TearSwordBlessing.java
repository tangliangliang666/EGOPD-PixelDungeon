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
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearShield;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.TearSword;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.effects.TearSwordVisual;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.rings.RingOfEnergy;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 「泪锋的加护」——自定义神器（2026-09-12）。
 *
 * <p>围绕泪剑的两形态神器：<b>消耗充能唤出泪剑</b>（{@link TearSword}），泪剑按当前形态呈现完全不同的
 * 环绕特效与数值，两种形态可在装备菜单里随时切换。</p>
 *
 * <h3>充能与升级（同「神偷袖章」{@code MasterThievesArmband}）</h3>
 * <ul>
 *   <li><b>获得经验充能</b>：{@link #onHeroGainExp}（{@code Hero.earnExp} 会对每个随身物品回调）——
 *       一个「英雄等级」的经验换 3 点充能，再乘 {@code RingOfEnergy.artifactChargeMultiplier}；
 *       充能上限 {@code 5 + level()/2}，随升级提高；</li>
 *   <li><b>「神器充能」buff 充能</b>：{@link #charge(Hero, float)}——秘能卷轴 / 冥想等给的
 *       {@code ArtifactRecharge} 按 {@link #CHARGE_PER_RECHARGE_TURN}（0.1）折算，
 *       一次满额（30 回合）的 buff ≈ 3 点，与「一个英雄等级的经验」等价（<b>对齐经验路径</b>）；</li>
 *   <li><b>使用升级</b>：每次「唤出泪剑」给神器攒 {@link #EXP_PER_USE} 点经验，攒够
 *       {@code 10 + round(3.33 × 当前等级)} 就升一级（上限 {@link #LEVEL_CAP}）。</li>
 * </ul>
 *
 * <h3>两种形态（设泪剑层数 x、神器等级 L）</h3>
 * <ul>
 *   <li><b>加护</b>（{@link TearSwordVisual.Form#BLESSING}，默认）：剑身竖直向下绕角色旋转。
 *       每 {@code 3/x} 回合积攒 {@code L+2} 点护盾（上限 {@code 3(L+2) = 3L+6}，{@link TearShield}）——
 *       结算在 {@code TearSword.act()} 里做，本神器只负责形态同步；
 *       并让护甲的<b>最低防御</b>提高 {@code x + L}（消费点在 {@code Hero.drRoll}）；</li>
 *   <li><b>绝望</b>（{@link TearSwordVisual.Form#DESPAIR}）：泪剑沿脚底上半圆排布并随近战协同突刺，
 *       伤害 {@code x × (L + 2)}；与此同时武器攻击力的<b>上限</b>降低 {@code 3x + L}（下限不变，
 *       消费点在 {@code KindOfWeapon.damageRoll}）。</li>
 * </ul>
 * <p><b>减层计数（两形态共用一份）</b>：加护形态<b>挨打</b>、绝望形态协同<b>造成伤害</b>各计 1 次，
 * 满 {@link TearSword#DECAY_PER_STACK} 次减 1 层泪剑——所以「加护挨 2 下 → 切绝望打 1 下」一样会掉 1 层。
 * 计数的实际存放与推进都在 {@link TearSword} 上（它才是被消耗的对象）。</p>
 *
 * <h3>诅咒惩罚</h3>
 * <p>被诅咒时神器完全不可用（{@link #actions} 不提供唤剑/切形态），并且令护甲的<b>最高防御减半</b>
 * （{@code TearSword.hasCursedArmorPenalty}，消费点在 {@code Hero.drRoll}）——
 * 详见 {@link TearSword} 类注释的「诅咒惩罚」一节。</p>
 *
 * <h3>非专属神器：可出售 / 可嬗变 / 可被偷</h3>
 * <p>本神器<b>不是专属神器</b>，因此<b>不设 {@code unique}</b>，完全按原版普通物品的待遇：
 * 可卖给商店（{@link #sellable()}）、可被嬗变卷轴重掷（{@link #transmutable()}）、
 * 也会被盗贼 / 水晶拟态视作普通战利品——两者都额外要求 {@code level() < 1}，
 * 所以只有<b>尚未升过级</b>的崭新泪锋才有被偷 / 被吃的风险，升过一级就安全了。</p>
 *
 * <h3>贴图</h3>
 * <p>加护形态 {@link ItemSpriteSheet#ARTIFACT_TEAR_BLESSING} xy(7,39)、
 * 绝望形态 {@link ItemSpriteSheet#ARTIFACT_TEAR_DESPAIR} xy(8,39)，见 {@link #updateImage()}。</p>
 *
 * <h3>形态的存放位置</h3>
 * <p>形态写在<b>本神器</b>上（{@link #STORE_FORM}）而不是只写在 {@link TearSword} 上：泪剑层数归零时
 * buff 会 detach、其上的形态随之丢失，而神器会一直在背包/装备槽里；每次被动回合神器把自己的形态
 * 推给泪剑（{@link TearSword#setForm}），召唤时再随层数一起带入。</p>
 *
 * <h3>调试</h3>
 * <p>桌面/安卓调试窗（F2）「神器」页浅层扫描 {@code items.artifacts} 包，本类编译后自动出现，
 * 点击即发放（附赠若干充能便于试玩，见 {@link #DEBUG_CHARGE}）。</p>
 */
public class TearSwordBlessing extends Artifact {

	/** 唤出一柄泪剑。 */
	public static final String AC_SUMMON = "SUMMON";

	/** 切换形态（加护 ⇄ 绝望）。 */
	public static final String AC_SWITCH = "SWITCH";

	/** 每唤出一柄泪剑消耗的充能。 */
	public static final int CHARGE_PER_SWORD = 1;

	/** 每次「使用」（唤剑）给神器攒的经验；量级对齐神偷袖章的「行窃一次 +3/+4」。 */
	public static final int EXP_PER_USE = 3;

	/** 神器等级上限（同神偷袖章）。 */
	public static final int LEVEL_CAP = 10;

	/** 一个「英雄等级」的经验换多少充能（与神偷袖章一致）。 */
	public static final float CHARGE_PER_HERO_LEVEL = 3f;

	/**
	 * 「神器充能」buff（{@code ArtifactRecharge}）每 1 点 {@code amount} 折算多少充能——<b>对齐经验路径</b>。
	 *
	 * <p>一次「英雄等级」的经验给 {@link #CHARGE_PER_HERO_LEVEL}（3）点；而该 buff 满额 30 回合
	 * （{@code amount} 累计 30）——取 {@code 3 / 30 = 0.1}，两者便严格等价：<b>一次满额的充能 buff
	 * ＝ 一个英雄等级的经验收益 ≈ 本神器 5~10 点能量条的半条到一条</b>。</p>
	 *
	 * <p>系数与神偷袖章 {@code MasterThievesArmband} 完全一致（它的经验路径同为「3 点 / 英雄等级」、
	 * {@code chargeCap} 同为 {@code 5 + level/2}，{@code charge()} 里也是 {@code 0.1f * amount}）。</p>
	 */
	public static final float CHARGE_PER_RECHARGE_TURN = 0.1f;

	/** 调试窗发放时附赠的充能（0 = 从零开始，与神偷袖章一致）。 */
	private static final int START_CHARGE = 0;

	/** 当前形态。加护是神器的本来面貌，故为默认值。 */
	private TearSwordVisual.Form form = TearSwordVisual.Form.BLESSING;

	{
		image = ItemSpriteSheet.ARTIFACT_TEAR_BLESSING;

		levelCap = LEVEL_CAP;

		charge = START_CHARGE;
		partialCharge = 0;
		chargeCap = 5 + level() / 2;

		//刻意不设 unique：本神器是普通（非专属）神器，按原版普通物品待遇——
		//可售、可嬗变，也会被盗贼 / 水晶拟态当作普通战利品（见类注释「非专属神器」一节）。
		bones = false;

		defaultAction = AC_SUMMON;
	}

	//==========================================================================
	// 非专属待遇：出售 / 嬗变
	//==========================================================================

	/**
	 * 泪锋的加护<b>不是专属神器</b>（它可正常出现在神器生成池中），因此允许在商店出售。
	 *
	 * <p>本类已<b>不再设 {@code unique}</b>，所以父类的默认实现（{@code !unique || stackable}）
	 * 本来就会返回 true；这里显式覆写只是把「可售」这一语义钉在类上——
	 * 万一日后有人为别的理由给它补上 {@code unique}，也不会顺带把商店这条口子关上。</p>
	 *
	 * <p>注意 {@code Shopkeeper.canSell} 的其余通则照旧：{@code value() <= 0}、已封印护甲、
	 * <b>已装备且被诅咒</b>都会被挡；本神器若在被诅咒状态下装备着，仍须先解咒卸下。</p>
	 */
	@Override
	public boolean sellable() {
		return true;
	}

	/**
	 * 允许被嬗变卷轴选为嬗变对象（＝原版对非专属神器的待遇）。
	 *
	 * <p>同理，本类不设 {@code unique}，父类默认实现即返回 true；显式覆写用于固定语义，
	 * 也让「爱慕 / 泪锋这类自定义神器可嬗变」这件事在代码里一眼可见。</p>
	 *
	 * <p>被嬗变掉不会留下残骸：形态与持剑状态虽然写在神器上，但装备栏里的神器一旦被替换就会走
	 * {@code doUnequip} → {@link BlessingBuff#detach()}，把泪剑层数与泪盾一并收走；若是在背包里
	 * 被嬗变，则本来就没有本神器的 buff 挂在身上。</p>
	 */
	@Override
	public boolean transmutable() {
		return true;
	}

	//==========================================================================
	// 动作
	//==========================================================================

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );

		//被诅咒时不提供任何操作（同神偷袖章：诅咒状态的充能类神器一律不可用）
		if (isEquipped( hero ) && !cursed) {
			//充能不足 / 已是满柄时不显示唤剑，免得点开才发现做不了
			if (charge >= CHARGE_PER_SWORD && TearSword.stacks( hero ) < TearSword.MAX_STACKS) {
				actions.add( AC_SUMMON );
			}
			actions.add( AC_SWITCH );
		}
		return actions;
	}

	@Override
	public String actionName( String action, Hero hero ) {
		if (action.equals( AC_SUMMON )) {
			return Messages.get( this, "ac_summon" );
		}
		if (action.equals( AC_SWITCH )) {
			return Messages.get( this, "ac_switch" );
		}
		return super.actionName( action, hero );
	}

	@Override
	public void execute( Hero hero, String action ) {

		super.execute( hero, action );

		if (action.equals( AC_SUMMON )) {
			summon( hero );
		} else if (action.equals( AC_SWITCH )) {
			switchForm( hero );
		}
	}

	/**
	 * 唤出泪剑：消耗 {@link #CHARGE_PER_SWORD} 点充能 +1 层，并给神器攒 {@link #EXP_PER_USE} 点经验
	 * （攒够即升级，见类注释「充能与升级」）。占 1 回合行动。
	 */
	private void summon( Hero hero ) {

		if (!isEquipped( hero )) {
			GLog.i( Messages.get( Artifact.class, "need_to_equip" ) );
			return;
		}
		if (cursed) {
			GLog.w( Messages.get( this, "cursed" ) );
			return;
		}
		if (charge < CHARGE_PER_SWORD) {
			GLog.i( Messages.get( this, "no_charge" ) );
			return;
		}
		if (TearSword.stacks( hero ) >= TearSword.MAX_STACKS) {
			GLog.i( Messages.get( this, "full_swords", TearSword.MAX_STACKS ) );
			return;
		}

		charge -= CHARGE_PER_SWORD;
		TearSword.addStacks( hero, 1, form );
		gainArtifactExp( EXP_PER_USE );

		Talent.onArtifactUsed( hero );
		Item.updateQuickslot();

		GLog.i( Messages.get( this, "summon", TearSword.stacks( hero ) ) );
		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		Sample.INSTANCE.play( Assets.Sounds.MELD );

		hero.spendAndNext( 1f ); //唤剑占 1 回合
	}

	/** 切换形态：贴图 + 把新形态推给已在场的泪剑（会播放「旋转 + 移位」动画，见 {@link TearSwordVisual#setForm}）。 */
	private void switchForm( Hero hero ) {

		if (!isEquipped( hero )) {
			GLog.i( Messages.get( Artifact.class, "need_to_equip" ) );
			return;
		}
		if (cursed) {
			GLog.i( Messages.get( this, "cursed" ) );
			return;
		}

		form = (form == TearSwordVisual.Form.BLESSING)
				? TearSwordVisual.Form.DESPAIR
				: TearSwordVisual.Form.BLESSING;

		TearSword.setForm( hero, form );

		//切换形态即清空泪盾：加护攒的盾不会跨到绝望形态（也不会在切回加护后复现）。
		//这里再显式清一次，是为了覆盖「泪剑已耗尽（TearSword buff 已 detach）、但盾还留着」时切形态的情况
		//——那条路径上 setForm 找不到 buff、无法代为清盾（见 TearSword.setForm 的注释）。
		TearShield.clear( hero );

		updateImage();
		Item.updateQuickslot();

		GLog.i( Messages.get( this, form == TearSwordVisual.Form.BLESSING
				? "switch_blessing" : "switch_despair" ) );
		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		Sample.INSTANCE.play( Assets.Sounds.MELD );
	}

	/** 按当前形态切换贴图（加护 xy(7,39) / 绝望 xy(8,39)）。 */
	private void updateImage() {
		image = (form == TearSwordVisual.Form.BLESSING)
				? ItemSpriteSheet.ARTIFACT_TEAR_BLESSING
				: ItemSpriteSheet.ARTIFACT_TEAR_DESPAIR;
	}

	//==========================================================================
	// 充能与升级
	//==========================================================================

	/** 升到下一级所需的经验（同神偷袖章）。 */
	private int expForNextLevel() {
		return 10 + Math.round( 3.33f * level() );
	}

	/** 攒经验，攒够就连续升级（与神偷袖章同一套阈值/提示/图鉴计数）。 */
	private void gainArtifactExp( int amount ) {
		exp += amount;

		while (exp >= expForNextLevel() && level() < levelCap) {
			exp -= expForNextLevel();
			Catalog.countUse( TearSwordBlessing.class );
			GLog.p( Messages.get( this, "level_up" ) );
			upgrade();
		}
		updateQuickslot();
	}

	/**
	 * 获得经验 → 充能（{@code Hero.earnExp} 会遍历随身物品回调本方法，见 {@code Hero.java}）。
	 *
	 * <p>只在装备中且未被诅咒时生效；与神偷袖章一样按 {@code levelPercent = 经验 / 英雄升级所需经验}
	 * 折算，所以「杀怪升级」才是主要充能来源，捡经验药水（{@code PotionOfExperience}）不算——
	 * 那条路径上 {@code Hero.earnExp} 根本不遍历物品。</p>
	 */
	@Override
	public void onHeroGainExp( float levelPercent, Hero hero ) {
		if (cursed || !isEquipped( hero )) return;
		accumulateCharge( CHARGE_PER_HERO_LEVEL * levelPercent
				* RingOfEnergy.artifactChargeMultiplier( hero ) );
	}

	/**
	 * 「神器充能」buff（{@code ArtifactRecharge}）→ 充能。
	 *
	 * <p>秘能卷轴 / 冥想 / 魔杖天赋等给英雄挂上该 buff 后，它每回合遍历英雄身上的
	 * {@link ArtifactBuff} 回调本方法（{@code amount} 单位 ≈「1 点/回合」，满额 30 点）。
	 * <b>神器不覆写本方法就完全吃不到这条充能</b>（父类 {@code Artifact.charge} 是空实现）。</p>
	 *
	 * <p>系数取 {@link #CHARGE_PER_RECHARGE_TURN}（{@code 0.1}）——<b>对齐经验路径</b>：
	 * 一次满额的充能 buff ≈ 3 点充能 ≈ 一个英雄等级的经验收益，与本神器
	 * {@code chargeCap = 5 + level/2}（5~10）的刻度相称。</p>
	 *
	 * <p>与神偷袖章一致，这条路径<b>不乘</b> {@code RingOfEnergy} 倍率——该倍率只进各神器
	 * 自身的经验 / 被动充能（原版惯例）。</p>
	 */
	@Override
	public void charge( Hero target, float amount ) {
		if (cursed || target.buff( MagicImmune.class ) != null) return;
		accumulateCharge( CHARGE_PER_RECHARGE_TURN * amount );
	}

	/**
	 * 把折算好的充能值累加进 {@link #partialCharge}，满格即封顶并提示。
	 *
	 * <p>经验路径（{@link #onHeroGainExp}）与「神器充能」buff（{@link #charge(Hero, float)}）
	 * 共用本方法，保证两条来源的累积 / 封顶 / 提示行为完全一致。</p>
	 */
	private void accumulateCharge( float gain ) {
		if (charge >= chargeCap) {
			partialCharge = 0f;
			return;
		}

		partialCharge += gain;

		while (partialCharge >= 1f) {
			partialCharge -= 1f;
			charge++;

			if (charge >= chargeCap) {
				charge = chargeCap;
				partialCharge = 0f;
				GLog.p( Messages.get( this, "full" ) );
				break;
			}
		}
		updateQuickslot();
	}

	@Override
	public Item upgrade() {
		//充能上限随等级提高（与神偷袖章同式），要在 super 之前按「升级后」的等级算
		chargeCap = 5 + (level() + 1) / 2;
		return super.upgrade();
	}

	//==========================================================================
	// 装备期间
	//==========================================================================

	@Override
	protected ArtifactBuff passiveBuff() {
		return new BlessingBuff();
	}

	@Override
	public void activate( Char ch ) {
		super.activate( ch );
		//重新装备（含读档后 Belongings 的 activate 链）时，把神器的形态带回泪剑，不必等下一个被动回合
		if (ch instanceof Hero) {
			TearSword.setForm( ch, form );
		}
	}

	/**
	 * 装备期间：把神器上存的形态同步给泪剑。
	 *
	 * <p>注意本类是<b>非静态内部类</b>：它不参与存档重建（{@code Bundle} 会跳过非静态内部类，
	 * 见 {@code watabou.utils.Bundle.get()}），而是在 {@link Artifact#activate(Char)} 重新装备/读档时
	 * 由 {@link #passiveBuff()} 现造一个——这一点与 {@code Admiration.AdmirationBuff} 一致。
	 * 因此「泪剑与护盾随卸下而收走」也写在 {@link #detach()}</p>。
	 *
	 * <p><b>加护的护盾不在这里结算</b>：它归 {@code TearSword.act()} 管（本类只保证形态同步）。
	 * 理由见 {@link TearSword#act()}——泪剑 buff 只要还在就必然每回合 act，把盾挂在它身上比挂在
	 * 神器被动 buff 上少一层依赖，「剑在转、盾不涨」这类问题从结构上就不会发生。</p>
	 */
	public class BlessingBuff extends ArtifactBuff {

		@Override
		public boolean act() {
			if (target instanceof Hero) {
				//形态可能在上次 act 之后被改过（切换动作、读档），每回合兜底同步一次
				TearSword.setForm( (Hero) target, form );
			}
			spend( TICK );
			return true;
		}

		@Override
		public void detach() {
			//泪剑与护盾都由本神器提供：神器失效（卸下/被移除/复活重置）即一并收回。
			//只在还持有它们时才动手，避免和别人的泪剑互相打架（TearSword 本身是通用 buff）。
			if (target != null) {
				if (TearSword.stacks( target ) > 0) {
					TearSword.setStacks( target, 0 );
				}
				TearShield.clear( target );
			}
			super.detach();
		}
	}

	//==========================================================================
	// 文案
	//==========================================================================

	/** 装备时仅在「被诅咒」时补一句提示（等级 / 充能 / 泪剑层数在游戏 UI 里已直接可见，不再重复）。 */
	@Override
	public String desc() {
		String desc = super.desc();

		if (cursed && Dungeon.hero != null && isEquipped( Dungeon.hero )) {
			desc += "\n\n" + Messages.get( this, "desc_cursed" );
		}
		return desc;
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String STORE_FORM = "form";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		//0/1 而不是枚举序号：缺键时 Bundle 返 0，正好落回默认的「加护」
		bundle.put( STORE_FORM, form == TearSwordVisual.Form.DESPAIR ? 1 : 0 );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		//旧档无此键 → 0 → 加护形态
		form = (bundle.getInt( STORE_FORM ) == 1)
				? TearSwordVisual.Form.DESPAIR
				: TearSwordVisual.Form.BLESSING;
		updateImage();
	}
}
