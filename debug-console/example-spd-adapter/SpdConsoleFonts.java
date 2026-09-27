/*
 * EGOPD — SPD-side adapter for the standalone debug console library.
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.mypd.debugconsole.ui.Fonts;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;

import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Visual;

/**
 * Supplies the game's pixel font to the standalone console library.
 *
 * <p>The library ships no font of its own (it must stay engine-agnostic), so this adapter hands
 * it {@link PixelScene#pixelFont}, giving rows the same crisp bitmap text the rest of the game
 * UI uses.
 */
public final class SpdConsoleFonts {

	private SpdConsoleFonts() {
	}

	private static boolean installed = false;

	/** Idempotent: wires the game's pixel font into the console library. */
	public static void install() {
		if (installed) return;
		installed = true;

		Fonts.setProvider(new Fonts.Provider() {
			@Override
			public Visual create(String text, int size) {
				//pixelFont is a single bitmap font; ignore the requested size and match the
				//game's own list rows so the console looks native
				BitmapText t = new BitmapText(PixelScene.pixelFont);
				t.text(text);
				t.measure();
				return t;
			}
		});
	}
}
