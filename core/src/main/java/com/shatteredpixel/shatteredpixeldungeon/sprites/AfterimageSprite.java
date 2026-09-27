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

import com.watabou.gltextures.SmartTexture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.utils.Random;
import com.watabou.utils.RectF;

/**
 * 「残像纠缠」的一个残像：角色**当前状态的时间平移复制体**（位置/朝向/帧/纹理都按延迟回放）。
 *
 * <p><b>跟随方式＝轨迹延迟回放，不是缓动追赶</b>。{@link Trail} 每帧记录一次角色的真实状态，
 * 第 i 个残像只是去读「{(i+1) ×}{@link #DELAY_STEP}{@code } 秒之前」的那份记录。因此残像的
 * <b>起步时刻、位移速度、停止时刻都与角色完全一致</b>，只是整体晚了一小段时间——这正是需求要的
 * 「完全复刻角色的移动轨迹和时机并延迟移动」。（缓动追赶做不到这点：残像会一直处在「慢慢靠近」
 * 的过程里，速度与节奏都跟角色对不上。）</p>
 *
 * <p><b>残像个数＝buff 层数</b>（{@code Afterimage.stacks}，1~{@link #MAX_COUNT}）。无论几个，
 * 它们都<b>共用一份轨迹</b>（{@link Trail} 由 {@code Afterimage} buff 创建并传给每个残像），
 * 差别只在 {@code delay} 与色调（{@link #TINTS} 的前 N 个：紫红 → 蓝紫 → 蓝 → 深蓝）。</p>
 *
 * <p><b>追上时的震荡</b>：回放位置与角色当前位置重合（角色停下、残像走完自己那一段尾巴）时，
 * 触发一次幅度 {@link #WOBBLE_AMPLITUDE} 像素、{@link #WOBBLE_TIME} 秒内线性衰减的摇摆——
 * 看起来就是「追上来之后轻轻抖了一下再贴合」。各残像延迟不同，触发自然错开。</p>
 *
 * <p><b>受击冲击</b>：{@code Afterimage.flinch(...)} 调 {@link #flinch(float, float)} 时，
 * 残像被朝受击方向<b>猛地甩开</b> {@link #FLINCH_DISTANCE} 像素（外层按
 * {@link #FLINCH_LAYER_SCALE} 逐层略远），随后在 {@link #FLINCH_TIME} 秒内
 * 「甩出 → 越过平衡点 → 反向摆一下 → 归位」（阻尼余弦），也就是需求要的「受击导致残像偏移」。
 * 每个残像的甩出角度按自身相位偏转 ±12°，看起来是被震散而不是整体平移。</p>
 *
 * <p><b>受击也是分层的</b>：第 i 个残像要等 {@code i × }{@link #FLINCH_LAYER_DELAY} 秒才起步，
 * 所以一次挨打在视觉上是「冲击波从角色身上一层层往外传」，而不是四个残像同时闪一下
 * ——与位移回放用 {@link #DELAY_STEP} 分层的思路一致。</p>
 *
 * <p><b>稳定偏移</b>：{@link #JITTER} 是始终叠加的 1 像素缓慢漂移（需求既有的「不稳定」表现），
 * 让角色静止、残像完全重合时仍能看见重影在游移。</p>
 *
 * <p><b>两个必须知道的实现要点</b>：</p>
 * <ul>
 *   <li>{@code Image} 只在 {@code frame(...)} 被调用时才把 {@code flipHorizontal} 写进顶点 UV
 *       （见 {@code Image.updateFrame()}），所以这里<b>每帧都调 {@code frame()}</b>。好在该方法
 *       复用顶点数组、只置 dirty 标记，开销极低。</li>
 *   <li><b>纹理必须先于帧矩形设置</b>：帧矩形是 UV，{@code Image.frame()} 要用
 *       {@code frame.width() * texture.width} 换算显示尺寸。轨迹里把「纹理 + 帧」作为同一份样本
 *       一起存下，就是为了二者永远配套（换装换了贴图册也不会错位）。</li>
 * </ul>
 *
 * <p>挂载层由 {@code Afterimage} buff 指定为 {@code GameScene.floorEmittersAdd(...)}——该层
 * 在 mobs 层之前添加 ⇒ <b>渲染在角色贴图之下</b>。</p>
 */
