#!/bin/bash
# 考验 YESOD（根基）+ NETZACH 阈值改动 —— 编译改动文件 → _chk/_javachk2，并可跑行为探针
# 用法：bash _chk/_build_yesod.sh [probe|run|all]
#   probe  只编译 11 个改动文件
#   run    编译并运行探针 YesodProbe
#   all    1 + run（默认）
set -u
cd /d/PD || exit 2

JDK="D:/PD/tools/jdk-21.0.12.1+1/bin"
GDX="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.14.0/8accfce8d9313d9ddd23a2c9e315179783085ef2/gdx-1.14.0.jar"
GDXCTRL="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx-controllers/gdx-controllers-core/2.2.4/d8a18ff371fb01c1763e9fe5ea050e39d2d66437/gdx-controllers-core-2.2.4.jar"

C="D:/PD/core/src/main/java/com/shatteredpixel/shatteredpixeldungeon"
CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL"

SRC=(
  "$C/Trials.java"
  "$C/sprites/ItemSpriteSheet.java"
  "$C/sprites/ItemSprite.java"
  "$C/items/Heap.java"
  "$C/windows/WndInfoItem.java"
  "$C/items/weapon/Weapon.java"
  "$C/items/armor/Armor.java"
  "$C/items/rings/Ring.java"
  "$C/items/wands/Wand.java"
  "$C/windows/WndEnergizeItem.java"
  "$C/levels/rooms/special/ShopRoom.java"
  # 2026-09-26 追加：容器也变问号 + 战利品指示器跟随（第 4 个出口）
  "$C/ui/ItemSlot.java"
  "$C/ui/LootIndicator.java"
)

OUT="D:/PD/_chk/_javachk2"
MODE="${1:-all}"

mkdir -p "$OUT"

if [ "$MODE" = "probe" ] || [ "$MODE" = "all" ]; then
  echo "=== [1/2] 编译 ${#SRC[@]} 个改动文件 → _chk/_javachk2 ==="
  "$JDK/javac" -proc:none -Xlint:all -encoding UTF-8 \
    -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d "$OUT" \
    "${SRC[@]}" 2> _chk/_ie_yesod.log
  echo "EXIT=$?  （日志 _chk/_ie_yesod.log，GBK）"

  echo "=== [1b/2] 同一批文件也刷进 _chk/_javachk（旧核验共用）==="
  mkdir -p _chk/_javachk
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" -sourcepath "D:/PD/core/src/main/java" -d "D:/PD/_chk/_javachk" \
    "${SRC[@]}" 2> _chk/_ie_yesod_javachk.log
  echo "EXIT=$?  （日志 _chk/_ie_yesod_javachk.log，GBK）"
fi

if [ "$MODE" = "run" ] || [ "$MODE" = "all" ]; then
  echo "=== [2/2] 编译并运行探针（_chk/_javachk2 排最前，覆盖旧产物）==="
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk2;$CP" -sourcepath "D:/PD/_chk" -d "D:/PD/_chk/_javachk2" \
    _chk/YesodProbe.java 2> _chk/_yesodprobe_javac.log
  echo "编译 EXIT=$?  （日志 _chk/_yesodprobe_javac.log，GBK）"

  "$JDK/java" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -cp "D:/PD/_chk/_javachk2;$CP" YesodProbe > _chk/_yesod.out 2> _chk/_yesod.err
  echo "运行 EXIT=$?   （输出 _chk/_yesod.out）"
  tail -n 6 _chk/_yesod.out
fi
