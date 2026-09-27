/*
 * EGOPD Debug Console — standalone debug console system
 */

package com.mypd.debugconsole.ui;

import com.watabou.input.PointerEvent;
import com.watabou.input.ScrollEvent;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.ColorBlock;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.Image;
import com.watabou.noosa.PointerArea;
import com.watabou.noosa.ScrollArea;
import com.watabou.noosa.Visual;
import com.watabou.noosa.ui.Component;

import java.util.ArrayList;

/**
 * A vertically scrollable list of {@link Row}s, driven by pointer drag and mouse wheel.
 *
 * <p>Self-contained: it does not use the host game's list widgets. Rows are laid out top to
 * bottom inside an inner content component that is translated by the scroll offset.
 *
 * <p>The list occupies a viewport rect in its parent's coordinate space. Visible rows are
 * clipped simply by the fixed {@link #itemHeight} stride — the list is meant for short, dense
 * debug entries, not long documents.
 */
public class ConsoleListView extends Component {

	private static final int SEPARATOR_COLOR = 0xFF2A2A2A;

	private final ArrayList<Row> rows = new ArrayList<>();
	private final Component content = new Component();
	private final PointerArea dragArea;
	private final ScrollArea scrollArea;

	private float offset = 0f;
	private float contentHeight = 0f;
	private int itemHeight = 18;
	private int iconBox = 16;
	private int labelSize = 7;

	private float dragStartY = 0f;
	private float dragStartOffset = 0f;

	public ConsoleListView() {
		super();

		add(content);

		//self-targeting hit area (the (x,y,w,h) constructor): covers the viewport
		dragArea = new PointerArea(0, 0, 1, 1) {
			@Override
			protected void onPointerDown(PointerEvent event) {
				dragStartY = event.current.y;
				dragStartOffset = offset;
			}

			@Override
			protected void onDrag(PointerEvent event) {
				setOffset(dragStartOffset - (event.current.y - dragStartY));
			}

			@Override
			protected void onClick(PointerEvent event) {
				//translate the click from viewport space into content space
				float localY = event.current.y - y + offset;
				for (Row row : rows) {
					if (row.handleClick(event.current.x, localY)) break;
				}
			}
		};
		dragArea.blockLevel = PointerArea.NEVER_BLOCK;
		add(dragArea);

		//mouse wheel
		scrollArea = new ScrollArea(0, 0, 1, 1) {
			@Override
			protected void onScroll(ScrollEvent event) {
				setOffset(offset - event.amount * itemHeight);
			}
		};
		add(scrollArea);
	}

	public void configure(int itemHeight, int iconBox) {
		this.itemHeight = Math.max(1, itemHeight);
		this.iconBox = Math.max(1, iconBox);
	}

	/** Sets the logical text size used for row labels. */
	public void setLabelSize(int size) {
		this.labelSize = Math.max(1, size);
	}

	public void clear() {
		content.clear();
		rows.clear();
		contentHeight = 0;
		offset = 0;
	}

	public void addRow(Row row) {
		row.iconBox = iconBox;
		row.applyLabelSize(labelSize);
		content.add(row);
		rows.add(row);
		layoutRows();
	}

	public int rowCount() {
		return rows.size();
	}

	/**
	 * The rows currently in this list, in display order.
	 *
	 * <p>Intended for tests and debug tooling: it lets a caller verify that a row really did
	 * attach its icon and label, which is the only way to catch the
	 * {@link Component}-constructor ordering trap from the outside.
	 */
	public java.util.List<Row> rows() {
		return java.util.Collections.unmodifiableList(rows);
	}

	public float offset() {
		return offset;
	}

	private void layoutRows() {
		float pos = 0;
		for (Row row : rows) {
			row.setRect(0, pos, width, itemHeight);
			pos += itemHeight;
		}
		content.setSize(width, pos);
		contentHeight = pos;
		clampOffset();
	}

	private void setOffset(float value) {
		offset = value;
		clampOffset();
	}

	private void clampOffset() {
		float max = Math.max(0f, contentHeight - height);
		if (offset < 0) offset = 0;
		if (offset > max) offset = max;
		//content is a plain Component: move it via setPos, since its fields are protected
		content.setPos(0, -offset);
	}

	@Override
	protected void layout() {
		super.layout();
		//listeners are Visuals: set their rect fields directly (Visual has no setRect)
		setViewport(scrollArea);
		setViewport(dragArea);
		layoutRows();
	}

	private void setViewport(Visual v) {
		if (v == null) return;
		v.x = x;
		v.y = y;
		v.width = width;
		v.height = height;
		v.scale.set(1, 1);
	}

	//================================================================================
	//   rows
	//================================================================================

	/** One selectable line: an optional icon plus a label. */
	public abstract static class Row extends Component {

		/** Child-kind ids for {@link #debugChildCount(int)}. */
		public static final int CHILD_ICON  = 0;
		public static final int CHILD_LABEL = 1;

		/** Displayed width of the icon column, in UI units. */
		int iconBox = 16;

		/** Logical text size, assigned by the owning list via {@link #applyLabelSize}. */
		private int labelSize = 7;

		/** The label's source text, kept so a size change can rebuild it. */
		private final String text;