public class AfterimageSprite extends Image {

	/**
	 * 残像个数上限（＝ {@link #TINTS} 的长度）。
	 *
	 * <p><b>实际个数由 buff 层数决定</b>（{@code Afterimage.stacks}，1~4 层 ⇒ 1~4 个残像），
	 * 本常量只是上限兼取色表长度。层数不足时取 {@link #TINTS} 的<b>前 N 个</b>颜色，
	 * 也就是「最近的残像先出现」，最远的那几个（蓝、深蓝）随层数增长才浮现。</p>
	 */
	public static final int MAX_COUNT = 4;

	/**
	 * 相邻两个残像之间的间隔（秒）：第 i 个残像比角色晚 {@code (i+1) × 本值}。
	 *
	 * <p><b>0.07 是怎么定的</b>：角色走一格耗时 {@code CharSprite.DEFAULT_MOVE_INTERVAL = 0.1f} 秒、
	 * 一格 16 像素 ⇒ 约 160 像素/秒。本值直接换算成「相邻残像之间的像素间距」：0.07 秒 ≈ 11 像素
	 * （近四分之三个身位，四个残像分得清清楚楚），全部延迟 4×0.07 = 0.28 秒 ≈ 45 像素 ≈ 2.8 格
	 * ——连续走动时拖尾最长约 2.8 格，这个幅度已确认是可接受的观感。</p>
	 *
	 * <p>调小手感更「紧」、但四个残像会糊成一团；调大则更分明、拖尾更长。</p>
	 */
	public static final float DELAY_STEP = 0.07f;

	/** 四个残像的色调（延迟由近及远）：紫红 → 蓝紫 → 蓝 → 深蓝。 */
	public static final int[] TINTS = { 0xD957B0, 0x93A7FF, 0x4A90FF, 0x2743D8 };

	/** 染色强度（{@code Visual.tint} 的 strength：越大越接近纯色、整体越亮）。 */
	public static final float TINT_STRENGTH = 0.5f;

	/**
	 * 整体透明度（四个残像一致）。
	 *
	 * <p>0.6 让残像「看得出形状」，静止时四个叠在同处也能分出层次；越接近 1 越像实体、
	 * 越失去残影质感。这是用户实机对照后调高的一档（原 0.45 偏虚）。</p>
	 */
	public static final float ALPHA = 0.6f;

	/**
	 * 漂移幅度（像素）。需求要求「1 个像素之内」。
	 *
	 * <p>位置公式是两个系数 0.7 / 0.3 的正弦之和，系数和恰为 1 ⇒ 瞬时偏移上限就是这个值本身。
	 * 曾试过 1.5f，实机对照后用户要求回调，故回到 1f：「不稳定」靠持续游移表现即可，
	 * 幅度再大就会被看成「残像在乱跑」而不是「浮在身上的重影」。</p>
	 */
	public static final float JITTER = 1f;

