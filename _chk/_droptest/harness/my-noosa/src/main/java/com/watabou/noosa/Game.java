package com.watabou.noosa;
public class Game {
    public static int width, height;
    public static Scene instance;
    public static void reportException(Throwable t) { t.printStackTrace(); }
    public static Scene scene() { return null; }
}
