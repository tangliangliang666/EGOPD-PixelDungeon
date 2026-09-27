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

package com.shatteredpixel.shatteredpixeldungeon.effects;

import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.actors.Char;
import com.shatteredpixel.shatteredpixeldungeon.effects.particles.TearSwordTrailParticle;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;

import java.util.ArrayList;

/**
 * 「泪剑」buff 的环绕视觉：每层在角色周围显示一柄泪剑（{@code sprites/tearsword.png}），
 * 带慢速浮动、跟随惯性与深蓝色拖尾，近战挥击时集体突刺目标。
 *
 * <h3>排布（绝望形态）</h3>
 * <ul>
 *   <li>圆心 = 角色<b>脚底</b>（贴图底边中点）再整体叠上 {@link #Y_OFFSET}（负值 = 上移），
 *       半径 {@link #RADIUS} = 16 像素；</li>
 *   <li>对齐点 = 剑贴图内的 <b>(5,5) 像素</b>（{@link #PIVOT_X}/{@link #PIVOT_Y}），<b>不是</b>贴图几何中心。
 *       noosa 的 {@code origin} 既是平移量又是旋转/镜像轴心，所以「以 (5,5) 为中心」只需
 *       {@code origin.set(5,5)}，再把位置写成 {@code x = 剑心x - 5}（定位、旋转、镜像三者同轴）；</li>
 *   <li>各剑的对齐点均分该<b>上半圆</b>圆弧，角度取「把半圆切成 n 等份后的每份中点」
 *       （{@code θ = π·(i+0.5)/n}）：1 层时 θ=90° 恰在头顶正上方，且任何层数都不会落在水平两端
 *       （否则会与角色的腿/脚重叠）。</li>
 * </ul>
 *
 * <h3>两种形态（{@link Form}）</h3>
 * <ul>
 *   <li><b>{@link Form#DESPAIR 绝望}</b>（默认）：上面描述的上半圆弧排布，剑尖朝向角色朝向，
 *       近战时集体突刺（{@link #strike}）；</li>
 *   <li><b>{@link Form#BLESSING 加护}</b>：剑身一律<b>竖直向下</b>（贴图原始剑尖指向右下 45°，
 *       再顺时针转 45° 即竖直向下），沿以角色<b>躯干中心</b>（同样叠上 {@link #Y_OFFSET}）为圆心、
 *       半径 {@link #BLESS_RADIUS}
 *       （比 {@link #RADIUS} 略小）的<b>整圆</b>均匀分布，并整体随时间绕角色<b>旋转</b>
 *       （{@link #ORBIT_SPEED}）；浮动 / 惯性 / 拖尾与绝望形态共用。</li>
 * </ul>
 * <p>形态切换走 {@link #setForm}：<b>位置</b>交给原有的惯性缓动自然挪过去（剑会沿直线飘到新轨道上），
 * <b>角度</b>则让基准角在 {@link #SWITCH_TIME} 内平滑过渡，并额外叠一圈 {@link #SWITCH_SPIN} 度、
 * 随后自行"解开"的旋转，于是读起来是「边转边换位」。两段曲线在切换那一帧的取值都与切换前完全等价
 * （附加旋转恰为整圈 ≡ 0、基准角从当前值出发），因此不会跳变。剑尖转速是形态切换动画的一部分，
 * 与突刺时的转向（{@link #strikeRotate}）叠加互不冲突。</p>
 *
 * <h3>浮动</h3>
 * <p>在弧上基准点之外再叠一层小幅慢速漂移：X/Y 用两个<b>不同频率</b>的正弦（{@link #FLOAT_SPEED} 0.73 倍）
 * 叠加，避免走成规整的圆（那样像个转盘而不是"漂浮"）；每柄剑相位按 {@code i * 2.399}（黄金角）错开，
 * 三柄剑不会整齐划一地同步起伏；角度另叠 {@link #FLOAT_WOBBLE} 度的慢速摆动，让剑身有轻微摇曳感。</p>
 *
 * <h3>惯性</h3>
 * <p>剑不会瞬移到新位置，而是每帧朝目标做指数缓动
 * （{@code s += (target - s) * (1 - exp(-FOLLOW_SPEED·dt))}）。角色走动时剑便落下一段距离再追上，
 * 停步后收敛归位——稳态滞后 ≈ 移速 / {@code FOLLOW_SPEED}。突刺阶段例外（见下）。</p>
 *
 * <h3>协同攻击</h3>
 * <p>{@link #strike(float, float)} 由 buff 在角色<b>近战挥击完成</b>时调用：全部剑沿
 * 「角色脚底 → 目标」的方向<b>依次</b>（每柄延迟 {@link #STRIKE_STAGGER}）冲向目标，抵住片刻后收回，
 * 整个过程 {@link #STRIKE_OUT}+{@link #STRIKE_HOLD}+{@link #STRIKE_BACK}（+层数错峰）。
 * 出力使用 smoothstep 曲线，收回段略长于冲出段，读起来像"戳出去→收回来"。
 * 突刺期间位置<b>直接跟随</b>这条定时曲线（不走惯性），因为这是一段脚本化动画，
 * 缓动会把方正的节奏糊掉；曲线两端位移均为 0，故进出突刺都不会突跳。
 * 到达点还带一点越过目标的 {@link #STRIKE_REACH} 与横向铺开的 {@link #STRIKE_SPREAD}，
 * 剑不叠在同一个像素上。</p>
 *
 * <p><b>突刺时的剑尖朝向</b>：光把剑平移到目标身上是没有「戳刺感」的，所以突刺期间剑身还要
 * <b>转成剑尖指向目标</b>——即把贴图原始朝右下的剑尖旋到落点方向。角度同样由 {@link #strikeRotate}
 * 驱动：冲刺段的<b>前 {@link #STRIKE_TURN_PART} 就完成转向</b>（此后才是明显的位移），
 * 收回段则与位移同步转回原位角度，于是读起来是「先摆正剑头 → 戳出去 → 边收边转回来」。
 * 角度插值走 {@link #angleDelta} 归一化的最短路径，避免绕远路；两端值均为 0，进出突刺不跳变。</p>
 *
 * <h3>朝向</h3>
 * <p>泪剑贴图是斜向绘制的（剑柄在左上、剑尖指向右下），逆时针旋转 45° 后剑身水平、剑尖指向角色朝向，
 * 即「正方向」。角色朝左时要求做<b>水平翻转</b>。noosa 的变换顺序是
 * {@code T(x,y)·T(origin)·R(angle)·S(scale)·T(-origin)}（scale 在 rotate 之前作用于点），
 * 因此“先转 45° 再左右镜像”等价于 {@code R(+45)·S(-1,1)}——即朝左时把角度取反、并让
 * {@code scale.x = -1}；镜像轴过 origin，故翻转后 (5,5) 仍落在圆弧上的同一点。
 * 注意：静态 {@code Image} 直接设 {@code flipHorizontal} 是<b>无效</b>的（该字段只在
 * {@code frame()} → {@code updateFrame()} 时才写进顶点，本类不播帧动画），必须用 {@code scale.x = -1}。</p>
 *
 * <p><b>突刺指向的角度换算</b>：贴图剑尖未旋转时指向 45°（noosa 角度正值为顺时针，右下即 45°），
 * 故要让剑尖指向屏幕角度 θ 只需 {@code angle = θ − 45}；朝左时贴图被水平镜像、剑尖基准角变为
 * {@code atan2(1, −1)} = 135°，相应改成 {@code angle = θ − 135}。两式与上面的「原位角度」自洽：
 * 怪物在正右 θ = 0° ⇒ {@code angle = −45}（= 朝右原位），正左 θ = 180° ⇒ {@code angle = 45}
 * （= 朝左原位）——也就是说正左/正右的贴身近战本来就已对准，肉眼能看出明显转向的是
 * <b>斜角方向</b>的怪物（θ = ±45° / ±135°）。</p>
 *
 * <h3>分层（环绕遮挡）</h3>
 * <p>与角色朝向<b>同侧</b>的剑（朝左→左半边的剑，朝右→右半边的剑）沉到角色贴图<b>之下</b>，
 * 会被角色遮挡，从而凸显「环绕」的立体感；背侧的剑仍在角色<b>之上</b>。实现：
 * 后者是本组的成员（本组挂在 {@code target.sprite.parent} = mobs 层，非 Visual 的 Gizmo 会被
 * {@code GameScene.sortMobSprites()} 排到队尾 → 渲染在所有角色贴图之上），前者移入 {@link #behind}
 * 组，而 {@link #behind} 挂在 {@code GameScene.floorEmitters}（在 mobs 之前添加 → 渲染在角色之下）。
 * 剑心恰好落在角色中轴上那柄（层数为奇数时的正上方那柄）归入「之上」，免得被脑袋挡掉。</p>
 * <p>分层判据用的是<b>弧上基准点</b>（不含浮动），否则剑随浮动左右晃过中轴时会来回换层而闪烁。
 * 但若某柄剑已经<b>离开轨道</b>超过 {@link #STRIKE_FRONT_DIST} 像素（只在突刺时发生），
 * 则一律拉回「之上」——否则冲过去的那几柄会被角色/怪物贴图挡住，白做动画。</p>
 * <p><b>加护形态的判据不同</b>：它是整圈环绕，沉下去的改为圆环<b>上半</b>（屏幕上方 = 较远的一侧）的剑，
 * 即「上远下近」而不是「左右」。这样每柄剑转一圈只在圆的<b>左右两端</b>换一次层——那里剑离身体最远，
 * 看不出跳变；若沿用左右判据，剑转到正上/正下时会跨过中轴，在脑袋/脚下闪一下层。</p>
 *
 * <h3>生命周期</h3>
 * <p>每帧读取角色贴图的绝对坐标重新定位，故自动跟随移动；切场景/换层后旧视觉随场景一同销毁
 * （销毁时 parent 会被置空），由 buff 在 {@code act()} 里重建。释放请走 {@link #dispose()} /
 * {@code killAndErase()}——背身剑不是本组的成员，必须单独回收。</p>
 */