	/**
	 * 受击冲击：残像被甩开的距离（像素）、回弹总时长（秒）与振荡角频率（弧度/秒）。
	 *
	 * <p><b>观感目标＝「被撞得晃一大下、慢慢收回来」，不是「Q 弹地抖两抖」</b>。
	 * 最初的 0.35 秒 / 20 弧度每秒，周期数（{@code 0.35 × 20 = 7} 弧度 ÷ 2π ≈ 1.11）看着并不夸张，
	 * 问题在于<b>整个动作被压缩在三分之一秒里</b>：从最大偏移回到平衡点这最显眼的一半只花
	 * {@code π/(2×20) ≈ 0.08} 秒（60fps 下不到 5 帧），眨眼就过；幅度又只有 7 像素
	 * （不到半个身位，「打飞」的语义立不住）。两件事叠加，落在眼里就只剩「颤了一下」——
	 * 这就是「Q 弹」的来源。</p>
	 *
	 * <p>现在把<b>时间与幅度一起放大</b>：{@link #FLINCH_DISTANCE} 7 → 13 像素（约一个身位）、
	 * {@link #FLINCH_TIME} 0.35 → 0.8 秒，最显眼的那一半随之拉长到
	 * {@code π/(2×9) ≈ 0.17} 秒（约 10 帧，看得清「被甩出去」）。</p>
	 *
	 * <p><b>频率必须跟着从 20 降到 9，这两项是一起改的</b>：余弦的极值点出现在
	 * {@code age × FREQ = kπ}，个数 ＝ {@code ⌊时长 × 频率 ÷ π⌋ + 1}。只拉长时间而保持 20，
	 * 则 {@code 0.8 × 20 = 16} 弧度 ⇒ 极值点 6 个（三组来回），反而更像弹簧。
	 * 取 {@code 0.8 × 9 = 7.2} 弧度 ⇒ 极值点仍是 3 个（与旧值同量级），但第 3 个落在时长的 87% 处、
	 * 线性阻尼只剩 0.13 ⇒ 仅约 1.7 像素、实际看不见，于是观感是干净的
	 * 「甩出 → 反向大摆一次 → 归位」。反向峰值出现在 {@code π/9 ≈ 0.35} 秒处，此时阻尼还剩 0.56
	 * ⇒ 反向能摆出 7 像素以上，回摆清晰可见，不会像原来那样「刚离开平衡点就被抹平」。</p>
	 *
	 * <p>阻尼一律是<b>线性衰减</b>（剩余时长 ÷ 总时长）乘在余弦上 ⇒ 幅度随时间收窄、
	 * 时长耗尽时必然归零，不会出现「时间到了还歪着」的突跳。</p>
	 */
	public static final float FLINCH_DISTANCE = 13f;
	public static final float FLINCH_TIME = 0.8f;
	public static final float FLINCH_FREQ = 9f;

	/**
	 * 受击冲击的<b>分层延迟</b>（秒/层）：第 i 个残像（由近及远，i 从 0 起）比最内层晚
	 * {@code i × 本值} 秒才开始甩出。
	 *
	 * <p>于是同一次受击表现为「冲击波从角色身上一层层向外传导」：最贴着角色的那个残像先被撞飞，
	 * 外层依次跟上——而不是四个残像齐刷刷地闪一下。多一层就看得出差别，这正是分层延迟要的效果。</p>
	 *
	 * <p><b>本值必须大于移动延迟 {@link #DELAY_STEP}（0.09 &gt; 0.07）</b>：否则传导节奏会和
	 * 走路的拖尾节奏撞在一起，看起来只像是又一次错帧，而不是「挨打传出去的一串」。</p>
	 *
	 * <p>四层时最外层晚 {@code 3 × 0.09 = 0.27} 秒起步，加上 {@link #FLINCH_TIME} 的回弹，
	 * 整串动作约 1.07 秒——慢到能看清每一层的先后，又不至于拖到下一次挨打还没收完。</p>
	 */
	public static final float FLINCH_LAYER_DELAY = 0.09f;

	/**
	 * 受击冲击的<b>分层幅度递进</b>：第 i 个残像的甩出距离 ＝
	 * {@link #FLINCH_DISTANCE} × (1 + i × 本值)。
	 *
	 * <p>外侧残像甩得更远，冲击波才有向外「扩散」的空间感；否则四层只是同一位置的四次错帧复制，
	 * 分层只剩下时间差。每层 +12%，四层分别甩 13 / 14.6 / 16.1 / 17.7 像素，彼此相差半格以内，
	 * 不会散得看不出是一组残像。</p>
	 */
	public static final float FLINCH_LAYER_SCALE = 0.12f;

	/** 「已追上角色」的判定距离（像素）：回放位置与角色当前位置的间距小于它就算追上。 */
	public static final float CAUGHT_RANGE = 0.5f;

	/** 追上时摇摆的初始幅度（像素）与持续时间（秒）。 */
	public static final float WOBBLE_AMPLITUDE = 1.8f;
	public static final float WOBBLE_TIME = 0.5f;

	/** 摇摆的角频率（弧度/秒）。 */
	public static final float WOBBLE_FREQ = 26f;

	private final CharSprite target;
	private final Trail trail;

