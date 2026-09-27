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
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blocking;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

/**
 * 「泪锋的加护」<b>加护形态</b>随时间积攒的护盾（{@link ShieldBuff} 子类）。
 *
 * <p>与 {@link Barrier 屏障} 的区别只有两点：<b>没有自然衰减</b>（只在挨打时被消耗），以及带一个
 * 由结算方设定的<b>上限</b>（= 3(L+2) = 3L+6，L 为神器等级）。这正是「随时间逐渐给护盾、
 * 上限 3L+6」的字面实现——攒到上限后继续积攒的份额直接作废，所以它是个有界资源而不是无限成长的墙。</p>
 *
 * <p>护盾上限由 {@code TearSword.act()}（加护形态的结算方）每次结算时用
 * {@link #add(Char, int, int)} 灌进来，并随 buff 一起写入存档；护盾本身不记「是谁给的」，
 * 所以卸下神器时由 {@code TearSwordBlessing} 主动 {@link #clear(Char)} 收回。</p>
 *
 * <p><b>切换形态（加护 ⇄ 绝望）时护盾同样被清空</b>（{@code TearSword.applyForm} 与
 * {@code TearSwordBlessing.switchForm} 两处），即盾不跨形态延续、切回加护要从零再攒；
 * 但泪剑单独消散（加护形态下被受击计数耗光）<b>不会</b>动盾——那是两笔独立的资源。</p>
 *
 * <p><b>顶层类</b>：它会被写进 {@code Char} 的 buff 列表并参与存档反射重建，因此不能做成内部类
 * （非静态内部类会被 {@code Bundle.get()} 跳过，见 {@link TearSword} 的同类说明）。</p>
 */
public class TearShield extends ShieldBuff {

	{
		type = buffType.POSITIVE;
	}

	/** 护盾上限（由结算方写入，= 3(L+2) = 3L+6）。0 表示尚未设置，此时不接受任何护盾。 */
	private int cap = 0;

	public void setCap( int cap ) {
		this.cap = Math.max( 0, cap );
	}

	public int cap() {
		return cap;
	}

	/**
	 * 叠加护盾（不超过 {@link #cap}），返回实际增加量（已满时为 0）。
	 * 走 {@link #setShield(int)}（只会抬高、不降低），所以不会把已有的护盾顶掉。
	 */
	public int addShield( int amount ) {
		if (amount <= 0 || cap <= 0) return 0;

		int before = shielding();
		setShield( Math.min( cap, before + amount ) );
		return shielding() - before;
	}

	@Override
	public boolean act() {
		//无自然衰减：只在挨打（absorbDamage）时减少，归零由 ShieldBuff.detachesAtZero 自动 detach
		if (target == null || !target.isAlive()) {
			detach();
			return true;
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ) {
		if (on) {
			target.sprite.add( CharSprite.State.SHIELDED );
		} else if (target.buff( Blocking.BlockBuff.class ) == null) {
			//与「格挡」雕文的护盾共用同一个 SHIELDED 状态，别把它一起摘掉（同 Barrier 的处理）
			target.sprite.remove( CharSprite.State.SHIELDED );
		}
	}

	@Override
	public int icon() {
		return BuffIndicator.ARMOR;
	}

	@Override
	public void tintIcon( Image icon ) {
		//与泪剑尾迹同色系（青蓝），和屏障的蓝、奥术护甲的紫区分开
		icon.hardlight( 0.35f, 0.85f, 1.15f );
	}

	@Override
	public String iconTextDisplay() {
		return Integer.toString( shielding() );
	}

	@Override
	public String desc() {
		return Messages.get( this, "desc", shielding(), cap );
	}

	//==========================================================================
	// 静态入口
	//==========================================================================

	/**
	 * 给 {@code ch} 叠加 {@code amount} 点护盾，并把上限设为 {@code cap}（= 3(L+2) = 3L+6）。
	 * 目标还没有该 buff 时自动创建；{@code amount}/{@code cap} 非正时什么都不做。
	 */
	public static void add( Char ch, int amount, int cap ) {
		if (ch == null || amount <= 0 || cap <= 0) return;

		TearShield s = ch.buff( TearShield.class );
		if (s == null) {
			s = Buff.affect( ch, TearShield.class );
		}
		//同 TearSword：Buff.append 会把 attachTo 的失败结果丢掉，可能拿到 target == null 的幽灵对象
		if (s == null || s.target == null) return;

		s.setCap( cap );
		s.addShield( amount );
	}

	/** 收回护盾（卸下神器时调用）。 */
	public static void clear( Char ch ) {
		if (ch != null) Buff.detach( ch, TearShield.class );
	}

	//==========================================================================
	// 存档
	//==========================================================================

	private static final String CAP = "cap";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( CAP, cap );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		cap = bundle.getInt( CAP ); //旧档无此键 → 0：下次结算时会被神器重新灌入
	}
}