public class TearSwordVisual extends Group {

	/** 环绕半径（像素），自角色脚底起算。 */
	public static final float RADIUS = 16f;

	/**
	 * 两种形态轨道的整体垂直偏移（像素，<b>负值 = 上移</b>）。它直接加在轨道圆心的高度上：
	 * 绝望形态的「脚底」与加护形态的「躯干中心」<b>同时</b>上/下移同样的量，
	 * 于是整个特效相对角色贴图整体平移（半径、浮动、突刺指向都不受影响）。
	 * 这是个纯视觉微调用的旋钮，按观感自行调整数值即可。
	 */
	public static final float Y_OFFSET = -4f;

	/** 正向（角色朝右）时的旋转角：逆时针 45°。noosa 中正角为顺时针，故取负值。 */
	public static final float BASE_ANGLE = -45f;

	/** 贴图内的对齐点（像素，自贴图左上角起算）：剑心的落点，同时是旋转轴与镜像轴。 */
	public static final float PIVOT_X = 5f;
	public static final float PIVOT_Y = 5f;

	//--------------------------------------------------------------------------
	// 形态
	//--------------------------------------------------------------------------

	/**
	 * 两种环绕形态。
	 * <ul>
	 *   <li>{@link #DESPAIR}（绝望，默认）：剑尖朝向角色朝向，沿脚底上半圆排布，可协同突刺；</li>
	 *   <li>{@link #BLESSING}（加护）：剑身竖直向下，绕角色躯干中心整圈旋转。</li>
	 * </ul>
	 */
	public enum Form { BLESSING, DESPAIR }