	/** 本残像的延迟时长（秒），＝(序号+1) × {@link #DELAY_STEP}。 */
	private final float delay;

	/** 本残像的序号（0 起，0＝最贴近角色的那个）。受击冲击的分层延迟与幅度递进都按它算。 */
	private final int rank;

	/** 漂移与摇摆的相位：每个残像不同，避免四个同步抖动。 */
	private final float phase = Random.Float( 6.2832f );

	/** 上一帧是否处于「已追上」状态：用于只在「刚追上」的瞬间触发一次摇摆。 */
	private boolean caughtUp = true;

	/** 摇摆剩余时长（秒），>0 表示正在震荡。 */
	private float wobble = 0f;

	/** 受击冲击：甩出方向（单位向量）与本次甩出的幅度（像素，按层递进）。 */
	private float impX = 0f;
	private float impY = 0f;
	private float kickAmp = FLINCH_DISTANCE;

	/** 受击冲击：回弹剩余时长（秒），>0 表示正在被甩开并回弹。 */
	private float flinch = 0f;

	/** 受击冲击：分层延迟的剩余等待时长（秒），>0 表示还没轮到自己甩出。 */
	private float flinchWait = 0f;

	/**
	 * @param target 要复制的角色精灵
	 * @param trail  四个残像共用的角色轨迹记录
	 * @param index  残像序号（0 起）：延迟 ＝ (index+1) × {@link #DELAY_STEP}，
	 *               受击时也按它依次延迟起步
	 * @param tint   色调（见 {@link #TINTS}）
	 */
	public AfterimageSprite( CharSprite target, Trail trail, int index, int tint ) {
		super();
		this.target = target;
		this.trail = trail;
		this.delay = (index + 1) * DELAY_STEP;
		this.rank = index;

		//复制纹理、当前帧与尺寸（注意 copy 也会复制角色当时的染色，所以随后要重置）
		copy( target );
		resetColor();
		tint( tint, TINT_STRENGTH );
		alpha( ALPHA );
	}

	/**
	 * 受击冲击：把残像朝 {@code (dirX, dirY)} 方向（需为单位向量）甩出去，随后自动回弹归位。
	 *
	 * <p>这是「受击导致残像偏移」的入口，由 {@code Afterimage.flinch(...)} 在角色挨打时调用。
	 * 每个残像有两处分层差异：</p>
	 * <ul>
	 *   <li><b>起步时间</b>：本残像要等 {@code rank × }{@link #FLINCH_LAYER_DELAY} 秒才动
	 *       ⇒ 冲击从内层往外一层层传出去；</li>
	 *   <li><b>甩出幅度</b>：越外层甩得越远（见 {@link #FLINCH_LAYER_SCALE}）⇒ 有扩散感。</li>
	 * </ul>
	 * <p>另外甩出角度会按本残像的随机相位偏转 ±12°，所以几个残像是「被震散」而不是整齐平移。</p>
	 *
	 * <p>重复受击 = 重新开始一轮冲击（覆盖，不叠加）：等待中的残像回到「最内层的等待时间」重排队，
	 * 已在回弹的残像从最大幅度重新甩出，不会把残像越推越远。</p>
	 */
	public void flinch( float dirX, float dirY ) {
		//相位偏转：phase 取自 0~2π ⇒ (phase-π)*0.07 ∈ [-0.22,0.22] 弧度 ≈ ±12°
		float rot = (phase - 3.1415927f) * 0.07f;
		float c = (float)Math.cos( rot );
		float s = (float)Math.sin( rot );
		impX = dirX * c - dirY * s;
		impY = dirX * s + dirY * c;
		kickAmp = FLINCH_DISTANCE * (1f + rank * FLINCH_LAYER_SCALE);

		//分层延迟：外层先排队等待，等到自己了才开始甩（最内层 rank=0，等待为 0、立即甩出）
		flinchWait = rank * FLINCH_LAYER_DELAY;
		flinch = flinchWait > 0f ? 0f : FLINCH_TIME;
	}

