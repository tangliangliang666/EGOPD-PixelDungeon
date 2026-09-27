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

import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Belongings;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.effects.TearSwordVisual;
import com.shatteredpixel.shatteredpixeldungeon.items.artifacts.TearSwordBlessing;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 泪剑 —— 可叠层 buff（帧 {@link BuffIndicator#TEAR_SWORD} = 112，紧接「攻击害虫」111）：
 * 以环绕特写为主，附近战协同伤害；两形态的数值见下。
 *
 * <p><b>特效 + 协同伤害</b>：持有 n 层时，在角色脚底为圆心、半径 16 像素的
 * <b>上半圆</b>上均分排列 n 柄泪剑（{@link TearSwordVisual}）。每柄剑各自小幅慢速<b>浮动</b>，
 * 跟随移动时带一点<b>惯性</b>（落下一小段再追上），身后拖出深蓝色尾迹，并随角色朝向翻转；
 * 角色<b>近战挥击</b>时全部剑会依次向目标<b>协同突刺</b>一次，并追加法术伤害
 * （{@code 层数 × (L + }{@link #DESPAIR_DAMAGE_BONUS}{@code )}，L = 神器等级，见 {@link #strikeAt}）。</p>
 *
 * <h3>形态数值（设层数 x、神器等级 L）</h3>
 * <ul>
 *   <li><b>加护</b>：护甲的<b>最低防御</b>提高 {@code x + L}（{@link #armorMinDrBonus}），
 *       并由本 buff 每回合推进 {@code x / }{@link #SHIELD_PERIOD}{@code } 的进度、满 1 就积攒
 *       {@code L + }{@link #SHIELD_AMOUNT_BONUS}{@code } 点护盾
 *       （上限 {@code 3(L+2) = 3L+6}，见 {@link TearShield}）；</li>
 *   <li><b>绝望</b>：武器攻击力的<b>上限</b>降低 {@code 3x + L}（下限不变，{@link #weaponMaxPenalty}），
 *       协同突刺伤害 {@code x × (L + 2)}。</li>
 * </ul>
 * <p>各形态的数值都以「当前形态 + 当前层数」实时计算，切换形态立即生效——所以这里只对外暴露
 * 静态查询方法，不在本类里缓存任何结果。</p>
 *
 * <h3>诅咒惩罚（{@link #hasCursedArmorPenalty}）</h3>
 * <p>神器「泪锋的加护」<b>被诅咒</b>时：既不能唤剑也不能切换形态（{@code TearSwordBlessing.actions}
 * 干脆不提供这两个动作），并且令护甲的<b>最高防御减半</b>——消费点在 {@code Hero.drRoll()}：
 * 先把护甲掷值区间的上端砍半，再把下端夹回新上端之内，因此与加护形态的「抬下限」互不冲突。
 * 这条惩罚与形态、泪剑层数都无关：只要神器还装备在身上且被诅咒就生效。</p>
 *
 * <h3>减层计数（{@link #DECAY_PER_STACK}）</h3>
 * <p>两形态<b>共用同一个计数</b>：加护形态<b>挨打</b>一次 +1（{@link #onHitTaken}），
 * 绝望形态协同突刺<b>造成伤害</b>一次 +1（{@link #strikeAt}）。累计满 3 次就减 1 层泪剑、计数归零；
 * 因为计数只有一份，「加护挨 2 下 → 切绝望打 1 下」同样会掉 1 层（层数归零时 buff 一并 detach，
 * 计数随之消失——没有剑也就无所谓计数了）。</p>
 * <p>计数就存在本 buff 上而不是神器上：泪剑才是被消耗的对象，神器只是提供者。</p>
 *
 * <p><b>两种形态（{@link TearSwordVisual.Form}）</b>：本 buff 同时也是神器「泪锋的加护」的
 * 泪剑载体，按 {@link #form} 决定剑的排布，切换走 {@link #setForm(Char, TearSwordVisual.Form)}：</p>
 * <ul>
 *   <li><b>{@link TearSwordVisual.Form#BLESSING 加护}</b>：剑身竖直向下，沿以角色躯干中心为圆心、
 *       半径 11 像素的整圆均匀分布并整体绕角色<b>旋转</b>（{@link TearSwordVisual} 内实现）；</li>
 *   <li><b>{@link TearSwordVisual.Form#DESPAIR 绝望}</b>（本 buff 单独使用时的默认值）：即上面描述的上半圆弧，
 *       且只有该形态会响应 {@link #strikeAt(Char, Char)} 的协同突刺（加护形态「守而不攻」，见该方法）。</li>
 * </ul>
 * <p>形态是<b>视觉层</b>的属性，但存在本 buff 上（{@link #STORE_FORM}）——buff 层数归零会 detach，
 * 届时形态也就没了，所以神器「泪锋的加护」自己另存一份形态，并在每次 {@code addStacks} 时带入
 * （{@link #setStacks(Char, int, TearSwordVisual.Form)}）。</p>
 *
 * <p><b>切换形态 = 清空泪盾</b>：形态一变（{@link #setForm(Char, TearSwordVisual.Form)} 或
 * {@link #setStacks(Char, int, TearSwordVisual.Form)} 带入的新形态），身上所有
 * {@link TearShield 泪盾}<b>立即清空</b>——加护攒的盾不跨形态延续，切回加护也要从零再攒。
 * 清盾在 {@link #applyForm} 里统一做（带 {@code stacks > 0} 守卫：buff 刚重建、0 层时带入形态
 * 不算玩家切换，免得把「泪剑耗尽后重新唤剑」时保留下来的盾误清）；
 * 「泪剑已耗尽但有盾」这种 buff 缺席的情况由玩家入口 {@code TearSwordBlessing.switchForm} 直接清。</p>
 *
 * <p><b>层数上限 {@link #MAX_STACKS} = 3</b>：剑沿上半圆均分，再多就会挤成一团；
 * 要放开只改这一个常量即可（{@code addStacks} 与提示文案都自动跟随）。</p>
 *
 * <p><b>无自然时限</b>：只要层数 ≥ 1 就一直保持（{@code act()} 每回合 spend(TICK) 并返回 true），
 * 没有倒计时；层数只会被「减层计数」消耗掉（见上），归零即自动 detach。</p>
 *
 * <p><b>入口约定</b>：请用 {@link #addStacks(Char, int)} / {@link #setStacks(Char, int)} 增删层数
 * （{@code setStacks(ch, 0)} 等价于移除）。直接 {@code Buff.affect(ch, TearSword.class)} 得到的是
 * <b>0 层</b>，会在首个 {@code act()} 回合自行 detach；调试窗「状态」页已接好叠层入口。</p>
 *
 * <p><b>协同攻击触发点</b>：{@code Hero.onAttackComplete()}（近战挥击结算处，
 * 与「巴勒莫对剑」叠层的钩子同一位置）会调用 {@link #strikeAt(Char, Char)}，且必须放在其内部
 * {@code attack()} 调用的<b>之后</b>（理由见 {@code strikeAt} 的注释）。
 * 只挂在英雄的<b>近战</b>路径上——{@code Hero.actAttack} 里被「逻辑工作室」接管的远程射击、
 * 以及 {@code Hero.shoot()} 的投掷，都不会触发。若日后要让怪物持有时也生效，
 * 在 {@code Mob.onAttackComplete()} 里加同样一行即可（那边目标字段叫 {@code enemy}）。</p>
 *
 * <p><b>「法术伤害」的登记</b>：伤害来源是 {@link MagicStrike} 这个<b>非 Buff 的空标记类</b>
 * （{@code target.damage(dmg, new MagicStrike())}），而 {@code AntiMagic.RESISTS} 里登记的是
 * <b>它</b>、不是 {@code TearSword} 这个 Buff 本身。这样做同时买到三件事：
 * ① {@code Char.damage} 里那条 {@code isImmune(srcClass) ⇒ damage = 0} 的判定 ⇒ 带
 * {@code MagicImmune}（反魔法卷轴）或「魔法免疫」精英特性（{@code ChampionEnemy.AntiMagic}，
 * 后者也是把 RESISTS 灌进 {@code immunities}）的目标<b>完全免伤</b>；
 * ② {@code AntiMagic} 雕文的 {@code drRoll} 会对其减伤（普通目标 {@code glyphLevel = -1} ⇒ 不减）；
 * ③ 伤害浮字自动显示法术图标（{@code Char.damage} 的图标分支按 RESISTS 判定，无需另加 {@code instanceof}）。</p>
 *
 * <p>而「泪剑本身不被魔法免疫驱散」正是靠<b>来源与 buff 分离</b>实现的：{@code MagicImmune.attachTo()}
 * 是用 {@code b.getClass().isAssignableFrom(immunity)} 遍历目标身上的 buff 并按类清除的，
 * 只有登记 {@code TearSword.class} 才会把它一起清掉；登记标记类后这条路径匹配不到泪剑，
 * 持有者吃反魔法卷轴<b>只会免疫泪剑伤害，泪剑照旧环绕</b>，免疫期间也能正常叠层
 * （{@code Buff.attachTo} 的 isImmune 检查同样只看 {@code TearSword.class}，它已不在 RESISTS 里）。
 * 细节与「若日后想改回去要同步改哪里」见 {@link MagicStrike}。</p>
 *
 * <p><b>独立顶层类</b>（非内部类），保证存档经 Bundle 反射可正常重建。视觉对象不参与存档（{@code visual}
 * 是普通字段，不写入 Bundle）——它挂在角色贴图所在图层（{@code target.sprite.parent}，即 GameScene 的
 * mobs 组），换层/切场景后旧视觉随场景一同销毁，由 {@code act()} 里的 {@code ensureVisual()} 重建。
 * 视觉内部还会按朝向把剑分到两层：背侧剑在角色之上、朝向同侧剑沉到 {@code GameScene.floorEmitters}
 * （角色之下）以形成环绕遮挡，详见 {@link TearSwordVisual} 的类注释。</p>
 */
public class TearSword extends Buff {

	/** 层数上限（同时决定环绕剑的柄数上限）。3 柄已能沿上半圆均匀铺开。 */
	public static final int MAX_STACKS = 3;

	/**
	 * 绝望形态协同伤害的固定加成：伤害 = 层数 × (神器等级 + 本值)。
	 * 即基础每柄 2 点、神器升一级每柄 +1。
	 */
	public static final int DESPAIR_DAMAGE_BONUS = 2;

	/**
	 * 减层阈值：加护形态挨打 / 绝望形态协同造成伤害，两形态<b>共用</b>这一个计数，
	 * 累计满本值就减 1 层泪剑（计数清零，见类注释「减层计数」）。
	 */
	public static final int DECAY_PER_STACK = 3;

	/**
	 * 加护形态护盾结算周期：x 柄剑每回合推进 {@code x / 本值} 的进度，满 1 结算一次
	 * 「{@link #SHIELD_AMOUNT_BONUS 见下的} L+2 点护盾」。
	 * 即 3 柄剑每回合一次、1 柄剑每 3 回合一次。<b>剑越多攒得越快</b>。
	 */
	public static final float SHIELD_PERIOD = 3f;

	/**
	 * 加护形态<b>单次结算</b>的护盾量固定加成：单次 = 神器等级 + 本值（{@code L+2}）。
	 * 神器未升级（L=0）时单次数值由它兜底，不会出现「一次结算 0 点」的空转。
	 */
	public static final int SHIELD_AMOUNT_BONUS = 2;

	/**
	 * 加护形态的护盾上限 = 本值 × 单次结算量 = {@code 3(L+2) = 3L+6}，
	 * 也就是「最多攒三次的量」；攒满后再积攒的份额白白浪费（见 {@link TearShield}）。
	 */
	public static final int SHIELD_CAP_TICKS = 3;

	/**
	 * 泪剑协同伤害的<b>来源标记类</b>（空类，与 {@code DimDusk.MagicStrike} 同一范式）。
	 *
	 * <p>它唯一的职责是给 {@code Char.damage} 提供一个可分类的 {@code src}，从而把两件事拆开：</p>
	 * <ul>
	 *   <li><b>免伤照旧</b>：{@code AntiMagic.RESISTS} 里登记的是这个标记类，而 {@code Char.damage}
	 *       用 {@code src.getClass()} 判定，伤害依旧会被魔法免疫完全抵挡、被魔法抵抗雕文减伤、
	 *       并显示法术图标；</li>
	 *   <li><b>不再牵连泪剑本身</b>：它不是 {@code Buff}，所以 {@code MagicImmune.attachTo()} 里那条
	 *       「遍历目标身上的 buff 并按类驱散」的逻辑匹配不到它，持有者获得魔法免疫时
	 *       <b>泪剑不会被驱散</b>，免疫期间也能正常叠层（{@code Buff.attachTo} 的 isImmune 检查
	 *       同样只看 {@code TearSword.class}，而它已不在 RESISTS 里）。</li>
	 * </ul>
	 *
	 * <p>若日后改为登记 {@code TearSword.class}（例如想让它被反魔法卷轴清掉），记得同步
	 * {@code Mob.die} 的英雄击杀白名单——那里的判断也必须跟着在两者之间切换。</p>
	 */
	public static class MagicStrike {}

	{
		type = buffType.POSITIVE;
	}

	private int stacks = 0;

	/**
	 * 减层计数（两形态共用，见类注释「减层计数」）：加护形态挨打、绝望形态协同造成伤害各 +1，
	 * 满 {@link #DECAY_PER_STACK} 次减 1 层并清零。
	 */
	private int decayCount = 0;

	/**
	 * 加护形态的护盾蓄积进度（回合）：每回合推进 {@code 层数 / SHIELD_PERIOD}，满 1 结算一次。
	 *
	 * <p>放在本 buff 上而不是神器上：结算就在 {@link #act()} 里做（见下），而本 buff 只要还有剑
	 * 就必然每回合 act——这是「泪剑在、护盾就必须在长」的唯一可靠保证。</p>
	 */
	private float shieldProgress = 0f;

	/**
	 * 泪剑的环绕形态（见类注释「两种形态」）。单独使用本 buff（如调试窗叠层）时默认为
	 * {@link TearSwordVisual.Form#DESPAIR 绝望}，即原有的上半圆弧 + 协同突刺表现。
	 */
	private TearSwordVisual.Form form = TearSwordVisual.Form.DESPAIR;

	/** 环绕视觉（挂在角色贴图层 mobs，不参与存档）。 */
	private TearSwordVisual visual;

	//==========================================================================
	// 图标与文案
	//==========================================================================

	@Override
	public int icon() {
		return BuffIndicator.TEAR_SWORD;
	}

	//大图标右下角显示当前层数
	@Override
	public String iconTextDisplay() {
		return Integer.toString( stacks );
	}

	@Override
	public String desc() {
		//实参保留 stacks / MAX_STACKS 备用：文案里想重新显示层数时直接写 %1$d/%2$d 即可
		//（String.format 会忽略未被引用的实参，所以精简文案时无需改动这里）
		String base = Messages.get( this, "desc", stacks, MAX_STACKS );

		//形态数值随「当前形态 + 当前层数 + 神器等级」实时变化，故在文案里直接算出当前值
		int lvl = artifactLevel( target );
		if (form == TearSwordVisual.Form.BLESSING) {
			//与 act() 的结算口径保持一致：单次 L+2 点、上限 3(L+2)
			int perTick = lvl + SHIELD_AMOUNT_BONUS;
			return base + "\n\n" + Messages.get( this, "desc_blessing",
					stacks + lvl, perTick, SHIELD_CAP_TICKS * perTick );
		} else {
			return base + "\n\n" + Messages.get( this, "desc_despair",
					stacks * (lvl + DESPAIR_DAMAGE_BONUS), 3 * stacks + lvl );
		}
	}

	//==========================================================================
	// 视觉（角色贴图层）
	//==========================================================================

	/**
	 * 创建/更新环绕视觉。由下面的 {@code act()} 每回合调用，因此换层、切场景（旧的视觉随场景一起销毁）
	 * 之后会在下一回合自动重建；角色贴图尚未就绪时直接跳过，等下次再建。
	 */
	private void ensureVisual() {
		if (target == null) {
			freeVisual();
			return;
		}
		if (visual != null && visual.parent != null && visual.attached()) {
			visual.setForm( form );      //形态可能在上次 act 之后被 setForm 改过
			visual.setStacks( stacks );
			return;
		}

		freeVisual();

		if (target.sprite == null || target.sprite.parent == null) {
			return; //场景未就绪，下次 act 再建
		}

		visual = new TearSwordVisual( target );
		visual.setForm( form );          //此时还没有剑 → 直接落到目标形态，不播切换动画
		visual.setStacks( stacks );
		//TearSwordVisual 是 Group（Gizmo 的子类，不是 Visual），无法走 GameScene.effect()；
		//改为挂到角色贴图所在图层（即 GameScene 的 mobs 组，与 MasterpieceShow 同一范式）。
		//mobs 组里非 Visual 的成员会被 GameScene.sortMobSprites() 排到队尾，因此渲染在所有怪物贴图之上；
		//朝向同侧的那几柄另由视觉内部的「背身层」挂到 GameScene.floorEmitters（角色之下），见 TearSwordVisual。
		target.sprite.parent.add( visual );
	}

	private void freeVisual() {
		if (visual != null) {
			visual.killAndErase();
			visual = null;
		}
	}

	@Override
	public void fx( boolean on ) {
		if (on) {
			ensureVisual();
		} else {
			freeVisual();
		}
	}

	@Override
	public void detach() {
		freeVisual();
		super.detach();
		BuffIndicator.refreshHero();
	}

	//==========================================================================
	// 回合
	//==========================================================================

	@Override
	public boolean act() {
		if (stacks <= 0) {
			detach();
			return true;
		}

		//每回合兜底重建一次（换层/切场景后旧视觉已随场景销毁；正常情况下 act 外由视觉自身每帧更新）
		ensureVisual();

		//加护形态：随时间积攒护盾（每 3/x 回合 L+2 点，上限 3L+6 = 3(L+2)）。
		//刻意放在这里而不是神器的被动 buff 上：本 buff 只要还有剑就必然每回合 act（上面 ensureVisual
		//就靠它驱动），所以盾的积攒与剑的存续是同一份生命期，不会出现「剑在转、盾不涨」。
		TearSwordBlessing art = artifact( target );
		if (form == TearSwordVisual.Form.BLESSING && art != null && !art.cursed) {
			//L = 神器等级。公式自带下限（L=0 时单次仍有 SHIELD_AMOUNT_BONUS 点、上限 3 倍），
			//所以这里不再把 L 抬到 1——数值与「L+2 / 3L+6」的公式完全一致。
			int lvl = Math.max( 0, art.level() );
			int perTick = lvl + SHIELD_AMOUNT_BONUS;
			shieldProgress += stacks / SHIELD_PERIOD;

			while (shieldProgress >= 1f) {
				shieldProgress -= 1f;
				TearShield.add( target, perTick, SHIELD_CAP_TICKS * perTick );
			}
		}

		spend( TICK );
		return true;
	}

	//==========================================================================
	// 层数管理（静态入口）
	//==========================================================================

	public int stacks() {
		return stacks;
	}

	/** 当前形态（单独使用本 buff 时恒为 {@link TearSwordVisual.Form#DESPAIR 绝望}）。 */
	public TearSwordVisual.Form form() {
		return form;
	}

	/** 查询某角色泪剑的形态（无该 buff 时返回默认的 {@link TearSwordVisual.Form#DESPAIR 绝望}）。 */
	public static TearSwordVisual.Form form( Char ch ) {
		if (ch == null) return TearSwordVisual.Form.DESPAIR;
		TearSword b = ch.buff( TearSword.class );
		return (b == null) ? TearSwordVisual.Form.DESPAIR : b.form;
	}

	/**
	 * 切换某角色泪剑的形态（带「旋转 + 移位」的过渡动画，见 {@link TearSwordVisual#setForm}），
	 * 并<b>清空泪盾</b>（见 {@link #applyForm}）。
	 *
	 * <p>角色身上<b>还没有泪剑</b>时不做任何事（形态无处可存：buff 层数为 0 会自行 detach）；
	 * 调用方下次用 {@link #setStacks(Char, int, TearSwordVisual.Form)} 带形态把剑创建出来即可
	 * ——「无剑但有盾」时切换形态由玩家入口 {@code TearSwordBlessing.switchForm} 直接清盾。</p>
	 */
	public static void setForm( Char ch, TearSwordVisual.Form f ) {
		if (ch == null || f == null) return;

		TearSword b = ch.buff( TearSword.class );
		if (b == null || b.target == null) return;

		b.applyForm( f ); //含「形态没变就早退」与清盾，逻辑与 addStacks / setStacks 走同一条路
	}

	/** 查询某角色当前的泪剑层数（无该 buff 时为 0）。 */
	public static int stacks( Char ch ) {
		if (ch == null) return 0;
		TearSword b = ch.buff( TearSword.class );
		return (b == null) ? 0 : b.stacks;
	}

	/** 叠层入口：+{@code amount} 层（上限 {@link #MAX_STACKS}），必要时创建 buff 并同步视觉。 */
	public static void addStacks( Char ch, int amount ) {
		addStacks( ch, amount, null );
	}

	/**
	 * 叠层入口（可顺带指定形态）。{@code form} 为 null 时保持现有形态
	 * （新创建的 buff 则是默认的 {@link TearSwordVisual.Form#DESPAIR 绝望}）。
	 */
	public static void addStacks( Char ch, int amount, TearSwordVisual.Form form ) {
		if (ch == null || amount <= 0) return;

		TearSword b = ch.buff( TearSword.class );
		if (b == null) {
			b = Buff.affect( ch, TearSword.class );
		}
		//Buff.append 会把 attachTo 的失败结果丢掉、照旧返回实例，所以目标免疫（魔法免疫）时拿到的是
		//一个 target == null 的「幽灵」对象。这里显式挡掉，避免在未附着的对象上改层数。
		if (b == null || b.target == null) return;

		b.applyForm( form );
		b.stacks = Math.min( MAX_STACKS, b.stacks + amount );
		b.ensureVisual();
		BuffIndicator.refreshHero();
	}

	/** 直接设定层数（0 表示移除 buff）。 */
	public static void setStacks( Char ch, int amount ) {
		setStacks( ch, amount, null );
	}

	/**
	 * 直接设定层数（0 表示移除 buff），并可顺带指定形态（{@code null} = 不改形态）。
	 *
	 * <p>神器「泪锋的加护」走这个入口：泪剑由它提供、形态也归它管——buff 层数归零即 detach，
	 * 形态会随之丢失，所以每次召唤/维持时都要把神器上存的形态带进来。已有视觉时形态变化会播放
	 * 切换动画（{@link TearSwordVisual#setForm}）；buff 刚创建（尚无视觉）时直接按目标形态建出来，
	 * 不会出现"先以旧形态露一帧再转过去"。</p>
	 */
	public static void setStacks( Char ch, int amount, TearSwordVisual.Form form ) {
		if (ch == null) return;

		TearSword b = ch.buff( TearSword.class );
		if (b == null) {
			if (amount <= 0) return;
			b = Buff.affect( ch, TearSword.class );
		}
		if (b == null || b.target == null) return; //同上：免疫时拿到的是未附着的幽灵对象

		b.applyForm( form );

		b.stacks = Math.max( 0, Math.min( MAX_STACKS, amount ) );
		if (b.stacks <= 0) {
			b.detach();
		} else {
			b.ensureVisual();
			BuffIndicator.refreshHero();
		}
	}

	/**
	 * 应用形态参数：null 表示保持现状；有变化时同步给已有视觉（按是否有剑决定是否播切换动画），
	 * 并<b>清空所有泪盾</b>——形态一换，旧形态攒下的盾不再延续（见类注释「切换形态清泪盾」）。
	 *
	 * <p>清盾带 {@code stacks > 0} 守卫：buff 刚被创建时（0 层）形态字段总要从默认的「绝望」
	 * 变成神器要求的形态，那不算玩家切换形态，此时清盾会把「泪剑耗尽、重新唤剑」时保留下来的盾误清掉。</p>
	 */
	private void applyForm( TearSwordVisual.Form f ) {
		if (f == null || f == form) return;
		form = f;
		if (visual != null) visual.setForm( f );
		if (stacks > 0 && target != null) TearShield.clear( target );
	}

	//==========================================================================
	// 形态数值与减层计数（供宿主 Hook 查询）
	//==========================================================================

	/**
	 * 装备中的「泪锋的加护」；没装备时为 null。
	 *
	 * <p>神器可以占 artifact 槽也可以占 misc 槽（{@code Artifact extends KindofMisc}），两个都要查。</p>
	 */
	public static TearSwordBlessing artifact( Char ch ) {
		if (!(ch instanceof Hero)) return null;
		Belongings own = ((Hero)ch).belongings;
		if (own == null) return null;

		if (own.artifact instanceof TearSwordBlessing) return (TearSwordBlessing) own.artifact;
		if (own.misc instanceof TearSwordBlessing) return (TearSwordBlessing) own.misc;
		return null;
	}

	/**
	 * 提供泪剑的神器等级 L；没装备「泪锋的加护」时为 0（此时本 buff 纯靠调试窗叠层，
	 * 各数值退化成「不含神器等级」的版本）。
	 *
	 * <p>采用<b>拉取</b>而不是让神器每回合把等级推一份进来：等级会随升级即时变化，拉取式不会拿到过期值，
	 * 也少了一处同步点（形态之所以仍由神器推入，是因为它同时还要驱动切换动画）。</p>
	 *
	 * <p>神器可以占 artifact 槽也可以占 misc 槽（{@code Artifact extends KindofMisc}），两个都要查。</p>
	 */
	public static int artifactLevel( Char ch ) {
		TearSwordBlessing art = artifact( ch );
		return (art == null) ? 0 : art.level();
	}

	/**
	 * 加护形态：护甲的<b>最低防御</b>加成 = {@code 层数 + 神器等级}。
	 * 非加护形态、没有泪剑、没有神器时均为 0。
	 *
	 * <p>消费点在 {@code Hero.drRoll()}：把护甲减伤投掷区间的<b>下端</b>抬高同样的量，且下端不越过上端
	 * （超出部分按 {@link #clampRaisedMin} 作废），所以抬的是「保底减伤」，与「提高上限」类效果互不冲突。</p>
	 */
	public static int armorMinDrBonus( Char ch ) {
		TearSword b = of( ch );
		if (b == null || b.stacks <= 0) return 0;
		if (b.form != TearSwordVisual.Form.BLESSING) return 0;
		return b.stacks + artifactLevel( ch );
	}

	/**
	 * 诅咒惩罚的开关：装备中的「泪锋的加护」被诅咒时返回 true —— 护甲的最高防御减半。
	 *
	 * <p>诅咒状态的神器完全不可用（{@code TearSwordBlessing.actions} 不提供唤剑/切形态），
	 * 这条就是「身上捆着一件死神器」的代价。它与形态、泪剑层数都无关，也与泪剑是否在场无关。</p>
	 *
	 * <p>消费点在 {@code Hero.drRoll()}：上端砍半后再把下端夹回新上端之内（区间恒合法）。
	 * 取整用<b>向下取整</b>（{@code 5 → 2}）。</p>
	 */
	public static boolean hasCursedArmorPenalty( Char ch ) {
		TearSwordBlessing art = artifact( ch );
		return art != null && art.cursed;
	}

	/**
	 * 绝望形态：武器攻击力<b>上限</b>的削减量 = {@code 3 × 层数 + 神器等级}（下限不变）。
	 * 非绝望形态、没有泪剑、没有神器时均为 0。
	 *
	 * <p>消费点在 {@code KindOfWeapon.damageRoll(Char)}：只削投掷区间的<b>上端</b>
	 * （削到低于下限时就取平，见 {@link #clampReducedMax}），所以伤害依旧从下限起算，只是不再有高爆发。</p>
	 */
	public static int weaponMaxPenalty( Char ch ) {
		TearSword b = of( ch );
		if (b == null || b.stacks <= 0) return 0;
		if (b.form != TearSwordVisual.Form.DESPAIR) return 0;
		return 3 * b.stacks + artifactLevel( ch );
	}

	/**
	 * 抬高投掷区间的<b>下端</b>，抬后<b>绝不高于上端</b>——采用<b>截断</b>语义。
	 *
	 * <p>用于「护甲的最低防御 +(x+L)」：抬后的下端被夹在原有上端之内，所以
	 * <b>护甲上限永远不会被这条效果改动</b>；代价是当加成的量超过「上端 − 下端」时，
	 * 超出的部分被上端吞掉，加成不足额。
	 * 例：0~1 的布甲 +3 ⇒ 1~1；0~5 的皮甲 +2 ⇒ 2~5（这条没被吞，照常足额）。</p>
	 *
	 * <p>曾考虑过「足额」语义（把上端一并抬到同值，让加成全额兑现，代价是整体区间跟着上移），
	 * 两种都保证区间合法；<b>现按截断实现</b>——就是下面这一行直接夹到上端。</p>
	 */
	public static int clampRaisedMin( int min, int max, int raise ) {
		if (raise <= 0) return min;
		return Math.max( min, Math.min( Math.max( min, max ), min + raise ) );
	}

	/**
	 * 压低投掷区间的<b>上端</b>，且上端<b>绝不低于下端</b>——差值不足时取平。
	 *
	 * <p>用于「武器攻击力上限 −(3x+L)」：削到低于下限时区间退化成 {@code 下限~下限}，
	 * 伤害仍然从下限起算，只是不再有高爆发。</p>
	 */
	public static int clampReducedMax( int min, int max, int reduce ) {
		if (reduce <= 0) return max;
		return Math.max( min, max - Math.max( 0, reduce ) );
	}

	/**
	 * 区间合法化兜底（幂等）：上端不低于下端。
	 *
	 * <p>{@link #clampRaisedMin} 采用截断语义后，它本身已保证「下端 ≤ 上端」，所以正常路径上
	 * 本方法什么都不改；保留它是给 {@code Hero.drRoll} 留一道「无论上游语义怎么改，投掷区间恒合法」
	 * 的护栏——区间没被改过时也不会误伤。</p>
	 */
	public static int clampMaxToMin( int min, int max ) {
		return Math.max( min, max );
	}

	/**
	 * 记录一次「受击」，推进两形态共用的减层计数。由 {@code Hero.damage} 在<b>减免结算之前</b>调用，
	 * 所以「被护甲/护盾完全吃下的一下」同样算挨打（见 {@code Hero.damage} 里的落点说明）。
	 * 只有 {@link TearSwordVisual.Form#BLESSING 加护}形态计数——绝望形态的计数来自协同攻击。
	 *
	 * <p>如果想改成「无论什么形态挨打都计数」，删掉下面那行形态判断即可；两者共用同一个计数，
	 * 所以切换形态不会丢失进度。</p>
	 */
	public static void onHitTaken( Char ch ) {
		TearSword b = of( ch );
		if (b == null || b.stacks <= 0) return;
		if (b.form != TearSwordVisual.Form.BLESSING) return;
		b.countDecay( ch );
	}

	/** 内部的 buff 查询（含 {@code target == null} 的幽灵对象兜底，见 {@link #addStacks}）。 */
	private static TearSword of( Char ch ) {
		if (ch == null) return null;
		TearSword b = ch.buff( TearSword.class );
		return (b == null || b.target == null) ? null : b;
	}

	/** 计数 +1；满 {@link #DECAY_PER_STACK} 次就减 1 层泪剑并把计数清零。 */
	private void countDecay( Char ch ) {
		if (ch == null || stacks <= 0) return;
		if (++decayCount < DECAY_PER_STACK) return;

		decayCount = 0;
		//层数归零的那一次 setStacks 会 detach 掉本对象，之后不能再碰 this，故直接返回
		setStacks( ch, stacks - 1 );
	}

	//==========================================================================
	// 协同攻击
	//==========================================================================

	/**
	 * 命令 {@code attacker} 身上的泪剑集体向 {@code target} 突刺一次，并追加协同法术伤害：
	 * 伤害 = {@code 当前层数 × (神器等级 + }{@link #DESPAIR_DAMAGE_BONUS}{@code )}。
	 *
	 * <p>由 {@code Hero.onAttackComplete()} 在近战挥击结算处调用，且必须放在 {@code attack()} <b>之后</b>
	 * ——若泪剑先把目标打死，{@code attack()} 就会在尸体上白跑一遍（命中判定、武器附魔、
	 * 各种 Tracker 都在那里面）。</p>
	 *
	 * <p>目标位置在<b>调用当帧</b>从目标贴图读出并记为世界坐标（之后目标被击退/死亡/贴图回收都不影响
	 * 动画），所以本方法不会持有对 {@code target} 的引用；即使这一击已把目标打死，其贴图也只是原地播放
	 * 死亡动画、坐标依然有效。</p>
	 *
	 * <p><b>目标已死则不再结算伤害</b>：本方法在 {@code attack()} <b>之后</b>调用，目标完全可能已被那次
	 * 近战挥击打死。此时必须整体跳过 {@code damage()}（动画照常播）——{@code Char.damage()} 里的
	 * {@code !isAlive()} 守卫<b>救不了这种情形</b>，因为 boss 会覆写 {@code damage()} 且覆写里不判死亡：
	 * {@code Goo.damage()} 首行就是「{@code BossHealthBar} 未挂载 ⇒ {@code assignBoss()} +
	 * {@code Dungeon.level.seal()}」，对已死 boss 再调一次会把刚解锁的楼层重新锁上（薄暝 proc 同款坑，
	 * 见 {@code AGENTS.md} 第 6 节「多段伤害击杀 BOSS 后出口不解锁」）。</p>
	 *
	 * <p><b>形态限制</b>：只有 {@link TearSwordVisual.Form#DESPAIR 绝望}形态才协同突刺；
	 * 加护形态（剑竖直向下绕身旋转）会直接跳过整段逻辑——既不播动画也不结算伤害。</p>
	 *
	 * <p>伤害以 {@link MagicStrike} 这个<b>非 Buff 的标记类</b>实例为来源（{@code src}），
	 * 而不是本 buff 实例：{@code Char.damage} 会按 {@code src.getClass()} 归类，
	 * 而 {@code MagicStrike.class} 已登记进 {@code AntiMagic.RESISTS}，所以这条伤害
	 * <b>会被魔法免疫完全抵挡</b>（{@code isImmune} ⇒ 伤害归零）、被魔法抵抗雕文减伤，
	 * 并自动显示法术图标；同时因为来源不是 buff，魔法免疫<b>不会</b>把泪剑本身驱散掉
	 * ——两件事的完整推导见 {@link MagicStrike}。</p>
	 *
	 * <p>结算完伤害后推进<b>两形态共用的减层计数</b>（{@link #countDecay}）——注意计数刻意放在
	 * {@code damage()} 之外：即使这一击被魔法免疫完全挡下，剑也照样算「刺出去了」。</p>
	 */
	public static void strikeAt( Char attacker, Char target ) {
		if (attacker == null || target == null) return;

		TearSword b = of( attacker );
		if (b == null || b.visual == null || b.stacks <= 0) return;
		if (!b.visual.attached()) return; //视觉尚未重建（如刚换层的那一帧）

		//只有「绝望」形态会协同突刺：加护形态的剑是绕着角色"守"的环，不参与进攻
		//（形态语义「加护=守、绝望=攻」）。日后若要让加护也有协同，去掉这一行即可。
		if (b.form != TearSwordVisual.Form.DESPAIR) return;

		CharSprite ts = target.sprite;
		if (ts == null) return;

		//① 触发动画：目标贴图中心即「怪物位置」
		b.visual.strike( ts.x + ts.width / 2f, ts.y + ts.height / 2f );

		//② 结算伤害：层数 × (神器等级 + 2)。
		//target 可能已被上面那次近战挥击打死（本方法在 attack() 之后调用），此时必须整体跳过：
		//boss 的 damage() 覆写不判死亡（如 Goo.damage() 首行「未挂载 BossHealthBar ⇒ assignBoss + seal()」），
		//对尸体再调一次会把刚解锁的楼层重新锁上。Char.damage() 自带的 !isAlive() 守卫拦不住覆写。
		int dmg = b.stacks * (artifactLevel( attacker ) + DESPAIR_DAMAGE_BONUS);
		if (dmg > 0 && target.isAlive()) {
			target.damage( dmg, new MagicStrike() );
		}

		//③ 造成伤害 → 推进共用的减层计数（与加护形态的受击计数是同一份）
		b.countDecay( attacker );
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String STACKS = "stacks";
	private static final String STORE_FORM = "form";
	private static final String DECAY_COUNT = "decay_count";
	private static final String SHIELD_PROGRESS = "shield_progress";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
		//用 0/1 而不是枚举序号：缺键时 Bundle 返 0，正好落回默认的「绝望」
		bundle.put( STORE_FORM, form == TearSwordVisual.Form.BLESSING ? 1 : 0 );
		bundle.put( DECAY_COUNT, decayCount );
		bundle.put( SHIELD_PROGRESS, shieldProgress );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		stacks = bundle.getInt( STACKS );
		//旧档无此键 → 0 → 绝望形态（即本 buff 单独使用时原有的表现）
		form = (bundle.getInt( STORE_FORM ) == 1)
				? TearSwordVisual.Form.BLESSING
				: TearSwordVisual.Form.DESPAIR;
		decayCount = bundle.getInt( DECAY_COUNT );    //旧档无此键 → 0 → 从头计数
		shieldProgress = bundle.getFloat( SHIELD_PROGRESS ); //旧档无此键 → 0 → 从头蓄积
	}
}
