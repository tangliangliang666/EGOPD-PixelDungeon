String zh = "这件武器命中时有_%d%%_概率将目标或英雄随机传送到本层某处；该概率随强化等级提升，_+0 时为 1/8_，等级越高越接近 _1/2_。";
System.out.println("zh +0  -> " + String.format(java.util.Locale.CHINA, zh, 12));
System.out.println("zh +10 -> " + String.format(java.util.Locale.CHINA, zh, 40));
System.out.println("默认 locale -> " + String.format(zh, 40));
