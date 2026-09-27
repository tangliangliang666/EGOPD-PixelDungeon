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

package com.shatteredpixel.shatteredpixeldungeon.actors.buffs;

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.RancorParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.RevengeLedger;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 「背叛家人者」的分支特色（2026-09-16）：<b>背弃家族的人不靠忠义，靠把旧账烧成力量</b>。
 *
 * <h3>规则</h3>
 * <p>只要<b>没有</b>处于「仇怨」状态，在用<b>莱瓦汀系列</b>武器（封印之剑 / 一阶段解封之剑 /
 * 二阶段解封之剑 / 莱瓦汀）攻击<b>之前</b>，会自动消耗 <b>{@value #CHARGE_COST}%</b> 的
 * {@link RevengeLedger 复仇账簿}充能，换来一次<b>「刚好挥得动」</b>的力量补足——
 * 加成刚好把力量补到武器需求线（不多给一点），且<b>只维持这一次攻击</b>。</p>
 *
 * <h3>三个必须写清楚的边界</h3>
 * <ul>
 *   <li><b>只在真的不够力量时才收费</b>：{@code STRReq() > hero.STR()} 才走这条路。
 *       力量本来就够，一分充能不花、也不会挂上这个 buff。</li>
 *   <li><b>「仇怨」期间完全不触发</b>：那边已经给了 +{@link RevengeLedger#RANCOR_STR} 点力量，
 *       本来就挥得动，再收一笔钱等于双重付费（这也是需求里「如果没有进入仇怨状态」那句话的意思）。</li>
 *   <li><b>付不起就什么都不发生</b>：充能不足 {@value #CHARGE_COST}% 时
 *       {@link RevengeLedger#consumeCharge} 返回 0，本 buff 不挂、力量不加——
 *       攻击照常打出去，只是仍按「力量不足」结算（精准惩罚 / 延迟惩罚照旧）。</li>
 * </ul>
 *
 * <h3>为什么做成 buff</h3>
 * <p>力量在全工程只有一个汇聚点：{@code Hero.STR()}（见 {@code TempStrength} / 号指 /
 * 「仇怨」都从那里加算）。要让加成影响<b>本击的命中判定与 damageRoll</b>，就必须在
 * 「命中判定之前」把它挂上去、在「damageRoll 之后」摘下来——一个瞬时 buff 正是这件事的自然载体。</p>
 *
 * <p><b>生命周期</b>：施加在 {@code Talent.onHeroAttackStarted}（{@code Char.attack} 里、
 * 命中判定与 {@code damageRoll} 之前），解除在 {@code Hero.spend(float)} —— 也就是<b>本击的回合
 * 成本已经结算完</b>的那一刻。中途有两条保险：{@link #onAttackStarted} 开头先摘掉上一击的残留；
 * {@value #DURATION} 回合的时长兜底自灭。</p>
 *
 * <p><b>为什么不能提前到 {@code Talent.onHeroAttackResolved} 摘</b>（2026-09-24 修的 bug）：
 * 那个钩子跑在 {@code Char.attack} 的尾巴上，而<b>攻击延迟要等它之后</b>才由
 * {@code Hero.onAttackComplete → spend(attackDelay())} 算出来 —— {@code Weapon.baseDelay()}
 * 读的正是 {@code Hero.STR()}。在那里摘掉，本击的精准惩罚（÷1.5ⁿ）确实没了，
 * 但 {@code ×1.2ⁿ} 的延迟惩罚会原样留下。把消费点挪到 {@code Hero.spend} 之后，
 * 本击的力量补足对<b>精准 / 伤害 / 攻击延迟 / 偷袭资格</b>四处同时生效，且仍然只活一次攻击。</p>
 *
 * <p>{@code Hero.spend(float)} 是英雄<b>所有</b>回合成本的唯一出口：平砍走
 * {@code Hero.onAttackComplete}，连击 / 武技 / buff 驱动的一击也都在各自结尾调
 * {@code spendAndNext(hero.attackDelay())}，所以这一个点就覆盖全部攻击路径；
 * 延迟为 0 的一击（手起刀落）走 {@code next()}，它内部同样落到这里。</p>
 *
 * <p><b>顶层类</b>：随存档序列化并通过反射重建，不能做成非静态内部类。</p>
 */
public class FamilyBetrayal extends FlavourBuff {

	/** 每次「预支力量」消耗的复仇账簿充能（百分点，100 点 = 基础满格）。 */
	public static final int CHARGE_COST = 5;

	/** 兜底时长（回合）：正常情况下攻击结算完就手动解除，这个值只是保险丝。 */
	public static final float DURATION = 1f;

	{
		type = buffType.POSITIVE;
	}

	/** 本次补足的力量点数。 */
	private int amount = 0;

	public int amount(){
		return amount;
	}

	/** 覆盖式刷新剩余时长（不叠加）。写法照 {@link TempStrength#setTurns(float)}。 */
	public void setTurns( float turns ){
		spend( turns - cooldown() );
	}

	//==========================================================================
	// 攻击前 / 攻击后（唯一入口，调用点见类注释）
	//==========================================================================

	/**
	 * 攻击<b>开始前</b>：不够力量就自动预支一笔账簿充能，换一次力量补足。
	 *
	 * <p>调用点在 {@code Char.attack} 的命中判定与 {@code damageRoll} 之前
	 * （{@code Talent.onHeroAttackStarted}），所以这次补足同时作用于<b>本击的精准</b>
	 * （{@code Weapon.accuracyFactor} 读 {@code STRReq() - STR()}）与<b>本击的伤害</b>；
	 * 它一直活到本击的回合成本结算完（{@link #consumeAfterAttack}），所以
	 * <b>攻击延迟</b>（{@code Weapon.baseDelay}）与<b>偷袭资格</b>
	 * （{@code Hero.canSurpriseAttack()}）这两处力量惩罚也一并免掉。</p>
	 *
	 * <p><b>无敌目标要提前退出</b>：{@code Char.attack} 在 {@code enemy.isInvulnerable(...)}
	 * 分支里直接 {@code return false}，整场结算都不会发生。若在那里付了钱，玩家就会白白损失
	 * {@value #CHARGE_COST}% 充能却什么都没做——所以「这一击根本打不出去」时一分不花
	 * （判据与 {@code Char.attack} 首段完全一致）。</p>
	 */
	public static void onAttackStarted( Hero hero, Char enemy ){
		if (hero == null || hero.subClass != HeroSubClass.FAMILY_BETRAYER) return;

		//这一击根本不会发生（目标无敌）：不收费
		if (enemy == null || enemy.isInvulnerable( hero.getClass() )) return;

		//手上（含技能临时换上来的）不是莱瓦汀系列就不管
		if (!(hero.belongings.attackingWeapon() instanceof SealedSwordBase)) return;

		//已经燃着「仇怨」：力量本来就够，不收费也不再补
		if (RevengeLedger.rancorActive( hero )) return;

		//先摘掉上一击的残留（正常已由 Hero.spend 收走，这里只兜「收了 buff 却一次都没 spend」的早退路径）：
		//残留会垫着力量让下面的 need 恒为 0 ⇒ 变成「付一次钱、永久挥得动」
		consumeAfterAttack( hero );

		SealedSwordBase sword = (SealedSwordBase) hero.belongings.attackingWeapon();
		int need = sword.STRReq() - hero.STR();
		if (need <= 0) return; //力量已够：一分不花

		//整笔扣款，付不起就整件事作罢（不会部分扣款，也不给残缺的加成）
		if (RevengeLedger.consumeCharge( hero, CHARGE_COST ) <= 0) return;

		apply( hero, need );
	}

	/**
	 * 本击的回合成本结算完 ⇒ 这一次攻击的力量补足到此为止
	 * （命中、落空、目标中途死亡三条路都会走到）。
	 *
	 * <p><b>消费点＝{@code Hero.spend(float)}</b>（在 {@code super.spend(time)} 之后调用），
	 * <b>不是</b> {@code Talent.onHeroAttackResolved}：后者跑在 {@code Char.attack} 的尾巴上，
	 * 而攻击延迟要等它之后才由 {@code Hero.onAttackComplete → spend(attackDelay())} 算出来
	 * （{@code Weapon.baseDelay} 读的是 {@code Hero.STR()}）。在那里摘，
	 * 攻击延迟的 {@code ×1.2ⁿ} 惩罚就会漏下来。详见类注释的「生命周期」一节。</p>
	 *
	 * <p>必须每击都摘掉，否则它会在下一次「不需要补足」的攻击里继续生效——
	 * 那就不再是「一次性」，而变成常驻力量了。{@code Hero.spend} 是英雄所有回合成本的唯一出口，
	 * 所以平砍、连击、武技、buff 驱动的一击全都覆盖得到；没挂 buff 时本方法只是空操作，
	 * 放在这条热路径上没有副作用。</p>
	 */
	public static void consumeAfterAttack( Hero hero ){
		if (hero == null) return;
		FamilyBetrayal buff = hero.buff( FamilyBetrayal.class );
		if (buff != null) buff.detach();
	}

	//==========================================================================
	// 施加 / 取值
	//==========================================================================

	/** 给 {@code ch} 挂上（或刷新）力量补足。 */
	public static void apply( Char ch, int amount ){
		if (ch == null || amount <= 0) return;

		FamilyBetrayal buff = Buff.affect( ch, FamilyBetrayal.class );
		//Buff.append 会吞掉 attachTo 的失败结果，可能拿到 target == null 的幽灵对象
		if (buff == null || buff.target == null) return;

		//取较大值：同一回合内若已经挂着（多段攻击的极端情形），不要把加成越刷越小
		buff.amount = Math.max( buff.amount, amount );
		buff.setTurns( DURATION );

		//视觉：从角色身上炸开一簇紫光（与「仇怨」同源的粒子），
		//提示这一次挥剑是拿账簿里的旧账换来的
		if (ch.sprite != null){
			ch.sprite.emitter().burst( RancorParticle.RAGE, 4 );
		}
		BuffIndicator.refreshHero();
	}

	/** 当前的力量补足点数（没有该 buff 时为 0），供 {@code Hero.STR()} 取用。 */
	public static int amountOf( Char ch ){
		FamilyBetrayal buff = (ch == null) ? null : ch.buff( FamilyBetrayal.class );
		return (buff != null) ? buff.amount : 0;
	}

	//==========================================================================
	// 显示 / 存档
	//==========================================================================

	@Override
	public int icon(){
		//暂无自绘图标 ⇒ 借原版「增强」帧 50（BuffIndicator.UPGRADE）。
		//按 BuffIndicator 里的【惯例】条目，未自绘的 buff 不预留帧号常量。
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ){
		//暗红着色：与同帧的临时力量（暖橙）、肾上腺素等区分开
		icon.hardlight( 0.85f, 0.22f, 0.28f );
	}

	@Override
	public String desc(){
		//FlavourBuff.dispTurns() 返回 String ⇒ 模板里用 %s
		return Messages.get( this, "desc", amount, dispTurns() );
	}

	private static final String AMOUNT = "amount";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( AMOUNT, amount );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		amount = bundle.getInt( AMOUNT );
	}
}