	/** 加护形态的轨道半径（像素）：以角色躯干中心为圆心，刻意比 {@link #RADIUS} 略小。 */
	public static final float BLESS_RADIUS = 11f;

	/**
	 * 加护形态「正向」时的旋转角（度）：贴图原始剑尖指向右下 45°，再顺时针转 45° 即<b>竖直向下</b>。
	 * 与 {@link #BASE_ANGLE} 恰好互为相反数（朝左时都取反并水平镜像）。
	 */
	public static final float BLESS_ANGLE = 45f;

	/** 加护形态整圈绕行的角速度（弧度/秒）：约 1.05 rad/s → 一圈约 6 秒，属"缓慢环绕"。 */
	public static final float ORBIT_SPEED = 1.05f;

	/** 形态切换的过渡时长（秒）：位置由惯性缓动收敛，时长与之相近，两者看起来是同一段动画。 */
	public static final float SWITCH_TIME = 0.42f;

	/**
	 * 形态切换时额外叠加的旋转量（度）。起点处它恰为整整一圈（≡ 0°，故与切换前等价、不跳变），
	 * 再随时间"解开"归零；与基准角本身的 90° 变化叠加后，净转约 3/4 圈。
	 * 必须是 360 的整数倍，否则起点不再等价于"不额外旋转"。
	 */
	public static final float SWITCH_SPIN = 360f;

	private Form form = Form.DESPAIR;

	/** 加护形态的整圈绕行角（弧度），随时间累加。 */
	private float orbit = 0f;

	/** 形态切换动画计时（秒）；< 0 表示当前没有在切换。 */
	private float switchTimer = -1f;

	/** 本次切换的附加旋转量（度，带符号，见 {@link #SWITCH_SPIN}）。 */
	private float switchSpin = 0f;

	/** 切换动画的基准角起止值（未镜像、未叠摆动的"正向"角）。 */
	private float baseFrom = BASE_ANGLE;
	private float baseTo = BASE_ANGLE;

	/** 当前（插值后）的"正向"基准角与附加旋转，供每帧使用，也让"连按切换"能接着当前值走。 */
	private float baseNow = BASE_ANGLE;
	private float spinNow = 0f;

	//--------------------------------------------------------------------------
	// 浮动
	//--------------------------------------------------------------------------

