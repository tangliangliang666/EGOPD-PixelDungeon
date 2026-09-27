/*
 * EGOPD — game-side gate for the debug window.
 *
 * The debug window itself is now the SPD-native {@code windows.WndDebug} (a {@code WndTabbed}
 * of {@code ScrollingListPane}s), so this class no longer wires up the standalone
 * {@code :debug-console} library. It is only the <em>gate</em>: whether the window may open at
 * all, kept here so the window's own code stays free of challenge/build policy.
 */

package com.shatteredpixel.shatteredpixeldungeon.debug;

import com.shatteredpixel.shatteredpixeldungeon.Challenges;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;

import com.watabou.utils.DeviceCompat;

/**
 * Whether the debug window ({@code windows.WndDebug}) may be opened.
 *
 * <p>The window is game-native and has no dependency on the standalone {@code :debug-console}
 * library, which still ships independently for other projects. This class only carries the
 * gameplay/build policy that decides <em>when</em> the window is reachable.
 */
public final class SpdDebugConsole {

	private SpdDebugConsole() {
	}

	/**
	 * Defaults to "dev builds only" ({@code DeviceCompat.isDebug()}), matching the original
	 * behaviour. A host that wants the window live in shipping builds can override it.
	 */
	private static boolean hotkeyAllowedForAllBuilds = false;

	public static void setHotkeyAllowedForAllBuilds(boolean value) {
		hotkeyAllowedForAllBuilds = value;
	}

	/** True when the F2 shortcut should be polled at all on this build. */
	public static boolean hotkeyAllowed() {
		return hotkeyAllowedForAllBuilds || DeviceCompat.isDebug();
	}

	/**
	 * Whether the window can be opened right now: a live run, plus the {@code DEBUG_MODE}
	 * challenge active in that run.
	 */
	public static boolean isAvailable() {
		return Dungeon.hero != null
				&& Dungeon.level != null
				&& isDebugChallengeActive();
	}

	/** True when the current run has the debug-mode challenge enabled. */
	public static boolean isDebugChallengeActive() {
		return Dungeon.isChallenged(Challenges.DEBUG_MODE);
	}
}
