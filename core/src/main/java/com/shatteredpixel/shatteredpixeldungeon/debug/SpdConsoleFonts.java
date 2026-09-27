/*
 * EGOPD — SPD-side adapter for the standalone debug console library.
 *
 * NOTE (2026-09-21): the in-game debug window is now the SPD-native windows/WndDebug
 * (WndTabbed + ScrollingListPane), so this adapter is no longer wired up by SpdDebugConsole.
 * It is kept as the reference implementation for connecting the :debug-console library to this
 * game, and as a copy source for other projects (see debug-console/example-spd-adapter).
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.mypd.debugconsole.ui.Fonts;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.RenderedTextBlock;
import com.shatteredpixel.shatteredpixeldungeon.ui.Window;

import com.watabou.noosa.Gizmo;

/**
 * Supplies the game's text rendering to the standalone console library.
 *
 * <h3>Why {@link RenderedTextBlock} and not the pixel font</h3>
 *
 * <p>The obvious choice is {@link PixelScene#pixelFont}: it is the game's own bitmap font, it
 * looks native, and {@code BitmapText} with it is what most of the HUD uses. It is also
 * <b>Latin-only</b> — {@link PixelScene} loads it with {@code BitmapText.Font.LATIN_FULL}, and
 * {@code BitmapText.Font.get()} falls back to {@code '?'} for any character outside that set.
 *
 * <p>That makes it unusable for this game's UI language. Every item, mob and buff name here is
 * Chinese, resolved through {@code messages/*_zh.properties}, so a bitmap label renders <em>the
 * entire list</em> as rows of question marks. (This was the "names don't match" bug: the names
 * were correct all along, they just had no glyphs.)
 *
 * <p>{@link RenderedTextBlock} renders through libGDX's FreeType pipeline instead, which loads
 * real font files and therefore covers CJK. It is also what the rest of this game's list UIs
 * use — {@code ScrollingListPane.ListItem} builds its label the same way — so this choice both
 * fixes the glyphs and matches the original debug window's appearance.
 *
 * <p>{@code RenderedTextBlock} is a {@code Component}, not a {@code Visual}; the library's font
 * seam is typed as {@code Gizmo} precisely so this can be handed back (see {@code Fonts}).
 */
public final class SpdConsoleFonts {

	private SpdConsoleFonts() {
	}

	private static boolean installed = false;

	/** Idempotent: wires the game's text rendering into the console library. */
	public static void install() {
		if (installed) return;
		installed = true;

		Fonts.setProvider(new Fonts.Provider() {
			@Override
			public Gizmo create(String text, int size) {
				//match the size the console asked for: this is the game's own text widget, so
				//its size argument works exactly as it does elsewhere in the UI
				RenderedTextBlock block = PixelScene.renderTextBlock(size);
				block.text(text);
				block.setHightlighting(false);
				return block;
			}

			@Override
			public void styleAsTitle(Gizmo g) {
				//mirrors ScrollingListPane.ListTitle: headings are tinted with the game's title
				//colour so console sections look like every other titled list in the game
				if (g instanceof RenderedTextBlock) {
					((RenderedTextBlock) g).hardlight(Window.TITLE_COLOR);
				}
			}
		});
	}

	/**
	 * True when text rendering is available (the pixel font is loaded).
	 *
	 * <p>A sanity gate for the wiring: {@code RenderedTextBlock} needs the platform font
	 * generators, which {@code PixelScene.create()} installs, so this doubles as "the scene is
	 * far enough along to build console rows".
	 */
	public static boolean ready() {
		return PixelScene.pixelFont != null;
	}
}