	/** 浮动漂移幅度（像素，单轴），整体位移不超过 √2 倍此值。 */
	public static final float FLOAT_AMP = 1.5f;

	/** 浮动角速度（弧度/秒）：约 1.1 rad/s → 周期 ≈ 5.7 秒，属于"慢速"。 */
	public static final float FLOAT_SPEED = 1.1f;

	/** 浮动时叠加的角度摆动幅度（度）。 */
	public static final float FLOAT_WOBBLE = 5f;

	/** 相邻两柄剑的浮动相位差（弧度）：取黄金角，三柄剑的起伏互不同步。 */
	public static final float FLOAT_PHASE_STEP = 2.399f;

	//--------------------------------------------------------------------------
	// 惯性
	//--------------------------------------------------------------------------

	/**
	 * 跟随收敛速度（1/秒）。指数缓动的稳态滞后 ≈ 角色移速 / 本值：
	 * 角色约 64 px/s，取 12 → 落后约 5 像素，走停各一拍即可看清"延迟追随"。
	 */
	public static final float FOLLOW_SPEED = 12f;

	//--------------------------------------------------------------------------
	// 拖尾
	//--------------------------------------------------------------------------

	/** 拖尾采样间隔（秒）。 */
	public static final float TRAIL_INTERVAL = 0.05f;

	/** 拖尾粒子间距（像素）：按位移线段补点，使尾迹连成一条线而不是孤立的点。 */
	public static final float TRAIL_STEP = 4f;

	/** 单次采样最多补几颗粒子（防长距离瞬移时爆发式生成）。 */
	public static final int TRAIL_MAX_STEPS = 6;

	/** 低于此位移（像素）就不铺粒子：站着不动、只有浮动微晃时不至于糊成一团。 */
	public static final float TRAIL_MIN_DIST = 2.5f;

	//--------------------------------------------------------------------------
	// 协同攻击
	//--------------------------------------------------------------------------

	/** 冲刺段时长（秒）。 */
	public static final float STRIKE_OUT = 0.10f;

	/** 抵住目标（戳刺定格）时长（秒）。 */
	public static final float STRIKE_HOLD = 0.06f;

	/** 收剑回位时长（秒）。刻意比冲刺略长，读起来像"戳出去快、收回来稳"。 */
	public static final float STRIKE_BACK = 0.22f;

	/** 相邻两柄剑的起手延迟（秒）：形成"依次戳刺"的协同感，而不是三柄同时叠上去。 */
	public static final float STRIKE_STAGGER = 0.035f;

	/** 突刺落点越过目标中心多少像素（沿冲刺方向），让剑"扎进去"而不是停在表面。 */
	public static final float STRIKE_REACH = 3f;

	/** 突刺落点的横向铺开间距（像素，垂直于冲刺方向），避免三柄剑叠在一个像素上。 */
	public static final float STRIKE_SPREAD = 4.5f;

	/** 偏离弧上基准点超过这个距离（像素）就强制画在角色之上（突刺离轨时用）。 */
	public static final float STRIKE_FRONT_DIST = 10f;

	/**
	 * 突刺的「转向」在<b>冲刺段</b>的这个比例内完成（0~1）。取 0.4 表示冲刺的前 40%
	 * 就把剑尖转到指向目标，之后才是明显的位移——即「先调整剑头方向，再戳出去」；
	 * 收剑段则与位移同步转回原位（{@link #strikeRotate}）。
	 */
	public static final float STRIKE_TURN_PART = 0.4f;

	private final Char ch;
	private final ArrayList<Sword> swords = new ArrayList<>();

	/**
	 * 「背身层」：与角色朝向同侧的剑都挂进这个组，而它挂在 {@code GameScene.floorEmitters}
	 * （角色贴图之下的图层）。惰性创建；场景未就绪时保持 null，此时所有剑都留在本组（角色之上）。
	 */
	private Group behind;

	/** 本视觉自己的计时（秒），驱动浮动；与角色是否移动无关。 */
	private float time = 0f;

	private float trailTimer = 0f;

	/** 突刺计时（秒）；< 0 表示当前没有突刺。 */
	private float strikeTimer = -1f;

	/** 突刺目标的世界坐标（角色脚底坐标系，与贴图绝对坐标同系）。 */
	private float strikeX = 0f;
	private float strikeY = 0f;

	public TearSwordVisual( Char ch ) {
		this.ch = ch;
	}

	/** 单柄剑的运行时状态（纯视觉，不参与存档）。 */
	private static class Sword {
		Image img;
		/** 当前渲染位置 = 贴图内 (PIVOT_X, PIVOT_Y) 像素在场景中的坐标。 */
		float x, y;
		/** 浮动相位偏移（弧度）。 */
		float phase;
		/** 是否已就位：首帧直接就位，之后才走缓动，避免从 (0,0) 飞过来。 */
		boolean placed;
		/** 上一次铺拖尾的位置。 */
		float trailX, trailY;
		boolean trailPlaced;
	}

