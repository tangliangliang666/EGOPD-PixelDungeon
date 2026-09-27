/*
 * EGOPD Debug Console — standalone debug console system
 *
 * Generic, engine-agnostic half of a runtime debug console for libGDX games.
 *
 * This library knows nothing about any particular game. The host project plugs in
 * one or more {@link com.mypd.debugconsole.ConsoleProvider}s describing which classes
 * to expose, and a {@link com.mypd.debugconsole.ConsoleHost} describing how to name,
 * icon, and spawn them.
 */

package com.mypd.debugconsole;

import com.mypd.debugconsole.spi.ConsoleHost;
import com.mypd.debugconsole.spi.ConsoleProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Global entry point and configuration for the standalone debug console.
 *
 * <p>Replaces the host game's own "debug challenge / debug build" gating: the console is
 * enabled by a plain runtime switch that any project can flip, and the host no longer has
 * to model "debug mode" as a gameplay challenge or a version-string check.
 *
 * <p>Typical host wiring:
 * <pre>
 *   DebugConsole.setEnabled(true);                     // or drive it from settings / a build flag
 *   DebugConsole.register(new MyItemProvider());       // describe what to expose
 *   DebugConsole.setHost(new MyGameHost());            // describe how to spawn it
 *   ...
 *   if (DebugConsole.isEnabled()) scene.addToFront(DebugConsole.createWindow());
 * </pre>
 */
public final class DebugConsole {

	/** Sentinel meaning "use the library default" for {@link Settings#windowWidth} etc. */
	public static final int DEFAULT_SIZE = -1;

	private static final List<ConsoleProvider> providers = new ArrayList<>();

	private static ConsoleHost host;

	/** Master switch. Defaults to {@code true} so dropping the library in "just works". */
	private static boolean enabled = true;

	/**
	 * Persisted/queried toggle hook. When set, {@link #isEnabled()} consults it so the host can
	 * drive the switch from its own settings storage instead of holding it in a static field.
	 */
	private static EnabledSource enabledSource;

	private static final Settings settings = new Settings();

	private DebugConsole() {
	}

	//================================================================================
	//   enable / disable
	//================================================================================

	/**
	 * Supplies the enabled state, typically backed by the host's settings storage.
	 * When no source is set, the plain static switch applies.
	 */
	public interface EnabledSource {
		boolean isEnabled();
	}

	public static boolean isEnabled() {
		if (enabledSource != null) {
			try {
				return enabledSource.isEnabled();
			} catch (Throwable ignored) {
				//fall through to the static switch if the host's storage misbehaves
			}
		}
		return enabled;
	}

	/** Sets the fallback switch used when no {@link EnabledSource} is installed. */
	public static void setEnabled(boolean value) {
		enabled = value;
	}

	/** Installs a host-driven toggle. Pass {@code null} to go back to the static switch. */
	public static void setEnabledSource(EnabledSource source) {
		enabledSource = source;
	}

	//================================================================================
	//   provider registry
	//================================================================================

	/** Registers a provider contributing one tab of console entries. Idempotent per instance. */
	public static void register(ConsoleProvider provider) {
		if (provider == null) return;
		if (!providers.contains(provider)) providers.add(provider);
	}

	public static void unregister(ConsoleProvider provider) {
		providers.remove(provider);
	}

	public static void clearProviders() {
		providers.clear();
	}

	/** All registered providers, in registration order (i.e. tab order). */
	public static List<ConsoleProvider> providers() {
		return Collections.unmodifiableList(providers);
	}

	//================================================================================
	//   host
	//================================================================================

	public static ConsoleHost host() {
		return host;
	}

	/**
	 * Installs the game-side callbacks used to label, icon, and spawn entries.
	 * The console is inert until a host is installed.
	 */
	public static void setHost(ConsoleHost host) {
		DebugConsole.host = host;
	}

	public static boolean isReady() {
		return host != null && !providers.isEmpty();
	}

	//================================================================================
	//   settings
	//================================================================================

	public static Settings settings() {
		return settings;
	}

	/** Tunable presentation knobs, so hosts can restyle the console without editing the library. */
	public static class Settings {
		int windowWidth = DEFAULT_SIZE;
		int windowHeight = DEFAULT_SIZE;
		int rowsPerTab = 2;
		int listItemHeight = 18;
		int iconBoxSize = 16;
		int labelSize = 7;
		int titleLabelSize = 9;
		int tabLabelSize = 9;
		int tabHeight = 25;
		boolean autoSizeWindow = true;
		boolean panelBorder = true;

		public Settings rowsPerTab(int rows) {
			this.rowsPerTab = Math.max(1, rows);
			return this;
		}

		/** Force a fixed window size (in UI units). Pass {@link #DEFAULT_SIZE} to auto-size. */
		public Settings windowSize(int w, int h) {
			this.windowWidth = w;
			this.windowHeight = h;
			this.autoSizeWindow = (w == DEFAULT_SIZE || h == DEFAULT_SIZE);
			return this;
		}

		public Settings listItemHeight(int h) {
			this.listItemHeight = Math.max(1, h);
			return this;
		}

		public Settings iconBoxSize(int size) {
			this.iconBoxSize = Math.max(1, size);
			return this;
		}

		public Settings labelSize(int size) {
			this.labelSize = Math.max(1, size);
			return this;
		}

		/** Logical size for group headings, typically one step above {@link #labelSize}. */
		public Settings titleLabelSize(int size) {
			this.titleLabelSize = Math.max(1, size);
			return this;
		}

		public Settings tabLabelSize(int size) {
			this.tabLabelSize = Math.max(1, size);
			return this;
		}

		public Settings tabHeight(int h) {
			this.tabHeight = Math.max(1, h);
			return this;
		}

		/**
		 * Whether to draw the 1px panel border.
		 *
		 * <p>Hosts that already draw a window frame (an {@code SPD} {@code Window}, say) turn
		 * this off so the console does not trade its own flat rectangle for the host's art.
		 */
		public Settings panelBorder(boolean value) {
			this.panelBorder = value;
			return this;
		}

		//---- read accessors ------------------------------------------------------
		//The widget classes live in a sub-package, so they cannot read these fields
		//directly. Prefixed with get- because the fluent setters above already own the
		//bare names (Java cannot overload on return type alone).

		public int getWindowWidth()      { return windowWidth; }
		public int getWindowHeight()     { return windowHeight; }
		public int getRowsPerTab()       { return rowsPerTab; }
		public int getListItemHeight()   { return listItemHeight; }
		public int getIconBoxSize()      { return iconBoxSize; }
		public int getLabelSize()        { return labelSize; }
		public int getTitleLabelSize()   { return titleLabelSize; }
		public int getTabLabelSize()     { return tabLabelSize; }
		public int getTabHeight()        { return tabHeight; }
		public boolean isAutoSizeWindow() { return autoSizeWindow; }
		public boolean isPanelBorder()   { return panelBorder; }
	}
}
