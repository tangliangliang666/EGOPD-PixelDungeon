package com.watabou.utils;
public class PointF {
    public float x, y;
    public PointF() {}
    public PointF(float x, float y) { this.x=x; this.y=y; }
    public PointF set(float x, float y) { this.x=x; this.y=y; return this; }
    public PointF set(PointF p) { this.x=p.x; this.y=p.y; return this; }
}