	/** 泪剑数量（= buff 层数）。增删时同步创建/销毁贴图。 */
	public void setStacks( int n ) {
		n = Math.max( 0, n );

		while (swords.size() > n) {
			Sword s = swords.remove( swords.size() - 1 );
			if (s.img != null) s.img.killAndErase();
		}
		while (swords.size() < n) {
			Sword s = new Sword();
			s.img = new Image( Assets.Sprites.TEARSWORD );
			//以贴图内的 (5,5) 像素为轴：noosa 的 origin 同时充当平移量与旋转/缩放轴心，
			//故下面定位时要用 x = 剑心x - PIVOT_X 抵消这份平移（勿用 width()/2——那是几何中心）
			s.img.origin.set( PIVOT_X, PIVOT_Y );
			s.phase = swords.size() * FLOAT_PHASE_STEP;
			swords.add( s );
			add( s.img );
		}

		if (n == 0) {
			strikeTimer = -1f; //没有剑了，突刺状态一并作废
		}
	}

	public int stacks() {
		return swords.size();
	}

	/** 当前形态。 */
	public Form form() {
		return form;
	}

	/**
	 * 切换形态：位置走惯性缓动自然挪到新轨道，角度播放「转 3/4 圈 + 基准角平滑过渡」的动画
	 * （见类注释「两种形态」）。传入 null 或与当前相同则什么都不做。
	 *
	 * <p>若当前<b>一柄剑都没有</b>（视觉刚创建、还没 {@link #setStacks}），则不播动画、直接落到目标形态
	 * ——否则新召唤出来的剑会先以旧形态的角度露一帧再转过去。</p>
	 */
	public void setForm( Form f ) {
		if (f == null || f == form) return;

		final float target = (f == Form.BLESSING) ? BLESS_ANGLE : BASE_ANGLE;

		if (swords.isEmpty()) {
			form = f;
			baseFrom = baseTo = baseNow = target;
			spinNow = 0f;
			switchTimer = -1f;
			return;
		}

		//切向加护取逆时针、切向绝望取顺时针（净转约 3/4 圈）。附加旋转量是在"当前已有的旋转"之上
		//再加整整一圈——起点处整整一圈 ≡ 0°，所以切换瞬间的总角度与切换前完全等价（不跳变），
		//同时也让"动画没播完就连按切换"能接着当前位置继续转，而不是跳回去。
		switchSpin = spinNow + ((f == Form.DESPAIR) ? -SWITCH_SPIN : SWITCH_SPIN);
		//基准角从"当前实际值"出发（上一次切换可能还没走完），避免连按切换时跳角
		baseFrom = baseNow;
		baseTo = target;
		form = f;
		switchTimer = 0f;
	}

	/** 角色贴图是否已在场景中就绪（未就绪时本视觉不可见，也不铺拖尾）。 */
	public boolean attached() {
		return ch != null && ch.sprite != null && ch.sprite.exists && ch.sprite.parent != null;
	}

	/**
	 * 命令所有泪剑向 ({@code x}, {@code y}) 协同突刺一次（世界坐标，与贴图绝对坐标同一坐标系）。
	 * 重复调用会以新目标重新起手；层数为 0 时无效果。
	 *
	 * <p>本动画只属于 {@link Form#DESPAIR 绝望}形态（加护形态的剑是"守"的环，不参与突刺）——
	 * 该语义由 {@code TearSword.strikeAt} 在调用前拦截。本方法只做动画，不判形态。</p>
	 */
	public void strike( float x, float y ) {
		if (swords.isEmpty()) return;
		strikeX = x;
		strikeY = y;
		strikeTimer = 0f;
	}

	/** 当前是否正在突刺。 */
	public boolean striking() {
		return strikeTimer >= 0f;
	}