	@Override
	public void update() {
		super.update();

		//目标不可用（已离场/不在视野内）时隐藏，而不是销毁——角色回到视野内应当立即恢复
		if (target == null || target.parent == null || !target.visible) {
			visible = false;
			return;
		}
		visible = true;

		//① 记录：四个残像共用一份轨迹，同一帧只采一次（Trail 内部去重）
		trail.sample( target );

		//② 回放：把「delay 秒之前」的角色状态原样搬过来
		float px, py;
		if (trail.query( Game.timeTotal - delay )) {
			px = trail.qx;
			py = trail.qy;
			//纹理先于帧矩形（帧矩形换算尺寸时要读纹理宽度），二者取自同一份样本
			if (texture != trail.qTexture) {
				texture = trail.qTexture;
			}
			flipHorizontal = trail.qFlip;
			frame( trail.qFrame );
		} else {
			//还没有任何记录（理论上只出现在第一帧）：退回直接跟随
			px = target.x;
			py = target.y;
			texture = target.texture;
			flipHorizontal = target.flipHorizontal;
			frame( target.frame() );
		}

		//③ 追上判定：回放位置与角色当前位置重合（角色停下、本残像走完自己那段尾巴）
		//   只在「由未追上 → 追上」的那一帧触发一次摇摆；摇摆本身不参与判定，否则会反复触发
		float dx = px - target.x;
		float dy = py - target.y;
		boolean caught = dx * dx + dy * dy <= CAUGHT_RANGE * CAUGHT_RANGE;
		if (caught && !caughtUp) {
			wobble = WOBBLE_TIME;
		}
		caughtUp = caught;

		float wobbleX = 0f, wobbleY = 0f;
		if (wobble > 0f) {
			float age = WOBBLE_TIME - wobble;
			wobble -= Game.elapsed;
			float damp = Math.max( wobble, 0f ) / WOBBLE_TIME; //线性衰减到 0
			float a = WOBBLE_AMPLITUDE * damp;
			wobbleX = a * (float)Math.sin( age * WOBBLE_FREQ + phase );
			wobbleY = a * (float)Math.cos( age * WOBBLE_FREQ * 0.78f + phase );
		}

		//④ 受击冲击：先等自己的分层延迟（外层排队），轮到了再被甩开并回弹
		//   （阻尼余弦 ⇒ 甩出 → 越过平衡点 → 反向摆一下 → 归位，参数见 FLINCH_* 常量）
		//   与摇摆同理，只影响绘制位置，绝不参与③的追上判定（否则会被自己甩出的偏移反复误判）
		float kickX = 0f, kickY = 0f;
		if (flinchWait > 0f) {
			flinchWait -= Game.elapsed;
			if (flinchWait <= 0f) {
				flinch = FLINCH_TIME; //排队结束，正式开始甩
			}
		}
		if (flinch > 0f) {
			float age = FLINCH_TIME - flinch;
			flinch -= Game.elapsed;
			float damp = Math.max( flinch, 0f ) / FLINCH_TIME; //线性衰减到 0
			float a = kickAmp * damp * (float)Math.cos( age * FLINCH_FREQ );
			kickX = impX * a;
			kickY = impY * a;
		}

		//⑤ 不稳定：JITTER 像素内的缓慢漂移（两个不同频率的正弦叠加，避免看出规律）
		float t = Game.timeTotal + phase;
		x = px + (float)(Math.sin( t * 1.7f ) * 0.7f + Math.sin( t * 3.3f ) * 0.3f) * JITTER + wobbleX + kickX;
		y = py + (float)(Math.cos( t * 2.1f ) * 0.7f + Math.sin( t * 1.3f ) * 0.3f) * JITTER + wobbleY + kickY;
	}

	//==========================================================================
	// 轨迹记录
	//==========================================================================

	/**
	 * 角色轨迹的环形缓冲：每帧记录一次角色的「位置 + 朝向 + 当前帧 + 纹理」。
	 *
	 * <p>四个残像共用<b>同一个</b> Trail 实例，各自按不同时间偏移来查——这样它们的位移轨迹
	 * 天然是同一条线的不同段，绝不会互相漂移。</p>
	 *
	 * <p>查询（{@link #query}）按时间做线性插值，所以即使帧率很低，残像的位移也是匀速的；
	 * 离散量（朝向/帧/纹理）取「不晚于目标时刻的那个样本」，不做混合。</p>
	 */
	public static class Trail {

