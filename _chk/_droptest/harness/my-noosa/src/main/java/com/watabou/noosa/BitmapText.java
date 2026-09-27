package com.watabou.noosa;
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
