/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole.spi;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.watabou.noosa.Image;

/**
 * The game-side half of the console: everything the library needs to know about a host's
 * object model but must not depend on.
 *
 * <p>Implement this once per project and install it with
 * {@link com.mypd.debugconsole.DebugConsole#setHost(ConsoleHost)}. Every method must be
 * defensive — the console calls into host code speculatively while enumerating classes, so
 * returning {@code null} or throwing is tolerated (the offending row is simply skipped).
 */
public interface ConsoleHost {

	/**
	 * Human-readable name for a spawnable class, e.g. {@code "Potion of Healing"}.
	 * Return {@code null} to let the console fall back to {@link Class#getSimpleName()}.
	 */
	String displayName(Class<?> clazz);

	/**
	 * Icon for a spawnable class, or {@code null} for a text-only row.
	 * The library never mutates the returned object, but it may {@link Image#copy} from it.
	 */
	Image icon(Class<?> clazz);

	/**
	 * Spawns one instance of {@code clazz}. Called on the render thread.
	 *
	 * @return a short outcome message shown to the player, or {@code null} to stay silent
	 */
	String spawn(Class<?> clazz);

	/**
	 * When true, the console hides itself before spawning so the player can see the game world
	 * (used together with {@link #requestPlacement(Class, Runnable)}).
	 */
	boolean hidesBeforeSpawning(Class<?> clazz);

	/**
	 * Optional hook for classes that need a click on the game world to place (e.g. spawning a
	 * monster at a chosen tile). The console must be hidden first.
	 *
	 * <p>Implementations should call {@code onPlaced} once the placement finishes (or is
	 * cancelled) so the console can restore itself.
	 *
	 * @return {@code true} if the host took over placement; {@code false} to use plain
	 *         {@link #spawn(Class)}
	 */
	boolean requestPlacement(Class<?> clazz, Runnable onPlaced);
}
