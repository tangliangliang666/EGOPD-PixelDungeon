/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole.ui;

import com.mypd.debugconsole.DebugConsole;

import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

/**
 * A minimal self-contained on-screen panel: a flat background, a 1px border, and content laid
 * out inside it.
 *
 * <p>Deliberately independent of any game's own {@code Window} class so the console can be
 * dropped into another project unchanged. The panel is an ordinary {@link Component} — the host
 * adds it to its scene exactly like any other UI element, and it renders through the scene's own
 * camera.
 *
 * <p>It re-centers itself on screen from {@link #layoutPanel(int, int)}.
 */
public class ConsolePanel extends Component {

	protected static final int BORDER_COLOR = 0xFF3A3A3A;
	protected static final int PANEL_COLOR  = 0xFF1B1B1B;
	protected static final int BORDER_SIZE  = 1;

	/** Content size in UI units, excluding padding. */
	protected int innerW;
	protected int innerH;

	protected float padLeft = 2, padTop = 2, padRight = 2, padBottom = 2;

	private ColorBlock chromeBorder;
	private ColorBlock chromePanel;

	public ConsolePanel() {
		super();

		createChrome();
		createChildren();
	}

	//================================================================================
	//   overridables
	//================================================================================

	/** Adds the panel background + border. Called before {@link #createChildren()}. */
	protected void createChrome() {
		chromePanel = new ColorBlock(1, 1, PANEL_COLOR);
		addToBack(chromePanel);

		//the border is optional: a host that already draws its own window frame turns it off
		if (DebugConsole.settings().isPanelBorder()) {
			chromeBorder = new ColorBlock(1, 1, BORDER_COLOR);
			addToBack(chromeBorder);
		}
	}

	/** Content size requested when the panel is first created. */
	protected int preferredWidth() {
		return 150;
	}

	protected int preferredHeight() {
		return 200;
	}

	/** True when the game currently uses a landscape layout. */
	protected boolean landscape() {
		return Game.width > Game.height;
	}

	@Override
	protected void createChildren() {
		//subclasses attach their content here
	}

	//================================================================================
	//   geometry
	//================================================================================

	public int innerWidth()  { return innerW; }
	public int innerHeight() { return innerH; }

	/** Top-left of the content area, in this panel's coordinate space. */
	public float innerX() { return padLeft; }
	public float innerY() { return padTop; }

	public float padLeft()   { return padLeft; }
	public float padTop()    { return padTop; }
	public float padRight()  { return padRight; }
	public float padBottom() { return padBottom; }

	/**
	 * Sizes the content area and re-centers the panel on screen. Named distinctly from
	 * {@link Component#setSize} to avoid an incompatible covariant-return clash.
	 */
	public void layoutPanel(int w, int h) {
		this.innerW = Math.max(1, w);
		this.innerH = Math.max(1, h);

		float totalW = innerW + padLeft + padRight;
		float totalH = innerH + padTop + padBottom;

		float originX = Math.round((Game.width - totalW) / 2f);
		float originY = Math.round((Game.height - totalH) / 2f);

		//the panel occupies its own footprint; content sits inside the padding
		super.setRect(originX + padLeft, originY + padTop, innerW, innerH);
		layoutChrome();
	}

	/** Screen-space origin of the whole panel (including border/padding). */
	public float panelX() { return x - padLeft; }
	public float panelY() { return y - padTop; }

	private void layoutChrome() {
		float totalW = innerW + padLeft + padRight;
		float totalH = innerH + padTop + padBottom;

		if (chromeBorder != null) {
			chromeBorder.x = -padLeft;
			chromeBorder.y = -padTop;
			chromeBorder.size(totalW, totalH);
		}
		if (chromePanel != null) {
			//without a border the panel fills the whole footprint; with one it insets by it
			float inset = chromeBorder == null ? 0 : BORDER_SIZE;
			chromePanel.x = -padLeft + inset;
			chromePanel.y = -padTop + inset;
			chromePanel.size(totalW - inset * 2, totalH - inset * 2);
		}
	}

	@Override
	protected void layout() {
		super.layout();
		layoutChrome();
	}

	/** Rounds a coordinate to the pixel grid. */
	public static float align(float value) {
		return Math.round(value);
	}

	/**
	 * Rounds a text element's position to the pixel grid.
	 *
	 * <p>Accepts both shapes the font seam may return: a bare {@link Visual} (bitmap label) is
	 * positioned by writing its public fields, while a {@link Component}-based text widget is
	 * moved with {@code setPos} so its own {@code layout()} runs — a component has no public
	 * {@code x}/{@code y} to write, and its children would otherwise stay unplaced.
	 */
	public static void align(Gizmo g) {
		if (g instanceof Component) {
			Component c = (Component) g;
			c.setPos(align(c.left()), align(c.top()));
		} else if (g instanceof Visual) {
			Visual v = (Visual) g;
			v.x = align(v.x);
			v.y = align(v.y);
		}
	}
}
