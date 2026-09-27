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

package com.shatteredpixel.shatteredpixeldungeon.windows;

import com.badlogic.gdx.graphics.Pixmap;
import com.shatteredpixel.shatteredpixeldungeon.Assets;
import com.shatteredpixel.shatteredpixeldungeon.SPDSettings;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.Trials;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.Button;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;

/**
 * 考验的选择/展示窗口 —— **卡巴拉生命之树的排版**（2026-09-25 起，取代原先的逐条勾选列表）。
 *
 * <h3>布局</h3>
 * 十个质点按生命之树的三列排布，列与列之间是**严格等距蜂窝**（列间距 = a·√3/2，相邻列错开 a/2）：
 * <pre>
 *                  KETER
 *        BINAH              HOKMA
 *                (Da'at 空位)
 *        GEBURA            CHESED
 *               TIPHERETH
 *         HOD              NETZACH
 *                  YESOD
 *                 MALKUTH
 * </pre>
 * 22 条路径把它连成生命之树：14 条长 a、7 条长 √3·a，外加 KETER—TIPHERETH 那条长 2a 的「跨过
 * 空掉的 Da'at 位」的直连。整树外包 = (2h + 最大图标显示宽) × (4a + 最大图标显示高)
 * = **103.3 × 212**（a = 47.5、图标 1.40 缩放时），装进 120 的内容宽（左右各留约 8.4px）。
 *
 * <h3>窗口尺寸（2026-09-25 与挑战窗对齐）</h3>
 * 内容 **120 × 236**，与 {@code WndChallenges}（13 条挑战 ⇒ 16 + 13×16 + 12×1 = 236）
 * **逐像素同规格**，外框同为 132 × 248。前一版按 a = 70 排 ⇒ 内容高 326（外框 338），
 * 在机上把上下沿顶出屏幕（用户 2026-09-25 反馈「占据整个屏幕」）。
 * 现在 a 收到 47.5 —— 这是「4a + 最大图标显示高 ≤ 212」卡出来的上限 —— 内容高恰为 236。
 * 树仍是「同一张图」的等比缩小，**图标显示尺寸（1.40）一个像素没动**。
 *
 * <h3>为什么几何能全写死</h3>
 * 树是**静态几何**：所有质点位置、路径端点都只由下面 A / H / NODE_DX / NODE_DY / EDGES 推出来，
 * 不依赖任何手绘像素。改 a 或 ICON_SCALE 只动这两个数，图会自己重排。
 * ⚠️ 别把 a 放大到「a·√3 + 最大图标显示宽 &gt; WIDTH」，那会让树溢出窗口。
 *
 * <h3>图标（用户 2026-09-25 口径）</h3>
 * 取样矩形恒取 tree.png 里那一帧的**原生 13~16px**（{@link Trials#ICON_W}）；
 * 需要显示得更大或更小时**只改 {@code Image.scale}（＝只改显示尺寸）**，贴图本身一个像素都不重采样。
 * {@code ICON_SCALE = 1.40} 是原 0.70 的两倍（用户 2026-09-25：0.70 在机上偏小，故翻倍）；
 * 取样走 NEAREST（{@code SmartTexture} 的默认），所以放大不会糊，代价是非整数倍会让少数行列宽一像素。
 * 三行分别对应「已开启 / 未解锁 / 已解锁未开启」，判据在 {@link Trials#iconRow}。
 *
 * <h3>交互</h3>
 * 质点本身就是热点：点开 {@link WndTrialInfo} 详情窗（左上角图标 + 名称 + 描述 + 开关）。
 * **开关放在详情窗里，而且必须由那个窗口自己构造** —— ⚠️ 不能在这里 new 好再传进去：
 * 全局 PointerArea 监听表是 stackMode 的 Signal（后注册优先、首个 true 吞掉其余），而窗口
 * 构造时会加一个覆盖全屏的 blocker，早注册的控件会被它抢先命中、永远点不动。见 WndTrialInfo 类注释。
 * 勾选只改本窗口的「待提交」位掩码，仍然是在关窗时（{@link #onBackPressed()}）一次性写回
 * SPDSettings —— 别改成实时写回（那样每点一下都会落盘）。
 */
public class WndTrials extends Window {

	//=== 窗口尺寸 ===
	private static final int WIDTH		= 120;	//内容宽（与 WndChallenges 齐平；树只占 103.3，左右各余 8.4）
	private static final int TTL_HEIGHT	= 16;	//标题高
	private static final int PAD		= 4;	//标题与树、树与下缘之间的留白

