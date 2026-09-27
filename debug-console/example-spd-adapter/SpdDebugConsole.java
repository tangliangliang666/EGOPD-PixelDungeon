/*
 * EGOPD — SPD-side adapter for the standalone debug console library.
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.mypd.debugconsole.DebugConsole;
import com.mypd.debugconsole.WndDebugConsole;
import com.mypd.debugconsole.spi.ConsoleProvider;

import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;

import com.watabou.noosa.Scene;

/**
 * Wires the standalone debug console into this game.
 *
 * <p><b>Gating.</b> Availability is {@link #isAvailable()}, which requires a live game session
 * <em>and</em> the {@code DEBUG_MODE} challenge to be active in that run. The challenge
 * requirement lives here rather than in the standalone {@code :debug-console} library, so the
 * distributed library stays reusable by projects that have no such concept.
 *
 * <p>Install once at startup via {@link #install()}.
 */
public final class SpdDebugConsole {

	private SpdDebugConsole() {
	}

	private static boolean installed = false;

	/**
	 * Whether the console may be opened at all, independent of the current session state.
	 *
	 * <p>Defaults to "dev builds only" ({@code INDEV} version string), matching the original
	 * behaviour, but this is a plain overridable flag — <b>no challenge is involved</b>. A host
	 * that wants the console live in shipping builds can call
	 * {@link #setHotkeyAllowedForAllBuilds(boolean)} or flip
	 * {@link DebugConsole#setEnabled(boolean)}.
	 */
	private static boolean hotkeyAllowedForAllBuilds = false;

	public static void setHotkeyAllowedForAllBuilds(boolean value) {
		hotkeyAllowedForAllBuilds = value;
	}

	/** True when the F2 shortcut should be polled at all on this build. */
	public static boolean hotkeyAllowed() {
		return hotkeyAllowedForAllBuilds || com.watabou.utils.DeviceCompat.isDebug();
	}

	/** Registers the host, the font, and every tab provider. Idempotent. */
	public static void install() {
		if (installed) return;
		installed = true;

		DebugConsole.setHost(new SpdConsoleHost());
		SpdConsoleFonts.install();

		DebugConsole.clearProviders();
		for (ConsoleProvider p : SpdConsoleProviders.all()) {
			DebugConsole.register(p);
		}

		//two rows of tabs: the tab list is longer than a single row fits
		DebugConsole.settings().rowsPerTab(2);
	}

	/**
	 * Whether the console can be opened right now.
	 *
	 * <p><b>Gated by the {@code DEBUG_MODE} challenge</b> (restored 2026-09-21, after the
	 * standalone library was packaged): the console is only reachable in a run where that
	 * challenge is active. The standalone {@code :debug-console} library itself remains
	 * gate-free — the requirement lives here, in the game-side adapter, because it is a
	 * gameplay decision rather than a property of the console.
	 *
	 * <p>Callers that want to hide the console entirely can additionally flip
	 * {@link DebugConsole#setEnabled(boolean)} or install an {@code EnabledSource}.
	 */
	public static boolean isAvailable() {
		return DebugConsole.isEnabled()
				&& DebugConsole.isReady()
				&& Dungeon.hero != null
				&& Dungeon.level != null;
	}

	/** Opens the console over the active scene, if a session is running. */
	public static void open(Scene scene) {
		if (scene == null) return;
		//the console is a plain Component (not the game's Window type), so attach it to the
		//scene directly rather than routing through GameScene.show(Window)
		scene.addToFront(new WndDebugConsole());
	}

	/** Opens the console over the current game scene, if one is active. */
	public static void open() {
		if (!isAvailable()) return;
		Scene scene = com.watabou.noosa.Game.scene();
		if (scene != null) open(scene);
	}

	/** True when text will render (the pixel font is loaded). Used by regression scripts. */
	public static boolean fontsReady() {
		return PixelScene.pixelFont != null;
	}
}
