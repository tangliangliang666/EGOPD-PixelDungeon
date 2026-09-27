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

package com.shatteredpixel.shatteredpixeldungeon.sprites;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.MeltingLove;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.tweeners.Tweener;

/**
 * 爱慕神器的召唤物「溶解之爱」的精灵。
 *
 * <p><b>两张贴图、三种形态</b>：</p>
 * <ul>
 *   <li>{@code lovenew.png}（160×144，帧 <b>16×24</b>，10 列 × 6 行）：每 <b>2 行一组</b>共 20 帧，
 *       组内只用前 15 帧 —— 1 默认 / 2 待机扭头 / 3 移动 / 4-6 攻击 / 7-10 死亡 /
 *       11 尴尬 / 12 快乐 / 13 焦虑 / 14-15 黑化。共<b>三组</b>：第一组 = 行 0-1（索引 0-14）、
 *       第二组 = 行 2-3（索引 20-34）、第三组 = 行 4-5（索引 40-54）；
 *       赠予嬗变卷轴会在这三组之间<b>随机换到另外一组</b>（不会原地不动，见 {@code Admiration.giveToLove}）。</li>
 *   <li>{@code loveanger.png}（128×32，帧 <b>20×24</b>，单行 6 帧）：反噬形态 ——
 *       1 默认 / 2 移动 / 3-4 攻击 / 5-6 死亡。由"焦虑倒计时结束 → 黑化动作 → 切换本贴图"接入。</li>
 * </ul>
 *
 * <p><b>动作配置</b>参考监狱守卫（{@link GuardSprite}）等标准怪物：待机为一循环内回头数次
 * （旧配置 12 帧才回头一次、频率过低），移动/攻击/死亡各自独立成段。</p>
 *
 * <p><b>朝向 与「停止即回待机」</b>：引擎 {@link CharSprite#turnTo(int, int)} 只改
 * {@code flipHorizontal}，而 {@code Image} 仅在被调用 {@code frame()} 时才把翻转写进顶点
 * （见 {@code Image.updateFrame}）；若移动动画只有一帧、循环中帧索引不变，{@code frame()}
 * 就永远不会被调用 → 朝向看起来"卡住不转"。因此本精灵：① 移动帧写成两帧（索引在 0/1 间
 * 切换，从而周期性刷新顶点）；② 覆写 {@link #move(int, int)} 强制重播一次 run，让翻转立即生效；
 * ③ 覆写 {@link #onComplete(Tweener)}，移动补间结束时回到待机默认帧（标准怪物的 run→idle
 * 由 {@code Hero.ready()} 一类逻辑负责，怪物侧没有，故在此自行收尾）。</p>
 *
 * <p><b>形态来源</b>：图像组与"是否已反噬"都保存在 {@link MeltingLove} 上（随存档持久化），
 * 精灵只按 {@link #link(Char)} 时读到的状态重建 —— 因此死亡后重新召唤也会保持玩家之前
 * 用嬗变卷轴切换到的图像组。</p>
 */
public class MeltingLoveSprite extends MobSprite {

	//========== 图像组（lovenew.png） ==========

	/** 第一组：行 0-1（索引 0-14）。默认使用。 */
	public static final int PALETTE_A = 0;
	/** 第二组：行 2-3（索引 20-34）。赠予嬗变卷轴后切换。 */
	public static final int PALETTE_B = 1;
	/** 第三组：行 4-5（索引 40-54）。2026-09-12 用户追加绘制。 */
	public static final int PALETTE_C = 2;

	/** 图像组总数（嬗变卷轴在 {@code 0 ~ PALETTE_COUNT-1} 之间随机切换）。 */
	public static final int PALETTE_COUNT = 3;

	private static final int LOVENEW_COLS = 10;                 //lovenew 每行 10 帧
	private static final int LOVENEW_GROUP_STEP = LOVENEW_COLS * 2;  //每组 2 行 = 20 帧

	//组内偏移（0 基，对应设计文档里的 1 基帧号 -1）
	private static final int F_IDLE        = 0;   //默认
	private static final int F_LOOK        = 1;   //待机扭头
	private static final int F_MOVE        = 2;   //移动
	private static final int F_ATTACK      = 3;   //攻击第 1 帧（后两帧顺延）
	private static final int F_DIE         = 6;   //死亡第 1 帧（后三帧顺延）
	private static final int F_EMBARRASSED = 10;  //尴尬
	private static final int F_HAPPY       = 11;  //快乐
	private static final int F_ANXIOUS     = 12;  //焦虑
	private static final int F_DARKEN      = 13;  //黑化第 1 帧（第二帧顺延）

