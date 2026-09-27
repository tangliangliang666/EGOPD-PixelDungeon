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
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.AfterimageSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * 「残像纠缠」：角色呈现**不稳定**状态——贴身浮出若干个半透明残像，渲染在角色贴图**之下**，
 * 各自比前一个再多延迟一小段时间，复刻角色的移动轨迹；追上角色时轻轻震荡一下，
 * 挨打时被甩开再慢慢回弹——<b>挨打也是分层的</b>，外层残像晚一点起步、甩得更远，
 * 看起来就是一次冲击波从内往外传出去。视觉主体与跟随方式见 {@link AfterimageSprite}。
 *
 * <p><b>层数＝残像个数</b>（{@link #stacks}，1~{@link #MAX_STACKS}＝4）。层数用的是本工程
 * 既有的惯例（照 {@code HuntingTarget} / {@code Brag}）：公有 {@code stacks} 字段
 * ＋ 静态 {@link #addStack} / {@link #setStacks} / {@link #stacksOn} ＋
 * 覆写 {@link #iconTextDisplay()} 把层数显示在图标上。</p>
 *
 * <p>本 buff <b>不参与任何数值结算</b>，只负责残像贴图的生命周期：</p>
 * <ul>
 *   <li>挂载/销毁写在 {@link #fx(boolean)} 里（{@code Buff} 的既定钩子）：
 *       {@code Buff.affect} 时 {@code Char.add()} 会调 {@code fx(true)}，
 *       {@code Buff.detach} 时 {@code Buff.detach()} 会调 {@code fx(false)}。</li>
 *   <li><b>残像共用一份轨迹</b>（{@link AfterimageSprite.Trail}）：轨迹由残像自己每帧采样
 *       （同一帧去重），这里只负责创建/销毁，不参与逐帧更新——{@code act()} 是回合制的。</li>
 *   <li><b>层必须先选对</b>：残像走 {@link GameScene#floorEmittersAdd}（floorEmitters 层）。
 *       分层顺序是 {@code terrain < ... < floorEmitters < mobs < emitters < effects < fog}，
 *       floorEmitters 在角色所在的 mobs 层之前添加 ⇒ 渲染在角色贴图之下。
 *       若改用 {@code GameScene.effect(...)}（effects 层）会盖在角色脸上。</li>
 *   <li><b>添加顺序＝叠放顺序</b>：由远及近添加，延迟最小的那个最后加、画在最上层。</li>
 *   <li><b>{@code fx(true)} 要幂等且鲁棒</b>：{@code Char.updateSpriteState()}（读档、场景重建、
 *       角色重新入场）会对身上每个 buff 再调一次 {@code fx(true)}；而
 *       {@code GameScene.floorEmittersAdd} 在 {@code scene == null} 时是空转（挂不上）。
 *       所以判据用「没建，<b>或建了但数量与层数不符、或没挂上</b>」而不是「没建」，
 *       否则一旦首次挂载空转，残像就永远不会出现了。</li>
 * </ul>
 *
 * <p>测试入口：{@code items.AfterimageTester}（调试窗口调出，反复使用可循环 1→2→3→4→0 层）。</p>
 */
public class Afterimage extends Buff {

	/** 层数上限（＝残像个数上限）。 */
	public static final int MAX_STACKS = AfterimageSprite.MAX_COUNT;

	private static final String STACKS = "stacks";

	/** 层数＝残像个数（1~{@link #MAX_STACKS}）。对外只经 {@link #addStack}/{@link #setStacks} 改动。 */
	public int stacks = 1;

	/** 当前的几个残像（按序号 0..count-1，即由近及远）。 */
	private final ArrayList<AfterimageSprite> residues = new ArrayList<>();

	/** 所有残像共用的角色轨迹记录。 */
	private AfterimageSprite.Trail trail;

	{
		//中性状态：纯粹是视觉表现，不加成也不惩罚
		type = buffType.NEUTRAL;
		announced = true; //弹 buff 名，方便测试时肉眼确认状态切换
	}

	@Override
	public boolean act() {
		//残像的位置/帧更新由贴图自身每帧驱动，这里只需维持 buff 存活
		spend( TICK );
		return true;
	}

	//==========================================================================
	// 层数
	//==========================================================================

	/**
	 * 实际生效的残像个数：夹到 1..{@link #MAX_STACKS}，并且不超过取色表
	 * {@code AfterimageSprite.TINTS} 的长度（两者理应相等，这里再挡一道，
	 * 免得日后有人调高上限却忘了补颜色 ⇒ {@code TINTS[i]} 越界崩溃）。
	 */
	public int count() {
		int n = Math.max( 1, Math.min( stacks, MAX_STACKS ) );
		return Math.min( n, AfterimageSprite.TINTS.length );
	}

	/** 目标身上的残像层数（没有该 buff 返回 0）。 */
	public static int stacksOn( Char ch ) {
		if (ch == null) return 0;
		Afterimage a = ch.buff( Afterimage.class );
		return a == null ? 0 : a.stacks;
	}

	/**
	 * 叠一层（身上没有该 buff 则新建）。返回叠加后的层数；已满则原样返回 {@link #MAX_STACKS}。
	 *
	 * <p>层数变化后残像个数要跟着变，所以内部会 {@link #refresh()} 重建。</p>
	 */
	public static int addStack( Char ch ) {
		if (ch == null) return 0;
		Afterimage a = ch.buff( Afterimage.class );
		if (a == null) {
			a = Buff.affect( ch, Afterimage.class );
			a.stacks = 1;
		} else if (a.stacks < MAX_STACKS) {
			a.stacks++;
		}
		a.refresh();
		return a.stacks;
	}

	/** 把层数直接设为 {@code n}（夹到 1..MAX_STACKS），并立刻重建残像。返回实际层数。 */
	public static int setStacks( Char ch, int n ) {
		if (ch == null) return 0;
		Afterimage a = ch.buff( Afterimage.class );
		if (a == null) {
			a = Buff.affect( ch, Afterimage.class );
		}
		a.stacks = Math.max( 1, Math.min( n, MAX_STACKS ) );
		a.refresh();
		return a.stacks;
	}

	//==========================================================================
	// 受击冲击
	//==========================================================================

	/**
	 * 角色受击时调用：让身上的残像被朝「远离伤害来源」的方向甩开，随后自动回弹归位。
	 *
	 * <p>这里只是<b>广播</b>一个方向：真正的幅度、时长、以及「第几层要等多久才动」都在
	 * {@link AfterimageSprite#flinch(float, float)} 里按各自的序号算 ⇒ 一次挨打在视觉上表现为
	 * 冲击波从内层往外一层层传出去（见 {@code AfterimageSprite.FLINCH_LAYER_DELAY}）。
	 * 所以本方法不需要知道层数，也不该在这里做任何延迟——延迟必须留在贴图侧，
	 * 否则残像的「起步时刻」会和它自己那条轨迹回放错位。</p>
	 *
	 * <p>调用点在 {@code Hero.damage()}（只对英雄生效——残像是给玩家角色的表现）。
	 * 拿不到方向（陷阱、环境伤害等非 {@link Char} 来源）时随机甩一个方向，
	 * 总之「挨打就该有反应」。身上没有残像时直接返回，零开销。</p>
	 *
	 * @param src 伤害来源，可以是 {@link Char}（近战/远程/技能），也可以是别的任何东西
	 */
	public static void flinch( Char ch, Object src ) {
		if (ch == null) return;
		Afterimage a = ch.buff( Afterimage.class );
		if (a == null || a.residues.isEmpty()) return;

		float dx = 0f, dy = 0f;
		if (src instanceof Char && ((Char)src).sprite != null && ch.sprite != null) {
			//朝「被推离攻击者」的方向甩（与攻击方向相反），最符合「被打飞」的直觉
			dx = ch.sprite.x - ((Char)src).sprite.x;
			dy = ch.sprite.y - ((Char)src).sprite.y;
		}

		float len = (float)Math.sqrt( dx * dx + dy * dy );
		if (len < 0.001f) {
			//方向不可用（陷阱、环境伤害，或双方恰好重合）⇒ 随机方向，保证一定有反馈
			float ang = Random.Float( 6.2832f );
			dx = (float)Math.cos( ang );
			dy = (float)Math.sin( ang );
		} else {
			dx /= len;
			dy /= len;
		}

		for (AfterimageSprite s : a.residues) {
			s.flinch( dx, dy );
		}
	}

	//==========================================================================
	// 贴图生命周期
	//==========================================================================

	@Override
	public void fx( boolean on ) {
		if (on) {
			//「未建成 / 数量与层数不符 / 建成但没挂上场景」三种情况都要重建
			if (target != null && target.sprite != null
					&& (!attached() || residues.size() != count())) {
				build();
			}
		} else {
			clearResidues();
		}
	}

	/** 按当前层数立刻重建残像（层数变化后调用）。 */
	public void refresh() {
		clearResidues();
		fx( true );
	}

	private void build() {
		clearResidues();

		trail = new AfterimageSprite.Trail();
		//由远及近添加：先加的在下面，延迟最小（最近）的残像最后加、叠在最上层
		for (int i = count() - 1; i >= 0; i--) {
			AfterimageSprite s = new AfterimageSprite(
					target.sprite, trail, i, AfterimageSprite.TINTS[i] );
			//渲染在角色贴图之下的层（不是 GameScene.effect）
			GameScene.floorEmittersAdd( s );
			residues.add( s );
		}
	}

	/** 残像是否都还挂在场景里；读档重建、场景重建等情况下可能整体丢失，需要重建。 */
	private boolean attached() {
		if (residues.isEmpty()) return false;
		for (AfterimageSprite s : residues) {
			if (s.parent == null) return false;
		}
		return true;
	}

	private void clearResidues() {
		for (AfterimageSprite s : residues) {
			s.killAndErase(); //null-safe：没挂上场景的游离对象也能安全回收
		}
		residues.clear();
		trail = null;
	}

	//==========================================================================
	// 显示与存档
	//==========================================================================

	@Override
	public int icon() {
		//暂无自绘图标 ⇒ 借原版帧 50（UPGRADE，增强箭头），配合 tintIcon 染成蓝紫与「强化」区分。
		//按 BuffIndicator 里的【惯例】条目，未自绘的 buff 不预留帧号常量——等画好后
		//按当时的空帧顺序在 BuffIndicator 里新增常量，再把这里改过去。
		return BuffIndicator.UPGRADE;
	}

	@Override
	public void tintIcon( Image icon ) {
		//与残像本体同色系的冷色调
		icon.hardlight( 0.62f, 0.72f, 1f );
	}

	/** 桌面 UI 的大图标上显示层数（＝残像个数）。 */
	@Override
	public String iconTextDisplay() {
		return Integer.toString( stacks );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", stacks );
	}

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( STACKS, stacks );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		//getInt 缺键返 0 ⇒ 必须夹一次，否则旧存档读进来层数变 0、一个残像都不建
		stacks = Math.max( 1, Math.min( bundle.getInt( STACKS ), MAX_STACKS ) );
	}
}
