/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole.ui;

import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

/**
 * Factory for the text used by the console. The library ships no font of its own — the host
 * supplies one (a bitmap font for crisp ASCII text, or a rendered-text widget when the host's
 * language needs glyphs a bitmap font cannot provide).
 *
 * <p>Implement {@link Provider} and install it with {@link #setProvider(Provider)}. With no
 * provider installed, rows render icon-only rather than crashing, so a host can wire text in
 * later without the console breaking in the meantime.
 *
 * <h3>Why the abstraction is {@link Gizmo}, not {@link Visual}</h3>
 *
 * <p>It is tempting to type everything as {@code Visual} — that is what a bitmap label is. But
 * engines commonly wrap their text pipeline in a <em>container</em> rather than a bare visual:
 * Shattered Pixel Dungeon's {@code RenderedTextBlock} extends {@code Component} (which extends
 * {@code Group}), because one logical block owns several per-word visuals plus optional
 * strike-through lines.
 *
 * <p>That distinction is not cosmetic. SPD's only ASCII-safe font is its 3x5 pixel font, whose
 * charset is {@code LATIN_FULL}; every non-Latin character silently falls back to {@code '?'}.
 * A host whose UI language is Chinese (or Japanese, Korean, …) <em>must</em> be able to hand
 * back a {@code Component}-based text widget, or every label renders as a row of question marks.
 * Typing this seam as {@code Gizmo} is what makes that possible while keeping the library
 * engine-agnostic.
 *
 * <p>Because a {@code Gizmo} has no geometry of its own, the helpers below normalise the two
 * shapes: position/measure go through {@link #at}/{@link #widthOf}/{@link #heightOf}, which
 * understand both {@link Visual} and {@link Component}.
 */
public final class Fonts {

	private Fonts() {
	}

	/** Supplies text elements to the console. */
	public interface Provider {
		/**
		 * Creates a text element for the given logical size (in UI units), or {@code null} to
		 * degrade that row to icon-only.
		 *
		 * <p>The returned gizmo may be a plain {@link Visual} (e.g. a {@code BitmapText}) or a
		 * {@link Component}-based text widget (e.g. a rendered-text block). Both are supported.
		 */
		Gizmo create(String text, int size);

		/**
		 * Restyles a text element as a group heading, in place.
		 *
		 * <p>Optional: the default does nothing. Hosts that have their own "title" treatment
		 * (a tinted colour, a different weight) implement this so console headings match the
		 * rest of their UI. {@code g} is whatever {@link #create} returned.
		 */
		default void styleAsTitle(Gizmo g) {
			//no host styling by default
		}
	}

	private static Provider provider;

	public static void setProvider(Provider p) {
		provider = p;
	}

	public static Provider provider() {
		return provider;
	}

	/** Creates a text gizmo, or {@code null} when no provider is installed (or it fails). */
	public static Gizmo create(String text, int size) {
		if (provider == null) return null;
		try {
			return provider.create(text, size);
		} catch (Throwable t) {
			return null;
		}
	}

	/** Asks the provider to restyle a text gizmo as a group heading. Best-effort. */
	public static void styleAsTitle(Gizmo g) {
		if (g == null || provider == null) return;
		try {
			provider.styleAsTitle(g);
		} catch (Throwable ignored) {
			//title styling is cosmetic; a plain heading is acceptable
		}
	}

	/** Applies a hardlight tint where supported. Silently ignored for unsupported shapes. */
	public static void tint(Gizmo g, int color) {
		if (g == null) return;
		try {
			if (g instanceof BitmapText) {
				((BitmapText) g).hardlight(color);
			} else if (g instanceof Image) {
				((Image) g).hardlight(color);
			} else if (g instanceof Component) {
				//rendered-text containers expose hardlight() without implementing a shared
				//interface; call it reflectively so the library stays free of host types
				try {
					g.getClass().getMethod("hardlight", int.class).invoke(g, color);
				} catch (Throwable ignored) {
					//not a tintable text widget; leave it as-is
				}
			}
		} catch (Throwable ignored) {
			//tinting is best-effort
		}
	}

	/** Sets opacity where supported. Silently ignored for unsupported shapes. */
	public static void alpha(Gizmo g, float value) {
		if (g == null) return;
		try {
			if (g instanceof Visual) {
				((Visual) g).alpha(value);
			} else if (g instanceof Component) {
				try {
					g.getClass().getMethod("alpha", float.class).invoke(g, value);
				} catch (Throwable ignored) {
					//not a fadeable text widget; leave it as-is
				}
			}
		} catch (Throwable ignored) {
			//alpha is best-effort
		}
	}

	/**
	 * The element's rendered width.
	 *
	 * <p>A {@code Visual} reports its size through {@code width()}; a {@code Component} shadows
	 * that with its own {@code width()} (its geometry lives in protected fields reachable only
	 * through these accessors). Both are called the same way, which is exactly what lets the
	 * seam stay shape-agnostic.
	 */
	public static float widthOf(Gizmo g) {
		if (g == null) return 0f;
		if (g instanceof Visual) return ((Visual) g).width();
		if (g instanceof Component) return ((Component) g).width();
		return 0f;
	}

	/** The element's rendered height. See {@link #widthOf}. */
	public static float heightOf(Gizmo g) {
		if (g == null) return 0f;
		if (g instanceof Visual) return ((Visual) g).height();
		if (g instanceof Component) return ((Component) g).height();
		return 0f;
	}

	/**
	 * Positions an element in its parent's coordinate space.
	 *
	 * <p>A {@code Visual} is moved by writing its public {@code x}/{@code y}. A
	 * {@code Component} must go through {@code setPos}, which also triggers its
	 * {@code layout()} — writing the protected fields directly would leave the widget's own
	 * children unlaid-out and the text would appear at the wrong offset.
	 */
	public static void at(Gizmo g, float x, float y) {
		if (g == null) return;
		if (g instanceof Component) {
			((Component) g).setPos(x, y);
		} else if (g instanceof Visual) {
			Visual v = (Visual) g;
			v.x = x;
			v.y = y;
		}
	}

	/**
	 * Horizontally scales a text element down so it fits in {@code avail} units.
	 *
	 * <p>Only applied to plain visuals. A {@code Component}-based text widget owns its own
	 * layout and scale, so squeezing it here would fight with {@code setPos}/{@code layout}.
	 */
	public static void squeezeToFit(Gizmo g, float avail) {
		if (!(g instanceof Visual) || g instanceof Component) return;
		Visual v = (Visual) g;
		float w = v.width();
		if (w > avail && w > 0) {
			v.scale.x = Math.max(0.5f, avail / w);
		}
	}

	/** Adds a possibly-null gizmo to a group. */
	public static void addTo(Group parent, Gizmo g) {
		if (parent == null || g == null) return;
		parent.add(g);
	}

	/** A 1px separator line. */
	public static ColorBlock separator(int color) {
		return new ColorBlock(1, 1, color);
	}
}