	//=== 树的几何（2026-09-25 定稿）===
	//a = 相邻两质点的纵向步长；h = 列间距 = a·√3/2。
	//「严格等距」的含义：纵向步长与斜向步长同为 a —— 斜向 = √(h² + (a/2)²) = √(3a²/4 + a²/4) = a。
	//中列在 y = 0 / 2a / 3a / 4a（a 处是空掉的 Da'at 位，纯空白），左右两列在 0.5a / 1.5a / 2.5a。
	private static final float A	= 47.5f;
	private static final float H	= A * 1.7320508f / 2f;	//≈ 41.14

	//=== 图标 ===
	//显示缩放：**只改显示尺寸、不重采样**（见类注释）。1.40 = 原 0.70 的两倍（用户 2026-09-25）。
	//两个「装得下」的判据（verify_trials_tree_ui.py 都钉住）：
	//  宽度：a·√3 + 最大图标显示宽 ≤ WIDTH ⇒ 120 宽下 a ≤ 57.1（不是瓶颈）
	//  高度：4a + 最大图标显示高 ≤ 212（= 挑战窗内容高 236 − 标题 16 − 上下留白 8）⇒ a ≤ 47.5
	//所以 a 取 47.5 是**高度**卡出来的上限，此时内容高恰 236，与 WndChallenges 同规格。
	private static final float ICON_SCALE	= 1.40f;
	//质点热点的边长：图标本体最大 22px，命中区取 26 把图标整个圈住。质点最近距离 = A = 47.5 > 26，不互压。
	private static final float TOUCH		= 26f;

	//=== 路径 ===
	private static final String LINES_KEY	= "trials_tree_paths";	//TextureCache 键（整张线稿只建一次）
	private static final float LINE_ALPHA	= 0.35f;				//35% 黑
	private static final float LINE_GAP		= 2f;					//线端与图标之间留的细缝

	//每个质点的显示尺寸（原生尺寸 × ICON_SCALE 后取整；静态块里填）
	private static final int[] ICON_DW = new int[Trials.NAME_IDS.length];
	private static final int[] ICON_DH = new int[Trials.NAME_IDS.length];

	//质点相对树外包框中心的行列偏移：0 = 中列，±H = 左右两列；下标对应 Trials.NAME_IDS
	private static final float[] NODE_DX = { 0f, +H, -H, +H, -H, 0f, +H, -H, 0f, 0f };
	private static final float[] NODE_DY = {
			0f, 0.5f*A, 0.5f*A, 1.5f*A, 1.5f*A, 2f*A, 2.5f*A, 2.5f*A, 3f*A, 4f*A };

	//22 条路径（下标对应 Trials.NAME_IDS）。三种长度：14 条 a、7 条 √3a、1 条 2a。
	private static final int[][] EDGES = {
			{0, 1}, {0, 2}, {0, 5}, {1, 2}, {1, 3}, {1, 5}, {2, 4}, {2, 5},
			{3, 4}, {3, 5}, {3, 6}, {4, 5}, {4, 7}, {5, 6}, {5, 7}, {5, 8},
			{6, 7}, {6, 8}, {6, 9}, {7, 8}, {7, 9}, {8, 9}
	};

	//=== 派生量（静态初始化一次）===
	private static final float TREE_W;		//整树外包宽
	private static final float TREE_H;		//整树外包高
	private static final float LINE_TRIM;	//线端相对质点中心的退让
	private static final int LINES_W;		//线稿贴图宽（上取整）
	private static final int LINES_H;

	static {
		int maxW = 0, maxH = 0;
		for (int i = 0; i < ICON_DW.length; i++) {
			ICON_DW[i] = Math.max( 1, Math.round( Trials.ICON_W[i] * ICON_SCALE ) );
			ICON_DH[i] = Math.max( 1, Math.round( Trials.ICON_H[i] * ICON_SCALE ) );
			maxW = Math.max( maxW, ICON_DW[i] );
			maxH = Math.max( maxH, ICON_DH[i] );
		}
		//横向：左右两列的中心各距中列 H，再各自向两边伸出半个图标
		TREE_W = 2 * H + maxW;
		//纵向：从 KETER 到 MALKUTH 共 4a，上下各留半个图标
		TREE_H = 4 * A + maxH;
		LINE_TRIM = Math.max( maxW, maxH ) / 2f + LINE_GAP;
		LINES_W = (int)Math.ceil( TREE_W );
		LINES_H = (int)Math.ceil( TREE_H );
	}

