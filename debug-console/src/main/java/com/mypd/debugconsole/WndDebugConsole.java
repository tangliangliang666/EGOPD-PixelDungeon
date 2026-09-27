/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole;

import com.mypd.debugconsole.spi.ConsoleHost;
import com.mypd.debugconsole.spi.ConsoleProvider;
import com.mypd.debugconsole.ui.ConsoleListView;
import com.mypd.debugconsole.ui.ConsolePanel;
import com.mypd.debugconsole.ui.Fonts;

import com.watabou.input.PointerEvent;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The console window: a tab bar beneath the content area, one scrollable list per registered
 * {@link ConsoleProvider}.
 *
 * <p>This replaces the project-specific debug window. It is fully generic — it never mentions a
 * game type; naming, icons and spawning all route through {@link ConsoleHost}.
 *
 * <p>Tabs build lazily: the first time a tab is opened its provider's
 * {@link ConsoleProvider#classes()} runs, so an expensive classpath scan is only paid for on use.
 */
public class WndDebugConsole extends ConsolePanel {

	private static final int TAB_ROW_GAP = 1;

	//The tab/pane lists are populated in the constructor body, NOT in createChildren():
	//ConsolePanel's constructor calls createChildren() before our field initialisers run,
	//so building panes there would hit a null list.
	private final ArrayList<Tab> tabs = new ArrayList<>();
	private final ArrayList<Pane> panes = new ArrayList<>();

	private int selected = -1;

	public WndDebugConsole() {
		super();

		buildPanes();
		buildTabs();
		layoutPanel(preferredWidth(), preferredHeight());

		if (!tabs.isEmpty()) select(0);
	}

	//================================================================================
	//   building
	//================================================================================

	@Override
	protected int preferredWidth() {
		DebugConsole.Settings s = DebugConsole.settings();
		if (!s.isAutoSizeWindow() && s.getWindowWidth() > 0) return s.getWindowWidth();
		return landscape() ? 230 : 150;
	}

	@Override
	protected int preferredHeight() {
		DebugConsole.Settings s = DebugConsole.settings();
		if (!s.isAutoSizeWindow() && s.getWindowHeight() > 0) return s.getWindowHeight();
		return landscape() ? 150 : 200;
	}

	@Override
	protected void createChildren() {
		//deliberately empty: the panel's own chrome is built by ConsolePanel.createChrome(),
		//and the tabs/panes are built in the constructor body (see buildPanes/buildTabs).
		//Building them here would run before our fields are initialised.
	}

	/** Instantiates one pane per available provider. Called from the constructor body. */
	private void buildPanes() {
		for (ConsoleProvider p : DebugConsole.providers()) {
			if (p == null || !p.available()) continue;
			Pane pane = new Pane(p);
			panes.add(pane);
			//panes are laid out manually in layoutPanel(); hidden until selected
			pane.visible = pane.active = false;
			add(pane);
		}
	}

	private void buildTabs() {
		for (int i = 0; i < panes.size(); i++) {
			final int index = i;
			Tab tab = new Tab(panes.get(i).provider.tabName(), index == selected);
			add(tab);
			tabs.add(tab);
		}
	}

	private int tabRows() {
		return Math.max(1, DebugConsole.settings().getRowsPerTab());
	}

	private int tabHeight() {
		return Math.max(1, DebugConsole.settings().getTabHeight());
	}

	/** Distributes tabs evenly across the configured number of rows, below the content area. */
	private void layoutTabs() {
		if (tabs.isEmpty()) return;

		int rows = tabRows();
		int perRow = (int) Math.ceil(tabs.size() / (float) rows);
		if (perRow <= 0) perRow = tabs.size();

		float fullWidth = innerW;
		float tabWidth = (fullWidth - (perRow - 1)) / perRow;

		float posX = innerX();
		float barTop = innerY() + innerH + 2;

		for (int i = 0; i < tabs.size(); i++) {
			Tab tab = tabs.get(i);
			int row = i / perRow;
			tab.setSize(tabWidth, tabHeight());
			tab.setPos(posX, barTop + row * (tabHeight() + TAB_ROW_GAP));
			posX = tab.right() + 1;
			if ((i + 1) % perRow == 0) posX = innerX();
		}
	}

	@Override
	public void layoutPanel(int w, int h) {
		super.layoutPanel(w, h);

		for (Pane pane : panes) {
			pane.setRect(innerX(), innerY(), innerW, innerH);
		}
		layoutTabs();
	}

	//================================================================================
	//   selection
	//================================================================================

	public void select(int index) {
		if (index < 0 || index >= panes.size()) return;
		if (selected == index) return;
		selected = index;

		for (int i = 0; i < panes.size(); i++) {
			Pane pane = panes.get(i);
			boolean active = (i == index);
			pane.visible = pane.active = active;
			if (active) pane.ensureBuilt();
		}
		for (int i = 0; i < tabs.size(); i++) {
			tabs.get(i).setSelected(i == index);
		}
	}

	/** Closes the console. */
	public void hide() {
		visible = active = false;
	}

	/**
	 * The rows currently shown by the selected tab, in display order.
	 *
	 * <p>A testing hook: it is the only way for an external harness to confirm that rows
	 * actually attached their icon and label (see {@link ConsoleListView.Row#debugChildCount}),
	 * which is what catches the {@code Component}-constructor ordering trap.
	 */
	public java.util.List<ConsoleListView.Row> debugRows() {
		if (selected < 0 || selected >= panes.size()) {
			return java.util.Collections.emptyList();
		}
		Pane pane = panes.get(selected);
		pane.ensureBuilt();
		return pane.list == null ? java.util.Collections.<ConsoleListView.Row>emptyList()
				: pane.list.rows();
	}

	//================================================================================
	//   tab button
	//================================================================================

	private class Tab extends Component {

		private boolean selectedState;
		private ColorBlock bg;
		/** Tab caption: a plain visual or a host text widget (see {@link Fonts}). */
		private Gizmo label;
		private PointerArea hitArea;

		/**
		 * Builds the tab's children.
		 *
		 * <p>Like {@link Pane}, everything is created here rather than in
		 * {@link #createChildren()}: {@code Component()}'s constructor calls that method before
		 * this class's field initializers run, so a field assigned inside it is reset to its
		 * default immediately afterwards — the gizmo would be in the scene graph but the field
		 * holding it would read {@code null}, silently disabling {@link #recolor()} and the
		 * {@link #layout()} that sizes the hit area.
		 */
		Tab(String text, boolean initiallySelected) {
			super();
			this.selectedState = initiallySelected;

			bg = new ColorBlock(1, 1, 0xFF2A2A2A);
			addToBack(bg);

			//self-targeting PointerArea covering this tab
			hitArea = new PointerArea(0, 0, 1, 1) {
				@Override
				protected void onClick(PointerEvent event) {
					int index = tabs.indexOf(Tab.this);
					if (index >= 0) select(index);
				}
			};
			add(hitArea);

			this.label = Fonts.create(text, DebugConsole.settings().getTabLabelSize());
			Fonts.addTo(this, label);

			recolor();
		}

		@Override
		protected void createChildren() {
			//deliberately empty: see the constructor.
		}

		void setSelected(boolean value) {
			if (selectedState == value) return;
			selectedState = value;
			recolor();
			layout();
		}

		private void recolor() {
			if (bg != null) bg.color(selectedState ? 0xFF4F4F4F : 0xFF2A2A2A);
			if (label != null) Fonts.alpha(label, selectedState ? 1f : 0.6f);
		}

		@Override
		protected void layout() {
			if (bg != null) {
				bg.x = x;
				bg.y = y;
				bg.size(width, height);
			}
			if (hitArea != null) {
				//hitArea is a Visual: position it directly to avoid the covariant setRect clash
				hitArea.x = x;
				hitArea.y = y;
				hitArea.scale.set(width, height);
			}
			if (label != null) {
				float lw = Fonts.widthOf(label);
				float lh = Fonts.heightOf(label);
				//mirrors WndTabbed.LabeledTab.layout(): an unselected tab sits 2px lower, which
				//reads as the selected one lifting out of the row
				float lift = selectedState ? 1 : 3;
				Fonts.at(label, x + (width - lw) / 2f, y + (height - lh) / 2f - lift);
				ConsolePanel.align(label);
			}
		}
	}

	//================================================================================
	//   one provider's pane
	//================================================================================

	private class Pane extends Component {

		final ConsoleProvider provider;
		private ConsoleListView list;
		private boolean built = false;

		/**
		 * Builds the pane's list.
		 *
		 * <p>The list is created in the constructor body, not in {@link #createChildren()}.
		 * {@link Component}'s constructor calls {@code createChildren()} first, and the field
		 * initializers ({@code list = null}) run <em>after</em> that — so anything assigned to a
		 * field inside {@code createChildren()} is immediately overwritten with its default and
		 * silently lost. Creating it here keeps the assignment last.
		 */
		Pane(ConsoleProvider provider) {
			super();
			this.provider = provider;

			list = new ConsoleListView();
			DebugConsole.Settings s = DebugConsole.settings();
			list.configure(s.getListItemHeight(), s.getIconBoxSize());
			list.setLabelSize(s.getLabelSize());
			add(list);
		}

		@Override
		protected void createChildren() {
			//deliberately empty: see the constructor. Component() calls this before our
			//field initializers run, so a list created here would be wiped back to null.
		}

		/** Builds rows on first open. One broken provider must not take down the whole window. */
		void ensureBuilt() {
			if (built) return;
			built = true;
			try {
				buildRows();
			} catch (Throwable t) {
				Game.reportException(t);
			}
		}

		private void buildRows() {
			String title = provider.groupTitle();
			if (title != null && !title.isEmpty()) {
				list.addRow(new ConsoleListView.Row(null, title, true) {
					@Override public boolean clickConsumed() { return true; }
				});
			}

			List<Class<?>> classes = provider.classes();
			if (classes == null) return;

			int max = provider.maxEntries();
			int count = 0;

			for (final Class<?> c : classes) {
				if (c == null) continue;
				if (max > 0 && count >= max) break;
				count++;

				try {
					list.addRow(new ConsoleListView.Row(iconFor(c), nameFor(c)) {
						@Override
						public boolean clickConsumed() {
							WndDebugConsole.this.onEntryClicked(c);
							return true;
						}
					});
				} catch (Throwable t) {
					//skip this entry, keep the rest of the list
				}
			}
		}

		@Override
		protected void layout() {
			super.layout();
			if (list != null) list.setRect(x, y, width, height);
		}
	}

	//================================================================================
	//   entry helpers
	//================================================================================

	private String nameFor(Class<?> c) {
		ConsoleHost host = DebugConsole.host();
		if (host != null) {
			try {
				String name = host.displayName(c);
				if (name != null && !name.isEmpty()) return name;
			} catch (Throwable ignored) {
				//fall through to the class name
			}
		}
		return c.getSimpleName();
	}

	private Image iconFor(Class<?> c) {
		ConsoleHost host = DebugConsole.host();
		if (host == null) return null;
		try {
			return host.icon(c);
		} catch (Throwable t) {
			return null;
		}
	}

	/** Routes a click to the host, preferring world-targeted placement when it asks for one. */
	private void onEntryClicked(final Class<?> c) {
		final ConsoleHost host = DebugConsole.host();
		if (host == null) return;

		try {
			if (host.hidesBeforeSpawning(c)) {
				boolean handled = host.requestPlacement(c, new Runnable() {
					@Override
					public void run() {
						//placement finished; the console intentionally stays closed
					}
				});
				if (handled) {
					hide();
					return;
				}
			}
			host.spawn(c);
		} catch (Throwable t) {
			Game.reportException(t);
		}
	}
}