	@Override
	public void update() {
		super.update();

		CharSprite cs = (ch == null) ? null : ch.sprite;
		if (cs == null || !cs.exists) {
			visible = false;
			return;
		}

		//与角色贴图同显隐：脱离视野（FOV）时不显示
		visible = cs.visible;
		if (!visible) return;

		final float dt = Game.elapsed;
		time += dt;

		//加护形态的整圈绕行角随时间前进（绝望形态用不到，一并推进无妨）
		orbit += ORBIT_SPEED * dt;
		final float TAU = (float)(Math.PI * 2);
		if (orbit >= TAU) orbit -= TAU;

		//---- 形态切换动画（见类注释「两种形态」）----
		//注意变量名避开下方拖尾循环里的粒子变量 p（同方法内不允许重名）
		float sw = 1f;
		if (switchTimer >= 0f) {
			switchTimer += dt;
			if (switchTimer >= SWITCH_TIME) {
				switchTimer = -1f;
			} else {
				sw = smooth( switchTimer / SWITCH_TIME );
			}
		}
		baseNow = baseFrom + (baseTo - baseFrom) * sw;
		//附加旋转：起点为整圈（≡0°）、终点为 0，所以切换首帧与实际基准角完全等价，不会跳变
		spinNow = (switchTimer >= 0f) ? switchSpin * (1f - sw) : 0f;
		final float spin = spinNow;

		final boolean bless = (form == Form.BLESSING);

		//角色贴图原点即其左上角：脚底 = 底边中点，躯干中心 = 贴图中心。
		//两者都叠上 Y_OFFSET，使两种形态的轨道整体平移（见该常量）。
		final float cx = cs.x + cs.width / 2f;
		final float cy = cs.y + cs.height + Y_OFFSET;
		//轨道按形态取：绝望 = 脚底 + 上半圆；加护 = 躯干中心 + 整圈
		final float ccy = (bless ? cs.y + cs.height / 2f : cs.y + cs.height) + Y_OFFSET;
		final float radius = bless ? BLESS_RADIUS : RADIUS;

		//CharSprite.turnTo() 已按目标 x 自动设置 flipHorizontal（见 CharSprite#turnTo）
		final boolean facingLeft = cs.flipHorizontal;

		//朝向同侧的剑要沉到角色贴图之下（见类注释「分层」）；场景未就绪时为 null
		final Group behindLayer = ensureBehind();

		final int n = swords.size();

		//---- 协同攻击计时（含按层数错峰的最后一柄）----
		//注意：strike() 起手时把 strikeTimer 置 0，必须在这里逐帧推进；漏了这句计时器会永远停在 0，
		//strikeAmount() 恒返回 0 → 剑一动不动，且永远退不出突刺状态。
		if (strikeTimer >= 0f) {
			strikeTimer += dt;
			if (strikeTimer > strikeTotal( n )) {
				strikeTimer = -1f;
			}
		}
		final boolean striking = strikeTimer >= 0f;

		//冲刺方向：角色脚底 → 目标。每帧重算，角色/目标在突刺途中移动也能跟上。
		float dirX = 0f, dirY = 0f;
		if (striking) {
			float ddx = strikeX - cx;
			float ddy = strikeY - cy;
			float len = (float)Math.sqrt( ddx * ddx + ddy * ddy );
			if (len > 0.001f) {
				dirX = ddx / len;
				dirY = ddy / len;
			}
		}

		//惯性系数：每帧固定值（指数缓动与帧率无关）
		final float ease = 1f - (float)Math.exp( -FOLLOW_SPEED * dt );

		for (int i = 0; i < n; i++) {
			Sword s = swords.get( i );

			//轨道基准点：绝望 = 上半圆均分（取每份中点，n=1 → 90° 头顶正上方）；
			//加护 = 整圈均分并整体随时间绕行（同样取每份中点，与躯干中轴错开半格）
			final double a = bless
					? orbit + TAU * (i + 0.5) / n
					: Math.PI * (i + 0.5) / n;
			final float bx = cx + (float)Math.cos( a ) * radius;
			final float by = ccy - (float)Math.sin( a ) * radius;

			//---- 浮动：双频正弦漂移 + 角度摆动（相位逐剑错开）----
			float ox = (float)Math.sin( time * FLOAT_SPEED + s.phase ) * FLOAT_AMP;
			float oy = (float)Math.cos( time * FLOAT_SPEED * 0.73f + s.phase * 1.37f ) * FLOAT_AMP;

			float tx = bx + ox;
			float ty = by + oy;

			//---- 协同攻击：从弧上位置沿曲线插值到目标 ----
			float amt = 0f;
			//本柄剑的突刺时钟（含逐柄错峰）；下面转角度时还要用，故提到块外
			float st = 0f;
			//突刺时剑尖要转到的绝对角度；turning 为 false 时用「原位角度」
			float strikeAng = 0f;
			boolean turning = false;
			if (striking) {
				st = strikeTimer - i * STRIKE_STAGGER;
				amt = strikeAmount( st );
				if (amt > 0f) {
					//垂直方向铺开：n=1 居中；n=3 为 -4.5 / 0 / +4.5 像素
					float perp = (i - (n - 1) / 2f) * STRIKE_SPREAD;
					float gx = strikeX - dirY * perp + dirX * STRIKE_REACH;
					float gy = strikeY + dirX * perp + dirY * STRIKE_REACH;

					tx += (gx - tx) * amt;
					ty += (gy - ty) * amt;

					//剑尖朝向：该剑落点相对角色脚底的方向（用脚底而非剑当前位置——冲刺后段
					//剑已贴近落点时方向会抖动）。贴图原始剑尖指向右下 45°，即 noosa 角度下的
					//45°；朝左时贴图被 scale.x = -1 水平镜像，剑尖基准角变成 135°，故减去对应
					//基准即得所需旋转量。注意角度与镜像必须成对推导，否则剑尖会指反。
					strikeAng = (float)Math.toDegrees( Math.atan2( gy - cy, gx - cx ) )
							- (facingLeft ? 135f : 45f);
					turning = true;
				}
			}

			//---- 位置：缓动跟随；突刺段直接跟随曲线 ----
			if (!s.placed) {
				s.x = tx;
				s.y = ty;
				s.placed = true;
			} else if (amt > 0f) {
				s.x = tx;
				s.y = ty;
			} else {
				s.x += (tx - s.x) * ease;
				s.y += (ty - s.y) * ease;
			}

			//让贴图内的 (PIVOT_X, PIVOT_Y) 落到 s.x/s.y：origin 会把贴图整体平移 PIVOT，
			//故定位要减掉它（用原始 width/height 字段而非 width()——后者已乘 scale）
			s.img.x = s.x - PIVOT_X;
			s.img.y = s.y - PIVOT_Y;

			//角度：基准角按形态取（绝望 -45°=剑尖朝朝向、加护 +45°=剑尖竖直向下），
			//再叠慢速摆动；朝左时取反并水平镜像（见类注释「朝向」）。形态切换时基准角平滑过渡，
			//并叠上那圈会自行"解开"的附加旋转（spin）。
			float wobble = (float)Math.sin( time * FLOAT_SPEED * 0.8f + s.phase ) * FLOAT_WOBBLE;
			float rest = (facingLeft ? -baseNow - wobble : baseNow + wobble) + spin;
			float ang = rest;
			if (turning) {
				//按突刺进度在「当前角度 → 指向目标」之间走最短路径插值（角度差需归一化到
				//±180°，否则可能绕远路转一大圈）。转向曲线 strikeRotate() 在冲刺前段就到位、
				//收剑段与位移同步归零，因此读起来是「先摆正剑头 → 戳出去 → 边收边转回原位」。
				ang = rest + angleDelta( rest, strikeAng ) * strikeRotate( st );
			}
			s.img.angle = ang;
			s.img.scale.x = facingLeft ? -1f : 1f;

			//---- 分层 ----
			//判据用弧上基准点 bx（不含浮动），否则剑晃过中轴会来回换层而闪烁；
			//但已离开轨道（突刺中）的剑一律拉到最上层，免得被角色/怪物挡住。
			float offX = s.x - bx;
			float offY = s.y - by;
			boolean offRing = offX * offX + offY * offY > STRIKE_FRONT_DIST * STRIKE_FRONT_DIST;

			//Group.add() 会自动把剑从原组摘除，故直接 add 到目标组即可双向切换。
			Group want;
			if (behindLayer == null || offRing) {
				want = this;                                  //角色之上（或场景未就绪）
			} else if (bless) {
				//加护形态是整圈环绕，按"上远下近"分层：圆环上半在角色身后、下半在身前。
				//判据用躯干中心（by 对比 ccy）而不是左右——否则每柄剑绕到正上/正下时都会跨过中轴
				//而闪一下层；用上下则只在左右两端换层，那里剑离身体最远，看不出跳变。
				want = (by < ccy) ? behindLayer : this;
			} else {
				want = (facingLeft ? bx < cx : bx > cx) ? behindLayer : this;
			}
			if (s.img.parent != want) {
				want.add( s.img );
			}
		}

		if (n == 0) return;

		//---- 深蓝色拖尾 ----
		//逐剑跟踪自己的位移：角色走动、突刺冲出、乃至浮动都会有尾迹，但低于 TRAIL_MIN_DIST
		//的微动（站着不动时的浮动）不铺粒子，免得原地糊成一团。
		trailTimer += dt;
		if (trailTimer < TRAIL_INTERVAL) return;
		trailTimer = 0f;

		for (Sword s : swords) {
			if (!s.trailPlaced) {
				s.trailX = s.x;
				s.trailY = s.y;
				s.trailPlaced = true;
				continue;
			}

			float ddx = s.x - s.trailX;
			float ddy = s.y - s.trailY;
			float dist = (float)Math.sqrt( ddx * ddx + ddy * ddy );
			if (dist < TRAIL_MIN_DIST) continue;

			int steps = Math.round( dist / TRAIL_STEP );
			steps = Math.max( 1, Math.min( TRAIL_MAX_STEPS, steps ) );

			for (int k = 1; k <= steps; k++) {
				float t = k / (float)steps;
				TearSwordTrailParticle p =
						(TearSwordTrailParticle) recycle( TearSwordTrailParticle.class );
				if (p == null) break;
				p.reset( s.trailX + ddx * t, s.trailY + ddy * t );
			}

			s.trailX = s.x;
			s.trailY = s.y;
		}
	}