	//本窗口内「待提交」的位掩码：点开关只改它，关窗时才写回 SPDSettings
	private int mask;
	private boolean editable;
	private Image[] icons;

	public WndTrials( int checked, boolean editable ) {

		super();

		this.editable = editable;
		this.mask = checked;

		RenderedTextBlock title = PixelScene.renderTextBlock( Messages.get(this, "title"), 12 );
		title.hardlight( TITLE_COLOR );
		title.setPos(
				(WIDTH - title.width()) / 2,
				(TTL_HEIGHT - title.height()) / 2
		);
		PixelScene.align(title);
		add( title );

		//树外包框在窗口内容坐标里的落点：水平居中，纵向接在标题下方
		float ox = PixelScene.align( (WIDTH - TREE_W) / 2f );
		float oy = PixelScene.align( TTL_HEIGHT + PAD );

		//① 路径层：22 条线一次画成一张贴图，之后只是 1:1 贴上去
		Image lines = new Image( pathsTexture() );
		lines.x = ox;
		lines.y = oy;
		add( lines );

		//② 十个质点
		icons = new Image[Trials.NAME_IDS.length];
		for (int i = 0; i < icons.length; i++) {

			final int idx = i;

			Image icon = new Image( Assets.Interfaces.TRIALS );
			updateIcon( icon, idx );
			//只改显示尺寸：取样矩形仍是原生 13~16px，缩放发生在 quad 上（贴图不重采样）
			scaleToDisplay( icon, idx );
			icon.x = PixelScene.align( ox + nodeX( idx ) - ICON_DW[idx] / 2f );
			icon.y = PixelScene.align( oy + nodeY( idx ) - ICON_DH[idx] / 2f );
			add( icon );
			icons[idx] = icon;

			//质点自己就是热点（图标太小，点不准）
			Button hot = new Button() {
				@Override
				protected void onClick() {
					super.onClick();
					openDetail( idx );
				}
				@Override
				protected String hoverText() {
					return Messages.get( Trials.class, Trials.NAME_IDS[idx] );
				}
			};
			hot.setRect(
					ox + nodeX( idx ) - TOUCH / 2f,
					oy + nodeY( idx ) - TOUCH / 2f,
					TOUCH, TOUCH );
			add( hot );
		}

		resize( WIDTH, (int)(TTL_HEIGHT + PAD + TREE_H + PAD) );
	}

	/** 质点中心的 x（相对树外包框左上角）。 */
	private static float nodeX( int index ){
		return TREE_W / 2f + NODE_DX[index];
	}

	/** 质点中心的 y（相对树外包框左上角，y 向下）。 */
	private static float nodeY( int index ){
		//上缘留半个图标，之后才是节点自身的纵向位移
		return (TREE_H - 4 * A) / 2f + NODE_DY[index];
	}

	/**
	 * 按「待提交的开启状态」把某个质点的图标切到对应的状态行。
	 * <p>三行包围盒逐格一致 ⇒ 只改取样矩形的 y 偏移，尺寸与原点都不用重算。</p>
	 */
	private void updateIcon( Image icon, int index ){
		int row = Trials.iconRow( index, (mask & Trials.MASKS[index]) != 0 );
		icon.frame( icon.texture.uvRectBySize(
				(index % Trials.ICON_COLS) * Trials.ICON_FRAME,
				row * Trials.ICON_FRAME,
				Trials.ICON_W[index], Trials.ICON_H[index] ) );
	}

	/**
	 * 把图标切到「显示尺寸」（＝原生像素 × {@code ICON_SCALE}）。
	 *
	 * <p>**只动 {@code scale}，贴图一个像素都不重采样**：取样矩形仍旧是原生 13~16px，
	 * 放大/缩小都发生在 quad 上（用户 2026-09-25 口径）。树上的质点与详情窗的图标共用它，
	 * 保证「点谁看谁」两处尺寸一致。</p>
	 */
	private static void scaleToDisplay( Image icon, int index ){
		icon.scale.set(
				ICON_DW[index] / (float)Trials.ICON_W[index],
				ICON_DH[index] / (float)Trials.ICON_H[index] );
	}