	//========== 表情 ==========

	public static final int EMO_EMBARRASSED = 0;
	public static final int EMO_HAPPY       = 1;

	//一次性表情：4 fps × 8 帧 = 2 秒定格，播完自动回待机
	private static final int EMO_FPS    = 4;
	private static final int EMO_FRAMES = 8;

	private Animation emoEmbarrassed;   //尴尬（不满）
	private Animation emoHappy;         //快乐（喜欢）
	private Animation emoAnxious;       //焦虑（持续表情，循环播放）
	private Animation darken;           //黑化（转入反噬形态前的两帧过渡）

	//========== 状态 ==========

	private int palette = PALETTE_A;
	private boolean angry = false;      //已切换到 loveanger.png
	private boolean anxious = false;    //正在播放"焦虑"持续表情

	public MeltingLoveSprite() {
		super();
		configureLove();
	}

	//========== 动作配置 ==========

	/** 按当前图像组重建 lovenew.png 的全部动画（同时清除"反噬"标记）。 */
	private void configureLove() {
		angry = false;
		texture( Assets.Sprites.LOVENEW );
		TextureFilm frames = new TextureFilm( texture, 16, 24 );

		int b = palette * LOVENEW_GROUP_STEP;

		//待机：帧 1 为主，循环内回头两次（参考 GuardSprite 的 0,0,0,1,0,0,1,1）
		idle = new Animation( 2, true );
		idle.frames( frames,
				b + F_IDLE, b + F_IDLE, b + F_IDLE, b + F_LOOK,
				b + F_IDLE, b + F_IDLE, b + F_LOOK, b + F_LOOK );

		//移动：本组只有一张移动姿势，写成两帧（索引在 0↔1 间切换）以保证帧索引会变化 ——
		//否则 MovieClip.updateAnimation() 不会调用 frame()，flipHorizontal 的翻转无法写进顶点，
		//表现就是"移动时不转身"。两帧同一姿势，视觉上仍是单帧移动。
		run = new Animation( 10, true );
		run.frames( frames, b + F_MOVE, b + F_MOVE );

		attack = new Animation( 12, false );
		attack.frames( frames, b + F_ATTACK, b + F_ATTACK + 1, b + F_ATTACK + 2 );

		die = new Animation( 8, false );
		die.frames( frames, b + F_DIE, b + F_DIE + 1, b + F_DIE + 2, b + F_DIE + 3 );

		emoEmbarrassed = hold( frames, b + F_EMBARRASSED );
		emoHappy = hold( frames, b + F_HAPPY );

		//焦虑是"持续表情"：循环播放同一帧，直到 clearAnxious()
		emoAnxious = new Animation( 2, true );
		emoAnxious.frames( frames, b + F_ANXIOUS );

		darken = new Animation( 5, false );
		darken.frames( frames, b + F_DARKEN, b + F_DARKEN + 1 );

		play( idle );
	}

	/** 反噬形态（loveanger.png，帧 20×24，单行 6 帧）。 */
	private void configureAnger() {
		angry = true;
		anxious = false;
		texture( Assets.Sprites.LOVEANGER );
		TextureFilm frames = new TextureFilm( texture, 20, 24 );

		idle = new Animation( 2, true );
		idle.frames( frames, 0, 0, 0, 0 );

		run = new Animation( 10, true );
		run.frames( frames, 1, 1 );   //同上：两帧以保证移动时帧索引变化、朝向能刷新

		attack = new Animation( 12, false );
		attack.frames( frames, 2, 3 );

		die = new Animation( 8, false );
		die.frames( frames, 4, 5 );

		//反噬形态没有表情与黑化帧
		emoEmbarrassed = null;
		emoHappy = null;
		emoAnxious = null;
		darken = null;

		play( idle );

		//贴图宽度 16 → 20，需按新宽度重新对齐格子（移动中不动，交给引擎下一个格子处理）
		if (ch != null && motion == null) {
			place( ch.pos );
		}
	}

	/** 定格式表情动画：同一帧重复 {@link #EMO_FRAMES} 次，播完自动回待机。 */
	private static Animation hold( TextureFilm frames, int frame ) {
		Animation anim = new Animation( EMO_FPS, false );
		Object[] seq = new Object[EMO_FRAMES];
		for (int i = 0; i < EMO_FRAMES; i++) {
			seq[i] = frame;
		}
		anim.frames( frames, seq );
		return anim;
	}

	//========== 对外接口（由 MeltingLove 调用） ==========

