#!/bin/bash
# YESOD（根基）容器问号 —— 编译本轮 5 个改动文件 → _chk/_javachk2（供 javap 层核验）
# 用法：bash _chk/_build_yesod_container.sh
#
# 为什么单独一份：这 5 个文件与 _build_yesod.sh（YESOD 首批）和 _build_netzach_overlay.sh
# （NETZACH 浮层）都不重叠，而核验脚本 verify_yesod.py 的容器段要 javap 它们的新字节码，
# core/build/classes 里的旧产物仍是「只拦 HEAP/FOR_SALE」的版本 ⇒ 必须先编一遍覆盖。
set -u
cd /d/PD || exit 2

JDK="D:/PD/tools/jdk-21.0.12.1+1/bin"
GDX="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.14.0/8accfce8d9313d9ddd23a2c9e315179783085ef2/gdx-1.14.0.jar"
GDXCTRL="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx-controllers/gdx-controllers-core/2.2.4/d8a18ff371fb01c1763e9fe5ea050e39d2d66437/gdx-controllers-core-2.2.4.jar"

C="D:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL"

SRC=(
  "$C/Trials.java"
  "$C/items/Heap.java"
  "$C/sprites/ItemSprite.java"
  "$C/ui/ItemSlot.java"
  "$C/ui/LootIndicator.java"
)

OUT="D:/PD/_chk/_javachk2"
mkdir -p "$OUT"

echo "=== 编译 ${#SRC[@]} 个文件 → _chk/_javachk2（含 -Xlint:all）==="
"$JDK/javac" -proc:none -Xlint:all -encoding UTF-8 \
  -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d "$OUT" \
  "${SRC[@]}" 2> _chk/_ie_yesod_container.log
echo "EXIT=$?   （日志 _chk/_ie_yesod_container.log，GBK）"
tail -n 20 _chk/_ie_yesod_container.log