	//==========================================================================
	// 协同攻击曲线
	//==========================================================================

	/** 一次突刺的总时长（含按层数错峰的最后那柄）。 */
	private static float strikeTotal( int n ) {
		return STRIKE_OUT + STRIKE_HOLD + STRIKE_BACK
				+ STRIKE_STAGGER * Math.max( 0, n - 1 );
	}

	/**
	 * 突刺进度：0 = 停在弧上原位，1 = 抵住目标。三段拼接：
	 * 冲刺（smoothstep 0→1）→ 抵住（恒为 1）→ 收回（smoothstep 1→0）。
	 * 两端值均为 0，因此进出突刺时与「缓动跟随」的结果连续、不会突跳。
	 */
	private static float strikeAmount( float t ) {
		if (t <= 0f) return 0f;

		if (t < STRIKE_OUT) {
			return smooth( t / STRIKE_OUT );
		}
		t -= STRIKE_OUT;

		if (t < STRIKE_HOLD) {
			return 1f;
		}
		t -= STRIKE_HOLD;

		if (t < STRIKE_BACK) {
			return 1f - smooth( t / STRIKE_BACK );
		}
		return 0f;
	}

	/** smoothstep：两端导数为 0 的 S 形插值，用于让冲刺/收回有加减速。 */
	private static float smooth( float p ) {
		return p * p * (3f - 2f * p);
	}

