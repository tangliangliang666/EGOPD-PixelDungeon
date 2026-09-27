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

package com.shatteredpixel.shatteredpixeldungeon.actors.hero.abilities.middlefinger;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.Hero;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.watabou.noosa.Game;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Random;

/**
 * 中指·长兄专属语音的集中入口（2026-09-18）。
 *
 * <p>与拇指的 {@code ValencinaSfx} 同构：素材、触发条件、去抖都收在这一处，调用方只管调一句。
 * 素材来自边狱公司灰机 wiki「敌方单位/1327 中指父辈 - 马蒂亚斯」，
 * 统一转码为 44100Hz / 单声道 / 64kbps、按语音档归一到约 <b>−14 ~ −16 LUFS</b>。</p>
 *
 * <h3>两条统一规则（用户要求，务必保持）</h3>
 * <ol>
 *   <li><b>语音与头顶台词成对</b>：每一条语音都绑死了自己的台词（{@link Line}），
 *       播放时同时在角色头顶弹出该文本，颜色 {@link #LINE_COLOR}（淡紫）。
 *       切勿把文本与语音拆成两个独立随机的表——那会出现「显示 A、念的却是 B」。</li>
 *   <li><b>上一条没播完就整条跳过</b>（{@link #busy()}）：语音+台词一起不播。
 *       游戏是叠加式播放（{@code Sound.play} 不会打断上一条），连击 / 连续受击时不拦就会糊成一团。</li>
 * </ol>
 *
 * <h3>时长表为什么是硬编码的</h3>
 * <p>{@code Sample} 只把音效整段解码进内存、并不对外提供「还剩多久播完」的查询，
 * 所以每条素材的实际时长在转码后用 {@code ffmpeg -i} 量出来写进 {@link Line#duration}。
 * <b>换素材必须同步这个数</b>，否则「播完没有」判错（偏大只是多等一会儿、偏小会叠音）。
 * 量法：{@code ffmpeg -hide_banner -i x.mp3}（读 Duration）。</p>
 *
 * <h3>触发点一览</h3>
 * <table>
 *   <tr><th>方法</th><th>台词</th><th>调用点</th></tr>
 *   <tr><td>{@link #playUnseal(int)}</td><td>好吧，这种程度的话该撕掉一层包装纸了！ / 有意思……特色是这种手感啊！ /
 *       竟然让我解放到这种地步……哈！</td><td>{@code SealedSwordBase.unsealInto}（封印之剑推进形态）</td></tr>
 *   <tr><td>{@link #playCounter()}</td><td>给我过来！</td>
 *       <td>{@code VengeanceArts.useMoveOnTarget}（忠义巡礼者使用复仇技艺）</td></tr>
 *   <tr><td>{@link #playLedgerFull()}</td><td>哈啊……看来没有打开账簿的必要了。</td>
 *       <td>{@code RevengeLedger.ledgerRecharge.act}（账簿满充能的回合开始）</td></tr>
 *   <tr><td>{@link #playHitTaken()}</td><td>你这……！</td>
 *       <td>{@code Hero.damage}（受击，{@value #HIT_CHANCE} 概率）</td></tr>
 *   <tr><td>{@link #playExecution()}</td><td>正中靶心啊！</td>
 *       <td>{@code InstantExecution.Execution} 第 5 拍（盔甲技能连段收尾）</td></tr>
 *   <tr><td>{@link #playStrike()}</td><td>（无台词，纯打击音）</td>
 *       <td>{@code VengeanceArts} 五式复仇技艺命中时</td></tr>
 * </table>
 */
public class MiddleFingerVoice {

	private MiddleFingerVoice() {}

	//==========================================================================
	// 常量
	//==========================================================================

	/** 台词浮字的颜色：<b>淡紫</b>（中指家系的主色）。 */
	public static final int LINE_COLOR = 0xCBA6F7;

	/** 受击语音的触发概率（每次真的被角色打中时掷一次）。 */
	public static final float HIT_CHANCE = 0.15f;

	//==========================================================================
	// 台词表（文本 ↔ 语音 ↔ 时长 三者绑死）
	//==========================================================================

	/** 一条「语音 + 头顶台词」。 */
	private static class Line {
		final String text;
		final String sound;
		/** 转码后实测时长（秒），用于「上一条播完没有」。 */
		final float duration;

		Line( String text, String sound, float duration ){
			this.text = text;
			this.sound = sound;
			this.duration = duration;
		}
	}

