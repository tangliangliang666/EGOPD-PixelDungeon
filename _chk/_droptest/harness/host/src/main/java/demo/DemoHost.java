package demo;

import com.mypd.debugconsole.DebugConsole;
import com.mypd.debugconsole.ClassScanner;
import com.mypd.debugconsole.WndDebugConsole;
import com.mypd.debugconsole.spi.ConsoleHost;
import com.mypd.debugconsole.spi.ConsoleProvider;
import com.mypd.debugconsole.ui.ConsoleListView;
import com.mypd.debugconsole.ui.Fonts;

import com.watabou.noosa.Image;

import java.util.List;

/** End-to-end usage, written only from README/DROP-IN.md. */
public class DemoHost implements ConsoleHost {

    //Non-ASCII on purpose: this is the case a Latin-only bitmap font silently mangles,
    //and it is exactly why the font seam must be able to return a Component-based widget.
    @Override public String displayName(Class<?> c) { return "名称_" + c.getSimpleName(); }
    //return a real icon so the icon path is actually exercised (a null-returning stub
    //would let an "icon never gets added" bug slip through unnoticed)
    @Override public Image icon(Class<?> c) {
        Image img = new Image();
        img.width = 16;
        img.height = 16;
        return img;
    }
    @Override public String spawn(Class<?> c) { return "spawned " + c.getSimpleName(); }
    @Override public boolean hidesBeforeSpawning(Class<?> c) { return false; }
    @Override public boolean requestPlacement(Class<?> c, Runnable onPlaced) { return false; }

    public static void main(String[] args) {
        //Supply a Component-based text widget (as a CJK host must), NOT a BitmapText.
        //The seam is typed Gizmo precisely so this is possible; typing it Visual would
        //compile this line out of existence and every non-ASCII label would be lost.
        Fonts.setProvider(new Fonts.Provider() {
            @Override public com.watabou.noosa.Gizmo create(String text, int size) {
                return new com.watabou.noosa.ui.BlockText(text);
            }
            @Override public void styleAsTitle(com.watabou.noosa.Gizmo g) {
                if (g instanceof com.watabou.noosa.ui.BlockText) {
                    ((com.watabou.noosa.ui.BlockText) g).hardlight(0xFFFF00);
                }
            }
        });

        DebugConsole.setHost(new DemoHost());
        DebugConsole.setEnabled(true);
        DebugConsole.settings().rowsPerTab(2);

        DebugConsole.register(new ConsoleProvider() {
            @Override public String tabName() { return "Components"; }
            @Override public List<Class<?>> classes() {
                return ClassScanner.find("demo", Object.class);
            }
        });

        if (!DebugConsole.isEnabled()) { fail("isEnabled() should default to true"); return; }
        if (!DebugConsole.isReady())   { fail("isReady() false after host+provider registration"); return; }

        WndDebugConsole wnd = new WndDebugConsole();
        if (wnd.innerWidth() <= 0 || wnd.innerHeight() <= 0) {
            fail("panel has no usable size");
            return;
        }

        // Regression guard for the Component-constructor ordering trap.
        //
        // Component() calls createChildren() before subclass field initializers run, so any
        // row that adds its icon/label from createChildren() ends up adding nulls and renders
        // as a bare separator line -- the window opens, but every row looks empty. Asserting
        // only "the window got a size" (as this harness used to) does NOT catch that.
        //
        // Rows must therefore expose how many children they actually attached.
        int rows = 0, iconRows = 0, labelRows = 0;
        for (ConsoleListView.Row row : wnd.debugRows()) {
            rows++;
            if (row.debugChildCount(ConsoleListView.Row.CHILD_ICON) > 0)  iconRows++;
            if (row.debugChildCount(ConsoleListView.Row.CHILD_LABEL) > 0) labelRows++;
        }
        if (rows == 0) { fail("no rows were built"); return; }
        if (iconRows < rows) {
            fail("only " + iconRows + "/" + rows + " rows attached their icon"
                 + " (Row likely builds children in createChildren())");
            return;
        }
        if (labelRows < rows) {
            fail("only " + labelRows + "/" + rows + " rows attached their label");
            return;
        }

        // A label child that exists but carries no text is the "empty list" failure mode.
        // Checking the text also proves the font seam survived a host-supplied
        // Component-based widget (a Visual-typed seam could not carry one at all).
        int withText = 0;
        String first = null;
        for (ConsoleListView.Row row : wnd.debugRows()) {
            String t = row.debugLabelText();
            if (t != null && !t.isEmpty()) { withText++; if (first == null) first = t; }
        }
        if (withText < rows) {
            fail("only " + withText + "/" + rows + " rows kept their label text"
                 + " (font seam dropped a Component-based text widget?)");
            return;
        }
        if (first == null || !first.contains("名称")) {
            fail("label text was not preserved non-ASCII: " + first);
            return;
        }

        System.out.println("PASS: console built " + wnd.innerWidth() + "x" + wnd.innerHeight()
                + ", scanning found " + DebugConsole.providers().get(0).classes().size() + " class(es)"
                + ", rows=" + rows + " all with icon+label, text preserved (e.g. " + first + ")");
    }

    private static void fail(String msg) {
        System.out.println("FAIL: " + msg);
        System.exit(1);
    }
}
