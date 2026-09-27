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
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Talent;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Eye;
import com.shatteredpixel.shatteredpixeldungeon.effects.FloatingText;
import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.utils.Bundle;

import java.util.HashSet;

/**
 * 「咬紧牙关」——中指 长兄 盔甲技能 {@code GritTeeth} 的状态。
 *
 * <h3>免死</h3>
 * <p>持有期间，英雄血量归零<b>不会死亡</b>：判定放在 {@code Hero.isAlive()}——
 * 那条分支正是原版狂战士「狂暴」免死走的那一条（{@code HP &lt;= 0} 时返回
 * {@code berserk.berserking()}），本 buff 只是又多了一个「免死来源」。所以「0 血不死」
 * 的全部行为（能不能继续行动、能不能被再次攻击、会不会被判负）都与狂暴免死完全一致。</p>
 *
 * <h3>被击穿</h3>
 * <p>有<b>两类</b>来源可以击穿免死，实现方式相同：在 {@code Char.damage} 的致命判定<b>之前</b>
 * 调一次 {@link #checkBypass} —— 这一击真的把血打到 0、且来源命中下面任一条时才置位
 * {@link #bypassed}，于是紧接着的 {@code isAlive()} 立即改判、正常死亡。
 * <b>只有致命的一击才算</b>——没打死你的时候不会提前作废这次免死。</p>
 * <ol>
 *   <li><b>解离射线</b>（{@link #RAY_SOURCES}）：邪能魔眼的射线、最终 boss 的死亡射线；</li>
 *   <li><b>「自伤换成长」</b>（{@link SelfHarmCost}）：蓄血圣杯的血祭、割腕的割腕。
 *       那两笔伤害是<b>升级代价</b>，升级判据只是「挨完这一下还活着」——若被免死兜住，
 *       就是每一下都成功升级、代价还被抹平，等于无条件刷满等级。</li>
 * </ol>
 *
 * <h3>到期结算</h3>
 * <p>{@link #act()} 在 {@code time} 到期时触发一次（{@code FlavourBuff} 不按回合 tick，所以
 * 这个 {@code act()} 只在到期那一刻跑）：先作废免死（让 {@code Hero.isAlive()} 马上改判成「会死」），
 * 再结算「绝境迫发」的回血，最后才看要不要真的死——回血够就把人从 0 血救回来。
 * 死亡分支照抄 {@code Berserk.act()}：{@code die(this)} + 还活着就 {@code Dungeon.fail(this)}。</p>
 *
 * <h3>图标</h3>
 * <p>按设计要求<b>套用「怒气（狂暴）」buff 的图标</b>（{@link BuffIndicator#BERSERK}），
 * 叠一层<b>紫色滤镜</b>（{@link #tintIcon}）。</p>
 *
 * <p><b>顶层类</b>：挂在英雄身上、随存档序列化并通过反射重建，不能做成非静态内部类。</p>
 */
public class GritTeethBuff extends FlavourBuff {

	{
		type = buffType.POSITIVE;
	}

	/**
	 * 是否已被击穿（解离射线，或血祭/割腕这类「自伤换成长」）。置位后本实例<b>不再提供免死</b>，
	 * 到期照常结算死亡。
	 *
	 * <p>注意 {@code Hero.isAlive()} 会把本 buff 缓存在一个字段里，所以到期时<b>必须先置位再 detach</b>：
	 * 否则缓存里那个已经卸载的实例仍然会说「免死」，英雄就永远死不了了。</p>
	 */
	private boolean bypassed = false;

	/** 本实例此刻是否提供免死。 */
	public boolean undying(){
		return !bypassed;
	}

	//==========================================================================
	// 击穿判定（解离射线 / 自伤换成长）
	//==========================================================================

	/**
	 * 「解离射线」的来源类——本作对射线的<b>唯一判定表</b>，
	 * 「击穿免死」的口径完全由它决定。
	 *
	 * <p>原版把邪能魔眼（{@code Eye}）与最终 boss 的死亡射线都归到 {@link Eye.DeathGaze}
	 * 这一个 src 上（{@code YogDzewa} 也是 {@code ch.damage(..., new Eye.DeathGaze())}），
	 * 所以这里一条就够了。以后新增射线类攻击，记得往这里补一条。</p>
	 */
	private static final HashSet<Class> RAY_SOURCES = new HashSet<>();
	static {
		RAY_SOURCES.add( Eye.DeathGaze.class );
	}

