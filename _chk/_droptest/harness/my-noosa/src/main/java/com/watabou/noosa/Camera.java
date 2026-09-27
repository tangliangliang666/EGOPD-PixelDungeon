package com.watabou.noosa;
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
