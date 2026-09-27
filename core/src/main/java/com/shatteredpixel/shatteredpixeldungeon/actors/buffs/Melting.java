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
import com.shatteredpixel.shatteredpixeldungeon.actors.blobs.Fire;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroSubClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.items.wands.WandOfFireblast;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.enchantments.Blazing;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.BrandWorkshop;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DiamondSword;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.DimDusk;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LastLight;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.SealedSwordBase;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;

import java.util.HashSet;

/**
 * 「融化」——中指 长兄 转职「<b>背叛家人者</b>」天赋「融化而死」施加的负面状态。
 *
 * <p>挂在<b>敌人</b>身上：莱瓦汀系列武器的攻击命中后施加，{@code Laevateinn} 形态下则每回合
 * 把周围火场里的敌人一并拖进融化（见 {@code Laevateinn.burnAura}）。</p>
 *
 * <h3>效果</h3>
 * <ul>
 *   <li>带融化的敌人受到的<b>火焰伤害</b>提高 {@code 50% × 天赋点数}（+1/+2/+3 ⇒ +50%/+100%/+150%）。</li>
 *   <li><b>免疫火焰</b>的敌人吃不到火焰增伤，于是改为其受到的<b>一切</b>伤害都提高同样比例
 *       （否则这个状态对它们等于不存在）。判定用 {@code isImmune(Burning.class)}，与原版
 *       「火焰免疫」的口径一致。</li>
 * </ul>
 *
 * <h3>持续时间</h3>
 * <p>{@value #DURATION} 回合；重复施加走 {@link Buff#prolong}（<b>只延不缩、不叠加</b>），
 * 所以是「刷新」而不是「叠层」。</p>
 *
 * <h3>图标</h3>
 * <p>{@link BuffIndicator#MELTING}（帧 116）。{@code announced = true} ⇒ 第一次挂上时会在
 * 敌人头顶弹一次名字（同一个敌人只弹一次；{@code prolong} 复用同一个实例，不会重复弹）。</p>
 *
 * <p><b>顶层类</b>：挂在怪物身上、随存档序列化并通过反射重建，不能做成非静态内部类。</p>
 */
public class Melting extends FlavourBuff {

	/** 持续时间（回合）。施加与刷新都用它。 */
	public static final float DURATION = 8f;

	/** 每点「融化而死」带来的伤害增幅：+50% / +100% / +150%。 */
	public static final float BONUS_PER_POINT = 0.5f;

	{
		type = buffType.NEGATIVE;
		announced = true;
	}

	//==========================================================================
	// 投入点数 / 施加
	//==========================================================================

	/** 「融化而死」的投入点数（0 = 未点 / 不是背叛家人者）。 */
	public static int points(){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.subClass != HeroSubClass.FAMILY_BETRAYER) return 0;
		return hero.pointsInTalent( Talent.MELT_TO_DEATH );
	}

	/**
	 * 给 {@code ch} 施加 / 刷新「融化」。
	 *
	 * <p>没点「融化而死」时静默跳过——调用方（莱瓦汀系列的命中钩子、莱瓦汀的火场）不需要自己判天赋。</p>
	 */
	public static void apply( Char ch ){
		if (ch == null || !ch.isAlive()) return;
		if (points() <= 0) return;
		Buff.prolong( ch, Melting.class, DURATION );
	}

	//==========================================================================
	// 伤害放大（唯一取点：Char.damage 里乘一次）
	//==========================================================================

	/**
	 * 「融化」对<b>这一次伤害</b>的放大倍率（1 = 不放大）。
	 *
	 * <p>只有两处例外会返回 1：自己身上没有融化；或者目标不免疫火焰、而这次伤害又不是火焰来源。</p>
	 *
	 * @param defender 挨打方
	 * @param src      伤害来源（{@code Char.damage} 的第二个参数）
	 */
	public static float damageMultiplier( Char defender, Object src ){
		if (defender == null || defender.buff( Melting.class ) == null) return 1f;

		int points = points();
		if (points <= 0) return 1f;

		//免疫火焰：吃不到火焰增伤 ⇒ 改为一切伤害都按同比例提高
		if (defender.isImmune( Burning.class )){
			return 1f + BONUS_PER_POINT * points;
		}

		return isFireDamage( src ) ? 1f + BONUS_PER_POINT * points : 1f;
	}

	/**
	 * 这次伤害是否算「火焰伤害」——本作对火焰来源的<b>唯一判定表</b>，
	 * 「融化」的增伤口径完全由它决定。
	 *
	 * <p>说明：原版绝大多数火焰伤害最终都以 {@link Burning} 这个 buff 当 src
	 * （点燃、火场、火焰陷阱、烈焰附魔的灼烧段都归到它），剩下的直接伤害来源逐个列出。
	 * 以后新增带火焰直伤的武器/法术，记得往 {@link #FIRE_SOURCES} 里补一条。</p>
	 *
	 * <p><b>与 {@code Char.damage} 里的伤害浮字图标表是两回事</b>：那张表只按需要
	 * 逐条列出「要画火焰小图标的来源」（例如 {@code SealedSwordBase} 的附加火伤），
	 * 不覆盖烈焰法杖这类仍按魔法显示的来源。两处的判据刻意分开，避免改一个显示把数值口径带偏。</p>
	 */
	public static boolean isFireDamage( Object src ){
		if (src == null) return false;
		for (Class c : FIRE_SOURCES){
			if (c.isAssignableFrom( src.getClass() )) return true;
		}
		return false;
	}

	/** 火焰伤害的来源类（子类也算，所以 {@code Laevateinn} 等形态自动包含在内）。 */
	private static final HashSet<Class> FIRE_SOURCES = new HashSet<>();
	static {
		FIRE_SOURCES.add( Burning.class );            //燃烧（点燃/火场/火焰陷阱/烈焰附魔的灼烧段都归到它）
		FIRE_SOURCES.add( Fire.class );               //火场本身
		FIRE_SOURCES.add( SealedSwordBase.class );    //莱瓦汀系列附带的火伤（src 是武器本身）
		FIRE_SOURCES.add( DimDusk.FlameStrike.class );//薄暝的灼烧光环
		FIRE_SOURCES.add( Blazing.class );            //烈焰附魔的即时灼烧
		FIRE_SOURCES.add( CharcoalResinBuff.class );  //焦炭松脂（烈焰涂层）
		FIRE_SOURCES.add( BrandWorkshop.class );      //内置炽热
		FIRE_SOURCES.add( DiamondSword.class );       //火焰附加
		FIRE_SOURCES.add( LastLight.class );          //薄暮之光的灼烧
		FIRE_SOURCES.add( WandOfFireblast.class );    //烈焰法杖
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon(){
		return BuffIndicator.MELTING;
	}

	/**
	 * <b>两个参数缺一不可</b>：文本里 {@code %1$d} = 增伤百分比、{@code %2$s} = 剩余回合。
	 * 只传前一个会让 {@code String.format} 抛 {@code MissingFormatArgumentException}，
	 * 而 {@code Messages.format} 的兜底是「返回原始格式串」⇒ 面板上会原样显示 {@code %1$d%%} 与
	 * {@code %2$s}（表现为乱码）。剩余回合由 {@link FlavourBuff#dispTurns()} 提供（读
	 * {@code visualcooldown()}，与图标上的数字同源）。
	 */
	@Override
	public String desc(){
		return Messages.get( this, "desc",
				(int)(BONUS_PER_POINT * 100 * Math.max( 1, points() )),
				dispTurns() );
	}
}