	/** 点开某个质点的详情窗：左上角图标 + 名称 + 描述 + 开关。 */
	private void openDetail( final int index ){

		Image icon = new Image( Assets.Interfaces.TRIALS );
		updateIcon( icon, index );
		//详情窗里的图标与树上那枚**同一显示尺寸**（点谁看谁，大小连着）：必须在交给
		//WndTrialInfo 之前设好 scale —— {@code IconTitle.layout()} 读的是
		//{@code width()} = 原生宽 × scale，设晚了标题位置会压在图标上。
		scaleToDisplay( icon, index );

		//⚠️ 开关**必须由 WndTrialInfo 自己在 super(...) 之后构造**，这里只能给文案 / 初值 / 回调。
		//  Button 的 PointerArea 是在**构造时**注册进全局监听表的，而那张表是 stackMode 的 Signal
		//  （后注册者优先、首个返回 true 的吞掉其余）；WndTrialInfo 构造时会加一个**覆盖全屏**的
		//  blocker（点窗口外即关窗）。若在这里先 new 好开关再传进去，它就比那个 blocker 早注册 ⇒
		//  每次点击都被 blocker 抢先吃掉，表现为「点上去毫无反应」。详见 WndTrialInfo 类注释。
		ShatteredPixelDungeon.scene().add( new WndTrialInfo(
				icon,
				Messages.get( Trials.class, Trials.NAME_IDS[index] ),
				Messages.get( Trials.class, Trials.NAME_IDS[index] + "_desc" ),
				Messages.get( this, "enable" ),
				(mask & Trials.MASKS[index]) != 0,
				editable && Trials.isUnlocked( index ),
				new WndTrialInfo.ToggleListener() {
					@Override
					public void onToggle( boolean checked ) {
						if (checked) {
							mask |= Trials.MASKS[index];
						} else {
							mask &= ~Trials.MASKS[index];
						}
						//树上的那枚图标立刻跟着换行（已开启 = 彩色行）
						updateIcon( icons[index], index );
					}
				} ) );
	}

	/**
	 * 22 条路径的线稿贴图（整层一次画好）。
	 *
	 * <p>像素直接写进 {@code SmartTexture.bitmap}，**不需要手动上传**：{@code TextureCache.create}
	 * 只是把对象建好（{@code id == -1}，它内部的 filter/wrap 见到未生成会跳过 GL 调用），
	 * 真正的 {@code generate() → bitmap(pixmap)} 发生在第一次 {@code bind()}（第一次绘制），
	 * 那时像素已经写好了；断上下文后 {@code TextureCache.reload()} 也会拿同一个 Pixmap 重新上传
	 * （与 {@code effects/HeatVignette} 同一套做法）。</p>
	 *
	 * <p>用 {@code Blending.None} 是有意的：线条本身就半透明，若走 SourceOver，两条线交叉处会
	 * **叠两次**而比别处更黑、一眼看出「节点附近颜色更深」。直接写入让全图每个像素都是同一个 35% 黑。</p>
	 */
	private static SmartTexture pathsTexture(){

		if (!TextureCache.contains( LINES_KEY )) {

			SmartTexture tx = TextureCache.create( LINES_KEY, LINES_W, LINES_H );
			Pixmap pix = tx.bitmap;

			pix.setBlending( Pixmap.Blending.None );
			pix.setColor( 0f, 0f, 0f, LINE_ALPHA );

			for (int[] e : EDGES) {

				float ax = nodeX( e[0] ), ay = nodeY( e[0] );
				float bx = nodeX( e[1] ), by = nodeY( e[1] );

				float dx = bx - ax, dy = by - ay;
				float len = (float)Math.sqrt( dx * dx + dy * dy );
				dx /= len;
				dy /= len;

				//两端各退让一点，别让线顶到图标上
				pix.drawLine(
						Math.round( ax + dx * LINE_TRIM ), Math.round( ay + dy * LINE_TRIM ),
						Math.round( bx - dx * LINE_TRIM ), Math.round( by - dy * LINE_TRIM ) );
			}
		}

		return TextureCache.get( LINES_KEY );
	}

	@Override
	public void onBackPressed() {

		//开窗时给的是**待提交值**，关闭时一次性写回；别改成实时写回（那样每点一下都会落盘）
		if (editable) {
			SPDSettings.trials( mask );
		}

		super.onBackPressed();
	}
}
