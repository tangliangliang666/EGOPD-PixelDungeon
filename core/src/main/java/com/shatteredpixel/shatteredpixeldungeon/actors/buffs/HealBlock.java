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
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WhiteSmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * 「禁疗」—— 洛伊德护符（{@code items.LloydTalisman}）命中后给目标挂上的负面状态。
 *
 * <h3>效果</h3>
 * <p>{@value #DURATION} 回合内，目标获得的<b>一切</b>血量恢复都被强制置 0。</p>
 *
 * <h3>唯一判定点 = {@code Char.heal(int)}</h3>
 * <p>本作把「把 HP 往上抬」这件事<b>收口</b>到了 {@code Char.heal(int)} 这一个方法上
 * （见 {@code Char.heal} 的注释与 {@link #blocks(Char)} 的说明），所以只要本 buff 在场，
 * {@code heal()} 就恒返回 0 —— 无论是药水、圣草、吸血鬼附魔、法杖，还是<b>CHESED 考验</b>
 * 的 {@code ChesedMend} 回血，一视同仁地被拦掉。<b>不要在别处再加判定</b>。</p>
 *
 * <h3>持续特效</h3>
 * <p>挂在身上期间持续散发白色烟雾（{@link WhiteSmokeParticle#FACTORY}），由 {@link #fx(boolean)}
 * 挂/摘发射器（Emitter 是共享池对象，必须<b>缓存引用</b>再关，见 {@code HeartFateBuff} 的同一手法）。</p>
 *
 * <p><b>顶层类</b>：随存档序列化并由反射重建，不能做成非静态内部类。</p>
 */
public class HealBlock extends FlavourBuff {

	/** 持续时间（回合）。洛伊德护符固定用它。 */
	public static final float DURATION = 50f;

	{
		//负面状态。这里标 NEGATIVE 是<b>刻意</b>的，且不会踩「全图怪一入场就醒」那个坑——
		//本 buff 只在被护符命中时施加，绝不在刷怪时挂（那条红线针对的是 Trials.bindMobPassives
		//那种「入场即挂」的被动 buff）。
		//副产物：神策军宝箱怪（Mimic）身上的 NEGATIVE buff 会触发 Mimic.add() 的自动暴露逻辑。
		type = buffType.NEGATIVE;
		announced = true;
	}

	//==========================================================================
	// 施加 / 判定
	//==========================================================================

	/** 给 {@code ch} 挂上（或刷新）禁疗。已有的走 {@link Buff#prolong}：只延不缩、不叠加。 */
	public static void apply( Char ch, float duration ){
		if (ch == null || !ch.isAlive()) return;
		Buff.prolong( ch, HealBlock.class, duration );
	}

	/**
	 * 该单位此刻是否处于禁疗（{@code Char.heal} 的唯一判据）。
	 *
	 * <p>做成静态工具是刻意的：这样「禁疗」的全部语义只在这里 + {@code Char.heal} 两处，
	 * 以后要改成「只禁某类回血」也只需动这两个地方。</p>
	 */
	public static boolean blocks( Char ch ){
		return ch != null && ch.buff( HealBlock.class ) != null;
	}

	//==========================================================================
	// 持续粒子
	//==========================================================================

	/** 持续烟雾的发射器（共享池对象，须缓存以便关掉）。瞬态，不进存档。 */
	private transient Emitter smoke = null;

	@Override
	public void fx( boolean on ) {
		if (on) {
			if (smoke == null && target != null && target.sprite != null) {
				smoke = target.sprite.emitter();
				if (smoke != null) {
					smoke.pour( WhiteSmokeParticle.FACTORY, 0.09f );
				}
			}
		} else {
			if (smoke != null) {
				smoke.on = false;
				smoke = null;
			}
		}
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon() {
		//暂无自绘图标 ⇒ 借原版「草药治疗」帧 19（BuffIndicator.HERB_HEALING）。
		//按 BuffIndicator 里的【惯例】条目，未自绘的 buff 不预留帧号常量；
		//等画出来了再按当时的空帧顺序新增常量并改过来。
		return BuffIndicator.HERB_HEALING;
	}

	@Override
	public void tintIcon( Image icon ) {
		//暗红着色：一眼看出是「回血被掐掉」，与同帧的圣草治疗区分开
		icon.hardlight( 0.55f, 0.13f, 0.13f );
	}

	@Override
	public String desc() {
		//描述只有一句话，但保留剩余回合（FlavourBuff.dispTurns() 返回 String ⇒ 模板里用 %s）
		return Messages.get( this, "desc", dispTurns() );
	}
}
