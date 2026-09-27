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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.ringmaster;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.items.weapon.melee.LifeWorkTibia;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * 环指大师（卡利斯托）专属语音的集中入口（2026-09-18）。
 *
 * <p>与拇指的 {@code ValencinaSfx}、中指的 {@code MiddleFingerVoice} 同构：素材、触发条件、去抖都收在这一处。
 * 素材来自边狱公司灰机 wiki「敌方单位/1331 卡利斯托」，统一转码为 44100Hz / 单声道 / 64kbps、
 * 按语音档归一到约 <b>−14 ~ −16 LUFS</b>。</p>
 *
 * <h3>本角色的唯一触发点：英雄击杀</h3>
 * <p>环指大师的技能文本本身很少，所以这六条**全部**做成<b>击杀播报</b>：每次英雄击杀
 * 掷一次 {@value #KILL_CHANCE} 的概率，中了就随机取一条（语音 + 头顶米色台词）。</p>
 *
 * <h3>两条统一规则（与中指/拇指一致，务必保持）</h3>
 * <ol>
 *   <li><b>语音与头顶台词成对</b>：每条都绑死了自己的台词（{@link Line}），取一次同时用
 *       ⇒ 不会出现「显示 A、念的 B」。颜色 {@link #LINE_COLOR}（米色）。</li>
 *   <li><b>上一条没播完就整条跳过</b>（{@link #busy()}）：{@code Sound.play} 是<b>叠加式</b>的，
 *       连杀时不拦就会几条语音糊在一起（击杀播报尤其容易连发：一刀一个）。</li>
 * </ol>
 *
 * <h3>「提比娅的旋律」两条是条件条目</h3>
 * <p>{@link Line#tibiaOnly} 为 true 的两条<b>只在主手装备「一生之作-提比娅」时</b>才进入候选池
 * （{@code hero.belongings.weapon() instanceof LifeWorkTibia}）。判定放在<b>每次击杀</b>时做，
 * 而不是缓存——换武器的当场就会生效。</p>
 *
 * <h3>时长表为什么是硬编码的</h3>
 * <p>{@code Sample} 只把音效整段解码进内存、不提供「还剩多久播完」的查询，所以每条素材的实际时长
 * 在转码后用 {@code ffmpeg -i} 量出来写进 {@link Line#duration}。<b>换素材必须同步这个数</b>
 * （偏大只是多等一会儿、偏小会叠音）。量法：解码成 wav 数采样点，或读 {@code ffmpeg -i} 的 Duration。</p>
 */
public class RingMasterVoice {

	private RingMasterVoice() {}

	//==========================================================================
	// 常量
	//==========================================================================

	/** 台词浮字的颜色：<b>米色</b>（wiki 上角色台词的统一色 `#E5CAA5`，与拇指同色）。 */
	public static final int LINE_COLOR = 0xE5CAA5;

	/** 每次英雄击杀时的播报概率。 */
	public static final float KILL_CHANCE = 0.10f;

	//==========================================================================
	// 台词表（文本 ↔ 语音 ↔ 时长 三者绑死）
	//==========================================================================

	/** 一条「语音 + 头顶台词」。 */
	private static class Line {
		final String text;
		final String sound;
		/** 转码后实测时长（秒），用于「上一条播完没有」。 */
		final float duration;
		/** 仅在装备「一生之作-提比娅」时进入候选池。 */
		final boolean tibiaOnly;

		Line( String text, String sound, float duration ){
			this( text, sound, duration, false );
		}

		Line( String text, String sound, float duration, boolean tibiaOnly ){
			this.text = text;
			this.sound = sound;
			this.duration = duration;
			this.tibiaOnly = tibiaOnly;
		}
	}

	/** 六条击杀播报：前四条常驻、后两条需要提比娅（wiki 上的来源各不相同，我方统一按「击杀」使用）。 */
	private static final Line[] LINES = {
			//—— 常驻四条 ——
			//wiki 原标注：装置艺术第 3 号「简易肋骨」使用时
			new Line( "让我们一起剖析，你身体中残存的美吧？",
					Assets.Sounds.RINGMASTER_KILL_1, 4.60f ),
			//wiki 原标注：对罪人或助战者 我们深爱着血与肉 使用时
			new Line( "观众的积极参与……不错呢！",
					Assets.Sounds.RINGMASTER_KILL_2, 3.81f ),
			//wiki 原标注：闭馆-装置艺术第 1 号「各位四散的肉与骨将化为观众席」使用时
			new Line( "我将为你生动呈现，人体派的艺术！",
					Assets.Sounds.RINGMASTER_KILL_3, 3.34f ),
			//wiki 原标注：提比娅的旋律 拼点失败时 2
			new Line( "你觉得如何？请快点……把你的感想告诉我吧！",
					Assets.Sounds.RINGMASTER_KILL_4, 5.59f ),

			//—— 提比娅的旋律：仅在装备「一生之作-提比娅」时 ——
			new Line( "你听见了吗？那由提比娅的一对肱骨与二十四根肋骨奏响的旋律！",
					Assets.Sounds.RINGMASTER_KILL_TIBIA_1, 6.51f, true ),
			new Line( "静脉和动脉，当你流出的两种不同颜色的血交融之时，啊啊……！真是艺术啊！",
					Assets.Sounds.RINGMASTER_KILL_TIBIA_2, 9.05f, true ),
	};

	//==========================================================================
	// 状态
	//==========================================================================

	/** 上一条语音的开始时刻（{@code Game.timeTotal}，累计秒）。 */
	private static float lastAt = -999f;

	/** 上一条语音的时长（秒）。 */
	private static float lastDuration = 0f;

	/** 这些语音只在环指大师身上播。 */
	public static boolean isRingMaster(){
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.RING_MASTER;
	}

	/**
	 * 上一条语音是否还没播完。
	 *
	 * <p>时间戳必须用 {@code Game.timeTotal}（累计秒），<b>不能用 {@code Game.elapsed}</b>——
	 * 后者在 {@code Game.java} 里是每帧增量（约 0.016 秒），当时间戳用差值恒为负、去抖形同失效。
	 * {@code Game.timeTotal > lastAt} 这半条件用于应对游戏重启（{@code timeTotal} 归零、
	 * 而静态字段仍持旧值 ⇒ 差值为负）时误判成「正在播放」而掐掉第一条。</p>
	 */
	private static boolean busy(){
		return lastAt > 0f
				&& Game.timeTotal > lastAt
				&& Game.timeTotal - lastAt < lastDuration;
	}

	/**
	 * 播一条：语音 + 头顶台词同时出；上一条没播完则<b>整条跳过</b>。
	 * <p>所有播放口最终都走这里，去抖与配色只写一遍。</p>
	 */
	private static void play( Line line ){
		if ( line == null || !isRingMaster() ) return;
		if ( busy() ) return;

		lastAt = Game.timeTotal;
		lastDuration = line.duration;

		Sample.INSTANCE.play( line.sound );

		Hero hero = Dungeon.hero;
		if ( hero != null && hero.sprite != null ){
			hero.sprite.showStatus( LINE_COLOR, line.text );
		}
	}

	//==========================================================================
	// 播放口
	//==========================================================================

	/**
	 * 英雄击杀敌人时的播报口（在 {@code Mob.die} 的英雄击杀分支里调用）。
	 *
	 * <p>顺序：先 {@value #KILL_CHANCE} 概率 → 再「上一条还在播就跳过」→ 最后随机取一条。
	 * 概率放在前面是为了省掉「没中还要筛候选池」的无谓开销，两者独立、不影响实际听感概率。</p>
	 */
	public static void onEnemyKilled( Hero hero ){
		if ( hero == null || hero.heroClass != HeroClass.RING_MASTER ) return;
		if ( Random.Float() >= KILL_CHANCE ) return;
		play( pick( hero ) );
	}

	/** 按当前主手武器筛出候选池（提比娅专属两条只在装备它时才参与随机）。 */
	private static Line pick( Hero hero ){
		final boolean tibia = hero.belongings.weapon() instanceof LifeWorkTibia;

		Line[] pool = new Line[LINES.length];
		int n = 0;
		for (Line line : LINES){
			if ( !line.tibiaOnly || tibia ) pool[n++] = line;
		}
		if ( n == 0 ) return null;

		return pool[Random.Int( n )];
	}
}
