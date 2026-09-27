#!/bin/bash
# 「带着标记倒下」击杀判定改动 ＋ 矿洞层虚空地形 —— 编译（改动文件 → _chk/_javachk）＋ 两个探针
# 用法：bash _chk/_build_marked_kill.sh [probe|run|probeB|all]
#   probe  只编译 11 个改动文件
#   run    只编译并运行探针 A（MarkedKillProbe，击杀判据）
#   probeB 只编译并运行探针 B（MiningVoidProbe，虚空地形）
#   all    1 + A + B（默认）
set -u
cd /d/PD || exit 2

JDK="D:/PD/tools/jdk-21.0.12.1+1/bin"
GDX="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx/1.14.0/8accfce8d9313d9ddd23a2c9e315179783085ef2/gdx-1.14.0.jar"
GDXCTRL="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx-controllers/gdx-controllers-core/2.2.4/d8a18ff371fb01c1763e9fe5ea050e39d2d66437/gdx-controllers-core-2.2.4.jar"
GDXJNI="C:/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx-jnigen-loader/2.3.1/$(ls /c/Users/14675/.gradle/caches/modules-2/files-2.1/com.badlogicgames.gdx/gdx-jnigen-loader/2.3.1/ 2>/dev/null | head -1)/gdx-jnigen-loader-2.3.1.jar"

CORE="D:/PD/core/src/main/java"
SRCMOB="$CORE/com/shatteredpixel/shatteredpixeldungeon/actors/mobs/Mob.java"
SRCTRIALS="$CORE/com/shatteredpixel/shatteredpixeldungeon/Trials.java"
SRCCHAR="$CORE/com/shatteredpixel/shatteredpixeldungeon/actors/Char.java"
SRCPALERMO="$CORE/com/shatteredpixel/shatteredpixeldungeon/items/weapon/melee/PalermoFencing.java"
SRCLEVEL="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/Level.java"
SRCCHASM="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/features/Chasm.java"
SRCMINE="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/MiningLevel.java"
SRCSMALL="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/rooms/quest/MineSmallRoom.java"
SRCLARGE="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/rooms/quest/MineLargeRoom.java"
SRCGIANT="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/rooms/quest/MineGiantRoom.java"
SRCPAINTER="$CORE/com/shatteredpixel/shatteredpixeldungeon/levels/painters/MiningLevelPainter.java"

CP="D:/PD/core/build/classes/java/main;D:/PD/SPD-classes/build/classes/java/main;D:/PD/services/build/classes/java/main;$GDX;$GDXCTRL"

MODE="${1:-all}"

mkdir -p _chk/_javachk

if [ "$MODE" = "probe" ] || [ "$MODE" = "all" ]; then
  echo "=== [1/3] 编译 11 个改动文件 → _chk/_javachk ==="
  "$JDK/javac" -proc:none -Xlint:all -encoding UTF-8 \
    -cp "$CP" -sourcepath "D:/PD/core/src/main/java" -d "D:/PD/_chk/_javachk" \
    "$SRCMOB" "$SRCTRIALS" "$SRCCHAR" "$SRCPALERMO" "$SRCLEVEL" "$SRCCHASM" \
    "$SRCMINE" "$SRCSMALL" "$SRCLARGE" "$SRCGIANT" "$SRCPAINTER" \
    2> _chk/_ie.log
  echo "EXIT=$?  （日志 _chk/_ie.log，GBK）"
fi

if [ "$MODE" = "run" ] || [ "$MODE" = "all" ]; then
  echo "=== [2/4] 编译探针 A（_chk/_javachk 排最前）==="
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" -sourcepath "D:/PD/_chk" -d "D:/PD/_chk/_javachk" \
    _chk/MarkedKillProbe.java 2> _chk/_probe_javac.log
  echo "EXIT=$?  （日志 _chk/_probe_javac.log，GBK）"

  echo "=== [3/4] 运行探针 A（击杀判据）==="
  # -Dstdout/stderr.encoding=UTF-8：JDK18+ 的 System.out 默认走控制台码页（GBK），
  # U+21D2「⇒」之类不在 GBK 里会被换成 '?'，让核验脚本没法按原文匹配。写日志文件一律 UTF-8。
  "$JDK/java" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" MarkedKillProbe > _chk/_markedkill.out 2> _chk/_markedkill.err
  echo "EXIT=$?"
fi

if [ "$MODE" = "run" ] || [ "$MODE" = "probeB" ] || [ "$MODE" = "all" ]; then
  echo "=== [4/4] 编译＋运行探针 B（矿洞层虚空地形）==="
  "$JDK/javac" -proc:none -encoding UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" -sourcepath "D:/PD/_chk" -d "D:/PD/_chk/_javachk" \
    _chk/MiningVoidProbe.java 2> _chk/_probeB_javac.log
  echo "COMPILE EXIT=$?  （日志 _chk/_probeB_javac.log，GBK）"

  "$JDK/java" -Dfile.encoding=UTF-8 -Dstdout.encoding=UTF-8 -Dstderr.encoding=UTF-8 \
    -cp "D:/PD/_chk/_javachk;$CP" MiningVoidProbe > _chk/_miningvoid.out 2> _chk/_miningvoid.err
  echo "RUN EXIT=$?  （输出 _chk/_miningvoid.out，UTF-8）"
fi
