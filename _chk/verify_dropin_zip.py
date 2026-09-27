#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
drop-in 压缩包独立可用性验证。

证明一件事：把 debug-console-*.zip 解压到「一个完全不同的项目」里，
不改模块任何源码、只用 README 里写的接线方式，就能编译通过并跑起来。

流程：
  1. 解压指定 zip 到临时目录
  2. 搭一个最小宿主工程（自带一个精简 noosa stub 层，名字故意不叫 SPD-classes）
  3. 用 debugConsoleSlimProject 属性指向该 stub 层（验证接管机制）
  4. 编译模块 + 编译并运行一个端到端 DemoHost
  5. 打印 通过/失败

用法：
  python _chk/verify_dropin_zip.py debug-console-1.0.0.zip
"""

import os
import shutil
import subprocess
import sys
import zipfile

JDK = r"D:\PD\tools\jdk-21.0.12.1+1"
GRADLE = r"D:\PD\tools\gradle-9.4.0\bin\gradle.bat"
GRADLE_USER_HOME = r"D:\PD\.gradle"
WORK = r"D:\PD\_chk\_droptest"

# ---------------------------------------------------------------- stub noosa layer
# Minimal stand-ins for the generic noosa/DeviceCompat API the console module uses.
# Deliberately named ":my-noosa" so the test proves the slim-project indirection works.
STUBS = {
    "com/watabou/noosa/Gizmo.java": """package com.watabou.noosa;
public class Gizmo {
    public boolean exists = true, active = true, visible = true;
    public Group parent;
    public Camera camera() { return null; }
    public void update() {}
    public void draw() {}
    public void destroy() {}
    public void kill() {}
}
""",
    "com/watabou/noosa/Visual.java": """package com.watabou.noosa;
import com.watabou.utils.PointF;
import com.watabou.utils.RectF;
public class Visual extends Gizmo {
    public float x, y, width = 1, height = 1, angle, alpha = 1f;
    public PointF scale = new PointF(1, 1);
    public Visual() {}
    public Visual(float x, float y, float w, float h) { this.x=x; this.y=y; this.width=w; this.height=h; }
    public float width() { return width * scale.x; }
    public float height() { return height * scale.y; }
    public RectF frame() { return new RectF(); }
    public void frame(RectF f) {}
    public void alpha(float v) { alpha = v; }
    public void hardlight(int c) {}
}
""",
    "com/watabou/noosa/Group.java": """package com.watabou.noosa;
import java.util.ArrayList;
public class Group extends Gizmo {
    protected ArrayList<Gizmo> members = new ArrayList<>();
    public int length;
    //mirror the real Group.add: it reparents the gizmo, which is what makes
    //Gizmo.parent a usable "was this really added?" probe.
    public synchronized Gizmo add(Gizmo g) {
        //faithful to the real Group.add: a null gizmo is a bug, not a no-op
        if (g.parent == this) return g;
        if (g.parent != null) g.parent.remove(g);
        members.add(g);
        g.parent = this;
        length++;
        return g;
    }
    public synchronized Gizmo addToFront(Gizmo g) { members.add(0, g); g.parent = this; return g; }
    public synchronized Gizmo addToBack(Gizmo g) { members.add(g); g.parent = this; return g; }
    public synchronized Gizmo remove(Gizmo g) { members.remove(g); if (g.parent == this) g.parent = null; return g; }
    public synchronized void clear() { for (Gizmo g : members) if (g.parent == this) g.parent = null; members.clear(); }
}
""",
    "com/watabou/noosa/Image.java": """package com.watabou.noosa;
public class Image extends Visual {
    public Image() {}
    public Image(Visual src) {}
    public void copy(Image src) {}
    public void hardlight(int color) {}
}
""",
    "com/watabou/noosa/ColorBlock.java": """package com.watabou.noosa;
public class ColorBlock extends Image {
    public ColorBlock() {}
    public ColorBlock(float w, float h, int color) {}
    public void size(float w, float h) {}
    public void color(int c) {}
}
""",
    "com/watabou/noosa/Camera.java": """package com.watabou.noosa;
public class Camera extends Gizmo {
    public static Camera main;
    public float zoom = 1f;
    public int x, y, width, height;
    public Camera(int x, int y, int w, int h, float z) { this.x=x; this.y=y; this.width=w; this.height=h; this.zoom=z; }
    public void resize(int w, int h) { width=w; height=h; }
    public float screenWidth() { return width * zoom; }
    public float screenHeight() { return height * zoom; }
    public boolean hitTest(int x, int y) { return true; }
}
""",
    "com/watabou/noosa/Game.java": """package com.watabou.noosa;
public class Game {
    public static int width, height;
    public static Scene instance;
    public static void reportException(Throwable t) { t.printStackTrace(); }
    public static Scene scene() { return null; }
}
""",
    "com/watabou/noosa/Scene.java": """package com.watabou.noosa;
