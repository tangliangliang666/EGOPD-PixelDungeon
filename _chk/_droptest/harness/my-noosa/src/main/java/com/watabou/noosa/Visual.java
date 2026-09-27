package com.watabou.noosa;
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
