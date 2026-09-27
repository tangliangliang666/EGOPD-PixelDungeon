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

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * 「怨恨标记」（中指 长兄 转职「忠义巡礼者」的职业特性）：<b>叠层型负面 buff</b>，
 * 挂在<b>敌人</b>身上。
 *
 * <h3>来源</h3>
 * <p>转职忠义巡礼者之后，<b>任何攻击命中中指长兄本人或任意友方单位</b>的敌人，都会获得
 * 1 层怨恨标记。<b>近战与投掷</b>走 {@code VengeanceArts.onAttackLanded}（挂在 {@code Char.attack}
 * 的命中分支——与中指长兄 T2「永不遗忘」的 {@code RevengeTarget} 同一处调用点）；
 * <b>远程 / 法术弹道</b>（萨满、术士、DM 系列、元素、眼魔等等，它们不走 {@code Char.attack}）
 * 走 {@code VengeanceArts.onRangedAttackHit}（挂在 {@code Char.hit} 的 3 参重载）。
 * 两条路各收一次口、互不重叠。所以敌人越是追着你和召唤物打，身上的标记越厚。</p>
 *
 * <h3>用途</h3>
 * <p>标记本身没有任何数值影响，它只是<b>复仇技艺的弹药</b>：五式复仇技能各自要求目标身上
 * 有 1/2/3/4/5 层标记，使用时从该目标身上扣掉对应层数（见 {@code VengeanceArts.Move}）。
 * 这样同一只敌人不会被同一招反复刷，逼着玩家「先挨打、再清算」。</p>
 *
 * <h3>持续时间</h3>
 * <p>默认 {@value #DURATION} 回合；重复挨打只<b>刷新</b>计时（{@code Buff.prolong}，只延不缩），
 * 不会把计时器一叠一叠堆成几十回合。专精天赋「永志不忘」+1 会让它<b>永不消退</b>——
 * 判定点是 {@code VengeanceArts.marksLastForever()}，由 {@link #act()} 在每次到期时就地续期。</p>
 *
 * <h3>为什么有层数上限</h3>
 * <p>{@link #MAX_STACKS} 取 {@value #MAX_STACKS}：最深的一式只要 5 层，而「即刻处刑」的
 * 专精天赋「加倍清算！」+3 会把目标<b>剩余</b>层数一次清空、每层换 5 回合余威——不封顶的话
 * 一个长期挨打的木桩能换出上百回合，所以这里封死。</p>
 *
 * <h3>显示</h3>
 * <p>{@code announced = true} ⇒ 第一次挂上时会在敌人头顶弹一次名字（同一个敌人只弹一次）；
 * 图标 {@link BuffIndicator#GRUDGE_MARK} 与层数用于查看敌人状态时的 buff 列表。</p>
 *
 * <p><b>顶层类</b>：挂在怪物身上、随存档序列化并通过反射重建，不能做成非静态内部类。</p>
 */
public class GrudgeMark extends FlavourBuff {

	/** 层数上限（最深的一式只要 5 层；封顶避免「即刻处刑」余威被无限拉长）。 */
	public static final int MAX_STACKS = 10;

	/** 默认持续时间（回合）。「永志不忘」+1 会让它变成无限。 */
	public static final float DURATION = 20f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	/** 当前层数。 */
	public int stacks = 0;

	//==========================================================================
	// 静态入口
	//==========================================================================

	/** 给 {@code ch} 叠 {@code amount} 层怨恨标记（自动建 buff、刷新计时、封顶 {@link #MAX_STACKS}）。 */
	public static void gain( Char ch, int amount ){
		if (ch == null || !ch.isAlive() || amount <= 0) return;

		GrudgeMark mark = ch.buff( GrudgeMark.class );
		if (mark == null){
			mark = Buff.affect( ch, GrudgeMark.class );
		}
		//Buff.append 会吞掉 attachTo 的失败结果，可能拿到 target == null 的幽灵对象
		if (mark == null || mark.target == null) return;

		//刷新持续时间：prolong 是「只延不缩」，反复挨打不会把计时器堆叠成几十回合
		Buff.prolong( ch, GrudgeMark.class, DURATION );

		int before = mark.stacks;
		mark.stacks = Math.min( MAX_STACKS, mark.stacks + amount );
		if (mark.stacks != before) refreshIndicator( ch );
	}

	/**
	 * 到期时的收尾：默认<b>消退</b>；「永志不忘」+1 时就地续期，变成永久。
	 *
	 * <p>续期用 {@code spend(DURATION)}（不是 {@code postpone}）：此刻已经过期，
	 * {@code postpone} 的 {@code time < now + duration} 判据在这里恒成立，两者等价但前者更直白。</p>
	 */
	@Override
	public boolean act(){
		if (VengeanceArts.marksLastForever()){
			spend( DURATION );
			return true;
		}
		detach();
		return true;
	}

	/**
	 * 标记层数变了就报一次刷新。
	 *
	 * <p>标记挂在敌人身上，所以常规情况下刷新的是 Boss 那一条 buff 条（{@code refreshBoss}）——
	 * 普通怪物没有 buff 条，这条调用等于空转；只有 Boss 会在头顶显示自己的 buff 与层数。</p>
	 */
	private static void refreshIndicator( Char ch ){
		if (ch == Dungeon.hero) {
			BuffIndicator.refreshHero();
		} else {
			BuffIndicator.refreshBoss();
		}
	}

	/** 目标当前的怨恨标记层数（无标记返回 0）。 */
	public static int stacksOn( Char ch ){
		if (ch == null) return 0;
		GrudgeMark mark = ch.buff( GrudgeMark.class );
		return mark == null ? 0 : mark.stacks;
	}

	/**
	 * 消耗 {@code amount} 层怨恨标记。
	 *
	 * @return 实际消耗掉的层数（不足时为 0，且不会扣到负数）；扣完则整条 buff 移除。
	 */
	public static int consume( Char ch, int amount ){
		if (ch == null || amount <= 0) return 0;
		GrudgeMark mark = ch.buff( GrudgeMark.class );
		if (mark == null || mark.stacks < amount) return 0;

		mark.stacks -= amount;
		if (mark.stacks <= 0){
			mark.detach();
		} else {
			refreshIndicator( ch );
		}
		return amount;
	}

	/**
	 * 清空目标身上<b>全部</b>怨恨标记并返回被清掉的层数（「即刻处刑」的专精天赋 +3 用）。
	 */
	public static int clearAll( Char ch ){
		if (ch == null) return 0;
		GrudgeMark mark = ch.buff( GrudgeMark.class );
		if (mark == null) return 0;
		int had = mark.stacks;
		mark.detach();
		return had;
	}

	//==========================================================================
	// 显示 / 存档
	//==========================================================================

	@Override
	public int icon(){
		return BuffIndicator.GRUDGE_MARK;
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( stacks );
	}

	@Override
	public String name(){
		return Messages.get( this, "name" );
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", stacks, MAX_STACKS, turnText() );
	}

	/**
	 * 剩余回合的显示文本：「永志不忘」+1 时是「无限」，否则是具体回合数。
	 *
	 * <p>「无限」走独立的消息键（{@code actors.buffs.grudgemark.forever}），
	 * 免得把 {@code ∞} 这类符号写死在描述模板里。</p>
	 */
	public String turnText(){
		if (VengeanceArts.marksLastForever()){
			return Messages.get( this, "forever" );
		}
		return Integer.toString( Math.max( 1, (int)Math.ceil( visualcooldown() ) ) );
	}

	private static final String STACKS = "stacks";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		//getInt 缺键返 0 ⇒ 老存档 / 异常 bundle 下夹进 1..MAX_STACKS，避免出现「有标记但 0 层」的幽灵状态
		stacks = Math.max( 1, Math.min( MAX_STACKS, bundle.getInt( STACKS ) ) );
	}
}
