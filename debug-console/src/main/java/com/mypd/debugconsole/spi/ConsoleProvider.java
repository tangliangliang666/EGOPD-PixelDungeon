/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole.spi;

import java.util.List;

/**
 * Contributes one tab of entries to the console.
 *
 * <p>A provider is the unit of extensibility: implement one per category the host wants to
 * expose (items, monsters, buffs, effects, levels, cheat commands, ...). Each provider becomes
 * one tab, in registration order.
 *
 * <p>Providers are queried lazily — {@link #classes()} is called the first time the tab is
 * opened, not at startup — so an expensive classpath scan costs nothing until asked for.
 */
public interface ConsoleProvider {

	/** Tab caption, e.g. {@code "Weapons"}. */
	String tabName();

	/**
	 * Classes this tab exposes. Each becomes one row; the host's
	 * {@link ConsoleHost#displayName(Class)} names it and {@link ConsoleHost#spawn(Class)}
	 * invokes it.
	 *
	 * <p>Implementations typically delegate to
	 * {@link com.mypd.debugconsole.ClassScanner#find(String[], Class, boolean)}. Returning an
	 * empty list is fine; the tab will show as empty rather than failing.
	 */
	List<Class<?>> classes();

	/** Optional grouping label rendered above the entries, or {@code null} for none. */
	default String groupTitle() {
		return null;
	}

	/**
	 * Cap on how many rows to show, or {@code 0}/{@code -1} for unlimited. Long lists are the
	 * usual reason a debug tab feels sluggish, so providers scanning huge packages may want to
	 * cap themselves.
	 */
	default int maxEntries() {
		return 0;
	}

	/** Whether this tab should be built at all. Lets hosts gate a tab on a build flag or setting. */
	default boolean available() {
		return true;
	}
}