	/**
	 * 设置图像组；已反噬则忽略。
	 * <p>参数归一化到 {@code 0 ~ }{@link #PALETTE_COUNT}{@code -1}：越界或旧档里的非法值
	 * 一律落回 {@link #PALETTE_A}，避免 {@link #configureLove()} 取到贴图外的帧。</p>
	 */
	public void setPalette( int palette ) {
		if (angry) return;
		int p = normalizePalette( palette );
		if (p == this.palette) return;
		this.palette = p;
		configureLove();
	}

	/**
	 * 把任意输入归一化到合法的图像组编号 {@code 0 ~ }{@link #PALETTE_COUNT}{@code -1}；
	 * 越界（含旧存档的异常值）一律落回 {@link #PALETTE_A}，避免取到贴图外的帧。
	 */
	public static int normalizePalette( int palette ) {
		return (palette >= PALETTE_A && palette < PALETTE_COUNT) ? palette : PALETTE_A;
	}

	public int palette() {
		return palette;
	}

	/** 播放一次性表情（尴尬 / 快乐）。 */
	public void showEmote( int emote ) {
		if (angry) return;
		Animation anim = emote == EMO_HAPPY ? emoHappy : emoEmbarrassed;
		if (anim != null) {
			play( anim );
		}
	}

	/**
	 * 进入"焦虑"持续表情（摘下爱慕神器、反噬倒计时开始）。
	 * <p>可重复调用（倒计时每回合都会重申一次状态）：已在焦虑中或正在移动时只置标志，
	 * 移动结束后由 {@link #idle()} 自动接上焦虑帧。</p>
	 */
	public void beginAnxious() {
		if (angry || emoAnxious == null) return;
		anxious = true;
		if (curAnim != run) {
			play( emoAnxious );
		}
	}

	/** 结束"焦虑"持续表情（重新装备爱慕 / 倒计时因其它原因取消）。 */
	public void clearAnxious() {
		anxious = false;
		if (curAnim == emoAnxious) {
			play( idle );
		}
	}

	/** 焦虑倒计时结束：先播黑化两帧，播完自动换成 {@code loveanger.png} 的反噬形态。 */
	public void darkenThenAnger() {
		if (angry) return;
		if (darken != null) {
			play( darken );
		} else {
			configureAnger();
		}
	}

	//========== 生命周期 ==========

	@Override
	public void link( Char ch ) {
		super.link( ch );
		//精灵可能在任意时刻被重建（读档、场景重载），形态一律以怪物身上的持久化状态为准
		if (ch instanceof MeltingLove) {
			MeltingLove love = (MeltingLove) ch;
			palette = normalizePalette( love.palette() );
			if (love.isAngry()) {
				configureAnger();
			} else {
				configureLove();
			}
		}
	}

	@Override
	public void move( int from, int to ) {
		super.move( from, to );
		//引擎在 CharSprite.move() 里是「先 turnTo() 再 play(run)」：若 run 已是当前动画
		//（正在循环），play() 会直接早退 → 不调用 frame() → 刚设置的翻转不生效。
		//这里强制重播一次 run（force=true），强制 Image.frame() → updateFrame() 把朝向写进顶点。
		if (curAnim == run) {
			play( run, true );
		}
	}

	@Override
	public void idle() {
		//焦虑期间待机也用焦虑帧，避免被移动/攻击后的常规 idle() 冲掉
		if (anxious && emoAnxious != null) {
			play( emoAnxious );
		} else {
			super.idle();
		}
	}

	@Override
	public void onComplete( Tweener tweener ) {
		//移动补间结束 → 立刻回到默认待机（焦虑中由 idle() 换焦虑帧；黑化中会被 play() 拦下）。
		//必须在 super 之前调用：super 里的 notifyAll() 会唤醒行为线程，可能马上发起下一次 move()，
		//若放到 super 之后再 idle()，就会把新一轮的 run 覆盖掉。
		//死亡时（isAlive()==false）不能 idle，否则会打断 die 动画、丢掉 MobSprite 的淡出回调。
		if (tweener == motion && ch != null && ch.isAlive()) {
			idle();
		}
		super.onComplete( tweener );
	}

	@Override
	public void play( Animation anim ) {
		//黑化动作播放期间不被打断（死亡动画优先，否则会丢失 MobSprite 的淡出回调）
		if (curAnim == darken && !finished && anim != die) {
			return;
		}
		super.play( anim );
	}

	@Override
	public synchronized void onComplete( Animation anim ) {
		if (anim == darken && !angry) {
			configureAnger();
		} else if ((anim == emoEmbarrassed || anim == emoHappy)
				&& curAnim == anim && !anxious) {
			idle();
		}
		super.onComplete( anim );
	}

	@Override
	public int blood() {
		return 0xFFFFA0B8;   //粉红血
	}

}