	/**
	 * 封印之剑的四段形态各有一句：下标 = 解封后所处形态在 {@code SealedSwordBase.STAGES} 里的序号 − 1
	 * （封印 → 一阶段 → 二阶段 → 莱瓦汀）。
	 * <p>只有<b>推进到新形态那一步</b>会播，落回封印（主副切换）与「即刻处刑」的强制换装都不算。</p>
	 */
	private static final Line[] UNSEAL = {
			new Line( "好吧，这种程度的话该撕掉一层包装纸了！", Assets.Sounds.MIDDLEFINGER_UNSEAL_1, 4.37f ),
			new Line( "有意思……特色是这种手感啊！",               Assets.Sounds.MIDDLEFINGER_UNSEAL_2, 3.49f ),
			new Line( "竟然让我解放到这种地步……哈！",             Assets.Sounds.MIDDLEFINGER_UNSEAL_3, 3.42f ),
	};

	/** 复仇技艺（忠义巡礼者）：被记仇的敌人出手时的一句「犯规！！」。 */
	private static final Line COUNTER = new Line( "给我过来！", Assets.Sounds.MIDDLEFINGER_COUNTER, 2.09f );

	/** 复仇账簿攒满那一刻的回合开始。 */
	private static final Line LEDGER_FULL =
			new Line( "哈啊……看来没有打开账簿的必要了。", Assets.Sounds.MIDDLEFINGER_LEDGER_FULL, 4.92f );

	/** 受击（概率触发）。 */
	private static final Line HIT = new Line( "你这……！", Assets.Sounds.MIDDLEFINGER_HIT, 1.45f );

	/** 盔甲技能「即刻处刑[莱瓦汀]」连段收尾。 */
	private static final Line EXECUTION = new Line( "正中靶心啊！", Assets.Sounds.MIDDLEFINGER_EXECUTION, 1.16f );

	//==========================================================================
	// 攻击音效（不是语音：没有台词、也不参与语音去抖）
	//==========================================================================

	/**
	 * 五式复仇技艺命中时的攻击音效（wiki「技能0」列表的第 1、2、5 条，随机取一）。
	 *
	 * <p><b>为什么单独一条通道</b>：语音的去抖窗口是 1~5 秒（上一条没播完就跳过），
	 * 若打击音共用它，刚喊完「给我过来！」之后的几秒里打击音全都会被吞掉。
	 * 三者互不干扰：语音照旧只受自己的时长约束，打击音每次命中都响。</p>
	 */
	private static final String[] STRIKE = {
			Assets.Sounds.MIDDLEFINGER_STRIKE_1,
			Assets.Sounds.MIDDLEFINGER_STRIKE_2,
			Assets.Sounds.MIDDLEFINGER_STRIKE_3,
	};

	//==========================================================================
	// 状态
	//==========================================================================

	/** 上一条语音的开始时刻（{@code Game.timeTotal}，累计秒）。 */
	private static float lastAt = -999f;

	/** 上一条语音的时长（秒）。 */
	private static float lastDuration = 0f;

	/** 这些语音只在中指长兄身上播（受击那条挂在通用的 {@code Hero.damage} 上，必须自己认人）。 */
	public static boolean isMiddleFinger(){
		return Dungeon.hero != null && Dungeon.hero.heroClass == HeroClass.MIDDLE_FINGER;
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
	 * <p>所有 {@code playXxx} 最终都走这里，去抖与配色只写一遍。</p>
	 */
	private static void play( Line line ){
		if ( line == null || !isMiddleFinger() ) return;
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
	 * 解封推进到第 {@code newStage} 形态时的台词（{@code 1} = 一阶段、{@code 2} = 二阶段、
	 * {@code 3} = 莱瓦汀；0 与越界一律不播）。
	 */
	public static void playUnseal( int newStage ){
		int idx = newStage - 1;
		if ( idx < 0 || idx >= UNSEAL.length ) return;
		play( UNSEAL[idx] );
	}

	/** 忠义巡礼者使用复仇技艺。 */
	public static void playCounter(){
		play( COUNTER );
	}

	/** 复仇账簿达到最大充能（每攒满一次只播一遍，见 {@code RevengeLedger}）。 */
	public static void playLedgerFull(){
		play( LEDGER_FULL );
	}

	/** 受击（{@value #HIT_CHANCE} 概率），只在真的被角色打中时调用。 */
	public static void playHitTaken(){
		if ( Random.Float() >= HIT_CHANCE ) return;
		play( HIT );
	}

	/** 盔甲技能「即刻处刑[莱瓦汀]」连段收尾（技能演完才播，不是按下技能时）。 */
	public static void playExecution(){
		play( EXECUTION );
	}

	/**
	 * 五式复仇技艺命中时的攻击音效（三条随机取一），<b>不</b>显示台词、<b>不</b>受语音去抖约束。
	 *
	 * <p>调用点在 {@code VengeanceArts} 的各式结算里，取代原来的原版
	 * {@code Assets.Sounds.HIT_STRONG}；踏碎（第 1 式）原本一声不响，本次一并补上。</p>
	 */
	public static void playStrike(){
		if ( !isMiddleFinger() ) return;
		Sample.INSTANCE.play( Random.element( STRIKE ) );
	}
}