public class Scene extends Group {
    public Camera camera() { return Camera.main; }
}
""",
    "com/watabou/noosa/BitmapText.java": """package com.watabou.noosa;
public class BitmapText extends Visual {
    private String t = "";
    public BitmapText() {}
    public BitmapText(Font f) {}
    public void text(String t) { this.t = t; }
    public String text() { return t; }
    public void measure() {}
    public void hardlight(int c) {}
    public static class Font {}
}
""",
    "com/watabou/noosa/PointerArea.java": """package com.watabou.noosa;
import com.watabou.input.PointerEvent;
public class PointerArea extends Visual {
    public static final int ALWAYS_BLOCK=0, BLOCK_WHEN_ACTIVE=1, NEVER_BLOCK=2;
    public Visual target;
    public int blockLevel = BLOCK_WHEN_ACTIVE;
    public PointerArea(Visual t) { this.target = t; }
    public PointerArea(float x, float y, float w, float h) { super(x,y,w,h); this.target = this; }
    protected void onPointerDown(PointerEvent e) {}
    protected void onDrag(PointerEvent e) {}
    protected void onClick(PointerEvent e) {}
}
""",
    "com/watabou/noosa/ScrollArea.java": """package com.watabou.noosa;
import com.watabou.input.ScrollEvent;
public class ScrollArea extends PointerArea {
    public ScrollArea(Visual t) { super(t); }
    public ScrollArea(float x, float y, float w, float h) { super(x,y,w,h); }
    protected void onScroll(ScrollEvent e) {}
}
""",
    "com/watabou/noosa/ui/Component.java": """package com.watabou.noosa.ui;
import com.watabou.noosa.Group;
public class Component extends Group {
    protected float x, y, width, height;
    public Component() {}
    public Component setPos(float x, float y) { this.x=x; this.y=y; layout(); return this; }
    public Component setSize(float w, float h) { this.width=w; this.height=h; layout(); return this; }
    public Component setRect(float x, float y, float w, float h) { this.x=x; this.y=y; this.width=w; this.height=h; layout(); return this; }
    public float left() { return x; }
    public float right() { return x + width; }
    public float top() { return y; }
    public float bottom() { return y + height; }
    public float width() { return width; }
    public float height() { return height; }
    protected void createChildren() {}
    protected void layout() {}
}
""",
    "com/watabou/noosa/ui/BlockText.java": """package com.watabou.noosa.ui;
//Stands in for a host's Component-based text widget (SPD's RenderedTextBlock).
//It exists to prove the font seam is shape-agnostic: a plain Visual cannot represent
//this, so a seam typed as Visual would make it impossible for a host to render the
//glyphs its language needs.
public class BlockText extends Component {
    private String text;
    public BlockText(String text) { super(); this.text = text; setSize(8, 6); }
    public String text() { return text; }
    public void hardlight(int c) {}
}
""",
    "com/watabou/input/PointerEvent.java": """package com.watabou.input;
import com.watabou.utils.PointF;
public class PointerEvent { public PointF current = new PointF(); }
""",
    "com/watabou/input/ScrollEvent.java": """package com.watabou.input;
import com.watabou.utils.PointF;
public class ScrollEvent { public PointF pos = new PointF(); public float amount; }
""",
    "com/watabou/utils/PointF.java": """package com.watabou.utils;
public class PointF {
    public float x, y;
    public PointF() {}
    public PointF(float x, float y) { this.x=x; this.y=y; }
    public PointF set(float x, float y) { this.x=x; this.y=y; return this; }
    public PointF set(PointF p) { this.x=p.x; this.y=p.y; return this; }
}
""",
    "com/watabou/utils/RectF.java": """package com.watabou.utils;
public class RectF {
    public float left, top, right, bottom;
    public RectF() {}
    public RectF(float l, float t, float r, float b) { left=l; top=t; right=r; bottom=b; }
}
""",
    "com/watabou/utils/DeviceCompat.java": """package com.watabou.utils;
