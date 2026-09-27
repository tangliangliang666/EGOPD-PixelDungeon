#!/bin/bash
# NETZACH（胜利）浮层淡出 —— 编译改动文件 → _chk/_javachk2（供 javap 层核验）
# 用法：bash _chk/_build_netzach_overlay.sh
#
# 为什么单独一份：这 5 个文件都不属于 YESOD 那批（_build_yesod.sh），
# 但核验脚本 verify_netzach_overlay.py 要 javap 它们的新字节码，
# 而 core/build/classes 里的旧产物没有 setAlpha / 新版 draw ⇒ 必须先编一遍覆盖。
set -u
cd /d/PD || exit 2

JDK="D:/PD/tools/jdk-21.0.12.1+1/bin"
GDX="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.14.0/8accfce8d9313d9ddd23a2c9e315179783085ef2/gdx-1.14.0.jar"
GDXCTRL="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx-controllers/gdx-controllers-core/2.2.4/d8a18ff371fb01c1763e9fe5ea050e39d2d66437/gdx-controllers-core-2.2.4.jar"

C="D:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL"

SRC=(
  "$C/Trials.java"
  "$C/ui/HealthBar.java"
  "$C/ui/CharHealthIndicator.java"
  "$C/ui/TargetHealthIndicator.java"
  "$C/effects/EmoIcon.java"
)

OUT="D:/PD/_chk/_javachk2"
mkdir -p "$OUT"

echo "=== 编译 ${#SRC[@]} 个文件 → _chk/_javachk2（含 -Xlint:all）==="
"$JDK/javac" -proc:none -Xlint:all -encoding UTF-8 \
  -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d "$OUT" \
  "${SRC[@]}" 2> _chk/_ie_netzach_overlay.log
echo "EXIT=$?   （日志 _chk/_ie_netzach_overlay.log，GBK）"
tail -n 20 _chk/_ie_netzach_overlay.log