	/** 这次伤害是否算「射线」。 */
	public static boolean isRay( Object src ){
		if (src == null) return false;
		for (Class c : RAY_SOURCES){
			if (c.isAssignableFrom( src.getClass() )) return true;
		}
		return false;
	}

	/**
	 * 这次伤害是否算「自伤换成长」——判据**只有一条**：{@code src} 实现了
	 * {@link SelfHarmCost} 标记接口（蓄血圣杯 / 割腕；以后新增的道具实现该接口即自动适用）。
	 *
	 * <p>刻意与 {@code Talent.perseveranceCap} 共用同一个标记：那里豁免的是「血量下限保护」，
	 * 这里作废的是「免死」——两处面对的是同一个问题（自伤代价被保命机制抹平 ⇒ 无条件刷等级），
	 * 所以判据必须是同一个，别在这里另开一份 {@code instanceof} 名单。</p>
	 */
	public static boolean isSelfHarm( Object src ){
		return src instanceof SelfHarmCost;
	}

	/**
	 * 免死判定的「击穿」检查——由 {@code Char.damage} 在 {@code if (!isAlive()) die(src);}
	 * <b>之前</b>调用（那时伤害已经打进血量、{@code HP} 已归零到下限）。
	 *
	 * <p>三个短路条件缺一不可：只对英雄生效、只在这一击真的打到 0 血时判定、来源必须是
	 * 射线或「自伤换成长」。第一条同时承担「绝大多数伤害都打在怪物身上」的廉价短路职责；
	 * 第二条保证<b>非致命</b>的一击不会提前作废免死。</p>
	 */
	public static void checkBypass( Char ch, Object src ){
		if (!(ch instanceof Hero)) return;
		if (ch.HP > 0) return;                       //非致命的一击不提前作废免死
		if (!isRay( src ) && !isSelfHarm( src )) return;

		GritTeethBuff buff = ch.buff( GritTeethBuff.class );
		if (buff == null || buff.bypassed) return;
		buff.bypassed = true;
	}

	//==========================================================================
	// 生命周期
	//==========================================================================

	/**
	 * 施加 / 刷新。重新施放会把「被击穿」的标记一并清掉（否则再放一次也还是死的）。
	 * 时长走 {@link Buff#prolong}——<b>只延不缩、不叠加</b>，即「刷新」而不是「叠层」。
	 */
	public static void refresh( Char ch, float duration ){
		GritTeethBuff buff = Buff.affect( ch, GritTeethBuff.class );
		buff.bypassed = false;
		Buff.prolong( ch, GritTeethBuff.class, duration );
	}

	@Override
	public boolean act(){
		//① 先作废免死：Hero.isAlive() 可能正拿着本实例的缓存，置位后它就会改判成「会死」
		bypassed = true;

		//② 到期结算「绝境迫发」的回血（在人还没被结算死亡之前给）
		Hero hero = (target instanceof Hero) ? (Hero) target : null;
		if (hero != null){
			int heal = Talent.gritTeethEndHeal( hero );
			if (heal > 0){
				int healed = hero.heal( heal );
				if (healed > 0 && hero.sprite != null){
					hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healed ), FloatingText.HEALING );
				}
			}
		}

		//③ 摘掉自己，这时 isAlive() 才是真话 ⇒ 该死死、该活活
		detach();
		if (target != null && !target.isAlive()){
			target.die( this );
			if (!target.isAlive()) Dungeon.fail( this );
		}
		return true;
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon(){
		//按设计要求套用「怒气」buff 的图标
		return BuffIndicator.BERSERK;
	}

	@Override
	public void tintIcon( Image icon ){
		//紫色滤镜（保留红与蓝、压掉绿 ⇒ 偏紫）
		icon.hardlight( 1f, 0.35f, 1f );
	}

	private static final String BYPASSED = "bypassed";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( BYPASSED, bypassed );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		bypassed = bundle.getBoolean( BYPASSED );
	}
}