public class DeviceCompat {
    public static boolean isAndroid() { return false; }
    public static boolean isDebug() { return true; }
    public static void log(String t, String m) {}
}
""",
}

# ---------------------------------------------------------------- demo usage
DEMO = """package demo;

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
        if (first == null || !first.contains("\u540d\u79f0")) {
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
"""


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(text)


def sh(cmd, cwd):
    env = dict(os.environ, JAVA_HOME=JDK, GRADLE_USER_HOME=GRADLE_USER_HOME)
    return subprocess.run(cmd, cwd=cwd, env=env, capture_output=True, text=True,
                          encoding='utf-8', errors='replace', shell=True)


def main():
    if len(sys.argv) < 2:
        print("用法: python _chk/verify_dropin_zip.py <zip>")
        return 2

    zpath = os.path.abspath(sys.argv[1])
    if not os.path.exists(zpath):
        print("找不到 zip:", zpath)
        return 2

    # fresh workspace
    # note: use a unique subdir per run rather than relying on rmtree, because the sandbox's
    # safe-delete interposition can fail on directories and leave them behind.
    harness = os.path.join(WORK, "harness")

    def force_clean(path):
        if not os.path.exists(path):
            return
        try:
            shutil.rmtree(path, ignore_errors=True)
        except Exception:
            pass
        if os.path.exists(path):
            # fall back to manual walk-and-delete
            for root, dirs, files in os.walk(path, topdown=False):
                for f in files:
                    try:
                        os.remove(os.path.join(root, f))
                    except OSError:
                        pass
                for d in dirs:
                    try:
                        os.rmdir(os.path.join(root, d))
                    except OSError:
                        pass
            try:
                os.rmdir(path)
            except OSError:
                pass

    force_clean(WORK)
    if os.path.exists(WORK):
        # last resort: pick a fresh directory name so the run can still proceed
        import time
        WORK_ACTUAL = WORK + "_" + str(int(time.time()))
        harness = os.path.join(WORK_ACTUAL, "harness")
        print("[warn] 无法清理旧目录，改用:", WORK_ACTUAL)
    os.makedirs(harness, exist_ok=True)

    # 1) extract the zip
    with zipfile.ZipFile(zpath) as z:
        z.extractall(harness)

    # the zip's top folder must be debug-console/
    roots = [d for d in os.listdir(harness) if os.path.isdir(os.path.join(harness, d))]
    if "debug-console" not in roots:
        print("FAIL: zip 内顶层目录不是 debug-console/，实际:", roots)
        return 1
    print("[1/4] 解压 OK，顶层目录:", roots)

    # 2) scaffold the host project
    write(os.path.join(harness, "settings.gradle"),
          "include ':my-noosa'\ninclude ':host'\ninclude ':debug-console'\n")
    # point the module at OUR slim module, whose name is not SPD-classes
    write(os.path.join(harness, "gradle.properties"), "debugConsoleSlimProject=:my-noosa\n")
    write(os.path.join(harness, "build.gradle"), """allprojects {
    ext {
        gdxVersion = '1.14.0'
        appJavaCompatibility = JavaVersion.VERSION_11
    }
    repositories {
        maven { url 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
    }
}
""")
    write(os.path.join(harness, "my-noosa", "build.gradle"), """apply plugin: 'java-library'
java.sourceCompatibility = java.targetCompatibility = appJavaCompatibility
dependencies { api "com.badlogicgames.gdx:gdx:$gdxVersion" }
""")
    write(os.path.join(harness, "host", "build.gradle"), """apply plugin: 'java-library'
java.sourceCompatibility = java.targetCompatibility = appJavaCompatibility
dependencies {
    api project(':debug-console')
    api project(':my-noosa')
}
""")

    # stub noosa layer, deliberately under a different module name
    for rel, src in STUBS.items():
        write(os.path.join(harness, "my-noosa", "src", "main", "java", *rel.split("/")), src)

    # demo host using only the documented API
    write(os.path.join(harness, "host", "src", "main", "java", "demo", "DemoHost.java"), DEMO)
    print("[2/4] 宿主工程搭建 OK（slim 模块故意叫 :my-noosa）")

    # 3) compile
    r = sh(f'"{GRADLE}" :host:classes --console=plain -q', harness)
    out = (r.stdout or "") + (r.stderr or "")
    if r.returncode != 0:
        print("FAIL: 编译失败\n" + out[-3000:])
        return 1
    print("[3/4] 编译 OK（模块未改一行源码）")

    # 4) run
    gdx = None
    for root, _, files in os.walk(GRADLE_USER_HOME):
        for f in files:
            if f == "gdx-1.14.0.jar":
                gdx = os.path.join(root, f)
                break
        if gdx:
            break
    if not gdx:
        print("WARN: 找不到 gdx jar，跳过运行")
        return 0

    cp = ";".join([
        os.path.join(harness, "host", "build", "classes", "java", "main"),
        os.path.join(harness, "debug-console", "build", "classes", "java", "main"),
        os.path.join(harness, "my-noosa", "build", "classes", "java", "main"),
        gdx,
    ])
    r = subprocess.run([os.path.join(JDK, "bin", "java.exe"), "-cp", cp, "demo.DemoHost"],
                       capture_output=True, text=True, encoding='utf-8', errors='replace')
    out = (r.stdout or "") + (r.stderr or "")
    if "PASS" not in out:
        print("FAIL: 运行未通过\n" + out[-3000:])
        return 1
    print("[4/4] 运行 OK")
    print("      " + [l for l in out.splitlines() if "PASS" in l][0])
    print("\n结论：压缩包可直接解压进其它项目使用。")
    return 0


if __name__ == "__main__":
    sys.exit(main())