		/** 缓冲容量（帧数）。按 60fps 计覆盖 1.6 秒，远大于最大延迟 4×{@link #DELAY_STEP}。 */
		private static final int CAPACITY = 96;

		/** 采样间隔超过本值（长时间暂停）时丢弃历史，避免残像横穿整张地图；时间倒流另判。 */
		private static final float RESET_GAP = 0.5f;

		private final float[] time = new float[CAPACITY];
		private final float[] xs = new float[CAPACITY];
		private final float[] ys = new float[CAPACITY];
		private final boolean[] flips = new boolean[CAPACITY];
		private final RectF[] frames = new RectF[CAPACITY];
		private final SmartTexture[] textures = new SmartTexture[CAPACITY];

		/** 有效样本数。 */
		private int size = 0;

		/** 最新样本的下标。 */
		private int head = 0;

		/** 上一次采样的时刻（NaN 表示还没采过）。 */
		private float last = Float.NaN;

		/** 查询结果：位置（已按时间插值）。 */
		public float qx, qy;

		/** 查询结果：朝向 / 帧矩形 / 纹理（取「不晚于目标时刻」的那个样本）。 */
		public boolean qFlip;
		public RectF qFrame;
		public SmartTexture qTexture;

		/**
		 * 记录一次角色状态。同一帧内被多个残像调用时只记一次（{@code Game.timeTotal} 去重）。
		 * 注意本方法由 floorEmitters 层的残像调用，而 sprites 的位移补间在 mobs 层——
		 * floorEmitters 先更新，所以采到的是角色<b>上一帧末</b>的位置，对四个残像一致，无副作用。
		 */
		public void sample( CharSprite t ) {
			float now = Game.timeTotal;
			if (now == last) return;
			//丢弃历史：①间隔过久（长时间暂停）；②时间倒流——Game.switchScene() 会把 timeTotal 归零，
			//若不判「now < last」就会一直取到上个场景的陈旧样本，残像会定在错误的位置上
			if (size > 0 && (now < last || now - last > RESET_GAP)) size = 0;

			head = (head + 1) % CAPACITY;
			time[head] = now;
			xs[head] = t.x;
			ys[head] = t.y;
			flips[head] = t.flipHorizontal;
			frames[head] = t.frame();
			textures[head] = t.texture;
			if (size < CAPACITY) size++;
			last = now;
		}

		/**
		 * 查出 {@code t} 时刻角色的状态，写入 {@code qx/qy/qFlip/qFrame/qTexture}。
		 *
		 * @return false 表示还没有任何记录，调用方应退回直接跟随角色。
		 */
		public boolean query( float t ) {
			if (size == 0) return false;

			int idx = newestAtOrBefore( t );
			int newer = (idx + 1) % CAPACITY;

			qFlip = flips[idx];
			qFrame = frames[idx];
			qTexture = textures[idx];

			//要查的时刻比最新的样本还新（历史还不够长、残像还没「出生」）⇒ 直接用最新样本
			if (idx == head || size < 2) {
				qx = xs[idx];
				qy = ys[idx];
			} else {
				float t0 = time[idx];
				float t1 = time[newer];
				float k = t1 > t0 ? (t - t0) / (t1 - t0) : 0f;
				qx = xs[idx] + (xs[newer] - xs[idx]) * k;
				qy = ys[idx] + (ys[newer] - ys[idx]) * k;
			}
			return true;
		}

		/** 找到「时刻不晚于 t 的最新样本」；所有样本都晚于 t 时返回最旧的那个。 */
		private int newestAtOrBefore( float t ) {
			int oldest = (head - (size - 1) + CAPACITY * 2) % CAPACITY;
			if (t >= time[head]) return head;
			if (t <= time[oldest]) return oldest;
			for (int i = 1; i < size; i++) {
				int idx = (head - i + CAPACITY * 2) % CAPACITY;
				if (time[idx] <= t) return idx;
			}
			return oldest;
		}
	}
}
