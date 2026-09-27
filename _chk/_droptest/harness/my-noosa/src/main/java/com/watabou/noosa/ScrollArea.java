package com.watabou.noosa;
import com.watabou.input.ScrollEvent;
public class ScrollArea extends PointerArea {
    public ScrollArea(Visual t) { super(t); }
    public ScrollArea(float x, float y, float w, float h) { super(x,y,w,h); }
    protected void onScroll(ScrollEvent e) {}
}
