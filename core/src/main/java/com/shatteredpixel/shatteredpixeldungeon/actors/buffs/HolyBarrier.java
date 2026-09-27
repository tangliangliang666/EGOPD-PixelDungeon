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

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.WhiteSmokeParticle;
import com.shatteredpixel.shatteredpixeldungeon.items.SelfHarmCost;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.shatteredpixel.shatteredpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * 「神圣屏障」—— 神圣卡（{@code items.HolyCard}）使用后获得的<b>一次性无敌</b>。
 *
 * <h3>效果</h3>
 * <p>挂上后<b>持续到被用掉为止</b>（没有回合数、没有衰减、不叠层）；任何一次「落到血量上的伤害」
 * 都会被<b>整段挡下</b>——不掉血、不闪红、不打断行动——然后本 buff 立刻消失。</p>
 *
 * <h3>挡什么、不挡什么</h3>
 * <p>判据只有一条，收在 {@link #blocks(Char, Object)}，<b>不要在别处再开一份名单</b>：</p>
 * <ul>
 *   <li><b>挡</b>：敌人攻击、陷阱、坠落、爆炸、环境伤害等一切「外部打过来的」伤害
 *       （它们的 {@code src} 是攻击者 / 陷阱 / 地形对象）；</li>
 *   <li><b>不挡 ①</b>：{@code src instanceof Buff} —— 毒 / 燃烧 / 流血 / 腐蚀 / 饥饿这类
 *       <b>自己身上的持续伤害</b>。它们是每回合跳一次的小额掉血，若也算「一次伤害」，
 *       屏障会被自己的毒瞬间吃掉，等于白用一张卡；</li>
 *   <li><b>不挡 ②</b>：{@link SelfHarmCost}（蓄血圣杯的血祭、割腕的割腕）——
 *       与 {@code Talent.perseveranceCap}、{@code GritTeethBuff.checkBypass} <b>共用同一判据</b>。
 *       那两笔伤害是「挨完还活着就升级」的<b>代价</b>，被屏障挡掉就等于「一张卡换一次免费升级」，
 *       与项目里既有的「别让保命机制把升级代价抹平」是同一条红线。</li>
 * </ul>
 *
 * <h3>唯一调用点</h3>
 * <p>{@code Hero.damage(int, Object)} 靠前的位置（紧随「时间停滞」「很烫的」两条整段免疫之后）。
 * 放在 {@code Hero.damage} 而不是 {@code Char.damage}：后者是英雄受击流程的<b>下半段</b>，
 * 走到那里时残像震颤、打断行动、神器充能等已经发生过了 —— 屏障要的是「这一下根本没发生」。</p>
 *
 * <p><b>顶层类</b>：随存档序列化并由反射重建，不能做成非静态内部类。</p>
 */
public class HolyBarrier extends Buff {

	{
		//增益。标 POSITIVE 是刻意的：它只影响图标着色与浮字颜色，不会像 NEGATIVE 那样
		//触发宝箱怪的自动暴露逻辑（那条红线针对的是 NEGATIVE）。
		type = buffType.POSITIVE;
		//获得时在角色头顶飘一行名字（Char.java 里 `buff.announced` 分支）
		announced = true;
	}

	//==========================================================================
	// 施加 / 判定
	//==========================================================================

	/** 该单位此刻是否持有屏障。 */
	public static boolean active( Char ch ){
		return ch != null && ch.buff( HolyBarrier.class ) != null;
	}

	/**
	 * 这一击是否应当被屏障挡下 —— <b>本 buff 的唯一判据</b>。
	 *
	 * <p>返回 {@code true} 时调用方必须接着调 {@link #trigger(Char)}（消耗 + 特效）。</p>
	 */
	public static boolean blocks( Char ch, Object src ){

		if (!active( ch )) return false;

		//① 自身 buff 造成的持续伤害（毒 / 燃烧 / 流血 / 腐蚀 / 饥饿…）不消耗屏障
		if (src instanceof Buff) return false;

		//② 「自伤换成长」不消耗屏障（详情见类注释；与 perseveranceCap / GritTeethBuff 同一判据）
		if (src instanceof SelfHarmCost) return false;

		return true;
	}

	/**
	 * 挡下这一击：把屏障用掉，并播「挡下」的浮字 / 粒子 / 音效。
	 *
	 * <p>特效刻意收在这里而不是写在 {@code Hero.damage} 里：让唯一的调用点只需要「判一下 +
	 * 调一下」两行，将来换表现不用动英雄那套受击流程。</p>
	 */
	public static void trigger( Char ch ){

		if (ch == null) return;

		Buff.detach( ch, HolyBarrier.class );

		if (ch.sprite != null) {
			ch.sprite.showStatus( CharSprite.POSITIVE, Messages.get( HolyBarrier.class, "blocked" ) );
			//纯白粒子：与神圣卡「纯白卡牌 / 神圣能量」同一套视觉语言
			ch.sprite.emitter().burst( WhiteSmokeParticle.BURST, 8 );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_PARRY, 1f, Random.Float( 0.96f, 1.05f ) );
	}

	//==========================================================================
	// 显示
	//==========================================================================

	@Override
	public int icon() {
		//专属图标：buffs.png / large_buffs.png 的**帧 117**（用户已绘，紧接「融化」116 的下一位）
		return BuffIndicator.HOLY_BARRIER;
	}
}
