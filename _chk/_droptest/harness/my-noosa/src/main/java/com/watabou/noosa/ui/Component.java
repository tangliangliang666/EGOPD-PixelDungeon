package com.watabou.noosa.ui;
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