		/** True for a heading: no separator line, and the host's title styling is applied. */
		private final boolean heading;

		/** The icon actually added to this row (a copy of whatever the host supplied). */
		private Image icon;

		/**
		 * The label. Typed as {@link Gizmo} rather than {@code Visual} because a host whose
		 * language needs more than ASCII returns a {@code Component}-based text widget (see
		 * {@link Fonts}). SPD's pixel font is Latin-only, so a Chinese host must be able to
		 * supply {@code RenderedTextBlock} — otherwise every name renders as question marks.
		 */
		private Gizmo label;
		private ColorBlock line;

		/**
		 * Called when the row is clicked.
		 *
		 * @return true if the click was consumed (stops the list from considering other rows)
		 */
		public abstract boolean clickConsumed();

		/**
		 * Builds the row's children.
		 *
		 * <p><b>All child construction happens here, in the constructor body — never in
		 * {@link #createChildren()}.</b> {@link Component}'s own constructor calls
		 * {@code createChildren()}, and Java runs instance field initializers only
		 * <em>after</em> {@code super()} returns. Putting {@code add(icon)} in
		 * {@code createChildren()} therefore adds a {@code null} icon and a {@code null} label,
		 * and the row renders as nothing but its 1px separator line.
		 */
		public Row(Image icon, String text) {
			this(icon, text, false);
		}

		/**
		 * @param heading true for a group heading. Headings get the host's title styling and no
		 *                separator line, so the list reads as titled sections of entries.
		 */
		public Row(Image icon, String text, boolean heading) {
			super();
			this.text = text;
			this.heading = heading;

			//copy the supplied icon: Image instances are not shareable across parents
			if (icon != null) {
				Image copy = new Image();
				copy.copy(icon);
				add(copy);
				this.icon = copy;
			}

			label = Fonts.create(text, labelSize);
			if (heading) Fonts.styleAsTitle(label);
			Fonts.addTo(this, label);

			//headings mark a section boundary themselves; a rule under them only adds noise
			if (!heading) {
				line = Fonts.separator(SEPARATOR_COLOR);
				add(line);
			}
		}

		/**
		 * Rebuilds the label at a new logical size.
		 *
		 * <p>Called by the owning list when it is configured, before the row is laid out. Text
		 * widgets size themselves at construction, so a size change means a fresh element; the
		 * old one is detached rather than resized.
		 */
		void applyLabelSize(int size) {
			int wanted = Math.max(1, size);
			if (wanted == labelSize && label != null) return;
			labelSize = wanted;

			if (label != null) remove(label);
			label = Fonts.create(text, labelSize);
			if (heading) Fonts.styleAsTitle(label);
			Fonts.addTo(this, label);
			layout();
		}

		/** True when this row is a group heading. */
		public boolean isHeading() {
			return heading;
		}

		@Override
		protected void createChildren() {
			//deliberately empty: see the constructor. Component() calls this before our
			//fields exist, so building children here would add nulls.
		}

		/**
		 * How many of the given child kind this row actually attached.
		 *
		 * <p>A testing hook. Counting the assigned fields would be useless — the whole bug this
		 * guards against is that the fields get set correctly while the <em>scene graph</em>
		 * stays empty. So this reports whether the gizmo was really added as a child.
		 */
		public int debugChildCount(int kind) {
			if (kind == CHILD_ICON) {
				return (icon != null && icon.parent == this) ? 1 : 0;
			}
			if (kind == CHILD_LABEL) {
				return (label != null && label.parent == this) ? 1 : 0;
			}
			return 0;
		}

		/** The row's display text, for tests. */
		public String debugLabelText() {
			if (label instanceof BitmapText) return ((BitmapText) label).text();
			if (label instanceof Component) {
				//a host text widget (e.g. RenderedTextBlock) exposes text() reflectively
				try {
					Object t = label.getClass().getMethod("text").invoke(label);
					if (t instanceof String) return (String) t;
				} catch (Throwable ignored) {
					//not a text widget we can read; fall through
				}
			}
			return null;
		}

		/** Hit-tests in content space, then calls {@link #clickConsumed()}. */
		boolean handleClick(float x, float y) {
			if (x >= this.x && x < this.x + width && y >= this.y && y < this.y + height) {
				return clickConsumed();
			}
			return false;
		}

		@Override
		protected void layout() {
			//a heading spans the full row with no icon column; an entry indents past its icon
			float textLeft = heading ? x : x + iconBox + 1;

			if (icon != null) {
				float iw = icon.width();
				float ih = icon.height();
				icon.x = x + (iconBox - iw) / 2f;
				icon.y = y + (height - ih) / 2f;
				ConsolePanel.align(icon);
			}

			if (label != null) {
				float lh = Fonts.heightOf(label);
				float avail = x + width - textLeft - 1;

				//shrink over-long plain labels rather than letting them spill out of the row
				Fonts.squeezeToFit(label, avail);

				Fonts.at(label, textLeft, y + (height - lh) / 2f);
				ConsolePanel.align(label);
			}

			if (line != null) {
				line.size(width, 1);
				line.x = x;
				line.y = y + height - 1;
			}
		}
	}
}