	/**
	 * 突刺的<b>转向</b>进度：0 = 剑尖保持原位角度，1 = 剑尖已指向目标。
	 *
	 * <p>冲刺段只在前 {@link #STRIKE_TURN_PART} 的比例内就转到位（剩下的时间纯位移），
	 * 因此读起来是「先把剑头摆正，再戳出去」；抵住段保持指向；收回段与位移共用同一条
	 * smoothstep 归零曲线，所以<b>收剑回位的过程中剑身同步转回原位角度</b>。
	 * 两端值均为 0，进出突刺时与「原位角度」连续、不会跳变。</p>
	 */
	private static float strikeRotate( float t ) {
		if (t <= 0f) return 0f;

		if (t < STRIKE_OUT) {
			float p = Math.min( 1f, t / (STRIKE_OUT * STRIKE_TURN_PART) );
			return smooth( p );
		}
		t -= STRIKE_OUT;

		if (t < STRIKE_HOLD) return 1f;
		t -= STRIKE_HOLD;

		if (t < STRIKE_BACK) return 1f - smooth( t / STRIKE_BACK );
		return 0f;
	}

	/** 把 {@code to - from} 归一化到 (-180, 180]，让角度插值走最短路径（不绕远）。 */
	private static float angleDelta( float from, float to ) {
		float d = (to - from) % 360f;
		if (d > 180f) d -= 360f;
		if (d <= -180f) d += 360f;
		return d;
	}

	//==========================================================================
	// 分层与回收
	//==========================================================================

	/**
	 * 确保「背身层」已挂到 {@code GameScene.floorEmitters}（角色贴图之下）：成功返回该组，
	 * 场景未就绪时返回 null（此时所有剑都留在本组，渲染在角色之上），下一帧再试。
	 */
	private Group ensureBehind() {
		if (behind != null && behind.parent != null) return behind;

		//注意：被 destroy() 过的 Group 其 members 已被置空、不能再复用，故每次一律新建
		Group g = new Group();
		GameScene.floorEmittersAdd( g );
		if (g.parent == null) return null; //场景未就绪

		behind = g;
		return behind;
	}

	/**
	 * 释放全部剑贴图与「背身层」分组。背身剑挂在 {@link #behind} 而不是本组里，
	 * 不会随本组一起被清掉，故必须在销毁前显式回收（本方法可重复调用）。
	 */
	public void dispose() {
		for (Sword s : swords) {
			if (s.img != null) s.img.killAndErase();
		}
		swords.clear();

		if (behind != null) {
			behind.killAndErase();
			behind = null;
		}

		trailTimer = 0f;
		strikeTimer = -1f;
	}

	@Override
	public synchronized void kill() {
		dispose();
		super.kill();
	}

	@Override
	public synchronized void destroy() {
		dispose();
		super.destroy();
	}
}
