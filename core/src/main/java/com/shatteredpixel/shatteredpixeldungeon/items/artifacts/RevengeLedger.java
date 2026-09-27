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
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.Buff;
import com.shatteredpixel.shatteredpixeldungeon.actors.buffs.MagicImmune;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger.MiddleFingerVoice;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RancorParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.Stylus;
import com.shatteredpixel.shatteredpixeldungeon.items.potions.elixirs.ElixirOfMight;
import com.shatteredpixel.shatteredpixeldungeon.journal.Catalog;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.sprites.ItemSpriteSheet;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.shatteredpixel.shatteredpixeldungeon.utils.GLog;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * 神器——复仇账簿（自定义，2026-09-13）。
 *
 * <h3>等级与充能上限（2026-09-15 起）</h3>
 * <p>账簿等级 = <b>英雄等级 ÷ 2</b>（{@link #levelForHero}，向下取整），最高 {@link #LEVEL_CAP} 级
 * ——英雄 20 级时账簿满级。每 1 级让充能上限 <b>+10%</b>：基础 {@link #CHARGE_CAP}（100%），
 * 满级 {@link #CHARGE_CAP_MAX}（200%）。上限存在继承来的 {@code chargeCap} 字段里，由
 * {@link #syncChargeCap} 在装备时与每回合被动 buff 里刷新，所以<b>升级后上限自动长大，不需要额外存档</b>。</p>
 *
 * <h3>充能（百分比点数制，0 ~ 上限）</h3>
 * <p>下面说的「充能 N%」都是<b>点数</b>：100 点 = 基础满格，升级后可以攒到 200 点。
 * 状态条恒按点数显示百分比（见 {@link #status()}），所以高等级下会看到超过 100% 的数字。</p>
 * <ul>
 *   <li><b>损失生命</b>：按「损失的生命值 ÷ 生命上限」的百分比<b>等比例</b>充能
 *       （{@link #DAMAGE_CHARGE_SCALE}，钩在 {@code Hero.damage} 里，只算真正掉掉的血，
 *       被护盾吃下的部分不算）；</li>
 *   <li><b>击杀怪物</b>：按「怪物生命上限 ÷ 生命上限」的百分比的<b>十分之一</b>充能
 *       （{@link #KILL_CHARGE_SCALE}，钩在 {@code Mob.die} 的英雄击杀分支）。</li>
 * </ul>
 * <p>两种充能都要求神器<b>已装备</b>且未被诅咒。</p>
 *
 * <h3>使用效果：「仇怨」</h3>
 * <p><b>使用有门槛与代价</b>：充能不足 {@link #RANCOR_ACTIVATION_COST}%（20%）时无法燃起仇怨
 * （动作不会出现在菜单里，硬调 {@code execute} 也会被挡下）；够门槛则<b>立刻扣掉 20% 充能</b>，
 * 再获得 {@link Rancor} buff。</p>
 * <p>buff 期间角色<b>力量 +12</b>（{@link #RANCOR_STR}，消费点在 {@code Hero.STR()}），
 * 并且<b>每回合再消耗 2% 神器充能</b>（{@link #RANCOR_DRAIN}）；充能归零时 buff 自动解除。
 * <b>初始消耗与每回合消耗各自独立结算</b>——前者是一次性的入场费，后者照旧逐回合扣，
 * 互不冲抵、也不因为扣了入场费而改变每回合的速率。</p>
 * <p>再次点击快捷栏/菜单可以主动解除（解除<b>不</b>再收费，剩余充能原样保留）。</p>
 * <p>视觉上，持有期间会从角色身上<b>向四周持续散发紫色粒子</b>
 * （{@link RancorParticle}，间隔 {@link #RANCOR_PARTICLE_INTERVAL}），发射器跟随角色贴图，
 * 由 {@link Rancor#fx(boolean)} / {@code Rancor.act()} 负责建与重建。</p>
 *
 * <h3>使用效果：「刻入纹身」（能力由天赋授予）</h3>
 * <p>中指长兄持有 T2 天赋「纹身铭刻」时，菜单里会多出 {@link #AC_TATTOO}：
 * 消耗一支 {@link Stylus 奥术刻笔}，直接换到 {@link Talent#tattooChargePercent}%
 * （+1 = 25%，+2 = 50%）的充能。动作本身占 1 回合。</p>
 *
 * <h3>其他充能来源</h3>
 * <p>中指长兄 T2「记录一餐」每次进食额外给 8%/15% 充能
 * （由 {@code Talent.onFoodEaten} 调 {@link #chargeByTalent}）。</p>
 *
 * <h3>贴图 / 图标</h3>
 * <p>神器贴图 {@link ItemSpriteSheet#ARTIFACT_REVENGE_LEDGER}（xy(1,41)，13×16）；
 * buff 图标 {@link BuffIndicator#RANCOR}（帧 113，紧接「泪剑」）。</p>
 *
 * <h3>调试</h3>
 * <p>桌面/安卓调试窗（F2）「神器」页会浅层扫描 {@code items.artifacts} 包，本类编译后自动出现。</p>
 */
public class RevengeLedger extends Artifact {

	/** 使用「仇怨」的动作。 */
	public static final String AC_RANCOR = "RANCOR";

	/** 「刻入纹身」的动作（中指长兄 T2「纹身铭刻」）：消耗一支奥术刻笔换充能。 */
	public static final String AC_TATTOO = "TATTOO";

	/** 充能上限基数（百分比点数）：账簿 0 级时为 100%。 */
	public static final int CHARGE_CAP = 100;

	/** 每 1 级账簿等级增加的充能上限（%）。 */
	public static final int CHARGE_CAP_PER_LEVEL = 10;

	/** 账簿等级上限：英雄 20 级时达到（20 ÷ 2）。 */
	public static final int LEVEL_CAP = 10;

	/** 满级充能上限（200%）。 */
	public static final int CHARGE_CAP_MAX = CHARGE_CAP + CHARGE_CAP_PER_LEVEL * LEVEL_CAP;

	/** 受伤充能系数：损失生命占生命上限的百分比 × 本系数 = 获得的充能百分点（等比例即 1.0）。 */
	public static final float DAMAGE_CHARGE_SCALE = 1f;

	/** 击杀充能系数：怪物生命上限占英雄生命上限的百分比 × 本系数（十分之一）。 */
	public static final float KILL_CHARGE_SCALE = 0.1f;

	/** 「仇怨」期间的力量加成。 */
	public static final int RANCOR_STR = 12;

	/** 「仇怨」每回合消耗的神器充能百分点。 */
	public static final int RANCOR_DRAIN = 2;

	/**
	 * 「燃起仇怨」的<b>门槛兼初始消耗</b>（充能百分点）。
	 *
	 * <p>两件事共用一个常量，因为它们是同一条规则的两面：<b>充能不足本值不能使用</b>
	 * （{@link #actions()} 不列出动作、{@link #execute} 也会挡下），<b>使用成功立刻扣掉本值</b>。
	 * 若两处各写一个数字，日后改门槛时极容易只改一处，出现「门槛 30 却只扣 20」这类隐性白嫖。</p>
	 *
	 * <p><b>与 {@link #RANCOR_DRAIN} 完全独立</b>：这里是一次性的入场费（在
	 * {@link #startRancor} 里扣），那边是 buff 存在期间每回合的持续消耗（在
	 * {@link Rancor#act()} 里扣），二者各自结算、互不冲抵。</p>
	 *
	 * <p>取 20% 的意图：在 100% 上限下最多燃起 5 次、每次再被 2%/回合的持续性消耗慢慢烧掉，
	 * 于是「攒仇怨 → 换一段爆发」成为有取舍的循环，而不是有充能就无限开着。</p>
	 *
	 * <p><b>边界情形</b>：充能恰好 20% 时可以使用，但扣完即为 0，仇怨会在下一次回合结算时
	 * （{@code act()} 里先扣 2% 再判归零）立刻熄灭——花钱只买到不到一回合。这是「门槛用尽」的
	 * 自然结果，符合「充能越多越划算」的直觉；若想避免，把门槛改成「高于 20%」即可。</p>
	 */
	public static final int RANCOR_ACTIVATION_COST = 20;

	/**
	 * 「仇怨」期间紫色粒子的发射间隔（秒/颗）。越小越浓；0.1 约合每秒 10 颗、
	 * 同屏常驻 6~8 颗（寿命 0.7 秒）。
	 */
	public static final float RANCOR_PARTICLE_INTERVAL = 0.1f;

	{
		image = ItemSpriteSheet.ARTIFACT_REVENGE_LEDGER;

		//先放到最大值：读档时 chargeCap 字段还没按英雄等级刷新过，若在这里写 100 会让
		//Artifact.restoreFromBundle 里的 Math.min(chargeCap, charge) 把超过 100 的充能裁掉。
		//真正的上限由 syncChargeCap() 在装备时/每回合刷新。
		chargeCap = CHARGE_CAP_MAX;
		charge = 0;
		partialCharge = 0;

		unique = true;
		bones = false;

		defaultAction = AC_RANCOR;
	}

	/**
	 * 「满充能台词」是否已经说过（2026-09-18）。只在充能掉下上限时复位，
	 * 于是「攒满 → 说一句 → 花掉 → 再攒满」每循环各说一次（判定见 {@link #ledgerRecharge}）。
	 */
	private boolean maxVoiceAnnounced;

	//==========================================================================
	// 充能（受伤 / 击杀）
	//==========================================================================

	/** 取出英雄身上装备着的复仇账簿（神器槽或杂项槽），未装备时返回 null。 */
	public static RevengeLedger find(Hero hero) {
		if (hero == null) return null;
		Item art = hero.belongings.artifact;
		if (!(art instanceof RevengeLedger)) art = hero.belongings.misc;
		return (art instanceof RevengeLedger) ? (RevengeLedger) art : null;
	}

	/**
	 * 英雄损失生命时充能（由 {@code Hero.damage} 在伤害结算后回调）。
	 *
	 * @param hpLost 本次<b>实际损失</b>的生命值（被护盾吃下的部分不计入）。
	 */
	public static void onHeroDamaged(Hero hero, int hpLost) {
		if (hero == null || hpLost <= 0 || hero.HT <= 0) return;
		RevengeLedger ledger = find(hero);
		if (ledger == null || ledger.cursed) return;
		ledger.gainCharge(CHARGE_CAP * DAMAGE_CHARGE_SCALE * hpLost / (float) hero.HT);
	}

	/**
	 * 英雄击杀怪物时充能（由 {@code Mob.die} 的英雄击杀分支回调）。
	 *
	 * @param mob 被击杀的怪物；生命上限必须在它 {@code super.die()} 之前读，故本方法在
	 *            {@code rollToDropLoot()} 之后、{@code super.die()} 之前调用。
	 */
	public static void onEnemyKilled(Hero hero, Mob mob) {
		if (hero == null || mob == null || hero.HT <= 0) return;
		RevengeLedger ledger = find(hero);
		if (ledger == null || ledger.cursed) return;
		ledger.gainCharge(CHARGE_CAP * KILL_CHARGE_SCALE * mob.HT / (float) hero.HT);
	}

	/** 累加充能（单位：百分点，可带小数，跨回合累积到 1 点才进位）。 */
	private void gainCharge(float percent) {
		//英雄可能刚刚升级（上限变大），充能前先对齐一次上限，避免被过小的旧上限提前截断
		syncChargeCap();
		if (percent <= 0f || charge >= chargeCap) return;

		partialCharge += percent;
		while (partialCharge >= 1f && charge < chargeCap) {
			partialCharge -= 1f;
			charge++;
		}

		if (charge >= chargeCap) {
			charge = chargeCap;
			partialCharge = 0f;
			GLog.p(Messages.get(this, "full"));
		}
		updateQuickslot();
	}

	/** 通用的「神器充能」入口（{@code ArtifactRecharge} 等外部充能来源会调到这里）。 */
	@Override
	public void charge(Hero target, float amount) {
		if (cursed || target.buff(MagicImmune.class) != null) return;
		gainCharge(amount);
	}

	/**
	 * 天赋直接给定额充能（「记录一餐」+8%/15%、「纹身铭刻」+25%/50%）。
	 * 未装备或已诅咒时静默跳过——与 {@link #onHeroDamaged} 一致，不给空提示。
	 */
	public static void chargeByTalent(Hero hero, float percent) {
		if (hero == null || percent <= 0f) return;
		RevengeLedger ledger = find(hero);
		if (ledger == null) return;
		ledger.charge(hero, percent);
	}

	/**
	 * 直接扣除定额充能（2026-09-16 新增，供「背叛家人者」的攻击前代价使用）。
	 *
	 * <p>是 {@link #chargeByTalent} 的反向操作——那边只加不减。两处的门槛口径一致：
	 * 神器必须<b>已装备且未被诅咒</b>，否则本方法返回 0、调用方按「付不起」处理。</p>
	 *
	 * <p><b>不部分扣款</b>：充能不足 {@code percent} 时原样返回 0，一点都不扣。
	 * 代价必须整笔付清，否则「只剩 1% 也能换来一次力量补足」会让这个机制在低充能时白送。</p>
	 *
	 * @param percent 想扣掉的百分点
	 * @return 实际扣掉的百分点（付不起时为 0）
	 */
	public static int consumeCharge(Hero hero, int percent) {
		if (hero == null || percent <= 0) return 0;
		RevengeLedger ledger = find(hero);
		if (ledger == null || ledger.cursed) return 0;
		if (ledger.charge < percent) return 0;

		ledger.charge -= percent;
		ledger.updateQuickslot();
		BuffIndicator.refreshHero();
		return percent;
	}

	/** 英雄此刻是否正燃着「仇怨」（{@link Rancor}）。 */
	public static boolean rancorActive(Hero hero) {
		return hero != null && hero.buff(Rancor.class) != null;
	}

	//==========================================================================
	// 等级 / 充能上限
	//==========================================================================

	/** 英雄等级对应的账簿等级：每 2 级英雄等级折 1 级，最高 {@link #LEVEL_CAP} 级。 */
	public static int levelForHero(Hero hero) {
		if (hero == null) return 0;
		return Math.min(LEVEL_CAP, hero.lvl / 2);
	}

	/** 指定账簿等级下的充能上限（%）。 */
	public static int chargeCapFor(int level) {
		return Math.min(CHARGE_CAP_MAX, CHARGE_CAP + CHARGE_CAP_PER_LEVEL * Math.max(0, level));
	}

	/** 按当前英雄等级刷新充能上限（英雄还没建好时按 0 级算）。 */
	private void syncChargeCap() {
		chargeCap = chargeCapFor(levelForHero(Dungeon.hero));
	}

	/** 本账簿当前等级（= 英雄等级 ÷ 2，封顶 {@link #LEVEL_CAP}）。 */
	public int ledgerLevel() {
		return levelForHero(Dungeon.hero);
	}

	/**
	 * 当前充能点数（0 ~ {@code chargeCap}）。100 点 = 基础满格，满级上限 200 点。
	 * <p>莱瓦汀系列的形态门槛读的就是这个值（见 {@code SealedSwordBase.canReachStage}）。</p>
	 */
	public int chargePoints() {
		return charge;
	}

	/**
	 * 状态条：恒按<b>充能点数</b>显示百分比（0%~200%）。
	 *
	 * <p>父类 {@code Artifact.status()} 只在上限恰好 100 时才用 {@code %d%%}，上限一旦随等级
	 * 涨到 150/200 就会退化成 {@code 87/150} 这种分数式；而本神器的形态门槛、天赋加成全部按点数
	 * 说话，所以这里统一成百分号显示，免得战场上出现两套刻度。</p>
	 */
	@Override
	public String status() {
		//未鉴定 / 被诅咒时不显示任何东西（与父类一致）
		if (!isIdentified() || cursed) return null;
		if (cooldown != 0) return Messages.format("%d", cooldown);
		return Messages.format("%d%%", charge);
	}

	/** 在神器描述末尾补一行「账簿等级 N/10：充能上限 M%」。 */
	@Override
	public String info() {
		String info = super.info();
		if (isIdentified() && !cursed) {
			info += "\n\n" + Messages.get(this, "level_info", ledgerLevel(), chargeCap);
		}
		return info;
	}

	//==========================================================================
	// 动作
	//==========================================================================

	@Override
	public ArrayList<String> actions(Hero hero) {
		ArrayList<String> actions = super.actions(hero);
		if (isEquipped(hero)
				&& !cursed
				&& hero.buff(MagicImmune.class) == null) {
			//未激活时要求充能达到「燃起」门槛，不够就不列出动作（与父类神器「没充能就不给用」一致）；
			//已激活时总是列出，方便主动解除
			if (charge >= RANCOR_ACTIVATION_COST || activeBuff != null) {
				actions.add(AC_RANCOR);
			}
			//中指长兄 T2「纹身铭刻」：有天赋就列出动作；手上没有刻笔时由 execute 提示
			if (hero.heroClass == HeroClass.MIDDLE_FINGER
					&& hero.hasTalent(Talent.TATTOO_ENGRAVING)) {
				actions.add(AC_TATTOO);
			}
		}
		return actions;
	}

	@Override
	public String actionName(String action, Hero hero) {
		if (action.equals(AC_RANCOR)) {
			return Messages.get(this, "ac_rancor");
		} else if (action.equals(AC_TATTOO)) {
			return Messages.get(this, "ac_tattoo");
		}
		return super.actionName(action, hero);
	}

	@Override
	public void execute(Hero hero, String action) {

		super.execute(hero, action);

		if (!action.equals(AC_RANCOR) && !action.equals(AC_TATTOO)) return;
		if (hero.buff(MagicImmune.class) != null) return;

		if (action.equals(AC_TATTOO)) {
			engrave(hero);
			return;
		}

		if (activeBuff != null) {
			//已激活：再次点击＝主动解除（无额外代价，剩余充能原样保留）
			activeBuff.detach();
			hero.sprite.operate(hero.pos);
			return;
		}

		if (!isEquipped(hero))       GLog.i(Messages.get(Artifact.class, "need_to_equip"));
		else if (cursed)             GLog.i(Messages.get(this, "cursed"));
		else if (charge < RANCOR_ACTIVATION_COST)
			                         GLog.i(Messages.get(this, "no_charge"));
		else {
			Sample.INSTANCE.play(Assets.Sounds.MELD);
			startRancor(hero);
			if (hero.sprite != null) hero.sprite.operate(hero.pos);
			hero.spendAndNext(1f); //使用神器占 1 回合
		}
	}

	/**
	 * 「刻入纹身」（中指长兄 T2「纹身铭刻」）：消耗一支奥术刻笔，直接换到
	 * {@link Talent#tattooChargePercent}% 的充能。
	 *
	 * <p>刻笔可能躺在卷轴袋里（{@code ScrollHolder} 接受刻笔），所以先用
	 * {@code belongings.getItem} 找、再交给 {@code detach(backpack)} ——
	 * {@code Item.detachAll} 会递归进嵌套包。</p>
	 */
	private void engrave(Hero hero) {
		if (!isEquipped(hero)) {
			GLog.i(Messages.get(Artifact.class, "need_to_equip"));
			return;
		}
		if (cursed) {
			GLog.i(Messages.get(this, "cursed"));
			return;
		}

		Stylus stylus = hero.belongings.getItem(Stylus.class);
		if (stylus == null) {
			GLog.w(Messages.get(this, "no_stylus"));
			return;
		}

		Sample.INSTANCE.play(Assets.Sounds.BURNING);
		if (hero.sprite != null) hero.sprite.operate(hero.pos);

		stylus.detach(hero.belongings.backpack);
		Catalog.countUse(Stylus.class);

		int before = charge;
		int percent = Talent.tattooChargePercent(hero);
		gainCharge(percent); //直接充，诅咒 / 魔法免疫在上面已经判定过
		GLog.i(Messages.get(this, "tattoo_used", percent, Math.max(0, charge - before)));

		//中指长兄 T2「纹身铭刻」增强：消耗刻笔还会额外交出「生命强化」＝当前生命上限的 25%/50%。
		//复用 ElixirOfMight.HTBoost（同名的既有「生命强化」）：升级 5 次后自然消失；
		//重复铭刻走覆盖式刷新，不叠加（数值先按当前 HT 算好再写入，避免自我滚雪球）。
		int htPercent = Talent.tattooHealthBoostPercent(hero);
		if (htPercent > 0){
			int htGain = Math.round(hero.HT * (htPercent / 100f));
			ElixirOfMight.HTBoost boost = Buff.affect(hero, ElixirOfMight.HTBoost.class);
			boost.setExplicitBonus(htGain);
			hero.updateHT(true); //与艾利克斯同款：上限提升的同时补足等量生命
			if (hero.sprite != null){
				hero.sprite.showStatusWithIcon(CharSprite.POSITIVE, Integer.toString(htGain), FloatingText.HEALING);
			}
			GLog.p(Messages.get(this, "tattoo_ht", htPercent, htGain));
		}

		hero.spendAndNext(1f); //刻入纹身占 1 回合
	}

	/**
	 * 激活「仇怨」：先扣一次性入场费 {@link #RANCOR_ACTIVATION_COST}%（门槛已在
	 * {@link #execute} 判过），之后由 buff 每回合再扣 {@link #RANCOR_DRAIN}%
	 * （充能归零即自行解除）。
	 */
	private void startRancor(Hero hero) {
		//入场费：一次性扣掉，与 Rancor.act() 里的每回合消耗各自结算（互不冲抵）
		charge = Math.max(0, charge - RANCOR_ACTIVATION_COST);

		activeBuff = activeBuff();
		activeBuff.attachTo(hero);

		Talent.onArtifactUsed(hero);
		updateQuickslot();
		BuffIndicator.refreshHero();

		GLog.i(Messages.get(this, "on_rancor"));
	}

	//==========================================================================
	// 装备期间
	//==========================================================================

	@Override
	public void activate(Char ch) {
		super.activate(ch);
		//装备/读档完成：按英雄等级定下充能上限
		syncChargeCap();
		//读档重建后 activeBuff 已从 Bundle 恢复但还没挂上，这里补挂
		if (activeBuff != null && activeBuff.target == null) {
			activeBuff.attachTo(ch);
		}
	}

	@Override
	public boolean doUnequip(Hero hero, boolean collect, boolean single) {
		if (super.doUnequip(hero, collect, single)) {
			//卸下即收回「仇怨」
			if (activeBuff != null) {
				activeBuff.detach();
			}
			return true;
		}
		return false;
	}

	@Override
	protected void onDetach() {
		if (passiveBuff != null) {
			passiveBuff.detach();
			passiveBuff = null;
		}
		if (activeBuff != null && activeBuff.target instanceof Hero
				&& !isEquipped((Hero) activeBuff.target)) {
			activeBuff.detach();
		}
	}

	@Override
	protected ArtifactBuff passiveBuff() {
		return new ledgerRecharge();
	}

	@Override
	protected ArtifactBuff activeBuff() {
		return new Rancor();
	}

	@Override
	public Item upgrade() {
		//本神器不升级（levelCap 保持 0），保留覆写以便日后扩展
		return super.upgrade();
	}

	@Override
	public int value() {
		return 0;
	}

	//==========================================================================
	// buff 实现
	//==========================================================================

	/**
	 * 常驻被动 buff：本神器的充能来自「受伤/击杀」事件而非回合，故这里什么都不做，
	 * 只负责让 {@link Artifact#activate(Char)} 有东西可挂、并让 {@code ArtifactRecharge}
	 * 之类的通用充能来源能找到本神器（{@link Artifact#charge(Hero, float)}）。
	 */
	public class ledgerRecharge extends ArtifactBuff {
		@Override
		public boolean act() {
			//每回合兜底对齐一次充能上限：英雄升级后上限变大，这里会自动跟上
			syncChargeCap();

			//中指长兄的台词「哈啊……看来没有打开账簿的必要了。」：
			//在「充能已满」的回合开始播一遍（攒满一次只说一次；一旦掉下上限就重新武装，
			//下次再攒满还会说）。判据放在回合开始而不是 charge 到顶的那一刻，
			//是为了让语音与「回合」对齐——伤血冲顶往往发生在敌人的回合里，
			//那时出声会和挨打的音效糊在一起。
			if (charge >= chargeCap) {
				if (!maxVoiceAnnounced) {
					maxVoiceAnnounced = true;
					MiddleFingerVoice.playLedgerFull();
				}
			} else {
				maxVoiceAnnounced = false;
			}

			spend(TICK);
			return true;
		}
	}

	/**
	 * 「仇怨」（复仇账簿激活时的效果 buff）。
	 *
	 * <p>力量 +{@link #RANCOR_STR}（消费点在 {@code Hero.STR()}）；每回合消耗
	 * {@link #RANCOR_DRAIN}% 的神器充能，充能归零时自动解除。
	 * <b>入场时已经扣过一次性的 {@link #RANCOR_ACTIVATION_COST}%</b>
	 * （见 {@link RevengeLedger#startRancor}），那笔与这里的每回合消耗各自独立结算。</p>
	 *
	 * <p>本类是<b>非静态内部类</b>（同奥丁之眼的 {@code precognition}）：它不参与存档重建
	 * （{@code Bundle} 会跳过非静态内部类），而是由 {@link #storeInBundle}/{@link #restoreFromBundle}
	 * 手动存取。放在内部是为了能直接读写神器的 {@code charge}。</p>
	 */
	public class Rancor extends ArtifactBuff {

		{
			type = buffType.POSITIVE;
		}

		/** 「仇怨」期间从角色向四周散发紫色粒子的发射器（不存档；换层后由 {@link #ensureAura()} 重建）。 */
		private transient Emitter aura;

		@Override
		public int icon() {
			return BuffIndicator.RANCOR;
		}

		//--------------------------------------------------------------------------
		// 视觉：紫色粒子
		//--------------------------------------------------------------------------

		/**
		 * 建/重建紫色粒子发射器。
		 *
		 * <p>{@code CharSprite.emitter()} 从 {@code GameScene.emitters} 组里领一个发射器并把目标设成
		 * 角色贴图（于是粒子跟着英雄走）；本方法只在<b>还没有可用发射器</b>时才领新的，
		 * 判断依据是 {@code exists}——换层/切场景时旧发射器随场景一起销毁（{@code exists} 变 false），
		 * 而英雄贴图重建时 {@code CharSprite.link → Char.updateSpriteState()} 会把所有 buff 的
		 * {@code fx(true)} 再跑一遍，正好走到这里重建。</p>
		 *
		 * <p>场景尚未就绪（{@code GameScene.emitter()} 返回 null）时静默跳过，
		 * 留待下一回合 {@link #act()} 再试。</p>
		 */
		private void ensureAura() {
			if (target == null || target.sprite == null) return;
			if (aura != null && aura.exists) return;

			aura = target.sprite.emitter();
			if (aura != null) {
				aura.pour( RancorParticle.RAGE, RANCOR_PARTICLE_INTERVAL );
			}
		}

		/**
		 * 停止发射：只把 {@code on} 置 false（不再产生新粒子），已经在飞的粒子自然消亡后
		 * 发射器会因 {@code autoKill} 自毁——直接 kill 会把满屏粒子一下抹掉，很难看。
		 */
		private void freeAura() {
			if (aura != null) {
				aura.on = false;
				aura = null;
			}
		}

		@Override
		public void fx(boolean on) {
			if (on) {
				ensureAura();
			} else {
				freeAura();
			}
		}

		@Override
		public void tintIcon(Image icon) {
			icon.brightness(0.75f);
		}

		/** 图标灰罩高度＝已失去的充能比例（充能越少、上限越高，灰得越多）。 */
		@Override
		public float iconFadePercent() {
			return (chargeCap - charge) / (float) chargeCap;
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", charge, RANCOR_STR);
		}

		@Override
		public boolean act() {
			//每回合兜底重建一次（换层/切场景后旧的已随场景销毁）
			ensureAura();

			//每回合消耗 2% 充能，归零即自动解除
			charge = Math.max(0, charge - RANCOR_DRAIN);
			updateQuickslot();

			if (charge <= 0) {
				if (target == Dungeon.hero) {
					BuffIndicator.refreshHero();
					GLog.w(Messages.get(RevengeLedger.this, "rancor_ends"));
				}
				detach();
			} else if (target == Dungeon.hero) {
				BuffIndicator.refreshHero();
			}

			spend(TICK);
			return true;
		}

		@Override
		public void detach() {
			activeBuff = null;
			//target.sprite 为 null 时 Buff.detach() 不会回调 fx(false)，这里补一刀停掉发射
			freeAura();
			updateQuickslot();
			//target 可能为 null（读档期间还没挂上就已被卸下），此时只清引用、不碰 Buff 链表
			if (target == null) return;
			if (target == Dungeon.hero) BuffIndicator.refreshHero();
			super.detach();
		}
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String BUFF = "buff";

	/** 「满充能台词是否已说过」的存档键。 */
	private static final String MAX_VOICE = "maxVoiceAnnounced";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		if (activeBuff != null) bundle.put(BUFF, activeBuff);
		bundle.put(MAX_VOICE, maxVoiceAnnounced);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		//先把上限放到最大值，避免父类里的 Math.min(chargeCap, charge) 把 >100 的充能裁掉；
		//上限随后由 syncChargeCap() 修正（英雄可能还没重建好，那就不动，等 activate 再对齐）
		chargeCap = CHARGE_CAP_MAX;
		super.restoreFromBundle(bundle);
		if (Dungeon.hero != null) syncChargeCap();
		//旧存档没有这个键 ⇒ 读成 false（Bundle.getBoolean 缺失即 0）⇒ 满充能时会补说一次，可接受
		maxVoiceAnnounced = bundle.getBoolean(MAX_VOICE);
		if (bundle.contains(BUFF)) {
			activeBuff = new Rancor();
			activeBuff.restoreFromBundle(bundle.getBundle(BUFF));
		}
	}
}
