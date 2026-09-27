package com.watabou.noosa;
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
